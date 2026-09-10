# 方块级热模型与材料温度闭环实施计划

- Time: `2026-09-08 21:58:58 +08:00`
- Updated: `2026-09-10`
- Author: `Codex; OpenAI GPT-6`
- Status: `ready`
- Scope: `材料节点与导热、温度查询、土壤测温、作物土壤输入、材料长波环境、休眠保存、表面红外`
- Outcome: `此前通风简化和接口修复已实施；本次新增材料闭环方案仅完成设计，尚未实施或验证性能。`

> **当前执行入口：** [材料温度闭环：完整工程方案](#material-temperature-closure)。下文此前的确定范围、实施步骤和日期记录保留为已执行方案的上下文；与新方案冲突时以新方案为准。尤其不再沿用“V0 只在暴露时建节点”“热容乘暴露面数”“所有消费者只查 transport”“红外直接合成解析场”“整个 section 的全部温度只能用 640 字节保存”这些约束。

<a id="material-temperature-closure"></a>

## 材料温度闭环：完整工程方案

- Time: `2026-09-10 00:42:55 +08:00`
- Reviewed: `2026-09-10 01:00:30 +08:00; Codex / OpenAI GPT-6`
- Author: `Codex; OpenAI GPT-6; engineering investigation and plan revision`
- Status: `ready`；下列新增行为、接口名、参数和预算是实施目标，不是当前功能说明。
- 目标：同一块材料的求解状态能够被测量、显示、用于相邻导热及玩家长波环境，并在卸载后按明确精度恢复。
- 设计依据：[创作原则](../design/creative-principles.md)、[世界设计](../design/world-design.md)；保留温度支撑生存、建设和探索的体验。没有人类设计要求真实光谱追踪或全世界逐块模拟；不修改 `design/`。
- 当前行为依据：[气候文档入口](../docs/climate/README.md)、[runtime](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[热源](../docs/climate/heat-production-and-network.md)、[玩家温度](../docs/climate/player-temperature.md)、[生命周期](../docs/climate/data-lifecycle-and-integration.md)，以及下列源码。

### 0. 工程复查结论与选型依据

上一版覆盖了功能缺口，但还不能称为最小工程方案：存在可推导的持久字段、两套屏幕温度间接寻址、过细的背景表、未核实的旧样本保留能力，以及缺少具体规则的驻留/恢复路径。本次已在对应章节直接修正，不要求实施者在两套互相冲突的建议中选择。

| 复查项 | 本次决定 | 需求与代价判断 |
|---|---|---|
| 持久 `finiteNodeCount` | 从总节点数减phase目录长度推导 | 同样O(1)，每Brick省一个计数字段和维护点 |
| R32UI对象标记后再查温 | 热表面pass直接写R16I温度 | 去掉每像素标记解码和最终合成中的3D寻址；屏幕颜色附件从4 B/pixel变2 B/pixel |
| `9×144×9`背景 | 改为`9³` Page背景，与物理初始化的Page自然边界相同 | 取样/数值存储减少16倍，并消除naturalBlock/naturalAir回退不一致 |
| 每块热材料建发射源 | 保留接收者方向采样 | 不增加随热材料数量增长的source索引与每玩家候选表 |
| 物品“沿用上次结果/下一次服务” | 删除不存在的承诺，复用当前64位置/tick缓存，超额用明确背景近似 | 不新增物品调度器、跨tick缓存或待办队列；单独核算物品射线预算 |
| 所有温度变化标红外脏 | 只标可见温度输入的变化，纯Air不标 | 材料纹理不再因室内Air波动发送重复INVALID |
| 保存/恢复泛称“接入原流程” | 明确同一cut、唯一输入流、只恢复一次、替换清理、超龄处理 | 防止旧空气均温污染材料、重复恢复余热和冷热位置互换 |

保留一个专用热表面pass作为正确性与低接入复杂度的共同基线；不再声称“多绘制一遍必然最快”。它避免对任意shaderpack的MRT输出进行侵入式改写，也避免为地形/玻璃/实体分别维护修补路径。GPU实际最优仍由第9节的同画面对照验证；代码和预算能证明删去了哪些工作，不能在实现前证明所有显卡上的全局最优。

### 1. 已核实的缺口与本轮完成标准

| 当前源码事实 | 实施后的合同 |
|---|---|
| `BrickTopologyCompiler.compileCells` 只为暴露 V0 材料建立节点；热容随暴露面积变化 | 已驻留 Brick 内每块普通材料都有一个有限热容节点；热容不随开门、遮挡和邻块改变 |
| `BrickTopologyCompiler.face` 对 V0/V0 直接返回 | 相邻材料之间有唯一导热连接；热能能够穿过多块实心墙 |
| `PagePublication.Brick.coverageSlot` 在 material-only Brick 中为 -1 | 通用节点 span 与 Air 覆盖分开解释，material-only Brick 也可读 |
| `SoilThermometer` 和 `SoilThermometerRequestPacket` 使用 `WorldTemperature.block -> gameplayPassiveEnvironment -> sampleAir` | 两个入口共用材料测温接口，明确返回实测、休眠近似或背景估计 |
| 红外编码与变化追踪只考虑 transport/Air | 材料改变能独立产生增量；按被看见的表面所属对象取温 |
| `DormantChunkThermalState.capture` 只遍历 `transportNodeCount` | 材料温度按原材料身份和方块位置保存，不混入空气均值 |
| `BrickMigrationKernel.migrate` 在dormant分支把Brick空气均温写入普通材料槽 | 删除该回退；材料只从匹配材料记录恢复，无记录用统一Page背景 |
| `PlayerThermalModel` 的长波背景等于空气温度；直接辐射只来自 source/fire/lava | 普通可见材料影响玩家平均辐射温度，热源直接辐射仍单独计算 |
| `gameplayCropEnvironment` 直接转发被动环境查询 | 根部土壤和地上环境各有明确来源，保留能量塔的作物玩法保底 |

完成不能只以编译或通用 solver 单测为准：测温工具、红外、材料热状态和保存恢复必须在同一真实世界夹具中对应起来。旧测试中“石块必须 INVALID”的断言随合同更新，不能保留后再把该测试排除。

### 2. 最小模型与范围

保留一个维度一个 worker、20-tick cut、16³ Page、4³ Brick、source ledger、arena、现有指数交换内核、Page/Brick 增量和现有预算。没有第二套世界温度引擎、材料 BlockEntity、逐块 tick、全世界导热图或逐观察者温度副本。

每个普通材料方块只保留一个温度，代表该块的有效体温，也作为六个面的表面温度近似。它不能表达同一方块的内外温差；不同方块可以有温差。V75 等部分方块仍只有一个材料/通风混合节点，不拆内部空气和材料。只有相连且无材料的 V100 空气可以合并。相变沿用独立潜热机制，见第 4 节的边界，不能把潜热槽误当普通材料温度。

本轮补上材料闭环；不增加全场材料互相辐射、光谱反射、多次反弹、实体热流反写世界、地热新模型、自动色阶、另一套可切换空气调试渲染。对火焰/岩浆原有“只向玩家提供直接辐射”的玩法边界不顺带改成新的世界功率 source。

### 3. 状态所有权与明确的数据接口

#### 3.1 一份材料状态，通用 span 地址

改造 `WorkerBrickTopology` 和 `PagePublication.Brick`：把现有 `coverageSlot` 的物理意义改为通用 `firstSlot`，保留 generation 与 transport数量。`BlockBrickLayout.nodeCount()` 返回现有 `nodeBlockMasks.length`；`PhaseCandidates.reservoirCount()` 返回现有profile目录长度；`finiteNodeCount()` 是两者之差的派生getter，不是新增字段。完整空气快路径总数/有限节点数均为1、空Brick为0。不新增逐节点坐标、种类、温度副本数组。

- `firstSlot` 指向任意非空 span，不能以是否有 transport 决定是否发布。
- 节点顺序继续为 transport、普通材料、phase；`[0, finiteNodeCount)` 都能读取普通有限热容温度。
- `resolveAirPoint` 必须继续检查 `node < transportNodeCount`，不能因 `firstSlot` 变成有效值而把整块石头当空气。
- 新增 `resolveMaterialPoint`：从同一发布签名取得材料分类，`nodeAt` 取得节点，检查它属于普通材料或带材料的 transport。纯空气和 phase 槽分别返回明确类别。
- `QueryPublication`已发布全部live slot温度，复用双缓冲和generation。普通材料槽参与红外变化比较，纯Air不标显示脏。带材料transport在已有`ThermalCellArena.cellKinds`中用一个新的`MATERIAL_TRANSPORT`枚举值表达，沿用同一个byte数组：`isAirCell`包含它，中心/容量/支持引用按mixed transport解释，`isMaterialPole`不包含它；新增`isSurfaceCell`是普通material或该枚举。编译时由profile决定kind，发布时只做字节判断，不每个slot回查Page/签名。材料transport的构建标志用一个临时long mask传递，不增加arena布尔数组。相变状态变化走已有phase候选发布事件。
- `coverageSlot` 的所有 `<0` 和 `+node` 使用点逐一替换：query、infrared、dormant、phase、worker publication 构造器及 GameTest。不能仅替换 `transportAt` 为 `nodeAt`。

`MATERIAL_TRANSPORT`的修改点包括stage/writeMixedComponent、supportRef中心定位、所有直接比较`MIXED_COMPONENT`的分支、isAirCell、source端口资格、迁移和slot回收。保持原kind数组容量与字节宽度；不把它误归入使用独立块坐标的material pole。实际布局与材质没有改变时不重新设置kind或标dirty。

#### 3.2 查询按物理量分开，复用具体方法

在 `MinecraftThermalInput` 增加具体方法 `sampleMaterial(BlockPos, MutableMaterialSample)` 和 `sampleSurface(...)`。`MutableMaterialSample` 只含摄氏温度、采样 tick、来源枚举和可用性；调用方复用 scratch，不为每次读取创建 record/list。`sampleSurface` 是普通材料、已放置 reservoir、有效热源的固定分支，不引入泛型 provider/service 框架。

来源枚举建议为 `LIVE_MATERIAL / RESERVOIR / EMITTER_MODEL / DORMANT_MATERIAL / NATURAL_ESTIMATE / UNAVAILABLE`。该来源用于测温提示和验证；普通红外每像素不必额外同步来源字节。

普通材料查询优先级：当前coherent材料节点 -> 同位置同材料的最近coherent发布 -> 同位置同材料的dormant记录 -> 统一Page自然边界估计。统一基准为`MinecraftEnvironmentCapture.naturalTemperature`目前采用的Page中心`WorldTemperature.naturalAir`，优先用现有发布/capture中的值，无runtime时在同一中心按需计算。不能给尚未驻留材料用naturalBlock、驻留后却初始化为naturalAir，造成没有传热也跳温。旧`WorldTemperature.block/naturalBlock`仍服务其既有玩法，不是本轮物理材料基准。上一次发布只允许精确位置匹配，不得采用现有`resolveLastPublication`的最暖transport代表值。区块未加载返回unavailable，不加载区块，不启动runtime。估计值从不回写arena。

暂时读不到完整新 cut 时保留最近已确认显示；材料替换/方块删除已确认时立即清掉对应旧读数，不能把旧热石头的温度继续贴到新空气或木头上。温度计遇到估计显示“估计”即可，不把节点编号和发布版本放进普通使用界面。

批量查询不反复调用会重新begin cursor的点接口。将现有`InfraredReadCursor`的只读温度cut能力抽出/重命名为具体`ReadCursor`，红外epoch访问继续复用；`readMaterialAt(publication, cursor, localBlock, out)`不重新定位Page或开启cut。温度计只读一点；红外/checkpoint先定位一次Page，唯一节点各读一次，再展开。最多两次完整cut尝试，最终检查`cursor.isCurrent()`及Page publication身份；失败不提交混合新旧代次数据。不能持有borrowed数组跨tick、回调或网络发送，builder只保留已编码字节。

保留 `WorldTemperature.block` 作为现有玩法环境接口，更新其注释说明它不是材料测温；不全局改写其所有调用者。`gameplayPlayerEnvironment`、镇区环境和空气采样仍保持自己的量纲与合同。

### 4. 材料节点、热容、连接和相变边界

#### 4.1 节点只随稀疏物理驻留产生

在已驻留 Brick 中，普通 V0 材料即使被完全包住也建节点。Brick 未驻留时没有 arena 分配；“支持内部材料温度”不意味着给整个已加载 section 或整个世界建节点。完整空气 Brick 仍是一个节点；普通材料块不合并，以免抹平不同块的温差。

热容改为 profile 的 `blockCapacityJPerK`，普通整块按一个有效块计。暴露面只影响换热连接和辐射可见性，不改变热容。V75 使用同一材料热容；无材料通风节点继续使用空气参考热容，不另加空气容量。形状变化不自动加减热量。

首轮调试参数：沿用当前七类 profile 的数值作为每有效块热容候选，即 fabric 120、wood 450、earth 1100、masonry 900、glass 250、metal 700、generic 650 J/K；这是一套游戏参数，不能标成真实一立方米材料常数。与旧的“每暴露面相同数值”相比会改变多面暴露块的响应速度，必须在热石/墙体夹具中记录，不暗称平衡不变。最终平衡只调整这七个表项，不把开门时容量跳变保留下来。新符号取代 `surfaceCapacityJPerK`，删除不再需要的 exposure 容量依赖扫描。

#### 4.2 每个相邻面最多一条导热连接

继续内部 144 个正向面、跨 Brick 每面 16 对，负坐标侧唯一持有。没有新邻接对象：复用 `MaterialContributions -> MaterialEdgeCompiler -> ThermalMaterialExecution` 的 primitive 数组、无向边聚合和指数交换内核。

- transport/transport：保留现有通风公式 `G=K*A/(dL/pL+dR/pR)`。带材料的 V75 仍按这个有效混合路径交换，不能再叠加第二条材料边。
- transport/普通实心材料：`G=h_material*A`，沿用当前 profile 面换热参数。
- 普通实心材料/普通实心材料：`G=A/(0.5/k_a+0.5/k_b)`，新增每类 `bulkConductanceWPerBlockK`；块长按 1 m 的游戏换算，A 是接触整块面面积，G 单位 W/K。首轮 k 候选采用当前 0.12/0.45/1.0/1.4/0.8/6.0/1.0 数值，但独立命名，不能把空气侧 h 和固体 k 当成同一物理量。
- 两侧有限节点按 `Q=C_a*C_b/(C_a+C_b)*(T_a-T_b)*(1-exp(-G*(1/C_a+1/C_b)*dt))` 转移能量；两边一减一加。温度为 °C，温差为 K，C 为 J/K，dt 为 s，Q 为 J。复用已有实现及预编译 coefficient，不另写一套公式执行器。
- 未驻留/未加载邻块不建立自然温度固定边界。对已加载且需要传播的邻 Brick 请求既有驻留；等待期间保留当前边界和能量，不虚构散热。

#### 4.3 沿材料传播，禁止一次扩展整片地下

`WorkerPageStore` 的热前沿检测由 transport-only 改为所有可导热有限节点。既有 hot mask/残差迟滞负责保留热材料；相邻候选每次 cut 只扩展一层 Brick，候选仍由原驻留队列去重、分批服务。读温、红外和玩家辐射不能触发扩展。

具体修改`faceResidualC`：只检查该面16个成员，使用`nodeAt`并去重，排除phase；`firstSlot<0`才代表没有节点。正Y方向的`topPortDirectSky`排除仅适用于无材料Air，不能让暴露于天空的热石头失去向上/邻区传播资格。原hot mask承担“本Brick有温差”的保留，面残差承担“哪一邻Brick需要加入”，两者不能互相替代。冷驻留目录不因内部非边界热节点直接扩展六面。

连接改变只收集本Brick和面相邻者，既有唯一面owner决定需重编的fragment，不递归重编整个连通材料体。新增材料节点导致的sky/exposure可见性更新只更新相应面/辐射分类，不再触发热容重建。每次cut的候选集合在编译前固定，新admission只能在下一次cut继续传播。

热源能量首先走已有 source 端口；材料通过连接接收能量。没有为每块材料注册 source。材料-only Brick 的向外热差也必须唤醒邻 Brick。达到已有 cells/pairs/work 预算时保留旧拓扑并在原队列继续服务待办，不清掉热材料、不把未传播能量算成自然损失；需要测量待办年龄与传播延迟，不能只看未超过内存上限。

#### 4.4 相变不冒充正常材料状态

现有 phase reservoir 是固定转换温度与潜热进度，不含完整低于转换点的显热模型。本轮不把“每块相变材料的完整显热/潜热重构”夹带进普通材料改造。

- 保留原候选、潜热、ACK 和方块替换流程。普通材料与 phase 相邻时，接入原 `PhaseContacts` 的有限节点到潜热槽交换；把 `air` 参数名改为 `finiteCell`，不新造 phase solver。
- phase/phase 不新增直接边；phase 在平台吸收/释放能量仍按现有上限和转换条件执行。
- `sampleMaterial/sampleSurface`对phase返回单独的状态：确有平台能量/转换过程时使用转换温度；尚无显热模型时返回统一Page背景估计。不能因槽位`temperatureC()`恒为转换点就把所有冻冰测成0°C。在已有phase候选发布对象中增加一个`long activePhaseBlocks`；同一worker cut根据潜热/待ACK状态生成，仅当整个mask与上次不同时标显示变化，并替换该候选快照，不能原地修改读者持有的发布对象。普通Brick复用EMPTY候选，不分配额外对象；主线程不读worker arena。
- 验收分别列出普通材料完整闭环与 phase 的此项近似；不得声称本轮完成了所有物质的完整热力学。此前“保留相变”的范围不因此扩成一个新物理系统。

### 5. 在线变化、卸载、重载与材料余热

#### 5.1 在线迁移

复用当前最多64块的成员迁移。相同材料和空间成员保留焓；门开关改变transport分类但保留同块材料能量；纯空气合并/拆分按已有成员份额分配。真正替换材料以新材料背景初始化，并在同一mutation清除旧dormant材料位。这里“同材料类别”不足以识别保留资格：stone替换为同属masonry的另一方块也应重置；同一个Block的门开关/方向属性变化才默认保留。主线程已有变更聚合为每个变更Brick OR一个临时`resetMaterialBlocks`，物品拆除再放回即使最终signature相同也不能丢失reset事实；该mask随已有input batch传worker，不建逐块generation目录。查询在reset尚未发布时不返回被重置位置的旧值。显式拆除的材料能量属于拆除物离开世界热模拟的边界，不灌给相邻空气。

取消仅由 exposure 变化造成的容量和 span 重建；邻面导热/天空/遮挡仍按原依赖闭包重编。保留 source 结算 -> staged spans -> material edge 编译 -> commit -> source 重绑 -> 旧 span 释放的顺序。

#### 5.2 在原 section 存档增加稀疏材料字段

空气继续沿用当前 format 2 的 `bricks/counts/residuals/blocks` 与 640 字节数值预算。本轮给同一个 section 增加可选材料字段，不复制整份 checkpoint，不引入旧版本转换服务；现有 format 2 无新字段自然解释为“无材料记录”。空气和材料共同决定 entry 是否为空、sourceSupported、保存与清理流程，不能在空气为空时丢掉 material-only entry。

材料记录按 Brick 地址和块内 bit 排序：

- 一个 section `long materialBricks`；每个有记录 Brick 一个 `long materialBlocks`。
- 保存普通 V0 和带材料 transport 的材料余热；只有量化后残差非零且超过 prune 阈值的块占记录。每个块存一个 `short` 温度残差和一个稳定的 `byte materialKind`。七类材料使用固定枚举代码，禁止保存运行期 dense profile ID。未来确实新增类型再扩字段，不预建 palette 框架。
- 新材料字段采用 `MATERIAL_RESIDUAL_SCALE=4`，即0.25°C/单位、误差最多0.125°C、可表示残差 -8192..8191.75°C；原空气 `RESIDUAL_SCALE=16` 不变。材料基准为保存时该 Page 的自然边界，正常材料/背景均在 -273.15..2000°C 时其温差可表示。极端配置超量程采用饱和并累计现有诊断计数，不能数值绕回冷温，也不能声称这种配置精确保存。
- 使用平铺 primitive 数组，只有 section 有材料余热时分配；64 个 Brick offsets 按现有 offset 模式复用/扩展。没有逐块对象、逐材料计时器或额外后台衰减任务。
- 带材料 transport 改为只进材料字段，原空气 capture/均值只统计无材料 transport。恢复时先按当前节点类别选择唯一流；只有无材料 transport 可以使用空气记录。这样旧混合材料的热量不会在新存档中被当空气均值保存，门开关也能在同材料类别下承接原温度。旧format2中的混合均值只能保持旧空气近似，不能从其推导不存在的材料历史。

全 section 4096 块实心材料都有残差时，材料原始数值上界为 `8 + 64*8 + 4096*(2+1) = 12,808` 字节；再加空气最多 640 字节，为 13,448 字节，不含 NBT 标签、数组对象、offsets、对齐和压缩。不得继续宣称材料全部能塞入旧 640 字节。不保留第二份均值和每块精确温度；均值只在确实需要的已有调用中临时求取。

材料残差按现有全局半衰期 `r(t)=r_saved*2^(-elapsed/(20*halfLifeSeconds))` 进行按需衰减，elapsed 单位 tick。材料不因 sourceSupported 或解析场存在而冻结余热；原 supported 标志仅继续作用于空气流。保存基准与恢复基准都取同一Page自然边界合同，点测温不得另用该块的naturalBlock替换残差基准。恢复 `T=N_current+r(t)`、`H=C_current*(T-T_reference)`，离线是温度近似，不宣称离线能量守恒。只有同位置同材料类别可用；已加载 mutation 清相应 bit，离线替换以加载后实际分类筛除。

材料恢复在普通迁移后、第一次 source 能量执行前进行；未匹配处按新 profile 背景初始化。无 source 的 dormant 材料可被测温/红外读出，但读取本身不唤醒求解器。世界卸载、保存、服务器停止和完整 tags/recipe reload 均先沿用现有 coherent checkpoint 入口收集材料，再释放旧 arena。新 tags 下稳定类别变更按替换处理；关闭物理 runtime 不能直接丢弃尚未 checkpoint 的材料热状态。

#### 5.3 恢复与存档的具体执行规则

1. `BrickMigrationKernel.migrate`先迁移有效旧节点。仅在没有可迁移旧节点、没有reset标记且存在本次admission材料输入时恢复dormant；已经迁移的块不能再加一份旧checkpoint能量。删除当前“给非transport有限节点套空气mean”的循环分支。
2. 将`ThermalInputBatch.DormantAirCut`扩为同一份`DormantThermalCut`，包含两条互斥输入流和一次冻结的衰减factor。保留原入队/所有权机制，不复制全section工作数组。worker Page增加一个`long pendingDormantBricks`，初值为空气/材料输入Brick并集；成功commit才清已承接的bit，staging失败不清。Brick再次睡眠/重编不能重放Page初次admission携带的旧记录。该字段8 B/Page，3200 Pages数值载荷25,600 B。
3. 点查材料索引为`offset[brick]+bitCount(storedMask & lowerBits(block))`；64个`short`偏移足以容纳4096项，128 B/有材料section。删除记录用`materialKind[index]=0`作墓碑，rank仍按原storedMask；在正常保存/重建时一次压实，不为一次敲石头搬移整个section数组。若存在并发保存读者则沿用section现有不可变替换时机，不共享可变数组给异步IO。维护一个`liveMaterialCount`用于O(1)空判断，不能把墓碑再次编码成有效材料。
4. `sample`只计算factor，不重写残差/时间；不会因红外请求频率不同而加速冷却。`rebaseForSave`至多在一次真实保存边界量化，反复查询或同tick重复保存不重复衰减。无新定时任务。保存后读数允许0.125°C量化误差，跨多次真实重存的误差单独测量。
5. `capture`按同一ReadCursor收集所有有限节点，验证同cut后一次替换section entry；失败保留旧entry，不能把半个材料流与新空气流拼接。退休/卸载先checkpoint再释放slot；当前`MinecraftThermalInput.close()`已经先调用`pages.checkpointAll(true,true)`，复用并扩展该入口，不新增第二次全世界扫描。
6. 全部`SectionEntry`方法成组扩展：decode/encode、contentEquals、isEmpty、rebase、删除、sourceSupported、admissionCut、红外fallback。`bricks==0 && materialBricks!=0`必须有效。仅新增可选材料字段，无旧档迁移器；读入时没有材料历史就明确估计，不借空气历史补造。

### 6. 接通玩法消费者，避免更换一个接口后全系统含义漂移

| 消费者/入口 | 实施动作 |
|---|---|
| `SoilThermometer.use/finishUsingItem` | 两种模式同用 `sampleMaterial`，命中已放置暖石/水袋用其既有表面状态；默认显示 0.1°C，估计值带简短提示 |
| `SoilThermometerRequestPacket` 与 `TemperatureGoogleRenderer` | 指向同一位置查询；返回温度和来源，不再另走 `WorldTemperature.block` |
| `WorldTemperature.checkPlantStatus` / `gameplayCropEnvironment` | 普通地上作物定位根部支持块；GROW/BONEMEAL使用根部材料温度，SURVIVE保留地上空气环境。两个值各自在作物位置应用原解析场规则，不能共用一个float覆盖全部阈值。上下两格作物定位到根块；水生、附着、无土作物保留自身环境查询，以已有方块/tag分类分支处理，不强行向下测土 |
| `gameplayPlayerEnvironment` / `PlayerThermalModel` | 对流继续使用空气；新增平均辐射温度输入，见第 7 节；不以材料温度替换空气 |
| `gameplayTownEnvironment` | 保留当前代表点环境合同；材料通过空气影响室内环境。本轮不把全镇温度改成墙体温度 |
| `ThermalReservoirBlockEntity` / dropped reservoir | 继续使用现有 core/surface 状态；显示直接读 surface；环境交换消费同一长波采样，不能增加另一套 reservoir 温度 |

通用材料测温与表面红外不直接应用 `FLOOR_FROM_NATURAL/ADD_DELTA/OVERRIDE`。能量塔的物理 source 仍通过求解器影响材料；区域玩法保底继续用于人物/作物/城镇。场移动、缩小、消失不瞬间改写材料温度和 dormant。旧红外 field-only 显示目标被本方案替代，相关全场合成测试改为检查“玩法场可变化，物理表面不被直接覆盖”。

这里改变的是读数语义而非删除能量塔能力。无需新增塔覆盖渲染或修改整合包配方。实施如确需调整作物 tags/pack 数据，先定位伴生仓库并读取其 AGENTS.md，对两个仓库分别验证和汇报。

### 7. 材料参与玩家长波环境：按接收者采样，不建立海量发射源

采用固定方向的可见表面采样，复用 `MinecraftRadiationOcclusion` 的 block DDA 遍历，增加“返回首个热表面位置”的具体入口。不给每块热石头注册 `PhysicalSourceSpatialIndex` source，不维护热材料 BVH、每玩家热源清单或可见性历史。

采用14个固定方向（六轴加八个归一化立方体对角线），轴向权重各`1/15`、角向各`3/40`，合计1。这比14方向等权在相同射线数下具有更好的低阶角向积分性质，不新增状态。半径16 blocks，每射线最多32个voxel steps。它不能保证命中任意小热块；尖锐的已注册source/fire/lava仍走原直接辐射。方向和两个权重是静态常量，不为每次采样创建向量。

在玩家现有错峰20-tick环境采样中，从躯干位置取得一次共享结果，五部位复用。DDA命中有热表面的方块即终止；玻璃阻挡、普通Air透过。有限节点先在同一ReadCursor下读取；dormant/估计走第3节同一合同。miss/区块缺失/暂不可读方向用空气背景。采样前检查起始voxel；现有source-to-receiver DDA跳过起始块的行为不能照搬到receiver-to-surface。每次enterSection仅`getChunkNow`取已加载section，并复用到下一次跨界；普通空气palette快退，完整实心直接命中，部分形状调用服务端`VoxelShape`的有限线段clip，复用MutableBlockPos。禁止在专用服务器引用客户端baked model。暖石/水袋使用已有方块形状与surface，跳过接收者自己的模型；动态特殊形状走该Block已有shape方法，不建立世界形状缓存。

计算量：令 `T_aK=T_air+273.15`，`T_iK=T_surface_i+273.15`，材料有效发射率 `epsilon_i` 首轮统一 0.9（游戏近似，后续平衡只改七类表项），则 `T_rK^4=T_aK^4+sum(w_i*epsilon_i*(T_iK^4-T_aK^4))`；`T_rC=T_rK-273.15`。先平方再平方，不每面调用通用 pow；最多一次四次方根。冷材料产生负修正，不把所有负值裁成零。

`PlayerThermalModel.preparePart` 将现有 operative temperature 改为 `(h_conv*T_air+h_lw*T_r+q_direct_absorbed)/(h_conv+h_lw)`，保持已有衣物/组织串联热阻、部位面积和稳定指数积分。`h_lw` 继续用当前 4.7 W/(m² K)，属于游戏线性长波近似；红外只显示表面温度，不倒算这个体感值。源码中基于空气的 `airLossFluxWPerM2`、平衡/提示计算也使用同一 `T_r`，避免主模型和显示两套公式。

被现有直接辐射覆盖的 fire/lava/机器发射面在方向积分中贡献空气背景，不再次叠加相同热源。暖材料的长波和 source 的直接通量分别保持来源，不能把材料温差转换成 source 功率再加一次。积分照顾负温差，但不新增空气或材料之间的全对全辐射。

材料长波仍沿用现有人物/掉落物“单向环境采样”的边界，不从材料节点逐玩家扣能量；因此它不是闭合世界-人体的能量守恒模型。若未来要求多人同时耗尽热石头，必须做有反馈的能量交换，不能把本轮单向采样宣传成已实现。此次世界内空气/材料导热保持守恒。

物品现有`ItemEnvironmentSampleCache`只有64个quarter-position条目，每tick清空，既无跨tick旧样本也无待办队列。本轮维持这一结构：同位置本tick的空气/长波/直接辐射一起复用；缓存可容纳的新位置做六轴等权`1/6`长波，最多`6*32`steps/位置；满额时仅使用已算空气作为辐射背景，记录近似次数，不杜撰“稍后必然补采样”。不新增物品调度器或延迟对象。最坏新增`64*6*32=12,288`steps/tick，另计原直接辐射；必须测量大量不同位置物品，不能只报告100玩家的射线成本。原cache容量保持64，不为测试成功调大。

长波结果在服务器上保留为一个明确的`meanRadiantTemperatureC`及可用性，`clear/copyFrom`和所有sample缓存一并更新；不要复用`radiantFluxWPerM2`字段存摄氏度。方向积分使用物理空气背景，在解析场合成前完成；最终对流温度仍由原玩法场合成。寒冷墙面会改变玩家体感，计划不再承诺新长波加入后旧体感平衡完全不变。

### 8. 表面红外：温度数据与几何归属同时修正

#### 8.1 温度纹理与稀疏同步

沿用 `InfraredViewRenderer` 的单份 direct `ShortBuffer`、`144³ GL_R16I`、729 Page 地址、Brick codec、40-tick 错峰 poll、Page subimage、full/尾包提交及 retry。0.25°C 编码继续沿用；不增加第二张全尺寸三维温度纹理。

服务端普通 Brick 读取各唯一可显示材料节点一次，按 `nodeAt` 展开。纯空气温度不再作为表面纹理记录，material-only Brick 可发送；带材料 transport 可发送其有效材料温度。读取与 dirty tracking 都覆盖有限材料节点。删除、材料替换、Page 退休必须发对应 INVALID 或 replacement；retirement 时先提供可用的 dormant 材料值，不整 Page 一律清冷。

休眠材料通过同一 `INVALID/UNIFORM/INDEXED/RAW` 记录发送精确块值；原 `DORMANT_SECTION` 的空气 Brick mean 不能再覆盖实心表面。现有 dormant ownership 和刷新标记按表面合同简化，不能为新材料另复制一套 144³ mirror。沿用客户端携带 presence/refresh 状态重建需要刷新范围，服务端不保存逐玩家温度历史。

热源/暖石等已有独立状态：固定 fire/lava 温度是小型服务器同步 profile 表；机器通过已有 source 索引按所在 Page 输出有效表面温度；暖石/水袋使用现有 BE 更新数据。动态独立表面的旧/新范围复用 `knownRefreshPages` 处理退出；没有 source Page 也能显示有效发射面。固定温度 profile 修改只更新小表，不让每个熔岩方块重建物理 Page。禁用/移除热源退回该位置普通材料或背景，不能留下旧的热源颜色。

`knownRefreshPages`不能单独作为每40tick重发整Page的理由。source token复用`MinecraftThermalInput.dimensionGeneration`与`PhysicalSourceSpatialIndex.nextRadiationRevision`，请求/响应携带这两个long；后者须在删除/禁用时同样推进，不能只在新增/改功率时推进。token、物理epoch、dormant量化结果及背景版本均未变时不重建源覆盖Page。token变化时才重建旧标记与当前源所在Page并集，物理或dormant已脏Page始终重新合成当前source值。无需服务端每玩家缓存或每source另一份显示版本。runtime不存在用显式空token，不能与重启后同数值revision混淆。

Page内源通过现有`sourcesByOriginSection`索引枚举，不能每Brick扫描全维度source列表。最终64个块值的优先顺序为独立surface provider -> 当前材料/phase -> 匹配dormant -> INVALID交由背景。每个输出Brick只写一次最终记录，禁用/删除时重算原覆盖位置；不得发物理温度后再靠包顺序叠加场。用一个请求复用的Page级源位置scratch即可，最多4096个short与64个long块掩码（8,704 B），只在Page确有source时按被使用的Brick清理，不预填整个显示窗口。BE和实体已有更新通道的surface无需再走这一source scratch。

material-only场景仍使用物理Page presence；source-only、dormant-only与小背景各有现有/新增的明确状态，不能把`presence=0`解释为整屏没有温度。任何Page的live cut暂不可读时不以背景覆写该Page旧材料值，继续当前coherent基线并在后续自动poll重建；同位置已确认的block reset例外，立即清除。收到主世界block replacement时客户端清该texel并标原Page上传dirty；服务器旧publication的geometryRevision必须覆盖对应reset事件后才能再次发送该位置值，防止旧包复活。无需逐块版本数组。

#### 8.2 无模拟数据的自然背景：小表而非重算 144³

背景改为`9×9×9 GL_R16I`，每个显示Page只有一个服务器已计算的自然边界温度，共729个值、1,458字节；CPU一份short镜像，GPU一份。优先从现有Page自然边界取值，无物理Page时按同一Page中心调用`WorldTemperature.naturalAir`。复用每次capture的Page句柄/loaded chunk，不对729个位置反复定位相同chunk。不传公式、不在客户端重新算气候、不在full时遍历近300万个块。

背景三轴均为16-block分辨率估计，与当前物理Page初始背景的精度相同。它不能表达Page内部的自然高度/群系差异，但温度计、材料初始化与红外在使用估计时采用同一个基准，不会因是否驻留而跳到另一套公式。实际材料温度仍为整块精度，不在shader跨墙插值。无热状态的真实温度未知，不能用更密的显示表伪装成真实材料模拟。

full 或窗口中心改变时带完整背景表；稳定窗口跟随已有自然边界的 200-tick 级刷新，客户端请求携带上次背景时间片和背景表版本。服务器小型显示profile/背景版本跟随现有tags/config重载事件递增，没有周期性hash。同片无变化不重发。另用两个long携带窗口81个chunk的已知加载位（16 B），与当前已加载状态对照触发背景更新；原物理Page presence不能表示这一信息。无加载数据的列保留 unavailable，不用零温度冒充。背景可以分包，但沿用同一 snapshot 的尾包安装，不增加后台观察服务。

小型背景表替代旧的“完全无独立自然温度同步”约束。相比上一版计划的`9×144×9`，取样和数值存储都减少16倍：每次full最多729个Page背景取样，已有值时无需再次算公式。旧r128全窗口场合成约300–440ms仅为旧实现记录，新路径仍须实测。背景表按已有响应附带固定数量short，无需再套一层Brick codec或自建调色板。

#### 8.3 几何归属：一个专用热表面 pass

不再使用 `SURFACE_SAMPLE_SCALE=2047/2048` 选择空气，也不把它改成反方向就宣称完成。原深度不能区分地形、生物、粒子，普通透明玻璃在长波下的遮挡也不同；这是新 pass 的具体必要性。

只在红外实际绘制期间增加一个thermal-surface pass，复用摄像机、视锥、已有可见chunk mesh/GPU buffer，使用自己的深度与单通道`GL_R16I`温度附件。直接在该pass输出量化温度，删除上一版R32UI对象标记附件及最终合成的标记分支。不重新逐块烘焙世界，不GPU读回，不给每块方块注册渲染器。

- terrain metadata只含`ownerLocalIndex`的12 bits与热绘制类别4 bits：普通表面/玻璃/火焰/熔岩等，不含当前温度或窗口内索引。一个quad的顶点携带相同owner；section/region draw origin与当前texture origin在draw时合成绝对归属。移动窗口、温度变化、profile温度更新都不重建mesh。模型偏移/跨块顶点不能floor猜owner。
- terrain vertex shader按owner查一次144³温度，INVALID才查9³背景；固定发射类别查小profile表。结果以`flat int`传到fragment，fragment只做必要alpha判定并输出温度与深度；四个顶点可能重复同一texel读取，列入V计数，不能称GPU每个像素只读一次。对fragment查温可作为同场景shader对照，只保留测量后更合适的一种；不增加运行期双路径开关。
- 临时温度附件用整数clear写`Short.MIN_VALUE`，禁用blend/filter/MSAA resolve。天空/无表面由depth=1判定；表面存在但温度仍INVALID时保留原画面并在准星提示暂无估计，不能把invalid转成0°C或最低温。切换/resize按所属资源生命周期释放。
- 原有mesh、UV和alpha cutout重用，热pass不跑光照。玻璃按热材质写depth，玻璃中间透明纹理不应用可见光alpha discard；栅栏/植物保留真实几何与cutout。烟雾不画热表面，火焰画发射面。透明水面的热波段按不透明表面处理，温度无物理节点时明确使用背景估计；不能透过水面直接看水底温度。
- 实体和BE复用既有模型/变换，通过draw uniform或现有per-part render context提供直接量化温度，不把温度复制到chunk顶点。保留热depth决定遮挡，衣物外层输出其温度。layer重绘不能再次执行实体tick、动画推进或产生粒子，只复用本帧计算结果。
- 最后原全屏红外pass只读取屏幕热温度、热depth和原颜色，量化值乘0.25后配色。距离由同一main Camera与热depth得到，扫描边界和0.43混合可沿用。最终合成不再读取世界3D温度、对象ID或发射profile，GPU温度没有第二份CPU屏幕镜像。
- 渲染输出由两个明确的 renderer adapter 接入：Vanilla 和当前装载的 Embeddium/Oculus。共享温度及标记协议，不写两套物理数据路径。项目目前只有 `IrisRenderingPipelineAccess` 等有限接入，不能把复用 mesh buffer 当作已存在 API；实施第一个客户端里程碑就核对实际 renderer 的格式、draw 调用和生命周期，并补齐两个 adapter。
- FBO、viewport、depth mask、blend、draw buffers 与纹理上传状态由当前 pass 成对恢复；窗口重建、关闭红外和世界退出释放所属资源。现有旧纹理 reset/generation 时序沿用。不能因 renderer 暂不支持就悄悄切回空气热图。

热pass统一处理实体、玻璃和复杂模型，避免多套屏幕修补mask/CPU射线。R16I加保守32-bit深度按`6*W*H`数值字节计，1080p约11.87MiB；相比上一版8WH省4,147,200B，约3.96MiB。实际驱动可能对齐，报告真实GPU分配。该pass确实多绘制一遍可见几何，必须测GPU时间；不能用“只一个pass”推导性能最优。

本次已只读检查当前Embeddium依赖：`CompactChunkVertex.STRIDE=20`，position/material/mesh占0..7，color占8..11，UV占12..15，light占16..19，没有可直接占用的空洞。最小通用实现增加一个packed owner/class字段并对齐到24字节，原mesh数值带宽增加20%，不能冒称关闭红外完全零内存成本。不要挪用light高位，其他渲染器可能读取它。Vanilla使用同一owner语义，实际stride单独计量。若后续现有renderer格式确有受支持的同类字段，可复用，但不能把待确认字段作为预算前提。

已确认的Embeddium接入锚点为`ChunkVertexEncoder.write`、`CompactChunkVertex`、`DefaultChunkRenderer.render`/`fillCommandBuffer`/`prepareTessellation`、`ChunkShaderInterface.setRegionOffset`。owner必须在原block/fluid mesh builder仍知道BlockPos时写入，并跟随原缓冲排序/拷贝/区域迁移；不可另按提交顺序拼一个未同步的sidecar。Oculus启用时可能替换vertex type，adapter必须选择实际`ChunkVertexType`和draw bindings，不能硬编码20/24字节去解释另一格式。

热pass只使用扫描球相交且当前view可见的section draw ranges，复用原索引和region多绘制批次；不按方块循环draw call。渲染温度不用与主颜色重复执行shaderpack后处理。复用绑定/绘制能力可以通过现有mixin/accessor取得，但不克隆整个Embeddium renderer源码或建立第二套chunk编译/驻留系统。模型改动只由正常chunk rebuild产生新owner数据，不因开启红外强制全部section重编。

#### 8.4 生物、热源和色标

玩家已有五部位体温是内部节点，不能直接当衣物表面温度。服务器复用现有部位热阻和本轮环境结果，按稳态热阻估计 `q=(T_part-T_op)/(R_tissue+R_cloth+1/h_out)`、`T_outer=T_op+q/h_out`。单位为 W/m²、m² K/W 和 °C；液体接触/湿润沿用现有分支，使用当前保护参数。该外表估计不建立新体温积分节点。

玩家的至多五个量化外表温度通过既有实体 tracking 范围发送，仅 0.25°C 值变化时更新，最多每 20 ticks；不能假设 `FHBodyDataSyncPacket` 已同步了其他玩家。没有完整体温模型的生物使用固定物种有效外表温度/随环境类别，小型静态表即可；未知种类使用环境估计，不一律恒温 37°C。若确需新实体同步包，复用 Forge tracking 分发，不增加自己的订阅系统。冷实体、衣物和小模型的精度范围写进最终文档。

tracked实体新开始跟踪时主动发送当前五个值，不能只等下一次changed事件；自身玩家也接收一次同格式更新。缓存上限随原tracked entity生命周期，离开tracking/换维度立即移除，entity ID复用不能继承旧温度。首次尚无表面数据先显示背景估计。温度快照只保留在实体已有温度数据/客户端对应实体中，不在红外renderer再维护一份ID到温度的全局map；是否需要发更新由同一份已量化值比较，不每帧重新计算服装热阻。

fire/lava 共享服务端已有温度参数或有效发射模型。功率型篝火可按 `T_K=(P_rad/(epsilon*sigma*A_eff)+T_refK^4)^(1/4)` 得有效发射温度；P 为 W，A 为 m²，sigma=5.670374419e-8 W/(m² K⁴)。给 profile 明确的有效面积/发射率候选，首轮 A=1、epsilon=0.9，不把功率 W 当 °C。熔岩沿用当前配置温度与有效发射率，不借此次重画调整其现有辐射功率。

默认固定环境色标候选 -50..50°C，额外高温档候选 0..1200°C；超量程饱和并保留图例/准星数值。两档都只是显示参数，不影响求解温度。第一版无自动量程和复杂光谱材质；已求解表面温度是主显示量，发射率用于辐射模型，不能宣传为带真实反射的辐射测温仪。

### 9. 成本、预算与实际性能验收

令B为驻留Brick数，S为有限节点数，E为唯一交换边数，D为本次需编码Brick数，F为含邻接闭包的重编owner fragment数，P为读取显示Page数，V_draw为本帧绘制顶点数，V_mesh为全部已保留chunk mesh顶点数。B受既有Page/Brick驻留和arena/work上限共同约束。显存按V_mesh计算，shader工作按V_draw计算，不能把两者混用。

| 路径 | 预期工作与状态上界 |
|---|---|
| 普通材料节点 | 每驻留 Brick 至多 64 个有限节点；不随观察人数复制，完整空气仍 1 个 |
| 拓扑编译 | 每owner fragment至多144内面+3*16正向跨面，最多192F面检查；依赖捕获另为6*16面邻位置。邻接闭包F不等于最初变更Brick数 |
| 求解 | O(S+E)，材料边复用原执行数组；不用每步重新计算指数 coefficient |
| 点测温 | Page/Brick 一次定位、一个节点读取；fallback 最多一次已有 dormant 查找 |
| 发布 | 原live-slot遍历同时记材料变化与热残差，O(S)；MATERIAL_TRANSPORT复用原kind字节，纯Air无显示脏；phase候选另8 B active mask |
| 红外增量 | O(P+64D+视野内源数)展开/编码，唯一节点只读一次；source token无变化跳过覆盖重建；原palette编码上界仍是固定64值且第36个不同值提前转RAW |
| 红外背景 | full最多729个取样；稳定每200ticks一张1,458 B小表，100人原始数据约14,580 B/s，不含包头/重传/移动full |
| 玩家材料长波 | 每次采样最多 14*32=448 voxel steps；100 玩家各每秒一次约 44,800 steps/s，另计原 source rays，不等同于耗时保证 |
| 物品材料长波 | 原64位置/tick限额，每新位置6*32，最多12,288 steps/tick；其满额次数纳入性能结果 |
| checkpoint | 旧空气最多640 B+新材料最坏12,808 B/section；额外offsets128 B和live count4 B/有材料section；worker恢复mask8 B/Page，均另计对象对齐 |
| 客户端 | 原CPU mirror 5,971,968 B+GPU同量；背景CPU/GPU各1,458 B；热pass约6WH B；owner属性按4V_mesh B保守计，温度vertex fetch按V_draw计 |
| 请求scratch | source Page scratch至多8,704 B/共享捕获器，按需分配并复用；临时reset/分类mask随已有batch，不留全世界副本 |

`maximumPages=3200` 不能证明全材料场景可承载：3200*64*64=13,107,200 个块节点只是未受 arena 限制的地址上界。实现必须按实际 `maximumArenaSlots/maximumLiveCells/maximumPairOperations` 收费，不能按 Page 数隐去材料成本，也不能为通过 fixture 悄悄增加上限。

固定机器上测同一运行目录/同一配置，保留现有构建产物与正常增量编译。无需复制仓库重编、路径敏感 hash 或钉死原料版本。复用已有 profiler/JFR与日志计数；只有缺少关键数值时加有限计数器，不开发新的性能平台。

基准场景：无热源冷世界、单篝火木/石/金属室、多层实心墙、100 个分离热源与100观察者、100人冷启动红外、移动跨 section、全窗口 hot/dormant 增量、玻璃+实体+Oculus。每场景预热后采样至少60秒，报告服务器主线程 p50/p95/max、worker cut耗时和积压、节点/边数、分配速率、retained heap、S2C bytes、客户端CPU/GPU帧耗时。

验收门槛：原无材料Air快路径节点/边不增加；从未开启红外时不分配显示mirror/FBO或计算背景，已开启后关闭停止pass/poll并按既有资源生命周期释放或保留有界复用缓冲，准确报告保留值。owner顶点字段是持续mesh成本，不能写成关闭红外零成本。无变化材料/source不重复发；物理/保存内存落在预算；100人持续负载主线程p95<50ms，worker无持续增长积压。满负载物品另测。50ms是20TPS目标，不是已达到的数字；冷启动尖峰单列。full首次发送按已有entity ID错峰，不能同tick强制100份背景计算；背景稳定发送仍走40-tick poll的错峰。预算内工作队列应最终被服务，但持续容量耗尽时不得承诺无限传播，记录受限区域和等待年龄，保留已有能量。

GPU比较至少固定相同分辨率、render distance、shaderpack、视角与场景：关闭红外、热pass空绘制开销、完整R16I表面绘制。再对照一次vertex查温与fragment查温，比较实际GPU ms及V_draw/片元量后保留一个实现。不能仅依据FBO少4MiB就宣称帧率提高，也不为争取纸面“最佳”同时维护MRT改shaderpack、深度猜归属和全场重绘三种生产后端。

### 10. 实施顺序、文件责任与阶段验收

每步应可单独检查，但本计划只有下表各项及第11节验证全部闭合才标 completed。为避免先实现昂贵模拟却仍无法观测，测温接通放在导热扩展之前。

| 阶段 | 主要文件/符号 | 交付与退出条件 |
|---|---|---|
| A：统一可读节点 | `BlockBrickLayout`、`WorkerBrickTopology`、`PagePublication`、`ThermalCellArena`、`QueryPublication`、`MinecraftThermalInput`、两个土壤温度计入口 | 现有已暴露材料可测；派生有限节点数；MATERIAL_TRANSPORT分类；material-only有效地址；同基准估计 |
| B：材料导热与驻留 | `MaterialBoundaryRegistry`、`MinecraftThermalProfiles`、`BrickTopologyCompiler`、`MaterialEdgeCompiler`、`TopologyPlan`、`WorkerPageStore` | 固定热容、内部材料节点、固体面导热；跨 Brick/Page 热传播、预算待办、同材料迁移正确 |
| C：余热生命周期 | `DormantChunkThermalState`、`ThermalInputBatch`、`BrickMigrationKernel`、`captureDormantPage`、`LevelChunkMixin_DormantThermal`、启动/重载入口 | 删除空气mean恢复材料；唯一输入流且只恢复一次；材料-only保存、重启、同类替换、tags重载 |
| D：玩法接入 | `gameplayCropEnvironment`、`ThermalEnvironmentSample`、`MinecraftRadiationOcclusion`、`PlayerThermalModel`、reservoir环境入口 | 土壤输入、冷/热材料长波、热源不重复、衣物外表估计在真实环境生效 |
| E：表面数据协议 | 两个红外packet、`InfraredBrickCodec`、`InfraredCapture`、`InfraredViewRenderer` | 材料delta、material-only、dormant替换、背景小表、发射profile、退出清理、分包coherence通过；不再把石头INVALID当正确 |
| F：几何与实体 | `InfraredViewRenderer`、`infrared_view.fsh`、`FHClientEvents`、Vanilla/Embeddium/Oculus adapter、实体tracking同步 | 一个热表面pass覆盖墙、台阶、玻璃、人物、暖石与火源；关闭时释放资源；专用服务器能正常加载 |
| G：联调与性能 | `ThermalLoadedWorldGameTests`、`ThermalInfraredGameTests`、新增同目录材料场景、既有run配置 | 完整因果夹具、实际客户端和100人目标场景完成；文档与日记落地 |

F 的 renderer 格式/mesh复用探查在 A 开始时做短原型验证，避免完成全部协议后才发现需要不同 owner 表达；最终实现仍只保留一个选定方案，原型不形成第二套渲染路径。不因这个原型更换项目 renderer、依赖版本或引入引擎框架。

### 11. 必须实际验证的因果链与边界

沿用 Java17、生产编译和真实 Forge GameTest 工作流，不新增或运行 JUnit。执行 `./gradlew.bat compileJava compileGameTestJava --offline --no-daemon --console=plain` 与 `./gradlew.bat runGameTestServer --offline --no-daemon --console=plain`；如继续使用现有范围 init 脚本，记录确切脚本、所含测试和排除原因。新材料闭环场景不得被排除。当前旧测试里未接通的掉落物接口问题不能被当作本方案已解决。

1. **可观测吸热/余热：** 真实篝火加热空气和石块；工具读数等于同位置材料发布值；灭火后空气先降、石块保留余热并向空气放热。最终实际客户端红外颜色与材料温度对应，不读取前方空气。测试不能只直接 set arena 温度替代全部真实加热链路。
2. **固体内部传播：** 绝热侧边的三块材料链，热量从首块传到末块；跨 Brick 和跨 Page 分别测试。没有空气通路时仍传播。检查封闭有限节点总焓和时间推进，排除通过周围空气绕路导致的假通过。
3. **容量与迁移：** 开洞/关门/邻块变化不改变现存材料热容；节点转换保持能量。同类不同Block替换、同cut拆掉再放回也不继承余热。MATERIAL_TRANSPORT仍能接受source并参与Air交换。完整Air fast path不变；纯Air温变不产生材料红外delta。
4. **稀疏边界：** 热材料能驱动下一层驻留，纯观察不能；未加载邻块无固定冷源；预算缩小时既有状态保留、待办最终被服务，记录延迟。测试冷地下不因一个热源在同一cut递归铺满。
5. **保存全过程：** material-only entry有效；NBT往返、真实chunk卸载/加载、进程重启、tags reload保持匹配材料温度到量化/衰减误差内。冷热材料不被空气mean抹平；旧无新字段format2仍读Air但不给材料编造历史。测试Brick在同Page中反复退驻留/重入、staging重试、迁移后恢复，确认旧checkpoint只承接一次；查询次数不改变衰减。删除一个墓碑不移动其他块温度。
6. **测温一致性：** 创造即测、生存长按、HUD请求同一块返回一致来源/值；无状态有明确估计；没加载返回不可用。查询材料-only不能被当成无Page；不得读取任意最暖transport fallback。
7. **作物：** 根部土壤改变影响普通作物生长/施肥；温暖土壤不能自动免除寒冷空气的生存阈值；双高作物定位根块正确；水生/附着例外不误取脚下墙；能量塔场仍保障作物玩法，却不改写土壤温度计和材料H。
8. **材料长波：** 相同空气下热墙减少失热、冷墙增加失热；遮挡改变；固定14方向权重和为1，六轴/对角场景都测。起始voxel、部分形状、玻璃和接收者自身排除正确；小source继续原直接辐射且不重复。64位置物品缓存命中/超额明确计数；不再测试并不存在的物品待办队列。
9. **红外协议：** 仅材料变化发delta；纯Air和稳定source不重发；source删除/禁用/重启token都覆盖。full移动、重试、尾包提交、material-only、dormant接管、729值背景、loaded mask、初次tracking全量、entity ID复用均检查。客户端镜像对应服务端值；block reset后旧publication不能复活旧温度。改变窗口只更新origin和温度，不重编owner mesh。
10. **真正的图像检查：** Vanilla及Embeddium/Oculus各测试石墙、台阶、栅栏、玻璃后生物、人物衣物、火焰、烟雾、放置/掉落暖石；第一/第三人称、潜行、远坐标、窗口resize、多次切换。墙后实体不得穿透；玻璃显示自身温度；热表面归属不随镜头抖动；无状态背景不是全冷蓝。仅GameTestServer通过不能替代此项。
11. **相变边界：** 普通材料接触潜热槽仍触发正确ACK；冻结但未到平台的材料读数标估计，不报恒定转换温度；既有相变潜热不因通用材料查询和checkpoint被覆盖。
12. **性能：** 第9节全部场景实测并保留可追溯日志；报告新增有限节点/边与内存，不以“复用了类”或少量fixture通过声称全场景最优。

### 12. 文档更新与本次计划结果

实施时同步更新：`docs/climate/world-climate-and-temperature.md`（三类温度及红外语义）、`thermal-runtime-architecture-and-optimization.md`（节点/span/固定容量/导热/复杂度）、`data-lifecycle-and-integration.md`（材料checkpoint/网络/重载）、`heat-production-and-network.md`（材料参数/辐射边界）、`player-temperature.md`（工具、土壤输入、长波、衣物表面）。Boss文档目前仍有旧的“红外不含解析场”表述，按实施后的表面合同复核，不能单凭旧文档证明当前行为。

本次只修订计划和新增调查日记；源码与实际行为没有改变，故不把上述目标写入 living docs 当作已实现。旧红外计划标 superseded 并指向本节；此前已完成的通风/协议/重载历史保留。新实现每阶段追加开发日记，说明真实验证、文档影响和未完成项。

Outcome：已完成源码核实及工程复查，实施尚未开始。已删除持久finiteNodeCount、R32UI标记附件和过细背景表，新增细节限定为既有kind枚举、phase激活mask、worker一次性恢复mask、稀疏checkpoint/offsets、当前sample中的辐射温度、729值背景及加载位/版本、R16I热附件/owner metadata、既有source revision token和实体表面值。临时reset/source scratch有明确边界。性能数字除引用旧记录外均为预算或验收目标；最终GPU选型保留一次同画面对照，不以纸面减少步骤冒称全硬件最优。

## 先前方案与实施记录

以下内容为2026-09-08至2026-09-09采用的方案与实施记录。其已通过的测试不证明上述新材料闭环已经完成。

## 确定范围

保留 16³ Page、4³ Brick、稀疏驻留、source ledger、解析场与现有求解器。删除单个方块内部的微格分割、微格体积计算、精细空隙与面片列表。每个方块用数值通风率 V 表达通透程度：0 不通风、100 为普通空气参考、未知方块兜底 75。楼梯等部分形状首版同样近似为 75。缺失或未加载状态仍不可用，不能当作未知方块。

保留暴露面积：从方块六个外表面与可通风邻块的接触统计，不再统计内部微格表面。普通材料沿用 `surfaceCapacityJPerK * exposedArea`，不是统一乘 6；通风率不作为体积或容量倍率。不维护方块内部两套空气／实体温度。无暴露材料容量的通风节点使用已有空气参考容量，以保持孤立通风位置可接收 source；有暴露材料容量时使用该材料容量，不叠加内部空气容量。此处只使用一个有效节点和温度。

已知完整实体和 T1/T2 为 V0。门／活板门关闭 0、打开 100；流体整块阻挡优先。有效相变配方保留 reservoir、潜热和 ACK 流程。保留既有材料分类；未知动态形状进入普通材料分类，不能令整个 Brick 编译失败。不顺带改变含水材料分类或增加地热。

## 实施

1. 启动期编译数值通风率及材料 profile；静态形状只判 empty/full/partial，动态形状不作世界查询。删除生产微格几何表。
2. `BlockBrickLayout` 保存 `byte[64] blockToNode` 与 `long[K] nodeBlockMasks`。仅合并相连的无材料 V100 方块；完整空气 Brick 仍只有一个节点且不分配混合布局。V75 和带材料的通风方块各自保留节点；V0 材料仅在暴露时建立节点。节点顺序为 transport、材料、phase。
3. 内部只遍历 144 个正向方块面，跨 Brick 对齐 16 对方块；完整空气双方直接编译一个面积 16 的连接。无向面由负坐标侧持有，包括材料／phase-only Brick。删除持久面片联系和重复材料坐标目录。
4. 通风交换使用 `G=K*A/(dL/pL+dR/pR)`，`p=V/100`，到面的中心距离至少 0.5。材料与 transport 用现有材料面导热参数。天空沿用既有资格及风系数，编译时乘 p；不为缺失邻区制造额外散热边界。保持指数交换内核。
5. 容量与暴露面积相关，因此边界变化可能使相邻材料节点容量变化，必须在最终签名／驻留 cut 上收集布局依赖；以暴露面数变化判断，而不只检查材料是否首次暴露。不递归扩散布局依赖。连接重编使用 `cellsResolved && compiled.resolved()`，resolved 翻转标记 source 重绑。
6. source、环境查询和红外按整块定位唯一 transport 节点。V75 接收完整端口功率，阻力只作用于后续交换。arena 中心从块成员 mask 求出，不增加 XYZ 中心数组。
7. 在线迁移最多扫描 64 块，以旧节点容量份额承接旧焓；相同材料改变暴露面积／门状态保留焓，再由新容量得温度。真正替换材料按自然温度初始化。phase 单独延续请求身份和焓。保留结算、staging、commit、重绑、释放旧 span 的顺序及预算。
8. 休眠直接采用唯一新格式：Brick 均值，加可选整块成员 mask／温度残差；数值载荷上限 640 字节，精确项全部装得下才保存，否则均值。均值按同一已发布布局的完整方块数量加权，不为休眠另外发布每节点容量，也不读取更新后的世界状态。恢复按空间温度聚合到新节点，不宣称离线能量守恒。无旧存档兼容层。
9. 保留主线程变更中心读取；同 Brick 的就绪中心合并后一次编码。替换 sparse face 查询的旧 face-port 循环，保留迟滞、天空排除与驻留预算。

## 验证与结果

只编译生产／GameTest，并运行真实 Forge GameTest；不写或运行 JUnit。验证空气快路径、未知75、楼梯、门、材料暴露面积及焓迁移、跨 Brick/Page、相变、generator source 与解析保底、休眠格式和生命周期。性能结论依据实际节点／边数和生产运行，不能仅凭布局载荷公式宣称全场景最优。

本文件集中记录进度和最终结果，不为每次澄清新建 Markdown。

2026-09-09 00:58 完成：生产编译通过，最终 **35 项真实 Forge GameTest 全部通过**。命令为 Java 17 下的 `./gradlew.bat runGameTestServer --offline --no-daemon --console=plain`；输出见 `run-gametest/block-model-verification.log`，服务端日志见 `run-gametest/logs/latest.log`。

- 实际世界捕获验证 75/75、75/100 和跨 Page 面连接；跨 Page 开洞使材料暴露热容从 900 变为 1800，迁移与内部交换保持总焓；两个完整空气 Brick 仍为两个节点、一个连接。
- 楼梯与未知动态方块能接收真实篝火 source，各自保留通风阻力；两处同时改为空气后正确合并，经过实际 mutation/capture/worker/publication 路径。
- 新休眠格式的生产样本为 48 个数值字节、4 个位置 mask，通过 NBT 往返；640 字节预算包含 mask 和按 long 打包后的残差。此验证不等同于服务器进程重启的专项测试。
- 塔体与排气口同 Brick：密闭物理温度约 29.86°C，高于约 25.10°C 的解析下限。露天塔物理温度约 -10.59°C，最终由解析场保底到 19.5°C。修正了旧测试把露天排气口放进天然深板岩的布置，现明确要求空气与可见天空。
- 待加载篝火“不实例化 BlockEntity”的断言移至生产 `discoverChunk` 返回时检查，再验证后续正常 ticking 仍保持 source；不再把延后 40 tick 的实体生命周期行为归因于发现函数。
- 生产热模型已移除旧微格几何、体积和面片调用。增量中心线性扫描，同 Brick 一次编码；未新增每 Page 的可变 Brick 缓存或新的调度系统。

范围与限制：未写或运行 JUnit，也未迁移 `src/test` 中引用已删除几何接口的旧 JUnit，因此本次通过的构建目标是生产／GameTest，不是包含旧 JUnit 的默认 `build`。尚无全服分配量、retained heap 和同场景前后耗时基准，不能把结构缩减称为已测得的全场景最优性能。已有 runtime 架构文档同步更新，按用户要求不新增开发日记。

## 实现复查（2026-09-09）

状态：完成；下列问题已由当前调用链确认并修复，真实 Forge GameTest 已重新通过。

- 删除无人调用的 `PrimitiveTopologyScratch.OwnerLongInt`、`ThermalCellArena.isMixedComponent` 与新布局未使用的 `nodeCount`。
- mixed transport 节点按序连续分配，节点编号等于 `slot - supportRef`；删除 arena 的重复 `int[] mixedComponentIds`。
- 相变迁移已经限定同 Page 生命周期、同 Brick，只需 slot 列表和 arena 内的 profile；删除 worker 相变目录的重复坐标、profile 数组及包装对象。
- 相邻 Brick 自身签名未变时，暴露面积只可能在外边界变化；仅比较这些面的净变化，保留角点多面增减抵消的正确性。跨 Brick 连接每个邻区只取一次 Page/cut，天空面直接使用 owner cut。
- 不新增缓存层或调度；通风率、暴露热容、相变 ACK、物理 source 和解析场语义保持现有约定。

首轮验证：生产／GameTest 编译通过，35 项中 34 项通过。容量恢复测试失败：测试以 `highWaterMark + 1` 假定仅余一槽，但 `allocateSlot` 优先复用 free list。修正真实世界测试布置，在第一个火源已登记后限制增长，并用实际有燃料的篝火占满回收槽；保留第二个火源被拒绝、chunk 排队、移除第一个后恢复的全部断言。未修改生产 source 分配器。

2026-09-09 01:12 最终结果：同一 Java 17 / `runGameTestServer --offline --no-daemon --console=plain` 命令 **35 项全部通过**，`BUILD SUCCESSFUL`；最终证据见 `run-gametest/block-model-review-final.log`，首轮失败保留在 `run-gametest/block-model-review.log`。`git diff --check` 通过，生产／GameTest 无已删符号调用。

可确定的代价改善：arena 少一条按容量分配的 `int[]`，数值载荷减少 `4 * arena容量槽位数` 字节，同时省去初始化、扩容复制和清理；每个含相变 Brick 少四个重复 int 数组及目录包装对象。邻 Brick 检查的外部位置比较上限由 64×6 降为 6×16（材料过滤和首次差异退出仍有效），跨面不再对 16 对方块重复解析同一邻 Page。此为代码路径与载荷核算，不是服务器耗时或 retained heap 实测。更新已有 runtime 架构文档；未新增文档文件、JUnit、缓存层或调度系统。

## 红外解析场接入：按需生成显示 Page/Brick（2026-09-09）

作者：Codex / OpenAI GPT-6。状态：**用户已授权实施，明确批准96字节通用显示Page刷新标记及最小分包方案；代码修复与限定范围的真实Forge验证完成**。新增机制仍需先说明必要性与代价，由用户判断。

### 已确定的边界

- 服务端负责最终温度计算；客户端只知道显示位置、有效性和温度，沿用解码、纹理上传及配色，不执行解析场或自然温度公式。
- 以显示 Page/Brick 为按需查询、更新和编码单位。这里的“激活”指产生/更新所需显示数据，不是创建物理模拟 Page、arena 节点、worker 或驻留请求，也不是给每个 Brick 新建一个解析器对象。
- 原有 9³=729 个 section 地址是固定显示窗口，原有纹理为 144³；本方案复用这些地址，不要求分配729个热模拟 Page，也不把现有固定纹理说成已经实现的稀疏客户端系统。
- **撤回把解析场纳入休眠覆盖系统的方案**。休眠仅在需要时提供已有温度记录；解析场的有效性、更新和退出不得借用休眠保存、衰减或覆盖所有权。不存在“解析场休眠 Page”这一新类别。
- generator 的 FLOOR_FROM_NATURAL 与 Boss 场一起接入服务端最终合成。自然温度只在服务端按需读取既有计算结果，不新增客户端自然温度同步，不延期 generator 保底显示。

### 复用原有温度链路

1. 沿用每40 tick错峰请求及窗口移动时的full刷新。服务端在显示范围内确定需要输出的Page/Brick，按需读取原始温度并合成；不每tick遍历整个144³方块窗口。
2. Page只做显示组织地址。一次定位已有publication，再按Brick读取各唯一transport节点，用现有成员布局展开到方块；不对64个点重复查Page或重复读取同一节点。
3. 解析场定义仍在现有ThermalAnalyticFieldIndex集中保存。对本次显示Brick筛选相交场，再对必要的位置计算；不创建每Brick解析器、场副本或独立客户端场通道。
4. 取温遵守现有可用性规则：有有效物理温度则读取；需要时取已有休眠背景。命中解析场但基础温度不可用时，服务端按该场规则读取自然温度。按现有mode/priority/key顺序合成，最后量化；无场位置保持原红外行为。
5. 无驻留物理Page时，仍能按显示地址直接生成解析场温度。只访问已加载世界数据，不为显示加载区块或启动热模拟。没有active runtime不能成为丢弃全部解析场显示的条件。
6. 每个输出Brick只发送最终温度。复用InfraredBrickCodec的INVALID、UNIFORM、INDEXED、RAW，不先传原始温度再追加同位置的场覆盖层。只有最终64个值确实一致时才用UNIFORM；物理Brick是纯空气不代表跨场边界后的显示温度也一致。
7. 客户端继续写同一温度镜像、按变化Page上传原有R16I纹理、由现有shader取温配色。没有第二张温度纹理、第二份全尺寸镜像或客户端场解释器。合成结果不回写query、arena或休眠存档。

### 显示更新必须解决的问题

这些是正确性要求，不能靠借用休眠生命周期掩盖：
- 物理温度变化、当前解析场新增/改变、场移动/缩小/消失都可能改变最终显示值。场内自然基准变化也必须在后续红外刷新中体现。
- 旧范围退出解析场后，输出该点现在的基础温度；没有可用结果则发送原有INVALID。整个显示Page无数据时才移除其显示存在信息；同Page内只退出一部分Brick不能依靠删除整个Page处理。
- 原worker infraredEpoch只反映物理发布，不能直接当作最终显示版本；原knownPresence只有Page存在位，不能凭空推导上次场覆盖的精确Brick。
- 若复用现有presence/变化标记，需要先核对所有读写方与全量、增量、物理不可读、休眠显示清理的关系，防止更新遗漏或误删。客户端显示有效性不应等同于worker驻留有效性。
- **显示变化记录已获用户明确批准**：客户端保存/回传12个long（96字节）的knownRefreshPages，响应给出refreshPages。服务端重建旧标记Page与当前场相交Page的并集，每Page输出64个Brick的最终结果；场退出后，新标记不再包含该Page。没有逐Brick场mask、场定义或服务器逐玩家记录。代价是相关Page中未变的Brick也可能重发；完整请求不传旧标记。

### 新增项的约束

| 项目 | 当前决定 | 理由 |
|---|---|---|
| 用休眠覆盖mask或BACKGROUND记录管理解析场 | 撤回 | 混合了温度来源与显示生命周期，增加语义耦合 |
| 独立解析场客户端协议、场参数、自然温度同步 | 不采用 | 客户端只需最终温度，原Brick载荷可以表达 |
| 每Brick解析器/场副本 | 不采用 | 按需调用现有集中场定义即可，不需要大量对象 |
| 通用显示Page刷新标记 | 用户明确批准96字节方案 | 精确定位需重建的Page，保留纯物理Page原增量；不加服务器逐玩家缓存 |
| 独立MinecraftInfraredFields服务或编码器 | 不默认保留 | 优先在原红外组包与Brick取温边界接入；拆分须说明实际必要性 |
| Page边界分包 | r128实测超限后已获用户明确批准并实现 | 原标志字节编码FULL/FIRST/LAST，客户端只增加一个接收中布尔量；不加场通道或第二份温度镜像 |

### 性能目标与验证

保留未受解析场影响区域的原有快路径，把额外逐点计算限定到需要更新的显示Brick。设B为本次要重新生成的Brick数，F_b为各Brick的相交场数量，逐点合成上界约为64×ΣF_b；Page查找与唯一节点读取不能重复嵌套进每点的场遍历。需要恢复的旧范围如何确定，其时间和内存成本必须计入，不能只报合成函数的成本。

原uniform/indexed/raw压缩与每变化Page 8192字节的GPU上传继续复用。减少服务器读取、压缩字节和客户端上传是不同指标，不能用“少一个数组”代替整体成本判断，也不声称在未实测时已经达到绝对最优。

用户允许动代码后：
1. 先列明此前未完成尝试中需要保留/撤回的具体变更；按新方案处理，不整仓回退，不动此前已验证的方块级热模型。
2. 核对显示更新的必要状态并取得所需决定，再接入原红外Page/Brick取温与唯一最终记录输出。
3. 真实Forge GameTest覆盖generator保底、物理温度超过保底、Boss形状/叠加、同Page移动/缩小/删除、休眠仅作数据来源、无runtime且零新增热学Page、负坐标/跨Page、编码往返与最大支持载荷；不写或运行JUnit。
4. 真实客户端验证无残影、窗口变化、开关红外、世界退出；测r16/r24、大场、移动Boss和多观察者的组包耗时、读取/合成点数、字节、上传及帧时间。编译前确认共用输出目录的runClient已退出。
5. 实施完成后更新原runtime文档及本plan，分别记录编译、GameTest和客户端验证结果，不互相替代。

### 当前代码状态

已撤回独立MinecraftInfraredFields、逐Brick field mask、分段响应及BACKGROUND记录标记。原红外捕获代码与缓冲移入MinecraftThermalInput.InfraredCapture，按服务器主线程请求复用一份，物理runtime可选；没有第二条场显示通道。真实休眠背景仍沿用其原记录，在每Page的普通最终温度记录之后输出，只保留真实休眠Brick的所有权。物理读取临时失效时回退该Page的未提交字节并保留刷新标记，下次重试；不把原来的热区误清成自然温度。

生产编译已通过一次；后续小修复、真实GameTest与实际payload测量仍在进行。此前35项通过的结果不代表新增红外接入已经验证。客户端仍使用同一温度镜像和纹理，没有解析场业务或自然温度同步。

### 2026-09-09 18:09 真实测试结果

- 先前38项运行37项通过；远处篝火fixture等待期间缺少区块保留ticket，物理温度为NaN。为该真实测试保持目标Chunk加载后，原升温、200°C场覆盖、同Page移走后恢复实际物理温度的断言均通过；未改生产热源或放宽断言。
- 用户已注释掉落物生产接口调用并要求暂不处理该功能，但其旧GameTest类仍引用缺失接口。本轮用临时Gradle init脚本仅排除FrostedHeartMinecraftThermalInputGameTests整个类，因此实际执行24项，不能声称是完整套件；其中新增4项红外测试均运行。
- 检测到runClient运行，本轮命令显式跳过compileJava/processResources/createMcpToSrg/createSrgToMcp，复用现有生产产物，只编译GameTest。日志：run-gametest/infrared-page-verification-4.log；临时范围脚本：run-gametest/infrared-test-scope.gradle。
- **23/24通过**。generator r16实际响应17276字节、单次6.21ms；r24为41602字节、单次18.35ms；Boss组合为2762字节、单次1.73ms；恢复篝火实际物理温度为324字节、单次0.18ms。这里只记录此次生产运行观察，不作为稳定性能分位或全场景最优结论。
- 唯一失败为largeGeneratorFieldFitsTheProductionDisplayPayload：真实GeneratorData设RLevel=15（r128）、TLevel=3，中心y240，加载11×11 Chunk。当前编码写到983035字节，下一次写8字节超过现有983040字节上限。未扩大限制、未截断或跳过该测试。大范围解析场的完整显示尚未完成。

当时提出的分包扩展随后获得用户明确批准。最终实现进一步省掉独立顺序号，使用TCP顺序、原requestId、原标志字节中的首包/尾包位，以及客户端一个接收中布尔量。下方记录最终修复与验证；运行中的客户端仍不得与生产重编译共用输出目录。

### 2026-09-09 19:11 审查修复结果

- 请求结束时清除InfraredCapture借用的Page handles与InfraredReadCursor引用，保留可复用数组，避免静态scratch延迟释放已退休Page和查询缓冲。
- 完整刷新若有Page未能读取完整物理数据，返回无响应并沿用现有full重试；不会发送一个先清空客户端、却缺少该Page内容的full响应。普通增量继续用Page回退与刷新标记重试。
- 未受场影响的无数据/普通空气Brick直接写INVALID/UNIFORM，不再展开64个相同值、重复量化和扫描字典。
- Builder在Page开始前预留最大完整Page记录空间，只在Page边界拆分，每包仍不超过960 KiB。服务端完整捕获后才发送已编码parts；客户端只在LAST提交纹理、窗口原点和增量基线，中断走full恢复。没有增加持久温度镜像或观察者缓存。
- 自然温度仅对需要的位置计算；证明原生BiomeManager可能选择的3³ quart采样具有同一biome后，复用已有节点scratch保存Brick四个高度的温度。混合biome保持逐点原函数。没有近似取样或长期缓存。进一步的整页复用尝试未证明明确收益，已撤回，最终未保留额外分支。
- 测试还复现了已有BlockRadiationIndex熔岩侧面检查的双轴跨区段越界：x/z边界邻块上方使局部y=16。修正为实际对角区段和三轴局部坐标；普通单邻面继续原缓存，没有新增对角缓存。

最终命令：Java17下运行runGameTestServer，使用run-gametest/infrared-test-scope.gradle临时排除含未接通掉落物接口的FrostedHeartMinecraftThermalInputGameTests类。**26项真实Forge GameTest全部通过，BUILD SUCCESSFUL，git diff --check通过**。日志：run-gametest/infrared-review-fixes-final.log。未写/运行JUnit，未修改用户暂时停用的掉落物调用。

覆盖包括：借用引用释放、物理读取关闭时full响应不提交、真实篝火热温度恢复、generator/Boss移动删除与合成、混合biome逐块对照原自然温度、熔岩X/Y及Z/Y跨边界、r128多包完整解码和请求/响应wire往返。

最终r128载荷1104489字节，2包；同场景首采集413.99ms、两次连续采集308.90/305.75ms。此次r16为12.57ms、r24为14.72ms。各轮受JIT和测试执行环境影响，不能挑选最快单次数字宣称固定加速比例；满窗口重算仍存在明显CPU峰值。上述数据是服务器组包观测，不是客户端FPS。客户端实际图像与分包帧提交尚未现场验证；本次通过的是明确列出的限定测试范围，不是完整测试套件。

### 2026-09-09 分包重试与编码扫描修复

作者：Codex。状态：已实现，生产编译与限定范围真实Forge测试通过；客户端慢网实测待完成。

- 服务端一次性发送同一响应的所有分包，TCP保持顺序；客户端沿用receivingResponse暂停接收期间的定时重试，避免旧请求时间达到41–59 tick就丢弃仍在接收的响应。等待首包仍重试，窗口移动仍立即发full。无需新增计时器或状态。
- Brick出现第36个不同温度后停止调色板扫描，复用已有RAW分支。此时INDEXED至少130字节，RAW129字节（均不计共享地址），继续扫描不会改变选择；不改协议或新增缓存。
- 验证沿用限定范围真实Forge GameTest及生产编译；专用服务器测试不覆盖客户端慢网接收时序，不将其报告为已实测。
- 结果：27项全部通过，BUILD SUCCESSFUL；日志run-gametest/infrared-retry-codec-verification.log。沿用临时init脚本排除含未接通掉落物接口的旧测试类，未写或运行JUnit。r128仍为1104489字节、2包；本轮采集442.74/435.89/288.53ms，不据此宣称整体固定加速。无新增生产状态、缓存或协议字段。

### 2026-09-09 全窗口上传与物理节点展开最小修复

作者：Codex。状态：已实现，生产编译及限定范围27项真实Forge测试通过；客户端OpenGL实测待完成。

- 在原dirtyUploadPages扫描中计数；729个有效显示Page全部变脏时，复用uploadFullTemperatureTexture，省去逐页复制与729次上传。部分更新保持原路径，尾包提交时机不变，不增加持久状态或缓冲。
- 普通物理混合Brick按transportNodeCount读取及量化节点，再通过blockLayout.transportAt直接展开。保留原infraredCursor与结束时一致性检查，不调用会重新取得cut的copyRawBrick。删除infraredUniqueSlots；原short温度scratch改为按节点编号索引，净减少256字节数组载荷。
- 沿用真实Forge GameTest验证物理取温、场退出与协议往返；客户端OpenGL调用/FPS只能由实际客户端验证，不把专用服务器结果当成客户端实测。
- 结果：run-gametest/infrared-upload-node-verification.log记录27项全部通过、BUILD SUCCESSFUL。现有篝火测试增加移除全部场后的普通物理混合Brick检查：热空气、同Brick石块INVALID、协议往返均通过。沿用原范围脚本排除未接通掉落物接口的旧测试类，未运行JUnit。删除已无调用者的纹理创建包装方法，混合Brick逐块完整赋值后省去预填循环；git diff --check通过。

### 2026-09-09 世界温度生命周期修复

作者：Codex。状态：已完成；Java17生产编译与限定范围29项真实Forge测试通过。

- 温度配方表重建后调用现有WorldTemperature.clear；空配方集合也重建空表，避免保留已删除配置。空气/方块公式与海拔曲线不改。
- 自然边界刷新完成后以实际gameTick加200安排下一次，保留原初始错峰与16项预算，不追赶历史采样。
- 有效机器输出按需启动现有runtime；篝火使用区块加载及已有LevelChunk mixin的首次点燃回调。启动/重载时只检查已加载区块的BlockEntity位置，找到有效篝火即复用原启动及发现队列。不增加tick轮询、持久缓存或新调度器；解析场/被动查询不主动启动物理runtime。
- 同步现有世界温度、runtime与生命周期文档，删除过时的子方块空气、continuation和红外描述。真实测试覆盖配方重建、无人热源启动/重载及过期自然刷新；不写或运行JUnit。
- 最终结果：run-gametest/world-temperature-lifecycle-final.log，29项全部通过，BUILD SUCCESSFUL；仍由临时范围脚本排除含未接通掉落物接口的旧测试类。真实T1/T2、喷泉、散热器改为由生产tick启动及恢复；篝火覆盖点燃、已加载世界重载及待恢复NBT的区块加载入口。温度表测试覆盖原生配方重建和空配方移除；积压测试使用实际source Page的生产队列。git diff --check通过。
- 没有新增长期缓存、定时轮询或独立服务。有效热源现在能让无人维度运行原有物理引擎，其必要驻留与计算是新增的实际工作；冷启动/重载检查只遍历已加载BE位置，不加载chunk。海拔数值曲线和旧城镇模拟器applyHeat保留。

### 2026-09-09 tags绑定与集成服务器同步时序

作者：Codex。状态：已完成；生产编译与限定范围30项真实Forge测试通过，集成客户端并发实机验证待完成。

- 配方监听器只关闭物理runtime并使profiles失效；恢复已有篝火移至现有Forge TagsUpdatedEvent的SERVER_DATA_LOAD阶段，避免按旧tags冻结材料/辐射表。初始启动仍沿用ServerStartedEvent；不增加待办标记、缓存或调度器。
- FHClientEvents.onRecipesUpdated在集成服务器连接中复用服务端已重建静态表，不再从客户端线程重复buildRecipeLists/清理温度缓存。远程客户端继续正常重建。不给取温热路径加锁。
- 真实Forge测试增加完整MinecraftServer.reloadResources调用及实际临时tag数据包，核对重载后的runtime使用新材料分类；专用服务器不能验证集成客户端线程，单独记录限制。
- 结果：run-gametest/world-temperature-tags-verification.log，30项全部通过，BUILD SUCCESSFUL。实际临时数据包将stone加入wool tag，完整重载后自动恢复的runtime采用新材料分类；移除并再次完整重载恢复原分类。原篝火恢复测试也改走完整reloadResources。测试数据包已清理，git diff --check通过。未写或运行JUnit，仍排除含未接通掉落物接口的旧测试类。
- 生产修复只移动启动时机并恢复客户端现有单人游戏条件，不新增持久状态、缓存、锁或调度器。已同步现有runtime、生命周期及热源文档；先前29项结果不覆盖完整tag重载，此轮验证补齐该范围。

### 2026-09-09 19:46 后续边界修复

- 首包已进入CPU镜像但GPU纹理尚未创建时，沿用receivingResponse暂停红外绘制；尾包提交前不调用空纹理初始化，不清掉已收到的数据。没有新增状态或缓冲。
- full请求的整个物理cut不可读时，复用现有Page收集方法确认是否仍有已发布物理Page；有则不发不完整full响应，沿用重试。没有物理Page的解析场仍可正常返回。新增真实测试移除所有目标场并关闭实际publication，验证没有刷新标记的纯物理目标也受保护。
- WorldTemperature.biome改用原map的primitive getOrDefault(biome, Float.NaN)识别未命中，0°C不再被误判。原cache对象与清理生命周期不变，没有containsKey二次探测或新增装箱。真实世界测试验证缓存零值及显式clear后刷新。
- Java17生产编译及限定范围的**27项真实Forge GameTest全部通过**，BUILD SUCCESSFUL；日志run-gametest/infrared-boundary-fixes-verification.log。仍以临时init脚本排除含未接通掉落物接口的旧测试类，未写或运行JUnit。客户端初始化保护已编译并静态核对，实际客户端图像验证仍待完成。现有runtime文档已同步。
