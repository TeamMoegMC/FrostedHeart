# 材料导热不再由相变能力覆盖

- Time: `2026-09-16 01:03:57 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `用户另行授权修正材料导热默认值；不属于可读性重构`

## Completed

- `MinecraftThermalProfiles.prepare` 统一从材料分类取导热系数，再采用配方显式 `conductance_w_per_k`；删除“存在相变边就使用统一系数”的分支。
- 泥土单完整面 G 从 5.0 降至土类 1.0 W/K；石头保持石类 1.4 W/K。规则适用于所有材料，水、冰、冻土等有相变的材料同样回到各自分类默认值。
- 删除 `FHConfig.Common.ThermalRuntime.phaseFaceConductanceWPerK` 字段和配置定义，没有留下无效配置或兼容读取。
- 在现有 `ThermalTransitionDataGameTests` 增加原生石头/泥土、石头新增相变仍保持导热值、显式配方覆盖三类断言，没有新增 JUnit 或测试框架。

## Decisions

- 热容、潜热、源功率、静态辐射、更新频率、求解器和存档逻辑保持。这里只改变初始化/重载时的 G 选择。
- 未增加热路径判断、数组或运行时对象。热行为改变可能影响温区驻留和后续工作量，因此不声称总 CPU/内存结果精确相同。
- Documentation impact: 更新热源/材料文档、runtime 文档及重构计划中的新基线。未改配方或源表，不涉及伴随整合包文件。

## Validation

- Java 17：`gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`，BUILD SUCCESSFUL；102/102 GameTest 全部通过。
- 导热值与显式覆盖断言通过；活动/休眠相变、两层接触、保存重建、红外和时间步等现有回归通过。
- 搜索生产源码无 `phaseFaceConductanceWPerK` 剩余引用。Forge 在测试配置加载时自动移除了旧键，未增加迁移代码。
- 清理本次生成的 `run-gametest/world`、`run-gametest/logs/debug.log` 和 `latest.log`；无临时源码、Python 或重定向验证日志。

## Remaining

- 未进行游戏内主观平衡体验或新的多人性能测量；后续可读性重构保持本次导热规则。
