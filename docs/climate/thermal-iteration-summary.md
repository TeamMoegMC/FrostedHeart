# 热模拟当前结论与记录入口

- Status: `Current for fixed cell enthalpy and v7; functional retirement checks recorded; 2026-11-19 source coverage and separated mixing cuts measured on the local 100-player server, full qualification open`
- Last verified: `2026-11-19`（本地热源控制与邻区几何顺序的分组验证及百人 CPU 对照）。架构整理于 `2026-11-14`。
- Scope: 本轮改造的最终状态和阅读入口；逐轮开发过程统一归档，不在这里重复。
- Code anchors: `MinecraftThermalInput.createWorker`、`ThermalDimensionEngine`、`TiledThermalField`、`SpatialSourceIngress.limitNumericalThrough`、`SpatialHeatStore`、`SpatialHeatBuffer`、`QueryPublication`、`AirFieldCheckpoint`。

## 当前结果

正式生产只保留方块单位存焓的固定温场。输入、材料/相变、Accepted查询、保存和故障恢复沿原链路完成；旧区域运行时、查询租约和无调用构建代码已退役。用户允许舍弃旧区块热数据后，热格式升为v7，不迁移旧v6热载荷。

最终main/GameTest编译通过；最后完整矩阵156/157，修正旧饱和余热归属断言后专项8/8，生产逻辑未变，共157项不同检查有通过记录。没有修正后的完整157/157。六次实际worker故障均有磁盘材料读回和新场供暖时旧场回收检查。

原回调引用泄漏有因果复刻与修复证据，但原事故没有heap dump；128MiB计账不等于实际可达堆硬上限。百人主线程、分配、40tick时效、完整内存/精度及8h/24h长期资格仍未取得。11 月 14 日的退役测试在 5800/8000 tick 停止。11 月 15 日的扩容修复已完成百人短测。

11 月 18 日生产代码保留了有界热源截止查询。对比同一百人基线，在 8,000 tick 内，截止查询占热工作线程执行采样的比例从 13.80% 降到 0.38%。热主线程 P99 从 4.66 降到 3.25 ms，34,600 个驻地观测中可读且不超过 40 tick 的样本从 24,532 增至 29,994。整服进程 CPU 从 267.84 升至 302.44 秒，热工作线程 CPU 从 16.66 升至 19.38 秒。该短测支持这条查询的局部优化，整服 CPU 降耗目标仍待完成。

在同一 100 人负载下，下一版（D1）的热工作线程 CPU 从 19.375 降至 16.516 秒，可用且不超过 40 tick 的驻地读数从 29,994 增至 31,150，进程 CPU 从 302.44 升至 304.28 秒。D1 测的是混合覆盖查表、驻留面阈值检查和邻区几何准备。

11 月 19 日的 E1 在每个已分配 Brick 存一个覆盖掩码，用于混合的数组数据从 256 减至 8 字节。已有 Brick 按热源变化更新。66 项分组测试通过。在同规格百人服中，几何安装刷新混合覆盖的执行采样从 D1 的 59/2,150 降至 E1 的 0/2,197。及时可读的驻地样本从 31,150 增至 31,458。热工作线程累计 CPU 从 16.52 增至 18.73 秒，每个及时样本的工作线程 CPU 从 0.530 增至 0.596 ms。整服 CPU 从 304.28 增至 350.09 秒。两次同采样时刻的温度观测超过 1°C 对照容差。

11 月 19 日的 F1 改为按实际提交的 Tile 通知驻留维护。64 个 Tile 中只推进一个时，64 次提交只触发了 64 次通知。百人服运行在第 5,200 tick 的 22,400 次驻地观测中，比 E1 多出 4,591 次过期和 1,581 次缺失。到第 8,000 tick，及时读数从 31,458 降为 13,659，每次及时读数分摊的工作线程 CPU 从 0.596 增至 0.936 ms。恢复期内 8 号基地仍有 3 个位置缺失、1 个过期。这个版本未通过时效验收。[F1 实测](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-scoped-residency-100p-2026-11-19)

当前工作区 G1 保留按 Tile 通知，在积压时分别处理早到的本地混合与晚到的邻区几何，热源在同一 Section 内复用截止时间。80 项分组测试通过。一轮同规格实服的及时读数达到 32,404/34,600，热工作线程 CPU 为 17.50 秒，每次及时读数分摊 0.540 ms，比 E1 低 9.32%。恢复阶段第 120 tick 首次全员及时，最后 1,000 次全部及时。23,019 对相同位置、工作负载 tick 和物理采样时刻的温度均在对照容差内，最大温差 0.792°C。混合阶段仍有 767 次过期和 1,429 次缺失。[G1 实测及精度口径](validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-causal-cuts-100p-2026-11-19)

## 只从这些文件进入

| 要查的内容 | 文件 |
|---|---|
| 同机 100 连接、8,000 tick 的 CPU、耗时、分配与时效对照 | [11 月 18 日至 19 日验证数据](validation/thermal-100p-validation-2026-11-18-to-19.json)，按 `records` 中的原文件名查找 B1、C2、D1、E1、F1 和 G1 |
| 11 月 18 日至 19 日的改动、测试结论和取舍 | [百人验证与迭代记录](../../diary/2026-11-19_01-37-47_thermal-100p-iteration-history.md) |
| 当前架构、热量/物理时间/查询/保存合同 | [运行说明](thermal-runtime-control.md) |
| 全部迭代的有效决策、撤回项、失败及最终口径 | [合并开发历史](../../diary/2026-11-14_23-52-57_thermal-cell-enthalpy-history.md) |
| 逐版本的原始指标、日志位置与限制 | [统一验证JSON](validation/thermal-cell-enthalpy-validation-2026-11-14.json)，按`records`中的原文件stem查找，共41条 |
| 待完成目标与下一批整体工作 | [唯一后续计划](../../plans/2026-11-10_21-24-44_thermal-bounded-spatial-architecture.md) |
| Q1/单格焓的推进、移动查询和玩法温差取舍 | [表示与成本对照](thermal-gameplay-representation-exploration.md) |
| 原OOM引用链、GC和堆归属 | [内存证据](thermal-server-memory-evidence.md) |

原服GC和泄漏因果回执早于本轮改造，仍见`validation/server-gc-22312-2026-11-09.json`与`validation/thermal-retention-causal-2026-11-10.json`。城镇/仓库问题见[城镇持久化与运行时](../town/persistence-and-runtime.md)，不混入热架构性能结论。
