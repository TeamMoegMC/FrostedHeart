# 依据5～10°C容差选择局部空气混合

- Time: `2026-09-15 18:49:35 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `数值试验与计划；生产源码未改`

## Completed

- 七轮Forge受控GameTest分别完成56、288、64、256、256、256、512组运行，累计1688组（含重复与边界/成本校验）。每轮BUILD SUCCESSFUL。
- 比较全域加速、局部加速、近表面/近源细分及逐块参考。早期固定高度2倍局部混合最高9.24°C；三轴检查揭示最高11.78°C，因此没有据固定高度结果选2倍。
- 用户明确5～10°C温差可接受后，撤回增加Air节点的优先推荐，选择四格范围内的直接Air换热4倍：最终房间/无顶采样最大平移极差6.73/5.85°C，600秒远处额外升温约0.35/0.03°C。
- 8倍在房间600秒仅把6.73改善到6.39°C，却继续增加远处升温；不选择更强倍率。
- 最终仅增强直接Air面，间接route保持原阻力。原型早期按路由中点加速的做法已撤回并重测。

## Decisions

- 当前方案及完整方法/数据集中在[固定近源混合计划](../plans/2026-09-15_18-49-35_thermal-local-air-mixing-tradeoff.md)。状态draft，待生产动态集成与负载验收，不称为任意场景最优保证。
- 复用原节点、原源端口、原读取和原G数组。变化集中在真实方向、源区域面覆盖及fragment重建触发，不增加空气状态/温度插值/细分生命周期。
- 两个读数阈值分开：用户允许≤10°C网格误差，不等于允许远处额外升温10°C。
- Documentation impact: 更新主计划后续入口与新计划，living docs保持当前未修复的事实。

## Validation

- 使用生产engine构建拓扑，固定驻留后直接驱动生产ledger和solver；未模拟真实玩家自动驻留、主线程capture、source index增删、query发布和相变ACK。
- 最终512组使用52×20×40固定域、X/Y/Z各0～3平移、四个8000 W源、初温-40°C、600秒，记录30/180/600秒与远点首次升温1°C时间。
- 源180/600秒Air输入4608000/15360000 J，未接收/降级0；最终最大总域能量误差0.0000449 J，所有solver步骤无数值降级。
- 最终匹配几何下的节点/边数量、arena原始数组及直接AirPairs载荷完全一致。局部4倍稳定solver计时区间0 B分配；房间基线/4倍176.2/178.4 μs，无顶147.0/146.4 μs。不是完整cut或MSPT，未测最终retained heap。
- 域外绝热、无风/自然散热；扩大域发现无顶长期值有边界敏感性，不能当作真实室外预测。房间对应结果稳定。
- 测试以临时直接面重建表达真实方向与区域系数。细分对照曾临时使用独立签名表99编码选择已有单Air分配分支后恢复100%；这些手段没有进入生产源码。
- `gradlew.bat runGameTestServer --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false --init-script build/air-spread-study/init.gradle`，临时namespace airspread。正常90项基线未重跑，因为生产源码未变。
- 清理本轮临时Java/编译类、Gradle脚本、CSV、日志和生成测试世界；保留计划中的方法与关键数据。用户实际run/saves与run/logs未操作。

## Remaining

- 生产浮力方向与局部系数更新尚未实施。
- 源增删/位置变化、重叠范围、fragment-only提交和预算延迟需在实际生命周期验证；更长时段、原现场、完整多人CPU/heap待完成。
