# 连续空气：函数筛选缓存、精确halo失效与局部因子试验

- Status: `Historical trials; the local function generators, rank caches and implicit factors described below have been retired`
- Last verified: `2026-10-06`
- Scope: `coarse/local Air build preparation, real geometry dependency invalidation, local i7-12800HX`
- Code anchors: `LocalAirBasisCache.prepare`, `LocalAirBasisCache.Preparation.finish`, `LocalAirModeRank.independent`, `AirShapeStore.ResponseStencil`, `MinecraftPageManager.invalidateHaloConsumers`, `AirFactorBlocks`, `CoupledThermalOperator.prepareAirTransportPreconditioner`

2026-10-25说明：本页列出的旧函数构建、基底筛选缓存和隐式因子已退出当前源码；halo位置失效及共享网格支持发现仍由现有实现提供。当前行为与本批删除后的验证入口见[运行时控制](thermal-runtime-control.md)，以下数据只对应原日期的试验。

2026-10-08后续已经完成pairwise无用隐式材料准备裁剪与两对真实256源观察。
每场累计分配下降8.74%/10.35%，已计费Root峰各约下降2.806MiB；CPU分别-21.52%/+5.30%，
整体CPU收益未重复证实，不能把本项写成原20%/30%目标达成。
pairwise保留材料储热、空气换热、内部传导和相变；implicit仍准备完整反馈表，表面复用仍保留索引。
最终173次重建原因都已关联，其中70次含Air构建确有Air身份/掩码变化，覆盖graph累计墙钟约80.9%；
下一项重点为前沿变化时局部空间编译，未实施全局稳定节点库或动态连通框架。
本文件下文保留原解析式历史，当前实现和剩余门槛见
[原执行表](../../diary/2026-10-29_23-38-00_thermal-development-history.md)及
[本次开发记录](../../diary/2026-10-29_23-38-00_thermal-development-history.md)。

## 新确认的重复工作

旧`ContinuousAirSolver.buildCandidate`每次从完整未筛选局部函数开始积分，再建立Gram矩阵筛选。
即使上次筛掉函数，下一次仍重新积分它；另一个基地的函数和支持Air不变，也重新处理其秩。
上一轮矩阵数组复用并没有覆盖这段工作发生前的决策。

`LocalAirBasisCache`默认`cached`；显式`FH_CONTINUOUS_AIR_BASIS_TRIAL=original`用于旧路径对照。
原始函数按实际Air组件中的重叠关系分组，比较每个函数的实际值/身份，以及全部支持组件的几何和局部函数。
几何捕获版本或全局紧凑编号变化本身不改变秩。未变组先应用已接受筛选，变化组才积分/筛选。
候选缓存在失败时随`SolverState`还原，当前/旧backing纳入计费，不保存额外Gram矩阵。
这只是函数重叠分组，尚不是独立热区域的温度、材料、源或任务所有权。

真实只改A的1200tick记录：53次重建中44次全部组复用，稳定rank约80–160µs。
通道变化时A两个局部分组重算，B范围`164:216`的256函数复用。
部分未变化函数的体积矩阵也可直接沿用，仍有全维度layout/contact/graph/projection工作。

## 实际halo位置决定失效

原`haloConsumers`只索引Section。该Section的任意几何变化都会让所有消费者Page失效，
无论变化点是否在实际已捕获的邻接数据中。这既引起额外捕获/重建，也反复清除`geometry_current`。

现在`PageEntry.capturedHalo`共享同一份原`GeometryHalo`，使用原mutation位图取变化点并查询已捕获的有序位置。
只有实际halo位置变化才使消费者失效；完整重同步和区块加载/卸载继续失效所有相关消费者。
直接捕获范围内的几何变化仍执行原规则；没有跳过相关墙体、放宽40tick或延后测试监测。
`irrelevantHaloGeometry`诊断计数记录被剔除的无关消费者失效。
原位图在所有halo消费完成后才清空，未增加逐变化坐标数组。

多次真实256场景geometry累计违规由上一轮302降至0；最后scalar对照仍有26个真正恢复超时，
同时慢cut和持续新事件仍影响有效性。精确化依赖范围不等同于解决所有发布等待。

## 静态响应邻接表

`AirShapeStore.ResponseStencil`只对原`connectedAir`准备一次实际Air单元、邻接和对角增量序列，
共享给scale1/scale3两个静态response。PCG仅遍历实际Air，保持原单元/邻居相加顺序、屏蔽/边界定义、
两种screen、256迭代上限和1e-22停止要求。`LocalAirShape`仍保存原trilinear数据；动态温度没有改成网格均值。
这是减少重复坐标判定，未放宽构建精度。单独冷启动收益未被严格隔离，不能把它宣称为数倍提速。

## 允许损失精度的实际试验

显式`FH_CONTINUOUS_AIR_BASIS_TRIAL=compact`优先保留scale1，再筛长尺度函数。
scale1仍用原相对平方残差1e-6，scale>1试验用1e-2。平方残差是质量内积下的相关性，不是°C误差。
第一个1e-4版本在稳定512函数时几乎不减少；修正到1e-2后约464函数。
相同发布时间/自然温度的144个密集观测，最大Air差0.124966°C，含独立世界捕获/推进差异。
该场景完整P95/P99=931.911/1185.954ms，root150.735MiB；age64/receiver20/geometry0，因此仍失败。
函数数量与内存有所下降，但不足以证明对玩家约0.01°C目标的稳定性能交换，未设为默认。
目前保留的是原筛选标准的缓存，热源位置/W/J、M/K定义、有限材料H/phase、停止要求、20tick与40tick不变。

## 正Air迭代块的结构复用试验

`FH_CONTINUOUS_AIR_FACTOR_TRIAL=blocks`显式选择`AirFactorBlocks`，默认仍scalar。
按当前正Air迭代块的图拆分，保存物理系数身份，当前全局地址变化时可重用旧IC结构；每次仍刷新数值因子。
材料负反馈/物理算子、RHS和独立P32停止估计不变。不是区域求解器或区域快照。
首版修改块内排序与fill级别导致完整延迟变差；一次修正保留原相对编号和全局fill强度。

修正后与scalar同一编译版本、同气候、2400tick的实际256顺序对照：

| measure | scalar | blocks, preserved order/fill |
|---|---:|---:|
| full P50/P95/P99 ms | 471.811 /995.121 /1381.571 | 423.861 /793.453 /1230.707 |
| topology P50/P95/P99 ms | 286.154 /671.173 /1157.064 | 256.992 /568.947 /1051.577 |
| solve P50/P95/P99 ms | 123.426 /163.868 /185.814 | 125.469 /165.711 /177.726 |
| root peak charged MiB | 161.418 | 164.481 |
| age /receiver /geometry cumulative | 160 /50 /26 | 208 /65 /0 |

实际仍256 enabled源、16 LIVE楼板、12000环境调用，无WORK_LIMITED/数值worker失败；两条路径均未通过。
P95改善约20%，P99仍超过1秒，冷启动与批量事件仍超龄。不能用结构命中6–8/9块宣布可支持百人。
复用与拆分主要节省构建；求解P95没有改善，不推广为默认，只保留显式试验供区域实现复用。

## 验证与下一交付

缓存/halo/邻接表的coupled实际五项生产生命周期5/5通过。
真实房间105个同sampleTick/同气候点最大rise差2.789e-13°C、floor0、66/66余热样本下降；
该单案例说明保留路径接近舍入差异，不是密集全场误差认证。
lockstep只认证这些生命周期和曲线，性能结论来自realtime20。
没有JUnit、独立热矩阵状态或温度注入；源码后端总开关仍false，普通材料仍coupled；性能对照选择local试验。
预算峰值是统一计费，而不是JVM真实堆；two_bases256MiB档位不证明默认128MiB通过。

结果：`build/local-preparation-results-20261006.json`；曲线：`build/basis-cached-room-curves-20261006.json`和`build/basis-compact-curves-20261006.json`。
最终默认cached/scalar单基地128的2400tick实时启停通过，age/geometry/receiver0，root93.446MiB。
完整P50/P95/P99=247.540/550.064/877.660ms；上一版232.897/557.634/800.913ms，不能宣称整体吞吐跃升或尾延迟稳定下降。
96个同sampleTick/同自然温度观测最大Air差0.0049488°C，仍是局部观测不是全场界限。
仍需持久热区域/稳定局部地址、变化区域投影、独立发布、正确事件年龄和保存/合并分裂时间协议。
目标仍完整P95<=500/P99<=800ms及所有点40tick可读；100实际客户端的人体/AI/网络/MSPT/TPS尚未认证。
