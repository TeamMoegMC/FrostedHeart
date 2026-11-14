# 服务端内存故障与复刻证据

- Status: Current for causally reproduced callback retention and the validated source fix; the original server's full object distribution and extreme-load real-time performance are not established by these tests.
- Last verified: 2026-11-10.
- Scope: Java heap OOM、Full GC、热运行时对象生命周期、分配量、协调轮推进和保存；本地复刻与原服证据分别标记。
- Code anchors: `RegionalAirRuntime.trackPendingRetention`、`SpatialSourceIngress.pendingFor`、`WorkerPhysicalSourceBindings.airPortTargets`、`ThermalDimensionEngine.processWorkRound/startGeometryWork/GeometryWork`。

## 2026-11-10：已确认的历史重建对象留存

持续拆建实验已把存活堆增长归因到具体引用链：`ThermalRegion.pending → retentionChanged → 捕获RebuildPreparation.this的lambda → 已结束重建的历史几何、区域和数组`。额度已经归还，Java强引用仍存在，所以这部分历史对象不再显示在热系统当前准入计账中。该缺陷的修复已在`RegionalAirRuntime.trackPendingRetention`：由外层运行时创建回调，只保留运行时和当前区域所需的引用。共享给真正重建任务的集合仍遵守原所有权交接，不能为释放准备对象而清空它们。

实验使用现有已编译组件，仅把当前运行环境中的`RegionalAirRuntime$RebuildPreparation`替换为已有的修复前内部类作为控制。正常生产路径的差别是回调捕获；该旧类另缺少被动诊断通知、异常消息较短，生产observer为空且未出现该异常。4GiB堆、128MiB目标、1000ms/s额度、四个热worker、三名服务端FakePlayer、3072个真实营火；每20tick在三个基地各替换4个营火地基方块。玩家保持基地加载，区块跟踪数量稳定，没有持续跑图。

- 较短旧回调控制的有效live堆：140个`closed=true`且`scratch.released=true`的准备对象，通过1669个已安装区域的回调继续存活；其共同独占保留集为8,427,636个对象、1,804,509,496B（约1.68GiB），占该堆约62%。旧组Full GC后从2820MiB进一步升至3623MiB，热计账约281～287MiB。
- 延长至24000tick、13704次替换的旧回调控制：186个已经关闭并归还额度的准备对象，独占保留14,346,479个对象、2,820,338,824B（约2.63GiB），占3,955,985,264B可达堆约71%。负载期间38次自然Full GC，关闭期间另30次，诊断采集1次；最差一个日历分钟所有GC暂停约17.38秒。该20分钟窗口没有抛出Java OOM异常，按计划正常结束，不能把它写成OOM异常已经重现。
- 修复版完成相同首14400tick内的7944次替换，再保持营火运行至24000tick。约12分钟和20分钟两份完整live堆均没有仍存活的已结束准备对象；可达堆分别为1,125,893,152B和1,123,493,256B，JVM两次诊断Full GC后均为1131MiB。0次自然Full GC、0次OOM、0次worker/保存失败，四次显式保存完成。

相同负载的直接比较限于首12分钟；延长的旧组持续改块20分钟，修复组后8分钟用于观察恢复，不能把整个20分钟GC次数包装成完全相同负载的性能A/B。这里的根因证据同时包含真实引用路径、已归还额度的对象集合、独占保留大小和回调差别的控制。原服所有6GiB对象的名单仍需原服堆才能逐一确认，但这条生产代码中的异常持有已经得到实际复刻和修复验证。

延长旧堆扣除这批被完成准备对象独占保留的2,820,338,824B后，剩余1,135,646,440B；修复组末尾为1,123,493,256B，两者接近。因此数GiB的额外占用有对象持有证据解释，不是把所有堆增长笼统归因于准入计数或临时分配。

第一组末尾诊断快照因本地诊断磁盘不足而不完整，已删除该不完整文件；引用链分析使用之前成功取得的live堆。该较早堆已完整压缩归档，分析报告保留。后续实验移至有足够空间的工作目录，修复组两份堆及延长控制的最终堆均完整。诊断程序也修正了不完整堆及非零进程退出码不能被记为成功的判断。磁盘错误没有计为Java OOM。原始文件位置由本地`build/thermal-mutation-retention-20261109/diagnostic-location.json`记录，结构化结论见[因果验证回执](validation/thermal-retention-causal-2026-11-10.json)。

大范围重建与引用泄漏需要分别解释。5920tick的实际输入包含8个位置更新、18条材料变化、1项居留更新及1个halo更新，形成29个Section写入和255个Brick片段。`TopologyUpdatePlanner.continuationBoundary`采用保守的相邻已捕获/已宣布Section闭包，再由`RegionalAirRuntime.expandBoundary`扩到完整相关区域，因此涉及551个旧区域；实际只新建3个，复用548个。最大的候选含37823个单元和2097个空气组件，整项准备额度28,760,392B。这个单次额度没有解释数GiB的持续增长；结束后的多轮准备对象被旧回调留下，才构成上面的异常持有。

源码修复与此前10/10统一语义回归仍有效。本轮没有另打发布包。极端负载下的部分区域推进迟滞仍存在；用户已明确要求只完成源码修复和总结，其余后续工作由用户处理。

## 2026-11-10：剩余约1.12GB与类似引用的补查

这里的1,123,493,256B是专用服务端live堆中仍然可达的Java对象，约1.12GB（1071MiB）。它不代表客户端用量、JVM最大/已提交堆或整个进程驻留内存。F3的`Mem`显示即时堆使用，包含尚未回收的临时对象；任务管理器还会看到堆驻留页、原生缓冲区和线程栈等。客户端额外加载渲染与资源数据，单人还运行内置服务端，因此`runClient`的3～4GB不能直接与该专用服GC后可达对象量比较。

按包实例及其独占附属对象的保留集去重，热模拟运行时为229,526,720B（约219MiB）；其余893,966,536B（约853MiB）来自Minecraft、共享注册数据、类加载、脚本及其他服务端对象。最大单一dominator为`ChunkMap`，约205MiB。FH包实例及独占对象合计232,053,880B；共享的Minecraft方块状态和注册表等不重复归入该数，因此它不是模组所有可能运行成本的完整归属。128MiB仍为可选新工作的准入目标，不是整个服务端堆的上限。

两份修复版live堆和相关源码补查没有发现第二处同类的已完成对象异常持有：已释放的重建、已释放的冻结几何、已关闭的FrameLease均无遗留；中途539个读者租约在末尾归零，末尾1682份当前帧与1682个已安装区域对应，所有已安装Scope的delegate均指向当前`WorkerPageStore`。末尾ready/memory/cleanup队列及active工作集合均空，预算容量等待回调为0。父帧租约关闭会清空持有表，安装会切换Scope的冻结视图，完成的数值构建会移除CandidateBuild，快照只接管结果数组；没有发现准备阶段回调被安装后的算子/模板继续保留。

用户确认超时指旧版泄漏当时的现象。原服GC最坏日历分钟有约55.1秒停在STW GC，可直接造成主线程长时间无法服务。保存代码仍有当前控制回合的协调等待；已有回归验证四worker全部暂停时真实磁盘保存14.5685ms完成，不能把它扩展成所有负载下都没有等待或超时的保证。本次补查使用已有堆和源码，没有启动新压力测试，也没有增加推测性的生产补丁。范围、数值与路径见[剩余堆核查回执](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-live-footprint-2026-11-10)。

## 2026-11-09：此前跑图与分配调查

2026-11-09继续使用已有GC日志、已有本地堆和专用服复刻深入检查，新增证据见[根因调查回执](validation/thermal-memory-root-cause-2026-11-09.json)。原服Full GC后约6GiB仍存活与本地短时大量分配是两类证据，不能混作同一项已完成的根因归属。

`build/thermal-root-profile-20261109/current-cpu1000`在4GiB堆、128MiB目标、1000ms/s额度、四个热worker、3072真实营火和三名服务端FakePlayer区块跟踪下完成9600tick。第800至7200tick的JFR匹配窗口约320.5秒；窗口中`ThreadAllocationStatistics`覆盖的280.5秒记录到热worker分配计数增长61,194,599,984B，约208MiB/s。`pendingFor → checkpointTarget`调用链的分配采样权重约42,018,828,824B。这是计数区间的分配量和采样归因，不是42GB存活对象或精确对象总量。该路径每个端口只需布尔归属，却反复构造不可变保存目标和临时解析对象。当前改为复用协调者已有解析空间；30,000次真实端口检查的独立线程分配计数为0B，同时检查了接收区域边界、已登记源、辐射损失和缺失几何的行为。

同一记录约半数热worker CPU采样包含`refreshBoundaryHoldsLocked`。原协调轮在这些前置工作之前建立2ms截止时间，前置耗时超出后仍轮转执行类别；数值队列会直接返回。3ms实际前置耗时的确定性控制在旧实现连续120轮不能推进。当前把选中工作的时间片起点放在前置处理之后，整轮仍计入原共享工作额度。这个控制证明调度缺陷存在，JFR采样证明扫描成本显著；不能据此精确断言旧堆里每个READY任务都是同一原因。

已有20分钟运行末尾的119个等待`BoundaryCandidate`还合计预留61,757,376B（58.9MiB），其中大部分是尚未创建的全源快照准备额度，也包含真实边界元数据。当前等待时只登记边界集合，选中任务后、真正冻结源之前才登记快照额度。该缺陷会放大准入压力，属于计账提前且重复；它本身不能解释原服数GB的可达对象，也不能将58.9MiB全数宣称为真实堆节省。

上述8分钟剖析有5次自然G1 Full GC，另1次由22:33:37的诊断堆采集触发；自然Full GC后为1191–1251MiB。现场live堆经MAT得到约1.1GiB可达对象，最大单一dominator为原版`ChunkMap`约220MiB，各`IOWorker`没有显著积压。它复现了大量分配和可观察的Full GC，尚未复现原服Full GC后仍保留约6GiB的堆耗尽。JFR开启会增加当前诊断事件，修复前后性能比较使用相同采样方式，不能把本轮数值当作未开启剖析的完整整合包性能。

最终源码的统一GameTest为10/10通过，日志`build/thermal-root-cause-unified-final-20261109.log`。包括21个已完成准备对象在热运行时仍存活时全部回收，较晚及待安装连接Page顺序、卸载清理边界、6,400J并发保存一次结算、四个worker全部暂停时的真实原版保存与磁盘NBT检查；该次暂停保存用时14.5685ms。新增调度回归从旧实现实际3ms前置耗时下的120轮不推进，变成3轮推进至40tick。第一次统一运行9/10，新增测试错误使用了仅在存在未完成几何时计数的`independentCommits`；最终改为检查真实`commitRevision`及物理tick后重跑整组，没有改变生产数值规则来迎合断言。

最终同配置专用服运行`build/thermal-root-cause-final-profile-20261109/current-cpu1000`完整完成9600tick，访问21983个不同跟踪区块位置，有1次显式保存；热worker错误、区块保存错误和OOM均为0。相同800至7200tick窗口中，热worker实际分配计数采样区间速率约85MiB/s，比基线约208MiB/s低59%；原检查路径中的不可变保存目标分配不再出现。计数事件覆盖的时间为272.6秒，比较使用各自计数区间的每秒速率，不把两次原始累计计数直接相减。

**该对照没有通过“原始OOM已彻底修复”或“实时性能达标”的验收。** 排除诊断堆采集，基线负载期间4次自然Full GC，最终源码为5次；各自关闭期间另有1次自然Full GC，各另有1次诊断Full GC。整个进程全部GC暂停合计约9.695秒与9.755秒，包含关闭及诊断，不是纯游戏时段的精确配对。单次对照不能把这1次计数差归因于某一补丁，但明确不能宣称Full GC已减少或消失。最终live堆约1.11GiB，没有遗留完成的`RebuildPreparation`；输入已到9680tick，474个受热区域都只到5920tick，推进滞后仍存在。

剩余分配主要来自主线程及地形生成线程，包括原版光照表复制、区块调色板序列化、流体更新和噪声生成。本模组`BiomeColumnSolver.baseFitness`也有约10.7GB分配采样权重：`ColumnCachingBiomeResolver`每个区块复制气候参数表，并为16个quart列分别创建参数规模的fitness数组。它是实际临时分配来源；当前没有历史对象异常持有证据，因此没有将它当作原服6GiB存活对象的根因，也未修改群系、地形算法或增加全局缓存。原服Full GC后仍存活约6GiB的对象归属保持未定位，不能用本地约1.1GiB的结果替代。

已在本地专用服务端的真实堆快照和已发布字节码中确认一处引用泄漏：长寿命热区的 `RegionPending.retentionChanged` 回调捕获了内部类 `RebuildPreparation.this`，继续持有已完成的重建对象、旧热区和几何准备数据。即使准备额度已经归还，Java 强引用仍然存在；128MiB 准入目标无法发现或回收这部分对象。

最小修复是在 `RegionalAirRuntime` 的外部方法 `trackPendingRetention` 中创建同一回调，使它仅捕获运行时和当前热区。回调的计账逻辑不变，已转交给 `Rebuild` 的集合也不清空。独立 Java 21 GC 对照验证了两种捕获方式的差异；真实 GameTest 又在热系统保持运行、控制房间仍受热的情况下验证 20 个已完成重建对象全部可回收。日志为 `build/thermal-preparation-retention-live-20261109.log`，1/1 通过。

默认 1000ms/s 工作额度下的跑图复刻另暴露了几何交接错误：新任务只等待现存热区到达边界，没有检查影响范围内更早的几何输入。当新捕获位置还没有热区时，较晚的区块描述可以先发布；尚未安装的连接区块又能在之后扩大旧输入的影响范围，最终要求新热区从更晚的物理时间回到旧边界。

修复在选取结构任务时检查范围内的 `topologyIngress` 和 `topologyInputs` 最早时间，并在路径范围计算中包含所有已收到、尚未安装的 Page 和实际 halo 足迹。该索引只持有段坐标，保留容量计入原有几何额度。新输入序列会使候选范围缓存失效；任务只对其捕获的序列原子登记。有更早输入时先处理旧输入，不改写 tick 或取消热量交接检查。卸载清理中服务数值任务的分支也先刷新新边界。

两相邻区块的实际发布回归在去掉输入顺序检查时失败，日志为 `build/thermal-geometry-input-order-control-20261109.log`。新增的范围回归还包含两个尚未安装的连接 Page，先访问较晚岛区并验证较早几何先发布。

关闭阶段的实际保存还触发了一次 `ConcurrentModificationException`：`ThermalDimensionMailbox.workPending()` 在检查 RUNNING 状态之前，先遍历了 coordinator 正在修改的集合。现在先检查运行、排队和关闭状态，只有 coordinator 空闲时才读取其集合；主线程检查不需要等待数值任务。

当前源码统一回归 `build/thermal-memory-prospective-unified-20261109.log` 为 7/7 通过：21 个完成对象在热系统仍运行时全部回收；相邻 Page 与待安装连接 Page 的实际发布顺序通过；卸载清理边界通过；运行中保存不取消任务，6,400J 输入只结算一次；暂停四个 worker 的原版磁盘保存用时 21.4ms，磁盘 NBT 保留 20,480J 待处理热量与真实铁块替换。诊断期间的临时首次晚到边界追踪已移除；额外保留的错误字段仅在交接异常时创建。较长的专用服务端压力结果以验证 JSON 中的最终回执为准，7 项通过不代表该长测已通过。

该源码的默认配置长测已完整完成，运行目录为 `build/thermal-memory-prospective-1000-20261109/current-cpu1000`：4GiB 堆、1000ms/s 共享工作额度、四个热 worker、3,072 个真实营火，三个注册进原版区块跟踪的服务端 FakePlayer。负载完成 24,000 tick、四次保存与 44,582 个不同区块位置，热 worker 失败、区块保存失败和 OOM 均为 0。热系统只启动一次，正常关闭；I/O 邮箱和原生检查点等待数在末尾均为 0。计账峰值 413,831,174B，约 394.7MiB，128MiB 仍然是准入目标。

结束前取得的 live 堆中，可达对象共 1,159,881,656B，约 1.08GiB；运行中的中间快照与结束快照均没有存活的 `RebuildPreparation`。对照产物的 live 堆保留了 38 个已结束重建对象。两次修复后快照均有活动的、未进入失败恢复的热运行时，不能用引擎关闭后的低堆占用代替这项验证。

这次通过的是内存生命周期、几何交接与保存的回归；极端负载下的实时推进仍不达标。结束快照输入已到 24,060 tick，428 个接收热源输入的热区处于 13,740～13,780 tick，仍有 179 个几何输入位置等待；温度探针也记录到不可读状态。普通 Full GC 后占用约 1.1～1.2GiB，但这些事实不能证明全部等待或吞吐问题已修复，也不能证明原服 OOM 只有一个原因。完整结果及原始文件位置记录在 `docs/climate/validation/thermal-overload-and-persistence-2026-11-09.json` 的 `final_source_results`。

容量预检、精度恢复条件和减少准备并发仍属于过载缓解措施，不应替代引用生命周期修复。原始远端事故没有堆快照，本地确认的这条泄漏不能据此宣称是 2026-11-08 22:24:52 堆耗尽的唯一原因。累计分配量、内部准入计账和 GC 后的存活堆是三个不同指标。

服务器已停机、手头只有日志时，无法还原当时的对象引用图；仍可在本地运行已有服务端产物，主动尝试复刻同一条故障链路。原存档、完整模组清单与原JVM配置下的证据有助于判断复刻是否相同。本地负载没有崩溃，只能说明该负载未触发问题。采集不需要重新build模组，对照时应保持内存上限、视距和负载一致。

启动参数至少保留以下诊断项；原有`-Xms`、`-Xmx`和javaagent沿用实际服务器配置。用户最后确认当前服务器使用4GiB堆。

```text
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=.
-Xlog:gc*:file=gc-%p-%t.log:time,uptime,level,tags:filecount=3,filesize=20M
```

堆快照写入服务端当前工作目录。GC日志使用PID区分自动重启前后的进程。这里不启用高频线程快照，也不默认启动JFR；当前热系统会在JFR初始化后生成额外诊断事件，可能改变分配负载。

服务器启动后，在服务端机器执行[采集脚本](../../Scripts/collect-server-memory.ps1)。可通过 `-ServerProcessId` 指定实际服务端 PID；省略时只在唯一可识别的专用服务端进程存在时自动选择。脚本在附加前排除已知 Gradle 和客户端进程，多进程不明确时停止。不要把本机Gradle或GameTest进程当作远端服务器。

```powershell
powershell -NoProfile -File .\Scripts\collect-server-memory.ps1 -ServerProcessId 12345 -ServerDirectory 'E:\TWRNEO_DEV'
```

把示例PID替换为本次真实PID。脚本也可单独复制到服务端后执行，并通过`-JcmdPath`指定同版本JDK的`jcmd.exe`。默认持续到指定进程退出或按Ctrl+C；`-DurationMinutes 60`可限定为一小时。脚本不启动、不停止服务器，不修改存档、配置或模组，也不自动上传数据。

正常情况下每15秒记录一次堆概况、实际线程数和进程内存，不主动Full GC。脚本读取真实GC日志；当连续3次观察到GC后占用仍达到最大堆的80%时，采集一次线程堆栈、全部对象直方图及live heap dump。80%只是提前采集证据的阈值，不是新的内存控制机制。这个触发覆盖G1的Young/Full GC摘要；正常回收前短暂用到3.6GiB不会直接触发。

堆快照会暂停JVM，并可能占用数GiB磁盘空间。采集前应在所选输出位置留出相应空间。需要主动取得当前堆时可加`-CaptureHeapNow`；这不是默认周期操作。直方图使用`-all`，包含尚未回收的垃圾，不可直接当作存活内存；持有者分析以随后取得的live heap dump为准。发生OOM时，JVM启动参数提供独立的自动堆快照，不依赖采集脚本及时触发。

输出目录包含实际JVM参数、Java版本、模组文件清单、Frosted Heart JAR散列、GC日志、服务端日志和采样记录。`*.hprof`保留对象内容，应留在本地，分析时提取主要持有者与GC-root引用路径即可。

已有GC日志可离线检查，完全不连接Java进程：

```powershell
powershell -NoProfile -File .\Scripts\collect-server-memory.ps1 -InspectGcLog 'D:\MC\gc-17884-2026-11-08_23-23-22.log'
```

拿到证据后，先比较GC后持续增长的对象和retained size，再追踪它们的强引用来源，确认是热系统数组、未归还的查询引用、区块/NBT队列、实体，还是其他模组。针对已证实的生命周期或准入缺口修改代码，并在相同负载下比较旧版与修复版。缺少这一步时，测试通过只能说明相应场景通过，不能称为原始OOM已彻底修复。

本地复刻入口为 `Scripts/run-thermal-oom-repro.py` 与外部 KubeJS 脚本 `Scripts/thermal-oom-repro.js`。它们复用已有的 deobf JAR 和已导出的专用服务端启动清单，不执行 Gradle，也不载入 GameTest 类。Java 21、4GiB 堆、128MiB 热目标、固定普通地形种子，使用三个注册进服务端玩家列表和原版区块跟踪器的 FakePlayer 对象。它们不包含真实网络客户端、客户端包队列和原服完整整合包。

必要增量编译已获得用户授权。复刻器的 `--classes-directory` 可把编译后的生产类覆盖到隔离的解包目录，保留已有资源并记录类目录散列；因此验证新源码不需要制作发布 JAR。`publishedFields` 遥测直接读取营火口袋中的已发布 Air，记录可读性、物理 tick、精度和温度；该诊断不请求新的捕获，也不持有区块票。

原服日志在 2026-11-08 15:08:24 明确记录 `Thermal checkpoint selected: mode=native_ready`。复刻器默认显式设置 `continuousAirNativeCheckpoint=true`。本机 `run/config` 中曾关闭该选项，早期继承这项配置的测试不能用来证明原服的提前卸载检查点路径已经通过。`--no-native-checkpoint` 只用作另一种存档模式的对照。

`native_ready` 表示通过原版卸载队列提前准备区块的热状态检查点，再继续原版卸载。需要区分两件事：保存已接受的温度与尚未消费的热量，以及等待数值任务结束后撤销该区块的运行时所有权。数值推进滞后不应成为普通存档的完成条件；卸载中的清理等待是否积压，则要同时观察检查点、原版区块持有者和队列，不能只看热系统内部计账。

下面是同一压力负载的两个已有产物对照；分别执行 `old` 与 `current`，输出目录不允许覆盖。`pockets` 使用既有过载回归中的真实石头和营火结构，保留一人在热源基地，另外两人活动，其中一人持续加载新地形、另一人往返基地。这是合成压力测试，热源数量不代表原服实际数量。

```powershell
python Scripts/run-thermal-oom-repro.py --variant old --minutes 20 --cpu-budget 250 --sources-per-base 1024 --geometry pockets --resident-player --warmup-ticks 2400 --snapshot-tick 6000 --capture-heap --capture-at-end-only --root build/thermal-native-paired-20261109
python Scripts/run-thermal-oom-repro.py --variant current --minutes 20 --cpu-budget 250 --sources-per-base 1024 --geometry pockets --resident-player --warmup-ticks 2400 --snapshot-tick 6000 --capture-heap --capture-at-end-only --root build/thermal-native-paired-20261109
```

`run-metadata.json` 记录产物散列、脚本散列和参数；`telemetry-samples.jsonl` 记录 GC 摘要、进程内存、实际线程数、不同区块位置、磁盘区块计数，以及热计账和原生检查点请求／完成／等待数。`loadedChunks` 来自原版跟踪计数，包含生成中的状态，不能解释成全部为 FULL 区块。磁盘区块数读取 `.mca` 位置表。

`scenario-finished.json` 必须达到请求的游戏 tick 数；`exit.json` 同时记录工作负载是否完成。生产 worker 失败、脚本报错、墙钟截止、进程无退出回执或中途终止都不能记为一次通过的 OOM 回归。复刻器检测生产热系统错误，记录 `runtime-failed.json`，在首次故障后采集线程和 `-all` 堆快照并停止；JVM 退出码为 0 也标记 `thermal_runtime_failed`。退出前还审计最后的堆采集和关闭阶段日志。

启用 `--capture-at-end-only` 后，完成前不通过诊断强制 Full GC；OOM 自动快照始终启用。完成时保存 live heap dump，墙钟超时也抓取快照但标记负载未完成。分析中另行取得的 `-all` 快照包括不可达对象，持有者分析需先剔除不可达对象，文件大小也不等于存活堆大小。

对照脚本在第 6,000 tick 另取一次 `-all` 堆快照，不强制 GC，保证结束前已有对象持有证据；两版使用相同采集时点。第 12,000 tick 通过实际燃料能力给当前已加载营火补煤，维持 20 分钟内的热源负载。实际 JVM 可用处理器数固定为 4（之前启动清单最后的同名参数也是 4），热工作额度使用本机配置 250ms/s；2026-11-09 新日志已确认原服启动默认额度为 1000ms/s，本地 250ms/s 对照不能视作配置完全一致。已有导出运行环境为 Forge 47.3.0、Java 21.0.9，用户原启动脚本为 NeoForge 47.1.106、Java 21.0.10，这些差异不能省略。
