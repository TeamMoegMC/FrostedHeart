# Thermal Runtime Architecture

- Status: `Transitional; material-body replacement is under integration; controlled performance comparison pending`
- Last verified: `2026-09-15`
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
| `thermal.radiation.minecraft` | Receiver-lazy block-radiation index and Minecraft occlusion adapter |
| `thermal.field` / `thermal.query` | Analytic fields and published gameplay query values |
| `thermal.mesh` / `thermal.solver` | Primitive storage and numerical heat transfer |

Packages are grouped by function. Thread ownership is documented on the owning
classes; it is not used as a catch-all reason to place topology, source, or
persistence code under `runtime`.

## Ownership

An enabled positive-power machine report can start the existing dimension runtime
without a player query. Lit campfires bootstrap through chunk load or the existing
CampfireBlock mixin's `onPlace` callback; server start and the server-side tags-updated
event inspect loaded block-entity positions once. Recipe listeners invalidate
profiles without rebuilding against old tags. Subsequent discovery uses the original bounded queue.
Inactive/zero-power reports and analytic-only queries do not bootstrap physics.
The ignition callback is specific to campfires, including LIT state transitions;
ordinary LevelChunk block writes have no added campfire hook. Forge snapshot
placement invokes it after acceptance, and the original discovery queue reads
block-entity positions after placement completes.
Source bootstrap uses the dimension baseline as the enthalpy reference, avoiding
biome/chunk reads during chunk attachment; admitted cells still initialize from
their Page's captured natural temperature.

Each active thermal `ServerLevel` has one `MinecraftThermalInput` on its level thread and one
`ThermalDimensionEngine` on a bounded thermal worker. Minecraft objects never
cross into the worker. The main thread reads loaded state and produces immutable
primitive cuts; the worker owns arena cells, topology, source integration,
phase state, solver execution, and publication.

Analytic gameplay fields have world lifetime through `MinecraftGameplayFields`.
The runtime caches the same `ThermalAnalyticFieldIndex` reference; field-only
publication does not construct a dimension runtime or attach loaded sections.
World unload/server stop clear the index, while physical close/profile reload
retain it. Main-thread fields never enter worker messages or Page hot masks.

The field index shares entries between an update map and a compact ordered list.
Unchanged sphere reports compare primitives without allocating a definition or
sorting; geometry/value changes retain their ordered slot. Insert/remove/reorder
cost O(F), and queries still scan O(F) fields. Generator provider reconciliation
adds O(F) work around the existing team enumeration, with no new world scan or
polling scheduler. Ordinary queries compose directly; town/phase callers reuse
reductions when they need a local natural baseline or several thresholds.
The update map adds memory and an entry indirection versus the former single
list. Ownership separation itself provides no steady-query speedup. Radius does
not allocate cells; existing physical source residency and its Page overhead
remain independent costs. See [composition](world-climate-and-temperature.md#7-analytic-control-fields)
and [lifecycle](data-lifecycle-and-integration.md#reload-and-restart).

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

Page is the lifecycle/container address, not the residency unit. Main-thread
state keeps one captured mask, lazy exact source-seed counts/mask, and the last
worker-desired mask. Worker state keeps only resident, resolved, source-seed,
and hot masks. Player, infrared, crop, town, dormant, and static-radiation
queries never enter those masks.

A physical `AIR_FACE` port seeds its exact Brick. Page admission starts from a
shared unresolved signature directory and captures only requested Bricks under
the fixed 64-Brick-per-tick main-thread budget. `LevelChunkSection.hasOnlyAir`
installs canonical Air payloads without individual BlockState reads; otherwise
one new Brick reads exactly 64 final states. Resident bits grow within one Page
lifecycle and inactive slots own no arena cell, fragment, or infrared payload.

After each 20-tick solve, `QueryPublication` computes temperature-hysteretic hot
masks while performing its existing live-slot write, using reusable Page-slot
primitive scratch and `REFINE_HIGH_C = 0.125 C` /
`RELEASE_LOW_C = 0.0625 C`. Air and material cells contribute their
physical temperature residual. A body with phase progress or a pending material
transition request keeps its Brick hot. The following Page-only pass
uses six static bit masks/shifts to find same-Page
nonresident faces and active cross-Page boundary faces, then reads only their
existing regular/mixed face topology. A cross-Page face keeps its admitted guard
desired until residual release; missing non-sky neighbors have no synthetic
FarField sink. Changed absolute `BrickResidency` masks request same-Page or
cross-Page guards and include zero-mask cancellation; unchanged masks produce
no completion payload. A newly committed Page lifecycle is the one exception:
one reusable primitive admission list forces its current absolute mask to be
published once even when the section's numeric mask matches the previous
lifecycle. The list is cleared by that completion and adds no steady Page scan.

An unavailable target chunk leaves its absolute request parked outside the
admission queues. The existing `pagesByChunk` lookup and `ChunkEvent.Load`
enqueue it when world data becomes available, so thermal propagation neither
loads the chunk nor polls `getChunkNow` every tick. A work-limited cold admission
keeps the committed worker topology and request, but discards its uncommitted
Minecraft signatures; retry backoff recaptures current final BlockStates.

If all main-thread interest disappears before a Page's first worker publication,
an admission still owned by the mutable accumulator is cancelled immediately.
An already sealed or in-flight admission retains its handle and queues the
latest resident/source-seed state for the following cut. The mandatory first
lifecycle residency completion then supplies either residual ownership or zero,
after which the ordinary retirement path runs. Losing interest immediately
removes the Page from its admission queue, so `queuedPriority` always describes
real queue membership if the source returns before that completion. A refused
admission with no remaining interest is discarded instead of entering
work-limit backoff.

When source and hot/incoming frontier ownership all disappear, the worker emits
zero desired residency. The main thread then checkpoints and retires the whole
Page transactionally. Bricks are not individually evicted, avoiding enthalpy
migration and threshold chatter.

`ThermalPageHandle` is only cross-thread identity, live geometry revision,
resync requirement, and a volatile `PagePublication`. `PageSignatures` stores a
flat directory of `64` immutable Brick payloads. Uniform Bricks reuse one
canonical immutable `Integer` per signature ID; nonuniform compact/wide Bricks
use `char[64]`/`int[64]`. Its reusable builder accepts only Brick-level reset and
replacement operations; no flat 4,096-entry test scratch exists in production.
A changed Brick alone is replaced or promoted.

`MinecraftStateThermalTable` is one dense registry-ID-indexed tagged int table.
Ordinary states contain their signature ID directly; only radiation, Campfire,
or exceptional occlusion semantics enter sparse primitive extended arrays.
`ThermalSignatureTable` stores shared whole-block ventilation (0–100) and
material profile IDs. Unknown dynamic shapes and partial collision shapes use
75; full solids use 0 and ordinary Air uses 100. No block-internal microcells,
void geometry, or material contact-pattern catalog remain.

`BlockBrickLayout` maps 64 whole blocks to actual Air regions followed by material
bodies. Connected material-free V100 Air members merge; full Air uses one node
without a layout. A material block owns one H and a fixed-capacity law regardless
of exposure. Its ventilation contributes geometric routes, never gap-Air capacity
or a second temperature. The ordinary Brick total remains at most 64 nodes.

`WorkerPageStore` holds one stable 64-Brick worker directory and replaces only
changed immutable Brick entries. `WorkerBrickTopology` retains cell/query and
whole-block membership and phase metadata but does not duplicate the solver's fragment
reference. A cross-thread `PagePublication` directory is shallow-cloned lazily
only when a Brick's query payload actually changes. A publication that changes
only geometry/topology identity reuses the existing private immutable directory.
Arena spans use a pooled primitive AVL best-fit index, migrate locally, and are
released only after source and solver references have been rebound.

Mixed transport nodes occupy consecutive arena slots, so their layout index is
`slot - supportRef`; there is no separate per-slot component-index array.
Worker phase metadata retains the phase-capable material slot list and published candidates;
migration reads profile identity from the arena within the same Brick/lifecycle,
without a second coordinate/profile directory. Neighbor-only geometry changes
recompile contact eligibility; the old exposure-dependent capacity rebuild is removed.
Shared-face compilation resolves each neighbor Page/cut once before its 16 pairs;
sky faces read the already selected owner cut. These changes reduce duplicate
storage and lookups without adding persistent caches.

## Mutation Capture

Both existing `setBlockState` bridges now use MixinExtras `ModifyReturnValue`;
they preserve the return value and do not allocate callback containers. The Section
bridge skips unowned/no-op writes locally; the Chunk bridge skips client/no-op writes.
They are not merged: the Chunk entry also maintains stored material without an active
SectionOwner. Ordinary untracked positions do not classify material changes or look
up natural temperature. Material checkpoint journal entries contain three ints
(position, next state, cause) plus a revision; pruning is limited to once per touched
Section/tick instead of rescanning on every write.

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

Lazy runtime startup synchronously attaches every currently loaded section,
including sections without a source. `MinecraftThermalInput.attachLoadedWorld`
enumerates visible chunk holders through loaded-only `getChunkNow`; it does not
scan all blocks or instantiate block entities. Section identity lookup uses the
existing attachment; `ownersBySection` is the sole owner map. Repeated attachment
preserves pending mutations. Three inline volatile flags with shared `VarHandle`
operations retain atomic enqueue/full-resync semantics without per-owner atomic
wrapper objects. Mutation bitmaps remain lazily allocated and reused.

Startup, real chunk loads, and exact capacity refusals share one ordered
`pendingSourceChunks` map. `drainPendingSources` attempts at most eight chunks
per tick, enumerating pending/live BE positions via `getBlockEntitiesPos()` and
reading current BlockState. Completed chunks leave the map; refused chunks go
to its tail. Source capacity exhaustion pauses discovery but not mutation,
removal, or flush. Startup prioritizes loaded chunks within 3x3 of its trigger.
Page admission has no discovery side effect, and campfire `cookTick` only manages
fuel lifetime. Stable campfires require no discovery polling.

Synchronous attachment is O(loaded sections); candidate discovery is proportional
to BE positions, plus retries, and allocates a temporary position union per chunk.
Eight chunks is a work-count budget, not a hard millisecond bound. Queue backing
arrays and dirty bitmaps can retain capacity; the 128 MiB reservation budget does
not bound the runtime's entire Java heap. Source discovery, cut submission, Page
admission, and observable warming are separate steps.

`MinecraftSignatureCapture` resolves an already-loaded state through one state
registry ID lookup and one tagged-table read. `StateStaticThermalResolver` runs
only while the server-wide tables are built. Dynamic shapes remain
conservatively unsupported. Residency capture reads the already-attached
section and never loads a chunk; exact mutation point capture retains
`getChunkNow` only as its loaded-only fallback.

For a topology-relevant position, the owning Page captures only that exact
block index when its Brick is resident. Uncaptured Brick mutations still update
sky/occlusion/source channels but do not invalidate Page geometry; their first
future residency capture reads final state. `StateStaticThermalResolver` is a pure function of the stored
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

State changes whose complete tagged topology/source/radiation/occlusion
semantics are unchanged are dropped before the inbox. Campfire changes still
update the source ledger, but do not invalidate geometry when their thermal
signature is the same.
Static fire/lava profile changes write only one radiation Brick bit; a
signature-equal DDA occlusion change invalidates the section witness without
allocating the Page mutation bitmaps.

## Static Block Radiation

When `FHConfig.COMMON.THERMAL_RUNTIME.enableStaticBlockRadiation` is enabled,
`BlockRadiationIndex` stores one `knownBrickMask`, one `emitterMask`, and compact
packed emitters only for receiver-covered palette-positive sections. Fire uses
fixed power; lava power is proportional to loaded-only exposed face area. This
field is a read-only player observation: it never writes Air, material, phase,
dormant, or infrared state and never registers a `ThermalSourceLedger` source.

Relevant profile changes and lava's own `LiquidBlock.updateShape` callback merge
into two primitive dirty maps until the 20-tick cut. Each dirty Brick reads its
64 final states once. Player sampling performs one fused pass over at most eight
loaded sections. `LevelChunkSection.maybeHas` rejects palette-negative sections
without retained state; palette-positive sections queue only unknown Bricks in
one shared primitive pending map. A fixed 64-Brick-per-tick budget bounds cold
capture. Section-local bit masks cover the conservative 8-block cube without a
125-Brick query loop; actual emitters still require the exact spherical range.

Thermal Page admission, recapture, retry, retirement, worker restart, and
dormant restore never install or remove radiation state. Covered sections reuse
the existing mutation owner only to receive final-state callbacks. Chunk unload
removes its fixed section keys; chunk/section availability marks only known
touching boundary Bricks. Neighbor compilation reads loaded sections directly
and never loads a chunk.

All lava states share one startup-compiled radiation profile. `LiquidBlock`
caches lava membership per block singleton and compares one profile epoch; a
recipe/tag reload changes that epoch, so the next liquid neighbor update
reclassifies once without adding a tag lookup to every steady update.

`RadiationService` discovers physical sources first. Static emitters use only
remaining visits, but the fused coverage pass still runs when none remain. Exact
`STATIC_BLOCK_REVISION` selects one current-eye block-grid DDA and bypasses
`ReceiverCache`, section revisions, and witness writes. Physical Campfire and
machine rays use the same DDA with their existing feet/torso/head witnesses.
Logout and old-dimension exit remove only that physical receiver cache by key.
The physical witness revision table and static coverage index share the
`3,200`-section dimension bound. This covers the declared 128 receivers and
100-player deployment without an eviction traversal or historical index.

## Topology Preparation And Commit

`TopologyPlan` collects changed Pages, sparse centers, environment deltas, local
contact dependencies and affected passage components. `BrickTopologyCompiler` produces
direct Air/material contacts, indirect routed contacts and exposed FarField payloads using worker-owned reusable
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

Brick compilation uses 144 interior block-face pairs and 16 aligned pairs per
shared face; full-Air/full-Air uses one area-16 connection. Interior attributes
come from reusable 64-entry scratch. Shared faces belong to the negative-coordinate
Brick, including material-only owners. Transport conductance is
`K*A/(dL/pL+dR/pR)`, where `p=V/100`, A is face area in square blocks, K is
`effectiveMixingWPerBlockK`, and each center-to-face distance d is at least 0.5
blocks. Material contacts keep their profile's face conductance. Direct-sky
boundaries retain existing eligibility and wind scaling, with conductance
multiplied by p. The solver continues to use exponential pair exchange.

`MaterialBoundaryRegistry` stores dense profile IDs in `1..N` list order.
There are no retained microcell contact lists or duplicate material-pole
coordinate directories. Neighbor geometry changes update contact edges and the
two-layer material range; they do not alter an existing body's capacity. Fragment recovery
uses `cellsResolved && compiled.resolved()` and marks source bindings dirty
when resolved status changes.

Preparation reserves replacement spans as arena `RESERVED` cells and may grow
backing arrays. `ThermalCellArena.stageBrickCells` returns `ArenaSpan` directly;
the former single-field `BrickAllocation` wrapper is removed. Reserved cells hold the exact next metadata and migrated
enthalpy needed by local fragment compilation, but are absent from `isLive`,
`liveCellCount`, `highWaterMark`, live-slot iteration, solver state, and query
publication. A failed prepare discards only its reserved spans and restores the
free-span index; geometric backing growth may remain for reuse. Preparation
reserves the material-edge table for the larger of its final edge count and
the current count plus all possible insertions, covering insertion-before-
deletion commit order without allocation in `TopologyCommitter`. Preparation
also reserves the admission-identity list for the exact number of Page
admissions in that prepared cut, so its commit append cannot grow backing.
Brick migration uses bounded 64-entry scratch. New Bricks without dormant state
and regular-to-regular migration take constant-time fast paths. Mixed migration
visits at most 64 whole blocks, preserving each surviving same-material member's
share of old enthalpy. Exposure or door-state changes retain enthalpy and use the
new capacity to determine temperature. Phase requests migrate separately. Preparation
also checks endpoint ownership, reference closure, final live-cell count,
operation limits, and arena/query capacity limits. The configured address
limit leaves staging headroom (`maximumArenaSlots` is twice the live-cell limit
in the production profile), so a valid replacement is not rejected merely
because its old span is still installed.

`TopologyCommitter` validates every structural version, Page owner, reserved
span, and phase-material identity before its first authoritative write. It
then promotes all exact reserved spans, installs solver fragments/material
indexes, Page state, phase-material index, topology version, and Page
publication. The worker rebinds exact dirty source sections at the already
settled cut. Solver/source references are checked once before the old spans are
released through the arena's single ownership check. No allocation, sort,
validation, or recoverable branch occurs after the first old-span release. There
is no rollback copy of the complete solver. If failure occurs after Page
references were exchanged but before Query publication commits, only those Page
references return to the prior immutable cut. Query preparation writes its
inactive buffer while the old envelope remains readable and opens the seqlock
write window only for the final metadata exchange. An unexpected exception
therefore emits one terminal `ENGINE_FAILED` completion with a coherent last cut.
The main thread checkpoints it, then terminal ACK closes the old engine before
the replacement generation reuses Page handles. A close exception is logged but
cannot retain `inFlight` ownership or suppress replacement. A `WORK_LIMITED` cut discards staging; affected
Pages fall back, existing topology is retired when necessary, and admission is
backed off for `200` ticks instead of repeating a full rebuild every cut.

## Solver

`ThermalCellArena` is primitive SoA storage for enthalpy `H`, capacity `C`,
inverse capacity `1/C`, identity, and recyclable spans. One arena-owned
`ThermalPhaseRequestStore` holds only request sequence and state by the same
slot; it has no latent or reserved energy arrays. `ThermalBrickCellLayout` is
the reusable compilation input. Every material body references one shared
`MaterialThermalLaw`; temperature and phase progress derive from its single H.
Normal zero-offset fixed-capacity coefficients are compiled once:

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
presence bitset per operation kind, so material/FarField passes do not
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

Every positive-share `AIR_FACE` port of an emitting source retains an exact
`(sectionKey, Brick index)` target.
Multiple sources in one Brick share a lazy main-thread int reference count;
only zero/nonzero transitions change the source-seed mask. Source discovery and
seed capture are independent of player queries.

`ThermalSourceLedger` is the worker authority for source identity, exact event
ticks, port bindings, and power integration. A source event advances only the
nodes it changes; the active node list is drained once at the batch target tick.
Bindings carry target arena slot and lifecycle generation. Rebind settles the
old node at the current cursor before changing references. Source and
accumulator slots are recycled after power, pending energy, and binding
references reach zero. The production dimension limits are `65,536` physical
sources and `131,072` simultaneously retained source-node generations; source
growth beyond those explicit bounds cannot enter the worker batch. A refused
state observation returns false, and its main-thread caller queues the exact
loaded chunk in `MinecraftThermalInput.pendingSourceChunks`. Removal is flushed
before discovery on cut ticks, so released capacity can be reused immediately.
Machines retry through their next normal complete production output. No completed
chunk directory or separate capacity-recovery pass is retained. Inactive and
zero-power Minecraft sources are removed at the cut; the generic worker ledger's
enabled and impulse contracts remain unchanged.

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
200-tick queue and coalesces changed sky columns. Each completed sample schedules
its next deadline at the actual game tick plus 200; delayed entries do not catch
up historical samples. Stale entries still consume the 16-entry tick budget.
Initial residency samples one
natural temperature and only the 16 heightmap columns of each newly resident
top-layer Brick; other columns remain unknown value `16`. Its Page builders are recycled
by `DimensionInputAccumulator`. Wind updates carry one scalar conductance scale;
FarField coefficients refresh lazily once per affected fragment.

Phase-capable bodies retain only the worker's phase-slot list; the unused published
profile/mask candidate arrays have been removed. Worker requests name the exact block, arena slot,
Page lifecycle, material profile, branch, target state and request sequence.
`PhaseTransitionRuntime` indexes phase-capable bodies by block position using
the existing fastutil library. `ThermalSolver` has no separate `PhaseContacts`
payload or fixed-threshold phase pass: material contacts use the ordinary fast
kernel or bounded `MaterialEnthalpyExchange` segments. Exchange stops at an ACK
boundary; ACK itself consumes no energy. Replacing the world block installs the
target law over the same H. Main-thread mutation continues to respect random-tick
speed and recipe/biome eligibility. Explicit gameplay phase intents pass through
the worker and record the external energy change before requesting mutation.
`tryMaterialPhaseAtRandomTick` uses existing Section ownership and scalar stored
records. Its `GAMEPLAY / DEFERRED / CHANGED` result distinguishes natural or
explicit recipe gameplay from owned phase energy and successful world mutation.
The facade delegates to `MinecraftPhaseController.tryAtRandomTick`, which owns
phase rules and submission. `DormantChunkThermalState.projectMaterial` owns stored
projection/law adaptation, while `MinecraftGameplayFields.guaranteedFloor` owns
field sampling. These details are not implemented in `MinecraftThermalInput`.
`MinecraftMaterialLawCompiler` now makes two passes over explicit data: C/O references,
then heating/cooling endpoints. There are no native-material branches or inferred phase
slots. Submission constraints and effects are two shared reference arrays per profile
snapshot, with no new per-node or saved-state allocation. See
[material data](heat-production-and-network.md#material-transition-data).
Water does not add block random-tick eligibility. `StateTransitionData.hasRandomTransitions`
also excludes it from generic phase dispatch in otherwise ticking Sections. Original
chunk surface sampling (default one column per 20 ticks) calls the shared controller;
the sampling count does not grow with ocean depth. Active water energy/phase requests
still use their existing worker path. No per-water state is added for sampling.
Vanilla `LevelChunkSection.setBlockState` counts nonempty fluids in its incremental
fluid counter, whereas `recalcBlockCounts` counts randomly ticking fluids. Consequently
a mutated pure-water Section can still be sampled by vanilla eligibility. This behavior
is preserved; the mod no longer adds water block eligibility or per-sample water freezing.
An owned but deferred transition cannot fall through to an environmental threshold;
unrelated native random behavior, such as lava ignition, still runs.

## Geometry Changes While Rebuilding

`AirRouteCompiler` expands at most 4096 route visits per 20-tick cut; uncompleted
work and dirty fragments survive to the next cut. `routeVisitsLastCut()` reports
zero when stable. This is a work counter, not a hard wall-clock deadline: final
edge preparation and publication also cost time.

`ThermalFragment.RoutedContacts` retains a shared `AirRouteValidity` for each
indirect connection. Retirement disables that connection immediately in the worker
cut; a new route becomes active only after matching geometry publication commits.
These contacts use the existing conservative exchange kernels without buoyancy.
Independent parallel passages retain independent validity. There is no route H/T.

While a route is invalid, its Q is zero and endpoint energy remains stored; other
valid contacts continue. No historical heat transfer is replayed after rebuilding.
A topology budget refusal also rebinds affected source ports and requeues halo
captures. Source power that cannot be delivered uses the existing loss/unaccepted
accounting rather than an accumulated heat debt.

`ThermalCellArena.pendingMaterialLayouts` holds one bit per arena capacity slot.
Changed material awaiting a replacement layout stops contact exchange, source
acceptance and new phase requests. Its old H remains available to checkpoint
projection; public world consumers still require matching geometry identity.
Releasing the old span clears the bit. This is an explicit temporary simulation
pause, not an estimate of exchange through the new geometry.

## Query Publication

`QueryPublication` is a lock-free seqlock double buffer addressed directly by
arena slot. A publish writes live slots once into the inactive buffer through
`ThermalCellArena.nextLiveSlot` while readers retain the old envelope. Only the
final buffer/envelope exchange makes the seqlock odd, so preparation failure
cannot strand or corrupt the previous cut. The buffers store only slot
generations and temperature; topology generation and sample tick remain in the
publication envelope. It never counts then rewrites, retains slot keys, scans
arena holes, or binary-searches a sorted cell list.

Infrared tracking reuses the live-slot publication pass. `BlockBrickLayout` has
one immutable `long surfaceNodeMask`, shared by worker topology and
`PagePublication`. It covers all material bodies, including phase-capable bodies.
`ThermalCellArena.isSurfaceCell` works for RESERVED and LIVE allocations without
changing Air/source eligibility, mixed centers, cell kinds, or per-slot arrays.
Full-Air Bricks still have one node and no layout. A material-only Brick publishes
its actual `firstSlot` and arena generation; Air consumers additionally require
`transportNodeCount > 0`. The worker's internal `coverageSlot` stays Air-only.

The first request lazily allocates Page/Brick epochs and pending masks. During the
80-tick activity window, only surface temperatures are compared at 0.25°C in the
existing write pass. Exact topology masks and temperature changes stamp one epoch
atomically. Reactivation advances an epoch and fills both arrays. No second solver
sweep or server observer cache is added. The existing fixed reservation remains
292 bytes/Page, including `HotMaskScratch`; Page/Brick epochs at 3,200 Pages use
832,000 bytes. Each existing block layout adds eight bytes of numeric payload.

`MinecraftThermalInput.InfraredCapture` is one lazy main-thread scratch. Existing
handles are gathered with `MinecraftPageManager.pagesByChunk`; current layouts,
slot generations, topology generations and the coherent `InfraredReadCursor`
select readable material Pages. Phase-capable bodies use their material law;
Air and dormant Air temperatures do not become material measurements.
For nonresident Bricks it also reads existing material records from loaded chunks.
Live resolved Bricks take precedence, and an updating live Page suppresses old
stored records until its geometry is coherent. `MaterialSectionState.read` is
shared with the thermometer, so saved phase energy uses the same temperature law.
Unchanged non-field Pages use Page/Brick epochs. Each eligible node is read once;
field Bricks expand raw double values before composition and quantize only the
final result, while non-field Bricks keep the quantized-node fast path.

Stored-material discovery checks at most 81 already-loaded chunks and nine vertical
Sections each. `storedPresence` is a 96-byte work mask; it does not own material
state. There are no `storedPages` or `storedRevisions` window arrays: the capture
reads Section records/revisions directly from 81 reused chunk references. One
reused 4096-double buffer expands a changed stored Section before the existing
Brick encoding and field composition. Added capture scratch is about 33 KiB,
shared across observers; chunk references are cleared when the capture returns.

`DormantChunkThermalState.materialRevision` advances when material content changes
or a refreshed natural-temperature cache invalidates the previous display. Its lazy long array costs eight bytes per Section of a chunk with material
history (192 bytes for a typical 24-Section chunk, excluding array headers).
The existing chunk attachment holds another eight-byte revision for replacement,
clearing and load, and each immutable material snapshot caches an eight-byte Brick
mask. These revisions are transient and use one process-wide sequence.
`storedEpoch` adds one VarLong, and `storedSampleTick` adds one long, to each
request/response header. The client commits both only on LAST. Stored fallback
compares the current and previous committed tick's final quantized material
temperatures, sending only changed Bricks. Changes to natural temperature or law
invalidate the old prediction through the existing revisions. The scratch buffer
also holds current values for encoding; it is not expanded a second time.
Same-age records reuse local sensible fractions, without a persistent factor table.
This supports time-only cooling, partial-live Pages and deletion without per-observer
server caches or old-protocol readers. Observer requests still incur separate CPU/network work.

`MinecraftGameplayFields.existing` supplies the original ordered analytic fields.
Window/Page/Brick intersections prune candidates, then exact block-center contains
checks feed the reusable `ThermalAnalyticFieldIndex.Sample`. The generator remains
FLOOR_FROM_NATURAL, including when a colder material value exists. Only an actual
field hit requiring natural/base evaluates `WorldTemperature.naturalAir`; override
skips it. Necessary loaded-biome neighbors are prepared lazily once per Page.
Proven uniform-biome Bricks reuse at most four occupied Y-layer natural values;
mixed biomes use exact point queries. No full-window natural grid is built, no
chunks are loaded, and display reads never start/admit/retain physical simulation.

The client echoes a field-Page footprint (empty or twelve longs), independent of
material presence and epoch. Each poll rebuilds the union of previous and current
field footprints. This handles field edits, natural changes and removal without a
server observer/history/cache or new field revisions. A Page is encoded only once.
Field deletion restores current material or INVALID, never the previous composed
value. The client no longer clears display by material-presence XOR: every delta,
including added/removed Pages, sends explicit INVALID when needed. Only full may
omit invalid Bricks after clearing the mirror. Local geometry invalidation does not
erase material temperatures. Infrared uses the last immutable Page publication and
the coherent query cut, excluding changed bodies through the existing mutation journal.
`collectMaterialChangesSince` expands the journal once per stale Page into one shared
64-long (512-byte) scratch; only affected Bricks need an extra delta. Unchanged bodies
in the same Brick/Page remain readable. Full Section replacement excludes every old
body. `sampleMaterial` uses the same identity rule; Air queries retain current-geometry
requirements. No additional temperature history or observer cache is created.

Two complete read attempts handle publication exchange. A concurrent version/slot/Page
mismatch retries; if both attempts conflict, no response is sent and the client's last
committed baseline remains visible until its next normal request. Read contention is
never serialized as INVALID removal. A stably invalid/older-than-40-tick cut still
yields a complete field-only display. Material readability
transitions force full. Subsequent unchanged unreadable state permits delta and
can send nothing when no fields remain. Generation/center changes, reactivation,
epoch reset and explicit client invalidation also force full. All borrowed handles
and field/world references are cleared after capture. The extra server arrays are
one bounded shared scratch, never per-viewer temperatures.

`InfraredBrickCodec` retains INVALID/UNIFORM/INDEXED/RAW and exits palette scanning
at 36 distinct values. Page-boundary parts stay below 960 KiB including the existing
2,048-byte header reserve. Packets carry FULL/FIRST/LAST/READABLE; READABLE describes
physical input, not permission to display. Each part carries the same current field
footprint; full requests omit their old field footprint. LAST alone installs both
masks, generation, readability, epoch and origin, then uploads the existing mirror.
The previous GPU image remains at its committed origin during staging. Field-only
full establishes a valid display baseline. All-Page deltas use the full upload;
partial deltas reuse one 8 KiB scratch. No background packet, background texture,
dormant ownership map or second material mirror exists.

`AFTER_LEVEL` directly blends the infrared result into the borrowed main color
attachment, preserving destination alpha and the original 0.43 tint/scan animation.
The RGBA8 intermediate, copy draw and all depth-based ownership heuristics are removed.
Depth reconstruction serves scan distance, sky rejection and the local display coordinate
of non-terrain occluders. Those pixels sample the existing 144³ display texture at their
own visible position, reusing analytic fields and the blue missing-value base. This is
an environment display approximation, not synchronized Air or entity body temperature.
Terrain still uses its exact vertex owner; no depth-based block-ownership guess is restored.

`BlockOwnerScope` carries the actual model block through existing mesh emission.
`OwnedChunkVertexType` delegates base encoding: compact stays at 20 bytes (two light
bytes plus a separate 16-bit owner); high precision preserves the original 28 bytes
and adds four bytes. Four production bridges supply the renderer factory, GPU arena
high-precision stride, custom attribute bindings and exception-safe model scope. The original static formats,
ordinary shader loader, meshing, culling, sorting and batch submission remain owned
by Embeddium. No per-block scope object, duplicate mesh or world scan is introduced.

`InfraredChunkRenderer` uses normal superclass program caching while IR is inactive;
capture programs are created lazily and cached per original pass/fog/type options.
SOLID/CUTOUT capture uses a vertex-stage integer fetch from the committed 144³ image
and a flat integer output. Main color and R16I share the same original fragment
discard and depth test. `InfraredChunkShaderInterface.setRegionOffset` inverts the
backend's actual quantized `CameraTransform` with
`round(offset + camera.frac) + camera.int - textureOrigin`; no large float world
coordinate or depth-derived block index is used.

`InfraredSurfaceTarget` owns one screen-size R16I image, a terrain depth snapshot and two FBOs. Capture borrows
main color/depth; final blending attaches color only to avoid depth feedback. Immediately
after the original CUTOUT draw, `captureTerrainDepth` copies the actual depth storage
at its original precision. Final blending compares the snapshot and main depth exactly:
later depth writers use their own environment display position, never the background
terrain heat image. Neither terrain nor entity geometry is submitted again.
An MRT depth-output replacement was tested and rejected for production: D24 sampling
does not exactly match raw fragment depth or the tested quantization conversions.
Even on D32F, where raw MRT matches, measured savings at one layer become a regression
at four overlapping layers; storage stays 6P. `verifyNativeDepth` preserves native-depth
equality coverage. Qualification and controlled timing data are in the IR plan.
The first active terrain pass clears only temperature to INVALID; subsequent passes
retain it, including when transparency sorting is disabled. An empty visited pass
also starts a fresh INVALID image, preventing previous-frame silhouettes. Shrink-to-zero
releases screen objects; reset releases owned resources, never borrowed attachments.
Final blending restores viewport, scissor, depth, blend, the previous GL program and
texture/framebuffer state. Restoring the program preserves `ShaderInstance.lastProgramId`:
the same vanilla shader can be applied again without leaving GL program 0 bound.
The temporary program and its restoration both use raw GL, leaving Oculus's separate
bind cache unchanged; a cached `GlStateManager._glUseProgram` can skip the restoration.
For D24/D32F the screen-image budget is 2P temperature + approximately 4P depth bytes,
so total 6P; the previous 2P implementation omitted entity occlusion. The 144³ image/mirror
is unchanged. The extra GPU copy occurs only during active IR, and screen objects are
released after shrink-to-zero. Environment tint adds at most one 3D lookup per occluding
fragment, one camera-to-window uniform, no server queries/wire fields/entity cache.
There is one extra integer terrain output and one final fullscreen draw, no extra terrain
draw or CPU work proportional to screen pixels/visible blocks in steady frames.
Owner encoding still occurs during ordinary mesh rebuilds while IR is off.

The strict production-encoder/GLSL test covers 8,640 scenes and 353,123,074 classified
pixels with zero ownership errors for both layouts and D24/D32F. Real backend integration,
performance evidence and remaining acceptance work are recorded in the
[exact surface capture plan](../../plans/2026-09-13_17-18-00_infrared-exact-surface-capture.md).
Without fields, unchanged material windows send no S2C and perform no display
natural queries. With fields, work and wire grow with their previous/current Page
area; stable fields are recomposed every poll, not promised zero traffic. This is
the original display-composition tradeoff without per-observer state. See
[world temperature](world-climate-and-temperature.md) for semantics and
[network lifecycle](data-lifecycle-and-integration.md#network-and-consumers)
for baseline and reset rules.
The underlying `WorldTemperature.biome` cache uses primitive `getOrDefault` with
an explicit missing value. A legitimate zero-degree biome contribution is cached
like other values and is refreshed by the existing `WorldTemperature.clear` path.
`WorldTemperature.dimension` returns the existing Level cache result directly;
the default config is read only by a cache-miss fallback or a non-Level reader.

Gameplay reads a Page's immutable current publication, resolves the local Air
point, reads the expected arena slot generation, and verifies that the same Page
publication is still current. During a bounded topology gap it may read the last
coherent Brick temperature without using stale point ownership. If no coherent
worker cut exists, it performs one O(1) lookup in the loaded chunk's dormant
section before falling back to natural temperature. A Brick whose current
publication still has no signature payload is treated as uncaptured and may use
its dormant checkpoint; a compiled Brick with no Air at the requested point
continues directly to natural fallback.

## Dormant Chunk Temperature

`DormantChunkThermalState` is a lazy `LevelChunk` attachment. Async
`ChunkDataEvent.Load` only validates and decodes primitive NBT. Main-thread
`ChunkEvent.Load` attaches the state without advancing it. There is no source-support
bit or unloaded-source heat exemption. Neither ordinary load nor serialization
integrates an offline solver or forces a thermal Page into residency.

Stored temperature uses signed `1/16 C` residuals from section-center
`WorldTemperature.naturalAir`. The Air payload stores a Brick mean weighted by represented
whole blocks, plus optional member-mask/residual entries when nodes differ after
quantization. All exact entries are retained only if packed residuals and masks
together fit the 640-byte section numeric budget; otherwise only means remain.
One-node or equal-residual Bricks need no exact entries. Restore fills 64 block
temperatures from the mean, overlays stored masks, and aggregates into the new
layout. `BrickMigrationKernel` restores this payload only into actual Air.
Checkpoint format 4 stores the Air capture's natural baseline, and exact material
H, branch, BlockState/law and sampling ticks in `MaterialSectionState`. Material
records do not use Air quantization, mean compression or the 640-byte Air budget.
Only v4 is read; older thermal checkpoints are not restored. World blocks are not
removed by this format change. Air history remains a spatial temperature approximation.

`DormantThermalCooling` is a pure, allocation-free projection into a caller-owned
sample. For a sensible state, `T=N+(T0-N)*exp(-lambda*dt)` with
`lambda=ln(2)/halfLifeSeconds`, `dt=max(0,t-t0)/20` seconds, and N the current
Section-center `WorldTemperature.naturalAir`. Air reconstructs T0 from its saved
natural baseline plus the 1/16 C residual; changing N no longer directly shifts T0.
Material follows `dH/dt=lambda*C*(N-T(H,branch))`, preserving its latent plateau.
The plateau has constant heat flux and zero flux when N equals its temperature.
Only a bounded number of analytic segments is evaluated, regardless of elapsed time.

This is a common dormant half-life, not geometry-dependent physical cooling:
ordinary Air and material lose the same fraction of temperature difference.
Current N approximates the whole unsolved interval; there is no weather history.
Server-stopped wall time is excluded. Current half-life settings reinterpret an
unsettled interval on profile/config reload. Project using the saved law before
adapting to changed law parameters. Reads do not rewrite H, tick, or chunk NBT.

Same-age material snapshots hold one long tick. Partial edits lazily allocate a
long per record so editing one block cannot reset another's age; merge carries each
record's tick and uses a scalar again for uniform results. The worst additional
timestamp payload is 8 bytes/record (32 KiB for 4096 records), plus the scalar,
array/object overhead and any simultaneously retained COW copies. No scheduler or
candidate bitset is added. `MutableMaterialSample` keeps explicit STORED provenance
because both live and stored samples now have valid timestamps.

Chunk-owned material editing uses `MaterialSectionState.Editor`: the first write
after sharing a snapshot forks data, subsequent writes reuse it, and deletions
compact when publishing a new snapshot. Palette reference counts are allocated
only for edited Sections. Metadata and scalar reads do not freeze the editor.
This avoids O(N) array copies for every mutation while preserving immutable worker,
checkpoint and serialization inputs. It is not zero-cost in all cases: the first
write after publication and deletion compaction still cost O(N). In the controlled
4096-record repeated-block-write test, 1024 measured edits after warm-up fell from
53,344 heap bytes/call to 0 and from 12.734 to 1.050 microseconds/call median;
this is a mutation fixture, not a whole-server tick benchmark.

Retirement captures one coherent `PagePublication`/`QueryPublication.sampleTick`
before clearing the handle. Save, unload, stop, recipe reload, and terminal
worker replacement reuse the same Page-local capture. Partial captures preserve
unrepresented material records and their clocks. Retained Air is merged at the
capture cut; repeated capture at the same sample tick reuses its natural baseline
to prevent repeated quantization drift. Saving otherwise serializes the anchor
without rebasing. `DormantMaterialCut` carries immutable records plus a main-thread
tick/N/rate cut; only newly restored nodes project it, while live migration retains H.

Original random tick and surface-freezing selections call the shared single-point
phase entry even without a worker. Native snow, lava and flowing-water transitions
have energy edges; disappearing recipe stages pay latent heat before removing the
record. Tracked snow treats its current state as one body and melts to Air, without
a new layer-mass model. Nonreciprocal recipes without a physical edge remain explicit
gameplay conversions; missing records retain original environmental gameplay.
Latent projection stops at the current state's target endpoint until a world update
is allowed. A successful dormant mutation passes its projected H and commit tick
through the existing scoped chunk hook; it does not manufacture a worker request/ACK.
The next state starts at commit time, without historical heat debt or a replay loop.
Unloaded/non-ticking chunks are not activated. Random completion has no maximum latency.
Recipe reload only recounts loaded Sections whose palettes contain changed random-tick eligibility.

`FHConfig.COMMON.THERMAL_RUNTIME.dormantTemperatureHalfLifeSeconds` defaults to
`1800`. Fallback caches one natural temperature per Section per aligned 20-tick
boundary; projection uses the actual query tick. A regular/collapsed Page uses packed rank
directly; only exact mixed data owns derived lookup arrays. Unloaded chunks own
no runtime heap. Infrared reads only the material portion of loaded dormant data;
Air residuals remain excluded. It creates no dormant ownership map or second
material store. Display revisions are transient Section/chunk change markers.
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
material keys, `Ks` changed state slots, and `A_admit` newly committed Page
lifecycles.

| Path | Bound |
|---|---|
| mutation capture | `O(Kp + 64*Kb)`; one world-state read per changed center, one encode per changed Brick and one Page build per ready cut |
| topology prepare | `O(Kb + Kf + Ko + Ke log Ke + Ks)`; no unrelated Page/arena/high-water scan |
| ordinary commit | `O(Kf + Ke + Ks)`; no allocation or sort, with one exact old-reference/ownership proof before release |
| Brick migration | new/no-dormant and regular-to-regular O(1); mixed ordinary migration at most 64 block mappings, using retained scratch |
| source event update | changed sources and affected bindings only; source-seed residency sync occurs only on zero/nonzero Brick reference transitions |
| source delivery | one ordered pass over active/touched nodes at target tick |
| Brick cold capture | all-Air O(1), otherwise exactly 64 BlockStates per new Brick; at most 64 new Bricks/tick |
| residency/frontier | hot bits piggyback on query live-slot publication with one contiguous `O(P_max)` hot-mask clear; additional Page/frontier work is `O(P_active + F_frontier + A_admit)` with changed absolute masks plus one required publication per admitted lifecycle; unavailable chunks are event-woken, and accepting a worker mask never echoes it back unchanged |
| static radiation mutation | changed profile/lava Bricks only; one section merge per cut |
| static radiation cold capture | at most 64 unknown Bricks/tick; 64 primary plus at most 208 lava-neighbor BlockState reads per Brick |
| static radiation query | one fused pass over at most eight sections; zero BlockState reads when coverage is known |
| static radiation trace | one uncached current-eye block DDA per selected emitter |
| radiation chunk lifecycle | fixed chunk sections plus known touching boundary Bricks only |
| query publish | one pass over live spans and live cells; unchanged sleep is `O(1)` |
| dormant capture | `O(64 + Page Air components)`, only at checkpoint |
| dormant query | Air O(1); material O(log N) lookup plus bounded energy segments, no snapshot/COW |
| dormant activation | attach decoded state; no cooling sweep or new Page |
| dormant random phase | existing sampled position only; reads leave anchors unchanged, actual mutations pay normal world/COW costs |
| player cadence | stable UUID phase offset over the 20 ticks |
| dropped reservoir query | one point; 64 same-tick quarter-block samples, 32 candidate visits, top 4, at most 4 rays, and a separate 64-receiver witness cache |

Capacity growth and fatal generation rebuilds are exceptional structural work.
They are measured separately from routine door/block/source/player workloads.

## Validation Standard

Production code contains no counters, traversal probes, test callbacks, debug
collections, or test-only constructors. Tests use deterministic outputs and
test-owned fixtures. Final performance evidence comes from external JVM JFR
and heap runs, not production bookkeeping.

The current worktree passes Java 17 `compileTestJava` and the real Forge
GameTestServer with all `16/16` required tests, including packed-ice phase
completion during same-Brick trapdoor topology churn and the Minecraft residency
handoff scenario. Numeric JUnit was intentionally not used as acceptance for
these gameplay/lifecycle fixes. Controlled
120-second door/block/source/player/crop JFR workloads and 10/30-minute
combined/churn heap runs remain performance evidence rather than undocumented
claims. Results and any remaining gap belong in the dated development diary.
