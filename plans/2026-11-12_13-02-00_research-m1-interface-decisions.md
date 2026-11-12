# M1-A：知识条件、项目入口与披露接口决定

- Time: `2026-11-12 13:02:00 +08:00`
- Authors: `Codex / OpenAI，M1-A 接口调查与交接`
- Status: `completed`（接口调查交付；以下均为 M1-B 待实现决定，不是当前功能）
- Scope: FH 知识条件、项目发现/开始/披露、多来源历史、观察用途、服务端视图与知识 UI；不实现 M2 事件概率/联络计时或 M4 阶段引擎。
- Related: [最新架构摘要](2026-11-11_22-20-43_research-system-architecture-revision.md)、[开发计划 M1](2026-11-11_23-38-07_research-system-playable-development-plan.md)、[M0 内容基线](2026-11-12_12-36-00_research-m0-content-baseline.md)、[M0 交接](../diary/2026-11-12_12-40-00_research-m0-baseline-handoff.md)、[知识文档入口](../docs/knowledge/README.md)。

## 依据与边界

按本次委派的最新规则及架构摘要决定语义。只读 [design/knowledge_research.md](../design/knowledge_research.md) 中早期的“每个成果必须来自想法”“联络至少两份”等限制不覆盖后续明确决定。这里采用用户允许的推荐默认；未发现这些默认与最新明确意图冲突。

本次未执行 Git 命令、构建或游戏测试；未修改生产代码、既有计划、diary、design 或 TWR。分支名称沿用委派给出的 `research-261108`，没有通过 Git 再核实。M0 自动基线不重复执行，也不提升为真实生存/人工画面验收。本文唯一新增文件为本文件；M1-B 完成代码后负责 living docs 和新 diary。

源码相对根为 `src/main/java/com/teammoeg/frostedresearch/`；下文 `knowledge/` 均相对此根。

## 已核实的实现接点

| 当前锚点 | 实际行为与 M1 影响 |
|---|---|
| `knowledge/KnowledgeService.isActive/query` | `isActive` 只读取档案且要求 `KnowledgeAvailability.available`；应直接复用，不能用 `entry` 或 `evidencePool` 代替知识条件查询，后二者包含来件。 |
| `knowledge/definition/ProjectDefinition` | 只有 title/body/results/requirements；requirements 是开始后的 evidence/calculation/experiment，不能改名充当开始条件。 |
| `KnowledgeService.researchStartStatus/startResearch` | 强制一份有效 IDEA，且 `IdeaDefinition.projects` 必须包含项目；实际开始 API 尚没有任务非空限制，绘图台另有限制。 |
| `knowledge/state/ResearchHistory` | 必填单个 `idea`；所有 withCalculation/withEvidence/withExperiment/completedWith/abandon 都复制它。 |
| `knowledge/state/KnowledgeState` | `SCHEMA_VERSION=2`，upgrade 尚无迁移；逐条读取且保留不可读原始内容；按 run UUID 合并历史。 |
| `knowledge/KnowledgeView.create` | 遍历档案想法推导项目；同步 `ideas` 和单个 `origin`；研究历史直接发送 `idea`。目前公开遍历本身没有先过滤 isActive。 |
| `knowledge/link/LinkRule.Slot`、`FieldCondition` | 已能匹配 kind、definition、observation_type、字段比较、object/biome 标签；未知值不满足普通比较。可复用 matcher，勿再造表达式语言。 |
| `knowledge/definition/KnowledgeDefinitions.Snapshot` | 用全部 link pattern 和项目 evidence pattern 判观察用途/等价/独立记录数；当前没有项目开始条件。注册 `ObservationUse` 可补用途。 |
| `KnowledgeService.submitEvidence/evidencePool` | 执行任务可以从档案和来件保存证据副本；这是任务证据，不等于学会知识，M1 保留此边界。 |
| `knowledge/client/KnowledgeJournalData`、`KnowledgeGraphModel`、`KnowledgeLayer` | 项目记录带 ideas/origin，开始包携带单起源，图谱从 ideas 连边；均需同步改。旧 `gui/archive/` 是另一套旧研究 UI。 |

## 1. 共用条件：只做存在性匹配

新增 `knowledge/definition/KnowledgeCondition` 值对象，最少字段：

| JSON 字段 | 语义 |
|---|---|
| `kind` | observation / idea / result，必填。 |
| `definition` | idea/result 必填资源 ID；观察不填。沿用 Slot 的字段名。 |
| `observation_type` | 观察必填 item / block / entity。 |
| `conditions` | 观察字段条件数组，默认空；复用 `FieldCondition.CODEC`，数组内全部满足。 |

实现可把条件转换为固定名称的单个 `LinkRule.Slot`，复用其 matches/validate；对外模型不带槽位名。观察不接受固定 `record_id` 或 `entity_instance` UUID 作为数据定义前置；匹配对象 ID、标签、必要字段即可。ID 精确匹配使用 `object eq`，值沿用 `{state:"known",type:"string",text:"minecraft:clay_ball"}`。标签依照 observation_type 查询对应 registry；物品观察不自动等价于放置方块观察。

推荐最小服务接口：

```text
KnowledgeService.matchKnowledge(KnowledgeCondition) -> Optional<KnowledgeKey>
KnowledgeService.projectAccess(projectId) -> ProjectAccessEvaluation
KnowledgeService.startResearch(projectId) -> Optional<UUID>
```

`matchKnowledge` 只在有效档案中匹配；想法/成果直接按 key 查，观察扫描有效观察。多条记录命中时按 key 稳定排序选一个作为见证。来件、休眠、被停用定义、已忘记条目均不满足。自动学习与手动学习经过同一档案后完全等价。当前档案中已有效的想法不因其获取前置后来缺失而失效。

项目开始使用 all；发现/组披露使用 any；只需要这三处固定组合，不添加递归 AND/OR/NOT 树、计数/评分条件或流程引擎。一条观察可以同时满足不同存在性条件，不占用或消耗；需要两份独立证据继续用 ProjectRequirement 的 slots/distinct。共享条件不是“两次提交”。

### 与想法获取的边界

M2 使用同一个 KnowledgeCondition 表达必要/非必要知识：必要列表 all 只在行为尝试和联络入口检查；非必要列表只影响概率/代价。M1 不提前新增其概率字段、事件注册或计时器。`understanding` 仍是现有学习条件，不自动转换成必要获取条件；通用 receive/produce/learn/外部 grant 不追加获取前置检查。外部普通授予保留原学习语义，命令/初始特殊授予保留原豁免；项目作者可以接受外部入档的想法而省略部分获取前置。

## 2. 项目 access：条件集合与三种引用

在 ProjectDefinition 新增可选 `access`；**字段缺席**表示旧格式适配，不能和显式空对象混同。推荐结构如下，两份完整示例见后文：

```text
access.conditions: Map<项目局部条件名, {上述条件字段, title?, hint?}>
access.start_all: List<条件名>，默认 []
access.discover_any: List<条件名>，默认 []，表示额外发现入口
access.groups: List<{id, reveal_any: List<条件名>, conditions: List<条件名>, unknown_hint?}>
```

`title/hint` 是已公开行的显示文字，不参与匹配；观察行应写 title，idea/result 省略 title 时取对应定义标题。`hint` 是具体寻找方向，`unknown_hint` 是作者主动允许的未知组方向，两者分开。组 ID/条件局部名只用于作者引用与稳定更新，不代表新的知识身份。

规则：

1. 有效发现集合 D = `discover_any` ∪ `start_all` 中所有 IDEA 条件。D 中任意条件有效入档即可发现；因此不能通过漏写 discover_any 阻止“任意前置想法发现项目”。无想法主线必须显式写成果/观察 discover_any。不因 start_all 为空而全局公开项目。
2. 开始知识 = `start_all` 的集合，且项目当前已发现；全部有效才可开始。不展开想法的 understanding、必要或非必要知识。需要包括哪些就显式列出，省略就是作者例外。
3. 每组 `reveal_any` 任意满足即公开该组 `conditions` 指定的关联。组内不隐式添加开始条件。反过来，start_all 不自动公开其内容；所有 start_all 条件应有组覆盖。没有任何组的显式配置可由加载时补一组，reveal_any=D、conditions=start_all，提供普通单入口的短格式。
4. 多组已公开时，按条件身份合并为一份具体行；组只引用该行，不重复计数。条件作者应复用同一个局部名。不同名的完全相同谓词也在加载时合并引用；归一化以 kind/definition/type/无序去重后的 FieldCondition 集合为依据，不使用路径敏感哈希或任意逻辑等价证明。相同谓词不同文案取局部名排序第一份并给内容作者诊断。
5. 项目显示全部必要知识要求：每个尚未披露的必要条件各有一个“未知知识”占位，允许玩家看到未知项数量和要求结构；组只展示来源关联，不把整组压成一个占位。即使 C 已入档，只要关联只属于未知 J 组，C 对应的要求仍显示未知内容，不展示名称或具体谓词；玩家仍能在自己的档案正常看 C。I/J 共用 B 时 I 公开 B 一次，J 与 C 各保留一个未知占位；J 入档后这两个占位更新为具体 J/C，B 不再增加。跨组共享的尚未披露条件同样按条件身份合并，不因组引用重复生成占位。
6. 推荐默认：非想法主线入口按其 reveal_any 公开整组，满足与想法入口同一套实现。没有“成果一律公开所有组”的特殊分支。
7. 允许组包含不在 start_all 的关联（例如作者明确省略的想法前置），显示为“相关参考，不影响开始”，不计入完成数。观察用途仍包含这些条件。这样省略前置与披露关联可以分别表达。

引用不存在等作者数据错误沿用现有加载诊断机制，给项目/字段路径；不删除团队历史或改变无关项目。省略字段采用上述默认，明确写 `access` 后不因错误回退旧想法启动逻辑。groups 非空但遗漏 start_all 覆盖时可补一个默认组（D 披露）并提示作者，避免一个已发现项目永远无法说明其必要条件；M1 示例应显式覆盖，不依赖修补。

### 可见性、生命周期推荐默认

未启动项目按当前有效档案计算发现/披露，不新增永久“发现账本”。忘记或停用最后发现来源后，未启动项目退出普通列表；历史与进行中项目仍可打开，组关联仍按当前披露规则过滤。此决定沿用当前“有效知识”语义，不把被撤销知识当权限。

开始前检查完整 access；开始后不重复把开启前置当持续任务，不因忘记 I、删改入口条件清空 run。任务进度继续按当前 requirements 判定，定义暂缺时保留 run 并显示不可用，恢复后继续。完成/放弃与成果学习仍走原生命周期；重开时重新检查 access，沿用“已有完成记录且全部成果仍在档案则不重复启动”的原规则。本轮不顺手改变完成成果的休眠/遗忘策略。

`ProjectAccessEvaluation` 是服务端计算结果，含 discovered、各条件见证、公开组、startStatus；未持久化，不传完整对象给客户端。优先呈现进行中/已完成状态，再为未启动项目判发现与缺项。UI 项目状态由以下情况投影：未开始且缺知识、可开始、进行中、任务全满可完成、已完成；无任务另附“暂无可执行步骤”。保持玩家按钮/包入口的无任务限制一致，内部/开发完成 API 仍可处理无任务项目，不用新增阶段占位填满它。

## 3. 旧定义和多来源存档

### 没有 access 的项目

在加载时生成兼容访问策略：引用它的启用 IdeaDefinition.projects 是可选发现来源，任意一个有效即可开始。**不能**把全部旧想法变为 start_all，否则改变旧包的多入口语义。用简单 legacy 标记/分支表达该 OR，不扩展新项目表达式树。旧定义的正文/任务/成果不变；无有效来源的旧项目仍不凭空公开。

显式 access 项目的关系以 access 为准，旧 IdeaDefinition.projects 不暗中追加门槛或发现来源。新例子的想法可保留 projects 作为旧数据关系信息，但服务端只使用 access；应提示不一致关联供作者清理，不强迫一次迁移全目录。旧 `config/fhresearches` 的 81 项旧 Research 不是这些 ProjectDefinition，M1 不替换正式目录。

### ResearchHistory

把 `KnowledgeKey idea` 替换为 `List<KnowledgeKey> origins`，表示启动当时实际采用的知识见证集合，允许 observation/idea/result；去重稳定排序。新项目取所有满足的 D 条件与 start_all 条件的见证并集，旧项目保持单个选中的有效想法。仅披露用的非必要参考不加入实际起源。条件观察选中的是运行时 UUID，可写历史，但不可反写数据包固定前置。

不持久化完整条件树或新增条件满足缓存；origins 是历史事实，不是将来再求值的许可。项目规则变化、来源被忘记不重造历史。所有 with*/completedWith/abandon 保留 origins；合并仍以 run UUID 为身份，不因多个来源复制启动。旧队伍合并可能产生同项目多个 open run 的问题不新增另一种合并算法；沿用已有行为并在多人回归中记录，M1 不丢任意已有记录。

存档版本 2 → 3：在 KnowledgeState.upgrade 对副本里的每条 research 记录，把旧 `idea` 包为 `origins:[idea]`；保留 id/project/time/results/completed/abandoned 以及全部进度不变。新字段已经存在时以它为准，不重复追加 idea；保存写新字段。缺版本但有旧 idea 的记录也执行同样的局部兼容转换；ResearchHistory 解码保留旧字段读取后备，便于原始不可读条目重试。来源缺失但其余 run 可恢复时使用空列表并诊断，不伪造想法，不丢进度；其他坏字段继续沿用现有逐条保留机制。历史空来源不意味着可以跳过未来开始条件。

旧 Java `startResearch(project, idea)` 如仍有调用，可暂留薄适配：旧项目使用指定合法想法；新项目转正式 `startResearch(project)` 重新算全条件，传入 idea 不能成为绕过入口。不要保留一个返回“第一份想法”的伪通用 origin getter。

## 4. 观察用途、差异与事件

加载 KnowledgeDefinitions.Snapshot 时，把全部 access.conditions 的观察条件（发现、开始、参考、披露触发）转换为**各自单槽** SlotPattern，与现有 link/evidence patterns 并列。不得把一个项目所有观察条件拼成多槽 pattern，否则会把普通前置错误变成要求多份独立记录。整个定义目录参与，不按项目可见性过滤；未发现项目所需观察因此也可入档。

当前 equivalent 比较每个槽的匹配结果，并在跨槽条件读取的字段上比较原始值。新存在性条件沿用这个语义：温度 10/11 若对所有已注册用途都同真同假，无需保存两份；若以后有条件区分它们则匹配向量不同，允许再入档。跨地点/时间等证据继续使用原 pattern 的 fieldsRead 与独立记录额度；同 UUID 不算独立，复制笔记不增加份数。

M2/M4/M5 加载器应将其必要/非必要、触发谓词、所有后续阶段/分支的观察用途加入同一索引，即使阶段尚未解锁。不为尚无 schema 的 M4 写假阶段字段。M1 提供可复用的“条件 → 单槽用途”辅助入口；以已有 registerObservationUse 测试模拟后续专用用途，证明隐藏用途可保留差异。后续接入需要实际遍历全部阶段定义，不能只在进入阶段时注册。

**观察动作事件与知识入档事件独立。** M1 不把新任务观察接在 learn 成功上；以后一次有效采样的上下文可交给任务事件，即使观察已重复/来件满/不能学也可完成当次任务；反之旧档案满足知识条件不能追溯完成一个“本次试样观察”的任务。任务仍需其项目/run/样品身份限制。M1 只定接口边界，不新增任务事件总线、M4 阶段执行器。

## 5. 服务端快照、UI、图谱、命令

继续通过 TeamResearchService 的合并同步和 KnowledgeSnapshotPacket 分片传输，不增加一条完整定义广播。学习/自动学习、遗忘、合并、重载标签与换队都会重新计算；包处理时再计算，不能相信旧客户端可开始状态。不新增每 tick 扫描全项目的后台任务；首次按当前定义/档案直接算，性能优化以实际测量为准。

`KnowledgeView.projects` 替换 ideas/origin 为 `sources`（当前允许公开的发现来源 key 列表），保留 title/body/outputs/requirements/open_run 等现有字段。增加：

- `start_status`：可开始/未发现/缺知识/已进行/已完成或定义不可用等服务端结果，客户端翻译。
- `access_conditions`：全部必要条件行，以及已公开的非必要参考行。每行有稳定视图行 ID、required、hidden；未披露必要条件各自占一行，不附知识名称、定义 ID、标签、具体谓词或匹配 key。已公开行另有 title、hint、met 及匹配 key（便于跳转）。未满足时可给 `inbox_keys` 指向已收到但未入档的匹配条目；来件存在不令 met 为真。观察多个候选只给适量入口，UI 可转来件筛选。视图行 ID 用于占位更新与去重，不使用知识名称作为占位文案。
- `groups`：携带组视图 ID、hidden、unknown_hint，以及对应的条件行 ID 引用；可以引用未知占位和已经由其他组公开的共享行。组的数量、成员数量及要求结构允许展示，不额外设计隐藏数量机制；未获想法的来源名称仍不展示。条件总数以去重后的 access_conditions 必要行计算，不能累加各组引用数。

不要把 access 或 KnowledgeCondition CODEC 直接写入 UI。已公开行用作者文字/定义标题和服务端算好的状态即可，不必下发可执行谓词。开始按钮禁用理由可以“还有未知知识要求需要探索”，并显示逐项未知占位及数量，不列出其具体知识名称。这里的裁剪只服务逐步披露玩法，不扩展成额外安全设计。body/title/任务标题本身是作者有意公开的文本，例子不要在正文提前写出隐藏 C。

历史 `research.origins` 也要经过展示投影：新格式只发当前已公开关联允许的来源；内部完整 origins 不直接塞进客户端。历史模式不豁免隐藏组。对旧格式/迁移的起源保留原历史引用行为；对定义缺失的新格式仅展示 run 与结果摘要，勿借未知定义推导隐藏关系。判断旧历史可在迁移时用其旧单想法关系适配信息，或用当前缺席 access 的项目；实现时需覆盖“旧记录对应项目后来加 access”的回归，采用更严格的当前组裁剪即可，不额外建披露历史存档。

图谱从 sources 连发现虚线，从裁剪后的 origins 连实际实线，支持成果/观察节点；已有档案节点不代表允许连出隐藏项目关联。按项目 ID + key 去重边；共享 B 只一个节点/关联，不为每组克隆知识。已放弃记录继续仅历史展示。

绘图台 `START_RESEARCH` 只需 project ID；去掉自动挑首个想法。研究页区分“开始所需知识”与“开始后任务”，前者列出全部去重后的必要条件，未披露项逐项占位；组作为来源关联展示。已公开参考标注非必要；已收到未入档提供跳转来件。保留当前任务交互和无任务说明。

`/knowledge research complete <project>`：优先复用 open run，无需重新持有来源；没有 run 时使用正常 access 开始，再跳过任务完成，仍不跳过开始知识。旧 `[起源想法ID]` 参数保留作旧项目兼容选择；新项目忽略其起源选择作用并仍按完整 access 检查，可提示已采用项目来源。纯成果项目无参数可运行。补一个普通只读 `research status <project>` 更利于定向调试（可选，不为它阻塞 M1），若做则只展示同一披露投影。管理员完整数据检查无需设计新的权限系统。

## 6. 两份具体数据例子

以下是 **M1-B 拟实现 schema 的测试数据**，当前加载器尚不支持 access。使用独立 example 命名空间，不冒充正式三个项目内容；想法/成果定义按现有格式一并提供。可放入知识示例包与 GameTest fixtures，TWR 正式内容留后续阶段。

### 多想法 I/J，A/B 与 B/C，允许省略前置

前提：已定义 `example:i`、`example:j` 两个想法（understanding=[]），`example:joint_finding` 成果。A=粘土球观察，B=沙观察，C=木炭观察。I 的相关前置 A/B，J 的相关前置 B/C；这些获取规则是 M2 的配置责任，本项目只声明自己的条件。

文件 `data/example/frostedresearch/knowledge/projects/joint_study.json`：

```json
{
  "title": "材料研究",
  "body": "汇集不同思路，再整理实验记录。",
  "results": ["example:joint_finding"],
  "access": {
    "conditions": {
      "i": {"kind": "idea", "definition": "example:i"},
      "j": {"kind": "idea", "definition": "example:j"},
      "a": {"kind": "observation", "observation_type": "item", "title": "检视粘土球", "conditions": [{"field": "object", "operator": "eq", "value": {"state": "known", "type": "string", "text": "minecraft:clay_ball"}}]},
      "b": {"kind": "observation", "observation_type": "item", "title": "检视沙", "conditions": [{"field": "object", "operator": "eq", "value": {"state": "known", "type": "string", "text": "minecraft:sand"}}]},
      "c": {"kind": "observation", "observation_type": "item", "title": "检视木炭", "conditions": [{"field": "object", "operator": "eq", "value": {"state": "known", "type": "string", "text": "minecraft:charcoal"}}]}
    },
    "start_all": ["i", "j", "a", "b", "c"],
    "groups": [
      {"id": "from_i", "reveal_any": ["i"], "conditions": ["i", "a", "b"]},
      {"id": "from_j", "reveal_any": ["j"], "conditions": ["j", "b", "c"], "unknown_hint": "还需要另一种思路。"}
    ]
  },
  "requirements": [{"id": "tabulate", "type": "calculation", "title": "整理记录", "level": 0, "count": 1}]
}
```

I 入档即发现并显示 I/A/B，另显示 J、C 对应的两个未知知识占位，共五项必要条件；J/C 的具体内容不发。仅预先掌握 C 不公开其名称或与项目的具体关联，仍保留 C 对应占位。J 入档后两个占位更新为具体 J/C，B 仍一份，总要求数仍为五。I/J/A/B/C 入档才可开始。

**作者例外的具体修改：** 将 start_all 改为 `["i","j","b","c"]`，其余不改，A 仍在 I 组作为参考显示，但不阻止开始。即使未来 I 的行为必要条件包含 A，外部授予并有效入档 I 后，这个项目也不检查 A；不能运行时自动补回它。若不想展示 A 的关联，同时从该组 conditions 去掉 a，且无其他用途时可删除 conditions.a。

### 纯成果主线入口，不造占位想法

已定义 `example:smelting_finding`（上一项目成果）、`example:followup_finding`。`smelting_finding` 可由研究或外部传播后入档，来源机制不改变是否掌握。

文件 `data/example/frostedresearch/knowledge/projects/followup.json`：

```json
{
  "title": "冶炼记录整理",
  "body": "根据已有成果继续整理记录。",
  "results": ["example:followup_finding"],
  "access": {
    "conditions": {"previous": {"kind": "result", "definition": "example:smelting_finding"}},
    "discover_any": ["previous"],
    "start_all": ["previous"],
    "groups": [{"id": "mainline", "reveal_any": ["previous"], "conditions": ["previous"]}]
  },
  "requirements": [{"id": "summarize", "type": "calculation", "title": "整理成果", "level": 0, "count": 1}]
}
```

成果只在来件时不发现；入档后公开主线组并可开始。历史 origins 为成果 key，图谱成果→项目→新成果。若后续项目另需观察，在 start_all 与该组 conditions 中添加对应条件即可，主线入口到达后公开该缺项，无需加想法。

## 7. M1-B 执行顺序与文件范围

按下面顺序做同一闭环；不用等 M2/M4 schema 定稿：

1. **数据与判定**：新增 `knowledge/definition/KnowledgeCondition.java`、`ProjectAccess.java`（可含小型嵌套记录）；修改 ProjectDefinition、KnowledgeDefinitions、KnowledgeService。复用 Slot/FieldCondition；把 legacy/no-access 和新显式 access 区分清楚，统一服务端 projectAccess。
2. **观察索引与存档**：KnowledgeDefinitions.Snapshot 加单槽用途；ResearchHistory、KnowledgeState 升级与所有复制方法；必要时调整 ObservationUse 辅助适配，避免改原等价规则。
3. **协议与命令**：KnowledgeView、`knowledge/network/KnowledgeWorkbenchActionPacket.java`、KnowledgeCommands。KnowledgeSyncSnapshot/KnowledgeSnapshotPacket 原传输机制原则上不需改，仅真实类型编译依赖时调整。搜索所有 ResearchHistory 构造和 `.idea()` 调用，包括测试。
4. **客户端完整接通**：`knowledge/client/KnowledgeJournalData.java`、KnowledgeLayer、KnowledgeGraphModel、KnowledgePresentation；需要时 `client/ui/KnowledgeNodeSheet.java`、`KnowledgeGraphViewport.java`。语言文件已核对为 `src/main/resources/assets/frostedresearch/lang/zh_cn.json` 与 `en_us.json`；不要顺手改旧 `gui/archive/` 图谱，除非发现实际共用调用。
5. **可复现例子、测试与文档**：扩展 `src/gametest/java/com/teammoeg/frostedresearch/ResearchGameTests.java` 或其 knowledge 子目录专门测试类及现有 fixture 资源；检查测试载入注册方式。更新 `docs/knowledge/definitions.md`、`state-and-api.md`、`workbench.md`、`observations-and-notes.md` 的实际受影响段落及 example-datapack。追加自己的 diary，记录代码/自动测试/客户端/人工验证的边界。

M1-B 不改 TWR 配方、正式项目、旧81项处置、原型设备、风箱、控温炉、阶段数据。若确需跨仓内容工作先另按 companion AGENTS 和两仓检查规则处理，不随本接口任务扩大范围。

## 8. 验收场景与完成判据

| 场景 | 应验证结果 |
|---|---|
| I/J 共享 B | 仅 I 入档：项目可见，I/A/B 具体，J/C 各一个未知占位，共五项必要条件；C 即使已有仍为未知占位；J 后补，两个占位更新为具体内容，B 行与边只一份。跨组共享未知条件也只占一行。 |
| 入档边界 | I 在来件不发现；A/B 在来件不满足；手动/自动学后下一份快照立即更新；停用/休眠与遗忘不能满足。 |
| 全条件与作者例外 | 完整版缺任意一项不开始；删掉 start_all.a 后外部学到 I、缺 A 也可开始，参考 A 明确非必要。 |
| 成果/观察入口 | 纯成果例子全流程，另将入口替换为标签观察；无需 IdeaDefinition.projects；item/block/entity 类型和未知字段分别测。 |
| 开始请求竞争 | 两成员同时开始只建立一次 open run；旧 UI 状态下撤销知识后发包应按新状态给缺项反馈，不创建半条记录。 |
| 观察用途 | 仅新前置引用的观察可入档；同真值无新用途的重复不入档；新条件区分的记录可入档；隐藏用途不因未发现消失。 |
| 独立证据 | 新普通前置不提高独立记录额度；已有双槽/跨地点任务仍收两份独立证据；抄录相同 UUID 不冒充第二份。 |
| 后续用途接点 | 注册模拟未来分支 ObservationUse，在无可见项目时仍能保留有意义差异；不声称已测不存在的 M4 阶段加载器。 |
| 快照逐步披露 | 直接检查 NBT：所有必要条件都有行，未知 J/C 各有视图行 ID 与组引用，数量及要求结构可见，但无其名称、具体知识身份或谓词；允许 C 出现在档案。普通及历史图谱、研究 origins 不提前显示未知项的具体知识关联。 |
| 兼容旧定义 | 无 access 的项目两个旧想法任一个可启动，不要求同时拥有；显式 access 不被旧 projects 字段添加前置。 |
| 存档往返 | v2 单 idea → v3 origins；各类进度、UUID、完成/放弃状态不变；第二次加载不再次追加；缺来源保留可用进度；真实服务端重启单列验收。 |
| 来源遗忘/重载 | 进行中不撤销进度；旧历史不重新编造起源；定义暂缺后恢复可继续；重新开始重新查当前条件。 |
| 命令与结果 | 无参数 complete 支持纯成果；open run 不被忘记来源阻断；无 run 不跳过开始知识；重复完成不重复发成果。 |
| 玩家 UI | 五种状态、全部必要要求与逐项未知占位、共享条件去重、未知项数量与组结构、来件跳转、参考非必要、纯成果图谱可读；小窗口仍可操作。实际画面/键鼠单独验证，不能以 NBT 断言代替。 |

M1-B 建议使用 M0 已给出的 Java17 环境与完整 compileJava/compileGameTestJava，再运行覆盖新场景的研究定向 Forge GameTest；不写已移除的 JUnit 套件，不以复制构建或路径哈希作为验收。旧目录未改时不必反复以81项 preflight 代替新机制测试。真实重启和客户端结果如暂未执行须在交接明确待验，不阻断已授权的其他实现。

## Outcome

M1-A 已给出可实现的最小数据/API、旧格式与存档兼容、观察用途接入、隐藏组投影、两份 JSON 和验收范围。推荐默认已明确，无需等待常规设计确认。M1-B 可直接按第7节推进；M2 获取概率/联络计时、M4 阶段/原型以及正式包材料游玩继续保持各自里程碑边界。

文档自检：两段 JSON 通过解析，开始/披露组引用均能解析到条件；6 个相对 Markdown 文档链接存在。这只是拟定数据和交接文件的一致性检查，不表示当前加载器支持新字段或 M1 行为已通过测试。

后续修订（2026-11-12，主协调转达用户明确规则）：未知必要条件改为逐项占位，允许展示数量与要求结构；共享条件去重，组只作来源关联展示。同步更新服务端视图、UI、示例解释和验收，取代原先整组单占位及隐藏成员数的建议。其余推荐默认不变。
