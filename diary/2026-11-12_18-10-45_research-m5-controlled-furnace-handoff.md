# M5 FH 控温炉、试验砖观察与最终重载索引交接

- Time: `2026-11-12 18:10:45 +08:00`
- Author: `Codex / OpenAI；FH M5生产实现与服务端验证者`
- Status: `completed`（生产实现、服务端、跨进程保存和正常打包；真实客户端界面/JEI另行验收）
- Scope: FH 自有控温炉与直接制品观察索引；另按主协调授权修M4管道画面。基线由主协调确认 FH e8c9a109d / TWR 2aee5753b。TWR当前M5内容仅只读，不覆盖内容者的refractory-brick.md。

## Completed

- 按 [M5决定](../plans/2026-11-12_16-00-00_research-m5-controlled-furnace-decisions.md) 注册 controlled_furnace block/BE/item/menu/CUI、controlled_firing recipe type/serializer、cracked_refractory_brick、模型/掉落/双语与JEI分类。三槽，输出不可手动放入，无自动投料capability。没有新增网络消息：使用原版menu button服务端操作。
- 配方使用 input/fuel/result/failure 的 item/count 小格式及独立可选 experiment；温窗/时长/数量由数据决定，FH不写死TWR试验或批量recipe ID。仅普通fire_clay_ball/charcoal→普通耐火砖/碎砖，不产高耐火或工业窑砖。
- 试验要求当前有效队伍/run的OBSERVATION任务，成功样沿现有ResearchSampleBinding写team/run/requirement/experiment。M4现场物品观察直接复用，不建另一套proof/提交框架；同队队友可观察，无owner、不扣物、不拷BE。普通砖/碎砖/旧run/外队不算。
- 生产每炉同时要求当前 enabled Design 管理该recipe，及真实ResearchHooks.canUseRecipe(team,recipe)。来件区不授权限，缺Design不能靠unmanaged默认公开。完成研究沿既有produce：理解满足则直接学入档，不另造额外学习步骤。遗忘只挡下一炉，已扣料批次保留交付。
- 600–1400°C，每次50，默认1000。启动锁定温度、数量、时长、结果与绑定；运行调温只改下一炉。1050–1150含端点产完好砖，其余同数碎砖并显示欠烧/过烧。TWR正式试验1+1/400tick，生产16+2/600tick均真实加载验证。
- 批次持久保存refund/planned/remaining/binding/outcome；完成后只存pending而不再保留可退坯料。输出受阻等待；取消退内部坯料一次，燃料不退；拆除退运行坯料或已完成成品，互斥。槽内未消耗物品和原炉正常掉落。卸载/离线不补时间，KnowledgeState保持schema5。
- RefractoryObservationIndex 在ServerStarted及全局OnDatapackSync后一次构建：仅可读CraftingRecipe直接输入接受砖tag的结果物品/BlockItem的Block，加明确extra item/block tags，不递归。观察集合O(1)，无每tick扫描，不改注册表tag、不硬依赖可选模组。
- 已核对本地Forge 47.3.0映射字节码：MinecraftServer reload完成回调先替换资源和updateRegistryTags，再PlayerList.reloadResources；该方法在玩家循环外派发OnDatapackSyncEvent(null)，随后发标签/配方。证据见build/m5-server-lifecycle-bytecode.txt与build/m5-playerlist-lifecycle-bytecode.txt。真实零在线reload测试也通过。
- 注册专用ObservationUse并刷新KnowledgeDefinitions revision；新鲜观察命中时派发frostedresearch:refractory_component_observed，与原observation_completed放同一IdeaAcquisitionService.fire，保持M2一次动作同一想法最多一次概率。用途/采样/休眠沿M1/M2，不复制任意NBT。
- M4可读性按授权只改KnowledgeLayer：九个JournalButton仍是原坐标/30×30/原点击动作，label为空；叠加disabled、只render的UIElement，从中心按N/E/S/W=1/2/4/8画4像素宽管线到边缘，墙暗色方块，中行左右箭头，won后ACCENT。JournalButton公共API和游戏规则不改。

## Decisions and documentation impact

- 同模式多个配方按ID稳定排序，使用首个材料匹配且资格有效者；试验任务复用observationTargets的稳定run顺序。没有通用炉物理、自动归属、阶段图或新任务框架。温度是工艺玩法参数，不冒充世界热网实温。
- 既有陶窑、IE高炉、II工业窑、旧blast_furnace效果/线索/父子项没有生产或权限改动，没有补偿或伪造研究history。TWR由内容者维护；本节点不运行KubeJS整包来冒称真实材料生存路线。
- 新 [控温炉实现文档](../docs/knowledge/controlled-furnace.md)，并更新definitions、observations-and-notes、workbench、results-and-access、stages-and-airflow。未改独立refractory-brick.md、README、总计划、coordination或design。
- build.gradle仅追加末尾M5 opt-in块：-Pm5Tests加入src/m5，runtime仅继承gameTest.runtimeClasspath；不包含compileClasspath，不需要Charcoal Pit。读取重启阶段才跳过cleanResearchGameTestWorld。原M3C/M4段不归本轮。

## Validation

进程JAVA_HOME=`C:/Program Files/Java/jdk-17.0.2`，所有Gradle串行。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileM5IntegrationJava runGameTestServer '-Pm5Tests' '-PgameTestNamespaces=frostedresearch_m5,frostedresearch_m4,frostedresearch_m3,frostedresearch_m2,frostedresearch_m1' '-PgameTestDirectory=build/m5-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [m5-validation-2.log](../build/m5-validation-2.log)：**18:00:23，29 GAME TESTS COMPLETE / All 29 required tests passed；BUILD SUCCESSFUL 7m29s**。10项M5 +19项M1/M2/M3/M4关键回归，无Charcoal Pit。
- M5包括正式Codec/引用、三个开始观察条件及砖参考的进入前后冻结；真实400tick服务器炉/队友60tick新鲜观察/放置者离线，不提交不扣物；普通砖/旧run/外队无效；完成正常produce解锁真实canUseRecipe；来件/遗忘/缺Design/放弃限制；600/1000/1050/1100/1150/1200/1400边界、399/400与599/600、运行调下一炉；满输出、取消、重复settle、实际destroyBlock；真实远区块已离开ServerChunkCache后保留绑定/暂停/恢复；真实零在线reload；一跳/不递归/extra tags/最终RecipeManager替换/观察用途刷新；真实配方网络往返、9格手工坯料和炉体配方匹配、服务端menu操作/输出槽规则/调温上下限。
- [m5-restart-write.log](../build/m5-restart-write.log)：**18:03:50，All 1 required tests passed；BUILD SUCCESSFUL 3m13s**。独立Forge JVM，实际Chorda队伍与两台远处炉：一台运行中剩300tick、下一炉1150/本炉1100，另一台已完成但输出堵住。
- [m5-restart-read.log](../build/m5-restart-read.log)：**18:06:29，All 1 required tests passed；BUILD SUCCESSFUL 2m11s**。第二个Forge JVM同一build/m5-restart-20261112目录，明确cleanResearchGameTestWorld SKIPPED；真实队伍/run、剩余加载tick、两种温度、binding和pending都恢复；清输出只交付一份真实绑定完好砖，未完成批次仍可退坯料。不是同进程NBT代替，未宣称崩溃恢复。
- 重启命令沿上述公共参数，仅namespace改frostedresearch_m5_restart、目录改build/m5-restart-20261112，分别加-Pm5RestartPhase=write/read。日志均检查本轮All N与总数，不以exit0冒认通过。
- [m5-production-jar.log](../build/m5-production-jar.log)：默认jar/reobfJar，**BUILD SUCCESSFUL 1m14s**；build/libs/frostedheart-1.20.1-0.7.10.jar，**14896430 bytes**。只读ZIP检查含炉BE、serializer、index、CUI、JEI、模型和掉落；没有GameTests、M5/M4正式fixture或m5_test数据。
- src/m5的四份正式知识JSON（含link）、四份制作/试验/批量配方及三个正式tag共11份，与当前TWR逐字节一致；两份extra tag增加了明确测试成员，另有两份直接/间接测试配方，均只在test source set。新增JSON、两语言UI键与新源码/fixture无尾部空白核对通过。
- 首轮[m5-validation.log](../build/m5-validation.log)真实26项，23过/3失败（BUILD FAILED）。三项是测试预期错误：produce已有理解时直接学入档，并非总进入inbox；空ItemStack保存的CompoundTag不是空tag，应用ItemStack.of判断。修断言契约后第二轮29全过，未为通过放宽生产判定。初次脚本受Windows默认gbk读取影响中止，后续统一显式UTF-8；最终源码完整编译通过。

## Client handoff and remaining

- **本轮Gradle已结束；生产/测试源码窗口释放并冻结，主协调可组织M5客户端jar+helper。** 本节点未改src/m3c，不声称新helper已编译。未Git暂存/提交/推送/切分支/重置，未操作Prism或用户实例。
- 客户端入口：受控炉右键；CUI450×220；三槽为0坯料/1燃料/2输出，其后玩家背包。原版menu button动作0=-50、1=+50、2=空闲切换模式、3=开始、4=取消。默认1000，两次+50到1100即可成功试验。样品手持/容器新鲜观察，不提交；观察后仍须正常手动结束末阶段。
- 真实客户端控温界面、JEI显隐/实际取出、多人操作、正式完整TWR/KubeJS材料生存路线，以及M4新管道截图/点击仍由客户端验证者完成；服务端与编译不能替代上述证据。
- M4原客户端通过由主协调确认；本轮没有重跑38项。M4管线新渲染已随生产编译，保留原9按钮坐标可供既有M4/M7驱动继续。
- 主协调负责统一审阅、Git与README链接。完整本节点文件如下；build.gradle仅末尾M5段，未包含内容者新文档/diary或其它节点源码。

## Exact file manifest

- `build.gradle`
- `diary/2026-11-12_18-10-45_research-m5-controlled-furnace-handoff.md`
- `docs/knowledge/controlled-furnace.md`
- `docs/knowledge/definitions.md`
- `docs/knowledge/observations-and-notes.md`
- `docs/knowledge/stages-and-airflow.md`
- `docs/knowledge/workbench.md`
- `docs/research/results-and-access.md`
- `src/m5/java/com/teammoeg/frostedresearch/knowledge/ControlledFurnaceGameTests.java`
- `src/m5/java/com/teammoeg/frostedresearch/knowledge/ControlledFurnaceRestartGameTests.java`
- `src/m5/resources/data/m5_test/recipes/direct.json`
- `src/m5/resources/data/m5_test/recipes/indirect.json`
- `src/m5/resources/data/the_winter_rescue/recipes/research/controlled_furnace.json`
- `src/m5/resources/data/the_winter_rescue/recipes/research/fire_clay_hand_mix.json`
- `src/m5/resources/data/the_winter_rescue/recipes/research/refractory_brick_batch.json`
- `src/m5/resources/data/the_winter_rescue/recipes/research/refractory_brick_trial.json`
- `src/m5/resources/data/the_winter_rescue/tags/blocks/research/refractory_bricks.json`
- `src/m5/resources/data/the_winter_rescue/tags/blocks/research/refractory_products_extra.json`
- `src/m5/resources/data/the_winter_rescue/tags/items/research/refractory_bricks.json`
- `src/m5/resources/data/the_winter_rescue/tags/items/research/refractory_products_extra.json`
- `src/m5/resources/data/the_winter_rescue/tags/items/research/refractory_raw_materials.json`
- `src/m5/resources/m5-formal-fixtures/ideas/refractory_firing.json`
- `src/m5/resources/m5-formal-fixtures/links/refractory_firing.json`
- `src/m5/resources/m5-formal-fixtures/projects/refractory_brick.json`
- `src/m5/resources/m5-formal-fixtures/results/controlled_refractory_production.json`
- `src/main/java/com/teammoeg/frostedresearch/FRClient.java`
- `src/main/java/com/teammoeg/frostedresearch/FRContents.java`
- `src/main/java/com/teammoeg/frostedresearch/compat/ControlledFiringCategory.java`
- `src/main/java/com/teammoeg/frostedresearch/compat/JEICompat.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/experiment/ControlledFurnaceScreen.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFiringRecipe.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceBlockEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/ControlledFurnaceMenu.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/furnace/RefractoryObservationIndex.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java`
- `src/main/resources/assets/frostedresearch/blockstates/controlled_furnace.json`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `src/main/resources/assets/frostedresearch/models/block/controlled_furnace.json`
- `src/main/resources/assets/frostedresearch/models/item/controlled_furnace.json`
- `src/main/resources/assets/frostedresearch/models/item/cracked_refractory_brick.json`
- `src/main/resources/data/frostedresearch/loot_tables/blocks/controlled_furnace.json`
