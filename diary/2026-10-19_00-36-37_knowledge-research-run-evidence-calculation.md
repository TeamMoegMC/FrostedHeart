# 知识研究项目执行 R1：证据、演算与研究页

- Time: `2026-10-19 00:36:37 -0500`
- Author: `Claude; Anthropic Opus 5.5; 实现`
- Status: `completed`
- Scope: `frostedresearch/knowledge`（定义、状态、服务、视图、网络、札记界面）、`blocks/DrawingDeskTileEntity`、`gui/drawdesk/MainGamePanel`、示例包、知识与研究文档

## Completed

按 [研究项目执行计划](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md) 的 R1 实现计算与证据两类需求。

- **定义**
  - 新增 `ProjectRequirement`（`evidence` / `calculation`）。`ProjectDefinition` 增加 `body` 与 `requirements`，保留原两参构造。
  - 证据复用联络规则的槽位与跨条件；`LinkRule` 抽出 `SlotPattern` 与共享的 `validateBindings`。`distinct` 组在 `pattern()` 中展开为两两 `ne`。
  - 校验：需求 `id` 唯一；证据为 1–8 个观察槽位、最多 10 条跨条件、必须有作者标题；计算 `level` 0–3、`count` 1–16。新字段用严格可选字段，作者写错值会报错，不会静默回落到默认值。
  - `KnowledgeDefinitions.Snapshot` 的观察用途由「含观察槽位的联络规则」扩展为「再加所有证据需求」（D5），重复判定同样计入。
- **状态**
  - `ResearchHistory` 增加 `abandoned`、`calculations`（每局胜利的难度）和 `evidence`（每槽观察副本），以及 `open()`、`withCalculation`、`withEvidence`、`completedWith`、`abandon`。
  - 空进度不写入；进度存在但解不出时用严格字段，使整条记录交给 `KnowledgeState` 隔离。
- **服务**（`KnowledgeService`）
  - 开始：`researchStartStatus`、`startResearch` 实现 B2，每队每项目一次进行中的启动，完成后只有缺成果时才能重启；`openRun` 查询进行中的启动。
  - 需求判断：`requirements`、`requirementMet`、`requirementsMet` 按当前定义判断（B5）。
  - 证据：`submitEvidence` 有两个重载，自动挑选和按给定记录精确提交；来源为档案和来件（D3），保存副本（D4）。
  - 演算：`calculationLevel`、`recordCalculation`。
  - 结束：`finishResearch` 在需求满足时才完成；`abandonResearch` 清空进度、保留记录（B4）。`completeResearch` 保持无条件，供研究系统与开发指令使用，但不再接受已放弃的启动；完成时保留进度。新增状态 `REQUIREMENTS_UNMET`。
  - `LinkMatcher` 增加 `bindExact`，以及带步数预算的 `find`（`EVIDENCE_SEARCH_STEPS = 20000`）；原有 `match` / `candidates` 行为不变。
- **绘图台**
  - `DrawingDeskTileEntity` 增加演算目标：`calculation_run`、`calculation_requirement`、`calculation_level`，随方块实体 NBT 同步。
  - 有目标时，操作 `0` 按目标难度开局，胜利计入 `recordCalculation`；需求满足或启动结束后自动清除目标。
  - `MainGamePanel` 在有目标时不再显示「没有线索」；客户端的纸、笔检查使用目标难度。
- **网络与视图**
  - `KnowledgeWorkbenchActionPacket` 增加 `START_RESEARCH`、`SUBMIT_EVIDENCE`、`CALCULATE`、`STOP_CALCULATION`、`COMPLETE_RESEARCH`、`ABANDON_RESEARCH`，以及一个 `requirement` 字符串字段。
  - `KnowledgeView` 中：
    - 项目带 `body`、需求摘要、起源想法、`start_status`、`open_run`；
    - 启动记录带 `status`；进行中的启动带每项进度；
    - 未满足的证据还带每槽可用记录数和 `ready`（D6），槽位条件本身不进入快照。
- **札记界面**
  - 第五个书签「研究」：项目列表、需求清单、‹ › 切换选中需求，以及开始、整理证据、演算、继续/停止演算、完成、放弃确认（G1）。
  - 项目浮纸新增「查看研究」。已放弃的启动只在历史视图出现，也不会把待研究的虚线改成实线。
  - 书签换行阈值改为 `KnowledgeLayout.WIDE = 460`。
- **其他**
  - 开发指令 `/knowledge research complete` 沿用进行中的启动；无法开始时提示 `project_unavailable`。
  - 中英文各新增 57 个翻译键，两种语言的键集合一致。
  - 示例包中 `material_record`（原木证据 + 难度 0 演算）、`basic_switch`（两局难度 1）、`lattice`（两种生物群系的原木，`distinct`）带上需求，同步到 `run/saves/knowledge-test` 中的示例包副本（只覆盖这三个项目文件）。

## Decisions

- 证据由服务端自动挑选，不再经「放到桌面」逐份选择。D4 让提交不消耗观察，选哪几份不影响结果，因此省去 8 个槽位的选择界面。搜索有步数上限，超出就算没找到。显式提交的重载保留给其他调用方。
- `distinct` 放在证据需求自己的字段里，没有作为 `conditions` 的新 `kind`，以免改动联络规则跨条件的编码。
- `completeResearch` 不加需求判断，玩家路径另设 `finishResearch`。现有研究接入接口与开发指令的语义不变；没有需求的项目对玩家永远不可完成，仍可由开发指令完成。
- 演算目标在需求满足前一直有效，需要再赢一局时直接按游戏区开局键即可；`STOP_CALCULATION` 让玩家把纸牌局交还给旧研究。
- `KnowledgeScreen.current()` 在没有 `Minecraft` 实例时返回空，使快照能在单元测试中安装；`KnowledgeLayer.openResearch` 改为包内可见，供脉络浮纸与测试使用。
- Documentation impact：更新了以下文档。
  - [definitions.md](../docs/knowledge/definitions.md)：项目需求格式与观察用途。
  - [state-and-api.md](../docs/knowledge/state-and-api.md)：研究启动记录与开发指令。
  - [workbench.md](../docs/knowledge/workbench.md)：研究页。
  - [playthrough.md](../docs/knowledge/playthrough.md)：示例包路线。
  - [示例包 README](../docs/knowledge/example-datapack/README.md)。
  - [gameplay-and-integrations.md](../docs/research/gameplay-and-integrations.md)：纸牌局的演算目标。

  各文档的 `Last verified` 未改，因为游戏内视觉与多人流程仍未验收。

## Validation

- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，36 个类、129 个测试，0 失败（合并后的基线是 118 个，本轮新增 11 个）。修正演算回复的提示计时后，`com.teammoeg.frostedresearch.knowledge.*` 的 66 个测试重跑全部通过。
- `./gradlew runGameTestServer`（含 FTB Teams）：BUILD SUCCESSFUL in 1m 21s，`All 106 required tests passed :)`；日志中没有 `Knowledge definitions:` 重载错误。未运行 `-PwithoutFtb -PwithoutJei` 一组。
- 新增测试覆盖了以下内容：
  - `ResearchRunTest`（7 个）：单启动与重启条件、证据来源与副本、演算难度与上限、按当前定义重新判断、完成门槛与无需求项目、放弃与读写往返、快照不含槽位条件。
  - `KnowledgeDefinitionsTest`：需求形状校验（8 类错误）；证据槽计入用途并保留重复槽位的独立记录；示例包加载并带上需求。
  - `LinkMatcherTest`：池搜索、步数上限与精确绑定。
  - `KnowledgeLayoutTest`：研究页在 296/456/660 宽度下，进行中与未开始两种状态都不越界。
- 未在真实客户端里点过研究页，也没有在游戏中走完示例路线；纸牌局目标只经过代码路径与单元测试验证。

## Remaining

- R2（实验需求、实验台、空间检查）未开始。
- 需要在游戏内走一遍 [检查路线](../docs/knowledge/playthrough.md)：材料比较的证据与演算、拨杆机关的两局难度 1、木条支撑的两处群系。多人下由不同成员推进同一项目也需要检查。
- 证据自动挑选在条件很苛刻、候选很多时可能因步数上限而找不到实际存在的组合；目前没有手动选择的后备界面。
- 居民执行需求、研究所与 `candidateLinks` 的回溯上界（审阅 P2-7）留给研究所计划。
