# 热模拟统一真实测试基线

- Status: `Current for recorded baseline and build scopes; retirement assertion and startup replay precision remain unqualified`
- Last verified: `2026-11-08`
- Scope: 后续状态收敛与空间表示改动的真实 Forge 行为、温度轨迹和运行代价比较入口
- Code anchors: `scripts/run-thermal-baseline.ps1`, `scripts/thermal-baseline.init.gradle`, `ThermalBaselineGameTests`, `RegionalRetirementObservation`, `ContinuousAirProductionTiming`, `ContinuousAirGameTestPacing`

## 运行

在仓库根目录使用 PowerShell 7 运行：

```powershell
./scripts/run-thermal-baseline.ps1 -Label current-baseline
```

脚本默认使用 `C:/Program Files/Java/jdk-17`；其它 Java17 路径通过 `-JavaDirectory` 指定。可用 `-Only curves,background` 或 `-Only 'curves,background'`选择场景，入口接受PowerShell数组和逗号字符串；`-Profile` 开启每个服务器进程的 JFR，`-Offline` 使用现有依赖缓存。`-ReferenceClimate <capabilities.dat>` 使用指定的真实气候记录，逐场景保存实际输入副本。`-WorldType minecraft:flat`可选固定平坦世界，实际预设写入配置和summary；默认继续复制原世界预设。已有本地 Forge 工具仓库适配可通过 `-GradleInit 'first.init.gradle,second.init.gradle'`传入一个或多个init脚本；入口不要求固定唯一工具版本。

每个标签保留自己的编译输出和服务器目录，直接编译当前源码，不复制源码 checkout。完成编译后，选定场景按顺序在独立 JVM 中运行。`-SkipCompile` 只用于继续使用同一标签下已经成功编译的输出；修改 Java 后应重新编译并使用新标签保留旧结果。

已有产物的性能对照可用`-PrecompiledDirectory <compiled目录>`共用其中的main/GameTest类和资源，并用`-MainClassesDirectory <main类目录>`选择已经编译好的生产组件；此模式跳过编译和资源处理，每个标签仍独立保留运行输出，不重编历史源码。`-NativeCpu`开启既有GameTest的Windows线程周期观察，结合`-Profile`记录任务活动耗时。周期保持cycles单位；任务活动耗时包含执行中的抢占/GC，不等于精确物理CPU秒。

原始日志、CSV、配置、世界和 JFR 留在 ignored 的 `build/thermal-baselines/<label>/`。`summary.json` 汇总本标签已有的独立场景结果；失败场景不阻止其余独立进程运行。通过数量、退出码、热保存/worker错误、最终root占用和重启阶段标记分别检查。

两份曲线用以下入口比较，整段轨迹超出容差、丢失行或采样身份不一致时会记录失败；后段诊断窗口不会覆盖整段结果：

```powershell
./scripts/compare-thermal-baseline.ps1 -ReferenceDirectory build/thermal-baselines/unified-replay-r5 -CandidateDirectory build/thermal-baselines/unified-replay-r6 -ToleranceC 0.1 -DiagnosticFromTick 300
```

## 共同配置与时钟

| 项目 | 本基线设置 |
|---|---|
| Java | 17 |
| 测试服务器最大堆 | 4GiB |
| JVM 可见处理器 | 5；当前 `ThermalWorkerPool` 因而使用4个worker |
| 世界种子 | `755111011717574879` |
| 世界生成/其它模组配置 | 从 `run-gametest` 复制；每个场景另存实际server.properties和common.toml |
| 热登记目标 | `thermalMemoryTargetMiB=128` MiB |
| 活动时间目标 | `continuousAirCpuBudgetMsPerSecond=1000` ms/墙钟秒 |
| 空气热容/混合 | `airHeatCapacityJPerBlockK=1200.0` J/(block·K)，`airMixingWPerBlockK=96.0` W/(block·K) |
| 休眠半衰期 | `dormantTemperatureHalfLifeSeconds=30.0` 游戏秒 |
| 其余热参数 | 沿用实际配置并存档，包括源辐射份额、材料定律和退出门槛 |

常规场景使用 `realtime20`。已完成但业务条件阻止的相变场景使用 `lockstep` 加速长时行为观察，其耗时不作为20TPS容量结论。曲线场景固定日照时刻和天气规则；成功退出后，入口从真实世界的 `data/capabilities.dat` 保留已经完成初始化的气候数据为 `climate.dat`，启动早期导出另留为 `climate-setup.dat`。后续可通过 `-ReferenceClimate` 和既有 `FH_PRECISION_CLIMATE_SNAPSHOT` 重放；不能用尚未生成小时缓存的早期快照声称气候完全相同。其它既有场景是否调用恢复入口取决于各自夹具，不能仅由同种子声称所有天气历史逐值相同。

## 场景

| 名称 | 真实动作或合同 | 记录 |
|---|---|---|
| curves | 四个真实房间：封闭、开门、多火源、实心隔墙；煤燃料、物品点火/熄火 | 空气/材料CSV、动作时序、辐射、样本tick和运行资源 |
| precision-room | 实际房间小温差持续冷却 | 既有温度曲线和冷却断言 |
| background / grid-switch | 不同自然背景、真实施工及粗细切换恢复 | 单次跳变、能量、保存采样差；按需摘要改动另检查同一Accepted的新旧投影 |
| coarse-cooling | 点火、实际降粗、熄火、退出与后续保存重载 | 既有断言及新增全区域余温/材料/等待诊断 |
| materials | 材料状态、相变、ACK、替换、保存 | 既有Forge材料合同场景；不冒充全部为玩家操作流程 |
| retention / retention-room | 部分记录删除、红外、露天降温和泥土潜热 | 原阈值与实际生命周期 |
| pending / reload | 未消费输入和实际区块卸载重载 | 保存与恢复责任 |
| concurrent-save | 一个Store合同和一个真实运行中保存场景 | 认领量/取消/保存的一次性语义 |
| normal-load | 两基地、256真实篝火和实际机器 | 温度消费者、更新时间及资源；不等同100联网玩家 |
| capacity | 实际千源几何压力与独立对照房间 | 降档服务、年龄、保存和资源压力 |
| blocked-phase | 已到冻结终点、世界变块条件阻止，改变实际邻接后恢复 | 明确刻画现存限制，不将现象复现计为生产修复 |
| river | 实际塔、短时篝火与无源河道对照 | 自然相变平台接力扩张回归 |
| restart-write/read/empty | 三个独立JVM使用同一磁盘世界 | 各自WRITE/DELETE/EMPTY标记及精确H/branch/tick |

`ThermalBaselineGameTests` 在elapsed tick 200点火、800打开door场景的实际墙洞、1400熄火、2600结束；每20tick采样。空气坐标为精确位置，`airC/naturalC` 单位°C、`radiantWPerM2` 单位W/m²；材料另存H（J）、branch、LIVE/STORED来源和sampleTick。缺失数值场与自然环境回退分别记录，不把回退当作有效发布。

后续比较按相同场景、位置、动作和物理采样时刻对齐。只有elapsed tick相同而sampleTick不同的读数，需要同时说明年龄差。温度误差、相变时间、构建/计算代价、分配和实现复杂度分别评价；当前版本自身的通过记录不提供未来近似方案的0.1°C误差资格。

## 2026-11-06基线结果

完整数据与构建归属见[可提交验证摘要](validation/thermal-unified-baseline-2026-11-06.json)。选定17组、29个required用例，按修正后的夹具结果计28通过、1个原无源退出断言失败；全部完成组热保存/worker错误0、关闭root0。曲线另做两次相同气候输入的独立重放，单次场景断言均通过，整段0.1°C重复性比较未通过。

后续按需摘要实现增加了`grid-switch`入口，定向12项结果见[2026-11-07记录](validation/thermal-minimum-summary-2026-11-07.json)。上述17组/29项是已记录基线范围，不是后续入口的固定总数。

| 范围 | 结果 | 关键证据 |
|---|---|---|
| 四房间曲线、精细冷却、背景粗细切换 | 各1通过 | 每次曲线3120行空气、1560行材料；精细冷却66次下降、0次停滞 |
| 材料、余温/红外、露天/泥土 | 8+4+2通过 | 保留原材料H/分支/时间及相变和删除要求 |
| 未消费J、真实卸载重载、并发保存 | 1+1+2通过 | 实际世界保存、冷却、重新点火；并发组另含一个Store合同 |
| 256源双基地与机器 | 1通过 | 12000查询；年龄/几何/消费者违规0；热主线程P95/P99=0.404/1.165ms |
| 1024源压力 | 1通过既有持续服务断言 | 77次推进、3保存、44529次准入拒绝；43tick不可读、最大年龄168tick |
| 相变业务等待、河道 | 各1通过对应检查 | 相变等待是现象刻画；河道远处无材料记录/接力扩张 |
| 三进程磁盘重启 | 1+1+1通过 | WRITE→DELETE→EMPTY真实阶段；精确H/branch/原tick读回且删除不复活 |
| 原无源粗档退出 | 1失败，未改断言 | 最后区域观测仍有4个活跃空气单元、最大扰动0.58808°C，退出门槛0.5°C；材料/潜热/等待均0 |

双基地热登记峰63593152B，worker累计分配1870466312B；完整server tick P95/P99=8.955/24.110ms，首个测量tick有1522.842ms尖峰。千源观测末登记169652720B，是软目标上的完成/保留代价，不是128MiB硬上限或精确JVM堆。Windows ThreadMXBean CPU计数仍留在原日志，未作为准确物理CPU或提速比例。

### 重放揭示的启动帧差异

R5/R6使用同一气候输入、同一位置与动作，全部3120条空气的sampleTick和available一致，naturalC、风和辐射列也一致。第280 tick有15个位置的温差超过0.1°C，最大3.3829712802443836°C，出现在sealed/door的`subcell_low`。第300 tick以后最大差0.0010882150138300517°C；材料最大温差约1.066e-6°C、H差约0.005756J，另有4条材料available/sampleTick不一致。

该后段窗口仅帮助定位问题。启动帧没有删除或放宽阈值，当前整段轨迹不能提供0.1°C比较资格。下一步应给启动期补齐已提交几何/区域身份、输入前缀和发布来源的对照，区分拓扑准备/发布时序与数值差异，再评估状态收敛改动。

### 夹具与构建范围

初版大房间的门洞未影响观测窗口，已用8×8×6房间替换，并要求开门实际改变温场；最终场景加热窗口的近点门洞差约0.01461°C，隔墙两侧最大差约4.19566°C。只看0.1°C绝对误差会漏掉这种较小的通风效果，后续还需比较效应本身。

旧精细冷却用例把房间建在默认模板Y=-59，点自然温度52°C且远点无发布。修正为高处受控小房间，并按当前普通退役合同区分有效发布与公开自然回退；原升温、余温和持续下降的数值要求保留。失败原件保存在R3；修正后R4通过。

R3/R4的3734个main class字节一致。R5/R6的3735个main class彼此一致，相对R3有工作区同期红外请求/渲染4类变化；`content/climate/thermal/`内核字节一致。分批结果未合称同一生产JAR的全量资格，新的客户端请求/渲染行为不在本轮验证范围。

R1因共享编译输出在运行中被改写而类加载失败，不能计为通过；入口现按标签隔离编译输出。R2在测试前因继承Java21导致构建初始化失败，现默认显式Java17。两次原始记录均保留；没有复制历史源码重编或引入路径敏感hash。

## 2026-11-07按需摘要性能补测

本节记录的试验实现已于2026-11-08撤回；历史原值和范围保持，当前帧准备行为见[运行合同](thermal-runtime-control.md)。

[测量摘要](validation/thermal-minimum-summary-performance-2026-11-07.json)记录一个既有`normal-load`负载的四次独立JVM，顺序为旧、新、新、旧。每次2400tick/120游戏秒，256真实篝火、256工作机器、100温度消费者、12000查询；使用该负载原来的普通世界预设。四次配置、server.properties和气候输入字节一致，堆4GiB、可见处理器5、worker4，JFR与原生线程周期观察同开。四次服务通过、热错误0、关闭root0；所有采样状态无降粗，JFR仅有细档推进接受事件，数量4299/4304/4299/4299。

共用`minimum-demand-r3/compiled`中的生产组件、资源和编译测试，只把本次8个类族替换为`unified-replay-r6/compiled`中已有旧组件形成before。两边各3741个main class，26个class文件字节不同且全部属于该8类族，其他生产类一致。没有重编历史源码，也未用完整产物间的城镇/红外同期差异计算收益。每次运行目录和JFR见摘要的`runs`。

| 指标（每版两次的中位中心值） | 旧 | 新 | 观测变化 |
|---|---:|---:|---:|
| 热worker累计分配（十进制GB） | 1.892 | 1.851 | 少2.14% |
| 热登记峰（MiB；不是精确JVM堆） | 60.56 | 59.93 | 少1.04% |
| 细档推进活动耗时（s） | 2.994 | 2.828 | 少5.54% |
| 全部区域时间片累计活动耗时（s） | 6.912 | 7.265 | 多5.10% |
| 热worker周期（十亿cycles） | 29.845 | 32.485 | 多8.85% |
| 热主线程tick P99（ms） | 1.664 | 1.641 | 少1.35% |
| 完整server处理tick P99（ms） | 42.152 | 57.338 | 多36.03% |

这组结果没有证明稳定的整体提速。只有两次/版本，中心值是描述统计；旧版worker周期18.528–41.161十亿、新版27.350–37.621十亿，细档推进活动耗时旧1.838–4.149s、新2.264–3.392s，重复波动大于中心值差异。预先选定的400tick后诊断窗，worker周期中心值也仅从21.420变为21.522十亿（多0.48%）。没有将首组较有利的结果单独作为交付结论，也不能将server P99的中心值退步直接归因为此次改动。

JFR任务活动时间排除排队等待，但仍受执行中抢占/GC影响；时间片汇总不覆盖所有协调与helper开销。Windows线程周期保持cycles单位，ThreadMXBean的CPU秒没有用于提速率。未控制CPU亲和性、调频、JIT与缓存因素；此处不增加100联网玩家、完整整合包、强制粗档或极端相变控制延迟资格。

首次`minimum-perf-before-1`使用平坦世界：空气未知数增长后出现降粗和服务违规，1用例失败，热错误0/root0。它不能与原普通世界负载混算，也未拿失败数据计算提速。主对照恢复原预设，预算、算法、负载和断言未调整。

## 2026-11-08摘要回退与重复边原型否决

[本轮记录](validation/thermal-graph-prototype-rejection-2026-11-08.json)撤回按需摘要的8文件/33行增量，保留材料自然背景及TRACE等7个无关hunk。普通细档恢复发布前生成摘要，首次材料控制同步补投影及其专用观测辅助退出；物理参数和保存格式保持。

重复边原型曾在原构建器内先哈希合并端点，再排序唯一键，复用端点输出数组完成地址重映射；生产净减16行、无新增顶层类型。固定生产网格夹具在真实Forge进程中比较，使用同一编译测试/资源，仅替换已有旧`CoupledThermalOperator`类族和`LongRadixSortCursor`。没有重编历史源码。两版各3项通过，独立有序参考的端点、边序和贡献地址映射一致；每个夹具预热48次、测12批各8次。

| 固定输入 | 每图分配：旧→候选 | 中位构建耗时：旧→候选 |
|---|---:|---:|
| 密集8192单元，98304次贡献/27200唯一边 | 2.185→1.834MB，少16.07% | 2.202→2.633ms，多19.57% |
| 不连通256单元，3072次贡献/3072唯一边 | 87.4→136.7kB，多56.34% | 0.0483→0.7219ms，约14.96倍 |

这是每版单进程的局部构建观察，耗时包含GC/JIT/调度影响；没有整体提速资格。候选虽少分配一部分密集图临时数据，却未证明耗时收益，低重复度还增加分配，因此按预设条件移除了两个原型文件的改动和专用基准源码。原排序、packed/paired路径继续使用；实验产物和补丁保留，避免重复投入该方案。

回退后的Java17编译通过。R4真实粗细切换1项通过、完成2次转换与2次checkpoint，最大能量误差7.45e-9J；R5真实双基地1项通过，256源、100温度消费者、12000查询，年龄/几何/消费者违规均0，未降粗；两项热错误0、关闭root0。R5复用保留编译组件，其thermal类与R4当前编译相同，另有10个同期玩家体温类不同，因此不为该玩家模型改动增加资格。

过程中的失败保留：R1的init参数被PowerShell合并，未编译；R3第一次编译成功但`Only`字符串合并导致没有选中场景，已修正为数组参数；其后R3生产类、R4第二进程的GameTest类目录缺失，启动失败，未计为通过，删除原因未定位。最终使用保留产物完成R5；没有把启动失败或空选择算作验证成功。
