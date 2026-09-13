# 材料显热、相变及生命周期统一修复方案

- Time: `2026-09-12 23:55:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `根据15项复查制定可实施计划；未修改生产/测试代码`

## Completed

- 新建[材料统一修复计划](../plans/2026-09-12_23-53-20_thermal-material-enthalpy-lifecycle-repair.md)，确定单arena、独立Air/M、单H分段相图和具体物体生命周期。
- 规定一个Brick最多64 Air+64材料节点；用Air映射与按块material mask/rank分址，避免继续拿一个long表达128个节点资格。full-Air仍一个节点。
- 明确统一能量参考、相态能量偏置、不同C/滞后阈值的相变边；普通边保留快速kernel，phase只在有限断点分段交换，不再用共享阈值池。
- 补充真实物体reset跨cut OR、复用代次/ACK以及ACK先于topology的固定交接顺序；资源受限时保留待law提交状态，不半应用或丢交接。
- 明确只读96面签名halo、固定C、稀疏材料前沿、独立材料NBT记录、inactive替换、reload重标定和材料温度计接入。
- 为任意玩法转化、含水嵌入相变、部分形状接触、未模拟区域及特殊热状态写明边界，避免以最小实现之名承诺全世界完整热力学。
- 将复查报告标为调查完成并链接执行方案；原红外计划相关材料条目转为新计划承接，其他扩展仍延期。

## Decisions

- 不引入第二solver、第二显示纹理、微格、逐玩家状态或Embeddium顶点改造。
- C固定于材料状态，暴露只影响交换；相变不删除显热。材料及几何/显示资格不再用同一分支替代。
- 单H内部交换守恒；既有解析场强制转化或Minecraft物质增减采用显式外部作用和同一状态交接，不假称闭合物理过程。
- 普通profile数值先作为固定每块有效C的初始游戏标定，明确变化与近似；具体性能结论必须由新节点/边数及实际负载测量。
- Documentation impact: 本轮只改plans和本日记；living docs仍描述现状，实施时同轮维护。

## Validation

- 对照现有Brick layout/arena、128节点所影响的mask/scratch、ResolvedGeometryBatch、exchange kernel正容量前提，以及engine ACK→topology顺序补齐实施细节。
- 检查plan链接、阶段与F01–F15映射及旧入口一致性。未编译或重复运行现有测试，方案尚未实施。

## Remaining

- 按P1–P6实施并验证：状态/布局、相变换热、生命周期、几何/持久化、消费者/画面、目标负载收敛。
