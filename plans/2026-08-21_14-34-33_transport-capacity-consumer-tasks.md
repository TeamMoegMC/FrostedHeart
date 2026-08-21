# 仓库接口运力消费者执行任务清单

- Time: `2026-08-21 14:34:33 +08:00`
- Last verified: `2026-08-21`
- Authors: `chang; execution preference`, `Codex; OpenAI GPT-5; task decomposition`
- Status: `ready`
- Scope: `城镇运力预约基础与仓库接口首个消费者的可分配实施任务`
- Source plan: [`2026-08-20_16-53-08_transport-capacity-consumers.md`](2026-08-20_16-53-08_transport-capacity-consumers.md)
- Deferred: [`2026-08-21_01-39-26_transport-p2p-devices.md`](2026-08-21_01-39-26_transport-p2p-devices.md)

## Purpose and Authority

源计划拥有玩法规则、公式、状态语义、生命周期和验证要求；本文件只拥有执行拆分、依赖、复杂度、关键代码锚点与
任务完成门槛。若两者冲突，以源计划为准并先修正本清单，不允许在任务实现中自行改变公式或准入规则。

每次开始任务前都要重新读取源计划的 Frozen Gameplay Rules、当前任务依赖的前序产物和实际源码。一个任务完成后
立即记录验证结果及偏离，不等到 `T13` 才回忆。P2P 不在任何任务的顺手扩展范围内。

## Complexity Guide

复杂度表示状态耦合、数据安全和验证范围，不表示精确工时。

| 等级 | 判断标准 | 建议配置 |
|---|---|---|
| `S` | 单点或机械性工作，所有权明确，失败容易发现和回退 | 均衡模型，`medium` 推理 |
| `M` | 涉及少量明确模块或一个纯模型，需要定向边界测试 | 较强模型，`high` 推理 |
| `L` | 跨模型、持久化、网络、生命周期或多个界面，需要系统性回归 | 强模型，`high`；状态迁移使用 `xhigh` |
| `XL` | 同时影响权威状态与物品/存档安全，错误会复制、吞失或留下幽灵状态 | 最强可用模型，`xhigh` 推理，单独执行 |

整个功能综合复杂度为 `XL`。关键路径是
`T00 -> T01 -> T02 -> T03 -> T06 -> T08 -> T09 -> T12 -> H01 -> T13`；
`T04-T05` 必须在 `T09-T12` 前汇合，`T07` 可在 `T01` 后并行准备。

## Task Overview

| ID | 任务 | 复杂度 | 推理强度 | 依赖 | 主要产物 |
|---|---|---:|---|---|---|
| `T00` | 建立修改前基线 | `S` | `medium` | 无 | 工作区、测试、编译基线 |
| `T01` | 实现消费者参数、公式与纯模型 | `M` | `high` | `T00` | `TransportConsumerParameters`, `TransportReservationModel` |
| `T02` | 实现端点、预约记录、Codec 与汇总 | `L` | `high` | `T01` | `TransportEndpointId`, reservation Map, 向前兼容 Codec |
| `T03` | 实现 `TeamTown` 权威预约状态机 | `L` | `xhigh` | `T02` | 登记、调速、刷新、注销 API 与结构化结果 |
| `T04` | 接入晨间结算和短缺比例 | `M` | `high` | `T03` | 实时占用日报、供给变化后的统一比例 |
| `T05` | 实现 transport snapshot 与增量同步 | `L` | `xhigh` | `T03`, `T04` | 全量/增量一致的客户端快照 |
| `T06` | 接入仓库绑定与清理生命周期 | `XL` | `xhigh` | `T03` | 幂等登记、刷新、解绑和未加载端点清理 |
| `T07` | 实现小数余数预算模型 | `M` | `high` | `T01` | 无突发、无饿死的纯 tick budget 模型 |
| `T08` | 用运力预算限制仓库真实物品移动 | `XL` | `xhigh` | `T06`, `T07` | 受限 `balance`、continuation、物品安全测试 |
| `T09` | 实现设备调速命令与接口菜单 | `L` | `xhigh` | `T05`, `T06`, `T08` | 服务端校验设置、权威设备视图 |
| `T10` | 完成镇长印章与方块状态反馈 | `L` | `high` | `T05`, `T06`, `T09` | 实时预约列表、有限 BlockState 状态 |
| `T11` | 接入每日短缺 Tip | `L` | `high` | `T04`, `T05` | 安全数值通知与每镇每日去重 |
| `T12` | 自动化回归、模拟审计与性能检查 | `XL` | `xhigh` | `T08-T11` | 完整测试、编译与性能证据 |
| `H01` | 游戏内生命周期、吞吐和数值验收 | `L` | 手动 | `T12` | 真实 20 TPS/低 TPS 验收记录 |
| `T13` | 更新 living docs、Outcome 与开发日记 | `S` | `medium` | `H01` | 权威文档和完成记录 |

## Detailed Tasks

### T00: 建立修改前基线

- 状态：`pending`
- 复杂度：`S`
- 建议推理强度：`medium`
- 关键锚点：`TownTransportStateTest`, `TeamTownTransportSettlementTest`, `TownResourceUpdatePacketTest`,
  `WarehouseInterfaceBlockEntity`

关键技术点：

- 记录 `git status --short`，区分源计划的用户修改与其他既有未跟踪文件；不得清理或回退无关差异。
- 确认 Java 17，运行当前 transport、warehouse interface、town 定向测试和 `compileJava`。
- 记录 `TownTransportState` 当前 Codec 形状、`TownResourceUpdatePacket` 当前字段及仓库接口生命周期入口，作为
  向前兼容证据。
- 运行完整测试与 `git diff --check`；既有失败必须带命令和错误摘要写入任务记录。

完成门槛：工作区、定向测试、完整测试和编译状态可复核，后续任务能判断失败由谁引入。

### T01: 实现消费者参数、公式与纯模型

- 状态：`pending`
- 复杂度：`M`
- 建议推理强度：`high`
- 关键锚点：`TownModelParameters`, `TownModelParameters.Defaults`, `FHConfig.SERVER.TOWN`,
  `TownTransportCapacityModel`

关键技术点：

- 新增不可变 `TransportConsumerParameters`，集中持有默认速率 `20 items/s`、最小活动速率 `1 items/s`、默认最大
  速率 `1280 items/s` 和仓库规模系数 `0.05`；`0` 只表示禁用。
- `TownModelParameters.Defaults` 是唯一源码默认值，`FHConfig.SERVER.TOWN` 和模拟参数从同一来源构造；补齐旧构造器
  或调用点，避免参数 record 扩展破坏现有测试。
- 新增 Forge/世界无关的 `TransportReservationModel`，实现 `sqrt(volume)`、规模因子、占用、ULP 比较、短缺比例和
  准入候选计算；不得读取 `Level`、`BlockEntity` 或配置单例。
- 只有准入、是否增加占用和是否短缺使用 `8 ULP` 比较；占用值和余数不量化、不夹到容差边界。
- 先覆盖源计划全部算例，再覆盖 NaN、无穷、负值、乘法溢出、`1/8/9 ULP` 和 `R=0`。

完成门槛：纯模型测试独立通过，默认值一致性测试证明配置、模拟和源码没有第二套常量。

### T02: 实现端点、预约记录、Codec 与汇总

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`high`
- 关键锚点：`TownTransportState`, `TeamTownData.CODEC`, `GlobalPos.CODEC`, `TownTransportStateTest`

关键技术点：

- `TransportEndpointId` 包装接口方块的 `GlobalPos endpointPos`；Map key 就是实际传输设备坐标，不保存
  `BlockEntity` 引用。
- `TransportReservation` 使用 `boundWarehouseCorePos` 保存仓库核心，明确区别于 key 中的接口坐标；同时保存 kind、
  requested/active rate、scale metric、派生占用和 admission status。
- 内存使用 Map；Codec 使用按“维度 ID、坐标”稳定排序的 entry list，`reservations` 为可选字段并默认空，保证当前
  只有 `dailyReport` 的存档继续加载。
- 序列化的权威输入是 endpoint、仓库核心、kind、requested/active rate、scale metric 和 status；
  `reservedTransportCapacity` 在加载后按当前参数重算，不能信任旧配置生成的缓存值。
- 非法条目要与正常条目隔离并警告。重复 endpoint ID 的冲突组全部丢弃，不按输入顺序任选一条；未知 enum、非有限
  double、越界 rate 和非法坐标条目不得阻止其他合法预约加载。
- 汇总从 Map 稳定重算，返回不可变、稳定排序视图；不得维护可独立漂移的 writable reserved total。

完成门槛：旧存档、空/活动/禁用/拒绝记录、稳定往返、重复键、部分损坏、配置变更重算和不可变视图测试通过。

### T03: 实现 TeamTown 权威预约状态机

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 关键锚点：`TeamTown`, `TeamTownData`, `TeamTownData.DataSyncCache`, `TownTransportState`

关键技术点：

- 所有修改通过 `TeamTown` 的 register/update、metric refresh 和 unregister 门面；`TownTransportState` Map mutator
  保持私有或包内，设备和网络包不能直接写 Map。
- 请求以服务端已解析的 endpoint、kind、`boundWarehouseCorePos`、请求速率和规模指标为输入；客户端永远不能提交
  占用结果或 active rate。
- 严格实现新建、上调、下调、设零、不增占用确认、metric 事实刷新和注销的状态转换；上调失败必须保存新
  requested rate，同时保留旧 active rate 与 reserved。
- 用 `currentReserved - oldReserved + candidateReserved` 计算候选总占用。下调和事实刷新不因当前短缺被阻止；
  事实刷新允许产生新的全镇短缺。
- 幂等请求不替换等值 record、不重复占用、不重复标脏；每次净变化只调用一次
  `DataSyncCache#markTransportStateChanged`。
- `TransportReservationResult` 返回 decision、reservationAfter、townSummaryAfter 和 requiredAdditionalCapacity，
  UI 不重复推导失败原因。

完成门槛：状态转换表、多端点候选计算、短缺中的下调/确认、metric 变化、重复请求和同步净变化测试通过。

### T04: 接入晨间结算和短缺比例

- 状态：`pending`
- 复杂度：`M`
- 建议推理强度：`high`
- 关键锚点：`TeamTownData#buildingsWork`, `TeamTownData#finishDailyTransportSettlement`,
  `TeamTownTransportSettlementTest`

关键技术点：

- 将 `finishDailyTransportSettlement` 中固定的 `reservedCapacity=0` 替换为预约 Map 的实时名义占用。
- 保持“重置 service -> 货运站生产 -> 读取占用 -> 写日报”的顺序；不得在中间归零状态发包或逐端点改 active rate。
- `DailyReport` 继续是晨间历史快照；当天登记、调速和拆除只改变实时 snapshot，不回写历史日报。
- 总运力下降只改变统一 `effectiveRateScale`；预约和 active rate 保留，恢复供给后自动恢复有效速率。
- 等值报告不标脏；真正变化只排队一次 transport state 增量同步。

完成门槛：零/充足/恰好/短缺/恢复、多货运站和跨日归零测试证明日报与实时状态语义分离。

### T05: 实现 transport snapshot 与增量同步

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 关键锚点：`TownResourceUpdatePacket`, `TeamTownData#tick`, `TeamTownData#applyResourceUpdate`,
  `TeamTownData.DataSyncCache`, `TownResourceUpdatePacketTest`

关键技术点：

- 定义不可变 `TownTransportSnapshot`，至少包含 DailyReport、稳定排序预约视图、总占用、剩余/缺口和有效比例；
  汇总字段要么由一个工厂统一派生，要么在解码时验证，不能形成第二权威来源。
- 全量 `TeamTownData.CODEC` 继续同步完整 `TownTransportState`；增量 `TownResourceUpdatePacket` 携带完整的小型 transport
  snapshot，不在首版增加逐端点增量协议。
- 包解码限制预约条目数量，并拒绝非有限/负数等非法字段，避免客户端接受任意尺寸或损坏快照。
- `TeamTownData#tick` 合并同 tick 的资源变化与 transport state 变化，只发一个资源包；snapshot 等值时不得空转发包。
- 客户端 `applyResourceUpdate` 在一次主线程任务内先应用资源和 transport snapshot，再只触发一次
  `fireResourcesChanged`，界面不能观察到半更新状态。

完成门槛：Codec/网络往返、条目上限、全量后增量、资源与预约同 tick 变化、等值去重和客户端原子应用测试通过。

### T06: 接入仓库绑定与清理生命周期

- 状态：`pending`
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 关键锚点：`WarehouseBlockEntity#publishInterfaces`, `WarehouseInterfaceBlockEntity#tryBind`,
  `WarehouseInterfaceBlockEntity#resolveBinding`, `WarehouseInterfaceBlockEntity#onLoad`,
  `WarehouseInterfaceBlockEntity#onRemoved`, `WarehouseBuilding#getInterfacePositions`,
  `TeamTown#removeTownBlockInternal`

关键技术点：

- endpoint ID 使用接口 `GlobalPos`，`boundWarehouseCorePos` 使用核心 `GlobalPos`。任何注册或刷新都从服务端现有绑定
  和 `WarehouseBuilding#getVolume()` 重新生成规模指标。
- `publishInterfaces`、幂等 `tryBind` 和 `onLoad` 都要核对预约；旧存档首次加载按 `20 items/s` 请求。准入失败只让
  active rate 为零，不能反过来破坏已经成功的仓库逻辑绑定。
- `clearBinding` 当前会清空 provider 与仓库坐标；所有真实解绑路径必须在清空前用旧 provider 注销。普通区块卸载
  由 `onRemoved` 的“方块仍是接口”分支识别，只清 Watcher 和运行时余数，保留预约。
- 方块破坏、明确扫描移除、改绑和换镇要注销旧 endpoint；同位置重复加载或同一绑定扫描不得重复登记。
- `TeamTown#removeTownBlockInternal` 必须在 `WarehouseBuilding#getInterfacePositions()` 被清空前，从城镇侧注销所有
  接口预约；该路径不能依赖接口区块已加载。
- 仓库重扫体积变化时刷新所有已接受接口的 metric；增长即使造成短缺也要提交。不存在的仓库映射、核心方块替换或
  binding core 不匹配只在能够权威确认时清理，不能把未加载当作不存在。

完成门槛：加载、卸载、破坏、扫描移除、改绑、换镇、核心拆除、同位置替换、服务器重启和体积变化测试均无重复
或幽灵占用；未加载接口不会被误删。

### T07: 实现小数余数预算模型

- 状态：`pending`
- 复杂度：`M`
- 建议推理强度：`high`
- 关键锚点：源计划 `Transfer remainder`, `TransportReservationModel`

关键技术点：

- 建立可独立测试的运行时预算类，只保存 `[0, 1)` item 的 `transferRemainder`，不保存一秒额度或未使用整数预算。
- 仅在设备确有搬运需求且有效速率大于零时执行 `remainder += rate / 20`，随后直接 `floor`；余数路径不使用 ULP 容差。
- 一个 tick 只生成一个预算，由导出和补货共享；本 tick 未使用的整数预算在结束时丢弃，不能回填余数。
- 禁用、解绑、方块实体重建和普通卸载都把运行时余数归零；余数不进 NBT 或城镇 Codec。
- 长周期测试覆盖 `1/7/19/20/64/128/1280 items/s`、非整数短缺比例、长时间空闲和重载。

完成门槛：长周期总搬运量只存在不足一件的离散误差，低速不饿死，空闲和重载后不产生突发。

### T08: 用运力预算限制仓库真实物品移动

- 状态：`pending`
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 关键锚点：`WarehouseInterfaceBlockEntity#tick`, `#validateAndBalance`, `#balance`,
  `TownResourceActions.ItemStackAction`, `IActionExecutorHandler`

关键技术点：

- 每个服务端 tick 在执行库存 Action 前读取一次该 endpoint 的 reservation 和全镇 effective scale，生成一致的
  `endpointEffectiveRate`；无绑定、无活动预约、比例为零或预算为零时不提交 Action。
- 将 `balance` 改为接收 `tickBudget` 并返回实际搬运量与 `hasRemainingWork`。保持先导出错误/超量物品、后补货，
  两阶段共享剩余预算。
- 每个 `ItemStackAction` 的 stack count 不得超过剩余预算；只按 Action 实际返回的移动量扣本 tick budget，未用整数
  预算在 tick 结束时丢弃。
- `hasRemainingWork` 设置纯运行时 continuation，使下一 tick 继续；不能复用会每 tick `setChanged()` 的
  `markNeedsBalance`，否则持续差额会造成无意义存盘和同步。真实库存改变仍必须标脏。
- Watcher 回调只负责唤醒，不在回调栈中递归平衡。红石继续只禁止从仓库补货，错误/超量物品仍可按预算返回。
- 用可控 fake executor 覆盖 ADD/COST 部分成功、返回零、目标已满、导出与补货竞争，以及绑定在 tick 间失效；证明
  本地 inventory 与虚拟仓库不会重复扣除、重复加入或吞失。

完成门槛：所有 Action 都受预算限制，continuation 不忙等，20 TPS/低 TPS 的 server tick 语义一致，自动化物品守恒
测试通过。

### T09: 实现设备调速命令与接口菜单

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 关键锚点：`WarehouseInterfaceMenu`, `WarehouseInterfaceScreen`, `FHMenuSlots`,
  `CCustomMenuSlot.CDataSlot`, `WarehouseInterfaceBlockEntity`

关键技术点：

- 增加设置速率命令和 `0 / 20 / 64 / 320 / 1280` 操作；客户端只发送整数 requested rate，不发送 active rate、
  metric、占用或城镇汇总。
- `receiveMessage` 在服务端重新检查菜单仍有效、BE 坐标、逻辑绑定、当前玩家队伍和速率范围，再通过 `TeamTown` API
  提交；失效或篡改请求返回明确失败状态。
- 为菜单定义一个不可变、Codec 支持的设备 transport view，并通过自定义 `CDataSlot` 同步 requested/active/effective
  rate、metric、reserved、town summary 和 decision；不要把 double 截断进单个 int slot。
- 上调失败后输入框保留 requested rate，但运行状态显示旧 active rate；`0` 显示禁用并释放占用。客户端不得先乐观
  修改 active 状态。
- 扩展现有 CUI 时保证九槽目标、红石按钮和玩家库存仍可用；新增文本在中英文、默认 GUI scale 下不溢出。

完成门槛：合法设置、越界、非本队、菜单失效、绑定竞态和失败保留旧速率测试通过；客户端所见完全来自服务端 view。

### T10: 完成镇长印章与方块状态反馈

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`high`
- 关键锚点：`TownVirtualResourcesPanel#transportRows`, `WarehouseInterfaceBlock`,
  `WarehouseInterfaceBlockEntity`, blockstate/model data, `en_us.json`, `zh_cn.json`

关键技术点：

- `TownVirtualResourcesPanel` 改读实时 snapshot，晨间 DailyReport 单独标注；显示总量、占用、剩余/缺口、有效比例及
  按 endpoint 稳定排序的预约行。
- 每行明确接口坐标与绑定仓库核心坐标，避免把 `boundWarehouseCorePos` 误当作设备位置；同时显示 requested、active、
  effective rate、scale metric、reserved 和 admission status。
- 方块状态只表达有限的正常、禁用、短缺和绑定不可用类别。选定 enum 或两个布尔属性后固定映射，生成全部 FACING
  组合模型；详细失败原因留在菜单。
- 服务端只在派生视觉状态净变化时写 BlockState；不得每 tick 重写相同状态或触发邻居更新风暴。
- 补齐本地化、模型和必要数据生成；检查窄窗口、GUI scale、列表滚动和 endpoint 数量增加时的稳定布局。

完成门槛：菜单、镇长印章和方块状态对同一 snapshot 的描述一致；模型、翻译、排序和净变化测试通过。

### T11: 接入每日短缺 Tip

- 状态：`pending`
- 复杂度：`L`
- 建议推理强度：`high`
- 关键锚点：`TeamTownData#finishDailyTransportSettlement`, `TeamTownData#flushTownTipNotifications`,
  `TownSignalNotificationPacket`, `TownSignalTipPresentation`

关键技术点：

- 仅在晨间 `meaningfullyGreater(reserved, total)` 时为该城镇排队一次短缺提示；当天接口调速拒绝只反馈操作者，
  不广播全镇。
- 当前 `TownSignalNotificationPacket` 明确排除 detail，不能塞入任意字符串。新增受限的 transport shortage notice/packet，
  只编码有限非负的 total、reserved、shortfall 和 effective scale，并设置条目/方向校验。
- 复用现有 `sendToOnline` 与客户端 Tip 展示入口；文本使用本地化参数，点击动作只导航到镇长印章运力详情，不执行
  服务端修改。
- 去重状态以城镇日为边界，不写成客户端静态状态；重登、多人在线和同 tick 其他 Tip 不得重复或覆盖错误。

完成门槛：充足/短缺边界、每日去重、多人、包篡改、客户端方向和本地化展示测试通过；首版不发送恢复 Tip。

### T12: 自动化回归、模拟审计与性能检查

- 状态：`pending`
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 关键锚点：全部新增 transport/warehouse tests、`TownStageFourSimulator`, Gradle 验证命令

关键技术点：

- 按源计划 Validation Matrix 建立公式、准入、Codec、同步、生命周期、吞吐、权限和反馈测试，不只依赖最终完整套件。
- 增加多接口、多仓库、短缺恢复、旧存档升级、同位置替换、未加载拆除和 Action 部分成功的跨模块回归。
- 将消费者参数纳入模拟/参数审计输入，确认默认值可搜索且输出能追踪；不把 BE、网络或 Forge 依赖带进纯模拟层。
- 用大量已加载但空闲接口检查：没有库存 Action、持续 `setChanged`、重复 BlockState 写或每 tick transport packet。
- 依次运行 transport 定向、warehouse interface 定向、town 包、完整测试、`compileJava`、必要的 `runData` 和
  `git diff --check`；审查生成范围和无关差异。

完成门槛：定向与完整自动化、编译和差异检查通过；性能检查有可复核证据，任何既有失败已与本功能失败分离。

### H01: 游戏内生命周期、吞吐和数值验收

- 状态：`pending`
- 复杂度：`L`
- 执行方式：`手动`

验收重点：

- 在实际 20 TPS 下验证 `20 items/s = 1 item/tick`、`1280 items/s = 64 items/tick`，并验证低速、小数短缺比例和
  人工低 TPS 场景按 server tick 工作。
- 覆盖绑定、重扫、扩建、缩建、卸载/重载、重登、重启、拆接口、拆核心、改绑和换队。
- 同时制造导出与补货需求，检查共享预算、红石语义、目标满和仓库不足；逐项核对物品守恒。
- 检查菜单、镇长印章、方块状态和 Tip 的数值、坐标、失败原因、本地化、滚动及多人同步。
- 对目标规模的空闲接口观察服务器日志、磁盘写入和网络行为，确认没有明显周期性风暴。

完成门槛：所有场景记录预期与实际结果；任何复制、吞物、幽灵占用或权限绕过都必须回到对应代码任务修复并重跑
`T12`，不能以已知问题关闭。

### T13: 更新 living docs、Outcome 与开发日记

- 状态：`pending`
- 复杂度：`S`
- 建议推理强度：`medium`
- 关键锚点：`docs/town/implementation-reference.md`, `docs/town/town-model.md`,
  `docs/transport_station_design.md`, `docs/README.md`, `diary/README.md`

关键技术点：

- 只记录最终实现行为：状态所有权、单位、参数、公式、Codec、同步、生命周期、限速、权限、反馈和精确代码锚点。
- 更新源计划状态与 Outcome，逐项记录任务完成/偏离、自动化命令、H01 结果和 P2P 前置 API 的实际形状。
- 新增时间戳 diary，记录决策、验证、文档影响和剩余工作；不改写既有日记历史。
- 若最终只形成一个仓库消费者，不提前创建独立物流系统入口；需要新增系统 README 时遵守项目 README Contract。

完成门槛：源码、living docs、源计划 Outcome、任务状态和 diary 对实现结果一致；P2P 计划可以从 `P00` 重新调查。

## Shared Validation Commands

```powershell
.\gradlew.bat test --tests "*TownTransport*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "*WarehouseInterface*" --offline --no-daemon --console=plain
.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain
.\gradlew.bat test --offline --no-daemon --console=plain
.\gradlew.bat compileJava --offline --no-daemon --console=plain
git diff --check
```

`runData` 只在 `T10` 实际新增 BlockState/model 或其他生成资源时运行；执行前后记录工作区状态，只保留本功能差异。

## Outcome

状态为 `ready`。任务已按模型/状态、同步、生命周期、物品安全、玩家反馈和交付拆分，并标注复杂度与推理强度；
尚未开始 `T00`，没有实现任何消费者代码。执行时逐项更新状态和验证结果，不在任务之间隐式携带未记录假设。
