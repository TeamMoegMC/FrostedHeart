# Player Temperature

- Status: `Current`
- Last verified: `2026-08-29`
- Scope: player environment queries, body-part temperature, clothing/effects, and synchronization
- Primary code anchors: `TemperatureUpdate`, `TemperatureComputation`, `PlayerTemperatureData`, `MinecraftThermalInput.gameplayPlayerEnvironment`, `MinecraftThermalInput.gameplayPassiveEnvironment`, `MinecraftThermalInput.gameplayCropEnvironment`, `FHBodyDataSyncPacket`

## Environment Query

`TemperatureUpdate` asks `MinecraftThermalInput.gameplayPlayerEnvironment` for
the player's eye position. The runtime requests a primary Page lease, reads a
current lock-free Page publication, and composes its air value with analytic
control fields and direct radiation. A missing Page, stale geometry, unresolved
Air point, old sample, or unavailable radiation result falls back to the
current `WorldTemperature.naturalAir` value without loading a chunk or waiting
for the worker.

Passive blocks and crops use the same publication reader but never admit a Page
on a miss. Town scanners query their weighted representative points and use an
all-or-natural fallback when a partial region is unavailable.

The Page runtime is asynchronous. Admission, geometry capture, source updates,
and solver publication share one 20-tick cut. A first query therefore returns
natural fallback; a later query observes the Page only after worker completion
and main-thread ACK. Repeated door/fence-gate/trapdoor mutations are coalesced
by position until the cut.

## Player Cadence

`temperatureUpdateIntervalTicks` defaults to `20`. `TemperatureUpdate` derives
a stable UUID phase offset, distributing players across the 20 ticks instead of
running every player's environment query in the same tick. The body/effect
calculation remains on the level thread and uses one caller-owned
`ThermalEnvironmentSample` per player update.

`TemperatureThreadingPool.java` is retained but its initialization, tick, and
shutdown calls are commented and disabled. It is not used for player or thermal
runtime work. There is no second player sampler or synchronous thermal path.

## Body And Clothing

`PlayerTemperatureData` stores the five body-part temperatures, clothing
insulation, effective environment, and total perceived temperature. The existing
food, armor, wetness, fire, potion, difficulty, and damage rules consume these
values after the environment query. Values representing body offset are in
degrees Celsius relative to the normal body temperature; environment and
perceived values are absolute degrees Celsius.

`TemperatureComputation` keeps the established conversion from direct radiant
flux (`W/m2`) to body temperature change. Analytic fields are control inputs,
not stored body energy and not part of the physical source ledger.

## Synchronization And Persistence

`FHBodyDataSyncPacket` carries the player-facing aggregate body/environment/
perceived values. Page cells, arena enthalpy, source bindings, and worker
topology are runtime state and are rebuilt after level load or worker-generation
restart; they are not written into player or chunk NBT.

Player capability cloning/reset follows the existing death and respawn rules.
Recipe reload invalidates the shared gameplay profile snapshot and closes active
thermal runtimes; the next query creates a fresh generation.

## Hot-Path Guarantees

- player lookup is O(1) after Page publication and arena-slot generation checks;
- unchanged sleeping worker batches republish the sample tick without copying cell values;
- one hundred players have stable cadence spread over the 20 ticks;
- query misses never admit Pages or trigger chunk loads;
- no production counter, probe, debug collection, or legacy compatibility adapter
  is used to implement or test this path.
