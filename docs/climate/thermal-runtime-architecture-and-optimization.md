# Thermal Runtime Architecture

- Status: `Current; functional validation complete, controlled profiling pending`
- Last verified: `2026-08-29`
- Scope: server-side thermal capture, asynchronous dimension workers, Page/Brick topology, source energy, phase requests, query publication, and hot-path cost bounds
- Primary code anchors: `MinecraftThermalEvents`, `MinecraftThermalInput`, `MinecraftPageManager`, `PhysicalSourceSpatialIndex`, `DimensionInputAccumulator`, `ThermalDimensionMailbox`, `ThermalWorkerPool`, `ThermalDimensionEngine`, `TopologyPlan`, `PreparedTopologyChange`, `TopologyCommitter`, `ThermalSolver`, `ThermalSourceLedger`, `QueryPublication`

## Ownership

Each `ServerLevel` has one `MinecraftThermalInput` on its level thread and one
`ThermalDimensionEngine` on a bounded thermal worker. Minecraft objects never
cross into the worker. The main thread reads loaded state and produces immutable
primitive cuts; the worker owns arena cells, topology, source integration,
phase state, solver execution, and publication.

The only topology lifecycle is:

```text
main-thread capture
        |
        v
DimensionInputAccumulator -> ThermalInputBatch
        |
        v
ThermalDimensionMailbox -> ThermalDimensionEngine
        |
        v
TopologyPlan -> PreparedTopologyChange -> TopologyCommitter
        |
        v
ThermalSolver / ThermalSourceLedger / PagePublication / QueryPublication
```

`TemperatureThreadingPool.java` remains in the repository because it is an
explicit user requirement. Its initialization, tick polling, and shutdown
calls remain commented and disabled. It is not used by the new worker pool.

## Fixed Cadence

`ThermalInputBatch.CUT_INTERVAL_TICKS` is the sole runtime cadence and is
currently `20` ticks (`1` logical second). Geometry, environment, source,
phase-ACK, Page admission, and Page retirement share that cut.

`MinecraftThermalInput` drains a completion at the start of a level tick. If a
worker completed after an aligned boundary, it submits the latest completed
boundary before collecting the current tick's new world changes. A sealed batch
is retained in `pendingSubmission` when the shared queue is full. A dimension
has at most one queued/running/awaiting-ACK batch, one retained submission, and
one mutable producer accumulator.

The worker validates dimension generation, consecutive sequence, monotonic
aligned target tick, applies phase/wind input, and then settles source energy
through the target tick against the currently installed topology. Only after
that settlement may it prepare and commit a topology replacement, so migration
cannot overwrite energy delivered in the same cut. A normal
20-tick interval executes one fixed one-second transport step. A larger delayed
interval executes at most one step, marks that batch time-degraded, and never
pretends that missing solver steps were processed.

## Page And Brick Model

One thermal Page represents a `16 x 16 x 16` section and contains `64` Bricks.
One Brick is `4 x 4 x 4` blocks. Air geometry is local to the Brick and its six
neighbor Bricks; there is no dimension-wide Air component graph or spanning
forest.

`ThermalPageHandle` is only cross-thread identity, live geometry revision,
resync requirement, and a volatile `PagePublication`. `PageSignatures` stores a
flat directory of `64` immutable Brick payloads. IDs that fit the compact range
use `char[64]`; a changed Brick alone is promoted to `int[64]` when needed.

`WorkerPageStore` holds the worker Page state. A normal mutation shallow-clones
the Page Brick directory and replaces only changed immutable Brick payloads.
Arena cell spans are allocated with the best-fit free-span index, migrated, and
released only after source and solver references have been rebound.

## Mutation Capture

`MinecraftPageManager.SectionOwner` is the only mutation inbox. The mixin path
records primitive section/local-position bits; it does not read world state,
source indexes, heightmaps, or radiation off-thread. Main-thread drain reads the
final block state once per position and updates source, sky, radiation, and
geometry work as appropriate.

For a topology-relevant position, only the Pages that can contain a center in
the one-block dependency halo are invalidated: an interior position touches one
Page, a face two, an edge four, and a corner eight. A fixed 27-slot owner table
is updated by shallow replacement on Page admission/retirement; no 27-by-27
Page scan is performed.

Repeated changes to the same position are coalesced until the next cut. Captured
center signatures stay in the Page manager until a batch is actually sealed.
Non-boundary ticks debounce a position that changed in the current tick; an
aligned cut captures its final state. One position therefore contributes one
final center entry per 20-tick cut, even when a door, fence gate, or trapdoor is
opened and closed repeatedly.

State changes whose static signature is unchanged and which do not involve a
campfire are dropped before the inbox. Campfire changes still update the source
ledger, but do not invalidate geometry when their thermal signature is the same.

## Topology Preparation And Commit

`TopologyPlan` collects changed Pages, sparse centers, environment deltas, and
the exact one-Brick dependency closure. `BrickTopologyCompiler` produces local
Air, material, phase, and exposed FarField payloads using worker-owned reusable
scratch. `MaterialEdgeCompiler` groups changed contributions by packed edge key
in one pass and rebuilds only affected canonical owner executions.

Preparation reserves replacement spans as arena `RESERVED` cells and may grow
backing arrays. Reserved cells hold the exact next metadata and migrated
enthalpy needed by local fragment compilation, but are absent from `isLive`,
`liveCellCount`, `highWaterMark`, live-slot iteration, solver state, and query
publication. A failed prepare discards only its reserved spans and restores the
free-span index; geometric backing growth may remain for reuse. Preparation
also checks endpoint generations, reference closure, final live-cell count,
operation limits, and arena/query capacity limits. The configured address
limit leaves staging headroom (`maximumArenaSlots` is twice the live-cell limit
in the production profile), so a valid replacement is not rejected merely
because its old span is still installed.

`TopologyCommitter` validates every structural version, Page owner, reserved
span, and phase-reservoir identity before its first authoritative write. It
then promotes all exact reserved spans, installs solver fragments/material
indexes, Page state, phase reservoir index, topology version, and Page
publication. The worker rebinds exact dirty source sections at the already
settled cut. All old spans are validated together before any is released, then
released last. No allocation, sort, validation, or recoverable branch occurs
after the first old-span release. There is no rollback copy of the complete
solver. An unexpected exception escapes to the mailbox, which closes that
engine and emits one terminal `ENGINE_FAILED` completion. A `WORK_LIMITED` cut discards staging; affected
Pages fall back, existing topology is retired when necessary, and admission is
backed off for `200` ticks instead of repeating a full rebuild every cut.

## Solver

`ThermalCellArena` is primitive SoA storage for enthalpy `H`, capacity `C`,
inverse capacity `1/C`, identity, phase state, and recyclable spans. Normal
fixed-step coefficients are compiled once:

```text
pair:     q = Kpair * (H_a / C_a - H_b / C_b)
boundary: q = Kboundary * (T_boundary - T_reference - H / C)
```

Buoyant pairs, phase contacts, FarField wind changes, and abnormal timing use
the generic inverse-capacity kernel. `ThermalSolver` keeps one execution
presence bitset per operation kind, so material/phase/FarField passes do not
walk fragments that cannot contain that operation. Forward and reverse order
remain deterministic and are selected directly from the batch sequence.

`ThermalSolver.maxTemperatureResidualC()` is only the final quiet-sleep gate.
It is not run while sources or topology changes are active and is not used as a
per-tick diagnostic.

## Sources And Energy

`PhysicalSourceSpatialIndex` is the main-thread authority for physical source
observations. It uses origin-section, origin-chunk, target-section, and source-ID
indexes. Source coordinates are decoded from the packed BlockPos ID, so no
parallel coordinate arrays are retained. Source state flags share one byte per
source and dirty ordering uses a reusable primitive list.

`ThermalSourceLedger` is the worker authority for source identity, exact event
ticks, port bindings, and power integration. A source event advances only the
nodes it changes; the active node list is drained once at the batch target tick.
Bindings carry target arena slot and lifecycle generation. Rebind settles the
old node at the current cursor before changing references. Source and
accumulator slots are recycled after power, pending energy, and binding
references reach zero.

The worker descriptor table is updated by the ledger's event observer in the
same order as register/unload events. Page topology commits return exact dirty
section keys; rebind never scans unrelated source descriptors or historical
generations.

## Environment And Phase

`MinecraftEnvironmentCapture` refreshes natural temperature on a staggered
200-tick queue and coalesces changed sky columns. Its Page builders are recycled
by `DimensionInputAccumulator`. Wind updates carry one scalar conductance scale;
FarField coefficients refresh lazily per affected fragment generation.

Phase reservoirs retain candidate masks in the Brick publication. Worker phase
requests contain the arena slot, lifecycle generation, Brick origin, profile,
candidate bit, and request sequence. Main-thread mutation ACKs are transferred
in the next 20-tick cut and are accepted only for the matching live reservoir.

## Query Publication

`QueryPublication` is a lock-free seqlock double buffer addressed directly by
arena slot. A publish writes live slots once through `ThermalCellArena.nextLiveSlot`
and stores slot generations, temperature, medium, flags, topology-resolved
state, and sample tick. It never counts then rewrites, retains slot keys, scans
arena holes, or binary-searches a sorted cell list.

Gameplay reads a Page's immutable current publication, resolves the local Air
point, reads the expected arena slot generation, and verifies that the same Page
publication is still current. Stale geometry, missing Page, unresolved topology,
and publication age all return the existing natural-temperature fallback.

## Cost Contract

For a routine cut, let `Kp` be changed positions, `Kb` affected Bricks, `Kf`
replaced fragments, `Ko` unique material execution operations, `Ke` affected
material keys, and `Ks` changed state slots.

| Path | Bound |
|---|---|
| mutation capture | `O(Kp * dependency offsets)` |
| topology prepare | `O(Kb + Kf + Ko + Ke log Ke + Ks)`; no unrelated Page/arena/high-water scan |
| ordinary commit | `O(Kf + Ke + Ks)`; delta prevalidation precedes all writes, with no allocation, sort, or post-write validation |
| source event update | changed sources and affected bindings only |
| source delivery | one ordered pass over active/touched nodes at target tick |
| query publish | one pass over live spans and live cells; unchanged sleep is `O(1)` |
| player cadence | stable UUID phase offset over the 20 ticks |

Capacity growth and fatal generation rebuilds are exceptional structural work.
They are measured separately from routine door/block/source/player workloads.

## Validation Standard

Production code contains no counters, traversal probes, test callbacks, debug
collections, or test-only constructors. Tests use deterministic outputs and
test-owned fixtures. Final performance evidence comes from external JVM JFR
and heap runs, not production bookkeeping.

The current functional validation is complete: `compileJava`, the thermal JUnit
selection (`108/108`), `compileGameTestJava`, and Forge GameTest (`14/14`) all
pass on Java 17; `git diff --check` reports no whitespace errors. Controlled
120-second door/block/source/player/crop JFR workloads and 10/30-minute
combined/churn heap runs remain performance evidence rather than undocumented
claims. Results and any remaining gap belong in the dated development diary.
