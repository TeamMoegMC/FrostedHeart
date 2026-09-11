# P2P 物流设备执行任务清单

- Time: `2026-08-23 22:40:50 +08:00`
- Authors: `chang; gameplay decisions`, `Codex; OpenAI GPT-5; task decomposition`
- Status: `completed`（`T00-T10` 与 `H01` completed）
- Scope: `P2P 发货终端、收货终端、双向物流终端、配对、城镇运力、物品传输、菜单与验证`
- Source plan: [`2026-08-21_01-39-26_transport-p2p-devices.md`](2026-08-21_01-39-26_transport-p2p-devices.md)
- Prerequisite: [`2026-08-22_15-55-12_town-owned-warehouse-interface-weighted-distance.md`](2026-08-22_15-55-12_town-owned-warehouse-interface-weighted-distance.md)

## Purpose and Authority

源计划拥有玩法规则、公式、交互、拓扑、容量语义和故障边界；本清单只拥有执行拆分、依赖和完成门槛。若两者冲突，
先修正本清单，不允许在实现任务中自行改变玩法。

`T00-T10` 与 `H01` 已于 `2026-08-24` 按依赖完成。任务编号统一使用 `Txx`；人工游戏内验收使用 `Hxx`。

## Task Overview

| ID | 任务 | 复杂度 | 依赖 | 主要产物 |
|---|---|---:|---|---|
| `T00` | 重新调查源码、文档、数据与构建基线（completed） | `M` | 前置 `W08` | 已验证边界、基线结果、ready 决定 |
| `T01` | 实现 P2P 公式、参数与端点类型纯模型（completed） | `M` | `T00` | 直连距离/占用、kind、边界测试 |
| `T02` | 拆分设备事实解析与通用运力准入核心（completed） | `XL` | `T01` | 设备专用入口、私有准入核心、仓库回归 |
| `T03` | 实现权威绑定状态、Codec 与原子配对状态机（completed） | `XL` | `T02` | 有向绑定、接收索引、路线牌协议、预约事务 |
| `T04` | 实现三类终端方块、方块实体与注册资源（completed） | `L` | `T03` | 方块、BE、物品、路线牌、模型骨架 |
| `T05` | 实现安全的发送方推送与公平预算调度（completed） | `XL` | `T03`, `T04` | 无突发传输核心、轮询、部分接收回退 |
| `T06` | 实现双向端八格缓存与受限物品能力（completed） | `XL` | `T04`, `T05` | 4+4 缓存、锁定模式、保存与物品安全 |
| `T07` | 实现过滤、菜单、调速、红石与玩家反馈（completed） | `XL` | `T03-T06` | 两套过滤器、相关终端页、准入反馈 |
| `T08` | 接入同步、城镇汇总、方块状态与完整生命周期（completed） | `XL` | `T03-T07` | snapshot、状态、休眠、解绑和清理 |
| `T09` | 完成自动化回归、性能审计与数据生成检查（completed） | `XL` | `T01-T08` | 完整测试矩阵、性能和资源证据 |
| `H01` | 游戏内玩法、生命周期、吞吐与 UI 验收（completed） | `L` | `T09` | 人工验收记录和必要返修 |
| `T10` | 更新 living docs、计划 Outcome 与开发日记（completed） | `S` | `H01` | 权威文档和任务收尾 |

## Architecture Boundary

以下边界属于 `T02` 的冻结完成条件，不是可选实现建议：

```text
终端 BlockEntity
├─ 暴露本地端点 ID、角色、红石和本地设备状态
└─ 发起服务端绑定/调速请求

设备专用服务入口
├─ 仓库接口：从 WarehouseTopologySnapshot 派生加权距离
└─ P2P：验证绑定双方后从两个 GlobalPos 派生直连距离

TeamTown 私有通用准入核心
└─ 使用已由服务端解析的 kind、rate 和 authoritative metric 执行准入与预约替换
```

- 终端类不得拥有城镇运力公式、决定准入结果或直接修改 `TownTransportState` 预约 Map。
- P2P 距离属于有向绑定，不属于任一单独终端；收货端多来源时每条入站连接有自己的距离。
- 网络包和其他客户端输入不得提交权威距离、距离因子或占用。内部已解析 metric 的入口必须保持私有或严格包内，不能
  形成客户端可调用的公共协议。
- `TeamTown` 不得为了事实解析依赖具体终端 BlockEntity 类型，也不得强制加载端点区块。配对发生时双方因玩家交互已经
  加载；稳态预约使用已接受绑定的固定 `GlobalPos`。
- 绑定与预约必须先完整评估，再原子提交。初次绑定失败保持未绑定；改绑失败保留旧绑定和旧预约；成功改绑才同时替换
  双方索引、稳定绑定 ID 和预约。
- 当前仓库接口行为是强制回归边界。重构后仓库加权距离、调速、拓扑刷新、禁用记录、短缺、同步和现有测试不得改变。

## Detailed Tasks

### T00: 重新调查源码、文档、数据与构建基线

- 状态：`completed`（2026-08-23）

- 重新读取源计划、前置计划 Outcome、transport living docs、最新相关 diary 和实际源码。
- 记录工作区已有差异；保留所有不属于本任务的用户文件和修改。
- 核对 `TeamTown`、`TeamTownData`、`TownTransportState`、`TransportReservationModel`、端点 Codec、snapshot、仓库接口
  生命周期和当前测试数量。
- 明确当前 `registerOrUpdateTransportEndpoint`、`refreshTransportEndpointMetric` 与
  `currentWarehouseDistance` 的仓库专用耦合，以及 P2P 必须新增的服务端事实来源。
- 运行 transport、warehouse、town 定向测试、完整测试和 `compileJava`；记录既有失败而不顺手修复无关问题。
- 核对注册 ID、模型、配方和 companion pack 影响。若任务将修改配套仓库，先读取其 `AGENTS.md` 并分别建立基线。

完成门槛：源计划假设与当前源码一致，剩余冲突有明确处理任务，源计划和本清单可提升为 `ready`。

执行结果（2026-08-23）：

- 前置 W08、living docs、最新相关 diary 与源码一致；当前仍不存在 P2P 方块、配对状态、注册 ID 或测试。
  `content.robotics.logistics` 是拥有网络核心、任务和机器人库存语义的另一套系统，本计划不复用其网络协议或持久化状态。
- `TeamTown#registerOrUpdateTransportEndpoint` 和 `refreshTransportEndpointMetric` 当前都会刷新仓库拓扑并调用
  `currentWarehouseDistance`；`TeamTownData#refreshWarehouseTopologyIfDirty` 已只批量重算 `WAREHOUSE_INTERFACE`。
  因而 T02 的设备专用事实入口与私有通用准入核心拆分可行，P2P 不得进入仓库拓扑回算链。
- `TransportEndpointId(GlobalPos)` 可继续作为每条有向预约的发送端 key；收货端多来源索引和稳定 binding ID 由 T03 的
  城镇级 P2P 状态另行持有。现有 `TransportAdmissionStatus`/`TransportReservation` 不能表达“保留设置速率、红石休眠且
  占用为 0”，现有单端点准入和 `TownTransportState` Map 变更也不能原子覆盖双向端改绑涉及的多条增删；缺口已分别
  落到 T01-T03，不阻塞 `ready`。
- 持久化 `TransportReservation.CODEC` 保存服务端派生的 `scaleMetric`，启动时通用参数重算只复用该值；P2P 必须由 T03/T08
  在加载恢复中以权威 binding `GlobalPos` 重新派生并校验距离，不能把存档缓存或 snapshot 当成事实来源。现有 transport
  snapshot 上限为 `4096` 条预约，新增 P2P 绑定/索引 Codec 也必须设置独立有界校验。
- 主仓库的注册锚点确认为 `FHBlocks`、`FHBlockEntityTypes`、`FHMenuTypes`、`FHMenuSlots` 和 `FHScreens`；类似设备的手写
  blockstate/model/item model 在 `src/main/resources`，loot/tag 在 `src/generated/resources`。现有仓库设备配方位于伴生仓库
  `TheWinterRescue/kubejs/server_scripts/src/recipes/shaped/new.js`，所以 T04 默认按同一归属添加 P2P 配方并单独报告两仓库。
  伴生仓库当前没有 `AGENTS.md`，基线只有未跟踪 `.workbuddy/`，`git diff --check` 通过。
- 构建必须显式使用 Java 17；环境原有 `JAVA_HOME` 指向 Java 8，而
  `C:\Program Files\Java\jdk-17.0.2` 可用。Java 17 基线结果：`*Transport*` 86 tests、
  `*WarehouseInterface*` + `*WarehouseLevelEmitter*` 26 tests、`content.town.*` 367 tests、完整 492 tests，全部
  `0` failure/error/skipped；完整 `test compileJava` 通过。仅观察到现有 Mixin/弃用与重复资源路径警告。
- 主仓库原有 P2P 源计划修改和未跟踪个人工具/备份文件均已保留；T00 未修改源码、living docs、生成资源或伴生仓库。
  所有发现都有后续任务承接，无剩余玩法或技术阻塞项，源计划和本清单可提升为 `ready`。

### T01: 实现 P2P 公式、参数与端点类型纯模型

- 状态：`completed`（2026-08-23）

- 增加 P2P 所需 `TransportEndpointKind`，但不把 P2P kind 直接塞进仍使用仓库距离的旧入口。
- 实现同维度直连曼哈顿距离和 `1 + 0.05 * distance` 占用公式，复用通用默认速率、最大值和 `8 ULP` 比较合同。
- 扩展纯状态模型，使红石休眠能够保留已接受设置速率和距离、同时令有效速率及预约占用为 `0`；不得与玩家把速率设为
  `0` 的 `DISABLED` 或事实不可用的 `UNAVAILABLE` 混为一类。
- 同一绑定距离恒定；只在初次绑定、改绑、恢复校验或存档迁移时从两个固定 `GlobalPos` 派生。
- 覆盖零距离、相邻、长距离、坐标溢出、非有限结果、速率边界、休眠保存/重算和双向端两条独立有向预约。

完成门槛：纯模型无 Forge/BlockEntity 依赖，公式和边界测试独立通过。

执行结果（2026-08-23）：

- 新增 `TransportEndpointKind.P2P_DIRECT_LINK`，明确表示由发送端拥有的一条同维度有向直连预约；Codec 使用稳定名称
  `P2P_DIRECT_LINK` 并继续拒绝未知类型。现有 `TeamTown#registerOrUpdateTransportEndpoint` 和
  `refreshTransportEndpointMetric` 在仓库事实解析前显式拒绝该 kind，T02 完成前不会误用 `currentWarehouseDistance`。
- `TransportReservationModel#p2pManhattanDistance` 从两个 `GlobalPos` 派生同维度三维曼哈顿距离，跨维度/缺失输入返回
  无效结果；坐标差先提升到 `long`，覆盖三轴 `Integer.MIN_VALUE -> Integer.MAX_VALUE` 而不溢出。`p2pDistanceFactor` 和
  `capacityForStoredRate` 实现 `rate * (1 + k_p2p * distance)`，继续复用通用速率边界和 `8 ULP` 准入比较。
- `TransportConsumerParameters` 新增独立 `p2pDistanceCostPerBlock`；源码默认与 Forge 配置
  `FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS.p2pDistanceCostPerBlock` 均为 `0.05`，并已进入
  `TownModelParameters`、`TownStageZeroAudit` 和阶段 4 summary 参数快照。仓库和 P2P 系数不再因恰好同值而共用一个字段。
- 新增 `TransportAdmissionStatus.REDSTONE_PAUSED`。`TransportReservation` 强制其只能用于 `P2P_DIRECT_LINK`，必须保留
  非零设置速率和有限非负距离、占用必须为 `0`；持久化/网络 Codec 往返及参数重算继续保持零占用。它与零速率
  `DISABLED`、零事实指标 `UNAVAILABLE` 具有互斥不变量。
- 失败先行阶段以 29 个缺失符号/构造器编译错误证明测试先于实现。完成后核心定向矩阵 42 tests、`*Transport*` 89 tests、
  `content.town.*` 370 tests、完整 495 tests 全部 `0` failure/error/skipped，`test compileJava` 通过；未运行 `runData`，因为
  本任务没有生成资源变化。
- living docs 已更新 `town-model.md` 和 `implementation-reference.md`，明确纯模型/配置已实现但 P2P 设备和绑定服务仍不存在；
  新增 diary [`2026-08-23_23-52-59_transport-p2p-t01.md`](../diary/2026-08-23_23-52-59_transport-p2p-t01.md)。

### T02: 拆分设备事实解析与通用运力准入核心

- 状态：`completed`（2026-08-24）

- 将当前公共预约流程拆为设备专用服务入口和不暴露给客户端的已解析准入核心。方法名可在 `T00` 后按代码风格确定，
  但职责等价于：

```text
registerOrUpdateWarehouseInterface(...)
bindOrRebindP2P(...)
admitResolvedEndpoint(...) // private or strict package-internal
```

- 仓库入口继续从 `WarehouseTopologySnapshot` 派生容量加权距离，并保留现有 topology dirty/refresh 行为。
- P2P 入口不得调用 `currentWarehouseDistance`，也不在稳态 tick 调用 `refreshTransportEndpointMetric`。
- 通用核心只负责数值校验、候选总占用、准入、结构化结果和受控预约提交，不读取 Level、仓库或具体终端类。仓库初次
  准入失败要保留现行 `rate=0` 记录，而 P2P 初次绑定失败必须不创建 binding/预约；两种服务入口须显式选择内部提交策略，
  不能让通用核心暗含其中任一种设备语义。
- 终端类只提供本地事实并发起请求；不得提供一个可被城镇层回调的“权威距离”方法。
- 为仓库现有 register/update/refresh/unregister 全状态矩阵增加或保留回归，证明重构没有改变已实现行为。

完成门槛：仓库和 P2P 使用不同事实来源、共享同一准入算法；错误 metric 不可从客户端进入；仓库全量回归通过。

执行结果：`TeamTown` 保留仓库拓扑门面并新增基于双方 `GlobalPos` 的 P2P 事务入口；通用准入只接收服务端已解析事实。
仓库接口继续只走加权仓库距离，原有注册、刷新、禁用和短缺回归全部通过。

### T03: 实现权威绑定状态、Codec 与原子配对状态机

- 状态：`completed`（2026-08-24）

- 冻结绑定权威所有者，优先使用城镇级 P2P 状态保存稳定有向连接和收货端入站索引；终端只保存本地缓存、过滤和必要
  同步视图，不能形成第二份可漂移绑定真相。
- 持久化 sender、receiver、稳定绑定 ID、已接受速率、状态和必要索引；派生距离与占用由服务端重算，不信任客户端。
- 实现发货端单目标、收货端多来源、双向端单对端及双向端互联的两条独立有向预约。
- 实现路线牌无序选择、同队伍权限、同维度、自环/不兼容拒绝、相同有向端点对后成功者覆盖和旧路线牌失效。
- 初次绑定使用通用默认速率准入；失败不保存目标。改绑沿用旧设置速率；失败原子保留旧连接。任一双向端解绑或改绑时
  对端索引和两个方向预约必须一致清理。
- 为一次连接事务增加有界的多预约 delta 评估和原子增删：双向端互联会同时增加两条有向预约，改绑还可能同时删除旧
  连接的一条或两条预约；容量准入必须基于整批候选总量，失败时 binding、索引和预约 Map 均保持原状。
- Codec 覆盖旧存档无 P2P 字段、稳定排序、重复键、部分损坏、未知类型和配置变化重算。
- 加载恢复必须从持久化 binding 的双方 `GlobalPos` 重算距离并校验端点事实，再发布 transport snapshot；不能仅调用现有
  `recalculateReservedCapacities` 复用持久化 `scaleMetric`。

完成门槛：绑定与预约没有半提交、重复连接或幽灵占用，保存/加载和全部配对状态测试通过。

执行结果：新增城镇级 `P2PBindingState`、稳定连接 UUID、有向绑定、端点索引、路线牌状态及容错 Codec；发货单目标、收货
多来源、双向双预约、覆盖、失败回退、对称解绑和加载距离重验均由同一候选事务提交并有自动化覆盖。

### T04: 实现三类终端方块、方块实体与注册资源

- 状态：`completed`（2026-08-24）

- 实现发货终端、收货终端、双向物流终端和货运路线牌的方块、BE、物品、菜单类型及注册。
- 发/收终端只与明确库存连接面相邻的物品能力主动交互；双向端不主动扫描相邻容器。
- 终端暴露稳定端点 ID、角色、红石和菜单入口，并调用 `T02/T03` 服务门面；不自行计算权威距离或写预约 Map。
- 方块形状、角色图形和基础状态遵守源计划；模型与贴图先保证角色不只依靠颜色区分。
- 依照现有仓库接口/发信器配方归属，P2P 配方默认加入 companion 仓库的
  `kubejs/server_scripts/src/recipes/shaped/new.js`；执行前再次检查该仓库是否新增 `AGENTS.md`，并按两个 Git 仓库分别验证
  和报告。任务或其他 pack 数据只有确有需要时才扩展。

完成门槛：三类终端放置、保存、交互、配对和拆除闭环通过，不强制加载其他区块。

执行结果：注册 `shipping_terminal`、`receiving_terminal`、`bidirectional_logistics_terminal`、`freight_route_card`、共用菜单和
方块实体；补齐六向方块状态、无固定顶面的模型、角色/货运站/库存连接面贴图、语言、掉落表、标签，并在 companion
`new.js` 添加四份配方。H01 返修把发/收终端的库存连接面分别改为蓝色 `shipping_terminal_port` 和橙色
`receiving_terminal_port`，含边框均严格为 6x6 像素；双向端不设开口面，六面暴露同一受限外部能力。路线牌第一次选择后
通过 `CustomModelData = 1` 切换待选模型并在 tooltip 显示端点类型、坐标和下一步提示，成功或清空后移除该模型标签。

### T05: 实现安全的发送方推送与公平预算调度

- 状态：`completed`（2026-08-24）

- 发送方主动推送，按 slot 升序选择第一个通过双方过滤的物品；收货端不主动拉取。
- 复用小数余数预算，空闲、过滤拒绝、目标满、冷却、红石或卸载期间不积累突发额度。
- 多来源收货端使用固定轮询且不提供权重；调度顺序不能由 Map、坐标或加载顺序永久决定。
- 先模拟目标接收，再按实际接受量提交来源扣减和预算；提交阶段余量立即留在或退回发送方。
- 首版只保证完整服务端调用、正常停服和区块生命周期，不实现硬崩溃恢复日志。
- 可选短冷却必须有上限，并尽量被库存、过滤、红石或连接状态变化提前唤醒。

完成门槛：目标部分接收、目标满、来源变化、多人来源、公平性、正常重启和全部受保证失败路径中无复制或吞失。

执行结果：`P2PItemTransfer` 实现模拟后提交、来源变化保护、目标部分接受和发送方恢复堆栈；小数预算不积累整件突发，
稳定 UUID/tick 调度提供多来源轮询，失败冷却上限为 5 tick。硬崩溃仍明确不在保证范围。

### T06: 实现双向端八格缓存与受限物品能力

- 状态：`completed`（2026-08-24）

- 实现四格“待发送”和四格“已接收”真实缓存，以及独立保存、同步和掉落/回收规则。
- 未锁定时外部只能插入待发送、只能提取已接收；远端只能写入已接收，P2P 只能从待发送取货。
- 与发货端绑定时锁定待发送；与收货端绑定时锁定已接收；双向端互联不锁定任何缓存。
- 非空缓存不阻止锁定或绑定。锁定缓存不向外部能力或 P2P 暴露，但菜单允许玩家手动取出遗留物品且不允许放入。
- 解绑后恢复正常能力，既有物品保持原位；任何拆除和恢复路径不得吞失、复制或错误跨缓存搬运。

完成门槛：4+4 槽能力矩阵、锁定切换、保存加载、自动化交互和物品守恒测试通过。

执行结果：`P2PTerminalBuffer` 提供四格待发送、四格已接收及分离的外部/P2P 能力视图；锁定不阻止非空绑定，菜单仍可
手动取出且禁止放入。缓存与恢复堆栈持久化，真实拆除时完整掉落，区块卸载保留。

### T07: 实现过滤、菜单、调速、红石与玩家反馈

- 状态：`completed`（2026-08-24）

- 发货/收货端使用端点终端界面，双向端使用带缓存区的独立界面；标题显示具体终端类型。两者提供“终端”和“过滤”
  子页面；收货端列出全部来源，双向端显示唯一对端及双方向状态，并在过滤页内选择输入或输出过滤。
- 发货端使用发送过滤、收货端使用接收过滤，双向端两套过滤完全独立；对端过滤只读可查。
- 空过滤放行全部；默认白名单和精确物品数据/NBT 匹配；支持黑名单及忽略数据/NBT、只匹配物品种类的模糊模式。
- 过滤名单容量、幽灵槽布局和复制操作按现有 CUI/菜单惯例确定，并覆盖最长中英文文本和滚动布局。
- 绑定成功后允许发送方输入或滚轮调整速率；滚轮复用仓库接口的 `1/8/16/64 items/s` 步长、累积和平滑限幅规则。
  初次绑定、改绑和上调失败显示 `requiredAdditionalCapacity`，并回退到权威旧状态。
- 红石信号暂停相关方向并把有效速率和占用降为 `0`；恢复后重建原预约，短缺时进入全镇比例限速。
- 显示未配对、空闲、传输、红石停机和短缺；不可用事实明确拆成“收货端无可用容器”和“对端未加载”。发货端自身没有
  来源容器只按无货处理；持续有需求的低速连接在预算间隔 tick 仍显示传输中。不使用永久跨世界光束。

完成门槛：过滤、权限、调速拒绝、红石、列表管理、本地化和菜单状态自动化通过，界面具备游戏内验收条件。

执行结果：端点终端与双向终端使用两个具体 Screen；顶层合并为“终端/过滤”，双向过滤页内选择输入/输出。标题、完整
坐标、剩余/总运力、待发货/已接收缓存标签、九格过滤、白/黑名单、精确/模糊模式、输入/按钮/滚轮调速和逐条解绑均已
接入；连接行使用完整终端名和“本终端”视角，例如“本终端->收货终端，20物品/秒”，双向互联分别列明两个方向。
三类终端的方向文字优先各占一行正常字号；单个方向超过 152 像素时拆成“来源 ->”与“目标，速率”，坐标使用最后一行，
悬停整条连接记录可查看对端过滤摘要；不再将方向文字等比缩小或允许其越出面板。双向端解除按钮位于坐标行右侧，不遮挡
英文速率单位。菜单首包前回退到客户端 BlockState 而不是
`UNBOUND` 空快照；状态以自然字号右对齐并压缩标题可用区，不再把长状态缩小。
授权成功后的视觉刷新读取已解析城镇的当前连接表，不再因打开菜单、调速或编辑过滤器而把已绑定终端短暂写成 `UNBOUND`。
滚轮与仓库接口共享 `TransportRateScroll`。红石按发送/接收端分别持久化，暂停释放预约，恢复可进入比例短缺。收货容器事实
由接收端缓存，邻居变化立即刷新并周期复核；发送端读取同一事实，避免状态抖动。持续需求在小数预算未满一件的 tick 仍
保持传输状态。

### T08: 接入同步、城镇汇总、方块状态与完整生命周期

- 状态：`completed`（2026-08-24）

- 扩展 transport snapshot 和增量同步，使 P2P 绑定、预约、设置/有效速率、距离因子、状态与过滤摘要保持服务端权威。
- 为新增 binding、入站索引和过滤摘要 Codec 设置独立数量/大小上限；现有 `TownTransportSnapshot.MAX_RESERVATIONS = 4096`
  只约束预约列表，不能被误当成所有新增 P2P 数据的通用边界。
- 镇长印章按稳定顺序显示 P2P 连接，接收端多来源和双向端两条预约不得重复或漏计。
- 未加载端点暂停实际搬运但继续占用预约，不强制加载区块；相邻容器或能力消失同样只暂停并保留绑定和预约。
- 终端方块被替换或两端队伍关系失效时自动解绑；短暂未加载不能误判为替换。
- 红石休眠、重新加载、换队、拆除、重启和双向端对称清理必须收敛到一致状态且无幽灵预约。
- BlockState 只表达有限玩家状态，避免空闲设备每 tick 标脏、写方块或发送同步包。

完成门槛：全量/增量一致、所有生命周期组合收敛、城镇汇总准确且无加载顺序抢速率。

执行结果：`TeamTownData`、`TownTransportSnapshot` 和增量同步携带有界绑定与过滤摘要；镇长印章按发送方向展示 P2P 预约。
未加载/相邻能力缺失只暂停并保留预约，已加载的方块替换、角色或队伍失配会解绑；端点索引、失败冷却及仅变更时写
BlockState 限制稳态工作量。

### T09: 完成自动化回归、性能审计与数据生成检查

- 状态：`completed`（2026-08-24）

- 覆盖源计划 Validation Outline 的公式、配对、容量、传输、缓存、过滤、生命周期、同步和性能矩阵。
- 重跑仓库接口全部回归，确认 `T02` 重构没有改变现行加权距离和预约行为。
- 对大量已加载但空闲、过滤无交集、目标满和对端未加载设备检查扫描、标脏、BlockState 写入和同步频率。
- 运行 transport/P2P 定向测试、town 包、完整测试、`compileJava` 和 `git diff --check`。
- 仅在实际新增生成资源时运行 `runData`，并检查生成前后差异。
- 若 T04 按当前归属修改 companion KubeJS 配方，额外执行该仓库 `git diff --check`，并在可用环境中完成 KubeJS 加载或
  整合包启动验证；该仓库当前没有独立测试/lint runner。

完成门槛：所有自动化零失败；无法自动观察的网络包、磁盘写入或多人项明确留给 H01 或记录为残余风险。

执行结果：Java 17 下 P2P/transport/warehouse 定向矩阵、`content.town.*` 399 tests 和完整 524 tests 均为零失败、错误或
跳过，`compileJava` 通过。`runData` 在 UTF-8 下成功，差异只含 P2P 语言、标签和三份掉落表；15 份 P2P JSON 可解析。
两仓库 `git diff --check` 通过，companion `new.js` 通过 `node --check`。静态性能审计确认冷却期不扫描物品槽、绑定索引
O(1)、空闲预算不积累整件额度、BlockState 和 transport sync 只在净变化时标记；真实多人、网络频率、磁盘写入和 UI
仍按计划留给 H01。

### H01: 游戏内玩法、生命周期、吞吐与 UI 验收

- 状态：`completed`（2026-08-24）

- `2026-08-24` 首轮实机截图已发现并返修：通用标题、三枚顶层按钮、坐标裁切、双向缓存未分组标注以及运力分母裁切。
  返修后的两个具体 Screen、过滤二级栏、滚轮调速及中英文/GUI scale 仍须重新进入游戏复核，不能仅以自动化判定完成。
- `2026-08-24` 第二轮实机截图继续发现并返修：库存连接面仍像仓库/信封面、连接方向和单位含糊、收货容器缺失及低速
  传输状态闪烁、路线牌待选阶段缺少持有反馈。自动化覆盖模型引用、路线牌模型标签与状态优先级；新纹理、长方向文案、
  容器恢复和低速持续状态仍须在游戏内复核。本轮 Java 17 完整回归为 534 tests / 139 suites，零失败、错误或跳过。
- `2026-08-24` 第三轮实机截图继续发现并返修：菜单首帧闪“未配对”、连接端名不完整且缺少本地视角、容器口过大且发/收
  未分色，以及长故障状态被缩小。新增自动化锁定空同步前哨值、发送/接收视角参数、6x6 双色端口边界和双向端六面能力；
  首帧、标题避让和实际方块纹理仍须进入游戏复核。本轮 Java 17 完整回归为 536 tests / 139 suites，零失败、错误或跳过。
- `2026-08-24` 第四轮实机反馈确认首帧修复不完整：打开菜单和提交速率仍会短暂显示“未配对”。根因是权限复核
  `claimOrAuthorize` 无条件以空城镇/空绑定参数刷新状态，服务端实际写入了一帧 `UNBOUND`；现改为读取已解析城镇的当前
  `P2PBindingState` 后刷新。GUI 测试补齐独立 Minecraft bootstrap，定向 7 tests 与完整 536 tests / 139 suites 均零失败；
  打开菜单、按钮调速、滚轮调速和过滤操作仍须在游戏内复核。
- `2026-08-24` 第五轮实机截图发现双向连接仍将两个方向压缩成一行小字。现为双向端分配一条 32 像素高的连接记录，两个
  `connection_flow` 分别按正常字号显示，坐标顺延到第三行；端点终端仍保留两条紧凑可见记录。新增双向方向顺序测试后，
  定向 8 tests 与完整 537 tests / 139 suites 均零失败；实际中文字号和三行间距仍须进入游戏复核。
- `2026-08-24` 第六轮英文实机截图发现单条完整方向仍会越出面板。双向连接记录扩至 46 像素，并按实际字体宽度在 152
  像素处选择完整行或“来源 ->”/“目标，速率”两条语义行；坐标跟随方向行，过滤预览移至右下角。中英文分段键均受资源
  测试覆盖，完整 537 tests / 139 suites 零失败；英文五行布局仍须进入游戏复核。
- `2026-08-24` 第七轮英文实机截图发现发货/收货端仍走旧的等比缩小路径，且双向端解除按钮会遮挡英文速率单位。现将正常
  字号的宽度感知语义换行统一应用到三类终端，端点连接记录扩至 32 像素；双向端解除按钮移到坐标行右侧，对端过滤摘要改为
  悬停整条连接记录查看，从而释放方向文字区域。定向测试与完整 537 tests / 139 suites 均零失败；游戏内英文布局仍须复核。

用户在第七轮返修后确认可以执行 `T10`，H01 据此完成。七轮实机反馈中发现的问题均已返修；未扩大首版硬崩溃保证范围。

- 验证三类终端外观、方向、配对顺序、改绑失败回退、逐条解绑和权限。
- 验证默认/最大/小数有效速率、短缺比例、红石释放预约、未加载保留预约和恢复无突发。
- 验证发/收相邻容器、双向端 4+4 缓存、锁定后手动回收、过滤精确/模糊模式和多来源公平性。
- 验证重登、重启、区块卸载、方块替换、换队、旧路线牌、相同端点对覆盖和外部缓存中继。
- 检查菜单在常用 GUI scale 和中英文下无重叠、裁切或状态歧义。

完成门槛：所有可执行场景记录实际结果；任何复制、吞失、幽灵绑定、权限绕过或汇总不一致都回到对应任务修复。

### T10: 更新 living docs、计划 Outcome 与开发日记

- 状态：`completed`（2026-08-24）

- 新增或更新 P2P 玩家系统 living docs，记录三类设备、配对、公式、缓存、过滤、生命周期、权限和故障保证范围。
- 更新源计划与本清单的实际状态、偏离、验证结果和残余风险。
- 按 `diary/README.md` 新增完成记录；不改写既有历史。
- 若 companion 仓库有改动，分别记录其文件、命令和 Git 验证结果。

完成门槛：源码、living docs、计划、任务清单和 diary 对当前实现描述一致。

执行结果：[`docs/town/p2p-logistics.md`](../docs/town/p2p-logistics.md) 升为 Current，
[`docs/town/README.md`](../docs/town/README.md) 同步索引状态；源计划与本清单记录最终实现、H01 七轮返修、
537 tests / 139 suites 回归和首版残余边界；新增
[`T10` 收口 diary](../diary/2026-08-24_23-17-00_transport-p2p-t10-closeout.md)。companion `new.js` 的四份配方保持不变，
该仓库 `git diff --check` 与 `node --check kubejs/server_scripts/src/recipes/shaped/new.js` 均通过。

## Validation Commands

`T00` 已按当前测试类名确认下列层级；后续任务新增类名时可以扩展过滤条件，但不能降低覆盖范围。命令须确保 Gradle
实际使用 Java 17，不能沿用当前机器指向 Java 8 的环境 `JAVA_HOME`：

```powershell
.\gradlew.bat test --tests "*Transport*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*P2P*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain
.\gradlew.bat test --offline --no-daemon --console=plain
.\gradlew.bat compileJava --offline --no-daemon --console=plain
git diff --check
```

## Outcome

状态为 `completed`。`T00-T10` 与 `H01` 已完成：P2P 公式、权威绑定、三类终端、路线牌、安全传输、4+4 双向缓存、两套过滤器、独立
端点/双向 GUI、仓库一致的滚轮调速、红石、同步、生命周期、生成资源和 companion 配方均已落地。H01 七轮实机反馈发现的
GUI、开口材质、连接文案、稳定状态、首帧同步、授权交互状态、状态字号和路线牌待选反馈均已返修，用户已确认进入 T10
收口。最终 Java 17 回归为 537 tests / 139 suites，零失败、错误或跳过。
首版残余边界为同维度、一对一发送、无频道/广播/内建中继、无来源权重及无硬崩溃恢复日志；这些均为已确认范围限制。
