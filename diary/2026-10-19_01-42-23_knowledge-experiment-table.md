# 知识研究项目执行 R2：实验需求与实验台

- Time: `2026-10-19 01:42:23 -0500`
- Author: `Claude; Anthropic Opus 5.5; 实现`
- Status: `completed`
- Scope: `knowledge/definition/ProjectRequirement`、`knowledge/state/ResearchHistory`、`KnowledgeService`、`KnowledgeView`、`knowledge/experiment`、`knowledge/client/experiment`、`KnowledgeLayer`、`KnowledgeJournalData`、`FRContents` / `FRClient` / `FRNetwork` 注册、方块资源与语言文件、示例包、知识与研究文档

## Completed

按 [研究项目执行计划](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md) 的 R2 实现实验需求。

- **定义**
  - 新增需求类型 `experiment`，字段为 `title`、`count`、`duration`（20–72000 tick）、`materials` 和 `space`。
  - `materials` 至多 3 项，每项是物品或物品标签加数量。
  - `space` 包括 `enclosed`、`min_volume` / `max_volume`、`min_temperature` / `max_temperature`，以及至多 8 项 `blocks`。
  - 其他类型的需求写这三个字段时报告错误。
- **状态**
  - 启动记录新增 `experiments`，按需求 `id` 保存完成次数，随存档往返，放弃研究时清空。
  - `KnowledgeService` 新增 `experimentRequirement` 与 `recordExperiment`；`requirementMet` 按完成次数判断。
  - 研究快照带上每次的 `duration` 和完成次数。
- **实验台**
  - 新增方块 `frostedresearch:experiment_table`，以及它的方块实体、菜单、操作包与界面。
  - 三格材料槽，四个动作：选用、检查、开始、停止。
  - 首次使用时归属使用者所在的队伍。
- **条件**
  - 房间：`ExperimentConditions` 从实验台上方的空气开始扫描（上方被占时依次取北、东、南、西侧），用 `ConfinedSpaceScanner`，上限 4096 格空气，统计体积和与房间空气相邻的方块。
  - 温度：读取实验台位置的 `WorldTemperature.block`。
  - 材料：按列出的顺序在槽位中贪心分配；开始时要么全部扣除，要么一件不扣。
- **运行**
  - 每 20 tick 检查一次：温度每次都读，房间每五次扫描一次。
  - 条件不满足时暂停，保留进度。
  - 运行满时长后调用 `recordExperiment`；需求满足后实验台清空选择。
  - 闲置时，槽位变动会在下一次检查时刷新材料行，不重新扫描房间。
- **研究页**：实验需求显示「n/m 次」；操作区是不可点击的「在实验台进行」和一段说明。
- **示例包**
  - `pressure_control` 新增 `load_test`：在木板小屋里，空气 4–256 格，至少 8 块木板，温度不高于 40°C；投入 4 块木板和 1 块石头，运行 400 tick。
  - 已复制到 `knowledge-test` 和 `knowledge-test-world` 两个开发存档。

## Decisions

- 计划里的 E5 操作扩展点没有写成代码，原因记在计划的 Outcome 里。
- 停止实验或换选其他实验都不退还材料，与「开始时一次性消耗」一致。
- 实验台的归属沿用 `IOwnerTile`，与机械计算器相同。操作包只有在三个条件同时成立时才执行：玩家打开的正是这张实验台，距离在 8 格以内，且属于同一队伍。
- 方块模型暂用原版纹理（制箭台的侧面、制图台的顶面），以后可以换成专门的纹理。
- Documentation impact：更新了以下文档：
  - [研究需求](../docs/knowledge/definitions.md)
  - [状态与 API](../docs/knowledge/state-and-api.md)
  - [研究页与实验台](../docs/knowledge/workbench.md#实验台)
  - [检查路线](../docs/knowledge/playthrough.md)
  - [示例包说明](../docs/knowledge/example-datapack/README.md)
  - [研究玩法](../docs/research/gameplay-and-integrations.md#knowledge-experiment-table)
  - [计划 Outcome](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md#outcome)

## Validation

- `KnowledgeDefinitionsTest.experimentRequirementsValidateMaterialsSpaceAndDuration`
  - 覆盖两个合法写法：带空间的实验和不要求空间的实验。
  - 覆盖八种错误：缺标题、时长过短、材料过多、未知物品、非封闭空间却写了体积、温度上下限颠倒、未知方块、非实验需求写了时长。
  - 示例包加载测试同时断言了 `pressure_control.load_test`。
- `ResearchRunTest.experimentsCountFinishedRunsAndRoundTrip`：次数累计；满足后不再接受新的完成；存档往返；快照进度；放弃研究时清空。
- `ExperimentConditionsTest`：材料按顺序分配并整体扣除；同一堆物品不能同时满足两项材料；检查报告的 NBT 往返。
- GameTest `ExperimentSpaceGameTests.enclosedRoomCountsAirAndWallsAndAnOpeningFailsIt`：一格大小的房间统计到 1 格空气和 5 块木板；拆掉顶部后判为不封闭。
- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，134 个测试，0 失败。
- `./gradlew runGameTestServer`：All 107 required tests passed。
- 还没有在游戏内运行过实验台。

## Remaining

- 游戏内验收：合成与放置、界面布局、材料行刷新、暂停与恢复、完成后研究页的计数。
- 多人：同队不同成员能操作同一张实验台，其他队伍打不开。
- 专门的方块纹理。
