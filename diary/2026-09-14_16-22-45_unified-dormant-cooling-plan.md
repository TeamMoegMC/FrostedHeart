# 空气与材料统一自然休眠冷却：方案调查

- Time: `2026-09-14 16:22:45 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `仅源码调查及工程方案；未改变运行时行为`

## Completed

- 核实 Air 指数温差衰减、材料 H 冻结、保存重定基准、一次性 sourceSustained 保温，以及红外只依赖 stored revision 的缺口。
- 写入[统一休眠方案](../plans/2026-09-14_16-22-45_thermal-unified-dormant-natural-cooling.md)，包含数值/潜热边界、局部时间锚点、COW、恢复/编辑、NBT、红外增量和性能验收。

## Decisions

- 复用现有半衰期的按需自然松弛，明确不复现 live 不同材料的物理冷却速度；不保存几何/天气历史，不增加 scheduler 或 Mixin。
- 同龄材料共用 scalar tick，出现异龄才分配逐记录 ticks；避免用单 Section/Brick 时间重置无关物体，列明最坏 8 bytes/记录及快照峰值。
- 保存不推进状态；休眠潜热最多到世界转换端点，恢复后走既有 ACK；删除休眠源豁免，不更改真实热源或解析场玩法。
- Documentation impact: 仅计划和主计划后续入口；现状 docs 未声称新功能已实现。

## Validation

- 阅读 `DormantChunkThermalState`、`MaterialSectionState`、`MinecraftThermalInput`、`MaterialThermalLaw`、`MaterialEnthalpyExchange`、`ThermalCellArena`、红外请求协议及相关 docs/测试。
- 本轮没有修改 Java/资源或运行功能测试；性能目标与数值场景是下一轮实施的验收项，不是已通过结果。

## Remaining

- 实施并验证新计划；同半衰期、气候历史近似和等待活动流程才改变相变方块是明确模型边界。
