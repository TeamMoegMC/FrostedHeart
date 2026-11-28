# T1/T2 能量塔中心增温与四向排热

以下保留 2026 年 11 月 17 日三个阶段的设计、改动和验证。各节按当时的源码状态记录，后续功率调整见[固定排热功率记录](2026-11-17_23-38-14_generator-tier-exhaust-6kw-8kw.md)。

- [中心增温初版](#generator-center-boost-t1-t2)
- [中心增温调整至 5°C](#generator-center-boost-5c)
- [四向物理排热](#generator-four-horizontal-outlets)
- [Port 记录与旧容量的历史核对](2026-11-17_22-29-50_generator-port-history-internal-heat.md)

<a name="generator-center-boost-t1-t2"></a>

## T1/T2 能量塔平滑中心增温

- Time: `2026-11-17 20:58:39 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; compilation and 11 focused Forge GameTests passed`
- Scope: `GeneratorData.publishGameplayHeat`, `GeneratorHeatFieldModel`, `ThermalAnalyticField`, `ThermalAnalyticFieldIndex`, `MinecraftGameplayFields`, `InfraredCapture`

### Completed

- T1、T2 共用 `GeneratorData.publishGameplayHeat`，统一在原解析保底上增加中心10°C，平滑减弱至原半径边缘的额外0°C。
- `GeneratorHeatFieldModel.CENTER_BOOST_CELSIUS = 10.0D` 为固定源码参数。基础温差仍由 `getTempMod()` 提供，半径仍由 `getRadius()` 提供。
- 设 `d` 为查询点至原球心的三维距离，`R` 为半径，均以方块为单位；范围内温差为 `D0 + 10 * (1 - d²/R²)²` 摄氏度。中心、四分之一半径、半径一半、四分之三半径、边缘的额外增温分别为10、8.7890625、5.625、1.9140625、0°C。
- 多塔继续取各自在查询点的最大有效温差，合成 `max(physicalOrFallback, natural + maximumLocalDelta)`。玩家、作物、城镇、物品、相变保底和红外均按各自真实采样位置取值。
- 未变化的塔报告继续复用同一不可变场定义；原常值构造/API以0中心增益委托，命令与Boss合成顺序保留。

### Implementation Decisions

- 在既有球形解析场中增加中心增益参数，统一处理所有消费者；T1/T2没有各自复制一套计算。
- `temperatureCAt(x,y,z)` 同时判断范围并求值，范围外返回NaN供既有采样循环跳过；球形增强路径仅计算一次距离平方。
- 半径平方倒数在场创建时预计算。每次增强取值只做加减乘与比较，没有除法、开平方或新对象。
- 增益在球心和球面边缘的径向斜率均为零。原边界基础温差和范围外行为保留。
- 沿用原有 O(F) 场列表扫描、物理源与余热生命周期，没有新增网格、空间缓存、定时更新器或逐块状态。

### Validation

- `./gradlew.bat --offline compileJava` 通过。
- 最终执行：`./gradlew.bat --offline runGameTestServer -PgameTestNamespaces=frostedheart_generator_heat -PgameTestDirectory=build/generator-center-boost-gametest`，11/11通过，`BUILD SUCCESSFUL in 2m 14s`。
- 核对六个轴向的中心到边缘五个采样距离、范围外、人物/作物/方块/城镇/相变路径、物理高温优先、多塔最大值、常值API和命令/Boss顺序。
- 复用真实T1/T2形成、正常机器tick、配方重载恢复、源停机余热、拆除、团队转移和已卸载塔查询场景；源区块不会因解析查询重新加载。
- 红外验证中心、半径一半、边缘、范围缩小、升温、移除和实际编解码。
- 真实封闭排气口样本：自然温度约-6.55°C，增强后当地保底约32.2391°C，物理空气约33.1715°C，最终查询约33.1715°C；露天自然约-10.024°C，物理空气约-6.3163°C，最终保底约28.7651°C。
- 首次测试入口曾把命名空间同时写进 `@GameTest.template`，在用例运行前退出；改为共享结构名 `phase0a_empty` 后，初版10/10及最终优化版11/11均通过。结构通过现有GameTest资源准备任务提供。
- `git diff --check` 通过。

本机同一最终构建中的生产被动查询采样：每组预热20,000次后测量20,000次，查询距球心2格，所有被测场均覆盖该点，物理运行时关闭。

| 场数量 | 常值场 ns/query | 增强场 ns/query | 常值场测量批次分配B | 增强场测量批次分配B |
|---:|---:|---:|---:|---:|
| 1 | 81.180 | 258.905 | 0 | 0 |
| 16 | 419.555 | 203.665 | 2904 | 0 |
| 128 | 762.775 | 647.795 | 0 | 0 |

这些是一次JVM运行中的局部采样，受JIT和执行顺序影响，不能据此宣称增强比常值更快或是所有负载中的绝对最优。增强组三组均无测得堆分配，查询复杂度仍为O(F)。

同轮24格半径塔的红外服务器capture采样：预热256次后测100次，增量请求中位数3.9727ms、p95 4.2348ms；完整请求中位数4.0436ms、p95 4.2142ms。它包含窗口处理与显示编码，不是单点温差运算耗时，也不是网络或客户端帧率测量。

运行日志、世界及崩溃诊断留在忽略的 `build/generator-center-boost-*`；最终日志为 `build/generator-center-boost-optimized.log`。

### Documentation Impact

- 更新 `docs/climate/world-climate-and-temperature.md` 的解析合成、公式、单位、T1/T2共用路径、红外与物品采样说明。
- 更新 `docs/climate/heat-production-and-network.md` 的能量塔解析保底说明。
- `src/gametest/README.md` 增加专用命名空间入口；新增 `GeneratorHeatFieldGameTests` 复用已有真实场景，并修正其原常值预期。

### Remaining

- 本次未做客户端画面验收或完整整服负载对照；服务端玩法查询、红外数据与机器生命周期的验证范围如上。

<a name="generator-center-boost-5c"></a>

## T1/T2 能量塔中心增温调整为5°C

- Time: `2026-11-17 21:18:18 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; compilation and 11 focused Forge GameTests passed`
- Scope: `GeneratorHeatFieldModel.CENTER_BOOST_CELSIUS`、能量塔现有验证用例及气候文档
- Follows: [T1/T2能量塔平滑中心增温](#generator-center-boost-t1-t2)

### Completed

- 按用户要求，将中心额外增温从10°C调整为5°C，`CENTER_BOOST_CELSIUS = 5.0D`。T1、T2继续通过共用的 `GeneratorData.publishGameplayHeat` 使用这一参数。
- 范围内的有效温差为 `D(d) = D0 + 5 * (1 - d²/R²)²`。`D0` 为原基础温差，单位为摄氏温差；`d` 为距原球心的三维距离，`R` 为原半径，二者单位均为方块，`0 <= d <= R`。
- 中心、四分之一半径、半径一半、四分之三半径、边缘的额外增温分别为5、4.39453125、2.8125、0.95703125、0°C。
- 保留原平滑曲线、共用消费者路径、距离平方复用和半径倒数预计算；本次只调整增益及相关预期，没有新增运行机制或测试用例。
- 更新现有中心/半径分布、多塔最大值、人物、物品、卸载和红外测试预期；前条10°C记录作为历史保留，当前数值以源码和本条为准。

### Validation

- 执行 `./gradlew.bat --offline runGameTestServer -PgameTestNamespaces=frostedheart_generator_heat -PgameTestDirectory=build/generator-center-boost-gametest`。
- 生产源码及GameTest编译通过，11/11项通过，`BUILD SUCCESSFUL in 2m 7s`。
- 覆盖真实T1/T2正常运行、余热、重载、拆除、团队转移、已卸载源区块，及玩家/作物/城镇/物品/相变保底、红外与多塔重叠。
- `git diff --check` 通过。运行日志留在忽略的 `build/generator-center-boost-5c.log`。

### Documentation Impact

- 更新 `docs/climate/world-climate-and-temperature.md` 的中心增益、半径一半增益和温度例子。
- 更新 `docs/climate/heat-production-and-network.md` 的5°C中心增益及公式。

### Remaining

- 本次未做客户端画面验收；服务端验证范围如上。

<a name="generator-four-horizontal-outlets"></a>

## T1/T2 能量塔四向物理排热

- Time: `2026-11-17 22:21:49 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; compilation and 14 focused Forge GameTests passed`
- Scope: `GeneratorLogic.tickFuel`、`MinecraftPhysicalSourceProfile.GENERATOR`、`PhysicalSourceSpatialIndex`、`SpatialSourceTimeline`、`ThermalDimensionEngine`及现有能量塔场景
- Follows: [中心额外增温5°C](#generator-center-boost-5c)

### Completed

- T1、T2从原单个塔顶出口改为东、西、北、南四向排热；用户明确选择出口在塔身下部、离底座2格高。
- `GeneratorLogic.tickFuel` 将底座中心同时作为源ID位置和profile锚点，四个Air目标相对坐标分别为 `(2,2,0)`、`(-2,2,0)`、`(0,2,-2)`、`(0,2,2)`，单位为方块。目标面的法线朝向塔身。
- 保持原总功率 `10,000 W * thermalLevel`，四个对流端口各占总功率20%，另20%继续使用原辐射分账。每个塔仍只注册一个物理热源。
- 出口继续使用现有独立绑定、源Brick引用、遮挡损耗、重开恢复、源启停和卸载路径；静态profile不在机器tick中重建。
- 原解析场中心额外5°C的曲线继续独立生效。

### Capacity And Radiation Decisions

- 原生产者 `CHANNELS = 4` 只容纳公共通道及最多3个端口，`ThermalDimensionEngine` 的ledger初始化和heat-buffer上限也写成每源3口；这些是实现中的固定数组/账本容量，不是用户配置。
- 首轮真实机器运行暴露了本次遗漏的同步修改，触发 `Too many source ports`。统一为 `SpatialSourceTimeline.MAX_PORTS_PER_SOURCE = 5`，生产者的公共及端口通道、预付存储估算、ledger和heat-buffer界限据此计算。
- 主线程去重后的目标Brick存储改为 `MAX_TARGET_BRICKS = 4`，与四个Air出口的最大目标数匹配。
- 原来的3口上限满足当时最多2口的机器profile；引入四向后未扩展容量属于本次改动的遗漏，不能归因于配置，也不代表物理求解器只支持三向。
- 用户追问“辐射口”含义后，核对了修改前 `HEAD` 和当前调用链：旧 `GENERATOR` 已有 `Port.radiationLoss(1, 0.2D)`；这项在Air/material账本中记作declared loss，`PhysicalSourceSpatialIndex.visitSection` 还会将该份额的功率提供给 `RadiationService`。
- 方块的静态辐射表是另一条路径；默认 `thermal/static_fire_radiators` 只有fire/soul_fire，没有能量塔。能量塔没有静态方块辐射条目，不等于缺少动态机器源辐射接线。“辐射口”不是模型上的额外开口。
- 本轮按原80/20分配完成四向；实际到达玩家的辐射受距离、遮挡和查询预算影响，本轮未单独测量能量塔对玩家的实际辐射通量。红外显示仍从材料温度和解析场合成。

### Validation

- 最终命令：`./gradlew.bat --offline runGameTestServer -PgameTestNamespaces=frostedheart_generator_heat,frostedheart_source_admission -PgameTestDirectory=build/generator-center-boost-gametest`。PowerShell中含逗号的 `-PgameTestNamespaces=...` 参数需整体加引号。
- 生产代码与GameTest编译通过，14/14项通过，`BUILD SUCCESSFUL in 2m 28s`。
- 真实T1/T2默认朝南形成，源位置放在Chunk角部，东/南出口跨Chunk边界；每个出口都读取未合成解析保底的物理Air，要求高于自然温度0.05°C。
- 两种塔均覆盖堵住东口、其余三口继续有Air、配方重载恢复、重开东口并停机/重启后四口恢复实际升温、余热和拆除清理；燃料没有因runtime恢复重复消耗。
- 封闭东口场景自然温度约-8.37°C，当地解析保底约26.3224°C，物理Air约30.5426°C，实际查询约30.5426°C；露天东口自然约-12.066°C，物理Air约-9.8829°C，玩法保底约22.6264°C。
- 复用既有相变ACK/同Brick拓扑反复变化场景，将输入锚点调整为新profile下东口仍命中原目标；新四向源通过该回归。
- 原中心5°C、人物/作物/城镇/物品/相变、红外、团队权威与卸载检查继续通过；另运行两项source admission预付、预算压力与取消释放检查。
- `git diff --check` 通过。最终运行日志为忽略的 `build/generator-four-direction-ports5.log`；首轮诊断为 `build/generator-four-direction.log`。

### Documentation Impact

- `docs/climate/heat-production-and-network.md` 更新出口坐标、面、功率分配、绑定、容量以及动态/静态辐射路径区别。
- `docs/climate/world-climate-and-temperature.md` 补充四向物理排热并链接拥有细节的热源文档。
- `src/gametest/README.md` 更新已有专用命名空间的覆盖范围。

### Remaining

- 未做客户端画面、全部朝向/镜像实机或完整整服负载对照。四向世界坐标不依赖形成朝向；本轮真实机器场景使用默认朝南形成。
- 未做此次改动后的独立进程保存/重启验证；此次未更改NBT格式，pending heat仍保存世界坐标、面、时段和焦耳值。
