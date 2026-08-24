# Research static-frame cache and texture batching

- Time: `2026-08-24 04:04:16 +08:00`
- Author: `Codex (OpenAI; primary implementation and validation agent)`
- Status: `completed`
- Scope: `CIconBatch`, research archive rendering caches, clue refresh, empty tick-clue handling, tests, and research UI documentation

## Completed

- Extended the opt-in `CIconBatch.Ordering.LAYER_THEN_LIGHTING` pass to retain texture requests, submit them by z layer and contiguous texture, reuse request storage, and restore GUI render state once per texture pass. Default submission ordering and unknown-icon barriers remain unchanged.
- Replaced the project dialog's partial caches with one revision-keyed presentation containing resolved header/state text, wrapped descriptions, material geometry, clue rows, effects, tabs, footer action, and content heights.
- Refreshed the shared archive state snapshot on clue packets so graph, summary, and dialog progress cannot retain stale values.
- Cached field-tab IDs/icons, fixed archive components, list-row title clipping, empty-summary wrapping, graph label coordinates, and the zoom label across static frames.
- Added an empty `ResearchHooks#tick` path before team-data lookup when the catalogue contains no tick clues.
- Updated `docs/research/research-ui.md`; the existing legacy left-list ordering and global graph layout contracts were preserved.

## Decisions

- Kept real item-model rendering and per-pass `BakedModel` lookup so animated/dynamic models, glint, resource reloads, combined icons, and the `15%-175%` content contract remain correct.
- Kept delayed texture ordering restricted to the graph's non-overlapping opt-in mode. The public default still preserves strict call order.
- Did not add an item-thumbnail atlas, GPU `VertexBuffer`, or off-screen graph target without an in-game JFR/RenderDoc/Spark profile proving that Phase 4 complexity is warranted.

## Validation

- Primary agent: `.\gradlew.bat compileJava --no-daemon --console=plain` passed.
- Primary agent: focused icon-planner, archive, and `ResearchHooksTest` command passed (`BUILD SUCCESSFUL in 47s`).
- Primary agent: `.\gradlew.bat test --no-daemon --console=plain --tests "com.teammoeg.frostedresearch.*"` passed (`BUILD SUCCESSFUL in 16s`).
- Primary agent: `.\gradlew.bat test --no-daemon --console=plain` passed (`133` suites, `485` tests, no failures, errors, or skips; `BUILD SUCCESSFUL in 18s`).
- `git diff --check` reported no whitespace errors; only line-ending conversion notices were present.

## Remaining

- Measure drawing desk versus static/dragged `15%` archive median and P95 frame time in the same runtime, and inspect the existing `frostedresearch_graph_*` profiler sections.
- Visually verify flat/block/glint/animated icons plus item-texture and texture-item combined icons at `15%`, `55%`, `100%`, and `175%`, including language/resource reload and returning to the drawing desk.
