# Scenario font cache memory optimization

- Time: `2026-08-20 16:41:22 +08:00`
- Authors: `Codex (OpenAI, engineering planning role)`
- Status: `completed`
- Scope: `FH scenario Java2D font loading, Unihex storage, glyph image caching, and resource reload lifecycle`
- Related: `KGlyphProvider`, `UnihexParser`, `GlyphData`, `GraphicGlyphRenderer`, `LayerManager`, `RegisterClientReloadListenersEvent`

## Goal

Reduce the stable client heap retained by the FH scenario font provider without changing font selection, glyph metrics, pixel output, formatting, fallback, or scenario layout behavior. The implementation must also make resource reload transactional and safe while scenario prerender jobs are running on multiple background threads.

This plan targets the measured FH font allocation. It does not claim to solve the separate model-bakery, ResourcefulLib, FTB Chunks, Embeddium, or vanilla rendering allocation costs.

## Verified Current State

### Measured baseline

The 2026-08-20 client audit, after a clean restart and Full GC, found:

- `115,404` `com.teammoeg.frostedheart.content.scenario.client.gui.layered.font.GlyphData` objects retaining about `7.0 MiB` shallow heap.
- About `112,993` `java.awt.image.BufferedImage` objects, plus matching `IntegerInterleavedRaster`, `SinglePixelPackedSampleModel`, `DataBufferInt`, and `StateTrackableDelegate` objects.
- The Java2D image object graphs retain at least tens of MiB of shallow heap. Their pixel `int[]` data is mixed into the process-wide `[I` total, so the exact retained pixel total was not isolated without a heap dump.
- The counts remained stable during ordinary play. This is an oversized eager cache, not a steady-state leak.

Acceptance comparisons must repeat the same post-Full-GC class histogram after implementation.

### Eager Unihex materialization

`UnihexParser.readFromStream` currently performs this work for every `.hex` entry during resource reload:

1. Parse sixteen bitmap rows.
2. Allocate one `BufferedImage` for the code point.
3. Write every pixel into that image.
4. Allocate one `GlyphData` and retain the image from `KGlyphProvider.data` and `KGlyphProvider.unicodeData`.

Minecraft's default Unihex resource contains roughly 113k glyphs, so almost the entire Unicode font becomes Java2D image state even if a play session renders only a few hundred distinct characters.

### Mutable shared render cache

`GlyphData.renderFont` lazily mutates `shadowGraphics`, `chachedGraphics`, and `cachedColor`. `LayerManager.renderThread` is a configurable fixed thread pool, so multiple scenario prerender jobs can render the same `GlyphData` concurrently. The current last-color image is therefore a shared mutable cache with no synchronization.

`cachedColor` is never assigned after rebuilding `chachedGraphics`. Non-white text recolors the same image on every draw even when the color has not changed.

### Non-transactional resource reload

`KGlyphProvider.prepare` calls `onResourceManagerReload`, which mutates the singleton's live maps on the reload preparation executor. `apply` is empty.

Consequences:

- Render threads can read maps while the reload thread writes them.
- `data` and `unicodeData` are not cleared before loading.
- `putIfAbsent` retains old glyphs after a resource pack removes or changes them.
- A failed or partial reload can expose a mixed old/new font state.
- Referenced font JSON files have no cycle detection.
- `readBitmap` does not close the resource stream explicitly.

### Existing behavior to reuse

- `RegisterClientReloadListenersEvent` already owns listener registration. Keep this entry point.
- Bitmap providers already share a sheet `BufferedImage` across their glyphs. Keep that representation; it is not the measured object explosion.
- Legacy Unicode providers share 256 page images. Keep their current representation in the first implementation.
- `GraphicGlyphRenderer` already centralizes style, shadow, bold, italic, underline, and strikethrough behavior. Keep it as the rendering entry point.
- `SimplePreparableReloadListener` already supplies a prepare/apply boundary. Use it correctly instead of adding a new executor or event system.

## Decisions

### 1. Compact Unihex source store

Replace eager per-code-point `GlyphData + BufferedImage` creation with a frozen `UnihexGlyphStore` containing primitive data:

- code point to glyph index lookup;
- sixteen 32-bit bitmap rows per glyph in one flattened `int[]`;
- compact left/right or width metadata in primitive arrays;
- advance and Unicode metrics derived exactly as today.

The parser writes into a builder using one reusable sixteen-row scratch array. Freezing the builder trims its backing arrays and prevents later mutation.

Target source-store budget: no more than `16 MiB` for the measured approximately 113k-glyph default Unihex resource, excluding temporary reload buffers.

### 2. Lazy, bounded rendered-image cache

Materialize a Java2D image only when `GraphicGlyphRenderer` requests a glyph/color combination. Store completed images in a cache owned by the active font snapshot.

- Cache key: font source identity, glyph index, and final ARGB color.
- White, shadow black, and styled colors use the same cache path.
- Cache value: an immutable-after-publication `BufferedImage` and immutable metrics/source rectangle.
- Maximum: `4096` rendered image variants.
- Eviction: access-order LRU.
- Thread safety: synchronize cache lookup/materialization/eviction; never mutate a cached image after publication.
- No new dependency and no user-facing configuration in the first implementation.

The maximum bounds the cache even if scripts deliberately render many Unicode code points and colors. A 4096-entry limit is large enough for normal Chinese/Latin scenario text while keeping the image cache in the low tens of MiB at worst.

### 3. Immutable transactional font snapshots

Change the listener to `SimplePreparableReloadListener<FontSnapshot>`:

```text
resource reload executor                         live readers
------------------------                         ------------
ResourceManager
      |
      v
FontSnapshot.Builder
  - load references
  - parse bitmap/space/legacy providers
  - build compact Unihex stores
  - validate cycles and provider data
      |
      v
immutable FontSnapshot  -----------------------> apply: volatile swap
                                                    |
scenario-render-pool -------------------------------+
  resolve glyph from one snapshot
  materialize/cache image in that snapshot
```

`prepare` builds only local state. `apply` performs one volatile reference assignment. A scenario render job that already holds a glyph from the old snapshot can finish safely; the old snapshot and its cache become collectible when the job releases them.

Do not call `BufferedImage.flush()` on the old snapshot during `apply`, because a background scenario prerender job may still be drawing it.

### 4. Preserve lookup and rendering semantics

Before changing provider resolution, characterize the current behavior with tests. Preserve:

- normal versus `forceUnicodeFont` lookup;
- provider and resource-stack precedence;
- missing-glyph and space advance behavior;
- bitmap atlas source rectangles;
- Unihex width, advance, and `size_overrides` handling;
- white, black shadow, arbitrary color, bold, italic, underline, and strikethrough output.

The only intentional behavior corrections are:

- resource reload fully replaces old provider state;
- cyclic references fail with a descriptive error instead of recursion overflow;
- identical color requests reuse an immutable cached image rather than recoloring shared state.

### 5. Keep the first diff scoped

Expected production files:

- `KGlyphProvider.java`: snapshot ownership, provider loading, and lookup.
- `UnihexParser.java`: emit compact bitmap rows instead of images.
- `GlyphData.java`: immutable glyph descriptor/render source; remove mutable last-color fields.
- `GraphicGlyphRenderer.java`: resolve and render through one snapshot-safe path.
- New `UnihexGlyphStore.java`: compact frozen data plus builder and image materialization.
- New `GlyphImageCache.java` only if keeping the LRU inside `FontSnapshot` makes that class materially harder to test.

Six production files is the upper bound. Do not rewrite `LayerManager`, `PrerenderParams`, or the scenario text layout system for this optimization.

## Detailed Data Flow

### Reload path

```text
prepare(resourceManager)
  |
  +-- load minecraft:font/default.json
  |     |
  |     +-- bitmap --------> shared atlas GlyphData
  |     +-- space ----------> metrics-only GlyphData
  |     +-- legacy_unicode -> existing paged representation
  |     +-- unihex ---------> UnihexGlyphStore.Builder
  |     +-- reference ------> recursively load with visiting set
  |
  +-- validation error? ----> fail prepare; active snapshot stays unchanged
  |
  +-- freeze maps/stores/cache owner
        |
        v
apply(snapshot) -----------> activeSnapshot = snapshot
```

### Render path

```text
GraphicGlyphRenderer.accept(codePoint, style)
  |
  +-- snapshot = KGlyphProvider.activeSnapshot()
  +-- choose normal or force-Unicode lookup
  +-- explicit bitmap/space glyph found? ---- yes --> immutable descriptor
  |                                           no
  +-- compact Unihex index found? ----------- yes --> immutable descriptor
  |                                           no
  +-- GlyphData.EMPTY
  |
  +-- shadow color key -> bounded image cache -> draw
  +-- foreground key --> bounded image cache -> draw
  +-- optional bold second draw
  +-- existing decoration drawing
```

One `accept` call must use a single snapshot for lookup, metrics, and image materialization. Do not re-read the volatile active snapshot between those operations.

## Implementation Steps

1. Add characterization tests before structural changes.
   - Parse representative 8-, 16-, 24-, and 32-bit Unihex lines.
   - Capture exact width, advance, and pixel masks, including `size_overrides`.
   - Capture bitmap/space/missing glyph and normal/force-Unicode selection.
   - Capture white, black shadow, and arbitrary-color pixels.

2. Introduce `UnihexGlyphStore` and change `UnihexParser` to feed its builder.
   - Reuse a single row scratch buffer while parsing.
   - Copy rows into flattened primitive storage.
   - Reject malformed row lengths and invalid hex with line/code-point context.
   - Preserve duplicate-code-point precedence from the characterized behavior.

3. Introduce `FontSnapshot` inside `KGlyphProvider` or as a package-private type.
   - Move maps, compact stores, and rendered-image cache under the snapshot.
   - Remove mutable `ResourceManager rm` state.
   - Add a reference visiting stack/set with descriptive cycle errors.
   - Close all opened streams with try-with-resources.
   - Make reload all-or-nothing and swap only in `apply`.

4. Replace mutable per-glyph color images with the bounded snapshot cache.
   - Fully build each image before inserting it.
   - Never recolor a published image.
   - Ensure cache hit, miss, and eviction preserve pixel output.
   - Ensure concurrent requests for the same key return a complete image.

5. Run automated and client validation.
   - Run the full JUnit suite.
   - Reload resources repeatedly while scenario text prerenders.
   - Compare reference screenshots for Latin, Simplified Chinese, supplementary-plane code points, icons, formatting, and forced Unicode.
   - Repeat class histograms and a short JFR allocation recording after a clean restart.

6. Update living documentation and development history with the implemented behavior and measured result.

## Test Coverage Plan

Create tests under `src/test/java/com/teammoeg/frostedheart/content/scenario/client/gui/layered/font/`.

```text
CODE PATHS
[+] UnihexParser
 |-- valid 4/5/6-digit code point ---------------- [unit]
 |-- valid 8/16/24/32-bit rows ------------------- [unit]
 |-- size override hit/miss/boundary ------------- [unit]
 |-- malformed delimiter/hex/row length ---------- [unit]
 `-- duplicate code point precedence ------------- [unit]

[+] UnihexGlyphStore
 |-- lookup hit/miss/supplementary code point ----- [unit]
 |-- exact metrics and pixel reconstruction ------- [unit]
 |-- frozen storage estimate <= target ------------ [unit]
 `-- builder scratch reuse does not alias rows ----- [unit]

[+] FontSnapshot reload
 |-- successful prepare/apply replaces all state -- [unit/integration]
 |-- failed prepare preserves previous snapshot --- [unit]
 |-- removed glyph disappears after reload -------- [regression]
 |-- cyclic reference reports the chain ----------- [unit]
 `-- old snapshot remains drawable during swap ----- [concurrency]

[+] GlyphImageCache
 |-- miss builds once; hit reuses ----------------- [unit]
 |-- white/black/color pixels match baseline ------ [unit]
 |-- 4097th key evicts least-recently-used -------- [unit]
 |-- concurrent same/different-color access -------- [concurrency]
 `-- published images never mutate ---------------- [concurrency]

USER FLOWS
[+] Start client without opening scenario
 `-- no eager Unihex BufferedImage population ------ [manual + histogram]
[+] Render representative scenario dialogue
 `-- layout and pixels match baseline -------------- [manual screenshot]
[+] Toggle Force Unicode Font
 `-- lookup and spacing match baseline ------------- [manual + unit]
`-- Reload resource packs during prerender
    `-- no mixed font state/crash/stale glyph ------- [manual + regression]
```

The test design should isolate parser/store/cache logic from Minecraft initialization. ResourceManager integration may use a small fake implementation or a package-private loader seam; do not start a full client in unit tests solely to reach the builder.

## Failure Modes

| Failure | Prevention or handling | Test | Player impact |
|---|---|---|---|
| Compact rows are aliased to the parser scratch array | Builder copies all sixteen rows before the parser reuses the buffer | Scratch-reuse unit test | Corrupt glyph pixels; visible immediately |
| Provider precedence changes | Characterization tests lock current normal and force-Unicode selection | Precedence regression tests | Wrong icons/font glyphs |
| Cache publishes a partially colored image | Build under cache synchronization and publish only after completion | Concurrent same-key test | Intermittent wrong-colored text |
| Resource reload exposes mixed maps | Build local snapshot and swap once in `apply` | Reload replacement test | Random missing/stale glyphs |
| Old snapshot is flushed while a render job uses it | No eager flush; rely on reachability and GC | Concurrent swap/draw test | Render exception or blank dialogue |
| Bad font reference loops forever | Track the reference stack and include the cycle in the exception | Cycle test | Resource reload fails clearly rather than stack overflow |
| Cache cap is too small for a script | LRU rematerializes evicted glyphs correctly; cap is package-private for benchmark experiments | Eviction correctness and churn benchmark | More prerender CPU, no visual failure |
| Cache cap is bypassed by color variants | Include color in one globally bounded snapshot cache | 4097 distinct-key test | Prevents renewed unbounded image retention |
| Failed reload replaces the working font | Swap only after successful prepare | Failed-prepare test | Existing font remains usable |

No planned failure mode is allowed to be silent without both a test and an explicit invariant.

## Performance Acceptance Criteria

Use a clean client restart, the same modpack/resource packs, and the same language for before/after comparisons.

1. Immediately after startup and Full GC:
   - Unihex entry count may remain approximately 113k in compact primitive form.
   - Unihex must not create one `GlyphData` or `BufferedImage` per entry.
   - Compact Unihex source storage must remain at or below `16 MiB` by explicit array-capacity accounting.

2. Before any scenario text renders:
   - Rendered Unihex image cache size must be `0`.
   - Remaining font `BufferedImage` objects must be bitmap/legacy shared sheets or unrelated vanilla/mod images, not per-Unihex-glyph images.

3. After representative Latin and Chinese scenario dialogue:
   - Cache entries must equal the distinct rendered source/color keys, capped at `4096`.
   - Re-rendering identical text must allocate no new glyph images and produce cache hits.

4. After at least three `F3+T` resource reloads:
   - Active glyph/store/cache counts must return to one snapshot's counts after Full GC.
   - Removed or changed resource-pack glyphs must not survive from prior snapshots.

5. Visual equivalence:
   - Pixel or screenshot comparisons must cover normal, shadow, bold, italic, underline, strikethrough, arbitrary color, icons, forced Unicode, CJK, and a supplementary-plane code point.

6. Runtime regression guard:
   - Scenario prerender time for a warm representative page must not regress by more than `5%` across at least 30 runs.
   - A cold page may pay one-time glyph materialization, but p95 must remain below the existing page prerender p95 or be explicitly reviewed before landing.

Expected outcome: remove the majority of the measured FH font-related Java2D retained heap and increase available G1 headroom. Do not promise a specific GC interval because model-bakery caches and unrelated per-frame allocations still dominate the client.

## Documentation Impact

Implementation must:

- add a current scenario-font document under `docs/` with status, verification date, lifecycle, cache limit, reload behavior, and code anchors;
- add the scenario system entry to `docs/README.md` if no scenario entry exists at implementation time;
- add a new diary entry with before/after class counts, retained bytes, JFR allocation observations, test command, and documentation impact;
- update this plan to `completed`, `superseded`, or `abandoned` with the actual outcome.

## NOT in Scope

- Replacing Java2D scenario rendering with Minecraft's GPU font renderer: much larger visual and layout blast radius.
- Building a GPU glyph atlas: unnecessary for fixing the measured retained heap and requires render-thread texture lifecycle work.
- Optimizing vanilla `FontManager` or its own Unihex provider: FH duplicates the font specifically for Java2D scenario prerendering.
- Reworking `LayerManager`, `PrerenderParams`, or dynamic texture upload: separate allocation/performance work.
- Optimizing legacy Unicode page storage in the first diff: it shares page images and was not the measured default-font explosion.
- Model-bakery, ResourcefulLib highlight, FTB Chunks, Embeddium, Oculus, or Citizen memory work: independently measured sources.
- Adding a user-facing cache-size configuration: a fixed tested bound is simpler and safer; reconsider only if real scenario packs show churn.

## Parallelization

Sequential implementation, no parallelization opportunity. Parser, snapshot, glyph descriptor, cache, and tests share one small package and their contracts must evolve together; parallel worktrees would create more merge and semantic risk than schedule benefit.

## Outcome

Completed on `2026-08-20`.

- Replaced eager per-code-point Unihex `GlyphData` and `BufferedImage` creation with `UnihexGlyphStore` flattened primitive rows, compact bounds, and primitive first/last lookup indexes.
- Added one synchronized access-order `GlyphImageCache` per immutable `FontSnapshot`, with a hard limit of 4096 glyph/color variants and immutable-after-publication images.
- Changed `KGlyphProvider` to build local snapshots in `prepare` and publish them with one volatile assignment in `apply`. Replacement snapshots remove stale glyphs, failed preparation retains the active snapshot, and active reference cycles fail with their chain.
- Removed mutable per-glyph color images and the broken `cachedColor` path from `GlyphData`. `GraphicGlyphRenderer` now holds one snapshot per text render and no longer shares a mutable transform across render threads.
- Added focused tests for Unihex parsing and pixels, `size_overrides`, malformed input, duplicate precedence, compact storage accounting, cache hits/colors/LRU/cap/concurrency, provider precedence, snapshot replacement, failed-preparation retention, stale-glyph removal, and old-snapshot drawing.
- Added current behavior documentation at [`docs/scenario-font-rendering.md`](../docs/scenario-font-rendering.md).

Validation: `./gradlew.bat test --console=plain` passed. `UnihexGlyphStore.estimatedStorageBytes(113000)` is `11,652,320 bytes`, below the `16 MiB` target. A post-change class histogram, JFR allocation capture, and visual client comparison still require a clean client restart with this build; no live after-count is claimed here.
