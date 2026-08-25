# 玩家可交互的生成式研究系统计划

- Time: `2026-08-25 10:30:52 +0800`
- Authors: `Codex（OpenAI，系统与玩法架构）；项目所有者提供目标与取舍`
- Status: `draft`
- Scope: `FrostedResearch、城镇居民与建筑、能量塔、配方/JEI/自动化权限、任务与旧研究迁移`
- Related: [`discussion/research_conversation.md`](../discussion/research_conversation.md), [`docs/research/README.md`](../docs/research/README.md), [`docs/research/gameplay-and-integrations.md`](../docs/research/gameplay-and-integrations.md), [`docs/research/research-ui.md`](../docs/research/research-ui.md)

## 目标

把研究从“交材料、攒研究点、领取效果”改造成玩家能够在 Minecraft 世界里亲自参与的认识与工程活动。后端知识图、义务编译器和技术投影必须分别表现为玩家看得见的记录、问题、任务、装置、争论、实验、原型和部署，而不能只存在于数据结构里。

玩家完整经历应当是：

```text
看见或收到一件具体事情
→ 主动记录并整理它
→ 发现值得追问的联系
→ 选择自己真正要解决的问题
→ 邀请居民提出互相竞争的解释或设计路线
→ 看见当前缺少的具体证据与能力
→ 计算、讨论、测量并在世界中搭建设备
→ 运行真实实验，处理污染区间和意外
→ 解释结果并决定接受什么结论
→ 把结论整理成 Finding / Procedure
→ 制造有编号的实体原型并进行现场试验
→ 签署 DesignStandard、训练居民并逐处部署
→ 配方、JEI、自动化、机器行为和城镇指标随之改变
```

系统的总不变量是：

> **系统、NPC 和机器可以在玩家签发的任务范围内生成数据制品；只有玩家动作能够产生语义承诺。**

语义承诺至少包括 `ArchiveObservation`、`OpenInquiry/ReframeInquiry`、`AdoptHypothesis`、`IssuePlanRevision`、`StartRun`、`AttachInterpretation`、`PublishFinding(mode)`、`PublishProcedure`、`AuthorizePrototypeDesign`、`DeployFieldTrial` 和 `SignDesignStandard`。它们必须由玩家显式执行，并记录 actor UUID、team epoch、玩家所见 aggregate revision 和来源。规则引擎可以指出缺口，居民可以提出草案，计算器可以生成结果，控制台可以自动暂停/停止已经签发的 Run，但它们都不能替玩家一路自动研究完毕。

多人游戏中，普通 team 成员可以记录、整理、讨论、测量和执行已经签发的任务；建立/改写 Inquiry、发布资产、授权原型和签署/撤回标准需要 `RESEARCH_COORDINATOR`。V1 默认把 team owner 设为 coordinator，并允许其委派其他成员；若 Chorda 未提供合适权限位，该委派表由 `TeamKnowledgeData` 持久化。

## 不采用的玩法

- 不显示“理论 63%”或一条总研究进度；允许显示具体劳动或采样进度，例如“有效记录 14/24 小时”。
- 不以隐藏成功率决定自然规律是真是假；随机性只来自世界本身、测量噪声、故障和居民能否完成一次明确工作。
- 不设置任意的最大假设迭代次数。被反驳的路线成为有来源的负知识，可以归档、复用或在适用范围改变时重开。
- 不让居民、计算器、任务奖励或实验室被动生产通用研究点。
- 不要求玩家猜隐藏物品组合来“合成想法”。系统必须解释记录之间为什么可能有关，但不透露答案。
- 不让普通机器逐 tick 向知识系统写入数据。只有正式链接的实验对象高频采样，日常运行只形成压缩摘要。
- 不让 Finding 魔法式提高所有机器性能。科学认识、制造权限、实体改装和城镇部署是不同步骤。
- 不把现有配对纸牌小游戏继续当成所有理论研究的通用表示；它最多保留为旧档兼容或特定文献复原玩法。

## 已验证的现状约束与先决重构

以下不是远期优化，而是首条 T1 切片开始前必须解决的事实：

1. 当前 `FHSpecialDataTypes.GENERATOR_DATA` 是每 team 一个 `GeneratorData`，只保存一个 `actualPos/dimension`；`GeneratorState#regist` 会让新位置接管。因此当前并不存在“多塔/逐塔组件”。首切片保留“一队一个 active tower”的游戏规则，但给该单例增加稳定 `GeneratorInstanceId/incarnation`、host type 和 `UPGRADE_SLOT`。新位置接管会结束旧 incarnation；拆塔前可正常取回组件，异常拆解则进入明确的 recoverable detached-component 记录。未来若游戏设计允许多塔，再把相同 instance contract 提升为 map，不让研究首切片先改变城镇核心平衡。
2. 当前 `GeneratorData#consumesFuel` 只能读取 team-wide variant，调用链没有 host/组件上下文。它必须改为通过当前 `GeneratorInstanceId` 调用 `GeneratorModifierResolver(host, fuel, mode)`，否则“只让已安装的塔获益”无法实现。
3. 现有 `TownOperationalHistory` 和 `TownOperationalStatusProvider` 没有燃料装入事件、process 轨迹或实际供给连续性；库存储备只能说明“可能还有燃料”，不能证明实验期间没有断供。首切片要新增下文定义的 T1 telemetry，不把 R2/R3 伪装成现成日报。
4. 现有档案图和 `ResearchArchiveViewCache` 绑定旧 `Research` DAG 与 `TeamResearchData`。可以抽取的是 `PanZoomViewport`、摄像机、布局/裁剪和视觉语言；Claim/Evidence/Asset 异构图需要新的 view model，不能直接复用旧 final/package-private 图组件。
5. 机械计算器注册受 Create 是否加载控制。居民人工计算必须始终可用；所有 worksheet UI 和义务 action 在计算器不存在时仍可完成。
6. 新 subject profile 使用真实 host ID `frostedheart:generator_t1`。旧研究字符串 `generator_T1` 含大写字符，只能通过显式 legacy alias 映射，不能直接构造 `ResourceLocation`。

## 玩家认识的对象

| 玩家用语 | 含义 | 谁能产生 | 是否能直接改变玩法 |
|---|---|---|---|
| 报告 `Report` | 值得注意但尚未整理的信号、事故、口述或日报 | 世界、居民、玩家、仪器 | 否 |
| 观察 `Observation` | 已记录的“何时、何地、对什么实际看见了什么” | 玩家或被委托的观察者 | 否 |
| 想法 `IdeaCandidate` | 若干记录之间可能存在的关系或目标缺口 | 规则提示、居民建议；玩家确认 | 否 |
| 调查 `Inquiry` | 玩家选择的问题框架、对象范围和验收标准 | 玩家 | 只产生工作空间 |
| 假设 `Claim` | 可以被观测结果支持或反驳的解释/设计主张 | 玩家采纳居民或系统草案 | 否 |
| 证据 `Evidence` | 观察、计算或实验结果对一个 Claim 的关系 | 玩家附入论证 | 否 |
| 发现 `Finding` | 在明确适用范围内被团队接受的事实或性能结论 | 玩家发布 | 开放理解与原型路线 |
| 程序 `Procedure` | 能被另一操作者复现的测量、制造、维护或处置方法 | 玩家发布 | 解锁可靠动作与自动执行条件 |
| 设计标准 `DesignStandard` | 通过实验室与现场验证的确定设计修订 | 玩家签署 | 解锁普通制造、自动化与标准参数 |
| 技术投影 | 下游系统可快速读取的 access 与按 host 安装贡献 | 服务端由资产和实例事实编译 | 是，但投影本身不是事实源 |

一次调查可以产出多个成果；同一个 Finding 也可以由不同调查路线得到。稳定成果 ID 表示语义结果，不表示一条固定研究路线。

## 玩家使用的载体

| 载体 | 处理的事情 | 现有/新增 | 关键边界 |
|---|---|---|---|
| 研究笔记 `research_notebook` | 现场记录、收件箱、钉住当前义务、查看时间线 | 新增物品与菜单 | 物品只存当前书签；权威记录属于 team |
| 绘图台 `frostedresearch:drawing_desk` | 整理记录、建立 Inquiry、采纳假设、签发文档、解释与发布 | 保留 ID，重做研究语义 | 保留双格模型、所有权、纸张和笔槽 |
| 镇长印章/城镇界面 | 把城镇事件、日报与目标送入研究收件箱 | 复用 | 城镇状态不是知识；玩家决定是否研究 |
| 机械计算器 `frostedresearch:mechanical_calculator` | 执行具体 CalculationWorksheet | 保留 ID 和 Create 动力表现 | 不再生产或转移通用点数 |
| 研究所 `frostedheart:research_institute` | 排班讨论、档案、人工计算、值班、复核与教学 | 新增城镇工作建筑 | 建筑只提供人员/设施能力，不持有知识图 |
| 实验控制台 `frostedresearch:experiment_console` | 冻结计划、链接装置、能力检查、运行和采样 | 新增 BE | 实验条件来自真实世界对象，而非材料倒计时 |
| 研究连接器 `frostedresearch:experiment_linker` | 将控制台与机器、仓库、仪器和采样点绑定 | 新增工具 | 服务端重新解析实际对象及其 incarnation |
| 原型工作台 `frostedresearch:prototype_bench` | 按签发的设计修订消耗真实 BOM，制造有序列号的原型 | 新增方块 | 不能普通批量合成；原型是实体，不是权限标记 |
| 统一研究文档 `frostedresearch:research_document` | 承载观察副本、计算单、计划、结果、评审册与蓝图引用 | 新增物品 | 仅保存 `team/document/revision/type`；丢失可重印 |
| 研究档案 | 查看全队知识、来源链、旧失败与技术状态 | 抽取/泛化现有 Archive UI 基础 | 图是只读投影；局部 Inquiry 是主要工作界面 |

现有 `lab_block_cabinet`、`lab_control_panel_*` 等实验室装饰通过 tag 为研究所提供 `ARCHIVE`、`DISPLAY`、`WORKSPACE` 等设施能力，不把旧世界已经放置的普通方块原地改成 BE。

## 从新档开始的解锁与教学

解锁依靠实际设施能力，而非额外的抽象研究等级。

### 1. 降落与现场笔记

初始场景向每名玩家提供一本研究笔记和一支基础炭笔。笔记不是教程书，也不预装科技树。

玩家对准一个可观察对象潜行使用并保持约 1 秒：

1. 准星旁出现羽毛图标与对象名称；
2. 服务端读取对象适配器和当前位置上下文；
3. 播放翻页声，笔记新增一条 `ObservationDraft`，玩家用语为“未整理记录”；
4. 屏幕只提示“已写下未整理记录”，不把它称为正式观察，也不弹出研究名称或奖励。

笔记自身只能形成粗略记录。例如没有温度计时只能记录“极寒”“附近存在热源”等档位；拥有仪器后才写入数值、单位和量程。第一段教学要求玩家分别记录一个暴露位置和一个热源影响位置，然后打开笔记比较。若真实 `WorldTemperature.air/heat` 没有形成差异，系统不会伪造联系，而是提示换位置或时段。

这段教学只建立三个习惯：观察不是解释；未测量的变量明确显示为“未知”；同一件事在不同条件下比较才可能形成问题。它不强迫玩家在开局立即完成一项正式研究。

### 2. 绘图台与个人研究

建成现有绘图台后，玩家第一次把未整理记录正式归档：

- `EXAMINE_SLOT` 放样品、拓印、旧文档或相关物件；
- `PAPER_SLOT` 放记录介质；
- `INK_SLOT` 放现有 `IPen`；
- 纸和笔只在正式归档、签发计划、发布结论时消耗，随手记录不按页收费。

绘图台由“纸牌局”改为四个主页面：`收件箱`、`证据板`、`进行中`、`知识档案`。玩家在城镇成立前可以独立完成观察、简单计算和小型顺序对照；系统不会用“必须有 NPC”阻断所有早期研究。

### 3. 城镇、研究所与居民协作

城镇成立后，镇长印章中的事件、历史和运营指标出现“送往研究”动作。建立研究所后，玩家能够把一项具体义务委托给居民，而不是给建筑分配“科研人数”并等待点数上涨。

研究所开放：讨论、档案整理、正式计算、实验设计、实验值班、独立复核和知识教学。居民数量决定并行工作，教育与经历决定能承担哪些角色，设施决定能使用哪些方法。

### 4. 正式实验与工程化

实验控制台开放后，玩家才能签发正式计划、链接世界装置并保存高频运行轨迹。工程 Inquiry 在需求、设计主张和测试计划齐备后即可授权一次性原型；由科学 Finding 派生的设计则必须先具备该设计所依赖的 provisional/established Finding。完成实验室试验、现场试验、复核和教学后才能签署标准并转入普通生产。

### 5. 设施可达性，禁止自举环

| 设施/物品 | 首次获得条件 | 无该设施时的替代 | 禁止的前置 |
|---|---|---|---|
| 研究笔记、炭笔 | 开场场景发放；另有无研究前置的基础配方 | 无 | 不得要求任何研究资产 |
| 绘图台 | 第一阶段避难所材料即可合成，或由开场任务明确发放配方 | 笔记可继续收集 Draft，但不能正式发布 | 不得要求完成旧研究 |
| 研究所 | 城镇成立并达到基础建筑材料条件 | 玩家亲自讨论/整理；简单项目仍可继续 | 不得要求 Finding/DesignStandard |
| 机械计算器 | 沿用 Create 工业能力和配方 | 受教育居民按班次人工计算 | 不得成为任何结论的唯一入口 |
| 实验控制台、连接器 | 研究所落成后开放基础制造，或无城镇时由较昂贵的手工配方制造 | 定性/非正式试验可继续，但不产生正式高频 Run | 不得要求先完成正式实验 |
| 原型工作台 | 已能制造实验控制台后开放基础制造 | 可先设计但不能制造实体原型 | 不得要求先签署 DesignStandard |

具体配方由整合包数据定义，但上述依赖是系统契约；任务或数据包不得重新引入“先研究实验设备，才能做解锁实验设备的实验”。

## 观察：玩家到底怎样记录世界

### 从信号到正式观察

```text
ResearchSignal（瞬时世界信号，不是知识）
→ ObservationGate 判断是否有人注意/是否已委托记录
→ 玩家/仪器形成 ObservationDraft，第三方来源形成 Report
→ 两者进入同一个 ReportInbox
→ 玩家执行 ArchiveObservation
→ immutable ObservationRecord
→ 钉住/送入 Inquiry 时只建立引用，不再改写记录内容
```

世界事件不自动等于知识。只有下列情况可以越过观察门：

- 玩家正在手持笔记主动记录；
- 玩家安装并启用了一个有明确期限的记录任务；
- 居民在本班次被分配到相关工作或观察任务；
- 正式实验计划已经签发并由玩家启动；该事前授权允许 Run 封存其测量为 ObservationRecord，无需玩家逐样本确认；
- 不可重复的重大事故自动进入收件箱，但仍需整理后才成为正式证据。

### 六种实际记录动作

1. **现场查看**：手持笔记对方块、实体或位置潜行使用，保持记录动作。基础记录只包含肉眼可见/适配器允许公开的状态。
2. **仪器读数**：手持温度计、流量计等仪器时记录数值、单位、量程与校准状态；仪器超量程会留下断线和原因。
3. **连续记录**：在笔记或控制台中签发一个记录任务，把记录器链接到对象，指定变量、间隔和停止条件；签发相当于事前授权，任务结束后原始记录不可改写。
4. **城镇日报**：玩家在镇长印章把 `TownHistoryEntry`、`TownSignalEvent` 或建筑日报送入共同收件箱。
5. **访谈证词**：与居民交谈时选择“谈谈这条记录”，把观察或 Idea 作为话题；居民只会外化与其经历和私人知识相关的内容。
6. **样品/文献检查**：将物品、拓印或旧档案放入绘图台检查槽；默认不消耗原物，只有明确标记为破坏性分析的 Procedure 才消耗。

每张观察卡固定显示：

- 谁在何时、何地记录；
- 对象、实例和适用上下文；
- 真正测到了哪些变量，单位与统计窗口；
- 来源类别：肉眼、口述、班次摘要、仪器连续记录、正式实验；
- 尚未测量或中断的变量；
- 为什么进入收件箱；
- 可展开的原始来源与后续处理历史。

不显示一个误导性的“可信度 83%”。玩家看到的是“口述”“估计”“连续记录”“校准过”“中间缺失 37 分钟”等可理解事实。

重复日报按对象、变量、上下文和时间窗口折叠成“同类报告出现 N 次”，显著变化和异常单独展开。普通运行只保存小时/班次/日摘要；正式实验结束后把高频轨迹压缩成统计摘要、代表曲线和事故段。

收件箱中的每张卡提供且只提供以下动作：`归档`、`降低此类提示频率`、`钉到证据板`、`询问报告人`、`送往已有 Inquiry`。玩家把 Draft 正式化时，要在绘图台确认对象、变量、上下文与来源；没有能力补出的字段继续标为未知，不能靠手填变成测量值。玩家可以自由写标题和备注，但所有匹配、编译和资产判据只读取类型化字段。

## 想法：怎样从记录中发现值得研究的关系

系统只生成 `IdeaCandidate`，从不自动开启 Inquiry。

### 证据板操作

玩家在绘图台执行：

1. 从收件箱选择 2–6 张记录钉到证据板；
2. 选择观察视角：`比较差异`、`寻找重复`、`检查先后`、`查看冲突` 或 `设为改进目标`；
3. 系统只高亮真实可比较的字段和可解释连线；
4. 玩家点击连线查看“为什么可能相关”；
5. 玩家选择“保留为想法”或拆掉连线继续整理。

例如：

> 可能有关联：燃料储备持续下降，但能量塔连续供热，物流没有报告中断。来源为 3 个不同结算窗口。

系统不能显示“最终解锁受控进气组件”。若玩家钉住不相关的卡片，不消耗材料、不判失败，只显示“当前没有可声明的共同变量”。

局部规则至少包括：同型对象差异、同对象前后差异、重复共现、预测偏差、证词冲突、事故链和城镇目标缺口。强模式可以自动把一张候选卡放入收件箱，但玩家仍要亲自建立问题。新记录再次匹配同一候选时只更新“新增 N 条来源”，不刷出重复想法。

## 问题框架：玩家决定研究什么

点击“建立调查”后，玩家必须选择框架，而不是选择一项预先揭底的科技：

| 框架 | 玩家填写 | 典型义务 | 可形成的主要资产 |
|---|---|---|---|
| 解释 `EXPLAIN` | 要解释的变化、对象范围、上下文 | 竞争假设、区分性预测、干预/自然对照 | Finding、Procedure |
| 诊断 `DIAGNOSE` | 故障表现、必须恢复的状态、允许停机范围 | 复现、排除根因、验证修复 | Negative/Positive Finding、Procedure |
| 复原 `RECOVER` | 物证/证词来源、要复原的动作或结构 | 鉴定来源、重建步骤、另一操作者复做 | Procedure、后续 Idea |
| 工程 `ENGINEER` | 要改善的指标、不得退化的指标、适用对象 | 需求、候选设计、原型、实验室与现场试验 | 性能 Finding、Procedure、DesignStandard |

表单由观察里的语义字段预填，但由玩家确认：

- 研究对象：当前实例、同类型机器、一个城镇建筑类别或全队流程；
- 目标变量/现象；
- 适用上下文：燃料、负载、天气、居民岗位等；
- 暂时排除的范围；
- 验收政策：`探索性`、`暂定可用`、`标准级`；
- 工程问题的数值目标与不可牺牲项。

界面立即说明取舍：“仅研究这一座塔更容易完成，但结论不能直接推广到所有 T1”；“允许暂定结论可以制造原型，但不能普通量产”。它仍不显示最终奖励。

确认时消耗纸和笔，生成一个有版本的 Inquiry 文档。修改框架不会删除旧证据；若只是收窄/拓宽范围则产生新 revision 并重新编译义务，若目标语义已经改变则从旧 Inquiry 派生一个新 Inquiry，历史连线仍可追溯。

## 居民怎样提出候选解释

### 私人知识不是入队礼包

每名居民以 UUID 绑定一个独立知识覆盖层：

- `Concept`：理解哪些变量和术语；
- `MechanismSchema`：知道哪些可能的因果结构；
- `CaseMemory`：亲历过哪些具体案例；
- `ProcedureKnowledge`：会怎样操作、测量或维护；
- `DesignPattern`：见过哪些工程解法。

难民加入不会立刻把这些内容变成团队 Finding。知识必须通过访谈、相关岗位工作、正式讨论或教学外化。

### 现场交谈

玩家打开一名居民的对话，选择笔记中的一张记录或 Inquiry 作为话题，然后可问：

- “你见过类似情况吗？”——可能得到有来源的 Testimony；
- “你觉得可能是什么原因？”——可能得到 DraftHypothesis；
- “怎样才能分辨？”——可能得到 Prediction 或 Procedure 草案；
- “谁还可能知道？”——指出另一名居民或知识来源。

回答只使用居民公开知识和私人知识包，不读取隐藏世界真相。低教育的锅炉工可以准确描述声音、火焰和操作经验；高教育居民可以把它形式化成变量与预测。两者需要合作，而不是简单比较一个“科研产量”数值。

### 正式讨论班次

玩家在研究所任务板：

1. 选择 Inquiry 和讨论目的：`提出解释`、`挑战当前解释`、`回忆案例`、`寻找类比` 或 `提出设计`；
2. 钉住 3–6 份观察/旧发现；
3. 邀请 2–4 名具体居民；
4. 选择一个工作班次并下达任务；
5. 班次结束后打开会议纪要。

服务端生成讨论草案的固定过程是：从团队 public knowledge 加本次参会者愿意外化的 private packet 取出 `MechanismSchema/CaseMemory/DesignPattern`；把 schema 的 cause/context/outcome 槽位与钉住记录中的类型化变量做局部匹配；实例化可观测预测与反证条件；按记录覆盖、与现有候选的结构差异、居民可表达复杂度排序并去重。它从不读取隐藏世界答案，也不靠一次“讨论成功率”决定有无进展。若没有任何 schema 能匹配，会议纪要就明确产出“缺少概念/案例”的 KnowledgeGap，并给出访谈、文献、观察或探索性运行路线。

会议纪要用 3–6 张带发言人的卡表示：候选假设、异议、证词、预测或设计草案。每张卡必须有“依据”“预期现象”“什么会反驳它”“当前局限”。属性作用为：

- 智力决定能同时组合多少概念和关系；
- 教育决定能否使用正式变量、计算与复杂仪器；
- 工作熟练度和背景决定能认出什么具体模式；
- 沟通表现决定证词能保留多少上下文；V1 由现有智力、教育、相关工作熟练度和知识背景共同派生，不凭空假定 `Resident` 已有 communication 字段；若以后增加独立属性，必须补 Codec、生成、迁移和 UI；
- 复核角色决定能否指出控制或范围问题。

这些属性决定可做性和表达深度，不决定一个观点凭概率“变真”。讨论完成后所有草案停在 `AWAITING_PLAYER_REVIEW`。玩家逐张选择“采纳为候选”“继续追问”“保留备查”或“归档”。只有采纳动作才创建 Claim；没有证据的“不同意”只表示不追这条路线，不把它写成已被反驳。

发现/诊断型问题默认要求至少两个结构不同的竞争解释；工程型问题默认要求一个明确性能目标和至少两个候选方案。若知识不足，玩家可以明确签署“探索性单路线”例外，但所得结论只能是 provisional。

### 居民任务生命周期与后期自动化

讨论、计算、值班、复核和教学共享同一条可恢复的任务生命周期：

```text
DRAFT
→ 玩家下达 QUEUED
→ 人员与设施满足后 RUNNING
→ 人员离岗/材料不足时 WAITING
→ 条件恢复后继续 RUNNING
→ 产生制品后 AWAITING_PLAYER_REVIEW
→ 玩家采用为 APPLIED，或保留为 ARCHIVED
QUEUED/RUNNING/WAITING → 玩家取消为 CANCELLED
```

讨论卡、worksheet 和 review report 都是带输入与 provenance 的 immutable `TaskArtifact`；任务完成不能直接创建图节点或自动排下一个义务。只有玩家执行 Adopt/Attach 才能进入 `APPLIED`。研究所拆除只把任务变成 `UNASSIGNED`，已有工时和制品仍在 team 数据中。任务使用 lease，保证同一 CalculationTask 不会同时被居民和机械计算器结算。

每个任务还创建 `ActivityReservation(start, end, role, opportunityCost)`。现有居民只有一个主要工作位置；参加会议、人工计算或实验值班时必须暂时离开原岗位，城镇排班界面显示对应生产损失或替补。一个物流员不能在同一班次既参加讨论、又负责实际送货、还兼任实验复核。标准级独立复核至少需要第二名合格居民；只有一名研究居民时仍可做 provisional 结论，不形成中期城镇的死锁。

成熟城镇可以由玩家签发一个范围明确的 `ResearchMandate`，允许居民在指定 Inquiry、预算、对象、方法和截止条件内自动重复采样、执行已验证 Procedure 或补做复现。Mandate 达到目标、预算或期限后必须停在玩家审阅；它不能采纳新假设、改变 scope、解释证据、发布资产或递归签发新的 mandate。这样减少后期重复点击，而不把认识决策交给后台。

## 研究义务怎样编译并呈现

义务是当前知识相对于玩家目标的具体缺口，不是任务树节点，也不持久化一个 `completed` 布尔值。

```java
record ObligationKey(
    InquiryId inquiry,
    ResourceLocation ruleId,
    List<KnowledgeRef> normalizedTargets,
    ObligationQualifier qualifier
) {}

record ResearchObligation(
    ObligationKey key,
    ObligationType type,
    RequirementExpr satisfiedWhen,
    Set<ObligationKey> dependencies,
    List<ActionOption> actions,
    ReasonTrace reason,
    Set<ArtifactType> expectedArtifactsForUi
) {}

record ActionOption(
    ActionId id,
    ActionVerb verb,
    ArtifactContract targetAwareResult,
    ActorRequirement actors,
    FacilityRequirement facilities,
    InputRequirement inputs
) {}
```

`RequirementExpr` 是可序列化的类型化表达式，例如 `HasClaim`、`CountEvidence`、`HasDiscriminatingRun`、`HasIndependentReplication`、`RivalsAddressed` 和 `HasReviewedArtifact`，不能用任意脚本布尔值直接改 team 数据。它只匹配已经由玩家采纳/附入、且 Inquiry、Claim、Plan、scope 和 revision 都兼容的制品。裸 `ArtifactType` 集合只用于 UI 说明，绝不能靠“某义务能产 Evidence”反推依赖，否则不同 Claim 的 Run/Review 会被错误连接。

### 编译时机与算法

`ObligationCompiler` 只依赖已承诺的知识图、`InquiryRevision`、catalog/rule revision；新增/修改观察、Claim、已附入的分析、计划、Run、评审或资产时，只重新编译受影响的 Inquiry：

1. 从 Inquiry 邻域建立 `FactIndex`；
2. 按问题框架选择规则集；
3. 规则根据缺失的制品发射 obligation；
4. 用结构化 `ObligationKey` 合并同一个缺口，并合并所有原因链；
5. 每条规则显式声明 target-aware requirement 和 dependency；`ActionOption` 的 `ArtifactContract` 指定会为哪个 Inquiry/Claim/Plan/scope 产生什么；
6. 从知识图和制品重新求值满足状态；
7. 给出语义上的当前 frontier 和建议顺序，但绝不自动选择或启动行动。

居民换班、箱子材料变化、机器停转和仪器移动不触发语义重编译。独立的 `ActionResolver` 按当前居民、设施、仪器、材料、世界绑定和 lease 为 frontier 生成实时可执行性 overlay。这样 `READY/BLOCKED_*` 可以每次打开界面或相关能力变化时刷新，而不会让整个知识工作流抖动。编译缓存键必须包含 `inquiryRevision + committedGraphRevision + catalogRevision + ruleRevision`。

派生状态只有：

- `SATISFIED`：判据现在已经满足；
- `WAITING_DEPENDENCY`：前置缺口尚未解决；
- `FRONTIER`：语义前置已经满足；
- `READY`：`ActionResolver` 判断至少一种实际行动当前可执行；
- `BLOCKED_ACTOR/FACILITY/INPUT/BINDING`：frontier 已到达，但实时缺居民、设施、材料、仪器或世界绑定。

旧规则不再发射某个 key 时，它不会伪装成当前 `SUPERSEDED` obligation。另存只读 `ObligationOccurrence/TransitionLog`，记录何时曾展示、满足、消失以及 `replacedBy`，用来回答“为什么不见了”；当前 obligation 仍不持久化 completed 状态。

玩家在 UI 中看到三个栏位：`当前必须解决`、`可以并行`、`可选加强`。义务卡固定显示：

```text
当前缺少：可比较的 T1 正常运行基线
为什么：现有燃料日报不能排除超载和短时断供
满足条件：同一燃料、普通模式、供给连续的 24 游戏小时（24000 active ticks）
玩家路线：链接塔与输入记录器后启动顺序对照
居民路线：安排物流员连续监控一个班次
当前缺口：尚未安排实验值班居民
完成后能区分：供应问题 / 超载问题 / 设计效率问题
```

玩家可以把最多三张 READY 卡钉到笔记 HUD。义务变动时必须显示“为什么新增/为什么已满足/被哪个修订替代”，避免后端规则神秘地移动目标。

### 四种框架的义务骨架

- 解释：刻画现象 → 正式定义变量 → 生成替代假设 → 推导不同预测 → 识别混杂 → 取得测量能力 → 区分性运行 → 分析 → 独立复现 → 定义范围 → 评审。
- 诊断：稳定复现故障 → 列出根因 → 逐项隔离 → 验证修复 → 写异常处置与验收 Procedure。
- 复原：确认来源 → 重建步骤/材料 → 第一次真实执行 → 由未参与者照文档复做 → 发布 Procedure。
- 工程：定义性能需求 → 生成至少两个设计 → 授权原型 → 实验室试验 → 修订 → 现场试验 → 维护/操作验证 → 独立复核 → 标准化 → 部署。

界面上的“理论”“实验”“工程”只是当前前沿义务的分类，不写入存档作为单向状态机。

## 计算、讨论、测量、搭建与分析的实际玩法

### 计算

玩家从义务卡选择“建立计算任务”，在绘图台得到一张 `CalculationWorksheet`：

1. 选择数据区间；
2. 将每个公式符号绑定到观察、常量或明确假设；
3. 选择输出单位和需要的精度；
4. 签发后把任务单放入机械计算器，或委托给研究所居民。

机械计算器保留 Create 转速、滚筒、声音、`0 < |speed| <= 64` 的工作区间和超速暂停。它不积攒点数，而是按实际机械功完成确定的 work units。停转、超速、拆机或取出任务单只暂停；team 侧任务保留已完成工作。

输出是一张有来源的表、曲线、推导或诊断，例如“单位燃料的有效运行 ticks”“同负载下两组平均值”“预计实验区间”。缺输入就保留空位并指出缺什么；单位不匹配就指出具体符号。计算器不会补造数据，也不会选择获胜假设。玩家必须点击“附到变量定义/预测/分析”，结果才进入论证图。

人工计算使用同一任务类型。居民教育决定可用公式模板，智力决定能处理的关系复杂度，设施决定耗时与诊断能力；它们不改变数学答案。

### 测量

瞬时仪器需要玩家到现场使用；连续测量需要实际放置/链接记录能力并指定窗口。仪器提供：变量、单位、量程、采样间隔、精度档位和校准 Procedure。没有正确工具时，系统允许保留定性观察，但不会悄悄给出精确数值。

控制台可以直接采样机器适配器已经公开的内部运行量；这表示机器仪表可读，不表示玩家知道所有隐藏实现字段。每个 `VariableKey` 必须声明单位、比较语义、可观察条件和聚合方式。

### 实验计划与搭建设备

玩家在实验页选择目标假设和一项能让竞争假设产生不同预测的干预。计划助手可以提出候选布局，但玩家必须选择：

- 并行控制组/实验组，或同一对象的前后交叉对照；
- 实际世界对象；
- 要改变的变量；
- 要保持相同、控制在范围内或只监控的变量；
- 测量变量、采样间隔与仪器；
- 供给、值班、持续时间和停止条件。

计划状态为 `DRAFT --IssuePlanRevision(player)--> ISSUED`；修改会建立新 revision，并把旧 revision 标为 `SUPERSEDED`，但旧 Run 永远引用原计划内容。

手持连接器先点控制台，再依次点击方块、IE 多方块主节点、能量塔、仓库接口或采样位置，并给它们分配 `CONTROL`、`TREATMENT`、`INSTRUMENT`、`SUPPLY` 角色。服务端使用对象适配器重新解析主体，坐标之外还绑定实例 incarnation，避免方块被拆后把两代机器混成同一实验。V1 直接链接要求同维度、绑定时目标已加载且位于数据定义的控制台半径内；开始 Run 后由服务器 signal sink 按 subject 订阅，控制台自身卸载不会抹掉目标已经产生的数据。

控制台不检查唯一多方块蓝图，而是比较能力集合：能否测量所需变量、执行干预、保持供给、覆盖量程并安排值班。检查项逐条显示 `PASS`、`QUALIFIED` 或 `UNRESOLVED` 及修复方法。存在关键 `UNRESOLVED` 时只能由玩家确认 `RunMode.EXPLORATORY`；它可以产生观察和 Idea，但不能满足正式区分性实验义务，且不能调用名义上的 formal start。

### 实验运行

Run 使用明确状态：

```text
PREPARED
→ 玩家 Arm 后 ARMED
→ 玩家 Start(mode) 后 RUNNING
↔ 条件中断/恢复时 PAUSED
→ 达到停止条件或玩家封存时 SEALED
→ 玩家主动中止且不再续跑时 ABORTED
```

`SEALED/ABORTED` 后 trace 不再改写。已经签发的 mandate 可以自动暂停、恢复和达到停止条件，但第一次 `Start` 必须由玩家执行。玩家按下 `Arm` 时控制台再次冻结对象、计划和仪器 revision；运行中玩家要真实地：

- 加载燃料和材料；
- 操作阀门、模式、负载或原型；
- 保持对照条件；
- 处理机器故障；
- 亲自值守或安排明确居民值班；
- 决定暂停、继续或提前停止。

控制台显示实时曲线和三类灯：蓝色为正在记录，绿色为控制条件在范围内，黄色为暂停或本段不能比较。供能中断、变量漂移、区块卸载、居民离岗和对象更换形成带起止时间的 incident；它们不会清空已经有效的数据。

停止后分别计算：干预达到率、测量覆盖、控制范围驻留率、输入连续性、可比时间窗、值班、仪器校准和意外故障。结论是“哪些时间段能用于哪些比较”，不是一张成功/失败彩票。

### 分析与复核

分析页面要求玩家选择 Run、基线/处理区间、排除的事故段以及使用的 Procedure。计算器或居民执行：时间对齐 → 按输入/负载归一化 → 求差异与测量误差 → 对照 PredictionMatrix。

输出 `AnalysisReport(AWAITING_PLAYER_REVIEW)`，对每个 Claim 给出 `SUPPORTED`、`OPPOSED`、`NON_DISCRIMINATING` 或 `OUT_OF_SCOPE` 草案，并展示曲线和理由。玩家逐条选择“作为支持附入”“作为反证附入”或“只保留为观察”；随后报告分别成为 `ATTACHED` 或 `ARCHIVED`。只有 `ATTACHED` 参与 RequirementExpr 并创建 Justification。

独立复核必须由未参加该 Run/analysis 的居民执行，产出遗漏控制、适用范围和可复现性意见。玩家可以在紧急模式下签署跳过复核，但只能发布 provisional 资产。

## 研究结果怎样成为稳定资产

### Finding

Claim 保持自己的 `PROPOSED/SUPPORTED/OPPOSED/PARKED` 论证状态，不能原地“升级成 Finding”。玩家执行 `PublishFinding(mode=PROVISIONAL|ESTABLISHED)` 时创建不可变的 `FindingRevision`；正式发布建立新 revision 并引用/替代先前暂定 revision。每个 revision 同时拥有：

- maturity：`PROVISIONAL` 或 `ESTABLISHED`；
- polarity：`POSITIVE` 或 `NEGATIVE`；
- kind：`DESCRIPTIVE`、`CAUSAL`、`DIAGNOSTIC` 或 `PERFORMANCE`；
- lifecycle：`ACTIVE`、`NEEDS_REVIEW`、`SUPERSEDED` 或 `WITHDRAWN`；
- 完整 scope、依据、发布者、catalog/definition revision。

不同 Finding 不能共用一个“必须真实干预”的硬规则：

| kind | 最低证据政策 |
|---|---|
| `DESCRIPTIVE` | 类型化 `ReplicationPolicy` 要求的多时段/多来源观察与明确 scope；不强迫干预不可干预的自然现象 |
| `CAUSAL` | 区分竞争解释的真实干预；内容可显式允许自然/准实验替代并说明限制 |
| `DIAGNOSTIC` | 故障复现、根因隔离、修复后不再复现或等价判据 |
| `PERFORMANCE` | 基线/处理可比、实际运行、性能与副指标均被测量 |

暂定 Finding 至少满足对应 kind 的单次可解释证据和明确 scope；它可以作为探索性原型依据，但不能单独签署生产标准。正式 Finding 还要求内容定义的独立复现、强竞争解释处理、所有关键有效性异议处置、完整变量/条件、独立复核，以及 coordinator 执行正式发布。`ReplicationPolicy` 明确到底要换对象、批次、时段、操作者或环境，不能笼统写“任选一个”。

稳健反证可发布 polarity 为 `NEGATIVE` 的同类 Finding，例如“在普通模式和焦煤范围内，短时物流中断不是本次高消耗的主要原因”。它会抑制相同范围内反复生成死路线，但不声明范围外永远不可能。`NEEDS_REVIEW` 仍可作为历史和当前警告来源，但不能默默支撑新的标准；`WITHDRAWN` 不再满足新的义务或设计依据。

### Procedure

```text
MethodDraft
→ 一次真实执行后 TRIALLED
→ 另一操作者只按文档复做且验收通过
→ resolver 派生 REPRODUCTION_ELIGIBLE
→ 玩家 PublishProcedure
→ immutable VERIFIED_PROCEDURE revision
```

`REPRODUCTION_ELIGIBLE` 只是“现在可以发布”的判定，不是资产。Procedure 必须包含目的、输入、能力需求、有序步骤、容差、验收检查、异常处置、适用范围和真实 execution refs。它可以独立于因果 Finding 形成，例如“如何校准 T1 燃料输入记录器”。已发布 Procedure 还可进入 `NEEDS_REVIEW/SUPERSEDED/WITHDRAWN`，但旧 revision 与执行记录不删除。Procedure 解锁可靠测量、维护、操作或居民自动执行条件，不直接提高机器效率。

### DesignStandard

设计稿、证据资格和标准资产是三种对象：

```text
DesignRevision: DRAFT --AuthorizePrototypeDesign(player)--> AUTHORIZED
                AUTHORIZED --创建 V2--> SUPERSEDED_BY(V2)

resolver 只派生：LAB_ELIGIBLE / FIELD_ELIGIBLE / STANDARD_ELIGIBLE

玩家 DeployFieldTrial：显式接受 lab basis 并建立现场试验
玩家 SignDesignStandard：显式接受 standard basis 并创建 DesignStandard
```

不存在由后台自动写入的 `LAB_QUALIFIED/FIELD_QUALIFIED/STANDARDIZED` 设计状态。DesignStandard 必须记录：依据 Finding/Procedure、设计参数与容差、BOM、适用 host、性能要求、维护 Procedure、签署前由至少一名普通独立操作者完成的可操作性验证、实验室 Run、现场 Run、签署人和版本。签署后的批量岗位教学是部署动作，不和签署前的独立复做混为一谈。

签署时冻结参数、容差、BOM、action 语义引用及 definition revision。catalog reload 只建立新 snapshot、检查兼容性并生成诊断/复核义务，不能让同一个已签标准的 `+0.1` 或 BOM 静默改变。无需路径敏感 hash；稳定 `ResourceLocation + schema/content revision` 足以标识内容版本。

DesignStandard 生命周期与下游行为固定为：

| 状态 | JEI/说明 | 新制造与新安装 | 既有安装 | 维护 |
|---|---|---|---|---|
| `ACTIVE` | 正常显示 | 允许 | 正常运行 | 正常 |
| `REVIEW_REQUIRED` | 显示复核警告 | 默认仍允许，除非玩家另行暂停 | 正常运行 | 正常并收集额外报告 |
| `PAUSED_NEW_DEPLOYMENT` | 显示“暂停采用” | 禁止新制造/自动化提交/安装，保留可解释原因 | 不自动拆除或停机 | 允许维护与拆除 |
| `SUPERSEDED` | 指向新标准 | 旧版不再新部署 | 可继续运行或安排升级 | 旧版维护 Procedure 仍可用 |
| `WITHDRAWN` | 仅档案/警告可见 | 不再制造或安装 | 明确列入处置清单，不瞬间删除物品/机器 | 允许安全拆除、必要维护和迁移 |

相关 Finding 进入 `NEEDS_REVIEW` 只让标准进入 `REVIEW_REQUIRED`，不会自动暂停、撤回或破坏现有安装；这些进一步动作仍由 coordinator 决定。

## 原型、部署与技术投影

### 原型制造

玩家在绘图台的工程页：

1. 选择已经理解的设计模式；
2. 填写目标指标、不得退化的指标和适用 host；
3. 在候选参数/材料之间做取舍；
4. 执行 `AuthorizePrototypeDesign`，得到有 revision 的蓝图；
5. 在原型工作台放入蓝图和真实 BOM；
6. 获得带 `team/designRevision/serial` 的实体原型。

原型处于 `PROTOTYPE_ONLY`：JEI 可见但用琥珀色标记“仅实验流程”，只在绑定的正式 Plan 或 FieldTrial 中工作，有明确并发数、人员和维护要求。限制来自当前已签发实验，而不是一个随意的全局数量。

### 实验室与现场部署

原型不能用一条互斥线性状态同时表达位置、维护和实验关系。三个维度分别持久化：

```text
placement:   INVENTORY / INSTALLED_LAB / INSTALLED_FIELD / RETIRED
maintenance: SERVICEABLE / INSPECTION_DUE / OUT_OF_SERVICE
bindings:    optional PlanBinding + optional FieldTrialBinding + Run refs
```

玩家必须把原型安装到真实对象上。拆下时 placement 回到 `INVENTORY`，原绑定封存；维修可把 `INSPECTION_DUE/OUT_OF_SERVICE` 恢复为 `SERVICEABLE`，但不抹去 incident。实验室证据满足 `FIELD_ELIGIBLE` 后，玩家选择实际服务中的能量塔、住房、矿场或生产线并执行 `DeployFieldTrial`；现场运行继续消耗真实资源并接受正常故障和维护。到期只产生待检查/待分析制品，不会因倒计时自动通过。

标准签署后，旧 serial 仍是 grandfathered prototype，不能自动变成正式组件。玩家可以退休保留、继续作对照，或在检查后通过明确转换工单补齐标准 BOM 并换成新的 standard-component serial。

### 标准化与普通部署

签署 DesignStandard 后：

1. `TechnologyAccessProjection` 开放独立的标准制造/安装 `ActionKey`；prototype fabrication 与 standard component fabrication 是两个键，不能把带 serial 的原型配方原地提升成普通配方；
2. JEI 使用同一 access projection 显示普通配方；
3. 普通工作台和自动化执行端开始允许该稳定 recipe/action；
4. 玩家制造正式组件并逐台安装，或在城镇界面签发一个有材料和工时的改装任务；
5. 教学班次让居民取得维护/操作 Procedure；
6. 机器 GUI 显示采用的标准、组件版本和效果来源；
7. 城镇报告显示已部署数量、待改装数量和真实指标变化。

投影拆成两层：

- `TechnologyAccessProjection(team)` 只回答某个稳定 `ActionKey` 对该 team 是 `FORBIDDEN/PROTOTYPE_ONLY/STANDARDIZED`，供服务端执行、JEI 和自动化使用；
- `InstalledContributionSnapshot(hostIncarnation)` 根据该 host 的组件 serial、标准 revision、运行/维护状态和燃料/模式计算实际贡献。

例如 access projection 可以允许制造 `t1_controlled_draft_v1`，而 installed snapshot 只在该组件确实装入某座 T1 时给出燃料时长 `+0.1`。服务端实际执行每次重查 access；客户端投影只是展示缓存。新标准绝不反向写入 team-wide `generator_effi`。

旧存档已有的 team-wide bonus 进入带 provenance 的 `LegacyProjection`。`GeneratorModifierResolver(host, fuel, mode)` 按 `exclusiveFamily` 去重：同源的 legacy T1 效率与新受控进气组件取该 family 的最大有效贡献，不能静默叠加成 `0.7 + 0.1 + 0.1`；不同 family 是否可叠加由类型化 resolver 明确声明，tooltip 列出每个实际来源。

## 完整垂直切片：T1 受控进气效率改良

这是一项工程研究。当前 `GeneratorData` 并没有“严寒导致塔外壳额外热损失”的机制，所以首版不能把不存在的规律包装成科学发现。

### 切片前置：T1 实例、附件和遥测

首切片给现有 team 单例 `GeneratorData` 增加 `GeneratorInstanceId`。IE 多方块形成时给 master 分配 incarnation；拆解会结束该 incarnation，重组产生新实例。T1 主界面新增一个 `UPGRADE_SLOT`：玩家手持 prototype/standard component 对主交互块潜行使用，看到 ghost slot 后确认安装；拆除也在同一界面执行。renderer 根据 master-relative attachment profile 显示风扇/送风组件，不再假定一个源码中不存在的“维护侧”。首版所有“部署状态”都指当前 active T1，而不是虚构多个并存塔。

`frostedheart:generator_t1` subject adapter 必须新增以下权威信号；R2/R3 只有在这些数据存在后才成立：

| 信号 | 必要字段 | 单位/语义 | 普通运行保留 |
|---|---|---|---|
| `FuelLoadEvent` | instance、燃料/数量、recipe revision、装入前后 process、实际 multiplier、credited process ticks、来源 | 每次真实装入；倍率取装入时快照 | 逐事件，按日再汇总 |
| `T1StateSegment` | start/end、working、active、overdrive、process、`TLevel/RLevel`、组件/维护状态 | 连续同状态区间；非逐 tick NBT | 状态变化和小时窗口 |
| `SupplyContinuitySegment` | input 可用、成功装料、starved/unknown、来源 | `AVAILABLE/STARVED/UNKNOWN`；仓库库存只作为 inferred reserve | 班次/小时段 |
| `HeatFieldProbeSample` | 固定探针位置、`WorldTemperature.heat/air`、建筑服务点温度 | 实际供暖结果，不把 T/R 内部档位当结果 | 日常低频，实验按计划频率 |

formal Run 订阅 generator signal sink；控制台卸载不丢失目标已经发出的事件。目标本身未加载且 town batch 无法提供等价样本时，记录明确 gap，不插值。`TownOperationalStatusProvider#captureT1FuelReserve` 在已登记实例未加载时也必须能根据实例注册表识别 T1，不能因实时 BE 不可见而把 kind/fuel reserve 变成 UNKNOWN。

### A. 问题出现

入口有两条：

- `TownSignalEvent` 或运营快照报告燃料储备低于目标；
- 玩家主动在镇长印章把 T1 燃料日数设为改进目标。

收件箱形成：

- `R1`：燃料储备日下降；
- `R2`：由新 `T1StateSegment` 汇总出的最近三个自然日 active/working 时间；
- `R3`：由实际 `FuelLoadEvent + SupplyContinuitySegment` 得出的供给区间；若只有仓库库存则明确写“推测有储备，尚未证明连续供给”。

玩家在证据板选择“设为改进目标”，接受 Idea：

> 能否让每份燃料维持更久，同时保持 T1 的热场？

### B. 定义问题

玩家选择 `ENGINEER` 并填写：

- host：T1 能量塔；
- 模式：普通，禁止超载；
- 燃料：焦煤；
- 主目标：降低每日焦煤需求；
- 不可退化：T1 active 有效率、两个固定热场探针和一个实际服务建筑的温度；`TLevel/RLevel` 只作为控制量；
- 第一版 scope：当前塔；
- 验收：暂定原型后再做标准级现场试验。

界面不显示 `generator_effi +0.1` 或最终组件名称。

### C. 居民讨论

玩家钉住三份报告，邀请物流员、T1 操作者和一名受教育居民。会议为离岗 `ActivityReservation`，因此在讨论结束后才另行安排供给监控和值班；一个班次后可能得到：

- `H1`：存在日报粒度没有看见的短时断供；
- `H2`：塔曾进入超载，额外消耗了 process ticks；
- `D1`：动力受控送风——较高燃料收益，但需要持续 Create 动力和更频繁检查；
- `D2`：被动导流结构——较低燃料收益，但不需要外部动力，维护间隔更长。

设计知识可来自有锅炉经历的难民、玩家检查机械风箱/燃烧器，或遗迹文献，避免把特定随机居民做成唯一进度锁。玩家采纳 H1/H2 作为需要排除的解释，并让 D1/D2 都进入候选设计。若玩家显式跳过 D2，单路线只允许 provisional performance Finding，不能签署 DesignStandard。

### D. 义务前沿

编译器给出可并行的具体缺口：

1. 定义可测量的燃料效率；
2. 记录正常模式基线；
3. 连续确认燃料供应；
4. 连续确认超载关闭；
5. 计算理论每日耗量；
6. 制造 D1/D2 两种进气原型；
7. 进行基线/D1/D2 对照；
8. 由未参与居民复核；
9. 进行城镇现场试验；
10. 编写操作/维护 Procedure 并形成标准。

物流员监控供料时，受教育居民或机械计算器可以并行处理公式。

### E. 计算任务

绘图台消耗纸和笔签发。下面数值是当前默认示例；实际任务必须在签发时冻结可 reload 的焦煤 recipe revision 和运行时 config，并调用 `GeneratorFuelModel#effectiveFuelProcessTicks` 与 `#idealFuelItemsPerDay`，不能在研究系统复制一份公式常量：

```text
T1 焦煤正常运行基线
配方时长：3200 process ticks
基础燃料时长倍率：0.7
每日有效运行：24000 game ticks
```

机械计算器或居民产出：

```text
每件焦煤有效时长 = floor(3200 × 0.7) = 2240 process ticks
理论焦煤/日 = 24000 / 2240 = 10.7143
```

玩家检查单位和来源后，把它附到“基线预测”。

### F. 建立基线

玩家放置控制台并用连接器链接：同一座 T1 主节点、燃料输入、仓库供给、两个固定热场探针和一个服务建筑温度点。选择顺序交叉对照，签发“焦煤、普通模式、累计 24 游戏小时，即 24000 个 active game ticks”。

因为燃料 multiplier 只在 `consumesFuel` 装入下一件燃料时读取，process 又会跨物品/日期结转，V1 协议固定为：

1. 隔离自动输入，让旧 process 余额在非样本段降到 `0`；
2. 设置当前条件后装入一批有计数的焦煤，等待 `FuelLoadEvent`；
3. 从该事件开始记录 24000 个 active ticks；停机 world ticks 不计入有效窗口；
4. 用 `startProcess + Σ(本条件 creditedProcessTicks) - endProcess` 核算实际消耗；
5. 切换/安装原型前再次完成 washout，绝不把旧倍率燃料余额混入新条件；
6. UI 分开显示“本段实际装入整数件数与期末余额”和“由完整账本推导的长期件/日”。

能力面板必须逐项显示：燃料装入事件、process 余额、`TLevel/RLevel`、actual heat probes、active/working、超载状态、供给连续性以及“玩家正在值守或已安排实验值班居民”。控制台运行时：

- 蓝灯表示正在记录；
- 绿灯表示本段符合控制条件；
- 黄灯表示暂停或本段不能比较。

若供料中断 1 小时 20 分，界面显示“有效样本 14h；排除 1h20m；原因：供料中断”，恢复后继续累计，不宣布整个实验失败。

### G. 制造并安装原型

排除 H1/H2 且计算完成后，玩家授权两个第一版设计。以下为切片默认，均由数据包冻结到 design revision：

- `D1 动力受控送风 V1`：2 个铸铁燃烧器、4 个动力风扇、8 个铸铁板和 1 个机械风箱；在 `SERVICEABLE` 且输入达到最低 Create 转速时提供 `+0.10` duration multiplier，失去动力就不提供贡献，并产生 uptime gap；检查间隔短。
- `D2 被动导流 V1`：燃烧器、较多铸铁板和无动力导流件；提供 `+0.06` duration multiplier，不消耗 Create 动力，检查间隔较长。

原型工作台分别输出带 team、Inquiry、design revision 和 serial 的实体。玩家把它放入当前 T1 master 的 `UPGRADE_SLOT`；D1 出现可见旋转/声音和动力状态，D2 出现静态导流外观。原型只在绑定实验/现场试验有效时向该实例提供贡献，不能被普通复制或拿到另一座塔绕过重新绑定。

### H. 原型对照

玩家按同一 washout 协议分别运行 D1 与 D2 条件，继续使用焦煤、普通模式，并记录实际热场、active uptime、D1 动力 uptime 和维护事件。条件顺序由计划记录；正式复现时交换顺序。若玩家开启超载，控制台黄灯并提供：

- 恢复普通模式，继续当前实验；
- 保留该区间，并派生一个“超载适用范围”调查。

区块卸载、值班居民离岗或原型维护到期都只暂停对应采样并保留已有有效窗口。

### I. 分析结果

当前默认值下的示例结果为：

| 指标 | 基线 | D1 动力送风 | D2 被动导流 |
|---|---:|---:|---:|
| 每件焦煤 credited duration | 2240 ticks | 2560 ticks | 2432 ticks |
| 24000 active ticks 内装入 | 11 件 | 10 件 | 10 件 |
| 期末 process 余额 | 640 | 1600 | 320 |
| 账本推导长期焦煤/日 | 10.7143 | 9.3750 | 9.8684 |
| active 与固定热场探针 | 基准 | 与基准比较 | 与基准比较 |
| 额外动力 uptime | 不适用 | 实际测得 | 不适用 |
| 超载 | 关闭 | 关闭 | 关闭 |
| 有效记录 | 24000 ticks | 24000 ticks | 24000 ticks |

UI 必须正确表达：D1 倍率由 `0.7` 到 `0.8`，在该条件下长期焦煤需求下降 `12.5%`；不能把 `+0.1` 直接写成“节省 10%”。D2 的较低收益与无需动力、较长维护间隔同时呈现。玩家的验收目标决定哪条路线更合适，而不是系统宣布唯一正确设计。

玩家可以选择：重复、接受 D1/D2 各自的暂定性能结论、测试其他燃料、测试超载或修改原型。接受后形成：

- 两条各自有 scope 的 provisional performance Finding；
- trialled Procedure：“T1 燃料时长顺序对照测试”；
- resolver 对有足够证据的 revision 派生 `FIELD_ELIGIBLE`；DesignRevision 自身仍为 `AUTHORIZED`。若玩家不接受分析，Run 仍留在档案但不进入资格判定。

### J. 复现、现场试验和标准化

正式结论要求更换时段、交换条件顺序；若内容确实要求实例独立性，则需要拆建形成新的 `GeneratorInstanceId` 后再运行，按该 Finding 的 `ReplicationPolicy` 排除实例与时序偏差。Procedure 由另一名居民只按文档执行一次。玩家接受 lab basis 并执行 `DeployFieldTrial` 后，把有序列号的候选原型部署到实际供暖中的 T1。现场资格至少要求 `72000` loaded active game ticks、跨越 3 个不同自然 `worldDay`，并只接受来源为 `NATURAL` 的城镇结算；`/town tick`、测试和命令来源不计。还必须满足：

- 热场未退化；
- active uptime、动力/维护成本满足玩家签发的需求，所有原型 incident 已有处置；
- 签署前至少一名普通岗位居民只按 Procedure 完成独立操作与维护验证；
- 一名未参与原实验的居民完成日志复核。

外部燃料短缺会排除对应区间，而不是判设计失败。性能不改善会反驳对应 design claim；改善但热场下降或动力 uptime 太低表示没有满足工程需求；这些结果都保留并允许创建 V2。

玩家最终在标准评审页根据燃料、动力和维护取舍选择 D1 或 D2，附上 established performance Finding、测试 Procedure、维护 Procedure、独立操作者验证和复核记录，执行 `SignDesignStandard`。若选择 D1，得到稳定资产 `frostedresearch:design/t1_controlled_draft_v1`。签署后才安排面向全部生产岗位的批量教学。

### K. 玩家可见的部署结果

标准签署后：

- JEI 保留带 serial 的 prototype 工序为“仅实验”，并新增独立的标准组件普通配方；
- 玩家或城镇工单消耗真实材料改装当前 active T1；未来若城镇规则开放多塔，同一 host contract 才自然扩成逐实例工单；
- T1 GUI 显示“采用标准：受控进气 V1”和组件 serial/version；
- 燃料 tooltip 显示基础倍率、标准贡献与最终有效时长；
- 镇长印章显示当前 T1 的组件/标准状态以及燃料储备日的真实重算；
- 签署后的生产岗位居民完成批量教学后才能自动维护；
- 档案可沿来源链回看 Idea、失败区间、Run、Finding、Procedure 和标准签署；
- 收件箱出现“超载或其他燃料是否也适用”的后续 IdeaCandidate，但不自动开研究。

这条切片同时证明观察、居民知识、义务编译、计算、实际装置、实验、结论、原型、现场部署、JEI 和现有能量塔效果能够贯通。

## 失败与边缘情况的玩家反馈

| 情况 | 可见反馈 | 保留的状态与后续动作 |
|---|---|---|
| 燃料/能量中断 | 黄灯和精确起止时间 | 排除污染段，恢复后继续累计 |
| 控制变量漂移 | 曲线越界着色并说明变量 | 只使相关比较降级，可补跑窗口 |
| 控制台区块卸载 | 玩家重新打开时提示离线期间状态 | server-side subject subscription 继续；不因 UI/BE 不在场丢目标事件 |
| 实验对象卸载且无等价 town batch 信号 | “目标未产生可验证样本” | 形成 gap，已有轨迹保留，加载后继续 |
| 居民离岗、生病 | 显示姓名和缺失职责 | 重新排班后恢复 |
| 仪器超量程/未校准 | 读数断线及原因 | 只影响对应变量；生成校准义务 |
| 实验对象拆除重放 | “对象实例已更换” | 旧 Run 封存；新对象不能接续同一 trace |
| 主动中止 | 要求选择原因并封存 | 结果可作观察，不满足正式义务 |
| 结果无改善 | 对应 design claim 受到反证 | 保留 Negative Finding 候选，回到设计义务 |
| 改善但副指标退化 | 明确哪项需求未满足 | 不能标准化，可修改目标或做 V2 |
| 没有专业难民 | 显示可替代知识来源 | 检查物件、遗迹档案、跨领域类比或探索运行 |
| 项目归档 | 从当前工作区移出 | 全部历史保留，新证据可建议恢复 |
| 服务端配置/配方改变 | 旧资产标出验证 revision | 生成复核义务，不偷偷把旧数字套入新规则 |
| `/town tick` 或测试结算 | 标明 `COMMAND/TEST` 来源 | 可作调试记录，不满足自然日/现场资格 |

## 后端边界与玩家动作映射

### 唯一写入口

新增 `TeamKnowledgeData` 作为 Chorda team 数据，和现有 `TeamResearchData` 分离。`TeamResearchService` 是唯一命令处理器和事务协调器；现有空的 `TeamResearchManager` 只保留为兼容 facade 并委托给 service，不再作为第二套命名或权威。世界系统只提交不可变信号，UI 只发送意图。

不同数据各有唯一权威，不能笼统塞进一个大对象：

| 数据 | 权威位置 | 说明 |
|---|---|---|
| Observation/Claim/Justification、Inquiry、任务元数据、Finding/Procedure/Standard | `TeamKnowledgeData` | team 语义状态 |
| 高频 Run trace | team 归属的分段 `ResearchTraceStore` | 主数据只保存引用、摘要和 sealing revision，避免一个巨大 NBT |
| 原型/标准组件的实际位置与磨损 | `PrototypeInstanceRegistry` 加世界中的 host BE/item | registry 管 serial/binding 索引；已加载 host 是安装和实时磨损事实，加载时按明确规则对账 |
| 规则、单位、BOM 与 resolver | immutable catalog snapshot | 来自数据包候选事务，带 revision |
| Access、客户端摘要、installed contribution | 可重建 projection cache | 绝不作为持久事实源 |

service 只协调这些权威的正常写入和恢复，不声称“持有一切”。

| 玩家操作 | C2S 意图 | 服务端产生 |
|---|---|---|
| 现场记录 | `CaptureObservationIntent` | ObservationDraft；第三方 Report 由服务端 source 产生 |
| 正式归档 | `ArchiveObservationIntent` | immutable ObservationRecord |
| 钉住与连线 | `CurateEvidenceIntent` | Anchor/RelationDraft，不改 Observation |
| 建立/改框架 | `OpenOrReviseInquiryIntent` | InquiryRevision + obligations |
| 安排讨论/计算 | `CommissionTaskIntent` | ResearchTaskOrder |
| 采纳解释 | `AdoptHypothesisIntent` | Claim(PROPOSED) |
| 签发计划 | `IssuePlanIntent` | immutable ExperimentPlanRevision |
| 链接装置 | `BindApparatusIntent` | server-resolved ApparatusBinding |
| 预备/启动实验 | `ArmRunIntent` / `StartRunIntent(mode)` | ExperimentRun(ARMED/RUNNING) |
| 附入解释 | `AttachInterpretationIntent` | Evidence + Justification |
| 发布结论/程序 | `PublishFindingIntent(mode)` / `PublishProcedureIntent` | immutable Finding/Procedure revision |
| 授权/部署原型 | `AuthorizePrototypeIntent` / `DeployTrialIntent` | DesignRevision/PrototypeInstance/FieldRun |
| 签署标准 | `SignStandardIntent` | 只提交 immutable DesignStandard |
| 暂停/替代/撤回标准 | `ChangeStandardLifecycleIntent` | 标准生命周期事件 |

每个意图都携带玩家看到的 `expectedRevision`；服务端重新解析当前 team、菜单/设施、对象、材料和状态，不接受客户端上传“已经测得的值”或“已经完成的义务”。状态冲突时返回最新差异和可恢复动作，不丢弃玩家已经生成的文档或轨迹。

提交 DesignStandard 后，resolver 才根据 `(teamKnowledgeRevision, catalogRevision)` 重算 `TechnologyAccessProjection` 并发布带独立 `projectionRevision` 的 snapshot/delta；host 安装/维护变化再重算对应 `InstalledContributionSnapshot`。重载时从资产和实例事实重建，delta 永远不是权威。

### 建议模块

```text
research.api           主体、变量、能力、信号与 TeamRef
research.observation   ObservationGate、收件箱、聚合与来源
research.knowledge     Claim/Evidence/Justification 图与真值维护
research.inquiry       Inquiry、义务规则、编译器与玩家行动
research.resident      私人知识、讨论、任务、教学与复核
research.experiment    计划、装置绑定、运行、有效性与分析
research.engineering   DesignRevision、PrototypeInstance、现场试验
research.asset         Finding、Procedure、DesignStandard 解析
research.projection    ActionAccess、VariantContribution、客户端投影
research.integration   温度、能量塔、城镇、Create、IE、StoneAge 适配器
research.storage       TeamKnowledgeData、事件日志、归档与同步
```

### 城镇结算

研究所先接入一个稳定的 `TownBuildingType` 注册缝，再新增 building codec；不要继续扩大 `ITownBuilding.CODEC` 的中心固定分派。抽缝时必须继续解码旧名字 `house`、`huntingBase`、`mine`、`mineBase`、`warehouse`、`transportStation`，并保持 `buildByNameWithLegacyInt()` 的旧整数顺序，不能为了新建筑破坏老城镇。

`ResearchInstituteBuilding#work` 只生成当日不可变的 staffed-service report，并像现有 daily report 一样暂存在 building 或显式 settlement collector。所有建筑完成当日 `buildingsWork` 后，由 `ResearchDailyCoordinator` 一次性消费城镇观察与研究服务，避免建筑结算顺序改变结果。settlement/report 新增 `origin=NATURAL|COMMAND|TEST`，只有内容政策允许的来源能满足现场时长/复现义务。

现有 `ITownResidentListener` 继续保持居民模拟单订阅用途；研究使用新的多订阅、不可变 `TownSettlementReport`，不抢占该监听器。

### Team 生命周期、同步与所有权

- 在 `FRSpecialDataTypes` 注册唯一 plain-string ID `knowledge` 对应 `TeamKnowledgeData`。
- 覆盖 team 创建/加载、玩家登录、维度切换、`PlayerTeamChangedEvent`、catalog reload 和保存生命周期；初始化与 reconcile 都走 service。
- S2C 先发送 `CatalogSnapshot(catalogRevision)`，再发送 `TeamResearchSnapshot(teamId, sessionEpoch, teamKnowledgeRevision, projectionRevision)`；后续 delta 必须连续，缺 revision 就请求 snapshot。
- town、legacy research 和新 knowledge 共用 `TeamContextEpoch` envelope。换队时客户端先清空三者，再原子安装同一 epoch 的快照，禁止新研究状态配旧城镇数据。
- 图详情、旧事件段和实验 trace 只在玩家打开对应 Inquiry/Run 时按需拉取。

现有绘图台和机械计算器的 owner 实际来自 `BlockEntityMixin_Research` 注入的根 NBT `fhowner`，不是设备自身字段。共存期 `OwnershipResolver` 继续读取它，并在方块下一次保存时迁入显式 `TeamRef`；新研究所、控制台和原型台直接持久化显式 owner 与 instance UUID，不再依赖给所有 BE 注入身份。

### 数据包与内容作者职责

数据包声明：

- `VariableKey`、单位、可观察条件与聚合方式；
- 主体 profile 与适配器选择键；
- Idea 关系规则；
- MechanismSchema、CaseMemory、Procedure/DesignPattern；
- Inquiry 规则模板和 obligation derivation；
- Finding/Procedure/DesignStandard resolver；
- recipe/action access 和类型化 variant contribution；
- 文本、图标、BOM、平衡时长和复现策略。

旧研究继续由当前 `config/fhresearches` 的 `ResearchCatalog` 加载，并在共存期保持自己的候选校验/回滚。新语义定义使用独立 datapack reload listener；同一次 reload 中，内置数据包、整合包数据和 KubeJS 生成/声明的资源按正常 pack priority 进入一个候选 `ResearchSemanticCatalog`，整批解析、交叉校验后原子安装并增加 `catalogRevision`。两类 catalog 不混成一个半旧半新的对象。

KubeJS 只能提交带稳定 `ResourceLocation` 的声明，不能直接取得可变 team 图。旧 ID 由文件名产生且存在 `generator_T1` 等非法大写形式，迁移必须使用显式 `legacyString -> canonical ResourceLocation` alias 表，不做直接字符串转换。

团队进度、居民私人知识、事件、实验和资产永远属于存档。客户端只同步当前 team 的 Inquiry 摘要、义务、资产、权限、variant 和按需请求的来源链；无界事件日志和全部实验轨迹不做登录全量同步。

## 与现有系统的映射和迁移

### 可直接复用

- `DrawingDeskBlock`、`DrawingDeskTileEntity`、`DrawDeskContainer` 和 `DrawDeskScreen` 的世界入口、全局 mixin owner 迁移入口和三槽。
- `PanZoomViewport`、布局/裁剪/摄像机思路与现有档案视觉语言；旧 `ResearchGraphViewport/ResearchArchiveViewCache` 强绑定 `Research` DAG，需要抽取/泛化，不列为可直接接入异构知识图的组件。空的 `EXPERIMENT` 页可成为导航入口。
- Create 存在时复用 `MechCalcTileEntity` 的动力、转速、声音和渲染；不存在时不注册，居民路径保持完整。
- `TownHistoryEntry`、`TownSignalEvent`、建筑日报与 `TownOperationalStatusProvider` 的报告来源。
- `Resident#getIntelligence/#getEducationLevel/#getWorkProficiency(...)` 作为任务资格输入；现有熟练度持久键依赖 `Class#getSimpleName()`，不能拿来存研究角色或私人知识。
- `WorldTemperature.block/air/heat` 与 `GeneratorData` 作为首切片的真实变量源/效果消费端。

### 需要替换的旧语义

- `TeamResearchData` 不再继续承担知识图、事件、任务和实验；新建 `TeamKnowledgeData`，旧对象只做兼容。
- `TeamResearchService` 成为唯一新系统命令处理器；空的 `TeamResearchManager` 变成 facade。共存期的 `ResearchCommand`、`ResearchHooks`、旧 C2S 包和 `EffectStats#grant/revoke` 通过 `LegacyResearchGateway` 修改旧状态与 contribution ledger，不允许它们直接写新图。
- `MechCalcTileEntity.currentPoints`、`TeamResearchData#doResearch` 和 `ComputeMachine#fetchPoint` 迁为具体 worksheet 工作流。
- `RubbingTool` 保留拓印/状态摘录用途，不再携带通用点数；旧 NBT 是根键 `"research"` 与 `"points"`，迁移读取必须按真实格式处理。
- `DrawingDeskTileEntity#gamedata` 与旧 `pts` 不具备来源，不能伪装成新证据；共存期走 legacy 执行器或转换成明确标记的 legacy 文档。
- 旧 completed research 只生成 `LegacyEntitlement/LegacyProjection`，继续提供已有 access/variant，但绝不伪造成有证据链的 Finding、Procedure 或 DesignStandard，也不参与新义务满足。只有人工明确映射时才生成 `UNVERIFIED_LEGACY_ASSET`，并立即产生复核义务。
- 现有 `generator_effi`、`generator_heat` 等字符串 variant 先快照成带 source/provenance 的 legacy contribution，由 ledger 幂等重建；旧消费者逐个迁到 typed resolver。`TeamResearchData#getVariants()` 暴露的 mutable tag 不能继续充当新旧共同账本。
- Recipe/JEI 以稳定 `ActionKey(recipe/category/action ResourceLocation)` 查询同一份 `TechnologyAccess`；不再按输出物品或 Recipe 对象身份锁定。
- StoneAge、Create、IE 和普通 crafting 分别在真实选方/执行边界接入同一查询，不能只依赖 vanilla crafting mixin。

旧 FTBQ insight 奖励在迁移期保留，记为 legacy provenance；新任务奖励只给予档案、证词、观察机会、材料或研究授权，不能直接发 Established Finding。任务查询知识资产或义务，禁止与研究形成双向自动完成环。

### 旧存档逐字段政策

| 旧字段 | 迁移政策 |
|---|---|
| `variants` | 按当前实际值建立 `LegacyVariantContribution` 快照与 source；不从定义重新累加，不和新同 family 组件重复 |
| team `active` | 留在 legacy executor/UI；不自动变成 Inquiry |
| `insight/usedInsightLevel` | 保留给旧研究与 200 个旧任务奖励；新系统不把余额换成“知识”或进度 |
| `visitedArea` | 继续满足旧 clue；只有显式内容映射时可生成有 legacy provenance 的 Report |
| `ResearchData.committed/clueData/active/level` | 原样保留给未完成/无限旧研究，不参加新 Finding 判定 |
| `finished` | 产生 LegacyEntitlement；不代表建立了科学结论 |
| `effectData` | 继续作为领取/撤销幂等依据；物品、经验和命令奖励绝不重放，未领取奖励仍由旧 UI 领取一次 |

无限研究的 `level` 若贡献效果，要用 `legacyResearchId@level` 形成可追溯 ledger 项；迁移与 reconcile 必须验证重复加载不会重复加成、已领取一次性效果不会再发、未领取效果不会丢失。

## 实施顺序

### Phase 0：语义目录与垂直切片验收脚本

- 固定 T1 切片的变量、单位、实际可观测字段、BOM、目标与验证窗口。
- 保留 team 单 active tower 规则，为 `GeneratorData` 增加 master incarnation、`UPGRADE_SLOT`、拆装/回收规则和 host-aware modifier 调用链；不在首切片顺带改变为多塔城镇。
- 实现 `FuelLoadEvent/T1StateSegment/SupplyContinuitySegment/HeatFieldProbeSample` 与 natural/command/test 时间来源；建立 fuel washout/余额账本测试。
- 明确每个玩家动作、服务端命令、产物和 UI 文案。
- 为世界机制建立回归测试，确认研究前后看到的实际变化与公式一致。

### Phase 1：team 内核和兼容投影

- 新增 `TeamKnowledgeData`、`TeamResearchService`、事件日志和 revision。
- 完成 Claim/Evidence/Justification、Inquiry、文档和 KnowledgeAsset 最小模型。
- 实现最小 `ObligationCompiler`、规则注册、`ActionResolver` 和 occurrence log，使 Phase 2 的义务前沿有权威来源。
- 建立 legacy entitlement/access/variant 的只读适配，先不改变玩法权威，不伪造新资产。
- 注册 SpecialData、team lifecycle、统一 epoch snapshot/delta 与 catalog revision。

### Phase 2：笔记、收件箱和绘图台

- 新增研究笔记和 ObservationGate。
- 接入玩家、温度、T1 与城镇日报信号。
- 把绘图台改造成证据板、框架表单和义务前沿；保留旧项目入口用于共存。

### Phase 3：居民任务和研究所

- 先抽 `TownBuildingType` 注册边界，再注册研究所。
- 加居民私人知识覆盖层、访谈、讨论、计算、复核和教学任务。
- 建立日结算后的 `ResearchDailyCoordinator`。

### Phase 4：计算器和义务编译器

- 把机械计算器改造成 worksheet 执行器。
- 在 Phase 1 最小编译器上扩展四种 Inquiry 的完整规则、原因链、target-aware dependency 和多行动路线。
- 所有任务产物停在玩家审阅，不允许自动 semantic commitment。

### Phase 5：实验控制台

- 实现主体 incarnation、连接器、能力检查、计划 revision 和 Run。
- 首先只适配温度、T1、仓库/供给和城镇值班。
- 实现污染区间、暂停恢复、有效性报告和按需轨迹同步。

### Phase 6：原型、资产与物理部署

- 实现原型工作台、DesignRevision、序列号、安装和现场试验。
- 实现 Finding、Procedure、DesignStandard 的发布/签署规则。
- 让 T1 组件效果仅在真实安装时生效，并让 GUI/城镇报告显示来源。

### Phase 7：统一技术访问和迁移内容

- 统一执行端、JEI 与自动化的 `TechnologyAccess` 查询。
- 并行比较旧/新投影，先记录差异，再逐条切换权威。
- 迁移旧研究定义、FTBQ 奖励、KubeJS 内容和第三方执行点。

## 验收标准

### 玩家体验

- 玩家从第一次记录到签署 T1 标准的每一步都有一个明确世界动作、界面选择或居民委托，没有通用点数按钮。
- Idea 只展示问题、来源和关系，不泄露结论、配方或最终数值。
- 每项义务能说明“为什么出现、如何满足、缺什么、能区分什么”。
- 居民输出可读的发言、草案或评审，而不是每日研究点。
- 实验中断能恢复，有效/污染区间分开显示；无结果、反证和异常都有后续玩法。
- Finding、Procedure 和 DesignStandard 的形成条件与效果边界在 UI 中可解释。
- 原型在世界中可见、可安装、可损坏/维护且绑定到具体试验；标准组件需要真实制造和部署。
- T1、JEI、镇长印章和居民维护状态都能显示同一 DesignStandard 的实际影响。

### 领域与持久化

- 重载、拆除研究所/控制台、丢失文档或成员离线不会删除 team 知识和已完成工作。
- 方块拆除重放不会把新实例接入旧实验 trace。
- 旧单例 Generator 存档升级后保留 inventory、process、位置与状态并获得唯一 incarnation；正常拆塔返还 upgrade，异常拆解也能从 detached-component 记录恢复。
- 同一 CalculationTask 不会由机器和居民重复结算；同一 asset/contribution 重放幂等。
- 修改 Inquiry scope 会保留旧来源，并只重编受影响义务。
- 被反驳、被替代和 legacy 知识保留 provenance，不直接删除。

### 集成与性能

- 普通机器只产生聚合窗口；只有正式实验对象高频采样。
- T1 的 fuel-load/state/supply/heat telemetry 跨保存重载保持连续 provenance；仓库库存永远不被误写为实际供给连续性。
- 客户端不能提交测量值、完成状态或资产，只能提交带 expected revision 的意图。
- 切换 team 时以统一 epoch 原子替换 town/research 投影，不混用两队快照。
- recipe reload 后权限仍按稳定 `ResourceLocation` 生效，JEI 和实际执行给出一致结果。
- catalog reload 不改写已签 asset revision；标准 lifecycle、JEI、新制造和既有安装严格遵循行为矩阵。
- legacy T1 bonus 与同 family 实体组件不会双重叠加，tooltip 和 town 估算显示同一个 resolver 的来源分解。
- `/town tick` 与测试结算不能满足 72000 active ticks/自然 world-day 的现场资格。
- T1 实验示例中的 `0.7 → 0.8`、`2240 → 2560` 与燃料日数 `10.7143 → 9.3750` 由同一公式源计算并测试。

## 文档影响

本计划描述 intended behavior，不改变当前系统事实。开始实现后，每一阶段必须同步更新 `docs/research/`，城镇建筑/结算变化还要更新 `docs/town/`；配方、KubeJS、任务或整合包配置变化前需读取伴生仓库的 `AGENTS.md` 并分别验证两个 Git 仓库。每个完成阶段在 `diary/` 新增记录，并在本计划的 Outcome 中链接实现、验证与文档。

## 待项目所有者确认

- 首切片是否接受“保留一队一座 active T1，但为它增加 incarnation 和实体组件槽”作为边界；本计划选择这一方案，不顺带改变城镇多塔平衡，旧存档的 team-wide `generator_effi` 只作为 legacy 兼容来源。
- 初始研究笔记是由开场场景发放，还是加入无需研究前置的基础配方；本计划默认由场景发放，并允许丢失后制作。
- 原型工作台是否作为独立方块进入首版；本计划选择独立方块，以让授权蓝图、真实 BOM 和有序列号的实体原型具有清楚的世界落点。

## Outcome

`Pending owner review. No implementation has started.`
