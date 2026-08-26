# Thermal in-flight, source invalidation, and refresh recovery

- Time: `2026-08-27 04:27:37 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation and lifecycle review agent`
- Status: `completed`
- Scope: `Thermal full-resync source binding, in-flight epoch recovery, transport retry boundaries, Page environment refresh budget, synchronous contracts, tests, and climate documentation`

## Completed

- Routed ordinary geometry invalidation and raw/full-resync invalidation through the same section-local physical-source dirty path. Main-thread invalidation is immediate; off-thread invalidation is drained before `MinecraftPhysicalSourceManager.flush`.
- Added `DimensionThermalRuntime.inFlightFrame` and `MinecraftThermalTopologyApplier.recoverInFlightEpoch`. An old solve or hard-cap epoch now settles its exact source cut and completes on an isolated empty sweep before a newer frame can drain topology input.
- Moved complete sweep endpoint validation before source integration. Preflight failures remain replayable; a mutation-stage failure resumes after the partially executed substep so already committed transport is not applied twice.
- Bounded natural-temperature refresh by entries dequeued rather than live Pages refreshed. Queue entries store section key plus lifecycle generation instead of retaining withdrawn `ThermalPage` objects.
- Replaced stale worker/`Executor` descriptions with the implemented server-main-thread synchronous ownership contract.
- Added JUnit coverage for hard-cap plus retirement recovery, exact failed-epoch preservation, source-before-sweep preflight, and stale refresh budgeting, plus a Forge GameTest for raw full-resync with a live physical source.

## Decisions

- Treat hard-cap as one trigger of the existing in-flight/new-frame interlock, not as a separate sixth defect.
- Never apply a newer Page staging cut to an older epoch. Exceptional recovery may skip that epoch's remaining transport through an explicitly unresolved empty sweep, but it preserves source energy accounting and scheduler progress.
- Keep `TemperatureThreadingPool` source and its commented lifecycle calls unchanged.

## Validation

- Independent `gpt-5.6-luna` verification ran `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain`: successful; all `185/185` selected JUnit tests passed with zero failures and errors.
- `gradlew.bat runGameTestServer --offline --console=plain`: successful; all `12/12` required Forge GameTests passed.
- `git diff --check`: successful with no whitespace errors; only repository LF/CRLF conversion warnings were reported.

## Remaining

- Profile large real-save Page churn, raw resync, long-lived physical sources, environment-refresh backlog, and hard-cap recovery with the same before/after JFR workload before claiming a measured tick-time improvement.
