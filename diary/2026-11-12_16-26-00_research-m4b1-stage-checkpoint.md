# M4-B1 阶段核心、临时权限和研究页 checkpoint

- Time: `2026-11-12 16:26:00 +08:00`
- Author: `Codex / OpenAI；FH M4 实现者`
- Status: `partial`（编译已过；GameTest在启动时失败，先前30项通过的报告已撤回）
- Scope: FH StageDefinition/StageRunState/ResearchStages、schema5、当前任务服务、临时配方来源、研究快照与推进界面。

## Completed

- 有序阶段、默认单阶段适配、进入时冻结变体与任务集合、可选任务、手动推进和最后完成。
- 临时配方来源并集、受管声明、来源单独撤销；M3双成果映射扩展至非空 Design/Construction 集合以支持旧 iron 单Design。
- 全阶段观察用途登记、当前任务服务统一四类旧任务；保留旧样品字段和schema4读取。
- UI阶段目标/方法/结论/可选项，未进入变体不下发；修复顶层空requirements判断，不给旧单阶段额外推进按钮。

## Decisions

- 按修订M4-A，开始条件两组选项继续由内容tag表达AND；没有OR框架。试验绑定无个人owner。
- 风道与原型独立B2继续实施，不能把本checkpoint当作完整M4交付。

## Validation

- [m4-b1-validation.log](../build/m4-b1-validation.log)：compileJava + compileGameTestJava + runGameTestServer，Gradle BUILD SUCCESSFUL，4m21s，但16:21:14数据包启动失败，**没有执行GameTest、没有30项通过证据**；Java17进程环境、build/m4-b1-gametest-20261112。
- 新增3项StageResearchGameTests覆盖：未来任务拒绝、ready不自动推进、就绪后可选任务/补知识、分支固定、旧advance重包、最后完成幂等；schema4缺阶段状态/缺定义恢复/二次读取/旧平铺无额外点击；两run临时来源并集、结束A保留B、永久来源接管及AccessSource codec。
- 本轮原M1–M3B的27项回归尚未实际运行，不能引用旧轮次结果代替。TrialKilnGameTests仅更新当前版本断言为5，仍计划验旧样品记录保留。
- [m4-b1-compile.log](../build/m4-b1-compile.log) 首轮因客户端Requirement解码漏新增字段失败，已补齐，生产编译通过；运行时暴露另一处Codec循环初始化，尚待修复后重跑。
- 无客户端、无部署、无Git变更操作。M3C仍使用已固定产物；其在途build配置未覆盖。

## Remaining

- B2完整风道会话、真实依赖试吹/对象关联及正式JSON验证正在实施；最终阶段将另写完整文件清单与最终验证日记。
- 编译后继续B2源码，当前工作区不是冻结在此checkpoint；编译产物/日志对应本次核心验证边界。

## Evidence correction

本文件最初把Gradle的exit0误写为30项GameTest通过，主协调逐行复核后指出错误，已在本地未提交交接中更正。
真实首因是 KnowledgeCondition.CODEC → ProjectRequirement.field → ProjectRequirement.CODEC → ObservationTask.CODEC → 未初始化的 KnowledgeCondition.CODEC。
将KnowledgeCondition默认字段帮助方法改为直接使用KnowledgeKey.strictOptionalField，真正打断初始化依赖；不靠换加载顺序。
后续必须看到当轮 GAME TESTS COMPLETE / All N required tests passed，并核对是否存在启动失败，才能报测试通过。
