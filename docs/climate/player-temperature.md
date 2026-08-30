# Player Temperature

- Status: `Current`
- Last verified: `2026-08-30`
- Scope: player environment sampling, five-part body energy, clothing, Wet, heating equipment, HUD, effects, persistence, and synchronization
- Primary code anchors: `TemperatureUpdate.updateTemperature`, `TemperatureComputation.updatePlayer`, `PlayerThermalEnvironment`, `PlayerEquipmentHeating`, `PlayerThermoregulation`, `PlayerThermalModel`, `PlayerThermalInjury`, `PlayerTemperatureData`, `BodyPartData`, `HeatingDeviceContext`, `FHBodyDataSyncPacket`, `FrostedHud.renderTemperature`

## Player-Facing Values

The temperature orb uses the environmental equivalent temperature for both its
number and its color. This keeps the HUD's existing visual language stable:
the number shows the value and the orb texture shows its cold-to-hot band.

| HUD surface | Value | Meaning |
|---|---|---|
| Number | environmental equivalent temperature, `C` | the still-air temperature that would produce the current immediate environmental heat exchange |
| Orb color | environmental equivalent temperature, `C` | the existing orb texture bands are selected from the same Celsius value as the number |
| Body status/effects | body temperature offset from `37 C` | accumulated physiological danger |

The number and color are never body temperature and never `air - 37`.
`FrostedHud.renderTemperature` passes the environmental equivalent Celsius
value to the existing orb texture thresholds. Clothing, Wet, movement,
difficulty, food, and equipment can change body power without changing either
HUD temperature presentation. Net body power remains available to the server
diagnostic command and body calculation, but is not sent for HUD color.

## Cadence And Environment

`temperatureUpdateIntervalTicks` defaults to `20`.
`TemperatureUpdate.shouldUpdatePlayer` assigns each UUID a stable phase so
players are distributed across the interval. Each update performs one
`MinecraftThermalInput.gameplayPlayerEnvironment` query using a reusable
`ThermalEnvironmentSample`.

The query returns absolute Thermal Air in degrees Celsius and direct radiant
flux in `W/m2`. `FHAttributes.ENV_TEMPERATURE` modifiers and the existing
Sauna effect are then applied to the player's local air boundary. Outdoor wind
is `WorldTemperature.wind * 19.444 / 100 m/s`; the initial indoor model applies
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

Air, long-wave exchange, direct source radiation, clothing resistance, contact
media, Wet, metabolism, movement, thermoregulation, and equipment all enter one
power balance in watts. `TemperatureComputation.updatePlayer` integrates that
balance through five visible phases: environment sampling, contact preparation,
active-power collection, body integration, and observation publication. Its
separate stateless `PlayerThermalModel` owns the formulas,
including the closed-form exponential step, so passive water or lava contact
cannot numerically jump through its boundary temperature. The configured
`temperatureChangeRate` multiplies one explicit `GAMEPLAY_TIME_SCALE` of
`8`; this is the gameplay acceleration, not another temperature unit. At the
default rate, a naked dry player in calm `-15 C` Air is intended to cross the
first torso cold threshold after roughly `45..60 s`; water and exposed wind
remain faster because they have independent transfer coefficients.

## Contact, Wet, And Clothing

Water and lava use `player.getFluidHeight(tag) / player.getBbHeight()`.
Each `BodyPart` owns a fixed vertical immersion band, so contact engages feet,
legs, torso/hands, and head progressively. Water uses a `0..35 C` boundary
derived from local air instead of the old fixed `x6` Celsius multiplier.
Powder snow and on-fire state remain local body inputs.

These local entity contacts do not write Thermal Air or register a world source.
Nearby ambient lava and ordinary-fire heating through Thermal Mesh/Page
topology is not implemented by this player-temperature pass. Campfire retains
its existing physical-source and radiation path unchanged.

The existing Wet effect remains the only post-exit wetness state. Leaving water
removes the water-contact conductance on the next player update; Wet continues
its extra exchange until the existing effect expires. Wet heat loss and
sweating share one low-cost evaporation ceiling.

`BodyPartData.fillClothData` reads existing equipment attributes and
`ArmorTempData` layers directly into one reusable `PartClothData`. It creates
no per-update list. Legacy insulation recipe values are converted once during
calculation with `LEGACY_INSULATION_TO_RESISTANCE = 0.0002 m2*K/W`.
Wind proof, water resistance, and radiant heat proof retain their existing data
sources.

## Equipment, Food, And Effects

`BodyHeatingCapability.tickHeating` now contributes explicit watts through
`HeatingDeviceContext.addPower`. Existing fuel, durability, heat-storage
capabilities, item stacks, and primary item NBT keys are unchanged. Device
resource use is scaled by real elapsed seconds, independently of
`temperatureUpdateIntervalTicks`; sub-second remainder uses the optional
`frostedheart:partial_heating_second` item key and is removed whenever it
returns to zero. A zero physiological time scale skips both equipment power and
resource use. Food converts its existing temperature delta into joules through
`TemperatureComputation.bodyEnergyForTemperatureDeltaJ` and applies the
existing minimum/maximum body offsets.

The established body-part effect thresholds still consume offsets relative to
`37 C`: torso drives hypothermia/hyperthermia, head drives confusion, lower
limbs drive slowness, and hands drive mining slowdown. The `INSULATION`
effect, creative mode, spectator mode, and invulnerability continue sampling
the environment but freeze body-energy changes and suppress climate injury.

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

`FHBodyDataSyncPacket` is a 5-byte fixed payload: version byte, environment
at `0.1 C`, and absolute core at `0.01 C`. Normal packets are sent only on the
configured temperature cadence and only when a quantized value changes. Login,
respawn, and dimension change force one complete state packet.

## Hot-Path Bound

The body update is fixed `O(5)`. `HeatingDeviceContext` is created lazily
once per server-side player and owns the reusable sample, one clothing value,
and fixed five-element primitive arrays. `TemperatureComputation` has no
global mutable player scratch or per-update collection. Stateless domain classes
separate ownership: `PlayerThermalEnvironment` reads inputs,
`PlayerEquipmentHeating` traverses equipment, `PlayerThermoregulation` owns
physiological power and costs, `PlayerThermalInjury` owns damage, and
`PlayerThermalModel` owns heat-balance formulas. No retained `ThermalStep` or
other calculation carrier is added. Model parameters remain co-located with
their method groups, with units, medium precedence, energy conservation, and
equilibrium bounds documented at the formulas.
