# Heat Production And Network

- Status: `Transitional; material-body replacement is under integration validation`
- Last verified: `2026-09-16`
- Scope: physical Minecraft sources, worker energy integration, material/phase sinks, and the separate heat-network model
- Primary code anchors: `MinecraftPhysicalSourceProfile`, `PhysicalSourceSpatialIndex`, `WorkerPhysicalSourceBindings`, `AirMixingRegion`, `ThermalSourceBatch`, `ThermalSourceLedger`, `NodePowerAccumulatorArena`, `HeatEndpoint`, `HeatNetwork`

## Physical Sources

Physical sources are observed on the server thread and coalesced into the next
20-tick `ThermalSourceBatch`. A source has one packed BlockPos identity, a
lifecycle generation, an origin, an anchor/target, a profile, and immutable
ports. `PhysicalSourceSpatialIndex` indexes origin section/chunk and target
section, so changing one machine or campfire does not scan all sources.

When worker delay combines several normal cuts, the ledger settles the full
elapsed source energy and transport advances once with the same elapsed game
time. The normal cut remains one second. Source events keep their timestamps;
the coarse exchange step does not reconstruct intermediate geometry changes.

Each positive-share `AIR_FACE` port retains its exact target Brick. Its zero/nonzero source
reference transition updates one main-thread source-seed bit; multiple sources
in the same Brick share one lazy count. Source seeds are the only main-thread
cause of new thermal residency. The worker retains warmed Pages and advances
heat through loaded neighboring Bricks with changed absolute residency masks;
players and other queries do not hold Pages open.

| Profile | Rated power | Thermal port | Other ports |
|---|---:|---:|---|
| Campfire | `8,000 W` default | `80%` convection into the Air block above | `20%` direct radiation |
| Generator | `10,000 W * level` | `80%` exhaust convection | `20%` radiation loss |
| Fountain | `2,000 W * level` | `90%` convection | `10%` radiation loss |
| Radiator | `4,000 W * level` | `90%` convection | `10%` radiation loss |

Blocked AIR_FACE ports become declared loss; topology-unavailable ports become
degraded loss until their exact source Brick is compiled. Neither creates a
fake natural sink, accumulates pending energy, or forces a chunk load. Campfire block-state changes are
coalesced by position, and a lit/unlit change with identical thermal signature
updates the source state and the nearby Air coefficients, without replacing
temperature nodes merely because the source switched on or off.

For enabled positive-power sources, direct Air faces within 4 blocks of a
positive-share `AIR_FACE` outlet center use 4 times their original mixing
conductance. This is a local mixing coefficient, not additional source power or
a maximum heat-propagation distance. Overlap does not multiply the factor again.
Outside the region, and for material exchange and indirect ventilation routes,
the original coefficients apply. The source Section index supplies these positions
only when fragments are rebuilt; there is no per-tick distance-field update.
The implementation and activity thresholds are described in
[thermal runtime](thermal-runtime-architecture-and-optimization.md).

Campfire total power and radiation share are configured by
`FHConfig.COMMON.THERMAL_RUNTIME.campfirePowerW` and
`campfireRadiationShare`; convection receives exactly the remaining share.
Defaults are `8,000 W` and `0.20`. The main source index and worker binding
resolver use the same immutable configured profile.

All four Minecraft profiles retain a source only while `enabled && powerW > 0`.
Inactive/zero-power observations allocate no new slot and remove an existing
source through the normal cut. Zero-share convection ports create no Page seed;
a fully radiative campfire still retains its emitting source. Removing or moving
targets recomputes their dormant support after removing old reverse references,
preserving support from other sources and the worker's residual heat. Repeated
off/on transitions across cuts use normal unload/register generations.

## Static Fire And Lava Radiation

Ordinary fire and exposed lava optionally contribute direct player radiation
through `BlockRadiationIndex`. They are not physical sources: no power enters
Air, crops, soil, material cells, machines, dormant checkpoints, or
`ThermalSourceLedger`. A wall blocks the DDA ray, and removing line of sight
removes the contribution without residual room heat.

Fire power is `FHConfig.COMMON.THERMAL_RUNTIME.fireRadiantPowerW` (`1000 W`
default). Lava uses the configured `lavaRadiationTemperatureC` (`1000 C`),
`effectiveLavaEmissivity` (`0.01`), and
`radiationReferenceTemperatureC` (`20 C`) in the Stefan-Boltzmann exposed-area
calculation. `enableStaticBlockRadiation` defaults to true. These COMMON values
are read once at startup; all tagged lava states share one compiled profile and
Campfire does not use it. A reload epoch invalidates each `LiquidBlock`
singleton's cached lava classification on its next neighbor update.

Coverage is receiver-lazy and independent of Thermal Page admission. A player
sample palette-rejects ordinary loaded sections, queues unknown 4-cubed Bricks
under a shared 64-Brick-per-tick budget, and reuses known results across players
until chunk unload. Static rays are retraced from the player's current eye
position with one block-grid DDA and
retain no receiver witness or source revision.
Physical-source DDA witnesses use the same `3,200`-section bound as static
coverage, avoiding the earlier 1,024-section exhaustion under separated
hundred-player workloads.

## Worker Energy

This section's direct arena delivery describes the default scalar backend. With
`continuousAir = true`, `acceptAndRecord` writes W spans/J impulses into `CutLoadBuffer`,
and `AIR_STENCIL` binds one physical port to a spatial `AirLoadTable` entry. The coupled
solver accepts delivery before the ledger confirms it; material H/branch and real phase ACKs
retain their existing owners. See [Continuous Air Runtime](continuous-air-runtime.md#source-and-material-advancement).

`ThermalSourceLedger` integrates continuous power at authoritative event ticks.
For each source event it settles only the nodes whose power or binding changes;
the active accumulator list is drained once through the batch target tick. A
thermal-node binding is `(arena slot, lifecycle generation)`. Rebind settles the
old node before moving the port contribution, and unload releases every binding
before recycling the source and accumulator slots.

The ledger writes joules into the same `ThermalCellArena` used by the solver.
There is no second source timeline, historical generation table, or cumulative
diagnostic total. Source event order is the order in the immutable batch and is
validated against the worker cursor.

## Materials And Phase

Each resident material block owns one enthalpy H in `ThermalCellArena` and a
shared `MaterialThermalLaw`; temperature comes from that law. A partially filled
block owns no gap-Air temperature. Actual Air regions and material bodies use
the same arena and conservative pair exchange. `AirRouteCompiler` compiles
ventilated geometry into region references and finite-conductance connections.
An ordinary Brick has at most 64 Air-region plus material nodes; full Air uses one.

`MaterialEdgeCompiler` aggregates each undirected contact once. Ordinary zero-offset
fixed-capacity laws retain the compiled one-second exchange kernel; nonzero-offset
or phase laws use `MaterialEnthalpyExchange` over bounded sensible/latent segments.
The phase plateau is part of H-to-temperature conversion, not a separate reservoir.
`PhaseTransitionRuntime` owns only completed-transition requests. Main-thread ACK
requires the matching block, Page lifecycle, profile, branch and request sequence;
an applied ACK does not subtract latent energy again.

Contact conductance uses the material category whether or not the state has phase
transitions: earth `1.0 W/K`, masonry `1.4 W/K` per full block face. A recipe's
explicit `conductance_w_per_k` overrides that default. Adding a heating/cooling edge
does not change conductance. The former `phaseFaceConductanceWPerK` override and
configuration entry are removed; this applies to all phase-capable materials.
Latent energy comes from the source/target C/O references described below.
The old `phaseBaseEnergyJPerHeatCapacity` probability-derived configuration is removed.

New material bodies initialize from the Page's captured natural temperature.
Capacity is fixed for a given material state: the initial ordinary-body tuning
uses six times the former per-face capacity (stone: 5400 J/K; wood: 2700 J/K).
Exposure changes connections, not capacity. `MaterialSectionState` stores exact
body H, phase branch and stable BlockState identity separately from compressed Air
history. Checkpoint format 5 retains actual state times and has no older-format migration reader.
Dormant bodies relax toward current Section-center natural temperature on demand,
using the same default 1800-game-second sensible half-life as Air. Latent heat is
advanced by energy flux, not by a temperature lerp; no offline source power is integrated.

`TopologyView.materialContactAllowed` limits material conduction to a geometric
surface and its second material layer. A second-layer block selects one surface
owner; second-to-second connections and connections to a different owner are
omitted. A resident third layer cannot join that chain merely because it has H.
This is an explicit gameplay range limit, not a physical claim about real walls.

`MinecraftMaterialLawCompiler` compiles only explicitly declared heating/cooling edges.
The shipped data preserves the ice-stage chain and its 9 C heating thresholds;
source water freezes toward thin ice at -5 C. There is no reciprocal-pair inference
or material-specific fallback conversion.
## Generator Gameplay Floor

Generator also publishes a regional analytic floor from authoritative team data,
independent of its normal physical source. Its sphere takes the maximum matching
natural-relative temperature increase; physical powers still add normally.
The sphere does not write worker enthalpy, alter FarField, seed Bricks, or clip
physical propagation. Positive generator levels can retain gameplay afterheat
after active power stops and while the source chunk is unloaded. Indoor heating
becomes visible above the floor only when the actual source-connected Air is
hotter than that floor. See [temperature composition](world-climate-and-temperature.md#7-analytic-control-fields).

Geometry uses whole-block ventilation: static full collision blocks are V0,
ordinary Air is V100, partial/dynamic/unsupported shapes normally use V75, and
registered generator T1/T2 blocks are V0. Unresolved capture data remains distinct
from that supported fallback. Material capacity and phase capability are independent
of ventilation. Fluids and waterlogged bodies retain material laws; no-matter
states such as fire do not acquire fictitious solid heat capacity.
Explicit analytic bounds can authorize gameplay melting/evaporation even
inside physical phase ownership when the bound itself reaches the candidate's
threshold. Analytic fields are an authoritative gameplay mechanism alongside
finite-power sources. For a physically owned body, `PhaseIntent` advances its H
to the required transition boundary and records the gameplay energy input before
the world mutation request. `ADD_DELTA` alone has no guaranteed temperature floor.
This distinction retains the existing field combination rules.

`MinecraftPhaseController` marks unrecorded environmental conversions with
`GAMEPLAY_TRANSITION`. Where a material layout is pending, this cause preserves
material temperature under the target law. `BrickMigrationKernel` records the
resulting H difference only after topology commit; normal physical phase ACK
preserves H exactly. `ThermalCellArena.externalMaterialEnergyJ()` also includes
modeled material replacement and effective-capacity changes, separately from
the source ledger's finite-power energy balance.

Original random updates also submit completed dormant physical transitions without
creating a Page. Data declares snow-to-Air, source-lava-to-basalt,
flowing-water freezing and other terminal energy edges.
Flowing lava shares the source lava's offset per unit heat capacity, so changing
fluid amount does not change temperature solely because of a different H reference.
Tracked snow is one material body, not a per-layer latent reservoir. Every declared
direction is compiled; missing directions have no conversion. `GAMEPLAY / DEFERRED / CHANGED`
distinguishes unowned native behavior, owned phase energy and successful mutation. Water/ice biome,
water-edge and lava-height conditions apply to the relevant physical commits.
Dormant heating forced by an explicit analytic floor supplies the required endpoint
H through the same scoped mutation; additive fields alone do not supply latent heat.

## Material Transition Data

Authoring input: `src/datagen/resources/data/frostedheart/data/state_transition.xlsx`.
`FHRecipeProvider` emits 74 `frostedheart:state_transition` recipes under the existing
`state_transition/` path. `StateTransitionData.CODEC` reads only the new schema.

Edit the workbook's original `block_temperature` data sheet, save, then use the
project's `runData` task. `FHRecipeProvider.materialTransitions` reads the table with
Apache POI/`ExcelHelper`, resolves `block` and `*_target` strings with
`BlockStateParser` (for example `minecraft:water[level=0]`), and emits the existing
runtime Codec. No spreadsheet parser or authoring flags enter the thermal runtime.
The `字段说明` sheet documents all columns, units and defaults.

The table uses the thermal fields below plus parallel `heating_*` and `cooling_*`
columns for target, temperature, terminal latent energy, world conditions and effect.
`recipe_id` is the stable path after `state_transition/`. `*_enabled=FALSE` retains
an editable direction without emitting it; a blank enable cell defaults to enabled
when a target exists. A blank target means no edge. An enabled target requires a
temperature; 0°C remains a valid explicit value. Blank optional numbers retain the
Codec defaults. Bauxite, kaolin, peat, sand and red sand retain their original
disabled cooling declarations in the table, without changing the shipped behavior.

| Field | Meaning / default |
|---|---|
| `block`, `all_states` | Exact BlockState, or expand its block's states; `all_states=false` |
| `capacity_j_per_k` | C in J/K; omitted uses material category defaults |
| `enthalpy_offset_j` | O in J at 0°C; default 0 |
| `conductance_w_per_k` | Per full-face conductance in W/K; omitted uses shared defaults |
| `heating`, `cooling` | Independent optional edges: `target` BlockState and `temperature_c` in °C |
| edge `latent_heat_j` | Used only when target has no material law; default 38,000 J |
| edge `world_conditions` | Optional flat `min_y_exclusive`, `fluid_boundary_tag`, `excluded_biome_tag` |
| edge `effect` | Optional presentation enum; default `none`; does not select thermal behavior |

For each state `H=O+C*T`. An edge at Tp has endpoints `Hs=Os+Cs*Tp` and
`Hd=Od+Cd*Tp`. Heating requires Hd>Hs; cooling requires Hd<Hs. Success preserves H
and installs the target law, so its temperature is Tp. With no target body, the
edge instead ends at Hs±L and removes the source record. Broad definitions scale
C and O together by the existing effective quantity ratio. Exact definitions override
broad definitions; within the same specificity, later sorted recipe IDs win.

The compiler makes two passes at startup/reload: establish references, then resolve
edges. It contains no material IDs, phase-family graph solver or strategy handlers.
`MinecraftThermalProfiles.Snapshot` stores two edge-reference arrays indexed by shared
profile ID. World conditions/effects participate in profile identity but are not
copied into nodes, worker energy laws or NBT. Ordinary unconfigured materials need no recipe.

`MinecraftPhaseController` shares submission checks across active requests, dormant
endpoints and unrecorded environmental equilibrium. It checks height before neighbor
or biome queries, probes at most four horizontal fluid neighbors only when configured,
and never loads a missing chunk. Water's cooling edge retains `minecraft:water`
boundary and `frostedheart:water_do_not_freeze` exclusion; ice heating retains
`frostedheart:ice_do_not_smelt`; source lava cooling requires Y>−55.

No-record equilibrium has no stored heat history or latent waiting time. Tracked bodies
retain energy/latent progression. Snow's temperature conversion uses its whole-body
Air target; `SnowLayerBlockMixin_Melt` separately retains light>11 layer loss only when
the phase controller yields to untracked gameplay. Lava's native ignition remains;
its separate 1/1000 cooling path is removed. Water uses the original chunk surface
sampling cadence: `(gameTime + chunkX + chunkZ) % tempBlockstateUpdateIntervalTicks == 0`
(20 ticks by default), provided randomTickSpeed>0. One X/Z column is selected with
`MOTION_BLOCKING`; only its surface water calls the shared phase controller. Freezing
does not require precipitation. The same sample still handles snow/precipitation.
`StateTransitionData.hasRandomTransitions()` excludes `minecraft:water` from added
block random eligibility and the generic phase dispatch, including when other blocks
make the Section tick. All water amounts keep their material laws. This is a sampling
policy, not a second thermal algorithm. There are no extra phase probability divisors,
candidate queues or new periodic tasks. Covered dormant water has no new random
conversion opportunity; active solver energy/phase requests remain independent.
The original incremental fluid counter may still make a modified pure-water Section
eligible for sampling. It is not overridden; water samples no longer invoke freezing
there. A Section recount excludes non-randomly-ticking water fluids as vanilla does.

Migration normalizes previously inconsistent references once in the authored data.
Examples in J: sand O=38,000; dirt O=228,000; farmland O=380,000; coarse dirt O=760,000;
source lava O=−580,000 (C=6,600 J/K). This supplies a consistent target reference for
previously unmodeled edges, including magma→lava. Earlier valid gaps are retained where
compatible; remaining gaps use the old content's nominal latent scale during migration.
These values are gameplay tuning, not measured thermodynamic constants. The runtime does
not infer or recompute offsets from a recipe graph. Natural lava's initial temperature
remains deferred; radiation temperature is not used as its material initial state.
Static radiation is an intentional gameplay balance setting, independent of material
temperature; their separation is not a defect or a planned coupling change.

This contract covers declared material transitions. Cauldron drip collection and
player ice-breaking are separate interactions and retain their existing rules.

## Heat Network

`HeatEndpoint`, `GeneratorData`, and `HeatNetwork` are a separate gameplay
inventory model. Their `heat`, `tempLevel`, consumer priority, provider output,
fuel, and grace-period values are not joules or watts unless a specific caller
documents a conversion. The network discovers connectors and endpoints through
its own block/entity lifecycle and does not write directly to thermal Page
cells. A machine may report a physical source and also expose a heat-network
endpoint; the two channels must not be counted twice by a caller.

## Lifecycle And Performance

Machine ticks call `MinecraftThermalInput.onGeneratorTick`,
`onFountainTick`, or `onRadiatorTick` after normal production calculations.
Enabled positive-power output starts the existing runtime if absent, without a
player query. Inactive/zero-power reports do not create a runtime.
T1/T2 generator publication continues even when `GeneratorData.tickBlock` skips
fuel consumption through `townProcessedTicks`. Runtime startup/reload does not
execute extra production or heat-network consumption: machines recover on their
next valid normal tick. Merely loaded but non-ticking machines are not actively
advanced; town/offline simulation is not a physical-source publisher.

Campfires are discovered through the runtime's shared startup/load/retry queue
after bootstrap by chunk load, first ignition, or the one-time loaded-source check
at server start or after reloaded server tags bind. Ignition reuses the existing
CampfireBlock mixin's `onPlace`, including same-block LIT transitions;
there is no campfire tick poll. Subsequent changes use the source queue
and final block-state mutation drain. Startup also attaches already-loaded
sections, so later ignition is observed even in an initially source-free section.
`PhysicalSourceSpatialIndex.discoverChunk` reads pending/live BE positions and
current states without creating BEs. Refueling still only changes BE data;
discovery does not depend on it or on campfire ticking. An unchanged observation
does not enter the source dirty queue. `onPhysicalSourceRemoved` marks one packed
source ID absent. Chunk unload settles and unloads sources in that origin chunk;
coherent Page checkpoints retain H and its sampling time. Target Page references
are released by the source index. Dormant data then follows natural relaxation;
there is no one-shot source-support bit, seven-section support refresh, offline
source integration or heat-network flow.

Routine work is proportional to changed sources and affected target buckets.
Stable campfires do not poll. Machines retain their existing O(1) observation
per production tick; unchanged output produces no dirty worker message.
Each dimension admits at most `65,536` physical sources and `131,072` retained
source-node generations. A source refused at the physical-source cap cannot
enter the worker batch. State-source refusals return a completion boolean and
queue their exact chunk; machines retry through normal production. Discovery
attempts at most eight pending chunks per tick and stops while capacity is full;
ordinary removals continue and the next available drain resumes queued work.
Completed chunks are not retained in a recovery directory. External JFR measures source event, binding, and accumulator
costs; production source classes contain no counters or test probes.
