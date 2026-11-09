# 队伍与城镇：保存、加载和运行边界

- Status: Partial — 调度计划同步已通过39项统一Forge用例及岗位/住宅调度真实客户端验证；后续局部编辑回复修复仅做源码检查，待新增场景验证。三类建筑GUI与独立进程迁移/重开沿用原记录，远程多客户端与长时规模性能未完整验证。
- Last verified: 2026-11-09（岗位/住宅计划同步、统一回归和真实镇长印章）；2026-11-07（三类建筑GUI）；2026-10-23（独立进程最终read）
- Scope: 队伍组件、玩家居民模拟、AI 城镇、旧档迁移、自动经营和管理视图同步。
- Code anchors: `CTeamDataManager`, `TeamDataHolder`, `PersistenceCoordinator`, `NbtRecordWriter`, `TownPersistenceAdapter`, `TownRuntimeManager`, `TownSettlement`, `AutoSettlementState`, `TownSync.ensureCurrent/requestFull`, `TownClientUpdates.pending/currentTown`, `ObjectWriter.readWithType`。

## 数据归属

`TeamTownData` 保存经营事实，包括居民档案、建筑名册、库存、计划、政策、历史和自动结算进度。`TownSimData` / `CitizenSim` 保存居民位置、朝向、行为和目标；用居民 UUID 与经营档案关联。两者仍是独立对象。

玩家镇以内部 `holderId` 为存储所有者，文件为 `chorda_data/<holderId>.nbt`。文件内 `data.town` 是经营数据，`townSim` 是该队伍的模拟；`data` 同时保留研究、能量塔等既有组件。`externalTeamId` 用于 FTB 或其他队伍提供者的映射，不用作新文件键。单个完整文件简化这两部分的检查点一致性，代价是模拟独自变化时，整个队伍文件仍需压缩和替换。

AI 镇使用 `data/fh_ai_towns/<aiTownId>.nbt`，不创建虚假的玩家队伍。未托管的命令居民及每维度 ID 分配器继续使用原版 `fh_citizen_sim` SavedData。`data/fh_ai_towns.dat` 由新适配器负责，保留 `lastAIId` 和尚未确认迁移的旧 `towns` / `playerSims`；`AITownManager` 只保留当前会话的对象注册表。

## 保存

服务器主线程拥有活对象和版本。`PersistentComponent` 通过监听器使组件缓存失效；经营数据、玩家模拟及 AI 档案采用所属记录的变化通知。尚未完成所有嵌套修改通知的组件，每次保存重新编码并与私有冻结缓存比较：未变化不重写文件，但仍有编码成本。客户端 holder 默认没有服务器存储绑定。

建筑集合使用 `TownBuildingStore` 的单栋懒编码缓存；坏条目逐项保留，迁移和恢复元数据也会使 town/TEAM 变脏。完整格式及恢复边界见 [building-data.md](building-data.md)。

`PersistenceCoordinator` 从统一记录表捕获 TEAM、AI_TOWN、TOWN_METADATA 和退休记录。捕获不会创建模拟、推进经济或读取世界来修复数据。经营组件与模拟各自缓存独立 NBT，只有相关输入变化才重新生成该部分；快照交付后台后不再修改。未知组件和未知根字段继续保留。

一个逻辑 drain 在 Minecraft 的 `Util.ioPool()` 上连续运行，负责压缩和替换文件，不需要后续游戏 tick 才能开始下一条任务。默认最多 64 个在途快照、估算 NBT 预算 32 MiB；单条超过预算的记录可以独占通道完成。预算包含正在写入和待写任务；等待预算可能阻塞捕获线程，不保证无限规模下保存完全不占主线程时间。

同一记录最多有一条正在写入的快照和一条待写快照；待写的旧版本可由新版本覆盖。只有实际成功回执推进 `persistedRevision`，旧回执不能清掉新修改。压缩使用 GZIP level 1、64 KiB 缓冲；先完成同目录临时文件，再替换正式文件，不先删除正式文件。普通 I/O 错误最多尝试三次，间隔 50/200 ms，终结失败保留待保存状态并记录原因。

原版普通保存的 HEAD 事件捕获异步任务，研究目录原有监听仍执行。强制保存、flush 和停服在原版 `saveAllChunks` RETURN 等待：业务记录 → 获得实际替换确认的退休记录 → 迁移元数据。模组记录失败会使保存结果为 false，原版世界保存已经执行。`ServerStopping` 在世界仍可用时准备快照；`ServerStopped` 排空已冻结任务后，统一清理适配器、模拟注册表和队伍绑定。共享 I/O 池不由本系统关闭。

## 加载与迁移

当前实现仍在启动时读取和解码记录。`loadIfNeeded()` 只是既有队伍加载事件的延迟触发，不代表惰性读盘或惰性解码。

启动先读取旧全局来源和独立 AI 文件，再读取队伍记录。规范 V1 文件头的 `deleted` 与 `supersededHolderIds` 先参与排除，再建立队伍映射；目录枚举顺序不决定被替换队伍是否恢复。规范文件优先于旧别名。只有成功写出新记录后才清理对应旧来源。

V1 `townSim` 显式为空表示没有模拟，不能从旧全局文件补回来。显式移除 `TOWN_DATA` 会注销运行绑定和玩家模拟，并保存缺失的 `data.town` 与空 `townSim`。移除 generator 只影响世界执行绑定，保留经营和模拟事实。provider、管理界面和状态查询使用非创建式读取。

加载先用旧岗位名册迁移保障人数，再清理无效和重复名册、恢复居民的住房／岗位镜像。合法名册是分配权威；无法解析的住房/岗位进入 `Resident.PendingAssignments`，实际分配字段为空，计划保留但不产生住房收益或模拟锚点。恢复名册不会抢占有效分配。加载不执行生产、配餐或衰老。模拟直接填充最终数组，一批居民对账使用临时 UUID → simId 索引。首次分配和显式接管会校准每维度 ID 分配器。既有每 20 tick 的注册表检查目前仍保留；未宣称已经消除全部周期扫描。

## 经营与生命周期

`TownSettlement.advanceOneDay` 固定日阶段顺序；生产平局按工作优先级降序、坐标 x/y/z 升序处理。`prepareDayCommit` 先构造历史，`commitDay` 一起更新历史、`townDay` 和可选自动完成标记，`publishDayEffects` 再发布通知。通知失败保留已完成经营事实，只安排绑定／视图恢复。单镇的重入调用返回 `IN_PROGRESS`。

自动机会使用主世界气候时钟：`logicalTick = getSec()*20 + elapsedDayTimeTicks(getClockDayTime(), overworld.getDayTime())`，单位是游戏 tick。相位为 `1000 + floorMod(holderId.hashCode(),20)`，窗口为 `floorDiv(logicalTick-phase,24000)`。所有镇在一个 Server END 使用同一次时钟采样。

持久 `autoSettlement` 包含 `processedThrough`、可选 `lastCompleted` 和可选 `pendingOrigin`。冷加载不补算过去窗口；离线、开关关闭和执行维度缺失只推进运行观察值。真正新建使用 NEW 来源，旧档缺字段使用 LEGACY，旧档跳过迁移所在逻辑日。时钟不可用时来源仍可保存。手动 `/town tick` 通过 `executionLevel` 选择建筑归属与 tower 匹配的世界，每次独立调用推进一天，不消耗自动机会。

`buildingDimension` 保存建筑归属，`TeamTownData.executionLevel` 统一导出当前可执行世界；维度错配暂停服务并保留事实。`TownRuntimeManager` 登记当前对象、绑定世界和模拟，并消费有限的生命周期请求。核心放置/移除在经营与绑定期间进入同一操作边界，按位置合并并核对当前组件。组件替换关闭旧菜单，解绑旧监听，再接管新对象；模拟接管和设备重新订阅有实际修复入口。经营与模拟仍在服务器主线程执行。气候 Level START 保留时钟与离线供热，居民 Level END 保留模拟，城镇 Server END 完成自动经营、同步及统一可见性刷新。

## 管理视图

`TeamTownDataS2CPacket` 使用 PENDING / PRESENT / ABSENT 信封。完整经营数据和运输视图成功解码为候选后才安装。完整建筑视图使用 `MANAGEMENT_CODEC`，只包含可解释对象和 `buildingLoadIssues` 摘要；未解析原文仅留在磁盘。每连接先建立完整视图，之后才发送 `TownViewPacket` 增量或提示。库存、居民、普通建筑新增/修改/删除使用已有增量，脏键仍在同一服务器轮次合并。

建筑注册变化调用 `TeamTownData.prepareAssignments`。`normalizeStaffingPlan` / `normalizeHousingPlan` 仅在权威计划实际变化时标记 `DataSyncCache.staffingPlanChanged` / `housingPlanChanged`；`TownSync.flushChanges` 先发送 `TownBuildingUpdatePacket`，再分别发送已有的 `TownStaffingPlanUpdatePacket` / `TownHousingPlanUpdatePacket`。同一轮多次建造、拆除或核心类型替换只同步最终计划；普通温度、产出报告和名册变化不重复发送未变计划。捕获异常保留相应脏标记，沿用现有恢复路径。

客户端不能从新收到的建筑名册人数推断保障目标：建筑首次增量抵达时，服务端可能已经给它分配了 3 名员工，而玩家设置的保障人数仍为 0。漏发权威计划会使岗位和住宅页面的建筑列表为空或过期，即使居民住房、工作和配餐均已正确结算。只关闭重开镇长印章会复用同一完整视图基线，不能可靠修复；完整重连可重新取得当前计划。界面字段含义见 [岗位界面](town-model.md#224-玩家界面)。

该回归的历史原因见 [2026-11-09 调查记录](../../diary/2026-11-09_19-51-00_town-scheduling-plan-sync-regression.md)：`763d8475b`（2026-10-26）将 getter 中的自动规范化迁到显式准备阶段，当时建拆建筑和打开 GUI 的全量同步仍补齐客户端计划；`fdacc2bc7`（2026-11-08）去掉普通建筑变化与打开 GUI 的重复全量同步，却未同时补发两种计划增量，使旧缺口可见。

`TownSync.ensureCurrent`供建筑GUI、镇长印章和无城镇的经营状态查询复用：连接的holder与`TeamTownData`对象均未变化且已经就绪时不发送完整视图。登录、重生、换队、组件变化和客户端恢复请求使用`requestFull`；待处理请求在既有连接基线中合并，不重置正在进行的重试。完整捕获在Server END的`TownSync.tick`进行。`TownBuildingStore`元数据回调的布尔参数区分普通记录变化与增量未包含的管理元数据：维度确认、恢复摘要等少见变化仍可安排完整视图，普通建筑增删不再令整队基线失效。

客户端`TownClientUpdates`将显示数据与增量接收资格分开。同一非空holder的PENDING只暂停增量应用，并保留当前`TeamTownData`用于显示，不触发整批GUI变更通知；新的完整候选解码完成后一次替换，再恢复增量。不同holder、未知归属、明确ABSENT和断开连接会清除旧显示。同镇更新没有第二份旧快照，也不重建现有GUI元素。服务器PENDING携带边界处解析出的真实holder，普通刷新不再先发`pending(null)`。重复同镇PENDING不会重新开始客户端退避。

压缩MapCodec的省略字段在混合列表中使用Java `null`槽。`ObjectWriter.readWithType`必须将列表中的类型标记0读回`null`；此前读成`DataOps.NULLTAG`会让严格可选建筑字段被当成“存在但不是数字”，实际客户端报`Not a Number`并不断请求完整快照。修正保留原有线上类型标记及字节格式，已有的整数、长整数、字符串和空列表编码保持；混合列表的标记区使用当前输入的`readSlice`，不再复制一份未归还的临时ByteBuf。

居民和建筑增量在服务器构造时编码，避免晚编码读取已经改变的对象。客户端应用失败暂停后续增量，通过`TownSnapshotRequestPacket`请求完整数据；同一holder可继续展示当前数据。重试间隔从20 ticks退避到200 ticks。READY表示已提交到有序通道，不代表客户端ACK。实际发送完整视图时的DEBUG `TOWN_VIEW_FULL`记录player、holder、reason与present；原因包括`initial_baseline`、`baseline_changed`、`building_metadata`、`client_recovery`和`delivery_failed`。FH通道协议版本继续包含`:town-view-1:building-data-1`，本次没有更改包字段。

## 验证入口

城镇验证按用户要求使用真实游戏测试，不运行 JUnit。2026-10-26 的城镇独立发布仅删除城镇及其队伍归属相关的 88 份旧 JUnit；其他系统的测试清理与全局 JUnit 构建依赖删除没有纳入这次发布。`TeamTownPersistenceGameTests` 在真实 Forge 服务端检查原版保存入口、实际队伍文件、组件替换、手动完成点、队伍转移，以及文件写失败后的原版保存和重试。`TownBuildingGameTests` 覆盖实际核心、逐项恢复、名册关联、扫描、错误执行上下文、资源重载与原版保存错误。2026-10-23 的统一运行共 28 项全部通过，结果记录在本地 `build/town-building-functional-r3.log`。

本批功能目录为 `-PgameTestDirectory=build/town-building-gametest-functional`，重启目录为 `build/town-building-gametest-restart`；两者复用本仓库编译产物。`TeamTownPersistenceRestartGameTests` 的 `teamTownPersistencePhase=seed/migrate/read` 分别在独立服务器进程验证旧格式迁移及再次重开，包含六种真实核心与 pending/archive。迁移和读阶段使用 `-x cleanResearchGameTestWorld` 保留原世界；`build/town-building-final-restart-read.log`（本地日志） 已通过。工程结果与成本边界见 [building-data.md](building-data.md)；执行命令及未覆盖项记录于开发日记。

2026-11-07最终安装包与统一回归使用`build/town-view-sync.init.gradle`，四个namespace为`frostedheart_town_view,frostedheart_buildings,frostedheart_persistence,frostedheart_warehouse_network`，36/36通过，见[统一日志](../../build/town-view-sync-release-20261107.log)。`TownViewSyncGameTests`含7项新检查；双ServerPlayer/EmbeddedChannel场景保持住宅菜单400tick，实际仓库菜单存入20次、建筑增删各一次，两连接各收到20资源增量和2建筑增量，完整信封均0。

同一实现另由`TownViewClientValidation`在真实Minecraft客户端与集成服务器执行：住宅、狩猎站、矿场各600tick，第二个ServerPlayer通过EmbeddedChannel参加。5037个受检渲染帧数据空白0、库存存入21次；保留同镇PENDING画面并仅完成一次主动恢复。正常操作未产生额外全量安装；第二个连接完整信封0、资源增量22、建筑增量13。[结果和截图](../../build/town-view-client-20261107/result.json)、[客户端日志](../../build/town-view-client-20261107-r6.log)保留实际边界：这不是两台远程图形客户端或百人长期性能资格。

2026-11-09 使用 [town-scheduling-validation.init.gradle](../../Scripts/town-scheduling-validation.init.gradle)，在上述四个 namespace 加入 `frostedheart_town_scheduling`，39/39 required 通过。[统一日志](../../build/town-scheduling-server-r3.log)包含真实连接上的权威计划、普通建筑更新不重发计划、核心类型替换、零保障就业/住房/配餐及既有城镇回归。`TownSchedulingClientValidation` 在真实图形客户端和集成服务器从空城镇基线开始建造、扫描并招募 3 人，保持原界面，经过鼠标滑条请求、次日结算、加建、拆除和同位置核心替换；844 渲染帧、额外完整视图安装 0，结果通过。建筑温度受控为 20 ℃，不据此推断低温、远程延迟或规模性能资格。[受控结果](validation/staffing-housing-client-2026-11-09.json)、[原始结果和截图](../../build/town-scheduling-after-r4/result.json)、[客户端日志](../../build/town-scheduling-after-r4.log)记录实际边界。

随后补入 `TownEditorClientChecks`，由上述图形入口在原场景结束后执行。它在客户端组件中控制草稿和回包顺序，检查两种内容相同的计划回复、拖拽期间延后处理、连续改名、相同名字回复、回车后失焦、其他居民更新与子输入框失焦顺序；计划回复经过真实包编码/解码。用户要求本轮不 build，这些新增检查尚未编译或执行，旧结果文件不含 `editorReplyChecks`，不能作为本轮编辑回复修复的通过证据。字段含义和当前源码处理见 [名称编辑](town-model.md#213-玩家界面)与 [计划编辑](town-model.md#223-持久化迁移和联机编辑)。
