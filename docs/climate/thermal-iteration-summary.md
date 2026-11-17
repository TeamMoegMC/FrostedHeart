# 热模拟当前结论与记录入口

- Status: `Current for fixed cell enthalpy and v7; functional retirement checks recorded; hundred-player performance qualification incomplete, optimization paused`
- Last verified: `2026-11-14`; documentation consolidated `2026-11-14 23:52:57 +08:00`
- Scope: 本轮改造的最终状态和阅读入口；逐轮开发过程统一归档，不在这里重复。
- Code anchors: `MinecraftThermalInput.createWorker`、`ThermalDimensionEngine`、`TiledThermalField`、`SpatialHeatStore`、`SpatialHeatBuffer`、`QueryPublication`、`AirFieldCheckpoint`。

## 当前结果

正式生产只保留方块单位存焓的固定温场。输入、材料/相变、Accepted查询、保存和故障恢复沿原链路完成；旧区域运行时、查询租约和无调用构建代码已退役。用户允许舍弃旧区块热数据后，热格式升为v7，不迁移旧v6热载荷。

最终main/GameTest编译通过；最后完整矩阵156/157，修正旧饱和余热归属断言后专项8/8，生产逻辑未变，共157项不同检查有通过记录。没有修正后的完整157/157。六次实际worker故障均有磁盘材料读回和新场供暖时旧场回收检查。

原回调引用泄漏有因果复刻与修复证据，但原事故没有heap dump；128MiB计账不等于实际可达堆硬上限。百人主线程、分配、40tick时效、完整内存/精度及8h/24h长期资格仍未取得。用户暂停时的最后百人运行在5800/8000tick停止，不能当完整负载通过。

## 只从这些文件进入

| 要查的内容 | 文件 |
|---|---|
| 当前架构、热量/物理时间/查询/保存合同 | [运行说明](thermal-runtime-control.md) |
| 全部迭代的有效决策、撤回项、失败及最终口径 | [合并开发历史](../../diary/2026-11-14_23-52-57_thermal-cell-enthalpy-history.md) |
| 逐版本的原始指标、日志位置与限制 | [统一验证JSON](validation/thermal-cell-enthalpy-validation-2026-11-14.json)，按`records`中的原文件stem查找，共41条 |
| 待完成目标与下一批整体工作 | [唯一后续计划](../../plans/2026-11-10_21-24-44_thermal-bounded-spatial-architecture.md) |
| Q1/单格焓的推进、移动查询和玩法温差取舍 | [表示与成本对照](thermal-gameplay-representation-exploration.md) |
| 原OOM引用链、GC和堆归属 | [内存证据](thermal-server-memory-evidence.md) |

原服GC和泄漏因果回执早于本轮改造，仍见`validation/server-gc-22312-2026-11-09.json`与`validation/thermal-retention-causal-2026-11-10.json`。城镇/仓库问题见[城镇持久化与运行时](../town/persistence-and-runtime.md)，不混入热架构性能结论。
