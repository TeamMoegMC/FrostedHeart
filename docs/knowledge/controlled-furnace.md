# 控温实验炉

- Status: Current
- Last verified: 2026-11-27
- Scope: 独立控温烧制、样品格、停炉结算及耐火制品观察索引。
- Code anchors: ControlledFurnaceBlockEntity、ControlledFurnaceMenu、ControlledFurnaceScreen、ControlledFiringRecipe、ControlledFiringCategory、RefractoryObservationIndex。

## 操作与停炉结算

控温实验炉（`frostedresearch:controlled_furnace`）是独立实验设备。首次打开时归当前队伍所有，同队成员可操作。六个样品格各放一件。把木炭放入燃料格，每次开炉用一块木炭。可放进不同材料一起试烧。缺少烧制配方的物品保持原样。

温度可输入 600–1400°C 内的整数，按钮每次增减 50°C。点击开始时应用该设定。倒计时可填 1–86400 秒，留空或填 0 时烧到玩家点「停止」。运行中锁定温度、样品和燃料格。只有区块加载期间才累计时间。拆除正在烧制的炉子会掉落炉内尚未结算的样品和剩余燃料。

手动或定时停炉时，每个样品独立结算。时长小于或等于配方阈值时保留原料。超过阈值时，在温区内得到成功产物，温区外得到失败产物，两者都在原格取出。例如耐火黏土球的阈值为 400 游戏刻（正常速度 20 秒），成功温区为 1050–1150°C（含端点）。倒计时填 21 秒即可超过阈值。

## 数据与存档

`frostedresearch:controlled_firing` 配方定义 `input`、`result`、`failure`（各为含 `item` 的单件物品）、`temperature_min`、`temperature_max` 和 `duration`（正整数游戏刻）。同物品匹配多个配方时，使用 ID 排序第一条。开炉时保存每格的时长阈值和候选结果。配方可在 JEI 查询。

`specimen_inventory` 保存格内物品，`running`、`elapsed_ticks`、`limit_ticks`、`temperature` 与 `firing` 保存本炉条件和进度。旧档中仍在运行的旧批次以退回原料结束。旧炉里的剩余燃料、成品和退款原料在迁移时保留，超出样品格的物品会在同队成员首次开炉界面时还给玩家。烧成的耐火砖可以用来完成正式项目的实物观察，见[耐火砖研究](refractory-brick.md)。

## 耐火制品观察索引

`RefractoryObservationIndex` 在 `KnowledgeRuntime.started` 及全局 `datapackSync` 后刷新。两者都使用安装好最终标签的服务器 RecipeManager，不依赖有在线玩家。已核对本地 Forge 47.3.0 映射字节码：`MinecraftServer.reloadResources` 的完成回调先替换资源管理器、调用 `ReloadableServerResources.updateRegistryTags`，再调 `PlayerList.reloadResources`；后者在玩家循环外派发 `OnDatapackSyncEvent(playerList,null)`，然后发送标签/配方。只读证据保存在 `build/m5-server-lifecycle-bytecode.txt` 和 `build/m5-playerlist-lifecycle-bytecode.txt`。只遍历能公开 ingredients/结果的 `CraftingRecipe`：某 ingredient 接受 `the_winter_rescue:research/refractory_bricks` 物品 tag 的任一成员时，纳入其直接输出物品；BlockItem 同时纳入对应 Block。不递归，也不扫描每 tick。

特殊机器产物通过 item/block 同名 tag `the_winter_rescue:research/refractory_products_extra` 补充。仅引用 tag 路径，不要求标签中的可选模组存在，不改注册表标签。最终 items/blocks 为不可变集合，观察时 O(1) 查询。索引内容变化或用途缺失时注册 `frostedresearch:refractory_components` ObservationUse 并增加 KnowledgeDefinitions revision；相同内容的重复生命周期刷新不额外增加 revision；失去用途仍按既有休眠/等价规则处理，不复制任意 NBT。

命中索引的新鲜观察额外派发 `frostedresearch:refractory_component_observed`。它与原 `observation_completed` 合并为一次 `IdeaAcquisitionService.fire`，保持 M2 一次实际动作最多为同一想法掷一次概率；重复记录或不收入档案不抹去动作。
