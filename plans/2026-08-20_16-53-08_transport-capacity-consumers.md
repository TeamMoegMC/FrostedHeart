# 运力使用方与接口占用计划

- Time: `2026-08-20 16:53:08 +08:00`
- Authors: `chang; gameplay decisions`, `Codex; OpenAI GPT-5; planning and documentation`
- Status: `draft`
- Scope: `城镇运力使用方、接口登记、占用校验、比例限速、状态反馈与通知`
- Related: [`docs/transport_station_design.md`](../docs/transport_station_design.md), [`docs/transport_station_tasks.md`](../docs/transport_station_tasks.md), `TeamTownData`, `TeamTownResourceHolder`, `VirtualResourceType.TRANSPORT_CAPACITY`

## Goal

在后续加入物流设备时，把货运站提供的城镇运力变成可登记、可解释、可限制的共享服务容量。本文只规划
运力使用方，不重新引入已搁置的 KHJ 物流方案，也不把尚未实现的接口设备算入当前货运站里程碑 B。

## Verified Current State

- `VirtualResourceType.TRANSPORT_CAPACITY` 已是 `isService=true` 的每日重建 service。
- `TeamTownResourceHolder#resetAllServices()` 已由 `TeamTownData#buildingsWork` 在建筑生产前调用。
- `TeamTownData#buildingsWork` 按优先级逐座执行可工作的生产建筑，并在全部货运站完成后写入
  `TownTransportState` 汇总日报。
- 当前没有物流接口、运力占用登记、有效传输速率或运力不足视觉状态。
- `TeamTownResourceHolder.occupiedCapacity` 已用于仓库物品容量，不能复用其名称或语义表示运力占用。

货运站里程碑 B 的每日 service 重建、货运站生产和城镇汇总日报已经完成；本计划在现有
`TownTransportState` 基础上增加接口 Map 和运行规则。

## Frozen Decisions

### Capacity semantics

- 货运站提供的是城镇级总运力。
- 运力不会被一次运输任务扣减；它表示当天可同时承诺的物流服务能力。
- 每次晨间城镇结算先把总运力归零，再由全部可工作货运站重新建立。
- 当天建立的总运力保持到下一次晨间结算；货运站当天中途拆除不追溯扣回，下一个结算日不再贡献。
- 当前总运力继续以 `resources[VirtualResourceType.TRANSPORT_CAPACITY]` 为唯一权威值。

### Ownership

- `TeamTownData` 持有一个城镇级 `TownTransportState`。
- `TownTransportState` 持有接口登记、实时已占用运力、有效速率比例和城镇汇总日报。
- `TransportStationBuilding` 只持有单站结构、员工、生产预测和单站日报，不持有接口 Map。
- `TeamTown` 门面提供只读查询和服务端权威的登记、改速、注销方法；调用方不能直接修改 Map。

### Stable endpoint identity

接口 Map 不使用 `BlockEntity` 实例作为键。方块实体会随区块卸载重建，不适合作为存档协议。建议结构为：

```text
Map<TransportEndpointId, TransportReservation>

TransportEndpointId
└─ GlobalPos interfacePos

TransportReservation
├─ endpointKind
├─ activeConfiguredRateItemsPerSecond
├─ lastAttemptedRateItemsPerSecond（可选，仅用于失败反馈）
├─ distanceMetric
├─ reservedTransportCapacity
└─ status
```

`reservedTransportCapacity` 必须与仓库物品容量的 `occupiedCapacity` 区分命名。

### Endpoint admission

- 接口默认设置速率为 `64 items/s`，这里的 item 指单个物品而不是物品堆。
- 新接口或玩家改速时，由城镇根据接口类型、设置速率和距离指标计算候选占用量。
- 接受条件为：

```text
candidateTownReserved = currentTownReserved - oldEndpointReserved + candidateEndpointReserved
candidateTownReserved <= currentTownTransportCapacity
```

- 新接口因运力不足被拒绝时，仍写入 Map，但活动设置速率和实际占用量均为 `0`，状态为
  `INSUFFICIENT_CAPACITY`。接口不自动恢复，必须由玩家重新设置。
- 已生效接口上调速率失败时，保留原有有效设置；下调、设为 `0` 和拆除始终允许。
- 接口失败状态需要同步到方块视觉和 UI。实现时应使用 BlockState/模型变体或等价的稳定状态表达，不能在
  运行时替换资源文件。

### Distance metrics

距离指标只由具体使用方提供，`TownTransportState` 不自行猜测端点：

- 具有输入端和输出端的 P2P 物流设备使用两端曼哈顿距离：

```text
distance = abs(x1 - x2) + abs(y1 - y2) + abs(z1 - z2)
```

- 仓库接口使用所绑定 `WarehouseBuilding#getVolume()` 的三分之一次方：

```text
distanceMetric = cbrt(max(0, warehouse.getVolume()))
```

从“设置速率 + 距离指标”换算为占用运力的最终函数尚未冻结，在具体接口实现前确定。货运站建筑生产阶段
不需要该函数。

### Shortage throttling

已登记接口的名义占用量不会因为货运站减产而被删除。当晨间重建后的总运力低于已占用运力时：

```text
effectiveRateScale = occupied > 0 ? min(1, totalCapacity / occupied) : 1
endpointEffectiveRate = endpointConfiguredRate * effectiveRateScale
```

- 比例作用于所有已生效接口；若总运力为 `0` 且存在占用，所有有效速率降为 `0`。
- 运力短缺期间不能激活新接口，也不能上调现有接口速率。
- 实时传输必须使用累计额度或令牌桶保留小数吞吐，不能每 tick 向下取整有效速率。
- 区块卸载不释放占用，避免通过卸载区块临时腾出运力；重新加载后沿用城镇权威登记。

### Reporting and notification

- 城镇实时状态至少公开：总运力、已占用运力、剩余可登记运力和有效速率比例。
- 城镇晨间日报至少记录该次结算后的总运力和已占用运力。
- 日报是晨间快照；当天新增、调速或拆除接口只更新实时状态，不反向改写已经完成的历史日报。
- 若晨间总运力低于已占用运力，向城镇在线玩家合并发送一次 Tip，内容应包含总运力、占用量和有效比例。
- 接口设置被拒绝时直接向操作者反馈，不额外向全镇广播。

## Lifecycle

推荐的晨间顺序为：

```text
校验建筑与分配员工
-> TRANSPORT_CAPACITY 归零
-> 全部货运站计算并加入当日产出
-> 读取 TownTransportState 的名义占用总量
-> 计算 effectiveRateScale
-> 更新城镇汇总日报
-> 在短缺时发送每日 Tip
```

服务器主线程内该过程是原子的；物流设备不会观察到“已经归零但尚未完成货运站生产”的中间状态。

接口生命周期为：

```text
放置/绑定/玩家改速 -> 向 TeamTown 请求登记或更新
区块卸载            -> 保留登记与占用
区块重新加载        -> 按 GlobalPos 恢复并核对本地状态
拆除/解绑/更换队伍  -> 显式注销旧登记
```

只对已加载区块执行存在性修复；不能因为区块未加载就删除城镇登记。

## Implementation Steps

1. 扩展 `TownTransportState` Codec，加入带默认值的 `Map<TransportEndpointId, TransportReservation>`，保证旧存档可加载。
2. 在 `TeamTown` 增加查询、登记、更新和注销 API，并把占用公式与准入校验集中在纯模型中测试。
3. 定义通用接口状态枚举及同步合同，覆盖 active、disabled、insufficient capacity 和 invalid binding。
4. 实现仓库接口，读取所绑定 `WarehouseBuilding#getVolume()` 并提供 `cbrt(volume)` 距离指标。
5. 实现 P2P 输入端/输出端配对，以两端曼哈顿距离提供距离指标。
6. 在实际物流调度器中应用全镇 `effectiveRateScale`，使用累计额度处理非整数 items/tick。
7. 完成接口方块视觉状态、设置 UI、失败原因、当前占用和有效速率显示。
8. 接入晨间短缺 Tip、城镇汇总显示和恢复后的状态清理。
9. 更新城镇 living docs，并分别验证存档、同步、卸载、拆除、多人操作和完整回归。

## Validation

- Codec：空 Map、有效登记、拒绝登记、旧存档默认值和稳定键往返。
- 准入：新建、上调、下调、设零、拆除和失败后手动重试。
- 汇总：多接口、多类型、重复登记和同位置替换不会重复占用。
- 晨间：总运力下降后比例限速，总运力恢复后回到设置速率。
- 距离：P2P 曼哈顿距离与仓库 `cbrt(volume)` 的边界和大值。
- 生命周期：区块卸载不释放，拆除和换队正确释放，重启后登记一致。
- 吞吐：低于 `20 items/s` 和非整数有效速率不会因 tick 取整而饿死。
- 反馈：拒绝视觉状态、设置 UI、每日短缺 Tip 和客户端同步一致。

## Documentation Impact

实现时需要更新城镇 living docs，明确运力的单位、准入公式、距离口径、存档所有权、晨间生命周期、限速和
通知规则。若接口属于独立物流系统，应在 `docs/README.md` 和相应系统 README 中增加入口。

## Open Questions

- `reservedTransportCapacity = f(rate, distanceMetric)` 的确切公式、倍率、取整规则和配置项。
- P2P 是否只允许同维度配对；跨维度时曼哈顿距离没有定义。
- 接口允许的最小、最大速率和玩家调整步长。
- 运力恢复时是否额外发送一次恢复 Tip；当前只冻结了短缺时每日提醒。
- 哪些队伍角色有权绑定接口和修改速率。

## Outcome

状态仍为 `draft`，仅完成后续使用方规划；货运站里程碑 B 的前置总运力和
`TownTransportState` 基础结构已具备。接口登记、占用 Map、距离成本、比例限速、短缺提示和物流设备均未实现。
