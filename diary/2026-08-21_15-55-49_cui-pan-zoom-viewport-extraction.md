# CUI pan/zoom viewport extraction

- Time: `2026-08-21 15:55:49 +08:00`
- Author: `Codex (OpenAI; production implementation) with Terra medium (tests and validation)`
- Status: `completed`
- Scope: `PanZoomViewport`, `ResearchGraphViewport`, `ResearchWorkspaceState`, shared and research GUI tests, research UI documentation

## Completed

- Added Chorda CUI `PanZoomViewport`, a reusable virtual-canvas layer for maps, node graphs, and other large two-dimensional surfaces.
- Centralized camera sanitization, configurable zoom limits/step, left/middle-button panning, pointer-anchored wheel zoom, fit/center operations, world/screen conversion, visibility checks, rectangular scissoring, and reusable segment clipping.
- Added extension hooks for bounded-map camera constraints, camera persistence callbacks, virtual marker input blocking, and separate background/content/overlay rendering.
- Migrated `ResearchGraphViewport` to the shared viewport and removed its duplicate camera, pan, zoom, coordinate conversion, and clipping implementations.
- Reused the shared `Camera` in `ResearchWorkspaceState` while preserving per-research-type camera state and the research-specific `0.15-1.75` zoom range.
- Kept research layout, projection, virtual node rendering, hit testing, status presentation, low-zoom icon/text minimums, and fixed-15-percent overview behavior in the research module.

## Performance Decisions

- Virtual map content remains batched and culled rather than creating one CUI child per node, tile, or marker.
- The shared viewport disables the general `UILayer` stencil pass and scissors only virtual content; ordinary CUI children remain available for small overlay toolbars.
- Horizontal and vertical clipping writes into a caller-owned `ScreenSegment`, avoiding per-edge result allocations.
- Large world-bound centers use difference-based arithmetic so valid same-sign coordinates do not overflow.

## Validation

- Terra medium added shared viewport coverage for camera sanitization, anchored zoom, left/middle panning, bounds fitting, large-coordinate centers, coordinate conversion, visibility, and segment clipping.
- Terra medium updated research state coverage to use `PanZoomViewport.Camera` and retained the search/projection cache regression test.
- Terra medium ran `.\gradlew.bat test`: exit code `0`, `BUILD SUCCESSFUL in 43s`.
- Terra medium ran `git -c safe.directory=D:/Frosted-Heart diff --check`: exit code `0`; only existing LF/CRLF conversion warnings were reported.

## Documentation Impact

- Updated `docs/research/research-ui.md`, the completed research GUI plan outcome, Chorda package documentation, and shared project structure memory to record the new ownership boundary.

## Remaining

- The future map implementation still needs domain-specific tile loading, marker indexing, map-border constraints, and in-game profiling; those concerns do not belong in the generic viewport.
- In-game visual QA remains necessary for research graph interaction and supported GUI scales.
