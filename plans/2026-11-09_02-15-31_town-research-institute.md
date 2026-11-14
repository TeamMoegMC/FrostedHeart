# 城镇研究所：知识容量、研究人力与联想

- Time: `2026-11-09 02:15:31 -0600`
- Authors: `Claude (Anthropic Opus 5.5), 计划起草与实现；项目所有者提出需求`
- Status: `completed`（2026-11-09；功能三按 G 另立计划，游戏内验收由项目所有者进行）
- Scope: 新城镇建筑 `research_institute`；`frostedresearch/knowledge` 的容量扩展、项目需求的人力协作、居民联想；城镇日结算
- Related: [`design/knowledge_research.md`](../design/knowledge_research.md)（「研究所」「观察的分组与容量」两节）、[研究项目执行计划](2026-10-18_23-42-16_knowledge-research-project-execution.md)（F1）、[知识系统审阅 P2-7](../discussion/2026-10-18_23-01-04_knowledge-system-development-review.md)、[`docs/town/`](../docs/town/README.md)、[`docs/knowledge/`](../docs/knowledge/README.md)

## 目标

项目所有者的需求（2026-11-09）：

- 研究所是新的城镇建筑。房间检测与仓库相同；房间平均照明足够亮、温度足够高时才能工作。
- 功能一：维持知识库容量。来件区与档案区的容量由研究所内的书架决定，书架越多容量越大。
- 功能二：居民在研究所做研究，形成「研究人力」。研究人力按教育等级分级，高等级可以做低等级的需求；以标准工人计，内部为小数，展示时向下取整；每个绘图台支持一名研究员，面积也要随人数增加；智力是主要加成，装饰方块提高效率；工作能锻炼智力，比猎人更多。
- 空闲人力每天进行一次联想，可能随机得到新的想法。
- 计算与实验需求增加数据包选项：人力协作是否必须。不必须时，人力减少玩家自己做小游戏或实验的次数；必须时，作者可以写人数、天数和每天消耗的物品（如纸和墨），物品直接从仓库调用。
- 功能三：探索派遣型证据收集。需求只给出名称，见「功能三」一节。
- 以上功能都以接口的方式接入知识系统。

## 已核实的现状

以 `fdacc2bc7` 加工作区未提交的仓库货架修复为准。

| 内容 | 位置 | 现状 |
|---|---|---|
| 观察容量 | `FRConfig.knowledgeArchiveObservations` / `knowledgeInboxObservations` | 固定配置 4096 / 512，`KnowledgeService` 在构造时读取；想法、成果不计容量 |
| 研究所接口 | `KnowledgeService.candidateLinks(limit)` | 没有调用方；回溯没有步数上限（审阅 P2-7） |
| 项目需求 | `ProjectRequirement` | 计算只有 `level`、`count`；实验有 `count`、`duration`、`materials`、`space`；没有人力字段 |
| 启动记录 | `ResearchHistory` | `calculations`、`evidence`、`experiments` 三类进度 |
| 联想（暗示） | `KnowledgeService.associate(topic, day, opportunity)` | 梦境与讨论使用；每个机会每天一次 |
| 有效人力 | `TownMathFunctions.linearResidentProductivity` | 狩猎、采矿、货运站都按「标准工人 = 四项属性 50、熟练度 0 时为 1.0」计算 |
| 运力 | `VirtualResourceType.TRANSPORT_CAPACITY` | 货运站每天以 `VirtualResourceAttributeAction` 加入；`resetAllServices` 只清 level 0 |
| 智力成长 | `Resident.recordDailyActivity(physical, learning)` | 狩猎 learning 0.25；成长在 `tickResidentsAging` 结算 |
| 家具装饰 | `TownMathFunctions.calculateDecorationRating`、`frostedheart:town/decorations` | 住宅使用；整格方块按墙计，需用 `countAllBlock` |
| 绘图台 | `frostedresearch:drawing_desk` | 两格方块，主格 `not_multi_main=false`；碰撞箱 15/16 高，扫描时按墙处理；打开时要求亮度 ≥ 8 |
| 仓库取物 | `TownResourceActions.ItemResourceAction(COST)` | 住宅从仓库取食物，数量可以是小数 |
| 教育等级 | `Resident.educationLevel` | 0–5，招募时按权重 15/50/20/10/4/1 % 生成，之后不变 |

## 决定

以下为起草时的选择。凡是需求没有写明的，都取最小、可解释、可配置的做法。

### A. 建筑

- **A1** 方块 `frostedheart:research_institute`，建筑类型键 `researchInstitute`，Java 类 `ResearchInstituteBuilding`。旧整数映射不变；FH 网络协议由 `building-data-1` 改为 `building-data-2`，两端须一起更新。
- **A2** 房间扫描沿用 `BlockScanner.scanRoomDataFromBlock`，要求有门。书架、绘图台、装饰都按 `countAllBlock` 计（房间内与六邻接的方块），与仓库货架修复一致。
- **A3** 三个方块标签，数据包可以扩展：
  - `frostedheart:town/institute_bookshelves` = `#forge:bookshelves`；
  - `frostedheart:town/research_desks` = 绘图台；只计主格，两格的绘图台算一张；
  - `frostedheart:town/institute_decorations` = 讲台、制图台、酿造台、附魔台、花盆与盆栽、蜡烛、旗帜、灯笼、饰纹陶罐、雕纹书架、唱片机。
- **A4** 照明取房间内全部可站立位置的**方块光**平均值，默认要求 ≥ 8（与绘图台的「太暗」阈值一致）。不计天光：天光随昼夜变化，而结算在清晨；只计灯光让判定稳定。
- **A5** 温度与狩猎基地相同：房间温度采样加热网修正值，默认要求 ≥ 10 °C（狩猎基地为 0 °C）。核心方块北面接热网，消耗与修正参数与狩猎基地相同。
- **A6** 可工作 = 已初始化、结构有效、无重叠、面积 ≥ 9、体积 ≥ 18、温度与照明合格。
- **A7** 岗位数 = min(绘图台数, ⌊空间评分 × 面积 / 4⌋)，与住宅「床数 × 面积」的写法相同。

### B. 功能一：知识容量

- **B1** 容量仍只限制**观察**。设计文档写的是观察记录的容量；想法与成果来自数据包、数量有限，而且研究产出不能因容量而丢失。
- **B2** 知识系统新增 `KnowledgeCapacity` 扩展点：其他系统登记 `Provider`，按队伍返回额外的档案与来件观察容量。队伍上限 = 配置基础值 + 各扩展之和。
- **B3** 研究所登记一个扩展：每座**可工作**的研究所提供 `书架数 × 16` 档案、`书架数 × 4` 来件。研究所停止工作（变冷、变暗、结构失效）时容量随之撤回，已保存的记录不删除，只是不能再新增，与「降低配置」的现有规则相同。
- **B4** 基础值默认由 4096 / 512 改为 128 / 64，让容量成为玩法约束。已有存档的服务器配置文件保留原值。
- **B5** 快照增加 `capacity`（已用、上限、基础值），札记的档案与来件页显示「已用/上限」。

### C. 功能二：研究人力

- **C1** 单名研究员的有效人力 = `linearResidentProductivity`（权重：健康 15、精神 20、力量 5、智力 60；属性 0 时 0.5、100 时 1.5；满熟练度 +0.5；下限 0.5、上限 2.5）× (1 + 0.3 × 装饰评分)。装饰评分用住宅的 `calculateDecorationRating` 公式与参数，标签换成研究所装饰。
- **C2** 研究人力按教育等级分为 6 级，记为城镇每日服务资源 `RESEARCH_LABOR`（level = 教育等级 0–5），与运力一样每天清零重建、不占仓库容量。`resetAllServices` 改为清除每一级。
- **C3** 一项需求要求教育等级 ≥ E 时，可以使用等级 ≥ E 的人力；分配时先用满足条件的最低等级，把高等级留给更难的需求。
- **C4** 研究员每天记录学习活动 1.0、体力活动 0.1（狩猎为 0.25 / 1.0），并获得研究所熟练度。研究所熟练度从 0 开始，不加入招募时的随机生成，以免改变已有的随机序列。
- **C5** 研究所界面与镇长印章显示各级人力的向下取整值；需求进度显示一位小数。

### D. 人力协作需求

计算与实验需求增加可选字段 `labor`：

```json
"labor": {
  "required": true,
  "workers": 2,
  "days": 3,
  "education": 2,
  "items": [{"item": "minecraft:paper", "count": 2}, {"item": "minecraft:ink_sac", "count": 1}]
}
```

- **D1** 省略 `labor` 时与现状相同，居民不参与。
- **D2** 进度单位是「人·天」：每天最多投入 `workers` 名标准研究员，完成一份协作需要 `workers × days` 人·天。人力不足时按实际投入累计，不会因差一点而整天作废。
- **D3** 物品按投入比例从仓库扣除：投入 `w` 人时扣 `count × w / workers`，可为小数（仓库本来就以小数保存）。物品不够时按最短缺的一项缩减当天投入；一项都没有时当天不投入。
- **D4** `required: false`：每完成一份协作，等于玩家完成一局演算或一次实验。玩家自己完成的次数加上协作份数达到 `count` 即满足。
- **D5** `required: true`：玩家的次数与协作都要完成。此时 `count` 可以为 0，表示纯人力需求。
- **D6** 绘图台与实验台只在「玩家的部分」还需要时接受这项需求；协作完成的次数同样让它们提前结束。
- **D7** 每天先收集所有进行中启动的需求，按教育等级从高到低分配（同级按启动时间、需求顺序）。B5 的规则不变：进度每次按当前定义重新判断。
- **D8** 校验：只有计算与实验可以写 `labor`；`workers` 1–32，`days` 1–100，`education` 0–5，`items` 至多 4 项，每项写 `item` 或 `tag`、`count` 1–64。

### E. 联想

- **E1** 分配后剩余、教育等级 ≥ 2 的人力为联想人力 `L`。当天成功联想的概率为 `1 − e^(−0.15 L)`（1 人约 14 %，5 人约 53 %）。
- **E2** 成功时先找新想法：在当前有效档案中寻找能完整绑定、而输出想法既不在档案也不在来件中的联络规则，随机选一条，按正常联络流程记录发现并产出想法（学不了时进来件）。来源记为 `institute:link:<规则>`，札记显示「研究所的同伴想到的」。
- **E3** 没有可产出的新想法时，以一项随机的有效知识为主题，按现有暗示规则给一条暗示；仍没有就什么也不发生。
- **E4** 候选搜索加入步数上限（每条规则 20000 步，与证据搜索相同），解决审阅 P2-7。
- **E5** 每天的人力分配、空闲人力与联想结果保存为队伍的「研究所日报」（`KnowledgeState.labor_report`），进入快照，供研究所界面和研究页显示。

### F. 结算顺序

- **F1** 研究所像其他工作建筑一样在 `buildingsWork` 中工作，把各级人力加入 `RESEARCH_LABOR`。
- **F2** 全部建筑工作并完成运力日报后，新增一步 `TownResearchSettlement`：读取各级人力，调用知识系统分配人力、扣除仓库物品、联想，并写入日报。异常只记录日志，不中断城镇日。
- **F3** 镇里没有研究所人力时跳过这一步，不写空日报。

### G. 功能三：探索派遣型证据收集

需求只给出了名称。证据要求的是真实观察；居民去远处观察，就要读取未加载的区块或实体，而城镇代码明确只读已加载区块。具体玩法（派往哪里、带回什么、怎样避免伪造观察）需要项目所有者决定，**本计划不实现**，另立计划。届时可直接复用本计划的人力池、教育等级与日报；居民产生的观察使用设计文档已规定的来源（`town`，组织为队伍，实际观察者为居民）。

## 步骤

1. 知识侧：`KnowledgeCapacity`；`labor` 定义与校验；`ResearchHistory.labor`；进度判断、`workLabor`、联想与日报；候选步数上限；快照字段。
2. 知识界面：研究页的协作进度与说明；档案、来件容量；实验台只列出玩家还需要做的实验。
3. 城镇侧：配置、标签、虚拟资源、建筑与方块实体、菜单与界面、注册、结算、容量扩展登记、镇长印章。
4. 资源：方块状态、模型、贴图、掉落、中英文、配套包配方。
5. 示例数据包加上人力协作需求；文档；GameTest；日记。

## 验证

- GameTest：研究所真实房间统计书架、绘图台、装饰与照明；变暗、变冷时不可工作；容量扩展让知识上限变化；codec 与网络往返包含第七种建筑；人力分配（必须/可选、教育等级、物品比例）；联想产出新想法；智力活动记录。
- 知识侧 GameTest：`labor` 校验、进度判断、启动记录往返。
- 编译 `compileJava`、`compileGameTestJava`；运行相关 GameTest 命名空间。

## 文档影响

- `docs/town/town-model.md`：新增研究所一节（公式、参数、结算顺序）。
- `docs/town/building-data.md`、`implementation-reference.md`：七类建筑、协议版本。
- `docs/knowledge/definitions.md`：`labor`。
- `docs/knowledge/state-and-api.md`：容量扩展、人力进度、`workLabor`、联想、日报。
- `docs/knowledge/workbench.md`、`playthrough.md`：研究页与容量显示、示例路线。
- `diary/`：完成后追加。

## Outcome

2026-11-09 按上述决定实现，记录见 [diary](../diary/2026-11-09_03-05-00_town-research-institute.md)。现行行为见 [town-model.md §24](../docs/town/town-model.md)、[definitions.md](../docs/knowledge/definitions.md) 与 [state-and-api.md](../docs/knowledge/state-and-api.md#研究所人力)。

与方案不同或方案未写明的几处：

- **知识侧 API 名称**：人力分配为 `KnowledgeService.workLabor(pool, supplies)`，联想为 `instituteAssociation(random, day)`，日报为 `recordLaborReport(report)`。城镇侧只通过 `ResearchLaborPool` 与 `ResearchSupplies` 两个接口交给知识系统。
- **物品共同扣除失败**：多项物品的标签可能重叠，此时各自有货、合起来却不够。这种情况下当天投入减半重试，至多 4 次。
- **容量同步**：研究所提供的容量变化时，方块实体标记队伍知识同步，札记的上限随之更新。
- **未开始的项目**：研究页在正文末尾提示其中有可以交给研究所的工作。
- **示例包**：`kiln` 新增只靠人力的需求「整理烧窑记录」。`basic_switch` 与 `pressure_control` 允许研究所代劳。
- **配套包**：在 `kubejs/server_scripts/src/recipes/shaped/new.js` 中加入核心配方，图案与其他城镇核心相同（`BAB/ACA/BAB`），中心为书架。

