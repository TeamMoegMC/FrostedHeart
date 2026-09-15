# 收敛统一热模型实施细节与六向换热合同

- Time: `2026-09-13 02:02:50 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `完善既有材料修复plan；未修改生产或测试代码`

## Completed

- 更新唯一[材料实施plan](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)，新增11.1–11.6：类与数据所有权、几何通路续算/失效、热源时间与能量边界、材料/ACK交接、v2→v3存档和消费者接线、实施及验收顺序。
- 收回热源多目标分配与闭腔多材料吸热扩展，保留既有端口数、powerShare、SourceBinding和ledger身份；通风位置只解析一个已有区域引用。
- 明确六向候选邻接、正轴去重和双向热流。楼梯的多个直接Air接触全部保留，仅无直接Air接触时使用一条间接区域近似边，避免误删正常方向连接。
- 查实当前TopologyPlan超限会丢弃staging而非续算；当前source accumulator直接加H后清pending，loss类别并非完整饱和收支；当前远端绑定不能继续使用源坐标的Page代次。计划据此写出必要改动，未宣称已接通。
- 保持本体单H、无逐气隙H/T、≤64节点/Brick、单solver及现有红外解析场/色标/单纹理合同。原历史计划和日记未重写。

## Decisions

- 单端口注入不等于单方向导热；分叉先注入一个区域的近似保留并明确热响应偏差，不引入额外功率分配系统。
- 拓扑内只补取消气隙状态所需的几何编译数据与有预算续算，不另建通用图引擎、全世界索引或热源寻路。
- 固定C、材料独立能量、相变边界、物体身份及持久化属于修复必要成本；性能是否满足预算由真实负载验证，不给未经测量的最优或FPS比例保证。
- Documentation impact: 当前仅计划和本日记改变，living docs继续描述实际代码；实施后同轮更新相关系统文档。

## Validation

- 对照WorkerPhysicalSourceBindings、ThermalSourceLedger、NodePowerAccumulatorArena、ThermalDimensionEngine、TopologyPlan、WorkerPageStore、MaterialEdgeCompiler、ThermalCellArena及DormantChunkThermalState核对接线假设。
- 检查计划内单目标/六向、直接与间接接触、未知边界、预算与生命周期合同的一致性；本轮文档相对链接和空白检查通过。
- git diff --check通过。计划编辑不要求编译；未重复运行GameTest或GPU测试。

## Remaining

- 下一轮按P0→P1→P2/P3→P4→P5→P6实施并验证。本日记完成状态只表示计划细化完成，不表示统一热模型已实现。
