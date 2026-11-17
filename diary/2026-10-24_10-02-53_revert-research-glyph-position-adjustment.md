# Revert per-glyph positioning in research labels

- Time: `2026-10-24 10:02:53 +08:00`
- Author: `Codex; OpenAI GPT-6; rollback and real-client validation`
- Status: `completed`
- Scope: Research label bitmap generator/asset and research UI documentation

## Completed

- Reverted the previous glyph-bounds positioning change at the user's request. Moving glyphs independently made characters such as 研 sit at a different vertical position from neighboring characters; the previous visual acceptance was incorrect.
- Restored the original shared 10-pixel source face, horizontal origin 0 and baseline 9, and regenerated the original-size bitmap. Removed per-glyph translations and the size-9 fallback. Joined icon/name frames, title-cache optimization and existing client lifecycle checks are retained.
- Documentation impact: restored the baseline description and original asset costs in [research-ui.md](../docs/research/research-ui.md). The preceding diary remains a historical record; this entry supersedes its acceptance of per-glyph positioning.

## Decisions

- Honor the requested rollback. Preserve the original line alignment rather than introduce another font adjustment during the rollback.

## Validation

- Regeneration restored the PNG to 316,863 bytes and the character map to 25,391 bytes, matching the recorded pre-adjustment sizes. Source image dimensions remain 640 by 1,428.
- Final command: `.\gradlew.bat validateResearchCatalog runResearchUiClient --offline --no-daemon --console=plain`, Java 17, current checkout. [Log](../build/research-ui-revert-glyph-position-20261024-final.log): `BUILD SUCCESSFUL in 2m 6s`; all 86 definitions passed.
- [Client result](../build/research-ui-validation/2026-10-24_10-04-18/result.json): 21 observations and 26 interaction stages passed, including language/resource refresh, panning and existing graph interactions.
- Inspected the restored [research-title crop](../build/research-ui-validation/2026-10-24_10-04-18/restored-research-title.png). Its text region in the 45% actual framebuffer capture is pixel-identical to the pre-adjustment `2026-10-24_03-56-13` capture, confirming restoration of the original line alignment rather than relying on geometry-only assertions. `git diff --check` passed for the touched tracked documentation.

## Remaining

- None for the requested rollback.
