# Thermal transaction, lifecycle, and recovery completion

- Time: `2026-08-27 03:42:23 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation and investigation agent`
- Status: `completed`
- Scope: `Thermal solve recovery, topology publication, Page lifecycle, source rebinding, environment boundaries, dispatch/unload lifecycle, tests, and climate documentation`

## Completed

- Replaced permanent runtime failure latching with recoverable in-flight epoch retry. A failed transport substep retains its epoch identity and retry boundary; already advanced source intervals are not injected twice, while the failed sweep interval is executed again after topology recovery.
- Kept gameplay Page coverage unpublished until `DimensionThermalRuntime.finishTopologyUpdate` accepts the replacement sweep. Incremental fragment commits preflight endpoint generations, FarField slots, traversal, operation counts, and material aggregates before mutation; rejected intermediate graph state is hidden and forces a complete rebuild.
- Added generation-safe `pageSlot` reuse, tail contraction, stable `pageSlot * 64 + baseBrickIndex` fragment IDs, and a separate spatial traversal order. Admission and retirement use local graph/sweep patches whenever installed fragment capacity is sufficient.
- Drained committed section notifications immediately after synchronous topology apply. `MinecraftPhysicalSourceManager` marks only source ports indexed to those sections and resolves the installed target Page through `installedActiveBySection`.
- Split Air graph connectivity rebuilds from environment boundary refreshes. Natural temperature, sky exposure, and wind retain adjacency and component membership, recompute only existing component metadata, and emit boundaries through cached open members. Heightmap refresh is capped at `64` dirty XZ columns per tick.
- Removed the generic thermal dispatch `Executor` contract. Gameplay sealing now synchronously applies topology and runs at most one epoch on the server main thread. Startup failure, `LevelEvent.Unload`, and server shutdown all close runtime publication and memory ownership through idempotent lifecycle paths.
- Updated `docs/climate/data-lifecycle-and-integration.md` and `docs/climate/world-climate-and-temperature.md` to match the implemented transaction, performance, dispatch, recovery, and unload contracts.

## Decisions

- Keep Minecraft capture, topology mutation, arena ownership, and solve dispatch synchronous. A future asynchronous design must define new thread ownership and backpressure contracts instead of reusing a nominal `Executor` hook.
- Preserve one authoritative arena and latest-only scheduler. Recovery continues the failed epoch from the exact transport boundary rather than creating a second source timeline or accepting a failed sweep as completed.
- Allow internal staging before commit, but expose only the last runtime-accepted Page publication. A failed patch recovers through a complete rebuild instead of attempting a second mutable rollback system.
- Keep stable fragment storage IDs separate from spatial solver order so page-slot reuse does not change established floating-point traversal order.

## Validation

- Independent `gpt-5.6-luna` verification ran `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain`: successful; all `183/183` selected JUnit tests passed with zero failures and errors.
- `gradlew.bat runGameTestServer --offline --console=plain`: successful; all `11/11` required Forge GameTests passed.
- Regression coverage includes failed-substep recovery without duplicate source energy, Page slot reuse after retirement, delayed Page publication, precise committed-section notifications, environment-only coverage stability, arena free-span allocation, and fragment patch preflight rejection.
- `git diff --check` completed with no whitespace errors; only repository LF/CRLF conversion warnings were reported.

## Remaining

- Profile the same large real-save mutation, source, Page churn, and environment backlog workloads with JFR before and after these changes to quantify P95 tick time and allocation rate.
