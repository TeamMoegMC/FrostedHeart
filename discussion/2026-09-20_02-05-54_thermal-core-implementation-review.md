# 热力系统核心实现与文档审阅

- Time: `2026-09-20 02:05:54 +08:00`
- Author: `Claude (Anthropic Opus 5), 代码审阅`
- Related: `src/main/java/com/teammoeg/frostedheart/content/climate/thermal/`, `docs/climate/`

本文是非权威审阅意见。结论基于阅读源码与文档，未运行 GameTest、未做性能测量。
凡是"推测"的地方都已标注。

---

## 1. 审阅范围

| 对象                          | 规模                               |
|-----------------------------|----------------------------------|
| `content/climate/thermal/`  | 98 个 Java 文件，28,133 行            |
| `docs/climate/`             | 7 篇文档，2,784 行                    |
| `src/gametest/.../thermal/` | 16 个文件，6,342 行，119 个 `@GameTest` |

阅读方式：`mesh` / `solver` / `topology` / `source` / `query` 逐行；`runtime.minecraft`、
`persistence`、`radiation`、`profile` 按接口面与关键路径抽读；文档全文。

---

## 2. 总体评价

**这是一套设计水平明显高于一般 Minecraft mod 的子系统。** 值得肯定的地方：

1. **线程所有权是真的被贯彻的，不是口号。**
   主线程只产出不可变原始类型 cut（`ThermalInputBatch`），worker 独占 arena / topology /
   solver / ledger / publication，回程只有 `ThermalCompletion`。我没有在 worker 侧找到任何
   `BlockState`、`Level`、`LevelChunk` 的引用泄漏。`QueryPublication` 用 seqlock 双缓冲做
   跨线程读，`tryEnsureCapacity` 的数组换引用也正确地放在写窗口内——这一点很容易写错，这里
   写对了。

2. **拓扑变更是真正的事务。**
   `TopologyUpdatePlanner.prepare`（可失败、可丢弃）→ `PreparedTopologyChange`（不可变）→
   `TopologyCommitter.commit`（无分配、无排序、无可恢复分支）→ `releaseOldSpans`（先证明
   solver 与 source 都不再引用旧 span 才释放）。`preflightReplacement` 用引用计数增量提前
   证明闭包不再引用退休 span，这是我在同类代码里很少见到的严谨度。

3. **能量口径是守恒式而不是温度插值式。**
   `MaterialThermalLaw` 用 `H = offset + C·T` + 显式相变平台，`temperatureC` / `slopeKPerJ` /
   `energyLimitJ` / `selectBranch` 四件套把"潜热平台 = dT/dH 为 0"而不是"C = 0"表达得很干净。
   `MaterialEnthalpyExchange` 在每个线性段内做解析积分并反解耗时，而不是简单截断——这是正确
   做法。`DormantThermalCooling` 用同一套段式积分做休眠投影，两处公式一致（`-expm1` /
   `-log1p` 配对），我验算过，没有量纲错误。

4. **分级回退链条完整且不会说谎。**
   live publication → routed air → dormant checkpoint → `naturalAir`，每一级都带来源标记
   （`MaterialSample.Source`、`ThermalEnvironmentSample.airBasisTerms`），消费者能区分"真的
   算过"和"退化值"。

5. **文档质量在开源 mod 里属于第一梯队。**
   尤其是 `continuous-air-runtime.md` 结尾的 "Outstanding implementation limits" 和架构文档里
   反复出现的 "this is not a universal error bound" / "this is an explicit approximation" /
   "measured separately"——**主动标注自己没有证明什么**，比堆结论有价值得多。

下面是我认为需要处理的问题，按严重程度排序。

---

## 3. 问题清单

### P0-1 Page 替换（同 cut 内退休 + 重新准入）会永久泄漏 arena cell

**位置**

- [TopologyUpdatePlanner.java:252](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyUpdatePlanner.java#L252)
  `collectAdmissions`
- [TopologyUpdatePlanner.java:289](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyUpdatePlanner.java#L289)
  `initializeAdmission`
- [TopologyUpdatePlanner.java:586](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyUpdatePlanner.java#L586)
  `prepareMigrationsAndRetirements`

**链条**

1. `collectRetirements` 先跑，设 `draft.retirement = true`、
   `draft.topologyDirtyMask = page.residentBrickMask`（旧的完整驻留集）。
2. `collectAdmissions` 发现同 section 有新 handle，转成 replacement，然后调
   `initializeAdmission`，其中 **`draft.topologyDirtyMask = admission.residentBrickMask()`
   是赋值而非按位或**，旧驻留集被整体覆盖。
3. `compileChangedCells` 只按 `topologyDirtyMask` 生成 `cellReplacementMask`。
4. `prepareMigrationsAndRetirements` 只遍历 `cellReplacementMask` 调 `collectOldSpan(previous, …)`。

结果：`previous.residentBrickMask & ~admission.residentBrickMask` 这些 Brick 的
`ArenaSpan` 既不迁移、也不进 `oldSpans`、也不会被 `installBrick` 覆盖。

**后果**

- `liveCellCount` 单调增长，永不回收 → 最终稳定触发 `maximumLiveCells`（65,536）的
  `WORK_LIMITED`，该维度从此拒绝一切拓扑更新。
- 孤儿 fragment 仍装在 solver 里（`collectFragmentDependencies` 对非退休 draft 用的是新的
  驻留掩码，不会重编它们），会继续对属于已死 lifecycle 的 cell 做换热，且这些 cell 仍会被
  `QueryPublication.publish` 每 cut 扫描一次。

**可达性**（推测，未构造复现）
`MinecraftPageManager.retire` → `accumulator.retire(handle)` → `removeEntry`；随后同一 20-tick
窗口内玩家/热源重新产生 interest，新 `PageEntry` 走 capture 队列后调 `accumulator.admit(…)`。
两者会落进同一个 `seal()`。新准入的 `residentBrickMask` 来自新的一轮请求，**没有任何机制保证
它是旧驻留集的超集**——`collectResidency` 里那条 "resident Brick mask cannot shrink" 的断言
只在同一 lifecycle 内生效，替换换了 lifecycle，正好绕过。

**建议**

最小修复：在 replacement 分支里保留旧掩码，

```java
draft.topologyDirtyMask =admission.

residentBrickMask() |previous.residentBrickMask;
```

并确认 `compileCells` 对"新驻留集之外"的 Brick 返回 `WorkerBrickTopology.EMPTY`（`span.count()==0`），
这样 `collectOldSpan` 能拿到旧 span、`installBrick` 能写回空拓扑。

更好的做法：在 `PreparedTopologyChange.PageWrite` 的构造校验里加一条不变量——
`replacedPage != null` 时，`replacedPage.residentBrickMask` 必须被
`stagedBrickMask | indexedBricks` 完全覆盖。这样同类问题以后会在 prepare 阶段就炸出来，
而不是几小时后表现为"这个维度不再更新了"。

另外建议给 `ThermalCellArena` 加一个廉价的不变量自检（DEBUG 下每 N cut 一次）：
`liveCellCount == Σ(所有 PageState 所有 Brick 的 span.count())`。这类泄漏靠读代码很难发现，
靠一行断言很容易发现。

---

### P1-1 `FarFieldSettings` 语义被误用，且两个参数实际是死的

**位置**

- [BrickTopologyCompiler.java:353](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickTopologyCompiler.java#L353)
- [FarFieldSettings.java:22](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/FarFieldSettings.java#L22)

全仓库只有一个调用点，且永远是常量：

```java
double conductance = farField.conductanceForPatches(16, true) * v / 100.0;
```

而这行在 **16 个天空列的循环里**，即"每一列都按整块 Brick 面（16 patch）计价"。
公式 `base · patches / (16 · refArea) · continuation` 在 `patches = 16` 时退化为 `base / refArea`，
于是整个 4×4 面的总电导是 `16 · base / refArea`，而不是 API 名字暗示的 `base / refArea`。

连带后果：

- `openPatchCount` 形参永远是 16 → 无信息量；
- `directSkyExposure` 永远是 `true` → `continuationDistanceBlocks`（生产配置 16.0）**从未被使用**，
  是死参数；
- `referenceOpeningAreaBlocksSquared`（32.0）与配置项 `farFieldConductanceWPerK`
  （默认 `7747.2298793470545`，一个没有任何推导记录的精确到小数点后 13 位的数）互相抵消，
  整条链路实际只有一个自由度。

我倾向于认为这是历史标定的产物而非行为 bug（数值是反推出来的，改公式反而会改变手感）。
但它让任何人都无法从代码或文档判断"如果我把 `farFieldConductanceWPerK` 调大一倍会发生什么"。

**建议**：把三参数塌缩成一个 `skyConductancePerColumnWPerK`，在 `docs/climate/` 里写明
**标定依据**——按当前数值，一个完全露天的满空气 Brick（C = 64 × 1200 = 76,800 J/K）对自然温度
的时间常数约 20 s。这个 20 s 才是设计意图，`7747.2298793470545` 不是。

---

### P1-2 生产路径上有 INFO 级调试日志

**位置
** [ThermalDimensionEngine.java:254](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/engine/ThermalDimensionEngine.java#L254)

```java
if(continuous !=null&&batch.

targetTick() %200==0){
        LOGGER.

info("AIR_TRACE tick={} topology={} limited={} components={} modes={} routeVisits={}", …);
}
```

`continuousAir` 开启后，每 10 秒每维度一条 INFO。同文件里"合并 cut"的日志已经正确地用了
`LOGGER.isDebugEnabled()` 守卫 + DEBUG 级别。`AIR_TRACE` 应该保持一致，否则专用服务器日志会被
稳定刷屏，也违反架构文档自己写的 "Existing route-visit diagnostics … are not substitutes for
performance measurement"。

---

### P1-3 `restorePagePublications` 把旧 Page 的 publication 发布到新 handle 上

**位置
** [TopologyCommitter.java:157](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/TopologyCommitter.java#L157)、
[PreparedTopologyChange.java:316](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/PreparedTopologyChange.java#L316)

```java
rollbackPublication =replacedPage ==null?page.publication :replacedPage.publication;
…
        write.page.handle.

publish(write.rollbackPublication);
if(write.replacedPage !=null)write.replacedPage.handle.

publish(write.rollbackPublication);
```

replacement 情形下，**新** handle 被写入了**旧** lifecycle 的 publication。读者拿到的 arena
generation 与新 lifecycle 不匹配，`tryRead` 会拒绝并退化到 dormant，所以不会读到脏数据——
但这只是碰巧安全。

另一个次序问题：这个回滚只在 `publish()` 抛异常时触发，而 `releaseOldSpans` **已经在此之前
执行过**。也就是说被"回滚"到的 publication 指向的正是刚被释放的 slot。

这条路径只在致命失败（随后 `ENGINE_FAILED` → 重启 worker）时走到，影响有限。但既然
`PreparedTopologyChange` 在别处如此严格，这里的语义应该也说清楚：要么把 `rollbackPublication`
拆成 `pageRollback` / `replacedPageRollback` 两个字段，要么直接注释"replacement 失败时不保证
恢复到一致 publication，依赖 generation 校验拒读"。

---

### P2-1 空气与固体的热容不在同一个标度上，文档没有说明

**位置
** [MinecraftThermalProfiles.java:257](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/profile/minecraft/MinecraftThermalProfiles.java#L257)
`bodyCapacity` 与 `GameplayMaterial`

| 介质 | 本系统       | 真实 SI（每 m³）    | 比值      |
|----|-----------|----------------|---------|
| 空气 | 1,200 J/K | ~1,200 J/K     | **1 ×** |
| 砖石 | 5,400 J/K | ~2,100,000 J/K | ~1/390  |
| 木材 | 2,700 J/K | ~1,000,000 J/K | ~1/370  |
| 水  | 6,600 J/K | ~4,190,000 J/K | ~1/635  |

而导热系数是接近真实的（砖石 1.4 W/K、泥土 1.0 W/K，量级正确）。

这不是 bug，是明确的玩法选择：固体的热扩散率被放大约 400 倍，让墙体在分钟级而不是天级响应。
问题在于**这个选择只体现在数字里，没有写进文档**。`heat-production-and-network.md:140` 只说
"uses six times the former per-face capacity (stone: 5400 J/K; wood: 2700 J/K)"——用一个内部
历史锚点解释另一个内部数字，读者无从判断这套单位是不是 SI 可信的。

实际的玩法含义值得写下来：**一格砖石的蓄热只相当于 4.5 格空气**（真实世界是 1750 倍）。
这意味着这套模型里"厚墙保温"主要靠的是导热阻隔，几乎没有蓄热惯性——房间不会有"石头白天吸热、
夜里放热"的行为。这是平衡设计必须知道的事实。

AGENTS.md 自己要求 "Define formula symbols, units, ranges, and whether values are source defaults
or runtime configuration"。建议在 `docs/climate/` 加一张表，两列：**数值** / **它相对真实物理
是几倍**，并显式声明"空气按 SI，凝聚相按玩法标度压缩约 400 倍"。

顺带一个小瑕疵：同一个方法里

```java
if(state.getBlock() instanceof LiquidBlock){
        return WATER_CAPACITY_J_PER_K *state.

getFluidState().

getAmount() /8.0;
        }
```

**熔岩也是 `LiquidBlock`**，于是熔岩拿到了水的热容参考。考虑到熔岩在
`heat-production-and-network.md:277` 有专门的 `O = −580,000 (C = 6,600 J/K)` 标定，这可能是
刻意复用同一个 C，但代码里读起来像是漏判。至少值得加一行注释说明是有意的。

---

### P2-2 "每个 resident Brick 内的每个材料方块一个 cell" 与 65,536 的上限存在张力

**位置
** [BrickTopologyCompiler.java](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/BrickTopologyCompiler.java)
`compileCells`、
[MinecraftThermalInput.java:198](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L198)
`ThermalDimensionLimits`

`compileCells` 给 Brick 内**每一个**有材料 profile 的方块都分配一个 arena cell，不管它是否与
空气接触。而 `TopologyView.materialContactAllowed` 只允许"表面层 + 其唯一拥有者的第二层"参与
换热。于是一个位于墙体内部的 resident Brick 会分配 64 个 cell，其中可能只有十几个真正参与
任何 operation，其余纯粹是常温死重——但它们照样占 live cell 配额、照样被
`QueryPublication.publish` 每 cut 扫一遍。

同时 `residentBrickMask` **在一个 Page lifecycle 内只增不减**（这是明确的设计，见架构文档
"Bricks are not individually evicted"）。

配额是 `maximumLiveCells = 65,536`。粗算：一个 16×16×16 的封闭房间，表面约 1,500 格，
两层材料约 3,000 cell，加上所在 Brick 里被顺带分配的内部方块，实际会更多。**十几到二十个
这种房间就会打满整个维度的配额**，之后永久 `WORK_LIMITED`。

我没有测量过真实基地的 resident Brick 分布，所以这是**推测而非结论**。但考虑到目标是"冬季
救援"这种会有大型聚落的整合包，建议：

1. 加一个"live cell 占用率"的运行期指标（DEBUG 或命令），先把真实分布测出来；
2. 如果确实吃紧，考虑对"无任何 contact 且不在红外可视窗口内"的材料方块不分配 cell——
   代价是红外显示会缺这些格子，需要权衡；
3. 无论如何，`WORK_LIMITED` 目前只退避 200 tick 然后重试，**到达硬上限时会变成永久重试循环**，
   没有任何面向运维的可见信号。至少应该 WARN 一次。

---

### P2-3 `sleepResidualC = 1.0e-6` 使休眠路径在实践中几乎不可达

**位置
** [MinecraftThermalInput.java:207](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L207)、
[ThermalSolver.maxTemperatureResidualC](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/solver/ThermalSolver.java)

休眠门槛是"**所有** pair 的温差 与 **所有** boundary 的温差都 ≤ 1e-6 °C"。

按当前标定：砖石 τ = C/G = 5400/1.4 ≈ 3,857 s。从 1 K 温差衰减到 1e-6 K 需要
`ln(10⁶)·τ ≈ 13.8 × 3857 s ≈ 15 小时`游戏时间。空气 Brick 间 τ ≈ 100 s，也要约 23 分钟。

而驻留释放门槛是 `FRONTIER_RELEASE_LOW_C = 0.5 °C`——也就是说，**Page 会先退休，而不是先休眠**。
`sleeping` 实际只在"整个维度已经没有任何 resident cell"时成立，此时 `O(1)` 的快路径省下的
也没多少。

这不是错误（保守门槛不会产生错误结果），但意味着：

- `republishUnchanged` / `unchangedSleeping` 这条被文档反复强调的优化路径基本是死的；
- `stableBatchesBeforeSleep = 20` 每次都会白跑一次 `maxTemperatureResidualC()`，那是一次
  全 fragment 全 operation 的扫描。

建议要么把门槛换成有物理含义的量（例如"本 cut 内最大 |ΔT| < 0.01 °C"，即变化率而非绝对残差），
要么承认这条路径不生效并删掉它。目前的状态是"有一条看起来很重要、实际不执行的优化路径"，
这本身就是维护负担。

---

### P2-4 `ThermalMemoryBudget` 覆盖面远小于文档暗示

**位置
** [MinecraftThermalInput.java:83](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L83)

`MEMORY = new ThermalMemoryBudget(128 MiB)` 是全局静态、跨维度共享，但只有
`QueryPublication`、`BlockRadiationIndex`、`RadiationService` 真正走 `tryReserve`。

**不受预算约束的**：`ThermalCellArena`（十几个与 slot 数同长的原始数组）、`ThermalSolver`
（fragments / materialExecutions / stateReferences / MaterialEdgeTable）、`WorkerPageStore`、
`PageSignatures`、`ThermalSourceLedger` / `NodePowerAccumulatorArena`、以及 continuous 后端
`ContinuousAirSolver.rebuild` 每次拓扑提交重新分配的全部向量。

架构文档有一句 "the 128 MiB reservation budget does not bound the runtime's entire Java heap"，
`continuous-air-runtime.md` 也在 Outstanding limits 里列了这一条——**标注是诚实的**。
但 128 MiB 这个数字在两篇文档里出现了多次，容易被误读成总量上限。

建议：给这个数字一个准确的名字（它实际上是"查询与辐射发布的预算"），并对未纳入预算的部分给出
一个可算的上界，例如 `maximumArenaSlots × 每 slot 字节数` —— 按 131,072 slot 粗算，光 arena
的 SoA 数组就在 8–10 MiB 量级，是可以直接写出来的。

---

### P3 其它（可读性 / 一致性 / 卫生）

| # | 问题                                                                                                                                                                                                                 | 位置                                                                                                                                                               |
|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| a | `RadiationService.Parameters` 13 个位置参数，调用点全是裸字面量 `(3_200, 128, 64, 8, 24, 8, 256, 16.0, 0.1, 0.5, 0.1, 0.9, 1.62)`，可读性为零                                                                                           | [MinecraftThermalInput.java:85](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L85)             |
| b | `ThermalDimensionLimits` 9 个位置数值参数，同上                                                                                                                                                                              | [MinecraftThermalInput.java:198](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java#L198)           |
| c | `MinecraftPhaseController.MATERIAL_SAMPLE` 是 **static** 可变 scratch，注释靠"两个调用方都在主线程"作保；同仓库其它地方用的是 `ThreadLocal<MaterialSample>`。不一致，且静态可变状态在多 `ServerLevel` 场景下是隐患                                                   | [MinecraftPhaseController.java:37](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPhaseController.java#L37) |
| d | `MinecraftThermalInput` 1,688 行、约 40 个 public static 方法，同时承担生命周期、玩法查询、Forge 事件、持久化捕获、热源发现。架构文档承认了 checkpoint 职责重叠，但没承认这个门面本身已经是 god object                                                                         | `runtime/minecraft/MinecraftThermalInput.java`                                                                                                                   |
| e | 98 个文件中 **79 个只有 `/* Copyright (c) 2026 TeamMoeg */` 一行**，没有 GPL-3 通知。`build.gradle:629` 配了 license 插件指向 `HEADER.txt`，但 `ignoreFailures = true`，所以 CI 不会发现。对 GPL 项目这是合规问题，不是风格问题                                   | `build.gradle:629`                                                                                                                                               |
| f | 0 个 `package-info.java`；98 个文件中只有 14 个带中文 Javadoc。`.claude/memory/architecture.md` 规定的中英双语注释规范在本包基本未执行（chorda 包做到了）。考虑到这是全仓库最复杂的子系统，注释密度反而最低                                                                       | 全包                                                                                                                                                               |
| g | `MaterialThermalLaw.selectBranch(energy, branch, 0)` 在 `direction == 0` 时会无条件返回 `COOLING`（`Math.signum(0) = 0`，两个不等式都退化为真）。当前调用方都保证 direction ≠ 0，但这是一颗定时炸弹                                                        | `mesh/MaterialThermalLaw.java`                                                                                                                                   |
| h | `ContinuousAirSolver.solveCut` 的 `while (tick <= toTick)` 循环：若出现 `eventTick < fromTick` 的记录，`next` 会小于 `tick`，`advanceSeconds` 收到负值直接返回，`tick = next` 回退 → **死循环**。目前靠 `ThermalSourceLedger` 的区间校验保证不发生，但求解器自己没有防线 | `solver/ContinuousAirSolver.java`                                                                                                                                |

---

## 4. 对文档的意见

### 4.1 好的部分

- 七篇文档的 `Status` / `Last verified` / `Scope` / `Code anchors` 头部是对的做法。
- **主动声明"这不是什么"**是最大的优点。例如："This stopping criterion is not a certified
  physical temperature-error bound"、"This is one controlled network, not a universal error
  bound"、"This is an explicit gameplay range limit, not a physical claim about real walls"。
  这些句子让文档可以被信任。
- `continuous-air-runtime.md` 的 Outstanding limits 五条清单，是我见过的 mod 文档里少有的
  诚实交代。

### 4.2 需要改的

**(1) 密度过高，缺少层次。**
`thermal-runtime-architecture-and-optimization.md` 1,036 行几乎全是"事实句"的平铺——每句话
都对，但读完记不住任何结构。`## Page And Brick Model` 一节里 "Page 是生命周期容器不是驻留
单位" 这条核心概念，被埋在第三段中间。建议每节开头加 2–3 句"这一节要解决的问题是什么"，
再进入细节。

**(2) 只讲 what，不讲 why。**
文档记录了大量"现在是这样"，但几乎不记录"为什么不是别的样子"。例如：

- 为什么 Brick 是 4³ 而不是 2³ 或 8³？
- 为什么用 exponential pairwise exchange（Gauss-Seidel 式）而不是隐式矩阵求解？
  （标量后端的 approach fraction ≈ 0.01，所以顺序偏差可以忽略——这个论证是成立的，
  但文档里没有，读者无法自行判断。）
- 为什么 `CUT_INTERVAL_TICKS = 20` 而不是 10 或 40？

这些"为什么"是最容易随人员流动丢失的知识，也是文档最该承载的部分。

**(3) 缺少数值标定表。**
散落在文档各处的数字（1200、96、7747.2298793470545、5400、1.4、1800、8000、0.2、16/32/…）
没有一张集中的表说明：单位、量级依据（SI 还是玩法标度）、改动它会影响什么、由谁标定。
这正是 P1-1 和 P2-1 两个问题的根源。建议在 `docs/climate/` 加
`thermal-calibration.md`，三列：**符号 / 当前值 / 标定理由与可观察后果**。

**(4) 没有一张图。**
`docs/climate/README.md` 里那个 ASCII 流程图是全部的视觉材料。对一个有
Page / Brick / cell / fragment / component / shape 六层空间概念 + 主线程/worker 两侧所有权 +
四级查询回退的系统，这远远不够。`docs/figures/` 目录已经存在，`Figure_Guidelines.md` 也有了。
至少需要两张：**空间层级图**、**一个 cut 的时序与所有权转移图**。

**(5) 锚点与代码的漂移无法机器检测。**
文档引用了大量类名、常量名、数值（`FRONTIER_REFINE_HIGH_C = 1.0 C`、`LocalAirShape.RADIUS = 6`、
`65,536` / `131,072` / `3,200`）。我抽查的都对得上，但这是靠人工比对。建议加一个轻量
CI 检查：从文档里抽取形如 `` `IDENT` `` 的代码锚点，验证它在 `src/` 中存在。成本很低，能
防住最常见的一类文档腐化。

**(6) `Last verified` 已经开始漂。**
`thermal-runtime-architecture-and-optimization.md` 头部写 `2026-09-16`，但它在最新提交
`b62378a09 连续空气温度场` 里被修改过。既然规范要求这个字段，就应该在同一次改动里更新它，
否则字段本身会失去意义。

**(7) 红外渲染内容放错了地方。**
架构文档里有约 60 行在讲 `InfraredSurfaceTarget` / `OwnedChunkVertexType` / GLSL /
D24 vs D32F / Embeddium 桥接。这些是**客户端渲染**，和"thermal runtime architecture"是两个
系统，只是碰巧共用数据源。建议拆成 `docs/climate/infrared-rendering.md`，架构文档只留
"红外从 `QueryPublication` 读什么、以什么一致性保证读"。

---

## 5. 测试与验证

- 119 个 `@GameTest`、6,342 行测试代码，且 `ContinuousAirProductionGameTests` 坚持"只用真实
  方块、真实燃料、真实世界 tick、公开查询接口，绝不直接写 H / 构造 engine / 伪造 ACK"——
  这个纪律非常正确，比任何数量的白盒单测都有价值。
- 但 **JUnit 层已在 2026-09-16 被整体删除**（架构文档 Validation Standard 一节有记录）。
  结果是：`MaterialThermalLaw` 的分支选择、`ThermalExchangeKernel` 的解析解、
  `DormantThermalCooling` 的段式积分、`ThermalFreeSpanIndex` 的 AVL best-fit、
  `MaterialEdgeTable` 的开放寻址删除语义——这些**纯函数、无 Minecraft 依赖、最适合单测、
  出错最隐蔽**的部分，现在完全没有针对性覆盖。

  我理解删除是用户的明确决定，不主张恢复旧 fixture。但建议为这几个纯数值类新写少量
  property-based 测试（例如"任意 H/branch/direction 下 `energyLimitJ` 非负且
  `selectBranch` 幂等"、"pair exchange 严格守恒且不越过平衡点"）。P0-1 那种结构性泄漏
  也完全可以用一个 arena 不变量断言在 GameTest 里覆盖。

- 性能证据目前是空白：架构文档自己写了 "Controlled 120-second door/block/source/player/crop
  JFR workloads and 10/30-minute combined/churn heap runs **remain performance evidence rather
  than undocumented claims**"——也就是说这些测量**还没做**。在 continuous 后端从 opt-in 转
  默认之前，这是必须补齐的。

---

## 6. 建议的处理顺序

1. **P0-1**（Page 替换 span 泄漏）+ 配套的 arena 不变量断言。这是唯一会导致维度永久失效的问题。
2. **P1-2**（AIR_TRACE 降级到 DEBUG）。一行改动。
3. **P2-2 的第 3 点**：`WORK_LIMITED` 达到硬上限时至少 WARN 一次。同样是小改动，但能让上面
   那类问题在生产环境可见。
4. **P1-1 + P2-1**：写 `docs/climate/thermal-calibration.md`，顺手把 `FarFieldSettings`
   塌缩成一个参数。这两件事一起做成本最低。
5. **P1-3 / P2-3 / P3-c / P3-g / P3-h**：一轮防御性清理。
6. **P2-2 的第 1 点**：加 live cell 占用率指标，实测后再决定要不要动 cell 分配策略。
7. 文档的 (4) 图 与 (7) 红外拆分。
8. P3-e（许可证头）可以一条 `./gradlew licenseFormat` 解决，但要先确认那 79 个文件的简写头
   是有意为之还是遗漏。

---

## 7. 一句话总结

这套系统的**工程纪律**（线程所有权、事务式拓扑、能量守恒口径、诚实的局限声明）达到了很高的
水准，我找到的唯一严重缺陷是一条罕见时序下的资源泄漏。真正的风险不在代码质量，而在
**标定数值缺乏可追溯的依据**和**容量上限（65,536 live cell）与"每方块一个 cell"策略之间
尚未被实测验证的张力**——这两件事都不会在小规模测试里暴露，但会在一个真实的大型聚落里暴露。
