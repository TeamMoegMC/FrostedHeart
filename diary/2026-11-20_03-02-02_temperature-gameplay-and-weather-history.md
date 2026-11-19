# 温度玩法与白幕开发记录

- Time: `2026-11-20 03:02:02 +08:00`
- Author: `OpenAI Codex`
- Status: 记录归并完成
- Scope: 2026 年 11 月 19 日傍晚至 20 日的取温、作物暖棚、篝火与白幕记录。

按原记录时间排列。读数、导热参数和验证结果对应各节记录时的版本。

## 目录

- [白幕从幕外到幕内的视线消光](#white-curtain-sightline-extinction)
- [作物位置温度统一与小麦供暖验证](#crop-growth-temperature)
- [纯土温种植的暖棚测量](#farmland-warmup)
- [暖棚散热与黑麦纯土温种植区间](#farmland-vent-balance)
- [一级塔暖棚的纯土温黑麦收获试验](#farmland-rye-harvest)
- [统一方块位置取温](#world-temperature-block-query)
- [环境与材料读数统一入口](#world-temperature-readers)
- [篝火本体与六面换热](#campfire-six-face-heat)
- [普通耕地导热标定与暖棚温控试验](#farmland-conductance-calibration)
- [土壤温度计统一材料优先取温](#soil-thermometer-material-first)
- [作物材料读数与耕地换热路径复测](#crop-material-soil-heat-paths)
- [热查询方法命名](#thermal-query-names)
- [方块取温调用核对](#world-temperature-consumer-audit)
- [植株材料优先后的暖棚生长实测](#planted-crop-growth-material-temperature)
- [作物每面换热导纳设为 2.5 W/K](#crop-conductance-2p5)

<a name="white-curtain-sightline-extinction"></a>

## 白幕从幕外到幕内的视线消光

- Author: Codex
- Time: 2026-11-19 18:50:58 +08:00
- Status: 默认 Fast 已通过不透明地标实机场景验证，低端 GPU 帧耗待测
- Scope: Forge 客户端空间天气，`CurtainVolume`、`CurtainExtinction`、`ClientWeatherFrame`、`FogModification` 与白幕着色器

### Completed

之前的雪墙在幕外只混合几层纹理颜色。相机进入后，局部雾才收紧远端距离。这使得同一座远塔从幕外清晰可辨，一跨进雪带就消失。现在 `AFTER_BLOCK_ENTITIES` 用不透明场景深度截断视线，计算它穿过前沿、雪带内部和尾沿的长度。相机在里外使用同一公式，外部能够看到的幕前塔和在白幕后的高塔可由同一帧验证。

### Decisions

- 默认 Fast 和 Fancy 都以 `16` 格的等效浓雪厚度耗尽地标对比度。雪带外壳密度为核心的 `0.28`，横向外壳厚 `4` 格，顶底各 `16` 格。材质将纹理用于雾色明暗。
- `CurtainVolume.set` 用事件的首末降雪小时计算体积与走廊的交集。由到这个体积的距离选取候选，相机从侧面或尾部观察时也能看见雪带。客户端仅在雪带投影范围内做两次解析求交。颜色专用 framebuffer 复用主颜色纹理，让主深度可以被直接采样。
- `FogModification` 保留背景天气的雾，局部白幕由视线积分负责。第三方光影包、流体等使用原纹理墙路径。近场粒子、声音、气候事件及其协议沿用现役实现。
- 更新 [天气渲染说明](../docs/climate/weather-rendering.md) 的第 4 至 9 节。服务端、存档和气候包的现行合同继续适用。

### Validation

- Java 17，Forge 1.20.1-47.3.0，`gradlew.bat --offline -I Scripts/dev_client_bridge.init.gradle runClient`。Embeddium/Oculus 已载入，外部光影包关闭，`weatherRenderingMode=SPATIAL_V1_FAST`，视距 10 区块，1280×720。最终运行以 `BUILD SUCCESSFUL` 正常退出。
- 建立专用平坦世界 `white-curtain-fast-20261119`，种子 `118062`。通过 `/climate white_curtain add 0 -60 0` 创建向北传播的事件。将 `doDaylightCycle=false`、`dayTime=174000` 固定为同一时刻。当前前沿位于 `z=32`。用该世界副本单独执行 `/climate white_curtain clear` 得到晴天对照。
- 幕外固定机位 `(8.5,-55,-45.5)`，yaw `1°`，pitch `-7°`。幕前砖塔位于 `z=18..20`，后方石塔位于 `z=86..90`。视野中同时有布置在不同深度的三棵树。在塔被实际移除、等待区块重绘后，对原始 framebuffer 截图进行 RGB 比较。像素框均为左上含、右下不含。

| 同一机位下的操作 | 像素框 (x0,y0,x1,y1) | RGB 有变化的像素 | 最大单通道差 (0–255) |
|---|---|---:|---:|
| 晴天移除后塔 | 660,325,692,440 | 1881 | 182 |
| 白幕中移除同一后塔 | 660,325,692,440 | 0 | 0 |
| 白幕中移除幕前塔 | 536,349,559,437 | 2021 | 184 |
| 幕前塔恢复后与原帧对比 | 536,349,559,437 | 0 | 0 |

- 固定朝向晴侧，从 `z=31.5、32.5、38.5、55.5` 四个位置采图。跨过前沿一格仍能辨认外面的砖纹，随后随雪程增加，外部地标逐渐消失。相机在 `z=73.5` 时同样拍到了贴近玩家的树叶和被遮蔽的远景。
- 原始截图在本地 `run/agent/screenshots`，以 `final-`、`extinction-clear-` 为场景标识。像素结果和同机位并排裁图在 `build/white-curtain-fast-20261119/`。测试结束时原雪带存档内已恢复两座塔。

### Remaining

- 实机验收限于本次 Fast 场景以及写入主深度的物体。透明水面、玻璃之间的多层消光和外部光影包的表现需要另外验证。此测试机为 RTX 4070 Laptop，尚缺低端 GPU 帧耗分位数。
- 跨过区块边界时，回望晴侧的画面中天空底色仍有明暗差异，前景砖塔在前沿两侧均可见。

<a name="crop-growth-temperature"></a>

## 作物位置温度统一与小麦供暖验证

- 时间　2026-11-19 22:38:36 +0800
- 作者　OpenAI Codex
- 状态　已完成生产入口、Forge 用例与客户端面板验证。
- 范围　`WorldTemperature.environment` / `checkPlantStatus`、`ClimateBlockUpdates`、`CropFertilization.temperatureData`、`SoilThermometerUpdatePacket.forCrop`、`PlantTempStats.getStats`。

旧基准 `0c9fe6209` 的 `checkPlantStatus` 在作物位置调用 `block(level, pos)`，土壤温度计也通过同一方块环境公式取值。本次沿用作物坐标来读取局部环境，篝火和能量塔共用空气场与解析场的合成。土壤温度计测量耕地时读取材料的储热记录或估算值。生长、随机死亡和面板应用相同的保温阈值，面板明确显示生长温度以及影响作物的冰雪暴露。树苗和一般作物共用温度判定。

### 验证

- 上一轮的统一热场验证在 `build/thermal-unified-release.log` 中完整结束，137 项通过。当前农业改动另以 Java 17 运行 `frostedheart_crop_temperature,frostedheart_fertilizer,frostedheart_generator_heat`，17 项通过，日志为 `build/crop-sources-gametest.log`。
- 真实小麦场景中，自然读数约 −6.4°C，篝火升温阶段约 −5.21°C，塔保底约 18.25°C，同时供热约 18.25°C。冷的耕地记录在只发布塔解析场时保持原温度。另用固定 −10°C 验证普通小麦有死亡判定而终极保温肥料允许生长，固定 40°C 验证保温肥料的高温上限。
- `runClient` 在隔离存档 `crop-wheat` 使用已有 GameTest 研究样本。手持土壤温度计，对准同一株小麦时，由 12.0°C「可以生长」自动更新为 −10.0°C「温度过低」。核对英文和中文截图，客户端正常退出。证据在 `build/crop-temperature-client-fixture/agent/screenshots/` 和 `build/crop-temperature-client-fixture.log`。场景用于温度与状态读数验证，供热的长期距离和开放地形生长收益仍需实际游玩数值定标。
- `git diff --check` 通过。伴生仓库保持干净。

文档影响见[作物温度与肥料](../docs/climate/crop-fertilization.md)、[世界温度](../docs/climate/world-climate-and-temperature.md)和[旧版游玩数值对照](../docs/climate/thermal-legacy-gameplay-parity.md)。

<a name="farmland-warmup"></a>

## 纯土温种植的暖棚测量

- 时间　2026-11-19 23:23:52 +0800
- 作者　OpenAI Codex
- 状态　两个 Forge 实景测量完成，临时测量源码和日志位于 `build/soil-farm-probe/`。
- 范围　`WorldTemperature.material`、`MinecraftThermalInput.materialOrEnvironment`、普通耕地相变、黑麦生长温度。

### 条件

使用当前工作树的物理热场，在固定自然背景约 −5°C 和 −20°C 下各测两间 3×3、净高 3 格的木墙玻璃顶暖棚。分别使用真实点燃的篝火和组装的 T1 能量塔，保持热源工作。两株黑麦种在湿润耕地，耕地与热风入口同高。一块紧邻热风入口，另一块沿出风方向偏移两格并横向偏移一格。热风入口留空。固定白天并将随机刻设为 0，记录 `LIVE` 材料读数、焓、光照和种植状态。时间按每秒 20 游戏刻折算。

### 观测

- −5°C 背景，供热 6 分 40 秒时，邻近两块耕地分别是 5.09°C（篝火）与 3.75°C（T1）。10 分钟时较远两块分别是 5.67°C 和 4.10°C。
- −20°C 背景，供热 3 分 20 秒时，两间棚的土仍在 −10°C 的冻结相变平台，热风入口分别约 29°C 和 23°C。焓随后增加并脱离平台。13 分 20 秒时近处土温达到 10.99°C 和 6.03°C。20 分钟时，近处土温为 37.96°C 和 28.24°C，远处为 12.12°C 和 6.36°C。黑麦的默认生长区间为 2–22°C。
- −20°C 背景的 T1 在初始一刻提供约 4°C 的 `ESTIMATED` 环境估算。物理记录发布后显示 `LIVE` 的 −10°C。估算可含塔保底。生长条目和温度计分别查询不同对象。

Forge 两个场景通过。完整轨迹见 `build/soil-farm-probe/extended.log`。测量覆盖耕地达到生长门槛及过热的时间窗口，完整收获周期需开启随机刻并使用选定的种植判定另测。当前实现合同见[作物温度与肥料](../docs/climate/crop-fertilization.md)。`git diff --check` 通过。

<a name="farmland-vent-balance"></a>

## 暖棚散热与黑麦纯土温种植区间

- 时间　2026-11-19 23:37:52 +0800
- 作者　OpenAI Codex
- 状态　完成现行 Forge 温场测量及同布局通风对照。
- 范围　`MaterialThermalLaw.initialBranch`、`MinecraftThermalInput.materialOrEnvironment`、`CampfireGameplay`、普通耕地冻结配方、黑麦生长区间。

沿用约 −20°C 固定自然背景、3×3 木墙玻璃顶试验，远离进热风的一侧在屋顶开一格。保持篝火和成形 T1 持续供热，固定白天与随机刻 0，测 48,000 tick。篝火延长燃烧由夹具提供，单次装填上限为 19,200 tick。数组中的时间按 20 tick/s 换算。

封顶棚中，黑麦普通生长温区 2–22°C 在入口旁的耕地仅覆盖约 5.23 分钟和 6.13 分钟，分别对应篝火与 T1。时间由测量点间线性插值。第 20 分钟，两块近处土温为 37.96°C 与 28.24°C。黑麦 `grow_time_days` 为 9。

天窗口控制住了热风温度，近处土在第 20 分钟分别为 −10.00°C 与 −5.36°C。第 40 分钟的作物格物理空气和土温分别为篝火 7.51°C / −2.01°C、T1 18.59°C / 6.04°C。T1 远处作物格物理空气为 21.71°C，土为 −4.67°C。冻结平台期间材料焓持续回升，随后才由 −10°C 向上变化。普通耕地升温 1 K 需要 6,600 J，并受相变进度约束。持续输入和棚内散热决定后续温度，热容量决定响应时间。

完整的 Forge 日志为 `build/soil-farm-probe/vented.log`，该场景通过。封顶基准为 `build/soil-farm-probe/extended.log`。材料曲线和试验夹具也在该目录。本次文档影响为测量日记，现行农业合同见[作物温度与肥料](../docs/climate/crop-fertilization.md)。

<a name="farmland-rye-harvest"></a>

## 一级塔暖棚的纯土温黑麦收获试验

- 时间　2026-11-20 00:06:08 +08:00
- 作者　OpenAI Codex
- 状态　完成一轮对称耕地、可开天窗的纯土温收获验证。
- 范围　`WorldTemperature.material`、`RyeBlock`、`ClimateCommonEvents.beforeCropGrow`、`plant_temperature/rye_block.json`、一级塔侧出风口。

### 场景与结果

两轮均用成形的一级塔持续供热，固定白天与约 −20°C 自然背景，耕地预设为湿润度 7。原生随机刻设为 0，各作物按 `randomTickSpeed=3` 的单格抽中概率 `1 − (1 − 1/4096)^3` 调用原生 `randomTick`。临时解析覆盖仅在该调用期间让生产生长检查读到对应耕地的实测材料温度。时间按 20 tick/s 换算。

- 常开天窗、近处一格和远侧角落各一块耕地。持续加热 288,000 tick（240 分钟），土温为 2.925°C、0.882°C。两块地始终未同时达到试验采用的 5–20°C 播种门槛。
- 对称布局采用内宽 3×3、内高 3 格的木墙玻璃顶棚。从东出风口的目标空气格 `(0, 0, 0)` 起，耕地位于 `(1, 0, −1)` 和 `(1, 0, 1)`，各自下一格垫云杉木板。棚底在 `y=−2`，玻璃顶在 `y=2`，天窗为 `(2, 2, 0)`。每 200 tick 检查土温，任一块达到 16°C 时开窗。冷处降到 12°C 且暖处不高于 14°C 时关窗。本轮只触发了一次开窗。
- 关窗预热 16,800 tick（14 分钟）后，两块土均约 5.36°C，播种。第 22,200 tick 开窗，播种后采样峰值分别为 20.23°C、20.27°C，随后回落到约 15.7–15.8°C。两株分别在第 202,976 和 165,006 tick 达到年龄 7，相当于播种后 155.1、123.5 分钟。各产出 1 份 `frostedheart:rye`，并分别掉落 2、3 粒种子。两轮的地形和田位有差异，数据用于检验各自的布置及操作。

### 设计与后续验证

以一级塔供热的玻璃小棚作为纯土温黑麦的起点。用同一排的耕地减少温差，先关窗暖土，再散热维持生长。作物生长、存活和作物面板可共用根部材料温度。默认黑麦在 2–22°C 可生长，在 12–22°C 加速。存活区间内的停长建议保留年龄，温度回到生长区间后接续。

证据保存在 `build/soil-farm-probe/harvest-cycle.log` 和 `build/soil-farm-probe/harvest-controlled.log`。夹具为同目录下 `src/FarmlandHarvestProbe.java`，使用 `harvest.init.gradle` 加入 GameTest 源集。Java 17、`FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep`、命名空间 `frostedheart_farmland_harvest_trial`。`FH_FARMLAND_ROOF_CONTROL=1` 选择对称布局及天窗控制。正式行为以[作物温度与肥料](../docs/climate/crop-fertilization.md)为准，本轮写入设计验证记录。后续验证正常昼夜、自然灌溉、停热后的存活和恢复，以及多块连排耕地的收获时间。

<a name="world-temperature-block-query"></a>

## 统一方块位置取温

- 时间　2026-11-20 00:56:13 +08:00
- 作者　OpenAI Codex
- 状态　已实现，Forge 定向回归通过。
- 范围　`WorldTemperature.getTempAtBlock`、位置取温调用方、`MinecraftThermalInput.gameplayPassiveEnvironment`。

### 实现

`WorldTemperature.getTempAtBlock(LevelReader, BlockPos)` 统一返回摄氏度。服务端主线程优先使用可读的活动或休眠材料温度，空气格及材料数据不可用时使用位置环境值。环境值依次取可用空气温度或自然背景，再合成解析场。温度探针对准方块时使用这个入口，提示文字显示“此处温度”。研究观察、实验条件和战利品等位置查询也使用统一接口。

作物生长、降雪和环境温度传感器直接使用环境采样。土壤温度计和相变继续通过 `MaterialSample` 区分实际材料、估算与等待材料数据的情况。

### 验证

Java 17 `compileGameTestJava` 通过。Forge 使用独立编译输出和测试存档，7 个命名空间的 43 项必需用例全部通过。`blockTemperatureReadsMaterialOrLocalEnvironment` 覆盖自然背景、空气解析场、无记录材料回退、休眠材料优先和移除后的读数。其余用例覆盖作物、能量塔、传感器、薄冰、栽培橡树及气候集成。日志位于 `build/block-temperature-verification/gradle-gametest.log`。

同步更新了世界温度、作物、薄冰和研究系统的代码锚点及查询说明。活动材料的红外发布用例随本轮源码编译，下次全量热场回归会运行该用例。

<a name="world-temperature-readers"></a>

## 环境与材料读数统一入口

- 时间　2026-11-20 01:34:01 +08:00
- 作者　OpenAI Codex
- 状态　接线完成，Forge 定向复测通过。
- 范围　`WorldTemperature.getTempAtBlock`、玩家环境与物品换热采样、`BlockScanner.sampleTemperature`、材料仪表与冰体相变。

### 实现

普通坐标查询使用 `WorldTemperature.getTempAtBlock(level, pos)`。作物、积雪、环境传感器、冰体热裂和背包换热使用同名入口的 `SURROUNDINGS`。冰体相变使用 `PHASE_CHANGE`，土壤温度计通过 `MaterialSample` 得到读数来源。材料候选发布在读取期间失效时，后续采样会重新判断来源，清除被拒绝的活动读数。城镇通过 `LoadedTownWorld` 查群系，与普通位置共享自然背景公式。

`samplePlayerEnvironment`、`sampleDroppedItemEnvironment` 和 `samplePlacedReservoirEnvironment` 接管采样位置、自然背景与回退。换热调用方使用返回的摄氏温度以及复用的辐射样本。核对玩法层，底层点采样调用集中在 `WorldTemperature`，红外网络路径使用自己的批量快照。

### 验证

- 第一轮 84 项 Forge GameTest 全部通过，覆盖取温分支、作物、环境传感器、薄冰、冰锥、城镇房屋和玩家体温。日志位于 `build/block-temperature-verification/gradle-facade-tests.log`。
- 加入蓄热物品与红外后运行 91 项，89 项通过。薄冰用例使用只覆盖一个区段的临时材料记录时，合跑的休眠维护扫描曾越界。改用整区块范围的记录。扩展回归剩余失败为薄冰裂纹进度和冰锥支撑检查的 tick 断言。日志位于 `build/block-temperature-verification/gradle-facade-final-tests.log`。
- 薄冰用例等待试块所在区块进入实体 tick，使用明确的材料温度。冰锥用例核对支撑检查的下个 tick 调度，再等待真实方块 tick 分发。35 项定向复测全部通过，含两项先前失败、背包与掉落物换热、放置的蓄热块及活动材料的红外读数。日志位于 `build/block-temperature-verification/gradle-facade-ice-focused.log`。
- `git diff --check` 通过。此次验证由上述定向运行组成。

更新了世界温度、作物、薄冰与冰锥、体温与物品换热和城镇室温文档中的入口与采样规则。

<a name="campfire-six-face-heat"></a>

## 篝火本体与六面换热

- 时间　2026-11-20 01:36:25 +08:00
- 作者　OpenAI Codex
- 状态　已实现，Forge 定向回归通过。
- 范围　`MinecraftPhysicalSourceProfile.campfire`、`PhysicalSourceSpatialIndex.refreshTargets`、`WorkerPhysicalSourceBindings`、`MinecraftThermalProfiles` 和材料热输入保存。

### 实现

普通篝火和灵魂篝火的默认 `8,000 W` 分成上方空气 `4,800 W`、本体 `1,600 W`、直接辐射 `1,600 W`。本体得到扣除辐射后功率的四分之一，经 `SOURCE_MATERIAL` 端口按材料身份交给现有焓温场。模型使用 `900 J/K` 的本体显热容量和每面 `3 W/K` 的材料导热参数。相邻空气或材料通过现有成对换热取得热量。熄灭时结束供能，本体继续散热。

六面被石块包围的首轮场景暴露了温场分块边界。旧扩张条件依赖边界空气或接触空气的材料，完全包围的篝火无法触发这些条件。`refreshTargets` 随本体端口保留直接邻格所在的 Brick，一个篝火最多保留四个。存档把未交付的本体输入与篝火状态 ID 一起保存，已交付部分由材料焓表达。

### 验证

Java 17 编译通过。最终 Forge 回归的 43 项必需用例全部通过。`blockedCampfireHeatsItsBodyAndAllSixSolidNeighbors` 用真实点火验证六面贴石块时本体和每面升温，覆盖跨 Chunk 和 Section 的邻格、方块测温及铲灭后的余温。`originalCheckpointQueueCapturesAcceptedAndClaimOnceWhileTheTrialContinues` 验证尚待交付的本体热量只计一次并带有材料状态 ID。另覆盖风道封堵与恢复、源时序、局部混合和容量等待。最终日志在 `build/campfire-six-face-final/gradle-gametest-retry.log`。

首轮 54 项扩展回归记录在 `build/campfire-six-face-verification/gradle-gametest.log`。定向复测处理了受功率分配影响的热量期望和分块覆盖。该轮的 `sourceAddedAfterSnapshotRecoversWhenCapacityReturns`、`sourceInputsSurviveRealExtinctionAndReplacement`、`physicalHeatingCanExceedTheBoostedFloor` 仍待分别确认。旧热源生命周期夹具使用了固定温场不调用的 `FrozenSources` 观察。大规模服务器性能待测。

### 文档

已更新[供热与热网](../docs/climate/heat-production-and-network.md)、[运行时合同](../docs/climate/thermal-runtime-control.md)、[作物温度](../docs/climate/crop-fertilization.md)和[旧玩法对照](../docs/climate/thermal-legacy-gameplay-parity.md)。

<a name="farmland-conductance-calibration"></a>

## 普通耕地导热标定与暖棚温控试验

- 时间　2026-11-20 01:37:31 +08:00
- 作者　OpenAI Codex
- 状态　普通耕地导热默认值已落地，Forge 已读取并验证配方。暖棚控温为试验结果。
- 范围　`minecraft:farmland`、`StateTransitionData.CODEC`、`MinecraftThermalProfiles`、一级塔与篝火暖棚。

### 参数与取舍

`state_transition.xlsx` 的 `block_temperature!E29` 与 `state_transition/minecraft/farmland.json` 设 `conductance_w_per_k = 1.5`，覆盖普通耕地所有湿度。热容为 6,600 J/K，焓偏移为 380,000 J，冻结边为 −10°C，目标为 `frostedheart:dirt_permafrost`。工作簿包核对到单个单元格变化，并保留原有表格和格式。生长温度的现行合同见[作物温度与肥料](../docs/climate/crop-fertilization.md)。

首轮预筛比较了 1.0、1.5、2.0 W/K、4,400 J/K 和 −15°C 冻结边候选。1.5 W/K 的目的在于缩短接触换热下的预热时间。第 24 分钟停止供热，5 分钟后恢复。固定 −20°C 的一级塔预筛中，1.5 W/K 的耕地最低为 12.18°C，2.0 W/K 为 9.73°C。两组的播种时间分别为 13 分钟与 11 分 20 秒。预筛的 1.0 W/K 塔棚开窗后仍持续过热，后续在同高度和相同耕地排布的两间棚复测。

### Forge 对照

两间小棚各一排三格湿润耕地，峭壁和树冠之外同高度建造。温度每 200 tick 采样。时间按 20 TPS 换算。使用实际热源和材料求解。一级塔收获试验推进昼夜，以中间耕地的 −25 至 −20°C 自然背景作为夹具控制范围。作物按默认抽中频率调用原生生长钩子，读数临时使用实测土温。小棚加照明，耕地保湿，生产随机枯萎、灌溉和耕地冻土转换另有验证需求。

| 场景 | 原值 1.0 W/K | 定标 1.5 W/K |
|---|---|---|
| −5°C 篝火棚，整排达到 5°C，最后一轮复测 | 8 分 30 秒 | 7 分钟 |
| 同一轮篝火棚停热 5 分钟，最低土温 | 17.76°C | 17.72°C |
| 一级塔，整排达到 5°C | 15 分 40 秒 | 12 分 50 秒 |
| 一级塔按土温停塔，5 小时试验中成熟的黑麦 | 1/3 | 3/3 |
| 一级塔按土温停塔，土温最高值 | 52.38°C | 54.25°C |
| 一级塔提前限制暖风，整排均在黑麦生长区的采样占比 | 66.67% | 71.29% |
| 一级塔提前限制暖风，土温最高值 | 12.11°C | 12.31°C |

暖风试验在达到播种条件后开窗。作物所在格最高空气温度达到 30°C，或最高土温达到 15°C，就暂停热源。最高空气温度回落到 23°C 及以下、最低土温回落到 12°C 及以下、最高土温低于 15°C 时恢复。夹具以实测空气提前减热，避免余热继续把根区推过 22°C。该组阈值使土温最低到 −10°C，两组各三株在时限内均为年龄 7 以下，适合作为过量散热和供热不足的对照。

### 证据与文档

`build/soil-calibration/sweep-run.log` 为首轮，`controlled-harvest-run.log` 为土温控塔，`air-limited-validation.log` 为限制暖风与生产配方加载验证。Java 17，`frostedheart_soil_calibration`，`FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep`。倍率对照采用 `FH_SOIL_CALIBRATION_MATCHED=1` 与 `FH_SOIL_CALIBRATION_CONTROLLED=1`，暖风轮次另启用 `FH_SOIL_CALIBRATION_AIR_LIMITED=1`。夹具和 Gradle 接线在该目录。最后一轮 2 项场景执行通过，温区比例和成熟数按日志分别判断。

已更新[材料参数合同](../docs/climate/heat-production-and-network.md#materials-and-phase)，为 `minecraft:farmland` 记录上述源默认值。

<a name="soil-thermometer-material-first"></a>

## 土壤温度计统一材料优先取温

- 时间　2026-11-20 02:02:03 +08:00
- 作者　OpenAI Codex
- 状态　已接线，Forge 定向验证通过。
- 范围　`WorldTemperature.getTempAtBlock`、`SoilThermometer`、`SoilThermometerUpdatePacket.forCrop`、`WorldTemperature.checkPlantStatus`。

### 实现

方块位置的两种常用签名进入同一方法，先读该坐标的活动或休眠材料记录，读不到时返回该坐标的环境气温。需要显示来源时，传入 `MaterialSample` 记录真实材料读数。气温回退会清空该输出，聊天显示「此处环境气温」。右键聊天复用 `FHTemperatureDisplayPacket` 按客户端单位设置显示。

作物悬浮提示与服务器生长、温度死亡判定都用这一入口读取作物格。右键直接测耕地时，取样坐标是准星命中的耕地格。

### 验证

Java 17 生产与 GameTest 编译通过。`frostedheart_material_runtime,frostedheart_crop_temperature,frostedheart_fertilizer,frostedheart_thin_ice` 的 23 项必需用例全部通过。材料读取验证覆盖无记录回退、冷材料优先于暖场、方块移除后恢复空气读数和清空旧材料样本。小麦用例验证在暖场中插入冷材料记录后，面板与服务器判定一起采用该材料温度。日志位于 `build/block-temperature-verification/gradle-material-first-gametest.log`。`git diff --check` 通过。

世界温度、作物温度和热系统接入文档已更新。图形客户端的聊天样式未在本轮单独复测。

<a name="crop-material-soil-heat-paths"></a>

## 作物材料读数与耕地换热路径复测

- 时间　2026-11-20 02:04:42 +08:00
- 作者　OpenAI Codex
- 状态　Forge 4 项用例通过。耕地换热结果适用于对应小棚。
- 范围　`WorldTemperature.checkPlantStatus`、`SoilThermometerUpdatePacket.forCrop`、`TiledThermalField.conductance`、`minecraft:farmland`。

### 作物读数

工作区的生长判定和面板均以作物坐标调用 `WorldTemperature.getTempAtBlock`。可读的活动或休眠材料记录优先。环境回退使用作物格中心的空气读数，可经该格的通风路径取样，缺少空气数据时使用自然背景，再合成解析场。能量塔的解析保底只参与环境读数，实际出风口另向物理温场供热。小麦和黑麦归入 `INSULATING_FABRIC`，每面 `0.12 W/K`，有效热容 `720 J/K`。

`ThermalCropGameTests.wheatSamplesHeatAtTheCrop` 验证同一株小麦在无材料记录时使用解析保底，在设置 `−12°C` 休眠植株记录后使用材料读数并进入枯萎判定。实际点燃篝火后的首次暖空气采样中，面板读出 `−6.399°C`。随后加入塔保底 `18.255°C` 时，面板仍读该植株记录。验证覆盖优先级和即时取样。

### 耕地换热

复用 `SoilCalibrationProbe` 的三格湿润耕地小棚，以生产配方 `1.5 W/K`、`6,600 J/K` 和 `−10°C` 冻结边运行。`SoilHeatPathProbe` 每 200 tick 抽取已发布的格子温度、自然背景和材料焓。空气和实体列为六个直接接触面按 `TiledThermalField.conductance` 与各格相对自然背景的温差得出的瞬时功率估计，正值表示流入中间耕地。

| 场景与时刻 | 正上方空气 | 中间土温 | 空气净功率 | 实体净功率 |
|---|---:|---:|---:|---:|
| −20°C 一级塔棚，供热第 7 分钟 | 50.27°C | −10°C | +273.97 W | −2.82 W |
| 同一塔棚，停热后第 1 分钟 | 0.28°C | 15.41°C | −66.73 W | −7.88 W |
| −5°C 篝火棚，供热第 7 分钟 | 67.77°C | 5.43°C | +284.60 W | +4.39 W |

塔棚中间一格在第 1 至第 7 分钟维持 −10°C，期间焓从 246.93 kJ 升到 302.34 kJ，冻结分支进度从 17.65% 回退到 3.07%。第 8 分钟已升到 −9.17°C。整排达到 5°C 所需时间为塔棚 13 分钟、篝火棚 7 分钟。控制场景的耕地上方留空，阈值只用于耕地预热标定。植株的完整温度曲线和收获节奏仍需实种验证。

### 证据与文档

日志为 `build/soil-calibration/heat-path-validation-r2.log`。Java 17、`FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep`、`FH_SOIL_CALIBRATION_HEAT_PATHS=1`，以 `build/soil-calibration/calibration.init.gradle` 加载夹具，Forge 命名空间为 `frostedheart_soil_calibration,frostedheart_crop_temperature`。四项用例通过。当前取温合同见[作物温度与肥料](../docs/climate/crop-fertilization.md)和[世界温度](../docs/climate/world-climate-and-temperature.md)。本轮复测的场景数据记于本日记。

<a name="thermal-query-names"></a>

## 热查询方法命名

- 时间　2026-11-20 02:09:40 +08:00
- 作者　OpenAI Codex
- 状态　改名完成，Java 17 与 Forge 定向验证通过。
- 范围　`WorldTemperature`、`MinecraftThermalInput` 的查询方法及调用者。

### 实现

`WorldTemperature.getTempAtBlock(level, pos)` 保留为坐标取温入口。服务端主线程优先读当前可用的材料温度，再读空气或区块休眠余温，最后使用自然背景。气温路径在采样位置合成解析热场。

点查询由 `gameplayPassiveEnvironment` 改为 `sampleAirTemperature`。玩家、加权取样和物品的摄氏读数使用 `AirTemperature`。蓄热物品同时填充气温与辐射样本的查询使用 `AirAndRadiation`。已有材料记录和允许估算的查询分别命名为 `sampleMaterialTemperature` 和 `sampleMaterialTemperatureWithEstimate`。解析热场使用 `analyticFieldsAt` 与 `hasAnalyticFieldAt`，红外使用 `captureInfraredSnapshot`。命名变更同步到玩法、GameTest 和 `Scripts/thermal-oom-repro.js`。

### 验证与文档

- 首轮 122 项必需 Forge GameTest 通过，日志为 `build/thermal-query-names-validation/gradle-gametest.log`。
- 最终命名通过生产及 GameTest Java 编译。一次扩大复测在区块生成中遇到篝火 Mixin 匿名类加载失败，见 `build/thermal-query-names-validation/gradle-gametest-final.log`。
- 最终定向 Forge 重试的 23 项必需用例通过，覆盖坐标取温、材料运行时、气候覆盖和温度传感器。日志为 `build/thermal-query-names-validation/gradle-gametest-retry.log`。
- `node --check`、旧方法调用搜索和 `git diff --check` 通过。整合包 TheWinterRescue 工作树干净，脚本中没有相关 Java 方法调用。
- 更新了世界温度、玩家体温和热系统接入文档中的方法锚点，也同步了采用这些入口的温度定标说明。

<a name="world-temperature-consumer-audit"></a>

## 方块取温调用核对

- Time: `2026-11-20 02:23:09 +08:00`
- Author: `OpenAI Codex`
- Status: `completed`
- Scope: `WorldTemperature.getTempAtBlock`、栽培橡树与蘑菇、红石温度传感器、暖石开发命令。

### Completed

- `CultivatedOakBlock.randomTick`、`WorldTemperature.canTreeGrow` 和 `canBigTreeGenerate` 使用两参数入口，先取同格材料温度，缺少可读记录时取环境气温。预留的 `VanillaClimate.getTemperature` 直接复用位置读数，已计入的海拔项只应用一次。
- `WarmStoneTestCommand` 的 `environment` 预设与背包换热共用当地气温读数，使核心与外层的初温都能反映局部供热。

### Decisions

- `TemperatureSensorBlockEntity.refresh` 以气温驱动界面和红石。热容或接触换热可使外壳温度与气温不同，环境控制以气温为目标。降雪、冰体热裂、相变、城镇室温、玩家与蓄热物品换热、预报和红外按各自所需温度或采样方式读取。
- 更新世界温度、栽培橡树和玩家温度文档，记录统一入口和专用读数的用途。

### Validation

- Java 17 的生产与 GameTest 编译通过。材料读数、栽培橡树、传感器、作物温度、肥料、薄冰和冰锥共 47 项现有 Forge GameTest 全部通过。日志位于 `build/block-temperature-verification/gradle-reader-audit.log`。
- `git diff --check` 通过。复扫生产调用，玩法侧的方块点采样经 `WorldTemperature` 进入热系统，底层自然背景和红外快照保留独立调用。

### Remaining

- 无。本轮验证范围为源码核对和现有服务端 GameTest。

<a name="planted-crop-growth-material-temperature"></a>

## 植株材料优先后的暖棚生长实测

- 时间　2026-11-20 02:30:12 +08:00
- 作者　OpenAI Codex
- 状态　两场 Forge GameTest 通过。记录对应棚型与供热操作的小样本结果。
- 范围　`WorldTemperature.checkPlantStatus`、`ClimateBlockUpdates.tickAfterThunder`、`TiledThermalField.projectedEnthalpy`、`plant_temperature/wheat.json`、`plant_temperature/rye_block.json`。

### 方法

复用三格耕地暖棚，每棚种中央一株小麦与两侧各一株黑麦。一级塔棚的参考自然背景按昼夜在 −25 至 −20°C 之间变化，篝火棚为 −10 至 −5°C。使用 `minecraft:farmland` 生产配方，以灯补光，夹具维持湿润，默认无肥料。整排土温首次进入 5–20°C 即播种。持续供热，篝火定期补燃料，最暖一格土温达到 16°C 时拆除单格天窗，最冷一格降至 12°C 且最暖一格不超过 14°C 时关窗。

夹具对耕地所在区块每游戏 tick 执行一次 `ClimateBlockUpdates.tickAfterThunder`，参数为默认随机刻频率 3。该生产路径运行相变、随机枯萎和原版生长事件。每 200 tick 通过与生长钩子相同的取温入口记录植株温度及来源，同时采样同格环境温度和脚下土温。成熟时核对真实区块的方块掉落。日志中的时间按 20 TPS 折算。

### 结果

| 棚 | 作物 | 播种后首次测到可生长 | 实际结果 |
|---|---|---:|---|
| 一级塔，预热 12 分 50 秒 | 小麦 1 株 | 未达到 | 10.25 秒后变枯萎灌木，末次采样植株 −19.70°C，环境 89.27°C，土 5.77°C。 |
| 一级塔 | 黑麦 2 株 | 14 分 20 秒 | 两株都长过且发生一次年龄回退；69 分 56 秒、78 分 53 秒枯萎，两株末次植株读数分别为 51.67°C、52.95°C。 |
| 篝火，预热 6 分 50 秒 | 小麦 1 株 | 5 分 40 秒 | 第 87 分 53 秒达到年龄 7，成熟掉落有 1 个小麦。 |
| 篝火 | 黑麦 2 株 | 4 分钟 | 两株都长过且发生一次年龄回退；123 分 14 秒、137 分 51 秒枯萎，两株末次植株读数分别为 31.86°C、37.47°C。 |

首个采样使用环境回退，后续样本使用 `LIVE` 植株温度。`TiledThermalField.projectedEnthalpy` 给新材料使用格内自然背景的焓，已暖起来的耕地和空气再参与换热。小麦的存活区间为 −5 至 35°C，生长区间为 5 至 30°C。黑麦的存活区间为 −25 至 30°C，生长区间为 2 至 22°C。塔棚复现了暖土之后的低温起苗和持续供热之后的过热。篝火棚复现了小麦成熟与黑麦后来过热。需要实种对照量化不同棚型、通风和相同自然背景下的影响。

### 验证与文档

`build/soil-calibration/planted-crop-growth.log`，Java 17，`FH_SOIL_CALIBRATION_NATIVE_CROPS=1`、`FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep`，通过 `build/soil-calibration/calibration.init.gradle` 接入 `frostedheart_soil_calibration`。两项用例均完成记录，`BUILD SUCCESSFUL`。测试夹具的 profiler 栈配平后 `compileGameTestJava` 通过。实验记录存于本日记，现行行为参见[作物温度与肥料](../docs/climate/crop-fertilization.md)。

<a name="crop-conductance-2p5"></a>

## 作物每面换热导纳设为 2.5 W/K

- 时间　2026-11-20 02:56:38 +08:00
- 作者　OpenAI Codex
- 状态　已实现，Java 17 定向 Forge GameTest 4/4 通过。
- 范围　`MinecraftThermalProfiles.classify`、`GameplayMaterial.CROP`、`BlockTags.CROPS`、`ThermalCropGameTests`。

### 实现

带 `#minecraft:crops` 标签的作物使用独立的 `GameplayMaterial.CROP`。按用户指定值，源码默认每面导纳为 `2.5 W/K`，有效热容为 `720 J/K`。`frostedheart:state_transition` 配方的 `conductance_w_per_k` 可覆盖该值。Forge 用生产 profile 核对小麦、黑麦及同批对照的羊毛和云杉树叶。

### 定标与实测

三格小棚按整排土温达到 5°C 预热后打开单格天窗，等作物高度的相邻物理空气温度降至 26°C 或以下再播种。按相邻空气的最高实测值 25°C 切断供热、17°C 恢复。播种 20 分钟后停热 5 分钟，再恢复供热。两侧各为黑麦，中间为小麦。采样只统计已发布的植株材料读数，时间按 20 TPS 换算。夹具在温度定标期间暂停原生随机刻。

一级塔以 −20°C 自然背景比较了 `0.12`、`0.45`、`0.6`、`0.9`、`1.2`、`1.5 W/K`。中间小麦在前四档的供热阶段均未到 5°C，`1.2 W/K` 在播种后 17 分 40 秒到达，`1.5 W/K` 在 13 分 40 秒到达。`2.5 W/K` 用生产配置完成定向复测。

| 棚与自然背景 | 黑麦首次达到 2°C | 小麦首次达到 5°C | 停热 5 分钟内最低植株温度 |
|---|---:|---:|---:|
| 篝火，−5°C | 50 秒 | 1 分 40 秒 | 9.04°C |
| 一级塔，−20°C | 3 分 40 秒 | 8 分 10 秒 | −2.29°C |

一级塔小麦播种后 1 分 50 秒首次达到 −5°C 存活下限，冷启动期间仍有随机枯萎风险。表中结果适用于实测风温控制、通风和停热操作。成熟周期属于后续独立实种验证范围。

### 证据与文档

`build/soil-calibration/crop-conductance-sweep-r3.log` 和 `crop-conductance-followup.log` 记录前六档。生产值复测见 `build/soil-calibration/crop-conductance-25-targeted.log`。使用 `FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep`、`calibration.init.gradle`、`crop-only.init.gradle` 和命名空间 `frostedheart_crop_conductance_production,frostedheart_crop_temperature`。定向编译排除了工作区内正处于接口变更中的材料扩张基准。`git diff --check` 通过。已同步[作物温度与肥料](../docs/climate/crop-fertilization.md#生长温度)及[供热与热网](../docs/climate/heat-production-and-network.md#materials-and-phase)。
