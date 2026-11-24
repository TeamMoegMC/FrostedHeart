# 编写研究内容

- Status: `Verified`：本页核对当前数据格式与服务端实现；各正式项目的客户端及游玩验证范围见其操作文档。
- Last verified: `2026-11-20`（折线图纸校核；其它流程见各自验证记录）
- Scope: 数据包内容作者的入口与可复制范例，不替代完整字段参考。
- Code anchors: [ProjectDefinition](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectDefinition.java)、[StageDefinition](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/StageDefinition.java)、[IdeaAcquisitionService.emit](../../src/main/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionService.java)。

## 绘图台小游戏的开发称呼

| 开发称呼 | 配置 ID | 判定目标 |
|---|---|---|
| 风道 | `frostedresearch:airflow` | 让冷风经过炭床并从炉顶排出 |
| 转镜子 | `frostedresearch:foldline` | 用限额内的镜片将每条线接到同字母出口 |
| 填满的拼图 | `frostedresearch:nesting_fill` | 铺满整个凹槽，盘内可剩件 |
| 用完零件的拼图 | `frostedresearch:nesting_pack` | 装入所有零件，槽内可留空 |

「方块拼图」是「填满的拼图」和「用完零件的拼图」的统称。转镜子分 0、1、2 级，例如「手炉用 0 级转镜子」。

## 方块排样

在阶段的 `tasks` 数组中用下列条目要求完成一次排样。使用 `frostedresearch:nesting_pack` 要求把所有零件放入凹槽，允许留空。改用 `frostedresearch:nesting_fill` 则须铺满凹槽，允许剩件。两种模式都使用默认 `level=0`，可在任务外层加 `"optional": true`。

```json
{ "requirement": { "id": "fit_parts", "type": "minigame", "game": "frostedresearch:nesting_pack" } }
```

左键点选后放置或直接拖放，R 或「旋转」顺时针转动。右键点槽内零件的任意一格即可整件收回，撤销可恢复上一步。同一绘图台保存每个研究任务的当前排样。可免费换题。满足目标后点击「确认定稿」才记录研究进度。

试玩命令为 `/knowledge grant idea frostedresearch:nesting_debug`。在绘图台打开「调试：方块排样」，两个模式各有一个任务。

## 折线图纸校核

在研究阶段的 `tasks` 数组中加入以下条目。玩家从任务详情点击「校核图纸」。`id` 在项目内唯一，`title` 和外层 `method` 可使用项目自己的语言键。省略标题时显示「校核图纸」。需要可选工作时在 `requirement` 同层设 `"optional": true`。

```json
{
  "requirement": {
    "id": "check_drawing",
    "type": "minigame",
    "game": "frostedresearch:foldline",
    "level": 1
  }
}
```

| level | 图纸 | 线路 | 镜位 | 镜片上限 |
|---|---|---:|---:|---:|
| 0（默认） | 双线 | 2 | 7 | 3 |
| 1 | 交错 | 2 | 8 | 4 |
| 2 | 三线 | 3 | 9 | 5 |

底稿已接通 A 线。玩家左键切换空位、正斜镜和反斜镜，右键清除。实心字母表示入口，空框表示出口，点击字母可只看该线，再点恢复全图。盘面提供撤销、恢复底稿、局部批注和同难度换题。三档各有 32 份基础图，开题时随机选择横纵位置、方向与转置。图纸生成与镜片调整均无纸笔消耗。

每个任务需要一次定稿。服务器检查镜片总数与所有实际出口，接受任意符合条件的布局。定稿后记录本次研究的任务完成，回到研究页按阶段要求继续。未定稿时，在同一张绘图台上重开同一研究任务会续上上次的题目和镜片。不同绘图台各存自己的草稿，完成情况属于队伍。

试玩命令为 `/knowledge grant idea frostedresearch:foldline_debug`。打开绘图台，在研究页开启「调试：折线图纸」，选任一档点击「校核图纸」。三项都定稿后可完成项目。示例定义见 [foldline_debug.json](../../src/main/resources/data/frostedresearch/frostedresearch/knowledge/projects/foldline_debug.json)。

## 从现有项目复制

正式整合包内容放在 TWR 的 `kubejs/data/<namespace>/frostedresearch/knowledge/`；普通数据包使用 `data/<namespace>/frostedresearch/knowledge/`。想法、项目、成果和联络规则分别位于 `ideas/`、`projects/`、`results/`、`links/`。JSON 文件路径决定定义 ID。模组不会自动安装文档目录里的示例包。

| 需求 | 可复制入口 | 需要一起修改 |
|---|---|---|
| 单阶段、提交并消耗实物的旧示例 | [trial_kiln 示例项目](example-datapack/data/example/frostedresearch/knowledge/projects/trial_kiln.json) | 这是 `sample` 任务示例，不是正式土制炉路线；对应想法、成果、观察和 sample 的 experiment/item |
| 正式土制炉、三种配比结果观察 | [正式土制冶炼炉路线](earthen-smelter.md) | 观察烧后物品的 `item.trial_kiln_variant`（`clay_rich`、`sand_rich`、`balanced`）；实验 ID 为 `frostedresearch:trial_kiln/<variant>`，绑定当前队伍 run。任务观察不消耗、不自动入档 |
| 两阶段、小游戏与原型 | [冶铁炉](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/iron_bloomery.json) | 阶段任务、临时配方、观察任务和最终 Design |
| 多个想法共同开启 | [joint_study](example-datapack/data/example/frostedresearch/knowledge/projects/joint_study.json) | 开始条件、逐项披露组、对应想法定义 |
| 成果承接下一研究 | [followup](example-datapack/data/example/frostedresearch/knowledge/projects/followup.json) | 成果入口及开始条件；不必强加想法 |
| 联络与事件使用不同前置 | [正式联络规则](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/) | inputs 是必要桌面记录；required_knowledge / optional_knowledge 独立配置，详见[定义参考](definitions.md#联络规则) |
| 行为获取想法 | [field_idea](example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json) | acquisition.required/optional/triggers；学习条件仍是 understanding |
| 知识影响下一阶段提示 | [耐火砖](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json) | 有序 variants、参考知识与默认路径；进入阶段时固定 |
| 可选任务减少后续任务 | [阶段格式](stages-and-airflow.md)、[可执行的阶段测试](../../src/gametest/java/com/teammoeg/frostedresearch/knowledge/StageResearchGameTests.java) | when.tasks_all 与 omit_tasks；不改变最终成果 |

人类或 AI 作者都先填写[研究内容简报模板](content-template.md)，再按其中的系统能力表确定哪些可纯数据配置、哪些要接到已有 Java 事件、哪些要新增机制。正式项目应从实际路线复制；旧 `trial_kiln` sample 是独立示例包中的消耗提交范例，不代表正式土制炉流程。示例包不会自动加载，也不保证任意设备能承载任意任务。

简报分别记录阶段目标、方法、判据和结论，便于开发者核对研究逻辑；这些栏目不是玩家文案格式。玩家看到的标题、方法和结论应像研究者自然写下的内容，不必逐项列出简报栏目，也不要提前透露尚未披露的要求。中英文内容放在资源包语言文件中。完整字段及可用观察谓词见[定义参考](definitions.md)。

项目`body`简述要研究什么、使用哪些实际物品及最终用途，避免重复开始条件。通用准备与设备操作写入阶段`method`；各试样的配比等写入`tasks[].method`，会直接显示在对应任务旁。制作或摆放只需指导时用说明文字，不为它新增必做任务。物品名称应能直接在JEI检索，不用“小炉”“控温炉”等不明确简称。

## 容易混淆的边界

- **五种知识角色分开设计。** 发现入口由 `access.discover_any`（未配置 discover_any 时，默认使用 `start_all` 中的想法条件）决定；行为触发由 `IdeaAcquisition.triggers` 决定；联络使用桌面 `inputs` 和队伍档案中的 `required_knowledge`，`optional_knowledge` 每满足一项少一个需消除的顺序数字，齐全为0数字；开始研究由 `access.start_all` 决定；阶段路径参考放在变体 `when.knowledge_all`。行为触发只有服务端已有发射点才会发生。每个知识条件只在明确配置的环节生效，不会自动扩展到其他环节。字段细节见[定义参考](definitions.md)。
- **普通前置需要入档，任务观察可以直接完成。** 作为发现、联络、开始或阶段路径条件的普通知识，必须是队伍有效档案中已掌握的知识；仅有未学习来件不算。研究观察本身也是一种知识，但 `type: "observation"` 任务可由符合条件的新鲜观察直接完成，不要求先手动记入档案；完成任务不会自动把这份观察学入永久档案。`team_current_run` 和 `experiment` 等任务观察限制见[定义参考](definitions.md)与[阶段说明](stages-and-airflow.md)。
- **只有真实消耗实物时才用 `sample`。** 它会消耗绑定当前队伍、当前 run 与任务的物品。观察烧后物品、查看世界对象或完成现场实验观察都不应仅因对象叫“样品”就建成 `sample`；正式土制炉三种配比结果是物品观察任务。保留[独立示例包中的 trial_kiln sample 项目](example-datapack/data/example/frostedresearch/knowledge/projects/trial_kiln.json)作为消耗提交范例。
- **阶段推进与变体选择是两个时点。** 阶段任务全部就绪后由玩家手动推进。进入阶段时按当时的有效档案知识选择首个匹配变体，并固定该变体；若知识是在更早阶段补齐，则到未来阶段进入时仍可参与选择。丢失已绑定的世界原型时可以重制原型，不能因此要求重做已成功的小游戏或其他任务。详见[阶段说明](stages-and-airflow.md)。

## 任务与可选项

把任务放在它实际适用的阶段，并说明是否必需。阶段的 `optional` 是任务级可选标记；`variants` 的 `omit_tasks` 可按路径移除指定的当前阶段任务。`when.tasks_all` 可在后续阶段参考先前阶段已完成的可选任务。不要只在玩家文案里称某任务“可选”而忘了配置字段。阶段任务完成后仍需玩家手动推进；下一阶段首次进入时才固定路径。

研究观察本身也是知识。当前 run 中完成的符合条件的新鲜观察可以直接满足 `type: "observation"` 任务；不必先抄录或学习入档，任务完成也不会自动把观察永久加入档案。发现、联络、开始和阶段路径中的普通前置则要求已学会的档案知识。`sample` 只用于确实消耗物品的任务。

## 系统能力参考

下表说明内容数据能表达什么，以及何时需要实现支持。JSON 字段以当前定义和示例为准；观察谓词还要求采集端真的记录该值。

| 目标 | 数据可配置 | 需要已有 Java 接点 | 需要新增机制 |
|---|---|---|---|
| 发现项目、开始门槛和逐项披露 | `ProjectAccess.discover_any`、`start_all`、`groups`、`conditions`；见 [joint_study](example-datapack/data/example/frostedresearch/knowledge/projects/joint_study.json) | 无 | 新发现界面行为或不同披露规则 |
| 通过玩家行为获得想法 | `acquisition.required`、`optional`、`triggers`、`chance`；见 [field_idea](example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json) | 服务端某处调用 `IdeaAcquisitionService.emit`；仅填写自定义 `event` 不会自动产生事件 | 需要模组当前未提供的行为检测/事件发射 |
| 联络必需知识与辅助知识 | LinkRule `inputs`、`required_knowledge`、`optional_knowledge`；见 [正式联络规则](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/links/) | 通常不需要额外事件接点 | 新联络交互或新的匹配语义 |
| 按知识开启研究 | `access.start_all`、`discover_any` 和 `groups`；见 [followup](example-datapack/data/example/frostedresearch/knowledge/projects/followup.json) | 无 | 新的启动流程或非知识条件门槛 |
| 阶段提示和分支 | `stages`、`variants[].when.knowledge_all`、`tasks_all`、`omit_tasks`、`optional`、`completion_recipes`；见[阶段说明](stages-and-airflow.md)及[耐火砖正式项目](../../../TheWinterRescue/kubejs/data/the_winter_rescue/frostedresearch/knowledge/projects/refractory_brick.json) | 任务所依赖的真实世界行为需已有提交接点 | 新阶段选择规则、新任务种类或新任务世界交互 |
| 观察物品、方块或实体 | `item_retrieval`、`observation.condition`、`experiment`、`team_current_run`；观察字段条件写在[身份与观察快照](definitions.md#身份与观察快照) | 实验或对象的特殊快照值需要已有观察/任务接点写入；土制炉通过 `item.trial_kiln_variant` 识别烧后配比 | 新观察字段的采集器、特殊检视方式或新任务证明机制 |
| 比较多份观察证据 | `type: "evidence"` 的 `inputs`、`conditions`、`distinct`；支持已有跨记录关系，见[定义参考](definitions.md) | 所需时空、环境或扩展值必须已被采集 | 尚未支持的跨记录比较或新采样字段 |
| 实验台计时与材料 | `type: "experiment"` 的 `duration`、`materials`、`space`；见[定义参考](definitions.md)中的 cold_trial 范例 | 需有实验台实际执行实验并提交进度 | 新实验设备或不同于现有实验台的运行规则 |
| 演算与已有小游戏 | `type: "calculation"` 的 `level`、`count` 使用旧连连看；`type: "minigame"` 的 `game` 当前支持 `frostedresearch:airflow`，即绘图台砂砖挖掘风道；漏风优先于炭床通路判定 | 已有游戏不需新增接点 | 新游戏实现及任务、界面接入 |
| 通用控温试验与批量生产 | `frostedresearch:controlled_firing` 配方定义材料、燃料、温度、时长和成功/失败产物；见[控温实验炉](controlled-furnace.md) | 绑定任务的 experiment 必须与设备配方对应 | 尚未支持的设备操作、工艺参数或结果机制 |
| 消耗绑定物品 | `type: "sample"` 的 `sample.experiment`、`sample.item`；见 [旧 sample 示例](example-datapack/data/example/frostedresearch/knowledge/projects/trial_kiln.json) | 设备需能制作并绑定该试验物品 | 新绑定方式或不同提交/消耗规则 |
| 研究完成后的权限 | `results` 引用成果；成果可声明当前支持的 Design 等 effect，见 [followup_finding](example-datapack/data/example/frostedresearch/knowledge/results/followup_finding.json) | 配方或机器须调用已有权限检查 | 新权限种类或尚未接入检查的设备 |

遇到“数据里能写，但运行中没有任何路径产生该事件、观察值或任务证明”的需求，应先标记所需 Java 接点或新增机制，不要用看似完整的 JSON 代替实现。

## 接入一个自定义行为

没有 Java 代码发出事件时，单写事件名称不会自动监听游戏。可以在已确定行为成功的服务端接点调用：

```java
IdeaAcquisitionService.emit(player,
    new ResourceLocation("example", "repaired_machine"), Optional.empty());
```

想法定义的 `acquisition.triggers` 对应填写：

```json
[{"event":"example:repaired_machine"}]
```

若需按物品或方块区分，发送对应 `KnowledgeElement`，并在 trigger 的 `subject` 中填写已有知识条件格式。多个 trigger 是任选入口；必要知识、非必要知识和概率仍由该想法自己的 acquisition 决定。不要把这些条件加到所有外部 `produce` 或 `learn` 授予途径。

这段 JSON 只登记监听条件，不会自行添加监听器。作者若找不到确切的服务端发射点，应在内容需求中标记“需要 Java 事件接点”；不得把一个尚未发射的事件名写成已可运行的行为。

## 最小验证步骤

先检查 JSON、定义引用、语言键和实际配方 ID，再在装有对应模组及脚本的环境重载。检查“没有知识 → 发现 → 缺项 → 开始 → 任务 → 完成 → 实际权限”整条流程。Design 写的是配方 ID，不是产物 ID；机器是否调用技术权限接口要单独确认，不能仅凭成果声明认定机器已受管。

阶段与观察测试入口见 [GameTest README](../../src/gametest/README.md)。运行测试时应看到当轮 `GAME TESTS COMPLETE` 和 `All N required tests passed`；游戏启动失败时 Gradle 仍可能退出成功。真实客户端、独立进程重启与生存获取是不同证据，分别记录。
