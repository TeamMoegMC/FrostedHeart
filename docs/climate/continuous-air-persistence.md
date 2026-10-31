# 连续空气：区块温度保存与原版卸载调度

- Status: `Transitional; current v6 ownership contract linked below, historical persistence trials retain their dated provenance`
- Last verified: `2026-10-22; current v6 contract and final 15-group/24-required qualification verified; dated trials remain historical`
- Scope: `continuousAir保存模式、区块NBT、普通维护/卸载、强制保存/停服和真实验收边界`
- Code anchors: `SpatialSourceTimeline.Capture`, `SpatialSourceTimeline.Pending`, `SourceInputWindow`, `SpatialHeatStore`, `ThermalDimensionEngine.advanceSnapshot`, `RegionalCheckpoint`, `MinecraftThermalInput.ChunkCapture`, `DormantChunkThermalState`

当前保存所有权见[运行时控制说明](thermal-runtime-control.md)：每维度一次封存一个Chunk，从固定`RegionalCheckpoint.Section/Part`、剩余J及producer尾部写当前v6，不要求物理追到保存tick，不重读实时Page/查询目录。保存拥有自己的有限完成空间，没有`WorkspaceLease`、跨Chunk清空候选或临时拥挤导致的额外近似。状态为`REQUESTED/QUIESCING/CAPTURING/SEALED`，封存后只等finish/cancel。最终生产r17的15组/24项流程资格通过，包含压力保存、未绑定J真实落盘读回、原版卸载取消及独立重启H/branch/tick位级核对；各组退出root0，实际耗时与内存见当前说明。

## 历史实现与试验

以下带日期的缓存、回收阶段、旧格式和通过范围保留原批次归属；当前writer、资源政策及最终同构建资格以运行时说明为准。

2026-10-21控制层替换以[输入、任务与保存所有权](thermal-runtime-control.md)为当前阅读入口。源窗口已从Page运输退休条件中移出，已接管J统一归Store，粗细计算共享Claim/取消/提交协议，质量意图与在途候选由独立控制者管理。当前已完成源码/删除清单核对及统一编译，真实运行矩阵尚未完成。后文按日期保留前批行为与失败证据，不能将旧入口描述或历史通过结果转用于本批。

## 当前替换状态

2026-10-20后续回路复核确认旧`ChunkCapture.write`并未序列化checkpoint自己持有的fields，而是从live Page与`QueryPublication.ReadCursor`再次取样。这是明确的保存合同缺陷，尚不能据此宣称已证实r22的唯一根因。当前writer已改为读取`RegionalCheckpoint.Section/Part`的固定索引：在coordinator封存时按世界Section一次分组，main只从这些已提交field的系数/最低场采样器和材料H/branch/身份生成现有格式。每component/record保留自己的实际sampleTick，材料仍按封存的worker几何revision投影之后的实际世界变更；未覆盖的dormant Brick仍保留。写入不再重新借用实时查询目录，也不在ready之后申请混合时间scratch容量。新增索引和临时数组由原capture credit覆盖，格式仍为v6。

2026-10-20保存准备曾包含`RECLAIMING`及材料容量通知；该阶段和配套借款现已删除。`releaseFrames`仍清空Section、field及lease引用，避免账目释放后checkpoint对象继续持有温场。旧批次实现与验证见[同一工程计划第29节](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

2026-10-20本轮在r22真实压力期间出现过一次维护保存崩溃：`air_slot_missing,slot=3066`。该错误原先也可能来自reader借读预算拒绝，已加入readerRefused与当前owner的准确失败分项；根因尚未取得同样复现。随后r23/r24选定3项连续通过，r24在1024真实源、89次拒绝期间主动完成3次原版非flush保存，正常控制maxAge21且保持精细，停服root0。此结果证明选定压力保存场通过，不能将r22一次失败视为已解释或授予完整保存资格。

2026-10-19的集中批次已经接上v6 writer/reader和main保存、维护、实际卸载及停服入口，production/GameTest统一编译通过，完整真实Forge资格仍待验收。正式SHARED_GRID保存由`captureRegionalCheckpoint`封存一致Accepted和有限待消费J，不再以`awaitThermalBoundary`物理追平为前置。原版卸载在单在途`ChunkCapture`中异步等待实际控制回执，主线程不等待热计算future；准备期间新增产热在消费时按源端口累计基线补齐。普通保存与停服复用同一捕获所有者并等待必要的封存/清理回执。先前v5/native-ready证据保留在后文，不证明这份新代码已合格。

v6空气component各自保存实际sampleTick；材料沿用逐record时间。保存时未消费热量用位置/面/材料BlockState和有限区间/J存储。按用户2026-10-20的明确要求，reader只读取当前格式6，格式5及其他历史温度记录直接忽略，不迁移；世界方块、物品和燃料仍由原版读取。冷输入先按既有休眠半衰期衰减，再在20–160tick内分配已产生的J。普通在线保存只写磁盘副本；实际卸载才导出在线输入并给源端口登记已导出量，旧输入窗口以后到达不会再次发热。原版待卸载holder被取消时不导出。

2026-10-20最低场改为独立背景加额外温变。`captureAir`仍通过`RegionalQuerySnapshot.componentTemperatureC`保存完整温度采样，材料仍保存实际H；不新增NBT版本，也不把背景数组或旧fine残差单独写入存档。r13真实跨基线降级/恢复场景中，最低checkpoint编码/解码后的采样差为0.028444°C；这是单场景空间摘要的结果，完整native/返回/停服资格仍待最终同版矩阵。延后几何通知的`TopologyRetry.storage`是运行时预算所有权，与NBT格式无关。

本批改变了旧固定截点合同，允许待办压缩和资源不足时的空间摘要。正常在线温差、卸载返回/重启、材料身份、总J及保存延迟都必须重新验收，不能继承旧曲线与ms数字。后文旧保存模式开关/格式说明以当前源码和[工程批次](../../diary/2026-10-29_23-38-00_thermal-development-history.md)为准。

## 正常配置入口

main通过`ThermalInputBatch.Submission`的区块键判断运输中的输入是否关联待保存区块；独立入口中的输入、恢复载荷和源端口尾部由一致封存命令汇总。运输接管回执与物理温度进度分开，保存不再要求旧物理完成水位追平。

`config/frostedheart-common.toml`的`["Thermal Runtime"]`提供以下独立设置，均在重启后使用：

```toml
["Thermal Runtime"]
continuousAir = true
continuousAirBackend = "SHARED_GRID"
continuousAirNativeCheckpoint = true
```

源码默认依次为false、FUNCTIONS、false；上面是本批验收所用的可选组合，完整资格尚未完成。`continuousAirNativeCheckpoint`在每个维度实例创建、Forge配置已经加载后读取并固定，避免类初始化时过早读取或同一实例中途混用模式。常规tick/查询不增加配置读取。

开发环境`FH_CONTINUOUS_AIR_CHECKPOINT_TRIAL`独立覆盖保存模式：`native_ready`选择原版队列准备+按需快照，`on_demand`只选按需快照，`latest`及其他非空对照值保留最新完成结果的原路径。未指定或空白值使用普通配置。后端开发覆盖是另一个入口，见`ThermalArchitectureTrial`；配置SHARED_GRID与配置保存模式各自检查实际日志。

## 保存内容与加载

温度状态由LevelChunk拥有，写入其原版NBT的`FrostedHeartThermal`。空气保存空间温度值、对应空间节点、自然温度基准和逐component实际采样时间；材料保存热量H、相变分支、方块身份/热律及逐record实际时间。连续空气节点温度以双精度位值保存；标量Air的1/16°C与640B规则不是整个连续空间payload的上限。

当前writer与reader都只使用外层格式6；不再兼容格式5，也没有新增格式号。v6增加有限signed待消费J、时间包络和位置/面/材料BlockState身份；不存运行时arena地址。加载按实际经过的游戏tick惰性计算休眠冷却；热源和空间结构按当前世界恢复。运行矩阵、预条件器、workspace、旧fine残差和离线逐tick求解记录不写入NBT。历史温度标签被忽略后，从当前世界初始化；当前系统自身在重建、降档/恢复和本格式卸载返回时仍承接实际温场、材料H及未消费J，不能把删除旧存档兼容扩大成清空运行中温场。

## 何时需要等待

普通保存封存已提交场及对应未消费J。全部相关Page成功后一次安装区块替换，不把部分新数据装进实际附件。快照保留各部分真实时刻，可以保存积压状态；在线查询的40tick新鲜度要求不能变成保存前必须追平的物理屏障。区域材料捕获按槽位生命周期和方块状态匹配，同Page其他Brick提交不会使未改动材料失效。

原版卸载在其future/unloadQueue上异步准备目标区块；每维度一个在途`ChunkCapture`，最多128请求。执行前重检holder/future、Page身份/几何、来源修订和捕获有效期；票据返回取消尚未消费的捕获。未发布空间不制造空气值，但已经接管的J仍进入v6 pending。实际写入后finish导出输入，main收到回执才释放producer并启动下一项。512B请求元数据不等于实际快照、源捕获或整个原版Chunk的堆占用。

普通维护保存、显式保存、flush与停服复用`ChunkCapture`封存入口，必要时同步等待实际helper退出和控制回执；不会代main执行求解或排空全部物理历史。准备空间不足时，撤销未发布候选并借用预付空闲封存空间；仍无容量则等相关容量变化。捕获、NBT编码、原版存储和GC分别计量。关闭分片必须显式安排下一片结构清理，真实helper等待使用完成通知，不能依赖下一次游戏tick。

## 历史真实验证（以下未重验结果不授予v6替换资格）

清除保存/后端环境覆盖的普通配置组合通过15项真实Forge回归：早期/初始化/返回取消3、原生产5、真实冻结副本1、独立JVM保存/加载各1、燃料2、读者/相变2。全部realtime20；冻结副本加热/熄灭实测最大差0.419/0.576°C，比较时相差20tick；实际读盘材料H/branch和tick460保留。实际源码未修改热参数、double、推进或格式。

CT2副本实际客户端通过1280tick操作：4件装备4→0→4、真实入水Wet、篝火熄灭/挖掉/重放/点燃、局部施工、实际区块卸载返回、HUD数据和红外渲染、真实健康界面显示升温。返回首次current为6tick，36native请求全部准备、pending/fallback0，保存热等待total10µs/max3µs，卸载0；停服Root0。第二个客户端JVM读取实际磁盘标记与同一副本，400tick通过，首次current18tick，HUD/IR继续有效，停服Root0。

HUD包按现有0.1°C环境/0.01°C核心量化传输；健康升降温趋势由菜单数据槽独立同步，不靠客户端未同步的瞬时净功率。诊断需按实际包精度对照，不能用服务器原始float逐位相等要求UI。

记录保留两次CT2失败：启动脚本误传空环境覆盖，及副本虚空远处无地面导致玩家坠落。修正使用真正删除进程变量和副本远处小平台，没有改变热状态/伤害参数或扩大恢复期限。原CT2与run配置未修改。

关闭新开关的旧保存路径5项回归通过；关闭continuousAir但保留新开关的标量燃料/读盘2项最终明确realtime20通过，确认不介入标量路径。测试辅助时钟最初仅START兼容标量，END仍连续限定，首次标量2功能通过为加速运行；修正START/END一致性后另跑真实时钟，不用首轮作性能证据。

双维度各128源、256真实炉子、100热消费者的4000tick和2400tick场景通过；共享128MiB总预算，Root峰值约47.0/47.1MiB。第二轮两处冷建场首次current均29tick，Nether实际卸载恢复两轮均20tick，源状态独立、每点新鲜度/几何及WORK_LIMITED断言通过，关闭后Root0。它与历史单维度场景不同，不给固定内存/CPU提速比例。测试未经过自然6000tick自动保存，日常自动保存依据此前独立12000tick记录。

多维度正常段排除首400tick与记录的强加载返回窗口：main P99约12.814/13.733ms，最大30.507/78.597ms；保留同步返回281–289ms与第二轮原版卸载编码峰值。CT2操作区间仍有同步加载/返回、原版编码、流体等慢tick，不能因热等待低而声称整体无卡顿。详细时点见[最终报告](../../build/formal-save-final-report-20261014.md)。

当前仍未认证100个真实网络客户端。用户确认没有现成真实连接工具，先完成可执行验收；有限两维度/100热消费者及单客户端不等同任意复杂机器或长期百人负载。现有默认和上线资格分开报告。

证据：[正式接入计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)、[CT2客户端](../../build/ct2-native-exercise-r3-20261013.log)、[独立重读](../../build/ct2-native-reload-r1-20261013.log)、[健康界面实图](../../build/ct2-native-exercise-r3-1160.png)。

## 原版区块耗时与热保存的边界

2026-10-14对三个新CT2副本实际客户端补了test-only直接计时。`ChunkSerializer.read`内部包含Forge `ChunkDataEvent.Load`，热数据读取包含在原版区块解码里；`ChunkDataEvent.Save`位于`ChunkMap.save`内、`ChunkSerializer.write`之后，所以原版编码计时不含热保存回调。`thermal_data_save`又可包含snapshot与热encode，这些inclusive区间不能相加。

两次12区块/一次8区块同步返回完整tick336/727/565ms，约531–539次区块decode累计279/391/332ms，其中热decode13/11/17ms；热chunk-load回调只有0.17–0.31ms。第二次12区块另外直接量得返回`LevelChunk.postProcessGeneration`约75ms：原版可在启用阶段直接推进流体，不能仅用`ServerLevel.fluidTicks`批次代表全部流体成本。普通流体batch三轮max约19/26/24ms，历史142.656ms未直接复现，不宣称已修复。后处理与decode之外仍有未精确归因余项，不能全称磁盘等待。

热等待三轮total12/26/7µs、卸载0，native全部准备、pending/fallback0、关闭Root0，实际玩家返回current5/7/8tick。首次冷捕获仍有snapshot约26ms、完整61.5ms的一次峰值；实际保存无需等待worker不表示捕获与原版存储完全没有主线程成本。最终强制保存完整tick约239–354ms，独立保留。

降低renderDistance/simulationDistance12→8减少该记录的整体解码/编码次数，但880tick操作段P99仍约53–54ms、同步返回没有改善，当前不作为已验证加载优化推广。该窗口包含施工、卸载/返回和GC，不等同此前正常在线段4–14ms。原版`ServerChunkCache.getLoadedChunksCount`返回`ChunkMap.size`，观察值包括生成中的ChunkHolder，不能直接称为完整活动区块或热Page数量。

本项没有改生产保存架构或热模型；原CT2只读复制、配置已恢复。相同gameTick的12/8试验57个实录点环境最大差约0.00020°C、核心记录相同，支持该局部场景体感，但不证明缩小模拟距离对远处机器没有影响。详细峰值、未知余项和原始记录见[本轮有限报告](../../build/ct2-native-latency-final-20261014.md)与[执行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## “几ms”的适用范围与直接热成本测量

几次最终保存的热回调约3–4ms，代表那些具体调用，不是所有保存或整个新系统的上限。后续control实际出现snapshot177.052ms/完整tick185.967ms、GC0、3Page/569616B、CPU增量0；本机Windows计数呈15.625ms量级，0不等于完全没有计算。该区间没有捕获到CPU样本/长park或VM操作重合，精确停顿来源尚未确认，不能全称温度计算CPU或宣布修复。

关闭目的地准备的thermal-work-r1直接测MinecraftThermalInput.tick、drainCompletion及PlayerTemperatureUpdate两个handler。CT2仍是8源/1实际玩家；实录点全部生存模式，返回稳定位置和8个已加载点燃篝火。input tick mean0.150ms/P992.495ms，completion P990.050ms（包含在tick中），单玩家update P990.604ms，冷期max32.580ms。选定热入口和snapshot去嵌套的880tick操作段并集mean0.298/P996.150ms，240tick返回稳定段mean0.091/P990.855ms。这是入口覆盖范围，不能称全模组CPU，也不能用单玩家分位数乘100认证网络容量。

177ms未复现；补测最大snapshot27.542ms与24.540ms GC重合，初次捕获18.999ms，最终完整flush236.632ms、热data_save累计2.522ms。全部真实事件保留，不从统计里删除异常。

后台工作单列：变化较多400–799段20cut mean177.080ms，返回稳定1040–1279段12cut mean39.510ms，后者空间/输入阶段mean29.253ms、solve6.698ms、publish3.558ms；池CPUmean15.625ms计数粒度有限，不能算成同量main等待。每cut spatialPreparationRequested不证明完整重建；需要实际失效/复用/组装数据，才有生产优化依据。

已知目的地原版票据研究仅在gametest：900加入一张distance1、911ready、960传送、980移除，返回356→12ms，但准备/返回窗口最大启动间隔仍107ms，未证明总工作变少。control后段有旁观者模式命令，后段温差比较失效；该研究没有生产玩家传送入口，因此未接入或改造原版。三轮native全部完成、pending/fallback0、关闭Root0，配置已恢复。记录、口径与范围见[独立结果](../../build/ct2-preparation-and-own-thermal-work-20261014.md)及[有限计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## 当前交付与后续工作的边界

本机两处各128源、同总256源的可执行热里程碑已有真实长测、正常配置/保存、CT2与有限双维度结果，当前可按本文配置启用。后台空间准备仍可细化，但当前没有持续容量/时效失败证据，不能将“还能更快”自动当成启用前缺口。默认推广是独立的版本范围选择；100真实连接验收依用户现有工具条件暂缓，不虚报通过。

当前停止追加算法/原版加载研究。只有已支持目标中可复现的热年龄/几何超40tick、持续队列积压、预算拒绝/worker异常、保存错误或不被接受的新增行为差异，或者用户明确扩大目标，才开启新的有限任务。已知孤立wall/GC/原版加载峰值保留，不要求全部消失才能交付。范围、已达门槛与停止条件见[有限收尾文件](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## 真实单硬件线程的新增边界

2026-10-14按用户新要求把整个Forge JVM从启动前固定到本机一颗无SMT兄弟的E核（group0/logical23，affinity0x800000），不是只改Java可见CPU数。正常SHARED_GRID/native配置实际processors1/thermalWorkers1，Serial GC，原堆不变；有效两轮服务端504/417次mask观察全部一致。兼容性8项全部通过，两个JVM正常停止、Root0，未观察到因单核导致的零worker异常、热worker崩溃或死锁。

同核两基地256真实篝火/256熔炉/100热消费者容量未通过：初期出现空气年龄/几何超时，实际卸载返回后未在40tick内恢复，4000tick计划提前终止。后台90cut平均wall685.879ms、CPU115.625ms，最慢wall4996.226ms，其中topology4315.305ms；不能把wall全称热CPU。样本Root最高60.91MiB不是完整峰值，记录WORK_LIMITED0，不能把本次失败说成已证实的128MiB容量拒绝。

测试elapsed300直接调用相关ChunkMap.save，完整1454.389ms/热等待1302.851ms，属于同步请求的屏障fallback；它绕过日常维护队列选择时机，不是busyLifecycle强制flush，亦不证明每次自动保存均如此。实际卸载队列native8请求全部prepared、pending/fallback0、卸载热等待0。单核兼容性通过不等于256源容量通过；此前多核结果仍限于此前硬件，未重新认证单核CT2/真实网络百人。详见[有限单核报告](../../build/real-single-core-final-20261014.md)与[执行计划](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。本轮未改生产算法、热参数、线程策略或格式。

同一JFR后续离线补查显示：返回窗口热线程在target2400完成与2420开始之间停车约5024.835ms；主线程同步返回3678.088ms，返回后固定日程追赶tick，从加载tick结束到40tick失败所在tick结束仅1553.908ms。2420任务随后才完成，不能据此声称所有探针已恢复。故原断言失败保留，但它还不是单核稳态最大容量结论；恢复输入准备/提交时序与实际正常服务端加载路径需要分辨。补查没有运行新测试或放宽期限，详细时刻与证据见同报告末节。

后续用户授权的一轮诊断/一轮候选复验均再次因实际重载40tick未恢复失败。发现/捕获本身在loaded后1/2–6tick完成，提交要到+15tick；候选未解决该时序，已撤回。两轮真实卸载native8请求均全部prepared、pending/fallback0、卸载热等待0；直接相关保存累计热等待分别约0.536/473.891ms，并非固定自动保存耗时。生产保存架构未改，两轮停止Root0。结果与测量限制见[有限恢复报告](../../build/single-core-recovery-final-20261014.md)。
