# Research graph rigid panning and native-size labels

- Time: `2026-11-05 23:06:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-client validation`
- Status: `completed`
- Scope: `ResearchGraphViewport`, rank spacing, removal of compact label font assets and existing client validation

## Completed

- Identified panning jitter: the previous `round(origin + world * zoom)` independently rounded each node after applying camera movement. Fractional drag deltas made neighboring nodes cross rounding boundaries on different frames, changing visible gaps by one GUI pixel.
- Changed the research viewport to round one common camera translation, then add each fixed-zoom projected world offset. Nodes, edge endpoints and grid use this origin; edge bends use a translation-invariant midpoint. Hit testing uses the same planned coordinates. No extra transform/cache subsystem was added.
- Replaced the tiny dedicated label bitmap with `Style.EMPTY` and Minecraft's normal default font at native size. Text has an 8-pixel body and 9-pixel line advance. Labels retain one shared baseline and use the existing matrix, without per-glyph adjustments or fractional camera scaling. Joined frames and per-source title caches remain.
- Increased `ResearchGraphLayoutEngine.RANK_SPACING` from 176 to 208 world units so the minimum-zoom text budget is 19 pixels, retaining two ordinary CJK characters next to an 8-pixel real icon. Layout spacing is fixed during panning; wider ranks show fewer columns in the same viewport, a deliberate readability tradeoff.
- Removed the unused compact font image, character map, OFL notice and offline generation script. This removes 346,849 resource bytes and the dedicated 3,655,680-byte decoded source image. The ordinary default font/atlas remains Minecraft-owned.
- Extended each existing real-client display scenario with fractional-delta middle-button dragging. Per-frame checks compare common nodes/icons/labels for identical translation, size and title retention; GUI scales 1/2 also compare actual title pixels after accounting for translation. Existing cache, input, zoom and resource-reload checks are retained.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with native font metrics, rank spacing, common projection, removed font costs and rendered-motion verification.

## Decisions

- Fix the common transform rather than adjusting node positions independently. Use the native font rather than another external typeface or a rerasterization pipeline.
- Preserve project identity and minimum-zoom short names while giving native text enough room. Validate actual moving frames, because static geometry/cache counts did not cover the reported defect.

## Validation

- Production catalogue: all 86 definitions passed. The selected real server research/login GameTests passed all four required cases in [the combined run](../build/research-ui-rigid-pan-native-font-20261105-final-r3.log).
- Existing unrelated deposit tests prevented the ordinary full `gameTest` source set from compiling: `DepositClientValidation` lacked `stateCounts`, and `DepositGameTests` supplied incompatible `ParameterList` generics. This run used the temporary ignored [init script](../build/research-ui-validation.init.gradle) to exclude just those two sources; their old generated class files also referenced the removed `DepositConfig` and were cleared. No deposit source was edited. Server validation therefore covers the selected research/login namespaces, not the unfinished deposit tests.
- The first client motion run completed 200 stable drag frames, then its blank-point helper could not find a blank point in a filled narrow viewport. The helper was corrected to start middle-button dragging at a fixed point, which works over projects. One retry stalled in ForgeGradle's remote environment connection check; its own pre-task daemon was stopped and subsequent offline invocation used the supported `-Dnet.minecraftforge.gradle.check.certs=false`. A later static observation saw a geometry refresh, so the observer now distinguishes changed render/projection inputs from unchanged inputs rather than treating genuine input changes as static.
- Removed leftover generated copies of the deleted font assets from `build/resources/main` before final validation. The final game therefore loads the actual native-font resource set.
- Final client command: `.\gradlew.bat validateResearchCatalog runResearchUiClient --init-script build/research-ui-validation.init.gradle -Dnet.minecraftforge.gradle.check.certs=false --offline --no-daemon --console=plain`. [Final log](../build/research-ui-rigid-pan-native-font-20261105-final-r6.log): `BUILD SUCCESSFUL in 2m 50s`.
- [Client result](../build/research-ui-validation/2026-11-05_23-27-54/result.json): 21 observations and 26 interaction stages passed. All 16 display configurations completed 20 actual fractional-delta middle-button drag frames each, totaling 320 rigid-pan frames. Every common node/icon/name translated by identical integer offsets with unchanged size/content. At GUI scales 1/2, 397,120 selected-title framebuffer pixels were compared with the original positions and matched exactly after translation. GUI scale 3 passed the geometry comparisons. This is a rendered motion check, not an FPS or monitor response-time benchmark.
- Static display intervals observed zero geometry/title rebuilds, and the final run required zero static-observation restarts. Warm repeated panning and return from a distant view produced zero title rebuilds. Distant panning exposed 17 new projects. Same-language resource reload refreshed 75 visible title layouts and preserved the camera. Actual intermediate wheel frames, pointer anchoring, minimum-zoom icon/name clicks, modal retention, focus, search, empty graph and ordinary visibility all passed.
- Chinese 15% in the 390 by 335 viewport retained 75 icons and 75 named nodes (152 title characters). Chinese 45% retained 24 named nodes with all 24 names complete. GUI scale 1 retained all 86 projects at minimum zoom. Counts reflect viewport clipping around the selected project; wider fixed ranks trade some simultaneous column visibility for native text size. English 45% retained all 24 names, including 13 complete names and 22 two-line names.
- Inspected actual [native-font compact crop](../build/research-ui-validation/2026-11-05_23-27-54/native-font-graph-crop.png), [minimum-zoom crop](../build/research-ui-validation/2026-11-05_23-27-54/native-font-minimum-crop.png) and GUI scale 3 screenshot. A [start/end GIF](../build/research-ui-validation/2026-11-05_23-27-54/rigid-pan-start-end.gif) contains two real captures held for comparison; it is not the timing of the runtime drag. `git diff --check` passed for touched tracked implementation/documentation.

## Remaining

- None for the reported per-node panning jitter and native-font replacement. Validation uses the default client font/resource configuration; alternate font packs, shaders and physical display-motion effects were not tested. No runtime FPS or total-memory delta is claimed.
