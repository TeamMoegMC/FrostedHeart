# 热力系统核心实现与文档审阅（第二轮）

- Time: `2026-10-18 19:23:47 -0500`
- Author: `Claude (Anthropic Opus 5), 代码审阅`
- Related: `src/main/java/com/teammoeg/frostedheart/content/climate/thermal/`, `docs/climate/`
- Previous: [`2026-09-20_02-05-54_thermal-core-implementation-review.md`](2026-09-20_02-05-54_thermal-core-implementation-review.md)
- Reviewed at: `3ad011603`（thermal 包最后一次代码改动为 `3ef1135f9`, 2026-09-21）

本文是非权威审阅意见。结论基于阅读源码与文档，**未执行构建、未运行 GameTest、未做性能测量**。
凡属推测的地方均已标注。

---

## 1. 审阅范围与前提

| 对象                         | 规模                     |
|----------------------------|------------------------|
| `content/climate/thermal/` | 98 个 Java 文件，28,140 行  |
| `docs/climate/`            | 7 篇文档                  |
| `src/gametest/.../thermal/`| 17 个文件                 |
| `src/test/.../thermal/`    | 10 个 JUnit 文件          |

**一个重要前提**：`thermal/` 包的最后一次代码改动是 `3ef1135f9`（2026-09-21 00:47），
而上一轮审阅的时间是 2026-09-20 02:05。该提交同时触及
`TopologyUpdatePlanner` / `TopologyCommitter` / `PreparedTopologyChange` /
`ThermalDimensionEngine`，与上一轮 P0-1、P1-2、P1-3 的修复建议精确对应——
**它就是对上一轮审阅的响应**。此后近一个月（至 2026-10-18）本包无代码改动。

因此本轮的工作分两部分：**(A) 逐条核验上一轮结论的当前状态**，
**(B) 对上一轮覆盖较浅的连续空气后端（`b62378a09` / `3ef1135f9` 引入）做新的逐行审阅**。

阅读方式：`solver/`（含 `ContinuousAirSolver`、`CoupledThermalOperator`、`AirOperatorFragment`、
`PcgSolver`、`MaterialEnthalpyExchange`）、`mesh/AirFieldLayout`、`topology/AirFieldCompiler`、
`query/QueryPublication` 的并发窗口逐行；`topology/TopologyUpdatePlanner`、
`source/` 的事件时间链路按关键路径抽读；文档抽查代码锚点。

---

## 2. 上一轮结论的当前状态

### 2.1 已修复（核验通过）

| 编号 | 问题 | 当前状态 |
|----|----|----|
| **P0-1** | Page 替换导致 arena cell 永久泄漏 | **已修复** |
| **P1-2** | 生产路径 INFO 级 `AIR_TRACE` 日志 | **已修复**（已删除） |
| **P1-3** | 回滚把旧 Page 的 publication 发布到新 handle | **主要部分已修复** |

**P0-1 的修复**在 [TopologyUpdatePlanner.java:586](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyUpdatePlanner.java#L586)：

```java
long remaining = retired.residentBrickMask & (draft.retirement ? -1L : ~draft.cellReplacementMask);
```

修复思路与上一轮建议的"按位或保留旧掩码"不同，但**更干净**：不去扩大 `topologyDirtyMask`
（那会为旧驻留集凭空编译出新 cell），而是把旧驻留集拆成两类——落在 `cellReplacementMask`
内的走下方 migration，其余直接 `collectOldSpan` + 回收 phase slot。
两类的并集正好是 `previous.residentBrickMask`，泄漏闭合。随附注释
"Old-only Bricks retire directly, without manufacturing new cells or treating residency loss
as a world block edit" 也把这个取舍写清楚了。**这是比我上一轮建议更好的修法。**

**P1-3**：`rollbackPublication` 已简化为 `page.publication`，
`restorePagePublications` 现在让被替换页恢复**自己的** publication
（[TopologyCommitter.java:158](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyCommitter.java#L158)），
"新 handle 拿到旧 lifecycle publication" 的问题消失。

但**次序问题仍在**：`releaseOldSpans` 在 `ThermalDimensionEngine:202` 执行，
`restorePagePublications` 在 `:262` 的 catch 里执行，所以被恢复的 publication 仍可能指向
已释放的 slot。方法上的注释已明确声明"不回滚已安装的 solver，失败的 engine 由其所有者关闭重启"，
算是**已知并已记录**；读者安全依赖 generation 校验拒读。可以接受，但见 §3.2——
ENGINE_FAILED 之后 `pages.checkpointAll(true)` 会读 publication，这条链路值得单独确认一次。

### 2.2 仍然存在（逐条核验，全部未变）

| 编号 | 问题 | 核验结果 |
|----|----|----|
| **P1-1** | `FarFieldSettings` 三参数只有一个自由度，`continuationDistanceBlocks` 是死参数 | 全仓库仍只有 [BrickTopologyCompiler.java:353](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickTopologyCompiler.java#L353) 一个调用点，参数仍是常量 `(16, true)` |
| **P2-1** | 空气按 SI、凝聚相压缩约 400 倍，文档未说明 | 未见 `thermal-calibration.md`；标度表仍缺 |
| **P2-2** | 每方块一 cell 与 `maximumLiveCells = 65_536` 的张力 | 限额未变；`retryWorkLimited` 仍**无计数、无 WARN、无上限信号**（[MinecraftPageManager.java:259](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPageManager.java#L259)） |
| **P2-3** | `sleepResidualC = 1.0e-6` 使休眠路径实践中不可达 | 仍是 `1.0e-6D`（[MinecraftThermalInput.java:207](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L207)） |
| **P2-4** | 128 MiB 预算只覆盖查询与辐射 | 仍只有 4 个文件调用 `tryReserve`；arena / solver / `ContinuousAirSolver.rebuild` 的向量仍在预算外 |
| **P3-c** | `MinecraftPhaseController.MATERIAL_SAMPLE` 静态可变 scratch | 仍是 `private static final MaterialSample`，同文件另有两处 `ThreadLocal`，不一致依旧 |
| ~~**P3-e**~~ | ~~许可证头~~ | **已在本文写作期间修复**,见 2.4 |
| **P3-f** | 包内注释密度 | 仍是 **0 个** `package-info.java`，98 个文件中 **14 个**含中文 Javadoc |

### 2.3 需要修正的上一轮结论

**(1) P3-g（`selectBranch` 在 `direction == 0` 时返回 `COOLING`）——可达性为零，不是"定时炸弹"。**

我这轮把全部 5 个调用点都查了：

| 调用点 | 守卫 |
|----|----|
| `ThermalCellArena:399` ← `MaterialEnthalpyExchange:24-25` | `if (delta == 0) return;` 在 `Math.signum(delta)` 之前 |
| `ContinuousAirSolver:383` (`applyImpulse`) | 循环条件 `remaining != 0` |
| `ContinuousAirSolver:437` | `if (rateW != 0 && ...)` |
| `ContinuousAirSolver:460` | `energyJ == 0 ? candidateBranches[body] : ...` |
| `DormantThermalCooling:57` | `if (delta == 0) break;` 在 `Math.signum(delta)` 之前 |

**全部 5 处都显式排除了零**。这降级为纯粹的防御性加固建议（在 `selectBranch` 入口加
`if (direction == 0) return branch;`），不构成现实风险。上一轮"定时炸弹"的措辞过重。

**(2) P3-h（`ContinuousAirSolver.solveCut` 死循环）——后果判断错误，不是死循环。**

详见 §3.3。实际后果是**时间回退与区间重复积分**，循环本身可证终止。

**(3) "JUnit 层已被整体删除"——已过时。**

`src/test/` 现有 174 个文件，其中 `content/climate/thermal/` 下有 10 个：
`ThermalExchangeKernelTest`、`BuoyancyConductanceTest`、`RadiationServiceTest`、
`RadiantEquivalentTemperatureTest`、`SphericalHeatFieldModelTest`、`ThermalMemoryBudgetTest`、
`TownThermalProjectionTest`、`DormantChunkThermalStateTest` 等。
上一轮点名"最该有单测"的 `ThermalExchangeKernel` 实际是**有**覆盖的。

但点名的另外几个**确实仍无针对性覆盖**：`MaterialThermalLaw` 的分支选择、
`MaterialEnthalpyExchange` 的解析积分、`DormantThermalCooling` 的段式积分、
`ThermalFreeSpanIndex` 的 AVL best-fit。连续空气后端
（`PcgSolver` / `CoupledThermalOperator` / `AirOperatorFragment`）整体没有 JUnit 层，
只有 GameTest（`ContinuousAirProductionGameTests`、`ContinuousAirPhaseProductionGameTests`）。

### 2.4 写作期间被修复：P3-e 许可证头

本文初稿完成后、提交之前，`origin/master` 上出现了 `d363a509b`（LICENSE）与
`063fa3519`（more license，作者 `khjxiaogu`，2026-10-18），为全仓库补齐 GPL-3 头。
重新核验：`thermal/` 包 98 个文件中**缺 GPL 通知的已降为 0**。上一轮 P3-e 关闭。

遗留一个小瑕疵：其中 **79 个**文件现在**同时**带着新的 GPL 注释块和原先的
`/* Copyright (c) 2026 TeamMoeg */` 单行头，即重复声明。不影响合规，
但下次跑 `./gradlew licenseFormat` 时值得顺手把单行头去掉。
另外 `build.gradle` 的 `ignoreFailures = true` 仍在，CI 依旧不会在头缺失时报错——
这次是人工补的，不是 CI 发现的。

---

## 3. 新发现的问题

以下集中在连续空气后端与跨线程发布路径——上一轮覆盖较浅、且是最近两次提交引入的部分。

### P1-N1 `stageAir` 的数组换引用在 seqlock 写窗口之外（内存可见性）

**位置** [QueryPublication.java:108](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/query/QueryPublication.java#L108) `stageAir`

对照同一个类里**写对了**的 `tryEnsureCapacity`（:152）：它把新数组建成局部变量、拷完内容，
然后 `beginWrite()` → 换字段引用 → `endWrite()`，**引用交换在写窗口内**，
由 `endWrite()` 对 volatile `publicationVersion` 的写提供 release。

`stageAir` 的扩容路径没有这么做：

```java
var reservation = budget.tryReserve(16L * capacity + 64);
if (reservation == null) return false;
double[][] replacement = {Arrays.copyOf(airCoefficientsC[0], capacity), Arrays.copyOf(airCoefficientsC[1], capacity)};
var previous = airReservation;
airCoefficientsC = replacement;          // ← 普通字段写，无 beginWrite/endWrite
airReservation = reservation;
if (previous != null) previous.close();
```

方法是 `synchronized`，但读者 `tryReadAir` **不加锁**，monitor 的 release 与读者的
`publicationVersion` acquire **不配对**。

**后果链条**

读者 `tryReadAir`（:129）的序列是：读 volatile `version` → 读
`airCoefficientsC[buffer]` → 算温度 → 复查 `version == publicationVersion`。
在 `stageAir` 换引用之后、后续 `publish()` 的 `endWrite()` 之前，读者可以：

1. 读到**旧的**（未变）`publicationVersion`；
2. 读到**新的** `airCoefficientsC` 引用，而 `Arrays.copyOf` 写入的元素尚不可见 → 读到 `0.0`；
3. 复查 version 通过（**扩容不 bump version**）；
4. `out.set(temperatureC, tick)` 并 `return true`——**把退化值当作有效读数返回**。

此时返回的温度是 `airReferenceC[buffer] + Σ(权重 × 0.0)`，即恰好等于参考温度。
这不会崩溃，也不会被任何校验拒掉，表现为"空气温度短暂塌回参考值"。

**窗口大小不是几条指令**：`stageAir` 之后紧跟的是 `publish()`
（[ThermalDimensionEngine.java:326-339](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/engine/ThermalDimensionEngine.java#L326)），
而 `publish` 要遍历全部 live arena cell 才进入写窗口。整个遍历期间窗口都是开的。

**可达性**（推测，未构造复现）：需要 (a) 扩容路径触发，即空气系数数变大；
(b) 弱内存序平台。x86 的 TSO 下几乎不会显现，**ARM（Apple Silicon、以及大量移动/ARM 服务器）
下是真实可能的**。考虑到开发机本身是 darwin/arm64，这条值得修。

**建议**：与 `tryEnsureCapacity` 对齐——把 `airCoefficientsC` / `airReservation` 的引用交换
移进 `beginWrite()` / `endWrite()`：

```java
double[][] replacement = {Arrays.copyOf(airCoefficientsC[0], capacity), Arrays.copyOf(airCoefficientsC[1], capacity)};
if (!beginWrite()) { reservation.close(); return false; }
var previous = airReservation;
airCoefficientsC = replacement;
airReservation = reservation;
endWrite();
if (previous != null) previous.close();
```

代价是多一次 version bump（读者重试一次），相对于正确性可以忽略。

### P1-N2 `ENGINE_FAILED` 无重启计数与熔断，且存在至少三个确定性失败源

**位置**
[MinecraftThermalInput.java:352-368](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L352)、
`restartWorker`（:383）

失败处理是：ERROR 日志 → `pages.checkpointAll(true)` → 关闭 worker → `restartWorker` →
`pages.reseedAll(accumulator)`。全文件只有**一处** `restartWorker` 调用、**一处**定义，
**没有任何重启计数器、退避或熔断**。

问题在于，连续空气后端里有若干 `IllegalStateException` 是**由几何/规模确定性决定**的，
重启后会原样复现：

| 失败源 | 位置 | 触发条件 |
|----|----|----|
| `"Continuous Air publication exceeded its query budget"` | `ThermalDimensionEngine:327` | `stageAir` 的 `budget.tryReserve` 失败——每维度查询预算固定 16 MiB，与 cell payload 共用 |
| `"Continuous Air topology projection did not converge"` | `ContinuousAirSolver.rebuild` | PCG 在 `max(64, min(2048, size*2))` 次内没到 1e-9，**此处无步长折半兜底** |
| `"Continuous Air could not advance its material segment"` | `advanceSeconds` | 步长折半至 `< 1e-10` 仍不被接受 |
| `"prepared query publication could not be installed"` | `ThermalDimensionEngine:340` | 同上预算链路 |

一旦某个基地的空气场规模稳定超过 16 MiB 预算，或某处几何让投影求解稳定不收敛，
就进入 **"失败 → 重启 → 重新 seed → 同样几何 → 再失败"** 的循环。
每轮都带一次 `checkpointAll(true)` 和一次全量 `reseedAll`，
**比单纯不工作更昂贵**，且除了反复刷 ERROR 日志外没有降级手段
（例如回落到标量后端、或标记该维度停用）。

对照：WORK_LIMITED 至少还有 200 tick 退避（虽然按 P2-2 也没有上限信号）。
ENGINE_FAILED 连退避都没有。

**建议**（按成本排序）：

1. 加连续失败计数；超过 N 次（比如 3）后 WARN/ERROR 一次"该维度热力运行时已停用"，
   并停止自动重启，而不是无限重试。这是最小改动，也最能把问题暴露给运维。
2. 把 §3.1 的预算失败与 §3.2 的不收敛失败**分类**：预算类失败应当降级
   （缩减 publication 覆盖面或回落标量后端），而不是等同于"引擎崩了"。
3. `rebuild` 的投影求解至少应有一次兜底（例如放宽容差重试一次再抛）。

### P2-N1 `PcgSolver` 的收敛判据是纯绝对量，与问题规模解耦

**位置** [PcgSolver.java:11](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/PcgSolver.java#L11)

```java
static final double CORRECTION_TOLERANCE_C = 1e-9;
...
private double maximumCorrection(int size) {
    double maximum = 0;
    for (int index = 0; index < size; index++) maximum = Math.max(maximum, Math.abs(preconditionedC[index]));
    return maximum;
}
```

判据是"预处理后残差的最大分量 ≤ 1e-9 °C"，**绝对、无相对项、不随 size 缩放**。
迭代上限是 `max(64, min(2048, size * 2))`。

在 `tryStep` 里这是安全的——求解失败会让 `advanceSeconds` 折半步长，
而更小的 `dt` 让 `mass/dt` 主导对角、条件数变好，所以折半确实有效。

在 `rebuild` 的投影求解里**没有这个兜底**（见 P1-N2），而投影用的是
`prepareProjection` 下的**完整一致质量矩阵**（`addProjectionMass`，非 lumped），
条件数明显劣于带 `mass/dt` 的瞬态算子。对于大空气场，
"2048 次迭代把每个分量压到 1e-9 °C" 是一个**没有依据的硬要求**——
它既不是物理精度需求（1e-9 °C 远低于任何可观察量），也没有随规模放宽。

另外 `CORRECTION_TOLERANCE_C` 同时被 `ContinuousAirSolver` 当作**能量**舍入尺度复用：

```java
double roundingJ = PcgSolver.CORRECTION_TOLERANCE_C * Math.max(1, law.capacityJPerK());
```

一个常量同时承担"温度收敛容差"和"焓舍入容差"两种量纲职责，
`Math.max(1, ...)` 里的 `1` 也没有单位说明。

**建议**：改成 `相对 + 绝对` 混合判据（例如 `tol = max(1e-9, 1e-12 * ‖rhs‖_∞ / 典型质量)`），
或者至少把投影求解的容差与瞬态求解分开命名、分别标定，并在
`continuous-air-runtime.md` 里写明这两个数字的依据。

### P2-N2 `solveCut` 对"早于 `fromTick` 的事件"无防线（修正上一轮 P3-h 的后果判断）

**位置** [ContinuousAirSolver.java:~348](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/ContinuousAirSolver.java)

```java
long tick = fromTick;
int event = 0;
while (tick <= toTick) {
    while (event < eventCount && eventTick(loads, loadEvents[event]) == tick) { ... }
    if (tick == toTick) break;
    long next = Math.min(toTick, tick + 20);
    if (event < eventCount) next = Math.min(next, eventTick(loads, loadEvents[event]));
    advanceSeconds((next - tick) / 20.0);
    tick = next;
}
```

内层只消费 `eventTick == tick` 的事件。若存在 `eventTick < fromTick` 的记录：
首轮它不被消费（`==` 不成立），`next` 被 `Math.min` 拉到该事件时刻 → **小于 `tick`** →
`advanceSeconds` 收到负值。

**`advanceSeconds(负值)` 直接返回**（`while (remaining > 1e-12)` 首判即假），
然后 `tick = next` **回退**。下一轮该事件因 `== tick` 成立而被消费，`event` 前进。
由于 `event` 严格单调，**循环可证终止——不是死循环**（上一轮此处判断有误）。

真实后果是：`tick` 回退后，`[staleTick, fromTick]` 这段区间被**重新推进一次**，
该区间内所有活跃 span 按其功率**重复积分**，`deliveredJ` 随之多计。
即"多交付了一段本不该存在的时间"，而不是挂起。

**上游是否真的保证不发生？** 记录由
[NodePowerAccumulatorArena.settleTo:169](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/source/NodePowerAccumulatorArena.java#L169) 发出：

```java
else recording.span(nodeIds[slot], lifecycleGenerations[slot], previousTick, targetTick, currentPowerW[slot]);
```

`previousTick = lastIntegralTicks[slot]` 是**节点自己的**上次积分时刻，
**没有被夹到本 cut 的 `fromTick`**。正常情况下：活跃节点每个 cut 都被
`drainAllPendingEnergyTo` 结算到 `targetTick`，所以下个 cut 开始时
`lastIntegralTicks == fromTick`；非活跃节点功率为 0，而 `CutLoadBuffer.span` 对
`watts == 0` 直接丢弃。所以**当前确实不会发生**。

但这个不变量（"CutLoadBuffer 中的记录起点不早于 `fromTick`"）
**没有在任何地方被断言**，而它依赖"功率非零 ⇒ 在活跃链表中"这个跨类的隐含约定
（`refreshActive` / `reclaimIfIdle` / slot 复用三者共同维持）。求解器自己毫无防线。

**建议**：在 `solveCut` 入口加一条廉价断言或夹取：

```java
// 记录起点早于 cut 起点会让时间回退并重复积分该区间
if (eventCount > 0 && eventTick(loads, loadEvents[0]) < fromTick) {
    throw new IllegalStateException("cut load precedes its window");
}
```

抛出比静默多计能量好——后者会表现为"热源偶尔多给了一点热"，几乎无法定位。

### P2-N3 `prepareProjection` 缺少 `prepareMass` 已有的零对角守卫

**位置** [CoupledThermalOperator.java](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/CoupledThermalOperator.java) `prepareProjection` / `prepareMass`

`prepareMass` 显式守卫了材料行的零对角：

```java
for (int index = airUnknownCount; index < size(); index++) if (diagonalJPerK[index] == 0) diagonalJPerK[index] = 1;
```

`prepareProjection` 对材料行只写 `diagonalJPerK[index] = massJPerK[index];`，**无守卫**；
对空气行写的是 `projectionMassJPerK` 的对角元，同样无守卫。
而 `precondition` 无条件做 `residualJ[index] / diagonalJPerK[index]`。

材料侧目前是安全的：`MaterialThermalLaw` 的紧凑构造器强制 `capacityJPerK > 0`，
`ThermalTopologyParameters` 强制空气热容 > 0，所以 coarse 行的 lumped 质量也为正。
**真正没有被任何不变量覆盖的是局部形函数（basis ≥ 8）行**：其对角来自
`Σ_gauss capacity · 0.125 · φ²`。若某个 shape 在其所属 component 的全部高斯点上都接近 0
（支撑几乎不与该 component 的实际空气块相交），对角会趋于 0，
`precondition` 产生 `±Inf/NaN`，`maximumCorrection` 变 NaN，
`curvature` 的 `!Double.isFinite` 分支让 `solve` 返回 false，
最终在 `rebuild` 里抛成 ENGINE_FAILED（接 P1-N2 的重启循环）。

`AirFieldLayout.withLocalShapes` 确实用 `intersects(component, shape)` 过滤了
"支撑完全不相交"的组合，所以**我没有构造出实际的零对角**——这是**推测而非结论**。
但两个 `prepare*` 对同一数组的守卫强度不一致，本身就是应该消除的不对称。

**建议**：把 `prepareMass` 的那行守卫推广到 `prepareProjection`，覆盖全部行；
或者在 `AirOperatorFragment` 构造末尾断言
`capacityJPerK[basis] > 0 for all basis`，把问题挡在编译期而不是求解期。

### P3 其它（新增）

| # | 问题 | 位置 |
|---|---|---|
| a | `ContinuousAirSolver.rebuild` 每次拓扑提交重新分配 8 个长度为 `size` 的 `double[]`/`byte[]`（`stateC`/`candidateC`/`rhsJ`/`powerW`/`contactRatesW`/`materialH`/`candidateH`/`branches`/`candidateBranches`）外加整个 `CoupledThermalOperator`。门狗/方块变更频繁的基地会持续产生大块垃圾，且全部在 `ThermalMemoryBudget` 之外（接 P2-4） | `solver/ContinuousAirSolver.java` `rebuild` |
| b | `CoupledThermalOperator` 的 `modePartners` 只配对**相邻两个** local shape；`AirShapeStore` 对每个出口生成 `scale ∈ {1, 3}` 两个 shape，所以当前恰好成对。但若将来 scale 集合变成 3 个，第三个会静默落单（退化为 scalar Jacobi 而非 2×2 Cholesky），没有任何断言会发现 | `solver/CoupledThermalOperator.java` 构造器 |
| c | `rebuild` 的高斯点循环里，`int brick = (component.minX & 15) >>> 2 \| ...` 只依赖 component 原点，却放在**每点 8 次 × 每块**的内层循环里重复计算 | `solver/ContinuousAirSolver.java` `rebuild` |
| d | `applyImpulse` 的材料分支把 3 段以外的剩余能量一律记入 `unacceptedJ`。按"至多一次相变"的法则结构 3 段是够的，但这个上界与 `MaterialEnthalpyExchange` 的 6、`DormantThermalCooling` 的 4 各不相同且都是裸字面量，没有一处注释说明为什么是这个数 | `ContinuousAirSolver:382`、`MaterialEnthalpyExchange:19`、`DormantThermalCooling:53` |
| e | `MinecraftThermalInput` 仍是 1,688 行的门面；`MinecraftPageManager` 1,607 行。上一轮 P3-d 未处理 | `runtime/minecraft/` |

---

## 4. 对文档的意见

### 4.1 本轮核验：代码锚点全部对得上

抽查了 `continuous-air-runtime.md` 的数值与标识符锚点，**无一漂移**：

| 文档断言 | 代码 | 结果 |
|---|---|---|
| `LocalAirShape.RADIUS = 6` | `LocalAirShape.java:8` | ✓ |
| "local shapes with scales 1 and 3" | `AirShapeStore.java:181,264` `new int[]{1, 3}` | ✓ |
| "`SourceBinding` reserves bit 31" | `SourceBinding.java:23` `1L << 31` | ✓ |
| "at most 20 ticks (1 game second)" | `solveCut` `tick + 20` | ✓ |
| `airHeatCapacityJPerBlockK` / `airMixingWPerBlockK` 为运行期配置 | `MinecraftThermalProfiles.java:54-55` 读 config | ✓ |

上一轮关于"锚点与代码漂移无法机器检测"的担忧依然成立（仍靠人工比对），
但**实际漂移为零**，说明作者在改代码时确实同步改了文档。这点值得肯定。

### 4.2 `Last verified` 漂移：上一轮指出的那一条不但没修，还又漂了一次

`thermal-runtime-architecture-and-optimization.md` 头部仍写
`Last verified: 2026-09-16`，但该文件此后被 `b62378a09`（09-20）和 `3ef1135f9`（09-21）
**两次修改**。上一轮已经点过这个问题，这轮它变得更明显。

相比之下 `continuous-air-runtime.md` 写 `2026-09-21`，与最后一次改动精确一致——
说明规范本身是可执行的，只是没有机制保证。

**建议**：加一个最轻量的 CI/pre-commit 检查——
若 `docs/climate/*.md` 在本次提交中被修改，则其 `Last verified` 必须等于提交日期。
十行脚本，能彻底消灭这一类漂移。

### 4.3 上一轮文档意见的状态

| 上一轮意见 | 状态 |
|---|---|
| (1) 密度过高缺层次 | 未处理 |
| (2) 只讲 what 不讲 why（Brick 为何 4³、为何不用隐式矩阵、`CUT_INTERVAL_TICKS` 为何 20） | 未处理 |
| (3) 缺数值标定表 `thermal-calibration.md` | **未创建**——这是 P1-1 与 P2-1 的共同根因，也是本轮 P2-N1（两个容差数字无依据）的根因 |
| (4) 一张图都没有 | 未处理 |
| (5) 锚点漂移无法机检 | 未处理（但实际无漂移，见 4.1） |
| (6) `Last verified` 漂移 | **恶化**（见 4.2） |
| (7) 红外渲染内容放错文档 | 未处理 |

其中 **(3) 标定表**的优先级在本轮进一步上升：
现在"无推导记录的精确数字"已经从 `farFieldConductanceWPerK = 7747.2298793470545`
扩大到 `CORRECTION_TOLERANCE_C = 1e-9`、`sleepResidualC = 1e-6`、
各处 segment 上限 `3 / 4 / 6`、`maximumLiveCells = 65_536`、
以及 `ThermalDimensionLimits` 与 `RadiationService.Parameters` 的两串裸字面量。

### 4.4 新增一条：`continuous-air-runtime.md` 的 Status 与失败模式不匹配

文档 Status 写 `Partial; production path implemented, lifecycle and capacity work still in progress`，
Outstanding limits 一节也诚实列了若干未完成项。但 §3 的 P1-N2 指出，
"capacity work still in progress" 的实际后果是
**预算耗尽 = `IllegalStateException` = 整个维度热力运行时进入无退避重启循环**。

这是面向运维最重要的一条信息，目前没有写在文档里。
建议在 Outstanding limits 里补一句：
"Exceeding the per-dimension query budget currently fails the worker rather than degrading;
the worker restarts without backoff, so a persistently oversized Air field will restart-loop."

---

## 5. 测试与验证

- GameTest 共 124 个 `@GameTest`，thermal 相关 17 个文件；
  `ContinuousAirPhaseProductionGameTests` 是 `3ef1135f9` 随修复一起加的（+71 行），
  **修 bug 同时补回归测试，这个习惯是对的**。
- JUnit 层并未被整体删除（见 2.3），`ThermalExchangeKernel` / `BuoyancyConductance` /
  `RadiationService` / `ThermalMemoryBudget` 都有覆盖。
- **连续空气后端是当前测试最薄的部分**：`PcgSolver`、`CoupledThermalOperator`、
  `AirOperatorFragment`、`AirFieldCompiler` 全部只有 GameTest 级别的端到端覆盖，
  没有一个纯数值单测。而这几个类恰好是：纯函数为主、无 Minecraft 依赖、
  出错最隐蔽（§3 的 P2-N1 / P2-N3 都落在这里）。

  具体建议几个成本很低、价值很高的 property-based 断言：

  1. **质量守恒**：对任意 component，`Σ_basis capacityJPerK[basis] == airCapacity × 空气块数`
     （`AirOperatorFragment` 的 lumped 质量应当精确等于总热容）。
  2. **算子对称正定**：对随机向量 `u, v`，`uᵀAv == vᵀAu`（`apply` 的对称性）
     且 `uᵀAu > 0`——这正是 `PcgSolver` 的 `curvature > 0` 前提。
  3. **投影幂等**：`rebuild` 的投影对一个已经是该基底精确表示的场应当是恒等的
     （warm-start 不变）。
  4. **对角非零**：`prepare` / `prepareProjection` / `prepareMass` 三者之后
     `diagonalJPerK` 全部 > 0——直接覆盖 P2-N3。
  5. **PCG 规模无关性**：对同一物理问题在 size 翻倍时迭代数不应爆炸——
     能把 P2-N1 的绝对容差问题量化出来。

- 性能证据仍为空白：`thermal-runtime-architecture-and-optimization.md` 自己列的
  120 秒 JFR workload 与 10/30 分钟 churn/heap run 仍是"待做"。
  在 `continuousAir` 从 opt-in 转默认之前，这仍然是硬门槛。

---

## 6. 建议的处理顺序

1. **P1-N1**（`stageAir` 换引用移进写窗口）。约 5 行，与同类中已写对的
   `tryEnsureCapacity` 对齐，是本轮唯一的正确性缺陷。
2. **P1-N2 第 1 点**（ENGINE_FAILED 连续失败计数 + 停用告警）。
   小改动，但能让 §3 里所有"抛异常"类问题在生产环境**可见**。
   和上一轮 P2-2 第 3 点（WORK_LIMITED 到达硬上限 WARN 一次）一起做，成本最低。
3. **P2-N2**（`solveCut` 入口断言）+ **P2-N3**（`prepareProjection` 补零对角守卫）。
   两条各一两行，都是把"目前靠上游约定维持"的不变量就地钉死。
4. **文档 (3) `thermal-calibration.md`** + **P1-1 `FarFieldSettings` 塌缩成一个参数**。
   现在它还额外是 P2-N1 的根因，优先级比上一轮更高。
5. **P2-N1**（PCG 容差改混合判据 / 投影与瞬态分开标定）。
   需要先有 §5 第 5 条的测量，不宜拍脑袋改。
6. **§5 的 5 条 property 测试**。连续空气后端目前的测试结构性缺口。
7. **文档 4.2 的 `Last verified` CI 检查**（十行脚本）+ **4.4 的一句补充**。
8. 上一轮遗留的 P2-3 / P2-4 / P3-c / P3-f、文档 (1)(2)(4)(7)。
   P3-e 已由 `d363a509b` / `063fa3519` 关闭（见 2.4），只剩 79 个文件的重复头待清理；
   但 `build.gradle` 的 `ignoreFailures = true` 仍让 CI 发现不了这一类问题。

---

## 7. 一句话总结

上一轮唯一的严重缺陷（P0-1 arena cell 泄漏）**已经用一个比我建议的更干净的方法修好了**，
修复还附带了回归测试，文档的代码锚点也无一漂移——**这套系统的工程纪律在持续兑现**。

本轮新找到的问题集中在最近引入的连续空气后端，其中真正的正确性缺陷只有一条
（`stageAir` 的发布可见性，且只在弱内存序平台显现）。
更值得关注的是一个**系统性**问题：连续空气后端把若干**由规模/几何确定性决定**的条件
（查询预算耗尽、投影不收敛）处理成 `IllegalStateException`，
而 `ENGINE_FAILED` 路径**没有计数、没有退避、没有降级**——
于是一个"该维度的空气场太大了"这样本应表现为性能退化的问题，
会表现为**无声的无限重启循环**。

风险判断与上一轮相同，但指向更明确了：**代码质量不是风险，容量边界才是**。
而且现在它有两个出口（65,536 live cell 的 `WORK_LIMITED` 永久重试、
16 MiB 查询预算的 `ENGINE_FAILED` 重启循环），两者都**不会在小规模测试里暴露**，
都**没有面向运维的信号**，也都**还没有被实测**。
在 `continuousAir` 转为默认之前，补齐这两条路径的可观测性比修任何单个 bug 都重要。
