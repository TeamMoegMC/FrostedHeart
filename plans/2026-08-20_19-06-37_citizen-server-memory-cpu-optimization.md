# Citizen 服务端内存与 CPU 优化实施方案

- Time: `2026-08-20 19:06:37 +08:00`
- Authors: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `implemented`（P1-P4 已完成；自动化验证通过，游戏内性能验收待执行）
- Scope: `Citizen 服务端热数据、调度、空间索引、AOI 与增量同步；不改变客户端渲染和玩法语义`
- Related: [`docs/town/hybrid-simulation-architecture.md`](../docs/town/hybrid-simulation-architecture.md), `CitizenSim`, `CitizenSimScheduler`, `SpatialGrid`, `BehaviorSystem`, `MovementSystem`, `SyncEngine`

## 结论

现有架构方向正确，不应重写成新的全局 ECS 或单一巨型 SoA。当前已经具备高人口模拟最重要的基础：基本类型 SoA、定点坐标、稳定 ID、swap-remove、行为分帧、共享空间网格、共享流场、AOI 上限和批量增量包。

本轮应只消除四个已确认的线性热点：

1. `CitizenSimScheduler.refreshRegistry` 每秒重新分配并重建注册集合；
2. `MovementSystem.tickAll` 和 `BehaviorSystem.tick` 每 tick 扫描总人口；
3. `SyncEngine.collectPlayerCandidates` 每个玩家分别扫描总人口；
4. `SyncEngine.flushDeltas` 每 4 tick 扫描总人口并逐居民遍历所有玩家。

推荐按 P0-P4 顺序实施，每阶段单独测量、提交和回退。生产代码不新增 Java 类，主要修改现有五个热路径类；只有 profile 证明封包分配仍是主要问题时才进入 P5。

## 目标与基准场景

目标负载：

- 单维度 `10,000` 个 Citizen；
- 其中约 `3,000` 个位于玩家活跃区；
- `1 / 2 / 4` 名玩家三档联机场景；
- AOI 半径 `96` 格，服务端可见数上限保持当前配置；
- 行为仍为每居民 `20 tick` 一次；
- 增量包仍每 `4 tick` flush，AOI 仍每 `20 tick` 刷新。

优化目标不是追逐未经实测的绝对毫秒数，而是让成本随真正需要处理的数据增长：

```text
当前
  movement       O(total citizens)
  behavior       O(total citizens)
  AOI refresh    O(players * total citizens)
  delta collect  O(players * total citizens)

目标
  movement       O(active citizens)
  behavior       O(active citizens)
  AOI refresh    O(total citizens once + nearby bucket candidates per player)
  delta collect  O(tracked unique citizens * players + tracked relationships)
                  tracked relationships 受 server-wide visibility cap 约束
```

`docs/town/hybrid-simulation-architecture.md` 中“服务端 Citizen 单 tick < 2 ms”目前仍是设计预算，不作为已经达成的事实。P0 要先在固定硬件和固定存档上建立实际基线。

## 已验证的当前状态

### 已有能力

| 子问题 | 当前实现 | 本方案处理 |
|---|---|---|
| 热数据布局 | `CitizenSim` 使用基本类型 SoA、定点数和 fastutil ID 索引 | 保留，不重写 |
| 删除与身份 | 稳定 ID + `CitizenSim.remove` swap-remove | 保留；新增热列表只能保存稳定 ID |
| 行为负载 | `BehaviorSystem` 用 `id % 20` 分片，每居民约 1 Hz | 保留频率和确定性，只缩小候选集合 |
| 邻居查询 | `SpatialGrid` 用 2 格 cell，每 5 tick 重建并复用列表 | 保留现有邻居网格；在同类中增加 AOI 粗粒度桶 |
| 寻路 | `FlowFieldCache` 共享异步流场 | 不改，不引入逐居民 A* |
| 可见性 | `SyncEngine` 有 96 格 AOI、玩家/服务器上限、稳定 Top-K | 保留选择、迟滞和服务器总上限 |
| 增量同步 | dirty 检测、距离分频、chunk 分组、240 条分包 | 保留包格式、节奏和 canonical 写回语义 |
| 持久化 | 每镇 `TownSimData` + per-level `UnmanagedCitizenData` | 保留文件格式和容器所有权 |

### 已确认热点

1. `CitizenSimScheduler.refreshRegistry`
   - 每 20 tick 新建 `ArrayList`、`Int2ObjectOpenHashMap` 和 `IntOpenHashSet`；
   - 每秒扫描并重建全部注册关系；
   - registry diff 必须保留，但集合本身可以双缓冲复用。

2. `MovementSystem.tickAll`
   - 每 tick 遍历所有容器的全部行；
   - 对每行调用 `sched.isActive`；
   - 即使只有 3,000/10,000 活跃，也仍扫描 10,000 行。

3. `BehaviorSystem.tick`
   - 每 tick 先扫描全部行，再过滤 `id % 20` 和活跃状态；
   - 决策本身已分帧，候选扫描仍与总人口成正比。

4. `SyncEngine.collectPlayerCandidates`
   - 每次 AOI 刷新为每名玩家分别遍历全部容器和全部 Citizen；
   - 复杂度为 `O(players * total population)`。

5. `SyncEngine.flushDeltas`
   - 每 4 tick 遍历全部 Citizen；
   - 每个符合表现条件的 Citizen 再遍历全部玩家计算最近距离；
   - 实际需要发包的范围已经由 `tracked` 和 server-wide cap 限制，但收集阶段没有利用该边界。

### 内存事实

`CitizenSim` 当前包含：

- `17` 个 `int[]`；
- `5` 个 `long[]`；
- `7` 个 `byte[]`。

不计数组头和 `idToIndex`，每个 capacity slot 的原始数组存储为：

```text
17 * 4 + 5 * 8 + 7 * 1 = 115 bytes/slot
```

因此源码和 living doc 中“10,000 居民热数据约 600 KB / 约 60 B 每人”的说明已经过时。一个从 64 按 2 倍扩容到 capacity `16,384` 的容器，仅基本类型数组就约为：

```text
16,384 * 115 = 1,884,160 bytes = 1.80 MiB
```

再加 `Int2IntOpenHashMap idToIndex`、数组对象头和容器运行期缓存后，约为 2 MiB 量级。该值仍然很小，但必须修正文档，不能继续使用 600 KB 作为已实现架构的证据。

整体 Citizen 内存未必由 `CitizenSim` 主导。权威 `Resident` 对象仍持有字符串、UUID、熟练度映射、营养和其他玩法数据。P0 必须用 heap dump/dominator 或等价工具确认真正的 retained heap 主体，不能根据字段数量猜测。

## 语义不变量

所有阶段必须同时满足：

- 不修改 Citizen 行为状态机、速度、路径、睡眠/起床时间或随机种子；
- 不修改 `ACTIVE_RADIUS=128`、`AOI_RADIUS=96`、`AOI_REFRESH=20`、`FLUSH_INTERVAL=4`；
- 不修改 32/64 格同步分频、240 条单包上限、玩家上限和服务器总上限；
- 不修改 packet wire format、客户端外推或渲染后端；
- 不修改 NBT 格式、稳定 ID、swap-remove 和 per-town 数据所有权；
- 有效床位上的睡眠居民继续可见，无有效床位的睡眠居民继续隐藏；
- canonical 状态只在条目成功交给至少一名玩家后，于 flush 尾部统一回写；
- 新索引和热列表保存稳定 ID，不保存会被 swap-remove 改写的运行期 row index；
- 不为本优化新增生产 Java 类；优先在现有类中增加少量字段和方法；
- 不为了测试而制造生产层抽象；无法在现有 JUnit 环境真实覆盖的服务端流程，明确列入游戏内验证。

## 目标数据流

```text
Server tick
  |
  +-- 20t: refreshActivity + refreshRegistry
  |          |
  |          +-- reuse scratch registries
  |          +-- rebuild per-container active stable-ID lists
  |
  +-- 5t:  SpatialGrid neighbor rebuild
  |
  +-- 1t:  BehaviorSystem consumes active IDs, keeps id % 20 slice
  |
  +-- 1t:  MovementSystem consumes active IDs
  |
  +-- 20t/initial: rebuild coarse visibility buckets
  |
  +-- SyncEngine
             |
             +-- AOI refresh: nearby buckets -> exact distance -> existing Top-K
             |
             +-- 4t delta: tracked union -> dirty/due -> existing packet batching
                                                  |
                                                  +-- successful handoff only
                                                       -> canonical writeback
```

两套空间用途仍然分开：

```text
SpatialGrid
  |
  +-- neighbor cells: 2-block cells
  |     active + spatialPresent only
  |     MovementSystem separation query
  |
  +-- visibility cells: initial 16-block cells
        all valid Citizen IDs, including sleepers
        SyncEngine rechecks presentationEligible + exact 96-block distance
```

粗粒度可见桶只提供候选，绝不成为权威判定。最终选择仍由 `CitizenPresence.presentationEligible`、当前坐标、精确距离、交互优先级、保留优势和现有 heap ranking 决定。

## 分阶段实施

### P0：建立服务端基线和停止条件

生产代码变化：无。

#### 场景

准备固定测试存档和固定位置：

1. 近区 3,000 个 Citizen，最终玩家位置使其落在 128 格活跃区；
2. 远区再放置 7,000 个 Citizen，保持不活跃但仍在同维度注册；
3. 分别运行 1、2、4 名玩家；
4. 记录 Java、Forge、模组版本、JVM 参数、CPU、内存、视距和存档 hash；
5. 每个场景预热至少 120 秒，采样 60 秒，重复 3 次。

可以用现有 `/fhcitizen spawn <count> <radius>` 在不同位置建立近区和远区。`/fhcitizen clear` 只删除未托管命令居民，不能拿它清理镇居民。

#### 采样

- `spark`：记录服务器 MSPT p50/p95/p99、Citizen 方法栈占比和 GC；
- JFR：记录 CPU sample、allocation sample、GC pause 和线程状态；
- `jcmd GC.class_histogram`：只在隔离测试服、统一触发 GC 后记录类数量；
- heap dump + MAT/JMC dominator：只在需要区分 `CitizenSim` 与 `Resident` retained heap 时执行；
- 网络：记录每玩家每秒 Citizen packet 条目数和字节数，避免 CPU 优化意外增加带宽。

重点锚点：

- `CitizenSimScheduler.refreshRegistry`
- `BehaviorSystem.tick`
- `MovementSystem.tickAll`
- `SpatialGrid.rebuild`
- `SyncEngine.collectPlayerCandidates`
- `SyncEngine.flushDeltas`

#### 停止条件

- 若某热点在 10k/3k 场景中低于 Citizen 服务端 CPU 的 5%，对应后续阶段先不实施；
- 若 `Resident` 是 Citizen domain retained heap 的最大 dominator，且明显大于运行期 SoA，则另立 Resident 数据方案，不在本计划中顺手重构；
- 所有后续百分比只与同一存档、同一硬件、同一 JVM 参数的 P0 数据比较。

### P1：消除周期性注册分配并修正初始容量

#### 修改点

`CitizenSimScheduler`：

- 为 `containers` 和 `byId` 各保留一个 scratch 缓冲；
- 将 `usedIds` 改为字段级复用 `IntOpenHashSet`；
- `refreshRegistry` 开始时清空 scratch，完整构建后才交换 active/scratch 引用；
- registry diff 仍比较“上一版 active byId”和“本次完整 nextById”；
- 构建中抛异常时不发布半成品 registry；下次刷新重新清空 scratch；
- 不调用 `trim()`，避免人口波动后重新分配。

伪代码：

```text
nextContainers.clear()
nextById.clear()
usedIds.clear()

build nextContainers / nextById completely
diff oldById against nextById

containers <-> nextContainers
byId      <-> nextById
```

`TownSimData`、`UnmanagedCitizenData`：

- 默认 `new CitizenSim(64)` 改为 `new CitizenSim(16)`；
- 保留 `CitizenSim` 当前最小 16 和 2 倍扩容；
- 不改加载格式和增长策略。

`CitizenSim` 与 living doc：

- 将 600 KB 旧说明改为当前 `115 B/capacity slot` 的可复核计算；
- 明确这是原始数组存储，不含 map、对象头和 `Resident`。

#### 验证

- 扩展现有 `CitizenSimPersistenceTest`，覆盖 16 初始容量、17 个条目触发增长、保存/加载和 swap-remove；
- 游戏内验证首次接管、重复 registry refresh、镇删除、跨维度和重复 ID 恢复；
- JFR 中稳定运行后的 `refreshRegistry` 不再分配新的 `ArrayList`、`Int2ObjectOpenHashMap`、`IntOpenHashSet`；
- 小镇数量固定时，GC 后 retained heap 不高于基线；容量节省与容器数量相符。

#### 回退

该阶段不改数据格式。可独立恢复初始容量和 registry 局部变量，不影响后续存档。

### P2：把移动和行为候选从总人口缩到活跃人口

#### 数据结构

在 `CitizenSimScheduler` 内维护与 `containers` 顺序对齐的复用 `IntArrayList`：

```text
containers[0] -> activeIdsByContainer[0]
containers[1] -> activeIdsByContainer[1]
...
```

每个列表保存稳定 Citizen ID，不保存 row index。列表对象复用，容器减少时放回 scheduler 内部池，不新增数据类。

#### 生命周期

- 每 20 tick 在 `refreshActivity` 和 `refreshRegistry` 完成后重建活跃 ID 列表；
- 重建时仍扫描所有行一次，并对离开活跃区的移动状态写入 `halt=1`；
- `register` 对已经登记且当前活跃的容器立即追加新 ID，保持出生当 tick 的现有语义；
- `remove` 从对应列表移除 ID，或在消费时发现 `indexOf(id) < 0` 立即清除；
- 每 tick 消费前仍用当前坐标做一次 `isActive` 防御检查；居民移动出 active cell 时立即置 `halt=1` 并从列表移除；
- 玩家移动导致 active cell 改变时，由下一次现有 20 tick activity refresh 完整重建；这与当前 activity 更新时间一致。

#### 消费方式

`MovementSystem.tickAll`：

- 按容器遍历对应 active ID 列表；
- 用 `sim.indexOf(id)` 解析当前 row，兼容 swap-remove；
- 保留 `CitizenPresence.movementIntegrated`、贴地、分离、碰撞、卡住处理和每容器一次 `markDirty`；
- 不改变处理顺序：active 列表按容器原始 row 顺序重建，新增 ID 追加到尾部。

`BehaviorSystem.tick`：

- 消费相同 active ID 列表；
- 保留 `(id % 20) == slice`，不改变居民的决策 tick；
- 暂不建立 20 份行为分片列表。只有 P2 后 profile 证明 `% 20` 候选扫描仍显著时才增加分片缓存。

#### 关键原因

不直接保存 row index，因为 `CitizenSim.remove` 会把末尾行交换到被删位置。保存 index 会导致错误居民被移动或决策；稳定 ID 每次解析多一次原始类型 hash lookup，但把扫描量从 10,000 缩到约 3,000，并保留正确性。

#### 验证

- 现有 `MovementSystemSeparationTest`、`MovementSystemCollisionTest`、`CitizenWakePolicyTest` 和 `CitizenPresencePolicyTest` 全部通过；
- 游戏内覆盖出生、删除非尾行、尾行被 swap、活跃边界进出、睡眠/起床、跨维度；
- 同一固定种子分别运行优化前后 2,000 tick，抽查状态、目标、位置、方向和 halt 变化；
- 保持 active=3,000，将 total 从 3,000 增加到 10,000，`BehaviorSystem.tick` 与 `MovementSystem.tickAll` 的 CPU 不应随 7,000 个 cold Citizen 近似线性增长；
- 若 stable-ID lookup 抵消收益，回退 P2，不改为脆弱的长期 row index。

#### 回退

恢复两个系统的容器全量循环即可。active ID 列表是纯运行期数据，不涉及存档、网络或客户端。

### P3：在现有 SpatialGrid 中增加 AOI 可见性粗网格

#### 修改点

`SpatialGrid` 保留当前 2 格 neighbor cells，并在同一个类中增加第二套 map/list pool：

- `neighborCells`：现有语义，active 且 `spatialPresent`；
- `visibilityCells`：初始 cell 边长 16 格，登记所有合法状态的 Citizen，包含睡眠居民；
- 可见性桶在 scheduler 首次 tick、每 20 tick，以及 `visibilityDirty` 时刷新；必要时可与现有遍历合并，但不得让每 5 tick 的 neighbor rebuild 被迫承担额外全量分配；
- `register`、`remove` 和 `TownSimData` 中床位/住宅出口等离散位置变化只设置 scheduler 的 dirty 标记；不新增事件类，也不在低频写路径维护第二份复杂反向索引；
- scheduled AOI refresh 时，在 behavior/movement 完成后、server-wide visibility selection 前重建，保证同 tick sleep/wake 使用当前状态和位置；
- 查询覆盖与 96 格圆相交的粗 cell，并多取一圈 halo，抵御两个 refresh 之间的有限移动；
- 查询结果写入 `SyncEngine` 的复用 `IntArrayList` scratch。

`SyncEngine.collectPlayerCandidates` 改为：

```text
coarse bucket candidates
  -> findById + sim.indexOf
  -> presentationEligible current-state check
  -> current-position exact distance <= 96
  -> existing retained advantage / interacting priority
  -> existing per-player heap
  -> existing server-wide heap
```

粗网格 cell 大小只影响候选数量，不影响最终结果。16 格是起始值，P3 profile 可在 8/16/32 中选择；不能用改变 AOI 结果换取速度。

#### 睡眠语义

可见桶必须登记睡眠居民，最终由 `CitizenPresence.presentationEligible` 判断：

- 有 `PRESENT_ON_VALID_BED`：可进入 AOI 选择；
- 没有有效床：过滤；
- 起床后使用当前坐标和状态；
- neighbor cells 仍排除睡眠居民，分离力语义不变。

halo 只负责连续移动的桶陈旧，不负责掩盖瞬移。任何可能跨越一个 visibility cell 的离散位置写入都必须设置 `visibilityDirty`，并在下一次 AOI 选择前完成全量可见桶重建。

#### 验证

- 扩展现有 `CitizenPresencePolicyTest`，覆盖 awake、有效床 sleeper、无效床 sleeper；
- 覆盖正负坐标、cell 边界、96 格内外边界、一圈 halo、重复查询不重复 ID；
- 游戏内覆盖同 tick sleep/wake、床到住宅出口、玩家快速移动、加入/退出、维度切换；
- 1/2/4 玩家场景中的 selected/tracked 集合与优化前一致；等距 ID 顺序、交互居民优先级和保留优势一致；
- 保持附近候选固定，增加 7,000 个远区 Citizen 后，AOI refresh CPU 不应近似增加 7,000 次每玩家扫描。

#### 回退

保留 neighbor grid，令 `collectPlayerCandidates` 恢复全容器扫描。没有数据迁移和 packet 变化。

### P4：以 tracked union 驱动增量 dirty 收集

#### 修改点

在 `SyncEngine` 中增加一个复用 `IntOpenHashSet trackedUnion` 和可增长复用玩家坐标数组：

1. 遍历每个玩家的 `tracked` 集合，构建本 flush 的唯一 ID union；
2. 对 union 中每个 ID 解析当前容器和 row；
3. 为保持当前语义，仍遍历全部在线玩家计算最近距离，而不是只看 tracking player；
4. 保留 `tierInterval`、`isDirty`、halt 上升沿和 `stick` 到期判断；
5. `pendingImmediate` 仍并入 `due`；
6. 按玩家现有 tracked set 组包；
7. 只把实际交给至少一名玩家的 ID 放入 `handedOffToAnyPlayer`；
8. flush 尾部统一写回 `sx/sy/sz/sdir/sstate/shalt/stick`。

目标复杂度：

```text
collect tracked union         O(sum tracked relationships)
dirty/tier evaluation         O(unique tracked IDs * players)
packet grouping               O(due tracked relationships)

unique tracked IDs <= tracked relationships <= server-wide visibility cap
```

这里不改成每玩家 canonical 状态。全局 canonical 仍保持 `O(total citizens)` 存储和当前“任一玩家成功接收后回写”的行为。

#### 验证

- 保留并扩展 `CitizenDeltaPacketBatcherTest` 的 0/1/239/240/241/480/481/1024 边界；
- 游戏内覆盖无玩家、玩家加入/退出、tracked 为空、同一 ID 被多玩家追踪、只对部分玩家 due、发送后 canonical 回写；
- 回归验证方向/状态变化不会因提前 canonical 回写而冻结；
- 回归验证 halt 上升沿仍抢发、静止 WORK 居民仍为零移动心跳；
- 在 server-wide cap 固定时，把 total 由 3,000 增至 10,000，delta collect CPU 应主要随 tracked 关系而非总人口增长；
- 每玩家 Citizen 带宽和包条目数不得高于基线噪声范围，除非修复了基线漏包。

#### 回退

恢复全人口 dirty collect。packet、tracked map 和 canonical 数组不变。

### P5：仅在 profile 证明后处理 packet allocation

当前 `flushDeltas` 会创建 chunk map、entry list、group list 和 packet entry record。这些对象的生命周期受 Forge 网络编码/调度时机约束，不能在没有证据时直接池化或发送后立即清空。

只有同时满足以下条件才进入 P5：

- P1-P4 后 JFR 显示 Citizen packet build 仍占服务端分配或 GC 的主要部分；
- 该分配对 p95/p99 MSPT 或 GC pause 有可测影响；
- 已确认 `FHNetwork.INSTANCE.sendPlayer` 之后 packet 数据何时不再被异步读取。

候选顺序：

1. 先复用仅在当前方法内、不逃逸到 packet 的 primitive scratch；
2. 再考虑用 primitive arrays 排序/分组，减少中间 map/list；
3. 最后才考虑对象池；
4. `FriendlyByteBuf` 或 packet-owned list 不得在异步编码完成前复用。

P5 不自动修改 wire format。若 profile 指向带宽而不是分配，应另写网络格式计划。

## 测试覆盖图

当前仓库使用 JUnit 5，但没有为这些类配置 Mockito 或完整 `ServerLevel` GameTest harness。本计划不为测试而新建生产抽象，也不声称纯单元测试可以证明服务端联机生命周期。

```text
CODE PATHS                                      COVERAGE

CitizenSim capacity/grow/swap/save/load
  +-- 16 initial capacity                       existing test file extension
  +-- 17th add grows and preserves arrays       existing test file extension
  +-- remove middle row updates id lookup       existing coverage + extension

SpatialGrid
  +-- neighbor query awake only                 existing JUnit
  +-- visibility includes valid sleeper         add JUnit
  +-- negative/boundary/halo cells              add JUnit

Scheduler registry + active IDs
  +-- first/repeated refresh                     in-game integration
  +-- register/remove/swap                       JUnit where pure + in-game
  +-- active -> cold sets halt                   in-game deterministic capture
  +-- dimension/container removal               in-game integration

SyncEngine
  +-- packet 240-entry boundaries               existing JUnit
  +-- AOI exact selected set                     in-game 1/2/4-player comparison
  +-- tracked union + canonical writeback        in-game packet/state comparison
  +-- player leave / no players                  in-game integration

Performance
  +-- 10k total / 3k active                      spark + JFR, three repetitions
  +-- cold population scaling                    3k vs 10k total
  +-- retained heap                              histogram, dominator if needed
```

实施时运行：

```powershell
.\gradlew.bat test --console=plain
```

每阶段还应运行对应 focused tests。无法自动化的服务端场景必须在 diary 中写明操作、观察结果和未覆盖风险，不能写成“测试通过”。

## 性能验收门

所有比较使用 P0 同一场景，先看三次运行分布，不用单次最好值。

| 指标 | 验收门 |
|---|---|
| 正确性 | 自动测试通过；状态、位置、睡眠可见性、tracked/canonical 和 packet 边界无回归 |
| Registry allocation | warm-up 后 `refreshRegistry` 不再持续分配三种重建集合；若仍有分配，给出 JFR 调用栈 |
| Cold scaling | active=3k 固定时，total 3k -> 10k 不再使 movement/behavior 成本近似线性增长 |
| AOI scaling | 附近候选固定时，远区 cold Citizen 不再按 `players * total` 增加 candidate scan |
| Delta scaling | server-wide cap 固定时，dirty collect 主要随 tracked 关系增长，而不是 total population |
| Server tick | 相同负载下 p95/p99 MSPT 不得回退超过 5%；热点阶段收益应超过重复采样噪声 |
| GC | allocation rate 和 GC pause 不得回退；P1 应消除每秒 registry collection churn |
| Memory | GC 后 retained heap 按容器数/容量解释；不得只用进程 RSS 判断 Java 对象泄漏 |
| Network | packet 条目数和每玩家字节数不因 CPU 优化无故增加 |

若某阶段热点收益低于 10% 且增加了明显维护复杂度，默认回退该阶段。正确性优化例外，但必须在 outcome 中说明。

## 故障模式与防护

| 故障 | 影响 | 防护与验证 |
|---|---|---|
| registry scratch 构建一半就发布 | 查找缺失、错误 despawn | 完整构建后交换；异常保留旧 active registry |
| active list 保存 row index | swap-remove 后驱动错误居民 | 只保存稳定 ID；消费时 `indexOf` 重解析 |
| 删除后 active list 留旧 ID | 无效查找或重复工作 | `remove` 清理；消费时 `indexOf < 0` 自愈 |
| 离开 active cell 未写 halt | 客户端继续外推 | 当 tick 防御检查并置 `halt=1`，验证 halt 抢发 |
| visibility grid 排除 sleeper | 远近睡眠居民消失 | 可见桶包含 sleeper；最终按有效床 flag 过滤 |
| 粗桶边界漏候选 | 96 格内居民不 spawn | halo + 当前坐标精确过滤；负坐标和边界测试 |
| 床位/出口离散换位跨粗桶 | 同 tick wake 后漏选 | 离散位置写入设置 `visibilityDirty`；选择前重建 |
| 粗桶出现陈旧 ID | lookup 空值或错误人 | stable ID 解析失败即跳过；refresh/register/remove 清理 |
| tracked union 只看 tracking player 距离 | 改变远玩家更新节奏 | union 后仍对全部在线玩家求当前最小距离 |
| canonical 在组包前更新 | 方向/状态永久不再 dirty | 仅 `handedOffToAnyPlayer` 在 flush 尾部写回 |
| 发送后立即复用 packet list | 异步编码读到被清空数据 | P5 前禁止复用逃逸对象；先确认网络生命周期 |
| 只看 RSS 判断内存 | 把 JVM committed heap 当泄漏 | GC 后 histogram/dominator 与 allocation JFR 组合判断 |

## 实施顺序与回滚

所有阶段都触及同一 Citizen 服务端模块，顺序实施，没有有价值的 worktree 并行机会：

```text
P0 baseline
  -> P1 registry/capacity
  -> profile gate
  -> P2 active iteration
  -> profile gate
  -> P3 AOI buckets
  -> profile gate
  -> P4 tracked-union delta
  -> profile gate
  -> optional P5 packet allocation
```

每个阶段使用独立提交，并保存同一套 before/after 结果。由于 P1-P4 都不改 NBT 和 packet 格式，回滚只需要恢复对应运行期算法，不需要迁移存档或踢出客户端。

## 不在本计划范围

- 全局 archetype SoA：将 active-moving、active-standing、sleeping、cold 拆成 per-level 大表，理论吞吐更高，但会引入跨表迁移、镇所有权、持久化和维度生命周期复杂度；当前 10k/3k 目标不值得。
- 将全部镇合并为一个 `CitizenSim`：会破坏当前每镇所有权和独立 dirty/persistence 边界。
- off-heap、`ByteBuffer` 或 unsafe 内存：增加生命周期和调试成本，当前规模没有证据支持。
- 进一步位打包 `CitizenSim` 字段：年龄镜像已经占现有 byte 的 2 bit，不是内存热点；继续压缩会降低可维护性。
- 稀疏 canonical 状态：当前数组随 SoA capacity 连续存储，改成 map 会增加查找和对象开销。
- 每玩家 canonical 状态：会把内存从 `O(citizens)` 提升到 `O(citizens * players)`，不符合当前取舍。
- 无证据的 packet/FriendlyByteBuf 对象池：异步编码生命周期不清楚时存在数据破坏风险。
- `Resident` 权威对象重构：先由 P0 dominator 证明；若确为主导，另立跨营养、职业、交易、持久化的方案。
- 客户端 Flywheel、Oculus、Body/Billboard 和 FakeEntity：本计划只处理服务端模拟与同步候选。
- 修改玩法参数、可见距离、活跃半径或人数上限：不能用减少功能掩盖算法成本。

## 文档与完成记录

实现每个阶段后：

1. 更新本计划状态和阶段 outcome；
2. 更新 `docs/town/hybrid-simulation-architecture.md` 的真实数据流、复杂度和内存估算；
3. 若没有改变当前行为，也明确记录“仅算法/分配变化，玩法语义和 packet/NBT 合同未变”；
4. 在 `diary/` 新增完成记录，包含 commit、测试命令、spark/JFR 场景、before/after、回退决定和剩余工作；
5. 不把原始 heap dump、JFR 大文件或私人服务器信息提交到仓库。

## Outcome

`implemented`。2026-08-20 已在一个实现批次中完成 P1-P4，生产代码没有新增 Java 类：

- `CitizenSimScheduler.refreshRegistry` 复用双缓冲 registry 与重复 ID scratch，并将每镇/未托管容器初始 `CitizenSim` 容量降为 16；
- scheduler 维护按容器对齐的活跃稳定 ID 列表，`BehaviorSystem` 与 `MovementSystem` 不再逐 tick 扫描非活跃行；
- `SpatialGrid` 增加复用的 16 格 visibility cells，AOI 以粗桶生成候选后继续执行原有当前状态、有效床和精确 96 格距离判定；
- `SyncEngine.flushDeltas` 以 tracked union 驱动 dirty 检查并复用玩家坐标数组，最近距离、分频、per-player 组包及 canonical 尾部回写语义保持不变；
- `docs/town/hybrid-simulation-architecture.md` 已同步当前数据流、115 B/capacity slot 内存口径及对象池限制。

统一执行 `./gradlew.bat test --console=plain`，结果为 `BUILD SUCCESSFUL in 39s`；`git diff --check` 通过，仅输出仓库的 LF/CRLF 转换提示。未执行游戏内 10k/3k 场景、spark、JFR、class histogram 或 heap dominator 验收，因此本文不声称已达到性能表中的毫秒数、比例或 retained-heap 目标。P5 仍由 profile 门控，没有实施，也没有修改 NBT、packet wire format、配置默认值或玩法参数。
