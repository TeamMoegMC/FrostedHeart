# Thermal player gameplay connection

- Time: `2026-08-25 17:44:31 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `MinecraftThermalInput real-level startup/Page admission and TemperatureUpdate player/HUD consumer`

## Completed

- Started the existing thermal runtime on the first real player temperature query in each `ServerLevel`.
- Built the shared automatically trusted state-static profile cut in the existing `ServerStarted` callback so the first player tick does not pay that global census cost.
- Captured the player's loaded section into the existing Page/topology/solver path and enabled existing Campfire/Generator physical sources.
- Routed published air temperature into the existing body-temperature and HUD synchronization chain; legacy environment remains only the initial value and explicit query fallback.
- Temporarily commented the legacy surrounding-block sampler scheduling block without adding a config, command, manager, or runtime-owner class.

## Decisions

- Execute the current gameplay test dispatch inline at level tick end so Page admission and the arena writer cannot race. Async dispatch remains future work after real workload validation.
- Keep machine, crop, town, radiation, material, FarField, and surface behavior unchanged.

## Validation

- `gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*"`: successful, `238/238` tests.
- `git diff --check`: no whitespace errors.

## Remaining

- Enter a real save and verify first-query build time, HUD/environment changes, Campfire/Generator heating, section movement, and server tick cost.
- FarField, surface compositor, material calibration, and production async scheduling remain outside this connection.
