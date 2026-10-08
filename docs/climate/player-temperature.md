# Player Temperature

- Status: `Current`
- Last verified: `2026-09-25`
- Scope: player environment sampling, five-part body energy, wearable thermal reservoirs, clothing, Wet, heating equipment, thermometers, HUD, effects, persistence, and synchronization
- Primary code anchors: `PlayerTemperatureUpdate.updateTemperature`, `PlayerTemperatureComputation.updatePlayer`, `PlayerThermalEnvironment`, `PlayerEquipmentHeating`, `PlayerThermoregulation`, `PlayerThermalModel`, `PlayerThermalInjury`, `PlayerTemperatureData`, `ThermometerItem`, `CreativeThermometerItem`, `FHTemperatureDisplayPacket`, `WearableThermalExchangeHandler`, `ThreeNodeWearableHeatExchange`, `ThermalReservoirBlock`, `ThermalReservoirBlockEntity.serverTick`, `FHBodyDataSyncPacket`, `FrostedHud.renderTemperature`

## Player-Facing Values

The temperature orb uses sampled Thermal Air for both its number and color.
Wind, clothing, and body state do not change this air reading. The still-air
environmental equivalent remains a server diagnostic in `/temperature get`.

| HUD surface | Value | Meaning |
|---|---|---|
| Number | sampled Thermal Air, `C` | the local world air before entity attribute and Sauna adjustments |
| Orb color | sampled Thermal Air, `C` | the existing orb texture bands are selected from the same Celsius value as the number |
| Body status/effects | body temperature offset from `37 C` | accumulated physiological danger |
| Mercury body thermometer | core body temperature, displayed to `0.1 C` | the existing held measurement flow uses the normal client temperature unit |
| Creative thermometer | raw absolute core body temperature, `C` | right-click reports immediately in every game mode without display quantization |

The number and color are never body temperature or `air - 37`.
`PlayerTemperatureData.getEnvTemp()` now returns sampled Air; its previous
still-air equivalent is available through `getEnvironmentEquivalentTemperatureC()`
on the server. `FrostedHud.renderTemperature`, the forecast's current-temperature
reading, and cold-breath particles consume the sampled Air value. Clothing,
Wet, movement, difficulty, food, and equipment can change body power without
changing the HUD air reading. The diagnostic command continues to show the
equivalent, Air, radiation, wind, and net body power separately.

Health-screen trend verified `2026-09-25`: `HealthStatMenu.coreTemperatureTrend`
uses `getCoreBodyTemp() - getPreviousCoreBodyTemp()` from the latest body update,
including the subsequent equipped reservoir exchange. Changes within
`0.0002 C` per elapsed game second are shown as stable; larger positive/negative
changes are rising/falling. `HealthStatScreen` displays the localized direction
below the body and nutrition widgets. The value uses the existing menu data-slot
sync only while the menu is open. It describes core temperature direction, not
the direction of environmental temperature or whether the player is healthy.

`ThermometerItem` keeps its 100-tick held measurement. Its floating-point
display packet is quantized to one decimal place and formatted with one decimal
digit on the client. `CreativeThermometerItem` handles the server-side right
click immediately, reads `PlayerTemperatureData.getAbsoluteCoreBodyTemp()`, and
passes `Float.toString` directly to the localized message. That raw value is
reported in Celsius and is not converted to the client Fahrenheit setting.

## Cadence And Environment

`temperatureUpdateIntervalTicks` defaults to `20`.
`PlayerTemperatureUpdate.shouldUpdatePlayer` assigns each UUID a stable phase so
players are distributed across the interval. Each update performs one
`MinecraftThermalInput.gameplayPlayerEnvironment` query using a reusable
`ThermalEnvironmentSample`.

The query returns absolute Thermal Air in degrees Celsius and direct radiant
flux in `W/m2`. `FHAttributes.ENV_TEMPERATURE` modifiers and the existing
Sauna effect are then applied to the player's local air boundary.

The owned environment attribute modifier is reused while its sampled value and
ADDITION operation match. Changed air replaces it; the final attribute value is
still read on every update so other modifiers remain effective.

Outdoor wind is `WorldTemperature.wind * 19.444 / 100 m/s`; the initial indoor model applies
that wind only when `ServerLevel.canSeeSky` is true at the player's eye
position. A roof therefore gives exactly `0 m/s` local wind. No ray, Page,
cache, or stored openness value is used for this gate.

Missing or stale Page data follows the existing natural-Air fallback and never
loads a chunk or waits for the worker.

## Body Energy

`BodyPartData.bodyEnergyOffsetJ` stores one energy offset for `HEAD`,
`TORSO`, `HANDS`, `LEGS`, and `FEET`. Absolute part temperature is:

```text
T_part_C = 37 C + E_part_J / C_part_J_per_K
C_part_J_per_K = 245000 J/K * BodyPart.area
```

The five `BodyPart.area` values sum to `1.0`. Core temperature remains the
existing weighted `HEAD + TORSO + LEGS` view. Internal torso/head, torso/legs,
torso/hands, and legs/feet transfers conserve total body energy and are clamped
before pair equilibrium.

Air, long-wave exchange, direct source radiation, clothing insulation, contact
media, Wet, metabolism, movement, thermoregulation, and equipment all enter one
power balance in watts. `PlayerTemperatureComputation.updatePlayer` integrates that
balance through five visible phases: environment sampling, contact preparation,
active-power collection, body integration, and observation publication. The
natural-convection coefficient uses the fixed 33 C reference skin temperature
and is calculated once per update, then passed to the five part preparations and
environmental-equivalent calculation. Clothing and forced convection retain
their per-part calculations. The stateless `PlayerThermalModel` owns the formulas,
including the closed-form exponential step, so passive water or lava contact
cannot numerically jump through its boundary temperature. The configured
`temperatureChangeRate` multiplies one explicit `GAMEPLAY_TIME_SCALE` of
`8`; this is the gameplay acceleration, not another temperature unit. The
runtime configuration default for `temperatureChangeRate` remains `1`.

`PlayerThermalModel.preparePart` applies separate dimensionless source-default
exchange multipliers to its base conductances: Air `0.0824`, water `0.51`,
powder snow `0.116`, Wet `0.03`, and lava `2`. Direct source radiation retains
its separate multiplier `2`, while conservative internal body-part transfers
retain `2`. The Wet multiplier applies to its additional `12 W/(m2*K)` path;
it remains active after leaving water until the Wet effect expires. These
values target the legacy normal-difficulty initial cold-weather cadence without
changing the clothing multiplier, world source power, or thermal runtime.
Full immersion and powder-snow contact have their own calibration; partial
immersion and strong wind still follow the current model. The still-air HUD
equivalent continues to be computed for diagnostics and is not scaled by the
body exchange multipliers.

## Contact, Wet, And Clothing

Water and lava use `player.getFluidHeight(tag) / player.getBbHeight()`.
Each `BodyPart` owns a fixed vertical immersion band, so contact engages feet,
legs, torso/hands, and head progressively. Water uses a `0..35 C` boundary
derived from local air instead of the old fixed `x6` Celsius multiplier.
Powder snow and on-fire state remain local body inputs.

These local entity contacts do not write Thermal Air or register a world source.
Nearby lava and ordinary fire may add optional read-only direct radiation from
the sparse `BlockRadiationIndex`; they do not heat Thermal Mesh Air. Lava/fire
contact remains a separate local body input. Campfire retains its existing
physical-source split and radiation path unchanged.

The existing Wet effect remains the only post-exit wetness state. Leaving water
removes the water-contact conductance on the next player update; Wet continues
its reduced extra exchange until the existing effect expires. Wet heat loss and
sweating share one low-cost evaporation ceiling.

`BodyPartData.fillClothData` reads existing equipment attributes and
`ArmorTempData` layers directly into one reusable `PartClothData`. It creates
no per-update list. The existing layer weights produce a dimensionless legacy
insulation score `I`; `PartClothData` converts it to the original environmental
exchange factor `100 / (100 + max(0, I))`. This factor multiplies passive air,
water, powder-snow, lava, and Wet conductances after the corresponding base
medium and tissue conductances are computed. It preserves the old clothing
curve for unchanged recipe values while the body still integrates watts and
joules. In particular, a lone torso item with `factor=500` has `I=200` and
one-third the uninsulated passive conductance; `factor=900` has `I=360` and
`100/460` the uninsulated conductance. Direct source radiation is added to the
air path's weighted boundary before clothing scaling, so insulation does not
reduce that heat gain; radiant heat proof still does. Wind proof and water
resistance retain their existing sources and layer weights. This conversion
reproduces clothing's relative exchange factor, not the removed model's
absolute body-temperature cadence, weather formula, or metabolic behavior.

`PlayerThermoregulation` uses source-default basal power `30.625 W`, walking
adds `30.625 W`, and sprinting adds no power, matching the old executed sprint
branch. `FHTemperatureDifficulty.heat_unit` (`easy=2`, `normal=1`, `hard=0.5`,
`hardcore=0`) multiplies basal and movement power as well as regulation.
With food available, normal-difficulty core temperatures below `36.9`, `36.5`,
and `36.0 C` select `61.25`, `91.875`, and `122.5 W` of shivering. With water
available, core temperatures above `37.1` and `37.5 C` request `30.625` and
`61.25 W` of sweating; the shared evaporation ceiling may reduce applied
cooling. `JOULES_PER_EXHAUSTION = 5444.444` maps applied regulation power to
food/water cost. Regulation uses the current weighted core reading, not the
legacy per-part post-exchange deviation. The five parts remain independent.

## Equipment, Food, And Effects

`BodyHeatingCapability.tickHeating` now contributes explicit watts through
`HeatingDeviceContext.addPower`. Existing fuel, durability, heat-storage
capabilities, item stacks, and primary item NBT keys are unchanged. Device
resource use is scaled by real elapsed seconds, independently of
`temperatureUpdateIntervalTicks`; sub-second remainder uses the optional
`frostedheart:partial_heating_second` item key and is removed whenever it
returns to zero. A zero physiological time scale skips both equipment power and
resource use. Food converts its existing temperature delta into joules through
`PlayerTemperatureComputation.bodyEnergyForTemperatureDeltaJ` and applies the
existing minimum/maximum body offsets.

The established body-part effect thresholds still consume offsets relative to
`37 C`: torso drives hypothermia/hyperthermia, head drives confusion, lower
limbs drive slowness, and hands drive mining slowdown. The `INSULATION` effect
freezes the ordinary environment/physiology body step, but an equipped thermal
reservoir still exchanges with the player afterward. Creative mode, spectator
mode, and invulnerability skip wearable exchange as well as climate injury.

## Wearable Thermal Reservoirs

`frostedheart:warm_stone` and `frostedheart:hot_water_bag` each persist a core
and surface temperature. Their frozen normalized capacity ratios are `0.10`
and `0.25` relative to the whole player capacity; surface share is `a=0.20`.
Core/surface transfer rates are `2.46452e-4 /s` and `3.6968e-3 /s`, while
surface/player rates are `6.0e-4 /s` and `4.0e-4 /s`. These are the original
reservoir constants multiplied by `4` and `5`, respectively; the derived
environment rates retain their `0.5` inventory and `8` dropped/placed ratios.
Every at-most-one-second substep uses
`core-surface half -> surface-player full -> core-surface half`
through `ThermalExchangeKernel.exchangePairWithInverseInto`.

Here `g` is the normalized heat-transfer rate per degree difference. For a
node with normalized capacity ratio `r`, its actual temperature coefficient is
`g/r` in `degC/s per degC`; the hotter side changes by `-g/r * deltaT` and the
colder side by `+g/r * deltaT`. The final node-side coefficients are:

| Edge | Warm stone | Hot-water bag | Node that changes |
|---|---:|---:|---|
| core -> surface | core `3.08065e-3`, surface `1.23226e-2` | core `1.8484e-2`, surface `7.3936e-2` | both finite nodes |
| surface -> player | surface `3.0e-2`, player `6.0e-4` | surface `8.0e-3`, player `4.0e-4` | both finite nodes |
| surface -> environment, inventory | surface `1.5e-2` | surface `4.0e-3` | surface only; environment is fixed |
| surface -> environment, dropped/placed | surface `2.4e-1` | surface `6.4e-2` | surface only; environment is fixed |

The corresponding isolated core/surface temperature-difference half-lives are
`45 s` for the warm stone and `7.5 s` for the hot-water bag. Inventory
surface/environment conductances are `3.0e-4 /s` and `2.0e-4 /s`; exposed
dropped/placed conductances are `4.8e-3 /s` and `3.2e-3 /s`. The environment
edge still connects only to the surface node, so the item surface rate also
depends on its `0.02` or `0.05` normalized surface capacity.

The normalized player-node delta is applied by
`PlayerTemperatureData.applyUniformBodyTemperatureDelta` to all five parts.
Because the five part capacities sum to `245000 J/K`, this preserves the
reservoir/player energy ratio and exchange speed while fitting the new body
energy representation. It does not advance `prevCoreBodyTemp`.

Only slot `curios:warm_stone` participates. Inventory reservoirs exchange with
air at half the surface/player rate; dropped single-item entities exchange at
eight times that rate and also consume bounded direct radiation. Placed
reservoir blocks use the same exposed exchange rate and radiation boundary. Unticked
containers pause. Normal tooltip shows the capacity-weighted mean
`(1-a)*T_core + a*T_surface`; advanced tooltip additionally shows both nodes.

Inventory air comes from `MinecraftThermalInput.gameplayPassiveEnvironment`;
dropped air comes from `gameplayItemEnvironment`. Both compose the generator
floor against local natural air before command/Boss controls, even without a
physical runtime. Equipped reservoirs initialize from composed player air and
then exchange with the five-part body; they have no additional direct
generator-heating step. See [world-climate-and-temperature.md](world-climate-and-temperature.md)
for composition order and dropped-item cache behavior.

Both items are also placeable blocks with the same registry IDs. Right-click a
supported surface to place one reservoir, oriented toward the player; creative
placement retains the held stack. The small ground models reuse the existing
item textures. Breaking the block or removing its support drops one item with
the current core/surface temperatures, custom name, and other original item
data. Pick block also returns this stored item state.

`ThermalReservoirBlockEntity` saves the complete single-item stack under
`ReservoirItem` using block entity type `frostedheart:thermal_reservoir`.
Server block ticks exchange once per `20 ticks` (one simulated second), staggered
by position, after at least `20` loaded ticks. Unloaded blocks pause; loading
does not simulate elapsed offline time. Fresh, uninitialized placements acquire
the effective environment temperature on their first exchange. Existing
temperatures survive placement and reload without reinitialization.

`MinecraftThermalInput.gameplayPlacedReservoirEnvironment` samples just above
the model at block-relative `(0.5, 0.3125, 0.5)` and shares the dropped-item
air/radiation cache and budgets. `DroppedReservoirExchangeHandler.exchangeInto`
owns both forms' core/surface update, including generator floors and bounded
direct radiation. Like dropped reservoirs, these blocks consume the environment
boundary without registering a new physical heat source in the world solver.

## Persistence And Synchronization

Player persistence writes:

- `thermal_schema = 1`;
- `difficulty`;
- each existing clothing `ItemStackHandler`;
- each part's new `energy_j`.

Old `temp`, `feel_temp`, `bodytemperature`, `envtemperature`,
`feeltemperature`, `blockTemp`, and `windStrengh` values are not migrated
into the new model. Loading an old player starts body energy at normal while
preserving clothing stacks, their complete item NBT, and temperature
difficulty. Environment observations are transient and are sampled again.

`FHBodyDataSyncPacket` is a 5-byte fixed payload: version byte, sampled Air
at `0.1 C`, and absolute core at `0.01 C`. Normal packets are sent only on the
configured temperature cadence and only when a quantized value changes. Login,
respawn, and dimension change force one complete state packet.
No player save schema or packet size changed for this balance adjustment.

## Hot-Path Bound

The body update is fixed `O(5)`. `HeatingDeviceContext` is created lazily
once per server-side player and owns the reusable sample, one clothing value,
and fixed five-element primitive arrays. `PlayerTemperatureComputation` has no
global mutable player scratch or per-update collection. Stateless domain classes
separate ownership: `PlayerThermalEnvironment` reads inputs,
`PlayerEquipmentHeating` traverses equipment, `PlayerThermoregulation` owns
physiological power and costs, `PlayerThermalInjury` owns damage, and
`PlayerThermalModel` owns heat-balance formulas. No retained `ThermalStep` or
other calculation carrier is added. Model parameters remain co-located with
their method groups, with units, medium precedence, energy conservation, and
equilibrium bounds documented at the formulas.
