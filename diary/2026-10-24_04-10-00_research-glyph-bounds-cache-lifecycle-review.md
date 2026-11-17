# Research glyph bounds and cache lifecycle review

- Time: `2026-10-24 04:10:00 +08:00`
- Author: `Codex; OpenAI GPT-6; correctness review, font asset repair and real-game validation`
- Status: `completed`
- Scope: Research label bitmap generator/asset, title-cache lifecycle review and opt-in client validation

## Completed

- Rechecked geometry, cache keys, per-source ownership, offscreen culling/reentry, input, component parsing and same-language resource reload. The current graph cache's width/two-line/presentation-revision key remains sufficient: Minecraft's normal resource reload creates and injects a fresh `ClientLanguage` even when the language code is unchanged, triggering the archive's existing presentation revision. Production viewport/cache code is retained.
- Found a real font-generation issue: baseline 9 in a 12-pixel source cell cropped some tall glyphs, including common characters such as 究/效/率 and accented letters. The generator now fits horizontal bearings and vertical bounds while preserving the usual baseline where possible. An oversized glyph uses the existing 9-pixel source face. The bitmap is regenerated offline; runtime font metrics, atlas cell dimensions and drawing code are retained.
- Added actual client cases that expose projects four rank columns away and return to the original view, checking new node arrivals, title ownership and cached reentry. Added a normal same-language resource reload, checking resource completion, language-instance replacement, presentation revision, title refresh and camera retention.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with glyph fitting, revised file costs and the additional lifecycle checks. Completed historical entries remain unchanged.

## Decisions

- Repair glyph cropping in offline asset preparation instead of adding per-frame positioning or rasterization. Keep the reviewed viewport/cache implementation because no new runtime defect was found there.
- Exercise real cache invalidation and offscreen reentry, supplementing the prior warm nearby-pan check. Run production-catalogue checks, the selected real Forge GameTests and the actual Minecraft client together after edits.

## Validation

- Original source-font bounds exceeded the fixed-position cell for 3,293 of 7,615 covered characters; one glyph exceeded the cell's dimensions and fits at size 9. Font raw-material inspection identified the cause. The regenerated asset fits those bounds before quantization and uses the same runtime cell dimensions.
- New PNG: 322,984 bytes; character map: 25,391; OFL notice: 4,595; total 352,970 bytes (about 345 KiB). Raw ZIP DEFLATE level 6 totals 341,905 bytes (about 334 KiB), excluding archive headers. Source RGBA allocation remains 3,655,680 bytes (about 3.49 MiB).
- Final command: `.\gradlew.bat validateResearchCatalog runGameTestServer runResearchUiClient -PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login --offline --no-daemon --console=plain`, Java 17, current checkout. [Log](../build/research-ui-glyph-lifecycle-review-20261024-final.log): `BUILD SUCCESSFUL in 3m 5s`; all 86 production definitions and all four required research/login Forge GameTests passed.
- [Client result](../build/research-ui-validation/2026-10-24_04-14-10/result.json): 21 observations and 26 interaction stages passed. Repeated nearby panning produced zero title rebuilds. Distant panning introduced 17 previously offscreen projects; returning restored the original IDs with zero title rebuilds. A normal same-language reload completed, advanced presentation revision, rebuilt 79 visible title layouts and preserved the camera.
- All 16 static display intervals reported zero geometry/title rebuilds. Compared all common scenario node/icon/name/full-name/two-line/character counts with the previous run: no differences. Per-node ownership, language-switch rendering, minimum-zoom input, modal retention, focus, pointer-anchored smooth zoom, search, empty graph and ordinary visibility all passed.
- Actual font observations remained one 256 KiB atlas payload and 3,655,680 bytes of source image allocation. Inspected a [Chinese compact crop](../build/research-ui-validation/2026-10-24_04-14-10/zh-complete-glyphs-crop.png), [old/new stroke comparison](../build/research-ui-validation/2026-10-24_04-14-10/glyph-stroke-comparison.png) and the [same-language reload crop](../build/research-ui-validation/2026-10-24_04-14-10/same-language-reload-crop.png) from real framebuffer captures.
- `git diff --check` passed for touched tracked source/documentation. Runtime viewport/cache code stayed unchanged in this review. No copied checkout, artifact pinning, extra runtime font logic, JUnit or standalone model tests were introduced.

## Remaining

- None for the identified glyph-cropping defect and reviewed cache lifecycle. These results cover the stated game scenarios and supplied font material; they are not an absolute guarantee for all future data, resources or hardware.
