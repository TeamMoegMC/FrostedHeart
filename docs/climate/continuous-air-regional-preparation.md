# 连续空气：区域输入、任务与结构交接

- Status: `Transitional; production and GameTest compilation passed; consolidated real Forge qualification in progress`
- Last verified: `2026-10-21; source/deletion review and compileJava/compileGameTestJava passed; runtime matrix incomplete`
- Scope: `SHARED_GRID输入接管、区域数值/构建任务、未知目标、结构交接、查询与保存消费者`
- Code anchors: `ThermalDimensionEngine.receiveRegionalInput`, `SpatialSourceIngress`, `SourceInputWindow`, `SpatialHeatStore`, `TopologyIngress`, `RegionPending`, `RegionalQualityControl`, `RegionJob`, `RegionalWorkQueue`, `TopologyInbox`

2026-10-21控制层替换以[输入、任务与保存所有权](thermal-runtime-control.md)为当前阅读入口。源窗口已从Page运输退休条件中移出，已接管J统一归Store，粗细计算共享Claim/取消/提交协议，质量意图与在途候选由独立控制者管理。当前已完成源码/删除清单核对及统一编译，真实运行矩阵尚未完成。后文按日期保留前批行为与失败证据，不能将旧入口描述或历史通过结果转用于本批。

## 当前状态

最新组合结果为`build/thermal-checkpoint-owner-closure-20261020-r4.log`：9项required中5项失败，不能再把这一轮写为等待结果。粉雪场在累计72次WORK_LIMITED后，最老样本持续停在6560，输入至少推进到9840；同期粉雪H固定为39098.875238268105J。输入9140/9340时无活动snapshot，`regionalReady=false`；tick6560的结构项已经`regionWait=ready`，但`sourceReady=false`，部分源前缀的`through/recordLimit/ownerLimit`均为6560。后续较晚结构等待region145/sample6560。这个现场首先指向输入接管与结构边界之间的推进阻塞，不能用求解器耗时或仍在等待保存解释；72是累计拒绝数，不表示每一轮仍在申请容量。

同一JVM随后出现两项关闭等待失败、独立房间容量场等待失败、跨背景场`maximumCells=45002`超限，最终停服`rootBytes=524288`。这些是5个失败场景，尚未证明是5个独立根因。`IntakeCursor.start/retireActive/sourcesReadyThrough`静态上仍存在Page级批次的交叉等待可能：active中某源尚不能越过结构边界，pending中另一源的到期记录被结构等待，而pending必须等整个active退休才能开始。现有日志没有列出`boundarySourcesReady`具体否决的源与批次，故这仅为待验证机制，不能宣称已定位或修复r4的唯一根因，也不能与r15合并结案。后续`thermal-checkpoint-current-save-20261020-r1`独立当前格式保存1/1通过、root0，只证明写入场景；没有给组合运行或独立JVM读盘授予资格。完整核对见[本次诊断记录](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

2026-10-20后续完整回路复核实际重现停滞。`SpatialHeatInbox`允许恢复/orphan记录迟到，却把精确队列当作始终按时间到达：`pendingThrough`能看到后面的到期记录，`route`只看队首未来区间而退出，区域因此阻止结构安装，结构边界又不让未来区间推进。r2实际记录为sample1920、owner/Page寿命均匹配、无snapshot、无容量拒绝，队首`[1920,1928]`而边界1920仍有到期记录。当前队列在最多64条精确记录内按(from,to)有序，同起点即时输入先于连续区间；部分路由后的剩余段重新有序插回，允许同边界输入继续接管。正常有序输入仍尾插，不改J、rate、时间包络或容量上限。此修正的最终真实验收尚待完成，不能把r1/r2记作通过，也不能冒认r15拒绝后的精确现场已被证明。

保存已改为`RegionalCheckpoint.Section/Part`固定索引直接投影已封存field，main不再重读live Page/query目录。保存准备区分等待helper、候选撤销清理、容量和已封存；材料控制的可执行判定统一排除SEALED区域，保留待办并等finish后继续，容量等待有实际通知。checkpoint退出清空数值引用，回调/lease释放在其monitor外执行，避免finish和engine close的锁顺序成环。具体当前保存规则由[保存文档](continuous-air-persistence.md)维护，整批证据由[工程计划第29节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)维护。

此前r23同版3项通过：1024真实源压力1600观察tick、107次拒绝，正常控制81次推进、maxAge21、failure0且保持FINE；被挖除重放的源实际generation363→1027，并继续记录到1980。fine 6400J和minimum 12800J均两取消一次发布、回放能量/温差0。跨背景16°C场的最大降档跳变0.685438°C、fine恢复0.008872°C、checkpoint采样差0.005447°C，停服root0。r24增加三次压力期间实际原版保存并通过。它们不授予后续改版的完整V0、真实单核或多人默认资格；r22那一次`air_slot_missing`尚未得到同样现场的唯一根因解释。

**r15恢复验收失败：** 真实数量和内存拒绝各返回一次WORK_LIMITED后，温场仍停在sample5300/5320，输入已到8400，热线程空闲。下面描述的是已接入的实现，不能视为超限恢复资格；后续intake/结构边界等待仍在定位。r13仅证明独立跨背景场景通过。

r16实际容量场还暴露`Rebuild.resumeRecovery`错误地限制纯材料候选只能有一个旧空气parent。现按候选职责处理：纯材料仍读全部parent材料snapshot；真实空气分区改变时放弃可选恢复并保留已提交最低场。r19已走过12/16个旧空气parent的材料恢复，没有该异常，但独立控制未始终保持精细档，容量资格仍失败。最新入口退休修正只在全部源窗口已交接且控制已归TopologyInbox后放行后续包；结构receipt和安装hold由原结构所有者继续持有，未完成源窗口仍受边界限制。该修正及真实篝火身份替换正在r20独立容量验收。

2026-10-19批准的替换批次已通过production/GameTest统一编译，完整Forge矩阵尚未完成。实际源码已有稳定世界输入所有权、分开的`RegionJob/RegionBuild`、连续pool CPU信用、160节点上界的最低网络、fine提交的匹配最低摘要，以及一致已提交温场加未消费J的v6保存调用端。含空气区域的最低工作空间预付256KiB；纯材料区域按至多`min(96,材料数)`节点的成对因子上界和固定数组预付64–128KiB，旧槽只交给可容纳的新区域。整体仍受128MiB准入；本次定容修正尚待同版真实矩阵。

固定物理保存hold已从正式循环删除。`captureRegionalCheckpoint`只等待实际helper退出、封存/导出回执，不要求数值推进到当前Minecraft时间。冷输入独立接管并保留临时空间seed直到未来分配也消费完毕；实际卸载、返回取消和热Page退驻分别走各自的所有权出口。GameTest直接接入可选`ThermalDiagnostics.Observer`；常规发行没有listener，不再对自己的区域/engine代码注入Mixin或反射私有状态。

原版卸载使用单在途`ChunkCapture`异步准备，再由原版卸载executor执行实际写入；普通保存与停服复用其封存和finish回执。候选路径失效由`PreparedTopologyChange.retiresRoute`传入分区和接触组装，失效范围包含内部Brick及实际存边的`edgeOwners`。正式旧路径直到提交才退休。真实native三项回归已通过，不能据此推定双128或单核过载通过。

扩展V0的实际reader退出已证明最后一个读者释放后root归零，但组合场景随后有新区域停止数值提交。独立同位置重启和保留旧房间后的新房间重启均通过，不能把组合失败笼统归因于所有同JVM重启。当前先检查真实来源/路由/结构等待，V1–V5未开始授予资格；完整证据与限制见[本次记录](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

2026-10-20的真实组合诊断定位并修正`nextQualityRequest`丢失最低请求：取消尚未退出时，请求不能又被恢复精度分支移除。现为最多4项的单队列轮转，成功发布前保留最低请求。r5/r6真实最低输入12800J已通过两取消/一发布/完整交付。后续资格仍被内存拒绝阻断；不能将此局部结果称作过载保护通过。

冻结halo表现按fastutil实际long[]/int[]容量预付且提前定容，更新已有位置不重复算新位置，也不在复制时填充另一份planner逐位置读缓存。r6的115份初始冻结捕获均在预付范围内，最大reserved约1.87MB/metered约1.12MB；后续约22.5MB拒绝来自旧arena几何单元保留。这个初始载荷计量不能当作整个候选或JVM堆的大小。`frostedheart.FrozenAirGeometry`按capture/retain分别记录预算与计量，当前继续验证纯材料工作空间定容能否消除该次拒绝。

2026-10-20修正最低场与驻留的背景联动：`MinimumThermalModel.Preparation`从冻结Page自然基线和实际volume质量生成`MinimumThermalState.Background`，不捕获旧精细温场残差。空气总能量和材料H保留，最低查询使用原layout的背景插值加最低额外温变；材料继续以anchor加池温变化/显式相变读取。成对换热使用相对各节点背景的温差，每对仍等量增减J；环境浴温保留节点背景，只叠加实际环境变化的区域平均。这改变最低档的换热近似，精细档的数值求解器和热参数未调整。

`RegionalQuerySnapshot.activityResidualC/componentPerturbationC`让驻留和外扩也读取额外温变，fine使用其匹配背景，不再直接把背景空间差异都算作热源影响。保存、红外、fine恢复继续读完整温度。背景新增数组进入准备/投影预付及唯一backing计量，普通最低cut不展开细向量。r13真实跨基线场景通过：自然基线范围18°C、最多1,979单元；降级跳变1.156177°C、fine恢复跳变1.78e-15°C，最低checkpoint采样往返差0.028444°C，关闭root0。降级期间可暂超1°C是用户允许的取舍，不将这份单场景结果当作总体温差上界。

`GeometryWork`现将拓扑数量/操作上限与内存准入区分：数量拒绝先用`recoveryInput`拆掉新增空间；内存拒绝清理后至多重试一次，再拆增长。缩小批次仍拒绝时，放弃该候选，已有所有者请求最低服务，几何描述经单个`TopologyRetry`通知槽交回main既有退避入口；实际J和控制不重放。通知持有独立预算直到main ACK或关闭，ACK能直接唤醒等待通知槽的工作；已发布温场只继续清理，不取消。真实大量热源容量拒绝及原完整V0仍待最终同版验证。r9/r10失败原因和现场保留在[增长调查记录](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

本文后续较早段落的固定截点/C4说明属于替换基底；它们不能作为新最低模型或v6已通过的证据。详细批次和待运行V0–V5见[工程计划27–28节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。旧normal与单E核结果保留历史日期，不能转用于当前代码。

## 2026-10-19整体资源所有权复核

当前正式SHARED_GRID入口是`receiveRegionalInput → RegionPending/SpatialHeatInbox → RegionJob`，物理时间来自各区Accepted。`GeometryWork/Rebuild`各自持有冻结输入、路由delta、halo覆盖与受影响父区域；维度保留协调写入、共享表和目录。完整R1和自动降级仍未交付，[原计划第12.1–12.2节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)记录整体审查及后续替换范围。

构建子步的`preparationScratchBytes`现读取本任务的载荷累计值与容器实际容量。`MaterialEdgeCompiler`在创建边/执行数组时累计一次；PageDraft只在变化时刷新，fragment只计自己拥有的数组，route组件只在实际推进后刷新。`AirBackingMeter`的完整共享去重留在交接/观察，不能在每256条边后重扫先前所有边。路线/trace由其真实owner负责，借用者不在每个fragment重复遍历。

冻结halo副本现只带所捕获Section的签名。数值支持在`pageAt()==null`时原本就无法继续，因此缺少捕获Page的地方不会借用另一区halo建立路径。仍需一次过滤全局平表，arena寻址BitSet和partition的int[]也仍按真实地址容量分配；不能称全部构建已与远处规模无关。SHARED_GRID的局部元数据预留不再套用旧解析式的全服端口数平方，实际图数组由每scope预付。

长期airReservation只接管已安装存储和本次将安装的数值/拓扑载荷；其他候选、当前delta/rollback/草稿仍由各GeometryWork的scratch负责。完整核对不再把所有独立任务的私有准备数据又计入长期owner。当前计费数字依然不是整个JVM热系统堆占用，捕获端和全部临时分配的完整合同尚未关闭。

已定位的主要剩余CPU放大是`refreshRegionalFrontier`：区域提交后仍重复发布所有区域、扫描所有Page。它尚未改为局部维护；按Page输出七份mask、受影响目标合并和同Page多区域hot mask的替换设计见[完整工程方案第13节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。没有用任意跳帧改变刷新频率，也没有宣布自动FINE/C4已接通。

两个中间正常运行均在线年龄/几何/消费者违规0、全FINE和Root0，但分别因2次及1次容量拒绝整体失败。最终安装所有权版`handoff-ownership-normal-20261019-r1`已通过2400tick/12000原频率读取、违规0/拒绝0/全FINE/Root0，计费峰121.7MiB；worker累计CPU6156ms、分配4.35GB，仍高于更早对照。33738个同位置/实际tick匹配样本max温差0.326567°C。最新两个真实依赖负向场景与最终同版源取消均已通过，6400J/两次取消/一次发布/Root0。

同版真实单E核结构交错`handoff-structure-single-20261019-r1`整体失败：新房31tick内current且先于远处较早结构安装，四窗口/余额0；800样本出现一次gameTick281读取tick240的年龄41不可用，不能把有效max40称通过。关闭Root0，root峰79.6MiB。缺口附近记录一次geometry工作873ms墙钟和多次126–220ms全局维护；这些包含单核抢占，不是精确热CPU。必要长阶段和全局维护的执行边界仍属R1缺口。完整计量/失败和范围见[本次报告](../../build/regional-ownership-results-20261019.md)；这些局部证据不构成默认启用或持续过载资格。

## 输入与温度的所有权

`SpatialHeatInput`保存世界位置、空气面或材料目标、目标生命周期、区间和W/J，不保存数值布局。非零区间的amount为W，区间长度除20换为秒；等端点记录为J脉冲。冻结执行时，`CutLoadBuffer.compile`才把这些身份映射到当前局部系数地址。

`RegionPending`保存一个冻结前缀、两个近期精确窗口，以及更早输入的有界位置汇总。正/负J分别累积；汇总保留时间包络和环境端点。`freeze(through)`可在一条未安装结构边界处分开前后输入，后半段仍由待办持有，避免结构等待时间T、数值任务却必须越过T才能完成的循环依赖。没有成功提交的前缀不会因取消而退休。

`SpatialHeatInbox`使用同样的世界目标表示保存暂时无法确定区域的份额。当前实现正补齐封闭端口、材料替换和Page退休的终结规则。真正的目标生命周期结束与暂时缺少容量不是同一件事；容量不足不能当作损失删除输入。普通初次几何不可用继续沿用已有degradedLoss语义，避免把冷启动期间本来未接受的热量积攒后突然注入。

空气目标开始携带现有Page生命周期；材料在结构交接时只把存续物体的待办映射到新地址。同坐标REPLACE不是同一物体，旧材料输入不能附着到新材料。这些新生命周期分支尚待完整生产矩阵验证。

## 一个区域任务如何执行

`RegionJob`持有一次数值前缀或一次候选构建。`RegionalWorkQueue`使用同一个就绪队列，让数值工作与候选组装轮转；同一区域只有一个执行者。后台helper完成片段后交回协调者，热发布和消费前缀由协调者一起接纳。

数值推进的`CutCursor/PairwiseCursor`保留事件排序位置、前后扫描位置、半步状态及材料阶段；暂停后从该位置继续，不重新输入同一笔J。取消未发布结果时，从`ThermalRegion.Accepted`恢复C/H/branch/waiting。已接受的结果不能通过取消旧job撤销。

构建的`CandidateBuild`将原计算分成12个阶段，保持原公式和算子。共享网格源支持阶段用`AirShapeStore.GridPreparation`按位置继续；已有路线发现用`AirRouteCompiler.beginPrepare/advancePreparation`继续，不在每片重播种。**阶段之间可让出不等于所有阶段已经满足2ms上限**：粗布局、网格、接触/算子构造与投影中仍有整体循环，需要结合真实延迟继续处理。

不含空气的互不耦合材料岛按局部Section组成工作批，避免全维度材料屏障和每个惰性方块各建一个solver。这只改变工作归组，不增加材料间导热边；真实导热连通分量仍保持在一起。

## 结构与环境

`TopologyInbox`按Page保留冻结描述、近期描述和旧摘要。工作使用接管时封存的源快照，通过原`TopologyUpdatePlanner`、`BrickTopologyCompiler`和空气寻路器准备候选。相变ACK/intent、天空列、源出口和真实路径依赖都必须进入边界。

重建只允许使用在交接时间对齐的父状态。旧区域尚未消费的世界输入须随拆分/合并交给新区域，暂时未知者继续留在位置待办。环境随区域自身的物理时间更新；不能在一个落后区域开工时直接读取后来更新的全维度天气。

正式路径的frontier直接读取区域Accepted中的空气值。全维度`combined`只供兼容接口按需使用；多个父区域合并时，只临时组合参与投影的父场。

当前续工已将正式入口改为多个`GeometryWork`轮转，每个任务持有私有`TopologyUpdatePlanner`和`RegionalAirRuntime.Rebuild`。路由准备使用已提交目录上的增删delta；静态Component不持有整个builder，候选取消不会退休旧路线。halo更新先进入TopologyView，提交才写WorkerPageStore。Page读取身份/发布引用、实际使用的冻结halo Section版本和父区域身份需在安装前保持有效；真正有关联的任务仍需串行。

候选捕获只包含受影响闭包的Page，partition输入来自这些父区域与本次片段改动。安装时合并当前区域表和本次delta，避免覆盖另一个已完成任务；单元所有权和空气Brick所有权只替换涉及的父/子区域。全局查询目录、frontier支持请求和部分长构建阶段仍有整表工作；这些费用不能被描述为已经严格限制在2ms。多结构交错及新依赖校验仍在验证中，不能据此宣布完整R1交付。

## 预算与等待

root仍为128MiB。输入准入先预留记录与分配开销，再推进源账目；结构交接预付新旧待办同时存在的开销。已有数组换owner可转移reservation；新增复制需要覆盖并存峰值。数值发布拒绝时保留结果并等待实际容量变化，不因一次读者仍持有旧快照就重启引擎。

结构选择只在输入、物理提交或结构版本实际变化后重试；候选边界带输入/版本身份并有预算owner。纯等待不能持续扫描相同闭包。预算释放用可撤销通知唤醒。捕获端、基础arena/source数组与全部临时分配的统一计费仍在审查，不能将当前计费数字说成全部Java堆占用。

当前自动FINE/C4策略尚未完整接到新任务生命周期，属于R2。正常精度、过载可暂时超过约1°C、从当前粗场恢复等授权不变；当前代码没有完整的百人服过载保护资格。

## 查询与保存

查询继续读取区域已提交场和真实sampleTick。HUD/IR的新鲜度仍为40tick，不能通过改时间戳掩盖落后。`publishGeometry`先准备查询快照、目录和读者共存的容量，再在同一次发布中安装拓扑；预算拒绝不再发生在正式拓扑写入之后。查询目录的完全局部准备与所有读者更新范围仍有剩余工作。

main区分接管水位和区块物理水位。维护保存检查目标区块是否有未提交几何/工作，未就绪时保留原版的dirty与重试调度。原版异步卸载持有一个固定输入截点；显式保存/停服的同步等待只封存一次输入，再接收进度，不重复捕获同tick输入。保存可能保留较老但具有真实时间的已完成截点，不能把它当成新的玩家温度样本。完整保存回归进行中，详见[保存文档](continuous-air-persistence.md)。

## 本次真实证据及限制

- `thermal-core-runtime-cold-single-20261018-r1`：真实单E核、双128源冷建，正常房间在同一未安装边界240期间提交260/280/300/320四窗口；当时余额差0。800样本有8次不可用，随后停服未排空，整体失败，未得到Root0。
- 上述真实窗口的离线JFR显示重复边界/出口扫描占据大量采样，累计分配权重约5GiB。这是累计分配估计，不是5GiB驻留占用。边界分段和按进度唤醒依据这次证据实施。
- `thermal-core-runtime-source-20261018-r1`：新增待办元数据未在交接时预留，触发预算转移错误；重启时间衔接又导致重复事件时间错误。之后修复先准入、后修改账目和重启截点。r2仅编译失败，未启动Forge。
- `thermal-core-runtime-source-20261018-r3`：源取消/重算结果已一致，观察器误把空气格纳入“材料不写回arena”的检查而失败；r4修正检查范围后，真实6400J、两次取消、一次提交、降温与关闭Root0通过。
- `thermal-core-runtime-cold-single-20261018-r2`：在tick231的维护保存入口阻塞，等待循环重复封存同tick输入。保留日志、JFR和线程栈后终止该次测试；未完成800样本，不能通过。维护保存入口与固定截点等待随后一起修正。

- `thermal-core-runtime-cold-single-20261018-r3`：小房间800样本max36tick且无不可用，但停服时存在孤立结构hold，整体失败。按实际剩余结构重建hold、先路由再推进之后，r4在真实单E核通过四个连续窗口、800样本max29tick、不可用/几何0及关闭Root0。
- `thermal-core-runtime-dependencies-20261018-r1`：受影响出口与本区几何两个真实负向场景通过。`thermal-core-runtime-production-20261018-r2`：production/native/IR/生命周期8个required通过，Root0。两组对应单结构上下文版本，不代替后续结构上下文改造的资格。
- `thermal-core-runtime-local-source-20261018-r1`：私有结构上下文首版的真实6400J、两次取消、一次交付/发布和Root0通过。其后补充的冻结halo读依赖、helper退出和提交前全局容量复核尚待验证。

以上失败文件均保留在build/。没有JUnit、模拟热源/worker延迟、提高root预算或ActiveProcessorCount。当前仍缺新多结构交错版本的完整验证、全部预算覆盖、正常双128温差/完整窗口性能资格，以及R2自动降级/恢复闭环。当前开发状态不适合据此宣布连续空气默认启用。
