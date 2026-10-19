# 知识系统开发状态审阅

- Time: `2026-10-18 23:01:04 -0500`
- Author: `Claude (Anthropic Opus 5.5), 代码审阅`
- Related: `src/main/java/com/teammoeg/frostedresearch/knowledge/`, `docs/knowledge/`, `design/knowledge_research.md`
- Reviewed at: `a8d89e5f2`（知识代码主体来自 `807f43691`，经 `3ad011603` 合入 master）

本文是非权威审阅意见，基于逐文件阅读源码、文档与示例包。**未执行构建或测试**（项目所有者已完成测试），**未做性能测量**；
文中的体量与耗时均为按代码推算的量级估计，已标注。

---

## 1. 结论摘要

基础层与 [设计文档](../design/knowledge_research.md) 对应得很紧：知识身份（记录 UUID / 定义 ID）、观察值三态（已知 / 未知 / 不适用）、
原子切换的定义快照与整体校验、统一服务入口与 Check/Committed 事件、按当前定义动态计算的休眠、规则级首次绑定的联络历史、
分片原子安装的客户端快照，都按设计落地，模型干净。玩家闭环
「观察 → 来件 → 档案 → 联络 → 想法 →（临时指令）项目 → 成果 → 配方/多方块/方块权限」已经打通。

开发阶段仍是「地基 + 示例包」：没有随模组发布的正式知识内容；研究项目执行、居民讨论/研究所、NPC/城镇/世界生成观察都只有 API，没有调用方。

主要风险不在单条逻辑的正确性，而在**持久化健壮性与规模**。这几类问题在示例包和几十条观察的测试规模下都不会暴露：

| 编号   | 问题                                                    | 影响                                    | 修复成本 |
|------|-------------------------------------------------------|---------------------------------------|------|
| P0-1 | 任一条知识记录解码失败 → 整个队伍文件加载时被跳过，下次保存时被删除                    | 该队伍全部 Chorda 数据（城镇、研究、发电机…）丢失          | 小    |
| P1-1 | 每次变更发送全量快照：内容重复、历史无界、状态计算 O(来件×档案×规则)、一次操作触发多次同步       | 档案增长后，每次点击都在主线程做重活、发 MB 级数据包（估算）      | 小～中  |
| P1-2 | 服务端权限投影每次查询都整体重建，且现在随档案条数线性增长                          | 每次右键方块/合成判定都遍历整个档案                     | 小    |
| P2-* | 图谱观察节点泛滥；梦境回退主题固定；讨论邀请显示原始键；重复检视实物；联络反馈；暗示深度优先；候选回溯无界 | 体验问题 / 潜在隐患                            | 小    |

---

## 2. 开发状态对照设计

| 设计条目                         | 状态   | 说明                                                                                      |
|------------------------------|------|-----------------------------------------------------------------------------------------|
| 队伍档案、来件、身份与重复判定              | 已实现  | `KnowledgeState`、`KnowledgeService`、`addsObservationDifference`                          |
| 统一学习 / 忘记 / 删除 / 事件          | 已实现  | `KnowledgeOperationEvent.Check` / `Committed`                                           |
| 休眠（定义缺失/停用、观察无用途）            | 已实现  | 查询时按当前定义计算，不存状态，因此无需迁移                                                                  |
| 主动观察                         | 已实现  | 长按观察键 3 秒，服务端锁定目标并一次采样。设计写 R，实现默认 N（文档已注明）                                             |
| 间接观察（NPC / 城镇 / 世界生成）         | 仅 API | `ObservationSessions.preview`、`Observation.Source` 均无生产调用方                             |
| 物品检索                         | 已实现  | 数据包 `nbt_keys` + `ItemRetrieval.registerField`；重复检视见 P2-4                              |
| 研究笔记录入 / 录出                  | 已实现  | 原始 RecordId 与 `originalSource` 沿抄本传递                                                   |
| 联络（完整无序匹配、多候选择一）             | 已实现  | `LinkMatcher.match`；反馈见 P2-5                                                            |
| 暗示：梦境                        | 已实现  | 见 P2-2、P2-6                                                                              |
| 暗示：玩家讨论                      | 已实现  | 见 P2-3                                                                                   |
| 暗示：居民讨论 / 研究所                | 仅 API | `KnowledgeService.discuss`、`candidateLinks` 无调用方；见 P2-7                                  |
| 外部观察用途 `ObservationUse`       | 仅 API | 没有注册方                                                                                   |
| 研究项目（计算 / 证据 / 实验）           | 未实现  | 仅 `/knowledge research complete` 临时指令走真实的启动/完成流程                                        |
| 成果：发现 / 设计 / 建造 / 流程         | 已实现  | 经 `TechnologyAccessResolver` 与旧研究效果合并；性能见 P1-2                                         |
| 成果：原型                        | 仅身份  | 与设计「留待后续设计」一致                                                                           |
| 知识图谱                         | 已实现  | 见 P2-1                                                                                   |
| 队伍合并                         | 仅指令  | `/knowledge merge`，无队伍系统挂钩                                                              |
| 正式内容                         | 无    | 只有 `docs/knowledge/example-datapack`，不随模组加载                                             |
| 验收                           | 部分   | 自动化测试已通过；游戏内视觉与多人矩阵在 [diary](../diary/2026-10-18_22-42-01_knowledge-login-gametest-ftb-skip.md) 中仍列为未做 |

`docs/knowledge/` 与源码核对后基本一致，仍可作为现行文档使用。

---

## 3. 问题与建议

### P0-1 单条记录解码失败会丢弃并删除整个队伍文件

事实链：

1. `KnowledgeState.read` 对 `archive`、`inbox`、`links`、`research`、`acquisitions` 都用 `codec.listOf()` 整体解码并
   `getOrThrow(false)`
   （[KnowledgeState.java:243-273](../src/main/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeState.java#L243)）。
   任何一个元素失败，整个 `read` 抛出异常。`hints` 的 `new ResourceLocation`、`dream_topics` 的 `UUID.fromString` 同理。
2. 异常经 `SpecialDataType.loadData` 进入 `DataHolderMap.load`，被包装为 `DataLoadException`
   （[DataHolderMap.java:100-104](../src/main/java/com/teammoeg/chorda/dataholders/DataHolderMap.java#L100)）。
3. `CTeamDataManager.load` 捕获后只记录 "contains unexpected data, ignoring..."，该队伍不进入 `dataByOwnId`
   （[CTeamDataManager.java:277](../src/main/java/com/teammoeg/chorda/dataholders/team/CTeamDataManager.java#L277)）。
4. 下一次 `save()` 把没有对应已加载队伍的 `.nbt` 文件全部删除
   （[CTeamDataManager.java:334](../src/main/java/com/teammoeg/chorda/dataholders/team/CTeamDataManager.java#L334)）。
5. 同一个文件里还保存着该队伍的 `research`、`town`、`generator`、`labour`、`scenario`、`seed` 等全部数据。

触发条件。按当前代码，自身产生的记录都能通过校验，所以现在不容易触发；但以下两点会随演进和接入而变得容易触发：

- `Observation.CODEC` 只在解码方向校验（`comapFlatMap` 的编码方向是 identity），而 `Observation` 是公开 record，构造器不校验
  （[Observation.java:55-71](../src/main/java/com/teammoeg/frostedresearch/knowledge/model/Observation.java#L55)）。
  将来接入 NPC / 城镇 / 世界生成观察时，只要构造出一条字段名拼错、类型不符或不适用的观察（例如给物品观察写 `biome`），
  `preview` 编码、`receive` 入库、存档写盘都会成功；**下次开服时整队文件被跳过，再次保存时被删除**。
  客户端预览解码失败只会静默不弹窗，开发时也不容易发现。
- `ObservationFields` 是硬编码表。以后调整字段类型或适用范围、重命名枚举、把可选字段改为必填，都会让旧存档整条解码失败
  （`KnowledgeEntry.original_source` 现在已经是必填）。`schema_version` 只写不读，没有迁移挂点。

建议（与 AGENTS.md「不要 fail-closed，正确处理不一致状态」一致）：

- `read` 改为逐元素解码。失败的元素保留原始 NBT 放进隔离列表，保存时原样写回，日志只记一次。这样坏数据只影响那一条，
  也符合设计中「休眠保留原始记录与历史」的精神。
- 在 `Observation` 紧凑构造器里（或在 `KnowledgeService.receive/learn` 入口）执行与解码相同的校验，非法记录在产生处就被拒绝，
  不会写进存档。
- `read` 读取 `schema_version`，为之后的迁移留出入口。
- （属于 Chorda，超出本审阅范围）`CTeamDataManager.save` 不应删除加载失败的文件，至少改名保留。

### P1-1 全量快照的体量与计算随档案二次增长

事实：

1. 每次 `notifyChanged` 都调用 `TeamResearchService.sync(team)`，构造完整的 `KnowledgeSyncSnapshot` 并发给所有在线成员
   （[KnowledgeService.java:94](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L94)、
   [TeamResearchService.java:121-130](../src/main/java/com/teammoeg/frostedresearch/api/TeamResearchService.java#L121)）。
   队伍无人在线时也会先把快照构造出来。
2. 快照同时包含 `team_data`（整个 `KnowledgeState`：archive、inbox、带完整元素的 acquisitions、daily、dream_topics）和
   `archive_view`（又一份 archive/inbox 的完整元素）
   （[KnowledgeSyncSnapshot.java:44-49](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeSyncSnapshot.java#L44)）。
   客户端的 `team_data` 镜像和 `knowledge` 投影目前**没有读取者**（`ClientKnowledgeDataAPI.getData()`、`knowledgeProjection()`
   在客户端没有调用点）。
3. `acquisitions` 在每次 receive、learn、产出保留时都追加一份完整的 `KnowledgeEntry`（含完整观察）；删除来件不会移除它，
   它没有上限，也不计入容量
   （[KnowledgeService.java:188](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L188)、
   [:231](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L231)、
   [:250](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L250)）。
   而视图只用到其中的 key、source、time
   （[KnowledgeView.java:119-127](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java#L119)）。
4. 视图对每条来件调用 `query` → `learningStatus` → `addsObservationDifference`。后者把候选与档案中每条观察逐对做
   `equivalent`，每一对又遍历所有规则的所有槽位条件
   （[KnowledgeView.java:158](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java#L158)、
   [KnowledgeDefinitions.java:240-278](../src/main/java/com/teammoeg/frostedresearch/knowledge/definition/KnowledgeDefinitions.java#L240)）。
   一次快照的代价约为 |来件观察| × |档案观察| × Σ(槽位条件)。
5. 一次操作会触发多次全量同步：
    - `link` 发现新路径并学会想法时 2 次
      （[KnowledgeService.java:299-301](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L299)）；
    - `initialize` 每条初始知识 1 次，最后再 1 次；
    - `TeamResearchService.grantResult` 2 次；
    - 登录至少 2 次：NORMAL 优先级的 `ResearchCommonEvents.syncDataToClient` 和 LOWEST 的 `KnowledgeRuntime.login`；
    - `/reload` 时先由 `ResearchHooks.ServerReload` 给**所有队伍**（包括离线队伍）各构造一次，这时标签还没有最终安装；
      之后 `OnDatapackSyncEvent` 再给在线队伍各发一次。

量级估计（未测量）：一条方块观察约 17 个值，NBT 约 1.4 KB；一条 `KnowledgeEntry` 约 1.7 KB。在配置上限附近
（4096 条档案观察 + 512 条来件观察），一次快照未压缩约 20 MB 以上（`team_data` 与视图各一份，acquisitions 另计且无上限）；
按示例包的 12 条规则，状态计算约为 10^8 次条件求值。这些工作都在服务器主线程上、在每次点击时发生。

建议，按收益与成本排序：

1. 从快照中去掉 `team_data`（客户端不用），体量约减半，客户端每次也少解码一整份状态。
2. acquisitions 只保存 `(key, source, originalSource)`，不复制元素。
3. 合并同步：`notifyChanged` 只把队伍标记为 dirty，在 `ServerTickEvent` END 统一发送，跳过无人在线的队伍。
   这一步同时消除第 5 条里的所有重复同步，也可以去掉 `batchDepth` / `syncPending` 机制。
4. 观察差异改用「等价签名」：每条观察按（类型、各规则槽位命中位图、命中槽位涉及的跨条件字段值、各 `ObservationUse`
   的命中位）计算一次签名，档案维护「签名 → 条数」，判定变成查表。`ObservationUse.equivalent` 默认未知，在签名中带上
   recordId 即可保持「无法证明可替代就保留」的语义。签名可以按定义 revision 缓存。
5. 做完 1–4 后再评估是否需要增量同步，大概率不需要。

### P1-2 服务端权限投影每次查询都整体重建

- 服务端的 `ResearchHooks.canUseBlock`、`canUseRecipe`、`canFormMultiblock`、`hasMultiblock` 每次调用
  `TechnologyAccessResolver.project(...)` 或 `projection(teamId)`
  （[ResearchHooks.java:128-157](../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java#L128)、
  [:266-283](../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java#L266)）。每次都会重新复制成果目录托管集合，
  遍历全部知识成果定义、锁定列表和全部旧研究×效果，再构造新的不可变集合
  （[TechnologyAccessResolver.java:67-124](../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java#L67)）。
- 调用点包括每一次右键方块（`RightClickBlock`，
  [ResearchCommonEvents.java:122](../src/main/java/com/teammoeg/frostedresearch/handler/ResearchCommonEvents.java#L122)）、
  合成结果、`getRecipeFor`（CRAFTING 且有合成玩家时）、Create 机械合成、IE 装配机、篝火和发电机界面。
- 知识系统合入后，`addResultSources` 改成遍历整个档案的 key 再过滤 RESULT
  （[TechnologyAccessResolver.java:130](../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java#L130)），
  所以每次右键都要线性扫过全部观察。「每次查询都重建」在 V2 Phase 1 就存在，但它的代价现在随档案增长。

建议：按队伍缓存投影，在 P1-1 的 dirty 标记、知识定义或成果目录重载、锁定列表重载时失效。快照里本来就计算了一份投影，可以直接复用；
客户端已经是这样做的。

### P2-1 脉络图把每条档案观察都画成节点

[KnowledgeGraphModel.java:73](../src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeGraphModel.java#L73)
把所有有效档案条目都加入图谱，包括与任何联络无关的观察。它们都落在第 0 列，行距 82 px；几百条观察时，
「全图」缩到最小倍率 0.1 也无法辨认。设计要求「粗略视图可以将相近观察显示为一个分组节点」，并「避免将所有观察记录和全部历史同时展开」。

建议：普通视图只放参与过实际联络的观察；其余按列表已经在用的显示分组合并成一个分组节点，或者只在历史视图中出现。

### P2-2 梦境的回退主题固定为第一条，「回想」也提示有新思绪

- 回退主题取 `archive.keySet().stream().filter(active).findFirst()`，也就是最早入档的条目
  （[KnowledgeService.java:397-401](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L397)）。
  它相关规则的暗示揭完以后，所有未选主题的梦境都只会进入「回想」分支。
- 「回想」分支返回 SUCCESS
  （[KnowledgeService.java:383-389](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L383)），
  醒来仍提示「梦中浮现了一些思绪，可以到随想里回味」
  （[KnowledgeRuntime.java:75](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java#L75)），
  但随想里并没有新内容。

建议：回退主题优先选择「存在相关、且仍有未揭示暗示的规则」的有效条目，可以按日期轮换；回想时换一条消息，或者不发消息。

### P2-3 讨论邀请显示原始翻译键或空标题

[KnowledgeDiscussion.java:48](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeDiscussion.java#L48) 把
`describe(topic).title()` 这个字符串直接作为翻译参数，会被原样显示。示例包的想法标题是翻译键，所以聊天栏里会出现
`knowledge.knowledge_example.ideas.materials.title`；观察的 title 是空串，显示为空。

建议：改用 `Component.translatableWithFallback(title, title)`（`KnowledgeCommands.completeProject` 已经这样写），观察改用对象名。

### P2-4 已入档的实物再次检视会产生一条无法学习的来件

[KnowledgeService.java:175-182](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L175)
只拿物品观察与来件比较 `sameItemContent`。纸已经学进档案后，再次检视纸会生成一条新 RecordId 的来件，状态是「已有类似记录」，
只能手动移走。设计要求「同一内容的重复录入不新增相同条目。若队伍已经拥有它，显示其所在位置及当前状态」。

建议：同时与档案比较，命中时返回档案中的 key 和 `ALREADY_OWNED`。绘图台已有的 `retrieved_key` 定位逻辑可以直接复用。
[state-and-api.md](../docs/knowledge/state-and-api.md) 描述的是现状，改动后需要同步更新。

### P2-5 联络反馈不区分「新路径」与「没有变化」

- 服务端只要匹配成功就返回 `linked=true`
  （[KnowledgeWorkbenchActionPacket.java:139](../src/main/java/com/teammoeg/frostedresearch/knowledge/network/KnowledgeWorkbenchActionPacket.java#L139)），
  客户端随即追加「这条联系已记下」
  （[KnowledgeLayer.java:686-690](../src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeLayer.java#L686)）。
  第二次用同一规则联络时显示「已经记下 / 这条联系已记下」，实际上什么都没变；而通过另一条规则找到已知想法的替代路径
  （设计里有意义的事件）同样只显示「已经记下」。
- `PREVIEW_LINK` 的候选也不标注想法是否已知、路径是否已发现。

建议：`link` 返回是否新增了 discovery；预览候选附带 `known`、`discovered` 两个布尔值。

### P2-6 暗示按规则 ID 深度优先

候选规则按（产出是否已在档案中，规则 ID）排序，然后取第一条仍有未揭示暗示的规则
（[KnowledgeService.java:376-390](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java#L376)）。
结果是同一主题下按字母序把一条规则的所有层级揭完，才轮到下一条。设计要求优先「尚未充分提示的规则」。
另外，已经发现的规则（`links` 中已有记录）仍然会被提示。

建议：排序键加入「该规则已揭示的层数」（少者优先），并排除已发现的规则。这属于玩法取舍，需要设计者确认。

### P2-7 `candidateLinks` 回溯没有上界（潜在问题）

[LinkMatcher.java:58-102](../src/main/java/com/teammoeg/frostedresearch/knowledge/link/LinkMatcher.java#L58) 先对整个档案按槽位筛选，
再做回溯搜索；`limit` 只限制返回的规则数，不限制搜索步数。一条 3 槽规则如果跨条件只连接其中两个槽、每槽有上千个候选，
最坏情况是 10^9 级组合。目前没有生产调用方；研究所或居民接入前，需要加步数预算或按 tick 分片。

### P3 小问题

- `KnowledgeKey.CODEC` 只校验、不规范化。`"seasons"` 能通过 `validateKeys`（它按 `minecraft:seasons` 检查存在），但运行时按原字符串查档案永远查不到。
  另外 `new ResourceLocation` 抛出的是 `ResourceLocationException`，不是 `IllegalArgumentException`，
  [KnowledgeKey.java:39-47](../src/main/java/com/teammoeg/frostedresearch/knowledge/model/KnowledgeKey.java#L39) 与
  [KnowledgeCommands.java:129-136](../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeCommands.java#L129) 的
  catch 接不住它。建议在 codec 中规范化为 `ResourceLocation.toString()`。
- `KnowledgeEntry.resultType` 在入档时写死，`findingIds()` 等旧式访问器由它派生
  （[TeamKnowledgeData.java:124-133](../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java#L124)）。
  成果定义改了类型，或者成果在定义暂不可用时被保留进来件、之后再学习，都会留下过期或空的类型。
  权限解析已经读取当前定义，这里也应该读当前定义，去掉这个第二权威。
- 死代码：`FHKnowledgeDataSyncPacket` 的发送路径（只剩 `installClient` 被复用，
  [FRNetwork.java:57](../src/main/java/com/teammoeg/frostedresearch/FRNetwork.java#L57) 仍在注册）；
  `KnowledgeClientState.receiveReply` 处理的 `snapshot` 字段服务端从不发送
  （[KnowledgeClientState.java:57](../src/main/java/com/teammoeg/frostedresearch/knowledge/client/KnowledgeClientState.java#L57)）；
  客户端 `team_data` 镜像（见 P1-1）。
- 「暂时无法理解」只显示通用文案；设计要求显示可以公开的原因（缺哪一项基础知识）。
- 来件的状态筛选里没有 `CAPACITY`。
- 间接观察 `ObservationSessions.preview` 会静默覆盖玩家尚未确认的主动观察
  （[ObservationSessions.java:140](../src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java#L140)）。
- `KnowledgeService` 为测试保留了 `team == null` 的第二套构造器和字段，可以改成一个接收 `Runnable changed` 的构造器。

---

## 4. 需要设计者确认的偏差

`design/` 为只读，以下只列出供人类决定：

- 观察键：设计写 R，实现默认 N。
- 设计要求「每项想法至少具有一条联络规则、每项成果至少具有一条研究路径」；加载器有意不强制，以免删除路径后已学知识进入休眠。
  建议至少输出非致命的作者警告，而不是完全不提示。
- P2-6 的暗示选择顺序。

---

## 5. 建议的处理顺序

1. **P0-1**（逐元素解码 + 构造时校验）：在任何 NPC / 城镇 / 世界生成观察接入之前完成。
2. **P1-1 第 1–3 步 + P1-2**（去冗余、合并同步、缓存投影）：一起做，改动集中在 `KnowledgeSyncSnapshot`、
   `TeamResearchService.sync` 和 `TechnologyAccessResolver`。
3. **P2-3、P2-4、P2-5**：改动小，能直接改善示例包的体验。
4. **P1-1 第 4 步（等价签名）+ P2-1（图谱分组）**：在正式内容规模化之前完成。
5. **P2-2、P2-6**：与设计者确认玩法后调整。
6. **P2-7**：研究所接入时一并处理。

---

## 后续（2026-10-19）

- **P0-1 已修复**（`05f3aa8b4`，见 [diary](../diary/2026-10-18_23-54-59_knowledge-state-unreadable-entries.md)）：
  无法解码的知识条目以原始 NBT 隔离并原样写回；`Observation` 构造时校验；修复时还发现 DFU 6 的 `optionalFieldOf`
  会把解不出的值静默当作缺失，已改用严格可选字段。Chorda `CTeamDataManager` 加载失败即跳过、保存时删除文件的行为未改动，
  其他队伍组件仍可能触发。
- **P1-1 第 1、3 步与 P1-2 已修复**（`1bdf58fba`，见 [diary](../diary/2026-10-19_00-00-13_knowledge-sync-coalescing-projection-cache.md)）：
  快照去掉 `team_data`，同步合并为每 tick 每队至多一次，服务端权限投影按队伍缓存。P3 中的 `FHKnowledgeDataSyncPacket`、
  回复里的 `snapshot` 分支和客户端 `team_data` 镜像一并删除。P1-1 第 2 步（获得历史不复制元素）与第 4 步（等价签名）尚未处理。
- 两项修复的合并提交为 `0c448bb3f`。
- P2 各项按项目所有者决定暂缓：P2-7 并入研究所计划，P2-1 在增加观察来源时一并处理。
- 研究项目执行见 [计划](../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md)。
