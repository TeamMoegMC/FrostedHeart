# Player Temperature

- Status: `Current`
- Last verified: `2026-11-08` for equipment demand, stacking and reservoir implementation (validation recorded below); `2026-11-06` for body time/parameters and cold calibration; `2026-11-05` for heat/exposure separation and the orb's combined air/radiation reading. Earlier evidence retains its scope.
- Scope: player environment sampling, five-part body energy, wearable thermal reservoirs, clothing, Wet, heating equipment, thermometers, HUD, effects, persistence, and synchronization
- Primary code anchors: `PlayerTemperatureUpdate.updateTemperature`, `PlayerTemperatureComputation.updatePlayer`, `PlayerThermalEnvironment`, `PlayerEquipmentHeating`, `PlayerThermoregulation`, `PlayerThermalModel`, `PlayerThermalInjury`, `PlayerTemperatureData`, `ThermometerItem`, `CreativeThermometerItem`, `FHTemperatureDisplayPacket`, `WearableThermalExchangeHandler`, `ThreeNodeWearableHeatExchange`, `ThermalReservoirBlock`, `ThermalReservoirBlockEntity.serverTick`, `FHBodyDataSyncPacket`, `FrostedHud.renderTemperature`

The current body and HUD formulas differ from the pre-simulation model. The
user's target is comparable survival difficulty and heating benefits with the
current mechanisms. See the [2026-11-05 comparison](thermal-legacy-gameplay-parity.md)
for the baseline, balance concerns, outcome criteria, and validation limits.

The [2026-11-05 release logic validation](validation/thermal-player-release-logic-2026-11-05.json)
records 22 distinct focused cases with passing evidence on the production
code before the later display and numerical changes. The final player group passed 9/9 after fixture corrections; the other
13 passed in the preceding combined run. Failed attempts and their causes are
retained. This does not certify full legacy survival balance or a hundred-player
deployment.

The initial [survival and campfire measurements](thermal-gameplay-balance.md)
found substantial differences in cold survival time and thermoregulation food
cost. The current calibration changes are recorded there separately; the
initial results describe the earlier parameters, not the current constants.
The final calibration run on `2026-11-06` passed `22/22` focused entries,
including finite-food survival, 29 body traces, four real campfire scenes,
player logic, food-rate independence, and spring contact/recovery. The
selected normal-difficulty cold/wind outcome targets were met; full pack
gameplay and all difficulty levels retain their separate validation scope.

## Clothing attribute sampling cost (2026-11-13)

`BodyPartData.fillClothData` reads `ItemStack.getAttributeModifiers(part.slot)`
once per equipped item in that clothing sample, then uses the same Forge result
for insulation, wind, radiation and equipment weight. The next sample asks Forge
again; there is no persistent cache, new invalidation protocol, cadence change or
body-formula change. The previous four calls each rebuilt the event-derived map.

`equipmentThermalPropertiesUseOneFreshAttributeSnapshot` uses a real equipped
stack and Forge event, checks one event per sample, all modifier operations and
layer weights, then changes NBT on the same stack and checks the next result.
The initial control observed four events; the final 100-case combination passed,
including the existing player and heating-equipment checks. In the subsequent
100-client mixed run, the outermost player update averaged 0.656358 to
0.486493 ms per server tick; whole main-thread mean improved only 1.87% and
remains unqualified. The [validation receipt](validation/thermal-cell-enthalpy-validation-2026-11-14.json#/records/thermal-local-unload-clothing-2026-11-13)
records exact scope, workload differences and the incomplete original objective.

## Player-Facing Values

The temperature orb shows sampled Thermal Air plus a fixed-reference equivalent
of the incoming thermal radiation. Both its number and color use
`PlayerTemperatureData.getDisplayTemperatureC()`. The server computes this
once when publishing the observation; the client receives the final value:

```text
T_display_C = T_sampled_air_C + q_radiant_W_per_m2 * 0.8 / 6.0
```

`q_radiant_W_per_m2` is the already sampled non-negative incoming source flux,
`0.8` is the dimensionless reference absorptivity, and `6.0 W/(m2*K)` is the
reference transfer coefficient. This reuses `RadiantEquivalentTemperature`,
also used by the wearable reservoirs. It is a reference heating indication,
not a claim that the air, skin, or body reached that temperature. For example,
`12 C` air and `95.5 W/m2` display approximately `24.7 C`; zero incoming flux
leaves the air reading unchanged. Clothing, wind, contact media, and body state
do not enter this display conversion. The broader still-air environmental
equivalent remains a separate server diagnostic in `/temperature get`.

| HUD surface | Value | Meaning |
|---|---|---|
| Number | sampled Thermal Air plus equivalent radiant increment, `C` | local environmental heating reference, before entity attribute and Sauna adjustments |
| Orb color | the same combined value, `C` | the existing orb texture bands use the number's Celsius value, also when displaying Fahrenheit |
| Forecast current number | current chunk's `WorldClimate.getTemp(ChunkPos)` baseline, `C` | weather/climate value including local white-curtain effects, independent of room heating and radiation; excludes dimension, biome, and altitude additions |
| Body status/effects | body temperature offset from `37 C` | accumulated physiological danger |
| Mercury body thermometer | core body temperature, displayed to `0.1 C` | the existing held measurement flow uses the normal client temperature unit |
| Creative thermometer | raw absolute core body temperature, `C` | right-click reports immediately in every game mode without display quantization |

The number and color are never body temperature or `air - 37`.
`PlayerTemperatureData.getEnvTemp()` returns sampled Air; its previous
still-air equivalent is available through `getEnvironmentEquivalentTemperatureC()`
on the server. `FrostedHud.renderTemperature` reads the final combined value;
the forecast reads `ClientClimateData.climateTemperatureC` from `FHClimatePacket`.
Food cooling and cold-breath particles keep the sampled Air value. Clothing, Wet, movement, difficulty,
food, and equipment can change body power without changing the orb's reference
reading. The diagnostic command continues to show the still-air equivalent,
Air, radiation, wind, and net body power separately.

The forecast formerly reused the player's local Air number, so a heated room
could raise its current reading. It now displays the weather system's climate
baseline in the existing top forecast bar. This is distinct from outdoor air
after biome/altitude additions. The weather packet adds one `0.1 C` short,
using the existing login/respawn/dimension-change and climate-hour/clock-jump
sync. One climate hour is 1000 game ticks (50 seconds at 20 TPS); no new polling
loop is added. The displayed number is the last such climate sample.
The existing forecast visibility rule remains in `FrostedHud.renderSetup`:
`enablesTemperatureForecast=true` plus `ResearchVariant.HAS_FORECAST > 0`, or
`forceEnableTemperatureForecast=true`; an active boss bar hides the forecast.
The source default for `forceEnableTemperatureForecast` is `false`.

The additional local Air short in the body packet remains at the user's
explicit preference to preserve cold-breath and food/drink tooltip behavior.
`CupTempAdjustProxy.getHeat` still uses its existing Air-vs-37-C multiplier
rule, and `FoodTempStats` matches that server rule. This display/sync change
does not retune the drink mechanic.

Body-temperature severity remains separate from this display. A comfortable
orb reading does not guarantee normal body temperature, but clothed campfire exposure
cannot cause a direct burn merely because an integration equilibrium is large.
The combined value is never an input to body integration or direct injury.
The existing orb layout is retained; its three-digit magnitude saturates at
`999` in the selected display unit. Digits are split after rounding once to
one decimal, so `24.7` is not accidentally rendered as `25.7`.

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

The historical `player.unused` particle simulator, `TemperatureThreadingPool`,
and their block-information cache remain in source separately from the shared-grid
thermal runtime. Active player environment sampling uses the query above.
The server configuration no
longer registers `Surrounding Temperature Simulation` or its `simulationRange`,
`simulationDivision`, and `simulationParticleInitialSpeed` keys. These keys had
no effect on the active temperature system; player sampling continues through
the query above. `BlockTempData` and its recipe reload remain available to their
current consumers, without clearing the deleted simulator's cache.

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
cannot numerically jump through its boundary temperature. Body time is elapsed
game seconds multiplied by the single `GAMEPLAY_TIME_SCALE` of `8`; this is
gameplay acceleration, not another temperature unit. `temperatureChangeRate`
now scales only hot/cold food and drink effects and their tooltip, matching
its scope in the pre-simulation player update. Its source default remains `1`;
the companion pack's `0.5` no longer halves normal body exchange, equipment
thermal watts, or shivering/sweating resource costs. The body still updates
on `temperatureUpdateIntervalTicks` (default `20`), using elapsed ticks.

Passive air/contact conductances and their actual medium temperatures are
separate from radiant and equipment watts. `closedFormEnergyChangeJ` integrates
net power directly; it does not publish a power/conductance equilibrium point as
skin or exposure temperature. `getExposureTemperatureC` reports the passive
air/contact boundary, with no radiant power folded into it.

`PlayerThermalModel.preparePart` applies separate dimensionless source-default
exchange multipliers to its base conductances: Air `0.1`, water `0.51`,
powder snow `0.116`, Wet `0.03`, and lava `2`. Direct source radiation retains
its separate multiplier `2`, while conservative internal body-part transfers
retain `2`. The Wet multiplier applies to its additional `12 W/(m2*K)` path;
it remains active after leaving water until the Wet effect expires. These
values include a normal-difficulty cold-survival calibration; its measured
scope and tradeoffs are recorded in [the balance report](thermal-gameplay-balance.md).
The clothing multiplier, world source power, and thermal runtime retain their
existing ownership.
Full immersion and powder-snow contact have their own calibration; partial
immersion and strong wind still follow the current model. The still-air
environmental equivalent continues to be computed for diagnostics and is not
scaled by the body exchange multipliers. The orb's simpler air/radiation
conversion is also independent of these multipliers.

Air additionally uses `WIND_EXCHANGE_MULTIPLIER = 0.045` for legacy-experience
calibration. The effective air multiplier is
`0.1 + 0.045 * (h_convection - h_natural) / (h_convection + 4.7)`;
`h_convection = max(h_natural, h_forced)` and all `h` values are in `W/(m2*K)`.
The additional term is zero when natural convection dominates and approaches
`0.045` at high forced convection. Clothing wind proof reduces `h_forced` before
this calculation. The gain uses existing inputs, with no extra state or query.

## Contact, Wet, And Clothing

Ordinary water and lava use `player.getFluidHeight(tag) / player.getBbHeight()`.
The [hot spring](hot-spring.md) uses Forge's
`player.getFluidTypeHeight(FHFluids.HOT_SPRING.get().getFluidType()) /
player.getBbHeight()` because Forge tracks modded fluid contact by `FluidType`.
Each `BodyPart` owns a fixed vertical immersion band, so contact engages feet,
legs, torso/hands, and head progressively. Water uses a `0..35 C` boundary
derived from local air instead of the old fixed `x6` Celsius multiplier.
Hot spring contact uses `38 C` with the same water conductance, tissue,
clothing, and water-resistance paths; overlapping water/spring height is
counted once with spring precedence. Its dedicated
`PlayerThermalModel.HOT_SPRING_EXCHANGE_GAIN` (provisional `64`) multiplies
spring conductance only, preserving the 38 C boundary and normal body energy
integration. The spring temperature does not alter
ordinary water's formula or write a world heat source.
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
`PlayerTemperatureUpdate.isSoaked` includes the spring's Forge `FluidType`
height explicitly, since `isInWater()` alone excludes that modded fluid.
Spring soaking refreshes the same Wet duration and armored-clothing multiplier
as ordinary water. A single source block with air above has surface height
`8/9` block: a standing 1.8-block player has immersion ratio about `0.494`,
which reaches feet, most of the legs, and part of the hands, but does not
directly immerse the torso. See [hot-spring.md](hot-spring.md) for the recovery
fixture and its validation limits; 38 C is the contact boundary, not a core
temperature assignment or a guaranteed recovery time.

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
`100/460` the uninsulated conductance. Direct source radiation is an explicit
power input. Clothing insulation reduces its transfer into the body, and radiant
heat proof separately reduces absorbed radiation. It does not enter the passive
medium temperature or the direct burn thresholds. Wind proof and water
resistance retain their existing sources and layer weights. This conversion
reproduces clothing's relative exchange factor, not the removed model's
absolute body-temperature cadence, weather formula, or metabolic behavior.

`ClothData.sumAttributes` follows vanilla attribute operations with base zero:
sum additions, multiply by one plus the sum of `MULTIPLY_BASE` amounts, then
apply the product of all one-plus-`MULTIPLY_TOTAL` amounts. A +50% base modifier
increases the insulation score instead of halving it.

`PlayerThermoregulation` uses source-default basal power `32 W`, walking
adds `30.625 W`, and sprinting adds no power, matching the old executed sprint
branch. `FHTemperatureDifficulty.heat_unit` (`easy=2`, `normal=1`, `hard=0.5`,
`hardcore=0`) multiplies basal and movement power as well as regulation.
With food available, normal-difficulty core temperatures below `36.9`, `36.5`,
and `36.0 C` select `30`, `45`, and `60 W` of shivering. With water
available, core temperatures above `37.1` and `37.5 C` request `30.625` and
`61.25 W` of sweating; the shared evaporation ceiling may reduce applied
cooling. `FOOD_ENERGY_J_PER_EXHAUSTION = 2800 J` maps applied shivering energy
to food exhaustion; `SWEAT_ENERGY_J_PER_WATER_EXHAUSTION = 5444.444 J` maps
applied sweating energy to water exhaustion. These are separate gameplay
resource conversions. Regulation uses the current weighted core reading, not the
legacy per-part post-exchange deviation. The five parts remain independent.

## Equipment, Food, And Effects

`BodyHeatingCapability.tickHeating` now contributes explicit watts through
`HeatingDeviceContext.addPower`. These are available device watts, not an
unconditional addition to the body. Existing fuel, durability, heat-storage
capabilities, item stacks, and primary item NBT keys are unchanged. Device
resource use is scaled by real elapsed seconds, independently of
`temperatureUpdateIntervalTicks`; sub-second remainder uses the optional
`frostedheart:partial_heating_second` item key and is removed whenever it
returns to zero. A zero physiological time scale skips both equipment power and
resource use. Food converts its existing temperature delta into joules through
`PlayerTemperatureComputation.bodyEnergyForTemperatureDeltaJ` and applies the
existing minimum/maximum body offsets.

Ordinary equipment targets `37 C` for each heated part. In
`PlayerThermalModel.integratePart`, the existing exponential response is
calculated once and shared by environmental and equipment power:

```text
R = C/G * (1 - exp(-G*dt/C))     if G > 0; otherwise R = dt
E_other = P_other * R
E_equipment = min(P_equipment * R, max(0, C*(37 - T_part) - E_other))
E_change = E_other + E_equipment
```

`C` is that part's heat capacity in `J/K`, `G` its applied dry plus Wet
conductance in `W/K`, `dt` physiological seconds, and `R` the integrated energy
response in `J/W`. `P_other` is net environmental, radiant, metabolic,
thermoregulatory and fire power in `W`; `P_equipment` is the sum of positive
device powers in `W`. The limit applies to additional equipment energy, not
the body's stored temperature. Hot air, radiation, lava and fire can still
overheat the player; an already hot body cools normally. Internal transfers
continue to conserve energy after the part integrations. No additional
exponential or environment query is added to ordinary body integration.

| Equipment registry ID | Available output | Target and resource rule |
|---|---:|---|
| `frostedheart:hand_stove` | main hand `12 W`; offhand `110 W` | hands / torso; fuel and ash advance by elapsed seconds |
| `frostedheart:heating_pad` | `110 W` | torso; activated pad lasts `200 s` |
| `frostedheart:steam_bottle` | approximately `70 W` | torso; up to `1.5` stored heat units per elapsed second |
| `frostedheart:oxygen_candle` | `28 W` | torso; activated candle lasts `600 s` |
| `frostedheart:straw_briquette_red_mushroom`, `frostedheart:straw_briquette_brown_mushroom` | `3 W` | torso; the existing growth/durability clock runs only above `36 C` |
| `frostedheart:heater_vest` | up to `275 W` | torso; controlled steam consumption for the uncovered demand |

Fixed-output devices continue their established burn/discharge/growth clocks
while active and equipped; unused heat does not enter the body or become a new
world heat source. Their cold-state maximum output remains available. All
devices targeting the same part share one warming allowance, including devices
in Curios slots and both hands. `EquipmentTempStats` labels the displayed watts
as maximum warming power and explains demand-dependent transfer.

`PlayerEquipmentHeating.collect` visits fixed-output devices before capabilities
whose `isControlled()` returns true. `HeaterVestItem` is the controlled device:
`HeatingDeviceContext.remainingHeatingDemandW` subtracts other active equipment,
metabolism/regulation, radiation, applied Wet exchange and fire before requesting
steam. Its existing `55 W/K` feedback gain and maximum output remain, and the
request also respects the exact step's `37 C` allowance. The existing standby
charge applies only when the vest actually requests heat. Thus adding a pad or
stove cannot make the vest independently buy the same heat again.

Validation on `2026-11-08`: the final build retaining original float reservoir
storage passed `30/30` Forge entries, including eight equipment entries and
the existing 22 player/spring/cold-survival/room entries. The equipment matrix
contains 72 fixed-input, 20-minute traces with maximum part temperature
`37.179314 C`; real player tick and Curios ordering are separate checks.
See the [equipment validation record](validation/equipment-heating-2026-11-08.json)
and [balance report](thermal-gameplay-balance.md#2026-11-08取暖装备的舒适体温与叠加)
for conditions, original storage-rounding assertion failure, scope correction,
and the unrelated deposit-test compilation exclusion. This is not an actual
client or original-save validation.

Food limits apply in the direction of its energy: warming stops at the upper
limit and cooling stops at the lower limit. An already out-of-range part is not
teleported back into the range; zero energy leaves all parts unchanged. The
heater vest combines signed target-temperature correction with environmental
loss, including incoming radiation. It does not continue replacing normal
cooling when an overheated body already needs to lose heat.

The established body-part effect thresholds still consume offsets relative to
`37 C`: torso drives hypothermia/hyperthermia, head drives confusion, lower
limbs drive slowness, and hands drive mining slowdown. The `INSULATION` effect
freezes the ordinary environment/physiology body step, but an equipped thermal
reservoir still exchanges with the player afterward. Creative mode, spectator
mode, and invulnerability skip wearable exchange as well as climate injury.

The temperature-owned `HYPOTHERMIA` and `HYPERTHERMIA` effects follow the current
torso offset on each player update. They change level/sign immediately and end
when torso temperature returns within one degree of 37°C. Vanilla confusion,
slowness and mining fatigue may have other owners and retain their original
expiry behavior. Heavy equipment adds/updates its effect only after checking
the current instance; removing the weight clears the effect.

Equipment weight (verified 2026-11-13, source defaults): `BodyPartData.fillClothData`
counts occupied valid layers per body part, outer equipment first, then occupied
lining slots in order; gaps do not select a heavier multiplier. The outer-to-inner
weight factors are `[1]`, `[1.1, 1.7]`, `[1.5, 2.3, 3.5]`, and
`[1.8, 3, 4.8, 7]` for one through four layers. These are separate from insulation
factors. `ClimateCommonEvents.insulationDataAttr` exposes data-defined outer
armor weight as an additive attribute, matching lining data.

`FHRecipeProvider` defines full five-part clothing base totals: hay/straw and
wool 1.1, rabbit/kelp/gambeson 3.6, wolf 4.5, fox 5.4, hide/buff coat 7.2,
and polar bear 11.2. Per-part base weight is total times body area. Polar bear
armor is accepted by the `inner_linning` item tag. Vanilla leather/chainmail/gold/
iron/diamond/netherite four-piece sets have explicit totals 3.6/5.4/6.3/7.2/9/10.8,
normalized over their combined area 0.95. Other armor retains the existing fallback.
Weight is a gameplay unit. Total weight at most 10 has no heavy-load effect;
above 10, amplifier is `floor((weight - 10) / 5)` (displayed level amplifier + 1).
Iron plus full hay/wool is approximately 9.7515; hide plus wool 9.79; hide plus
rabbit 13.518; reversed rabbit plus hide 15.75 (one lining glove,
using the inner set glove; hands cannot wear two lining gloves); polar bear alone 11.2.

Direct severe-exposure injury uses `getExposureTemperatureC` and clothing
transmission, preserving the ±100/150/200/250°C bands. The expected hit count
scales with real elapsed seconds rather than the number of configured updates.
Radiation enters body energy and can lead to actual body overheating; a large
computed equilibrium point cannot directly trigger an instant burn. Vanilla
fire/lava contact damage remains separate, without an extra thermal hit while
the entity is on fire/in lava. `/temperature get` labels each part's reading
`Exposure`, and `/temperature set exposureTemp` controls this diagnostic input.

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

The normalized player node uses `PlayerTemperatureData.getMeanBodyTemperatureC`,
the body's total energy divided by `245000 J/K`, referenced to `37 C`.
`getEquipmentWarmingEnergyJ` sums the energy deficits of parts below `37 C`.
The wearable overload of `ThreeNodeWearableHeatExchange.exchangeInto` limits
positive surface-to-player transfer to this remaining demand on every substep;
unused energy stays in the reservoir surface and can still exchange with its
core. This is a transfer limit, not a clamp on stored body or item temperature.

`PlayerTemperatureData.applyReservoirBodyEnergyJ` distributes received warming
in proportion to the cold parts' energy deficits. Already warm parts receive
none, so a hot torso does not prevent cold hands from benefiting or get heated
again itself. Negative transfer from a cold item remains possible and is
distributed by part heat capacity. Item plus body energy is conserved, including
the residual from limiting accepted positive energy. The exchange still uses
real elapsed seconds, retains the existing capacities/rates and save schema,
and does not advance `prevCoreBodyTemp`. At normal body temperature, a warm
reservoir retains surplus heat for later demand instead of causing overheating.

`WearableThermalState.writeTo` retains the existing NBT float storage for
`core_temperature_c` and `surface_temperature_c`. Version `1`, key names and
numeric reading remain unchanged, including the companion pack's `60 C`
hot-water-bag recipe. Exchange conservation checks distinguish double-precision
calculation from the existing float-storage rounding; this work does not change
the item's storage precision or payload size.

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

`FHBlocks.WARM_STONE/HOT_WATER_BAG` register each `WarmStoneItem` through the
block builder's `.item(...)`. Blocks use the standard
`block.frostedheart.warm_stone` and `block.frostedheart.hot_water_bag` translation
keys; items retain `item.frostedheart.warm_stone` and
`item.frostedheart.hot_water_bag`. Registrate generates both sets
without disabling block language generation. The normal `dropSelf` loot tables
are generated under `data/frostedheart/loot_tables/blocks/`; ordinary block
breaking still uses `ThermalReservoirBlock.getDrops` to preserve the complete
stored stack, rather than replacing it with a fresh item.

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
Resetting old body temperature is intentional release policy; no old-temperature
migration is required. Exposure is also cleared on load so a previous life or
pre-load environment cannot trigger a stale exposure hit.

`FHBodyDataSyncPacket` version `2` is a 7-byte fixed payload: version byte,
sampled Air at `0.1 C`, absolute core at `0.01 C`, and the final orb reading
at `0.1 C`. Raw radiation is server observation state and is not synchronized.
Normal packets are sent only on the configured
temperature cadence and only when quantized Air, core, or the combined orb
reading changes. A radiation-only change or its return to zero can therefore
update the orb even at constant Air and core. Login, respawn, and dimension
change force one complete state packet. Reset, death reset, and load clear
transient radiation and the final display observation. The existing
`setEnvTemp` command path refreshes the server's final reading as well.
No player save schema changes.

Compared with the original 5-byte packet, the orb adds two payload bytes per
transmitted body packet, at most `2 bytes * 100 players = 200 bytes/s` at the
default 20-tick cadence and 20 TPS, before network framing. Compared with the
intermediate raw-radiation packet, it removes two bytes and the client-side
radiant conversion. Rendering reads the synchronized scalar; it adds no rays,
world queries, scans, or packets per render frame. Client and server must use the
updated packet implementation together.

Focused validation on `2026-11-05`: current-source `assemble compileGameTestJava`
passed, then `frostedheart_player_release` passed `13/13` in
`build/thermal-orb-radiation-20261105-r2.log`. This includes the original nine
body/clothing/injury cases, real campfire display input, radiation-only sync,
the final 7-byte packet round-trip/client-state application, zero-flux removal,
reset/load, air-only sync at constant orb value, and the independent climate
packet reading/round-trip/clear. The earlier r1 validated the intermediate
9-byte raw-radiation design and does not describe the final protocol.
Thermal runtime errors were zero and final shutdown `rootBytes=0`. Client HUD
rendering was compiled and reviewed; no actual client screenshot was taken.
These checks preceded the numerical calibration; its later validation and
remaining balance limits are recorded in [the balance report](thermal-gameplay-balance.md).

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
