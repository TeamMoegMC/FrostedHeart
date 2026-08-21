# Research GUI hot-path and CUI optimization

- Time: `2026-08-21 15:31:48 +08:00`
- Author: `Codex (OpenAI; production implementation) with Terra medium (regression tests)`
- Status: `completed`
- Scope: `ResearchTypeListPanel`, `ResearchProjectWorkspace`, `ResearchGraphViewport`, `ResearchArchiveLayer`, `DrawDeskScreen`, focused tests, research UI documentation

## Completed

- Cached the project index's filtered and sorted rows and removed per-row hover list reconstruction.
- Cached clue presentation, tab filtering, wrapped descriptions, and modal content metrics with explicit progress/definition/selection/resize invalidation.
- Split search refresh from research-type projection refresh so typing no longer rebuilds the graph projection.
- Batched graph background, grid, visible edge segments, and node backgrounds through CUI `TesselateHelper`; culled nodes and clipped orthogonal edges to the viewport.
- Replaced manual fit/focus tools with CUI buttons while retaining virtual graph nodes and list rows to avoid full child traversal.
- Prevented unchanged archive ticks and selection-only navigation from relaying out the full responsive UI, and avoided repeated writes to already hidden external widgets.

## Decisions

- Graph layout, camera projection, node virtualization, and virtual project rows remain domain-specific code. CUI owns static controls and batched primitives where reuse lowers work without creating one child widget per research definition.
- Optional FTB sidebar polling remains active so delayed injection and resource reloads are still hidden and restored correctly.
- No research progress, clue, privacy, packet, zoom-floor, theory/detail, or empty-experiment behavior changed.

## Validation

- `./gradlew.bat compileJava` passed.
- Terra medium added `ResearchTypeListPanelCacheTest` and `ResearchGraphViewportPerformanceTest`.
- `./gradlew.bat test --tests "com.teammoeg.frostedresearch.*"` passed in 36 seconds.
- `./gradlew.bat test` passed for the full repository suite.
- `git diff --check` reported no whitespace errors; only line-ending conversion notices were present.

## Remaining

- Measure archive frame time, allocation rate, and draw submissions in game with JFR or Spark, and visually verify the CUI tool icons and graph batching at supported GUI scales.
