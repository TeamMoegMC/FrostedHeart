# 知识状态、API 与生命周期

- Status: `Current`（源码与本轮自动化测试核对；游戏内视觉与多人联机流程尚未人工验收）
- Last verified: `2026-11-12`
- Scope: 队伍知识身份、档案、来件、学习与忘记、理解与休眠、联络、历史、联想、队伍迁移、权限桥接及同步
- Code anchors: [`KnowledgeService`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeService.java)、[
  `KnowledgeState`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/state/KnowledgeState.java)、[
  `KnowledgeAvailability`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeAvailability.java)、[
  `KnowledgeView`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeView.java)、[
  `KnowledgeOperationEvent`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/event/KnowledgeOperationEvent.java)、[
  `KnowledgeRuntime`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeRuntime.java)

## 所有权与存储

`FRSpecialDataTypes.KNOWLEDGE_DATA` 继续使用 Chorda 组件地址 `frostedresearch:knowledge`，存于队伍的
`chorda_data/<内部队伍 UUID>.nbt`。`data/TeamKnowledgeData` 是已有调用位置的薄封装，其实际状态由
`knowledge/state/KnowledgeState` 保存。`TeamResearchData` 继续独立保存旧研究进度。

存储格式为 `schema_version: 5`（`KnowledgeState.SCHEMA_VERSION`；`TeamKnowledgeData.CURRENT_SCHEMA_VERSION` 引用同一常量），重新定义了原有
knowledge 结构，没有从旧版四个 acquired-ID 集合迁移的逻辑。新源码中的 `findingIds()` 等旧式访问器从档案派生，不再形成另一份成果权威。
`KnowledgeState.read` 先读取 `schema_version` 并交给 `KnowledgeState.upgrade`，这是存储格式迁移的唯一入口；版本 2 是知识系统的第一版布局；版本 3 把研究记录的单个 `idea` 迁移为去重排序的 `origins` 列表，保留 UUID、时间、结果与全部进度。迁移在副本上执行，已有 origins 优先；缺版本也兼容旧 idea。缺少来源而其他字段可读时保留进度与空来源。版本号高于当前值时按现有格式解码，`write()` 始终写入当前版本。

| 保存项            | 含义                                           |
|----------------|----------------------------------------------|
| `archive`      | `KnowledgeArchive` 已学习条目，按 `KnowledgeKey` 索引 |
| `inbox`        | `KnowledgeInbox` 已收到、尚未学习的条目，独立于档案           |
| `links`        | 以规则 ID 去重的首次实际联络，保存输出、命名输入绑定和时间              |
| `research`     | 每次启动的 UUID、项目定义 ID、实际来源知识列表 `origins`、完成状态与输出            |
| `acquisitions` | 实际获得经历；不会补造外部知识的推导关系                         |
| `hints`        | 已揭示的提示键或原文，同一内容不重复累计                         |
| `daily`        | 梦境及玩家与讨论对象的逐日额度                              |
| `dream_topics` | 玩家选择的队伍知识主题                                  |
| `initialized`  | 初始知识是否已经发放；重新加入队伍不重复发放                       |

`KnowledgeKey` 将类型和身份一起保存。观察身份是原始记录 UUID，想法与成果身份是数据包 `ResourceLocation`。`KnowledgeEntry`
保存内容、当前队伍实际获得来源 `source`、沿笔记保留的最初来源 `originalSource`、学习时间及成果类型摘要。观察本身的原始观察者、组织与来源位置在不可变
`Observation.Source` 中，接收笔记不会改变它。

原始记录、知识摘要和实际历史不因数据包重载改写。`KnowledgeState` 的存储写方法供知识服务和持久化桥接使用，游戏系统应通过下述服务提交操作。

### 无法解码的条目

`KnowledgeState.read` 不抛出异常，单条记录无法解码不会再让整个队伍文件加载失败。`archive`、`inbox`、`links`、`research`、`acquisitions`
逐条解码；`hints` 的规则键（须为 `ResourceLocation`）与 `dream_topics` 的玩家键（须为 UUID）连同各自的值逐键解码。无法解码的条目以原始 NBT
按原分区保存在 `KnowledgeState` 内部的 `unreadable` 中：

- 不进入档案、来件或其它运行时状态，因此不参与查询、学习、联络、同步视图与权限；
- `write()` 将其原样写回原分区：列表条目追加在有效条目之后；键值条目只在同一键没有有效条目时写回，有效条目优先；
- 随保存/加载往返、`replaceState`（包括 `TeamKnowledgeData.CODEC` 解码与 `copy()`）和 `merge` 保留，合并时相同的原始条目只保留一份；
- 每次加载最多一条 `FRMain.LOGGER` 警告，给出条数以及第一条的位置（如 `archive[1]`、`hints[<键>]`）和原因。

以后能解码它们的版本会在原位置重新读到这些条目。分区本身不是预期的 NBT 类型时（只可能来自手工编辑），列表分区整体作为一条保留，键值分区按空处理。

MC 1.20.1 使用的 DFU 6 会把 `optionalFieldOf` 中“存在但解码失败”的值当作缺失。为免一条观察因单个值无法读取而被清空全部值、再按清空后的样子写回，
`KnowledgeElement.observation` 与 `Observation.values` 使用 `KnowledgeKey.strictOptionalField`：编码不变，存在但无法解码的值使整条记录进入
`unreadable`，日志给出的也是内层的真实原因。

`Observation` 紧凑构造器执行与解码相同的规则：字段已知且类型一致；不适用的字段只能记为 `not_applicable`；值的载荷与状态、类型一致且数值有限；
只有物品观察可以带 `item_data`。违反时抛出 `IllegalArgumentException`，非法观察在产生处就被拒绝，不会写进存档；解码同样经过构造器，失败时返回
codec 错误而不是异常。`ObservationSampling` 把非有限的坐标与温度记为未采集。

知识组件因此不再触发 Chorda 的整队丢弃。但 `CTeamDataManager.load` 在任一组件加载失败时仍会跳过该队伍文件，`save()` 随后删除没有已加载队伍的
`.nbt` 文件；这属于 Chorda，尚未改动。

## 统一操作

玩家入口为 `KnowledgeService.forPlayer(ServerPlayer)`；城镇、研究或队伍机制使用 `forTeam(TeamDataHolder, gameTime)`
。两者均针对服务端当前队伍。时间参数的单位是游戏 tick，用于获得与历史排序；每日梦境和讨论额度另用统一世界日期。

| 方法                                             | 行为                                        |
|------------------------------------------------|-------------------------------------------|
| `receive(element, source)`                     | 收入来件；同一身份在档案或来件中已经存在时返回所在状态，不新增           |
| `receive(element, receipt, originalSource)`    | 传播内容时，分别保存接收来源和最初来源                       |
| `learnInbox(key)` / `learnInbox(Collection)`   | 从来件尝试学习，每条分别返回 `OperationResult`          |
| `learn(element, source, Grant)`                | 普通或显式特殊学习；成功加入档案并移除同身份来件                  |
| `produce(element, source)`                     | 联络或研究产出的正常学习；想法、成果未能学习时仍保存来件              |
| `forget(key)`                                  | 移除档案中的指定知识，保留实际历史，不沿推导关系级联                |
| `deleteInbox(key)` / `deleteInbox(Collection)` | 只删除选定来件，不销毁物品、档案和历史                       |
| `query(key)`                                   | 分别返回 `inArchive`、`inInbox`、`active` 与状态原因 |
| `isActive(key)`                                | 当前是否具有有效的档案知识；不使用来件或历史替代                  |
| `canExport(key)`                               | 检查源条目、可传播性及扩展权限；不消耗笔记                     |
| `previewLinks(keys)`                           | 返回当前选择完整命中的全部规则及实际绑定，不修改状态                |
| `link(ruleId, keys)`                           | 内部完成接口；只匹配指定规则、检查必要前置、记录实际关系并产生想法                      |
| `candidateLinks(limit)`                        | 面向研究所的候选查询；只用有效档案输入，按槽位筛选和回溯，每条规则返回首个可行绑定 |
| `merge(sourceState)`                           | 由队伍系统明确发起的知识合并；加入、退出队伍不调用                 |

`Grant.NORMAL` 检查独立理解条件。`Grant.INITIAL` 与 `Grant.COMMAND` 明确标记特殊来源，跳过理解条件，仍经由定义/用途判断、状态提交、事件与同步。初始知识来自
`frostedresearch/knowledge/initial/*.json`，每个队伍初始化一次。缺失或停用的知识定义不能正常学习。

物品观察的 `receive` 另按 `Observation.sameItemContent` 比较物品类型、采集字段和允许的附加数据；来件中已有相同内容时返回其已有身份和
`ALREADY_RECEIVED`，不产生新来件或获得事件。该比较忽略新 RecordId
和记录者，不改变普通世界观察的独立身份规则。研究笔记物品不能作为物品观察录入，其已有内容仍可通过笔记研读收入来件。

常用失败状态为 `ALREADY_OWNED`、`ALREADY_RECEIVED`、`NO_NEW_DIFFERENCE`、`NO_USE`、`NOT_UNDERSTOOD`、
`DEFINITION_UNAVAILABLE`、`CAPACITY`、`CANCELLED` 与 `NOT_FOUND`
。重复学习不会产生成功事件；被拒绝或取消的学习不改动原有来件。批量学习/删除分别执行条件判断与事件，并将队伍同步合并为一次。

普通玩家界面没有档案删除按钮；忘记由游戏系统或行政指令调用。

## 学习条件与休眠

想法、成果的 `understanding` 是学习时检查的基础知识集合，全部成员都必须是当前有效档案知识。它与联络输入、研究生产路径独立。学习后不重复检查理解条件，因此忘记基础知识不会自动撤销已学习的其他知识。

观察只要可能匹配任一当前注册用途，就有学习用途，不要求队伍已经收集完整组合。观察重复判断由
`KnowledgeDefinitions.Snapshot.addsObservationDifference` 执行，同时考虑原始身份、规则槽位、跨记录字段、独立数量以及外部
`ObservationUse`。无法证明可替代的记录保留为不同记录。

想法、成果的定义缺失或停用时，档案身份仍然保留，但 `active=false`
。观察没有当前注册用途时同样休眠。定义或用途恢复会自动恢复有效性，不要求重新学习。单独删除生产规则不让仍存在的想法定义休眠。

观察容量由服务器配置指定：

| 配置键                            | 源码默认值 | 单位与范围                             |
|--------------------------------|------:|-----------------------------------|
| `knowledgeArchiveObservations` |  4096 | 每队档案原始观察条数，`0..Integer.MAX_VALUE` |
| `knowledgeInboxObservations`   |   512 | 每队来件原始观察条数，`0..Integer.MAX_VALUE` |

分组不减少数量。达到容量会返回原因，不删除已保存记录。降低配置、合并队伍都保留超过容量的已有记录，随后限制新增。想法和成果没有观察容量限制，因此已完成联络和研究的产出不会因观察区满而丢失。

## Forge 事件

`KnowledgeOperationEvent.Check` 可取消，`Committed` 只在操作实际提交后发布。事件包含队伍 UUID、可选操作者、操作名称、知识元素与获得来源。

操作名包括 `receive`、`learn`、`learn_initial`、`learn_command`、`forget`、`delete_inbox`、`export`，以及产出保留通知
`receive_produced`。其中 `export` 是复制前的权限检查，实际物品提交由绘图台槽位操作完成；`receive_produced`
保证已经形成的产出被保存，不是第二次普通学习。

默认允许当前成员共同处理来件和笔记，绘图台另要求队伍归属。其他系统可通过前置事件提供具体权限或学习限制；观察的原始来源、实际获得来源和特殊授予方式均可区分。

## M2 行为事件与短联络

代码锚点：[IdeaAcquisitionService](../../src/main/java/com/teammoeg/frostedresearch/knowledge/IdeaAcquisitionService.java)、
[KnowledgeBehaviorBridge](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeBehaviorBridge.java)、
[KnowledgeBehaviorEvent](../../src/main/java/com/teammoeg/frostedresearch/knowledge/event/KnowledgeBehaviorEvent.java)、
[KnowledgeLinkSessions](../../src/main/java/com/teammoeg/frostedresearch/knowledge/KnowledgeLinkSessions.java)。

| 服务器配置（源码默认） | 含义 |
|---|---|
| knowledgeBehaviorChance = 0.15 | 未指定 chance 的行为基础概率，0–1 |
| knowledgeBehaviorOptionalBonus = 0.1 | 每项已掌握 optional 的加概率，0–1 |
| knowledgeLinkTicks = 200 | 基础联络等待，1–72000 tick；20TPS 时 10 秒 |
| knowledgeLinkOptionalReduction = 20 | 每项已掌握 optional 减免，0–72000 tick |
| knowledgeLinkMinimumTicks = 20 | 最短等待，1–72000 tick，并不超过基础等待 |

行为概率 p = min(1, p0 + n × bonus)，p0 为想法 chance 或基础配置；n 为有效档案中满足的 optional 项数。
联络等待 ticks = max(min(base, minimum), base − n × reduction)。可选知识不需要摆在桌面上；等待在开始时固定，
中途得到更多参考不改变本次时间，必要知识与所选规则则持续校验。合法等待完成不抽概率。

物品获得桥接只有两处：实际 ItemPickupEvent 的取得堆叠，以及 AbstractContainerMenu.clicked 前后背包＋光标＋玩家工作网格原料的
物品/NBT 净增量（同次点击聚合）。因此制作、熔炉输出、外部容器普通/快捷取出可触发，背包内部、背包/光标/玩家工作网格原料之间的
单纯搬动不触发；不再同时监听 crafted/smelted 抽第二次。一次 shift-click 的批量产出是一次操作，非每个物品一次。
这是玩家实际取出路径；绕过这些原版入口的其他模组直接物品授予可主动调用公开 API。

useItem 仅在服务器接受的成功动作触发（食物在开始使用时，非每个持续 tick）；实际 ItemStack.useOn 的成功物品使用
与成功 useItemOn 方块交互按同一操作合并再评估，失败/PASS 不触发。空手开关方块只有 block_interacted。
行为物品载荷取样用 ItemRetrieval.behaviorSample，允许研究笔记物品本身，不解析或学习其内含知识；
主动检视仍走 observe/canObserve 和既有笔记研读规则。

ObservationSessions 完成有效采样时直接发 observation_completed，早于接收/学习；即使档案已有等价记录或来件不能接收，
动作仍可供行为和后续任务监听。仅从旧记录学习不会补发观察动作。KnowledgeService 实际 learn/learn_INITIAL/learn_COMMAND
提交后独立发 knowledge_learned；同一线程用队列排空级联，每个队伍/想法在这次级联中最多尝试一次，且档案/来件已有输出
直接跳过，防止 A 学到 B、B 又学到 A 的无限触发。事件 ID 索引在定义加载时生成，不逐事件扫描全想法目录。

外部服务器调用（Java/KubeJS Java.loadClass 也可用）：

```java
IdeaAcquisitionService.emit(player, new ResourceLocation("example:field_work"), Optional.empty());
// 或传 Optional.of(KnowledgeElement.observation(record)) / idea / result 作为有类型上下文。
```

API 携带真实玩家、队伍与事件 ID；KnowledgeBehaviorEvent 提供 team() / actor() / id() / subject()，供实际采样动作的
其他消费者监听；不是通用脚本执行器。fire(service, signals, random) 是一次操作的批量入口，同操作多个 signal 对同想法只抽一次。

KnowledgeLinkSessions 是每玩家的临时短计时，不写存档：关闭绘图台容器、离线、换队、必要知识/规则失效会取消；
容器内翻页或返回旧研究页仍继续，重开已关闭容器须重新选择并从零计时。重复开始不重置现有计时，取消后不能完成旧请求。
每 tick 只校验选定 rule ID，不重算全部候选。进度通过 LINK_PROGRESS 每20 tick 推送，完成/取消另发状态。
START/LINK 完成入口仍服务端重新判定，不相信客户端进度。

## 实际联络与研究历史

联络使用作者显式配置的 0–5 个不同身份、当前有效的档案元素。输入顺序无关，不消费输入，也不忽略额外输入。匹配规则与槽位条件见 [定义文档](definitions.md)
。玩家预览可得到多个输出，确认一条后服务端重新匹配并开始短计时，完成时再次校验；不是立即发放。

每队每条规则默认只保存首次成功绑定。既有不同规则的替代路径历史保留；手动联络仍允许输出已在档案或来件的规则形成新路径，同一规则反复请求保留首次绑定，不追加历史或再次产出。外部学习不创建本队未执行的联络。

### 研究启动记录

`ResearchHistory` 是一次启动：实际来源知识列表 `origins`、启动时间、`completed` / `abandoned` 状态、完成时的成果，以及按需求 `id` 保存的进度——
`calculations` 记录每局获胜的难度，`evidence` 记录每个槽位提交的观察副本，`experiments` 记录每项实验完成的次数。空进度不写入存档；进度存在但无法解码时整条启动记录按
[无法解码的条目](#无法解码的条目) 隔离，不会以丢失证据的样子加载。

| 方法                                                     | 行为                                                                                                        |
|--------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `researchStartStatus(project)` / `projectAccess(project)`                   | 返回 `LEARNABLE` 或不能开始的原因：定义不存在、未发现或缺少开始知识、已有进行中的启动（`ACTIVE`）、已完成且全部成果都在档案中（`ALREADY_OWNED`）                |
| `startResearch(project)`（旧起源参数重载仅为兼容）                         | 状态为 `LEARNABLE` 时记录新启动并返回 UUID。每队每个项目同时最多一次进行中的启动；完成后只有当某项成果不在档案中（例如被忘记）才能再次启动                         |
| `openRun(project)`                                     | 该项目进行中的启动                                                                                                 |
| `submitEvidence(run, requirement)`                     | 在档案与来件的全部观察中寻找一组满足该证据需求的互异记录，并保存其副本。搜索至多尝试 `EVIDENCE_SEARCH_STEPS = 20000` 个候选，找不到返回 `NO_MATCH`         |
| `submitEvidence(run, requirement, keys)`               | 按给定记录精确绑定，每个槽位一份                                                                                          |
| `calculationLevel(run, requirement)`                   | 该计算需求仍需要的纸牌局难度；已满足或启动已结束时为空                                                                               |
| `recordCalculation(run, requirement, level)`           | 记录一局胜利；难度低于要求返回 `INVALID_INPUT`，已满足返回 `ALREADY_OWNED`                                                     |
| `experimentRequirement(run, requirement)`              | 进行中启动里仍未满足的实验需求定义；已满足、启动已结束或不是实验时为空。实验台据此选用和运行                                                      |
| `recordExperiment(run, requirement)`                   | 记录一次完成的实验；已满足返回 `ALREADY_OWNED`，没有这项实验或启动已结束返回 `NOT_FOUND`                                           |
| `requirementMet(run, requirement)` / `requirementsMet(run)` | 按当前定义重新判断。没有需求的项目对 `requirementsMet` 永远不成立                                                              |
| `finishResearch(run)`                                  | 玩家路径：全部需求满足时调用 `completeResearch`，否则返回 `REQUIREMENTS_UNMET`                                                  |
| `abandonResearch(run)`                                 | 结束进行中的启动，清空已提交进度；记录保留为已放弃，脉络历史视图可见                                                                        |
| `completeResearch(run)`                                | 研究系统接口与开发指令使用，不检查需求。使用项目定义的成果集合；带显式列表的重载要求与声明集合相符                                                         |

进度每次按当前定义判断：证据副本必须仍能完整绑定当前槽位与条件，计算只计难度不低于当前 `level` 的胜局，实验按完成次数与当前 `count` 比较。作者修改需求后，玩家可能需要重新提交。
证据可以来自档案或来件，提交不消耗观察；之后删除来件或忘记档案条目不影响已提交的副本。同一观察可以用于不同项目，同一需求内各槽位必须是不同记录。

每个启动记录只有一个实际来源知识列表 `origins`，完成重复调用不会重新产出。获得成果失败时保留来件，研究关系仍属于实际历史。居民参与与原型数值升级尚未实现，
见 [研究项目执行计划](../../plans/2026-10-18_23-42-16_knowledge-research-project-execution.md)。

## 梦境、讨论与暗示

`setDreamTopic(playerUUID, key)` 仅保存有效档案主题。`KnowledgeRuntime` 在 `SleepFinishedTimeEvent` 中，对实际睡眠并已达到
100 tick 的玩家触发 `dream`。未选择或主题已不可用时，从当前有效档案中选择主题；没有相关提示不强行产生。

日期使用主世界 `WorldClimate` 的 `WorldClockSource` 秒数除以 `secondsPerDay=1200` 得到，单位是该统一时钟的游戏日，不直接用玩家所在地的
`/time` 值。每位玩家每日一次成功梦境联想，每位玩家每日每个讨论对象一次成功讨论联想。`KnowledgeState.addHint`
全局去重同一提示键/原文；初始化与重载时 `reconcileHints`
将旧的原文阶段对应到当前规则提示，避免改成翻译键后又保存一份。梦境在相关提示已经掌握时可以回想旧思路，但只更新每日额度，不追加提示副本。客户端还对最终显示文本去重。

`KnowledgeDiscussion.invite` 要求双方在同一维度、相距不超过 8 格且都掌握主题。邀请保存双方队伍与主题，60 秒内由接收者点击
`/knowledge discuss accept` 确认；完成时重新检查双方状态，各自使用自己的队伍档案联想。邀请不会复制任何知识。居民流程在自身教育与交互判定完成后调用
`KnowledgeService.discuss(playerUUID, residentUUID, topic, day)`。

规则候选优先产生当前未知想法的路径，再考虑已知想法的其他路径；同级使用规则 ID 的稳定顺序。客户端仅看到已揭示文本。

## 权限桥接与旧研究

`TechnologyAccessResolver` 合并新知识成果和旧研究已完成效果：Design 对应配方，Construction 对应多方块成型，Procedure
对应方块操作。新权限只读取档案中当前可解析、启用的成果；来件、笔记摘要、获得历史和忘记后的引用均不授予权限。Finding
的信息查询同样来自有效成果。

现有 `ResearchResultCatalog` 的成果仍可作为定义后备；同 ID 的新 `ResultDefinition` 优先，明确停用会覆盖后备。旧研究的效果、完成条件和小游戏没有改为新知识前提。

新 Prototype 是可以学习和传播的成果身份，重复学习幂等。本轮没有实现数值升级、叠加或安装。已有 `/research result grant`
的旧物理原型制造分支仍独立保留；`/knowledge grant result <id>` 授予知识身份，不制造额外原型物品。

## 生命周期、同步与命令

队伍创建和读取时初始化知识；玩家登录、换队、知识状态变更以及数据包重载后标签最终同步时发送对应队伍的可见状态。队伍成员变化只改变查询归属，不自动复制档案。
`merge` 按知识身份、规则 ID 和实际研究 UUID 合并，重新按当前定义计算有效性，保留超容量内容。

`TeamResearchService.sync(team)` 只把队伍标记为待同步，并丢弃该队伍缓存的权限投影。知识服务的每次提交、旧研究访问效果的授予或重置、换队与
`/reload` 都只做这一标记。`KnowledgeRuntime` 在 `TickEvent.ServerTickEvent` 的 `END` 阶段调用 `TeamResearchService.flush()`
，为每个待同步队伍构造一次快照，发给已建立网络连接的在线成员；无人在线的队伍只清除标记，不构造快照。某个队伍构造失败只记录错误，不影响其他队伍。
批量学习/删除、联络产出、研究完成和初始知识发放因此各只发送一次快照。

两条路径不等 tick 结束：

- 登录与绘图台 `REFRESH` 调用 `TeamResearchService.send(player, team)`，立即给该玩家发送当前快照；队伍正待同步时，同一份快照也发给其他在线成员并清除标记。
- 绘图台其他操作在发送回复前调用 `TeamResearchService.flush(team)`。客户端总是先装入更新后的视图再处理回复，例如 `RETRIEVE` 按
  `retrieved_key` 聚焦新来件。

首次登录的知识初始化与同步在 `PlayerLoggedInEvent` 的 `LOWEST` 优先级执行，晚于 Architectury/FTB Teams 建立玩家队伍；登录只发送这一份知识快照。
`OnDatapackSyncEvent` 携带单个玩家时属于更早的登录阶段，不在此处读取队伍；其玩家参数为空时才标记 `/reload`
后已在线的队伍。更早运行的 `ResearchHooks.ServerReload` 此时标签尚未最终安装，只清除权限投影缓存，不为任何队伍构造快照。新世界中首次进入的玩家因此不需要预先存在队伍或知识存档。

服务端权限投影按队伍缓存：`TechnologyAccessResolver.project(team)`、`projection(teamId)` 及其布尔适配器读取同一份缓存，没有队伍的假玩家和机器共用一份。
缓存项记下构造时的 `KnowledgeState` 实例与 `mutationRevision()`、`TeamResearchData` 实例，以及 `ResearchResultCatalog`、
`KnowledgeDefinitions` 的当前快照，任一不同即重建。因此知识变更（包括 `TeamKnowledgeData` 的旧式直接写方法）、成果目录与知识定义安装不需要另行失效。
旧研究进度没有修订号，由上述同步标记失效；`ResearchHooks.reload`、`FHResearch.reindex`（效果重新声明全局锁定列表）以及 `ServerReload`
末尾（重新核对各队旧研究进度之后）清空全部缓存。队伍待同步期间的读取每次重新计算、不写入缓存，直到 flush，以免缓存同一操作的中途状态，例如可重复研究在授予后立即重置。
快照构造复用这份缓存投影。

服务端 `KnowledgeView`
包含档案、来件、已揭示暗示、实际联络、实际研究和获得经历。未发现规则的全部输入条件不进入快照。正常图谱中的联络必须仍能以历史绑定满足当前规则，并且其实际输入/输出有效；历史视图可以查看失效路径和忘记后的引用。数据包重载不重新编造历史绑定。

可见快照还包含 `projects`：列出当前 access 发现的项目，以及已有历史的项目；active 只表示当前发现或存在 open run，纯历史项目在普通视图隐藏。包含经过组披露裁剪的 sources、access_conditions、groups 与成果说明，供未启动项目展示。它与 `research`
中的实际启动/完成记录分开。项目完成后的多项成果共用一次最终快照，分别保留其学习事件和结果。

`KnowledgeSnapshotPacket` 将 `KnowledgeSyncSnapshot`（`catalog_revision`、发现投影 `knowledge`、权限投影 `technology` 与可见视图
`archive_view`）的 NBT 压缩后按 128 KiB 分片。持久化的队伍状态不进入快照，客户端也不保存它的镜像。客户端收齐一轮后一次替换两份投影和可见视图；传输期间继续显示上一份完整状态。这样容量配置允许的观察档案不会被普通单包
NBT 大小限制截断。绘图台回复只携带本次操作结果与联络候选，不再次复制整个档案。

行政操作：

```text
/knowledge grant idea|result <定义ID>
/knowledge forget observation|idea|result <记录UUID或定义ID>
/knowledge status observation|idea|result <记录UUID或定义ID>
/knowledge merge <来源队伍UUID>
/knowledge research complete <项目ID> [起源想法ID]
```

授予、忘记、合并需要权限等级 2。普通成员可用 `/knowledge dream <类型> <身份>` 设置主题，用
`/knowledge discuss invite <玩家> <类型> <身份>` 邀请并通过 `/knowledge discuss accept` 确认。想法与成果按定义 ID
读取，即资源位置，不需要引号，并补全已加载的定义；观察按记录 UUID 读取。项目不是知识身份，不能授予或忘记；要重新开始已完成的项目，
忘记它的一项成果即可。指令生成历史观察由 `Observation` API 明确提供内容和 `command_generated`
来源，不会使用一个缺乏历史内容的裸 UUID 伪造观察。

`research complete` 是权限等级 2 的开发命令，跳过项目需求。它沿用该项目进行中的启动；没有时检查正常 access；纯成果入口无需想法参数，再调用
`startResearch` / `completeResearch`，不会把研究项目当作新的知识元素或直接伪造成果权限。项目已完成且成果都在档案中时提示无法开始。示例操作见
[playthrough.md](playthrough.md)。

## M3-A 实物进度（schema 4）

ResearchHistory.samples 为可选任务回执映射，记录 sample.experiment 与 sample.item；旧 v3 缺席读为空，既有进度不变。
KnowledgeService.sampleTargets(experiment) 只列当前开放且未满足的 sample 任务；submitSample(run, requirement, stack)
检查真实物品和 ResearchSampleBinding 后消耗1件并写回执；重复/错误样品不扣物。finishResearch 继续统一检查所有任务。
更改绑定语义、设备生命周期与绘图台操作见[小炉与实物提交](trial-kilns-and-samples.md)。

## 旧研究承接与完成等价查询（M3-B）

[`LegacyResearchBridge`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/LegacyResearchBridge.java)
仅在当前有效 `legacy_replacement` 存在时接替旧入口。`KnowledgeService.initialize` 在队伍加载、登录和最终数据包
同步时执行 reconcile；没有逐 tick 重试。最终 OnDatapackSyncEvent 全局分支先刷新映射有效性，即使零在线玩家也执行；
ServerStarted 也刷新最终目标，ServerStopped 清理静态缓存。配方可用性改变不需要知识定义 revision 改变。
旧定义 JSON 可以继续保留，桥接不删除定义或旧记录。

- 旧 raw finished=true：使用正常 `KnowledgeService.learn` 和来源 `legacy_research:<旧ID>` 承接两成果，
  不要求旧 effectData 已领取；不会调用 grantEffects、发旧实物奖励或创建 ResearchHistory。
- 仅有某个 effectData=true：只导入对应成果，不宣告项目完成。
- 未完成但 active=true、committed>0 或有已完成 clue：原材料投入、点数和线索全保留，一次 produce 新想法，
  正常入档失败时保留来件；不折算为实验、样品或新项目完成。
- `TeamResearchData.variants["frostedresearch:legacy_replacements"][<项目ID>]` 保存 imported_results 字符串集合、
  completed_credit 与 partial_idea_granted。成果仅在成功/确实已入档后写回执，取消的成果在下次生命周期重试；
  已导入再主动遗忘不会重新强发。completed_credit 只来自旧 finished，是承接凭据，不能当作实验历史。

带 completed_credit 的当前有效替换项目可见，`start_status=LEGACY_COMPLETED`、`completion_source=legacy`，
即使没有想法也显示“旧研究承接完成”并免重复启动。已有真实 open run 保留，视图状态为 legacy_superseded；
它不再接受计算、实验、样品或证据提交，也不能伪造完成。原始历史不会删掉。

旧项目的 commitItem、服务端 setCurrentResearch、doResearch、两种 setClueCompleted、checkResearchComplete
统一拒绝正常推进；初始化/重载清除旧当前选择并解除线索订阅，但不清 active/committed/clueData。
显式行政 setResearchFinished 仍保留原语义。

`isPrerequisiteCompleted(team, legacyId)` 判定为旧 raw finished，或当前有效映射的真实 completed run 加两成果
均有效入档。单靠外来两成果只能获得技术权限，不能伪装完成来开旧后继。旧 Research.isUnlocked 和 commitItem
都用这一等价查询；客户端消费服务端 legacy_replacements 中的 effective_completed，不自行推断。
新成果遗忘/学习和真实完成通过 TeamResearchService.sync 使权限与旧父项投影一起刷新；真实旧来源仍独立保留。

移除映射/新包后，旧入口与原进度恢复；回执保留但当前不投影为新完成，不做反向迁移。
`KnowledgeService.merge(TeamDataHolder)` 同时合并知识和这一个回执子键的集合/布尔并集，管理 merge 命令使用此入口。
仅合并 KnowledgeState 的重载不带旧数据，供已有纯知识调用使用。其它 variants 不受回执合并影响。

## 阶段状态与新鲜观察（M4）

ResearchHistory新增stage_state（StageRunState），保存order/current/visits/completed_tasks；旧四类进度maps和samples不变。
KnowledgeState.upgrade对schema4及更早记录补main占位，定义暂缺时延后填任务；read/write不会靠缺内容删进度。
有序多阶段旧存档首次采用首阶段，只有同ID任务保留原进度，不按进度百分比跳过；保存顺序不跟随定义重排。

KnowledgeService统一当前选定任务的计算、证据、实验和样品提交；新recordMinigameWin/recordObservationTask也走同一进度更新。
任务成功同次服务操作记录ready及completion_recipes；advanceStage(run,expectedStage)单独手动推进，最后finishResearch产出成果。
临时来源TemporaryResearchSource按project/run/stage归属，与成果/旧来源/其它run取并集，结束只撤自身。

ObservationSessions完成真实采样后，在preview/receive之前调用observeResearchTarget；现场BlockPos或已核验ItemStack只用于本次动作。
试验样品读取既有ResearchSampleBinding；世界试件查询PendingResearchProofs，均核对当前队伍/run/task/experiment。
无需接受或学习观察，重复记录、主动丢弃或来件容量不决定任务成功。详见 [阶段与风道](stages-and-airflow.md)。

旧平铺run改成多阶段时，仅首阶段同ID任务进入活动进度。其余旧计算/证据/实验/样品原值存于可选
stage_state.legacy_progress（LegacyStageProgress），不改写原任务ID，不把它们算成未来阶段的新工作；codec往返保留。
这是schema5内部的迁移档案字段，不新增TWR定义字段。legacy_superseded既有run及合并承接回执都会清本run的pending proof。
