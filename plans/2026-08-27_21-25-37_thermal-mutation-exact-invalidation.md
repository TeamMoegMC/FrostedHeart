# Thermal Mutation Exact Invalidation And Adaptive Reuse Plan

- Time: `2026-08-27 21:25:37 +08:00`
- Authors: `Codex; OpenAI GPT-5; primary planning and architecture review agent`
- Status: `in-progress` (implementation and functional validation complete; controlled post-change JFR pending)
- Scope: `thermal geometry mutation intake, final-state coalescing, signature delta classification, Brick/material invalidation, topology commit, and optional repeated-structure reuse`
- Related: [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md), [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md), [`diary/2026-08-27_20-32-24_thermal-live-jfr.md`](../diary/2026-08-27_20-32-24_thermal-live-jfr.md), `run/thermal-live-20260827-204347.jfr`, `MinecraftThermalInput`, `ThermalPage`, `MinecraftThermalTopologyApplier`

## Purpose And Authority

This plan designs the next implementation batch for high-frequency reversible block-state changes. The motivating workload is repeatedly opening and closing one room door, but the architecture must also cover fence gates, trapdoors, fences, fluids, multipart blocks, modded dynamic shapes, and any resolver whose final thermal signature depends on nearby loaded state.

The plan is intended work, not evidence of implemented behavior. Source remains authoritative until the plan is completed and its `Outcome` is filled. No Java implementation is changed by this document.

## Goal

Make geometry mutation cost proportional to the number of distinct final-state positions and actual changed contacts, rather than the number of raw toggles multiplied by all candidate centers, whole Bricks, and six neighboring Bricks.

The target behavior is:

1. Repeated changes to one position retain one exact packed position and resolve only its final state.
2. All routine geometry mutations in one dimension use one fixed 20-tick batch cut. There is no separate quiet window, hard deadline, per-owner timer, or timing wheel.
3. Returning to the already-applied topology produces an acknowledged `TOPOLOGY_UNCHANGED` epoch without rebuilding topology or material fragments.
4. A real change rebuilds only affected Bricks, material owner blocks, boundary directions, and arena bindings.
5. Common door-like activity allocates tens of bytes of primitive storage per pending section, not a permanent 4096-bit Page bitmap and not a `BlockPos` object graph.
6. The implementation removes redundant Page/ring revision state and whole-Page staging clones before considering any cache.
7. Repeated structural variants may be investigated in a separate follow-up only after exact invalidation is measured and proven insufficient.

## Non-Goals

- Do not introduce `DoorBlock`, fence-gate, trapdoor, or block-class branches in the thermal runtime.
- Do not introduce `DynamicThermalAperture` as the correctness boundary. Blocks remain inputs to the resolver/signature contract.
- Do not make the thermal runtime asynchronous and do not reactivate `TemperatureThreadingPool` or an `Executor`-owned thermal worker.
- Do not change solver formulas, material coefficients, FarField formulas, solve cadence outside mutation urgency, or gameplay temperature composition.
- Do not make a fixed microcell lattice permanent for every admitted Page.
- Do not weaken loaded-only capture, lifecycle generation checks, transactional topology commit, source settlement, phase ACK authority, deterministic traversal, or natural fallback.
- Do not optimize radiation, sky-column refresh, physical-source indexing, publication high-water copying, or accumulator traversal in this batch unless a direct mutation contract requires a small integration change.

## Verified Current State

The following was checked against the working tree and the controlled 120-second repeated-door JFR on `2026-08-27`.

### Runtime behavior

- `MinecraftThermalInput.recordMutation` handles main-thread block events immediately.
- Main-thread geometry work calls `recordGeometryMutation`, which examines a loaded `3 x 3 x 3` center cube and resolves every center whose current `DispatchPlan.dependencyMask()` includes the changed position.
- Thread-external events use `SectionOwner.deferredBlockMutations`, currently a lazily allocated dense `long[DEFERRED_BLOCK_WORD_COUNT + 1]`, then perform the same center expansion when drained on the main thread.
- `scheduleGeometryRebuild` keeps the earliest `effectiveTick + GEOMETRY_REBUILD_DELAY_TICKS` through `Math::min`. This is a fixed leading deadline, not a trailing quiet-window debounce.
- `ThermalPage.recordGeometryMutation` immediately advances geometry revision and coalesces at Brick granularity.
- `MinecraftThermalTopologyApplier.cancelUnchangedBrickMutations` compares every one of the 64 signatures in each desired Brick with the applied Brick.
- A rebuilt Brick calls `markMaterialNeighbors`, which unconditionally marks all six neighboring Bricks material-dirty.
- `rebuildDirtyMaterials` scans all 64 blocks in every material-dirty Brick and calls `compileMaterialBoundaries`; its hot lookup path includes `airMicrocellIfPresent` and `ConservativeAirGeometry.Resolution.componentAt`.
- Topology fragment replacement already has preflight/commit separation. Old arena spans are released only after an accepted replacement or retirement commit.

### JFR evidence

Recording: `run/thermal-live-20260827-204347.jfr`, 120 seconds, with the player repeatedly toggling a room door.

| Observation | Result | Meaning |
|---|---:|---|
| Average server tick, full recording | `12.48 ms` | Startup/warm work materially affects the full mean. |
| Average after first 10 seconds | `7.71 ms` | Steady workload is below the 50 ms budget but expensive for one local interaction. |
| P95 after first 10 seconds | `12.58 ms` | Repeated mutation creates visible recurring topology work. |
| Maximum after first 10 seconds | `20.87 ms` | No single tick exceeded budget, but local mutation owns too much headroom. |
| Thermal share of Server-thread CPU samples | `45.84%` | Thermal work is a dominant CPU consumer in this artificial interaction workload. |
| Thermal execution samples | `551` | `532` are in the topology applier; only `3` are solver samples. |
| `airMicrocellIfPresent`-related samples | `437` | `423` come from `rebuildDirtyMaterials`; `14` come from `compileBrick`. |
| Server-thread allocation rate | `57.24 MiB/s` | This is short-lived allocation, not proof of thermal retained memory. |
| After-GC heap | `1.64-1.79 GiB` | Whole-game/modpack retained heap; thermal old-object samples were `0/72`. |
| GC pauses | `18`, total `233 ms`, max `21.55 ms` | Allocation reduction is useful, but there is no evidence of a thermal retained-memory leak. |

The primary measured problem is repeated material/topology compilation and temporary allocation. It is not solver cost and not an active background thread.

## Decision Summary

Implement the optimization in this order:

1. Record exact packed local mutation positions and coalesce to final state.
2. Use one dimension-level fixed 20-tick mutation batch. Reuse the existing deadline owner and delete the proposed quiet/hard dual clock and timing wheel.
3. Keep exact packed positions exact through final-state resolution and the applier; do not convert sparse work into another `long[K]` staging representation.
4. Delete the redundant `GeometryDeltaCoalescer`/`GeometryDeltaRing` transport and the unused duplicate per-Brick revision array.
5. Replace the 4096-entry desired-signature clone with a sparse desired-signature overlay; retain a full array only for admission/full resync.
6. Classify exact signature impact using registry-cut lookup tables instead of repeated deep geometry equality, face aggregation, and component-list scans.
7. Separate material contact structure dirtiness from arena binding dirtiness.
8. Propagate only through boundary directions actually affected by changed block faces or component bindings.
9. Stage and commit only changed Brick/Page/material entries rather than cloning every 64-entry Page array.
10. Preserve the current topology transaction and recovery lifecycle.
11. Reprofile before designing a structural-template cache; no cache is implemented in this batch.

Correctness depends only on exact final-state input, generation checks, preflight, and accepted commit. Cache state is outside the selected implementation.

## Target Lifecycle

```text
Minecraft block-state event
        |
        |-- event-time physical-source handling (unchanged authority)
        |-- sky/radiation invalidation (existing subsystem contracts)
        `-- geometry-relevant mutation
                  |
                  v
       SectionOwner.PendingGeometryMutations
       section key + lifecycle generation
       exact 12-bit local positions, final-state only
                  |
                  |  first mutation opens one dimension batch
                  |  later mutations only join that batch
                  |  due = batchStartTick + 20
                  v
       main-thread batch drain in section/local-index order
                  |
                  |-- capture loaded final state; never load chunks
                  |-- expand resolver dependency masks
                  |-- deduplicate affected center positions
                  `-- resolve final signature once per center
                                  |
                                  v
                       compare with applied signatures
                          |              |
                    all equal       exact deltas
                          |              |
                          v              v
                acknowledge current   stage Brick/component/
                geometry revision     material patches
                TOPOLOGY_UNCHANGED          |
                                             v
                                  full topology preflight
                                             |
                          failure -----------+----------- success
                             |                               |
                             v                               v
                    retain old authority           atomic fragment/Page commit
                    request latest/full resync     migrate state/enthalpy
                    natural query fallback         release replaced old spans
                                                    exact source rebind notice
```

## Core Data Design

### 1. Exact packed positions are the normal representation

A Page/section contains `16 x 16 x 16 = 4096` block positions. A local block index therefore requires only 12 bits:

```text
localIndex = localX | (localZ << 4) | (localY << 8)   // 0..4095
```

Store it as an unsigned `short` inside one `SectionOwner`-owned accumulator. Do not store `BlockPos` objects and do not create `HashSet<BlockPos>` or boxed-integer collections.

Proposed `PendingGeometryMutations` state:

```text
long lifecycleGeneration
long pendingRevision
short sparseSize
short[] sparseLocalIndices       // starts at 8 entries
long[] denseLocalPositionBits    // null unless promoted
```

Rules:

- The accumulator is attached to the existing `SectionOwner`; it does not require a map node per changed position.
- Linear duplicate detection is intentional while `sparseSize <= 128`. Door-like workloads normally contain one or two positions and repeated positions hit at the front. Even 128 unique positions require at most 8,128 primitive comparisons while the mutations themselves are much more expensive; this avoids prematurely paying for dense storage.
- Sort unsigned local indices only when draining. Event order is not topology authority; final world state and canonical spatial order are.
- Grow through `short[8]`, `short[16]`, `short[32]`, `short[64]`, and `short[128]`, then promote to a 4096-bit dense representation on the 129th distinct position. Dense mode is for explosions, tree growth, machine multiblock updates, and other genuinely broad changes.
- Reuse the existing thread-external owner handoff set. Thread-external callbacks may add packed positions under the owner lock, but only the server main thread may read Minecraft state, schedule deadlines, seal input, or mutate topology.
- Main-thread owners are deduplicated in one dimension-owned primitive/reference list plus an owner-listed flag, not a new `HashSet` or map. The existing concurrent owner set remains only the thread-external handoff.
- Reset and retain at most the small sparse array while the section remains active so a repeatedly used door does not allocate once per second. Drop dense storage after the accepted batch and release all storage on Page retirement.

Approximate HotSpot 64-bit sizes with compressed ordinary object pointers, excluding the already-existing owner and active-owner set entry:

| Representation | Primitive payload | Approximate array object | Intended case |
|---|---:|---:|---|
| `short[8]` | `16 B` | `32 B` | One door, double door, gate, trapdoor. |
| `short[16]` | `32 B` | `48 B` | Small redstone/multipart burst. |
| `short[32]` | `64 B` | `80 B` | Moderate unique-position burst before dense promotion. |
| `short[64]` | `128 B` | `144 B` | Larger sparse burst. |
| `short[128]` | `256 B` | `272 B` | Last sparse tier before dense promotion. |
| `long[64]` | `512 B` | `528 B` | Dense fallback for many positions in one Page. |
| `BlockPos` hash set | object-dependent | substantially larger | Rejected because every key/node adds object and pointer overhead. |

The 528-byte form is a temporary worst-case representation for a pending dirty section. It must never be allocated permanently for every active Page.

### 2. Exact positions remain exact through the applier

After dependency expansion, a changed center maps to one Page, one base Brick, and one of 64 block positions inside that Brick.

Do not convert these positions to the previously proposed `dirtyBrickMask + long[K] changedBlocksByDirtyBrick`. That representation creates another array and another ownership transition without adding information. Carry exact center indices through `ResolvedGeometryInputRing`, then retain a sparse desired-signature overlay in `PageState`:

```text
long dirtyBrickMask                  // derived summary for Brick iteration
short[] desiredCenterIndices         // sorted at preflight
int[] desiredCenterSignatureIds      // parallel final IDs/status encoding
byte[] desiredCenterDeltaFlags       // optional parallel primitive flags
int desiredCenterCount
```

Rules:

- Exact center indices are the authority for sparse mutation work; `dirtyBrickMask` is only a derived iteration summary.
- Multiple ring entries for one center overwrite the overlay entry with the greatest accepted geometry revision.
- Normal mutation must not call `ensureDesiredSignatureIds` and clone `appliedSignatureIds[4096]`.
- Compilation of one dirty Brick copies only its 64 applied IDs into one reusable compiler scratch and overlays changed centers belonging to that Brick.
- Accepted commit writes only overlay positions into `appliedSignatureIds`; failed staging leaves the applied array untouched.
- Sparse overlay is not mandatory for dense work. If a batch reaches at least 1,024 changed centers or 32 dirty Bricks, create one full desired `int[4096]` cut from applied state plus the exact overlay. This is a representation promotion, not a full topology resync; `dirtyBrickMask` still limits compilation.
- Admission/full resync also owns a complete 4096-entry array because all positions are authoritative in that path.
- `offerFullResync` transfers ownership of its newly captured `int[4096]` into the ring instead of cloning it a second time. The producer must not mutate the transferred array.

Approximate normal dirty-Page staging with eight exact centers is two arrays totaling about `80 B` (`short[8]` about `32 B`, `int[8]` about `48 B`), plus fields in the existing `PageState`. The current `appliedSignatureIds.clone()` is about `16,400 B` for the same one-position change. Dense promotion/admission/full resync keeps the larger representation only when the work is genuinely broad.

### 3. Remove duplicate delta transport and revision storage

After exact centers carry revision, Page identity, block index, status, and final signature, the Brick-only `GeometryDeltaRing` carries no independent correctness evidence. Delete:

- `GeometryDeltaCoalescer` and its per-Page `long[64] latestBrickRevisions`;
- `GeometryDeltaRing` and its dimension-level parallel arrays/watermark path;
- `ThermalPage.latestBrickMutationRevisions`, which is currently written and cleared but never read;
- `GeometryDeltaCoalescer` seal/overflow tests that validate only the removed duplicate transport.

Replace them with one Page method that advances the monotonic pending geometry revision and invalidates publication, plus exact resolved-center/full-resync entries in `ResolvedGeometryInputRing`. The ring should carry the stable `ThermalPage` reference and validate it through `pagesByPage` identity plus lifecycle generation, allowing the applier to avoid allocating a new `PageIdentity` record for every resolved center.

This removes approximately two `long[64]` array objects, about `1,056 B` including typical array headers, from every active `ThermalPage`, plus the Page-local coalescer object and the dimension-level Brick ring.

### 4. Signature delta is primitive hot-path state

`ThermalSignatureDelta` is a semantic helper, not a required allocation per block. Its implementation should compare registry-cut primitive metadata for old and new signature IDs and write flags/masks into the Page/Brick staging state.

The delta must distinguish:

| Delta | Evidence | Required action |
|---|---|---|
| `NOOP` | `sameTopologySignature(old, new)` | Copy/acknowledge final signature and revision; no topology/material rebuild. |
| `RESOLUTION_STATUS` | resolved/unresolved status changes | Existing loaded-only unresolved/full-resync route. |
| `AIR_OCCUPANCY` | XOR of the 64 microcell air masks is nonzero | Recompile the owning Brick; mark only touched Brick faces. |
| `AIR_COMPONENT_PARTITION` | component membership/connectivity differs even if union air mask is equal | Recompile the owning Brick and rebind descriptors that target replaced components. |
| `FACE_PATCH` | any of six combined 4 x 4 face masks differs | Propagate only through those actual directions when the block lies on the corresponding Brick boundary. |
| `MATERIAL_STRUCTURE` | material profile/contact pattern or material-facing air patch differs | Patch contact structure for the exact owner block and affected neighbor material blocks. |
| `BINDING_ONLY` | contact structure is equal but referenced arena component generation/slot changes | Re-resolve endpoints and rebuild bound fragments without scanning 64 block signatures. |

Comparing only union air masks is insufficient: two signatures can contain the same open microcells with different connectivity. Comparing only signature IDs is also insufficient because distinct IDs may map to equal `SignatureGeometry`.

Compile one immutable `CompiledThermalSignatureView` when the signature registry cut is installed, and share that view across all dimension inputs/appliers using the cut:

```text
int[] topologyClassBySignatureId
long[] airMaskBySignatureId
short[] faceMaskBySignatureAndDirection       // 6 * signatureCount
byte[] componentOrdinalBySignatureMicrocell   // 64 * signatureCount; 0xFF = no air
```

- Intern equal `SignatureGeometry` values to one topology class ID during construction, then discard the temporary interning map. `sameTopologySignature` becomes one integer comparison.
- Precompute the six combined face masks once rather than iterating component lists in mutation/material paths.
- Store component ordinal, not component ID, in the flat byte table. At lookup, use the ordinal to read the existing component list and obtain its exact ID; up to 64 components fit with `0xFF` reserved for no air.
- Move the existing converted `SignatureGeometry[]` and component lists out of each dimension applier into this shared view. The lookup data must replace per-dimension conversion, not sit beside one copy per dimension.
- The flat tables cost about `88 * signatureCount` primitive bytes plus four array headers. At 1,000 signatures this is about 86 KiB once per shared registry cut, independent of dimension and Page count. Record actual gameplay signature count and reject optional table fields if their measured retained cost exceeds the CPU they remove.
- These tables replace repeated derived work; do not create one lookup array per Page or per block.

### 5. Separate material structure from live binding

The current material rebuild path combines two jobs:

1. Discover whether a material/air contact, phase reservoir, or stateless bridge exists and what its stable geometry/profile descriptor is.
2. Resolve that descriptor to current arena slots/generations and build live sweep fragments.

These jobs need different invalidation.

`materialSurfaceDirty` means the stable descriptor set may have changed. Re-evaluate only:

- changed material owner blocks;
- material blocks immediately adjacent to changed air geometry;
- exact face patches named by the signature delta;
- cross-Page neighbors reached through those faces.

`materialBindingDirty` means descriptor structure is still valid but one of its target air/material cells was replaced or migrated. Reuse descriptors and resolve only their live endpoints. Do not call `collectMaterialCandidates` for all 64 blocks and do not call `airMicrocellIfPresent` to rediscover an unchanged contact.

Each Brick keeps descriptors in the existing canonical spatial order. Exact block patching removes descriptors owned by the affected block indices, recompiles those indices, and merges the result with untouched descriptors. A permanent per-block object/list table is prohibited; use sorted primitive owner indices or ranges inside the Brick fragment.

### 6. Exact boundary propagation

Do not call an unconditional six-neighbor `markMaterialNeighbors` after every Brick replacement.

For each changed block, derive a six-bit direction mask from the actual signature delta. A neighbor Brick is affected only when both conditions hold:

1. The changed contact/air/component evidence includes that direction.
2. The block is on the corresponding boundary of its `4 x 4 x 4` Brick.

Examples:

- An interior changed block dirties no neighboring Brick.
- A block on one Brick face may dirty one neighboring Brick.
- An edge block may dirty two.
- A corner block may dirty three.
- Crossing local Page coordinate `0` or `15` uses `installedActiveBySection` to find the exact adjacent Page in O(1).

Air pair ownership remains canonical. If pair fragments are owned only on negative directions, preserve that owner rule while marking the exact owner Brick. Material contacts may require either side, but only actual changed face directions propagate.

When a component allocation changes without a contact-structure change, propagate `materialBindingDirty`, not `materialSurfaceDirty`, to descriptors that reference the replaced component. Rebinding still validates arena generation before commit.

### 7. Sparse transactional staging and reusable compiler scratch

Current local rebuilds allocate far more than the changed data:

- `ensureDesiredSignatureIds` clones about 16 KiB per dirty Page.
- `rebuildPage` clones or creates roughly 3.5 KiB of 64-entry Page arrays before element payloads, even for one dirty Brick.
- `rebuildDirtyMaterials` separately clones three 64-entry fragment-reference arrays, about 816 bytes per dirty Page.
- Each dirty Brick creates a Page-shaped `PageBuild` containing five 64-entry arrays, multiple lists, and a map; the fixed arrays alone are about 1.3 KiB.
- `PageIdentity` is allocated for each resolved ring item during lookup.

Replace that staging shape with:

```text
DesiredSignatureOverlay          // exact center index -> final signature
BrickReplacement[K]             // only actual dirty Bricks, canonical order
MaterialReplacement[M]          // only changed structure/binding fragments
BrickCompileScratch              // one applier-owned reusable 64-block workspace
```

Requirements:

- Split the single-Brick compiler result from full-Page build state. A `BrickBuild` uses scalar `baseIndex`, summary, mixed geometry, and only its actual cell/material lists; it must not allocate arrays indexed by all 64 Page Bricks.
- Keep full-Page arrays only for admission/full resync, where all 64 entries are truly required.
- Change `ThermalPage` local-install APIs to accept sorted sparse Brick replacements rather than forcing callers to clone full coverage/summary arrays. The Page applies these assignments only after the complete topology/sweep preflight has succeeded.
- Stage old/new references only for the K changed entries. PageState array assignments are the final non-throwing commit step; unexpected staging failure still leaves old arrays authoritative.
- Reuse one bounded `BrickCompileScratch` because the applier is synchronized and server-main-thread-owned. Scratch may contain the 64 local signature/geometry references and primitive candidate arrays, but it must be reset between Bricks and never stored in committed state.
- Use the stable `ThermalPage` reference carried by the resolved-input ring and `pagesByPage` identity lookup, then validate lifecycle generation. Do not allocate `PageIdentity` per center.
- Reuse one `ResolvedGeometryInputRing.MutableInput` holder as an applier field instead of allocating it per apply.
- Do not pool committed lists/maps or arena replacements across Pages. Only allocation-free scratch is reused; accepted immutable fragments retain normal ownership.

This is still a transaction: sparse staging reduces copied state, but preflight must validate the entire affected operation set before any authoritative assignment.

## Expected Memory And Allocation Effect

The plan distinguishes retained memory from per-batch allocation:

| State/work | Current approximate cost | Planned cost |
|---|---:|---:|
| Duplicate Page Brick revision/coalescer arrays | two `long[64]`, about `1,056 B/Page`, plus coalescer object | removed |
| Gameplay `GeometryDeltaRing(16,384)` | four `long[]` plus one `int[]`, about `576 KiB/dimension` primitive payload | removed |
| Resolved-ring Page identity | two `long[16,384]`, about `256 KiB/dimension`, plus `PageIdentity` allocation per center | one Page-reference array, about 64 KiB with compressed references or 128 KiB without; identity lookup allocates nothing |
| Common pending raw mutation | thread-external `long[65]` is about `536 B`; main path resolves immediately | `short[8]` about `32 B` only on a pending/previously reused active section |
| Eight desired center changes | full `int[4096]` clone about `16,400 B` | `short[8] + int[8]` about `80 B` |
| Dense desired center changes | full clone regardless of density | sparse until 1,024 centers/32 Bricks, then one full desired array |
| Local Page transaction arrays | roughly `3.5 KiB` of Page-wide arrays before payload | K sparse Brick replacements |
| Dirty-material Page arrays | three cloned 64-reference arrays, about `816 B` | M sparse material replacements |
| Single-Brick build fixed arrays | about `1.3 KiB` before lists/map/output | scalar `BrickBuild` plus one reused scratch |
| Signature conversion/lookups | converted geometry duplicated in every dimension; component/face queries scan lists | one shared registry-cut view, about `88 * signatureCount` lookup bytes replacing per-dimension conversion |
| Structural template cache | none currently | none added by this implementation |

The existing authoritative `appliedSignatureIds[4096]`, about 16 KiB per active Page, remains in this batch. It is a real retained-memory candidate, but compressing it changes every lookup and is deferred until active Page/signature counts prove that the retained saving outweighs decode cost.

## Mutation And Coalescing Algorithm

### Event intake

1. Validate `SectionOwner.valid`, owner identity, and lifecycle generation exactly as today.
2. Preserve physical-source event-time semantics. On the main thread, `onBlockMutation(oldState, newState)` still settles/rebinds sources at the authoritative tick. Thread-external source resync continues to read final loaded state on main-thread drain.
3. Preserve sky-column and radiation subsystem invalidation contracts, but allow their existing position/section coalescing to remain independent.
4. Apply the current `staticMutationSemanticsUnchanged(oldState, newState)` fast rejection when its gameplay registry/dispatcher identity conditions hold.
5. Pack the raw local position into the owner accumulator. If it is already present, do not add another entry and do not change the batch deadline.
6. On the first insertion of a new packed position, invalidate publication for candidate admitted Pages in the one-block dependency halo without resolving signatures. Deduplicate this notification with the pending position.
7. Do not call `recordGeometryMutation` or resolve the `3 x 3 x 3` cube from the event callback.

### Unified 20-tick batch semantics

Use one constant and the existing dimension-level deadline owner:

```text
MUTATION_BATCH_TICKS = 20

if no routine geometry batch is pending:
    geometryBatchDeadlineTick = effectiveTick + MUTATION_BATCH_TICKS
else:
    geometryBatchDeadlineTick is unchanged
```

- One click is reflected in thermal topology no later than 20 ticks after that batch starts.
- All additional routine geometry mutations observed before the cut join the same final-state batch and do not move its deadline.
- Continuous activity produces at most one routine geometry cut per 20 ticks instead of one cut per 5 ticks.
- After an accepted/unchanged cut, mutations not included in that sealed revision open the next 20-tick batch.
- Reuse `geometryRebuildDeadlineTick` with a single compare-and-set/min-on-first-pending contract. Do not add a timing wheel, priority queue, per-owner deadline, first/last tick, or stale timer token.
- Maintain one main-thread pending-owner list with owner-listed deduplication. Sort due owners by section key at the global cut so insertion order cannot affect topology order.
- Full resync required by admission, reload, lifecycle replacement, in-flight recovery, or explicit corruption evidence remains governed by its existing urgent lifecycle. The 20-tick batch applies to routine coalescible block geometry mutations, not recovery authority.

### Final-state resolution

For each owner captured by the dimension's 20-tick cut, on the server main thread:

1. Revalidate owner identity, lifecycle generation, Page admission, and loaded section authority.
2. Sort sparse raw positions by unsigned local index; dense mode iterates set bits in ascending order.
3. For each distinct raw position, inspect candidate centers in the existing deterministic `dy/dz/dx` halo order.
4. Read the current center block/fluid state from loaded-only snapshot data.
5. Obtain the current resolver dispatch plan and keep the center only when its conservative dependency mask contains the raw mutation offset.
6. Deduplicate candidate centers before signature resolution. Adjacent raw mutations must not resolve the same center twice.
7. Resolve each final center signature once and offer it through the existing bounded input/ring lifecycle with the latest Page geometry revision.
8. Unsupported, unloaded, missing, out-of-order, or ring-capacity cases use the existing full-resync requirement and natural fallback. They must not partially publish a guessed signature.

Using only the raw position itself is incorrect for fences, connected shapes, fluids, and dependency-bearing modded resolvers. Exact raw positions reduce repeated work; resolver dependency expansion preserves general correctness.

## Page Revision And Publication Contract

Coalescing must not leave stale geometry queryable during the 20-tick batch window.

- The first pending relevant mutation in a batch immediately advances one monotonic input geometry revision for each admitted Page in its dependency halo, or records an equivalent pending revision that publication validation observes.
- Existing publication for those Pages becomes stale immediately and gameplay uses the current natural/analytic fallback contract until a matching topology/publication revision is available.
- Repeated mutations and additional positions in the same Page/batch share that Page batch revision. Geometry revision is a state cut, not an event counter; source events keep their separate event-time authority.
- If final signatures equal applied signatures, `tryAcknowledgeUnchangedBricks` (or its exact-center successor) advances the Page's acknowledged geometry revision without replacing fragments.
- Scheduler completion for this path is `TOPOLOGY_UNCHANGED`, not a fake `APPLIED` result and not a retry.
- A later frame cannot publish against an older geometry revision, lifecycle generation, or topology generation.

## Topology Transaction And Failure Behavior

Exact invalidation changes staging inputs, not commit authority.

Before mutation of authoritative Page/arena/sweep state, preflight must validate:

- frame and Page lifecycle generations;
- desired geometry revision and input watermarks;
- all resolved signature IDs and unresolved/full-resync requirements;
- replacement span capacity and current arena generations;
- exact air-pair, material descriptor, binding, phase, and FarField fragment endpoints;
- fragment patch base version and deterministic operation capacity;
- phase ACK identity when phase state participates;
- source binding sections that will be notified after commit.

Commit order remains:

```text
stage signatures/deltas/descriptors/replacement spans
    -> complete preflight
    -> commit Page/component/fragment replacement
    -> migrate enthalpy/state under existing ledger
    -> install canonical traversal
    -> release replaced old spans
    -> publish committed source-binding section notifications
```

If preflight or apply cannot proceed:

- Leave old Page, arena spans, material descriptors, sweep fragments, and source bindings authoritative.
- Retain or reconstruct the exact pending mutation set from the latest authoritative frame; do not lose positions merely because staging consumed a scratch buffer.
- Use `LATEST_FRAME_REQUIRED`, `FULL_RESYNC_SNAPSHOT_REQUIRED`, in-flight recovery, or the existing natural fallback according to the actual failure.
- Reaching the 20-tick batch cut does not force a full Page rebuild. It only forces final-state resolution; unchanged work is still acknowledged as unchanged.
- Repeated failure must not allocate a fresh dense bitmap or template on every retry.

## Admission, Retirement, Reload, And Close

### Page admission

- A newly admitted Page starts with no pending mutation accumulator state and no template authority.
- Mutations that occurred before the loaded-only admission snapshot are represented by that snapshot's final state, not replayed as old events.
- Admission/full snapshot remains the fallback when exact deltas cannot be proven against an applied generation.

### Page or section replacement

- `SectionOwner.lifecycleGeneration` is part of pending mutation identity.
- Replacing a section invalidates and clears the old owner's sparse/dense pending positions, pending-owner-list flag, and desired-signature overlay.
- A stale owner-list or resolved-ring reference is ignored by Page identity and lifecycle-generation validation.
- No pending position may migrate silently to a reused Page slot.

### Retirement and chunk unload

- Retirement is idempotent.
- Remove the owner from pending handoff/list ownership and release dense fallback/overlay storage.
- Accepted retirement commit releases old arena spans only after fragments and source bindings no longer reference them.
- A late mutation or deferred callback for the retired owner is ignored by identity/generation checks.

### Profile/signature reload

- Increment the existing profile/signature revision cut.
- Pending positions may be retained only as raw coordinates; all resolved overlays, lookup tables, and descriptors compiled under the old revision are invalid.
- Require the existing full authoritative capture/rebuild where registry IDs or material semantics can change.
- Rebuild the one shared `CompiledThermalSignatureView` from the new immutable registry cut before admitting new dimension inputs.

### Runtime close

- Close resets the single geometry batch deadline and invalidates pending owner handoffs/list flags before Page/arena teardown.
- Repeated close/unload calls remain harmless.
- No worker, timer thread, or `TemperatureThreadingPool` lifecycle is introduced.

## Deferred Adaptive Structural Template Cache

This cache is explicitly outside the selected implementation. Exact coalescing, removal of duplicate transport/clones, lookup tables, sparse staging, and structure/binding separation must land and be profiled first. If a repeated 120-second JFR still attributes material CPU to compiling already-seen structures, write a separate plan using the constraints below.

### Admission policy

A Brick becomes cache-eligible only when all are true:

- it has at least 16 structural rebuilds in a rolling 400-tick observation window;
- at least one exact structural signature repeats;
- it is fully resolved and belongs to a current admitted Page generation;
- its compiled structure is below the per-entry size limit;
- the dimension cache remains below its byte budget.

Initial limits:

```text
maximum variants per hot Brick = 2
maximum dimension cache budget = 1 MiB
maximum single template = 8 KiB
```

These are future experimental upper bounds, not preallocation sizes. This implementation allocates no cache table, counters, key, or template.

### Key and value

The exact key contains:

- 64 local final signature IDs in canonical Brick order, or an exact compact equivalent;
- exact neighbor-face signature dependencies required by cross-Brick contacts;
- signature registry revision;
- material/profile revision;
- compiler schema revision.

A hash may select a candidate entry, but reuse requires exact key-content equality. Hash equality alone is never authoritative.

The cached value may contain only immutable structural output:

- air/component geometry structure;
- stable pair/contact/phase descriptor structure;
- canonical local traversal metadata.

It must not contain:

- arena slots or slot generations;
- temperature, enthalpy, heat capacity, source energy, or phase ACK state;
- Page slot, lifecycle generation, topology generation, publication epoch, or source binding;
- live `ThermalSweepFragments.Patch` instances.

On a hit, the runtime still allocates/resolves current arena spans, migrates state through the existing ledger, binds current endpoints, preflights, and commits transactionally.

### Eviction and invalidation

- Use deterministic two-entry recency per hot Brick or a byte-accounted dimension LRU whose eviction never affects numerical traversal order.
- Evict all Page variants on retirement/replacement.
- Invalidate all incompatible entries on signature/material/profile/compiler revision.
- Memory refusal or cache corruption evidence is a cache miss followed by normal compilation, not topology failure.
- If the cache does not reduce measured CPU after accounting for key comparison and binding, remove it. Exact invalidation remains the final architecture.

## Rejected Alternatives

### Block-class special cases

Special-casing doors, gates, and trapdoors would miss connected fences, fluids, modded shapes, and future resolver types. It would also duplicate the signature resolver's authority. Rejected.

### Always-allocated Page bitmap

A permanent `long[64]` per active Page is unnecessary for sparse interaction. The plan uses packed positions and compact dirty-Brick masks, promoting to a dense bitmap only for truly dense pending work. Rejected as the default.

### `HashSet<BlockPos>` or boxed packed positions

Object keys and hash nodes cost more memory and create more allocation than a small primitive array. Rejected.

### Only rebuild the exact raw position

Neighbor-dependent signatures would become stale. Exact raw positions are input evidence; the resolver dependency mask determines affected centers. Rejected as incorrect.

### Moving trailing deadline or per-owner scheduling

A quiet-window plus hard-cap pair needs first/last ticks, per-owner deadlines, and either a timing wheel or heap. The user-selected fixed 20-tick dimension batch bounds delay and repeated work with the existing deadline owner, so the additional scheduler state is accidental complexity. Rejected.

### Cache as correctness authority

Cached live slots, Page generations, or hash-only keys can bind stale cells and violate lifecycle isolation. Rejected.

### Permanent fixed microcell graph

One Page has 4096 blocks and up to 64 microcells per block, or 262,144 possible microcells before operation/state overhead. Keeping this dense graph permanently would trade a local mutation spike for large retained memory and solver traversal. Rejected.

### Background thermal worker

The measured hotspot runs on the server main thread and is caused by excess work, not lack of a thread. Moving Minecraft capture/topology mutation off-thread would violate ownership and add synchronization/lifecycle failure modes. Rejected.

## Determinism And Numerical Constraints

- Raw positions drain by unsigned local index after section keys are sorted.
- Dependency-expanded centers drain in canonical Page/block index order, regardless of event or pending-owner insertion order.
- Bricks drain by ascending base Brick index.
- Material descriptors merge by the existing stable spatial rank/key order.
- Air pairs retain their canonical ownership direction.
- Forward and reverse sweep traversal order must remain exact reversals of the same canonical operation set.
- Any future cache hit must emit byte-for-byte equivalent structural ordering to normal compilation.
- Enthalpy migration uses the current `GeometryMigrationLedger` rules and compensated accumulation order.
- No optimization may replace exact equality/preflight with approximate floating-point or hash equality.

## Implementation Stages

Implementation should finish the complete selected batch before the first compile/test run. Do not edit one method, compile, then continue method-by-method. Review source references first, write all production and test changes, then run the planned validation once and fix concrete failures.

### [x] Stage 0: Freeze baseline and invariants

- Preserve the JFR and record its exact command/workload notes.
- Record current production/test references for mutation deadlines, deferred block masks, geometry delta rings, Page revisions, material dirty masks, descriptor fragments, and neighbor propagation.
- Record current thermal JUnit and Forge GameTest baseline counts.
- Confirm no persistence/network/config contract serializes the internal dirty representation.
- Add no production diagnostics object solely for tests.

### [x] Stage 1: Exact final-state mutation intake

Primary owner: `MinecraftThermalInput` and its existing `SectionOwner`.

- Replace immediate main-thread `recordGeometryMutation` expansion with the adaptive packed-position accumulator.
- Replace the thread-external always-dense `long[65]` path with the same sparse-first representation and existing main-thread handoff.
- Preserve physical-source event-time handling and loaded-only deferred resync.
- Change routine geometry batching to one fixed `MUTATION_BATCH_TICKS = 20` deadline using the existing dimension-level deadline owner.
- Make publication observe pending Page revisions immediately.
- Drain the one pending-owner list in canonical order at the batch cut and feed the existing bounded resolved-input lifecycle.
- Do not add a second mutation queue, worker, global manager, or permanent Page bitmap.

### [x] Stage 2: Remove duplicate delta transport and full desired-signature clones

Primary owners: `ThermalPage`, `ResolvedGeometryInputRing`, and `MinecraftThermalTopologyApplier`.

- Delete `GeometryDeltaCoalescer`, `GeometryDeltaRing`, `ThermalPage.latestBrickMutationRevisions`, constructor parameters, drain loops, and transport-only tests.
- Carry stable Page identity and exact center/revision through `ResolvedGeometryInputRing`; remove per-center `new PageIdentity(...)` allocation.
- Deduplicate dependency-expanded centers before resolution.
- Add the sparse desired-signature overlay and stop cloning `appliedSignatureIds[4096]` for normal mutation.
- Replace 64-signature scans for sparse changed Bricks with exact changed-center comparison against applied signatures.
- Keep full 64-block comparison for full snapshot/full-resync and as a guarded fallback.
- Transfer full-resync snapshot ownership into the ring without a second 4096-int clone.
- Move converted signature geometry out of per-dimension appliers and build one shared topology-class, air-mask, face-mask, and component-ordinal view per registry cut.
- Classify primitive signature delta flags and exact face directions.
- Acknowledge final-state reversions without topology replacement.

### [x] Stage 3: Sparse Brick/Page transaction staging

Primary owners: `ThermalPage` and `MinecraftThermalTopologyApplier`.

- Replace Page-shaped single-Brick `PageBuild` with a scalar `BrickBuild` plus one reusable `BrickCompileScratch`.
- Change local Page install APIs to accept sorted sparse Brick replacements.
- Remove local-rebuild clones of unchanged coverage, summary, mixed-geometry, material-slot, phase-slot, and descriptor arrays.
- Stage only K changed entries and assign committed PageState slots after full preflight.
- Reuse the resolved-ring mutable input holder.
- Preserve the full-Page builder only for admission/full resync.

### [x] Stage 4: Material structure/binding split and exact propagation

Primary owner: `MinecraftThermalTopologyApplier` with fragment integration in `ThermalSweepFragments` only where required.

- Replace `materialDirtyBrickMask` semantics with distinct surface-structure and binding dirty state.
- Patch stable material descriptors by exact owner block ranges.
- Rebind unchanged descriptors without `collectMaterialCandidates`/`airMicrocellIfPresent` discovery scans.
- Replace unconditional `markMaterialNeighbors` with exact direction/boundary propagation.
- Preserve canonical descriptor/fragment order and transactional patch preflight.
- Notify physical source bindings only for Page sections whose accepted topology/bindings changed.

### [x] Stage 5: Lifecycle, retry, and full-resync convergence

- Cover Page admission, section replacement, retirement, profile reload, ring overflow, memory refusal, stale frame, in-flight recovery, and idempotent close.
- Ensure pending exact positions survive retry or are reproducible from the latest authoritative frame.
- Ensure each 20-tick batch cut resolves one final-state cut and cannot cause repeated full resync merely because final state keeps changing.
- Remove obsolete Brick-wide/deferred-dense fields only after all callers use the new owner.
- Do not leave commented-out compatibility code or a parallel old path.

### [ ] Stage 6: Post-change cache decision

Pending a controlled 120-second post-change JFR using the same room-toggle workload. No cache state was added in this implementation batch.

- Run the controlled JFR gate after Stages 1-5.
- Do not implement a cache in this batch.
- If normal compilation of repeated exact structures remains significant after the selected changes, create a separate timestamped plan with the 1 MiB experimental budget and cache-specific measurements.
- Record either `cache follow-up justified by JFR` or `cache not justified` in `Outcome`.

### [x] Stage 7: Documentation and implementation outcome

- Update `docs/climate/thermal-runtime-architecture-and-optimization.md` with implemented mutation intake, deadlines, exact propagation, and cache status.
- Update `docs/climate/data-lifecycle-and-integration.md` if Page revision/publication/fallback lifecycle changes.
- Update `docs/climate/README.md` only if its current-system navigation or summary changes.
- Append a new timestamped diary entry with decisions, source changes, exact tests, JFR comparison, allocation observations, documentation impact, and remaining work.
- Keep this plan `in-progress` until Stage 6 is measured, and fill the current implementation outcome without rewriting earlier diary history.

## Validation Matrix

### Unit and integration coverage

Extend the existing classes rather than creating a parallel benchmark-only runtime:

- `MinecraftThermalInputTest`
- `MinecraftThermalTopologyApplierTest`
- `MinecraftMaterialBoundaryTest`
- `ComponentBrickCompilerTest`
- `ConservativeAirGeometryTest`
- `ThermalPageTest`
- `ThermalCellArenaTest`
- `ImplicitAirAdjacencyTest`
- `GeometryMigrationLedgerTest`
- `ThermalSweepTest`
- `DimensionThermalRuntimeTest`

Required cases:

1. One packed local position, repeated hundreds of times, occupies one sparse entry and resolves once at drain.
2. Double-height/two-position changes remain generic and deduplicate through resolver dependencies without a door branch.
3. Sparse mode grows through 8/16/32/64/128 entries and promotes on the 129th distinct position with no lost/duplicated position.
4. A mutation at tick 100 opens deadline 120; mutations at ticks 101-119 join without moving it.
5. Continuous toggles produce cuts at most once per 20 ticks and start a fresh batch only after the prior cut is sealed.
6. A final signature equal to the applied signature returns `TOPOLOGY_UNCHANGED` and completes the scheduler epoch.
7. A signature-ID change whose `SignatureGeometry` is equal is also a topology no-op.
8. An interior changed block does not propagate material surface/binding dirtiness to any of six neighboring Bricks; a real Brick replacement still rebinds its own and three negative-axis pair owners because arena endpoint identity changed.
9. Face, edge, and corner changes propagate to exactly one, two, and three neighboring Bricks.
10. Cross-Page boundary propagation uses the correct Page generation and base Brick.
11. Equal contact structure with replaced arena slots performs binding-only work.
12. A changed air/material face patch performs structure patching for exact owner/neighbor blocks.
13. Same air union with changed component partition is not misclassified as a no-op.
14. Unresolved dependency, unloaded neighbor, and resolved-input ring overflow request the correct full-resync/fallback path.
15. Page retirement/reuse while a mutation is pending cannot apply the old owner's position to the new Page.
16. Profile/signature reload invalidates staged overlays/descriptors and replaces the shared registry-cut compiled view before new dimension admission.
17. Preflight failure leaves old Page, fragments, arena spans, material descriptors, and bindings unchanged.
18. Accepted replacement releases old spans only after the replacement is committed.
19. Source settlement/rebind order and routed energy are unchanged for source-bearing block mutations.
20. Forward/reverse material and air fragment ordering remains deterministic.
21. Geometry migration conserves enthalpy under repeated open/closed structural toggles.
22. In-flight recovery and 20-tick batch retry converge without repeated full-Page reconstruction.
23. Unload/close/deferred-late-callback behavior remains idempotent.
24. Normal sparse mutation uses the desired-signature overlay; dense promotion occurs exactly at 1,024 centers or 32 dirty Bricks and does not broaden the dirty-Brick mask.
25. Full-resync snapshot ownership transfers once without a second 4096-int clone.
26. Every registered signature equals its topology-class representative, and representatives of different classes are unequal; sampled cross-pairs cover collision/equality boundaries without an O(S^2) test.
27. Precomputed face masks and all 64 component-ordinal lookups match `combinedFaceMask`/`componentAt` for every registered signature.
28. Sparse Page install changes only named Brick entries and leaves all other array identities/values unchanged.
29. Reused `BrickCompileScratch` is reset between adjacent Page/Brick compiles and cannot leak candidates or signatures.

Coverage map:

```text
CODE PATHS                                             REPEATED-TOGGLE FLOW
[+] recordMutation                                     [+] click/open/close events
    |-- source/sky/radiation existing contracts            |-- exact packed-pos dedup
    `-- packed local pos -> one 20-tick batch                `-- publication stale immediately
              |                                                       |
[+] final-state dependency resolution                               tick + 20
    |-- loaded-only center capture                                    |
    |-- exact center dedup                                             v
    `-- resolved-input ring                                  [+] signature overlay/delta
              |                                                |-- unchanged -> ACK only
[+] applier sparse overlay/preflight                            `-- changed -> exact Brick/face
    |-- topology-class/face/component tables                            |
    |-- BrickCompileScratch                                             v
    |-- surface vs binding dirty                              [+] sparse transactional commit
    `-- exact neighbor propagation                              |-- migration/source order
              |                                                 `-- old span release last
              v
[+] retry/full-resync/unload/reload/close                      [+] natural fallback while stale
```

### Build discipline

After all implementation/test edits are complete:

1. Run `git diff --check` on touched files.
2. Run the repository's complete thermal JUnit command once.
3. Run required Forge GameTests once.
4. Run broader build/static checks required by the repository.
5. Inspect the final diff for obsolete fields, duplicate lifecycle owners, accidental `design/` changes, and documentation drift.

The implementation turn should delegate final validation to a separate Luna agent only when explicitly requested at that time; the primary implementation still owns investigation and fixes.

## Performance Acceptance

Repeat the same 120-second room-toggle workload after a 10-second warmup. Record active Page count, gameplay signature count, raw mutation count, distinct packed positions, resolved centers, topology commits, dirty Bricks, surface-patched blocks, binding-only descriptors, full resyncs, PageBuild/array-clone counts, and lookup-table bytes using existing low-overhead observability or temporary profiler instrumentation that is removed before completion.

Required correctness gates:

- zero crash, invalid arena generation, stale Page publication, duplicate source settlement, partial commit, or conservation failure;
- zero full-Page resync in the normal repeated-toggle case;
- final open/closed state agrees with the world and a batch is resolved no later than 20 ticks after its first mutation;
- later mutations in an active batch do not move its fixed deadline;
- natural fallback remains available while affected Page publication is stale.

Required performance gates against `run/thermal-live-20260827-204347.jfr` under the same workload:

- at least 90% fewer final signature resolutions than raw mutation-dependent center visits in the baseline-equivalent event stream;
- no unconditional six-neighbor material invalidations;
- at least 95% fewer component-list scans attributable to `airMicrocellIfPresent`/`rebuildDirtyMaterials` (`423` baseline samples, sampling variance reported); remaining exact lookups use the registry-cut component-ordinal table;
- at least 70% fewer topology-applier thermal execution samples (`532` baseline samples, sampling variance reported);
- at least 50% lower Server-thread allocation rate attributable to thermal mutation/topology stacks;
- zero repeated-door/sparse-mutation `int[4096]` desired-signature clones, density promotion only at the documented cut, and zero second-copy full-resync snapshots;
- zero normal local-rebuild clones of unchanged 64-entry Page/material fragment arrays;
- approximately 1,056 bytes less retained revision/coalescer array storage per active Page, verified from removed fields and a representative heap/JOL measurement;
- approximately 576 KiB less `GeometryDeltaRing` primitive storage per gameplay dimension, plus about 128-192 KiB/dimension saved by replacing resolved-ring section/generation arrays with one Page-reference array;
- zero `PageIdentity` allocation in the resolved-center drain;
- the shared registry lookup tables stay within `88 * signatureCount + array headers` primitive bytes, converted signature geometry is no longer duplicated per dimension, and neither structure scales with Page count;
- no regression greater than 5% in after-GC retained heap attributable to thermal state;
- steady-state server tick P95 materially below the `12.58 ms` baseline and no worse maximum than `20.87 ms` under equal Page/source/world conditions.

CPU sample percentages are directional. Final acceptance must also use exact operation counts and tick-duration distributions, not sample counts alone.

## Failure Modes And Required Fallbacks

| Failure | Required behavior |
|---|---|
| Sparse accumulator cannot grow | Promote to bounded dense bits; if bounded storage is unavailable, require existing full resync and natural fallback. |
| Pending owner is stale/retired | Drop by owner identity/lifecycle generation; never apply to replacement Page. |
| Dependency is unloaded/missing | Mark unresolved/full-resync requirement; do not load chunk or guess geometry. |
| Resolved-input ring is full | Preserve latest revision requirement and retry/full snapshot through existing bounded contract. |
| Fixed 20-tick cut fires during continuous toggles | Resolve one final-state cut; do not replay every event or move the current deadline. |
| Final state returned to applied topology | Acknowledge `TOPOLOGY_UNCHANGED`; no compile, replacement span, or material neighbor scan. |
| Arena replacement cannot be allocated | Keep old topology authoritative, expose natural fallback, retry under existing memory policy. |
| Fragment preflight fails | No partial Page/arena/material/source commit. |
| Unexpected delta classification | Escalate that Page/Brick to the existing conservative full compile/full-resync path, record observable reason, and preserve old authority until accepted. |

Fallbacks resolve state inconsistencies inside the runtime. They must not require the player to reopen a door, reload a chunk, restart the game, or manually clear a cache.

## Contracts That Must Remain True

1. One `ServerLevel` has one active input/runtime owner.
2. Minecraft capture, topology mutation, source flush, solver mutation, and publication production remain server-main-thread operations.
3. Lifecycle generation, geometry revision, topology generation, source/input watermarks, and solve epoch remain monotonic.
4. Topology replacement completes full preflight before authoritative mutation.
5. Old arena spans remain live until accepted replacement/retirement commit no longer references them.
6. Source energy is settled exactly at authoritative event boundaries and rebind settles the old sink first.
7. In-flight epoch recovery completes or restores exact state before the latest pending frame starts.
8. Phase ACK authority continues to match generation, Brick, profile, sequence, and current world state.
9. Forward/reverse traversal and compensated accumulation order remain deterministic.
10. Query readers retain generation/revision/age/seqlock validation.
11. Miss, stale, unresolved, memory refusal, and work-limit paths use natural fallback without loading chunks.
12. Page withdrawal, chunk unload, section replacement, runtime close, and duplicate notifications are idempotent.
13. No active `TemperatureThreadingPool`, thermal worker, or hidden executor fallback is introduced.

## Not In Scope

- Structural template caching: it adds retained state before the measured exact-invalidation path is optimized; a separate plan requires post-change JFR evidence.
- Adaptive-width applied signature storage: potentially saves 8-12 KiB/Page, but changes every hot signature access and needs retained-heap/Page-count evidence.
- `LoadedCube`/resolver snapshot scratch redesign: coalescing may make this cost disappear; change it only if the applier is no longer dominant and input capture becomes measurable.
- Solver, publication high-water, arena compaction, source accumulator, radiation, natural refresh, sky refresh, and query spatial indexes: independent performance surfaces already documented in the living architecture review.
- Steady five-tick solve cadence: `MUTATION_BATCH_TICKS = 20` changes routine geometry batching only.
- Block-specific door/gate/trapdoor handling and any active thermal worker.

## Open Questions

These questions do not block Stages 1-5:

- Does exact invalidation alone remove enough compilation work that a future two-variant, 1 MiB experimental cache has negative value? Decide only from the controlled post-change JFR; no cache state is added by this plan.
- Can existing material descriptor fragments expose compact owner-block ranges without adding permanent per-descriptor metadata? Prefer deriving ranges during compile; measure before adding an index.
- Do active Page count and gameplay signature count justify adaptive-width applied signature storage (`byte[]`/`char[]`/`int[]`)? This could reduce the current applied 4096-ID Page payload from about 16 KiB to 4 or 8 KiB, but it adds access decoding on every lookup and is deferred until retained-heap evidence exists.
- Does final-state input capture remain visible after coalescing? Reuse `LoadedCube`/snapshot scratch only if the post-change JFR shows input capture rather than the applier as a remaining hotspot.

## Outcome

Implementation Stages 0-5 and documentation Stage 7 are complete. Stage 6 remains open because no controlled post-change JFR has been recorded yet.

- `MinecraftThermalInput` now owns one fixed 20-tick exact-position batch, shared by main-thread and thread-external hooks. Positions remain sparse through 128 distinct entries, promote on the 129th, expand resolver dependencies only at the cut, and immediately advance affected Page revisions so stale publication falls back naturally.
- `ThermalPage`, `ResolvedGeometryInputRing`, and `MinecraftThermalTopologyApplier` now carry stable Page identity and sparse center/Brick transactions. `GeometryDeltaCoalescer`, `GeometryDeltaRing`, duplicate Page-local Brick revisions, per-center `PageIdentity` allocation, ordinary `int[4096]` desired clones, unchanged Page-fragment clones, unconditional six-neighbor material invalidation, and repeated component discovery for binding-only work were removed.
- Full-resync snapshot ownership transfers once; registry-cut topology/face/component tables are shared; material surface structure and arena binding dirtiness are separate; exact changed faces control material propagation, while a replaced Brick rebinds the bounded own/negative-axis Air pair owners; source-binding notifications remain section-local.
- The touched thermal production diff is 1,095 added and 804 removed lines, net `+291`, including deletion of 301 transport-only lines. Planned retained primitive payload reductions from removed owners are approximately 1,056 bytes/Page plus 576 KiB/dimension for the old geometry delta ring; post-change retained heap is not yet measured.
- Independent Luna Max validation passed all `190/190` thermal JUnit tests and all `13/13` required Forge GameTests. The admission regression freezes only Page revisions actually invalidated by the open batch, and the mixed-Brick regression verifies replacement slot identities rebind every pair owner before old spans retire. The source integration test allows the 20-tick topology cut, binding commit, and one subsequent 5-tick source integration interval; its fixture work limits cover the seven-Page continuation topology instead of suppressing publication at `385/256` cells.
- `gradlew build --offline --console=plain` was executed. Thermal tests passed, but the complete build remains red because the unrelated `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec` reads a missing hard-coded local save path (`784` tests, `1` failure).
- Living documentation: [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md) and [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md). Implementation diary: [`diary/2026-08-27_23-16-53_thermal-mutation-exact-invalidation.md`](../diary/2026-08-27_23-16-53_thermal-mutation-exact-invalidation.md).
- Cache decision: deferred to Stage 6; no cache was added. Before/after CPU, allocation, tick distribution, after-GC heap, operation counters, and remaining measurable traversal hotspots must be recorded from the controlled post-change JFR before this plan becomes `completed`.

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|---|---|---|---:|---|---:|
| CEO Review | `/plan-ceo-review` | Scope and strategy | 0 | not run | 0 |
| Eng Review | `/plan-eng-review` | Architecture, lifecycle, tests, performance, and memory | 1 | completed with incorporated findings | 7 |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | not applicable | 0 |
| DX Review | `/plan-devex-review` | Developer experience | 0 | not applicable | 0 |

Eng findings incorporated: replace dual deadlines with one 20-tick batch; keep positions exact end-to-end; delete duplicate delta/revision storage; use sparse desired-signature overlays; share compiled signature lookup data across dimensions; replace Page-wide local staging with sparse replacements/reusable scratch; defer all cache allocation until a separate JFR-backed plan.
