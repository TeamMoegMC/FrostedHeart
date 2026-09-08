# 观察、实物检索与研究笔记

- Status: `Current`
- Last verified: `2026-09-08`
- Scope: 主动观察、间接记录预览、研究笔记身份和物品检索；不包含后续研究项目的证据提交规则。
- Code anchors: [
  `ObservationSampling`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSampling.java)、[
  `ObservationSessions`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/observation/ObservationSessions.java)、[
  `Observation`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/model/Observation.java)、[
  `ResearchNotes`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/item/ResearchNotes.java)、[
  `ItemRetrieval`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/item/ItemRetrieval.java)。

## 主动观察

准星指向方块或实体后持续按住观察键（源码默认 **N**）。服务端锁定该方块位置与类型，或该实体 UUID；默认持续
`ObservationSessions.DURATION_TICKS = 60` 游戏刻（20 TPS 时 3 秒）。开始时沿用玩家的实际触及范围并留 1
格移动余量，不再向方块中心重新发射射线；因此准星框中的目标不会因为中心射线撞到邻块而被拒绝。持续期间距离为
`max(MAX_DISTANCE = 8, 当前触及距离 + 2)`，兼容扩展触及距离。

观察期间世界继续运行。半透明暗色逐渐覆盖目标之外的区域，开口随被锁定目标的屏幕投影移动。一次长按只启动一次；操作系统的按键重复不会重开界面。中途松开立即取消，不显示记录预览；晚到的主动观察完成消息同样受该手势状态约束。取消确认前的下一次按压会等待确认再启动，避免关闭前一轮时干扰下一轮。进度和提示抬到物品栏及状态球上方，不再需要
Esc。

目标方块被移除或换成不同类型、实体消失、目标越界、玩家受到实际伤害或切换维度时，服务端也会取消。实体移动不会切换观察目标；同一方块类型的状态变化在完成时被采样。间接观察仍使用独立的
`PREVIEW` 消息，不要求接收者按住观察键；主动完成使用 `OBSERVED` 消息。

当前源码的轮盘默认键为 **G**；已有玩家保存的按键配置不会被覆盖，观察时的提示始终显示实际设置。

完成时服务端只采样一次，随后显示「观察记录」窗口。玩家选择收入队伍来件区或舍弃。收入来件区不等于学习。容量等接收条件不满足时，窗口显示原因并保留待确认记录，可重试或舍弃。断线清理未确认的临时预览。

## 记录内容与单位

每条新观察使用独立随机 UUID 作为 `record_id`。`Observation` 及其字段映射不可变；NBT 快照在传入和读取时复制。传播使用完整原始记录，不重新采样或换
ID。

| 字段                             | 采样依据及适用范围                                                                                       |
|--------------------------------|-------------------------------------------------------------------------------------------------|
| `object` / `type`              | 实际方块、实体或物品注册 ID；`block` / `entity` / `item`                                                     |
| `dimension`, `x`, `y`, `z`     | 目标采样位置；方块使用整数坐标，实体使用实际小数坐标，单位格                                                                  |
| `time`                         | 所在维度 `getGameTime()`，单位游戏刻，20 游戏刻 = 1 正常运行秒                                                     |
| `year`, `month`, `day`, `hour` | 由采样时的 `WorldClimate.getSec()` 建立 `WorldClockSource` 时间快照，再用 `getGameCalendar()` 获取显示日期；月份为 1–12 |
| `biome`                        | 目标采样方块坐标的生物群系注册 ID                                                                              |
| `climate`                      | `WorldClimate.getClimate(new ChunkPos(target))`，包含目标附近白幕影响；明确无事件为 `none`                        |
| `temperature`                  | `WorldTemperature.block(world, targetBlockPos)`，摄氏度；仅有限采样值有效                                    |
| `temperature_type`             | 与数值温度同一次采样；分类阈值见下文                                                                              |
| `time_period`                  | 同一 `WorldClockSource` 小时的时段分类                                                                   |
| `block_state.<property>`       | 仅方块观察，完成时实际 `BlockState` 每个属性的名称和值                                                              |
| `entity_instance`              | 仅实体观察，目标实体 UUID；不与记录 ID 混淆                                                                      |
| `item_block`                   | 仅方块物品检索，对应方块类型；不推断放置后的状态                                                                        |

`temperature_type` 的当前源码默认范围：`extreme_cold` < −20°C；`cold` [−20, 0)°C；`comfortable` [0, 30)°C；`hot` [30, 45)°C；
`extreme_heat` ≥ 45°C。这些是观察分类规则，不代替玩家健康或植物生长温度规则。

`time_period` 当前按小时分段：0–3 / 21–23 `midnight`，4–5 `predawn`，6–8 `dawn`，9–11 `morning`，12–13 `noon`，14–17
`afternoon`，18–20 `dusk`。

没有气候能力的维度中，日期、气候与时段是 `UNKNOWN`，不会伪造零或借用接收者的位置。`Observation.value(path)` 将未采集字段表示为
`UNKNOWN`，将该类型不适用的字段表示为 `NOT_APPLICABLE`。物品观察的所有环境坐标、时间、上下文、场信息均不适用。

`Observation.Source` 分开保存来源机制 `kind`、实际观察者 `originalObserver`、组织 `organization` 和记录生成位置。主动观察机制为
`player_observation`；物品检索为 `item_retrieval`。队伍本次接收方式保存在 `AcquisitionSource`，不覆盖原始来源。

## 间接观察接口

NPC、对话或遗迹系统调用 `ObservationSessions.preview(player, observation, acquisitionSource)`
，复用上述记录窗口。调用者提供已有记录便保留该记录的身份和未知字段；只有真正新生成原始记录时才创建新
UUID。方法本身不填充历史记录的缺失信息，也不自动收入来件区。

## 笔记载体与录入录出

物品 ID 为 `frostedresearch:research_note`，配方 [
`research_note.json`](../../src/main/resources/data/frostedresearch/recipes/research_note.json) 使用两张
`minecraft:paper` 无序合成一份空白笔记。空白笔记堆叠 64；含内容的笔记堆叠 1。

内容存储在 `KnowledgeNote` NBT：可选 `original_source` 保存知识的来源元数据，缺失时保持未知；连续转抄使用
`KnowledgeEntry.originalSource()` 传递原始来源，队伍本次接收的 `source()` 独立记录；`element` 是 `KnowledgeElement.CODEC`
编码，`title` 和 `remarks` 仅属于这份笔记。观察保存完整快照；想法和成果保存定义 ID 及已有标题、正文摘要。标题最多
`ResearchNotes.TITLE_LIMIT = 128` 字符，备注最多 `REMARKS_LIMIT = 2048` 字符。空标题通过知识类型与对象或定义 ID
生成确定性的可翻译标题。定义缺失时笔记仍可保存，载体不会授予权限。

绘图台调用 `ResearchNotes.read(stack)` 读取 `Content(element, title, remarks, originalSource)`，将 `element` 交给统一
`KnowledgeService.receive`；检索不消耗笔记。重复身份由来件区与档案统一处理。`ResearchNotes.create`
只创建结果，不直接扣材料；绘图台在检查权限、空白笔记和输出位置后，同时完成一份空白笔记消耗和成品写入。档案与来件区中的可录出内容均可抄录，未学习的内容保持未学习。

`ResearchNotes.canExport` 和解码入口均排除物品观察，因此不能通过制造或检索笔记绕过持有实物的要求。

## 实物检索

`ItemRetrieval.observe(player, stack)` 复制数量为 1 的实物快照，不消耗或修改原物品。默认记录物品类型，方块物品额外记录
`item_block`。它不自动复制任意 NBT、数量、世界坐标或环境数据。后续物品变化不会修改已有记录。再次检索可以生成新的原始记录，其是否适合学习由知识用途与差异规则判断。

其他模组可在初始化时用 `ItemRetrieval.registerField(itemId, field, collector)` 显式注册该物品允许采集的 `item.<field>`
字符串字段；每个采集器读取独立的数量为 1 的副本。检索获得的知识由绘图台直接收入来件区。

数据包 `data/<namespace>/frostedresearch/knowledge/item_retrieval/*.json` 经
`KnowledgeDefinitions.current().itemRetrievalRules()` 的匹配规则可通过 `nbt_keys` 明确允许复制指定顶层附加 NBT
键；匹配规则的允许键取并集。选中的内容保存为独立 `itemData` 副本，并以 `item.<key>` 的 SNBT 字符串参与条件匹配，不包含物品堆数量。

## 玩家展示（2026-09-08 CUI 更新）

观察聚焦与记录预览使用 Chorda `PrimaryLayer` / `CUIScreenWrapper`，纸页和按钮框沿用研究 UI 素材。按键提示读取实际
KeyMapping，并以物理按住状态驱动手势。主页面只显示物件译名、合并日期/地点和少量环境描述，精确位置与温度放入可展开详情，不显示原始
UUID、游戏刻或 NBT。笔记物品名也使用可读的知识或物件名称。界面细节与缓存策略以 [workbench.md](workbench.md) 为准。
