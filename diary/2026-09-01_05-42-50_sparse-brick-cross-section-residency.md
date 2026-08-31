# Sparse Brick cross-section thermal residency

- Time: `2026-09-01 05:42:50 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `thermal Page/Brick admission, source seeds, frontier propagation, FarField, completion lifecycle, tests, plans, and living climate documentation`

## Completed

- Replaced player/source/continuation Page references with exact source-seed and
  worker-desired Brick masks. Player, infrared, crop, town, dormant, and static-
  radiation queries no longer create thermal residency.
- Replaced `PageContinuation` completion payloads with changed absolute
  `BrickResidency` masks, including zero-mask cancellation and stale lifecycle
  rejection.
- Added sparse initial admission and same-lifecycle Brick additions. All-Air
  sections reuse canonical Air payloads without individual BlockState reads;
  other new Bricks read exactly 64 final states under a 64-Brick-per-tick main-
  thread budget.
- Added exact source `(section, Brick)` reference counts and carried absolute
  resident/source-seed masks through immutable worker messages and prepared
  topology commit.
- Added resident-aware `TopologyView` semantics so uncaptured Brick slots are
  absent rather than unresolved. Removed full-Page admission/resync/retirement
  scans from production paths.
- Deleted continuation state from `WorkerBrickTopology`, prepared Page writes,
  the worker completion, and `MinecraftPageManager`. Missing non-sky neighbors
  no longer receive synthetic FarField conductance; direct loaded sky remains.
- Added post-solve hot-mask hysteresis and six-direction frontier generation.
  Exact regular/mixed face residuals request same-Page and cross-Page guards;
  actual compiled cross-section Air pairs carry heat after admission.
- Limited initial heightmap work to the 16 columns of each newly resident top-
  layer Brick and kept uncaptured mutation geometry out of Page rebuilds while
  preserving source, sky, and radiation-occlusion channels.
- Updated the active architecture plan and living runtime, lifecycle, and heat-
  production documents.

## Decisions

- Keep exactly four worker Page masks: resident, resolved, source-seed, and hot.
  Absolute desired residency is held by one dimension primitive map plus its
  reusable scratch map, not a fifth Page mask or parent graph.
- Resident Brick bits grow during one Page lifecycle; whole-Page retirement
  occurs only after source, thermal hot state, and incoming frontier ownership
  disappear. Normal retirement clears main-thread capture so chunk reload reads
  current final state; an uncommitted work-limited admission alone reuses its
  captured payload during backoff.
- Freeze the implemented deterministic thresholds at `0.125 C` refine and
  `0.0625 C` release pending the controlled 100-source workload. They remain
  code constants, not runtime configuration.
- Keep direct-sky FarField only. An unavailable non-sky neighbor has no invented
  natural sink and is requested by the error-driven frontier instead.
- Add no per-Brick eviction, hot-Page index, room graph, LOD, matrix-free solver,
  or steady-source sleep path.

## Validation

- `compileJava`: passed.
- `compileTestJava`: passed.
- `compileGameTestJava`: passed.
- Complete `com.teammoeg.frostedheart.content.climate.thermal.*` JUnit selection:
  `109/109` passed after direct-sky frontier and fused hot-mask regressions.
- Forge `runGameTestServer`: all 14 required GameTests passed.
- New deterministic coverage passes for one-Brick admission, changed absolute
  cross-section request, zero cancellation, refine/release hysteresis, and
  positive heat transfer through an admitted neighboring Page.

## Remaining

- Run the controlled 100-source long-duration JFR/heap workload and record Page,
  Brick, live-cell, pair, boundary, completion-latency, main-thread capture, and
  retained-heap results.
- Validate campfire removal/cooling, long loaded tunnels, walls/doors, mixed
  geometry, chunk unload/reload dormant restoration, and infrared continuity in
  the live game.
