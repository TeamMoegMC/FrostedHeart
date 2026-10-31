# 热模拟接入收敛：11项Mixin的完整工程链路

- Time: `2026-10-30 23:51:15 +08:00`
- Updated: `2026-10-31; selected implementation and scoped validation complete`
- Authors: `Codex; OpenAI GPT-6; source investigation and implementation design`
- Status: `completed for the explicitly selected implementation; retained hooks and limits stated below`
- Scope: `7个直接接入、1个篝火配套、3个随机/冰水相变基础，共11个正式Mixin`
- Related: [运行合同](../docs/climate/thermal-runtime-control.md)、[当前待办](2026-10-29_23-38-00_thermal-next-steps.md)、[注册清单](../src/main/resources/frostedheart.mixins.json)

## 目标与明确决策

用户要求可读性与可维护性，减少独立接入、内部字段依赖和原版方法副本。反射、ASM、AT或替换Chunk/Section对象不作为本轮替代手段。移动文件只改善查找，不能单独记作减少侵入。

本方案已逐项复查调用者、正常路径、拒绝/取消路径和生命周期，并按用户2026-10-31授权完成选定实现。下面保留实施链路和取舍；当前实现与验证结果见文末Outcome及[验收记录](../docs/climate/validation/thermal-integration-2026-10-31.json)，不把保留的随机循环、岩浆通知或全局规则称为已经删除。

| 序号 | Mixin | 本批选择 | 侵入/依赖的实际变化 |
|---|---|---|---|
| 1 | LevelChunkSectionMixin_ThermalInput | 保留入口，复用owner和状态编码 | 不新增原版访问；减少重复查询 |
| 2 | LevelChunkMixin_DormantThermal | 合并最终写入入口，公开API判空 | 删除工具性父类继承与构造适配 |
| 3 | ChunkMapMixin_ThermalMaintenanceSave | 改为包围helper内部实际save调用 | 删除冷却表字段依赖、外层重复筛选 |
| 4 | LiquidBlockMixin_ThermalRadiation | 保留薄通知，明确依赖归属 | 不承诺删除；替代方案的具体成本见下 |
| 5 | ServerLevelMixin_TemperatureUpdate | 本批集中实际业务，保留现有抽样顺序 | 可读性改善；整段cancel仍存在，不计为降低接管范围 |
| 6 | SnowLayerBlockMixin_Melt | 相变与层雪规则集中，保留直接调用保护 | 不用额外上下文缓存去强删第二处保护 |
| 7 | CampfireBlockMixin_TimeLimit | 删除专用点火通知，集中交互业务 | 少一个onPlace热源接入和分散入口 |
| 8 | CampfireTileEntityMixin_TimeLimit | 共享燃料规则，保留tick/NBT状态 | 去重业务；不增加Capability或计时系统 |
| 9 | BlockStateBaseMixin_RandomTick | 原版true时立即返回 | 减少tag和数据表查询，无新缓存 |
| 10 | IceBlockMixin_Melt | 窄拦截替代playerDestroy整段重写 | 删除一个完整Overwrite及其原版副本 |
| 11 | BiomeMixin_FreezeWater | 保留全局禁冻规则，清掉失效文本 | 保持现有全局语义，不用更重的取消适配 |

不设“11类必须降到某个数”的指标。篝火类仍有原有交互功能，删除其热通知不等于整个类消失。其余温度消费者、天气系统、城镇、世界生成、design和受保护旧粒子不在本轮修改范围内。涉及世界生成的现有规则仅记录并保持。

## 文件与职责归属

11个接入类统一放到`com.teammoeg.frostedheart.mixin.minecraft.thermal`，在现有`frostedheart.mixins.json`中连续分组登记，删除原temperature/campfire路径中的相应旧项。沿用已有配置与refmap，不增加一份启动配置，不修改无关Mixin。

实际业务归属：

- 世界写入的公开接入仍由`MinecraftThermalInput`持有；Page变更交给`MinecraftPageManager`，材料能量/相变交给`MinecraftPhaseController`，源登记交给`PhysicalSourceSpatialIndex`，辐射交给`BlockRadiationIndex`。
- 新的普通实现`ClimateBlockUpdates`承载现有天气表面、植物、层雪以及明确的区块更新步骤。它拥有实际实现，不是再套一层同名转发。无持久状态、每位置缓存或新任务队列。
- 新的普通实现`CampfireGameplay`承载两份篝火Mixin中的交互和共享燃料计算。lifeTime、原能力对象仍由原BlockEntity持有，不为此再创建一份FuelState。
- 不给11个Mixin分别生成11个helper，不另建通用通知总线。Mixin保留原签名、原对象身份条件、廉价筛选和必要的取消/返回。

## 1. Section变化：LevelChunkSectionMixin_ThermalInput

源码：[Section接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/LevelChunkSectionMixin_ThermalInput.java)、[Page管理](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPageManager.java)。

完整链路：

```text
Section.setBlockState返回旧状态
→ owner存在、前后状态不同
→ onSectionSetBlockState(owner, localXYZ, previous, next)
→ 一次读取old/new状态编码、签名与变化类别
→ 材料变化记录 / Page几何失效 / 源待同步 / 辐射与遮挡标脏
→ MinecraftPageManager.tick消费真实变化
→ 捕获、输入交接、区域构建/求解
→ QueryPublication发布
```

工程步骤：

1. Mixin把volatile owner读到局部变量并传下去；运行时入口不再从Section重新取owner。`SectionOwner.isValid()`只读取已有valid状态，不增加另一份标志；入口拒绝已失效owner或已关闭input。`record`已有valid检查仍保留，材料记录也要遵守同一有效性边界。
2. `MinecraftStateThermalTable`提供以已取出的old/new编码计算变化类别的内部方法，签名也从同一编码取得；删除被替代的内部入口，而非保留两套分类实现。
3. 继续使用现有dirty bitset和材料变化数组，不创建每次写入的Change对象。不因资源等待增加世界几何版本。
4. 不能把Section写入全部改成玩家BlockEvent。正常直接Section写入不一定经过玩家事件。

失败/释放：无新资源拥有者。owner失效后忽略其记录；不能把旧owner的变化转送到同坐标的新Section。未执行的材料/源工作继续按既有dirty/输入存储交接，不直接在Mixin消费热量。

删除项：重复owner读取、重复状态分类。保留这个短钩子，不删除覆盖直接Section写入的能力。

验收：正常改块、直接Section写入、Section替换与卸载后旧引用、材料变换、无关元数据变化不扩大几何失效。检查同一真实变化只登记一次，源J没有重放。

实现判断：可实施，原版接入点不增加，主要改善重复读取与职责清晰度。

## 2. 最终方块写入：LevelChunkMixin_DormantThermal

源码：[Chunk接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/LevelChunkMixin_DormantThermal.java)、[运行时入口](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java)。

保留休眠状态与材料版本字段。把当前仅面向材料的最终回调收敛成一个`onBlockStateWritten(chunk, pos, previous, next)`入口，承接第7项点火识别。

```text
Chunk.setBlockState
→ Section写入通知
→ 原版onRemove/onPlace与BlockEntity建立/更新
→ Chunk返回通知
→ 验证最终状态
→ 新点火识别与登记
→ 当前owner材料记录 + 休眠材料修正
→ 后续输入提交 / 保存
```

工程步骤：

1. 旧状态null、状态身份未变或客户端直接返回。非主线程通知先复制不可变坐标，沿现有方式交回主线程；到主线程后重新核对最终状态，不能拿过期next强行覆盖后来变化。
2. 当前方块必须仍等于本次next。该检查放在启动源和材料修正之前，处理onPlace中重入写入。不要把Chunk最终回调提前为Section时刻。
3. 若next是燃烧篝火且previous不是燃烧篝火，执行第7项的启动/登记；必须发生在“没有休眠材料也没有Page就返回”的材料快路径之前。
4. 材料部分移成此入口内部的既有实现，保留`MinecraftPhaseController.materialChangeCause`与APPLYING相变上下文、当前材料焓、冷却、版本和dirty规则。更新`ThermalDormantCoolingGameTests`中的旧内部调用；删除不再使用的旧公开入口。
5. BE判空直接使用`!chunk.getBlockEntities().isEmpty() || !chunk.getBlockEntitiesPos().isEmpty()`。删除附件的`frostedheart$hasBlockEntityPositions`、Mixin的`extends ChunkAccess`、对应构造器与导入。无新AT、反射或复制的字段镜像。

代价：live表非空时不复制；只有packed NBT时仍创建一次入队检查集合，空表也有空集合对象。真正开始分段发现时的独立快照继续保留。每维度8游标、每游标32位置、总256位置、每tick1个新快照，以及已删的逐tickArrayList副本都保持。这是明确的局部加载代价，不宣称零性能差异。

最终状态通知后移到Chunk返回，与旧onPlace位于同一服务器tick，但不是同一个调用栈时刻。目标保留写入完成后的玩法结果；第三方在onPlace内部读取尚未完成写入的热源中间态不具有逐指令等价承诺。若产品要求这种中间态也一致，保留旧专用入口，不能同时声称完成入口合并。

验收：首源直接放置、已存在运行时点火、嵌套改块、仅packed NBT、无活跃worker时材料替换、原材料相变能量记录。检查不加载额外Chunk。

实现判断：公开API判空与入口整理可实施；性能代价和通知中间态差异已明确。

## 3. 保存：ChunkMapMixin_ThermalMaintenanceSave

源码：[保存接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/ChunkMapMixin_ThermalMaintenanceSave.java)。

### 维护保存链

```text
processUnloads / 非flush saveAllChunks
→ 原版saveChunkIfNeeded检查可访问性、区块类型、冷却
→ 原版真正调用save(ChunkAccess)的位置
→ 干净区块：原样调用save，不创建热封存
→ 相关且dirty：封存当前热状态与未消费J
→ 设置preparedMaintenanceSave
→ 原版save / ChunkSerializer.write
→ Forge ChunkDataEvent.Save只编码已封存热状态
→ finally恢复prepared标记
→ 原版刷新可访问性，成功时更新冷却
```

工程步骤：

1. 删除针对`processUnloads/saveAllChunks → saveChunkIfNeeded`的外层Redirect、`chunkSaveCooldowns` Shadow和`saveChunkIfNeeded` Shadow。
2. 在`saveChunkIfNeeded → save(ChunkAccess)`处保留一个精确包装；新增的`save(ChunkAccess)`调用桥只执行原方法。当前已查看的Minecraft1.20.1映射字节码里该helper只有一处实际save调用。
3. 包装器先检查原始`access.isUnsaved()`。干净区块直接调用原方法，以保留原版返回值和副作用，不能先封存再把它变脏。
4. `LevelChunk`与`ImposterProtoChunk`解包一次。无运行时、无完整区块或无热关联时直接调用原save。相关区块继续用`runMaintenanceSave`，成功后才记录`onMaintenanceSave`。
5. `captureRegionalCheckpoint`返回false时，普通维护保存返回false、不调用实际写盘、不设置prepared，并递增已有`maintenanceSaveDeferrals`计数。现有代码仅在`checkpointInProgress`时返回false，调用者却未检查；这里明确补齐拒绝处理，不宣称已发生真实数据损坏。dirty和原版重试路径保留，避免写入未经准备的状态。
6. 捕获成功后，在try/finally范围内设置与恢复prepared标记。捕获和编码失败均走已有释放/异常路径，不能降级为写旧状态并报告成功。

成本澄清：当前`runMaintenanceSave`调用`captureRegionalCheckpoint`，后者会`awaitCheckpointCommand(capture.ready)`，并在finally等待finish。这是同步接管等待。移动钩子减少重复筛选，不会让自动保存自动变成零等待；要改变这一点属于另一套保存准备策略，本轮不增加该系统。

### 卸载链

保留现有：原chunkReady → 原卸载executor → 有限Checkpoint准备 → 核对pending holder、future身份与几何 → 写封存状态 → 原卸载consumer → finish/release。

- 票据返回或holder不再pending：取消并释放热封存，让原版闭包自行判断，不强行卸载。
- 几何/源修订变化：finish(false)，通过原scheduleUnload重新安排，不沿用旧捕获。
- 额度不足：沿现有fallback处理；不丢未消费J，不新增等待队列。
- 正常完成或退出：preparedNativeUnload在finally恢复，引用与reservation按既有一次释放规则结束。

这条卸载链不因维护保存钩子搬迁而更换future或executor。Forge Save事件位于原版序列化之后，不能作为提前准备的直接替代。

验收：干净区块零封存、冷却未到零封存、无关区块保存不加入热等待、真实维护写盘、明确flush、准备拒绝后保留dirty、票据返回取消、几何变化重排、停服释放。沿用现有源/保存隔离与NativeCheckpoint重点场景。

实现判断：可实施，删除一项内部字段依赖和重复筛选；保留现有同步成本与卸载协议。

## 4. 岩浆：LiquidBlockMixin_ThermalRadiation

源码：[岩浆通知](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/LiquidBlockMixin_ThermalRadiation.java)、[辐射索引](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/radiation/minecraft/BlockRadiationIndex.java)。

本批选择保留这一个薄通知，集中位置和明确归属，不能把它记成已经删除的侵入。

```text
LiquidBlock.updateShape
→ ServerLevel + lava/profile epoch廉价判断
→ onRadiantLiquidNeighborChanged
→ BlockRadiationIndex标记已覆盖的自身Brick
→ tick合并dirty mask
→ 重读64格及所需邻接状态
→ 更新聚合辐射源
→ RadiationService查询
```

原因不是“还没核对”：已确认辐射表面积依赖本格、上格、四个水平邻格，以及侧面邻格上方流体状态。改变位置p时，逆向可能影响的源位置包括p、p-down、p±X、p±Z及p±X-down、p±Z-down。按4格Brick合并后最多涉及当前/下层的6个相邻Brick。

完整替代方案可以实现：在统一世界变化入口计算这些逆向Brick，只标记已覆盖数据，批量在同一dirty锁内合并。为避免每次普通改块都重建无岩浆Brick，需要在现有SectionState上增加一个派生`lavaBrickMask`，由原Brick捕获/重建同步维护；每已覆盖Section新增8字节，无第二份源数据。新岩浆加入时必须无条件标记本Brick，不能依赖旧mask；未知Brick交给第一次捕获，加载/卸载邻边规则保持。

但这改变了通知策略，并且不能自动等价覆盖“显式updateShape但没有对应正常方块写入”的调用；同时增加派生状态与更多标脏计算。它不满足本轮优先减少耦合和状态的取舍，因此明确不实施这套替代。保留的kind/epoch是每液体Block实例的小缓存，不用新Map替换。

验收：本批只迁移包名，不改变标脏范围；保持跨Brick/Section/Chunk、岩浆高度、遮挡、加载/卸载与profile reload行为。

实现判断：保持薄钩子可完整实施；删除它也有具体工程路线，但代价不合本批目标，已经作出保留决策。

## 5. 区块随机更新：ServerLevelMixin_TemperatureUpdate

源码：[现有更新](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/ServerLevelMixin_TemperatureUpdate.java)。

本批实施职责集中，保留现有显式抽样循环；不把搬走循环称为减少原版流程接管。

```text
原版tickChunk先执行雷电部分
→ 同一个既有接入点
→ ClimateBlockUpdates.tickAfterThunder(level, chunk, randomTickSpeed)
   → 一次取得Chunk气候基温、天气与配置
   → 按现有周期执行表面样本/屋檐/水相变/积雪/降水
   → 按原Section顺序与随机次数抽取位置
   → 相变 → 植物 → 冰锥 → 原方块tick → 原流体tick
→ 恢复profiler栈，取消原方法后续
```

`ClimateBlockUpdates`持有实际代码：表面更新、植物判断、随机样本分派和第6项层雪规则。相变继续调用既有PhaseController。Mixin只保留参数桥与必要cancel，删除其天气、植物方法副本和无用Shadow。

必须逐条保留：

1. 当前表面周期`(gameTick + chunkX + chunkZ) % tempBlockstateUpdateIntervalTicks`、randomTickSpeed为0的行为、最小建造高度处理。
2. 当前实现已经取代原版weather的`random.nextInt(16)`分支，不能恢复后多消耗一次随机数。
3. 每Chunk只读一次气候基温。屋檐、植物死亡和冰锥的随机调用顺序不变。
4. 外层相变或植物实际改块时，方块和流体tick同时跳过；DEFERRED不能机械当作CHANGED。
5. 使用同一个已抽出的旧BlockState、位置和RandomSource，不重新抽样，不更换流体读取对象。

为何本批不直接复用原版循环：需要把同一样本的handled结果同时交给方块资格与流体资格判断，并保留自定义天气分支的随机消耗。只包方块randomTick会漏掉流体抑制；把handled放在全局、ServerLevel临时字段或ThreadLocal会引入重入状态管理。

若必须删除循环副本，具体可选路线是引入MixinExtras的局部捕获与调用帧共享：在原天气位置执行当前表面逻辑，抑制原16分之一分支的随机调用；在原Section.getBlockState后处理样本；同一方法帧共享handled，分别包住block/fluid的isRandomlyTicking。递归调用必须各自独立帧。该路线仍使用Mixin，增加多个精确调用点、第三方依赖和每Chunk帧上下文，不能预先保证运行分配相同。当前工程未声明该依赖，本批不采用。这是一项明确的架构取舍，不再以“需要核对”作实施占位。

验收：同一预置随机源下记录样本位置、随机调用顺序、block/fluid执行序列；覆盖天气开关、randomTickSpeed=0、最小高度、植物改块和相变三种结果。提取前后序列必须相同。

实现判断：职责集中可实施；原循环副本完全删除有具体技术路线，但其新依赖与上下文代价不纳入本批，也不算本批已完成目标。

## 6. 层雪：SnowLayerBlockMixin_Melt

源码：[层雪接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/SnowLayerBlockMixin_Melt.java)。

```text
外层随机样本或其他合法直接调用
→ SnowLayerBlock.randomTick
→ ClimateBlockUpdates.tickSnowLayer
→ 既有PhaseController.tryAtRandomTick
→ DEFERRED/CHANGED：抑制光照融雪
→ GAMEPLAY：按原亮度>11减一层或掉落后移除
→ 取消原版整块融化
```

工程步骤：把现有层雪处理的完整实际实现放入共享的ClimateBlockUpdates；Mixin保留一次调用和cancel。单层dropResources、多层减一、更新flags、同一RandomSource和不加载Chunk规则保持。

修正先前过强的“重复检查可直接删除”建议：生成数据中雪all_states=true，外层未实际改块时确实可能再次进入此处检查。但内层同时保护绕过外层循环的直接randomTick调用，外层还要维持相变优先于植物/冰锥的顺序。

本批不增加每位置缓存/ThreadLocal来跨方法传PhaseAttempt，不绕过Snow.randomTick去破坏其他回调，也不删除其直接调用保护。保留两处各自入口上的保护，只集中实际规则；这是已作出的等价性选择。若仅保障原版单一路径，可让雪完全接管相变，但会改变其他直接调用或特殊数据组合的行为，不作为纯整理。

验收：直接调用与外层调用、活跃材料、休眠材料、未追踪材料、多层/单层雪、亮度边界、相变成功和延后；每条路径都不得绕过材料能量或重复掉落。

实现判断：业务集中可实施；无新增上下文状态条件下，不承诺把两条保护强并成一次。

## 7. 篝火方块：CampfireBlockMixin_TimeLimit

源码：[篝火方块](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/CampfireBlockMixin_TimeLimit.java)、[源索引](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/source/minecraft/PhysicalSourceSpatialIndex.java)。

### 点火合并的完整链

```text
最终Chunk写入通知
→ 燃烧篝火从无到有
→ 取得或启动本维度input
→ PhysicalSourceSpatialIndex.resyncBlock登记期望状态
→ 20tick cut上的flush
→ SourceInputWindow/累计输入接管
→ mailbox与区域任务
→ 成功提交后消费J
→ 发布温场供玩法查询
```

工程步骤与拒绝处理：

1. 删除CampfireBlock Mixin的专用onPlace override和thermal运行时import。将`onCampfireIgnited`的实际逻辑合入第2项最终写入入口，删除不再使用的旧外部入口，不保留备用路径。
2. vanilla字节码已确认：onPlace早于BE建立/更新，最终返回晚于该步骤。新入口可直接登记，并且拒绝后区块位置表已包含正常新BE。
3. input已存在时，resync成功即结束登记，随后补发现再次观察同位置必须幂等，不创建第二份源或消费同一J。
4. resync返回false时，复用`pendingSourceChunks`保留该Chunk。显式的补发现请求应使该Chunk的旧游标失效并重新排入；内部正常分片续读不调用这个重置入口。原因是旧快照可能建立于新BE加入之前，简单把同Chunk重新放入队列并不能保证包含新位置。
5. 失效游标只释放位置快照，不回滚已登记源；重建仍遵守每tick最多一个新快照。容量不够时不忙循环、不伪造几何变化，源槽释放和下一tick沿现有队列恢复。
6. 初次start因额度拒绝时，把既有`startupCapacityWait`的Long值扩展为`StartupRetry(capacityRevision, preferredPosition)`，不另建启动队列。新的点火请求可以更新preferred位置，但额度版本没变不再次分配。只有无active input时的LevelTick检查这一个记录；额度版本没变就返回。版本变化后，优先验证已加载的preferred位置仍为有效燃烧源并重试；若它已消失，复用一次已有加载源扫描寻找其他有效源，无源则移除记录。每次额度版本至多尝试一次start；找到候选后如果被拒绝，立即停止该次扫描，不能在同一版本对每个热源重复启动。
7. start成功移除记录；再次额度拒绝更新版本并保留一个位置。Level卸载、closeAll和配方重载清除记录。重试不强加载区块，不依赖玩家再次点火，也不为未接管时段虚构历史输入J。

最后一项是对已有等待记录的扩展：最多每个等待维度一份小记录，不是新的源权威。active运行时的正常热源链不增加该检查。除额度拒绝外的启动异常继续记录与清理，不把它自动解释为额度等待。

### 交互与投料链

use/stepOn/getStateForPlacement的实际业务进入`CampfireGameplay`。原Mixin保留对应签名和返回行为。保留研究烹饪门槛、主副手/shift、空手提示、默认未点燃、客户端粒子、地面物品owner限制和容器物掉落。

共享燃料计算使用明确的`MAX_FUEL_TICKS=19200`、`BURN_MULTIPLIER=3`，保留现有整数除法顺序。只共享容量计算，不强并地面投料和能力插入的不同接收策略。一次stepOn复用一次BE查找，不新建FuelState。

现有地面投料缺少能力插入的`maxcs<=0`拒绝条件，超上限存档时可能出现不同边界行为；本批纯整理不偷偷修改。`-1337`等特殊值与容器规则也原样保留，行为修复应另列具体变更。

删除项：专用热onPlace、旧点火公开入口、两处重复燃料算式、Mixin内大段交互实现。use仍被覆盖，不能把它改为HEAD总cancel再宣称侵入已减少。

验收：现有3项源回归，加“游标快照早于新BE、容量释放后恢复”这一必要场景；初始start被拒绝后无玩家操作也能在额度变更后恢复；首源中途熄灭/卸载、重入改块、已存在源幂等。输入接管、提交和保存保持原J守恒合同。

实现判断：可实施。最终通知时点的同tick中间态差异见第2项；等待记录和快照重置是使新链闭合所必需的工程细节。

## 8. 篝火实体：CampfireTileEntityMixin_TimeLimit

源码：[篝火BE](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/CampfireTileEntityMixin_TimeLimit.java)。

```text
IItemHandler.insertItem或地面投料
→ CampfireGameplay共享容量计算
→ 原BE的lifeTime更新与dirty
→ 点燃状态下原cookTick返回
→ 原lifeTime扣减/下一次熄灭
→ setBlock进入统一世界写入通知
→ 源停用与未消费J结算
→ 原NBT LifeTime保存/读盘
```

工程步骤：lifeTime字段、已有能力对象、ICampfireExtra与NBT键保留；把接收计算和必要交互实现迁入CampfireGameplay，能力仍直接引用该BE。删除重复构造归零注入，在普通新BE测试中确认初值0。不要另加全维度定时器、Capability状态副本或燃料缓存。

完整时间语义：lifeTime>0时减一，变为0后下一次cookTick才熄灭；不点燃时不按这个回调扣减；区块停止ticking时暂停；卸载后读盘继续。普通篝火仍每次扣减标记真实区块dirty，已有无比较器时减少完整通知的做法保留。

simulate不得改燃料或输入ItemStack；实际插入的返回栈、地面容器与物品消失规则保持各自现状。管道查询只改实现归属，不改能力方向和对象生命周期。

验收：复用真实木棍烹饪/最后燃料tick熄灭，以及保存卸载重载两项；增加共享helper的调用不应新增每次tick对象。检查特殊燃料、simulate、满容量和容器物品，但不借整理修正其旧行为。

实现判断：可实施，共享规则改善可维护性；tick/NBT接入继续保留。

## 9. 随机资格：BlockStateBaseMixin_RandomTick

源码：[资格接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/BlockStateBaseMixin_RandomTick.java)、[转换数据](../src/main/java/com/teammoeg/frostedheart/content/climate/data/StateTransitionData.java)。

```text
原版BlockState.isRandomlyTicking
→ true：立即返回true
→ false：冰锥基底或现有数据定义的随机相变资格
→ Section计数/抽样资格
→ 第5项随机样本处理
```

工程步骤：在现有RETURN钩子第一行增加原版true快路径。false路径的tag、ThinIce排除和数据读取保持。不新增一份状态资格数组，不变更`StateTransitionData`缓存权威。

重载闭环：构建新转换表 → 比较随机资格变化的状态 → 只对包含这些状态的已加载Section重建计数 → 后续抽样采用新资格。WATER继续不因相变定义而随机tick，表面水由第5项处理。

删除项：原版true情况下不必要的tag/数据查询，失效注释；钩子本身保留。两次额外查询无副作用，快路径不消耗随机数。

验收：原版true、原版false但有转换、无转换、冰锥基底/ThinIce、WATER，以及数据重载后的Section资格。无需全量性能矩阵。

实现判断：可实施，性能收益仅表述为减少查询，具体占比未测。

## 10. 破冰：IceBlockMixin_Melt

源码：[冰接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/IceBlockMixin_Melt.java)。

```text
原版Ice.playerDestroy
→ 原基础掉落/统计
→ 原精准采集判断
→ 非精准采集进入Level.dimensionType前的窄拦截
→ !(实际温度 > WATER_ICE_MELTS)：取消后半段
→ 否则继续原版ultraWarm/下方支撑/水替换
→ 正常世界写入及热材料维护
```

工程步骤：删除整个playerDestroy Overwrite、其super调用副本及相同原版分支。在上述调用点加入一次小型cancellable Inject；本地1.20.1字节码已确认该顺序。温度只在非精准采集路径读取，保留原`!(t > threshold)`的NaN结果。不能在HEAD取消基础掉落。

另一个空randomTick规则继续抑制原版光照融冰，真实融化由材料能量控制。删除注释旧融冰代码、未使用Shadow和导入；不把把空Overwrite换成总取消回调当成性能或侵入收益。

验收：精准/普通工具、冷/暖、超暖维度、下方可支撑/不可支撑、基础掉落只执行一次、原版随机融化仍被抑制。测试温度不改变正常采样/求解精度。

实现判断：可实施，确实删除一个完整原版方法副本。

## 11. 禁冻：BiomeMixin_FreezeWater

源码：[Biome接入](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/thermal/BiomeMixin_FreezeWater.java)。

```text
原版天气、SnowAndFreezeFeature或其他shouldFreeze调用
→ 现有全局false规则
→ 原版结冰不发生
→ 游戏运行后的水相变按表面样本/材料能量处理
```

工程步骤：保留当前单行false，删除整段注释掉的旧实现、无用Shadow和导入，移入统一目录并登记其全局作用。不恢复原版冻结，不修改世界生成配置，也不再包一个总是返回false的控制器。

删除整类的具体后果已经确定：SnowAndFreezeFeature也会恢复原版冻结，超出“整理代码不改玩法”的范围。用事件全局替代当前没有等价入口；改为反射/ASM只是换工具，不采用。

验收：规则仍应用于运行时和原版生成调用，未因热系统active状态而变化；包迁移后正式配置只登记一次。

实现判断：保留最小规则并清理文本可实施；按本轮语义不能删除这个规则。

## 总体实施顺序与交接

1. 先完成第2、7项最终写入入口与源拒绝闭环，再移除旧onPlace。配套更新旧内部API的测试调用；不留下双入口备用。
2. 完成公开API判空、Section编码复用、资格true快路径、保存内部调用包装与破冰窄拦截。保存prepared和source startup记录按上述所有者结束。
3. 提取两个真实业务实现，迁移11类及配置，删除旧包名、重复实现和失效注释。第4、5、6、11项按已选保留范围执行，不临时追加事件总线或上下文框架。
4. 静态核对链路：真实变化 → 记录 → 输入接管 → 区域任务 → 提交/取消 → 发布；保存/卸载 → 封存 → 写入/拒绝 → finish → 引用释放。检查资源失败不制造几何变化、未提交J不提前消费。
5. 整批完成后统一编译与最小联合验收，不逐项反复跑全矩阵，不使用JUnit，不重编历史源码，不设路径敏感hash门槛。

## 联合验收与删除证明

优先复用3项源回归、2项燃料生产回归、NativeCheckpoint与保存隔离的必要场景，以及休眠材料替换。只为新增工程边界补短场景：旧发现快照遇新BE、start额度拒绝唤醒、维护保存准备返回false、破冰分支和随机序列。24,000tick长相变场景不代替这些接入检查。

性能检查针对公开API的packed NBT加载成本和当前保存等待分别记录，不拿Mixin行数减少当提速。正常温场算法、步长、输入量、粗细模式和多核策略不因可维护性整理调整。

完成时必须能搜索证明：

- 原包下11个旧文件与旧注册项已退出；无重复类或测试Mixin进入正式包。
- CampfireBlock的专用热onPlace和旧点火公开入口已删除；完整源链只有一个写入完成入口。
- `frostedheart$hasBlockEntityPositions`及为它增加的ChunkAccess继承、构造适配已删除；无新增AT字段访问。
- 保存Mixin不再读取chunkSaveCooldowns，不再在processUnloads/saveAllChunks外层重复包saveChunkIfNeeded。
- Ice.playerDestroy整段Overwrite已删除；原版基础掉落和精准采集仍执行。
- 篝火容量/倍率计算只有一个实现；Mixin没有第二套业务副本。
- 层雪和材料相变仍保护直接调用；保留的大段随机循环明确记录为尚未消除的侵入，不以文件移动冒充完成。

## 研究证据与当前状态

核对了11类源码、运行时输入/源索引/Page/相变/辐射/Checkpoint所有者、生成的雪转换数据、现有重点GameTest入口，以及本地Minecraft1.20.1映射类的Chunk.setBlockState、saveChunkIfNeeded、Ice.playerDestroy和SnowAndFreezeFeature字节码，并对照Forge47.3.0保存/邻居事件补丁。没有编译或运行新的测试。

## Outcome

2026-10-31用户进一步要求专用玩法不得进入公共基线，已由[专用接入修正计划](2026-10-31_13-56-49_thermal-specific-hooks-and-snow.md)取代第2/7项的篝火入口合并决策。当前源码以CampfireBlock自身onPlace通知点火，公共Chunk仅维护材料；历史13项结果仍只对应本计划原实现，不作为后续批次资格。

已完成选定实现：11类与注册集中；最终方块写入统一源启动和材料修正；移除专用热onPlace、判空继承适配、保存冷却字段依赖和Ice.playerDestroy副本；实际气候/篝火业务归位。外层随机循环、岩浆薄通知、雪直接调用保护和全局原版禁冻按本方案保留。

生产代码相对本轮开始净减少115行（不含测试/文档，计入此前待提交15行变化后的起点）。assemble/compileGameTestJava通过，13项重点回归最终全过，各GameTest进程关闭rootBytes=0，热系统错误0。正式JAR在独立Forge47.3.0读取原v6世界、继续保存并正常退出；磁盘温场样本540→840tick、系数全部有限，燃料18643→18359。正式包无旧热Mixin类或GameTest类，受保护旧粒子4类保留。

首次源组4/5通过，失败来自“填满软额度一定拒绝有界启动”的测试前提。生产额度与实现未为此改变；用例改为建立受控等待状态，再验证真实预算释放、LevelTick、发现与worker接管。生产代码首轮构建后保持同一版，仅重编修订测试；其余重点组按各自记录通过。具体日志、版本范围及限制见验收JSON。

本批未增加反射/ASM/AT等生产接入，没有引入第二套调度或源队列。公开pending NBT复制代价和维护保存同步等待仍存在；没有新的整体CPU对照或完整整合包百人联网验收。
