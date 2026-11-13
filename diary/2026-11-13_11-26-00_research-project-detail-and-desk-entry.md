# 研究整页详情、绘图台默认入口与鼓风口模型

- Time: `2026-11-13 11:26:00 +08:00`
- Author: `Codex / OpenAI；实现与核对`
- Status: `completed`（实现与构建完成；游戏内视觉待验）
- Scope: `KnowledgeLayer`、`DrawDeskScreen`、`FRContents.PROTOTYPE_TUYERE`、原型模型与中英文按钮。

## Completed

- 研究列表右侧只显示状态、当前阶段和成果概览；「查看研究详情」打开独立整页，隐藏列表，完整显示原有正文、知识条件、任务和操作。沿用同一容器与现有滚动文本，未改变数据包格式和服务端研究规则。
- 详情提供返回列表按钮和 Esc；风道内 Esc 先返回详情。脉络中的查看研究直接进入项目详情。
- 绘图台初次打开默认知识札记；有 open run 时选中研究栏及一份进行中记录。首个刷新快照补充选择，手动导航后停止自动选页；窗口重新初始化不重新打开默认页。
- 鼓风口原型外轮廓由 X/Z 的 1–15 扩展到 0–16，保留风孔；注册加 noOcclusion，避免非实心模型错误裁掉相邻方块表面。未改变碰撞和试吹关联。
- 更新 workbench、player-guide、知识 README 和 stages-and-airflow，明确本次视觉验收尚未进行。

## Validation

- 中英文 JSON、按钮翻译键及模型完整方块外边界检查通过；git diff --check 通过。
- Java17 下 compileJava、jar、reobfJar 最终通过（3m56s）；日志位于忽略目录 build/research-ui-20261113-final-build.log。另用 javap 核对最终类包含搜索时停止自动选页的补充逻辑，避免将编译期间修改遗漏在交付物中。

## Remaining

- 游戏内检查窄/宽窗口详情阅读、默认页与已有研究选择，以及原型在地面/墙侧的邻面显示。本轮未部署至 Prism，未操作用户正在运行的游戏。
