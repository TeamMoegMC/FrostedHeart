# Compact research bitmap font and measured resource cost

- Time: `2026-10-24 03:34:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `completed`
- Scope: Research label font assets, offline generator, client font-cache observations and research UI documentation

## Completed

- Removed the full 8,482,020-byte Noto Sans SC OTF after the user's explicit objection to its cost. Kept the separate icon/name frames, reserved names, fixed native text size, cached title sequences and continuous pointer-anchored graph zoom.
- Replaced the font provider with the same standard Minecraft bitmap mechanism already used by Chorda's font icons. Added a 640 by 1,428 bitmap with 10 by 12 pixel source cells, logical height/ascent 6 and sixteen alpha levels. Covered CJK glyphs advance at most 6 GUI pixels; GUI scale 2 draws source pixels at physical 1:1. No application renderer or runtime font-generation code was added.
- Added the small offline `Scripts/generate_research_label_font.py` generator. It derives 7,615 ASCII, Latin-1, GB2312 and ellipsis characters from an OFL font source, independently of research definitions. New names in that character set need no regeneration; other characters retain Minecraft fallback. The source OTF is a generation input only and is not shipped. Updated the derivative font's OFL notice.
- Added opt-in real-client observations for bitmap source image bytes, used glyph count, atlas count and atlas base-level payload. These are resource observations rather than FPS or total-process memory benchmarks.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with provider/cell metrics, supported character ranges, regeneration, bitmap cost, resident source memory, fallback limits and profiling boundaries. Finalized the preceding in-progress diary with its actual successful OTF validation and subsequent replacement reason.

## Decisions

- A full outline font was excessive for a small UI label style. Bake a generic reusable character set offline and retain the platform's existing font lifecycle and caches.
- Report the decoded source image and used atlas costs as well as compressed file size. The image is retained by the ordinary font provider even while the research interface is closed; it is not a zero-memory change.
- Do not claim unchanged FPS or a measured font-load-time delta without a before/after profile. Validate the changed asset, client rendering and interaction together after implementation/documentation.

## Validation

- Asset generation: PNG 316,863 bytes; font JSON 25,391; OFL notice 4,595; total 346,849 bytes (about 339 KiB). Raw ZIP DEFLATE level 6 totals 335,776 bytes (about 328 KiB), excluding archive headers. Source image RGBA allocation is 3,655,680 bytes (about 3.49 MiB), before font metadata/atlases. The removed OTF alone compressed to 7,341,872 bytes. No OTF remains under the mod's asset directory.
- Final unified command: `.\gradlew.bat validateResearchCatalog runGameTestServer runResearchUiClient -PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login --offline --no-daemon --console=plain`, Java 17, ordinary current-checkout outputs. [Final log](../build/research-ui-paired-frames-bitmap-20261024-final.log): `BUILD SUCCESSFUL in 2m 37s`; all 86 production definitions and all four required research/login Forge GameTests passed.
- [Client result](../build/research-ui-validation/2026-10-24_03-35-13/result.json): 16 display scenarios plus a search observation, 17 interaction stages and four actual zoom-transition captures; `intermediateZoomObserved = true`. Passed Chinese/English, GUI scales 1/2/3, narrow windows, former transition boundaries, minimum-zoom icon/name clicks, hover cache reuse, project dialogs, camera retention, focus, pointer anchoring, pan, search, empty graph and normal visibility.
- In the 390 by 335 GUI-pixel viewport, Chinese 15% retained 79 real 8-pixel icons and 79 nameplates containing 160 original-name characters. Chinese 45% retained all 24 complete names. English 15% retained 79 nameplates/213 characters; English 45% retained all 24 nameplates, 20 complete names. The narrow 118 by 175 view retained 24 icons and 24 two-character Chinese names at minimum zoom. GUI scale 1 retained all 86 graph projects/nameplates within its larger logical viewport. These are scenario counts around the selected project, not a claim that all 86 definitions always fit every viewport.
- Every observed scenario used one font atlas with 262,144 bytes of base-level RGBA pixel payload (256 KiB); used glyph counts ranged from 43 to 177. The actual provider's `NativeImage.size` reported 3,655,680 bytes in every scenario. Font Java metadata and driver overhead were not measured, so these are not total-memory or total-VRAM figures. No FPS or font-loading delta was claimed.
- All 16 settled display scenarios observed zero static geometry-plan and title-plan rebuilds. Captured actual frames under [the run directory](../build/research-ui-validation/2026-10-24_03-35-13/); inspected Chinese minimum/compact, English compact and GUI scale 3 minimum views. Created unscaled [paired-frame crop](../build/research-ui-validation/2026-10-24_03-35-13/zh-paired-frames-native-crop.png), [minimum-zoom crop](../build/research-ui-validation/2026-10-24_03-35-13/zh-minimum-native-crop.png) and a deliberately [slow-play zoom GIF](../build/research-ui-validation/2026-10-24_03-35-13/paired-frames-zoom-slow.gif) from those real framebuffer captures. Actual wheel animation remains 150 ms.
- `git diff --check` passed for the touched tracked source/documentation. No JUnit, standalone model tests, copied checkout or artifact pinning was used. The client restored GUI scale, FPS limit, pause-on-lost-focus, language and research editor settings on exit.

## Remaining

- None for the requested paired-frame/minimum-zoom name presentation and replacement of the oversized font asset. Very small physical text retains a resolution limit; characters outside the generic bitmap set use the existing font. Runtime FPS, load-time and total-memory comparisons were not performed.
