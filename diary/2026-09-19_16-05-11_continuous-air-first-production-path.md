# 连续空气首个生产调用链与篝火场景

- Time: `2026-09-19 16:05:11 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial; 实施继续，尚未完成整体计划`
- Scope: `连续空气首个生产调用链、真实篝火测试与编译/投影复用`

## Completed

- 新增共享四格空气基函数、几何连通分支编译、有限R6局部响应、容量与水平/竖直导热算子、联合材料试算及块预条件PCG。
- 接入正式source ledger的时间段/impulse记录、AIR_STENCIL绑定、Engine更新和QueryPublication空气系数双缓冲；近源供能、材料接触和查询使用同一空间场。
- 补充实际FarField面和route出口面描述、跨Page依赖、空间HotMask/前沿判断；物品quarter缓存命中后重算精确空气。
- `FHConfig.COMMON.THERMAL_RUNTIME.continuousAir`为启动配置，默认false。当前是开发中的后端，不能据此认为所有合同已完成。
- 加入 `ContinuousAirProductionGameTests`：真实房间、燃料capability、打火石点火、铁锹熄火、正式空气/材料查询和余热。新增 `-PgameTestNamespaces=frostedheart_production`，没有执行JUnit或独立数值测试。

## Decisions

- 保留当前同步worker调用，不实施新调度状态机。数值内核为具体类，无通用矩阵框架。
- 几何未变时复用静态shape数据和volume矩阵，布局投影使用已有场热启动。局部shape的支撑按Brick索引匹配，不让每Brick遍历所有源。
- 熄火后的40 tick余热断言曾因发布尚未恢复而失败；优化模板/矩阵复用及投影热启动后，保留原断言通过，没有改为宽松超时。
- 旧存档兼容不在范围。新版空间存档尚未完成，不把当前旧codec行为当作R4保存交付。
- Documentation impact: 本条记录阶段结果；最终行为文档在后续实现收敛时更新。

## Validation

- Java17 `compileJava compileGameTestJava`通过。构建使用用户原有Gradle缓存；项目内旧缓存缺少依赖，已改回默认用户缓存，不复制生产源码或依赖版本。
- 旧后端真实篝火测试1/1通过；日志 `build/continuous-air-production-baseline.log`。
- 新后端粗场初次真实场景1/1通过；日志 `build/continuous-air-production-first.log`，当时尚无局部模式。
- 局部模式接入后的首次运行失败于熄火后40 tick发布恢复，原始日志 `build/continuous-air-production-modes.log`保留。
- 缓存/投影优化后的同场景1/1通过；`build/continuous-air-production-cached.log`中tick600附近近源11.762°C、4格处10.232°C；tick640近源11.773°C、远点10.239°C；tick1200近源10.484°C、远点10.211°C。公开查询显示10个basis项，即8粗项加2局部模式。
- 以上温度是该场景记录，不是≤2°C认证或大型服务器性能结论。

## Remaining

- 继续实现并验证新版空间存档/恢复、材料相变端点和迁移细节、prepare/commit资源完整预留、预算记账及材料精确消元。
- 增加真实改块/跨Brick及Page、相变、多源、保存重启场景；尚未测试大型服务器负载。
- 当前全布局编译/投影及部分生命周期仍需收敛，默认启用条件未满足。
- 开发测试用 `run-gametest/config/frostedheart-common.toml` 临时开启continuousAir；原文件备份在 `build/continuous-air-common-before.toml`，结束测试后恢复。
