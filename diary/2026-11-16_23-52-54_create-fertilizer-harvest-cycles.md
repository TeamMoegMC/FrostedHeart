# Create 连续收割肥料记录验证

- Time: `2026-11-16 23:52:54 UTC`
- Author: `OpenAI Codex`
- Status: 完成，专用 GameTest 3/3 通过。
- Scope: 新增连续收割测试，未改生产逻辑，未提交推送。

## 决策与验证

针对用户关于 Create 下一次收获是否继承旧增产记录的疑问，新增 `CropFertilizationGameTests.createConsecutiveHarvests`。直接调用当前依赖的 `HarvesterMovementBehaviour.visitNewPosition`，执行实际掉落生成、种子预留及作物补种。只覆盖 `dropItem` 接收掉落以统计数量；生长由测试逐年龄推进并调用 `CropFertilization` 结算入口。

三轮使用同一位置、同一 Create 行为实例：第一轮全程基础增产肥，小麦 2；第二轮换普通耕地，小麦 1；第三轮重新施基础增产肥，小麦 2。每次收割后验证补种年龄为 0、旧增产记录为 0。各轮间跨 tick 验证延迟清理后仍能进行下一轮。证明此收割路径不继承上一轮记录，也不妨碍之后重新积累。

命令：`./gradlew runGameTestServer --offline -PgameTestNamespaces=frostedheart_fertilizer -PgameTestDirectory=build/fertilizer-gametest`。
日志：`build/fertilizer-create-cycles.log`，`All 3 required tests passed`，`BUILD SUCCESSFUL in 2m 42s`。既有两个用例也通过。`git diff --check` 通过。

## 文档影响与边界

更新 `docs/climate/crop-fertilization.md` 的验证数量和连续收割范围。未执行客户端运动装置、自然随机生长全周期、独立进程重启验证。无需修改生产实现。
