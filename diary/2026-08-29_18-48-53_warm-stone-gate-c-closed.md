# Warm stone T17-T19 and Gate C

- Time: `2026-08-29 18:48:53 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent with T17/T18/T19 sub-agents`
- Status: `completed`
- Scope: `one-point receiver regression, Campfire dropped-item GameTest, dropped workload limits, Gate C`

## Completed

- Expanded one-point radiation and item-environment tests across distance, occlusion revisions, independent caps, publication/fallback selection, cache generation and close cleanup.
- Added a real Forge GameTest for warm stone and hot-water bag heating, stone-wall occlusion, extinguish-plus-move cooling, and passive query Page/cell/chunk invariants.
- Added fixed workload tests for exact hook claims, cadence distribution, sample reuse/overflow, receiver isolation, lifecycle cleanup and stable-path allocation ceilings.
- Closed Gate C and updated lifecycle test coverage, the implementation plan, and shared handoff.

## Decisions

- Test observability is package-private, read-only and allocation-free; production receiver budgets and gameplay behavior remain unchanged.
- Extinguishing a Campfire does not erase already-admitted mesh residual heat. The cooling GameTest therefore uses the accepted extinguish-or-move contract instead of changing thermal physics.
- Workload acceptance uses deterministic counters and allocated-byte measurements; wall-clock time is diagnostic only and is not a pass condition.

## Validation

- T17: `2` JUnit suites, `18/18` tests.
- T18: `compileGameTestJava` passed; Forge GameTest `13/13 required` passed.
- T19: `3` JUnit suites, `7/7` tests.
- Final JDK 17 player/thermal/Curios regression: `53` suites, `277/277`, zero failures, errors, or skips.
- `git diff --check` and dropped-path forbidden-call scans passed.

## Remaining

- Phase 3 begins at T20 with a fresh companion-repository constraint and identifier review. No companion files were changed here.
