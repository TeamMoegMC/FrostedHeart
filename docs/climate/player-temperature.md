# 玩家体温系统

- Status: `Current`
- Last verified: `2026-08-22`
- Scope: 玩家周围方块采样、环境温度、衣物与介质换热、分部位体温、设备/食物、状态效果、持久化和同步
- Primary code anchors: `TemperatureUpdate`, `TemperatureComputation`, `TemperatureThreadingPool`, `SurroundingTemperatureSimulator`, `CachedBlockTempInfo`, `PlayerTemperatureData`, `BodyPartData`, `ClothData`, `HeatingDeviceContext`, `BodyHeatingCapability`, `FoodTemperatureHandler`

## 1. 数值基准

玩家管线同时使用“相对 37degC 的偏移”和“绝对摄氏度”，不能混用：

| Field/API | Current representation |
|---|---|
| `BodyPartData.temperature` | 相对 `37degC` 的偏移；`0` 表示 `37degC` |
| `PlayerTemperatureData.coreBodyTemp` | 核心部位加权偏移 |
| `TemperatureComputation.environment` 返回值 | 相对 `37degC` 的环境偏移 |
| `HeatingDeviceContext` body/effective/feel | 相对 `37degC` 的偏移 |
| `PlayerTemperatureData.envTemp` | 平滑后的绝对摄氏度 |
| 各部位 `feelTemp` 与 `totalFeelTemp` | 绝对摄氏度 |

`TemperatureCommand` 显示核心体温时会给 `coreBodyTemp` 加 `37`。后续重构必须先固定类型或命名中的基准，否则相同的 `float` 很容易被重复加减 `37`。

## 2. Tick 管线

`ClimateCommonEvents.onPlayerTick` 每 tick 依次调用食物温度、预报、`TemperatureUpdate.updateTemperature` 和 `regulateTemperature`。

服务端 START 阶段的更新流程是：

```text
every player tick
  PlayerTemperatureData.tick() decrements environment-task cooldown
  if cooldown <= 0: tryCommitWork(player)
  every temperatureUpdateIntervalTicks (default 20):
    compute raw environment offset
    compute clothing/effective temperatures
    apply equipment heating
    update five body parts and core
  send FHBodyDataSyncPacket
```

环境采样成功提交后，冷却设为 `envTempUpdateIntervalTicks`，默认 `20`。若异步任务仍在队列，提交失败且冷却保持不大于零，后续 tick 会继续尝试。

服务端 END 阶段每 `temperatureUpdateIntervalTicks` 执行一次 `regulateTemperature`，按部位体温添加效果并按体感温度检查直接冷热伤害。创造、旁观或无敌玩家跳过这部分，但 START 阶段环境/数据同步仍会进入能力逻辑。

`FHServerEvents.serverTick` 在服务端 tick START 收割已完成的异步结果；服务器启动前创建线程池，完全停止后关闭。`envTempThreadCount=0` 时仍走同一模拟器，但在主线程同步计算。

## 3. 周围方块蒙特卡洛采样

`SurroundingTemperatureSimulator` 的构造必须在游戏线程执行。它围绕玩家位置读取一个 `32 x 32 x 32` 方块窗口：四个相邻区块、两个相邻 section，共八个 `PalettedContainer<BlockState>`，另取四张 `MOTION_BLOCKING_NO_LEAVES` 高度图。异步模式复制方块状态容器；高度图仍引用世界对象，没有复制。

默认模拟参数来自 `FHConfig.SERVER.SIMULATION`：

| Parameter | Default | Current role |
|---|---:|---|
| `simulationRange` | `8` | 选择快照原点时的半径，配置上限也是 `8` |
| `simulationDivision` | `10` | 单位球速度方向的格点细分，粒子数近似按立方增长 |
| `simulationParticleInitialSpeed` | `0.4` | 每轮轨迹步长 |
| `num_rounds` | `20` | 每粒子轮数，硬编码 |

速度方向来自整数格点球内除原点外的所有点，归一化后乘初速度。每个粒子从玩家附近同一点出发，在碰撞形状中传播：完整方块使用无分配入射面计算，部分碰撞形状回退到原版 AABB clip，空形状直接穿过。发生碰撞时三分之一概率从全部方向重采样，三分之二从出射半球重采样。

每步读取 `BlockTempData` 作为该方块的热/冷贡献：

- `must_lit=true` 时只在方块具有且启用 `BlockStateProperties.LIT` 时生效；
- `level_divide=true` 时按 `LEVEL`、`LEVEL_COMPOSTER`、`LEVEL_FLOWING` 或 `LEVEL_CAULDRON` 比例缩放；
- 没有数据时贡献为 `0`；
- 动态碰撞形状的方块一律按完整方块处理，不读取方块实体动态热状态。

热贡献按当前粒子的垂直速度加权：热源对向下轨迹的权重降低，冷源对向上轨迹的权重降低，权重范围为 `[0.5,1]`。所有步的贡献求和后除以粒子数，并限制在本次遇到的最冷/最热贡献之间。这是方向偏置的采样器，不是保存热量或流速的对流求解器。

模拟同时以高度图和远处空气命中估算风暴露度：开阔地上方命中记强风，离起点至少 4 格的遮蔽下空气命中记弱风。结果保存为 `windStrength`，`PlayerTemperatureData.getAirOpenness()` 返回：

```text
openness = clamp(windStrength / 20, 0, 1)
```

### 3.1 缓存与任务复用

每个工作线程持有 `ThreadLocal` 的 `cellKind[32768]`、`cellTemp[32768]` 和 `topYCache[1024]`。每次模拟只清零 `cellKind`。`CachedBlockTempInfo` 还按全局 `BlockState` 缓存碰撞形状和温度；数据包配方重载时 `BlockTempData.updateCache` 会整体清空该缓存。空碰撞形状只有在温度也为零时才复用空气单例，所以 fire 一类空碰撞热源仍能贡献温度。

`TemperatureThreadingPool` 记录每名玩家上次提交的维度、精确坐标和 `gameTime >> 6` 种子窗口。三者不变时直接复用旧结果。静止玩家身边的方块变化没有单独失效通知，因此旧结果最多沿用到下一个 64-tick 种子窗口；移动或维度变化会立即改变输入。

## 4. 原始环境温度

`TemperatureComputation.environment` 首先在玩家眼睛位置查询 `WorldTemperature.air`，转为 37 度偏移，再加入上一次异步采样的 `blockTemp`：

```text
E0 = WorldTemperature.air(pos) - 37 + sampledBlockTemp
```

随后加入昼夜与原版天气修正：

```text
relativeTime = sin(dayTime angle)
if skyLight < tempSkyLightThreshold: relativeTime = -1

if Vanilla rain reaches player:
    E0 -= snowTempModifier
    if Vanilla thunder: E0 -= blizzardTempModifier
    dayNightMultiplier = 0.2
else:
    dayNightMultiplier = 1

E = E0 + relativeTime * dayNightTempAmplitude * dayNightMultiplier
```

当前默认 `snowTempModifier=-5`、`blizzardTempModifier=-10`，而代码使用减法，所以默认雨雪分支实际分别增加 `5degC` 和额外 `10degC`，与配置注释中的“降温”意图相反。该分支读取 Vanilla `world.isRaining/isThundering`，不是直接读取 `ClimateType`。

最后有覆盖式介质规则：

| State | Environment offset result | Absolute equivalent |
|---|---:|---:|
| powder snow | `-67` | `-30degC` |
| water | clamp prior offset to `[-37,63]` | `[0,100]degC` |
| on fire | `263` | `300degC` |
| lava | `963` | `1000degC` |

`onFireTempModifier` 配置当前未被此公式读取。

## 5. 风、衣物与环境换热

原始环境偏移先写入玩家 `ENV_TEMPERATURE` 属性。空气中的当前风寒温度使用：

```text
relativeWind = openness * clamp(globalWind,0,100) / 100
v16 = (relativeWind * 70)^0.16
windAffected = 13.12 + 0.6215*E - 11.37*v16 + 0.3965*E*v16
```

代码把 37 度偏移 `E` 直接代入该摄氏温度经验式。水中直接使用 `E`，`SAUNA` 效果下固定使用 `80`。湿度被读取并归一化，但当前风寒分支没有使用 `relativeHumidity`；`TemperatureComputation.feelTemperature` 的湿度公式存在，却未接入主更新路径。

每个部位的环境目标为 `windAffected`，手和脚再减 `5`。衣物导热系数为：

```text
conductivity = 100 / (100 + weightedInsulation)
partEffective = partBody + (partEnvironment - partBody) * conductivity
```

头/手/脚使用两层权重 `[0.4,0.6]`，躯干/腿使用四层权重 `[0.1,0.2,0.3,0.4]`；内层对保温权重更高，外层对 `fluidResist` 权重更高。当前主换热循环只使用 `heatConductivity`，计算出的 `windResist/fluidResist` 没有应用。水中温差乘 `6`，细雪中乘 `2`。

## 6. 体温推进

每个更新周期的环境交换量为：

```text
unit = 1 / heatExchangeTimeConstant
heatExchange = (effective - body) * unit / heatExchangeTempConstant
```

默认 `heatExchangeTimeConstant=1000`、`heatExchangeTempConstant=10`。配置注释把前者定义为秒，但代码没有按 `temperatureUpdateIntervalTicks` 对 `unit` 再缩放；改变更新间隔会改变每真实秒的推进次数。

静止或冲刺时自发热为 `1 * difficulty.heat_unit * unit`，普通步行为 `2 * difficulty.heat_unit * unit`。冲刺分支已注释，因此冲刺当前落入静止值。难度倍率为：

| Difficulty | `heat_unit` |
|---|---:|
| `easy` | `2` |
| `normal` | `1` |
| `hard` | `0.5` |
| `hardcore` | `0` |

头、躯干和腿可通过消耗饱食度/饮水能力进行额外恒温，手脚不能。设备的 `BodyHeatingCapability.tickHeating` 在环境交换后直接修改各部位 effective temperature；手炉、加热背心、加热垫、蒸汽瓶等都走这条接口，不创建世界热区。

五个部位及权重如下：

| Part | Surface area weight | Core weight | Clothing slots |
|---|---:|---:|---:|
| head | `0.10` | `0.10` | `1` |
| torso | `0.45` | `0.50` | `3` |
| hands | `0.05` | `0` | `1` |
| legs | `0.35` | `0.40` | `3` |
| feet | `0.05` | `0` | `1` |

每轮先把头、躯干、腿全部覆盖成三者的核心加权温度，再让腿与脚、躯干与手交换。端部交换把温差限制在 `[-3,3]`，绝对温差超过 `0.1` 时双方各移动温差的 `10%`。

`PlayerTemperatureData.update` 将各部位体感转成绝对摄氏度，以表面积加权并叠加移动体感项得到 `totalFeelTemp`，再对环境温度和总体验温都执行 `20%` 新值、`80%` 旧值的平滑。

## 7. 玩家后果

`regulateTemperature` 使用部位体温偏移而不是总体验温：

- 躯干绝对偏移超过 `1`：添加 `HYPOTHERMIA` 或 `HYPERTHERMIA`；
- 头部绝对偏移超过 `1`：反胃；
- 腿或脚中绝对值更大的一个超过 `1`：缓慢；
- 手部绝对偏移超过 `1`：挖掘疲劳。

偏移区间 `1..2`、`2..3`、`3..5` 对应逐级 amplifier，超过 `5` 后继续随偏移增长。冷热效果各自再按 amplifier 决定伤害频率；amplifier 大于 `8` 时使用 20 点即时伤害。

`TemperatureComputation.burning` 另取除手以外部位的最高/最低绝对体感温度，在 `100/150/200/250degC` 和对应负阈值触发概率性即时伤害。这些阈值使用绝对体感温度，不是体温偏移。

玩家进入水时获得 `WET`：无护甲默认 `100` ticks，有任意护甲时默认乘 `4`。湿润效果本身的具体消费者属于相邻效果逻辑；主换热公式对“在水中”直接乘导热，未读取 WET amplifier。

## 8. 食物与直接体温修改

吃完食物或饮品后，`FoodTemperatureHandler` 从 `ITempAdjustFood`/`FoodTempData` 取得体温偏移和允许的最小/最大核心偏移，乘 `temperatureChangeRate` 后直接把所有部位设为同一值。该配置当前只用于这条食物路径和 tooltip，不控制环境换热速率。

物品 NBT `Temperature` 使用 `FROZEN=0`、`COLD=1`、`HOT=2`。冷食延长食用时间，热食缩短，冻结食物禁止食用。库存温度轮询当前存在一个确定性边界：`UPDATE_CHANCE=1.0`，代码在 `random.nextFloat() < UPDATE_CHANCE` 时提前返回，因此每 200 ticks 的库存冻结/回温循环实际上总是跳过；已有 NBT 状态的食用效果仍会执行。

## 9. 持久化、同步与当前约束

完整玩家能力 NBT 保存：核心/前核心体温、环境/总体验温、采样 `blockTemperature`、`wind_strengh`、可选难度，以及五个部位的衣物库存、体温和体感温度。死亡 clone 先复制能力，再重置温度和采样值；衣物库存不在 `deathResetTemperature` 中清空。

`FHBodyDataSyncPacket` 使用 packet 模式，只同步前核心体温、核心体温、环境温度和总体验温，不同步各部位、衣物、难度、blockTemp 或 openness。包当前在每个服务端玩家 START tick 发送，即使主体计算默认每 20 ticks 才运行一次。

当前实现还存在以下研究约束：

- `updateWhenInsulated` 首次写入环境/体感时没有与普通 `update` 一致地加 `37`，后续平滑分支才加；
- 热端平衡分支的条件顺序使 `deviation > 1` 的中间档不可达；
- frostbite 的 `< -150` 分支使用 `random.nextFloat() < 5`，实际概率为 `100%`；
- 全局 BlockState 缓存无法表达同一 BlockState 下方块实体驱动的动态温度；
- 玩家周围采样没有专门的基准测试夹具，现有测试只锁定空碰撞热源的缓存规则。

这些条目是当前源码事实，不是本文建议的新玩法。
