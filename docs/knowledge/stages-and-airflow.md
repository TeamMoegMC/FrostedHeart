# 有序阶段、临时配方与风道试验

- Status: `Current`（粒子调整已通过12项定向GameTest及正式单客户端；阶段/真实风箱既有证据范围见各节）
- Last verified: `2026-11-17`（减半密度、通风前炉腔无粒子、通关后固定路线、循环/重开/重置及客户端显示）
- Scope: 有序研究阶段、变体固定、临时配方、绘图台风道、队伍试吹对象关联；正式整合包路线由 iron-bloomery.md 说明。
- Code anchors: `StageDefinition`、`StageRunState`、`ResearchStages`、`KnowledgeService.advanceStage`、`AirflowGame`、`AirflowParticles`、`AirflowDeskPanel`、`TuyereTrials`、`PendingResearchProofs`。

## 阶段和任务

鼓风口原型的模型外轮廓覆盖完整 16×16×16 方块范围，保留中央风孔；注册使用 `noOcclusion()`，避免镂空模型把相邻地面等方块的面错误遮掉。该外观修正不改变试吹、绑定或碰撞逻辑。

`ProjectDefinition.stages` 与非空顶层 requirements 互斥；旧平铺任务适配为 main 单阶段，不增加推进点击。
阶段包含 id/title/objective/method/conclusion、tasks、variants、completion_recipes。
Task 包装 requirement/optional/method；任务 ID 在项目内唯一，旧 calculation/evidence/experiment/sample 进度键不变。
绘图台 `minigame` 任务由 `game` 选择盘面。`frostedresearch:airflow` 用于风道，`frostedresearch:foldline` 用于[折线图纸校核](authoring-guide.md#折线图纸校核)。`frostedresearch:nesting_pack` 和 `frostedresearch:nesting_fill` 分别用于[布置零件和拼合衬板](authoring-guide.md#方块排样)。每项小游戏通关一次即可完成对应任务。observation 的 observation 包含 condition、可选 experiment、team_current_run。
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

风道使用28×28固定网格：左侧偏下为风入口，上方偏右是预挖炉腔；炭床覆盖炉腔底部截面，顶部设出口。入口旁预留一小段空腔，玩家从那里继续凿开砂砖。入口与出口为蓝色，炭床为橙红色。炭床未通风时轻微闪烁阴燃，提示目标位置。
玩家按住左键拖动挖砂砖，3格宽笔刷沿拖动线挖掘并补齐线段间的采样点。冷空气进入后在连通空腔内游走，尚未经过炭床前可见冷色粒子；经过炭床后粒子转暖色并上升至炉顶。判定先检查冷空气是否能从除入口以外的任意地图外边界漏出，包括绕过炭床从顶部出口漏出。若能漏出，本轮立即失败，即使另有通路经过炭床；盘面暂停并显示红色漏口提示，可无消耗重置。只有不存在冷风漏口后，才判定入口到炭床再到顶部出口的成功路径。无评分或时限。

`AirflowGame.dig(List<Stroke>)` 先完成整批笔段，再统一先判冷风漏口、后判通关，避免同批前半段成功掩盖后半段漏气。未与入口连通的外边界空洞不构成漏风；仅能经炭床到达的其它开口不额外触发冷风失败。入口开口豁免，周围保护格与普通砂砖同色，无单独边框/图例；入口后方初始空腔用于引导继续开凿。

`AirflowParticles` 只负责视觉，不参与服务端漏气、炭床连通或成功判定。粒子目标数按可充气的 `OPEN`/`BED` 空腔面积计算：`target = ceil(area_cells × DENSITY)`，`DENSITY = 0.25 particle/cell`，即每4格约1粒，28×28地图的理论上限 `LIMIT = 196 particles`。通关前，`AirflowGame.furnaceCavity` 标识的炭床及上方预挖炉腔不计入面积，不生成也不接受随机游走的粒子；炭床自身仍阴燃闪烁。入口旁23格初始空腔对应6粒，玩家新挖的独立空腔仍可补生。新开凿后优先从入口或最近开凿处补入，空间受限时选择其它合法空腔点，每tick最多补16粒至目标数。通关后的定向气流纳入炉腔面积，示例风道共45粒。

图上圆点直径1 GUI像素；逻辑碰撞半径 `RADIUS = 0.24 cell`，`CELL = 4 GUI pixels`，对应碰撞直径1.92 GUI像素。粒子按等质量弹性碰撞并修正重叠，砂砖也用同一半径检测。每tick固定2子步、每子步2轮修正，速度上限 `0.45 cell/tick`；用1格空间桶只查相邻桶。渲染在前后tick位置间插值。

未通关时粒子在有效空腔内持续活动，不再按500tick寿命消失；出界或卡入砖格后补回。成功后将粒子均匀分布到 `AirflowGame.path()` 给出的有效通关路线，按入口→炭床→出口的方向，以 `PATH_SPEED = 0.25 cell/tick` 沿相邻格中心移动，停止随机转向和粒间碰撞。经过炭床变为暖色，到出口后回入口重新变冷；回入口时同步前后位置，避免插值画出横穿地图的轨迹。通关展示立即补齐目标数量，重开已通关盘面也恢复此模式。历史已记成功但按新规则无有效路径的存档保留成功，不绘制穿砖的替代路线。

粒子位置和移动模式不入NBT，重置清空并恢复随机运动。炭床未送风仍有微弱闪烁，研究成功时明显增亮。

本轮12项定向GameTest及正式Playtest单客户端检查通过：初始入口空腔6粒且预挖炉腔无粒子，示例通关后45粒沿固定路线持续移动，冷热颜色、漏气失败/重置、盘面重开与返回任务页通过。详见[本轮记录](../../diary/2026-11-17_00-14-33_research-sparser-gas-and-completed-flow.md)。此前密度0.5的版本通过11项定向GameTest及正式Playtest检查，性能与数量证据见[前轮粒子记录](../../diary/2026-11-16_23-39-00_research-volume-particles-and-collisions.md)；其中61/90粒及387粒耗时不代表本版。
绘图台独立保存 airflow_target/airflow_game，不改变旧连连看记录。客户端发送 `DIG_AIRFLOW` 笔段和 `RESET_AIRFLOW` 重置，服务端先判漏气，再判成功并写任务；保留旧 `ROTATE_AIRFLOW` 编号但不执行动作。每批最多32笔段，同一面板一个在途请求，客户端先预览挖掘再按服务端回复重放未确认笔段；离开地图断开笔段，返回或关闭时补发未发送笔段。新 NBT 的 `format=3` 保存 `dug`、`completed` 和 `failed`；读取 format=2 时保留已挖砂砖，旧 rotations 用于重建更早的未完成盘面，已完成研究任务保持完成。关闭重开延续盘面。客户端从知识札记任务按钮切到旧绘图台桌面，由 AirflowDeskPanel 占用连连看原来的区域（165,25,218×164），桌面整体仍为387×203。返回按钮或Esc恢复原项目详情、阶段任务标签和所选记录。小游戏期间不展示旧连连看/旧项目进度控件，桌面三种工具槽停用；玩家背包仍可用。

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

## M4 风道绘制

风道现由 AirflowDeskPanel 在旧桌面右侧绘制28×28网格，每格4 GUI像素、画布112×112。网格区分砂砖、炭床、入口和出口；入口后方预挖小房间，玩家从空腔继续凿开砂砖。实现中入口周边仍有不可挖保护格，但与普通砂砖同色，不绘制额外边框或图例；外边界上的入口开口仍是合法进风口。炭床阴燃提示目标，冷空气粒子在空腔内游走，通关后沿有效风道定向经过炭床并升向炉顶；红色漏口提示失败位置。3格宽菱形笔刷用线段补点减少快速拖动的漏格。UI右侧图例和提示标识地形与目标。地图及粒子均由GUI绘制，不增加贴图。

`DrawDeskLayer.drawBackground` 按当前小游戏切换纸面：连连看使用 `DrawDeskIcons.Background` 原有格子背景；风道用图集中独立的空白羊皮纸 `DrawDeskIcons.DIALOG` 覆盖右侧画布（160,19,215×170），隐藏原来的网格、外框和右侧组合格。木框、左侧纸面和背包保持原布局，不复制或修改 `textures/gui/draw_desk.png`，不改变小游戏规则。

KnowledgeClientState 把风道操作回复转交当前桌面面板；隐藏的札记也会在研究快照更新时刷新，返回后显示服务端真实进度。旧连连看数据不参与风道结算。以下既有11月14日记录验证的是当时旋转管道版本的客户端往返行为，不作为当前挖掘玩法的验证证据。

此前漏气版已通过15项服务端测试及正式Playtest单客户端检查：入口空腔中冷色粒子在到达炭床前活动，绕床接炉顶与直接穿透外边界均失败并可重置；正常通关、43格笔画立即Esc退出补发/重开恢复、临时原型配方和返回任务页通过。人工查看初始、两种失败与成功截图，入口保护无额外框或图例。见[此前记录](../../diary/2026-11-16_22-01-22_research-airflow-cold-leaks-and-smolder.md)。这些旧证据不覆盖后续粒子参数调整；粒子及炭床闪烁为视觉表现，结果由服务端连通判定。客户端为事件驱动和材料/知识夹具，不代表完整生存或物理键鼠验收。

11月16日正式Playtest新测试世界验证的是本轮之前的挖掘规则：实际Screen鼠标事件与C2S通关、退出重开保留挖掘及炭床路径；13项定向服务端测试覆盖当时的地图/存读档与风箱流程。该轮记录的49格挖掘数属于旧初始空腔布局，不代表本版预挖小房间后的保存格数。该轮未测试本轮新增的冷风漏边优先失败、失败暂停/漏口提示和 format=3 迁移，不构成这些规则的验证证据。材料与知识为夹具，非物理键鼠或完整生存验收，详见[此前记录](../../diary/2026-11-16_20-29-00_research-excavated-charcoal-airflow.md)。

11月14日正式Playtest定向客户端检查确认桌面管线/按钮显示、真实C2S通关、重开盘面以及Esc/按钮返回原阶段任务页，见[当轮记录](../../diary/2026-11-14_13-57-10_research-material-families-and-desk-airflow.md)。材料与起始知识为夹具，不代表生存流程。

## M6 合并后的备用记录

所有run入口按ResearchRuns.ORDER（创建时间+UUID）排序。M4原型的既有空手cycle仍选择实际绑定，目标文案附同项目记录序号、阶段和任务，可区分合入的进度。若该项目已有真实完成run且全部成果有效已学，其余open run按统一standby暂停；原阶段/任务/proof事实保留，推进和TemporaryResearchSource停用，遗忘成果后恢复。不同项目的临时来源与永久来源仍取并集。知识merge不重写世界原型或未提交样品的队伍绑定，需重新绑定或制作。
