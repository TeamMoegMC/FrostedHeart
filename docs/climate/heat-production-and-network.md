# Heat Production And Network

- Status: `Transitional; current source and material ownership described below, earlier measurements retain their build scope`
- Last verified: `2026-11-22` for T2 pressure accounting; `2026-11-20` for six-face campfire heat exchange and material-frontier propagation without a depth cutoff, `2026-11-19` for fixed-field outlet mixing and source-triggered coverage updates, `2026-11-17` for fixed T1/T2 exhaust power, 2,000 W radiation, four horizontal targets and smooth 5°C center boost; `2026-11-06` for energy-complete phase submission; `2026-10-25` for source ownership and internal subsystem retirement; `2026-10-12` for campfire fuel countdown. Other sections retain their existing provenance.
- Scope: physical Minecraft sources, worker energy integration, material/phase sinks, and the separate heat-network model
- Primary code anchors: `MinecraftPhysicalSourceProfile`, `PhysicalSourceSpatialIndex`, `WorkerPhysicalSourceBindings`, `AirMixingRegion`, `SpatialSourceTimeline`, `SpatialSourceIngress`, `SpatialHeatStore`, `ThermalSourceBatch`, `ThermalSourceLedger`, `LocalMaterialBoundary`, `MaterialThermalLaw.initialBranch`, `RegionalQuerySnapshot.materialPerturbationAtSlotC`, `WorkerPageStore.collectMaterialFrontier`, `HeatEndpoint`, `HeatNetwork`

## Physical Sources

当前SHARED_GRID使用`SpatialSourceTimeline → SpatialSourceIngress → SpatialHeatStore`，producer接管、按结构边界绑定和区域提交消费是三个进度。Page片段不再携带源窗口；`ThermalSourceBatch`保留源控制的有序表达，旧后端已于2026-10-25删除。当前实现、计账、保存与不同编译版本的验证范围见[运行时所有权](thermal-runtime-control.md)。

Physical sources are observed on the server thread and integrated by `SpatialSourceTimeline`.
For power W and event interval deltaTick, input J is W * deltaTick / 20; signed impulses
already contain J. The producer normally seals a window every 20 ticks. A source has one packed BlockPos identity, a
lifecycle generation, an origin, an anchor/target, a profile, and immutable
ports. `PhysicalSourceSpatialIndex` indexes origin section/chunk and target
section, so changing one machine or campfire does not scan all sources.

Source windows retain their event times and total signed energy when delayed intervals
are compacted. `ThermalSourceBatch` only describes registration, power, enabled state
and unload controls; it no longer transports a second impulse or node-integration path.
Regional jobs consume claimed J only when the accepted result is published. The coarse
exchange step does not reconstruct intermediate geometry changes.

Each positive-share `AIR_FACE` or `SOURCE_MATERIAL` port retains its target Brick.
Its zero/nonzero source reference transition updates one main-thread source-seed
bit. Multiple sources in the same Brick share one lazy count. Source seeds are the only main-thread
cause of new thermal residency. The worker retains warmed Pages and advances
heat through loaded neighboring Bricks with changed absolute residency masks;
players and other queries do not hold Pages open.

| Profile | Rated power | Thermal port | Other ports |
|---|---:|---:|---|
| Campfire and soul campfire | `8,000 W` default | `60%` to the Air above and `20%` to the campfire body at default settings | `20%` direct radiation by default |
| T1 generator | `26,000 W` while emitting | Four horizontal exhaust targets, `6,000 W` each (`24,000 W` combined) | `2,000 W` radiation |
| T2 generator | `34,000 W` while emitting | Four horizontal exhaust targets, `8,000 W` each (`32,000 W` combined) | `2,000 W` radiation |
| Fountain | `2,000 W * level` | `90%` convection | `10%` radiation loss |
| Radiator | `4,000 W * level` | `90%` convection | `10%` radiation loss |

### Generator Horizontal Exhaust

T1 and T2 share `GeneratorLogic.tickFuel`. T1 uses
`MinecraftPhysicalSourceProfile.GENERATOR` (profile ID `2`); T2 uses
`MinecraftPhysicalSourceProfile.GENERATOR_T2` (profile ID `5`). Both immutable profiles
use the same four-direction geometry, constructed once by `generator`.
`MinecraftThermalInput.onGeneratorTick` receives the tower's bottom-center block as
both the stable source identity and the profile anchor. The profile defines four
Air targets outside the 3-by-3 tower, two blocks above its bottom layer:

| Direction | Offset from bottom center `(x, y, z)`, blocks | Target face | Port ID | T1 power | T2 power |
|---|---|---|---:|---:|---:|
| East | `(2, 2, 0)` | `NEGATIVE_X` | 0 | 6,000 W | 8,000 W |
| West | `(-2, 2, 0)` | `POSITIVE_X` | 1 | 6,000 W | 8,000 W |
| North | `(0, 2, -2)` | `POSITIVE_Z` | 2 | 6,000 W | 8,000 W |
| South | `(0, 2, 2)` | `NEGATIVE_Z` | 3 | 6,000 W | 8,000 W |

The target face is the Air block's face toward the tower. Both tiers retain a
fixed `2,000 W` radiation allocation in the `RADIATION_LOSS` record, ID4.
The profile factory computes each Air fraction as `outletPowerW / totalPowerW`
and the radiation fraction as `2000 / totalPowerW`, where
`totalPowerW = 4 * outletPowerW + 2000`. This gives `26,000 W` for T1 and `34,000 W`
for T2; radiation is not a fixed percentage of the new totals.
`GeneratorLogic.tickFuel` supplies a power multiplier of `1` for positive temperature
levels and `0` otherwise; the active flag still controls emission. Thus the physical
powers above are fixed while the tower is emitting, including at temperature
levels `1` and `10`. Gameplay temperature levels continue to determine the analytic floor.
Power is distributed by the existing port mechanism from one registered source.
Rotation and mirroring preserve the same four world directions.

Each outlet binds, seeds its target Brick and responds to block changes through
the existing source lifecycle. A blocked outlet's own share becomes declared
loss; the other outlets keep their shares. Reopening the outlet restores its
normal binding. Unavailable topology retains the existing degraded-loss path.
`PhysicalSourceSpatialIndex.MAX_TARGET_BRICKS` reserves four deduplicated target
slots per source; unchanged machine reports do not reconstruct the static port table.
`SpatialSourceTimeline.MAX_PORTS_PER_SOURCE = 5` is the shared implementation
capacity for four `PortKind.AIR_FACE` records and one `PortKind.RADIATION_LOSS`
record. `ThermalDimensionEngine` uses
that same value for the ledger and heat-buffer bounds; the producer retains one
additional common channel and computes its reserved backing from the resulting size.
These are code capacities, not server configuration settings.
Here `Port` and `EmissionPort` count entries in the source's power-share array,
not just physical openings. `WorkerPhysicalSourceBindings.initialPorts` converts
every profile entry into an `EmissionPort`: `AIR_FACE` has a world offset and face,
whereas `RADIATION_LOSS` has neither and uses `SourceBinding.declaredLoss`.
The latter therefore occupies an array slot under the current representation.
The radiation query itself uses the separately summed `radiativePowerShare`;
it does not require an Air target or an additional physical outlet.
The separate 5°C analytic center boost is described under
[Generator Gameplay Floor](#generator-gameplay-floor).

`RADIATION_LOSS` is an energy-accounting destination, not an additional opening
on the multiblock. The earlier generator profile used a 20% radiation fraction;
the current tier profiles fix radiation at `2,000 W` and derive its fraction from
the tier's total power. The Air/material ledger records it as declared loss, while
`PhysicalSourceSpatialIndex.visitSection` exposes the active machine's
`profile.radiativePowerW(totalPowerW)` to `RadiationService`. Actual received flux
also depends on distance, occlusion and query budgets.
This dynamic machine-source path is separate from `MinecraftThermalProfiles` /
`BlockRadiationIndex` static block radiation: the shipped
`thermal/static_fire_radiators` tag contains fire and soul fire, not generator
multiblocks. Absence from that static table does not remove the machine-source path.
Infrared display reads material temperatures and analytic fields independently
of this declared radiation share.

### Campfire Body and Six-Face Exchange

`MinecraftPhysicalSourceProfile.campfire` applies `campfirePowerW` (total power
`P` in W, default `8,000 W`) and `campfireRadiationShare` (fraction `r` in `[0, 1]`,
default `0.20`) from `FHConfig.COMMON.THERMAL_RUNTIME`. The shares of power are
`0.75 × (1 − r) × P` for the upward `AIR_FACE` port, `0.25 × (1 − r) × P` for the
`SOURCE_MATERIAL` port at the campfire position, and `r × P` for direct radiation.
At the defaults this gives `4,800 W`, `1,600 W`, and `1,600 W`, respectively.
The partition of the non-radiative share is a source default.

`MinecraftThermalProfiles` gives `minecraft:campfire` and `minecraft:soul_campfire`
an effective material heat capacity of `900 J/K` and per-face conductance of
`3 W/K` before waterlogging and material data overrides. It assigns six thermal
contact faces to represent the area around the fuel and flame. `TiledThermalField`
exchanges heat between the body and each face-adjacent Air or contacting material
cell according to the modeled temperature difference and contact conductance.
`PhysicalSourceSpatialIndex.refreshTargets` retains the positive-share port
targets: the body and the upward Air target (at most two distinct Bricks).
`WorkerPageStore.collectMaterialFrontier` admits contacting neighbors once the
body's Accepted temperature differs from its natural material background by at
least 1 K; an existing request uses 0.5 K. This applies to every material on a
Brick boundary, including completely enclosed campfires and heated inner stone.
The six neighbors are not unconditional source seeds.
Neighbors already inside a captured Brick can exchange immediately; a neighbor
outside it waits for the ordinary threshold and geometry admission.
If a solid blocks the upward outlet, the body still receives its share and
can warm the solid above it. Extinguishing the fire stops the input and the
body sheds stored heat through the same faces. `WorldTemperature.getTempAtBlock`
can read that body temperature.

### Campfire Fuel Lifecycle

`CampfireTileEntityMixin_TimeLimit` still consumes one `lifeTime` unit on every
actual lit cooking tick. The final decrement to0 leaves that tick lit; the next
tick extinguishes through the existing block update. Countdown calls vanilla
`Level.blockEntityChanged` every tick to preserve chunk dirty marking. Ordinary
campfires have no analog output, so only their countdown skips the additional
neighbor-output scan; an extended state with `hasAnalogOutputSignal` keeps
`setChanged`. Fuel insertion, lifetime assignment/load, vanilla cooking/inventory
updates and extinguishing retain their full notifications. Save serialization
reads the current lifetime, with the existing item burn-time multiplier and cap.

The real stick/cooking/comparator and save/unload/disk-reload scenes passed before
and after this change. The same900tick fuel lifecycle and exact saved/loaded
lifetime were retained;23 matched Air observations differed by at most0.000119C.
See the [fuel comparison](../../build/checkpoint-hotspots-fuel-comparison-20261012.json)
and [save hotspot execution plan](../../plans/2026-10-12_checkpoint-save-hotspots-next.md).

### Port Binding And Mixing

Blocked AIR_FACE ports become declared loss; topology-unavailable ports become
degraded loss until their exact source Brick is compiled. Neither creates a
fake natural sink, accumulates pending energy, or forces a chunk load. Campfire block-state changes are
coalesced by position, and a lit/unlit change with identical thermal signature
updates the source state and the nearby Air coefficients. The campfire body
carries its accumulated heat through the lit state transition.

Each air cell whose center lies within 4 blocks of a positive-share `AIR_FACE`
outlet on an emitting source gets a mixing factor of 4. The factor is 1 outside
the union of these spheres. At a direct horizontal air-to-air face the solver
averages the two cells’ factors, so a face crossing the radius uses 2.5 times
its base conductance. Vertical exchange also applies each cell’s buoyancy factor.
The radius defines the local mixing boost. Heat continues to propagate across its boundary.

`AirMixingRegion.cellMask` computes the union. `TiledThermalField.replaceMixing`
stores one bit per cell in a 64-bit word per allocated Brick. The fixed-field
engine initializes newly captured Bricks and updates existing coverage when an
outlet starts or stops emitting. The source and geometry clocks are described
in the [thermal runtime contract](thermal-runtime-control.md).

The main source index and worker binding resolver use the same immutable
configured profile. Minecraft profiles retain a source while `enabled && powerW > 0`.
Inactive or zero-power observations remove an existing source through the
normal cut. Only positive-share thermal ports seed Pages. A fully radiative
campfire still retains its emitting source. Removing or moving
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

The formal runtime uses `SpatialSourceTimeline` to capture event-time power and signed impulses,
`SpatialSourceIngress` to transfer source windows, and `SpatialHeatStore` to own accepted,
unconsumed joules. Regional jobs claim a prefix and consume it only when their result commits;
cancellation returns the claim. `ThermalSourceLedger` supplies source metadata and the energy
balance, while `MaterialStateAccess` reads the actual regional material owner. See
[the current ownership contract](thermal-runtime-control.md). The old node accumulator,
direct arena delivery and dual recording-mode switch have been deleted. Binding changes
affect the current port identity; already accepted J keeps the Store's world identity and
handoff rules. The ledger accounts for committed delivery, unaccepted energy, declared
loss, unavailable-geometry loss and exported pending heat without integrating power again.

## Materials And Phase

`TiledThermalField` stores one accepted enthalpy and phase branch per resident material cell. `MaterialThermalLaw` converts that state to degrees Celsius. Local air/material and material/material exchange transfers equal and opposite joules. Geometry, world mutation acknowledgement and chunk handoff use the same cell identity. See the [fixed-field contract](thermal-runtime-control.md).

The phase plateau is part of H-to-temperature conversion, not a separate reservoir.
`PhaseTransitionRuntime` owns completed-transition requests and applies explicit
gameplay phase controls through the selected material owner. Main-thread ACK
requires the matching block, Page lifecycle, profile, branch and request sequence;
an applied ACK does not subtract latent energy again.
`MinecraftPhaseController` applies `world_conditions.fluid_boundary_tag` when an
untracked body starts a natural transition. A live or dormant body whose actual H
has completed the latent interval commits without that start-boundary condition;
interior water can become thin ice after its finite energy interval is complete.
The dormant entry rejects incomplete H. Random-tick switches, biome exclusions,
minimum height and analytic floors retain their existing effect. See the
[completion and cooling contract](thermal-residual-lifecycle.md#自然冻结启动与已完成相变提交).
Production completions hand off at most 160 requests per 20-tick cut so the main
thread's 8-per-tick mutation limit can be used throughout the cut. A matching body
whose Page query has not caught up to topology publication is retried with the same
sequence; a missing Page lifecycle or changed block identity is rejected.

Contact conductance uses the material category whether or not the state has phase
transitions: earth `1.0 W/K`, masonry `1.4 W/K` per full block face. A recipe's
explicit `conductance_w_per_k` overrides that default. Adding a heating/cooling edge
does not change conductance. The former `phaseFaceConductanceWPerK` override and
configuration entry are removed; this applies to all phase-capable materials.
Latent energy comes from the source/target C/O references described below.
The old `phaseBaseEnergyJPerHeatCapacity` probability-derived configuration is removed.

Blocks in `#minecraft:crops` use `GameplayMaterial.CROP` with a source-default
heat capacity of `720 J/K` and per-face conductance of `2.5 W/K`. See
[crop temperature](crop-fertilization.md#生长温度) for the gameplay reader.

Ordinary `minecraft:farmland` has a per-face conductance of `1.5 W/K` across
all moisture states. The source recipe sets `capacity_j_per_k` to `6,600`
and the freezing transition to `frostedheart:dirt_permafrost` at `−10°C`.
The value applies to physical exchange with air and adjacent solids. Glass
shelter ventilation and the amount of heat supplied govern the long-term soil
temperature. Crop status reads the surroundings at the crop position as
described in [Crop Temperature and Fertilizer](crop-fertilization.md).

New material bodies initialize from the Page's captured natural temperature.
`MaterialThermalLaw.initialBranch` selects the same initial phase branch for arena
initialization and material-frontier background calculation. A cold natural water
or soil body can therefore start on its freezing plateau. Its latent responsibility
retains that body; outward material admission separately requires an actual boundary
temperature perturbation from that material's natural initialized temperature, with
the existing 1K/0.5K hysteresis. See [residual ownership and material expansion](thermal-residual-lifecycle.md).
Capacity is fixed for a given material state: ordinary category defaults use
stone 5400 J/K and wood 2700 J/K, with explicit recipe capacities taking precedence.
Exposure changes connections, not capacity. `MaterialSectionState` stores exact
body H, phase branch and stable BlockState identity separately from compressed Air
history. Checkpoint format 7 retains actual state times and has no older-format migration reader.
Dormant bodies relax toward the current natural temperature of their own block on demand,
using the same source-default 30-game-second sensible half-life as Air (explicit existing configuration still applies). Latent heat is
advanced by energy flux, not by a temperature lerp; no offline source power is integrated.
Ordinary residuals below 0.5°C can be removed during capture or loaded-checkpoint maintenance;
latent progress and pending input retain their existing owners. See [residual lifecycle](thermal-residual-lifecycle.md).

Material conduction has no fixed layer-depth cutoff or exclusive surface owner.
Adjacent inner bodies can exchange and carry heat through the third and deeper
layers. The fixed solver and reference contact compiler use
`ThermalSignatureTable.contactFraction`. A slab or stair contributes its occupied
eighths to the category-default heat capacity. Two adjacent bottom slabs share
half a horizontal face; opposite halves share none. Full blocks use the full-face
conductance. Material admission examines actual boundary-material perturbation
with the existing 1 K / 0.5 K thresholds, whether the body is exposed to Air or
buried inside a wall. A heated inner boundary can request the next contacting
Brick; capture alone does not create heat or authorize a cold chain of requests.
There are no material-layer permission masks to rebuild. See [runtime residency](thermal-runtime-control.md)
and [residual ownership](thermal-residual-lifecycle.md).

`MinecraftMaterialLawCompiler` compiles only explicitly declared heating/cooling edges.
The shipped data preserves the ice-stage chain and its 9 C heating thresholds;
source water freezes toward thin ice at -5 C. There is no reciprocal-pair inference
or material-specific fallback conversion.
## Generator Gameplay Floor

Generator also publishes a regional analytic floor from authoritative team data,
independent of its normal physical source. Its sphere adds a smooth 5°C center
boost to the existing natural-relative temperature increase, tapering the extra
boost to zero at the original boundary. At distance `d` within radius `R`, the
extra boost is `5 * (1 - d²/R²)²` Celsius. Overlapping towers take the maximum
local temperature increase, including each tower's boost; physical powers still add normally.
T1 and T2 share `GeneratorData.publishGameplayHeat`, so both tiers use this profile.
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

冰锥另有手写 `src/main/resources/data/frostedheart/recipes/state_transition/frostedheart/icicle.json`，
不属于下面的 74 行表格。配方定义 2°C 平台及约 126666.67 J 潜热，约为当前完整薄冰
heating 能量端点差的 1/3；比例由数据维护，编译器没有冰锥专用分支。其非原位相变缩短连续链末端，并以不可见
`melt_cycle` 状态和现有 `GAMEPLAY_TRANSITION` 材料迁移复位存活源段；ACK 本身仍不扣能量。
缺失记录回退、休眠投影与保证温度场均沿用通用流程，没有冰锥专用材料补建或存档逻辑。
被删除末端的部分潜热不转交源段。详细的生长、热裂与生命周期规则见 [icicle.md](icicle.md)。

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

Within `world_conditions`, `fluid_boundary_tag` is a natural initiation condition;
`min_y_exclusive` and `excluded_biome_tag` also apply to energy-complete submission.

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

An eligible natural random update initializes an unrecorded body with the local environmental estimate. An incomplete latent interval becomes a dated chunk material record. A completed interval or an explicit heating floor can commit the declared transition. At the exact threshold zero progress needs no checkpoint. Snow's temperature conversion uses its whole-body
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

**Pipe revalidation.** `ConnectorNetworkRevalidator.tick` checks the open faces
of a connected pipe once per 10 valid block-entity ticks. The fallback checks
already-loaded neighbors and does not load chunks. An opened face still attempts
to connect immediately; a closed face, removed pipe, or changed network owner
still queues the existing topology refresh. The interval is unrelated to how
often an already-connected network distributes heat.
The focused Forge fixture `HeatPipeRevalidationGameTests` measured
10 fallback visits in each of two consecutive 100-tick windows and verified
reconnect/removal/merge plus an unloaded neighbor that reconnects after reload.
This is a call count for stable pipes, not a server-tick speedup measurement.

**T2 outlet inventory.** `HeatNetwork.tick` transfers from the provider and returns
unused heat before the generator refills. `T2GeneratorState.updateOutlet` buys
only the deficit below `25 × steamLevel` using whole extra fuel process ticks
at `(1 + GENERATOR_HEAT bonus) × 25/8` heat per tick actually paid.
The native provider is the only live balance and the master saves it as
`outletHeat`; switching off, breaking or running out of fuel permits a final
discharge of existing heat, not another offer of its previous value. A tower
without a ticking master buys no network heat. A legacy team offer can seed
an old master once; see [town fuel accounting](../town/town-model.md#54-t2-网络实际结算).
No heat-to-watts conversion, station conversion or physical exhaust rate changes.
The separate T2 fluid tank warms when it can supply the configured
`generatorSteamRate` (mB per active tick). The same exact dose and shortage
behavior are kept by checking the local tank amount before one real drain.

The focused Forge namespace `frostedheart_t2_pressure` uses a real T2 state,
town and live fuel paths, native heat network, the coal recipe, and a continuously
drained endpoint with the same 5-heat/tick intake and demand as one station.
Its `T2PressureGameTests.oneCoalFeedsAHotSingleStationWithAndWithoutTown` measurement
starts empty at steam level 1, no research or overdrive, with one ordinary coal worth
1,120 process ticks. It does not simulate boiler warm-up, an entire town, or a chargeable item.

| Base settlement | Base ticks spent | Extra process ticks spent buying heat | Process remaining | Network heat delivered |
|---|---:|---:|---:|---:|
| Each live tick | 427 | 693 | 0 | 2,165.625 |
| Town, 20-tick batches | 420 | 683 | 17 | 2,134.375 |

Both outlets and both receiver buffers finished empty. In the town case the final 17
process ticks cannot pay another base batch; they stay in the team fuel balance.
These are two measured finite-run results, not a universal heat-per-coal constant.



## Lifecycle And Performance

Machine ticks call `MinecraftThermalInput.onGeneratorTick`,
`onFountainTick`, or `onRadiatorTick` after normal production calculations.
Enabled positive-power output starts the existing runtime if absent, without a
player query. Inactive/zero-power reports do not create a runtime.
T1/T2 generator publication continues even when `GeneratorData.tickBlock` skips
base simulation already covered by a saved town batch (`baseAccountedThrough`)
or by the in-memory last live tick. Loaded T2 pressure purchases are independent
of that skip; see [town fuel accounting](../town/town-model.md#54-t2-网络实际结算).
Runtime startup/reload does not execute extra production or heat-network
consumption: machines recover on their next valid normal tick. Merely loaded
but non-ticking machines are not actively advanced; town/offline simulation
is not a physical-source publisher.

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
