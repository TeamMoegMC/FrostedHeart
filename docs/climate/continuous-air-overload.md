# 连续空气过载保护：当前状态与未通过的试验

- Status: `Transitional; current overload protocol linked below, historical trials retain their dated provenance`
- Last verified: `2026-10-22; current control contract and final 15-group/24-required qualification verified; dated trials remain historical`
- Scope: `SHARED_GRID；源发现上限、已撤回策略、显式区域粗细恢复、同cut按完成接纳、构建输入所有权与剩余隔离边界`
- Code anchors: `RegionalQualityControl`, `RegionWorkPolicy`, `MinimumThermalModel.InputPlan`, `MinimumRegionStep`, `RegionJob`, `RegionBuild`, `ThermalWorkerPool`

当前过载协议见[运行时控制说明](thermal-runtime-control.md)：`RegionalQualityControl`统一接收CPU/资源压力，最低模型按需构建，粗细共用`RegionJob/RegionalWorkQueue`。内存压力允许小房间粗化，不靠取消其他区域准备或重新捕获世界推进。默认内存准入目标128MiB、全池累计CPU250ms/s保持，CPU可配置上限改为4000ms/s；完成占用可以超过内存目标并继续记账。最终生产r17的15组/24项流程资格通过，千源、64→128压力及多维度的空读、年龄、峰值和恢复等待单列，不把流程通过当作压力全程零空读。

## 历史实现与试验

以下“当前”“未通过”和固定预算描述属于其标明日期的旧批次；新控制协议、最终同构建结果及粗档年龄/空读代价以当前运行时说明为准。

2026-10-21控制层替换以[输入、任务与保存所有权](thermal-runtime-control.md)为当前阅读入口。源窗口已从Page运输退休条件中移出，已接管J统一归Store，粗细计算共享Claim/取消/提交协议，质量意图与在途候选由独立控制者管理。当前已完成源码/删除清单核对及统一编译，真实运行矩阵尚未完成。后文按日期保留前批行为与失败证据，不能将旧入口描述或历史通过结果转用于本批。

## 2026-10-20 替换批次的当前入口

后续`thermal-checkpoint-owner-closure-20261020-r4`已经结束：9项required中5项失败，72次累计拒绝后sample6560持续不动，至少到输入9840；停服计费资源剩524288B。同期tick6560结构项的区域条件已满足、来源条件未满足，部分源前缀又被6560结构边界限制，见[当前推进状态](continuous-air-regional-preparation.md#当前状态)。尚未证明具体否决来源及后续场景是否受前一个失败状态影响，因此不新增独立缺陷计数，也不将独立保存1/1、root0写为过载恢复通过。

后续完整回路试验新增两类已据源码修正、仍待最终验证的交接问题。第一，`SpatialHeatInbox`对每目标最多64条精确记录维持时间顺序，同起点的即时输入先行，部分路由后剩余区间也有序插回；原先后面的到期输入可被队首未来区间堵住，形成routing/结构/来源三者循环等待。第二，构建因预算从FINE改为最低档重试时，`RegionBuild`持有的父Accepted数组会重新交给solver；`resumeProjection`更新到本次恢复对齐时刻的父数组，release才清除。不能因为`abortPreparedRebuild`释放借用指针，就退回读取最低档已经释放的fine向量。

独立结构任务的暂存容量也按共同峰值处理：`ThermalSolver.StagedEdge`标记新增物理键，全体尚未落表的插入数在preflight和实际private stage共同预付。预算拒绝发生在commit前，提交后的cleanup不增长表，游标在实际finish成功后才前进。保存封存阶段与材料容量通知由[保存文档](continuous-air-persistence.md)维护；r1/r2/r3真实失败和最终版本资格由[工程计划第29节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)维护，未将越过原停滞点当作整批通过。

**r15恢复验收失败：** 真实数量和内存拒绝各返回一次WORK_LIMITED后，温场仍停在sample5300/5320，输入已到8400，热线程空闲。下面描述的是已接入的实现，不能视为超限恢复资格；后续intake/结构边界等待仍在定位。r13仅证明独立跨背景场景通过。

本轮已确认并修正三处所有权/选档缺陷：纯材料候选恢复可以读取多个旧空气区域的材料；已交接完源窗口和结构描述的入口不再等结构物理完成，`Receipt.detach`让压缩摘要释放无消费者的回执；`ThermalWorkBudget.Account.policyCpuNanos`用累计实际执行墙钟限制Windows粗粒度CPU对单个短任务的过量归账。原始CPU/JFR与全池信用仍按原值记录，选档费用上界不包含排队时间。r21独立控制保持精细档、样本通常不超过21tick，只有1次整维度发布seqlock导致的空读；当前区域点温度、slot与材料读已改用单个volatile Accepted不可变frame，实际几何和材料身份检查保留，IR/保存lease不变。r23/r24选定3项连续通过，r24另完成3次压力期间真实原版保存；r22一次保存崩溃仍未解释，完整容量资格未授予。

当前SHARED_GRID以独立区域输入和`RegionJob/RegionBuild`工作运行。最低档为64空气池、64材料池及至多32显式相变体；`MinimumThermalState.Background`由冻结几何/自然基线派生，温度查询为背景加额外温变，不保存一份旧fine残差。最低成对推进按背景外温变交换等量J，驻留和外扩使用同一扰动定义。r13单场景通过不构成过载误差或CPU截止的全局保证。

空间放不下时，`GeometryWork`先保留已有空间更新，把新增空间交回既有main退避重捕获；缩小批次仍放不下则退回整份几何描述，并为已有区域请求最低服务。源J仍归稳定空间输入所有者，phase控制不重放。数量限制不再一律等待内存变化；真正的内存失败先释放候选并有限重试。`TopologyRetry.storage`将通知计费保持到主线程ACK或关闭，最多一个待交给mailbox的retry；mailbox仍最多3条通知。ACK会唤醒等候工作，发布后只能继续清理，不能撤销已提交物理状态。完整实际容量资格仍在验收，详情见[当前工程合同](continuous-air-regional-preparation.md)。

以下段落保留2026-10-17/18的旧实现及原测量口径，不能用来授予当前替换代码资格。

## 协调轮与完成通知

2026-10-17协调轮/有序通知有限验收完成；完整硬隔离未实现。

随后区域执行调度已替换为`RegionalWorkQueue`：普通与构建检查点共用每区域一个排队/运行工作，删除`SolveRound`任务集合、独立串行回退和`advanceStableRegions`计算循环。真实单核首轮32次不可用后，将结构不变的普通推进/发布服务也接入同一循环；修正后231样本max25tick、不可用/几何0，两个依赖负向通过。最终normal/native功能通过，CPU增长仍未关闭。输入边界资格仍存在，维度物理批次及有界传输尚未改成逐区域合并接管；它不构成完整过载保护。合同见[单一区域工作循环](continuous-air-regional-preparation.md#2026-10-17单一区域工作循环)，失败、JFR日志长尾及CPU数据见[本轮报告](../../build/region-work-results-20261017.md)。

`ThermalDimensionProcessor.processRound`返回null仅表示本次协调轮让出执行权，既不发布半个拓扑，也不产生物理完成通知。`ThermalDimensionEngine.CutRound`保存本输入已登记、已结算的阶段，续跑不重复供热或应用phase意图。`TopologyUpdatePlanner.prepareRound`保留私有draft，单轮最多一个Page的单元/迁移、16片段；`MaterialEdgeCompiler.compileRound`按16片段、256边、16执行owner分批，原排序与累加顺序保持。

区域推进和构建仍用原`ThermalAssemblyTask`与共享pool；每轮最多认领一个未启动区域任务，其他helper运行期间通过`whenRoundReady`的实际完成回调再调度，不用忙轮询或单worker等待自己的队列。数值候选继续冻结几何/源并走原install/abort；构建期间不能开启另一份planner事务。单个区域内部数值运算、全局partition及排序等仍存在不可中断区间，不能仅凭有轮次接口宣称CPU硬截止。

mailbox最多保留`ThermalCompletion.MAXIMUM_PENDING=3`份按序物理完成通知；worker可开始下一份已准入输入而不等待上一份main ACK。main在一tick中最多应用3份，phase/resync/驻留/retry不合并覆盖。`ThermalInputStore`统一管理输入与预算：最多2份QUEUED等待输入，开始后转ACTIVE。2026-10-18起，转COMPLETED时清空完整batch并缩减到Submission/TopologyRetry预留，main ACK释放剩余摘要与重试数据；main自身只持有区块摘要。已完成项不占等待槽，总保留上限为5个所有权条目，不能把三个状态上限叠加。载荷寿命与[摘要/重试合同](continuous-air-regional-preparation.md#主线程区块摘要与重试载荷)一致，排队/物理完成门槛尚未删除。SHARED_GRID首份输入也在保留前计费。`lastCompletedTargetTick`只随物理完成通知推进，接收和yield不推进它。查询容量回收在下一个worker cut入口执行，避免main收缩正在准备的新候选预留。通知上界加入原Air backing启动预留：3×(1024+128×最大phase请求+128×最大Page)，正常配置1,293,312B/维度；不是增加热预算。

该输入所有权/重复提交清理子项已完成源取消1、production/native8、正常256全FINE1和实际单E核一般输入1，共11个required验收；两次夹具覆盖/交互失败原录保留。最终单核3/3/3接收/处理/ACK、含前批运行时实际UNLOAD、余额0/关闭Root0。两份完成待ACK再准入的角落未触发；整维度活动cut与提前服务资格仍在，尚非逐区域合并/长期隔离。正常延迟改善、CPU与内存不利项及具体口径由[本批报告](../../build/input-store-results-20261017.md)维护，不据此推广完整自动保护。

详细区域计量由`ThermalWorkBudget.recordWork`写入`frostedheart.RegionalThermalWork` JFR事件；整轮CPU/轮次写入已有`frostedheart.ThermalCut`。不再每区域提交同步输出`THERMAL_REGION_WORK`，也不每cut输出`THERMAL_WORK_CYCLE/THERMAL_COORDINATOR_CUT`。来源是实际正常r2的JFR：1400cut在`acceptStep`→Log4j写latest.log约563.897ms，server同时等待该manager约515.730ms；1220cut同一路径593.565ms。它们是重叠费用，不能相加，也不能把全部慢cut归为日志。故障和档位状态变化日志保留，最终对照由[本批报告](../../build/coordinator-rounds-results-20261017.md)保留。协调轮可迁移worker，因此旧离线工具把整个cut的eventThread park当作coordinator等待的统计不再有效；须看实际执行栈或轮次/CPU数据。

自动额度修正：`RegionalAirRuntime.observeWorkCycle`仅在累计执行wall超过这8cut的8秒到达窗口时，才用实际服务比例收缩既有CPU额度。已在到达窗口内完成的工作，仍使用原250ms/worker的总CPU额度；不把短工作里的日志/GC/等待比例当成持续供给不足。正常r3揭示1340cut因此错误切粗并造成3.734°C观测差，required新鲜度通过不能代替正常FINE资格。最终入口增加原正常不降档后验，normal r4全程降级0、45594匹配点max0.157915635°C；单核过载仍需自己的真实CPU成本和积压。

限定实测：一般输入单E核r2 1/1、7接收/处理/ACK且7次后输入在前main ACK前启动；pending专项r3 1/1、健康max38/不可用0/两个负向；最终单E核完整257火/256熔炉/100消费者1/1，健康FINE/current max22、基地24/23、11400查询超龄0、余额0，Root69.633MiB/关闭0，982亲和性全0x800000。最终production/native8/8、源取消和粗细恢复各1/1。正常最终cut P99590.921ms、完整main P9960.358ms仍较参考不利；多维度首40tick冷建失败，尚未到竞争阶段。全部失败、温差与计量口径见[最终报告](../../build/coordinator-rounds-results-20261017.md)，不据此推广自动入口。

本批未改变热参数、double、网格/成对换热、人体衣服或v5。完整的一般源事件隔离、最低档工作上限与粗档生命周期仍按[唯一计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)验收。自动策略仍只在原开发入口启用。

## 普通游戏实际行为

用户最新澄清的恢复目标是资源允许后重新构建精细场，并从当前粗温场接回计算；不要求粗档期间长期保留历史精细残差。原计划中的FineFieldAnchor/复合温场保留合同已撤回，未接入普通游戏；不新增长期残差内存、残差换热或存档字段。粗档可能丢弃细小历史温差，恢复后由当前热源和边界重新形成空间细节。显式区域切换的实现与验收进度见[执行计划的当前修正](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

普通配置SHARED_GRID使用既有精细网格、区域pairwise推进和局部材料换热。没有按节点/篝火数量自动降级，也没有已通过验收的超时强制降级。`continuousAirMaxRegionNodes`配置项与4096×worker自动触发已经撤回；不能继续按旧文档称其生效。

保留源发现的工作量限制：每tick最多检查256个BE位置，每个活动区块最多32个；最多8游标、每tick最多1个新位置集合副本。副本包含活BE和packed NBT，卸载/关闭清理游标。原版集合复制仍与BE数成正比，这不是CPU硬截止。新的目标兴趣分批准备/延后供能注册候选已经撤回，普通供能时间规则保留。

## 未发布运行框架的统一重构计划

用户已授权整体重构。2026-10-18源码复核确认此前小步迁移没有删除固定窗口/整批屏障，[原统一区域运行设计](../../diary/2026-10-29_23-38-00_thermal-development-history.md)保留对应调查。当前施工以[区域隔离完整工程方案](../../diary/2026-10-29_23-38-00_thermal-development-history.md)为准：输入/源时间线、区域job、必要长阶段、结构提交、派生维护、固定局部保存与关闭放在同一代码批次，源码与回路复核后才实施，最后统一验收。该整体替换尚未实现，本文件以下源码事实和历史验收不能作为新运行链的通过证据。

## CPU归属与整区域自动档位试验

2026-10-16新增开发入口`FH_CONTINUOUS_AIR_WORK_POLICY_TRIAL=observe|auto`。`observe`仅计量；`auto`在SHARED_GRID/pairwise区域路径启用`RegionWorkPolicy`。普通环境未设置时仍精细。新入口启用时旧`DEADLINE_FALLBACK_TRIAL`不同时运行。以下规则已实现并通过一次实际单E核闭环，不能视为全部生产保护通过。

`ThermalWorkBudget.Account`归属现有任务；`ThermalAssemblyTask`将不可变阶段上下文传给helper。服务其他区域时暂停候选计费；等待和OS未调度期间不记作CPU。源认领、材料输入、空气/接触/算子/投影、推进、接纳和发布分别计量。`THERMAL_WORK_CYCLE.cpuNs`包含后代，阶段与`exclusiveCpuNs`为公共独占费用，不能将周期总量与区域量再次相加；`helperCpuNs`含实际其他线程CPU。缺少CPU支持、计量准入或账户容量有`THERMAL_WORK_UNAVAILABLE`，不当作零费用。

计量元数据先在原预算准入：周期及分区后的保守额为`8192 + 1024*(1 + 3*已安装区域数 + 2*候选区域数)`B，随周期释放。历史只保留成本/退避标量。Windows CPU样本约15.625ms量化，推进用8cut聚合，0ms不代表没有工作。

2026-10-17追加一次正常双128源诊断：500次真实区域推进均为20tick/1步/0拒绝，480次原生`GetThreadTimes`差值为0而`QueryThreadCycleTime`均非0。相同生产逻辑下记录池CPU1406ms、cut均值111.775ms；前轮为5468ms/107.163ms。短周期任务的粗粒度CPU计量与墙钟方向并不一致，不能用阶段93.750→1250.000ms断言材料工作量增长13倍，也不能用这次低CPU宣布优化。诊断没有调整现有策略，自动份额仍需更可靠计量资格；[原生计量与空扫证据](../../build/region-work-ms-root-evidence-20261017.md)保留观察开销、GC和完整归因限制。

试验份额先取每个热worker每20tick间隔250ms CPU，在已注册维度间分配；再用最近8cut的CPU/执行wall估计实际服务率并留25%余量，扣除公共工作和无空气场材料区域费用，分给空气区域。每区域最低8ms保护量化噪声。等待影响可用服务率估计，但须区域自身CPU超额才触发推进降档，不直接按wall、热源数或节点数判定。精细准备额度扣除已测推进费，最低62.5ms（四个CPU量化单位）。这些是开发策略参数，不是全服硬CPU上限。

已有成本先选整区档位；预测失误时只取消未发布且账户匹配的精细准备，再完整构建一次C4。图暂存拒绝时先降低最大精细候选再核算；全部C4仍不够则保留拒绝。取消CPU计入总量，已发布状态/已确认源J不撤销。提前算到未来的区域须等普通输入追到接受时间再替换质量；自动模式早期拓扑服务只排除待切档区域，无关区域仍须通过几何/源出口证明。

C4只推进当前粗系数和材料H/branch。两个连续低费用窗口及至少600tick后尝试精细；恢复失败或恢复后再次超额时退避翻倍，最多9600tick。从当前粗场和当前时间投影并重建细网格，不保留旧精细场或历史残差。

这不是完整硬隔离：C4完整准备/推进、公共拓扑、长库调用、一般事件/未知路径与外层completion/ACK仍有边界。最低份额和一次精细加一次粗档不能保证全cut固定额度；大材料网络在C4中仍逐体内导热。粗档卸载、独立JVM重读、合并/分裂和多维度压力资格尚待。

自动r1真实129源（小房间1+远处基地128），固定480tick内每40tick实际拆放石柱，之后停止施工；没有手动档位请求。target960的精细候选实际CPU93.75ms越过83.333ms额度，原私有候选撤销，完成C4（该替代准备CPU15.625ms）；量化/检查点使退出不是精确83.333ms抢占。小房间的新鲜度观察从400tick预热后开始，不把它称冷建隔离证明。实际接受48个C4 cut、恢复19个精细cut，小房间max age27tick、年龄/几何违规0，源余额0、时间回退0，三个同时间投影点max1.78e-15°C，Root峰38664502B、关闭0。PID13512实际processors/worker各1、原堆4084924416B、Copy/MarkSweepCompact；966次进程/282次server亲和性采样全8388608。计量不可用/worker失败0。一般未知路径/跨JVM与单核256完整生命周期仍未由该场景证明。

## 未完成通风与页面恢复的局部边界

2026-10-17，`TopologyUpdatePlanner.continuationBoundary`在通风搜索未完成时，预准入一份保守section包络：当前/候选/本输入Page、实际halo位置和既有路由依赖，从全部pending搜索/未提交dirtyBricks及输入影响范围沿捕获section六面扩张。整个section按可通行处理，会多等待一些实际不相连的空间；包络外的区域还要通过原源出口、质量交接和先前输入依赖证明。可续4096访问的原搜索继续，不提前完成路由，也不为包络建立温度矩阵。

`hasPendingWork`覆盖已完成未提交的路由，避免候选拒绝后过早解除本地等待；`includesUnresolvedRoutes`只记录真正未完成搜索。临时footprint set/队列增长在原reservation内准入，拒绝时继续等待，128MiB保持。`prepareTopology`可在该证明下服务远处区域，源目标仍由`FrozenSources.targetsUnchangedAcross`逐面检查。

真实首轮进一步证实驻留更新携带恢复数据也在全局挡路：sourceEvents=0、residencyUpdates=1仍steady=false。共享网格改用`steadyContinuation(true)`，以恢复Page/region依赖保持其物理安装顺序，同时允许边界外区域推进稳定供热；不提前消费恢复H/C/branch或推进main完成水位。无参及非GRID_PAIRWISE表示保留原恢复门禁。注册、卸载、启停、非正功率、脉冲和未知/受影响源出口仍在原源边界。

真实单E核257火、双基地同时施工及扩展大厅，最终required1/1：在未完成路由期间跨24700结构输入发布24720，6400J一次确认、其他1638400J待办保留、余额0；84个健康样本max34tick、不可用/几何0，出口修改和本区修改两个负向均通过。570次亲和性全0x800000，原堆/GC、Root0。首轮required失败及9次不可用保留，不以仅有效样本max40代替时效通过。

正常多核256源/256熔炉/100消费者回归通过、12000查询违规0、降级0、最终FINE42043；同点/实际sampleTick45509个观察最大差0.157916°C，无>1°C。cut均值82.201ms、完整main P9929.122ms、Root峰84101880B/关闭0；观察堆峰较前轮高，独立JVM不宣称固定因果提速。原版保存/卸载8/8和真实单核一般输入1/1通过；8份接收/处理/ACK、2份一般输入在前一批执行时接收、pending0、余额0、Root0，612次亲和性一致。

完整来源见[本批报告](../../build/pending-route-boundary-results-20261017.md)。公共长工作/硬CPU配额、一般源事件、逐区域拓扑及外层completion/main ACK仍待；两份输入只有有限领先能力，一个长期不结束的任务仍会用尽窗口。下一项落实有界协调轮和真实完成/通知水位，不能把本子项称为任意堆源保护。

## 真实单核双基地在线容量

2026-10-17完成固定在线场景：两处各128篝火加一处1火健康房间，共257火；完整轮另有256台真实熔炉和100个热消费者。全JVM从启动实际绑定单E核8388608，processors/thermalWorkers各1，原堆/GC和128MiB预算；884次亲和性观察一致。400tick预热后固定稳定供热、单基地施工、实际灭火/重燃、双基地施工和停止施工五窗口，不手动选档。

完整轮required1/1，健康房间始终FINE、geometry_current，max age29tick、年龄/几何/质量违规0。两基地主探针max age29/33tick、不可用0，最终16探针有效且40tick内；施工基地仍会短暂page_unpublished，不宣称其所有位置全程几何有效。11400次真实玩家环境入口查询超龄0；实际熔炉产出1024铁锭。源余额/时间回退0，在线degradedLoss增长0，WORK_LIMITED/worker失败/账户缺测0；Root峰51165760B（48.795MiB）、关闭0。100消费者不是100网络连接。

本批只细分MATERIAL_INTERNAL（含beginStep初始化拷贝）、MATERIAL_CONTACT、MATERIAL_STATE的CPU计量；不改换热、热参数、double或v5。安静窗口完整后台cut平均54.971ms wall/34.598ms CPU；双基地施工平均736.068ms wall/87.240ms CPU，最大1505.352ms。固定目标下可以运行，不是硬CPU截止或任意堆源保护。冷建/预热最大cut4091.256ms，未验收冷加载隔离。

对比旧材料CPU总项时，须求和MATERIAL与三个新增分项；各分项互斥。周期CPU包含子账户，不能再加一次区域CPU。0ms仍受Windows线程CPU量化影响，不表示没有工作。

完整主线程在线P99仍有不利项：稳定窗口211.237ms、施工349–353ms、停止施工93.176ms，最长1109.253ms；不能称稳定20TPS。离线记录显示长tick与编译/单核调度有重叠，也有原版区块和热捕获工作，全部因果未闭合，不将其简单全归JIT或热系统。最终显式关闭tick单列。全部原始数据、口径和不利结果见[完整报告](../../build/work-capacity-results-20261017.md)。下一关是公共/一般事件/未知路径与真实完成边界的有限工作；当前自动策略仍为开发入口，完整粗档生命周期和正式推广尚待。

## 粗档材料面的单点换热

最终固定回归全部通过：普通256与开启策略正常256各1/1，后者所有诊断降级0、FINE42043；production/native8/8、取消重算1/1、显式粗细恢复1/1，均关闭Root0。开/关45217个同点/sampleTickmax0.00284768°C，普通对前轮45132点max0.157763°C，无>1°C观察。性能有明显不利变化，离线JFR未证明新增预算计费为主热点，但没有闭合全部因果，也不宣称总体加速。[完整报告](../../build/work-policy-results-20261016.md)保留指标、全部运行、JIT/GC、不同监控边界及下一关停止条件。

首轮真实单E核129源成本观察r2为1/1、Root0。柱体增加窗口各12次推进，FINE区域CPU125ms、旧C4为203.125ms，后者几乎全在材料阶段。因此仅C4改为每材料面一个面中心点、全额G；FINE仍为四Gauss点、各G/4。`LocalMaterialBoundary.surfacePoints`同时约束构建与旧面复用检查。

单点保留总导热能力、材料独立H/branch和成对交换能量记账，但改变面内反馈和有限容量交换的分步近似，属于过载档热行为改变。源世界面位置/W/J、正常精细参数、double、材料热律、人体衣服和v5不调整。

修正后r3真实单E核1/1：小房间max age35tick、年龄/几何违规0、源余额0、Root0；C4接受25cut后恢复精细13cut，同时间三个位置投影max3.55e-15°C。材料5957体/空气456变量的相同入口布局，接触数18472→4618、contactTerms122486→31814、最终int/double暂存1920780→569124B，该数组不是全部候选或全Root。

CPU有明显独立运行波动：r3柱体窗口FINE/C4分别296.875/171.875ms；恢复后FINE的12cut也为171.875ms。不能据此承诺稳态CPU固定加速。中心近似对长期全场曲线尚无配对误差上界；恢复三个点的投影误差不是粗档演化误差。原始数据由`build/thermal-work-policy-observe-20261016-r2.log`、r3同名日志及对应`.analysis.json`保存。

## 显式直接四格对照

`ContinuousAirSolver.selectRegionalGridStep(x,y,z,step)`现提供仅协调者使用的显式选择入口，step为1或4。选择属于当前物理region；`RegionalAirRuntime`在下一次候选准备中选完整区域档位，成功安装后清理请求，废弃候选不丢选择；普通游戏没有自动调用。几何替换继续继承父区域档位，合并选择确定，不为每个基地新增线程。原环境变量只决定初始试验档位。布局、体积片段、图准备费用与contacts容量选择均读区域候选的step，不再只读进程常量。

恢复使用当前已接受粗系数、当前时间和现有nodal投影，重建精细网格，材料保留当前H/branch。不保留历史精细布局、第二温场或残差。粗场和精细场继续使用现有v5空间checkpoint；已验证当前发布数据的捕获/编解码，未以此代替粗档跨独立JVM重载等完整P5验收。

真实两个房间的一处FINE→C4→FINE，多核及实际单E核各1/1。目标变量3204→125→3204，另一处solver保持精细并复用；粗档经历灭火及石柱拆放后，恢复同时间的八Gauss点/实际空气体素内最大转换差约8.88e-15°C，切粗最大1.616572°C。这个恢复精度来自同一Q1函数的重新采样，不是提高停止精度。材料H/branch、源账目保持；真实候选abort/重备通过，checkpoint最大差1.78e-15°C，各自停服Root0。首轮测试错误重复认领外层FrozenAirGeometry，导致观察器失败和一次worker失败；修正为数值候选内重备后才通过，原失败保留。

单核PID18368报告processors1/thermalWorkers1，924次进程采样、其中283次server采样均为mask8388608，原堆/GC。首次切粗时布局与算子已计费backing为1028752→644408B，少37.36%；不是全Root/总堆或CPU减少比例。普通256源回归2400tick通过，原年龄/几何/接收/WORK_LIMITED违规0，degradedRegions0、fineAirNodes42043、关闭Root0；实录mean75.750/P99390.648ms，不能据此宣称总提速。原始数据见`build/thermal-regional-grid-switch-20261015-r2.log`、`build/thermal-regional-grid-switch-single-20261015-r1.log`、`build/regional-grid-switch-normal-comparison-20261016.json`。

`FH_CONTINUOUS_AIR_GRID_STEP_TRIAL=4`且实际选择fine-grid/local/pairwise时激活 `DIRECT_COARSE_GRID`，普通配置不启用。它复用 `AirFieldCompiler` 的共享四格八角点几何，跳过单格节点身份构建；每个实际捕获空气分支构造一个八节点CV片段，空气热容按实际体素权重累计，部分空气/增强混合导热按体积比例近似，沿用成对换热与现有局部材料/源/查询。

它改变试验温场的空间离散和传播/换热曲线，没有恢复按热源叠加局部函数；源W/J、有效热参数配置、double、材料热律/潜热及v5格式未改。日志 `gridStep=4`、查询质量span4、`fineAirNodes=not_built`表示未构建细网格，不虚报精细计数。环境变量仍是全场初始档位选择。区域候选现可显式选择档位，已安装场支持同cut按完成收据接纳；自动超时选择、独立拓扑和外层ACK尚未完成，不保留长期精细残差。

已完成两轮多核粗档与一轮当前细控制，均为真实2400tick、两128源/256熔炉/100热消费者；单核首轮兼容8项通过并实录Root0。最终粗档相对细控制的热池CPU少约25.9%、已计费Root峰值64.11→48.99MiB，但主线程CPU增加、主线程加热池总CPU近乎持平，不能声称全服CPU已下降。同采样tick46944个共同观察的温差均值0.655/P951.856/max3.706°C，不是全场界限。普通容量case没有关闭Root0断言，不能与单核关闭证据混用。

JFR定位到Contacts权重数组扩容，后续修正仅在粗档规模未缩小时按旧实际term数量预分配，保持权重/顺序。额外 `contactTermScratchSumBytes`只计各准备区域最终int/double term数组容量，非完整Contacts瞬时峰值；全部临时预算仍未闭合。详细限制、首轮不利结果和单核容量状态见[有限报告](../../build/direct-coarse4-pilot-results-20261015.md)。

最终粗档的实际单核256源容量仍失败，保留原40tick恢复断言；在重载前也已有年龄/几何违规，WORK_LIMITED0，停服Root0。重载窗口热线程等输入约5.892s，加载相关主线程完整tick约4.749s；随后target2420约1.160s wall/187.5ms池CPU，完成晚于失败。初期炉子启动后也有1122变量但3.315s wall/312.5ms CPU的cut。数值缩减未解决整条输入/提交与调度路径，不能把本轮称为单核256源通过或自动保护交付。

## 区域结果及时接纳与本轮验收

2026-10-16新增同cut按完成接纳：`ContinuousAirSolver.solveRecordedCut(cut, airReservation)`为有界收据队列准入`512+160*N`B，`RegionalAirRuntime.runRegionalTasks`在唯一协调者上接纳实际完成任务。helper仍只计算/发布原快照，提交H/branch、确认source assignment和completedSequence不再等齐其余任务。同档按已捕获cell数加空气变量数优先处理小任务，规模不触发降精度。准入失败沿用原等待路径并记录`acceptanceScratchRefusals`；外层ACK/拓扑安装/跨区间输入仍保留。

真实小房间+128源基地多核1/1；排序修正后的最终生产实现实际单E核1/1，PID15008、processors/worker均1、原堆/GC，1035次进程/其中266次server观测均mask8388608。小区域6400J及792个材料H/branch已提交时，被观察的7814变量区域尚未完成，其余源待确认819200J，余额0。7814变量是128源基地中的一个物理区域，不是整个128源强制连通；这是同cut提前接受证明，不是单核256容量/全服隔离证明，相关关闭Root0。

生命周期首轮7/8与独立native复验2/3的失败已保留。只读观察确认：目标holder ticketLevel46、save future已完成、可见缓存已退；旧GameTest时钟前599次原版`processUnloads`循环的spareTimeLoops全部0，pendingHolderLoops593、实际回调队列最高1030，热保存请求因此尚未开始。源代码`ChunkMap.processUnloads`只有haveTime或队列超过2000才排空回调；测试END时钟丢弃追赶债务后，没有同步`MinecraftServer.nextTickTime`，导致原版维护预算饥饿。

仅测试端`ContinuousAirGameTestPacing`在实时START将原版deadline推进到`max(oldDeadline, Util.getMillis()+50ms)`，保留原haveTime supplier、END等待、真实tick、40tick与不等待热worker。`GameTestNativeClockAccess`和只读卸载循环观察均只随GameTest加载；正式server/client保存机制没有更改。`realtime20_legacy`保留旧时钟失败复现。修正后生产/native8/8：两个卸载场景分别22/31tick、真实requests8/9、prepared8/9；返回场景5个cancelled、pending0，所有thermalWait0，停服Root0。完整证据在[时钟记录](../../build/native-unload-clock-evidence-20261016.json)。

修正后的时钟会执行原先未获得预算的原版维护保存，新的普通负载性能不能与旧clock的零维护保存数据直接作成对提速比较；最终实录由[本轮报告](../../build/regional-acceptance-results-20261016.md)维护。区域请求/接纳子项均不自动开启过载选档，仍须完成输入接收与物理完成水位拆分、受影响闭包及有限工作/恢复退避。

最终源码与对齐测试时钟的两处128源、256熔炉、100热消费者、2400tick/120cut通过1/1。12000次查询中年龄/几何/接收/WORK_LIMITED违规均0，最高完成年龄15tick，degradedRegions0、fineAirNodes42043、acceptanceScratchRefusals0；停服Root0。主要实录如下，P99指该轮99%观察不超过的值：

| 指标 | 最终观察 |
|---|---:|
| 完整后台cut均值 / P99 / 最大值 | 73.383 / 345.854 / 667.930 ms |
| solve / topology / 后续与发布均值 | 18.240 / 45.647 / 9.495 ms |
| 主线程热回调P99 | 1.340 ms |
| 完整主线程处理tick P99 | 28.682 ms |
| 原Root已计费backing峰值 | 83530448 B，约79.661 MiB |
| 热池累计分配 | 3565004496 B |
| 原版维护保存次数 / 热等待计时 | 1617 / 4 µs |

Root是128MiB预算内的计费占用，累计分配和整个JVM堆另算。完整tick包括该测试中的原版及其他工作；后台cut是一次完成延迟，不能作为主线程独占时间。旧clock接纳改动前后匹配47114个位置/实际sampleTick观察，绝对差均值0.000408/P990.006033/max0.108990°C；最终对齐clock相对旧clock改动后匹配46816点，均值0.000981/P990.009425/max0.129099°C，两组均无>1°C观察。独立捕获/保存/调度历史仍不同，不据此证明全场误差界限或固定CPU收益。最终取消重算1/1、混合粗细切换1/1也通过；100热消费者不代表100真实网络连接，未再宣称单核256源容量或自动隔离通过。

## 有界输入接收与实际完成

2026-10-16进一步将原两份续传的接收与提前推进资格分开。2026-10-17以`ThermalDimensionEngine.reserveInput`和mailbox共享的`ThermalInputStore`替换重复的offer队列与reservation表；QUEUED/RUNNING/AWAITING_ACK期间可接收已计费的一般输入。注册、熄灭、卸载和dormant恢复输入在自己的正常process中按序解释，不提前确认J、清除dirty/resync或推进完成时间。`pumpContinuation`仍执行原steady/正向源身份/priorBoundaryReady限制，当前一般队首未处理时不绕过它。

输入token从接收保留到主线程实际应用phase/resync/驻留/retry后的ACK；等待队列仍最多两份，完成待ACK载荷单独受总保留上限约束。注册端口及完整dormant空气/材料数组加入payload计量；提前推进所需源cut暂存在真正dispatch前扩充，每次以原始payload加本次派生量为目标，避免重复尝试累加同一余量。扩充不足则保留输入等待正常处理。关闭先让processor及正在读取的helper退出，再由store释放剩余输入。此项没有新线程池或第二套温场；当前store仍保存有界整批输入，尚未实现统一计划要求的逐区域合并待办。计量及具体边界由[区域准备](continuous-air-regional-preparation.md#区域发布查询时间与保存)维护。

初次多核场景未出现跨cut输入接收，required覆盖断言失败；首轮单核接收3份空续传、全部处理/ACK，但一般事件与引起构建的变化落在同cut，一般事件覆盖仍失败，原记录保留。修正真实世界操作为建筑变化后下一cut灭火/重燃；新单E核JVM读取上一轮实际保存的测试世界后1/1通过：6份输入、其中3份一般输入在前批运行时接收，包含dormant恢复和真实UNLOAD事件，6份均正常处理/ACK，pending0、最大保留2、最大payload1082028B、源余额差0、关闭Root0。这个重载场景没有重跑冷地形生成，不作为冷启动容量结果；普通256及其他回归在[本轮报告](../../build/queued-input-results-20261016.md)汇总。

最终普通两128源/256熔炉/100热消费者、2400tick/120cut通过，原年龄/几何/接收/WORK_LIMITED均0、最高完成年龄15tick、fine变量42043、降级0、Root83530448B≈79.661MiB且关闭0。与上一节对齐时钟基线相比，cut均值73.383→73.783ms、P99345.854→363.770ms，最慢首cut667.930→651.128ms；P99均由target40冷构建决定，两轮queued input均0。main热回调P991.340→1.751ms、完整处理tick28.682→34.498ms；worker/main监控CPU3203/4281→3062/4296ms、worker累计分配3565004496→3558874904B、GC26次582ms→43次622ms、维护保存1617→1629。尾值有不利项，不能宣称总提速或把它归因于新增排队任务；JIT/GC/宿主调度尚未逐项分离。

与该共享网格基线匹配46816个位置和实际sampleTick观察，平均绝对差9.30e-12°C、P992.27e-10°C、最大9.76e-10°C，位置/自然温度不匹配0、>1°C观察0。这是有限真实场景观察，不是全场误差证书或提高停止精度。production/native8/8（实际卸载18/34tick，票据返回5cancelled、pending0、热等待0）、取消重算1/1、粗细恢复1/1均通过，相关关闭Root0；切粗1.616572°C及当前粗场恢复的有限范围保持。

这完成输入接收子项；整维度物理完成、受影响拓扑闭包、长工作有限服务机会和自动选档/恢复仍未交付。真实单核接收到一般输入，不等于小基地已经独立跨过该一般输入继续推进，也不等于单核256源隔离通过。

## 局部结构边界与真实跨越验证

2026-10-16后续实现`RegionalContinuationBoundary`及源世界面稳定性证明。独立区域可跨过未安装的远处结构输入继续接收下一cut；受影响区域、出口被替换或通风路径未知/受影响时继续等待原边界。局部捕获、源目标证明、输入token寿命与领域检查点的实现合同见[区域准备](continuous-air-regional-preparation.md#局部结构边界与服务机会)。热参数、源W/J、double、停止条件、普通精细网格及v5没有调整；没有接通自动选档或新增温场。

初期真实单E核场景是一处小房间加一座128源大厅。首轮只在数值准备服务，未覆盖两份后续cut跨结构边界，required失败。JFR target4020完整2446.495ms中topology2274.667ms、solve153.396ms、publish18.430ms；区间热池CPU187.5ms，数值子构建约0.26s。主要等待更早发生，因而将服务点扩到拓扑准备/partition；不能将单核竞争下墙钟当CPU时间。

随后r2已观察B的7020候选尚未完成，A跨过未安装7040结构输入继续发布7060：A确认6400J，其他pending1638400J，源余额0。真实出口变化也在7140的当前拓扑准备被正确拒绝早期服务，但测试仅观察ahead阶段而漏记，required整体失败；不能把正向子项通过写成整项通过。r3构建在第二个后续cut到达前完成，也保留为覆盖失败。观察器补记实际目标拒绝/局部选择与对应prepare前后A发布tick保持，有限重试只改真实石柱/顶梁，不注入worker延迟或fake future。

r1/r2的`maxAgeTicks`误把自然温度回退的无局部sampleTick当旧样本，原日志保留但不作为局部场年龄。修正后要求`sourceField`及真实sampleTick，另计不可用局部样本；这项针对时序的case不自行认证完整40tick隔离。最终有限回归见下。

r4的三次真实建筑变更仍未覆盖，源码核对补上最后内联任务完成后关闭窗口之前的收尾服务检查。r5的PROFILE诊断再确认早期窗口被全局phase ACK门禁关闭：target16020/16040等steady=true、mixing0、pendingRoutes=false、qualityRecovery=false，但有28个phase ACK。不能把所有失败都归因于构建过快。当前ACK已在prepare之前实际应用，因而改由包含ACK位置的局部依赖集合和源目标证明决定服务资格，phase intent门禁保留。

最终单核r9功能验收1/1：真实加载两处各128源基地，另加一处有实际篝火的小观察房间，共257源；仅对第一基地做真实结构变化。24040输入自己的`TopologyUpdatePlanner.prepare`尚未RETURN、几何未安装时，A已接受并发布24060（`crossingStage=unresolved_topology`），确认6400J，其他pending819200J，源余额差0。它不是“24020原始构建仍在计算”的同一时序；r2记录了那个更早版本的正向子项，但required整体失败，二者范围分开。r9的77份有效局部样本最大29tick，不可用/几何0；它不是任意负载下的40tick保证。实际堵源出口与本区编辑的两个负向case均通过，后两阶段曾含28个phase ACK。PID17848、processors/worker各1、原4084924416B堆及Copy/MarkSweepCompact；674次进程/其中198次server亲和性均0x800000，关闭Root0。

r1–r8的required失败全部保留：r6/r7负向case已通过但正向交错不足，r6还提前离开了尚未提交输入的第三次尝试；r7修正后有限重试仍未覆盖。r8实际257源同时改动两基地，使通风发现跨cut，target21020仍pendingRoutes=true，正确返回HOLD_ALL；观察器不应把这批输入预设为独立。r9保留同负载、只改第一基地，并只把有实际局部证明的输入计为正向证据。r8/r9启用额外区域诊断（含图统计），只用于功能/时序验证，不从它们宣称CPU收益或单核256完整生命周期通过。完整原始身份、失败、源/预算和计量见[本轮报告](../../build/regional-boundary-results-20261016.md)。

最终普通多核256源、256熔炉、100热消费者的2400tick/120cut回归1/1，12000查询的age/geometry/receiver/WORK_LIMITED均0，最高完成年龄20tick，degradedRegions0、fineAirNodes42043、Root83895116B≈80.009MiB，关闭0。这轮与有界输入基线的比较如下；两者均没有提前排队输入，因此不能用这张表宣称跨区域服务的固定提速。

| 指标 | 前一版基线 | 本轮 |
|---|---:|---:|
| 后台cut均值 / P99 / 最大ms |73.783 /363.770 /651.128|84.269 /354.655 /915.662|
| 主线程热回调P99 ms |1.751|1.579|
| 完整主线程处理tick P99 ms |34.498|50.170|
| Root计费峰值B |83530448|83895116|
| 监控热池 / 主线程CPU ms |3062 /4296|1531 /828|
| 热池累计分配B |3558874904|3637841416|
| GC次数 / 总ms |43 /622|31 /362|
| 维护保存次数 |1629|1556|

温场对齐46489个位置/实际sampleTick，绝对差mean0.001709/P990.022572/max0.112559°C，位置及自然温度不匹配0，>1°C观察0。独立捕获、JIT、GC和维护保存历史不同，CPU下降不等于全JVM提速，温差也不是全场误差界限。当前最慢cut是target20，915.662ms中topology819.436ms、solve25.365ms、后续70.860ms；首次保留的156 spans/2799 cells/11752 Air变量和基线相同。JFR核对120cut的热池CPU为1531.25ms，对照3062.5ms；不能把更长墙钟解释为同等CPU增加。

完整tick的JFR范围内，≥50ms观察从8变24，后者有7次GC覆盖一半以上；最慢首tick约2170.660ms只有27.878ms GC，不能将全部尾部归因于GC或热计算。这组JFR范围包括重复的elapsed1事件，与脚本2400份统计不完全同口径；其余冷启动、JIT/宿主调度和其他工作尚未完整归因，保留性能缺口，不改40tick门槛或挑选复跑结果。证据见`build/regional-boundary-tail-evidence-20261016.json`。

同版production/native8/8：两个真实卸载18/36tick、prepared8/9，票据返回deferred5/cancelled5/pending0、热等待0。取消重算1/1，实际6400J/792材料、40144B私有token释放、一次发布、余额0；粗细恢复1/1，切粗max1.616572°C、当前粗场同时间恢复max8.88e-15°C、checkpoint max1.78e-15°C。所有相关关闭Root0，没有运行JUnit。

这个子项保留三项主要隔离缺口：REGISTER/UNLOAD/恢复及phase intent仍在正常顺序边界；未知/受影响通风或源绑定可保留整体门禁；领域检查点仍不是有限CPU配额或完整独立main ACK。自动整区域选档、公平份额、恢复退避及粗档跨JVM生命周期仍未交付；本轮没有认证单核256完整生命周期或百人真实网络连接。

## 已证伪的策略

固定节点方案在单E核小A+128源B实测仍有35个陈旧tick；正常多核256源虽然通过40tick，却有原本健康的33475节点区域被提前降级。节点数量只描述规模，不能替代实际工作耗时。

1新源/tick的准备候选把单核陈旧tick从35增加到85，引入更多重建并延后注册。差异保存在[撤回patch](../../build/local-overload-new-target-candidate-withdrawn-20261014.patch)，源码默认没有这个机制。

## 仅显式开发试验存在的deadline路径

`FH_CONTINUOUS_AIR_DEADLINE_TRIAL=true`且配置选择SHARED_GRID时，才激活当前候选。普通运行不读取其预算决策。两个配置值默认CPU20ms、墙钟150ms，是失败候选的试验参数，不是经过生产认证的配额。

候选在精细构建的Air组件、支持搜索、网格、体积和部分图/contacts循环检查实际耗时。超限丢弃未提交状态，直接从已捕获成员构建1个区域空气均温自由度；无需完成细网格。保留空气总热容和已注册源W/J、已有材料H，暂时省略空间梯度、材料空气换热和材料内部传热。环境边界按Page合并，材料仍可接收直接源输入。精度恢复最早600tick后尝试。它改变过载区域温度分布和升降温，可能远超1°C误差。

真实单E核r4未通过：小基地15.625ms CPU却有441.211ms墙钟等待，被误降级，断言`The small base must retain the fine grid`失败。墙钟门槛包含调度竞争，不能定位谁消耗CPU。该候选已从默认路径禁用，保留用于后续架构对照；不能作为可交付的服务器保护。

协作检查也无法中断一次库调用、JVM暂停或GC，检查间有越界。现在预算尚未覆盖前置维度拓扑准备、所有求解工作与所有材料容量问题。单个均温候选也不等于硬任务截止。

## 需要的隔离边界

`ThermalDimensionEngine.process`仍按维度接受源事件、准备/提交拓扑、发布与ACK。`steadyContinuation()`拒绝注册/卸载等提前解释；`priorBoundaryReady()`现按下面的局部影响及源目标证明处理earlier结构输入，受影响或未知源绑定仍保留门禁。一个区域的源拆放或没有服务机会的长工作仍可能延迟同维度另一处更新。

不能简单放开门禁：前置拓扑准备仍使用可重用planner，已冻结的供能必须一次确认，区域物理提交和主线程ACK尚未拆开。下述源输入冻结及数值几何持有解决部分所有权问题，没有解除这些依赖。

## 已接入的源输入冻结

`WorkerPhysicalSourceBindings.freeze()`在已获准入的数值准备中生成一次`FrozenSources`，同次各区域/体积helper共享。它只复制当时正向供能端口的原始世界坐标、面向、section索引和待处理mixing Brick集合；不持有可变`SourceDescriptor`、ledger绑定或Minecraft对象。端口遍历顺序保持，候选几何仍按原规则解析实际出口，mixing仍是原半径内的并集。`AirShapeStore`、区域划分和空气体积构建都改读此快照，普通源W/J、热参数、网格档位与换热算法未调整。

mixing变更由Brick到最新revision的表记录，`mixingCommitted(throughRevision)`只确认本轮开始时的水位；同一Brick后来的变化具有更高revision，旧提交不会删除它。当前仍按整维度拓扑确认，尚不是逐区域确认或取消协议。对外的pending集合只读。

复制前在既有准备reservation中加计`512 + 256*p + 64*b`字节，`p`是当时正向供能Air端口数，`b`是pending mixing Brick数。这是快照及临时section计数器的保守数组/容器上界，随准备成功或拒绝释放。section索引按实际不同section数定容，空发热输入直接跳过mixing搜索。revision表清空后仍保留底层容量，`recordMixingBacking`按历史Brick容量高水位计入既有Air backing，并在诊断中列出`sourceMixingRegistryBytes`；不能只按当前pending数计成零。这里没有新增长期温场副本，也不代表所有旧的准备临时量已经计全；尤其整个旧源注册表/事件接收的分配前准入仍属于P0/P2待闭合项。PROFILE记录`THERMAL_SOURCE_INPUT`的实际backing、上界及冻结耗时。

显式GameTest观察器只读取真实篝火操作和实际构建快照，不注入热状态或延迟。真实灭火、重燃、挖除/原位重放两轮各1/1通过，每轮11次冻结、25次保留输入一致性检查，其中4次跨发热状态变化；新生命周期重新发布当前精细空气，最终轮关闭Root0。实录`futurePendingObserved=0`，现有保守门禁没有实际产生新mixing与旧候选确认交错，因此不能把本轮测试称为任意未来事件交错已通过。后续数值几何输入见下；完整planner交错、独立区域提交和自动降级仍未完成；长期残差已按最新要求取消。

源冻结后的两轮普通多核256源场景各1/1通过，原年龄/几何/接收与WORK_LIMITED均0。最终包含revision表容量计费的2400tick实录：Root峰值63.867MiB、后台cut平均72.534ms/P99370.615ms，关闭Root0；主线程加热池CPU7156ms，对照6890ms，不能宣称全服CPU下降。与既有精细控制的46944个同点/同sampleTick观察，首轮max0.004834°C，最终轮max0.206475°C；两轮捕获/重建历史不同（最终轮76次，对照77次；target40的细节点18165，对照18373），不是同几何的数学误差证书。最终轮没有开启粗档。

实际单E核首轮兼容7/8，保留了票据返回case的失败记录：其8个卸载请求在同tick4182准备并消耗，下一tick测试没有执行返回操作。测试动作改到真实请求建立边界，通过原版票据/FULL区块加载请求返回；不改生产checkpoint代码、不延迟future。最终针对3项native生命周期复测3/3，真实取消2个deferred请求、热等待1.657ms、原chunk未卸载，各场景及最终Root0。它验证这三项生命周期，不等于单核256容量/区域保护通过。完整记录见[本轮报告](../../build/source-input-freeze-results-20261015.md)。

## 已接入的数值几何输入与节点地址持有

`TopologyUpdatePlanner.freezeAirGeometry`在数值准备前创建一次`FrozenAirGeometry`。它复制候选Page的身份/几何版本/驻留掩码和环境元数据、已知halo签名目录、精确Air成员及当前活动原始源端口所需的路由出口，共享不可变`PageSignatures`。全Air Brick直接使用原成员规则；混合Brick只复制64字节的block→Air节点查找。数值helper读取此输入，不再查询后续会reset的`TopologyView`、`PageDraft`或可变路由编译器。当前仍复制整个已知Page/halo目录，尚非按受影响闭包局部捕获；预解析路由只覆盖这次冻结的活动源端口，不承诺解析任意未来端口。

`candidateAirGeometry()`仍用于协调者串行的源目标准备；拓扑planner没有变成可并行实例。环境数值沿用此前数值消费者直接读取Page元数据的规则；此次没有改变温度参数、热源W/J、网格档位、double、材料热律或v5存档。元数据Page壳不提供可变Brick目录，节点查询由几何view处理。

捕获完成后，`RegionalAirRuntime`只为实际变化、需要重建的区域调用`retainCells`，按完整Brick span建立`ThermalCellArena.GeometryReadLease`；稳定区域不重复持有。单次Brick选择最多检查64个地址，避免位集搜索跨越远处基地的地址空洞。正常安装或废弃释放owner；额外读者由协调者在helper结束后释放，使用幂等close。退役/废弃的span若尚有读者，进入`GEOMETRY_RETAINED`，立即退出live/staged集合及源/phase目标，保留结构元数据并延后地址复用；最后读者释放后才清理和加入空闲范围。没有新增线程池或复制温场/矩阵。

捕获前通过同一预算的独立reservation准入，寿命可长于准备scratch。初始上界为`4096 + 2048*P + S + 384*B + 64*H + 192*E + 2*ceil(C/8)`字节：`P`是候选Page数上界，`S`是共享签名载荷保留上界，`B`是非空驻留Brick数，`H`是已知halo项数，`E`是活动Air端口数，`C`是arena地址容量。持有前再加`320*N`，`N`为被持有完整span的单元数；320B是地址保留引起的arena primitive/phase列及小型mixed支持的保守费用，不是新复制320B温度数据。额外ReadLease另计96B。共享签名及仍活动的地址可能被保守重复覆盖，不能把此上界当精确JVM堆峰值；全系统所有临时项准入仍未闭合。

真实房间、点火和石柱拆放的首轮观察1/1：旧签名/版本/源出口及地址的generation/位置/Air mask跨真实提交保持；旧span确实退役，读者释放后地址归还、输入reservation为0，Root下降；新场仍`geometry_current`。这验证一次实际借用输入跨几何替换，不是任意planner并行或取消协议已完成。

普通多核256源/256熔炉/100热消费者、2400tick两轮各1/1，年龄/几何/接收/WORK_LIMITED违规0，最终42043个精细空气变量，停服Root0。原源冻结基线与本轮实录如下，均未启用粗档或deadline：

| 指标 | 源冻结基线 | 几何输入首轮 | 有界Brick检查及分段计时后 |
|---|---:|---:|---:|
| Root计费峰值B |66969298|83653538|83835434|
| 完整后台cut平均ms |72.534|83.277|87.044|
| cut P95/P99 ms |135.076/370.615|170.035/337.905|194.178/337.605|
| 主线程完整处理tick P99 ms |23.656|37.852|21.149|
| 主线程热回调P99 ms |2.582|1.024|1.352|
| 数值重建数 |76|78|78|
| 维护保存次数 |0|1593|0|

最终78次重建的捕获/持有/owner释放平均分别0.996/0.298/0.143ms，合计平均1.436ms、P952.773ms、最大10.831ms；这是新阶段墙钟，不含所有后续数值读取成本或额外读者未来释放。输入reservation最大16512312B，元数据/持有表费用上界最大2173112B，其余主要是保守地址保留费用。完整cut均值没有改善，不能把有界选择修正宣称为已测得总提速；首轮维护保存负载与另外两场不同，本机也存在其他应用负载，独立捕获/JIT/调度历史不可视为配对实验。热池CPU/分配、GC和完整限制见[结果报告](../../build/geometry-input-results-20261015.md)。

与源冻结基线匹配elapsed/坐标/sampleTick的有效观察，首轮45934点max0.112557°C，最终47114点平均0.001268/P990.009284/max0.206475°C，无>1°C观测；不同捕获/材料/供能历史仍在，不能作为全场误差上限或逐位等价证明。

最终源码的普通生产/native生命周期8/8，原版票据返回实际观察5个请求、4个deferred、5个cancelled、pending0、热等待0，关闭Root0。实际单E核几何场景1/1，PID20108报告processors1/worker1、原4084924416B堆和Copy/MarkSweepCompact；834次进程采样全部mask8388608，其中server258次。旧输入跨实际石柱替换一致、地址退役/最后释放通过，关闭Root0。它是小房间几何所有权的单核验证，不是单核256容量或自动隔离通过；测试配置/属性字节已恢复。

这套地址持有不是精细温度细节备份，也不自动保留空间残差。后续assignment取消子项见下；P2独立物理提交和P4自动整区域选档仍未完成；恢复改为当前粗场投影，不增加残差查询和保存字段。

## 未发布数值尝试的取消与源重新认领

本节保留2026-10-15试验的实现与结果。2026-10-22清理确认`ContinuousAirSolver.prepareStepAttempt/StepAttempt`已无调用入口并移除；当前取消由`RegionalWorkQueue`、`RegionJob`及`RegionalAirRuntime.discardJob/discardJobSlice`完成，恢复已接受状态并归还`SpatialHeatStore.Claim`。下述额外回滚副本和reservation不是当前路径。

`RegionalSourceCut.Assignment`当前使用CLAIMED/CONFIRMED/CANCELLED三个局部状态。协调者在helper结束、尚未提交时调用`cancel`，将原始记录交还为可认领状态，不修改ledger的delivered/unaccepted，也不重新录制W/J。随后按当前布局和真实世界材料身份重新`claim`。已取消对象的迟到`confirm`不改变记录，即使新assignment已经认领同一笔输入也一样；已确认的重复ACK和取消均无效。2026-10-17区域推进所有权改造后，普通helper不再发布，快照只在协调者提交/确认后可见；原PUBLISHED与`markPublished`已删除。receipt表及确认由协调者单写。

只改receipt不足以撤销热状态。`ContinuousAirSolver.prepareStepAttempt`为已经冻结输入的regional子solver、local材料、pairwise路径提供显式私有尝试。准入后复制C/H/branch，并记录material waiting和原快照；保留原接受数组不变。helper只修改私有状态，`advance`不发布或更新区域完成时间；协调者接受时先生成私有快照，再提交材料/派生Air统计、确认源能量、发布完整结果。取消则恢复原数组、waiting和原快照，清理保留引用并释放token。`commit`阻止绕过接受边界写入私有尝试。

额外reservation在复制前通过原预算准入，上界`256 + 8*C + 8*M + B + W`字节：`C`为完整stateC长度，`M`为materialH长度，`B`为branch字节数，`W`为waiting布尔数；每个数值solver最多一份尝试。不支持的后端或准入不足返回无尝试，普通cut不调用此入口、不复制备份、不新增池或任务队列。接受/取消后同时清除旧数组引用；engine关闭在唯一writer排空helper后清理剩余私有尝试，再释放拓扑、源、Page和查询。

真实Forge房间中，显式GameTest钩子在实际source cut完成、协调者接受前取消一次，再从同一真实cut重新认领并重算。没有注入热量、人工延迟、fake future或中断工作线程。两轮实际`160→180 tick`、6400J、792个材料状态的尝试均通过：原arena未提前改变；C/H/branch重算逐值一致；旧owner迟到确认、重复取消、替代owner重复确认无效；只发布一次，私有reservation40144B归零、source账目差0、停服Root0。第一次启动因测试Mixin包范围过宽失败，修正后才运行热系统；原失败保留。完整回归及计时见[报告](../../build/source-step-results-20261015.md)。

取消目前由显式测试动作触发，用于验证生产接口。它不是已启用的超时策略，也不会抢占尚在执行的helper；本轮验证同一已安装布局，没有证明跨几何/档位替换、强制中途取消或大基地隔离。私有尝试只保留一次任务之前的状态，接受后释放旧数组；它不是为未来精细恢复长期保存的历史场。

已有拓扑prepare/install/abort事务继续使用，材料也已有协调者commit；本轮不是修复已经观察到的半截温场/重复供热bug。新增的是“整次数值推进可放弃并交还能量”操作，不能给已有私有拓扑候选再无条件复制一次。

最终生产/native生命周期8/8、实际单E核取消场景1/1，所有关闭Root0；单核860次进程亲和性均0x800000，其中server267次，原堆/GC。普通256原测试时钟首轮通过、第二轮失败（age112/receiver35/geometry0/WORK_LIMITED0）。JFR证实日志原生写入阻塞后，GameTest时钟将60tick压到88.770ms，目标2280本身仅算125.682ms却观察为54tick旧；没有把失败改称通过。

只修正GameTest的20Hz deadline以丢弃积累的追赶债务，不等待热worker、不改生产时钟或40tick门槛。修正后完整2400tick/122170ms、barrier0通过，年龄/几何/接收/WORK_LIMITED均0，最终42043fine变量、Root峰值83835434B，关闭Root0。实测最短40tick跨度由53.003变1902.659ms，恢复约20Hz输入间隔。日志I/O本身未修复，不能据此改写以前单核容量失败。

最终后台cut平均71.717/P99369.424ms；主线程完整处理tick P9932.934ms、高于前轮几何基线21.149ms，维护保存702对0，CPU/累计分配也有不利项，不宣称总提速。47114个同点/实际采样tick匹配最大0.003219°C；仍是独立捕获历史的有限观察。私有备份复制耗时尚未单独测量。正常与失败、时钟修正、保存负载和完整代价均在[报告](../../build/source-step-results-20261015.md)。

必要的隔离边界仍是区域独立发布/确认和有界工作，具体实现尚未完成。[当前执行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)选择开工前整区域选档，辅以 Page/Brick 等必要领域游标和安全取消点；不优先建设任意位置暂停/恢复的通用框架。等待的小任务需要执行机会，不能仅因排队 wall 较长被误降级。

用户最新澄清允许过载区域暂时超过约1°C，重点是资源恢复后重新形成精细分布和平稳接回；不要求长期保存和演化旧精细残差。恢复以当前粗系数和当前时间作为精细投影输入，材料沿用当前H/branch，不回滚旧时间或重放源供能。已有v5同时支持粗角点和单格节点，暂不增加保存格式。[当前计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)已据此撤回FineFieldAnchor/复合场路线；正常区域原精度与40tick门槛保留。
