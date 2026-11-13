# 控温实验炉

- Status: `Current`（11月13日16项服务端回归通过，含第二种材料/实验和同台交错操作；Playtest实际打开原生GUI、调温和切换生产模式通过；此前完整客户端试烧/批量证据仍为旧CUI）
- Last verified: `2026-11-14`（本轮正式砂砖前置与服务端回归；11月13日原生GUI实际显示和按钮检查、通用物料服务端回归；详见[当轮记录](../../diary/2026-11-13_23-12-00_research-root-journal-and-general-furnace-gui.md)）
- Scope: 通用控温加工设备、按配方指定的试验/生产批次、队伍本次试验样；另记录既有耐火制品观察索引。整合包当前耐火砖路线由 [refractory-brick.md](refractory-brick.md) 维护。
- Code anchors: `ControlledFiringRecipe`、`ControlledFurnaceBlockEntity`、`ControlledFurnaceMenu`、`ControlledFurnaceScreen`、`ControlledFiringCategory`、`RefractoryObservationIndex`、`ObservationSessions`。

## 模式和权限

方块/物品/方块实体/menu ID 为 `frostedresearch:controlled_furnace`，配方 type/serializer 为 `frostedresearch:controlled_firing`。它是通用试验/生产设备，不绑定某个研究或材料；现有耐火砖配方只是一个用途。
炉首次由玩家打开或启动时归属当前队伍；同队成员可以投取物品、调温和启动。三个槽依次为原料、燃料、产物；产物槽禁止手动放入，无自动化物品 capability。

每炉手动启动。有 `experiment` 的配方属于试验模式：启动必须存在 `KnowledgeService.observationTargets(experiment)` 中的当前有效任务。样品绑定复用 `ResearchSampleBinding(team,run,requirement,experiment)`，没有个人 owner。同一模式多个合格配方按 ID 排序，选择首条材料匹配且资格有效的配方；任务按创建时间、UUID稳定排序。只有一个合格目标时自动选用；多个目标且没有有效已选绑定时，启动返回 choose_target、不扣料，界面提供目标切换。仍有效的选择不会被新合入记录或下一次启动覆盖。

没有 `experiment` 的配方属于生产模式：启动同时要求当前 enabled `ResearchResult.Design` 声明该配方，以及 `ResearchHooks.canUseRecipe(team,recipe)`。Design 定义缺席时不会通过 unmanaged 默认权限公开批量生产；已投料批次可以继续结算。来件区的成果不授权限。完成研究时沿现有 `produce` 正常学习，理解条件尚未满足时才保留来件；没有增加二次学习规则。

原料、燃料、成功和失败产物均由配方指定，四项数量相互独立，不再由代码限制为耐火黏土、木炭或耐火砖。只有温度落在成功范围内的试验产物写入绑定；成功和失败即使使用同一种物品，失败品也不被写成有效试样。正式包当前仍只定义普通耐火砖两条配方，没有凭空增加高耐火砖/工业砖配方。既有陶窑、IE高炉、II工业窑和旧研究权限未改。

## 普通设备界面

`ControlledFurnaceScreen` 使用原生 `AbstractContainerScreen`，由 `MenuScreens.register` 注册，不再套札记CUI。界面尺寸276×232 GUI像素，复用原版 `demo_background.png` 灰色面板和 `container/furnace.png` 的槽位/加工箭头、原版按钮；没有新增位图。玩家背包为三行九列加九格快捷栏，既有物品栏索引和三个工艺槽索引不变。

顶部选择试验/生产模式及对应研究任务，调整下一批温度；中部是原料、燃料、产物槽、加工箭头与开始/取消按钮。状态文本过长时悬停阅读；加工箭头悬停显示进度和通用操作说明。具体物料、时长、成功温窗和产物在JEI中查询。文字不再要求所有实验都观察耐火砖。

## 小配方格式和温度

`input`、`fuel`、`result`、`failure` 各为 `{ "item": "namespace:id", "count": N }`，count 默认 1、范围为该物品合法堆叠数；`temperature_min`、`temperature_max` 是摄氏度，范围 600–1400、下界不大于上界；`duration` 是正整数加载 tick。`experiment` 是独立可选资源 ID。配方位于标准 `data/<namespace>/recipes/`，FH 不写死 TWR recipe ID。

TWR 当前耐火砖试验为1坯料+1木炭、400tick，生产为16坯料+2木炭、600tick。成功温窗1050–1150°C，含端点；其它温度产同数量碎砖。这些是该配方的内容约定，不是设备限制；JEI展示每条实际配方。

炉设定 600–1400°C，每次 50°C，默认 1000°C。这是炉内工艺参数，没有升温曲线或世界热网模型。启动时锁定 `locked_temperature` 和完整批次成品；运行中改变 `temperature` 只作用于下一炉，欠烧/过烧结论也随本炉保存。

## 批次、取消与持久化

开始要求输出槽为空，一次扣除材料及燃料。批次在 BE 中保存 `refund`（内部未结算坯料）、`planned`（锁定未来产物）、`remaining`/`duration`、`binding`、`outcome`；完成时原子地清 `refund`/`planned` 并生成 `pending`。只有已加载的服务器 tick 减少 remaining，卸载、离线无补时。

输出受阻时 pending 等待，不能自动再烧或重复扣料；清空输出后交付一次。运行中取消只退内部坯料，燃料不退；若输入槽无法容纳，余料掉在炉旁。已完成 pending 不可取消成坯料。拆除掉原炉、未消耗槽内物品，运行批次退坯料或完成批次掉 pending 成品，二者互斥；`settle()` 清空状态后返回掉落，重复调用为空。

上述字段属于炉BE，不改变 `KnowledgeState` schema 5。保存不复制整份BE到观察或样品。成功试验产物仅写四字段binding；生产和失败产物无样品绑定。试验任务已完成/放弃/结束后不能再启动旧run；已投料批次仍可交付，但旧样品不能完成新run。

## 新鲜观察与一跳索引

`ObservationSessions` 成功完成实际手持、容器或世界采样后，将真实 ItemStack 传给既有 `observeResearchTarget`，先于预览和收件。当前队伍/run/任务/experiment 和实际普通砖必须全部匹配；同队任何成员可观察，不消耗、不提交。普通砖、碎砖、旧 run、外队或笔记不替代此样品。

`RefractoryObservationIndex` 在 `KnowledgeRuntime.started` 及全局 `datapackSync` 后刷新。两者都使用安装好最终标签的服务器 RecipeManager，不依赖有在线玩家。已核对本地 Forge 47.3.0 映射字节码：`MinecraftServer.reloadResources` 的完成回调先替换资源管理器、调用 `ReloadableServerResources.updateRegistryTags`，再调 `PlayerList.reloadResources`；后者在玩家循环外派发 `OnDatapackSyncEvent(playerList,null)`，然后发送标签/配方。只读证据保存在 `build/m5-server-lifecycle-bytecode.txt` 和 `build/m5-playerlist-lifecycle-bytecode.txt`。只遍历能公开 ingredients/结果的 `CraftingRecipe`：某 ingredient 接受 `the_winter_rescue:research/refractory_bricks` 物品 tag 的任一成员时，纳入其直接输出物品；BlockItem 同时纳入对应 Block。不递归，也不扫描每 tick。

特殊机器产物通过 item/block 同名 tag `the_winter_rescue:research/refractory_products_extra` 补充。仅引用 tag 路径，不要求标签中的可选模组存在，不改注册表标签。最终 items/blocks 为不可变集合，观察时 O(1) 查询。索引内容变化或用途缺失时注册 `frostedresearch:refractory_components` ObservationUse 并增加 KnowledgeDefinitions revision；相同内容的重复生命周期刷新不额外增加 revision；失去用途仍按既有休眠/等价规则处理，不复制任意 NBT。

命中索引的新鲜观察额外派发 `frostedresearch:refractory_component_observed`。它与原 `observation_completed` 合并为一次 `IdeaAcquisitionService.fire`，保持 M2 一次实际动作最多为同一想法掷一次概率；重复记录或不收入档案不抹去动作。

## 本地验证

定向测试和正式内容 fixture 在 `src/m5`，只在 `-Pm5Tests` 时加入服务器，不进入生产 JAR。正式项目的砂砖观察前置依赖 Charcoal Pit，因此此测试开关也加载真实 Charcoal Pit 服务端依赖；普通生产构建不受此测试开关影响。命名空间 `frostedresearch_m5`；真实重启单独使用 `frostedresearch_m5_restart` 和相同运行目录，先 `-Pm5RestartPhase=write` 再 read。read 保留先前 world。

本轮命令和实际通过数记录在交接 diary；只认当轮 `GAME TESTS COMPLETE` / `All N required tests passed`，不以进程退出码代替。正式 TWR/KubeJS 全包、生存材料获取及实际界面/JEI 键鼠由后续客户端验收，不由服务端 fixture 冒充。

## M6 目标选择兼容

`selectedTarget()` 与 `binding()` 分离：前者为下炉已选任务，后者为本炉锁定绑定。`cycleTarget(player)` 与menu button 5切换下炉目标，0–4原动作保留。原生GUI仅在多个目标且空闲时允许切换；标签包含项目、记录序号、阶段和任务，不显示UUID。服务端重新检查真实队伍、当前阶段及目标资格；运行中不能换本炉目标。

BE可选字段 `selected_target` 保存四字段绑定，`target_title`/`target_count` 为展示信息；旧M5 NBT缺失时以既有本炉 binding 为兼容选择，不修改refund/planned/pending或剩余tick。菜单广播仅在知识mutationRevision、定义revision或RecipeManager对象变化时重建候选；不遍历每tick的配方。

同项目真实完成且全部成果有效已学时，备用目标暂不列入候选、不再启动试验，但选择字段保留。遗忘后原目标仍有效则继续使用。已投料批次继续按M5一次性交付；暂停期间样品不推进备用任务，恢复后新鲜观察仍可使用其正确绑定。来源队伍的旧样品不会因merge被改绑。


同台多人操作时，原生GUI开始按钮仍用 `ControlledFurnaceStartPacket(containerId,pos,trial,target)`，携带当前显示的模式/绑定；`ControlledFurnaceMenu.startSeen` 对比台面选择，不一致只同步界面，不扣料或启动另一份记录。原 `start(player)` 和button 0–4保持API兼容，button 5切目标；没有额外持久序号。已锁定批次不被旧点击换绑。
