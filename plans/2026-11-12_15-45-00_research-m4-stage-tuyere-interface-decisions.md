# M4-A：有序阶段、风道游戏与鼓风口试验接口决定

- Time: `2026-11-12 15:45:00 +08:00`
- Authors: `Codex / OpenAI，M4-A 只读接口调查`
- Status: `completed`（设计交付，待 Sol 实施；不是已实现功能）
- Scope: 最小多阶段执行、可选任务/知识路径、风道小游戏、临时配方、独立原型试吹与同队本次成果观察、旧 iron 衔接。
- Related: [开发计划 M4/M5/M6](2026-11-11_23-38-07_research-system-playable-development-plan.md)、[最新架构摘要](2026-11-11_22-20-43_research-system-architecture-revision.md)、[M3-B 交接](../diary/2026-11-12_15-29-57_research-m3b-legacy-bridge-handoff.md)。

## 1. 核对结果与采用默认

以下源码锚点相对 `src/main/java/com/teammoeg/frostedresearch/`。本次未构建/测试、未执行 Git、未改既有文件或发其他对话消息。M3-B 27项服务端通过来自交接，不代表本设计已验证；源码正在由主协调集成，Sol 开始前复核具体调用签名。

| 当前实现/数据 | M4 必须处理的真实差异 |
|---|---|
| ProjectDefinition.requirements；KnowledgeService.requirements/openRequirement | 仍是平铺任务；各执行入口（实验台、M3 样品、计算）都需统一只接受当前选定阶段的任务。 |
| ResearchHistory、KnowledgeState | 现为 schema **4**，origins 与 calculations/evidence/experiments/samples 已持久化；从4扩展，不能按旧总计划误认为仍是2。 |
| DrawingDeskTileEntity.setCalculation/updateGame；ResearchGame | 已有 run+requirement 定位、服务端通关回调、持久桌面状态；现有纸牌需纸笔且是配对游戏，不能改名冒充风道连通游戏。 |
| ObservationSessions.tick 完成采样处 | M2 已在 preview/receive 之前 emit OBSERVATION_COMPLETED；可相邻调用独立任务观察接口。入档/重复记录处理不能决定观察任务成功。 |
| AccessSource、TechnologyAccessResolver | 只有 ResultSource/LegacySource，没有临时研究来源；未列为 managed 的配方默认可用，必须同时登记临时配方的全局受管集合。 |
| LegacyResearchBridge.problem/reconcile/isPrerequisiteCompleted | 当前硬编码两项不同结果、一 Design 一 Construction；effects 仍为 nonce→单结果 ID，真实完成+映射结果有效才解旧父项。 |
| TWR config/fhresearches/iron.json | 规范旧 ID `iron`；唯一效果 nonce **`c520be3259914225`**，只按产物解锁 `charcoal_pit:main_bloomery`。无 Construction 效果。 |

**依赖实际核对：** 对 Gradle 缓存中当前 `curse.maven:charcoal-pit-reloaded-534602:7748097` mapped JAR 做了只读 `javap -p -c`，并直接读取 JAR JSON：

- `BlockBellows.use` 仅安排约10 tick后的服务端 tick；tick 压下时才调用 `blow(Level, BlockPos, BlockState)`。blow 用风箱 FACING 找前方一格，之后才检查 `tuyere_blocks`、扫描相邻非 DUMMY Bloomery，并调用 `TileBloomery.blow(140/count)`。实际试吹应接在 **blow HEAD，原生 Bloomery 检查之前**，不接 use，也不等 TileBloomery。
- `ModBlockRegistry.TuyereSandy` 是 `new Block(Properties.copy(SandyBrick))`；`sandy_tuyere` blockstates 只有空 variant。它**没有朝向属性、没有 BE**。真实朝向来自风箱；转化目标用正常鼓风口 defaultBlockState，勿对它 setValue(FACING) 或硬塞自定义 BE。
- `BlockMechanicalBellows extends BlockBellows`；首版可接受它实际调用同一 blow 的物理动作，不增加“只认玩家右键”限制。最终需在加载真实依赖的环境验其 tick 调用，不能仅据继承关系宣称已实测。
- TWR `kubejs/data/charcoal_pit/tags/blocks/tuyere_blocks.json` replace=true，仅正常 sandy_tuyere；原型在前置专用回调中识别，**不必加入原生 tuyere_blocks**。正常鼓风口仍走原供气路径。
- TWR `recipes/shaped/new.js` 的 main_bloomery 用4砂浆+4青铜板+1砂砖方块；`recipes/shapeless.js` 的 sandy_tuyere 用1砂砖方块+1青铜板+1砂浆。普通模组还有 conditional `sandy_tuyere`/`sandy_tuyere2` 两个 shaped 配方，发布时必须核对并统一处理，不能只锁新增 shapeless 路线。

采用默认：两阶段，游戏通关→手动进入试验→试吹并由绑定队伍任意当前成员新鲜原地观察→手动完成。游戏无表现评分、无耐久质量；原型独立试吹，不需要任何 Bloomery。同队可分工通关/制作/放置/吹气/观察；只要求观察对象属于该队当前run的试验产物，不要求观察者是放置者。

## 2. 最小 schema：有序阶段、固定主干与变体

ProjectDefinition 新增可选 `stages`，与非空顶层 requirements 不同时使用。无 stages 的旧定义内存适配为 `main` 单阶段，原任务 ID/进度保持原样。

```text
StageDefinition {
  id, title, objective, method, conclusion: String
  tasks: List<{ requirement: ProjectRequirement, optional: boolean=false, method: String="" }>
  variants: List<{ id, when?: { knowledge_all: List<KnowledgeCondition>=[],
                              tasks_all: List<先前阶段可选任务ID>=[] },
                   omit_tasks: List<本阶段任务ID>=[], method?: String }>=[]
  completion_recipes: List<RecipeID>=[]
}
```

- 阶段按数组顺序；没有 next 边、任意跳转、循环或并行阶段。任务 ID **项目内全局唯一**，沿用现有 progress map 键，勿用数组下标或改写旧 ID。StageTask 包装负责可选性/方法说明，不往已很长的 ProjectRequirement 塞更多元数据。
- variants 按作者顺序选首个满足项；有 when 时 knowledge_all 与 tasks_all 都须满足；省略 when 的最后一项是默认路径。无 variants 则为 `default`，不增加额外条件。匹配知识只认有效入档；tasks_all 只引用已进入/完成的先前阶段的可选任务，读取已记录成功事实。
- 变体仅允许省去本阶段部分任务、替换方法/提示；任务不新增不同奖励，最终 results 始终项目共用。基础路径必须可独立完成；仅用于后续分支的额外知识不放进 access.start_all。这不同于想法获取时的非必要知识：后者按通常项目规则仍可且本项目必须列入开始条件，见第7节。空任务变体可直接就绪，但仍需玩家推进。
- 进入阶段时持久化 variant ID 与选定任务 ID 集合，之后不重选；方法文字通过已选 variant 查当前定义。中途获得知识只参与未来阶段进入；可选任务可在阶段就绪后继续做，推进后不再补做前阶段任务。
- `completion_recipes` 表示**该阶段必需任务首次全部完成时**开放，到本次 run 结束为止，不随离开该阶段撤销。这样通关即开放原型配方，不强迫玩家先按“下一阶段”才算游戏成功。
- 全部阶段/变体/可选任务的观察用途在定义加载时登记，包含 knowledge_all 与任务观察谓词；不等阶段进入才允许观察入档。复用 KnowledgeCondition→单槽用途，旧独立多证据 slots/distinct 规则不变。

新增任务仅两类，保留 EVIDENCE/CALCULATION/EXPERIMENT/SAMPLE：

```text
MINIGAME: { id, type:"minigame", title, game:"frostedresearch:airflow" }
OBSERVATION: { id, type:"observation", title,
               observation:{ condition:KnowledgeCondition,
                             experiment?: ResourceLocation,
                             team_current_run:boolean=false } }
```

MINIGAME 首版固定一次通关，不使用 level/count 当质量。OBSERVATION 在新鲜采样完成时判断；指定 experiment+team_current_run 时额外核对队伍当前run的试验关联。普通知识、旧档案或别人分享的观察都不触发任务，同队成员现场新鲜观察可以触发。两类成功用持久 `completed_tasks:Set<String>` 表达，重复成功幂等。研究对象“试吹→观察”是一项专用试验链，不为它新增通用事件 DSL。

## 3. 状态/API/保存

ResearchHistory 新增一个嵌套 `stage_state`（避免把 codec 参数无限摊开）：

```text
StageRunState {
  order: List<StageID>, current: StageID,
  visits: Map<StageID,{variant, tasks:List<TaskID>, ready:boolean,
                       temporary_recipes:List<RecipeID>}>,
  completed_tasks: Set<TaskID>
}
```

order 在 start 时保存；visits 只存已进入阶段。旧 progress maps 继续保存计算/证据/实验/样品，新增两类用 completed_tasks；历史结论由 ready 的 visit 与定义关联。临时配方在首次 ready 时记一次，不持久化完整定义或完整知识快照。

```text
startResearch(project) -> 创建run并选择首阶段
currentRequirements(run) -> 当前visit.tasks对应的任务，供所有设备/UI使用
recordMinigameWin(run, task, game) -> 由服务端游戏完成调用
observeResearchTarget(player, Observation, liveTarget) -> 新鲜采样任务判定
advanceStage(run, expectedStage) -> 仅ready时进入下一阶段并冻结变体
finishResearch(run) -> 仅最后阶段ready，产出项目results一次
```

每次任务提交后同次服务端操作更新任务进度、检查必需项、置 ready 和登记 completion_recipes，再一次 notifyChanged。ready 是本次已完成阶段工作的事实，不因后来忘记参考知识回退；任务可选项不影响 ready。阶段就绪不自动前进；最后阶段按钮为“完成研究”。并发请求携带 expectedStage，重复包不得连跳两阶段；所有实验/小游戏完成回调还须检查当前 task/run，旧阶段回调不推进新阶段。

放弃：沿用旧进度清除语义，清本 run 的临时授权/未完成试验关联；完成：保留历史与结论，临时授权失效，成果按现有 produce/learn；旧完成承接的 legacy_superseded run 亦不再产生临时授权或接受任务。关闭页面、离线、区块卸载不改变阶段或授权；重开是新 UUID、新分支选择。

schema **4→5**：旧平铺 run 设 order=[main]、current=main、variant=default，保留所有旧 maps/状态/样品绑定。尚未安装定义时延后补 visit.tasks，不抹去数据；旧已完成/放弃不重发奖励。仍进行的旧平铺项目若被作者直接改为多阶段，默认把旧任务只映射到同 ID 首阶段，不能凭进度百分比跳阶段，写清数据升级诊断。已进入阶段/任务 ID 被移除时保留记录、提示恢复定义；恢复后继续，禁止自动选新路径/越过缺失任务。阶段重排不改变已保存 order；未进入阶段的变体在实际进入时取当前定义。

UI 快照增加当前阶段说明/目标/方法、所选任务的必需/可选标志、ready/可推进状态和已就绪阶段结论；未进入分支不发送其条件/具体任务变体。项目入口仍遵守 M1 逐项未知占位规则。当前临时配方显示可制作到本次研究结束；队友同步后可共同推进。旧单阶段继续原按钮，不为 M3 小炉额外多点一次推进。

## 4. 风道小游戏：复用任务定位，不替换纸牌玩法

新增小型 AirflowGame + CUI 面板，复用绘图台容器、玩家所在桌面、run/task 选择和服务端完成回调模式；保留 ResearchGame 和计算任务行为。桌面游戏目标扩展为 run/task/game，或保留原 calculation 字段旁新增专用 airflow session，优先后者避免改纸牌存档。不可让风道胜利误走 recordCalculation 或旧 ResearchHooks.commitGameLevel。

首版固定 **3×3** 旋转管道盘：西侧入口在(0,1)，东侧出口在(2,1)。可旋转五格的解为 `(0,1) EW → (1,1) WN → (1,0) SE → (2,0) SW → (2,1) NE`；其余四格为墙，坐标从左上0开始。从这份可解盘随机旋转五格，不随机生成不可解拓扑。点击旋转90°，服务端检查相邻端口对接、入口连到出口即通关；无步数/时限/评分，重试无限。UI 显示流向与成功提示，不宣称物理气流仿真。

首版风道免纸笔消耗（纸牌仍按原规则），避免额外材料门槛；桌面会话保存 run/task、五格朝向，关闭重开可继续，重置仅重置盘面。只传旋转/重置动作，服务端持有盘面并判通关；不是客户端直接发“已赢”。完成写一次任务成功，阶段ready和临时配方随之更新；不另授质量/奖励物品。任务已完成后可展示结果，不反复刷计数。

## 5. 临时配方：按run来源并集

AccessSource 增加 `TemporaryResearchSource(project, run, stage)` 与 codec；TechnologyAccessResolver：

1. 把全部已加载项目 completion_recipes 加入 managedRecipes，即使没有任何玩家开始研究，原型配方也不公开。运行中已记录的 grant 需与当前可用定义/实际配方核对；内容暂缺时保留记录，恢复后重建，不趁缺定义放开无来源配方。
2. 对 open、非legacy_superseded、定义可用的 run，把 ready visit 中记下的 recipe ID 加入允许来源。与永久 ResultSource、LegacySource 和别的 run 来源取并集；结束A只移除A的来源，不操作全局解锁列表。
3. ready/推进/结束/成果学习/队伍变化/重载通过既有 TeamResearchService.sync 与定义 revision 刷新投影；不每tick扫描全目录。JEI、合成取出、机器消费沿用同一 technology projection。

M4 原型是专用方块配方，最终正式鼓风口是另一配方：临时原型在完成后不再可造，正式成果接管生产鼓风口与冶铁炉。若成果因事件取消留在来件，项目已结束即撤临时来源，UI 指引学习成果；不要暗中永久保留原型授权。已造出的原型/正常鼓风口不删除；结束后的原型不能为新run提交，但当前新run可重新放置绑定并试吹。

数据包卸载时，其原型 recipe 应随包移除；若 recipe 仍在而项目定义缺失，须避免 unmanaged 默认开放：最小做法是 FH 对专用 prototype_tuyere 配方目标保留受管声明（按产物识别该专用试验物品），无有效run来源时不可造。不把所有配方都改成默认锁定。

## 6. 原型→真实普通鼓风口→同队本次成果观察

建议注册 `frostedresearch:prototype_tuyere` 方块/BlockItem+小型 BE。TWR 配方 `the_winter_rescue:research/prototype_tuyere`：1 `charcoal_pit:sandy_brick` + 1 `#forge:plates/bronze` + 1 `frostedheart:mortar` →1原型，复用现有普通鼓风口材料，可调整。它只需放在风箱 FACING 前方一格，无炉体、不要求燃料/压力阈值。

放置时若存在当前可接试吹的任务，按放置者当时所属队伍绑定 `team/run/task/experiment`，复用 M3 ResearchSampleBinding 的队伍/run/task/experiment概念，不保存个人归属。不在合成时绑定，避免自动合成/堆叠归属混乱；无适用run也可放置但提示未绑定，不完成研究。多候选任务提供目标选择（可复用小炉选择方式）；本项目每队一个open run，正常只有一个候选。

在可选依赖 mixin 的 BlockBellows.blow HEAD 调 `tryBlowPrototype(ServerLevel, bellowsPos, state)`：按 FACING 取前方；只处理已绑定、队伍run及任务仍有效的原型。不检查原放置者在线状态或后来所属队伍；不需要本次吹气操作者身份，因为延迟 tick 本来不携带玩家。正常/未绑定对象继续原逻辑；处理成功返回并取消本次原生吹炉逻辑，避免同一次动作顺带给相邻真实炉加气。第一次转化后已不是原型，后续动作自然按正常模组规则运行。外部模组缺席时 compat 不加载，不能使 FH 基础启动失败。

**对象关联的最小存储：** 正常鼓风口没有BE，采用专用每维度 `SavedData` 的 pending proof 表，key=方块坐标，value=`team,run,task,experiment`。原型成功替换为真实 `charcoal_pit:sandy_tuyere` 后写proof，不给所有正常鼓风口加BE。proof只服务尚未完成观察的任务，不存完整观察/方块实体。写入与替换在同一服务端动作完成并标脏；替换失败不写成功。

- 观察完成处直接调用任务接口，要求新鲜 **block** 采样的维度/坐标命中proof，现场仍是正常鼓风口，观察者当前队伍=team、run/task仍当前有效；任意当前队员均可成功，记 completed_tasks 并移除proof。无需把观察保留到来件或入档。只拿手里普通鼓风口、旧笔记、转交已有观察记录或别的坐标都不算。
- 拆除/替换/活塞搬移会使位置proof失效，不把它转移到普通鼓风口物品。建议在服务端实际方块状态变化接点，对**已有proof的位置**作常数时间检查，变化时移除；不要仅监听玩家 BreakEvent（爆炸/其他替换也会发生）。单次转化写proof在setBlock成功之后；区块卸载不视为方块变化。用目标位置索引，不扫描全世界/所有队伍。
- 若成功对象在观察前丢失，原型配方仍在，重做/重放/再试吹即可；无需重玩已通关游戏。同一team/run/task跨维度只保留最近成功试件的待观察proof，新的成功转化后回收旧proof，不在失败尝试时清除原有效试件。多人可分工制作和观察，任意当前队员观察最近试件即可完成队伍任务，完成后清该task全部proof。
- 放弃/旧研究承接/完成清本run的pending proof；观察者换队后不能替旧队提交，但原放置者退出或换队不影响剩余队员观察现有试件。短暂退出不清proof，重登同队可继续。跨维度保存与重启需实际验；proof关联丢失时通过可重制恢复，不伪造观察成功。

“风箱试吹成功”以物理 blow 动作和原型转化共同成立；不把 use 点击、手持观察或直接入档成果视为试吹。状态只用于玩法对象关联，不引入签名、路径哈希或额外安全系统。

## 7. 冶铁炉内容骨架与旧 iron 补偿

建议项目 `the_winter_rescue:iron_bloomery`、想法 `the_winter_rescue:iron_smelting`（若已有同义定义先复用）、最终 **一个** Design `the_winter_rescue:iron_bloomery_design`，recipes 包含固定的 `the_winter_rescue:research/main_bloomery` 与 `the_winter_rescue:research/sandy_tuyere`。保持两条原材料/产量，消除所有绕过正式目标的同产物旧配方；需在实际 RecipeManager 中确认，不按文件名推定已生效。

入口沿M2获取铁矿/铁制品，想法 acquisition.required=[]；铜/青铜任一种观察、煤/木炭/焦炭任一种观察是 acquisition.optional 中的两个条件。项目按通常规则要求想法及其全部前置知识，含获取想法时非必要的知识：**start_all=[idea, copper_or_bronze, carbon_fuel]**。两组各需一份有效入档观察；只在来件不算。作者例外仍由M1显式配置表达，但本冶铁项目不采用省略例外。正常项目不强行增加分支，可选任务/知识分支用针对性 fixture 验证，M5再用于温度提示。

已核对当前 `KnowledgeCondition.CODEC`、`FieldCondition.TAG` 与 `TagLookup.SERVER`：支持 `observation_type:"item"` + `{field:"object",operator:"tag",tag:"..."}`，按物品标签成员匹配。conditions数组本身是AND，**组内OR用一个合并物品标签**，两组之间用start_all的AND，不新增通用OR节点。TWR新增 `tags/items/research/iron_copper_or_bronze.json`，首版values为 `#forge:ingots/copper`、`#forge:ingots/bronze`；新增 `tags/items/research/iron_carbon_fuel.json`，values为 `minecraft:coal`、`minecraft:charcoal`、`immersiveengineering:coal_coke`。首版铜/青铜以锭形态作为明确观察样本；青铜tag的FH生成数据含frostedheart:bronze_ingot，焦炭ID在TWR现有Ponder中有引用，实际加载后的tag成员由实施验收核对。两份相同观察谓词分别放入想法 acquisition.optional 与下方项目conditions；想法入档后披露三项要求，获取想法仍不要求先满足两组观察。

项目 access 与 stages 核心片段（沿用结果定义；其中两组观察谓词也用于想法 acquisition.optional）：

```json
{
  "access": {
    "conditions": {
      "idea": {"kind": "idea", "definition": "the_winter_rescue:iron_smelting"},
      "copper_or_bronze": {"kind": "observation", "observation_type": "item", "title": "观察铜或青铜", "conditions": [{"field": "object", "operator": "tag", "tag": "the_winter_rescue:research/iron_copper_or_bronze"}]},
      "carbon_fuel": {"kind": "observation", "observation_type": "item", "title": "观察煤、木炭或焦炭", "conditions": [{"field": "object", "operator": "tag", "tag": "the_winter_rescue:research/iron_carbon_fuel"}]}
    },
    "start_all": ["idea", "copper_or_bronze", "carbon_fuel"],
    "groups": [{"id": "from_idea", "reveal_any": ["idea"], "conditions": ["idea", "copper_or_bronze", "carbon_fuel"]}]
  },
  "stages": [
    {
      "id": "airflow", "title": "规划风道", "objective": "连通入口与出口",
      "method": "在绘图台旋转管道，使气流抵达出口。", "conclusion": "连续风道能将空气送至出口。",
      "tasks": [{"requirement": {"id": "airflow_win", "type": "minigame", "title": "连通风道", "game": "frostedresearch:airflow"}}],
      "completion_recipes": ["the_winter_rescue:research/prototype_tuyere"]
    },
    {
      "id": "trial", "title": "试吹鼓风口", "objective": "观察本队本次研究已完成试吹的鼓风口",
      "method": "制作原型，进入本阶段后放在风箱出风侧；吹气使其转为普通鼓风口，再原地观察。不需要冶铁炉。",
      "conclusion": "鼓风口已通过供气验证，可以建设冶铁炉。",
      "tasks": [{"requirement": {"id": "observe_tuyere", "type": "observation", "title": "观察本次试吹成果",
        "observation": {"experiment": "frostedresearch:tuyere_trial", "team_current_run": true,
          "condition": {"kind": "observation", "observation_type": "block", "conditions": [
            {"field": "object", "operator": "eq", "value": {"state": "known", "type": "string", "text": "charcoal_pit:sandy_tuyere"}}
          ]}}}}]
    }
  ],
  "legacy_replacement": {"research": "iron", "effects": {"c520be3259914225": "the_winter_rescue:iron_bloomery_design"}, "partial_idea": "the_winter_rescue:iron_smelting"}
}
```

游戏通关至手动进入 trial 之间原型已可制作；提前放置的原型可在进入trial后重新放置绑定，不在前阶段偷记试验。首版不另建“预绑定未来阶段”系统。

**Bridge 最小扩展只有校验规则：** `LegacyResearchBridge.problem` 改为非空 effects，目标须属于项目 results，目标允许 Design 或 Construction，可只有一种类型；每个目标逐项验证启用/空understanding/配方或多方块存在，去掉“恰好两项、一种各一”的限制。保留 Map<nonce,ResourceLocation>，不需要一对多字段：一个 Design 自身即可装两条配方。M3 的两映射原样通过；`reconcile`/回执/父项 allMatch 本来可遍历一项，核对并移除其余大小假设即可。

旧 iron finished 或已领 `c520be3259914225` 的玩家获得这个双配方 Design，明确补偿新增的正式鼓风口配方锁，不重玩风道、不做试吹、不造新run。仅 finished 授 completed_credit，沿M3规则；只有单效果未finished者已有两配方权限但不冒充项目完成。部分旧进度只承接想法，保留原投入。正常鼓风口此前可公开制作、现在纳入最终成果是本次明确玩法变化：已有实物不删除，未完成iron的新队伍走原型试吹；旧完成玩家通过上述补偿保持生产能力。

## 8. 实施顺序/边界/必要验收

1. **阶段核心**：ProjectDefinition、新StageDefinition/StageTask/Variant、ResearchHistory/KnowledgeState、KnowledgeService统一当前任务与推进；完整保留M1–M3B接口/回执；更新KnowledgeDefinitions观察用途。先用纯服务端fixture验证默认路径/可选分支及schema4存档。
2. **权限与界面**：AccessSource、TechnologyAccessResolver、KnowledgeView/JournalData/Layer、WorkbenchActionPacket加ADVANCE_STAGE与阶段字段；所有实验台/小炉/桌面取任务改用同一currentRequirements。维护 docs/knowledge 及新diary，不能靠旧UI只显示第一阶段蒙混过关。
3. **游戏**：DrawingDeskTileEntity与现有DrawDeskContainer/面板路由旁接AirflowGame、旋转/重置包；保留纸牌旧目标与存档。风道参数只在该实现集中，不泛化任意棋盘语言。
4. **原型与compat**：注册专用block/BE/item/模型语言、pending proof SavedData、新鲜采样任务入口、Charcoal Pit可选mixin及实际状态变更清理。读取当前 FrostedHeartMixinPlugin/研究mixins配置按既有风格接，不硬依赖客户端runtimeOnly的类；GameTest现有配置不含Charcoal Pit服务端，集成验收需专门有真实依赖的服务器测试入口。
5. **正式数据与bridge**：TWR iron新定义/临时与永久固定配方/引导、旧iron保留兼容；FH只放机制与测试fixture。桥接校验小扩展之后跑M3双成果与M4单成果回归。每仓分别记录验证，禁止顺手实施其余81项迁移。

必须覆盖：

- 旧单阶段evidence/calculation/experiment/sample无行为回归；schema4→5、两次读取、独立进程重启保持阶段/任务/临时来源；未知定义恢复不丢进度。
- 开始条件：只有想法不能开始；铜或青铜任一个有效观察满足第一组，煤/木炭/焦炭任一个满足第二组，两组均满足才可开始；来件不算。分别覆盖两组各替代项与不在tag的物品。获取想法时两组为optional，缺观察不阻止获取；入档想法后项目三项要求完整披露。
- A阶段就绪前/后补知识与可选任务，在手动进入B时选正确变体；进入B后补知识不改B，进入C可用；多个命中按顺序，默认路径可玩，omit_tasks确实减少必需项；重开重新选择。
- ready不自动推进；双人重复advance不连跳，旧游戏/实验完成回调不能交到后阶段；无任务变体、最后阶段手动完成幂等。
- 风道真实旋转求解、退出重进/重试、非通路不算通关；无需评分；纸牌仍需原纸笔且正常计数。自动验证与真实鼠标/窗口布局分别记录。
- 通关立即开放原型；离线/跨维度保留；runA结束不撤runB或永久来源；放弃/legacy_superseded撤自身；无人有来源时原型配方不能因unmanaged放开；JEI与真实合成/机器一致。
- **真实Charcoal Pit**：无任何Bloomery的世界，正常风箱实际延迟blow一次使原型转为真实sandy_tuyere；use但未到blow不算，方向不对不算；普通鼓风口和已有炉正常供气不受影响。验证机械风箱时另记证据。
- A队员放置、B队员吹气、C队员新鲜原地观察可完成；A退出或换队后C仍可观察现有试件。外队、旧run、无试吹关联的普通鼓风口、物品观察、笔记都不算；同team/run/task只保留最近成功试件，跨维度新成功试件替换旧proof；相同观察已入档/拒绝保留/来件容量不影响任务动作；方块拆换/爆炸/活塞、区块卸载与重启分别验；丢失可重制且不用重玩。
- 旧iron一Design补齐两配方，旧alloy_kiln两类型仍正常；真实新完成+有效成果解旧父项，外部成果不造完成历史；新包缺席旧入口可用，残留配置不形成双路线。

不实现通用阶段图引擎、质量评分、真实流体模拟、临时物品销毁、把正常鼓风口替换为自定义“看似正常”方块、全局观察历史扫描或任意奖励事务框架。以上默认可直接实施；无须等待常规参数确认。

修订记录（2026-11-12，主协调实施决定）：项目开始改为想法＋铜/青铜观察组＋煤/木炭/焦炭观察组；获取想法的optional与项目required分开配置。试验只绑定team/run/task/experiment，允许绑定队伍任意当前成员观察，移除个人归属与放置者身份限制。实际blow接点和阶段冻结规则不变。
