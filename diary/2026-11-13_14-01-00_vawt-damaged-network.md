# VAWT 损坏状态与发电更新顺序修复

- Author: Codex
- Date: 2026-11-13
- Status: 实现与真实 Forge GameTest 服务端回归完成。
- setDamage 立即刷新损坏标记；getGeneratedSpeed 直接检查剩余寿命。
- lazyTick 同步计算倍率、扣除寿命后，再调用 updateGeneratedRotation，避免先发电后损坏。
- 验证：runClient 的 compileJava 阶段通过；git diff --check 通过。已尝试启动客户端，尚未完成游戏内放置及网络回归，不能宣称验证通过。
- 文档影响：更新 docs/vawt.md 的生命周期说明。

## 服务端回归结果

- 2026-11-13 14:19 UTC：`./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_vawt -PgameTestDirectory=build/vawt-gametest -x cleanResearchGameTestWorld` 正常退出，BUILD SUCCESSFUL，All 1 required tests passed。
- `VAWTNetworkGameTests.brokenPlacementAndExhaustionLeaveNoRotation` 顺序覆盖两项场景：损坏物品恢复后立即无发电、25 tick 后轴静止；健康涡轮实际驱动轴后，寿命耗尽同次更新停止，25 tick 后轴仍静止。
- 前两轮健康场景分别受测试高度与地形遮挡影响而失败；最终场景移到世界顶部并清空检测区，复用测试世界减少生成耗时。没有放宽健康发电断言。
- 日志：`build/vawt-gametest/logs/latest.log`（生成产物，不提交）。客户端控制桥启动成功，但未完成 GUI 实放回归；用户随后明确以这两项服务端场景通过作为提交推送条件。
