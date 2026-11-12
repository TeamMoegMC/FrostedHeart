# 知识定义、观察快照与联络规则

- Status: `Current`
- Last verified: `2026-11-12`
- Scope: 知识身份、不可变观察、独立想法/成果定义、项目关系与研究需求、物品检索规则、服务端联络匹配及观察用途判定。
- Code anchors: [
  `KnowledgeElement`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/KnowledgeElement.java)、[
  `Observation`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/Observation.java)、[
  `KnowledgeDefinitions`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java)、[
  `LinkRule`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkRule.java)、[
  `LinkMatcher`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java)、[
  `ProjectRequirement`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectRequirement.java)。

## 行为想法与联络前置（M2）

`IdeaDefinition.acquisition` 缺席时保持没有行为入口、没有获取前置。存在时支持：

```json
{
  "acquisition": {
    "required": [{"kind": "idea", "definition": "example:i"}],
    "optional": [{"kind": "result", "definition": "example:smelting_finding"}],
    "chance": 0.15,
    "triggers": [
      {"event": "frostedresearch:item_obtained", "subject": {"kind": "observation", "observation_type": "item"}},
      {"event": "example:field_work"}
    ]
  }
}
```

`required` 和 `optional` 均复用 KnowledgeCondition，默认空；triggers 默认空。每个 trigger 必填 event，subject 可选，
缺席表示只匹配事件 ID。有多个入口时 OR；同一次操作命中几个入口也只为该想法抽一次概率。
全部 required 仅限制行为和联络；不限制外部 receive / produce / learn，understanding 继续只负责学习。
subject 描述当次行为载荷，required / optional 查询有效档案；不把两者混成同一种事件判断。
chance 可按想法指定 [0,1]，未指定用服务器 knowledgeBehaviorChance。概率与等待公式见 [API 文档](state-and-api.md#m2-行为事件与短联络)。

内置事件 ID：`frostedresearch:item_obtained`、`item_used`、`block_interacted`、`observation_completed`、`knowledge_learned`。
前三者载荷为对应 item/block Observation，观察完成为当次采样，学习为入档的 KnowledgeElement；
`example:field_work` 这类外部事件可不带载荷，或带同一有类型元素。无联络规则的想法仍可行为获得。
全部 required / optional / trigger.subject 观察条件加入单槽用途索引，包括隐藏想法。

LinkRule.inputs 仍必填，作者可显式给 0–5 槽。0 槽表示明确配置的零输入候选，1 槽只需一份札记；
旧 2–5 槽仍按互异身份与原跨字段条件完整匹配。不会从全部想法自动生成候选目录。
示例 [field_idea](example-datapack/data/example/frostedresearch/knowledge/ideas/field_idea.json)
及 field_zero/field_one/field_legacy 演示三个输入规模。新可选字段严格解码，错误类型不静默变空。

## 身份与观察快照

`KnowledgeKey(kind, id)` 使用三个互不混淆的身份空间：`observation` 的 `id` 是原始记录 UUID，`idea` 和 `result` 的 `id`
是资源标识。`KnowledgeElement` 的 `observation` 保存完整原始观察，`title`、`body` 为可选摘要；想法/成果摘要不替代当前定义。

`Observation` 保存 `record_id`、`type`（`block` / `entity` / `item`）、`object`、`values`、`source` 与可选 `item_data`
。映射不可变，NBT 在接收和读取时复制。原始身份与来源不会因为抄录而变化。`source` 的字段为：

| 字段                     | 类型与含义                                                 |
|------------------------|-------------------------------------------------------|
| `kind`                 | 产生机制字符串，例如 `player`、`npc`、`town`、`worldgen`、`command` |
| `original_observer`    | 可选 UUID，实际原始观察者                                       |
| `organization`         | 可选 UUID，组织观察的队伍                                       |
| `dimension`、`position` | 可选来源维度和整数坐标，独立于被描述对象的采样位置                             |

`values` 是有类型的快照字段映射。每个值的结构为 `{ "state": "known", "type": "number", "number": -12.5 }` 或
`{ "state": "known", "type": "string", "text": "minecraft:plains" }`。`state` 还可取 `unknown`、`not_applicable`，这两种状态不带
`text` / `number`。省略适用字段表示未知；通过 `Observation.value(field)` 读取不适用字段总是得到不适用状态。未采集数值不会成为零。

[`ObservationFields`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/ObservationFields.java) 定义可查询字段：

| 字段                                                                  | 类型     | 适用范围、语义                                |
|---------------------------------------------------------------------|--------|----------------------------------------|
| `object`、`type`、`record_id`                                         | 字符串    | 从记录身份与对象派生                             |
| `dimension`                                                         | 字符串    | 方块/实体的采样维度                             |
| `x`、`y`、`z`                                                         | 数值     | 方块/实体采样坐标，单位格；实体保留小数位置                 |
| `time`                                                              | 数值     | 原始世界 tick，供排序与间隔比较                     |
| `year`、`month`、`day`、`hour`                                         | 数值     | 采集时 WorldClockSource 的日期展示快照           |
| `biome`、`climate`、`temperature_type`、`time_period`                  | 字符串    | 方块/实体的离散环境上下文                          |
| `temperature`                                                       | 数值     | 方块/实体的 `WorldTemperature.block` 温度，摄氏度 |
| `block_state.<property>`                                            | 字符串    | 仅方块观察，目标方块状态属性                         |
| `entity_instance`                                                   | 字符串    | 仅实体观察，具体实体 UUID                        |
| `item_block`                                                        | 字符串    | 仅物品观察，方块物品对应的方块标识                      |
| `item.<key>`                                                        | 字符串    | 仅物品观察，被检索规则允许的顶层 NBT 值的文本快照            |
| `context.<id>`、`field.<id>`                                         | 字符串、数值 | 方块/实体扩展上下文和数值场；采集方负责定义单位与含义            |
| `source_kind`、`original_observer`、`organization`、`source_dimension` | 字符串    | 从原始来源派生；没有实际观察者时仍是未知                   |
| `source_position.x`、`source_position.y`、`source_position.z`         | 数值     | 从原始来源位置派生，单位格                          |

物品观察没有环境时空、气候、温度和已放置方块状态，也不能录出为研究笔记。`item_data` 不保存物品数量；检索时刻属于队伍接收记录。

## 数据包目录与定义

资源 `data/<namespace>/frostedresearch/knowledge/<kind>/<path>.json` 的标识是 `<namespace>:<path>`。目录分别为 `ideas`、
`results`、`links`、`projects`、`item_retrieval`、`initial`。

[可加载示例数据包](example-datapack/README.md) 演示物品观察、替代联络路径、独立想法、项目关系及两种成果。示例由加载器测试验证，未作为正式游戏内容自动启用。

想法示例：

```json
{
  "title": "knowledge.example.migratory_birds.title",
  "body": "knowledge.example.migratory_birds.body",
  "understanding": [
    {
      "kind": "idea",
      "id": "example:seasons"
    }
  ],
  "projects": [
    "example:bird_tracking"
  ],
  "enabled": true
}
```

`title` 必填；`body` 默认为空；`understanding`、`projects` 默认为空；`enabled` 默认为 `true`
。理解条件是学习时要求有效掌握的知识身份列表，采用全部满足语义。定义不能引用运行中才产生的观察 UUID。

独立成果示例（文件 `results/bird_map.json`）：

```json
{
  "title": "knowledge.example.bird_map.title",
  "body": "knowledge.example.bird_map.body",
  "understanding": [],
  "enabled": true,
  "result": {
    "type": "finding",
    "id": "example:bird_map",
    "views": [
      "example:bird_map"
    ]
  }
}
```

`result` 复用已有 [`ResearchResult`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResult.java) 的
`finding`、`design`、`construction`、`procedure`、`prototype` 五类结构，嵌入的 `id` 必须等于资源路径标识。它不需要嵌在旧
research topic 内。类型效果结构见 [成果与权限](../research/results-and-access.md)。

项目定义声明成果关系和研究需求：

```json
{
  "title": "knowledge.example.tracking",
  "body": "knowledge.example.tracking.body",
  "results": ["example:bird_map"],
  "requirements": [
    {
      "id": "two_sites",
      "type": "evidence",
      "title": "knowledge.example.tracking.two_sites",
      "inputs": [
        {"name": "a", "kind": "observation", "observation_type": "entity",
         "conditions": [{"field": "object", "operator": "tag", "tag": "example:birds"}]},
        {"name": "b", "kind": "observation", "observation_type": "entity",
         "conditions": [{"field": "object", "operator": "tag", "tag": "example:birds"}]}
      ],
      "conditions": [{"kind": "distance", "left": "a", "right": "b", "operator": "gte", "value": 1000}],
      "distinct": [{"field": "biome", "slots": ["a", "b"]}]
    },
    {"id": "tabulate", "type": "calculation", "level": 1, "count": 2},
    {
      "id": "cold_trial",
      "type": "experiment",
      "title": "knowledge.example.tracking.cold_trial",
      "duration": 1200,
      "materials": [{"item": "minecraft:feather", "count": 2}],
      "space": {"min_volume": 8, "max_temperature": 0, "blocks": [{"tag": "minecraft:logs", "count": 4}]}
    }
  ]
}
```

`title`、`body` 默认为空；`results` 必填，至少一项且必须存在；`requirements` 默认为空。没有需求的项目只能由研究系统接口或开发指令完成，
玩家在研究页无法开始它。

### 项目发现、开始与披露（M1）

[`KnowledgeCondition`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeCondition.java)
复用 `LinkRule.Slot` / `FieldCondition` 匹配，仅查询有效档案；来件不等于掌握。
条件必填 kind；idea/result 必填 definition；observation 必填 observation_type，可带 conditions 数组、title 与 hint。
观察按 item/block/entity 及字段/标签匹配，不接受固定 record_id / entity_instance 作为前置。

项目可选 [`ProjectAccess`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/ProjectAccess.java) `access`：

- `conditions`：局部名 → KnowledgeCondition；相同谓词的别名在加载时合并，文案取局部名字典序第一项。
- `start_all`：开始必须全部掌握的条件名，默认 []；只检查作者列出的条件，不展开想法获取或 understanding 前置。
- `discover_any`：额外发现入口，默认 []；并入 start_all 中的全部 idea 条件形成发现集合，任一掌握即可发现。
- `groups`：`id`、`reveal_any`、`conditions`、可选 `unknown_hint`；组的任一揭示条件掌握后公开组引用的关联。组不添加开始要求。

缺席 access 的旧项目保持任一有效 IdeaDefinition.projects 来源可发现并启动；显式 access 不使用旧 projects 补门槛。
显式空 access 无发现入口，不因 start_all 空而公开。字段存在但类型错误会报告加载错误，保留上一份定义快照。
必要条件未被组覆盖时补一个发现集合揭示的组，生成 ID 避开已有 default 名。

所有必要条件逐项显示；未知项各有占位，即使已拥有 C，只属于未揭示 J 组的 C 仍不公开关联。
共享 B 在所有组间只有一行，组引用该行；数量与要求结构可见。公开的非必要条件标为参考，不阻止开始。
观察条件的全部用途（包括未发现项目、参考和揭示条件）逐项注册为单槽 pattern，不增加独立证据份数。
可运行数据见 [joint_study](example-datapack/data/example/frostedresearch/knowledge/projects/joint_study.json)
及 [纯成果 followup](example-datapack/data/example/frostedresearch/knowledge/projects/followup.json)。M2 获取概率与短联络见上文；M4 阶段尚未实现。

每项需求的 `id` 在项目内唯一，是进度身份：增删一项需求不会让其他需求的进度错位。`type` 取（另有独立实物类型 sample，见[小炉与实物提交](trial-kilns-and-samples.md)）：

| 类型            | 字段                                                    | 含义与校验                                                                                       |
|---------------|-------------------------------------------------------|---------------------------------------------------------------------------------------------|
| `evidence`    | `title`（必填）、`inputs`、`conditions`、`distinct`          | 提交一组观察，一份记录占一个槽位。`inputs` 为 1–8 个观察槽位，写法同联络规则；`conditions` 最多 10 条跨条件；不使用 `level`/`count` |
| `calculation` | `title`（可选）、`level`（0–3，默认 0）、`count`（1–16，默认 1） | 在绘图台赢下 `count` 局难度不低于 `level` 的纸牌局；不读取观察                                                   |
| `experiment`  | `title`（必填）、`count`（1–16，默认 1）、`duration`、`materials`、`space` | 在实验台完成 `count` 次实验，每次连续运行 `duration` tick（20–72000）；不读取观察，不使用 `level`                             |

`distinct` 中每组的 `field` 必须是已知观察字段，`slots` 至少列出两个不同的已有槽位，表示这些槽位在该字段上两两不同；校验时展开为两两
`ne` 比较，因此未知或不适用的值同样不满足。证据需求的条件不进入客户端快照，玩家看到的是作者撰写的 `title`，所以证据需求必须提供它。

`duration`、`materials`、`space` 只属于实验需求。`materials` 至多 3 项，在实验开始时一次投入：每项写 `item` 或 `tag`
（两者都写时都要满足）和 `count`（1–64，默认 1）。`space` 省略时不要求房间；写出时各字段为：

| 字段                                    | 默认           | 含义                                                         |
|---------------------------------------|--------------|------------------------------------------------------------|
| `enclosed`                            | `true`       | 实验台必须在封闭房间里。为 `false` 时只能写温度，不能写体积和方块                      |
| `min_volume` / `max_volume`           | `1` / `4096` | 房间的空气格数，`1 ≤ min ≤ max ≤ 4096`                              |
| `min_temperature` / `max_temperature` | 不限           | 实验台所在位置的温度（°C），下限不得高于上限                                    |
| `blocks`                              | `[]`         | 至多 8 项，每项写 `block` 或 `tag` 和 `count`（1–64）；计入与房间空气相邻的全部方块 |

房间是从实验台相邻的空气连通出去的空间：先取上方，被占时依次取北、东、南、西侧；梯子按空气计。连通空气超过 4096 格，或任何一格空气向上直通
建筑高度上限，都视为不封闭。物品和方块 ID 必须已注册；标签不检查是否存在，空标签永远凑不够数量。实验的条件同样不进入客户端快照，玩家在研究页
看到的是标题、次数与每次时长，在实验台上看到逐项检查结果。

需求与项目字段使用严格可选字段：字段存在但无法解码时报告错误，而不是静默回落到默认值。进度规则见
[状态与 API](state-and-api.md#实际联络与研究历史)。

`initial` 文件内容直接是知识身份数组，例如 `[{"kind":"idea","id":"example:seasons"}]`；所有文件合并并按身份去重，由队伍初始化入口授予。

`item_retrieval` 示例：

```json
{
  "item": "minecraft:potion",
  "nbt_keys": [
    "Potion",
    "CustomPotionEffects"
  ]
}
```

也可使用 `tag` 指定物品标签；同时指定 `item` 与 `tag` 时两者都需满足。`nbt_keys` 默认为空，按顶层键复制匹配物品的附加
NBT。未列出的数据不会进入这次观察，物品本身不被消耗。

## 联络规则

```json
{
  "inputs": [
    {
      "name": "a",
      "kind": "observation",
      "observation_type": "entity",
      "conditions": [
        {
          "field": "object",
          "operator": "tag",
          "tag": "example:birds"
        }
      ]
    },
    {
      "name": "b",
      "kind": "observation",
      "observation_type": "entity",
      "conditions": [
        {
          "field": "object",
          "operator": "tag",
          "tag": "example:birds"
        }
      ]
    }
  ],
  "conditions": [
    {
      "kind": "compare",
      "left": "a",
      "field": "object",
      "right": "b",
      "operator": "eq"
    },
    {
      "kind": "compare",
      "left": "a",
      "field": "biome",
      "right": "b",
      "operator": "ne"
    },
    {
      "kind": "distance",
      "left": "a",
      "right": "b",
      "operator": "gte",
      "value": 1000
    }
  ],
  "output": "example:migratory_birds",
  "hints": [
    "knowledge.example.birds.hint1",
    "knowledge.example.birds.hint2"
  ]
}
```

`inputs` 必须有 2 至 5 个槽位；`name` 在规则内唯一。想法/成果槽位写为
`{"name":"idea","kind":"idea","definition":"example:seasons"}`，不能读取观察字段。观察槽位可省略 `observation_type`
，但匹配对象标签时必须声明对象类型。

槽位 `conditions` 与规则级 `conditions` 默认为空，各自采用全部满足语义。单字段操作符包括 `eq`、`ne`、`lt`、`lte`、`gt`、`gte`、
`exists`、`unknown`、`not_applicable`、`tag`。普通比较使用与字段同型、状态为 `known` 的 `value`；区间写为上下界两个条件。有序比较只适用于数值。
`tag` 只适用于 `object`、`biome`，读取当前服务端标签。存在性条件没有 `value` 或 `tag`。

跨输入最多 5 条条件。`compare` 比较 `left.field` 与 `right.right_field`；省略 `right_field` 则使用相同字段名。来源需要明确写出
`source_kind`、`original_observer`、`organization` 或 `record_id`，不存在含义模糊的整体 `source` 比较。`distance`
读取双方维度及三轴坐标，使用欧氏距离，单位格；仅当维度已知且相同、坐标全部已知时成立。未知或不适用的值不会满足普通比较，包括
`ne`。

`LinkMatcher.match` 要求所选全部元素恰好填满全部槽位，每个槽位绑定不同 `KnowledgeKey`
。它先计算每槽候选，再从候选最少的槽位递归绑定，及时判定已具备双方输入的跨条件。匹配结果包含每条成立的规则及实际绑定，供玩家选择产出。
`LinkMatcher.candidates` 给居民等调用者寻找档案中的候选组合，每条规则至多返回一个有效绑定，并受调用方给定结果数量上限约束。

## 当前用途、差异与重载

`Snapshot.hasObservationUse` 判断观察能否匹配任何已注册用途的局部输入：含观察槽位的联络规则、所有项目的证据需求槽位、全部 access.conditions 与 acquisition 的独立单槽用途，或外部
`ObservationUse`。不要求队伍已拥有完整组合。证据需求与联络规则一样参与 `addsObservationDifference`，同一需求中的重复槽位会保留所需数量的独立记录。

`addsObservationDifference` 先排除相同原始 UUID；对不同 UUID
比较所有当前输入条件的匹配结果，以及跨条件所读取字段的准确值，并保留同规则重复槽位需要的独立记录数。位置、时间、原始观察者或记录身份被跨条件使用时，相关差异会被保留。

外部系统通过 `KnowledgeDefinitions.registerObservationUse(id, use)` 登记需求。`ObservationUse.matches` 表示可用输入，
`independentRecords` 默认 1，`equivalent` 默认未知；未知等价性保留记录。只有扩展明确确认记录可互换，才允许它参与观察去重。注销或更新用途递增定义版本，后续状态查询重新判断用途。

重载先解析所有定义并校验数量、名称、字段类型、引用和条件，再整体安装新 `Snapshot`。错误通过 `ReloadResult.diagnostics`
汇总，原快照继续生效。成功提交递增 `revision`；重载本身不删除任何队伍历史。

想法和成果的内容定义独立于产生路径。删除联络路径不会仅因失去生产者而使仍存在的定义失效；加载器允许这样的存量定义。定义缺失/停用与当前观察用途变化的休眠行为，由统一知识服务查询决定。

## 面向玩家的文案

想法/成果的 `title`、`body`、规则的 `hints` 和项目标题支持翻译键；语言资源由客户端资源包/模组提供。已有键由当前客户端语言解析，普通作者正文使用原文。请不要把定义
ID、字段名或 JSON 规则说明当作玩家正文。示例包使用 `knowledge.knowledge_example.*` 键，模组提供中英文译文；包内 README
保留作者说明，游戏正文只描述玩家看到或想到的内容。

M3-A：ProjectRequirement.type=sample 使用 sample.experiment 与 sample.item，固定消耗1件绑定当前队伍/run/任务的真实物品；它不增加观察用途或模拟实验台计时。完整字段与例子见[小炉与实物提交](trial-kilns-and-samples.md)。

## 旧研究的可选替换映射（M3-B）

`ProjectDefinition.legacy_replacement` 缺席时不影响旧路线。格式固定为：

```json
"legacy_replacement": {
  "research": "alloy_kiln",
  "effects": {
    "350c485a4b92425c": "the_winter_rescue:earthen_smelter_design",
    "1bb68ce233ef4f21": "the_winter_rescue:earthen_smelter_construction"
  },
  "partial_idea": "the_winter_rescue:earthen_smelting"
}
```

这是静态的一项旧研究到新项目的映射，不是通用迁移脚本。`research` 是旧 JSON 文件名得到的字符串 ID，
`effects` 的键是旧效果 nonce；effects 必须非空，目标属于本项目 results，允许 Design、Construction 或二者组合；同一 Design 可承载多条生产配方。
映射成果必须启用、understanding 为空；Design 的每个 recipe 必须在当前服务器 RecipeManager 中存在，
Construction 的每个 multiblock 必须能由 IE `MultiblockHandler.getByUniqueName` 解析；partial_idea 也必须存在且启用。
同一旧 ID 多重映射停用该 ID 的替换并告警。缺任一映射定义或目标时记录诊断，保留旧入口；最终 recipes/tags、
队伍加载与登录重新核对。映射成果/想法引用缺席属于桥接局部不可用，不以保留上一快照的方式留住已失效替换。
其他项目引用错误仍遵守现有原子重载验证。

运行时桥接和回执语义见 [状态与 API](state-and-api.md#旧研究承接与完成等价查询m3-b)。

## 有序阶段（M4）

项目可写 stages，不能同时写非空顶层 requirements。无 stages 的项目继续按 main 单阶段执行。
每阶段声明 id/title/objective/method/conclusion、tasks、variants、completion_recipes；任务为
`{requirement: ProjectRequirement, optional: false, method: ""}`。任务ID在整个项目内唯一。
变体为 `{id, when?: {knowledge_all: KnowledgeCondition[], tasks_all: string[]}, omit_tasks: string[], method?: string}`；
最后无when的项是默认路径，按作者顺序首命中，省略的任务必须属于本阶段，tasks_all只能引用先前阶段可选任务。
全部阶段的证据、观察谓词及变体参考知识都会登记观察用途，不等进入阶段才允许观察入档。

新增任务：minigame 的 game 固定 frostedresearch:airflow；observation 的 observation 包含 condition、可选experiment、
team_current_run（缺省false）。有experiment须team_current_run=true；只接受新鲜采样动作，不能靠学来件/旧笔记完成。
当前队伍样品沿用ResearchSampleBinding四字段，无个人owner；world试件使用位置proof，不把它塞入知识字段。

铜/青铜和碳燃料任选组仍使用已有物品tag谓词，start_all在两组之间取AND；没有新增OR表达式。
阶段行为、保存与客户端披露见 [阶段与风道](stages-and-airflow.md)。
