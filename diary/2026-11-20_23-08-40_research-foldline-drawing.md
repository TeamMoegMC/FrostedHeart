# 折线图纸校核

- Time: `2026-11-20 23:08:40 +08:00`
- Author: Codex / OpenAI，研究玩法实现与验证
- Status: completed
- Scope: Frosted Research 绘图台、阶段任务与双语说明，Prism 研究试玩实例

## Completed

- 研究阶段可用 `type=minigame`、`game=frostedresearch:foldline` 和 `level=0..2` 指定折线校核。[作者指南](../docs/knowledge/authoring-guide.md#折线图纸校核)和[阶段文档](../docs/knowledge/stages-and-airflow.md)已更新。随模组提供隐藏试玩项目 `frostedresearch:foldline_debug`。
- 图纸按绘图台、本次研究和任务保存。研究要求选择游戏类型与等级，服务端检查当前底稿、出口与镜片限额，玩家定稿后记录一次通关。

## Decisions

- 将网页版生成器筛选的 96 份基础图随模组打包。每档 32 份，开题时随机排列轴线间距、方向与转置，避免在服务器刻内穷举摆法。所有有效解都能定稿。
- 正式项目由内容作者自行指定。此轮正式整合包仓库无需修改。试玩实例保留本轮模组 JAR。

## Validation

- 独立穷举 96 份基础图，所有预算内解都要求 A 改道。Forge 定向测试通过 15/15，覆盖三档共 3,072 次随机生成、实际绘图台重读 NBT、过期操作、定稿、项目完成以及阶段/风道回归。日志为 `build/foldline-verification.log`。
- Prism Research Playtest 隔离实例新世界通过客户端双线题，真实界面点击调整镜片并定稿，返回原研究页后队伍已取得任务进度。截图与结果在 `build/foldline-client/`。首次测试辅助 JAR 缺少验收类，重新打包、确认类已包含后运行通过。临时辅助模组已移除。

## Remaining

- 客户端操作验收覆盖双线档，另外两档有服务端验收。多人同桌并发手感与完整生存流程未实玩。
