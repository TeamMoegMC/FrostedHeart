# 玩家可交互的“发现与创新”研究系统计划

- Time: `2026-08-25 10:30:52 +0800`
- Last revised: `2026-08-25 12:28:19 +0800`
- Authors: `Codex（OpenAI，系统与玩法架构）；项目所有者提供目标与取舍`
- Status: `draft，已按项目所有者反馈收缩玩法范围，等待垂直切片验证`
- Scope: `FrostedResearch、绘图台小游戏、城镇居民与研究所、通用研究升级、数据包创作、旧研究兼容`
- Related: [`discussion/research_conversation.md`](../discussion/research_conversation.md), [`docs/research/README.md`](../docs/research/README.md), [`docs/research/gameplay-and-integrations.md`](../docs/research/gameplay-and-integrations.md), [`docs/research/research-ui.md`](../docs/research/research-ui.md)

## 本次收缩的结论

新研究系统不再试图把完整科学方法、实验设计和工程认证都变成玩家表单。玩家不是研究员模拟器的操作员；玩家真正需要参与的是两个有趣的瞬间：

1. **发现**：把几件原本分散的观察联系起来，形成一个值得相信或继续追问的想法。
2. **创新**：把一个需要和若干已有知识拼成一种改法，做成实体原型，装到真实设备上并决定是否采用。

因此前台只保留一条共同主循环：

```text
观察或收到报告
→ 钉到证据板
→ 玩一局“汇聚灵感”
→ 亲手记下一个想法
→ 找居民讨论、补充或挑战它
→ 去世界里做一件明确的事
→ 形成发现，或做出并试用一个设计
```

之后分成两条短循环：

```text
科学发现：观察 → 灵感 → 想法 → 讨论 → 再观察/验证 → Finding

工程创新：需要 → 灵感 → 设计想法 → 讨论 → 动态原型 → 通用升级界面安装
          → 正常使用中的试用报告 → 采用为 Innovation 或带着结果返工
```

玩家只需要稳定理解六个词：`观察`、`证据`、`想法`、`发现`、`设计`、`原型`。`Claim`、`Justification`、`Procedure`、采样窗口、统计摘要、运行记录和技术投影仍可存在于后台，负责一致性、来源和集成，但不再成为主流程中的必填表。

系统的总边界改为：

> **玩家选择“什么值得联系、相信和采用”；居民、计算器与设备负责“怎样算、怎样记录和怎样检查”。**

规则引擎可以找出可比较的记录，居民可以提出候选解释，计算器可以生成报告，设备可以在一次已启动的试用中自动采样；但只有玩家能执行 `记下想法`、`采纳讨论方向`、`形成发现`、`授权原型` 和 `采用设计`。

## 明确删除或下沉的设计

以下内容不进入 V1 的常规玩家流程：

- 四种抽象 `ProblemFrame` 选择器；入口只显示 `找规律` 和 `想改法`。
- 玩家手填变量、单位、控制条件、实验组、停止条件、有效区间和统计方法。
- 一屏十几张“研究义务”与通用义务 DAG。
- 单独的实验控制台、实验连接器和原型工作台。
- 给每一种机器的原 GUI 加研究槽位、研究标签页或维护面。
- 让每项内容作者编写公式、知识图、实验状态机或任意脚本布尔表达式。
- 把 `Procedure`、`DesignStandard` 作为与 Finding 并列的两套前台进度树。
- 普通设备逐 tick 向知识系统倾倒原始数据。
- 用通用研究点、百分比进度或等待条替代玩家的选择与世界行动。

精密验证、曲线、来源链和方法文档可以放在 `查看详情` 中，也可以成为少数后期内容的可选深化玩法，但不能成为每一个发现和改良的默认负担。

## 玩家看见的对象

| 玩家用语 | 它是什么 | 玩家做什么 | 后台表示 |
|---|---|---|---|
| 未整理记录 | 刚写下或收到、尚未确认的事情 | 查看、归档、忽略或钉上证据板 | `ObservationDraft` / `Report` |
| 观察 | 有时间、地点、对象和来源的正式记录 | 引用到多个想法，不再改写原文 | immutable `ObservationRecord` |
| 证据板 | 本次思考所用的 2–5 张记录与样品 | 选择意图并开始纸牌局 | `EvidenceBoardSession` |
| 想法 | 玩家认为几张记录之间可能有联系 | 讨论、验证、改写或搁置 | `IdeaRevision` |
| 发现 | 团队接受的、带适用范围的认识 | 发布、引用；以后可被新记录修订 | `FindingRevision` |
| 设计 | 一个具体的改进方向和材料方案 | 画草图、制作原型、返工 | `InnovationProject` |
| 原型 | 可拿在手里、可安装、带来源的升级物品 | 用专用工具装到兼容设备，试用后拆下或采用 | `PrototypeUpgradeItem` + prototype ID |
| 已采用改良 | 经过试用、可以稳定制造和部署的设计 | 正常制造、安装和自动化 | `InnovationRevision` + projection |

`Procedure` 在后台表示居民或计算器采用的作业方法；玩家看到的是“计算报告”“测量方法”“操作说明”。`DesignStandard` 在后台是采用设计时冻结的参数、材料和适用设备快照；玩家看到的是“已采用改良”。这样仍保留可追溯性，却不要求玩家维护三种近似的成果类型。

## V1 的物品、方块和界面

| 载体 | 玩家用途 | 现有/新增 | V1 边界 |
|---|---|---|---|
| 研究笔记 `research_notebook` | 现场记录、收件箱、当前任务提示 | 新增物品 | 物品只存书签；team 数据才是权威 |
| 绘图台 `frostedresearch:drawing_desk` | 归档、证据板、汇聚灵感、想法与成果页 | 保留现有方块并改造菜单 | 保留样品、纸张、笔三个现有槽和局中保存 |
| 机械计算器 `frostedresearch:mechanical_calculator` | 执行已委托的计算工作 | 保留现有方块 | 不再生产通用研究点；Create 缺席时由居民代算 |
| 研究所 `frostedheart:research_institute` | 安排讨论、计算、整理与复核班次 | 新增城镇建筑 | 只产生待玩家查看的工作产物，不持有知识真相 |
| 研究升级工具 `frostedresearch:research_upgrade_tool` | 对兼容机器打开统一升级界面 | 新增工具 | 不调用目标机器自己的 GUI |
| 可写入升级坯件 `frostedresearch:upgrade_blank` | 用普通合成按 BOM 做出尚未写入设计的实体部件 | 新增通用物品 | 自定义装配 recipe 可以记录所选材料，但没有任何升级效果 |
| 通用原型物品 `frostedresearch:upgrade_prototype` | 承载某一设计、材料与 prototype ID | 新增单一物品 | 不为每项创新注册新 Item；试用进度不写在物品上 |
| 标准升级物品 `frostedresearch:upgrade_component` | 承载已经采用、可重复制造的改良 | 新增通用物品 | 同定义/材料可以堆叠；继续使用动态 renderer |
| 研究文档 `frostedresearch:research_document` | 把报告、草图或结果作为物品展示/转交 | 可选新增 | 只保存 team/document/revision 引用，丢失可重印 |

V1 不新增实验控制台、连接器和原型工作台。玩家先按 JEI 中的数据定义装配 recipe，在普通工作台做出 `upgrade_blank`；再把坯件放进绘图台 `EXAMINE` 槽，配合纸和笔点击 `写入当前草图`，原位转成 team-bound 原型。这样材料从真实合成格消耗，绘图台也不需要远程扫描玩家背包。安装统一走研究升级工具。

现有 `lab_block_cabinet`、`lab_control_panel_*` 等装饰可用 tag 为研究所提供环境加成，但不把已放置的普通方块迁成 BE。

## 从新档开始的实际体验

### 1. 先学会记录，而不是先选科技树

开场给玩家一本基础笔记和炭笔。玩家手持笔记，对准可观察对象潜行使用约一秒：

1. HUD 显示对象名称与“正在记录”；
2. 服务端解析对象和当时上下文；
3. 笔记收件箱新增一条“未整理记录”；
4. 玩家只看到人话摘要，例如“这里比刚才暖”“塔目前在工作”“仓库中的焦煤正在下降”。

没有温度计时，系统只记录肉眼可知的冷热档位；有仪器时才附带数值和量程。它不会让玩家手填一个并未测量的温度。

第一段教学只要求在两个不同条件下各记录一次，并把两张卡钉到绘图台。教学重点是“比较”，不是术语。

### 2. 第一次汇聚灵感

绘图台保持现有三个物品槽：

- `EXAMINE`：样品、拓印、旧文档或相关物品；
- `PAPER`：新草稿的纸张；
- `INK`：现有 `IPen` 实现。

绘图台主页面为：`收件箱`、`证据板`、`想法`、`发现与设计`。玩家把 2–5 条记录钉上证据板，选择 `找规律` 或 `想改法`，再点击 `汇聚灵感`，进入改造后的现有纸牌小游戏。

### 3. 城镇让研究变丰富，而不是让研究才刚能开始

没有城镇时，玩家仍能记录、玩证据板、形成简单现场发现，并自己完成短验证。

城镇与研究所开放的能力是：

- 邀请不同经历的居民参与讨论；
- 把计算或资料整理委托成一个班次；
- 同时推进几个想法；
- 让另一名居民复看结果；
- 从居民经历中获得新的观察、概念和改进路线。

居民不是“研究点产出机器”。他们为玩家增加的是可想到的候选解释、可执行的工作和解释结果的能力。

### 4. 第一个工程原型

当玩家形成一个工程想法并完成讨论后，设计页直接显示装配配方和 `在 JEI 中查看`。玩家用普通工作台制作升级坯件，再回绘图台把当前草图写入坯件。拿到原型后，玩家手持研究升级工具右键兼容设备，在统一界面中放入原型并确认安装。机器按原有方式运行；研究系统只在试用期间收集该设计需要的摘要。

玩家最终看到的是“做出来、装上去、用了以后发生什么”，而不是签发一套实验表。

## 观察与报告

### 权威管线

```text
ResearchSignal（瞬时事实，不是知识）
→ ObservationGate（是否有人注意或已接受记录任务）
→ ObservationDraft / 第三方 Report
→ ReportInbox
→ 玩家归档
→ immutable ObservationRecord
```

只有以下入口会形成 Draft 或 Report：

- 玩家正在用笔记主动记录；
- 玩家接受了一张明确的世界行动卡，系统在该任务范围内记录；
- 居民本班次的工作使其有机会注意到相关事件；
- 城镇日结算产生值得报告的摘要；
- 一次不可重复的事故进入收件箱；
- 一个已安装原型正处于玩家启动的试用期。

普通世界 tick 只产生系统自己的运行事实，不自动变成团队知识。

### 玩家可执行的记录动作

1. **看一眼**：笔记对方块、实体或位置使用。
2. **带仪器记录**：笔记自动引用手中或附近仪器允许读取的变量。
3. **把报告送往研究**：在城镇历史、机器摘要或居民对话中点击按钮。
4. **问一个人**：拿一张观察卡作为话题，与居民谈其经历。
5. **检查样品**：将样品、拓印或文献放入绘图台的 `EXAMINE` 槽。
6. **跟随行动卡再看一次**：HUD 给出地点/对象/条件提示，后台捕获上下文。

每张观察卡默认只显示：`谁/何时/何地`、`看见什么`、`当时对象状态`、`来源类型` 和 `有什么不知道`。数值、原始采样和来源链放进展开详情。

收件箱动作固定为：`归档`、`钉到证据板`、`询问来源`、`降低此类提示频率`、`搁置`。归档后的观察内容不再被改写；钉住只建立引用。

## 证据板与“汇聚灵感”纸牌局

这是整个系统最重要的玩家操作。它不是答案测验，也不随机决定世界真理，而是对“翻资料、找关联、产生思路”的抽象。

### 开局前

1. 玩家钉住 2–5 张观察、报告或样品卡；
2. 选择 `找规律` 或 `想改法`；
3. 服务端先按 `找规律/想改法` 过滤 `discovery_v1/innovation_v1`，再检查各 project 的必需 `evidence_slots` 是否都被所钉来源满足；
4. 如果没有任何候选关系，按钮不会扣纸墨，并提示缺少哪座桥，例如“还缺同一对象在另一种条件下的记录”；
5. 如果存在候选，创建 `InspirationSession`，消耗一张纸和开局墨水，生成保证可完成的一盘牌。

候选想法在开局时已经由真实证据和数据定义确定。一次 session 可以同时命中多个 project，因此清盘后能出现 1–3 张候选卡；单个 project 仍只需声明自己贡献的那一张 Idea。纸牌局决定玩家是否完成这次思考，不决定哪条自然规律为真。

### 复用现有小游戏

保留现有 `ResearchGame` 的核心：9×9 布局、只有外露牌可动、两张匹配牌消除、顺序牌最后收束、T1–T4 布局和中途保存。只替换入口、牌面含义和完成产物。

| 现有牌类 | 新的玩家含义 | 视觉反馈 |
|---|---|---|
| 四类基础牌 | `对象 / 条件 / 变化 / 结果` | 角标显示来源证据的颜色 |
| `PAIR` | `已知 ↔ 异常` | 消除时突出“预期与偏差” |
| `WILDCARD` | `换个角度` | 可连接任一当前关系 |
| `ADDING` | `思路 1…N → 收束` | 按顺序揭示一句关系摘要 |

跨来源牌成功配对时，证据板上的两张卡之间亮起连线，右侧出现“同一对象”“不同条件”“先后相邻”“共同结果”“同一需求”等短语。牌面可以先复用现有图标并增加 tooltip；内容专属牌面属于资源包增强，不是 V1 前置。

局内按钮只有：`提示一对`、`保留并退出`、`重新整理`。错配只抖动并提示“这两张暂时连不上”，不耗墨；成功配对才消耗墨。居民经验或已有 Finding 可以提供一次提示、揭开一张遮挡牌或加入万能牌，但不能直接代替清盘。

### 清盘后的产物

清盘后展示 1–3 张人话候选卡，例如：

- 找规律：`塔工作时，周围似乎存在一个有边界的热区。`
- 想改法：`稳定送风也许能让每份焦煤维持得更久。`
- 另一角度：`储备下降也可能来自补给中断，而不是机器耗得更快。`

结果按钮为：`记下这个想法`、`再想一个`、`先放着`。只有点击 `记下这个想法` 才创建 `IdeaRevision`。已存在的相同 Idea 不会复制，只显示“新增 2 条来源”。

源码迁移上，`DrawingDeskTileEntity#updateGame → ResearchHooks.commitGameLevel` 的新路径改为 `finishInspirationSession → revealIdeaCandidates`；旧研究仍可保留原 `MinigameClue` 兼容入口。`DrawingDeskTileEntity` 继续在 `gamedata` 保存盘面，但新增 session kind、证据引用和候选 pool revision。

## 科学发现短循环

科学发现的玩家流程固定为六步，不出现实验计划编辑器。

### 1. 形成想法

玩家至少准备两个可比较的来源，在证据板选择 `找规律`，完成纸牌局并记下一张 Idea。

Idea 页只给三个主要动作：`找人讨论`、`照这个再看一次`、`搁置`。

### 2. 居民提出候选解释

玩家可以直接拿 Idea 找一名居民谈，也可以在研究所安排一次讨论班次：

1. 选择 Idea；
2. 选择最多 3 张要带进会议的证据；
3. 邀请 1–3 名当前没有岗位冲突的居民；
4. 安排一个班次；
5. 班次结束后查看 1–3 张带发言人与来源的讨论卡。

讨论卡只有三类：

- `新见闻`：可以钉回证据板；
- `另一种解释`：可以记成另一张 Idea；
- `建议再看什么`：可以变成一张世界行动卡。

候选来自 project 定义、居民持有的知识标签、岗位经历和团队已有 Finding 的组合。居民不会读取隐藏答案。`intelligence`、教育和岗位经验影响其能否说出某张卡、需要几个班次以及说明是否清楚，不作为“说真话概率”。

玩家对卡片执行 `采纳建议`、`记为另一个想法`、`钉回证据板` 或 `留档`。讨论结束本身不会自动选择一个解释。

### 3. 玩家做一次具体的世界行动

采纳建议后，系统最多显示一张主行动和两张可选行动。例如：

- `在相近天气下，记录塔工作时的近处和远处温度。`
- `等塔停止后，在同一位置再记录一次。`
- `请另一名居民检查这两组记录。`

玩家点击 `去看看` 后，笔记 HUD 显示对象、地点和简单条件；服务端自动保存 subject、状态、天气、距离、时间与仪器上下文。玩家无需填写变量表。

### 4. 居民或计算器处理量化部分

世界行动结束后，Idea 页出现一个 `处理这些记录` 按钮。固定 flow 自动带上当前证据、补充观察和 action profile；玩家若同时拥有研究所与机械计算器，只选择由谁处理，然后等待结果卡。基础 profile 在两者都没有时允许玩家直接在绘图台整理，因此早期发现不会被城镇或 Create 锁死；只有明确标成后期内容的 profile 才能要求专用设施。玩家不再选记录区间、工作名称、公式或方法。

计算模板负责单位检查、区间对齐、聚合和误差说明。玩家看到的默认摘要是：

- `多数记录一致；`
- `新记录与想法相反；`
- `条件差异太大，暂时无法判断；`
- `只在较近距离成立。`

原始数字和曲线可展开，但不是推进按钮的前置。

### 5. 玩家决定结论

结果页提供：

- `形成现场发现`；
- `缩小说法`；
- `继续观察`；
- `保留反例并放下`。

系统会根据已记录条件自动建议适用范围，玩家只确认人话表述，不能删除不利记录。`形成现场发现` 创建 immutable `FindingRevision`，保存所用 Idea、观察、讨论和结果引用。

### 6. 以后怎样修订

新观察与 Finding 冲突时，不直接撤销下游机器或抹掉历史。它会形成一张“需要复看”的收件箱卡。玩家可以缩小范围、发布新 revision 或保留反例。前台仍然只叫“修订发现”。

## 工程创新短循环

工程创新与科学发现共享证据板和讨论，但它的目标是做出一个可用改良。

### 1. 把事情设为“待改进”

入口可以来自：

- 城镇警报或日报上的 `设为待改进`；
- 对兼容设备用笔记记录后选择 `想改法`；
- 一个 Finding 页上的 `能否利用这个发现？`；
- 居民对话中的工艺建议；
- 任务给予的一份档案或样品。

玩家把问题报告、已有 Finding、操作者证词或样品钉到证据板，完成 `想改法` 纸牌局并记下设计 Idea。

### 2. 讨论设计方向

讨论界面与科学路线相同，但候选卡变成 1–3 个工程方向，例如：

- `增加稳定送风；`
- `改变导流结构；`
- `先排除补给中断。`

玩家只需要选择“要试哪一种”和可选材料方案。适配设备、需要的接口、效果计算和试用条件来自被 project 引用的 upgrade definition 与集成 profile。

### 3. 画草图并制作动态原型

选择方向后，设计页显示：

- 原型名称与示意图；
- 兼容的升级类别；
- 1–3 个材料选项及其人话取舍；
- 实体 BOM；
- 一句试用要求；
- `查看装配配方`。

缺材料时每项可跳到 JEI。玩家在普通工作台按专用装配 recipe 做出 `upgrade_blank`，其 NBT 只记录 upgrade definition 与所选材料；此时没有效果。然后把它放进绘图台 `EXAMINE`，用纸和笔点击 `写入当前草图`，服务端将它原位转换成 `upgrade_prototype`，写入 team、prototype ID、design revision、material 与 visual profile。效果仍由服务端定义解析，不信任物品 NBT 中的任意数值。

### 4. 用通用工具安装，而不是改机器 GUI

玩家手持 `research_upgrade_tool` 右键目标设备：

1. 服务端把普通 BE 或多方块任意从属块解析成统一 host；
2. 若设备不支持研究升级，显示“这个设备没有可研究改良接口”；
3. 若支持，打开 `ResearchUpgradeMenu`；
4. 玩家从背包选择原型，界面显示可用接口与兼容性；
5. 点击 `安装并开始试用`；
6. 服务端重新检查目标、队伍、距离、实例、prototype ID 和 host revision，再原子安装。

统一界面示意：

```text
┌──────────────── 研究改良 ────────────────┐
│ 目标：T1 能量塔         状态：正常工作       │
│ 接口：燃烧控制          [受控进气原型]        │
│                                             │
│ 预期：每份燃料维持更久                      │
│ 观察：供热是否维持；是否发生断供             │
│ 状态：尚未开始 / 试用中 / 可判断 / 需补做    │
│                                             │
│ [选择背包中的原型] [安装并开始试用] [拆下]   │
└─────────────────────────────────────────────┘
```

这里的“槽”只是统一菜单中的受控选择与确认，不是目标 BE 暴露给漏斗、原生菜单或自动化的 `ItemStackHandler`。

### 5. 正常使用就是试用

原型安装后，玩家继续正常经营机器。HUD、笔记和通用升级菜单只显示：

- `样本不足`；
- `试用正常进行`；
- `暂时无法判断：发生断供/停机/目标被拆除`；
- `看起来改善`；
- `出现副作用`；
- `可以查看试用报告`。

后台 provider 只在这一个 active trial 范围内捕获所需事件和压缩摘要。中断不会清零，也不会硬判失败；恢复条件后继续累计可比较时段。

原型 effect 可以在安装后确定性生效；试用不是让玩家用统计学重新发现 JSON 里的 `+10%`，也不靠隐藏成功率决定原型是否“碰巧成功”。它检查的是组件确实装到了正确 host、在正常服务中运行了足够久、预期收益能被游戏机制表现出来，以及 authored tradeoff/副作用是否出现。这是一段工程采用体验，不伪装成论文复现。

### 6. 采用、返工或拆除

试用报告固定提供：

- `采用这个设计`：形成 `InnovationRevision`，开放标准组件制造或部署；
- `继续改`：把试用表现和副作用自动钉回证据板，开启下一 revision；
- `限定用途`：采用，但把兼容范围缩小到本次验证条件；
- `拆掉并留档`：返还同一个原型物品，保留负结果。

采用设计不是把某个全队数值加到所有机器上。它开放的是一种标准升级物；只有真正安装了该组件的 host 才获得效果。

## 居民、计算器与抽象化研究工作

### 居民私人知识

居民入队不会把所有经历自动灌入 team 知识。其背景只在以下情况外化：

- 玩家拿观察或 Idea 主动交谈；
- 居民被邀请参加相关讨论；
- 居民在相关岗位遇到异常并提交报告；
- 玩家安排其整理或教学。

私人知识由稳定 knowledge tags 与 authored cards 表示，不读取 Java 类名或岗位类的 `getSimpleName()` 作为持久键。

### 固定的讨论与处理班次

V1 只有两种可委托班次：`讨论` 和 `处理资料`。玩家不创建通用工作单，也不排列任务图：

- 讨论班次：选择当前 Idea 与居民，结束后得到候选话语卡；
- 处理资料：把当前 flow 已经收集到的记录交给研究所或计算器，结束后得到一张人话结果卡。

两者内部都只需要 `排队 → 进行/等待 → 等待玩家查看 → 已查看`。系统不会在一个班次完成后自动开启下一项。居民当前岗位与研究班次不能同时占用；研究所用 `ActivityReservation` 或等价机制表达离岗与替补。

机械计算器和居民处理同一份 registered profile。计算器加快处理并给出更细详情；Create 未加载时，受教育居民仍能完成主线。玩家不选择公式和统计方法。

## 两条固定 flow，而不是义务编译器

V1 不实现 `CaseStepResolver`、通用 step DAG、`after/optional` 依赖或内容作者可编排的状态机。project 的 `template` 只能选择下面两条固定流程：

```text
DiscoveryFlow
证据就绪 → 灵感局 → 记下 Idea → 讨论（可跳过）
→ 执行唯一 action profile → 自动/委托处理资料 → 玩家 review → Finding

InnovationFlow
证据就绪 → 灵感局 → 记下设计 Idea → 讨论（可跳过）
→ 做坯件并写入原型 → 通用 GUI 安装 → 唯一 trial profile
→ 玩家 review → Innovation 或返工
```

流程引擎只根据当前固定阶段显示一个主按钮和至多两个退出/返工按钮。世界目标没加载、居民没空或坯件不足时，按钮直接显示人话原因；这只是可用性检查，不会生成另一层“义务”。

知识图仍在后台保存来源关系，但不决定流程结构。Finding 和 Innovation 只能由玩家 review 按钮创建，不能由 flow 到达末端自动发布。

## ActionProfile：把量化和实验设计收进集成层

每个 discovery project 只能引用一个 `action_profile`，每个 upgrade definition 只能引用一个 `trial_profile`。普通数据包作者不能组合采样原语、覆盖统计阈值或声明公式。

profile 由 Java/模组集成作者注册，完整拥有：

- 玩家看到的一句行动提示；
- 能绑定的 subject 与需要捕获的上下文；
- 监听的有名事件/摘要；
- 自动使用机械计算器或居民处理的方式；
- 缺测、中断和可比较条件；
- `支持 / 挑战 / 暂无法判断 / 出现副作用` 的人话输出。

常见 profile 可以由引擎内置成“同一对象开/关各看一次”“带回 N 份样品”“重复一次动作”等，供数据包直接引用；复杂机器由其集成模组注册专用 profile。project JSON 不读取 `GeneratorData.process` 等内部字段，也不存 Java/NBT 路径。

对于 T1，实际装料事件、运行状态段、process 余额和供给中断由 FrostedHeart 集成层暴露给 profile；城镇仓库库存只能作为“储备估计”，不能伪装成实际连续供料。手动 `/town tick` 或测试结算标记为 `COMMAND/TEST`，不能满足要求自然运行时间的 profile。

## Finding 与 Innovation

### Finding：一种玩家可理解的发现

Finding 至少冻结：

- 一句人话陈述；
- 自动建议、由玩家确认的适用范围；
- 支持它的观察和结果引用；
- 已知反例与尚未覆盖条件；
- 发布者和 revision。

前台只显示两个强度徽记：

- `现场发现`：一次合理的再观察或验证后可形成，足以开启新想法或原型路线；
- `已复看`：另一次独立记录/居民复看后派生，可作为要求更高内容的前置。

徽记由 project 的 finding policy 根据已有来源派生，不要求玩家进入另一套发布状态机。升级仍由玩家点击 `确认修订`。

### Innovation：一种已经被采用的改良

工程 Idea 与未完成草图属于 `InnovationProject`；它们不是技术权限。玩家试用后点击 `采用这个设计` 才创建 immutable `InnovationRevision`。它冻结：

- 所依据的 Idea/Finding/讨论；
- upgrade endpoint 与适用 host；
- 材料方案、视觉 profile 和 BOM；
- 效果 handler 及参数 revision；
- 试用报告与已知副作用；
- 标准制造动作与发布者。

内部可以由该 revision 物化 `ProcedureArtifact` 和 `DesignStandardSnapshot`，供自动化、维护与溯源使用；它们不再要求内容作者单独写两条前台研究路线。

### 技术投影

投影由 `(teamKnowledgeRevision, catalogRevision)` 重建，不是事实源：

- `KnowledgeProjection`：哪些 Idea pattern、居民话题和档案条目可见；
- `TechnologyAccessProjection`：哪些配方/动作是隐藏、原型专用或标准可用；
- `InstalledContributionSnapshot(host)`：该 host 当前安装的组件实际提供什么效果。

JEI 和客户端菜单读取投影；真正制造、安装和机器效果仍由服务端重新查询。新 Innovation 不写回 team-wide `generator_effi`。旧档效果只走 legacy entitlement，并与对应新升级放进同一 exclusive family，避免重复叠加。

## 通用研究升级架构

### 目标接口与 adapter

自己的普通 BE 可以直接实现窄接口；IE 多方块和第三方设备通过 adapter 注册，不要求修改它们的原 GUI：

```java
interface ResearchUpgradeableDevice {
    ResearchUpgradeHostRef hostRef();
    List<ResearchUpgradeSocketView> sockets();
    ResearchUpgradeHostState snapshot();
    ItemStack installed(SocketKey socket);
    InstallCheck validate(ItemStack upgrade, SocketKey socket);
    ItemStack insert(SocketKey socket, ItemStack upgrade);
    ItemStack extract(SocketKey socket);
    void onResearchUpgradeChanged();
}

interface ResearchUpgradeHostAdapter<T> {
    Optional<ResearchUpgradeHostAccess> resolve(ServerLevel level, BlockPos clickedPos);
}
```

接口只描述稳定 host、逻辑接口、当前安装、revision 和受控读写原语；不暴露 `Menu`、`Screen`、`LazyOptional<ItemStackHandler>` 或目标机器内部字段。

`ResearchUpgradeTargetResolver` 的解析顺序：

1. 查普通 BE 是否直接实现接口；
2. 用 `CMultiblockHelper#getBEHelperOptional` 将 IE 从属块规范化到 master；
3. 以 `CMultiblockRegistration` 或 BE type 查 adapter；
4. 返回 `hostType + GlobalPos(master) + incarnation UUID`。

只用坐标不够：机器拆除再搭建后必须是新 incarnation，不能继承上一台机器的原型和试用记录。

### 工具与菜单事务

专用工具交互应由 `PlayerInteractEvent.RightClickBlock` 优先路由，再在服务端 `NetworkHooks.openScreen`。仅实现 `Item#useOn` 可能被目标方块自己的 `use` 提前消费。

`ResearchUpgradeMenu` 不继承某一种 BE/多方块菜单。打开和每次按钮意图都重新检查：

- 玩家仍在同一维度且距离不超过 8 格；
- 当前点击位置仍解析到相同 host type、master 与 incarnation；
- 当前 team/owner 可操作；
- host 与 knowledge revision 符合预期；
- prototype ID 对应的物品确实位于服务端玩家背包，且没有另一项 active trial 使用同一 ID。

客户端只发送 `INSTALL/REMOVE`、逻辑 socket、背包 slot 与 expected revisions，不发送任意 ItemStack、效果或材料事实。菜单展示 ghost slot；实际移动由 `TeamResearchService` 完成：校验 → 精确取出同一 prototype ID 的 stack → 写入 host → 建立/结束 team trial runtime → mark dirty/sync → revision++。失败返回最新视图和可恢复原因，不部分扣物。

安装事实属于世界 host：普通 BE 写自己的 save/load，IE T1 adapter 写 `GeneratorState#writeSaveNBT/readSaveNBT` 并 `markDirtyAndSync`。team runtime 只保存 `prototype ID + host ref` 对应的试用摘要，不保存第二份 ItemStack 或全局位置索引。正常拆除返还 host 中的同一 stack；目标被拆毁时沿用该 host 的正常掉落/返还生命周期，V1 不另造 detached-component 恢复系统。

当前 `GeneratorContainer` 负责整座塔的结构升级、材料扣除和重组，不能复用为这个通用菜单。

### 少量通用物品，动态定义与外观

V1 固定注册 `upgrade_blank`、`upgrade_prototype`、`upgrade_component` 三个通用载体，不按每项创新注册新 Item。原型不可堆叠；在 Minecraft 1.20.1 中使用 NBT 保存稳定引用：

```text
team
prototype_id
innovation_id
design_revision
visual_profile
material_id
```

NBT 不保存可自由篡改并直接生效的 `+10%`；服务端根据 `innovation_id + revision` 解析效果。

渲染分两级：

1. 固定轮廓的原型使用 `item/generated`：`layer0` 为可染色材料底层，`layer1` 为功能 overlay；`ItemColor` 按 `material_id` 给底层染色。
2. 若 design 需要不同轮廓，`FRClient` 在 model bake 中为这三个通用 Item 共用一层 `BakedModel + ItemOverrides`，只按 catalog 白名单 `visual_profile` 选择已预烘焙模型；缓存键包含资源重载 revision、profile 和 material。

不直接复用 `LiningModel` 系列：其 model bake/texture stitch 入口目前已注释并标记待移除。平面升级图标也不需要 BEWLR。现有 `wooden_cup_drink.json`、`ceramic_bucket.json` 和 `FHClientEventsMod#onTint` 可作为分层与染色的活跃参考。

V1 明确使用两个基础物品：不可堆叠、team-bound 的 `upgrade_prototype`，以及采用后可正常制造、同定义和材料可堆叠的 `upgrade_component`。二者共享动态 renderer 与 host 接口，但不会混用 team 绑定和批量制造语义。

## 数据包作者契约

### 总原则

每项普通研究只要求一个主文件：

```text
data/<namespace>/frostedresearch/projects/<path>.json
```

文件路径就是 canonical `ResourceLocation`。普通内容作者只声明：

- 证据板上至少两个必需证据位、至多三个可选补充位；
- 清盘后出现哪张 Idea；
- 居民可能提出哪些候选话语；
- discovery 引用哪一个 `action_profile` 并形成什么 Finding；或 engineering 引用哪一个 `upgrade_definition`。

引擎按固定 flow 生成 session、讨论/处理班次、来源引用、运行摘要、revision 和时间线。作者不写 step、`after/optional`、knowledge graph、Obligation DAG、玩家 UUID、坐标、随机 nonce、`completed/progress/points`、Java 字段/NBT 路径或直接 variant 写入。

V1 的 project schema 没有通用 `type/params` dispatch，也不提供脚本表达式。扩展能力集中在由集成作者注册的 `action_profile` 和另行定义的 `upgrade_definition`。

### 最小 project 结构

```text
format             schema 版本
template           discovery_v1 | innovation_v1
evidence_slots     2 个必需位 + 0–3 个可选位
idea               清盘后该 project 贡献的 Idea local key
discussion_cards   居民话语卡；可以为空
action_profile     discovery_v1 必需
finding            discovery_v1 必需
upgrade            innovation_v1 必需，引用 upgrade definition
legacy             可选旧 ID 桥
```

集合内部使用稳定 local key；显示文本默认派生 translation key，例如：

```text
research_project.frostedheart.t1_heat_zone.idea.boundary
research_project.frostedheart.t1_heat_zone.discussion_card.stored_heat
research_project.frostedheart.t1_heat_zone.finding
```

各部分的公共字段固定如下：

| 部分 | 公共字段 | 作者实际决定什么 |
|---|---|---|
| `evidence_slots[]` | `id, accept, optional?` | 每个钉位接受哪一类 observation/report tag |
| `idea` | local key | 该 project 命中时贡献哪张 Idea 卡 |
| `discussion_cards[]` | `id, knowledge, kind` | 什么居民能给出什么类型的话语 |
| `action_profile` | 一个 `ResourceLocation` | discovery 的唯一世界行动与后台处理 |
| `finding` | `scope, policy` | review 时可形成怎样的发现 |
| `upgrade` | 一个 `ResourceLocation` | innovation 使用哪项可复用升级定义 |

`kind` 只使用 `evidence / alternative / caution / design` 四个讨论卡语义。居民卡影响提示、解释和玩家选择，不创建另一条任务分支。若新内容无法塞进这两种 template，应先写新的引擎 template，而不是给普通 project 增加流程语言。

### 科学发现最小示例

```json
{
  "format": 1,
  "template": "discovery_v1",
  "evidence_slots": [
    { "id": "active", "accept": "#frostedheart:observation/t1_active_temperature" },
    { "id": "inactive", "accept": "#frostedheart:observation/t1_inactive_temperature" },
    { "id": "weather", "accept": "#frostedheart:observation/weather", "optional": true }
  ],
  "idea": "bounded_heat",
  "discussion_cards": [
    { "id": "stored_heat", "knowledge": "#frostedheart:heat_operation", "kind": "alternative" },
    { "id": "weather_change", "knowledge": "#frostedheart:weather_experience", "kind": "caution" }
  ],
  "action_profile": "frostedheart:t1_heat_boundary",
  "finding": {
    "scope": "frostedheart:t1_active_heat_zone",
    "policy": "frostedresearch:field_finding"
  }
}
```

作者没有声明温度读取公式、天气校正、距离采样或“几组算一致”。这些属于 `frostedheart:t1_heat_boundary` profile。作者只提供内容组合和文本。

### 工程创新完整 V1 示例

```json
{
  "format": 1,
  "template": "innovation_v1",
  "legacy": {
    "id": "generator_efficiency_1",
    "mode": "coexist"
  },
  "evidence_slots": [
    { "id": "need", "accept": "#frostedheart:report/fuel_reserve_low" },
    { "id": "machine", "accept": "#frostedheart:observation/t1_fuel_use" },
    { "id": "airflow", "accept": "#frostedheart:evidence/airflow", "optional": true }
  ],
  "idea": "controlled_draft",
  "discussion_cards": [
    { "id": "supply_gap", "knowledge": "#frostedheart:logistics", "kind": "caution" },
    { "id": "normal_mode", "knowledge": "#frostedheart:t1_operation", "kind": "design" }
  ],
  "upgrade": "frostedheart:t1_controlled_draft"
}
```

普通研究作者到这里已经完成。固定 `InnovationFlow` 会从 upgrade definition 取得坯件配方、外观、host、试用 profile 与采用后的 standard action；只有玩家点击 `采用这个设计` 才创建 Innovation。

### 升级定义：给高级内容/集成作者

可复用升级单独放在：

```text
data/<namespace>/frostedresearch/upgrades/<path>.json
```

它不是研究流程，只描述一种能被通用工具安装的技术部件：

```json
{
  "format": 1,
  "host": "frostedheart:generator_t1",
  "socket": "frostedheart:combustion_control",
  "materials": {
    "cast_iron": {
      "ingredient": { "tag": "forge:plates/cast_iron" },
      "count": 8,
      "visual": "frostedresearch:cast_iron"
    }
  },
  "bom": [
    { "item": "create:encased_fan", "count": 1 },
    { "item": "charcoal_pit:mechanical_bellows", "count": 1 }
  ],
  "appearance": "frostedheart:controlled_draft",
  "trial_profile": "frostedheart:t1_fuel_upgrade_trial",
  "effect": {
    "type": "frostedheart:generator_fuel_duration",
    "value": 0.10,
    "exclusive_family": "frostedheart:t1_fuel_efficiency"
  },
  "standard_action": "frostedheart:install/t1_controlled_draft"
}
```

`effect.type` 是集成层注册的 typed handler，知道单位、组合方式、host 上下文和 tooltip；`trial_profile` 拥有采样与判读。升级作者只选择公开参数，不能读任意 Java/NBT 字段。一个 upgrade definition 可以被多个 project、任务或旧内容桥引用。

upgrade loader 为每个材料方案生成一个可被 JEI 展示的 `ResearchUpgradeAssemblyRecipe`：设计 Idea 尚未形成时不可执行；草图可用后输出带 upgrade/material 引用的 `upgrade_blank`；采用 Innovation 后，`standard_action` 允许相同 BOM 直接产出 `upgrade_component`。这是 recipe/action 投影，不为每种升级动态注册新物品或新 recipe type。

### 数据包与资源包的分工

数据包提供 project、标准配方、tags 和服务端参数；资源包提供：

```text
assets/<namespace>/textures/item/research_upgrade/<appearance>.png
assets/<namespace>/models/item/research_upgrade/<appearance>.json   （仅不同轮廓时）
assets/<namespace>/lang/<locale>.json
```

缺失 appearance 时使用醒目的通用原型 overlay 并给 reload diagnostics，不让服务端研究内容失效。材料 tint 可以引用引擎内置 material profile；只有新色彩/纹理需要资源包。

### 集成作者与内容作者的边界

**普通研究作者**只写 evidence tags、Idea/讨论卡 local key、一个 action profile 或一个 upgrade ID，以及翻译文本。

**高级升级作者**写可复用 upgrade definition，组合已经注册的 host/socket、material profile、trial profile 和 effect handler；不设计研究流程。

**Java/模组集成作者**才负责把真实系统接入：

- 注册 observation/report tags 及其有名 facts/metrics；
- 注册 `ActionProfile` / `TrialProfile`；
- 注册 `ResearchUpgradeHostAdapter`；
- 注册 effect handler 与组合规则；
- 生成默认人话摘要和高级详情；
- 确保 telemetry 在重启、区块卸载和实例更换时语义正确。

KubeJS 只生成同一份 JSON/资源，不直接操作 `TeamKnowledgeData` 或运行时任务。

为了让“引用 profile/tag”不是猜 ID，开发环境提供 `/research catalog dump`（或等价 datagen 输出），列出每个 evidence tag、action/trial profile、upgrade、host/socket 和 effect handler 的：ID、提供模组、玩家提示、适用对象、所需资源及一段最小 JSON。reload 诊断必须定位到 `project ID + 字段/数组索引`，并给出未知 ID 的同名候选。

### Reload 与验证

新 project/upgrade catalog 通过 datapack reload listener 整批 parse、交叉引用验证并原子安装；错误项目保留上一份可运行定义并给出具体诊断。catalog 使用稳定 `ResourceLocation + format/revision`，不使用路径敏感 hash 或把一次构建版本钉入存档。

旧 `config/fhresearches/*.json` 继续由旧 loader 读取。新 project 的 `legacy.id` 是 raw 旧字符串，允许兼容 `generator_T1` 等大写 ID；不得直接包装成 `ResourceLocation`。

## 后端最小架构

### 权威数据与服务

```text
TeamKnowledgeData
├─ ObservationRecord / ReportInbox
├─ IdeaRevision / FindingRevision / InnovationRevision
├─ DiscoveryFlowRuntime / InnovationFlowRuntime
├─ ResidentWorkOrder / WorkArtifact
├─ PrototypeTrialRuntime（prototype ID + host ref + 摘要）
└─ revision / timeline

世界 host / ItemStack
└─ 唯一一份已安装 ItemStack 与 host 运行事实

ResearchProjectCatalog
└─ 固定 template、project/upgrade 定义与 profile 引用

TechnologyProjection
└─ 可重建缓存
```

`TeamResearchService` 是所有语义命令和跨存储事务的协调入口；不再同时使用 `TeamResearchManager` 作为另一个权威。现有空 `TeamResearchManager` 可以成为 facade，最终弃用。

建议服务边界：

- `ObservationService`：gate、收件箱、归档与去重；
- `IdeaSynthesisService`：证据板匹配、盘面 session 与 Idea 提交；
- `ResidentResearchService`：讨论卡、班次订单和工作产物；
- `ResearchFlowService`：推进两条固定 flow，并投影当前主按钮；
- `ActionProfileService`：世界行动、试用采样与人话报告；
- `ResearchUpgradeService`：host resolve、安装、拆卸与 trial 绑定；
- `TechnologyProjectionService`：JEI、recipe/action access 与 installed contribution。

世界、城镇和机器只能提交 signal/report/telemetry；不能直接写 Finding 或 Innovation。

### 玩家命令

V1 需要的显式命令只有：

```text
ArchiveObservation
StartInspirationSession
RememberIdea
CommissionDiscussionOrWork
AdoptDiscussionCard
StartWorldAction
ReviewResearchResult
PublishFinding
AuthorizePrototype
InstallOrRemoveUpgrade
AdoptInnovation
```

每个 C2S intent 带 team epoch 与 expected aggregate/host revision。服务端重新解析玩家当前 team、subject、背包、距离与定义；客户端不能声称自己已经观察、完成或拥有某个效果。

### Team 生命周期与同步

`TeamKnowledgeData` 以唯一 plain-string `SpecialDataType` ID 注册到 Chorda team holder。登录、建队、换队、换维度和 catalog reload 都要有显式生命周期。

客户端同步顺序：

```text
CatalogSnapshot(catalogRevision)
→ TeamResearchSnapshot(teamId, teamEpoch, stateRevision)
→ bounded delta
```

切队时先清空旧 town/research projection，再安装同一 team epoch 的快照，避免新研究状态与旧城镇数据混用。观察详情、高频试用片段和图来源按 UI 请求加载，不全量广播。

## 与现有代码和旧研究的映射

### 可利用的现有锚点

- `DrawingDeskTileEntity`：现有 `EXAMINE/PAPER/INK`、`ResearchGame`、纸墨消耗、局中 NBT 与 MenuProvider 可保留。
- `ResearchGame` / `GenerateInfo.T1–T4`：保留盘面生成、可动牌判断和难度布局；替换语义与完成回调。
- `MechCalcTileEntity`：把现有点数生产改成 registered profile executor；Create 可选依赖必须保留居民替代。
- `FRSpecialDataTypes.RESEARCH_DATA` 与 Chorda `TeamDataHolder`：为新 `TeamKnowledgeData` 提供 team 级持久化模式，但必须使用新的唯一 ID。
- `Resident#getIntelligence/getEducationLevel/getWorkProficiency`：作为居民能否承担工作、耗时和说明深度的输入，不作为真理概率。
- `CMultiblockHelper#getBEHelperOptional/#getAbsoluteMaster`：把点击的 IE 从属块解析到统一 host。
- `NetworkHooks.openScreen`、`CBaseMenu`、`P2PTerminalMenuView` 的 bounded view 思路：支撑通用升级菜单。
- `FHClientEventsMod#onTint` 与现有分层 item model：支撑材料色与 overlay。
- 现有 Archive 的 `PanZoomViewport`、相机和裁剪思路：以后可泛化；旧 `ResearchArchiveViewCache` 不能直接拿来显示异构知识。

### 旧系统共存

没有新 project 覆盖的旧 `Research` 继续使用旧 executor，并在新档案中标记 `旧式研究`。新 project 可以：

- `legacy.mode=coexist`：新旧路线并存；
- `legacy.mode=supersede`：只隐藏新玩家尚未开始的旧入口，不改写旧存档状态。

旧 completed、active、level、clueData、effectData、insight、visitedArea 和 variant 必须逐字段保留。旧 `effectData` 继续作为领取幂等依据，不能因迁移重复发物品、经验或命令奖励。

旧完成状态形成 `LegacyEntitlement/LegacyProjection`，不伪造带证据链的 Finding。旧 `generator_effi` 与新实体升级进入同一 exclusive family；resolver 按明确迁移策略取一个有效来源，而不是静默叠加。

## 首条垂直切片：T1 的发现与改良

一条垂直切片包含两个短 project，用来分别验证“发现”和“创新”，不再模拟一套完整科研机构。

### A. 发现：工作中的 T1 有怎样的热影响范围

1. 玩家分别在暴露处和工作中的 T1 附近用笔记记录。
2. 把记录钉上证据板，选择 `找规律`，完成纸牌局。
3. 记下 Idea：`T1 工作时，周围似乎存在一个有边界的热区。`
4. T1 操作者提出“停机后也要在同一地点看看”；有天气经验的居民提醒“天气不同不能直接比”。
5. 玩家接受行动卡，在相近天气下记录近处、边缘、远处，并在停机时复看一个点。
6. `WorldTemperature` 集成 profile 自动保存距离、塔状态、天气与温度来源；计算器/居民输出人话摘要。
7. 玩家选择形成现场 Finding、缩小范围或继续观察。

这条路线验证主动观察、证据板、小游戏、居民候选、世界行动、后台量化和 Finding，而不要求玩家设计实验。

### B. 创新：T1 受控进气原型

1. 城镇燃料储备低报告或玩家的燃料使用记录进入收件箱。
2. 玩家钉住燃料、塔运行和风箱/操作者见闻，选择 `想改法`。
3. 清盘后记下 `稳定送风也许能让每份焦煤维持更久`。
4. 物流员可能提醒“断供会让结果看起来更差”，T1 操作者建议“先在普通工作状态试”；这些话直接成为试用报告的解释条件，不产生基线工作单。
5. 设计页显示受控进气原型的材料、兼容接口和外观；玩家合成坯件并在绘图台写入一个带 prototype ID 的动态原型。
6. 玩家拿研究升级工具右键 T1 任一可解析部位，在通用菜单安装到 `combustion_control`。
7. `trial_profile` 优先读取该塔已有的普通运行摘要；若历史不足，菜单只提示“先让塔按原配置正常运行一段”，后台补齐参考段，不创建新的计算任务。
8. 原型安装并正常运行期间，T1 集成记录最小 telemetry：燃料装入事件、process 余额、active/overload 状态段、供给中断和固定热场摘要。
9. profile 排除旧 process 余额混入、命令结算和断供区间；玩家只看到“试用正常”“需补做”或“可以判断”。
10. 报告展示燃料表现与供热是否退化，玩家选择采用、限定用途、继续改或拆除。
11. 采用后开放标准组件配方；只有安装该组件的 T1 获得 typed contribution，JEI 显示正式可制造。

当前 T1 是每 team 一个 `GeneratorData`，并非多实例注册表。V1 可维持“一队一个 active T1”，但必须在该单例中加入 incarnation、已安装组件和 host-aware modifier 查询。未来多塔化时复用 host contract；不要在本次切片中假装已有逐塔部署能力。

## 实施顺序

### Phase 0：先做可玩的 T1 体验切片

- 用两份极窄 fixture 固定 T1 发现/创新内容，不先建设通用 catalog 或工作流语言。
- 在现有绘图台接出证据钉位、InspirationSession 和 Idea 结果，先沿用最小临时 team 存储。
- 做一个只支持 T1 adapter 的通用升级工具/菜单、坯件写入和动态原型。
- 让少量测试玩家完整走一次两条短循环，重点观察纸牌是否像“汇聚灵感”、原型安装是否有创造感。

验收：玩家不读任何科学表单也能复述自己“发现了什么、为什么想到、做了什么改良”；若这一点不成立，先改玩法，不冻结 schema。

### Phase 1：冻结两种 template 与权威数据

- 根据 Phase 0 结果固定 `discovery_v1`、`innovation_v1`、project/upgrade schema。
- 注册 `TeamKnowledgeData` 与 `TeamResearchService`，定义 Observation、Idea、Finding、Innovation、WorkArtifact、prototype ID 与 revision。
- 新增 project/upgrade datapack loader、profile registry 与原子 reload。
- 实现正式的笔记、ReportInbox、固定 flow 持久化和 legacy MinigameClue 并存。

验收：两份 T1 fixture 可无损改写成精简 JSON；新档从两次真实记录形成 Idea；无候选不扣材料，退出重进盘面仍在。

### Phase 2：居民讨论与资料处理

- 研究所建筑先抽 `TownBuildingType` 注册缝，并保持六个旧 codec 名与 legacy int 顺序。
- 实现居民知识标签、讨论卡和 ActivityReservation。
- 把机械计算器改为 profile executor，并保留无 Create 的居民路径。
- 实现固定 flow 的讨论/处理班次与结果 review，不增加 task graph。

验收：居民只能提出其知识允许的卡；工作结束停在等待玩家查看；不能自动发布 Finding。

### Phase 3：把 T1 升级缝泛化

- 将 Phase 0 的工具、prototype/standard item、Menu/Screen 整理成稳定 API。
- 泛化 `ResearchUpgradeTargetResolver`、直接接口与 adapter registry。
- 为 IE T1 增加 incarnation、组件持久化、最小 telemetry 和 host-aware modifier resolver。
- 实现动态 overlay/material tint 与 prototype 安装事务。

验收：不修改 T1 原 GUI，也能从任意合法多方块部位打开统一界面；安装、重连、拆除、重启和拆塔不复制/丢失原型。

### Phase 4：投影、内容扩展与旧系统桥

- 实现 Finding/Innovation review 和 revision。
- 接入 JEI、recipe/action access 与 installed contribution。
- 保留旧研究 executor 和逐字段存档语义。
- 为 `generator_effi` 建 exclusive legacy bridge。

验收：采用设计后标准组件可制造；未安装机器不获益；旧档不重复发奖、不丢 active/level/effectData，也不把旧解锁伪造成 Finding。

## V1 总体验收

### 玩家体验

- 新玩家能在 20–30 分钟内经历一次“记录 → 纸牌 → 想法 → 再看一次 → 发现”。
- 玩家不需要理解变量、置信区间、实验组、Obligation 或 DesignStandard 才能推进。
- 纸牌局的输入和输出都能指回真实观察，不再只是给旧研究加点。
- 居民会提出有来源的不同意见，不会自动替玩家选答案。
- 工程原型必须是实体物品，并通过统一工具装到真实机器上。
- 不改任何目标机器原 GUI，也不为每项创新注册一个新物品。
- 失败、反例、断供与停机给出可继续的下一步，不清零整条路线。

### 内容创作

- 一项普通发现只需一个 project JSON、translation 和已有 profile 引用。
- 一项普通工程创新只需 project JSON 引用一个已有 upgrade；新增硬件时再写一份可复用 upgrade JSON。
- 内容作者不写公式、NBT 路径、运行状态机、知识图、研究点或客户端对象身份。
- 缺资源外观有明确 fallback；缺 provider/profile 给出定位到 project/local key 的 reload 诊断。
- KubeJS 与手写数据包生成完全相同的 schema。

### 工程架构

- 所有语义写入经过 `TeamResearchService`；投影可由资产重建。
- host 以 type + master GlobalPos + incarnation 识别，坐标复用不会串联两台设备。
- 安装物品的事实只存一份；team trial runtime 只有 prototype ID 与 host 引用，不复制 ItemStack。
- 普通 telemetry 是摘要；只有 active action/trial 收集更细记录。
- 切 team、reload 和菜单陈旧 revision 不会把旧快照或旧 host 操作应用到当前上下文。

## 后续但不阻塞 V1

- 可选高级验证页：曲线、原始来源、计算方法和复看记录。
- 把更多机器、StoneAge 工作设备和 Create/IE recipe action 接入通用 host/action adapter。
- 为不同居民性格增加讨论措辞和纸牌提示方式。
- 为常用 appearance/material 建资源库。
- 在真正需要时增加多阶段或多分支 project，而不是预先发明通用工作流语言。
- 若未来允许一队多座 T1，再把 `GeneratorData` 迁为 instance map。
- 将 Archive 泛化为可选的来源图浏览器；主流程继续使用局部证据板。

## Outcome

本计划尚未实施。当前 draft 已由“科研全过程模拟”收缩为以证据板小游戏为核心的两条固定短循环，并确定了不修改设备原 GUI 的通用升级工具、动态原型物品、后台量化 profile、精简 project 与可复用 upgrade 数据契约。下一步先用两份 T1 fixture 验证实际游玩感受，再冻结 schema。
