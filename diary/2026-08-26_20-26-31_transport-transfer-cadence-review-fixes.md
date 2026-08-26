# 城镇物流传输频率复核修正

- Time: `2026-08-26 20:26:31 +08:00`
- Author: `Codex; OpenAI GPT-5; code review and implementation role`
- Status: `completed`
- Scope: `TransportTransferBudget`, P2P sender scheduling, cadence tests, and town transport documentation

## Completed

- 让状态机初始化或重置后的首次运行先等待设置速率对应的基础间隔。
- 新增 `TransportTransferBudget#defer`，用于 P2P 对端未加载时清空额度并按当前间隔延后探测，同时保留失败退避级别。
- 等待预算期间不再清除 `peerUnavailable`，避免“对端未加载”状态闪烁及随之产生的方块状态写入。
- 删除不能验证生产接线的 `schedulerGatesHandlerAccessUntilPositiveBudget`，新增首次间隔和延后探测的直接状态机测试。
- 更新 `docs/town/p2p-logistics.md`、`docs/town/implementation-reference.md` 和实施计划结果。

## Decisions

- 对端未加载仍不算零搬运失败，因此不扩大退避；但它必须服从当前运行间隔，不能在等待 tick 重复探测端点。
- `pause` 保留给红石暂停等离线额度控制；`defer` 表达“保留当前退避并安排下一次探测”的独立语义。
- 等待 tick 没有新的端点事实，视觉状态沿用最近一次实际探测结果。

## Validation

- Java 17 聚焦测试：`TransportTransferBudgetTest`、`WarehouseInterfaceBalanceTest`、`P2PItemTransferTest`、
  `P2PTerminalScreenTest`，共 39 个测试，全部通过。
- Java 17 城镇测试：共 431 个测试，430 个通过；唯一失败为既有
  `TeamTownActualSaveCodecProbeTest#actualSaveSurvivesTheFullSyncCodec`，其硬编码路径
  `/Users/wyc/Development/FrostedHeart/run/saves/...` 在本机不存在，与本次修改无关。
- Java 17 完整测试：共 807 个测试，806 个通过；唯一失败仍为上述硬编码路径测试。
- `git diff --check` 和 `git diff --cached --check` 通过。
- 生产源码中无 `failureCooldown`、`P2PFairTransferScheduler`、`hasTransferDemand` 或 `incoming.size()` 速率补偿残留。

## Remaining

- 游戏内长时间运行和真实整合物流组合仍按现行 P2P 文档列为发布回归项目。
