# Research graph paired frames and native-size labels

- Time: `2026-10-24 03:23:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `completed` (validated; full OTF subsequently removed after the user's resource-cost review)
- Scope: `ResearchGraphViewport`, research label font assets, opt-in client validation and research UI documentation

## Completed

- Replaced the large filled card with two separated cut-corner frames: a darker square icon mount and a paper nameplate. Both retain project state/hover/selection colors. They remain one node for graph layout, dependency endpoints and clicking. Nameplates are centered and fitted vertically to one or two lines; the space around the frames is not filled.
- Removed arbitrary fractional font scaling. Labels use fixed native-size `frostedheart:research_label` glyphs, 8-pixel line spacing, integer GUI-pixel origins and the existing screen matrix. Camera zoom changes available name space rather than font size. Existing 150 ms pointer-anchored wheel animation, node centers, dependency graph and 8-16 pixel real icons are retained.
- Added the unmodified Noto Sans SC Regular 2.002 font and SIL Open Font License. Its generic CJK/Latin coverage is independent of the current research catalogue. Minecraft's ordinary `ttf` provider owns glyph baking, atlases and reload; no custom text renderer, offscreen target or per-name texture was introduced. The font resource uses size 8.5, oversample 2 and shift [0, 1]. The binary is 8,482,020 bytes.
- Measurement, clipping and drawing share `TITLE_STYLE`. Cached styled character sequences and integer coordinates replace the separate title matrix. Every visible project retains its reserved nameplate, short prefixes and optional second line. At minimum zoom the available inner text width is 12 GUI pixels, enough for two native compact CJK glyphs.
- Extended the real-client driver to check the loaded font's CJK metrics, separated frame geometry, native title bounds and minimum-zoom name input alongside all existing scenarios. Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with frame formulas, typography, font asset provenance/cost, cache behavior and validation boundaries. Finalized the preceding in-progress diary with its actual successful validation; earlier completed history was not rewritten.

## Decisions

- Use the platform's existing font machinery with one generic font asset instead of shrinking a pixel-font texture or implementing a new text rendering/cache subsystem. Labels keep a stable size throughout camera motion.
- Checked Chorda after the user's follow-up: `UIElement#getFont` delegates through `PrimaryLayer` to `Minecraft.font`; its text helpers/styles and bitmap font-icon resources do not include a compact CJK face. `TextField`'s scaled rendering path still uses a scale matrix. The graph already reuses that native font path; no Chorda font engine was duplicated. The scenario image-text renderer is a separate subsystem and would add unnecessary per-title image handling here.
- Keep separate frames within the existing node bounds so the visual improvement does not add a floating-label placement algorithm or remove project names.
- Prepare implementation and documentation before a unified final real-game batch in this checkout. No JUnit, standalone model tests, copied checkout or artifact fingerprinting.

## Validation

- Unified production catalogue, Forge GameTest server and real-client command passed: [log](../build/research-ui-paired-frames-native-font-20261024-final.log), `BUILD SUCCESSFUL in 3m 33s`; all 86 definitions and all four required research/login GameTests passed.
- [Client result](../build/research-ui-validation/2026-10-24_03-26-00/result.json): 17 display/search observations and 17 interaction stages passed. Inspected the actual Chinese 15%, 45% and 100% screenshots: two separate frames and native-size Noto glyphs were present. Chinese 15% retained 79 icons and 79 names (161 characters); 45% retained all 24 complete names. Static display intervals reused geometry/title plans.
- The user asked for actual font cost and objected to the 8 MiB source font. Direct measurement found 8,482,020 OTF bytes and 7,341,872 bytes after raw ZIP DEFLATE at level 6. The next revision removes this binary and replaces it with a compact, catalogue-independent bitmap font. No runtime FPS or total memory comparison was performed for the discarded OTF version.

## Remaining

- The next diary entry owns the bitmap replacement, actual asset/cache observations and its final real-game validation. This entry records the discarded OTF version only.
