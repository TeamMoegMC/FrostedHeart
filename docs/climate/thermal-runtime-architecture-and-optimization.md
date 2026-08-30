# Thermal Runtime Architecture

- Status: `Current; functional validation complete, controlled profiling pending`
- Last verified: `2026-08-31`
- Scope: server-side thermal capture, asynchronous dimension workers, Page/Brick topology, source energy, phase requests, query publication, and hot-path cost bounds
- Primary code anchors: `MinecraftThermalEvents`, `MinecraftThermalInput`, `MinecraftPageManager`, `PhysicalSourceSpatialIndex`, `DimensionInputAccumulator`, `ThermalDimensionMailbox`, `ThermalWorkerPool`, `ThermalDimensionEngine`, `TopologyPlan`, `PreparedTopologyChange`, `TopologyCommitter`, `ThermalSolver`, `ThermalSourceLedger`, `QueryPublication`

## Source Layout

| Package | Responsibility |
|---|---|
| `thermal.runtime.minecraft` | Forge lifecycle and the public gameplay facade only |
| `thermal.runtime.minecraft.input` | Page interest, Minecraft state capture, phase ACK, and input accumulation |
| `thermal.runtime.minecraft.message` | Immutable main-thread/worker ownership-transfer messages |
| `thermal.runtime.minecraft.engine` | Per-dimension execution engine and runtime limits |
| `thermal.runtime.async` | Shared worker pool and single-slot dimension mailbox |
| `thermal.topology` | Brick compilation, Page topology state, migration, and transactional commit |
| `thermal.persistence.minecraft` | Chunk-owned dormant temperature checkpoints |
| `thermal.profile` / `thermal.profile.minecraft` | Immutable signature lookup and Minecraft BlockState/profile compilation |
| `thermal.source.minecraft` | Minecraft source profiles, main-thread source index, and worker binding |
| `thermal.radiation.minecraft` | Minecraft block-occlusion adapter for radiation |
| `thermal.field` / `thermal.query` | Analytic fields and published gameplay query values |
| `thermal.mesh` / `thermal.solver` | Primitive storage and numerical heat transfer |

Packages are grouped by function. Thread ownership is documented on the owning
classes; it is not used as a catch-all reason to place topology, source, or
persistence code under `runtime`.

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

Gameplay coefficients are read once from
`FHConfig.COMMON.THERMAL_RUNTIME` when
`MinecraftThermalProfiles.prepare()` creates the server-wide immutable tuning
snapshot. Workers receive plain `double` values and the same immutable campfire
profile used by the main-thread source index; no solver, source, query, or tick
path reads a `ConfigValue`. Values live in
`config/frostedheart-common.toml` and require a client or dedicated-server
restart after editing. There is no thermal config hot-reload lifecycle.

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
use `char[1]` for a uniform Brick and `char[64]` otherwise; wide IDs use the
corresponding `int[1]`/`int[64]` payload. A changed Brick alone is replaced or
promoted.

`WorkerPageStore` holds one stable 64-Brick worker directory and replaces only
changed immutable Brick entries. `WorkerBrickTopology` retains cell/query and
incremental contact metadata but does not duplicate the solver's fragment
reference. A cross-thread `PagePublication` directory is shallow-cloned lazily
only when a Brick's query payload actually changes. A publication that changes
only geometry/topology identity reuses the existing private immutable directory.
Arena spans use a pooled primitive AVL best-fit index, migrate locally, and are
released only after source and solver references have been rebound.

## Mutation Capture

`MinecraftPageManager.SectionOwner` is the only mutation inbox. The mixin path
records primitive section/local-position bits plus one cut-level source-relevant
boolean; it does not read world state, source indexes, heightmaps, or radiation
off-thread. Main-thread drain touches the physical-source index only when that
section's cut contains a campfire mutation. Door, fence-gate, trapdoor, and
ordinary material-only cuts therefore perform no source lookup or source-state
read. A section allocates one 4096-position changed bitmap on first mutation;
the second bitmap exists only after a non-geometry source-only position needs
to be excluded. Page center arrays are likewise created at eight entries only
after the first local geometry mutation. No per-position object is retained.

`MinecraftSignatureCapture` resolves that already-loaded state directly through
`StateStaticThermalResolver`. Static states do not allocate a dependency view or
re-read a cube from the world. Dynamic shapes remain conservatively unsupported,
and point capture uses `getChunkNow` without loading a chunk.

For a topology-relevant position, the owning Page captures only that exact
block index. `StateStaticThermalResolver` is a pure function of the stored
`BlockState` and `FluidState`, so neighboring positions are not recaptured and
each section owner retains only its own Page handle. Cross-Brick and cross-Page
effects are compiled from the changed Brick through the fixed fragment
neighborhood in `TopologyPlan.markFragmentNeighborhood`.

Repeated changes to the same position are coalesced until the next cut. Captured
center signatures stay in the Page manager until a batch is actually sealed.
Non-boundary ticks debounce a position that changed in the current tick; an
aligned cut captures its final state. One position therefore contributes one
final center entry per 20-tick cut, even when a door, fence gate, or trapdoor is
opened and closed repeatedly. The worker compares that final signature before
allocating a Brick scratch, so a final state equal to the installed state does
not rebuild an immutable signature payload.

State changes whose static signature is unchanged and which do not involve a
campfire are dropped before the inbox. Campfire changes still update the source
ledger, but do not invalidate geometry when their thermal signature is the same.

## Topology Preparation And Commit

`TopologyPlan` collects changed Pages, sparse centers, environment deltas, and
the exact one-Brick dependency closure. `BrickTopologyCompiler` produces local
Air, material, phase, and exposed FarField payloads using worker-owned reusable
scratch. `MaterialEdgeCompiler` groups changed contributions by packed edge key
in one pass and rebuilds only affected canonical owner executions. Reusable
named builders group material-contact and prepared-transaction arrays before
creating the same immutable primitive payloads; they do not add per-Brick or
per-transaction group objects.

When one cut retires a Page handle and admits a newer handle for the same
section, `TopologyPlan` represents them as one Page replacement. The new Page
reuses the committed worker Page slot, compiles one complete next Brick
directory, migrates current worker Air/material heat, replaces the exact local
fragment closure, and clears the old handle only after commit. It does not
perform a retirement transaction followed by a later admission, add a
20-tick temperature gap, or grow the Page/fragment address space. Outstanding
phase requests belong to the old lifecycle and are not copied to the new Page;
their stale ACKs are rejected by the existing lifecycle identity check.

One Brick compile carries its current `PageState` and `nextSignatures` as local
arguments. Interior material/microcell adjacency therefore uses those direct
references; `TopologyView` consults section/slot hash indexes only for genuine
cross-Page access. This avoids repeated same-Page map lookup without a retained
compiler cache.

`MaterialBoundaryRegistry` requires dense profile/contact-pattern IDs in
`1..N` list order and stores both catalogs in direct-index arrays. Brick
compilation performs bounds checks and array loads rather than boxed
`Map<Integer, ...>` lookups.

Preparation reserves replacement spans as arena `RESERVED` cells and may grow
backing arrays. Reserved cells hold the exact next metadata and migrated
enthalpy needed by local fragment compilation, but are absent from `isLive`,
`liveCellCount`, `highWaterMark`, live-slot iteration, solver state, and query
publication. A failed prepare discards only its reserved spans and restores the
free-span index; geometric backing growth may remain for reuse. Preparation
also checks endpoint ownership, reference closure, final live-cell count,
operation limits, and arena/query capacity limits. The configured address
limit leaves staging headroom (`maximumArenaSlots` is twice the live-cell limit
in the production profile), so a valid replacement is not rejected merely
because its old span is still installed.

`TopologyCommitter` validates every structural version, Page owner, reserved
span, and phase-reservoir identity before its first authoritative write. It
then promotes all exact reserved spans, installs solver fragments/material
indexes, Page state, phase reservoir index, topology version, and Page
publication. The worker rebinds exact dirty source sections at the already
settled cut. Solver/source references are checked once before the old spans are
released through the arena's single ownership check. No allocation, sort,
validation, or recoverable branch occurs after the first old-span release. There
is no rollback copy of the complete
solver. An unexpected exception escapes to the mailbox, which closes that
engine and emits one terminal `ENGINE_FAILED` completion. A `WORK_LIMITED` cut discards staging; affected
Pages fall back, existing topology is retired when necessary, and admission is
backed off for `200` ticks instead of repeating a full rebuild every cut.

## Solver

`ThermalCellArena` is primitive SoA storage for enthalpy `H`, capacity `C`,
inverse capacity `1/C`, identity, and recyclable spans. One arena-owned
`ThermalPhaseReservoirStore` holds phase metadata/request arrays by the same
slot, while `ThermalBrickCellLayout` is the reusable compilation input rather
than an arena responsibility. No per-cell phase or layout object is created.
Normal fixed-step coefficients are compiled once:

```text
pair:     q = Kpair * (H_a / C_a - H_b / C_b)
boundary: q = Kboundary * (T_boundary - T_reference - H / C)
```

Air pairs always use the production buoyancy kernel. Phase contacts, FarField
wind changes, and abnormal timing use the generic inverse-capacity kernel.
Operation payloads store arena slots without duplicate endpoint generations;
the topology transaction proves their ownership before old spans can be
released. A FarField fragment stores one owner Page and one lazy wind
coefficient generation rather than repeating them per boundary.
`ThermalSolver` keeps one execution
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
references reach zero. The production dimension limits are `65,536` physical
sources and `131,072` simultaneously retained source-node generations; source
growth beyond those explicit bounds cannot enter the worker batch. If an
observation is refused at the physical-source cap, the index records that
capacity recovery is required. After a slot is released, the Page manager
round-robins at most `64` already-scanned loaded chunks per 20-tick cut until
the refused loaded sources have been observed again. This exceptional recovery
uses the existing chunk set and retains no overflow-source objects.

`ThermalSourceMode.IMPULSE` is an intentionally retained exact-tick contract.
It routes one signed energy amount in joules to a selected source port instead
of integrating continuous watts over time. No current gameplay producer emits
an impulse, but the contract is explicitly part of the retained source model.

The worker descriptor table is updated by the ledger's event observer in the
same order as register/unload events. Page topology commits return exact dirty
section keys; rebind never scans unrelated source descriptors or historical
generations.

## Environment And Phase

`MinecraftEnvironmentCapture` refreshes natural temperature on a staggered
200-tick queue and coalesces changed sky columns. Its Page builders are recycled
by `DimensionInputAccumulator`. Wind updates carry one scalar conductance scale;
FarField coefficients refresh lazily once per affected fragment.

Phase reservoirs retain candidate masks in the Brick publication. Worker phase
requests contain the arena slot, lifecycle generation, Brick origin, profile,
candidate bit, and request sequence. Current production profiles apply only the
compiled `StateTransitionData` heating recipe and respect random-tick speed.
Main-thread mutation ACKs are transferred in the next 20-tick cut and are
accepted only for the matching live reservoir.
`ownsGameplayHeatingTransition` consults the precompiled phase-profile index and
does not reconstruct `StateTransitionData.HeatingTransition` on random ticks.

## Query Publication

`QueryPublication` is a lock-free seqlock double buffer addressed directly by
arena slot. A publish writes live slots once through `ThermalCellArena.nextLiveSlot`
and stores only slot generations and temperature; topology generation and sample
tick remain in the publication envelope. It never counts then rewrites, retains
slot keys, scans arena holes, or binary-searches a sorted cell list.

Infrared tracking reuses that same live-slot write. A dimension keeps one
`temperatureChangeId` and `long[maximumPages] pageChangeIds`; while the
80-tick activity window is open, Air temperatures are compared at 0.25degC
quantization and changed Pages receive the next ID. Inactive publication pays
one deadline branch and performs no infrared comparison. Reactivation advances
one ID and fills the fixed Page backing so clients with an older ID rebuild
without a server-side observer. The fixed Page backing and geometrically growing
cell buffers hold separate reservations in the same dimension/server memory
budget.

`InfraredReadCursor` fixes one buffer, slot generations, Page change IDs,
sample tick, topology generation, and publication version for a complete
response. Main-thread encoding validates it once after staging. Page geometry
gaps use `ThermalPageHandle.lastPublication`; retirement alone removes
presence. `PagePublication.workerPageSlot` remains server-internal and maps the
immutable Page directory to its change ID.

Gameplay reads a Page's immutable current publication, resolves the local Air
point, reads the expected arena slot generation, and verifies that the same Page
publication is still current. During a bounded topology gap it may read the last
coherent Brick temperature without using stale point ownership. If no coherent
worker cut exists, it performs one O(1) lookup in the loaded chunk's dormant
section before falling back to natural temperature.

## Dormant Chunk Temperature

`DormantChunkThermalState` is a lazy `LevelChunk` attachment. Async
`ChunkDataEvent.Load` only validates and decodes primitive NBT. Main-thread
`ChunkEvent.Load` consumes the disk-only `sourceSustained` bit once, applies one
factor to each complete Brick mean/component vector, rebases to the load tick,
and clears the bit before random ticks. Normal queries and worker admission never
read that support bit.

Stored temperature uses signed `1/16 C` residuals from section-center
`WorldTemperature.naturalAir`. Pages with at most `256` Air components preserve
exact mixed component order and a capacity-weighted Brick mean; larger Pages
store one mean per Brick. Missing/count-mismatched geometry restores from the
mean. Partial phase energy and topology are not persisted.

Retirement captures one coherent `PagePublication`/`QueryPublication.sampleTick`
before clearing the handle. Save, unload, stop, recipe reload, and terminal
worker replacement reuse the same Page-local capture. Save/unload refresh the
disk-only support bit from at most the target section and six face neighbors
while `PhysicalSourceSpatialIndex` is still live. Campfire, generator, radiator,
and fountain qualify; `IMPULSE` does not. Existing warm Brick vectors may be
held across an unloaded interval, but no offline solver or source integration
adds heat.

`FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds` defaults to
`1800`. Ordinary fallback caches one natural temperature and decay factor per
section per aligned 20-tick boundary. A regular/collapsed Page uses packed rank
directly; only exact mixed data owns derived lookup arrays. Unloaded chunks own
no runtime heap and dormant data is never synchronized to clients.
When a runtime is active, dormant fallback resolves the loaded chunk through the
existing `MinecraftPageManager.SectionOwner`; one lookup supplies both Page
handle and chunk, so the normal path does not enter `ServerChunkCache.getChunkNow`
or allocate its Optional/future wrappers. If a section was loaded before the
runtime existed, its first query performs one `getChunkNow` and lazily attaches
that section owner; subsequent queries use the owner directly. The no-runtime
bootstrap boundary retains `getChunkNow` for correctness.

## Cost Contract

For a routine cut, let `Kp` be changed positions, `Kb` affected Bricks, `Kf`
replaced fragments, `Ko` unique material execution operations, `Ke` affected
material keys, and `Ks` changed state slots.

| Path | Bound |
|---|---|
| mutation capture | `O(Kp)` exact positions |
| topology prepare | `O(Kb + Kf + Ko + Ke log Ke + Ks)`; no unrelated Page/arena/high-water scan |
| ordinary commit | `O(Kf + Ke + Ks)`; no allocation or sort, with one exact old-reference/ownership proof before release |
| source event update | changed sources and affected bindings only |
| source delivery | one ordered pass over active/touched nodes at target tick |
| query publish | one pass over live spans and live cells; unchanged sleep is `O(1)` |
| dormant capture | `O(64 + Page Air components)`, only at checkpoint |
| dormant query | O(1), allocation-free after lazy section cache |
| dormant activation | one bounded pass over that section's stored values per disk load |
| player cadence | stable UUID phase offset over the 20 ticks |

Capacity growth and fatal generation rebuilds are exceptional structural work.
They are measured separately from routine door/block/source/player workloads.

## Validation Standard

Production code contains no counters, traversal probes, test callbacks, debug
collections, or test-only constructors. Tests use deterministic outputs and
test-owned fixtures. Final performance evidence comes from external JVM JFR
and heap runs, not production bookkeeping.

The current functional validation is complete: `compileJava`, the thermal JUnit
selection (`99/99`), `compileGameTestJava`, and Forge GameTest (`14/14`) all
pass on Java 17; `git diff --check` reports no whitespace errors. Controlled
120-second door/block/source/player/crop JFR workloads and 10/30-minute
combined/churn heap runs remain performance evidence rather than undocumented
claims. Results and any remaining gap belong in the dated development diary.
