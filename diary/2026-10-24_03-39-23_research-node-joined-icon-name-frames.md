# Joined icon and name frames for research nodes

- Time: `2026-10-24 03:39:23 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-client validation`
- Status: `completed`
- Scope: `ResearchGraphViewport`, existing client validation and research UI documentation

## Completed

- Replaced the zoom-dependent 1-3 GUI-pixel gap with a shared one-pixel border between each project's icon mount and nameplate. The two paper fills and frame heights still distinguish icon and text, while the attached silhouette makes ownership clear.
- Returned 2-4 GUI pixels to the nameplate width, increasing the minimum text budget from 12 to 14 pixels. Existing node bounds, dependency layout, fixed-size bitmap typography, hit testing and smooth zoom continue to use the same geometry/cache path.
- Updated the existing client geometry assertion for joined frames. Documentation impact: updated the attachment and text-width formulas in [research-ui.md](../docs/research/research-ui.md). No font resources or additional rendering/cache structures were introduced.

## Decisions

- Share one border rather than add a connector or a new surrounding frame. This is a local geometry change with the same rendering passes.

## Validation

- Final command: `.\gradlew.bat validateResearchCatalog runResearchUiClient --offline --no-daemon --console=plain`, Java 17, ordinary current-checkout outputs. [Log](../build/research-ui-joined-frames-20261024-final.log): `BUILD SUCCESSFUL in 2m 27s`; 86 production research definitions passed.
- [Real-client result](../build/research-ui-validation/2026-10-24_03-41-16/result.json): 16 display scenarios plus search and 17 interaction stages passed, including Chinese/English, GUI scales 1/2/3, narrow windows, shared frame geometry, icon/name clicks, dialogs, camera retention, pointer-anchored intermediate zoom, dragging and search. Static display intervals observed zero geometry/title-plan rebuilds.
- Inspected actual Chinese 15%, 45% and 100% framebuffer captures. The shared border attaches the frames across those sizes. Chinese 15% retained 79 icons and 79 names; Chinese 45% retained all 24 complete names. English 45% retained 24 names with 22 complete names, compared with 20 complete names in the preceding wider-gap capture.
- Saved an unscaled [45% crop](../build/research-ui-validation/2026-10-24_03-41-16/zh-joined-frames-crop.png) and [15% crop](../build/research-ui-validation/2026-10-24_03-41-16/zh-minimum-joined-crop.png) from this run. `git diff --check` passed for the touched tracked source/documentation. No JUnit or standalone model tests were added.

## Remaining

- None.
