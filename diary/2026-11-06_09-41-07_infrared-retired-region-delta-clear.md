# 红外区域缩小或退出后的旧热色增量清除

- Time: `2026-11-06 09:41:07 +08:00`
- Author: `OpenAI Codex / GPT-6; primary engineering agent`
- Status: `completed`（定位、修复、真实 Forge 回归及生产打包；未进行客户端画面验收）
- Scope: `QueryPublication.installRegions()`、`InfraredTopologyGameTests`、红外网络与世界温度当前说明

## Completed

- 用户报告红外旧热色不自动清除，需要重进游戏才恢复蓝色。客户端本来每 40 游戏 tick 错峰请求；问题不在缺少自动请求。
- 定位到查询区域安装的删除遗漏：旧实现仅调用 `markRegionalInfrared()` 标记新增/替换区域的当前范围，未标记退出区域的旧范围。区域撤销或缩小时，若其他材料仍使同一 Page 保持 presence，旧 Brick 不进入后续增量，客户端保留旧热色；重新进入游戏的 full 会清空 mirror，因而掩盖此缺陷。
- `QueryPublication.installRegions()` 现在通过既有 `addInfraredChange()` 标记被移除快照的旧 Page/Brick mask。它与新范围一起提交现有 epoch；已无材料/解析场的旧 Brick 经 `InfraredCapture.writeBrick()` 使用既有 INVALID 编码清除。没有新增轮询、定时全图清空或第二份温度状态。
- 新增 `InfraredTopologyGameTests` 两项 Forge 回归：完全撤销区域但同页另一温区仍可读；同一 region 替换为更小范围。前者也验证另一温区温度可读、无须被该删除单独标脏。
- 使用当前工作区 `mod_version=0.7.9` 生成 `build/infrared-refresh-20261105/libs/frostedheart-1.20.1-0.7.9.jar`；本任务未修改版本号或替换本地实例 JAR。

## Decisions

- 修复服务端区域变更的通知归属。仍有真实材料余温或解析场时继续显示其当前值，实际数据退出才恢复缺失值蓝色占位。
- 保留同一文件已有的余温保存变更及其他工作区改动；本次生产改动仅在区域退出循环中追加旧范围的标脏。
- Documentation impact: 更新 [world-climate-and-temperature.md](../docs/climate/world-climate-and-temperature.md) 与 [data-lifecycle-and-integration.md](../docs/climate/data-lifecycle-and-integration.md)，说明新旧区域范围的增量清除合同和本次核对日期。
- 无配套整合包仓库或配置变更。此修复位于服务端发布路径；现有客户端已能处理该 INVALID 协议。

## Validation

- 修复前：Java 17、当前工作区、独立输出和运行目录，真实 Forge GameTest 两项均在预期删除标记断言失败，`2 required tests failed`。日志：`build/infrared-refresh-before-20261105.log`。
- 修复后：相同测试 `All 2 required tests passed`，`BUILD SUCCESSFUL in 2m 24s`。日志：`build/infrared-refresh-after-tests-20261105.log`。这是生产查询目录和增量 epoch 的真实 Forge 验证，未将它描述为实际客户端纹理画面验收。
- 生产打包：`assemble` 成功，`BUILD SUCCESSFUL in 56s`。日志：`build/infrared-refresh-package-20261106.log`。最终 JAR 为 14,593,468 字节，Manifest 为 `1.20.1-0.7.9`；相关生产类存在，新增 GameTest 未进入生产包。`javap` 确认最终 `installRegions()` 包含旧范围的 `addInfraredChange()` 调用。
- 工具依赖解析曾在 ForgeGradle 的动态版本网络请求中等待，相关打包尝试停止后，以调用级 init script 从本机 Maven 元数据和已有工具解析原动态版本约束，再完成普通重新混淆；没有修改项目依赖约束或固定工具源码版本。中断记录：`build/infrared-refresh-after-20261105.log`；临时构建设置位于 ignored `build/infrared-refresh.init.gradle`。
- GameTest 构建沿用现有热系统验证对未完成 deposit 夹具的排除，仅声明本次两个 namespace 用例；没有新增或运行 JUnit。
- 本次修改文件的 `git diff --check` 通过。

## Remaining

- 修复包尚未替换到运行实例，未独立验证真实客户端的连续画面、网络延迟或多人同时观察。两项回归证明已定位的服务端删除标记遗漏被修复，不代表所有红外渲染问题均已验收。
