# 控温实验炉

- Status: Current
- Last verified: 2026-11-28
- Scope: 独立控温烧制、样品格、停炉结算及耐火制品观察索引。
- Code anchors: ControlledFurnaceBlockEntity、ControlledFurnaceMenu、ControlledFurnaceScreen、ControlledFiringRecipe、RefractoryObservationIndex。

## 操作与停炉结算

控温实验炉（`frostedresearch:controlled_furnace`）是独立实验设备。首次打开时归当前队伍所有，同队成员可操作。六个样品格各放一件。把木炭放入燃料格，每次开炉用一块木炭。可放进不同材料一起试烧。缺少烧制配方的物品保持原样。

温度可输入 600–1400°C 内的整数，按钮每次增减 50°C。点击开始时应用该设定。倒计时可填 1–86400 秒，留空或填 0 时烧到玩家点「停止」。运行中锁定温度、样品和燃料格。只有区块加载期间才累计时间。拆除正在烧制的炉子会掉落炉内尚未结算的样品和剩余燃料。

空燃料格用火焰标识，烧制中的样品格以灰色遮罩标示锁定。

手动或定时停炉时，每个样品在原格独立结算。炉子用开炉时设定的温度和实际烧制时长匹配配方。没有配方或没有符合两项条件的配方时，样品原样保留。自动停炉时间由玩家设置的倒计时决定。

耐火黏土球烧制超过 400 游戏刻（正常速度 20 秒）时，在 1050–1150°C 得到普通耐火砖，在 600–1049°C 或 1151–1400°C 得到碎裂耐火砖。三个温区均包含端点，时长均无上限。整合包为三个温区分别定义了配方。

## 数据与存档

每条 `frostedresearch:controlled_firing` 配方定义 `input`、`result`（各为含 `item` 的单件物品）、`temperature_min`、`temperature_max` 和 `duration`（非负整数游戏刻）。可选的 `duration_max` 必须大于 `duration`。温度区间包含两端，时长区间为大于 `duration`、小于等于 `duration_max`。省略上限时，超过下限的时长均可接受。

同一种样品可以用多条配方配置不同产物。开炉时记录符合样品和设定温度的所有候选配方，停炉时按时长匹配。多个区间都满足时，取配方 ID 排序的第一条。控温烧制配方由炉子读取，已从 JEI 移除。

`specimen_inventory` 保存格内物品，`running`、`elapsed_ticks`、`limit_ticks`、`temperature` 与 `firing` 保存本炉条件和进度。支持样品格的旧存档会继续当前烧制并保留开炉时已保存的产物。早期三格炉中仍在运行的批次以退回原料结束。旧炉里的剩余燃料、成品和退款原料在迁移时保留，超出样品格的物品会在同队成员首次开炉界面时还给玩家。烧成的耐火砖可以用来完成正式项目的实物观察，见[耐火砖研究](refractory-brick.md)。

## 耐火制品观察索引

`RefractoryObservationIndex` 在 `KnowledgeRuntime.started` 及全局 `datapackSync` 后刷新。两者都使用安装好最终标签的服务器 RecipeManager，不依赖有在线玩家。已核对本地 Forge 47.3.0 映射字节码：`MinecraftServer.reloadResources` 的完成回调先替换资源管理器、调用 `ReloadableServerResources.updateRegistryTags`，再调 `PlayerList.reloadResources`；后者在玩家循环外派发 `OnDatapackSyncEvent(playerList,null)`，然后发送标签/配方。只读证据保存在 `build/m5-server-lifecycle-bytecode.txt` 和 `build/m5-playerlist-lifecycle-bytecode.txt`。只遍历能公开 ingredients/结果的 `CraftingRecipe`：某 ingredient 接受 `the_winter_rescue:research/refractory_bricks` 物品 tag 的任一成员时，纳入其直接输出物品；BlockItem 同时纳入对应 Block。不递归，也不扫描每 tick。

特殊机器产物通过 item/block 同名 tag `the_winter_rescue:research/refractory_products_extra` 补充。仅引用 tag 路径，不要求标签中的可选模组存在，不改注册表标签。最终 items/blocks 为不可变集合，观察时 O(1) 查询。索引内容变化或用途缺失时注册 `frostedresearch:refractory_components` ObservationUse 并增加 KnowledgeDefinitions revision；相同内容的重复生命周期刷新不额外增加 revision；失去用途仍按既有休眠/等价规则处理，不复制任意 NBT。

命中索引的新鲜观察额外派发 `frostedresearch:refractory_component_observed`。它与原 `observation_completed` 合并为一次 `IdeaAcquisitionService.fire`，保持 M2 一次实际动作最多为同一想法掷一次概率；重复记录或不收入档案不抹去动作。
