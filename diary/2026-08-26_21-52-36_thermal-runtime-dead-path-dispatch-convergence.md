# Thermal runtime dead-path and dispatch convergence

- Time: `2026-08-26 21:52:36 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal source state, Minecraft input/topology dispatch, production test fixtures, tests, plans, and climate lifecycle documentation`

## Completed

- Deleted `SourceResyncReplayer`, `SourceResyncSnapshot`, `ThermalRuntimeCoordinator`, and production `LoadedOnlyResolverSnapshot`.
- Removed source replay history, ACK/checksum metadata, the duplicate registry watermark, callback-only energy drain, hot-path `SealReport`/`ApplyReport`, and the unused topology-apply entry point.
- Connected the serial executor directly to topology apply and the owning `DimensionThermalRuntime`.
- Moved the loaded-only Phase A capture fixture into GameTest-private code and changed tests to assert authoritative arena, topology, lifecycle, and routed-energy state.
- Reduced the six retained batch targets from `10,764` to `9,025` lines and deleted another `989` lines in four top-level production classes, for `2,728` fewer production lines in the touched scope.
- Updated the living lifecycle document and sparse-runtime plan to describe the implemented current-only source registry and direct per-dimension dispatch.

## Decisions

- Keep `ThermalSourceTimeline` as the only source watermark owner and `ThermalCellArena` as the only live `H/C` authority.
- Preserve exact event-boundary source integration, current bindings, route totals, lifecycle generations, five-tick cadence, urgent wakeups, and latest-only writer semantics.
- Keep only the serial `Executor` interface for future scheduling. Rejected execution leaves the latest frame pending for urgent retry and never falls back to concurrent caller-thread execution.
- Do not replace removed replay, coordinator, reports, or fixtures with compatibility shells or new diagnostic state.

## Validation

- `./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `204/204` passed with zero failures, errors, or skips across `34` result files.
- Forge GameTest: all `14/14` required tests passed.
- Deleted-symbol search across all source sets returned no matches; `git diff --check` reported no whitespace errors.
- No JFR was recorded, so this batch makes no measured CPU-improvement claim. Removing the unused source history saves approximately `225-230 KiB` of primitive payload per gameplay dimension, excluding array headers and other removed metadata.

## Remaining

- Run the same real-save repeated door/mining workload under JFR before attributing a CPU percentage improvement or choosing another performance data-structure change.
