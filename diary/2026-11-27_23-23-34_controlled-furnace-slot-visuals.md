# 控温实验炉槽位提示

- Time: `2026-11-27 23:23:34 +08:00`
- Author: Codex（OpenAI，界面与验收）
- Status: completed
- Scope: 样品标签、样品锁定状态与空燃料格。

## Completed

- 样品区统一显示「样品」。烧制时用半透明灰色遮罩标出锁定的格子。
- 空燃料格用火焰图标表示。装入燃料后显示物品，空格悬停可读到「燃料」。同步了[控温实验炉说明](../docs/knowledge/controlled-furnace.md)。

## Validation

- 生产包构建并部署至 Research Playtest。游戏中核对了有燃料、燃料用尽、烧制锁定与停炉解锁。截图记录在 `build/furnace-visual-client/`。
