# M6 合并研究记录、备用进度与同台目标一致性交接

- Time: `2026-11-12 19:32:38 +08:00`
- Author: `Codex / OpenAI；FH M6生产实现与服务端验证者`
- Status: `completed`（源码、最终定向服务器测试与生产打包；M6实际客户端界面接续验收）
- Scope: 基线 FH6b4605d0b / TWRaa076eac7；采用 [M6决定](../plans/2026-11-12_18-33-11_research-m6-merged-runs-decisions.md)。TWR、Prism、Git、src/m3c不属于本节点改动。

## Completed and decisions

- `ResearchRuns` 集中定义创建time、UUID并列排序及有效完成/standby规则；KnowledgeService、KnowledgeView/JournalData/Layer、sampleTargets/observationTargets、临时来源和设备共用。正常开始不造第二份open run，显式merge仍按UUID完整保留各份历史，不修改KnowledgeState schema5或项目数据字段。
- 多份可推进记录时研究页才显示选择，附派生记录序号、阶段与进度，不显示UUID；选中ID仅客户端按项目保留，操作继续携带原UUID。M3/M4既有cycle文案能区分记录与阶段，实验台候选也有对应标签。缺失/放弃的选择回退到同一稳定首份。
- 必须存在真实completed run且当前全部成果有效已学，项目才优先完成。其它open run原数据保留为standby，任务入口和TemporaryResearchSource同步暂停；遗忘任一成果或定义失效后原进度恢复，跨项目与永久来源仍取并集。外来成果不制造completed历史，放弃只清选中run；旧legacy_completed/legacy_superseded保持原规则。项目snapshot completed也采用当前有效完成判定，不能用历史完成标志盖住遗忘后恢复的空任务阶段。
- M5下炉selectedTarget与本炉binding分离；多个目标无有效选择时先选择、不扣料，有效选择不被新合入记录覆盖。新增可选BE selected_target及展示字段，兼容缺字段的旧M5 NBT，不更改已投料的refund/planned/pending/remaining。原公开start/binding及原版menu button0–4保留；5切目标。候选只在知识/定义revision或RecipeManager变化时重建。
- 已投料M5批次仍一次性交付；备用期间样品不推进任务，恢复后新鲜观察可继续使用正确绑定。实验台已投入的未完成实验原地暂停，旧菜单check/start也不清投入，遗忘后继续原progress并清旧暂停提示。merge不搬世界设备或改写未提交样品team绑定；须重新绑定/制作，归属旧队伍的炉可拆下重放，已提交研究进度保留。
- 主协调转述Astra只读review发现的两处同台P2已修：ExperimentTableActionPacket非SELECT操作核对点击run/requirement，旧R1请求不得停止/启动队友新R2；备用状态STOP发送实际台面绑定。实际控温CUI开始使用ControlledFurnaceStartPacket携带所见模式/四字段binding，menu.startSeen不匹配只刷新，不扣料或改投另一run。不新增持久序列、全局框架或任务结构。主协调转述Astra复核触发链已解决，无新阻塞。
- 修正重复的最终索引用途注册：RefractoryObservationIndex仍在最终生命周期一次扫描，内容变化或用途缺席才registerObservationUse；相同内容不额外加revision，保留旧桥接同revision的最终RecipeManager刷新契约。
- 更新六份living docs、src/gametest README的M5/M6最小入口，以及本M6决定的实际status/outcome；详细规则由文档拥有，不复制到README。没有改根README/四份主协调文档、总计划/coordination、design、旧81目录或任一TWR文件。build.gradle也无需修改，复用既有-Pm5Tests的独立src/m5运行类路径，没有Charcoal Pit依赖。

## Validation and exact source boundaries

进程Java17为 `C:/Program Files/Java/jdk-17.0.2`；全部Gradle串行，按要求中途交出helper窗口，归还后才恢复构建。

- [m6-validation.log](../build/m6-validation.log)：首轮真实32项，30过/2失败，BUILD FAILED。实验台最后补的旧菜单guard在该轮compileJava启动后编辑，未进入该class，已读字节码确认；另一项是M5索引重复注册导致旧bridge同revision生命周期断言失败。保留事实，不把这轮算通过。
- [m6-validation-2.log](../build/m6-validation-2.log)：**19:11:34，32 GAME TESTS COMPLETE / All 32 required tests passed；BUILD SUCCESSFUL 7m42s**。9项M6加23项受影响M3样品、M3B旧桥接、M4阶段/临时权限与M5炉回归。此轮覆盖排序/选择/standby、实验台投入保护和幂等索引，但**不覆盖在该轮编译后追加的project.completed快照修订及后续review的两处P2消息端点**。
- [m6-final-validation.log](../build/m6-final-validation.log)：**19:23:39，16 GAME TESTS COMPLETE / All 16 required tests passed；BUILD SUCCESSFUL 5m55s**。最终完整源码重新编译后，10项M6（5纯状态/快照、5真实设备）和6项M3B定向复验，覆盖上述晚补修订。没有把先前32项结果覆盖到新源码，也未重跑无关大套。
- 最终M6检查实际调用merge，构造插入顺序/UUID与time顺序相反、不同阶段的两份记录；核对server默认、snapshot、JournalData显式选择及M3/M4/M5目标一致。验证schema5与BE NBT往返/旧字段兼容、真实完成后暂停/遗忘恢复、全部成果有效性、单独放弃、正常单run忘记重开、外来成果不伪完成、跨项目临时来源并集和旧队伍未提交实物不改绑。
- 真实设备检查包含已投料批次交付、暂停样品不推进/恢复后新鲜观察生效；真实实验台tick、旧check/start不清投入、暂停/恢复。新双玩家交错测试使用两个实际ServerPlayer/菜单与消息codec往返：B选择并投入R2后，A旧R1实验台STOP/START不损R2进度；A旧炉开始不扣料或投R2，B当前开始只产R2绑定批次，迟到A请求不改该批次。该服务端测试不是两桌面客户端实机证明。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileM5IntegrationJava runGameTestServer '-Pm5Tests' '-PgameTestNamespaces=frostedresearch_m6,frostedresearch_m6_devices,frostedresearch_m3b' '-PgameTestDirectory=build/m6-final-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

32项轮次另含frostedresearch_m4/frostedresearch_m3/frostedresearch_m5三个namespace，目录build/m6-gametest-20261112。M6新选择的持久化为实际BE/队伍NBT保存重载检查，**未另跑独立JVM，不把M5既有重启证据冒称覆盖M6新字段**。

## Final production artifact

- [m6-production-jar.log](../build/m6-production-jar.log)：第一次停留在Gradle任务输出前。只读线程栈 [m6-jar-stall-thread-dump.txt](../build/m6-jar-stall-thread-dump.txt) 明确Daemon worker卡在Socket/HTTPS read→ForgeGradle DownloadUtils/MavenArtifactDownloader；没有产生新版包。按主协调授权验证PID/父进程后只停止本轮daemon18356，其wrapper正常失败退出，未操作其它Java/客户端进程。
- [m6-production-jar-retry.log](../build/m6-production-jar-retry.log)：保持离线缓存，在**仅本次JVM**设置连接/读取各10000ms超时后，默认生产jar/reobfJar **BUILD SUCCESSFUL 1m6s**；compileJava为UP-TO-DATE，使用已通过最终16项的main类。没有修改全局Gradle/网络/DNS/Prism设置。

```powershell
.\gradlew.bat jar --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false' '-Dorg.gradle.jvmargs=-Xmx4G -XX:MaxMetaspaceSize=1G -Dfile.encoding=UTF-8 -Dsun.net.client.defaultConnectTimeout=10000 -Dsun.net.client.defaultReadTimeout=10000'
```

产物：`build/libs/frostedheart-1.20.1-0.7.10.jar`，**14907337 bytes**。只读ZIP核对ResearchRuns、新ControlledFurnaceStartPacket、炉BE和FRNetwork注册存在；无GameTests、M4/M5正式fixture或m5_test资源。最终16项之后没有再改生产/测试源码。

## Handoff and remaining

- **Gradle已空闲；生产、测试源码冻结，客户端可直接用最终M6包编新helper，做M5完整流程与M6多run UI验证。** 产物与主类均为上述最终源码。清单同时保存为build/m6-owned-files.txt。
- 新控温CUI实际开始消息携带所见模式/target，原按钮0–4/API保留，5切目标；客户端验收请使用实际按钮覆盖新端点。实验台STOP即使无候选仍携带实际run/requirement。研究页只多open记录显示左右选择，不展示UUID。
- M6实际客户端排版、双玩家真实点击和最终包M5完整流程交由客户端继续；不把固定M5或M7既有结果计作本轮M6证据。helper getSlot重映射问题由客户端自修，本节点未改src/m3c或部署实例。
- 主协调负责Git、目录处置/总文档。此节点未暂存/提交/推送/分支切换/重置，未操作Prism或外网/DNS。

## Exact owned manifest

- `diary/2026-11-12_19-32-38_research-m6-merged-runs-final-handoff.md`
- `docs/knowledge/controlled-furnace.md`
- `docs/knowledge/stages-and-airflow.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/trial-kilns-and-samples.md`
- `docs/knowledge/workbench.md`
- `docs/research/results-and-access.md`
- `plans/2026-11-12_18-33-11_research-m6-merged-runs-decisions.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/MergedResearchRunGameTests.java`
- `src/m5/java/com/teammoeg/frostedresearch/knowledge/MergedRunDeviceGameTests.java`
- `src/main/java/com/teammoeg/frostedresearch/FRNetwork.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchRuns.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeJournalData.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/experiment/ControlledFurnaceScreen.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/experiment/ExperimentTableScreen.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/ExperimentTableActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/ExperimentTableBlockEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/PrototypeTuyereBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TrialKilnBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceBlockEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceMenu.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceStartPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/RefractoryObservationIndex.java`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
