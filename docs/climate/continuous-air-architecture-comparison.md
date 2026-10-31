# 连续空气架构生产对照

- Status: `Historical recorded trials; two-level prototype removed from source on 2026-10-24; current production behavior linked below`
- Last verified: `2026-10-08`
- Scope: `historical global architecture comparison and subsequent regional/local unit-grid follow-on; actual capacity, consumers and persistence`
- Code anchors: `ThermalArchitectureTrial`, `TwoLevelThermalPreconditioner`, `SharedAirGridCompiler`, `AirOperatorFragment.unitGridVolumes`, `AirShapeStore.prepareSharedGrid`, `AirFieldCheckpoint`, `ContinuousAirProductionTiming`

2026-10-24：本文保留当时对照，不是当前可选实现清单。已删除未改善性能的`two_level/TwoLevelThermalPreconditioner`及相关workspace、准备和接受分支；保留正常FUNCTIONS和SHARED_GRID。当前控制见[运行时控制](thermal-runtime-control.md)，最新数据见[统计与存储优化](thermal-storage-optimization.md)。

当前性能主线为原物理输入位置的`pairwise`，已完成体积图、连续12槽边地址、无损单long排序键、查询读者共享计费及完整同源支持缓存/全region复用捷径。最新带JFR的标准256同轮基线P95/P99=145.994/285.013ms，最终117.193/309.450ms、root65.697MiB；实际worker CPU下降6.40%、累计分配下降7.27%，P99更慢，不宣传全面提速。累计生产5/5、读者/phase2/2、两JVM保存重启各1/1通过且准备拒绝0。约1°C相对原解析式目标仍未完成：此前九火初期11.748°C/后续2.875°C差异不因本包消失。普通默认未推广；`pairwise_plume`仍只作25/75输入对照。完整取证与边界见本文末节及[运行合同](continuous-air-runtime.md)。

2026-10-03实际接入并比较了两种候选。两层候选未改善密集/偏移的完整耗时；当时共享细网格减少日常求解工作，但完整多房间/余热回归因128MiB预算持续拒绝新布局。2026-10-07复用区域生命周期及性能优先的后续结果见末节；历史失败仍保留。最新目标是性能优先、约1°C观察差异次之，严格1.000°C结果仍单独记录。测试通过不等于体感等价、百人服务器或默认启用已经交付。

这里的baseline数据保留架构试验当时实现。后续原表示的大基地、分解/存储与查询压力改动见
[大基地优化记录](continuous-air-large-base-optimization.md)，其结果不混入本报告的既有对照。

## 实现与比较条件

同一工作区实现，通过 `FH_CONTINUOUS_AIR_TRIAL=baseline|two_level|fine_grid` 为每个独立服务器进程选择。默认仍baseline；`continuousAir`源码默认false，本地GameTest配置true。未切换CT2或正常runClient到新架构。

- 基线：共享四格粗函数+源接收面R6局部形状，P32/粗项和材料Jacobi，Air投影IC。
- 两层：保留相同空间表示、物理A/rhs及原停止估计；按实际局部重叠构建32模式core和有限粗/局部/材料halo，采用平衡粗/局部校正。每次预条件增加两次实际全局matvec。投影仍用原IC。
- 细网格：捕获的实际Air全部采用单格八节点三线性单元，开放面共享身份，墙/不连通分支分开；没有近细远粗或自适应回收接口。源不再生成专属温度模式。保留原物理热参数、混合区、四格浮力采样、材料law/phase、源W/J及cut时间线；节点质量集中与空间表示改变。节点/材料Jacobi用于传热，投影IC临时使用后释放。

全部使用真实Forge块、煤燃料、物品点燃/熄火、Minecraft输入/worker/公开查询/保存路径，20TPS时钟。未使用JUnit、独立矩阵夹具或此前排除的慢相变命名空间。JDK17，固定生成seed `755111011717574879`，20-tick cut、40-tick查询年龄、128MiB根预算、65,536 arena live-cell限制、有源/投影1e-5°C停止估计及无源/冲量1e-9°C上限均保留。

正常源阵列布局、燃料和参数一致，但独立运行的风/捕获/相变历史与最终驻留不同；表中是观测值，不给固定因果加速倍数。fine节点数量不等于arena live-cell数量。停止估计也不是严格的温度场误差证书。

## 完整cut结果

使用各方案共同完成的targetTick交集：密集1300–1980（35轮）、偏移660–1380（37轮）、分散600–2380（90轮）。密集两层直到1300才进入全部点当前的稳态监测，故采用共同窗口，同时另列整个运行的尾延迟。没有把更晚、更容易的监测窗口单独当作改善证据。

| 场景 | 方案 | 共同窗口P50/P95/P99（ms） | 全运行已完成cut P95/P99（ms） | 根已准入高水位（MiB） |
|---|---|---:|---:|---:|
| 密集128 | 基线 | 169.245 / 292.486 / 298.466 | 412.547 / 975.270 | 90.924 |
| 密集128 | 平衡两层 | 247.217 / 378.499 / 386.085 | 506.090 / 1000.263 | 95.943 |
| 密集128 | 共享细网格 | 68.005 / 277.286 / 310.263 | 277.286 / 303.109 | 87.228 |
| 偏移128 | 基线 | 428.742 / 485.692 / 513.560 | 590.058 / 1099.512 | 93.323 |
| 偏移128 | 平衡两层 | 672.192 / 709.034 / 729.839 | 739.042 / 1253.232 | 98.686 |
| 偏移128 | 共享细网格 | 69.525 / 278.482 / 378.818 | 293.122 / 378.818 | 92.186 |
| 分散128 | 基线 | 188.924 / 266.039 / 339.936 | 290.439 / 613.830 | 93.528 |
| 分散128 | 平衡两层 | 186.850 / 221.439 / 336.436 | 254.602 / 642.710 | 99.535 |
| 分散128 | 共享细网格 | 25.180 / 104.693 / 116.861 | 148.048 / 317.092 | 58.955 |

基线和最终平衡两层三类容量各通过1/1；最终细网格三类各通过1/1，另有内存优化前的独立通过记录。窗口内逐tick查询检查保持适用的新鲜度/几何条件，没有监测后持续WORK_LIMITED。节点共享不使温度变为每方块均温，实际同Brick细节与开放面连续回归通过。

三类首次读到全部点当前字段的观测tick，基线约97/110/110，两层97/114/110，最终细网格44/44/45（密集/偏移/分散）。局部模式和细节点的识别条件不同，不能只以基函数数量解释启动改善；真实温差/连续性测试另外保留。

根高水位是准入记账，不是整个JVM堆峰值；所有临时分配的精确预准入仍未完成。数据见 `build/thermal-architecture-comparison-20261003.json`；绘图及PDF来自真实日志：
`build/thermal-architecture-comparison-20261003.png` / `.pdf`。

## 行为差异

单火房间对应相同实际sampleTick的公开读数：

| 观测 | 基线近处（°C） | 两层近处（°C） | 细网格近处（°C） | 细网格与基线差值（°C） |
|---|---:|---:|---:|---:|
| 燃烧200tick | 13.067089 | 13.067086 | 13.731941 | +0.664852 |
| 燃烧600tick | 13.762923 | 13.762916 | 14.159345 | +0.396422 |
| 熄灭后40tick | 13.577295 | 13.577288 | 13.953953 | +0.376658 |
| 熄灭后600tick | 12.480897 | 12.480894 | 12.516402 | +0.035505 |

两层基本保留当前曲线，差值是实际独立运行/有限停止下的观测，不是任意场景误差上界。细网格升温更快且近火峰值/冷却不同，保留W/J和物理参数不代表热行为逐值一致。图中只画sampleTick≥0的真实字段，启动阶段回退到自然温度的读数不伪装成实际空气升温。

## 生产回归与失败

| 检查 | 基线 | 平衡两层 | 细网格 |
|---|---|---|---|
| 五项生产回归：热源/余热、细节连续、实际卸载重载、半砖出口、双源IR | 5/5 | 5/5 | 多次4/5，完整IR未通过 |
| IR单独调用同一真实世界回归 | 未另跑 | 未另跑 | 1/1 |
| 休眠/点燃唤醒 | 既有验证 | 未另跑 | 1/1 |
| 两个独立JVM保存/重启 | 既有验证 | 未另跑 | 首次/重启各1/1 |

首次加性两层偏移运行失败：部分cut约2秒，字段过期且不能进入稳定监测。改为平衡组合后通过，但额外matvec和局部应用抵消了迭代下降，密集/偏移更慢，未选为替代方案。

细网格第一次生产运行因尚有旧 `airBasisTerms>8` 判据而主动停止，记录保留但不计为架构行为失败。修改只针对表示检查，温差、连续性、真实热量、实际块更换/卸载与时效要求保留。随后完整生产运行反复在IR或材料替换阶段失败；单独IR通过不足以消除整体失败。

完整回归的多个房间与历史余热累计扩大fine字段。在最终生产r6，空气未知量约64,662，候选64,950+26,778材料；已安装状态仍可求解，但新旧layout/operator并存及准备余量无法通过根预算，累计89次WORK_LIMITED。一条实际拒绝为：
`Air retained backing 114630304 bytes refused; dimension used=71125976; limit=134217728`。
IR near/far编码缺失且材料NaN；另一轮两个chunk均loaded时仍无法得到replacement LIVE材质。原因是新布局/材料更新未获容量，而不是IR比较阈值。

两项保留的无损存储优化是：fine名义静态/风接触精确非零存储（不裁小非零，保留累加和组合顺序）；投影结束后释放日常传热/冲量不用的IC工作区。它们降低短窗高水位，但并未解决累积场景的完整容量，失败没有通过提高预算或放宽40tick隐藏。

fine JVM记录：保存near/far=-10.616847/-11.896068°C，重启读取=-10.617717/-11.896347°C；实际磁盘材料H/branch检查通过，重新点燃并打开实际通道后得到新区域的字段。此验证使用内存优化前同一fine表示/codec；最后两项内存优化不修改空间公式、保存格式或物理值。任意旧世界跨表示迁移、旧发布版reader和客户端CT2渲染没有认证。

## 结论与下一步

当前两层候选不值得替换基线；这不证明所有域分解方案无效，只否定本次具体实现的密集/偏移收益。共享单位网格证明了规整局部连接可以大幅降低日常求解工作，但“全部捕获空气一格网格”的累计容量仍未满足当前预算。

当前保持默认路径，两个候选仅显式试验选择。后续若推进网格，优先调查近区细化/远区粗化与能量/连续性约束，或在现有表示下完成增量算子和热驻留生命周期；不能继续仅减少迭代就声明解决。候选仍需长期/批量128源、复杂建筑、实际100人/多维度、完整预准入和客户端消费者证据。

## 2026-10-07：区域local运行与投影数据回收

复用`RegionalAirRuntime`、原Air捕获与真实source路径、已完成的读者生命周期及`LocalMaterialBoundary`，重新验证既有fine表示。runner新增`-ArchitectureTrial baseline|fine_grid`，默认baseline；不是恢复已拒绝的公共长尺度求和。当前source-default停止策略见[运行合同](continuous-air-runtime.md)，下表不沿用10月3日的1e-9°C设置。

发现fine投影成功后还持有整套聚合质量矩阵。`CoupledThermalOperator.releaseProjectionWorkspace`现释放projection端点、对角/非对角数组及IC；unit矩阵模板和已接受温度仍保留。fine仅有八普通节点单元，日常传热/冲量无解析local cross-mass项；下次构建显式禁止复用已释放的projection，重新组装。原解析路径保留其质量矩阵。新增`releasedProjectionBackingBytes`记录实际释放的唯一数组字节；不改变fine的M/K、源W/J、double、停止条件或checkpoint格式。

同一实际单基地128源、2400tick、标准128MiB、realtime20：

| 方案 | worker P50/P95/P99 ms | 根预算已准入峰值MiB | 与原表示最大匹配观测差°C |
|---|---:|---:|---:|
| 当前原解析表示控制 | 285.593 /477.440 /752.365 | 103.882 | — |
| regional/local fine，回收前 | 332.518 /514.398 /524.450 | 82.586 | 0.552210，160个匹配观察 |
| regional/local fine，回收后 | 328.035 /488.141 /517.374 | 71.155 | 0.552210，192个匹配观察 |

两个fine运行之间160个同sampleTick/natural/位置的观察差值为0°C；源账与原表示也一致：114176000J输出=90521600J交付+22835200J声明损失+819200J降级损失，pending/unaccepted/余额0。回收改动的有限观察未改变fine行为，不能因此把fine与原表示的差异说成零。独立异步历史和有限采样仍限制比较；单次P95变化不提供固定加速比例或全场误差上界。

回收后同维度两处128实际篝火、both_bases完整2400tick，原root/Air128MiB、query16MiB标准档通过1/1：256源启用、16 LIVE楼板、12000次环境调用，age/geometry/receiver/WORK_LIMITED/worker异常0。worker P50/P95/P99=414.340/546.532/588.391ms，topology/solve/publish P95=431.054/111.683/27.789ms，根预算峰值97.320MiB；172次区域迁移父时间均对齐，源余额0，输出/交付及loss与原256场景相同。实际2400tick耗时122788ms、barrierWait0；这不是lockstep吞吐结果。完整cut计时仍不含输入排队，不能直接作每区域端到端500/800ms通过。

五项真实累积房间生产5/5、读者/粉雪两项2/2通过。但前者仍有3次候选准备预算拒绝，之后恢复，根高水位125.169MiB；不能写成零拒绝或任意历史空间容量已通过。三代读者关闭计费1702904字节，最后释放为0。粉雪placedTick784、convertedTick7641，供热6857tick，原表示对应场景6877tick；这是单一相变行为观察，物理配方未改。

97.320MiB是既有预算账目，包含旧新保留数据及现有准备余量，不是JVM总堆或全部构建暂存的精确硬上限。暂存预留仍采用`ThermalDimensionEngine.airPreparationScratchBytes`估计，R7完整预准入尚未交付。一次整服`jcmd GC.heap_info`观察heap used2018109KiB也不能作为thermal独占值或峰值。

**帕累托判断：这是已测方案中的取舍候选，未证明全局最优。** fine的内存与P99优于原表示，P50较慢，温场也不同；没有全面支配原表示。若把约0.01°C作为可接受观察目标，fine未进入满足该约束的可行集合。源重叠去重的收益已得到容量证据，但全部捕获空气一格网格仍随空间体积/历史驻留增长。近细远粗是可比较的后续假设，需要粗细交界、守恒投影和实际曲线验证，不能预先声称它支配统一网格。

保留fine显式试验，不改变源码默认或CT2选择，不推广默认；百人真实客户端、长周期、混合时间协议和游戏阈值/体温趋势等仍未认证。数据：[完整场景](../../build/continuous-air-regional-fine-results-20261007.json)、[最终原表示对照](../../build/continuous-air-regional-fine-final-128-20261007.json)、[回收前后对照](../../build/continuous-air-regional-fine-reclamation-128-20261007.json)。

## 2026-10-07：帕累托候选的实际探针

`FH_CONTINUOUS_AIR_GRID_OPERATOR_TRIAL=projection_cells`为fine新增显式A0投影候选，runner对应`-GridOperatorTrial projection_cells`，默认仍assembled。保留原unit矩阵值/RHS、全部节点、double和停止条件，直接逐单元应用质量矩阵；八节点gather/scatter避免反复地址访问，不生成聚合projection pair哈希表、端点数组或IC。投影使用对角预条件PCG；数学方程保留，累加次序和迭代路径改变。该探针没有实施完整的固定块增量构建。

| 实际realtime20完整2400tick场景 | worker P50/P95/P99 ms | 重建P95 ms | 根预算峰值MiB |
|---|---:|---:|---:|
| 已有fine128 assembled控制 | 328.035 /488.141 /517.374 | 388.224 | 71.155 |
| A0 fine128 unit投影 | 225.534 /283.840 /385.288 | 200.765 | 71.205 |
| 已有fine256 assembled控制 | 414.340 /546.532 /588.391 | 431.054 | 97.320 |
| A0 fine256 unit投影 | 238.980 /327.714 /386.820 | 249.796 | 97.209 |

两个A0场景均通过，worker异常/预算拒绝0，源余额0。256源最终启用、16 LIVE楼板、12000消费者调用，age/geometry/receiver/WORK_LIMITED0；178次迁移父时间全对齐。时间仍不包括输入排队，独立异步运行不提供固定因果加速比例。预算峰值近似相同，因为之前已在投影后释放聚合矩阵；此次主要省掉构建时的临时分配和组装，不应宣传持久内存大幅下降。

大区域构建中位`coupledGraphUs`由207245.5降至87702，内部projection assembly由99365.5降至340us；A0投影中位27迭代，完整耗时仍下降。与已有fine128控制192个匹配观察最大差0.0000027323°C；新的原解析128控制P95/P99=415.207/608.156ms、root103.884MiB，与A0匹配192点最大差0.555067°C。A0没有解决fine对原表示的模型差异，不把同fine观察等价扩展为原体感完全等价。

A0实际生产5/5、读者/phase2/2通过，但累积房间仍有与控制相同的3次候选拒绝，root125.169MiB；关闭读者1702904字节→0、粉雪6857供热tick转换与fine控制一致。保存格式未改，本轮没有新增独立JVM保存/重启或12000tick耐久认证。

`ReadCursor.observeGridCoarsening`通过真实发布快照读取统计，不暴露温度数组或更改权威状态；GameTest显式`-ObserveGridCoarsening`在800/1600/2400tick观察。只允许完整且当前的4×4×4 Air cube试算4/2格插值，其他组件保留fine。Q1顶点差可界定该瞬间格内误差，不能保证未来曲线。原始所选顶点并非已实现的粗细交界、守恒投影或实际内存。

| 相对fine的即时最大误差线 | 三个实际256快照的原始顶点减少范围 |
|---|---:|
| 0.01°C | 0% |
| 0.05°C | 3.45%–6.79% |
| 0.10°C | 11.39%–16.37% |
| 0.25°C | 17.20%–19.31% |

2400tick有893个Air组件，仅201个完整cube；本次简单完整块粗化在严格偏差目标下无收益。不否定各向异性、复杂几何分级等其他表示，但不能凭本次统计宣传混合网格大幅省内存。暂降低其实施优先级。

`ReadCursor.observeAirOperatorStorage`在新的真实原表示128场景量当前basis宽度。2400tick三份三角volume数值载荷5881152B，若显式储存每Air单元每函数的8个double modal变换则27320832B，约4.65倍；宽度>=32部分5275872B→15613952B，约2.96倍。主要区域width P50/P95/max=8/62/96。它是当前接受空间的原始载荷估计，非root峰值；不包含headers、共享、接触、图和预条件器。简单储存完整变换矩阵作为C容量优化应排除；按需复用shape的原空间作用仍未实装，可能增加每轮计算，未产生可比较性能点。

当前优先保留A0，以固定空间片段的稳定身份/复用继续减少transport图和新旧候选并存；不以本轮局部探针宣称完整A或帕累托最优已经交付。B0/C只完成有范围的现场调查。依据：[完整候选报告](../../build/continuous-air-pareto-candidates-20261007.json)、[同fine对照](../../build/continuous-air-pareto-projection-128-20261007.json)、[新原表示对照](../../build/continuous-air-pareto-original-vs-cells-128-20261007.json)。

## 性能优先：节点控制体积

用户随后明确“我没有严格复刻的需求，我只想要性能”。本节按新目标选型，之前的原解析/fine约0.01°C匹配门槛只保留为当时目标和描述数据，不再否决更便宜的离散模型。功能、热输入、余热、实际阻隔、材料/phase与保存仍验证。

`ThermalArchitectureTrial.GRID_CONTROL_VOLUME`通过显式`FH_CONTINUOUS_AIR_GRID_OPERATOR_TRIAL=control_volume`选择。复用实际捕获、Q1节点身份/位置查询和version5 checkpoint，避免新建整个单元中心状态/读者接口。每unit cube将导热改为12条轴向边，内部节点通常6邻居；每边G=conductance/4 W/K，节点热容为airCapacity/8 J/K。水平两方向及原四格浮力采样保持；矩阵是对称守恒边作用。材料/外界四个面采样改为节点换热，源W/J、20tick时间线和材料law参数不改。

拓扑准备用`ContinuousAirSolver.addNodalProjectionRhs`按每个实际Air cell的旧节点侧温度加权转移，重新共享的节点按热容平均；不做全局一致质量投影。未变化的fine场不因每次重建被平滑。保存的边界角点用`Math.nextAfter`取单元内侧，保持正确分支。这是新的离散热响应，不声称原曲线严格等价。

| 完整2400tick realtime20场景 | worker P50/P95/P99 ms | 根预算峰值MiB |
|---|---:|---:|
| 快速fine128，A0控制 | 225.534 /283.840 /385.288 | 71.205 |
| 初版控制体积128，Jacobi | 182.997 /334.024 /380.514 | 63.146 |
| 控制体积128，一次现有IC修正 | 130.493 /186.599 /248.310 | 71.552 |
| 快速fine256，A0控制 | 238.980 /327.714 /386.820 | 97.209 |
| 控制体积256，现有IC | 147.917 /198.349 /337.194 | 96.454 |

初版虽然省内存，但求解P95=142.337ms且整体P95更慢，负面结果保留。一次定向修正启用已有稀疏air IC(fill1)，保持物理A/RHS与停止策略；128求解P95降到58.542ms。IC backing计费，不以初版63MiB当最终容量。

最终标准root/Air128MiB、query16MiB的256场景通过：256enabled、16 LIVE楼板、12000环境调用，age/geometry/receiver/WORK_LIMITED/worker异常0；173次迁移父时间对齐，源输出187392000J=148275200J+37478400J+1638400J，pending/unaccepted/余额0。topology/solve/publish P95=136.779/55.779/17.678ms。worker计时不含排队，预算峰值不是所有暂存精确硬上限或JVM堆，100对象不是100真实客户端。

真实生产5/5、读者/phase2/2、两个独立JVM同世界保存/重进各1/1；worker异常0。累积房间仍2次候选拒绝后恢复（快速fine控制3次），root121.978MiB；不记录为零拒绝。关闭读者1702904字节→0。粉雪供热7297tick完成，快速fine6857tick，变化约+6.42%；配方/潜热未改。192个匹配fine观察最大温差0.268239°C仅作行为记录。restart near/far=-10.882212/-11.835221°C，重载=-10.882960/-11.835528°C，磁盘H/branch及重新点燃/通道新场通过。

性能优先下保留此候选作为后续主线，普通默认尚未推广。后续集中消除未变空间片段的重复组装及新旧并存峰值；R3广泛源事件/混合时间、精确暂存、完整端到端/长周期和真实客户端仍待交付，原温场严格匹配已不是阻塞。数据：[完整性能结果](../../build/continuous-air-performance-control-volume-results-20261007.json)、[初版](../../build/continuous-air-performance-control-volume-128-20261007.json)、[IC修正](../../build/continuous-air-performance-control-volume-ic-128-20261007.json)。


## 2026-10-07：成对推进、扩大温场采样与局部源分布

用户先增加约1°C温差目标，随后明确性能优先、精度第二。新增显式`pairwise`与唯一局部修正`pairwise_plume`；选择和公式见[运行合同](continuous-air-runtime.md)。普通默认仍baseline/global/coupled、continuousAir=false。

原表示为四格粗函数加源相关单格三线性形状，fine为共享单格节点；两者格内都连续三线性，未知量约束和质量/传热离散不同。扩大到每个fire上方0.125/0.875格，以及房间距离、高度、格内角点，每40tick读实际发布温场，发现以前192个近远点漏掉了低层热点。不是扩大停止阈值或提高浮点精度即可修复：已有fine FEM同样超限。

下表全部是本轮同一实际128-source双层基地2400tick/realtime20，含开关通道、半数及全体熄火/再点燃、实际coal补给。表中的最大温差相对本轮原解析表示，约23,342个同elapsed/source事件、坐标、sampleTick、naturalC匹配观察；独立世界输入历史并非逐条相同。

| 方案 | worker P50/P95/P99 ms | 推进P95 ms | 预算记录峰值MiB | 最大观察温差°C |
|---|---:|---:|---:|---:|
| 原解析表示 | 175.105 /331.185 /519.659 |55.861|103.884|—|
| 节点CV+IC |131.975 /193.837 /248.194|57.452|71.329|1.916153|
| 已有fine FEM/unit projection |196.043 /256.477 /307.380|68.070|71.108|2.501941|
| 对称成对推进 |112.913 /186.877 /237.464|39.221|62.919|2.299943|
| 成对推进+局部源分布 |95.830 /152.618 /221.492|24.702|62.819|1.080789|

成对推进相对CV还有约0.507°C差异，不能把双方接近当成对旧温场接近。最坏同一点/同1480tick，原/CV/pairwise分别21.062938/22.934929/23.362880°C；源总账一致。该点低/高位置差原表示0.549593°C、pairwise1.891235°C。原受限函数空间的近源响应更平缓，新自由节点配合集中face输入形成更陡热点；这属于离散模型和时间响应差异，不是篝火方向绑错或源J丢失。实际campfire入口检查为offset(0,1,0)、NEGATIVE_Y。

`AirLoadTable.compileLoad`对plume保留25%物理face输入，75%在接收Air及其可用六邻接Air中心分配，按sum(weights)=1保存总W/J。仅绑定/重绑定编译短stencil；查询和日常步进没有source扫描。它是有效输入空间分布近似，改变近火温度和材料受热；没有给HUD减一个常数，也不通过统一降低热源功率修曲线。

同维度两处各128源完整2400tick、both_bases：

| 方案 | 容量档 | worker P50/P95/P99 ms | topology/solve/publish P95 ms | 预算记录峰值MiB |
|---|---|---:|---:|---:|
| 原解析温场参照 |历史two_bases扩大档，仅作参照|202.596 /390.498 /630.497|289.705 /55.434 /46.811|166.312|
| 最终plume候选 |standard：root/Air128MiB、query16MiB|104.050 /153.181 /305.398|123.855 /24.200 /29.010|85.708|

新候选256enabled、16 LIVE楼板、12000真实环境调用，age/geometry/receiver/WORK_LIMITED/worker异常及准备拒绝全0；173次父区域迁移全部时间对齐。实际816位置45,802匹配观察最大差1.080822°C，全熄火阶段0.971192°C。原参照的扩大预算不能宣传成标准容量成功；新候选实际使用原标准预算。worker耗时不含输入排队，预算峰值不是整个JVM堆或所有暂存的硬上限，不把独立运行比值当固定因果加速。

完整生产5/5，但累计房间仍1次候选准备预算拒绝后恢复，root121.933MiB；上一CV控制为2次/121.978MiB。此处不能写成全部空间零拒绝。读者/phase2/2，三代读者关闭计费1702904B，最后释放后0。粉雪实际supply7797tick转换，上一CV为7297、原参考6877；分别慢6.85%和13.38%。law/latent heat不改仍会因温场与交换曲线变化改变相变时间；这是可感知取舍，不能称热行为完全相同。lockstep仅压缩生命周期等待，不用于实时吞吐。

独立JVM同世界`thermal-pairwise-plume-restart-20261007`保存/重进各1/1。保存near/far=-11.147735/-11.879300°C，恢复=-11.148360/-11.879586°C；真实磁盘H/branch、再次点燃和实际新通道检查通过，字段仍version5。全部源账实测余额0。128输出93696000J=74137600交付+18739200声明损失+819200降级；256恰为两倍，pending/unaccepted0。

**当前结论：** 性能优先下保留plume显式候选，最大观察约1.081°C；严格<=1.000°C布尔检查依然失败，不静默放宽报告阈值。不继续第三种权重/推进器配方。完整固定块/旧新共享、所有暂存预准入、R3广泛时间/源事件、端到端/长期/实际100客户端和CT2渲染仍待验收，普通默认没有推广。并未证明全局帕累托最优。

数据：[完整报告](../../build/continuous-air-performance-pairwise-plume-results-20261007.json)、[128温场](../../build/continuous-air-pairwise-plume-vs-original-128-20261007.json)、[256温场](../../build/continuous-air-pairwise-plume-vs-original-256-20261007.json)。


## 2026-10-07：相变慢13%的隔离对照

补一次实际`pairwise`生命周期运行，只关闭25/75源分布，保留CV节点、局部推进、材料law和所有源参数。九个真实煤篝火、实际粉雪、相同39000J initialH：

| 路径 | 实际供热tick | 20TPS游戏时间 | 相对原参考 |
|---|---:|---:|---:|
| 原解析表示/local材料 |6877|343.85s|—|
| CV/implicit推进 |7297|364.85s|+6.11%|
| CV/pairwise，无plume |7017|350.85s|+2.04%|
| CV/pairwise，25/75 plume |7797|389.85s|+13.38%|

原→CV增加420tick，CV→pairwise减少280tick，pairwise→plume增加780tick，共多920tick。额外延迟主要落在输入分布选择上，省去PCG的推进本身在本场景稍快；不能只从总13%推断求解器精度更差。各运行是独立生产历史，原/CV为此前记录，单次对照不提供所有场景固定比例。

现有粉雪recipe cap3900J/K、phase9°C，`MinecraftMaterialLawCompiler.compileEdge`对无材料目标采用`latent_heat_j`默认38000J，H端点35100→73100J。热容/阈值/latent保持，净供热因邻近温场和传热路径变化而不同。helper elapsed5300：原/pairwise/plume邻近Air29.437/29.332/27.279°C，phase progress71.93%/70.33%/61.76%；这是持续积热更慢的记录，不是worker花更多CPU时间。

**范围补充：** 本九火粉雪场景的共同LIVE观察，plume与原邻近Air最大差2.557964°C（elapsed800）。前文1.081°C是128/256双层基地的位置集合结果，不能作为所有场景上界。新推进可选作性能主线，25/75源扩散作为独立校准项保留限制，尚未证明跨场景温度或相变全面优于CV。

本补测2/2通过，worker异常/预算拒绝0、源余额0、读者释放1702904B→0，没有修改运行参数/配方/源码。数据：[隔离归因](../../build/continuous-air-phase-delay-attribution-20261007.json)、[新实际运行](../../build/continuous-air-performance-pairwise-phase-attribution-20261007.json)。


## 2026-10-07：原输入位置的成对主线，补齐standard256

固定25/75源分配不纳入最终推荐的统一规则；保留原物理输入和pairwise作为性能基础。此前plume256通过不能替代纯方案容量证据，现补完整standard128MiB、同维度two128、both_bases/2400tick/realtime20，实际煤、几何/通道/启停与查询1/1通过。

纯方案worker P50/P95/P99=136.131/234.732/397.065ms；topology/solve/publish P95=192.838/30.473/35.061ms；root85.909MiB。256enabled、16 LIVE楼板、12000环境调用、age/geometry/receiver/WORK_LIMITED/准备拒绝/worker异常0、172迁移时间对齐、源余额0。体积/内存和日常推进可作为基础，但本次完整P95仍高于此前CV198.349ms，不能宣传所有指标更优或固定因果加速。其余主要开销在构建。

816位置45,802匹配观察最大2.299909°C，约1°C温场目标仍未完成。下一有界调查是原房间平移分区敏感性、源/储热/面换热及时间离散，原输入/总W/J/law不调；未实施或声称近源精度修复。建议phase约5%作为玩法验收线属于工程提议，不是用户已确认硬要求。默认、R3/R7、长期及真实客户端仍未推广/认证。

数据：[纯方案容量](../../build/continuous-air-performance-pairwise-final-base-256-20261007.json)、[纯方案温场](../../build/continuous-air-pairwise-final-vs-original-256-20261007.json)、[收敛计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## 2026-10-07：成对体积图提速与有界近源调查完成

`CoupledThermalOperator`的pairwise分支现在只为Air体积建立传热边。材料/外界/路由接触继续按原局部rank-one路径推进，不再建立未参与推进的全局接触矩阵边。`compilePairwiseGraph`用primitive pair key和体积条目位置排序、去重并同时填写`volumeEdgePositions`；保留体积累加顺序和非零正反扫顺序。接触支持变化只更新实际接触数据，不再单独使体积图失效。没有更换参数、温度精度、源输入、H/latent或存档格式。

| 同一plain pairwise负载 | 优化前P50/P95/P99 ms | 本次P50/P95/P99 ms | 原/新已计费峰值MiB |
|---|---:|---:|---:|
| 128真实源、2400tick、realtime20 | 112.913 / 186.877 / 237.464 | 100.976 / 164.404 / 216.837 | 62.919 / 62.030 |
| 同维度两处128源、完整both_bases | 136.131 / 234.732 / 397.065 | 104.815 / 174.801 / 325.233 | 85.909 / 84.742 |

本次256的topology/solve/publish P95为143.806/18.720/27.317ms，256enabled、16 LIVE楼板、12000调用，age/geometry/receiver/WORK_LIMITED/准备拒绝/worker异常0，173迁移父时间对齐、源余额0。与优化前同plain方案的匹配温差：128为23,632次/408点、最大7.11e-15°C；256为46,362次/816点、最大0.003229°C。不同256独立世界的构建/迁移次数不同，不能写成bitwise输入回放相等。这里只报告完整worker墙钟，不升级成全服CPU或100真实玩家认证。

128中Air系数>=10000的65个构建，`coupledGraphUs`中位数25.667→18.633ms；原独立`volumePositionUs`中位数7.140ms现不再执行该查表阶段，位置编译并入排序。因`contact_support`而失效的大构建15次→0；这些接触仍正常重编译并执行换热。保留数组仍被meter计费，排序暂存等全部临时对象的逐项预准入未在本包完成。[构建归因](../../build/continuous-air-pairwise-volumegraph-build-attribution-20261007.json)。

有界近源调查使用实际四个九火房间、相隔65格/相对四格分区平移0/1/2/3、156点、20tick采样、1000tick、600tick熄灭。首次四房间生物群系温度不同，没有可用四房间匹配，结果只保留作有限同位置对照；随后用实际`fillbiome`设置plains并复测原表示、CV implicit和pairwise。三者当前查询/容量均1/1通过，各1895个四房间匹配最大spread为0.833448/0.361390/0.519317°C。位置外几何/捕获Page背景仍是归因限制，不能把所有spread都当四格分区偏差。

原表示对pairwise最大11.747885°C发生在点火初期第120tick；后续燃烧最大2.874889°C、冷却2.869388°C。原表示对CV implicit初期9.639606°C、后续3.039833°C。CV implicit对pairwise最大2.108278°C。这些范围比此前两个基地的2.300°C更广，约1°C目标明确未通过。原`AirShapeStore.compile`在support未齐时不生成局部函数，日志显示原表示到100tick仍0 local modes、120开始出现、140四房间均18；共享节点在Air已捕获时即可接受局部输入。三者累计源交付账一致，支持旧表示初期局部响应延迟，而不是新增了源J。时间推进差异也有贡献，后续空间温差仍存在，换回全局PCG并不能自动满足1°C。

本调查未找到一个已证实能低成本同时修复所有场景的局部改动，不继续调整统一源比例、latent或新增架构。它完成了归因范围与失败边界，不算温场修复交付。[统一房间原/pairwise](../../build/continuous-air-near-source-translation-uniform-20261007.json)、[原/CV](../../build/continuous-air-near-source-translation-cv-vs-original-20261007.json)、[CV/pairwise](../../build/continuous-air-near-source-translation-pairwise-vs-cv-20261007.json)。

同源码生产5/5通过，仍1次累计房间准备拒绝后恢复、root122.057MiB；读者/phase2/2且拒绝0，关闭1702904B→0，粉雪仍7017供热tick，与旧plain pairwise一致。没有重复独立JVMcodec测试：本包未改保存格式或保存实现，实际卸载/重载已在5项回归覆盖，既有跨JVM结果范围保持原记录。下一交付缺口仍是累计空间/临时预算、约1°C温场/玩法边界、排队/长期/真实客户端和R3/R7，普通默认未推广。

证据：[128性能](../../build/continuous-air-pairwise-volumegraph-128-performance-20261007.json)、[256性能](../../build/continuous-air-pairwise-volumegraph-256-performance-20261007.json)、[256曲线](../../build/continuous-air-pairwise-volumegraph-256-envelope-20261007.json)、[生产](../../build/continuous-air-pairwise-volumegraph-production-20261007.json)、[生命周期](../../build/continuous-air-pairwise-volumegraph-lifecycle-20261007.json)。

## 2026-10-08：容量包交付与新增失败的实际原因

本包在既有pairwise架构上完成12槽连续边地址、无损单long排序输入、变化区域构图前预留和单次backing计量。旧的准备暂存不再与已完成准备的完整候选同时计费。热源、M/K、double、时间推进、材料law/latent与v5 codec没有调整；准入/回收时机改变仍需要整条生产链回归。

| 累计生产运行 | 功能结果 | 实际问题 |
|---|---|---|
| 10月7日volumegraph | 5/5 | 1次保留状态准备拒绝后恢复 |
| 初次flat12 | 5/5 | 2次保留状态准备拒绝 |
| 生命周期修正、双long构图 | 4/5 | 156次构图预留拒绝，红外替换失败 |
| 收紧完整Brick端点上界 | 4/5 | 121次构图预留拒绝，红外替换失败 |
| 单long输入、旧读者协议 | 4/5 | 构图/保留拒绝0，快照借用拒绝，后来达到活动单元上限 |
| 共享读者计费、最终代码 | 5/5 | 准备/读者拒绝0、该次运行没有活动单元超限 |

前两次持续图拒绝与本包新增的保守预留有关，不能全部解释为原系统问题。单long减少实际临时数组后，后续布局可继续安装，触发既有读者协议的更大规模边界。取证tick3400：query已用13815934B，borrow另需5021928B，总18837862B=17.965MiB>16MiB，snapshotPresent=false，材料仍LIVE。失败版本最大已建区域123147空气系数，旧通过版本96259；此前有限场景通过不能认证新运行状态。

`borrowRegionalState`原来为已经由基础发布持有的数组再保守预付完整payload。现在当前发布及借出版本通过一个预留覆盖唯一数组并集；`ensureRegionalPayload`在引用交换前检查并集；最后读者释放后关闭交接费用归零。当前版本的首次借用使用既有1024B元数据余量，无需全维度重复扫描或整份新预留。checkpoint也使用`beginRead`，读取拒绝会阻碍休眠/回收；日志中读者拒绝早于后续66327/65536活动单元拒绝，后者不能当作红外第一次失败的直接原因。当前五项回归较早完成，尚未证明长期永不达到活动上限。

| 最终同源码验收 | 结果 |
|---|---|
| 累计生产 | 5/5、准备拒绝/读者拒绝/worker异常0；root114.992MiB |
| standard双基地256、2400tick/realtime20 | 1/1；P50/P95/P99=125.148/195.534/408.707ms，root65.502MiB |
| 256实际消费与源账 | 256enabled、16 LIVE楼板、12000调用、age/geometry/receiver/WORK_LIMITED0、172迁移对齐、余额0 |
| 与优化前plain pairwise温场 | 816点46528匹配，最大0.003229°C；独立历史，非全场/bitwise证明 |
| 读者/材料phase | 2/2；1703096B→0，粉雪仍7017供热tick |
| 独立JVM真实保存/重启 | 各1/1；实际H/branch磁盘校验、重新点燃和通道更新 |

前次256 P95/P99=174.801/325.233ms、root84.742MiB；本次内存更低但CPU观测更高，不能宣称所有性能指标支配前版。约1°C旧表示温场、全部剩余构建暂存、广泛R3、长期/真实客户端/CT2与默认推广仍未完成。失败日志均保留。[根因链](../../build/continuous-air-flat12-reader-root-cause-20261008.json)、[当前容量](../../build/continuous-air-flat12-readers-final-256-20261008.json)、[温场](../../build/continuous-air-flat12-readers-final-256-envelope-20261008.json)、[生命周期](../../build/continuous-air-flat12-readers-final-lifecycle-20261008.json)、[保存](../../build/continuous-air-flat12-readers-final-restart-20261008.json)。

## 2026-10-08：C0/C1支持缓存、准备复用与真实CPU观察

本包沿用`regional/fine_grid/local/pairwise`，没有增加新的空间表示或材料模型。完整同源支持依赖不变时复用搜索；全部region原身份/顺序保留时复用组合布局和索引；网格局部编号使用原暂存数组，计量身份表按前次实际条目数预分配。具体条件见[运行合同](continuous-air-runtime.md)。普通源码默认未推广。

| 同轮实际two128/2400tick/realtime20，均带JFR | 基线 | 最终 |
|---|---:|---:|
| 完整worker P50/P95/P99 ms | 89.768/145.994/285.013 | 75.835/117.193/309.450 |
| 构建/准入/安装P95 ms | 111.654 | 84.412 |
| 累计worker CPU ms | 4156 | 3890 |
| 累计worker分配B | 4674150000 | 4334419232 |
| Root已计费峰值MiB | 65.502 | 65.697 |
| 输入tick主线程P95/P99 ms | 0.310/1.807 | 0.317/2.097 |
| 支持搜索/命中次数 | 12928/0 | 2322/10606 |
| 支持搜索访问格数 | 12115360 | 2113148 |

最终完整P95约下降19.7%，累计CPU/分配仅下降6.40%/7.27%，P99约上升8.6%。最慢cut位于启动，独立异步历史及不同GC分布仍影响计时；不把搜索少82.6%或中间最佳CPU3359ms当成最终总收益。累计分配不是同时存活内存，输入tick也不是全部服务器MSPT。

最终256全部消费/时效/geometry/WORK_LIMITED/预算拒绝检查通过、源余额0、173迁移对齐。46919匹配/816点相对本轮pairwise最大差5.153e-11°C。累计生产5/5、root114.991MiB、拒绝0；读者/phase2/2、1703096B→0、供热7017tick转换；独立JVM保存/重启各1/1。实际单区块写盘隔离1/1，在生产入口观察到pending时skip=1且无新增barrier。

自动卸载回调重叠探索未触发，单独序列化/整服save的探针失败保留；有关热区块保存仍等待。无数值构建cut P95=60.680ms，CPU20%/分配30%/稳定50ms目标未全部过，因此不追加12000tick或默认宣传。区域排队在内时延、宽泛R3、完整暂存、真实客户端/多维度与约1°C旧解析式差异继续待办。

报告：[所有运行与门槛](../../build/continuous-air-cpu-package-20261008.json)、[温场](../../build/continuous-air-cpu-package-final-envelope-20261008.json)、[日记与失败说明](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。
