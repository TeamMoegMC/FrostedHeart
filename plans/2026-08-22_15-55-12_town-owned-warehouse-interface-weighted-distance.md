# 城镇级仓库自动化设备与容量加权距离改造计划

- Time: `2026-08-22 15:55:12 +08:00`
- Last verified: `2026-08-22`
- Authors: `chang; gameplay decisions`, `Codex; OpenAI GPT-5; source investigation and planning`
- Status: `completed`
- Scope: `仓库接口与发信器任意位置放置、城镇归属、容量加权平均距离、预约模型、红石、生命周期、权限与界面反馈`
- Related: [`2026-08-20_16-53-08_transport-capacity-consumers.md`](2026-08-20_16-53-08_transport-capacity-consumers.md),
  [`2026-08-21_14-34-33_transport-capacity-consumer-tasks.md`](2026-08-21_14-34-33_transport-capacity-consumer-tasks.md),
  [`2026-08-21_01-39-26_transport-p2p-devices.md`](2026-08-21_01-39-26_transport-p2p-devices.md),
  [`docs/town/implementation-reference.md`](../docs/town/implementation-reference.md),
  [`docs/town/town-model.md`](../docs/town/town-model.md)

## Goal

把仓库接口与 `WarehouseLevelEmitter` 从“由某一座仓库扫描并绑定的墙面设备”改为“由城镇拥有的仓库自动化设备”。
两者都可以放在任意位置并连接所属城镇的全局虚拟仓库。接口实际搬运物品，继续占用运力；其占用不再由单仓库体积
平方根决定，而由接口到城镇全部有效仓库核心的仓库容量加权平均距离决定。发信器只监听一种物品的城镇库存并按阈值
输出红石，不搬运物品、不创建运力预约，也不计算距离成本。

## Verified Current State

- `WarehouseBlockScanner` 收集仓库结构中的接口候选，并由 `getWallInterfacePositions()` 只接受背面朝内、正面朝外的墙面
  接口。
- `WarehouseBlockEntity#publishInterfaces` 调用 `WarehouseInterfaceBlockEntity#tryBind`；`WarehouseBuilding` 的 Codec 和
  `interfacePositions` 保存单仓库拥有的接口坐标。
- `WarehouseInterfaceBlockEntity` 持久化 `townProvider + warehousePos`。`resolveBinding` 要求该仓库仍存在、仍将接口
  列在 `interfacePositions` 中且结构可工作。
- `WarehouseLevelEmitterBlockEntity` 同样持久化 `townProvider + warehousePos`，由 `WarehouseBuilding.emitterPositions`
  和 `WarehouseBlockEntity#publishEmitters` 绑定。它通过城镇级 `TeamTownResourceHolder` 精确监听一种物品的 NBT 存量，
  在 `HIGH_SIGNAL`/`LOW_SIGNAL` 条件满足时输出固定 `15` 级红石；单仓绑定并不是读取库存所必需的。
- `TransportReservation` 和 `TransportEndpointRequest` 保存 `boundWarehouseCorePos`；仓库拆除时
  `TeamTown#unregisterTransportEndpointsBoundTo` 会注销该仓库的全部接口预约。
- 当前仓库接口规模指标为 `sqrt(WarehouseBuilding#getVolume())`，占用因子为
  `1 + warehouseScaleCostPerMetric * scaleMetric`，源码默认系数为 `0.05`。
- 实际库存和 Watcher 已经属于城镇级 `TeamTownResourceHolder`，不依赖某座仓库的独立库存。因此解除单仓绑定不需要
  改写物品存储模型。
- `TeamTownData.buildings` 以 `BlockPos` 为键而不是 `GlobalPos`。本计划沿用当前单维度城镇前提，不顺带实现跨维度城镇
  建筑。

## Frozen Gameplay Rules

1. 仓库接口和发信器都可放在任意位置，不参与仓库结构扫描，也不要求位于仓库外壁或朝向仓库内部。
2. 两种设备都属于一个城镇，而不是某一座仓库；接口端点 ID 继续使用其物理 `GlobalPos`。发信器不是运力端点。
3. 玩家放置任一设备时自动绑定到玩家当前队伍的城镇。无玩家上下文放置时保持“未绑定城镇”，可由城镇成员首次
   交互认领。
4. 已绑定设备不能被其他队伍静默接管。接口的目标、数量、红石和速率，以及发信器的过滤、阈值和模式，全部由
   服务端验证操作者属于设备城镇。
5. 发信器继续精确监听一个 `SimpleItemKey`：`HIGH_SIGNAL` 在存量大于等于阈值时输出 `15`，`LOW_SIGNAL` 在存量低于
   阈值时输出 `15`。`FACING` 只控制模型朝向和邻居刷新，不再表达仓库内外。
6. 发信器不搬运物品，因此不占用运力、不参与容量加权距离，也不出现在 `TownTransportSnapshot` 端点列表中。
7. 接口距离使用接口方块到仓库核心方块的三维曼哈顿距离。
8. 只统计同一城镇中 `WarehouseBuilding#isBuildingWorkable()` 为真且 `getCapacity()` 为有限正数的仓库。逻辑建筑已加载
   到城镇数据但物理区块未加载时仍参与计算。
9. 每座仓库以 `WarehouseBuilding#getCapacity()` 为权重；不按仓库数量做无权平均，也不再使用仓库体积平方根。
10. 仓库集合、有效性或容量变化属于事实变化：直接重算已有接口预约，可令全镇进入或退出比例限速，不反向修改接口的
   单一设置速率。
11. 新接口在城镇没有有效仓库时以 `rate=0` 创建且不可用。已有接口暂时失去全部仓库时保留设置速率、停止传输并将
    实时占用降为 `0`；仓库恢复后按新距离恢复占用，必要时造成短缺。
12. 发信器在城镇没有有效仓库时强制关闭红石并显示“城镇无可用仓库”；仓库恢复后立即重新读取所监控物品的库存，
    不能等待下一次库存变化才恢复输出。
13. 普通区块卸载继续保留设备归属与配置；接口还保留库存、设置速率和预约。确认设备被破坏或显式解绑城镇时释放
    Watcher，接口另外注销运力端点并掉落九槽物品。
14. 暂不支持跨维度仓库距离或跨维度仓库自动化设备。设备与其城镇仓库必须处于当前城镇建筑模型所假定的同一维度。
15. 仓库自动化功能仍未发布，旧 `warehousePos`、`interfaces`、`emitters` 和 `boundWarehouseCorePos` 开发存档不做迁移
    兼容。

## Capacity Formula

定义：

- (P=(P_x,P_y,P_z))：接口方块位置。
- (W_i=(W_{ix},W_{iy},W_{iz}))：第 (i) 座有效仓库的核心位置。
- (K_i)：第 (i) 座仓库的 `WarehouseBuilding#getCapacity()`，单位为仓库容量。
- (N)：有效仓库数量。
- (R)：接口设置速率，单位 `items/s`。
- (D_i)：接口到第 (i) 座仓库核心的曼哈顿距离，单位 `blocks`。
- (D_eff)：仓库容量加权平均距离，单位 `blocks`。
- (k_d)：每格距离成本，源码默认 `0.05 / block`。
- (F_d)：距离因子，无量纲。
- (C)：接口占用运力。

```text
D_i = abs(Px - Wix) + abs(Py - Wiy) + abs(Pz - Wiz)

D_eff = sum(K_i * D_i) / sum(K_i)

F_d = 1 + k_d * D_eff

C = R * F_d
```

边界规则：

- `N = 0` 或 `sum(K_i) <= 0` 时没有可定义的有效距离，接口进入仓库不可用状态且实时占用为 `0`。
- 坐标差先提升到 `long` 或 `double` 再取绝对值，避免 `int` 溢出。
- 仓库按核心坐标稳定排序后求和，避免 Map 遍历顺序制造浮点差异。
- 权重、距离、中间和、结果必须为有限非负值；非法仓库权重不进入集合，非法最终结果拒绝写入预约。
- 准入边界继续只使用局部 `8 ULP` 比较，不量化、不向上取整，也不修改权威结果。

示例：接口到两座仓库的距离分别为 `10` 和 `50 blocks`，仓库容量分别为 `1000` 和 `3000`：

```text
D_eff = (1000 * 10 + 3000 * 50) / 4000 = 40 blocks
F_d = 1 + 0.05 * 40 = 3
R = 20 items/s 时，C = 20 * 3 = 60 运力
```

这意味着容量较大的仓库对等效距离影响更大，更接近“城镇物品主要分布在哪些仓库”的抽象。同比例放大所有仓库容量
不会改变等效距离。

## State Ownership

### WarehouseInterfaceBlockEntity

持久化：

```text
townProvider                         // 城镇/队伍归属
inventory, targets, redstoneMode     // 现有设备配置和物品
```

不再持久化 `warehousePos`。Watcher 直接从城镇 `TeamTownResourceHolder` 创建；接口可用性由“城镇存在且至少有一个有效
仓库”决定。

### WarehouseLevelEmitterBlockEntity

持久化：

```text
townProvider                         // 城镇/队伍归属
filter, threshold, redstoneMode      // 现有监听配置
lastKnownStock, emitterOn            // 现有展示/红石状态
```

不再持久化 `warehousePos`。发信器从同一城镇 `TeamTownResourceHolder` 创建精确物品 Watcher，不创建
`TransportReservation`。仓库拓扑为空或城镇失效时必须释放/停用 Watcher、把红石关断并通知邻居；拓扑恢复时由城镇侧的
瞬态已加载设备监听器立即调用 `ensureWatcherAndRefresh`。未加载发信器不需要持久化坐标注册表，区块加载时自行恢复。

### TransportReservation

通用运力预约只保存容量服务所需状态：

```text
endpointKind
rateItemsPerSecond
scaleMetric                         // 对仓库接口即 D_eff
reservedTransportCapacity
admissionStatus
```

删除 `boundWarehouseCorePos`。设备与建筑的关系属于设备自身状态，不属于通用容量预约；未来 P2P 的目标位置也应由 P2P
绑定状态保存。

为了表达“速率仍保留，但城镇暂时没有有效仓库”，`TransportAdmissionStatus` 明确增加 `UNAVAILABLE`：

- 不可用预约保留 `rateItemsPerSecond`；
- `scaleMetric=0` 仅作不可定义时的有限占位，UI 不显示为零距离；
- `reservedTransportCapacity=0`；
- 不参与短缺占用；
- 不允许物品传输；
- 仓库恢复时以保留速率重算，不产生第二套请求/活动速率。

### Warehouse topology snapshot

城镇侧维护可派生、非持久化的稳定快照：

```text
[(warehouseCorePos, capacityWeight), ...]    // 按核心坐标排序
```

快照从 `TeamTownData.buildings` 重建。快照中位置、成员、有效性或容量任一净变化才触发全部仓库接口 metric 重算；配置
未变的周期扫描不得重算所有端点、标脏或发包。拓扑净变化还要通知当前已加载的接口和发信器监听器：接口恢复或释放
Watcher，发信器立即重新判定库存与红石。监听器注册是瞬态运行时状态，不写入城镇 Codec。

## Lifecycle

### Placement and claiming

- `WarehouseInterfaceBlock#setPlacedBy` 和 `WarehouseLevelEmitterBlock#setPlacedBy` 从 `ServerPlayer` 的团队数据创建
  `TeamTownProvider`，保存设备城镇归属；接口随后执行新端点准入，发信器随后连接库存 Watcher。
- 无玩家或无法解析团队时保持未绑定，不注册预约。
- 未绑定设备首次被玩家使用时可绑定到该玩家城镇；已经绑定的设备只接受所属队伍成员操作。
- 新端点存在有效仓库时，按当前加权距离和默认速率执行现有准入；运力不足则保持 `rate=0` 并定向提示放置者。
- 新端点没有有效仓库时直接以 `rate=0`、仓库不可用状态建立或保持设备状态，不伪报为运力不足。

### Warehouse changes

- 仓库加入、拆除、结构失效/恢复、重叠状态变化或 `capacity` 净变化都刷新城镇仓库拓扑。
- 刷新遍历所有 `WAREHOUSE_INTERFACE` 预约，包括接口区块未加载的端点，并根据 endpoint 坐标计算新 metric。
- 自动刷新属于事实刷新，不经过玩家上调准入；它可以造成统一比例限速。
- 拆除任意仓库不注销接口。删除最后一座有效仓库时，接口进入不可用状态但保留已有设置速率。
- 仓库重新可用后恢复 Watcher 和搬运；运行时小数余数从 `0` 重新开始，不能积累离线突发。
- 删除最后一座有效仓库时，已加载发信器立即关断红石并通知邻居；仓库恢复时立即按当前库存重新计算输出。

### Device unload and removal

- 普通区块卸载将设备从拓扑监听器注销并释放 Watcher；接口另重置 `TransportTransferBudget`，但保留城镇归属、库存和
  预约。发信器保留过滤、阈值、模式和最后展示状态，实际红石只由已加载方块提供。
- 接口确认被破坏时注销 endpoint、释放 Watcher、按现有规则掉落九槽物品；发信器被破坏时只释放 Watcher 和城镇归属。
- 仓库扫描不再拥有或清理两种设备；孤儿预约只能由接口物理生命周期、已加载位置校验或显式城镇解绑清理。

## Code Changes

### Formula and parameters

- `TransportReservationModel`：将 `warehouseScaleMetric(double volume)` 替换为容量加权平均距离纯函数；保留统一占用和
  比较逻辑。
- `TransportConsumerParameters`、`TownModelParameters`、`FHConfig`：把
  `warehouseScaleCostPerMetric` 重命名为可搜索、带单位的 `warehouseDistanceCostPerBlock`，默认仍为 `0.05`。
- `TransportReservation`、`TransportEndpointRequest`、`TeamTown`：移除单仓绑定字段及按仓库核心注销 API，增加不可用
  预约 `UNAVAILABLE` 和独立的拓扑事实更新入口。

### Warehouse decoupling

- `WarehouseBlockScanner`：删除接口/发信器候选、`getWallInterfacePositions()`、`getWallEmitterPositions()` 和仅为这两种
  设备服务的墙面方向判断；扫描器仍统计仓储架装饰。
- `WarehouseBuilding`：删除 `interfacePositions`、`emitterPositions`、对应 Codec 字段及全部设备绑定/解绑方法。
- `WarehouseBlockEntity`：删除 `publishInterfaces`、`publishEmitters`、对应 clear 方法和仓库恢复时逐设备唤醒逻辑。
- `TeamTown#removeTownBlockInternal`：仓库拆除改为刷新拓扑，不调用按核心批量注销接口。

### Town-owned devices

- `WarehouseInterfaceBlockEntity`：`BindingContext` 改为城镇上下文和当前仓库拓扑，不再返回单个 `WarehouseBuilding`。
- `tryBind`/`unbindIfBoundTo` 改为城镇认领/解绑 API；保存 `townProvider`，删除 `warehousePos` NBT。
- `ensureWatcherAndRefresh` 直接连接城镇资源持有者；工作条件改为城镇有效且拓扑非空。
- `WarehouseLevelEmitterBlockEntity` 使用相同的城镇认领、解绑和可用性上下文，删除单仓 `BindingContext`、
  `warehousePos` NBT 及 `containsEmitter` 校验；不接入 transport reservation。
- 发信器加入城镇仓库拓扑监听：加载/认领时注册，卸载/解绑时注销，拓扑变化时立即刷新红石。禁止用逐 tick 仓库扫描
  代替事件通知。
- 两种设备的所有菜单命令统一调用服务端队伍、距离、菜单实例和方块实体有效性验证，不能只保护接口速率命令。

### Snapshot and UI

- `TownTransportSnapshot` 保留 endpoint、设置速率、有效速率、metric、占用和状态，不再同步绑定仓库核心；额外同步服务端
  权威的有效仓库数和 `warehouseDistanceCostPerBlock`，供镇长印章显示 disabled endpoint 的距离因子。
- 接口紧凑 UI 继续只显示当前传输速率、占用运力、剩余/总运力；仓库不可用时显示明确原因。
- 镇长印章展开详情删除“绑定仓库核心”，改为显示有效仓库数量、容量加权平均距离、距离因子和占用运力。
- 发信器 UI 把“未绑定仓库/仓库不可用”改为“未绑定城镇/城镇无可用仓库”，保留当前存量、阈值、模式和输出状态。
- 更新中英文翻译和展示模型测试；不要在紧凑接口 UI 中重新堆叠全部调试字段。

## Refresh and Performance Contract

仓库刷新频繁，不能在每次扫描或每个接口 tick 中执行 `O(E*W)`：

1. 没有 building change dirty 时 `O(1)` 返回，不构造快照。
2. dirty 时从城镇建筑构造按核心坐标排序的 `(pos, capacity)` 快照；与上次完全相等时清 dirty 后返回。
3. 净变化时一次遍历所有仓库接口预约，以稳定顺序计算加权距离并替换净变化记录。
4. 一次更新总占用缓存，最多标记一次 transport dirty。
5. 预约状态应用完成后，通知当前已加载的仓库自动化设备监听器；发信器立即关断或恢复红石，接口恢复或释放 Watcher。
6. 未加载接口不需要加载区块也能更新占用；未加载发信器没有红石输出，并在下次 `onLoad` 按最新拓扑恢复。

触发统一收口到现有 building change 链：仓库 setter、结构有效性/重叠 setter 和 buildings Map add/remove 只设置 topology
dirty；下一次 `prepareTransportState` 执行幂等刷新。存档解码后 dirty 默认为 true。不能只依赖 `reloadMaxCapacity` 的总容量
相等判断，因为两组不同位置的仓库可能具有相同总容量但产生不同距离。

## Tasks and Complexity

| ID | 任务 | 复杂度 | 推理强度 | 依赖 | 主要产物 |
|---|---|---|---|---|---|
| `W00` | 建立接口/发信器墙面绑定、预约 Codec 和测试基线 | `S` | `medium` | 无 | 基线、变更文件清单 |
| `W01` | 实现容量加权平均距离纯模型和参数重命名 | `M` | `high` | `W00` | 公式、边界和配置测试 |
| `W02` | 移除预约中的单仓绑定并加入不可用语义 | `L` | `xhigh` | `W01` | 新 request/reservation/Codec/API |
| `W03` | 将接口和发信器改为城镇认领、持久化和权限模型 | `XL` | `xhigh` | `W02`, `W04` | 任意位置放置、服务端权限、Watcher |
| `W04` | 实现容量/位置拓扑快照、端点事实刷新和已加载设备通知 | `XL` | `xhigh` | `W02` | 未加载端点重算、发信器唤醒、O(1) 空闲路径 |
| `W05` | 从仓库扫描、Codec 和拆除生命周期移除两种设备 | `L` | `high` | `W03`, `W04` | 仓库/自动化设备生命周期解耦 |
| `W06` | 更新接口、发信器与镇长印章反馈、同步和翻译 | `M` | `high` | `W03`, `W04` | 无单仓文案、距离与红石状态 |
| `W07` | 完成回归、性能和数据安全自动化 | `L` | `xhigh` | `W05`, `W06` | 完整自动化矩阵 |
| `H02` | 游戏内两种设备的任意位置、拓扑变化、红石和权限验收 | `L` | 手动 | `W07` | 实际游戏记录 |
| `W08` | 更新 living docs、计划 Outcome 和 diary | `S` | `medium` | `H02` | 当前行为与完成记录 |

综合复杂度为 `XL`。为降低共享状态接错的风险，默认执行顺序固定为
`W00 -> W01 -> W02 -> W04 -> W03 -> W05 -> W06 -> W07 -> H02 -> W08`。W04 先提供权威拓扑、批量刷新和监听器
合同，W03 再让方块实体消费该合同。只有在独立分支、独立工作区且 W02 API 已冻结时，W03/W04 才允许并行；同一工作区
不得并行修改 `TeamTown`、`TeamTownData` 和设备生命周期。

## Mandatory Implementation Playbook

本节是后续任务的强制实施手册，不是可选建议。执行者如果发现源码已经与本节不符，应先重新调查、更新本计划并说明
差异，不能自行猜测或静默改变玩法口径。

### 1. 每个任务的工作纪律

1. 开始任务前先执行 `git status --short`，记录已有改动；只修改该任务列出的源码、测试和资源，不吸收无关未跟踪文件。
2. 重新搜索关键字段和调用者。至少用 `rg` 搜索本任务删除/重命名的字段、Codec 键、翻译键和方法名，不能只改编译器
   报错指出的位置。
3. 先增加或改写能在旧行为上失败的定向测试，再改实现。若测试受 Forge 环境限制，应提取 Forge 无关的纯模型，而不是
   省略关键状态机测试。
4. 每完成一个小阶段立即跑对应定向测试；任务结束再跑 town 包和编译。不得一次改完 W01-W06 后才首次编译。
5. 旧开发存档无需迁移，因此删除旧字段时直接修改 Codec/NBT，不要保留双读、兼容构造器或废弃壳。仍需保留现有的
   畸形条目隔离、重复 endpoint 拒绝和 `MAX_RESERVATIONS=4096` 防护。
6. 只有 W08 才把最终行为写入 living docs；前置任务只更新本计划的状态、实际偏离和测试证据。
7. 任一任务结束时必须执行 `git diff --check`，并用 `rg` 确认该任务声明删除的旧符号已经归零。

### 2. 必须保持的状态机

设备连接状态、预约状态和全镇短缺是三件不同的事，不能合并成一个布尔值：

| 情况 | `townProvider` | 保存的设置速率 | metric | 实时占用 | `TransportAdmissionStatus` | 是否搬运/监听 |
|---|---|---:|---:|---:|---|---|
| 未认领设备 | `null` | 无预约 | 无 | `0` | 无预约 | 否 |
| 已认领但城镇解析暂时失败 | 保留 | 保留旧预约 | 不更新 | 保留城镇权威状态 | 保留 | 设备侧停止 |
| 城镇没有有效仓库 | 保留 | 新设备为 `0`；旧设备保留原值 | `0` 占位，不表示零距离 | `0` | `UNAVAILABLE` | 否 |
| 有仓库且玩家设置 `rate=0` | 保留 | `0` | 当前 `D_eff` | `0` | `DISABLED` | 不搬运；Watcher 可连接 |
| 有仓库且 `rate>0` | 保留 | 原值 | 当前 `D_eff` | `R * F_d` | `ACTIVE` | 是 |
| 全镇运力短缺 | 保留 | 不变 | 不变 | 不变 | 仍为 `ACTIVE` | 按全镇比例限速 |

必须按以下不变量实现构造器、Codec 解码和状态替换：

- `ACTIVE` 要求 `rateItemsPerSecond > 0`，metric 和占用均为有限非负数。
- `DISABLED` 要求 `rateItemsPerSecond == 0`、占用为 `0`；metric 保存当前可定义的 `D_eff`，供界面显示。
- `UNAVAILABLE` 允许保存 `rateItemsPerSecond >= 0`，但 metric 固定为 `0`、占用固定为 `0`。界面必须根据状态隐藏该占位值，
  不能显示成“距离 0”。
- 短缺不是第三套速率，也不是预约状态。唯一保存的速率仍是玩家设置值；有效速率始终临时派生为
  `rateItemsPerSecond * TownTransportSummary.effectiveRateScale()`。
- 总运力为 `0` 但存在有效仓库时属于短缺/限速，不属于 `UNAVAILABLE`。仓库可用性只由仓库拓扑决定。
- 有效仓库恢复时，保存速率为 `0` 的预约转为 `DISABLED`；保存速率大于 `0` 的预约直接转为 `ACTIVE` 并重算占用，
  即使这会让全镇进入短缺。事实刷新不得因运力不足拒绝恢复。
- 玩家在 `UNAVAILABLE` 状态只能明确设置 `0`；其他速率修改拒绝且不改变任何字段。没有距离时不能预存一个未经准入的
  非零新速率。
- 已保存的非零速率可能因之后下调配置上限而高于新上限。配置/拓扑事实重算使用
  `capacityForStoredRate` 保留它；只有新的玩家输入才使用 `isRateValid` 拒绝超界值。

### 3. 权威数据边界

客户端和方块实体都不是距离与占用的权威来源：

- `TransportEndpointRequest` 最终只包含 `endpointId`、`endpointKind` 和玩家请求的 `rateItemsPerSecond`。删除
  `boundWarehouseCorePos` 和 `scaleMetric`；不要用另一个名字把它们继续留在请求里。
- 菜单只把整数速率交给服务端方块实体。方块实体从自身 `level.dimension()` 与 `worldPosition` 构造 endpoint ID；客户端
  不能提交 endpoint 坐标、仓库列表、metric、距离因子、占用或剩余运力。
- `TeamTown` 在处理请求前强制准备当前参数与仓库拓扑，再从 endpoint ID 和已应用快照派生 metric。只有 `TeamTown`/
  `TownTransportState` 可以替换预约和总占用缓存。
- `TransportReservation.CODEC` 是磁盘状态，不编码派生的 `reservedTransportCapacity`；
  `TransportReservation.SNAPSHOT_CODEC` 是网络状态，包含服务端已经计算好的占用。两者不得互换。
- `TownTransportSnapshot` 继续是全量同步和增量同步的共同权威载荷。客户端不得读取本地 `FHConfig` 重算距离因子；W06
  应在 snapshot 中同步 `effectiveWarehouseCount` 和 `warehouseDistanceCostPerBlock`，接口菜单 view 直接同步服务端算出的
  `distanceFactor`。
- `WarehouseLevelEmitter` 在任何层都不能构造 `TransportEndpointId`、`TransportEndpointRequest` 或
  `TransportReservation`，也不能出现在 snapshot 的 reservations 中。

### 4. 拓扑值对象与数值算法

建议新增 Forge 无关的不可变值对象，名称可按现有包习惯调整，但字段和职责不能混淆：

```text
WarehouseTopologyEntry(corePos: immutable BlockPos, capacityWeight: double)
WarehouseTopologySnapshot(townDimension: ResourceKey<Level>?, entries: immutable sorted List<Entry>)
```

- `townDimension` 使用队伍发电塔的维度，这是当前玩家城镇唯一的维度概念；解析方式必须与
  `CitizenSimScheduler#isTownInLevel` 一致。没有可解析的城镇维度时快照不可用，不能默认成接口所在维度。
- 从 `TeamTownData.buildings` 构造 entries，只选择 `WarehouseBuilding#isBuildingWorkable()` 且 capacity 有限并严格大于
  `0` 的建筑。禁止调用 `level.isLoaded`、读取仓库 BE 或强制加载区块。
- 构造时对坐标调用 `immutable()`，按 `(x, y, z)` 字典序排序并 `List.copyOf`。快照相等采用字段精确相等；不要用 epsilon
  吞掉真实容量变化。
- `entries.isEmpty()` 表示无有效仓库；`townDimension == null` 表示城镇维度不可解析。两者都令接口不可用，但 UI 原因可
  分别显示为“城镇不可用”和“城镇无可用仓库”。

加权距离不要直接计算可能溢出的 `K_i * D_i`。按以下等价算法实现纯函数：

```text
weightedDistance(endpointPos, sortedEntries):
    if entries empty: return empty
    maxWeight = max(entry.capacityWeight)
    if maxWeight is not finite or maxWeight <= 0: return empty

    numerator = 0.0
    denominator = 0.0
    for entry in sortedEntries:
        dx = abs((long) endpoint.x - (long) entry.x)
        dy = abs((long) endpoint.y - (long) entry.y)
        dz = abs((long) endpoint.z - (long) entry.z)
        distance = (double) (dx + dy + dz)
        normalizedWeight = entry.capacityWeight / maxWeight
        numerator += normalizedWeight * distance
        denominator += normalizedWeight
        if any intermediate is not finite or is negative: return empty

    if denominator <= 0: return empty
    result = numerator / denominator
    return result only when finite and non-negative
```

这样既保持 `sum(K_i*D_i)/sum(K_i)` 的玩法语义和容量同比缩放不变性，也避免容量与距离先相乘造成不必要的 overflow。
坐标必须先转 `long` 再相减和取绝对值；不得对 `int` 差值调用 `Math.abs`。计算结果不取整、不量化、不缓存到设备 NBT。

### 5. 拓扑缓存必须放在哪里

`TeamTown` 只是 `TeamTownData` 的短生命周期包装器，`TeamTownData#createTeamTown()` 会频繁创建新实例。因此以下字段必须
是 `TeamTownData` 上的 `transient` 运行时字段，绝不能放在 `TeamTown`、Codec 或 static 全局表中：

```text
warehouseTopologyDirty = true
warehouseTopologyInitialized = false
warehouseTopologyRefreshInProgress = false
appliedWarehouseTopology
loadedWarehouseAutomationDevices: Map<GlobalPos, WarehouseTopologyListener>
```

建筑变化已有统一链路：`WarehouseBuilding` setter/`AbstractTownBuilding` setter -> `fireChange` ->
`TeamTownData.DataSyncCache#onBuildingChange`。在该回调和 `buildings` Map 的 add/remove 回调中把
`warehouseTopologyDirty=true`，用它覆盖容量、结构有效性、初始化、重叠、添加、移除和同位置替换。不要在
`WarehouseBlockEntity#refresh`、`reloadMaxCapacity`、`checkOccupiedAreaOverlap` 等位置各复制一套刷新逻辑；这些入口只需
确保现有 setter 能 fire。解码阶段监听尚未接线，所以 dirty 默认必须为 `true`，第一次服务端 prepare 会建立基线。

### 6. 一次拓扑刷新的原子顺序

在 `TeamTownData.tick` 中，拓扑 prepare 必须发生在 transport 增量包 drain 之前。其他查询/设备入口也可调用同一个
幂等 prepare。实现顺序固定如下：

```text
refreshWarehouseTopologyIfDirty(parameters, authoritativeTownDimension):
    if refreshInProgress: return
    if initialized and not dirty: return                  // O(1) 空闲路径
    refreshInProgress = true
    try:
        candidate = buildStableSnapshot(buildings, authoritativeTownDimension)
        dirty = false                                     // 回调期间的新变化可重新置 true
        if initialized and candidate == applied: return   // 不标脏、不通知

        applied = candidate
        initialized = true

        build a replacement map for every WAREHOUSE_INTERFACE reservation
        for each endpoint in TransportEndpointId.STABLE_COMPARATOR order:
            if dimension unavailable/mismatch or entries empty:
                preserve rate; set metric=0, reserved=0, status=UNAVAILABLE
            else:
                metric = weightedDistance(endpoint.pos, entries)
                if metric invalid: use UNAVAILABLE; never persist NaN/Infinity
                else if saved rate == 0:
                    set metric; reserved=0; status=DISABLED
                else:
                    set metric; reserved=capacityForStoredRate(...); status=ACTIVE

        apply all replacements to TownTransportState in one batch
        recompute reservedTransportCapacity once
        mark transport sync dirty at most once, only if at least one record changed

        copy listeners before callbacks; notify each loaded device once
    finally:
        refreshInProgress = false
```

批量 API 不得在循环内调用现有 `replaceAndMark`，否则会重复调整总量和重复标脏。建议在 `TownTransportState` 增加一个只供
`TeamTown`/`TeamTownData` 使用的 batch replace 方法：先生成完整 replacement，再一次比较、替换、求和并返回
`boolean changed`。监听器回调必须发生在预约状态应用之后，使设备回调读取到新状态。回调前对 registry values 做快照，
避免回调注销自身引发 `ConcurrentModificationException`；单个失效监听器应记录警告并继续通知其他设备。

### 7. 已加载设备监听器注册规则

注册表以设备物理 `GlobalPos` 为键、当前 BE 实例为值：

- `register(pos, listener)` 覆盖同位置旧实例，处理同位置方块实体替换。
- `unregister(pos, listener)` 只在 map 当前值与传入实例相同时删除，避免旧实例稍晚 unload 时误删新实例。
- `onLoad` 和成功 claim 后注册；普通 unload、物理 break、显式 unbind 时注销。
- 拓扑为空时设备仍保持注册，这样仓库恢复能够即时唤醒；只释放 Watcher 和运行时搬运预算。
- 注册表不持久化、不下发客户端、不保存未加载设备坐标、不调用 `level.getChunk`。未加载接口由预约 map 更新；未加载
  发信器没有实际红石输出，下一次 `onLoad` 主动读取最新拓扑和库存。
- 方块实体 tick 禁止扫描 `TeamTownData.buildings`。接口 tick 只做已有的搬运/视觉净变化，发信器不新增 tick 扫描。

### 8. 认领、打开菜单与命令权限

认领和访问必须走同一服务端 helper，不能由两个方块各自实现略有差异的判断。推荐流程：

```text
claimOrAuthorize(ServerPlayer player):
    reject unless server side, same ServerLevel, live BE at exact BlockPos, distance <= 8 blocks
    holder = CTeamDataManager.get(player); reject if null
    reject unless holder's generator/town dimension == BE level.dimension()
    if townProvider == null:
        set townProvider = new TeamTownProvider(holder.getId())
        setChanged; register topology listener; reconcile watcher/reservation
        return true
    return townProvider is TeamTownProvider and townProvider.ownsTeam(player)
```

- `setPlacedBy` 只在服务端且 placer 是 `ServerPlayer` 时调用该 helper。接口认领后再处理默认速率准入和定向失败提示；
  发信器认领后只建立 Watcher。
- 两种 Block 必须在调用 `CGuiBlock#use` 打开菜单之前执行授权。其他队伍不仅不能改配置，也不能打开菜单查看 filter、目标、
  库存或状态。未绑定设备由首次合法主手交互认领。
- 不要因为 `townProvider.getTown()` 暂时返回 `null` 就清除 provider 或允许其他队接管。已保存 owner UUID 是所有权事实；
  解析失败只令设备不可用，直到原队伍恢复或设备被明确解绑/破坏。
- 两种 Menu 的所有 `receiveMessage` 必须先通过统一 guard：`menuPlayer.containerMenu == this`、服务端玩家、同 level、距离
  `<=64.0` 平方、当前坐标仍是同一 BE、BE 未 removed、玩家仍属于 provider 队伍。`CBlockEntityMenu#stillValid` 目前只检查
  `!isRemoved()`，必须覆写为同一 guard，使换队、离开范围或 BE 替换会关闭菜单。
- guard 必须位于 switch 之前。接口的 target、clear、amount、redstone、rate 和发信器的 filter、clear、threshold、mode
  全部使用它；不能只在 `setTransportRate` 内补权限。
- 参数在服务端再次限界：接口 slot `0..8`、数量沿用现有合法范围、速率为 `0` 或配置 min..max、发信器 threshold 为
  `1..Integer.MAX_VALUE`、mode 只由服务端枚举循环。非法值必须零状态变化。
- carried stack 只从服务端菜单的 `getCarried()` 读取并转换为 count=1 的精确 `SimpleItemKey`；不能信任客户端提交的
  item/NBT payload。

### 9. 设备生命周期清理矩阵

| 事件 | topology listener | Watcher | 接口预约 | `townProvider`/配置 | 接口库存 | 发信器红石 |
|---|---|---|---|---|---|---|
| `onLoad` | 注册/覆盖 | 拓扑可用时创建一次 | 对账但不重复创建 | 保留 | 保留 | 主动拉库存后判定 |
| 拓扑变空 | 保持注册 | reset 并置 `null` | 转 `UNAVAILABLE`，保留速率 | 保留 | 保留 | 立即变 `0` |
| 普通 chunk unload | 注销 | reset 并置 `null` | 保留 | 全部保留 | 保留 | 不额外持久化关断；加载时重判 |
| 方块被破坏/替换 | 注销 | reset 并置 `null` | 接口按 endpoint ID 注销 | 清除 | 九槽各掉落一次并清空 | 清零，必要时通知邻居 |
| 显式解绑城镇 | 注销 | reset 并置 `null` | 接口按 endpoint ID 注销 | provider 清除，配置保留 | 保留 | 立即变 `0` |
| provider 暂时解析失败 | 尽力保留已有注册 | reset 并置 `null` | 不伪造新状态 | provider 不清除 | 保留 | 立即变 `0` |

继续沿用当前 `onRemoved` 的区分方式：当前位置仍是同种方块表示 chunk unload，否则表示 break/replacement。不要在 unload
路径调用完整 `clearBinding`，否则会产生幽灵注销、物品掉落或所有权丢失。接口的 `TransportTransferBudget` 在 unload、
不可用、禁用、解绑和 break 时 reset，恢复后从 `0` 开始，防止离线累计突发。

### 10. Watcher 与发信器副作用

- 创建新 Watcher 前必须 reset 旧 Watcher；`watcher != null` 时重复 ensure 直接返回。拓扑净变化但仍可用时不要无条件
  重建 Watcher，只主动刷新库存状态。
- 接口和发信器只在城镇、维度和仓库拓扑都可用时连接 `TeamTownResourceHolder`。发信器 filter 为空时输出 `0`；可以保留
  已创建但未订阅 key 的 Watcher，不能轮询库存。
- 发信器比较式固定为：`HIGH_SIGNAL: stock >= threshold`，`LOW_SIGNAL: stock < threshold`。两式在边界上互补，禁止写成
  `>`/`<=`。
- `setEmitterState(on, stock)` 必须分别计算 `stockChanged` 和 `outputChanged`。任一变化都更新字段并 `setChanged()`，这样
  当前库存展示与 NBT 不会停留在旧值；只有 `outputChanged` 才调用红石邻居更新。
- 发信器当前向所有方向提供 `15` 级 weak/direct signal；`FACING` 仅负责模型方向。不得把邻居通知或信号查询改成只对正面。
- `onLoad`、filter/threshold/mode 变化、Watcher 回调和拓扑恢复都调用同一个服务端 `refreshState`，避免五套比较逻辑发生
  边界差异。

### 11. Codec、同步与 UI 数据

- W02 直接从 reservation 的磁盘 Codec 和 snapshot Codec 删除 `boundWarehouseCorePos`；W03 直接从两种 BE NBT 删除
  `warehousePos`；W05 直接从 `WarehouseBuilding.CODEC` 删除 `interfaces`/`emitters`。这是未发布数据，不写兼容读取。
- 增加 `UNAVAILABLE` 后，Codec 只接受精确枚举名。未知状态继续让该 reservation 条目被 tolerant list 丢弃，不能把未知值
  静默映射为 `ACTIVE`。
- full sync 继续由 `TeamTownDataS2CPacket` 附带 `TownTransportSnapshot`，incremental sync 继续由
  `TownResourceUpdatePacket` 附带同一 Codec。新增字段必须同时覆盖两包的 encode/decode/apply 交错测试。
- `reservedTransportCapacity`、distance factor、有效速率和剩余运力都是服务端派生展示值。客户端只格式化，不重新跑准入、
  拓扑或 config 公式。
- 镇长印章的“实时运力”读取当前 snapshot；“最近晨间结算”读取 immutable daily report。不要拿晨间占用覆盖实时占用。
- `UNAVAILABLE` reservation 仍保留在 endpoint 详情和“已登记设备”计数中，但不计入 reserved total。发信器永远不计入该
  设备数，因为它不是 transport endpoint。

### 12. 任务交接门槛

后续执行者不得只写“测试通过”。每个 W 任务结束时在对应任务下追加：实际修改文件、关键 API 最终签名、定向测试命令
与 tests/failures/errors/skipped 数量、未执行项目及原因、与计划的偏离。只有完成门槛全部满足后才能把状态从 `pending`
改为 `completed`；部分完成保持 `in-progress`，并逐项列出剩余工作。

## Detailed Task Contracts

### W00: 建立改造前基线

- 状态：`completed`（2026-08-22）
- 复杂度：`S`
- 建议推理强度：`medium`
- 关键锚点：Git `HEAD`/工作区、`WarehouseBlockScanner`、`WarehouseBuilding`、`WarehouseBlockEntity`、
  `WarehouseInterfaceBlockEntity`、`WarehouseLevelEmitterBlockEntity`、`TransportReservation`、现有相关测试

关键技术点：

- 记录当前提交、分支和已有未跟踪文件，保证后续实现不吸收无关工作区内容。
- 固定当前墙面发现链：scanner 候选 -> wall direction -> warehouse publish -> device `tryBind` -> building position Set。
- 固定当前预约链：接口 endpoint `GlobalPos`、单仓 `boundWarehouseCorePos`、仓库体积平方根 metric、按核心批量注销。
- 枚举当前接口、运力和发信器自动化覆盖；若发信器没有测试，明确登记为基线缺口而不是伪记通过。
- 运行接口/运力定向测试、town 包、完整测试、`compileJava` 和 `git diff --check`；记录精确测试数量和失败状态。

完成门槛：当前行为、改造锚点、工作区边界和自动化基线可搜索；不修改玩法源码。失败的既有测试必须先判断是否为
基线故障，不得进入 W01 掩盖。

执行结果（2026-08-22）：

- Git 基线为分支 `master`、提交 `71d42b7e482b7692a194710409056ed5cf2d65b6`；执行前后均无已跟踪源码改动。
  工作区原有未跟踪目录和文件不属于本计划，本次只新增并维护此计划文件。
- 当前墙面绑定基线已确认：`WarehouseBlockScanner` 只收集仓库外壁候选并计算朝向，`WarehouseBlockEntity`
  发布接口和发信器，`WarehouseBuilding` 持久化两类设备坐标，设备通过单个仓库核心完成绑定。
- 当前接口运力基线已确认：`WarehouseInterfaceBlockEntity` 保存 `townProvider` 和 `warehousePos`，以绑定仓库体积的
  平方根作为距离 metric，并以接口 `GlobalPos` 作为 transport endpoint；仓库失效时按核心批量解除绑定。
- 当前发信器基线已确认：`WarehouseLevelEmitterBlockEntity` 依赖单仓绑定，但实际读取城镇级库存；
  `WarehouseLevelEmitterBlock` 向所有方向输出同一红石强度。源码中没有 `WarehouseLevelEmitter*Test`，这是 W07
  必须补齐的明确覆盖缺口。菜单消息目前也没有城镇成员权限检查，归入 W03。
- 接口/运力定向测试通过：`59` tests，`0` failures，`0` errors，`0` skipped。
- `com.teammoeg.frostedheart.content.town.*` 测试通过：`341` tests，`0` failures，`0` errors，`0` skipped。
- 完整测试通过：`437` tests，`0` failures，`0` errors，`0` skipped；同一命令中的 `compileJava` 成功。
- `git diff --check` 通过。W00 未修改玩法源码、资源或 living docs；当前实现范围和缺口足以进入 W01。

### W01: 容量加权距离纯模型与参数

- 状态：`completed`（2026-08-22）
- 复杂度：`M`
- 建议推理强度：`high`
- 依赖：`W00`
- 关键锚点：`TransportReservationModel`、`TransportConsumerParameters`、`TownModelParameters`、
  `TownStageZeroAudit`、`FHConfig.Server.Town.TransportConsumers`

关键技术点：

- 建立 Forge 无关的不可变仓库拓扑 entry 输入，至少包含 immutable 核心 `BlockPos` 和有限正容量权重，并以
  `(x,y,z)` 稳定排序。
- 以 Mandatory Implementation Playbook 第 4 节的 max-weight 归一化算法实现
  `sum(K_i * D_i) / sum(K_i)`；坐标差先扩为 `long`。不要直接乘原始 `K_i * D_i`。
- 将 `warehouseScaleCostPerMetric` 全链路改名为 `warehouseDistanceCostPerBlock`，单位固定为 `1/block`，源码默认保持
  `0.05`；更新 Stage 0 审计单位和 Stage 4 参数 JSON 锚点。
- 删除 `sqrt(volume)` 公式和相应测试，但本任务不修改 reservation Codec、设备绑定或仓库扫描。

执行顺序：

1. 先在 `TransportReservationModelTest` 写单仓、两仓加权、容量同比缩放、输入乱序、极端坐标、极大容量、空/非法权重
   测试，确认旧 `sqrt(volume)` 实现失败。
2. 新增最小 entry/距离纯模型；先完成输入过滤、immutable copy 和稳定排序，再写加权循环。
3. 重命名 `TransportConsumerParameters` record component 和校验消息，再依次修改 `TownModelParameters` 默认值、
   `TownStageZeroAudit` 名称/单位、`FHConfig` 字段与配置键、`TeamTown#currentTransportParameters`。
4. 更新 `TownModelParameterDefaultsTest`、`TownStageZeroAuditTest`、`TownStageFourModelTest`；最后用 `rg` 确认源码和测试中
   `warehouseScaleCostPerMetric` 与 `warehouseScaleMetric` 均为零处。
5. 本任务不得碰 `TransportReservation`、设备 NBT、仓库 Codec 或墙面扫描；若编译迫使修改这些区域，说明任务边界设计
   有误，应停止并更新计划。

完成门槛：纯模型和参数定向测试先失败后通过；任意输入顺序结果一致；单仓库、加权示例和同比缩放不变性锁定；
编译通过。

执行结果（2026-08-22）：

- 失败先行证据：先改写 `TransportReservationModelTest` 后运行定向测试，`compileTestJava` 失败于
  `找不到符号: 类 WarehouseTopologyEntry`（旧行为无法满足新公式测试）。
- 新增 `WarehouseTopologyEntry`（transport 包）：immutable `BlockPos corePos` + `double capacityWeight`，
  附 `CORE_POS_ORDER` (x,y,z) 字典序比较器；权重校验刻意宽松，由距离函数与 W04 快照构造器负责过滤。
- `TransportReservationModel`：删除 `warehouseScaleMetric(double)`；新增
  `warehouseWeightedDistance(BlockPos, Collection<WarehouseTopologyEntry>)`，按 Playbook §4 max-weight 归一化
  算法实现（内部按核心坐标稳定排序，坐标差先扩 `long`，任何非法输入返回 `NaN`）；
  `warehouseScaleFactor` 更名 `warehouseDistanceFactor`，改用 `warehouseDistanceCostPerBlock`。
- 参数全链路重命名：`TransportConsumerParameters.warehouseDistanceCostPerBlock`（含校验消息）、
  `TownModelParameters.Defaults.TRANSPORT_CONSUMER_WAREHOUSE_DISTANCE_COST_PER_BLOCK`（值仍 `0.05`）、
  `TownStageZeroAudit` 键 `transportConsumers.warehouseDistanceCostPerBlock`（单位 `1/block`）、
  `FHConfig` 字段/配置键/注释、`TeamTown#currentTransportParameters`。
- 与计划的偏离（已声明）：`WarehouseInterfaceBlockEntity` 两处旧 metric 调用是唯一编译依赖，W01 对其做了
  单仓条目的最小过渡；W02 已删除该过渡和方块实体提交 metric 的路径。绑定 NBT 与墙面扫描仍留给 W03/W05。
- 定向测试通过：`TransportReservationModelTest` 10、`TownModelParameterDefaultsTest` 1、
  `TownStageZeroAuditTest` 4、`TownStageFourModelTest` 12，合计 `27` tests，`0` failures，`0` errors，
  `0` skipped。
- town 包回归通过：`343` tests，`0` failures，`0` errors，`0` skipped；
  编译随 test 任务通过。
- `git diff --check` 通过；`rg` 确认 `warehouseScaleCostPerMetric`/`warehouseScaleMetric`/
  `warehouseScaleFactor` 在 `src/` 为零处。

### W02: 预约合同去单仓化与不可用语义

- 状态：`completed`（2026-08-22）
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 依赖：`W01`
- 关键锚点：`TransportReservation`、`TransportEndpointRequest`、`TransportAdmissionStatus`、
  `TransportReservationResult`、`TownTransportState`、`TeamTown`

关键技术点：

- 从持久化和 snapshot Codec、request/result 校验及 `TeamTown` API 中删除 `boundWarehouseCorePos`；不保留伪造的
  “代表仓库”坐标。
- 仓库接口距离必须由 `TeamTown` 根据 endpoint 和当前仓库拓扑派生，调用者不能提交占用或伪造权威距离；W02 冻结
  `TransportEndpointRequest` 的最终最小字段，随后 W03/W04 只消费该合同。
- 在 `TransportAdmissionStatus` 增加精确枚举值 `UNAVAILABLE`：可保留非零单一设置速率，但 metric/占用固定 `0`、不参与
  短缺、不允许传输；`DISABLED` 只表示玩家设置 `rate=0` 且当前有有效仓库。
- 玩家新增/上调继续走准入；已有 endpoint 的拓扑事实刷新不走准入拒绝，可直接造成比例限速。
- 直接采用新 Codec，不兼容未发布的旧开发存档；损坏、重复、未知状态和 snapshot 上限策略保持现有防护。
- 删除按仓库核心批量注销 API；保留单 endpoint 注销和未来按 endpoint kind 的受控批处理能力。

执行顺序：

1. 先为 `ACTIVE/DISABLED/UNAVAILABLE` 的合法与非法字段组合写构造器和 Codec 测试；特别锁定 unavailable 非零速率、
   metric/占用为零，以及 unknown enum 被 tolerant list 仅丢弃单条记录。
2. 修改 `TransportAdmissionStatus` 和 `TransportReservation` 不变量；持久 Codec 仍排除 derived capacity，snapshot Codec
   仍包含它。不要通过放宽所有构造器校验来让新状态通过。
3. 把 `TransportEndpointRequest` 缩成 endpoint ID、kind、rate。让 `TeamTown` 从 `data.buildings` 和服务端已验证的城镇
   维度即时构造 W01 entry 并派生 metric；这个低频请求路径可在 W04 前临时 `O(W)`，但不得把拓扑交给 BE 构造。
4. 分开两个 mutation 入口：玩家 request 走 admission，服务器 topology refresh 走无 admission 的事实替换。事实入口即使
   令 reserved total 超过 total capacity 也必须成功。
5. 修改 `TownTransportState#recalculateReservedCapacities`：`ACTIVE` 重算，`DISABLED/UNAVAILABLE` 保持占用 `0`；配置上限
   下调时不得删除已有高于新上限的 stored rate。
6. 删除 `TeamTown#unregisterTransportEndpointsBoundTo` 两个重载和所有调用者。用 `rg` 同时确认 Java、测试、翻译和计划外
   living docs 中还剩哪些旧符号；living docs 留到 W08 改，源码/测试必须归零。

完成门槛：新 Codec 往返、禁用/不可用/活动状态、上调拒绝零状态变化、拓扑事实刷新和全量/增量 snapshot 测试通过；
源码不再出现 reservation 的 `boundWarehouseCorePos`。

执行结果（2026-08-22）：

- `TransportEndpointRequest` 已冻结为 `endpointId + endpointKind + rateItemsPerSecond`；
  `TransportReservation` 的磁盘与 snapshot Codec 均删除单仓核心字段，磁盘 Codec 继续排除派生占用。
- 新增 `TransportAdmissionStatus.UNAVAILABLE` 并锁定三态不变量：`ACTIVE` 非零速率、`DISABLED` 零速率和零占用、
  `UNAVAILABLE` 保留单一设置速率但 metric/占用为零。配置重算只为 `ACTIVE` 重算占用。
- `TeamTown` 临时从全部可工作且容量为有限正数的仓库 `O(W)` 派生容量加权距离；玩家请求继续准入，
  `refreshTransportEndpointMetric` 作为无准入的事实刷新，可恢复保存速率并造成全镇比例限速。
- 无有效仓库时，新端点以零速率进入 `UNAVAILABLE`；已有端点保留速率，非零玩家修改以
  `INVALID_BINDING` 拒绝且不改变状态。删除了按仓库核心批量注销 API。
- 修复 `TownTransportState` 容错列表解码：逐条解析预约，未知枚举或单条损坏只丢弃该条，后续合法记录保留。
- 接口 view 将 `UNAVAILABLE` 映射为仓库不可用且有效速率为零；镇长印章详情删除绑定核心行。
- 定向验证通过：预约模型/状态、城镇准入与结算、仓库移除、接口 view、全量/增量网络快照和镇长印章，
  合计 `52` tests，`0` failures，`0` errors，`0` skipped；`compileJava`、`compileTestJava`、
  `git diff --check` 通过。
- `rg` 确认 `src/main` 与 `src/test` 中不再访问 `boundWarehouseCorePos`；唯一命中是明确验证编码结果不含该键的测试。

### W03: 两种设备的城镇认领、权限与 Watcher

- 状态：`completed`（2026-08-22）
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 依赖：`W02`, `W04`
- 关键锚点：`WarehouseInterfaceBlock`/`BlockEntity`/`Menu`、`WarehouseLevelEmitterBlock`/`BlockEntity`/`Menu`、
  `TeamTownProvider`、`TeamTownResourceHolder`、`IWarehouseStockWatcherNode`

关键技术点：

- 两种 Block 的 `setPlacedBy` 在服务端从放置者团队创建 `TeamTownProvider`；无玩家上下文保持未绑定，首次合法交互可
  认领，已绑定设备不能被另一团队覆盖。
- 两种 BE 只持久化 `townProvider` 和自身配置，删除 `warehousePos` 与单仓 `BindingContext`；共用一条城镇存在、同维度、
  至少一座有效仓库的服务端可用性判断。
- 接口从城镇直接创建 Watcher、执行 transport 准入和共享搬运预算；发信器只创建精确库存 Watcher并输出红石，不创建
  endpoint 或 reservation。
- 建立统一服务端命令守卫：操作者、所属队伍、同 level、距离 `<=8 blocks`、当前 BE、当前有效菜单和参数范围全部通过
  才能修改。覆盖接口目标/数量/红石/速率和发信器过滤/阈值/模式，不允许只保护速率。
- 普通 unload 释放 Watcher/运行时状态但保留归属与配置；break/replacement 执行各自完整清理，接口保持物品掉落安全。
- 发信器红石输出净变化时才 `setChanged` 和通知邻居；无仓库/无城镇时立即输出 `0`。

执行顺序：

1. 先增加共享的服务端 owner/dimension/access helper，并为无队伍、错误队伍、错误维度、超距、旧菜单、BE 替换和合法
   成员建立纯判断测试。不要先复制两份 `use`/menu 判断。
2. 改两种 Block 的 `setPlacedBy` 和 `use`：先认领/授权，后调用 `CGuiBlock#use`。跨队失败必须消费交互但不打开菜单；
   客户端只返回对应 sided result，不写 provider。
3. 改接口 BE：删除 `warehousePos`/单仓 context，认领成功后向 W04 registry 注册，按拓扑建立 reservation 和 Watcher；
   `validateAndBalance` 的所有早退路径都要检查 reservation 为 `ACTIVE` 且仓库拓扑可用。
4. 改发信器 BE：删除单仓 context，仅使用城镇资源 holder；实现 Playbook 第 10 节的单一 `refreshState` 和 stock/output
   双变化处理。确认任何代码路径都没有 transport 类型引用。
5. 改两个 Menu：构造器保存 `menuPlayer`，`stillValid` 与 `receiveMessage` 共用同一 access helper；所有命令在 guard 后执行。
6. 最后实现 unload/break/unbind 清理矩阵，并用计数型 fake watcher 验证 create/reset 恰好次数。接口九槽物品用已有
   `suppressInventoryCallback` 路径清空，避免掉落时重新触发平衡。

完成门槛：任意位置认领、跨队拒绝、全部菜单命令权限、Watcher 重连、卸载/拆除和物品守恒自动化通过；发信器在任何
不可用路径均不能残留红石 `15`。

执行结果（2026-08-22）：

- 新增 `TownWarehouseDeviceAccess`，统一服务端认领、队伍归属、城镇维度、八格交互距离、当前方块实体和当前菜单验证；
  两种 Block 都只在服务端主手交互中认领或授权，跨队访问被消费且不打开菜单。
- 接口和发信器都只持久化 `townProvider` 与自身配置，不再写入 `warehousePos`。Provider 临时解析失败不会清除归属，
  仓库拓扑不可用时分别停止搬运/关断红石，拓扑恢复回调会重新建立 Watcher 并立即刷新状态。
- 接口继续独占 `WAREHOUSE_INTERFACE` 预约与九槽物品搬运；发信器只订阅精确 `SimpleItemKey` 库存，代码路径不创建
  transport endpoint。阈值边界固定为 high `stock >= threshold`、low `stock < threshold`。
- 两个 Menu 的全部命令都在共享访问守卫之后执行；非法速率、目标数量和阈值不会先修改状态。普通 unload 注销拓扑
  listener、释放 Watcher 和运行时预算，确认 break 才注销接口预约并掉落物品。
- 独立运行测试暴露并修复了 `WarehouseBuilding` 与 `ITownBuilding.CODEC` 的首次加载环，仓库分派现在和货运站一样使用
  lazy codec；`TeamTownWarehouseTopologyTest` 也显式加载内存服务端配置，不再依赖其他测试类的执行顺序。
- W03 定向 `29` tests 全通过；`compileJava`、`compileTestJava` 和 `git diff --check` 通过。旧 scanner/建筑拥有关系的
  临时调用壳仍由 W05 统一删除，避免在两阶段之间留下无法编译的半链路。

### W04: 仓库拓扑快照、事实刷新与设备通知

- 状态：`completed`（2026-08-22）
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 依赖：`W02`
- 关键锚点：`TeamTownData.buildings`、`TeamTown`、`TownTransportState`、`WarehouseBuilding`、
  `WarehouseBlockEntity#refresh`、`TeamTownData#checkOccupiedAreaOverlap`/`reloadMaxCapacity`

关键技术点：

- 从全部可工作、容量为有限正数的仓库构造稳定 `(corePos, capacity)` 快照；保存瞬态 applied snapshot/revision，不写入
  Codec。
- 快照净变化时批量重算所有 `WAREHOUSE_INTERFACE` endpoint，包括未加载接口；按稳定 endpoint 顺序重算，批量更新
  总占用，并且最多标记一次 transport dirty。
- 建立瞬态已加载设备监听注册表。接口和发信器 onLoad/claim 注册、unload/unbind 注销；拓扑变化时通知接口重连
  Watcher、通知发信器立即重新读取库存和红石。
- 通过 `DataSyncCache#onBuildingChange` 和 buildings Map add/remove 统一把 topology dirty 置真，从而覆盖扫描后容量/有效性、
  重叠、添加、移除、同位置替换和晨间修复；不能被 `reloadMaxCapacity` 的“总容量相等”提前返回吞掉。
- clean dirty flag 的空闲路径为 `O(1)`；只有收到建筑变化但最终快照相同的路径才做 `O(W)` 构造/比较。两者都不得遍历
  endpoint、标脏、发包或通知红石邻居；设备 tick 不扫描仓库。

执行顺序：

1. 先实现 `WarehouseTopologySnapshot` 的 immutable/sorted/equality 测试，包括“总容量相同但位置不同”“同位置容量分布
   改变”“不可工作和非法容量被排除”“未加载仓库仍保留”。
2. 在 `TeamTownData` 增加 Playbook 第 5 节列出的 transient 字段；初始化 dirty 必须为 true。把 topology dirty 接入现有
   building change 和 Map change 回调，不在仓库 BE 分散调用 refresh。
3. 在 `TownTransportState` 增加 batch replace，并用操作计数测试锁定：4096 endpoint 的一次拓扑变化只重算总量一次、
   transport dirty 最多一次。
4. 实现 prepare 顺序和 reentrancy guard，再接 `TeamTownData.tick` 的 transport packet drain 之前；首次存档解码后的查询
   也必须触发 prepare。
5. 实现以 `GlobalPos` 为键的 listener registry 及 identity-safe unregister；先用 fake listener 测试自注销、同位置替换、
   单个回调异常和稳定通知次数，再让 W03 的真实 BE 接入。
6. 最后删除 W02 的即时 `O(W)` 请求派生，改为只读取 `appliedWarehouseTopology`。任何 request 和事实刷新必须消费同一个
   snapshot 实例，避免准入时距离与随后显示距离不同。

完成门槛：近/远、大/小仓库和“总容量相同但位置/权重分布不同”全部正确；未加载接口即时更新占用；已加载发信器在
删除最后仓库/恢复仓库时即时关闭/恢复；空闲性能守卫通过。

执行结果（2026-08-22）：

- 新增 transient-only `WarehouseTopologySnapshot`：保存可空的权威城镇维度和 immutable、按 `(x,y,z)` 排序的
  `WarehouseTopologyEntry` 列表；只统计 `isBuildingWorkable()` 且容量为有限正数的仓库。
- `TeamTownData` 保存 dirty/initialized/reentrancy guard、applied snapshot 和以设备 `GlobalPos` 为键的监听注册表；
  字段不进入 Codec。权威维度由 `GeneratorData.dimension` 解析，与 `CitizenSimScheduler#isTownInLevel` 同口径。
- 建筑对象 fire 与 buildings map add/remove 都经 `DataSyncCache#onBuildingChange` 置 topology dirty；
  `TeamTown#addTownBlock/removeTownBlockInternal` 也覆盖监听初始化前的直接入口。
- `TeamTownData.tick` 在 transport 增量状态 drain 前执行拓扑 prepare。clean 路径仅比较 dirty 与维度，
  不构造列表、不遍历 endpoint、不标脏也不通知监听器。
- `TownTransportState#replaceReservations` 原子应用稳定顺序 replacement，一次重算总占用并记录本批使用的参数快照，
  避免 tick 随后再次全量参数重算。一次净拓扑变化最多 mark transport dirty 一次。
- 快照净变化会重算所有未加载/已加载的 `WAREHOUSE_INTERFACE` 预约；跨维、无维度、空拓扑和非法 metric 统一转
  `UNAVAILABLE` 并保留设置速率。恢复后零速率转 `DISABLED`，非零速率转 `ACTIVE`，不做准入拒绝。
- listener registry 支持同位置新实例覆盖、identity-safe unregister、回调前复制和单个异常隔离；真实接口/发信器
  的 onLoad/unload 接线留在依赖本任务的 W03。
- 自动化覆盖 immutable/sorted、无效仓库过滤、等总容量位置变化、跨维不可用、4096 endpoint 单批重算、
  空闲 prepare、回调自注销、同位置替换、旧实例注销和异常继续通知。最终 W04 定向 `21` tests 全通过；
  此前 town 包 checkpoint `352` tests 全通过；`compileJava`、`compileTestJava`、`git diff --check` 通过。

### W05: 从仓库结构和生命周期移除设备所有权

- 状态：`completed`（2026-08-22）
- 复杂度：`L`
- 建议推理强度：`high`
- 依赖：`W03`, `W04`
- 关键锚点：`WarehouseBlockScanner`、`WarehouseBuilding.CODEC`、`WarehouseBlockEntity`、
  `TeamTown#removeTownBlockInternal`、仓库移除与 Codec 测试

关键技术点：

- 删除 scanner 的接口/发信器候选及 wall direction 筛选；保留仓储架装饰统计。两种方块仍可作为合法墙体材料，
  “可构成墙”不等于“必须放在墙上”。
- 删除 `WarehouseBuilding.interfacePositions/emitterPositions`、Codec 字段、构造参数、replace/remove/contains 和
  `unbindLoadedDevices`。
- 删除 `WarehouseBlockEntity.publish/clearInterfaces`、`publish/clearEmitters`、恢复时逐设备唤醒和相关导入。
- 仓库拆除只更新建筑、最大容量和仓库拓扑；不注销或解绑城镇级设备。
- 清理所有旧单仓方法和测试夹具，不保留无人调用的兼容壳。

执行顺序：

1. 先改 scanner 测试，证明接口/发信器不再出现在扫描结果且把它们当普通合法墙体不会破坏房间扫描；仓储架装饰断言
   必须保持。
2. 删除 `WarehouseBlockScanner.interfaceCandidates/emitterCandidates`、两类 getter 和 wall-device helper；清理 import。
3. 从 `WarehouseBuilding.CODEC` 同时删除字段、getter、构造器参数、Set 和方法；直接更新所有 fixture 构造器，不保留旧
   overload。
4. 删除 `WarehouseBlockEntity` publish/clear/wakeup 全链，再删除 `TeamTown#removeTownBlockInternal` 中按核心解绑/注销逻辑。
   仓库移除后必须先让 building Map 产生 topology dirty，再由 W04 统一重算。
5. 运行 `rg -n "interfacePositions|emitterPositions|publishInterfaces|publishEmitters|warehousePos|boundWarehouseCorePos"`
   覆盖 `src/main` 与 `src/test`；此时除明确无关语境外应无旧所有权符号。

完成门槛：仓库 Codec/扫描/添加/拆除/同位置替换回归通过；设备不在墙面也能工作；拆任意仓库不会注销接口或关断仍有
其他有效仓库的发信器。

执行结果（2026-08-22）：

- `WarehouseBlockScanner` 只保留仓储架装饰统计；接口/发信器候选集合、墙面朝向筛选和 getter 已删除。两种设备方块仍
  作为普通非空气建筑方块参与房间边界，但扫描结果不再拥有它们。
- `WarehouseBuilding.CODEC`、字段和构造器已删除 `interfaces`/`emitters`；仓库逻辑对象只保存结构、容量、面积、体积和
  装饰状态。新增 Codec 回归明确断言新格式不写两类设备坐标。
- `WarehouseBlockEntity` 已删除发布、清理、逐设备唤醒和预约注销链；仓库恢复统一由 W04 拓扑 listener 通知。接口与
  发信器中的 `tryBind`/单仓 unbind 兼容壳也已删除。
- `TeamTown#removeTownBlockInternal` 不再先解绑仓库设备。拆除和同位置替换只移除建筑、标记拓扑 dirty 并执行通用建筑
  清理；现存 endpoint 由 W04 事实刷新转为新距离或 `UNAVAILABLE`，不会被删除。
- 旧所有权符号搜索只剩 `warehousePos` 的普通局部变量/测试变量以及两个负向 Codec 断言；W05 定向 `20` tests 全通过，
  `compileJava`、`compileTestJava` 和 `git diff --check` 通过。

### W06: 同步、界面、红石反馈与本地化

- 状态：`completed`（2026-08-22）
- 复杂度：`M`
- 建议推理强度：`high`
- 依赖：`W03`, `W04`
- 关键锚点：`WarehouseInterfaceTransportView`/`Screen`、`WarehouseLevelEmitterMenu`/`Screen`、
  `TownVirtualResourcesPanel`、`TownTransportSnapshot`、`zh_cn.json`、`en_us.json`

关键技术点：

- 接口紧凑区维持现有三类数据，不重新增加调试行；仓库不可用、未绑定城镇、禁用、限速和活动状态互斥且文案明确。
- 镇长印章设备详情删除绑定仓库核心，显示全镇有效仓库数、该接口容量加权平均距离、距离因子、设置/有效速率和占用。
- 发信器同步过滤、阈值、模式、当前库存、城镇连接状态和输出状态；文案改为“未绑定城镇/城镇无可用仓库”。
- 红石状态变化立即通知邻居；仅 UI 状态或库存数显示变化不得无条件重复通知邻居。
- 全量和增量 snapshot 使用同一 metric/占用语义；折叠、排序和滚动状态保持现有合同。

执行顺序：

1. 先扩 `TownTransportSnapshot`：同步有效仓库数和服务端 `warehouseDistanceCostPerBlock`；同步值必须校验 finite/non-negative，
   EMPTY 使用 `0`。同时扩 `WarehouseInterfaceTransportView`，直接携带服务端计算的 distance factor。
2. 同步改 `TeamTownDataS2CPacketTest` 与 `TownResourceUpdatePacketTest`，覆盖 full -> incremental -> replacement full 交错，
   确认新增字段不会回退或丢失。
3. 再改接口屏幕：仍只保留当前物品传输速率、占用运力、剩余/总运力；限速时速率红色，不显示 raw metric 调试字段。
   unavailable 时显示原因且不把 metric 占位 `0` 当成零距离。
4. 改镇长印章折叠详情：删核心坐标，显示有效仓库数、`D_eff`、`1+k_d*D_eff`、设置/有效速率、占用和状态；endpoint
   仍显示接口自身 `GlobalPos`。
5. 改发信器 UI 文案和同步。库存可大于 int 时必须明确沿用饱和显示还是扩为 long Codec；不得先 cast 溢出再 clamp。
6. 最后补齐 `zh_cn.json`/`en_us.json` 一一对应和资源测试；用 `rg` 确认玩家可见文案不再出现“绑定仓库核心”“仓库墙面”。

完成门槛：中英文资源、菜单 view/Codec、镇长印章展示模型、发信器状态和红石邻居净变化测试通过；默认和较大 GUI
scale 的最终视觉留给 H02。

执行结果（2026-08-22）：

- `TownTransportSnapshot` 新增 `effectiveWarehouseCount` 和服务端 `warehouseDistanceCostPerBlock`，Codec 拒绝负数仓库数与
  非有限/负系数。客户端 `TownTransportState` 只瞬态保存这两个展示元数据，不写入持久化 Codec。
- 全量与增量包都携带同一快照。网络回归覆盖 `full -> incremental -> replacement full`，确认预约派生占用、有效仓库数
  和距离系数一起收敛且不会回退；资源观察者仍只在资源与快照原子应用后回调一次。
- `WarehouseInterfaceTransportView` 直接携带服务端计算的 `distanceFactor` 并进行 finite/non-negative 校验。接口紧凑区
  保留状态、当前物品传输速率、占用运力和“剩余/总量”合并行，限速速率继续标红，不显示 raw metric。
- 镇长印章实时区显示全镇有效仓库数；默认收起的设备详情显示接口自身 `GlobalPos`、设置/有效速率、容量加权平均距离、
  距离因子、占用运力和状态，不再显示或本地化“绑定仓库核心”。不可用预约用 `-` 隐藏 metric 占位值。
- 发信器沿用饱和到 `Integer.MAX_VALUE` 的库存显示，避免 long -> int 溢出；状态文案改为“未绑定城镇/城镇无可用仓库”。
  红石输出仍只在净变化时通知邻居，库存显示变化不会无条件触发邻居更新。
- 中英文资源一一对应，旧墙面/单仓玩家文案与维护注释已清除。W06 定向 `21` tests 全通过，`compileJava`、
  `compileTestJava`、资源合并和 `git diff --check` 通过；最终视觉与实际邻居行为留给 H02 游戏内验收。

### W07: 自动化回归、性能与数据安全

- 状态：`completed`（2026-08-22）
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 依赖：`W05`, `W06`
- 关键锚点：全部 `Transport*Test`、`WarehouseInterface*Test`、新增 `WarehouseLevelEmitter*Test`、
  `TownBuildingRemovalTest`、网络包测试、Gradle 完整矩阵

关键技术点：

- 为当前完全缺失的发信器测试建立纯逻辑、绑定、Watcher、阈值两模式、红石关断/恢复、权限、Codec 和生命周期覆盖。
- 改写所有把墙面发现、单仓核心和 `sqrt(volume)` 当作正确行为的旧断言；不得通过删除断言降低覆盖。
- 交叉覆盖多接口、多发信器、多仓库、容量权重变化、最后仓库消失/恢复、未加载 endpoint、团队变化和服务器重启。
- 保留接口共享预算、部分 Action、空闲无突发和物品守恒；发信器不得执行库存 Action 或占用运力。
- 扩展 4096 endpoint 空闲守卫，并增加大量已加载发信器的无拓扑/无库存变化守卫；记录操作计数而非声称真实 TPS。
- 运行所有定向、town 包、完整测试、编译和差异检查；仅在模型/tag 资源实际变化时运行 `runData`。

执行顺序：

1. 先按 W01-W06 各自留下的测试补缺清单逐项核对，不要用一个“大集成测试”代替纯公式、状态机和 Codec 的小测试。
2. 新建发信器测试族，至少拆成阈值纯模型、菜单访问守卫、Watcher 生命周期、红石净变化和 NBT/菜单 Codec；若 BE 难以
   无游戏启动构造，把逻辑提取成 package-private 纯 helper。
3. 建立多仓/多设备交叉测试和 4096 endpoint batch 测试，显式计数 topology builds、endpoint recomputes、dirty marks、
   listener callbacks、watcher create/reset、neighbor updates。性能结论只依据这些计数。
4. 运行定向命令时先 `cleanTest`，避免旧 XML 被误计为本次结果；从 `build/test-results/test/*.xml` 汇总精确数量。
5. 最后运行 town 包、完整 `test compileJava`、`git diff --check` 和旧符号 `rg`。若 `runData` 被执行，先检查生成差异，
   不得提交与本系统无关的格式化/重生成文件。

完成门槛：新语义具有失败先行和通过证据；完整回归零失败/错误；编译与 `git diff --check` 通过；H02 才能开始。

执行结果（2026-08-22）：

- 新增 package-private `WarehouseLevelEmitterModel`，把阈值两模式、Watcher 精确物品过滤、long 库存饱和菜单显示和
  “库存变化/红石输出净变化”拆成 Forge 启动外可测逻辑；`WarehouseLevelEmitterBlockEntity` 只在输出位变化时通知邻居。
- 新增 `WarehouseLevelEmitterPersistence`，集中读写过滤物、阈值、模式、最近库存、输出位和 `TeamTownProvider`；回归覆盖
  精确 NBT 过滤、团队归属、阈值/模式规范化和不再写入 `warehousePos`。
- `TownWarehouseDeviceAccessTest` 覆盖当前/旧菜单、真实/替换或过远 BE、团队与维度组合；所有真实菜单命令继续复用同一
  服务端守卫。`TeamTownWarehouseTopologyTest` 新增 `4096` 个已加载 listener 的单批通知计数，并证明空闲 prepare 零回调、
  下一次净拓扑变化才产生下一批回调。
- 完整城镇回归发现并修复建筑 Codec 类初始化顺序问题：`CodecUtil.DispatchNameCodecBuilder#typeLazy` 在分发时才取得具体
  Codec，保留 `RecordCodecBuilder` 的内联 map 语义；全部 `ITownBuilding` 类型使用延迟注册，旧整数索引仓库标签仍可读取。
- W07 定向矩阵 `97` tests、城镇包 `367` tests、完整 Gradle 回归 `463` tests 均为 `0` failure/error；`compileJava` 通过。
  本次没有模型/tag 资源变化，因此未运行 `runData`。真实邻居通知、区块卸载/重载、重登/重启、GUI scale 与多人权限仍由
  H02 游戏内验收，不把纯逻辑覆盖冒充为游戏内通过。

### H02: 游戏内验收

- 状态：`completed`（2026-08-22，用户验收）
- 复杂度：`L`
- 执行方式：`手动`
- 依赖：`W07`

验收重点：

- 在仓库墙外、仓库内部、普通建筑旁和远距离位置放置两种设备；验证自动认领、首次认领、跨队拒绝和拆除。
- 用不同距离和容量的至少三座仓库核对加权距离；依次让仓库失效、恢复、扩建、缩建和拆除，检查接口占用与限速。
- 验证接口 `20/1280 items/s`、低速、小数限速、红石补货、目标满和导出/补货共享预算，检查物品守恒。
- 验证发信器精确 NBT 过滤、阈值滚轮、两种比较模式、全方向 `15` 级红石，以及最后仓库消失/恢复时即时关断/恢复。
- 覆盖区块卸载/重载、重登、重启、换队、无效菜单和中英文/GUI scale；记录预期、实际与环境限制。

完成门槛：所有当前可执行的单人场景有结果；任何幽灵占用、残留红石、越权、吞物或复制必须回到对应任务修复；无法
执行的包观测、磁盘写入或多人项目保持“未测”，不能写成通过。

执行结果（2026-08-22）：用户确认已完成游戏内验收并允许进入 W08，未报告需要继续修改的缺陷。对话未提供逐场景
验收记录，因此本计划不虚构各条目的单独通过证据；按既有环境限制，可观察包类型、磁盘写入和多人游戏仍记为“未测”。

### W08: 文档与交付收尾

- 状态：`completed`（2026-08-22）
- 复杂度：`S`
- 建议推理强度：`medium`
- 依赖：`H02`
- 关键锚点：本计划 Documentation Impact 中列出的 living docs、P2P 计划、`diary/README.md`

关键技术点：

- 只把最终源码行为写入 living docs：公式单位、城镇归属、拓扑派生、预约 Codec、Watcher、红石、权限、同步和生命周期。
- 更新本计划状态与 Outcome，记录任务偏离、自动化数量、H02 实测和明确“未测”项。
- 更新 P2P 草案的实际通用预约 API；新增 diary，记录决策、验证、文档影响和剩余风险。

完成门槛：源码、测试、living docs、计划 Outcome 和 diary 一致；没有把历史墙面/单仓语义残留为当前说明。

执行结果（2026-08-22）：

- `docs/town/town-model.md`、`docs/town/implementation-reference.md` 和 `docs/transport_station_design.md` 已记录最终公式、
  城镇归属、拓扑派生、预约 Codec、Watcher、红石、权限、同步和生命周期。
- P2P 草案已按实际 API 删除 `boundWarehouseCorePos` 和客户端 `scaleMetric` 请求字段，记录持久化/快照 Codec 差异，
  并明确当前 `TeamTown` 距离派生仍是仓库专用，P2P 必须先增加 kind-specific 服务端事实解析。
- H02 用户验收结果、自动化数量、明确未测项和最终交付状态已写回计划；新增 W08 diary。相关源码、测试、资源、文档、
  计划和日记已加入 Git 暂存区，无关个人工具目录、备份、图片和模拟脚本未加入。

## Validation

### Formula

- 单仓库时退化为该仓库实际距离。
- 多仓库不同容量的加权算例；所有容量同比例缩放后结果不变。
- 接口与核心重合、相同距离、极远坐标、零/负/非有限容量和有限性检查。
- 仓库输入顺序变化不影响结果；精确边界继续覆盖 `1/8/9 ULP`。

### Ownership and permissions

- 两种设备在任意位置、无墙面、仓库内部、远距离位置均可认领同一城镇。
- 无玩家放置保持未绑定；本队首次认领成功，其他队伍不能查看或修改配置。
- 接口速率/目标/数量/红石和发信器过滤/阈值/模式篡改均由服务端拒绝；旧菜单和远距离命令无效。

### Topology and lifecycle

- 添加近/远、大/小仓库分别按权重降低或提高等效距离。
- 仓库容量变化但总容量巧合不变时仍正确重算。
- 仓库失效、恢复、重叠、拆除和同位置替换；删除最后仓库后不可用，恢复后保留设置速率。
- 接口卸载/重载、服务器重启、拆接口、换队和无效 provider；无幽灵占用。
- 未加载接口在仓库变化后 snapshot 占用立即正确，不能等待接口区块加载。
- 发信器卸载/重载、拆除和无效 provider；没有有效仓库时红石立即关闭，恢复仓库时无需等待库存变化便正确恢复。

### Transfer and performance

- 继续覆盖低速、小数短缺比例、`1280 items/s`、共享导出/补货预算、目标满和物品守恒。
- 大量接口和仓库的拓扑变化测试允许 `O(E*W)`；随后多个空闲 tick 不再遍历仓库、不标脏、不发包、不写方块状态。
- 全量和增量 snapshot 交错后客户端总占用、距离和端点状态一致。
- 多个发信器监听相同/不同物品时只响应相关 Watcher 事件；拓扑未变化时不重复注册、标脏或更新红石邻居。

### Commands

```powershell
.\gradlew.bat test --tests "*TransportReservation*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*WarehouseInterface*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*WarehouseLevelEmitter*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*TownTransport*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain
.\gradlew.bat test compileJava --offline --no-daemon --console=plain
git diff --check
```

## Documentation Impact

实现完成后更新：

- `docs/town/town-model.md`：加权距离公式、符号、单位、默认参数、无仓库和拓扑事实变化语义。
- `docs/town/implementation-reference.md`：两种设备的城镇归属、状态所有权、Codec、刷新触发、生命周期、权限和同步。
- `docs/transport_station_design.md`：仓库接口和发信器不再属于某座仓库或墙面扫描结果。
- P2P 草案：通用预约移除 `boundWarehouseCorePos` 后的实际前置 API。
- 本计划状态/Outcome 和新的 diary。未实现前不得把本方案写入 living docs 作为当前事实。

## Risks and Controls

| 风险 | 控制 |
|---|---|
| 新增远距离大仓库令全部接口占用显著上升 | 这是加权平均的预期结果；通过镇长印章显示平均距离和距离因子 |
| 大量小型近仓库操纵平均值 | 容量加权降低小仓库影响；只统计可工作且容量为有限正数的仓库 |
| 周期仓库扫描导致 `O(E*W)` 空转 | 稳定 `(pos, capacity)` 快照净变化守卫，空闲路径不得遍历端点 |
| 拆仓库误删任意位置接口 | 删除按 `boundWarehouseCorePos` 批量注销路径；接口只由自身生命周期注销 |
| 未加载接口保留旧距离 | 城镇从 endpoint ID 统一重算，不依赖接口 BE 加载 |
| 任意位置接口扩大越权交互面 | 所有配置命令共用服务端队伍、距离、菜单和 BE 验证 |
| 仓库恢复但发信器等待库存变化才重新发信 | 拓扑净变化通知已加载发信器立即刷新；未加载设备在 `onLoad` 刷新 |
| 发信器错误占用运力或进入 transport snapshot | 发信器只订阅库存和输出红石，不创建 endpoint/reservation |
| 跨维度坐标被当作同维距离 | 明确拒绝跨维接口；跨维城镇建筑需另立计划改为 `GlobalPos` 所有权 |
| 无仓库时保留速率重新造成双速率 | 只保存一个设置速率；不可用是运行状态，不保存另一套请求/活动速率 |

## Outcome

状态为 `completed`。城镇级任意位置接口和发信器、有效仓库容量加权平均曼哈顿距离、接口单速率、发信器零运力成本
和同维度限制均已按 Mandatory Implementation Playbook 落地。W00-W08 与 H02 已在 2026-08-22 完成，源码、自动化、
living docs、P2P 前置 API 说明和 diary 已同步；自动化最终为定向 `97`、城镇包 `367`、完整 `463` tests，全部零失败/
错误。H02 由用户确认验收完成且未报告待修缺陷；可观察包类型、磁盘写入和多人游戏因环境限制继续明确记录为“未测”。
