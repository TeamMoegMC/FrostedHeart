# 自然温度查询与玩家对流系数复用

- Time: `2026-09-11 00:21:13 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `WorldTemperature / PlayerThermalModel / PlayerTemperatureComputation`

## Completed

- 玩家每次更新只计算一次自然对流系数，作为局部double传入五部位及等效温度计算。
- 地下自然空气跳过零贡献的气候查询；自然方块温度直接使用原自然公式和下限，绕过零热量加热路径。
- 更新现有计划、玩家温度及世界温度文档。

## Decisions

- 无新缓存/对象/上下文成员；保留部位防护差异、海拔公式及原公共公式接口，旧城镇模拟器继续调用applyHeat。

## Validation

- Java17 runGameTestServer：完整55项真实Forge测试全部通过，BUILD SUCCESSFUL；无JUnit、无测试类排除。
- 新增10个高度、40组自然方块温度对照及地下空气下限验证；现有玩家与世界热学测试通过。日志run-gametest/thermal-shared-coefficients-verification.log；git diff --check通过。

## Remaining

- 整体耗时改善比例尚未实测。
