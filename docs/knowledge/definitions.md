# 知识定义、观察快照与联络规则

- Status: `Current`
- Last verified: `2026-09-08`
- Scope: 知识身份、不可变观察、独立想法/成果定义、项目关系、物品检索规则、服务端联络匹配及观察用途判定。
- Code anchors: [
  `KnowledgeElement`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/KnowledgeElement.java)、[
  `Observation`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/Observation.java)、[
  `KnowledgeDefinitions`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java)、[
  `LinkRule`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkRule.java)、[
  `LinkMatcher`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java)。

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

项目定义只声明知识图谱关系，例如 `{"title":"knowledge.example.tracking","results":["example:bird_map"]}`
。每个项目至少声明一项存在的成果。它不实现计算、证据和实验的执行玩法。

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

`Snapshot.hasObservationUse` 判断观察能否匹配任何已注册规则的局部输入，或外部 `ObservationUse`。不要求队伍已拥有完整组合。

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
