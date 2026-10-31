# Continuous Air Runtime

- Status: `Partial; current control contract linked below, dated numerical and gameplay trials retain their original scope`
- Last verified: `2026-10-25; current retirement/control references updated; numerical trials below retain their original dates and build scope`
- Scope: historical continuous Air backend trials, material coupling, source delivery, spatial queries and version-6 data representation; current control linked below
- Code anchors: `RegionalWorkQueue`, `AirFieldLayout`, `LocalAirShape`, `AirFieldCompiler`, `SharedAirGridCompiler`, `AirOperatorFragment`, `CoupledThermalOperator.prepareProjectionWarmstart/clearPairwiseEdges/sweepPairs`, `ContinuousAirSolver.CutCursor`, `LocalMaterialBoundary.exchangeAirContact`, `AirFieldCheckpoint`, `RegionalAirRuntime`, `ThermalDimensionEngine.processWorkRound`, `MinecraftThermalInput.needsChunkCheckpoint`

当前区域共享网格的输入、任务、内存准入、查询和保存合同由[运行时控制说明](thermal-runtime-control.md)维护。2026-10-25已退役旧FUNCTIONS/scalar运行时、`continuousAir/continuousAirBackend`选择项、`ThermalArchitectureTrial`、`ThermalPrecisionTrial`和旧整批协议。源J统一归`SpatialHeatStore`，粗细共用领取、取消和提交；可选细档使用内存准入目标，已接收工作、粗档、发布和保存可以超出并继续记账。旧函数布局的v6存档读取和数学求值仍按数据用途保留。下文旧选择器、启用方式、方法名和测试数量只描述相应历史批次。

后续内部清理已删除旧局部函数生成、`LocalAirBasisCache/LocalAirModeRank/CoarseAirModeSpan`、旧体积函数积分和专用试验开关；构建现在明确选择共享细网格或直接粗布局。旧`LocalAirShape`数据的解码、温度求值和恢复投影仍使用。本页下文有关这些已删除构建器的内容保留原试验日期，不代表当前有可选运行入口。

以下带日期的实现与验收保留其原批次归属；旧“未交付”结论、旧CutRound、RegionPending或TopologyRetry描述不能替代上述当前协议。

## Activation and ownership

当前正式SHARED_GRID控制与2026-10-25退役后的验证范围见[输入、任务与保存所有权](thermal-runtime-control.md)。旧Page源接管和重复质量状态已删除，当前`TopologyInbox`只接管同一份Page控制载荷；下文历史批次不能证明本次删除后的代码已经逐项重跑。

2026-10-19批次曾改为独立接管、世界位置待办、RegionJob与私有GeometryWork/Rebuild；当时完整资源隔离与自动降级尚未交付。其[区域试验说明](continuous-air-regional-preparation.md)和[工程方案](../../diary/2026-10-29_23-38-00_thermal-development-history.md)保留当时的实施与失败证据，当前控制和验收转到上面的运行时说明。

以下带日期条目保存相应实现阶段的证据。它们涉及的旧控制流程与通过范围不能覆盖当前替换代码；尤其旧版正常容量、CT2和保存通过不自动变成新版本资格。

2026-10-18主线程输入持有改为`ThermalInputBatch.Submission`区块摘要，保存/卸载检测不再遍历整批输入。worker完成时释放原载荷，main只持有摘要及`TopologyRetry`直到应用世界效果并ACK；主线程不再提供原批次作为重试fallback。当前实现及验收状态见[摘要与重试合同](continuous-air-regional-preparation.md#主线程区块摘要与重试载荷)。本项尚未删除两输入窗口或整批物理完成约束。

2026-10-18区域源输入已移入`RegionPending`，区域接管Receipt与一次执行Assignment分开；取消保留前缀，提交退休前缀，布局交接归还未消费输入。原cut的全批源副本/逐记录receipt数组已删除。该批核心输入合同和正常256场景通过，选定13 required通过；单核连续两轮跨越未通过，仍有末段整体预算扫描的服务机会缺口。待办保持一个执行前缀和两个等待前缀；旧区间聚合、独立接管与两窗口/整批屏障删除尚未交付。当前实现、混合性能结果和未通过证据见[区域输入合同](continuous-air-regional-preparation.md#区域待办接管与执行收据)。

本区材料内部导热的地址现于构建时编译为紧凑数组，推进不再查找其他区域的候选边；热运算次序与材料热律保持。数组生命周期、候选预算及验收状态见[本区材料边](continuous-air-regional-preparation.md#本区材料内部边的编译)。

2026-10-17当前重构已接入`RegionalWorkQueue`统一普通与构建检查点的区域推进，删除维度源求解的`SolveRound`列表、其串行回退和独立`advanceStableRegions`循环。每区域一个排队/运行工作，由相同冻结/执行/接纳流程处理；结构保持不变的普通输入也按同一依赖证明服务。环境仅保存尚未安装的局部覆盖值。维度输入解释和结构事务仍保留。功能/单核边界通过，最终normal/native补测通过，CPU增长仍未关闭。实现合同由[区域说明](continuous-air-regional-preparation.md#2026-10-17单一区域工作循环)维护，具体性能不利项及全部失败见[本轮报告](../../build/region-work-results-20261017.md)。

2026-10-16新增`FH_CONTINUOUS_AIR_WORK_POLICY_TRIAL=observe|auto`开发计量/自动整区档位路径，复用已有prepare/install/abort、源收据及当前粗场恢复；普通环境仍精细。仅C4材料面改用中心单点/全G，精细面仍四点。当前实现、成本实录、恢复退避和未资格化边界见[CPU与过载档说明](continuous-air-overload.md#cpu归属与整区域自动档位试验)；不能将开发闭环当全部生产隔离已通过。

2026-10-15过载调查已撤回固定节点自动降级及新源延后注册。超时/均温候选的单核隔离也未通过，现仅显式开发环境变量可启用；普通SHARED_GRID保留既有精细计算。保留源发现分片，但没有认证全服deadline保护；[当前过载状态](continuous-air-overload.md)记录失败证据、原事件门禁及所需架构条件。下文既有认证不覆盖这个未交付候选。

2026-10-16新增显式区域粗细选择和从当前粗系数重建精细场的开发验证，不保存长期精细残差。随后已安装区域的结果接纳改为协调者按完成收据调用原commit/源确认，队列先通过原预算准入；外层维度ACK和拓扑门禁保持。具体行为、单核/普通负载结果与未交付范围分别见[过载记录](continuous-air-overload.md)及[区域发布边界](continuous-air-regional-preparation.md#区域发布查询时间与保存)；不把同cut提前接纳视作自动降级或单核256源认证。

同日将有界一般输入接收与提前推进资格分开，原两份队列可接收注册/卸载/恢复等数据，token持有到main应用完成后的ACK。提前推进仍使用原事件和边界限制，一般输入按正常process处理；[接收实录](continuous-air-overload.md#有界输入接收与实际完成)保留两个未覆盖目标时序的失败和实际单核重载通过范围。它尚未解除整维度物理完成或拓扑依赖。

后续局部结构输入增加`RegionalContinuationBoundary`和`FrozenSources.targetsUnchangedAcross`：区域依赖独立且所有共同录制源的世界面目标稳定时，允许跨越未安装的远处变化；出口/受影响路由保留原边界。领域服务点扩到拓扑准备/partition，边界记录保留至对应正常输入完成，外层ACK保持。真实单E核257源功能1/1、普通256回归1/1、production/native8/8及取消/粗细恢复均通过；普通cut均值和完整tick尾部有不利变化，未知路由等隔离缺口仍在。具体实现与全部失败见[区域合同](continuous-air-regional-preparation.md#局部结构边界与服务机会)和[验收状态](continuous-air-overload.md#局部结构边界与真实跨越验证)。此项不调整热公式、精度、网格默认或v5，也不是CPU硬截止/自动降级。

2026-10-17进一步限定未完成/未提交通风的等待范围，并将GRID_PAIRWISE恢复输入按Page/region依赖保护。远处区域可在原源目标稳定的前提下继续接受实际源区间；恢复C/H/branch仍只在正常process安装，原源身份事件和外层ACK保留。真实单E核257源pending路径、原40tick及两个负向通过；正常256与原版保存/卸载8/8通过，具体范围和尚未闭合的硬工作上限见[当前状态](continuous-air-overload.md#未完成通风与页面恢复的局部边界)。

同日后续实现`processRound`保留单份cut阶段，拓扑/材料和区域任务分轮归还既有pool，运行中的helper以真实完成回调唤醒。完成通知最多3份，worker不再逐cut等待旧main ACK；接收仍不代表物理完成，dirty/phase/source收据仍按真实提交和有序main应用处理。查询回收移至worker安全边界，通知最大载荷已在原预算预留；实现与尚未通过的边界见[协调轮说明](continuous-air-overload.md#协调轮与完成通知)。协调轮/通知及原40tick局部子项已有限验证，正常误降级已修复；完整输入屏障、最低档长工作、正常尾延迟和多维度冷加载资格仍未闭合。结果见[最终报告](../../build/coordinator-rounds-results-20261017.md)。

后续统一输入所有权：mailbox与engine共享`ThermalInputStore`，删除重复队列和reservation表。最多2份等待输入、5份总保留载荷，首份SHARED_GRID输入也计费；已完成等待main ACK的载荷不再占等待槽，原128MiB预算保持。2026-10-18区域材料进一步改由`ThermalRegion.Accepted`拥有，区域接纳/安装不再向arena镜像写回材料H/branch；迁移、初始化、相变和控制通过`MaterialStateAccess`访问。完整合同见[区域已提交状态](continuous-air-regional-preparation.md#区域已提交状态与材料边界同步)。当前仍是有界整批输入和维度活动cut，尚未交付逐区域合并待办及完整U1；后续架构与删除清单以[统一区域运行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)为准。

该子项真实Forge共11个required通过，正常同点温差max0.160189°C；平均延迟改善，但更早基线CPU仍较低、计费峰值略升，完整区域隔离尚未实现。两次单核失败和最终通过范围由[本批报告](../../build/input-store-results-20261017.md)保留。

当前本机两处各128源的基础可执行热系统里程碑已完成。普通配置启用组合、实际保存/CT2/双维度验收及已知限制由[当前保存说明](continuous-air-persistence.md)维护；[有限收尾与停止条件](../../diary/2026-10-29_23-38-00_thermal-development-history.md)列出基础门槛，新增的完整过载保护按[统一区域运行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)继续。以下带日期的旧pending与性能数据是对应阶段记录。源码默认仍可选，100热消费者不等于100真实连接。

用户随后要求的真实单核/单硬件线程测试独立验证了更小硬件范围：整个Forge JVM固定到i7-12800HX一颗无SMT的E核，正常SHARED_GRID/native组合8项兼容性通过，但两基地总256源生命周期容量失败，实际重载40tick未恢复。两个有效JVM仍正常关闭Root0。此结果不改写此前多核通过记录，也不能推广为单核256源可用；[单核结果](../../build/real-single-core-final-20261014.md)保留wall/CPU、保存路径和失败计数的区别。生产线程池本就至少保留1worker，本轮没有更改热行为。

后续有限恢复诊断与一次小候选复验也未通过单核容量。8点捕获在loaded+2至+6tick已完成，+15tick才提交，随后仍有构建/调度成本；单纯放宽网格生命周期复用只增加1次非关键命中，候选已撤回，以下生产复用规则保持。保留的新增代码仅GameTest开发观察，原40tick/20tick供能/热参数未改；[恢复调查收尾](../../build/single-core-recovery-final-20261014.md)记录了下一步独立准备为何需要实质生命周期改造，不再自动追加小候选。

Unchanged shared Air structure now retains the compiler's immutable node addresses,
cell-corner maps and node positions. Reuse checks the ordered component slots,
generations, coordinates and Air masks exactly; newly captured components keep
their current Page dependencies and geometry revisions. Combined layouts cannot
donate their renumbered rows, which protects the existing pairwise sweep order.
Geometry changes retain the original full compiler. Material contacts, field
projection, source input, time steps and thermal parameters still follow the
existing path. This adds no numerical approximation or persistent node registry.

The final local i7-12800HX run covered two128 real campfire bases,256 real furnaces,
three source/geometry/recipe cycles and three actual Forge chunk unload/reloads
over12000 ticks. Every reloaded base produced fresh current samples within40 ticks;
age/geometry/receiver/WORK_LIMITED violations and worker failures were zero. The
600-cut run took600008ms and released the charged Root to zero on close. Its worker
P95/P99 was111.240/150.559ms, Root peak72.965MiB, and cumulative worker allocation
11,613,165,960B. The completed baseline allocated12,484,967,824B: recorded allocation
decreased6.98%. CPU/wall improvements are one observed pair and include load/JIT/GC
variation; server CPU increased in these counters, so this is not a fixed total
server CPU speedup. Root peak was nearly unchanged; cumulative allocation is a
different measure from retained backing or live JVM heap.

The temperature comparison validates identical A_only action signatures and
matches46,327 observations at813 Air points, covering98.476% of candidate current
observations. Maximum difference from the accepted current implementation is
0.003332C. Reader release and the real powder-snow transition still pass, with the
latter retaining7017 supply ticks. All nine final required production instances
passed. The earlier comparison using A_only against both_bases is explicitly
invalidated and retained as diagnostic history.

Endurance bases are separated512 blocks so the other base/spawn terrain tickets
do not prevent the target's actual unload; the original160-block temperature and
short-load scenes remain available. The GameTest clock waits after processing on
a fixed50ms schedule, rather than consuming Minecraft's maintenance budget at
START. It is isolated to GameTest sources. Related-save thermal waits remain:
the final longest was301.939ms; another complete run observed423.317ms. Unloads
occurred but no pending-worker unload overlap was observed.100 FakePlayer thermal
consumers are not100 network clients. Defaults remain unchanged. Evidence:
`build/continuous-air-retained-grid-20261010.json` and
`build/retained-grid-temperature-matched-20261010.json`.

Current acceptance follows the user's 2026-10-09 instruction to preserve the current
observed temperature differences and prioritize performance and maintainability.
The earlier 2026-10-07 approximately 1 °C objective is retained as historical evidence.
Earlier 0.01 °C targets remain historical. Reports retain a strict 1.000 °C boolean
check separately. The current performance foundation is original-face `pairwise`,
whose dense-base observations reached 2.300 °C from the original coarse/local field.
Uniform-biome nine-fire rooms additionally exposed 11.748 °C during initial support
growth and 2.875 °C during later burning. The historical approximately 1 °C objective
was not met; subsequent optimizations are compared against the accepted current field.
Successful capacity tests do not certify a whole-field temperature error bound. The
25/75 `pairwise_plume` comparison is not the recommended universal source rule.
Source energy, finite state, residual heat, actual geometry, material/phase,
persistence and query lifecycle are still validated.

The common configuration now exposes `continuousAirBackend = "SHARED_GRID"`
alongside `continuousAir = true`. This selects the existing regional/fine-grid/local/
pairwise combination as one mode; it does not introduce another numerical path.
`FUNCTIONS` remains the configuration default and `continuousAir` remains false by
default. Restart the client or dedicated server after changing the backend.
If any of the four development backend environment settings is present, their
existing process selection overrides the configuration as a group. The dimension
startup log reports the actual selection, runtime, representation, material mode
and operator. Precision and the other diagnostic settings keep their existing roles.
Eight final Forge JVMs passed 17 production cases through this configuration and
the preserved environment override. The machine-field comparison matched 46,338
observations at 813 available Air points, with a maximum difference of 0.003332 °C.
The three remaining probes were occupied by actual machines. A separate full
2400-tick machine run with PROFILE and point logs disabled observed worker P95/P99
166.595/265.349 ms and a charged Root peak of 63.724 MiB. This certifies the recorded
short capacity and behavior checks, not a fixed speedup, real-client load or default
promotion. See the formal-backend report in `build/continuous-air-formal-backend-20261009.json`.

The bounded checkpoint-isolation candidate was tested and withdrawn. Early region
readings, source settlement and actual material waiting flags were investigated,
but full pending-input checks did not admit a real related save in the two-base
scene. The trial's additional snapshot flags, readiness checks and diagnostics were
removed from the runtime. Related saves/unloads still use the established barrier;
the previously delivered unrelated-chunk fast path remains in place.

Explicit `FH_CONTINUOUS_AIR_RUNTIME_TRIAL=regional` now enables persistent numerical
regions and region-scoped construction/projection using the existing solver.
Stable regions retain their local layout/operator; dimension-wide source handles carry regional
stencil addresses. The bounded worker pool runs regional work concurrently and publishes
immutable region snapshots at each region's actual advanced tick. Air readers use one
atomic directory/snapshot envelope without rejecting a point solely because a distant
region publishes. A bounded future-input candidate accepts at most two later inputs during
construction and can advance a stable region with frozen spans and once-only source receipts.
Topology and completion ACK order remains serial; broad dispatch, merge/split time protocols
and default capacity remain pending production gates.
With neither a shared-grid config selection nor a runtime trial, the existing global
path remains active. See [regional preparation](continuous-air-regional-preparation.md).

Regional merge/split preparation now inherits immutable volume matrices and unchanged
rank decisions from all parent regions. `ContinuousAirSolver.seedMerged` still projects
the complete old field; a single parent may donate mutable operator/PCG scratch. This
avoids treating a large field as a cold numerical build when it absorbs a small Air
component. It preserves the spatial functions, double state, physical matrices and
stopping policies, and remains subject to the original basis/mixing reuse checks.
It does not by itself satisfy the 128 MiB capacity gate.

Regional infrared/save cursors register a shared lease for their immutable envelope.
The installed envelope and all borrowed generations share one payload reservation,
deduplicated by backing identity before regional reference exchanges. Borrowing the
current envelope uses its already-paid arrays; the first lease/map fit the existing
1024-byte bookkeeping allowance. Retired generations continue contributing every
unique array until their readers clear. Retained layouts remain in Air backing.
Closing a dimension retains the exact reader payload, transfers its retained-layout reservation
and keeps the infrared epoch-array reservation until the last cursor clears. Ordinary
point reads remain lock-free, and global cursors retain their seqlock path. The
main-thread completion ACK requests reclamation; the next worker cut safely shrinks unused regional publication capacity before candidate admission. These
ownership changes preserve the numerical model and checkpoint format.

The R6 common-broad-response experiment was tested and rejected. Its mean-only
and one corrected affine-source-moment representation reduced memory and worker
time in the actual 128-source base, but maximum matching near/far temperature
differences were respectively 1.666 and 0.992 °C. The runtime selection and
experimental source were removed. Current launches retain the original coarse/local
representation; no shared-broad trial is available through configuration. Its evidence
and stopping decision are recorded in [regional preparation](continuous-air-regional-preparation.md).

The existing `FH_CONTINUOUS_AIR_TRIAL=fine_grid` representation remains an explicit
experiment. With regional/local ownership it passed one complete standard-budget
256-source run; it still differs from the original field by up to 0.552 °C in the
matched 128-source observations. After successful unit-grid projection,
`CoupledThermalOperator.releaseProjectionWorkspace` now releases the aggregate
projection matrix and factor; unit-element templates, accepted state and ordinary
transport mass remain. A later build reassembles projection rather than reusing
released backing. This lifetime change preserved all 160 matched fine-versus-fine
observations, but does not remove the representation's behavior differences.
Accumulated-room regression still recorded three recovered preparation refusals;
exact construction-temporary admission and default enablement remain incomplete.
See [architecture comparison](continuous-air-architecture-comparison.md).

Fine launches may explicitly select `FH_CONTINUOUS_AIR_GRID_OPERATOR_TRIAL=projection_cells`
to project using the existing unit mass matrices directly. It changes assembly and
iteration aids, preserves the matrix/RHS and stopping policy, and avoids the aggregate
projection graph and IC allocation. Actual 128 and standard256 runs plus production
and reader/phase cases passed; matched fine differences were below 0.000003 °C.
The unset selection retains assembled projection. Full block-incremental construction,
accumulated-space capacity and original-versus-fine behavior gates remain incomplete.
Read-only cursor observations can report instantaneous grid coarsening and original
basis storage estimates; neither applies a coarser representation to the live field.

`FH_CONTINUOUS_AIR_GRID_OPERATOR_TRIAL=control_volume` is the performance-first
nodal control-volume candidate. It reuses Q1 nodes, continuous queries and checkpoint
format, but changes the discrete operator: twelve axial edges per unit cube, nodal
surface exchange and cell-side mass-weighted nodal transfer on topology changes.
Unchanged grid geometry retains its node temperatures; restored corner samples use
the cell interior side. Transport uses the existing sparse air IC with fill level 1.
It changes heating/cooling response and is not an exact reproduction of the earlier
FEM/analytic field. The unset selection still uses assembled projection.
Actual standard256 worker P95/P99 was 198.349/337.194 ms with zero monitored
freshness/geometry/receiver/WORK_LIMITED failures; accumulated-room regression still
recorded two recovered preparation refusals. See the architecture comparison.

`FH_CONTINUOUS_AIR_GRID_OPERATOR_TRIAL=pairwise` explicitly uses the same nodal
control-volume field with local symmetric exponential exchanges. It requires
`fine_grid` and `FH_CONTINUOUS_AIR_MATERIAL_TRIAL=local`; the recorded capacity
path also uses `FH_CONTINUOUS_AIR_RUNTIME_TRIAL=regional`. Daily advancement omits
global transport PCG and Air IC. Projection and J impulses retain the existing
mass paths. For an isolated Air pair, with capacities `C_i,C_j` in J/K,
conductance `G` in W/K and half-duration `h` in seconds, exchanged energy is
`E=(T_j-T_i)*(1-exp(-G*(1/C_i+1/C_j)*h))/(1/C_i+1/C_j)` in J.
Forward/reverse sweeps cancel pair energy internally; whole-graph composition and
buoyancy frozen at the substep start remain time approximations. Material surface
exchanges use actual finite H, law slope and phase endpoints, and retain private
internal conduction. This is a different advancement from coupled Euler.

The pairwise constructor now compiles only volume transport pairs. Boundary,
material and route contacts retain their actual contact arrays and local flows;
their unused global matrix pairs and nominal off-diagonal arrays are omitted.
`compilePairwiseGraph` sorts primitive node-pair keys and occurrence addresses,
deduplicates the pairs and fills twelve edge addresses per voxel in a contiguous
int array. A shared map preserves the original triangular matrix-entry order.
When node and occurrence index widths together fit63 bits, one long stores the
complete `(first node,second node,address)` tuple with a clear sign bit. Larger
layouts retain the two-long-array path. No index truncation or float conversion
is involved. Primitive radix sorting preserves the original pair/address order.
Volume accumulation and the order of nonzero forward/reverse exchanges remain
unchanged. Reuse checks depend on volume addresses/nonzero structure; changed
contact support refreshes the local contacts without invalidating this volume graph.
Other advancement backends retain their existing graph branch. Retained arrays
continue through the existing meter. Changed regions reserve graph inputs, output
addresses/endpoints and radix stacks before helpers start. A full Air Brick has
300 potential edges; partial Bricks retain a conservative twelve-per-voxel endpoint
bound. Numerical preparation and one backing scan finish before their scratch
reservation is released and the complete candidate backing is admitted. Other
shape/contact preparation still uses the existing scratch estimate; this is not
a completed enumeration of every allocation in the runtime.

`pairwise_plume` adds the retained performance-first near-source correction.
`AirLoadTable.PLUME_DIRECT_FACE_FRACTION=0.25` is a dimensionless trial constant,
not a configuration default. A source Air stencil puts 25% at the original
physical receiving face and 75% at the centers of its receiving Air voxel and
available six adjacent Air voxels in the same numerical region. Center weights
are 1/8 per voxel corner, normalized so total input remains the recorded W/J.
Material voxels are excluded. The stencil is compiled on binding/rebinding and
frozen-load remapping; queries do not scan sources. This deliberately broadens
effective source deposition and changes near-fire warming, cooling and material
phase timing. It is not a lossless change or a proof of a more accurate PDE.

Expanded actual sampling exposed the earlier small-probe limitation: original
versus CV/fine/pairwise maxima were 1.916/2.502/2.300 °C at low positions just
above fires. Both original local shapes and unit-grid queries are trilinear within
Air cells; the function space, mass/transport discretization, face exchange and
time advancement differ. More grid degrees of freedom do not guarantee closer
agreement with the old model. The plume correction reduced the maximum to
1.080789 °C in 23,342 matched dense128 observations and 1.080822 °C in 45,802
matched two128 observations. These compare exact coordinates, elapsed event
schedule, published sampleTick and natural temperature; independent input histories
and finite samples still prevent a whole-field certificate.
Standard256 plume worker P95/P99 was 153.181/305.398 ms and charged peak 85.708 MiB;
zero monitored age/geometry/receiver/WORK_LIMITED failures and zero source imbalance
were recorded. Accumulated-room production still had one recovered preparation
refusal. Powder snow required 7,797 actual supply ticks in the recorded phase
scene, versus the earlier CV's 7,297 and original reference's 6,877. Ordinary
defaults, broad R3 timing protocols, precise all-temporary admission and real
100-client certification remain pending. See the architecture comparison and
the [plume results](../../build/continuous-air-performance-pairwise-plume-results-20261007.json).

A subsequent actual `pairwise` lifecycle run disabled only plume source deposition.
It converted powder snow in 7,017 supply ticks, 280 ticks faster than CV+implicit
transport and 780 ticks faster than `pairwise_plume`. This isolates the source
deposition choice observationally; it does not justify attributing the additional
delay to removing PCG. The current powder-snow recipe uses a 9 °C plateau and
3900 J/K capacity; the compiler's absent-target fallback adds 38,000 J latent
energy. Identical initial H was 39,000 J. In the recorded phase scene at helper
elapsed 5300, adjacent Air was 29.437 °C original, 29.332 °C unspread pairwise,
27.279 °C plume; phase progress was 71.93%, 70.33%, 61.76%. Global source W/J
balance alone does not guarantee equal heat reaching that material.
See [phase attribution](../../build/continuous-air-phase-delay-attribution-20261007.json).

`FHConfig.COMMON.THERMAL_RUNTIME.continuousAir` is a restart setting, default `false`.
`MinecraftThermalProfiles.Tuning` captures it with the other thermal settings.
`continuousAirCorrectionToleranceC` is also captured at restart, with source default
`1e-5 °C` and allowed range `[1e-12, 1e-3] °C`. It controls Air topology projection.
Direct J impulses retain `min(configured tolerance, 1e-9 °C)`. Ordinary transport
uses the gameplay policy below; the explicit legacy `baseline` comparison also
uses this configuration for powered transport and caps unpowered transport at
`1e-9 °C`. Neither this configuration nor the gameplay stopping estimate certifies
total model or whole-field temperature error.

`ThermalPrecisionTrial.SELECTED` reads the optional server-process variable
`FH_CONTINUOUS_AIR_PRECISION_TRIAL` once. Ordinary launches now use `gameplay_1e_3`:
both powered and unpowered **ordinary transport** use
`max(1e-6 °C, min(0.001 °C, 0.001 * firstStepCorrectionEstimateC))`.
The first estimate comes from the actual pending physical substep's initial
preconditioned residual. The resulting threshold remains fixed across that
substep's nonlinear and factor retries; a rejected substep and its replacement
each select their own threshold. An initial nonzero correction estimate requires
at least one PCG update, even when already below the absolute floor. An exactly
zero initial estimate can return without iteration. This prevents unconditional
acceptance of unchanged temperature on every small late cooling step; it is not
a proof that every mode or long trajectory has bounded error.

The normal rule no longer switches every unpowered solve to `1e-9 °C`. It keeps
double storage, the physical mass/conductance/source matrices, source W/J,
material H/branch/phase, spatial functions, 20-tick cuts and checkpoints. Projection,
impulse inversion, phase endpoint rounding, nonlinear buoyancy checks and the
existing `1e-6 °C` sleep residual are separate rules and retain their prior values.
The small endpoint rounding comparison itself does not cause long PCG iteration.

Explicit `baseline` retains the previous transport stopping rules for comparisons.
Explicit production experiments may select `absolute_1e_3` or
`absolute_1e_2`, replacing **ordinary transport** stopping with respectively
`0.001 °C` or `0.01 °C` for both powered and unpowered steps; `relative_1e_2` uses
`max(1e-9 °C, min(0.01 °C, 0.001 * initialCorrectionBoundC))` in each linear solve.
The initial correction estimate is formed from the residual of the pending step
and the independent acceptance preconditioner, not from absolute Celsius temperature.
`step_relative_1e_2` computes the same relative threshold on the first linear solve
of each physical substep, then retains it for factor retries and nonlinear
material/buoyancy refinements of that substep. A rejected substep and its smaller
replacement each compute their own threshold. The material branch and buoyancy
convergence criteria remain independent and unchanged. This variant prevents
repeated relative tightening from consuming the original precision saving.
These policies retain topology projection, direct J impulse inversion, phase
endpoint rounding, double storage, spatial functions and thermal parameters.
The stopping estimate is not a certificate of total field error, cumulative
temperature drift, cooling progress, sleep eligibility or gameplay equivalence.

`ThermalCompletion.CutTiming.linearSummary` snapshots the corresponding completed
cut's transport diagnostics. `poweredSolves`/`poweredIters`/`poweredZeroIters`,
`quietSolves`/`quietIters`/`quietZeroIters` and `impulseSolves`/`impulseIters` separate
the actual linear calls; nonlinear repeats and rejected attempts are included.
`transportInitialMaxC`, `transportFinalMaxC` and `transportToleranceMinC`/
`transportToleranceMaxC` report estimates and effective thresholds in °C.
Rebuild timing also separates `volumeAssemblyUs`, `rankUs` and `rankRecompileUs`;
`ThermalCompletion.CutTiming.rebuildTiming` now carries every successful preparation
attempt of its own cut, including a later refused candidate. `geometryPrepareUs`,
`airPrepareUs`, `admissionUs` and `installUs` separate the original topology,
continuous numerical construction, budget/reporting and installation. GameTest
`THERMAL_REBUILD_CUT` records these coherent completions. The periodic worker
summary still only describes the last successful preparation; it must not be
substituted for a complete rebuild distribution.

### Numerical construction reuse

`LocalAirBasisCache` now defaults to `cached`. Before volume integration it groups
overlapping raw local functions and checks the actual function data and all Air
support components. An unchanged group reuses its accepted rank selection before
integration; changed groups retain the original `1e-6` relative squared residual.
The candidate cache is included in backing accounting and rollback. Explicit
`FH_CONTINUOUS_AIR_BASIS_TRIAL=original` retains the earlier preparation, and
`compact` is an unpromoted approximation: scale1 first, scale>1 residual `1e-2`.
Neither rank threshold is a temperature-error certificate.

Halo invalidation now checks actual `GeometryHalo.positions` against the original
mutation journal. A change elsewhere in the same Section does not invalidate an
unrelated halo consumer. Full resync/chunk availability and directly captured
geometry keep their existing rules. Static response preparation shares one
`ResponseStencil` between both scales, preserving the `1e-22` stopping requirement.
The detailed implementation, actual results and limits are in
[local preparation](continuous-air-local-preparation.md).

Ordinary coarse/local launches now use `CoupledThermalOperator.ContactFragment`.
`FH_CONTINUOUS_AIR_CONTACT_ASSEMBLY_TRIAL=global` selects the earlier global contact
assembly for explicit comparisons. The compiler tags regular material faces and
far boundaries with their single Air component. Their Air/Air quadratic terms
are accumulated in that component's local coordinates, then scattered to the
current global addresses. Current contacts still supply all RHS, boundary, routed,
waiting, material feedback and material/Air cross terms; four-point face quadrature
is retained. Routed multi-component contacts use the original path. The unit-grid
and two-level architecture trials also retain their earlier assembly paths.

A new wrapper owns current contact indices and component metadata. Only immutable
numeric matrices/structure bits may share old backing, after exact basis, ordered
Air support, double weight, nominal conductance and wind-category comparisons.
The component-cache reuse does not subtract an old global matrix. It remains available when
the existing ill-scaled-layout check prevents global contact delta assembly.
For healthy layouts the existing `buildTransportDelta` remains available; the
cache supplies a faster fresh assembly when that delta is unsuitable. An initial
cache-only trial disabled delta assembly and regressed small-change work, so it
was repaired before the final retained hybrid implementation.
The grouped summation can change floating-point rounding; it is not bitwise
identity. `contactFragmentUs`, `reusedContactFragments`, `rebuiltContactFragments`,
`reusedFragmentPairs` and `rebuiltFragmentPairs` report work. `recordBacking`
includes old/new unique cache arrays, headers and wrapper estimates.

`AirOperatorFragment` normally integrates the same trilinear functions using
eight tensor Legendre polynomial coordinates inside each actual Air unit cube.
The transform diagonalizes the consistent mass and derivative integrals, avoiding
repeated Gaussian evaluation of values and three derivatives. These are temporary
construction coordinates; the dynamic field still uses the original coarse/local
functions and coefficient addresses. Coarse/coarse transport lumping, local cross
mass, mixing masks and buoyancy sampling remain unchanged. Arithmetic order and
rounding differ, and marginal rank decisions can consequently differ. Explicit
`FH_CONTINUOUS_AIR_VOLUME_BUILD_TRIAL=gauss` retains the previous quadrature kernel.

Projection normally tries at most eight iterations with its existing P32 aid
before preparing an Air IC factor. It still uses the configured projection
tolerance and freshly verifies the true residual. Failure of that cheap attempt
prepares the original strong factor and resumes within the original total budget;
later strengthening remains available. `projectionPreparation=adaptive`,
`projectionWarmIters` and `projectionWarmAccepted` distinguish the paths.
Explicit `FH_CONTINUOUS_AIR_PROJECTION_PREPARE_TRIAL=eager` prepares IC first.
This is an iteration strategy, not a weaker transport operator or relaxed stopping
requirement. Room production passed, but it only avoids IC preparation for part
of the dense rebuilds; broad geometry changes still need the strong path.

`MinecraftThermalInput.sampleAir` re-reads Page and query identities once when a
publication exchange crosses the read. `QueryPublication.readVersion` identifies
the exchange; stable missing geometry uses the existing fallback. The second
attempt retains the same real query tick, geometry rules and maximum age. It
does not lock the writer, wait for a cut, or mark an old sample as new.
`airPublicationRetries` in the production progress summary records this path.

Exactly shared transport graphs also share the immutable Air IC pattern via
`SparseThermalPreconditioner.copyPattern`; each candidate owns fresh factor/scaling
arrays. `airTransportPatternReused` reports that case. Changed global graphs still
rebuild the pattern. In the global path numerical construction still scans the dimension,
and the surface cache increases retained/peak memory. The opt-in regional path now has
persistent solvers and regional snapshots; its remaining cut barrier and capacity limits
are described in [regional preparation](continuous-air-regional-preparation.md).

Explicit `FH_CONTINUOUS_AIR_FACTOR_TRIAL=blocks` additionally prepares persistent
IC structures for the positive Air graph's connected blocks. It retains current
addresses/numeric factors, the original relative row order/fill strength, physical
operator and P32 stopping. The default remains scalar: a complete256 comparison
improved P95 but still missed freshness/latency. These iteration blocks are not
complete thermal regions including material state and independent publication.

### Projection coordinates for coarse-contained local functions

`CoarseAirModeSpan.inspect` checks whether an individual local function is already
in the captured coarse function space. It fits a coarse trilinear polynomial from
one actual Air unit cell, verifies all eight vertices of every actual Air cell,
and verifies shared coarse coefficient addresses across all affected components.
The comparison uses `max(256 * ulp(shapeMaximum), 1e-12 * shapeMaximum)` rounding,
where `shapeMaximum` is the dimensionless maximum absolute function value. A
small absolute amplitude alone never qualifies a function for this treatment.
These checks preserve the represented spatial space and the original function
and coefficient addresses.

The normal continuous backend uses `FH_CONTINUOUS_AIR_COARSE_SPAN_TRIAL=pin` when
the process variable is absent. Explicit `off` retains the previous projection
for comparisons; `observe` performs the checks and logs `THERMAL_COARSE_SPAN`
without constraining the solve. The choice is captured once by
`ContinuousAirSolver.COARSE_SPAN_TRIAL`.

Only during `Mode.PROJECTION`, each verified redundant local coefficient is fixed
at its existing `warmStartProjection` value. Its original mass coupling is moved
to the other RHS rows, and its row becomes the coefficient equality. PCG matvec,
Air IC and independent P32 correction estimation all use the same symmetric
elimination. The new `projectionIterationOffDiagonal` and pin/residual arrays
are included in `recordBacking`; factor reuse additionally requires the same pin
mask. The constraint is a choice among equivalent spatial representations, not
a material capacity or a source energy deposit.

The ordinary transport mass lumps coarse/coarse terms and retains local cross
terms, whereas projection uses the full consistent Gram mass. Removing the
functions from the whole layout could therefore change transport. The fix keeps
all functions, warm-start coefficient history, source W/J, material H/branch,
checkpoints and the original `TRANSPORT`/`MASS` operators. Functions cease to be
constrained when a later geometry makes them independent of the coarse space.
The old local/local rank pruning still runs separately. This detects individual
coarse-contained functions, not every possible dependence involving combinations
of multiple coarse and local functions.

`projectionPinnedModes` counts the constraints in the compiled layout; they are
active only for projection. `projectionPinnedResidualEstimateC` observes the
omitted **original** Gram equations after convergence: for each constrained row
it computes `abs(b_i - (M*c)_i) / M_ii * shapeMaximum_i`, then reports the maximum
in °C. Here `b_i` is the original projection RHS, `M` the unchanged consistent
mass and `c` the complete resulting coefficient vector. This diagnostic does not
change acceptance and is not a certified whole-field error bound.

Ordinary launches now reuse rank-input matrices. Explicit process
`FH_CONTINUOUS_AIR_RANK_INPUT_REUSE=false` retains the earlier second integration
for comparisons. When rank pruning removes functions, the second volume compilation
matches against the first compilation of that same candidate. Retained functions
are a subset in unchanged geometry/mixing, so `AirOperatorFragment` copies the
already integrated entries and remaps coefficient addresses using its existing
matching rules. Projection still consumes the last accepted layout and state.
`rankInputMatricesReused=true` identifies this path; final volume-entry reuse
counts then refer to the unpruned candidate, rather than the installed old layout.
This optimization also applies to baseline stopping and does not relax the rank
threshold, quadrature or thermal equations.

The enabled backend uses the existing dimension worker, source ledger, Page capture,
phase controller and query publication. It creates no additional executor or world sampler.
Dense coarse/local volume assembly can borrow a helper from the same shared pool,
while the owning dimension worker assembles the other disjoint fragment indices.
The default scalar backend is described in [runtime architecture](thermal-runtime-architecture-and-optimization.md).

Installed regional Air coefficients, material enthalpy H (J), phase branch and waiting
state belong to `ThermalRegion.Accepted`. `ContinuousAirSolver` holds reusable numerical
work arrays and restores them from that accepted frame after cancellation. Material
migration and phase decisions read through `MaterialStateAccess`; staged initialization
and nonregional backends still use `ThermalCellArena`. Air arena values are derived component
means for existing topology/residency infrastructure; live continuous queries evaluate
the spatial coefficients. The main thread reads `QueryPublication`: global paths use
double buffers; regional paths use immutable snapshots with actual regional timestamps.

## Explicit local material candidate

`FH_CONTINUOUS_AIR_MATERIAL_TRIAL=local` selects `LocalMaterialBoundary` for an
explicit process comparison. An absent variable retains the coupled implementation.
The candidate keeps the coarse/local Air basis, double state, source W/J and phase
laws. Only Air coefficients enter PCG. Material H/branch remain separately stored;
the existing source-address vectors also retain material addresses. Reducing solve
rows does not remove every material record or make all retained vectors Air-only.

All quadrature contacts of one material share a finite-capacity response. For an
ordinary sensible segment, `D = C + dt * sum(G)` and
`Tm = (C * Told + dt * P + dt * sum(G * Tair)) / D`, where C is J/K, dt is seconds,
G is W/K, P is direct material power W, and temperatures use the same reference
offset. Substituting this response into Air keeps its feedback within the implicit
step. `subtractResponse` applies the resulting negative rank-one correction without
assembling a dense matrix across all faces. Air surface energy loss and the material
H gain use the same exchange. A latent segment instead fixes its plateau temperature
while accumulating H; waiting bodies pause exchange and return unaccepted source J.

Material-to-material execution moves outside Air PCG. Private forward/reverse analytic
pair sweeps precede and follow the Air step. They preserve opposite pair energy and
use material branches/phase boundaries, but this splitting is not the former fully
coupled backward-Euler time trajectory. Failed Air/phase trials discard private H,
branches and waiting state; only an accepted cut commits the state (regional snapshot
or nonregional arena). Save formats are
unchanged. Thick-wall curves, direct material inputs and phase timing still require
their real production comparisons; a room probe pass is not whole-field validation.

The separate explicit `FH_CONTINUOUS_AIR_MATERIAL_SURFACE_REUSE_TRIAL=true` enables
`appendUnchangedSurfaces`. It remains off after the first realtime trial missed
geometry recovery deadlines, despite high cache reuse. The current implementation
checks each component's basis once per rebuild; its revised performance is unverified.
It reuses prior quadrature weights only for the same physical
material block, unchanged face G and exact neighboring `sameLocalBasis` geometry.
It remaps compact Air addresses and verifies the complete body before copying.
Ambiguous shared-row remaps and routed bodies use normal assembly. Temperature
changes do not invalidate these static weights. Existing nominal-contact graph delta
reuse remains in use; this does not implement independent regional scheduling.

`materialTrial`, `airSolveUnknowns`, `localMaterials`, `surfaceContacts`,
`surfaceCouplingEntries`, `materialInternalUs`, `materialResponseUs`,
`materialPairUpdates`, `reusedMaterialSurfaceBodies` and
`rebuiltMaterialSurfaceBodies` distinguish solve size, local work and rebuild reuse.
Candidate/old response arrays are included in `recordBacking`; internal execution
payload remains owned by the topology budget, with only new reference arrays counted
by Air. Neither this candidate nor the optional capacity tier changes backend default.

`FH_CONTINUOUS_AIR_MATERIAL_SURFACE_OPERATOR_TRIAL=matrix_free` is a separate
local-material implementation comparison. Material surface traces contribute
`dt * G * w * (w^T * c)` directly during `CoupledThermalOperator.apply`, followed
by the same finite-capacity material response. These surfaces no longer expand
every pair of basis coefficients into the transport graph. Air volume matrices,
projection, other routed contacts and natural boundaries retain their existing
implementation. A separate `matrixFreeBaseDiagonal` keeps volume/other-contact
diagonal contributions distinct from the directly applied positive surface block.
The iteration/acceptance factors use the positive surface diagonal with the
assembled volume/other-contact off-diagonal entries; their approximation and
iteration path change. Double rounding and finite stopping can therefore change
observed trajectories, although the physical surface bilinear form is retained.
All new flags, base diagonal and weight scratch are budgeted by `recordBacking`.
Absent selection retains assembled surfaces. This is not the proposed eight-point
Air volume operator or a switch to free unit-grid state.

The first actual room comparison completed 105 coherent publications; matrix-free
versus local assembled near-Air maximum observed difference was 3.975e-6 °C,
floor difference zero, and all 66 cooling observations progressed. The subsequent
128-source realtime trial failed freshness: total transport iterations5386 versus
678 in the first local assembled reference, solve P95 778.037ms and full cut
P95/P99 912.625/1143.246ms. Its weaker assembled auxiliary factors and repeated
surface application erased the rebuild savings. This implementation is rejected
for adoption; two-base testing of it was not pursued. The optional trial remains
available to reproduce the recorded tradeoff, not as a supported optimization.

`FH_CONTINUOUS_AIR_CAPACITY_TRIAL=two_bases` is a separate process capacity comparison:
root and Air budget ceilings 256 MiB, query ceiling 32 MiB, arena slots 262144,
live cells 131072, pair operations 524288 and boundary operations 131072.
Ordinary limits remain 128 MiB root, 16 MiB query, 131072 slots, 65536 live,
262144 pairs and 65536 boundaries. Increasing ceilings is not CPU optimization.
`ContinuousAirTwoBaseProductionGameTests` uses two real 128-campfire bases in one
dimension, separated by 160 blocks, 16 probes and 100 FakePlayer environment
consumers. `FH_CONTINUOUS_AIR_TWO_BASE_TICKS` selects 600–2400 test ticks; a short
capacity screen omits later lifecycle events. These consumers are not 100 connected
clients. Real-time deadlines must be checked with `realtime20`, independently of
the capacity tier and lockstep screening.

`FH_CONTINUOUS_AIR_TWO_BASE_ACTIONS=A_only` makes only base A perform the scheduled
passage/source changes; B keeps normal burning and environment queries. The default
remains `both_bases`. The runner exposes `-TwoBaseActions A_only` and restores the
prior environment. Short runs execute each action when its actual tick is reached,
so a 1200-tick window includes passage close/open but omits later source actions.
This fixture supports future regional-reuse verification; its addition is not a
regional runtime or a new performance acceptance result.

The recorded two-base realtime assembled-surface comparison failed both tiers:
standard root128 accumulated37 WORK_LIMITED with a127.314MiB admitted peak;
expanded root256 removed refusals, peak146.518MiB, but full cut P95/P99 remained
1372.256/1833.500ms and probe age/geometry recovered too slowly. It does not qualify
256-source realtime support. The fixture measures consecutive mismatch per probe,
not one dimension's latest invalidation timestamp, and verifies256 sources actually
offered enabled through `PhysicalSourceSpatialIndex.enabledSourceCount` after the
final reignition. 100 FakePlayer environment queries still do not certify clients.

`FH_CONTINUOUS_AIR_REGION_DIAGNOSTICS=true` inspects `ThermalRegionPartition`
during explicit rebuild comparisons. It groups shared Air coefficients/volumes,
surface material state, material-material edges and potential routed contacts.
Waiting state and a current zero heat rate do not remove connections. Diagnostics
include `airRegionsBeforeMaterialConduction`, `airRegions`, `materialOnlyRegions`,
largest region sizes and per-region Air/material counts with physical X bounds.
The observer does not split scheduling, state or publication. Its union/find
workspace is transient diagnostic allocation; it is not default runtime work or
evidence that regional capacity/CPU has passed.

## Real-clock and accelerated Forge validation

`ContinuousAirGameTestPacing` runs only on `GameTestServer` when continuous Air is
enabled. Its default is `realtime20`: each server tick START is paced to at least
50 ms from the preceding START. Normal `runClient` and dedicated-server clocks
are unaffected by this GameTest subscriber.

The explicit process variable `FH_CONTINUOUS_AIR_TEST_CLOCK=lockstep` removes that
50 ms parking for behavior comparisons. Before the next game tick starts, it calls
the package-local `MinecraftThermalInput.awaitSubmittedCut` for active levels.
This waits for the already submitted immutable cut through the existing mailbox
completion mechanism and drains/ACKs its original completion. A pool-rejected,
already sealed pending batch is retried until accepted. The barrier never seals
new input and never waits for `geometry_current`, which can require future ticks.
Actual main-thread capture, source/item/fuel events, async worker, publication,
phase requests, every Minecraft tick and 20-tick cut still run. Thermal integration
uses the original simulated dt; wall time and simulated time are distinct.

Lockstep is a behavior/reconstruction test clock, not a server throughput test.
Blocking ticks on worker completion intentionally prevents wall-clock backlog,
so a lockstep freshness pass cannot certify the ordinary 40-tick deadline or
100-player real-time capacity. It also makes completion/ACK scheduling more
synchronous; capture, residency and subsequent layout histories can change.
Compare actual saved climate, source actions and sampleTick-aligned curves;
preserved ticks alone do not prove identical temperatures for every scene.
The first 2026-10-05 room comparison passed with identical sampled warming/cooling,
but the legacy-baseline accelerated 128-source endurance attempt failed ordinary
unpowered transport after extinguishing all sources. With the gameplay stopping
policy, a later 12000-tick accelerated run completed all 600 cuts and three source/
passage cycles without worker failure. That validates the recorded lifecycle
trajectory, not realtime throughput or identical accelerated/realtime curves:
their sparse observations differed by up to 0.1542 °C. The separate gameplay
realtime short run's 80 same-publication probe observations differed from its
historical realtime reference by at most 6.829e-7 °C. Complete scope and failures
are recorded in the results document. Large-base capacity still requires realtime20.

`THERMAL_TEST_CLOCK_START` and `THERMAL_TEST_CLOCK_RESULT` log `mode`, planned or
elapsed tick count, `wallMs`, `simulatedMs` (`50 * elapsedTicks`) and lockstep
`barrierWaitMs`. The timing observer still records completed targetTick sequences
and cut CPU times. START-barrier wait is excluded from `lastProcessingNanos`;
accelerated `previousTickProcessingUs` is not real-server MSPT evidence.

Use lockstep for initial curve, source lifecycle and geometry comparisons; use
short realtime20 windows for cut latency and freshness, and full 12000-tick
realtime20 endurance at retained-candidate milestones. The real-world
[clock/curve results](continuous-air-large-base-optimization.md) record the
validated scope and measured compression. No JUnit, artificial thermal states
or larger heat time steps are needed for this workflow.

## Spatial representation

### Architecture selection and the retired two-level trial

2026-10-24 source update: configured `SHARED_GRID` selects the unit-grid/local/pairwise runtime. Explicit `FH_CONTINUOUS_AIR_TRIAL` retains `baseline` and `fine_grid` for the existing backend and numerical reference. The slower `two_level` experiment, `TwoLevelThermalPreconditioner`, its private workspace and all operator branches have been removed. The dated description below records that former comparison; it is not an available runtime option. Current production behavior and validation are owned by [thermal-runtime-control.md](thermal-runtime-control.md).

During the original architecture comparison the ordinary spatial backend was the coarse/local representation below.
The server-process environment variable `FH_CONTINUOUS_AIR_TRIAL` then selected
`baseline`, `two_level`, or `fine_grid`.
`ThermalArchitectureTrial.SELECTED` captures this choice once in the JVM; it does not
hot-switch an installed field. These are comparison prototypes, not default-enable
certification. `continuousAir` must still be enabled independently.

The retired `two_level` retained the physical operator, RHS, source timing, spatial coefficients,
query representation and checkpoints. `TwoLevelThermalPreconditioner` used graph-connected
32-mode cores with bounded coarse/local/material halos, weighted local blocks and
a coarse-space IC correction. Its balanced form applies
`C + (I-C*A)*L*(I-A*C)`, where A is the actual current symmetric system, C is the
approximate coarse correction and L the local/material correction. Block regularization
affects only the preconditioner. Original P32 correction estimation and true-residual
acceptance remain independent; projection retains its existing Air IC path. Factor,
halo, scratch and coarse arrays are included in `recordBacking`. The first additive
prototype failed offset128 freshness; the balanced prototype passed that window but
was slower than baseline. These observations do not establish benefits for all loads.

`fine_grid` uses `SharedAirGridCompiler` to build unit-cell trilinear nodes in captured
actual Air. Node identities join across actual open unit faces; solid walls and
unconnected vertex branches remain separate. Page/Brick capture and arena ownership
remain unchanged. The fine grid covers the captured Air; this prototype does not yet
have a near-fine/far-coarse adaptive interface. Positive outlets request the existing
connected R6 support through `AirShapeStore.prepareSharedGrid`, with no source-specific
temperature modes. Residual field heat retains source support until the observed
field deviation is small.

`AirOperatorFragment.unitGridVolumes` shares two immutable unit-element matrix templates
for ordinary/enhanced mixing and retains the original four-block lower/upper buoyancy
sampling scale. Physical capacities, conductances, source W/J and material laws are
unchanged. Fine-grid transport uses lumped nodal Air capacity and the existing
nonlinear material/buoyancy trials; projection uses its consistent unit-element mass.
Consequently spatial approximation and capacity discretization differ from the
coarse/local representation: unchanged coefficients do not guarantee identical
temperature curves or phase timing. Transport has no local-mode blocks and uses
nodal/material Jacobi in the established P32 path; projection uses Air IC.

Queries interpolate eight unit-cell nodes at the exact requested position.
`airBasisTerms=8` therefore represents a fine continuous field in this trial; production
checks retain gradients, cross-face continuity, current geometry and 40-tick freshness
instead of requiring the old source-mode count. `AirFieldCheckpoint` adds per-component
`unitCorners`/`unitValues` double-value data to the existing root-version-5 container;
the current codec samples and merges it without storing runtime identities.
Prototype save/reload must be verified using this codec. Compatibility with older
released readers and migration of arbitrary existing worlds are not certified.

The trial uses the same 128 MiB root budget and existing cell/query-age limits.
Root charged peaks still do not certify whole-JVM heap or every transient allocation.
The recorded comparison, complete-window statistics, actual temperature differences
and cumulative-capacity failures are owned by
[architecture comparison](continuous-air-architecture-comparison.md). Neither trial
is selected as the ordinary backend: balanced two-level loses dense/offset performance,
while unit-grid passes the short128 windows but repeatedly refuses cumulative
room/material changes under the unchanged budget.

An actual connected Air component in a 4×4×4 Brick uses eight trilinear coarse functions.
`AirFieldCompiler` shares coefficient identities across open neighboring Brick faces;
solid walls do not create connections. Page dependencies follow the shared functions.
`AirFieldLayout.Component` owns coefficient indices, Air block masks and geometry dependencies.
Positions use world coordinates, with floor-based indexing for negative coordinates.

Real positive-power `AIR_FACE` outlets also request two local shapes with scales 1 and 3,
within `LocalAirShape.RADIUS = 6` blocks. `AirShapeStore` prepares dimensionless static
screened-diffusion shapes on captured geometry, with separate vertex branches where
Air is disconnected. Missing support Bricks are requested through normal worker residency.
A live-source response is compiled only when its support is captured. Restored fragments
can represent the captured part while requesting missing support; their checkpoints are
not consumed until that support is complete and the candidate projection is committed.
Routed sources use the actual
nearest Air outlet face found by `AirRouteCompiler`, rather than the ventilated material's position.

Temperature is `T(x) = Tref + Σ qi φi(x)`, where T, Tref and qi are in °C and φi is dimensionless.
The same functions supply volume integration, face contacts, source loads, queries and checkpoints.
Air capacity defaults to `airHeatCapacityJPerBlockK = 1200 J/(block·K)` and mixing to
`airMixingWPerBlockK = 96 W/(block·K)`; both are runtime configuration values.
Shapes and preintegrated volume arrays are reused when their local Air geometry and mixing mask match.
`AirFieldLayout.Component.matchingBasis` also identifies surviving functions when other
local functions change. `AirOperatorFragment` copies their already integrated mass
entries on unchanged Air volume, and their horizontal/vertical entries when the mixing
mask is also unchanged. New or changed pairs retain the original quadrature and
accumulation order. This saves repeated integration; a partially changed fragment
still owns newly allocated triangular arrays. It is not a sparse storage format or
a change to the represented functions.
Volume quadrature already enumerates actual Air cells. `weightAtAirCell` and
`derivativeAtAirCell` reuse that known cell for all basis values and derivatives at
the same point. Point loads, contact traces and temperature samples resolve the
cell once per point while retaining the original boundary-side selection and
coarse/local arithmetic. These paths do not cache temperatures or new geometry;
ordinary `weight` and `derivative` still resolve a cell for standalone callers.
For changed overlap groups, after their volumes are compiled, `LocalAirModeRank` checks the integrated local-mode
mass Gram matrix across connected overlapping supports. A local function whose squared
orthogonal residual is at most `1e-6` of its own mass norm is omitted and the layout is
compiled again. This extends the same relative dependence criterion beyond the two
scales of one footprint. It does not test dependence on coarse functions or place a
hard cap on the number of independent overlapping modes. Unchanged groups reuse
accepted selections through `LocalAirBasisCache` before volume integration.

When there are at least 128 local shapes and 128 volumes, `compileVolumes` assigns
alternating volume indices to the owning worker and one `ThermalAssemblyTask` helper.
Each uses its own `AirMixingRegion` and writes only its assigned output entries;
candidate geometry, old volume arrays and source bindings are read-frozen for this
assembly phase. Rank analysis and global assembly retain their sequential volume
order. This parallelizes matrix preparation, not independent thermal systems.
The existing `ThermalWorkerPool` still has at most four threads and a 64-task queue;
helpers also use that queue. If a helper has not started when the parent finishes
its half, the parent claims it inline. A full/closing queue therefore needs no extra
thread or queued nested wait. Both jobs finish, including failures, before projection,
rollback or source mutation resumes. Completed tasks clear their work closures so
a delayed queued no-op does not retain old/candidate layout or source backing.
`parallelVolumeAssemblies` counts assemblies where a helper actually ran elsewhere;
queue admission alone is not counted as parallel execution. Unit-grid trial assembly
uses its existing shared templates and does not enter this helper path.

## Source and material advancement

`ThermalSourceLedger.acceptAndRecord` settles source event times into a reusable
`CutLoadBuffer`. Nonempty spans carry W; equal-endpoint impulses carry signed J.
One `AIR_STENCIL` binding refers to one `AirLoadTable` entry, irrespective of basis count.
The numerical load scatters its spatial weights; delivery is counted once for the physical port.
`SourceBinding` reserves bit 31 of the target index to distinguish Air-load indices from material slots.

`ContinuousAirSolver.solveCut` splits at event timestamps and at most 20 ticks (1 game second).
It advances Air and materials together by backward Euler. `CoupledThermalOperator` stores
packed volume arrays and flat contact weights. For transport it aggregates non-diagonal
volume pairs into a sparse edge graph at rebuild time; each material/buoyancy trial
refreshes edge values and PCG matvec visits that graph. Volume edge positions are
precomputed during the rebuild. A contact with trace weights `w` and conductance
`G` contributes `dt * G * w(w^T x)`. Here `dt` is seconds, `G` is W/K, `w` is
dimensionless, `x` is a vector of temperature offsets in °C, and the resulting
vector is in J. Contacts are not expanded into a quadratic array of pair edge
positions. Instead, their ordinary and wind-scaled off-diagonal weights are
preaggregated into the sparse graph at rebuild. Each trial computes the active
contact diagonal, skips graph pairs touching fixed-temperature rows, and applies
only changed-contact off-diagonal corrections as
`dt * (Gactive - Gnominal) * wi * (sum(j free, wj*xj) - wi*xi)` for each free entry
`i`. Fixed-temperature entries are supplied through the right-hand side. This
uses double precision and no quadratic retained contact-position array. The full
active contact diagonal remains in the PCG preconditioner. Coarse/coarse
transport mass is lumped; cross terms involving a local mode remain. Topology
projection uses the full Air mass matrix instead. Its material rows have no
Air/material cross terms, so `PcgSolver` solves only the Air unknowns during
projection and leaves already migrated material state untouched. Projection
off-diagonals are preaggregated over global Air coefficient pairs and retained
as a compact, independently addressed `projectionFirst` / `projectionSecond` /
`projectionOffDiagonal` graph. It has no material entries and does not retain a
dense value array sized to the coupled transport graph. Fresh assembly preserves
the previous order: sorted transport-supported mass pairs, then sorted extra
mass pairs. Only exactly zero ordinary mass pairs are omitted, as before; extra
pairs retain the previous structural treatment. All arrays are charged by
`AirBackingMeter`; the volume matrices remain
the numeric source for assembly and the block preconditioner.

During rebuild, `CoupledThermalOperator.sameContactSupport` compares the ordered
coefficient addresses of consecutive nonzero-conductance contacts. Equal supports
share clique discovery and pair-index lookup, including the four quadrature contacts
on a material face when their supports match. Each contact's own weights and
conductance are still accumulated in the original order. The constructor retains
only one temporary pair-position scratch, grown to the widest encountered contact;
it does not keep a quadratic array per contact or merge their physical contributions.
The ordinary backend stores nominal wind-boundary off-diagonals as exact nonzero
edge/value pairs; nominal static contacts retain their dense array. Each trial still
combines `static + windScale * wind` in the same order, including zero wind values,
before adding it to the volume contributions. No small nonzero entry is pruned.
`ContinuousAirSolver.lastRebuildTiming` reports `materialIndexMs`, `contactsMs` and
`graphMs` separately inside the inclusive `operatorMs` stage.

Across rebuilds, `CoupledThermalOperator` can share an unchanged transport edge graph
and volume edge positions after checking coefficient addresses and exact nonzero
structure. Nominal contact aggregates additionally require identical ordered weights,
conductances and boundary classification. `sameProjectionMass` independently checks
Air/coarse counts, ordered volume coefficient addresses and exact double mass
entries. Equal Air mass arrays and a valid projection factor can therefore be shared
even when material count, contact structure or transport edge numbering changes.
The shared projection factor is never refactored: its application writes only the
caller's correction vector. The projection RHS, migrated material state and P32
acceptance blocks are still prepared for the current candidate. Active conductance,
buoyancy, trial diagonals and transport values remain candidate-owned. The broad
transport-graph conditions did not hit in the first large-base runs; independently
reusing Air projection is a narrower optimization, not general incremental transport
assembly.

Ordinary `BASELINE` rebuilds can also use
`CoupledThermalOperator.buildTransportDelta`. It maps old compact rows to current
rows using surviving coarse corners, source block/face/branch/scale identities,
and sorted material arena slots. The mapping is one-to-one; ambiguous corner
merges/splits do not alias old rows. Accepted temperatures, material H and branches
are still migrated by the existing solver/topology path, never by this matrix cache.

The cache compares each contact's ordered mapped rows, exact double weights,
nominal conductance and static/wind classification. Hash lookup is followed by
exact comparison, including duplicate-contact multiplicity. Old immutable nominal
edge values are remapped, removed/changed contributions subtracted, and new
contributions added. Current volume values, active contacts, fixed rows, RHS,
buoyancy, P32 acceptance and true-residual checks still refresh normally. Surviving
volume edge-position arrays may be shared even when global row numbers move.
Mapped old edges can temporarily retain zero structure; broad changes or up to
32 contact-changing incremental preparations return to fresh aggregation.
Ill-scaled Air, unit-grid and two-level trials use the existing full path.

This preserves the represented functions and physical equation, with changed
floating-point accumulation/order; it is not a bit-identical replay. No small
nonzero coefficient is intentionally pruned. The compact Air preconditioner edge
selection is sorted by current pair keys even when an exactly shared global graph
inherits a preceding delta's order. This is required by its binary pair lookup.
`Contacts.term` keeps the original insertion and addition order, but after 16
terms uses one reusable primitive index instead of repeated linear duplicate scans.
This reduces dense-overlap contact compilation without merging source positions.

`assemblyReuseSummary` reports `transportReuseFailure`,
`incrementalTransportGraph`, `reusedContactPairs`, `changedContactPairs`,
`remappedRows`, `reusedGraphEdges` and `contactDeltaDepth`. Pair counters describe
constructor work, not source-energy delivery or total matvec work. Contacts and
the enriched layout are still compiled as a whole; this stage is incremental
graph/value preparation, not complete local topology/contact compilation.
Retained arrays remain identity-metered, including the already solver-owned
material slot array. Matching/remapping scratch is disposable inside the existing
preparation reservation; complete transient/JVM-heap certification remains open.

`lastRebuildTiming` includes `preparedRebuilds`, `preparedProjectionMassReuses` and
`preparedProjectionFactorReuses`. These count successfully projected preparations,
including a preparation later refused by budget admission; they are not committed-cut
counts. `projectionPayloadBytes` and `projectionDenseEquivalentPayloadBytes` compare
the current Air storage with the former dense storage for the same candidate graph.
`airTransportPayloadBytes` and `airTransportDenseEquivalentPayloadBytes` do the same
for the exceptional Air transport workspace below. They exclude array headers and
factor backing; the actual identity meter includes retained arrays and headers.
`transportAssemblyUs`, `projectionAssemblyUs` and `airTransportPatternUs` split the
operator-constructor work without changing the inclusive `graphMs` convention.

`ContinuousAirSolver.reserveRebuildScratch` shares disposable candidate/RHS/power/rate
and candidate-enthalpy vectors with the paused previous solver. It grows them with
one-eighth headroom, at least 256 entries, and initializes the projection RHS before
assembling it. Accepted `stateC`, `materialH` and material branches remain separate,
and old query/checkpoint arrays are not reused as workspace. Rollback restores the
accepted arrays; subsequent trials overwrite their scratch before use. The meter
counts shared array identity once while retaining old and candidate states.

`CoupledThermalOperator.workspace` also borrows disposable backing from the paused
previous operator: trial diagonals, fixed temperature offsets, buoyancy factors,
active contact conductances/boundary offsets, changed-contact indices, transport
edge values, Air transport edge values and same-size P32 block matrices. It adds
one-eighth headroom (at least 256 entries) on growth; empty ranges release their
backing. Matrix/contact loops use current `size()`, `conductanceWPerK.length`,
`transportFirst.length` and `airTransportEdges.length`, rather than spare capacity.
`prepareProjection`, `prepareMass`, `refreshContacts`, `prepareSparseTransport`
and `prepareModeBlocks` overwrite the corresponding active ranges before use.
The next transport trial recomputes all buoyancy factors. Abort restores the old
accepted arrays/operator; the next solve prepares its work arrays again. Accepted
coefficients/H/branches, material masses, waiting/fixed flags, immutable volume
matrices, nominal contacts, sparse graphs and query/checkpoint arrays are not
borrowed by this mechanism. There is one active solver per dimension; parallel
volume assembly reads immutable fragments, never these borrowed work arrays.
`reusedWorkspaceBytes` is identity-meter backing shared with the previous operator,
not a change in physical coefficients or a whole-JVM heap measurement.

`ContactWeightTable` stores contact weights in immutable 4096-entry blocks. It
hashes raw double bits to build a palette, retaining double values and unsigned
short palette indices only when both aligned arrays are smaller than a dense
double block. Other blocks keep dense doubles. A block with the same length and
ordered raw double values as the previous operator shares its backing, independently
of coefficient addresses. Reading a palette returns the exact original double;
it does not convert a weight to float, round it, delete small values or change
contact order. Nominal-contact equality retains the former `Arrays.equals(double[])`
bit comparison semantics. Ordinary, incremental and two-level trial assembly all
read the same table. The identity meter includes both outer arrays and all retained
palette/code/dense arrays. The temporary 4096-entry palette builder remains in the
unchanged preparation-reservation scope. `contactWeightsPayloadBytes` and
`contactWeightsDensePayloadBytes` compare logical numeric payload for this candidate;
`contactWeightDictionaryBlocks` and `contactWeightSharedBlocks` report compression
and cross-rebuild sharing, separately. Solver/array headers remain in actual backing
accounting. Extra lookup work must be judged by whole production cuts, not just
the smaller payload or iteration count.

`ThermalDimensionEngine.reportAirPreparationBacking` logs
`THERMAL_AIR_PREPARATION_MEMORY` for admitted Air-budget growth snapshots at or above
80 MiB, separated by at least 2 MiB, and for refused preparations. The report
separates root/current Air-budget charges, the full preparation reservation,
candidate/previous retained data, source cut tape and query-held layout contributions.
`airBudgetBytes` covers this engine's Air budget child; query/radiation children and
other dimensions also contribute to `rootBytes`/`rootOtherReservedBytes`.
`rootPeakBytes` includes admitted intermediate reserve/resize peaks. These snapshots
are not every rebuild or a complete transient-allocation/JVM-heap audit. The
preparation-reservation formula and the 128 MiB shared root cap remain unchanged.
Array backing is deduplicated by identity, while container entries use conservative
estimates. A small `queryOnlyLayoutBytes` charge can therefore remain for a layout's
container estimate even when its arrays were already counted with the solver.

Contact activity/environment are refreshed once per material trial; diagonal/preconditioner
preparation then runs once per buoyancy iteration, after the material branches are selected.
`materialContactRates` gathers the same contact temperatures but scatters only the material
entries consumed by H/branch trials. Transport matvec uses the graph plus linear
corrections for paused/rerouted contacts; the material rate path reads the packed contacts. Thermal
parameters are unchanged; configurable stopping precision is described below.

For transport, `prepareSparseTransport` now runs before `prepareModeBlocks`. The
32-mode blocks read the same assembled off-diagonal graph and apply the same
active-minus-nominal corrections for paused/rerouted contacts. Projection and mass
blocks read the assembled projection graph, including extra pairs. This removes
the second traversal of packed volume matrices and ordinary contact pairs used
solely to assemble preconditioner blocks. The block diagonal, partition, Cholesky
factorization and physical matvec are unchanged; no additional matrix arrays are
allocated. Floating-point summation order can differ, so real regression results
and the unchanged recomputed-residual acceptance still determine validity.

`PcgSolver` reuses primitive work arrays and warm starts. Ordinary transport and impulses use
scalar Jacobi for coarse/material unknowns and disjoint local-mode Cholesky blocks
of up to 32 modes. Air topology projection uses `SparseThermalPreconditioner`,
a normalized incomplete Cholesky factor built only over nonzero Air mass
pairs. It tries preconditioner diagonal shifts `0, 1e-6, 1e-4, 1e-2, 0.1, 1` to obtain
positive finite pivots, falling back to the original blocks if none succeeds. These
shifts change the iteration preconditioner only; the physical projection mass matrix,
matvec and right-hand side stay unchanged. The factor arrays are included in
`AirBackingMeter`. Applying that factor to the full transport graph was rejected after
the actual offset128 trial showed expensive repeated factorization and inadequate freshness.

For a layout with at most 4096 Air unknowns, either a positive mass-diagonal minimum
below `1e-12` times its maximum or at least 128 retained local shapes selects the
Air transport factor. Fill is capped at 131072 extra entries. An ill-scaled projection
starts with level-two fill and reverse Cuthill–McKee factor ordering;
the physical coefficient addresses and matrix stay unchanged. This handles very
small residual-shape scales observed after closing a real passage. The retained-local
shape condition also covers dense ordinary transport layouts with expensive PCG
trials observed in the 2026-10-04 large-base profiling run. This is a preconditioner
selection rule, not a source/mode cap or a change to thermal capacity. For the same
layouts, transport builds a separate factor over the current
Air principal block, including active/waiting/routed contact corrections, and keeps
material Jacobi. Transport uses level-two fill with reverse Cuthill–McKee ordering
when `illScaledAir` is true, and level-one fill without that permutation for ordinary
dense layouts. The stronger pattern is built during candidate preparation and is
included in retained-backing admission, rather than allocated during a failed live
solve. A real all-sources-extinguished run with the former level-one transport factor
exhausted 2048 iterations at an approximately `2.2e-8 °C` correction estimate while
the unchanged unpowered target was `1e-9 °C`; subsequent dt reductions reached
`5.82e-11 s` and the worker failed. A mass-dominated tiny-dt equation still contains
the same badly scaled Air mass block. Selecting the stronger iteration aid keeps
the physical matrix/RHS, timestep policy, independent P32 acceptance and precision
settings. Qualification of a candidate is owned by the actual production results;
this selection rule alone is not evidence that every such layout will converge.
Its numeric edge workspace is packed in the exact `airTransportEdges` selection
order; `SparseThermalPreconditioner` stores source addresses into that packed array.
Current transport coefficients and active-minus-nominal corrections are copied and
accumulated in the original order. Material edge slots are not allocated in this Air
workspace. `beginTransportStep` requests a fresh factor for the first nonlinear trial
of each dt/material substep. Later trials in that same substep can reuse the valid
SPD factor as an iteration aid. They still refresh the physical matrix, RHS, contact
activity, material rows and original P32 acceptance blocks. A new/retried substep
refreshes the factor again. It stays fixed within each PCG solve and does not factor
the whole material graph. `airTransportFactorPrepares` / `airTransportFactorReuses`
are cumulative for that operator. The original P32/Jacobi
acceptance and recomputed residual remain independent. Directly using the projection
factor for transport failed the actual large-base trial and is not retained.
All retained factor, edge and workspace arrays are included in `recordBacking` and
candidate admission. This does not close every existing temporary pre-admission gap.

When a transport trial uses a reused Air factor, its first PCG attempt gets up to
64 iterations. On failure, the solver can refactor the current Air block, reset the
initial iterate to accepted substep state and retry the same equation using the
remaining original iteration budget. `refreshedTransportRetries` counts these retries
per cut. The refreshed factor remains fixed during the retry; the physical matrix,
RHS, tolerance and P32 acceptance do not change. Unused-budget restart does not
increase the original `max(64, min(2048, 2 * solveSize))` cap.

If material substep halving reaches the existing `1e-10` second floor, diagnostics
now report `rejectionKind`: 1 is a linear-solve rejection, 2 a material energy-limit
crossing, 3 exhausted outer iterations with branch changes, and 4 exhausted outer
iterations with buoyancy-factor changes. The diagnostic adds dt, outer index,
material slot/H/energy/limit where relevant, factor change and linear failure text.
Ordinary endpoint crossings still follow the original substep adjustment; logging
does not change the phase rounding or acceptance rules.

Ordinary dense projection initially uses the cheaper level-zero factor, or a valid
factor shared with exactly equal mass arrays. If there are at least 128 local shapes,
at most 4096 Air unknowns and the factor has not reached level two, the first attempt
gets up to 64 iterations. On nonconvergence, `strengthenProjection` allocates a private
level-two/RCM factor and restarts PCG from the current candidate iterate and the true
residual of the same mass equation. The combined iteration budget stays
`max(64, min(2048, 2 * airUnknownCount))`; it is not enlarged for the second attempt.
The same tolerance, original P32 correction estimate and fresh-residual acceptance
apply to both attempts. `projectionIters` includes both attempts and
`projectionStrengthened` reports the upgrade. Shared old factors are never modified
by escalation. Factors, including an upgrade, are built before candidate admission.

Projection acceptance still uses the original 32-mode/Jacobi correction estimate,
independently of the iteration factor. Ordinary transport reuses its already computed
block correction; transport using an alternative factor independently computes the
same original correction. The selected threshold is in °C of represented
temperature, with a recomputed true residual before acceptance and another check
after the last permitted update at the iteration cap. `AirFieldLayout.Component`
bounds the spatial correction by the largest coarse-corner correction plus each local
coefficient correction times its maximum shape value; material corrections use their
direct temperature coefficients. This avoids treating a nearly zero local shape's
coefficient as a full degree of temperature. The stopping criterion remains an
iterative correction estimate, not a certified physical temperature-error bound.
The threshold is `1e-5 °C` by source default for powered transport/projection and
at most `1e-9 °C` for unpowered transport/impulses. Power is checked across the actual
scattered load vector; equal total positive/negative W at different locations do not
make a driven field count as unpowered.
Buoyancy uses an outer iteration. Material trials keep candidate H/branch separate from
accepted state. A material segment crossing rejects the coupled substep and reduces its duration;
only endpoint rounding of `PHASE_ENDPOINT_ROUNDING_C = 1e-9 °C` times
`max(1 J/K, material capacity in J/K)` is canonicalized. This energy allowance in J
is independent of the configurable linear-solve stopping threshold. Latent and waiting materials use
fixed-temperature rows, with actual contact energy updating latent H.

The engine settles the previous geometry's source interval, commits the accepted state,
and processes ACK/environment updates. For a topology change, `TopologyUpdatePlanner` stages
candidate cells; `ContinuousAirSolver.prepareRebuild` compiles and projects candidate Air
through `AirGeometryView` before `TopologyCommitter.commit`. `ContinuousAirSolver.prepareQueryCapacity`
selects global Air coefficient capacity or regional snapshot capacity;
`ThermalDimensionEngine.reserveAirBacking` reserves candidate solver backing first.
On refusal, `TopologyUpdatePlanner.discardPrepared` releases staged cells and the old topology
remains authoritative. On success, the engine commits topology, installs the prepared Air,
rebinds sources, releases old spans, collects phase requests and publishes the final cut.
Regional advances additionally publish their accepted private state before topology preparation;
the final regional snapshots include accepted material heat and refreshed phase/ACK
metadata. Explicit gameplay heat controls settle once per region before topology
preparation; they do not restore thermal values from an arena mirror. Saves use
`ReadCursor.pageSampleTick` in both `DormantChunkThermalState.capture` and
`MinecraftThermalInput.captureDormantPageWithCursor`, so a Page's actual common regional
tick is not replaced by the dimension's last publication time. Pages with mixed times
cannot yet form a single checkpoint; save/unload retains the existing whole-cut wait.
When a prepared change is empty or contains only natural-temperature Page updates, no mixing change,
and no restored source response awaiting refresh,
`PreparedTopologyChange.onlyNaturalTemperatureUpdates` keeps the existing spatial layout
and operator. Unchanged Brick bases use their already integrated projection mass matrix
to form the topology-projection right-hand side; changed bases still integrate the old
field over actual Air cells. Both paths retain the same projection solve and tolerance.
After `stableBatchesBeforeSleep = 20` quiet batches, continuous Air may sleep only if every
represented Air component and material body is within `sleepResidualC = 1e-6 °C` of its
Page natural temperature. Active/pending power, geometry, environment, wind and phase
changes wake it. Extinguished sources with residual Air or material heat therefore keep
advancing. These are current source defaults, not a performance guarantee for changing climates.
`MinecraftPhaseController` performs real world conversions and supplies the existing ACK.
The production handoff drains up to `8 * ThermalInputBatch.CUT_INTERVAL_TICKS = 160`
requests per 20-tick completion; the main thread still applies at most 8 world mutations
per tick. A matching request whose Page material query is temporarily behind a topology
publication receives `RETRY`, preserving its sequence until a coherent query is available.
Confirmed source delivery is recorded after numerical acceptance.

## Queries and persistence

Live player, block and item queries evaluate the actual requested xyz. The item quarter-position
cache retains reusable radiation work but refreshes spatial Air at exact xyz. Existing analytic
fields and direct radiation are composed afterward. `ThermalEnvironmentSample.airBasisTerms`
and `airSampleTick` expose whether a real spatial publication supplied the result; Natural fallback
has no spatial basis. Cross-Page dependencies must match the captured geometry.
Infrared continues to read material temperature.

In the global path, after `QueryPublication.publish` switches the coherent publication, it clears the
inactive buffer's `AirFieldLayout` reference under the same write version. Coefficient
and material arrays remain double buffered. The old published layout stays available
through candidate preparation; an already captured `ReadCursor` holds its own reference
and must pass its existing `isCurrent()` check. `republishUnchanged` also clears the
unused staged layout, and invalidation/close clear both layout references. Air readers
retry a missing/invalid snapshot when the publication version changed during the read.
`MinecraftThermalInput.localAirWeight`, `materialPublicationRevision` and
`captureDormantPage` clear their reusable checkpoint cursor in `finally`, including
early returns and failed captures. Dormant/infrared capture also end their existing
cursor scopes. These changes shorten ownership of obsolete layouts; query age and
geometry requirements retain their existing meaning. After a layout replacement,
`ThermalDimensionEngine.publish` re-meters retained backing to release the retired
query layout's charge immediately. Stable-layout cuts keep their existing single
backing-measurement pass.

When a normal block mutation has invalidated a Page's live revision but a replacement has
not yet been captured, `MinecraftThermalInput.sampleAir` may use a complete captured Page
publication and `QueryPublication.tryReadCapturedAir`. The requested world block must
still be Air and no full geometry resync may be pending. Fine query samples must satisfy
the normal 40-tick maximum age. Minimum gameplay Air samples may use their latest
Accepted physical time as described in [runtime control](thermal-runtime-control.md);
this does not extend the captured-geometry deadline. Each layout dependency must have the matching geometry
revision in its `lastPublication` or `previousPublication`; the latter covers the short
window in which a Page commit precedes the query-buffer switch. Every dependency whose
live revision differs from that captured revision must independently pass
`ThermalPageHandle.capturedFallbackWithin` for at most 40 game ticks. The deadline may
advance only after a strictly newer actual geometry revision has been published together
with a successful query cut; republishing coefficients for the same revision cannot renew it.

`MinecraftThermalInput.sampleMaterial` first tries the current Page publication. During
the brief interval after `ThermalPageHandle.publish` advances geometry but before
`QueryPublication` advances, it may read `previousPublication` only if the material
change journal excludes that block, its saved signature still matches the current
BlockState, and the coherent query topology is older than the replacement Page.
Stored material is still the fallback when no live publication is readable.

`DormantChunkThermalState` reads and writes only root `FrostedHeartThermal.version = 5`.
Version 4 and older thermal records are treated as missing; there is no migration reader or dual write.
The continuous path stores `AirFieldCheckpoint`: double coarse values, static local-shape Section
pieces and amplitudes, plus the capture tick/natural baseline. Runtime slots are not serialized.
Continuous checkpoints do not also write the scalar Brick preview. The scalar payload remains only
for the currently selectable scalar backend, within the same version-5 root.

Material H/branch/law/BlockState and per-record ages stay in `MaterialSectionState`.
Dormant point queries evaluate the saved field and existing natural half-life cooling.
Admission reconstructs geometry from world blocks, restores shape pieces and projects the old
field into current coefficients. Partial Section captures retain unrepresented spatial history.
This is finite-support spatial history, not a replay of unloaded-world simulation.

The 2026-10-12 checkpoint capture implementation reads each Page component from its original
regional layout and coefficient vector through `AirFieldCheckpoint.Capture`. It does not create
a combined layout or a whole-region coefficient copy to save a Page spanning multiple regions.
The existing pinned-read, lifecycle-generation, geometry and actual-sample-time checks still
govern checkpoint acceptance. Component order, doubles, the 1/16C omission threshold and
version-5 encoding are preserved.

`AirFieldCheckpoint.sampleC` uses 64 Brick heads and one next-component index per component.
The index preserves the original order within a Brick, including disconnected Air components;
the interpolation itself is unchanged. The two arrays are created on capture/decode/merge,
are included in `recordBacking`, and are not written to NBT. They speed candidate lookup;
they do not cache world topology or eliminate re-admission construction. Validation and
performance limits are recorded in
[the local checkpoint plan](../../plans/2026-10-12_checkpoint-local-capture-and-restore.md).

The final checkpoint candidate passed13 required instances in6 separate real Forge JVMs,
plus a legacy-code save for the independent-JVM compatibility chain. In the accepted
4000tick two-base workload, mean snapshot allocation fell172,889B→156,250B and P99
828,456B→513,752B relative to the late control. Snapshot duration, whole-server CPU/P99
and22tick re-admission did not establish a stable speed improvement. Heating/extinguished
recovery differences remained0.475328/0.576372C in the stated16,480-point scenes;
same-tick stored/published fields differed only by floating-point roundoff. The codec and
default backend selection are unchanged. Full evidence, unsuccessful coverage attempts
and the measurement window are in the
[checkpoint report](../../build/checkpoint-local-final-report-20261012.json).

## Validation and remaining work

`ContinuousAirProductionGameTests` uses actual blocks, fuel capabilities, flint-and-steel,
shovels, normal world ticks and public temperature/material queries. It never writes H,
constructs an engine, calls a solver, or creates synthetic ACKs. The selected namespace is
`frostedheart_production`, containing five shorter heating, spatial, reload,
slab-outlet and infrared/material-replacement scenarios. `ContinuousAirInfraredProductionGameTests`
uses real campfires and block replacement while checking decoded infrared material values.
The long powder-snow phase scenario is in `ContinuousAirPhaseProductionGameTests`, selected only
with `-PgameTestNamespaces=frostedheart_production_slow`; it is temporarily excluded from the
normal development run at the user's request. `ContinuousAirGameTestPacing` paces every
GameTest server at 20 TPS only when `continuousAir = true`; the unthrottled GameTest clock
can otherwise outrun the production worker.

Run with the GameTest common config's `continuousAir = true`:

The ordinary `frostedheart` GameTest batch passed `102/102` with `continuousAir = true`
on 2026-09-24; it has not been rerun after the 2026-09-30 operator/query changes.
The focused `frostedheart_production` batch passed `5/5` on 2026-09-30 with those changes.
Both exercise the real Forge server path under the paced GameTest clock.
The separate long production phase batch passed `1/1`: four fueled, lit campfires
advanced powder snow until its world block changed to Air after 17,244 game ticks.
`ContinuousAirCapacityProductionGameTests` uses 16 real fueled campfires, extinguishes
eight and breaks four through player actions. Its focused `frostedheart_capacity` run
passed `1/1` on 2026-09-30 with the bounded cross-Page captured-query change. It
asserts that Air remains readable after first publication, including observations
after both player actions. One earlier diagnostic run exposed a 42-tick sample age
during a simultaneous rebuild and solve; that run failed the tightened assertion.
This is one local capacity result, not a target-server latency certification.
The earlier dense 128-source refusal was traced to `contactEdgePositions`, which
stored a position for every pair of contact trace terms. In a real 128-source
diagnostic with 285 volume fragments, basis widths had P50/P95/max 40/68/80;
volume matrices were about 97.7% nonzero and only five fragments had identical
numeric templates. Each of the three candidate volume matrices was about 2.31 MB,
while contact edge positions were about 38.6 MB. This is why sharing volume
templates or sparse volume storage was not the first capacity fix. Removing the
quadratic contact positions and retaining the preaggregated graph/correction path
kept the same thermal parameters, double arrays and then-current `1e-9 °C` PCG
threshold. The later precision change is recorded in the 2026-10-03 section below.

The 128-source GameTests use real fueled campfires and fixed-position public
placed-reservoir environment queries, not 100 simultaneous player radiation queries.
The current dense 16 by 8 scenario observes until tick 2000 and starts continuous
monitoring only after tick 600 and nine current probes. A captured geometry may be
temporarily behind immediately after a real mutation, but after 40 quiet ticks every
probe must again report `geometry_current`; an unreadable probe or a sample over 40
ticks old still fails. The first violation is retained until the end so slow-cut
diagnostics remain available.

Before the experimental partial-residency changes described below, with fixed
GameTest world seed `755111011717574879`, the implementation's
ordinary dense run passed `1/1` through tick 2000: nine probes from tick 720,
maximum age 22 ticks, no `WORK_LIMITED`, root charged peak 78,408,556 bytes and
maximum observed monitored cut 1,006 ms. Its later cuts were about 450-740 ms.
The shifted/reverse-order dense run passed separately through tick 1400: nine
probes from tick 820, maximum age 22 ticks, no `WORK_LIMITED`, root charged peak
63,043,160 bytes and maximum monitored cut 1,004 ms. At its terminal sample two
probes were on bounded captured geometry after a real mutation at tick 1399;
the test requires `geometry_current` at every probe after 40 quiet ticks, including
at the terminal sample when that condition holds. These local observations meet
the query deadline but the roughly one-second peak cuts do not establish a
comfortable CPU margin on a target server.

`ContinuousAirDispersedProductionGameTests` places 128 sources in eight closed,
spaced rooms. With the same fixed seed it failed at tick 1400: only four rooms
remained readable, 57 `WORK_LIMITED` completions accumulated, and a proposed
topology had 65,817 live cells against the independent 65,536-cell limit while
root charged usage was about 68.5 MB. Earlier fresh-world runs on 2026-09-29
passed ordinary, shifted and dispersed 128-source layouts under the 128 MiB root;
those passes are real local observations but do not certify the final code on
arbitrary terrain. The 32-source open-world control likewise had one 67,539-cell
failure and one pass on different fresh worlds. The current limitation is Page/Brick
residency and combined CPU work, not the former quadratic contact-position backing.

The current worktree contains an experimental partial-residency path added after
those dense passes. `WorkerPageStore.collectResidencyChanges` starts from source/hot
Bricks and preserves incoming face, route and shape requests. `MinecraftPageManager.processShrinks`
tries to capture the current publication into the existing dormant checkpoint before
submitting a smaller mask, at most one Page per 20 ticks. `TopologyUpdatePlanner`
can retire the removed Bricks within the same Page lifecycle, and subsequent
`PageResidencyUpdate` additions can carry dormant Air for projection as well as
the existing material restoration. Correctness over repeated partial retirement,
re-admission, source events and saving still needs production validation.

Two fixed-seed dispersed runs of this experiment failed. The diagnostic run ended
at tick 1400 with three of eight rooms readable, 60 `WORK_LIMITED` completions,
a last refused candidate of 65,676 live cells and peak charged usage of 67,286,620
bytes. Its last successful residency decision recorded 1,203 resident Bricks,
404 source/hot Bricks, 1,152 shape-support Bricks, 1,287 desired Bricks and zero
reclaimable resident Bricks. These sets overlap; in that run the decision was not refreshed on
`WORK_LIMITED` cuts. The historical `shrinks=10` diagnostic counts main-thread submissions,
not confirmed worker retirements. It cannot certify ten successful reclamations.
The last installed layout contained 65,036 material unknowns.

That failure used `AirShapeStore.requestSupport` for every Brick intersecting the
radius-six support cube. `BrickTopologyCompiler.compileCells` creates a body for
each material block in admitted Bricks, so geometry discovery unnecessarily pulled
buried terrain into physical residency. Historical evidence is in
`build/continuous-air-residency-diagnostic-20260930.log`; both saved diagnostic
worlds' `level.dat` seeds were verified as `755111011717574879`.

On 2026-10-01, `AirShapeStore.compile` changed normal live-source support discovery
to traverse source-connected, ventilated Air within the same radius-six cube and
inspect its blocking neighbor cells. Only visited Bricks are requested. Unknown
geometry postpones compilation and requests capture; it is never treated as a wall.
Visited Page handles/revisions remain dependencies, including blocking walls, so
opening a wall invalidates the shape and discovers the newly connected branch.
The connected-cell mask, response equations and support radius are unchanged.
Each footprint caches its support requests and copies/accounts their backing with
the candidate state. Source/hot, route, material-frontier and incoming-face residency
requests still apply independently; this is not a claim that source/hot Bricks alone
are sufficient. `discoverSupport` also seeds every cell belonging to saved or previously
installed spatial pieces, preserving branches disconnected from the active source while
stopping at their real blocking faces. Restored shapes retain their saved values during
projection; unknown portions continue requesting capture. `AirShapeStore.projected`
consumes complete saved pieces only after commit. Subsequent preparation refreshes the
active source's screened response on its current connected branch and reconstructs
detached historical branches separately. Inactive footprints reconstruct their history
without generating a new source response. `hasPendingSourceResponse` schedules this
preparation even if no further world block changes; natural-temperature-only layout reuse
cannot skip it. A missing Air component at the old outlet alone no longer removes an
inactive footprint with installed historical shapes. Its remaining Air branches are
reconstructed on the new geometry. The existing inactive-amplitude threshold remains.
That lifecycle change preserved radius, thermal parameters and solver tolerance.
Geometry capture and physical state allocation still share Brick residency; the
implementation reduces unnecessary discovery instead of introducing separate masks.

`ThermalDimensionEngine.process` now collects installed-topology residency decisions
after a rejected candidate as well, so rejection does not suppress cold reclamation
requests. A rejected mixed cut can now retry one smaller atomic candidate using
`TopologyUpdatePlanner.recoveryInput`: installed Page geometry/environment, retirements,
and resident-mask intersections proceed without new admissions or added Bricks.
If this candidate commits, `ThermalCompletion.deferredTopology` contains only the
uncommitted growth and its Page events. Main-thread retry never resets committed
Pages or replays source/phase events; source time is settled once before either
preparation attempt. If the smaller candidate also fails, the original topology
input is retried. Growth keeps the existing 200-tick backoff. Shrink preparation
can checkpoint captured cold Bricks while other requested Bricks are still missing;
it still requires a coherent current publication and submits at most one Page per
20 ticks. This path does not promise arbitrary over-capacity growth or every possible
40-tick recovery sequence. The historical `submittedShrinks` diagnostic counted
submissions, not commits. That unused counter was retired on 2026-10-26; current
runtime ownership and validation are described in [the control contract](thermal-runtime-control.md).

The first fixed-seed dispersed run with connected discovery passed `1/1` through
tick 1400: eight rooms readable/current at the final sample, monitoring from tick
600, maximum age 22 ticks, zero `WORK_LIMITED`, peak charged backing 55,673,448 bytes
and maximum monitored cut 771 ms. Shape support was 256 Bricks at ticks 300/700/1100
and 368 at the terminal sample; material unknowns were 59,045 at the terminal cut.
This run predates the additional rejection-reclamation change and saved-branch discovery.
It does not prove long-run capacity or complete geometry/physical-state separation.
Extending that initial implementation to 2400 ticks exposed a further capacity refusal
after tick 1800; the run failed before its terminal sample. It still used full-cube
support for restored pieces. Saved-branch discovery was added in response, and the
failure remains recorded in `build/continuous-air-connected-dispersed-long-20261001.log`.

With saved-branch discovery included, the same dispersed seed passed through tick
2400 (`1/1`): all eight probes remained readable during monitoring from tick 600,
maximum age 22 ticks, `WORK_LIMITED=0`, peak charged backing 49,094,664 bytes,
maximum monitored cut 882 ms, and 52,254 material unknowns at the final cut. All
terminal probes were `geometry_current`; shape support remained 256 Bricks in the
reported checkpoints. Evidence: `build/continuous-air-connected-restored-20261001.log`.
This is a two-minute world run, not long-running server or default-enable certification.

The final connected-support implementation also passed the ordinary dense fixed-seed
128-source run through tick 2000 (`1/1`). Monitoring began at tick 720, all nine
probes were readable/current at the terminal sample, maximum age was 22 ticks,
`WORK_LIMITED=0`, peak charged backing was 52,110,728 bytes and maximum monitored
cut was 673 ms. The terminal cut contained 36,779 material unknowns. Evidence:
`build/continuous-air-connected-dense-20261001.log`. Peak cut measurements do not
provide P95/P99 or a target-server CPU guarantee.

`ContinuousAirSupportProductionGameTests` passed separately through tick 1200
with real ignition, opening, closure and reopening (`1/1`, no capacity refusals).
The material count changed 1,619 → 2,026 → 1,619 → 2,026. This run preceded the
final saved-branch discovery change, and its four stage observations do not certify
continuous query deadlines or saved-heat fidelity. It checks local basis presence,
not a before/after spatial heating curve. In particular, `airBasisTerms` reports
the containing component's basis count, not the nonzero local contribution at the
queried position.

The combined namespace regression was interrupted after three recorded failures:
the initial wall test required current geometry during an active update window;
the 16-source test lost readable Air at tick 582; and the distant unload/reload
test never obtained local modes before saving. The wall assertion was adjusted
to the existing 40-tick mutation window before its separate pass. The other two
failures remain unresolved. The default-batch tests build beyond their small
templates, making scene interference a possibility, not a demonstrated cause.
Evidence: `build/continuous-air-connected-regression-20261001.log`. The final
implementation at that date had not passed the separate 16-source and focused production
regressions or the shifted/reverse-ignition 128-source case. The 2026-10-02 independent
16-source and focused production runs passed `1/1` and `5/5`, respectively, including
the distant real chunk save/unload/reload. This does not prove interference caused
the earlier combined-run failures.

The saved-basis issue identified on 2026-10-02 was reproduced through actual ignition,
extinguishing, save, chunk unload/reload, reignition and a new doorway. Before the fix,
the new room had current geometry but zero point-local shape support until the test
timed out. After checkpoint consumption and response refresh were corrected, the
same scene passed; the opened point had local weight `0.10901178429776337`, warmed,
and retained the pre-save near/far heat gradient. `QueryPublication.ReadCursor.localAirWeight`
is an optional read-only diagnosis under a coherent publication; `airBasisTerms`
alone reports a component's mode count and cannot establish detail at a specific point.
Logs: `build/continuous-air-restored-support-before-20261002.log`,
`build/continuous-air-restored-support-after-20261002.log`, and
`build/continuous-air-final-frostedheart_restored_support_production-20261002.log`.
Repeated partial captures/re-entry and all heat-history fidelity still need broader acceptance.

The extended saved-support run also extinguishes the source after opening the doorway,
replaces its former Air outlet with actual stone, and requires a fresh current-geometry
query with nonzero local support in the remaining room. It passed `1/1`; the final
point was `-12.160583671361055 °C`, local weight `0.1090117836574791`.
Evidence: `build/continuous-air-acceptance-restored-outlet-20261002.log`.

The stronger overload scene replaces an actual inner wall inside the installed source
support while 64 constructed column rooms request more than 65,536 live cells. It
passed `1/1`, with 60 recorded refusals and 60 installed-state recovery commits by
tick 2402. The final existing-room sample was `geometry_current`, age 22 ticks.
This deliberately excessive scene verifies continued existing-region updates; its
refusals are expected. It does not establish that all requested rooms fit or that
every submitted shrink committed. Evidence:
`build/continuous-air-refusal-innerwall-20261002.log`.

`ContinuousAirRestartProductionGameTests` passed in two separate Forge GameTest server
JVM launches against the preserved world `thermal-jvm-restart-20261002`. The first
launch saved actual extinguished heat; the second loaded the actual unlit campfire,
checked disk-decoded material H/branch against observations recorded after saving,
read public Air/material queries, then reignited and opened a wall. The stored
near/far Air temperatures were `-10.974097969769076/-11.899772244208448 °C`; the
second launch read `-10.97480292333772/-11.900049544251347 °C`. Its new passage had
local weight `0.10901178429776337`. `RestartObservation` stores observations only,
not thermal state. Evidence: `build/continuous-air-jvm-save-20261002.log` and
`build/continuous-air-jvm-reload-20261002.log`. This closes the basic actual-JVM
restart case, while repeated partial recovery, phase history and target-server
restart/load acceptance remain open.

The experimental change from 32-mode to 64-mode preconditioner blocks failed the
real shifted/reverse-ignition 128-source scene: projection reached its 2048-iteration
limit without satisfying the unchanged `1e-9 °C` correction bound. It was reverted.
The retained block size is 32; that experiment introduced no tolerance relaxation. Evidence:
`build/continuous-air-offset-block64-20261002.log`. Coarse/local conditioning
needs further work if similar failures occur with the retained partition.

`ThermalCompletion.CutTiming` carries a coherent completed-cut timing observation
from the worker to the main thread. `MinecraftThermalInput.completedCutTiming`
returns the last drained observation, rather than mixing independently read live
worker fields. `ContinuousAirProductionTiming` observes each completion in actual
dense/dispersed source scenes and logs worker solve/topology/publication durations,
transport PCG iterations, and the first fresh current-geometry response at all configured
probes. Projection iteration counts remain in rebuild diagnostics. These observations
do not participate in solving or add a thermal limit. Ordinary gameplay queries
used for observation may request Page capture, like other consumers.
`THERMAL_FIRST_LIVE_READ` records the first fresh local Air readable at every probe,
including permitted captured-geometry fallback; `THERMAL_FIRST_RESPONSE` additionally
requires all probes to report current geometry. The latter can occur later during
ongoing world mutations and must not be labeled the first available Air sample.
First response means the probes used by the capacity scene; it is not IR rendering,
100-player load, main-thread cost or complete target-server latency certification.

`ThermalMemoryBudget.peakUsedBytes` records each admitted reserve/resize high-water
mark, including the root shared by dimensions. `THERMAL_CAPACITY_ROOT_PEAK` and
the timing diagnostic's `rootPeakChargedBytes` report it. Earlier periodic
`maxChargedBytes` measurements can miss short-lived preparation and old/new overlap;
they remain historical sampled values. This is a peak of charged backing, not an
independent whole-JVM heap measurement or completion of transient pre-admission.

The final 2026-10-02 measurement reruns used separate newly created named worlds
and the same configured seed `755111011717574879`. They produced the following
observations; monitoring includes ordinary world mutations and rebuild costs.
Percentiles use nearest rank and are from small single-run windows.

| Scene | Final result | Monitored completed cuts | Worker cut P95 / P99 (ms) | Recorded root charge high-water (bytes) | First all-probe fresh live / current-geometry response (ticks) |
|---|---|---|---|---|---|
| Ordinary dense 128, through 2000 ticks | Failed query freshness | 65 | 1201.006 / 1609.997 | 93,333,512 | 106 / 106 |
| Eight-room dispersed 128, through 2400 ticks | Passed `1/1` | 90 | 436.545 / 1099.576 | 96,517,312 | 76 / 112 |
| Shifted/reverse-ignition dense 128, through 1400 ticks | Failed; never entered the monitoring window | 0 | No monitored percentile; longest observed completed cut 8265.021 ms | 89,767,294 | Neither observed |

Evidence: `build/continuous-air-peak-dense-20261002.log`,
`build/continuous-air-peak-dispersed-20261002.log`,
`build/continuous-air-peak-offset-20261002.log`; parsed observations are in
`build/continuous-air-acceptance-timing-20261002.json`. The successful dispersed
run also recorded a terminal `THERMAL_CAPACITY_ROOT_PEAK`; failed scenes assert
before that terminal log, so their reported high-water values come from completed-cut
observations and do not certify later unfinished work. These charge observations
are about 89.01/92.05/85.61 MiB, respectively; they include admitted reservations
and are not actual whole-heap peaks.

The dense failure occurred at game tick 742 with the publication still at tick
700. Current geometry alone did not make the 42-tick-old sample fresh: the ordinary
gameplay query returned a fallback. Its next completed cut took 1609.997 ms:
949.745 ms before topology preparation, 555.269 ms topology and 104.982 ms publication.
`maximumAgeTicks=22` in the harness does not contradict this failure: it counts
only samples that the ordinary query returned as live, excluding expired fallback reads.

The shifted rerun had two projection failures with the retained **32-mode**
preconditioner, each reaching 2048 iterations. Reported temperature correction
bounds were `1.2650993750061258e-9` and `6.4064940965991014e-9 °C`, above the
unchanged `1e-9 °C` threshold. Both caused `ENGINE_FAILED` recovery; after accumulating
longer cut intervals, a completed cut took 8265.021 ms and had completion age
165 ticks. It never achieved fresh local Air at all probes. This is separate
from the rejected 64-mode experiment and makes retained-solver robustness a
default-enable blocker. The underlying numerical cause and the differences from
earlier passing cut histories remain under investigation. These runs differ in
observed wind/input/capture histories and cannot alone assign the failures or
timing changes to the graph-assembly optimization.

The 128-source dense harness starts continuous assertions only after tick 600
and all probes becoming readable/current, then requires at least 400 monitored
ticks. This verifies a bounded steady-state window, not a first-publication
latency requirement or a long-run server guarantee. Ordinary dense has passed the
connected-support implementation. The later final-code independent runs are recorded
separately; short steady-state passes do not establish first-publication or long-run bounds.

`ContinuousAirSleepProductionGameTests` passed `1/1` on 2026-09-25: a source-free idle
worker slept, then actual flint-and-steel ignition woke it and published local Air modes.
This exercises idle wakeup; sleep frequency in occupied or changing climates remains unmeasured.
The source default remains `continuousAir = false`; the GameTest common config
persists between runs, so check its setting before comparing backend results.

```powershell
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_production --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_production_slow --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_capacity --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_capacity_128_dispersed --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_capacity_128 --offline --no-daemon --max-workers=1 --console=plain
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_capacity_128_offset --offline --no-daemon --max-workers=1 --console=plain
```

The Gradle `cleanResearchGameTestWorld` dependency deletes `run-gametest/world`
even when `server.properties` selects another world name. Full-restart validation
must preserve its actual world between launches. Restore temporary test config
after the run. Logs and profiles belong in `build/`; validation outcomes belong in the diary.

### 2026-10-03 stopping precision and Air projection factor

The user authorized production investigation of lower stopping precision. A full
Air/material incomplete-Cholesky candidate at `1e-9 °C` still failed projection
three times in actual offset128 production (`2.35e-9` to `3.11e-8 °C` stopping
estimates at 2048 iterations). At `1e-6 °C` it avoided those failures, but repeated
full factorization delayed the all-probe monitoring window until tick1300, leaving
too little acceptance time. That full-system application was removed. The retained
factor handles Air topology projection only; transport and impulses retain P32.

Independent real Forge runs then compared `1e-6` and `1e-5 °C` with the same retained
kernel, seed, fueled campfire layouts and source order. These are production latency
observations, not identical-input solver replays or certified numerical-error bounds.
The table uses each scene's common elapsed monitoring window so a later warmup is
not mistaken for a speedup. Values are nearest-rank worker-cut P95/P99 in ms.

| Actual scene | Common window begins (tick) | `1e-6 °C` P95 / P99 | `1e-5 °C` P95 / P99 | Result |
|---|---|---|---|---|
| Dense128, through 2000 ticks | 1320 | 462.974 / 537.067 | 363.020 / 393.871 | Both passed; 34 observed cuts each in this common window |
| Offset128, through 1400 ticks | 660 | 1026.271 / 1058.161 | 597.577 / 607.402 | `1e-6` had a 41-tick query expiry; `1e-5` passed |
| Dispersed128, through 2400 ticks | 720 | 645.411 / 864.591 | 288.432 / 421.312 | Both passed; 84 observed cuts each in this common window |

All three `1e-5` runs had no projection failure, no coalesced intervals, and admitted
root high-water between 95,257,808 and 98,071,656 bytes. Different wind/capture/input
histories can affect latency and temperature observations; no precise causal
speedup ratio or gameplay-temperature error bound follows from this table.
Dense/offset freshness now checks every tick once monitoring starts. Dispersed was
still sampled every20 ticks in this comparison; its final repeat adds tick-level
expiry observation. Logs: `build/continuous-air-ic-projection-{micro,tenmicro}-*-20261002.log`.
Analysis: `build/continuous-air-precision-comparison-20261003.json`.

The source default is now `continuousAirCorrectionToleranceC=1e-5 °C` for powered
transport/projection. Unpowered cooling and J impulses are capped at `1e-9 °C`;
phase rounding, double precision, source W/J, physical capacities/conductances,
R6, cell/budget limits and query age are not relaxed. This changes the approximate
solution's stopping precision; it is not a lossless numeric change and does not
certify the temperature field to `1e-5 °C` actual error. Core/body balancing and
the clothing model were not edited in this solver work.

Production behavior passed 5/5 at the tested threshold: near-source heating/retained
heat after extinguishing, within-Brick detail and cross-face continuity, actual
chunk unloading/reloading, slab-outlet rebinding and the IR data path. The stricter
unpowered/impulse guard was added afterward and the final production repetition
also passed 5/5.
Quiet sleep/wakeup passed 1/1. Two independent JVM launches passed 1/1 each against
`thermal-precision-jvm-restart-20261003`: near/far Air restored from approximately
`-10.974099/-11.899773 °C` to `-10.974804/-11.900050 °C`, actual floor H/branch was
checked, and reignition plus a new passage produced local shape contribution.
Logs: `build/continuous-air-ic-projection-regression-*-20261002.log`.
This verifies server data, not the rendered IR image in CT2.

Final source-default repeats removed the explicit tolerance key before launching;
Forge regenerated the actual `1e-5 °C` default. Dense, offset and dispersed all passed
with tick-level expiry observation. No projection failure, coalesced interval,
sample-expiry violation or persistent `WORK_LIMITED` was observed in those windows.
Final production passed 5/5 after the stricter unpowered/impulse guard was retained.

| Final scene | Monitoring begins (tick) | Completed monitored cuts | Cut P95 / P99 (ms) | Root admitted high-water (bytes) |
|---|---|---|---|---|
| Dense128 | 720 | 64 | 494.282 / 547.939 | 95,183,760 |
| Offset128 | 660 | 37 | 733.426 / 749.700 | 97,408,392 |
| Dispersed128 | 600 | 90 | 361.531 / 490.044 | 98,071,936 |

Logs: `build/continuous-air-ic-projection-final-default-*-20261002.log`.
Root charges remain admission accounting, not whole-heap peaks. Variation between
the first and final series prevents claiming a fixed percentage speedup. Offset
still exceeds the aspirational 500 ms P95 engineering margin; all three currently
pass the actual 40-tick query contract in their recorded windows.
`continuousAir` remains opt-in pending the remaining default-enable gates.

### 2026-10-03 repeated contact pair lookup

Production rebuild timing was split into material indexing, contact compilation
and sparse graph assembly. The offset128 baseline observed `contactsMs=32..39`
and `graphMs=206..214` in the four periodic rebuild snapshots. Exact ordered-support
reuse then reduced observed graph assembly to `116..128 ms`, with independent
per-contact accumulation preserved. These snapshots are not per-cut percentiles.

Both offset runs monitored37 completed cuts from tick660 through1400. Baseline
worker-cut P95/P99 were755.607/771.401 ms; with lookup reuse they were
655.545/680.354 ms. Topology-stage P95 changed from338.831 to238.418 ms. They use
the same configured stopping threshold and scene/seed, but different wind/input
histories, so this is observational evidence rather than a fixed causal speedup.

The retained lookup change also passed dense and dispersed capacity scenes:

| Scene | Monitoring begins (tick) | Completed monitored cuts | Cut P95 / P99 (ms) | Root admitted high-water (bytes) |
|---|---|---|---|---|
| Dense128 through2000 ticks | 1320 | 34 | 413.912 / 416.847 | 95,183,760 |
| Offset128 through1400 ticks | 660 | 37 | 655.545 / 680.354 | 97,408,392 |
| Dispersed128 through2400 ticks | 600 | 90 | 334.545 / 415.595 | 98,071,360 |

No projection failure, query-expiry violation or coalesced interval was recorded
in these three runs. Dense's later monitoring start means its low steady-window
latency does not certify startup behavior. Offset still exceeds the500 ms P95
margin. Logs: `build/continuous-air-ic-projection-contact-{profile,pairs}-*-20261002.log`.
Analysis: `build/continuous-air-contact-pairs-20261003.json`.
This change does not alter heat equations, integration points, double precision,
stopping thresholds, source W/J or material phase endpoint rules.
Production behavior regression also passed5/5 with the retained lookup change,
including extinguished heat, within-Brick/face behavior, actual chunk reload,
slab outlet and server IR data. All four runners exited successfully, temporary
world properties were restored and no test JVM remained. This does not certify
128-source simultaneous extinguish/reignition or rendered-client behavior.

Outstanding implementation limits:

- The shared retained-backing meter covers candidate/previous Air layouts, operators,
  shapes, source tape, PCG scratch and query-held old layouts, with a separate
  conservative preparation reservation. Dynamic source-tape growth after source
  acceptance and every transient compiler allocation are not yet pre-admitted;
  `128 MiB` is not a certified whole-heap bound.
- The contact-position budget bottleneck is removed. Ordinary dense, dispersed
  and shifted/reverse-ignition 128-source scenes passed independent regressions,
  but a later dense rerun failed the unchanged 40-tick query deadline: a
  current-geometry publication from tick 700 was still installed at tick 742,
  causing a gameplay query to fall back. That run's monitored completed-cut
  P95/P99 were 1201.006/1609.997 ms. Earlier passes therefore do not establish
  repeatable capacity or target-load margin. Connected support
  discovery removes the observed dispersed capacity failure through 2400 ticks,
  while broader and longer capacity acceptance is still required. Saved spatial
  pieces now discover all their branches instead of requesting the whole cube.
  Geometry capture versus simulation residency and broad checkpoint/re-admission correctness
  remain open. Saved-basis refresh after reload/opening a doorway is fixed and has a
  real reproduction/regression. Refusal discards the oversized candidate and may commit
  installed-state updates separately; production overload evidence and remaining
  deadlines must be assessed per scene, not inferred from submitted shrink counts.
  Raising the memory budget or cell limit alone does not establish general capacity.
- Only strictly natural-temperature-only updates reuse the layout; other environment updates can still trigger full rebuilds. Quiet sleep is conservative and has not been measured on a target server.
- Local/local overlap rank pruning is implemented, but coarse/local dependence and a projection-error bound for inactive-mode removal remain open. Inactive-mode removal still uses a 0.01 °C coefficient heuristic. The old strict shifted rerun reached the projection iteration limit twice; the new factor/configured precision passed the described128-source windows, while longer and repeated acceptance remains necessary before default enablement.
- First-publication latency, target-server restart/load, complex-building capacity,
  repeated terrain/phase mutation and target-server latency have not been certified.
  The implementation remains opt-in.

Intended work and acceptance criteria are tracked in the [engineering plan](../../diary/2026-10-29_23-38-00_thermal-development-history.md).


### Plain pairwise capacity follow-on

The recommended performance foundation now retains the original source face (`pairwise`),
while `pairwise_plume` remains an experimental calibration comparison. An actual
standard256/both_bases/2400tick/realtime20 run passed: worker P95/P99
234.732/397.065 ms, topology/solve/publish P95 192.838/30.473/35.061 ms,
charged peak85.909MiB, 256 enabled sources,16 LIVE floors and12000 environment
calls, with zero age/geometry/receiver/WORK_LIMITED/preparation/worker failures.
172 parent transfers aligned; source imbalance0. This closes plain-variant capacity,
not numerical fidelity:45802 matching observations still differed by up to2.299909 C.
Its full worker P95 is higher than the earlier CV observation198.349ms; it reduces
advancement and memory, not every recorded cost. Construction remains dominant.
The bounded near-source diagnosis and the subsequent graph-only optimization are
now completed below; defaults stay unchanged.

### Volume-only pairwise graph and bounded near-source diagnosis

Actual standard-budget, realtime20 runs on the local i7-12800HX used the same
original-face pairwise variant, event schedule and existing saved climate:

| Load | Before worker P95/P99 ms | After worker P95/P99 ms | Before/after charged peak MiB |
|---|---:|---:|---:|
| One dense128 base | 186.877 / 237.464 | 164.404 / 216.837 | 62.919 / 62.030 |
| Two dense128 bases | 234.732 / 397.065 | 174.801 / 325.233 | 85.909 / 84.742 |

After256 topology/solve/publish P95 was 143.806/18.720/27.317 ms. The full test
retained 256 enabled fires,16 LIVE floors and12000 environment calls; age,
geometry,receiver,WORK_LIMITED,preparation refusals and worker failures were0.
173 parent transfers aligned. Source output187392000 J still equalled
delivery148275200 J + declared loss37478400 J + degraded loss1638400 J.
The graph change modifies no source W/J, receiving face, M/K, H law, latent heat,
solver tolerance or checkpoint codec. Exact matched temperature observations
before/after reached7.11e-15 °C at128 and0.003229 °C at256. The latter independent
worlds had different construction/transfer histories; this is observed fidelity,
not a bitwise replay claim. Complete worker wall time is not total server CPU/TPS.

`ContinuousAirTranslationProductionGameTests` builds four actual17x17x9 stone
rooms, each with nine coal campfires, separated65 blocks and translated0/1/2/3
relative to the four-block partition. It uses actual ignition/extinguishing,
holds the ordinary daylight gamerule, restores the existing climate and uses
vanilla `fillbiome` to set plains. It injects no Air temperature or material H.
Each run observes156 positions every20 ticks through1000 ticks, with extinguishing
at600. Corresponding sampleTick/naturalC/source state are matched; outside-room
geometry and captured-page backgrounds still limit causal attribution.

Original, implicit CV and pairwise each passed their current-query/capacity test.
1895 four-room matches per variant had maximum spreads0.833448/0.361390/0.519317 °C.
7604 original/CV/pairwise matches showed:

| Compared fields | Startup maximum °C, elapsed<200 | Later powered maximum °C, elapsed200–600 | Cooling maximum °C |
|---|---:|---:|---:|
| Original versus pairwise | 11.747885 | 2.874889 | 2.869388 |
| Original versus implicit CV | 9.639606 | 3.039833 | 3.034982 |
| Implicit CV versus pairwise | 2.108278 | 2.073110 | 2.049345 |

Before120 ticks, the original builds had no local response modes; modes first
appeared at120 and reached18 in all four rooms at140. This agrees with
`AirShapeStore.compile` returning null while `discoverSupport` is incomplete.
Until then source input is represented by the coarse field; the shared grid
already has local nodes as Air is captured. All three final source balances were
identical. This supports delayed original localization as a major startup cause,
while CV/pairwise differences also expose time composition effects. Later
differences persist even with implicit transport, so neither translation alone
nor replacing pairwise with PCG establishes approximately1 °C fidelity. No source
percentage, latent heat or speculative correction was changed in this diagnosis.

Same-version production regression passed5/5, including actual unload/reload,
slab outlet and infrared queries, but recorded one recovered preparation refusal
and a122.057 MiB charged peak. Reader/phase lifecycle passed2/2 with zero refusals:
three readers released1702904 B to0, and powder snow again required7017 actual
supply ticks (placed784, converted7801). Broader independent dispatch, all-temporary
admission, accumulated-space capacity, queue/endurance and real clients remain
pending; no default promotion follows from these runs.

Evidence: [128 performance](../../build/continuous-air-pairwise-volumegraph-128-performance-20261007.json),
[256 performance](../../build/continuous-air-pairwise-volumegraph-256-performance-20261007.json),
[256 curve comparison](../../build/continuous-air-pairwise-volumegraph-256-envelope-20261007.json),
[uniform translation](../../build/continuous-air-near-source-translation-uniform-20261007.json),
[CV comparison](../../build/continuous-air-near-source-translation-cv-vs-original-20261007.json),
[time comparison](../../build/continuous-air-near-source-translation-pairwise-vs-cv-20261007.json),
[production](../../build/continuous-air-pairwise-volumegraph-production-20261007.json),
[lifecycle](../../build/continuous-air-pairwise-volumegraph-lifecycle-20261007.json).

### 2026-10-08: contiguous addresses, packed graph input and shared reader payload

`pairwiseVolumeEdgePositions` now stores12 addresses per unit volume in one int
array, with a shared matrix-entry map. `packedPairwiseKey` uses variable-width
integer fields when two node indices plus an occurrence address fit63 bits;
otherwise the original paired long arrays remain. The stored indices are exact,
and the represented node field, volume accumulation and pair sweep order stay the
same. `ContinuousAirSolver.preparationGraphBytes` counts actual candidate Air in
changed scopes; `RegionalAirRuntime` reserves their combined graph bound before
starting helpers. Full4x4x4 Air Bricks have300 unique local edges. Paired/unary
radix stacks use a64KiB allowance per simultaneous sorter on the existing pool.

`ThermalDimensionEngine.observeAirBacking` supplies admission and diagnostics from
one scan. The scan returns counts/text rather than array references. After query
capacity, numerical preparation and this scan complete, preparation scratch ends;
the full old/new retained backing is then charged. Graph bounds and other numerical
scratch no longer remain charged as though their temporary arrays were still live.
This lifecycle change affects admission and timing even though heat parameters are
unchanged, so functional regressions must accompany the storage comparison.

Intermediate versions exposed two failures. Overly conservative graph output
bounds and overlapping preparation charges caused156/121 graph refusals in two
failed runs. After packed inputs removed those refusals, the cumulative infrared
test still failed. At tick3400 the query budget used13815934 B and borrowing
requested another5021928 B:18837862 B, or17.965MiB, exceeded the16MiB query cap.
The current immutable snapshot arrays were already covered by the regional
reservation, but the old lease code reserved the whole payload again. Live
material point reads still worked; full snapshot acquisition returned null.
Checkpoint/reclaim paths use the same `beginRead` gate. The failed test later
encountered66327 proposed live cells against65536, but reader failure came first;
that later limit is not the direct cause of the first missing infrared snapshot.

The shared payload implementation described above fixes the duplicated ownership
charge. `ensureRegionalPayload` accounts for a proposed publication and all held
generations before swapping references; ACK reclamation also keeps held arrays.
Closing the publisher keeps the exact reader union until the last release.
Ordinary point reads and the40tick age policy retain their existing contracts.
`describeReaderMemory` exposes query usage, limits and refusal counts. No128MiB
root,16MiB query or65536 live-cell limit was increased.

Final same-source Forge validation: cumulative production5/5, preparation/reader
refusals0, charged root peak114.992MiB; standard256 full realtime20/both_bases
passed with256 enabled sources,16 LIVE floors,12000 environment calls and zero
age/geometry/receiver/WORK_LIMITED/worker failures. Worker P50/P95/P99 was
125.148/195.534/408.707ms and charged peak65.502MiB. The previous volume-only
graph run recorded104.815/174.801/325.233ms and84.742MiB; current memory is lower
but this independent run's worker latency is higher. These results do not prove
all-metric CPU dominance or real100-client server performance.

816 points/46528 exact matched observations versus that prior pairwise run differed
by at most0.003229C. Reader/phase lifecycle2/2 passed, with three readers retaining
1703096B at close and releasing all to0; powder snow again took7017 supply ticks.
Two separate JVMs on one actual saved world each passed1/1, restoring Air and
material H/branch before actual ignition and passage changes. No JUnit, injected
temperature/H or test-threshold changes were used.

The successful cumulative run finished at3360ticks with no observed live-cell
overrun. It is not a long-duration capacity certificate. The old approximately1C
temperature objective, broad R3 timing, all remaining temporary allocation bounds,
endurance and real clients/CT2/default promotion remain pending.

Evidence: [failure chain](../../build/continuous-air-flat12-reader-root-cause-20261008.json),
[production](../../build/continuous-air-flat12-readers-final-production-20261008.json),
[256 performance](../../build/continuous-air-flat12-readers-final-256-20261008.json),
[256 curve](../../build/continuous-air-flat12-readers-final-256-envelope-20261008.json),
[lifecycle](../../build/continuous-air-flat12-readers-final-lifecycle-20261008.json),
[restart](../../build/continuous-air-flat12-readers-final-restart-20261008.json).

## 2026-10-08: bounded support and unchanged-region preparation reuse

The recorded performance foundation remains the explicit
`regional/fine_grid/local/pairwise` selection. `continuousAir=true` alone does
not select these trials, and ordinary defaults have not been promoted.

`AirShapeStore.GridSupportCache` belongs to the existing source footprint. Only
complete support searches are cached. Reuse requires current Page identity and
geometry revisions, the same captured Air masks for the visited Bricks, and no
pending saved-piece/source-response reconstruction. Masks come from
`AirFieldLayout.airBlocksAtBrick`, independently of coefficient numbering.
Candidate copies and regional inheritance share immutable search results;
support-request maps remain separately owned. Incomplete or invalidated
searches run normally, and inactive residual-heat retention is evaluated on
every preparation. New cache backing is included in the Air budget and dies
with its existing owner. This preserves source radius, reachability and heat
input; it is not a union of differently clipped source searches.

`RegionalAirRuntime.prepareRebuild` still partitions and validates candidate
regions. When every region is retained in the same order and identity, it
reuses the combined layout, aggregate storage, ownership index and source-load
addresses. Ordinary advance, fragment/environment updates, restoration
acknowledgements and completion ordering continue. Changed ownership takes
the full path. This shortcut does not yet remove the full partition or
backing scans.

`SharedAirGridCompiler.compile` resolves all final corner identities before
reusing its union-parent workspace as the per-Brick local-address lookup.
This replaces repeated linear searches without additional arrays or changed
node order, face connectivity or wall separation. `ThermalDimensionEngine`
also sizes each temporary `AirBackingMeter` identity table from the previous
scan's entry count. It still performs a fresh, complete identity-deduplicated
scan; byte totals and reader ownership are not cached or omitted.

`MinecraftThermalInput.needsChunkCheckpoint` checks tracked and pending Page
lifecycle, physical-source origins, and in-flight/queued batch ownership.
Unrelated saves skip the dimension barrier; unrelated unloads keep their
normal Page/source/radiation invalidation callbacks without awaiting thermal
work. Related chunks retain the existing barrier. Final shutdown still aligns
and checkpoints. The actual single-chunk save regression invokes Minecraft's
`ChunkMap` save method through Forge's mapped-access helper; the normal save
event and NBT write execute, and the production checkpoint entry records the
worker as pending when the unrelated barrier is skipped.

`FH_CONTINUOUS_AIR_PROFILE=true` enables input-tick and save-wait observations.
GameTest resource observations use `ThermalWorkerPool.observedResources` to
measure cumulative CPU/allocation for each worker once, including inline
helpers. The optional Gradle `thermalJfrFile` property records only the GameTest
JVM. `mainThermalTick` covers capture/ACK/phase/radiation tick work, not all
player queries, lifecycle callbacks or whole-server MSPT. Construction
diagnostics now distinguish support discovery, shared-grid compilation, unit
description creation and layout combination. Wall time remains distinct from
thread CPU.

In the same diagnostic configuration, standard two128/both_bases/2400tick
baseline worker P50/P95/P99 was 89.768 / 145.994 / 285.013 ms. The final candidate
was 75.835 / 117.193 / 309.450 ms, with charged root 65.697 MiB versus 65.502 MiB.
Worker CPU 4156→3890 ms and allocated bytes 4674150000→4334419232 improved by
6.40% and 7.27%. P99 increased; no universal speedup is certified. Source support
searches fell 12928→2322 with 10606 cache hits, visited cells 12115360→2113148,
and 41 cuts reused the combined layout. The no-numerical-build cuts had
P95=60.680 ms, above the 50 ms goal; numeric-build cuts had P95=142.581 ms.
CPU reduction of 20% and allocation reduction of 30% remain unmet. Input-tick
P95/P99 was 0.317 / 2.097 ms, within its scoped budget. These are independent asynchronous
histories with identical scripted gameplay, not an exact input replay.

All final selected regressions passed: standard 256-source 1/1, accumulated
production 5/5 with charged root 114.991 MiB and no refusals, readers/phase 2/2,
actual single-chunk save isolation 1/1, and separate-JVM save/restart 1/1 each.
The 256-source scene retained 256 sources, 16 LIVE floors, 12000 environment calls,
all freshness/geometry/receiver/WORK_LIMITED checks, 173 aligned transfers and
zero source balance. There were 46919 matched observations across 816 points,
differing from this round's pairwise reference by at most 5.153e-11 °C. Readers
released 1703096 B→0; powder snow still converted after 7017 supply ticks.
Parameters and version 5 did not
change.

Automatic unload-overlap probes did not observe a Forge unload callback and
remain `not_triggered`. A disappearing `getChunkNow` result is insufficient
proof. Serialization alone did not emit the save event, and whole-world saves
first aligned related chunks; those failed probes are retained. Related-save
nonblocking behavior, complete queue-inclusive regional timing, all remaining
temporary bounds, the original approximately 1 °C objective and real clients/
multidimension/default gates remain pending. The 12000 tick endurance was not
run because the selected short performance goals were not all met.

Evidence: [all runs and gates](../../build/continuous-air-cpu-package-20261008.json),
[temperature observations](../../build/continuous-air-cpu-package-final-envelope-20261008.json),
[bounded outcome](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-08: omit unused implicit material preparation in pairwise builds

`LocalMaterialBoundary` now prepares feedback rows, accumulated/sorted surface
weights and scalar response vectors only for the implicit path. The explicit
`regional/fine_grid/local/pairwise` selection does not call that response.
It retains material laws, enthalpy/branches/waiting, contact-to-body addresses,
finite-capacity Air exchange, internal material conduction and routed edges.
Reverse body-to-contact indexing remains available when `REUSE_SURFACES` is
enabled. Implicit `control_volume` still prepares and uses the full response;
an implicit preparation entry on a pairwise-only boundary fails explicitly.
Shared empty arrays are still charged once by identity in `AirBackingMeter`.
Diagnostics report actual surface-contact counts independently of retained
reverse indexes. This changes preparation/storage, not the advance formula,
source energy, double precision, nodes, cadence, thermal parameters or version 5.

Two sequential real Forge A/B pairs used the same compiled implementation,
two dense 128-source bases, 2400 ticks, realtime20, standard budgets, JFR and
matching temperature observations. The temporary legacy/compact selector was
removed after comparison. Worker allocations fell 8.74% and 10.35%; charged
Root peaks fell about 2.806 MiB in both pairs. Worker CPU changed -21.52% and
+5.30%, so a repeatable whole-worker CPU improvement is **not established**.
Whole cut P95 changed 173.829→169.060 and 140.483→118.866 ms; P99 changed
337.466→365.574 and 347.675→292.674 ms. These mixed tails do not certify a
universal latency improvement or the prior 50/150 ms and 20%/30% goals.

The first pair had identical build/material/contact totals; the second had
172→173 builds and a small capture-history difference. Matching observations
covered 816 positions: 47328 samples differed by at most 2.640e-12 °C, and
47070 samples by at most 0.00322896 °C. This is relative to current pairwise,
not the original analytic behavior. About 151.6 MB of retained-array
constructions per scene and 2.18 million sort calls, including empty sorts,
were omitted. Those are cumulative allocations, not simultaneous live heap.
JFR allocation weights support removal at the constructor but are not exact
bytes. Ordinary backend defaults remain unchanged.

Profile diagnostics now record the original regional reuse predicates without
weakening them. In the final 256-source observation, 173 cause records joined
173 builds; 57 were material-only and 116 had Air. Seventy Air builds changed
captured Air identity/masks and accounted for 900.616 of 1113.408 ms of summed
Air graph construction wall time. Twelve had unchanged members/laws/ports
but replaced fragment/execution objects, accounting for only 15.645 ms of
graph work; identity replacement is not a deep numerical comparison. The
next significant construction investigation is local compilation under actual
Air frontier changes, not a claim that every rebuild is avoidable metadata.
No stable global-node database or incremental connectivity framework was added.

`CutTiming` also exposes immutable-batch creation→processing and
creation→query-publication latency, including sealed-input queueing. It
excludes uncaptured input age and main completion drain and does not yet
attribute every regional queue. Profile save/unload counters include maximum
single waits and their packed `ChunkPos`. GameTest processing spans the
preceding complete Forge START/END tick, excluding pacing sleep; it is distinct
from `mainThermalTick` and real-client MSPT. The final 1225.751 ms maximum was
observed at elapsed1 for the previous scene-setup tick; P95/P99 were
9.346/24.564 ms. Related saves still keep their barrier.

Final code passed 21 required Forge cases in eight sequential JVMs: capacity,
ordinary production, readers/phase, surface-index compatibility, real unrelated
single-chunk save, separate-JVM save/restart, and implicit production. The
implicit production suite passed functionally but encountered one
retained-memory admission refusal under the same 128 MiB Root limit. That
control is compatibility evidence, not implicit capacity acceptance. Selected
pairwise runs had no admission refusal or worker failure. Results are recorded
in the execution plan and development record. Nonblocking related persistence, automatic unload
overlap, endurance, actual clients/multidimension/default promotion and the
original approximately 1 °C objective remain separate outstanding gates.

Evidence: [paired observations](../../build/continuous-air-material-preparation-20261008.json),
[rebuild causes](../../build/material-preparation-rebuild-causes-20261008.json),
[execution table](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-09: remove repeated local work and distinguish log blocking from CPU

Pairwise unit volumes within one Component share a `GridProbe`. A refresh now
evaluates that probe once per consecutive identity group, then fills the same
factor for its volumes. The scalar lives only in that refresh; shared probes
do not hold mutable temperature caches. Different/null probes and non-grid
volumes retain individual evaluation. Four-block sampling, dot-product order,
clamp, source energy, cadence, nodes, precision and material/phase behavior
remain unchanged.

Confirmed hot integer loops in regional partitioning and `airAt` now use
primitive iteration with the original visit/union/sort order. Snapshot cell
centers use their already known Air cell and the original eight interpolation
terms. Generic queries still select their original incident cell; integer
face/vertex selection was not replaced with an enumerated-cell shortcut.

Four sequential real 256-source/600tick/realtime20 comparisons used one
compiled binary in A1/B1/B2/A2 order. Volume refreshes were 718429 in each
run; probe evaluations fell 718429→20430. Both matched temperature pairs
differed by at most 0.00313816 °C. CPU observations decreased 6.4%/32.4% and
allocation 1.3%/1.8%, but construction histories differed and log I/O interfered.
Those percentages are not a fixed whole-runtime speedup. The temporary
comparison selector and per-refresh mechanism logs were removed afterward.

PROFILE-only `frostedheart.ThermalCut` JFR events identify exact completed
cut boundaries and whole-pool resource intervals, counting helper threads
once. B1 target440 took 659.353ms wall time with 15.625ms pool CPU. JFR
directly records a 576.830ms Server thread write to `logs/latest.log`, called
by `GameTestServer.tickServer` INFO, while all thermal threads waited for the
same `RollingRandomAccessFileManager` lock for about 572–577ms. These waits
overlap and must not be added. This explains that cut, not every previous P99;
point-temperature logging is not proven to have caused the slow file write.
The local CPU timer has about 15.625ms granularity, so a single-cut zero does
not imply no computation.

Routine region/source/successful-graph traces now require PROFILE; time,
source-balance and admission anomalies remain visible. No global Log4j
configuration or new asynchronous logging framework was added. Temperature
observations and throughput runs are separated. PROFILE/JFR and GameTest's
own logging still affect the reported throughput configuration.

After the four short comparisons, nine final Forge JVMs passed 18 required
test instances: full two-base and A-only scenes, production, throughput,
readers/phase, implicit compatibility, real unrelated chunk save and separate
save/load JVMs. The first three groups validated the final numerical code;
the remaining groups ran after logging-only cleanup and recompilation.
Pairwise final runs had zero admission refusals and worker failures. Implicit
control passed functionally but repeated one known admission refusal above
128MiB, recovering within that cut; it is not implicit capacity acceptance.
Readers released 1703096B→0, powder snow still converted after 7017 supply
ticks, and the separate JVM restored STORED/material branch state and passed
the new passage check. Full-scene temperatures differed from the previous
current pairwise implementation by at most 0.002301280205 °C across 46490
matched samples; this does not close the original analytic approximately 1°C
objective. Version 5 and the ordinary backend default remain unchanged.

The unpaired final throughput scene omitted point-temperature logs. It
observed 3015ms worker CPU, 3749864824B cumulative allocation, a 62.891436MiB
charged Root peak, and 120 cut P95/P99=162.029/321.175ms. The original grouped
goals still fail: numeric-build cuts P95=170.053ms>150ms; no-numeric-build
cuts P95=66.412ms>50ms. Scoped input-tick P95/P99=0.265/1.425ms and full
GameTest processing-tick P95/P99=6.753/16.813ms do not certify real-client
MSPT. No 12000tick endurance or default promotion followed. Related-save
nonblocking behavior, automatic unload overlap, regional queue attribution,
actual clients/multidimension/CT2 and original analytic fidelity remain open.
Further B/C construction candidates and S persistence work are not delivered.

Evidence: [observations and gates](../../build/continuous-air-local-work-20261008.json),
[one slow cut's I/O evidence](../../build/local-work-B1-slow-440-20261008.txt),
[execution plan](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-09: retain dormant Air without repeatedly preparing unavailable Bricks

Regional preparation no longer treats any retained `DormantAirCut` as work
that can progress immediately. Genuine local source-response work still
triggers preparation; saved Air additionally triggers it only when
`savedMask & capturedMask & ~consumedMask` is nonzero. Uncaptured history
stays in the existing restoration map. Admissions, residency changes, actual
geometry, routes and mixing retain their existing wakeups. Captured but
unconsumed records remain eligible for retries. No new cache, array or
serialization field is introduced. `SectionEntry.brickMask()` exposes its
existing immutable mask, and the post-install completion check uses the
equivalent mask test in place of 64 individual bit checks.

The engine reuses its initial pending-work result for preparation diagnostics.
PROFILE-only cut JFR events include retained restoration-section counts and
whether spatial preparation was requested. Ordinary thermal parameters,
source J/W, nodes, double precision, 20tick cadence, material H/branches,
phase behavior, version 5 and backend defaults remain unchanged.

Four sequential same-binary full 2400tick/realtime20 two128 runs used ABBA
order. Both candidate runs retained the same one-section history but omitted
41 no-progress preparation passes: preparation cuts 117→76, actual numerical
build cuts 76→76. All completed Air cuts are classified even without a
REBUILD log, giving 43 no-build cuts in each comparison. Their P95 changed
80.353→34.601ms and 83.780→32.152ms. Worker allocations fell 4.33%/4.46%; Root
peaks remained about 62.891MiB. CPU changed 4171→3390ms and 2343→2656ms, so
a fixed CPU improvement is not established. Capture/construction histories
differ slightly. Numerical-build P95=200.231/141.271ms does not certify stable
150ms acceptance. The temporary comparison selector was removed.

Final code passed 12 required Forge test instances in seven JVMs, following
the four comparison instances: two actual machine scenes, A-only temperature
observation, production 5, readers/phase 2 and separate save/load JVMs.
All final workers and admissions completed without failure/refusal. Readers
released 1703096B→0, powder snow converted after the same 7017 supply ticks,
and the new JVM restored STORED/checkpoint H/branch state and heated a newly
opened passage. Across 47940 matched current-reference observations at 816
positions, maximum temperature difference was 7.105427357601002e-15°C.
That is current pairwise stability, not original analytic approximately1°C
acceptance. Functional lockstep timings are not throughput benchmarks.

Machine scenes reuse the existing two dense bases with 256 real campfires,
256 actual vanilla furnaces and staggered iron obstacles. Real input/fuel
and block-entity ticking cause start, burnout and restart; each scene produces
1024 iron ingots and retains all freshness/geometry/consumer/material checks.
The both-bases scene has Root64.018465MiB and build/no-build P95=173.049/64.530ms;
both internal goals fail. Its no-build preparation averages only 0.075ms,
while advance/publication average 27.162/14.479ms. The A-only source/passage
scene has Root63.756353MiB and build/no-build P95=274.243/31.265ms. This is one
real machine load tier, not every modded multiblock or actual hundred-client
capacity. It does not validate new machine heat-source integrations.

An A-only machine cut at target1420 took 1442.131ms. JFR directly records
1356.855ms writing latest.log from GameTestServer INFO and 1339.665ms thermal
coordinator waiting on that file manager from PROFILE source-cut logging;
pool CPU was 15.625ms. These waits overlap. The sample is retained, not removed
or replaced with wall time minus I/O. This explains one peak, not all P95/P99;
PROFILE/GameTest logging still limits throughput interpretation.

The remaining CPU candidate must distinguish advance, publication, true
reconstruction and diagnostic waiting. Optimizing changed-layout graphs alone
cannot close the observed complex-scene stable-cut goal. Related-save
isolation, actual unload overlap, endurance, real clients/multidimension/CT2,
original-reference fidelity and explicit production configuration/default
promotion remain open. No extra solver candidate or endurance run followed.

Evidence: [all observations](../../build/continuous-air-restore-preparation-20261009.json),
[matched temperatures](../../build/restore-preparation-temperature-envelope-20261009.json),
[this package's source diff](../../build/restore-preparation-final-source-diff-20261009.patch),
[execution plan](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-09: deduplicate identical face vertices inside one frontier check

`WorkerPageStore.continuousFaceResidual` retains the current Component/slot
and a 25-bit mask only for that function invocation. Consecutive patches with
the same slot reuse the Component lookup; component changes reset the mask.
Integer points already sampled for that component on that face are skipped.
X faces use their actual Y/Z tangent order, while Y/Z faces use their respective
coordinates. The original incident-cell selector, sky filtering, temperature
evaluation, maximum residual, 1.0/0.5°C frontier thresholds and requests remain.
No persistent cache, new array or cross-tick invalidation contract is introduced.

The existing PROFILE-only cut event now includes frontier duration, point
visits/evaluations, Component lookup count and an order-independent desired
request checksum. Diagnostics do not add per-point text logs. Same-field
legacy/candidate comparison ran only in temporary functional verification:
482142 actual face residuals were bit-identical across machine and reader/phase
scenes. The checksum is diagnostic corroboration, not an exact map-equality
proof. Temporary old/verify algorithms and selectors were removed afterward.

Five full 2400tick/realtime20 machine runs retained every slow sample. A long
wall-time interruption between B2/A2 required a fresh adjacent B3; the two
paired comparisons use A1/B1 and A2/B3, with B2 preserved as an unpaired repeat.
Both pairs had 18337928 point visits, reduced evaluations to 8159802 (-55.503%)
and Component lookups 4584482→536270 (-88.302%). Desired request checksums
matched 120/120 cuts. Frontier means changed 5.563→3.532ms and 8.640→4.626ms,
about 36.5%/46.5% lower. First-pair construction counts were identical; the
second differed slightly in async capture. Whole-worker CPU changed
3687→3515ms and 4390→2625ms, but the large variation does not establish a fixed
whole-runtime speedup. Allocation was slightly higher and Root remained about
64.02MiB. Whole-cut P95 worsened 122.565→138.388ms in the first pair and improved
187.199→159.248ms in the second. Local work reduction does not certify every tail.

Final code passed 11 required Forge instances in six JVMs: machine A-only,
ordinary A-only temperature observations, production 5, readers/phase 2 and
separate save/load JVMs. Including verification and comparisons, all 13 JVMs/
19 required instances passed without worker failure or admission refusal.
No JUnit or artificial heat states were used. Final temperatures matched 47606
current-reference observations at 816 positions with full candidate-current
coverage and maximum difference 0.00313815934922°C. These independent capture
histories do not replace original analytic approximately1°C acceptance.
Readers released 1703096B→0, powder snow still converted after 7017 supply ticks,
and separate-JVM STORED/H/branch restoration and new-passage heating passed.

Final machine A-only Root was 63.765867MiB. No-build P95=37.397ms passed 50ms;
numeric-build P95=298.614ms did not pass 150ms. Whole-cut P95/P99 was
174.208/321.342ms. Thermal input-tick P95/P99=.249/1.752ms and GameTest
processing-tick 5.453/13.847ms remain scoped observations, not actual-client
MSPT. Cold/slow records and PROFILE/GameTest logging overhead remain included.
Nodes, source energy, precision, cadence, material/phase, version 5 and ordinary
defaults are unchanged. This closes the bounded CPU candidate; related-save
isolation, stable numeric-build goals, explicit production configuration,
original-reference fidelity, endurance and actual-client/multidimension/CT2
acceptance remain open. No fourth CPU candidate was started.

Evidence: [all runs and paired results](../../build/continuous-air-frontier-dedup-20261009.json),
[temperature observations](../../build/frontier-temperature-envelope-20261009.json),
[this package's source diff](../../build/frontier-dedup-final-source-diff-20261009.patch),
[execution plan](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-11: retry optional native maintenance saves while thermal work is pending

`ChunkMapMixin_ThermalMaintenanceSave` redirects only the `saveChunkIfNeeded`
invocation inside vanilla `processUnloads`. It first preserves vanilla eligibility:
accessibility, a ready full chunk or its Imposter wrapper, dirty state and expired
save cooldown. A chunk requiring the existing thermal checkpoint is deferred
when its dimension has unfinished submitted work or queued continuations.
The original dirty/accessibility/cooldown state remains available to the next
native retry. A ready completion with no continuation proceeds through the
original method and barrier. No new retry queue, timer or checkpoint snapshot
protocol is introduced. `saveAllChunks`, direct saves, actual `scheduleUnload`
writes and stop retain their existing consistency barriers.

The initial 4000tick real Forge baseline recorded six maintenance checkpoint
waits over5ms, max227.9791ms. Of all seven recorded waits, mailbox blocking was
575.0831ms and main acknowledgement .2166ms. The longest startup wait required
data that was not ready, so removing the checkpoint barrier was not valid.
The first candidate had no maintenance event over5ms, while a deliberate save
retained its wait. That run also had worse worker P99,270.088→309.441ms, and its
initial retry observations included attempts under vanilla's10s cooldown.
The final eligibility refinement skips thermal ownership checks until cooldown
expiry and observes successful native saves even after thermal ownership retires.

Final code passed10 required production instances in5 JVMs: readers/phase2,
separate save/load1each, production5 and target endurance1. The final configured
SHARED_GRID run used normal128MiB capacity, two actual128-fire bases and256
operating furnaces for12000tick/realtime20,600005ms wall time and600cuts.
It made59845 thermal-consumer queries, produced3072 iron across six actual
recipes per machine and performed three actual Forge unload/new-Chunk reloads
with18/17/19tick recovery. Freshness/geometry/consumer/WORK_LIMITED violations,
worker errors and observed source imbalance were zero. Root46770154B→0.

Final JFR had zero maintenance checkpoint events above5ms and three deliberate
save waits, maximum73.4989ms. The scalar checkpoint maximum was73.549ms with
a slightly different timing scope. All25 deferred-save sequences on15chunks
resolved through native saves, longest3tick;51attempts and2810 successful native
maintenance writes were observed. No deferred sequence remained unmatched at
recording end. A native success submits NBT IO; durability is separately verified
by real reload and separate-JVM exact disk H/branch restoration. Absence of an
event above5ms is not a claim of mathematically zero execution time.

Worker cut mean/P95/P99=66.445/146.996/230.085ms, max328.998ms; creation-to-publication
P95/P99=141.401/223.175ms; charged Root peak72.965361MiB. Thermal main tick P95/P99
=.103/.413ms and GameTest server processing P95/P99=5.779/15.752ms. The processing
maximum4240.693ms is retained at scene-construction tick1. These are scoped
observations, not a fixed total-CPU improvement or real network-client MSPT.
Direct RecordingFile analysis retains main-thread method counts without
expanding repeated class-loader metadata into enormous JSON exports.

Powder snow still converted after7017 supply ticks and borrowed readers released
1703096B→0. Source power/distribution, grid nodes/transport, double, precision,
20tick cadence, material/phase laws, checkpoint version and ordinary backend
defaults remain on the existing path. An incorrect test namespace ran zero
tests and is not counted; the runner now rejects exit0 without positive required
test completion. All test server processes ended and configuration hashes were
confirmed restored.

This closes optional maintenance scheduling in the local target scene. Explicit
or forced saves and busy actual unloads can still wait. Actual unload pending
overlap was zero here and does not establish unload isolation. Actual clients,
CT2, multiple dimensions and default rollout still need their own acceptance;
the earlier region-isolated checkpoint candidate remains withdrawn.

Evidence: [delivery report](../../build/continuous-air-maintenance-save-20261011.json),
[recorded runs](../../build/continuous-air-maintenance-save-runs-20261011.json),
[final wait/retry observations](../../build/maintenance-save-final-endurance-summary-20261011.json),
[source diff](../../build/maintenance-save-final-source-diff-20261011.patch),
[execution plan](../../diary/2026-10-29_23-38-00_thermal-development-history.md),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-11: avoid redundant idle maintenance wrapper probes

The optional-save mixin now takes one scheduling hint at `processUnloads` entry.
Idle attempts use the original `saveChunkIfNeeded` eligibility/future/cooldown
checks once; the chunk is retrieved afterward only for a successful native-write
observation. Busy entry retains the previous per-chunk deferral. The hint expires
at the next invocation and never authorizes bypassing the actual checkpoint.
The normal loop does not seal new thermal cuts. Actual saves/unloads retain their
barriers, and numerical/source/material/precision/schema/default behavior stays
on the existing path. No queue or persistence protocol was added.

An instrumented4000tick/realtime20 before/after pair used real two128-fire bases,
256 furnaces, full forced flush,128 actual iron-block construction mutations and
world-tick-aligned ticket removal. Extra wrapper future probes fell2405900→94931
(-96.054%); idle probes2314518→1426(-99.938%). Idle/no-write loop mean759.292→760.393us,
P95=1201.2→1183.2us and P99=1462.7→1459.5us show no clear latency reduction.
Total loop5492.091→5056.519ms includes different native writes1758/1649 and unload
work, so this is a bounded operation cleanup, not a fixed whole-CPU speedup.
Temporary per-loop JFR events/counts were removed from final source.

The new scene confirmed actual busy unload waits89.877/97.841ms with22/21tick
new-Chunk recovery; that wait remains. Full forced save baseline455.757ms included
248.7375ms in ChunkStorage.flushWorker and57.874ms in EntityStorage.flush.
These native physical-IO barriers cannot be eliminated by removing thermal
consistency. CPU samples do not fully explain the remaining time.

Final uninstrumented-loop source passed the regular4000tick two-base scene1/1 and
production5/5 in2 JVMs. Cut P95/P99=117.338/259.293ms, Root peak66.286806MiB,
GameTest processing P99=23.108ms, regular unload recovery19tick, all recorded
freshness/geometry/consumer/WORK_LIMITED errors zero and Root42543760B→0.
All21 deferred native-save sequences completed within2tick with no unmatched tail;
maintenance checkpoint events above5ms were zero. Explicit save max20.337ms retains
its barrier. Configuration hashes were restored and test server processes ended.
The previous12000tick success remains historical, not rerun for this package.

Failed fixture compilation, the elapsed/world tick offset2 non-overlap run and an
aligned recovery retrigger assertion failure remain in history. The final fixture
uses world tick%20==19 plus a phase==0 guard. Busy unloading was observed without
worker delay or manufactured temperatures. Future unload scheduling requires
separate native-future identity, reload and forced-flush/stop validation; it is
not implemented here. Actual network clients, CT2, multidimension and default
promotion still require acceptance.

Evidence: [report](../../build/continuous-air-maintenance-loop-20261011.json),
[recorded runs](../../build/continuous-air-maintenance-loop-runs-20261011.json),
[final checkpoint observations](../../build/maintenance-loop-final-normal-checkpoints-20261011.json),
[source diff](../../build/maintenance-loop-final-source-diff-20261011.patch),
[development record](../../diary/2026-10-29_23-38-00_thermal-development-history.md).

## 2026-10-12: one maintenance decision per direct save attempt

`ChunkMapMixin_ThermalMaintenanceSave` consumes a call-scoped marker in the
direct `saveChunkIfNeeded` invocation after its optional-loop redirect has
already decided whether to defer. A `try/finally` restores the caller's context;
the marker is consumed before the save body, so saves reentered from that body
perform their own check. External non-flushing saves still use the injected
guard. Vanilla dirty/accessibility/cooldown rules, unload holder/future identity,
actual checkpoint acceptance and forced flush/stop barriers remain in place.
No queue, longer-lived busy-state cache, codec or thermal parameter was added.

The GameTest observer now uses one native `GameTestSequence.thenExecuteFor` per
observer instead of registering all future ticks in the task map. It preserves
the real scene's action ticks, per-tick probes, rotating100 consumers and
assertions. Tick observations run after scheduled same-tick actions; the old
task map provided no order between callbacks at the same tick. Cleanup still
starts one tick after all workload observers. `GameTestServerProcessing` JFR
events measure full test ticks before clock waits, and logs retain the actual
JVM heap configuration. This is test-only cost removal, not a production CPU claim.

The actual local4000tick/realtime20 control and each isolated production candidate
were repeated. The two final256-fire/256-furnace scenes retained all freshness,
geometry, consumer and capacity checks, recovered the returning base in22/21ticks,
had0 ordinary unload thermal wait and released Root ownership to0. Root peak
remains about66.8MiB. Main allocation decreased in both campfire comparisons;
aggregate CPU and GC did not show a stable win. Forced whole-world flush and
synchronous return of the whole base still produced hundreds-of-ms ticks.
These are separate from the small thermal snapshot stage and still need their
own measured optimization. Source/transport/precision/material/phase behavior,
checkpoint format5 and default backend/configuration are on the existing path.

Evidence and all failed coverage attempts are recorded in the
[execution plan](../../plans/2026-10-12_checkpoint-save-hotspots-next.md),
[capacity report](../../build/checkpoint-hotspots-capacity-report-20261012.json) and
[fuel control comparison](../../build/checkpoint-hotspots-fuel-comparison-20261012.json).
