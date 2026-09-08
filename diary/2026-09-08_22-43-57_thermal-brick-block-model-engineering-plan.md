# Brick 修复与方块级热模型工程方案

- Time: `2026-09-08 22:43:57 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering planning agent`
- Status: `completed`
- Scope: `细化已有精简计划；不修改 Java 实现`

## Completed

- 将[现有计划](../plans/2026-09-08_21-58-58_thermal-unknown-block-ventilation-75.md)更新为可实施工程方案：方块级属性、未知通风率 75、完整面连接、聚合、迁移、source 和查询接入、存档版本转换及验收。
- 明确普通空气聚合、有限通风块独立节点，避免丢失通风限制或将纯空气 Brick 无条件扩为 64 节点。
- 指定两数组布局、16 对共享面匹配和 64 方块迁移，删除已无消费者的内部微格结构。
- 补齐门开关的材料能量保留、邻区材料节点重新建立、可通风节点发布范围与旧 component 存档不兼容问题。

## Decisions

- 使用已有稀疏 Page、source ledger、主线程冻结属性和 worker 求解；不增加动态位置缓存或全世界扫描。
- 整块材料容量的系数转换是待校准的明确模型提议，不声称复现旧微格表面模型的全部温度曲线。
- 岩石地热的发现／激活／维持，以及基岩附近的 Page 中心背景精度问题，单列为后续功能边界；固定岩石温度参数不能绕过接收空气 Page 的需求。
- 按当前休眠数值载荷预算设计 v2，v1 自动保留均值；不以节点数量相同判断旧微格 component 可直接恢复。

## Validation

- 核对 `BrickTopologyCompiler`、`TopologyPlan`、`ThermalCellArena`、`WorkerPageStore`、`MinecraftPageManager`、`QueryPublication` 和 `DormantChunkThermalState` 的实际调用与布局。
- 文档检查；未运行 GameTest、JUnit、性能基准或堆分析。本轮没有 Java 变更，living docs 无需更新。

## Remaining

- 按计划实施，运行真实 Forge GameTest／生产路径，对照精简前后的物理结果、稳定求解和重建成本；不新增或运行 JUnit。
