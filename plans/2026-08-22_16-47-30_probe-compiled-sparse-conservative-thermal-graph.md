# 探针编译式稀疏守恒热图技术架构

- Time: `2026-08-22 16:47:30 +08:00`
- Authors: `Codex; OpenAI; coding agent`
- Status: `ready`
- Scope: `Frosted Heart 气候、世界温度、玩家环境采样、局部热源、地热与未来冷热场的原型和迁移架构`
- Related: [`docs/climate/README.md`](../docs/climate/README.md), [`world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md), [`player-temperature.md`](../docs/climate/player-temperature.md), [`heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), [`data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), `WorldTemperature`, `SurroundingTemperatureSimulator`, `ChunkHeatData`, `GeneratorData`, `HeatNetwork`

> 本文描述尚未实现的目标架构，不是当前系统行为的依据。`ready` 表示可以据此开始纯 Java 原型与影子基准，不表示已经批准替换生产温度系统。

## 1. 决策摘要

目标架构定为：

> **事件驱动的探针编译式稀疏守恒热图**

它由三个职责固定的后端组成：

1. `NaturalBackend`：解析计算自然气候、群系、海拔、空气基线和岩层地热。
2. `AnalyticFieldBackend`：处理 Generator、Boss、管理员命令及兼容旧热区的解析控制场。
3. `LocalTransportBackend`：用稀疏热容节点和守恒传输边处理篝火、墙体、门窗、空气分层、液体和局部热运输。

其中 `LocalTransportBackend` 的几何不是固定网格，也不是每次复制或扫描 `16^3` section。热源、玩家兴趣点和方块变化只会产生有预算的局部探针；探针提出候选节点和边，确定性 DDA 或方块面通透性验证决定拓扑真值。建筑稳定后，系统只更新仍有热不平衡的稀疏热岛，并向查询方发布不可变快照。

该选择针对以下已明确的负载特征：

- 建筑和地形通常比温度查询稳定得多；
- 同一基地会被多个玩家、作物、设备和显示系统反复查询；
- 可以接受局部温度的有限误差和逐步收敛；
- 不能接受穿墙升温、窄竖井完全丢失或主线程无上限扫描；
- 需要真实区分温度、温差、功率、热容和旧 heat unit；
- 未来需要移动 Boss 冷热场，但不能因 Boss 每 tick 移动而重编地形。

这不是数学或工程上的绝对最优声明。它是当前约束下最值得实现和测量的帕累托候选；生产迁移必须由第 24、25 节的验证矩阵和阶段门决定。

## 2. 已验证的当前边界

当前实现不是一个统一热力学模型，而是数套相邻模型：

```text
WorldClimate + biome/dimension/altitude
                  |
                  v
          WorldTemperature.air/block
                  |
                  +--> ChunkHeatData 常值温度控制场
                  |
                  +--> SurroundingTemperatureSimulator 随机粒子采样
                                      |
                                      v
                         PlayerTemperatureData 五部位体温

GeneratorData/HeatEndpoint/HeatNetwork
                  |
                  +--> 任意 heat 缓冲和 tempLevel
                  +--> 部分设备再创建 ChunkHeatData
```

已经确认的约束包括：

- `SurroundingTemperatureSimulator.getBlockTemperatureAndWind` 会反复读取附近方块和碰撞形状；异步模式会复制若干 `PalettedContainer` 以避免线程竞争。
- 先前运行时采样日志给出的代表值约为每次 `6.03 ms`、约 `1,237` 个不同方块访问；该数字不是可复现实验基线，原型阶段必须重新测量。
- `WorldTemperature.air`、`WorldTemperature.block`、玩家环境采样、`ChunkHeatData` 和热网没有共享的能量状态。
- 当前“热上升、冷下沉”只是采样轨迹权重，不保存空气质量、焓或速度。
- `GeneratorData.power`、`HeatEndpoint.heat` 和 `tempLevel` 不能直接解释为 SI 功率、焦耳或摄氏度。
- `TemperatureUpdate` 当前在每个玩家 START tick 发送 `FHBodyDataSyncPacket`，即使主体温度通常没有每 tick 更新。
- `ChunkHeatData.queryAdjust` 查询局部已登记热区；大范围热区通过复制到覆盖区块换取查询局部性。

因此，本架构保留现有玩家体温和公开查询作为兼容边界，但不会把旧字段改名后假装成为物理量。

## 3. 目标

### 3.1 玩法目标

- 保持现有气候、玩家体温、衣物、设备、食物、状态效果和 HUD 的基本玩法。
- 保留并稳定实现玩家与篝火之间的遮挡效果。
- 支持可调功率、热容、导热、局部空气分层和玩法级自然对流。
- 地热必须来自暴露岩石向空气的耦合；竖井应能运输热量，但深处空气不能凭坐标自动升温。
- 支持静态 Generator 控制场、移动 Boss 冷热场、物理热源、物理冷源和瞬时热冲击。
- 为作物、状态转变、城镇、红外显示和未来设备提供统一查询合同。

### 3.2 性能目标

- 常规查询不读取 Minecraft 世界、不等待 worker、不触发区块加载。
- 常规运行不复制或扫描整个 `16^3` section，也不遍历固定半径网格。
- 编译成本与实际探针步数和变化边数相关，而不是与加载体素总体积相关。
- 求解成本与活跃热节点和边相关；达到平衡的热岛可以休眠。
- 查询优先复用消费者到热节点的绑定，正常路径接近 `O(1)`。
- 主线程几何读取、worker 编译、热求解和解析场查询都有独立硬预算。
- 过载只允许增加陈旧度、降低空间精度或减慢收敛，不能突破 tick 预算。

### 3.3 正确性目标

- 已发布的内部传热边必须对两端执行等量能量增减。
- 不确定的路径可以暂时缺失并低估热量，不能发布未经验证的穿墙边。
- 随机或低差异探针不能单独成为拓扑真值。
- 只有满足空气质量守恒的求解器才能把边权称为 `massFlow`。
- 热容节点不能被稳态约化静默删除。
- 时间推进使用游戏 tick；服务器卡顿不能按现实墙钟时间制造额外热量。
- 异步结果发布前必须再次验证几何 revision，禁止旧几何覆盖新世界状态。

## 4. 非目标

- 不求解逐方块 Navier-Stokes、湍流、湿度、真实烟气或完整 CFD。
- 不为所有已加载区块建立逐体素温度场。
- 不使用房间洪水填充作为常规更新路径。
- 第一版不做全局 Schur 补、稠密矩阵约化或无界图粗化。
- 第一版不重新定义现有 `HeatNetwork` 的库存、优先级、`tempLevel` 或设备续航玩法。
- 第一版不要求局部瞬态热量跨服务器重启精确持久化；拓扑缓存也不是存档真值。
- 不保证任意位置立即获得高精度新热场；冷启动和几何变化允许在预算内逐步收敛。
- 不因温度模拟主动加载未加载区块。

## 5. 术语和单位

新核心使用明确的量纲边界。配置数值可以按玩法调校，但不能混用量纲。

| Symbol/API | Meaning | Unit |
|---|---|---|
| `T` | 绝对温度数值 | `degC` |
| `DeltaT` | 温差 | `K`；数值上与摄氏温差相同 |
| `E` | 相对固定参考温度的焓 | `J` |
| `C` | 集中参数热容 | `J/K` |
| `P` | 连续热功率，冷源为负 | `W = J/s` |
| `G` | 导热或等效换热导纳 | `W/K` |
| `mDot` | 质量流量，仅压力守恒模式使用 | `kg/s` |
| `cp` | 定压比热 | `J/(kg*K)` |
| `qRad` | 辐射热流密度 | `W/m^2` |
| `HU` | 旧热网库存量 | 独立 gameplay unit |

游戏时间定义为：

```text
DeltaTimeSeconds = elapsedGameTicks / 20.0
```

即 TPS 下降只让现实时间中的模拟变慢，不会使单个游戏 tick 突然注入更多能量。

节点可使用固定 `Tref = 0 degC` 保存：

```text
E = C * (T - Tref)
T = Tref + E / C
```

允许 `E` 为负；守恒依赖能量差和成对更新，不依赖参考零点。

## 6. 总体架构

```text
                             +--------------------------+
 climate/biome/height ------>| NaturalBackend           |
 exposed rock depth -------->| analytic reservoirs      |
                             +-------------+------------+
                                           |
                                           v
 +-----------------------+      +----------+-----------+
 | AnalyticFieldBackend  |----->| ThermalQueryComposer |
 | Generator/Boss/legacy |      +----------+-----------+
 +-----------+-----------+                 ^
             | physical power              |
             v                             |
 +-----------+-----------------------------------------+
 | LocalTransportBackend                                |
 |                                                      |
 | block/source events                                  |
 |   -> revision + emitter registry                     |
 |   -> budgeted probes / DDA                           |
 |   -> deterministic validation                        |
 |   -> sparse conservative graph                       |
 |   -> active-island solver                            |
 |   -> immutable ThermalQuerySnapshot -----------------+
 +------------------------------------------------------+
                                           |
                                           v
                       player / crop / block / town / HUD
```

后端不会针对同一地区维护多份完整世界模型：

- 自然背景始终由解析公式负责；
- 移动和脚本化控制场始终由解析字段负责；
- 只有需要历史状态、热容和局部运输的部分进入热图。

`LocalTransportBackend` 内可以逐步增强边权算法，但对外仍是一套节点、拓扑和快照合同。

## 7. 查询合同

建议冻结逻辑合同，而不是过早冻结所有 Java 类布局：

```java
record ThermalQuery(
    Vec3 position,
    AABB receiverBounds,
    UUID consumerId,
    ThermalQueryPurpose purpose,
    int maximumAgeTicks
) {}
```

`receiverBounds` 让玩家三点遮挡、作物表面接收和机器控制体使用同一查询入口；没有具体接收体的查询使用包含 `position` 的退化小 AABB。`consumerId` 用于复用绑定和遮挡缓存，不参与物理合成。

```java
record ThermalSample(
    double airTemperatureC,
    double radiantFluxWPerM2,
    Vec3 airVelocityMps,
    ThermalMedium medium,
    double confidence,
    long sampleGameTick,
    int flags
) {}
```

最低实现允许 `airVelocityMps` 为零，并用 `flags` 明确 `BUOYANT_MIXING_ONLY`；没有压力质量流求解时不得返回伪造风速。

查询合成顺序固定为：

```text
natural = NaturalBackend.query(position)

air = localSnapshot.hasValidBinding(position)
    ? localSnapshot.airTemperature(position)
    : natural.airTemperature

air = AnalyticFieldBackend.applyControlPolicy(air, position)
radiation = localDirectRadiation + analyticRadiation

sample = ThermalSample(air, radiation, velocity, medium, confidence, tick, flags)
```

`ANALYTIC_CONTROL` 是目标/控制策略，不与摄氏度简单相加。多个控制场必须显式定义优先级、耦合强度、功率上限或 legacy 合成规则。

旧消费者通过适配器取得原有标量环境温度：

```text
legacyEnvironmentC = ThermalComfortAdapter.toLegacyEnvironment(sample, playerState)
```

玩家五部位体温模型在第一轮迁移中不修改。

## 8. 热源合同

单个热源分量的模式必须互斥并带单位；一个设备或技能 profile 可以由多个明确分量组成：

| Mode | Semantics | Typical use |
|---|---|---|
| `POWER_SOURCE` | 每秒向绑定节点注入有正负号的能量 | 篝火对流、物理 Boss 热/冷、机器废热 |
| `BOUNDARY` | 通过 `G*(Tb-T)` 与无限或大型热库换热，可带功率上限 | 岩层、外界大气、恒温设备 |
| `IMPULSE` | 在明确 tick 一次性增减能量 | 爆炸、法术冲击、倾倒热液体 |
| `ANALYTIC_CONTROL` | 查询时施加目标场，不声称守恒 | 移动 Boss 领域、脚本场 |
| `LEGACY_CONTROL` | 精确保留旧热区聚合语义 | `ChunkHeatData` 迁移期 |
| `BODY_DEVICE` | 直接作用玩家体温，不进入世界热图 | 手炉、加热背心 |

设备 profile 负责从旧 `HU` 和 `tempLevel` 映射到新合同。第一版不能全局声明 `1 HU = N J`；不同设备可以有独立的兼容转换，直到玩法重新平衡。

## 9. 稀疏热图

### 9.1 节点

节点是集中参数热库，不是固定体素：

```text
ThermalNode
  id
  supportBounds
  medium
  role
  volumeEstimate
  heatCapacityC
  enthalpyE
  temperatureT
  naturalBoundaryRef
  topologyRevision
  lastIntegratedTick
```

典型节点包括：

- 热源附近的下层空气；
- 同一区域的上层空气；
- 墙体、地面或液体界面等效热容；
- 门窗、烟囱和竖井转折处的端口空气；
- 外界大气和地下岩层边界；
- 查询或玩法阈值附近需要提高精度的局部节点。

最低 LOD 是一个小型热学控制体，而不是一个八叉树叶：

```text
       Outside boundary
              |
         Upper air
              |
         Lower air ---- Surface mass ---- Rock boundary
```

探针发现明显温度梯度、介质界面、开口、转弯或路径过长时，才增加节点。节点支持范围可以用稀疏 BVH、loose octree 或其他索引查找；索引不参与跨墙插值，也不决定传热真值。

### 9.2 边

热图不是“所有边都单向”的简单有向图，而是带类型的守恒传输图：

```text
ThermalEdge
  fromNode
  toNode
  type
  conductanceG
  permeability
  areaEstimate
  direction/heightDelta
  optionalMassFlow
  geometryWitnesses
  geometryRevision
```

边类型包括：

- `CONDUCTION`：对称导热；
- `BUOYANT_MIXING`：状态相关但成对守恒的浮力混合；
- `AIRFLOW`：可选的质量守恒焓输送；
- `BOUNDARY_EXCHANGE`：与自然或设备边界换热；
- `GATE`：门、活板门、风门等参数化通透边；
- `RADIATIVE_LINK`：确定性遮挡验证后的辐射耦合缓存。

热容节点、动态 gate、空气出口、热源节点和介质界面不得被稳态约化删除。

### 9.3 存储布局

worker 内部优先使用 primitive struct-of-arrays：

```text
nodeTemperature[]
nodeEnthalpy[]
nodeCapacity[]
edgeFrom[] / edgeTo[] / edgeType[] / edgeG[]
islandOffsets[] / adjacencyOffsets[]
```

拓扑版本和查询快照不可变；求解中的热状态只归 worker 所有。查询路径不分配临时集合，不遍历对象图。

## 10. 不扫描 section 的几何编译

### 10.1 方块变化入口

`LevelChunk#setBlockState` 或等价事件只比较旧、新 `ThermalSignature`。结果是可组合 bit flags，不是互斥枚举；例如放置篝火可以同时产生 `SOURCE | MEDIUM | STRUCTURAL`：

```text
old/new block state
       |
       v
ThermalSignature delta flags
  NONE        -> no work
  SOURCE      -> update EmitterRegistry
  GATE        -> update parameterized edge state
  MEDIUM      -> enqueue local medium probes
  STRUCTURAL  -> invalidate witnessed edges and enqueue local probes
```

`ThermalSignature` 缓存于 `BlockState` 对应 profile，至少包含介质、六面通透性、材料热学 profile、热源类型和动态 gate 分类。

这一入口不读取邻近 section，不扫描 `4096` 个方块。

### 10.2 Revision 只做元数据

`GeometryRevisionIndex` 是稀疏 revision 计数表。revision key 可以按小区域或 section 地址编码，但其含义仅是：

```text
coordinate -> integer revision
```

更新 key 不读取该区域内容。是否使用 section 级或更细的 revision bucket 应由失效扇出和内存基准决定；它不是模拟分辨率。

每条边保存其确定性路径经过的 witness key/revision，并在反向索引中登记：

```text
revisionKey -> affectedEdgeIds
```

方块变化时，主线程先增加 revision 并立即禁用受影响边，再把重编任务交给预算队列。worker 发布新边前重新比较全部 witness revision；不匹配则丢弃结果。

### 10.3 编译触发器

只有以下事件产生探针：

- 热源新增、删除、启停或功率 profile 大幅变化；
- 玩家或重要消费者进入没有可靠绑定的新区域；
- 墙体、门窗、流体或介质发生热学变化；
- 旧边失效或查询置信度低于阈值；
- 热误差指标表明当前节点过粗；
- 空闲后重新尝试恢复曾被标记为高频变化的区域。

静态建筑和稳定热源不会持续发射热量包。

### 10.4 探针与确定性验证

探针分工如下：

1. 六个轴向和关键向上/向下方向用于确定性发现墙、顶、地面和竖井。
2. 门、方块面变化和已知开口直接产生确定性局部端口任务。
3. 低差异方向序列用于估计远距离耦合、可见面积和是否需要细化；它比每次独立随机采样更可复现、方差更低。
4. 任意候选边发布前执行 DDA 或逐面通透性验证。

关键规则：

```text
probe hit/miss       -> candidate evidence
deterministic DDA    -> topology truth
revision witnesses   -> continued validity
```

单格孔洞、门和烟囱不能依赖随机探针“恰好命中”。如果当前预算尚未验证路径，边保持不存在；短暂低估优于穿墙高估。

### 10.5 路径拆分

候选长边在以下位置强制拆分：

- 方向发生显著改变；
- 空气、液体、固体等介质改变；
- 穿过门窗、洞口、烟囱口或外界边界；
- 跨越上层/下层空气分界；
- 路径长度达到配置上限；
- 中间体积的热容不能忽略。

因此编译不会把一条长竖井简化成没有中间热容和延迟的一条边。

## 11. 主线程、worker 与发布

Minecraft 世界读取只发生在主线程预算内：

```text
Main thread
  block/source event -> revision + queue
  trace budget       -> immutable TraceResult
                              |
                              v
Worker
  candidate compile -> validation checks -> graph patch
  source integration -> active-island solve
  build immutable ThermalQuerySnapshot
                              |
                              v
Atomic publication
                              |
                              v
Main/player queries: read-only snapshot
```

worker 不持有 `Level`、`LevelChunk`、`PalettedContainer` 或 `BlockEntity` 引用。主线程传递的 `TraceResult` 只包含编译所需的方块热学签名、相交距离、面信息和 revision witness。

查询永不等待 worker。新区块、未完成编译或 worker 过载时，查询使用：

1. 上一份仍有效的局部快照；
2. 若局部边已失效，则使用自然场和解析控制场；
3. 对篝火直接辐射使用仍有效的独立遮挡缓存；
4. 返回降低的 `confidence` 和陈旧标志供诊断，而不是阻塞。

## 12. 能量求解

### 12.1 基本方程

每个节点满足：

```text
dEi/dt = Pi + sum(Qji) + Qboundary
Ti = Tref + Ei/Ci
```

对任意内部边 `i <-> j`：

```text
Ei -= DeltaEij
Ej += DeltaEij
```

因此不考虑外部源和边界时：

```text
sum(Ei) = constant
```

### 12.2 精确双节点松弛

对常数导纳 `G` 的两个热容节点，使用解析边更新而不是不稳定的显式欧拉：

```text
Ceq = Ci*Cj/(Ci+Cj)
lambda = G*(1/Ci + 1/Cj)
DeltaEij = Ceq * (1-exp(-lambda*dt)) * (Ti-Tj)
```

这对单条边无条件稳定并严格守恒。多边图采用确定性交替边序或分裂积分；离线参考求解器用于测量分裂误差。

对固定边界温度 `Tb`：

```text
Ti(t+dt) = Tb + (Ti(t)-Tb) * exp(-G*dt/Ci)
```

对单节点、常功率 `P` 和单固定边界，还可以解析快进：

```text
Teq = Tb + P/G
Ti(t+dt) = Teq + (Ti(t)-Teq) * exp(-G*dt/Ci)
```

这允许稳定热岛长时间休眠，而不按每 tick 重复积分。

### 12.3 活跃热岛

图按连通分量形成 thermal island。节点优先级使用预计温度误差，而不是单纯 FIFO/FILO：

```text
priority_i ~= abs(netPower_i) * pendingDt / Ci
```

调度器同时使用 deadline 和 aging，防止低功率节点永久饥饿。以下事件唤醒热岛：

- 源功率、边界温度或控制场负载变化；
- 拓扑、gate、介质或风区间变化；
- 玩家查询需要更高新鲜度；
- 到达根据时间常数计算的下一误差 deadline。

达到平衡且外部输入不变的热岛休眠。延迟处理的功率必须通过 `lastIntegratedTick` 或 pending energy 精确记账，不能丢失或积分两次。

## 13. 对流模型

### 13.1 第一生产级：守恒浮力混合

第一版不伪造质量流，而使用方向和状态相关的等效混合导纳：

```text
DeltaTunstable = max(0, Tlower - Tupper)
Ggeom = kMix * permeability * area/distance

Gmix = clamp(
    Ggeom
    * (1 + buoyancyScale * DeltaTunstable/DeltaTref)
    * windFactor,
    Gmin,
    Gmax
)

Qmix = Gmix * (Tlower - Tupper)
```

其中：

- `kMix` 单位为 `W/(m*K)`，`area` 为 `m^2`，`distance` 为 `m`；
- `Ggeom`, `Gmin`, `Gmax` 单位为 `W/K`；
- `permeability` 和 `windFactor` 无量纲；
- `DeltaTref` 是配置的温差标度，单位 `K`。

热下冷上时混合增强，热上冷下时混合减弱。每次更新仍对两端等量增减能量，因此它是能量守恒的玩法级自然对流，但不输出 `mDot`。

### 13.2 可选增强：压力修正质量流

只有基准证明烟囱、贯通通风和风压玩法需要更高精度时，才在同一空气子图上增加压力修正。它不是另一个世界后端。

令 `B` 为节点-边关联矩阵，`H` 为边水力导纳，`mStar` 为风压和浮力产生的暂定流量，`s` 为外界通风口质量源项：

```text
L = B*H*transpose(B)
L*p = B*mStar - s
m = mStar - H*transpose(B)*p
```

发布前必须满足：

```text
B*m ~= s
```

内部节点的 `s = 0`。焓输送使用迎风状态：

```text
Qadvect = mDot * h(Tupwind)
```

固定迭代预算未收敛时继续使用上一份已守恒流场并衰减动态浮力项，不能发布质量不守恒的新流场。

## 14. 篝火模型

篝火功率按 profile 分成三个通道：

```text
Pfuel
  +--> Pradiative  -> player/object direct radiation
  +--> Pconvective -> receiving air node
  +--> Pconductive -> contacted ground/surface node
```

三个比例之和必须为 `1`，除非 profile 显式记录未模拟损失。

### 14.1 玩家遮挡

直接辐射不依赖空气图插值。候选篝火由稀疏源索引给出，然后对玩家脚部、躯干和头部代表点执行三条确定性 DDA 射线：

```text
viewFactor = weightedVisibleRays / totalRayWeight
receivedRadiation = Pradiative * distanceFalloff * viewFactor
```

结果按以下键缓存：

```text
(sourceId/sourceClusterId, playerQueryBinding, geometryWitnessRevisions)
```

玩家跨越绑定范围、源变化或 witness revision 变化时才重算。正常体温查询只读取缓存结果。

### 14.2 大量篝火

- 进入同一空气节点的对流功率可以精确相加为一个 `Pconvective`。
- 接触同一表面节点的传导功率可以精确相加。
- 辐射只有在源共享接收区域和经过验证的可见性类别时才能聚合。
- 不同墙后、不同窗口或不同遮挡 witness 的源不能用单个 Barnes-Hut 远场项替代。
- 新增一百个稳定篝火可能增加一次性编译工作，但稳定后的每 tick 热求解不应增加一百条独立源积分。

## 15. 地热竖井

地热由自然岩石边界和局部运输共同产生：

```text
NaturalBackend.rockTemperature(depth, dimension, climate)
                           |
                           v
exposed rock boundary -- conduction --> lower shaft air
                                             |
                                      buoyant mixing/airflow
                                             |
                                        upper shaft air
                                             |
                                        outside leakage
```

编译器的向下、向上和侧壁探针估算暴露岩石面积、竖井截面、层间连接和顶部泄漏。岩石以 `BOUNDARY` 节点提供温度、导纳和可选最大功率。

必须通过的行为：

- 封闭并暴露深层岩壁的竖井可以逐渐积热；
- 打开顶部后热空气可以向上运输并增加外界损失；
- 仅仅站在低 Y 空气中不会无条件得到岩石温度；
- 一格宽竖井不能因低分辨率节点或随机探针而完全消失；
- 未加载的更深区块不能被强制加载来完成地热查询。

## 16. Generator、Boss 与旧系统

### 16.1 Generator

迁移期保留两条明确路径：

- `LEGACY_CONTROL` 精确复现当前半径、温度值和重叠规则；
- 新物理设备 profile 将燃料或 HU 转换成有上限的 `POWER_SOURCE` 或 `BOUNDARY`。

二者不能同时对同一设备生效，避免重复加热。是否迁移某类 Generator 由 feature flag 和玩法基准决定。

### 16.2 Boss

Boss 支持两种语义：

- `ANALYTIC_CONTROL`：移动目标温度场，更新空间索引，不重编地形，适合必须立即生效的技能领域。
- `POWER_SOURCE`：向范围内已存在空气/表面节点注入有正负号的功率，适合会积热、被墙体和热容影响的物理冷热源。

Boss 移动只更新解析场 AABB 或节点绑定并唤醒受影响热岛，不触发 section 扫描。一个技能可以同时包含即时控制成分和较慢物理余热，但两部分功率和合成顺序必须在 profile 中明确。

### 16.3 旧热网

`HeatEndpoint` 和 `HeatNetwork` 第一阶段保持现有 gameplay unit。世界热学系统只通过设备适配器观察：

```text
available HU + tempLevel + device profile
                  |
                  v
POWER_SOURCE or BOUNDARY or LEGACY_CONTROL
```

热网重构不是本计划的前置条件。

## 17. 查询、索引和生命周期

### 17.1 玩家查询绑定

每个高频消费者保存：

```text
ThermalQueryBinding
  node/island id
  support bounds
  snapshot epoch
  geometry revision summary
  last position bucket
```

只要消费者仍在支持范围且 epoch 有效，查询直接读取 primitive snapshot 数组。绑定失效时异步请求新验证；等待期间使用旧有效结果或自然回退。

### 17.2 解析场索引

静态和移动解析场使用动态 AABB tree、BVH 或经过基准选择的等价稀疏索引：

- 查询目标为 `O(log F + k)`；
- 移动 Boss 更新不复制到覆盖区块；
- 查询不加载范围内区块；
- 字段数量很少时允许线性小数组快路径，避免索引常数成本。

### 17.3 Chunk 加载和卸载

- chunk load 从已知 block entity/source registry 恢复显式热源，不扫描所有普通方块。
- 探针访问到普通热源方块时可以惰性登记；之后由方块变化事件维护。
- chunk unload 立即关闭跨边界编译任务并禁用依赖其 witness 的边。
- 未加载方向视为受配置约束的外部/未知边界，不继续追踪。
- 第一版拓扑和瞬态局部焓可以在无兴趣 TTL 后丢弃并回到自然温度；这与当前无局部热容持久化的玩法兼容。

### 17.4 数据包 reload

材料、热源或介质 profile reload 时增加独立 `thermalProfileGeneration`：

- 旧图立即停止接受使用过期 profile 的新边；
- 受影响源和边按预算重编；
- 查询使用旧兼容快照或自然回退；
- 新旧 generation 不能混合发布为一份快照。

## 18. 预算、退化和可观测性

预算优先按工作量计数，时间只作为第二道硬保护：

| Budget | Counted work |
|---|---|
| `traceBudget` | DDA voxel steps、碰撞/通透检查 |
| `compileBudget` | 候选节点、候选边、witness 验证 |
| `solveBudget` | edge relaxations、island updates |
| `airflowBudget` | 压力迭代和 residual checks |
| `queryBudget` | 解析场候选、直接辐射源和绑定刷新 |
| `publishBudget` | 快照复制字节数或节点/边数 |

预算耗尽时：

- 当前 tick 停止对应昂贵工作；
- 未完成任务保留 generation 和 deadline；
- scheduler 使用优先级加 aging，不使用纯 FILO；
- 已失效边保持禁用，不能因为“旧快照更暖”而继续穿墙；
- 未处理功率按游戏 tick 精确累计；
- 查询报告较低 confidence 和 snapshot age；
- TPS 优先于即时收敛。

必须提供独立指标：

```text
trace voxel steps/tick
compiled/rejected/stale edges
active/sleeping islands
edge relaxations/tick
snapshot age p50/p95/p99
query binding hit rate
main-thread thermal time p50/p95/p99
worker CPU and queue depth
energy conservation residual
optional airflow mass residual
```

不能再用一个总“温度 tick 时间”掩盖玩家查询、世界 random tick、热区维护、热网和 worker 的不同扩展维度。

## 19. 失败模式和安全退化

| Failure | Required behavior | Required validation |
|---|---|---|
| 墙在 trace 后、publish 前被放置 | revision 比较拒绝新边 | 并发 publication test |
| 墙被移除但原图没有边 | 方块事件种下局部端口探针；暂时低估 | opening recovery test |
| worker 长时间落后 | 查询旧有效快照/自然回退，主线程不等待 | forced backlog test |
| 活塞或流体持续变化 | 禁用不确定边，降低重编频率并公平重试 | volatile-region benchmark |
| 区块在 trace 中卸载 | 终止路径，不加载区块，不发布跨界边 | unload race test |
| 热源删除后旧任务仍在队列 | source generation 不匹配时丢弃注入 | source lifecycle test |
| 功率任务延迟多个 tick | 能量不丢失、不重复、按游戏 tick 积分 | pending-energy property test |
| 极小热容或极大导纳 | 指数更新保持有限；profile validation 拒绝非法值 | numerical boundary test |
| 压力流未收敛 | 不发布新流场，复用并衰减旧守恒流场 | airflow convergence test |
| 解析场大量重叠 | 查询预算截断必须使用确定的优先/上界策略 | field-overlap benchmark |
| snapshot 与绑定 epoch 不一致 | 绑定 miss 并安全回退 | atomic snapshot test |

任何“无测试、无错误处理且玩家只能静默看到错误温度”的路径都是生产迁移阻断项。

## 20. 性能模型

设：

- `Vloaded`：已加载体素数；
- `S`：活跃源数；
- `N`, `E`：已编译节点和边数；
- `Ea`：当前热不平衡涉及的活跃边；
- `Rchange`：热学相关方块变化率；
- `Q`：查询数；
- `Fnear`：查询附近解析场候选数。

期望复杂度为：

```text
normal memory       O(N + E + S), not O(Vloaded)
block mutation      O(1 + affectedWitnessEdges)
topology compile    O(budgeted traced voxel steps)
steady solve        approximately O(Ea), Ea -> 0 near equilibrium
cached query        O(1)
field query         O(log F + Fnear)
```

最坏情况仍可能出现大量 source、edge 或结构变化，因此架构不依赖平均复杂度保护 TPS；所有高成本路径必须有硬预算。

与当前每玩家重复蒙特卡洛采样相比，主要收益来自：

- 几何只在变化或首次需要时探索；
- 多个消费者共享同一传输拓扑和温度状态；
- 多个进入同一节点的功率源可以精确聚合；
- 稳态热岛休眠或解析快进；
- 玩家查询不再复制 section 或调用碰撞形状。

## 21. 备选方案结论

| Architecture | Strength | Rejection/retained role |
|---|---|---|
| 固定 `4^3` 或更粗网格 | 实现直接、守恒方便 | 固定成本高，窄墙和竖井误差大；不作为核心 |
| 每 `16^3` section 有限体积 | 局部物理清楚 | 重编和内存按 4096 体素付费；不采用 |
| 纯稀疏八叉树插值 | 查询与 LOD 方便 | 可能跨墙插值，没有传输和热容语义；仅可作索引 |
| 纯解析热泡 | TPS 极高 | 多房间、开口和竖井能力弱；作为最低 LOD |
| 持续热量包 | 工作量可设预算 | 稳定建筑重复探索且有噪声；不作为运行时求解器 |
| 热量包直接决定图 | 稀疏、直观 | 会漏窄孔并可能发布假连接；探针只保留候选发现职责 |
| 全局压力/CFD | 物理精度高 | 对玩法和服务器成本不成比例；压力修正仅作稀疏可选增强 |
| 本计划 | 成本随兴趣、变化和活跃梯度增长 | 冷启动近似、实现和失效逻辑更复杂；进入原型验证 |

## 22. 实施边界和建议包结构

具体类名应在原型后确认，建议职责边界为：

```text
content/climate/thermal/
  api/          ThermalSample, ThermalSource, source modes, units
  natural/      existing WorldTemperature-compatible natural formulas
  field/        analytic fields and sparse field index
  geometry/     signatures, revision index, trace commands/results
  graph/        nodes, typed edges, immutable topology snapshots
  solver/       energy integration, islands, optional airflow
  runtime/      emitter registry, budgets, scheduling, publication
  compat/       WorldTemperature/player/ChunkHeatData/Generator adapters
  benchmark/    pure-Java scenarios and reference solver fixtures
```

需要在复杂实现处保留 ASCII 数据流注释，尤其是：

- revision 增加、边禁用、worker publication 的竞态顺序；
- pending energy 的恰好一次积分；
- source mode 到查询/热图的分流；
- 可选压力修正的矩阵符号和发布条件。

## 23. 分阶段实施

### Phase 0：契约、基线和参考模型

- 冻结 `ThermalSample`、单位、source mode 和守恒不变量。
- 为现有玩家环境温度、篝火遮挡、地热和 Generator 输出建立兼容夹具。
- 建立纯 Java 高精度小规模有限体积参考求解器，不接入服务器 tick。
- 建立可重复 benchmark，替代会话中的单次 `6.03 ms` 数据。

### Phase 1：兼容外壳与快照查询

- 建立三个后端和 `ThermalQueryComposer`。
- `WorldTemperature.air/block` 继续返回旧结果。
- 引入不可变 snapshot 和玩家 query binding，但先装载旧采样结果。
- 将 `FHBodyDataSyncPacket` 改为数值变化/固定 heartbeat 时发送，单独提交并验证客户端表现；它是独立 TPS 优化，不是热图原型的阻断前置。

### Phase 2：热源注册、revision 与篝火遮挡

- 实现 `ThermalSignature`、`EmitterRegistry`、`GeometryRevisionIndex`。
- 实现有预算的主线程 DDA 和三射线辐射缓存。
- 验证放墙、拆墙、门开关、区块卸载和 source generation race。
- 此阶段先不引入热容图，也应能替换最昂贵的重复遮挡查询。

### Phase 3：最小守恒热图

- 实现 lower air、upper air、surface、outside 的最低 LOD。
- 实现 `P`, `C`, `E`, `G`、指数边松弛、边界换热和活跃热岛。
- 将篝火对流/传导通道接入图，旧玩家结果保持生产生效，新结果仅记录。

### Phase 4：探针拓扑编译

- 实现候选节点/边、路径拆分、确定性验证和 witness invalidation。
- 加入门、单格孔洞、烟囱、地热竖井和液体界面场景。
- 高频动态区域只降低精度和恢复速度，不能突破预算。

### Phase 5：影子模式和校准

- 同时计算旧、新结果，只有旧结果驱动玩法。
- 记录温度阈值附近误差、快照陈旧度、编译恢复时间、能量残差和 CPU。
- 调校材料 profile、节点体积估算、功率和热容，而不是修改玩家体温公式掩盖误差。

### Phase 6：Generator、Boss 与消费者迁移

- 先接入 `LEGACY_CONTROL`，再对选定设备启用物理 source profile。
- 验证移动 Boss 解析场、正负功率竞争和离开范围后的余热。
- 按玩家、作物/状态转变、城镇、红外显示的顺序迁移消费者。

### Phase 7：生产切换和旧核心退役

- feature flag 分世界或分消费者启用，保留即时回退路径。
- 通过全部阶段门后停止 `SurroundingTemperatureSimulator` 主路径和 `ChunkHeatData` 大范围复制。
- 更新 `docs/climate/` 为真实新行为，计划状态改为 `completed` 并记录 outcome。

压力修正质量流不是 Phase 0-7 的生产切换前置条件；只有守恒浮力混合无法满足已批准玩法指标时另立阶段。

## 24. 验证矩阵

### 24.1 数学和属性测试

- 封闭无源图在任意边更新顺序下总能量只存在浮点容差误差。
- 正功率、负功率和 impulse 的积分不丢失、不重复。
- `C <= 0`、非有限温度、负导纳和非法 source profile 被拒绝。
- 指数更新在极大 `G*dt/C` 时仍有限并趋于平衡。
- 若启用 airflow，所有内部空气节点满足质量 residual 上限。

### 24.2 几何和玩法场景

| Scenario | Required observation |
|---|---|
| 篝火与玩家之间无墙/实墙/半遮挡 | 三射线结果方向正确，墙后无假辐射 |
| 墙在编译中放置 | 旧候选边不能发布 |
| 拆除一格墙 | 在预算内发现新路径，等待期只低估 |
| 门快速开关 | 参数边更新，不重复全区域编译 |
| 封闭房间 | 升温受功率和热容限制，停止加热后逐渐冷却 |
| 烟囱 | 上层先升温，开放顶部增加泄漏 |
| 基岩/深层竖井 | 热来自暴露岩石耦合，不是空气 Y 坐标捷径 |
| 水或高热容介质 | 升降温明显慢于同体积空气 |
| 百个篝火集中/分散 | 稳态 solve 不按源数线性增加独立积分 |
| Generator 与 Boss 重叠 | source mode 和控制优先级无双重计入 |
| Boss 高速移动 | 不重编地形，旧绑定及时失效 |
| 活塞/流体高频变化 | TPS 受预算保护，无穿墙旧边 |
| 区块边界和卸载 | 不加载区块，不发布跨未知区边 |

### 24.3 多人性能场景

- 1、10、50、100 名玩家集中在同一稳定基地；
- 相同人数分散在独立基地；
- 大量玩家共享同一热岛和各自独立热岛；
- 大量热源少查询、大量查询少热源、同时大量变化；
- 冷启动进入基地、稳定运行、拆墙恢复和持续动态四个阶段。

每个场景比较：

```text
current implementation
analytic-only compatibility shell
probe-compiled sparse graph
high-accuracy offline reference where applicable
```

记录：

- 主线程 p50/p95/p99 和最大温度系统时间；
- worker CPU、队列深度和完成延迟；
- 分配率、GC 压力和热图内存；
- 玩家 query binding 命中率；
- 几何变化后的恢复时间；
- 玩法阈值附近温度误差；
- 内部能量和可选质量守恒误差。

## 25. 生产阶段门

只有同时满足以下条件才能替换现有玩家环境主路径：

1. 常规缓存命中查询没有 Minecraft 世界读取、碰撞查询或 section copy。
2. 所有主线程昂贵操作都有可测试的硬预算，且过载测试不突破预算。
3. 查询和 worker 都不强制加载区块。
4. 未经验证或 revision 过期的边不能发布或继续传热。
5. 封闭图内部能量误差在已声明浮点容差内。
6. 篝火遮挡、门、单格孔洞、烟囱和地热竖井通过行为测试。
7. 高频变化只增加陈旧度和恢复时间，不形成主线程尖峰。
8. 玩家体温关键阈值附近的新旧结果差异经过策划接受，而不只比较全场均方误差。
9. 目标多人负载下主线程 p99 明显优于当前基线，且 worker CPU 和内存处于服务器预算内。
10. feature flag 可以在不损坏存档的情况下回退旧路径。

阶段门不预设未经测量的绝对毫秒数字。Phase 0 必须先固定目标硬件、玩家数、视距、模组列表和当前基线，再给出数值预算。

## 26. 优点和代价

### 优点

- 稳态计算量由活跃热变化决定，而不是由玩家数量乘固定采样体积决定。
- 不需要重复 `get section`、复制 `PalettedContainer` 或扫描 `4096` 个方块。
- 遮挡拓扑确定、可复现，并可通过 revision 安全失效。
- 功率、热容、导热和冷源拥有一致的量纲与守恒边界。
- 自然场、Boss 控制场和局部热运输不会被塞进同一种求解器。
- 同一拓扑可逐步增加空气质量流，而无需推翻查询 API 或热图状态。
- 符合 Minecraft 世界“结构稳定、查询重复、变化局部”的典型特征。

### 代价

- 初次进入和拆墙后不是立即获得完整热场，必须接受有预算的最终收敛。
- 房间体积、界面面积和远距离耦合来自稀疏估计，不能达到逐体素参考精度。
- revision witness、异步 publication、source generation 和 pending energy 需要严格竞态测试。
- 错误的节点合并或体积估算会影响热容和响应时间，即使能量形式上守恒。
- 大量持续动态结构会使系统长期处于低精度回退状态。
- 在真实多人基准完成前，不能证明它优于所有更简单的解析方案。

## 27. 待原型回答的问题

以下问题不改变总体架构，但必须由数据确定：

1. revision bucket 使用 section 级、较小空间 key 还是混合 witness，才能平衡内存和失效扇出？
2. 最低 LOD 的空气体积和墙体等效热容如何从探针结果估算，才能保持现有升温节奏？
3. 边的最大长度和三射线缓存移动阈值应取多少？
4. 哪些普通非 block-entity 热源需要 chunk load 索引，哪些允许由探针惰性发现？
5. 解析场数量多到什么程度后，AABB tree 才优于小数组线性查询？
6. 第一版局部焓在 chunk unload 后直接丢弃、短期内存保留还是保存粗粒度状态？
7. 守恒浮力混合在哪些玩法场景下不足以替代压力质量流？
8. `ThermalSample` 的 radiant flux 如何映射回当前玩家 environment/effective temperature，才能保持玩法阈值？

推荐先用基准和影子模式回答，不在生产代码中提前加入复杂自适应策略。

## 28. 文档影响

本文留在 `plans/`，直到实现完成前不得作为现行行为引用。

实施每个阶段时应同步维护：

- `docs/climate/world-climate-and-temperature.md`：自然后端、局部场合成和地热边界；
- `docs/climate/player-temperature.md`：环境查询、辐射、异步时序和兼容适配；
- `docs/climate/heat-production-and-network.md`：source mode、功率、热容与旧 HU 映射；
- `docs/climate/data-lifecycle-and-integration.md`：revision、线程、快照、配置、网络和性能路径；
- `docs/climate/README.md`：新系统地图和阅读入口。

## 29. Outcome

尚未实施。下一步是完成 Phase 0 的契约、纯 Java 参考模型和可重复性能基线；任何 Minecraft 生产路径替换都必须等待原型阶段门。
