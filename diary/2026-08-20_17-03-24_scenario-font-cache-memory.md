# Scenario font cache memory optimization

- Time: `2026-08-20 17:03:24 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `FH scenario Java2D Unihex storage, rendered glyph cache, resource reload lifecycle, concurrency, tests, and living documentation`

## Completed

- Replaced eager Unihex `GlyphData` and `BufferedImage` creation with `UnihexGlyphStore`, which freezes sixteen bitmap rows per entry into one flattened `int[]` plus compact bounds and primitive lookup maps.
- Added lazy glyph/color materialization through a snapshot-owned `GlyphImageCache`, capped at 4096 access-ordered entries.
- Reworked `KGlyphProvider` into a transactional `SimplePreparableReloadListener<FontSnapshot>`: preparation is local, publication is one volatile swap, removed glyphs do not survive replacement, and active reference cycles report their chain.
- Made `GlyphData` immutable and removed its unsynchronized `shadowGraphics`, `chachedGraphics`, and never-updated `cachedColor` state.
- Made `GraphicGlyphRenderer` retain one snapshot for a text prerender and removed its shared mutable transform. Non-italic characters no longer copy a transform object.
- Added regression tests and documented the current lifecycle in [`docs/scenario-font-rendering.md`](../docs/scenario-font-rendering.md).

## Decisions

- Preserve provider order, Force Unicode behavior, Unihex source bounds, metrics, pixel masks, fallback advance, bitmap atlas sharing, legacy page sharing, and all existing scenario formatting/layout behavior.
- Use a fixed 4096-entry cache rather than configuration. The cap bounds adversarial glyph/color combinations while eviction changes only cold rematerialization cost.
- Keep old snapshots reachable for in-flight renderers and do not call `BufferedImage.flush()` during `apply`.
- Keep this change scoped to FH's Java2D scenario font path. Vanilla font management, GPU atlases, `LayerManager`, model bakery, Citizen rendering, and other mod caches remain separate work.

## Validation

- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.*" --console=plain`: passed.
- `./gradlew.bat test --console=plain`: passed.
- Tests cover 8/16/24/32-bit rows, `size_overrides`, exact metrics/masks/colors, supplementary code points, malformed input, duplicate precedence, scratch-array copying, cache hits, color variants, LRU eviction, the 4096-entry limit, concurrent publication, provider precedence, failed-preparation retention, stale-glyph removal, snapshot swapping, and continued old-snapshot drawing.
- Before change, the clean post-GC histogram contained approximately 115,404 `GlyphData` objects (`about 7.0 MiB` shallow) and approximately 112,993 `BufferedImage` objects with matching raster/sample-model/data-buffer graphs attributable to eager Unihex loading.
- The replacement source representation has no eager per-Unihex `GlyphData` or image. `UnihexGlyphStore.estimatedStorageBytes(113000)` is `11,652,320 bytes` (`about 11.1 MiB`) including rows, bounds, and two primitive hash tables.
- No post-change class histogram or JFR result is claimed. Those require rebuilding and restarting the client with this implementation.

## Remaining

- Restart the client with this build, force a Full GC before opening scenario text, and verify that per-Unihex `GlyphData`/`BufferedImage` populations are absent.
- Render representative Latin, Simplified Chinese, supplementary-plane, icon, shadow, bold, italic, underline, strikethrough, arbitrary-color, and Force Unicode samples; compare screenshots and warm prerender timing.
- Repeat at least three `F3+T` reloads, then capture a post-GC class histogram and short JFR allocation recording. Confirm one live snapshot after GC and a cache size no greater than 4096.
