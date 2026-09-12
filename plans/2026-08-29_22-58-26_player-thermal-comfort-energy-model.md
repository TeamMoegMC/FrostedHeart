# Player Thermal Comfort And Body Energy Architecture

- Time: `2026-08-29 22:58:26 +08:00`
- Last revised: `2026-09-01 03:38:42 +08:00`; restored historical Wet semantics, added optional
  event-driven static Block radiation, selected the initial binary
  `canSeeSky` wind gate, defined contact media, bounded integration, legacy
  effect migration, exposure timing fixtures, and the HUD contract where both
  the number and orb color use environmental equivalent temperature; removed
  the unused client net-power presentation path; shortened the naked `-15 C`
  mild-hypothermia target to roughly one minute; first implementation pass now
  completes the player body/HUD/network path while optional world lava/fire
  radiation remains deferred; final review removes their infinite Air-energy
  path and fixes sparse ownership, candidate-budget, source-isolation, and
  surface-invalidation contracts; the final architecture review replaces the
  parallel dense state arrays with one tagged state-code table, consolidates
  signature/geometry ownership into one server-wide primitive table, removes
  runtime reverse resolution and duplicate phase state lookup, shares immutable
  uniform Brick signature scalars, and closes receiver/source/revision and
  section-batched lazy neighbor-read lifecycle contracts; the final residency
  correction removes thermal-Page coupling and replaces it with receiver-lazy
  known-Brick radiation coverage retained only while its chunk is loaded; the
  final trace correction replaces redundant quarter-cell traversal with one
  section-cached block DDA and gives static rays a true no-witness path; the
  final query correction fuses coverage and emitter discovery into one bounded
  section pass that cannot be skipped by empty state or exhausted visit budget
- Authors: `Codex; OpenAI GPT-5; architecture and implementation planning`
- Status: `in-progress`
- Scope: `optional static Block radiation, player environment sampling, environmental equivalent temperature, body heat balance, clothing, wetness, equipment heating, food temperature, HUD, persistence, networking, effects, commands, tests, and performance validation`
- Related: [`design/creative-principles.md`](../design/creative-principles.md), [`design/world-design.md`](../design/world-design.md), [`docs/climate/README.md`](../docs/climate/README.md), [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md), [`docs/climate/world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md), [`docs/climate/heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), [`thermal async runtime plan`](2026-08-28_01-18-39_thermal-async-runtime-topology-refactor.md)

## Goal

Replace the current mixed-unit player temperature path with one scientifically
coherent, allocation-free heat-balance model while keeping the player-facing
experience simple:

```text
temperature-orb number = how cold or hot this place feels
temperature-orb color = the same place-temperature cold-to-hot band
body status bar = how far the player's body has already moved into danger
```

The environment number must remain an ordinary absolute Celsius value. A player
can understand `-20 C`, `-5 C`, and `+15 C` without knowing about the internal
normal-body reference, heat-transfer coefficients, clothing resistance, or body
energy. Clothing, wetness, activity, food, and difficulty affect the body state,
not the physical air-temperature reading.

The model must preserve the project's core experience: the world is physically
cold, heat is produced rather than fabricated, shelter and equipment make a
measurable difference, and maintaining body temperature remains the primary
survival problem.

## Design Alignment

- [`design/creative-principles.md`](../design/creative-principles.md) defines
  temperature and realistic body-temperature survival as core gameplay.
- [`design/world-design.md`](../design/world-design.md) requires ordinary
  physical laws, an average world temperature near `-15 C`, common local daily
  temperatures around `-10..-20 C`, and extremes reaching `-80 C` or lower.
- Nanomachine or physiological adaptation may change how a body tolerates an
  environment. It must not rewrite the temperature shown for that environment.
- Heat sources must conserve their declared source split. Direct radiation may
  warm a receiver without first warming the air, but it may not be injected into
  the Air Mesh a second time.
- Nothing under `design/` is changed by this work.

## Player-Facing Contract

| Surface | Value | Meaning |
|---|---|---|
| HUD temperature-orb number | environmental equivalent temperature, `C` | standard still-air temperature causing the same immediate environmental heat loss |
| HUD temperature-orb color | environmental equivalent temperature, `C` | uses the existing cold-to-hot orb bands from the same Celsius value as the number |
| hypothermia/hyperthermia bar and effects | body thermal state | whether current clothing, metabolism, wetness, and exposure are sufficient |
| `TemperatureProbe` | physical air temperature, `C` | actual Thermal Air value at the probed point |
| `ThermometerItem` | core body temperature, `C` | actual physiological core temperature |
| admin temperature command | all named values plus `W`, `W/m2`, and confidence | diagnostics; never a gameplay formula source |

The default Celsius interpretation bands describe the displayed number and
select its existing orb color; they are not instant damage thresholds:

| Equivalent temperature | HUD meaning |
|---:|---|
| `< -10 C` | severe cold |
| `[-10, 0) C` | cold |
| `[0, 10) C` | very cool |
| `[10, 18) C` | cool |
| `[18, 24] C` | comfortable reference environment |
| `(24, 30] C` | warm |
| `> 30 C` | hot |

`15 C` is therefore cool but normally manageable, not universally comfortable
and not immediately dangerous. Safety is decided by the body model over time.

## Verified Current State

### Environment And Radiation

- `MinecraftThermalInput.gameplayPlayerEnvironment` returns absolute Air
  temperature in degrees Celsius and fills one caller-owned
  `ThermalEnvironmentSample` with direct `radiantFluxWPerM2`.
- A lit Campfire is `8,000 W`: `80%` convection enters the thermal source
  ledger and `20%` is declared radiation. `RadiationService` receives the
  `1,600 W` radiative share and calculates nominal irradiance as
  `P / (4*pi*r^2)` after bounded candidate discovery, three receiver rays, and
  loaded-section occlusion.
- The Air query is O(1) after Page publication. The radiation query is bounded
  by `64` candidate visits, top `8` candidates, `24` rays, and `256` DDA steps
  per ray, with revision-backed witness reuse.
- Current-source `RadiationService.projectedMaximumBytes()` with production
  parameters is about `701,056 B` (`0.67 MiB`) per dimension. This plan adds no
  second receiver cache or per-block source registration; one optional sparse
  Brick aggregate index feeds the existing service.
- `StateStaticThermalResolver` currently treats every non-empty fluid as a
  conservative full-block non-Air volume, while `MinecraftThermalProfiles`
  assigns ordinary material profiles only when `fluid.isEmpty()`. Lava
  therefore creates neither a material boundary nor a physical source and does
  not heat nearby Thermal Air. Ordinary fire blocks likewise have no declared
  ambient heat path. Campfire is the existing implemented exception and must
  remain unchanged.

### Player Calculation Defects To Remove

- `TemperatureComputation.environment` converts absolute Celsius to a
  `37 C`-relative value before wind handling. The wind-chill expression then
  consumes the relative value even though the expression expects absolute
  Celsius, and it changes temperature even at zero wind.
- `HeatingDeviceContext.setPartData` initializes feeling from effective
  temperature, while `setFeelTemperature` performs addition despite its name;
  the caller adds the complete effective temperature again. Current feeling is
  therefore approximately double-counted before radiation.
- Direct radiation is dimensionally converted by
  `q*A*absorptivity*seconds/C`, but the provisional `5,000 J/K` effective
  capacity is not a documented core-body capacity. The HUD conversion
  `q*0.8/6` uses one coefficient outside a unified heat balance.
- `PlayerTemperatureData` mixes relative and absolute Celsius. The ordinary
  path restores `+37`; the first insulated/invulnerable path does not.
- The disabled legacy `TemperatureThreadingPool` no longer updates
  `blockTemp` or `windStrengh`, but both values are persisted, reloaded, and
  still influence players. Existing saves may therefore carry permanent stale
  per-player offsets.
- Weather defaults are negative (`snow=-5`, `blizzard=-10`) while the player
  path subtracts them, warming the player by `+5/+10 C`.
- `movementFeelTempDelta` subtracts literal `1` from a value near `0.001`, so
  the nonnegative clamp normally reduces movement feeling to zero.
- `FHMobEffects.WET` is currently only refreshed and displayed, but this is a
  regression from the historical player model. Before commit `e5835fe62`, Wet
  increased air heat exchange and was reduced by clothing fluid resistance;
  commit `475c42c7f` used an especially strong `+10 * (1-fluidResistance)`
  modifier. The current source retained the effect duration and wet-clothes
  comment while removing its thermal consumer.
- `FHBodyDataSyncPacket` is sent outside the 20-tick update gate, producing up
  to `20` NBT packets per player per second for a model updated once per second.

### Existing State And Consumers

- `PlayerTemperatureData` persists five part temperatures, five part feeling
  values, aggregate body/environment/feeling values, stale block/wind values,
  clothing inventories, and difficulty.
- Torso drives hypothermia/hyperthermia; head drives confusion; hands drive
  digging; legs/feet drive movement. `ResearchCommonEvents` also reads hand
  body temperature.
- `FoodTemperatureHandler` directly adds a Celsius delta to all body parts.
- `BodyHeatingCapability` implementations directly add effective Celsius.
  Current implementations include `SteamBottleItem`, `HeaterVestItem`,
  `CoalHandStove`, `MushroomBed`, `HeatingPadItem`, and `OxygenCandleItem`.
- `ArmorTempData.factor` is a legacy insulation score, typically `200..900`,
  not `clo` or `m2*K/W`. `heat_proof` and `wind_proof` are unitless values, and
  current code also uses wind proof as fluid resistance.
- `TemperatureProbe` already reports Air temperature. `ThermometerItem` already
  reports core body temperature after restoring the `37 C` reference.

## Architecture Decision

The player-side production path has exactly four owners. Optional main-thread
radiation indexing is upstream of this path:

```text
MinecraftPageManager                 BlockRadiationIndex
  owns source/frontier thermal         owns receiver-lazy static Block emitters
  residency only                       independently until chunk unload
             \                         /
              v                       v
MinecraftThermalInput
  observes live/dormant/natural Air and direct radiation
             |
             v
PlayerThermalModel
  owns pure heat-transfer and equivalent-temperature formulas
             |
             v
PlayerTemperatureData
  owns five existing body-part energy states, clothing inventory, and sync cut
             |
             v
TemperatureUpdate
  owns Minecraft event inputs, food/water costs, effects, and packet cadence
```

`FrostedHud`, commands, tooltips, food, effects, and research are consumers.
They never recompute the model or reinterpret stored fields.

Do not create another player thread pool, Page query path, radiation manager,
generic physics engine, or compatibility coordinator. The retained
`TemperatureThreadingPool.java` remains disabled as required and is not reused.

The player is a read-only receiver of Thermal Air and direct radiation. Body
heat loss is not written back into the Air Mesh, the player is not registered as
a moving physical source, and this plan reserves no later two-way coupling hook.

Player contact does not register a Thermal source. The level-thread cut reads
existing entity state (`isOnFire`, `isInPowderSnow`, and water/lava fluid
heights) and feeds primitive local exposure inputs directly to
`PlayerThermalModel`. That contact path performs no block scan, source-index
insertion, extra radiation ray, Air write, or retained allocation.

The existing Campfire implementation is outside this static Block-radiation change and
must not be modified: its source registration, `8,000 W` split, Air port,
radiation entry, mutation handling, and receiver cache remain the authorities.
This plan neither converts Campfire to a boundary nor adds a second Campfire
contribution.

## Optional Lava And Fire Direct Radiation

Lava and ordinary fire do not enter Air Mesh. The previously proposed fixed
lava boundary and ordinary-fire constant-power operation are rejected because
their persistent blocks would be infinite world-energy authorities and would
allow loaded farms to consume free heated Air. Nearby non-contact behavior is
limited to an optional read-only player-radiation contribution; local lava and
on-fire contact remain primitive `PlayerThermalModel` inputs.

This selection has the following exact consequences:

- crops, towns, probes, phase reservoirs, Air transport, doors, shelter heat
  retention, FarField, dormant Air, and solver sleep are unchanged;
- radiation can warm or endanger a visible player, but it cannot heat Air or
  soil and cannot be harvested by a thermal machine;
- a wall blocks the direct contribution, and leaving line of sight removes it
  without residual room heat;
- the field is intentionally a read-only gameplay observation of persistent
  world state, not a fuel-conserving `ThermalSourceLedger` source.

### Receiver-Lazy Radiation Coverage Correction

- Time: `2026-09-01 02:48:07 +08:00`
- Status: `completed`
- Precedence: this subsection supersedes every later statement that requires a
  thermal Page handle, Page admission/full recapture, Page retirement,
  continuation, player thermal lease, or `MinecraftSignatureCapture` callback
  to build or retain `BlockRadiationIndex` state. The profile, exposed-power,
  packed-emitter, mutation, candidate, range, and direct-player-only contracts
  remain authoritative.

Static Block radiation is receiver-lazy but not per-player-owned. One
dimension-owned `BlockRadiationIndex.ensureAndVisitNearby(...)` call both ensures
coverage for a conservative section-local 8-block cube and enumerates only known
emitters inside the exact spherical range. Built coverage is shared by every receiver
until chunk unload; there is no observer record, reference count, lease, expiry
wheel, or player cleanup in the index.

For each covered section the sparse index stores:

```text
long knownBrickMask
long emitterMask
long[] packedEmitters  // ascending emitter-bit order
```

`knownBrickMask` distinguishes an already compiled empty Brick from an unknown
Brick, preventing repeated 64-state scans. A section whose palette cannot
contain any state with a nonzero radiation profile is rejected by
`LevelChunkSection.maybeHas(...)` and receives no radiation map entry or
radiation-created owner; an owner may already exist for unrelated thermal
runtime work. Repeating that bounded palette predicate on a later player sample
is cheaper than retaining known-empty state for ordinary sections.

For a palette-positive section, the fused call computes a section-local Brick
mask for the conservative 8-block cube and queues only
`requestedMask & ~knownBrickMask`. One reusable
`Long2LongOpenHashMap<sectionKey, pendingBrickMask>` unions requests from all
players. The existing staggered 20-tick player cadence and a fixed per-tick
budget of 64 Bricks bounds cold work. Each Brick reads 64 primary states and, in
the extreme all-lava case, at most 208 cross-Brick/section neighbor states; the
conservative 64-Brick bound is therefore 17,408 BlockState accesses per tick.
Missing coverage contributes no fabricated radiation and becomes available
after its final-state Brick is compiled. Known emitter submission still applies
the exact spherical 8-block range, so conservative corner coverage cannot affect
the result.

`RadiationService` calls the fused provider whenever it is nonnull, even when
the index has no current state or physical discovery consumed every candidate
visit. Remove `NearbySourceIndex.isEmpty()` and do not add a separate
`ensureNearby` API. The provider traverses each of the at most eight sections
once: it resolves the loaded section, performs the map/palette/coverage work,
and visits known in-range emitters only while the passed remaining-visit count
is positive. Exhaustion stops emitter submission but never stops the remaining
coverage checks. A section with `knownBrickMask == -1L` skips requested-mask
construction entirely.

Each captured Brick reads 64 already-loaded BlockStates once, reuses the
existing profile/height/occlusion scratch, and produces the existing one packed
emitter or an empty known bit. Same-section requests are batched into one
commit. Capture never reads or creates `PageSignatures`, `ThermalPageHandle`,
`PagePublication`, worker Page state, arena cells, solver fragments, dormant
state, or infrared data.

Mutation remains event-driven:

- a known Brick with a radiation-profile state change is ORed into the existing
  two-buffer dirty map;
- the targeted lava `LiquidBlock.updateShape` hook marks its own known Brick
  when exposure may change;
- an unknown Brick ignores mutation because its later first capture reads final
  state;
- section replacement clears known/emitter bits for that section;
- chunk load performs no full radiation scan, while chunk unload walks only that
  chunk's fixed section array and removes its index, pending, dirty, and optional
  owner entries; no reverse chunk-membership index is added;
- loaded horizontal-neighbor availability marks only known boundary Bricks.

The low-level mutation hook may reuse an existing `SectionOwner` only for a
palette-positive covered section or a thermal-resident/source-bearing section.
Player sampling a palette-negative section never attaches an owner for
radiation. The index checks its own known mask, never
`pages.handle(sectionKey)`.

Static Block rays deliberately bypass `ReceiverCache`. Range is 8 blocks,
there are at most 8 static candidates, and each candidate uses one eye ray at
the player's current `getEyeY()`;
re-running the short DDA each player sample removes static source revision,
section-witness invalidation, and wall-mutation cache lifecycle. Physical
campfire/machine sources retain their existing multi-ray witness cache.

The occlusion predicate is one whole-BlockState boolean and never samples a
sub-block `VoxelShape`. Quarter-cell traversal therefore repeats arithmetic
without observing additional geometry. Replace it with one allocation-free
block-grid DDA shared by static and physical rays. Keep exact source/receiver
doubles, the existing corner/edge tie rule, safety step cap, loaded-only result,
and quarter-position receiver-cache key for physical witnesses; only the
traversal grid changes.

Add one `collectWitnesses` boolean to the existing tracer call rather than a
second tracer/interface. Static rays pass false: on section entry the tracer
resolves one loaded `LevelChunkSection`, reads subsequent BlockStates directly,
and never calls `MinecraftPageManager`, `loadedSectionOrAttach`,
`MutableTrace.addSection`, or the section-revision table. Physical rays pass
true and retain their current section witnesses/revisions. The tracer caches
only the current section key/reference for the duration of one ray and clears
the reference in `finally`; it performs at most one loaded-only section lookup
per crossed section and retains no new world state.

At 100 staggered players, stable source-free work is bounded by one fused pass
of at most eight section map/palette checks per player sample and zero BlockState
capture. A lava-heavy stable view performs index lookup plus at most 800 short static rays
per second. An 8-block block-grid ray crosses at most 8 boundaries on each axis,
so the conservative three-axis bound is 24 DDA advances per ray and 19,200
advances/s total; ties make the ordinary count lower.
Cold and mutation work is charged separately to the fixed Brick budget. The
index retains only covered palette-positive sections and is capped independently
from thermal `maximumPages`; capacity refusal leaves the optional contribution
absent without creating a Page or loading a chunk.

This selection rejects both competing extremes:

- indexing every loaded radiation-positive section performs unnecessary Nether
  work far outside every player's 8-block range;
- scanning the complete 8-block sphere on every player sample repeats thousands
  of BlockState reads and exposure calculations.

The architecture comparison is:

| Candidate | Cold work | Stable query | Retained/lifecycle cost | Decision |
|---|---:|---:|---|---|
| thermal Page-coupled capture | appears free only when a Page already exists | fast | misses source-free player areas or forces player thermal residency | rejected |
| all-loaded-section index | every loaded positive section | fastest | scans/retains regions no receiver can use | rejected |
| direct receiver sphere scan | none | repeated BlockState/exposure scan | no index, but highest routine CPU | rejected |
| receiver-lazy known-Brick index | 64 reads once per requested unknown Brick, budgeted | at most eight section checks plus actual rays | covered positive sections only; chunk-unload cleanup | selected |

The receiver-lazy known-Brick index keeps the current fast steady query while
building only gameplay-reachable coverage and preserving zero thermal/network
coupling.

### Startup State And Radiation Profiles

Add one immutable nested radiation-profile table to
`MinecraftStateThermalTable` in the existing server-wide Minecraft profile
snapshot. Profile `0` means no static radiation. Every other profile contains
only startup-precomputed `fixedPowerW`,
`surfacePowerWPerM2`, and the one shared directional bound. Initial profiles set
at most one power mode: arbitrary code-owned static radiators use fixed power,
while exposed-surface power is lava-only. Do not add a generic surface-provider
shape/group contract before another implemented block requires one. Runtime
performs no config lookup, fourth power, tag lookup, callback dispatch, or
profile search.

Use one dense `int[] thermalStateCodeByStateId`, keyed by the process-local IDs
from `Block.BLOCK_STATE_REGISTRY`. It replaces the current boxed
`IdentityHashMap<BlockState, Integer> signatureIdsByState`; do not add a field to
every `BlockState` singleton or retain parallel dense signature/radiation arrays.
Build it only after Forge block-state IDs are stable, size it to `maximum
registered state ID + 1`, initialize every entry to `Integer.MIN_VALUE`, and
query IDs directly with `Block.BLOCK_STATE_REGISTRY.getId(state)`. Do not call
`Block.getId(state)`, which maps an unknown state to Air ID `0`.

The code is the complete immutable mutation semantics for one registered state:

```text
code >= 0
  code is the signatureId directly
  radiation profile = 0
  source semantics = 0

code < 0 and code != Integer.MIN_VALUE
  ~code indexes ExtendedStateSemantics

code == Integer.MIN_VALUE
  unresolved/dynamic/unknown state
```

`ExtendedStateSemantics` is represented by parallel primitive arrays, not
one object per entry:

```text
int[]  signatureByExtended
int[]  radiationProfileByExtended  // absent when optional radiation is disabled
byte[] sourceFlagsByExtended
byte[] occlusionByExtended
```

One `byte[] occlusionBySignature` stores the first/canonical DDA occlusion bit
for each signature. A state matching that bit keeps the direct signature code;
only a same-signature exception enters `occlusionByExtended`. This closes
`canOcclude` witness invalidation without a dense per-BlockState bitset or two
virtual predicate calls on every ordinary mutation.

`sourceFlagsByExtended` is the complete current Campfire source semantic, not a
generic "source involved" boolean. Bit `CAMPFIRE_PRESENT` preserves the current
disabled-descriptor lifecycle and bit `CAMPFIRE_LIT` mirrors
`CampfireBlock.LIT`. The mutation hook emits `SOURCE_MUTATION` exactly when the
decoded old/new bytes differ. A facing or visual-only transition with equal
source flags does not rescan the source, while placement, removal, lighting, and
extinguishing do. No source-kind registry or wider source payload is added.

Only fire, soul fire, lava variants, Campfire source states, and another state
with genuinely different static mutation semantics receive an extended entry.
States with identical signature, radiation profile, and the exact source-flags
byte share one entry even when their visual properties differ. Distinct entries
are used only when fixed watts, emitter position, lava height/surface semantics, or
physical-source observation can change. The extended count must remain below
`Integer.MAX_VALUE` so `~index` can never equal the unresolved sentinel.

The same server-wide snapshot owns one immutable `ThermalSignatureTable`; it
replaces both retained `ThermalSignatureRegistry` and the per-dimension
`ThermalSignatureCatalog`:

```text
signatureId -> geometryId
signatureId -> materialProfileId

geometryId -> ConservativeAirGeometry.Resolution
geometryId -> proven Air mask
geometryId -> material contact-pattern ID
geometryId -> component lookup mode/offset
mixed component ordinals -> one packed byte range only for multi-component geometry
```

The startup builder interns geometry first, then interns the exact
`(geometryId, materialProfileId)` signature in the current deterministic
BlockState traversal/first-seen order, preserving every existing signature ID.
`materialContactPatternId` is derived once from the geometry's material mask and
is not repeated in every signature. A neutral `materialProfileId == 0` still
exposes contact pattern `0`; only a nonzero material profile reads its geometry's
derived pattern, preserving the current no-material fast path. Zero-component
geometry always returns `0xff`; one-component
geometry returns `0` only for a set Air-mask bit; only multi-component geometry
owns 64 ordinal bytes. This removes the current 64-byte component table per
signature when several material profiles share one geometry.

The builder-only maps used to intern geometry, signatures, contact patterns,
extended state semantics, and profiles are discarded after startup. The frozen
table retains no `idsBySignature`, `topologyClass`, `TopologyIdentity`,
`signatureIdsByState`, or `phaseProfileIds`. Exact signatures are already
interned, so `topologyEquivalent(a, b)` is simply `a == b`. Every dimension and
replacement engine receives the same immutable table reference rather than
rebuilding arrays and a topology-class map.

`PageSignatures` continues to store signature IDs, never geometry or extended
state codes. `ThermalSignatureTable` supplies one canonical immutable `Integer`
payload for each uniform signature ID, allocated once at startup and reused by
Page admission and Brick replacement. The unresolved `-1` sentinel uses one
shared immutable scalar as well. Nonuniform Bricks alone retain
`char[64]`/`int[64]` arrays.

`PageSignatures.valueAt` tests the scalar first and returns its int directly;
array payloads are therefore always length 64 and need no uniform-length branch.
`PagePublication.Brick` accepts only the scalar or a valid 64-entry primitive
array. This removes shared mutable length-one arrays, keeps every read O(1), and
eliminates up to 64 tiny objects per Page without a runtime interning map. Do not
intern or hash mixed Brick arrays.

Static capture performs exactly one state-ID lookup and one state-code array
load, then decodes the signature. It never calls
`StateStaticThermalResolver.resolve` at runtime: every supported static state was
resolved during the same startup build, while dynamic, moving-piston, and
unsupported states deterministically resolve to the sentinel again. Removing
that fallback also removes the only production need for a frozen
signature-to-ID reverse map.

The existing section mutation hook resolves old/new state IDs and codes once.
If either code is `Integer.MIN_VALUE`, the change remains topology-relevant. If
equal non-sentinel codes are observed, all thermal semantics are equal and the
hook returns. Otherwise it decodes only the required signature, radiation, and
source channels. An ordinary non-special mutation therefore performs two state
ID lookups, two dense int loads, and direct signature comparison with no
extended-table access, ambient-kind switch, tag check, map lookup, allocation,
Page invalidation, or worker message when unchanged.

Campfire detection moves into the exactly defined `sourceFlagsByExtended`, but
its existing `8,000 W` profile, source index, ledger, split, and observation
rules remain the sole Campfire authority. Source refresh compares the decoded
byte, not code identity and not a nonzero test; states whose source behavior is
identical share one code and do not create a false refresh.

Phase ownership derives `materialProfileId` from the decoded signature and
validates that the material profile is a `PHASE_RESERVOIR`; it does not keep a
second BlockState map. `MinecraftPhaseController.apply` reads the loaded
BlockState once, derives its signature/profile from the shared tables, and then
uses that same state for `StateTransitionData`, avoiding the current duplicate
`getChunkNow` and BlockState read through `MinecraftSignatureCapture`.

The startup builder expands `FluidTags.LAVA`, one explicit ordinary/soul-fire
block tag excluding both Campfire blocks, and future code-owned static radiator
definitions into exact BlockState profile IDs. It is discarded after table
construction. Do not add a runtime registry, inheritance graph, datapack loader,
hot reload, or dynamic callback registry. A BlockEntity whose power changes
without a BlockState change remains a physical source instead of using this
static table.

Precompute lava surface power once:

```text
lava_radiant_power_per_exposed_m2_W =
    effective_lava_emissivity
  * stefan_boltzmann_W_per_m2K4
  * ((T_lava_C + 273.15)^4 - (T_radiation_reference_C + 273.15)^4)
```

`Blocks.FIRE` and `Blocks.SOUL_FIRE` initially share the configured fixed
`fireRadiantPowerW` profile. All ordinary-fire age/connection states that have
the same fixed power map to that same profile ID: an age tick does not rebuild an
emitter. `AIR -> FIRE`, spread, replacement, extinguish, and `FIRE -> AIR`
change profile ID and mark only the fire's own Brick through the existing
section-state hook. Fire uses its block center, requires no neighbor callback,
and lets DDA own every wall/door visibility change. Campfire is excluded because
its physical-source split remains authoritative; a burning entity stays a local
body-contact input and is not a world Block emitter.

Lava power is proportional to faces exposed to the same loaded-only
non-occluding space used by the direct radiation model; hidden depth adds thermal
mass in reality, not instantaneous direct watts, and this optional model has no
lava-cooling reservoir. A larger visible lava surface therefore emits more while
equal-area deeper lava does not.

Use one named package-visible `MinecraftRadiationOcclusion.blocksRadiation`
predicate to compile the frozen state-table occlusion bit and for loaded DDA
reads. Emitter compilation reads that same bit from the already-resolved state
code:

```text
blocksRadiation(state) =
    !state.getBlock().hasDynamicShape() && state.canOcclude()
```

Do not invoke `VoxelShape`, shape joins, or micro-face compilation. For one lava
state at `p`, read only already-loaded state/fluid neighbors and define:

```text
h(p) = lava(p.above) ? 1 : fluid(p).getOwnHeight()

top_area =
    lava(above) || blocksRadiation(above) ? 0 : 1

bottom_area =
    lava(below) || blocksRadiation(below) ? 0 : 1

side_area(direction) =
    blocksRadiation(neighbor) ? 0
  : max(0, h(p) - (lava(neighbor) ? h(neighbor) : 0))
```

Use the top/bottom face center and the exposed vertical side segment center as
watt-weighted coordinates. Read each local BlockState/profile once into Brick
scratch. A neighbor inside the Brick reads scratch; a neighbor outside the Brick
but inside the current `16^3` section reads the already-owned
`LevelChunkSection` directly.

For a neighbor crossing the current section face, resolve that face lazily once
per radiation section batch into the reusable six-entry neighbor-section
scratch. `NEGATIVE_Y/POSITIVE_Y` read the already-loaded current chunk directly;
horizontal faces perform at most one loaded-only chunk lookup per required
direction. This path never calls `MinecraftPageManager`, tests a Page handle,
creates a thermal Page, or loads/tickets a missing chunk.

The radiation index attaches the existing mutation owner only after a
palette-positive section receives covered Brick state. It does not attach an
owner for a palette-negative query. Resolved/present bitmasks distinguish
not-yet-read from unavailable without an object wrapper. A batch with no
boundary radiator performs no neighbor-section lookup. Clear all six scratch
references in `finally` so the dimension scratch never retains another chunk.

Never call a world getter that loads a missing chunk, allocate
`Direction.values()`, retain a face list outside scratch, add a block-state halo,
or store per-block exposure after compilation. Treat an unavailable
cross-section neighbor as non-emitting/opaque for this cut; the explicit chunk
load/unload boundary repair converges it when availability changes.

Static Block emitters use a fixed `8 block` maximum gameplay range and one eye
ray at the player's current `getEyeY()`. This range owns the one/four/eight-block calibration fixtures and remains
narrower than the physical-source service's `16 block` feet/torso/head path. Do
not add another user-facing range until measured gameplay requires it.

### Event-Driven Brick Invalidation

`BlockRadiationIndex` is one nullable dimension-owned derived-state owner. It is
independent of thermal Page admission/lifecycle and uses
`MinecraftPageManager.loadedSectionOrAttach` only once to reuse the existing
mutation owner for a newly covered palette-positive section. It does not consume
ordinary geometry mutations. Only these events may mark a known Brick dirty:

1. the existing `LevelChunkSection.setBlockState` hook observes different
   old/new radiation profile IDs and marks the changed block's own Brick;
2. a lava `LiquidBlock` receives its normal Minecraft direct-neighbor shape
   callback and marks its own Brick;
3. section replacement clears that section's known/emitter state so a future
   nearby receiver compiles final loaded state;
4. loaded-neighbor availability changes mark only already-known boundary Bricks;
5. chunk unload walks only that chunk's fixed section array and removes every
   corresponding known, emitter, pending, dirty, and optional owner entry.

For vanilla and Forge lava, one targeted mixin observes only
`LiquidBlock.updateShape(...)`. Standard `Level.markAndNotifyBlock` already routes
direct-neighbor shape changes through this method; also injecting
`neighborChanged(...)` would duplicate the normal notification and is rejected.

The mixin retains one lazy tri-state byte (`UNKNOWN`, `NOT_SURFACE`, `SURFACE`)
per registered `LiquidBlock` singleton. The first enabled callback resolves
`FluidTags.LAVA`; later water/other-liquid callbacks perform one predictable
false byte comparison with no tag lookup. This is per liquid block type, not per
placed fluid, section, or Page. A non-liquid neighbor never enters the mixin.
Lava calls the Brick marker with `pCurrentPos`. Fixed-power profiles require no
block-specific callback. Code that deliberately suppresses Minecraft
neighbor-shape updates beside lava must explicitly invoke the same marker, while
raw section replacement is recovered by clearing the section entry.

This ownership deliberately distinguishes exposure from visibility. A block
touching lava can change exposed area and notifies the lava. A door, wall, or
other occluder farther along the receiver ray never rebuilds an emitter; the
next static `MinecraftRadiationOcclusion` DDA trace reads current visibility.
Physical-source visibility continues to use its existing section revisions and
cached DDA witnesses.

Relevant notifications merge into two reusable, synchronized
`Long2LongOpenHashMap<sectionKey, dirtyBrickMask>` buffers owned by
`BlockRadiationIndex`. A mark first tests the index's own `knownBrickMask`; an
unknown Brick is ignored because its first future capture reads final state. A
known Brick then performs only primitive `get(sectionKey)` and
`put(sectionKey, oldMask | brickBit)` under the short producer lock. Do not use
`Map.merge`, a lambda, boxed key/value, event object, per-block history, Page
field, worker message, or per-event allocation.

On the existing aligned 20-tick cut, `MinecraftThermalInput` swaps the
write/drain map references under the producer lock and releases the lock before
any world read, Brick compile, or index update. Drain rechecks that the section
is loaded and each bit remains known, processes the detached entries, and clears
that detached map in `finally` for reuse. A late or off-thread mark therefore
enters the new write map for the next cut instead of blocking behind 64-state
work or being lost.

Each set bit rebuilds exactly one final `4 x 4 x 4` Brick. Rebuild reads its 64
already-loaded BlockStates and only the face-neighbor states required by
lava, using caller-owned mutable positions and the reusable Brick scratch.
Repeated fire spread, lava flow, or direct-neighbor churn in one Brick produces
one rebuild per cut. This bounded scan is never reached by an unrelated block
mutation and never calls `collectCenter`, changes `PageSignatures`, submits a
`ThermalInputBatch`, touches `TopologyPlan`, or causes arena/solver/source/
infrared work.

The ordinary mutation hook still bumps the existing DDA section revision for
geometry changes because physical-source visibility already requires it. Static
Block rays do not consume that revision because they are retraced without a
witness cache. Section replacement clears derived radiation state. Chunk
load/unload and neighbor-availability hooks repair only already-known touching
boundary Bricks and never load another chunk. These low-frequency lifecycle
paths cover bulk/container availability changes without a periodic scan or
ordinary-geometry search.

### Receiver-Lazy Capture And Sparse Index Lifetime

`RadiationService` calls
`BlockRadiationIndex.ensureAndVisitNearby(receiver, remainingVisits, visitor)`
once for the loaded Bricks whose AABBs intersect the fixed 8-block range. The
fused request touches at most eight sections. A static, allocation-free
`LevelChunkSection.maybeHas(radiatingStatePredicate)` rejects a palette-negative
section without creating index state or attaching a mutation owner.

For a palette-positive section, queue only
`requestedMask & ~knownBrickMask` into one reusable primitive pending map. A
fixed per-tick cold-capture budget drains requested Bricks; requests from all
players union by section and Brick bit. A first query may therefore omit an
unknown optional contribution, but repeated queries never rescan a known empty
Brick and never create a thermal Page.

Use one caller-owned scratch per dimension:

```text
int[64]   profileIds
float[64] fluidHeights
long      radiatorMask, occludingMask
long[64]  replacementValues
long[64]  nextValues
long      replacementMask
LevelChunkSection[6] neighborSections
byte      neighborResolvedMask, neighborPresentMask
double    power, weightedX, weightedY, weightedZ
```

Each captured Brick reads exactly 64 already-loaded BlockStates once. Fixed
watts and exposed lava face watts accumulate as scalar `double` values, then
pack once into `replacementValues[brickIndex]`. The same private compiler handles
a known dirty Brick. There is no `BlockState[4096]`, Page-signature write, second
scan, per-Brick object, retained per-block/face state, stream, or temporary
collection. Same-section captures are committed as one batch and scratch resets
in `finally`.

`BlockRadiationIndex` stores one lazily allocated
`Long2ObjectOpenHashMap<SectionRadiationState>` keyed by section. A state exists
only for a covered palette-positive loaded section:

```text
SectionRadiationState
  long knownBrickMask
  long emitterMask
  long[] packedEmitters  // ascending Brick-bit order, compact capacity
```

`knownBrickMask` is the only extra authority required to distinguish unknown
from compiled-empty Bricks. The section object may remain with
`emitterMask == 0` while covered because deleting it would force repeated
64-state scans. Palette-negative sections retain no corresponding object.

One packed emitter is one primitive `long`, not a Java object. It stores three
unsigned 8-bit centroid coordinates at `1/16 block` resolution inside the Brick
and the raw bits of one finite nonnegative `float` watt value; Brick identity
comes from `emitterMask`.

Commit once per section, not once per dirty Brick. Compile all bits in the
section's dirty mask into indexed `replacementValues`, derive

```text
nextMask = (oldMask & ~dirtyMask) | replacementMask
```

and merge unchanged old values plus replacements in ascending Brick order into
`nextValues`. If `nextMask` and every packed value are bit-identical to the old
state, return without a map write, array copy, allocation, or revision change.
Otherwise grow the compact target array at most once, copy the complete next
sequence once with `System.arraycopy`, and publish `nextMask`. The target array
never shrinks while its section remains covered. Removing the last emitter keeps
only the known mask and reusable empty payload until chunk unload, preventing a
known-empty Brick from becoming cold-scan work again.

No radiation field is added to `PageEntry`, `ThermalPageHandle`,
`PagePublication`, worker `PageState`, arena, solver, Air operations, source
ledger, material edges, phase state, dormant storage, or infrared/network
payloads. Thermal admission, retry, retirement, worker restart, and dormant
restore never install or remove radiation state.

Section replacement clears the section state and pending/dirty bits. Chunk
unload removes all index, pending, dirty, and optional owner state by walking
only that chunk's fixed section array. A loaded section persists independently
of player movement and thermal Page retirement, so walking across a Page
boundary cannot erase nearby lava or fire coverage. There is no observer lease,
refcount, expiry wheel, per-player mask, or matching Page generation.

### Bounded Query And Static-Ray Identity

Do not register static emitters in `PhysicalSourceSpatialIndex` or
`ThermalSourceLedger`. `RadiationService` keeps the existing section provider for
physical sources and receives one nullable nearby-provider contract implemented
by `BlockRadiationIndex`. Its single `ensureAndVisitNearby` call receives
receiver coordinates, remaining visits, and `SourceVisitor`. It rejects section
AABBs, Brick AABBs, and out-of-range emitter centroids before calling the
visitor; rejected emitters do not consume candidate visits.

Discovery is two-phase under the unchanged candidate budget:

1. discover physical sources first with their current `64`-visit behavior;
2. whenever the nullable provider exists, call it exactly once and enumerate the
   eye-centered `8 block` cube, covering at most `2 x 2 x 2 = 8` sections.
   Empty index state and zero remaining visits still queue unknown coverage;
   only emitter submission is disabled;
3. both domains compete by flux upper bound for the same top `8` candidates and
   share the existing `24`-ray cap.

Physical-source revisions remain nonnegative. Define one named
`RadiationService.STATIC_BLOCK_REVISION = -1L`. Every static Brick uses its
packed world-aligned Brick-minimum `BlockPos` as `sourceKey` and exactly that
constant as its revision; do not reuse the `Long.MIN_VALUE` section-revision
sentinel. Exact equality with `STATIC_BLOCK_REVISION` selects eye-only
upper-bound distance and one eye ray and does not divide visible flux by
three. Do not use a generic `revision < 0` domain test or add a ray-mode/revision
array.

Exact equality with `STATIC_BLOCK_REVISION` also bypasses `ReceiverCache` and
performs one eye-to-emitter block DDA immediately with `collectWitnesses=false`. Static
range is only 8 blocks and contributes at most eight candidates, so retracing is
cheaper and simpler than retaining source/section revisions and wall witnesses
for mutable Block emitters. Physical Campfire/machine sources call the same DDA
with `collectWitnesses=true` and retain their existing feet/torso/head witness
cache and section-revision invalidation. A static membership, centroid, or power
update therefore changes only the sparse section payload; it never bumps a
revision for cache ownership.

Create/lookup `ReceiverCache` lazily on the first non-static candidate after
discovery, not before discovery. A source-free or static-only receiver therefore
allocates and retains no cache entry; a mixed receiver uses exactly the existing
physical entry. This is one nullable local branch, not a second cache.

Physical receiver-cache lifetime is explicit rather than time-scanned.
`RadiationService.removeReceiver(receiverKey)` performs one owner-thread O(1)
primitive-map removal. The existing player-logout event removes the receiver
from the current dimension, and one `PlayerChangedDimensionEvent` handler removes
it from the old dimension obtained from `event.getFrom()`. Respawn in the same
dimension keeps the key and lets the existing entity-generation mismatch clear
that receiver in place on its next sample. Do not add inactivity timestamps,
periodic receiver sweeps, a global player map, or another cache. The existing
`128`-receiver bound and LRU capacity fallback remain unchanged. Static Block
radiation adds no per-receiver entry, revision, witness, or cleanup path.

### Exact Cost Contract

- disabled: the tagged state/signature table still replaces existing topology
  lookup, but there is no radiation-profile array, index, dirty maps, provider,
  or capture scratch; the existing section entry and targeted liquid callback
  each keep one predictable nullable feature check, and the liquid tri-state is
  not resolved;
- enabled ordinary mutation: two process-global BlockState ID lookups, two
  `thermalStateCodeByStateId` int loads, and direct code/signature comparison;
  non-special states do not read an extended radiation array, and unchanged
  semantics perform no radiation map lookup, queue write, scan, or allocation;
- relevant event: one primitive section-map get/put OR and one Brick bit;
  repeated events in the same cut allocate nothing and collapse to that bit;
- surface notification: non-liquid blocks never enter the mixin; a non-surface
  `LiquidBlock` performs one cached false byte comparison; each registered liquid
  block type retains one tri-state byte rather than performing repeated tag
  lookup or adding state per placed fluid;
- stable palette-negative query: one section lookup plus one allocation-free
  `maybeHas` predicate and no retained index state;
- first palette-positive coverage: one pending-mask OR, then exactly 64 primary
  BlockState/state-code reads plus only the cross-Brick/section neighbor reads
  required by lava exposure under the fixed cold-capture budget; known empty
  Bricks are never scanned again;
- rebuild: one final 64-state scan only for the radiator's own changed Brick or
  lava that itself received a direct-neighbor callback, followed by at most one
  section array growth/copy. Same-section neighbors use direct reads; each
  required outer section is resolved loaded-only at most once per section batch,
  with no outer-neighbor Page handle/owner lookup, chunk load, or static
  occlusion-revision bump. The covered current section reuses its existing
  mutation owner in O(1);
- chunk/section availability: direct removal or replacement by chunk/section
  key, plus at most the already-known touching boundary Bricks; no Page
  enumeration, world scan, or chunk load;
- ordinary/pending PageEntry, every loaded SectionOwner, and all worker Page
  state retain zero radiation fields;
- active sparse state: exactly two 64-bit masks and `8 B` per emitting Brick as
  primitive payload, plus the JVM map/object/array overhead measured for the
  target runtime. A covered known-empty palette-positive section retains only
  its known mask and empty payload; a palette-negative section retains nothing;
- dirty coalescing: two primitive maps retain capacity proportional to the
  maximum simultaneously dirty known sections in one cut, never the number of
  historical mutations;
- player update: one fused traversal performs at most eight loaded-section
  map/palette checks total, not eight ensure checks plus eight discovery checks.
  It always queues unknown coverage, submits only in-range known emitters within
  the remaining physical-source visit budget, and the stable known or
  palette-negative path allocates no state. First palette-positive coverage alone
  allocates its retained `SectionRadiationState` and pending entry;
- static trace: one block-grid boundary advance per entered block, one direct
  BlockState read per advance, and at most one loaded-only section lookup per
  crossed section. It performs no Page lookup, witness-array write, revision-map
  access, or retained allocation;
- player logout or dimension exit: no `BlockRadiationIndex` work; the existing
  physical receiver cache still performs one O(1) removal in the dimension being
  left.

The single dense state-code table costs `4 * (maximum registered BlockState ID +
1)` bytes plus one occlusion byte per signature. Extended state payload costs
`6 B * special state semantics` without optional radiation and `10 B * special
state semantics` when its int profile array is present, before primitive-array
headers/alignment. Profile coefficients
remain approximately `16 B * unique radiation profile`. This replaces the
current boxed identity map while avoiding the rejected second dense `4 B/state`
radiation array and any field on every BlockState singleton.

The shared signature table retains two ints per exact signature plus one
geometry reference, Air mask, contact-pattern ID, and component mode per unique
geometry. Only multi-component geometries retain 64 ordinal bytes. It removes
the frozen reverse map, topology-class array/map, duplicate signature/geometry
object arrays, 64 ordinal bytes per material variant, duplicate phase map, and
all per-dimension catalog copies. Canonical immutable scalar payloads reduce the
Page limit's worst case from up to `3,200 * 64 = 204,800` separate length-one
arrays to one startup scalar per signature ID plus one unresolved sentinel;
mixed payload allocation and lookup are unchanged.

Radiation capacity is independent of thermal `maximumPages`. Keep one named
maximum covered-section constant in `MinecraftThermalInput` and include its
worst-case index/pending/dirty storage in the dimension memory reservation; it
does not belong to worker `ThermalDimensionLimits`. Capacity refusal skips only the optional unknown coverage;
it does not admit a Page, load a chunk, clear existing coverage, or affect Air.
Backing capacity is released on dimension close, while section entries are
removed on chunk unload.

For 100 one-Hz staggered player updates, the static provider performs at most
800 section map/palette checks per second total, not per player. A stable
source-free view performs zero BlockState captures. A lava-heavy Block-only view
uses at most 800 short rays/s; the conservative fixed-range bound is 24
block-grid boundary advances per ray and 19,200 advances/s total.
Physical-source rays keep
their existing independent behavior. This path adds no network payload.
Infrared continues to display published Air only and never fabricates hot texels
from read-only Block radiation.

### Minimality And Dirty-Buffer Decision

The two dirty maps are transient producer/consumer buffers, not two world
indexes. A relevant callback performs one synchronized primitive OR into buffer
A. At a 20-tick cut the level thread swaps A/B while holding the lock only for
the reference exchange, then rebuilds from detached B without blocking a late
or off-thread section callback; new events immediately continue in A. After the
cut, B is cleared and reused. No world read, Brick scan, emitter merge, target
array growth, or payload copy occurs while holding the producer lock. First-time
coverage publication and lazy dirty-map creation remain bounded cold structural
operations under that lock.

Normal dirty-buffer cost follows only the largest number of known sections
dirtied in one cut. Both maps are lazy, are bounded by the independent radiation
section limit, and retain reusable capacity until dimension close; repeated
changes do not grow them by event count or historical position count. Exact
retained bytes are measured on the target JVM and reserved through the same
limits object instead of inferred from a thermal Page count.

One map would be smaller only if every section mutation were guaranteed to stay
on the level thread. Current mutation delivery explicitly accepts deferred
off-thread marks, so a single map would require either holding its lock across
all 64-state rebuilds or allocating a copied key/value snapshot. A pending word
on every loaded section would charge unrelated sections. The double primitive
map is therefore the smallest design that preserves current cross-thread input
tolerance, 20-tick coalescing, and a short producer lock.

The minimum production ownership is two new feature classes, one replacement
table, and one targeted mixin:

```text
MinecraftStateThermalTable
  tagged state codes, sparse extended semantics, startup-only profile builder

ThermalSignatureTable
  replaces Registry + per-dimension Catalog with shared primitive signature/geometry lookup

BlockRadiationIndex
  known-Brick masks, cold pending mask, two dirty maps, sparse emitter payloads,
  nearby visitor

LiquidBlockMixin_ThermalRadiation
  one updateShape injection and per-LiquidBlock cached surface classification
```

Existing owners change only at their current boundaries:

- `MinecraftThermalProfiles` constructs and freezes the state/profile and
  signature/geometry tables, then discards all builder maps;
- `MinecraftSignatureCapture` remains thermal-signature-only and performs no
  radiation callback;
- `PageSignatures` reuses canonical uniform scalars supplied by the signature
  table but keeps the current mixed payload and O(1) read contract;
- `MinecraftPhaseController` decodes one already-read state through the shared
  tables and owns no capture/registry field;
- `MinecraftPageManager` has no radiation install/remove call and no radiation
  field on Page or SectionOwner;
- `MinecraftThermalInput` owns the nullable index, fixed per-tick cold-capture
  budget, and aligned 20-tick dirty flush;
- `RadiationService` accepts one nullable exact-range nearby provider, uses
  exact `STATIC_BLOCK_REVISION` equality in its existing candidate/ray arrays,
  always invokes one fused coverage/discovery pass, lazily creates a cache only
  for a physical candidate, passes one `collectWitnesses` boolean to the shared
  tracer, and removes a physical receiver by key in O(1);
- `MinecraftRadiationOcclusion` uses one block-grid DDA. It caches the current
  loaded section per trace, records revisions only for physical rays, and has no
  static Page/owner/revision path;
- `MinecraftThermalEvents` removes the existing receiver entry on logout and
  from the old dimension on dimension change;
- the mixin configuration adds only the targeted `LiquidBlock` mixin.

`SectionRadiationState` remains a private three-field nested value. Folding its
masks into `packedEmitters` would make unknown/known-empty/emitting state depend
on implicit array layout, so it is rejected. A flat Brick-to-emitter hash map
saves memory
only in extremely sparse sections but requires up to `5^3 = 125` Brick lookups
per player update and uses more memory for lava surfaces. A Page sidecar removes
one sparse map but adds a nullable field to every Page and reintroduces worker
lifecycle coupling. Per-block/face caches remove the 64-state rebuild only by
adding several retained words per emitting Brick. None dominates the selected
grouped sparse index for the declared many-player, mixed-fire/lava workload.

### Architecture Closure

The selected architecture has two independent sparse domains with no lifecycle
bridge: source/frontier-driven thermal Brick residency and receiver-lazy static
Block-radiation coverage. The tagged state table is shared data, not shared
ownership. A player query may queue radiation capture, but it cannot change a
thermal Page, cell, pair, source, dormant entry, infrared publication, or network
packet.

The radiation index is justified because it replaces repeated receiver sphere
scans. Its minimum retained state is one known mask, one emitter mask, compact
emitter values, one cold pending map, and the two already-required cross-thread
dirty buffers. No observer record, Page callback, static witness cache, source
revision, periodic scan, or loaded-world index remains.

One block-grid DDA is the sole occlusion traversal. BlockState-wide occlusion
cannot justify a finer grid. A boolean witness mode preserves physical caching
without a second algorithm, while the static path performs no witness or Page
work. This removes repeated quarter-cell arithmetic and per-block chunk lookup
without adding retained state, an abstraction, or another lifecycle.

One fused nearby-provider call is the sole static coverage/discovery traversal.
It replaces the old `isEmpty`/remaining-budget gate and any separate ensure
call. Empty state must be observable so it can become covered, and exhausted
physical visits may suppress only emitter submission. This preserves the exact
eight-section bound without a preparation cache, section list, second pass, or
new ownership state.

No additional routine high-water traversal remains to replace in this scope.
`QueryPublication` already iterates the arena live-slot bitset;
`ThermalSourceLedger` and `PhysicalSourceSpatialIndex` scan high water only for
engine reseed/close, not ordinary source delivery or block mutation. Adding live
indexes for those exceptional lifecycle operations would increase steady state
and mutation maintenance cost.

The remaining paired maps are distinct O(1) ownership indexes rather than
duplicates: Page/section identity, worker page slot, chunk membership, and
source origin/target answer different callers. Likewise, reusable compiler,
radiation, dormant, passive, and town scratch fields avoid per-call allocation;
environment/signature arrays transferred across the main-thread/worker boundary
require separate immutable ownership. Do not collapse these structures without
new JFR/heap evidence naming a dominant cost.

No palette-native full capture, mixed-Brick interning, generic emitter registry,
periodic cleanup, new live-slot tree, or additional cache is part of this plan.
Palette inspection is used only as the allocation-free negative gate. These
other mechanisms either repeat loaded-world work, move cost into routine
updates, or add an authority that current behavior does not need.

## Pareto Selection

The architecture is selected against four simultaneous objectives: dimensional
correctness, player comprehension, gameplay decisions, and steady CPU/retained
memory. Medical-model completeness is not an objective.

| Candidate | CPU/memory | Correctness | Gameplay value | Decision |
|---|---|---|---|---|
| raw Air number + one core scalar | minimum | misses wind/radiation and discards existing part mechanics | weak environmental feedback | rejected below the correctness and preservation floor |
| equivalent environment + five existing body-part energy states | near-minimum; same state order as today | unit-closed and preserves all existing part ownership | preserves shelter, clothing, wetness, part penalties, fire, and recovery | selected constrained Pareto point |
| five body-part energies plus new core/dose reservoirs | higher state/migration/tuning cost | duplicates existing physiological ownership | little visible gain | dominated by the selected model |
| full PMV/SET or multi-node medical model | highest complexity | wrong steady indoor domain for extreme survival | opaque and difficult to balance | rejected |

The selected model preserves `HEAD`, `TORSO`, `HANDS`, `LEGS`, and `FEET`, their
clothing inventories, area fractions, effects, research checks, and equipment
slots. Each existing part stores one unit-bearing energy value instead of one
relative temperature plus one persisted feeling value. Core temperature and
feeling are derived outputs, not additional authorities. Raw retained thermal
state remains the same order as the current ten floats, while no completed
player-facing mechanic is deleted.

Correctness means explicit units, energy-consistent body integration, monotonic
responses, one source contribution, stable persistence, and no contradictory
display semantics. It does not mean predicting an individual human's clinical
response.

## Unit Contract

Every new field, method, configuration key, formula symbol, test assertion, and
documentation anchor uses explicit units:

| Suffix or symbol | Unit |
|---|---|
| `TemperatureC`, `T` | absolute degrees Celsius |
| `TemperatureDeltaK`, `dT` | temperature difference in kelvin |
| `PowerW`, `Qdot` | watts, `J/s` |
| `EnergyJ`, `E` | joules |
| `FluxWPerM2`, `q` | watts per square metre |
| `HeatCapacityJPerK`, `C` | joules per kelvin |
| `ResistanceM2KPerW`, `R` | square-metre kelvin per watt |
| `CoefficientWPerM2K`, `h` | watts per square-metre kelvin |
| `AreaM2`, `A` | square metres |
| `VelocityMPerS`, `v` | metres per second |
| `Seconds`, `dt` | seconds |

Numerically, Celsius differences and kelvin differences are equal. Absolute
Celsius and a body-relative offset are not interchangeable. No new API uses a
bare name such as `temp`, `heat`, `factor`, `unit`, or `level` where the unit is
not fixed by an existing external contract.

The player model stores actual core temperature through energy; it does not use
`0 == 37 C`. Legacy getters may temporarily convert during migration, but no
new formula consumes the legacy representation.

## Environmental Equivalent Temperature

### Inputs

One player update obtains exactly one environmental cut:

- `T_air`: composed Thermal Air or the current natural fallback, absolute `C`;
- `T_water`: when vanilla water occupies the player boundary, derive one local
  liquid-water boundary temperature from the same environmental cut by clamping
  `T_air` to the configured liquid-water range; do not add a water simulator or
  a second mesh query;
- `T_lava = 1000 C` and `T_powder_snow = -30 C` as explicit local
  contact-boundary defaults, replacing the current hard overwrites with unit-bearing
  medium paths;
- `q_rad`: accumulated visible flux from existing physical sources and published
  static Block emitters, `W/m2`;
- `radiationConfidence` and flags from the same radiation sample;
- climate wind converted from the existing `0..100` scale to `m/s` through one
  explicit configured maximum; initial compatibility default is `70 km/h =
  19.444 m/s` at wind `100`; one level-thread
  `ServerLevel.canSeeSky(BlockPos.containing(player.getX(), player.getEyeY(),
  player.getZ()))` boolean gates whether that outdoor wind reaches the player;
- water/lava fluid heights, powder-snow contact, on-fire state, precipitation
  exposure, Wet effect, movement, food level, water level, equipment, and
  difficulty from the level thread.

The player layer does not add `blockTemp`, Gaussian noise, or arbitrary weather
temperature deltas. `T_air` is the sole air-temperature authority. Move the
existing configured diurnal amplitude upstream into
`WorldTemperature.naturalAir` and `MinecraftEnvironmentCapture` so players,
crops, towns, probes, and FarField boundaries observe the same physical air.
Preserve the existing configuration/default during that ownership move; any
later balance change is separate. Snow and blizzard affect outdoor wind, while
the initial `canSeeSky` gate decides whether that wind reaches the player. Their
event temperature is already represented by `WorldClimate` and is not
subtracted again in the player model. They do not create a second Wet producer.

### Initial Wind Exposure Gate

The first implementation intentionally uses one binary sky test rather than a
local airflow simulation:

```text
v_world_m_per_s =
    clamp(world_wind_0_to_100, 0, 100) / 100
  * configured_maximum_wind_m_per_s

sky_wind_exposed = ServerLevel.canSeeSky(player_eye_block)
v_local_m_per_s = sky_wind_exposed ? v_world_m_per_s : 0
```

Use `v_local_m_per_s` for environmental equivalent temperature, body
convection, and evaporation capacity. Do not read persisted `windStrengh` or
derive local wind from Air temperature. A roof that blocks `canSeeSky` fully
blocks environmental wind in this initial model. Doors, windows, open walls,
cave mouths, and cross-ventilation do not create partial drafts while the eye
block remains sky-blocked; they still change Air through the existing Mesh.
This is a deliberate initial gameplay/performance approximation, not a claim to
calculate indoor fluid velocity.

The sky test runs once per player model update on the level thread. It creates
no Page interest, block scan, ray, cache, retained player state, NBT, packet, or
worker operation. A later partial-draft model requires separate measured
justification and is not part of this implementation.

### Reference Heat-Loss Formula

Use a fixed reference skin temperature only to define the environmental index.
For air-only exposure:

```text
T_skin_ref = 33 C
h_nat = 2.38 * abs(T_skin_ref - T_air)^0.25
h_forced = 12.1 * sqrt(max(v_local_m_per_s, 0))
h_c = max(h_nat, h_forced)
h_r = configured long-wave coefficient, initial source default 4.7 W/(m2*K)

loss_ref_W_per_m2 =
    h_c * (T_skin_ref - T_air)
  + h_r * (T_skin_ref - T_air)
  - reference_absorptivity * q_rad

h_reference =
    max(h_nat, 12.1 * sqrt(reference_air_velocity_m_per_s)) + h_r

T_equivalent = T_skin_ref - loss_ref_W_per_m2 / h_reference
```

For contact media, calculate reference area fractions from the fixed body-part
vertical bands defined below. Lava and water use their existing fluid-height
ratios. Powder snow preserves the current whole-body contact behavior when
`isInPowderSnow` is true. Fractions are mutually exclusive in the fixed
precedence `lava > water > powder snow > air`, so they sum to one and a body
surface is never charged twice. Direct radiation applies only to the air share.
The local on-fire exposure is present only while `isOnFire && !isInLava` and is
converted from its configured reference power to `W/m2`; it never replaces Air
with a fictitious `300 C` value.

```text
f_air_ref = 1 - f_lava_ref - f_water_ref - f_powder_ref

loss_ref_contact_W_per_m2 =
    f_air_ref * (
        h_c * (T_skin_ref - T_air)
      + h_r * (T_skin_ref - T_air)
      - reference_absorptivity * q_rad
    )
  + f_water_ref * h_water * (T_skin_ref - T_water)
  + f_lava_ref * h_lava * (T_skin_ref - T_lava)
  + f_powder_ref * h_powder_snow * (T_skin_ref - T_powder_snow)
  - on_fire_reference_power_W / body_surface_area_m2

T_equivalent =
    T_skin_ref - loss_ref_contact_W_per_m2 / h_reference
```

This keeps the HUD as one still-air equivalent Celsius value: `15 C` water may
correctly display as much colder exposure than `15 C` air because the same
temperature removes body heat far faster in water. `TemperatureProbe` remains
the physical-temperature tool. Fire, lava, and powder snow therefore change the
equivalent exposure immediately without claiming that surrounding Air became
`300 C`, `1000 C`, or `-30 C`.

Required invariants:

- at reference air speed with no direct flux, `T_equivalent == T_air`;
- increasing wind cannot warm a sub-skin-temperature environment;
- increasing positive direct flux cannot cool the environment index;
- a blocked physical source or static Block emitter contributes zero direct flux but
  does not erase Air heat already stored in the Page;
- clothing, persisted Wet duration, activity, food, hydration, difficulty, and
  current core temperature do not alter `T_equivalent`; contact fractions,
  contact boundaries, and the reference on-fire power do because they describe
  immediate external exposure;
- missing or limited radiation contributes the resolved lower-bound flux and
  carries confidence for diagnostics; it does not fabricate a hot field or
  interrupt the body update.

This is an environmental equivalent index, not PMV/PPD or full SET. Those
iterative indoor-comfort models have restricted steady-state domains and do not
fit blizzards, immersion, direct Campfire radiation, or `-80 C` gameplay.

## Body Heat-Balance Model

### Persistent State

Preserve all five existing `BodyPartData` owners and replace each persisted
relative temperature/feeling pair with one energy state:

- `bodyEnergyOffsetJ` (`double`) on `HEAD`, `TORSO`, `HANDS`, `LEGS`, and
  `FEET`, relative to that part's normal energy;
- existing clothing inventories, body-part identity, area/weighting data, and
  difficulty;
- a model schema version.

Derived/transient values are not persisted:

- five absolute part temperatures and the aggregate core temperature;
- five estimated environmental/skin feeling temperatures;
- environmental equivalent temperature;
- net body power;
- radiation flux/confidence;
- air temperature, wind, and equipment profile.

Do not add persisted per-part, per-garment, or continuous player moisture.
`FHMobEffects.WET` remains the existing binary gameplay authority: present means
wet exchange is active; absent means it is not. Its existing effect duration
already persists and synchronizes through the Minecraft effect lifecycle.

Each part temperature and the existing aggregate core temperature are derived:

```text
C_part = body_heat_capacity_J_per_K * thermal_mass_fraction_part
T_part = core_reference_temperature_C + bodyEnergyOffsetJ_part / C_part

T_core = weighted(T_head, T_torso, T_legs, existing affectsCore weights)
```

Use an initial whole-body heat-capacity source default of `245,000 J/K`
(`70 kg * 3,500 J/(kg*K)`) and a body-surface-area source default of `1.8 m2`.
A separate dimensionless `playerThermalTimeScale` accelerates real
physiological time for gameplay. Do not reduce heat capacity to a hidden
`5,000 J/K` in order to make effects happen faster.

### Part Heat Transfer

Retain the existing five area fractions, effects, and equipment coverage. The
initial thermal-mass fractions use the same normalized part fractions so they
sum to one; they may become separately configurable only if calibration proves
that the existing fractions cannot satisfy both core and extremity timing.

For each part, calculate environmental exchange through the same unit-bearing
path:

```text
h_total = h_c + h_r
R_boundary = 1 / h_total

q_rad_part = q_rad * (1 - radiant_heat_proof_part)
T_rad_equivalent_part = T_air + absorptivity * q_rad_part / h_r
T_operative_part =
    (h_c * T_air + h_r * T_rad_equivalent_part) / h_total

R_path_part =
    tissue_resistance_part
  + clothing_resistance_part
  + R_boundary

G_air_part_W_per_K = area_part_m2 / R_path_part
Q_air_loss_part_W =
    G_air_part_W_per_K * (T_part - T_operative_part)
```

### Contact Media And Wet

Water immersion uses fixed normalized vertical bands on the existing five body
parts. Initial source defaults are `FEET [0.00,0.15]`, `LEGS [0.15,0.55]`,
`HANDS [0.40,0.70]`, `TORSO [0.55,0.85]`, and `HEAD [0.85,1.00]`. Hands overlap
legs/torso because their physical height is not their body ownership order.
Use the same formula for the existing water and lava fluid-height ratios. Then
apply the fixed precedence `lava > water > powder snow > air`:

```text
band_fraction(H_medium) =
    clamp((H_medium - part_lower) / (part_upper - part_lower), 0, 1)

f_lava_part = band_fraction(H_lava)
f_water_part = (1 - f_lava_part) * band_fraction(H_water)
f_powder_part =
    (1 - f_lava_part - f_water_part) * (is_in_powder_snow ? 1 : 0)
f_air_part = 1 - f_lava_part - f_water_part - f_powder_part
```

For each occupied medium `m`, form one conductance and boundary:

```text
h_water_effective = h_water * (1 - water_resistance_part)
h_powder_effective = h_powder_snow * (1 - water_resistance_part)
h_lava_effective = h_lava * (1 - radiant_heat_proof_part)

R_medium_path_part =
    tissue_resistance_part
  + clothing_resistance_part
  + 1 / h_medium_effective

G_medium_part_W_per_K =
    area_part_m2 * f_medium_part / R_medium_path_part
```

An effective coefficient of zero omits that path instead of dividing by zero.
Water uses the same environmental cut and configured liquid range; lava uses a
`1000 C` local-boundary default. Powder snow uses `-30 C`, its own explicit
`W/(m2*K)` coefficient, and no Wet refresh. Calibrate the powder coefficient so
the unprotected fixed fixture approximates the current `2x` cooling response;
the production equation never multiplies a Celsius delta by `2` or `6`.

The existing `FHMobEffects.WET` restores its historical role as a binary extra
heat-transfer path. Keep the current refresh contract unchanged: water applies
the configured `wetEffectDuration`, worn armor applies the existing
`wetClothesDurationMultiplier`, and Sauna remains an existing producer. Do not
derive a continuous value from remaining duration.

For the non-immersed share of each part:

```text
wet_active = has(FHMobEffects.WET) ? 1 : 0

G_wet_requested_part_W_per_K =
    area_part_m2
  * f_air_part
  * wet_active
  * wet_exchange_coefficient_W_per_m2K
  * (1 - water_resistance_part)

Q_wet_requested_part_W =
    G_wet_requested_part_W_per_K * (T_part - T_operative_part)
```

This is the unit-bearing replacement for both historical formulas
`+0.2*(1-fluidResistance)` and `+10*(1-fluidResistance)`. The historical
coefficients are calibration evidence, not production defaults. Positive Wet
power increases cooling; negative Wet power increases warming, preserving the
old rule that Wet accelerates exchange rather than acting as a hidden cold-only
Celsius penalty.

Only the positive, cooling share competes with sweating for the environment's
evaporative capacity:

```text
Q_evap_request_W =
    sum(max(Q_wet_requested_part_W, 0)) + Q_sweat_requested_W

evap_scale =
    Q_evap_request_W <= 0
      ? 0
      : min(1, Q_evap_capacity_W / Q_evap_request_W)

wet_conductance_scale =
    Q_wet_requested_part_W <= 0 ? 1 : evap_scale

G_wet_applied_part_W_per_K =
    G_wet_requested_part_W_per_K * wet_conductance_scale

Q_wet_applied_part_W =
    G_wet_applied_part_W_per_K * (T_part - T_operative_part)

Q_sweat_applied_W = Q_sweat_requested_W * evap_scale
```

Calculate `Q_evap_capacity_W` once per player update from the already sampled
Air temperature, humidity, gated local wind, and exposed body area. Wet exchange never
consumes player hydration; only the applied sweat share does. This adds one
player-level sum/scale and fixed part arithmetic, with no allocation or state.
After scaling, express the applied Wet term as the same conductance to
`T_operative_part` used by the stable integration below.

On leaving water, `f_water_part` becomes zero within the next model update,
so strong water exchange ends immediately while the still-present Wet effect
continues accelerated air exchange for its existing duration. Per-part or
per-item drying, water inventories, fabric absorption simulation, new moisture
packets, hot springs, and a separate hot-water system are outside this plan.

Fire is a fixed local active-power input, not a registered Thermal source or a
temperature overwrite:

```text
P_on_fire_part_W =
    (is_on_fire && !is_in_lava ? on_fire_heat_power_W : 0)
  * normalized_area_fraction_part
  * (1 - radiant_heat_proof_part)
```

Calibrate `on_fire_heat_power_W` against the measured current part-warming curve
rather than preserving the broken `300 C` unit conversion. Lava contact owns
its boundary path while immersed. If vanilla leaves the entity burning after
lava exit, the on-fire power continues until `isOnFire` clears. Vanilla retains
direct fire/lava damage and ignition ownership. The climate model retains body
hyperthermia, but suppresses its separate probabilistic direct-burn damage while
vanilla fire or lava damage is active, preventing duplicate injury from one
contact event. No burn-dose state is added.

### Stable One-Step Integration

Treat every air, Wet, water, lava, and powder-snow path as a conductance `G_i`
to a boundary `T_i`. Coefficients and active powers are constant over the sampled
one-second cut:

```text
G_part = sum(G_i)

P_active_part_W =
    allocated_basal_and_movement
  + allocated_shivering
  - allocated_sweating
  + equipment_power_part_W
  + P_on_fire_part_W

if G_part > 0:
  T_passive_equilibrium_part = sum(G_i * T_i) / G_part
  alpha = -expm1(-G_part * physiological_seconds / C_part)
  T_forced_equilibrium =
      T_passive_equilibrium_part + P_active_part_W / G_part
  delta_energy_part_J =
      C_part * (T_forced_equilibrium - T_part) * alpha
else:
  delta_energy_part_J = P_active_part_W * physiological_seconds
```

This is the exact closed-form update for the sampled linear balance. Passive
exchange alone cannot cross its conductance-weighted equilibrium, including in
cold water or lava; an active body-power input may move the forced equilibrium
as physics requires. It uses five `expm1` calls per player per second, no runtime
substeps, no arbitrary body-temperature clamp, no allocation, and no extra
retained state. Direct food/drink joules are applied exactly once as their
declared event energy, outside this continuous-power solve.

Internal body transfer remains conservative over the fixed pair order
torso-head, torso-legs, torso-hands, and legs-feet. For each pair, calculate from
the temperatures after the external update and clamp transfer to the energy
required to equalize that pair:

```text
E_request_ab_J =
    conductance_ab_W_per_K * (T_a - T_b) * physiological_seconds
E_equalize_ab_J = (T_a - T_b) / (1 / C_a + 1 / C_b)
E_transfer_ab_J =
    sign(E_request_ab_J)
  * min(abs(E_request_ab_J), abs(E_equalize_ab_J))

energy_a -= E_transfer_ab_J
energy_b += E_transfer_ab_J
```

Each pair uses the energies produced by the preceding pair in that fixed order.
It cannot cross pair equilibrium, and the equal/opposite writes conserve total
body energy without per-part histories or a solver.

### Metabolism And Regulation

All continuous body gains and losses are power. The stable solve above owns
their simultaneous integration:

```text
P_metabolic_W = basal + movement
P_regulation_W = shivering - Q_sweat_applied_W
P_equipment_W = sum of active wearable/device heat
physiological_seconds = elapsed_seconds * playerThermalTimeScale
```

- difficulty scales available thermoregulation power and resource cost, not
  environmental temperature or passive physical heat transfer;
- the existing `easy/normal/hard/hardcore` values initially map to active
  thermoregulation multipliers `2/1/0.5/0`; basal metabolism remains nonzero;
- shivering consumes food through one documented joules-per-exhaustion
  conversion;
- sweating/evaporation consumes water through one documented
  joules-per-water-exhaustion conversion;
- walking and sprinting add configured metabolic watts rather than Celsius;
- every continuous body power, part-energy transfer, wearable fuel transfer,
  and food/water regulation cost uses the same `physiological_seconds`; the
  scale cannot accelerate only one side of the balance;
- server lag uses actual elapsed ticks but remains bounded to the established
  one-second player cadence; no skipped interval is silently counted twice.

### Physiological Effects

- Every part keeps the same normal reference `37 C`; hands and feet are not
  assigned a different normal temperature. Preserve the current offset bands by
  translating them directly to absolute part temperature:

| Absolute controlling-part temperature | Migrated state |
|---:|---|
| `[36,38] C` | safe; do not add or refresh a temperature effect |
| `[35,36) C` | cold amplifier `0` |
| `[34,35) C` | cold amplifier `1` |
| `[32,34) C` | cold amplifier `2`; torso hypothermia damage begins |
| `<32 C` | torso amplifier `floor(35 - T_part)`; peripheral amplifier capped at `3` |
| `(38,39] C` | hot amplifier `0` |
| `(39,40] C` | hot amplifier `1` |
| `(40,42] C` | hot amplifier `2`; torso hyperthermia damage begins |
| `>42 C` | torso amplifier `floor(T_part - 39)`; peripheral amplifier capped at `3` |

- Torso, not the HUD equivalent temperature, drives hypothermia/hyperthermia;
  head drives confusion, hands drive digging slowdown, and the more severe of
  legs/feet drives movement slowdown.
- Preserve the existing `100 tick` effect duration and one-second evaluation.
  Returning to `[36,38] C` stops refresh; the existing effect may remain for at
  most about `5 s`, which is the current release hold and needs no new history.
  A changed severity takes effect through the same expiry/reapplication path.
- For acceptance measurements, `ready-to-travel` means every controlling part
  has returned to `[36,38] C` and its old `100 tick` penalty has expired.
  `Severe-state exit` means torso has crossed back to `>=34 C` from cold or
  `<=40 C` from heat and the old amplifier-`2+` effect has expired; exact
  normalization to `37 C` is not required.
- Existing getters continue exposing each part through absolute Celsius or a
  clearly named derived severity during migration; research hand checks retain
  their gameplay meaning.
- Outside active vanilla fire/lava damage, direct burns retain the hottest
  transient part feeling and existing probabilistic damage path. Do not add a
  persisted heat-dose reservoir.
- Preserve current special-state behavior exactly: `INSULATION` freezes all
  five body energies while environment/HUD sampling continues, so adverse
  effects from an already unsafe frozen state may continue. Creative,
  spectator, and invulnerable players also freeze body energies and continue
  environment/HUD sampling, but temperature effects and climate direct damage
  are not added or refreshed. These states do not silently reset body energy.

## Clothing And Equipment Contract

### Static Clothing

Introduce canonical `ArmorTempData` fields:

| Field | Unit/range | Meaning |
|---|---:|---|
| `thermal_resistance` | `m2*K/W`, nonnegative | dry clothing resistance |
| `radiant_heat_proof` | `[0,1]` | reduction of absorbed direct radiant flux |
| `wind_proof` | `[0,1]` | reduction of forced-air penetration |
| `water_resistance` | `[0,1]` | reduction of water/powder contact and binary Wet extra exchange; does not alter Wet duration |

Keep legacy codec aliases during one data migration. The existing `factor`
values cannot be called scientific units. Initial compatibility conversion is
centralized and monotonic (`legacy factor 500` starts near `0.10 m2*K/W`), then
all generated recipes are rewritten to explicit resistance after scenario
calibration. Do not retain both values as parallel runtime authorities.

Layer ordering remains relevant. Inner layers dominate retained insulation;
outer layers dominate wind and water resistance. Production aggregation must be
a fixed direct loop over the existing small slot set with primitive accumulators.
Do not allocate `List<ClothData>` or add a cache/invalidation system unless JFR
shows the one-second direct loop is material.

Before changing generated recipes or any pack override, inspect the companion
repository's `AGENTS.md` if present and search both repositories for
`frostedheart:armor_temp`.

### Dynamic Heating Equipment

Replace `BodyHeatingCapability.tickHeating(..., HeatingDeviceContext)` with an
explicit power contract, for example a caller-owned sink receiving part and
`powerW`. Migrate every implementation atomically:

- `SteamBottleItem`;
- `HeaterVestItem`;
- `CoalHandStove`;
- `MushroomBed`;
- `HeatingPadItem`;
- `OxygenCandleItem`;
- any Curios or downstream implementation found by the final caller census.

Device body contribution is explicit watts. Existing arbitrary fuel, durability,
and stored-heat units remain a one-way gameplay resource rather than a second
thermodynamic energy model: they are consumed by real elapsed seconds so
`temperatureUpdateIntervalTicks` and physiological gameplay acceleration cannot
change device duration. Tooltip min/max values change from temperature additions
to power and expected duration. No compatibility adapter may continue adding
Celsius after the production switch.

### Food And Drinks

Replace direct body-temperature mutation with explicit energy delivery:

```text
ingestedEnergyJ = declared food/drink thermal energy
```

The first implementation may apply the energy immediately at the one-second
body update, provided `DeltaT = E/C` is used exactly once. Hot/cold/frozen item
state and eating restrictions remain, but `DEFAULT_HOT_FOOD_HEAT` and
`DEFAULT_COLD_FOOD_HEAT` become energy defaults with unit-bearing names.

## Persistence And Migration

Add a player thermal schema version and make migration idempotent.

For an old save:

1. preserve clothing inventories and difficulty;
2. convert each bounded legacy part-temperature offset to absolute Celsius and
   then to that same part's `bodyEnergyOffsetJ`; preserve all five part
   differences rather than collapsing them;
3. derive aggregate core temperature from the migrated head/torso/legs values;
   never migrate stale `feelTemp` as physiological state;
4. discard `blockTemperature`, `wind_strengh`, `envtemperature`, and
   `feeltemperature` as transient observations;
5. ignore legacy part `feel_temp` after migration while preserving item data;
6. write only the new schema on the next save.

Migration must not heal or kill a player merely by loading. Clamp only the
legacy input used for conversion to the representable physiological domain and
record no permanent compatibility mode.

Death/clone semantics remain explicit:

- ordinary clone preserves clothing inventory and difficulty;
- configured death reset sets all five part energies to neutral;
- respawn and dimension change force one fresh environment sample and sync;
- stale pre-death client packets cannot overwrite the new server state.

## Networking And Client Presentation

Replace per-tick NBT body sync with one compact versioned player thermal packet.
The packet is sent:

- after the staggered one-second model update when a quantized value changes;
- on login, respawn, dimension change, and capability reset;
- as a low-frequency heartbeat only if required to recover client UI state.

Initial payload target:

```text
equivalent environment temperature   signed short, 0.1 C
core temperature                     signed short, 0.01 C
status flags                         byte
```

The fixed payload is `6 B` before channel framing, versus an allocated
CompoundTag every tick. The client displays the latest synchronized sample
through the existing HUD presentation path and never feeds a value back to
gameplay. Additional interpolation, prediction, or smoothing work is outside
this plan.

`FHBodyDataSyncPacket` may keep its registry ID for protocol continuity within
the mod if its encoding is replaced atomically. Do not maintain simultaneous NBT
and binary production packets.

## HUD And Diagnostics

- `FrostedHud.renderTemperature` draws the synchronized environmental
  equivalent temperature as the number already placed directly on the orb.
- Orb color uses the same environmental equivalent Celsius value and the
  existing cold-to-hot texture thresholds. Two players in the same `-20 C`
  environment therefore see the same number and orb band. Clothing, Wet,
  difficulty, and body state still affect the body calculation and effects, but
  do not change this environmental HUD presentation.
- Do not add an up/down trend arrow or reserve new HUD space. The orb's existing
  color area is the warming/cooling indicator.
- The existing body bar consumes aggregate core state as it does today and
  remains the accumulated danger indicator. It does not derive severity from
  the orb number or color.
- `TemperatureProbe` continues to report physical Air temperature.
- `ThermometerItem` reports absolute core temperature.
- `TemperatureCommand` reports named values with units: Air `C`, equivalent
  environment `C`, direct flux `W/m2`, outdoor and applied local wind `m/s`,
  `canSeeSky`, net body power `W`, core temperature `C`, all five part
  temperatures, and clothing resistance `m2*K/W`.
- Forecast temperature remains world climate/air forecast, not body state or
  player-specific equivalent temperature.

## Package And Ownership Changes

Keep the class surface small:

```text
content/climate/player/
  PlayerThermalModel.java          pure formulas and one-second integration
  PlayerThermalParameters.java     immutable unit-bearing parameter snapshot
  PlayerTemperatureData.java       persistence, clothing inventory, derived accessors
  TemperatureUpdate.java           Minecraft orchestration and effects
  BodyHeatingCapability.java       explicit device-power contract
```

`PlayerThermalModel` must not reference `ServerPlayer`, `Level`, capabilities,
NBT, packets, HUD classes, or registries. Tests call the same pure production
formula used by gameplay. Production uses caller-owned primitive scratch or
primitive arguments; test convenience objects do not justify per-player runtime
allocation.

Migrate existing ownership in place:

- keep `BodyPartData`, its five identities, clothing inventories, effects, and
  public gameplay accessors; replace only the internal thermal value with
  unit-bearing energy and derive absolute temperatures;
- keep `HeatingDeviceContext` as the existing extension boundary, but change its
  accumulated value from Celsius to explicit part power;
- keep `PartClothData` as the existing aggregation result, but give its fields
  explicit resistance/wind/water/radiation semantics;
- stop consuming stale `blockTemp`, `windStrengh`, and player-local arbitrary
  weather Celsius without deleting the completed surrounding-simulator source;
- migrate ambiguous config keys through `ConfigMigrationSupport`; physical
  cleanup of unused aliases is a separate human-approved task after the new
  model is proven;
- retain `SurroundingTemperatureSimulator` and `TemperatureThreadingPool.java`;
  the latter remains disabled according to the existing explicit requirement.

## Configuration And Defaults

Create one immutable `PlayerThermalParameters` snapshot at server startup and
refresh it only through the existing config/reload lifecycle. Static Block
radiation parameters belong to the existing immutable
`MinecraftThermalProfiles`/engine-generation snapshot, not the player snapshot;
read them once before the first dimension runtime and apply changes only after a
server restart. Do not add a radiation hot-reload/rebuild path. Parameter defaults
must be centralized; configuration mirrors them rather than defining a second
default set.

Required parameter groups:

- human reference: core temperature, reference skin temperature, surface area,
  core heat capacity, tissue resistance;
- environment transfer: long-wave coefficient, reference air velocity,
  wind-scale-to-`m/s`, radiation absorptivity, liquid-water coefficient and
  temperature bounds;
- optional world radiation: startup-only enable flag, shared `1000 C` lava
  radiation temperature, `effectiveLavaEmissivity`, radiation reference
  temperature, and ordinary-fire radiant power; Campfire uses none of these
  parameters;
- local contact exposure: lava-body contact coefficient, `-30 C` powder-snow
  boundary and coefficient, and `onFireHeatPowerW`; the contact parameters do
  not create Thermal sources, indexes, scans, or rays;
- Wet: preserve the existing `wetEffectDuration` and
  `wetClothesDurationMultiplier`, add one binary
  `wetExchangeCoefficientWPerM2K`, and define the player-level evaporation
  capacity inputs shared by positive Wet cooling and sweating;
- metabolism: basal, walking, sprinting, shivering, sweating powers;
- gameplay time: one explicit dimensionless thermal time scale;
- resources: joules per food/water exhaustion unit;
- part effects: the migrated absolute-temperature/feeling thresholds and the
  existing `100 tick` release behavior for all five consequences;
- display: Celsius comfort bands and network quantization.

There is no hot-water block state, per-fluid temperature, hot-spring registry,
or hot-water configuration group. The liquid-water bounds only derive the one
local boundary from the already sampled Air value.

Deprecate and migrate these ambiguous keys rather than reading them in the new
model: `temperatureChangeRate`, `heatExchangeTimeConstant`,
`heatExchangeTempConstant`, `minBodyTempChange`, `maxBodyTempChange`, and
player-only snow/blizzard temperature modifiers. Replace the ambiguous Celsius
`onFireTempModifier` with `onFireHeatPowerW`; use its measured legacy response
only as calibration evidence. `temperatureUpdateIntervalTicks` remains and
defaults to `20`.

## Performance And Memory Contract

- Preserve stable UUID staggering across the 20-tick update interval.
- Perform exactly one Air query and one radiation query per player update.
- Perform exactly one `canSeeSky` test at the player eye block per update; it
  must not request a Page, trace a ray, scan blocks, allocate, or retain state.
- Body work is O(5 body parts) with no collection traversal beyond equipped
  layers and no per-update object allocation.
- Compute wind coefficients once per player update, not per part.
- Do not move player capability mutation or radiation DDA off the level thread.
- Do not add production counters, probes, debug collections, or test callbacks.
- Player/body/radiation queries never create or retain a thermal Page. Air lookup
  remains strictly `live publication -> dormant half-life -> natural`.
- New retained primitive state must remain below `64 B/player`, excluding the
  existing clothing inventories. No new per-player map, queue, executor, or
  radiation cache is permitted.
- The orb deadband/hold is client presentation state only and uses fixed
  primitive fields for the local player; it adds no server-side per-player
  state, capability field, NBT, collection, or packet field.
- Contact/Wet handling adds zero persistent player bytes: it consumes existing
  water/lava fluid heights, `isInPowderSnow`, `isOnFire`, and the existing Wet
  effect, then computes five contact fractions as loop-local primitives. It adds
  no moisture packet and registers no Thermal source.
- Optional static Block radiation adds no `PhysicalSourceSpatialIndex` descriptor,
  `ThermalSourceLedger` binding, material pole, Air operation, dormant support,
  or per-block runtime state. It stores at most one packed `8 B` value per
  emitting Brick plus one sparse emitting-section entry and array header;
  internal lava emits none. PageEntry, Page publication, thermal signatures, and
  worker state add no fields; only a covered palette-positive section may attach
  the existing mutation owner.
- A fixed-profile change or lava neighbor-shape callback performs one
  coalesced owner-Brick emitter update and no arena,
  solver-fragment, material-edge, source-rebind, or neighbor-fragment work.
- Physical-source discovery runs first with its unchanged `64`-visit behavior.
  Published emitters use only remaining visits, then share the existing top `8`
  and `24`-ray cap. Static rays bypass the witness cache; no candidate/ray/cache
  backing grows.
- Keep the current radiation reservation and `128` simultaneous receiver-cache
  bound, but remove entries explicitly on logout and dimension exit. A capacity
  increase remains a measured follow-up, not a speculative change.
- Sync at most once per completed model update plus lifecycle events, never once
  per player tick.

For `100` players at one update per second, the new body model executes only
`500` part calculations and `500` scalar `expm1` calls per second. Radiation
retrace remains the dominant physical-source worst-case cost (`6144` DDA
steps/player/update before witness reuse). Static coverage performs at most 800
section map/palette checks/s total and zero stable BlockState capture. Enabled
Brick emitters use one eye ray each; top `8` and the global `24`-ray cap stay
unchanged, while static rays retain no witness. The model must not add another cache,
receiver cache, source ledger entry, per-block source registration, periodic
world scan, or player-query block scan for fire/lava/powder snow.

## Playability Contract

The body model is physically structured but intentionally forgiving and
legible. It must create decisions, not continuous maintenance work.

- Environmental equivalent temperature reacts within the next model update
  (`<= 1 s` at the default cadence) to wind, shelter, source visibility, and
  extinguishing, so player actions have immediate visible feedback.
- Core temperature changes slowly. The first adverse state is the orb's cold or
  hot environmental band, not damage or a severe movement penalty.
- Every harmful progression has the existing `100 tick` release hold and a
  recovery path. Crossing one quantization boundary cannot add/remove an effect
  every model update.
- A visible Campfire at useful distance confirms effective warmth within `1 s`,
  changes exposed extremities to an improving trend within `5 s`, and produces
  an unmistakable body-warming indication within `15 s`.
- A dry player with only mild, short-duration cold exposure should recover its
  ordinary ready-to-travel state after about `30..60 s` at that Campfire. The
  player must not wait for exact `37 C` core normalization before ordinary cold
  penalties clear.
- Severe hypothermia remains consequential: reaching a visible Campfire starts
  improvement immediately, but leaving the severe state targets `2..4 min` and
  full core normalization may take longer. A Campfire cannot instantly erase a
  long exposure.
- Fast feedback is derived from the orb's environmental equivalent number and
  color plus the existing five part-energy states. Do not add a second
  fictitious comfort temperature, recovery reservoir, extra arrow, or
  unconditional fire bonus to meet these timings.
- Thermoregulation consumes food/water only while producing nonzero regulation
  power and is capped. Ordinary safe play does not create constant hidden drain.
- Movement provides useful metabolic heat but cannot make sprinting the dominant
  permanent heating strategy.
- Clothing comparisons expose simple insulation, wind, water, and radiant
  protection values; players are not expected to reason about watts or tissue
  resistance during ordinary play.
- Typical cold is survivable long enough to diagnose and respond. Extreme cold,
  wetness, and wind remain dangerous because they defeat ordinary equipment, not
  because the model applies an unexplained instant penalty.

Initial gameplay calibration targets, measured in real play time at normal
difficulty, are:

The fixed `useful distance` Campfire fixture places the player center `1.5 m`
from a normal lit Campfire with direct line of sight, calm `-15 C` exterior Air,
dry starter layered clothing, and no other heat source. Mild and severe recovery
fixtures differ only in their declared initial body-energy state.

| Scenario | Target experience |
|---|---|
| naked, dry, calm `-15 C` | HUD cooling warning on the next update; mild hypothermia in about `45..60 s`; stronger tiers only after continued exposure |
| starter layered clothing, dry, calm `-15 C` | clear cooling trend but at least `8..12 min` before damage, allowing an expedition/shelter response |
| good winter clothing, dry, calm `-15 C` | near-stable core; wind or wetness still matters |
| starter clothing in exposed strong blizzard at `-15 C` Air | extremity effect in `45..90 s`; torso below `36 C` in `2..4 min`; torso damage tier below `34 C` in `5..8 min` |
| full unprotected immersion in `0 C` water | hands/feet first effect in `45..90 s`; torso below `36 C` in `60..120 s`; torso below `34 C` in `2.5..4 min` |
| visible Campfire at useful distance | HUD response `<= 1 s`; exposed extremity trend improves `<= 5 s`; unmistakable warming `<= 15 s`; mild short exposure clears to ready-to-travel in `30..60 s`; severe-state exit in `2..4 min`; no instant core reset |
| exposed lava without contact | one/four/eight-block fixtures have strictly decreasing optional direct flux while physical Air is unchanged; visible exposure is hotter than stone-wall occlusion; one-block exposure warns on the next player update and becomes dangerous under sustained exposure; four-block exposure may provide visible warmth without climate direct-burn damage during the first minute |
| ordinary open fire without contact | contributes only its configured optional direct radiation; containing/adjacent Air and Campfire readings remain unchanged |
| heated shelter near `18..22 C` | stable comfort and reliable gradual recovery |
| `-40 C` and below | advanced-equipment challenge; ordinary clothing buys time but is not indefinitely stable |
| near `-80 C` | short-duration endgame exposure with unmistakable warning and escape window |

These are balance acceptance targets, not replacements for the heat equations.
Tune explicit powers, resistances, thresholds, and the one time scale against
them; do not add hidden difficulty multipliers to environmental temperature.
The water fixture uses `H_water=1`, no clothing, and normal difficulty. The
strong-blizzard fixture uses the production maximum wind mapping (`100` to
`19.444 m/s`), direct sky/weather exposure, dry starter clothing, and no heat
source. These windows are measured outcomes, not hard-coded gameplay timers.

Before selecting defaults, record deterministic old-model on-fire, lava, and
powder-snow part curves at `1`, `5`, `15`, and `30 s` where vanilla survival
permits. Fit the new local power/coefficient defaults to preserve useful old
response timing and ordering within a declared calibration tolerance; do not
preserve the known `300/1000/-30 C` overwrite or `2x` arithmetic as a formula.
Vanilla fire/lava damage cadence and powder-snow freezing remain unchanged.
The nearby-lava windows apply only without contact; entering lava remains an
immediate vanilla hazard. They are calibration outcomes from radiant flux, the
body model, and existing damage thresholds, never timers, Air writes, or a
distance-based player bonus.

## Implementation Sequence

The body-model production switch is atomic. Intermediate implementation commits
may exist on the development branch, but no release or merged state may run old
and new body authorities in parallel. The upstream optional lava/fire radiation
work has one existing receiver authority and may land first with its own
tests; it does not require the body schema migration.

### Stage 0: Baseline And Contract Fixtures

1. Record current source anchors, NBT fixtures, armor recipe catalog, equipment
   implementations, food consumers, HUD values, packet cadence, and effect
   thresholds, including the exact absolute migration table above.
2. Add test-owned numerical scenario inputs for cold exterior, roofed calm
   interior under the same world wind, windy sky-exposed exterior,
   visible/blocked Campfire, wet clothing, immersion, movement, and all
   difficulty levels. Record the current Campfire curves at fixed
   distances as comparison evidence, including time to trend reversal, part
   response, penalty recovery, and core recovery. Record fixed shallow, partial,
   full, and post-exit water fixtures without treating the legacy `*6` output as
   compatibility truth. Record deterministic on-fire, lava, and powder-snow
   part curves without turning those contacts into Thermal sources. Record the
   current verified absence of nearby-lava/fire Air and radiation response, plus
   fixed one/four/eight-block visible/occluded ambient fixtures.
3. Freeze expected qualitative monotonic behavior and the approved Campfire,
   `0 C` full-immersion, and strong-blizzard response windows before selecting
   final balance coefficients. Do not freeze known mixed-unit output numbers as
   compatibility.

### Stage 1: Optional World Radiation And Pure Model

1. [x] Add `MinecraftStateThermalTable` with one dense tagged state-code array,
   sparse primitive extended semantics, and its nested startup-only static
   profile builder. Replace `ThermalSignatureRegistry` plus
   `ThermalSignatureCatalog` with one server-wide `ThermalSignatureTable` that
   interns geometry separately, derives contact patterns, compresses component
   ordinals by geometry, and supplies canonical immutable `Integer` uniform
   Brick payloads. Define Campfire `PRESENT/LIT` source bits and compare decoded
   source bytes independently from topology/radiation. Remove
   runtime resolver/reverse-map fallback, the BlockState phase map, and
   per-dimension catalog construction; phase application reads and decodes one
   BlockState. Preserve Page signature IDs and all topology output exactly. Do
   not add a BlockState field, runtime registry, hot reload, second dense state
   array, profile callback, or mixed-Brick interning path.
2. [x] Refactor `BlockRadiationIndex` to Page-independent covered-section state:
   `knownBrickMask`, `emitterMask`, compact emitters, one pending unknown-Brick
   map, and the two reusable primitive dirty maps. Keep the targeted lava-only
   `LiquidBlock.updateShape` mixin. Known own-profile/lava changes mark only
   their own Brick; unknown Bricks wait for first final-state capture.
3. [x] Make player sampling enumerate at most eight loaded sections, reject a
   palette-negative section with `maybeHas`, and queue only unknown in-range
   Bricks under one fixed per-tick cold-capture budget. Fuse this coverage work
   with known-emitter enumeration in one `ensureAndVisitNearby` section pass;
   full-known sections skip requested-mask construction. Capture 64 BlockStates
   per unknown Brick with reusable section-batch scratch; attach mutation ownership
   only to covered palette-positive sections. Remove every
   `MinecraftSignatureCapture`, Page admission/recapture, retry, retirement,
   Page lifecycle, `pages.handle()`, `pagesByChunk`, and `maximumPages`
   dependency from `BlockRadiationIndex`. Give covered sections one named
   independent limit in `MinecraftThermalInput` and its memory reservation.
   Clear section state on
   replacement and all chunk-owned radiation state on unload.
4. [x] Keep the exact-range nearby-provider contract in `RadiationService`, with
   physical sources discovered first and static emitters using only remaining
   visits under unchanged candidate/ray capacities. Remove `isEmpty()` and any
   standalone ensure call; invoke the fused provider exactly once whenever it is
   nonnull, including with empty index state or zero remaining visits. Visit
   exhaustion disables emitter submission but not coverage of later sections.
   Exact `STATIC_BLOCK_REVISION` selects one current-eye ray and bypasses
   `ReceiverCache`;
   create that cache lazily only when a physical candidate exists, and let
   physical sources retain their existing witnesses. Replace quarter-cell DDA
   with one allocation-free block DDA and add one `collectWitnesses` tracer
   argument: static false performs one loaded-only lookup per crossed section
   with no Page/revision/witness work; physical true preserves existing cache
   semantics. Cache the current section reference only for the trace and clear
   it in `finally`. Keep O(1) physical receiver removal on logout/dimension exit
   and do not register emitters in `PhysicalSourceSpatialIndex` or
   `ThermalSourceLedger`.
5. Add `PlayerThermalParameters` and pure `PlayerThermalModel`.
6. Implement convective coefficient, environmental equivalent temperature,
   the caller-provided binary `canSeeSky` wind gate, operative temperature,
   five-part clothing paths, metabolism, conservative clamped inter-part
   transfer, water/lava part-band contact, powder-snow contact, binary Wet
   exchange, local on-fire power, shared evaporation capacity, and closed-form
   one-step integration with explicit units.
7. Add deterministic unit tests over normal and extreme inputs.
8. Keep the production caller unchanged until state and all integrations are
   ready; do not add a runtime feature toggle.

### Stage 2: State, Persistence, And Packet

1. Migrate each existing `BodyPartData` temperature/feeling pair to one part
   energy while retaining all five identities, inventories, and accessors.
2. Add idempotent NBT migration and death/clone tests.
3. Replace NBT per-tick sync with compact post-update sync; keep existing client
   presentation behavior and add no new smoothing system.
4. Update admin command output to expose all model terms during development.

### Stage 3: Clothing, Equipment, And Food

1. Add canonical clothing resistance/wind/water/radiation fields and legacy
   conversion.
2. Migrate generated armor recipes and tooltips after cross-repository override
   inspection.
3. Convert every `BodyHeatingCapability` implementation to power and explicit
   fuel-energy consumption.
4. Convert food/drink temperature effects to joules.
5. Redirect all Celsius-add callers through the new power/energy semantics in
   the same production switch; do not remove their completed gameplay features.

### Stage 4: Gameplay Cutover

1. Make `TemperatureUpdate` collect one input cut and call only
   `PlayerThermalModel`.
2. Stop using player-only block/wind/weather legacy composition while retaining
   the existing source files and upstream systems.
3. Route derived core and all five part temperatures to the existing effects,
   research hand checks, overlays, food, tooltips, and body thermometer.
4. Route environmental equivalent temperature to both the number already drawn
   on the HUD orb and its existing cold-to-hot color area. Do not add a separate
   trend arrow or reserve new HUD space.
5. Keep `TemperatureProbe` on physical Air and forecast on climate/air data.
6. Preserve current `INSULATION`, creative, spectator, and invulnerable freeze
   and effect semantics. Keep vanilla direct fire/lava damage, suppress only the
   duplicate climate direct-burn path during those active contacts, and retain
   vanilla powder-snow freezing.
7. Verify that entity fire/lava/powder inputs are primitive local contact reads
   and do not mutate Air or radiation ownership. Optional non-contact lava/fire
   flux arrives only through the upstream sparse Brick-emitter index, never through
   an Air write or `TemperatureUpdate` source registration.
8. Keep existing feature/API ownership; mark obsolete internal values unused and
   schedule any later physical deletion only after human review of the proven
   caller census.

### Stage 5: Calibration And Documentation

1. Calibrate physics defaults first, then use only
   `playerThermalTimeScale` and explicit regulation/device powers to meet
   gameplay timing targets.
2. Validate ordinary-human comfort near `18..24 C`; treat `15 C` as cool,
   `-15 C` as the ordinary world challenge, and `-80 C` as advanced-equipment
   territory. Physiological adaptations modify regulation, not the HUD number.
3. Calibrate visible Campfire equivalent-temperature gain by distance while
   preserving the declared `1600 W` radiation and wall occlusion. At the fixed
   useful-distance fixture, verify the `<= 1 s`, `<= 5 s`, `<= 15 s`,
   `30..60 s`, and `2..4 min` response windows. Meet them through the existing
   five part-energy model, physiological time scale, effect thresholds, and
   body-state progression, never by fabricating extra source power or Celsius.
4. Calibrate full `0 C` immersion and exposed strong-blizzard curves to their
   declared windows. Fit `onFireHeatPowerW`, lava contact coefficient, and
   powder-snow contact coefficient to the Stage 0 curves where current behavior
   is useful, preserving vanilla direct-damage ownership.
5. Calibrate effective lava emissivity and ordinary-fire radiant power at the
   one/four/eight-block visible/occluded fixtures, including the one-block
   reaction/danger and four-block no-direct-burn windows. Preserve physical Air
   and Campfire Air, flux, source count, and response curves exactly.
6. Update all climate living documents, exact config/data anchors, commands,
   formulas, units, defaults, and the development diary.

## Validation Matrix

### Optional World-Radiation Tests

- the tagged state table returns the same static signature IDs as the current
  implementation, distinguishes every radiation-relevant lava, ordinary-fire,
  soul-fire, and Campfire source semantic, and leaves worker signature IDs
  bit-identical. Direct and extended codes decode exactly; equal semantics share
  a code; `Integer.MIN_VALUE` remains topology-relevant even against itself;
  maximum/gapped IDs index safely, and an unknown state remains sentinel/none
  rather than becoming Air through `Block.getId`;
- Campfire source flags encode exact `PRESENT` and `LIT` behavior. Placement,
  removal, lighting, and extinguishing refresh the physical source once; equal
  facing/visual transitions do not. Source refresh is decided by old/new byte
  inequality rather than code inequality or a nonzero test;
- the startup builder is the only owner of signature/geometry reverse maps.
  Frozen production state has no `idsBySignature`, `topologyClass`,
  `TopologyIdentity`, `signatureIdsByState`, `phaseProfileIds`, or runtime
  `StateStaticThermalResolver` call. Two dimensions and a replacement engine
  share the same immutable `ThermalSignatureTable` identity;
- geometry and exact-signature interning preserve every geometry, Air mask,
  material profile, contact pattern, topology-equivalence decision, and
  component ordinal. Zero/single-component lookup needs no 64-byte ordinal
  range; material variants of one multi-component geometry share one range, and
  neutral signatures still return contact pattern `0`;
- uniform `PageSignatures` Bricks reuse the exact canonical immutable `Integer`
  for their signature ID across Pages and replacements, including the unresolved
  sentinel. No uniform payload is a mutable array; nonuniform `char[64]`/`int[64]`
  payloads remain private and bit-identical, and changed-Page
  reads/publications remain O(1);
- phase ownership derives the material profile from the decoded signature,
  rejects non-`PHASE_RESERVOIR` profiles, and applies a valid request using one
  loaded chunk lookup and one BlockState read. Removing the phase BlockState map
  and capture fallback does not change ACK/lifecycle/recipe behavior;
- a palette-negative section repeats only the allocation-free `maybeHas` gate
  and never gains an index entry or mutation owner. A palette-positive request
  unions only in-range unknown Brick bits; two players requesting the same Brick
  still produce one pending bit and one capture;
- an initially empty index is still invoked and queues palette-positive unknown
  Bricks on the first receiver sample. `NearbySourceIndex.isEmpty()` and a
  standalone ensure call do not exist in production;
- when physical discovery consumes all `64` visits, the fused provider submits
  zero static candidates but still checks all at-most-eight nearby sections and
  queues their unknown Bricks. When visits remain, the same section pass also
  enumerates known emitters without a second section lookup;
- a test-owned loaded-section provider observes at most eight resolutions for
  one player update across empty, partial-known, full-known, and mixed-source
  fixtures. A full-known section changes no pending mask and still exposes its
  in-range emitters;
- the fixed cold-capture budget reads exactly 64 primary BlockStates per drained
  unknown Brick. Lava exposure may add at most 208 neighbor reads per Brick; a
  compiled-empty Brick sets `knownBrickMask` and is never rescanned;
  missing budget leaves the optional contribution absent without loading a chunk
  or creating a thermal Page;
- thermal Page admission, retry, retirement, worker restart, and dormant restore
  do not install, erase, or rebuild radiation state. Moving a player across a
  Page/section boundary does not remove already-covered lava/fire state;
- profile ID changes mark only their own known Brick. An unknown Brick ignores
  mutation and later captures final state. A non-radiating state change performs
  no dirty-map write, Block-index lookup, Page invalidation, allocation, or
  worker input;
- the targeted `LiquidBlock.updateShape` mixin resolves each liquid block type's
  surface classification once. Breaking or placing a direct lava neighbor marks
  that known lava Brick once; unknown lava waits for first capture. Standard
  updates produce no duplicate `neighborChanged` mark;
- source, falling, stepped, vertically stacked, side-exposed, bottom-exposed, and
  fully enclosed lava fixtures follow the declared `getOwnHeight` face-area
  formula without VoxelShape work. Exposed lava compiles one finite
  watt/centroid emitter per emitting Brick; fully internal lava emits none;
- section-batched capture reads in-Brick neighbors from scratch, same-section
  neighbors directly, and each required outer section at most once through a
  loaded-only lookup. It never calls `MinecraftPageManager`, `pages.handle()`,
  `pagesByChunk`, `MinecraftSignatureCapture`, or a chunk-loading getter;
- ordinary fire compiles its configured radiant watts and disappears on
  extinguish without changing any arena cell, solver fragment, Air temperature,
  dormant cut, infrared Page, or network output;
- the double dirty buffers accept marks while a detached 20-tick cut rebuilds,
  lose no known Brick bit, allocate no per-event object, and retain capacity by
  maximum simultaneous dirty known sections rather than event history;
- one cut dirtying one or all known Bricks in a section performs one merge and at
  most one target-array growth/copy. Bit-identical masks/values are a complete
  no-op; known-empty coverage remains known until chunk unload;
- section replacement clears known/emitter/pending/dirty state for that section.
  Chunk unload clears all such state and optional mutation owners for the chunk;
  neighbor availability touches only already-known boundary Bricks;
- published lava flux follows area and inverse-square ordering at the fixed
  one/four/eight-block fixtures, is absent beyond the fixed Block range, and
  stone occlusion removes the direct term;
- a test-owned quarter-grid reference and the production block DDA return the
  same visible/blocked/unresolved result for axis, edge, corner-tie, section-
  crossing, and seeded random endpoint/occupancy fixtures. Physical mode records
  the same ordered section witnesses; the old quarter traversal is not retained
  in production;
- one trace performs at most one loaded-only section lookup per crossed section
  and then reads BlockStates directly from that section. Static mode invokes no
  Page manager, mutation-owner attach, revision lookup, or witness-array write;
  physical mode retains the existing revision/witness behavior;
- physical-source-only discovery, witness reuse, and results are bit-identical.
  In mixed scenes physical sources consume visits first, static emitters use the
  remainder, and both obey the existing top `8` and `24`-ray cap;
- exact `STATIC_BLOCK_REVISION` equality selects one current-eye ray with no receiver
  cache lookup/write. Wall changes affect the next static trace directly; static
  membership/centroid/power changes do not bump source-section revisions;
- 100 staggered one-Hz receivers perform at most 800 section map/palette checks/s
  total. Stable source-free coverage performs zero BlockState captures; a
  Block-only top eight performs at most 800 short rays/s and 19,200 block-grid
  boundary advances/s under the conservative three-axis bound;
- logout/dimension exit performs no static-index cleanup. Existing O(1) receiver
  removal and same-dimension generation invalidation remain exact for physical
  Campfire/machine witnesses only;
- Campfire registration, `8,000 W` split, source watermark, Air result, direct
  flux, and witness reuse remain exactly unchanged in Campfire-only fixtures.

### Pure Numerical Tests

- reference wind, no radiation: equivalent temperature equals Air temperature;
- no-wind path has no legacy `13.12 + 0.6215*T` offset;
- identical world wind produces the configured outdoor speed when
  `canSeeSky=true` and exactly `0 m/s` applied wind when `canSeeSky=false`;
- colder Air monotonically increases body heat loss;
- increasing wind in cold Air monotonically lowers equivalent temperature;
- direct flux raises equivalent temperature linearly before any declared cap;
- blocked flux contributes exactly zero direct term;
- clothing does not change environmental equivalent temperature;
- changing clothing, Wet, metabolism, or equipment leaves the displayed
  environmental equivalent number and orb texture band bit-identical;
- clothing resistance monotonically reduces dry body heat loss;
- wind proof affects forced convection but not calm Air;
- water resistance reduces water/powder contact conductance and binary Wet
  exchange but does not change Wet duration or create heat;
- wetness cannot improve insulation;
- zero, partial, and full immersion produce exact exclusive
  air/water/lava/powder partitions; feet/legs/torso/head engagement follows the
  fixed vertical bands and the declared medium precedence;
- equal-temperature water removes more reference/body heat than air according
  to the declared coefficients, without a legacy Celsius multiplier;
- leaving water removes the water term within one model update without changing
  stored body energy; binary Wet exchange remains at full configured strength
  while the existing effect is present and becomes zero when it expires;
- powder snow uses the `-30 C` boundary and explicit coefficient without Wet;
- lava uses the `1000 C` boundary and fluid-height fractions without crossing
  the passive equilibrium; post-lava `isOnFire` continues only local fire power;
- on-fire power changes body/equivalent trend with no Air write, source
  registration, source-index mutation, or radiation query;
- device watts and food joules change core energy with correct sign;
- zero-active-power closed-form integration matches the analytic exponential
  and never crosses passive equilibrium; with active power it converges toward
  the declared forced equilibrium;
- each clamped internal pair transfer conserves energy exactly and never crosses
  pair equilibrium, including at the largest configured time scale;
- the fixed visible-Campfire timeline reaches immediate HUD feedback, extremity
  trend reversal, clear warming, mild recovery, and severe-state exit in their
  approved windows without resetting core energy;
- the full `0 C` immersion and exposed strong-blizzard fixtures meet every
  declared extremity, mild-torso, and damage-tier window without timers;
- all formulas remain finite across `-273..1000 C`, production wind, maximum
  source flux, contact fractions, and every configuration boundary;
- normal and first-sample paths use the same units; insulated, creative,
  spectator, and invulnerable paths preserve the declared freeze/effect
  semantics while environment sampling continues.

### Persistence And Network Tests

- old player NBT resets thermal energy to normal while preserving every
  clothing stack, its item NBT, and difficulty;
- stale block/wind/feel fields do not enter the new model;
- no moisture field is added to player NBT or the compact body packet; the
  existing Wet effect remains authoritative across save/load;
- old thermal offsets cannot create an immediate lethal state;
- save/load and clone preserve all five part-energy states exactly;
- death reset follows configuration;
- packet encode/decode preserves quantized values and flags;
- unchanged ticks do not send body packets;
- login/respawn/dimension change sends a complete fresh state;
- client packet decode/rendering cannot mutate server capability state.

### Forge GameTests

- exposed player observes natural fallback, then the accepted Page Air without a
  `37 C` jump;
- closed shelter and opened door produce the expected Air/equivalent ordering
  through Mesh Air transport; opening the door does not bypass a false
  `canSeeSky` result or create partial wind in the initial model;
- visible Campfire raises equivalent temperature within one model update and
  Air gradually;
- the fixed useful-distance Campfire scenario meets the approved `1 s`, `5 s`,
  `15 s`, `30..60 s`, and `2..4 min` player-response windows;
- stone wall removes direct radiation while retaining already heated Air;
- extinguishing Campfire removes direct radiation immediately and Air cools
  through the solver;
- wind changes equivalent temperature without mutating Page Air only when
  `canSeeSky` is true; placing an opaque roof makes applied wind exactly zero on
  the next player update, and removing it restores outdoor wind;
- dry/wet and low/high clothing resistance change body heat trend but not the
  HUD environmental number or orb texture band;
- shallow water affects feet before higher body bands; partial and full
  immersion use the declared area fractions and water boundary;
- leaving water removes strong water exchange within one model update while the
  existing Wet effect continues its full binary extra exchange until it expires;
- the full `0 C` immersion and strong-blizzard scenarios meet the approved
  timing windows;
- a player beside but outside lava observes unchanged published Air plus visible
  Brick-emitter flux; a stone wall removes the direct flux without changing Air;
- the nearby-lava fixture warns on the next player update, gives at least `3 s`
  before climate direct-burn damage at one block, becomes dangerous under
  sustained `10..20 s` exposure, and causes no direct-burn damage at four blocks
  during the first minute;
- entering partial/full lava adds the declared local body contact on top of the
  already sampled ambient Air/flux; the player contact calculation itself does
  not write Air, register a source, or publish a radiation emitter;
- on-fire, post-lava fire, and powder-snow contacts produce their declared local
  part curves without changing Thermal source count or source watermark;
- ordinary fire leaves Air unchanged and publishes only its configured optional
  Brick-emitter radiation; the new optional index does not touch Campfire code,
  registration, split, indexing, Air port, or radiation entry, and the existing
  Campfire physical-source path produces the same results;
- active vanilla fire/lava damage does not also invoke the climate probabilistic
  direct-burn path; body hyperthermia remains temperature-driven;
- equipment and food add declared power/energy exactly once;
- core and all existing part thresholds produce the intended effects without
  duplicate damage paths.

### Compatibility Tests

- every generated `frostedheart:armor_temp` recipe decodes and maps to the
  intended explicit resistance;
- every existing heating-device capability has one migrated production caller;
- `TemperatureProbe`, `ThermometerItem`, forecast, HUD, food, research hand
  checks, breath particles, overlays, and admin commands each consume the
  correct named value;
- no production caller remains for legacy effective-temperature mutation,
  `blockTemp`, `windStrengh`, per-part feeling, or direct Celsius food heat.

### Performance Validation

- compile and run the complete player/climate/thermal JUnit selection and Forge
  GameTests;
- run controlled JFR for `100` staggered stationary players with no sources,
  visible Campfires, blocked Campfires, movement, Wet, water, lava,
  powder-snow/on-fire local contact, and equipment;
- run separate receiver-covered fixtures for a flat exposed lava surface, fully
  internal lava, and coalesced ordinary-fire spread/extinguish; verify thermal
  Page count, solver operations, arena live/high-water state, source-ledger size,
  physical Air, dormant state, infrared payload, and network bytes remain
  unchanged while measuring the independent sparse index;
- compare repeated unrelated door/block churn against repeated direct-lava-face
  churn. The former must perform zero Block-index writes/rebuilds; the latter
  must merge to at most one rebuild per affected Brick per 20-tick cut;
- compare startup and retained heap for one and multiple dimensions before/after
  the tagged/shared tables. State-table memory must follow one dense int array
  plus special semantics, signature component memory must follow unique geometry
  rather than signatures times 64, and engine recreation must allocate no
  signature catalog/map;
- admit `3,200` uniform Pages and verify uniform signature payload identity is
  one immutable canonical `Integer` per signature ID rather than up to `204,800`
  length-one arrays. Mixed Brick payload identity must remain Page-local, every
  array must be length 64, and no runtime interning map may appear in allocation
  profiles;
- compare dense lava updates at an internal Brick boundary and each outer
  section face. Same-section neighbors use direct reads; each required outer
  section is resolved at most once per batch through a loaded-only lookup, and
  an unloaded neighbor remains opaque. A section with no boundary radiator
  performs zero neighbor lookup; all six scratch references clear after the
  batch without a halo allocation;
- compare one and all-64 dirty Brick cuts against independent 64-state Brick
  captures. They must
  publish identical masks/values, perform one section commit, retain target-array
  capacity, and leave array identity unchanged when final packed output is
  bit-identical;
- measure the pending map and both dirty buffers from one entry through the
  independent radiation-section limit; verify retained capacity follows peak
  simultaneous occupancy rather than event count or historical positions;
- compare the old algorithm in a test-owned quarter-grid reference against the
  production block DDA for identical results, then compare JFR trace-miss CPU.
  Static 8-block rays must remain within 24 block-boundary advances and one
  loaded-only lookup per crossed section, with zero Page/revision/witness/cache
  work; do not retain the reference traversal or add counters to production;
- use the existing test-owned nearby provider boundary to verify 100 one-Hz
  receivers perform at most 800 total section resolutions, including empty and
  zero-remaining-visit cases. No separate ensure/discovery pass may raise this
  to 1,600;
- compare update CPU, allocation rate, packet allocation, physical radiation
  cache reuse, static no-cache traces, and level-thread P95/P99 against the
  current build;
- run retained-heap checks after repeated login/logout and dimension changes;
- verify logout removes the current-dimension receiver and dimension change
  removes the old-dimension receiver by key in O(1); no new per-player
  collection, queued task, stale receiver, timer, cleanup traversal, or packet
  state remains;
- use external JFR/heap evidence only; do not add production instrumentation.

## Acceptance Criteria

The plan is complete only when all of the following are true:

1. The HUD number and the same orb's color use the absolute environmental
   equivalent Celsius value, never core body temperature or a `37 C`-relative
   offset. Existing orb texture bands remain in use; no separate trend arrow or
   extra HUD space is added.
2. Air, radiation, wind, clothing, wetness, metabolism, equipment, food, body
   energy, and display conversions each have one named unit and one owner.
3. Campfire convection and radiation retain their declared source split:
   radiation is emitted once as a read-only field, never injected into Air, and
   each player integrates one sampled intercepted flux per model update.
4. Existing five-part effects remain; every part derives from its own conserved
   energy state, and aggregate core temperature remains a derived weighted view.
5. Clothing and difficulty affect tolerance but not the environmental number.
6. Existing saves retain clothing, item NBT, and difficulty without importing
   old body/feel/environment offsets or adding a compatibility branch.
7. Temperature sync is bounded by the one-second update cadence and lifecycle
   events, not player ticks.
8. The steady player model is allocation-free O(5), performs one Air/radiation
   sample, and stays within the retained-memory budget.
9. Living documentation and tooltips describe current implemented units and
   behavior; `design/` remains untouched.
10. Functional tests, GameTests, JFR, heap checks, and real-save manual scenarios
    all pass before the production switch lands.
11. Existing five-part clothing, effects, research checks, heating devices,
    food interactions, probes, thermometers, and surrounding-simulator work
    remain available; the implementation changes their thermal units and
    internals without deleting completed gameplay.
12. A visible Campfire gives immediate actionable feedback and restores ordinary
    play after mild exposure without idling, while severe core recovery remains
    gradual; all timing comes from the one five-part energy model rather than an
    unrelated fire-specific temperature bonus.
13. Immersion is computed from the existing water-height ratio and five fixed
    body bands; leaving water ends the water boundary within one model update,
    while coarse post-exit wetness reuses the existing Wet effect with no new
    persisted moisture state or synchronization path.
14. Optional static Block radiation contributes at most one compact emitter per
    emitting Brick and never writes Air. It creates no per-block Thermal source,
    source-ledger binding, extra receiver cache, material pole, dormant support,
    periodic world scan, Page/SectionOwner field, or ordinary-geometry index
    lookup. Player sampling rejects palette-negative sections without retained
    state and queues only unknown in-range Bricks in positive sections. Each
    first capture reads 64 loaded BlockStates under a fixed budget; known-empty
    Bricks are not rescanned. One always-invoked fused provider call performs at
    most eight section checks total and queues coverage even when index state is
    empty or physical visits are exhausted; only emitter submission observes
    the remaining visit count. Page admission/retirement/restart never changes the
    index, and chunk unload owns cleanup. Only an own-profile change or lava's
    own `updateShape` callback schedules a known Brick. Static-ray mode requires
    exact `STATIC_BLOCK_REVISION` equality, uses the shared block DDA with
    `collectWitnesses=false`, performs no Page/revision/witness/cache work, and
    stays within 24 boundary advances at 8 blocks. Physical rays use the same
    DDA with existing witnesses; Campfire source refresh requires exact old/new
    source-flags inequality.
15. Passive contact cannot cross its weighted equilibrium in one update, and
    clamped internal transfer cannot cross pair equilibrium or change total body
    energy. This holds without runtime substeps.
16. The `0 C` full-immersion and exposed strong-blizzard fixtures meet their
    explicit warning/mild/damage windows; nearby lava meets its explicit
    one/four/eight-block response windows; contact fire/lava/powder defaults
    retain useful legacy curves and vanilla direct-damage ownership.
17. Entity contact remains a local primitive body input and never writes Air;
    optional nearby lava/fire warmth is read-only direct radiation. Campfire
    retains exactly its existing source registration, `8,000 W` split, Air path,
    radiation path, and response curve with no second contribution.
18. Initial local wind is exactly the outdoor `m/s` value when
    `ServerLevel.canSeeSky(player_eye_block)` is true and exactly `0 m/s` when
    false. The gate is sampled once per player update, ignores stale
    `windStrengh`, creates no Page/ray/scan/cache/state/packet, and does not
    simulate partial drafts through doors, windows, open walls, or caves.
19. One tagged BlockState table and one server-wide signature/geometry table are
    the only frozen thermal state authorities. Production retains no state or
    signature reverse map, topology-class array, phase BlockState map,
    per-dimension catalog, runtime static resolver fallback, shared mutable
    uniform array, or per-Page uniform payload duplication. Uniform Bricks use
    canonical immutable scalar IDs; geometry/material/component/topology and
    phase behavior remain bit-identical.
20. Logout and dimension exit remove the physical-source receiver cache entry
    from the dimension being left with one primitive-map removal. Static Block
    radiation retains no receiver state; no stale receiver relies on periodic
    cleanup or another retained player authority.

## Rejected Alternatives

- **HUD shows raw core body temperature:** it remains near `37 C` until danger
  and does not explain shelter or Campfire improvement.
- **HUD shows `T_air - 37`:** the value is an internal coordinate offset, not a
  temperature players can interpret.
- **HUD shows raw Air only:** it cannot represent wind or immediate direct
  radiant comfort.
- **Orb color encodes net body power:** it changes too abruptly for the current
  HUD and makes the same environment appear to change temperature when only
  clothing, activity, or contact power changed. Keep the existing environmental
  Celsius bands for both the number and the orb texture.
- **Add a warming/cooling arrow:** the existing number occupies the orb and the
  current HUD has no clear space for another indicator. Reusing orb color gives
  the required warning without layout growth.
- **Add radiation as an arbitrary Celsius bonus:** it discards `W/m2`, body area,
  absorptivity, and heat-transfer context.
- **Register every lava block, fire block, or burning player as a Thermal
  source:** entity contact already exposes O(1) state, while world lava/fire is
  cheaper as one optional radiation emitter aggregated by an existing Brick.
  Per-block registration adds source generations, indexing, ledger bindings,
  accumulators, and churn without improving receiver flux.
- **Attach a thermal int field to every BlockState singleton:** only a very small
  fraction of states need radiation or source semantics. One registry-ID-indexed
  int array has the same direct lookup without modifying every state object or
  charging unrelated states an object-layout field.
- **Keep parallel dense signature and radiation arrays:** radiation is sparse in
  the registered state domain. A tagged direct signature plus sparse extended
  semantics retains one-load ordinary lookup and halves the dense primitive
  memory.
- **Keep frozen signature reverse lookup, `topologyClass`, or one Catalog per
  dimension:** startup already interns exact signatures, and unsupported runtime
  states cannot become resolvable by repeating the same resolver. These
  structures duplicate identity and geometry without changing an answer.
- **Couple radiation capture to thermal Page admission:** it reuses an existing
  scan only when a Page happens to exist, but player-only lava/fire radiation is
  then absent in source-free areas. Repairing coverage with player Page leases
  would charge the worker, solver, dormant, and retirement paths for a read-only
  query. The domains therefore stay independent.
- **Index every loaded radiation-positive section:** it avoids receiver cold
  misses but scans and retains Nether farms, chunk-loader regions, and other
  loaded areas outside every player's 8-block range.
- **Scan the complete receiver sphere on every sample:** it minimizes retained
  state but repeats thousands of BlockState/exposure reads. Receiver-lazy
  known-Brick capture pays 64 reads once and records compiled-empty state.
- **Read raw palette storage:** use public `LevelChunkSection.maybeHas` only as
  the allocation-free negative gate. Coupling to palette container internals for
  spatial extraction adds version/format branches and still cannot replace the
  exact 64-position Brick capture.
- **Intern mixed Page-signature Bricks:** hashing 64-value arrays adds startup
  and mutation work plus an ownership table. Uniform payloads have the dominant
  repeated shape and can be shared by direct signature ID without hashing;
  mixed payloads remain private.
- **Share uniform `char[1]`/`int[1]` arrays:** arrays remain mutable even when a
  comment declares them immutable, so one accidental write could corrupt every
  Page sharing that payload. One canonical startup `Integer` per signature is
  smaller, truly immutable, and gives the common read path one direct scalar
  branch.
- **Run a loaded-chunk lookup for every Brick-boundary block:** most Brick
  boundaries remain inside the current `LevelChunkSection`. Direct reads plus
  one lazy six-face section-batch scratch resolve each actually required outer
  section at most once, without a block-state halo, per-batch object, Page owner
  index, or second section index.
- **Cache static Block DDA witnesses:** the source/centroid and occluding walls
  are mutable, so cache correctness needs revisions and lifecycle invalidation.
  At an 8-block range, retracing at most eight eye rays per one-Hz sample is
  cheaper and removes all static per-receiver state. Physical receiver caches
  still use exact logout/dimension lifecycle removal.
- **Separate `ensureNearby` and `visitNearby` calls:** both need the same bounded
  section cube, so separation doubles stable section resolution and lets the old
  `isEmpty`/remaining-visit gates starve first coverage. One fused call deletes
  the gate and second traversal while preserving physical-first candidate order.
- **Keep quarter-cell DDA for whole-block occlusion:** every four microcells map
  back to the same BlockState predicate, so the finer grid adds arithmetic but
  cannot observe more geometry. One block DDA is both simpler and has a fourfold
  lower conservative boundary-advance bound. Do not add a separate static DDA;
  one witness-mode boolean preserves the physical path without duplicating the
  algorithm.
- **Keep a nonzero emitter for every fully enclosed lava Brick and leave all
  exposure to DDA:** DDA is binary and candidate ranking occurs before tracing.
  A one-block opening could expose an entire Brick's volume power, while strong
  blocked lava could displace a weaker visible candidate. It also stores and
  visits internal emitters continuously. Exposed-face power plus a targeted lava
  neighbor callback gives the required area scaling without involving ordinary
  geometry mutations.
- **Make every geometry mutation search nearby surface radiators:** this couples
  door/mining traffic to an optional field and creates false-positive Brick
  rebuilds. Minecraft already notifies the directly adjacent lava block through
  `LiquidBlock.updateShape`; the radiator owns invalidation. Injecting
  `neighborChanged` too would duplicate the standard notification.
- **Overwrite player Air with `300 C`, `1000 C`, or `-30 C`:** it corrupts the
  HUD/environment authority and mixes a boundary or power with Air temperature;
  local fire power and explicit contact conductances preserve units.
- **Use unbounded explicit Euler or runtime substeps for stiff contact:** the
  first can overshoot water/lava boundaries and the second multiplies hot-path
  work; the sampled closed-form solve plus four clamped internal pairs is stable
  with fixed O(5) cost.
- **Persist moisture per body part or clothing stack:** it adds state, migration,
  synchronization, and drying rules without enough gameplay value; fixed
  part-specific immersion plus the existing player-wide Wet duration is the
  selected minimum sufficient model.
- **Use wind chill below a threshold and heat index above another:** piecewise
  indices have restricted domains and discontinuities in extreme gameplay.
- **Use full PMV/SET iteration:** it is an indoor steady-state comfort model,
  creates unnecessary complexity, and does not own body injury or extreme
  survival.
- **Add local wind rays or a Mesh airflow/exposure field in the initial
  implementation:** partial drafts improve spatial fidelity but add block
  traversal or per-cell state/propagation. The selected first version uses one
  binary `canSeeSky` gate; revisit partial airflow only with measured gameplay
  need and profiling evidence.
- **Keep old and new player models behind a runtime toggle:** it creates two
  authorities, doubles migration/testing work, and allows saves to alternate
  incompatible units.
- **Reuse `TemperatureThreadingPool`:** the model is tiny, main-thread state is
  authoritative, and radiation traversal already has the required bounded
  main-thread ownership.
- **Return player heat to the Air Mesh:** moving-source registration and
  bidirectional energy ownership add source churn and solver cost without enough
  player value; the selected model is explicitly one-way.

## Documentation Impact

Implementation must update:

- [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md)
  with the complete unit-bearing environment/body/HUD model;
- [`docs/climate/world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md)
  for the diurnal-ownership move into natural Air;
- [`docs/climate/heat-production-and-network.md`](../docs/climate/heat-production-and-network.md)
  for event-driven optional static Block radiation, exposed-area conversion,
  unchanged Air, and unchanged Campfire source split;
- [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md)
  for tagged state semantics, the shared signature/geometry table, canonical
  uniform Brick scalars, receiver-lazy known-Brick coverage, palette-negative
  rejection, section-batched loaded-only neighbor reads, targeted surface
  invalidation, double-buffered dirty Bricks, independent memory bounds,
  physical-first discovery with one always-invoked fused static coverage/visit
  pass, the shared section-cached block DDA, static no-witness tracing, and
  physical witness preservation;
- [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md)
  for NBT schema, migration, packet, cadence, receiver removal on logout/old
  dimension exit, and reload behavior;
- [`docs/climate/README.md`](../docs/climate/README.md) only if navigation or
  scope changes;
- a new timestamped [`diary/`](../diary/) entry with decisions, validation, and
  remaining calibration evidence.

## Outcome

First implementation pass completed the in-place player energy calculation,
five-part clothing/contact inputs, equipment watts, food joules, compact sync,
and split HUD contract. Old thermal values are deliberately discarded while
clothing stacks, item NBT, and difficulty are preserved. The existing Campfire
source/radiation path is unchanged.

The optional static Block-radiation implementation now follows the receiver-lazy
correction above. `MinecraftSignatureCapture`, Page admission/recapture/retry,
Page retirement, worker restart, and dormant restore have no radiation callback
or ownership. Covered palette-positive sections alone retain known/emitter
masks; one pending map drains at 64 Bricks per tick, and known mutations use the
two existing 20-tick dirty buffers.

The final target creates no Air boundary, crop heat, per-block source, ledger
entry, thermal Page/cell/pair, dormant state, infrared payload, network payload,
or per-player radiation state. Receiver-near unknown Bricks alone enter the
independent bounded index and remain shared until chunk unload. Static rays use
one block DDA with no Page/revision/witness/cache work; physical rays reuse that
algorithm with their existing witness behavior. Coverage scheduling and known
emitter enumeration share one at-most-eight-section call that runs even from an
empty index or after physical visits are exhausted.

All tagged lava states now share one compiled radiation profile. Each
`LiquidBlock` singleton retains its steady cached classification but compares a
profile reload epoch, so a tag/recipe reload reclassifies once. Physical DDA
witness revisions and static coverage both use the existing 3,200-section bound,
which covers the declared separated hundred-player workload without adding an
eviction index.

The user retained execution of build and gameplay validation. Controlled
JFR/heap work and final fire/lava calibration remain pending; no additional
architecture is required.
