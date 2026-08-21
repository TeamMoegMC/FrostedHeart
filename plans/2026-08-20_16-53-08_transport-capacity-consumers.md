# 仓库接口运力接入、占用与限速实施计划

- Time: `2026-08-20 16:53:08 +08:00`
- Last verified: `2026-08-21`
- Authors: `chang; gameplay decisions`, `Codex; OpenAI GPT-5; investigation and planning`
- Status: `ready`
- Scope: `城镇运力预约基础、仓库接口登记、占用公式、比例限速、同步、状态反馈与通知`
- Related: [`docs/transport_station_design.md`](../docs/transport_station_design.md),
  [`docs/transport_station_tasks.md`](../docs/transport_station_tasks.md),
  [`docs/town/implementation-reference.md`](../docs/town/implementation-reference.md),
  [`docs/town/town-model.md`](../docs/town/town-model.md),
  [`plans/2026-08-21_14-34-33_transport-capacity-consumer-tasks.md`](2026-08-21_14-34-33_transport-capacity-consumer-tasks.md),
  [`plans/2026-08-21_01-39-26_transport-p2p-devices.md`](2026-08-21_01-39-26_transport-p2p-devices.md),
  `TeamTownData`, `TeamTown`, `TownTransportState`,
  `WarehouseInterfaceBlockEntity`, `WarehouseBuilding`, `VirtualResourceType.TRANSPORT_CAPACITY`

## Goal

把货运站每日建立的城镇总运力变成可供设备预约、可持久化、可审计、可比例限速的共享服务容量，并先让现有
仓库接口成为第一个真实使用方。

本计划交付以下闭环：

1. 城镇能够按稳定端点 ID 登记、更新、拒绝和注销运力预约。
2. 占用量由统一的“活动设置速率 x 仓库规模因子”公式计算。
3. 运力供给下降时，所有已生效设备按同一比例降速，不删除已有预约。
4. 仓库接口的真实物品移动受有效速率限制，低速和小数速率不会饿死。
5. 存档、增量同步、镇长印章、设备菜单、方块状态和每日 Tip 使用同一权威状态。

源码中目前不存在 P2P 输入端或输出端方块，其方块、配对、距离、缓存和物品安全属于独立后续计划。本计划只需
保证端点登记 API 不把仓库实现写死，后续消费者能够复用预约、准入和比例限速基础。

## Non-goals

- 不恢复 `docs/backup/` 中已搁置的外部 capability、逐任务扣费或 KHJ 物流方案。
- 不把运力当作运输一次就扣除的库存，也不加入跨日囤积。
- 不修改 `design/` 下的人类设计文档。
- 不在第一阶段改造 `content.robotics.logistics` 的机器人物流网络；它与城镇仓库接口是不同消费者。
- 不实现 P2P 方块、配对、距离或传输；相关内容见独立后续计划。

## Verified Current State

- `VirtualResourceType.TRANSPORT_CAPACITY` 已是 `isService=true` 的每日重建 service。
- `TeamTownData#buildingsWork` 已按“重置全部 service -> 重建仓库容量 -> 全部生产建筑工作 ->
  `finishDailyTransportSettlement`”执行。
- 标准货运站工人每日生产 `64` 运力；当前公式允许单工人贡献 `32` 至 `147.2` 运力。
- `resources[VirtualResourceType.TRANSPORT_CAPACITY]` 是当前总运力的唯一权威值。
- `TownTransportState` 目前只持有 `DailyReport(totalCapacity, reservedCapacity)`，结算时占用固定写 `0`。
- `TownResourceUpdatePacket` 目前只同步资源、仓库物品占用和运力晨间日报；尚无实时预约快照。
- `TownVirtualResourcesPanel` 已会计算剩余、缺口和有效比例，但当前使用晨间日报中的固定占用 `0`。
- `TeamTownResourceHolder.occupiedCapacity` 专指仓库物品容量，不能复用为运力占用字段。
- `WarehouseInterfaceBlockEntity` 已实现九槽缓冲、仓库绑定、目标库存、红石门控、Watcher、NBT、区块卸载
  保留绑定和拆除清理；但一次 `balance` 可以瞬间移动全部差额，尚无运力登记或速率预算。
- 仓库扫描由 `WarehouseBlockEntity` 发现墙面接口并调用 `WarehouseInterfaceBlockEntity#tryBind`；仓库逻辑对象
  已持久化接口位置，可作为仓库拆除时的清理索引。
- 当前源码和测试中没有 P2P 设备或配对协议。

## Frozen Gameplay Rules

### Capacity semantics

- 运力是城镇级“同时承诺的服务能力”，不是单次运输的消耗品。
- 每次晨间结算先将总运力归零，再由全部可工作的货运站建立当日总运力。
- 当天总运力保持到下一次晨间结算；货运站中途拆除不追回已经建立的当日运力。
- 端点预约保存的是名义活动速率及其占用。设备实际搬运速率还要乘全镇有效比例。
- 区块卸载不释放预约；拆除、解绑或换镇必须显式注销。
- 仓库仅因重叠等原因暂时不可工作、但逻辑绑定仍存在时，接口停止搬运但保留预约；扫描明确解绑、仓库映射
  移除或接口拆除才释放预约。
- 玩家不能通过重复绑定、重复加载或同位置替换产生重复预约。

### Rate rules

| 项目 | 首版值 | 说明 |
|---|---:|---|
| 默认请求速率 | `20 items/s` | 等于 `1 item/tick`；item 指单个物品，不是物品堆 |
| 最小活动速率 | `1 item/s` | `0` 表示禁用并释放该端点占用 |
| 默认最大设置速率 | `1280 items/s` | 等于 `64 items/tick`；服务端配置可继续调高 |
| 设置精度 | `1 item/s` | 玩家设置使用整数；短缺后的有效速率允许小数 |

首版允许菜单直接输入 `0..configuredMaximumRate` 的整数，并提供 `20 / 64 / 320 / 1280` 快捷值。最大值默认
`1280`，但不是硬编码协议上限。所有范围与默认值在 `TownModelParameters.Defaults` 只有一个默认来源，
`FHConfig.SERVER.TOWN` 引用同一来源。

### Warehouse equivalent distance

规模指标由具体使用方提供，`TownTransportState` 不读取世界或猜测绑定目标。

仓库接口使用所绑定仓库有效体积的平方根作为规模指标：

```text
warehouseScaleMetric = sqrt(max(0, WarehouseBuilding#getVolume()))
```

这里的平方根是玩法规模指标，不声称是仓库内真实几何平均距离。它让仓库扩建明显增加接口占用，同时避免按体积
一次方增长导致大型仓库成本失控。仓库必须已有有效逻辑绑定；体积不是有限正数或建筑映射不存在时，不得创建
新的活动预约。

### Reservation formula

定义：

- `R`：端点活动设置速率，单位 `items/s`，范围 `0..configuredMaximumRate`。
- `D`：仓库规模指标，即 `sqrt(volume)`。
- `k = 0.05`：每单位规模指标增加的成本比例。

首版占用公式冻结为：

```text
warehouseScaleMetric = sqrt(max(0, warehouseVolume))
warehouseScaleFactor = 1 + 0.05 * warehouseScaleMetric
reservedTransportCapacity = activeRateItemsPerSecond * warehouseScaleFactor
```

`R=0` 时公式自然得到占用 `0`。运力单位定义为零规模/零距离下支持 `1 item/s`，因此没有参考速率除数：零指标
下 `1 item/s` 就占用 `1` 运力。

权威模型不做 `0.01` 向上量化，直接保留原始有限 `double`。运力模型不复用资源库存的
`TeamTownResourceHolder.DELTA`，而只在“是否增加占用、是否准入、是否短缺”这些离散边界使用局部 ULP 容差：

```text
comparisonMagnitude(a, b) = max(1, abs(a), abs(b))
comparisonTolerance(a, b) = 8 * ulp(comparisonMagnitude(a, b))
lessThanOrNearlyEqual(a, b) = a <= b
                             or a - b <= comparisonTolerance(a, b)
meaningfullyGreater(a, b) = a - b > comparisonTolerance(a, b)
```

调用这些方法前必须已校验两个输入有限且非负。容差只影响比较结果，不修改、截断或量化权威数值；UI 只在显示时
格式化小数。非有限数、负体积、越界速率和计算后非有限结果直接返回 `INVALID_REQUEST`，不能静默变为零成本预约。

基准算例：

| 仓库体积 | 规模指标 `sqrt(V)` | 规模因子 | 速率 | 占用运力 |
|---|---:|---:|---:|---:|
| `0` | `0` | `1.00` | `20/s` | `20.00` |
| `64` | `8` | `1.40` | `20/s` | `28.00` |
| `256` | `16` | `1.80` | `20/s` | `36.00` |
| `512` | `22.627...` | `2.131...` | `20/s` | `42.627...` |
| `1024` | `32` | `2.60` | `20/s` | `52.00` |
| `4096` | `64` | `4.20` | `20/s` | `84.00` |
| `512` | `22.627...` | `2.131...` | `1280/s` | `2728.155...` |

### Admission and update rules

每条预约同时区分“玩家最近请求速率”和“当前已准入活动速率”：

```text
requestedRateItemsPerSecond
activeRateItemsPerSecond
```

这样新接口失败可记录请求值并保持活动速率 `0`，已有接口上调失败也能继续按旧活动速率工作。

候选占用为：

```text
candidateTownReserved = currentTownReserved
                      - oldEndpointReserved
                      + candidateEndpointReserved
```

规则按以下顺序执行：

1. 服务端校验端点 ID、绑定目标、设备类型、速率范围和仓库规模指标。
2. `lessThanOrNearlyEqual(candidateEndpointReserved, oldEndpointReserved)` 时始终接受。所以下调、设为 `0`、
   重复应用原活动速率和拆除不会被当前短缺卡住。
3. 新建或上调只有在
   `lessThanOrNearlyEqual(candidateTownReserved, currentTownTransportCapacity)` 时接受。
4. 新接口失败：保留一条状态为 `INSUFFICIENT_CAPACITY` 的记录，`requestedRate=请求值`、`activeRate=0`、
   `reserved=0`；不会自动恢复，玩家需重试。
5. 已活动接口上调失败：记录新的 `requestedRate` 和 `INSUFFICIENT_CAPACITY`，但保留旧 `activeRate` 与旧
   `reserved`；界面明确显示“请求失败，仍按旧速率运行”。
6. 玩家再次应用当前活动速率时，即使全镇正处于短缺，也按“不增加占用”接受，并清除上一次失败状态。

仓库体积改变属于绑定事实变化，不是玩家抢占新容量。系统必须按原活动速率重算占用，即使这会让全镇进入短缺；
随后由比例限速保持公平。否则玩家可以先用小仓库准入，再扩建仓库规避成本。

### Shortage throttling

全镇名义占用不因货运站减产或距离增长而删除：

```text
effectiveRateScale = reserved <= 0
        ? 1
        : meaningfullyGreater(reserved, totalCapacity)
                ? max(0, totalCapacity) / reserved
                : 1

endpointEffectiveRate = activeRateItemsPerSecond * effectiveRateScale
```

- 比例同时作用于所有活动端点，不按端点加载顺序、类型或玩家区分。
- 总运力为 `0` 且存在占用时，所有端点有效速率为 `0`。
- 短缺时不能新建活动预约或增加占用，但允许下调、禁用、注销和不增占用的确认操作。
- 总运力恢复后，原有活动预约自动恢复到活动设置速率；失败的新请求或失败的上调不会自动准入。
- 首版不发送恢复 Tip；实时界面和方块状态会立即恢复。短缺仍按每个晨间结算最多一次提醒。

### Transfer remainder

仓库接口用一个双向共享的小数余数限制“城镇虚拟仓库 <-> 接口九槽缓冲”的实际移动，不实现可积累突发额度的
完整令牌桶：

```text
transferRemainder += endpointEffectiveRate / 20 // items/tick
tickBudget = floor(transferRemainder)
transferRemainder -= tickBudget
```

- `transferRemainder` 正常保持在 `[0, 1)` item，只保存不足一件物品的尾数，不保存未使用的整数带宽。
- `tickBudget` 在本 tick 内由导入和导出共享，不能各自获得完整速率。
- 每次向 `ItemStackAction` 提交的数量不得超过本 tick 尚未使用的 budget；Action 实际少搬的部分不会积累到
  后续 tick，因此空闲或失败不会形成突发。
- 保留现有“先导出错误/超量物品，再填补缺额”的顺序，但两阶段共享本 tick 的剩余 budget。
- 未完成的差额必须让接口下一 tick 继续工作；不能等待一个不会发生的新 Watcher 事件。
- 没有搬运需求时不累加；禁用、解绑和方块实体重建时余数归零。余数不写存档，重载不会获得额外吞吐。
- `effectiveRate < 20 items/s` 或短缺比例产生非整数速率时依靠小数余数累计，禁止逐 tick 对速率先取整。
- 余数累计与 `floor` 不使用比较容差；一次因浮点尾差稍晚跨过整数边界时，余数会保留到下一 tick，不会饿死。
- `1280 items/s` 在比例为 `1` 时恰好产生 `64 items/tick`，无需特殊分支。

## State Model

### Stable identity

接口 Map 不使用 `BlockEntity` 实例作为键。城镇内部使用：

```text
TransportEndpointId
└─ GlobalPos endpointPos
```

`GlobalPos` 同时包含维度和坐标，能够跨区块卸载、方块实体重建和服务器重启保持稳定。Map 属于具体
`TeamTownData`，因此 ID 不再重复保存队伍 ID。

### Reservation record

```text
Map<TransportEndpointId, TransportReservation>

TransportReservation
├─ endpointKind                         // 首版只有 WAREHOUSE_INTERFACE，保留可扩展枚举
├─ boundWarehouseCorePos: GlobalPos     // 绑定的仓库核心；接口坐标已在 Map key 中
├─ requestedRateItemsPerSecond: int
├─ activeRateItemsPerSecond: int
├─ scaleMetric: double                  // 首版为 sqrt(warehouse volume)
├─ reservedTransportCapacity: double    // 服务端公式生成的派生快照
└─ admissionStatus                      // ACTIVE, DISABLED, INSUFFICIENT_CAPACITY
```

`reservedTransportCapacity` 必须与仓库物品容量的 `occupiedCapacity` 区分命名。它是由 kind、active rate、scale
metric 和当前公式参数派生的服务端快照，调用方不能提交或直接修改。

仓库有效性、红石门控、接口库存是否满足目标属于方块实体运行状态，不进入城镇预约记录。全镇
`THROTTLED` 也由实时总量与占用派生，不持久化为逐端点状态。

### Aggregate views

`TownTransportState` 提供以下只读派生值：

```text
getReservedTransportCapacity()
getRemainingRegistrableCapacity() = max(0, total - reserved)
getTransportCapacityShortfall()    = max(0, reserved - total)
getEffectiveRateScale()
getReservations()                  // unmodifiable, stable display order
```

晨间 `DailyReport` 仍是历史快照，只记录结算完成时的 `totalCapacity` 和实时名义 `reservedCapacity`。当天登记、
调速、仓库规模变化或拆除只改变实时状态，不反向改写已经完成的日报。

## Persistence and Codec

1. `TownTransportState.CODEC` 增加可选 `reservations` 字段，默认空列表，保证当前只有 `dailyReport` 的存档可加载。
2. 内存使用 Map，Codec 使用按 `GlobalPos` 稳定排序的 entry list；不要依赖 `HashMap` 顺序，也不要把
   `GlobalPos.CODEC` 强塞为字符串 Map key。
3. 解码后若同一 endpoint ID 出现多条记录，丢弃该冲突 key 的整组记录并警告；不能按输入顺序任选一条。其他
   合法 endpoint 继续加载。
4. 所有 double 解码后校验有限、非负；所有 rate 校验范围。未知 enum 或损坏条目应隔离并记录警告，不能让整个
   城镇存档无法加载。
5. Codec 保存 endpoint、仓库核心、kind、requested/active rate、scale metric 和 status 等权威输入；服务器加载后
   按当前 `TransportConsumerParameters` 重新计算 `reservedTransportCapacity`。配置或公式参数变化可以造成短缺，
   但不取消已活动速率；短缺由统一比例处理。
6. 初次升级旧存档时，未加载的旧仓库接口没有预约也不能执行搬运；接口下一次加载或仓库下一次成功扫描时按
   默认 `20 items/s` 尝试登记。

## Authoritative APIs

纯模型放在 `content.town.transport`，不读取世界、不发包：

```text
TransportReservationModel
├─ warehouseScaleMetric(volume)
├─ warehouseScaleFactor(scaleMetric, parameters)
├─ requiredCapacity(kind, activeRate, scaleMetric, parameters)
├─ comparisonTolerance(first, second)
├─ lessThanOrNearlyEqual(first, second)
├─ meaningfullyGreater(first, second)
├─ effectiveRateScale(total, reserved)
└─ evaluateAdmission(total, currentReservations, oldReservation, request)
```

服务端修改只能通过 `TeamTown` 门面：

```text
getTransportSummary()
getTransportReservations()
getTransportReservation(endpointId)
registerOrUpdateTransportEndpoint(request)
refreshTransportEndpointMetric(endpointId, boundWarehouseCorePos, scaleMetric)
unregisterTransportEndpoint(endpointId)
```

`registerOrUpdateTransportEndpoint` 返回结构化结果，不使用 boolean：

```text
TransportReservationResult
├─ decision: ACCEPTED | INSUFFICIENT_CAPACITY | INVALID_REQUEST | INVALID_BINDING
├─ reservationAfter
├─ townSummaryAfter
└─ requiredAdditionalCapacity
```

`TownTransportState` 的 Map mutator 保持包内或私有；调用方只能读取不可变视图。每次净变化由
`TeamTownData` 统一调用 `DataSyncCache#markTransportStateChanged`，不能依赖设备方块自己记得发同步。

## Synchronization

- 全量 `TeamTownData` Codec 继续负责登录、切维度和恢复同步。
- 将 `TownResourceUpdatePacket` 的运力字段从单独 `DailyReport` 扩展为不可变 `TownTransportSnapshot`，至少包含
  日报、实时预约列表和实时汇总。这样镇长印章不再把日报误当实时登记表。
- 预约 Map 预计规模远小于居民和仓库物品表，首版每次净变化发送完整、小型、稳定排序快照；不要先加入复杂的
  端点增量协议。若实测规模使包过大，再单独优化。
- 客户端 `applyResourceUpdate` 必须先同时应用资源值和运力快照，再触发一次资源数据回调，避免界面观察到半更新。
- 仓库接口菜单继续用现有 Menu data slot 同步该设备的状态；速率用整数，占用与有效比例用明确的定点显示单位
  或 Codec data slot，避免把 double 截断成错误含义。显示精度不改变服务端权威原始值。
- 所有设置请求在服务端重新解析和校验。首版没有城镇角色权限系统，因此权限冻结为：能够合法打开该接口菜单、
  且玩家当前队伍就是绑定城镇的任意队员可以修改；不额外发明“镇长”角色。

## Warehouse Interface Integration

### Registration lifecycle

1. `WarehouseBlockEntity#publishInterfaces` 成功绑定接口后，用接口 `GlobalPos`、仓库核心 `GlobalPos`、
   `WarehouseBuilding#getVolume()` 和默认/已有请求速率向 `TeamTown` 登记或核对。
2. 已有同 ID、同 kind、同 `boundWarehouseCorePos` 的记录幂等返回，不重复占用。
3. 接口 `onLoad` 解析现有绑定后核对城镇记录；没有记录时按默认速率尝试登记。
4. 普通区块卸载只移除 Watcher 和运行时余数，保留绑定与城镇预约。
5. 接口方块被破坏时，在清空 provider 前注销预约，再执行现有库存掉落和绑定清理。
6. 接口改绑到另一仓库或另一城镇时，先用旧 provider 注销旧预约，再写新 provider 并尝试新预约。
7. 仓库核心拆除或同位置替换时，`TeamTown#removeTownBlockInternal` 根据
   `WarehouseBuilding#getInterfacePositions()` 注销相关预约；即使某接口区块未加载，也不能留下占用。
8. 已加载接口发现逻辑仓库不存在、记录 `boundWarehouseCorePos` 不匹配或该位置已不是接口方块时，注销记录并进入现有
   unbound 状态。未加载区块不能仅因无法检查就删除。
9. 仓库成功重扫且体积净变化时，刷新所有已绑定接口的规模指标和派生占用；增长导致短缺时不拒绝结构变化，而是
   立即进入全镇比例限速。

### Balance throttling

- 把当前 `balance(IActionExecutorHandler)` 改为接受本 tick 可移动物品上限并返回实际移动数与是否仍有待处理差额。
- 导出阶段和补货阶段分别把 Action 的 ItemStack 数量裁到剩余预算。
- 导出与补货全过程共享 `tickBudget`；每次 Action 请求量不超过剩余 budget，实际移动后再减少本 tick budget。
- 有剩余差额且有效速率大于零时，保持 continuation 标志，让下一 tick 继续。
- 有剩余差额但全镇比例为零时，不执行资源 Action；保留待处理状态，下一 tick 或 transport snapshot 变化后重试。
- 红石只门控从城镇仓库补货的方向，继续允许超量/错误物品按同一速率返回仓库，保持现有语义。

## Deferred P2P Work

P2P 设备尚无源码、注册或玩家交互基础，其方块、配对、曼哈顿距离、跨维度规则、缓存和物品安全全部迁移到
[`2026-08-21_01-39-26_transport-p2p-devices.md`](2026-08-21_01-39-26_transport-p2p-devices.md)。本计划只要求
`TransportEndpointId`、预约结果和服务端 API 保持按 endpoint kind 扩展的能力，不实现或测试 P2P 行为。

## Player Feedback

### Warehouse interface menu

在现有九槽和目标设置之上显示：

- 请求速率与当前活动速率；
- 仓库规模指标及公式结果；
- 本接口占用运力；
- 全镇总运力、已占用、剩余/缺口；
- 全镇有效比例与本接口有效速率；
- `ACTIVE`、`DISABLED`、`INSUFFICIENT_CAPACITY`、`UNBOUND`、`WAREHOUSE_UNAVAILABLE` 状态。

上调失败时保持输入值，显示所需新增运力，并明确旧活动速率仍然有效。设为 `0` 是禁用，不是拆除。

### Mayor's Seal

`TownVirtualResourcesPanel` 的运力详情改读实时 `TownTransportSnapshot`，显示汇总及稳定排序的预约列表。每行至少有
设备类型、维度/坐标、请求/活动/有效速率、规模指标、占用和状态。晨间日报单独标注为最近结算快照。

### Block state

`WarehouseInterfaceBlock` 增加稳定的有限状态表达，至少区分正常、禁用、运力不足和绑定不可用。可使用枚举
BlockState 与预生成模型/纹理变体；禁止运行时替换资源文件。若完整枚举造成模型组合过多，可用两个布尔属性表达
`ACTIVE` 与 `ERROR`，详细原因留在菜单，但必须保证服务端状态变化会同步 BlockState 且不在每 tick 重写相同值。

### Daily shortage Tip

`finishDailyTransportSettlement` 在 `meaningfullyGreater(reserved, total)` 时每个城镇日合并发送一次 Tip。现有
`TownSignalNotificationPacket` 不携带 double，无法满足“总量、占用、比例”文本，因此增加专用的安全 S2C 通知或
扩展其安全 notice 模型；不要把任意服务端 detail 字符串直接显示给客户端。

Tip 显示总运力、占用、缺口和有效比例，点击进入镇长印章运力详情。接口设置被拒绝只反馈操作者，不广播全镇。

## Implementation Sequence

逐任务复杂度、建议推理强度、代码锚点、关键技术点与完成门槛由
[`2026-08-21_14-34-33_transport-capacity-consumer-tasks.md`](2026-08-21_14-34-33_transport-capacity-consumer-tasks.md)
统一维护。本计划只维护阶段目标，避免两份技术清单漂移。

| 阶段 | 任务 | 阶段目标 | 退出门槛 |
|---|---|---|---|
| `P0` 基线 | `T00` | 固定当前工作区、编译和测试基线 | 后续失败能区分既有问题与本功能回归 |
| `P1` 模型与权威状态 | `T01-T03` | 完成参数、公式、Codec、预约状态机和 `TeamTown` 门面 | 无世界依赖的模型与城镇 API 测试全部通过 |
| `P2` 结算与分发 | `T04-T05` | 日报读取实时占用，客户端收到原子 transport snapshot | 服务端、全量存档和增量包对同一状态达成一致 |
| `P3` 首个消费者 | `T06-T08` | 接入仓库生命周期、小数预算和真实物品限速 | 无幽灵占用、低速饿死、空闲突发、复制或吞物 |
| `P4` 玩家反馈 | `T09-T11` | 完成设备设置、镇长印章、方块状态和每日短缺 Tip | 所有界面只显示服务端权威值，拒绝与短缺原因一致 |
| `P5` 交付 | `T12`, `H01`, `T13` | 完成回归、性能、游戏内验收和知识收尾 | 自动化与人工验收通过，living docs 和 Outcome 已更新 |

`T07` 的纯预算模型在 `T01` 后可与 `T02-T05` 并行；其余任务按任务清单依赖执行。任何 `XL` 任务不得为了减少
模型调用而与相邻任务合并，尤其不得把 `T06` 生命周期、`T08` 物品安全和 `T12` 完整回归压成一次实现。

## Validation Matrix

### Formula and model

- 表中全部基准算例、`R=0`、默认最大速率、平方根非整数、恰好相等和 `8 ULP` 比较边界。
- NaN、正负无穷、负体积、负规模指标、越界 rate 和乘法溢出。
- 总运力/占用为 `0`、相差 `1/8/9 ULP`、轻微浮点尾差和严重短缺；容差不得写回权威值。

### Admission

- 新接口成功/失败；已有接口上调成功/失败；下调、设零和不增占用确认。
- 上调失败后 `requested > active`，旧速率继续工作；手动重试成功后两者一致。
- 多端点、不同类型、重复登记、同位置替换和候选计算不重复占用。
- 仓库扩建提高规模指标并造成短缺，缩建降低占用；不得通过先小后大的方式绕过公式。

### Codec and sync

- 空 Map、有效/禁用/拒绝预约、旧 `dailyReport`-only 存档、稳定顺序和全量往返。
- 重复 key、未知 enum、损坏数值和部分坏条目处理符合固定策略。
- 全量同步与资源增量竞争时，客户端最终同时得到正确总运力、实时占用、日报和列表。
- 当天调速只改实时 snapshot，不改晨间日报。

### Lifecycle

- 接口加载、普通区块卸载/重载、方块破坏、仓库拆除、仓库同位置替换、重新绑定、换队和服务器重启。
- 只删除已加载且确认无效的孤儿；未加载端点不被误删。
- 仓库接口物品在任何注销路径都不复制、不吞失，现有拆除掉落仍生效。

### Throughput

- `1`, `7`, `19`, `20`, `64`, `128 items/s` 以及短缺产生的非整数有效速率。
- 空闲期间余数不增长，恢复工作后没有累计突发；重载后余数从零开始。
- 导出与导入竞争同一预算，Action 部分成功只扣实际移动量。
- 红石禁止补货时仍可按速率回收错误/超量物品。
- 20 TPS 与人工降低 TPS 时按 server tick 语义稳定，不按客户端帧率运行。

### Feedback and authority

- 菜单输入篡改、越界值、非本队玩家、失效菜单和绑定在请求期间改变均由服务端拒绝。
- 方块状态、接口菜单、镇长印章和 Tip 对同一时刻的状态描述一致。
- 每日短缺 Tip 只发给本队在线玩家，每镇每日最多一次；调速拒绝只通知操作者。

### Commands

```powershell
.\gradlew.bat test --tests "*TownTransport*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*WarehouseInterface*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain
.\gradlew.bat test --offline --no-daemon --console=plain
.\gradlew.bat compileJava --offline --no-daemon --console=plain
git diff --check
```

若 BlockState 或模型变体需要数据生成，再执行 `runData`，并在前后记录工作区状态，只保留本功能生成差异。

## Documentation Impact

实现时至少更新：

- `docs/town/implementation-reference.md`：状态所有权、API、Codec、同步和生命周期。
- `docs/town/town-model.md`：单位、仓库平方根规模指标、占用公式、参数、准入、比例限速和模拟算例。
- `docs/transport_station_design.md`：从“占用固定为 0”改为链接到已实现消费者行为，但不把使用方状态写回单站。
- `docs/README.md` 或新的物流系统 README：仅当 P2P 或多个消费者形成独立玩家系统时增加入口。
- 新 diary：记录实现决策、自动化验证、游戏内 H01 结果和剩余 P2P 工作。

## Risks and Controls

| 风险 | 控制 |
|---|---|
| 区块卸载被当作拆除，释放运力 | 沿用方块状态判定；卸载只清运行时余数和 Watcher |
| 仓库拆除留下未加载接口预约 | 用 `WarehouseBuilding#getInterfacePositions` 从城镇侧注销，不依赖 BE 已加载 |
| 浮点累加导致边界请求误拒绝或误报短缺 | 保留原始有限 double，聚合从稳定排序的 Map 重算，仅在离散边界使用 `8 ULP` 比较 |
| 上调失败把旧设备停掉 | 分开 requested/active rate，失败保留旧 active 与 reserved |
| 总运力下降后端点按加载顺序抢速率 | 全镇统一比例，每端点独立应用同一 scale |
| 低于 20 items/s 永远不搬物品 | 小数余数累计到整件后执行 |
| 部分搬运后不再收到 Watcher 事件 | 显式 continuation 标志逐 tick 继续 |
| 客户端日报与实时状态混淆 | snapshot 和 DailyReport 分栏，实时报表不回写历史日报 |
| 公式改动使旧预约失真 | 加载后按当前参数重算，必要时进入短缺而不静默取消 |
| P2P 还不存在却提前固化错误设备结构 | 当前计划不含 P2P 行为，独立 draft 在仓库接口完成后重新核验 |

## Remaining Non-blocking Decisions

以下不阻塞仓库接口里程碑：

- 是否在未来增加短缺恢复 Tip；首版只在短缺晨间提醒。
- 当预约数量实测达到需要优化的规模时，是否把完整 snapshot 改为端点增量包。

## Outcome

计划已达到 `ready`：现有生产端、首个真实消费者、占用公式、距离口径、准入语义、持久化、同步、限速、
生命周期、反馈和验收顺序均已核对到当前源码。尚未实现任何预约 Map、仓库接口限速、实时快照、视觉状态或 Tip；
执行应从 `T00` 开始；P2P 设备只在本计划 `T13` 完成后进入独立计划的重新调查阶段。
