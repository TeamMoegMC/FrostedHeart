# 尺规绘图与四叶泵验收研究

- Time: 2026-11-26 18:50:17 +08:00
- Author: Codex（OpenAI，开发与验收）
- Status: completed
- Scope: Frosted Research 绘图台分阶段小游戏、整合包可选测试研究。

## Completed

阶段用图纸 ID 加载尺寸、基准、线段、圆弧和可选提示。四叶泵从空白笔迹开始，整圆和跨径直线可覆盖桨叶。绘图台保存笔迹并将成功回写当前运行。数据格式及玩家操作已加入[内容作者指南](../docs/knowledge/authoring-guide.md)。

## Decisions

按用户调整，整合包通过独立的 `testpacks/drafting` 提供测试项目 `twr_researches:drafting_test`，提交 `6694ef639`。正式流体运输沿用旧研究。试玩实例装入测试项目的三份知识定义，供继续体验。Minecraft 界面使用固定画布、尺寸摘要、滚轮调值与点选擦除，量角器按指定角度从基线转出等长边。

## Validation

- `jar`、生产研究目录校验（80 项）及 Forge GameTest `frostedresearch_drafting` 通过。测试独立构图，核对 0.5 mm 缺口和整圆的局部重合。
- 指定 Prism 实例的两个独立测试世界中，测试程序初始化构想、研究和绘图台，然后通过 Minecraft 屏幕输入进入研究任务。第一次逐笔完成 R48、R12、R24、四个 R20 及两条跨径线并结题。随后合批绘图渲染，第二次在最终构建上画一笔，退出并重开，确认一笔保留，启动、取消、重新启动 16 秒代绘，通过后返回、结题并核对服务器运行状态。
- [实机截图和结果](../build/drafting-client/)；试玩 JAR 与本地生产构建内容一致，验收专用模组已移出实例。

## Remaining

本次实机输入由脚本驱动。旧流体运输定义与试玩实例逐字节一致，未使用旧玩家存档回放。
