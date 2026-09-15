# 休眠相变计划修订：复用既有随机更新

- Time: `2026-09-14 16:46:57 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `源码调查与计划修订；没有修改 Java 或现状行为`

## Completed

- 核实原通用 `StateTransitionData` 随机转换、地表水冻结、雪层/岩浆专用入口，以及 `BlockStateBaseMixin_RandomTick` 候选覆盖。此前“没有 Page 就无法转换”遗漏了环境阈值路径，已纠正。
- 更新[统一休眠冷却计划](../plans/2026-09-14_16-22-45_thermal-unified-dormant-natural-cooling.md)第 1–3、5–10 节及主材料计划入口，增加真实调用顺序、三结果分流、相变 H 提交、随机覆盖、成本及验收。

## Decisions

- 取代前一份日记中的“等待活动流程才改变方块”决定：在既有随机更新命中时提交休眠相变，不需要 Page；不添加独立轮询、候选位图或强制区块加载。
- tracked 自然转换使用 H/潜热，untracked 保留原自然玩法；显式解析场玩法继续受控能量转换。
- 暂缓相变不能落回旧阈值，也不能取消不相关的原生随机行为。成功转换以原作用域和变更钩子交接精确 H/tick，不预写状态或创建假 worker ACK。
- Documentation impact: 修改计划并新增本日记，未提前改写现状 docs；方案仍未实施。

## Validation

- 交叉阅读随机 tick Mixin、StateTransitionData、MinecraftPhaseController、MinecraftThermalInput、MaterialSectionState/Editor、BrickMigrationKernel 和 mixin 注册。
- 文档修改检查与链接检查；本轮不运行 Java 功能测试，随机覆盖/数值/性能项目留在实施验收中。

## Remaining

- 实施修订后的计划；验证 source 拆除后无 Page 的转换、潜热不被独立入口绕过、世界更新无额外 Chunk 加载、配方重载计数及实际抽样成本。
