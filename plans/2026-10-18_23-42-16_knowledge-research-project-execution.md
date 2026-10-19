# 知识研究项目执行：计算、证据与实验

- Time: `2026-10-18 23:42:16 -0500`
- Authors: `Claude (Anthropic Opus 5.5), 计划起草；项目所有者确认「决定」一节`
- Status: `ready`（`2026-10-18 23:58:00 -0500` 项目所有者确认全部采用推荐方案）
- Scope: `frostedresearch/knowledge` 的研究项目定义、启动记录与进度；绘图台纸牌局；新增实验台；知识札记研究页
- Related: [`design/knowledge_research.md`](../design/knowledge_research.md)、[V2 计划](2026-08-25_16-17-39_generative-research-knowledge-laboratory-v2.md)、[知识系统审阅](../discussion/2026-10-18_23-01-04_knowledge-system-development-review.md)、[`docs/knowledge/`](../docs/knowledge/README.md)、[`docs/research/`](../docs/research/README.md)

## 目标

让「想法 → 研究项目 → 成果」这一段不再依赖管理员指令。设计文档给研究项目定了三类需求：

- **计算**：通过绘图台中的元素消除小游戏等方式完成。
- **证据**：提交满足项目要求的观察记录，并达到相应数量与独立性要求。
- **实验**：建造符合要求的实验空间，并提交相应材料或完成指定操作。

它同时写明「具体执行方式、进度计算和完成规则留待研究系统设计阶段展开」。本计划就是这一阶段的方案：把 V2 计划和设计文档对齐，给出一个尽量复用现有机制的最小实现。项目所有者已确认的玩法决定见「决定」一节。

## 已核实的现状

以 `a8d89e5f2` 为准。

### 知识侧已有的接口

| 内容 | 位置 | 现状 |
|---|---|---|
| 项目定义 | [`ProjectDefinition`](../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java) | 只有 `title` 和 `results`，没有需求字段 |
| 想法可启动的项目 | [`IdeaDefinition.projects`](../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/IdeaDefinition.java) | 已有；加载时校验项目存在 |
| 启动与完成 | [`KnowledgeService.startResearch` / `completeResearch`](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java) | 启动要求起源想法当前有效、且声明了该项目，返回启动 UUID；完成时按项目定义的成果调用 `produce`，学不了的成果进来件 |
| 启动记录 | [`ResearchHistory`](../src/main/java/com/teammoeg/frostedresearch/knowledge/state/ResearchHistory.java) | `id, project, idea, time, completed, results`，没有进度 |
| 玩家可见 | [`KnowledgeView`](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java) | `projects` 列出已掌握想法公开的项目；`research` 列出实际启动记录；脉络图用虚线和实线连接 |
| 开发捷径 | `/knowledge research complete <项目> [想法]` | 权限等级 2，跳过全部任务 |
| 观察用途 | `KnowledgeDefinitions.Snapshot.observationRules`、`ObservationUse` | 联络规则槽位决定观察是否「有用途」和是否「增加差异」；外部需求可以登记 `ObservationUse`，目前没有登记方 |
| 条件语言 | [`LinkRule.Slot`](../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkRule.java)、`CrossCondition`、[`FieldCondition`](../src/main/java/com/teammoeg/frostedresearch/knowledge/link/FieldCondition.java)、[`LinkMatcher`](../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java) | 有类型的单字段条件、标签、跨槽比较、距离；完整无序绑定，每槽绑定不同 RecordId |

### 旧研究可复用的部分

| 机制 | 位置 | 现状 |
|---|---|---|
| 纸牌局 | `gui/drawdesk/game/ResearchGame`、`GenerateInfo.all[0..3]` | 9×9 棋盘，四个难度级别 |
| 纸牌局的绑定 | [`DrawingDeskTileEntity.initGame` / `updateGame`](../src/main/java/com/teammoeg/frostedresearch/blocks/DrawingDeskTileEntity.java) | 难度来自 `ResearchHooks.fetchGameLevel`（旧研究当前课题的第一个未完成 `MinigameClue`），胜利交给 `commitGameLevel`；每张桌子只有一局 |
| 纸墨消耗 | `ResearchPaperRecipe`、`IPen` | 开局耗一张纸和 5 点笔耐久，每次配对再耗 1 点 |
| 机械计算器 | `MechCalcTileEntity` | 用 Create 动力产生旧研究点数 |
| 物品线索 | `ItemClue`、检视槽提交 | 只服务旧研究 |

### 城镇侧

- `Resident` 已有 `educationLevel` 与 `intelligence`；`TownStaffingPlan` 负责岗位分配；还没有研究所建筑。
- [`ConfinedSpaceScanner(Level, BlockPos, maxScanBlocks)`](../src/main/java/com/teammoeg/frostedheart/content/town/block/blockscanner/ConfinedSpaceScanner.java) 是有上限的封闭空间扫描，带空气格与边界格两个钩子，可以用来检查实验空间。

### V2 计划的位置

V2 计划写于 2026-08-25，Phase 1（五类成果与权限投影）已经实现。设计文档 `design/knowledge_research.md` 写于 2026-09-08，知识系统按它实现。V2 中关于想法来源、讨论和工作页的部分早于设计文档，并与之冲突（见下节）。

## V2 与设计文档的对齐

| V2 概念 | 设计文档 | 本计划的处置 |
|---|---|---|
| 证据板：钉记录、玩纸牌局、产出想法候选 | 想法来自联络、外部学习和研究所；纸牌局是项目的「计算」 | 不采用 V2 做法；纸牌局改作计算需求 |
| 讨论是研究劳动，产出候选解释与建议 | 讨论触发联想、获得暗示 | 讨论不做研究需求；暗示部分已实现 |
| 研究工作页 + 五种缺口 + resolver 推荐行动 | 项目有计算/证据/实验三类需求 | 用作者写的需求清单代替缺口编译 |
| 结果停在「等待审阅」，玩家接受后才发布 | 完成后调用统一学习方法 | 见 B3 |
| 一份 topic JSON 写全部来源、灵感、protocol、结果 | 想法、联络、项目、成果分开定义 | 需求写进 `knowledge/projects`；topic 目录只保留为成果后备 |
| KnowledgeOffer | 来件区 + 观察记录窗口 | 知识系统已实现，不再需要 |
| 实验台、空间能力、Protocol | 建造实验空间、提交材料或完成操作 | 保留思路，简化后放进 R2 |
| 研究所：讨论/计算/实验三列劳动 | 居民可参与三类任务，研究所居民寻找联络 | 保留建筑与排班思路，改为计算/证据/实验（及联络搜索），另立计划 |
| 五种成果 | 相同 | 已实现，不变 |
| Prototype 实体与安装界面 | 留待后续设计 | 不在本计划内 |
| 研究点、insight | 未提及 | 新项目不使用点数 |

## 方案概要

### 数据：需求写进项目定义

```json
{
  "title": "knowledge.example.projects.bird_tracking.title",
  "body": "knowledge.example.projects.bird_tracking.body",
  "results": ["example:bird_map"],
  "requirements": [
    {
      "id": "two_sites",
      "type": "evidence",
      "title": "knowledge.example.projects.bird_tracking.two_sites",
      "inputs": [
        {"name": "a", "kind": "observation", "observation_type": "entity",
         "conditions": [{"field": "object", "operator": "tag", "tag": "example:birds"}]},
        {"name": "b", "kind": "observation", "observation_type": "entity",
         "conditions": [{"field": "object", "operator": "tag", "tag": "example:birds"}]}
      ],
      "conditions": [{"kind": "distance", "left": "a", "right": "b", "operator": "gte", "value": 1000}]
    },
    {
      "id": "tabulate",
      "type": "calculation",
      "title": "knowledge.example.projects.bird_tracking.tabulate",
      "level": 1,
      "count": 2
    }
  ]
}
```

- `id` 在项目内唯一，作为进度身份。增删需求不会让其他需求的进度错位。
- 证据需求直接复用联络规则的槽位与跨条件语言。把 `LinkRule` 的 `inputs + conditions` 抽成共享的槽位模式，联络规则和证据需求各自规定槽位数和条件数上限。
- 计算需求只有 `level`（纸牌局难度 0–3）和 `count`（需要赢几局）。
- 实验需求在 R2 定义（见 E 组决定）。
- 没有 `requirements` 的项目沿用现状：只能用开发指令完成。

### 运行：启动记录带进度

`ResearchHistory` 增加三个可选字段：`status`（进行中 / 已完成 / 已放弃）、按需求 `id` 记录的进度、以及证据的已提交绑定（槽位 → 观察副本）。进度属于队伍，任何成员都可以推进。

| 操作 | 服务端判定 |
|---|---|
| 启动 | 起源想法有效且声明该项目；遵守 B1、B2 |
| 提交证据 | 选中的观察来自档案或来件；用该需求的槽位模式做完整无序绑定；成立则保存观察副本 |
| 计算 | 绘图台这一局记录目标（旧研究，或某次启动的某项需求）；赢下一局且难度不低于要求时加一 |
| 完成 | 全部需求满足（按 B5 判定）；调用现有 `completeResearch` |
| 放弃 | 见 B4 |

证据槽也登记为观察用途：能匹配证据槽的观察不再显示「暂未发现用途」，重复判定也把证据槽算进去。这与设计文档「用于重复判定的知识用途，包括联络规则，以及其他系统向知识系统显式登记的观察需求」一致。

### 界面：札记中的「研究」页

- 左侧列出已公开的项目和进行中的启动记录；右侧是需求清单与进度。
- 证据：复用档案页的「放到桌面」选择，点某项证据需求的「提交」。
- 计算：点「开始演算」，打开现有纸牌局界面，本局目标绑定到这项需求；研究页显示纸与笔槽。
- 完成：全部需求满足后出现「完成研究」。
- 脉络图中的项目浮纸可以跳转到这里。

### 实验（R2）

新增实验台方块，作为实验空间的锚点：

- 玩家选择要做的实验需求后点「检查」；用 `ConfinedSpaceScanner` 从实验台扫描一次，有上限、按需刷新，运行中不逐 tick 扫描。
- 检查通过后放入材料，开始时一次性消耗。
- 持续指定的游戏刻数，期间低频复查条件；条件中断时的处理见 E3。
- 完成后进度加一。

## 决定

项目所有者于 `2026-10-18 23:58:00 -0500` 确认：以下 25 项全部采用起草时的推荐方案。编号保留，方便后续引用。

### A. 总体

- **A1** V2 计划标为 `superseded` 并链接到本计划。Phase 1 已实现的部分不受影响；其中的实验台、研究所、Prototype 章节保留作为参考。
- **A2** 需求写在 `knowledge/projects/*.json`，不采用 V2 的 topic JSON。
- **A3** 81 项旧研究保持不变，与新项目并存，本计划不做迁移；配方等权限继续由投影合并。

### B. 启动记录的生命周期

- **B1** 启动不需要额外成本。
- **B2** 同一队伍同一项目同时只能有一次进行中的启动。项目完成后，只有当它的某项成果不在档案里（例如被忘记）时才能再次启动。
- **B3** 需求全部满足后，由成员在绘图台点「完成研究」；不引入 V2 的「审阅」状态。
- **B4** 允许放弃进行中的启动：已提交进度作废，记录保留并标为已放弃，脉络历史视图可见。
- **B5** 已提交的进度每次按当前定义重新判定，与联络在图谱中的「有效」判定一致。作者修改条件后，玩家可能需要重新提交。

### C. 计算

- **C1** v1 只用现有纸牌局。机械计算器以 Create 动力代替一局、居民演算，都放到后续。
- **C2** 参数为 `level`（0–3）和 `count`（需要赢几局）。
- **C3** 沿用旧纸墨规则：纸张等级与笔等级都不低于难度，开局耗 5 点笔耐久，每次配对耗 1 点。

### D. 证据

- **D1** 复用联络规则的槽位与跨条件语言，一次提交绑定全部槽位。证据需求允许 1–8 个槽位、最多 10 条跨条件，并新增 `distinct` 条件，表示列出的槽位在某字段上两两不同。
- **D2** 默认只要求不同 RecordId；更强的独立性（不同观察者、相距距离、不同时间）由作者用跨条件写出。
- **D3** 档案和来件里的观察都可以提交。
- **D4** 提交不消耗观察；启动记录保存观察副本，之后删除来件或忘记档案条目都不影响已提交的证据。同一观察可以用于不同项目，但在同一需求内不能重复。
- **D5** 证据槽登记为观察用途，参与「是否有用途」与重复判定。
- **D6** 需求文字由作者撰写，不自动把条件翻译成文字。服务端在快照中给出每个槽位当前可用的观察（有数量上限），研究页据此高亮。

### E. 实验（R2）

- **E1** 新增实验台方块，作为空间检查的锚点。
- **E2** v1 支持四项空间条件：是否封闭、空间体积范围、实验台处的温度范围（`WorldTemperature`）、空间内必须存在的方块（方块或标签加数量）。动力、机器接入和 V2 那种装置能力放到后续。
- **E3** 条件不满足时暂停计时，不清进度、不退材料。
- **E4** 实验进行时不要求玩家在场，区块加载即可；居民值守放到研究所阶段。
- **E5** v1 不做「完成指定操作」，只预留一个由 Java 注册的操作扩展点。

### F. 范围

- **F1** 本计划只预留接口，需求进度不假设执行者一定是玩家。研究所建筑、居民执行计算/证据/实验、居民联络搜索（含审阅 P2-7 的回溯上界）另立计划。
- **F2** Prototype 不在本计划内。

### G. 界面

- **G1** 札记新增第五个书签「研究」，脉络图的项目浮纸可以跳转过去。

## 分阶段步骤

前置：审阅 P0-1（持久化逐条解码、`schema_version`）与 P1（合并同步、权限投影缓存）两个会话的改动已经合入。本计划要在 `KnowledgeState` 里新增持久化字段，也会增加同步触发点，必须建立在它们之上。

### R1：计算 + 证据 + 研究页

1. **定义**：`ProjectDefinition` 增加 `body`、`requirements`；把 `LinkRule` 的槽位与跨条件抽成共享槽位模式，并新增 `distinct`；校验需求 `id` 唯一、槽位与条件合法、`level` 在 0–3、`count ≥ 1`。证据槽并入 `Snapshot` 的观察用途。
2. **状态**：`ResearchHistory` 增加可选的 `status`、进度和证据绑定；读取沿用 P0-1 之后的逐条宽松解码。
3. **服务**：`KnowledgeService` 增加 `submitEvidence`、`recordCalculation`、`requirementsMet`、`abandonResearch`；`completeResearch` 改为要求需求满足，开发指令保留一个跳过判定的入口。
4. **纸牌局目标**：`DrawingDeskTileEntity` 的这一局记录目标（旧研究，或「启动记录 + 需求」）；胜利时提交给对应目标。旧研究流程不变。
5. **网络与视图**：绘图台新增 `START_RESEARCH`、`SUBMIT_EVIDENCE`、`CALCULATE`、`COMPLETE_RESEARCH`、`ABANDON_RESEARCH` 动作；`KnowledgeView` 增加需求说明、进度与（按 D6）槽位可用观察。
6. **界面**：札记「研究」页（G1）；研究页显示纸与笔槽；脉络浮纸跳转。
7. **内容**：给示例包两三个项目加上需求（一个距离证据、一个计算），更新 `playthrough.md`。

### R2：实验

1. 实验台方块、方块实体与界面，固定材料槽。
2. 空间检查（`ConfinedSpaceScanner`，有上限、按需刷新）、温度与方块条件。
3. 运行计时、低频复查与 E3 的中断规则。
4. 示例包加一个实验项目。

### 之后（另立计划）

- 研究所与居民：执行三类需求、联络搜索、居民讨论。
- 机械计算器作为计算方式。
- 新的知识来源接入（难民/剧情对话、世界生成研究笔记）。
- Prototype。

## 验证

- 单元测试：需求定义校验；启动记录进度的读写往返与宽松解码；证据绑定（不同 RecordId、跨条件、`distinct`、档案与来件来源、观察副本）；计算提交的目标；完成门槛；B5 的重新判定。
- GameTest：绘图台纸牌局的目标绑定；实验台空间检查与中断（R2）。
- 游戏内：用示例包走一遍「联络 → 启动 → 提交证据 → 演算 → 完成 → 获得成果 → 配方解锁」，并在多人下由不同成员推进同一项目。

## 文档影响

- `docs/knowledge/definitions.md`：项目需求格式与校验。
- `docs/knowledge/state-and-api.md`：启动记录生命周期、进度与证据规则、新增服务方法。
- `docs/knowledge/workbench.md`：研究页。
- `docs/knowledge/playthrough.md`：示例包的完整研究路线。
- `docs/research/gameplay-and-integrations.md`：纸牌局目标；R2 实验台。
- 每个阶段完成后追加 `diary/`。

## Outcome

计划已 ready，实现尚未开始。前置的审阅 P0-1 与 P1 已合入（`05f3aa8b4`、`1bdf58fba`，合并提交 `0c448bb3f`），R1 可以开工。
