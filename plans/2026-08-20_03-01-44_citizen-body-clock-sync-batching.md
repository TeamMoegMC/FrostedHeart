# Citizen Body 抽动与大批量同步修复计划

- Time: `2026-08-20 03:01:44 +08:00`
- Authors: `Codex (OpenAI GPT-5; investigation and implementation-plan author)`
- Status: `completed`
- Scope: `Citizen Flywheel Body snapshot/yaw timing, SyncEngine delta batching, focused regression coverage, town living documentation`
- Related: [`docs/town/citizen-rendering-at-scale.md`](../docs/town/citizen-rendering-at-scale.md), [`docs/town/hybrid-simulation-architecture.md`](../docs/town/hybrid-simulation-architecture.md), `FlywheelCitizenBackend`, `citizen.vert`, `SyncEngine.flushDeltas`, `S2CCitizenBatchPacket`, `FHConfig.SERVER.TOWN.maxVisibleCitizensPerPlayer`

## Goal

修复两个会被描述为“Citizen 客户端跟不上”的独立问题，并避免用一个修复掩盖另一个问题：

1. Flywheel `Body` 渲染必须和 shader 使用同一动画时钟，使每个位置和 yaw 段从 `blend=0` 开始，不在第一帧预跳。
2. 真实服务端同步在单玩家追踪超过 `240` 个到期居民时，必须发送完整的多包序列，不能截断后把未发送居民记为已发送。

本计划不修改服务端移动规律、客户端插值公式、shader 动画公式、包内编码或 `240` 条的单包上限。

## 结论先行

在当前复现场景 `/citizen_debug benchmark load 1024 moving` 中，**不是客户端算不过来，也不是服务端同步落后**。该基准由 `CitizenClientBenchmark` 直接在客户端缓存中创建和更新居民，不经过服务端、网络包或 `SyncEngine`。当前 Body 抽动的直接原因是 Citizen Flywheel backend 把 Create 的动画时钟写入实例数据，而 shader 的 `uTime` 使用 Flywheel 自己的动画时钟。

真实服务器上另有一个可独立触发跳动、冻结或延迟追赶的同步缺陷：`SyncEngine.flushDeltas` 对每名玩家装入 `240` 条后 `break`，却在 flush 末尾推进全部 `due` ID 的规范状态。默认可见上限已提高到每玩家 `1024`，所以这个旧的隐含上限现在会被正常配置触发。

| 现象 | 是否涉及服务端 | 直接原因 | 修复单元 |
|---|---:|---|---|
| `1024 moving` 客户端 benchmark 中 Flywheel Body 抽动 | 否 | Create/Flywheel 时钟域错配 | A. Flywheel 时钟统一 |
| 真实人口超过 240 时部分居民长时间不更新 | 是 | 单 flush 截断，但未发送 ID 仍推进规范状态 | B. Sync 多包与提交语义 |

## Pre-fix Evidence (Historical)

### A. Flywheel backend 使用了 shader 之外的时钟

证据链，置信度 `10/10`：

- 修复前的 `FlywheelCitizenBackend` 导入
  `com.simibubi.create.foundation.utility.AnimationTickHolder`。
- `FlywheelCitizenBackend.writeSnapshot` 用该类的 `getRenderTime()` 写入 `snapshotTime` 和 `yawTime`。
- `citizen.vert` 用 Flywheel program 的全局 `uTime` 计算 `uTime - snapshotTime` 和 `uTime - yawTime`。
- Flywheel `0.6.11-13` 的 `WorldProgram`/动画工具使用 `com.jozufozu.flywheel.util.AnimationTickHolder` 上传该时间。Create 和 Flywheel 的同名类各自维护独立静态 tick 字段，不是别名。
- 运行时 tick 顺序中，Flywheel 在客户端 tick `START` 推进；Citizen 在 `END` 写快照；Create 也在 `END` 推进，并且当前订阅顺序晚于 Citizen。Citizen 因此可能读取上一 tick 的 Create 值，而 shader 已读取当前 Flywheel 值。

当前链路：

```text
Client tick START
  Flywheel clock += 1
          |
          |                         shader render
          |                         uTime = Flywheel clock
          |                                  |
Client tick END                              v
  CitizenClientBenchmark.update()      elapsed = uTime - start
  FlywheelCitizenBackend.writeSnapshot()
          |
          +-- start = Create clock (still previous tick)
          |
  Create clock += 1
```

对 4 tick 的 benchmark 段，若 shader 时钟比写入时钟领先 1 tick：

```text
first-frame position blend = 1 / 4 = 25%

large-turn first-frame yaw travel
  = 12 yaw units
  = 12 / 256 * 360 degrees
  = 16.875 degrees
```

这正好会让 Body 在每次 4 tick 更新和方向反转时表现为短促抽动。帧率下降可能让症状更显眼，但不是根因。

### B. Sync 把未装包居民当成已发送

证据链，置信度 `10/10`：

- `SyncEngine.MAX_ENTRIES_PER_PACKET = 240` 的注释定义的是单包上限。
- `flushDeltas` 当前为每名玩家创建一个 `byChunk`，`count` 达到 `240` 后直接退出该玩家的 tracked-set 遍历。
- 每名玩家随后最多调用一次 `sendPlayer(...S2CCitizenBatchPacket)`。
- 玩家循环结束后，代码遍历全部 `due`，回写 `sx/sy/sz/sdir/sstate/shalt/stick`，没有区分是否进入任何包。
- `FHConfig` 当前默认 `maxVisibleCitizensPerPlayer=1024`、`maxVisibleCitizensPerServer=8192`，正常配置已经允许一个玩家追踪远多于 240 名居民。
- 当前 Citizen 测试目录没有 `SyncEngine`、`MAX_ENTRIES_PER_PACKET` 或 `S2CCitizenBatchPacket` 分包边界测试。

以 1024 个 tracked 且全部 due 的居民为例：

```text
due = 1024
   |
   v
current player pack loop
   +-- packed and sent: 240
   +-- skipped by break: 784
   |
   v
canonical writeback over all due: 1024

Result: 784 entries were not sent, but their next dirty check starts from
        the new server state as if they had been sent.
```

这些客户端对象只能等下一次心跳、状态变化或 Dead Reckoning 误差再次越界才被纠正，因此真实场景会出现冻结、突然追赶或群体更新不均匀。

## Decisions

### 1. Flywheel clock is the sole citizen instancing animation clock

`FlywheelCitizenBackend` 改用 `com.jozufozu.flywheel.util.AnimationTickHolder`。`snapshotTime`、`yawTime`、shader `uTime` 和 `ANIMATION_PERIOD_TICKS` 必须属于同一时钟域。

不通过增加负偏移、把 duration 拉长或削弱 yaw 速度来遮掩一 tick 差值。这些做法只能降低肉眼幅度，无法保证段起点为零，还会把正常动画调慢。

实现处应留一句短注释，写明必须与 Flywheel shader `uTime` 同源。Create 与 Flywheel 的类同名，这是值得显式防回归的依赖边界。

### 2. `240` remains a per-packet limit

保留包协议和 `MAX_ENTRIES_PER_PACKET=240`。一个 flush 可以为同一玩家发送多个 `S2CCitizenBatchPacket`：

```text
1024 due entries
   |
   +-- packet 1: 240
   +-- packet 2: 240
   +-- packet 3: 240
   +-- packet 4: 240
   +-- packet 5:  64
```

不把上限直接改成 1024。保留小包可以控制单次编码缓冲、解码工作和 Netty frame 大小，也不改变已有 wire format。

### 3. Canonical state follows network handoff, not `due`

本修复采用以下明确语义：

> 对某个 citizen ID，只有包含它的 packet 调用 `FHNetwork.INSTANCE.sendPlayer` 并正常返回，才把它加入本 flush 的 `handedOffToAnyPlayer`；flush 末尾只推进该集合的共享 canonical 状态。

这里的“发送”只表示本地 Forge network API 调用正常返回，不表示新增应用层 ACK。底层连接的可靠传输仍由现有网络栈负责。

同一 ID 被多名玩家追踪时，每名玩家各收到一次；`handedOffToAnyPlayer` 用 set 合并，并只推进一次共享 canonical。没有任何当前玩家收到的 due ID 不得推进。

### 4. Packing and canonical commit stay visibly separated

新增 package-private 纯分包器 `CitizenDeltaPacketBatcher`，输入现有 `S2CCitizenBatchPacket.Group` 列表与条目上限，通过 callback 逐个产出 packet group 列表。它必须能在**同一个 chunk group 内部**切分，不能只在 group 边界切分；未切分 group 复用原对象，只有跨界片段复制条目引用。

`SyncEngine` 保留权限、追踪、容器反查和状态读取；纯分包器只负责以下不变量：

- 每个输出 packet 的 entry 数 `<= limit`。
- 每个输入 entry 在输出中出现且只出现一次。
- chunk 坐标和值保持不变。
- 空输入不产生空包。

`SyncEngine.flushDeltas` 附近应加入这张短 ASCII 注释，因为“发送后才能推进 canonical”的顺序是历史上已经回归过的非显然约束：

```text
due -> per-player filtered groups -> <=240 packet batches -> send
                                                        |
                                                        v
                                               handedOffToAnyPlayer
                                                        |
                                                        v
                                             canonical writeback
```

## What Already Exists

| Existing code/flow | Reuse decision |
|---|---|
| `ClientCitizen` 的双快照、秒制 `t0/t1`、连续位置起点与 1.5 秒外推 | 原样复用；不是这次抖动的根因 |
| `citizen.vert.citizenElapsed` 与 `ANIMATION_PERIOD_TICKS=1_728_000` 回绕 | 原样复用；只修正写入时钟来源 |
| `FlywheelCitizenBackendTest.animationClockWrapAndYawDeltaUseShortestPaths` | 保留，并补“时钟类身份”和“段首 elapsed=0”覆盖 |
| `S2CCitizenBatchPacket.Group/Entry` 和 chunk-local 量化 | 原样复用；不改编码、解码或包注册 |
| `SyncEngine.due`、per-player `tracked`、共享 canonical 数组 | 原样复用；新增 packet partition 与 sent set |
| `pendingImmediate` 合入 `due` 的 sleep/wake 快照 | 保留；测试它即使落在第 2 个及以后 packet 也不会被截断 |
| `CitizenClientBenchmark` 的 4 tick 确定性 moving 更新 | 继续作为时钟修复的首要实机回归场景；明确不能用于验证服务端分包 |
| `CitizenRenderMetrics` 的实例数量和 dirty bytes | 继续验证修复没有引入每帧全量重写 |

本计划不重建插值系统、网络协议或 per-player canonical。现有基础足以用一个 import 级时钟修复和一个小型纯分包器完成目标。

## Target Data Flows

### Flywheel snapshot timing after fix

```text
ClientCitizen snapshot (game-time seconds)
          |
          v
FlywheelCitizenBackend.writeSnapshot
  animationNow = Flywheel AnimationTickHolder.getRenderTime()
  snapshotTime = animationNow + (snapshotStart - gameNow) * 20
  yawTime      = animationNow
          |
          v
CitizenInstanceData timing/yaw
          |
          v
citizen.vert
  uTime = same Flywheel clock
  first frame: elapsed ~= 0
```

### Delta batching after fix

```text
all eligible simulation rows
          |
          v
dirty + tier-due IDs
          |
          +-------------------------------+
          | for each player               |
          v                               |
tracked + still eligible + resolvable     |
          |                               |
          v                               |
chunk-grouped entries                     |
          |                               |
          v                               |
partition at 240, including group splits  |
          |                               |
          v                               |
send packet 1..N -------------------------+
          |
          v
union IDs from locally handed-off packets
          |
          v
advance shared canonical once per ID
```

## Implementation Steps

### Unit A: Flywheel clock unification

1. Add a failing regression assertion proving compiled `FlywheelCitizenBackend` references `com.jozufozu.flywheel.util.AnimationTickHolder` and does not reference Create's same-named holder. A small classfile dependency assertion is acceptable here because it tests the exact architectural boundary without ticking a live `Minecraft` singleton.
2. Add or extract a pure timing assertion for a snapshot starting “now”: when the CPU sample and shader time are the same value, `citizenElapsed(start)` is zero and a 4-tick segment begins at `blend=0`, including period wrap.
3. Replace the Create import in `FlywheelCitizenBackend` with Flywheel's holder. Do not change the timing formula, duration clamp, yaw rates, instance format or shader.
4. Run the focused Flywheel test, then compare Flywheel and CPU rendering in the existing 1024 moving benchmark.

### Unit B: Sync batching correctness

1. Add `CitizenDeltaPacketBatcher` as a package-private pure helper under `citizen/sync`. Keep it independent of `ServerPlayer`, `CitizenSimScheduler` and `FHNetwork` so packet-boundary behavior is deterministic and cheap to test.
2. Remove the `count >= 240 -> break` truncation from the logical set of entries for each player. Build all valid tracked-due groups, then partition them into packets of at most 240 entries.
3. Send every non-empty partition for that player. For `1024` entries the exact sizes must be `240, 240, 240, 240, 64`.
4. Maintain a reused `handedOffToAnyPlayer` set in `SyncEngine`. Add batch IDs only after `sendPlayer` returns normally. Replace the final `for (id : due)` canonical writeback with `for (id : handedOffToAnyPlayer)`.
5. Preserve defensive `id -> container -> index` re-resolution during both packing and canonical writeback. Removed, hidden or no-longer-eligible rows are skipped and never committed as sent.
6. Keep current `pendingImmediate` ownership and clear timing unless a failing test demonstrates a separate retry bug. Required coverage is that immediate IDs beyond the first packet are packed, not that the project gains an application-level retransmission queue.

### Shared closeout

1. Run focused tests for Flywheel timing and sync batching.
2. Run the full Gradle test suite.
3. Execute Flywheel visual/lifecycle regression and a real server-backed `>240` population smoke test.
4. Update both town living documents with implemented behavior and exact class anchors.
5. Add a new diary entry with implementation decisions, focused/full test results, manual evidence and remaining work.
6. Mark this plan `completed` and fill `Outcome`; if implementation changes the delivery rule, update `Decisions` before marking complete.

## Test Review

Current and planned coverage map:

```text
CODE PATHS                                      OBSERVABLE FLOWS
[~] FlywheelCitizenBackend.writeSnapshot       [+] 1024 client-only moving benchmark
 |-- [GAP, CRITICAL] clock class identity        |-- [GAP] Flywheel starts/reversals do not twitch
 |-- [PLANNED] start-now -> elapsed 0             |-- [GAP] Flywheel vs CPU cadence/turn comparison
 |-- [TESTED] period wrap                         |-- [GAP] F3+T keeps timing smooth
 `-- [TESTED] shortest yaw delta                  `-- [GAP] dimension/backend switch smoke

[!] SyncEngine.flushDeltas                      [+] Real server-backed >240 tracked residents
 |-- [GAP, CRITICAL] 0/1/239/240/241 boundaries  |-- [GAP] client cache receives all residents
 |-- [GAP, CRITICAL] 1024 -> 5 packets            |-- [GAP] no frozen 241st+ residents
 |-- [GAP] one chunk split across packets         `-- [GAP] no starvation across flushes
 |-- [GAP] many chunks preserve exact IDs
 |-- [GAP] per-player exact-once coverage
 |-- [GAP] pendingImmediate after entry 240
 |-- [GAP] unresolved/hidden row is skipped
 `-- [GAP, CRITICAL] canonical only for sent IDs

LEGEND: [TESTED] existing automated coverage
        [PLANNED] focused automated regression added by this work
        [GAP] currently absent and required before completion
        [!] known faulty path
```

Required automated cases:

| Test | Input | Assertion |
|---|---|---|
| Flywheel clock dependency | compiled backend class | references Flywheel holder; no Create holder reference |
| Segment start | same animation time for writer/shader, duration 4 | elapsed `0`, blend `0` |
| Wrapped segment start | writer/shader near `1_728_000` wrap | elapsed remains continuous and starts at `0` |
| Empty partition | 0 entries | 0 packets |
| Packet boundaries | 1, 239, 240, 241, 480, 481 entries | packet sizes correct; none exceeds 240 |
| Default-cap partition | 1024 entries | sizes exactly `240,240,240,240,64` |
| Single-chunk overflow | 481 entries in one chunk | group is split across 3 packets without loss |
| Multi-chunk boundary | groups crossing entry 240 | coordinates preserved; no duplicate/missing IDs |
| Multiple players | overlapping and disjoint tracked sets | exact once per player; canonical union once per ID |
| No recipient | due ID not tracked by any player | no packet and no sent-canonical commit |
| Immediate ordering | immediate ID appears after 240 normal entries | included in a later packet in the same flush |
| Invalidated row | `findById`/`indexOf` no longer resolves | omitted from packet and sent set |

Focused command target after implementation:

```powershell
.\gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.FlywheelCitizenBackendTest --tests com.teammoeg.frostedheart.content.town.citizen.sync.CitizenDeltaPacketBatcherTest
```

Then run:

```powershell
.\gradlew.bat test
```

## Failure Modes

| Failure mode | Prevention/handling | Automated test | User/operator visibility |
|---|---|---:|---|
| Future import auto-completes Create's same-named clock again | compiled dependency assertion plus explicit source comment | Yes | Without test it is silent visual twitch |
| CPU/shader times differ at period wrap | retain shared period and wrap tests | Yes | Visible as a rare jump without coverage |
| One chunk alone contains more than 240 entries | batcher splits inside a group | Yes | Otherwise encoder would exceed contract or drop tail silently |
| Entry duplicated or omitted when a group crosses packets | exact ID multiset assertions | Yes | Otherwise only some residents freeze; silent |
| Player A and B track different sets | build and send per player; union only canonical commit IDs | Yes | Otherwise one player can remain stale; silent |
| A due ID resolves to a removed/hidden row | existing re-resolution and eligibility guards | Yes | Expected disappearance; no packet |
| `sendPlayer` throws during a packet loop | do not catch and pretend success; canonical commit must not include an unreturned call | Focused fault injection if network wrapper is cheaply injectable; otherwise integration | Server error is logged/raised, not silent |
| `pendingImmediate` lands after the first 240 entries | multi-packet drain includes the tail | Yes | Otherwise sleep/wake appears delayed; silent |
| Worst-case all 1024 residents are due every 4 ticks | preserve per-packet limit, measure real traffic, retain configurable visibility cap | Size/count test plus manual metrics | Bandwidth increase is observable; correctness is not traded for silent loss |
| Flywheel fix regresses lifecycle rebuild | retain F3+T, origin, dimension and backend-switch tests | Existing automated plus manual | Existing backend status/logging reports fallback |

After the planned tests, no listed silent production failure remains without either automated coverage or an explicit visible error path. Before implementation, the clock identity and canonical commit rows are critical gaps.

## Performance and Bandwidth

### Render path

Changing the imported clock does not alter instance stride, dirty frequency, draw batches, shader instruction count or allocation behavior. Expected Flywheel metrics remain:

- `1024 moving` with 64 detailed entities: `960 * 58 = 55,680 B` dirty on each 4-tick snapshot and `0 B` between snapshots.
- Instance ownership counts, `batchDraws`, light samples and rebuild peaks remain unchanged.
- The fix should reduce visible discontinuity without changing throughput.

### Network path

`S2CCitizenBatchPacket.Entry` remains 6-8 bytes plus chunk and packet headers.

| Scenario | Packets per flush | Raw entry bytes per flush |
|---|---:|---:|
| 240 due | 1 | about 1.4-1.9 KiB |
| 1024 due | 5 | about 6-8 KiB |
| 8 players x 1024 due, server relationship cap 8192 | 40 | about 48-64 KiB server-wide |

At the nearest 4-tick tier, the absolute worst case repeats five times per second: about 30-40 KiB/s per fully saturated player, or 240-320 KiB/s at 8192 simultaneously due relationships, before headers. Normal Dead Reckoning should make only a subset dirty; the worst case is still the correct capacity envelope for the configured default.

This is an intentional correction: the server already promises up to 1024 tracked residents per player. If measured traffic is unacceptable, the follow-up must explicitly change visibility, cadence or introduce a fair queued budget with per-player state. Silent truncation is not a valid bandwidth policy.

The batcher streams at most one current packet payload list at a time. Unsplit chunk groups reuse their existing entry lists; only groups crossing a 240-entry boundary allocate copied slice lists. For `N=1024`, five packets are emitted without retaining a five-packet outer collection. Do not add an async worker or persistent retransmission queue for this fix.

## Acceptance Criteria

### Automated

- Focused Flywheel and batcher tests pass.
- Full `gradlew test` passes with no new failures.
- All packet-boundary tests prove `entryCount <= 240` and exact-once ID coverage.
- A 1024-entry input produces `240,240,240,240,64`.
- Canonical advancement is based on sent packet IDs, never the full `due` set.
- Existing instance layout remains `58 B`; shader resource, yaw wrap, LOD, coordinator fallback and lifecycle tests remain green.

### Client visual regression

1. Select Flywheel and load `/citizen_debug benchmark load 1024 moving` in the same fixed camera setup used by prior Flywheel validation.
2. Observe at least two full benchmark direction-reversal cycles. Body position and facing must start each segment continuously, with no rhythmic 4-tick twitch or first-frame yaw snap.
3. Switch to `cpu_batch` without moving the camera and compare movement/turn cadence, then switch back to Flywheel. Ownership counts must remain valid and no duplicate or empty frame may appear.
4. Repeat after F3+T, Flywheel renderer reload, an origin boundary crossing and a dimension round trip.
5. Record `/citizen_debug metrics`; dirty-byte cadence and ownership counts must match the pre-fix contract.

### Real server synchronization

1. Use a real server-backed town/save with more than 240 presentation-eligible tracked residents. Do not use `CitizenClientBenchmark`, because it bypasses networking.
2. Trigger a simultaneous moving or sleep/wake update large enough to cross packet boundaries.
3. Confirm the client cache reaches the expected tracked count and residents after index 240 update in the same flush window rather than freezing until a later heartbeat.
4. Repeat with two players whose tracked sets overlap only partially. Neither player may starve, and disconnecting one player must not affect the other's updates.
5. Capture packet counts or temporary debug logging for one 1024-due flush and verify five packets with sizes `240,240,240,240,64`. Remove temporary logging before completion unless it is made an intentionally bounded debug metric.

## NOT in Scope

- Rewriting `ClientCitizen` interpolation/extrapolation: current client-only benchmark proves the Body issue occurs after that state is already present.
- Tuning shader turn rates, walk cadence or segment duration: these values are not the clock-domain defect.
- Changing `S2CCitizenBatchPacket` wire format or raising the 240-entry limit: multi-packet drain solves correctness while preserving compatibility.
- Adding application-level ACKs, retransmission queues or per-player canonical arrays: Forge's existing reliable connection and the shared canonical model remain the project contract.
- Lowering `maxVisibleCitizensPerPlayer` to hide the overflow: the default 1024 capacity must work as configured.
- Making the client-only benchmark send server packets: it remains an isolated rendering benchmark.
- Flywheel default enablement, Oculus policy, GPU profiling, new LODs, atlases or shadows: those remain separate rendering acceptance work.
- Server movement, pathfinding, behavior scheduling or town balance changes: neither root cause originates there.
- Any change under `design/`: this is an implementation defect plan, not a lore or creative-design revision.

## Documentation Impact

After implementation, not before:

- Update `docs/town/citizen-rendering-at-scale.md` to name the fully qualified Flywheel clock, record the clock-domain regression and add the no-first-frame-jump validation contract.
- Update `docs/town/hybrid-simulation-architecture.md` so `240` is documented as a per-packet limit, describe multi-packet flush behavior, and define canonical advancement as successful local network handoff.
- Add a new timestamped `diary/` entry covering both independently shipped units, tests, real-client evidence and any remaining manual matrix.
- Mark this plan `completed` and fill the outcome with exact files, commands and results. If only one unit ships, keep status `in-progress` and state which unit remains.

## Worktree Parallelization

| Step | Modules touched | Depends on |
|---|---|---|
| A. Flywheel clock and regression | `citizen/client`, client tests | - |
| B. Delta batcher, sent-union commit and regression | `citizen/sync`, sync tests | - |
| C. Full suite and real-client/server validation | client + sync runtime | A and B |
| D. Living docs, diary and plan outcome | `docs/town`, `diary`, `plans` | C |

Parallel lanes:

```text
Lane A: Flywheel clock test -> clock import fix
Lane B: packet boundary tests -> pure batcher -> SyncEngine sent-union commit
                          |
                          +-- merge A + B
                                  |
Lane C: full tests -> visual/server validation -> docs/diary/outcome
```

Lanes A and B can run in parallel worktrees because they touch different packages and tests. Lane C is sequential after both. Do not let A and B independently edit the two shared town documents; defer documentation to Lane C to avoid conflicts.

## Outcome

Implementation completed on `2026-08-20` in the existing Citizen worktree.

- Unit A: `FlywheelCitizenBackend` now imports
  `com.jozufozu.flywheel.util.AnimationTickHolder`, so instance snapshot/yaw
  timestamps and shader `uTime` use one Flywheel clock domain. The classfile
  dependency regression test prevents a future reintroduction of Create's
  same-named clock.
- Unit B: `CitizenDeltaPacketBatcher.forEachPacket` streams all per-player delta
  groups at the existing `SyncEngine.MAX_ENTRIES_PER_PACKET = 240` limit,
  including splitting a single chunk group. Unsplit groups are reused and only
  boundary fragments copy entry references. `SyncEngine` sends every emitted
  packet and only advances the shared canonical state for IDs included in a
  packet after `FHNetwork.INSTANCE.sendPlayer` returns normally.
- Regression coverage: `FlywheelCitizenBackendTest` (11 tests) and
  `CitizenDeltaPacketBatcherTest` (5 tests) pass. The focused Gradle command
  completed successfully. The full `./gradlew.bat test --console=plain` run
  completed successfully with 72 suites / 252 tests / 0 failures / 0 errors /
  0 skipped.
- Living documentation was updated in
  `docs/town/citizen-rendering-at-scale.md` and
  `docs/town/hybrid-simulation-architecture.md`; this plan is the implementation
  record for the two independent root causes.

Remaining validation is manual and does not block the source fix: run the
`1024 moving` Flywheel-vs-CPU visual matrix (direction reversal, F3+T, origin
crossing, dimension round trip and Flywheel reload) and a real server-backed
town with more than 240 tracked residents. In the latter, capture one flush's
packet sizes and confirm the expected `240,240,240,240,64` sequence and client
updates after entry 240. No application-level ACK or retransmission queue was
added; `sendPlayer` success means local network handoff only.

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|---|---|---|---:|---|---|
| CEO Review | `/plan-ceo-review` | Scope and strategy | 0 | Not run | Bug fix does not change product scope |
| Codex Review | `/codex review` | Independent second opinion | 0 | Not run | No outside-model review requested |
| Eng Review | `/plan-eng-review` | Architecture and tests | 0 | Blocked | Current host mode has no callable interactive decision tool; non-interactive fallback covered architecture, code quality, tests, performance, failure modes and parallelization |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | Not run | No UI layout or interaction design change |
| DX Review | `/plan-devex-review` | Developer experience gaps | 0 | Not run | No public developer workflow change |

- **UNRESOLVED:** `0` implementation decisions in this document; formal interactive gstack approval was unavailable.
- **VERDICT:** Plan is implementation-ready from local source investigation, but it is not recorded as a formally cleared gstack Eng Review.
