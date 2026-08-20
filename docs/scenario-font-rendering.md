# Scenario font rendering

- Status: `Current`
- Last verified: `2026-08-20`
- Scope: `FH scenario Java2D font lookup, Unihex storage, rendered-image caching, concurrency, and resource reload`
- Code anchors: `FHClientEventsMod.registerReloadListeners`, `KGlyphProvider`, `KGlyphProvider.FontSnapshot`, `UnihexParser.readFromStream`, `UnihexGlyphStore`, `GlyphImageCache`, `GlyphData`, `GraphicGlyphRenderer`

## Purpose and entry point

The layered scenario UI prerenders text into Java2D images instead of drawing through Minecraft's GPU font renderer. `FHClientEventsMod.registerReloadListeners` registers the singleton `KGlyphProvider.INSTANCE` as a `RegisterClientReloadListenersEvent` listener. It begins at `minecraft:font/default.json` and supports the `bitmap`, `legacy_unicode`, `reference`, `unihex`, and `space` provider types used by Minecraft 1.20.1 resources.

`GraphicGlyphRenderer` remains the formatting and drawing entry point. Shadow, bold, italic, underline, strikethrough, color, advance, fallback, and layout behavior are applied there.

## Lookup behavior

`KGlyphProvider.FontSnapshot` stores providers in resource traversal order.

- Normal lookup searches from first to last. This preserves the former `data.putIfAbsent` behavior.
- Force Unicode lookup searches Unicode-capable `legacy_unicode` and `unihex` providers from last to first. This preserves the former `unicodeData.put` replacement behavior.
- Code point `U+0020` and an unresolved code point use `GlyphData.EMPTY`, preserving the existing fallback advance.
- A duplicate code point inside one Unihex source uses its first entry for normal lookup and its last entry for Force Unicode lookup.

Each `GraphicGlyphRenderer` captures one `FontSnapshot` when it is constructed. One text prerender therefore uses one provider set and one cache even if a resource reload completes concurrently.

## Unihex storage

`UnihexParser.readFromStream` parses 8-, 16-, 24-, and 32-bit Unihex rows into a reusable sixteen-row scratch array. `UnihexGlyphStore.Builder.add` copies those rows into flattened primitive arrays and records compact left/right bounds plus primitive code-point indexes.

The frozen `UnihexGlyphStore` stores no per-code-point `GlyphData` or `BufferedImage`. It creates a Java2D image only when that glyph is drawn. `size_overrides`, metrics, source-bit selection, duplicate precedence, and supplementary code points are covered by unit tests.

At the previously measured approximately 113,000 default Unihex entries, `UnihexGlyphStore.estimatedStorageBytes` accounts for about `11,652,320 bytes` (`11.1 MiB`) for flattened rows, bounds, and two primitive lookup tables. The acceptance ceiling is `16 MiB`. This replaces the former eager graph of approximately 115,404 `GlyphData` objects and 112,993 per-glyph `BufferedImage` objects.

Bitmap providers still share their atlas `BufferedImage`. Legacy Unicode providers still share one image per loaded page. Those representations were not the source of the measured per-code-point image explosion.

## Rendered-image cache

Every `FontSnapshot` owns one `GlyphImageCache`.

- Limit: `4096` image variants.
- Policy: synchronized access-order least-recently-used eviction.
- Key: snapshot-local glyph cache ID plus final ARGB color.
- Publication: an image is completely materialized before insertion and is not mutated afterward.
- Variants: Unihex white, shadow black, and arbitrary colors use the cache. Bitmap and legacy white rendering draw directly from their shared source atlas; colored variants use the same bounded cache.

The fixed limit is not runtime-configurable. A page that exceeds it remains visually correct but may rematerialize evicted glyph/color variants.

## Resource reload lifecycle

`KGlyphProvider` extends `SimplePreparableReloadListener<KGlyphProvider.FontSnapshot>`.

1. `prepare` follows font references, reads every resource into local provider objects, closes resource streams, detects active reference cycles, and builds a complete snapshot.
2. A parse, I/O, image decode, or reference error fails preparation. `apply` is not called, so the active snapshot remains unchanged.
3. `apply` performs one volatile assignment to publish the new snapshot.
4. Removed providers and glyphs are absent from the replacement snapshot; no mutable singleton maps survive across reloads.
5. The old snapshot is not explicitly flushed. In-flight scenario renderers may finish with it, after which normal reachability-based garbage collection reclaims its source data and cache.

This lifecycle does not add an executor. Resource preparation uses Minecraft's reload pipeline, while scenario prerendering continues to use the existing `LayerManager.renderThread` pool.

## Validation

Automated coverage under `src/test/java/com/teammoeg/frostedheart/content/scenario/client/gui/layered/font/` verifies parsing, bounds, metrics, exact mask colors, malformed input, duplicate precedence, cache hits, color separation, the 4096-entry cap, LRU eviction, concurrent same-key publication, provider precedence, snapshot replacement, failed-preparation retention, stale-glyph removal, and continued old-snapshot drawing.

A clean client restart is required for post-change `GC.class_histogram` and JFR measurements. Before any scenario text is opened, there should be no per-Unihex-glyph `BufferedImage` population and the active snapshot cache should contain zero rendered variants. After text is rendered, the cache must remain at or below 4096 entries.
