# 热模拟运行时：固定温场、输入与保存

- Status: `Current for the production fixed-field implementation; retirement validated by full 156/157 plus corrected source-focused 8/8, 157 distinct functional checks; hundred-player performance qualification incomplete`
- Last verified: `2026-11-19`（热混合覆盖、驻留通知、几何切点顺序与百人实测）。热源输入截止与存盘判定核验于 `2026-11-18`。自然背景与扩容压力核验于 `2026-11-15`，固定温场主体核验于 `2026-11-14`。
- Scope: 正式生产运行链路、输入所有权、物理时间、逐格自然背景、查询、热数据v7与关闭恢复。
- Code anchors: `MinecraftThermalInput.createWorker`、`ThermalDimensionEngine`、`TiledThermalField`、`NaturalProfile`、`NaturalProfiles`、`SpatialSourceIngress`、`SpatialHeatStore`、`SpatialHeatBuffer`、`QueryPublication`、`RegionalCheckpoint`、`AirFieldCheckpoint`、`DormantChunkThermalState`。

## 正式入口与数值状态

`MinecraftThermalInput.createWorker`只创建`TiledThermalField`和`QueryPublication.createFixed`。`ThermalDimensionEngine`只协调这一套固定温场；旧`RegionalAirRuntime`、`RegionJob`、`RegionBuild`、工作队列及粗细切换运行链路已经退役。没有生产后端切换选项。独立图构建/数值参照所需的六个类只在GameTest源集中，正式JAR不包含它们。

每个Tile的Accepted帧和`sampleTick`共同拥有实际值及其计算时刻。`acceptedTick()`只是现有采样时间的诊断最小值。`domains()`按已捕获邻接和已安装通路形成Section成员集，不拥有额外时钟、热量或任务。相互传热的空间一起推进；断开的基地可分别推进。实际起止区间完全相同的域可以在原已付费输入容量内共用事务。

源、输入和数值只有一条生产链路：`SpatialSourceTimeline` → `SpatialSourceIngress` → `SpatialHeatStore` → 一个范围内的Claim和`SpatialHeatBuffer` → 固定温场提交。Store拥有已接受输入，缓冲只保存可取消的准备副本及已提交的deferred J；取消不扣原输入，提交只确认一次归属。其他范围的余额不能随当前范围提交而清除。

## 输入、几何与物理时间

`transportThrough`在同批几何引用完成交接后才更新。数值截止同时考虑输入前缀、真实几何、源控制和原20tick切片。源热量可先记录到控制边界，源启停状态等实际接收方达到该边界后才生效。

营火熄灭时，`PhysicalSourceSpatialIndex.flush` 把启停变化和生效 tick 交给 `SpatialSourceTimeline.enabled`。它先按原功率结算到这个 tick，再关闭供热。`SpatialSourceTimeline.seal` 交出最后一段输入后，会把已关闭的持续功率源移出发送队列。已经进入空气和材料的热量继续由温场推进。

工作线程在 `ThermalDimensionEngine.fixedNumericalThrough` 先用已交接输入、当前区域的物理切点和 20 tick 上限确定本步终点。`SpatialSourceIngress.limitNumericalThrough` 只访问当前区域中已捕获且有源索引的 Section，同一热源只检查一次。仅当源的待处理时间**早于**当前终点，它才检查输入归属。恰好落在终点的输入留给该边界处理。若较早输入归当前温场，就缩短步长，最早停在本步起点。

`ThermalDimensionEngine.sourceFrozen` 只在当前快照已经冻结输入时，才通过 `WorkerPhysicalSourceBindings.airPortsTouchChunk` 检查实际出热口是否接触快照区块。出口可以跨区块，源方块的坐标不足以判断这笔热量归谁。已冻结输入按快照合同处理。出口归属沿用现有的快照与几何版本缓存。

`activeSince`是当前活动寿命的起点，不随求解推进。首次捕获或原生重入之前的控制在该起点初始化；它不代替`sampleTick`。旧Page身份不能约束或退休同坐标的新Page。

真实几何在受影响旧温场到达事件切点后安装。纯扩容留在原Page队列等待容量，已有空间继续计算；混合输入中的实际修改、收缩和退休优先作用于已付费空间。冻结写回期间不安装可选扩容。普通几何不能通过直接修改采样时间冒充补算完成。

`TopologyInbox.PageQueue.enqueue` 为每个 Page 保留最近两个输入。积压需要合并时，若先到的是本页混合系数、后到的是较晚的 Page 变更，旧输入最多占两项。先按原时刻处理本地系数，再按第一个 Page 变更的时刻处理可能影响邻区的几何。`foldThroughRetirement` 同样保留这个顺序。这样一来，已经越过本页系数时刻的邻区仍能在自己的几何变更时刻参与更新。

`WorkerPhysicalSourceBindings.bindingLimit` 读取热源所在 Section、定义的出热口和实际路由终点的时间上限。若出热口或路由终点就在已查的热源 Section，同一上限可直接复用。跨 Section 的终点仍参加取最小值。分组测试覆盖同区出口、跨区出口和积压中的两个时刻。百人服的时效、恢复与 CPU 见[本地混合切点验证](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-causal-cuts-100p-2026-11-19)。

### 邻格供热与驻留判定

`AirMixingRegion.cellMask` 用一个 64 位整数表示 4×4×4 个格子的混合覆盖范围。每个格子的中心在距任一出热口中心不超过 4 格时进入覆盖，同格的多个出口取并集。`TiledThermalField.replaceMixing` 每个常驻 Brick 存一个 `long`，换热时直接读取该格对应的位。已有 Brick 在扩缩容时复制掩码。几何安装用 `FixedGeometry.restoringBricks` 初始化新捕获的 Brick，已有 Brick 按源启停批次的 `mixing` 范围更新。12×12×12 种可能的出口偏移共享一张只读表，数组数据占 13,824 字节，由所有维度共用。

常驻维护过程中，`WorkerPageStore.requestFace` 仅对仍需决定的相邻 Brick 检查边界。`TiledThermalField.airFaceAtThreshold` 在当前已提交的温场中比较空气温度与本格自然背景的绝对差，命中首个合格面格就结束。新增驻留采用 `ThermalRetentionPolicy.ENTER_C`（1°C），已请求的驻留采用 `EXIT_C`（0.5°C）。朝上的面沿用各列的直通天空判定，材料边界请求由 `collectMaterialFrontier` 处理。

驻留维护沿用 `WorkerPageStore` 的 Section 队列。`TiledThermalField.commit` 只在已准备的 Tile 成为已提交值时通知该 Section，首次发布同样入队。一个基地单独推进时，其他基地保有自己的采样时钟。自然背景实际变化、已提交材料控制、恢复和激活分别通知对应的 Section。几何提交和相邻驻留需求变化仍由 `WorkerPageStore.markGeometryMaintenance` 和 `advanceMaintenance` 更新邻居。

`WorkerPageStore.advanceMaintenance` 先按 `residentBrickMask` 检查常驻 Brick 的温差与材料相变，再按热源和温差形成的活动掩码检查边界。两步均从最低置位起顺序遍历，单个面完成后可以按原时间预算让出工作线程。当摄氏温差跨过 1°C 或 0.5°C 阈值，既有的驻留请求继续生效。

几何准备的 `TopologyView.prepareHaloOverlay` 顺序比较同一拥有者的新旧邻接采样位置。共有坐标沿用引用数，新增和移除坐标分别登记引用增减。新值仍按各写入者的顺序处理，`readHaloSection` 记录所依赖的 Section 版本。

### 扩容与内存压力

自 `2026-11-15` 起，"纯扩容"（`ThermalDimensionEngine.fixedStorageOnly`）有两种：已安装Page的Brick增长（`fixedGrowthOnly`），以及准入一个温场尚未表示的Section——既没有Tile、也没有已安装Page（`fixedOpenAdmission`，热量越过墙外时的邻接Section就是这种）。在安装之前，这样的准入不作用于任何已付费空间，因此和Brick增长一样：

- 不产生物理切点（`refreshFixedBoundaries` 跳过它），边界上的已有Tile照常推进；
- 应用时刻浮动为 `max(targetTick, 边界最小sampleTick)`，在边界对齐时安装，新Tile从邻居当时的时钟开始；
- 仍在自己的Page队列里按顺序：等待期间该Section之后的修改留在它后面，安装后再按序应用，不会被当作过期输入丢掉；
- 只在自己的Section上留一个存储切点（`fixedStorageCuts`，取准入tick）。它进入 `geometryBoundary`，因此只限制源绑定和热量路由：该Section里的源在Section存在之前把J留在生产者里，不在输入仓库里堆积；它不限制任何Tile的数值推进。

扩容和物理修改从不同批（`FixedGeometry` 的 `storage` 标记）：物理批次因此从不为可选扩容等内存。挑选顺序是维护、物理修改、扩容（`nextFixedGeometryInput`）。内存满时（`TiledThermalField.reserveGeometryStorage` 的 `tryReserve` 被拒）：

1. 扩容批次留在几何任务位里等容量；容量变化后的重试只是一次预留，不重新规划。它不切住任何空间，并且给维护输入和任何就绪的物理修改让位（`fixedGeometryYields`），让位时把输入退回各自Page队列。
2. 被拒后进入存储压力（`fixedStorageWait` 记录被拒时的容量版本）。压力下新的扩容批次只含一个输入，并且只在容量版本变化之后才组成；第一个成功付费的扩容批次退出压力。被跳过的扩容让空闲的协调线程登记容量等待，内存释放后再试，不轮询。

安装扩容前还检查每个扩容输入的边界都停在发布时刻（`FixedGeometry.storageAligned`）；若等待期间边界已分开，就退回重组，而不是把新Tile装在邻居已经离开的时刻。进度诊断 `regionalProgressDescription` 带 `storagePressure`、`storageCuts` 和待处理输入的 `storage` 标记。

在这之前，邻接准入被当作物理事件：它在 `targetTick` 切住整个边界，内存满时连同同批的墙体修改一起等待。100人负载（`2026-11-15` r4/r6）中，一个覆盖748个Section的批次一直等内存，这些基地的温度停在同一tick直到测试结束。GameTest `ThermalResidencyTimeGameTests.anUnpaidNeighbourAdmissionLeavesTheAdjacentBaseOnItsClock` 和 `anEditInsideAWaitingAdmissionIsKeptBehindIt` 覆盖这一情形；旧实现在两者上都停在tick 40。`TiledThermalSaturationGameTests` 覆盖等待中Section里的源（其J留在生产者里、恢复后恰好一次）和两个Section的整批等待。六次100人运行（r4–r9，含master对照）的结果、保存世界里各基地停住的tick和r6的一条停顿转储见 [validation/thermal-load-100p-2026-11-15.json](validation/thermal-load-100p-2026-11-15.json)。

仍在等待内存的物理输入（例如带新Brick的同坐标生命周期替换）保持原来的切点和等待方式；它们需要的是物理事件的存储，不是可选扩容。

## 自然背景

自 `2026-11-15` 起，固定温场模拟相对逐格自然背景的扰动。每个Tile的 `natural[cell]`（float）是该格的背景温度：
有已发布的 `NaturalProfile` 时取 `NaturalProfile.at(pageBlock)`，否则取Tile的 `ambientC`。背景公式和主线程的
发布节奏见[世界气候与温度](world-climate-and-temperature.md#6-空气温度)。帧里仍是绝对焓，所以温度、能量守恒
和存档格式都不变；变的是以下各处的参照：

- 换热按与背景之差计算：空气–空气面对和通路（`exchangeRoute`）减去两侧背景，材料对把两侧背景差作为
  `ThermalExchangeKernel.exchangeMaterialsInto(..., backgroundOffsetC, ...)` 传入，FarField 和环境面以本格背景为浴温。
  两格都在各自背景上时没有热流，背景不同也一样。
- 新格、几何替换和质量变化按本格背景初始化（`admit`、`resizeTile`、`projectedEnthalpy`、`changeFrozenCell`、
  `projectSnapshotCell`）。`SectionSnapshot.naturalC` 带出每格背景；手工构造的快照为 NaN，此时退回 `ambientC`。
- 查询对“与背景之差”做角点重构和三线性插值，再加上查询点所在格的背景（`airMean`、`queryCorner`、
  `sampleAirCell`）；`Sample.naturalC` 返回该格背景。驻留判断 `activeBrick(section, brick, threshold)` 和
  `WorkerPageStore` 的材料扩张与面残差都以每格自己的背景为准。
- 背景变化：`NaturalProfiles` 是主线程到 worker 的旁路，带递增 revision。`begin` 先调用
  `refreshNaturalProfiles`，对 profile 身份变了的Tile执行 `shiftToNatural`：每个非冻结、非相变握手的格加上
  `(新背景 - 旧背景) × capacity`，计入 `acceptedExternalJ`，因此背景变化同时到达每一格，不留下假扰动。
  冻结Tile只更新背景，重入时 `activate` 朝新背景做休眠冷却。没有 profile 的Tile在 `ambient`/`stageThaw`
  改变 `ambientC` 时同样整体平移。

休眠存档仍按 Section 保存绝对温度和一个 Section 自然值。为与逐格背景一致，以下几处改用逐格自然温度：

- 写入区块热数据时的保留过滤（`captureChunkSnapshot`）：有 profile 时用 `RegionalCheckpoint.Section.captureAir(IntToDoubleFunction)`
  和 `MaterialSectionState.capture(..., IntToDoubleFunction)`，只保留偏离本格背景的 Brick 和材料体；
  静止在自己背景上的格即使偏离 Section 中心值也不写盘。
- 重入恢复：`DormantAirCut.cellTemperatureC(brick, block, naturalC)` 由场传入本格背景；材料体按
  `TiledThermalField.naturalC(position)` 衰减。
- 休眠读数：空气 `DormantChunkThermalState.sampleAt`，材料 `DormantChunkThermalState.projectMaterial`（温度计、
  `MinecraftPhaseController`），红外 `InfraredCapture.readStoredPage`，以及休眠方块变化前的冷却
  （`MinecraftThermalInput` 里 `applyMaterialChange` 的 `coolingNatural`），都朝该方块的 `WorldTemperature.naturalAir`
  衰减，因此红外与温度计读数一致。这几处仍调用 `DormantChunkThermalState.naturalTemperature`，只为在 Section 值
  变化时推进材料 revision、让红外刷新。

仍以 Section 自然值为准的是：休眠维护的老化删除（`DormantThermalMaintenance`）、重入准入时的过滤
（`DormantChunkThermalState.admissionCut`、`MinecraftThermalInput.dormantMaterialAdmissionCut`）、检查点之后写盘之前的
方块替换（`MinecraftPageManager` 的 `projectMaterialCheckpoint`）以及旧版 Brick 均值残差。这些位置在
Section 内背景差较大时（地表附近、覆盖层厚度不一的 Section）会偏早或偏晚删除已有扰动，或给检查点后新放的方块
一个 Section 中心的初值。

## 查询与读者生命周期

`QueryPublication`只发布几何头和固定温场的实际提交版本。普通点查询直接读取Accepted，`ReadCursor`通过Page几何与世界单元地址读取温度、材料及采样时间。旧数值槽位查询、区域查询索引、区域帧租约和payload家族计账已经移除。

私有Working值不能被查询或普通SAVE读到。读者在提交后检查版本；关闭使旧读者失效，仍借用的Page元数据额度在最后一个读者释放后归还。超过原40tick有效期的数据继续按原玩法路径回退，不能给旧值重打新时间戳。

## 区块热数据v7

用户明确允许舍弃旧区块热数据，因此`FrostedHeartThermal.version`改为7，不迁移v6。版本不匹配时忽略整个旧热数据根，包括旧空气温度、材料储热和待处理热输入。这个决定只作用于该热数据根，未修改原版区块、物品或实体的存档处理。

`AirFieldCheckpoint`只保存Section原点、Brick/单元掩码、每个单元的实际温度和采样时间，不再保存旧角点、形状、模态或系数。加载当前v7值时直接恢复单元能量，休眠冷却从原采样时刻计算。合并、过滤和重新编码不能重置已保存数据的年龄。

材料继续使用`MaterialSectionState`保存状态身份、材料定律、焓、分支与采样时间。`RegionalCheckpoint`这个现有名字保留，但内容已收敛为Accepted Section空气/材料值和原输入所有者的pending J，没有旧区域帧列表、Part索引或FrameLease。

普通SAVE在原值/输入事务下取得一致快照；数值私有计算不持有这个保存事务。原生UNLOAD冻结真实Accepted值，保存不要求数值追平服务器tick。取消卸载后先完成原生回执，由worker推进相关邻区到真实重入边界，再按原休眠合同接回。

## 资源与故障

本轮没有调整进入1°C、退出/保存0.5°C、误差`max(1°C, 5% × |参照温度−环境温度|)`及工作额度。配置`continuousAirWorkBudgetMsPerSecond`仍按任务执行经过时间计量，包含抢占和GC，不能当作纯CPU。128MiB仍是原资源计账目标，不是已证明的实际可达堆硬上限。

旧投影专用的`continuousAirCorrectionToleranceC`配置及其引擎参数已经移除。当前构建/材料/源所共用的拓扑和物理定律继续使用原实现。

worker故障取消未提交状态，原Store、已提交温场和源窗口保持一次归属，经原保存/替换链路恢复。关闭清空原生回调持有关系，不能让旧场经保存回调继续存活。

## 验证及边界

完整矩阵157项中156项通过，最后一项原区域断言遗漏了当前固定引擎持有的pending J；只修正该测试后，相关8项全部通过，严格验证40/90J余额、材料不重复吸热和完整守恒。两轮生产逻辑相同，157项不同检查均有通过证据；没有把后续专项写成又一轮完整157/157。六次实际故障均存盘读回并回收旧场，v7写入/读回及旧版本忽略通过。

本轮退役范围、迁移测试、字节码依赖检查与最终结果见[生产运行链路退役回执](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-production-runtime-retirement-2026-11-14)。此前[提交时间批次](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-committed-domains-2026-11-14)和[第一轮小范围清理](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-production-cleanup-2026-11-14)只证明其对应代码。

11 月 18 日的第一组合并候选已有[历史对照](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-cpu-hot-paths-100p-2026-11-18)。当前生产代码采用上述有界截止查询，同一组 52 项 Forge GameTest 通过。百人服同基线的 CPU、时效、分配和恢复实测见[当前验证记录](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-bounded-source-cutoff-100p-2026-11-18)。后续局部几何改动通过 57 项测试，阶梯式改动范围与单轮百人数据见[局部几何实测](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-brick-work-100p-2026-11-18)。11 月 19 日的 66 项 Forge GameTest 全部通过。同一热源历史的三组对照在 5,888 个温度样本和热量账上差异为 0。百人服的 22,453 对同采样时刻温度中，有两次读数超出该点 1°C 的容差，均来自同一位置和同一温场采样时刻。实测 CPU、时效及采样范围见[覆盖掩码验证记录](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-packed-mixing-100p-2026-11-19)。完整 128 MiB 可达峰和 8h/24h 耐久资格仍待验证。
