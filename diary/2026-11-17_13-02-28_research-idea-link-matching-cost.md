# 想法联络改用连连看代价

- Time: `2026-11-17 13:02:28 +08:00`
- Author: `Codex / OpenAI；主代理实现与验证`
- Status: `completed`
- Scope: FH想法联络会话、札记合成页、绘图台操作包、联络测试和Playtest生产JAR。

## Completed

- 删除联络等待计时、进度字段和三个时长配置。候选页先显示缺失辅助项数与实际难度；点击开始后在札记内打开连连看，通关立即走`KnowledgeService.link`正常学习/来件流程。
- 缺失辅助来自LinkRule.optional_knowledge，省略才继承想法acquisition.optional，显式空列表为0项；只读有效档案。每缺一项增加一个顺序数字，齐全时0数字但仍需配对图案及点结束牌。现有棋盘支持0–7数字，超过7项缺失时采用7数字盘面。
- 难度开局固定，重新排列按当前知识重算。会话按玩家临时保存，不写NBT；取消、关闭容器、离线、换队、必要前置失效或队友已取得输出时结束本局。已取得想法不能再次开始游戏；内部联络API仍能保留其它历史路径。
- `LinkGamePanel`复用旧牌面和CardButton；复用`FHDrawingDeskOperationPacket`类型并新增4/5/6为联络重排/单格/配对，0–3保持原用途。明确区分操作，取消后的迟到联络点击不作用于旧棋盘。无纸笔消耗或研究演算计数。
- 同步更新知识API、定义、界面、玩家指南、三个正式研究路线、内容模板/作者指南、旧研究操作包说明及GameTest README。历史diary保留；旧客户端记录明确标为当时的等待版本。无TWR源码、build.gradle、gradle.properties或design/修改。

## Decisions

- 玩家小游戏代价放在`KnowledgeLinkSessions`；通用`KnowledgeService.link`继续承担正常产出与历史关系职责，供其它系统使用。
- 原有level ID和已有研究存档不变，由GenerateInfo把实际数字数映射为历史编号。只删除联络计时，不改变行为触发概率、开始研究前置或阶段规则。

## Validation

- 第一轮`runGameTestServer`（frostedresearch_m2,frostedresearch_matching）15项全部通过；日志`build/research-link-game-tests-20261117.log`，3m43s。
- 操作包改为明确的4/5/6联络操作后，最终Java17 `runGameTestServer jar reobfJar m3cValidationJar -PgameTestNamespaces=frostedresearch_m2,frostedresearch_matching -PgameTestDirectory=build/research-link-game-20261117 -PthermalTestHeap=4G -PthermalTestProcessors=4 --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过，15项required tests全通过，3m54s；日志`build/research-link-game-final-20261117.log`。
- 覆盖0–7数字普通通关、必要前置/联络独立辅助覆盖、来件不减难度、固定本局与重排降级、无计时payload、取消/真实容器关闭/离线、不重复授予、知识失效、正式物品组合。既有2048局可解性检查继续通过。
- `LinkClientValidation`临时助手在Research Playtest新建flat夹具世界；实际Screen点击经普通C2S完成正式土制炉联络。`result.json=passed/phase16`：2级棋盘→Esc取消无想法→重新开始→夹具补两项辅助→重排0级→客户端独立搜索并逐对点击→非作弊中央点击获得想法。玩家无作弊权限，绘图台三工具槽始终为空，旧棋盘保持空。
- 人工检查2级和0级截图，难度/缺项、帮助、牌面与底部操作完整可见。证据：`C:/Users/chang/.codex/visualizations/2026/11/08/01a1ba78-b37a-7762-9426-964f95a6aee3/research-link-game-20261117/build/link-client/`。
- 正式FH JAR已同步Playtest并逐字节核对，备份`build/prism-backups/2026-11-17_12-55-46-link-game/`。客户端正常退出，临时helper与marker已删除；原GUI缩放/帧率/语言/失焦暂停设置恢复。生产包排除GameTest/ClientValidation类。
- 本轮仅选择性提交FH研究文件，不推送。FH原`.gitignore`及其它未跟踪内容、TWR原暂存`config/.gitignore`与.workbuddy原样保留。

## Remaining

- 当前最高为7数字；现有三个正式想法全缺辅助时分别为土制炉2级、冶铁炉1级、耐火砖3级。辅助齐全均0级。
- 客户端验证使用真实Screen事件与C2S、夹具知识和新世界；不是物理鼠标/键盘输入或完整生存路线验证。
