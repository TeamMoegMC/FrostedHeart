# Thermal runtime 已加载世界接入方案

- Time: `2026-09-08 00:38:47 +08:00`
- Last reviewed: `2026-09-08 02:28:51 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary design/review agent`
- Status: `completed; runtime fix and owner optimizations implemented, production GameTests passed; large-world performance characterization remains follow-up`
- Scope: runtime 启动/重建、区块接入、物理热源发现、mutation owner、容量恢复
- Related: [runtime 当前实现](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[物理热源](../docs/climate/heat-production-and-network.md)、[此前篝火修复](../diary/2026-09-01_20-34-07_preexisting-campfire-source-discovery.md)

## 决策

采用 runtime 负责的“一次世界接入 + 增量事件”。启动、真实 chunk Load、状态热源容量拒绝统一进入一个待处理队列。完成的区块不留扫描记录，静止篝火不执行周期发现。generator、radiator、fountain 复用已经存在的正常生产输出发布；四种 Minecraft physical source 在同一个观察入口应用输出是否有效的规则。

保留按需创建 runtime 的现有策略。整个 runtime 新建时补齐已加载世界；仅 worker 重启时复用已有主线程索引。runtime 未运行期间不补算热量，注册仍使用现有 `flush(gameTick)` 和 20-tick cut 时间语义。无人维度是否从世界启动起持续模拟，不在本次变更范围内。

性能优化分开归因：`ownersByIdentity` 与每个 `SectionOwner` 的三个 `AtomicBoolean` 都是当前已有结构，不是本方案新增，也不是漏发现的根因。将删除重复身份索引、原子标记内联纳入独立实施步骤，降低补齐 owner 时的分配成本；runtime 正确性修复不依赖这两项优化。机器观察提前返回只列为低优先级候选。

## 复查结果

上一版方向成立，但以下内容需要修正；下文是修订后的完整方案。

| 问题 | 源码依据与影响 | 修订 |
|---|---|---|
| 扩大扫描会把熄灭篝火也变成 residency seed，停机机器有同类问题 | 四种 Minecraft profile 共用 `observe`；新 slot 在 `flush` 中调用 `refreshTargets`，后者不检查 enabled | 在公共入口按 `enabled && powerW > 0` 保留 source；零份额 AIR_FACE 不建立 seed |
| 启动单独排队、真实 Load 同步扫描 | 原预算没有覆盖运行中的批量加载 | Load 只绑定 owner 并入队；普通候选扫描共用预算 |
| 启动列表与常驻已扫描集合重叠 | 实际只有未完成和被拒绝的区块需要继续处理 | 一个待处理队列替代两者，完成后移除 |
| 容量拒绝只有全局标记 | `capacityRecoveryPending` 无法定位失败的 chunk；已完成 chunk 的新 mutation 也可能被拒绝 | 内部观察返回完成结果，主线程调用方通过 `SectionOwner.chunk()` 精确入队 |
| 扫描顺序影响附近首次可用时间 | `ChunkMap.getChunks()` 没有附近优先语义 | 创建时一次性优先触发位置周围已加载的 3x3 chunks |
| `getFullChunk()` 与 Forge loaded-only 读取条件不同 | `getChunkNow` 还处理 `currentlyLoading`，并读取 FULL status future | 枚举 holder 坐标，以已有 `getChunkNow` 取得可用 chunk；不等待 future |

## 已核实的生命周期

- [`MinecraftThermalInput.start`](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java) 只创建对象并写入 `ACTIVE`，未接入旧 chunks。主要创建入口为 `gameplayPlayerEnvironment`、`upsertGameplayAnalyticField`。
- [`MinecraftPageManager.onChunkLoad`](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPageManager.java) 绑定 owner、唤醒已有 Page 请求；chunk source 扫描在 `captureResidency` 首次创建 Page 后。首个 source 与首个 Page 会互相等待。
- `onSectionSetBlockState` 只有 section 和局部坐标，必须借助 owner 定位 runtime/世界位置。close 会解绑 owner；仅绑定有 source 的 section 会漏掉其他 section 后来出现的热源。
- recipe reload 销毁整个 runtime；`restartWorker` 保留主线程索引并 `reseedAll`。两者应采用不同恢复路径。
- [`PhysicalSourceSpatialIndex.onChunkLoad`](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/source/minecraft/PhysicalSourceSpatialIndex.java) 只遍历已实例化的篝火 BE；`getBlockEntitiesPos()` 覆盖 pending/live 并集，篝火无需实例化 BE 就能读取状态。
- [`accesstransformer.cfg`](../src/main/resources/META-INF/accesstransformer.cfg) 已开放 `ChunkMap.getChunks()`。本地 mapped Forge 源码确认 `ServerChunkCache.chunkMap` 与 `getChunkNow` 可用于主线程 loaded-only 枚举。

## 对比过的替代方案

| 方案 | 成本与结论 |
|---|---|
| 整个 runtime 提前到 level load | 改变各维度资源分配，recipe reload 后旧 chunks 仍不会重放 Load；单独采用不能闭合重建问题 |
| 独立于 runtime 的长期 source/input 目录 | 能减少重建扫描，但增加跨 runtime 的所有权、profile 更新和 replay 状态；当前重建频率没有证明这项复杂度必要 |
| BE onLoad 或 source 周期检查 runtime epoch | onLoad 仍覆盖不了已加载 BE 遇到 runtime 重建；epoch 检查仍需周期入口，也覆盖不了无 tick 状态源 |
| 同步扫描全部 BE | 代码少，但把所有候选读取和 source 建立集中到启动的玩法调用 |
| 全 section palette 过滤后扫描命中 section | 能处理无 BE 状态源，但每个命中 section 要读 4096 个位置；`GlobalPalette.maybeHas` 总是 true。不能假定比现有 BE 稀疏候选枚举更快 |
| 单一待处理队列 + 当前候选枚举 | 复用事件与索引，统一预算，只重试未完成 chunk；选用 |

保留按需启动、不维护另一份 source 目录、不周期轮询时，新 runtime 必须读取已有状态。优化目标是读取范围、排程和常驻状态，而不是承诺消除这一必要工作。

## 输入所有权

```text
start: current loaded chunks ---+
                               v
real ChunkEvent.Load ----> attachLoadedChunk
                          section owners + enqueue
                               |
state observation refused -----+--> pendingSourceChunks
                                    budgeted current-state scan
                                               |
BlockState mutation / machine output ----------+
                                               v
                                  PhysicalSourceSpatialIndex
                                               |
                                  source seed / input batch
                                               v
                                   Page admission / worker
```

`MinecraftThermalInput` 编排接入并持有唯一 discovery 队列；`MinecraftPageManager` 继续拥有 section mutation 与 Page capture；`PhysicalSourceSpatialIndex` 负责 observation、target 索引及 batch 编码。不新增 scheduler/provider 框架。删除 `captureResidency` 中的 source 发现副作用，扫描也不创建整个 chunk 的 Page。

### 1. 先绑定 owner，再排队扫描

内部 `attachLoadedChunk(LevelChunk)` 同步完成轻量元数据接入：绑定或复用各 section owner、唤醒已有 Page 请求、将 chunk 入队。这里不枚举 BE，不捕获全 section 几何。

相同 runtime/chunk/section 身份必须保留原 owner 及其 pending mutation。扩展 `ensureSectionOwner` 并核对 chunk、section 身份和 section key；不能重复使用当前无条件替换 owner 的 `attachSection`。真正替换仍走 `onSectionIdentityReplaced` 和 full resync。

启动在主线程注册 runtime 后，一次枚举 visible holders，以其坐标调用 `getChunkNow`，接入可用 chunk。迭代器不跨 tick 保存，不 pump/wait future；尚未可用的 chunk 由后续真实 Load 接入。

同步绑定使 `start` 返回后已有 section 能接收 mutation，也避免 source cap 满时停绑 owner。这个步骤仍有 `O(C + S)` 启动成本；分 tick 的是候选扫描。当前 hook 所需的全覆盖元数据，不能通过只绑定已有 source 的 section 省略。

真实 Load 中 dormant `activateLoaded`、磁盘支持位消费和 radiation/occlusion 邻居通知保持真实生命周期语义。启动不伪造 Load、不重复执行这些动作；新 radiation 索引仍按已有查询惰性建立。Page 所需的 chunk 可用性唤醒由内部接入保留。

### 2. 一个 discovery 工作集

使用已有 fastutil 的 `Long2ObjectLinkedOpenHashMap<LevelChunk>`：packed chunk 坐标为 key，当前 loaded 实例为 value，提供 FIFO、坐标去重和卸载实例核对。

该 map 替代 `sourceScannedChunks`、`sourceRecoveryRemaining`、恢复轮次逻辑及上一版的 bootstrap list/cursor。完成后没有条目；待办期间重复 Load 合并，完成后偶发的重复通知允许再观察一次，依靠现有值比较避免重复 payload/seed。不为“永远只扫描一次”保留完成目录。

主线程直接使用待处理 chunk 引用，每次 drain 不再 `getChunkNow`。卸载按坐标和实例移除待办；close 清空；同坐标新实例替换旧待办。Minecraft 对象不进入 worker batch。

drain 开始时取 `min(初始队列大小, 每 tick 预算)` 次尝试。取出队首，读取当前状态；全部观察完成则结束条目，容量拒绝则放回队尾。一次 drain 不反复处理刚拒绝的 chunk。

初始预算建议 `8 chunks/tick`，为待测内部常量；启动、Load、容量重试共享。单 chunk 的 `getBlockEntitiesPos()` 分配和遍历不可中途截断，因此这不是硬毫秒上限。本阶段不再增加候选游标或第二层预算。

source cap 满时不扫描 discovery 队列。普通 mutation/full resync、source remove、unload、worker completion 和 flush 继续处理，释放容量后下一次 drain 自然恢复。只做队列/容量的常数检查，不扫描已完成世界。

队列空时不持有 chunk 引用；backing arrays 可保留高水位容量复用，逻辑条目归零不等于堆内存立即归零。

### 3. 容量拒绝定位到具体 chunk

内部 `observe`、`resyncBlock`、`resyncSection` 和 chunk 扫描返回“所需观察是否全部完成”。非热源、未变化、成功移除均算完成；需要新 slot 却无法获得才算未完成。用 boolean 即可，不分配结果对象。

- chunk 扫描未完成，由 drain 将当前 chunk 放回队尾。
- `drainMutations` 的点观察/full resync 未完成，通过已有 `owner.input()`、`owner.chunk()` 请求同一队列。
- full section resync 遇到拒绝也必须完成旧 source 清理与当前状态遍历。不能先标记 absent，再提前退出，把未访问的有效 source 误删。
- 普通队列重试只 upsert 当前候选，不通过 full geometry resync 制造批量重建。
- 机器容量拒绝由之后的正常完整输出重试；篝火候选队列不声称能恢复机器计算结果。

去掉无定位信息的 `capacityRecoveryPending`、begin/continue recovery-pass API。原容量测试转为验证实际拒绝、待办保留、释放后注册。

### 4. 四种 Minecraft source 共用输出规则

```text
emitting = enabled && powerW > 0
seedPort = emitting && port.kind == AIR_FACE && port.powerShare > 0
```

在 `PhysicalSourceSpatialIndex.observe` 分配 slot 之前应用 `emitting`：有输出时沿现有路径注册/更新；没有输出时不新建 slot，已存在的本来源沿 remove/flush/unload 释放。该规则覆盖 CAMPFIRE、GENERATOR、RADIATOR、FOUNTAIN，不在各生产者重复写条件，也不改变通用 worker `ThermalSourceLedger` 的 enabled/IMPULSE 合约。

篝火适配只提供 BlockState 的 LIT 和固定 ratedPower；机器提供已有生产结果的 active 与 `profile.powerForLevel(thermalLevel)`。当前四种 profile 都是非负功率，适用同一判断。`resyncBlock` 的非篝火清理仍只作用于篝火 profile，不让普通方块 mutation 移除机器来源。

初始扫描、点 mutation、full resync、重试和机器输出都落到同一规则。`campfirePowerW=0` 不保留无输出 source；`campfireRadiationShare=1` 仍保留有功率的辐射 source，但 `refreshTargets` 跳过零份额 AIR_FACE，不为零对流功率建立 Page。

重新点燃由 owner 接收；机器恢复输出由下一次正常生产发布接收。同一 cut 仍合并最终状态；已提交卸载后的注册沿现有 lifecycle generation 执行。停机不清空温度，其余 source、worker hot/frontier 和 dormant checkpoint 继续决定余热。

释放 source 要刷新原 target 的 dormant support 闭包：移除本 source 支持后查询其他来源，避免留下旧 `sourceSustained`。当前 `releaseTargets` 没有完整刷新，需一起修正。anchor/profile 变化释放旧目标时也更新原闭包，再观察新目标。此处复用最多三个 port 的现有目标信息，不增加历史目标目录。

跨 cut 的频繁启停会产生 unregister/register；20-tick 最终状态合并仍抑制同 cut 内来回切换。通过公共移除路径释放闲置 source/seed，减少常驻工作，不另建 disabled-source 缓存。启停、目标重定向与已有 generation 合约需一起验证。

### 5. 候选枚举与机器边界

普通扫描使用 `getBlockEntitiesPos()` 的 pending/live 并集，读取当前 BlockState，共用状态观察。无需 `getBlockEntity()`、NBT 解析或 BE 实例化。

full section/container 替换仍沿已有位置扫描修复。`maybeHas` 只能在清理旧记录后作为负向快速过滤；positive 不代表一定存在热源，`GlobalPalette` 总是 true。未来实际增加无 BE 状态源时，再扩展同一队列的候选枚举。

`onGeneratorTick`、`onRadiatorTick`、`onFountainTick` 保留正常生产的完整输出发布。bootstrap 不执行会耗燃料/热网资源的 `tickFuel`，新 runtime 从下一次权威输出恢复这些 source。以下已追到实际生产调用，不以方法名推断会重复发布：

| Source | 正常发布链与数据 | 停止/移除 | runtime 重建或容量拒绝后的恢复 |
|---|---|---|---|
| Generator T1/T2 | `HeatingLogic.tickServer -> GeneratorLogic.tickFuel -> state.tickData -> onGeneratorTick`，提交 `state.getTempLevel()` 和 `GeneratorData.isActive`；T1 继承，T2 明确调用 `super.tickFuel` | 正常计算仍提交 inactive；结构拆除走 `HeatingMultiblock.disassemble` | 下一次有效机器 tick 再提交完整当前输出，不依赖功率变化 |
| Radiator | `HeatingLogic.tickServer -> RadiatorLogic.tickFuel -> onRadiatorTick`，提交本次热网取热结果和温度级别 | 取热失败提交 false/0；结构拆除同上 | 下一次有效机器 tick 自动重试；bootstrap 不额外 `tryDrainHeat(4)` |
| Fountain | `FountainTileEntity.tick` 的供热分支每次 `publishHeat`，提交当前网络级别及 `worldPosition.above(height + 1)` | 无输出分支和 `onRemoved` 调用 `removeHeat` | 下一次有效 BE tick 发布或移除，不等待 BlockState 变化 |
| Campfire | chunk 候选/精确 mutation 提交 LIT 和 profile | 熄灭或移除经公共观察规则清理 | 由统一发现队列和 owner 接入恢复，无发现专用 tick |

代码锚点：[HeatingLogic](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/HeatingLogic.java)、[GeneratorLogic](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorLogic.java)、[T2GeneratorLogic](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/t2/T2GeneratorLogic.java)、[RadiatorLogic](../src/main/java/com/teammoeg/frostedheart/content/climate/block/radiator/RadiatorLogic.java)、[FountainTileEntity](../src/main/java/com/teammoeg/frostedheart/content/steamenergy/fountain/FountainTileEntity.java)、[HeatingMultiblock](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/HeatingMultiblock.java)。

Generator 的 source ID 是 `absoluteMaster.below(masterYInMB)`，热出口是该位置上方 multiblock 高度处；radiator 使用同种 ID、目标上移 2 格。拆除计算同一 ID，且 source 与 master 的 X/Z 相同，因此 `beforeChunkUnload` 的 origin-chunk 索引也覆盖它们。Fountain 使用自身 BlockPos。source 坐标与热出口不能混为一个身份。

`GeneratorData.tickBlock` 可能因 `townProcessedTicks` 提前返回，这只是避免重复消费；外层 `GeneratorLogic.tickFuel` 仍继续发布。`GeneratorData.townTick` / `TeamTownData.tickSecond` 本身不提交 thermal physical source，因此不能把城镇离线燃料/温度模型当作另一条物理热源发现入口，也不能在 bootstrap 再调用它们。

恢复承诺以“生产者再次获得一次正常有效 tick”为界。已核对 IE `MultiblockBEHelperMaster.tickServer` 先检查其 chunk 可运行条件再调用 `IServerTickableComponent`，因此 loaded 不等于机器一定 tick；本方案不绕过 Minecraft/IE 的运行条件。仅 loaded、一直不 tick 的机器不会被主动推进或从存档 active 标记推算新的物理功率；现有 held-last-output 与暂停 tick 的关系也没有被本方案改成新的模拟规则。

未来仅在变化时通知的生产者必须提供首次/重新接入发布。第三方 BE 或离线机器若要求无正常 tick 也恢复供热，需要其明确的当前输出适配；不能宣称当前方案自动覆盖。现有机器每次正常计算后发布，不额外增加发现轮询。输出不变仍有一次 O(1) 索引检查，但不产生 dirty batch；改为 change-only 还要补首次、重建、拒绝重试状态，当前不增加这套状态。

### 6. 延迟与 cut 顺序

私有 `start` 增加已有触发位置参数：玩家位置或 analytic field 中心。接入结束后，一次性将其周围已在队列中的 loaded 3x3 chunks 提到队首。只影响发现次序，不保持 player residency、不创建 Page、不在后续查询重复执行、不加载区块。

保留当前 completion、迟到 cut、`pages.tick`、phase/radiation 顺序，在已有 flush 位置附近安排 discovery：

```text
existing completion / overdue cut handling
pages.tick / phase.tick / blockRadiation.tick
if cut: physicalSources.flush(t)       // 先落实移除并释放 slot
processed = drainPendingSources(8)    // 每 tick 可推进
if cut:
    if processed: physicalSources.flush(t)
    submitCut(t)
```

非 cut tick 的观察进入 desired/dirty，后续 cut 才编码事件；source seed 在 flush 建立，Page admission 沿原预算推进。扫描完成、注册提交、Page 可用、升温是不同时间点，不承诺下一 tick 一定升温。

无容量阻塞时，至多 9 个优先 chunk 可在两个 discovery tick 内完成候选观察；不包含同步 owner 接入、20-tick cut、Page 等待或 worker 耗时。其余 chunk 按队列处理。

## 生命周期结果

| 场景 | 处理 |
|---|---|
| 首次 lazy start | 同步绑定旧 loaded sections，候选扫描进入统一队列 |
| 新 chunk 加载 | 真实 dormant/radiation 通知 + owner 绑定 + 同一队列 |
| recipe reload 后新 runtime | 新 profiles、新 owner 接入、新队列；不依赖新 Load |
| worker-only restart | 保留 source/index/待办，现有 `reseedAll` 恢复，不再枚举世界 |
| source cap 拒绝 | 精确保留/加入所在 chunk，空位恢复后继续 |
| 卸载/同坐标重载 | 移除旧实例待办，checkpoint/support 判定先于 origin source 删除，新实例重新入队 |
| close / level unload | 沿现有 checkpoint 次序清理，移除所有待处理 chunk 引用 |

## 成本与范围

`C` 为 loaded chunk 数，`S` 为 section 数，`B` 为 BE 候选总数，`P` 为未完成 chunk 数，`L` 为 lit campfire 数。

| 项目 | 修订后成本 |
|---|---|
| 同步启动元数据 | `O(C + S)`，不读所有方块、不枚举全部 BE |
| 候选发现 | 通常 `O(B)`，分 tick；拒绝或重复通知只重读相关 chunk |
| discovery 状态 | 一个 map，条目 `O(P)`；数组取决于高水位，无完成目录 |
| owner 内存 | 保留 `O(S)` owner 与惰性 dirty 数据；独立优化删除重复身份索引、内联原子标记，降低常数 |
| 稳定篝火发现 | 删除每 tick 错峰判断和每秒 `O(L)` 发现观察 |
| source/solver | 仍随活跃 source/Page/Brick/真实变化增长；四种 Minecraft source 无输出时不新占 source/seed，纯辐射端口不产生对流 seed |
| 持续满容量 | discovery 仅队列/容量检查；真实移除与 mutation 继续 |

必要代码范围：`MinecraftThermalInput`、`MinecraftPageManager`、`PhysicalSourceSpatialIndex`、`CampfireTileEntityMixin_TimeLimit` 及对应测试。删除 `onCampfireTick`、Page admission 发现副作用和原全量恢复轮次状态；保留燃料寿命逻辑、worker 协议、Page capture 预算。

需测量 owner 同步绑定在大量 chunks 下的峰值、单个 BE 密集 chunk 的扫描耗时、source 上限附近重试成本。若 owner 接入尖峰实测不可接受，再细分绑定阶段；不提前添加跨 tick owner 状态机。8 chunks/tick 是起点，不是已测最优值。

### 成本估算与口径

以下为源码结构估算，不是实测。假设 Java 17 HotSpot 使用压缩引用、8 字节对象对齐，主世界每 chunk 有 24 个 section；其他维度按实际 section 数计算。以 1,000 个 loaded chunks、24,000 个 sections 为例：

| 项目 | 估算及含义 |
|---|---|
| 当前 owner、三个原子对象及两份索引 | 约 4–5 MiB；属于完整覆盖的基础量级，新增量取决于此前未绑定的 section 数 |
| 1,000 个 pending chunks | linked map 底层数组约 40 KiB，不复制 chunk 内容；同时替代原已扫描集合 |
| mutation 位图 | 每份 `long[64]` 约 528 bytes；10% sections 各分配一份约 1.2 MiB，全部各一份约 12.1 MiB，全部各两份约 24.2 MiB |
| 两项 owner 优化 | 预计合计减少约 1–2 MiB 基础占用；不减少 mutation 位图、source 或 worker 占用 |

位图在 `record` 按需分配，`takeDirty` 与 `MutationScratch` 交换并复用，消费完仍可保留；启动不为所有 owner 预分配位图。pending map 归零会释放 chunk 引用，但底层数组可能保留容量。source slot 数下降也不保证 SoA 数组立即缩小。`MinecraftThermalInput` 的 128 MiB `ThermalMemoryBudget` 不是整个 runtime 的 Java 堆上限，不能据此声称 owner、队列和所有索引已受总量限制。

8 chunks/tick 在 20 TPS、无容量阻塞和新增待办时，处理 1,000 个 chunk 约需 125 ticks，即 6.25 秒；这是队列推进时间，不是一次主线程阻塞，也不是全部升温完成期限。1,000 个正常 ticking 的 lit campfires 可省去每秒约 20,000 次发现错峰判断与 1,000 次发现观察；燃料计时继续。BE 密集 chunk 的临时候选集合、容量零星释放导致的重复扫描、跨 cut 启停仍需纳入采样。

恢复漏掉的有效 source 会增加应有的 Page/Brick/worker 模拟，不能将这部分增长算作发现调度退化，也不能用漏热的旧运行结果证明新方案更慢。对比应同时记录有效 source 数、模拟覆盖与输出正确性。

## 现有 owner 结构优化

### A. 删除重复身份索引

当前 `MinecraftThermalSectionAttachment` 已直接持有 owner，`ownersByIdentity` 再保存 section 到 owner 的关系。保留 `ownersBySection` 供空间查询，身份查询改读 attachment，关闭时遍历坐标索引完成解绑。预计每 24,000 个 owner 减少约 0.3–0.6 MiB，并减少 attach/unload 时的哈希操作。

实施落点是 `attachSection`、`ensureSectionOwner`、`onChunkUnload`、`onSectionIdentityReplaced` 和 `close`，不只机械删除 map。绑定前同时检查 attachment 和坐标索引：同 manager/chunk/section/key 的有效 owner 直接复用并保留 pending；真正替换时先解绑失效记录再发布新 owner。同坐标旧 chunk/section 的迟到卸载只能移除其自身 owner，坐标项与 attachment 都按实例匹配后删除。跨 runtime 的旧 owner 不能被新 runtime 复用或误清除。

### B. 将原子标记内联为字段

将 `enqueued`、`fullResync`、`deferredFullResync` 的三个独立 `AtomicBoolean` 改为 owner 内三个 `volatile boolean` 字段，使用共享静态 `VarHandle` 执行原子操作。保持三个标记独立，不增加打包状态机，不修改现有 `synchronized` 位图交换。

逐项保持现有语义：`set` 对应 volatile 写，`compareAndSet` 对应 CAS，`getAndSet(false)` 对应原子交换；尤其保留 drain 清除 enqueued 与后续 mutation 重新入队的顺序，不能用普通读写代替原子消费。24,000 个 owner 可少创建 72,000 个小对象，预计减少约 1 MiB 量级占用；精确对象布局和 CPU 收益待测。

### C. 低优先级候选与暂缓项

`PhysicalSourceSpatialIndex.observe` 可在已有 source 为 PRESENT、profile/目标/功率/enabled 均相同，且无 TARGETS_CHANGED/REGISTRATION_STALE 时提前返回成功。已有 dirty 待办必须保留，不能跳过首次注册、移除后重新出现、重定向或容量拒绝处理。该候选只节省当前观察的重复写入，现有代码已经合并 dirty 消息；仅在机器密集采样显示值得时实施。

机器继续每次正常生产发布完整输出，不增加 producer 缓存、epoch 或 change-only 协议。暂不改 mutation 位图为稀疏列表、不在每次 drain 后释放位图、不增加对象池或跨 tick BE 游标。只有实测表明它们构成热点时，再提出有对应收益的局部方案。

## 实施顺序与测量

1. **runtime 正确性修复**：完成统一接入/待办、容量恢复、公共输出与 target 释放规则，移除篝火发现轮询；保留当前 owner 存储与原子实现，先通过相关验收。
2. **owner 索引精简**：实施 A，验证重复绑定、实例替换、迟到卸载和 close。与第 1 步使用相同有效热源和模拟覆盖，记录启动与占用变化。
3. **owner 原子字段**：实施 B，验证生产/消费交错与 full resync；对比对象分配及 mutation 吞吐，保持并发语义。
4. **受控性能验证**：在同一工作区、JVM 参数和场景下顺序采样，不复制源码版本或引入路径哈希。根据结果调整内部 chunk 预算；C 仅在有收益依据时追加。

采样覆盖 loaded chunks 多而 source 少、BE 密集单 chunk、lit/unlit campfire 密集、稳定运行机器密集，以及批量 mutation/容量零星释放。记录同步 owner 接入耗时、发现 tick 耗时分布及最大值、候选数/重试次数、有效 source/Page/Brick 数、近处发现延迟、分配量、GC 和静置后的保留堆。用现有 profiling/JFR 工具作受控测量，避免新增永久逐 source/tick 日志。阶段 1 用 GameTest 证明恢复正确；阶段 2、3 用相同正确行为比较优化收益。

验收不预设未经测量的毫秒或 TPS 承诺：期望重复身份 map 和每 owner 三个独立原子对象不再存在；稳态队列空时不扫描世界；记录启动及主线程 tick 成本是否改善、有无超出测量波动的退化。位图占用单列，避免用短时分配下降冒充长期内存下降。每个阶段只运行与变化有关的检查，最终覆盖集成场景；发现退化后先定位并调整对应优化。

## 工程实施确认

已通过当前依赖的 `javap` 核对二进制公开签名，除源码追踪外，确认计划依赖的 API 确实存在：

| 所需能力 | 当前可用接口/代码边界 | 实施动作 |
|---|---|---|
| 枚举可用 chunk | `ServerChunkCache.chunkMap`、`ChunkMap.getChunks()`、`ChunkHolder.getPos()`、`getChunkNow(int,int)` | 在两个现有私有 `start` 调用点提供触发位置，启动绑定并入队 |
| 稀疏状态候选 | `ChunkAccess.getBlockEntitiesPos()` | 读取当前 BlockState；不创建 BE、不新增 NBT 解析 |
| 有序去重队列 | fastutil 8.5.9 的 `removeFirst()`、`firstLongKey()`、`putAndMoveToFirst(long,V)`、`remove(long,Object)` | 一个主线程 pending map 覆盖三种待办；卸载核对实例 |
| mutation 拒绝定位 | `SectionOwner.input()`、`chunk()`，现有主线程 `drainMutations` | boolean 结果失败时精确入队；不在异步 mutation hook 读世界 |
| 共同输出规则 | 四种 producer 已汇入 `PhysicalSourceSpatialIndex.observe` | 一处有效输出判断，复用移除/重注册与 target 更新 |
| worker 恢复与能量 | 既有 register/unload、generation、`reseedAll`、20-tick batch | 保留协议，验证公共移除路径和重新绑定 |
| 生产入口验证 | 现有 Forge GameTest 已有 runtime 创建、campfire 冷启动和 `onGeneratorTick` 场景 | 增加实际 producer 调用覆盖，不能只直接调用 facade 代替机器链验证 |

没有识别到实现所需但缺失的 API、依赖或跨线程协议。代码变更可以集中在上述四个生产类，机器逻辑和燃料/热网算法不需要为发现机制新增接口。实际机器 GameTest fixture 必须按现有 T1/T2/radiator 注册和源 ID 构造，不用测试专用生产 API。

在“保留按需 runtime、已存在机器正常 tick、保留现有 source/worker 协议”的约束下，这是当前推荐的最小复杂度实现：状态源补一次观察，计算源使用已有完整输出，公共索引统一生命周期，一个队列承接剩余工作。更快的重建可用长期输入目录换取，更少的机器索引检查可用发布 epoch/缓存换取，均增加当前不必维护的状态。

这里的确认是工程可实施和当前约束下的选型结论；不是已实现、已测优于所有负载的证明。同步 owner 启动峰值、频繁启停和每 tick 8 chunks 的预算，必须通过实施后的测量决定是否调整。

## 验收

1. 原 pre-existing campfire GameTest 在删除 `cookTick` 发现后通过，保留邻居传播断言。
2. 大量旧 chunks 排队时，触发位置附近优先观察；验证次序，不捏造升温硬期限。
3. 初始 unlit campfire、`campfirePowerW=0`、停机/零级别机器不创建 source/seed/Page；`campfireRadiationShare=1` 保留辐射来源而无对流 Page。恢复正输出可重新注册。
4. 同 Brick 多 source 共享引用：熄灭一个不释放其他 source 的 seed；最后熄灭保留合理余热并更新 dormant support。
5. 原无 source 的 section 后来放置/点燃篝火有效；重复绑定不丢 pending mutation。
6. pending BE 未实例化仍发现 lit campfire；bootstrap 不执行机器生产 tick。
7. 批量真实 Load 与启动共用预算；卸载/重载不使用旧待办；偶发重复观察不重复注册/增加 seed。
8. cap 覆盖初始扫描拒绝、已完成 chunk 的后来点 mutation/full resync 拒绝；释放后恢复。满容量不阻止旧 source 清理。
9. 完整 recipe reload 后，无新 Load/mutation 的已有篝火恢复，后续 mutation 有效；worker-only restart 不产生第二轮扫描。
10. T1/T2 generator、radiator、fountain 在 runtime 创建前已运行，创建后不修改其 BlockState/功率，由下一次实际 producer 调用恢复。generator 覆盖 townProcessedTicks 提前返回分支；验收记录 source ID、目标与额定功率换算。
11. 每种机器覆盖零输出、跨 cut 停机重启、source cap 拒绝后下一次正常输出重试、拆除与 origin chunk 卸载；确认清理 target/dormant support 且不重复消费燃料/热网。
12. 机器测试分别记录同一 runtime 的 worker-only restart，以及完整 runtime reload 的恢复；暂停机器 tick 时不宣称主动恢复，恢复有效 tick 后应重新发布。原始 section/container 替换仍正确增删状态源。
13. owner 索引精简后，重复接入保留 pending；同坐标替换后旧实例卸载不删除新 owner；旧 runtime close 不解绑新 runtime owner；close 后本 runtime 不留有效 attachment。
14. 原子字段覆盖受控的 mutation/drain 交错、消费期间再次入队、非主线程 full resync 和 section 失效。断言最终 mutation/resync 被处理且失效 owner 不影响新实例，不以时间 sleep 或内部 CAS 调用次数作为正确性依据。
15. 若实施 observe 提前返回，覆盖稳定输出、尚未 flush 的 dirty、移除后同 cut 恢复、目标/profile 变化和容量拒绝后的重试，确认没有跳过有效更新。

按用户最新要求，不新增或运行 JUnit；实施时运行 Java 编译与真实 Forge GameTest，使用实际世界、生产入口和升温/传播结果验收。一次受控采样比较 loaded-chunk 多/source 少、BE 密集、lit/unlit 密集场景的启动峰值、发现延迟、分配与稳态调用。沿用当前工程，不复制版本或引入构建哈希。

## 文档影响与结果

已更新 [runtime 架构](../docs/climate/thermal-runtime-architecture-and-optimization.md)、[物理热源](../docs/climate/heat-production-and-network.md)、[数据生命周期](../docs/climate/data-lifecycle-and-integration.md) 的启动、发现、source residency、reload 与成本。上文保留设计依据与验收清单；以下为实际执行结果。

- 实施步骤 1–3 已完成：统一 loaded-world 接入与 discovery 队列、精确容量重试、共同有效输出和 target 支持清理、删除篝火发现轮询、删除重复身份 map、原子标记内联。低优先级 observe 提前返回未实施，现有 dirty 合并继续使用。
- `runGameTestServer --offline --no-daemon --console=plain` 成功退出，25 项必需 GameTest 全部通过。没有新增或运行 JUnit；原仅测试容量标记的 `PhysicalSourceSpatialIndexTest` 已由真实 runtime 队列恢复场景取代。
- 新增 [ThermalLoadedWorldGameTests](../src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/ThermalLoadedWorldGameTests.java) 的 9 项真实世界场景：旧篝火/reload/后续点燃、容量拒绝恢复、section 替换与异步 mutation、pending BE、零功率/纯辐射，以及实际成型 T1/T2、radiator、fountain 的正常生产恢复。原篝火升温和邻居传播 GameTest 继续通过。
- 机器测试通过实际结构形成与 BE ticking 驱动；generator 覆盖 townProcessedTicks 提前返回、未重复扣燃料、跨 cut 停机/恢复和拆除。GameTestServer 缺少标准 GameProfileCache，测试夹具补齐该标准服务；生产类没有测试专用接口。
- 容量测试先等待整个初始队列完成，再压低真实索引容量，避免其他尚未发现的世界热源争用唯一空位。无需改变生产 FIFO/容量策略。
- 实施步骤 4 只完成真实测试场景的启动调用采样：缓存 profiles 的样本约 3.35–6.97 ms；包含重建 profiles 的样本约 179.53–232.69 ms，首次样本 78.81 ms。这些是整个玩法启动调用的单次值，不是 owner 接入独立耗时、TPS 指标或 before/after 优化证明。
- Remaining: 大规模 loaded chunks、BE 密集与批量 mutation 的 CPU/保留堆对比，专门的 worker 故障与实际 chunk unload/reload 压力组合尚未本轮实测；8 chunks/tick 保持初值，不宣称性能最优。以上作为后续性能/压力验证，不影响已经通过的恢复修复。
- Outcome: runtime 恢复修复与两项 owner 优化已完成并通过真实 Forge 验证；开发记录见 [diary](../diary/2026-09-08_02-28-51_thermal-loaded-world-runtime-fix.md)。
