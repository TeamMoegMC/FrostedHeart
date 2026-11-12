# M3-B FH 旧研究承接与正式内容接口交接

- Time: `2026-11-12 15:29:57 +08:00`
- Author: `Codex / OpenAI；M3-B FH 实现与验证者`
- Status: `completed`（本阶段服务端实现、编译与自动验证；正式包客户端联调另行组织）
- Scope: FH 旧 alloy_kiln 到数据包新项目的桥接、回执、父项投影、旧入口和知识界面；基线 a565d4180 / research-261108。

## Completed

- 按 [Astra 最小衔接决定](../plans/2026-11-12_14-00-00_research-m3-legacy-alloy-kiln-bridge.md) 增加可选 ProjectDefinition.legacy_replacement；字段 research/effects/partial_idea 原样采用，没有更换 schema。
- LegacyResearchBridge 校验两项不同的 Design/Construction、启用状态、空 understanding、实际 RecipeManager 目标和 IE 成型目标、partial_idea；缺项或重复旧 ID 映射时停用替换并给诊断。映射局部缺定义不因整份快照拒绝而留住旧的有效映射。
- 旧 finished 承接两成果，单项已领取效果只承接对应成果；使用正常 learn、来源 legacy_research:<ID>。不重放 grantEffects，不修改 raw finished/effectData，不生成 ResearchHistory。
- variants 下 frostedresearch:legacy_replacements 按项目保存 imported_results/completed_credit/partial_idea_granted；成功/已有档案才记录成果回执，失败生命周期补做，遗忘不强制补回。部分 active/点数/clue 一次 produce 想法，保留所有旧投入；不伪造新实验。
- 新真实 completed run + 两有效成果等价满足旧父项；外来两成果仅给权限。新成果学习/遗忘刷新技术权限和旧父项；真实旧 finished + 对应 effectData 仍独立提供允许来源。
- commitItem、setCurrentResearch、doResearch、两种 setClueCompleted、checkResearchComplete 和 canResearch 拦截替换项；清理旧当前选择/线索订阅，保存原 active/点数/clue。旧定义未加载的持久 ID 也可清选择。撤除映射/整个新包后旧进度可续，不做反向迁移。
- 知识页接收 completion_source=legacy / LEGACY_COMPLETED，旧完成无想法也可见且免重复启动；已有 open run 保留，投影 legacy_superseded 并停止新任务提交。旧档案页列表标记接替，详情说明承接状态并可前往知识页；未知项目只给引导，不披露隐藏任务。新快照刷新旧页独立父项缓存。
- KnowledgeService.merge(TeamDataHolder) 和管理 merge 命令同时合并知识与回执并集，仅处理此命名空间子键，不合并其它旧变量/原始进度。
- 最终 OnDatapackSyncEvent 在遍历在线玩家之前全局 refreshDefinitions，零在线同样执行；ServerStarted 刷新最终目标，ServerStopped 清静态缓存。队伍加载/登录继续 reconcile；没有逐 tick 重试或额外缓存框架。
- 更新知识 definitions/state-and-api/workbench、旧研究 state-persistence-and-sync/research-ui 与 GameTest README。没有编辑 sideworker 的 earthen-smelter.md、总计划、coordination 或 design。

## Validation

进程 JAVA_HOME=`C:/Program Files/Java/jdk-17.0.2`，串行构建；没有启动客户端、Prism、部署或关闭用户进程。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-PgameTestNamespaces=frostedresearch_m3b,frostedresearch_m3,frostedresearch_m2,frostedresearch_m1,frostedresearch,frostedresearch_knowledge_login,frostedresearch_observation' '-PgameTestDirectory=build/m3b-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

- 最终 [m3b-validation-2.log](../build/m3b-validation-2.log)：**BUILD SUCCESSFUL，6m7s，27 个 required GameTest 全通过**，生产与 GameTest 完整编译。包含原 21 项回归（M1 未知占位、M2 手动回环/自动去重、M3-A 三配比/真实样品/schema4）及 6 项独立 batch 的桥接测试。
- 六组：①旧完成/单效果/部分/新队伍、单结果事件取消与下次补发、重复初始化与遗忘、无伪造历史、真实队伍 CODEC 回执往返；②真实 startResearch/recordCalculation/finishResearch，成果来件不解父项、学习后解、遗忘撤新来源、raw 旧来源与新来源并集、外来成果不冒充完成；③原已选旧研究被清、激活/选择/直接投点/两种真实 ItemClue 完成入口不能绕行、缺结果/撤映射/全新包缺席恢复；④队伍知识与回执集合/布尔并集、既有 open run 留存且不再推进；⑤五份正式 TWR JSON 的真实 Codec/引用检查、固定配方 ID、IE 实际成型注册与 M3-A sample 契约；⑥零在线服务器真实 RecipeManager 撤出/恢复目标，知识 revision 不变时最终 reload 回调与 ServerStarted 回调刷新映射、静态 clear 后可重建。
- 首轮 [m3b-validation.log](../build/m3b-validation.log)：25 项全过，6m22s；之后加入正式内容与生命周期测试、完善真实线索和 UI 提示，最终第二轮全过。最初 [m3b-compile.log](../build/m3b-compile.log) 生产编译也通过，2m29s。通过后没有再改生产或测试源码。
- 只读核对 TWR 的旧 ID/两 nonce、正式映射、两成果、sample、获取入口标签及 change.js 中稳定 ID。五份 formal fixtures 与当前 TWR 正式 JSON 逐字节一致，引用标签文件存在；FH 两语言 JSON 与本阶段 git diff --check 通过。
- 自动验证运行的 recipe 目标为 vanilla crafting_table，形成目标为真实 IE alloy_smelter；**没有声称正式 KubeJS RecipeManager 已注册 the_winter_rescue:research/alloybrick**。正式五份 JSON 只证明 Codec/引用/目标声明一致。
- 日志仍有既有依赖/配方格式、缺客户端类、模型重复、版本检查网络等告警；GameTest 成功不等同完整包无告警。
- 回执 CODEC 往返与正常服务器保存退出不冒充独立进程重启恢复；零在线重载测试直接调用注册生命周期处理方法并替换真实 RecipeManager 目标，不冒充完整资源管理器 /reload 命令或完整包运行。
- Git 仅只读检查，无暂存、提交、推送、切分支、重置；TWR 和 sideworker 文档无本实现者写入。主协调的 coordination 变动与其它未跟踪文件保留。

## Exact M3-B FH file list

- `diary/2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md`
- `docs/knowledge/definitions.md`
- `docs/knowledge/state-and-api.md`
- `docs/knowledge/workbench.md`
- `docs/research/research-ui.md`
- `docs/research/state-persistence-and-sync.md`
- `src/gametest/README.md`
- `src/gametest/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridgeGameTests.java`
- `src/gametest/resources/m3b-formal-fixtures/ideas/earthen_smelting.json`
- `src/gametest/resources/m3b-formal-fixtures/links/earthen_smelting.json`
- `src/gametest/resources/m3b-formal-fixtures/projects/earthen_smelter.json`
- `src/gametest/resources/m3b-formal-fixtures/results/earthen_smelter_construction.json`
- `src/gametest/resources/m3b-formal-fixtures/results/earthen_smelter_design.json`
- `src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java`
- `src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchArchiveViewCache.java`
- `src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchProjectSummaryPanel.java`
- `src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchProjectWorkspace.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeCommands.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridge.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeClientState.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/client/LegacyResearchClient.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/LegacyReplacement.java`
- `src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java`
- `src/main/java/com/teammoeg/frostedresearch/research/Research.java`
- `src/main/resources/assets/frostedresearch/lang/en_us.json`
- `src/main/resources/assets/frostedresearch/lang/zh_cn.json`

## Remaining

- 主协调统一审阅与 git add/commit；本实现者未提交。保持现有 TWR 字段与稳定 ID。
- 正式包 RecipeManager 的炉砖目标覆盖、IE 权限实际成型、完整生存获取/观察/小炉路线、真实客户端键鼠/隐藏要求展示/窗口布局、多人分工与独立服务器进程重启留给后续集成。未部署或启动 Prism。
