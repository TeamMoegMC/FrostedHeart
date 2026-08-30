# Thermal COMMON tuning configuration

- Time: `2026-08-30 21:41:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal gameplay config ownership, startup snapshot, source profile consistency, defaults, tests, plan, and living climate documentation`

## Completed

- Added `FHConfig.COMMON.THERMAL_RUNTIME` with seven restart-scoped values:
  Air heat capacity/mixing, phase face conductance/base energy, FarField
  conductance, and campfire power/radiation share.
- Replaced the runtime Air, phase, FarField, and campfire hardcoded calibration
  with one synchronized server-start read in `MinecraftThermalProfiles.prepare`.
- Retained only three worker scalar inputs and one immutable campfire profile in
  the server-wide tuning snapshot. Phase-only values are local to profile
  compilation and are not retained for the server lifetime.
- Passed the same configured campfire profile to `PhysicalSourceSpatialIndex`
  and `WorkerPhysicalSourceBindings`, so initial ports, later rebinds, direct
  radiation, and offered source power cannot disagree.

## Decisions

- The tuning is COMMON rather than SERVER config because it is instance-wide
  modpack/development calibration, not per-save state. It is stored in
  `config/frostedheart-common.toml` and is also authoritative on a dedicated
  server.
- Config values are frozen at startup. There is no hot reload, revision,
  watcher, compatibility layer, or active-runtime rebuild; editing requires a
  client or dedicated-server restart.
- No solver, source, query, mutation, tick, or worker-restart path calls
  `ConfigValue.get()`.

## Validation

- Java 17 production, test, and GameTest compilation: passed.
- Thermal JUnit: `97/97` passed.
- Forge GameTest: all `14/14` required tests passed.
- The GameTest server added the seven missing COMMON keys with the documented
  defaults, then compiled `707` phase states successfully.
- Residual searches found no authoritative old Air/phase/FarField hardcodes and
  exactly seven startup config reads.
- `git diff --check`: passed.

## Documentation

- Updated thermal runtime architecture, heat production/phase behavior, and
  climate data lifecycle documentation with config keys, defaults, ownership,
  restart requirements, and hot-path behavior.
- Updated the active async-runtime plan and marked all tuning-config items
  complete.

## Remaining

- Controlled gameplay calibration of enclosed Air mixing, open-tundra
  FarField loss, and permafrost melting remains empirical tuning work; it does
  not require another architecture change.
