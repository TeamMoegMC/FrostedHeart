# 联络难度玩家文案

- Time: `2026-11-17 23:07:51 +08:00`
- Author: `Codex / OpenAI；主代理`
- Status: `completed`
- Scope: FH知识札记联络候选页及共享难度悬停提示。

## Completed

- `gui.frostedresearch.journal.link.difficulty` 中文从「连连看难度」改为「联络难度」，英文从 `puzzle difficulty` 改为 `linking difficulty`；保留缺少辅助知识数量和难度等级两个占位符。
- 核对 `KnowledgeLayer` 候选页和 `DeskProgressPanel` 悬停共用此键；开发调试入口及其它演算文本不在本次改动范围。
- 文档影响：更新[绘图台操作](../docs/knowledge/workbench.md)的玩家显示名称；玩法、难度计算和存档均未改变。

## Decisions

- 玩家看到操作本身的名称，开发文档仍可使用小游戏的实现称呼。

## Validation

- 中英文语言JSON可解析；共享语言键均保留两个 `%s`；定向差异核对。
- 纯文本修改，不运行构建或GameTest。

## Remaining

- 尚未部署或进行游戏内视觉验收。
