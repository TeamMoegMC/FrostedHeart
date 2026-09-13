# Heat Production And Network

- Status: `Transitional; material-body replacement is under integration validation`
- Last verified: `2026-09-13`
- Scope: physical Minecraft sources, worker energy integration, material/phase sinks, and the separate heat-network model
- Primary code anchors: `MinecraftPhysicalSourceProfile`, `PhysicalSourceSpatialIndex`, `ThermalSourceBatch`, `ThermalSourceLedger`, `NodePowerAccumulatorArena`, `HeatEndpoint`, `HeatNetwork`

## Physical Sources

Physical sources are observed on the server thread and coalesced into the next
20-tick `ThermalSourceBatch`. A source has one packed BlockPos identity, a
lifecycle generation, an origin, an anchor/target, a profile, and immutable
ports. `PhysicalSourceSpatialIndex` indexes origin section/chunk and target
section, so changing one machine or campfire does not scan all sources.

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
updates only the source state.

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
Air, crops, soil, material poles, machines, dormant checkpoints, or
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

Phase contact conductance and base energy are configured by
`FHConfig.COMMON.THERMAL_RUNTIME.phaseFaceConductanceWPerK` and
`phaseBaseEnergyJPerHeatCapacity`. Defaults are `5 W/K` per full exposed block
face and `38,000 J` multiplied by the recipe `heat_capacity`.

New material bodies initialize from the Page's captured natural temperature.
Capacity is fixed for a given material state: the initial ordinary-body tuning
uses six times the former per-face capacity (stone: 5400 J/K; wood: 2700 J/K).
Exposure changes connections, not capacity. `MaterialSectionState` stores exact
body H, phase branch and stable BlockState identity separately from compressed Air
history. Checkpoint format 3 has no format-2 migration reader.

`TopologyView.materialContactAllowed` limits material conduction to a geometric
surface and its second material layer. A second-layer block selects one surface
owner; second-to-second connections and connections to a different owner are
omitted. A resident third layer cannot join that chain merely because it has H.
This is an explicit gameplay range limit, not a physical claim about real walls.

`MinecraftMaterialLawCompiler` preserves the configured ice-stage chain and its
9 C heating thresholds; source water freezes toward thin ice at -5 C. Other
unambiguous reciprocal phase pairs use the same law. Unsupported gameplay
transformations are not silently interpreted as physical latent heat.
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

`MinecraftPhaseController.applyGameplayTransition` marks gameplay material
conversions that do not use a physical phase edge. They preserve the previous
material temperature under the target law. `BrickMigrationKernel` records the
resulting H difference only after topology commit; normal physical phase ACK
preserves H exactly. `ThermalCellArena.externalMaterialEnergyJ()` also includes
modeled material replacement and effective-capacity changes, separately from
the source ledger's finite-power energy balance.

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
before that removal, chunk checkpoints query the existing target-section index
for a disk-only one-shot support bit. Target Page references are then released
by the source index. This checkpoint can retain existing warm residuals across
an unloaded interval but never simulates source power or heat-network flow.

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
