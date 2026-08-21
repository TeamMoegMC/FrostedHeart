# Research graph render performance implementation

- Time: `2026-08-21 17:39:57 +08:00`
- Author: `Codex (OpenAI; production implementation) with Terra medium (tests and validation)`
- Status: `completed`
- Scope: Chorda icon batching and CUI visibility propagation; Frosted Research graph render plan, presentation/state caches, virtual list/summary hot paths, tests, and research UI documentation

## Completed

- Added reusable ordered `CIconBatch` passes and low-level flat/block item grouping. The research graph uses non-overlapping layer-then-lighting submission while unsupported icons remain explicit immediate barriers.
- Added `ResearchArchiveViewCache` for definition/language presentation and synchronized research-state snapshots without moving authoritative progress into CUI.
- Replaced per-frame graph coordinate conversion, edge clipping, localized-name parsing, title truncation, and per-label pose stacks with an invalidated screen-space render plan and shared label transform.
- Cached the concise summary presentation, limited the project list loop to visible rows, reused the bookmark view/category array, and removed hot-path current-research `Supplier` allocation.
- Skipped hidden CUI descendant render-info/mouse-over propagation while preserving tick behavior and same-frame recovery after showing a subtree.
- Added Minecraft profiler anchors for graph shapes, item icons, and labels; updated `docs/research/research-ui.md` and the implementation plan outcome.

## Decisions

- Preserved the `15%-175%` zoom contract, including the `4px` item and `0.25` text-scale floors; performance is not obtained by hiding node content.
- Kept strict ordering as the public batch default. Only the research graph opts into lighting reordering because its node icon rectangles do not overlap; z layers preserve combined-icon base/overlay order.
- Did not cache `BakedModel`, upload static GPU buffers, create an icon atlas, or add an off-screen render target without runtime evidence that those costs remain material.
- Kept research presentation/state derivation inside the archive and kept generic camera, visibility, and icon submission in Chorda.

## Validation

- Terra medium: focused batch/visibility/list/state tests, `BUILD SUCCESSFUL in 22s`.
- Terra medium: `ResearchGraphViewportPerformanceTest` and `ResearchArchiveLayerConstructionTest`, `BUILD SUCCESSFUL in 17s`.
- Terra medium: `.\gradlew.bat test --no-daemon --console=plain`, `BUILD SUCCESSFUL in 18s`; `compileJava` and `compileTestJava` passed.
- `git diff --check` passed with only existing CRLF conversion warnings.

## Remaining

- Re-run the fixed drawing-desk, static `15%` overview, dragging, `100%` local view, and modal scenarios in game. Record median/P95/P99 frame time, the three profiler sections, item submission count, allocation rate, and visual correctness for flat/block/glint/combined/animated icons.
- Enter Phase 4 only if that profile still misses the plan thresholds; runtime FPS improvement is not asserted by unit tests.
