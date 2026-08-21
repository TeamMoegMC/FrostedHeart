# P2P 物流设备与运力接入后续计划

- Time: `2026-08-21 01:39:26 +08:00`
- Last verified: `2026-08-22`
- Authors: `chang; gameplay direction`, `Codex; OpenAI GPT-5; investigation and planning`
- Status: `draft`
- Scope: `未来 P2P 输入/输出设备、配对、传输安全、距离成本与城镇运力接入`
- Depends on: [`2026-08-20_16-53-08_transport-capacity-consumers.md`](2026-08-20_16-53-08_transport-capacity-consumers.md)
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
  `TransportEndpointKind` 当前只登记 `WAREHOUSE_INTERFACE`，P2P 仍需新增 kind 并定义自身事实校验。
- 因此本计划目前不冻结类名、注册 ID、配方、模型、菜单布局、配对工具或网络拓扑。

## Entry Conditions

以下条件全部满足后才进入实现：

1. 前置计划完成 `T13`，其 Outcome 已记录实际 API、公式、存档和验证结果。
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
- 非整数 `items/tick` 复用前置计划验证后的小数余数模型，不因设备空闲积累整数突发额度。
- 默认请求速率、默认最大值及配置来源优先复用通用 `TransportConsumerParameters`；P2P 如需不同边界，必须给出
  玩法理由和独立测试。

## Direct-link Formula

同维度、一个输入端直连一个输出端时，距离与占用公式冻结为：

```text
manhattanDistance = abs(inputX - outputX)
                  + abs(inputY - outputY)
                  + abs(inputZ - outputZ)
distanceFactor = 1 + 0.05 * manhattanDistance
reservedTransportCapacity = rateItemsPerSecond * distanceFactor
```

这使距离每增加一格只增加基础占用的 `5%`，不会指数增长，也没有参考速率或向上量化。该公式只有在“单条链路、
同维度、两端位置明确”时定义完整；跨维度、多目标、中继或共享链路必须先确定怎样计距和由谁占用，不能用伪造
坐标差静默代替。

## Design Questions to Freeze

### Device and topology

- 输入端和输出端各自面对什么库存能力，方向能否配置，红石如何门控。
- 配对是一对一、一对多、多对一，还是由频道形成网络；一条链路由哪一端拥有稳定 ID。
- 配对动作使用菜单、手持工具、频道号还是其他交互；谁有权限创建、改绑和解除。
- 是否只允许同维度配对；跨维度若允许，固定附加成本、独立升级条件和失联行为是什么。
- 是否允许中继、过滤、优先级、循环连接，以及如何阻止自环和重复链路。

### Transfer semantics

- 谁主动推送或拉取，单 tick 内如何选择物品，过滤条件和目标已满时如何处理。
- 传输是否经过可见缓冲；服务器崩溃、区块卸载或 Action 部分成功时物品由谁持有。
- 两端只有一端加载、目标方块被替换、维度未加载或队伍改变时，是暂停、解绑还是报错。
- 输入和输出是否共享一条 rate budget；若多目标存在，预算如何公平分配且不随遍历顺序偏置。
- 设备空闲后恢复时不产生突发，并证明任意失败路径都不会复制或吞失物品。

### Capacity ownership

- 单条连接只创建一条预约，避免输入端和输出端对同一传输重复占用；预约键及注销责任必须唯一。
- 距离或绑定变化属于事实刷新还是玩家上调；何时允许它直接造成全镇短缺。
- 配对请求失败时是否保存未生效目标，以及玩家如何看到所需新增运力和旧连接是否仍运行。
- 多目标或频道网络若无法归约为单条连接，需先定义可审计的逐链路或逐设备占用模型。

## Proposed State Contract

已经实现且 P00 必须优先复用的通用前置合同：

```text
TransportEndpointId
└─ GlobalPos endpointPos

TransportEndpointRequest
├─ TransportEndpointId endpointId
├─ TransportEndpointKind endpointKind
├─ GlobalPos boundWarehouseCorePos
├─ int rateItemsPerSecond
└─ double scaleMetric

TransportReservationResult
├─ decision
├─ optional reservationAfter
├─ TownTransportSummary townSummaryAfter
└─ requiredAdditionalCapacity

TeamTown
├─ registerOrUpdateTransportEndpoint(...)
├─ refreshTransportEndpointMetric(...)
├─ unregisterTransportEndpoint(...)
└─ unregisterTransportEndpointsBoundTo(...)
```

未来 P2P 自身的绑定状态仍待 P01 冻结，可从以下最小形状开始讨论，但不能让客户端提交权威距离或占用：

```text
P2PBinding
├─ owner/input GlobalPos
├─ target/output GlobalPos
├─ rateItemsPerSecond
├─ distanceMetric
└─ admissionStatus
```

若采用一对一拓扑，暂定由输入端拥有唯一预约，输出端不重复预约。Map 仍属于具体 `TeamTownData`；绑定双方必须属于
同一城镇，且服务端要重新验证端点方块类型和队伍。任何客户端提交的距离或占用都只能视为请求参数，权威值必须
由服务端位置和统一公式生成。

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
- 配对：重复操作、自环、目标替换、改绑、解除、不同队伍、跨维度和无权限玩家。
- 生命周期：双方加载/卸载的四种组合、拆除任一端、换队、重启和存档升级。
- 安全：来源扣除与目标插入的原子边界、部分接受、目标满、异常中断、过滤变化和循环网络。
- 吞吐：默认速率、`1280 items/s`、短缺小数比例、多个链路公平性、空闲后恢复无突发。
- 同步与反馈：服务端拒绝篡改值，菜单、方块状态和镇长印章描述同一权威状态。
- 性能：大量已加载但空闲设备不持续扫描库存、标脏或发送网络包。

## Documentation Impact

实现时根据最终归属更新城镇运力 living docs，并为 P2P 玩家系统增加或更新相应系统文档。若改动配套整合包中的
配方、任务、KubeJS、数据包或配置，必须在其仓库单独记录验证。完成后新增 diary，写明设备语义、公式、生命周期、
物品安全验证和仍未解决的拓扑限制。

## Outcome

状态为 `draft`。前置仓库计划及 T13 已完成，通用端点、单速率预约、结构化结果和 snapshot 合同已记录；P2P 设备、
配对、距离成本和传输仍未实现。下一步是执行 `P00`，重新核对上述复用边界与当时源码，再在 P01 冻结拓扑后将本计划
升为 `ready`，不能直接按当前草案编码。
