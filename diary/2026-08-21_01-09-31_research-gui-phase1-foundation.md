# Research GUI Phase 1 Foundation

- Time: `2026-08-21 01:09:31 +08:00`
- Author: `Codex (OpenAI; implementation and validation)`
- Status: `completed`
- Scope: `com.teammoeg.frostedresearch.gui.archive`, research GUI refresh notifications, focused Frosted Research tests, and living documentation

## Completed

- Added client-only archive open context, workspace/camera/list state, and source-aware navigation with no network dependency.
- Added clue presentation, tab classification, and destination resolution. Experiment points are exposed as a read-only system clue backed by existing progress calculations.
- Added stable graph snapshots, hidden-safe category projection, category alias normalization, and deterministic SCC-aware layered layout with manual-anchor collision diagnostics.
- Replaced the empty `ResearchGui` marker with definition/progress/active/clue notifications; the legacy drawing-desk screen keeps full refresh as a fallback.
- Added current research documentation and marked Phase 1 complete in the active implementation plan.

## Decisions

- Kept `Research.CODEC`, `Clue.CODEC`, `ClueData.CODEC`, `ResearchData.CODEC`, action packets, and server completion behavior unchanged.
- Kept the existing fixed `ResearchLayer` player-facing while the full archive UI is built in Phase 2.
- Treated layout hints as display-only model input; current definitions continue to use automatic layout until the optional codec field is implemented later.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.*" --no-daemon` passed: 15 tests.
- `./gradlew test --no-daemon` passed for the full repository suite.
- `compileJava` and `compileTestJava` passed in the same Gradle run; existing project deprecation and duplicate-resource warnings remain.
- `git diff --check` reported no whitespace errors (only repository line-ending notices).

## Remaining

- Phase 2: implement and mount `ResearchArchiveLayer`, `ResearchGraphViewport`, type/search/status controls, compact type/reward list, canvas navigation, and shared read-only browser while retaining the legacy fallback until manual QA is complete.
