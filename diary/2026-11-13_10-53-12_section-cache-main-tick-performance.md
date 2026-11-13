# Section cache main-server tick cost

- Author: Codex
- Date: 2026-11-13 UTC
- Status: completed; nine measured phases, settings restored and game closed
- Scope: main-thread service/city intervals and whole Forge server tick CPU/wall cost

## Changes

- Extend development TownPathProbe/TownPathCacheMixin with whole-server thread CPU and per-section copy timers. Select the level when arming so the first shared-service tick is attributed correctly.
- Add test-only clearcache/dirtycache controls and Scripts/run-town-cache-tick-performance.py, plus docs/town/town-cache-tick-performance.md and JSON/CSV summaries.
- Production cache/navigation code unchanged in this performance-only task; prior 41-case functional result remains the last functional validation.

## Validation

- Profiling client compiled and ran the saved 32-building natural world in one fixed-4G JVM, zero residents and no missing sampled chunks.
- Two cold/immediate pairs, dirty/grace phase, idle controls and refresh tails completed. Main feature CPU mean about .70–.73ms/tick; p95 1.31–1.52ms, maximum 1.84ms. All active copy budgets <=2 sections/tick.
- Direct main-wall p95 3.43–5.35ms, maximum 32.32ms; software-rendering/single-core scheduling means CPU is not a latency upper bound. Dirty-write callbacks are covered only by whole-tick timing, not independently attributed.
- Whole-server baseline drift and small phase samples are documented; no pristine-vanilla comparison or guaranteed tick cap claimed.
- Cleanup released 221 forced chunks; restore and quit succeeded. Log confirms all dimensions saved at 10:47:36 UTC and BUILD SUCCESSFUL.
- Generated raw data: build/town-cache-tick-performance/. Script syntax, JSON/CSV and git diff --check validated. No new functional GameTest run or commit/push for this task.
