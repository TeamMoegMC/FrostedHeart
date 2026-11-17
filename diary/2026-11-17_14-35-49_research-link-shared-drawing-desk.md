# 想法联络沿用绘图台通用连连看页面

- Time: `2026-11-17 14:35:49 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH绘图台CUI、联络页面往返与客户端验收助手；无TWR源码改动。

## Completed

- 按用户指定截图，将想法联络移到原387×203绘图台页面，直接复用`MainGamePanel`、原格子背景、CardButton、CardStatPanel、OrderWidget、HelpPanel及重排按钮，删除独立`LinkGamePanel`和札记LINK_GAME显示模式。
- `DrawDeskScreen.receiveLinkReply`接收联络盘面并切换原桌面；`ClientResearchGame.bind`在旧研究盘面与临时联络盘面之间切换显示和操作通道，旧研究盘面及演算目标不被覆盖。原有4/5/6联络操作仍独立结算，费用/难度/产出规则未改。
- 左上`DeskProgressPanel`显示推导中的想法、标题、本局难度，悬停显示缺项和说明。联络时纸笔/检视工具槽与旧研究切换操作停用，玩家背包仍可见可用。
- 通关立即回原札记；Esc或「知识札记」取消并返回，保留原页面与桌面选择。帮助打开时Esc优先关闭帮助。隐藏的札记继续收到知识快照。
- 更新[绘图台操作](../docs/knowledge/workbench.md)、[开始研究](../docs/knowledge/player-guide.md)、三个正式路线、[旧研究游戏说明](../docs/research/gameplay-and-integrations.md)和[测试入口](../src/gametest/README.md)。历史diary不改写；无build.gradle、gradle.properties或design/改动。

## Decisions

- 统一的是实际页面和控件，不再为联络单独排版棋盘。显示上下文由桌面控制，持久旧盘面和临时联络盘面继续各自保存、结算。
- 普通绘图台操作0–3与联络操作4–6保持区别，防止异步回复和迟到输入改动其它棋盘。

## Validation

- Java17 `runGameTestServer jar reobfJar m3cValidationJar validateResearchCatalog -PgameTestNamespaces=frostedresearch_m2,frostedresearch_matching -PgameTestDirectory=build/research-shared-desk-20261117 -PthermalTestHeap=4G -PthermalTestProcessors=4 -PresearchCatalogDir=../TheWinterRescue/config/fhresearches --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，4m53s；15项required tests及2048局回放全部通过，81个旧定义有效。日志`build/research-shared-desk-20261117.log`。
- 首次客户端运行停在夹具准备：助手对继承的MC方法`DrawingDeskTileEntity.setChanged()`未正确重映射。改为显式以`BlockEntity`接收者调用，仅重建临时助手，34s；日志`build/research-shared-desk-helper-20261117.log`。失败证据及完整客户端日志保存在`build/link-client-first-helper-attempt/`，不算游戏流程通过。
- 最终Playtest新flat世界`result.json=passed/phase16`：确认387×203原桌面、同一个MainGamePanel和81个CardButton；2级联络→Esc取消→重开→夹具补齐辅助→原重排按钮降至0级→客户端独立搜索、真实Screen点击和普通C2S配对→非作弊中央点击获得想法→回到同一札记。
- 夹具预先在绘图台保留一盘未完成的旧初级研究；最终NBT与原盘面完全一致，三个工具槽仍为空。玩家无作弊权限，未绕过通关记账或知识产出。
- 人工检查2级/0级截图：原木框、左背包、格子背景、牌面、右侧统计/顺序、帮助/重排和推导标题/难度均完整可见。证据：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-link-shared-desk-20261117/build/link-client/`。
- 正式JAR已同步Research Playtest并逐字节核对；备份`build/prism-backups/2026-11-17_14-21-53-shared-link-desk/`。生产包不含独立LinkGamePanel或测试助手类。客户端正常退出，临时helper与marker已移除，原缩放/语言/帧率/失焦暂停设置恢复。
- 仅选择性提交本轮FH文件，不推送；FH原.gitignore、其它未跟踪文件和TWR原暂存config/.gitignore及.workbuddy保留。

## Remaining

- 验收为真实Screen事件和C2S、夹具知识/旧盘面与新世界；不代表物理输入或完整生存流程。联络最高7数字的既有范围不变。
