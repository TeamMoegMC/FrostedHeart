# 温度代码可读性与可维护性重构实施计划

- Time: `2026-09-16 00:15:34 +08:00`
- Updated: `2026-09-16 16:00:58 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed; 八批整理及验证已完成，保留材料导热修复基线`
- Scope: `温度代码命名、表达、必要的职责整理；保持热行为与主要性能成本`
- Related: [当前架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[温度语义](../docs/climate/world-climate-and-temperature.md)、[导热修复基线](../diary/2026-09-16_01-03-57_material-conductance-defaults.md)、[调查记录](../diary/2026-09-16_13-29-11_thermal-refactor-plan-review.md)

## 1. 目标与范围

让维护者从名称和主流程理解数据来源、处理顺序与修改位置。优先可读性和可维护性，允许必要的方法提取、内部类型改名和职责归位；JIT 内联等细微影响尽量控制，不追求字节或测量结果绝对相同。

本轮覆盖整条温度链路：数据编译、捕获和调度、源账本、节点与连接、求解、查询、休眠保存、辐射/解析场、红外与外部消费者。整理名称、复杂表达、职责和维护入口；已经清楚的实现保留并说明，无须机械修改所有文件。确定的类型归位仍只有材料样本与红外采集，不把全面覆盖等同于大量拆类。行为缺陷、平衡调整、渲染技术替换不在本轮范围。

不增加每节点/每查询对象、额外全量扫描、重复大数组、通用 Context、服务定位器或无实际需要的接口层。不恢复已删除的 JUnit。源码和现有数据为准，调查历史不再重复写入实施要求。

## 2. 当前基线与必须保留的行为

审核基线为 `7e242c2ff` 的源码；当前未提交的计划与 diary 是本次准备工作。实施前确认是否有新的代码改动，保留他人和前序任务的修改，不另建源码副本或计算路径敏感哈希。

最新验证是材料导热修复后的 **102/102 GameTest**，不是本计划实施后的验收结果。

| 范围 | 保留内容 |
|---|---|
| 材料参数 | 分类默认导热：泥土 1.0、石头 1.4 W/K；配方显式值优先；有无相变不再覆盖导热；不恢复旧配置 |
| 能量 | Air 和材料继续存 H；材料 law 共享 C/O/相变边；显热 H=O+C*T；暴露面影响连接，不改变整块热容 |
| 空间 | Page=16³、Brick=4³；普通 Brick 最多 64 节点；楼梯没有单独气隙温度 |
| 传播 | 材料从受热表面最多进入两层；水平面不施加浮力；近源四格直接 Air 面 4×G，重叠取并集 |
| 驻留 | 相对自然基线 1°C 扩张、0.5°C 保留/释放；原采集和路由预算、Page 退出规则不变 |
| 时间 | 20 tick 普通 cut；延迟 cut 用实际 dt，一次遍历；源先结算后迁移，正反扫次序不变 |
| 相变 | 潜热完成等待世界 ACK；活动、休眠、无记录环境转换各自入口保持；水仍按原地表列采样 |
| 查询/存档 | 查询不额外加载区块；休眠惰性冷却；缺少材料状态仍表示不可用，不替换成空气温度 |
| 玩法 | 解析场仍具有独立世界生命周期；静态火/岩浆辐射保持；玩家体温与热网单位不混用 |
| 红外 | 原顶点归属、深度快照、差量同步、实体环境近似、冷蓝缺失值、−20～20°C 色带和 0.43 混合不变 |

## 3. 确定的命名与职责

### 名称

| 当前名称/表达 | 最终决定 |
|---|---|
| Page | 文档首次出现写“ThermalPage：对应一个 Section 的热模拟容器”；代码保留已有 `ThermalPageHandle/PagePublication/WorkerPageStore`，不额外创建 ThermalPage 包装类 |
| Brick、Topology | 保留；分别说明为“4³ 方块组”和“节点及可换热连接”；不将热 Page 机械改成 Minecraft Section |
| `TopologyPlan` | 改为 `TopologyUpdatePlanner`，同目录移动文件、构造器和直接引用同步改名；仍负责准备，不负责安装 |
| `PreparedTopologyChange/TopologyCommitter` | 保留，分别是已准备变更和提交器 |
| `removedReservoirs/addedReservoirs` 及对应 slot 字段 | 按各字段实际含义改为 `removedPhaseSlots/addedPhaseSlots` |
| `QueryPublication.MutableMaterialSample` | 移至 `thermal.mesh.MaterialSample`，保留“调用者复用的可变读取结果”语义 |
| `QueryPublication.InfraredReadCursor/beginInfraredRead` | 改为嵌套 `ReadCursor/beginRead`，不搬出发布器 |
| 迁移中的 `old/next/n/first/os/ns/pb` | `oldBrick/newBrick/nodeCount/firstSlot/oldSlot/newSlot/pageBlockIndex` |
| 迁移中的 `previous/initial` | 已按覆盖方块数分摊，使用 `previousEnergyPerBlockJ/initialEnergyPerBlockJ` |
| scratch `enthalpy/temperatures` | `nodeEnthalpiesJ/blockTemperaturesC`，区分索引与数值单位 |
| `WorldTemperature.block/air` | 保留外部 API，修正注释：玩法环境温度，不是材料本体；本体使用 `material` |
| 实际 Air 布局的 `transportNodeCount/transportSlot/setTransportCapacity` | 改为 `airNodeCount/airSlotAt/setAirCapacity`；仅对确认代表真实 Air 状态的符号替换，不改空气路径的独立概念 |
| `addMaterialPole/writeMaterialPole` | 改为 `addMaterialCell/writeMaterialCell`；本体是一份材料 H，不是另一种表面储能模型 |
| `ThermalCellArena.isSurfaceCell`、布局 `surfaceNodeMask` | 当前实际判断/标记所有材料节点，改为 `isMaterialCell/materialNodeMask`；真正的几何暴露/接触仍由原接触判断表示，不能按名字新增过滤 |
| `MaterialThermalLaw.Transition.temperatureC` | 改为 `transitionTemperatureC`，更新 Java 调用；实际节点 `temperatureC(H, branch)` 保留，数据键 `temperature_c` 不改 |
| AirRouteCompiler.Component 的 `distance/labels/stage` | `pathResistance/airRegionIds/stage`；四个命名 int 常量表示发现位置、初始化端口、传播阻力、生成连接，保留原阶段顺序 |

小循环的 i、坐标 x/y/z 保留。revision 表示几何变更版本，generation 表示身份代次，sequence 表示批次/请求序号，不全部改叫 version。pageSlot 与 arena 的 nodeSlot 分开命名。

### 文件与责任边界

| 文件/类型 | 实施后的责任 |
|---|---|
| `MinecraftThermalInput` | 维度启动/关闭/重启、输入提交、完成回执、世界事件入口；保留环境/材料查询和检查点交接，按职责分组，不再包含红外采集算法 |
| 新文件 `runtime/minecraft/InfraredCapture.java` | 搬移现有嵌套采集器及采集专用常量/helpers，package-private final；不新建每玩家或每维度实例 |
| 新文件 `mesh/MaterialSample.java` | 搬移现有样本字段和操作；无世界访问、无发布缓冲、无自主缓存 |
| `QueryPublication` | 双缓冲发布与一致读取；ReadCursor 继续嵌套；不改变发布和版本读取顺序 |
| `MinecraftPageManager/SectionOwner` | 驻留、采集、变更记录和检查点协调；SectionOwner 本轮保留嵌套，方法和记录格式写清楚 |
| `MinecraftPhaseController` | 请求校验、世界转换及 ACK；tick 直接接收当前 QueryPublication，不缓存发布器，不经 PageManager 回调 Input |
| `TopologyUpdatePlanner/TopologyCommitter/Engine` | 准备、安装、驱动，边界不变；按真实顺序排列主流程 |
| `MaterialThermalLaw` 与两个变更入口 | 已有共同公式继续共享；活动迁移与休眠编辑的生命周期操作分别保留 |

请求校验转发已删除：Input 调用 `phase.tick(queryPublication)`，控制器直接校验已有 Page 与材料样本。删除 PageManager 的纯转发和 Input 的公共校验入口；没有新增接口、持久依赖字段、对象或重启通知。此前“删除转发需要新增依赖替换机制”的理由不成立。

检查点仍跨 Input（等待 worker/读取发布/写 Chunk）、PageManager（枚举与退出）和 SectionOwner（变更记录投影），这是**已知职责重叠**。本轮没有消除该重叠；“紧密关联”仅说明拆分需要核对交接顺序，不能证明现有分工合理。后续如处理，应先明确唯一的保存协调入口、删除重复编排，再决定归属，而非自动再加一个协调器。本次补充不宣称已解决存档职责。

SectionOwner 保留为 static 嵌套类有实际归属依据：仅由 PageManager 创建/挂接/失效；通过管理器的私有 dirtyOwners 排队，takeDirty 与管理器 scratch 交换数组，Page 引用也由管理器发布。当前没有独立生命周期或其他所有者，移出只会扩大内部 API，不能删除状态或依赖。它承担的检查点投影部分仍属于上述职责重叠，嵌套位置合理不代表其所有方法职责都已合理。

### 删除优先的审查方法

每个拟提取的类/接口/转发先检查：删除后哪项独立责任会丢失；调用处能否直接提供当前值；是否只是替另一层透传参数。优先删除无效入口、重复编排与不必要的依赖，再考虑移动或提取。真实的同步、状态与能量校验不因“删抽象”被取消。

| 对象 | 结论 | 证据或剩余问题 |
|---|---|---|
| Phase → PageManager → Input 校验转发 | 已删除 | 调用时传入 publication 即可；复用原样本，无替换机制 |
| Input/管理器/SectionOwner 的检查点分工 | 已知职责重叠，未处理 | 等待、枚举、采样、投影、落盘跨三处，不能以关联紧密当作合理性证明 |
| SectionOwner 的嵌套位置 | 有依据保留 | 管理器唯一拥有生命周期、队列、句柄和数组交换；检查点投影债务另记 |
| 已移除旧名称、无效初温参数/常量 | 保持删除 | 无调用或无运行作用；不补旧别名/兼容包装 |
| WorkerPageStore.resolveAirFaceSlot | 已删除 | 无调用者；唯一实际入口 resolveAirFaceTarget 同时提供 slot/generation，取消仅供该包装使用的 null 输出模式 |
| MaterialBoundaryRegistry.Profile.body | 已删除 | 仅包装公开构造器；调用方直接 new Profile，构造校验保持 |
| Input 的解析场 upsert/remove 转发 | 已删除 | 命令、Curiosity 和测试直接调用已有 MinecraftGameplayFields；没有新增统一入口类 |
| 每 Engine 的 TopologyCommitter 实例 | 已删除 | 无实例状态；原提交/恢复/释放方法改 static，删除 Engine 字段，顺序和校验保持 |

以上是本轮确定边界。除重命名文件外，只计划上述两个类型归位，不以文件行数或“零依赖环”为目标。其他固定生命周期协作者只有遇到明确维护障碍才考虑，并说明成本；不自动追加类清单。

## 4. 代码应该整理成什么样

下面是当前源码的局部示例；实施时保持表达式和调用次序。

### 示例一：迁移索引

当前：
```java
int n=next.span.count(), first=next.span.firstSlot();
for(int i=0;i<n;i++) enthalpy[i]=arena.enthalpyJ(first+i);
```

目标：
```java
int nodeCount = newBrick.span.count();
int firstSlot = newBrick.span.firstSlot();

for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
    nodeEnthalpiesJ[nodeIndex] = arena.enthalpyJ(firstSlot + nodeIndex);
}
```

不增加临时集合或每节点对象。Page 方块索引为 x | z << 4 | y << 8，Brick 方块索引为 x | z << 2 | y << 4；只在转换位置说明范围，不逐行复述位运算。

### 示例二：复杂条件

当前：
```java
if (node < 0 || !queryPublication.tryReadMaterial(brick.firstSlot() + node, brick.arenaGeneration(),
        publication.topologyGeneration(), sample) || sample.requestSequence() != request.requestSequence()
        || sample.branch() != request.materialBranch()) return false;
```

目标：
```java
if (nodeIndex < 0) {
    return false;
}
if (!queryPublication.tryReadMaterial(
        brick.firstSlot() + nodeIndex,
        brick.arenaGeneration(),
        publication.topologyGeneration(),
        sample)) {
    return false;
}
if (sample.requestSequence() != request.requestSequence()
        || sample.branch() != request.materialBranch()) {
    return false;
}
```

样本获取仍留在原位置，不提前求值原短路分支。这里不需要再提取三层谓词方法。

### 示例三：主流程与必要注释

```java
// 先向旧连接结算截至本次更新时间的供能，避免迁移覆盖刚输入的能量。
sources.acceptAndAdvance(
        batch.sourceEvents(), batch.targetTick(), sourceBindings);
pages.awaitChangedMaterials(batch, arena);
```

保留后续准备、提交等完整动作，注释解释为何必须这样排序。不要改写成一串含义模糊的 processStage1/processStage2，也不为每行增加“执行某某”的注释。

## 5. 全链路覆盖与八批实施顺序

每组检查参数单位、字段含义、缺失值、修改落点与回调顺序。最终逐组标明“已整理”或“原实现清楚，保留的理由”，不能只改大文件前几十行就宣称完成。

| 覆盖组 | 主要文件 | 必须交付的整理 |
|---|---|---|
| 材料与数据编译 | MaterialThermalLaw、MaterialBoundaryRegistry、MinecraftMaterialLawCompiler、MinecraftThermalProfiles、StateStaticThermalResolver | G/C/O/T 与相变阈值分清；默认/配方覆盖及签名分类的修改位置明确 |
| 捕获与主线程 | MinecraftThermalInput、MinecraftPageManager、MinecraftEnvironmentCapture、DimensionInputAccumulator、消息类 | 采集、封批、排队、ACK、保存分清；集合能否再写、何时清空明确 |
| 异步调度 | ThermalDimensionMailbox、ThermalWorkerPool、ThermalDimensionEngine | 状态转换与线程归属就地说明；队列、等待和关闭行为不变 |
| 源与账本 | PhysicalSourceSpatialIndex、WorkerPhysicalSourceBindings、ThermalSourceLedger、NodePowerAccumulatorArena、源消息/绑定 | sourceId/sourceSlot/nodeSlot/portOffset 和 tick/W/J 分清；供能、重绑、损失及未接收能量各自可追踪 |
| 几何与连接 | BrickTopologyCompiler、TopologyView、AirRouteCompiler、MaterialEdgeCompiler、TopologyUpdatePlanner、TopologyCommitter | 连通编译、路径阻力、接触资格、边归属、迁移/提交阶段可读 |
| 节点与分配 | ThermalCellArena、ThermalBrickCellLayout、BlockBrickLayout、PageSignatures、ThermalFreeSpanIndex、ThermalPhaseRequestStore | 数组索引、slot 代次、FREE/RESERVED/LIVE、phase 状态和释放条件明确；现有分配算法保留 |
| 换热与相变 | ThermalSolver、ThermalFragment、ThermalMaterialExecution、ThermalExchangeKernel、MaterialEnthalpyExchange、PhaseTransitionRuntime | 每类边的温度来源、G/C/系数单位、快慢路径及潜热停止边界明确 |
| 查询与驻留 | QueryPublication、PagePublication、ThermalPageHandle、WorkerPageStore | 几何与数值发布、hot/desired/resident/sourceSeed 位图、缺失/过期/有效状态分清 |
| 休眠与存档 | DormantChunkThermalState、MaterialSectionState、DormantThermalCooling、Chunk attachment | 空气残差与材料 H、索引/编码、时刻/冷却/部分覆盖交接分别清楚 |
| 辐射与解析场 | RadiationService、BlockRadiationIndex、MinecraftRadiationOcclusion、ThermalAnalyticFieldIndex、MinecraftGameplayFields | 发射、候选、遮挡缓存、温度合成各自单位和失效规则明确 |
| 红外两端 | InfraredCapture、InfraredBrickCodec、请求/响应 packet、InfraredViewRenderer、surface target/后端桥 | 采集、分包、接收、镜像上传、渲染五段入口可追踪；原资源和绘制路径保持 |
| 消费者边界 | WorldTemperature、PlayerThermalEnvironment、热物品查询、TownThermalProjection、测温/植物提示、机器源出口 | 自然/环境/材料温度及辐射通量分清；玩家、城镇及热网内部算法不扩展重构 |

每批完成相关检查再继续，不为每个名字重启完整测试服务器。

| 批次 | 工作 | 完成条件 |
|---|---|---|
| 1 | 材料迁移、law、DormantChunkThermalState 与 MaterialSectionState 的命名、存储布局、编码和状态处理整理 | H 与空气残差分清；单位、索引、哨兵、位布局可直接读懂；编码结果与计算时机不变 |
| 2 | Brick 编译、空气路由、材料边、节点分配整理；TopologyUpdatePlanner 改名；Engine/提交器交接表落实 | 连通/阻力/接触/分配分清；旧类型引用清完；交接顺序清楚，预算及算法保持 |
| 3 | MaterialSample 归位、ReadCursor 改名 | 所有调用同批更新；样本来源/请求序号、一致读取及原实例数量保持 |
| 4 | InfraredCapture 整体移出 | 原静态实例复用、重试和 finally 保持；Input 无采集算法，packet 和 Snapshot 不迁移 |
| 5 | 源发现/绑定/账本与 accumulator/mailbox 交接整理 | 能从机器报告追到节点供能与能量去向；批次数据不被提前重用 |
| 6 | Input、PageManager、PhaseController、solver/query 发布与驻留整理；共同计算审查 | 重启/保存/潜热/驻留边界清楚；无新增协议、重复扫描或空壳转发 |
| 7 | 辐射、解析场、红外客户端、profile 编译和消费者边界整理 | 缓存失效、温度单位与数据流清楚；玩法、GPU 资源及更新频率保持 |
| 8 | 各组旧注释/无效参数清理；按覆盖表复核；更新文档 | 每项删除有引用依据；未改组说明理由；完成最终验证 |

`StateTransitionData.updateCache` 只在原方法/类内整理定义覆盖、差异计算和 Section 刷新，保留原 Runnable 与调度。无效参数删除前检查实参是否有副作用；不顺手改变世界查询。

## 6. 容易误改的位置

### 四个重点问题的具体完成标准

以下四项均属于本轮正式实施范围，当前只是补齐计划，生产代码尚未修改。

**A. BrickTopologyCompiler 的紧凑代码。** `compileCells` 按当前算法展示六个连续阶段：读取 64 个签名并检查可用性；全 Air 快速路径或真实 Air 连通分组；追加独立 Air 节点；追加材料并统计可相变节点；生成布局并分配 staged slot；安装材料 law、收集 phaseSlots、返回结果。这里收集相变 slot，不是把多个材料本体合并为一个相变储能器。

- `ids/mapping/masks/mergeable/nodes/transport/bodyPhaseCount` 分别表达为签名表、方块到节点映射、节点覆盖位图、可合并 Air 位图、节点数、Air 节点数、可相变材料数。邻接位图构造和洪泛循环展开排版，说明 x/z/y 位顺序。
- 分组循环与材料安装可以提取完整动作的私有方法，但不得为传回几个计数新建结果 record、数组或 Context；不增加扫描次数。计数相互依赖的短阶段留在主方法，比藏进编译器可变字段更清楚。
- 原全 Air 单节点、无布局快速路径，未解析签名返回 EMPTY，材料顺序、scratch 复用、原 layout clone、stage 失败和异常释放均保持。完成后不应再出现一行多个循环操作，或必须同时推算三种索引才能理解的变量。

**B. 状态名称与温度接口。** 统一的是含义与接口表达，不改变底层热状态：Air cell、material cell、phase request、几何暴露分别表示不同事物。

- 当前 `isSurfaceCell` 只检查 MATERIAL_CELL，`surfaceNodeMask` 在编译时给全部材料节点置位，并非几何表面可见性；按命名表修正，不能借改名改变哪些节点发布或显示。
- 实际材料温度仍由 H/law/branch 求得，潜热平台时等于转换温度是正确的材料状态，并非“返回了假的温度”。Transition 的固定阈值使用独立名称，不能把它接到实际温度读取位置。
- `phaseReservoir` 本次生产源码搜索未发现仍在运行的同名入口；不按历史名重建类型。逐个检查现存 pole/reservoir/surface/transport 用法，仅修改确实误导的名称。验收检查调用者拿到的是实际温度、转换阈值还是环境温度，不能只补一句注释。

**C. 跨类执行顺序。** 在 Engine 主流程及相关方法边界落实下表，帮助维护者追踪读写，而非新增阶段对象：

| 步骤 | 主要读取 | 主要修改 | 完成后可依赖的状态 |
|---|---|---|---|
| 源结算 | 当前绑定、已安装节点、事件时刻 | 源账本及旧节点 H | 能量结算到 targetTick，之后才能按该 H 迁移 |
| 失效与准备 | 新输入、旧几何/H | 失效标记、staging、路由待办 | 得到准备载荷或保留预算不足待办；不代表新状态已全部发布 |
| 提交 | PreparedTopologyChange | arena/solver/Page/phase 注册 | 新结构已安装，尚不能释放仍被源引用的旧 slot |
| 重绑与释放 | 新结构、源/solver 引用 | 绑定、旧 span | 引用已解除的旧 span 才归还；后续换热使用已安装连接 |
| 换热与请求 | 新连接、dt、H/law | H/branch、待确认请求 | 本次能量推进完成；实际方块转换仍需主线程 ACK |
| 查询发布与驻留 | 本次结果、hot mask | QueryPublication、驻留 completion | 读者仍须通过原版本验证，不能单凭新 Page 引用认定查询已一致 |

保留原 try/catch/finally 和失败处理，尤其不能把 prepare 当纯计算、把 Page 发布引用恢复当成完整事务回滚。

**D. DormantChunkThermalState 的压缩存储。** 用户指出的行段主要处理休眠 Air 检查点，材料 H 在 MaterialSectionState，二者都整理但不混成一个编码模型。

- 在 SectionEntry/fromScratch/pack/residualAt/putResidual 附近说明布局：Brick 按 brickMask 置位顺序排列；每 Brick 先均值，再放 exactCount 个节点残差；blockMasks 与这些节点逐一对应；valueOffsets 指向解包后的 short 值索引，maskOffsets 指向覆盖位图索引。
- 空气 residual 的单位为 1/16°C，每个 long 装四个 16 位有符号值；写入掩码、读取右移后转 short、现有取整/截断保持。Short.MIN_VALUE 在这里是合法负残差，不能误用红外的无效温度哨兵。
- 640 B 是现有 packed residuals 与节点 masks 的预算判定，不是整个对象堆大小。保持 exact 明细超预算退回均值、PRUNE_RESIDUAL=4 及原严格大于判断，不重调精度或裁剪条件。
- 展开 fromScratch、SectionEntry 构造/encode/decode、mergeAir，命名 residualIndex/nodeMaskIndex/brickRank 等局部变量；编码、索引建立和冷却/合并各自表达完整动作，保留 primitive 数组、字段键、格式版本及错误数据处理。
- 说明各处 null/空位图/无记录的真实含义，保留“保存未变化检查点不重新计时”与部分 Brick 合并规则。材料容器则标明 H 的 J 单位、palette 索引及逐记录 tick，不套用空气量化。
- 验收不仅是能编译：用现有保存/恢复与休眠 GameTest 核对 NBT、负残差、预算临界/回退、部分覆盖和反复保存的结果。实际缺少覆盖的改动边界才补 GameTest，不恢复 JUnit。

### 材料样本和发布读取

- MaterialSample 保持原字段和 Source 枚举，不改成每次返回新 record。set 重置 stored/requestSequence，setStored 标记存档来源，clear 保持原缺失状态；source 仍先看 law 是否为空。
- 外部类原来直接写 requestSequence。搬移后用完整 live 样本赋值操作传入 H/law/branch/tick/requestSequence，不公开字段。只有 QueryPublication 完成版本/generation 验证后才能写成功结果。
- ReadCursor 保持“读版本→取缓冲引用→再读版本”、原两次尝试与 isCurrent 检查。不增加读锁，不复制快照，不把相邻步骤各读到的不同版本拼在一起。

### 红外采集和 worker 重启

- InfraredCapture 由 Input 继续惰性创建，closeAll 关闭并置空。采集调用传入当前 PageManager、QueryPublication 和 dimensionGeneration，不新增 Context 或缓存旧发布器。
- restartWorker 会更换 accumulator/mailbox/queryPublication；长期对象不能持有过期引用。沿用已有 replaceAccumulator/createWorker/reseedAll 顺序，不新增通知链。
- 没有活动 runtime 时仍可读休眠材料和解析场，不为显示启动物理模拟。
- 保留 payload reset、两次重试、完整成功才返回。发布竞争返回 null 保留客户端基线，不能发送“空窗口”替代；差量 generation/epoch/presence/storedSampleTick 判定保持。
- finally 清游标、handle/Chunk 引用、列表、level 和 payload。原来输出时 clone 的位图仍在原位置 clone，工作数组不能交给网络长期持有。
- 采集专用常量随实现移动；共享的发布年龄限制保持单一命名来源。InfraredSnapshot、packet 注册名/格式、GL 与 Mixin 均不改。
- closeActiveLevel 不等于 closeAll；一个玩家/维度退出不能释放全局采集器。普通查询/tick 不增加等待，保存/卸载保留原 awaitLatestCompletion。

### 更新与变更记录

固定次序：ACK/意图/风 → 源结算 → 材料失效 → 准备/提交 → 源重绑 → 释放旧 slot → 换热 → phase 请求 → 查询发布/驻留。

- 提取方法不能扩大或缩小 try/catch/finally 覆盖范围。WORK_LIMITED 保留待办；committed/mixingCommitted 只在原成功分支执行，不能放到 finally。
- restorePagePublications 仅恢复发布引用，不是整个 worker 回滚；prepare 会失效路线并保留待办，不是纯函数。
- SectionOwner pending 材料四元组用于迁移；checkpoint 三元组另配 revision/tick 用于保存投影。命名 stride/字段偏移，数组追加、交换和剪裁成组保持，不改成每条记录一个对象。
- 同位置多次修改按原序回放，不合并成最终状态、不排序、不合并两份记录。保留 PhaseController.APPLYING 与 superseded 对嵌套回调的处理。

### 材料计算的共享范围

| 情形 | 原语义 |
|---|---|
| 普通属性 cause=0 | H/branch 保留；休眠保留其冷却投影 |
| REPLACE | 新 law 按原自然温度初始化；休眠不先冷却旧体 |
| MASS_CHANGE | 已有 afterMassChange 共享公式，branch 按原流程重置 |
| GAMEPLAY_TRANSITION | 旧 law 的温度映射到新 law 的 H |
| THERMAL_TRANSITION | 保留 H，原流程重置 branch，ACK 单独处理 |
| 旧/新 law 或记录缺失 | 活动处理初始化/删除和外部能量计账；休眠仅编辑已有记录 |

只共享完全等价的纯计算，优先使用已有 MaterialThermalLaw 返回 primitive 的方法。不要把 runtime 的 MaterialChanges 原因码引入 law，也不增加统一策略/结果类。短公式重复允许保留；不能为了去重合并活动与休眠流程。

## 7. 性能与可读性验收

### 补充模块的工程边界

- **源账本：** 分清观测变化、有效时刻、积分、端口分配、重绑和释放。保留最后端口分配剩余功率以消除舍入差的算法。declaredLoss、degradedLoss、unaccepted 分开，不因都没进入节点而合并；EnergyBalance 不改成每源每 tick 生成的对象。
- **源索引：** 保留主线程 Section 索引和 worker 绑定索引的不同职责，不跨线程共用可变状态。稳定源仍不生成 dirty 消息；正功率调整与启停导致的混合区域改变分别处理，不新增每 tick 全源扫描。
- **空气路由：** Component.advance 的四阶段使用命名 int 常量和完整动作方法，明确预算扣减。pathResistance 是归一化累计阻力，不是距离或温度；原最小堆、同阻力按 region/position 的决胜顺序、可恢复 cursor 和 validity 保持，不换图框架或状态对象。
- **封批/mailbox：** seal 后数组属于 batch，builder reset/recycle 不得修改已交出的内容。pendingSubmission、inFlight、mutable accumulator 分别命名；AWAITING_ACK 不表示可接下一批。整理状态机不新增 Future/任务/锁，不将 save 等待移入普通 tick。
- **求解/发布：** 原 Air、材料、FarField 执行方法保留，不引入每边虚调用。QueryPublication 的一次 live-slot 遍历继续同时写温度/材料快照、hot mask 和红外变化位，不拆成几次全量扫描。hot mask 的 begin 交换及发布间读取最新位图保持。
- **分配/材料边：** 空 slot/span、staged/live 和代次命名清楚；保留 free-span 索引、释放/合并次序、边去重排序与引用计数。边的 contributor 和合计 conductance 分清，不能把多份面贡献误当重复边删除。
- **辐射：** 候选收集、排序/截断、射线、缓存复用按动作分组；静态索引按需覆盖、脏块重建、液面发射分别表达。保留受体/物品预算及缓存有效性，不改为材料动态辐射，不增加逐受体临时集合。
- **解析场：** 区分 natural/base/composed/floor；保留 combineMode、priority、key 的排序和 unchanged sphere 快速路径。不用无序 hash 遍历替换有序列表，不将合成提前到 worker。
- **客户端：** InfraredViewRenderer 按请求/响应、镜像/上传、帧捕获/绘制、关闭分组。保持 requestId/中心/generation/epoch 校验、脏页上传及 GL 状态恢复，不新增管理器、纹理副本或渲染 pass。实际改到 GL 控制流必须使用客户端验证，服务端 GameTest 不能替代。
- **数据/消费者：** 分类、默认、配方覆盖、签名生成按现有步骤表达；运行期不额外读配置。查询保留自然温度回退与材料不可用的区别，源表/JSON/NBT/config 键不变；纯计算类不新增 Level、网络或主线程总入口依赖。

**代码检查：** 保留 SoA 数组、双缓冲、位图、预算、索引、缓存粒度、锁和线程归属；不增加逐节点/查询分配、重复大缓冲、全量扫描、世界读取或网络工作量。固定生命周期对象若增加，单列数量、引用及维护收益。

频繁循环中不用 Stream、装箱集合、捕获 lambda 或通用函数对象替代原循环。不凭推测 JIT 不内联否定一个清楚的方法提取；也不能用“应该会内联”忽略实际退化。浮点运算顺序、expm1、位移、排序、正反扫及缓冲交换位置不变。

**人工阅读：** 从代码直接回答：入口在哪、值是什么单位和索引、谁改状态、为何这个顺序、常见规则改哪里。简单 getter 不机械展开；只格式化涉及代码；不设任意行数上限。Java 名称用常规英文，复杂规则注释用简短直白的说明，不堆“权威/事务/契约”等词。

**性能对照：** 名称/注释改动集中编译即可。确实提取频繁调用路径时，在改前记录相同场景基线，改后用同 JDK/JVM、预热和工作量核对耗时分布与分配。保持完成 cut/节点/边数量和行为输出一致，不能以少做热工作获得的低耗时当成功。solver 微基准不代表采集/保存或多人 MSPT。

有可重复的明显退化先消除额外包装或调用绕行，必要时保留原热循环，仅整理外围；JIT 细微差异尽量控制，可读性和可维护性仍优先。不新增监控/benchmark 框架，不以一次测试声称所有规模性能相同。

每组交付按以下成本维度验收，不能仅用总耗时掩盖工作增加：

| 维度 | 基本不变的条件 |
|---|---|
| 工作规模 | 相同输入的节点/边、完成 cut、变化 Brick、路由访问预算和查询频率不增加，结果可用性保持 |
| 分配与常驻 | 稳态无新增逐节点/逐查询对象；大数组/纹理数量及容量保持；初始化对象单列，不只看 GC 后净堆 |
| 遍历与同步 | 不增加全量扫描、额外排序、世界读取、锁和线程切换；维护动作保持原脏数据范围 |
| 网络与 GPU | 同窗口的消息与有效温度保持；请求频率、上传范围、深度复制和绘制次数不增加 |
| 实测 | 对实际改动热点采用相同运行参数、预热和工作量比较耗时分布与分配，可重复退化须处理 |

这些是实施验收条件，不是尚未修改代码前的实测保证。最终区分结构检查和实际测量结果；不以“JIT 会优化”接受新增扫描或分配。测试一次发现差异先辨别波动和工作量，不用无限重测挑选好看的结果。

## 8. 验证与交付

| 改动 | 现有验证重点 |
|---|---|
| 类型/方法改名 | 搜索 Java 和字符串引用，compileJava、compileGameTestJava |
| 材料样本/公式 | ThermalTransitionDataGameTests、ThermalDormantCoolingGameTests、phaseRequestAcknowledgementIsExactlyOnce |
| 拓扑流程 | ThermalTimingGameTests、localMutationRebuildsOnlyTheChangedBrick、phaseRequestSurvivesSameBrickTopologyChurn |
| 待办/驻留/双缓冲 | sourceMixingChangeSurvivesRefusedGeometryBudget、coldMaterialNeighborKeepsItsIncomingRequest、activityThresholdsKeepTheCompletedHotMask |
| 红外搬移 | brickMutationKeepsUnchangedMaterialTemperatures、restartedRuntimeCannotReuseTheOldMaterialBaseline、warmedMaterialRemainsVisibleAfterTheRuntimeCloses、fullWindowPartitionsPreserveEveryBrickAndFieldFootprint |
| 保存/重载 | ThermalLoadedWorldGameTests 和现有 dormant/save 回归 |
| 源发现/绑定/账本 | sourceLedgerDeliversPowerAtTheExactCut、sourceEnergyIsAccountedWhenMaterialCannotAcceptMore、sourceLifecycleChangesConductanceWithoutReplacingCells、延迟 cut 对照 |
| 批次/调度 | fixedTwentyTickBatchesAdvanceWithoutLegacyScheduler、空发布、预算拒绝、卸载/重启已有回归 |
| 辐射/场/客户端 | 现有静态辐射/物品查询和 ThermalGameplayFieldGameTests；涉及 GL 控制流沿用客户端验证，记录实际范围 |

改名还需更新 VarHandle 的 enqueued/fullResync/deferredFullResync、测试 getDeclaredField/getDeclaredMethod 及反射 helper 的字符串。按实际改动符号搜索，不增加测试专用生产 getter，不保留旧类型别名。

结构改动按影响运行相应 GameTest，所有批次结束后运行一次完整 GameTest；仅在后续修改、失败或新疑点时重跑。相同输入的 H/温度、请求顺序和查询可用性保持，不能放宽断言掩盖改变。不恢复 JUnit；只有实际变更缺少必要边界覆盖时才补现有 GameTest。

每批报告：改名与移动、职责如何更清楚、对象/数组增减、验证结果、保留问题。更新 docs/climate 的实际类名和阅读入口，追加 diary，清理本轮临时文件；保留必要测试、人工源表与用户存档。

不实施植物提示缓存刷新缺口，不改 worldgen 岩浆初温、热平衡、渲染后端、玩家或热网模型。player/unused 中曾明确要求保留的历史模拟器不自动删除。源表 state_transition.xlsx 与生成器保持，128 MiB 显式预算也不能误称为整个系统堆上限。

**完成标准：** 覆盖表每组有结论，八批改动可单独解释与核对；主流程直白、类型职责清楚、必要重复有理由；热行为和主要成本保持；对应验证完成。

## 9. 实施结果

2026-09-16 已完成。细节与测量见[实施记录](../diary/2026-09-16_15-24-40_thermal-readability-implementation.md)。没有新增运行时节点对象、数组、扫描、锁或渲染 pass；原样本和采集器移动后仍按原生命周期复用。最后一次完整 GameTest 为 102/102，最后的存档索引命名与等值常量整理另经编译通过。

| 覆盖组 | 实际结果 |
|---|---|
| 材料与数据 | 实际温度/相变阈值分开命名，profile/data 编译展开排版，G/C/O 法则保持 |
| 捕获与主线程 | Input 移出采集器；请求校验归 PhaseController 并删除 PageManager/Input 转发入口；变更记录 stride/offset 命名，移除无效初温传参；检查点跨层协调仍记为职责重叠 |
| 异步调度 | Engine/提交器说明交接次序，mailbox 标明等待 ACK；线程池和关闭算法已清楚，保留 |
| 源与账本 | 绑定解析展开，三种能量去向注明；原索引/账本/累积器已有明确阶段和单位，保留算法及存储 |
| 几何与连接 | TopologyUpdatePlanner 改名，compileCells 提取真实 Air 连通动作，路由阻力/四阶段命名，接触端点分清 |
| 节点与分配 | airNode/materialCell/materialNodeMask 统一，布局位编码说明；原 free-span 索引和 phase 状态机保留 |
| 换热与相变 | 相变阈值独立接口，材料 law/冷却展开；原 solver 分三类边执行、显热/潜热内核保留，不增加共享策略 |
| 查询与驻留 | MaterialSample 归位、ReadCursor 改名，保留一次发布遍历；WorkerPageStore 展开表达，位图交换和驻留规则保持 |
| 休眠与存档 | 两种存储分清，残差单位/打包/预算说明，偏移和计数命名、同一预算常量复用；NBT 不变 |
| 辐射与场 | 场合成表达展开；静态辐射 index/service/tracer 已按真实职责分开，原候选与缓存算法保留 |
| 红外两端 | InfraredCapture 独立、调用传当前发布器；客户端标明接收/上传边界，原编码、GPU 资源及 GL 流程保留 |
| 消费者边界 | WorldTemperature 旧注释和无调用常量清理；玩家、热物品、城镇查询单位边界核对，算法保持 |

性能结论限定为结构成本保持及已测路径：求解器同为 2176 节点、零稳态分配；多次耗时有波动。IR 原计时没有预热，本轮补 256 次预热并记录全部结果，不把不同预热方式或不同场景负载算成严格加速比。未对全部硬件、多人大服或 GPU 做新性能承诺。

删除优先补充结果：相变校验不再回调 Input，tick 直接接收当前 publication，复用控制器原材料样本和已解析 Page，无新增对象/通知/缓存。补充修改后完整 GameTest 102/102 通过。详见[删除转发记录](../diary/2026-09-16_15-30-06_thermal-validation-forwarding-removal.md)。

后续四处精简已完成：无调用的 resolveAirFaceSlot、Profile.body 构造包装、Input 的两个解析场写转发已删除，TopologyCommitter 不再实例化。编译通过；固定 seed=0 的平坦 plains 测试世界 102/102 GameTest 通过。原随机自然世界两轮出现不同失败，尚未全部查明原因，不能将受控环境结果表述为原环境全通过。临时测试配置已还原，见[无用抽象清理记录](../diary/2026-09-16_16-00-58_thermal-unused-abstractions.md)。

验证中确认原水边界测试偶发落在禁止结冰的 deep_dark 群系；仅隔离测试的独立群系条件并在 finally 恢复数据，生产规则不变。未恢复 JUnit、未新增兼容别名；既有植物提示缓存和透明地形红外边界不在本轮修改范围。
