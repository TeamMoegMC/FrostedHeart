# Thermal Async Runtime And Topology Refactor Plan

- Time: `2026-08-28 01:18:39 +08:00`
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
- The class-size targets under `Ownership-Sized Class Layout` are hard gates.
  Moving unchanged code into extra files does not satisfy them. A helper is
  permitted only when it owns a real immutable payload or reusable numeric/data
  structure kernel; it must remove duplication or a distinct reason to change.
- The final replacement scope must not have positive net production Java LOC
  against the production classes it deletes and replaces. Tests, docs, and
  generated files are excluded. If correct final behavior would require net
  growth, implementation stops for explicit user approval with an itemized
  ownership and cost proof; line formatting or mechanical file splitting is
  not a justification.
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
6. The main thread is the sole writer for Minecraft capture state, Page admission interest, exact-position mutation accumulation, physical-device observations, radiation integration, and application of phase mutations to the world.
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
- Page admissions: `ThermalPageHandle`, capture revision, natural temperature, 256 sky-column bytes, and owned immutable Page signature cut;
- Page retirements: section key and lifecycle generation;
- geometry deltas grouped by Page identity: capture revision plus sorted local indices/signature IDs, or one owned full Page cut;
- environment deltas grouped by Page identity: natural temperature and sparse sky columns;
- source observations: source identity/generation, primitive anchor coordinates, profile enum ID, power, enabled/present state, and effective tick;
- phase acknowledgements: exact reservoir/request sequence and outcome; batch sequence prevents replay, while any resulting world geometry mutation enters the normal exact-tick geometry cut;
- global FarField conductance scale when changed;
- close/cancel is a mailbox lifecycle operation, not a fake input frame.

Arrays are exact-sized at sealing. Normal door/gate/trapdoor changes retain exact local positions; no permanent Page bitmap is introduced.

There is no `InputWatermarks`, `SealedInputFrame`, or five-stream cut vector. Mailbox serialization is the ordering proof: the engine accepts only its dimension generation and `lastBatchSequence + 1`, and requires `targetTick >= lastTargetTick`. Page lifecycle/revision, source event tick/generation, and phase request sequence validate their own payloads. A profile reload creates a new engine generation instead of adding another watermark stream.

### Cadence And Backpressure

- Geometry, environment, source, phase-ACK, admission, and retirement input share one aligned 20-tick cut. There is no `urgent` batch flag, immediate source-only solve, or second geometry cadence. Source/ACK records retain their exact event ticks, so delayed processing preserves ledger time.
- A mutation invalidates the affected main-thread publication identity immediately, so gameplay uses fallback during the remaining cut instead of forcing an early worker run. Initial admission becomes visible at the next cut under the same rule.
- If the dimension mailbox is not `IDLE` at a cut boundary, the producer keeps coalescing in the same accumulator. After completion ACK, the next batch seals at the latest aligned boundary. No second immutable queue is created and no event is retimestamped.
- Queue-capacity refusal retains the one already sealed `pendingSubmission` and retries it before sealing later input. The mutable accumulator continues to own later observations. A dimension therefore owns either one queued/running/awaiting-ACK batch or one retained submission, never both, plus one coalescing accumulator.
- `ThermalDimensionEngine` stores `lastTargetTick`; no `ThermalTimePolicy` or `ThermalStepPlan` exists. Normal elapsed time is exactly 20 ticks and executes one 1-second transport step. Zero elapsed time performs no transport. An abnormal larger interval integrates source events through the exact target tick, executes at most one 20-tick transport step, marks the engine non-sleeping/time-degraded, and advances the batch clock. The performance gate rejects routine backlog, so no general substep-array framework is retained.

### `ThermalCompletion`

- dimension generation, batch sequence, status (`COMPLETED`, `WORK_LIMITED`, or terminal `ENGINE_FAILED`), topology generation, and sleeping state;
- exact Page continuation entries carrying section key, lifecycle generation, geometry revision, topology generation, and face mask;
- exact committed full-resync tokens carrying Page lifecycle/revision identity;
- bounded phase requests;
- a failure object only for terminal `ENGINE_FAILED`.

Source-dirty sections and internal topology versions remain worker-internal. Close is mailbox state, not a `ThermalCompletion.Status`. Arrays are exact and ownership transfers once; completion accessors do not clone them.

There is no separate `APPLIED`, `TOPOLOGY_UNCHANGED`, `NUMERIC_DEGRADED`, `REBUILD_REQUIRED`, or `CLOSED` completion status. Topology changes are described by exact Page continuation/resync payloads; numeric/time degradation only prevents sleep and affects publication confidence; unexpected failure is terminal.

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
- Remove fixed functional caps of 128 radiation sources and 64 source-owned Pages. Source metadata grows geometrically under `ThermalMemoryBudget`; Page/arena admission uses the shared priority budget below. Candidate visits/rays remain explicitly bounded so many distant campfires cannot increase one player's query cost.
- Source removal unlinks every index in O(number of ports + owned buckets), releases Page interests and receiver witnesses by exact key, and recycles the slot. Shutdown-only iteration over live source slots is allowed once; no routine global replay/scan remains.

### Page Geometry Decision

The spatial decomposition is final: one thermal Page is exactly one Minecraft `16 x 16 x 16` section, and each Page contains 64 `4 x 4 x 4` Bricks.

- `16^3` aligns with chunk-section identity, load/unload events, section mutation hooks, sky columns, lifecycle generation, and O(1) section-key lookup. `8^3` would multiply Page maps/publications/cross-Page edges; `32^3` would capture unloaded sections and make admission or local invalidation too broad.
- `4^3` is the practical local compile unit: a changed block affects at most its dependency closure, owning Brick, and exact negative-face pair owners. `2^3` multiplies fragment/edge/metadata overhead; `8^3` makes a door or one placed block recompile eight times as many block signatures. Neither alternative improves the measured door workload as a whole.
- Replace mutable `PageSignatureStorage` plus nested `Snapshot` with one immutable `PageSignatures` value. It owns one flat 64-reference Brick directory; each entry directly references `char[64]` compact signatures or `int[64]` promoted signatures. `withBrick(...)` shallow-clones 64 references and installs one 64-entry payload. Main capture uses a reusable mutable builder and freezes once; worker/Page publication share immutable payloads directly.
- Worker `WorkerPageState` keeps stable 64-Brick authority. A local prepared change replaces only changed Brick references and scalar revisions. Full Page admission alone builds all 64 payloads.
- `PagePublication` uses one flat 64-reference directory of immutable Brick publication payloads. One payload owns coverage slot plus arena generation, mixed geometry/signature reference, and phase candidates for that Brick. A local topology change shallow-clones the 64 references, replaces changed payloads, and installs one Page publication reference; it does not clone several parallel 64-entry arrays.
- A compact Page retains 8 KiB of signature values plus one 64-reference directory and array headers. One changed compact Brick copies about 512 bytes of directory references plus 128 bytes of signatures. The direct one-index read and smaller type surface are preferred over saving a few hundred mutation bytes with a persistent tree.

### Page, Phase, And Query Publication

- `ThermalPageHandle` contains only section key, lifecycle generation, atomic live geometry revision/resync requirement, and one volatile `PagePublication`; it owns no worker topology arrays, arena spans, compiler state, or mutation orchestration.
- A topology commit prepares one owned `PagePublication` only for a Page whose installed Brick payload or topology identity changed. It contains geometry revision, topology generation, committed batch sequence, and the flat Brick directory defined above.
- Geometry and phase candidates are published by one final volatile reference assignment after the Page's topology commit. The publication constructor takes ownership of already exact arrays and does not clone them. Unchanged Pages keep the existing publication. There is no successful-solve all-Page phase pass and no `Publication.withPhaseCandidates()` copy path.
- Reservoir lookup uses a worker-owned primitive open-address reverse index keyed by `(lifecycleGeneration, brickMinX, brickMinY, brickMinZ, profileId)`. Preparation reserves capacity and commit updates it allocation-free. ACK handling checks `fastSlot`, then this exact index, then request sequence. A stale/missing ACK is ignored; strict batch sequence prevents replay, and each outstanding reservoir request accepts its sequence at most once. There is no transition watermark stream or arena scan.
- `QueryPublication` is a flat arena-slot-addressed double buffer: `double[2][capacity] temperature`, `int[2][capacity] mediumId`, `int[2][capacity] flags`, and `int[2][capacity] slotGeneration`. Reader lookup uses the arena slot directly, strict O(1), with no slot directory, chunk object, binary search, or published `arenaSlots[]`.
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

There is no dimension-wide Air connected-component authority. Delete `IncrementalAirGraph`, component IDs/member lists, BFS/frontiers, spanning-forest edge classes, component sky state, unresolved-component counts, and maximum-delta witnesses. Brick-local adaptive Air cells and solver pair fragments are sufficient for physical transport.

- Air-to-Air heat moves only through unique local conductance edges. Opening a door adds the exact face/cell edges compiled from its final collision/occlusion shape; closing it removes them. `ThermalExchangeKernel` then moves energy from hotter endpoint to colder endpoint. No room classification or component relabel participates in transport.
- Each Brick owns a local FarField boundary fragment for Air cells with directly exposed microface patches. It stores arena slot/generation, owning Page slot, base exposed-area conductance, and local continuation factor. Geometry/sky changes replace only affected Brick fragments.
- Solver boundary temperature is read from `naturalTemperatureByPage[pageSlot]`. Natural-temperature refresh updates one Page scalar. Wind changes one engine scale/generation; boundary fragments lazily recompile their local fixed-step coefficients when next executed. Neither change rebuilds topology or scans all boundary fragments separately.
- Delete `FarFieldProfileRegistry`, environment-class maps, source-power applicability, and maximum-temperature-delta profile selection. Current gameplay constructs the same conductance for every environment class, so this registry adds policy/object structure without distinct behavior.
- `FarFieldSettings` contains only `enabled`, `baseConductanceWPerK`, `referenceOpeningAreaBlocksSquared`, and `continuationDistanceBlocks`. For an exposed cell, `G_far = baseConductanceWPerK * windScale * openPatchCount / (16 * referenceOpeningAreaBlocksSquared) * continuationFactor`. Direct sky exposure uses `continuationFactor = 1`; a loaded-but-unrepresented continuation boundary uses `1 / (1 + continuationDistanceBlocks)`. Interior cells have no natural fixed boundary and exchange heat only through Air/material edges.
- Unmapped geometry or unloaded required shape input keeps the exact Page/Brick publication unresolved and on gameplay fallback. It does not require discovering a whole connected component. Admission of a neighboring Page replaces a local boundary approximation with exact cross-Page Air edges.
- Worker Page state keeps a 64-bit resolved-Brick mask and the dimension keeps an incrementally updated unresolved-Page count. `topologyResolved` is O(1); no solve or publication scans Pages/components to derive it.
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

- One main-thread `MinecraftPageManager` owns Page handles, reasoned interest, priority admission, expiry, full/sparse capture queues, lifecycle generation, and retirement. Do not split interest and capture into two coordinators; capture is work performed for an admitted/dirty Page owned by this manager.
- It keeps primitive references for player leases, physical-source target ports, and continuation parents. One Page is retired when all reasons expire/release; no player-admitted vertical Page remains until chunk unload merely because its original query is gone.
- Player leases are renewed by stable receiver identity and expire through a 20-tick timing wheel; logout releases immediately. A short bounded previous-Page lease avoids section-boundary churn. Source and continuation reverse indexes release exact references without scanning all sources, players, or Pages.
- Primary player Pages have highest admission priority, physical-source targets next, continuations last. Admission is round-robin within a priority and charged against shared Page/signature/arena/pair memory-work budgets, replacing separate fixed 64-Page and 128-source behavior. Budget refusal leaves publication fallback and remains retryable without rebuilding unrelated state.
- `MinecraftPageManager` processes count-bounded complete Page capture and resolved-center queues each server tick. Defaults are frozen only after the 100-player workload below meets the tick budget. A Page publication stays stale until all centers for its revision are resolved, so deferring excess work preserves correctness while bounding main-thread spikes.
- Player temperature updates keep the configured 20-tick interval but use a stable UUID-derived phase offset. One hundred players are distributed across the 20 ticks instead of converging on `player.tickCount % 20 == 0`. The first Page request follows the same stagger and uses natural fallback while queued.
- Full Page capture writes normalized signature IDs into a reusable 4,096-entry primitive builder and freezes one immutable `PageSignatures`; it does not build `LinkedHashMap<BlockPos, Snapshot>`, a second resolver snapshot, or repeated normalized copies. A main-thread-only resolver scratch uses packed relative offsets plus reusable `BlockState` references and is cleared before handoff, so no Minecraft object enters the batch.
- Sparse mutation capture remains exact-position-first. It resolves only the dependency closure into sorted local indexes/signatures and promotes to an owned full Page cut only at the existing density threshold. Repeated changes to one position keep one final entry per 20-tick cut.
- `WorkerPageState` owns stable committed Page identity plus 64 Brick slots. Its portion of `PreparedTopologyChange` contains only sorted changed Brick indexes, replacement span/fragment/publication references, 64-entry signature payloads, and scalar revision changes. It never constructs `new PageState(old)` or copies 4,096 signatures and every Page field for a local change.
- Admission may install a complete Page state because the Page is new. Retirement carries exact Page identity and removes only that Page's indexes/fragments after sweep/source references permit release. Re-admission always creates a new lifecycle generation; it cannot revive worker state from the retired generation.
- `DimensionInputAccumulator`, geometry/source builders, and compiler scratch retain geometrically grown producer/worker capacity across cuts. Sealing transfers exact used prefixes and resets logical size without discarding reusable builders. Shared immutable empty payloads are used for empty arrays; no routine empty cut allocates one zero-length array per schema field.

### Ownership-Sized Class Layout

The extraction is behavioral-neutral and follows authority, not arbitrary line splitting.

| Class | Sole responsibility | Target size |
|---|---|---:|
| `MinecraftThermalInput` | main-thread level lifecycle, 20-tick seal/submit/drain, consumer facade delegation | `< 800` lines |
| `MinecraftPageManager` | Page handles/interests/admission/capture/expiry/retirement | `< 1,200` lines |
| `ThermalPageHandle` | cross-thread Page identity/live revision/volatile publication only | `< 250` lines |
| `PageSignatures` / `PagePublication` | immutable flat Brick directories and query payloads | `< 300` lines each |
| `MinecraftEnvironmentCapture` | bounded natural-temperature/sky/FarField input capture | `< 600` lines |
| `PhysicalSourceSpatialIndex` | main-thread source SoA and origin/chunk/target/kind spatial indexes | `< 700` lines |
| `DimensionInputAccumulator` | producer-owned coalescing and immutable sequence/tick batch sealing | `< 600` lines |
| `ThermalInputBatch` / `ThermalCompletion` | immutable ownership-transfer schemas and validation only | `< 350` lines each |
| `ThermalWorkerPool` | shared bounded workers and server-wide close only | `< 350` lines |
| `ThermalDimensionMailbox` | one dimension's submit/run/completion-ACK/close state only | `< 300` lines |
| `ThermalDimensionEngine` | all worker runtime ownership and one visible `process(batch)` pipeline | `< 800` lines |
| `ThermalCellArena` | primitive H/C/inverse-C/cell identity/allocation spans only | `< 1,100` lines |
| `WorkerPageStore` | Page identity/lifecycle/indexed committed state | `< 800` lines |
| `TopologyPlan` | changed closure, drafts, spans, migration, hard-cap preparation scratch | `< 800` lines |
| `BrickTopologyCompiler` | reusable primitive scratch to exact Air/material/FarField/phase Brick payload | `< 800` lines |
| `PreparedTopologyChange` | one immutable exact changed payload; data validation only | `< 400` lines |
| `TopologyCommitter` | prepared nonthrowing write order | `< 500` lines |
| `ThermalSolver` | primitive fragment stores, material aggregation, numeric execution, sleep residual | `< 1,100` lines |
| `ThermalSourceLedger` | source slots/events/bindings/exact Pdt and node accumulator ownership | `< 900` lines |
| `NodePowerAccumulatorArena` | primitive per-node power/pending-energy storage and active/free indexes | `< 350` lines |
| `WorkerPhysicalSourceBindings` | descriptor-to-topology endpoint resolution only | `< 500` lines |
| `PhaseTransitionRuntime` | phase contact/request/ACK state only | `< 500` lines |
| `QueryPublication` | flat slot-addressed double-buffer projection and O(1) reader | `< 400` lines |

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
- Store immutable `PageSignatures` as 64-entry Brick payloads behind one flat 64-reference directory; use `char[64]` while IDs fit and promote only the affected Brick to `int[64]`. Sentinel values live outside registry consumers.
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
- [ ] Local Air/FarField: delete global `IncrementalAirGraph`, component/forest/member/witness state, `FarFieldProfileRegistry`, and profile applicability logic; compile only exact local Air pair plus exposed FarField boundary fragments and apply Page natural temperature/global wind scale at solve time.
- [ ] Page management/player scheduling: replace over-wide `ThermalPage` with `ThermalPageHandle`, mutable/snapshot `PageSignatureStorage` with immutable `PageSignatures`, and merge Page interest/capture into `MinecraftPageManager`; add leases, fair admission, expiry, bounded capture, and UUID-staggered updates.
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
- Main-thread Page admission and geometry resolution never exceed their configured per-tick work units. One hundred simultaneous receiver requests are staggered/fallback-capable and cannot trigger 100 full Page captures in one tick.
- Physical-source Page/chunk lifecycle, nearest-generator, infrared, and radiation candidate work remains spatially bounded when total sources grow from 128 to 5,120; no routine path scans all sources.
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

Update [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`docs/climate/heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), and [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md). Document local FarField conductance, inverse capacity, compiled fixed-step pair/boundary coefficients, dynamic fallback paths, and memory costs with formulas/units. The climate README remains the navigation owner. No file under `design/` may change.

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

- Last updated: `2026-08-29 01:15:40 +08:00`
- State: `final async owners are present; source-before-migration ordering and isolated arena staging are implemented; unified compilation, test, GameTest, and profiling validation remain incomplete`
- Resume authority: update this section before context compaction instead of reconstructing the task from chat history or appending another overlapping plan.

### Architecture Completion Decision

The architecture is complete enough to implement without another design pass. `Final Runtime Specifications`, `Optimization Boundaries`, and `Minimality Proof` fix data layouts, ownership, operation order, failure semantics, 20-tick cadence, complexity, retained memory, class layout, and validation. There are no open architecture choices delegated to implementation and no accepted temporary stage. If source evidence later contradicts an invariant, update this checkpoint before editing; do not silently invent a compatibility path or append another overlapping plan.

This plan-only review changed no production code. The final architecture removes obsolete coordination layers and compiles the fixed 1-second numerical invariants: arena inverse capacity plus fixed pair/boundary transfer coefficients eliminate routine divisions and `expm1` while retaining the generic kernel only for dynamic/abnormal paths. No further orchestration or numeric migration is reserved for later.

### Frozen Architecture

Do not replace these decisions in later continuation turns: `16^3` Page represented cross-thread only by `ThermalPageHandle`, `4^3` Brick, adaptive Brick-local Air cells, no dimension-wide Air component authority, primitive SoA arena with H/C/inverse-C, compiled fixed-step coefficients in direct `ThermalSolver` fragments, one `ThermalSourceLedger`, unique local conductance edges and exposed fixed-temperature boundaries, one `ThermalDimensionEngine` writer per dimension behind a shared bounded pool, one sequence/tick batch plus completion/ACK, fixed 20-tick cuts, one three-stage sparse topology pipeline, flat immutable Page Brick directories, and flat slot-addressed query buffers. Do not reintroduce watermark/frame/epoch vectors, generic time plans, runtime/executor/sweep/source facades, over-wide coordinators, permanent block lattices, room graphs, octrees, synchronous paths, or compatibility architecture.

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
10. Add reasoned Page interest leases, fair priority admission, expiry timing wheel, count-bounded capture/center queues, and stable UUID staggering for 100-player main-thread work. Chunk unload is not the only player-Page reclamation path.
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

1. Simplify batch schema and merge all runtime/executor/time ownership into `ThermalDimensionEngine`; delete watermark/frame/epoch/time-plan/runtime classes.
2. Replace sweep facade/fragments with primitive `ThermalSolver`, including inverse capacity, compiled fixed-step coefficients, unique material execution, lazy wind refresh, and terminal failure boundary.
3. Finish `PhysicalSourceSpatialIndex` and merge worker source state into `ThermalSourceLedger` with recyclable accumulators/bindings.
4. Finish `ThermalPageHandle`, immutable `PageSignatures`/`PagePublication`, merged `MinecraftPageManager`, reservoir index, and flat O(1) arena-slot query publication.
5. Delete giant topology/source managers and global Air/FarField profile machinery; enforce the final source index, local boundary fragments, three-stage topology pipeline, compiler scratch, player staggering, ownership-sized classes, and mailbox/pool lifecycle.
6. Migrate all JUnit/GameTest code to observable async behavior without production instrumentation; update living climate docs and this plan; add the diary entry.
7. Only after all production code, tests, and docs are written, run the unified compile/tests/GameTests, controlled 120-second door/block/source/player/crop JFR runs, and 10/30-minute combined/churn heap runs. No sub-agent, Luna, thread, or worktree is authorized. Keep `TemperatureThreadingPool` source present and its lifecycle calls commented/disabled.

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
