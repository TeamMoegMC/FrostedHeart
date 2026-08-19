# 居民渲染确定性基准与统计

- Time: `2026-08-19 20:17:47 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `citizen client benchmark injection, CPU batch render metrics, debug commands/HUD, tests, and living documentation`

## Completed

- Added a deterministic client-only crowd harness for 32, 64, 256, and 1024 residents in moving or sleeping modes. The scene is anchored in front of the player, moving snapshots update every four ticks, and sleeping residents cover four principal directions.
- Isolated benchmark residents in a high positive id range and remove them by object identity, so loading or clearing a benchmark never clears or overwrites synchronized citizen cache entries.
- Added `/citizen_debug` client commands for benchmark load/status/clear, metric query/reset, and debug-overlay control; dimension changes and logout clear benchmark state and metric history.
- Instrumented `ClientCitizenRenderer` with allocation-free steady-state counters for batch-frustum residents, Body/Billboard ownership, batch draw calls, light samples, instance dirty bytes, and CPU render time.
- Added a reusable 256-frame timing ring with latest/average/nearest-rank p95 reporting. The optional HUD rebuilds text once per second; strict captures should leave it disabled and query the command afterward.
- Updated `docs/town/citizen-rendering-at-scale.md` and `docs/town/hybrid-simulation-architecture.md` with commands, exact metric boundaries, lifecycle behavior, and M0 status.

## Decisions

- Keep the benchmark purely client-side and additive to the synchronized cache. Exact comparison runs must use an empty flat test world so unrelated real citizens do not contaminate the cache count.
- Measure `ClientCitizenRenderer.render` separately from vanilla fake-entity rendering and label it `CPU batch`; do not infer GPU time or total entity-render cost from this counter.
- Reserve `instanceDirtyBytes` now with value zero on the CPU backend so M2/M3 can report Flywheel uploads through the same output contract.
- Reset the metric window on benchmark load/clear, dimension changes, and logout so p95 samples never mix scenarios.

## Validation

- Focused citizen suite passed: 43 tests, 0 failures, 0 errors, 0 skipped. New coverage includes four supported crowd sizes, deterministic grid/movement boundaries, the 256-frame p95 window, and 1024 same-distance candidates capped to 64 detailed proxies.
- Full Gradle suite passed: 217 tests across 68 suites, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESSFUL`). Compilation emitted only the existing JEI removal/deprecation warnings.
- `git diff --check` passed; trailing-whitespace search found no matches in the new source, tests, or rendering document. Git reported only existing LF-to-CRLF working-tree warnings.

## Remaining

- Run the 32/64/256/1024 scenes in Minecraft with fixed graphics settings and capture CPU/JFR, GPU/RenderDoc, screenshots, and configuration metadata; automated tests do not prove visual correctness or the performance budget.
- Implement M1 render ownership/backend boundaries, then compare the CPU baseline with a static Flywheel thousand-instance PoC.
