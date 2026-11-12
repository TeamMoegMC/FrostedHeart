# M3-A 小炉设备与实物提交交接

- Time: `2026-11-12 14:53:36 +08:00`
- Author: `Codex / OpenAI；M3-A 实现者`
- Status: `completed`（本可回退节点的代码与自动验证；M3-B 正式内容/迁移尚未实施）
- Scope: FH 三种原型小炉、共用 BE、模型/配方/语言、绑定试样任务、实际绘图台提交、schema4、示例与 Forge GameTest。基线 d6b5b4e73 / research-261108。

## Completed

- 按 [采用的内容卡](../plans/2026-11-12_13-40-36_research-m3-trial-kiln-content-card.md) 注册 trial_kiln_clay_rich / trial_kiln_sand_rich / trial_kiln_balanced，配比3:1 / 1:3 / 2:2，使用黏土球与沙的直接2×2 shapeless配方。结果为 cracked_trial_kiln / fired_trial_kiln，一共2坏1好。
- TrialKilnBlock / TrialKilnBlockEntity 共用实现：主手至少4根木棍一次扣除，400个加载 tick；phase=0/1/2/3 表示未烧/烧制/完好/损坏；保存 remaining、settled、binding，配比由实际方块 ID 保存。没有离线追赶、热网或M4风箱/阶段。
- 未烧/途中拆除回原配比未烧小炉，燃料不退，重新放置无进度/绑定。完成空手收取或拆除共用先标记后结算的 settle；只出1件结果，收取移除块后不会再掉第二件。共享 collect 方法在背包装不下时掉落剩余结果。
- 成功或失败样都只携带 ResearchSampleBinding 的 team/run/requirement/experiment；不复制全量NBT，不新增材料模拟。实验标识 frostedresearch:trial_kiln，从当前开放且未满足 sample 任务查询目标，不写死 TWR 项目。唯一目标自动关联；多个目标空手循环选择项目/任务后再起烧；没有目标可试烧但明确提示未绑定，不能作为之后研究的样品。
- ProjectRequirement 新增独立 SAMPLE + sample={experiment,item}，固定提交1件；不会伪装成观察或实验台计时。KnowledgeService.submitSample 核对真实物品和当前开放run/任务/队伍/实验后扣1件，写回执；错误或重复不扣，正常 finishResearch 继续统一判所有任务。
- 绘图台 SUBMIT_SAMPLE 从真实 EXAMINE_SLOT 取物：工具页放入样品，无需检视/学习，返回研究页提交，按钮写明消耗1件；显示进度与重复/错误反馈。
- ResearchHistory.samples 记录任务ID到{experiment,item}；schema_version=4，新增可选映射缺席默认空，旧v3和v2兼容不丢既有进度/起源。所有复制方法保留回执，放弃清空任务进度；当前定义改变实验或物品后旧回执不自动满足。
- 新模型全部使用原版贴图，无外部图片依赖：低矮中空炉体、三组原料外观、烧制发光、完成/裂纹；物品模型和中英文名称/交互说明齐备。未启动客户端，不冒充实际渲染验收。
- 完整 example:trial_kiln 示例包括 M2 黏土球取得想法、想法+黏土球/沙观察入档开始、成功样提交、正常完成获得 Finding。只是独立机制示例，不假装TWR正式Design/Construction已落地。

## Decisions and interfaces for M3-B

正式项目可直接用：

```json
{"id":"submit_intact","type":"sample","title":"提交本次试烧的完好小炉","sample":{"experiment":"frostedresearch:trial_kiln","item":"frostedresearch:fired_trial_kiln"}}
```

- 项目 access/results 仍按 M1/M2；M3-B 可声明独立 Design 与 Construction 两项成果，无需改设备绑定代码。
- 坏炉不是必交计数，成功样以起烧时的 run 为准，预制未烧炉可用，旧已烧样不可用于新 run。
- 多目标有明确选择交互，不默选第一个；样品绑定不因中途换项目、放弃或换队自动重写。
- 未新增自定义菜单或额外网络包类型，沿用绘图台操作包、知识快照和原BlockState同步；剩余秒数由右键服务端提示。
- Living docs 更新：新增 [小炉与实物提交](../docs/knowledge/trial-kilns-and-samples.md)，更新 knowledge README、definitions/state-and-api/workbench、示例 README、GameTest README。根 docs 既有知识系统入口已覆盖，无需新增系统条目。

## Validation

全部在 FH 根，进程 JAVA_HOME=C:/Program Files/Java/jdk-17.0.2，无并发构建、无客户端/Prism或用户程序关闭。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-PgameTestNamespaces=frostedresearch_m3,frostedresearch_m2,frostedresearch_m1,frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=build/m3a-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- [最终 m3a-validation-2.log](../build/m3a-validation-2.log)：**BUILD SUCCESSFUL，5m56s，21个required GameTest全通过**。defaultBatch原18项先跑；m3_trial_kiln批次3项后跑，隔离异步测试的临时定义。完整 compileJava / compileGameTestJava 均包含，没有排除现有测试、没有JUnit。服务器正常保存退出，成功后未再改生产/测试代码或重复构建。
- 三配比：原生RecipeManager在玩家2×2工作格匹配三配方；真实Block.use燃料不足3根不扣、4根扣完且绑定当前run；自然世界tick在398仍烧制、405已完成，2坏1好；第180tick保存并通过BlockEntity.loadStatic/setBlockEntity重建，剩余tick与绑定不变，恢复后按自然tick完成。
- 结算：未烧拆除回原料炉；手动调用真实serverTick推进100tick后途中拆除回未烧炉、无燃料、旧BE不再结算；完成收取后实体移除、settle为空，完成拆除每炉1个坏样；共享collect的满背包交付路径给1个世界掉落（该分支直接调用collect，非声称原版空手时背包必然无空槽）。
- 提交：坏样/无绑定/错误队伍/run/任务/实验都不扣；真实绘图台检视物槽消耗成功样；重复不扣；放弃重开后旧样无效；多开放项目先选目标、不默扣燃料；schema4 roundtrip回执相等、v3缺回执正常读空。真实自然tick成功样也实际提交并调用正常finishResearch取得成果。
- 这证明服务端实物/任务路径，不代表客户端键鼠或完整正式包生存路线。BE NBT重建不冒充真实区块卸载、独立进程重启或崩溃恢复验证。
- [m3a-compile.log](../build/m3a-compile.log) 初次编译通过；[m3a-validation.log](../build/m3a-validation.log) 首轮21项中原18项过、3个新用例因不可变列表contains/indexOf(null)抛异常，已显式处理尚未选择目标。随后使用独立批次和足够大的trial_kiln_empty结构，最终全过；未隐去失败。
- JSON、模型父引用/四phase变体、配方配比、3份fixture与示例相等、git diff --check 均通过。日志含M0/M1/M2已知依赖/客户端类/配方告警，不将通过等同正式整合包无告警。
- Companion仓未找到AGENTS.md；只读检索TWR KubeJS无同名小炉ID。TWR分支 codex/research-261108，无本轮修改，原 staged config/.gitignore 与 .workbuddy/ 保留。design/总计划/coordination均未由本实现者修改。Git操作全部只读，无暂存/提交/推送/重置/切分支。

## Exact M3-A file list

仅本节点文件（不含协调文档或既有无关未跟踪文件）：

- `diary/2026-11-12_14-53-36_research-m3a-trial-kiln-sample-handoff.md`
- `docs/knowledge/README.md`
- `docs/knowledge/definitions.md`
- `docs/knowledge/example-datapack/README.md`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/ideas/trial_kiln.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/projects/trial_kiln.json`
- `docs/knowledge/example-datapack/data/example/frostedresearch/knowledge/results/trial_kiln_finding.json`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/trial-kilns-and-samples.md`
- `docs/knowledge/workbench.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/TrialKilnGameTests.java`
- `src/gametest/resources/m3-fixtures/ideas/trial_kiln.json`
- `src/gametest/resources/m3-fixtures/projects/trial_kiln.json`
- `src/gametest/resources/m3-fixtures/results/trial_kiln_finding.json`
- `src/gametest/resources/trial_kiln_empty.snbt`
- `src/main/java/com/teammoeg/frostedresearch/FRContents.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeJournalData.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectRequirement.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/ResearchSampleBinding.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TrialKilnBlock.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/experiment/TrialKilnBlockEntity.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeState.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/state/ResearchHistory.java`
- `src/main/resources/assets/frostedresearch/blockstates/trial_kiln_balanced.json`
- `src/main/resources/assets/frostedresearch/blockstates/trial_kiln_clay_rich.json`
- `src/main/resources/assets/frostedresearch/blockstates/trial_kiln_sand_rich.json`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_balanced_bad.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_balanced_burning.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_balanced_good.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_balanced_raw.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_base.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_clay_rich_bad.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_clay_rich_burning.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_clay_rich_good.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_clay_rich_raw.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_sand_rich_bad.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_sand_rich_burning.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_sand_rich_good.json`
- `src/main/resources/assets/frostedresearch/models/block/trial_kiln_sand_rich_raw.json`
- `src/main/resources/assets/frostedresearch/models/item/cracked_trial_kiln.json`
- `src/main/resources/assets/frostedresearch/models/item/fired_trial_kiln.json`
- `src/main/resources/assets/frostedresearch/models/item/trial_kiln_balanced.json`
- `src/main/resources/assets/frostedresearch/models/item/trial_kiln_clay_rich.json`
- `src/main/resources/assets/frostedresearch/models/item/trial_kiln_sand_rich.json`
- `src/main/resources/data/frostedresearch/recipes/trial_kiln_balanced.json`
- `src/main/resources/data/frostedresearch/recipes/trial_kiln_clay_rich.json`
- `src/main/resources/data/frostedresearch/recipes/trial_kiln_sand_rich.json`

## Remaining / handoff

- 主协调复核并统一提交本节点，再分派 M3-B 正式内容/旧进度桥接；本实现者未提交。
- 客户端画面/操作、小窗口与样品槽交互、多人分工、真实区块卸载/独立进程重启、正式 TWR 生存材料路线未验。没有启动Prism或部署。
- 正式两成果 Design/Construction、旧alloy_kiln映射和IE配方/成型权限完全留给M3-B；M4风箱独立试吹/阶段/临时权限未提前实现。
