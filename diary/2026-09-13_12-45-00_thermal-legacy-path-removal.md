# 删除已被材料本体模型替代的旧热路径

- Time: `2026-09-13 12:45:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `partial`
- Scope: `材料热模型实施中的旧路径清理；不进行独立可读性重构`

## Completed

- `MaterialBoundaryRegistry.Profile`只保留本体law和面换热系数，删除CAPACITIVE_SURFACE/PHASE_RESERVOIR模型分支及旧工厂。
- 删除`ThermalBrickCellLayout`/`ThermalCellArena`的共享相变池分配、候选池储能、固定阈值温度和旧ACK扣能量；`ThermalPhaseRequestStore`只有请求序列和状态。
- 删除`ThermalFragment.PhaseContacts`、`ThermalSolver.applyPhase`及独立相变操作预算。普通本体保持固定C快路径；带偏置/相变的接触使用同一H上的分段交换。
- `BrickTopologyCompiler`仅为实际Air分配空气容量，物质块各自一份本体H；删除按暴露面改变容量和mixed槽兼任材料温度的分支。保留两层材料接触范围和已有IR着色/解析场显示。
- `PhaseTransitionRuntime`直接按方块定位请求，复用fastutil位置索引；删除自制Brick/profile池索引、候选位和独立队列平行字段。提交前预留索引容量，提交时不自动缩容。
- 删除format 2读取兼容；新checkpoint只读写format 3。存档不会迁移旧热状态。
- 生产Java相对此清理轮开始净减少992行（包含新增请求状态类，不含文档和测试）。整个未提交工作区相对HEAD的主程序增减包含此前IR工作，不能归为本轮独立净增长。
- 相变辅助SoA由每槽46字节降至9字节，即少37字节/arena容量槽；这是原始数组载荷对比，不是整体内存或运行速度基准。

## Decisions

- 用户明确尚未投入生产，不保留被替代的旧模型和旧存档兼容逻辑。
- 第一轮曾用临时Python脚本修改Java，用户反对后立即停止，删除该脚本；第二个脚本未创建/执行。后续修改使用显式代码补丁，工程未引入Python运行依赖。
- 解析场的ADD_DELTA和明确下限仍是原有不同玩法语义。物理拥有的方块只由worker推进H；明确的玩法强制转化通过PhaseIntent记外部能量，再请求主线程转换。没有将IR显示值当作物理热量。
- 旧JUnit相变池测试验证的是ACK清零潜热，已删除；其重试、重复ACK、槽复用和拒绝不丢能量检查移入现有Forge GameTest，不保留测试专用旧模型实现，也未新增测试排除。
- Documentation impact: 更新[材料和热源说明](../docs/climate/heat-production-and-network.md)、runtime架构、持久化、红外材料来源及[主plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)；文档入口标明材料整合期间的Transitional状态。

## Validation

- Java 17；`gradlew.bat compileJava compileGameTestJava --offline --no-daemon --console=plain -Dnet.minecraftforge.gradle.check.certs=false`通过。
- 完整`runGameTestServer`从61/62修正为62/62通过。唯一失败原为解析场测试要求即时改块；更新为等待实际worker结算/ACK，保留纯delta、负向修正、显式下限及非物理拥有块的全部行为断言。
- 生产源码及GameTest源码已无PHASE_RESERVOIR、CAPACITIVE_SURFACE、PhaseContacts、旧phase能量访问器或applyPhase调用。
- 最终代码验证日志：`build/thermal-cleanup-compile.log`、`run-gametest/thermal-cleanup-final.log`。未修改shader，不重复既有GPU栅格验证。

## Remaining

- 完整热模型计划仍在实施，需继续完成长通路跨cut预算/失效边、存档与物体身份边界，以及不可配对玩法转换的显式外部事件覆盖；本条记录不是整个计划完成声明。
- 尚无完整新旧模型受控性能对比，不以行数或62个功能测试宣称最佳性能。
