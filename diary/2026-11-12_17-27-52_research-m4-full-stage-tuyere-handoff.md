# M4 FH 有序阶段、风道与原型试吹最终交接

- Time: `2026-11-12 17:27:52 +08:00`
- Author: `Codex / OpenAI；FH M4生产实现与服务端验证者`
- Status: `completed`（生产实现、编译、服务端验证和打包；M4客户端由主协调组织，未在此冒充通过）
- Scope: FH 阶段/分支/临时配方、风道、原型试吹、队伍观察proof、旧iron单Design承接；基线 FH81452c69e / research-261108，TWR3501a9276后的正式内容只读配合。

## Completed

- 按修订 [M4-A接口](../plans/2026-11-12_15-45-00_research-m4-stage-tuyere-interface-decisions.md) 实现 StageDefinition/Task/Variant、StageRunState、ResearchStages，顶层平铺需求适配main单阶段。阶段有序手动推进，进入冻结变体/任务集合，默认路径可玩，可选任务不阻塞ready；旧expectedStage重包不能连跳。全部旧计算/证据/实验/样品入口只接当前任务，M1逐项未知占位、M2回环与自动去重、M3样品契约保持。
- KnowledgeState schema4→5；旧progress maps保留，未知定义恢复不丢历史；平铺改多阶段时只有首阶段同ID旧进度生效，其余原值存可选内部 stage_state.legacy_progress（LegacyStageProgress），不伪造未来阶段工作。不改TWR项目字段或M3样品四字段。
- ready首次记录completion_recipes。TemporaryResearchSource(project,run,stage)与永久成果、旧来源、其它run取并集；完成/放弃/legacy_superseded只撤自身。全部声明配方受管，最终配方重载还按专用prototype_tuyere产物索引受管集合，避免定义缺席时成为unmanaged公开路线。沿既有TeamResearchService/technology projection/JEI同步，未另造解锁表。
- 研究快照/客户端增加当前阶段目标、方法、可选性、历史结论、临时配方及手动推进/最后完成；未进入变体不下发。普通M3单阶段不多点一次。绘图台平铺空requirements检查与全阶段任务标题查询已适配。
- 独立AirflowGame：3×3固定可解拓扑随机旋转五格，服务端旋转判连通，无评分/时限/纸笔消耗；绘图台持久airflow_target/airflow_game，关闭重开不重置，重排只改盘面。旧ResearchGame纸牌、纸笔和计算回调保留。
- 注册prototype_tuyere方块/BE/物品/模型/双语。绑定team/run/task/experiment，不存个人owner；可选任务明确选择，无候选交互保留既有绑定并反馈。Astra指出的外队空手查看清A队绑定P2已按最小修法修复并用真实Block.use覆盖。
- 可选Charcoal Pit BlockBellows.blow HEAD接点在原Bloomery检查前处理原型；不需要Bloomery。真实成功后替换为原生sandy_tuyere默认状态（无朝向、无BE），后续普通供气原样继续。机械风箱的实际延迟tick同样验证。可选检测用Forge模组清单，不加载BlockBellows类，缺CharcoalPit也能启动。
- 每维度PendingResearchProofs SavedData保存待观察位置→四字段绑定，配位置/绑定/run索引；同team/run/task跨维度仅保留最近成功对象。真实方块变动删位置proof、区块卸载保留，完成/放弃/承接合并清run关联。新鲜ObservationSessions在preview/receive之前判任务；观察者当前队伍与当前run匹配即可，放置者退出不影响。样品观察也复用M3 stack binding，不消耗，不实现M5设备。
- LegacyResearchBridge校验扩展至非空Design/Construction映射，支持旧iron单Design同时补齐普通风口和主冶铁炉；M3两类成果映射原样回归。无旧奖励重放、无伪造实验history。
- 修复M3C截图确认的legacy知识页rawkey：三类调用使用绝对knowledge.frostedresearch.research.*键；旧档案键保留，不复制文案。新增M4知识页统一gui.frostedresearch.journal.*。两语言和调用静态检查通过，真实客户端复核由验证者执行。

## Decisions and ownership

- 开始前置是想法 + 铜/青铜锭tag观察 + coal/charcoal/coke tag观察；组内OR只用内容tag，组间start_all AND。获取想法时两组仍为acquisition.optional。没有通用OR框架、阶段图、质量评分或气流物理模拟。
- 正式TWR知识JSON、三个tag与prototype配方只读复制为fixture，当前iron_materials为29项；其余两条生产recipe测试fixture按KubeJS声明构建，不声称执行了正式KubeJS整包。
- build.gradle **仅末尾M4专用段**归本实现者：-Pm4CharcoalPitTests的实际服务器依赖、src/m4独立source set、restart write/read属性与读阶段保留world。运行类路径只继承gameTest.runtimeClasspath，不带compileClasspath。其它M3C配置/hunks由主协调汇总。
- 不含验证者两份src/m3c客户端driver；没有把它们列为本轮已编译/验证产物。未编辑iron-bloomery.md、design/、总计划、coordination或README链接。未操作Git、未部署/启动/关闭Prism或用户进程。

## Actual validation

进程JAVA_HOME=`C:/Program Files/Java/jdk-17.0.2`，所有本节点Gradle串行；曾按主协调要求两次让出helper构建窗口。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-Pm4CharcoalPitTests' '-PgameTestNamespaces=frostedresearch_m4,frostedresearch_m4_cp,frostedresearch_m3b,frostedresearch_m3,frostedresearch_m2,frostedresearch_m1,frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=build/m4-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [m4-validation-5.log](../build/m4-validation-5.log)：**17:11:58，38 GAME TESTS COMPLETE，All 38 required tests passed；BUILD SUCCESSFUL，6m42s**。包含原27项和11项M4：4项阶段/迁移/权限、7项真实依赖/正式内容集成。
- 覆盖阶段前后补知识/可选任务、变体冻结、旧包不连跳、任务范围、无任务变体、最后完成一次、schema4/5往返及缺定义恢复、平铺未来进度不冒领、双run及永久权限并集。真实正式JSON/Codec/引用、铜/青铜×三燃料六组替代、来件不算开始前置、旧iron单Design补偿。
- 风道使用真实服务端旋转求解，桌面真实NBT重建/关闭再选不重排，空纸笔库存仍成功，旧阶段游戏不能记后阶段。独立纸牌/实验台/样品回归同时过。
- 普通风箱真实use后前8tick不转化、实际10tick延迟blow后转化，无Bloomery、真实正常风口无BE；放置者退出，同队观察者通过新鲜ObservationSessions完成任务，随后正式Design接管两生产权限。观察fixture固定玩家位置，并显式分发既有PlayerTickEvent处理器，未直接调用任务成功来冒充观察。
- 机械风箱使用真实红石邻接安排的延迟tick；普通风口/原生TileBloomery.airBuffer增加，证明原供气路径仍执行。错误朝向、外队、无proof普通块、拆换后重放、最近试件、跨维度替换、丢失可重制、放弃/承接合并清proof均覆盖。M3 stack四字段对新鲜同队物品观察有效，旧run/普通物品不算、不扣物。
- 区块检查先强制远处区块加载，释放后等待其**确实离开ServerChunkCache**再重载；normal对象/proof与未吹原型BE绑定均保留。不是仅BE.loadStatic的卸载宣称。
- [m4-restart-write.log](../build/m4-restart-write.log)：独立Forge JVM，**17:16:00 All 1 required tests passed**，3m9s，正常保存退出。以同一运行目录build/m4-restart-20261112、namespace frostedresearch_m4_restart、-Pm4RestartPhase=write。
- [m4-restart-read.log](../build/m4-restart-read.log)：第二个Forge JVM，**17:18:33 All 1 required tests passed**，2m12s；-Pm4RestartPhase=read，日志明确cleanResearchGameTestWorld SKIPPED。真实Chorda队伍文件、当前trial/先前ready风道、临时权限、保存的普通风口与pending proof均恢复。不是codec往返或同进程重建；不宣称崩溃恢复。
- [m4-no-charcoal.log](../build/m4-no-charcoal.log)：不带-Pm4CharcoalPitTests，独立build/m4-no-charcoal-20261112，**17:21:59 All 4 required tests passed**；缺依赖基础启动和阶段测试实际通过。
- [m4-production-jar.log](../build/m4-production-jar.log)：默认生产jar/reobfJar，**BUILD SUCCESSFUL，1m15s**；生成build/libs/frostedheart-1.20.1-0.7.10.jar。只读内容检查含AirflowGame/PrototypeTuyere及模型，不含GameTests或测试fixture。此后客户端验证者将合并重建jar+helper，其新driver不是本记录已验内容。
- 四份正式知识JSON、三个tag、原型配方与当前TWR逐字节一致，全部新增资源JSON及两语言有效；legacy绝对键保留，M4 t()键前缀核对。没有路径敏感hash或复制生产版本重编译。

## Failure history and evidence correction

- B1曾把Gradle exit0误报为30项通过，主协调逐行核对后指出，已在 [B1记录](2026-11-12_16-26-00_research-m4b1-stage-checkpoint.md) 明确撤回。本最终交接只使用当轮GameTest完成/总数标志，未引用旧XML。
- m4-b1-validation.log实际是Codec静态循环初始化导致数据包启动失败；KnowledgeCondition默认字段改为独立帮助方法，打断ProjectRequirement初始化依赖，不靠偶然加载顺序。
- m4-validation.log独立测试类漏import，编译失败，未运行测试。m4-validation-2.log可选mod用Class.forName提前加载Block使Chorda mixin失败，改用Forge加载清单。
- m4-validation-3.log专用source set误把compileClasspath带入runtime，Oculus在dedicated server加载Screen，退出0但无测试。已复用基础gameTest运行依赖分离编译/运行类路径。
- m4-validation-4.log终于实际执行38项，36过、2失败：机械风箱检查早于原生10tick，以及悬空测试观察者未保持位置/会话tick。修正fixture时序和真实观察处理，未放宽生产判定；最后第5轮38项全部过。
- 日志仍有既有可选依赖/配方格式等告警。通过仅证明列出的服务器情形，不冒充完整TWR生存或客户端键鼠验收。

## Exact M4 file manifest

清单仅本节点文件。build.gradle按上面的末尾M4段归属，不将M3C部分视为本实现者修改。

- `build.gradle`
- `diary/2026-11-12_16-26-00_research-m4b1-stage-checkpoint.md`
- `diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md`
- `docs/knowledge/definitions.md`
- `docs/knowledge/stages-and-airflow.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/trial-kilns-and-samples.md`
- `docs/knowledge/workbench.md`
- `docs/research/results-and-access.md`
- `docs/research/state-persistence-and-sync.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/StageResearchGameTests.java`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/TrialKilnGameTests.java`
- `src/m4/java/com/teammoeg/frostedresearch/knowledge/ResearchRestartGameTests.java`
- `src/m4/java/com/teammoeg/frostedresearch/knowledge/TuyereResearchGameTests.java`
- `src/m4/resources/data/the_winter_rescue/recipes/research/main_bloomery.json`
- `src/m4/resources/data/the_winter_rescue/recipes/research/prototype_tuyere.json`
- `src/m4/resources/data/the_winter_rescue/recipes/research/sandy_tuyere.json`
- `src/m4/resources/data/the_winter_rescue/tags/items/research/iron_carbon_fuel.json`
- `src/m4/resources/data/the_winter_rescue/tags/items/research/iron_copper_or_bronze.json`
- `src/m4/resources/data/the_winter_rescue/tags/items/research/iron_materials.json`
- `src/m4/resources/m4-formal-fixtures/ideas/iron_smelting.json`
- `src/m4/resources/m4-formal-fixtures/links/iron_smelting.json`
- `src/m4/resources/m4-formal-fixtures/projects/iron_bloomery.json`
- `src/m4/resources/m4-formal-fixtures/results/iron_bloomery_design.json`
- `src/main/java/com/teammoeg/frostedheart/mixin/FrostedHeartMixinPlugin.java`
- `src/main/java/com/teammoeg/frostedheart/mixin/charcoalpit/BlockBellowsMixin_ResearchTrial.java`
- `src/main/java/com/teammoeg/frostedresearch/FRContents.java`
- `src/main/java/com/teammoeg/frostedresearch/blocks/DrawingDeskTileEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/AccessSource.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridge.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchStages.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/TemporaryResearchRecipes.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeJournalData.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeCondition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectRequirement.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/StageDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/PendingResearchProofs.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/PrototypeTuyereBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/PrototypeTuyereBlockEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TuyereTrials.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/game/AirflowGame.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeState.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/LegacyStageProgress.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/ResearchHistory.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/StageRunState.java`
- `src/main/java/com/teammoeg/frostedresearch/mixin/minecraft/research/LevelChunkMixin_ResearchProof.java`
- `src/main/resources/assets/frostedresearch/blockstates/prototype_tuyere.json`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `src/main/resources/assets/frostedresearch/models/block/prototype_tuyere.json`
- `src/main/resources/assets/frostedresearch/models/item/prototype_tuyere.json`
- `src/main/resources/frostedheart.mixins.json`
- `src/main/resources/frostedresearch.mixins.json`

## Remaining and handoff

- **生产/测试源码已冻结；Gradle最后一次jar/reobfJar已退出，构建窗口已明确交接给M4客户端验证者。** 本实现者后续只整理文档，不再启动Gradle或改生产/测试源码，等待主协调M5授权。
- 主协调统一审阅/暂存/提交与README链接合并。本节点没有提交、推送、分支切换或重置。
- M4客户端driver、真实绘图台排版/点击、正式KubeJS完整包RecipeManager/JEI/实际取出与多人客户端、生存材料路线由客户端/内容协调继续；原型与材料链不增加新配方来修并不存在的循环。材料计数引用内容工作者最新说明。
