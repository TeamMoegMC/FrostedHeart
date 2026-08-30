# Heat Production And Network

- Status: `Current`
- Last verified: `2026-08-31`
- Scope: physical Minecraft sources, worker energy integration, material/phase sinks, and the separate heat-network model
- Primary code anchors: `MinecraftPhysicalSourceProfile`, `PhysicalSourceSpatialIndex`, `ThermalSourceBatch`, `ThermalSourceLedger`, `NodePowerAccumulatorArena`, `HeatEndpoint`, `HeatNetwork`

## Physical Sources

Physical sources are observed on the server thread and coalesced into the next
20-tick `ThermalSourceBatch`. A source has one packed BlockPos identity, a
lifecycle generation, an origin, an anchor/target, a profile, and immutable
ports. `PhysicalSourceSpatialIndex` indexes origin section/chunk and target
section, so changing one machine or campfire does not scan all sources.

| Profile | Rated power | Thermal port | Other ports |
|---|---:|---:|---|
| Campfire | `8,000 W` default | `80%` convection into the Air block above | `20%` direct radiation |
| Generator | `10,000 W * level` | `80%` exhaust convection | `20%` radiation loss |
| Fountain | `2,000 W * level` | `90%` convection | `10%` radiation loss |
| Radiator | `4,000 W * level` | `90%` convection | `10%` radiation loss |

Blocked AIR_FACE ports become declared loss; topology-unavailable ports become
degraded loss. Neither creates a fake Page, accumulates pending energy, or
forces a chunk load. Campfire block-state changes are
coalesced by position, and a lit/unlit change with identical thermal signature
updates only the source state.

Campfire total power and radiation share are configured by
`FHConfig.COMMON.THERMAL_RUNTIME.campfirePowerW` and
`campfireRadiationShare`; convection receives exactly the remaining share.
Defaults are `8,000 W` and `0.20`. The main source index and worker binding
resolver use the same immutable configured profile.

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

Material surface poles and phase reservoirs are compiled only for affected
Bricks. `MaterialEdgeCompiler` groups raw material contributions by packed cell
edge and creates one deterministic execution entry for each unique edge. A
phase reservoir stores latent energy and candidate microcells; a phase request
is ACKed by the main thread only when Page lifecycle, profile, request sequence,
and current world state still match.

Phase contact conductance and base energy are configured by
`FHConfig.COMMON.THERMAL_RUNTIME.phaseFaceConductanceWPerK` and
`phaseBaseEnergyJPerHeatCapacity`. Defaults are `5 W/K` per full exposed block
face and `38,000 J` multiplied by the recipe `heat_capacity`.

New material poles initialize from the Page's captured natural temperature.
During dormant admission they instead use the stored capacity-weighted Air mean
for that Brick. Partial phase-reservoir energy is intentionally reset. Air and
material transfer uses the fixed one-second coefficient compiled during topology
preparation; phase and buoyant paths use the generic inverse-capacity kernel.

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
`onFountainTick`, or `onRadiatorTick`; campfires are discovered on chunk load
and final block-state mutation drain. `onPhysicalSourceRemoved` marks one packed
source ID absent. Chunk unload settles and unloads sources in that origin chunk;
before that removal, chunk checkpoints query the existing target-section index
for a disk-only one-shot support bit. Target Page references are then released
by the source index. This checkpoint can retain existing warm residuals across
an unloaded interval but never simulates source power or heat-network flow.

Routine work is proportional to changed sources and affected target buckets.
Each dimension admits at most `65,536` physical sources and `131,072` retained
source-node generations. A source refused at the physical-source cap cannot
enter the worker batch; after capacity is released, already-scanned loaded
chunks are revisited in bounded 20-tick slices so a still-loaded campfire is not
permanently lost. External JFR measures source event, binding, and accumulator
costs; production source classes contain no counters or test probes.
