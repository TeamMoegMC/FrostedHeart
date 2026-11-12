# 控温实验炉与耐火制品观察索引

- Status: `Current`（M5基线29项及独立进程保存；M6最终16项验证新目标选择、完成标志和同台交错操作；实际客户端界面另验）
- Last verified: `2026-11-12`
- Scope: 自有控温炉、普通耐火砖批次、队伍本次试验样、配方直接制品观察用途。整合包正式路线由 [refractory-brick.md](refractory-brick.md) 维护。
- Code anchors: `ControlledFiringRecipe`、`ControlledFurnaceBlockEntity`、`ControlledFurnaceMenu`、`ControlledFurnaceScreen`、`ControlledFiringCategory`、`RefractoryObservationIndex`、`ObservationSessions`。

## 模式和权限

方块/物品/方块实体/menu ID 为 `frostedresearch:controlled_furnace`，配方 type/serializer 为 `frostedresearch:controlled_firing`，失败物品为 `frostedresearch:cracked_refractory_brick`。
炉首次由玩家打开或启动时归属当前队伍；同队成员可以投取物品、调温和启动。三个槽依次为坯料、燃料、成品；成品槽禁止手动放入，无自动化物品 capability。

每炉手动启动。有 `experiment` 的配方属于试验模式：启动必须存在 `KnowledgeService.observationTargets(experiment)` 中的当前有效任务。样品绑定复用 `ResearchSampleBinding(team,run,requirement,experiment)`，没有个人 owner。同一模式多个合格配方按 ID 排序，选择首条材料匹配且资格有效的配方；任务按创建时间、UUID稳定排序。只有一个合格目标时自动选用；多个目标且没有有效已选绑定时，启动返回 choose_target、不扣料，界面提供目标切换。仍有效的选择不会被新合入记录或下一次启动覆盖。

没有 `experiment` 的配方属于生产模式：启动同时要求当前 enabled `ResearchResult.Design` 声明该配方，以及 `ResearchHooks.canUseRecipe(team,recipe)`。Design 定义缺席时不会通过 unmanaged 默认权限公开批量生产；已投料批次可以继续结算。来件区的成果不授权限。完成研究时沿现有 `produce` 正常学习，理解条件尚未满足时才保留来件；没有增加二次学习规则。

只处理 `frostedheart:fire_clay_ball`、`minecraft:charcoal`，成功产普通 `frostedheart:refractory_brick`，失败产碎砖；结果数量与投入坯料数一致。不能用此炉产高耐火砖或工业窑砖。既有陶窑、IE 高炉、II 工业窑和旧 blast_furnace 研究未改权限或生产接点，没有迁移补偿。

## 小配方格式和温度

`input`、`fuel`、`result`、`failure` 各为 `{ "item": "namespace:id", "count": N }`，count 默认 1、范围为该物品合法堆叠数；`temperature_min`、`temperature_max` 是摄氏度，范围 600–1400、下界不大于上界；`duration` 是正整数加载 tick。`experiment` 是独立可选资源 ID。配方位于标准 `data/<namespace>/recipes/`，FH 不写死 TWR recipe ID。

TWR 当前默认试验为 1 坯料+1 木炭、400 tick，生产为 16 坯料+2 木炭、600 tick。成功温窗 1050–1150°C，含端点；其它温度产同数量碎砖。UI 的材料数与时长读取实际配方，JEI 展示实际温窗和两种结果。

炉设定 600–1400°C，每次 50°C，默认 1000°C。这是炉内工艺参数，没有升温曲线或世界热网模型。启动时锁定 `locked_temperature` 和完整批次成品；运行中改变 `temperature` 只作用于下一炉，欠烧/过烧结论也随本炉保存。

## 批次、取消与持久化

开始要求输出槽为空，一次扣除材料及燃料。批次在 BE 中保存 `refund`（内部未结算坯料）、`planned`（锁定未来产物）、`remaining`/`duration`、`binding`、`outcome`；完成时原子地清 `refund`/`planned` 并生成 `pending`。只有已加载的服务器 tick 减少 remaining，卸载、离线无补时。

输出受阻时 pending 等待，不能自动再烧或重复扣料；清空输出后交付一次。运行中取消只退内部坯料，燃料不退；若输入槽无法容纳，余料掉在炉旁。已完成 pending 不可取消成坯料。拆除掉原炉、未消耗槽内物品，运行批次退坯料或完成批次掉 pending 成品，二者互斥；`settle()` 清空状态后返回掉落，重复调用为空。

上述字段属于炉 BE，不改变 `KnowledgeState` schema 5。保存不复制整份 BE 到观察或样品。成功试验成品仅写四字段 binding；生产砖和碎砖无样品绑定。试验任务已完成/放弃/结束后不能再启动旧 run；已投料批次仍可交付，但旧样品不能完成新 run。

## 新鲜观察与一跳索引

`ObservationSessions` 成功完成实际手持、容器或世界采样后，将真实 ItemStack 传给既有 `observeResearchTarget`，先于预览和收件。当前队伍/run/任务/experiment 和实际普通砖必须全部匹配；同队任何成员可观察，不消耗、不提交。普通砖、碎砖、旧 run、外队或笔记不替代此样品。

`RefractoryObservationIndex` 在 `KnowledgeRuntime.started` 及全局 `datapackSync` 后刷新。两者都使用安装好最终标签的服务器 RecipeManager，不依赖有在线玩家。已核对本地 Forge 47.3.0 映射字节码：`MinecraftServer.reloadResources` 的完成回调先替换资源管理器、调用 `ReloadableServerResources.updateRegistryTags`，再调 `PlayerList.reloadResources`；后者在玩家循环外派发 `OnDatapackSyncEvent(playerList,null)`，然后发送标签/配方。只读证据保存在 `build/m5-server-lifecycle-bytecode.txt` 和 `build/m5-playerlist-lifecycle-bytecode.txt`。只遍历能公开 ingredients/结果的 `CraftingRecipe`：某 ingredient 接受 `the_winter_rescue:research/refractory_bricks` 物品 tag 的任一成员时，纳入其直接输出物品；BlockItem 同时纳入对应 Block。不递归，也不扫描每 tick。

特殊机器产物通过 item/block 同名 tag `the_winter_rescue:research/refractory_products_extra` 补充。仅引用 tag 路径，不要求标签中的可选模组存在，不改注册表标签。最终 items/blocks 为不可变集合，观察时 O(1) 查询。索引内容变化或用途缺失时注册 `frostedresearch:refractory_components` ObservationUse 并增加 KnowledgeDefinitions revision；相同内容的重复生命周期刷新不额外增加 revision；失去用途仍按既有休眠/等价规则处理，不复制任意 NBT。

命中索引的新鲜观察额外派发 `frostedresearch:refractory_component_observed`。它与原 `observation_completed` 合并为一次 `IdeaAcquisitionService.fire`，保持 M2 一次实际动作最多为同一想法掷一次概率；重复记录或不收入档案不抹去动作。

## 本地验证

定向测试和正式内容 fixture 在 `src/m5`，只在 `-Pm5Tests` 时加入服务器，不进入生产 JAR。不引入 Charcoal Pit，runtime 仅继承 `gameTest.runtimeClasspath`。命名空间 `frostedresearch_m5`；真实重启单独使用 `frostedresearch_m5_restart` 和相同运行目录，先 `-Pm5RestartPhase=write` 再 read。read 保留先前 world。

本轮命令和实际通过数记录在交接 diary；只认当轮 `GAME TESTS COMPLETE` / `All N required tests passed`，不以进程退出码代替。正式 TWR/KubeJS 全包、生存材料获取及实际界面/JEI 键鼠由后续客户端验收，不由服务端 fixture 冒充。

## M6 目标选择兼容

`selectedTarget()` 与 `binding()` 分离：前者为下炉已选任务，后者仍是 M5 原公开 API 的本炉锁定绑定。`cycleTarget(player)` 与原版 menu button 5 切换下炉目标，0–4原动作保留。CUI仅多目标且空闲时显示选择按钮，标签包含项目、记录序号、阶段和任务，不显示UUID。服务端重新检查真实队伍、当前阶段及目标资格；运行中不能换本炉目标。

BE可选字段 `selected_target` 保存四字段绑定，`target_title`/`target_count` 为展示信息；旧M5 NBT缺失时以既有本炉 binding 为兼容选择，不修改refund/planned/pending或剩余tick。菜单广播仅在知识mutationRevision、定义revision或RecipeManager对象变化时重建候选；不遍历每tick的配方。

同项目真实完成且全部成果有效已学时，备用目标暂不列入候选、不再启动试验，但选择字段保留。遗忘后原目标仍有效则继续使用。已投料批次继续按M5一次性交付；暂停期间样品不推进备用任务，恢复后新鲜观察仍可使用其正确绑定。来源队伍的旧样品不会因merge被改绑。


同台多人操作时，实际CUI开始按钮使用 `ControlledFurnaceStartPacket(containerId,pos,trial,target)`，携带按钮生成时显示的模式/绑定；`ControlledFurnaceMenu.startSeen` 对比当前台面选择，不一致只同步界面，不扣料或启动另一份记录。原 `start(player)` 和原版button 0–4保留供既有单用户调用/API兼容，button 5用于切目标；没有持久序号或额外状态版本。已锁定批次不被旧点击换绑。
