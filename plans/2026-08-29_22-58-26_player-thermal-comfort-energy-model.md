# Player Thermal Comfort And Body Energy Architecture

- Time: `2026-08-29 22:58:26 +08:00`
- Last revised: `2026-08-30`; restored historical Wet semantics, added
  topology-aggregated ambient lava/fire heat, selected the initial binary
  `canSeeSky` wind gate, defined contact media, bounded integration, legacy
  effect migration, exposure timing fixtures, and the split HUD contract where
  the number shows environmental equivalent temperature while orb color shows
  the player's current net body-power direction; shortened the naked `-15 C`
  mild-hypothermia target to roughly one minute; first implementation pass now
  completes the player body/HUD/network path while world ambient lava/fire
  topology remains deferred
- Authors: `Codex; OpenAI GPT-5; architecture and implementation planning`
- Status: `in-progress`
- Scope: `ambient lava/fire Air operations and radiation payloads, player environment sampling, environmental equivalent temperature, body heat balance, clothing, wetness, equipment heating, food temperature, HUD, persistence, networking, effects, commands, tests, and performance validation`
- Related: [`design/creative-principles.md`](../design/creative-principles.md), [`design/world-design.md`](../design/world-design.md), [`docs/climate/README.md`](../docs/climate/README.md), [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md), [`docs/climate/world-climate-and-temperature.md`](../docs/climate/world-climate-and-temperature.md), [`docs/climate/heat-production-and-network.md`](../docs/climate/heat-production-and-network.md), [`thermal async runtime plan`](2026-08-28_01-18-39_thermal-async-runtime-topology-refactor.md)

## Goal

Replace the current mixed-unit player temperature path with one scientifically
coherent, allocation-free heat-balance model while keeping the player-facing
experience simple:

```text
temperature-orb number = how cold or hot this place feels
temperature-orb color = whether this player is currently gaining or losing heat
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
| HUD temperature-orb color | net body power after clothing, Wet, contact, metabolism, and equipment, `W` | blue means losing heat, neutral green/white means approximately stable, and orange/red means gaining heat; intensity communicates rate |
| hypothermia/hyperthermia bar and effects | body thermal state | whether current clothing, metabolism, wetness, and exposure are sufficient |
| `TemperatureProbe` | physical air temperature, `C` | actual Thermal Air value at the probed point |
| `ThermometerItem` | core body temperature, `C` | actual physiological core temperature |
| admin temperature command | all named values plus `W`, `W/m2`, and confidence | diagnostics; never a gameplay formula source |

The default Celsius interpretation bands describe the displayed number; they
do not select orb color and are not instant damage thresholds:

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
  parameters is about `701,056 B` (`0.67 MiB`) per dimension. This plan does not
  add a second radiation cache or source index.
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

The player-side production path has exactly four owners. Existing Thermal Mesh
hot-boundary compilation is upstream of this path:

```text
MinecraftThermalProfiles + BrickMaterialKernel
  own aggregated lava boundaries, fire powers, and hot Brick radiation payloads
             |
             v
MinecraftThermalInput
  owns Air temperature and direct-radiation observation
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

The existing Campfire implementation is outside this ambient-heat change and
must not be modified: its source registration, `8,000 W` split, Air port,
radiation entry, mutation handling, and receiver cache remain the authorities.
This plan neither converts Campfire to a boundary nor adds a second Campfire
contribution.

## Ambient Lava And Fire Heat

Lava and ordinary fire need ambient effects before contact. Implement those
effects as topology-owned aggregated Air operations and hot-radiation payloads
in already admitted Pages, not as one
`PhysicalSourceSpatialIndex`/`ThermalSourceLedger` entry per block.

`AmbientHeatProfileRegistry` is not a replacement source registry. It must
explicitly exclude Campfires and must not alter any Campfire code, data,
configuration, registration, power split, Air port, radiation entry, or
lifecycle. Reusing the receiver-side radiation visitor means only that one
bounded query can observe both the unchanged physical-source entries and the
new immutable Brick patches.

### Air Heating

Compile hot world state through `MinecraftThermalProfiles` and
`BrickMaterialKernel` with the heat owner's correct form:

- add one immutable `AmbientHeatProfileRegistry` to the server-wide compiled
  profile snapshot and one `ambientHeatProfileId` channel to
  `ResolvedThermalSignature`/`SignatureMetadata`; zero means none. Pages still
  store only interned signature IDs, so this adds no per-block Page field or
  runtime object. Tag classification assigns lava, ordinary fire, and soul fire
  profiles; Campfire signatures keep ambient profile zero;

- tag-matched lava keeps its conservative non-Air fluid geometry and uses a
  `1000 C` boundary plus `lavaAirConductanceWPerM2K`;
- only lava faces adjacent to resolved Air contribute; fully internal lava has
  zero operation and zero retained hot-boundary payload;
- the compiler sums all exposed `4x4` microface patches targeting the same Air
  arena cell inside one Brick, then emits one fixed-boundary operation for that
  target, not one operation per lava block or microface;
- no lava material pole, reservoir, source descriptor, source generation,
  ledger accumulator, or periodic lava scan is created;
- tagged ordinary fire blocks, excluding Campfires, keep their resolved Air
  geometry. They declare `fireBlockRatedPowerW` with convection/radiation shares
  summing to one. The compiler sums the convective watts of all fire blocks
  resolving to the same Brick Air cell and emits one primitive
  `ThermalFragment.ConstantPowers` operation for that cell. The normal solver
  update is exactly `delta_energy_J = power_W * dt_seconds`; it needs no source
  descriptor or retained accumulator;
- lava flow and fire spread/extinguish use the existing position mutation,
  signature difference, one-Block dependency closure, and 20-tick coalescing.
  Steady lava boundaries execute only their precompiled one-second coefficients,
  while steady ordinary-fire cells execute one aggregated constant-power write.

The resulting Air then travels through the existing Air pairs, buoyancy,
FarField, doors, and shelter geometry. A player beside but not touching lava
therefore receives the hotter published Air through the ordinary single O(1)
query. A wall can block transport, and a room can retain heat, without a
player-specific lava-distance formula.

### Direct Radiation

Hot lava and open flame also radiate. Reuse `RadiationService` without creating
another source index or receiver cache:

- topology compilation reduces all exposed lava area and ordinary-fire radiant
  power in one Brick to at most one immutable hot-radiation patch with an
  area/power-weighted centroid;
- lava patch power is unit-bearing:

```text
lava_radiant_power_W =
    exposed_area_m2
  * effective_lava_emissivity
  * stefan_boltzmann_W_per_m2K4
  * ((T_lava_C + 273.15)^4 - (T_radiation_reference_C + 273.15)^4)
```

- ordinary fire contributes its declared radiative share; Campfire contributes
  nothing to this patch because its existing `1,600 W` radiation entry already
  owns that energy;
- hot patches live in the existing immutable Page/Brick publication and use
  Page/Brick revision as witness identity. `RadiationService.SourceIndex`
  visits the current section's physical sources and published hot patches
  through one composite visitor; patches never enter
  `PhysicalSourceSpatialIndex` or `ThermalSourceLedger`;
- hot patches share the existing `64` candidate visits, top `8` candidates,
  `24` rays, occlusion witnesses, and range. They add no ray and no second
  receiver cache. Internal lava has no exposed area and therefore no patch.

Use `effective_lava_emissivity` in `[0,1]`, fire convection/radiation shares
that sum to one, and the existing minimum ray distance. Calibrate gameplay at
fixed one-, four-, and eight-block visible/occluded fixtures; do not add an
arbitrary Celsius bonus or a second environmental field. The hot-patch payload
must remain primitive and no larger than `48 B` per exposed hot Brick before
array headers; a flat lava surface normally produces one patch per surface
Brick, while completely submerged/internal lava produces none.

Lava Air conductance and lava radiation are separate physical channels: the
fixed boundary writes only Air, while the published patch contributes only
receiver flux. Ordinary fire's declared split similarly sends each joule to
exactly one channel. Neither path duplicates Campfire energy.

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
  hot Brick patches, `W/m2`;
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
- a blocked physical source or hot Brick patch contributes zero direct flux but
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
- net body power and warming/cooling trend;
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

Device fuel/energy extraction must state its conversion to joules or watts.
For a device delivering `P W`, the item spends `P * physiological_seconds J`
through the same gameplay time scale used by the body. Tooltip min/max values
change from temperature additions to power and expected duration. No
compatibility adapter may continue adding Celsius after the production switch.

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
warming/cooling trend                signed short, quantized W
status flags                         byte
```

The fixed payload is approximately `7..10 B` before channel framing, versus an
allocated CompoundTag every tick. The client displays the latest synchronized
sample through the existing HUD presentation path and never feeds a value back
to gameplay. Additional interpolation, prediction, or smoothing work is outside
this plan.

`FHBodyDataSyncPacket` may keep its registry ID for protocol continuity within
the mod if its encoding is replaced atomically. Do not maintain simultaneous NBT
and binary production packets.

## HUD And Diagnostics

- `FrostedHud.renderTemperature` draws the synchronized environmental
  equivalent temperature as the number already placed directly on the orb.
- Orb color is player-specific and consumes synchronized net body power after
  clothing, Wet, contact media, metabolism, regulation, and equipment. Cooling
  is blue, approximately stable is neutral green/white, and warming is
  orange/red; color intensity increases monotonically with net-power magnitude.
- The number and color deliberately have different owners. Two players in the
  same `-20 C` environment see the same number, while suitable clothing may
  make one orb neutral and leave the other strongly blue. Clothing, difficulty,
  and body state never alter the displayed environmental Celsius value.
- Use a configurable symmetric power deadband around zero and a short
  client-only direction hold so one-second quantization near equilibrium does
  not alternate the orb between cooling, neutral, and warming. The hold changes
  only presentation; gameplay and synchronized power use the unsmoothed model
  result.
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
refresh it only through the existing config/reload lifecycle. Ambient
lava/fire parameters belong to the existing immutable
`MinecraftThermalProfiles`/engine-generation snapshot, not the player snapshot;
changing them follows the existing thermal-runtime rebuild lifecycle. Parameter
defaults must be centralized; configuration mirrors them rather than defining a
second default set.

Required parameter groups:

- human reference: core temperature, reference skin temperature, surface area,
  core heat capacity, tissue resistance;
- environment transfer: long-wave coefficient, reference air velocity,
  wind-scale-to-`m/s`, radiation absorptivity, liquid-water coefficient and
  temperature bounds;
- ambient world heat: shared `1000 C` lava temperature,
  `lavaAirConductanceWPerM2K`, `effectiveLavaEmissivity`, radiation reference
  temperature, ordinary-fire rated power, and convection/radiation shares;
  Campfire uses none of these parameters;
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
- display: Celsius comfort bands, warming/cooling power deadband, and network
  quantization.

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
- Do not increase Page admission merely to calculate body state; retain current
  natural fallback behavior while a first player Page is pending.
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
- Ambient lava/fire adds no `PhysicalSourceSpatialIndex` descriptor,
  `ThermalSourceLedger` binding, material pole, or per-block runtime state.
  Exposed lava compiles to one existing fixed-boundary operation per unique
  target Air cell in a Brick; ordinary fire compiles to one constant-power
  operation per target Air cell. Direct radiation stores at most one primitive
  hot patch per exposed Brick, with a retained target of `<=48 B/patch` before
  array headers; internal lava stores none.
- `ambientHeatProfileId` is stored once per interned global signature; Page
  Bricks continue storing only signature IDs, so no extra field is multiplied by
  loaded block count.
- Primitive retained payload targets are `32 B` per aggregated lava fixed
  boundary (`cell`, generation, temperature, conductance, coefficient) and
  `16 B` per aggregated ordinary-fire power (`cell`, generation, watts), before
  amortized array headers. Both count against existing fragment operation and
  engine memory limits during topology preparation.
- Keep the current radiation reservation and receiver ownership initially.
  More than `128` simultaneously active receiver caches is a measured follow-up,
  not a speculative capacity increase.
- Sync at most once per completed model update plus lifecycle events, never once
  per player tick.

For `100` players at one update per second, the new body model executes only
`500` part calculations and `500` scalar `expm1` calls per second. Radiation
retrace remains the dominant worst-case cost (`6144` DDA
steps/player/update before witness reuse). Hot Brick patches share the existing
`64` visits/top-`8`/`24`-ray budget and section traversal. The model must not add
another ray, section traversal, block scan, receiver cache, source ledger entry,
or per-block source registration for fire/lava/powder snow.

## Playability Contract

The body model is physically structured but intentionally forgiving and
legible. It must create decisions, not continuous maintenance work.

- Environmental equivalent temperature reacts within the next model update
  (`<= 1 s` at the default cadence) to wind, shelter, source visibility, and
  extinguishing, so player actions have immediate visible feedback.
- Core temperature changes slowly. The first adverse state is the orb's
  cooling/warming color, not damage or a severe movement penalty.
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
- Fast feedback is derived from the orb's environmental equivalent number,
  net-body-power color, and the existing five part-energy states. Do not add a
  second fictitious comfort temperature, recovery reservoir, extra arrow, or
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
| exposed lava without contact | one/four/eight-block fixtures have strictly decreasing direct flux; adjacent Air warms through the Mesh; visible exposure is hotter than stone-wall occlusion; one-block exposure warns within the next accepted thermal cut, gives at least `3 s` reaction before climate direct-burn damage, and becomes dangerous under sustained `10..20 s` exposure; four-block exposure is useful warmth in cold Air without direct-burn damage during the first minute |
| ordinary open fire without contact | heats its containing/adjacent Air and contributes its declared radiative share; Campfire readings remain unchanged |
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
immediate vanilla hazard. They are calibration outcomes from Air conductance,
radiant flux, the body model, and existing damage thresholds, never timers or a
distance-based player bonus.

## Implementation Sequence

The body-model production switch is atomic. Intermediate implementation commits
may exist on the development branch, but no release or merged state may run old
and new body authorities in parallel. The upstream lava/fire ambient-heat
work has one existing Air/radiation authority and may land first with its own
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

### Stage 1: Ambient World Heat And Pure Model

1. Add `AmbientHeatProfileRegistry` and the interned-signature
   `ambientHeatProfileId` channel, then extend `BrickMaterialKernel` to aggregate
   lava surface contacts into the existing fixed-boundary execution. Add the
   minimal primitive
   `ThermalFragment.ConstantPowers` execution for ordinary-fire convective watts,
   with operation-limit, reference, commit, forward/reverse, and unload tests.
2. Publish at most one primitive hot-radiation patch per exposed Brick and make
   the existing `RadiationService.SourceIndex` visitor compose physical sources
   with current Page patches under the unchanged budgets. Do not register those
   patches in `PhysicalSourceSpatialIndex` or `ThermalSourceLedger`.
3. Add `PlayerThermalParameters` and pure `PlayerThermalModel`.
4. Implement convective coefficient, environmental equivalent temperature,
   the caller-provided binary `canSeeSky` wind gate, operative temperature,
   five-part clothing paths, metabolism, conservative clamped inter-part
   transfer, water/lava part-band contact, powder-snow contact, binary Wet
   exchange, local on-fire power, shared evaporation capacity, and closed-form
   one-step integration with explicit units.
5. Add deterministic unit tests over normal and extreme inputs.
6. Keep the production caller unchanged until state and all integrations are
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
4. Route environmental equivalent temperature to the number already drawn on
   the HUD orb, and route net body power to the orb's existing color area. Do
   not add a separate trend arrow or reserve new HUD space.
5. Keep `TemperatureProbe` on physical Air and forecast on climate/air data.
6. Preserve current `INSULATION`, creative, spectator, and invulnerable freeze
   and effect semantics. Keep vanilla direct fire/lava damage, suppress only the
   duplicate climate direct-burn path during those active contacts, and retain
   vanilla powder-snow freezing.
7. Verify that entity fire/lava/powder inputs are primitive local contact reads
   and do not mutate Air or radiation ownership. Ambient lava/fire heat must
   arrive only through the upstream compiled boundary/hot-patch publication,
   never from `TemperatureUpdate`.
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
   trend presentation, never by fabricating extra source power or Celsius.
4. Calibrate full `0 C` immersion and exposed strong-blizzard curves to their
   declared windows. Fit `onFireHeatPowerW`, lava contact coefficient, and
   powder-snow contact coefficient to the Stage 0 curves where current behavior
   is useful, preserving vanilla direct-damage ownership.
5. Calibrate lava Air conductance, effective emissivity, and ordinary-fire
   power/split at the one/four/eight-block visible/occluded fixtures, including
   the one-block reaction/danger and four-block no-direct-burn windows. Preserve
   Campfire Air, flux, source count, and response curves exactly.
6. Update all climate living documents, exact config/data anchors, commands,
   formulas, units, defaults, and the development diary.

## Validation Matrix

### Thermal Mesh Ambient-Heat Tests

- interned signatures distinguish Air, lava, ordinary fire, and soul fire
  through `ambientHeatProfileId`; irrelevant fire age/flow-state changes with
  identical thermal semantics reuse a signature; Campfire ambient ID is zero;
- one exposed lava face compiles a `1000 C` fixed boundary to adjacent Air;
  fully internal lava compiles no boundary and allocates no material pole;
- many lava microfaces targeting one Brick Air cell aggregate to one boundary
  with summed `W/K`, and the compiled one-second coefficient matches the generic
  boundary kernel;
- ordinary fire resolves to existing Air geometry, aggregates declared
  convective watts by target Air cell, applies `P*dt` once, preserves its
  convection/radiation split, and disappears on extinguish;
- lava flow and fire churn rebuild only the exact existing Brick dependency
  closure after mutation coalescing; no source descriptor, ledger binding, or
  accumulator is created;
- an exposed hot Brick publishes at most one primitive radiative patch with
  finite centroid/power; internal lava publishes none; replacing or unloading
  the Brick removes the patch with its ordinary publication lifecycle;
- published lava flux follows area and inverse-square ordering at the fixed
  one/four/eight-block fixtures, and stone occlusion removes the direct term;
- physical sources plus hot patches still obey the existing `64` candidate
  visits, top `8`, `24` rays, range, confidence, and witness invalidation;
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
- changing clothing, Wet, metabolism, or equipment may change orb-color power
  while leaving the displayed environmental equivalent number bit-identical;
- net body power below/inside/above the symmetric display deadband selects
  cooling/neutral/warming before client-only hold, with monotonic color
  intensity outside the deadband;
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
- dry/wet and low/high clothing resistance change orb color/body trend but not
  the HUD environmental number;
- shallow water affects feet before higher body bands; partial and full
  immersion use the declared area fractions and water boundary;
- leaving water removes strong water exchange within one model update while the
  existing Wet effect continues its full binary extra exchange until it expires;
- the full `0 C` immersion and strong-blizzard scenarios meet the approved
  timing windows;
- a player beside but outside lava observes hotter published Air and visible
  hot-patch flux; a stone wall removes direct flux while Air follows ordinary
  Mesh transport;
- the nearby-lava fixture warns on the next accepted cut, gives at least `3 s`
  before climate direct-burn damage at one block, becomes dangerous under
  sustained `10..20 s` exposure, and causes no direct-burn damage at four blocks
  during the first minute;
- entering partial/full lava adds the declared local body contact on top of the
  already sampled ambient Air/flux; the player contact calculation itself does
  not write Air, register a source, or publish a radiation patch;
- on-fire, post-lava fire, and powder-snow contacts produce their declared local
  part curves without changing Thermal source count or source watermark;
- ordinary fire heats Air and publishes only its declared hot-patch radiation;
  the new ambient-heat implementation does not touch Campfire code,
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
- run separate admitted-Page fixtures for a flat exposed lava surface, fully
  internal lava, and coalesced ordinary-fire spread/extinguish; compare solver
  operations, topology allocation, Page payload bytes, candidate visits, and
  source-ledger size against equivalent no-ambient-heat Pages;
- compare update CPU, allocation rate, packet allocation, radiation cache reuse,
  and level-thread P95/P99 against the current build;
- run retained-heap checks after repeated login/logout and dimension changes;
- verify no new per-player collection, queued task, stale receiver, or packet
  state remains after logout;
- use external JFR/heap evidence only; do not add production instrumentation.

## Acceptance Criteria

The plan is complete only when all of the following are true:

1. The HUD number is an absolute environmental equivalent Celsius value and
   never core body temperature or a `37 C`-relative offset. The same orb's color
   independently shows player-specific net body power with a deadband and short
   client-only hold; no separate trend arrow or extra HUD space is added.
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
14. Ambient lava and ordinary fire heat nearby published Air through
    topology-aggregated lava fixed boundaries and ordinary-fire constant powers,
    and contribute at most one hot radiation patch per exposed Brick. They
    create no per-block Thermal source, source-ledger binding, extra
    ray/cache/index, material pole, or periodic scan; internal lava creates no
    operation or patch.
15. Passive contact cannot cross its weighted equilibrium in one update, and
    clamped internal transfer cannot cross pair equilibrium or change total body
    energy. This holds without runtime substeps.
16. The `0 C` full-immersion and exposed strong-blizzard fixtures meet their
    explicit warning/mild/damage windows; nearby lava meets its explicit
    one/four/eight-block response windows; contact fire/lava/powder defaults
    retain useful legacy curves and vanilla direct-damage ownership.
17. Entity contact remains a local primitive body input and never writes Air;
    nearby lava warmth comes from the Mesh boundary. Campfire retains exactly
    its existing source registration, `8,000 W` split, Air path, radiation path,
    and response curve with no second contribution.
18. Initial local wind is exactly the outdoor `m/s` value when
    `ServerLevel.canSeeSky(player_eye_block)` is true and exactly `0 m/s` when
    false. The gate is sampled once per player update, ignores stale
    `windStrengh`, creates no Page/ray/scan/cache/state/packet, and does not
    simulate partial drafts through doors, windows, open walls, or caves.

## Rejected Alternatives

- **HUD shows raw core body temperature:** it remains near `37 C` until danger
  and does not explain shelter or Campfire improvement.
- **HUD shows `T_air - 37`:** the value is an internal coordinate offset, not a
  temperature players can interpret.
- **HUD shows raw Air only:** it cannot represent wind or immediate direct
  radiant comfort.
- **Orb color repeats the numeric Celsius bands:** it tells the player the same
  fact twice and does not answer whether current clothing and metabolism can
  tolerate that environment. The selected color encodes net body power while
  the number remains environmental Celsius.
- **Add a warming/cooling arrow:** the existing number occupies the orb and the
  current HUD has no clear space for another indicator. Reusing orb color gives
  the required warning without layout growth.
- **Add radiation as an arbitrary Celsius bonus:** it discards `W/m2`, body area,
  absorptivity, and heat-transfer context.
- **Register every lava block, fire block, or burning player as a Thermal
  source:** entity contact already exposes O(1) state, while world lava/fire is
  cheaper as exposed-face/Air-cell work aggregated by existing Bricks.
  Per-block registration adds source generations, indexing, ledger bindings,
  accumulators, and churn without improving the boundary equation.
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
  for topology-owned lava/fire heat, receiver conversion, and unchanged
  Campfire source split;
- [`docs/climate/thermal-runtime-architecture-and-optimization.md`](../docs/climate/thermal-runtime-architecture-and-optimization.md)
  for fixed lava boundaries, aggregated ordinary-fire powers, hot Brick
  publication, operation/memory bounds, and unchanged radiation budgets;
- [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md)
  for NBT schema, migration, packet, cadence, and reload behavior;
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

The plan remains in progress. Topology-aggregated nearby lava and ordinary-fire
Air/radiation work, its dedicated validation, and final gameplay calibration
are deferred to the next implementation pass.
