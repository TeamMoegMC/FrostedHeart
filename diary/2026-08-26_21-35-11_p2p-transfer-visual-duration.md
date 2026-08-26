# P2P 传输状态判定与持续时间修正

- Time: `2026-08-26 21:35:11 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed`
- Scope: `P2PTerminalBlockEntity`, P2P visual-state tests, and P2P living documentation

## Completed

- 将“传输中”的触发点从正预算传输尝试开始移到 `movedItems > 0` 的成功分支。
- 成功传输后，发送端和接收端的状态持续时间改为至少 20 tick，并取成功后当前运行间隔的两倍。
- 连续成功传输会按最新恢复后的运行间隔重新计算并刷新持续时间。
- 补充持续时间边界测试并更新 P2P 现行文档和实施计划结果。

## Decisions

- 固定 4 tick 无法覆盖低速设备的运行间隔，也会把零搬运尝试误报为传输中。
- 使用成功后的间隔，使失败退避恢复时的显示时长与下一阶段实际调度频率一致；20 tick 下限保证高速设备的单次成功在
  GUI 同步后仍可辨认。

## Validation

- Java 17 聚焦测试：`P2PTerminalScreenTest`、`P2PItemTransferTest` 和
  `TransportTransferBudgetTest`，共 33 个测试，全部通过。
- Java 17 城镇测试：共 432 个测试，431 个通过；唯一失败为既有
  `TeamTownActualSaveCodecProbeTest#actualSaveSurvivesTheFullSyncCodec`，其硬编码 macOS 存档路径在本机不存在，
  与本次修改无关。
- `git diff --check` 通过。

## Remaining

- None after validation completes.
