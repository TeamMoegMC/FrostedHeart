# 温度架构检查与可读性、可维护性重构计划

- Time: `2026-09-16 00:15:34 +08:00`
- Updated: `2026-09-16 00:52:38 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `ready`
- Scope: `当前温度系统的职责、依赖、状态所有权、测试及成本约束；本次只调查和规划，未实施生产代码重构`
- Related: [当前架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[温度语义](../docs/climate/world-climate-and-temperature.md)、[数据生命周期](../docs/climate/data-lifecycle-and-integration.md)、[最近时间步修复](../diary/2026-09-16_00-00-26_thermal-elapsed-time-fix.md)

## Goal

目标是让修改一条热行为有明确落点，让维护者能沿入口找到状态、算法、世界提交与显示，而不必理解整个系统才能改局部代码。保持现有游戏规则和实际 CPU/内存成本；以减少修改波及范围、重复规则和隐式时序为验收标准，不以文件数或总行数最少为标准。

2026-09-16 00:38 用户进一步限定：本计划只做可读性、可维护性与命名整理。算法、计算顺序、数据布局、分配次数、缓存策略、线程/锁、更新频率、网络格式和渲染流程保持。下文发现的问题是调查记录，不代表全部都要借这次重构改掉；行为问题只记录，另行决定。

### 当前实施边界：可读性和可维护性优先

以用户最新决定为准：允许必要的结构整理，JIT 内联、方法调用和类元数据等细微影响尽量控制，不要求字节或实测耗时绝对相同。不能为了推测中的微小内联收益保留难读代码。

| 可以实施 | 必须保持 |
|---|---|
| 局部、字段、内部方法和类型的准确命名 | 外部 API 行为、网络/存档/配置键和资源标识 |
| 方法提取、方法排列、必要的类型归位 | 热算法、数值计算顺序、更新频率及线程同步语义 |
| 减少转发和完全等价的重复计算 | 状态唯一归属、相变 ACK、源积分和保存/恢复规则 |
| 少量有明确责任的具体类，清楚的注释与维护文档 | 节点规模、大数组布局、缓存复用、网络载荷和渲染工作量 |

优先移动已有对象和状态；若一个仅随维度初始化的具体对象能实质减少职责混杂，可以采用并说明成本。禁止每节点/每方块包装、逐次查询新建结果对象、额外全量遍历、重复大缓冲及只为“解耦”建立的接口层。改名涉及反射、record component 或注入字符串时，核对实际入口，不作盲目文本替换。

性能验证集中于确实受影响的频繁调用和批处理路径。细微 JIT 差异不是拒绝合理重构的默认理由；可重复的明显 CPU/分配退化需要处理。不把“尽量保持性能”扩大成更换热模型或另做一轮性能架构设计。

### 人能直接读懂的代码标准

- 先整理现有方法的名称、排版和局部变量，再考虑提取方法，最后才考虑移动类。禁止把拆类数量当作成果。
- 名称说明实际动作和对象：例如迁移循环里的 `n/first/os/ns/pb` 改为 `nodeCount/firstSlot/oldSlot/newSlot/pageBlockIndex`；材料迁移中 `previous/initial` 已除以节点覆盖方块数，应写为 `previousEnergyPerBlockJ/initialEnergyPerBlockJ`。极小循环的 `i` 和坐标 `x/y/z` 无需机械加长。
- 修正已经不符合模型的名字：`removedReservoirSlots` 实际是移除的可相变材料 slot，可改为 `removedPhaseSlots`；检查点也使用的 `InfraredReadCursor` 应表达发布快照读取。先确认调用语义，再整组改名；不为了“专业感”造新术语。
- Page、Brick、slot、enthalpy 是已有明确含义的术语，保留并在入口解释一次；避免同一概念混用 body/pole/reservoir 等名称，也避免给每个变量叠加 physical/material/thermal/runtime 前缀。
- 一行一个主要操作；复杂分支展开，不把声明、赋值、循环推进、条件返回挤成一行。使用正常 import，避免方法体里反复写全限定类名。
- 注释解释必要的原因和先后关系，例如“先结算旧连接上的供能，再迁移节点”；删除重述代码、空泛描述架构或已经过时的注释。公式说明单位，位打包说明字段和位宽。
- 主流程能自上而下阅读；提取的方法对应完整动作，不能为缩短行数制造大量单行转发。参数直接表达需要的数据，不用万能 Context 隐藏依赖。
- 同一规则共享实现时必须保留原分支顺序、算术表达式和结果写入时机；不能趁机重写公式或修复边界行为。无法明确证明等价的整理暂缓。
- 新名称不意味着新数据结构。已有嵌套对象可直接移到独立文件，保留原实例和数组；不默认增加协作者对象、结果对象、包装层或 getter 集合。

本计划包含命名、表达与下文六批必要的结构整理。用户已明确不恢复失效 JUnit。`ready` 表示实施边界和验收条件明确，生产重构尚未开始。

### 命名落点

以下名称随直接调用者一并整理，不保留旧名称转发。类型提取仍需满足下文的维护收益条件，已有清晰名称不动。

| 位置 | 当前名称/表达 | 整理方向 |
|---|---|---|
| `BrickMigrationKernel.migrate` | `old/next` | `oldBrick/newBrick`，与旧/新 slot 明确对应 |
| 同上 | `b/pb` | `blockIndex/pageBlockIndex`，区分 Brick 内 0～63 与 Page 内 0～4095 |
| 同上 | `enthalpy/temperatures` scratch | `nodeEnthalpiesJ/blockTemperaturesC`，把索引单位和数值单位写出来 |
| `TopologyPlan` 与提交载荷 | `removedReservoirs/addedReservoirs` | `removedPhaseSlots/addedPhaseSlots`；只指可相变材料，不泛指所有节点 |
| `QueryPublication` | `InfraredReadCursor` | `ReadCursor`；方法同步改为 `beginRead`，保留与该发布器相关的内部访问 |
| 共享材料样本 | `QueryPublication.MutableMaterialSample` | 如提取则为 `mesh.MaterialSample`；Javadoc 写明调用者复用的可变结果，不增加同名包装 |
| `SectionOwner` 材料记录 | 三元组/四元组的 `index * 3/4` | 各自在拥有记录的类声明 stride 和字段偏移，注释记录格式和清除时机 |
| `MinecraftThermalInput` | 环境/材料/红外变量中泛称 `temperature` | 在可能混淆的边界分别使用 `naturalTemperatureC/materialTemperatureC/displayTemperatureC`，小函数内不重复长前缀 |
| `WorldTemperature.block/air` | capability/provider 旧注释 | 写明玩法环境合成和自然基线；本轮保留这两个外部 API 名称及行为 |

范围标记约定：`pageSlot` 是 worker 目录索引，`firstSlot/nodeSlot` 是 arena 索引，`blockIndex` 必须由方法上下文确定 Brick 或 Page，跨两种范围时加 `page` 前缀。`revision` 表示几何变更版本、`generation` 表示身份代次、`sequence` 表示批次/请求序号；不要全部改叫 version。

位打包保留原布局：Page 方块索引为 `x | z << 4 | y << 8`，Brick 方块索引为 `x | z << 2 | y << 4`，局部坐标范围分别为 0～15 和 0～3。优先调用已存在且含义相同的 `BlockBrickLayout.pageBlock`；不要额外创建坐标对象。位掩码不能改成逐位布尔数组。

## Verified Current State

### 检查范围和证据

本次追踪了自然温度、玩家与被动查询、热源事件、主线程捕获、worker cut、拓扑准备/提交、Air/材料交换、相变 ACK、休眠恢复与存档、配方编译入口、红外同步/后处理、测温与植物提示调用链。数值内核和高耦合类读取实现；周边体温、热网、内容生成读取入口和现有文档，不宣称逐行证明所有内容或客户端效果。

检查时 `content/climate/thermal` 有 85 个 Java 文件、23,633 行（含注释/空行）。重点文件：

| 文件 | 行数 | 实际承担的职责 |
|---|---:|---|
| `runtime/minecraft/MinecraftThermalInput.java` | 1904 | 维度运行时、事件入口、各类查询、红外采集、检查点与休眠交接 |
| `runtime/minecraft/input/MinecraftPageManager.java` | 1627 | Page 驻留、采集预算、Section 变更追踪、halo、存档投影 |
| `radiation/minecraft/BlockRadiationIndex.java` | 1041 | 静态辐射几何索引与增量维护 |
| `radiation/RadiationService.java` | 1010 | 受体查询、候选选择、遮挡及复用缓存 |
| `topology/TopologyPlan.java` | 947 | 拓扑准备、依赖收集、预算、迁移与构建提交载荷 |
| `mesh/ThermalCellArena.java` | 938 | 原始数组、slot 分配、能量及材料状态操作 |
| `topology/WorkerPageStore.java` | 910 | worker Page 状态、驻留请求、查询几何与目标解析 |
| `query/QueryPublication.java` | 827 | 查询双缓冲、材料检查点字段、红外变化标记及 hot mask 写入 |
| `solver/ThermalSolver.java` | 817 | 编译连接安装、引用维护、顺序换热与休眠残差 |
| `source/ThermalSourceLedger.java` | 804 | 源事件时间积分、绑定和能量账本 |

行数仅用于定位。辐射索引与求解器有实际紧密共享的数组和遍历状态，不能仅因文件长就强制拆分。

### 当前架构与单位

```text
WorldClimate + dimension/biome/altitude
                  │
                  └─ WorldTemperature.naturalAir / naturalBlock
                                      │ 自然边界
机器正常 tick / 篝火发现                ▼
        └─ PhysicalSourceSpatialIndex → DimensionInputAccumulator
方块变更 / Page 采集 / phase ACK ───────┘
                                      │ ThermalInputBatch
                                      ▼
                               单维度 mailbox
                                      ▼
ThermalDimensionEngine:
  ACK/意图/风 → 源按时间结算 → 失效标记 → 拓扑准备/提交
  → 源重绑 → 释放旧 slot → 实际 dt 换热 → phase 请求 → 查询发布/驻留
                                      │
              ┌───────────────────────┴────────────────────┐
              ▼                                            ▼
     主线程应用方块转换并 ACK                    查询 / 检查点 / 红外同步

独立玩法通道：解析场合成；静态辐射受体查询；热网库存；玩家五部位体温
```

| 模型 | 状态和单位 | 维护时不能混淆的边界 |
|---|---|---|
| 自然温度 | °C；世界、群系、高度和气候函数 | 是边界条件，不是已加热材料的 H |
| 活动 Air | `ThermalCellArena` 中 H，J；C，J/K；T=参考温度+H/C | 合并真正的 Air 区域，非每个空格必有独立节点 |
| 活动材料 | 每个建模材料方块一个 H；`MaterialThermalLaw` 的 C/O/相变边共享 | 显热 H=O+C*T；平台根据 branch 读温度；暴露面影响 G，不改变整块 C |
| 部分方块通风 | `AirRouteCompiler` 的几何路径/阻力/有效性 | 没有楼梯气隙热容或第二个温度；其材料 H 仍独立 |
| 休眠 | Chunk 上稀疏 Air 检查点和 `MaterialSectionState` | 非另一套持续 tick 模拟；查询/恢复时惰性自然换热，不加载区块 |
| 解析场 | `MinecraftGameplayFields` 世界生命周期索引；温度合成 | 不进源能量账本；明确温度下限仍可按玩法触发相变 |
| 静态辐射 | 配置的发射功率/辐射温度，受体得到 W/m² | 火/岩浆静态辐射是游戏平衡，不改成材料动态辐射 |
| 玩家体温 | 五部位能量；整人体热容 245,000 J/K | 消费环境与辐射；不是热网 heat unit，也不是一个世界材料节点 |
| 热网 | 机器 heat/tempLevel 等玩法库存 | 仅在机器明确出口处转成物理 source，不能按名称当成 J/W |
| 红外 | 活动/休眠材料温度与解析场的显示数据 | 非地形遮挡物使用既有显示体积纹理近似融入环境，不是同步实体体温或真实 Air 场 |

### 当前必须保留的工程与玩法约束

- 2026-09-16 用户另行授权导热规则修正：`MinecraftThermalProfiles` 不再按相变能力覆盖 G。默认采用材料分类（泥土 1.0 W/K、石头 1.4 W/K），显式配方 `conductance_w_per_k` 优先；旧 `phaseFaceConductanceWPerK` 配置删除。这是重构前已实施的行为修正，之后整理代码须保留该新基线。
- Page 为 16³，Brick 为 4³；每 Brick 普通布局最多 64 个节点。一个建模材料方块一个状态，不新增楼梯双节点或统一细网格。
- 从受热表面最多进入两层连续材料；`TopologyView.materialContactAllowed/ownsInnerLayer` 的表面与内层归属限制保持。表面之间、单个内层归属及第三层截止的行为都要测试，不能只把变量名改成 depth 后重写规则。
- 真正水平接触不施加浮力；近正功率 AIR_FACE 出口四格范围内直接 Air 接触 4×G，重叠取并集。保持现有间接通风规则。
- 新活动/扩张 1°C、保留/释放 0.5°C 是相对自然基线的滞回，不是把远处温度钳制到某个值。
- 普通 cut 20 tick；延迟 cut 按 `elapsedTicks/20.0` 换热，一次遍历。源先结算再迁移，保持求解顺序和正反扫次序。大步近似不改成历史子步重放。
- `WORK_LIMITED` 保留待办；几何已失效的空气通路不继续换热；仅系数变化的待办与几何失效不是同一件事。
- 活动材料潜热完成后等待世界 ACK；没有记录的环境平衡转换、休眠相变端点、活动 worker 请求是三种状态入口，不是应删除的兼容模式。
- 水继续使用原 Chunk 地表列采样，不给整个海洋新增随机 tick 资格；自然岩浆初始材料温度不在结构重构中重平衡。
- 红外地形使用真实顶点归属，保留现有 SOLID/CUTOUT 捕获、地形深度快照和最终混合。实体环境近似、缺失值冷蓝底色、−20～20°C 色带和 0.43 混合保持；不增加 Mixin 或重绘地形。

## Findings

本节记录调查发现及整理方向，按下方六批步骤实施；行为问题 B1 继续排除在本次重构之外。

### R1：总入口承担了五种独立变化原因（优先）

证据：[MinecraftThermalInput](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java) 的 `tick/drainCompletion`、`gameplay*Environment`、内嵌 `InfraredCapture`、`captureDormantPage/onMaterialBlockChanged`。修改显示编码需要进入运行时生命周期类，材料存档也引用该入口的查询游标。

处理：先把现有方法按生命周期、环境读取、材料读取、存档、红外分组，并将高耦合点命名清楚。`InfraredCapture` 是已有独立对象，作为文件提取候选；材料读取/检查点先整理现有方法，不预设新协作者。只有直接搬移实现和原状态、无新增对象且减少阅读跳转时才提取。入口保留真正需要的公开 API，不增加一层转发后仍把计算留在原类。

### R2：主线程依赖成环（优先）

证据：`MinecraftPhaseController.apply → MinecraftPageManager.matchesMaterialRequest → MinecraftThermalInput.matchesMaterialRequest`；`MinecraftPageManager` 多处经 `input` 获取休眠 admission、捕获检查点、查询材料发布 revision，`SectionOwner.pruneMaterialCheckpoint` 又回调入口。

处理：先明确每个回调的职责和真实依赖；能通过已有对象直接调用且不改变状态归属时，删除中间转发。不能为了画出单向依赖图引入协作者实例、捕获 lambda 或公开内部容器。源发现队列的归属和排队/清空时机本轮保持。暂时保留的必要回调在方法旁说明原因，不把“零依赖环”设为强制完成指标。

### R3：同一材料变更规则有两份实现（优先）

证据：[BrickMigrationKernel.applyMaterialChanges](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickMigrationKernel.java) 和 [MaterialSectionState.Editor.applyChange](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/persistence/minecraft/MaterialSectionState.java) 分别判断 `REPLACE/MASS_CHANGE/GAMEPLAY_TRANSITION/THERMAL_TRANSITION`。`afterMassChange` 公式已共享，但原因分派、branch 重置规则还没有统一。

处理：先按变更原因逐项对照两份实现。只提取输入语义、分支与运算完全相同的能量转换片段，优先在已有 `MaterialThermalLaw` 放置返回 primitive 的方法；不创建新的变更结果对象或策略类。活动入口保留外部能量计账，休眠入口保留先按时间投影，branch 和删除记录的操作保留原时机。若共享整份分派需要包装参数、增加判断或改变特殊分支，则只共享共同公式并清楚命名两入口，不以消除所有重复为目标。

### R4：存档和热学工具依赖查询发布内部类型（优先）

证据：`DormantThermalCooling`、`MaterialSectionState`、`BrickMigrationKernel` 使用 `QueryPublication.MutableMaterialSample`。保存材料状态却必须引用发布器内嵌类型；`InfraredReadCursor` 同时被材料检查点使用，名字掩盖了真实职责。

处理：将已有可变材料样本移动到材料/mesh 层，保持原字段与调用者复用方式；直接替换引用并移除原嵌套类型，不保留别名包装。游标改名表达一致发布读取，仍复用现有对象和版本验证。H/law/branch 是共享材料语义，sampleTick/requestSequence 是样本元数据，不能误复制成第二份活动权威状态。

### R5：准备阶段并非完全无副作用，顺序契约藏在调用之间（优先）

证据：`ThermalDimensionEngine.process` 先 `sources.acceptAndAdvance`，再 `TopologyPlan.prepare`；后者调用 `airRoutes.invalidate`，且会持有跨 cut 待办和 staging。随后 commit、源重绑、旧 span 释放、query 发布分别完成。`restorePagePublications` 只恢复 Page 发布引用，不是完整回滚求解器。Engine 类头注释未明确最重要的源结算顺序。

处理：保持一个编排方法按实际阶段从上到下展示；在 `prepare/commit/release/restore` 方法旁写清可变状态、失败后谁保留待办、哪些引用已经可见。类内方法提取优先，不为每个阶段新增对象。禁止把“prepare”误当纯函数而提前重试/并行化，也禁止把发布回退扩成第二套事务框架。

### R6：SectionOwner 隐含了两种不同变更记录（次优先）

证据：`MinecraftPageManager.SectionOwner` 有 `pendingMaterialChanges` 四元组和 `checkpointMaterialChanges` 三元组，后者另配 revision/tick；前者用于 worker 几何迁移，后者把已发布检查点投影到最新世界。还有 dirty 位图、待主线程失效标志以及 Page 身份。

处理：先整理 SectionOwner 的方法分组和命名；只有移动后确实更便于维护才移为独立类型，保持每 Section 一个原对象。为两种记录分别命名 stride/字段偏移和消费时机，不额外创建每 Section 记录组件。两份记录的消费者和清理时机不同，不能仅因字段相似删除其中一份。

### R7：温度 API 名称掩盖不同语义（次优先）

证据：`WorldTemperature.block` 调用 `gameplayPassiveEnvironment`；`material` 才读取本体。`SoilThermometer.reportMaterialTemperature` 读材料，`SoilThermometerRequestPacket` 读环境且最终进入 `TemperatureGoogleRenderer → PlantTempStats` 的生存/生长判断。这不是一条可以盲目统一成材料读数的旧兼容路径。

处理：明确“自然空气/自然方块背景、玩法环境、材料本体、辐射通量、显示温度”六种契约。本轮整理内部名称和 `block/air` 的过时注释，保留外部 API、packet 类和注册名、编码顺序、配方/NBT/config 键及资源路径。植物判断与材料读取不得相互替换。涉及脚本公开名称或反射入口的改名，先核对引用；无法确定只是内部符号的名称保留。

### R8：失效 JUnit 清理，不作为重构前置

检查时 `ThermalTestFixtures` 仍 import 并构造已删除的 `ConservativeAirGeometry`，编译也确认旧几何测试及依赖夹具无法使用；旧红外协议测试和状态解析测试还引用已移除的方法。最近 102/102 GameTest 成功不等于普通 JUnit 全通过。

用户决定：不迁移或恢复这些 JUnit，不补生产兼容类型，直接删除无法使用的测试与专用辅助代码。编译检查只用于确认清理边界和剩余引用，不作为要求用户恢复 JUnit 的理由。重构以现有可用 GameTest、真实客户端和必要的直接验证为依据。

本轮结果：已删除 20 份温度/红外失配测试及夹具，以及编译揭示的 11 份城镇旧构造器测试。`compileTestJava` 和 `compileGameTestJava` 通过；未恢复或新增 JUnit，未运行剩余 JUnit。

### R9：存在可以直接清理的误导性残留（低风险）

证据：`MinecraftThermalInput` 构造函数的 `initialTemperatureC` 已不参与初始化，只由 `start` 继续传入；节点初温来自 Page 环境。`WorldTemperature.ABSOLUTE_ZERO/OVERWORLD_BASELINE` 标注 legacy compatibility，当前仓库 Java 调用搜索仅见声明，实际行为读配置。`ThermalCellArena` 注释仍称 material poles，TopologyPlan 的 reservoir 命名实际对应可相变材料 slot。

处理：删除无效参数及内部传递；核对仓库内实际调用后删除不用的常量；修正材料、phase slot 和 mixed layout 术语。`player/unused/TemperatureThreadingPool` 及历史模拟器不在活动调度链，现有文档记载曾被明确要求保留；本次重构不将它们接回，也不把它们算作正在运行的兼容层。清理历史文件需先核对该保留约束的后续决定。

### R10：量化和预算口径容易被错误维护（次优先）

证据：`QueryPublication.quantizedInfrared`、`InfraredBrickCodec.quantize` 及 GLSL 解码共同约定四分之一度和 short 范围；CPU 比较与发送分别实现量化。`QueryPublication.projectedPayloadBytes` 按 74 B/slot 预留双缓冲数值及引用载荷；引用实际大小与 VM 有关。`MinecraftThermalInput.MEMORY` 的 128 MiB 只给显式 reservation 记账的发布和辐射存储，不是整个温度系统 retained heap 上限。

处理：写明量化单位、无效值、截断区间和预算覆盖范围。保留当前量化代码与各自非有限数处理；不为两处短公式增加工具类，也不恢复已经删除的 codec JUnit。若以后改编码，才在现有 GameTest 验证相应边界。本轮不扫描对象图或给所有容器追加计账，不删除发布双缓冲、slot 身份和 hot mask 双缓冲。

### R11：内容定义还承担 Minecraft 生命周期动作（次优先）

证据：`StateTransitionData.updateCache` 同时处理配方优先级与差异，并扫描已加载 Section 执行 `recalcBlockCounts`。`MinecraftMaterialLawCompiler` 已采用两遍数据编译，无需按材料建立策略子类。

处理：在现有 `updateCache` 内区分定义整理、差异计算、已加载 Section 刷新三个动作，必要时提取同类私有方法；保留现有回调注册、Runnable 创建与调度时机，不移动到新 reload 入口。保留两遍编译及水排除规则。人工输入是 `src/datagen/resources/data/frostedheart/data/state_transition.xlsx`，通过 `FHRecipeProvider.materialTransitions` 生成 JSON，源表不在本次改动范围。

### B1：植物提示缓存生命周期有实际缺口（单独行为修复）

证据：`TemperatureGoogleRenderer.renderOverlay` 只在 `lastHovered` 改变时发请求；持续盯住同一植物时不会更新温度。响应只携带一个 float，并写入全局 `cachedTemperature`，没有目标身份，切换目标到新响应到达前也会复用旧值。

影响：本体/环境 API 重命名后这个问题仍存在，不能靠重构自然消失。

这属于行为问题，已移出本次实施范围。本次保持请求频率、缓存和 packet 格式，只在此记录发现；需要用户另行决定修复。本次只确认源码路径，未启动客户端复现。

## Target Boundaries

本节说明结构整理的责任边界，不要求为每一项新建类。

下列是职责说明，不是必须创建的新类清单。优先在已有类里整理；仅对职责完整、能够直接搬移原实现和状态的部分提取文件。不会为了实现表格而增加运行时对象。

| 职责 | 目标拥有者 | 核心约束 |
|---|---|---|
| 维度生命周期、batch/completion 编排 | `MinecraftThermalInput` | 按实际顺序排列主流程和相关方法，保持启动/关闭/重启顺序 |
| 玩法环境与材料读取 | 总入口中明确分组的读取方法 | 空气 fallback 和材料 unavailable 分开；减少无必要的中间转发 |
| 活动↔休眠交接、检查点 | 原检查点方法与 scratch | 交接时机就地说明；保留发布读取及 Section 变更记录，不增加第二份实时 H |
| 红外服务端采集 | 现有 `InfraredCapture`，有条件移动文件 | 仍是全服务端主线程复用的一份 scratch，不改为每玩家/维度各一套 |
| Page 驻留和捕获调度 | `MinecraftPageManager` | 方法按驻留、采集、变更、检查点分组；保留必要回调及原状态归属 |
| Section 捕获与材料变更记录 | 原 Section owner，必要时移动文件 | 保持惰性数组、锁和清理语义，不增加实例或变成方块对象目录 |
| 材料法则与修改规则 | `MaterialThermalLaw` 与原两个变更入口 | 只共享完全等价的共同计算；各入口生命周期操作保留 |
| worker 拓扑与求解 | 现有 compiler/plan/committer/arena/solver | 拓扑仍唯一提交，保留热循环连续数组与编译系数 |
| 静态辐射 | 现有 index/service/occlusion | 先清楚命名和方法排列，不强制拆成每步一个类 |
| 红外客户端 | 现有 renderer/surface target/Embeddium 桥 | 网络接收/镜像上传和 GL 状态职责可独立整理；不改捕获技术路线 |
| 玩家与热网 | 原各自模型 | 接口标单位，结构重构不合并能量账本或改变平衡 |

不新建万能 `ThermalContext`、插件式材料处理器、通用事件总线、DI 框架或跨线程调用代理。若移出类后需要大量总入口 getter 才能工作，保留原位置并改进类内组织，不为完成拆分类目而实施。

## State And Lifecycle Contract

| 数据 | 谁写 | 谁读/何时交接 | 重构禁止的变化 |
|---|---|---|---|
| BlockState/Chunk | Minecraft 世界线程和原世界回调 | 捕获阶段转 primitive signatures | worker 读取可变世界对象 |
| mutable accumulator / pendingSubmission / inFlight | 主线程 | seal 后转 worker，completion 后 ACK | 把三个不同阶段合成一个布尔或随意清空 sealed 输入 |
| arena H/branch/request | 单维度 worker | 双缓冲发布，主线程只读 | 暴露数组供主线程直接写或复制为第二个活动模型 |
| PagePublication / QueryPublication | worker 安装/发布 | 几何 revision、slot generation、topology generation 共同匹配 | 仅检查一个版本，或将退让读当材料已删除 |
| Section checkpoint mutation log | 主线程记录并剪裁 | 把旧检查点投影到当前世界 | 因 worker 未 ACK 就清掉记录，或重复扣除材料能量 |
| dormant material snapshot | Chunk 拥有；Editor 写时复制 | 保存、查询、重新 admission | 在已交给 worker 的快照上就地修改 |
| AirRouteValidity / pending routes | worker 路由编译器 | 查询/执行检查有效性；提交后生效 | 假设布局里所有引用永远不变，或重建失败恢复失效路线 |
| analytic fields | 世界生命周期主线程索引 | 查询时合成 | 跟随物理 worker 重建丢失能量塔场 |
| IR scratch / 客户端镜像 / GPU 资源 | 各原线程拥有者 | packet 和已完成纹理上传 | 复制完整窗口给每节点/每帧，或删除借用的主 framebuffer 资源 |

## Cost Contract

- 活动 Air/材料仍使用 SoA 原始数组。不能以“可维护”为由改成每节点 `ThermalNode` 对象、装箱集合或虚方法调度。
- `QueryPublication` 两份材料/温度快照是跨线程读取和检查点需要，不是旧模型残留。名称/类移动保持数组数量及容量不变。
- `InfraredCapture.storedTemperatures` 为共享 `double[4096]`（32 KiB 数值载荷）；不能提取后变成每玩家每请求分配。729 页 presence 每份 `long[12]` 只是 96 B，真正大的部分是温度窗口和节点缓冲。
- 144³ 的 short 温度窗口单份约 5.70 MiB；当前客户端镜像/GPU 所有权保持。屏幕表面温度约 2P B，D24/D32F 深度约 4P B，P 为屏幕像素数。重构不增加第三份全屏/体积镜像。
- 主线程原预算、变更去重、64-Brick 每 tick 捕获、路由每 cut 4096 visits、source dirty 索引、冷 Page 退出策略保持。共享 scratch 不变成调用内 new。
- 命名常量放在拥有规则的现有类，区分 gameplay tuning、硬工作预算、容量初值和编码常量；不把所有数字收进一份全局“设置大全”。
- 优先原对象移动和类内方法提取，保持主要数组数量、容量与分配时机。必要的每维度具体协作者须有实际职责和明确维护收益，不复制状态；报告固定初始化成本，避免逐查询/逐节点分配增长。
- 验收保持现有工作规模，无额外全量遍历/世界读取/网络载荷；JIT 和方法边界影响尽量控制，频繁调用处有实际疑点才对照测量。只测 solver 的结果不能代表完整 tick/MSPT，不以不可测的微小差异阻止有价值的整理。

## Steps

按以下批次顺序实施；每批只包含一个明确整理目的，通过相应检查再进入下一批。已有工作区热行为修复不能被还原。第一批开始前记下当前差异与验证结果，作为本次重构起点，不要求提交、另建源码副本或计算路径敏感哈希。

| 批次 | 文件/方法重点 | 具体改动 | 完成判据 |
|---|---|---|---|
| 1：材料代码读得懂 | `BrickMigrationKernel`、`MaterialSectionState.Editor`、`MaterialThermalLaw`、`DormantThermalCooling` | 按命名表展开代码；区分节点能量/每块能量、活动迁移/休眠投影；补公式单位 | 读者能沿迁移判断找到 H 的来源和去向；表达式、分支及写入顺序保持 |
| 2：运行顺序看得见 | `ThermalDimensionEngine.process`、`TopologyPlan.prepare`、`TopologyCommitter` | 改 phase slot 名称；在调用处说明源结算、几何失效、提交、重绑、释放和发布；整理方法排列 | 主流程无需跨多层转发即可看清顺序；prepare 的副作用及 WORK_LIMITED 待办清楚 |
| 3：主线程职责分得清 | `MinecraftThermalInput`、`MinecraftPageManager.SectionOwner`、`MinecraftPhaseController` | 按职责排列方法，命名版本/坐标范围及两份变更记录；删除可直接省略的转发 | 确认材料状态在哪读、在哪记录世界改变、何时保存；原锁、回调时机、scratch 所有者保持 |
| 4：修正类型归属 | `MutableMaterialSample`、`InfraredReadCursor`；候选 `InfraredCapture` | 前两者按命名表处理；红外采集仅在满足下方提取条件时移动；更新直接 Java 调用 | 原对象与数组实例数不变；不暴露内部缓存；跨包访问只增加必要的完整操作 |
| 5：减少规则重复 | 活动/休眠变更入口与 `MaterialThermalLaw` | 完成下方语义对照，只共享完全等价的纯计算 | 不创建结果对象，不新增热路径分派；无法等价的部分保留并解释差异 |
| 6：外围名称与收尾 | `WorldTemperature`、`StateTransitionData`、实际涉及的源/辐射/查询代码 | 修正误导名称和旧注释，清理无效参数/import，补维护阅读路径 | 不扩展到整个玩家/热网/渲染系统重排；变更文件都能对应本计划中的一个问题 |

独立文件提取必须同时满足：已有完整职责；原实例和初始化/释放地点可保留；不增加 Context/工厂/接口；不需要一排 getter；调用链实际更短或实现更容易定位。`ReadCursor` 需要访问发布器内部缓冲，默认继续嵌套；`MaterialSample` 是跨存储/查询使用的值容器，适合直接移动。样本中的 requestSequence 通过完整的样本赋值操作传入，不能把所有字段公开以绕过原外部类的私有访问。`InfraredSnapshot` 本轮保留原位置和网络使用方式，避免扩大协议相关改名。

### 材料变更共享前的对照

| 原因/输入 | 必须保留的差异或结果 |
|---|---|
| 普通属性改变，cause=0 | 原 H/branch 保留；活动入口还要匹配原材料身份，休眠入口保留其原时间投影 |
| REPLACE | 新材料按原入口自然温度初始化；休眠替换不先对旧体执行自然冷却 |
| MASS_CHANGE | 调用已有 `afterMassChange`，保留参数/算术次序和 branch 重置 |
| GAMEPLAY_TRANSITION | 由旧 law 的当前温度映射新 law 的 H；不改成热转换守恒语义 |
| THERMAL_TRANSITION | 保留 H，按原流程重置 branch；ACK 和几何交接仍由原层处理 |
| 旧 law 缺失/新 law 缺失 | 活动迁移仍处理初始化/删除和能量计账；休眠 Editor 仍只更新已有记录，不能自动新建记录 |

### 表达整理不能顺手改变的执行细节

- 改名时一并更新 Java 调用、必要文档锚点和仍使用该符号的 GameTest；对 Minecraft 注入字符串、反射、VarHandle 字段名、序列化和脚本入口单独核对，不能全仓库文本替换。
- 提取布尔局部变量不得提前执行原短路条件中的方法，不重复调用有状态 getter；不得交换 `&&/||` 条件或浮点求和顺序。
- 不将 `1 / C` 替换为另一种预计算，不将 `expm1` 换成 `exp - 1`，不改变位移、排序/遍历顺序、正反扫、原双缓冲交换位置。
- 方法排列可以调整；字段初始化、静态初始化和构造函数赋值次序保持。同步方法或锁内片段不移到锁外，也不以名称整理改变 `ThreadLocal/volatile`。
- 删除无效参数前检查实参表达式是否有副作用；不因删除形参顺手跳过原来实际执行的世界查询或状态更新。
- 单行返回和简单 getter 无需机械展开；复杂主流程展开后不再逐行配“翻译代码”的注释。
- 新发现的行为问题记入计划未实施部分，继续完成独立的名称整理，不将问题修复带入本轮。不要只为了保持整洁而删除仍承担原行为的分支。

## Validation

本轮架构调查为静态检查，随后按用户要求编译定位并删除失效 JUnit，没有重新运行游戏测试。上次实现的验证证据为 102/102 GameTest；4 项目标浮力 JUnit 是此前独立结果，不等于全部 JUnit。接下来的重构使用以下分层验收，数值检查优先复用现有 GameTest，不恢复旧 JUnit：

| 范围 | 必须验证的行为 |
|---|---|
| 纯计算/索引 | 当前整块布局、负坐标、显热公式、潜热反向推进和能量守恒；单步系数与实际 dt |
| 材料变更统一 | REPLACE、同块属性、质量变化、玩法转换、热转换、目标无材料；活动/休眠共有规则对照 |
| 生命周期 | 源启停/移动/零功率、无源残热、跨 Page、两层截止、WORK_LIMITED 待办、几何先失效、重建时材料读取和 checkpoint |
| 时序 | 20/40/100/200 tick，相同总输入 J；源先结算后迁移；latent 完成等待 ACK；失败重启、保存、卸载和重载 |
| 数据/玩法 | 水地表采样、配置快照、解析场独立生命周期；若改生成器，使用实际注册表验证 workbook→74 份配方，保留开关/0°C/空白语义 |
| 红外 | 全量/增量、几何改变不闪烁、活动/休眠切换、字段合成；若动 GL/顶点桥需客户端验证边缘、实体遮挡、资源重建/关闭和高精度格式 |
| 性能 | 固定相同场景的稳态与拓扑 churn；记录完成 cut 数、节点/边数、分配和完整 cut 耗时。保持节点/数组/网络规模，重复测量仅在变化或疑点存在时进行 |

验证按改动类型决定，避免每改一个名字都启动完整服务器：

| 改动类型 | 足够的验证 | 不应增加的工作 |
|---|---|---|
| 只改局部名、import、排版、注释 | 差异检查，确认表达式不变；同批完成后编译 | 逐名字重跑完整 GameTest、反复采集性能 |
| 跨文件/类型改名 | 搜索直接引用与必要字符串入口；`compileJava compileGameTestJava` | 恢复旧别名、旧 JUnit 或给测试加排除配置 |
| 方法提取/原对象移动/共同计算提取 | 上述检查与受影响 GameTest；核对 new/数组/锁/访问次数 | 全局增设计数器、节点对象化、改规则后放宽断言 |
| 确实改变热路径调用边界 | 对比同输入、同完成 cut/节点/边数量的耗时与分配 | 仅因源码短了宣称更快，或用不同工作量比较 |

涉及结构或计算提取的批次完成后，最终运行一次现有完整 GameTest；只有后续实际修改、失败或未解决疑点才重复。纯移动和提取要求相同输入的数值与请求顺序保持，不通过放大容差接受差异。已有 GameTest 缺少某个实际变更边界时，仅补必要的该边界验证，不新建测试框架。没有恢复旧 JUnit 或新增 JUnit 体系的任务。

需要性能对照时，在相关改动前于当前工作区记录基线；使用相同 JDK/JVM 参数、场景和固定工作量，预热后读取耗时与分配。初次差异有疑点时复测，排除噪声后若存在明显退化，调整实现或撤回该项提取。细微 JIT 差异尽量控制，但可读性和可维护性仍为主要目标，不引入没有场景依据的百分比审批门槛。原 16 源/2176 节点微基准只覆盖 solver；触及主线程捕获或检查点时应测对应路径。无需为名称整理新增生产监控或 benchmark 框架。

客户端图形效果、在线多人完整 heap/MSPT 不由 GameTest 覆盖，只有实际完成相应验证才能报告通过。本计划不为未改动的外围系统追加全面重测。

## Documentation Impact And Outcome

- 本次检查及失效测试清理完成，生产代码、配方、Mixin、配置和求解行为未修改。后续用户决定覆盖早先调查 diary 中“恢复 JUnit”的建议。
- 实施时仅更新实际移动/改名涉及的 `docs/climate` 锚点和维护入口；配方刷新仍是原入口。文档按游戏系统组织，不按新增 Java 类逐份创建文档。
- 失效 JUnit 按用户决定删除，不作为前置阻碍；主要维护障碍是总入口混杂、回调环、材料变更重复和发布器内部类型泄漏。B1 是独立的显示刷新行为缺口。
- 不把静态辐射、显式解析场、空气与材料的不同温度、持久化快照、双缓冲、原水采样视作“必须消除的架构不统一”。
- 成功标准：名字和主流程直接可读、职责有明确落点、必要的共同计算可维护；保持热行为和主要运行成本，完成必要编译/GameTest。实际减少的回调/重复逻辑和保留原因逐项报告，不以文件变多或行数变化作为成果。
- 收尾只检查本轮修改及引用，不再扩展到未动过的材料特例或其他系统。保留必要 GameTest，清理本轮临时文件，不改人工源表、用户存档和既有待提交改动。当前已统一为六批重构计划，生产实施尚未开始。
