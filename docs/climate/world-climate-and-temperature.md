# 世界气候与环境温度

- Status: `Current`
- Last verified: `2026-09-09`
- Scope: 逻辑气候时钟、长期事件、局部白幕、自然/mesh/analytic 温度合成、红外视野、方块状态消费者
- Primary code anchors: `WorldClockSource`, `WorldClimate`, `ClimateEventModel`, `ClimateEventTrack`, `InterpolationClimateEvent`, `WhiteCurtainDescriptor`, `WhiteCurtainFieldModel`, `WhiteCurtainInfo`, `WorldTemperature`, `BlockTemperatureModel`, `ThermalAnalyticField`, `ThermalAnalyticFieldIndex`, `MinecraftThermalInput.gameplayPassiveEnvironment`, `MinecraftThermalInput.gameplayCropEnvironment`, `MinecraftThermalInput.gameplayInfraredSnapshot`, `TownThermalProjection`, `MinecraftThermalInput.gameplayTownEnvironment`, `InfraredViewRenderer`

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
air 替换局部自然值；当前稀疏 Page 尚未发布目标 Brick signature payload 时先读取该 Brick 的
dormant checkpoint，已编译但目标点确实无 Air 时仍保留 natural backend；最后应用 analytic control fields。该 passive 查询不会
创建 Page，也不会加载区块。`blockHeatApplicationMultiplier` 仍由
`WorldTemperature.naturalBlock` 传入 `BlockTemperatureModel`，但当前调用没有额外局部热区项。

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
FarField 外部温度。已 admission Page 按 section hash 错峰排入刷新队列，同一 Page 两次刷新
至少间隔 `200` ticks，每 tick 最多出队处理 `16` 个到期项；已 withdraw 或 generation
不匹配的 stale 项同样占用该预算且不再持有 `ThermalPage` 引用，积压时会继续顺延。背景变化达到
`0.25 degC` 才局部替换受影响 Brick 的 FarField boundary，几何、coverage slot、Air
邻接和已有 cell enthalpy 都不重建。全维度风力仍每 `200` ticks
采样一次，作为一个有界 FarField scale 输入交给 worker；不会遍历房间或全局连通集合。天空截面不做周期全
Page 重采样：方块 mutation 在 heightmap 更新完成后于 tick-end 合并查询实际变化的 XZ 列，
每 tick 最多处理 `64` 列；积压列留到后续 tick，并只重编该列穿过的 Brick open fragment。

空气公式有三个必须保留的当前差异：

- `alpha_air` 使用 `WorldTemperature.SEA_LEVEL=63` 和 `STONE_INTERFACE_LEVEL=0` 硬编码常量，不读取对应服务端配置；
- 气候最大影响为 `1.0`，而方块默认最大影响为 `0.5`；
- `WorldTemperature.air` 的 fallback 有 `0.3degC` 高斯扰动，`naturalAir` 和 FarField 没有。

每次服务端空气温度查询还会从世界随机源加入标准差 `0.3` 的高斯扰动，所以相同位置连续查询不保证相同结果。

当前 gameplay runtime 为各维度安装同一空气 open-space FarField 阻抗，维度只改变
`T_natural_air`。Page capture 还封存每个 XZ 列首个天空暴露 local Y；拓扑编译只在当前 Brick
及其已 admission 的相邻 Brick 中生成 Air pair。真实天空暴露才会生成完整 FarField；开放方向
数量不作为室外证明。玩家或物理热源直接 admission 的地下 Page 会沿开放面额外 capture 一层已经加载的
相邻 Page，自动 continuation 只在共享的 Page admission 预算内保留，且不会递归扩张或加载 chunk。剩余非天空
边缘保持 degraded，但在 approved profile 校准域内会按真实 microface 面积、风力以及
`1 / (1 + 16)` 距离因子获得弱 `ThermalFragment.FarBoundaries`，避免长隧道末端成为完全
绝热边界。全局风力把 calm 导纳连续缩放到 `1.0..1.8` 倍；近似 continuation 不会被标记为
完整室外闭合。

## 7. Analytic control fields

`MinecraftGameplayFields` 在服务端主线程按实际 `ServerLevel` 身份持有一份
`ThermalAnalyticFieldIndex`；`MinecraftThermalInput` 缓存同一索引引用。field 可为
`CUBE`、`PILLAR` 或 `SPHERE`，范围内为常值，无衰减或遮挡；不会复制到覆盖区块、挂 capability、
创建 Page，也不参与 `H/C/P/G` 守恒账本。键为 `ThermalFieldKey(provider, ownerHigh, ownerLow, channel)`：
generator 使用团队数据完整 UUID，Curiosity 使用实体完整 UUID，命令使用独立命名空间内的位置。
更新 map 与有序列表引用同一 Entry；重复球形报告不创建定义，不排序；仅新增、删除、mode/priority
变化需要移动列表。普通点查询仍按列表进行 O(F) 扫描。

合成发生在 natural/mesh 选择之后，固定顺序为：

```text
FLOOR_FROM_NATURAL -> OVERRIDE -> MAX_HEAT -> MIN_COOL -> ADD_DELTA
```

同一 mode 内按 priority 和完整 key 排序。Generator 使用 `FLOOR_FROM_NATURAL`：令 N 为当前消费者
在查询点的自然温度，P 为原有 live/dormant/natural 选择，D 为命中保底场的最大温差，先求
`max(P, N + D)`；没有保底场时保留 P。N/P 是摄氏温度，D 是摄氏温差。Curiosity 的负值
`ADD_DELTA` 在后续扣减；`/heat_adjust` 创建 `OVERRIDE`，删除时只删除命令自己的场。
保底适用于区域玩法，包括人物、作物和城镇，并不向物理网格注入能量。

Generator 的半径/温差来自 `GeneratorData.getRadius/getTempMod` 和 `GeneratorHeatFieldModel`：
默认半径为 `floor(16 * Lr)`（`0 < Lr <= 1`）或 `floor(16 + 8 * (Lr - 1))`（`Lr > 1`）个方块，
默认温差为 `floor(10 * Lt)`。配置属于 `FHConfig.SERVER.TOWN.GENERATOR_T1` 的
`baseRadiusBlocks`、`additionalRadiusPerLevelBlocks`、`temperaturePerLevelCelsius`。
例如 N=-40、D=30 时保底是 -10；室内物理温度只有超过 -10 后才进一步提高合成温度。
这不是旧 `BlockTemperatureModel.applyHeat` 的 `min(N + 2H, H)` 曲线；该旧曲线不用于通用场。

解析场不会触发物理 runtime 启动，物理关闭/配方重载不清场。实际世界卸载和服务器停止清理索引；
服务器重启后 generator 从团队数据重建，Curiosity 从实体状态重建，命令场不持久化。
红外视野不直接显示 analytic field 或 physical source；它只读取
`PagePublication` 与 `QueryPublication` 已求解的实际 Air 温度，并在未解析 Brick
使用已存储的 dormant 均温。服务端对每个实时
world block 中心调用 `PagePublication.resolveAirPoint`，再把得到的实际 Air cell
温度量化为一个 0.25degC signed-short texel；同一 `4 x 4 x 4` Brick 内被完整墙体
隔开的两侧因此可以显示不同温度，`Short.MIN_VALUE` 表示中心没有可显示 Air。
这是 block-position exact，不是任意子方块 component exact；楼梯、门、栅栏等同一
方块内存在多个 Air component 时只表示包含方块中心的 component。

客户端开启、跨 chunk/section 时请求 full snapshot，稳定时按 entity ID 错峰每
`40` ticks 携带 infrared epoch 和 729-bit Page presence。服务端使用固定 Page/Brick
epoch 数组回答任意旧客户端，只编码视野内 epoch 更新的 Bricks；presence mismatch
只发送 added/removed Page delta。无可见 Brick/presence 变化时不发送 S2C，即使维度
内别处推进了 epoch。QueryPublication 暂时 invalid 或超龄时不清除旧实时覆盖，
仍可返回 dormant 更新，并以实时 epoch 0 要求下一份 coherent 响应重建实时基线；
此时已知实时 section 内只更新此前由 dormant 拥有的 texel。
center/full 请求在匹配响应被接受前不会降级为
delta。等待 full 时暂停固定 40-tick poll，并按 entity ID 分散在 `41..59` ticks 后
重试；该区间没有 20 的倍数，因此不会与 20-tick thermal cut 永久同相。客户端只把
delta 应用到相同 texture center；不同中心的 delta 被丢弃并在
下一 tick 重新 full，full 响应则接管其服务端中心。有效 publication 确认的 Page
retirement 只清除对应 `16^3` 区域。
范围枚举复用 `MinecraftPageManager.pagesByChunk`，只读取已有 coherent
publication，不 admission Page、retain lease 或加载 chunk。客户端写入一张线性
`GL_R16I 144 x 144 x 144` 纹理，CPU 侧只保留同一份 persistent direct
`ShortBuffer`；mirror 在首次实际渲染红外视野时分配，8 KiB Page scratch 在首次
Page delta 时分配，之后都有界复用。
世界 reset 会立即摘下 GPU handle，并让 render callback 只删除捕获的旧资源。首个
matching full snapshot 安装前，客户端以玩家当前 section 为中心渲染全 `INVALID`
纹理，红外初始化不依赖服务端立即响应；跨 section 等待新 full 时继续按旧 texture
origin 渲染旧 snapshot，响应到达后再整表替换，因此网络 full 状态不会使红外 pass
闪烁。full 上传整张纹理，delta 通过该 scratch 只上传改变的
`16^3` Page。fragment shader 每像素只执行一次 integer texture
fetch。depth 重建得到的是可见几何表面；采样前沿 camera ray 向摄像机偏移
`1/2048` 的相对距离，使方块面稳定读取表面前方的
Air texel，不在相邻 texels 间闪烁。扫描球内 invalid/无 Page texel 按
`MIN_TEMP` 显示冷蓝。shader 的逆视图矩阵来自当前 `GameRenderer` main `Camera`；
Java 先以 double 计算 camera 到 texture origin 的相对坐标，再转换为小范围 float
uniform，避免远世界坐标丢失一格精度。潜行眼高平滑和第三人称不会把温度坐标相对
depth 偏移。篝火烟雾等写 depth 的粒子沿用同一世界坐标采样，与粒子所在 Air
texel 的温度颜色融合；不增加粒子 mask、专用 pass 或渲染时序分支。

每次红外请求还检查视野内已加载 chunk 的 dormant Brick mean，不依赖 source
是否发现，不 admission Page、不加载 chunk。section 首次被查询才创建共享量化缓存，
复用 20-tick 自然温度/衰减缓存；常规增量只发送变化 Brick，落后客户端收到 section
替换，数据消失则显式删除。客户端增加一份 5,832 字节的 dormant Brick 所有权位图，
复用原纹理。实时 `resolved` Brick（包括无 Air）优先，未解析 Brick 可继续显示
暂存均温；实时更新时同包重写受影响 section 的剩余 fallback，避免覆盖丢失。
关闭红外立即停止客户端请求；服务端实时比较最多延续 80 tick，dormant 计算只由
请求触发。编码及生命周期详见 [data-lifecycle-and-integration.md](data-lifecycle-and-integration.md#network-and-consumers)。

Campfire、Generator 和蒸汽喷泉仍由 `PhysicalSourceSpatialIndex` 注册为显式功率 source。
Generator 另外提供上述解析保底；其物理功率和传播范围不受解析场半径裁剪。
`ChunkHeatData`、`IHeatArea`、chunk capability、周期 revalidation 和旧失效包均已删除。

## 8. 主要消费者

`WorldTemperature.block` 及同一 compositor 目前驱动：

- `ServerLevelMixin_TemperatureUpdate` 中的水冻结、冰/流体/其他 `StateTransitionData` 状态变化；
- `PlantTempData` 的施肥、生长、生存和死亡检查；
- 动物、蜂巢、村民交易、战利品条件和温度探针；
- 城镇住宅和狩猎建筑的内部体素温度扫描。`MineBlockScanner` 中的旧温度累积当前没有生产调用者，`MineBaseBlockScanner` 不计算温度。

`WorldTemperature.air` 主要供被动环境查询、降雪判断及显示工具使用。玩家体温路径直接消费 sparse publication、analytic field 和物理辐射；旧 `BlockTempData` 粒子采样当前不再调度，见 [player-temperature.md](player-temperature.md)。

`WorldTemperature.checkPlantStatus` 真正需要温度的路径调用 `MinecraftThermalInput.gameplayCropEnvironment`。已有 Air Mesh publication 命中时，返回的空气温度直接进入施肥、生长、生存和死亡阈值；无 active runtime、无 Page、无可解析 Air 点、stale 或超龄 publication 时使用 natural block temperature，再合成 analytic field。天气先行决定植物状态时不发起 thermal query。该 passive 路径不会创建 Page、Brick、Cell 或 Interest。

`MinecraftThermalInput.gameplayItemEnvironment` 为掉落暖石和热水袋读取已有 live/last
publication，未命中时使用已加载 chunk 的 dormant 温度，再回退 `WorldTemperature.naturalAir`。
随后在物品中心按当前 `MinecraftGameplayFields` 合成解析场：能量塔取
`max(physicalOrFallback, naturalAir + maximumMatchingDelta)`，再执行命令和 Boss 控制。
即使物理 runtime 不存在或刚关闭，世界拥有的解析场仍然生效；查询不会启动 runtime 或加载区块。
有 runtime 时，同 tick 最多缓存 64 个四分之一方块位置的原始空气/直接辐射结果；解析场不缓存，
每次按精确位置重新合成，保留同 tick 场更新、移除与边界变化。缓存满后仍采样空气并合成解析场，
新增位置的直接辐射为零。`RadiationService.sampleItem` 保留独立的物品辐射预算。

住宅与狩猎基地扫描器访问内部空气时同步把坐标压缩成 `TownThermalProjection` 的 `4×4×4` weighted groups。
每组在 representative 点独立选择 live/dormant/natural，合成解析场后按体素数加权，并驱动评分与日结算。
命中相对保底的 live/dormant 点才额外计算当地 natural；不会使用全建筑平均值构造局部保底。
该路径没有第二次房间/体素遍历，不保留 mesh lease，miss 也不能 admission。矿井基地当前没有温度工作条件。

`ServerLevelMixin_TemperatureUpdate` 对每个候选融化/蒸发阈值检查显式解析下限：
`L = compose(natural, -Infinity)`。被物理 phase 接管的方块，仅当解析下限自身达到对应阈值时，
才允许原有玩法相变路径执行该升温变化；融化下限不能越权触发更高阈值的蒸发。Boss 负温差会降低 L，
单独 `ADD_DELTA` 没有绝对下限，不绕过潜热。方块变化仍走原有 mutation/phase ACK 失效路径。

## 9. 持久化与当前约束

`WorldClimate.save` 当前保存 `WorldClockSource`、`dailyTempData`、`whitecurtains` 和 `isInitialEventAdded`。`ClimateEventTrack` 流本身没有写入该 NBT；载入后现有小时缓存先继续使用，轨道在需要生成更远日期时从当前时刻重新增长。预报帧不持久化，而是从缓存重建。

`WorldTemperature.worldCache` 和 `biomeCache` 分别按 `Level` 和 `Biome` 缓存数据配方结果。当前数据重载监听器会替换 `WorldTempData.cacheList`/`BiomeTempData.cacheList`，但源码中没有调用 `WorldTemperature.clear()`；已缓存的维度和群系值可能在 `/reload` 后继续沿用到进程或对应对象生命周期结束。
