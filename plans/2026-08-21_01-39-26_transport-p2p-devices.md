# P2P 物流设备与运力接入后续计划

- Time: `2026-08-21 01:39:26 +08:00`
- Last verified: `2026-08-22`
- Authors: `chang; gameplay direction`, `Codex; OpenAI GPT-5; investigation and planning`
- Status: `draft`
- Scope: `未来 P2P 输入/输出设备、配对、传输安全、距离成本与城镇运力接入`
- Depends on: [`2026-08-22_15-55-12_town-owned-warehouse-interface-weighted-distance.md`](2026-08-22_15-55-12_town-owned-warehouse-interface-weighted-distance.md)
- Code anchors to recheck: `TransportEndpointId`, `TransportEndpointKind`, `TransportEndpointRequest`,
  `TransportReservationResult`, `TownTransportState`, `TownTransportSnapshot`, `TeamTown`,
  `WarehouseInterfaceBlockEntity`, `content.robotics.logistics`

## Goal

在仓库接口运力预约闭环完成后，单独确定并实现 P2P 输入端与输出端的玩家交互、配对关系、物品传输、距离成本、
生命周期和故障安全。P2P 的工作量和未决设计都明显大于“增加一种 endpoint kind”，因此不作为仓库接口首版的
附带任务。

本文件是后续调查入口，不授权直接照此实现。开始开发前必须重新核对当时的源码、注册、数据、living docs 和人类
设计，并把状态从 `draft` 更新为 `ready`。

## Verified Current State

- 截至 `2026-08-22`，当前源码和测试中没有 P2P 输入端、输出端、配对协议、方块注册或界面。
- `content.robotics.logistics` 是已有的机器人物流网络，不能未经设计确认就当作 P2P 的实现基础或兼容协议。
- 前置仓库计划已经提供稳定端点 ID、结构化预约结果、城镇汇总、比例限速和全量/增量 snapshot 同步基础。
  `TransportEndpointRequest` 已不再接受客户端距离或绑定核心，但 `TeamTown#registerOrUpdateTransportEndpoint` 当前仍通过
  `WarehouseTopologySnapshot` 派生仓库接口距离，尚不是可直接接入 P2P 的通用 metric resolver。
  `TransportEndpointKind` 当前只登记 `WAREHOUSE_INTERFACE`，P2P 仍需新增 kind、服务端事实来源和对应公式分发。
- 因此本计划目前不冻结类名、注册 ID、配方、模型、菜单布局、配对工具或网络拓扑。

## Entry Conditions

以下条件全部满足后才进入实现：

1. 前置仓库设备计划完成 `W08`，其 Outcome 已记录实际 API、公式、存档和验证结果。
2. 当前源码中通用运力 API 已重新核对，P2P 不需要绕过 `TeamTown` 直接改预约 Map。
3. 玩家交互和网络拓扑的未决项已形成明确决策，并写入本计划的 Frozen Gameplay Rules。
4. 方块、物品、菜单、配方或数据文件的实际改动范围已确认；若涉及配套整合包仓库，先读取其 `AGENTS.md` 并
   分别验证两个 Git 仓库。

## Frozen Capacity Decisions

以下是从仓库接口讨论中继承的已定基础。若最终拓扑无法直接表达这些规则，必须把冲突重新列为设计决策，不能在
实现中静默改写：

- 运力表示持续占用的服务能力，不是每搬一件就扣除的库存；不跨日囤积。
- 零距离下传输 `1 item/s` 占用 `1` 运力，不引入参考速率 `R_ref`。
- 权威占用保留有限 `double`，不增加 `Q` 或 `ceil(raw / Q) * Q` 量化层；复用前置运力模型的局部 `8 ULP`
  容差比较，不复用 `TeamTownResourceHolder.DELTA`，也不修改权威数值。
- 设备只保存一个服务端已接受的 `rateItemsPerSecond`；已有端点上调失败时整条旧预约不变，新端点失败时建立
  `rate=0` 的禁用记录。供给短缺不改设置速率，而是统一按全镇比例计算有效速率。
- 红石停机不修改服务端已接受的设置速率或绑定，但使受影响连接暂时休眠：有效速率和占用运力均为 `0`。红石恢复
  后按原设置速率恢复预约；若此时全镇运力不足，进入统一的比例短缺，不把设置速率静默降低。
- 非整数 `items/tick` 复用前置计划验证后的小数余数模型，不因设备空闲积累整数突发额度。
- 默认请求速率、默认最大值及配置来源优先复用通用 `TransportConsumerParameters`；P2P 如需不同边界，必须给出
  玩法理由和独立测试。

## Direct-link Formula

同维度、任意一个发送端点直连一个接收端点时，每条有向连接的距离与占用公式冻结为：

```text
manhattanDistance = abs(senderX - receiverX)
                  + abs(senderY - receiverY)
                  + abs(senderZ - receiverZ)
distanceFactor = 1 + 0.05 * manhattanDistance
reservedTransportCapacity = rateItemsPerSecond * distanceFactor
```

这使距离每增加一格只增加基础占用的 `5%`，不会指数增长，也没有参考速率或向上量化。该公式只有在“单条链路、
同维度、两端位置明确”时定义完整。多来源收货端对每条入站连接分别应用公式；双向端互联对两个方向分别应用公式。
跨维度、中继或共享链路必须先确定怎样计距和由谁占用，不能用伪造坐标差静默代替。

## Frozen Device and Topology Decisions

以下规则已经完成人类玩法确认。它们描述预期行为，不代表设备已经实现；P00 仍须按 Entry Conditions 重新核对源码和
数据边界。

### Device forms and names

- 玩家可见名称使用“发货终端”“收货终端”和“双向物流终端”，避免使用视角不明确的“输入端”“输出端”。代码内部
  可以继续使用 input/output 等方向术语，但玩家反馈必须明确写出发送方和接收方。
- 三类终端均表现为一格的复古工业货运设备，共用铆接钢板、线缆盒、仪表和状态灯等基础外观，并符合项目既有的
  `16x16` 像素贴图与沉浸工程式复古未来风格。
- 发货终端使用内凹装货口、汇聚箭头或压入式滚轮表达发送；收货终端使用外凸卸货槽、发散箭头或短传送带表达接收。
  角色差异必须同时依靠轮廓和方向图形，不能只依靠颜色。
- 发货终端和收货终端各有一个明确的库存连接面，分别自动从相邻物品能力取货、向相邻物品能力交货。双向物流终端
  不主动与任何相邻容器交互，只向漏斗、管道等外部自动化暴露受限的物品能力。
- 世界中不显示跨越整条链路的永久光束。配对时可以临时显示端点连线；实际传输只在两端播放短暂机械动作、灯光或
  少量粒子。
- 方块状态至少要区分未配对、已配对空闲、正在传输、红石停机、运力短缺和目标不可用。最终颜色、动画和模型资源
  在 P06 确定，但角色图形与运行状态图形不得互相替代。

### Order-independent route-card pairing

- 配对工具暂称“货运路线牌”。路线牌记录两个被玩家分别右击的端点，点击顺序不决定传输方向；方向完全由两端类型
  按下表推导。
- 第一次点击把稳定端点位置和已知类型写入路线牌。第二次点击后，服务端重新读取两个位置并统一验证方块类型、城镇、
  队伍、权限、维度规则、距离和运力准入，不能信任路线牌保存的类型、距离或占用。
- 配对成功后清空路线牌的待选端点，并把这次成功连接的稳定标识写回路线牌。路线牌对空气或无关方块右击时解除它
  当前记录的连接；如果该连接已经被后来操作覆盖，旧路线牌只能报告连接已失效，不能误删替代它的新连接。只有一个
  待选端点而尚未形成连接时，对空气或无关方块右击只清除本次选择。
- 配对失败时不修改任何既有绑定或预约，并保留可继续使用的第一次选择，同时向玩家说明失败原因。
- 同一个端点不能与自身配对。两个发货终端或两个收货终端互不兼容，服务端必须拒绝建立连接。

| 端点组合 | 建立的方向 |
|---|---|
| 发货终端 + 收货终端 | `发货终端 -> 收货终端` |
| 发货终端 + 双向物流终端 | `发货终端 -> 双向端已接收格` |
| 收货终端 + 双向物流终端 | `双向端待发送格 -> 收货终端` |
| 双向物流终端 + 双向物流终端 | 同时建立两个相反方向 |
| 发货终端 + 发货终端 | 不兼容 |
| 收货终端 + 收货终端 | 不兼容 |

### Binding cardinality and ownership

- 一个发货终端最多拥有一个目标；改绑属于替换该发货终端的唯一出站连接。
- 一个收货终端可以同时接受多个发货终端。每个发货终端分别拥有自己的连接、设置速率和唯一运力预约；收货终端只
  维护入站连接索引，不为同一传输重复预约。
- 双向物流终端首版最多绑定一个对端，不能同时加入多条连接。该限制避免它同时进入互相冲突的缓存锁定模式，也把
  多目标路由、广播和汇聚留在后续独立设计中。
- `发货终端 -> 收货终端` 和 `发货终端 -> 双向端` 的预约由发货终端拥有；`双向端 -> 收货终端` 的预约由双向端
  拥有。任何拆除、改绑和失效清理都必须由同一个稳定预约键完成，不能由接收方再登记一份占用。
- 一个收货终端存在多个可发送来源时，传输调度必须使用可持久化或可确定恢复的轮询游标，避免端点坐标、区块加载或
  Map 遍历顺序永久偏袒某条连接。
- 收货终端界面必须列出全部来源及各自坐标、设置速率、有效速率和状态，并允许逐条解除；发货终端只显示自己的唯一
  目标。
- 创建、改绑和解除连接的权限属于城镇所属队伍的所有成员，不再额外限定镇长或终端放置者。
- 多人竞争以服务端实际处理顺序为准：最后一个成功通过验证的绑定操作覆盖同一发货端或双向端的旧绑定；验证失败的
  后续操作不得破坏仍有效的旧连接。向同一收货端增加不同来源属于追加，不覆盖其他发货端已有的入站连接。
- 双向端的单对端绑定必须在双方保存相互一致的索引。任意一方改绑、主动解绑、被拆除或因生命周期规则失效时，另一
  方同步解除旧索引；双向端互联时还要原子注销两个方向的预约，不能留下单边绑定或幽灵占用。

### Bidirectional-terminal buffer contract

- 双向物流终端恰好有两个单格缓存，玩家可见名称为“待发送”和“已接收”。“待发送”只接受玩家或外部物流插入，
  不允许玩家或外部物流提取；“已接收”只允许玩家或外部物流提取，不接受玩家或外部物流插入。
- 远端送达的物品只能进入“已接收”，绝不能重新进入“待发送”或被自动回送。目标不能接受时保留来源侧物品并形成
  背压，不允许因缓存已满复制、吞失或丢出物品。
- 双向端与发货终端绑定时充当接收方并锁定“待发送”；该格非空时拒绝绑定。双向端与收货终端绑定时充当发送方并
  锁定“已接收”；该格非空时拒绝绑定。解除连接后恢复两个格子的正常外部能力。
- 两个双向端互相绑定时都不锁定缓存。每一端的“待发送”自动传到另一端的“已接收”，两端分别相当于对方的发货端。
- 双向端互联在权威模型中是两条有向连接，不是一条隐藏的共享带宽。A、B 分别保存自己的出站设置速率并分别拥有
  预约；同距离下总占用为 `(rateA + rateB) * distanceFactor`。任意一端把速率设为 `0` 即可得到实际上的单向传输，
  但不会锁定两个缓存。

### Redstone, dimensions, menus, and filters

- 首版中，终端收到红石信号时进入停机。发货端暂停唯一出站连接，收货端暂停全部入站连接，双向端暂停与该设备有关
  的所有发送和接收方向；连接和设置速率保留，但这些休眠方向的有效速率和占用运力都是 `0`。
- 首版只允许同维度端点配对。服务端在建立和恢复连接时都要验证双方维度相同；不定义跨维度距离、附加成本或升级。
- 三类终端菜单都增加“相关终端”和“物品过滤”子页面。“相关终端”显示本地全部连接、对端位置、连接状态、速率及
  对端过滤内容，并提供与权限一致的逐条解除操作；收货端在此列出全部来源。
- 发货端配置发送过滤，收货端配置接收过滤，双向端分别配置发送过滤和接收过滤。一件物品只有同时通过该有向连接的
  发送方过滤与接收方过滤才允许移动；对端过滤在“相关终端”中只读展示，修改仍须操作拥有该过滤器的终端。
- 过滤器的默认模式、名单容量、标签或 NBT 匹配粒度以及复制操作仍待 P01/P06 确定。

## Design Questions to Freeze

### Device and topology

- 多来源收货端是否需要来源优先级，以及轮询公平性是否允许玩家配置权重。
- 首版是否明确禁止中继、频道、广播和循环网络；后续若允许，怎样阻止重复链路和不可审计的传输环。

### Transfer semantics

- 谁主动推送或拉取，单 tick 内如何选择物品，过滤条件和目标已满时如何处理。
- 传输是否经过可见缓冲；服务器崩溃、区块卸载或 Action 部分成功时物品由谁持有。
- 两端只有一端加载、目标方块被替换、维度未加载或队伍改变时，是暂停、解绑还是报错。
- 单 tick 内如何在多来源收货端执行已冻结的公平轮询，并同时正确处理双方过滤器、目标剩余空间和各连接独立预算。
- 设备空闲后恢复时不产生突发，并证明任意失败路径都不会复制或吞失物品。

### Capacity ownership

- 单条有向连接只创建一条预约；实现时仍需冻结预约键、休眠表达和跨双方原子注销的具体结构。
- 距离或绑定变化属于事实刷新还是玩家上调；何时允许它直接造成全镇短缺。
- 配对请求失败时是否保存未生效目标，以及玩家如何看到所需新增运力和旧连接是否仍运行。
- 若以后引入频道或多目标发送，必须先定义可审计的逐链路或逐设备占用模型。

## Proposed State Contract

已经实现且 P00 必须优先复用的通用前置合同：

```text
TransportEndpointId
└─ GlobalPos endpointPos

TransportEndpointRequest
├─ TransportEndpointId endpointId
├─ TransportEndpointKind endpointKind
└─ int rateItemsPerSecond

TransportReservation (persistent authority)
├─ TransportEndpointKind endpointKind
├─ int rateItemsPerSecond
├─ double scaleMetric             // server-derived fact
└─ TransportAdmissionStatus

TransportReservation.SNAPSHOT_CODEC
└─ adds double reservedTransportCapacity

TransportReservationResult
├─ decision
├─ optional reservationAfter
├─ TownTransportSummary townSummaryAfter
└─ requiredAdditionalCapacity

TeamTown
├─ registerOrUpdateTransportEndpoint(...)
├─ refreshTransportEndpointMetric(...)
└─ unregisterTransportEndpoint(...)
```

`TransportEndpointRequest` 是不可信设置输入，不能重新加入距离、对端坐标或占用字段。当前
`registerOrUpdateTransportEndpoint`/`refreshTransportEndpointMetric` 会调用仓库拓扑专用的
`currentWarehouseDistance`，`TeamTownData#refreshWarehouseTopologyIfDirty` 也只批量重算 `WAREHOUSE_INTERFACE`。
因此 P2P 的 `P00/P02` 必须先增加按 `TransportEndpointKind` 选择的服务端事实解析和占用公式；在该边界完成前，不能把
P2P kind 塞进现有入口并误用“接口到全镇仓库的加权距离”。删除连接只按稳定 `TransportEndpointId` 调用
`unregisterTransportEndpoint`；按单仓核心批量注销的 API 已不存在。

未来 P2P 自身的持久化类型和字段仍待 P01/P03 确定。其逻辑至少要表达已冻结的有向连接、双向端缓存模式和多来源
收货索引，但不能让客户端提交权威距离或占用：

```text
P2PDirectedBinding
├─ stable binding ID
├─ sender GlobalPos
├─ receiver GlobalPos
├─ rateItemsPerSecond
├─ distanceMetric
├─ active/dormant transport state
└─ admissionStatus

P2PReceiverIndex
└─ incoming sender endpoint IDs

BidirectionalTerminalState
├─ pendingSend ItemStack
├─ received ItemStack
├─ send filter
├─ receive filter
└─ peer endpoint ID

P2PRouteCardState
├─ optional pending first endpoint
└─ optional stable binding ID
```

每条有向连接只由发送端点拥有唯一预约，接收端点不重复预约；双向端互联因此登记两个相反方向的预约。Map 仍属于
具体 `TeamTownData`；绑定双方必须属于同一城镇，且服务端要重新验证端点方块类型和队伍。任何客户端提交的距离或
占用都只能视为请求参数，权威值必须由服务端位置和统一公式生成。

## Implementation Sequence

| ID | 任务 | 依赖 | 完成标准 |
|---|---|---|---|
| `P00` | 重新调查源码、数据、文档和前置计划 Outcome | 前置 `T13` | 当前基础、复用边界和冲突均有可搜索记录 |
| `P01` | 冻结设备交互、拓扑、权限、跨维度和故障语义 | `P00` | 本计划升为 `ready`，无阻塞玩法问题 |
| `P02` | 接入距离/占用公式、配置和纯模型 | `P01` | 基准算例、边界、溢出和准入测试通过 |
| `P03` | 实现方块、方块实体、注册、Codec 与配对协议 | `P02` | 放置、配对、改绑、保存和同步闭环通过 |
| `P04` | 实现有预算的安全物品传输 | `P03` | 部分成功、目标满、卸载和重启不复制或吞失 |
| `P05` | 接入城镇预约、比例限速和全生命周期清理 | `P04` | 无重复、幽灵占用或加载顺序抢速率 |
| `P06` | 完成菜单、状态反馈、模型和玩家操作保护 | `P05` | 设置/有效速率、距离、占用和错误一致 |
| `P07` | 自动化、性能与游戏内验收 | `P06` | 完整回归通过，无空闲 tick 忙等或同步风暴 |
| `P08` | 更新 living docs、计划 Outcome 与开发日记 | `P07` | 当前行为和剩余工作具有权威记录 |

## Validation Outline

- 公式：距离 `0`、相邻、长距离、速率边界、相差 `1/8/9 ULP`、非有限数和乘法溢出。
- 配对：无序点击、重复操作、自环、目标替换、改绑、路线牌精确解除、旧牌不误删新连接、不同队伍、跨维度、队伍
  外玩家和同 tick 竞争操作。
- 生命周期：双方加载/卸载的四种组合、拆除任一端、换队、重启和存档升级。
- 安全：来源扣除与目标插入的原子边界、部分接受、目标满、异常中断、发送/接收过滤交集变化和循环网络。
- 吞吐：默认速率、`1280 items/s`、短缺小数比例、多个链路公平性、空闲后恢复无突发；红石停机时连接保留但有效
  速率和占用均为 `0`，恢复后重建占用并正确进入比例短缺。
- 同步与反馈：服务端拒绝篡改值，菜单、方块状态和镇长印章描述同一权威状态。
- 性能：大量已加载但空闲设备不持续扫描库存、标脏或发送网络包。

## Documentation Impact

实现时根据最终归属更新城镇运力 living docs，并为 P2P 玩家系统增加或更新相应系统文档。若改动配套整合包中的
配方、任务、KubeJS、数据包或配置，必须在其仓库单独记录验证。完成后新增 diary，写明设备语义、公式、生命周期、
物品安全验证和仍未解决的拓扑限制。

## Outcome

状态为 `draft`。前置仓库设备计划 W08 已完成，稳定端点、单速率请求、结构化结果和 snapshot 合同已按当前源码更新；
当前 `TeamTown` metric 派生仍是仓库接口专用，P2P 必须先实现 kind-specific 服务端事实解析。P2P 设备、
距离成本和传输仍未实现。玩法讨论已经冻结三类终端的表现、无序路线牌配对、兼容矩阵、发货端单目标、收货端多来源、
双向端单对端与双缓存语义、逐方向预约和公平轮询。现已进一步冻结全队成员权限、服务端后成功者覆盖、双向绑定对称
解除、路线牌精确解绑、红石休眠释放运力、仅同维度、相关终端/过滤子页面及发送与接收双重过滤。中继、频道、广播、
循环网络、来源优先级和完整故障语义仍待 P01 决定。下一步是执行 `P00`，重新核对上述复用边界与当时源码，再完成
P01 并将本计划升为 `ready`；不能直接按当前草案编码。
