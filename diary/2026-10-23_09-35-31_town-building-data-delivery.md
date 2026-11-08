# 城镇建筑数据系统交付与闭合复验

- Time: `2026-10-23 09:35:31 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation, source review and real Forge validation`
- Status: `completed`（本批实现与服务端验收完成；未覆盖的图形、长时及规模验证在 Remaining 列明）
- Scope: [统一建筑数据计划](../plans/2026-10-23_00-18-00_town-building-data-system.md)、六类建筑、存档恢复、核心绑定、居民关联、非加载扫描、管理视图与相关真实游戏测试。

## Completed

- `TownBuildingStore` 统一拥有已解析建筑、pending/archive 原文、恢复索引与单栋懒编码缓存；`BuildingRecordsCodec` 逐项加载，`TownBuildingTypes` 固定类型名称、版本 1 与旧整数 0..5。损坏条目不清空健康建筑；损坏快照与展示日报按字段归档，关键经营数据保留整条待恢复。
- 名册、矿场链接和空间快照提供只读查询。建筑 setter 比较值，批量更新合并通知；`ChangeKind` 使用可读枚举。存储缓存失效、town 组件 dirty、队伍实际写入回执维持各自含义。
- `Resident.PendingAssignments` 保存未知建筑的关联意图，实际 housePos/workPos 继续只代表有效分配。计划保留未知位置；恢复不得抢占有效分配，不提供虚构住房或模拟锚点。解码/查询不再随机补熟练度，服务器安装时明确补齐。
- 实际放置、加载绑定/缺项修复、实际移除分别使用 `onCorePlaced`、`ensureCoreBound`、`onCoreRemoved`。缺项修复通过 `installMissing` 保留 archive、计划与意图；真实替换才清理旧位置。操作占用期间按 holder/位置合并，晚到的旧核心不能删除替代核心。
- `buildingDimension` 保存建筑归属，统一 executionLevel 连接模拟、运输、管理准备和命令。最终复查补齐结算/调试难民入口：错误世界、已替换 town 或已退休 holder 在世界读取前跳过。
- 扫描使用已加载区块；缺数据保留最后已知输入。环境/结构目标间隔为 20/200 game ticks，轮询任务绑定实际世界会话并支持取消/释放。狩猎站空几何/非有限评分使用 0；矿业配方缓存的读取和重载同步访问，完整构建后才标记已加载。
- 管理全量数据不发送恢复原文，只发送可解释建筑与恢复摘要；已知建筑使用现有差量。居民页显示待恢复住房/岗位，建筑页显示摘要，协议包含 `:town-view-1:building-data-1`。
- 删除无调用的 `BuildingBlockScanner`、`FloorBlockScanner`、`CachedWorld`、`TownLevelWorld`、`MineBaseBlockScanner`；保留仍被知识条件使用的旧通用扫描器。没有增加 JUnit，拉取带入的 8 份 JUnit 按既有用户要求移除。

## Decisions

- 沿用服务器主线程拥有经营与世界访问、既有后台队伍快照写出；三类建筑基础设施完成职责收口，不新增游戏线程、数据库或每建筑文件。
- 顶层未知字段保留，已知可选字段省略即保持删除；嵌套格式改变必须升级条目版本。archive 保留证据但不自动注册，真实移除清理该坐标的恢复资料，关闭删除后复活路径。
- 编码失败保留上一份完整文件和待保存版本，不用旧缓存假报成功。队伍文件与区块文件继续独立保存，重开对账处理连接。
- 所有构建直接使用当前共用仓库。等待温度矩阵 16 组结束后于 09:29:44 应用最终补丁，生产源码保持稳定直至本批功能、read 和构建结束；热力源码未由本轮城镇收尾修改。拉取事件保留于原 [恢复记录](2026-10-23_01-34-21_pull-shared-workspace-thermal-preservation.md)，未重复应用恢复 stash。

## Validation

- Java 17；最终功能命令：`gradlew.bat -PgameTestDirectory=build/town-building-gametest-functional -PgameTestNamespaces=frostedheart_buildings,frostedheart_persistence,frostedresearch_knowledge_login runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`。09:32:07 所有 28 个 required GameTest 通过，退出 0、1m49s；[完整日志](../build/town-building-functional-r3.log)。包含真实核心、保存/重装、50 条混合损坏、计划/居民恢复、同位置替换、错误维度、旧实例、扫描、编码/I/O 失败及真实资源重载。首次运行的住房夹具问题已修正，未为测试放宽玩法规则。
- 跨进程 [seed](../build/town-building-restart-seed.log)、[migrate](../build/town-building-restart-migrate.log) 已通过；最终额外执行 `gradlew.bat -PgameTestDirectory=build/town-building-gametest-restart -PgameTestNamespaces=frostedheart_persistence_restart -PteamTownPersistencePhase=read -x cleanResearchGameTestWorld runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`。09:33:18 read 通过，退出 0、45s；[日志](../build/town-building-final-restart-read.log)。沿用原世界，manifest 保持 08:09:03、180 bytes，六类核心重新绑定原恢复对象，居民、库存、模拟、余量、未知字段与两份保留记录均通过。
- `gradlew.bat assemble reobfJar --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false` 成功，退出 0、22s；[构建日志](../build/town-building-final-build.log)。产物 [frostedheart-1.20.1-0.7.7.jar](../build/libs/frostedheart-1.20.1-0.7.7.jar)，14,766,244 bytes。
- 实际 10/100/500 核心的冷捕获为 0.3552/2.2818/6.6326 ms，稳定缓存捕获为 0.0879/0.2662/0.9681 ms，修改一栋后为 0.1434/0.5769/1.0730 ms。断言确认稳定时零额外建筑 Codec，修改一栋只重编该栋。采样包含 town NBT 组装与复制，非旧架构对照或完整 tick 百分位；原版强制保存同时覆盖世界其他部分。
- 本轮没有运行 JUnit；`src/test/java` 的 Java 文件数为 0。源码空白检查通过；运行日志中的故障注入异常用于验证恢复，最终通过结论来自服务器结果与退出码。
- 最终交付核对：6 份核心文档中的 56 个本地链接可解析，双语各 4 个新增文案键存在，源码空白检查退出 0。运行 JAR 包含三个新建筑基础类型、不包含建筑 GameTest 夹具和五个已删除扫描器，仍包含知识系统需要的通用扫描器；核对结果无未解决项。
- 文档影响：新增并完成 [building-data.md](../docs/town/building-data.md)，更新 [保存与运行](../docs/town/persistence-and-runtime.md)、[实现参考](../docs/town/implementation-reference.md)、[城镇模型](../docs/town/town-model.md) 与系统入口。统一计划已记录最终结果和完成状态；历史日记保持原样，只读 `design/` 与整合包未修改。

## Remaining

- 图形客户端和多客户端管理操作尚未完整执行；本次通过的是实际 Forge 服务端场景、网络数据编解码路径与独立进程文件恢复。
- 缓存内存峰值、长时扫描延迟分布和完整服务器 tick 性能矩阵尚未完整测量，不能据单次捕获样本声明规模上限或相对旧架构的性能倍数。
- 队伍与原版区块文件没有共同事务，任意进程崩溃点不属于本次全部覆盖的范围；该持久化边界保留在当前文档中。
