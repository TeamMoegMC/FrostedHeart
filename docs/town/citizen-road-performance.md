# 城镇居民道路性能基线（2026-11-13）

- Status: `Current`，仅对下述环境与负载有效。
- Last verified: `2026-11-13 UTC`
- Scope: `真实玩家城镇居民的共享道路导航、客户端站位、两种批量渲染及内存分配；未修改生产算法`
- Code anchors: `TownRoadPerformance`、`TownRoadClientTools`、`TownRoadNavigation`、`TownRoadNetwork`、`CitizenRoadPresentation`、`RoadPresentation`、`CitizenRenderMetrics`
- Tested production revision: `86eedc23a`；采样与负载刺激代码仅在 `src/gametest/.../devtools/`，正式 JAR 不包含。

## 结果与环境

服务端常规 tick 耗时尚可控，但千人客户端目前不能称为流畅；碰撞/站位查询的临时分配及 GC 是明确的后续优化方向。
环境为单逻辑 CPU（虚拟 AMD Ryzen 9 5900X）、Java 17.0.21.0.3、最大堆 1,854,930,944 B（约 1.73 GiB），
Mesa llvmpipe 软件光栅化，无真实 GPU 加速。1280×720，视距 6，VSync 关闭，帧率上限 260。
客户端与集成服务端共享同一进程/单核资源。以下 FPS 不能外推为玩家独显上的 FPS。

全部阶段相机固定在 `(19,88,39)`，yaw `174.61°`，pitch `39.73°`；已从每阶段 begin/end 的原始状态检查一致。
五座真实建筑形成四建筑工作组件、三条代表道路和一个隔离建筑；包含台阶与梯子。保持白天，不开 J 高亮。
通过真实 `town.addResident` 创建 100/1000 名托管居民，保留服务端模拟、导航、AOI 和网络；仅在招募时放宽测试住房容量并设为 20°C，
这不是客户端假人群，也不代表正常住房平衡。所有人位于紧凑道路上，模型大量重叠，不能据此声称均匀分布的 1000 人可见验收。

## 固定视角采样

每阶段先预热 200 客户端 tick，再采样 400 客户端 tick；实际每阶段约 20–23 秒，原始 JSON/CSV 包含准确 wallSeconds、tick/帧样本数。
静止模式固定 IDLE，移动模式在居民完成一次行程后重新进入 WANDER；每步仍执行生产道路导航，而非重写位置。
下表“位移人数”按每个服务端 tick 的真实三轴位置变化统计，包含垂直移动和建筑入口转接；不是仅检查 MOVING 状态。

| 场景 | 平均真实位移人数/tick | 平均 FPS | 服务端 tick p95 ms | 客户端 tick p95 ms | 完整帧间隔 p95 ms | 最长帧 ms | GC 累计 ms |
|---|---:|---:|---:|---:|---:|---:|---:|
| 0 人基线 / Flywheel | 0.0 | 23.8 | 5.44 | 4.63 | 72.50 | 100 | 0 |
| 100 人持续移动 / Flywheel | 91.9 | 23.2 | 5.22 | 4.32 | 70.00 | 1544 | 1444 |
| 1000 人静止 / Flywheel | 0.4 | 14.2 | 4.99 | 6.67 | 103.38 | 1319 | 2461 |
| 1000 人持续移动 / Flywheel | 929.0 | 13.8 | 5.97 | 6.35 | 97.47 | 1378 | 3724 |
| 1000 人移动 / 关闭客户端道路表现 | 924.6 | 15.5 | 5.98 | 2.79 | 69.11 | 1383 | 1303 |
| 1000 人持续移动 / CPU 批量 | 920.0 | 12.3 | 5.05 | 5.57 | 77.10 | 1391 | 3783 |
| 1000 人移动 / Flywheel 重复 | 919.1 | 14.0 | 4.98 | 5.35 | 64.58 | 1362 | 3801 |

- 所有阶段平均观测 TPS 约 20，但曾出现约 1.3 秒停顿后追赶；这不意味着始终平稳。
- 1000 人静止阶段平均约 0.4 人/tick 有位置调整，因此是近似静止而非严格零位移。
- 关闭道路表现通过开发工具临时清空客户端 `roadTown`，停用整套道路表现采样、站位与相应 GPU 快照更新，服务端导航不变；它不是仅把侧移量设为零。
- 关闭道路表现后客户端 tick p95 为 2.79 ms，而开启后的两次千人 Flywheel 移动为 5.35–6.35 ms。整帧 p95 不单调，不能用单次 p95 宣称确定的 FPS 提升比例。
- 每条件一次测量，仅千人 Flywheel 移动重复一次；随机目的地、JIT、堆状态和 GC 不完全相同。平均 FPS 用帧间隔均值倒数计算，非游戏 HUD 的瞬时读数。
- 两种批量后端都有 1000 个缓存/Body 条目、0 个详细实体、7 个皮肤批次。Flywheel 的条目计数不是实际通过 GPU 遮挡的可见人数。
- `CitizenRenderMetrics` 中 Flywheel render() 只记录提交/统计阶段，不覆盖全部实例上传、shader 执行和驱动时间；CPU 的该项尾部 256 帧平均约 15.51 ms，同样不能直接当整帧或 GPU 耗时。
- ServerTick 从 Forge START 到 END 采样，排除测试工具强制状态的循环；真实位移统计发生在计时后。ClientTick 排除开发开关操作。完整帧间隔仍包含这些工具及所有模组的影响。

## JFR 分配与 GC

在千人移动 Flywheel 状态另录制 JFR；不把录制阶段混入上表。完整 64 帧调用栈解析后，去掉录制开头 2 秒、结尾 1 秒，
并排除各线程首个分配样本（其权重异常偏大）；剩余稳态窗口约 22.19 秒。分配权重是采样估计，不是精确对象数量。

- 稳态分配权重总计约 1.149 GB（约 51.8 MB/s）。按调用栈中首个 FrostedHeart/Chorda 方法归属：
  `RoadPresentation.surface` 约 47.8%，`RoadPresentation.clearBody` 约 25.3%，`TownRoadRenderer.nearRoad` 约 7.38%。
  前两项合计约 73.1%，涉及 VoxelShape、AABB、BlockPos、临时集合/迭代器等。
- Render thread 的 605 个 Java 执行样本中，334 个调用栈包含 `CitizenRoadPresentation`（55.2%，含下游）；
  Server thread 的 842 个样本中，518 个包含 `TownRoadNavigation`（61.5%，含下游）。这些不是独占 CPU 百分比，也不包含完整 native 软件光栅成本。
- JFR 确认 JVM 使用 DefNew/SerialOld；录制中两次 `Allocation Failure` 引发的 SerialOld 停顿分别约 1.281 s 和 1.283 s。
- 优先优化方向：共用道路点/局部碰撞结果，减少 `toAabbs`、AABB/BlockPos 和中间集合分配，降低站位重采样成本；随后再评估增大堆或更换收集器。
  本轮没有修改生产逻辑、GC 参数，也没有进行优化后的 A/B 对比，因此不声称某项修复已带来收益。

## 共享图微基准

这是合成线形图，每段 16 步；每规模预热两次、测构建八次，首次端到端路线一次、同一路线缓存读取 10000 次。
不读取游戏世界、不测 section 分类，也不代表同规模真实城镇的完整扫描耗时。

| 建筑节点数 | 图构建均值 ms | 首次端到端路线 ms | 缓存命中均值 μs | 航点数 |
|---|---:|---:|---:|---:|
| 32 | 1.24 | 6.44 | 1.91 | 497 |
| 128 | 2.80 | 1.96 | 1.31 | 2033 |
| 512 | 8.63 | 4.59 | 1.32 | 8177 |

32 节点的首次路径比 128 节点慢，包含首次代码/JIT 影响，不应解释成小图算法更慢。
五建筑实际场景从创建到观察到工作组件/道路就绪约 2.01 秒，包含建造、结构扫描、排队与轮询，不能换算成纯寻路 CPU 耗时。
大型真实城镇的 section 复制预算、长期路线缓存峰值与独立服务端吞吐尚未在本轮测量。

## 复现与产物

1. 从仓库根目录启动新开发客户端：`./gradlew --offline runClient -I Scripts/dev_client_bridge.init.gradle`，停在主菜单。
2. 执行 `python3 Scripts/run-citizen-road-performance.py`。它创建独立存档，固定设置和视角，运行矩阵与 JFR，最后恢复设置并关闭游戏。
3. 执行 `python3 Scripts/analyze-citizen-road-performance.py <结果目录>` 解析 JFR；需要 Java 17 的 `jfr` 工具。

版本化摘要：[JSON](validation/citizen-road-performance-2026-11-13.json)、[CSV](validation/citizen-road-performance-2026-11-13.csv)。
本轮本地完整命令/阶段结果为 `build/citizen-road-performance/commands.jsonl`、`results.json`、`profile-summary.json`；
JFR 为 `run/agent/perf/citizen-roads-profile.jfr`，千人截图为 `run/agent/screenshots/01794556094703908000-citizen-road-performance-1000.png`。
大体积生成文件不纳入版本化摘要。

恢复命令和 quit 均返回成功；客户端日志确认 07:48:15 UTC Stopping、07:48:22 All dimensions are saved，Gradle BUILD SUCCESSFUL。
生产源码没有改动，本轮只增加开发采样工具、复现脚本和性能记录；未执行新的功能 GameTest，开发工具编译和实际完整采样成功。
