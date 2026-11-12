# M2 行为想法与短联络实现交接

- Time: `2026-11-12 14:07:13 +08:00`
- Author: `Codex / OpenAI；M2 实现者`
- Status: `completed`（代码、示例及服务端自动验证；真实客户端等综合验收另列）
- Scope: FH knowledge 行为获取、事件桥接、联络等待、协议/绘图台、配置、示例、GameTest 和 living docs。基线 `23b08d3c1`，分支 `research-261108`。

## Completed

- IdeaDefinition.acquisition 新增 required / optional / chance / triggers，沿用 KnowledgeCondition。多个触发入口 OR，同一次操作/级联每个队伍的同一想法最多一次概率尝试；必要知识只限制行为/联络，外部 receive/produce/learn 不新增门槛。understanding 继续是独立学习条件。
- IdeaAcquisitionService 按事件 ID 加载索引，支持 item_obtained / item_used / block_interacted / observation_completed / knowledge_learned，以及公开 emit(player, eventId, Optional<KnowledgeElement>) 自定义入口。KnowledgeBehaviorEvent 带 team / actor / id / subject；学习级联用队列与每次级联尝试集合，已有档案/来件输出直接跳过，不刷重复产出或通知。
- 物品获得采用实际 post-pickup 事件，以及一次菜单点击前后的玩家物品净增量。玩家背包、装备、副手、光标及玩家工作网格原料都算已有；制作/熔炉/外部容器结果可触发，纯内部搬动或从工作格取回原料不触发。没有另外监听 craft/smelt 再抽第二次概率。
- 成功 useItem 接受动作与实际成功 ItemStack.useOn 的物品动作、成功 useItemOn 方块交互接通；同一次 useOn 的物品/方块信号合并。持续物品使用在服务器接受开始时计一次，非每个持续 tick，失败/PASS 不计；本轮没有覆盖其他模组绕过这些原版入口的自定义操作，它们可调用公开 API。
- ObservationSessions 完成有效采样即派发动作，早于 receive/learn，重复记录不抹去动作；旧档案学习不补发采样动作。全部 acquisition required / optional / trigger.subject 观察条件进入潜在单槽用途，包括尚未取得的想法。
- KnowledgeLinkSessions 实现每玩家临时计时、重复请求不重置、进度/取消/完成通知、必要知识/选定规则/队伍/容器校验；关闭容器或离线取消，容器内正常翻页继续。每 tick 只匹配已选 rule ID，不枚举全部候选；等待完成不抽概率。
- 联络支持作者明确声明的 0/1 输入，旧 2–5 槽仍保持完整匹配/互异身份/跨字段语义；不从所有想法生成候选。可选知识读档案，无需摆桌面。UI 档案空选择也有联络入口，显示等待秒数和取消；已在来件/档案的输出不重复列为候选。
- 修正主协调指出的笔记物品路径：ItemRetrieval.behaviorSample 与 observe/canObserve 分开。行为可描述笔记物品本身，不能据此学习其内含知识；主动检视与笔记研读规则保持。
- 示例包新增 field_idea / learned_hint、behavior_study、field_zero / field_one / field_legacy；其中 learned_hint 没有联络规则，演示通过学习行为取得。同步更新 definitions / state-and-api / workbench / observations-and-notes、示例 README 和 GameTest README。

## Decisions

服务器集中默认值：knowledgeBehaviorChance=0.15、knowledgeBehaviorOptionalBonus=0.1、knowledgeLinkTicks=200、knowledgeLinkOptionalReduction=20、knowledgeLinkMinimumTicks=20；想法可显式覆盖 chance，示例用1便于复现。

p=min(1,p0+n×bonus)；ticks=max(min(base,minimum),base−n×reduction)。n 为已满足 optional 项数。等待时长在开始固定，必要知识与规则持续复核；不新造货币或永久候选账本。

- chance 明确只接受有限的 [0,1] 数值，避免 JsonOps 把 Boolean 隐式当作数值；其他新可选字段沿用 strictOptionalField / ProjectRequirement.field，错误类型不静默变空。
- 一个容器 click 聚合取得的所有物品是一次操作，一次 shift-click 的多份产物不按物品个数反复抽概率。外部容器取出视为取得，未新增永久所有权账本。
- 短计时不持久化，存档 schema 仍为3。关闭后重开从零开始；不改 M1 项目 access/origins 规则、不实现阶段或设备。

## Validation

所有命令在 FH 根，进程 JAVA_HOME=C:/Program Files/Java/jdk-17.0.2。未改全局环境，未同时运行多份构建或客户端。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-PgameTestNamespaces=frostedresearch_m2,frostedresearch_m1,frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=build/m2-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [最终日志](../build/m2-validation-3.log)：**BUILD SUCCESSFUL，5m35s，17 个 required GameTest 全通过**；含 6 个新增 M2 和原有 11 个回归。完整 compileJava / compileGameTestJava 均纳入，没有排除测试或引入 JUnit。服务器正常保存退出。最终构建开始后没有再改生产或测试源码，也没有重复运行成功的检查。
- 新测试：必要知识不阻外部 receive/learn/produce；多入口独立可用、批量入口只抽一次、输出已有不重抽；可选概率提升；默认200tick、减免至180tick、提前不能完成、取消/重复请求/必要知识撤销；0/1/旧双槽；学习 A↔B 级联终止；触发观察用途；真实重复采样动作；示例引用与严格字段解码。
- 实际服务器桥接：ItemEntity.playerTouch（含研究笔记）、InventoryMenu 制作结果、真实 FurnaceBlockEntity 输出取出、ChestMenu 快捷取出、背包/光标与工作网格原料搬动、ServerPlayerGameMode 物品使用/放置/拉杆交互；真实 DrawDeskContainer 保持计时、closeContainer 取消、PlayerList.remove 离线取消。熔炉输出由测试布置为已烧成，验证真实取出入口，不冒充等待烧炼全流程。
- 重复观察测试是已登录服务器玩家，两次真实 ObservationSessions 计时，中间关闭第一次记录预览；先前档案已经有同等纸观察，动作仍两次发出，想法只取得一次。它不代表物理键盘输入或客户端画面。
- 原有 ObservationGameTests 的孤立 Observer 从未登录 FTB；为了派发带队伍上下文的新动作，在其 tick 中临时提供 Chorda 测试队伍并立即恢复原 provider，保留全部原断言。新 M2 测试另用真实 placeNewPlayer/FTB 登录，不以测试替代真实队伍入口。
- [m2-compile.log](../build/m2-compile.log) 初次编译缺 Optional 引用、[m2-validation.log](../build/m2-validation.log) 测试 SimpleContainerData 包名错误，均已修复。第一次实际运行 [m2-validation-2.log](../build/m2-validation-2.log) 17项有2项失败：布尔概率类型、未关闭首份记录预览就再次观察的测试步骤。前者修编解码，后者修测试操作顺序；最终17项全过。
- 示例包 JSON 解析通过，6份 m2-fixtures 与对应示例 JSON 相等；git diff --check 在本阶段路径通过。日志仍有 M0/M1 已记录的依赖/客户端类/配方告警，不将通过等同于正式整合包无告警。
- TWR 无改动，仍保留既有 staged config/.gitignore 与未跟踪 .workbuddy/。design 无改动；没有改总计划或 coordination。所有 Git 命令仅只读查询，未 add/commit/push/reset/切分支。

## Changed files

完整本阶段清单（不包含主协调 coordination 或其他既有未跟踪文件）：

- `docs/knowledge/definitions.md`
- `docs/knowledge/example-datapack/README.md`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/learned_hint.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_legacy.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_one.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/links/field_zero.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/projects/behavior_study.json`
- `docs/knowledge/observations-and-notes.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/workbench.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionGameTests.java`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/ObservationGameTests.java`
- `src/gametest/resources/m2-fixtures/ideas/field_idea.json`
- `src/gametest/resources/m2-fixtures/ideas/learned_hint.json`
- `src/gametest/resources/m2-fixtures/links/field_legacy.json`
- `src/gametest/resources/m2-fixtures/links/field_one.json`
- `src/gametest/resources/m2-fixtures/links/field_zero.json`
- `src/gametest/resources/m2-fixtures/projects/behavior_study.json`
- `src/main/java/com/teammoeg/frostedresearch/FRConfig.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeBehaviorBridge.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeLinkSessions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/IdeaAcquisition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/IdeaDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/event/KnowledgeBehaviorEvent.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/item/ItemRetrieval.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkRule.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeItemUseAcquisitionMixin.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeMenuAcquisitionMixin.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/KnowledgeUseAcquisitionMixin.java`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `src/main/resources/frostedresearch.mixins.json`
- `diary/2026-11-12_14-07-13_research-m2-behavior-contact-handoff.md`

## Remaining / handoff

- 主协调复核后统一暂存/提交；本实现者未提交或推送。
- 客户端计时/取消画面与键鼠、小窗口布局、双客户端同队行为、独立进程重启仍待后续综合验收；没有启动客户端、关闭用户游戏或部署到 Prism。
- 本轮未改 TWR 三个正式项目、配方/阶段、临时设备或正式生存路线。M3–M5 再把本 API 接入具体材料和设备，并对绕过原版玩家菜单/使用入口的第三方行为按实际接点补桥接。
