# Thermal Async Runtime And Topology Refactor Plan

- Time: `2026-08-28 01:18:39 +08:00`
- Last revised: `2026-09-01 21:20:55 +08:00`
- Authors: `Codex; OpenAI GPT-5; architecture and implementation owner`
- Status: `in-progress`
- Scope: `new thermal runtime threading, input capture, Page/Brick topology, source ledger, solver, publication, phase completion, lifecycle, tests, docs, and performance validation`
- Related: [`thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md), [`2026-08-27_21-25-37_thermal-mutation-exact-invalidation.md`](2026-08-27_21-25-37_thermal-mutation-exact-invalidation.md), `run/thermal-repeat-20260828-004801-120s.jfr`, `MinecraftThermalInput`, `ThermalDimensionEngine`

## Highest-Priority Acceptance Rule

This rule outranks implementation speed and every narrower checklist item: the first accepted production implementation must already be the best practical architecture for correctness, CPU, retained memory, allocation rate, and readability under the contracts below. A known temporary hot-path implementation is not acceptable merely because it is functionally correct or intended to be optimized later.

- Correctness, asynchronous ownership, lifecycle termination, sparse-work complexity, bounded memory, and readable ownership boundaries are one acceptance decision. None may be deferred behind another.
- A routine exact-position mutation must do work proportional to its affected dependency closure. It must not clone or scan all active Pages, all fragment slots, all material edges, or arena high-water state.
- Full-structure work is allowed only for initial construction, explicit engine reconstruction after a fatal engine loss, or an actual geometrically grown backing store. Capacity growth must be geometric and amortized; it must never occur once per ordinary mutation.
- Do not land compatibility scaffolding, duplicate state machines, global copy-on-write snapshots, or placeholder abstractions that are already known to require a second performance rewrite.
- Tests do not authorize changes to production contracts. Do not add counters, traversal statistics, debug/test fields, test-only getters, probes, observers, callbacks, failure-injection switches, retained diagnostic collections, or conditional branches to production code merely to make a test possible.
- A "counting test" may count only data owned by the test itself, such as fixture inputs, fake dependency calls at an existing production boundary, returned results, allocations observed by external tooling, or backing identity/capacity already exposed by the real design. It must not count through new production state, a new production API, or hot-path bookkeeping.
- Before a gate can be checked, its production path must have: an ownership proof, a failure-boundary proof, explicit asymptotic CPU and memory cost, allocation behavior, deterministic-order proof where floating-point order matters, and regression tests that distinguish local work from total-system work.
- If an implementation satisfies the semantic contract but violates the cost or readability contract, it is rejected rather than marked partially complete. In particular, "preflight builds a complete next solver version and commit swaps references" is rejected for routine local topology patches.
- Every implementation stage must compare the viable general data structures for its read path, write path, retained memory, temporary allocation, deterministic order, lifecycle failure boundary, and code ownership before production code is accepted. The selected design must be the best practical whole-system tradeoff under measured Minecraft workloads, not merely the smallest patch.
- "Optimize once" is an explicit acceptance contract: no stage may be checked while a known, generally applicable design would remove another routine global traversal, repeated payload copy, historical-state scan, avoidable object graph, or compatibility state machine. Do not leave a knowingly inferior intermediate implementation, TODO, dormant alternative path, or planned second performance rewrite for the user to discover later.
- Absolute hardware-independent optimality cannot be asserted without measurement. Therefore final acceptance requires both structural proof of the bounds in this plan and the controlled JFR/heap evidence; a benchmark regression reopens the owning stage instead of being deferred as future optimization.

## Production Code Admission Standard

The thermal system is new and has no compatibility obligation. This section is
a hard gate, not a cleanup suggestion:

- No legacy constructor, facade, alias, adapter, deprecated method, parallel
  dispatch path, old data schema, or transitional production API may remain.
  Tests and callers migrate to the final owner directly.
- One fact has one production authority. A Brick must not be successively
  re-encoded through parallel layout, committed-state, material-metadata,
  fragment, and publication wrappers when the same immutable payload or direct
  primitive arrays can be shared by their real owners.
- No production class, field, branch, collection, callback, validation pass, or
  conversion exists only to make migration easier, preserve an old test, expose
  diagnostics, or support a hypothetical future implementation.
- Class and net production LOC counts are review signals, not acceptance gates.
  Moving unchanged code into extra files does not reduce complexity. A helper
  is permitted only when it owns a real immutable payload or reusable numeric/
  data kernel, removes duplication, or has a distinct reason to change.
- Every retained production field must have a reachable non-diagnostic
  consumer. Every retained method must be called by final production behavior
  or be a necessary public gameplay API. Repository search for obsolete
  coordination names, TODOs, compatibility language, and unreachable paths
  must be empty before validation.
- The only topology lifecycle remains
  `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter`. Data kernels
  may support these owners, but no fourth coordinator, rollback state machine,
  staging facade, or mirrored committed model may be introduced.
- A stage cannot be checked while its implementation exceeds a size/cost gate,
  duplicates an authority, retains its predecessor, or is known to need a
  later simplification pass. Correct-but-bloated code is rejected immediately.

## Goal

Replace the synchronous thermal dispatch and its recovery state machine with the actual asynchronous architecture now. The server main thread must only inspect loaded Minecraft state, resolve it into immutable primitive input, submit one bounded batch per dimension, apply worker completions, and answer queries from lock-free publications.

The worker-owned `ThermalDimensionEngine` owns all mutable thermal authority directly: Page/topology state, `ThermalCellArena`, `ThermalSourceLedger`, `ThermalSolver`, fixed-step progression, phase state, and publication writes. There is no second dimension runtime, generic executor facade, watermark synchronizer, or compatibility path.

Readability is an acceptance requirement. Coordinators remain thin, ownership is visible from class boundaries, lifecycle is represented by concrete messages instead of boolean combinations, and no new giant nested class or compatibility adapter is introduced.

## Verified Current State

- `MinecraftThermalInput` is 3,918 lines and combines Minecraft capture, admission, mutation batching, physical sources, environment refresh, synchronous dispatch, failure recovery, queries, and lifecycle.
- `MinecraftThermalTopologyApplier` is 4,562 lines with roughly 89 methods and 35 nested types. `PageState` mixes committed topology, desired input, transaction scratch, retirement, publication, source invalidation, and diagnostics.
- `DimensionThermalRuntime` is documented and implemented as a server-main-thread logical writer. `LatestSolveEpochScheduler`, `ResolvedGeometryInputRing`, writer-busy statuses, in-flight recovery, and full-topology retry exist primarily because capture and execution share one thread while pretending to have a transport boundary.
- `ThermalPage` currently holds both capture revision and mutable installed topology. This forces synchronization and lets one object straddle main-thread and solver ownership.
- `MinecraftPhysicalSourceManager` resolves worker-owned topology synchronously and writes directly into `ThermalSourceTimeline`, so it cannot remain unchanged when the timeline moves to the worker.
- The latest controlled repeated-door JFR is `run/thermal-repeat-20260828-004801-120s.jfr`: server tick average/P95/P99/max `10.607/24.483/77.922/161.412 ms`; thermal accounts for `397/930 = 42.69%` of Server-thread samples; `signatureIdAt` has 230 samples, material compilation 64, Air lookup 64; Server-thread allocation is `41.48 MiB/s`.
- Existing functional baseline is 190 thermal JUnit tests and 13 required Forge GameTests.

## Non-Negotiable Contracts

1. Minecraft objects are main-thread-only. No `ServerLevel`, chunk, section, `BlockState`, `FluidState`, `BlockPos`, player, block entity, registry object, Forge event, capability, or mutable resolver view enters a worker task.
2. A submitted batch is immutable and primitive-oriented. Arrays transfer ownership to the worker and are never mutated by the producer afterward.
3. Each dimension has at most one executing batch and one main-thread accumulator for the next batch. There is no unbounded task queue and no per-mutation `Future`.
4. One dimension is processed serially. Different dimensions may share a bounded worker pool.
5. The worker is the sole writer for topology, arena H/C, source ledger, sweep, solve clocks, phase request production, and query publication.
6. The main thread is the sole writer for loaded Minecraft capture state, exact-position mutation accumulation, source-seed observations, radiation integration, and application of phase mutations to the world. The worker is the sole authority for thermal Brick residency after admission; player/infrared/passive queries never create residency, and loss of a source seed alone cannot retire warm state.
7. Queries never wait for the worker. A missing, stale, or generation-mismatched publication uses the existing natural/analytic fallback.
8. Topology replacement performs complete planning, limit checks, allocations, migration calculation, and fragment preflight before changing committed Page state. Old spans are released last.
9. Source energy is settled through each event tick. A topology rebind settles the old sink before the new binding becomes effective.
10. Close is idempotent. It rejects new submission, removes the dimension mailbox, lets an already-running task observe cancellation at a batch boundary, releases worker-owned memory exactly once, and never blocks a server tick indefinitely.
11. Routine local topology preflight and commit are sparse: their CPU and temporary memory are independent of total active Page count, total installed fragment count, total material-edge count, and arena high-water mark.
12. Preflight may allocate only delta-sized prepared data, except for a genuine geometric capacity growth. Commit performs no allocation, hashing resize, validation traversal, callback, rollback, or recovery branch after its first committed write.
13. Internal topology commit is a non-throwing, unobservable worker-owned write sequence, not a blanket full-state pointer swap. Readers observe only immutable publications installed after the complete topology commit.
14. No old arena span is releasable until the prepared reference delta proves its next reference count is zero and every dependent fragment is included in the replacement closure.

## Thread Ownership

| State | Main thread | Thermal worker | Readers |
|---|---:|---:|---:|
| loaded Minecraft world and resolver capture | write/read | forbidden | forbidden |
| `ThermalPageHandle` identity/live capture revision | write | validate identity/publish | atomic read |
| pending exact mutations and source observations | write | forbidden | none |
| immutable `ThermalInputBatch` | create/transfer | consume | none |
| Page topology/signature/fragment state | forbidden | write/read | publication only |
| `ThermalCellArena` H/C | forbidden after handoff | sole owner | publication only |
| `ThermalSourceLedger` and worker bindings | forbidden after handoff | sole owner | none |
| `QueryPublication` buffers | no writes | sole writer | lock-free read |
| phase requests | completion drain | create | none |
| phase ACK capture | create/transfer | consume | none |
| radiation service | write/read | forbidden | main-thread query |

## Runtime Data Flow

```text
Minecraft main thread
  capture final signatures / Page lifecycle / environment / source observations
        |
        v
  DimensionInputAccumulator
  one mutable producer-owned next batch
        |
        | seal one aligned 20-tick cut
        v
  immutable ThermalInputBatch
        |
        v
  ThermalWorkerPool (shared, bounded)
        |
        v
  ThermalDimensionEngine (one serial owner per dimension)
  topology plan -> transactional commit -> source bind/integrate -> solve -> publish
        |
        v
  immutable ThermalCompletion
        |
        v
Minecraft main thread
  lifecycle acknowledgements + phase mutations
```

## Batch Schemas

### `ThermalInputBatch`

- dimension generation, strictly increasing batch sequence, and aligned target tick;
- Page admissions: `ThermalPageHandle`, capture revision, natural temperature, 256 sky-column bytes, an exact captured-Brick mask, and owned immutable payloads for only those `4^3` Bricks;
- Brick residency updates: exact Page identity plus captured/required Brick masks; inactive Brick entries use one shared unresolved payload and carry no arena cell or fragment;
- Page retirements: section key and lifecycle generation;
- geometry deltas grouped by Page identity: capture revision plus sorted local indices/signature IDs, or one owned full Page cut;
- environment deltas grouped by Page identity: natural temperature and sparse sky columns;
- source observations: source identity/generation, primitive anchor coordinates, profile enum ID, power, enabled/present state, and effective tick;
- phase acknowledgements: exact reservoir/request sequence and outcome; batch sequence prevents replay, while any resulting world geometry mutation enters the normal exact-tick geometry cut;
- global FarField conductance scale when changed;
- close/cancel is a mailbox lifecycle operation, not a fake input frame.

Arrays are exact-sized at sealing. Normal door/gate/trapdoor changes retain exact local positions. Worker Page/slot state keeps only fixed 64-bit resident, resolved, source-seed, and hot masks. The dimension owns one bounded primitive previous-desired map plus one reusable current-desired scratch map. There is no player-required mask, variable bitset, per-source influence list, persistent frontier mask, or Page-sized continuation-parent graph.

There is no `InputWatermarks`, `SealedInputFrame`, or five-stream cut vector. Mailbox serialization is the ordering proof: the engine accepts only its dimension generation and `lastBatchSequence + 1`, and requires `targetTick >= lastTargetTick`. Page lifecycle/revision, source event tick/generation, and phase request sequence validate their own payloads. A profile reload creates a new engine generation instead of adding another watermark stream.

### Cadence And Backpressure

- Geometry, environment, source, phase-ACK, admission, and retirement input share one aligned 20-tick cut. There is no `urgent` batch flag, immediate source-only solve, or second geometry cadence. Source/ACK records retain their exact event ticks, so delayed processing preserves ledger time.
- A mutation invalidates the affected main-thread publication identity immediately, so gameplay uses fallback during the remaining cut instead of forcing an early worker run. Initial admission becomes visible at the next cut under the same rule.
- If the dimension mailbox is not `IDLE` at a cut boundary, the producer keeps coalescing in the same accumulator. After completion ACK, the next batch seals at the latest aligned boundary. No second immutable queue is created and no event is retimestamped.
- Queue-capacity refusal retains the one already sealed `pendingSubmission` and retries it before sealing later input. The mutable accumulator continues to own later observations. A dimension therefore owns either one queued/running/awaiting-ACK batch or one retained submission, never both, plus one coalescing accumulator.
- `ThermalDimensionEngine` stores `lastTargetTick`; no `ThermalTimePolicy` or `ThermalStepPlan` exists. Normal elapsed time is exactly 20 ticks and executes one 1-second transport step. Zero elapsed time performs no transport. An abnormal larger interval integrates source events through the exact target tick, executes at most one 20-tick transport step, marks the engine non-sleeping/time-degraded, and advances the batch clock. The performance gate rejects routine backlog, so no general substep-array framework is retained.

### `ThermalCompletion`

- dimension generation, batch sequence, and status (`COMPLETED`, `WORK_LIMITED`, or terminal `ENGINE_FAILED`);
- exact Brick residency deltas carrying target section key, the expected lifecycle generation when a Page already exists, and one absolute desired Brick mask; a zero mask withdraws an unadmitted request or permits a cold Page to retire;
- exact committed full-resync tokens carrying Page lifecycle/revision identity;
- bounded phase requests;
- a failure object only for terminal `ENGINE_FAILED`.

Source-dirty sections and internal topology versions remain worker-internal. Close is mailbox state, not a `ThermalCompletion.Status`. Arrays are exact and ownership transfers once; completion accessors do not clone them.

There is no separate `APPLIED`, `TOPOLOGY_UNCHANGED`, `NUMERIC_DEGRADED`, `REBUILD_REQUIRED`, or `CLOSED` completion status. Topology changes are described by exact Brick-residency/resync payloads; numeric/time degradation only prevents sleep and affects publication confidence; unexpected failure is terminal.

No Minecraft mutation is performed by a completion callback. The main thread drains completions during the level tick.

## Worker Lifecycle

1. Server lifecycle calls `ThermalWorkerPool.startShared()` once and `closeShared()` once. It creates a bounded daemon pool named `frosted-heart-thermal-N`; size is capped by available processors and active dimensions, not the legacy player sampler setting.
2. `MinecraftThermalInput` creates a `ThermalDimensionMailbox` and hands a fully constructed `ThermalDimensionEngine` to it once.
3. Submission performs one monitored `IDLE -> QUEUED` transition. While queued/running/awaiting ACK, new input remains in the main-thread accumulator and is sealed only after the completion is accepted.
4. The worker processes one whole batch serially and places one completion in a single-slot mailbox. It never calls Minecraft or schedules nested tasks.
5. Completion drain validates and applies the complete owned payload, then explicit ACK clears in-flight ownership; only afterward may the next accumulated batch seal/submit.
6. Level close detaches capture hooks first, closes source/radiation capture, removes the mailbox from scheduling, and releases the engine at the next worker boundary. Server close drains/cancels all mailboxes and shuts down the pool once.
7. An unexpected failure before or after execution begins closes that dimension engine on its worker and returns exactly one terminal `ENGINE_FAILED` completion. The failed engine is never reused or rolled back. After the main thread ACKs the terminal completion, it creates a new engine generation from one complete cut of all admitted Pages and current source descriptors, merged with input accumulated while the old batch was in flight. Stale completions from the old generation are ignored by identity. There is no partial in-flight retry against mixed old/new input and no `REBUILD_REQUIRED` state inside a live engine.

## Topology Change Pipeline

```text
immutable Page/delta cut
        |
        v
TopologyPlan
  classify unchanged / rebuild / retire
  compute exact Air pair / local FarField / material / source dependencies
        |
        v
BrickTopologyCompiler + MaterialTopologyStore
  use worker scratch and packed Air references
        |
        v
PreparedTopologyChange
  new spans + migrations + exact affected fragment/Page closure
  validate affected endpoints and retiring-slot closure
  reserve backing capacity before any committed write
  immutable, exact-sized, already validated
        |
        v
TopologyCommitter
  single non-throwing authoritative commit
  arena values -> sparse fragments/indexes -> Page state/publication
  -> exact source-dirty sections
        |
        v
release old spans last
```

These are the only three topology lifecycle concepts: mutable worker-local `TopologyPlan`, immutable `PreparedTopologyChange`, and stateless `TopologyCommitter`. Do not create parallel transaction/coordinator hierarchies for arena, sweep, Page, FarField, or publication. Their exact changed arrays are fields of the prepared change and are installed by the owning storage methods.

Any planning or preparation failure discards staging without changing committed state. The worker is the only internal topology reader/writer, and query readers see only publication installed after commit, so internal atomicity does not require cloning complete solver state. Commit performs only prevalidated primitive writes, reference assignments, count/version updates, and old-span release last; it does not allocate and has no rollback/recovery branch.

The sweep fields inside `PreparedTopologyChange` are sparse, not a complete next-version clone. They contain only:

- sorted changed fragment indexes and their already-built fragment references;
- affected material-edge keys, already-built replacement aggregates or removals, and replacement references for only the old/new owner execution fragments;
- sorted unique arena slots with their precomputed next reference counts and presence bits;
- changed local FarField boundary fragments with precomputed slot/page identity, exposed-area conductance, and presence;
- precomputed aggregate counts and the expected base structural version;
- optional grown backing references only when a real geometric capacity expansion is required.

Stable fragment arrays, FarField arrays, reference arrays, presence bitsets, and the material-edge index remain the same instances during an ordinary local change. Preparation applies no mutations to them. Commit writes only prepared indexes. If capacity must grow, preparation constructs the grown backing once and commit swaps it; this exceptional `O(capacity)` work is amortized and analyzed separately.

Material topology has two representations with one authority each. Raw fragment-local contributions exist only for sparse aggregation updates. Solver traversal uses `MaterialExecutionFragment[]`, where each packed endpoint key appears exactly once. The aggregate's canonical owner is the first contribution in `(fragmentRank, operationOrdinal)` order; owner fragments store exact primitive SoA arrays sorted by that same order. `pairOperationCount()` and the hard cap count these execution entries, so admitted work and executed work are identical.

Material-edge preparation groups new contributions by packed edge key in one pass over changed fragment operations. For each affected key it combines unchanged existing contributions with grouped replacements, sums conductance in canonical order, and derives the old and new canonical owner fragments. It then filters affected keys from only those owner execution fragments, inserts the prepared aggregates, and sorts only those local fragments. It must not iterate every changed fragment for every affected key, scan the complete edge table to rebuild owner fragments, copy the complete `materialEdges` map, or rebuild unaffected ranks. The material-edge index must expose explicit preflight reserve and allocation-free/no-shrink commit APIs through project-owned code; it must not depend on protected fastutil internals.

Full traversal validation is revision-scoped. Builders validate finite capacities/conductance and live endpoint generations when constructing initial or replacement fragments. Local preflight validates the exact replacement closure, next reference counts, and retiring slots; a committed structural version certifies unchanged fragments. Solve never calls `requireCurrentTargets()` over the complete traversal. Admission, retirement, or genuine backing growth validates the changed structural closure once. The reference-count invariant prevents release of any certified endpoint while an installed operation still names it.

### Topology Cost Contract

Let `Kf` be changed fragment owners, `Ko` the old plus new operations in those fragments, `Ke` affected material contributions/keys, and `Ks` unique touched state/FarField slots. Let `P`, `F`, `E`, and `H` be total active Pages, installed fragment capacity, material edges, and arena high-water mark.

| Path | Required CPU | Required temporary memory | Forbidden work |
|---|---:|---:|---|
| routine local preflight | `O(Kf + Ko + Ke log Ke + Ks log Ks)` or better | `O(Kf + Ko + Ke + Ks)` | any `O(P)`, `O(F)`, `O(E)`, or `O(H)` copy/scan |
| routine local commit | `O(Kf + Ke + Ks)` | `O(1)` new allocation | rehash, resize, validation, sorting, callbacks, rollback |
| structural admission/retirement | proportional to changed Pages/fragments plus affected ordered indexes | proportional to the structural delta | unrelated Page signature/material/arena scans |
| geometric capacity growth | `O(old capacity + delta)` amortized | one prepared grown backing | growth on every patch or exact-size repeated growth |
| initial/fatal engine rebuild | `O(P + live cells + live operations)` | proportional to live state | scanning unused high-water holes when a live iterator exists |

These are acceptance bounds, not future optimization candidates. Source inspection must make the bounds structurally evident; scale-separated fixtures and external JFR/allocation analysis must distinguish routine work from structural growth and rebuild work. Production counters or diagnostic bookkeeping are forbidden.

## Final Runtime Specifications

The following specifications are final implementation decisions. They replace older review notes in this file wherever wording conflicts.

### Solver Execution And Failure Boundary

- Delete `ThermalSweep`, `ThermalSweepFragments`, and `ThermalStepExecutor` as separate lifecycle/facade layers. One worker-owned `ThermalSolver` directly owns primitive Air/material/FarField/phase fragments, material contribution/index state, reference counts, traversal order, numeric scratch, and sleep residual evaluation.
- `ThermalSolver.step(dtSeconds, forward)` returns one allocation-free status: `COMPLETED` or `NUMERIC_DEGRADED`. Odd batch sequences run canonical forward order and even sequences exact reverse order; no epoch/direction object is allocated. Remove applied-operation counters, reports, whole-state initial/final enthalpy, boundary-energy totals, conservation residuals, and source-applied totals from production.
- `BrickTopologyCompiler` writes primitive fragment builders directly. Remove public `PairOperation`, `BoundaryOperation`, `PhaseOperation`, sweep builders, pending patch facades, and temporary operation record lists. `PreparedTopologyChange` owns exact persistent fragment references consumed by `ThermalSolver.install(...)`.
- Remove `ThermalCellArena.MutationCheckpoint` and the phase substep checkpoint/rewind API. Numeric-domain failures return degraded status without mutating that operation. An unexpected exception escapes directly to `ThermalDimensionMailbox`, which closes and discards the engine and emits terminal `ENGINE_FAILED`; no partially executed engine is retried.
- Solve performs no full traversal preflight and does not catch an execution exception to settle/retry a remainder. Expected topology validity was certified by the installed structural version; source and topology scalar payloads were validated before execution.
- Conservation tests compute before/after energy from test-owned fixture slots and expected fixed-boundary/source terms. Production code does not retain data solely for those assertions.
- The exact maximum-temperature residual scan remains only as the final sleep gate after normal fixed-step status, no active/touched source accumulator, and the configured stable-batch precondition. It is not run during active source input, after degradation, or while already asleep. This exact gate is retained because transfer magnitude alone can falsely sleep a low-conductance but far-from-equilibrium graph.

#### Compiled Fixed-Step Coefficients

The normal transport interval is fixed at `dt = 1 s`, while cell capacity and ordinary Air/material conductance remain constant for an installed topology. Preparation therefore compiles the expensive invariant part once.

- `ThermalCellArena` stores `inverseCapacityKPerJ = 1 / capacityJPerK` beside capacity. Allocation/migration validates both finite and positive and writes them together. Temperature offset becomes `H * inverseCapacity`, removing hot-loop division.
- For a fixed pair, preparation computes `reducedCapacity = Ca * Cb / (Ca + Cb)` using the overflow-stable existing form and `pairCoefficientJPerK = reducedCapacity * -expm1(-G * (invCa + invCb) * 1s)`. Runtime executes `energyJ = pairCoefficientJPerK * (Ha * invCa - Hb * invCb)`, then subtracts/adds the same energy.
- For a fixed-temperature boundary, preparation computes `boundaryCoefficientJPerK = C * -expm1(-G * invC * 1s)`. Runtime executes `energyJ = boundaryCoefficientJPerK * ((Tboundary - Treference) - H * invC)`.
- Fixed Air pair, unique material execution, and fixed material-boundary arrays store compiled coefficient instead of retaining a second conductance array. Raw material contributions retain base conductance only because local edge re-aggregation needs it.
- Buoyant Air pairs, phase contacts, and abnormal non-1-second degraded steps use `ThermalExchangeKernel` with precomputed inverse capacities but dynamic conductance/dt. This is the only generic numeric fallback.
- FarField fragments retain base conductance plus cached coefficient because wind scale changes. `ThermalDimensionEngine` increments one wind generation; each fragment compares one generation before execution and locally recompiles its own coefficient once on first use after the change. There is no all-boundary refresh traversal.

At 65,536 live cells, inverse capacity adds about 512 KiB. Fixed pair coefficient replaces an existing execution double and adds no pair-array memory. Wind-dependent FarField coefficient adds at most one double per FarField operation. This memory/CPU trade is part of the final JFR/heap acceptance.

### Final Engine Pipeline

`ThermalDimensionEngine.process(batch)` is the only worker orchestration method and follows this fixed order:

1. validate dimension generation, `lastBatchSequence + 1`, and monotonic aligned target tick;
2. accept source events and descriptor changes in stable event order; settle/rebind new, changed, or removed descriptors against the currently installed topology at their exact event ticks;
3. apply phase ACKs and Page admission/retirement/geometry/environment inputs to worker staging;
4. build `TopologyPlan` and complete `PreparedTopologyChange`; no committed topology state changes here;
5. settle/deliver `ThermalSourceLedger` through `targetTick` on old bindings;
6. commit topology once, rebind exact dirty source sections at `targetTick`, and release old spans last;
7. execute zero or one fixed transport step, update sleep/work-limit state, and write query publication;
8. advance `lastBatchSequence/lastTargetTick` and return one immutable completion.

Any expected unresolved input remains local fallback/staging. Any unexpected exception from this method escapes to the mailbox, which closes the engine and emits terminal `ENGINE_FAILED`; no inner layer returns retry/rebuild statuses.

### Source Ledger And Accumulator Lifetime

Delete the separate `ThermalSourceTimeline` and `ThermalSourceRegistry` orchestration layers. One worker-owned `ThermalSourceLedger` owns source slots, exact event-tick integration, bindings, pending batch events, degraded-loss routing, and `NodePowerAccumulatorArena`. It does not retain cumulative per-source diagnostic totals. The accumulator remains a separate primitive data structure, not a lifecycle coordinator. Backing arrays grow geometrically and use recyclable slots/project-owned indexes with deletion support.

- A source slot has an occupied bit and free-list link. Unload first settles the source ledger to its event tick, removes continuous power from bound nodes, releases every binding reference, removes the source-ID table entry, clears its fixed port range, and returns the slot to the free list. Re-registration receives a deterministic recycled slot or extends high-water. `sourceCount` means live sources, not historical high-water.
- Remove per-source routed-energy totals and their query methods. Repository search confirms they have no non-test consumer, they prevent complete reclamation, and their current callers are tests/GameTest helpers. Tests assert delivered arena energy, absence of delivery for loss/unbound routes, and test-owned expected ledgers instead.
- Every thermal-node accumulator stores `(nodeId, lifecycleGeneration)`, aggregate power, compensated pending energy, last integral tick, binding-reference count, occupied state, free-list link, and intrusive active-list links. A port retain/rebind/release updates the binding count only after settling the old binding at the authoritative event tick.
- The active list contains exactly accumulators with nonzero power or pending energy. Activation/deactivation is O(1); sleep readiness is O(1); drain traverses the active list once in deterministic slot order, settles continuous power, writes energy, clears pending state, and reclaims entries whose power, pending energy, and binding-reference count are all zero. Unexpected destination failure is terminal engine failure, so no second validation traversal or rollback pass is retained.
- A bound zero-power port keeps its accumulator and old arena span alive through the binding-reference count. An unbound accumulator with pending energy stays active until delivery. An empty unbound accumulator is removed from the node-key table and recycled immediately.
- `ThermalSourceLedger.accept(batchEvents)` validates stable event order and per-source generation/tick, then applies events as the engine advances the batch. It builds a primitive exact index for thermal-node identities referenced by the current pending cut. Old-span release iterates only concrete slots in that span and performs O(1) accumulator/pending-cut lookups; it never scans all sources or historical slots.
- Descriptor/profile/anchor/port changes may reindex and rebind. Power and enabled changes update only the existing source ledger and active accumulator state; they do not clone ports, allocate a descriptor payload, or mark topology ports dirty.
- `WorkerPhysicalSourceBindings` remains separate because it translates primitive Minecraft descriptors into worker topology endpoints. It owns no power/time ledger and calls `ThermalSourceLedger.rebind(...)` only after the old binding is settled.

### Main-Thread Physical Source Spatial Index

`MinecraftPhysicalSourceManager` must not retain a general object map that every secondary consumer scans. One main-thread `PhysicalSourceSpatialIndex` is the primitive authority for observed campfires and machines; radiation, chunk/Page lifecycle, nearest-generator lookup, infrared fields, and worker descriptor production consume it.

- Source state is recyclable SoA keyed by source ID. Project-owned primitive indexes map origin section, origin chunk, retained target section, and source kind to source slots. Page withdrawal and chunk unload visit only their exact reverse buckets; nearest-generator and infrared queries visit only intersecting spatial buckets.
- Stable machine ticks perform an O(1) handle/ID lookup and compare primitive offered state. Unchanged power/enabled/profile/anchor produces no batch payload and no radiation update. A changed source updates the affected buckets and queues one coalesced delta for the next 20-tick cut.
- `RadiationService` reuses the origin-section source buckets instead of owning a duplicate source map. It retains only bounded receiver LOS witnesses and candidate scratch. Receiver work remains `O(intersecting buckets + capped candidate visits + capped rays)` and is independent of total dimension source count.
- Remove fixed functional caps of 128 radiation sources and 64 source-owned Pages. Source metadata grows geometrically under `ThermalMemoryBudget`; Brick/Page/arena admission uses the shared priority budget below. Candidate visits/rays remain explicitly bounded so many distant campfires cannot increase one player's query cost.
- Source removal unlinks every index in O(number of ports + owned buckets), decrements exact target-Brick seed counts, releases receiver witnesses, and recycles the slot. Warm worker residency survives until its normal cooling decision. Shutdown-only iteration over live source slots is allowed once; no routine global replay/scan remains.
- Source discovery is independent of thermal Page admission. Existing campfires are observed from their loaded block-entity/chunk lifecycle; later campfire state changes use the exact source-bearing section hook; machine tick observation starts or updates the runtime directly. Attaching a source-bearing section owner does not create a thermal Page. The Page admission path never scans a chunk merely to discover sources.

### Page Geometry Decision

The spatial decomposition is final: one thermal Page is exactly one Minecraft `16 x 16 x 16` section, and each Page contains 64 `4 x 4 x 4` Bricks.

- `16^3` aligns with chunk-section identity, load/unload events, section mutation hooks, sky columns, lifecycle generation, and O(1) section-key lookup. `8^3` would multiply Page maps/publications/cross-Page edges; `32^3` would capture unloaded sections and make admission or local invalidation too broad.
- `4^3` is the practical local compile unit: a changed block affects at most its dependency closure, owning Brick, and exact negative-face pair owners. `2^3` multiplies fragment/edge/metadata overhead; `8^3` makes a door or one placed block recompile eight times as many block signatures. Neither alternative improves the measured door workload as a whole.
- Replace mutable `PageSignatureStorage` plus nested `Snapshot` with one immutable `PageSignatures` value. It owns one flat 64-reference Brick directory; each entry directly references a uniform `char[1]`/`int[1]` or nonuniform `char[64]`/`int[64]` payload. `withBrick(...)` shallow-clones 64 references and installs one payload. Main capture uses a reusable mutable builder and freezes once; worker/Page publication share immutable payloads directly.
- Resolve the server-wide static Air signature once. When `LevelChunkSection.hasOnlyAir()` is true, requested Brick entries reuse one shared immutable uniform-Air payload and perform no per-block state traversal. This is an exact Minecraft section proof, not a topology cache or heuristic.
- Worker `WorkerPageState` keeps stable 64-Brick authority plus one monotonic resident mask and one resolved mask. Captured and active are the same state because a Brick is compiled when captured and is not individually evicted during that Page lifecycle. A local prepared change replaces only changed Brick references and scalar revisions. Initial admission builds only the requested source/player/guard Bricks; later additions capture and compile another exact Brick without rebuilding the Page.
- `PagePublication` uses one flat 64-reference directory of immutable Brick publication payloads. Inactive entries share one empty payload. An active payload owns coverage slot plus arena generation, mixed geometry/signature reference, and phase candidates for that Brick. A local topology change shallow-clones the 64 references, replaces changed payloads, and installs one Page publication reference; it does not clone several parallel 64-entry arrays.
- A fully nonuniform compact Page retains 8 KiB of signature values plus one 64-reference directory and array headers. A fully uniform Page uses 64 one-value payloads, about 1.5 KiB before the directory. One changed nonuniform compact Brick copies about 512 bytes of directory references plus 128 bytes of signatures; a uniform replacement copies only one encoded value. The direct one-index read and smaller type surface are preferred over a persistent tree.

### Page, Phase, And Query Publication

- `ThermalPageHandle` contains only section key, lifecycle generation, atomic live geometry revision/resync requirement, and one volatile `PagePublication`; it owns no worker topology arrays, arena spans, compiler state, or mutation orchestration.
- A topology commit prepares one owned `PagePublication` only for a Page whose installed Brick payload or topology identity changed. It contains geometry revision, topology generation, committed batch sequence, and the flat Brick directory defined above.
- Geometry and phase candidates are published by one final volatile reference assignment after the Page's topology commit. The publication constructor takes ownership of already exact arrays and does not clone them. Unchanged Pages keep the existing publication. There is no successful-solve all-Page phase pass and no `Publication.withPhaseCandidates()` copy path.
- Reservoir lookup uses a worker-owned primitive open-address reverse index keyed by `(lifecycleGeneration, brickMinX, brickMinY, brickMinZ, profileId)`. Preparation reserves capacity and commit updates it allocation-free. ACK handling checks `fastSlot`, then this exact index, then request sequence. A stale/missing ACK is ignored; strict batch sequence prevents replay, and each outstanding reservoir request accepts its sequence at most once. There is no transition watermark stream or arena scan.
- `QueryPublication` is a flat arena-slot-addressed double buffer: `double[2][capacity] temperature` and `int[2][capacity] slotGeneration`. Reader lookup uses the arena slot directly, strict O(1), with no slot directory, chunk object, binary search, or published `arenaSlots[]`.
- Geometry revision remains Page-local and is validated by reading the same current `PagePublication` from its handle before and after the query seqlock read. Remove geometry revision from the dimension query envelope/API. The numeric sample additionally requires matching arena slot generation and a query topology generation at least as new as the Page publication's topology generation. This prevents one Page mutation from invalidating unrelated Pages and prevents a reused slot from exposing an older sample.
- Capacity grows geometrically with arena slot capacity under `ThermalMemoryBudget`. `Limits` includes an explicit maximum arena slot capacity covering live plus staging slots; preparation refuses optional admission before exceeding it. A deterministic live-span cursor writes every live slot once into inactive arrays; it never counts first. Stale freed-slot values are unreachable through current Page publications. Sleeping republish remains O(1).
- At slot capacity 65,536, the four double-buffered value arrays consume about 2.5 MiB before headers; capacity 131,072 consumes about 5 MiB. Best-fit/reuse plus the explicit slot-capacity limit bounds holes and staging memory.

### JFR Door And Block-Churn Closure

`run/thermal-repeat-20260828-004801-120s.jfr` measured repeated room-door changes at server tick average/P95/P99/max `10.607/24.483/77.922/161.412 ms`, thermal Server-thread samples `397/930 = 42.69%`, and Server-thread allocation `41.48 MiB/s`. Earlier allocation attribution highlighted temporary `int[]`, `double[]`, `ThermalSweep.PairOperation`, and material-key/compiler objects. The following plan items jointly close those causes:

- exact-position accumulation keeps only the final state of one repeatedly toggled position per 20-tick cut;
- count-bounded main capture resolves only the dependency closure and prevents a mutation burst from monopolizing one server tick;
- 64-entry signature payloads and flat 64-reference Page publication directories copy only dirty Bricks rather than 512/4,096 signatures or several complete Page field arrays;
- primitive compiler scratch and exact persistent SoA fragments remove hot `PairOperation`, material-key, linked-map, and discarded list graphs;
- direct batch/engine/solver execution removes per-batch watermark/frame/epoch/time-plan/report/facade objects;
- one sparse `PreparedTopologyChange` updates only affected fragments/material keys/reference slots and never clones complete sweep/FarField/arena backing on a routine mutation;
- local Air edge and exposed-boundary fragments remove all room-component BFS/forest/relabel work, so every door shape change remains in its exact Brick/face closure;
- worker execution removes topology compile/solve from the server thread, while free-span/source/accumulator reclamation prevents churn from retaining historical generations.

These are structural solutions, not a claim that the unimplemented tree is already faster. Door/block churn is accepted only after the controlled JFR shows the allocation classes and CPU stacks above have disappeared or fallen within `Performance Acceptance`; a lower average with the same routine global copy path does not pass.

### Local Air And FarField Boundaries

There is no dimension-wide Air connected-component authority. Delete `IncrementalAirGraph`, component IDs/member lists, BFS/frontiers, spanning-forest edge classes, component sky state, and unresolved-component scans. Brick-local adaptive Air cells, topology-gated frontier masks, and solver pair fragments are sufficient for physical transport.

- Air-to-Air heat moves only through unique local conductance edges. Opening a door adds the exact face/cell edges compiled from its final collision/occlusion shape; closing it removes them. `ThermalExchangeKernel` then moves energy from hotter endpoint to colder endpoint. No room classification or component relabel participates in transport.
- Each Brick owns a local FarField boundary fragment only for microface patches with direct loaded sky exposure. Only resident Page-top-layer Bricks can own this boundary. Their `4 x 4` XZ column group is captured on demand; nonresident/unknown columns remain value `16` and cannot prove sky. The fragment stores arena slot/generation, owning Page slot, and base exposed-area conductance. Geometry/sky changes replace only affected Brick fragments.
- Solver boundary temperature is read from `naturalTemperatureByPage[pageSlot]`. Natural-temperature refresh updates one Page scalar. Wind changes one engine scale/generation; boundary fragments lazily recompile their local fixed-step coefficients when next executed. Neither change rebuilds topology or scans all boundary fragments separately.
- Delete `FarFieldProfileRegistry`, environment-class maps, source-power applicability, and maximum-temperature-delta profile selection. Current gameplay constructs the same conductance for every environment class, so this registry adds policy/object structure without distinct behavior.
- `FarFieldSettings` retains only the calibrated direct-sky conductance inputs. For a proven sky patch, `G_far = baseConductanceWPerK * windScale * openPatchCount / (16 * referenceOpeningAreaBlocksSquared)`. A missing or inactive non-sky neighbor never receives a weak natural-temperature boundary merely because a Page is absent.
- Existing Brick geometry exposes owner-side outer-face apertures. After transport, the worker derives frontier faces from resident masks and inspects only Air components touching an owner-side aperture toward a nonresident neighbor. It requests that neighbor when the known face-local temperature residual reaches the single validated gameplay tolerance. The neighbor is only a candidate until captured; exact two-sided topology decides whether an Air pair exists. High/low thresholds provide hysteresis; source power, total boundary flow, and unknown neighbor properties are not expansion criteria.
- An admitted guard Brick supplies real finite Air capacity. If it warms enough, the same topology-gated rule advances the frontier across the next Brick or section. Until then its unknown non-sky exterior is conservative and has no fake natural sink. Direct sky remains the only routine Air-to-natural boundary.
- Unmapped geometry or unloaded required shape input keeps the exact requested Brick unresolved and on gameplay fallback. The runtime never loads a chunk to prove topology. Admission of a neighboring Brick replaces the conservative frontier with exact cross-Brick Air edges.
- Worker Page state keeps only fixed 64-bit resident, resolved, source-seed, and hot masks. There is no `required` mask: absolute desired residency is output/scratch derived from those authorities and the dimension desired-section map. `QueryPublication` accumulates next-hot bits while performing its already-required live-slot write, using three reusable `maximumPages` primitive scratch arrays for natural temperature and previous/next hot masks. The following Page pass performs only six-direction frontier and changed-mask work. Cross-Page faces remain candidates after their guard is resident so the absolute mask retains that Page until residual release. Delete `continuationFaceMask` without replacement; add no second cell/span scan, Page authority, Brick field, index, or retained frontier state.
- This intentionally removes the old behavior where one sky-exposed member selected one FarField profile for an entire connected Air component. Local diffusion is the authoritative thermal behavior: outdoor influence propagates through conductance over time rather than changing a room-wide classification instantly.

### Prepared Change Order

`TopologyPlan` is mutable worker scratch. `PreparedTopologyChange` is one immutable value containing exact changed Page/Brick indexes, staging spans/migrations, replacement fragment references, material edge writes, state-reference writes, reservoir-index writes, Page publications, source-dirty sections, and retirement candidates. `TopologyCommitter` is stateless. These three types are the complete topology lifecycle.

1. Planning creates worker-private replacement spans and Page drafts; these are invisible to installed sweep and publications.
2. Preflight validates all exact changed endpoints/generations, numeric inputs, next reference counts, hard caps, source-held retirement conditions, and base versions. It reserves every hash/array capacity and creates any exact persistent fragment/publication payload. Failure releases only staging allocations and leaves installed authority unchanged.
3. `ThermalSourceLedger` settles the topology cut on old bindings after preparation succeeds and before the first authoritative topology write.
4. Commit performs only prepared primitive writes/reference assignments in this order: arena migrated values; Air/material/FarField/phase fragments and material/reference indexes; Page state and reservoir reverse index; topology versions; Page publication references; exact source-dirty section queue. No allocation, sorting, validation, callback, or recoverable branch occurs after step 4 begins.
5. Worker source binding consumes the exact dirty sections, rebinds at the already settled cut, and releases old accumulator binding references. Retirement candidates whose sweep and source references are now zero are released last; remaining candidates stay in a bounded exact retirement queue until the responsible pending cut is consumed.
6. Any unexpected exception from step 3 onward is terminal `ENGINE_FAILED`. Because the engine is discarded and no publication from a failed batch is accepted, production contains no post-write rollback architecture.

### Lifecycle State Machine

- `ThermalDimensionMailbox` has one explicit state enum: `IDLE`, `QUEUED`, `RUNNING`, `AWAITING_ACK`, `CLOSE_REQUESTED`, `CLOSED`. State transitions occur under one mailbox monitor; batch execution and processor close occur only on a thermal worker.
- Submission transfers exactly one batch from `IDLE` to `QUEUED`. Queue refusal restores `IDLE` and the main-thread `pendingSubmission` remains owned for retry. A dimension never has two queued/running tasks.
- A normal or terminal completion remains in `AWAITING_ACK` until the main thread consumes its owned payload and ACKs the matching generation/sequence. New captured input remains in the single producer accumulator. ACK returns a healthy mailbox to `IDLE`; terminal ACK moves it to `CLOSED` and authorizes generation replacement.
- Close rejects new submission immediately. Idle/queued close schedules processor closure without using bounded normal-task capacity. Running close lets the already bounded batch reach its worker boundary, discards any nonterminal payload, closes the processor once, and then reaches `CLOSED`. Repeated close is a no-op.
- Level close is nonblocking on the server tick. Global server stop first closes registration, requests all mailbox closes, then joins the bounded workers after accepted batches/close tasks reach their boundaries. Worker interruption is restored to the stopping thread but is not used as a partial-state recovery mechanism. A new shared pool generation is not published until the old pool is fully closed.
- `TemperatureThreadingPool.java` and its dormant related source remain present. Its init/tick/shutdown calls stay commented and disabled; it is neither deleted nor reused.

### Page Management, Capture, And Batch Allocation

- One main-thread `MinecraftPageManager` owns thermal Page handles, source/frontier loaded-state capture, source-first admission queues, lifecycle generation, and application of worker residency deltas. It does not own player or static-Block-radiation coverage and does not decide that non-equilibrium thermal state is disposable.
- It keeps lazy per-Brick physical-source seed counts, one captured mask, and one absolute worker-desired Brick mask per Page. There are no player leases, player masks, continuation-parent references, or dormant-owned interests. Loss of a source seed is input to the worker; retirement occurs only after the worker returns zero desired residency and no source seed remains.
- Source seed reverse indexes and worker pending-section masks release exact requirements without scanning all sources or Pages. Physical-source discovery is driven by source chunk/block-entity and machine lifecycle, not by Page admission.
- Physical-source target Bricks have highest admission priority and worker frontier additions follow. Admission is round-robin within a priority and charged against shared Page/signature/arena/pair memory-work budgets. Budget refusal leaves publication fallback and retains one exact retry request without rebuilding unrelated state.
- `MinecraftPageManager` processes count-bounded 64-state Brick capture and resolved-center queues each server tick. Defaults are frozen only after the 100-player workload below meets the tick budget. A captured Brick publication stays stale until all centers for its revision are resolved, so deferring excess work preserves correctness while bounding main-thread spikes.
- Player temperature updates keep the configured 20-tick interval and stable UUID-derived phase offset, but perform only `live publication -> dormant -> natural` lookup plus analytic/radiation composition. They never enqueue Page/Brick capture. With no source/hot Page, 100 players create zero thermal Pages, cells, pairs, or admission work.
- Cold capture first tests `LevelChunkSection.hasOnlyAir()`. A true result installs the shared uniform-Air payload for every requested Brick with no BlockState loop. Otherwise it reads exactly 64 BlockStates for each requested Brick. All additions for one Page in one cut are coalesced into one `PageSignatures.withBricks` directory replacement, so several neighboring requests do not repeatedly clone the 64-reference directory. Inactive entries share an unresolved payload. The existing Page `byte[256]` sky array is initialized to `16`; admission queries heightmap only for the 16 columns of each newly resident top-layer Brick. Later top-layer additions use the existing sparse `PageEnvironmentUpdate`, while non-top-layer additions perform no heightmap query. The resident top-layer bits themselves prove which column groups were captured, so no `skyKnownMask` is added. A section-identity resync recaptures only the Page's captured mask and its resident top-layer columns; it does not scan all 4,096 positions or all 256 columns. A main-thread-only resolver scratch is cleared before handoff, so no Minecraft object enters the batch.
- Sparse mutation capture remains exact-position-first. It resolves only captured-Brick dependency closures into sorted local indexes/signatures and promotes to complete payloads for the current captured mask only when sparse centers exceed the density threshold. Repeated changes to one position keep one final entry per 20-tick cut.
- `WorkerPageState` owns stable committed Page identity plus 64 Brick slots. Its portion of `PreparedTopologyChange` contains only sorted changed Brick indexes, replacement span/fragment/publication references, 64-entry signature payloads, and scalar revision changes. It never constructs `new PageState(old)` or copies 4,096 signatures and every Page field for a local change.
- Admission installs a Page container plus its exact initial Brick mask. Later mask additions use the same lifecycle and only add newly captured Brick spans/fragments. Bricks are not individually evicted during that lifecycle; a Page retires as one unit only after the worker returns zero desired residency and no physical source seed remains. Re-admission creates a new lifecycle generation and restores only valid dormant Brick residuals.
- `DimensionInputAccumulator`, geometry/source builders, and compiler scratch retain geometrically grown producer/worker capacity across cuts. Sealing transfers exact used prefixes and resets logical size without discarding reusable builders. Shared immutable empty payloads are used for empty arrays; no routine empty cut allocates one zero-length array per schema field.

### Ownership-Sized Class Layout

The extraction is behavioral-neutral and follows authority, not arbitrary line
splitting. Numeric line counts are intentionally not gates: split a class only
when the result has an independent owner or reason to change.

| Class | Sole responsibility |
|---|---|
| `MinecraftThermalInput` | main-thread level lifecycle, 20-tick seal/submit/drain, consumer facade delegation |
| `MinecraftPageManager` | thermal Page handles, source seed masks, worker residency application, loaded capture |
| `ThermalPageHandle` | cross-thread Page identity/live revision/volatile publication only |
| `PageSignatures` / `PagePublication` | immutable flat Brick directories and query payloads |
| `MinecraftEnvironmentCapture` | bounded natural-temperature/sky/FarField input capture |
| `PhysicalSourceSpatialIndex` | main-thread source SoA and origin/chunk/target/kind spatial indexes |
| `DimensionInputAccumulator` | producer-owned coalescing and immutable sequence/tick batch sealing |
| `ThermalInputBatch` / `ThermalCompletion` | immutable ownership-transfer schemas and validation only |
| `ThermalWorkerPool` | shared bounded workers and server-wide close only |
| `ThermalDimensionMailbox` | one dimension's submit/run/completion-ACK/close state only |
| `ThermalDimensionEngine` | all worker runtime ownership and one visible `process(batch)` pipeline |
| `ThermalCellArena` | primitive H/C/inverse-C/cell identity/allocation spans only |
| `WorkerPageStore` | Page identity/lifecycle/indexed committed state |
| `TopologyPlan` | changed closure, drafts, spans, migration, hard-cap preparation scratch |
| `BrickTopologyCompiler` | reusable primitive scratch to exact Air/material/FarField/phase Brick payload |
| `PreparedTopologyChange` | one immutable exact changed payload; data validation only |
| `TopologyCommitter` | prepared nonthrowing write order |
| `ThermalSolver` | primitive fragment stores, material aggregation, numeric execution, sleep residual |
| `ThermalSourceLedger` | source slots/events/bindings/exact Pdt and node accumulator ownership |
| `NodePowerAccumulatorArena` | primitive per-node power/pending-energy storage and active/free indexes |
| `WorkerPhysicalSourceBindings` | descriptor-to-topology endpoint resolution only |
| `PhaseTransitionRuntime` | phase contact/request/ACK state only |
| `QueryPublication` | flat slot-addressed double-buffer projection and O(1) reader |

Hot compiler scratch uses primitive arrays and project-owned primitive key tables for at most the current Brick/cut. It replaces `PageBuild` lists, linked maps, `MaterialPoleKey`/bridge builder graphs, and discarded temporary `PairOperation` lists. Persistent changed fragments receive exact primitive SoA arrays once; scratch capacity is reused geometrically and never becomes a second topology authority or unbounded cache.

### Optimization Boundaries

Future measured constant-factor work must stay inside an existing owner and must not require another architecture migration:

- numeric/vectorization/coefficient changes stay inside `ThermalExchangeKernel`, `ThermalCellArena`, and `ThermalSolver` primitive loops;
- fragment layout/material aggregation changes stay inside `ThermalSolver` plus `BrickTopologyCompiler` output;
- Page copy/admission tuning stays inside `PageSignatures`, `PagePublication`, and `MinecraftPageManager` budgets;
- source bucket/accumulator tuning stays inside `PhysicalSourceSpatialIndex` and `ThermalSourceLedger`;
- query copy/storage tuning stays inside `QueryPublication` without changing consumer APIs;
- worker count and capture/admission limits are configuration/constants selected by the final workloads, not new scheduling abstractions.

Do not add interfaces, service loaders, generic graph APIs, alternate storage backends, or compatibility adapters merely to preserve hypothetical extensibility. Optimizable means hot kernels and storage ownership are isolated, not that every component has multiple implementations.

### Minimality Proof

Every remaining top-level boundary has one non-overlapping reason:

| Boundary | Why it must remain separate |
|---|---|
| `DimensionInputAccumulator -> ThermalInputBatch` | mutable main-thread coalescing versus immutable ownership transfer |
| `ThermalDimensionMailbox -> ThermalDimensionEngine` | cross-thread scheduling/ACK state versus worker thermal authority |
| `MinecraftPageManager -> ThermalPageHandle -> WorkerPageStore` | main capture ownership, cross-thread identity/publication, and worker topology ownership |
| `PhysicalSourceSpatialIndex -> ThermalSourceLedger` | live Minecraft observation/spatial queries versus worker exact energy time integration |
| `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter` | fallible mutable planning, immutable validated payload, and non-throwing authoritative writes |
| `ThermalDimensionEngine -> ThermalSolver` | batch/lifecycle orchestration versus dense numeric hot loops/storage |
| `ThermalDimensionEngine -> QueryPublication` | sole writer versus lock-free gameplay readers |

Merging across one of these rows would mix threads, expose mutable authority, obscure the commit boundary, or pollute numeric loops. Splitting within a row would recreate a deleted facade/coordinator. This is the final class-boundary test; no further architecture simplification is accepted unless a remaining class demonstrably owns two of these reasons.

### Deleted Coordination Layers

The final implementation deletes rather than deprecates these production abstractions: `InputWatermarks`, `SealedInputFrame`, `SolveEpoch`, `ThermalTimePolicy`, `ThermalStepPlan`, `ThermalStepExecutor`, `DimensionThermalRuntime`, `ThermalSweep`, `ThermalSweepFragments`, `ThermalSourceTimeline`, `ThermalSourceRegistry`, mutable/snapshot `PageSignatureStorage`, over-wide `ThermalPage`, `MinecraftPageCapture`, `ThermalPageInterestManager`, `MinecraftPhysicalSourceManager`, and giant `MinecraftThermalTopologyApplier`. Their required behavior is owned by the final classes above. Tests migrate to batch/engine, compiler/solver, source-ledger, Page-manager/handle, and prepared-change boundaries; no aliases or compatibility constructors remain.

## CPU And Memory Work

- Replace `ResolvedGeometryInputRing` with exact-sized batch arrays; remove atomics and full-snapshot queue accounting that no longer serve an SPSC producer/consumer ring.
- Remove every scheduler/watermark/frame/epoch/time-plan abstraction listed above. One mailbox batch sequence is the ordering contract; `ThermalDimensionEngine.lastTargetTick` is the solve baseline and fixed 20-tick policy is direct code.
- Replace hot-loop `AirMicrocell` and `AirRegionKey` allocation with one packed primitive Air reference decoded through helper methods.
- Fill one reusable 64-entry signature scratch per Brick and stop repeated sparse-overlay `signatureIdAt` scans.
- Store immutable `PageSignatures` as 64-entry Brick payloads behind one flat 64-reference directory; use one-value payloads for uniform Bricks, `char[64]` for compact nonuniform Bricks, and promote only the affected Brick to wide storage. Sentinel values live outside registry consumers.
- Keep sparse exact desired deltas; full snapshots transfer ownership rather than clone.
- Keep the arena free-span indexes and local material-rank maintenance already implemented.
- Keep Minecraft source observation in `PhysicalSourceSpatialIndex`; keep worker port lookup section-local through `installedActiveBySection`. No source lifecycle/query path scans the complete source registry.
- Keep all worker source time/power/binding state in one `ThermalSourceLedger`; do not recreate timeline/registry facades around it.
- Coalesce environment changes and source observations by identity in the main-thread accumulator.
- Reuse worker scratch per dimension; do not retain per-batch object graphs after completion.
- Publish flat arena-slot values and one shallow-cloned Page Brick directory; never copy arena high-water holes, all Brick payload contents, several parallel Page arrays, or a sorted slot-key array for one local change.
- Keep routine topology preparation delta-sized. Never clone complete fragment/FarField/reference arrays or the complete material-edge map to obtain transactional semantics.

## Readability Budgets

- `MinecraftThermalInput`: target under 800 lines after Page, source, environment, and batch ownership move to named collaborators.
- `ThermalDimensionEngine`: target under 800 lines after absorbing `DimensionThermalRuntime`, fixed-step execution, sleep/work limits, and publication orchestration; one visible `process(batch)` pipeline remains.
- Each topology planner/compiler/committer class: target under 800 lines and no nested lifecycle state machine.
- State holders contain state and invariants, not orchestration.
- Batch records contain data and validation, not hidden mutation.
- Topology ownership is exactly `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter`. Do not add another transaction, rollback coordinator, `Prepared*` hierarchy, or wide mutable patch with many `prepared*` fields.
- No method exceeds roughly 80 lines unless it is a linear numeric kernel whose loop structure is clearer in one place.
- Delete obsolete code instead of retaining deprecated aliases or synchronous adapters. Tests use the production async boundary for lifecycle behavior and direct engine APIs only for deterministic topology kernels.

## Execution And Context-Recovery Protocol

This file is the durable implementation state. Chat history and model context are not authoritative. After any interruption or context compaction, resume in this order:

1. Read `AGENTS.md`, `.Codex/memory/project-structure.md`, `.Codex/memory/architecture.md`, `plans/README.md`, `diary/README.md`, and this plan.
2. Inspect `git status --short` and the thermal-only diff. Preserve every pre-existing mutation batching and crash fix; do not restore files from `HEAD` merely to simplify the refactor.
3. Search production/tests for every class under `Deleted Coordination Layers`, plus `LatestSolveEpochScheduler`, `ResolvedGeometryInputRing`, active lifecycle calls to `TemperatureThreadingPool`, `enableSynchronousDispatch`, `LATEST_FRAME_REQUIRED`, logical-writer ownership, and in-flight recovery. The retained disabled `TemperatureThreadingPool` source is expected; every other active reference is incomplete migration.
4. Continue from the first unchecked checklist item whose acceptance condition is not satisfied. A class existing on disk is not completion; its production ownership, lifecycle, tests, and obsolete predecessor must all be resolved first.
5. Do not compile after individual edits. Finish the complete source, test, documentation, and lifecycle migration, then let the primary agent run the one unified validation pass itself.
6. Mark checklist items complete only after the implementation is internally complete. Record exact commands and results in `Outcome` and a new diary entry after validation finishes. No sub-agent, new task, worktree, or delegation is authorized.

The refactor is divided into dependency-ordered gates:

| Gate | Completion condition |
|---|---|
| A. Transport | Pool, mailbox, accumulator, immutable batches, completions, and server lifecycle form one bounded production path. |
| B. Ownership | Main-thread code performs no topology, arena, source-ledger, solver, or publication writes. Worker code receives no live Minecraft world objects. |
| C. Runtime | `ThermalDimensionEngine.process(batch)` is the only worker coordinator; every class under `Deleted Coordination Layers`, scheduler, logical-writer, retry, and synchronous compatibility path is gone. |
| D. Topology | `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter` cannot mutate committed Page/arena/solver state before commit; routine work obeys `Kf/Ko/Ke/Ks`; commit is allocation-free/non-throwing and releases old spans last. |
| E. Hot paths | Packed Air references, one 64-signature Brick scratch, immutable `PageSignatures`, direct primitive solver fragments, section-local source rebind, and flat bounded publications are installed. |
| F. Consumers | Phase, query, source, shutdown, JUnit, and GameTest use the asynchronous contract without sleeps or hidden synchronous dispatch. |
| G. Validation | One final primary-agent run passes compile, all thermal JUnit, all required GameTests, and `git diff --check`; controlled JFR/heap evidence is recorded; docs, plan, and diary match the result. |

## Current Implementation Checkpoint

As of `2026-08-28 17:49:53 +08:00`, the worktree is intentionally mid-refactor and must not be treated as buildable or releasable.

- Present but unvalidated: bounded async pool/mailbox/engine, immutable batch/completion types, explicit completion ACK, 20-tick cuts, packed Air references, adaptive compact `PageSignatureStorage`, worker source binding, free-span arena allocation, sparse sweep preparation, and lock-free query publication.
- Deleted production predecessors: `ResolvedGeometryInputRing`, `LatestSolveEpochScheduler`, `GeometryDeltaRing`, and `GeometryDeltaCoalescer`.
- Confirmed incomplete: deletion/absorption of every coordination layer listed above; direct batch sequence/tick schema; inverse capacities/compiled fixed-step coefficients; unique primitive material execution; physical-source spatial index and merged source ledger; recyclable source/accumulator slots; narrow Page handle plus immutable signatures/publication; merged Page manager; deletion of giant topology/source managers; local FarField fragments; compiler scratch; three-stage topology ownership; flat query publication; player staggering; lifecycle tests; stale test migration; docs, diary, and all runtime validation.
- Existing `DimensionThermalRuntime`, watermark/frame/epoch/time-plan/executor, sweep facade/fragments, timeline/registry, and mutable/snapshot Page signature sources are obsolete inputs to the migration, not accepted architecture.
- The valid pre-refactor baseline remains 190 thermal JUnit tests and 13 Forge GameTests. No compile or runtime validation has been run for the async intermediate tree.

## Implementation Checklist

- [ ] Coordination deletion: remove `InputWatermarks`, `SealedInputFrame`, `SolveEpoch`, `ThermalTimePolicy`, `ThermalStepPlan`, `ThermalStepExecutor`, and `DimensionThermalRuntime`; move sequence/tick/fixed-step/work-limit/sleep/publication ownership into `ThermalDimensionEngine`.
- [ ] Solver/material: replace sweep facades with primitive `ThermalSolver`; add arena inverse capacities and compiled 1-second pair/boundary coefficients, unique material execution, exact hard-cap accounting, lazy wind-generation FarField recompilation, no operation records/counters/global scans/rollback, and terminal execution failure.
- [ ] Source lifetime/indexing: add `PhysicalSourceSpatialIndex`; replace `ThermalSourceTimeline` + `ThermalSourceRegistry` with `ThermalSourceLedger`; add source/accumulator slot reuse, binding refs, O(1) sleep readiness, one active-list drain, exact pending/span checks, spatial queries, and remove fixed caps/test-only totals.
- [ ] Page/publication: replace 512-entry signature COW chunks and several flat 64-entry Page field arrays with one flat 64-reference directory of 64-entry Brick payloads; move phase candidates into Brick publication payloads; add reservoir reverse lookup and flat O(1) arena-slot query publication.
- [ ] Local Air/FarField residency correction: retain local Air pairs, remove non-sky weak FarField/`PageContinuation`, compile topology-gated open-frontier masks, and let the worker request exact neighboring Bricks.
- [ ] Page management/player scheduling correction: keep `ThermalPageHandle`/`PageSignatures`, replace Page-wide interest with seed/worker Brick masks, add 64-state sparse captures, delete player leases/expiry/PRIMARY admission, retain fair bounded source/frontier admission, and permit retirement only after the worker releases cold residency.
- [ ] Commit/compiler: delete giant `MinecraftThermalTopologyApplier`; replace `PageBuild` object graphs with `WorkerPageStore`, reusable `BrickTopologyCompiler` scratch, `TopologyPlan`, one `PreparedTopologyChange`, and `TopologyCommitter`; verify allocation-free commit and release spans last after source rebind.
- [ ] Lifecycle/ownership: finalize mailbox/pool state transitions, terminal generation replacement, stale completion rejection, one-in-flight backpressure, Page identity/revision handoff, and keep `TemperatureThreadingPool` present but fully disabled.
- [ ] Readability: perform the ownership-sized extractions in the final class layout without compatibility adapters or duplicated mutable authority.
- [ ] Tests: migrate all stale JUnit and 13 Forge GameTests to observable async behavior; add lifecycle, churn, local-complexity, deterministic numeric order, source energy, phase, query, and rejection coverage without production instrumentation.
- [ ] Documentation: reconcile every climate living document with final async ownership/20-tick lifecycle, update this checkpoint/checklist, and add one diary entry.
- [ ] Validation: only after all preceding source/tests/docs are written, run one unified primary-agent compile + thermal JUnit + GameTest + `git diff --check`, then all controlled door/block/source/player/crop/combined/churn JFR and heap workloads and record exact results.

## Validation Matrix

| Area | Required validation |
|---|---|
| topology kernels | existing geometry/material/adjacency tests plus rejection cases exercised through existing planner/preflight inputs; no production failure-injection hook |
| sparse transaction complexity | run the same one-fragment mutation against small and very large unchanged test-owned fixtures, assert identical local results and unchanged ordinary-path backing identities/capacities, inspect the sparse index structure, and confirm externally with JFR; no production counter or traversal probe |
| transaction capacity | ordinary commit preserves backing identities and material-index capacity; genuine growth allocates only during preflight, swaps once, and remains geometrically amortized |
| retiring-slot closure | preflight rejects a retiring arena slot with a nonzero prepared next reference count without changing committed state |
| deterministic material order | sparse material-edge replacement preserves forward/reverse fragment and operation order and exact expected numeric results |
| compiled coefficient equivalence | fixed 1-second pair/boundary coefficients match generic `ThermalExchangeKernel` over test-owned finite cases within existing floating tolerance and apply the same signed pair energy to both endpoints |
| coefficient lifecycle | capacity/conductance replacement recompiles only affected fragments; wind generation lazily recompiles each touched FarField fragment once; abnormal dt uses the generic fallback |
| direct batch ordering | generation/sequence/tick reject stale or skipped batches; Page/source/phase identities reject stale entries without aggregate watermarks |
| fixed-step engine | normal 20-tick batch executes one step; zero tick executes none; abnormal delay follows the one bounded degraded branch without time-plan arrays |
| Page hot mutation | one changed Brick replaces one 64-entry signature payload plus one shallow-cloned 64-reference directory; unchanged Brick payload identities remain shared |
| source spatial indexes | Page/chunk removal, nearest-generator, infrared, and radiation candidate results match brute-force test-owned fixtures while production exposes no traversal counter |
| interest/capture budget | 100 receiver leases remain bounded/fair, logout/expiry retires exact Pages, and excess admission/capture work returns fallback without a main-thread burst |
| local Air transport | door/block shape changes replace only exact Air pair/FarField Brick fragments; no component, BFS, forest, or room relabel exists |
| local FarField semantics | directly exposed cells receive configured area/wind/continuation boundary conductance; interior cells do not; admitted neighbors replace approximation with exact cross-Page pairs |
| async ordering | two dimensions may overlap; one dimension never overlaps; batch sequence is monotonic |
| backpressure | repeated mutation while busy produces one coalesced next batch and no queue growth |
| Page lifecycle | admit/mutate/retire/re-admit rejects stale identity and stale completion |
| source ledger | register/power/rebind/unload preserves `P * dt` exactly across batch boundaries |
| phase | worker request -> main mutation -> ACK batch ordering, including reject/retry outcomes |
| query | stale/reused slot and stale Page publication fall back; one Page revision change does not invalidate unchanged Pages; current direct slot read is O(1) and never blocks |
| shutdown | close before submit, while queued, while running, and after completion is idempotent |
| failure | naturally invalid planner/preflight inputs and test-owned fakes at existing dependency boundaries leave committed topology coherent; no production-only injection branch or hook |
| game integration | all 13 existing required GameTests adapted and passing |
| obsolete layer removal | production/tests contain no class or compatibility reference listed under `Deleted Coordination Layers` |

## Performance Acceptance

Performance acceptance requires the same JVM/modpack/server settings and a 10-second warmup for every run. The existing 120-second repeated-room-door workload remains the first comparison, but it is not sufficient for the hundred-player architecture claim. Run and report these additional controlled workloads:

1. repeated single-door toggle and repeated alternate-route door toggle;
2. rapid mining/placement distributed across at least 200 distinct positions per second, including multiple positions in one Brick and across Pages;
3. at least 4,096 observed campfires plus 1,024 active machines across at least 256 sections, with a deterministic 10% power/enabled change per cut;
4. 100 staggered player temperature receivers with the configured view distance and a large crop/random-tick workload;
5. the combined 100-player/source/mutation/crop workload for at least 10 minutes, plus a 30-minute source/Page churn retained-heap run.

- Server-thread thermal CPU samples attributable to topology/solve submission must fall by at least 80% from `397/930` because heavy work leaves the Server thread.
- Server-thread allocation must be below `15 MiB/s` for the controlled workload; report worker allocation separately.
- Tick P95 must be below `15 ms`, P99 below `35 ms`, and no thermal-caused tick above `50 ms` after warmup.
- Worker backlog per dimension is structurally bounded to one executing batch plus one producer accumulator.
- Repeated toggles at one position compile at most one final geometry delta per 20-tick cut.
- Every door/gate/trapdoor shape change replaces only its dependency closure and exact owning/negative-face Air pair fragments. Alternate routes and true room splits have the same local topology cost because no room component is maintained.
- Main-thread Brick admission and geometry resolution never exceed their configured per-tick work units. One hundred simultaneous receiver requests are staggered/fallback-capable and cannot trigger 100 full Page captures in one tick.
- Physical-source Brick-seed/chunk lifecycle, nearest-generator, infrared, and radiation candidate work remains spatially bounded when total sources grow from 128 to 5,120; no routine path scans all sources.
- Player and crop publication lookup is strict O(1). Query publication writes flat arena-slot arrays once per solve and local Page publication replaces only changed Brick payloads plus one shallow directory.
- After coalescing, one-position topology preparation visits only its exact dependency closure. Source structure, stable backing identities/capacities, scale-separated fixtures, and external JFR must show no complete fragment-array, material-map, FarField-array, reference-array, or arena-high-water copy on the routine path.
- JFR allocation stacks must contain no routine `InputWatermarks`, sealed frame, epoch, step plan/report, sweep operation record, or compatibility-facade allocation; these types do not exist in production.
- Normal fixed Air/material pair stacks must not call `Math.expm1` or divide by capacity. JFR should show `expm1` only in buoyancy, phase, abnormal-dt fallback, or one-time coefficient compilation after topology/wind changes.
- Report worker CPU and allocation separately for routine sparse preparation, structural traversal change, capacity growth, and explicit engine rebuild. A low average cannot compensate for a routine path with global asymptotic work.
- Thermal retained heap must be reported by Page/signature/arena (including inverse capacity)/fragment/FarField coefficient/mailbox category. No acceptance claim may infer thermal retained memory from whole-process after-GC heap alone.
- The combined workload must keep worker completion latency below the next 20-tick cut at P99 and must not grow a dimension backlog beyond the frozen mailbox/accumulator bound. If this fails, the architecture is not accepted; it is not deferred as a later parallel-solver project.
- After source/Page churn, retained source slots, accumulator slots, interest leases, query chunks, Page payloads, and arena spans must converge to the live working set and remain independent of historical generations/visited Pages.
- Functional energy, topology, query, and phase assertions must remain exact; performance does not justify weakening lifecycle validation.

## Documentation Impact

Update [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`docs/climate/heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), and [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md). Document local FarField conductance, inverse capacity, compiled fixed-step pair/boundary coefficients, four-mask Brick residency ownership, post-solve directional-bitset frontier enumeration, threshold selection evidence, dynamic fallback paths, and memory costs with formulas/units. The climate README remains the navigation owner. No file under `design/` may change.

## Not In Scope

- Changing Air/material exchange kernels, source power integration, phase latent-energy formulas, material coefficients, player body-temperature formulas, or gameplay balance. The component-wide-to-local FarField boundary ownership/formula described above is explicitly in scope.
- Moving Minecraft world capture, radiation ray traversal, player capability mutation, or block-state transition application off the main thread.
- Introducing block-class special cases for doors, fence gates, trapdoors, fluids, or modded shapes.
- Reusing the legacy player `TemperatureThreadingPool` or its configuration.
- Adding distributed execution, persistence of transient mesh state, GPU compute, or an unbounded work queue.

## Outcome

In progress. The `2026-08-28` re-review rejected complete next-version sweep cloning and made first-pass final hot-path quality the highest acceptance rule. Checkboxes and validation results will be updated only after semantic, complexity, allocation, memory, and readability acceptance all hold. `GSTACK REVIEW REPORT` remains the final section so later plan review output has one stable append point.

## GSTACK REVIEW REPORT

Applied the engineering review checklist directly because the current interaction mode does not expose the skill's interactive question tool. The re-review found one P1 architecture violation in the intermediate implementation: full solver-state copy-on-write made routine local mutation scale with fragment capacity, material-edge count, and arena high-water. The plan now rejects that design, specifies a sparse prepared transaction and revision-scoped validation, gives routine/structural/growth/rebuild paths separate cost contracts, and requires scale-independent fixtures plus external profiling before Gate D can pass. Production regression counters are explicitly forbidden.

## Primary-Agent Read-Only Re-Review (2026-08-28)

- Time: `2026-08-28 04:50:41 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent, direct source review only`
- Status: `confirmed blockers; no implementation, compilation, or runtime validation performed in this review`
- Scope: the current uncommitted thermal production, test, and climate-document state, with emphasis on async ownership, Page lifecycle, topology transaction boundaries, source/phase ordering, sparse-work complexity, allocation, retained memory, and readability.

This section records the source findings that motivated the final specifications above. It is historical evidence, not a second active architecture contract; `Final Runtime Specifications`, the current checklist, and the continuation checkpoint own all implementation decisions. No sub-agent, delegated task, worktree, compiler, unit test, or GameTest was used for this review. `design/` was read-only and unchanged. The worktree remains an unvalidated intermediate tree.

### Confirmed P0 Correctness Blocker

1. **The authoritative topology commit is not failure-atomic.** `MinecraftThermalTopologyApplier.apply` sets `authoritativeCommit` only after `DimensionThermalRuntime.commitTopology` returns. That runtime method first calls `ThermalSweepFragments.commitPendingFragmentPatch`; `ThermalSweepFragments.commit` mutates effective material conductance before exchanging the fragment, material-edge, FarField, and reference arrays. An exception between those writes is still treated by the applier as a pre-commit failure, so the Page/arena transaction rolls back while the installed sweep may already be partially changed. This must be resolved before any async topology implementation is accepted: every potentially failing validation and allocation occurs before the commit boundary, and the commit itself consists only of proven non-throwing primitive writes/reference exchanges. If the JVM cannot provide that contract for a step, failure after the first authoritative write is `ENGINE_FAILED` and the entire dimension engine is discarded; it is never a recoverable full-capture request against the old engine.

### Confirmed P1 Lifecycle And Ownership Blockers

1. **Forced pool shutdown can strand a mailbox in `RUNNING`.** `ThermalWorkerPool.close` calls `shutdownNow` after the first timeout but does not process the returned queued dimension tasks. A dropped `ThermalDimensionMailbox.execute` wrapper never publishes a completion and never closes its processor, leaving the mailbox and worker-owned engine unfinished.
2. **Pool termination is not established before a new shared pool can start.** The second `awaitTermination` result is ignored, and the interrupted path calls `shutdownNow` and returns immediately. Because `closeShared` clears the static owner before closing, a later `startShared` can create new workers while old workers are still alive. Shutdown must have one explicit terminal result and must not permit replacement until all prior workers and processors have acknowledged termination.
3. **Lifecycle rejection violates the sole-writer boundary.** `ThermalDimensionMailbox.close` directly calls `closeProcessor` on the caller thread when `executeLifecycle` returns false. In production this caller is the server thread, so it can close the worker-owned runtime and arena. Lifecycle work needs a guaranteed worker-owned control path; rejection cannot transfer engine ownership to the main thread.
4. **Unknown engine exceptions are classified as recoverable after state has already advanced.** `ThermalDimensionEngine.process` advances `lastBatchSequence`, accepts the source batch, begins the runtime epoch, applies descriptor/phase/Page lifecycle mutations, and then catches almost every `RuntimeException` as `REBUILD_REQUIRED`. Only a proven prepare-phase failure may request another capture. Unknown failures, or failures after any worker authority changed, must terminate that engine and produce `ENGINE_FAILED`.
5. **Continuation completions do not carry Page lifecycle identity.** `ThermalCompletion` transports only `continuationSectionKeys` and masks. `MinecraftThermalInput.drainThermalCompletion` writes those masks to whichever Page currently occupies the section, so a completion from a retired Page can admit continuations for a re-admitted Page with a different lifecycle generation. Each entry needs section key plus Page lifecycle generation and the topology/publication revision it describes.
6. **Mailbox capacity is released before the main thread accepts the completion.** `pollCompletion` changes `COMPLETED` to `IDLE` before `drainThermalCompletion` validates generation, sequence, array consistency, resync identity, and continuation identity. Completion application must be fully prevalidated and non-throwing before one atomic accept/ack releases the mailbox, or the mailbox must retain an explicit awaiting-main-ack state.
7. **The thread-external mutation path still reads and mutates Minecraft-owned state.** `MinecraftThermalInput.recordMutation` calls `level.getGameTime()` and invokes `MinecraftRadiationOcclusion.onSectionMutation` when `Thread.currentThread() != mainThread`. The off-thread path may record only primitive section/local-position invalidations into a thread-safe inbox. The server thread must assign the effective tick, update radiation state, inspect owner identity, and capture world data.

### Confirmed P1 Topology And Transaction Blockers

1. **Routine sweep preflight remains global copy-on-write.** `ThermalSweepFragments.preflight(Patch)` clones every Air/material/boundary/phase fragment array, clones all FarField arrays and bitsets, clones all state-reference arrays and bitsets, and copies the complete `materialEdges` map. A one-Brick patch therefore remains `O(F + E + H)` CPU and temporary memory, directly violating the highest-priority acceptance rule.
2. **Per-Page rollback checkpoints copy complete Page state.** `TopologyTransaction` creates a `PageStateCheckpoint` for dirty Pages and neighbors. Each checkpoint copies the complete 4,096-entry `PageSignatureStorage`, desired storage when present, several 64-entry arrays, and fragment/list/map arrays. Routine temporary memory is therefore close to `O(D * 4096)` rather than the changed Brick closure. Replace this with immutable prepared Page next-state plus sparse deltas, or an equally bounded undo log.
3. **Topology preparation mutates committed arena and Page authority before commit.** `MinecraftThermalTopologyApplier.rebuildPage` allocates live arena spans, writes enthalpy, replaces PageState arrays/maps/masks, and queues old spans during preparation, relying on `TopologyTransaction.close` to undo failure. Planning must build worker-private next-state without changing committed Page/arena/sweep authority; the final commit installs it once.
4. **The advertised single non-throwing commit extends past the sweep exchange.** After `runtime.commitTopology`, the applier still commits the transaction, publishes Pages, acknowledges resync tokens, changes installed Page indexes, releases old spans, removes retired Pages, and clears state. These operations can still throw. All capacity and consistency work must be proven before the authoritative boundary, and release/retirement must be represented by a bounded prepared commit whose write sequence cannot fail.
5. **Material-edge preparation is multiplicative.** `ThermalSweepFragments.prepareMaterialEdges` iterates every affected packed edge key and then scans all replacement fragments and their operations to rediscover contributions. The routine cost is `O(Ke * Ko)`. Replacement operations must be grouped by packed edge key in one deterministic pass, followed by one merge per affected key.
6. **Global Air connectivity is unnecessary for heat transport.** `IncrementalAirGraph.patch` rebuilds touched connected components only to derive component-wide FarField/sky/unresolved metadata; solver heat already moves through local Air pair operations. Delete the global component authority and compile exact local exposed FarField boundary fragments instead. Door/gate/trapdoor changes then replace only local pair/boundary fragments with no BFS, forest, room relabel, or block-class special case.

### Confirmed P1 CPU And Allocation Blockers

1. **Phase publication scans and allocates for every active Page after each successful solve.** `MinecraftThermalTopologyApplier.publishPhaseCandidates` walks all installed Pages and reservoir fragments, allocates five arrays per non-empty Page, shrinks them with five `Arrays.copyOf` calls, and `ThermalPage.PhaseCandidateSnapshot` clones all five arrays again. Publish only Pages whose phase candidate revision changed and transfer one immutable owned payload without redundant copies.
2. **Mixed-Page geometry publication copies the full Page for a local mutation.** `PageSignatureStorage.snapshot` clones all 4,096 signatures, while `ThermalPage.prepareGeometryPublication` also clones 64 coverage entries and 64 mixed-geometry references. Use immutable Page/Brick publication state or a two-buffer Page publication that replaces only the changed Brick closure while keeping main-thread reads lock-free.
3. **Full Page capture creates object graphs per block and copies the payload repeatedly.** `MinecraftThermalInput.captureFullPageSnapshot` creates a `LinkedHashMap` for dependency-bearing blocks; `ResolverBlockView.snapshot` then copies that map again before resolver execution. The resulting `int[4096]` is normalized/copied into `PageSignatureStorage`, and transaction checkpointing copies it again. Replace maps and per-cell wrappers with an offset-indexed reusable capture scratch/view, emit an already normalized compact transferable Page cut, and transfer ownership to the worker exactly once.
4. **Node power accumulators retain historical arena generations forever.** `NodePowerAccumulatorArena.ensureNode` only appends; there is no remove, reference release, free-slot reuse, or compact active index. Every `hasActivePowerOrPendingEnergy` scans all historical accumulators, and every drain scans them twice. Brick replacement plus source rebind creates a new `(nodeId, lifecycleGeneration)` entry while the zeroed old entry remains permanently. Add deterministic active/touched indexing and reclaim empty, unreferenced nodes without losing pending energy or changing summation order.

### Confirmed P2 Hot-Path And Readability Debt

1. `QueryPublication.publish` traverses live slots twice, once to count and once to copy, while the caller always reserves from `arena.liveCellCount()` and publishes `[0, highWaterMark)`. Write once into the reserved buffer, validate the final count, or maintain a deterministic live-span index.
2. `DimensionInputAccumulator.seal` discards the reset `ResolvedGeometryBatch.Builder` and constructs a new builder every cut, losing retained scratch capacity. Empty steady cuts also allocate fresh zero-length arrays across geometry/source/batch payloads. Reuse builders and shared immutable empty payloads.
3. `MinecraftPhysicalSourceManager.flush` queues a descriptor even for power-only or enabled-only changes. `WorkerPhysicalSourceBindings.apply` then removes/reindexes/recreates the descriptor, marks every port dirty, allocates a `SourceBinding[]`, and repeatedly clones `MinecraftPhysicalSourceProfile.ports()`. Descriptor updates belong only to lifecycle/profile/anchor changes; power and enabled events must not trigger topology rebind.
4. `PhaseTransitionRuntime.findReservoir` falls back from its fast slot to a raw `0..arena.highWaterMark()` scan for stale ACKs. Maintain a reverse key index for `(lifecycle generation, Brick origin, phase profile)` or at least use a live-slot iterator.
5. Every solver substep calls `requireCurrentTargets`, checkpoints every referenced state, and computes total enthalpy before and after transport. Remove these production diagnostics and full-state rollback; validate endpoints at topology installation and terminate the engine on an unexpected execution exception. Keep the exact residual scan only at the otherwise-eligible sleep transition so low-conductance graphs cannot sleep prematurely.
6. `MinecraftThermalTopologyApplier.resolveAirFacePort` allocates `int[16]` although it only needs to detect whether a second distinct live slot exists. Two scalar candidates provide the same result without allocation.
7. Confirmed unused or over-wide schema must be removed rather than preserved as a future contract: `ThermalInputBatch.urgent`; `DimensionInputAccumulator.hasFarFieldConductanceScale` and `takeFarFieldConductanceScale`; `ThermalCompletion.Status.CLOSED`; `ThermalDimensionMailbox.closed`; completion fields `acceptedTargetTick`, `appliedWatermarks`, `committedSourceBindingSections`, and `topologyResolved`, which the main thread currently does not consume; `SourceDescriptorUpdate.effectiveTick`; and the unused `LongArrayList` import in `WorkerPhysicalSourceBindings`.
8. The ownership split is still obscured by class size: `MinecraftThermalInput` is about 3,700 lines, `MinecraftThermalTopologyApplier` about 4,400 lines, and `ThermalSweepFragments` about 1,500 lines. `MinecraftThermalInput.topologyApplier` is now only an enabled/null marker and construction handoff, yet it leaves a main-thread object graph pointing at worker authority. Replace it with an explicit lifecycle flag/factory handoff and complete the planned coordinator/planner/committer extractions before readability can pass.

### Confirmed Superseded Checkpoint And Documentation Claims

- Production `ResolvedGeometryInputRing`, `LatestSolveEpochScheduler`, `GeometryDeltaRing`, and `GeometryDeltaCoalescer` are already deleted. `DimensionThermalRuntime` now exposes direct mailbox-owned `beginBatch`, `prepareTopology`, `commitTopology`, and `solve`; the older checkpoint saying the engine still calls the scheduler/in-flight state machine is obsolete.
- `MinecraftThermalInput` no longer invokes methods on `topologyApplier` after construction; the older Gate B statement about direct main-thread phase/query calls is obsolete. Query publication and phase completion are now the intended boundaries, but the lifecycle defects above still block acceptance.
- Packed Air references, reusable 64-signature Brick scratch, adaptive `PageSignatureStorage`, section-local source indexing, the free-span allocator, dirty-owner tracking, bounded environment refresh, 20-tick steady cadence, retained `pendingSubmission`, and hard-cap `WORK_LIMITED` handling are present. They must be preserved and tested; their presence does not satisfy the remaining transaction and lifecycle gates.
- The checklist instruction to delete `TemperatureThreadingPool` is wrong. The user contract is final: keep `TemperatureThreadingPool.java` and its related dormant source, leave its server tick/init/shutdown lifecycle calls commented and disabled, and do not reuse its executor for the new worker runtime.
- The execution protocol and Gate G instruction to delegate final validation to Luna is obsolete. The primary agent must complete source, tests, and living documentation first, then run the unified compile/test/GameTest/JFR validation itself. No Luna or other sub-agent is authorized for this work.
- Living climate documentation is currently not current truth despite its `Current` label: it still states synchronous main-thread topology/solver ownership, names the deleted ring/scheduler, documents `GAMEPLAY_SOLVE_INTERVAL_TICKS=5L` although production is `20L`, and describes obsolete apply/retry statuses.
- Tests are currently a compile blocker. Nine GameTest call sites still invoke removed `enableSynchronousDispatch`. Six test files still reference deleted scheduler/ring/status or old seal/runtime APIs: `SolveEpochContractTest`, `DimensionThermalRuntimeTest`, `MinecraftMaterialBoundaryTest`, `MinecraftThermalInputTest`, `MinecraftThermalTopologyApplierTest`, and `FrostedHeartMinecraftThermalInputGameTests`. No tests currently exercise `ThermalWorkerPool`, `ThermalDimensionMailbox`, or `ThermalDimensionEngine` lifecycle.

### Revised Dependency Order

This historical review order is superseded by the current `Implementation Checklist` and `Exact Resume Order`. Those two sections are aligned and are the only active execution order.

### Required Cost And Lifecycle Gates

Let `Kp` be changed positions, `Kb` affected Bricks, `Kf` replaced fragments, `Ko` operations in those fragments, `Ke` affected material keys, `Ks` affected state slots, `L` live published cells, `H` arena high-water, and `Nhistory` historical source-node generations.

- Routine capture: `O(Kp * dependency offsets)` CPU and `O(Kp * dependency offsets)` bounded transferable data, with no per-block map graph.
- Routine topology prepare/commit: `O(Kb + Kf + Ko + Ke + Ks)` CPU and temporary memory. It must contain no `O(P)`, `O(F)`, `O(E)`, `O(H)`, or `O(Nhistory)` scan/copy merely to process an unrelated local mutation.
- Air shape changes compile only exact local Air pair and exposed FarField boundary fragments. There is no component/forest term and no work proportional to room or exterior size.
- Main-thread physical-source mutation is `O(changed sources + affected ports/buckets)`; spatial query/lifecycle work is proportional to intersecting buckets. Worker power/enabled events are O(1) ledger changes. Neither side scans total sources or historical generations.
- Page hot mutation is `O(Kb * 64 signature entries + changed Pages * 64 directory references)` plus actual fragment operations, independent of other Brick payload contents. Page capture/interest work is bounded by queued work units and exact expiring references.
- Query publication writes deterministic live slots once, `O(live spans + L)`, unchanged sleeping publication is O(1), and each player/crop lookup is O(1). It does not retain slot keys/chunk directories, scan `H`, or binary-search `L` slots.
- Rejection tests exercise every existing planner/preflight validation boundary with test-owned inputs and prove that the prior engine version remains exact. They do not add a production failure-injection branch. Any real unexpected failure after the first authoritative write produces one terminal `ENGINE_FAILED`, never Page rollback against a partially changed sweep.
- Shutdown tests cover close before submit, queued but not started, running, completed but not main-acknowledged, worker timeout/interruption, repeated close, and server restart. Every accepted task reaches exactly one completion or terminal cancellation acknowledgement, every processor closes on its owner worker, and a new shared pool cannot overlap the old generation.
- Page lifecycle tests cover admit/mutate/retire/re-admit with stale geometry, resync, continuation, phase, query, and source-binding completions. Every cross-thread Page payload carries and validates lifecycle generation plus the relevant revision.
- Churn tests prove bounded retained memory for repeated Brick generation replacement and source rebind through observable results, stable/reused backing capacity where the design already exposes it, and external heap/JFR analysis. Source inspection and scale-separated fixtures must show that solver/source work is indexed by active/touched nodes rather than `Nhistory`; no production counter or probe may be introduced.

No checkbox in this appended review is complete. The next implementation must satisfy correctness, CPU, memory, and readability together before any gate is checked.

## Production Traversal, Instrumentation, And Test-Pollution Ban

This section repeats the highest-priority acceptance rule as the final implementation checkpoint. No earlier testing or validation wording may be interpreted as permission to alter production code for observability of a test.

- Do not add production counters, traversal counters, debug fields, test probes, observers, callbacks, tracing branches, retained diagnostic collections, or feature flags merely to prove performance or make tests easier.
- Do not add a Page, fragment, material-edge, FarField, arena-high-water, source-registry, historical-generation, or global-owner traversal to any routine tick, local mutation, source power/enabled update, publication, completion, or retry path.
- A new lifecycle path may inspect only the concrete worker/task/mailbox objects it is closing, once during shutdown. Shutdown-only work must not leak into steady runtime or mutation complexity.
- Complexity must be guaranteed by the chosen data structures and ownership boundaries. Tests may use existing public/package behavior, input sizes, object/capacity identity already exposed by the real design, deterministic outputs, test-owned fixtures, and test-owned fakes at existing dependency boundaries; they must not require production instrumentation or a new production-facing testing API.
- Counting is permitted only inside test-owned code and only over test-owned input, fake calls, returned output, or external profiler data. A counter stored or incremented by production code is prohibited even when compiled only for tests or hidden behind package-private access.
- Final performance evidence comes from the controlled JFR workload and external analysis. JFR support must use the JVM's existing facilities and must not add thermal hot-path bookkeeping.
- If a test cannot prove a complexity property without adding production bookkeeping, redesign the production data structure so the bound is structurally evident, then validate its observable behavior and profile it externally.
- Before every production edit, check the changed routine path against this ban and the `O(Kp + Kb + Kf + Ko + Ke + Ks)` local-work contract. Any implementation that is semantically correct but adds a later cleanup/optimization obligation is rejected immediately.

## Primary-Agent Continuation Checkpoint

- Last updated: `2026-08-31 23:00:17 +08:00`
- State: `async owners, transaction ordering, dormant state, and infrared publication are implemented; Page-wide lease/continuation residency is rejected and the Brick-residency correction below is ready but not implemented`
- Resume authority: update this section before context compaction instead of reconstructing the task from chat history or appending another overlapping plan.

### Architecture Completion Decision

The solver, worker, transaction, source-ledger, publication, and 20-tick ownership decisions remain complete. The Page-wide interest/continuation decision was reopened on `2026-08-31` after live behavior and source inspection proved that Page existence followed player/source leases and that a continuation was withdrawn as soon as its neighbor became admitted. The `Brick Residency And Source-Independent Propagation Correction` below is now the authority for admission, non-sky frontier, and retirement. No other architecture choice is delegated to implementation.

This plan-only review changed no production code. The final architecture removes obsolete coordination layers and compiles the fixed 1-second numerical invariants: arena inverse capacity plus fixed pair/boundary transfer coefficients eliminate routine divisions and `expm1` while retaining the generic kernel only for dynamic/abnormal paths. No further orchestration or numeric migration is reserved for later.

### Frozen Architecture

Do not replace these decisions in later continuation turns: `16^3` Page represented cross-thread only by `ThermalPageHandle`, `4^3` Brick as the capture/residency unit, topology-gated worker-owned sparse residency, adaptive Brick-local Air cells, no dimension-wide Air component authority, primitive SoA arena with H/C/inverse-C, compiled fixed-step coefficients in direct `ThermalSolver` fragments, one `ThermalSourceLedger`, unique local conductance edges and direct-sky fixed-temperature boundaries, one `ThermalDimensionEngine` writer per dimension behind a shared bounded pool, one sequence/tick batch plus completion/ACK, fixed 20-tick cuts, one three-stage sparse topology pipeline, flat immutable Page Brick directories, and flat slot-addressed query buffers. Do not reintroduce watermark/frame/epoch vectors, generic time plans, runtime/executor/sweep/source facades, over-wide coordinators, permanent world lattices, room graphs, octrees, synchronous paths, or compatibility architecture.

### Newly Confirmed Corrections

These source findings are required implementation scope, not future optimization work and not claims of completion. They supersede conflicting earlier wording in this plan.

1. `ThermalSweepFragments.pairOperationCount()` counts unique material edges, but `applyMaterialPairsForward/Reverse()` still traverses every raw material contribution and skips zero `effectiveConductance` entries. Hard-cap accounting therefore understates actual solver work. Compile unique material edges into fragment-local deterministic execution arrays; keep raw contributions only for local aggregation updates.
2. Remove production-only solver diagnostics and redundant recovery work: `ThermalSweep.Result` operation counters, initial/final whole-state enthalpy sums, conservation residual bookkeeping, and full-state mutation checkpoint/restore are consumed only by tests or by an engine that is discarded after an unexpected execution failure. Expected endpoint/capacity/state errors must be rejected when a topology revision is installed; numeric kernels return degraded status; any unexpected failure after execution starts produces terminal `ENGINE_FAILED`. Tests compute conservation from test-owned fixture state. This supersedes the earlier instruction to retain full substep rollback.
3. `NodePowerAccumulatorArena` and `ThermalSourceRegistry` both retain historical entries. Unload currently marks a source without reclaiming its slot, `referencesThermalNodeRange()` scans every historical source/port, and accumulator sleep/drain scans every historical node generation. Maintain per-node binding reference counts and deterministic active/touched indexes, settle before rebind/unload, recycle empty source and accumulator slots, and answer old-span reference checks from the exact nodes in that span plus the current pending cut. Do not add another global source traversal.
4. Phase candidates belong to `PagePublication` and change only with affected Brick topology. Remove the successful-solve all-Page phase pass and transfer one immutable exact payload without re-cloning. Add reservoir reverse lookup so stale ACK handling never scans arena high-water.
5. `QueryPublication.publish()` writes the live-cell cut once into flat arena-slot-addressed double buffers including slot generation. It must not first count slots, retain slot keys/chunk directories, or binary-search. Page-local geometry revision stays in `PagePublication`; the dimension query envelope uses topology generation, not one false global geometry revision. Preserve deterministic write order and lock-free O(1) reads.
6. Delete dimension-wide `IncrementalAirGraph`, component/forest/member/witness state, `FarFieldProfileRegistry`, and component-wide profile selection. Compile exact local Air pair and exposed FarField boundary fragments using the frozen local conductance formula. Door/gate/trapdoor changes never perform room connectivity traversal.
7. Replace hot Brick compiler object graphs (`PageBuild` lists and linked maps) with worker-owned reusable scratch plus exact immutable prepared payloads, without retained caches or parallel mutable authority. Split giant input/topology classes along capture, planning, commit, source, local boundary, and publication ownership only.
8. Keep `16^3` Page and `4^3` Brick, but replace eight 512-entry signature chunks and several flat Page field arrays with 64-entry Brick payloads behind one flat 64-reference directory. One local Brick change shallow-clones the directory and replaces only changed payloads.
9. Replace all routine main-thread physical-source registry scans and duplicate radiation source storage with `PhysicalSourceSpatialIndex`. Remove fixed 128-source/64-source-Page functional caps; use spatially bounded queries and shared memory/work admission.
10. Delete player thermal leases entirely. Source seed counts are the only main-thread Brick admission input; worker temperature/topology state owns continued residency and returns absolute desired masks. Player updates retain UUID staggering for body-query distribution but never enter `MinecraftPageManager`, Page capture, or worker masks.
11. Use flat arena-slot query buffers for O(1) player/crop lookup while preserving lock-free double buffering and one live-span write.
12. Topology lifecycle is exactly `TopologyPlan -> PreparedTopologyChange -> TopologyCommitter`; do not introduce another transaction or `Prepared*` hierarchy.
13. Delete `InputWatermarks`, `SealedInputFrame`, and `SolveEpoch`. `ThermalInputBatch` carries generation, sequence, target tick, and exact per-entry identities; mailbox serialization is the ordering proof.
14. Delete `ThermalTimePolicy`, `ThermalStepPlan`, `ThermalStepExecutor`, and `DimensionThermalRuntime`. `ThermalDimensionEngine` directly owns fixed-step time, work limits, sleep, solve, and publication.
15. Replace `ThermalSweep`/`ThermalSweepFragments` with `ThermalSolver`; compiler writes primitive fragments directly and no operation records/patch facade remain.
16. Replace `ThermalSourceTimeline`/`ThermalSourceRegistry` with `ThermalSourceLedger`; keep `NodePowerAccumulatorArena` as its primitive store and `WorkerPhysicalSourceBindings` as the descriptor resolver.
17. Replace over-wide `ThermalPage` with `ThermalPageHandle`, mutable/snapshot `PageSignatureStorage` with immutable `PageSignatures`, and merge `MinecraftPageCapture`/`ThermalPageInterestManager` into `MinecraftPageManager`.
18. Delete `MinecraftPhysicalSourceManager` and giant `MinecraftThermalTopologyApplier`; their responsibilities belong to `PhysicalSourceSpatialIndex`, `WorkerPageStore`, `BrickTopologyCompiler`, `TopologyPlan`, `PreparedTopologyChange`, and `TopologyCommitter`.
19. Store inverse capacity in the arena and compile exact 1-second pair/boundary coefficients during topology preparation. Fixed execution replaces conductance with coefficient; buoyancy/phase/abnormal dt use the generic kernel; wind-dependent FarField coefficients refresh lazily by fragment generation.

### Exact Resume Order

1. Treat the previously completed async engine, source ledger, transaction, dormant storage, and publication work as the base; do not reimplement those owners.
2. Execute `Brick Residency And Source-Independent Propagation Correction -> Implementation Steps` in order, deleting the old Page continuation contract in the same change.
3. Migrate JUnit/GameTest coverage to observable Brick-residency behavior without production instrumentation; update living climate docs and add the diary entry.
4. Run unified compile/tests/GameTests and the controlled 100-source residency/JFR/heap workload. If a named capacity or performance gate fails, record that exact metric and explicitly reopen the plan; do not preselect a limit change, second solver, sleep path, LOD, or impedance.

### Current Implementation Gate

The earlier simplification blocker is resolved in source but not yet validated:

1. `TopologyPlan` is below the `< 800` line target and owns only changed-closure
   collection, ordering, preparation, and exact staging cleanup.
2. `BrickTopologyCompiler` is below the `< 800` line target and uses reusable
   primitive scratch. The deleted giant topology/source/runtime classes are no
   longer present as parallel production owners.
3. `ThermalCellArena` has one allocation API: `stageBrickCells`. Preparation
   writes exact `RESERVED` SoA slots which are absent from committed high-water,
   live count, live iteration, solver state, and query publication. Commit
   promotes the prepared spans; failure discards them and restores the indexed
   free ranges. No second arena snapshot or per-Brick next-state object graph is
   retained.
4. Source energy is settled through the target tick against the installed
   topology before `TopologyPlan.prepare`; migration therefore observes energy
   delivered in the same cut. Exact dirty source sections rebind only after the
   topology commit.
5. `TopologyCommitter` validates structural version, Page ownership, all staged
   spans, and phase reservoir identities before the first authoritative write.
   Old spans are all validated before the first release and are released last.
6. Regression tests now cover reserved-cell invisibility/discard, work-limit
   staging cleanup, and source-energy preservation across same-cut topology
   migration. These tests and the complete current tree have not yet passed the
   unified validation command, so no implementation checkbox is complete yet.

## Brick Residency And Source-Independent Propagation Correction

- Time: `2026-08-31 23:00:17 +08:00`
- Status: `ready`
- Scope: `MinecraftPageManager`, `PhysicalSourceSpatialIndex`, `ThermalInputBatch`, `ThermalCompletion`, `WorkerPageStore`, `TopologyPlan`, `BrickTopologyCompiler`, dormant Page wake, and infrared publication availability
- Precedence: this section supersedes every Page-wide source interest, full-Page admission, continuation-parent, non-sky weak FarField, and lease-driven retirement rule earlier in this plan.

### Verified Defects In The Current Architecture

1. `PhysicalSourceSpatialIndex.refreshTargets` retains only the section containing an `AIR_FACE` port. A campfire therefore owns no transport domain beyond its target Page.
2. `TopologyPlan.initializeAdmission` marks all 64 Bricks dirty. Admitting one edge Brick pays for a complete Page and makes Page count a misleading proxy for live thermal work.
3. `BrickTopologyCompiler` treats every missing Page neighbor as a FarField face and publishes continuation only while that neighbor is absent.
4. Once the neighbor is admitted, the continuation bit clears. `MinecraftPageManager.applyContinuation` then releases the neighbor; without player/source references it retires, after which the parent requests it again. This is an admit/retire oscillation, not propagation.
5. Player leases are currently part of the final `PageEntry.interested()` decision. The same warm world position can consequently lose its solver state when the player crosses a section boundary.

### Final Ownership And State

`Page` remains the section-aligned address, immutable signature/publication directory, mutation owner, and whole-lifecycle persistence unit. It is not the minimum residency unit. `Brick` is the minimum capture, admission, topology, and propagation unit.

Main-thread Page state retains only:

- one 64-bit captured Brick mask;
- lazy per-Brick source-seed counts and their derived 64-bit mask;
- one absolute worker-desired 64-bit mask from the last accepted completion;
- the existing handle, capture revision, priority/retry state, and sparse mutation payloads.

Worker Page/slot state retains only:

- resident, resolved, source-seed, and hot 64-bit masks;
- the existing 64-entry immutable signature/topology/publication directories;
- no persistent frontier mask, per-source radius, continuation parent, room/component graph, or variable bitset.

`required` is not a fifth mask. The absolute desired mask exists only while
building/comparing a completion and then as the main-thread last-accepted value.
Delete `WorkerBrickTopology.continuationFaceMask` without replacement. Regular
Air already has one coverage slot and mixed geometry already owns exact
face-port/component IDs, so another cached aperture byte would duplicate
topology authority.

The worker dimension additionally owns `Long2LongOpenHashMap desiredBySection`
plus one reusable `desiredScratch` map. They contain the previous and current
union of absolute desired masks for both admitted Pages and unresolved frontier
sections. Admitted sections remain in this comparison authority so changed-only
completion, warm-Page retention, and zero cancellation need no fifth Page mask.
The maps are bounded by active Pages plus their unresolved frontier and are not
keyed by parent/source.
One reusable primitive list holds only Page lifecycles committed since the
previous residency completion. It forces one identity-bearing absolute mask,
including zero, when the numeric section mask matches the prior lifecycle, then
clears. It is not a third residency authority or steady frontier index.

Source observations add source-seed bits. Only source-seed and worker hot state drive frontier expansion. Player, infrared, crop, town, and static Block-radiation queries are passive and never enter these masks. Only the worker can keep a Page alive because of non-equilibrium Air/material/phase state. A Page may retire only after the worker publishes zero desired residency and the main thread has no current source seed.

### Sparse Capture And Admission

1. A physical `AIR_FACE` source records its exact target Brick, not only its section. Multiple sources in one Brick use one lazy primitive reference count.
2. A cold admission captures only requested Bricks. An exact `LevelChunkSection.hasOnlyAir()` proof reuses one shared uniform-Air payload without reading individual states; other sections read exactly 64 current BlockStates per requested Brick. All additions for one Page in the cut are frozen through one directory replacement. Uncaptured entries share one unresolved payload. Sky capture is independent: only newly resident top-layer Bricks query their 16 XZ columns, unknown columns remain `16`, and later additions reuse the existing sparse environment update schema.
3. One topology guard layer is captured with a source/frontier request. Within an existing Page, later additions replace only newly captured Brick payloads. Across a Page boundary, the request creates or extends the neighboring Page container without compiling its other Bricks.
4. `SectionOwner` publishes the Page's captured mask as one volatile long for off-thread mutation classification. A topology mutation invalidates Page geometry and records a signature delta only when its Brick bit is captured. Source-relevant mutation, sky-column invalidation, and radiation-occlusion invalidation remain independent channels and are processed even when the mutated Brick is uncaptured.
5. A section identity replacement recaptures the current captured mask and independently rescans source state. The normal production architecture has no 4,096-position full Page admission/resync path.

### Topology-Gated Error-Driven Frontier

The compiler can prove only that the resident owner Brick has an outer-face aperture; it cannot prove that an uncaptured neighbor is Air. A closed owner face cannot request expansion. For an open owner face, the worker reads only the known Air components touching that face. Mixed geometry uses its existing face-port/component mapping, so an unrelated hot component does not activate another component's opening. The neighbor is captured once as a topology candidate; exact two-sided compilation stops at a wall and never propagates energy through it.

Hot-mask thresholding is fused into the existing `QueryPublication` live-slot
loop; residency performs no second cell/span scan. Its one post-solve Page pass
reads the resulting hot bits and, for each of six
directions, compile-time boundary masks and shifts compute:

```text
active = sourceSeedMask | hotMask
samePageBits = active & interiorOwnerMask & ~shiftedResidentMask
crossPageBits = active & boundaryOwnerMask
candidateFaceBits = samePageBits | crossPageBits
```

`crossPageBits` intentionally includes an already resident guard because it is
also the Page-lifetime retain signal. Iterate only `candidateFaceBits` with
`Long.numberOfTrailingZeros`. Each bit
reads the regular O(1) coverage slot or existing exact mixed face ports for that
direction. The masks are static constants, not retained Page state. This adds no
candidate array, aperture cache, neighbor directory, field, allocation, or
lifecycle.

After each normal 20-tick transport step:

1. derive frontier faces into reusable scratch from resident masks and owner-side apertures;
2. for each frontier face, compute `faceResidualC = max(abs(T_component - T_natural_page))` over its known touching Air components;
3. add the candidate neighboring Brick when `faceResidualC >= REFINE_HIGH`;
4. retain a previously hot face while `faceResidualC > RELEASE_LOW`, with `REFINE_HIGH > RELEASE_LOW`;
5. union same-section and cross-section requests before comparing with the previous desired/pending masks;
6. emit only changed absolute section masks in `ThermalCompletion`; unchanged residency produces no completion payload.

The thresholds bound the largest represented-to-natural temperature discontinuity at an omitted Air face. They are one gameplay error contract, not source-power, total-flux, infrared-color, player-distance, Page-age, or an estimate involving unknown neighbor state. The earlier proposed `0.125 C / 0.0625 C` values are not frozen: infrared uses `0.25 C`, player sync uses `0.1 C`, and server gameplay consumes unquantized Air. Before production editing passes the first gate, a pure-Java regular/mixed/tunnel fixture must sweep candidate values and freeze the largest `REFINE_HIGH` that simultaneously produces no approved infrared/player/crop boundary regression and passes the 100-source residency cap. Then select the highest strictly lower `RELEASE_LOW` that produces no two-cut frontier toggle in heat-up, cool-down, door, and mixed-component fixtures. This ordering minimizes residency without trading away the declared error bound. Implementation cannot choose values ad hoc, collapse to one threshold, or expose a new config.

Every hot Brick therefore has one compiled finite-capacity guard layer. A guard that becomes significant advances the domain naturally across Brick/Page/section boundaries. A cold guard does not create a visible Page edge. This is a moving error boundary, not a fixed source radius.

### FarField And Loaded-World Boundary

- Direct loaded sky exposure is the only routine Air-to-natural FarField proof.
- A missing, inactive, or unrepresented non-sky neighbor receives no weak natural-temperature boundary. The worker requests its Brick when the error gate requires real continuation.
- An unloaded chunk is not loaded or ticketed for thermal propagation. Its geometry is unknown and cannot be represented as outdoor Air. The main thread parks its pending Page outside the admission queues and the existing `pagesByChunk`/`ChunkEvent.Load` path wakes it exactly once when world data becomes available. Dormant storage handles unload/reload residuals but does not simulate offline source power.
- Work-limit refusal retains the exact pending residency request and current committed worker state, then uses the existing retry backoff until capacity or interest changes. An uncommitted main-thread cold capture is discarded so mutations or chunk identity changes during backoff cannot make retry install stale signatures. A `WORK_LIMITED` completion emits no additional expansion delta. The source continues heating its valid local binding; the runtime does not invent a natural sink, clear the committed Page, or retry every cut. This overload may produce local overheating, which is the explicit bounded-memory degraded result when the accepted 100-source capacity gate is exceeded.

### Growth, Cooling, And Persistence

Resident Brick bits may grow during one Page lifecycle but are not individually evicted. This removes per-Brick enthalpy-disposal/migration and prevents threshold chatter from repeatedly replacing topology. When source requirements disappear, hot and worker-desired masks shrink through hysteresis; an outer Page retires as one existing transaction only after all resident Air/capacitive material is below the release contract, all phase reservoirs have returned their unreserved partial energy to colder Air, and no adjacent hot frontier requests it. A phase reservoir's fixed transition temperature is not itself a hot-state signal.

Retirement continues to capture the existing `DormantChunkThermalState.SectionEntry.brickMask`. Dormant state answers passive queries and initializes a Brick only when a real source/frontier admission later reaches it; dormant data alone never creates Page interest. Without a live Page, residual temperature continues its existing analytic half-life toward current natural temperature.

### Cost Contract And Corrected Capacity Claims

- Stable source-free player work is zero thermal Page/Brick capture; source/frontier changes alone can request residency.
- A newly created Page performs one natural-temperature sample. Heightmap work is `16 * newlyRequestedTopLayerBricks`, from zero to 256 queries; lower-layer Bricks add none. A non-air section reads `64 * newlyRequestedBricks` BlockStates; an all-air section uses the O(1) proof and reads zero individual BlockStates. Existing-Page additions query only newly resident top-layer column groups and use the existing sparse environment update.
- Stable worker solve remains `O(B_resident + E_resident + boundaries)`. Query publication already visits every live slot and now performs one additional Page-slot/Brick-index/threshold/OR sequence there plus one contiguous `O(maximumPages)` hot-mask clear. Residency adds no second cell pass. Its Page phase scans at most `P_active <= maximumPages` headers, returns immediately for zero `sourceSeedMask | hotMask`, and performs exact component work only for candidate faces. Additional frontier work is bounded `O(P_active + F_frontier)` with no 64-Brick loop, incrementally maintained frontier-Page index, or aperture cache.
- Page containers remain bounded by `maximumPages`, but inactive Brick slots create no arena cell, solver fragment, or infrared temperature payload.
- Existing 40-tick staggered infrared polling, client-carried presence, Page change IDs, and unchanged-response suppression remain unchanged. Infrared observes residency; it never creates it.

For a regular-air lattice, a center-radius-four-Brick sphere plus one six-neighbor guard contains exactly 443 cells, 1,110 internal pairs, 438 geometric exterior faces, and touches 22 or 23 Page containers depending on alignment. For 100 non-overlapping copies this is 44,300 cells and 111,000 pairs, within current regular-air limits. These figures are a fixed comparison mask, not a long-lived maximum.

A read-only linear regular-air calculation using current default capacity, conductance, and campfire convection grows the `>= 0.125 C` set from roughly 437 Bricks at one hour to roughly 847 at four hours when sky, walls, material loss, and buoyancy are omitted. It is not a production benchmark; it invalidates the earlier claim that 443 Bricks is a stable per-source bound. The controlled 100-source fixtures, not this model, own capacity acceptance.

### Capacity Acceptance

The controlled 100-source workload is the only capacity decision. If it passes,
stop. If it fails, record the exact failing cells, arena slots, pairs,
boundaries, phases, retained heap, or P99 metric and explicitly reopen this plan.
A limit change or alternative solver is not part of the current architecture
and is not selected in advance.

### Rejected Competing Architectures

- A fixed 27-Page cube is simpler but compiles 1,728 regular cells per source and exceeds current 100-source cell/pair/boundary limits.
- A fixed Brick sphere has predictable cost but creates the same permanent Air wall in long tunnels at a different coordinate.
- Room/component graphs lose local temperature gradients and require split/merge work on doors.
- Dijkstra or analytic source fields discard transient capacity, material/phase coupling, and the authoritative energy ledger.
- Matrix-free regular-Air traversal, certified steady-source sleep, `4/16` all-air LOD, and one-pole exterior impedance are not part of this implementation. None is required to correct residency, each adds another solver or reduction contract, and any future choice requires a separately reopened plan backed by the exact failed metric.

### Implementation Steps

1. [ ] Complete the controlled 100-source threshold/JFR/heap sweep. Deterministic tests freeze implemented `REFINE_HIGH = 0.125 C` and `RELEASE_LOW = 0.0625 C`; production acceptance still requires the long workload.
2. [x] Replace source target-section retain/release with exact target-Brick seed counts and masks; delete player Page/Brick leases, masks, timing wheel, logout release, previous-Brick lease, and PRIMARY admission priority.
3. [x] Add sparse Brick capture/admission payloads and one monotonic resident mask; resolve/share the uniform-Air payload through `LevelChunkSection.hasOnlyAir()`, coalesce same-cut Page additions, capture only newly resident top-layer sky-column groups through the existing environment update, and retain whole-Page retirement.
4. [x] Publish the captured mask to `SectionOwner` and split mutation handling into resident geometry, always-observed source, sky-column, and radiation channels.
5. [x] Replace `PageContinuation`/continuation references with changed absolute Brick-residency deltas and one worker-owned desired-section map with zero-mask cancellation. Delete every `required` mask and delete `WorkerBrickTopology.continuationFaceMask` without replacement.
6. [x] Accumulate next-hot bits inside the existing query-publication live-slot loop, then run one Page-only frontier pass. Use six compile-time boundary masks/shifts to derive same-Page nonresident faces and all active cross-Page boundary faces; the latter retain an admitted guard until residual release. Iterate only candidate set bits and evaluate existing regular coverage slots or exact mixed face-port components. Remove non-sky weak FarField construction and add no second cell scan or frontier authority/index.
7. [x] Apply existing retry backoff to work-limited residency without new expansion deltas, natural sinks, or repeated rebuilds.
8. [x] Keep player/passive lookup strictly `live publication -> dormant half-life -> natural`; dormant data never queues admission. A later real source/frontier admission may reuse its Brick residual without a new NBT version or offline solver.
9. [x] Decouple physical-source discovery and radiation visibility from Page admission. Static no-witness DDA and covered static-radiation mutation ownership never create or retain a thermal Page. Thermal admission remains source/frontier-only.
10. [x] Update living climate/runtime/source documents for the implemented ownership, cadence, fallback, and cost contracts.
11. [x] Park unavailable frontier/source targets until the existing chunk-load index wakes them; remove per-tick failed admission polling without adding another pending collection.
12. [x] Discard uncommitted work-limited admission captures before backoff so retry recaptures current BlockStates without a retained invalidation flag.
13. [x] Exclude fixed phase-transition temperatures from hot masks, use the existing phase contact to return unreserved stored energy to colder Air, and cover that direction in the existing quiet-sleep residual without a new pass.
14. [x] Force one absolute residency publication for every committed Page lifecycle through a reusable primitive admission-identity list; preserve ordinary changed-mask suppression and zero cancellation.
15. [x] Cancel a mutable admission when its last main-thread interest disappears; if it is already sealed/in-flight, retain the handle, queue the latest source-seed state, and let the mandatory first lifecycle completion decide residual retention or retirement. Discard uninterested work-limited admissions without backoff.
16. [x] Keep `queuedPriority` equivalent to physical admission-queue membership: dequeue immediately when interest reaches zero, clear metadata immediately after `removeFirstLong`, and share one dequeue helper with Page removal.

### Validation

- Same world position has the same Page/Brick temperature while a player crosses every X/Y/Z section boundary.
- A lit campfire keeps its domain with no nearby player while its chunk remains loaded; removing/extinguishing it preserves and cools residual state instead of clearing immediately.
- A straight loaded tunnel propagates across at least three Page boundaries without orange/blue Page seams or admit/retire oscillation.
- A solid wall prevents frontier expansion through its closed faces; opening and closing a door adds/removes only the exact local edge and required guard.
- A hot mixed component expands only through its own face ports; another disconnected component in the same Brick cannot trigger that face.
- Regular, solid, and mixed fixtures produce the same directional candidate bits and exact frontier results as a test-owned brute-force 64-Brick/component reference, including every Page edge/corner alignment. Production performs one post-solve Page pass, six static mask/shift expressions, and candidate set-bit iteration; it retains no `required` mask, continuation/aperture byte, neighbor directory, or frontier array.
- Two sources sharing a Brick/Page survive either source removal without losing the remaining seed.
- One hundred source-free players perform passive live/dormant/natural queries while Page count, resident Brick count, arena cells, pair operations, and admission queues remain zero.
- Static Block-radiation coverage and no-witness block DDA leave the same thermal counts at zero. Physical radiation witness ownership changes only visibility-cache invalidation and cannot create Page residency.
- An all-air section admission performs zero individual BlockState reads and all requested entries share the same immutable Air payload. A non-air admission reads exactly 64 states per requested Brick.
- A Page with no newly resident top-layer Brick performs zero heightmap queries. Each new top-layer Brick queries exactly its 16 XZ columns; repeated Y-level additions in the same XZ group cannot occur because only Page-local Brick Y=3 owns direct-sky state. Unknown columns remain `16` and never produce FarField.
- An uncaptured campfire mutation is still observed and seeds capture, while an ordinary uncaptured block mutation does not invalidate the Page publication; roof and radiation invalidation still occur.
- A requested but unloaded neighbor is represented by one pending section mask outside the admission queues, emits no repeated unchanged completion or failed `getChunkNow` polling, is awakened by its exact chunk-load event, and receives a zero-mask cancellation if the parent frontier cools before admission.
- A work-limited frontier retains committed state, discards uncommitted Minecraft capture, follows retry backoff, recaptures after intervening mutation/unload, emits no repeated expansion delta, and never appears as natural-temperature Air.
- An empty phase reservoir at a transition temperature different from natural Air does not retain its Brick. Partial unreserved energy returns to colder Air and reaches zero; reserved request energy remains resident until matching ACK.
- Replacing a Page lifecycle while an older residency completion is in flight cannot suppress the new lifecycle's numerically identical mask; the new generation publishes exactly once, including when its current desired mask is zero.
- Removing a source after its Page admission is sealed but before the first publication cannot discard source Pdt or residual Air. A still-mutable admission cancels without worker work; an in-flight admission publishes once, consumes the queued latest source-seed state on the following cut, and then cools or retires normally.
- Removing and re-adding a source while a partially captured admission is in flight cannot leave a stale priority that suppresses requeue; every missing requested Brick re-enters the real queue exactly once.
- A retired/unloaded warm Page answers from its analytically decayed dormant Brick without creating residency; when a real source/frontier later admits it, the same residual initializes the live Brick without a natural-temperature frame.
- The 100 non-overlapping source fixture records active Pages, Bricks, cells, pairs, boundaries, worker completion latency, main-thread capture time, and retained heap externally. It must remain below the next 20-tick cut at P99 and within the existing hard limits, or this section is reopened before adding LOD/sleep/implicit traversal.
- The threshold sweep records boundary error, two-cut residency toggles, and live work for every candidate pair. It freezes the largest correctness-valid `REFINE_HIGH` and highest lower non-toggling `RELEASE_LOW`; no hand-selected or runtime-configured value passes.
- If a 100-source limit fails, the evidence names live cells, arena slots, pairs, boundaries, phases, retained heap, or P99 separately and explicitly reopens the plan. No limit change or alternative solver is preselected.
- The external fixture confirms hot-mask work is fused into the existing query live-slot write and additional residency work is `O(P_active + F_frontier)` without a retained frontier index or second cell/span scan.
- Existing source-energy, topology transaction, phase, infrared codec, 40-tick stagger, unchanged-S2C, and Forge GameTests continue to pass.

### Implementation Outcome

- Time: `2026-09-01 05:42:50 +08:00`
- Status: `implemented; controlled performance and live-game validation pending`

Production now admits and extends Pages by exact Brick masks, retains four
worker masks, publishes changed absolute residency with zero cancellation, and
propagates Air through compiled cross-section pairs without non-sky missing-
neighbor FarField. Player/infrared/passive queries create no residency. Source
ports seed exact Bricks, all-Air capture reads zero individual BlockStates, and
top-layer heightmap work follows only newly resident Brick columns.

`compileJava`, `compileTestJava`, `compileGameTestJava`, all 14 required Forge
GameTests, the complete thermal JUnit selection, sparse one-Brick admission, threshold hysteresis, changed-only
neighbor masks, cancellation, and actual cross-section heat transfer pass.
Controlled 100-source JFR/heap and live long-tunnel/campfire validation remain
the only open gates for this correction.

## Single-Worker FarField Admission Crash Correction

- Confirmed on `2026-08-30`: with `ThermalWorkerPool` configured for one
  worker, initial Page admission could fail with
  `IllegalArgumentException: cell slot is not live: 0` while compiling
  FarField boundaries.
- Root cause: `TopologyPlan.prepare` intentionally compiles fragments from
  replacement cells while they are `RESERVED`, before `TopologyCommitter`
  promotes them to `LIVE`. `BrickTopologyCompiler.freezeFarBoundaries`
  unnecessarily recovered the owner through `ThermalCellArena.pageSlot`, whose
  contract correctly accepts only `LIVE` cells.
- Resolution: FarField compilation now writes the already authoritative
  `WorkerPageStore.PageState.pageSlot` for every boundary owned by that Page.
  `ThermalCellArena.pageSlot` remains strict; no lifecycle validation was
  weakened and no traversal, allocation, cache, compatibility path, or
  production diagnostic was added.
- Regression coverage: the existing engine admission gameplay-path test now
  enables FarField, so it crosses the exact `RESERVED` fragment-compilation
  boundary before asserting the committed Page publication.
- Validation: the focused diff passes `git diff --check`. The unified thermal
  compile/test command is currently blocked before test execution by unrelated
  uncommitted player-temperature work; those files were not changed or
  reverted as part of this correction.

## Consecutive Batch Sequence Restart-Loop Correction

- Confirmed on `2026-08-30` with JDWP at the real `ThermalDimensionEngine`
  rejection branch: the engine owned `dimensionGeneration=1000`,
  `lastBatchSequence=1`, and `lastTargetTick=32100`, while the next batch carried
  `dimensionGeneration=1000`, `sequence=1`, and `targetTick=32120`. Generation
  and tick were valid; the producer duplicated sequence `1` on its second cut.
- Root cause: `DimensionInputAccumulator.seal` passed
  `Math.incrementExact(nextSequence)` into the batch constructor without storing
  the result. The field therefore remained zero and every cut in one generation
  was emitted as sequence `1`.
- Resolution: `seal` now advances and stores `nextSequence` before constructing
  the batch. A replacement accumulator for a new dimension generation still
  starts at zero, so its first batch is sequence `1`, matching a fresh
  `ThermalDimensionEngine`.
- Regression coverage directly exercises the producer lifecycle: one generation
  seals sequences `1` then `2`, and a replacement generation restarts at `1`.
  No production counter, diagnostic field, compatibility branch, traversal, or
  new abstraction was added.
- Validation: Java 17 offline `compileJava`, `compileGameTestJava`, selected
  thermal JUnit (`109/109`), and Forge GameTest (`14/14`) passed in one Gradle
  run. A real quick-play client loaded the existing world and ran from integrated
  server startup at `15:03:59` through `15:05:11`; its fresh log contained zero
  matches for `thermal batch generation`, `Thermal dimension worker failed`, or
  `cell slot is not live`.
- Documentation impact: no living climate document changed. It already states
  the intended consecutive-sequence contract; this correction makes the
  producer conform to that documented behavior.

## Dead Production Surface Cleanup

- Confirmed on `2026-08-30` by a complete thermal production-symbol census.
  Every top-level production type has a production reference except
  `MinecraftThermalEvents`, which is loaded by Forge through
  `@Mod.EventBusSubscriber`. Every zero-reference member was removed except the
  Forge subscriber methods and the explicitly retained source impulse entrance.
- Deleted six production types whose behavior existed only for tests or an
  unconfigured future branch: `GeometrySummaryCache`, `DependencyOffsetMask`,
  `ResolverBlockView`, `ThermalSignatureResolver`, and
  `ThermalSignatureResolverDispatcher`, plus the write-only `GeometrySummary`.
  Their four dedicated prototype test classes were deleted with them.
- The dispatcher cleanup is behavior-preserving for the shipped runtime:
  `MinecraftThermalProfiles` constructed an empty dispatcher with no explicit
  or contextual registration. `MinecraftSignatureCapture` now resolves the
  already-loaded `BlockState` directly through `StateStaticThermalResolver`.
  It no longer retains the `18^3` section scratch, the `3^3` point scratch, two
  empty resolver maps, resolver-ID registries, or dependency-mask machinery.
  Dynamic shapes still return the same conservative unsupported result, and
  point capture still uses `getChunkNow` without loading a chunk.
- Removed production methods used only by tests: geometry component/combined-face
  convenience queries, the non-inverse pair-exchange wrapper, the signature
  region-count wrapper, and an unused solver bit lookup. Tests now inspect the
  returned structures or invoke the real production kernel directly.
- A second source-plus-classfile closure removed the write-only geometry summary,
  geometry/compiler diagnostic payloads, geometry effective ticks, Query medium/
  flags/topology state, radiation confidence/flags, arena cell flags, dead Air
  and material profile writes, unused source binding variants, and test-only
  buoyancy/material-array wrappers. `QueryPublication` double-buffer payload is
  reduced from `40` to `24` bytes per slot.
- Removed three geometry flags that no compiler path emitted and reduced
  class-internal constants from public to private. Thermal-scoped diff is
  `+503 / -2361`, net `-1858` lines; production thermal files decreased from
  `79` to `73`.
- `ThermalSourceMode.IMPULSE` and `DimensionInputAccumulator.emitSourceImpulse`
  remain because an older explicit plan decision retained the exact-tick signed
  joule contract, and the user reconfirmed that decision on `2026-08-30`.
  There is currently no gameplay producer. This is the sole non-Forge
  zero-production-caller exception and is an explicit whitelist entry, not dead
  code.
- Validation: Java 17 offline `compileJava`, `compileGameTestJava`, selected
  thermal JUnit (`96/96`), and Forge GameTest (`14/14`) passed in one Gradle
  run. Deleted type names have zero remaining compiled class files, and
  `git diff --check` passed.
- Final bytecode census covered every main classfile. Its `14` unreferenced
  top-level methods are exactly `7` Forge subscriber methods, `6` interface
  implementations invoked through their interfaces, and the retained impulse
  entrance. Source census reports no other zero-reference type, method, or field.
- Documentation impact: the living thermal architecture now records direct
  state-static capture, retained impulse semantics, and the reduced query
  payload. Gameplay formulas, cadence, Page lifecycle, continuous source power,
  solver, and temperature query behavior are unchanged.

## Nested Reachability And Allocation Closure

The earlier symbol census was exact for top-level methods but was not a semantic
reachability proof for nested records/enums or mutually referencing feature
branches. The following cleanup closes those confirmed gaps without changing
the currently registered gameplay profiles:

- [x] Remove unregistered stateless/natural-rock material models, deep poles,
  fixed material boundaries, unused snow/ice/custom phase actions, and their
  policy/handler branches. Production phase profiles continue to apply compiled
  `StateTransitionData` recipes and respect random-tick speed.
- [x] Replace `LocalAirRegionPattern` plus worker reconversion with one canonical
  `ConservativeAirGeometry.Resolution`; remove the five signature channels that
  were always zero in every production classifier.
- [x] Remove remaining handwritten test-only or zero-call production members:
  `ContactPattern.fullBlock`, `PageState.resolved`, buoyancy factor/status
  accessors, exchange-result status accessors, `SourceChannel.OTHER`, and the
  unused missing-port redistribution policy. `IMPULSE` remains whitelisted.
- [x] Replace the test-only critical/optional memory classification with one
  hierarchical publication/radiation byte budget. Bound source SoA growth with
  explicit per-dimension limits of `65,536` sources and `131,072` source nodes.
- [x] Make Section mutation bitsets and 27-entry Page directories lazy; use an
  adaptive dirty-center bitmap only for Pages that cross the small-array
  threshold.
- [x] Replace boxed arena free-span trees with a pooled primitive AVL index and
  make the hierarchical live-word summary track LIVE cells only.
- [x] Reuse one `TopologyView` for the plan lifetime and remove the duplicate
  old-span arena ownership traversal.
- [x] Narrow same-package runtime transport/configuration types and restore the
  scalar-only `FarFieldSettings` record.

Validation status: Java 17 production/test/GameTest compilation succeeded;
thermal JUnit passed `95/95` and Forge GameTest passed `14/14`. The final
thermal-scoped residual-name search and `git diff --check` also passed.

## Final Semantic And Payload Closure

The previous nested-reachability closure was incomplete. It proved symbol
reachability but did not re-derive mutation dependencies or prove that every
persisted operation field had a production reader. The following source-backed
items supersede the earlier claim that no further cleanup remained:

- [x] Replace the obsolete 27-center mutation halo with exact changed-position
  capture. `StateStaticThermalResolver` depends only on the captured
  `BlockState` and `FluidState`; cross-Brick effects remain the responsibility
  of `TopologyPlan.markFragmentNeighborhood`. Do not retain neighboring Page
  revision directories after this invariant changes.
- [x] Make `TopologyPlan.PageDraft.setBlock` compare against the current
  signature before allocating/copying a 64-entry Brick scratch. A final state
  equal to the installed state must not rebuild immutable signature payloads.
- [x] Remove all operation endpoint-generation arrays that have no production
  reader. Slot ownership remains proven once by topology preflight/commit and
  by reference-counted old-span release; do not add per-step validation.
- [x] Store fragment invariants once: one FarField owner Page, one lazy wind
  coefficient generation, and one production Air execution mode. Do not retain
  per-operation arrays for globally or fragment-constant values.
- [x] Remove production mode switches that exist only so tests can disable
  buoyancy or FarField. Tests must exercise the production physics path instead
  of selecting a simpler production branch.
- [x] Remove the unused `LongPairDouble` flag payload and boolean add argument.
- [x] Make physical-source capacity recovery lifecycle-complete: a source
  ignored at the explicit hard cap must become discoverable after a slot is
  released without an unbounded overflow collection or routine global scan.
- [x] Replace `ThermalResolution.Reason` and `SourceChannel` with the exact
  production data actually consumed, and narrow same-package transport members
  whose `public` modifier cannot widen the package-private owning type.

Acceptance remains one-pass: no compatibility layer, future-facing branch,
test-only production API, new routine traversal, production counter, or
allocation-heavy diagnostic is permitted. Complete all source edits before the
unified compile/JUnit/GameTest run, then update this checklist, living docs, and
the development diary with the measured result.

Outcome: completed on `2026-08-30`. Production/test/GameTest compilation passed;
thermal JUnit passed `96/96`, Forge GameTest passed `14/14`, the removed-symbol
and package-surface searches are clean, and `git diff --check` passed. Controlled
JFR/heap profiling remains the plan's separate performance-evidence gate.

## Arena And Transaction Readability Closure

The final field/data-flow review confirmed three non-constant write-only fields
and two dense ownership surfaces. This cleanup changes organization only; it
must not alter topology ordering, primitive array ownership, solver traversal,
or per-Brick retained object count.

- [x] Remove `BrickMaterialKernel.arena`, `TopologyPlan.parameters`, and
  `WorkerBrickTopology.fragment`, including their redundant constructor and
  `withFragment` arguments.
- [x] Move the reusable Brick layout builder to `ThermalBrickCellLayout` and
  move phase metadata/request arrays to one arena-owned
  `ThermalPhaseReservoirStore`. Both remain primitive reusable storage; no
  per-cell or per-request object is permitted.
- [x] Replace positional construction of `MaterialContacts` with one reusable
  builder owned by `BrickMaterialKernel`.
- [x] Replace the 14-argument `PreparedTopologyChange` construction with one
  reusable grouped builder owned by `TopologyPlan`; replace `PageWrite`'s
  positional call sites with named active/retirement factories. The committed
  payload remains one exact immutable delta with the existing primitive arrays.
- [x] Re-run production/test/GameTest compilation, thermal JUnit, Forge
  GameTest, field read closure, removed-symbol search, and `git diff --check`.
- [x] Extend the field closure beyond write-without-read detection: remove
  `MinecraftThermalInput.signatureCapture`, which was read only while wiring
  constructor-owned consumers and then retained redundantly for the dimension
  lifetime. The compiled constructor-only field set must also be empty.

This is not authorization to split `MinecraftPageManager` or redesign the
thermal architecture. It is a readability and dead-state closure inside the
current ownership boundaries.

Outcome: completed on `2026-08-30`. `ThermalCellArena` decreased from `1,163`
to `927` lines. The extracted stores and reusable builders add no per-Brick,
per-cell, per-request, or per-transaction grouping object. Production/test/
GameTest compilation passed, thermal JUnit passed `96/96`, Forge GameTest passed
`14/14`, the compiled non-constant write-only-field set is empty, removed-symbol
searches are clean, and `git diff --check` passed. A follow-up semantic closure
also reports zero fields whose only reads occur in their owning constructor.

## Door Mutation Hot-Path Closure

Source review on `2026-08-30` found two remaining constant-size costs on the
coalesced door/block path. This closure must use the existing ownership model
and may not add a position bitmap, collection, traversal, production probe, or
compatibility branch.

- [x] Preserve `SOURCE_MUTATION` through the section inbox as one cut-level
  boolean and skip `PhysicalSourceSpatialIndex.resyncBlock` for cuts containing
  only door, gate, trapdoor, or ordinary material mutations.
- [x] Clone the immutable 64-Brick `PagePublication` directory lazily only when
  an actual Brick query payload changes. Identity-only publication envelopes
  must share the existing private directory without exposing its array.
- [x] Run unified production/test/GameTest compilation, thermal JUnit, Forge
  GameTest, residual source checks, and `git diff --check`. Record allocation
  improvement only after a controlled JFR comparison.

Outcome: completed on `2026-08-30`. The section inbox gained one boolean and no
third position bitmap. Door-only cuts no longer call the physical-source block
resync path. Identity-only Page publications reuse the prior private Brick
directory; a real Brick payload change still performs one lazy shallow copy.
Production/test/GameTest compilation passed, thermal JUnit passed `96/96`,
Forge GameTest passed `14/14`, all new state has a production reader, and
`git diff --check` passed. A controlled door JFR remains required before
quantifying the CPU or allocation reduction.

## Restart-Scoped Thermal Tuning Configuration

The thermal gameplay coefficients are instance-wide development/modpack
tuning, not per-save state. They belong in Forge COMMON config and are frozen
once at server startup. This work must not add hot reload, a config revision,
hot-path `ConfigValue.get()`, or separate main/worker source profiles.

- [x] Add `FHConfig.COMMON.THERMAL_RUNTIME` with Air capacity/mixing, phase
  conductance/base energy, FarField conductance, and campfire power/radiation
  share using documented units and defaults.
- [x] Freeze those values in the existing server-wide gameplay profile snapshot
  and pass plain values plus one campfire profile to every dimension owner.
- [x] Keep runtime config reads out of solver/source/query/tick paths and use the
  same campfire profile in main-thread discovery and worker rebind.
- [x] Update focused tests, living docs, and run one unified production/test/
  GameTest validation plus residual/diff checks.

Outcome: completed on `2026-08-30`. Seven gameplay values now live in
`config/frostedheart-common.toml` and are read exactly once into the synchronized
server-start profile snapshot. Phase-only values remain preparation locals;
the retained tuning contains three scalar worker inputs and one immutable
campfire profile. Main-thread discovery and worker rebind receive that same
profile. No hot reload, config revision, tick polling, or hot-path
`ConfigValue.get()` was added. Production/test/GameTest compilation passed,
thermal JUnit passed `96/96`, Forge GameTest passed `14/14`, residual hardcoded/
config-read checks were clean, and `git diff --check` passed.

## Uniform Signature And Lazy Mutation Memory Closure

These are representation-only changes. They must not alter Page/Brick/component
ownership, topology dependencies, source classification, or the 20-tick cut.

- [x] Encode a uniform Brick with one compact/wide signature value while keeping
  direct O(1) lookup and existing 64-value payloads for nonuniform Bricks.
- [x] Allocate one section changed bitmap by default and a second non-geometry
  exception bitmap only after source-only positions require it, without adding
  a routine word scan.
- [x] Allocate Page center/signature mutation buffers at eight entries only on
  first use, then retain the existing geometric growth and bitmap promotion.
- [x] Run unified compilation, thermal JUnit, Forge GameTest, residual payload/
  field checks, and `git diff --check`; update living docs and diary.

Outcome: completed on `2026-08-30`. Uniform compact/wide Bricks now use
one-value arrays while nonuniform Bricks keep their existing 64-value payloads.
The section inbox swaps one changed bitmap with manager scratch and allocates a
second owner bitmap only after a non-geometry event occurs; the temporary word
summary was removed during minimality review. Page center/signature buffers are
absent until first mutation and start at eight entries. No class, cache,
collection, routine traversal, production probe, or compatibility path was
added. Production/test/GameTest compilation passed, thermal JUnit passed
`96/96`, Forge GameTest passed `14/14`, removed-state searches and
`git diff --check` passed.

## Dormant Thermal Storage Implementation Plan

- Status: `implemented; functional validation complete; controlled profiling pending`
- Added: `2026-08-30`
- Last reviewed: `2026-08-31`
- Author: `Codex; OpenAI GPT-5; primary design agent`
- Scope: `chunk-local Page temperature persistence, analytical unloaded cooling, pre-publication query fallback, and worker admission restore`

This plan adds storage to the existing Page/Brick/component model. It is not a
new thermal authority and must not change source, solver, topology transaction,
20-tick cadence, or live query ownership. The highest requirement is that a
loaded crop/soil query never observes natural-temperature fallback merely
because its restored Page is still waiting for asynchronous admission.

### Fixed Decisions

1. Store dormant temperature in chunk NBT, not a dimension `SavedData`, global
   map, custom region file, retained arena, or unloaded solver. Unloaded chunks
   consume disk only and cannot increase dimension heap or tick work.
2. Store Air-cell temperature residuals at the solver's existing Brick-local
   component resolution. Regular Air Bricks have one value. An exact mixed
   Brick stores its capacity-weighted mean first, followed by component values
   in deterministic component order. The mean is the fallback for changed
   geometry and the initial temperature for material poles; it avoids retaining
   old component volumes. Do not serialize topology, signatures, source
   bindings, material edges, solver fragments, arena slots, or Page lifecycle
   generations.
3. Keep exact component values while a Page has at most `256` Air components.
   Above that fixed bound, collapse each Brick to one capacity-weighted Air
   temperature. This bounds encoded work/storage while preserving exact normal
   houses; no configurable memory mode or alternate storage backend is allowed.
4. Quantize temperature residuals relative to the Page's captured natural Air
   temperature (`WorldTemperature.naturalAir` at the section center) to signed
   `1/16 C` fixed-point values. Never add an Air residual to a caller-supplied
   `naturalBlock` value. A Brick is omitted when every stored component is
   within `0.25 C` of natural Air. Values outside the signed-short range are
   clamped; invalid/non-finite samples retain the previous saved entry rather
   than replacing it with bad data.
5. Unloaded temperature approaches the current natural temperature by analytic
   half-life decay. Add exactly one COMMON setting:
   `FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds`, default
   `1800` seconds. Resolution, prune threshold, and component bound remain code
   constants so normal configuration exposes no encoding internals.
6. Source blocks/bound entities continue to use their existing persistence and
   fuel ownership. A loaded/force-loaded source keeps its Page live through the
   existing source reference. At a disk checkpoint, any enabled persistent
   `POWER_SOURCE` already observed by `PhysicalSourceSpatialIndex` may set one
   `sourceSustained` bit for its target section and fixed six-face neighbor set.
   This includes a lit campfire and an active generator, radiator, or fountain;
   `IMPULSE` is not persistent support. The bit freezes only the checkpoint's
   existing warm Brick vectors while the Page is unloaded: a Brick is warm when
   any of its exact components (or its sole/collapsed value) has a positive
   residual, and its mean plus all component residuals then share retention
   factor `1`. A Brick with no positive residual applies one common decay factor
   to its complete vector. This preserves the stored capacity-weighted mean
   relation without component-volume storage. It never integrates source power,
   raises a cold Brick, runs a heat network, or changes fuel. The
   generator's existing team-owned `GeneratorData` continues its independent
   unloaded fuel settlement. The bit is one-shot disk-load metadata, not a
   runtime-query or Page-capture policy: only main-thread activation immediately following
   `ChunkDataEvent.Load` may read it. That activation applies the Brick-vector
   rule above, rebases the entry to the load tick, clears the bit, and dirties
   the chunk once. Ordinary attachment queries, stale fallback, worker admission,
   and entries captured while their chunk is already loaded ignore the bit and
   use normal half-life decay. A live source/Page then resumes authority;
   otherwise cooling continues while loaded crops can tick. If generator fuel
   was exhausted during dormancy, this prevents retroactive crop/soil damage
   without granting indefinite warmth to a loaded dormant Page.
   Do not add source-owner UUIDs, activity counters, an offline-source policy
   hierarchy, or a persistent source graph for this retention rule.
7. Persist Air thermal state only. Material poles initialize from the restored
   Brick's capacity-weighted Air temperature instead of natural temperature;
   phase reservoirs restart with zero partial latent energy. BlockState already
   persists independently, and current farmland freezing is a passive ambient
   query path rather than the heating-only phase-reservoir path.
8. Dormant Air reconstruction is idempotent: it derives an absolute Air
   temperature from current Page natural Air plus the stored residual, ignoring
   the caller's fallback once dormant data is found. Before storage integration,
   fix the precomputed-temperature overload of `WorldTemperature.checkPlantStatus`
   so it consumes its supplied composed temperature exactly once instead of
   invoking `gameplayCropEnvironment` a second time.
9. Temperature-driven crop death has one owner. The custom ServerLevel
   temperature random-tick path applies the existing `heatCapacity` probability
   and performs the block replacement. `CropGrowEvent.Pre` may deny growth for
   `WILL_DIE` but must not replace the crop/farmland itself; otherwise it bypasses
   the probability and can kill every selected crop immediately after load.
10. A configured hot-side `StateTransitionData` transition remains owned by the
    new phase runtime while its Page is live or already stale/pending behind an
    existing handle. Random tick defers heating only for that bounded handoff.
    A dormant-only entry does not create Page interest or claim phase ownership;
    without a handle, legacy hot-side transition keeps its existing
    heat-capacity cadence. Cold-side freeze/condense also keeps its cadence and
    uses dormant-decayed temperature normally.

### Compact Chunk Format

Use one versioned root tag under the Frosted Heart chunk payload. Each nonempty
section entry contains only:

```text
sectionY             int
savedGameTick         long
sourceSustained       byte     one-shot support metadata consumed only after disk load
brickMask             long     Bricks with a retained residual
mixedMask             long     retained Bricks with componentCount > 1
componentCountMinusOne byte[]  unsigned 8-bit exact-mixed counts minus one
temperatureResiduals  long[]   signed 16-bit values, four per long
```

Values are ordered by ascending Brick index. A regular or collapsed Brick has
one capacity-weighted mean/sole value and no count entry. An exact mixed Brick
stores its capacity-weighted mean followed by its component residuals in
deterministic compiled-component order. Its component count is encoded as
`count - 1`; the exact-Page bound of 256 components makes one unsigned byte
sufficient even for a 256-component Brick. If any count, mask, or array length
is invalid, discard only that section entry and continue with natural fallback;
do not fail chunk load or server startup.

Only activation of a freshly decoded disk entry interprets
`sourceSustained=1`. For that one transformation, each retained Brick uses
exactly one factor for its mean and component vector. If its warmest component
residual is positive, the factor is `1`; otherwise the ordinary half-life factor
applies to every value in that Brick. Activation quantizes each transformed
vector, sets `savedGameTick` to the load tick, clears the bit, and installs a new
immutable entry before random ticks. Normal source discovery and fuel state then
resume authority. Every ordinary dormant query and worker admission treats the
installed entry as unsupported and applies normal half-life decay regardless of
any support bit later captured for a future NBT save.

The typical 64-regular-Air Page is about `150-300` raw bytes before region-file
compression. Exact mode is bounded to `256` component residuals plus at most one
mean for each of 64 Bricks and must remain below approximately `1 KiB` raw per
section. Collapsed mode stores at most 64 residuals. Empty/fully decayed sections
remove their tag on the next main-thread activation/save.
Old tags in chunks that are never loaded may remain on disk but consume no heap
and trigger no cleanup scan.

A regular/collapsed section (`mixedMask == 0`) reads its packed residual directly
with `brickMask` rank and `Long.bitCount`; it does not allocate a duplicate
`short[64]`. Only a section containing exact mixed Bricks derives one immutable
`short[64] warmestResidualByBrick` array. Both fallback paths remain O(1), while
the common regular Page avoids roughly 128-144 bytes of loaded heap.

### Ownership And Types

Add only these storage-specific production surfaces:

- `DormantChunkThermalState`: one lazy server-side chunk attachment owning
  validated section entries, fixed-point packing, decay, pruning, and NBT
  read/write. Page/query sampling remains in `MinecraftThermalInput`; this type
  owns no worker/publication traversal. It is the only dormant storage
  implementation; readability and single ownership are acceptance criteria,
  not an arbitrary compressed line-count target.
- `MinecraftThermalChunkAttachment`: a minimal getter/setter mixin contract on
  `LevelChunk`; the field remains `null` for chunks without dormant data.
- `ThermalInputBatch.DormantAirCut`: one nullable immutable admission payload
  referencing one immutable `SectionEntry` plus its frozen natural-temperature
  and decay inputs. It does not clone packed residual arrays. It is temporary
  worker input, not a retained Page authority.

Do not add a storage service, repository, manager hierarchy, codec framework,
capability provider, global chunk map, background task, scheduler, executor,
cache layer, per-Page lock, or compatibility format. `MinecraftThermalEvents`
owns the existing Forge chunk events; `MinecraftThermalInput` and
`MinecraftPageManager` own capture/query/admission integration.

Inside `DormantChunkThermalState`, store `minimumSectionY` plus one direct
`SectionEntry[]` sized to the owning chunk's section count. Allocate it only for
a chunk with decoded/captured dormant data. Section lookup is
`sectionY - minimumSectionY`; do not allocate a per-chunk hash map or linearly
scan NBT section entries on gameplay queries.

Async Load constructs and publishes the initial attachment once. `SectionEntry`
and its packed arrays are immutable, so a worker admission cut may share them
while the main thread later replaces the attachment slot. After the chunk future
reaches the server thread, all section replacement, decay-cache, prune, capture,
and NBT-save operations are server-thread-only; do not add locks, atomics,
concurrent collections, or mutable copy-on-write chunk state.

### Lifecycle

```text
ChunkDataEvent.Load
  -> async: decode tag into nullable LevelChunk attachment only
  -> no level lookup, PageManager access, config read, or chunk load

ChunkEvent.Load
  -> main thread: consume sourceSustained entries before random ticks
  -> retain warm Brick vectors, decay wholly non-positive vectors, rebase
  -> clear the bit and mark only changed attachments unsaved

live Page retirement (before handle publication is cleared)
  -> read current PagePublication + QueryPublication
  -> capture regular/mixed Air component temperatures
  -> replace that section entry with sourceSustained cleared
  -> mark the LevelChunk unsaved so an otherwise-clean chunk is written

ChunkDataEvent.Save
  -> best-effort refresh every live Page in that chunk
  -> if the source index is live, refresh support bits for nonempty entries
  -> prune analytically decayed entries
  -> write the attachment into the event NBT
  -> do not mark the chunk unsaved from inside Save

ServerStoppingEvent (before vanilla final save)
  -> capture every still-live Page into its already-loaded chunk
  -> refresh support bits while physical-source target buckets remain live
  -> mark only changed chunk attachments unsaved

query while Page/publication is unavailable
  -> getChunkNow only; never load a chunk
  -> if the handle has a last worker cut, sample that Brick coherently first
  -> otherwise use dormant section/Brick value with ordinary half-life decay
  -> never interpret sourceSustained outside fresh disk-load activation
  -> compose existing analytic fields afterward

Page admission
  -> freeze one nullable DormantAirCut at current natural temperature/tick
  -> worker initializes Air components and material poles from the cut
  -> first valid live publication becomes authoritative
  -> release the temporary cut reference after admission commit
```

`ChunkDataEvent.Load` is posted from async `ChunkSerializer.read`; its handler
may only validate primitive NBT and publish one attachment reference. Existing
full chunks arrive as a `LevelChunk` before Forge returns its
`ImposterProtoChunk`; ignore non-full ProtoChunks. `ChunkDataEvent.Save` runs in
the main-thread `ChunkMap.save` path. If its chunk is an `ImposterProtoChunk`,
resolve `getWrapped()` before attachment/Page lookup.

`ChunkEvent.Load` is the first main-thread activation boundary. Consuming a
source-sustained entry there is one bounded primitive pass over that entry, not
a block/section/world scan. Decode each Brick group once, choose its factor from
the warmest component, and apply that same factor to its stored mean and every
component. The transformed immutable entry is installed before crop or
state-transition random ticks can query it. Clearing the bit is persisted by one
`setUnsaved(true)` only when an entry actually changed.

`sourceSustained` is refreshed only for a disk checkpoint, not inherited from a
Page capture. A normal capture installs a cleared-bit entry. Ordinary
`ChunkDataEvent.Save` refreshes every nonempty stored section from the current
source target buckets immediately before encoding. Chunk unload, orderly stop,
and `MinecraftThermalInput.close()` perform the same refresh while the source
index is still live; a later Save with no active input uses that frozen bit.
This prevents a source that stopped after Page retirement from leaving stale
support in NBT. The bit is not consulted by the current attachment's sample/
admission methods. No extra runtime flag is required: the only reader is the
fresh-load activation method, and that method always replaces the entry with a
cleared-bit version before publishing it to normal main-thread consumers.

Normal Page retirement handles teleports and interest expiry while a chunk
remains loaded. Forge unload posts `ChunkEvent.Unload` before `ChunkMap.save`,
so retirement capture plus `setUnsaved(true)` makes the following save include
the new tag. `ChunkMap.save` clears the dirty flag before posting
`ChunkDataEvent.Save`; the Save handler must therefore never set it again.

Current production unload order removes `PhysicalSourceSpatialIndex` entries
before `MinecraftPageManager` retirement. Storage integration must reverse that
order: checkpoint/retire the chunk's Pages (including continuation releases),
refresh every nonempty stored section's support bit while the exact source target
buckets are still present, then call
`PhysicalSourceSpatialIndex.beforeChunkUnload`. This preserves cross-section/
cross-chunk continuation capture without a dimension or source-registry scan.

`ChunkDataEvent.Save` opportunistically refreshes active Pages when another
chunk mutation already caused a save. `ServerStoppingEvent` is the explicit
checkpoint for active but otherwise-clean chunks before orderly shutdown. A JVM
crash may lose changes since the last retirement/save and is outside this
contract.

`LevelEvent.Unload` also checkpoints its active Pages before
`MinecraftThermalInput.closeActiveLevel` clears publications. During orderly
server shutdown, the earlier `ServerStoppingEvent` checkpoint owns disk
durability; the later level-unload checkpoint is idempotent and primarily covers
non-shutdown dimension lifecycle.

`MinecraftThermalInput.close()` checkpoints active Pages before closing
Page/query/mailbox ownership and refreshes disk support before closing the source
index. This preserves heat across recipe/data-pack reload,
whose current invalidation path calls `closeAll()` while chunks remain loaded.
`restartWorker()` likewise freezes the last coherent query cut into loaded chunk
attachments before replacing `QueryPublication`, then reseeds admissions from
those in-memory dormant cuts. Neither path requires an NBT save/load round trip.

Capture uses the common `QueryPublication.sampleTick` of the accepted component
samples as `savedGameTick`, not the later retirement/save tick. If
`currentPublication()` is stale because a geometry mutation is in flight, use
a narrowly named read-only `ThermalPageHandle` accessor for the last non-empty
worker publication only for dormant capture. Live gameplay queries must remain
strict. If no worker publication has ever existed, retain the previous dormant
entry.

Component capture is one coherent checkpoint. Read the same Page publication
before and after the scan and accept it only when every component sample reports
one `QueryPublication.sampleTick`. Retry the Page-local scan at most once if a
worker publication races it; a second mismatch preserves the previous dormant
entry. Do not lock the worker or combine values from different solver cuts.

### Query And Restore Rules

1. Live `PagePublication + QueryPublication` always wins. The steady live query
   path performs no NBT lookup, decay calculation, allocation, or extra map
   traversal.
2. If a handle exists but strict live publication is temporarily stale due to
   geometry/resync, try its last non-empty worker publication as a temperature-
   only fallback before chunk storage. Sample the addressed Brick's warmest
   component and require the same publication before/after; retry once on a
   race. Do not use stale geometry for exact point ownership.
3. If no coherent handle cut is available, query the loaded chunk attachment before using
   natural temperature. This fallback must also work when
   `MinecraftThermalInput` has not started yet, so crops cannot run once against
   cold natural fallback before a real source/frontier starts live residency.
   Reconstruct against current section-center `WorldTemperature.naturalAir`,
   not the `naturalBlock` argument supplied by crop/block callers.
   Attachment sampling always applies ordinary half-life decay and never reads
   `sourceSustained`; that bit belongs exclusively to fresh disk activation.
4. Dormant fallback has no current mixed-geometry point mapping. It returns the
   warmest stored component in the addressed Brick, decayed toward current
   natural temperature. This intentionally avoids destructive false-cold
   results during the bounded admission window. Exact point lookup resumes with
   the first live publication. The warmest-component approximation may
   temporarily overprotect a mixed Brick, but it decays normally and is chosen
   over an irreversible false-cold crop/soil mutation.
5. Cache one decay factor per dormant section per aligned 20-tick boundary.
   Regular/collapsed lookup uses packed rank directly; exact mixed lookup uses
   the lazily derived `warmestResidualByBrick`. Dormant crop queries therefore
   execute O(1) primitive lookup and arithmetic; they do not call `Math.exp` per
   crop or block query.
6. Worker restore uses exact component ordinal only when the stored and compiled
   component counts match. A mismatch initializes every current component from
   that Brick's stored capacity-weighted mean. Missing Bricks initialize from
   the admission's Page natural temperature.
7. Replace the dimension-start Air initialization for new Page cells with Page
   natural/dormant initialization. If this leaves
   `ThermalTopologyParameters.initialAirTemperatureC` unread, delete it and its
   constructor argument rather than retaining a compatibility field.

Fresh disk-load activation is:

```text
elapsedTicks = max(0, currentGameTick - savedGameTick)
factor       = 2 ^ (-elapsedTicks / (halfLifeSeconds * 20))
brickWarm    = warmestComponentResidualInBrick > 0
effective    = sourceSustained && brickWarm ? 1 : factor
rebasedResidual_i = quantize(residual_i * effective)
savedGameTick = currentGameTick
sourceSustained = 0
```

`effective` is selected once per Brick and shared by its stored mean and every
exact component. Independent per-value sign decisions are prohibited because
they would make the stored mean cease to represent the component vector.

After activation, and for every entry captured while already loaded, ordinary
query/admission decay is always:

```text
elapsedTicks = max(0, currentGameTick - savedGameTick)
factor       = 2 ^ (-elapsedTicks / (halfLifeSeconds * 20))
temperature_i = currentNaturalTemperature + residual_i / 16 * factor
```

No normal query/admission branch reads `sourceSustained`.

World downtime does not advance game time and therefore does not cool chunks;
only elapsed server simulation ticks count.

### Performance And Memory Contract

- Live steady tick/solve/source/topology cost: unchanged.
- Live successful query: unchanged except the already-required availability
  branch; dormant storage is consulted only after live lookup fails.
- Capture/save: one `O(64 + A)` pass for the retiring/saved Page's `A` actual
  Air cells. Exact output retains at most 256 component values plus one mean per
  retained Brick; larger Pages retain at most 64 Brick means during that same
  Page-local pass. No unrelated Page, arena-high-water, source-registry, or
  dimension-history scan is permitted.
- Restore/admission: `O(C)` within the existing Page compile; no second Page
  topology pass, residual-array clone, or per-component object.
- Dormant query: O(1) section/Brick lookup using either direct packed rank or one
  mixed-only derived-array read; one exponential calculation per Page per
  20-tick boundary.
- Source-sustained main-thread activation: one `O(V)` pass over only that
  section's stored residual stream, typically 64 values and bounded by 256 exact
  components plus at most 64 mixed-Brick means. It runs once per chunk load, not
  per tick/query, and scans no blocks, other Pages, sources, or arena slots.
- Loaded chunk without dormant data: one nullable attachment reference and no
  state allocation. Unloaded chunk: zero dormant heap.
- Network: zero bytes. Dormant state remains server/chunk authoritative and is
  never synchronized to clients.
- No periodic save scan, global TTL sweep, cleanup thread, production counter,
  probe, debug payload, or test-only API.
- The orderly-stop checkpoint may traverse the exact active Page entries once;
  this is shutdown-only work and cannot enter ordinary save, tick, mutation, or
  query paths.
- Disk-checkpoint source lookup performs at most seven O(1) map probes per
  nonempty stored section against the existing target-section source index (own
  section plus six faces) and inspects only sources in matching buckets. It runs
  only during Save/unload/stop/close, never Page retirement or query, and may not
  scan a dimension or source registry.

### Review Findings Before Implementation

1. `ClimateCommonEvents.beforeCropGrow` can replace a crop with a dead bush on
   the first `CropGrowEvent.Pre` whose temperature reports `WILL_DIE`; it has no
   admission grace. `ServerLevelMixin_TemperatureUpdate` can likewise freeze a
   liquid state on its first selected temperature random tick. Attachment load
   and no-runtime dormant fallback are therefore correctness requirements, not
   optional visual smoothing.
2. The precomputed-temperature overload of
   `WorldTemperature.checkPlantStatus(..., float blockTemp)` currently calls
   `gameplayCropEnvironment` again, while the custom random-tick path already
   passed a composed value. Fix this double sampling before dormant fallback so
   analytic fields and residuals are never applied twice.
3. Page environment capture uses section-center `naturalAir`, while crop and
   block callers supply `naturalBlock`. Dormant residual decode must use the
   former independently; otherwise soil/crop fallback reconstructs the wrong
   physical quantity.
4. Forge posts `ChunkDataEvent.Load` from async deserialization. Only attachment
   decode/publication is allowed there. Page/source/world work stays in existing
   main-thread load/admission paths.
5. Forge saves only dirty chunks and clears the dirty flag before
   `ChunkDataEvent.Save`. Retirement/storage changes must mark dirty before
   save; Save itself must not, and orderly shutdown needs a pre-save checkpoint.
6. A geometry mutation intentionally makes `currentPublication()` unavailable.
   Dormant capture needs the last worker cut as a storage approximation or it
   can lose the only warm snapshot during rapid mutate-and-teleport churn.
7. Full loaded chunks can be wrapped as `ImposterProtoChunk`. Load/save handlers
   must target the wrapped `LevelChunk`; generated/incomplete ProtoChunks never
   allocate dormant state.
8. Do not remove the dormant attachment on successful admission. Keep the last
   disk checkpoint until a later retirement/save replaces it, so a worker
   failure before first live publication cannot erase the only fallback.
9. `CropGrowEvent.Pre` currently performs immediate destructive replacement in
   its `WILL_DIE` branch after the custom temperature random tick already had a
   chance to apply `heatCapacity`. Make the event deny growth only and retain
   the random-tick temperature path as the single gradual-death owner.
10. Query publication may advance during a multi-component save scan. Require
    one Page reference and one sample tick across the cut, retry once, and keep
    the old entry on a second race rather than storing a torn thermal frame.
11. A live handle can be temporarily strict-null on every door/block topology
    mutation. Prefer its coherent last worker cut over an older chunk snapshot;
    otherwise storage would reintroduce a temperature discontinuity during the
    normal 20-tick rebuild window.
12. Recipe reload closes all active inputs, and `ENGINE_FAILED` replaces the
    dimension worker/query publication. Both paths currently reset runtime heat
    unless they checkpoint the last coherent cut before closing old ownership.
13. `ownsGameplayHeatingTransition` currently returns false while an admitted
    Page is pending/stale before phase candidates republish. That bounded window
    can bypass latent energy. Extend deferral to an existing handle, but do not
    make dormant storage alone retain/admit a Page or block legacy transition
    indefinitely.
14. Treating every unloaded source as inactive would cool an already warm home
    across logout/restart even though crops and soil did not tick. The same
    player-facing rule must cover every enabled persistent `POWER_SOURCE` already
    known at retirement, including generator and radiator, not only a lit
    campfire. At the disk checkpoint, persist one section bit and hold only Brick vectors containing
    existing positive residuals;
    do not simulate unloaded heat transfer, integrate source joules, duplicate
    `GeneratorData` fuel settlement, or add per-source dormant records.
15. A permanent `sourceSustained` bit would also preserve warmth after a chunk
    becomes ticking while its Page remains dormant; passive crop queries do not
    create Page interest. Consume and rebase the bit at main-thread chunk load so
    the unloaded interval is protected but a loaded fallback cannot remain warm
    forever without a live source/Page. Page retirement, save, reload, or worker
    replacement can capture an entry while the chunk remains loaded, so normal
    sampling/admission must ignore the bit even before the next disk round trip.
16. Exact component temperatures alone cannot produce the old capacity-weighted
    Brick mean after a component-count mismatch because old component
    volumes are intentionally not serialized. Store the mean once at capture;
    do not add old topology, volume arrays, or a second capture pass.
17. The exact-Page bound is 256 components. Encoding `componentCount - 1` needs
    one unsigned byte, not 16 bits. NBT already owns `byte[]`, so use it directly
    instead of packing counts into `long[]`; regular/collapsed Pages need no
    count entry.
18. A universal `short[64]` fallback table duplicates every common regular Page.
    Keep direct packed rank lookup for `mixedMask == 0` and allocate the table
    only for exact mixed data. Admission shares immutable packed entries rather
    than cloning them.
19. Applying source retention independently by residual sign can make a stored
    mixed-Brick mean diverge from its component vector. Select retention/decay
    once from the Brick's warmest component and apply the same factor to its
    mean and all components; do not store component volumes or add another pass.
20. `sourceSustained` cannot be a general attachment-query condition. Pages may
    retire or be checkpointed while their LevelChunk remains ticking, in which
    case no `ChunkEvent.Load` will consume a newly captured bit. Make fresh-load
    activation its sole reader; all normal query/admission paths use standard
    half-life decay without another flag or state machine.
21. A support bit chosen at ordinary Page retirement can become stale while its
    chunk remains loaded and the source later stops. Page capture therefore
    clears the bit. Refresh it only at Save/unload/stop/close while the current
    source target index still exists; this is bounded by stored sections and
    removes source lookup from ordinary retirement.

### Failure And Edge Handling

- Unknown format version, malformed counts, array mismatch, non-finite decoded
  values, or out-of-range section Y discards only the affected dormant entry.
- Game-time rollback uses zero elapsed time. Very large elapsed time naturally
  underflows the residual to zero and prunes the entry.
- Geometry/config/datapack changes may alter component counts; restore uses the
  Brick mean rule and never rejects Page admission.
- The root format version also owns compiled component-ordinal semantics. If a
  future compiler change can reorder equal-count components, bump that version
  and discard old exact entries; do not add per-Brick hashes or a migration
  framework for transient temperature data.
- A missing/unloaded chunk is never loaded for storage or fallback. Consumers
  retain their current natural-temperature behavior when no loaded state exists.
- Repeated unload/reload overwrites one section entry; it cannot append history
  or grow by source generation, Page lifecycle, or mutation count.

### Implementation Checklist

- [x] Add the one half-life COMMON config and retain it in the existing immutable
  thermal tuning snapshot; no hot reload.
- [x] Implement `DormantChunkThermalState` packing, NBT validation, decay cache,
  pruning, exact/collapsed capture, stored mixed-Brick means, native `byte[]`
  count-minus-one storage, lazy mixed-only warmest lookup, and zero-copy
  immutable admission cut.
- [x] Clear source support on ordinary Page capture. At Save/unload/stop/close,
  refresh one bit for every nonempty stored section from enabled persistent
  `POWER_SOURCE` entries in the existing target-section index and fixed
  six-neighbor closure while that index is still live. Cover campfire, generator,
  radiator, and fountain uniformly; exclude `IMPULSE`. Preserve existing warm
  Brick vectors without offline energy, fuel accounting, owner IDs, or history.
- [x] Consume source-sustained entries at main-thread `ChunkEvent.Load`: bake
  each Brick-vector factor once from its warmest component, transform the mean
  and all components together, rebase to the load tick, clear the bit, prune,
  and dirty only changed chunks. Verify a ticking dormant Page cannot retain the
  unloaded grace indefinitely and mixed means remain consistent.
- [x] Make the fresh-load activation method the only production reader of
  `sourceSustained`. Ordinary attachment sampling, last-publication fallback,
  `DormantAirCut`, recipe reload, and worker restart must ignore it and use the
  standard half-life formula; add no parallel runtime support flag.
- [x] Add the lazy `LevelChunk` attachment and `ChunkDataEvent.Load/Save` wiring.
- [x] Keep async Load limited to primitive decode; unwrap full
  `ImposterProtoChunk` saves and ignore incomplete/generated ProtoChunks.
- [x] Capture before Page retirement clears the handle and refresh active Pages
  during chunk save without loading chunks or reading Minecraft state off-thread;
  use query sample tick, storage-only last publication, and correct dirty-flag
  ordering.
- [x] Reverse the current unload integration order so Page/continuation capture
  and stored-section support refresh run while physical-source target buckets
  are still present, followed by
  `PhysicalSourceSpatialIndex.beforeChunkUnload`; add no global recovery scan.
- [x] Capture/mark still-live Pages during `ServerStoppingEvent` before final
  save and before non-shutdown `LevelEvent.Unload`; retain the last attachment
  across admission and worker failure.
- [x] Checkpoint before `MinecraftThermalInput.close()` and `restartWorker()` so
  recipe reload and terminal worker replacement reuse in-memory dormant cuts
  rather than natural-temperature admission.
- [x] Add dormant fallback before natural fallback for player/passive crop/town
  queries, including the no-active-runtime case; live publication remains first.
- [x] Add coherent last-worker-cut temperature fallback for a present but stale
  Page handle before consulting chunk dormant state; never use it for exact
  stale geometry ownership.
- [x] Make dormant Air reconstruction independent of caller `naturalBlock` and
  remove the existing double `gameplayCropEnvironment` call from the
  precomputed crop-status overload.
- [x] Remove destructive mutation from `CropGrowEvent.Pre` `WILL_DIE`; deny
  growth there and leave heat-capacity-controlled death to the existing custom
  temperature random tick.
- [x] Extend heating-transition deferral to an existing pending/stale Page handle
  until phase publication; dormant-only storage must not create interest or
  suppress legacy hot/cold transition cadence.
- [x] Carry the nullable cut through `PageAdmission`, initialize Air/material
  cells, clear it after commit, and remove obsolete dimension-start Air state.
- [x] Update living architecture, heat/source, lifecycle/persistence, crop/player
  consumer documentation, plus one completed development diary entry.
- [x] Run production/test/GameTest compilation, focused thermal JUnit, Forge
  GameTest, dormant half-life/exact/mismatch/source/NBT result tests,
  residual-field/dead-surface checks, and `git diff --check`.
- [ ] Measure encoded NBT sizes externally and run controlled quick-teleport,
  server-restart, crop/soil, door/source churn, 100-player-like loaded-chunk JFR,
  and long heap workloads. Do not add production counters for validation.

### Acceptance Scenarios

1. A warm enclosed farm Page retires, reloads before one half-life, and the very
   first `CropGrowEvent.Pre` and temperature state-transition random tick observe
   dormant-decayed Air rather than natural fallback; no crop death/freeze occurs
   solely during admission.
2. After exactly one configured half-life, unsupported positive and negative
   temperature residuals are halved within fixed-point tolerance. Every value in
   one source-sustained Brick vector uses the same factor.
3. A mixed Brick stores one capacity-weighted mean followed by exact component
   temperatures. Matching version/count restores distinct components; a count
   mismatch and material-pole initialization use the stored mean without old
   topology or volume data. A format-version ordinal mismatch discards exact
   data under the stated version rule.
4. A Page above 256 Air components writes bounded collapsed data and reloads
   without exceeding the per-section storage/work contract.
5. Active live publication replaces dormant fallback without a discontinuous
   natural-temperature frame. Page retirement, re-admission, and server restart
   do not expose stale arena slots or lifecycle identities.
6. Repeated source creation/removal and Page churn overwrite bounded chunk
   entries. After chunk unload, retained thermal heap is independent of the
   number of historical heated chunks.
7. A clean warm chunk becomes dirty on Page retirement and persists its tag on
   the immediately following unload save. `ChunkDataEvent.Save` does not leave
   it dirty again; an orderly server stop checkpoints a still-live clean Page.
8. Async full-chunk load publishes the attachment without level interaction;
   ProtoChunk data is ignored, Imposter saves use the wrapped LevelChunk, and
   the first main-thread random tick sees the decoded fallback.
9. A precomposed crop temperature is consumed once. Dormant decode against
   current natural Air is idempotent even when legacy callers pass natural block
   temperature or an already composed value.
10. A cold restored crop is denied growth immediately but dies only through the
    existing heat-capacity random-tick probability; `CropGrowEvent.Pre` cannot
    bypass that cadence. Soil transition keeps its recipe heat-capacity cadence.
11. A worker publication racing component capture either yields one coherent
    sample tick after one retry or leaves the previous dormant entry unchanged;
    no stored Page mixes solver cuts.
12. Opening/closing a door makes strict publication temporarily unavailable but
    crop/player/block temperature uses one coherent last worker cut, not natural
    temperature or an older chunk checkpoint. The fallback disappears with the
    next live publication.
13. Recipe reload and injected terminal worker failure preserve the last
    coherent Air temperatures through close/reseed; neither path exposes a
    natural-temperature frame or requires the chunk to unload first.
14. Warm permafrost behind an existing pending/stale handle waits for phase
    publication and then resumes from zero partial latent progress. A
    dormant-only Page follows legacy heat-capacity transition without creating
    hidden Page lifetime or background solver work.
15. An already warm Page with a lit campfire or active generator/radiator/
    fountain retains its positive checkpoint across overnight logout and
    orderly restart in its target/one-ring Page closure; an identical Page
    without a supported source follows half-life decay. A cold supported Page
    does not heat while unloaded. `GeneratorData` continues its existing
    independent unloaded fuel consumption. Main-thread chunk activation
    preserves the unloaded positive checkpoint once, consumes the support bit,
    and then either hands authority to the live source/Page or begins ordinary
    half-life cooling; a ticking dormant fallback cannot remain warm forever and
    no retroactive crop or soil mutation is applied for the unloaded interval.
16. A regular/collapsed dormant Page performs direct packed O(1) Brick lookup
    without a duplicate 64-short table. Exact mixed data derives the table once.
    Admission observes the same immutable packed entry without copying its
    residual arrays.
17. A support bit refreshed by a disk checkpoint while the chunk remains
    ticking has no effect on current fallback/admission temperatures. Page
    capture clears it, and each disk checkpoint refreshes it from current source
    buckets. After an actual disk reload it is consumed exactly once before
    random ticks, and the cleared/rebased entry is the only version visible to
    normal queries.

Outcome on `2026-08-31`: the chunk-local implementation is complete. Production,
test, and GameTest source compilation passed; focused thermal JUnit passed
`99/99`; Forge GameTest passed `14/14`; and `git diff --check` passed. The full
repository `build` recompiled production and ran 687 tests but remains blocked by
the unrelated `TeamTownActualSaveCodecProbeTest` missing its external save file.
Controlled storage-size, teleport/restart, crop/source JFR, and long heap evidence
remain the only unchecked acceptance item.

## Post-Implementation JFR Closure

The first 120-second storage run
`run/thermal-dormant-storage-20260831-005633-120s.jfr` found three local costs;
the fixes below are implemented and functionally validated. Percent improvement
remains unclaimed until the same workload is recorded again.

- [x] Reuse the existing loaded-section `SectionOwner` to obtain its `LevelChunk`
  for active-runtime dormant fallback. One owner lookup supplies Page and chunk;
  only owner-unavailable cold start uses `getChunkNow`. Remove routine Optional,
  Either, and CompletableFuture lookup without adding a map.
- [x] Carry current `PageState + nextSignatures` through one Brick compile and
  bypass `TopologyView` section/slot hashes for same-Page Air adjacency. Retain
  hash lookup only for actual cross-Page references; delete the superseded
  coordinate-only overload.
- [x] Remove redundant `StateTransitionData.heatingTransition(state)` creation
  from `ownsGameplayHeatingTransition`; the compiled phase-profile lookup already
  owns eligibility.
- [x] Re-run production/test/GameTest compilation, thermal JUnit (`99/99`), Forge
  GameTest (`14/14`), dead-overload search, and `git diff --check`.
- [ ] Repeat the controlled 120-second JFR and compare dormant Server-thread
  samples, same-Page topology hash samples, and phase ownership allocation.

## Cross-Section Brick Residency Review Closure

- Time: `2026-09-01 16:57:05 +08:00`
- Status: `implemented; user validation pending`
- Scope: `cross-section Brick admission, work-limit capture lifetime, phase hot-state ownership, and residency hot path`

### Confirmed Findings And Outcome

- [x] A refused cold admission previously retained captured signatures after its
  handle was removed, although later mutation and chunk replacement had no live
  handle to invalidate them. Backoff now retains only committed worker state and
  the pending request; retry recaptures current Minecraft state.
- [x] An unavailable target previously re-entered the admission queue after each
  failed `getChunkNow`. It is now parked in the existing Page/chunk index and is
  enqueued only by an exact chunk-load event or a new interest change.
- [x] Phase reservoirs previously contributed their fixed transition temperature
  to the Brick hot mask even at zero stored energy. Only positive reservoir
  enthalpy now contributes; the existing phase contact returns unreserved energy
  to colder Air so partial progress can cool to zero, and its existing residual
  loop prevents sleep while that reverse transfer remains active.
- [x] The fused live-slot path now derives local Brick coordinates with the exact
  power-of-two mask instead of three `Math.floorMod(..., 16)` operations.
- [x] Residency diff authority previously compared section masks without Page
  lifecycle identity. A reusable primitive admission list now forces exactly one
  current-mask completion for each committed lifecycle, including zero, and is
  cleared immediately after publication.
- [x] Source removal previously retired a sealed/in-flight admission before its
  first publication could be checkpointed. Mutable admissions now cancel;
  in-flight lifecycles retain their handle and queue the latest source seed until
  their mandatory first residency completion. Uninterested work-limited
  admissions are discarded immediately.
- [x] A lazy runtime could start after a chunk containing an already-lit
  campfire had loaded; refueling changed only BlockEntity data, so neither source
  discovery path ran and its frontier Bricks stayed absent. Existing lit
  `cookTick` now calls `MinecraftThermalInput.onCampfireTick` once per
  position-staggered 20 ticks. Unchanged observations remain O(1) and do not
  enqueue source, Page, or worker work.
- [x] A retained no-interest lifecycle could leave `queuedPriority` set after its
  queue entry was consumed. Interest loss now dequeues immediately, every queue
  pop clears metadata before state filtering, and Page removal reuses the same
  helper.
- [x] No Page/Brick field, map, index, cache, message field, threshold, solver
  pass, or compatibility path was added.

### Validation

- Production references and the final diff are reviewed locally in this change.
- Automated tests are intentionally not run in this change; user validation and
  the existing controlled 100-source JFR/heap gate remain pending.

## End-To-End Correctness And Cost Closure

- Time: `2026-09-01`
- Status: `implemented; user validation pending`
- Scope: `phase migration, terminal persistence, sparse dormant query, commit allocation, Brick migration cost, radiation capacity, infrared retained memory, and production test surfaces`

- [x] Migrate phase enthalpy from the old reservoir slot together with its
  request reservation. Do not validate this through a synthetic numeric kernel
  fixture. `phaseRequestSurvivesSameBrickTopologyChurn` drives a real packed-ice
  transition through `MinecraftThermalInput`, a public generator source,
  same-Brick trapdoor mutations, mailbox/worker completion, and main-thread ACK;
  it observes only the resulting world BlockState.
- [x] Keep a failed engine quiescent and readable until the main thread captures
  its last coherent Page cut. Terminal ACK closes it before the replacement
  generation reuses handles.
- [x] Let a sparse Page Brick with no published signature payload use its dormant
  checkpoint; compiled no-Air points retain natural fallback.
- [x] Reserve the admission identity list during preparation for the exact batch
  admission count, leaving authoritative commit allocation-free.
- [x] Reuse geometrically grown Brick migration scratch, skip new/no-dormant
  migration, hoist mixed signature lookup to one per block, and use 64 Air-mask
  intersections for regular-to-regular migration.
- [x] Align physical witness and static coverage capacity at 3,200 sections;
  share one lava radiation profile and invalidate the singleton classification
  cache with one reload epoch.
- [x] Allocate server infrared epoch arrays and the client 144-cubed direct
  mirror only after first use. CPU buffers remain one bounded reusable
  allocation; reset detaches and deletes captured GPU resources without
  confusing full-sync eligibility with render availability.
- [x] Remove the flat Page-signature builder, alternate static-profile
  classifiers, and four-argument query publication overload that existed only
  for tests. Tests now construct through Brick-level production paths.
- [x] Leave `SurroundingTemperatureSimulator` and its retained legacy cluster
  unchanged by explicit user direction.
- [x] Java 17 `runGameTestServer` passes all `16/16` required real-world tests,
  including `phaseRequestSurvivesSameBrickTopologyChurn`; `compileTestJava`
  also passes after production API cleanup.
- [ ] Run the existing controlled JFR/heap/network gates before changing the
  plan status; numeric JUnit is not acceptance evidence for these fixes.

## Five-Finding Post-Closure Correction

- Time: `2026-09-01`
- Status: `implemented; automated validation passed, live infrared validation pending`
- Scope: `terminal publication coherence, terminal close recovery, regular Brick migration, phase RETRY GameTest, infrared render validation`

- [x] Prepare `QueryPublication` in its inactive buffer and reserve the seqlock
  write window for the final exchange. If a committed topology fails before
  that exchange, restore only the affected `ThermalPageHandle` publication
  references to their prior immutable cut; do not clone or roll back solver
  state that belongs to the terminal generation.
- [x] Let terminal ACK finish mailbox cleanup even when `processor.close()`
  throws. `MinecraftThermalInput` logs that cleanup failure, clears `inFlight`,
  and starts the replacement generation instead of leaving the dimension stuck.
- [x] Preserve temperature directly across regular-to-regular single-cell Brick
  replacement using the new cell capacity. This path is O(1); only mixed
  geometry performs signature/microcell overlap work.
- [x] Make `phaseRequestSurvivesSameBrickTopologyChurn` hold
  `randomTickSpeed = 0` during real generator heating and same-Brick trapdoor
  churn, forcing production phase application to return `RETRY`; restore the
  gamerule before requiring the actual world BlockState transition.
- [x] Delete `InfraredViewRendererStateTest`. Reflection over private flags did
  not execute packet scheduling, GPU upload, shader sampling, or section
  movement and therefore was not evidence. Keep live client rendering as the
  actual gate rather than adding a production test seam or another synthetic
  state test.
- [x] Java 17 production/JUnit/GameTest source compilation passes. Focused
  `QueryPublication`, mailbox, engine, and topology suites pass `32/32`.
  The final unchanged-code Forge run passes all `16/16` required GameTests,
  including the forced-RETRY phase churn path; one earlier run exposed the
  existing timing-sensitive residency-handoff test once, and its immediate
  unchanged rerun passed. `git diff --check` passes. Actual client infrared
  rendering remains the non-synthetic external gate.

## Dormant Neighbor Persistence Correction

- Time: `2026-09-01`
- Status: `implemented; automated validation passed, user live validation pending`

- [x] Write source support together with each coherent Page capture instead of
  relying on a later scan of active Page chunks. Source target/power/enabled
  changes update only existing dormant entries in the exact target plus six-face
  closure; shutdown, chunk history, and total loaded sections are not scanned.
- [x] Retain consumed load-time source support in a lazy transient bitset until
  live source discovery updates that section. Initial infrared full no longer
  races campfire or machine BlockEntity registration.
- [x] On full infrared responses only, encode current-source-supported dormant
  Brick means through the existing `UNIFORM` record as temporary bootstrap. Do
  not add them to presence/delta epochs, admit Pages, load chunks, or affect the
  stable poll; real admission replaces them with block-exact values.
- [x] Final production/test compilation and focused dormant-state tests pass
  `4/4`. Actual save inspection, not an unrelated GameTest, proves the affected
  face-neighbor entry is stored, source-supported, mixed, and now selected by
  the full-response Brick-mean path. Live client rendering remains the gate.
