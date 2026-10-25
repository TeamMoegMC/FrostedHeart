# 城镇建筑：数据、恢复与世界绑定

- Status: Partial — 源码与 28 个真实 Forge 功能场景、独立进程 seed/migrate/read 已核对；图形客户端与长时负载尚未完整验证。
- Last verified: 2026-10-23
- Scope: 玩家队伍的六类逻辑建筑、持久化、名册关联、核心生命周期、结构与环境刷新、管理视图。
- Code anchors: `TownBuildingStore`, `BuildingRecordsCodec`, `TownBuildingTypes`, `TownBuildingChangeEvent`, `AbstractTownBuilding`, `TeamTown`, `TownAssignments`, `AbstractTownBuildingBlockEntity`, `LoadedTownWorld`, `BlockScanner`, `SchedulerQueue`。

## 数据归属

`TeamTownData` 持有一个 `TownBuildingStore`。store 拥有建筑记录、未解析原文、恢复摘要和每栋建筑的一份编码缓存。`resolvedMap()` 是只读投影，与存储记录指向同一个 `AbstractTownBuilding`，没有第二份可经营状态。

建筑对象继续保存经营名册、生产小数余量、最近日报，以及面积、容量、布局、温度等最后已知输入。物理方块、设备缓冲、供热端点和 `townProvider` 仍由 Minecraft 区块 NBT 保存。Level、方块实体、扫描任务、监听器、重叠关系准备状态和运输拓扑属于服务器会话。

建筑仍在 `chorda_data/<holderId>.nbt` 的 `data.town` 内保存。每栋缓存减少业务 Codec 的重复执行；整镇组装、冻结复制、整个队伍文件的压缩与替换仍然存在。后台写出继续由原有 `PersistenceCoordinator` 负责。缓存成功编码不等于磁盘成功，只有队伍记录的写入回执推进持久版本。

## 文件格式与恢复

`BuildingRecordsCodec.DISK` 拥有以下顶层字段：

| 字段 | 含义 |
|---|---|
| `blocks` | 已解析记录列表，每项为 `{pos, version, building}` |
| `buildingDimension` | 可选的建筑归属维度 ID |
| `retainedBuildingData` | 保留原文列表，区分 `pending` 和 `archive` |

`TownBuildingTypes.VERSION` 为 `1`。缺少 `version` 是旧版本 `0`；旧整数映射固定为 `0=house`、`1=huntingBase`、`2=mine`、`3=mineBase`、`4=warehouse`、`5=transportStation`。这个映射不依赖今后的注册顺序。每类 `MAP_CODEC` 声明拥有的顶层字段，`CODEC` 从它派生；类型表通过延迟 supplier 避免父类与子类静态初始化循环。

读取逐项进行。外层合法 `pos` 是位置权威，覆盖解码候选中的内部镜像；冲突或损坏的原位置进入字段 archive。只有内部位置合法时可以恢复外层位置。两个位置都无法解释的条目只归档，不生成原点建筑。同坐标选择第一条完整可解析记录，其余归档；没有健康候选时，第一条可定位记录成为 pending。

| 问题 | 恢复动作 |
|---|---|
| 未知类型、未来版本、损坏的经营名册或生产余量 | 整条 pending，保留原文 |
| 面积、几何、布局、温度、容量等可重建字段损坏 | 字段原文 archive，使用默认输入并标记尚未完成结构复核 |
| 最近日报无法解码 | 日报原文 archive，使用 EMPTY 日报，保留其他经营数据 |
| `blocks` 整个容器无法解释 | 归档原容器，继续读取城镇其他部分，保留计划与关联意图 |
| 同坐标冲突条目 | archive，不覆盖已选建筑 |

`BuildingRecordsCodec.strict()` 只改变建筑字段的可选值语义：缺失可以使用默认值，存在但错误必须报告解码失败。它不改变全局 Codec 工具。

只有 pending 会在启动或资源重载时重试；普通核心绑定和 getter 不重复解码失败原文。archive 不自动注册。保留区按原值去重，重开不再包装同一份已保留数据。实际拆除或明确新放置会清理该坐标的 pending 与可定位 archive；没有位置的原文继续留作证据。

已解析记录保留条目层和 building payload 顶层的未知字段。已知字段归当前 Codec 完整拥有，可选字段本次省略即保持删除，旧入口不会从原文合并回来。日报、布局、列表等嵌套格式变更必须提高记录版本；不支持在同一版本内任意合并嵌套扩展。编码失败使整个队伍捕获保持待保存状态，不能跳过该栋输出缺项检查点。

## 名册与恢复意图

`Resident.housePos`、`workPos` 只表示实际分配。无法解析的原住房或岗位坐标保存在一个 `Resident.PendingAssignments` 字段中，分别使用可选的 `housePos` 和 `workPos`。它参与 `Resident.CODEC` 与手动 NBT 保存，不直接提供住房、工作或模拟锚点。

`TownHousingPlan.normalize(buildings, preserve)` 和 `TownStaffingPlan.normalize(buildings, preserve)` 保留 pending 坐标对应的顺序及目标。整个建筑容器损坏时也保留原计划。日常分配仅使用可工作的已解析建筑，其他有效住房和岗位仍可以接收居民。

`TownAssignments.reconcile` 优先整理现有有效名册，再处理本次恢复的建筑。恢复名册还检查可工作性、容量和岗位条件，不抢占有效分配、不重复计入居民，也不补历史经营。目标位置已有明确且不兼容的类型时，移除对应旧计划和恢复意图。`allocateHouse`、`assignWork` 先计算最终名册，再通过 `replaceResidents` 一次提交各建筑的结果；查询集合只读。

居民解码和查询不生成随机熟练度。`Resident.initializeServerData()` 在服务器安装、招募或 AI 城镇接管时补齐旧档缺少的职业数据，并报告真实变化。

## 可读的修改通知与编码缓存

`TownBuildingChangeEvent.ChangeKind` 使用命名枚举：

| 名称 | 额外影响 |
|---|---|
| `DATA` | 经营值或环境输入变动，保存并同步建筑 |
| `STRUCTURE` | 占用空间变化，需要重算重叠关系 |
| `AVAILABILITY` | 可工作性或容量变化，需要刷新相关服务 |
| `REGISTRATION` | 添加、移除或替换，更新计划与派生关系 |

所有变化都使该栋记录缓存失效；类型说明其余哪些工作需要重做。调用处通过 `requiresSpaceRecheck()`、`affectsAvailability()` 表达用途。`updateBatch(Runnable)` 在一批字段应用完成后合并发布一次；未变化的 setter 和相同最终名册不增加建筑版本。这里没有要求读者计算数字位掩码。

store 以 `changeVersion` 与 `encodedVersion` 判断单栋缓存是否过期。元数据修复、迁移、维度确认和 pending 提升通过独立通知使 town/TEAM 变脏，即使没有可运行建筑也会保存。`codecCalls()` 统计持久编码执行次数，`metadataVersion()` 用于核对元数据变化；它们不表示文件写入次数或总耗时。

## 核心生命周期与维度

业务变更通过 `TeamTown` 的三个入口完成：

- `onCorePlaced`：实际新放置，清理旧位置数据并创建新记录；同一核心实例的重复放置回调保持幂等。
- `ensureCoreBound`：加载与修复，兼容既有记录时连接原对象，pending 位置等待恢复。确实缺项时通过 `installMissing` 补记录，保留归档、计划及恢复意图。
- `onCoreRemoved`：实际移除，核对当前世界、核心和建筑实例，完成名册、计划、原文、矿场链接及容量清理。

`AbstractTownBuildingBlockEntity.getBuilding()` 是纯查询，并核对当前方块实体和建筑维度。`onLoad` 注册扫描任务，`onUnloaded` 释放任务但保留经营记录，`onRemoved` 处理实际拆除。周期绑定也能处理核心先加载、队伍稍后才就绪的顺序。

经营或绑定占用当前 town 操作边界时，核心请求进入 `TownRuntimeManager` 的按 holder/位置合并队列，在操作结束后核对当前组件并执行。晚到的旧核心移除不能删掉新核心。捕获和发送完整管理视图不会执行实际住房或就业分配。

`buildingDimension` 表示存储归属；`TeamTownData.executionLevel(holder)` 表示当前执行世界，并要求 holder 仍是当前队伍注册表中的实例。旧档在完整 holder 安装后从 generator 恢复归属。已有记录不随 tower 换维度而自动迁移。generator 缺失、维度不匹配或 Level 不存在时，执行连接不可用，建筑事实仍保留。模拟注册表、运输设备、管理视图准备、`/town tick` 与 `/town spawn_refugees` 使用同一执行上下文。

`TownSettlement.advanceOneDay` 与调试难民入口还调用 `isCurrentExecutionContext`，在读取世界前核对当前 town 组件和传入的实际 `ServerLevel`。错误世界、已替换组件和已退休 holder 不推进经营；重入仍优先返回 `IN_PROGRESS`。

区块文件和队伍文件继续各自保存，不具有共同事务；重开后以已加载世界事实对账。

## 扫描与环境刷新

`LoadedTownWorld` 只使用已加载区块，缓存只存活于一次查询。结构候选遇到缺区块时返回 `DATA_UNAVAILABLE`，保留最后已知输入，不将缺区块作为空气提交。扫描器处理维度顶底边界；温度读取也通过非加载 biome source，拒绝缺数据、空采样和非有限结果。

`OccupiedZoneInfo` 是不可变值。`sameGeometry` 比较空间，`equals` 还比较有效性、错误位置与重叠状态；`OccupiedCell` 包含可达高度的相等性。扫描先计算布局、设施与容量，再在建筑更新批次中应用。空间变化批量重算重叠后，才更新仓库容量和运输视图。

服务器主线程沿用 `SchedulerQueue`。每个实际 `ServerLevel` 会话一份按坐标的 `LinkedHashMap` 轮询队列；注册去重，句柄取消在任务临时取出时也有效，世界卸载和停服清理。客户端 tick 不注册服务器任务。环境目标间隔为 `20` game ticks，结构复核为 `200` game ticks，资源重载可提前复核；期限使用 `gameTime`。

间隔受 `ChordaConfig.SERVER.taskPerTick` 与队列长度限制。B 个已加载核心、每 tick 平均 q 次机会时，一轮约为 B/q ticks；20/200 是到期目标，不是每栋的硬时限。环境刷新复用几何，仍需对可达内部位置采样，未增加第二套后台扫描线程。

狩猎站无可用面积/体积或评分计算未得到有限值时，使用 `0` 评分；正常几何下沿用原公式。无效房间的环境刷新不会把 NaN 写入快照，生产小数余量保持原值。

矿业配方缓存按 RecipeManager 身份和重载生命周期失效，空结果也视为已经加载。读取与失效入口使用同一同步锁，避免单人游戏客户端读到服务端正在清空或构建的表；返回的权重 Map 不可变。完整加载后才记录 RecipeManager 身份。服务器停止时释放缓存；远端客户端收到配方更新时失效自己的缓存。

旧建筑扫描链已清理；`AbstractBlockScanner`、`ConfinedSpaceScanner` 仍是知识实验条件的实际依赖，应继续保留。

## 管理视图与验证入口

`TeamTownData.MANAGEMENT_CODEC` 导出可解释建筑及 `buildingLoadIssues` 摘要，不发送 `retainedBuildingData` 原文。磁盘继续使用 `TeamTownData.CODEC`。FH 协议版本包含 `:town-view-1:building-data-1`，两端须一起更新。居民页显示待恢复住房与岗位；建筑页显示恢复摘要。

`TownBuildingGameTests` 覆盖真实核心、原版保存入口、混合损坏记录、恢复关联、布局扫描、非加载查询、操作边界及编码失败。`TeamTownPersistenceRestartGameTests` 使用独立 `seed/migrate/read` 进程读取同一个测试世界，seed 由真实保存结果生成旧格式，读取阶段不重建预期数据。

2026-10-23 的 `build/town-building-functional-r3.log`（本地日志） 记录 28 个 required GameTest 全部通过：建筑专项 16 项、基础存储 10 项、知识登录 2 项。覆盖真实六类核心、混合损坏恢复、关联意图、删除不复活、单栋编码缓存、原版保存失败恢复、实际门床扫描、无效狩猎房间、非加载读取、错误维度、旧实例、操作中核心替换及真实资源重载。较早一轮的替代住房夹具缺少可入住输入，已修正；住房判定规则没有为测试放宽。

独立进程 `build/town-building-restart-seed.log`（本地日志）、`build/town-building-restart-migrate.log`（本地日志）、`build/town-building-final-restart-read.log`（本地日志） 分别通过。最终 read 使用本轮代码，保留同一个 `build/town-building-gametest-restart/world`，manifest 仍为 08:09 的原文件。它检查跨进程居民、经营/模拟、六类实际核心、生产余量、未知顶层字段和两份保留记录，未重新生成预期状态。

功能运行同时取得以下单次采样，单位为 ms，包含 town NBT 组装与复制：

| 实际核心数 | 首次捕获 | 稳定缓存捕获 | 修改一栋后的捕获 |
|---|---:|---:|---:|
| 10 | 0.3552 | 0.0879 | 0.1434 |
| 100 | 2.2818 | 0.2662 | 0.5769 |
| 500 | 6.6326 | 0.9681 | 1.0730 |

这些数据比较同一实现的冷缓存与热缓存；不是旧架构基准，也不是完整 tick 的百分位。原版强制保存覆盖其他队伍和世界区块，不能把日志中的 `forcedSaveMs` 当成单镇独占 I/O。缓存内存峰值、长时扫描延迟分布、图形客户端及多客户端操作仍需对应工作负载验证。整体保存边界见 [persistence-and-runtime.md](persistence-and-runtime.md)。
