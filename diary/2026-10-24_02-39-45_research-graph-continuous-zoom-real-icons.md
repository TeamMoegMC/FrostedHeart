# Continuous research graph zoom with persistent real icons

- Time: `2026-10-24 02:39:45 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `completed`
- Scope: `ResearchGraphViewport`, `ResearchGraphLabelPlacement`, opt-in client validation and research UI documentation

## Completed

- Followed the user's explicit preference to preserve the full dependency graph and allow moderate icon shrinking. Removed automatic field-overview replacement and discrete card/icon/marker modes. The independent knowledge graph remains a separate UI.
- Every visible project keeps its actual icon at all zooms: 8 GUI pixels at minimum 15% zoom, rising to 16 pixels according to available lane spacing. Paper geometry, icon inset and status stripe expand with one continuous smoothstep function. World node centers and dependency topology are retained.
- Kept normal-size names beside projects at every zoom, including ordinary projects at minimum zoom. A reused two-pixel owner grid reserves nodes, controls and accepted names. Fixed hover/selection/search/active/ordinary priority passes and three clipped widths choose only local right/left/top/bottom positions. No global free-space search, remote name placement or leader lines remain. An unavailable name never removes its icon.
- Added a single 150 ms camera animation for wheel input. Zoom uses smoothstep easing in logarithmic space; camera position is derived from the pointer's fixed world anchor on every frame. Repeated wheel input accumulates the target; opposite input, clicks, dragging, resize, definition changes and field changes resolve/cancel pending motion. Settled frames stop animation work.
- Reused graph node/edge/grid buffers, per-node label records, accepted-label references, shape batching, `CIconBatch` and presentation/style caches. A hovered accepted name retains its position during priority changes. No per-node animation objects, UI child widgets or render targets were added.
- Preserved the earlier localized-component parser fixes and screen-adapter coordinate-origin correction.
- Reworked the opt-in real-client route to require real icons and multiple local ordinary names at 15%, test former switch boundaries, verify nearby/nonoverlapping names, click an actual minimum-zoom icon, exercise the modal/focus/pan/search/empty/normal-mode paths, and observe actual intermediate wheel frames with a fixed world anchor. Cursor motion uses Minecraft's normal callback so an unfocused automated window still receives the event.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with current formulas, constants, placement, animation, caching and validation. The earlier field-overview diary is marked partial/superseded, with its actual validation failures preserved.

## Decisions

- Preserve project identity and spatial continuity rather than automatically replacing the tree with another presentation. All in-view projects keep real icons; names compete for nearby space at normal font size.
- Animate one camera rather than every project. Recompute visible geometry/placement only during actual camera/presentation changes, and reuse it once settled.
- Retain the user-approved minimum zoom rather than forcing a larger zoom floor. Width/height interpolate continuously; there is no representation threshold that removes icons or ordinary names.
- Validate through the current checkout's ordinary Gradle outputs, a real Forge server and a real Minecraft client after implementation and documentation. No JUnit or standalone model tests.

## Validation

- Final unified command: `.\gradlew.bat validateResearchCatalog runGameTestServer runResearchUiClient -PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login --offline --no-daemon --console=plain`, using Java 17 and the current checkout.
- [Final log](../build/research-ui-continuous-20261024-final.log): `BUILD SUCCESSFUL in 2m 59s`; 86 production research definitions passed preflight, and all four required Forge GameTests in the selected research/login namespaces passed.
- [Real-client result](../build/research-ui-validation/result.json): `passed`; 14 settled display scenarios plus a search observation, 15 interaction stages, and `intermediateZoomObserved = true`. Covered Chinese/English, 15%, 29%, 30%, 37%, 38%, 45%, 100% and 175%, GUI scales 2/3 and a physical 640 by 480 window.
- In the 390 by 335 GUI-pixel viewport, Chinese 15% displayed 79 real-icon submissions at 8 pixels with 17 nearby names; English 15% displayed 79 icons with 16 names. Chinese 45% displayed 24 icons at 16 pixels with 24 names. The 118 by 175 narrow viewport retained 24 icons and four local names at 15%. These are in-view counts around the selected project, not a claim that all 86 definitions are on-screen at once.
- All 14 settled scenarios reported zero geometry-plan and zero label-plan rebuilds over the static observation interval. Observed icon-batch flushes ranged from two to five. These are direct cache/batch observations, not FPS or allocation benchmarks.
- Actual screen input passed minimum-zoom item clicking, project opening, Escape camera retention, focus, smooth wheel input with an unchanged immediate camera and observed intermediate frames, a fixed pointer/world anchor throughout the motion, settled animation shutdown, dragging, search projection reuse, empty graph/selection focus and normal visibility filtering.
- Captured 25 original framebuffer PNGs under [the final run directory](../build/research-ui-validation/2026-10-24_02-41-46/), including five actual transition frames. Visually inspected Chinese minimum/compact/full/narrow views, the transition midpoint and normal mode. Created an unscaled [minimum-zoom graph crop](../build/research-ui-validation/2026-10-24_02-41-46/zh-minimum-graph.png) and [slow-play transition GIF](../build/research-ui-validation/2026-10-24_02-41-46/continuous-zoom-slow.gif) from those real frames for review. The runtime transition is 150 ms; the GIF intentionally holds/slows its frames.
- `git diff --check` passed for the changed tracked source/documentation. No JUnit, standalone model tests, copied checkout or artifact fingerprinting was used. The driver restored GUI scale, FPS cap, pause-on-lost-focus, language and editor settings before shutdown.

## Remaining

- None for the agreed full-tree zoom behavior. Long/dense names still use nearby shorter variants or yield their text slot; every in-view project continues to submit its real icon. Runtime FPS/allocation benchmarking was not performed.
