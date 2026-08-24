# Research project list legacy ordering

- Time: `2026-08-24 03:17:06 +08:00`
- Author: `Codex (OpenAI; production implementation) with Terra medium (tests and validation)`
- Status: `completed`
- Scope: `FHResearch` client project ordering, archive left project index, tests, and research UI documentation

## Completed

- Extracted the original `FHResearch#getResearchesForRender` status grouping into an iterable overload while retaining the legacy category entry point.
- Changed `ResearchTypeListPanel` to apply archive privacy, field, and search filters before using the original client ordering.
- Removed active-research and bookmark ranking from the index. Both remain row markers, while research progress now invalidates the ordered-list cache.
- Updated `docs/research/research-ui.md`; `ResearchGraphLayoutEngine`, graph projection, and graph coordinates were not changed.

## Decisions

- Preserved the original grouping and iteration semantics exactly, including reverse relative order for multiple unclaimed-reward projects caused by front insertion.
- Reused the ordering implementation without passing the global catalogue directly to the archive, so normal-mode discovery privacy remains enforced at the archive boundary.
- Kept the full graph's deterministic dependency layout independent from player-state list ordering.

## Validation

- Terra medium: focused `FHResearchRenderOrderTest` and `ResearchTypeListPanelCacheTest` passed.
- Terra medium: `.\gradlew.bat test --tests "com.teammoeg.frostedresearch.*"` passed, `55` tests with no failures or errors.
- `git diff --check` completed after implementation; only repository line-ending conversion notices remained.

## Remaining

- In-game verification of the left index after completing research, claiming effects, changing field/search filters, and toggling editor mode.
