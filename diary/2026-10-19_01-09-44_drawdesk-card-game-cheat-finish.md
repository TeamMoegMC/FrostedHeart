# 绘图台纸牌局：作弊模式下点击中心格直接完成

- Time: `2026-10-19 01:09:44 -0500`
- Author: `Claude; Anthropic Opus 5.5; 实现`
- Status: `completed`
- Scope: `gui/drawdesk/game/ResearchGame`、`gui/drawdesk/game/ClientResearchGame`、`gui/drawdesk/CardButton`、`blocks/DrawingDeskTileEntity`、语言文件、研究与知识文档

## Completed

- 应项目所有者的测试需求加了一条快捷方式。权限等级 2 以上的玩家（单人开启作弊，或服务器管理员）在一局进行中点击棋盘中心的最终论文格子 `ResearchGame.FINAL_CARD = (4, 4)`，这一局立即获胜。
- 获胜照常结算：有知识研究的演算目标时计入 `recordCalculation`，否则完成旧研究的纸牌局线索。
- 服务端：`ResearchGame#forceWin` 清空正在进行的棋盘并判定获胜，没有进行中的棋局时返回 `false`。`DrawingDeskTileEntity#tryCombine` 在单格点击落在中心、且玩家有权限时调用它，不消耗笔耐久。
- 客户端：`CardButton` 对有权限的玩家在中心格显示手形光标和提示「作弊模式：点击直接完成这一局」；`ClientResearchGame#finishForTesting` 发送普通的单格操作 `1`。
- 新增中英文键 `gui.frostedresearch.draw_desk.cheat_finish`。

## Decisions

- 沿用现有的单格操作包，不新增操作码。服务端再检查一次权限，客户端只决定是否显示提示和发包。
- 权限以 `hasPermissions(2)` 为准，与原版作弊指令的门槛一致。
- 对旧研究和知识研究的纸牌局都生效，因为两者共用同一张棋盘，测试时都能用上。
- Documentation impact：更新了 [纸牌局](../docs/research/gameplay-and-integrations.md#theory-card-game)、[研究页](../docs/knowledge/workbench.md) 和 [检查路线](../docs/knowledge/playthrough.md)。

## Validation

- 新增 `ResearchGameTest`：没有开局时不生效；开局后中心是最终论文；强制获胜后棋盘清空且标记为完成；已完成的棋局不会再次获胜。
- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`：BUILD SUCCESSFUL，130 个测试，0 失败。
- 没有在游戏内点过，需要重启开发客户端才会生效。

## Remaining

- 游戏内确认：有权限时中心格显示手形光标和提示，点击后这一局完成，研究页的演算进度加一。
