# Thermal shadow runtime cleanup

- Time: `2026-08-25 20:16:43 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `Minecraft thermal dispatch, gameplay query state, GameTests, Phase L benchmark names, living climate documentation, and active thermal plan`

## Completed

- Removed the unused machine environment query/observer and the player, machine, crop, town, and aggregate shadow snapshots from `MinecraftThermalInput`.
- Removed legacy/new comparison state, published-air lookup counters, seal/worker timing counters, last dispatch reports, and the input-owned single-slot worker mailbox.
- Renamed the runtime connection to `enableDispatch` and retained Java `Executor` as the only future scheduling boundary. Gameplay continues to pass `Runnable::run` and therefore remains synchronous.
- Kept player/crop/town gameplay queries, explicit fallback flags, topology application, coordinator solve, publication, and `DimensionThermalRuntime.Diagnostics`.
- Renamed Phase L JMH/JOL classes and report files from shadow-query to published-air-query and updated the living docs and active implementation plan.

## Decisions

- A future asynchronous executor must serialize tasks for one `MinecraftThermalInput`, preserve submission order, and prevent overlap. The input does not own another queue or worker state in advance.
- Do not keep a machine API until a real environment-sensitive machine owns a receiver point and cadence.
- Performance evidence belongs in JFR/JMH and workload reports, not always-on per-dimension comparison state without a consumer.

## Validation

- Java 17 `compileJava` and `jmhClasses` passed.
- Repository JUnit executed `817` tests: `816` passed; the only failure remains the missing external fixture in `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`.
- Forge GameTest passed all `19/19` required tests.
- `thermalPhaseLQueryRetainedHeap` passed with the renamed published-air benchmark classes and output path.
- `git diff --check` reported no whitespace errors; existing LF-to-CRLF notices remain.

## Remaining

- Replace `Runnable::run` only when a real serial worker executor and production-like concurrency workload are ready.
- Add a machine environment query only alongside an actual gameplay consumer.
