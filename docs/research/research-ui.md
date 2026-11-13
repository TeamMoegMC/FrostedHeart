# Research Progress And UI

- Status: `Current`
- Last verified: `2026-11-13` (shared project frame source review; native-label and rigid-panning visual evidence remains from November 5; shared-frame visual acceptance pending)
- Scope: Implemented research completion semantics, responsive drawing-desk archive, continuous graph zoom, readable localized names, project dialog, clue routing, and refresh boundaries
- Code anchors: [`Research`](../../src/main/java/com/teammoeg/frostedresearch/research/Research.java), [`ResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/ResearchData.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`DrawDeskScreen`](../../src/main/java/com/teammoeg/frostedresearch/gui/drawdesk/DrawDeskScreen.java), [`ResearchArchiveLayer`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchArchiveLayer.java), [`ResearchArchiveViewCache`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchArchiveViewCache.java), [`ResearchGraphViewport`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchGraphViewport.java), [`ResearchProjectWorkspace`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchProjectWorkspace.java), [`ResearchGraphLayoutEngine`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/graph/ResearchGraphLayoutEngine.java), [`CIconBatch`](../../src/main/java/com/teammoeg/chorda/client/icon/CIconBatch.java), `PanZoomViewport`, `UILayer`

## Current Player Surface

The legacy workspace and [datapack project details](../knowledge/workbench.md) now share `ResearchProjectFrame.drawPanel`, `drawTab`, `HEADER_HEIGHT = 35` and `TAB_HEIGHT = 20`. This reuses the original `TechIcons.DIALOG` artwork and tab styling without adapting new staged projects into legacy `Research` objects. The legacy dialog retains its detail/theory/experiment semantics, fixed preferred size, graph navigation, control packets and progress model. New projects use their own description/knowledge/stage-task tabs in the larger journal-sized area.

`DrawDeskScreen` keeps independent drawing-desk and research archive CUI layers, plus the lazily
created [knowledge journal layer](../knowledge/workbench.md). In drawing-desk mode, its root remains `387 x 203`;
`DrawDeskLayer` owns the drawing game, help, progress summary, item examination action, and inventory slots. In archive
mode, `DrawDeskScreen#resizeArchiveToWindow` expands the root to the scaled window minus a `12` pixel margin and hides
container slots without moving them.

`ResearchArchiveLayer` is the active player-facing archive. It contains:

- `ResearchFieldTabBar`, which reuses the original technology tree's five icon tabs plus an all-fields tab in the same `31` pixel top header as the archive title and drawing-desk action;
- a search box directly above the project index on the left;
- `ResearchGraphViewport`, which continuously zooms the same dependency graph while retaining a separate real-icon frame and paper nameplate for every project, with panning, smooth wheel zoom, fit, and selected-node focus;
- `ResearchProjectSummaryPanel`, a deliberately concise right-hand summary with name, category, status, progress, and the first description paragraph;
- `ResearchProjectWorkspace`, a centered small dialog opened by clicking a revealed graph node or the summary action. `DETAIL` owns descriptions, materials, all real clue rows including `MinigameClue`, effects, and existing research actions; `THEORY` retains a dedicated view of the same `MinigameClue` rows; `EXPERIMENT` is an empty integration boundary reserved for the future town system.

Clicking a row in the left index changes selection and the concise summary only. Clicking a graph node opens the centered project dialog. Clicking a visible name beside a node targets that same project. Minimum zoom uses this same interaction. Closing the dialog does not rebuild the graph or reset its camera. The older `ResearchLayer` and its panels remain in source as compatibility fallback classes but are no longer mounted by `DrawDeskScreen`. A standalone read-only `BROWSE` screen is not implemented yet.

The left index applies archive privacy, research-field, and localized search filters before passing the remaining definitions to the original `FHResearch#getResearchesForRender(Iterable, boolean)` client ordering. That ordering places completed projects with unclaimed effects first, followed by incomplete unlocked projects, revealed-but-locked projects, and other completed projects; editor mode appends hidden or otherwise undiscovered definitions. Definition iteration order is retained inside each group except for the legacy front insertion of unclaimed projects. Active research and bookmarks remain visible row markers but do not change ordering. This list ordering is independent of `ResearchGraphLayoutEngine` and never changes graph coordinates.

At archive widths of at least `620`, the index/summary widths are `142/176`; below that they are `92/112`, and below `340` they are `72/84`. The graph consumes the remaining center width. The project dialog follows the original `ResearchDetailPanel` and `TechIcons.DIALOG` size of `302 x 170`, shrinking only when the archive cannot provide an `8` pixel safe margin. Its `z=600` render transform keeps graph items below the dialog, and its dimming mask uses the archive's actual render origin and full dimensions.

Graph zoom is clamped to `0.15-1.75`. `fitToVisible` measures the projected world bounds, computes the largest scale that fits both axes inside a `24` GUI-pixel viewport margin, and clamps it to the same zoom limits. The focus button centers the selected project and raises zoom to at least `1.0`; with no selection it falls back to fitting the graph.

### Readable Node Presentation

`ResearchGraphViewport` renders one dependency graph throughout the `0.15-1.75` zoom range. There are no `CARD/ICON/MARKER` or field-overview modes, and zoom never replaces projects with monochrome markers or another screen layout. Node world centers and dependency topology remain stable. Sizes below are source defaults in GUI pixels before Minecraft's GUI scale:

- Every visible project submits its actual `Research#getIcon()` at every zoom. Icon size is `clamp(round(LANE_SPACING * z - 4), MIN_ICON_SIZE, ICON_SIZE)`, where `LANE_SPACING = 72` world units, `z` is camera zoom, `MIN_ICON_SIZE = 8`, and `ICON_SIZE = 16`. Thus `15%` retains 8-pixel icons and ordinary browsing uses 16-pixel icons. The icon floor preserves recognizable colors/shapes without making adjacent low-zoom lanes overlap.
- Every node reserves space for a square icon mount and a separate paper nameplate beside it. Let `s = smoothStep((z - 0.24) / 0.46)` and `tile = iconSize + 2`, where `smoothStep(t) = u²(3 - 2u)` and `u = clamp(t, 0, 1)`. The compact node width is `max(tile, floor(RANK_SPACING * z) - 1)`, with `RANK_SPACING = 208` world units. The rank spacing was widened from 176 to accommodate native-size text at minimum zoom; it is a definition-time layout constant and does not change during dragging. Node width interpolates from this compact width to the rounded projection of `NODE_WIDTH = 144`. This uses spare column space for names at small zoom and leaves at least a one-pixel horizontal dependency corridor. Node height interpolates from `tile` to the rounded projection of `NODE_HEIGHT = 52`, with `tile` as its minimum. These bounds own layout, edges and input; no large background fills the space between the two frames.
- The icon mount is a square of side `min(nodeHeight, iconSize + 2 + round(6 * s))`. The nameplate starts at `nodeX + iconMountSide - 1`, sharing one border column with its icon mount at every zoom. This joins the two frames visually and gives their former gap to the name's text budget. Both frames retain one-pixel borders and cut corners. The icon mount uses darker paper; the nameplate uses the archive's ordinary paper. Both use the project's state/hover/selection border color. The nameplate is vertically centered and only as tall as its one or two text lines plus the available inset.
- Text uses `TITLE_STYLE = Style.EMPTY`, Minecraft's normal `minecraft:default` font, the existing unscaled screen pose, and integer GUI-pixel origins. The label is the same native size as other interface text: `TITLE_TEXT_HEIGHT = 8` pixels with `TITLE_LINE_HEIGHT = 9` between line origins. Camera zoom does not resize or rerasterize letters. At minimum zoom the 19-pixel text budget holds two ordinary 9-pixel CJK advances. Resource packs and native language fallback continue through Minecraft's ordinary font machinery.
- Nameplate width is `nodeWidth - iconMountSide + 1`. The cached text budget is `nameplateWidth - 2 * textInset`, where `textInset = 1 + round(3 * s)`. Measurement, clipping and drawing use the same `TITLE_STYLE`. Every project draws its own short name in that space. Two lines require 17 text pixels inside the node's one-pixel border; English prefers word boundaries, while CJK and long single words use glyph boundaries. Increasing zoom gives the same-size letters more room, so names become longer without a font-size switch.
- Very small text slots retain the longest fitting original prefix. They do not spend their few character positions on an ellipsis. On the final line, a text budget of at least `MIN_ELLIPSIS_WIDTH = 48` GUI pixels uses `…` when truncation is necessary. Tooltips and the selected-project summary retain the full name. Partially offscreen nodes use the viewport's ordinary scissor clipping. At GUI scale 1 the smallest labels still have very few physical pixels; full-title inspection remains available through the same tooltip and summary.

The two frames form one project node. Names have a reserved slot rather than competing as movable labels. The former occupancy grid, width-variant arrays, priority passes, distant-label fallback and separate label hit rectangles are removed. The existing rank/lane layout separates nodes, and names stay inside their own nameplate bounds. Hover changes colors/edge emphasis and exposes the full tooltip without rebuilding or moving names.

Labels use Chorda's existing `UIElement#getFont` -> `PrimaryLayer#getFont` -> `Minecraft.font` path. The former compact Noto bitmap, character map, license notice and offline generator have been removed. Their 346,849 bytes of added resources and 3,655,680-byte decoded source image are no longer needed. Titles share Minecraft's existing default font/atlases with the rest of the interface; used glyphs can still add ordinary atlas/cache entries. The separate [scenario font renderer](../scenario-font-rendering.md) is not involved.

At a fixed zoom, `ResearchGraphViewport` computes one integer translation: `renderOriginX = round(viewportX + width / 2 - cameraX * z)`, similarly for Y. `projectX(worldX) = renderOriginX + round(worldX * z)` and `projectY` are shared by nodes and dependency endpoints. The grid uses the same origin. Thus every already visible node changes by the exact same integer displacement during panning; separate node rounding no longer changes adjacent gaps by a pixel. Edge bends use `x1 + floorDiv(x2 - x1, 2)` so crossing the viewport origin does not introduce another midpoint rounding jump. Drawing and hit testing consume the same planned coordinates. Camera/world conversion stays continuous for pointer-anchored zoom, with ordinary subpixel-to-pixel display quantization.

Wheel input creates a single `ZoomMotion`, eased over `ZOOM_DURATION_NANOS = 150_000_000` (150 milliseconds). Zoom interpolates logarithmically with smoothstep easing. Each animation frame derives camera position from the same world point and local pointer anchor, so that world point stays under the pointer throughout the transition. Repeated wheel input accumulates the target zoom; animation stops on settlement. Clicking, dragging, resizing, changing definitions or changing field cancels pending motion. Fit and selected-project focus remain explicit camera operations. Static frames do not resample an animation or rebuild geometry/labels once motion ends.

The selected, hovered, completed, active and contextual node colors remain research-state presentation. Actual dependency edges attach to the current continuously sized card bounds, retain a one-pixel width and highlight connections to selection/hover. A minimum 12-pixel hit area and nearest-center resolution keep small items usable; the name column is part of that same clickable project card. Drawing and hit testing use the same screen-adapter origin, including window centering. The independent knowledge journal's `KnowledgeGraphViewport` uses its own presentation.

`Research#getName` resolves configured or translated names through `FRTextUtil` and `StringTextComponentParser`. `ComponentOptimizer#components` flushes its pending final text run before returning the component list, and `StringTextComponentParser#parse(Component)` advances its reader through each line before parsing. Both steps are required to retain translated text. The real-client route below checks production translations and nonempty visible project names.

While the archive is active, `DrawDeskScreen` temporarily makes native `AbstractWidget` children inactive and invisible,
suppressing their normal input. FTB's `SidebarGroupGuiButton` overrides the native render path and ignores
`visible/active`, so an optional `@Pseudo` client mixin cancels that render override while the archive or knowledge
journal is open. No FTB group collection is removed, cached, or restored; closing the archive naturally reveals FTB's
current state, including changes made during a resource reload.

## Progress And Completion

`Research.points` is the required experiment-point total. The Java field initializer is `1000`, but `Research.CODEC` makes the JSON `points` field mandatory; omitting it does not apply a codec default. No positive-range validation is performed. `ResearchData.committed` stores directly submitted experiment points.

For a research with required points `P`, direct committed points `D`, and triggered clue contributions `c_i`, `ResearchData#getTotalCommitted` computes effective experiment points as:

```text
if sum(c_i) >= 0.999:
    effective = P
else:
    effective = min(D + floor(sum(c_i) * P), P)
```

`c_i` is a dimensionless fraction from `Clue.value`. Experiment points are integer points. `ResearchData#getProgress` returns `effective / P` as a fraction.

Completion is server-owned by `TeamResearchData#checkResearchComplete`. It requires both:

1. `getTotalCommitted(research) >= research.getRequiredPoints()`;
2. every clue with `Clue.required == true` is triggered in `ResearchData.clueData`.

`ResearchData#canComplete` expresses the second condition only. Item submission, theory-game settlement, kill/advancement/tick/custom triggers, and effect grants continue through existing hooks and packets.

## Clue Presentation

`ResearchClueViewFactory` creates immutable presentation rows from `Research#getClues()` plus synchronized `ResearchData`. Rows retain nonce, text, `required`, completion, contribution, and original definition order. UI sorting is:

1. unfinished required clues;
2. other unfinished clues;
3. completed clues.

Sorting never mutates `Research#getClues()`, clue nonces, or `ClueData`.

The factory also adds `$experiment_points`, a read-only system row backed by `getTotalCommitted/getRequiredPoints`, without adding a real `Clue`, nonce persistence, codec field, or second completion rule. The current project dialog presents those values in the `DETAIL` progress header and excludes the synthetic row from the clickable clue list.

`CluePresentationClassifier` maps `MinigameClue` to `THEORY` and other current clue types to `DETAIL`. The detail view includes both classifications so it remains a complete clue checklist, while the theory view filters to `MinigameClue` only. `EXPERIMENT` intentionally receives no materials or clue rows. `ClueDestinationResolver` only selects a client navigation target:

| Clue | Drawing-desk context | Read-only context |
|---|---|---|
| `ItemClue` | item examination area | drawing desk required |
| current eligible `MinigameClue` | theory game | drawing desk required |
| kill/advancement/tick clue | return to world | return to world |
| unknown `CustomClue` | details only | details only |

A destination never submits an item, starts or settles the theory game, grants points, completes a clue, or sends a research action packet.

## Archive State And Navigation

`ResearchWorkspaceState` is client-only and is not serialized into team data. It retains surface, normalized research type, search text, selected research/clue, project tab, bookmarks, list expansion/scroll, per-type camera/zoom, and a one-shot drawing-desk focus target. Zoom is clamped to `0.15-1.75`.

`StatefulResearchNavigationController` implements the current route contract without depending on `FRNetwork`:

```text
project workspace -> full archive -> drawing desk -> close
```

The drawing-desk step is omitted for `BROWSE` context. Definition reconciliation keeps valid cameras and list scroll, removes invalid bookmarks, and falls back from a removed selection to the active or first visible research.

## Graph Foundation

`ResearchGraphSnapshot#fromResearches` copies IDs, normalized categories, parent IDs, derived child IDs, effective visibility state, and display-only layout hints into stable ID order. `ResearchGraphProjection#forResearchType` excludes hidden nodes and edges in normal mode, keeps current-type nodes as primary, and retains visible ancestors as low-priority prerequisite context. Editor snapshots explicitly reveal hidden definitions so projection, layout, node rendering, and edge rendering include the complete graph. Projection never changes coordinates.

`ResearchGraphLayoutEngine` is a deterministic definition-time layout pass:

1. skip missing-parent edges and emit `MISSING_PARENT` diagnostics;
2. collapse cycles with Tarjan SCC and emit `CYCLE` diagnostics;
3. assign left-to-right rank by longest path in the condensed DAG;
4. establish stable category/ID lane order and run four barycentric sweeps;
5. preserve manual anchors and move only automatic nodes out of occupied bounds;
6. report overlapping manual anchors without moving either anchor;
7. calculate padded world bounds for future fit and viewport clipping.

`ResearchArchiveLayer` filters definitions before archive components receive them. Outside editor mode, a research must be non-hidden and `isShowable()`, `isUnlocked()`, or `isCompleted()`; otherwise it is absent from the graph, index, selection reconciliation, search, tooltip, summary, and dialog. The player archive never creates anonymous unknown-project placeholders and never exposes internal research IDs in tooltips.

Editor mode passes all definitions into the archive's index, summary, dialog, graph snapshot, projection, layout, and render paths. Hidden research and its incident edges are visible only there. Normal mode continues to exclude hidden definitions before graph, index, search, tooltip, selection, and detail construction, so synchronized hidden IDs are not exposed through the player UI.

`ResearchGraphViewport extends PanZoomViewport` renders the filtered snapshot and projection directly. The Chorda CUI base owns the reusable two-dimensional camera, left/middle-button panning, pointer-anchored wheel zoom, fit/center operations, world/screen conversion, rectangular scissoring, visibility checks, and allocation-free axis-aligned segment clipping. The archive keeps only research-specific projection, layout, node virtualization and hit testing, status presentation, and per-research-type camera persistence. Search highlights matching visible nodes by research ID or cached localized name and dims other nodes without changing coordinates. Search and progress changes update node styles without rebuilding the category projection or graph layout; research-type changes rebuild the projection while retaining saved cameras.

Current research definitions do not yet expose `display.layout`; `ResearchGraphSnapshot#fromResearches` therefore supplies `AUTO`. The model already accepts manual hints so a future optional codec field does not require a layout-engine rewrite.

## Rendering And Derived-State Performance

The archive deliberately reuses CUI where it removes shared control or rendering work without turning every domain object into a child widget:

- `PanZoomViewport` is the shared CUI `UILayer` for map-style virtual canvases. It avoids the general `UILayer` stencil pass, scissors only virtual world content, leaves ordinary CUI children available for overlay controls, and exposes a camera-constraint hook for bounded maps.
- `ResearchGraphViewport` subclasses that shared viewport; its fit and selected-node focus tools are CUI `Button` controls with framework-owned hit testing, tooltips, cursor state, and click feedback.
- The graph's background, cached grid, cached clipped orthogonal edges, visible project-card backgrounds are submitted through one `TesselateHelper.ShapeTesslator` buffer. Icons and names render in later passes because textured drawing cannot occur while the shared shape buffer is open.
- Projection rebuilds resolve direct `Research`, layout-position, and context references. Camera, viewport, projection, or language changes rebuild a screen-space render plan containing visible node geometry, clipped edge rectangles and icon/nameplate bounds. `RenderPlanKey` identifies this reusable geometry plan. Text wrapping has its own per-source cache, so moving an existing nameplate does not measure its text again. `NodeStyleKey` separately identifies selection, hover, search and synchronized-state styling. Those changes update node colors and edge highlights without rebuilding geometry, title wrapping or projection.
- `ResearchArchiveViewCache` resolves localized names, category labels, and lowercase search text on definition/language revision, and snapshots completed/active/unlocked/progress values on the existing progress, active-research, and clue-progress notifications. Each `View` keeps those concerns in separate `LocalizedPresentation` and `SynchronizedState` groups while retaining the narrow accessors consumed by widgets. It remains a client-only archive derivative; `TeamResearchData` is still authoritative.
- `CIconBatch` is a reusable Chorda icon pass. Its default mode preserves strict submission order. The graph opts into `LAYER_THEN_LIGHTING` because submitted project icon rectangles do not overlap: base flat/block items are submitted first, followed by overlay flat/block items. Exact flush counts also depend on textures and custom-icon barriers. Animated and combined icons delegate into the batch. Texture icons are delayed by z layer and grouped by contiguous texture inside this opt-in mode; text and unknown custom icons remain explicit immediate-render barriers. Item models are still resolved each pass so dynamic models and resource reloads stay correct.
- Titles use the existing screen matrix directly, without fractional font scaling. Each `NodeRenderSource` owns one mutable `NodeTitleLayout`, keyed by available text width, whether two lines fit, and `ResearchArchiveViewCache#presentationRevision`. It retains only the latest one/two-line strings and styled `FormattedCharSequence` instances. Panning, window movement and zoom frames with the same text constraints reuse that layout; a newly exposed source is measured only if its cached inputs differ. Geometry still updates integer font coordinates and frame bounds. Projection rebuilds replace the source list and its caches; no map or collection of zoom variants is kept. `titlePlanBuildCountForTest` counts actual per-node title layout misses, rather than geometry passes. Clipping measures only the fitting prefix and, when needed, the shorter final line/ellipsis budget; it no longer scans the complete title first just to check overflow. Empty second lines use `FormattedCharSequence.EMPTY`. Static frames and hover reuse geometry and typography. Minecraft caches native font glyphs in its standard font atlases. All visible nodes retain a real 8-16 pixel icon and a nonempty name prefix. There are no per-project CUI widgets, force simulation, per-title texture cache or new render target.
- `ResearchTypeListPanel` remains a virtual row renderer because CUI traverses every child each frame. Its `VisibleResearchCacheKey` identifies the filtered/legacy-ordered definition list, which is invalidated by definition, type/search, presentation, or research-progress changes; active-research and bookmark changes only update row styling. Rendering calculates the first and last visible row directly, lowercases search text only when the source query changes, and uses a separate `RowTitleCacheKey` for bookmarked and ordinary clipped titles.
- `ResearchProjectSummaryPanel` uses `SummaryCacheKey` to cache its truncated title/category, status, first-description wrapping, and action label/position by selection, width, presentation revision, and state revision. A separate text-wrap key also caches the empty-selection state, including the valid cached-null summary.
- `ResearchProjectWorkspace` caches one display-ready `WorkspaceRenderSnapshot`. The snapshot groups header, progress, tabs, detail, theory, experiment, and footer data instead of exposing one flat multi-purpose record; `WorkspaceRenderKey` controls reuse. Definition, selected research, progress, active-research, clue-progress, language, and resize changes invalidate that snapshot.
- Invisible CUI layers and elements are skipped by descendant render-info and mouse-over traversal. `setVisible(false)` clears the element hover state; making the element visible before the next update restores coordinates and hover in that same update. Tick semantics are unchanged.
- `DrawDeskScreen` still polls for delayed optional FTB widgets while the archive is open, but archive layout runs only when the scaled window dimensions or project-dialog visibility change, and already hidden native widget state is not rewritten.
- `ResearchHooks#tick` returns before resolving team research data when the loaded catalogue has no tick clues.

The graph exposes Minecraft profiler sections `frostedresearch_graph_shapes`, `frostedresearch_graph_item_icons`, and `frostedresearch_graph_labels`. These sections are measurement anchors, not automatic performance claims.

Graph layout, projection, project-card hit testing, continuous node presentation, and virtual list rows remain archive-specific code. Converting all research nodes, map markers, or rows into CUI children would add full child traversal and transform work each frame without replacing the owning domain logic.

## Refresh Contract

`ResearchGui` distinguishes definition, research-progress, active-research, and clue-progress notifications. Definition-sync completion and the incremental research, active, clue, and effect handlers emit narrow notifications after applying synchronized data. `DrawDeskScreen` forwards these notifications to the mounted archive.

`ResearchArchiveLayer#onResearchDefinitionsChanged` always rebuilds the graph snapshot and layout. While an archive remains open, `ResearchArchiveLayer#refreshEditorModeIfNeeded` also treats an `FHResearch.editor` transition as a definition change, so privacy filtering, selection reconciliation, the left index, graph projection/layout, summary, and project dialog move to the same mode in one rebuild. Research- and clue-progress notifications first compare the player-visible definition ID set; they rebuild only when discovery or completion changes that set. Other progress, active-research, and clue notifications are read by the affected index, node, summary, or dialog presentation without resetting selection, camera, zoom, filter, bookmarks, or list scroll.

Full-state `FHResearchDataSyncPacket` replacement calls the dedicated `ResearchUtils#notifyResearchDataReplaced` hook after rebuilding client derived state. The open archive rebuilds visible definitions, graph/layout, and all state-derived presentation caches in one refresh. A still-valid research type, filter, search, and camera remain; an invalid selection is cleared/reconciled.

`ResearchArchiveLayer.ArchiveLayoutKey` is the only archive-layout cache. It includes scaled width, scaled height, and project-dialog visibility; `DrawDeskScreen#resizeArchiveToWindow` always delegates to the layer so opening or closing the dialog cannot be hidden by a width/height-only parent cache. An empty or fully filtered catalogue is a valid state: nullable selection is checked before querying immutable definition maps, and the archive renders its empty-selection presentation instead of failing construction.

## Drawing-Desk Routing And Actions

`StatefulResearchNavigationController#goToDrawingDesk` only changes client surface state and records a one-shot `DrawDeskFocusTarget`. `DrawDeskLayer#focusTarget` briefly frames the item-examination controls or theory game; it does not click a control, move an item, start a paper, or send a packet. World destinations close the menu and leave completion to existing world hooks.

The project dialog is the only new component that sends research action packets, and it reuses the existing calls:

| Dialog action | Existing packet |
|---|---|
| commit material and start | `FHResearchControlPacket(COMMIT_ITEM, research)` |
| start | `FHResearchControlPacket(START, research)` |
| pause | `FHResearchControlPacket(PAUSE, research)` |
| claim effects | `FHEffectTriggerPacket(research)` |

The back order in drawing-desk context is project dialog, archive, drawing desk, then close. `Esc` and mouse button 4 use the same state machine. The drawing-desk and archive layer instances remain mounted while visibility and input are switched, preserving both sides' client UI state.

## Validation

The former JUnit suite under `src/test/java/com/teammoeg/frostedresearch` and `src/test/java/com/teammoeg/chorda` covered the following contracts. Those sources were removed on 2026-10-19 at the user's request; this list is no longer evidence of a runnable suite:

- unchanged `ResearchData#getTotalCommitted` and required-clue behavior;
- per-type state retention and navigation back order;
- clue sorting, system point row, tab classification, and read-only routing;
- normal/editor definition visibility and empty experiment-tab presentation;
- deterministic layout under reversed registry order;
- multi-node/self cycles, missing parents, manual-anchor preservation/conflicts;
- hidden-node privacy, category alias normalization, and prerequisite context projection;
- project-list cache reuse and invalidation;
- search updates not rebuilding graph projection;
- ordered item-lighting segment transitions and reuse reset;
- hidden CUI subtree frame-propagation short-circuit and same-frame restoration;
- progress-driven legacy project-list reordering without changing graph layout;
- shared camera sanitization, pointer-anchored zoom, left/middle-button panning, bounds fitting, coordinate conversion, visibility checks, and horizontal/vertical edge clipping.

`./gradlew.bat validateResearchCatalog runResearchUiClient --no-daemon --console=plain` runs the opt-in `ResearchGraphClientValidation` in an actual Minecraft client. It creates a fresh flat world, opens a real drawing desk through normal block interaction, and renders the production catalogue using the editor's full graph and the ordinary player visibility mode. It checks Chinese/English presentation, `15%`, `45%`, `100%`, `175%`, GUI scales `1/2/3`, a `320 x 240` GUI window, static geometry/title cache reuse, a real icon and name for every project at every tested zoom, joined icon/name frames, native font metrics, integer text origins, one/two-line names and ordinary graph interactions. Each display scenario also performs twenty rendered fractional-delta middle-button drag frames. Every common node, icon and label must move by the exact same displacement, without changes to size or text. At GUI scales 1/2, the selected title's actual framebuffer pixels must remain identical after accounting for its movement. `stablePanFrames` and `comparedTextPixels` record that verification; GUI scale 3 checks geometry as well. Repeated real dragging through two rendered views must rebuild geometry while adding zero title layouts once warm; `repeatedPanTitleRebuilds` records that result. Every inspected name must match its current localized source. Additional `29%/30%` and `37%/38%` captures exercise former display-switch boundaries. Results and framebuffer PNGs are written under `build/research-ui-validation/`; `result.json` records the outcome. This route uses the game-test source set but performs real client rendering and screen input. It covers the default client resource configuration; alternate font packs, shaders or display-motion effects require their own visual check.

Runtime FPS, allocation, and draw-call performance claims still require in-game profiling. Static geometry/title cache reuse and item-batch flush counts are directly observed by the client route above; they are not FPS measurements.

Static observation windows restart if the render key or projection inputs actually change during observation (for example, an asynchronous definition sync or resize). With unchanged inputs the driver still requires zero geometry/title rebuilds. The final 2026-11-05 run needed no restarts.

The client route also moves the camera four rank columns away, requires previously offscreen projects to enter view, and checks that returning restores the same project IDs without redoing their title layouts. A normal `Minecraft#reloadResourcePacks` call with the selected language unchanged must publish a new `Language` instance, advance `ResearchArchiveViewCache#presentationRevision`, refresh visible title layouts and preserve the camera. `farPanNewNodes`, `farPanReturnTitleRebuilds`, `sameLanguageReloadPassed` and `resourceReloadTitleRebuilds` record those observations.

## Legacy replacement entries (verified 2026-11-12)

An entry covered by an active `ProjectDefinition.legacy_replacement` is marked ↗ in the archive list. Its summary and
workspace explain the replacement, completion credit or carried-forward idea, and route to the Knowledge page.
The workspace offers no old activation, points or clue actions. Undiscovered new projects show discovery guidance;
the old entry cannot disclose their hidden requirements. Every knowledge snapshot refreshes the archive's prerequisite
and workspace caches using the server's `legacy_replacements.effective_completed` projection. See the owning
[workbench behavior](../knowledge/workbench.md#旧研究接替入口m3-b) and [state contract](../knowledge/state-and-api.md#旧研究承接与完成等价查询m3-b).
