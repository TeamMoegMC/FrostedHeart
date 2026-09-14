# 世界气候与环境温度

- Status: `Current`
- Last verified: `2026-09-14`
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
创建 Page，也不会加载区块。`WorldTemperature.naturalBlock`直接使用
`BlockTemperatureModel.climateBlockAffection`和`naturalTemperature`，再应用绝对零度下限；
它不读取`blockHeatApplicationMultiplier`或执行零热量的旧加热公式。旧城镇模拟器仍使用`applyHeat`。

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
不匹配的 stale 项同样占用该预算；其Entry中的handle与天空列数组在出队时释放。
每次实际采样后以当前gameTick加200安排下次刷新，不追赶历史截止时间。背景变化达到
`0.25 degC` 才局部替换受影响 Brick 的 FarField boundary，几何、coverage slot、Air
邻接和已有 cell enthalpy 都不重建。全维度风力仍每 `200` ticks
采样一次，作为一个有界 FarField scale 输入交给 worker；不会遍历房间或全局连通集合。天空截面不做周期全
Page 重采样：方块 mutation 在 heightmap 更新完成后于 tick-end 合并查询实际变化的 XZ 列，
每 tick 最多处理 `64` 列；积压列留到后续 tick，并只重编该列穿过的 Brick open fragment。

空气公式有三个必须保留的当前差异：

- `alpha_air` 使用 `WorldTemperature.SEA_LEVEL=63` 和 `STONE_INTERFACE_LEVEL=0` 硬编码常量，不读取对应服务端配置；
- 气候最大影响为 `1.0`，而方块默认最大影响为 `0.5`；
- `WorldTemperature.air` 的 fallback 有 `0.3degC` 高斯扰动，`naturalAir` 和 FarField 没有。

自然空气在`y <= STONE_INTERFACE_LEVEL`时的气候系数为零，直接跳过气候查询；
维度、群系、海拔贡献及绝对零度下限保持原公式。

每次服务端空气温度查询还会从世界随机源加入标准差 `0.3` 的高斯扰动，所以相同位置连续查询不保证相同结果。

当前 gameplay runtime 为各维度安装同一空气 open-space FarField 阻抗，维度只改变
`T_natural_air`。Page capture 还封存每个 XZ 列首个天空暴露 local Y；拓扑编译只在当前 Brick
及其已 admission 的相邻 Brick 中生成 Air pair。真实天空暴露才会生成完整 FarField；开放方向
数量不作为室外证明。物理source播种其出口Brick，worker根据热残差请求相邻驻留Brick；
缺失的非天空邻居没有合成FarField散热项，也没有旧的一层continuation扫描。
当前FarField由顶层Brick上方缺失邻Page且该列真实天空暴露的方块面生成，按通风率缩放。
全局风力把calm导纳连续缩放到`1.0..1.8`倍。玩家、作物和显示查询不直接admission Page。

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
红外温度数据的基础为已有方块材料表层平均温度，
再在服务端按原顺序合成玩法解析场。无解析修正的材料值采用黑体近似`epsilon=tau=1`，
无需额外距离平方、视角余弦或辐射求解。解析修正后的`displayTemperature`是玩法显示值，
不能称为实测材料温度；它不回写材料H/C/T。现有节点六面共用，不代表六面独立或内部温度。

`MinecraftThermalInput.gameplayInfraredSnapshot`通过`BlockBrickLayout.surfaceNodeMask`
读取材料本体节点的H→T，包括普通材料、相变平台与相变后的状态。楼梯本体不再与空气
共用温度；实际Air和休眠Air均温不作为材料基础。材料本体H/分支/时间另存于format 4，
不通过Air均温恢复。旧相变池和旧存档兼容读取已移除。

活动Brick优先使用当前材料发布；未驻留的Brick可直接读取已加载Chunk中
`DormantChunkThermalState.materials`的既有材料记录，并验证当前BlockState一致。
这与温度计使用同一`MaterialSectionState.read`关系。活动Page正在更新时不以旧记录覆盖它。
活动模拟关闭后，保存的材料温度按`DormantThermalCooling`投影后显示；不创建新Page或第二份H。
空气与普通材料共用默认1800游戏秒温差半衰期，相变材料保留潜热平台。
当前Section中心自然温度近似整个休眠区间；不回放天气历史，也不累计服务器停止的现实时间。

局部几何变化不等于材料温度消失。红外和`sampleMaterial`读取`ThermalPageHandle.lastPublication()`
这份既有材料发布，并继续检查slot generation及query cut一致性。材料变更日志只排除发布之后
真正替换、物质量变化或转换的位置；未变化方块在同Brick/同Page重建期间仍可读。
红外通过`collectMaterialChangesSince`一次展开到共用的64个long（512 bytes）临时位图，
只重发受影响Brick，不为每Page/玩家保存额外温度。被删除或A→Air→A替换的旧物体不会保温串值；
整段替换仍排除整段旧材料。这不允许空气传输查询复用已失效的几何路线。

`storedEpoch`只用于同步变化：客户端在完整响应的最后一片提交后回传编号，服务端按Section
变化编号更新，删除记录和替换/重载Chunk也会清除旧热色。编号不写入存档；协议不保留旧格式读取。
协议另带`storedSampleTick`，LAST时一起提交。即使记录未编辑，两个时刻的量化温度不同也会更新
对应Brick；没有量化变化且没有解析场刷新时不产生重复响应。自然温度变动会使Section显示基准失效。

有材料且无场时显示材料温度；无材料且无场时发送`Short.MIN_VALUE`，客户端使用
`MIN_TEMP=-20`的蓝色**占位**，不把-20当实测值。全窗口自然背景估计、729值背景包和
第二张背景纹理已删除。完整扫描覆盖不受材料缺值影响。

命中场的块中心使用`ThermalAnalyticFieldIndex.Sample`合成。能量塔仍是
`max(base, naturalAir + getTempMod())`保底；有冷材料也执行，较热材料不被降低。
没有材料而公式需要base时，仅在场范围内用`WorldTemperature.naturalAir`作原公式基准；
OVERRIDE不需要这项读取。场合成后统一量化0.25°C，保留原priority/key排序及其他场模式。
自然读取只在命中且需要时发生，复用精确同biome Brick按Y层计算；未加载的必要邻区不会
被强制加载，也不拿Page中心值替代。能量塔显示与物理runtime的可用性独立。

客户端仍用单张`GL_R16I 144³`最终显示纹理、一个direct mirror和部分Page上传scratch。
稳定窗口每40 ticks错峰请求；无场窗口按材料epoch增量，场窗口重算上次/当前场Page并集，
使关闭或缩小范围后恢复材料/蓝底。材料presence不会在客户端直接擦掉场热色。
并发发布导致Page/slot/cursor不一致时，整份读取最多重试两次；仍未读到一致cut则不发送响应，
客户端保留已提交的显示，下一次按原节奏请求，不把读竞争编码成材料删除。
全局物理cut稳定invalid或超过40 ticks时
返回field-only full，恢复后full重建。同一不可读状态可以继续delta。只有LAST才提交
origin、epoch、generation与两个Page位图并上传。详见[网络合同](data-lifecycle-and-integration.md#network-and-consumers)。

`FHClientEvents.onRenderSurfaceInfrared`在`AFTER_LEVEL`直接混合到主颜色附件。
`InfraredChunkRenderer`继承Embeddium原地形renderer，沿用原网格、可见性、排序和multiDraw。
`InfraredBlockScopeMixin`在实际`BlockRenderer.renderModel`调用期间设置`BlockOwnerScope`，
普通/Forge fallback/FRAPI模型产生的顶点由同一encoder携带真实源方块归属。
不再从像素深度、表面法线、整数面或相邻温度猜方块，跨块伸出的普通模型也使用源方块温度。

`OwnedChunkVertexType.COMPACT`仍为20 bytes/vertex：原前16 bytes不动，block/sky light
各占byte 16/17，独立`UNSIGNED_SHORT`归属属性占18..19。高精度型保留原28 bytes，
尾部增加4 bytes，总32 bytes。归属为`0x1000 | x | (z << 4) | (y << 8)`，其中xyz
是0..15的Section局部坐标；0表示没有模型归属。原格式常量与普通shader不改。
有4个生产Mixin桥接：renderer创建、GPU arena高精度步长、自有属性绑定、模型归属scope。

开启期间，SOLID/CUTOUT的同一次地形draw额外写屏幕尺寸`R16I`；原材质alpha discard和
深度测试同时约束颜色与温度，顶点shader查询144³纹理后用`flat int`传递温度。
`InfraredSurfaceTarget`拥有温度图、地形深度快照和两个FBO，借用主颜色/深度；最终混合FBO只挂颜色，
采样主深度不会形成读写反馈。每活动帧第一次地形pass清温度为INVALID，后续pass保留已有结果。
Embeddium在同一solid调用中依次绘制SOLID/CUTOUT；自有renderer在CUTOUT完成后、实体绘制前，
用`captureTerrainDepth()`复制一次实际深度存储。快照沿用主深度精度；不使用低精度压缩或epsilon。
最终shader逐像素比较快照与主深度：相同才使用地形热图，较晚的深度写入者不能继承身后的冷热轮廓。
没有重新绘制地形或实体。
保留原精度复制经过对比：D24下同次MRT输出的`gl_FragCoord.z`未通过原生深度一致性测试，
简单量化也不等价，故未替换生产路径。`verifyNativeDepth`另覆盖12,441,600个默认/D24/D32F
渐变像素，要求全部保持正确地形温度；资格和GPU成本结果见IR plan末尾。

实体等遮挡像素采用环境显示近似：从自己的可见表面位置重建camera-relative坐标，加
`cameraToTemperatureOrigin`，直接读取既有144³显示纹理。已有能量塔等解析场显示可直接复用；
没有有效值时仍采用蓝色基底。此路径不查询/同步真实空气温度，不取脚下块，不建立实体体温或逐实体缓存。
其目的仅是实体融入环境，不能称为实测空气温度或体温；方块本身仍通过精确owner取材料温度。
不写深度的透明介质/粒子的独立遮挡与热成像尚未实现。

`InfraredRasterValidation`使用实际encoder、属性和捕获shader，覆盖两种顶点格式、D24/D32F、
平面、完整块、楼梯及cutout，共8,640场景、353,123,074个分类像素，归属错误0，GL error 0。
此结果验证精确归属，不能代替真实整合包性能与所有特殊模型验收；实际接入进度见
[实施plan](../../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md)。
独立开发客户端已通过两种格式的真实接入、resize、资源重载、缩圈释放及重开；原图与红外图
37,246个热表面像素对照RGB误差≤1色阶、alpha精确相同。Forge AFTER_LEVEL与地形入口的
PoseStack不是同一个对象；最终显示使用已捕获camera pose和本帧主FBO/terrain访问标志。
稳定圈内保留-20..20°C原色标和`mix(originalRGB,heatColor,0.43)`；最终RGB受原图影响，
不是定量温度图。天空/圈外保留原图，前沿3 blocks保留原扫描动画。
RGB使用预乘`ONE, ONE_MINUS_SRC_ALPHA`，alpha使用`ZERO, ONE`保留目标alpha。
混合结束恢复进入前的GL program，保持`ShaderInstance`编号缓存与实际绑定一致；不调用
只解绑到0的`ShaderProgram.release()`。自有program使用raw GL绑定，因此也用raw GL恢复，
不让Oculus的绑定缓存跳过恢复。纹理、viewport/scissor及depth/blend状态也恢复。
删除了旧RGBA8中转图和颜色复制；当前自有图像为2P-byte温度图加原精度深度快照。
D24/D32F快照通常按4P bytes计，总预算6P bytes（P为屏幕像素数）；比此前未处理实体遮挡的2P多4P。
快照是GPU→GPU复制，没有CPU整屏读回；实体环境取色每个遮挡像素最多增加一次3D纹理读取。
关闭并完成缩圈后释放屏幕附件；资源重载重建program，卸载时删除自有资源，不删除借用附件。
本轮范围是固定Embeddium整合包且不启用光影；特殊BE和实体的独立热状态留待以后。
Campfire、Generator 和蒸汽喷泉仍由 `PhysicalSourceSpatialIndex` 注册为显式功率 source。
Generator 另外提供上述解析保底；其物理功率和传播范围不受解析场半径裁剪。
`ChunkHeatData`、`IHeatArea`、chunk capability、周期 revalidation 和旧失效包均已删除。

## 8. 主要消费者

环境温度及同一 compositor 目前驱动：

- `MinecraftPhaseController.tryAtRandomTick` 中无材料记录的环境平衡转换；有记录时由材料H和同一数据边决定相变；
- `PlantTempData` 的施肥、生长、生存和死亡检查；
- 动物、蜂巢、村民交易、战利品条件和温度探针；
- 城镇住宅和狩猎建筑的内部体素温度扫描。`MineBlockScanner` 中的旧温度累积当前没有生产调用者，`MineBaseBlockScanner` 不计算温度。

`WorldTemperature.air` 主要供被动环境查询、降雪判断及显示工具使用。玩家体温路径直接消费 sparse publication、analytic field 和物理辐射；旧 `BlockTempData` 粒子采样当前不再调度，见 [player-temperature.md](player-temperature.md)。

`WorldTemperature.checkPlantStatus` 真正需要温度的路径调用 `MinecraftThermalInput.gameplayCropEnvironment`。已有 Air Mesh publication 命中时，返回的空气温度直接进入施肥、生长、生存和死亡阈值；无 active runtime、无 Page、无可解析 Air 点、stale 或超龄 publication 时使用 natural block temperature，再合成 analytic field。天气先行决定植物状态时不发起 thermal query。该 passive 路径不会创建 Page、Brick、Cell 或 Interest。

`MinecraftThermalInput.gameplayItemEnvironment` 为掉落暖石和热水袋读取已有 live/last
publication，未命中时使用已加载 chunk 的 dormant 温度，再回退 `WorldTemperature.naturalAir`。
方块形态的 `gameplayPlacedReservoirEnvironment` 复用同一查询和预算，在方块相对坐标
`(0.5, 0.3125, 0.5)`（模型上方）采样；两种形态使用相同的暴露换热速率。
随后在接收点按当前 `MinecraftGameplayFields` 合成解析场：能量塔取
`max(physicalOrFallback, naturalAir + maximumMatchingDelta)`，再执行命令和 Boss 控制。
即使物理 runtime 不存在或刚关闭，世界拥有的解析场仍然生效；查询不会启动 runtime 或加载区块。
有 runtime 时，同 tick 最多缓存 64 个四分之一方块位置的原始空气/直接辐射结果；解析场不缓存，
每次按精确位置重新合成，保留同 tick 场更新、移除与边界变化。缓存满后仍采样空气并合成解析场，
新增位置的直接辐射为零。`RadiationService.sampleItem` 保留独立的物品辐射预算。

住宅与狩猎基地扫描器访问内部空气时同步把坐标压缩成 `TownThermalProjection` 的 `4×4×4` weighted groups。
每组在 representative 点独立选择 live/dormant/natural，合成解析场后按体素数加权，并驱动评分与日结算。
命中相对保底的 live/dormant 点才额外计算当地 natural；不会使用全建筑平均值构造局部保底。
该路径没有第二次房间/体素遍历，不保留 mesh lease，miss 也不能 admission。矿井基地当前没有温度工作条件。

`MinecraftPhaseController` 对编译升温边检查显式解析下限：
`L = compose(natural, -Infinity)`。被物理 phase 接管的方块，仅当解析下限自身达到对应阈值时，
才允许原有玩法相变路径执行该升温变化；融化下限不能越权触发更高阈值的蒸发。Boss 负温差会降低 L，
单独 `ADD_DELTA` 没有绝对下限，不绕过潜热。方块变化仍走原有 mutation/phase ACK 失效路径。

## 9. 持久化与当前约束

`WorldClimate.save` 当前保存 `WorldClockSource`、`dailyTempData`、`whitecurtains` 和 `isInitialEventAdded`。`ClimateEventTrack` 流本身没有写入该 NBT；载入后现有小时缓存先继续使用，轨道在需要生成更远日期时从当前时刻重新增长。预报帧不持久化，而是从缓存重建。

`WorldTemperature.worldCache` 和 `biomeCache` 分别按 `Level` 和 `Biome` 缓存数据配方结果。当前数据重载监听器会替换 `WorldTempData.cacheList`/`BiomeTempData.cacheList`，但源码中没有调用 `WorldTemperature.clear()`；已缓存的维度和群系值可能在 `/reload` 后继续沿用到进程或对应对象生命周期结束。
