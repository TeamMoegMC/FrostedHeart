# 热模拟百人验证与迭代记录

- Time: `2026-11-19 01:37:47 +08:00`
- Author: `OpenAI Codex`
- Status: 记录归并完成
- Scope: 2026 年 11 月 18 日至 19 日的五条热模拟开发日记与六份百人服验证回执

各阶段的原始时间、决策、测试结果和验证边界保留在下文。完整指标和本机原始产物的路径按回执存放。见[百人服验证数据](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json)和[热模拟迭代摘要](../docs/climate/thermal-iteration-summary.md)。

- [常用路径候选与 CPU 对照](#thermal-cpu-hot-paths-single-pair)
- [有界热源截止](#thermal-source-cutoff-100p)
- [局部几何准备与混合覆盖](#thermal-brick-work-and-packed-mixing)
- [覆盖掩码实测](#thermal-packed-mixing-100p)
- [驻留时效回退与本地混合切点](#thermal-causal-mixing-and-freshness)

<a name="thermal-cpu-hot-paths-single-pair"></a>

## 百人热模拟候选的 CPU 对照与取舍

- Time: `2026-11-18 22:27:35 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; candidate recorded and reverted; per-tick load fixture retained`
- Scope: `ThermalNetworkLoadServer`, `MinecraftThermalInput`, `ClimateBlockUpdates`, `PlayerTemperatureUpdate`, `PlayerEquipmentHeating`, `SpatialHeatStore`, `ThermalDimensionEngine`, `TiledThermalField`
- Follows: [此前百人扩容压测](2026-11-15_18-46-29_thermal-load-validation-record.md)

### 改动与决定

先在 `eca9ec7c5` 及合并后的候选上各跑了一次真实百人服，按用户要求取消了其余重复轮次。候选一起调整了植物死亡检查、已加载区块的温度查询、玩家更新的能力读取和 Curios 槽遍历、输入流正负摘要的分配时机、几何预留时的片段遍历。两轮内存准入拒绝均为 0，保持现有扩容等待规则。

两个版本均从同一本机存档重新开始。100 个套接字客户端全程在线，25 个基地、1,600 堆营火、8 个侦察点，测 8,000 tick，再测 600 tick 恢复。热主线程墙钟平均耗时从 0.979 降至 0.810 ms/tick，P99 从 4.66 降至 3.45 ms。整服进程累计 CPU 从 267.84 升至 321.56 秒，服务器主线程和热工作线程的累计 CPU 也上升。两组都约 20 TPS。改动后的可读驻地样本中有 3,025 次超过 40 tick，对照组为 5,906 次。两组回家后均在第 120 tick 首次全部新鲜。两组的区块保存和协调次数有差异，单次对照只能判断这一组测量结果。

以 CPU 为本次取舍依据，生产代码还原到已测对照版本。已保存[可应用的原始候选补丁](../docs/climate/validation/thermal-hot-paths-candidate-2026-11-18.patch)。百人夹具保留 `tick-timings.csv`，测试结束后再写出每 tick 的阶段墙钟耗时和服务器主线程 CPU。两组的最慢 80 个热 tick 中，输入 tick 都占用最多的平均时间。

### 验证与文档

候选版本同一组 Forge GameTest 通过 53/53。已持有区块与从坐标获取区块的对照各查询 4,718,592 次，线程 CPU 均为 62.5 ms。最终生产源码与基线一致，`git apply --check` 验证补丁可应用，最终 `compileJava compileGameTestJava` 通过。源码与资料在本地工作区，无新提交。

[结构化结果和计量范围](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-cpu-hot-paths-100p-2026-11-18)保留两组 CPU、分配、慢 tick、JFR 样本、时效和恢复结果。[当前说明](../docs/climate/thermal-runtime-control.md)和[系统入口](../docs/climate/thermal-iteration-summary.md)记录最终取舍。长期负载与完整性能资格仍开放。

<a name="thermal-source-cutoff-100p"></a>

## 营火停热的时间边界与百人服 CPU 实测

- Time: `2026-11-18 23:02:13 +08:00`
- Author: `OpenAI Codex`
- Status: `completed; production changes applied and measured`
- Scope: `SpatialSourceTimeline`、`SpatialSourceIngress.limitNumericalThrough`、`ThermalDimensionEngine.fixedNumericalThrough`、`WorkerPageStore.hasResolvedResidentBrick`、玩家和植物热路径
- Follows: [第一组合并候选的实测](#thermal-cpu-hot-paths-single-pair)

### 改动与决定

沿用营火状态变化的事件时间和现有 Section 源索引。热源在关闭前按原功率结算到事件 tick。工作线程先确定本次推进终点，只对可能缩短步长的源查快照归属。仅当实际有输入冻结时才走物理出口解析。同一个空间域一次查询只访问每个源一次，到达本步起点即返回。`WorkerPageStore` 找到第一个已解析的常驻 Brick 就能回答 Section 是否可接收输入。

同一实测版还把玩家更新周期判断放到读取能力之前，遍历实际佩戴的 Curios 槽，提前跳过不会死亡的植物，并把天气查询移到消费结果的 tick。这组改动已保留在生产源码中。

### 验证

52 项 Forge GameTest 全部通过。截止专项用同一批 64 个真实源窗口，每组查询该区域 131,072 次，输入在推进终点，每次查询更换快照版本。两组都得到 tick 40。无区间上界时进入物理出口检查 8,388,608 次，有上界时 0 次。线程 CPU 分别计得 31.25 ms 和低于计时器分辨率。较早输入、未交接的熄火控制、冻结输入、未捕获 Section 以及已到步长下限均有断言。

实服复用本机基线 A1。当前版本 C1 在登录阶段出现编码器 `ConcurrentModificationException`，只到 99 人，未进入测量。随后 C2 的 100 个套接字客户端完成 8,000 tick 和 600 tick 的恢复，两端正常退出。依据 Git reflog 与 A1 编译产物中的栽培橡树类，基线工作区提交应为 `1e20c4d2c`。上一篇记为 `eca9ec7c5`，两者的热引擎源码相同。

| 8,000 tick 指标 | A1 基线 | 当前 C2 |
|---|---:|---:|
| 进程累计 CPU（秒） | 267.84 | 302.44 |
| 服务器主线程 / 热工作线程累计 CPU（秒） | 63.08 / 16.66 | 70.78 / 19.38 |
| 热主线程墙钟平均 / P95 / P99（ms/tick） | 0.979 / 2.03 / 4.66 | 0.831 / 1.75 / 3.25 |
| 热工作线程分配（MiB/s） | 11.21 | 10.49 |
| 34,600 个驻地探针中的缺失 / 可读但超过 40 tick | 4,162 / 5,906 | 2,324 / 2,282 |
| 恢复期首次全部新鲜（tick） | 120 | 120 |

实测窗口内，截止查询占热工作线程 JFR 执行采样的比例从 496/3,595 降为 10/2,621，含出口检查的该路径采样从 214 降到 0。该路径迭代器的采样分配权重从 252.47 降至 26.81 MiB。C2 的最大工作线程阶段是局部几何更新和安装（1,168/2,621 个样本）。两组的热协调轮次分别为 118,103 和 135,235。进程多用的 34.59 CPU 秒中，主线程实计多 7.70 秒，热工作线程多 2.72 秒，Netty 线程负载采样估计多 13.78 秒（含内核态 13.06 秒）。本次综合负载能定位增量所在的线程，每个改动对整服 CPU 的因果贡献仍待量化。

### 文档和后续

当前运行说明记录了事件时间、查询范围和快照判定顺序。验证记录保留两次运行的原始计数、计量口径和恢复数据。整服 CPU 降耗及长时负载资格仍开放。[运行合同](../docs/climate/thermal-runtime-control.md) · [当前结论](../docs/climate/thermal-iteration-summary.md) · [结构化数据](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-bounded-source-cutoff-100p-2026-11-18)

<a name="thermal-brick-work-and-packed-mixing"></a>

## 热混合覆盖、邻区阈值与百人验证

- Time: `2026-11-18 23:43:46 +08:00`
- Author: `OpenAI Codex`
- Status: `D1 completed; subsequent packed-mask runtime compiled, real validation pending`
- Scope: `AirMixingRegion`、`TiledThermalField`、`WorkerPageStore`、`TopologyView`、`ThermalDimensionEngine.FixedGeometry`

D1 用 1,728 种出热口相对位置的覆盖掩码计算 64 格并集。相邻 Brick 的驻留检查对单张面读一次 Tile，并在首个达到阈值的空气格结束。邻区几何准备时，在有序遍历中抵消新旧集合共有的位置。57 项 Forge GameTest 通过。对全部出热口偏移逐格对照后，262,144 次同一工作负载的线程 CPU 从 187.5 ms 降至低于当前计时器分辨率。6,144 个几何叠加参考样本的几何签名及归属差异均为 0。

D1 与上一轮 C2 同为 100 客户端，8,000 tick 测量和 600 tick 恢复。热工作线程累计 CPU 从 19.375 降至 16.516 秒，分配从 10.49 降至 9.80 MiB/s。34,600 次驻地观测中及时可读的样本从 29,994 增至 31,150。首次恢复全新鲜仍在 120 tick。进程累计 CPU 从 302.44 升至 304.28 秒，热主线程 P99 从 3.25 升至 3.26 ms。基于相同探针位置和物理采样时间的 21,761 对温度观测，绝对差 P95 为 0.491°C，一对达到 1.127°C，高于该点 1°C 的对照容差。实服历史异步，精度资格仍待独立验证。

后续工作区改为每个已分配 Brick 持有一个 64 位掩码，单个 Brick 的混合数据从 256 字节减至 8 字节。结构更新沿用旧 Brick 的掩码，首次捕获和重入时初始化，实际源启停按输入队列中的受影响 Brick 重算并集。独立求解器测试由其自身的出口夹具提供源集合。该版主代码与 GameTest 均已编译；结果归属以 D1 为界，后续模型选择及验证待续。[运行合同](../docs/climate/thermal-runtime-control.md) · [百人数据](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-brick-work-100p-2026-11-18)

<a name="thermal-packed-mixing-100p"></a>

## 热源覆盖掩码的玩法与百人服验证

- Time: `2026-11-19 00:16:03 +08:00`
- Author: `OpenAI Codex`
- Status: `completed`
- Scope: `AirMixingRegion.cellMask`、`TiledThermalField.replaceMixing`、`ThermalDimensionEngine.FixedGeometry`、100 连接下的 CPU 与温度时效
- Follows: [局部几何开销和候选实现](#thermal-brick-work-and-packed-mixing)

### 玩法

`gpt-6.1-terra` 以 `max` 推理强度运行分组 Forge GameTest 和一轮百人服候选 E1，使用既有 D1 基线。66 项必需测试全部通过。`packedMixingSurvivesBrickReindexAndOverlappingSourceRemoval` 覆盖最高位、Brick 扩容换位、部分重叠、关闭一个出口和关闭最后一个出口。三个相同热源历史场景逐格对照全部几何准备与普通源更新，共 5,888 个温度样本，最大温差和热量差均为 0。单独的标量与查表用时对照验证的是 D1 已包含的改动。

### 性能

2026 年 11 月 19 日 00:04:47 至 00:11:29（北京时间）完成 8,000 tick 测量，随后运行 600 tick 恢复。100 个客户端全程在线，测量期 0 次掉线，服务端与客户端均正常退出。

| 指标 | D1 | E1 |
|---|---:|---:|
| 混合覆盖数组数据 / 每个已分配 Brick | 256 B | 8 B |
| 几何安装中刷新覆盖 / 热工作线程 JFR 执行样本 | 59 / 2,150 | 0 / 2,197 |
| 整服累计 CPU（秒） | 304.281 | 350.094 |
| 服务器主线程 / 热工作线程累计 CPU（秒） | 70.641 / 16.516 | 87.625 / 18.734 |
| 热主线程 P99 / 整服 tick P99 墙钟（毫秒） | 3.26 / 71.7 | 3.00 / 80.9 |
| 34,600 个驻地观测中及时可读（最多 40 tick） | 31,150 | 31,458 |
| 所有热工作线程 CPU / 及时驻地观测（毫秒） | 0.530 | 0.596 |

几何提交由 2,752 次增至 2,821 次。按每次及时读数折算，热工作线程 CPU 增加 12.32%。进程多用的 45.81 秒 CPU 中，主线程直接计量增加 16.98 秒，热工作线程增加 2.22 秒，Netty 的线程负载采样估算增加 17.75 秒。两轮都在恢复期第 120 tick 首次全部新鲜，最后 1,000 次观测全部及时可读。

固定探针共有 22,453 对读数可按相同位置、工作负载 tick 和点火以来的物理采样时刻配对。绝对温差 P95 为 0.539°C。有两次观测超过 1°C 对照容差，最大 1.107°C，均在基地 5 的第 1 号探针，对应同一个物理采样时刻 5380。实服两次异步运行的轨迹仍有差异。

### 决定与文档

当前工作区保留每 Brick 一个 64 位整数的表示和事件驱动的局部刷新，圈内外规则及重叠热源关停已有受控验证。整服和热工作线程 CPU 在本次负载上均以 D1 较低。运行合同、热源说明和气候系统索引已更新。[完整数据与对照口径](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-packed-mixing-100p-2026-11-19) · [运行合同](../docs/climate/thermal-runtime-control.md) · [热源说明](../docs/climate/heat-production-and-network.md)

<a name="thermal-causal-mixing-and-freshness"></a>

## 驻地温度时效回退和本地混合切点修复

- Time: `2026-11-19 01:29:22 +08:00`
- Author: `OpenAI Codex`
- Status: `completed for the focused tests and one local 100-player run of each candidate`
- Scope: `TopologyInbox.PageQueue.enqueue`、`foldThroughRetirement`、`WorkerPhysicalSourceBindings.bindingLimit`、驻留维护与驻地观测
- Follows: [覆盖掩码实测](#thermal-packed-mixing-100p)

### 为什么少了 6,172 次

E1 与 F1 在第 5,200 tick 均有 22,400 次驻地观测，各基地的观测次数也相同。F1 额外产生 4,591 次过期和 1,581 次缺失。当帧 76 个可读固定探针的温度全部落后 43 tick，E1 的 75 个可读固定探针全部落后 3 tick。夹具对及时温度的要求是年龄不超过 40 tick。

第 3,600 到 5,200 tick，两版都有 6,800 次驻地观测，E1/F1 及时读数为 6,264/701，几何提交为 587/255。`ThermalWorkerPool` 按任务的墙钟运行时间计费。两版有效预算均为每秒 250 ms，JFR 记录的 `ThermalServiceWait` 延后决定分别有 1,204/4,848 次。它们记录决定时的请求延时。F1 早段快照集中在前 400 tick，中段记录不足以把全部落后归因于某一条通知。

F1 恢复阶段六次工作线程快照均显示，8 号基地的几何输入需要其边界同时到达世界 tick 16,724，其中一个活跃 Section 已在 16,740。另一边的退休又等前者推进。恢复末尾连续十帧都是这个基地的三个位置缺失、一个位置过期。该世界时刻晚于第 5,200 个测量 tick。全部原始读数和各阶段口径见 [F1 时效诊断](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-scoped-residency-100p-2026-11-19)。

### 改动与验证

F1 将全维度驻留扫描改为已发布 Tile 通知与常驻/活动 Brick 位遍历。G1 保留这个改动。当本地混合和较晚的 Page 几何在积压中合并时，旧实现会把后者也放到较早的时刻。G1 先处理本地系数，再在原有几何时刻让相邻区域参与。每个 Page 最多多保留一个早到的本地系数摘要。相同 Section 的热源位置、原始出口和实际终点复用一次截止读取。观测夹具将混合阶段有限的快照机会分布在全程，只在可采集时构造逐人诊断详情。

F1 有 74 项 Forge GameTest 通过，G1 有 80 项通过。G1 回归用例经过 100 次后续几何更新仍分别保留 16,724 和 16,740 的事件。用例另行核对了同区热源只读一次截止、跨区出口取较早截止。相同热源历史的 5,888 个温度样本在摄氏温度和焦耳账上差异均为零。

| 本机百人服 8,000 tick | 基线 E1 | 局部驻留 F1 | 修正版 G1 |
|---|---:|---:|---:|
| 及时 / 过期 / 缺失 | 31,458 / 1,383 / 1,759 | 13,659 / 14,345 / 6,596 | 32,404 / 767 / 1,429 |
| 热工作线程 CPU（秒） | 18.734 | 12.781 | 17.500 |
| 所有热工作线程 CPU / 及时观测（ms） | 0.596 | 0.936 | 0.540 |
| 进程 CPU（秒） | 350.094 | 402.953 | 319.516 |
| 热主线程 / 整服 tick P99 墙钟（ms） | 3.00 / 80.9 | 7.78 / 101.9 | 3.13 / 74.1 |
| 恢复期首次全员及时（tick） | 120 | 600 内未达到 | 120 |

`gpt-6.1-terra` 在 `max` 推理强度下负责本次 GameTest 和真实客户端负载。G1 复用 E1/F1 已落盘数据，补跑一轮 100 个网络客户端，服务端和客户端均以 0 退出。23,019 对同地点、同负载时刻和同物理时刻样本的温差 P95 为 0.459°C，最大为 0.792°C，全部在 E1 对照容差内。G1 最后十个恢复帧的 1,000 次驻地观测全部及时。第 3,600 至 5,200 tick 的预算延后决定降为 339 次，预算仍为 250 ms/s。[G1 百人验证](../docs/climate/validation/thermal-100p-validation-2026-11-18-to-19.json#/records/thermal-causal-cuts-100p-2026-11-19)

### 决定与边界

保留 G1。相对 E1，本轮多 946 次及时读数，每次及时读数分摊 CPU 降低 9.32%。混合阶段仍有 2,196 次观测没有达到 40 tick 目标。两项生产修改与测量夹具改进统一实测，各自的贡献仍待独立归因。完整 8h/24h 耐久资格仍待验证。

已更新[气候系统入口](../docs/climate/README.md)、[热模拟运行合同](../docs/climate/thermal-runtime-control.md)和[迭代摘要](../docs/climate/thermal-iteration-summary.md)，并分别保留两组验证数据。

## 本次归并的验证与文档影响

- 本次处理工作区新增记录。能量塔日记共四条，已提交日记引用的两个路径均保留。百人服回执按原文件名索引，B1 的候选补丁仍可单独取用。
- 核对了六份 JSON 的数据、原有日记内容、替换后的文档链接和 `git diff --check`。运行说明与迭代摘要已指向合并后的记录。
- 各阶段的测试结论沿用原回执。8h/24h 耐久验证和完整可达堆峰值测量仍待完成。
