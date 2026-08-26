# Temperature threading pool disabled

- Time: `2026-08-27 01:56:49 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation agent`
- Status: `completed`
- Scope: `FHServerEvents`, dormant player environment sampler lifecycle, and climate documentation`

## Completed

- Commented out the server lifecycle calls that initialized, polled, and shut down `TemperatureThreadingPool`.
- Commented out the matching `SurroundingTemperatureSimulator.init()` call so the dormant sampler is not initialized at server startup.
- Kept `TemperatureThreadingPool`, `SurroundingTemperatureSimulator`, their tests, configuration fields, and legacy data fields in place as requested.
- Updated the living player and lifecycle documents to identify the retained sampler as disabled code rather than the current player environment path.

## Decisions

- `MinecraftThermalInput.gameplayPlayerEnvironment` remains the active player environment authority.
- This change disables the old lifecycle without deleting or migrating its source, configuration, tests, or persisted `blockTemp`/`windStrengh` state.
- No source fix was made for the first client launch failure: it was caused by launching `runClient` while `compileJava` was rebuilding `build/classes/java/main`, not by the disabled temperature lifecycle.

## Validation

- `.\gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --tests "com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator*" --offline --console=plain` completed successfully.
- `181/181` selected JUnit tests passed across `34` suites with zero failures, errors, or skips.
- `git diff --check` reported no whitespace errors; only existing line-ending conversion warnings were emitted.
- The `01:58:29` client crash reported many unrelated Frosted Heart classes missing from the same output directory; affected class files were written back after that failure as compilation completed.
- A fresh client launched at `02:02:50` after compilation completed, passed mod registration and FML client setup, initialized OpenAL and texture atlases, remained running, and produced no new crash report.

## Remaining

- The retained legacy sampler cluster and stale persisted player sampling fields still require a separate deletion or migration decision.
