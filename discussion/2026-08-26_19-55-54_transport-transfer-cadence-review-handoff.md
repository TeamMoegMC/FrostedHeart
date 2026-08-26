# Transport Transfer Cadence Implementation - Review Handoff

- Time: `2026-08-26 19:55:54 +08:00`
- Author: `DeepSeek Harness coding agent; work-like-codex; implementation and handoff role`
- Related:
  - `plans/2026-08-26_18-33-40_transport-transfer-cadence-implementation-tasks.md`
  - `plans/2026-08-25_17-46-00_transport-transfer-backoff-and-cadence.md`
  - `docs/town/p2p-logistics.md`
  - `docs/town/implementation-reference.md`
  - `diary/2026-08-26_19-45-34_transport-transfer-cadence-implementation.md`

## 背景与目标

按 `plans/2026-08-26_18-33-40_transport-transfer-cadence-implementation-tasks.md` 完成 T01-T06，实现城镇物流传输降频与失败退避。行为约定来自 `plans/2026-08-25_17-46-00_transport-transfer-backoff-and-cadence.md`，未自行修改传输规则。

核心行为：

- 设置速率决定最短运行间隔：`1..32→20t`、`33..64→10t`、`65..128→5t`、`129..640→2t`、`641..1280→1t`
- 实际速率只生成额度，额度桶最多一秒运力，单次最多 64 件
- 零搬运失败 → 间隔倍增，上限 80t；成功 → 向上取整减半，最低回到基础间隔
- 降频后首次成功清空剩余额度；不追赶降频/阻塞损失
- 仓库接口按整个方块统一调度；P2P 只限制发送方，多个发送方可同 tick 依次运行
- 未到期或额度不足时不得访问外部物品能力

## 修改文件

**生产代码**

- `src/main/java/com/teammoeg/frostedheart/content/town/transport/TransportTransferBudget.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceBlockEntity.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PTerminalBlockEntity.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PItemTransfer.java`
- `src/main/java/com/teammoeg/frostedheart/content/town/transport/device/P2PFairTransferScheduler.java`（已删除）

**测试**

- `src/test/java/com/teammoeg/frostedheart/content/town/transport/TransportTransferBudgetTest.java`
- `src/test/java/com/teammoeg/frostedheart/content/town/buildings/warehouse/WarehouseInterfaceBalanceTest.java`
- `src/test/java/com/teammoeg/frostedheart/content/town/transport/device/P2PItemTransferTest.java`

**文档/计划/日记**

- `docs/town/p2p-logistics.md`
- `docs/town/implementation-reference.md`
- `plans/2026-08-25_17-46-00_transport-transfer-backoff-and-cadence.md`
- `plans/2026-08-26_18-33-40_transport-transfer-cadence-implementation-tasks.md`
- `diary/2026-08-26_19-45-34_transport-transfer-cadence-implementation.md`

## 关键实现决策

- `TransportTransferBudget` 是纯运行时状态机，不持久化、不解析物品能力。
- 红石暂停使用 `pause()`（清额度、保留当前退避间隔），不使用 `reset()`；只有真实成功才会缩短退避。
- 解绑、零速率、预约无效、拓扑不可用、卸载/移除使用 `reset()`。
- P2P 接收端 `receiverContainerProbeTicks == 20` 保留，不参与发送端调度。
- 删除 `P2PItemTransfer.Result.shouldCooldown` 及 `foundEligibleSource`/`targetAcceptedSimulation` 字段；保留模拟优先提交和 recovery stack 守恒逻辑。
- 事件（过滤器/缓存/速率/红石变化）只 `wake()`，不直接清除退避。

## 验证结果

- 聚焦测试：`TransportTransferBudgetTest`、`WarehouseInterfaceBalanceTest`、`P2PItemTransferTest` **通过**。
- 城镇测试：`com.teammoeg.frostedheart.content.town.*` → **430 tests, 1 failed**
- 完整测试：`test --no-daemon` → **806 tests, 1 failed**
- 唯一失败：`TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`，读取硬编码 macOS 路径 `/Users/wyc/...`，与本实现无关，未修改该测试。
- `git diff --check` / `git diff --cached --check`：通过。
- 已确认生产代码无 `failureCooldown`、`P2PFairTransferScheduler`、`hasTransferDemand`、`incoming.size()` 速率补偿残留。

## 已知偏差/风险

1. 完整/城镇测试不是全绿，原因是上述既有环境相关测试；不是本次改动引入。
2. 未跑游戏内 before/after 性能基准；性能证据是结构级的：计数 `IItemHandler` 测试证明无正预算时不读取物品槽，`sourceScanCount` 仅在正预算尝试后递增。
3. `WarehouseInterfaceTransfer.java` 未改动，因为现有 `Result` 已满足方块级结果分类。

## 建议 Review 重点

- `TransportTransferBudget` 状态转换是否正确：`beginAttempt` / `recordSuccess` / `recordFailure` / `pause` / `wake` / `reset`
- 降频后首次成功清空额度逻辑（`clearTokensOnNextSuccess`）
- 仓库接口 `validateAndBalance` 是否在任何路径意外访问外部能力
- P2P `performOutgoingTransfer` 是否在“正预算后才解析能力”和“对端未加载不算失败”之间保持一致
- 删除旧公平轮询后，是否真的不存在 `incoming.size()` 速率补偿残留
- `P2PItemTransfer` 简化后是否仍保持物品守恒和 recovery stack 语义

## 测试运行备注

本机 Gradle wrapper 的 `.lck` 被占用，使用已安装发行版直接运行：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.2'
$env:GRADLE_USER_HOME="$env:TEMP\fh-gradle-home"
& "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.1.1-bin\9wiye5v2saajue4irfo8ybqfp\gradle-8.1.1\bin\gradle.bat" test --no-daemon
```

## Git 状态

相关文件已 `git add`，**未 commit**。无关 untracked 文件（如 `.workbuddy/`、其他 plan/diary）未纳入。

## 2026-08-26 20:26:31 +08:00 Codex 复核回复

代码复核发现并修正三处调度边界：首次运行原本没有安排基础间隔；P2P 对端未加载时只清额度却没有延后下一次探测；
等待预算期间会清除已经确认的“对端未加载”状态。修正后首次运行遵守设置速率档位，对端未加载按当前间隔复查且不增加
失败退避，等待 tick 保留最近确认的可视状态。

原 `schedulerGatesHandlerAccessUntilPositiveBudget` 由测试代码自行决定正预算后才调用搬运，不能证明生产接线，已删除；新增
`TransportTransferBudgetTest` 用例直接覆盖首次间隔和 `defer` 时序。聚焦测试共 39 个全部通过；城镇测试共 431 个，除
既有 `TeamTownActualSaveCodecProbeTest` 因硬编码 macOS 存档路径缺失失败外，其余 430 个通过；完整测试共 807 个，
同样只有该测试失败，其余 806 个通过。详见
`diary/2026-08-26_20-26-31_transport-transfer-cadence-review-fixes.md`。
