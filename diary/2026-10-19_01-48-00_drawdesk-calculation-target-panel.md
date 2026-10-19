# 绘图台：演算时左上角显示对应的知识研究

- Time: `2026-10-19 01:48:00 -0500`
- Author: `Claude; Anthropic Opus 5.5; 实现`
- Status: `completed`
- Scope: `gui/drawdesk/DeskProgressPanel`、`gui/drawdesk/DrawDeskLayer`、`blocks/DrawingDeskTileEntity`、语言文件、研究与知识文档

## Completed

- 项目所有者测试 R1 时指出：演算期间，绘图台左上角仍显示旧研究，看不出这局纸牌局计入哪项研究。
- 左上角改用新增的 `DeskProgressPanel`，取代原来的 `ResearchProgressPanel`。
- 桌子有演算目标时，面板显示：
  - 标题「演算中的研究」；
  - 纸张图标；
  - 项目标题，最多两行；
  - 进度条和「n/m 局 · 难度 k」；
  - 悬停时显示项目标题、需求标题和说明。
- 桌子没有演算目标时，面板内嵌原来的 `ResearchProgressPanel`，旧研究的显示不变。
- 数据取自客户端知识快照 `KnowledgeJournalData.runs`；只有演算目标或快照序号变化时才重新解析和重建。
- `DrawingDeskTileEntity` 新增访问器 `calculationRequirement()`。
- 新增中英文键 `gui.frostedresearch.draw_desk.calculation_for` 与 `gui.frostedresearch.draw_desk.calculation_progress`。

## Decisions

- 用外层面板二选一，而不是继承旧面板。旧面板的 `render` 总是先画子元素、再画旧研究，子类无法跳过后一步。
- 快照里还没有这次启动时（例如同步尚未到达），面板暂时显示旧研究，快照到达后自动切换。
- 悬停区域只覆盖按钮上方的 48 像素，不和下方的科技树、暂停、札记按钮的提示重叠。
- Documentation impact：更新了 [研究页](../docs/knowledge/workbench.md#研究页) 和 [纸牌局](../docs/research/gameplay-and-integrations.md#theory-card-game)。

## Validation

- `./gradlew compileJava` 通过。
- 单元测试与 GameTest 的结果与 R2 同一轮，见 [R2 记录](2026-10-19_01-42-23_knowledge-experiment-table.md#validation)。这个面板没有自动化测试。
- 还没有在游戏内看过。

## Remaining

- 游戏内确认布局：英文「1/2 · level 1」的宽度，以及两行项目标题与进度条不重叠。
