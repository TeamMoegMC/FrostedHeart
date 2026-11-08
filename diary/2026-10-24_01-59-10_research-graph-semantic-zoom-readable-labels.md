# Research graph semantic zoom and readable localized names

- Time: `2026-10-24 01:59:10 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `completed`
- Scope: `ResearchGraphViewport`, new `ResearchGraphLabelLayout`, Chorda component text parsing, opt-in real-client validation, and research UI documentation

## Completed

- Implemented three presentation levels in [ResearchGraphViewport](../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchGraphViewport.java): proportional cards at projected sizes of at least 80 by 32 GUI pixels, 20 by 20 icon tiles when projected height is at least 20, and 6 by 6 overview markers otherwise. Item icons use 16 GUI pixels and titles use the normal font without a graph-scale pose.
- Added screen-space label priority for hover, selection, search, active research and immediate neighbours. [ResearchGraphLabelLayout](../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchGraphLabelLayout.java) reserves nodes, controls and labels in a reusable four-pixel grid. A primary focus preview can occupy a fixed lower position in a fully packed view; obscured icons are omitted and the preview targets its own project.
- Kept world coordinates, dependency layout, projection, research progression and the independent knowledge graph unchanged. Edges follow the displayed node bounds and highlight connections to selection/hover. Completed overview markers are filled.
- Increased tiny-marker hit areas to 12 GUI pixels and resolve overlapping hit areas by nearest center. Drawing and hits use the adapter-provided origin, including the CUI root's window-centering offset. Focus centers the selection at a zoom of at least 100%; an empty selection falls back to fit.
- Fixed two existing causes of empty research names: [ComponentOptimizer#components](../src/main/java/com/teammoeg/chorda/text/ComponentOptimizer.java) now flushes the final pending text run, and [StringTextComponentParser#parse(Component)](../src/main/java/com/teammoeg/chorda/client/StringTextComponentParser.java) advances the line reader before parsing each line. These are small fixes to the existing text path, rather than archive-specific replacement names.
- Added [ResearchGraphClientValidation](../src/gametest/java/com/teammoeg/frostedresearch/gui/archive/ResearchGraphClientValidation.java) and the opt-in `runResearchUiClient` Gradle run. It places a complete drawing desk through normal item use, waits for its client block entity, opens it through the actual client interaction packet, exercises the real screen and captures framebuffers. Ordinary client runs do not include or enable this driver.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with presentation thresholds, label ordering/placement, hit testing, caching, localized-name parsing and the real-client validation route. Preserved the existing note that the former JUnit sources were removed.

## Decisions

- Reused the existing geometry and style cache boundaries. Geometry/title clipping changes with camera, viewport, projection or language; selection, hover, search and state changes rebuild presentation/labels without rebuilding the projection. Static frames reuse both plans.
- Used a small occupancy array and stable priority passes rather than sorting labels or comparing every pair. Reused mutable node/edge/label buffers and grew storage only when needed. The shape pass and existing `CIconBatch` remain the rendering path; nodes do not become CUI child widgets.
- Used standard Gradle runs against the current checkout and its existing dependencies and build outputs.
- Ran no JUnit or standalone model tests. Validation was deferred until implementation and documentation were prepared; failures found in that final phase were repaired and the relevant real-game runs repeated.

## Validation

- Final unified command:

  ```powershell
  .\gradlew.bat validateResearchCatalog runGameTestServer runResearchUiClient -PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login --offline --no-daemon --console=plain
  ```

- [Final unified log](../build/research-ui-validation-20261024-final-r2.log): `BUILD SUCCESSFUL in 2m 39s`; 86 production research definitions passed preflight; all 4 required Forge GameTests in the selected namespaces passed. This is the selected research/login coverage, not a claim that every project GameTest namespace was run.
- [Real-client result](../build/research-ui-validation/result.json): `passed`, using all 86 production definitions in the editor overview and the ordinary visibility mode at the end. Ten display scenarios plus search were observed; eleven driver stages completed. The final run entered the drawing desk automatically without external input assistance.
- Checked Chinese and English translations (`研究效率提升` / `Boosted Research`), 15%, 45%, 100% and 175% zoom, GUI scales 2 and 3, and a physical 640 by 480 window with a 320 by 240 GUI. In all ten settled display scenarios, geometry-plan and label-plan rebuild deltas were both zero over the observed static frames. The 15% wide overview displayed 79 nodes with one icon flush; observed display-scenario flush counts ranged from 1 to 5 depending on detail and icon types.
- Real screen input passed expanded tiny-marker clicks, project opening, Escape camera retention, focus zoom, pointer-anchored wheel zoom, screen-distance panning, search projection reuse, clearing an empty graph, empty-selection focus, and normal-mode definition filtering.
- Captured [15 original framebuffer PNGs](../build/research-ui-validation/2026-10-24_01-56-59/) and visually inspected Chinese overview/icon/card views, the narrow overview, English overview and project dialog. Titles are present and readable, labels stay within the graph, and toolbar/modal placement remains usable.
- Earlier client attempts exposed setup issues: opening during world loading, a normal logout event during initial level cleanup, incomplete direct block placement, and opening a server menu before the client block entity had arrived. The final driver uses normal item placement followed by client-side interaction after synchronization. An assisted intermediate rendering run passed structural checks but showed blank names; that visual result was rejected and led to the text fixes and explicit translation/nonempty-name assertions. The first text-only repair was insufficient because the component reader also had not entered its first line; the final run validates both corrections.
- `git diff --check` passed for the changed tracked files. The driver restored the original GUI scale, FPS cap, language and pause-on-lost-focus settings.

## Remaining

- None for the requested behavior. Runtime FPS and allocation benchmarking was not performed; the recorded performance evidence covers static cache reuse, visible node counts and icon flushes.
