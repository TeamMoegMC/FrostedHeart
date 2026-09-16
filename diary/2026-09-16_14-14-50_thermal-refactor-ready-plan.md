# 将温度重构计划整理为实施稿

- Time: `2026-09-16 14:14:50 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `完善并精简既有计划；未开始生产代码重构`

## Completed

- 将[原计划](../plans/2026-09-16_00-15-34_thermal-readability-maintainability-refactor.md)由 406 行整理为 224 行，合并重复的调查、职责和限制说明，保留必要状态交接细节。
- 确定 `TopologyPlan → TopologyUpdatePlanner`、`MutableMaterialSample → mesh.MaterialSample`、`InfraredReadCursor → ReadCursor`；明确 ThermalPage 是术语说明，不新建包装类，不机械改为 Minecraft Section。
- 确定两项类型归位：共享材料样本与现有红外采集器。Input 保留生命周期、查询入口和检查点交接，移出采集算法；SectionOwner 与请求校验回调保留并说明原因。
- 增加迁移索引、复杂条件和主流程注释的真实代码前后示例；六批工作均有完成条件。
- 合并性能和人工阅读验收，保留 worker 重启、发布读取、红外重试/finally、多次材料变更回放、潜热 ACK、最新导热默认值等约束。

## Decisions

- 不再用反复追加规范替代实施决定；剩余结构问题明确保留边界，不自动扩展拆类。
- 固定材料导热修复后的基线；不恢复旧 JUnit，不改行为缺口或游戏平衡。
- Documentation impact: 本轮只改计划及 diary；活文档描述的生产代码位置尚未变化。

## Validation

- 从当前源码核对三个示例及 Input 的相变请求校验依赖。
- 检查命名、职责边界、实施次序和验收口径一致；文档改动不运行编译或游戏测试。

## Remaining

- 计划 ready；六批生产重构尚未开始。
