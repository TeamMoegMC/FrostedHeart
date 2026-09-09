# General gameplay analytic fields: architecture and cost

- Time: `2026-09-08 18:28:06 +08:00`
- Authors: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `generator floor plus physical source; shared boss/command fields; ownership, composition, publication, and production validation`
- Related: [climate documentation](../docs/climate/README.md), [superseded generator draft](2026-09-08_14-14-41_generator-analytic-heat-field.md), [loaded-world recovery](2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md)

## Goal and Decision

Implementation review, 2026-09-08: ordinary queries with a known natural baseline
compose directly in one ordered loop; only town/phase consumers need reusable
reductions. Empty indexes bypass town sampling entirely. Real disassembly tests
show that the master block entity can already have lost its state: T1/T2 cleanup
therefore calls `GeneratorData.unregister`, which matches authoritative team
data by master position and dimension in a disassembly-only enumeration. No
additional retained reverse index or per-tick scan is introduced.

Retain generators as ordinary physical sources and add a regional gameplay temperature floor. Characters, crops, town buildings, and relevant block-temperature rules must observe the composed result. Bosses and `/heat_adjust` use the same general field definitions, index, and compositor. This completed plan records implementation intent and review history; source and living documentation own current behavior. The reasons below describe the pre-implementation state.

Move ownership of the existing analytic index from the physical runtime to a small main-thread service scoped to the actual `ServerLevel`. Reuse the existing geometry and combination concepts. This is a Java ownership boundary, not another thread, process, simulation engine, capability, or scheduler.

Ownership separation primarily fixes lifecycle coupling. It does not inherently accelerate an already-running temperature query. Cache the same index reference in `MinecraftThermalInput`, so the hot path does not acquire another world-map lookup or duplicate the definitions. Separate update-path optimization from this ownership decision when measuring results.

## Verified Reasons for Separation

- [MinecraftThermalInput.upsertGameplayAnalyticField](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalInput.java) calls `start` when the physical runtime is absent. Startup constructs the dimension runtime and calls `attachLoadedWorld`. Publishing a command or boss field therefore currently depends on physical-runtime initialization.
- The index is an instance member of that runtime. `invalidateGameplayProfilesForRecipeReload` calls `closeAll`; recreating the runtime constructs an empty index.
- [CuriosityEntity](../src/main/java/com/teammoeg/frostedheart/content/world/entities/CuriosityEntity.java) can retain `coldApplied=true` across a physical-runtime rebuild and only retries its ordinary AI publication when this flag is false. The field and its producer can therefore disagree after a rebuild.
- Analytic definitions describe gameplay temperature controls. Their validity depends on the producer and world, not Page admission, worker generation, source capacity, or physical-profile reload.
- `WorldClimate` is not a universal replacement owner: [ClimateCommonEvents.attachToWorld](../src/main/java/com/teammoeg/frostedheart/content/climate/event/ClimateCommonEvents.java) attaches its capability only to dimensions without fixed time. The service must also work in fixed-time dimensions.

Keeping ownership inside the physical runtime is possible, but requires producers to detect and republish after every rebuild and still makes analytic-only publication depend on runtime creation. A world-lifetime store removes those dependencies directly. No runtime epoch or retry protocol is needed for fields that remain valid across a worker rebuild.

## Ownership and Interfaces

Add `MinecraftGameplayFields` beside the Minecraft thermal integration classes. Keep pure definitions and composition in `thermal.field`.

```text
GeneratorData / Curiosity / heat_adjust
                  |
                  v
MinecraftGameplayFields: ServerLevel -> one ThermalAnalyticFieldIndex
                  ^                          |
                  | same reference           | analytic composition
MinecraftThermalInput -> raw live/dormant ----+----> gameplay consumers
                  |
                  +--> physical sources / worker / Page publications
```

- Use `IdentityHashMap<ServerLevel, ThermalAnalyticFieldIndex>` on the server main thread. Actual world identity prevents a new world instance from inheriting a previous world's fields at the same dimension key.
- Create an index lazily on the first publication or when constructing a physical runtime. A read-only miss returns the raw base without creating state. Retain an existing index, even when empty, until world unload so cached runtime references remain valid.
- Runtime construction stores the exact `indexFor(level)` reference. Never replace that index on physical close/restart. Runtime-absent consumers use a non-creating service lookup.
- Route field upsert, remove, inspection, and membership through this service. Existing `MinecraftThermalInput` facade methods can delegate during migration; callers do not need to know whether a worker exists.
- [MinecraftThermalEvents.onLevelUnload/onServerStopped](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/MinecraftThermalEvents.java) explicitly clear the world store/all stores. Physical-only close and recipe/profile reload do not clear them.
- The service has no worker lock, chunk ticket, mesh lease, independent tick, or field serialization. Existing server infrastructure may already start a shared worker pool; the claim is that field publication no longer initializes a dimension runtime.

Fields remain transient. Generator data reconstructs generator fields after a server restart; loaded boss entities reconstruct boss fields. Command fields remain session-only. Persistence is the provider's existing responsibility.

## General Identity and Storage

Replace the shared packed-position `long fieldId` domain with an immutable `ThermalFieldKey(ResourceLocation provider, long ownerHigh, long ownerLow, int channel)`.

| Provider | Owner identity | Channel |
| --- | --- | --- |
| `frostedheart:generator` | Full internal `TeamDataHolder.getId()` UUID | 0 |
| `frostedheart:curiosity` | Full entity UUID | 0 |
| `frostedheart:heat_adjust` | Packed center position in one owner word, other word zero | 0 |

Use namespace, both owner words, and channel for deterministic ordering after mode and priority. Do not truncate UUIDs or merge namespaces into a guessed bit partition. Cache producer keys for frequently repeated reports; this is identity caching, not a cache that can suppress restoration of desired state. Command removal at a center removes the command-owned field only; inspection can list every matching provider.

Keep one general index:

```text
Object2ObjectOpenHashMap<ThermalFieldKey, Entry> byKey
ArrayList<Entry> ordered
Entry: immutable field definition + provider-refresh seen flag
```

The existing fastutil dependency supplies the map. Both structures reference the same entry; neither duplicates field geometry. Queries traverse the compact ordered list with an indexed loop, not hash-table capacity or an allocating iterator.

1. An upsert looks up its key. For a matching primitive definition, mark it seen when applicable and return. A hot `upsertSphere(key, priority, mode, x, y, z, radius, value)` overload compares before allocating a definition.
2. Geometry/value-only changes replace the definition on the existing entry in expected O(1); the ordered position remains valid.
3. Insertions and mode/priority changes find a sorted insertion position and shift the list in O(F). Deletions remove from both structures; O(F) list work is acceptable for these less frequent operations. Do not sort the complete list on every report.
4. Keep the existing immutable cube, pillar, and sphere definitions; cache squared radius. An infrequent object-based upsert remains useful for commands and boss stage changes.

The current [ThermalAnalyticFieldIndex](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/field/ThermalAnalyticFieldIndex.java) scans to find the ID and calls `sort` even on unchanged upserts. Its sort can exploit already-ordered input; do not claim every update necessarily costs O(F log F). The proposed improvement removes the linear search and unnecessary sorting from repeated reports, at the cost of a map and an entry per field.

Do not introduce a second generator index, per-chunk field copies, spatial tree, callback formula language, or configurable composition pipeline. A spatial index becomes justified only if real field counts and query rates make whole-list containment scans a measured bottleneck.

## Temperature Contract

An analytic field evaluates a closed-form geometric membership test and applies a scalar operation. It does not simulate transport, obstruction, distance falloff, energy conservation, or latent heat. The existing shapes are constant inside their inclusive boundary.

Extend the explicit mode order to:

```text
FLOOR_FROM_NATURAL -> OVERRIDE -> MAX_HEAT -> MIN_COOL -> ADD_DELTA
```

For query point `p`, define `N(p)` as this consumer's local natural temperature and `P(p)` as its existing raw live/dormant/natural selection, both in Celsius. A relative-floor field supplies a temperature difference `d` in Celsius degrees, not an absolute Celsius value.

```text
D(p) = maximum d among matching FLOOR_FROM_NATURAL fields
B(p) = max(P(p), N(p) + D(p))       when such a field matches
       P(p)                        otherwise
T(p) = existing ordered control operations applied to B(p)
```

Multiple generator floors take a maximum; physical generator powers still add through the existing source ledger. No matching relative floor means no implicit clamp to natural. Explicit boss/command controls run afterward and may lower or replace the floor. Existing priority order is retained within each mode; ties use the complete new key.

Ordinary queries with a known natural baseline compose directly in one ordered pass. Town and phase consumers reduce matching operations into a caller-owned reusable nested `ThermalAnalyticFieldIndex.Sample`: maximum relative delta, last matching override, maximum absolute floor, minimum absolute ceiling, ordered additive sum, and presence flags. `Sample.compose(natural, rawBase)` applies those reductions. `Sample.guaranteedFloor(natural)` evaluates the same expression with raw base negative infinity. A sample belongs to the immediate main-thread consumer scope, like the existing `townScratch`; do not expose one shared mutable result across callbacks or retain its sampled values across ticks. The sampling loop invokes no external callbacks. Natural baseline calculation must remain a raw query that does not recursively compose fields.

Player/passive consumers already have their natural baseline. Town obtains a local natural value for a representative point only when raw fallback needs it or a matching relative floor requires it. Sample before that decision, and reuse the reductions instead of scanning fields twice. Diagnostic `fieldsAt` may still allocate; normal composition must not allocate lists, lambdas, streams, or field definitions. Summing deltas before applying them can change floating-point low bits compared with sequential addition to the base; require temperature tolerances, not bit-identical values.

### Generator Values and Physical Output

Read `GeneratorData.getRadius()` and `getTempMod()` using [GeneratorHeatFieldModel](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorHeatFieldModel.java) and the existing runtime configuration. Do not copy the hardcoded `HeatingState` formulas.

With source-default config, for positive levels `Lr` and `Lt`:

```text
r = floor(16 * Lr)                  if Lr <= 1
r = floor(16 + 8 * (Lr - 1))        if Lr > 1
d = floor(10 * Lt)
```

`r` is in blocks; `d` is a temperature difference. Publish only for an authoritative formed generator with positive radius and delta. Center its sphere at `actualPos.below(masterYPosInMB) + (0.5, 0.5, 0.5)`. Block-center membership preserves the former sphere boundary; player eye-position queries remain continuous.

At natural -40 C with delta 30 C, the floor is -10 C. Raw physical -20 C composes to -10 C; raw physical +5 C stays +5 C before controls. A strict floor hides physical gains until the source raises raw temperature above it. Useful enclosure amplification therefore depends on actual power and exhaust connectivity and must be validated in gameplay.

[GeneratorLogic.tickFuel](../src/main/java/com/teammoeg/frostedheart/content/climate/block/generator/GeneratorLogic.java) retains normal physical publication after `state.tickData`: 10,000 W per thermal level, 80% exhaust Air power, 20% radiation loss under the current profile. It works outdoors and indoors, without a room detector or analytic-radius clipping. A disconnected room gets the gameplay floor but does not receive exhaust heat merely because it is in the sphere. Physical sources, residual cooling, and direct radiation keep their existing contracts.

Never feed composed temperatures into FarField, worker enthalpy, Page publications, or dormant state. Never populate the sphere with cells. Field afterheat follows positive `RLevel`/`TLevel`; active physical power separately follows `isActive`.

### Relation to BlockTemperatureModel.applyHeat

[BlockTemperatureModel.applyHeat](../src/main/java/com/teammoeg/frostedheart/content/climate/BlockTemperatureModel.java) is a historical block-heating balance formula, not the current general field engine:

```text
if N > H: result = N
else:     result = min(N + m * H, H)
then apply the absolute-zero clamp
```

`m` defaults to 2. For N=-40 and H=30 it yields +20 C, whereas the proposed relative floor yields -10 C. Current production `WorldTemperature.naturalBlock` supplies H=0; `TownStageFourModel` still uses the old nonzero formula. This plan explicitly chooses uniform natural-relative floors across gameplay consumers. It does not claim exact restoration of every old heat-chunk temperature formula. Align the town model and its documentation during implementation; bosses keep their own ordinary combination mode and do not pass through `applyHeat`.

## Publication and Removal

### Generator

Use the existing `ClimateCommonEvents.onServerTick` level START enumeration of `CTeamDataManager.INSTANCE.getAllData()` as the single analytic publisher. After a holder's optional town advancement and before its morning settlement, read and publish that holder's desired `GeneratorData` state. This does not tick fuel or town progression again. Normal machine tick remains the physical publisher.

- Analytic publication runs for matching-dimension generator data even when members are offline or the source chunk is unloaded. Preserve the existing rules for whether levels themselves advance. Enforce the dimension match even when `DEBUG_MODE` bypasses town filtering.
- A machine state change after the START pass reaches ordinary analytic queries on the next valid level START, at most one tick later. Queries during town advancement can see the prior published state. Do not add another scheduler or a second every-tick producer to remove this bounded latency.
- Team transfer can remove a holder from the authoritative manager. Reconcile the generator namespace around the completed existing enumeration: `beginProviderRefresh(provider)` marks its entries unseen; valid upserts mark them seen, including unchanged reports; `endProviderRefresh(provider)` removes unseen entries with one list-compaction pass and map removals. This is enumeration reconciliation, not a TTL or producer heartbeat. Boss and command entries are unaffected. Complete removal only after completing the authoritative enumeration, not in an unconditional `finally` after partial traversal.
- Keep refresh startup lazy if no index exists. Entries created during the pass are seen by construction. Refresh work is O(F) plus the already-required holder enumeration, with no world/block/chunk scan and no per-tick set allocation.
- Authoritative disassembly removes the generator key immediately; rebind removes it from the old level before changing position/dimension. Only the currently bound generator may remove the team's field. A stale tower's disassembly must not remove a newer tower's output.
- Shutdown with positive levels retains the floor while the existing data model decays it. Ordinary chunk unload does not remove a team-owned field. Actual world unload clears its transient index; the next loaded-world holder enumeration reconstructs it.

### Curiosity and Commands

Curiosity publishes on its existing cold-stage lifecycle with its full entity UUID, and removes the field on reset/disperse/removal. Reset `coldApplied` on world removal so same-object readdition can republish through the existing AI condition. A physical-runtime rebuild leaves both the field and flag valid; no per-tick full-state republish is needed. NBT loading continues to reset transient publication state.

`/heat_adjust` publishes an ordinary namespaced field without starting the physical runtime. Preserve its existing shapes, controls, and command language. A command at a boss arena center cannot overwrite the boss's identity. Inspection reports the provider and key along with geometry/mode/value.

## Consumer and Block-Transition Integration

Integrate the shared composition into player, passive/crop, and weighted town representative queries, including runtime-absent and runtime-start-failure paths. Keep each caller's natural baseline and raw sampling policy. Town currently chooses its fallback per representative group; the all-groups-or-natural description in living documentation is stale and must be corrected when implementing.

Membership checks and inspection must use the same world index. Analytic coverage alone never acquires a mesh lease. Infrared remains a physical/dormant view under the existing renderer contract; this task does not fill Pages or bake a 144-cubed texture to display a scalar field.

The current [ServerLevelMixin_TemperatureUpdate](../src/main/java/com/teammoeg/frostedheart/mixin/minecraft/temperature/ServerLevelMixin_TemperatureUpdate.java) skips legacy solid warming and liquid evaporation when `ownsGameplayHeatingTransition` is true. Merely changing scalar temperature queries can therefore make an analytically heated block behave differently inside and outside admitted physical state.

Use the sampled explicit analytic bound to bridge this narrow ownership rule:

```text
L = Sample.compose(localNatural, negativeInfinity)
canApplyLegacyWarming(threshold) =
    composedTemperature >= threshold
    AND (not physicallyOwned OR L >= threshold)
```

Apply that test separately to the actual evaporation/melting candidate in the existing priority order. A bound sufficient for melting must not bypass physical ownership for evaporation that only raw physical temperature reaches. Negative boss deltas lower the bound. ADD_DELTA alone supplies no absolute bound and does not bypass latent heat solely because a field matches. Existing freeze/condense behavior and candidate throttling retain their contracts.

This explicitly permits gameplay-directed warming transitions independently of physical enthalpy when the analytic control itself guarantees the threshold. It does not introduce a second conservative phase solver. Use existing block mutation and phase-request invalidation after the transition, and test stale pending requests against actual block/profile changes. Keep the bound and composed scalar in the same immediate query result so fields are not rescanned for each threshold.

## Performance and Memory Comparison

Let F be fields in one world, Q queried points per tick, and G generator reports per tick. Existing team enumeration cost is already paid by town logic.

| Case | Current runtime-owned list | Proposed shared ownership and index |
| --- | --- | --- |
| Publish field without dimension runtime | May initialize runtime and attach loaded sections | Lazy small index only |
| Same definitions and algorithm, runtime active | Direct index reference | Same index reference; no inherent speedup |
| Unchanged generator reports | Linear ID search and unnecessary sort if using current upsert | Expected O(G), compare without definition allocation, plus O(F) provider reconciliation |
| Geometry/value update | Search and sort | Expected O(1) replacement per field |
| New/remove/reorder field | List mutation and sort/search | O(F) ordered-list mutation |
| Q ordinary query points | O(QF) containment and composition | O(QF) containment and reductions; no automatic asymptotic improvement |
| Field storage | O(F) definitions and list | O(F) definitions, keys, entries, map capacity and list |
| Physical-runtime rebuild | Loses field store; producer must restore | Same world-owned field store survives |
| Larger analytic radius at fixed F | No per-voxel field data | No per-voxel field data |

New query traversal has an additional entry indirection and may perform reductions absent from the old operation. For a tiny static population the old list can be equally fast or slightly faster and uses less index memory. Do not describe service extraction or the map as a proven steady-query optimization. The map is an update accelerator, not a spatial query index. At a fixed number of queries, radius does not add containment tests, but larger coverage can make more gameplay decisions eligible and require more covered town natural-baseline queries.

Compared with the older heat-chunk registration scheme, this plan avoids registering references in every covered chunk, associated persistence, and registration-induced chunk loads. That old partitioning could nevertheless make a local query faster when many unrelated fields exist elsewhere. A direct scan is the minimum-complexity choice for the expected small field population, not proof of a universally fastest data structure.

The service adds a world map and a few references. Keys, entries, and hash capacity increase index memory versus the current single list. Avoiding an otherwise-unused dimension runtime can save substantially more work/storage, but active generator sources still require normal physical simulation. Exact retained bytes, allocation rates, and milliseconds require real JVM measurements; no numeric benchmark is claimed by this plan.

### One Brick at a Page Boundary

A Page is 16-cubed blocks with 64 possible 4-cubed Bricks. [MinecraftPageManager](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/input/MinecraftPageManager.java) captures requested missing Bricks, not all 64 because an edge Brick activates. Face-neighbor expansion from one boundary Brick can initially reach at most 1/2/3 additional Pages for face/edge/corner placement; this is not a bound on later propagation.

The current refinement/release residual thresholds are 0.125/0.0625 C. Admission permits one new Page and up to 64 Brick captures per tick; it does not bound total solver work or retained memory. A sparse Page still carries fixed metadata/directory/publication costs. [WorkerPageStore.collectResidencyChanges](../src/main/java/com/teammoeg/frostedheart/content/climate/thermal/topology/WorkerPageStore.java) retains the entire resident mask when any source or hot Brick remains in the Page, so previously expanded cold Bricks need not retire individually.

Analytic fields never enter those hot masks. The retained generator source can still incur these physical costs; extracting its analytic floor neither multiplies nor eliminates them. Do not redesign Page residency in this change.

## Implementation Sequence and Production Acceptance

1. Move world ownership and adapt existing boss/command facades first. Verify ordinary field behavior with runtime absent, rebuilt, and recipe-reloaded; actual unload must clear the store. Add the namespaced key and update-aware ordered index, preserving control semantics and deterministic ordering.
2. Add the relative-floor mode and shared sampling to every gameplay consumer, then integrate generator publication/removal from authoritative team data. Preserve the existing physical source path and avoid duplicate fuel/town advancement.
3. Integrate the explicit analytic bound into the affected legacy heating branches. Update inspection and the owning living documents. No renderer, mesh-residency, or generic scripting changes are included.
4. Validate on real Forge GameTests and a normal mod server. Do not add JUnit or mocked thermal-engine tests. Existing entry points include [ThermalLoadedWorldGameTests](../src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/ThermalLoadedWorldGameTests.java) and [FrostedHeartMinecraftThermalInputGameTests](../src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/FrostedHeartMinecraftThermalInputGameTests.java).

Acceptance scenarios:

- Form real T1/T2 generators, exercise configuration and sphere boundaries, and compare outdoor, source-connected enclosed, and disconnected-room behavior. Record natural, raw physical, floor, composed temperature, and time until raw enclosure heat exceeds the floor.
- Confirm player/crop/town decisions, local natural changes, afterheat, overlapping floors, additive physical powers, boss stage controls, command overlap/removal, and fixed-time dimensions. Check physical-only gameplay when no floor matches.
- Exercise late runtime creation, runtime-start failure fallback, unchanged-state runtime rebuild, recipe reload, actual server restart, boss removal/readdition, generator chunk unload, offline holders, team transfer/removal, and binding/dimension changes.
- Give equivalent ice/liquid blocks analytic bounds inside and outside physical ownership. Check each phase threshold, negative boss controls, delta-only fields, and pending physical requests after analytic-driven block mutation.
- With identical physical inputs and temperature level, vary analytic radius and confirm unchanged source targets/power, seeds, and residency attributable to the field itself. Isolate player interests and other physical producers. Separately measure physical exhaust placements at Page center/face/edge/corner.
- Measure normal-server main/worker time, allocations, and retained heap for both tiny/static and larger/updating field populations under matching query workloads. Record actual F, G, Q and town natural-query counts. Compare field-only cold publication separately from already-active runtime queries; do not merge those into one claimed speedup.

## Documentation Impact and Outcome

Design and source review completed. This plan supersedes the generator-only collection, runtime-start-on-field-report, chunk-unload removal, and machine-only analytic publisher in the earlier draft. It also records that `applyHeat` restoration is a gameplay balance choice, not a technical requirement of a general field engine.

Implementation completed on 2026-09-08. Generator, boss, and command fields now share world-lifetime ownership and namespaced keys; generator floors publish through the existing team enumeration. Ordinary queries compose directly, town/phase reuse reductions, and phase ownership checks the explicit bound for each heating threshold. Real T1/T2 disassembly tests exposed unavailable master state; cleanup now resolves authoritative team data only on disassembly. `TownStageFourModel` retains its historical offline heat-chunk formula, explicitly documented as a legacy balance reference rather than the current runtime floor.

Final Java 17 Forge `runGameTestServer --offline --no-daemon --console=plain` passed all 33 required tests after removing temporary diagnostics and matching the physical query's 40-tick age policy. No JUnit was added or run. The source-connected enclosure reached raw 29.883692 C above a 25.200000 C floor; the outdoor query returned the floor with no available raw Air sample. Eight new production-path GameTests cover field consumers, actual boss lifecycle, team authority, actual source-chunk unload, phase behavior, query cost, and enclosure heating; existing T1/T2 tests also verify floor lifecycle.

Updated [heat production](../docs/climate/heat-production-and-network.md), [world temperature](../docs/climate/world-climate-and-temperature.md), [runtime architecture](../docs/climate/thermal-runtime-architecture-and-optimization.md), [data lifecycle](../docs/climate/data-lifecycle-and-integration.md), [boss behavior](../docs/boss/curiosity-boss-design.md), and [town model](../docs/town/town-model.md). Full results and remaining validation are in the [implementation diary](../diary/2026-09-08_19-22-00_gameplay-analytic-fields-implementation.md).

The full acceptance matrix above is not claimed complete. Dynamic tower shapes can make an exhaust Brick unresolved, preventing physical enclosure heating for those placements; the passing enclosure fixture isolates a resolvable exhaust Brick. This existing topology limitation is documented, not fixed by analytic ownership. Long-running normal-server/multiplayer workloads, actual restart restoration, retained heap, matched old/new performance baselines, and Page center/edge/corner physical cost measurements remain unverified. Current query samples are descriptive and do not establish universal optimality.
