# Thermal Runtime Dead-Path And Dispatch Convergence Plan

- Time: `2026-08-26 21:15:44 +08:00`
- Authors: `Codex; OpenAI GPT-5; primary planning and architecture review agent`
- Status: `completed`
- Scope: `thermal source ledger retention, Minecraft tick/apply reports, production-only test fixtures, and synchronous runtime dispatch`
- Related: [`plans/2026-08-26_20-22-41_thermal-architecture-deletion-first-convergence.md`](2026-08-26_20-22-41_thermal-architecture-deletion-first-convergence.md), [`plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`](2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`diary/2026-08-26_20-47-13_thermal-deletion-first-convergence.md`](../diary/2026-08-26_20-47-13_thermal-deletion-first-convergence.md)

## Purpose And Authority

This plan continues the completed deletion-first convergence batch. It does not redesign thermal physics and does not replace the full sparse-runtime architecture specification. Source code remains the authority for implemented behavior.

The earlier architecture specified retained source replay and a bounded cross-dimension coordinator for a future asynchronous worker. The current gameplay implementation never connected either recovery consumer or an asynchronous worker: source commands and solves execute synchronously through `Runnable::run`, while `DimensionThermalRuntime` already owns latest-only epoch scheduling and logical-writer exclusion. This plan removes those dormant implementation mechanisms while preserving the contracts that are actually exercised.

Implementation and final validation completed on `2026-08-26`; the exact result is recorded in `Outcome`.

## Goal

Reduce accidental runtime complexity, startup memory, hot-path allocation, and production source-set test scaffolding without changing:

- `ThermalCellArena` as the sole live `H/C` authority.
- Exact source `integral(P dt)` at event-time boundaries.
- Source identity, port rebind, unload/revival, signed impulse, and explicit sink routing.
- Geometry, mixed Brick, material, phase, FarField, radiation, publication, and gameplay compositor semantics.
- The current five-tick steady solve cadence and urgent mutation/source wakeups.
- One logical writer per dimension and latest-only epoch semantics.

The batch is accepted only if:

1. Every stage is net-negative in production Java lines within its touched scope.
2. The complete batch removes production classes and fields; it introduces no manager, adapter, shadow runtime, cache table, benchmark source set, or alternate solver.
3. Removed diagnostics are replaced by assertions against authoritative state, not by another production report or test API.
4. Source energy still reaches exactly one historical binding or declared sink at each event boundary.
5. All edits are completed before the full test command is run once.
6. A same-workload real-save JFR is the final performance gate, not a new synthetic benchmark framework.

## Verified Current State

The following was checked against the working tree on `2026-08-26`. Existing uncommitted changes must be preserved.

### 1. Dormant source recovery history

- `SourceResyncReplayer` has no production caller.
- `SourceResyncSnapshot` is created only by tests and GameTests through `snapshotAt` or `snapshotAtCursor`.
- No production component consumes a replay, sends an ACK, or uses retained segments to recover a source queue gap.
- `ThermalSourceRegistry` nevertheless allocates a fixed history ring for every source and maintains ACK, dropped-history, checksum, and a second registry-local event watermark.
- Gameplay constructs `ThermalSourceRegistry(64, 3, 64, accumulators)`. The ten history arrays alone occupy `221,184` primitive payload bytes per dimension (`64 * 64 * 54 bytes`), before ACK/drop metadata and array-object overhead.
- Exact gameplay injection does not read this history. It flows through `ThermalSourceTimeline -> NodePowerAccumulatorArena -> ThermalCellArena`.

### 2. Test-shaped reports on the live path

- `MinecraftThermalInput.SealReport` is allocated for every seal. Production reads only `runtimeResult`; its frame and counters are test observations.
- `MinecraftThermalTopologyApplier.ApplyReport` is allocated on every apply branch. Production reads only `readyForSolve`; Page/pair counters are test observations.
- `ThermalRuntimeCoordinator.DispatchResult` is allocated for each executed run and once more for the terminating empty result in the current drain loop.
- `DimensionThermalRuntime.RunReport` is different: runtime tests use its status, publication, sleep, and thermal-step result to verify real solver outcomes. It is not included in the initial deletion set.

### 3. Duplicate scheduling ownership

- `DimensionThermalRuntime` already owns `LatestSolveEpochScheduler`, `hasReadyWork`, writer exclusion, sleep/wake state, generation checks, and latest-only epoch completion.
- Gameplay also routes each accepted frame through the `483`-line `ThermalRuntimeCoordinator` with a second `IDLE/QUEUED/RUNNING` mailbox and ready queue.
- Production passes `Runnable::run` to `enableDispatch` and immediately drains the coordinator queue on the same call stack.
- Under this synchronous connection, fairness, recovery reserve, sticky re-offer, oldest-age promotion, and cross-dimension queue admission do not schedule a real worker. Each `ServerLevel` reaches its own input from the server thread.
- The coordinator reserves only `528` primitive payload bytes at the gameplay configuration (`16` ready slots, `16` dimensions); its main cost is duplicated state and control flow, not memory.

### 4. Test-only production code

- `LoadedOnlyResolverSnapshot` is a `100`-line production class used only by Phase A Forge GameTests.
- `NodePowerAccumulatorArena.NodeEnergyConsumer` and its callback drain overload are used only by one unit test. Production uses the concrete `ThermalCellArena` overload.
- `ThermalSourceRegistry.entry`, `port`, `ThermalSourceEntry`, and several recovery accessors are test-only APIs.
- `ThermalRuntimeCoordinator.mailboxState` is test-only and disappears with the coordinator.

## What Already Exists

| Required behavior | Existing owner | Decision |
|---|---|---|
| Source event ordering and source watermark | `ThermalSourceTimeline` | Reuse unchanged; it remains the only command-stream cut. |
| Current source identity, lifecycle generation, mode, power, port IDs/shares, and bindings | `ThermalSourceRegistry` | Keep only state used by settlement/rebind/unload; remove retrospective and display-only metadata. |
| Exact continuous-power integration and retryable pending energy | `NodePowerAccumulatorArena` | Keep concrete arena drain and pending-energy ownership. |
| Live temperature/enthalpy state | `ThermalCellArena` | Keep as sole `H/C` authority. |
| Per-binding-kind source accounting | `ThermalSourceRegistry.routedEnergyJ` | Keep as the small observable account for node/internal/declared/degraded routes. |
| Latest-only solve scheduling and logical writer | `DimensionThermalRuntime` + `LatestSolveEpochScheduler` | Reuse directly; do not replace with another mailbox. |
| Future execution boundary | `Executor` accepted by `MinecraftThermalInput` | Keep the interface and explicit serial-order contract. |
| Loaded-only production capture | `MinecraftThermalInput.LoadedCube` and `LoadedSectionSnapshot` | Keep. Phase A's separate helper moves out of production scope. |

## Necessary State That Must Not Be Deleted

This plan must not repeat earlier false-positive cleanup proposals:

- Input `dirtyPages` and applier `dirtyPages` represent unsealed input versus installed topology work.
- `ThermalPage` coverage and `PageState.appliedCoverageRefs` represent desired/current coverage versus rollback-safe installed-old coverage.
- `LiveSource.retainedSections` and input Page reference counts are per-source retention versus aggregate admission ownership.
- Phase request, phase ACK, and pending ACK are distinct cross-tick states.
- `installedActivePages` and `installedActiveBySection` provide deterministic traversal and O(1) lookup respectively.
- `RunReport`, `ThermalStepExecutor.Report`, lifecycle generations, source command watermarks, geometry revisions, and publication versions carry executed correctness semantics.

## Target Architecture

### Source path

```text
source event at tick T
        |
        v
ThermalSourceTimeline command ring + one source watermark
        |
        v
ThermalSourceRegistry current source/port/binding state
        |                         |
        |                         `-> small routedEnergyJ counters
        v
NodePowerAccumulatorArena exact P*dt / impulse
        |
        v
ThermalCellArena authoritative H
```

Removed branch:

```text
ThermalSourceRegistry
  `-> retained segment ring -> SourceResyncSnapshot -> SourceResyncReplayer -> no production consumer
```

### Dispatch path

```text
ServerLevel tick
  -> seal latest SealedInputFrame
  -> serial Executor boundary
  -> topologyApplier.apply(frame) returns ApplyStatus
     |-- APPLIED / DUPLICATE -> runtime.runOne()
     `-- pending/stale       -> keep latest scheduler state; next input event/tick retries
  -> QueryPublication
```

Removed layer:

```text
serial Executor
  -> shared coordinator mailbox/ready queue/fairness
  -> immediately drain same-thread queue
  -> per-dimension runtime scheduler
```

## Deletion Rules

1. Delete the dead owner before simplifying callers. Do not leave deprecated or commented-out copies.
2. Do not introduce a replacement recovery protocol, coordinator, result hierarchy, or diagnostic cache.
3. Tests must assert authoritative energy, bindings, coverage, sweep, publication, and lifecycle state.
4. Do not change formulas, profile values, cadence, interest admission, Page topology, or gameplay consumer behavior in this batch.
5. Preserve `routedEnergyJ` unless a real command/log replacement is implemented in the same stage; removing the only loss account is not simplification.
6. Keep the `Executor` contract, but do not pretend current V1 has a real asynchronous worker.
7. If direct dispatch cannot preserve unload, latest-frame, and single-writer semantics without a new queue or persistent state machine, stop Stage 4 and keep the coordinator pending a separate design.

## Ordered Implementation

### Stage 0: Freeze the deletion inventory

Before edits:

- Record line, field, nested-type, and primitive-array counts for the touched production files.
- Search all main, test, and GameTest references for every deletion candidate.
- Record the current source configuration and exact history payload calculation.
- Confirm no external API, network packet, command, persistence codec, config, or companion-pack identifier references the recovery/report/coordinator types.

No code is changed in Stage 0.

### Stage 1: Delete unconsumed source replay and history

Delete:

- `SourceResyncReplayer.java`.
- `SourceResyncSnapshot.java`.
- `historyCapacityPerSource` and the constructor parameter.
- All `history*`, `ack*`, and `dropped*` arrays and their allocation/growth/reset logic.
- `snapshotAt`, `snapshotAtCursor`, `acknowledge`, retained-segment materialization, checksum, prune, and dropped-history methods.
- Registry-local `eventWatermarks` and `nextEventWatermark`; the command timeline remains the one source watermark authority.
- Recovery-only `cumulativeEnergyJ` and `cumulativeCompensationJ` checksum arrays.
- Registry-local `packedPositions`, `profileIds`, and `sourceRevisions`; source position/profile already belong to the Minecraft physical-source owner, and registry revision has no consumer after replay removal.
- Registry-local `portRevisions` and `portChannels`; port revision exists only for replay, while channel remains authoritative in `MinecraftPhysicalSourceProfile` and is not needed after a port is compiled to ID/share/binding.
- The unused `EmissionPort.portRevision` and `rebound` API.

Retain:

- Source ID, lifecycle generation, mode, enabled/unloaded state, declared power, last ledger tick, port ID/share/contribution, and current binding.
- `SourceChannel` in physical source profiles and profile compilation; only its duplicate registry storage is removed.
- Pending node energy and exact event-time settlement.
- Small per-source/per-binding-kind `routedEnergyJ` counters for explicit loss and source-profile verification.
- Direct signed impulse routing and degraded/unbound sink handling.

Rewrite tests:

- Delete retained replay, repeated snapshot, ACK, exhausted-history, and replay-target tests because they validate removed behavior only.
- Replace snapshot-as-settlement calls with execution through a real `SolveEpoch` or assertions on actual accumulator/arena state.
- Assert exact `P*dt`, rebind-at-tick, on/off, cold route, unload, revival, impulse, node generation, declared loss, and degraded loss through existing runtime state and `routedEnergyJ`.
- Update all `ThermalSourceRegistry` constructor call sites.
- Remove packed position/profile arguments from `ThermalSourceTimeline` register commands; the physical source manager continues to own that metadata.

Acceptance:

- No replay/snapshot/history/ACK symbol remains in production source.
- Source queue watermark and registry history watermark are no longer parallel authorities.
- Gameplay source primitive payload drops by approximately `225-230 KiB` per dimension at the current `64 x 64` configuration, excluding array headers.
- Exact source-energy tests still pass without adding a diagnostic class.

### Stage 2: Remove production test fixtures and accessors

Delete or move out of `src/main`:

- `LoadedOnlyResolverSnapshot`.
- `NodeEnergyConsumer` and the callback drain overload.
- `ThermalSourceRegistry.entry`, `port`, `ThermalSourceEntry`, `maxPortsPerSource()`, and recovery/capacity getters used only by tests.

Test migration:

- Put the small `getChunkNow` snapshot fixture directly in the existing Phase A GameTest class as private test code, or drive the production input path when the same assertion is practical. Do not add another main class.
- Keep the concrete stale-generation/retry test against `drainAllPendingEnergyTo(long, ThermalCellArena)`; remove the callback-only duplicate.
- Convert source revival tests to behavioral assertions: source count remains bounded, new generation receives power, old generation receives none, and energy is not duplicated.

Acceptance:

- Production class count decreases.
- Production source no longer exposes APIs solely to make tests convenient.
- Loaded-only/no-chunk-load and dynamic-exclusion GameTests retain their player-relevant assertions.

### Stage 3: Collapse hot-path report wrappers

Change:

- Make `MinecraftThermalInput.sealTick` return the existing `LatestSolveEpochScheduler.SealResult`, not `SealReport`.
- Delete `SealReport`; tests read existing frame/watermark owners or assert resulting behavior.
- Make `MinecraftThermalTopologyApplier.apply` return `ApplyStatus`, not `ApplyReport`.
- Delete apply counters used only by tests and replace those tests with arena, coverage, sweep, topology-generation, and publication assertions.
- Delete the unused public `MinecraftThermalInput.applyTopology` manual route.
- Keep `DimensionThermalRuntime.RunReport` and `ThermalStepExecutor.Report`; they describe actual execution results and support solver correctness tests.

Acceptance:

- Normal seal/apply performs no `SealReport` or `ApplyReport` allocation.
- Production dispatch branches directly on existing enums.
- Local rebuild tests prove which Brick/Page state changed instead of trusting a returned counter.
- No new last-report field, counter table, or test-only production getter is introduced.

### Stage 4: Remove the dormant cross-dimension coordinator

This is the highest-risk stage and starts only after Stages 1-3 compile cleanly by inspection.

Delete:

- `ThermalRuntimeCoordinator.java` and `ThermalRuntimeCoordinatorTest.java`.
- Static `gameplayCoordinator`, coordinator creation/admission, coordinator close, and per-input coordinator fields.
- Mailbox, ready queue, recovery reserve, fairness, sticky re-offer, and `DispatchResult` code that has no asynchronous production worker.

Replace with:

- `MinecraftThermalInput.enableDispatch(Executor serialExecutor)`.
- One submitted task per accepted/duplicate sealed frame.
- Inside the task: apply the concrete frame; on `APPLIED` or `DUPLICATE`, call that input's `DimensionThermalRuntime.runOne()` once.
- Keep the explicit contract that tasks for one input execute serially, in submission order, without overlap.
- On executor rejection, retain the scheduler's latest sealed frame and request a later retry; do not execute concurrently on the caller as a hidden fallback.
- On input close, invalidate/unload/close its runtime using the existing dispatch field as the ownership marker; add no second lifecycle boolean.

Correctness requirements:

- A newer sealed frame cannot be overwritten or published as an older geometry/topology generation.
- Closing one level cannot close another dimension's runtime.
- Two synchronously ticked dimensions each advance from their own level tick; no shared ready queue is required for current gameplay.
- Repeated or stale tasks remain harmless through existing frame, generation, and latest-only scheduler checks.
- No loop repeatedly calls `runOne` for one frame. A later accepted frame owns the later run.

Stop Stage 4 if any requirement needs a replacement mailbox, manager, or queue. In that case retain the coordinator temporarily and record the exact missing runtime contract instead of recreating it under another name.

### Stage 5: Documentation and outcome

After implementation and validation:

- Update `docs/climate/data-lifecycle-and-integration.md` to describe the direct serial-executor dispatch and current-only source registry.
- Update the original sparse-runtime plan's implemented-state notes so retained replay and coordinator are historical intent, not current behavior.
- Update the prior deletion-first plan only with a link to this follow-up; do not rewrite its completed outcome.
- Append one diary entry with deletions, retained boundaries, exact test counts, before/after lines/fields/types, memory estimate, JFR result, and remaining work.
- Set this plan to `completed`, `superseded`, or `abandoned` and fill `Outcome`.

## Planned Coverage

```text
SOURCE CODE PATHS                                  GAMEPLAY FLOWS
[keep] timeline register/power/on/off/rebind        [test] campfire/generator/fountain/radiator
  |-- exact event tick settlement                     |-- one-second expected mesh H
  |-- signed impulse                                   |-- declared/internal/degraded route totals
  |-- cold route and unload                            `-- unload/revival without double injection
  `-- node generation retry

[delete] snapshot/history/replay                    [delete] replay-only tests
  `-- no production consumer                          `-- no player/runtime flow existed

[change] seal -> executor -> apply status -> run    [test] real Minecraft mutation/publication
  |-- accepted / duplicate                            |-- steady five-tick solve
  |-- stale/latest/generation mismatch                |-- urgent dirty deadline
  |-- executor rejection retry                        |-- level unload
  `-- publication generation gate                     `-- two dimensions advance independently

[keep] runtime run report                           [test] solver/runtime unit outcomes
  |-- completed / sleeping / no work
  |-- inputs pending / recovery required
  `-- publication and thermal-step result
```

Required regression coverage:

1. **CRITICAL:** rebind at tick `T` settles old binding through `T` and new binding only after `T`.
2. **CRITICAL:** removing retained history does not change total signed energy delivered to arena plus declared/internal/degraded routes.
3. **CRITICAL:** direct dispatch cannot publish a stale Page after mutation or unload.
4. **CRITICAL:** an executor rejection does not lose the latest sealed frame and does not overlap a logical writer.
5. Source queue full/retry semantics remain owned by the existing timeline command ring; removal of replay tests must not remove queue-full coverage.
6. Local topology tests assert installed state directly after report-counter deletion.
7. Two active dimensions solve independently under the current synchronous executor.

## Failure Modes

| Change | Realistic failure | Required handling/test | Player visibility |
|---|---|---|---|
| Remove history | Settlement accidentally depended on `appendSegment` side effects | Exact source integral and route-total regressions | Heat disappears or doubles |
| Remove cumulative checksum | Declared loss is no longer observable | Keep and assert `routedEnergyJ` by binding kind | Silent energy-accounting gap |
| Remove registry watermark | Source command deduplication uses the wrong owner | Timeline watermark remains monotonic; same-tick tests | Duplicate or skipped source event |
| Remove callback drain | Failed arena write clears pending energy | Concrete stale-generation retry test | Heat lost during rebuild |
| Remove seal/apply reports | Tests stop detecting local rebuild regressions | Assert coverage/sweep/arena/publication state | Stale wall or opening temperature |
| Direct dispatch | Newer frame arrives while an older task runs | Serial executor contract plus latest-frame/generation checks | Stale temperature publication |
| Executor rejection | Accepted frame is never retried | Preserve latest frame and set existing urgent retry | Temperature freezes temporarily |
| Input close | Runtime remains published or another dimension is closed | Per-input close and two-dimension unload test | Stale HUD or cross-dimension failure |
| Remove coordinator fairness | One dimension monopolizes a future async pool | Out of current V1; current synchronous two-dimension test | Future-only risk, no current change |

No current gameplay failure may remain silent and uncovered. Future asynchronous fairness is explicitly deferred rather than simulated by dormant code.

## Performance And Size Expectations

These are acceptance estimates, not benchmark claims:

- Delete at least four production top-level classes: `SourceResyncReplayer`, `SourceResyncSnapshot`, `ThermalRuntimeCoordinator`, and `LoadedOnlyResolverSnapshot`.
- Those four files currently total `989` production lines before registry/input/report simplification.
- Remove approximately `225-230 KiB` of source history/recovery primitive payload per gameplay dimension at initial capacity `64`.
- Remove the `528`-byte coordinator primitive reservation plus object/array overhead at current server configuration.
- Remove approximately three small result allocations per scheduled solve (`SealReport`, `ApplyReport`, and coordinator dispatch/empty results); retain execution reports that carry real solver outcomes.
- Reduce `ThermalSourceRegistry`, `MinecraftThermalInput`, and topology apply branch count materially.
- Whole-batch production Java reduction should exceed `900` lines. If it does not, inspect replacement glue before accepting the batch.

Do not claim a CPU percentage improvement from these counts. The post-change same-workload JFR must determine whether topology, solver, source settlement, or allocation profiles changed measurably.

## Final Validation

Run once after all code and documentation edits:

```powershell
./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain
git diff --check
```

Then verify:

- Record exact thermal JUnit and Forge GameTest pass counts.
- Search all source sets for every deleted class, field, method, and constructor signature.
- Inspect production and deobfuscated JAR contents; removed recovery/coordinator/test-fixture classes must not be present.
- Record before/after production lines, fields, top-level/nested types, primitive arrays, and estimated retained bytes.
- Run the same real-save repeated door/mining workload used for the previous JFR.
- Compare thermal CPU share, allocation rate, top thermal stacks, retained memory, and tick duration under the same duration and actions.
- Do not add runtime profiling counters to make JFR easier.

## Stop Conditions

Stop and revise this plan if implementation:

- Requires a second source-energy store or replays energy into the current binding.
- Removes `routedEnergyJ` without another real observability consumer.
- Adds a dispatch mailbox, queue, manager, adapter, or last-report cache.
- Changes source power, source profiles, solve cadence, thermal formulas, Page interest, FarField, radiation, material, phase, or compositor behavior.
- Cannot preserve pending source energy after a stale arena generation.
- Cannot preserve latest-frame and unload publication gates with direct serial dispatch.
- Increases production lines in any stage or production class count for the full batch.

## NOT In Scope

- Real asynchronous worker execution, thread-pool sizing, backpressure, cross-dimension fairness, or recovery scheduling. Only the serial `Executor` boundary remains.
- `ThermalCellArena` allocator/layout redesign or free-span optimization without JFR evidence.
- FarField refresh cadence, sky-exposure scans, continuation policy, or connected-component formulas.
- Source command-ring capacity redesign; only unconsumed registry replay history is removed.
- Source profile power, campfire balance, radiation budget, material calibration, phase recipes, HUD, crop, town, or machine gameplay changes.
- Page admission, unknown-position full resync, `4096` cold capture, or local `4^3` mutation topology.
- Removing useful historical architecture text. Documentation should mark superseded implementation mechanisms rather than erase design reasoning.
- JMH, JOL, synthetic workload modules, permanent profiling tables, shadow comparison, or compatibility work without a reproduced player bug.

## Parallelization

Sequential implementation, no parallelization opportunity. Source constructor/report changes, Minecraft dispatch, runtime lifecycle, and their tests share the same classes and correctness contracts. Splitting them would require temporary adapters or duplicate APIs, which this plan explicitly forbids.

## Expected Outcome

- One source command watermark authority instead of timeline plus registry recovery watermark.
- Current source state plus small route totals instead of current state plus unconsumed retrospective history.
- One per-dimension scheduler instead of a synchronous executor wrapped around a dormant shared coordinator.
- Enum/status returns for seal and topology apply instead of test-shaped hot-path records.
- Test fixtures live in test source, and tests assert authoritative behavior instead of production counters.
- Lower startup memory, fewer allocations, fewer branches/types, and a smaller production JAR without changing thermal semantics.

## Outcome

Completed on `2026-08-26` without adding a replacement manager, adapter, cache, shadow runtime, or alternate solver.

- Deleted `SourceResyncReplayer`, `SourceResyncSnapshot`, `ThermalRuntimeCoordinator`, and production `LoadedOnlyResolverSnapshot`, totaling `989` removed top-level production lines.
- Reduced the six retained source/input/topology files recorded for this batch from `10,764` to `9,025` lines, a further net deletion of `1,739` lines. The complete touched production scope therefore fell by `2,728` lines.
- Removed source segment history, ACK/checksum/replay state, the registry-local watermark, report-only `SealReport`/`ApplyReport`, the callback-only `NodeEnergyConsumer`, and unused `MinecraftThermalInput.applyTopology`.
- Kept `ThermalSourceTimeline` as the sole source watermark owner, `ThermalCellArena` as the sole live `H/C` authority, exact event-boundary `integral(P dt)`, route totals, lifecycle generations, latest-only scheduling, and the serial `Executor` boundary.
- Direct dispatch is now `Executor -> MinecraftThermalTopologyApplier.apply -> DimensionThermalRuntime.runOne`. Executor rejection keeps the latest sealed frame pending and requests an urgent retry; it does not run concurrently on the caller.
- The removed source history accounts for approximately `225-230 KiB` of primitive payload per gameplay dimension at the current initial source/history capacity, excluding array headers and removed metadata arrays.
- Java 17 thermal JUnit passed `204/204` across `34` result files. Forge GameTest passed all `14/14` required tests. `git diff --check` passed, and deleted-symbol search across every source set returned no matches.
- No JFR was recorded for this deletion batch, so no CPU percentage improvement is claimed. A same-save repeated door/mining JFR remains the performance comparison gate.
