# 热模拟验证报告

两份报告保留各自的计量口径、结果与适用版本。结构化回执按原文件名索引。 [验证数据](thermal-validation-data-2026-11-19-to-20.json)

## 目录

- [百人热模拟的启动期延迟](#thermal-service-delay-2026-11-19)
- [材料两层接触与边界扫描验证](#thermal-material-frontier-2026-11-20)

<a name="thermal-service-delay-2026-11-19"></a>

## 百人热模拟的启动期延迟

- 状态　已复现预算延期如何影响查询，并核对前 1,400 tick 的四组实服数据。每组为一次独立 JVM 运行。
- 核验日期　2026-11-19，Asia/Shanghai
- 范围　100 人、25 个基地、1,600 个营火的混合负载。本文的及时读数指成功读到采样年龄 ≤40 tick 的驻地空气温度。
- 代码锚点　`ThermalWorkerPool.workerLoop`、`chargeService`、`serviceReadyAt`、`TiledThermalField.advance`、`ThermalNetworkLoadServer.sample`、`PageSignatures.copyBricksTo`。

### 是什么把更新推迟了

该测试存档把 `continuousAirCpuBudgetMsPerSecond` 设为 250 ms/s。源码默认值为 1,000 ms/s。所有热工作线程共用一笔额度。每项区域任务从进入 `run()` 到返回的经过时间都计入已用额，排队发生在计量区间之外。每经过 1 ms，已用额减少 0.25 ms，最低为零。普通任务入场时，若已用额高于 250 ms，线程就将它推迟到差额还清。已入场的任务可以完成，控制任务可以越过入场检查。

历史 H3 全程记录在测量第 600–1,000 tick 捕获到一笔 **126.62 ms** 的延期请求。池当时的已用额是 281.65 ms，超出的 31.65 ms 按每毫秒偿还 0.25 ms 计算，正好需要约 126.6 ms。在这次请求前约 0.88 ms，JVM 刚结束一次约 41.2 ms 的垃圾回收暂停。原记录能给出这两个事件的时间关系，下面的控制实验单独检验暂停位置的因果作用。

该段 H2 和 H3 各有 1,600 次驻地查询，及时数分别为 1,536 和 792。延期请求区间的并集覆盖这段实际时间的 0.016% 和 40.68%。在第 600 tick 的独立固定探针中，基地 1 的第 0 号点，查询时刻均为世界 tick 11,683。H2 最新温度算到了 11,660，年龄 23。H3 算到了 11,640，年龄 43。少完成一步 20 tick 的更新，就从及时变成了过期。

### 哪些工作用掉了额度

在放宽额度的测试服上，逐笔记录实际完成的普通任务，再按原额度核算同一批任务。第 100–200 测量 tick 是两种读法按 100 tick 边界统计时，温场推进超过原额度最多的区间。只统计完整落在窗口内的 `TiledThermalField.advance`，剔除控制任务和出现过几何推进、几何准备或页投影的整笔任务，再从每笔的推进耗时中扣掉该任务与 JVM 垃圾回收重叠的全部时长。

| 同一工作区的页读取方式 | 区间实际经过 | 原额度最高可用量（含起始 250 ms） | 仅上述温场推进 | 超出量 |
|---|---:|---:|---:|---:|
| 按变更 Brick 读取 | 4.769 s | 1.442 s | 1.833 s | 0.390 s |
| 整页逐格读取 | 4.454 s | 1.364 s | 1.918 s | 0.554 s |

这些都是调度器计账口径下的任务经过时间。只计最后一笔普通推进开始前至少 1 ms 就已完成的工作，按 Brick 读取时仍有 1.829 s，对应时刻最多可用 1.438 s。这排除了仅因窗口末尾已有任务获准入场而产生的表面超额。按 Brick 读取时，这个窗口内的整项页投影累计只用 0.938 ms。温场推进本身就足以让后续工作排队。

### 在真实服务器上改变额度

所有短测复用已编译的 H3。整页对照仅在进程中把页投影替换为 4,096 次 `PageSignatures.get`。四组均在世界 tick 11,063 开始，距点火完成 200 tick。每组 100 个客户端完成连接并保持到停止，1,400 个测量 tick 内有 200 次传送、1,555 次改墙，TPS 在 19.999 到 20.010 之间。25 个基地在七个 200-tick 统计窗口的逐基地尝试数全部相同，每组共 6,000 次。4,000 ms/s 仅用于诊断。

| 页读取方式 | 共享额度 ms/s | 及时 | 过期 | 缺失 | 预算延期请求 |
|---|---:|---:|---:|---:|---:|
| 按变更 Brick 读取 | 250 | 4,144 | 942 | 914 | 3,819 |
| 整页逐格读取 | 250 | 4,632 | 626 | 742 | 3,090 |
| 按变更 Brick 读取 | 4,000 | 5,478 | 57 | 465 | 0 |
| 整页逐格读取 | 4,000 | 5,511 | 63 | 426 | 0 |

在原额度下，按 Brick 读取比整页读取少 488 次及时读数。放宽后差 33 次。按 Brick 读取时余下的 57 次过期全部发生在第 160 tick，当时的固定探针也有 57 个位置的采样年龄为 43 tick。整页组的 63 次过期分别发生在第 180 和第 200 tick。各窗口内的事务、加载和发布时间取决于各自 JVM 的实际运行次序。历史全程对照起始世界 tick 为 11,083，1,452 次及时读数的历史净差仍只有逐窗口和逐探针记录，逐个查询的任务执行次序未被记录。

### 同一批任务的停顿对照

独立 JVM 中使用实际 `ThermalWorkerPool` 和 `TiledThermalField`。固定初始已用额 247 ms、热输入、入队顺序和读数时刻，仅将同一次约 40 ms 的人为停顿从任务入场前移到执行中。放在入场前时，两种读法合计 8/8 次在约 80 ms 的读点读到新值。放在执行中时，8/8 次读到旧值，下一项温度计算的开工时刻中位数从放行后约 41 ms 变为 153–158 ms。初始已用额为零时，两种停顿位置都能按时读到新值。连同其余控制共 19 次，最终 64 格逐项一致。

### CPU 记录的解释与复核入口

历史 H2 到 H3 的热工作线程 Java CPU 读数下降了 15.9%。在本次 250 ms/s 短测中，整页到按 Brick 读取的投影耗时从 77.78 ms 降至 16.82 ms，温场推进累计用时从 8.466 s 增至 8.941 s。Java 工作线程 CPU 的读数从 1,593.750 ms 降至 984.375 ms，而在任务内独立读取的原生线程周期从 32.66 增至 34.23 十亿。整场任务时序不同，周期按原单位报告。此前同机固定工作验证也复现了 Windows 对短时、间歇运行任务的 CPU 时间少计。15.9% 只量化了该计数器的差额。[固定工作计数器验证](thermal-cpu-counter-diagnosis-2026-10-28.json)

本机 `build/thermal-ab-20261118/early-service-trace/` 四个运行目录保留 `trial.json`、`metrics.jsonl`、`service-tasks.csv`、`service-frames.csv`、`server-profile.jfr`、`service-analysis.json` 与 `demand.json`。互斥阶段按进程内观测器测量，含探针开销。`demand.json` 的 `fieldAdvanceLessAllGcMs` 包含同时参与几何的任务，本文表格另行剔除了这些任务。四组任务记录均无丢弃或未闭合阶段，JFR 未报告数据丢失。预算延期请求的覆盖时间按区间并集计算。`causal-service-pause/REPORT.md` 与 `results.json` 保存受控停顿的逐次记录。历史长测的固定点及 JFR 区间在 `H3/freshness-detail.json` 和 `cpu-diagnosis/H3-events.tsv`。

<a name="thermal-material-frontier-2026-11-20"></a>

## 材料两层接触与边界扫描验证

- Status: `Historical; the user subsequently removed the two-layer rule. Measurements below apply only to the earlier bounded implementation, not the current runtime.`
- Last verified: `2026-11-20 02:40:29 +08:00`
- Scope: 材料两层接触、篝火六向驻留、几何失效、同构建边界扫描候选。
- Code anchors: `MaterialContactPolicy`, `TiledThermalField.replaceMaterialContacts/conductance`, `WorkerPageStore.collectMaterialFrontier`, `TopologyUpdatePlanner.collectLayoutDependencies`, `AirRouteCompiler.includeMaterialContactDependencies`, `MaterialFrontierGameTests`, `MaterialFrontierBenchmarkGameTests`。
- Author: `OpenAI Codex`
- Data: [逐项测量与测试记录](thermal-validation-data-2026-11-19-to-20.json#/records/thermal-material-frontier-2026-11-20)。本记录只描述本次版本；当前行为见[运行时说明](../thermal-runtime-control.md)与[供热规则](../heat-production-and-network.md)。

后续变更：用户已要求取消“第三层不传热”。生产层数策略、接触掩码及仅用于该规则的比较夹具已移除；`MaterialFrontierGameTests` 已改为验证第三层及更深传热和通用边界扩张。下文保留原测量与结论的历史语境，原复现命令不再代表当前用例集合。

### 结论和选择

按用户要求保留实体深度两层限制，同时保持 1 K 进入、0.5 K 保留。篝火本体作为表层，六面邻接实体可成为第二层；第二层不能接力进入第三层，也不能桥接两个表层。真实材料接触仍受 `contactFraction` 约束。取消上一轮篝火六邻 Brick 的强制 source seed，改由带温差门槛的材料边界请求接管。

数值换热采用三字接触许可缓存，每 Brick 24 字节数组载荷；每 Tile 另有数组头及引用开销。材料边界仍直接读取最终 Accepted 温度，在通过门槛后检查接触。额外边界候选缓存和整块扫描摘要只保留在测试中，不增加生产策略开关或第二份温度权威。

这里没有证明“两层规则本身值得作为性能限制”。它限制墙体深度，但不限制空气与沿表面的全部扩张。取消深度限制也不等于必然无限扩张：温差门槛会阻止不够热的边界；持续输入而缺乏散热时仍可能继续增长。两层规则有模型成本，包括第三层断热、第二层唯一归属以及几何更新依赖。是否保留这条玩法约束，应与实现同一规则时的性能选择分开判断。

### 方法

Windows / Java 17 / Forge，同一构建中的三个扫描实现使用同一份几何与最终 Accepted H。场景为 16×8×16 全石环境，32 个 Brick、2,048 个驻留格、16 个未点燃篝火本体，控制温度为 0.25、0.75、50°C，自然背景为 0°C；进入与保留门槛分别取 1、0.5 K。每个候选先运行四轮预热，再运行九轮，轮换执行顺序并消费校验值。三种边界结果和两种接触许可结果必须一致。

边界微基准包含材料活跃判定和材料接触候选，不含空气边界、世界捕获、队列调度或生产目标请求的去重。借用数组和反射准备不计入内核计时。合并候选从最终 Accepted 读取，不依赖还会被后续面交换及后半输入改变的中间态。当前场景没有用于测量的相变；自然相变的正确性由独立回归验证。

计量分别记录 `ThreadMXBean` CPU 纳秒、墙钟、线程分配和 Windows `QueryThreadCycleTime`。CPU 纳秒中位数在这些时长下多为零，不作为零开销或速度依据。线程周期始终以 cycles 表示，没有换算成 CPU 秒。主机存在并行活动，墙钟不可当作独占 CPU 时间。

### 最终同构建测量

表中都是每次处理该 2,048 格场景的中位数，墙钟单位 μs，周期单位 cycles；不是每格时间，也不是每服务器 tick 的额外开销。

| 操作 | 直接判定 cycles | 缓存 cycles | 合并扫描 cycles |
|---|---:|---:|---:|
| 材料边界，0.25°C / 1 K 门槛 | 29,920 | 31,281 | 32,541 |
| 材料边界，0.75°C / 0.5 K 门槛 | 54,389 | 38,189 | 39,969 |
| 材料边界，50°C / 1 K 门槛 | 51,375 | 36,809 | 38,798 |
| 材料面对接触许可 | 372,462 | 160,622 | 不适用 |

接触许可缓存比相同语义的直接判定少约 56.9% 的周期。两种接触测试共同执行了地址与签名查询，因此这是该检查内核的对照，不能解释为整个求解器或服务器提速 56.9%。额外边界缓存在热场景少约 28.4%，但冷场景多约 4.5%；合并扫描没有稳定胜过分开的缓存扫描。三个边界微基准和许可重建均未观察到测量区间内的每次堆分配。

| 操作 | cycles | 墙钟 μs | 每次分配 bytes |
|---|---:|---:|---:|
| 正式接触掩码重建 | 355,802 | 154.872 | 0 |
| 测试候选边界缓存重建 | 44,800 | 19.400 | 0 |
| 正式求解 + 驻留维护 | 688,100 | 297.475 | 8,096 |
| 正式方块替换几何提交 | 4,080,426 | 1,817.750 | 93,880 |

正式求解与几何提交是当前实现的总成本观察，没有取消深度限制的匹配对照；不能把这些值全部归因于两层限制。额外边界缓存原型还需 48 bytes/Brick 的候选数组载荷。这次不为数微秒的场景收益增加这份生产状态和更新合同。该结论仅支持当前较简单的选择，不证明所有建筑分布的最优性。

### 正确性和复现

最终 `build/material-frontier-validation/final-routes.log`：28 项必需 GameTest 全部通过，Gradle 成功。覆盖真实篝火六面升温、跨 Chunk/Section、铲灭和余温；驻留第三层不接力；两表层之间唯一第二层归属；1/0.5 K 正负温差迟滞；自然相变平台不扩张；材料状态替换、存档和收缩；通风与路由变化。

首个角点失效用例实际失败于 `invalidation.log`：打开一个空气格改变竞争表层后，三个 Section 轴外的旧主人接触位仍为真。补充八角依赖后通过。最终还把半砖通路放在 Y/Z Section 边界，开关远端空气入口，并逐面比较所有已安装缓存与新鲜策略求值。路由发现完成时单次补齐面邻/棱邻，避免沿每个发现切片重复扩大。

复现使用 Java 17：

```powershell
.\gradlew.bat --offline --no-daemon --max-workers=2 --console=plain `
  '-Dnet.minecraftforge.gradle.check.certs=false' `
  runGameTestServer `
  '-PgameTestNamespaces=frostedheart_material_frontier,frostedheart_material_frontier_benchmark,frostedheart_tiled_residency,frostedheart_campfire_heat,frostedheart_tiled_persistence,frostedheart_tiled_ventilation,frostedheart_material_runtime' `
  '-PgameTestDirectory=build/material-frontier-validation/server'
```

本次没有复制工作树版本做 A/B，也没有路径敏感哈希要求。没有进行百人完整生产资格验证、各种建筑分布的长时扩张测试，或取消两层规则后的端到端资源增长对照。
