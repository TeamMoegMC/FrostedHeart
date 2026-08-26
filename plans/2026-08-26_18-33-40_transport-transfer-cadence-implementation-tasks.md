# 城镇物流传输降频实施计划与任务清单

- 时间：`2026-08-26 18:33:40 +08:00`
- 作者：`Codex；OpenAI GPT-5；实施规划与验证角色`
- 状态：`completed`（已完成）
- 范围：`TransportTransferBudget`、仓库接口平衡循环、P2P 发送端传输循环及相关测试和文档
- 相关：
  - 已确认方案：`plans/2026-08-25_17-46-00_transport-transfer-backoff-and-cadence.md`
  - P2P 现行文档：`docs/town/p2p-logistics.md`
  - 城镇实现参考：`docs/town/implementation-reference.md`
  - 既有测试：`TransportTransferBudgetTest`、`WarehouseInterfaceBalanceTest`、
    `P2PItemTransferTest`

## 目标

按照已确认方案实现统一的物流传输调度机制：

- 设置速率决定最短运行间隔；
- 真实传输尝试搬运零件物品后，运行间隔倍增，最高为 80 tick；
- 传输成功后，运行间隔向上取整减半，直到恢复设置速率对应的最短间隔；
- 传输额度按照实际可用速率积累，最多保存一秒运力，且任何一次运行最多
  搬运 64 件；
- 降频期间溢出的运力和首次恢复后剩余的额度直接丢弃，不进行追赶；
- 仓库接口按整个方块统一调度；
- P2P 只调度发送端，所有到期发送端都可以在同一个 tick 依次运行。

## 范围边界

本轮不包含以下修改：

- 不新增服务器配置项，也不改变现有设置速率上下限；
- 不修改 GUI、菜单协议、语言文件、方块模型或材质；
- 不增加或修改 NBT、Codec、网络同步字段；全部调度状态仅存在于运行时；
- 不给 P2P 接收端增加传输冷却、失败退避或每 tick 接收数量限制；
- 不恢复接收端的单发送方公平轮询，也不增加新的来源权重或优先级；
- 不在本轮补全 `TeamTownResourceHolder` 的 watch-all 监听实现；
- 不改变 P2P 的模拟、提交、回退和 `recoveryStack` 物品守恒约定；
- 不修改伴生整合包仓库。

## 已核对的修改基线

- `TransportTransferBudget` 当前每 tick 增加 `effectiveRate / 20`，只保留
  小于 1 件的小数余量，没有运行间隔和整件额度桶。
- `WarehouseInterfaceBlockEntity#tick` 在 `needsBalance` 为真时调用
  `validateAndBalance`；失败且仍有需求时会把 `needsBalance` 再次设为真，
  因而可能每 tick 重试。
- `WarehouseInterfaceTransfer#balance` 已经能够让九个槽位共享同一个预算，
  并返回 `movedItems`、`hasRemainingWork` 和 `inventoryChanged`。
- `P2PTerminalBlockEntity#performOutgoingTransfer` 当前使用固定 5 tick
  `failureCooldown`，并在预算检查前解析源端和目标端能力。
- P2P 当前先调用 `hasTransferDemand` 模拟扫描，再调用
  `P2PItemTransfer#move` 扫描和提交。
- `P2PFairTransferScheduler` 当前阻止同一个接收端的多个发送端在同一 tick
  运行；`incoming.size()` 速率放大用于补偿该限制。
- Java 17 下的 `TransportTransferBudgetTest`、`P2PItemTransferTest` 和
  `WarehouseInterfaceBalanceTest` 已在方案评估阶段通过。

## 实施约定

### 调度状态归属

继续使用 `TransportTransferBudget` 作为纯运行时辅助类，但把它扩展为同时
管理额度和运行间隔的状态机。每个仓库接口和每个具有发送方向的 P2P 终端各自
持有一个实例。不要按仓库槽位或 P2P 接收来源创建额外状态。

建议由该类统一持有：

- `baseIntervalTicks`
- `currentIntervalTicks`
- `nextAttemptTick`
- `lastAllowanceTick`
- `tokens`
- 当前是否处于降频状态

调用方负责判断领域状态和执行物品传输；公共状态机不得解析 Forge 能力、
访问城镇仓库或操作物品槽。

### 一次运行的状态流程

1. 调用方先检查绑定、红石、预约和设置速率等廉价条件。
2. 状态机按服务器游戏时间和实际可用速率补充额度并限制容量。
3. 尚未到期或完整额度不足 1 件时，直接结束，不访问物品处理能力。
4. 状态机返回 `1..64` 的本次预算后，调用方执行一次真实传输路径。
5. 搬运量大于零时记录成功；真实尝试搬运量为零时记录失败。
6. 成功只扣除实际搬运量；降频后的首次成功还要清空剩余整数和小数额度。
7. 失败不扣除物品额度，但额度仍受一秒容量和 64 件上限限制。

额度容量使用以下公式：

```text
tokenCapacity = min(64, max(1, ceil(effectiveRate)))  // effectiveRate > 0 时
```

这里的向上取整只允许保存不足 1 件的跨秒小数余量，不会额外生成物品额度。
正常状态下小数持续顺延，例如 `17.25 件/秒` 连续四秒应得到 `17`、`17`、
`17`、`18` 件完整额度。降频后的首次成功仍按已确认方案清空剩余小数。

### 结果分类

| 场景 | 状态机结果 |
|---|---|
| 设置速率为零、红石暂停、未绑定、预约无效、对端区块未加载 | 暂停或重置，不记失败 |
| 尚未到期或完整额度不足 1 件 | 跳过，不记成功或失败 |
| 仓库接口本地库存已经满足目标且没有待存物品 | 无需求，不记失败 |
| 仓库接口有平衡需求，但一次运行总搬运量为零 | 失败 |
| P2P 已进入正预算发送尝试，但源为空、过滤无匹配、目标已满或能力不可用 | 发送端失败 |
| 任一运行实际搬运至少 1 件 | 成功，包括部分成功 |

P2P 接收端现有的 `receiverContainerProbeTicks` 仅用于结构能力存在性复核，
保持 20 tick，不参与上述发送端状态机。

## 预计修改文件

| 文件 | 修改内容 |
|---|---|
| `src/main/java/com/teammoeg/frostedheart/content/town/transport/TransportTransferBudget.java` | 改为有界额度和自适应间隔状态机 |
| `src/test/java/com/teammoeg/frostedheart/content/town/transport/TransportTransferBudgetTest.java` | 重写额度与间隔合同测试 |
| `src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceBlockEntity.java` | 接入方块级到期检查、唤醒和结果反馈 |
| `src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceTransfer.java` | 仅在结果分类确有需要时补充窄接口 |
| `src/test/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceBalanceTest.java` | 补充 64 件共享预算和结果分类测试 |
| `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PTerminalBlockEntity.java` | 接入发送端调度并删除固定冷却、重复扫描和来源数补偿 |
| `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PItemTransfer.java` | 删除只服务于旧冷却的结果接口，保留物品守恒逻辑 |
| `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PFairTransferScheduler.java` | 删除单接收端单发送方调度器 |
| `src/test/java/com/teammoeg/frostedheart/content/town/transport/device/P2PItemTransferTest.java` | 删除旧轮询测试并补充同 tick 顺序传输测试 |
| `docs/town/p2p-logistics.md` | 实现后更新 P2P 现行行为 |
| `docs/town/implementation-reference.md` | 实现后更新仓库接口预算和生命周期说明 |

## 任务总表

| 编号 | 任务 | 依赖 | 状态 |
|---|---|---|---|
| `T00` | 核对方案、源码、现行文档和基线测试 | 无 | 已完成 |
| `T01` | 实现并测试通用额度与间隔状态机 | `T00` | 已完成 |
| `T02` | 仓库接口接入方块级调度 | `T01` | 已完成 |
| `T03` | P2P 发送端接入调度并移除旧轮询 | `T01` | 已完成 |
| `T04` | 完成跨模块自动化和性能回归 | `T02`、`T03` | 已完成 |
| `T05` | 更新现行文档、计划结果和开发日记 | `T04` | 已完成 |
| `T06` | 执行完整验证并关闭计划 | `T05` | 已完成 |

## 详细任务清单

### T00 核对基线

- [x] 阅读项目结构、架构约定、计划目录规范和城镇现行文档。
- [x] 核对已确认方案与当前 `TransportTransferBudget`、仓库接口和 P2P
  发送路径。
- [x] 确认仓库接口已有整次运行汇总结果，不需要按槽位创建退避状态。
- [x] 确认 P2P 的固定冷却检查发生在能力解析之后，无法充分减少容器访问。
- [x] 确认旧公平轮询只有 `P2PFairTransferScheduler` 及对应单元测试直接持有。
- [x] 记录 Java 17 聚焦测试基线。

完成条件：实施假设与源码一致，没有待用户选择的行为问题。

### T01 通用额度与间隔状态机

- [x] 开始实现前，将本任务文档状态改为 `in-progress`（实施中）。
- [x] 先在 `TransportTransferBudgetTest` 中写入设置速率档位边界测试：
  `1`、`32`、`33`、`64`、`65`、`128`、`129`、`640`、
  `641`、`1280`。
- [x] 为失败倍增、80 tick 上限、成功向上取整减半和基础间隔下限编写测试。
- [x] 为实际速率 `0`、低于 `1`、带小数和非有限值编写额度测试。
- [x] 验证 `tokenCapacity` 对正数实际速率向上取整后再限制到 64，向上取整
  不会改变额度补充速率。
- [x] 验证 `17.25 件/秒` 在正常状态下跨秒保留小数，连续四秒得到
  `17`、`17`、`17`、`18` 件完整额度。
- [x] 为一秒容量、单次 64 件上限、正常状态余量保留和长时间阻塞溢出丢弃
  编写测试。
- [x] 为降频后首次成功清空剩余额度、失败不扣除额度和重置不积累离线运力
  编写测试。
- [x] 验证两个独立状态机实例在同一个游戏 tick 到期时，都能获得各自的预算，
  不存在接收端级别的互斥状态。
- [x] 继续使用 `BigDecimal` 确定性十进制计算保留现有长期速率精度，
  不退回会在长期取整边界产生误差的裸 `double` 累加。
- [x] 实现设置速率到 `baseIntervalTicks` 的集中映射，避免仓库和 P2P
  各自复制档位判断。
- [x] 实现基于绝对游戏时间的到期判断、额度补充、`1..64` 预算生成、
  成功反馈、失败反馈、唤醒和重置。
- [x] 设置速率改变时重建基础间隔并清除不兼容的旧调度状态；实际速率缩放
  改变时只限制额度容量，避免频繁缩放造成低速饥饿。
- [x] 保持状态机不持久化，不向 NBT、网络或城镇数据添加运行时字段。

完成条件：

- 所有状态转换可以只使用游戏时间和数值输入进行确定性单元测试；
- 任意调用都不会返回大于 64 的预算；
- 正常状态长期平均额度不超过实际可用速率；
- 降频和暂停期间不存在可供后续追赶的无界运力欠账。

### T02 仓库接口接入

- [x] 在 `WarehouseInterfaceBlockEntity#validateAndBalance` 中，把红石、拓扑、
  预约、设置速率和额度检查放在城镇资源 Action 执行之前。
- [x] 让 `needsBalance` 表示“存在待处理或需要探测的工作”，让状态机的
  `nextAttemptTick` 决定该 tick 是否真正运行平衡。
- [x] `markNeedsBalance` 只唤醒一次检查，不直接清除已经产生的降频间隔。
- [x] 没有本地平衡需求时关闭 `needsBalance`，等待库存、目标、红石、拓扑
  或仓库 Watcher 事件重新唤醒。
- [x] 有需求且获得正预算时，只调用一次
  `WarehouseInterfaceTransfer#balance`，所有九个槽位共享同一预算。
- [x] 使用整次运行的 `movedItems` 和 `hasRemainingWork` 反馈成功、失败
  和是否继续调度，不按槽位分别调整频率。
- [x] 保证 `ADD` 和 `COST` 合计不会超过状态机返回的预算，且单次总量
  永远不超过 64。
- [x] 在速率归零、绑定清除、拓扑不可用、方块卸载和移除时重置运行时状态。
- [x] 保持精确物品 Watcher 的现有唤醒能力；对于无法收到容量释放通知的路径，
  依靠最高 80 tick 的有界重试，不在本任务扩展 watch-all。
- [x] 扩展 `WarehouseInterfaceBalanceTest`，覆盖多槽共享 64 件上限、
  零搬运失败输入、部分成功和完全满足时不提交 Action。

完成条件：

- 低速接口不会每 tick 执行城镇资源 Action；
- 阻塞接口最多每 80 tick 进行一次真实平衡尝试；
- 一次运行跨所有槽位最多搬运 64 件；
- Watcher 或本地库存变化可以立即触发探测，但只有成功才会缩短失败间隔。

### T03 P2P 发送端接入

- [x] 从 `P2PTerminalBlockEntity` 删除
  `FAILURE_COOLDOWN_TICKS`、`failureCooldown` 及所有重置和递减逻辑。
- [x] 在解析源端和目标端 `IItemHandler` 之前完成绑定、红石、预约、设置
  速率、实际速率、到期时间和完整额度检查。
- [x] 对端未加载、绑定失效或预约未激活时暂停或重置状态，不记录传输失败，
  也不强制加载区块。
- [x] 获得正预算后，源能力缺失、接收容器不可用、目标能力缺失或
  `recoveryStack` 无法归还时，只在发送端记录零搬运失败。
- [x] 删除独立的 `hasTransferDemand` 扫描；一次到期运行只通过
  `P2PItemTransfer#move` 完成模拟、提交和结果统计。
- [x] 搬运量大于零时记录成功并保留现有目标端 `setChanged` 和恢复堆栈逻辑；
  搬运量为零时记录发送端失败。
- [x] 删除 `P2PItemTransfer.Result#shouldCooldown` 以及仅服务于旧冷却、
  且不再被其他行为使用的结果字段；不得改变部分提交和物品回退逻辑。
- [x] 删除 `P2PFairTransferScheduler` 及其调用，让每个到期发送端在同一个
  服务器 tick 都能各自运行。
- [x] 删除 `incoming.size()` 和 `sourceCount` 速率补偿，直接使用
  `binding.rateItemsPerSecond() * effectiveRateScale`。
- [x] 保持接收端 `receiverContainerProbeTicks == 20` 的结构检查和现有
  可视状态语义，不给接收端添加调度状态。
- [x] 将过滤器变化、双向缓存变化和明确的绑定或速率变化改为唤醒发送端；
  除设置速率重建状态外，这些事件不直接清除失败间隔。
- [x] 删除 `P2PItemTransferTest` 中的稳定轮询测试，增加两次独立
  `P2PItemTransfer#move` 在同一目标上依次提交的测试。

完成条件：

- 未到期和额度不足时不解析相邻容器能力；
- 一次真实发送不再先执行一遍独立需求扫描；
- 多个到期发送端可在同一个 tick 依次传输；
- 每个发送端只使用自己的实际速率，接收端数量和来源数量都不会放大速率；
- 发送端失败不会写入或改变接收端的频率状态。

### T04 跨模块自动化与性能回归

- [x] 增加“长时间源为空后恢复”的测试，确认首次恢复最多搬运 64 件且没有追赶。
- [x] 增加“目标已满后释放空间”的测试，确认失败间隔降低频率，成功后逐步恢复。
- [x] 增加“部分接受”的测试，确认只要搬运量大于零就提高频率。
- [x] 增加“实际速率低于 1 件/秒”的测试，确认额度不足时不调用传输处理器，
  最终仍能搬运 1 件。
- [x] 增加“高设置速率但城镇有效速率较低”的测试，确认检查上限由设置速率
  决定，真实访问仍受完整额度门控。
- [x] 使用 `TransportTransferBudgetTest` 验证未到期、额度不足和退避等待 tick
  返回零预算，并核对生产调用路径在零预算分支先于 P2P 能力解析和仓库 Action。
- [x] 验证两个发送端同 tick 竞争有限目标容量时按服务器顺序提交，较晚发送端
  可以正常得到零搬运失败而不会阻止较早发送端。
- [x] 验证红石暂停、解绑、重新绑定、区块卸载和加载不会积累离线追赶额度。
- [x] 检查 `sourceScanCount`、`transferVisualTicks` 和
  `blockStateWriteCount`，避免调度重构造成额外扫描或视觉状态写入。

完成条件：关键行为均有自动化覆盖，并且等待 tick 的外部库存访问次数为零。

### T05 文档和开发记录

- [x] 实施过程中同步更新本任务文档的复选框和任务总表状态。
- [x] 更新 `docs/town/p2p-logistics.md` 的传输与故障边界，写入发送端档位、
  自适应间隔、一秒额度、单次 64 件、多发送端同 tick 和无来源数补偿。
- [x] 更新 `docs/town/implementation-reference.md` 中仓库接口的
  `TransportTransferBudget`、事件唤醒、方块级结果判断和运行时生命周期。
- [x] 不改写已经完成的旧 P2P 计划和日记；它们保留为当时实现的历史记录。
- [x] 实现完成后，把已确认方案和本任务文档状态改为 `completed`（已完成），
  在“结果”章节记录最终实现差异和验证结果。
- [x] 按 `diary/README.md` 新增时间戳开发日记，记录修改、决策、测试、
  性能结果、文档影响和剩余工作。

完成条件：现行文档只描述已实现行为，计划和日记能够追踪本次变更结果。

### T06 最终验证与关闭

- [x] 使用 Java 17 运行聚焦测试：

  ```powershell
  ./gradlew test --no-daemon --tests "com.teammoeg.frostedheart.content.town.transport.TransportTransferBudgetTest" --tests "com.teammoeg.frostedheart.content.town.transport.device.P2PItemTransferTest" --tests "com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseInterfaceBalanceTest"
  ```

- [x] 运行城镇系统测试：

  ```powershell
  ./gradlew test --no-daemon --tests "com.teammoeg.frostedheart.content.town.*"
  ```

- [x] 运行完整测试：

  ```powershell
  ./gradlew test --no-daemon
  ```

- [x] 运行 `git diff --check`。
- [x] 检查生产代码中不再存在 `failureCooldown`、
  `P2PFairTransferScheduler`、`hasTransferDemand` 和
  `incoming.size()` 速率补偿引用。
- [x] 核对未修改 GUI、配置、NBT、网络协议、资源文件和伴生仓库。
- [x] 记录测试数量、失败数量和结构级性能验证结果，然后关闭计划。

完成条件：聚焦、城镇和完整测试全部通过，文档已更新，任务清单无未解释的
未完成项。

## 风险与处理

| 风险 | 处理方式 |
|---|---|
| 额度状态机同时被两个系统使用，错误会影响所有物流吞吐 | 先完成纯数值测试，再分别接入仓库和 P2P |
| 设置速率与实际速率混用，导致检查频率或吞吐错误 | 基础间隔只使用设置速率；额度补充只使用实际速率 |
| 降频等待时间被算成追赶额度 | 一秒容量硬截断；首次恢复清空剩余额度 |
| P2P 删除轮询后仍保留来源数补偿 | 将删除调度器和删除 `incoming.size()` 作为同一任务验收 |
| 事件唤醒意外清除失败退避 | 唤醒只提前下一次探测；成功才缩短间隔 |
| 删除需求预扫描破坏物品守恒 | 不改 `P2PItemTransfer#move` 的模拟、提交和恢复流程 |
| 仓库 Watcher 无法覆盖容量释放 | 保留最高 80 tick 的有界重试，本轮不扩展 watch-all |

## 开放问题

无。已确认方案足以开始实现。

## 结果

已完成 T01-T06 实施。实际修改：

- `TransportTransferBudget` 扩展为统一额度和自适应间隔状态机。
- `WarehouseInterfaceBlockEntity` 接入方块级调度，共享预算和整次运行结果。
- `P2PTerminalBlockEntity` 接入发送端调度，删除固定冷却、需求预扫描、旧公平轮询和来源数补偿。
- `P2PItemTransfer` 移除仅服务于旧冷却的结果字段。
- 删除 `P2PFairTransferScheduler`。
- 更新 `docs/town/p2p-logistics.md`、`docs/town/implementation-reference.md`。
- 新增开发日记：`diary/2026-08-26_19-45-34_transport-transfer-cadence-implementation.md`。

验证：

- 聚焦测试（`TransportTransferBudgetTest`、`WarehouseInterfaceBalanceTest`、`P2PItemTransferTest`）通过。
- 城镇测试：430 个测试，1 个失败；完整测试：806 个测试，1 个失败。唯一失败为
  `TeamTownActualSaveCodecProbeTest` 读取硬编码 macOS 路径，属于本环境无关的既有问题。
- `git diff --check` 通过；生产代码已无 `failureCooldown`、`P2PFairTransferScheduler`、
  `hasTransferDemand` 和 `incoming.size()` 速率补偿引用。
- 性能验证为结构级：`TransportTransferBudgetTest` 覆盖未到期和额度不足的零预算结果；生产调用顺序保证仓库 Action 与
  P2P 物品能力解析位于正预算检查之后。

复核修正（2026-08-26）：

- 状态机初始化或重置后的首次运行改为先等待设置速率对应的基础间隔，避免低速设备在相邻两个 tick 运行。
- 新增 `TransportTransferBudget#defer`；P2P 对端未加载时清空额度并按当前间隔安排下一次探测，不增加失败退避。
- P2P 发送端在未到期或额度不足的等待 tick 保留最近一次确认的对端可用性，避免状态闪烁和无意义方块状态写入。
- 删除由测试自身决定何时调用 `P2PItemTransfer#move` 的无效门控测试，改为直接覆盖首次间隔和延后探测时序。
- 复核后的完整测试共 807 个，除同一个既有硬编码存档路径测试外，其余 806 个通过。
- 复核日记：`diary/2026-08-26_20-26-31_transport-transfer-cadence-review-fixes.md`。
