# Heat Production And Network

- Status: `Current`
- Last verified: `2026-08-29`
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
| Campfire | `8,000 W` | `80%` convection through the block below | `20%` radiation declared loss |
| Generator | `10,000 W * level` | `70%` exhaust convection | `10%` internal heat, `20%` radiation loss |
| Fountain | `2,000 W * level` | `90%` convection | `10%` radiation loss |
| Radiator | `4,000 W * level` | `80%` convection | `10%` internal heat, `10%` radiation loss |

Blocked or unresolved AIR_FACE ports use the profile's missing-port policy.
They become an internal reservoir, declared loss, or degraded loss; they do not
create a fake Page or force a chunk load. Campfire block-state changes are
coalesced by position, and a lit/unlit change with identical thermal signature
updates only the source state.

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

New material poles initialize from the Page's captured natural temperature. Air
and material transfer uses the fixed one-second coefficient compiled during
topology preparation; phase and buoyant paths use the generic inverse-capacity
kernel.

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
target Page references are released by the source index.

Routine work is proportional to changed sources and affected target buckets.
Each dimension admits at most `65,536` physical sources and `131,072` retained
source-node generations. A source refused at the physical-source cap cannot
enter the worker batch; after capacity is released, already-scanned loaded
chunks are revisited in bounded 20-tick slices so a still-loaded campfire is not
permanently lost. External JFR measures source event, binding, and accumulator
costs; production source classes contain no counters or test probes.
