# 生成式研究系统 V2：从现有研究出发

- Time: 2026-08-25 16:17:39 +0800
- Updated: 2026-08-27
- Authors: Codex（OpenAI，系统、玩法与工程架构）；项目所有者提供目标、批评与取舍
- Status: in-progress；Phase 1 五类成果基础层已完成；Phase 2 第三轮返工的可玩主链已实现，正在做自动回归与客户端验收；Phase 3–6 暂缓
- Scope: FrostedResearch、观察与想法、证据板、居民讨论/计算/实验、实验台、五种研究成果、通用原型升级、数据包创作、旧研究兼容
- Related: [研究讨论](../discussion/research_conversation.md)、[V2 前一版](2026-08-25_16-17-39_generative-research-knowledge-laboratory-v2-prev.md)、[V0](2026-08-25_10-30-52_player-interactive-generative-research-system-v0.md)、[V1](2026-08-25_10-30-52_player-interactive-generative-research-system-v1.md)、[现有研究文档](../docs/research/README.md)

## 结论

V2 保留此前已经成立的研究过程：

> 世界、人物和城镇提供记录；证据板帮助玩家汇聚灵感；居民可以讨论、计算和做实验；玩家在实验台周围搭建真实空间并决定是否接受结果。

但研究成果回到当前游戏已经使用的语言。MVP 只有五种：

| 成果 | 玩家完成研究后得到什么 | 与旧系统的关系 |
|---|---|---|
| Finding（发现） | 一条能在档案、观察、提示或对话中使用的世界认识 | 唯一新增的知识成果 |
| Design（设计） | 明确列出的新配方 | 取代新内容中的 recipe Effect |
| Construction（建造） | 明确列出的多方块成型权 | 取代新内容中的 multiblock Effect |
| Procedure（方法） | 明确列出的方块右键使用权 | 取代新内容中的 use Effect |
| Prototype（原型） | 一件有来源和 serial 的实体试作品；安装后只影响该设备 | 唯一新增的工程成果 |

不在 MVP 中建立 DesignStandard，不要求 Design 再搭配“制造 Procedure”，也不把 Construction 继续拆成结构规格、施工方法和应用规则。一个研究可以同时产生多个成果。简易高炉本来就同时解锁耐火砖配方和高炉成型，V2 仍然一次给出 Design + Construction。

研究过程中的“观察方法、讨论方式、计算方式和实验步骤”统一叫 Protocol。Construction 只表示学会形成明确多方块；Procedure 只表示学会右键使用明确方块。

首批内容不再用白幕、水相变或一套完整 T1 工业线证明整个系统。它从五项现有研究开始：

1. 岩石的性质：以 Finding 为主；
2. 简易高炉：以 Construction 为主，同时保留耐火砖 Design；
3. 进阶真菌养殖：以 Design 为主；
4. 能量核心再利用：展示一项研究同时产生 Design、Construction 和普通物品奖励；
5. 能量塔效率提升 1：以 Prototype 为主。

这五项覆盖自然观察、现场调查、材料试验、生物培养、居民计算、工程台架和真实设备安装，同时不要求先发明新的世界主题。**它们是首批内容 fixture 和验收样本，不是平台能理解的五种固定流程。** 核心观察、证据关系、人物知识和行动编译层不得引用铜、探矿、菌床、高炉、能量塔、具体 topic ID 或具体 recipe ID。

## 2026-08-26 客户端验收结论与纠偏

Phase 2 地质纵切当时已经证明结果投影、桌面会话和现场比较可以接通，但没有通过玩家体验与通用性验收，因此不能标为完成。这次客户端实测暴露的四类问题是后续返工的历史输入：

1. **收件箱同步实际中断。** 测试步骤产生的 1 条铜矿露头和 3 条岩样已经持久化在服务端；当时 `FHKnowledgeDataSyncPacket.handle` 的 `DistExecutor.safeRunWhenOn` 使用了捕获同类 client 方法的 lambda，被 Forge 判为 unsafe referent，`ClientKnowledgeDataAPI` 从未安装快照。当时的 `runClient` 世界还主动安装着引用未注册 `frostedresearch_test:smoke_view` 的 Phase 1 测试 datapack，导致正式 catalog revision 保持 0。这两项都要先修复，不能把空界面误判为玩家没有记录。
2. **观察入口被内容反向绑架。** 当时的 `ResearchNotebookItem` 只认识铜矿和石头，失败文本也只谈探矿；`TeamResearchService` 和 `TechnologyAccessResolver` 直接引用地质 topic、岩样类型、对照距离和铜矿结果。这些只能存在于地质 provider、Idea rule、protocol 和 resolver 中，不能成为研究平台语义。
3. **发现过程被写成了隐藏任务清单。** 当时实现要求玩家先收齐“露头—邻近岩样—远处对照”，再获得本应更早出现的想法，同时提前显示“补露头/补对照”等 topic 目标。这把非线性发现变成了线性前置任务并泄露答案。对照样本与正式比较应是记下 Idea 之后才可能出现的 ResearchNeed。
4. **人物交互替玩家预设了问题。** 当时的“询问经验”实际等于“询问探矿经验”，空结果也回答“没有探矿经验”。通用入口应让人物谈自己的经历或知识；只有人物确实拥有某个领域知识包，或玩家明确拿相关记录追问时，才进入该领域。

返工采用前一版已经提出、但本次实现没有守住的边界：`ResearchSubject / observable facets`、不可变 `KnowledgeIngress`、可扩展 `ObservationContext`、直接组合匹配的 `IdeaRule`、Idea 之后的 `ResearchNeed / MethodContract`，以及人物持有的 `KnowledgePackage`。通用证据关系只保留为展示辅助。不恢复完整 Claim DAG、DesignStandard 或任意脚本；仍以五项现有研究控制 MVP 范围。

发现阶段的可见性固定为：

~~~mermaid
flowchart LR
    O["带可选 ObservationContext 的记录"] --> R["证据板：选择记录组合"]
    R --> G["整理证据纸牌会话"]
    G --> I["一个已记下的 Idea"]
    I --> N["当前 ResearchNeed 与可用方法"]
    N --> A["玩家接受结论"]
    A --> T["正式 topic 档案与固定成果"]
~~~

- Idea 之前不显示 topic 名、目标 Finding、目标配方、完成条件或“补某个 topic 专用证据”的行动卡。
- IdeaRule 直接定义“哪些 ObservationContext 组合能够产生这个想法”。任意一个组合满足即可；`EvidenceRelation` 只用于把组合解释成人话，不再是 IdeaRule 之前必须生成和持久化的第二层语义。
- 服务端可以在纸牌期间保存不可见的匹配令牌，但玩家和内容模型只有一个 Idea 概念。单一匹配在完成纸牌后直接写入 Idea；多个匹配只在结束页让玩家选一个，不再创建需要再次确认或再次补证据的 `IdeaCandidate` 资产。
- 记下 Idea 后，工作页只显示玩家已经说得出口的问题、当前最小缺口和可采用的方法，不预告尚未取得的固定成果。
- 完成并接受研究后，才把正式 topic 标题、结论档案与 Finding / Design / Construction / Procedure / Prototype 固定展示。
- 直接获得成熟知识仍可跳过相应阶段；例如人物直接教授 Design 时不要求玩家伪装成重新发现它。

## 2026-08-26 第三轮客户端验收：ObservationContext 优先

第二轮可操作客户端已经证明收件箱、钉位、纸墨与旧纸牌能接通，但继续暴露出五项必须先解决的设计问题：

1. **整理会话不能成为单向页面跳转。** `V2_INSPIRATION` 进行中打开知识实验室时，当前实现会在下一 tick 强制弹回绘图台。玩家必须能够在纸牌和实验室之间自由往返；纸牌左上角持续显示“正在整理证据”以及本局记录数，实验室显示同一只读会话并提供“返回纸牌”。
2. **行动卡不是斜纹底上的单行文字。** 当前普通、禁用和悬浮状态对比不足，长文本如“增加另一份独立样本”与按钮背景互相干扰。行动卡要有稳定实色/边框、足够高度、自动换行或省略加 tooltip，并且禁用态仍可读。
3. **地质首例过度研究化。** 初级研究不再要求露头邻近、远处对照、增加独立样本和 `MATCH` 报告。任意一条石头观察与任意一条矿石观察就是一个合法 Idea 组合；Idea 后若需要研究，只安排一个消费已有两条记录的轻量理论整理，不要求玩家再次外出取样。
4. **当前记录不是完整的观察。** 方块 ID、位置与时间被写成固定字段，天气、温度、生物群系、实体和玩家选择没有统一语义。Phase 2 的首要对象改为可扩展 `ObservationContext`；IdeaRule 直接对玩家选择保留的上下文字段做类型化组合匹配。
5. **知识实验室还不是团队知识的前端。** 当前页面主要服务证据钉位和单个地质 workflow，玩家无法稳定浏览全部观察、全部 Idea、研究制品和已经取得的各类成果。Knowledge Lab 必须成为 `TeamKnowledgeData` 的完整、安全、可检索读模型；某条数据可以分页或折叠，但不能因为不属于当前 topic、超出首屏或定义暂时缺失而没有可达的呈现路径。

这次返工不以升 schema 号为目标。快速迭代允许直接替换尚未通过验收的 `pendingCandidates / ideaCandidates / FieldComparisonArtifact` 地质原型；只保留团队成果、已归档普通记录、旧研究和稳定配方等仍有玩家价值的数据。

## 设计原则

1. **研究过程比抽象进度条重要。** 玩家做的是记录、连接、询问、委托、搭建、运行和判断，不是往研究点池里填数字；但“正在观察一个对象”的实际动作必须有短时进度条和取消反馈，而不是瞬间右击写库。
2. **成果必须立即可解释。** 玩家能用一句话回答“我知道了什么”“我会搭什么”“我能造什么”“我手里多了什么”。
3. **不强制所有研究从玩家自己的 Idea 开始。** 难民、居民、遗迹、任务和器物可以直接带来 Idea、Finding、Design、Construction、Procedure 或 Prototype。
4. **居民不是灵感生成器。** 讨论、计算、实验是三种平级劳动，都占用居民、材料和时间。
5. **实验仍然是真实 Minecraft 活动。** 玩家要放实验台、准备样品、满足环境和装置要求；数学与采样细节由 Protocol、居民或机械计算器处理。
6. **不是所有研究都需要实验室。** 岩石调查可以靠现场记录与计算；高炉和真菌研究需要实验台；塔升级还需要安装到真实设备试用。
7. **现有机制决定可声称的结论。** 当前探矿工具实际扫描附近矿石，因此“岩石的性质”首版只讨论可用的勘探迹象，不虚构尚不存在的地层化学或矿脉生成学。
8. **一项研究可以给多个结果。** 这是当前 Effect 数据的真实形态，不再用复杂的知识包类型包装它。
9. **Prototype 是实体，普通奖励不是 Prototype。** 只有具备 serial、兼容 host 和安装后局部效果的物品才是 Prototype。
10. **普通作者只写稳定内容数据。** 公式、世界采样和机器内部字段由注册过的 provider/profile 负责；JSON 不直接读取 NBT 或 Java 字段。
11. **观察先于 topic，记录不属于 topic。** 玩家记录一个对象时不需要先知道它能参与哪项研究；没有任何当前 rule 能解释的记录也永久保留，并可在未来 datapack reload 后参与新想法。
12. **工具只选择观察通道。** 研究笔记、样品、岗位报告和访谈决定“怎样观察”，不决定“玩家正在研究什么”；地质扫描、温度、机器状态等由独立 provider 追加。
13. **Idea 是问题的开始，不是证据收齐奖励。** 一个作者声明的最小 ObservationContext 组合即可产生 Idea；对照、重复、计算和实验只在该 Idea 确实需要时出现，不能成为所有初级 Idea 的通用税。
14. **内容 fixture 不进入核心分支。** 首批五项研究必须通过同一注册接口运行；增加第二项不同领域内容时，不得修改通用 Java 服务、绘图台页面或人物对话。
15. **记录保留什么由玩家决定。** 目标对象是记录的最低事实；位置、生物群系、时间相位、天气事件、温度和其他 provider 字段是否进入可匹配 Context，由当前笔记的 capture profile 与可用仪器共同决定。未选择的字段不得被 IdeaRule 偷用。
16. **一个 Idea 只有一层。** `IdeaRule.any_of` 中任意组合满足就产生同一 Idea。内部匹配令牌、纸牌 session 或结束页选择不是第二种知识资产，也不能要求玩家在纸牌完成后再次满足另一套 Idea 条件。

## 玩家只需要学会的词

| 玩家用语 | 含义 | 常见动作 |
|---|---|---|
| 记录 | 亲眼所见、别人所说、报告所写或样品所代表的一件事 | 归档、追问、钉到证据板 |
| 想法 | 值得继续追查的规律、解释或改法 | 讨论、计算、调查、实验、搁置 |
| 研究 | 围绕一个想法或外来知识打开的工作页 | 选择下一件事 |
| 实验 | 在实验台及其周围真实完成的一次试验 | 准备、运行、封存 |
| 发现 | 团队现在能够使用的一条世界认识 | 查看、识别、引用 |
| 设计 | 一张已经能够照着制造的配方 | 在 JEI 查看、合成 |
| 建造 | 已经学会的明确多方块成型办法 | 成型 |
| 方法 | 已经学会的明确方块使用办法 | 右键使用 |
| 原型 | 一件尚未量产、但可以真实安装试用的物品 | 安装、拆除、继续观察 |

“证据”“候选解释”和“实验建议”可以出现在界面文本里，但不要求玩家理解 Claim、Obligation、revision 或 ApplicationRule。

## 玩家循环

~~~mermaid
flowchart LR
    S["世界、难民、居民、任务、器物"] --> R["记录或直接知识"]
    R --> B["证据板：汇聚灵感"]
    B --> I["想法 / 研究工作页"]
    I --> D["讨论"]
    I --> C["计算"]
    I --> E["调查或实验"]
    D --> I
    C --> I
    E --> I
    I --> F["Finding"]
    I --> G["Design"]
    I --> C2["Construction"]
    I --> P["Procedure"]
    I --> O["Prototype"]
    O --> H["真实设备安装与试用"]
    H --> I
~~~

这不是固定顺序。一个完整难民手册可以直接给 Design、Construction 或 Procedure；一件遗迹原型可以直接拿去安装；岩石调查可以没有正式实验；一项工程研究也可以同时给 Design 和 Construction。

工作页在任何时刻只展示：

1. 当前想弄明白或改变什么；
2. 一个推荐行动；
3. 至多两个替代行动；
4. 每个行动需要的地点、物品、居民或设施；
5. 完成后会得到“记录、讨论纪要、计算报告、实验记录”中的哪一种。

## 知识实验室：团队知识的完整呈现

知识实验室是 `TeamKnowledgeData` 与相关安全 projection 的主界面，不是当前 topic 的任务面板。顶层只分三页：**观察档案、想法与研究、成果档案**。证据板是观察页中的工作区；行动卡是想法详情中的工作区；它们不再占据整个实验室并挤掉历史数据。

| 权威数据 | 唯一主要呈现位置 | 关联入口 |
|---|---|---|
| `observations` | 观察档案 | Idea 证据、制品引用、Finding annotation |
| `ideas` | 想法与研究 | 来源观察、研究制品、完成后的成果 |
| `workArtifacts`（当前原型为 `comparisons`） | 对应 Idea 时间线；无主项进“未归类研究材料” | 来源 observation、protocol、结果审阅 |
| `acquiredFindingIds` | 成果档案 / Finding | 对观察、HUD、对话的 view |
| `acquiredDesignIds` | 成果档案 / Design | recipe IDs、JEI、实际解锁状态 |
| `acquiredConstructionIds` | 成果档案 / Construction | multiblock IDs、成型状态 |
| `acquiredProcedureIds` | 成果档案 / Procedure | usable block IDs、使用状态 |
| `prototypePlacementIndex` 与真实实体 | 成果档案 / Prototype | serial、背包/实验台/host 位置 |

### 共用外壳与自适应布局

~~~text
┌ 知识实验室 ───────────── 当前整理：5 条记录 [返回纸牌] [取消] ┐
│ [观察 126]   [想法与研究 8]   [成果 14]        [搜索……]     │
├────────────────┬────────────────────────┬─────────────────┤
│ 可滚动索引/筛选 │ 当前条目的完整安全详情   │ 上下文操作/证据篮 │
│                 │                        │ /行动卡/来源      │
└────────────────┴────────────────────────┴─────────────────┘
~~~

- 顶栏永久显示三个分页及总数；当前 active inspiration session 作为全局横条存在，所以切到想法或成果页也不会失去“返回纸牌”。
- 宽屏使用三栏：索引约 28%、详情约 44%、上下文区约 28%；中等宽度合并详情与上下文；窄窗口使用单栏和面包屑返回，不把三栏压到互相重叠。
- 所有尺寸基于 GUI-scaled width/height 与字体实际测量，不针对 1920×1080 写死坐标。`KnowledgeLabLayout` 是 render、hover、tooltip 和 click hitbox 的唯一几何来源；不得分别计算可见按钮和点击区域。
- 列表采用稳定排序和滚动/分页，默认最近更新优先；搜索支持本地化标题与 raw ID，筛选和当前选中条目保存在客户端 `KnowledgeLabViewState`，不是团队知识。
- “显示全部”表示每条权威数据都能通过分页、筛选或关联跳转到达，不要求一帧绘制全部，也不允许只截取前 10/16 条。
- 空态分别解释“团队还没有观察”“还没有形成想法”“还没有完成研究”，不把空页解释成网络错误或某个领域缺材料。

### 观察档案

观察页索引显示所有安全 observation summary，至少支持全部、方块、实体、人物/证词、报告/器物、含仪器测量、带 Finding 注解等筛选。每一行只显示图标、对象人话名称、两到三个关键 retained-context chips、来源和最近观察时间；坐标只在玩家当时选择保留 `exact_position` 时出现。

详情页显示：

- subject 类型、对象 ID/图标和公开 state；
- 玩家实际保留的位置、生物群系、时段、天气、温度与其他 typed context；
- 观察渠道、仪器、来源人物/报告、首次与最近时间、合并来源数；
- 已取得 Finding 允许显示的 annotation；
- “钉到证据篮 / 从证据篮移除”，以及跳到引用它的 Idea。

未选择字段、sealed facts 和未取得 Finding 的解释不显示占位符，也不能通过 tooltip、搜索文本或排序泄露。缺少翻译/provider 的记录仍用对象 raw ID、已知 context 和“档案定义暂不可用”显示，不从列表消失。

右侧证据篮最多五条，和当前绘图台 session 共用真实 `pinnedRecordIds`。未开始整理时可编辑并显示“整理这组记录”；active session 中改为只读，提供“返回纸牌”和显式取消。证据篮不因切换主分页清空。

### 想法与研究

想法页列出全部 `IdeaRecord`，包含 `OPEN / READY / RESOLVED / ORPHAN`，默认把仍需玩家处理的放在前面。每一行显示 Idea 人话标题、状态、来源数、关联 observation/artifact 数和最近更新时间；`RESOLVED` 不从列表删除。

选中 Idea 后，详情由上到下固定为：

1. 玩家已经能说出口的 Idea 文本和状态；
2. 来源与关联 observation，可点击跳回观察详情；
3. 按时间排列的讨论纪要、计算报告、调查/实验制品；
4. 当前一到三张 action card；
5. `READY` 时的“审阅并接受结论”，`RESOLVED` 时跳到成果档案。

work artifact 必须归到其 Idea 的时间线；引用缺失 Idea 的 artifact 放进“未归类研究材料”，仍可查看类型、时间、来源和 raw ID。`ORPHAN` Idea 显示“定义暂不可用，资料已经保留”，不显示虚假的下一步，也不被 projection 丢弃。Idea 前继续不显示隐藏 topic 标题或成果；Idea 建立后可显示研究工作名与方法，固定结论和成果仍到接受后才进入成果页。

### 成果档案

成果页呈现团队已经取得的全部研究结果，并按五类切换：

| 类别 | 列表与详情必须显示 |
|---|---|
| Finding | 结论文本、适用范围、Finding views、来源研究，以及它给观察/HUD/对话增加了什么解释 |
| Design | 设计标题、来源研究、明确 recipe IDs、当前配方可用状态，并能跳到 JEI/配方查看 |
| Construction | 建造标题、来源研究、明确 multiblock IDs 与成型权限状态 |
| Procedure | 方法标题、来源研究、明确 usable block IDs 与使用权限状态 |
| Prototype | profile、serial、owner team、当前在背包/实验台/哪个 host；它是物理实体索引，不伪装成 acquired team set |

前四类分别以 `acquiredFindingIds / acquiredDesignIds / acquiredConstructionIds / acquiredProcedureIds` 为权威，不能只展示 `KnowledgeProjection.findings()`。Prototype 页合并 `prototypePlacementIndex` 与当前可解析的真实物品/host 状态；若一项已完成研究定义了 Prototype，但当前没有实体，只显示“当前没有可定位的原型”，不能凭 topic 重新生成物品。

每个已取得 ID 都必须有一行。catalog 定义暂时缺失时显示类别、raw ID、已知来源和“定义暂不可用”；不能像当前 `KnowledgeProjection` 注释所说那样故意省略 orphan result。legacy 权限可以在来源区标为“旧研究”，但不得冒充 V2 Finding/Design 资产。普通物品奖励不进入成果档案。

### 完整呈现与安全投影

客户端不直接接收持久化 `TeamKnowledgeData`。服务端提供专用的 `KnowledgeLabProjection`：

~~~text
KnowledgeLabCounts
  observationCount / ideaCount / unresolvedIdeaCount
  findingCount / designCount / constructionCount / procedureCount / prototypeCount

KnowledgeLabPageQuery
  tab / filters / search / sort / cursor / expectedRevision

KnowledgeLabPage
  revision / entries(max 40) / nextCursor

KnowledgeLabDetailQuery
  entryType / stableId / expectedRevision

KnowledgeLabDetail
  safe typed summary / links / allowed actions
~~~

列表与详情按需分页，客户端按 revision 缓存；旧响应到达时丢弃并重新查询。所有 packet 和关联跳转使用稳定 UUID/ResourceLocation，不使用屏幕行号。projection 负责把 observation、Idea、work artifact、四类 acquired result 和 Prototype 位置映射成安全条目，并为 orphan 生成最低限度 fallback；sealed facts、未保留 context、隐藏 IdeaRule 和未取得成果永不进入这些页面。

## 知识怎样进入研究

### KnowledgeOffer

世界来源先生成玩家可感知的 `KnowledgeOffer`。玩家亲历、交谈、阅读、拾取、领取报告或接受教学后，服务端才把它接受为不可变 `KnowledgeIngress` 并写入团队档案。Offer 是“现在可以接收什么”，Ingress 是“团队实际在何时、从谁、通过什么方式得到什么”；二者不能用 topic ID 代替来源事实。

Offer 的载荷只需支持：

~~~text
Observation
Idea
Finding
Design
Construction
Procedure
Prototype
~~~

每条 Offer / Ingress 保存来源、人物或地点、时间、适用对象，以及它是亲历、证词、文献、教学还是实体物品。直接成熟知识可以带 result ID；Observation 和证词本身不带 topic 所有权。

### 通用观察契约

研究笔记是一层通用观察外壳，不是“瞬间把目标方块 ID 写进团队数据”的工具。一次观察分为 `CaptureSession → ObservationDraft → Archive/Discard`：玩家瞄准方块或实体，服务器在短时持续动作结束时对同一目标和同一 game tick 采样，玩家只把本次选择的上下文字段归档。首版必须同时支持方块和实体，确保“傍晚在苔原发现一只羊”不是未来另开一套模型的特殊需求。

目标对象身份是观察的最低事实。其他字段来自可扩展的 `ObservationFieldType` 注册表：

| 首批 field ID | 来源 | 无额外工具可选 | 说明 |
|---|---|---:|---|
| `frostedresearch:block_state` | 目标方块 | 是 | 方块 ID 与可见 state；目标为实体时不出现 |
| `frostedresearch:entity_type` | 目标实体 | 是 | 实体 type；稳定 UUID 只在确有实例追踪需要时保留 |
| `frostedresearch:exact_position` | 服务端目标坐标 | 是 | dimension + BlockPos；取消选择后 IdeaRule 不能读取位置 |
| `frostedresearch:biome` | `ServerLevel#getBiome` | 是 | 记录完成 tick 的 biome key/tag |
| `frostedresearch:time` | `dayTime / gameTime` | 是 | 原始采样 tick 留在 provenance；玩家可选保留日相/时段供匹配 |
| `frostedheart:weather_event` | `WorldClimate#getClimate` 与白幕空间查询 | 是 | 记录当前位置实际生效的离散气候/天气事件，不从客户端视觉猜测 |
| `frostedheart:block_temperature` | `WorldTemperature.block` | 否 | 只有搭配已注册测温仪器（首个为土壤温度计）时可选 |

后续设备状态、光照、实体年龄/健康、样品性质和居民岗位条件都通过相同 field registry 加入，不给 `ObservationContext` 增加 topic 字段或为每个领域新增 record class。

~~~text
ResearchSubjectRef
  kind                 // block / entity / item / location / event / report / person
  typeId               // 稳定 registry ID 或注册 subject type
  instanceRef?         // 位置、实体、样品或报告的稳定引用

ObservationCaptureMeta
  capturedAtGameTime   // 服务端归档与幂等所需；不是自动可匹配的“已保留时间”
  observer / channel / instrumentIds
  targetRef            // 完成采样前用于重验目标；不等于公开 exact_position

ObservationContext
  fields: Map<FieldId, TypedContextValue>
  retainedFieldIds     // 只有这些字段可以投影、显示或参与 IdeaRule

ObservationRecord
  id / subjectRef / context
  publicFacets         // 客户端可见的对象能力/分类
  measurements         // 已选择并由工具/provider 实际取得的公开测量
  sealedFacts          // 只在服务端，供取得相应 Finding 后投影或供 protocol 计算
  provenance: ObservationCaptureMeta
  semanticKey
~~~

`ObservationProvider` 负责 `supports(subject, channel)`、对象 snapshot、可提供 field、仪器要求、该 provider 的语义去重和安全投影。通用 provider 支持所有方块与实体；天气 provider 读取现有权威 `WorldClimate`，温度 provider 只在 capture session 确认玩家携带兼容仪器时调用 `WorldTemperature.block`。没有 provider 增量时也应反馈“已记录”或“没有可区分的新信息”，不得反馈“可以探矿”。

`NotebookCaptureProfile` 保存玩家对这一本笔记的默认字段选择，并在开始观察时显示为可切换的 context chips。首版交互固定为：

1. Shift+右键打开/收起记录设置，勾选位置、生物群系、时间、天气与当前可用仪器字段；目标身份始终保留。
2. 对方块或实体长按使用 `40 ticks`（默认 `2 s`）；HUD 显示目标名、已选择字段和进度条。
3. 松开、换目标、离开交互距离、切换维度或目标失效时取消，不产生半条记录、不消耗耐久。
4. 完成 tick 由服务器一次性采样所有已选择字段，形成 `ObservationDraft`；玩家可以保存或丢弃。保存才进入团队收件箱。
5. 相同 capture profile 可连续使用；新增仪器只让新 field 变为可选，不暗示某个 topic。

这个短进度条表示角色确实花时间观察，不是研究点，也不与后续实验/理论进度共享数值。

语义去重由 provider、subject 类型和**已保留字段**共同定义：同一方块但记录了不同天气/时间/温度时不能被错误合并；未保留 exact position 的记录不能按隐藏坐标参与 IdeaRule。机器运行窗口与人物证词仍有各自签名。`16³` 区段和 `OreProspectingModel.Snapshot` 只是某个地质/工具 provider 的策略，不属于通用 record。

归档路径不得查询当前 topic catalog，也不得因为缺少 topic、IdeaRule 或翻译而拒绝记录。网络只发送玩家已保留的 context、`publicFacets`、来源摘要和已获 Finding 允许公开的 view fragment；未选择字段与 `sealedFacts` 永不进入客户端 projection。catalog 删除、出错或 reload 时，历史记录继续存在，恢复或新增 rule 后重新参与匹配。

示例：“傍晚在苔原发现一只羊”至少是 `subject=entity:minecraft:sheep`，并且玩家在 capture profile 中保留 `time=evening` 与 `biome=<tundra biome>`。如果玩家没有选择位置或天气，这条记录就没有这些字段；后续 IdeaRule 不能从服务端采样元数据偷偷补上。

### 首批来源

| 来源 | 玩家看到什么 | 可能给什么 |
|---|---|---|
| 玩家亲历 | 发现矿石、使用菌床、看到塔燃料下降、拾到核心 | Observation、Idea seed |
| 主动记录或取样 | 对方块、设备、样品或地点使用笔记/工具 | Observation、specimen ref |
| 未招募难民 | 交谈、展示相关物品、交换知识 | Idea、Finding、Design、Construction、Procedure，少量情况下给 Prototype |
| 已招募居民 | 岗位报告、访谈、研究所劳动 | Observation、讨论纪要、计算报告、实验记录 |
| 城镇岗位 | 矿场、食物、物流、锅炉或塔运行的压缩报告 | Observation、问题报告 |
| 遗迹与任务 | 旧图纸、操作手册、损坏器件、船上档案 | 任意成熟度知识或普通物品 |

难民不在入队时把全部知识自动并入团队。玩家必须交谈、让其进入相关岗位、邀请其参加讨论，或接受其教学。若难民已经掌握完整 Construction、Procedure 或 Design，玩家可以直接获得它，不必把已有知识重新演一遍发现流程。

当前招募会创建新的 Resident UUID，因此实现时必须把难民 NBT 中的稳定 person knowledge 在招募成功事务内显式转移；失败时不删除、不重抽。

### 人物知识包与领域中性对话

`PersonKnowledgeOverlay` 保存的是人物已经初始化过的 `KnowledgePackage` ID 集合与来源背景，不保存“会不会探矿”这种平台字段。每个 package 可提供 Observation、证词、Idea 或成熟成果 Offer，并声明年龄、职业、生成背景与上下文触发条件。空集合也持久标记为已初始化，重载不重抽。

人物菜单的无上下文入口使用“聊聊你的经历”或“你熟悉些什么”。服务端枚举该人物当前愿意提供的 0–N 个 Offer：

- 没有可提供内容时只回答“暂时没有想起能帮上忙的事”一类领域中性文本；
- 人物拥有探矿 package 时，由人物主动提到矿场、岩石或寻找矿物，而不是假装玩家已经问过探矿；
- 玩家展示一条记录、物品或已存在 Idea 后，可以发起明确的上下文追问；此时领域相关的否定回答才合理；
- 一次谈话只接受玩家明确选择的 Offer，不在招募或打开菜单时倾倒全部知识；
- 招募事务成功后复制完整 overlay 到新 Resident，再加入城镇；失败时难民 overlay 原样保留。

首个探矿经历仍可在符合条件的成年/老年难民中按 10% 生成，但它只是第一个数据包 knowledge package。Phase 2 完成前必须另有一个非地质 package 通过同一菜单与 transfer 路径，不改通用 Java 分支。

## 证据板与“汇聚灵感”

绘图台继续使用现有 ResearchGame 的纸牌局：9×9 棋盘、可动牌、配对消除、顺序收束、纸墨消耗和局中保存都保留。

玩家钉上 2–5 份记录、证词、样品或失败报告，再选择：

- 我想弄明白它；
- 我想改进它。

如果内容只有一个明显方向，界面直接开始，不多问一次。

证据板可以从公开 subject、facet 与已保留 context 中派生 `EvidenceRelation`，但它只是 UI 解释与纸牌牌面，不是 Idea 的前置数据层。第一批可展示关系只有：

- 同一对象、不同条件或不同时间；
- 不同对象、相同条件或相同变化；
- 空间邻近或同一地点；
- 多个独立来源重复出现；
- 两条记录互相矛盾；
- 与常态不一致的异常或失败；
- 人物证词、文献与亲历互相支持；
- 两个已公开 facet 之间的跨领域联系。

这些关系不引用 topic、不判断真理，也不被持久化成另一种“准想法”。牌面显示对象和玩家确实保留的条件；没有记录温度、天气或位置时，牌面不得展示或利用这些字段。跨来源配对时显示人话联系，例如：

- 同一地点，发现了不同材料；
- 相同材料，在不同温度下表现不同；
- 塔保持工作，但燃料下降得很快；
- 菌床能培养真菌，但携带和补热都不方便；
- 一块耐火材料在普通炉火中没有损坏。

隐藏的 `IdeaRule` 直接消费团队已经公开的 subject/facet/context。每条 rule 至少声明一个 `ObservationCombination`，多个组合使用 `any_of`：其中任何一个满足就能产生同一个 Idea。每个组合可以包含一个或多个 observation role，并只约束自己需要的 context 子集；没有声明的位置、天气、时间或温度不会被隐式要求。role 可接受替代 subject/facet/source，额外钉入记录默认忽略，匹配与钉入顺序无关。

证据板整理本身不生成隐藏真理。没有 rule match 时不开始或消耗纸墨，界面只说“这些记录暂时没有形成可追查的联系”，并给领域中性的建议。存在 match 时，服务器把匹配 rule 和实际使用的 record IDs 固定到 `InspirationSession`，纸牌完成后直接记录 Idea 并打开/更新工作页；相同 Idea 只合并来源。若同一组记录同时匹配多个 Idea，结束页只让玩家选择想记下哪一个，不创建持久 `IdeaCandidate`，也不重新要求另一轮证据。

地质首例只需要一个最小组合：任意 `forge:stone` 观察 + 任意 `forge:ores` 观察。它不要求相邻、不要求同一区段、不要求铜、不读取封存扫描结果。矿工证词 + 石头或人物直接提供 Idea 可以作为同一 rule 的其他 `any_of` 组合。该初级研究不再追加远处对照、重复采样或正式 `MATCH` 比较；这些保留给将来真正需要控制变量的进阶地质研究。

纸牌进行中，知识实验室与绘图台是同一 session 的两个视图：

- `MainGamePanel` 左上角显示“正在整理证据 · N 条记录”，并保留可点击的知识实验室入口；
- 打开实验室不会被 tick 自动弹回，当前五个会话钉位以只读状态显示，主按钮变成“返回纸牌”；
- 本局 rule match 与已消费纸墨不因页面往返重新计算或再次扣除；修改钉位必须先明确取消当前 session；
- 完成纸牌后可以留在绘图台看到“整理完成”，也可以返回实验室查看已经写入的 Idea；
- `LEGACY_CLUE` 游戏仍显示原 clue 状态，不冒充“正在整理证据”。

难民、居民、文献或器物直接给出的 Idea 可以跳过纸牌局。直接给出的 Finding、Design、Construction、Procedure 或 Prototype 则可以跳过整个想法阶段。

## 研究工作页与最小缺口编译

只有已经记下的 Idea 才进入工作页。后台保留证据来源和结果关系，但首版只识别五种缺口：

~~~text
NEED_OBSERVATION
NEED_DISCUSSION
NEED_CALCULATION
NEED_EXPERIMENT
READY_TO_DECIDE
~~~

每个注册的 `NeedResolver` 根据当前 Idea、可复用记录和工作制品返回一到三个 `ResearchNeed`。全局 `MethodContract` 再把 Need 与当前可用的现场观察、讨论、计算或实验方法匹配成行动建议。topic 可以提供方法提示和优先级，但不能封闭方法集合；其他内容注册的兼容方法也能满足同一 Need。

它只说明当前还缺什么，不替玩家开始任务，也不替玩家接受结论。结果清单与固定 topic 档案在接受前不显示；行动卡不得用未取得的 Finding 或配方名称剧透目标。

例：

~~~text
当前想法：改善送风也许能减少能量塔燃料消耗

推荐：把一段正常运行记录交给居民计算
也可以：请有锅炉经验的人讨论
也可以：先记录一次断供前后的塔状态
~~~

人员、设施、材料和当前世界条件由 `ActionResolver` 实时检查。居民换班或材料不足只改变“现在能不能做”，不重编知识关系。

普通内容作者不写义务 DAG，也不写布尔脚本。resolver/profile 是有界的注册类型，例如“两地点比较”“材料耐热试验”“培养前后对比”“设备运行前后比较”。同一种通用比较方法可以被不同 topic 引用，不能在 `TeamResearchService` 为每个 topic 写完整分支。

## 研究所：讨论、计算、实验平级

### 研究所作为城镇工作建筑

新增 frostedheart:research_institute，进入现有 TownStaffingPlan。玩家提高研究所用工，就会少一个矿工、猎人或物流员。

研究所只有三列工作：

| 工作 | 玩家提交什么 | 成本 | 产物 |
|---|---|---|---|
| 讨论 | 当前想法、2–5 份资料、需要的岗位或知识背景 | 通常 2 名居民、1 班次、纸墨 | 候选解释、证词、设计建议、实验建议 |
| 计算 | 一组记录和一个人话目标 | 1 名居民若干班次，或机械计算器动力；纸墨 | 比较、排序、规格、物料核对、前后差异 |
| 实验 | 一个实验建议、实验台和样品 | 实验员班次、样品、耗材、实际环境与装置 | 实验记录、异常和人话结论 |

居民讨论不会读取隐藏答案。岗位经验和私人知识决定能提出什么；智力与教育决定能处理多少来源和多复杂的计算。它们不变成“正确率”或研究点。

计算也不让玩家填写公式。玩家只选择“比较这两处”“核对材料”“比较塔改装前后”等目标。CalculationProfile 处理单位、采样窗口和异常，先输出人话摘要，详细数值默认折叠。

三类任务统一经历：

~~~text
QUEUED
→ WAITING_STAFF / WAITING_SUPPLY / WAITING_FACILITY
→ WORKING
→ AWAITING_REVIEW
→ APPLIED / ARCHIVED / CANCELLED
~~~

一名居民一天只能为一张研究工作单贡献一个班次。完成的讨论纪要、计算报告和实验记录都停在“等待玩家审阅”，不会自动发布成果，也不会自动排下一项研究。

机械计算器执行同一种 CalculationProfile，以 Create 动力代替居民班次；Create 缺席时居民路径完整可用。

## 专属实验台与实验空间

### 玩家看到的实验

新增 frostedresearch:experiment_table。实验台不是通用科研表单，而是一张固定准备屏：

| 检查栏 | 示例 |
|---|---|
| 空间与环境 | 封闭工作间、最低体积、当前温度、火源安全距离 |
| 装置 | 样品架、加热位、记录器、动力、待测部件位 |
| 人员与供给 | 玩家值守或居民班次、样品、纸墨、燃料、试剂 |

玩家选择一条作者写好的人话路线，然后：

1. 在实验台周围搭空间并放装置；
2. 放入真实样品和耗材；
3. 点击重新检查；
4. 选择亲自值守或排入研究所；
5. 运行中处理缺料、温度漂移、断电或错误样品；
6. 封存结果并回到研究工作页。

实验台首版固定为 4 个样品槽、4 个试剂槽和 1 个待测部件槽。protocol 可以给槽改名和过滤，但不能动态创建任意数量菜单槽。

### 空间不是固定多方块

实验以有限能力组合要求空间：

- 封闭空间、面积和体积；
- 当前真实温度和光照；
- 静态工作面、样品架与储物设施；
- 由 adapter 提供的实际加热、动力、记录或旋转能力。

玩家可以用不同房型满足要求。空间扫描只在重新检查、结构变脏或低频刷新时发生；运行中只读取缓存的空间和少量真实采样点，不逐 tick flood-fill。

不是每项研究都要实验台：

- 岩石的性质以现场观察和轻量理论整理为主；
- 简易高炉使用材料加热试验；
- 真菌养殖使用培养试验；
- 能量核心使用带动力的部件台架；
- 塔效率原型先在实验台制造，再安装到真实塔上试用。

### Protocol 与 Procedure 的边界

Protocol 是研究过程定义，例如：

- 对两个地点做盲测并比较后来发现的矿物；
- 加热一份耐火材料样品；
- 对两份真菌培养基采用不同补热方式；
- 给能量核心做低功率通电检查；
- 比较塔改装前后的有效运行时间与燃料装入记录。

Construction 与 Procedure 都是最终成果：Construction 只表示玩家已经学会形成某个多方块，Procedure 只表示玩家已经学会右键使用某个方块。二者不共用数据类型，也都不与 Protocol 共用数据类型。

## 五种成果与三条直接消费路径

### Finding

Finding 是带来源和适用范围的团队认识。它完成后直接进入 KnowledgeProjection。

Finding 可以：

- 在档案中形成可查条目；
- 让笔记、观察工具或居民报告显示更明确的解释；
- 给某种现象、材料或状态命名；
- 开放相关问法、提示或后续研究入口。

Finding 不解锁配方，不解锁多方块成型，也不增加设备数值。某项研究若还要给配方，就在 results 中再列一个 Design。

### Design

Design 就是一组明确 recipe ID。团队取得 Design 后，这些配方立即进入 TechnologyAccessProjection，JEI 和真实执行端都按同一集合判断。

Design 不再要求另一份“制造 Procedure”。如果一个研究产出培养箱 Design，玩家得到的就是培养箱配方。

新内容必须使用稳定 recipe ID，不能再按输出物模糊地锁住未来所有同输出配方。旧 EffectCrafting 的动态行为只在 legacy 兼容层保留。

### Construction

Construction 就是一组非空、明确的可形成多方块 ID。团队取得 Construction 后，对应权限立即进入 TechnologyAccessProjection。

Construction 不表示结构规格层、居民施工熟练度或设备数值。简易高炉 Construction 就是“可以形成 IE 简易高炉”。

### Procedure

Procedure 就是一组非空、明确的可右键使用方块 ID。团队取得 Procedure 后，对应权限立即进入 TechnologyAccessProjection。

Procedure 不表示实验步骤、维修手册或居民熟练度，也不提供效率加成。机械计算器 Procedure 就可以是“可以右键使用机械计算器”。

### Prototype

Prototype 是一件动态实体物品，至少保存：

- prototype profile ID；
- serial；
- owner team；
- 外观 profile 与材料；
- 当前是背包中、实验台中还是安装在哪个 host。

Prototype 不进入全队技术权限。只有它真实安装到兼容设备后，设备的 InstalledContributionSnapshot 才包含它的局部效果。

普通物品奖励不是 Prototype。进阶真菌养殖赠送的一份菌床、能量核心再利用领取的 energy_core，都仍是普通奖励；它们没有 serial 和局部安装效果。

### 一项研究可以给多个结果

成果不互相转换。研究直接列出它给什么：

~~~text
岩石的性质
→ Finding：规范勘探迹象可以提示附近矿物
→ Design：铜探矿镐配方

简易高炉
→ Design：高炉砖配方
→ Construction：形成 IE 简易高炉

进阶真菌养殖
→ Design：培养箱配方
→ 普通奖励：一份棕色蘑菇菌床

能量核心再利用
→ Design：T1 核心配方
→ Construction：形成 T1 能量塔
→ 普通奖励：能量核心

能量塔效率提升 1
→ Prototype：受控进气模块
~~~

没有 DesignStandard，也没有“先把 Finding 编译成 Design”的隐藏步骤。研究 resolver 只判断玩家是否已经得到这项研究定义的结果。

### 四个读模型到底投影什么

投影只是由权威数据重建的查询结果，不是第六种成果，也不是成果之间的转换。知识实验室的全量可浏览需求和世界中随时消费的轻量知识提示分开，避免为了打开 HUD 而同步整个档案。

| 内部读模型 | 输入 | 输出 | 消费者 |
|---|---|---|---|
| KnowledgeLabProjection | `TeamKnowledgeData` 全部安全条目、catalog、result source、Prototype placement | 分页的观察、Idea、work artifact、四类团队成果与实体 Prototype 索引；包含 orphan fallback | 知识实验室三大主视图 |
| KnowledgeProjection | 已取得 Finding、当前需要常驻的 view scope 与少量上下文摘要 | 笔记注解、识别、提示和对话所需的轻量信息 | 笔记、HUD、居民报告、剧情 |
| TechnologyAccessProjection | 团队已经取得的 Design、Construction、Procedure，以及 legacy 权限 | unlockedRecipeIds、formableMultiblockIds、usableBlockIds | JEI、配方执行、多方块形成、RightClickBlock |
| InstalledContributionSnapshot | 某个 host 里真实安装的 Prototype 和该 host 当前状态 | 这台设备当前生效的类型化局部贡献 | T1、以后实现升级接口的设备 |

最窄查询接口是：

~~~text
isRecipeUnlocked(team, recipeId)
canFormMultiblock(team, multiblockId)
canUseBlock(team, blockId)
resolveInstalledContributions(host)
~~~

每个答案附带来源 ID，方便 UI 显示“来自：进阶真菌养殖”或“来自：已安装的受控进气原型”。除此之外不需要 CapabilityManifest、ApplicationRule、RuntimeApplicationSnapshot 或 AppliedProcedureBinding。

## 五种成果的最小研究与一个复合案例

以下不是概念示例，而是 V2 的首批可玩内容。每项都从当前 research JSON、现有物品和现有 Effect 出发。这里从作者和验收角度完整列出入口、方法与成果；这些信息不得在玩家记下 Idea 前合并成可见 topic 任务单，固定成果也不得在接受研究前预告。

### A. Finding：岩石的性质 → 岩石与矿迹

#### 当前基础

- 旧 ID：geology_understanding；
- 当前线索：铜锭；
- 当前成本：16 个 forge:stone；
- 当前效果：解锁 frostedheart:copper_pro_pick 的输出匹配配方；
- 当前真实机制：ProspectorPick 与 GeologistsHammer 扫描玩家点击位置附近的 FHTags.Blocks.ORES。

因此首版 Finding 不能宣称已经存在真实地层化学。它只声称：

> 在已测试的距离和工具条件下，规范取样与敲击得到的勘探迹象，能够帮助判断附近是否存在矿物。

#### 最初想法从哪里来

至少提供三条等价入口，不要求铜、不要求相邻，也不要求玩家先知道自己正在做探矿研究：

1. 玩家把任意一条 `forge:stone` 方块观察与任意一条 `forge:ores` 方块观察钉在一起；
2. 矿场居民提交一条矿石或石材观察，和玩家已有的另一类记录组成同一最小组合；
3. 有相关背景的难民主动给出证词或直接传授这个 Idea，也可以直接传授外来 Finding。

第一条数据包 rule 只匹配两个 subject/facet，不读取坐标、区段、天气、时间、温度或封存矿物计数。两条记录可以来自不同地点、不同时间，钉入顺序任意，并允许旁边存在无关记录。完成一次整理纸牌后直接记下尚未证实的 Idea：

> 普通岩石中的勘探迹象也许能提示附近矿物，而不必先看见矿石本身。

这只是从“石头与矿物同时值得比较”得到的研究方向，不是系统提前证明了结论。远处对照、重复采样和统计吻合不属于这个初级 topic；以后真正需要控制变量的进阶地质研究可以另写自己的 `IdeaRule` 与方法。

#### 研究玩法

这项研究首版只安排一次轻量理论整理，不要求实验室、居民计算或再次外出：

1. Idea 工作页显示“整理石材可能保留哪些与矿物有关的可辨认迹象”；
2. 玩家在绘图台使用形成 Idea 的同一条石头记录和同一条矿石记录，完成一次短理论卡；
3. 理论卡生成 `RockAndOreReviewArtifact`，只说明这组观察足以尝试把矿迹做成可重复使用的工具反馈；
4. 玩家审阅后即可接受 Finding 与 Design。

它没有 `MATCH / NO_MATCH / INSUFFICIENT`，没有“增加另一份独立样本”，也不编译“补露头、补邻近岩样、补远处对照”行动卡。实现仍从 ProspectorPick / GeologistsHammer 抽出只读 `OreProspectingModel`，让 Finding 后的粗略解释和探矿工具的详细反馈读取同一附近矿物事实；这个封存事实不参与 IdeaRule，也不能另写一个只为研究服务的假扫描。

#### 直接结果

- Finding：frostedheart:prospecting_signs_indicate_nearby_ore；
- KnowledgeProjection：地质档案、样点记录解释、矿场居民更明确的勘探报告；
- 附加 Design：铜探矿镐的精确配方 ID，保留当前旧研究的实际回报。

若难民直接给 Finding，玩家可以立即看懂相应记录；若只给一张石材或矿石观察，仍需另一类观察才能形成这个 Idea。Finding 和铜探矿镐 Design 是同一研究的两个并列结果，不是 Finding 自动变成配方。

### B. Construction：简易高炉

#### 当前基础

- 旧 ID：blast_furnace；
- 当前线索：纸牌局、4 个因瓦锭、1 个耐火砖；
- 当前成本：4 个 fire_clay_ball；
- 当前效果：高炉砖 recipe + IE blast_furnace multiblock formation。

#### 最初想法从哪里来

1. 玩家得到耐火砖并记录它在普通炉火中的表现；
2. 玩家需要处理因瓦或更高温材料，但现有炉具能力不足；
3. 铁匠/冶炼工居民提交高温冶炼需求；
4. 有冶炼背景的难民可以直接给“怎样垒简易高炉”的 Construction，或给一份缺少高炉砖配方的残页。

证据板把“高温需求”“耐火砖”“火黏土”连起来，生成 Idea：

> 用耐火材料围出更集中的炉膛，也许能建立一座简易高炉。

#### 研究玩法

1. 绘图台先形成一张材料试验建议；
2. 玩家在实验台旁放置加热位和样品架，放入耐火砖样品、4 份火黏土和燃料；
3. 玩家短时值守，或让一名冶炼工居民工作一个班次；
4. 加热 Protocol 记录真实供热是否持续、样品是否放对、耗材是否完整；内部规格和结构推导交给居民计算；
5. 结果卡写“这组材料可用于高温炉体”或“供热中断，暂时无法判断”；
6. 玩家接受结果后一次取得高炉砖 Design 与简易高炉 Construction。

首版并不声称 FrostedHeart 已有通用材料耐久模拟。这是一个作者定义的工程材料试验，使用真实样品、热源、时间和耗材；若以后实现材料退化接口，再由同一 Protocol provider 读取更丰富的世界事实。

#### 直接结果

- Design：高炉砖精确 recipe ID；
- Construction：immersiveengineering:multiblocks/blast_furnace 的 formation；
- 不新增“操作高炉”的限制，因为当前旧研究也没有 use/operation effect。

如果难民只给 Construction，玩家会知道怎样成型，但仍可能缺少高炉砖 Design。这种缺口来自知识来源确实残缺，不是系统强制把一项完整研究拆碎。

### C. Design：进阶真菌养殖

#### 当前基础

- 旧 ID：incubator；
- 当前线索：纸牌局、生石灰；
- 当前投入：一份蘑菇和一份 straw briquette；
- 当前效果：培养箱 recipe + 一份棕色蘑菇菌床奖励；
- 当前机器：IncubatorTileEntity 和 IncubateRecipe 已存在。

#### 最初想法从哪里来

1. 玩家多次携带或更换菌床，记录“能养殖，但占空间且补热麻烦”；
2. 得到生石灰后，记录它能让培养过程脱离随身菌床，成为固定设备的燃料；
3. 城镇食物岗位提交真菌供应不稳定的报告；
4. 有真菌培养经验的难民可以直接给培养箱 Design，或给“生石灰补热”的实验建议。

证据板把“菌床”“固定空间”“生石灰”连起来，生成 Idea：

> 把菌床、养料和补热固定在一个设备中，可能更适合持续培养。

#### 研究玩法

1. 玩家准备两份同类菌床记录：一份继续随身培养，一份放进实验台的固定培养位；
2. 固定培养位加入蘑菇、草料、水和生石灰，复用培养箱现有的输入、催化剂、水与燃料语义；
3. 实验空间检查样品位、供水、生石灰燃料和记录设施，不把环境温度伪装成当前培养箱已经使用的变量；
4. 玩家可以亲自完成短试验；研究所路线消耗一名实验员一个或两个班次、纸、水和生石灰；
5. CalculationProfile 把随身占用、培养时间、燃料/水消耗和有效产出整理成“适合固定设备 / 仍不稳定 / 样本中断”；
6. 玩家选择“画成培养箱设计”后得到配方。

首版 Protocol 可以复用 IncubateRecipe 的输入输出以及 IncubatorTileEntity 的生石灰、水、效率和时间语义。当前培养箱并不读取世界温度，MushroomBed 的随身培养则依赖玩家身体加热条件；两条路径必须如实分别采样，不能在菜单里伪造一套不存在的温区或成功率。实验结果是工程设计依据，不额外发布一条虚构真菌自然规律 Finding。

#### 直接结果

- Design：培养箱的精确 recipe ID；
- 普通奖励：沿用一份棕色蘑菇菌床；
- 不创建 Procedure，因为当前培养箱使用并未被旧 EffectUse 限制。

### D. Prototype：能量塔效率提升 1 → 受控进气原型

#### 当前基础

- 旧 ID：generator_efficiency_1；
- 当前线索：level 1 纸牌局、mechanical_bellows、铸铁或钢锅炉；
- 当前投入：2 个 cast_iron_burner、4 个 encased_fan、8 个铸铁板；
- 当前效果：team-wide generator_effi +10%；
- 当前文字已经明确描述“加装新的燃烧室和鼓风模块”。

这是一项最适合从全队数值改成实体 Prototype 的现有研究。

#### 最初想法从哪里来

1. 玩家在运行中的 T1 上记录“塔持续工作，但燃料消耗很快”；
2. 物流居民提交燃料库存下降报告；
3. 塔操作员提交装料与有效运行时间报告；
4. 玩家接触机械风箱、鼓风机或锅炉后，把它们与燃烧问题钉在一起；
5. 有锅炉/轮机背景的难民可以直接提出“稳定进气”Idea，甚至带来一件旧 Prototype。

证据板生成：

> 更稳定的进气和新的燃烧室，也许能让每份燃料维持更久。

#### 研究玩法

1. 讨论可提出“其实是断供”“超载工况不同”“进气不足”三类人话解释；
2. 计算任务把现有塔报告整理成基线，玩家不计算 process tick；
3. 玩家把现有旧研究的真实材料放入实验台待测部件路线；
4. 工程实验需要工作空间、动力、风扇/燃烧室装置和一名实验员，完成后产出一件有 serial 的受控进气 Prototype；
5. 玩家用通用研究升级工具打开 T1 的统一升级 GUI，把该原型安装到当前 active T1；
6. 塔正常运行时记录燃料装入和有效运行时间；断供或停机显示“暂时无法判断”，不清空已有记录；
7. 玩家可以保留安装、拆下返工或带着现场报告重新进入证据板。

Prototype 的 +0.1 是作者定义的工程效果，不是假装由玩家从数学中发现的自然常数。实验和试用验证的是材料齐备、装置可工作、实际运行没有被断供污染，以及效果是否在真实 host 上生效。

#### 直接结果

- Prototype：frostedheart:t1_controlled_draft_prototype；
- 安装后 InstalledContributionSnapshot 为该 T1 提供 generator_fuel_duration +0.1；
- 不创建 Design，不开放量产配方，不引入 DesignStandard；
- 以后如果确实需要可重复制造，再新增一个普通 Design 研究，直接解锁标准组件配方即可。

当前 GeneratorData 是每 team 一个 active tower，且直接读取 team-wide variant。MVP 明确只支持当前 active T1，先给它增加 incarnation、升级存储和 host-aware modifier resolver；不假装首版已经支持多塔。

### E. 复合边界案例：能量核心再利用

#### 当前基础

- 旧 ID：generator_T1；
- 当前线索：纸牌局、generator_brick、generator_amplifier_r1；
- 当前投入：4 个铜锭；
- 当前效果：generator_core_t1 recipe、energy_core 普通物品奖励、T1 multiblock formation。

#### 最初想法从哪里来

1. 飞船任务或残骸档案给出“能量核心已经失去原供能，但主体可能完整”的记录；
2. 玩家拿到或观察 generator_brick 与 amplifier；
3. 工程居民对核心做检查后提出替代供能 Idea；
4. 有飞船设备经验的难民可以直接给部分或全部知识。

#### 最小研究玩法

1. 证据板连接“旧核心”“替代供能”“周围供热用途”；
2. 居民或机械计算器完成一次接线/材料核对；
3. 实验台使用 component 槽做低功率检查，玩家放入铜材、砖样和放大器，周围必须有动力与记录装置；
4. 玩家审阅“核心可被当前材料重新接入 / 输入不稳定 / 部件不匹配”；
5. 接受后一次取得配方和成型方法。

为了让器物来源真正成立，内容重制时优先让飞船任务交付一件可检查的受损核心或稳定器物记录，而不是继续只在研究完成后凭空领取；但这属于该任务内容迁移，不是新成果类型。

#### 直接结果

- Design：generator_core_t1 的精确 recipe ID；
- Construction：frostedheart:multiblocks/generator 的 formation；
- 普通奖励：energy_core；
- energy_core 本身不叫 Prototype，因为它没有 serial、兼容升级位和安装后局部贡献。

这个案例证明：Design 和 Construction 不需要再被拆成规格、制造法、施工法和应用规则。一项完整研究可以直接同时给出两个权限。

## 通用研究升级 GUI

Prototype 不要求修改每台机器原 GUI。

### 玩家交互

1. 玩家对兼容设备使用 frostedresearch:research_upgrade_tool；
2. 服务端把普通 BlockEntity 或 IE 多方块从属块解析到稳定 host；
3. 打开统一 ResearchUpgradeMenu；
4. 菜单显示该 host 的研究升级位、当前安装 Prototype 和兼容背包物品；
5. 玩家确认安装或拆除；
6. 真实物品保存在 host 的升级存储中，team registry 只维护 serial 到 placement 的索引。

普通设备可以实现 ResearchUpgradeableDevice；IE/第三方设备通过 ResearchUpgradeHostAdapter 接入。host 身份至少包含 host type、主节点 GlobalPos 和 incarnation UUID，不能只用坐标。

MVP 只需要两个通用物品：

| 物品 | 用途 |
|---|---|
| research_upgrade_tool | 打开兼容 host 的统一 GUI |
| upgrade_prototype | 保存 profile、serial、team、材料和视觉信息的动态原型 |

Prototype 外观使用同一基础材质、材料 tint 和白名单 overlay/profile，不为每个设计注册一个新 Item。

安装、拆除和效果查询都在服务端完成。客户端只提交菜单动作、背包槽和 expected revision，不提交 ItemStack、效果值或 host 事实。

## 数据包作者契约

### topic 是创作 bundle，不是运行时流程所有者

普通作者仍应能把一项研究写在一份 topic JSON 中；这是维护便利，不代表运行时把记录、方法或人物知识私有化给该 topic。loader 必须把内联定义编译进以下公共注册面：

| 注册面 | 拥有什么 | 可否跨 topic 复用 |
|---|---|---|
| Observation field / provider | subject 支持、typed context codec、仪器要求、采样、语义去重、安全摘要 | 必须可以 |
| IdeaRule | 对公开 subject、facet 与已保留 `ObservationContext` 子集的组合匹配、Idea 文本 | 必须可以 |
| ResearchTopic + NeedResolver | Idea 后的问题身份、当前缺口、接受条件 | 可以引用公共记录与制品 |
| MethodContract / Protocol handler | 输入角色、现场/讨论/计算/实验动作、typed artifact | 必须可以 |
| KnowledgePackage / Results | 人物背景可提供的 Offer，以及五类固定成果 | 必须可以 |

首版推荐路径为：

~~~text
data/<namespace>/frostedresearch/observations/<path>.json
data/<namespace>/frostedresearch/methods/<path>.json
data/<namespace>/frostedresearch/knowledge_packages/<path>.json
data/<namespace>/frostedresearch/topics/<path>.json
data/<namespace>/frostedresearch/prototypes/<path>.json
~~~

大多数内容作者只写 topic，并引用已有 observation、method 与 package；需要新增世界采样、跨 topic 方法或人物背景时才拆文件。内联 section 也要安装成有稳定 ID 的公共定义，不能变成 topic 私有 Java 分支。

每一种注册类型都由真实 handler 对象提供 typed spec codec、聚合校验和运行时 `match / compile / execute / resolve`。`ResearchWorkflowRegistry` 不能只是允许通过 loader 的 ID 集合；topic 引用的同一个 handler 必须在运行时被调用。

### 最小 topic 形状（返工方向）

以下只固定运行时边界与可见性，不冻结开发期 format 号或最终字段拼写：

~~~json
{
  "legacy": {
    "id": "geology_understanding",
    "mode": "coexist"
  },
  "presentation": {
    "archive": "frostedheart.topic.geology_understanding",
    "icon": "frostedheart:copper_pro_pick"
  },
  "idea_rules": [
    {
      "id": "frostedheart:rock_and_ore_signs",
      "idea": "frostedheart.idea.rock_and_ore_question",
      "any_of": [
        {
          "observations": {
            "stone": {
              "subject": { "block_tag": "forge:stone" }
            },
            "ore": {
              "subject": { "block_tag": "forge:ores" }
            }
          }
        }
      ],
      "extra_records": "ignore",
      "order": "any"
    }
  ],
  "research": {
    "need_resolver": {
      "type": "frostedheart:rock_and_ore_review",
      "reuse_bound_records": true
    },
    "method_hints": [
      "frostedheart:review_rock_and_ore_notes"
    ]
  },
  "resolution": {
    "type": "frostedheart:reviewed_rock_and_ore_notes"
  },
  "results": [
    {
      "type": "finding",
      "id": "frostedheart:prospecting_signs_indicate_nearby_ore",
      "views": [
        "frostedheart:geology_archive",
        "frostedheart:prospecting_report_detail"
      ]
    },
    {
      "type": "design",
      "id": "frostedheart:copper_prospecting_pick",
      "recipes": [
        "the_winter_rescue:research/copper_pro_pick"
      ]
    }
  ]
}
~~~

一个 role 可以只约束 subject，也可以再约束自己真正需要的 context 子集。例如另一条 rule 可以要求 `entity_type=minecraft:sheep`、`time.period=evening` 和 biome tag，而不要求位置或天气。组合不得读取玩家没有保留的字段；需要比较两个 role 时，只能使用注册的类型化关系，如相等、不同、距离范围或数值范围，不能执行任意脚本。

`idea_rules[].idea` 只在纸牌完成后显示；`presentation.archive`、`results` 和其固定说明只在研究接受后进入客户端投影。服务端可以提前加载和校验完整定义，但不能把这些字段作为 Idea 前的 UI 或网络摘要。

地质 observation profile 可以引用 `OreProspectingModel` 作为封存 sampler，人物 package 可以引用 `prospecting_experience`，两者都在 topic 外独立注册。铜探矿镐现有稳定配方 ID 为 `the_winter_rescue:research/copper_pro_pick`；同数组中的其他旧配方 ID 不因本次返工被重排。

当前尚在快速迭代，旧的 `idea_sources / inspiration / protocols / resolution` 开发期形状可以直接替换，不为每次重构升 schema 或扩写大型兼容矩阵。必须保留的是已经有意义的观察、Idea、团队成果、人物初始化结果、稳定 recipe ID 与 last-known-good reload；被本轮明确否决的 `pendingCandidates`、两地点比较和临时 action 形状不享有发布后兼容承诺。等 Phase 2 玩家循环验收通过后再冻结首个公开 format；此后只有真实不兼容变更才升号。

### 另外三种结果写法

简易高炉：

~~~json
{
  "results": [
    {
      "type": "design",
      "id": "frostedheart:blast_brick",
      "recipes": [
        "the_winter_rescue:research/blastbrick"
      ]
    },
    {
      "type": "construction",
      "id": "frostedheart:form_simple_blast_furnace",
      "multiblocks": [
        "immersiveengineering:multiblocks/blast_furnace"
      ]
    }
  ]
}
~~~

进阶真菌养殖：

~~~json
{
  "results": [
    {
      "type": "design",
      "id": "frostedheart:incubator",
      "recipes": [
        "the_winter_rescue:research/incubator"
      ]
    }
  ],
  "rewards": [
    {
      "item": "frostedheart:straw_briquette_brown_mushroom",
      "count": 1
    }
  ]
}
~~~

能量塔效率提升：

~~~json
{
  "results": [
    {
      "type": "prototype",
      "profile": "frostedheart:t1_controlled_draft"
    }
  ]
}
~~~

### Prototype profile

~~~json
{
  "format": 1,
  "host": "frostedheart:generator_t1",
  "socket": "frostedheart:air_control",
  "bom": [
    {
      "item": "steampowered:cast_iron_burner",
      "count": 2
    },
    {
      "item": "create:encased_fan",
      "count": 4
    },
    {
      "tag": "forge:plates/cast_iron",
      "count": 8
    }
  ],
  "visual": {
    "profile": "frostedheart:ducted_fan",
    "material": "frostedheart:cast_iron"
  },
  "installed_contributions": [
    {
      "type": "frostedheart:generator_fuel_duration",
      "add": 0.1,
      "exclusive_family": "frostedheart:t1_efficiency_stage_1"
    }
  ]
}
~~~

数值由注册的 UpgradeEffectHandler 验证单位和 host；普通作者不能写 generator_effi 之类旧 CompoundTag 键。

### 三层作者职责

| 角色 | 负责什么 |
|---|---|
| 普通内容作者 | 组合已注册 observation field/role、IdeaRule、MethodContract、Need/resolution、五种 result、普通奖励、材料和文本 |
| 资源/叙事作者 | 翻译、证据牌面、难民背景、遗迹文本、报告文本、Prototype overlay |
| Java/集成作者 | 实现通用 typed handler：observation field/provider、Idea rule、method、need/resolution、apparatus、Finding view、升级 host/effect；不得为单个 topic 在核心服务中复写整条流程 |

KubeJS 只生成同一 JSON 或在数据生成阶段注册同结构候选；它不能直接给团队添加 Finding、权限或数值。

### 校验与 reload

catalog reload 一次性校验：

- result ID 与引用是否存在；
- observation field/kind 的 public/sealed codec、仪器要求、去重策略与安全投影是否齐全；
- IdeaRule 是否只读取公开且可保留的 context/facet，`any_of` 是否非空，角色、字段和类型化关系是否可满足；
- MethodContract 的输入角色、artifact kind 与 Need 类型是否兼容；
- provider、rule、method、need/resolution、knowledge package 引用是否对应真实可执行 handler，而不只是白名单 ID；
- Design 是否只引用明确 recipe ID；
- Construction 的 multiblock 是否存在且有服务端拦截点；
- Procedure 的 usable block 是否存在且有服务端拦截点；
- Finding 的 view handler 是否存在；
- Prototype 的 host、socket、BOM、effect handler、metric 和单位是否存在；
- experiment 的槽位数是否超过固定上限；
- legacy raw ID 是否明确；
- 翻译和图标是否缺失。

候选有错误时保留上一份可运行 catalog，并一次显示全部诊断。reload 不修改已生成 Prototype 的 serial、材料或贡献；实体原型引用它制造时冻结的 profile revision。

topic schema 禁止 research progress、points、team/player UUID、任意 Java/NBT path、任意公式脚本、直接 variant key、客户端翻译作为逻辑条件，以及按输出物模糊展开新配方。研究笔记固定的采集时长与当前 `CaptureSession` 进度是交互状态，不是 topic 研究进度，因此不受这条限制。

## 后端工程架构

### 总图

~~~mermaid
flowchart TB
    S["世界方块或实体"] --> O["CaptureSession → ObservationContext"]
    Q["难民 / 居民 / 任务 / 器物"] --> P["KnowledgePackageProvider"]
    O --> X["ObservationDraft → KnowledgeIngress"]
    P --> X
    X --> G["TeamKnowledgeData：topic-free 记录、Idea、工作制品、四类团队成果 ID"]
    G --> B["证据板：玩家选择记录"]
    B --> E["EvidenceRelation：仅用于牌面解释"]
    B --> R["隐藏 IdeaRule：直接匹配上下文组合"]
    R --> C["InspirationSession / 纸牌"]
    C -->|"完成；多匹配时选一个"| A["IdeaRecord"]
    A --> N["NeedResolver + MethodContract + ActionResolver"]
    N --> W["研究工作页"]
    W --> L["现场 / 讨论 / 计算 / 实验 Protocol"]
    L --> G
    G --> J["KnowledgeLabProjection：分页完整档案"]
    G --> K["KnowledgeProjection：轻量常驻提示"]
    G --> T["TechnologyAccessProjection"]
    Z["Prototype Item + host storage"] --> I["InstalledContributionSnapshot"]
    T --> V["配方 / JEI / 多方块 / 方块使用"]
    J --> Y["知识实验室：观察 / 想法与研究 / 成果"]
    K --> U["笔记 / HUD / 居民报告 / 对话"]
    I --> H["具体设备运行模型"]
~~~

### 核心数据

~~~text
TeamKnowledgeData
  observations              // subject + retained ObservationContext + public/sealed typed facts + provenance
  ideas
  workOrders
  workArtifacts             // protocol ID + typed role binding + outcome，不固定为地质比较
  acquiredFindingIds
  acquiredDesignIds
  acquiredConstructionIds
  acquiredProcedureIds
  prototypePlacementIndex

DrawingDeskSession
  pinnedRecordIds
  intent
  gamePurpose
  activeInspiration?
    matchedRuleIds
    boundRecordIds
    gameState
    resourcesCharged

NotebookCaptureSession      // 玩家短时服务端状态，不写入团队知识
  targetRef / startedAt / selectedFieldIds / instrumentIds
  progress / cancellationReason

ObservationDraft           // 完成采样后等待玩家保存或丢弃
  subjectRef / capturedContext / provenance

PersonKnowledgeOverlay
  initialized
  backgroundIds
  knowledgePackageIds
  disclosedOfferIds

Prototype 物理事实
  inventory ItemStack 或 host upgrade storage
  profileRevision
  serial
  ownerTeam
  placement
~~~

Prototype 不以 acquiredPrototypeIds 作为全队知识保存。实体丢失、拆除或安装都以真实 ItemStack / host 存储为准，team index 只用于定位和对账。

实验高频 trace 独立分段存储；TeamKnowledgeData 只保存 run 元数据和 segment refs。实验台保存 inventory、active run ID 和局部 checkpoint，不保存第二份完整 Run。

`KnowledgeRecord`、`ActionCard`、`WorkArtifact` 和 `FindingView` 都采用注册 type ID + typed payload；核心层不得包含 `COPPER_OUTCROP`、`ROCK_SAMPLE`、`RECORD_OUTCROP`、nearby/control 专用字段或 `Trace.PRESENT/ABSENT`。地质插件可以定义这些 payload 的内容与粗略矿迹 view fragment。

### 快速迭代期的保留边界

当前是快速迭代阶段，不为每次字段调整升 schema，也不为已经被客户端否决的原型形状建造大型迁移工程。本轮优先把行为做对；只有以下有价值状态需要保留：

- 已归档 observation 的 UUID、subject、可恢复公开 context、observer 与来源时间；旧地质记录可在读取时映射为新的 subject/context，但不要求保留旧专用 enum 作为公共 API；
- `OreProspectingModel.Snapshot` 继续作为 sealed fact 持久化且永不入网，本轮不为了包装它而破坏已有世界数据；
- 已经明确记下的 Idea、四类已取得结果和仍能指向真实记录的桌面钉位；旧 `FieldComparisonArtifact`、pending candidate 与被否决的两地点会话可以丢弃、忽略或成为不可见 orphan，不为其增加专门迁移测试；
- 已初始化为空或已有 `prospecting_experience` 的人物 overlay 不重抽，新 package 只扩展 ID 集合；
- 缺少 provider、rule、topic 或 package 的有价值历史对象保留为可恢复 orphan，reload 后重新挂接。

是否继续写 schema 2 由最小实现成本决定，不把 schema 号当作功能里程碑。等新的观察—Idea—研究循环通过客户端验收后，再冻结面向内容包和长期存档的公开格式及兼容矩阵。

### 服务边界

所有写操作统一经过 TeamResearchService：

~~~text
BeginObservationCapture
TickObservationCapture
CancelObservationCapture
CompleteObservationCapture
ArchiveObservationDraft
DiscardObservationDraft
ArchiveObservation
AcceptKnowledgeOffer
PinEvidence
SetBoardIntent
StartInspirationSession
ResumeInspirationSession
CancelInspirationSession
FinishInspirationSession
RecordIdea
CommissionDiscussion
CommissionCalculation
PrepareExperiment
StartExperiment
SealExperiment
AttachWorkArtifact
AcceptFinding
AcceptDesign
AcceptConstruction
AcceptProcedure
FabricatePrototype
InstallPrototype
RemovePrototype
~~~

客户端只发送意图、稳定 ID、字段选择、team epoch 和 expected revision。服务端逐 tick 或在完成时重新检查目标、距离、维度、仪器、玩家、team、居民、实验台、物品、空间、host 和当前世界状态；客户端不能提交观察值、“实验成功”“解锁这个配方”或“原型有 +0.1”。

`TeamResearchService` 只负责编排、事务、幂等、权限与 revision；它通过 registry 分发给 observation field/provider、Idea rule、method、need/resolution 和 package handler。核心服务不得比较具体 topic/result ID、矿石 tag、空间距离或具体 artifact outcome。

TeamResearchManager 保留为兼容 facade，唯一命令处理器名称使用 TeamResearchService。

### 端口

~~~text
KnowledgeSourceProvider
KnowledgePackageProvider
KnowledgeLabProjectionService
ObservationFieldType
ObservationInstrumentProvider
ObservationKind
ObservationProvider
ObservationCaptureService
EvidenceRelationExtractor
IdeaRuleHandler
InspirationSessionService
NeedResolver
MethodContract
ProtocolHandler
WorkArtifactKind
ResolutionResolver
DiscussionProfile
CalculationProfile
ExperimentProfile
ResearchApparatusAdapter
FindingViewHandler<ViewFragment>
ResearchUpgradeHostAdapter
UpgradeEffectHandler
TechnologyAccessResolver
~~~

不实现 AssetCapabilityCompiler、ApplicationRule、ApplicationProfile、RuntimeApplicationSnapshot 或 AppliedProcedureBinding。

### 同步和性能

- team 切换时用共同 TeamContextEpoch 清空并安装 town/research/knowledge 快照；
- catalog revision 先于 team 状态；
- 客户端常驻只同步轻量 `KnowledgeProjection`、四类成果 ID/技术权限、Knowledge Lab counts 与 revision；完整 observation、Idea 和 work artifact 由打开 Knowledge Lab 后的 page/detail query 按需取得，不同步 sealed facts 或无界 trace；
- `KnowledgeLabProjection` 的搜索、筛选、排序只基于安全字段；每页上限 40，翻页不漏项或重复，team/catalog revision 变化时失效旧 cursor 与详情缓存；
- 当前玩家的 capture progress、draft 摘要和当前绘图台 active inspiration session 使用有界临时同步；切换知识实验室与纸牌页不会销毁 session，也不会再次扣纸墨；
- `KnowledgeSyncSnapshot` 中脱敏的 `TeamKnowledgeData` 与可见 `KnowledgeProjection` 分工保持明确，不能为修收件箱而把 sealed record 放回 network codec；
- 客户端安装放到独立 client-only handler，登录、断线、换世界和换 team 先 reset `ClientKnowledgeDataAPI`，再原子安装新 snapshot；
- 只有 active experiment 高频采样，普通机器和城镇只提交稀疏或聚合记录；
- 行动卡在知识变化时重算，可执行性在人员、材料或设施变化时局部刷新；
- Prototype 安装/拆除只失效目标 host 的 InstalledContributionSnapshot。

## 与当前源码和内容的边界

1. `DrawingDeskTileEntity` 当前把 `ResearchGame` 完成提交为 `MinigameClue`。V2 保留棋盘，新增 `finishInspirationSession` 直接写入所选 Idea；旧 research 继续走原 clue executor。
2. MechCalcTileEntity 当前生产 points。新路径只执行具体 CalculationWorkOrder；Create 缺席时居民路径仍可用。
3. ITownBuilding.CODEC 当前使用 `buildByNameWithLegacyInt()`：新存档按名称分派，新类型可插入任意位置；MVP 新增 researchInstitute 时只需验证名称 round-trip 与旧整数解码回归，不再维持虚假的新增顺序约束。
4. BuildingBlockScanner / ConfinedSpaceScanner 可作为封闭实验空间基础，但必须有 bounded scan、明确 interior anchor 和缓存；露天调查使用独立 field protocol。
5. Resident 当前没有私人知识字段；使用独立 PersonKnowledgeOverlay，并处理难民招募换 UUID。
6. ProspectorPick / GeologistsHammer 已有真实附近矿物扫描；岩石研究复用抽出的 OreProspectingModel，不虚构地层性质。
7. IncubatorTileEntity / IncubateRecipe 已存在；真菌试验尽量复用其输入输出语义和温度接口。
8. GeneratorData 当前每 team 只有一个 active T1，并直接读 team variant；Prototype 切片必须先增加 incarnation、host storage 和 host-aware resolver。
9. 当前 81 个旧 research 定义中有 128 个 recipe effect、27 个 multiblock、4 个 use 和 6 个 stats。V2 不在首版一次重制全部。
10. 旧 EffectCrafting 大多按输出物在 reload 时展开配方；新 Design 必须切换到稳定 recipe ID。

当前地质原型的以下位置是返工清单，不是已接受的公共接口：

| 当前耦合 | 返工目标 |
|---|---|
| `ResearchNotebookItem` 已能瞬间记录方块 | 方块/实体 `CaptureSession`、可选 context chips、40 tick 进度、draft 保存/丢弃与注册 instrument enrichment |
| `KnowledgeRecord` 的 context 仍以固定位置/时间/block state 字段为主 | typed `ObservationContext` field map + retained-field 边界 + provider dedup |
| `KnowledgeOffer` 只携带固定 topic/idea | Observation、Idea 与五类成果的 tagged payload，不以 topic 代替来源 |
| `TeamResearchService` 仍 import `OreProspectingModel` 并暴露 `ROCK_*`、地质比较 facade | 通用事务服务；地质逻辑只由 geology IdeaRule / method / resolver handler 调用 |
| `ResearchWorkflowRegistry` 已执行 handler，但 handler 契约仍围绕 `IdeaCandidate` / `FieldComparisonArtifact` | 直接 `IdeaRule` binding + 通用 typed `WorkArtifact`，不把比较协议当作唯一协议形状 |
| `DrawingDeskTileEntity` 仍以 candidate 结束页和页面强制回跳驱动 V2 | 保存可恢复 `InspirationSession`；允许实验室/纸牌自由往返；纸牌完成后直接写 Idea |
| `KnowledgeLabLayer` 打开即显示“岩石与矿迹”、Finding 和探矿行动 | Idea 前只有收件箱、关系、证据板和中性整理动作；之后按阶段披露 |
| action 按钮沿用密集斜纹背景、单行文本 | 可读的实底/边框状态、足够高度、换行或省略号 + tooltip；disabled 也保持对比度 |
| `FindingViewHandler` 只返回矿迹 `Trace` | 通用 typed view fragment；地质 handler 才产生粗略矿迹 annotation |
| `PersonKnowledgeOverlay` 初始化与 RPC 只认识探矿 | 数据驱动背景池、多个 package、领域中性入口与上下文追问 |

模块依赖方向必须是 FrostedHeart 的 geology 集成层依赖 FrostedResearch 通用 API；FrostedResearch 核心不得反向 import `OreProspectingModel` 或具体地质 ID。

## 旧研究兼容

未重制的旧研究继续使用旧 Research / Clue / Effect executor。兼容映射只回答既有权限，不伪造新的知识成果：

| 旧数据 | 新查询中的来源 | 不自动生成 |
|---|---|---|
| EffectCrafting | legacy recipe entitlement，与 Design recipe IDs 合并 | Design、Finding |
| EffectBuilding | legacy multiblock entitlement，与 Construction 合并 | Construction 资产、Finding |
| EffectUse | legacy usable-block entitlement，与 Procedure 合并 | Procedure 资产、完整机器操作知识 |
| EffectStats | LegacyTeamModifier，旧消费者迁移前继续兼容 | Prototype |
| EffectItem / experience / command | 旧 executor 按 effectData 幂等发放 | Prototype 或知识成果 |
| 直接 research-ID gate | 原 adapter 或显式 legacy entitlement | Finding |

规则：

1. 旧 effectData 仍是是否已经 grant 和物品奖励幂等的权威，不能仅凭 finished 重发；
2. 旧 variants、level、clueData、insight 和 visitedArea 原样保留；
3. legacy 权限可以进入 TechnologyAccessProjection，但不能满足新研究 resolver；
4. 旧 generator_effi 继续作为 legacy team modifier；人工重制后的受控进气 Prototype 使用同一 exclusive family 去重，不能叠成 +0.2；
5. 新旧来源同时存在时，UI 显示两条来源，失去一条不会关闭另一条；
6. 不自动把完成的旧 geology_understanding 伪造成新 Finding。若内容作者决定给旧玩家承认该知识，必须写显式 legacy migration，并标记来源；
7. 新 topic 可以 coexist 或 supersede 旧入口，但 supersede 不删除旧存档历史和已经领取的奖励。

五项首批内容的迁移目标：

| 旧研究 | V2 明确结果 |
|---|---|
| geology_understanding | Finding + 铜探矿镐 Design |
| blast_furnace | 高炉砖 Design + 高炉成型 Construction |
| incubator | 培养箱 Design + 原普通物品奖励 |
| generator_T1 | 核心 Design + T1 成型 Construction + 原普通物品奖励 |
| generator_efficiency_1 | 新档走实体 Prototype；旧档保留并去重 legacy +0.1 |

## 实施顺序

### Phase 0：纸面验证五项研究

- 为五项研究各画一张从 Idea 来源到结果卡的可点击流程；
- 验证五个结果词是否无需解释即可理解；
- 为每项列出现有 world fact、需要新增的 observation/provider 和不允许声称的机制；
- 冻结实验台准备屏的信息结构，不冻结复杂 schema。

验收：玩家能够复述每项研究为什么开始、下一步做什么、最终得到什么。

### Phase 1：五种结果和三类直接查询（已完成基础层）

- [x] 在独立 `frostedresearch:knowledge` TeamKnowledgeData 中加入 Finding、Design、Construction、Procedure 的 acquired ID；
- [x] 实现五种 result codec、最小 topic/profile datapack loader 和独立普通 rewards 定义；
- [x] 实现不可变单调 revision catalog、候选聚合诊断与 last-known-good reload；
- [x] 实现 KnowledgeProjection、TechnologyAccessProjection、managed universe、来源 trace 与最小四个查询；
- [x] Design 直接驱动 recipe ID；Construction 直接驱动 multiblock；Procedure 直接驱动 usable block；
- [x] 合并 EffectCrafting / EffectBuilding / EffectUse legacy entitlement，并从已完成且 effect 已 grant 的权威状态重建来源；
- [x] 实现全量 knowledge snapshot、登录/换队/grant/reload 同步以及 JEI 刷新；
- [x] 实现 `upgrade_prototype` 实体壳、冻结 profile revision/serial/owner team，以及管理员 grant/revoke/info 命令和可选在线玩家团队目标。

验收：用开发命令授予五种结果时，Finding 只改变信息，Design 只开放精确配方，Construction 只开放精确成型，Procedure 只开放精确方块使用，Prototype 每次制造一件新 serial 的实体；revoke 可移除四种团队成果及 orphan 历史，info 可检查定义与团队状态，命令可显式选择在线玩家所属团队。Phase 1 未新增正式 topic，具体内容纵切从 Phase 2 开始。

### Phase 2：ObservationContext、直接 IdeaRule 与地质初级纵切（第三次返工）

现状：收件箱同步、任意方块 fallback、五钉位证据板、V2 纸牌入口、通用人物 package、结果投影与正式配方/topic 资源已有原型，玩家也已经能从五条记录进入小游戏。这些基础保留。第二轮客户端实测同时否决了四个方面：active 游戏强制弹回纸牌、action 文本不可读、Knowledge Lab 不能浏览完整团队知识，以及“邻近岩样—远处对照—比较—增加样本”的初级地质流程。`pendingCandidates`、`FieldComparisonArtifact` 和当前地质 action 不再视为已接受接口。

本阶段不以 schema bump 或旧原型兼容矩阵为重心。先完成下面的玩家循环，再冻结公开数据格式。

#### Phase 2.0：重做 Knowledge Lab 全量档案与绘图台会话 UI

- [x] 建立 `KnowledgeLabLayout` 共用外壳，顶层固定为“观察档案 / 想法与研究 / 成果档案”及各自总数；当前三栏布局在宽/窄窗口共用同一套 render/hover/click 几何；
- [ ] 实现 `KnowledgeLabProjection` 的 page/detail query：每页最多 40 条、稳定 cursor/revision、服务端搜索与筛选、客户端分页缓存；列表动作只传稳定 UUID/ResourceLocation；
- [x] 观察页可遍历全部 observation，显示 retained context、时间、Finding annotation 和证据篮；不再截取首屏前 10/16 条；
- [x] 想法页可遍历全部 `OPEN / READY / RESOLVED / ORPHAN` Idea，并显示来源/证据计数、相关 artifact 与当前行动；跨页关联跳转仍待补；
- [x] 成果页可遍历全部 Finding、Design、Construction、Procedure ID，显示真实 target ID，所有 orphan 显示 fallback；可定位 Prototype projection 仍待 Phase 5 的实体索引；
- [x] 删除 active `V2_INSPIRATION` 时把 Knowledge Lab 自动切回 `MainGamePanel` 的 tick/render 路径；页面切换只改变视图，不改变 `DrawingDeskSession`；
- [x] `MainGamePanel` 左上角显示“正在整理证据 · N 条记录”，Knowledge Lab 的三个主分页都保留 active-session 横条和“返回纸牌”；观察页显示同一组只读钉位；
- [x] 纸墨只在 session 开始时原子扣除一次；往返页面、关闭并重开桌面、客户端重建 widget 和重开牌局都不再次扣除；
- [x] active session 中服务端拒绝修改钉位，Knowledge Lab 提供显式“取消本次整理”；资源返还说明仍待定稿；
- [x] action card 和“记下/审阅”按钮改为可读实底与清晰边框状态；空间不足时省略并在 hover 显示完整 tooltip；
- [ ] GUI scale 2、3、4 与 16:9/窄窗口下检查标题、关系、报告、按钮互不覆盖，disabled 文本仍清晰可读且所有可点击元素有 hover 变化。

验收：任意 observation、Idea、work artifact 和已取得结果都能从三个主分页到达，orphan 不消失；纸牌进行到一半时可反复进入实验室并浏览任意分页再返回，画面不闪回、牌局不丢失、资源不重复扣。

#### Phase 2.1：建立可扩展 ObservationContext

- [ ] 将观察拆成 `ResearchSubjectRef + ObservationCaptureMeta + ObservationContext + public/sealed facts`；`ObservationContext` 是 `FieldId → TypedContextValue`，并显式保存玩家选择的 `retainedFieldIds`；
- [x] 首版 subject 同时支持 block 与 living entity；通用 provider 提供 block state/entity type、exact position、biome、weather 和 time；
- [ ] 接入 Frosted Heart 权威世界字段：天气/气候事件读取 `WorldClimate#getClimate` 与白幕空间查询，方块温度读取 `WorldTemperature.block`；不从客户端画面或 tooltip 反推；
- [x] 首版工具门控已接土壤温度计：无工具记录位置、生物群系、时间、天气；`ENVIRONMENT` 组合只有携带温度计时才取得方块温度；独立 `ObservationInstrumentProvider` 注册面仍待抽出；
- [x] 玩家未保留的字段不进入公开 `contextFacts` 与网络 projection；服务端验证仍保留基础 dimension/position/target，但当前 Idea 规则只读公开 facet/context；
- [ ] dedup 由 subject、provider 策略和 retained context 共同决定；不同时间、天气、温度或玩家字段选择不能被错误吞并。

验收：同一模型能表达“记录一块石头”和“傍晚在苔原发现一只羊”；后者若没有选择位置/天气，客户端和 IdeaRule 都看不到这两项。

#### Phase 2.2：把研究笔记改成真实采集交互

- [ ] `NotebookCaptureProfile` 保存在笔记物品上；Shift+右键打开 context chips，玩家选择位置、生物群系、时间、天气及当前仪器可提供字段，目标身份始终保留；
- [x] 对方块或 living entity 长按使用默认 `40 ticks / 2 s`，HUD 显示记录状态与进度条；瞬间右击不再直接归档；
- [x] 提前松开、超出交互距离、切维度或实体失效时服务端取消，不产生半条记录；持续瞄准重验和工具中途移除仍待补；
- [ ] 完成 tick 由服务端同时采样各字段并形成 `ObservationDraft`，显示本次实际取得的 subject/context/measurement；玩家选择“保存记录”或“丢弃”；
- [x] 完成采集后进入团队收件箱并触发同步；重复观察反馈新增或合并，不谈任何隐藏 topic；draft 保存/丢弃 UI 仍待补；
- [ ] capture progress 是短时动作状态，不写成研究 points；断线或死亡直接取消未完成 capture，已形成 draft 按明确生命周期清理。

验收：方块和实体都经过可见进度；目标改变会可靠取消；玩家可以有意识地决定“这次观察保留哪些上下文”。

#### Phase 2.3：用直接 IdeaRule 取代双层候选流程

- [ ] `IdeaRule` 直接匹配公开 subject/facet 与 retained context；每条 rule 至少有一个 `ObservationCombination`，多个入口放在 `any_of`，任意一个满足即可；
- [ ] combination 可有一个或多个 observation role，只约束需要的 context 子集；顺序无关，默认忽略额外记录，并支持注册的相等、不同、距离与范围关系；
- [ ] `EvidenceRelation` 只生成中性牌面和解释文本，不持久化为准 Idea，也不成为所有 rule 的强制前置；
- [x] 单一 rule match 在完成纸牌后直接幂等创建/合并 `IdeaRecord`，不再显示“记下想法”作为第二道确认；如果同组记录命中多个 rule，仍在结束页选择一个 Idea；
- [ ] active `InspirationSession` 保存匹配 rule、实际 role binding、牌局状态和已扣资源；客户端只收到会话展示摘要，不收到未完成 Idea 或隐藏 topic 结果；
- [ ] 人物/文献直接提供 Idea 时继续跳过纸牌，不伪造 evidence match。

用两个 data-only fixture 证明泛化：

1. 任意石头 + 任意矿石 → 地质 Idea；
2. 羊 + `time.period=evening` + tundra biome tag → 一条非地质 Idea；相同羊记录缺任一 retained context 时不匹配。

#### Phase 2.4：简化地质初级研究

- [x] 将 `rock_and_ore_signs` 的现场入口改为任意 `forge:stone` observation + 任意 `forge:ores` observation；不要求铜、相邻、同区段、固定钉位数或 sealed scan；
- [x] 完成整理纸牌后直接得到同一个 Idea；删除这项 topic 的 nearby/control 角色、两地点 action、`MATCH / NO_MATCH / INSUFFICIENT` 完成门槛和“增加另一份独立样本”；旧 artifact 仍作为可读历史；
- [x] Idea 后只生成一个轻量理论行动 `review_rock_and_ore_notes`，复用已经绑定的两条记录，不要求玩家再次出门、重新钉证据或补仪器；
- [x] 理论完成后即可审阅并一次取得 `prospecting_signs_indicate_nearby_ore` Finding 与铜探矿镐 Design；Finding 继续只公开粗略矿迹，工具继续给即时详细反馈；
- [ ] `OreProspectingModel` 仅由 Frosted Heart geology provider/Finding view/工具使用；它可以提供 sealed fact，但 IdeaRule 不读取它，FrostedResearch 核心不引用它；
- [x] 探矿人物 package 可以直接提供同一 Idea；空人物和非地质人物继续使用领域中性对话。

验收：玩家只记录一块任意石头和一块任意矿石就能开始并完成初级地质研究；整个工作页不再出现邻近、对照、增加样本或比较吻合率。

#### Phase 2.5：实现与自动回归

- [ ] 单元测试聚焦 `ObservationContext` typed value、retained-field redaction、block/entity provider、instrument gating、dedup、`any_of`、顺序/额外记录容忍、直接 Idea 幂等和 Idea 来源合并；不为被否决的临时 schema 扩写迁移矩阵；
- [ ] capture 集成测试覆盖 40 tick 完成、提前松开、换目标、越距、维度变化、实体消失、仪器移除、draft 保存/丢弃与同步；
- [ ] session 测试覆盖实验室/纸牌自由往返、关闭重开恢复、取消、一次扣费、完成后直接 Idea、多个匹配选择、旧 `LEGACY_CLUE` 分流与多绘图台隔离；
- [ ] geology GameTest 走通“任意石头 + 任意矿石 → 纸牌 → Idea → 轻量理论 → Finding + Design”，并断言不生成旧 comparison artifact；
- [ ] 非地质 GameTest 用羊的时段/生物群系 rule 证明新增上下文字段和 rule 不需要地质 Java/UI 分支；
- [ ] packet 回归继续证明 client-only snapshot 安装、断线 reset、sealed facts 缺席与 active session 有界同步；
- [ ] Knowledge Lab 回归用超过 40 条 observation、多状态 Idea、无主 artifact、四类 acquired result、Prototype placement 和缺失 catalog 定义证明分页、搜索、关联跳转与 orphan fallback；
- [ ] 客户端手测 GUI scale 2/3/4、三大主分页、block/entity capture、context chips、进度取消、纸牌往返、按钮 hover/可读性、地质完整循环与旧研究纸牌。

主仓库在功能稳定后串行执行 focused tests、完整 `test`、`runGameTestServer`、无 FTB/JEI GameTests、`build`、旧 81 项 catalog preflight 与 `git diff --check`。伴生仓库只有在其镜像数据再次改变时才单独执行 JSON、脚本与 diff 校验。Phase 2 只有在上述自动回归与第三轮客户端验收都通过后才改为 completed。

### Phase 3：最小研究所、实验台、高炉与真菌

- 增加 researchInstitute 的最小 staffing、讨论/计算/实验队列和一人一日一单；
- 实现 experiment_table、固定槽位、封闭空间检查和基础加热/记录 apparatus；
- 实现高炉材料试验和真菌培养试验；
- 打通 Design 配方与 Construction formation 的真实执行端；
- 保留 Incubator 普通物品奖励。

验收：两项研究都消耗实际样品、设施、居民或玩家时间；高炉一次给两个结果，培养箱一次给配方，玩家不填写公式。

### Phase 4：能量核心复合研究

- 把任务/飞船档案接成稳定 KnowledgeOffer；
- 实现核心低功率台架和居民材料/接线计算；
- 给 generator_core_t1 配方建立稳定 ID；
- 一次授予 Design + Construction + 普通 energy_core 奖励；
- 不把普通核心标成 Prototype。

验收：该研究完整保留当前配方、成型和物品回报，同时不引入标准化层。

### Phase 5：实体 Prototype 与塔效率

- 实现 research_upgrade_tool、统一菜单、动态 prototype item 和 T1 host adapter；
- 给当前 active T1 增加 incarnation、升级存储和必要燃料运行摘要；
- 实现受控进气 Prototype 的实验制造、安装、拆除和局部 +0.1；
- 把 generator consumer 切到 host-aware resolver，并与旧 variant 原子去重；
- 不修改 Generator 原 GUI，不开放量产配方。

验收：同一 team 只有安装着该 serial 的 active T1 得到效果；拆下后效果消失；旧存档不会得到双份加成。

### Phase 6：内容作者工具、迁移和文档

- 完成 topic/prototype schema、datapack reload、validate 和 dump template；
- 迁移 JEI、实际 recipe 执行、IE formation 与 RightClickBlock 到统一查询；
- 再选择下一批旧 research 做人工重制，不自动猜 Finding；
- 更新 docs/research、相关 town/climate 文档和 diary。

## 验收标准

### 玩家体验

- Knowledge Lab 的观察、想法与研究、成果三个主分页能够遍历团队的全部安全知识；分页、筛选和搜索不会让超出首屏的条目永久不可达；
- 每条 observation、Idea、work artifact、Finding、Design、Construction、Procedure 和可定位 Prototype 都有详情入口；缺失定义的 orphan 显示保留状态与 raw ID，而不是消失；
- 研究笔记可以用同一套 `CaptureSession` 记录任意可观察方块或实体；长按时有目标、字段和进度反馈，取消时不产生半条记录；
- 玩家可以选择是否保留位置、生物群系、时间、天气与仪器字段；没有匹配 topic 的记录仍真实出现在收件箱；
- Idea 前只显示记录和中性关系，不显示 topic 名、目标成果、专用补件清单或锁定结论；
- IdeaRule 只匹配玩家保留的 context 子集；pin 顺序、少量无关记录和玩家/人物/报告替代来源不会把有效组合变成固定线性配方；
- 完成整理纸牌后直接记下匹配 Idea，不再经过持久 Candidate 或第二次“记下想法”门槛；
- 纸牌与知识实验室可以在 active session 中自由往返；纸牌左上角说明正在整理哪组证据，往返不重置、不重扣纸墨；
- 按钮和行动卡在普通、悬浮、按下、禁用状态都可读，长文本不会被斜纹背景遮住或与其他文本重叠；
- 初级地质 Idea 只需要任意石头 + 任意矿石；后续不要求邻近、对照、增加样本或 `MATCH`；
- Idea 可以由最小但有意义的观察组合出现；对照、重复和实验只属于明确需要它们的后续研究；
- 人物无上下文对话领域中性，只有人物自己拥有或玩家明确追问的知识才进入具体领域；
- 玩家能从亲历、难民、居民、遗迹或任务开始研究；
- 证据板帮助产生 Idea，但直接知识不被迫再玩一次小游戏；
- 工作页最多显示三个下一步；
- 讨论、计算和实验都占用真实资源和时间；
- 实验需要真实空间、装置和样品，但玩家不填写数学表；
- Finding 有明确的信息反馈；
- Design 立即开放明确配方；
- Construction 立即开放明确成型；
- Procedure 立即开放明确方块使用；
- Prototype 是实体，只有安装后影响目标设备；
- 简易高炉和能量核心再利用可以一次给多个结果；
- 固定 topic 档案与成果说明只在研究接受后公开。

### 内容创作

- 普通作者可以用一份 topic bundle 组合 observation fields/roles、`IdeaRule.any_of`、MethodContract、Need/resolution 和 results；loader 会把它们安装到公共注册面；
- 普通作者不需要理解能力清单、应用规则或标准化状态机；
- observation field/provider、IdeaRule、method 与 knowledge package 可以跨 topic 复用；
- rule 可以只引用任意 context 子集；增加新 field 或新 `any_of` 组合不要求修改通用证据板 UI；
- 一个非地质 data-only fixture 不修改通用 Java 或 UI 就能走完 observation → Idea → artifact → result；
- 新 Design 只引用稳定 recipe ID；
- 新 Construction 只引用实际存在且已接服务端检查的 multiblock；
- 新 Procedure 只引用实际存在且已接服务端检查的 usable block；
- Prototype profile 明确 BOM、host、socket、外观和类型化安装效果；
- 所有错误在 reload 时一次给出，上一份 catalog 继续运行。

### 工程

- 世界记录、团队成果、实验 trace 和 Prototype 物理事实各有唯一权威；
- `KnowledgeLabProjection` 从权威数据重建三页 counts、分页 index 和详情；所有关联和动作使用稳定 ID，revision 变化会废弃旧 cursor/缓存；
- 四类 acquired result 的每个 ID 都生成 archive entry，Prototype 只从真实 placement/物品/host 投影；orphan fallback 不授予或伪造任何能力；
- 观察值、天气、温度、目标与完成 tick 全部由服务端采样；客户端只选择字段与发送交互意图；
- 未保留的 context 和 sealed facts 都不得进入网络、牌面或 IdeaRule；
- 服务端记录成功后，同一轻量 snapshot 必须推进 Knowledge Lab count/revision 或发送明确失效通知；已打开的观察页重新查询后必须看到新记录，sealed facts 始终缺席；
- packet 的 client install、登录/断线 reset、team epoch 和 catalog revision 有集成回归；
- 快速迭代期不以 schema 号或被否决原型的兼容矩阵作为完成条件；只保留有价值的 observation、Idea、成果、人物初始化与稳定 ID；
- registry 的同一 typed handler 同时负责 codec/校验和运行时执行，不允许“ID 白名单通过、Java 另写流程”；
- FrostedResearch 核心不引用正式地质 ID、铜标签或 `OreProspectingModel`；
- projection 可以从结果 ID、legacy 权限和 host 实体重建；
- 旧 effectData 不重复发奖；
- JEI 与真实执行使用同一 recipe query；
- multiblock formation 与 block use 分别使用 Construction / Procedure query，但共享同一 TechnologyAccessResolver；
- 新设备数值不写 team-wide variant；
- catalog reload 不改变已经存在的 Prototype serial 和效果 revision。

## 明确不做

- 不在 MVP 中实现 DesignStandard；
- 不实现 CapabilityManifest、ApplicationRule 或多层资格编译；
- 不把制造配方拆成 Design + 制造 Procedure；
- 不把 Construction 再拆成结构规格 + 施工 Procedure；
- 不让 Procedure 提供运行数值；
- 不把普通 item reward 叫 Prototype；
- 不要求每项研究都有讨论、计算和实验三步；
- 不要求每个 Idea 都先建立对照、重复样本或统计报告；
- 不把 `EvidenceRelation` 或 `IdeaCandidate` 做成所有 Idea 必经的持久知识层；
- 不允许 IdeaRule 使用玩家没有选择保留的隐藏位置、时间、天气、温度或其他 context；
- 不再用瞬间点击完成研究笔记观察；
- 不要求玩家填写公式、变量、误差或实验计划；
- 不为每台设备修改原 GUI；
- 不在首发继续扩写白幕、水相变、植物温区或未实现的世界观机制；
- 不一次重制全部 81 项旧研究。

## Outcome

Status: in-progress（Phase 1 completed；Phase 2 第三次返工的可玩主链已实现，等待完整自动回归与客户端验收；Phase 3–6 paused）。

Phase 1 已完成五类成果基础层：Finding、Design、Construction、Procedure、Prototype；其中 Construction 独占多方块成型权，Procedure 独占方块右键使用权。

历史实现记录（保留，但不代表本轮验收）：地质原型增加了 `research_notebook`、`OreProspectingModel`、`KNOWLEDGE_LAB`、五个持久钉位、inspiration 会话、岩石比较、Finding view、schema 2 团队记录、`PersonKnowledgeOverlay`，以及内置 `the_winter_rescue:geology_understanding` 和稳定研究笔记/铜探矿镐配方；旧研究使用 `coexist`。首轮客户端崩溃和背包重叠已经修复。

第一轮客户端验收曾推翻“纵切完成”的结论：服务器已保存 1 条露头和 3 条岩样，但 safe referent 中断知识 snapshot，无效 `smoke_view` 又使 catalog revision 保持 0；同时旧实现把探矿流程写进了核心服务和 UI。这些同步与入口问题后来已经修复。

后续客户端已经能钉入五条记录并进入整理纸牌，证明收件箱、钉位和小游戏入口可工作；但 active 纸牌会把实验室页面强制弹回，action 文本与斜纹背景冲突，而且当前地质内容仍要求邻近、对照、比较和追加样本。该原型因此仍未通过玩家体验验收。

第三次返工已落下可玩主链：`KnowledgeLabProjection` 和三页档案能够遍历安全 observation、Idea、artifact 与四类团队成果/orphan；active 纸牌和实验室可自由往返并共用实底按钮；研究笔记对方块/living entity 执行 40 tick 采集，公开 context 支持位置、时间段、生物群系、天气、状态和温度门控；单一纸牌匹配完成后直接记录 Idea；地质入口已简化为“任意石头 + 任意矿石”，后续只有一次轻量理论再取得 Finding + Design。尚未完成的是服务端 cursor/search 分页、任意字段 chips/draft 保存页、正式 typed `IdeaRule.any_of` 与非地质羊 fixture，以及更完整的 capture/session 集成测试。居民计算工单仍属于 Phase 3。
