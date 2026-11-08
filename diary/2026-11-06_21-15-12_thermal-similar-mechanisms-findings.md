# 河道修复后的同类机制检查与剩余等待问题

- Time: `2026-11-06 21:15:12 +08:00`
- Author: `OpenAI Codex / GPT-6; primary investigation and production-validation agent`
- Status: `completed`（定向调查；已确认限制及未闭合的退出验收明确保留，未实施新生产修复）
- Scope: 空气/材料准入、自然背景、粗细切换、通风支撑、恢复种子、休眠相变等待

## Completed

- 按[检查计划](../plans/2026-11-06_17-29-00_thermal-similar-mechanisms-inspection.md)检查材料/空气的扩张条件及自然背景扣除、通风支撑、恢复输入责任，回答用户是否仍有类似问题。没有把河道的有限测试结论扩大为全系统无缺陷。
- 真实Forge确认一个仍存在的边界限制：休眠水体已经到冻结终点1493000J，但四个水平邻居都是水时不允许变块。无源、自然-23.7°C下12000tick仍保持水/-5°C/原H/降温分支，98次维护未删除；西侧改为石头后约61tick完成薄冰转换并开始继续降温。新测试通过只表示现象和条件恢复均已复现，不表示这个等待行为已经修复。
- 重跑真实跨自然背景粗细构建/恢复1/1通过：自然背景跨度18°C，最大单次粗化温度跳变约1.38987°C、恢复约0.02834°C，能量误差约3.73e-9J；6400J输入跨两次取消仅发布一次，重放能量/温度差0。
- 无源粗档退出场景原断言两次失败。第二次只增加测试日志，源为0且pending源J为0，区域在细档恢复后仍持续提交；最后点温-11.9311°C、点自然查询-11.9°C，仍1region。一个点不能证明全区域材料/空气责任已低于各自背景门槛，因此保留失败，未将它判定为新增重复产热或无效驻留。

## Decisions

- 本轮仅新增 `ThermalBlockedPhaseProductionGameTests` 与既有粗档用例的日志，生产代码和上一轮产物没有因本次调查重新修改/打包。
- 已完成相变遇到世界条件阻止时，普通显热半衰期不再能预测其退出；不能通过直接删掉潜热记录把这个边界行为说成解决。后续实现需处理实际物态、热量交换和等待条件的一致性。
- Documentation impact: 在[余温生命周期](../docs/climate/thermal-residual-lifecycle.md)明确记录这条当前限制；新增[验证摘要](../docs/climate/validation/thermal-similar-mechanisms-2026-11-06.json)，包含两个通过场景、两个退出失败及未到达的后续检查。`design/`保持只读，无配套仓库变更。

## Validation

- Java17 `compileGameTestJava`成功，生产 `compileJava UP-TO-DATE`；日志 `build/thermal-similar-mechanisms-compile.log`。
- `similar-natural-baselines`：真实Forge1/1通过，`similar-blocked-water`：现象刻画1/1通过；均热保存/worker错误0、关闭root0。
- `similar-coarse-recovery`、`similar-coarse-diagnostics`：各1项退出断言失败，均错误0、关闭root0；没有把尚未到达的保存/卸载/重载/重新点火写成通过。
- 原始数据、源码锚点、配置范围和时钟含义见验证摘要。没有复制历史源码重编、增加路径hash要求或修改工具版本约束。

## Remaining

- 处理已完成相变被业务条件阻止时的持续平台/记录保留问题；当前仅确认和解释，尚未实现新规则。
- 闭合无源区域退出失败的实际全区域温差、材料责任与门槛原因，再决定是夹具假设还是生产生命周期问题。
- 完整整合包、客户端画面、超长多孔通风结构和新的跨JVM验证不在本轮已验证范围。
