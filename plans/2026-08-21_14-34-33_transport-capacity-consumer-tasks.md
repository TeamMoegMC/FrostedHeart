# 仓库接口运力消费者执行任务清单

- Time: `2026-08-21 14:34:33 +08:00`
- Last verified: `2026-08-22`
- Authors: `chang; execution preference`, `Codex; OpenAI GPT-5; task decomposition`
- Status: `completed`
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

整个功能综合复杂度为 `XL`。首版关键路径
`T00 -> T01 -> T02 -> T03 -> T06 -> T08 -> T09 -> T12` 已完成；H01 首轮反馈后的当前关键路径是
`R01 -> (R02 + R03) -> R04 -> H01复验 -> R05 -> H01继续复验 -> T13`。`R02` 与 `R03` 可以在 `R01` 的单速率
数据合同冻结后并行，但 `R04` 必须汇合两者；`R05` 来自第二轮界面反馈。

## Task Overview

| ID | 任务 | 复杂度 | 推理强度 | 经济委派 | 依赖 | 主要产物 |
|---|---|---:|---|---|---|---|
| `T00` | 建立修改前基线 | `S` | `medium` | `M` | 无 | 工作区、测试、编译基线 |
| `T01` | 实现消费者参数、公式与纯模型 | `M` | `high` | `M` | `T00` | `TransportConsumerParameters`, `TransportReservationModel` |
| `T02` | 实现端点、预约记录、Codec 与汇总 | `L` | `high` | `M` | `T01` | `TransportEndpointId`, reservation Map, 向前兼容 Codec |
| `T03` | 实现 `TeamTown` 权威预约状态机 | `L` | `xhigh` | `M` | `T02` | 登记、调速、刷新、注销 API 与结构化结果 |
| `T04` | 接入晨间结算和短缺比例 | `M` | `high` | `M` | `T03` | 实时占用日报、供给变化后的统一比例 |
| `T05` | 实现 transport snapshot 与增量同步 | `L` | `xhigh` | `M` | `T03`, `T04` | 全量/增量一致的客户端快照 |
| `T06` | 接入仓库绑定与清理生命周期 | `XL` | `xhigh` | `M` | `T03` | 幂等登记、刷新、解绑和未加载端点清理 |
| `T07` | 实现小数余数预算模型 | `M` | `high` | `D?` | `T01` | 无突发、无饿死的纯 tick budget 模型 |
| `T08` | 用运力预算限制仓库真实物品移动 | `XL` | `xhigh` | `M+R` | `T06`, `T07` | 受限 `balance`、continuation、物品安全测试 |
| `T09` | 实现设备调速命令与接口菜单 | `L` | `xhigh` | `M` | `T05`, `T06`, `T08` | 服务端校验设置、权威设备视图 |
| `T10` | 完成镇长印章与方块状态反馈 | `L` | `high` | `M` | `T05`, `T06`, `T09` | 实时预约列表、有限 BlockState 状态 |
| `T11` | 接入每日短缺 Tip | `L` | `high` | `D` | `T04`, `T05` | 安全数值通知与每镇每日去重 |
| `T12` | 自动化回归、模拟审计与性能检查 | `XL` | `xhigh` | `M+R?` | `T08-T11` | 完整测试、编译与性能证据 |
| `R01` | 收敛为单一已接受速率 | `L` | `high` | `M` | `T12`, H01 首轮反馈 | 单速率 reservation/Codec/状态机 |
| `R02` | 修正接口调速反馈和紧凑信息区 | `L` | `high` | `M` | `R01` | 输入回退、零速率新增、明确标签与红色限速态 |
| `R03` | 修正镇长印章实时快照并折叠设备明细 | `XL` | `xhigh` | `M` | `R01` | 全量/增量一致的实时汇总、默认收起明细 |
| `R04` | 更新纠偏后的自动化回归 | `L` | `xhigh` | `M+R?` | `R02`, `R03` | 拒绝、全量同步与 UI 回归证据 |
| `R05` | 压缩接口调速区并修正输入边界 | `M` | `high` | `M` | `R04`, H01 第二轮反馈 | 滚轮调速、服务端上限、紧凑布局 |
| `H01` | 游戏内生命周期、吞吐、数值与 UI 验收 | `L` | 手动 | `H` | `T12`, `R05` | 多轮反馈记录及纠偏后复验 |
| `T13` | 更新 living docs、Outcome 与开发日记 | `S` | `medium` | `M` | `R05`, `H01` | 权威文档和完成记录 |

## Economical Sub-agent Guide

| 标记 | 含义 |
|---|---|
| `D` | 经济上推荐完整委派；独立性和并行收益足以抵消重新读取上下文与主 Agent 复核成本 |
| `D?` | 仅在缩短墙钟时间优先时委派；通常不会降低 token 成本 |
| `M+R` | 主 Agent 实现，经济上值得增加一次范围严格受限的只读审查 |
| `M+R?` | 主 Agent 实现；只在质量优先且预算允许时增加独立审查 |
| `M` | 主 Agent 连续执行，重新委派的上下文和协调成本高于收益 |
| `H` | 需要人工游戏内操作，不交给代码子 Agent |

默认推荐只有一项完整实现委派：

- `T11` 在 `T05` 后接口稳定，主要读取通知与 Tip 子系统，可在主 Agent 执行 `T06-T10` 时独立推进；它的 `L`
  工作量足以摊薄上下文加载成本，而且通常不与仓库搬运文件冲突。

以下只在特定目标下使用子 Agent：

- `T07` 是纯预算模型，适合追求速度时与 `T02-T06` 并行；但任务较小，若只考虑 token 成本，主 Agent 顺手完成更省。
- `T08` 由主 Agent 实现后，委派一个只读审查，输入只给源计划相关规则、`T08` diff、搬运类和定向测试；审查目标
  只限预算绕过、复制、吞物、continuation 忙等和红石方向。这类缺陷代价高，审查收益通常高于上下文成本。
- `T12` 的额外审查不是默认项。只有质量优先时，才让一个子 Agent 检查最终 diff 与验证矩阵；不要再拆成多个领域
  审计 Agent，否则重复读取仓库和计划的成本会快速增长。

不推荐委派 `T01-T06`、`T09-T10`、`R01-R03` 或 `T13`。这些任务要么构成连续的权威状态上下文，要么与相邻任务
共享核心文件，要么工作量太小；子 Agent 即使技术上能完成，也需要主 Agent 重新核对同样的源码和接口。

推荐执行档位：

| 目标 | 子 Agent 使用 |
|---|---|
| 成本优先 | `0` 个；全部由主 Agent 连续执行 |
| 平衡推荐 | `T11` 实现子 Agent + `T08` 完成后一个只读审查 Agent |
| 速度优先 | 在平衡方案上增加 `T07` 实现子 Agent |
| 质量优先 | 在平衡方案上增加一次 `T12` 最终只读审查；不增加更多实现 Agent |

共享文件写入约束：

- `T11` 子 Agent 不得修改 `TeamTownData` 中超出通知排队/发送边界的 transport 状态、Codec 或同步逻辑。
- `T07` 子 Agent 只拥有纯预算模型与其测试，不修改 `WarehouseInterfaceBlockEntity`；由主 Agent 在 `T08` 接线。
- 审查 Agent 只报告问题，不直接写文件。修复与最终测试始终由主 Agent 完成。
- 主 Agent 在接受任何子 Agent 结果前检查 `git diff`、任务完成门槛和实际测试输出。

## Main-agent Model Strategy

根据 [OpenAI model guidance](https://developers.openai.com/api/docs/guides/latest-model)，模型应按工作负载选择，
`medium` 是平衡起点，`high/xhigh` 应用于确实能获得质量收益的复杂任务；多 Agent 主要适用于能干净拆分的并行工作流。

本计划推荐“按阶段切模型、按任务调推理强度”，而不是全程固定同一强度，也不是每个任务都切模型：

| 阶段 | 主 Agent 模型 | 推理强度 | 原因 |
|---|---|---|---|
| `T00-T02` | `gpt-5.6-terra` | `T00 medium`; `T01-T02 high` | 建立基线和纯数据合同，边界明确，平衡模型足够且成本较低 |
| `T03-T12` | `gpt-5.6-sol` | 按总览中的 `high/xhigh` | 权威状态、同步、生命周期和物品安全形成连续高风险上下文，保持同一强模型比逐任务切换稳定 |
| `R01-R05` | `gpt-5.6-sol` | `R01/R02/R05 high`; `R03/R04 xhigh` | 单速率状态收敛后仍需修复同步与界面合同，保持同一强模型可减少跨任务漂移 |
| `T13` | `gpt-5.6-terra` | `medium` | 机械性收尾，但仍需理解实际实现，不建议降到完全脱离上下文的独立 Agent |

- 同一模型、同一推理强度全程运行最简单，但会让 `T00/T13` 浪费推理预算，或让 `T06/T08/T12` 强度不足。
- 同一模型、不同推理强度比全程固定更合理；若不想切模型，使用 `gpt-5.6-sol` 并按任务在
  `medium/high/xhigh` 间调整，是质量优先的稳妥方案。
- 每个任务都切模型不推荐。模型切换只放在 `T03` 和 `T13` 这种清晰阶段边界，且切换前要求前一任务的代码、测试和
  状态记录完整，避免依赖未写下来的推理。
- 成本优先时可让 `gpt-5.6-terra` 执行全部任务，只在 `T06/T08/T12` 的实际结果证明需要更高能力时切到
  `gpt-5.6-sol`；质量优先时则让 `gpt-5.6-sol` 从 `T00` 持续到 `T12`，只调整推理强度。

## Detailed Tasks

### T00: 建立修改前基线

- 状态：`completed (2026-08-21)`
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

基线结果：

- 开始时 `git status --short` 没有 tracked file 差异。既有未跟踪内容包括 `.agents/`、`.comate/`、`.workbuddy/`、
  `docs/backup/`、玩家营养系统对话记录、P2P 后续计划、5 篇 diary、`sim_guard.py`、`sim_test.py` 和
  `warehouse.png`；全部保持不动。执行期间本任务文件出现了用户的子 Agent 分工指南修改，本记录保留并叠加其上。
- `java -version` 为 `17.0.2`，但本机原始 `JAVA_HOME` 指向 32 位
  `C:\Program Files\Java\java-se-8u41-ri`。首次 Gradle 调用因此无法应用 `-Xmx6G`；所有有效基线命令均在进程内将
  `JAVA_HOME` 和 `Path` 指向 `C:\Program Files\Java\jdk-17.0.2`，没有修改系统环境。
- `test --tests "*TownTransport*"`：通过，约 `49s`。首次 `processResources` 报告 6 个既有 scaffolding item model
  duplicate-path 警告，没有测试或编译失败。
- `test --tests "*WarehouseInterface*"`：Gradle 返回 `No tests found for given includes`。当前测试源码确实没有匹配
  `*WarehouseInterface*` 的测试类，这是 `T06/T08/T12` 必须补齐的基线覆盖缺口，不是断言回归。
- `test --tests "com.teammoeg.frostedheart.content.town.*"`：通过，约 `28s`。
- 完整 `test`：通过，约 `29s`；XML 汇总为 `88` 个 suite、`332` 个测试、`0` failure、`0` error、`0` skipped。
- 独立 `compileJava`：通过，约 `15s`；任务为 up-to-date。记录计划结果前的 `git diff --check` 通过。
- 当前兼容基线：`TownTransportState.CODEC` 只有可选 `dailyReport`；`TownResourceUpdatePacket` 携带资源 changes、仓库
  `occupiedCapacity` 和 transport `DailyReport`；仓库接口生命周期入口是 `tryBind`、`clearBinding`、
  `validateAndBalance`、`balance`、`tick`、`onLoad` 与 `onRemoved`。

完成门槛：已满足。后续任务统一显式使用 Java 17；`T01` 可开始。

### T01: 实现消费者参数、公式与纯模型

- 状态：`completed (2026-08-21)`
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

执行结果：

- 新增 `TransportConsumerParameters` 与无 Forge 依赖的 `TransportReservationModel`。源码默认值集中在
  `TownModelParameters.Defaults`，分别为 `20 items/s`、`1 items/s`、`1280 items/s` 和 `0.05`；
  `TownModelParameters.currentDefaults()`、`FHConfig.SERVER.TOWN.TRANSPORT_CONSUMERS` 与阶段零参数审计均从该表接线。
- 实现 `sqrt(volume)`、`1 + 0.05 * metric`、无量化的占用计算、`8 ULP` 比较、短缺比例与候选准入计算。
  无效数值、无穷、负值和溢出均拒绝形成容量结果。
- `TransportReservationModelTest` 覆盖默认算例、`R=0`、无效输入、溢出与 `1/8/9 ULP` 边界；参数默认值与审计测试
  同时覆盖新配置来源。验证命令及全量结果记录在本文件末尾。

### T02: 实现端点、预约记录、Codec 与汇总

- 状态：`completed (2026-08-21)`
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

执行结果：

- 新增 `TransportEndpointId`（接口方块 `GlobalPos endpointPos`）、`TransportReservation`
  （含 `boundWarehouseCorePos`）以及端点类型和准入状态枚举。接口坐标是 Map key，仓库核心坐标只作为绑定目标保存。
- `TownTransportState` 现在以内存 Map 保存预约，并以按维度 ID、`x/y/z` 稳定排序的 entry list 编码；`reservations`
  为可选字段，因此旧 `dailyReport` 存档继续加载。派生 `reservedTransportCapacity` 不写入 Codec，加载后由
  `recalculateReservedCapacities` 按当前参数重新生成。
- Codec 对单项损坏采用部分恢复并告警；重复 endpoint 的完整冲突组会丢弃。读视图不可变且稳定排序，聚合占用、剩余、
  短缺和有效速率比例均由 Map 派生。测试覆盖旧存档、活动/禁用/拒绝记录、缓存排除与重算、重复键、损坏条目、越界速率
  隔离以及不可变稳定视图。

### T03: 实现 TeamTown 权威预约状态机

- 状态：`completed (2026-08-21)`
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

执行结果：`TeamTown` 已成为预约修改门面，返回 `TransportReservationResult`；新建/上调失败、下调、禁用、事实
metric 刷新、幂等与注销语义由 `TeamTownTransportReservationTest` 覆盖。净变化统一标记 transport state dirty。

### T04: 接入晨间结算和短缺比例

- 状态：`completed (2026-08-21)`
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

执行结果：晨间日报读取预约 Map 的实时名义占用；供给短缺只改变统一比例，不修改活动预约。当日预约变化不回写
已生成的 `DailyReport`，结算顺序和多站行为由 `TeamTownTransportSettlementTest` 覆盖。

### T05: 实现 transport snapshot 与增量同步

- 状态：`completed (2026-08-21)`
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

执行结果：新增 `TownTransportSnapshot`，增量资源包携带完整稳定预约列表和当前总量；客户端先应用资源与 snapshot，
再触发一次资源回调。Codec 限制 `4096` 条并校验重复 key、数量和有限非负汇总输入。

### T06: 接入仓库绑定与清理生命周期

- 状态：`completed (2026-08-21)`
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

执行结果：`publishInterfaces` 在发布逻辑接口集合后核对预约；接口加载、平衡前和幂等绑定会再次核对。真实解绑在
清 provider 前注销，普通卸载只清 Watcher 与余数。扫描移除和 `TeamTown#removeTownBlockInternal` 可按逻辑坐标注销
未加载接口；核心拆除的无 level 修复路径按核心 `BlockPos` 清理所有匹配维度记录。

### T07: 实现小数余数预算模型

- 状态：`completed (2026-08-21)`
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

执行结果：`TransportTransferBudget` 仅保存运行时 `[0,1)` 十进制余数。初版 double 在 `7 items/s` 的 2000 tick
边界得到 `699.999999...`，定向测试据此改为 `BigDecimal` 余数；仍直接 floor，不引入令牌、量化步长或 ULP 容差。

### T08: 用运力预算限制仓库真实物品移动

- 状态：`completed (2026-08-21)`
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

执行结果：`WarehouseInterfaceBlockEntity#validateAndBalance` 每 tick 读取活动预约与全镇比例；无需求不累计额度，零
有效速率不发 Action。`WarehouseInterfaceTransfer` 保持先导出后补货，两阶段共享预算并只扣实际移动数；纯运行时
continuation 不调用 `setChanged()`，真实库存修改仍标脏。fake executor 测试覆盖跨槽共享、部分成功和导出/补货竞争。

### T09: 实现设备调速命令与接口菜单

- 状态：`completed (2026-08-21)`
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

执行结果：接口菜单支持直接整数输入、回车/设置按钮和 `0/20/64/320/1280` 快捷值。客户端命令只有 rate；菜单与
`WarehouseInterfaceBlockEntity#setRequestedTransportRate` 分层复核实际 BE、维度、八格距离、provider 队伍和逻辑
绑定，速率范围最终由 `TeamTown` 当前配置校验。`WarehouseInterfaceTransportView` 通过自定义 Codec data slot 同步
请求/活动/有效速率、规模、占用、全镇汇总、准入决策和所需新增运力；中英文界面保留九槽、目标和红石控制。

### T10: 完成镇长印章与方块状态反馈

- 状态：`completed`
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

执行结果：镇长印章运力页改为实时汇总加稳定 endpoint 明细，接口位置与绑定仓库核心分别标注，晨间日报独立放在
末尾。仓库接口新增 `active/disabled/shortage/unavailable` 四态属性与全部朝向模型；全镇比例限速使用明确的
`THROTTLED` 菜单态并映射到短缺外观。服务端每 tick 只比较派生状态，只有净变化才用 `Block.UPDATE_CLIENTS` 写回。
通知点击可经 `TownManagerClientHelper#openTransportCapacity` 直达该页。

### T11: 接入每日短缺 Tip

- 状态：`completed`
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

执行结果：晨间按 `meaningfullyGreater` 和城镇日排队一次短缺 notice，经 `sendToOnline` 发送受限数量的专用 S2C
数值包。包校验有限非负字段、派生缺口/比例、条目数和方向；客户端只用本地化参数生成 Tip，点击直达运力详情。
恢复与当日调速拒绝均不广播。T11 由独立子 Agent 实现并由主 Agent复核。

### T12: 自动化回归、模拟审计与性能检查

- 状态：`completed (2026-08-21)`
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

执行结果：补齐多仓库共享占用、供给短缺恢复、缩仓、同位置仓库替换、客户端资源与 snapshot 原子应用以及
Stage Four `summary.json` 消费者参数追踪。既有旧存档、未加载拆除、权限和 Action 部分成功用例纳入分层矩阵。
性能审计发现空闲接口查询会反复全表重算；`TownTransportState` 因此改为参数快照命中的一次性重算与 O(1) 总占用
缓存。4096 条预约连续 20 tick 的接口状态查询用例耗时约 `0.243 s`，期间同步缓存保持干净；4096 个空库存接口
审计耗时约 `0.025 s`，库存 Action、inventory changed/`setChanged` 门、continuation 和 BlockState 净变化均为零。

## H01 Feedback Corrections

`T02/T03/T09/T10/T12` 的完成状态只记录首版实施历史。其中 requested/active 双速率、拒绝后保留请求值、接口菜单
旧信息布局和镇长印章首版明细布局已被 `2026-08-21` 的 H01 首轮反馈取代；固定速率快捷按钮和客户端百万上限又被
`2026-08-22` 的第二轮反馈取代，后续实现以 `R01-R05` 为准。旧自动化通过不等于新语义已经验证。

### R01: 收敛为单一已接受速率

- 状态：`completed (2026-08-21)`
- 复杂度：`L`
- 建议推理强度：`high`
- 关键锚点：`TransportReservation`, `TransportEndpointRequest`, `TransportReservationResult`,
  `TransportAdmissionStatus`, `TransportReservationDecision`, `TownTransportState`, `TownTransportSnapshot`, `TeamTown`

关键技术点：

- 将 reservation 的 `requestedRateItemsPerSecond` 与 `activeRateItemsPerSecond` 收敛为唯一
  `rateItemsPerSecond`。它只表示已经通过服务端准入的设置；`effectiveRate = rate * townScale` 仍是运行时派生值，
  但不得作为第二套设置或持久化字段。
- 已有端点上调失败时返回 `INSUFFICIENT_CAPACITY` 命令结果，但 reservation 必须逐字段保持不变；不得写入失败值、
  不得标脏、不得发送内容相同的 snapshot。下调、设零和同值确认仍按不增加占用接受。
- 新端点按默认 `20 items/s` 准入失败时，创建 `rate=0`、`reserved=0`、`DISABLED` 的记录并保留仓库逻辑绑定；
  被拒绝的默认值不写入城镇状态。仓库体积变化仍按当前非零 rate 重算，允许由事实变化造成全镇比例限速。
- `TransportAdmissionStatus` 只描述稳定状态；`INSUFFICIENT_CAPACITY` 保留在本次 `TransportReservationDecision` 中作为
  瞬时失败原因，不再依赖“拒绝预约”保存第二个速率。全镇 `THROTTLED` 继续由汇总比例派生。
- 新持久化 Codec 直接写单速率字段，不兼容未发布的 requested/active 字段。旧开发存档不在支持范围，实施和验收可
  直接使用新世界或清除旧 transport 数据；不增加兼容 Codec、DataFix、容错承诺或一次性迁移代码。
  `SNAPSHOT_CODEC` 继续额外携带服务端派生占用。
- 同步更新 `TransportReservationModel`、`TeamTown` 门面、snapshot/view、稳定排序及所有调用点；客户端仍只能提交一个
  整数设置值，不能提交占用、距离因子或有效速率。

完成门槛：新单速率 Codec 往返、新增失败零速率、上调失败零状态变化、下调/禁用、metric 重算与同步净变化测试
全部通过；不包含双速率旧存档兼容或迁移测试。

执行结果：预约、请求、配置参数和 snapshot/view 已统一为 `rateItemsPerSecond`；持久化 Codec 直接使用新字段，未加入
旧开发存档兼容。已有端点拒绝返回旧记录且不标记 transport dirty，新端点拒绝生成零速率 `DISABLED` 记录。定向
Codec、预约、结算和同步测试于 `2026-08-21` 通过。

### R02: 修正接口调速反馈和紧凑信息区

- 状态：`completed (2026-08-21；2026-08-22 游戏内反馈闭环)`
- 复杂度：`L`
- 建议推理强度：`high`
- 关键锚点：`WarehouseInterfaceBlockEntity`, `WarehouseInterfaceMenu`, `WarehouseInterfaceScreen`,
  `WarehouseInterfaceTransportView`, `WarehouseBlockEntity#publishInterfaces`, `zh_cn.json`, `en_us.json`

关键技术点：

- 速率输入框的稳定值始终来自服务端已接受 rate。玩家尝试上调但准入失败时，不乐观显示更高值；收到结果后输入框
  回退到旧 rate，界面内显示本地化的“运力不足，无法上调”，旧占用和当前运行保持不变。
- 新接口默认准入失败后在设备 view 中只出现 rate `0`。玩家触发的仓库扫描/绑定沿调用链传递可选操作者或返回批量
  失败结果，由该调用者提示“运力不足，无法新增，接口速率已设为 0”；`onLoad` 等无玩家上下文自检不得随机广播。
- “运力已启用”下方只保留三类紧凑数据：`当前物品传输速率`、`占用运力`、
  `剩余可用运力 A / 总运力 T`。移除请求/活动双值、原始规模和含义不明的“占用”“城镇”短标签。
- 当前物品传输速率显示实际有效值；当 `effectiveRate < rate` 时该行变红，满速时使用正常颜色，rate 为 `0` 时显示
  禁用状态。接口菜单不显示原始 `scaleMetric`；若未来需要高级信息，只显示名为“距离因子”的直接乘数。
- 失败反馈是短暂 UI 状态：成功设置、菜单重开或后续合法命令后清理；它不能改变方块持久状态或产生全镇广播。
- 保持九槽、快捷值、目标设置、红石按钮和玩家库存布局可用；在默认与较大 GUI scale 下检查最长中英文文本不溢出。

完成门槛：上调失败输入回退、新增失败零速率与操作者提示、红色限速态、三条明确数据、本地化和服务端权限回归通过；
界面不再出现“请求/活动/规模/占用/城镇”的旧歧义组合。

执行结果：接口菜单现在以服务端单一设置速率回填输入；上调拒绝只显示瞬时提示，重开菜单清理。信息区显示实际有效
速率、占用运力、剩余可用运力和总运力，限速时速率行变红，不再显示原始规模。新接口准入失败保留零速率禁用记录，
只向放置该接口的玩家发送提示；加载与后台扫描没有玩家上下文时不广播。中英文运行时资源测试通过；默认/较大 GUI
scale 的最终画面仍归入 H01 复验，不伪记为已测。

### R03: 修正镇长印章实时快照并折叠设备明细

- 状态：`completed (2026-08-21；2026-08-22 无新增游戏内问题)`
- 复杂度：`XL`
- 建议推理强度：`xhigh`
- 关键锚点：`TeamTownDataS2CPacket`, `TownResourceUpdatePacket`, `TeamTownData#applyResourceUpdate`,
  `TownTransportSnapshot`, `TownTransportState`, `TownVirtualResourcesPanel#transportRows`, `TownInfoPanel`

关键技术点：

- 先建立截图所示回归：服务端存在非零预约，晨间日报占用正确；经过 `TeamTownDataS2CPacket` 全量替换后，客户端实时
  汇总不得变为 `0`。重点核查持久化 `TransportReservation.CODEC` 排除派生占用，而全量包复用该 Codec 的链路。
- 推荐让 `TeamTownDataS2CPacket` 的网络载荷在 town data 之外显式携带同一时刻的服务端
  `TownTransportSnapshot`。客户端先加载 town data，再原子应用 snapshot，最后替换 client data 并只触发一次刷新；
  不允许客户端按本地配置重算占用来掩盖缺失快照。
- `TownResourceUpdatePacket` 与全量包必须使用相同 snapshot 语义。登录、重登、切维度、打开镇长印章和实时调速后，
  总运力、已占用、剩余、有效比例、日报和端点列表来自同一权威时刻。
- 镇长印章顺序改为：实时状态与汇总 -> 最近晨间结算 -> 设备详情控制。设备详情放在最底部并默认收起；控制文本为
  “展开设备详情（N）/收起设备详情”，点击只改变客户端展示状态。
- `TownVirtualResourcesPanel` 持有展开状态；资源回调或 snapshot 更新不能自动收起，也不能无故把滚动位置跳回顶部。
  设备行保持 endpoint 稳定排序，展开后分别标注接口坐标与绑定仓库核心坐标。
- 展开行只显示单一设置速率、实际有效速率、`warehouseScaleFactor`（玩家文案“距离因子”）、占用运力和状态；不显示
  requested/active 双值或原始 scale metric。窄窗口和长维度 ID 继续使用现有省略/换行策略。

完成门槛：全量和增量同步回归均显示正确非零实时占用；首屏不再被端点坐标挤占；折叠默认值、点击、刷新保态、
滚动、排序和本地化测试通过。

执行结果：`TeamTownDataS2CPacket` 现在在持久化 town data 之外携带服务端 `TownTransportSnapshot`，客户端解码后先
应用 snapshot 再替换实例并触发现有的一次全量刷新；持久化 Codec 省略派生占用不再使实时汇总归零。镇长印章顺序
已改为实时汇总、晨间结算、底部设备控制；详情默认收起，点击和 snapshot 刷新保留本地展开与滚动状态。展开行按
endpoint 稳定排序，显示接口/核心坐标、设置/有效速率、由服务端占用反推的距离因子、占用运力和派生状态，不显示
原始规模。自动化与全量回归通过；实际画面仍归入 H01，不伪记为已测。

### R04: 更新纠偏后的自动化回归

- 状态：`completed (2026-08-21)`
- 复杂度：`L`
- 建议推理强度：`xhigh`
- 关键锚点：全部 `TownTransport*Test`, `WarehouseInterface*Test`, `TeamTownDataS2CPacket` 测试，
  `TownVirtualResourcesPanel`/展示模型测试，Gradle 验证命令

关键技术点：

- 删除或改写把 `requested > active`、拒绝预约持久化、失败输入保留视为正确行为的旧断言；新断言必须验证上调失败
  后 reservation、占用、同步脏状态和输入稳定值都不变化。
- 覆盖新端点失败生成 `rate=0`/`DISABLED`、操作者有无上下文的提示策略、单速率 snapshot/view Codec、短缺有效
  速率和供给恢复；明确不保留双速率旧存档迁移用例。
- 增加真实全量包 round-trip：非零派生占用经过 `TeamTownDataS2CPacket` 后仍在客户端实时汇总中；再与增量包交错，
  证明最终状态一致且不会被旧全量包覆盖成零。
- 将镇长印章行构造尽量提取为可测展示模型，断言汇总、晨间、折叠控制和端点详情顺序；接口 view 断言精确标签所需
  字段及限速红色条件，不用像素截图代替状态测试。
- 重跑 transport 定向、warehouse 定向、town 包、完整测试和 `compileJava`；保留 T12 的 4096 端点缓存/空闲性能回归，
  确认状态收敛与同步修复没有恢复每 tick 全表重算或无变化发包。

完成门槛：所有新语义和截图缺陷都有先失败后通过的自动化证据；完整测试、编译和 `git diff --check` 通过后才能进行
H01 复验。

完成记录（`2026-08-21`）：保留并扩充单速率拒绝、新端点零速率、snapshot/view Codec、短缺与恢复、4096 端点空闲
读取、镇长印章折叠与稳定排序回归。新增真实 `TeamTownDataS2CPacket` 全量 round-trip 后接
`TownResourceUpdatePacket` 增量 round-trip 的交错测试，分别确认非零占用 `28` 与最终占用 `56`；增量包生产处理器和测试
共用 `TownResourceUpdatePacket#applyTo`。接口测试同时锁定失败后输入框仍显示已接受速率、短缺文字条件、供给恢复，以及
“准入失败且存在操作者 UUID 时才产生定向接收者”的提示策略。运力定向 `33` 项、仓库接口 `16` 项、town 包 `331`
项、完整 `383` 项全部零失败/错误，`compileJava` 与 `git diff --check` 通过。R04 只增加可测入口和回归证据，没有改变
已由 R01-R03 记录的玩家行为，living docs 无需修改。

### R05: 压缩接口调速区并修正输入边界

- 状态：`completed (2026-08-22；游戏内复验接受)`
- 复杂度：`M`
- 建议推理强度：`high`
- 关键锚点：`WarehouseInterfaceScreen`, `WarehouseInterfaceMenu`, `WarehouseInterfaceTargetElement`,
  `WarehouseInterfaceTransportView`, `WarehouseInterfaceBlockEntity#getTransportView`

关键技术点：

- 删除 `0/20/64/320/1280` 固定速率快捷按钮及其整行高度；目标过滤槽、接口库存槽、状态区和玩家背包同步上移
  `16 px`，菜单总高度从 `234` 收敛为 `218`。共享坐标由公共 `WarehouseInterfaceMenu` 持有，服务端菜单不得引用客户端
  `WarehouseInterfaceScreen`。
- 在速率输入框悬停时用滚轮直接调整并提交；步长与当前接口目标数量一致：无修饰键 `1`、Shift `8`、Ctrl `16`、
  Shift+Ctrl `64 items/s`。高精度滚轮通过 `ScrollTracker` 累积，结果限制在 `0..maximumRateItemsPerSecond`。
- `WarehouseInterfaceTransportView.CODEC` 增加服务端权威 `maximumRateItemsPerSecond`。输入验证器和最大字符数随 view
  更新；默认错误提示范围改为 `0..1280`，数值型输入超过同步上限时直接拒绝。服务端 `TeamTown` 的配置范围、队伍、
  距离和绑定校验继续作为最终防线。
- 自动化不做像素截图：锁定非默认最大值 Codec 往返、四种滚轮步长、上下限、非法文本回退来源，以及公共布局坐标
  关系；实际缩放和视觉间距仍由 H01 复验。

执行结果：上述代码和自动化已完成。仓库与运力合并矩阵 `55` 项、town 包 `334` 项、完整 `386` 项全部通过，
`compileJava` 与 `git diff --check` 通过；随后游戏内复验未再报告布局、滚轮或输入边界问题，用户确认可以进入 T13。

### H01: 游戏内生命周期、吞吐、数值与 UI 验收

- 状态：`completed-with-residual-risk (2026-08-22)`
- 复杂度：`L`
- 执行方式：`手动`

首轮已确认问题：

- 接口 UI 暴露请求/活动/有效三套速率以及原始规模，“占用”“城镇”标签不明确。
- 镇长印章实时已占用可显示为 `0`，与同页非零晨间结算矛盾；端点明细过长并占据首屏。

第二轮已确认问题：

- 接口调速区的五个固定快捷按钮继续挤占垂直空间；改为只保留输入和设置按钮，并支持与目标数量一致的滚轮调节。
- 客户端速率验证器错误使用 `0..1000000`，导致错误提示上限不符且超过默认最大值 `1280` 仍可作为有效输入。

纠偏后验收重点：

- 验证已有接口上调失败时输入框无法停留在更高值，旧速率继续运行且 UI 显示失败；验证新增接口准入失败时 rate 为
  `0`，触发操作的玩家收到无法新增提示。
- 验证接口只显示当前物品传输速率、占用运力、剩余可用运力/总运力；短缺导致实际速率不满时文字变红。
- 验证固定速率快捷按钮已经移除，菜单压缩后各槽位仍对齐；在速率框上测试普通/Shift/Ctrl/Shift+Ctrl 滚轮步长
  `1/8/16/64`，并确认默认最大值提示为 `1280`、`1281` 无法进入输入框或提交。
- 验证镇长印章实时已占用与接口合计一致，登录/重登/调速后立即更新；设备详情位于最下方、默认收起且展开后坐标
  和距离因子含义正确。
- 在实际 20 TPS 下验证 `20 items/s = 1 item/tick`、`1280 items/s = 64 items/tick`，并验证低速、小数短缺比例和
  人工低 TPS 场景按 server tick 工作。
- 覆盖绑定、重扫、扩建、缩建、卸载/重载、重登、重启、拆接口、拆核心、改绑和换队；同时制造导出与补货需求，
  检查共享预算、红石语义、目标满、仓库不足和物品守恒。

当前环境限制项：

| 验收项 | 状态 | 原因 |
|---|---|---|
| 可观察网络包类型、包数量或包频率 | `未测` | 没有可观察包类型的工具 |
| 空闲接口的实际磁盘写入频率 | `未测` | 没有磁盘写入观察工具 |
| 多人同步、多人 Tip 与权限交互 | `未测` | 暂时无法进行多人游戏 |

完成门槛：所有当前可执行的单人场景记录预期与实际结果；上述环境限制项保持“未测”并作为残余风险写入 `T13`，
不得改写为通过。任何复制、吞物、幽灵占用或权限绕过都必须回到对应代码任务修复并重跑 `R04`。

验收结果：两轮实际单人画面分别发现并促成 R01-R04 与 R05，最终 R05 结果由用户确认无进一步问题并授权进入 T13。
这证明最终接口布局、单速率呈现、输入上限和主要镇长印章信息流已完成反馈闭环；没有独立留存上述全部生命周期、
低 TPS、极限吞吐和物品守恒手动场景的逐项结果，因此这些项目只具有自动化证据，作为残余人工验收风险记录，不能
写成完整手动通过。三项环境限制继续保持“未测”。

### T13: 更新 living docs、Outcome 与开发日记

- 状态：`completed (2026-08-22)`
- 复杂度：`S`
- 建议推理强度：`medium`
- 关键锚点：`docs/town/implementation-reference.md`, `docs/town/town-model.md`,
  `docs/transport_station_design.md`, `docs/README.md`, `diary/README.md`

关键技术点：

- 只记录最终实现行为：状态所有权、单位、参数、公式、Codec、同步、生命周期、限速、权限、反馈和精确代码锚点。
- 更新源计划状态与 Outcome，逐项记录任务完成/偏离、自动化命令、H01 可测结果、明确标为“未测”的网络包观察/
  磁盘写入/多人项，以及 P2P 前置 API 的实际形状。
- 新增时间戳 diary，记录决策、验证、文档影响和剩余工作；不改写既有日记历史。
- 若最终只形成一个仓库消费者，不提前创建独立物流系统入口；需要新增系统 README 时遵守项目 README Contract。

完成门槛：源码、living docs、源计划 Outcome、任务状态和 diary 对实现结果一致；P2P 计划可以从 `P00` 重新调查。

完成记录：已校正货运站与城镇运力 living docs，关闭源计划和本清单，记录最终自动化、H01 证据边界、三项“未测”
及 P2P 前置 API 实际形状；没有因单一仓库消费者新增独立物流 README。P2P 草案已移除过时双速率前提，可从 P00 开始。

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

状态为 `completed (2026-08-22)`。`T00-T12` 已完成首版实现和自动化；H01 首轮试玩随后否决了 requested/active 双速率交互，
并确认镇长印章实时占用可能错误显示为 `0`、端点明细过长；第二轮又确认快捷按钮拥挤和客户端最大值错误。
`R01-R05` 已于 `2026-08-21` 至 `2026-08-22` 完成；最终界面复验由用户接受，T13 已完成知识收尾。包类型/数量、
磁盘写入和多人同步受环境限制，状态为“未测”；未逐项留存的单人生命周期、低 TPS、极限吞吐和物品守恒场景保留为
残余人工验收风险，不以自动化结果冒充手动通过。以下既有记录证明首版和纠偏自动化矩阵。P2P 仍未实现，但其独立
草案的前置条件已经满足，可以开始 P00。

本轮验证（所有 Gradle 命令均在进程内使用 `C:\Program Files\Java\jdk-17.0.2`）：

- `test --tests "*TransportReservation*" --offline --no-daemon --console=plain`：通过，`4` 个纯模型测试。
- `test --tests "*TownTransportStateTest" --offline --no-daemon --console=plain`：通过，`7` 个状态/Codec 测试。
- `test --tests "*TownModelParameterDefaultsTest" --tests "*TownStageZeroAuditTest" --offline --no-daemon --console=plain`：通过。
- `test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain`：通过。
- 完整 `test --offline --no-daemon --console=plain` 与 `compileJava --offline --no-daemon --console=plain`：通过。
- `test --tests "*TransportTransferBudgetTest" --tests "*WarehouseInterfaceTransportViewTest" --tests
  "*TeamTownTransportReservationTest" --tests "*TeamTownTransportSettlementTest" --tests
  "*TownResourceUpdatePacketTest" --tests "*TownBuildingRemovalTest"`：通过，`24` 个测试。
- `test --tests "*WarehouseInterfaceBalanceTest"`：通过，覆盖共享预算、部分成功和导出/补货竞争。
- T10 状态/资源定向测试通过：覆盖 `THROTTLED`、有限映射、净变化、`16` 个朝向组合、模型引用和双语键。
- T11 子 Agent 定向测试 `11` 个通过；扩展 `TownTransport`、`TownSignal` 和 Tip 运行时选择测试 `44` 个通过。
- `runData --offline --no-daemon --console=plain`：通过，`src/generated/resources` 前后无差异。
- T10-T11 合并后的完整 `test compileJava --offline --no-daemon --console=plain`：通过，`371` 个测试，零失败、错误或跳过。
- T12 transport 定向矩阵：`45` 个测试通过，零失败/错误；仓库接口定向：`13` 个测试通过。
- T12 town 包：`326` 个测试通过；完整 `test compileJava --offline --no-daemon --console=plain`：`377` 个测试通过，
  零失败、错误或跳过。
- T12 未新增或修改生成资源；沿用 T10 已通过且生成目录无差异的 `runData` 结果，未重复运行数据生成。
- R01-R02 定向矩阵：预约、结算、Codec、资源包、接口视图和模型参数共 `47` 个测试通过。
- R01-R02 完整 `test compileJava --offline --no-daemon --console=plain`：`378` 个测试通过，零失败、错误或跳过；编译通过。
- R03 全量/增量同步、折叠详情和双语资源定向测试：`7` 个通过，零失败/错误。
- R03 完整 `test compileJava --offline --no-daemon --console=plain`：`380` 个测试通过，零失败、错误或跳过；编译通过。
- R04 transport 定向矩阵：`33` 个测试通过；仓库接口定向：`16` 个测试通过；town 包：`331` 个测试通过。
- R04 完整 `test compileJava`：`383` 个测试通过，零失败或错误；编译通过。
- R05 仓库与运力定向矩阵：`55` 个测试通过；town 包：`334` 个测试通过。
- R05 完整 `test compileJava`：`386` 个测试通过，零失败或错误；编译通过。
- `git diff --check`：通过。
