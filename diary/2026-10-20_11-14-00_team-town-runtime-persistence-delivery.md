# 队伍与城镇基础架构交付、真实游戏验收及计划清理

- Time: `2026-10-20 11:14:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and validation`
- Status: `completed`（基础版代码与本次真实服务端验收完成；未覆盖范围明确保留）
- Scope: `Chorda 队伍身份/组件、玩家与 AI 城镇运行及保存、管理同步、旧档迁移；按用户要求删除全仓库 JUnit`

## Completed

- 复查并修复统一计划中旧文档链接、进度冲突、测试时机、业务提交前后的回滚边界和保存准备修改事实的矛盾；补齐独立性能输入说明。删除已合并的 5 份旧计划/重复复查文件。修正 7 篇历史日记的链接目标，保留其历史正文。
- `COOKIES` 成为 `SCENARIO_COOKIES` 的同实例别名。队伍查询区分内部 holderId 和外部 teamId，修复同队转移删除自身及陈旧外部映射。真实重启发现 FTB Teams 2001.3.0 的 `getTeamByID` 对不存在项抛空指针，提供者改用其 map/集合解析，暂不可用的外部身份继续保存在内部记录。
- 组件加载/新建/替换/删除收拢到 `DataHolderMap` 安装入口。服务器持久化绑定按 holder 实例显式安装，客户端默认无绑定。编码失败不输出缺失组件的记录；保留未知根和组件数据。修复两条 TeamCommand set 分支丢弃解码结果的问题。
- 重写 `ObservableTownMap`，默认修改方法及 entry/key/value 视图都经过提交后的通知。核心 town 和模拟追踪所属记录版本；历史、居民集合和熟练度查询不暴露直接修改的结构视图。住房/岗位 getter 不再承担规范化。
- 分离经营日编排、分配算法、运输准入/配对、同步输出和客户端通知。历史、townDay 与自动完成标记共同提交；通知异常不重放经济。单镇重入受控，组件生命周期在操作边界应用。
- 自动经营使用一次主世界气候时钟采样、holderId 稳定相位、持久尝试水位和运行观察值。新建/旧档 pending 来源可跨保存保留；手动推进不消耗自动窗口，离线不补历史。
- 新的 TEAM 记录包含 data.town 与 townSim，AI 独立文件；未托管居民维持原版每维度 SavedData。共享 Util.ioPool 上的单 drain 连续处理冻结快照，队列有限、同记录待写版本合并，成功回执才确认保存。替换关系先由 keeper 的实际成功写入确认，再允许旧记录退休。
- 保留原版保存 HEAD 的研究事件，普通捕获与 RETURN 强制等待各自只执行一次；失败合并到原版保存结果，原版世界保存仍执行。停止准备快照与最终排空由 Chorda 统一负责，reset 在排空后发生。
- 管理同步增加 PENDING/PRESENT/ABSENT、每连接完整基线、重同步请求与退避。居民/建筑包在构造时编码，旧连接延迟包不能安装进新连接视图。FH 协议版本追加 `:town-view-1`；新增消息在原注册序列末尾追加。
- 模拟直接恢复最终数组，批次对账使用临时 UUID→simId 索引。真实读档发现显式接管缺少活跃列表初始化，已将注册表、活跃列表和空间索引共同恢复；保留现有 20 tick 注册表检查。
- 按用户要求删除全仓库原有 174 份 JUnit 源码及 Gradle 依赖；本次曾准备的 3 份新增 JUnit 也已删除且从未运行。系统拒绝直接递归清理生成物后，改用 Gradle 的 `cleanTest cleanCompileTestJava cleanProcessTestResources` 成功清理相应输出。

## Decisions

- 最小复杂度的基础版采用一队一份完整记录：经营和模拟在内存中仍各自拥有事实，以居民 UUID 关联；保存时一起捕获。模拟独自变化仍需压缩/替换完整队伍文件，缓存只节省未变化部分的编码。
- 研究、知识、generator 等组件暂用每次捕获并比较冻结 NBT 的兼容路径，避免不完整通知漏写。它们未变化时不重复写文件，但仍会编码，不能宣称全组件零编码。
- `TownTransportService` 放在城镇核心包，保持数据包内访问；没有为了源码目录层次增加公开 setter。结构修复采用合并标志与待重绑设备集合，不新建通用任务系统。
- 加载仍为启动读取/解码。取消周期扫描、细分视图、进一步算法优化与提高保存并发度都按真实热点再决定。
- 测试运行目录使用 `-PgameTestDirectory=build/team-town-gametest*`，复用当前仓库构建产物。未复制源码重建，也未修改 design 或整合包。

## Validation

- Java 17 运行 Gradle Wrapper 8.1.1；生产源码和 GameTest 编译通过，`assemble` 及 `reobfJar` 通过，制品为 [frostedheart-1.20.1-0.7.7.jar](../build/libs/frostedheart-1.20.1-0.7.7.jar)。
- 最终统一命令：`assemble -PgameTestDirectory=build/team-town-gametest-functional -PgameTestNamespaces=frostedheart_persistence,frostedresearch_knowledge_login runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`。结果：11 个真实 Forge GameTest 全部通过，正常停服完成；[完整日志](../build/team-town-final-validation.log)。
- 场景覆盖真实保存中的最新修改、经营/模拟同文件、干净记录不重写、显式移除不复活、组件替换和旧对象解绑、结算后通知异常与重入、队伍转移/退休、I/O 失败后的原版保存及恢复、AI 档案/模拟独立记录、真实玩家登录与自动/手动/离线窗口、原版 save-off/save-on/save-all flush、generator 移除/重装，以及组件绑定失败恢复原对象/模拟。
- 独立进程验证分别运行 `teamTownPersistencePhase=seed/migrate/read`，namespace 为 `frostedheart_persistence_restart`；migrate/read 使用 `-x cleanResearchGameTestWorld`。从实际游戏创建的经营/模拟数据生成旧文件外壳，迁移阶段确认起点确为旧格式，再次重开读取 V1。玩家与 AI 居民、库存、模拟、lastAIId、未知字段及成功后的旧来源清理均通过。日志：[seed](../build/team-town-restart-seed.log)、[migrate](../build/team-town-restart-migrate.log)、[read](../build/team-town-restart-read.log)。
- 最终代码额外执行 `cleanTest cleanCompileTestJava cleanProcessTestResources` 和独立 read，2026-10-20 结果通过；[日志](../build/team-town-final-restart-read.log)。未执行 JUnit。
- 早期复现的坏住宅夹具、缺失外部 FTB 队伍和首次模拟活跃列表问题已经修复并在真实游戏中重跑。共用 GameTest 世界被其他热系统任务占用的启动失败不计通过，改为独立运行目录后重新生成、迁移、读取。
- 文档影响：新增 [保存与运行边界](../docs/town/persistence-and-runtime.md)，更新 town 实现、模拟、模型、运输和入口；更新研究及字体文档对已删除 JUnit 的说明。历史气候工作与只读 design 未改。

## Remaining

- 图形客户端完整视图恢复、多客户端同时换队、多维度可见性、同 JVM 连续打开不同世界及 100/1000/10000 人长时性能矩阵尚未逐项执行；本次通过的是实际 Forge 服务端和跨进程文件恢复，未把未覆盖项宣称为已通过。
- 少数组件保留兼容全量编码，20 tick 模拟注册表检查仍在；没有据本次验收声明零编码、人数容量或性能提升倍数。
- 当前行为以源码和 docs 为准；[统一计划](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) §17 记录本次实际交付与条件延期。
