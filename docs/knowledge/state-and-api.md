# 知识状态、API 与生命周期

- Status: `Current`（源码与本轮自动化测试核对；游戏内视觉与多人联机流程尚未人工验收）
- Last verified: `2026-09-08`
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

本轮存储格式为 `schema_version: 2`，重新定义了原有 knowledge 结构，没有从旧版四个 acquired-ID 集合迁移的逻辑。新源码中的
`findingIds()` 等旧式访问器从档案派生，不再形成另一份成果权威。

| 保存项            | 含义                                           |
|----------------|----------------------------------------------|
| `archive`      | `KnowledgeArchive` 已学习条目，按 `KnowledgeKey` 索引 |
| `inbox`        | `KnowledgeInbox` 已收到、尚未学习的条目，独立于档案           |
| `links`        | 以规则 ID 去重的首次实际联络，保存输出、命名输入绑定和时间              |
| `research`     | 每次启动的 UUID、项目定义 ID、实际起源想法、完成状态与输出            |
| `acquisitions` | 实际获得经历；不会补造外部知识的推导关系                         |
| `hints`        | 已揭示的提示键或原文，同一内容不重复累计                         |
| `daily`        | 梦境及玩家与讨论对象的逐日额度                              |
| `dream_topics` | 玩家选择的队伍知识主题                                  |
| `initialized`  | 初始知识是否已经发放；重新加入队伍不重复发放                       |

`KnowledgeKey` 将类型和身份一起保存。观察身份是原始记录 UUID，想法与成果身份是数据包 `ResourceLocation`。`KnowledgeEntry`
保存内容、当前队伍实际获得来源 `source`、沿笔记保留的最初来源 `originalSource`、学习时间及成果类型摘要。观察本身的原始观察者、组织与来源位置在不可变
`Observation.Source` 中，接收笔记不会改变它。

原始记录、知识摘要和实际历史不因数据包重载改写。`KnowledgeState` 的存储写方法供知识服务和持久化桥接使用，游戏系统应通过下述服务提交操作。

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
| `link(ruleId, keys)`                           | 重新判定所选规则，记录实际关系并产生想法                      |
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

## 实际联络与研究历史

联络使用 2–5
个不同身份、当前有效的档案元素。输入顺序无关，不消费输入，也不忽略额外输入。匹配规则与槽位条件见 [定义文档](definitions.md)
。玩家预览可得到多个输出，确认一条后由服务端重新匹配。

每队每条规则默认只保存首次成功绑定。同一想法经不同规则产生会增加替代路径；同一规则反复尝试不会无限追加历史。外部学习不创建本队未执行的联络。

`startResearch(project, idea)` 要求实际起源想法有效且声明可以启动该项目，返回本次启动 UUID。`completeResearch(run)`
使用项目定义的成果集合；带显式列表的重载要求与声明集合相符，不能用任意输出替代项目定义。每个启动记录只有一个实际起源想法，完成重复调用不会重新产出。获得成果失败时保留来件，研究关系仍属于实际历史。

这些是研究系统接入接口。本轮没有实现新研究的计算、证据、实验执行器，也没有把旧研究项目强行映射成新项目；新研究任务、居民工作时间、实验设施和原型数值升级属于设计文档明确后续展开的部分。

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

队伍创建和读取时初始化知识；玩家登录、换队、知识状态变更、数据包重载和标签最终同步时发送对应队伍的可见状态。队伍成员变化只改变查询归属，不自动复制档案。
`merge` 按知识身份、规则 ID 和实际研究 UUID 合并，重新按当前定义计算有效性，保留超容量内容。

首次登录的知识初始化与同步在 `PlayerLoggedInEvent` 的 `LOWEST` 优先级执行，晚于 Architectury/FTB Teams 建立玩家队伍。
`OnDatapackSyncEvent` 携带单个玩家时属于更早的登录阶段，不在此处读取队伍；其玩家参数为空时才处理 `/reload`
后已在线队伍的同步。新世界中首次进入的玩家因此不需要预先存在队伍或知识存档。

服务端 `KnowledgeView`
包含档案、来件、已揭示暗示、实际联络、实际研究和获得经历。未发现规则的全部输入条件不进入快照。正常图谱中的联络必须仍能以历史绑定满足当前规则，并且其实际输入/输出有效；历史视图可以查看失效路径和忘记后的引用。数据包重载不重新编造历史绑定。

可见快照还包含 `projects`：仅列出档案中想法已经公开的项目、可用来源与成果说明，供未启动项目展示。它与 `research`
中的实际启动/完成记录分开。项目完成后的多项成果共用一次最终快照，分别保留其学习事件和结果。

`KnowledgeSnapshotPacket` 将完整的 `KnowledgeSyncSnapshot` NBT 压缩后按 128 KiB
分片。客户端收齐一轮后一次替换档案、技术投影和可见视图；传输期间继续显示上一份完整状态。这样容量配置允许的观察档案不会被普通单包
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
`/knowledge discuss invite <玩家> <类型> <身份>` 邀请并通过 `/knowledge discuss accept` 确认。含冒号的 ID
用命令字符串参数，必要时加双引号。指令生成历史观察由 `Observation` API 明确提供内容和 `command_generated`
来源，不会使用一个缺乏历史内容的裸 UUID 伪造观察。

`research complete` 是权限等级 2 的临时开发命令，只跳过未实现的研究任务。它要求队伍拥有实际起源想法，并调用
`startResearch` / `completeResearch`
，不会把研究项目当作新的知识元素或直接伪造成果权限。示例操作见 [playthrough.md](playthrough.md)。
