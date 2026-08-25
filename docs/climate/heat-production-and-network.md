# 产热设备与热网

- Status: `Current`
- Last verified: `2026-08-25`
- Scope: T1/T2 能量塔、局部热区写入、`HeatEndpoint`/`HeatNetwork`、散热器、蒸汽喷泉、穿戴设备
- Primary code anchors: `GeneratorData`, `GeneratorLogic`, `GeneratorState`, `GeneratorHeatFieldModel`, `HeatingLogic`, `HeatingState`, `T2GeneratorLogic`, `HeatEndpoint`, `HeatNetwork`, `RadiatorLogic`, `RadiatorState`, `FountainTileEntity`, `MinecraftPhysicalSourceProfile`, `MinecraftPhysicalSourceManager`, `MaterialBoundaryRegistry`, `PhaseTransitionRuntime`, `MinecraftPhaseTransitionHandler`, `MinecraftThermalInput.enablePhysicalSources`, `MinecraftThermalInput.sampleMachineEnvironment`, `MinecraftThermalInput.observeRegisteredMachineEnvironment`

## 1. 三种“热”不是同一种量

当前代码包含三条彼此耦合但单位不同的产热路径：

| Layer | State | Current semantics |
|---|---|---|
| 世界热区 | `ChunkHeatData` / `IHeatArea.value` | 直接影响空气或方块温度公式的空间温度控制值 |
| 热网 | `HeatEndpoint.heat`, `maxIntake`, `maxOutput` | 每 tick 存取的任意 heat unit 缓冲和流量限额 |
| 玩家设备 | `BodyHeatingCapability` / item heat storage | 直接改变玩家部位 effective temperature，不经过世界热区 |

`GeneratorData.power` 会复制到 T2 的 `HeatEndpoint.heat`，但没有物理单位换算。热网的 `tempLevel` 又独立于 heat 数量传递给散热器，再由散热器换算成摄氏度热区。现有变量名中的 `power` 有时表示库存，有时表示每 tick 输入/输出显示；都不能直接解释为瓦特。

## 2. 能量塔燃料与等级

一个队伍通过 SpecialData 持有一个 `GeneratorData`，记录燃料槽、配方剩余 process ticks、工作/超载/损坏状态、`heated/ranged` 进度、`TLevel/RLevel`、蒸汽等级和 power。多方块 `GeneratorState` 负责把队伍数据投影到当前方块实例。

燃料由 `frostedheart:generator` recipe 提供 `time` process ticks。加载一件燃料时：

```text
effectiveProcessTicks = floor(recipe.time *
                              (baseFuelDurationMultiplier + researchEfficiencyBonus))
```

默认 `baseFuelDurationMultiplier=0.7`。正常工作每 game tick 消耗 `1` process tick，超载额外消耗 `1`。剩余 process 不足一个结算批次时才加载下一件燃料，并保留旧余额。

活动时 `heated` 和 `ranged` 每 tick 各以基础概率 `0.05` 朝目标移动 1；超载时概率翻倍。不活动时各以 `0.10` 概率向零衰减：

```text
TLevel = heated / 100
RLevel = ranged / 100
max TLevel = 1 + (overdrive ? 1 : 0) + steamLevel
max RLevel = 1 + steamLevel
```

`steamLevel` 在 T2 状态中以整数 `0..100` 保存，写回 `GeneratorData` 时除以 `100`。T2 活动时默认每 tick模拟抽取 `144 mB` 蒸汽，累计默认 `60` ticks 增加 1 个百分点；供应不足时蒸汽等级按现有 `liquidtick/noliquidtick` 状态机回落。

超载每 tick 增加过载进度，同时平时按研究倍率恢复；达到 `240400` 时塔损坏并进入爆炸表现倒计时。该机制与热力学能量无关。

## 3. 能量塔世界热区

`HeatingLogic.tickServer` 每 tick 依次结算燃料、更新等级、检测半径/温度变化并维护球形热区。热区中心是多方块绝对 master 向下移动 `masterYPosInMB`。

T1/T2 共用 `GeneratorHeatFieldModel`：

```text
RLevel <= 0 : radius = 0
0 < RLevel <= 1 : radius = floor(baseRadius * RLevel)
RLevel > 1 : radius = floor(baseRadius + (RLevel - 1) * radiusPerLevel)

heatFieldTemperature = floor(TLevel * temperaturePerLevelCelsius)
```

默认 `baseRadius=16`、`radiusPerLevel=8`、`temperaturePerLevelCelsius=10`。因此等级 `1` 产生半径 `16`、热区值 `10degC` 的整数格点球；等级 `2` 产生半径 `24`、热区值 `20degC`。只有半径和温度都大于零时写入热区，否则删除。

热区内部值恒定，没有径向衰减，也不消耗 `GeneratorData.power`。T1 即使没有热网也直接维护该球形热区。T2 在此基础上额外提供热网端点。

城镇批处理调用 `GeneratorData.townTick` 时会按城镇更新间隔批量推进同一燃料/等级状态，并直接维护同一个球形热区；`townProcessedTicks` 暂时阻止方块 tick 重复推进和重复写场。

## 4. HeatEndpoint 状态

`HeatEndpoint` 是设备与 `HeatNetwork` 的连接状态，主要字段为：

| Field | Meaning |
|---|---|
| `heat` | 当前任意 heat unit 缓冲，NBT 键为 `net_power` |
| `capacity` | 缓冲上限 |
| `maxOutput` | 每次 provider 提取调用的上限 |
| `maxIntake` | 每次 consumer 接收调用的上限 |
| `tempLevel` | 最近供热网络给出的无量纲温度等级 |
| `priority` | 分配顺序，高值先处理 |
| `distance` | 管路到网络中心的距离，只用于同优先级排序/连接信息，不产生损耗 |
| `intake/output` | 当前 tick 实际接收/输出 |
| `avgIntake/avgOutput` | `95%` 历史、`5%` 新值的显示平滑 |

`consumer(priority,maxIntake)` 和 `provider(priority,maxOutput)` 都经由私有构造器创建；该构造器当前固定令 `capacity = 4 * max(maxIntake,maxOutput)`，传入的 `capacityRatio` 参数没有参与公式。例：散热器 `consumer(100,4)` 的容量为 `16`；T2 塔 `provider(0,2000)` 的容量为 `8000`。

`drainHeat`/`tryDrainHeat` 从设备自己的端点缓冲扣除数值。端点从 NBT 载入后有 `20` 次 drain grace：调用会报告可用热量但只减少 `loadtick`，暂不扣除 `heat`。

## 5. HeatNetwork 每 tick 分配

热网通过 `NetworkConnector` 沿六面 DFS 发现管道和端点。管路位置与最短已知距离保存在 `propagated`；连接变化可以在 10 或 20 ticks 后触发全量重建。网络规模较大的网络在合并竞争中优先保留。

端点优先队列按：

```text
priority descending, then distance ascending
```

每个网络 tick 的分配顺序为：

1. 所有可输出端点各取 `min(heat,maxOutput)`，相加为 `accumulated`。
2. 本 tick 网络 `tempLevel` 取所有实际输出 provider 的最大 `tempLevel`，不是按输出量加权。
3. 按优先队列给可接收端点分配，每次调用最多 `min(maxIntake,capacity-heat)`。
4. 若第一次接收非零且仍有余额，当前代码会对同一端点再调用一次接收，因此一个 tick 实际最多可得到约 `2 * maxIntake`，而不是字段名暗示的一次上限。
5. 未使用余额按各 provider 本 tick 原始贡献比例退回 provider 缓冲。
6. 更新端点 intake/output 平滑显示。

管道没有传输时间、沿程热损、容量、温度衰减或环境交换。网络自身也没有独立储能；所有余额都在端点中。

## 6. T2 能量塔与热网耦合

`T2GeneratorState` 持有 provider 端点、内部 `HeatNetwork manager` 和管道重连器。T2 每 tick 先保证自身 provider 加入 manager，再运行网络分配和基础能量塔燃料逻辑。

`GeneratorState.tickData` 的当前同步顺序是：

```text
GeneratorData.tickBlock(...)
GeneratorData.lastPower = endpoint.heat
endpoint.heat = GeneratorData.power
endpoint.tempLevel = GeneratorData.TLevel
HeatingState range/temp levels = GeneratorData R/T levels
```

`GeneratorData.tickFuelProcess` 又使用 `lastPower` 计算当前补充需求并在方法末尾重置部分状态。因而塔内 `power`、端点缓冲、网络先取后写和城镇批处理存在明确的 tick 顺序耦合；不能只凭字段名把它简化成稳定的“输入功率”。现有 [town/town-model.md](../town/town-model.md) 也把 T2 时序夹具列为单独验证边界。

## 7. 热网到世界热区的消费者

### 7.1 散热器

`RadiatorState` 使用 `HeatEndpoint.consumer(100,4)`。每 tick `RadiatorLogic` 尝试从自身端点缓冲扣除 `4`：

- 成功时 active，`tempLevel=rangeLevel=endpoint.tempLevel`；
- 失败时 inactive，两个等级归零。

散热器继承 `HeatingLogic`，所以把等级转换为球形世界热区：

```text
radius(level <= 1) = floor(8 * level)
radius(level > 1)  = floor(8 + 8 * (level - 1))
temperature        = floor(10 * level)
```

等级 `1` 即半径 `8`、热区值 `10degC`。虽然 `RadiatorState` 仍定义上下范围方法，当前 `HeatingLogic` 已改用球体而不是柱体，这两个垂直范围不参与世界场。

### 7.2 蒸汽喷泉

`FountainTileEntity` 使用 `HeatEndpoint.consumer(10,1)`。补充阶段从端点请求热，把实际值乘 `0.8` 存入自己的 `power`；工作时每 tick 消耗 `1 power`。它创建柱形 `ChunkHeatData`：

```text
radius = structure range
upper = nozzle height + 1
lower = 1
temperature = floor(endpoint.tempLevel) * 15
```

喷泉同时给水中的玩家施加 warmth/sauna 相邻效果。其世界热区和玩家效果是两条并行路径。

### 7.3 其他热网设备

充能器、热孵化器、创意热源和部分城镇建筑也使用 `HeatEndpoint`，但并非都创建 `ChunkHeatData`。只有显式调用 `ChunkHeatData.add*TempAdjust` 或继承当前 `HeatingLogic` 的设备才改变世界温度。

## 8. 穿戴设备路径

`BodyHeatingCapability` 是瞬态物品能力。`TemperatureComputation.equipmentHeating` 遍历原版装备槽和可见 Curios 槽，每个体温更新周期调用设备：

```text
item storage/fuel -> addEffectiveTemperature(bodyPart, delta)
```

手炉主要加手或躯干，加热背心加躯干，蒸汽瓶从 `ITEM_HEAT` 抽取并按设备自己的比例换成 effective temperature。该路径不经过 `HeatNetwork`、不写世界热区，也没有统一的 heat unit 到摄氏度换算。

## 9. 玩家测试路径中的物理 source

新热学 runtime 有两种冻结的 `POWER_SOURCE` profile。首次玩家温度查询创建 runtime 时会调用 `MinecraftThermalInput.enablePhysicalSources(64)`，所以它们参与当前玩家空气与辐射实机测试；本页第 2..7 节的 legacy 热网、HU 缓冲和非玩家消费者行为仍未替换：

| Profile | SI power | Ports | 缺失端口 |
|---|---:|---|---|
| Campfire | `8,000 W` | `6,400 W (80%)` 向上表面空气对流；`1,600 W (20%)` radiation declared loss | 堵塞对流进入 declared loss；unloaded/unresolved 进入 `DEGRADED_LOSS` |
| Generator | `10,000 W * TLevel` | `70%` exhaust 空气对流；`10%` internal heat；`20%` radiation declared loss | 堵塞 exhaust 进入 internal heat；unloaded/unresolved 进入 `DEGRADED_LOSS` |

这里的 Generator 只读取 `GeneratorState.getTempLevel()` 和活动状态来形成独立 SI profile；它不把 `GeneratorData.power`、`HeatEndpoint.heat` 或现有 heat unit 重新解释为 J/W。Phase J 用同一个 source lifecycle 和 radiation port share 建立只读 receiver index；source ledger 中这部分仍只结算一次 declared ambient loss，不会因玩家采样次数再次扣除或注入。

空气端口按声明的 block face 解析 published topology 中唯一接触的 air component，不使用最近 cell。无空气是 blocked；包含 unresolved topology 或一个面接触多个无法由单端口唯一表达的 component 时走 degraded loss。source 自己可在已加载 chunk 内申请有硬上限的 cold Page interest；预算不足不会积累以后突然释放的 energy debt。Page mutation、unload 或 topology replacement 会先在旧 binding 精确结算到事件 tick，再切换 sink/binding；post-preapply 安装竞争则完成一个无 transport 的 unresolved epoch，下一 frame 重建。

Campfire 的 `8,000 W` 是恢复密闭小屋取暖体验的暂定玩法校准值，不改变 `80/20` 功率分配、遮挡、能量记账或相变合同。Campfire 通过放置/section mutation/chunk block-entity load 观察，Generator 由 `GeneratorLogic` tick 直接报告并在 multiblock disassemble 时移除。玩家首次温度查询前这些 hook 仍只有一次 active-input/null 检查；runtime 建立后它们只更新这一份 physical source ownership，不产生第二份新架构 source ledger。

## 10. 材料边界与物态转换

`MaterialBoundaryRegistry` 是显式传给 `MinecraftThermalInput.enableTopologyApplication` 的 immutable 参数切片；测试可继续使用空 registry overload，gameplay runtime 则安装由 `StateTransitionData` cache 一次编译出的热侧 phase profiles。profile 的 `G` 单位为 `W/K` per full block face，surface/deep `C` 为 `J/K` per full block face，实际 `4x4` material contact 按面积缩放。`STATELESS_CONDUCTANCE`、`CAPACITIVE_SURFACE` 与 `NATURAL_ROCK` 尚未定义 gameplay profile，仍为 dormant。

| Model | Runtime relation | State |
|---|---|---|
| `STATELESS_CONDUCTANCE` | 仅当一个完整 barrier block 的同轴两侧都确认是空气时生成 `Air A <-> Gwall <-> Air B` | 不分配 solid node；两个及以上 solid block 不 shortcut |
| `CAPACITIVE_SURFACE` | `Air <-> Gsurface <-> Surface H/C` | 只为暴露 material interface 创建 surface pole，可存放并释放墙体余热 |
| `NATURAL_ROCK` | `Air <-> Surface H/C <-> optional Deep H/C <-> fixed natural boundary` | 只为暴露深岩创建；温度使用 `Trock(y) = Trock(0) - gradient * y`，不直接按 Y 修改空气 |
| `PHASE_RESERVOIR` | `Air <-> Gphase <-> Brick-local latent H` | 同一 `4x4x4` Brick/profile 的暴露候选共享一个潜热账户；不建立逐雪块节点或全局 SnowPatch |

surface/deep pole、phase reservoir 与空气 cell 共用同一个 `ThermalCellArena` 权威和 Page span。拓扑重建按 material block/face/quarter-plane/depth 或 phase Brick/profile key 迁移 `H`；卸载 Page 时一起释放。contact mask 与 proven-air mask 冲突、缺少 profile 或缺少 contact pattern 时保持 unresolved，不从 collision shape 猜测材料。

phase reservoir 只吸收高于 transition temperature 的空气显热，达到 `transitionEnergyJPerUnit` 后预留一个可见转换单位，并通过固定容量 request/ACK bridge 请求主线程修改方块。gameplay 编译器复用旧 recipe 的 `block/ignoreState`、热侧阈值、三相目标与 `will_transit`；较低阈值先执行，replacement `BlockState` 再形成下一阶段，同阈值时保留旧实现的 gas 优先级。主线程再次验证 loaded chunk、Page generation、当前 profile、精确 `4x4x4` material-air microface 和当前 recipe，再应用原目标 `BlockState`，因此冰和高温材料的中间状态链不会被跳过。成功 ACK 才从 reservoir 扣除预留能量并计入 committed latent-energy ledger；`BlockTags.ICE` 在 `FHTags.Biomes.ICE_DO_NOT_SMELT` 群系内暂缓 mutation 且不扣潜热。

旧 `heat_capacity` 仍不是 `J/K`。gameplay profile 暂按 `transitionEnergyJPerUnit = 38,000 J * heat_capacity` 保留相对快慢，并统一使用 `20 W/K` full-face conductance；这是按默认 random tick speed `3`、直接热区绕过 ambient divisor 时约 `68 s` 的 `heat_capacity=1` 基准做的首轮玩法换算，后续实机校准可以调整两个常量，但不能把旧整数重新解释成物理热容。`RESPECT_RANDOM_TICK_SPEED` 当前只保留 `randomTickSpeed=0` 停止自动 mutation 的语义，不按 gamerule 数值缩放能量速度。

只有 `hasDynamicShape=false`、热侧目标有效且保守几何存在非空气 material mask 的状态进入 registry。旧随机转换仅在对应 Page 已安装该候选时跳过热侧分支；冻结、凝结、Page 外状态、无接触 mask 和动态形状继续走旧逻辑。datapack `/reload` 会关闭旧 profile cut，下一次 gameplay query 按新 recipe cache 懒重建，稳定 tick 不扫描 recipe。

## 11. 玩家直接辐射

首次玩家温度查询建立 runtime 后会以有界生产参数调用 `MinecraftThermalInput.tryEnableRadiation`。Campfire 与 Generator 的 radiation profile 都是总功率的 `20%`，分别从冻结的方块内 origin 发出，V1 directional upper bound 为 `1`。`RadiationService.samplePlayer` 返回 feet/torso/head 三个确定 receiver point 的平均 radiant flux，单位为 `W/m²`：

```text
q = Prad / (4 * pi * max(r², rMin²))
```

候选只来自 source 原点附近的 packed section buckets。当前生产参数为 `16` 格范围、`0.1 W/m²` cutoff、`64` 次 candidate visit、top `8`、最多 `24` 条射线和每条 `256` 个 quarter-block DDA step。遮挡通道独立于 collision shape、Air Mesh 和 material contact：loaded-only DDA 直接读取 section 内的 `BlockState`，`hasDynamicShape=true` 按空气，其余状态仅按 `BlockState.canOcclude()` 判断透明或整块不透明；不经过热签名 resolver。DDA 遇到墙返回零贡献，遇到 unloaded/unresolved 或预算耗尽返回较低 confidence/flags，绝不加载 chunk。receiver/source/section witness cache 有固定容量并由独立 section revision 失效；cache hit 不读取 world。

该查询只读取 source 的当前辐射功率，重复调用不改变 `ThermalSourceTimeline`、source accumulator 或 declared-loss ledger。`TemperatureComputation.radiantBodyTemperatureDelta` 将辐照度乘 `0.7 m²` 有效投影面积、`0.8` 吸收率和本轮秒数，再除以 `5,000 J/K` 有效热容，结果进入原五部位体温链；同一 flux 另除以 `6 W/m²/K` 形成 `totalFeelTemp` 的等效辐射体感，不重复增加身体能量。材料吸收/融雪 radiation 没有实现，也不会由 player query 次数间接发生；这些 profile 与身体换算仍需多人 workload 和玩法校准。

## 12. Dormant machine environment shadow

机器不会因为存在 BlockEntity、热网端点或每 tick 运行就自动成为 thermal node。当前冻结角色为：普通机器 `NONE`，显式环境敏感机器 `QUERY_ONLY`，废热设备 `POWER_SOURCE`，自带过热储能的设备才可能是 `MACHINE_CAPACITY`，固定环境条件才是 `BOUNDARY`。Generator 已由第 9 节的 concrete `POWER_SOURCE` adapter 覆盖，不能再注册为 `QUERY_ONLY` 并重复观察。

`MinecraftThermalInput.sampleMachineEnvironment` 只读取调用者明确给出的 receiver point 所在既有 Page/publication。它与 player query 复用同一个 exact mixed-Brick air lookup，但不自动执行 player radiation，也不扫描机器、读取 BlockEntity、创建 Page/Brick/Cell/Interest、修改 `ThermalSourceTimeline`，或写 `HeatEndpoint`/`HeatNetwork`。无 Page、无 air component、geometry/publication stale 或 publication miss 时返回 flags，由现有机器逻辑继续使用自己的 legacy fallback。

当前源码没有一个普通机器实际读取 `WorldTemperature.air/block`，所以本阶段只提供 `observeRegisteredMachineEnvironment` 这一显式 shadow 接口和 `MachineShadowSnapshot`，没有接到无关机器上。将来出现真实环境敏感 consumer 时，应在它原有 cadence 上调用并传入 legacy ambient result；在 calibration gate 通过前仍不得让 shadow result 掌权。

## 13. 当前建模边界

后续增加物理功率、热容或对流前，需要保留或显式迁移以下现有玩法事实：

- 热网 heat 数量决定设备能运行多久，`tempLevel` 独立决定生成热区的温度和半径；
- 多 provider 温度等级取最大值，低温大流量不会稀释高温小流量；
- 热区是无衰减的恒温控制场，不随覆盖体积增加而提高消耗；
- 热网没有管损，距离只影响排序；
- T1 直接生成世界热区，不需要热网 power；
- 玩家设备直接改 effective temperature，与世界产热和热网库存相互独立。

因此，现有 `heat`、`power`、`temperature` 字段不能在不定义迁移规则的情况下合并为同一个守恒量。
