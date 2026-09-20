# 连续空气代码可维护性设计

- Time: `2026-09-18 21:27:47 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed; 设计交付，生产代码未实施`
- Scope: `连续空气代码职责与可读性；修订提前确定的调度实现`

## Completed

- 新建[代码设计](../plans/2026-09-18_21-27-47_continuous-air-code-maintainability.md)，明确Engine编排、solver试算、operator/PCG计算和publication读取的职责与依赖。
- 补充主调用骨架、可变数组owner、trial/正式状态边界、单位与索引命名、接触算子示例、常见修改入口和具体评审标准。
- 同步[工程计划](../plans/2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md)，默认复用同步process；切片由P2测得的调度问题决定，移除预定MORE_WORK接口和队列交换算法。

## Decisions

- operator与PCG为solver包内具体计算类型，不引入通用矩阵框架或策略层。优化集中在内核，Engine保持可顺读的生命周期调用。
- 修订此前计划将续算直接列为必做的决定；源记录一次、trial可重试、完整cut/保存延迟验收仍保留。
- 用户取消旧存档兼容的要求保持，无v4导入/双写/兼容adapter。
- Documentation impact: 只补充并修订plans，生产行为未改变，活文档不更新。此前diary保留历史。

## Validation

- 对照已有温度可读性重构计划与当前MaterialExecution、WorkerBrickTopology等代码风格。
- 检查文档链接、代码围栏、空白及主计划的调度/结构引用一致性。
- 未运行编译或GameTest；示例是实现设计，不声称新生产类型已存在。

## Remaining

- 按P0–P2建立数值和成本证据，再落实设计中的代码边界；切片只有明确测量依据时进入实现。
