# 温度区域运行时：输入、任务与保存所有权

- Status: `Current for the sole regional implementation and active-time service target; stated scenes validated at their listed allowances`
- Last verified: `2026-10-31 public specialization removal: final assemble and15 focused Forge cases passed, thermalErrors0/root0; earlier results retain their own build scope; defaults1000ms/s and128MiB`
- Scope: `SHARED_GRID输入接管、区域任务、资源准入、粗细计算、查询、v6保存与关闭`
- Code anchors: `MinecraftThermalInput`, `ThermalMemoryBudget`, `ThermalWorkBudget.Account.activeNanos`, `ThermalWorkerPool.regionalWorkAllowanceNanos`, `RegionWorkPolicy`, `FHConfig.COMMON.THERMAL_RUNTIME.continuousAirWorkBudgetMsPerSecond`, `ThermalDimensionEngine`, `SpatialSourceTimeline`, `SpatialSourceIngress`, `SpatialHeatStore`, `AirLoadTable`, `AirFieldLayout`, `CoupledThermalOperator.ProjectionPreparation`, `RegionalAirRuntime`, `RegionalQualityControl`, `RegionJob`, `RegionBuild`, `RegionalWorkQueue`, `QueryPublication`, `RegionalQueryIndex`, `RegionalCheckpoint`, `ThermalPageHandle`, `ThermalSignatureTable.samePhysicalProperties`, `AirRouteValidity`

本文件描述当前源码与服务取舍。正式运行只保留区域共享网格；`LegacyThermalDimensionEngine`、FUNCTIONS/scalar运行入口、`ThermalDimensionProcessor`双协议和旧`ThermalInputStore`已经删除。旧存档的函数布局求值与投影属于数据兼容，继续保留；它们不能再选择第二套调度。[本轮退役计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)记录删除与验收范围。2026-10-24的27组/66项只证明清理前的对应版本，不自动转为本轮资格。

2026-10-25后续[模块复查与实施](../../diary/2026-10-29_23-38-00_thermal-development-history.md)继续删除了旧标量推进、节点源积分器、局部函数生成/基底筛选，以及`ContactPreparation`的旧全局合并控制。`ThermalSolver`仅提供当前拓扑、材料边和引用管理；`ThermalSourceLedger`只保留当前源身份、端口绑定和实际J结算，内部不再选择第二种积分模式。旧函数存档的读取/求值与向当前网格投影继续保留。两个旧源测试已改为真实Timeline/Store/区域提交，并检查负J瞬时输入、饱和拒收和后续不重放。这批编译与重点验证见[子系统清理摘要](validation/thermal-subsystems-cleanup-2026-10-25.json)，之前的[静态复查证据](validation/thermal-obsolete-subsystems-review-2026-10-25.json)保留调查当时的状态。

随后[表示与交接复查中的实施](../../diary/2026-10-29_23-38-00_thermal-development-history.md)删除了活跃布局的空局部函数集合与重复计数、`AirLoadTable`旧布局模式、同步PCG/同步重建入口、无非空矩阵工作的`SparseThermalPreconditioner`、固定内部构建开关和单参数块快照入口。当前布局只携带实际节点、几何、网格跨度及父地址映射。该批曾保留分段PCG，现由下文的直接对角计算替换；实际粗档直接构造`MinimumThermalModel`。旧Checkpoint独立保存的函数数据解码、求值、合并及恢复投影继续保留，v6格式不变。

分页维护使用同一`ReadCursor`：`pageAirSampleTick`一次校验并固定Air槽代次、组件与拓扑，随后`captureValidatedAir`读取这些不可变帧，不再重复全页校验或另借读者。结束时仍检查真实Page身份；材料捕获保留独立时刻与实际状态检查。旧标量实时捕获和`hasSpatialAir/isPinned`读者模式别名已经删除；旧标量冷数据的`fromScratch/mergeAir`仍用于兼容。每次借用创建独立lease，用已有`readerLeases`归属管理一次释放；`frameReferences/payloadFamilies`继续管理帧/数组共享。保存ready/finish回执与关闭顺序保持。旧局部权重测试已改为实际温度/新鲜度/几何断言，候选stride2/4及旧宽函数存储诊断已退役；它们不是当前粗档算法。

末次[消费条件复查及实施](../../diary/2026-10-29_23-38-00_thermal-development-history.md)已删除四组残留：无法命中的父数值片段缓存、当前候选布局不可达的非节点投影/暖启动尾部、失去调用者的便利入口，以及LocalAirShape的空运行元数据。`seedMerged`不再收集和排序旧片段；真实父温场合并、选定的workspaceDonor与细网格静态图复用继续使用。粗单元仍为Minimum模型提供容量和传热边，材料额度公式保持。LocalAirShape直接接收存档branch，保留切片/三线性求值，不再持有空Page依赖、重复构造和扫描最大幅值。RADIUS/WIDTH仍服务实际支持搜索。

细档重建投影现由[直接对角计算](../../diary/2026-10-29_23-38-00_thermal-development-history.md)完成：`addNodalProjectionRhs`按旧温场或存档积分节点能量，`CoupledThermalOperator.ProjectionPreparation`计算`C_i=J_i/m_i`。其中`C_i`是相对参考温度的节点温度（°C），`J_i`是本次继承的节点能量（J），`m_i`是已有`massJPerK`中的节点热容量（J/K）；与细档传热使用同一份质量。每次最多处理1024行再检查时间片，空系统直接结束，只写空气前缀。结果必须有限，质量必须正且有限，`abs((J_i-m_i*C_i)/m_i)`不超过当前correctionToleranceC，源码默认1e-5°C。

细档不再为迭代器准备温度猜测或先扫描误差，也不会因为旧估计已在容差内就保留它；父温场仍作为J的来源。与旧PCG接受近似初值/浮点迭代的结果可有低位或原容差内差异，不降低剩余误差要求。粗档的`MinimumWarmStart`仍按原父快照/背景投影。`PcgSolver`、四份迭代向量、额外两份全局对角缓冲、旧片段投影质量矩阵和恒为0的PCG统计链已删除；失败仍在私有候选内恢复并由原任务清理，部分计算不会发布。已有构建计时保留，普通日志标记`projection=diagonal_direct`。

后续[材料耦合残留清理](../../diary/2026-10-29_23-38-00_thermal-development-history.md)将CoupledThermalOperator收拢为实际空气行。材料H/branch/等待由细档LocalMaterialBoundary或粗档minimumMaterialWaiting按body索引持有；旧材料arena列、Coupled.waiting和四份材料端点列/扫描已删除。通路编译先分流材料↔材料和材料↔空气，空气通路不再保留材料行备用路径。RegionalQuerySnapshot构造、复制和Restoration不再接收恒为0的waitingOffset。实际材料相变、帧/数组引用、Checkpoint释放顺序与v6持久化保持。该批属于清理，容量预估、缓存和0.1°C近似均未实施；性能计划按用户要求暂缓。

本次[内部架构复查与清理](../../diary/2026-10-29_23-38-00_thermal-development-history.md)已删除四个只写不读字段、无调用的`WorkerPageStore.installNaturalTemperature`和旧arena能量注入链及专属方法。测试注热在现有Simulation内通过`MaterialStateAccess`直接读取并更新区域材料状态，不再来回复制到arena；正式源J继续由Timeline/Store及区域提交处理。实际页面后继、父快照交接、共同任务控制和Checkpoint释放职责保持。城镇及其接口、玩家旧粒子模拟器排除；性能优化继续暂缓。

后续[通路维护尾项清理](../../diary/2026-10-29_23-38-00_thermal-development-history.md)删除了输出无人使用的`AirRouteCompiler.invalidatedSourceSections`及其收集/计费、区域层重复热量累计、无人读取的几何计时和无调用便利入口。正式重编仍使用dirtyBricks/changedSections、真实依赖及route退休，实际能量由`sourceLedger.confirmRecordedDelivery`确认，几何仍通过选定span的pin/lease保留与释放。该批只删代码，生产净减33行；首次统一编译后，仅真实柱子替换/读者退出和源取消重放两项重点场景各运行一次，全部通过并退出root0，测试源码未改。当前性能与粗细算法没有新增取舍，旧矩阵仍按原编译归属阅读。

随后完成[整套消费链清理](../../diary/2026-10-29_23-38-00_thermal-development-history.md)：统一检查入口、任务/粗细计算、源、查询、拓扑、保存与诊断/配置，再删除无消费的solver材料转发、AirPairs方向列、FarBoundaries汇总导热列及专属生成/计费、重复状态、孤立诊断和同步prepare等旧入口。真正空气边顺序、导热系数、天空面片、材料接口、v6兼容和实际观测保留；配置只修正旧PCG文字，值和默认不变。SnapshotRequest成为static内部类，去掉无用途的外部实例持有。

该轮统一验证发现了稀疏捕获与驻留描述的保存缺陷，现已修复：持有的Section在Chunk离开普通查询表时仍是有效捕获来源；驻留完整签名先通过`compareFullSignatures`标记实际差异，再替换，避免mask不变时沿用旧材料拓扑。保存仍使用固定Checkpoint，不猜测或跳过坏材料。新增驻留mask不变的STONE→AIR→STONE引擎回归，核对材料归属、捕获stateId和实际NBT编码。最终[27项验证](validation/thermal-comprehensive-cleanup-2026-10-26.json)全过、真实热保存错误0、root0；此前19项断言过却保存失败的记录没有归为通过。七类范围和当前产物复查已收敛，性能优化仍暂缓。

## 安装与启用

`gradlew assemble`生成安装包，并通过`reobfJar`完成Forge重映射。安装`build/libs/`内不带`sources`或`deobf`后缀的主JAR；版本名以[构建配置](../../gradle.properties)为准。GameTest夹具与观测代码使用独立source set，不作为热系统正式运行的依赖。

区域共享网格现在是唯一正式运行时，不再需要选择后端。在实际启动实例的`config/frostedheart-common.toml`已有`["Thermal Runtime"]`小节中，以下保留配置的源码默认值均为true；改动后重启客户端或专用服务器：

```toml
continuousAirNativeCheckpoint = true
continuousAirOverloadProtection = true
```

`continuousAir`和`continuousAirBackend`已退役，不再控制源码行为。启动日志为`Thermal runtime: regional shared-grid`，默认保存日志为`mode=native_ready`；将`continuousAirNativeCheckpoint`设为false只关闭提前安排原版卸载准备，普通保存仍封存区域状态。旧架构、精度预设和Checkpoint试验选择器不再覆盖正式入口。

内存准入目标和共享区域任务活动时间目标的源码默认分别为128MiB和1000ms/s；它们的语义及代价见下文。旧配置文件中的250会保留，不自动换算；已有实例如需采用新默认，应明确将`continuousAirCpuBudgetMsPerSecond`改为1000并重启。运行资格来自所列真实场景，不能把旧CPU额度和新活动额度视为相同资源配置。

## 源发现与温场加载

篝火自身的onPlace/onRemove及CampfireBlockEntity.onLoad调用onCampfireStateChanged，覆盖点火、熄灭、水熄、替换与实体加载。主线程读取当前已加载Chunk的实际状态，不强加载；BE加载通知还核对当前实体身份，旧实体的迟到回调不能操作新实体。新BE可以晚于onPlace建立，源登记只读取BlockState；既有燃料、能力、LifeTime存档和下一tick熄灭规则保持。索引观察和下一次cut向worker实际交出窗口是两个时点，enabledPhysicalSourceCount只统计后者。

captureBlockSnapshots期间不启动新源，旧源变化仅标记PhysicalSourceSpatialIndex每源sourceFlags中的STATE_RECHECK并复用dirtyOrder。tick末和flush前的reconcileCampfireChanges在捕获结束后核对最终世界状态；回滚保留源，确认移除才退休。捕获期间若flush，待重读记录继续留在原待办中；显式Chunk卸载清除该标志并按原beforeChunkUnload退休，不能被尚存的旧BlockState复活。没有新增队列或第二份源身份/热量。

LevelChunkMixin_DormantThermal的写入返回通知仅调用onMaterialStateWritten维护在线与休眠材料，不判断篝火、启动热源或调用源补查。真实共有的Section几何变化、材料与保存职责继续使用公共接入；专用玩法不能为了合并入口而加入公共基类或通用写入回调。

公共Section写入已经删除SOURCE_MUTATION、campfireFlags、篝火状态表标志、sourceRelevant/pendingSourceMutation及逐位置resyncBlock调用。Page管理不再持有PhysicalSourceSpatialIndex，整Section扫描resyncSection也已删除；仅源变化缓冲退出后，pendingNonGeometry及对应筛选/参数同步删除。显式整段Section/原始容器替换仍属于共同结构生命周期：只失效已登记源的依赖并由其自身索引核对，不扫描4096格寻找新篝火。后台原始容器替换只记录原有几何待办，再通过原服务器队列把源/辐射依赖维护交回主线程；任务核对input.closed和owner有效性，不能操作已退休的旧Section。

[本批计划](../../plans/2026-10-31_18-28-24_thermal-public-specialization-removal.md)保留删除清单和边界。合法方块/BE生命周期、现有加载发现和显式结构重同步受支持；任意第三方裸改Section且不维护BE或生命周期，不具有无条件发现未知源的承诺。共有材料/几何/保存、加载及通用缓存失效仍保留，不能笼统表述为热系统没有公共链路。

区块读入已有篝火仍使用getBlockEntitiesPos：它合并已实例化和packed NBT中的方块实体位置，发现代码读取对应BlockState，不为找篝火实例化全部BE。attachLoadedChunk的未知源发现先判断公开live表是否非空，否则使用公开位置集合判空；空表加载不排入发现。已知源登记被拒绝时的enqueueSourceDiscovery不做空BE过滤，因为CampfireBlock.onPlace可能在新BE加入之前请求补查；后续tick才创建位置快照。没有新增队列或等待状态。已移除为判空新增的ChunkAccess父类访问和附件方法；只有packed NBT时仍有一次加载准入集合复制，空表也有空集合创建成本。活跃游标最多8个，每游标每tick至多32位置，总逐位置额度256，每tick最多启动1个新快照。无待办时直接返回，有待办时通过原游标表迭代器推进和移除完成项，不创建逐tick的ArrayList副本。跨tick快照仍保留；完整合并/复制不属于256次处理额度，冷启动也有位置遍历，不能声称完全无扫描或耗时有固定上限。

源登记容量拒绝会复用pendingSourceChunks。显式补查请求使该Chunk的旧游标失效，避免沿用新BE加入前的快照；正常内部续读不重置进度。首次启动额度拒绝由startupRetries保存一份StartupRetry(capacityRevision, preferredPosition)，无active运行时时由tickLevel在额度版本变化后尝试恢复。每版本最多一次启动，优先位置失效才复用已加载源扫描；不强加载Chunk，不估算未接管历史J。启动成功、维度关闭和closeAll清除记录。

2026-10-30的[源发现阶段验证](../../diary/2026-10-30_17-46-04_thermal-source-discovery-allocation.md)编译打包与当时3项源回归通过。该记录属于改用公开API及最终写入通知之前的版本，不自动转为当前接入改造资格。仅供旧测试调用的整块同步discoverChunk(LevelChunk)已经删除，正式发现使用分段游标。

ChunkDataEvent.Load先解码v6温场、材料和待处理J；需要在线计算时，按当前方块签名捕获相关Page/Brick，重建实际连通/传热拓扑并投影旧状态。只读玩法采样可以借用有效休眠温度及既有冷却规则；它不因读取一个位置而扫描全维度BE或强行加载未加载区块。保存中的producer扫描与拓扑构建仍有各自的成本，见后续待办。

2026-10-30[正式服务端验证](validation/thermal-dedicated-server-2026-10-30.json)使用生产JAR、Java17/Forge47.3.0和正常forgeserver入口：新世界建房点火、保存、停服、新进程重启后继续保存通过。v6温场531系数均有限，样本260→540tick。补充源回归在预先加载的远处区块构造空气房间，直接放置燃烧篝火并使用真实燃料，不手动补发onChunkLoad、不依赖receiver启动；沿用同高度近远温差>0.05°C检查。完整整合包配置及100联网玩家不在本项资格内。

## Minecraft接入与玩法归属

前批[接入集中验收](validation/thermal-integration-2026-10-31.json)和[专用点火/积雪验收](validation/thermal-specific-hooks-2026-10-31.json)保留各自编译范围、失败与限制。当前[公共专用链路移除验收](validation/thermal-public-specialization-removal-2026-10-31.json)对应进一步移除公共源写入、随机资格和液体邻接钩子：最终同一轮源7、接入6、燃料2项共15项全部通过，热错误0、关闭root0。首版15项通过后，调用约定复查补齐后台容器替换的主线程交接并扩展同一源测试，再重新构建、运行最终15项。启动重试仍使用受控等待状态与真实预算释放，没有声称填满软额度一定会拒绝有界启动工作。没有新的整体性能百分比或百人联网资格。

正式接入统一位于`com.teammoeg.frostedheart.mixin.minecraft.thermal`，由原`frostedheart.mixins.json`连续登记9类，不增加另一份启动配置。BlockStateBaseMixin_RandomTick和LiquidBlockMixin_ThermalRadiation的源码与登记已删除。数值内核、区域任务和粗细控制仍是普通Java实现。

| 接入类 | 当前职责与实际处理者 |
|---|---|
| LevelChunkSectionMixin_ThermalInput | 只读取一次owner，把已取出的状态编码交给输入/Page管理；无每次改块事件对象 |
| LevelChunkMixin_DormantThermal | 保存区块休眠状态与材料版本，最终写入仅维护在线/休眠材料，无篝火判断 |
| ChunkMapMixin_ThermalMaintenanceSave | 原版saveChunkIfNeeded真正调用save前准备热状态；保留原卸载future/executor身份 |
| ServerLevelMixin_TemperatureUpdate | 接入post-thunder阶段，调用ClimateBlockUpdates；仍取消原版后续流程 |
| SnowLayerBlockMixin_Melt | 直接调用也经过ClimateBlockUpdates.tickSnowLayer的材料相变保护 |
| CampfireBlockMixin_TimeLimit | 仅作用CampfireBlock；onPlace/onRemove通知源状态，保留原移除行为，交互业务在CampfireGameplay |
| CampfireTileEntityMixin_TimeLimit | 自身onLoad核对身份并通知源；原BE持有燃料/能力，原cookTick和LifeTime存档，复用CampfireGameplay计算 |
| IceBlockMixin_Melt | 抑制原版随机融冰；在原掉落/精准采集之后，仅按温度阻止破冰后半段产水 |
| BiomeMixin_FreezeWater | 保持全局原版禁冻，包括SnowAndFreezeFeature的生成调用 |

ClimateBlockUpdates承载实际天气、植物、随机样本和层雪规则。自定义区块随机循环仍保留，气候基温读取、原样本内部的随机调用、改块后跳过block/fluid的顺序保持；移动实现不表示已消除整段流程接管。外层相变和雪回调分别保护各自入口，不增加跨方法上下文缓存。

随机资格现在由该气候循环自己判断：原版Section.isRandomlyTicking为false时，才通过调色板maybeHas检查当前相变或冰锥资格；样本按原randomTickSpeed推进，未被相变/植物接管时仍按“原版或气候资格”调用方块，再调用流体。WATER仍走表面相变。StateTransitionData.updateCache仅发布规则，不再扫描所有已加载Section重算原版计数。没有新增资格缓存/版本或计数器。代价是部分Section增加调色板检查，残留条目可能造成无效果样本并推进随机数；BlockState.isRandomlyTicking对原版/第三方恢复原版结果，扩展只在本模组气候循环生效，不能承诺逐随机调用序列或新CPU收益。

静态辐射只消费通用状态变化，不接入LiquidBlock.updateShape或全局邻居事件。BlockRadiationIndex根据现有面公式的11个读取依赖位置，在一个原dirty锁内归并为至多6个不同4×4×4 Brick，只标记已覆盖knownBrickMask；跨Section及水平邻居上方的对角依赖、整段身份替换和Chunk重载一并失效。不创建逐改块集合/坐标对象，不强加载，不改变面积/功率/流体高度公式、20tick刷新或每tick64块首次捕获额度。新增/改变相同profile的流体高度也会通过原始状态变化使缓存失效。共有的缓存失效通知仍存在，不能称为零公共观察。

额外积雪直接在tickAfterThunder的气候/随机更新结束后执行placeExtraSnow。旧ServerLevelMixin_PlaceExtraSnow在原tickChunk的RETURN处，被中段取消绕过；该独立类、登记与ClimateCommonEvents中的旧实现已经删除。原配置enableSnowAccumulationDuringWeather（源码默认true）、snowAccumulationDifficulty（默认16）、降水/冷群系、温度不高于WorldTemperature.SNOW_REACHES_GROUND（源码常量−13°C）、方块光小于10和额外分支五层上限保留；主气候分支仍使用原GameRules积雪上限。地表替换与既有地表的SNOWY属性实际写入，修正丢弃不可变setValue返回值的问题。恢复这条玩法会增加此前被绕过的概率与位置采样，后续tick的随机序列可能变化，不能声称与旧缺陷版本逐随机调用等价。

维护保存的调用点已收窄为`saveChunkIfNeeded → save(ChunkAccess)`，不再在外层复制可访问性/冷却筛选，也不读取chunkSaveCooldowns。原始干净区块直接走原版，不因热准备变脏。相关dirty区块通过runMaintenanceSave封存，拒绝则返回false并保留dirty，成功后才在try/finally范围设置preparedMaintenanceSave；实际写入成功才计数。captureRegionalCheckpoint仍可能同步等待接管和finish，当前没有“普通保存零等待”的合同。

破冰保持原版基础掉落与精准采集，非精准采集到Level.dimensionType前才做温度拦截；`!(temperature > WATER_ICE_MELTS)`继续保留不可用温度时的结果。篝火保持19200tick容量、倍率3、原整数除法顺序、最后燃料tick后下一次cookTick熄灭、卸载暂停及LifeTime键。地面投料和能力插入的原边界策略保持，不因共享计算而统一为另一套行为。

## 数据与任务的所有者

2026-10-29[提交前简化](../../diary/2026-10-29_23-38-00_thermal-development-history.md)已实施，当前验证见[本批验收](validation/thermal-preproduction-simplification-2026-10-29.json)，不沿用下文历史r4作为新产物资格。普通结构准备保留同类父区域的已提交档位及必要降粗，粗转细由RegionalQualityControl发起QUALITY_RECOVERY候选，沿已有依赖取消、当前时刻投影和提交路径；qualityChangeIn及普通结构的隐式粗转细退出。纯自然温度更新继续原快路径。

源控制直接从SourceInputWindow/SpatialSourceTimeline.Descriptor交给ThermalSourceLedger.BindingUpdates，顺序仍为注册、按旧绑定结算前缀J、功率变化、启停变化、卸载，through最后推进。ThermalSourceBatch、Builder、applyEvent与batch/index回调已删除；登记/身份/源J权威没有增加新表示。RegionalQueryIndex按已有Template.PageIndex为本次Page单元表预分配；全局cells/regions按先删后加的实际数量预留，Page/Section交错替换仍保留保守峰值。计费、提交锁内身份复核及旧读者持有继续执行。

RegionalQualityControl.hasReadyWork/next共用selectedStep，自动策略与显式意图使用相同就绪判断。临时结构边界、封存或其他恢复占用时，区域保留在原ready集合中；hasReadyWork只报告当前可执行的变化，取消、退休和唯一recovery候选仍归原控制器。首轮删除隐式恢复后，大区持续粗档直到运行中断；本批补齐上述就绪不一致后，千源撤压恢复与400tick稳定期通过。恢复退避和额度没有修改，持续施工可能延后恢复。

真实物理源熄灭沿PhysicalSourceSpatialIndex.observe的既有remove路径退休；WorkerPhysicalSourceBindings.unloaded在移除后通知现有诊断观察器，避免测试保留旧发热状态。FrozenSources准备固定开销包含空容器与临时计数表，共768B，再按256B/activeAirPort和64B/dirtyBrick预留；这不是新增容量上限。生产12文件净减300行、无新顶层类型；保存、源重放、区域隔离、粗细切换和压力服务的具体结果与分批编译归属见验收，不代表100真实联网玩家或完整整合包重启验收。

| 对象 | 拥有的数据 | 交接与结束条件 |
|---|---|---|
| `SpatialSourceTimeline` | main产生的积分J、两个普通运输槽、一个退休终结槽及有界尾部 | Store完整接管窗口后归还运输槽；退休终结可以先于旧普通槽释放而公布 |
| `SpatialSourceIngress` | 源代次顺序、运输引用、最多两份已接管窗口的调度引用 | 绑定按代次和物理时间有序，J不归Ingress重复持有 |
| `SpatialHeatStore` | 全部已接管未消费J、尚未解析目标、已解析记录和Claim | 只有成功提交、实际卸载导出或既有sink才结算J；取消归还Claim |
| `TopologyIngress/TopologyInbox` | Page几何、环境、材料和世界控制 | 局部结构任务领取；不等待Page下全部源完成 |
| `RegionJob/RegionBuild` | 本次时间前缀、Claim或构建父引用、工作空间、私有结果 | 真实helper退出后取消释放；成功发布是唯一提交点 |
| `Accepted/QueryPublication` | 不可变已提交温场与查询目录 | 仍有效的旧状态持续可读；真实世界变化决定几何有效性 |
| `RegionalCheckpoint` | 封存帧引用、固定目录、剩余J、producer尾部和保存工作空间引用 | 主线程写入当前v6内容，finish通知协调者释放和恢复本区 |

`RegionalAirRuntime`在构造时接收唯一`QueryPublication`并以final字段持有，不再后续attach或选择无发布器直接提交的模式。材料控制、最低模型和普通数值结果都经同一publishRegion回调提交，父帧借用也使用该发布器；帧载荷始终按查询持有登记，旧standalone统计入口和专属recordBacking包装已删除。

[固定选项与发布依赖复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)核实上述构造条件和实际调用者。该批统一构建后，源取消重放、粗细转换与区域生命周期5项同轮首过，热保存错误0、关闭root0；验证仅覆盖该批清理范围，既有压力和性能记录保留各自版本归属。

`RegionalQueryIndex`在现有发布monitor内维护稳定region/slot索引；每个Page的单元和brick关系在发布后不可变。结构候选只准备旧、新区域涉及的Page，其依赖为父Template和这些Page的对象身份。无关Page提交不会使提案过时。索引扩容在物理提交前准备，移除条目保留已登记容量；普通数值提交只交换本区域的Accepted帧。solver和查询登记接收同一帧对象，不再维护另一份全维度快照数组。

`QueryPublication`已删除整维度arena-slot双缓冲和仅服务旧缓冲的版本锁。区域目录交接由已有短发布monitor保护；读者继续按需保留旧Page、帧及其几何，最后一个使用者释放后才归还占用。数值求解内部的候选数组仍保留。区域更新时新旧快照可以重叠，所以这项删除不等于整体内存减半。

main用`ThermalInputEnvelope`一次交出按Page拆分的控制载荷和源窗口，随后只保留尚未被接收的`pendingSubmission`。`TopologyIngress`运输与`TopologyInbox`领取直接复用同一个Page载荷，取消重新包装后再按Page分组。源成员变化只为实际涉及的Page建立mixing控制。`ThermalCompletion.effectSequence`标记有序世界效果，不能解释为某个整维度物理批次已经算完；旧CutTiming、TopologyRetry和重复提交摘要已删除。

`ThermalDimensionMailbox.submit`保留实际接管回执：关闭或终止时返回false，成功返回前由`ThermalDimensionEngine.offerInput`完成控制和源窗口转移；内部offerInput为void，重复序列直接结束。已接管源的`prepareSpatialInput`和构建的`prepareInputHandoff`使用同一完成预留，不再返回恒为true的失败判定。`SpatialSourceIngress`只按窗口数量、冻结和bindingWait检查就绪；旧源capacityWait、routingMemory集合及专属容量唤醒已经删除。可选细档工作空间和真实拓扑准备的容量等待仍由原任务控制。

`MinecraftPageManager.processAdmissions`按现有每tick捕获额度和Chunk可用性处理页面，不再持有始终为0的PageEntry.retryAfterTick或始终为false的restoreAirOnRetry。没有新增Brick的居留刷新使用已有状态；新增Brick仍按addedBrickMask触发实际休眠空气恢复，保存和恢复格式保持。

[参数与容量协议复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)核对了上述调用链及全部非城镇thermal文件的未消费参数/恒返回值。该批统一构建后，源取消重放、粗细转换和区域生命周期5项同轮首过，保存错误0、关闭root0；前轮测试保留各自编译归属。

材料控制通过`MaterialStateAccess`读取和更新区域权威；普通显热材料不读取相变专用请求编号。`MinecraftPhaseController.apply/matchesMaterialRequest`按实际方块状态、材料改变记录、当前帧中的请求编号与分支、目标状态和玩法条件处理已完成相变。同Page内其他方块变化不会单独使这个材料请求失效；旧的请求整页`geometryRevision`门槛及其回调、消息字段已删除。区域数值帧和真正发生替换的材料仍遵守各自的身份与几何规则。

物理源的`WorkerPhysicalSourceBindings`固定使用`AirLoadTable`记录世界面身份，目标只从已提交`WorkerPageStore`解析；不再有无绑定表的旧空气节点分支、准备几何标志或专属转接方法。已绑定面继续先保留新身份再释放旧身份，冻结窗口仍复制世界身份。`SourceBinding.thermalNode`保留实际材料热量输入合同，与物理空气源的旧备用方式不同。

天空散热通过`FarFieldSettings.skyFaceConductanceWPerK()`计算`baseConductanceWPerK × 16 / (16 × referenceOpeningAreaBlocksSquared)`，再乘面片通风值`v/100`；前者单位W/K，参考面积单位block²，正式入口参考面积仍为32block²。乘除顺序沿用既有直达天空路径；旧directSkyExposure=false分支和无人消费的continuationDistanceBlocks已删除。`captureDormantPage`保存实际发生变化时始终调用chunk.setUnsaved(true)，接口不再携带恒true的markDirty选项；ReadCursor仍在finally释放，v6不变。

`QueryPublication.tryReadMaterial`及`ReadCursor.tryReadMaterial`按区域帧内的槽代次和状态身份读取；接口不再接收没有参与判断的minimumTopology参数。主线程仍核对实际Page身份及材料改变记录，保存继续借用同一不可变帧。`WorkLimitedException`只表示真实拓扑规模限制，旧资源拒绝分支删除后不再持有恒为true的topologyLimit分类字段；细档资源拒绝仍通过其实际异常类型处理。

材料↔材料的真实传热由`ThermalFragment.MaterialContacts`提供，细档`LocalMaterialBoundary`按原顺序做对称扫掠，粗档从相同实际边构图。`RoutedContacts`的正式生产者只生成空气↔空气、材料↔空气通路；不存在“两端都是材料”的运行数据。[构造不变量复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)已删除为该不可达分支维护的空表、空数组、冻结方法和双路扫掠，真实材料边及空气通路的活动判定继续保留。该批五文件净减55行、测试零改动，[10项重点验证](validation/thermal-construction-invariants-2026-10-26.json)一次运行全过、保存错误0、root0；此前27项仍属于上一编译。

`ContinuousAirSolver.stateC/candidateC/rhsJ`只准备空气行，材料权威仍为独立的H、branch和waiting；查询按材料law求温度。`powerW`仍覆盖空气和材料行，功率半步在没有空气节点的材料区域也执行。瞬时材料输入使用`LocalMaterialBoundary.law`中的同一不可变物性；operator继续冻结环境与真实路由。`RegionalQuerySnapshot.copyStateTo/Restoration`只恢复这些实际状态，不再生成或复制未消费的材料温度尾段。专属重复物性数组、冻结选择标志及恒为0的`rejectedSteps`已删除，实际acceptedSteps与成本计量保留。

`RegionalAirRuntime.prepareJob`按Accepted.minimumQuality分流：细档使用`CutLoadBuffer`和`ContinuousAirSolver.CutCursor`，最大子步仍为`ThermalInputBatch.CUT_INTERVAL_TICKS=40`；粗档直接把Claim记录送入`MinimumRegionStep`，按现有规则准备最多8个时间段。细档入口不再保存旧粗档时间合并及可变最大步长参数；Store的时间汇总和质量政策中的aggregated判定仍有效。

`RegionBuild`的构建几何直接使用求解器已有的`regionScope`；图容量仍读取同一Scope内的fineVolumes/fineEdges/airComponents，不再保留不可达的通用几何重数备用扫描。真实几何遍历保留分段`AirGeometryView.cellCursor`，旧的无调用forEachCell接口和覆盖已删除。`RegionWork.prepareFrame`在任务就绪前完成帧及最低摘要，提交直接使用该帧；取消释放私有帧，接受时只记录`acceptedSnapshot`，没有提交端同步补帧入口或第二份“最后快照”字段。[清理闭合复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)记录对应构造不变量和本批验证范围。

`ContinuousAirSolver.CandidateBuild`使用`CandidatePhase`依次表达捕获布局、支持搜索、节点编号、体积算子、材料索引、接触、装配、材料边界、状态初始化和节点能量/温度投影。粗档在`ALLOCATE_STATE`完成自身暖启动后直接进入`COMPLETE`；细档继续两个投影阶段。暂停点仍为状态初始化之前，截止时间与异常恢复出口保持。枚举只命名现有构建进度，不另建一套任务状态机。`ContactWeightTable`按4096项分块保存double权重；原始位相同的整块只存一个值，其他块沿用无损字典或密集数组。字典暂存只在实际需要时创建，旧块共享、BackingMeter登记和最后引用释放沿原路径。实现及当前性能边界见[存储与性能说明](thermal-storage-optimization.md)。

2026-10-27控制层改造使构建的128材料、8component等工作段成为内部检查点。同一次`CandidateBuild.run`在传入的截止时间内连续推进；`RegionWork.runSlice(deadline)`、拓扑准备、Claim、取消恢复和安装清理共用本次执行额度。`ThermalAssemblyTask.run()`从池中真正开始执行时取得约2ms，内联入口`run(deadline)`使用协调轮剩余时间。路由发现、实际输入/helper/容量等待及粗转细的投影暂停仍让出，`ACCOUNTING`仍由协调者汇总准入后才能进入安装。2ms是协作让出的目标，不能限制GC、抢占或一次不可分割操作的最坏墙钟时间。

局部安装在调用发布器之前形成`RegionMembership`只读交接值，同一次协调调用立即使用；`regionsById`继续决定成员身份。`RegionalWorkQueue.replace(removed, added)`只取消被替换任务、按原优先级加入新任务，无关ready/waiting任务保留原位置。旧的`install(List)`、工作队列installed成员副本和全维度`advancementOrder`排序退出。必要全体区域/heartbeat只读快照和恢复遍历仍存在，不把局部安装宣称为严格O(局部变化)。

上述[重复状态清理](../../diary/2026-10-29_23-38-00_thermal-development-history.md)生产净减35行，没有数值近似。首轮21项中20项通过，半空气邻区夹具因未清空上半部、实际探针为砂砾而失败；核对真实存档后补全夹具，原阈值/时限保持，仅重跑输入组8项全部通过。共21个不同检查项取得通过证据，两个运行均保存错误0、root0；不能称为21项同一轮全过。[验证摘要](validation/thermal-material-state-cleanup-2026-10-26.json)记录失败场景与各轮归属。

结构提案和ReadCursor借用旧Page与帧。Page持有计数在发布锁内更新，最后一个目录/读者持有者释放后才归还其费用；取消提案也沿同一路径释放。空气查询沿用短发布锁，slot查询在锁内查找不可变帧后读取；数值求解与图构建不在发布锁内执行。完整目录准备与samePage事后比较已经移除，具体实现与待验收路径见[当前计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

源进度仍按`generation + tick + envelopeSequence + windowOrdinal`区分。接管完成与物理消费完成分开；Store路由仅移动已有J，不重新产热。迟到的到期输入不会被未来队首挡住。`ControlTail`在普通槽满时公布首个未运输控制时间，结构只等相关源在边界T前的前缀。

`SourceInputWindow`只按本窗口的实际Span数量分配数组；仅为空间路由端口分配`remainingJ/exportedJ`。声明辐射损失在接管时结算，不分配同样的待路由数组。所有实际接管数组和目的记录在改producer累计量前取得存储。

`AirLoadTable`的世界绑定按六个实际方向保留六个槽。没有正式调用者的`bindUnresolvedFace`及其第二组槽、布尔状态和转接构造已经删除；`SpatialHeatInput.takeWorldIdentity`只导出真实方向。`SourceInputWindow`和v6恢复使用的原出口方向编码6–11仍有用途，继续保留。瞬时signed J的`emitSourceImpulse/SpatialSourceTimeline.impulse/ThermalSourceBatch`属于明确保留的输入合同，不能与该废弃绑定分支混为一谈。

`RegionalAirRuntime.receiveSpatialHeat/routeUnowned`在一次调用内收集受影响的旧、新区域，完成目标归属更新后各刷新一次路由状态；取消重叠任务仍即时执行。路由成功接收的J在刷新后才交给`RegionalWorkQueue.offer`，因为该方法可以立即冻结输入。集合只在本次调用内存在，`finally`在返回前刷新已修改的区域；没有跨轮待刷新队列。`SpatialHeatStore.pendingThrough`复用只含tick和布尔值的`DueScan`，通过`ArrayDeque.forEach`检查记录；队首未到期仍检查后续记录，不改变迟到、瞬时输入和取消归还的语义。

`TopologyUpdatePlanner.continuationBoundary`仍保守地沿相邻已捕获Section求影响闭包；遍历时直接查询既有Page/halo、候选draft、输入seed和当前/已提交route索引，不再为每次边界查询复制全维度captured集合。`RegionalContinuationBoundary.including`在追加范围已包含时复用原不可变边界。天空遮挡更新可由多批输入以任意列号顺序到达；`PageDraft.setSkyColumn`保持编号和值成对、有序、每列最新值唯一，再交给`PreparedTopologyChange.PageWrite`，避免合并次序触发提交异常。

区域结构任务通过`TopologyUpdatePlanner.forInput(emitters, boundary.sections())`从准备开始约束几何范围。`TopologyView`的Page、slot和signature读取与`FrozenAirGeometry`采用同一范围；范围外属于未捕获几何，由既有支持请求继续处理，不能先把外部出口写进图、再在冻结时丢掉其数值几何。

正式调用`TopologyUpdatePlanner.prepareRound`的对象全部来自forInput；通风通路准备完成后才进入片段编译，不再保存旧completeRoutes处理开关。根planner继续提供已提交通路索引和私有准备的起点，区域任务各自持有实际准备状态。

该批[页面与通路状态复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)只删除固定状态和未使用处理分支；现有区块保存/卸载/重载后扩展温场、区域粗细切换2项同轮首过，保存错误0、关闭root0，测试源码未改。

`receiveRegionalInput`在既有2ms协调时间片的前半段批量移入已到达的拓扑输入，集中刷新边界与受影响chunk。源通道随后推进，源控制确实再次改变边界时才补充刷新。临时集合只存活于本次接管，没有新增跨轮输入队列；区域完成仍各自提交和继续，不等待整批任务完成。

## 2026-10-28：任务费用统一活动时间

正式开工和区域质量成本改由`ThermalWorkBudget.Account.activeNanos`提供，单位ns，累计各线程实际执行片段的墙钟耗时。不同线程并行相加；同线程嵌套上下文暂停父计费，helper只在自己执行时累计。交还线程后排队、输入/容量等待不计，片段中的系统抢占/GC仍计入。`elapsedNanos`是包含等待的生命周期诊断，不能作为策略费用。正式费用链不再调用ThreadMXBean，不保存CPU/helper双列，不需要CPU可用性判断或量子截断。

旧费用源是ThreadMXBean线程CPU时间，新费用源是System.nanoTime的执行入口/出口差值；QueryThreadCycleTime仅用于默认关闭的GameTest诊断。固定工作量实验中活动时间同样在577.348至1400.778ms之间变化，所以这次替换没有证明活动时间更准确地表示纯计算量或物理CPU消耗。它能直接表达执行片段实际经过时间，代价是包含片段内的抢占、阻塞和GC；采用它的资格由所列服务验证限定，不能称为所有环境下更好的CPU时钟。

`ThermalWorkerPool`对现有RegionalTask执行范围扣活动时间：ThermalDimensionMailbox通过RegionalTask提交普通协调轮，内联helper包含在协调轮中只扣一次，排队helper单独计费。已经开工的并行片段完成后形成共享活动欠账；墙钟流逝按配置速率偿还至0，超过一个服务周期的allowance才暂缓新普通任务。短时突发量为配置中的一秒工作量，默认1000ms；空闲不继续积攒credit，原15.625ms CPU量子额度和尾账退出。控制入口、容量唤醒和关闭推进沿用既有协议；controlPending协调轮和生命周期/关闭可继续推进，所以它是共享热工作服务目标，不是整服或整池所有操作的绝对时间上限。

配置文件键`continuousAirCpuBudgetMsPerSecond`为用户兼容保留，Java字段为`FHConfig.COMMON.THERMAL_RUNTIME.continuousAirWorkBudgetMsPerSecond`；默认1000，范围1..4000，单位为共享区域任务活动ms/墙钟秒，跨维度/线程分配，重启生效，不代表精确CPU核数。区域按原真实工作权重分配Allowance，构建4倍普通Allowance、8cut成本窗口与恢复退避保持。费用源更保守，负载/抢占较高时可能更早降粗。

额度重新标定的依据：同源码250ms/s下双基地首次完整服务延至573tick，千源压力出现独立房间31tick空读；4000ms/s仅作需求诊断，双基地首次完整服务71tick，千源四窗空读0。按同线程执行区间取并集消除内联重复后，千源连续10秒窗口需求达400–542ms/s，恢复窗口982ms/s；双基地冷启动也超过250ms/s。用户选择优先保持服务能力，因此以一个共享线程活动秒/墙钟秒的1000作为新默认，独立验证，不增加自动倍率或平台特例。该并集未覆盖池包装和所有控制费用，且诊断同时改变质量阈值；它不建立精确最小资源需求或同额度提速，也不能证明1000适合任意负载。

最终r4统一构建74s通过；仅源码默认值由r3的250改为1000，计时/调度实现相同。r3的6项重点与结构隔离按250ms/s单独记录，隔离800样本/4928远端变化、空读/过龄0、最大年龄36tick。r4的1000ms/s双基地2400tick、12000消费者查询从56tick完整监测，年龄/几何/消费者违规0，观察全细档；热主线程P99 1.600ms、整服处理P99 25.549ms，首tick最大1743.399ms与保存最长52.518ms保留。千源四窗分别21/21/21/19次提交、空读0，最大年龄25/22/22/21tick；独立房间最大年龄28tick，三次保存、撤压恢复并稳定细档400tick，热错误0、关闭root0。千源整轮含撤压后目标调至128MiB，登记峰191036010B约182.19MiB，不能当成64MiB硬内存保证。正式JAR430个thermal类，测试/原生诊断不入包。

详细配置、不同编译归属、失败和计量限制见[验收记录](validation/thermal-active-work-budget-2026-10-28.json)与[实施计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。生产11文件净减57行，没有新增生产顶层类型。原始平台CPU、分配和native周期观测属于GameTest诊断，不能混作正式策略费用；本批没有重验32基地、100真实联网玩家或新的0.1°C近似轨迹。

## 2026-10-28：材料接触由Brick片段直接拥有

当前材料关系只保存在`ThermalFragment.MaterialContacts`，与空气边、通风和远场边界一起由片段的InstallToken交接。全局`MaterialEdgeCompiler/MaterialEdgeTable/ThermalMaterialEdge`及重复`ThermalMaterialExecution`已经退出生产源码；下文按日期保留的旧压缩结果不描述当前材料边结构。本批一次统一构建和4组11项重点验收首次通过，热错误0、关闭root0，范围与限制见[验收摘要](validation/thermal-local-material-topology-2026-10-28.json)。

`BrickTopologyCompiler.compileFragment`只枚举正向内部/邻砖接触面，一对物理端点只有一个生成片段；同一Brick内多个材料—Air面由原局部累加器合并。通风通路继续使用RoutedContacts。区域划分直接读取片段接触；细档LocalMaterialBoundary和直接粗档Minimum按已排序片段及原局部顺序读取材料—材料导热。材料—Air实际数值交换仍使用表面Contacts。没有改变导热公式、源J消费、粗档节点数或v6保存格式。

PreparedTopologyChange不再携带独立材料边/执行表列，RegionalTopologyInstall不再逐边预留/暂存/清理；ThermalSolver只维护片段、端点引用和环境列。区域Scope不再保存执行表副本。已提交但尚未清理的片段由其token决定可见性，准备工作量计数包含这部分已生效的替换。旧读者继续持有不可变片段，几何槽在实际引用退出后释放。

## 资源目标与完成空间

`FHConfig.COMMON.THERMAL_RUNTIME.thermalMemoryTargetMiB`为服务器共享的内存准入目标，单位MiB，默认128，配置范围16至16384，重启生效。它不是JVM堆上限，也不是任何时刻绝对不能超过的字节线。

可选细档工作空间/图使用`tryReserve/tryResize`。空气、源输入、查询使用同一root下的统计类别，取消固定16MiB查询子限和重复128MiB子限。`ThermalDimensionLimits.sharedGrid`按Page目录推导表示边界：每Page至多4096个空气单元及4096个材料单元，3200Pages对应26214400个单元；arena为两代重叠取整后的索引容量，边/边界计数遵守Java数组索引范围。数组只按实际数据增长，不预分配这些上界。旧的65536活跃单元、131072槽、262144pair和65536boundary服务门槛及其旧后端已经退役；旧整维度休眠阈值也已删除。

已有目录/运输边界仍保留：每维度3200Pages、65536物理源、131072源节点，最多128个原版Checkpoint请求；`SpatialSourceTimeline`另允许最多两代源条目重叠。它们约束对象数量和在途工作，不是本轮证明可全部容纳于默认内存或维持40tick的服务承诺；其中源数量65536与已删除的活跃单元65536不是同一个限制。本轮没有为这些目录上限选定新的数值。

真实源登记及有界生产记录、已接收的世界控制/结构更新、有限窗口交接、冻结几何和读者、已开工操作的实际数组替换、最低图转换、已完成结果发布、现存帧读取和当前Chunk保存使用`reserveCompletion/completeResize`或`Reservation.beforeAllocation`登记必要占用。**可以超过准入目标，全部仍计入used/peak；超出时不再启动新的可选细档工作；已经接收的世界更新仍在有限结构开工数内完成，并按需要转换粗档。** 所有权受实际窗口/记录数量、工作池和已开工任务、每维度一个Chunk封存及真实读者引用约束，实际内存仍随活跃世界和源数量增长；这不是一个固定总内存保障。任务之间不再互相借还额度。压力验收必须报告实际超出量。

每个源登记不再向Store预付两份72条最大窗口。对当前两端口篝火，登记从50352B降为26896B；1024源为26.265625MiB，减少22.90625MiB登记余量。实际接管窗口和后续目标流另按持有量计费，因此这不是整个场景的堆内存节省承诺。producer自身有界运输及尾部的最大记录保障仍保留。

| 两端口篝火登记组成 | 改前B/源 | 当前B/源 |
|---|---:|---:|
| producer Entry、运输与尾部记录保障 | 22224 | 22224 |
| 两份最大Store窗口预付 | 23456 | 0 |
| ingress身份、索引及控制范围 | 4480 | 4480 |
| register端口附加量 | 192 | 192 |
| 合计（不含后续实际接管和目标流） | 50352 | 26896 |

当前公式锚点为`SpatialSourceTimeline.ENTRY_BYTES/register`、`SpatialSourceIngress.metadataBytes`和`SourceInputWindow.storageBytes`。令`A(n,w)=align8(16+n×w)`为数组登记B，`n`为本窗口实际Span数、`p`为端口数、`r`为空间路由端口数，则已接管窗口登记为`384+A(n,8)+80n+2A(nr,8)+A(p,8)+160p` B。两端口篝火只有一个空间路由端口：`n=1`时888B，`n=72`时8272B；这部分按实际接管另计，取消的是每源预先持有两份最坏窗口的要求。每个源的代次重叠、全局索引和解析后目标流另计。

正式路径保留已有任务预留、数组扩容登记和容量判断。2026-10-24 的当前源码对大块稳定数据使用容量摘要，不再要求共享数组在不同摘要之间精确去重；`thermalMemoryTargetMiB` 是估算的软准入目标，不能当作精确 JVM 内存。共享数据的保守重复估算可能提前触发资源降粗，CPU 成本判定和恢复策略没有改动。本批正常四窗48000查询全部细档，低额度千源完成降粗、保存和撤压恢复；精确条件见[统计优化结果](thermal-storage-optimization.md)。

`ThermalDimensionEngine.backingMeter`由维度协调者独占，以`AirBackingMeter.summaries()`模式汇总容量。结构PUBLICATION准备完成后仍经过ACCOUNTING，再进入INSTALLATION；任务关闭、重置和最低图释放仍触发回收估算。单个就绪候选不等待其他候选。这里保留现有生命周期，没有新增存储块持有或额度转交协议。每次汇总在`finally`清空引用与累计字节，工作空间继续按`128 + AirBackingMeter.shallowArrayBytes(key)` B登记，并在关闭时释放。固定列和少量目录仍会访问；不能把本次优化称为完全没有统计成本。

`AirFieldLayout`缓存不可变布局的容量；`CoupledThermalOperator`只缓存构建后容量不变的部分。首次计算局部摘要仍访问本对象的数组，后续读取一个long；projection和冻结输入数组不纳入固定摘要。`PageSignatures`创建时计算容量，`AirRouteCompiler.Component`复用已有随准备更新的`backingBytes`。缓存不保存旧对象引用，不改变数据释放时间。`ThermalSolver`在片段替换/删除入口维护载荷总数；片段列和引用列的实际数组容量继续计入。

`FH_THERMAL_DEEP_MEMORY_PROFILE=true`才执行关闭时的完整维度盘点、冻结几何JFR详细盘点和算子工作空间复用字节诊断，并启用原fastutil反射容量诊断；普通Profile/JFR不自动执行这些工作，未采集的JFR字节值为-1。诊断报告不回写预算或修正总数。Debug沿用原反射容器口径，首次局部摘要中的容器估算可能与默认口径不同；本批Debug旧读者保留1323376B、默认1322536B，均在最后释放后归零。原`AirBackingMeter`数组/容器去重实现保留，服务诊断、首次局部容量计算和少量固定列计量；不能宣称整个系统已经完全改成分配事件计数。

查询载荷由现有帧数组family、索引实际容量及不可变Page持有计数累计，普通确认不再遍历所有区域规模。下述材料边对象/执行列压缩为2026-10-23至27历史实现，已由上述片段直接消费替代：`ThermalMaterialEdge`把首项贡献的fragment/operation/rank/原始conductance存于边对象；有额外贡献时，由唯一编译入口转入一个独占long数组，每项三列为打包fragment/operation、完整rank、double原始位。`recordBacking`按边对象一次去重后计入`backingBytes()`。`ThermalMaterialExecution`的两端slot直接从既有64位边键解析，不再额外持有两份int数组；执行顺序、总conductance与系数不变。其他共享几何继续使用原盘点方式，不能据此宣称全系统已经改为增量记账。

`SharedAirGridCompiler`直接生成short型局部角索引和局部位置。一个4×4×4砖最多512个角身份，空项为-1，位置编码`x | z<<3 | y<<6`最大292；这两类整数均无损表示，不合并同坐标但不连通的顶点。全局系数地址保持int，温度和能量保持double。`AirFieldLayout`的合并/复用共享不可变局部数组；`unitCornersCopy()`只在v6捕获边界展开为原int数组，NBT格式不变。本批性能实测状态见[无损存储计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)，不得将此前编译批次视为本批已经通过。

`WorkspaceLease/borrowedWorkspaces/borrowPreparationSpace`、`resourcePriority/reclaimForMemory`和保存跨Chunk清空准备的流程已删除。`QueryPublication.FrameLease/ParentFrames`保留：它们持有会被读取的数组，防止helper或读者退出前释放数据，与额度借款无关。

## 开工、等待与质量选择

`RegionalWorkQueue`继续公平排队、协作切片，并在同维度独立区域间利用共享多核池。普通结构准备的同时开工数按工作池线程数限制；已释放暂存的容量等待者不占开工位置，不能阻挡其他区域的退休/清理。资源充足时多个独立结构可以同时准备，没有“每维度永久只有一个结构任务”的限制。

`ThermalAssemblyTask`本身实现`ThermalWorkerPool.RegionalTask`，所有区域求解和私有构建切片按共享池活动时间政策计量；旧空Regional子类与工厂已删除。AtomicBoolean领取、完成future、工作上下文和取消后释放顺序保持。`RegionalWorkQueue.waitingMemory`只持有实际加入该队列的RegionJob，`capacityObserved`归Job所有；构建任务的资源处理仍由结构协调者执行，不再保留该队列中的构建任务分支。

该批[任务包装与等待归属复查](../../diary/2026-10-29_23-38-00_thermal-development-history.md)确认唯一任务创建入口及两个容量队列加入点；统一构建后源取消重放、粗细切换2项同轮首过，保存错误0、关闭root0。没有新增等待机制或数值政策；本批没有单独制造容量拒绝或重新验收百人负载。

结构任务阶段为`MATERIAL_INPUT → MATERIAL_SETTLE → TOPOLOGY → REGIONS → PUBLICATION → ACCOUNTING → INSTALLATION → COMMIT → CLEANUP → DONE`。既有2ms结构时间片内遍历已就绪任务，公共盘点在本轮统一进行；没有新增整批完成屏障。材料事件只在第一阶段应用，随后等待已有材料控制完成；依赖或资源重试从TOPOLOGY重新准备。容量等待在本轮释放scratch/全局占用之后登记，避免自己的释放反复唤醒同一失败请求。COMMIT成功后只执行CLEANUP，粗转细恢复也不能因后续容量等待重新标成放弃。进度日志`geometryPhase`使用这些阶段名。

| 状态/原因 | 保留的数据与责任 | 下一事件 |
|---|---|---|
| 细档资源不足或真实拓扑规模超限 | 同一任务责任、有效几何输入与Store中的J；可选细档转粗，超过真实规模边界的准备可归还暂存 | 粗档完成或真实资源/结构变化；不通过主线程重捕获重放 |
| 细档成本或资源过载 | 当前Accepted、未消费J和同一任务控制 | `RegionalQualityControl`请求最低模型，helper退出后准备并发布 |
| 相关世界发生变化 | 真实Page版本和新控制决定候选是否过时 | 原mutation/捕获入口；资源失败不制造新revision |
| helper正在取消 | 父数据、Claim及工作空间继续由任务持有 | helper真实退出，沿普通清理出口归还 |
| 发布提案过时 | 私有候选和已有Accepted | 按当前目录重新准备提案，不等待无关内存事件 |
| 保存本区冻结 | 封存Accepted、目录和全部剩余J | 当前save完成/取消通知；不要求物理追平保存tick |

资源失败不再产生SHARED_GRID的`TopologyRetry`，不把输入退给主线程重捕获。WORK_LIMITED通知只反映资源/规模拒绝，不携带重新接管同一份热量的指令。真正缺失的Page捕获和世界mutation仍走实际输入；旧函数数据的读取与恢复仅保留在持久化兼容边界。

`RegionalQualityControl`是唯一模式控制者。不同容量出口通过`resourcePressure`请求；优先选择相关边界内一个已有细档区域转换；若本区没有可缩减状态，则从共享root下本维度已有细档中选择一个，不遍历取消其他区域的准备。小房间在内存压力下降粗档允许发生。活动时间触发降档仍要求规模有实际缩减收益，平台CPU量化修正已退出费用链。

最低模型保留64空气节点、64材料池、最多32显式相变体和8个时间段。粗细共用输入领取、取消、发布和释放；热容量、能量、背景、输入、查询使用相同空间权重。细档只准备最低摘要；细档区域不长期预留粗档工作空间，实际转换/粗档首步才取得有明确归属的空间。粗档步工作空间按已提交图的真实pairCount计量，取消空气区域一律256KiB以及候选间workspace donor交接。首次降档在同一`RegionJob.Kind.MINIMUM_LAYOUT`中按需构建最低传热图，不领取J、不改变物理时间。粗转细用同一质量候选和父帧借用，准备时粗档继续推进，实际交接对齐父提交时间。

已经选择粗档的结构候选从父快照直接进行最低模型投影，跳过父细布局拼接、细系数数组复制和细工作空间借用；`minimumConstruction`也直接使用粗布局/最低传热图，跳过细格编译、细pairwise operator与细档节点投影。真实几何、材料身份和相变记录仍用于路由、结构依赖和v6保存，成本不能按64个数值节点视为常数。

时间汇总的两处直接降档入口已从`RegionalAirRuntime`移除，但这不等于全链路解耦：`RegionalWorkQueue.offer`仍在自动模式下对`RegionPending.Input.aggregated`请求`aggregated_input`降粗，`RegionalQualityControl.current/recoveryEligible`也会因待处理汇总窗口阻止细档恢复。2026-10-23晚间存储优化调查同时核对了当前源码与18:17的已验收JAR，确认这些条件原本就存在；此前本段“时间汇总不会单独强制降档”的表述过满。Store继续保留目标/时间范围/符号/总J及Claim取消、提交规则。本批存储压缩没有改动上述质量规则；两轮分散复测中的实际降档记录见[存储计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## 服务政策与代价

`THERMAL_REGION_TRANSFER`只报告实际父时间、对齐情况和已消费序号；旧`ThermalRegion.receivedSequence`只有父间继承、没有输入更新，始终为0，已连同复制计算及日志列删除。`RegionPending.receivedSequence`仍由真实接收/交接更新，用于输入去重，数值推进和热量所有权不受该诊断清理影响。

`WorkerPageStore.advanceMaintenance`沿用1°C进入、0.5°C退出的活动滞回。`RegionalQuerySnapshot.activeAtSlot`合并空气/材料分类和阈值判断；已确认热砖后跳过额外数值判定，仍为每个slot持有快照并通知相变。维护批次的FrameLease、几何失效重来、完成后才替换frontier输出的规则保持。面采样直接读取已知Air单元的顶点系数，细网格不按坐标合并互不连通的顶点；粗档沿用扰动插值和原坐标去重，天空直接开口仍在采样前排除。

`ThermalDiagnostics.TRACE`集中读取`FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS`，默认false。详细逐slot极值、局部构建文本与交接记录由它开启；局部构建只记录本次prepared区域。普通`FH_CONTINUOUS_AIR_PROFILE`保留廉价计数，不再自动打开逐slot扫描和整维度构建日志。实际模式变化、故障与已有JFR事件仍可观察。显式TRACE输出可阻塞控制台，属于诊断成本；本批新旧生产性能对照均关闭它，实际收益及局部退步见[维护与存储优化](thermal-storage-optimization.md)。

- 共享工作池使用`max(1, min(4, processors-1))`个线程。`continuousAirCpuBudgetMsPerSecond`为**整个池跨线程、跨维度累计活动时间**的软额度，默认1000ms/墙钟秒，范围1至4000；一秒额度允许短时突发。扩大额度允许更多工作开工；实际服务延迟和执行周期必须另行观察，不能用不可靠的平台CPU读数推导精确占核。
- `RegionWorkPolicy`保留8窗口费用历史及恢复防抖；600至9600tick的恢复等待在20TPS下为30秒至8分钟，本批没有顺带缩短。
- 正常细档保留40tick读取年龄门槛。粗档仍允许读取最后几何有效的Accepted，不承诺40tick；实际年龄、提交活性、可读比例和最长空读必须分别记录，不能用旧温度可读冒充持续求解。
- `WorkerPageStore.advanceMaintenance()`沿用1°C进入、0.5°C退出的热活动门槛。全部热源熄灭后，低于退出门槛的区域可以保存余温并退出在线计算，玩法读取由保存场按休眠规则冷却；退休后不继续发布在线帧。粗档压低局部温度峰值，可能比细档更早进入这条退休路径。
- 主线程的有界捕获继续限制每tick工作量，冷启动/恢复会产生相应排队延迟。
- 保存不再因临时额度拥挤而额外近似温场或输入分布；已经粗化或聚合的已提交/输入状态照常保存。v6格式保持，精度保障的代价是保存空间或耗时可能更高。

## 保存、发布与关闭

每维度一次处理一个Checkpoint控制请求，`RegionalCheckpoint.Purpose`在创建`ChunkCapture`时固定为SAVE或UNLOAD。SAVE借用同一协调执行点的Accepted不可变温场，并收集Store未提交J及producer尾部，普通数值任务可继续；`SpatialHeatStore.collectStream`包含已领取但未accepted的前缀，排除accepted后尚待清理的前缀。UNLOAD继续暂停本区数值、取消并等待真实helper退出，再封存和导出。真实材料控制必须先发布后seal，无论保存目的为何均遵守原结算要求。

相关结构、材料变更及源运输仍受同一个`SnapshotHold`持有和封存约束；该持有者统一记录Chunk、相关区域、目的及sealed状态，替换原来的三份集合。SAVE不增加数值暂停计数；UNLOAD增加并在取消/完成时配对归还，关闭的暂停语义保持。保存空间按该Chunk实际温场与待存记录规模取得，不等待普通root额度、不借区域最低工作空间，也不要求被冻结区域先完成新结构。SAVE不导出输入；UNLOAD只在成功写入后的finish导出，native失效或取消沿原恢复路径。

保存状态为`REQUESTED → QUIESCING → CAPTURING → SEALED`。`QUIESCING`按目的等待卸载任务或真实材料控制，`SEALED`只等主线程finish/cancel，不反复安排无工作可做的协调轮。v6 writer只从`RegionalCheckpoint.Section/Part`的固定索引读取温场、材料H/branch和实际sampleTick，并组合封存的未消费J及producer尾部；不在write时重读实时Page或实时查询目录。独立重启验证以`ChunkDataEvent.Save`中生产listener实际写出的NBT为基准，避免把另一次内存缓存当成磁盘最终状态。

其中SAVE的QUIESCING只等待实际材料控制所需的任务结算，普通正在计算的任务不构成等待条件。`RegionalQuerySnapshot.PageIndex`随Template一次建立Section键及空气/材料两组offset/entry数组，帧间共享。`RegionalCheckpoint.Part`只携带field/page，writer和checkpointBytes读取同一页索引，不在每次保存扫描整个区域并重新复制索引。新增数组由现有payloadFamilies保留到最后读者退出；writer期间新帧提交可能延长旧帧重叠，这是普通保存不中断计算的实际内存代价。producer捕获仍可能涉及全局源，保存并非完全O(Chunk)或完全异步。

发布前准备查询目录、所有权和清理容量。`RegionalTopologyInstall.commit()`在token提交、区域首次`initializeEnvironment()`之前安装Page自然温度；环境属于新状态，不能延到退休清理后安装。成功发布后只能继续提交清理，不能反向取消已经接受的结果。清理游标在操作成功后前进，材料边容量按多个暂存任务的合计插入峰值准备。旧查询帧在最后读者退出后释放其占用并唤醒准备；关闭后仍有读者的数组由读者租约继续持有。

`MinecraftThermalInput.sampleAir()`与`QueryPublication.readAir()`使用已有发布monitor，取得同一次完整交接的状态；查询可能等待短发布，不等待结构构建。连续场首先查询`RegionalQueryIndex`及自身几何有效性，Page槽仅用于scalar和route解析。路由查询保留实际sampleTick、basis和质量。`ThermalEnvironmentSample.airMinimumQuality()`与温度和tick来自同一个Accepted帧，clear/copy遵守同一规则。通风、材料物性、物质类型、真实替换/质量变化与全量重捕获仍使相关旧帧失效，captured回退期限保持40tick。

完整捕获版本`ThermalPageHandle.liveGeometryRevision`仍覆盖状态资料更新。`lastPhysicalGeometryRevision`只记录最近一次实际物理变化在该完整版本序列中的位置，不单独生成版本；每个Page增加一个long，捕获数组与v6不增加版本列。`samePhysicalProperties`比较已有ventilation、materialProfileId、fullContactFaces、airMatter与materialTypeId，未知签名不视为等价；`materialStateId`变化仍捕获并进入保存日志。

连续空气和通路查询使用`hasCurrentAirGeometry/currentAirPublication`：最后物理变化版本≤场所捕获版本≤可见Page发布版本≤实时完整版本，且Page未退休、无待满足的full resync。写入在递增完整版本前先公布物理失效标记，读者复核该标记。这样物性不变的状态切换仍可读取原Accepted温场，sampleTick不被重写；完整捕获、构建依赖检查、材料读取、Page驻留和保存继续使用原版本规则。`AirRouteValidity.commit/activeForSolve`保持捕获与数值任务的原语义。

`SectionOwner`汇总本批是否包含物理变化，在主线程处理deferred和halo失效时传递；同位置已经有资料变化待办，后来的物理变化仍升级失效。Halo继续按现有位置过滤和Page粒度保守处理，不新增逐块变化历史。该改动保证查询含义一致；现有结构构建成本仍需后续单独优化。

2026-10-23早期复核曾发现完整BlockState改变导致空气过度失效；该问题已由上文的物理修订标记与`samePhysicalProperties`处理，并有实际状态变化、保存及拆除回归。[早期草案](../../diary/2026-10-29_23-38-00_thermal-development-history.md)与[方案复盘](../../diary/2026-10-29_23-38-00_thermal-development-history.md)保留当时的取舍，不能再作为当前实现仍未处理该问题的描述。

关闭先结算真实helper和提交后清理，再释放源运输、Store、候选、区域工作空间和查询。`thermalBudgetUsedBytes()==0`仍是没有外部读者的完整退出条件。

此前清理删除了均温兜底、deadline配置、备用分块因子、plume注热、材料表面复用和matrix-free表面试验。共享细网格、局部材料边编译、支持搜索缓存、不可变布局复用、primitive循环及完成后释放scratch继续使用。旧函数布局的数学求值与存档投影仍有数据兼容用途，正式FUNCTIONS运行后端已退役。

## 验证状态

2026-10-25的[退役与检查摘要](validation/thermal-retirement-2026-10-25.json)区分编译批次：r4已完成本轮26组真实Forge回归，包含多核/实际单worker、源取消重放、粗细切换、64MiB压力、独立JVM重载、12000tick双基地和分散基地；旧方块夹具未通过，保留失败记录。随后继续删除无引用方法、固定关闭的隐式传热/材料响应/图增量试验及其数组，形成r5。r5统一编译、正式包检查和修订后的3项方块回归通过。末次又删除仅供旧片段试验写入的组件列表、写入调用及无调用的int工作空间重载，形成r6，仅重编和核对正式包，没有重复运行场景。按用户要求，没有再次运行整套矩阵，不能将不同编译的结果写成最终源码逐项复测。

前一轮thermal正式源码146→141文件、54962→49443物理行，净减5519行。另删除未使用的`TopologyCommitter`、旧恢复转接、空提交、重复同步求解入口与仅写不读的统计；该批曾由PCG执行构建投影，当前已使用上文的直接节点计算。旧函数布局数学求值及v6数据兼容保留；`climate/player/unused`四个旧粒子文件完整保留并排除在上述删除范围外。

随后内部子系统清理使生产源码141→136文件、49443→46675物理行，净减2768行，没有新增生产类型。材料边与远场不再准备旧标量推进专用的一秒系数；局部函数固定、基底分块因子、空缓存合并与重复编译入口一起退出。本批仅验证涉及的源输入、存储、取消重放、粗细切换和独立JVM保存重载，不重复此前的大矩阵或性能负载。精确结果与夹具修正保留于[本批摘要](validation/thermal-subsystems-cleanup-2026-10-25.json)。

本批六组17项均通过并退出归零：输入8、存储5、源取消重放1、粗细切换1、独立JVM保存/重载各1。第一次迁移的饱和夹具未通过：周围温度0°C与实际相变温度9°C不同，存在额外传热，不能要求材料仅接收10J。夹具按实际9°C初始化后原1e-8容差通过；最终200J结算为接收10、拒收90、声明损失60、几何不可用损失40，等式闭合。修改夹具后仅重新编译测试并补跑输入，生产代码没有再修改。

后续[运行表示清理](validation/thermal-residual-cleanup-2026-10-25.json)使生产源码136→134文件、46675→45411物理行，净减1264行，新增生产类型0。统一主编译首次通过；正式JAR中两个退役类型和测试类均为0，旧粒子四类保留。该批六组21项最终通过并关闭归零：输入8、存储5、粗细切换1、方块3、读者/材料生命周期3、真实保存重载后开门1。三名旧读者跨发布端关闭后保持数据，最后释放使root从1321864B归零；两轮粗细转换/两次checkpoint保持最大恢复跳变0.0043284°C及能量误差2.98023224e-8J。

该批恢复夹具有两次失败，不能写成全部首过：r1仍要求点采样基底>8，而共享网格每点固定8角；r2已完成重载/开门，却被本轮错误新增的“历史温场必须高于当前自然温度”条件挡住。只改夹具，三次GameTest专用重编均21s，生产compileJava保持UP-TO-DATE，其他五组没有重跑。r3完整通过，堵口后历史温场-12.1112442°C、当前自然温度-11.9°C，sampleTick1140/tick1143，非零有符号历史、当前几何和新鲜度同时成立。原0.25°C重载误差、0.01°C梯度和0.005°C开门升温要求保持。另两项同类旧夹具只编译；本批不新增独立JVM保存重载、性能对照或百人资格。

最后的[尾项清理验证](validation/thermal-tail-cleanup-2026-10-25.json)保持134个生产文件，45411→45153物理行，净减258行，新增生产类型0。统一主编译首次通过，1m5s；正式JAR14962833B/439个thermal类，全部GameTest编译类与正式包交集为0，保护粒子四类保留。存储6、粗细转换1、真实保存重载开门1共三组8项均首次通过并关闭归零，没有修改后补跑。新增旧函数NBT检查保留负振幅得到的-5/-25°C、两个Section切片和完整64位branch及来源信息，重新编码后温场不变。两轮转换/两次checkpoint最大恢复跳变0.0043362751°C、checkpoint误差0.0967455986°C、能量误差2.98023224e-8J；堵口后的有符号历史和新鲜几何仍成立。前批21项保留原编译归属，没有用它们宣称末批逐项复测，也没有新增性能或百人资格。

[直接投影验收](validation/thermal-direct-projection-2026-10-25.json)于2026-10-25实施、26日完成，生产134→133文件、45153→44836行，净减317行；顶层PcgSolver及嵌套迭代器退出，新增顶层生产类型0。统一主编译57s首过，正式JAR14954112B/435个thermal类，PCG类/测试类0，粒子四类保留。数学投影2、真实源取消重放1、粗细转换1、真实保存重载开门1共四组5项均首过并关闭归零，无编译后修改或重跑。

数学检查覆盖有符号节点能量、原初值容差内差异也直接求值、空系统、1024行块让出、材料尾部保持、无效质量的原候选失败出口，以及实际网格装配后仿射场和8192J/K容量/81920J总能量。真实源6400J全部交付、两次取消/一次发布，重放能量和温差均0。粗细两轮/两checkpoint最大恢复跳变0.0036435497°C、checkpoint误差0.0967455986°C、能量误差2.98023224e-8J。保存恢复的历史/新鲜几何检查通过。

直接投影删除的四份迭代向量和两份全局对角数组，按源码长度至少减少40B/空气节点的原驻留double暂存、48B/空气节点的构建峰值double载荷；细档暖启动还少一份4B/节点的contributions临时数组，其生命周期不同，不简单与上述峰值相加。数组头、扩容余量和片段矩阵未计入这些下界。这是代码存储量，不是JVM实测内存；本批没有整场CPU或分配对照。平稳推进本来就不调用PCG，收益主要落在结构重建和恢复细档，不能据此宣称普通每tick同幅度提速或新增百人资格。

同一编译于2026-10-26完成[当前双基地性能测量](validation/thermal-performance-current-2026-10-26.json)：默认128MiB/250ms每秒，两个各128源基地与100温度消费者，2400tick/120.463s；CPU3.718s、热主线程P99 1.709ms、热线程累计分配3.583GB、热登记峰94.97MiB，监测期服务违规/容量拒绝0，观察全细档并关闭root0。该场景含实际关火/重燃，不是全程256源点燃；JFR采样开启，只有当前单次结果，不能计算版本提速率。首tick1.754s与温度保存最长46.151ms等待仍是已观察代价。热点和下一阶段0.1°C正常误差约束见[存储与性能说明](thermal-storage-optimization.md)，新近似尚未实施。

随后用户要求继续清理并反复复查后才考虑优化。[材料残留批次](validation/thermal-material-cleanup-2026-10-26.json)在源码调用、构造不变量、任务交接和当前编译产物四轮核对后，删除Coupled旧材料列/等待备用路径、恒为0的快照等待偏移、不可达的粗档容量构造和无调用包装。生产133文件/44836→44746行，净减90行，新增类型0；数值公式、步长、精度、更新频率和粗档策略未改。统一主编译56s首过；存储6、源重放1、读者/材料生命周期3、粗细转换1共四组11项均首过，关闭root0，无编译后修改或重跑。相变完成、两次取消/一次发布、三名读者最后释放1321864B→0、两轮粗细/两checkpoint均通过；最大恢复跳变0.0043498497°C、能量误差2.98023224e-8J。

该材料清理批次没有性能或近似实现，也没有重跑性能矩阵。前文3.718s等数字仍属于清理前的直接投影编译，不能作为材料清理后的精确性能结果。[性能计划](../../plans/2026-10-29_23-38-00_thermal-next-steps.md)当时暂缓，清理收尾后用户已授权上文的无损权重与构建可读性优化；其结果单独记录。自动零引用候选包含接口分派、容器回调和观察入口，不能直接解释为尚有同数量的垃圾，也不能用四轮范围内检查承诺全仓动态入口绝对无冗余。

随后[内部残留清理验收](validation/thermal-internal-cleanup-2026-10-26.json)保持133个生产文件，44746→44673行，净减73行；现有测试净增8行，总Java净减65行。四个无读字段和七个旧方法已从当前编译类中退出，新增生产类型0。初次主源码编译通过，测试编译因漏改一处MaterialEdgeCompiler构造调用失败；补齐后统一构建29s通过，主源码保持UP-TO-DATE。一次服务器运行22项全部通过：输入8、材料7、存储6、取消重放1，耗时2m53s，退出root0；6400J源输入两次取消/一次发布后的重放能量和温差均0。正式JAR14951336B/435个thermal类，测试类交集0、保护粒子四类保留。当前源码和产物复核未再确认本批范围内需要删除的整套架构；没有重跑性能、粗细转换或完整重启矩阵，也没有新增0.1°C近似资格。

以下性能和服务数字保留至2026-10-24的原批次归属。

最新局部容量摘要结果见[统计与存储优化记录](thermal-storage-optimization.md)：存储3项、材料/读者/相变3项、粗细切换1项、64MiB千源1项、独立JVM写读各1项、完整分散场1项以及Debug生命周期3项通过。当前5GiB JVM下六窗72000查询、7保存及实际卸载重载完成，71931细档读取、69存档回退，粗档/自然回退/非法值均0，能量/顺序/运行时错误0、root0。相对compact r4，热线程CPU减少27.7%、分配增加1.18%，统计路径采样42.6%→10.7%。构建与启动中断保留于过程记录；此前单worker等结果仍按其原编译归属，不冒充全部项目在本次重跑。

18:17批次的完整负载、资源代价、部署配置和资格边界集中在[服务器验证记录](thermal-regional-server-validation.md)。该批多核场景包含2/8/16/32基地、100温度消费者、保留建筑撤源、真实区块卸载/新实例重载，6窗口7保存完成，能量/顺序/工作器错误为0，停服占用归零；该批单处理器/单worker的3组5项也通过并归零。

512MiB热目标、1000ms/s整池CPU额度、6GiB堆下，正常四个负载窗口共48000次玩法查询全部使用在线细档；撤源有280/12000次存档回退，重载后12000次全在线细档。实际构建期间仍允许既有的40tick捕获回退。这里的在线和存档含义、严格几何探针以及完整服务器耗时应按验证记录分别阅读，不能把样本可读当成始终没有任何近似。

时间合并输入曾触发大量空间转换和恢复工作；RegionalAirRuntime的两处直接触发已删除，队列和恢复资格中的残留条件如上文所述，不再概括成全链路解耦。CPU/资源压力保护继续保留，128MiB和64MiB千源场的通过结论按当时批次保留。原逐项过程报告为`build/thermal-regional-control-20261023-validation-notes.md`，属于不入库的生成文件，当前工作区已不可用；本节摘要不能代替缺失的逐项日志证据。

历史失败、夹具修订和验收定义变化的摘要保留：原四连续旧结构交错条件未覆盖，最终改为一次真实独立提交、四个连续完整源窗口及原新鲜度/新房间时限；原分散卸载夹具受邻近强制票保留，修订后要求真实Unload事件和新实例；早期4GiB堆OOM与后续6GiB验证也不混为同一条件。此前[分散调查计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)仍在；原生成报告`build/thermal-simplification-verification-20261022.md`和`build/thermal-physical-validation-20261023.md`当前工作区已不可用。这些历史摘要不得作为当前代码的无条件资格。

本轮未运行JUnit，未用Python修改代码，也未声称覆盖100个真实网络连接、所有整合包系统或任意长时硬件负载。
