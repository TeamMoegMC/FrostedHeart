# 河道扩张之后的同类机制检查

- Time: `2026-11-06 17:29:00 +08:00`
- Authors: `OpenAI Codex / GPT-6; primary investigation and production-validation agent`
- Status: `completed`
- Scope: 空气自然背景、粗细切换/退出/重载、通风支撑、已完成但业务条件阻止的相变
- Related: [河道修正](2026-11-06_09-56-00_river-thermal-expansion-production.md)、[余温合同](../docs/climate/thermal-residual-lifecycle.md)

## Goal

回答用户“还有类似的问题存在吗”。以明确范围的源码检查和真实 Forge 观察区分仍存在的边界问题、已覆盖路径及尚未验证项，不把一次修复或有限回归写成全系统无问题。

## Steps

1. 检查材料/空气请求、自然背景扣除、通风支撑及恢复种子的所有权；区分保留与扩大。
2. 重跑真实跨自然背景粗细恢复、无源粗档冷却/保存/卸载/重载测试。
3. 建立有真实世界业务限制的已完成水相变记录：四周为水时观察长期温度/记录；改变一侧为真实河岸后观察是否恢复转换。
4. 记录仍存在行为、证据及其对玩家的影响，更新相关文档和日记。

## Outcome

- 定向调查完成。确认休眠已完成相变会在世界变块条件不满足时长期保持平台：真实Forge12000tick无源水体仍为-5°C/1493000J，改变一侧实际边界后61tick恢复转换。这是仍存在的限制，本轮没有修改生产规则。
- 跨自然背景粗细构建/恢复1/1通过；粗化单次温度跳变约1.39°C，能量仍守恒，属于已观察近似代价。
- 无源粗档退出原断言两次失败；保留日志和最后读取，未仅凭单点接近自然值宣称全区域应退休，后续需闭合材料/空气责任。
- 结论与限制写入[验证摘要](../docs/climate/validation/thermal-similar-mechanisms-2026-11-06.json)、[现行余温说明](../docs/climate/thermal-residual-lifecycle.md)及[日记](../diary/2026-11-06_21-15-12_thermal-similar-mechanisms-findings.md)。
