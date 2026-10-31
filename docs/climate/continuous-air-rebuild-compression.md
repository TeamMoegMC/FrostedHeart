# 连续空气：重建耗时分解与组件矩阵复用

- Status: `Current for implementation and recorded trials; full256 and hundred-client acceptance incomplete`
- Last verified: `2026-10-06`
- Scope: `numerical construction of coarse/local Air; local and coupled materials; local i7-12800HX`
- Code anchors: `ThermalCompletion.RebuildTiming`, `ThermalDimensionEngine.process`, `ContinuousAirSolver.buildCandidate`, `AirOperatorFragment.integrateModalCell`, `CoupledThermalOperator.ContactFragment`, `SparseThermalPreconditioner.copyPattern`, `ContinuousAirTwoBaseProductionGameTests`

## 实际瓶颈

原统计的`topologyUs`同时包含原拓扑与连续数值层，不能解释成空气路径耗时。
本轮先补同一已完成cut的旧拓扑准备、连续构建、预算/报告和安装计时，再记录每个连续构建阶段。
日志仍沿用runner的20261003文件名后缀；本轮实际执行日期为2026-10-06。

实际两基地、只改A通道的对照在elapsed639记录了888.315ms连续构建。
它所在cut的旧拓扑准备13.914ms；以下均为同一次连续构建内的观测：

| Stage | ms | Meaning |
|---|---:|---|
| `shapesMs` | 70 | 当前几何上的源局部响应/历史形状 |
| `volumeAssemblyUs` | 100.156 | 第一遍体积质量与导热积分 |
| `rankUs` | 25.683 | 重叠局部函数的Gram秩处理 |
| `rankRecompileUs` | 81.708 | 筛除函数后再次编译体积 |
| `contactsMs` | 42 | 全维度表面采样/路由/远场接触 |
| `transportStructureUs` | 67.454 | 成对地址、耦合图和排序 |
| `nominalAssemblyUs` | 203.554 | 表面权重外积重复累加 |
| `airTransportPatternUs` | 137.235 | Air IC结构及填充 |
| `localBoundaryUs` | 19.348 | 有限材料表面响应索引 |
| `projectionMs` | 97 | 状态迁移、RHS、投影准备与求解 |

还有编码、projection矩阵组装、布局等小项。阶段存在不同层级，不能把父子统计相加。
这里最贵的重复工作是表面外积；几十个局部函数同处一Brick，使每个采样点的系数对数按宽度平方增长。
全局contact delta在旧布局/新质量尺度过小时退出，改变A后也重新累加B的表面项。
它不是128个篝火的W/J输入自身很昂贵，也不是get查询逐玩家求解。

## 已保留实现

组件矩阵缓存、同一次rank输入复用、三线性解析积分、投影按需IC准备和相同图的IC结构复用见[运行合同](continuous-air-runtime.md#numerical-construction-reuse)。
保留四点表面采样、double动态状态、当前热参数、现有运输/投影/冲量停止要求、20tick物理cut和40tick年龄。
局部材料试验仍显式选择，普通材料推进仍coupled；后端源码`continuousAir=false`仍保留。

表面缓存也支持coupled：只缓存Air/Air部分，所有材料/Air交叉项仍由当前原接触提供。
阶段数组按唯一backing预算，拒绝候选不会安装其温度/H状态。缓存增加内存，不能把CPU节省写成内存也降低。

曾试静态response残差从绝对1e-22放宽到相对初始积1e-14；同场景shape耗时差异不稳定，未证明端到端收益，已移除该试验代码。静态response保留原停止要求。

## 真实Forge结果

实际方块、煤能力、点燃/铁铲熄灭、真实worker/query/save路径；没有JUnit或人工热状态。
单独JVM顺序运行。lockstep仅用于生命周期/曲线筛查，真实容量计时使用realtime20。

只改A、1200tick场景：原连续重建888.315ms，组件缓存后同一通道阶段685.636ms，
再加入rank输入复用和解析积分的版本629.465ms。对应第一遍体积100.156→35.316ms，
rank后81.708→7.699ms，nominal外积203.554→13.189ms。
它们来自独立真实世界，启动/JIT/GC与异步捕获历史有差异，不能视为同一温场的严格逐项证明。

2400tick完整两基地源启停对照均使用local材料、总256真实源和two_bases容量档位：

| Completed cut measure | Previous expanded run | Cache-only stage trial | Final hybrid |
|---|---:|---:|---:|
| count | 118 | 119 | 118 |
| full P50/P95/P99 ms | 437.418 /1372.256 /1833.500 | 338.433 /794.019 /1094.920 | 470.473 /985.614 /1222.424 |
| topology P50/P95/P99 ms | 240.714 /1215.405 /1655.818 | 209.156 /596.641 /882.063 | 270.221 /788.507 /1033.174 |
| solve P50/P95/P99 ms | 119.439 /185.712 /195.796 | 90.894 /149.940 /172.507 | 122.815 /169.909 /184.500 |
| root peak charged MiB | 146.518 | 161.469 | 161.412 |

阶段试验在200tick之后完整P95/P99为730.851/858.618ms；最终版冷启动峰值约2.4s。
这些是worker墙钟和预算计费峰值，不是全服MSPT或JVM实际堆。
阶段试验曾完整P95下降约42%、拓扑P95下降约51%；最终复跑对应约28%/35%。
不同运行存在明显墙钟漂移，不以最好一次代表稳定收益。P95<=500/P99<=800ms门槛仍未达到。

完整阶段新场景实际启停/重燃完成，最终256 enabled sources、16 LIVE楼板；12000次环境查询，
age violations=0、receiver violations=0、WORK_LIMITED=0。严格逐点连续geometry mismatch超过40tick的计数=268，
因此GameTest仍失败。计数按探针和tick累计，不是268次worker崩溃或求解失败。
没有发生worker数值失败。两基地容量用扩大档位消除了已知128MiB准入拒绝，并不证明默认预算通过。
最终hybrid复跑仍有256源/16楼板、无WORK_LIMITED/数值失败，但age=48、receiver=15、geometry=302累计记录。
熄灭半数后的target1220 cut为1222.424ms，上一份1200样本在新cut完成前超40tick，造成短暂超龄。
因此最终代码仍未完成256验收，不能把阶段试验的零超龄写成最终结果。

新增deadline日志在尚未进行601tick门操作时便记录了超时。例如elapsed389的探针3/7：
firstMismatch348、expected7、last7、live9、firstStaleTick380、sampleTick380。
这说明Page已经推进到新捕获，但新一轮几何/halo失效又发生；持续mismatch不等同于同一个旧几何41tick完全未处理。
严格`geometry_current`仍不满足，不能通过放宽年龄或删除断言宣称成功。
下一轮必须区分同一几何修改的恢复时间、持续更新期间捕获进展和真正失效的查询范围。

真实房间105个同sampleTick、同气候的已发布观测：最大near差9.069e-7°C、far差1.295e-6°C、rise差2.202e-6°C，floor差0；
66/66余热样本继续下降，没有停滞。这不是全场误差证明。普通coupled材料的五项生产生命周期均通过；最终hybrid与重读代码也再次5/5通过。
错位128源、跨Page实际场景通过，root峰值85.128MiB；该场景使用lockstep，不能作为真实吞吐结果。
单基地首轮完整P95/P99为414.678/717.644ms、root峰值93.441MiB。
它记录了1tick Air read丢失而失败：elapsed881诊断geometry_current/sampleTick860，实际样本年龄约23tick，
随后elapsed882的target880 cut仅177.066ms便发布；不是40tick旧样本或超1s计算。
源码Page引用检查和query交换都可能跨过一次读取。已加只在交换/引用变化时重读一次，
保留原时钟与所有validity/age检查。随后单基地复跑通过、三项违规都0，root93.439MiB；
完整P95/P99=606.689/911.624ms，显示耗时漂移。该次`airPublicationRetries=0`，没有再次触发竞态，
所以不能把一次通过解释为该竞态已得到稳定复现与修复证明。

缓存接入最初让健康布局也退出全局delta。最终已恢复原`buildTransportDelta`，
不适用时才按组件缓存做fresh组装；同一编译版本、同气候顺序对照：

| Real128 measure | global/gauss/eager/no rank-input reuse | final hybrid defaults |
|---|---:|---:|
| full P50/P95/P99 ms | 235.839 /612.957 /1335.148 | 232.897 /557.634 /800.913 |
| topology P95/P99 ms | 527.460 /1225.931 | 458.090 /725.556 |
| root peak MiB | 86.414 | 93.441 |
| freshness acceptance | fail, one age tick after passage change | pass, age/geometry/receiver0 |

最终128版77次重建中48次走增量图。中位完整耗时基本不变，最慢重建改善；
不能把单基地说成整体吞吐大幅提升。64个同sampleTick/同自然温度的密集观测最大Air差0.056233°C；
异步空间捕获/推进历史也不同。这未证明全场0.01°C，已记录到`build/rebuild-paired-hybrid-128-comparison-20261006.json`。

原始报告：`build/rebuild-compression-results-20261006.json`；曲线对照`build/rebuild-final-room-comparison-20261006.json`。
这些build文件是可重建的本机观测，不是版本固定或路径敏感的产物验证。

## 剩余交付边界

目前全维度粗编号、contact编译、rank处理、迁移和变化图的IC仍会准备；组件缓存不是区域运行时。
下一项转向持久区域/稳定局部地址与按Brick/实际依赖范围发布：未变B静态构建/投影=0，B自身推进不等慢A。
保留本轮体积与表面缓存作为区域内部算子，之后的区域、共享局部自由度或细网格都可继续使用其组装经验。

密集单一连通区若仍超门槛，再做有界的局部模式压缩，以真实近火/隔墙/缝隙/余热误差验收。
约0.01°C是玩家曲线目标，不能直接当作每个构建残差或累计温场误差证明。
100 FakePlayer环境消费者不包含100真实客户端的人体、网络、AI、MSPT/TPS认证。
