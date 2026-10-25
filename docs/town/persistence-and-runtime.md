# 队伍与城镇：保存、加载和运行边界

- Status: Partial — 源码已核对，28 个真实 Forge 功能场景及独立进程迁移/重开通过；图形、多人大规模与长时性能未完整验证。
- Last verified: 2026-10-23（建筑与存储统一功能复验，以及沿原世界的独立进程最终 read）
- Scope: 队伍组件、玩家居民模拟、AI 城镇、旧档迁移、自动经营和管理视图同步。
- Code anchors: `CTeamDataManager`, `TeamDataHolder`, `PersistenceCoordinator`, `NbtRecordWriter`, `TownPersistenceAdapter`, `TownRuntimeManager`, `TownSettlement`, `AutoSettlementState`, `TownSync`, `TownClientUpdates`。

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

`TeamTownDataS2CPacket` 使用 PENDING / PRESENT / ABSENT 信封。完整经营数据和运输视图成功解码为候选后才安装。完整建筑视图使用 `MANAGEMENT_CODEC`，只包含可解释对象和 `buildingLoadIssues` 摘要；未解析原文仅留在磁盘。每连接先建立完整视图，之后才发送 `TownViewPacket` 增量或提示；登录、重生、换队、组件变化和显式请求均使用 `TownSync`。

居民和建筑增量在服务器构造时编码，避免晚编码读取已经改变的对象。客户端应用失败会清除未就绪视图，并通过 `TownSnapshotRequestPacket` 合并请求完整数据；重试间隔从 20 ticks 退避到 200 ticks。READY 表示已提交到有序通道，不代表客户端 ACK。FH 通道协议版本包含 `:town-view-1:building-data-1`，新请求 ID 追加在已有注册之后。

## 验证入口

城镇验证按用户要求使用真实游戏测试，不运行 JUnit。2026-10-26 的城镇独立发布仅删除城镇及其队伍归属相关的 88 份旧 JUnit；其他系统的测试清理与全局 JUnit 构建依赖删除没有纳入这次发布。`TeamTownPersistenceGameTests` 在真实 Forge 服务端检查原版保存入口、实际队伍文件、组件替换、手动完成点、队伍转移，以及文件写失败后的原版保存和重试。`TownBuildingGameTests` 覆盖实际核心、逐项恢复、名册关联、扫描、错误执行上下文、资源重载与原版保存错误。2026-10-23 的统一运行共 28 项全部通过，结果记录在本地 `build/town-building-functional-r3.log`。

本批功能目录为 `-PgameTestDirectory=build/town-building-gametest-functional`，重启目录为 `build/town-building-gametest-restart`；两者复用本仓库编译产物。`TeamTownPersistenceRestartGameTests` 的 `teamTownPersistencePhase=seed/migrate/read` 分别在独立服务器进程验证旧格式迁移及再次重开，包含六种真实核心与 pending/archive。迁移和读阶段使用 `-x cleanResearchGameTestWorld` 保留原世界；`build/town-building-final-restart-read.log`（本地日志） 已通过。工程结果与成本边界见 [building-data.md](building-data.md)；执行命令及未覆盖项记录于开发日记。
