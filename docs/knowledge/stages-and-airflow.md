# 有序阶段、临时配方与风道试验

- Status: `Current`（源码、38项完整服务端验证、独立进程重启及缺依赖检查；实际客户端待主协调验证）
- Last verified: `2026-11-12`
- Scope: 有序研究阶段、变体固定、临时配方、绘图台风道、队伍试吹对象关联；正式整合包路线由 iron-bloomery.md 说明。
- Code anchors: `StageDefinition`、`StageRunState`、`ResearchStages`、`KnowledgeService.advanceStage`、`AirflowGame`、`TuyereTrials`、`PendingResearchProofs`。

## 阶段和任务

`ProjectDefinition.stages` 与非空顶层 requirements 互斥；旧平铺任务适配为 main 单阶段，不增加推进点击。
阶段包含 id/title/objective/method/conclusion、tasks、variants、completion_recipes。
Task 包装 requirement/optional/method；任务 ID 在项目内唯一，旧 calculation/evidence/experiment/sample 进度键不变。
新类型 minigame 的 game 为 frostedresearch:airflow；observation 的 observation 包含 condition、可选 experiment、team_current_run。
专用试验关联不增加知识记录或 M3 样品字段。

变体按顺序选首个满足项；when.knowledge_all 查询有效入档知识，tasks_all 仅引用先前阶段可选任务的已成功事实。
最后无 when 的项是默认路径。omit_tasks 只能省当前阶段任务，method 可替换说明；结果仍属于整个项目。
进入时保存 variant 和任务集合，中途补知识不重选；已就绪但尚未推进时仍可做可选任务。

每次任务提交统一经过当前阶段定位，更新 ready 与首次 completion_recipes，然后同步。
ready 不自动前进；advanceStage(run,expectedStage) 只从当前就绪阶段前进一次。重复旧包或旧设备回调不能跳阶段。
末阶段手动 finishResearch 统一产出成果。缺失已进入的阶段/任务/变体保留存档，恢复定义后继续，不自动跳过。

## 持久化和权限

KnowledgeState schema_version=5。schema4 记录在副本上补 main 占位 stage_state，定义可用后补任务集合；全部旧进度与样品绑定保留。
StageRunState 保存 order/current/visits/completed_tasks；visit 保存 variant/tasks/ready/temporary_recipes。
已保存 order 不随作者重排，未来阶段变体在真正进入时按当前定义选择。放弃保留 run 身份但清本次进度和授权。

临时来源为 TemporaryResearchSource(project,run,stage)，与永久结果、旧研究和其它 run 取并集。
首次必需任务全部成功就开放 completion_recipes，直至完成、放弃或旧完成承接；离线、关闭页面、区块卸载不撤销。
所有声明配方都受管理，无来源时不可用。专用 prototype_tuyere 产物配方在最终配方重载时另建受管集合，避免项目定义缺席时变成公开配方。
当前有效定义和真实 RecipeManager 均须认可记录的临时授权；缺内容保留历史，恢复后重建。

## 风道与试吹

风道是3×3固定可解管道拓扑，随机旋转五个可操作格；其余为墙。左侧入口到右侧出口连通即成功。
绘图台独立保存 airflow_target/airflow_game，不改变旧纸牌记录。客户端仅发选择、旋转和重置，服务端判通路并写任务。
风道不耗纸笔，无评分、时限或质量；关闭重开延续原盘面。

prototype_tuyere 放置或空手选择任务时绑定 team/run/task/experiment，无个人 owner。多候选须选择。
真实 Charcoal Pit BlockBellows.blow 前置接点按 FACING 查出风侧原型，当前队伍任务仍有效才转为真实 sandy_tuyere。
不需要 Bloomery；正常鼓风口没有朝向和 BE，保持原方块默认状态。未处理对象继续原供气逻辑。

转化成功后在每维度 SavedData 保存位置到绑定的 pending proof；同一任务跨维度保留最近成功对象。
新鲜现场观察直接判任务，不依赖收件/学习/是否重复观察。同队任意当前成员可观察；普通鼓风口、旧run、物品观察或笔记不替代本次试吹。
世界实际方块变化按坐标删除 proof，区块卸载保留；完成/放弃/旧承接清本 run 的关联。丢失对象可重制，不重玩已通关游戏。

真实Charcoal Pit、同队新鲜观察、真实区块卸载与独立进程重启已通过本轮服务端测试；正式包客户端、生存和真实键鼠仍由主协调组织。

## 本地服务端验证入口

普通共享测试为 StageResearchGameTests（frostedresearch_m4）。真实依赖/正式内容测试位于src/m4，只有
-Pm4CharcoalPitTests时加入游戏mod根，不进入生产JAR或M3C helper。该开关仅追加实际Charcoal Pit服务端依赖，
独立source set只继承现有gameTest.runtimeClasspath；绝不把compileClasspath中的Oculus等客户端类库带入服务器。

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-17.0.2'
.\gradlew.bat compileJava compileGameTestJava runGameTestServer '-Pm4CharcoalPitTests' '-PgameTestNamespaces=frostedresearch_m4,frostedresearch_m4_cp' '-PgameTestDirectory=build/m4-gametest-20261112' '-PthermalTestHeap=3G' '-PthermalTestProcessors=4' --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

跨进程验证单独使用frostedresearch_m4_restart命名空间、固定同一build下运行目录，先-Pm4RestartPhase=write，
进程保存退出后再-Pm4RestartPhase=read；读阶段保留先前world。标记文件只存本测试生成的team/run/位置。
每次运行必须确认当轮 GAME TESTS COMPLETE 和 All N required tests passed；Gradle exit0可能只是Minecraft启动失败，不能当测试通过。

正式JSON与三个物品tag是当前TWR内容的逐字节fixture；原型配方也直接复制正式JSON。
另外两条生产配方是按当前TWR KubeJS声明建立的运行fixture，不能据此宣称KubeJS整包脚本已执行。

最终服务端验证与完整文件清单见 [M4交接](../../diary/2026-11-12_17-27-52_research-m4-full-stage-tuyere-handoff.md).
