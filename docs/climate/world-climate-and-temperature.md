# 世界气候与环境温度

- Status: `Current`
- Last verified: `2026-08-26`
- Scope: 逻辑气候时钟、长期事件、局部白幕、自然/mesh/analytic 温度合成、方块状态消费者
- Primary code anchors: `WorldClockSource`, `WorldClimate`, `ClimateEventModel`, `ClimateEventTrack`, `InterpolationClimateEvent`, `WhiteCurtainDescriptor`, `WhiteCurtainFieldModel`, `WhiteCurtainInfo`, `WorldTemperature`, `BlockTemperatureModel`, `MinecraftThermalInput.AnalyticField`, `MinecraftThermalInput.gameplayPassiveEnvironment`, `MinecraftThermalInput.gameplayCropEnvironment`, `TownThermalProjection`, `MinecraftThermalInput.gameplayTownEnvironment`

本文只描述当前源码行为。所有温度若无特别说明均为摄氏度；“修正”表示摄氏度增量。

## 1. 气候状态与更新节奏

`ClimateCommonEvents.attachToWorld` 给所有 `dimensionType().hasFixedTime() == false` 的维度挂载 `FHCapabilities.CLIMATE_DATA`。初始预设事件只在主世界创建，但能力本身不限于主世界。

`WorldClockSource` 从 Minecraft `dayTime` 派生逻辑秒数，并显式吸收 `/time` 与睡眠跳时：

```text
1 logical second = 20 game ticks
1 climate hour   = 50 logical seconds = 1000 game ticks
1 climate day    = 24 climate hours    = 1200 logical seconds = 24000 game ticks
1 climate month  = 30 climate days
```

每个服务端维度 tick 的 START 阶段先调用 `WorldClimate.updateClock`。`dayTime` 停止时逻辑时钟也停止；时间向后跳时，`WorldClockSource.elapsedDayTimeTicks` 把它解释为跨到下一日相同日内时刻。每 20 game ticks 调用一次 `updateCache` 和 `trimTempEventStream`：每次都会检查并移除已经完全结束的白幕；逻辑小时变化时切换小时缓存并更新预报。小时变化或 `WorldClockSource.update` 检出的 `>20 dayTime ticks` 大跳都会向该维度玩家发送一次现有 `FHClimatePacket`，供客户端按同源 `clockDayTime` 重锚。

`WorldClimate.DAY_CACHE_LENGTH` 为 `8`。内部 `dailyTempData` 保留前一日、当前日和未来日队列，并按 `populateDays` 的 `size <= DAY_CACHE_LENGTH` 条件填充。温度在同一气候小时内不插值更新，查询值整小时保持不变。

## 2. 长期事件模型

默认有三条独立 `ClimateEventTrack`。每条轨道串接冷期或暖期，再接平静期；事件温度由 `ClimateEventModel.temperatureAt` 使用端点导数为零的三次 Hermite 曲线：

```text
u = (t - t0) / (t1 - t0)
T(t) = T0 * (1 + 2u) * (1-u)^2 + T1 * (1 + 2(1-u)) * u^2
```

默认事件参数的唯一策划默认来源是 `TownModelParameters.Defaults`，运行时由 `FHConfig.SERVER.CLIMATE.eventModelParameters()` 读取：

| Parameter | Default | Current meaning |
|---|---:|---|
| `trackCount` | `3` | 独立事件轨道数 |
| `eventChoiceRollBound` | `10` | 冷暖选择随机数上界 |
| `warmEventMinimumRollInclusive` | `8` | 随机结果达到该值时生成暖期 |
| `openingWarmRollBonus` | `3` | 开局偏暖加值 |
| `openingBiasThroughDayInclusive` | `15` | 第 0 至 15 日应用开局加值 |
| cold bottoms | `-40,-30,-20,-10` | 极端、严重、强、普通冷谷 |
| cold weights | `1,2,3,4` | 对应冷谷权重 |
| `eventMinimumDays` | `2` | 事件最短时长 |
| `eventMaximumDaysExclusive` | `7` | 事件时长排他上界 |
| padding hours | `[8,24)` | 事件开始到首个峰值的随机范围 |
| calm days | `[2,7)` | 平静期时长范围 |
| `coldPreludePeakCelsius` | `-5` | 冷期开始后的短暂峰值 |
| `warmPeakCelsius` | `8` | 暖期峰值 |
| event noise sigma | `1` | 冷谷/峰值高斯扰动尺度 |
| `warmNoiseScale` | `2` | 暖期噪声附加倍率 |

开局期之后，默认随机数 `0..9` 中 `8,9` 为暖期，即暖期基础概率 `20%`；开局加 `3` 后，原随机数 `5..9` 为暖期，即 `50%`。冷期结束前约四分之一位置达到冷谷；暖期约在事件中点达到峰值。

每个小时聚合轨道时，`WorldClimate.generateDay` 分别取所有轨道的最大正贡献 `max` 和最小负贡献 `min`，最终气候温度为：

```text
C_hour = max + min
```

这不是轨道总和。天气类型通过 `ClimateType.merge` 按内部优先级合并。普通事件温度不高于 `-13` 时为雪；显式 blizzard 事件不高于 `-30` 时为暴风雪，`[-30,-13]` 区间为 `SNOW_BLIZZARD`。

新世界默认由 `addInitTempEvent` 写入一段显式开局事件：第一条轨道从暖峰 `8` 进入 `-50` 冷谷，其余轨道以 `EmptyClimateEvent` 对齐。管理员命令也可以追加普通暖/冷事件或旧式 blizzard 事件。

## 3. 湿度、风与局部白幕

每日湿度由前一日值叠加标准差 `5` 的高斯随机游走，并限制在 `[0,50]`。`dayNoise` 同样在 `[-5,5]` 随机游走，但 `generateDay` 当前明确没有把它加到小时温度。

风速按天气获得基础值：`NONE=0`、`SUN=30`、`CLOUDY=40`、`SNOW=50`、`BLIZZARD=70`、`SNOW_BLIZZARD=90`。随后执行：

```text
wind = clamp(0.5 * baseWind + 0.5 * previousWind + gaussian(0,3), 0, 100)
```

这里使用新建的 `java.util.Random`，不使用世界种子，因此风噪声不具备固定种子复现性。生成一日的 24 个小时都传入上一日最后一小时风速，而不是逐小时把本日上一小时结果继续传递。

`WhiteCurtainDescriptor` 保存矩形走廊、水平传播方向和局部 `ClimateEvent`；`WhiteCurtainInfo` 是保留预报缓存和旧 Codec 外形的运行时包装器。`WhiteCurtainFieldModel` 统一计算四方向传播，延迟为每区块 `6` 个气候小时，即 `300 logical seconds/chunk`。查询某区块时：

```text
local climate type = merge(global type, white-curtain type)
local climate temp = min(global temp, white-curtain temp)
```

相交白幕不会被创建，含末端区块也不能与另一走廊共享。白幕在事件结束并完全越过影响矩形后移除。区块结果继续按整气候小时采样；`WorldClimate.whitecurtainCache` 会在该区块下一玩法相位精确过期，并在创建、清除、载入和自然移除时通过 generation 立即失效。`getTemp(BlockPos)` 的 cache hit 直接从 block 坐标计算 packed chunk key，只在 miss 时构造 `ChunkPos`。客户端连续视觉场不反向参与这里的温度、作物或玩法查询，具体见 [weather-rendering.md](weather-rendering.md)。

## 4. 世界温度分层

`WorldTemperature` 暴露四个基础来源：

| Symbol | Source | Fallback/default |
|---|---|---:|
| `D` | `WorldTempData` recipe，按维度 ID | `overworldBaselineCelsius = -10` |
| `B` | `BiomeTempData` recipe，按群系 ID | `0` |
| `A(y)` | `WorldTemperature.altitude` 硬编码分段 | 见下式 |
| `C(pos)` | `WorldClimate.getTemp` 当前小时/区块 | 无能力时 `0` |

海拔修正当前是只为主世界高度写死的分段函数：

```text
y > 240       : A = -2.0 * (clamp(y,240,320) - 240)
63 < y <= 240 : A = -0.1 * (clamp(y,63,320) - 63)
0 < y <= 63   : A = 0
-55 < y <= 0  : A = 0.1 * (0 - clamp(y,-55,0))
y <= -55      : A = 20.0 * (-55 - clamp(y,-64,-55))
outside [-64,320] : A = 0
```

这些分段在 `y=240/241` 和 `y=-54/-55` 附近并不连续；文档按源码保留这一事实，不将其平滑化。

`WorldTemperature.base` 是未加局部热区的简单和：

```text
T_base = D + B + A + C
```

## 5. 方块温度

气候对方块的影响比例由 `BlockTemperatureModel.climateBlockAffection` 给出。默认 `stoneInterfaceLevel=0`、`seaLevel=63`、`blockMaximumClimateAffection=0.5`：

```text
alpha_block(y) = 0                         , y <= stoneInterface
               = alpha_max * (y-stone)/(sea-stone), stone < y <= sea
               = alpha_max                 , y > sea

T_natural = D + B + A + alpha_block * C
```

`WorldTemperature.naturalBlock` 返回上述自然值。`WorldTemperature.block` 随后调用
`MinecraftThermalInput.gameplayPassiveEnvironment`：revision-valid mesh publication 命中时以 published
air 替换局部自然值；否则保留 natural backend；最后应用 analytic control fields。该 passive 查询不会
创建 Page，也不会加载区块。旧 `blockHeatApplicationMultiplier` 仍保留为配置兼容项，但当前生产
合成不再把任何局部场送进旧 `BlockTemperatureModel` 热区公式。

## 6. 空气温度

`WorldTemperature.air` 的 natural fallback 使用另一套公式：

```text
alpha_air(y) = 0                              , y <= 0
             = (y / 63)                       , 0 < y <= 63
             = 1                              , y > 63

T_natural_air_query = max(absoluteZero,
                          D + B + A + alpha_air * C + gaussian(0,0.3))
```

`WorldTemperature.naturalAir` 使用同一个 `alpha_air`，但明确排除 mesh、analytic fields
和随机扰动：

```text
T_natural_air = max(absoluteZero, D + B + A + alpha_air * C)
```

新热学 runtime 在 Page admission 时以 section 中心的 `T_natural_air` 初始化空气并作为
FarField 外部温度。每 `200` ticks 最多刷新一次已 admission
Page，背景变化达到 `0.25 degC` 才替换 sweep，几何和已有 cell enthalpy 不重建。

空气公式有三个必须保留的当前差异：

- `alpha_air` 使用 `WorldTemperature.SEA_LEVEL=63` 和 `STONE_INTERFACE_LEVEL=0` 硬编码常量，不读取对应服务端配置；
- 气候最大影响为 `1.0`，而方块默认最大影响为 `0.5`；
- `WorldTemperature.air` 的 fallback 有 `0.3degC` 高斯扰动，`naturalAir` 和 FarField 没有。

每次服务端空气温度查询还会从世界随机源加入标准差 `0.3` 的高斯扰动，所以相同位置连续查询不保证相同结果。

当前 gameplay runtime 为各维度安装同一空气 open-space FarField 阻抗，维度只改变
`T_natural_air`。Page capture 还封存每个 XZ 列首个天空暴露 local Y；拓扑编译把 Air-Air pair
连通的 component 聚合；只有真实天空暴露能批准完整 FarField，开放方向数量不再作为室外
证明。玩家或物理热源直接 admission 的地下 Page 会沿开放面额外 capture 一层已经加载的
相邻 Page，自动 continuation 每维度最多 `64` Page，且不会递归扩张或加载 chunk。剩余非天空
边缘保持 degraded，但在 approved profile 校准域内会按真实 microface 面积、风力以及
`1 / (1 + 16)` 距离因子获得弱 `ThermalSweep.BoundaryOperation`，避免长隧道末端成为完全
绝热边界。全局风力把 calm 导纳连续缩放到 `1.0..1.8` 倍；近似 continuation 不会被标记为
完整室外闭合。

## 7. Analytic control fields

`MinecraftThermalInput` 每个维度只保存一份按 `(combineMode, priority, fieldId)` 排序的
`AnalyticField` 列表。field 可为 `CUBE`、`PILLAR` 或 `SPHERE`，不会复制到覆盖区块、不会挂
capability、不会创建 Page，也不参与 `H/C/P/G` 守恒账本。相同 `fieldId` 的更新原位替换定义。

合成发生在 natural/mesh 选择之后，固定顺序为：

```text
OVERRIDE -> MAX_HEAT -> MIN_COOL -> ADD_DELTA
```

同一 mode 内再按 priority 和 field ID 排序。Curiosity 冷场使用 `ADD_DELTA`，`/heat_adjust`
创建运行期 `OVERRIDE` field。控制场当前不跨服务器重启持久化；Curiosity 由实体状态在载入后
重新报告。红外请求直接从 analytic list 和 physical source manager 生成一次性 shader payload，
不维护客户端对应的区块热区副本。

Campfire、Generator 和蒸汽喷泉不是 analytic field；它们由
`MinecraftPhysicalSourceManager` 注册为显式功率 source，进入 mesh 与直接辐射路径。
`ChunkHeatData`、`IHeatArea`、chunk capability、周期 revalidation 和旧失效包均已删除。

## 8. 主要消费者

`WorldTemperature.block` 及同一 compositor 目前驱动：

- `ServerLevelMixin_TemperatureUpdate` 中的水冻结、冰/流体/其他 `StateTransitionData` 状态变化；
- `PlantTempData` 的施肥、生长、生存和死亡检查；
- 动物、蜂巢、村民交易、战利品条件和温度探针；
- 城镇住宅和狩猎建筑的内部体素温度扫描。`MineBlockScanner` 中的旧温度累积当前没有生产调用者，`MineBaseBlockScanner` 不计算温度。

`WorldTemperature.air` 主要供被动环境查询、降雪判断及显示工具使用。玩家体温路径直接消费 sparse publication、analytic field 和物理辐射；旧 `BlockTempData` 粒子采样当前不再调度，见 [player-temperature.md](player-temperature.md)。

`WorldTemperature.checkPlantStatus` 真正需要温度的路径调用 `MinecraftThermalInput.gameplayCropEnvironment`。已有 Air Mesh publication 命中时，返回的空气温度直接进入施肥、生长、生存和死亡阈值；无 active runtime、无 Page、无空气 component、stale 或超龄 publication 时使用 natural block temperature，再合成 analytic field。天气先行决定植物状态时不发起 thermal query。该 passive 路径不会创建 Page、Brick、Cell 或 Interest。

住宅与狩猎基地扫描器访问内部空气时同步把坐标压缩成 `TownThermalProjection` 的 `4×4×4` weighted groups；成功扫描后每组只查询一个已有 publication。全部 group 命中时，新加权空气平均值直接写入建筑温度并驱动评分与日结算；任一 group miss 时整体回退同次 natural 全体素平均，并按 representative group 合成 analytic field，避免混合两套不完整区域。该路径没有第二次房间/体素遍历，不保留 mesh lease，miss 也不能 admission。矿井基地当前没有温度工作条件，因此未增加虚构的 mine consumer。

## 9. 持久化与当前约束

`WorldClimate.save` 当前保存 `WorldClockSource`、`dailyTempData`、`whitecurtains` 和 `isInitialEventAdded`。`ClimateEventTrack` 流本身没有写入该 NBT；载入后现有小时缓存先继续使用，轨道在需要生成更远日期时从当前时刻重新增长。预报帧不持久化，而是从缓存重建。

`WorldTemperature.worldCache` 和 `biomeCache` 分别按 `Level` 和 `Biome` 缓存数据配方结果。当前数据重载监听器会替换 `WorldTempData.cacheList`/`BiomeTempData.cacheList`，但源码中没有调用 `WorldTemperature.clear()`；已缓存的维度和群系值可能在 `/reload` 后继续沿用到进程或对应对象生命周期结束。
