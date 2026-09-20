# 连续空气 R4 最小复杂度与 CPU 工程计划

- Time: `2026-09-18 20:53:28 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed; 计划交付完成，数值原型与生产实现未开始`
- Scope: `连续空气方案细化、现有源码接入核对及后续实施准备`

## Completed

- 创建[工程计划](../plans/2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md)，把用户提供的R4方案收敛为P0–P6实施路径；明确P0–P2先证明空间表达、真实功率下的精度和成本。
- 核对现有空气slot查询、源accumulator直接交付、材料law纯函数及arena副作用、phase等待、拓扑commit、mailbox/pool、query双缓冲、quarter缓存和v4存档。
- 写明最少数据结构/owner、M/K/trace及独立body消元公式、预条件选择、缓存失效、source记录与pending账、两个提交点、模式迁移、查询/保存与回退时间选择。
- 补充数值参考收敛、等价优化、完整CPU/内存/sample age验收、实际代码入口和第一批任务；没有把附件自述测试当成当前仓库已通过证据。

## Decisions

- 先做模板/算子复用、紧凑接触、合格材料精确消元、热启动/块Jacobi和primitive scratch；维度内并行、热岛、AMG和通用框架不进入V1。
- 相对附件R4明确修改同步超额即失败的路线：同一cut保留trial与事件记录，在原有有界pool内续算，不重放源事件、不增加生产SHADOW服务；完整cut和save等待仍须达标。
- source功率保留实际tick；mixing系数沿用提交cut冻结/端点更新的明确语义；参考使用同一约定。
- Documentation impact: 本次没有改变已实现行为，因此未修改活文档；计划列出未来各阶段应更新的四份气候文档。README保持入口用途，design只读。

## Validation

- 对照当前HEAD `e1372280e`及相关源码/活文档；核对Java17、现有Gradle/GameTest入口和最近测试限制。
- 检查计划内部结构、代码围栏与仓库内Markdown链接；链接目标均存在。
- 本次为文档交付，没有运行编译、GameTest、R4数值测试或性能测试；计划中的参数预算和PASS条件是未来验收标准。
- 指定的两份 `.Codex/memory` 文件不存在，使用源码、活文档和现有diary核对，没有补造文件。

## Remaining

- 从P0采集真实配置、设备level/事件频率与成本基线，再完成P1/P2。
- 两模式/R6、目标硬件绝对预算、时间细分、局部预条件和codec容量尚待实验；未声称≤2°C或服务器成本已通过。
- 生产代码、配置、存档及伴随整合包均未修改。
