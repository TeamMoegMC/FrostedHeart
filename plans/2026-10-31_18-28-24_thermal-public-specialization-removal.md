# 专用热玩法退出公共方块入口

- Time: `2026-10-31 18:28:24 +08:00`
- Authors: `Codex; OpenAI GPT-6; source investigation and engineering design`
- Status: `completed`
- Scope: `篝火源状态、随机更新资格、岩浆辐射邻接通知；保留真正共有的材料/几何/保存和数据缓存失效`
- Related: [当前运行合同](../docs/climate/thermal-runtime-control.md)、[前批修正及残留](2026-10-31_13-56-49_thermal-specific-hooks-and-snow.md)

## Goal And Boundary

普通方块写入不执行篝火存在/LIT规则、冰锥生长资格或岩浆邻接规则。数值求解、多核池、粗细控制、材料能量、v6保存和消费者接口保持。公共世界加载/卸载、原始几何变化与数据缓存失效仍是共同职责；这里的目标不是让热模拟脱离世界数据。

不得将相同对象判断搬进全局Forge事件或包一层泛用名称后宣称移除。不得用反射、ASM、AT、替换原版方块对象或新增独立调度器来替代。

## Verified Source

- SOURCE_MUTATION完全来自campfireFlags，非通用的机器输入机制。机器仍经observeMachine进入同一物理源索引。
- CampfireBlock.onRemove实际存在；onPlace在当前Mixin自行覆盖。LevelChunk写入时先onRemove/onPlace，之后创建/更新BE；addAndRegisterBlockEntity在安装实体后调用onLoad。
- Forge捕获期间跳过onPlace，但onRemove和BE创建仍可能发生。直接在移除回调里永久消费源或热量会破坏回滚。
- PhysicalSourceSpatialIndex已有dirtyOrder和每源sourceFlags；源传输仍在原cut推进。可以复用待维护源而不新增世界事件队列。
- ClimateBlockUpdates已经拥有实际随机更新循环；目前同时依赖Section.isRandomlyTicking和被Mixin扩展的BlockState.isRandomlyTicking。
- PalettedContainerRO.maybeHas检查调色板；它可能因残留条目返回多余true，不能用来声称原随机序列逐步一致。
- BlockRadiationIndex拥有已覆盖4×4×4 Brick、knownBrickMask、writeDirty/drainDirty、20tick刷新和每tick64块首次捕获额度。其岩浆面读取自己、六邻居，以及水平邻居的上方方块。
- MinecraftPageManager.onChunkLoad已经为已加载Chunk的全部Section安装owner，不需要因辐射邻居另造观察目录；首次辐射覆盖仍应验证其读取依赖所在的已加载Section具有当前owner。

## 1. 篝火：完整生命周期通知与最终状态交接

1. 将onCampfireIgnited收敛为一个篝火专用状态通知，调用者只允许CampfireBlock/CampfireBlockEntity自身生命周期，以及原有加载发现。onPlace覆盖点火、熄灭、水熄、状态改变；在现有CampfireBlock.onRemove返回处补通知，保留原掉落/移除实现；CampfireBlockEntity.onLoad处理合法BE建立与加载后的恢复。
2. 只依据当前已加载Chunk和真实位置状态登记，不强加载，不创建第二份燃料或源身份。迟到旧BE通知必须核对当前实体/Chunk身份，不能退休同位置的新机器或新篝火。
3. 新源在captureBlockSnapshots期间不启动、不正式登记，确认onPlace后才接管。旧源在捕获期间收到移除通知时，只标记“需重读最终状态”；回滚和确认都交给原tick末的源维护核对实际状态。
4. 复用PhysicalSourceSpatialIndex.dirtyOrder。需要区分传输待办和最终状态重读时，仅使用该索引每源标志的剩余一位；这个标志只归源索引持有，核对后清除。不得新增第二个位置队列/全局确认事务。tick末先核对需重读的已登记篝火，再使用原cut的flush和Timeline交接；热量仍成功提交才消费。
5. 显式Chunk卸载仍沿beforeChunkUnload退休该Chunk的已登记源并flush。最终状态核对不得把已经显式卸载的记录因旧BlockState尚存在而重新置为PRESENT。
6. 完成后删除公共路径中的campfireFlags、CAMPFIRE_PRESENT/CAMPFIRE_LIT、SOURCE_MUTATION、sourceFlagsFromCode及其表存储/键字段；删除SectionOwner.pendingSourceMutation、MutationScratch.sourceRelevant、onBlockMutation/record的对应参数和drainMutations逐位置源同步。
7. 删除PageManager对resyncSection的篝火整Section扫描。初始/重新加载继续使用现有BE位置发现；结构身份替换只失效已经登记源的依赖并交回其自身维护，不能再借几何恢复扫描4096格注册篝火。

玩法兼容边界：原版/Forge改块、实际BE加载、合法编辑器的BE建立及明确的结构重同步需要覆盖。任意第三方直接修改Section数组、完全不维护BE也不发出生命周期时，没有篝火事件可接收；无条件发现这种裸改写必然需要公共观察或主动复查。不得偷偷保留旧源扫描作为备用，也不能把不可能的无条件兼容说成已实现。实施前对当前真实Section替换调用者逐个确认其BE及已有源依赖交接；未知裸写入不自动获得兼容承诺。

代价：几次篝火自身通知，必要时一位局部待重读标志，替换整套逐Section源变化传播。普通非源方块不再比较篝火标志。没有实测CPU百分比；不改变功率、cut、燃料或能量精度。

## 2. 随机更新资格：由已有气候循环决定

低复杂度方案：删除BlockStateBaseMixin_RandomTick，使用原版查询语义；在ClimateBlockUpdates的原Section筛选中，先看原版isRandomlyTicking，只有false时再用section.getStates().maybeHas(CUSTOM_RANDOM_ELIGIBILITY)。谓词使用当前StateTransitionData.hasRandomTransitions和IcicleBlock.canGrowRootFrom，不创建每块对象、位置队列或新的全世界扫描。

对进入的Section仍按原pRandomTickSpeed采样。每个样本先处理原有相变/植物/冰锥；未被相变或植物接管时，按“原版资格或当前气候资格”调用方块randomTick，随后按原顺序调用fluid.randomTick。不能因为删除了查询Mixin，就漏掉以前经扩展资格调用的非原版随机方块。WATER继续只走表面采样，不把海洋纳入随机相变Section。

StateTransitionData.updateCache只发布新规则。删除仅为修改原版随机资格而存在的全世界Section.recalcBlockCounts；气候谓词下一次读取当前缓存，无独立资格缓存、版本计数或热运行时启动。

必须披露的取舍：

- 一些没有原版随机块的Section每tick多一次调色板资格检查；不是4096格遍历，但成本不能说为零。
- 调色板残留条目可能使本来可以跳过的Section多抽取无效果样本，并推进共享随机数状态；概率规则保留，逐个随机调用顺序不保证一致。
- 原版与第三方调用BlockState.isRandomlyTicking时将看到原版资格；Frosted Heart扩展只在本模组气候调度内生效。
- 如果必须保留这些查询的扩展结果和逐步随机顺序，应保留查询扩展入口。为了删一个Mixin再建立精确活跃计数及公共写入维护，不符合本方案目标。

## 3. 岩浆：辐射缓存自己维护几何依赖

1. 删除LiquidBlock.updateShape上的专用通知。公共Section接入只交出已有“哪个位置的BlockState发生变化”，不判断FluidTags.LAVA，也不向全局NeighborNotifyEvent搬相同判断。
2. BlockRadiationIndex消费通用状态变化并使已覆盖的依赖Brick失效。必须覆盖所有实际状态变化，不能只比较辐射profileId：岩浆高度变化可能使用相同profile，邻居变实心也会影响原位置辐射。
3. 对变化位置B，当前公式受影响的候选发射位置共11个：B；B±X、B±Y、B±Z；B±X−Y、B±Z−Y。后四个来自水平邻居上方的读取依赖。将位置归并到原Brick位掩码，只标记已有knownBrickMask，沿writeDirty/drainDirty合并处理；不要逐改块创建Set/List/BlockPos对象。
4. 在原dirty锁内批量合并，避免一次改块反复加锁。跨Brick/Section/Chunk、邻居未加载、Chunk卸载/重载和Section身份替换都走现有覆盖与失效生命周期；不强加载。
5. 不改变原辐射面积、功率、流体高度和聚合公式；保留现有20tick刷新及64块首次捕获额度。删除LiquidBlockMixin_ThermalRadiation、登记、onRadiantLiquidNeighborChanged及只服务该入口的状态字段。

代价：局部几何变化可能使更多相关Brick失效，但合并后每块按原刷新轮重算；普通水的形状更新不再经过岩浆专用判断。仍然保留通用数据缓存失效。若连这条通用变化通知也禁止，只能周期重读世界，产生额外CPU或可见延迟，不能同时声称零公共观察、零扫描、即时更新。

## Order And Deletion Gate

先完成篝火全生命周期及捕获回滚，再删除源标志链；再完成辐射依赖与删除LiquidBlock钩子；最后处理随机资格方案及其明确的语义/性能取舍。整批编写后复查源、空间、查询与保存交接，再统一构建和重点验证。

验收检查新增路径，也必须确认上述旧常量、字段、参数、整Section源扫描、两个Mixin及登记真正删除。允许保留共有Section输入、材料、保存与现有ServerLevel气候循环，不新增等价的全局对象判断。独立Mixin数量预计减少2个，实际生产代码规模以实现结果为准，不预先承诺行数或性能收益。

## Focused Validation

- 篝火：点燃、熄灭、水熄、挖除、替换、同位置旧BE迟到、源容量拒绝、加载/卸载；捕获新放置取消/确认，旧源捕获移除取消/确认；源J不丢、不重复。
- Section直接修改/身份替换：逐一使用当前真实入口验证已有BE与已登记源的交接。没有确认覆盖前，不称为完整移除。
- 随机资格：只有自定义相变/冰锥而没有原版随机块的Section仍推进，reload即时生效，水保持表面采样，pRandomTickSpeed=0保持，方块/流体调用顺序保留。比较真实调色板判断成本，不预先宣称提速或0.1°C资格。
- 辐射：邻接墙开闭、岩浆流体高度、水平邻居上方变化、跨Brick/Section/Chunk及重载；使用同一公式即时重建结果核对缓存刷新，水形状更新不触发专用岩浆入口。
- 复用当前实际燃料、保存与关闭回归；GameTest观察仅进入测试source set。只跑受影响重点，不用JUnit、全矩阵、历史工程复制重编或路径敏感hash。

## Outcome

已按上述边界实施三条迁移。篝火onPlace/onRemove及自身BE.onLoad负责状态通知，捕获变化复用源索引最后一个标志位和dirtyOrder，在tick末/flush前读取最终状态；公共Section源标志、参数、源同步、整Section源扫描与仅源变化缓冲退出。公共BlockState资格Mixin与液体邻接Mixin及其登记退出；气候循环自筛资格，辐射11位置依赖归并为最多6个Brick，跨Section/Chunk和对角读取被覆盖。没有新增生产类型或世界事件队列。

最终生产Java由8203行减为7985行，净减218行（相同受影响文件范围，测试/文档另计）。最终JAR有9个集中热Mixin、0个退役专用类、0个GameTest类。

整批静态闭合后首次assemble/compileGameTestJava通过，首轮15项全部通过。后续实际调用者复查确认FastNoiseEngine的原始容器替换可在后台调用，补齐其源/缓存依赖回到主线程并核对owner有效性；对应现有源用例新增异步原始容器替换，最终整批重建后同一轮15项再次全部通过，热错误0、关闭rootBytes=0。没有通过调整配额、删玩法或新条件强迫测试通过。

当前合同：[运行说明](../docs/climate/thermal-runtime-control.md)；完整证据：[验收JSON](../docs/climate/validation/thermal-public-specialization-removal-2026-10-31.json)。随机调色板检查、原版查询恢复/随机序列，以及不维护BE的任意裸写入边界仍按本文披露。数值内核、功率、源cut、粗档和v6格式保持；没有新CPU百分比或百人联网结论。改动尚未提交/推送。
