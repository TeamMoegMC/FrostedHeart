# Warm stone T23 test tools

- Time: `2026-08-29 19:40:03 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `OP-only reservoir test Stack presets and manual player/core/surface observation command`

## Completed

- Added `WarmStoneTestCommand` with `/fh_warm_stone_test give` for each reservoir item and its `cold`, `environment`, `hot`, and `core_hot_surface_cold` version-one thermal state.
- Added default-off `/fh_warm_stone_test observe start [interval_ticks]|status|stop`, which reports the game tick, player core temperature, and the equipped dedicated-slot reservoir's core/surface state to chat and server logs.
- Updated the player-temperature living document, plan, and shared handoff with command syntax, units, cadence, read-only behavior, and cleanup boundaries.

## Decisions

- `environment` uses `WorldTemperature.naturalAir` at the executing player's position, so the test state is a stable natural-air baseline rather than an invented high temperature or a physical-source target.
- The observer reads only the existing `warm_stone` Curios slot. It has no item, recipe, tag, creative-tab, receiver, NBT, or packet side effect beyond the new Stack deliberately given by the OP command.
- Command construction uses deferred registry Suppliers; test registration therefore does not initialize Registrate before the live registry phase.

## Validation

- JDK `17.0.2`: `WarmStoneTestCommandTest` and `WearableThermalStateTest` passed `2` suites and `10/10` tests with zero failures, errors, or skips.
- `git diff --check` passed.

## Remaining

- Stop before T24. T24 owns broader automation; T25 owns real-world measurements with the new command; T26-T28 own documentation consolidation, two-repository validation, and final outcome.
