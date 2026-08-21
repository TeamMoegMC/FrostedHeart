# Research Progress And UI

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: Implemented research completion semantics, responsive drawing-desk archive, full graph, project dialog, clue routing, and refresh boundaries
- Code anchors: [`Research`](../../src/main/java/com/teammoeg/frostedresearch/research/Research.java), [`ResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/ResearchData.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`DrawDeskScreen`](../../src/main/java/com/teammoeg/frostedresearch/gui/drawdesk/DrawDeskScreen.java), [`ResearchArchiveLayer`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchArchiveLayer.java), [`ResearchGraphViewport`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchGraphViewport.java), [`ResearchProjectWorkspace`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchProjectWorkspace.java), [`ResearchGraphLayoutEngine`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/graph/ResearchGraphLayoutEngine.java), `PanZoomViewport`,

## Current Player Surface

`DrawDeskScreen` keeps two mounted CUI layers. In drawing-desk mode, its root remains `387 x 203`; `DrawDeskLayer` owns the drawing game, help, progress summary, item examination action, and inventory slots. In archive mode, `DrawDeskScreen#resizeArchiveToWindow` expands the root to the scaled window minus a `12` pixel margin and hides container slots without moving them.

`ResearchArchiveLayer` is the active player-facing archive. It contains:

- `ResearchFieldTabBar`, which reuses the original technology tree's five icon tabs plus an all-fields tab in the same `31` pixel top header as the archive title and drawing-desk action;
- a search box directly above the project index on the left;
- `ResearchGraphViewport`, which displays the complete projected dependency graph and supports empty-space or middle-button panning, pointer-anchored wheel zoom, fit, and selected-node focus;
- `ResearchProjectSummaryPanel`, a deliberately concise right-hand summary with name, category, status, progress, and the first description paragraph;
- `ResearchProjectWorkspace`, a centered small dialog opened by clicking a revealed graph node or the summary action. `DETAIL` owns descriptions, materials, all real clue rows including `MinigameClue`, effects, and existing research actions; `THEORY` retains a dedicated view of the same `MinigameClue` rows; `EXPERIMENT` is an empty integration boundary reserved for the future town system.

Clicking a row in the left index changes selection and the concise summary only. Clicking a graph node opens the centered project dialog. Closing the dialog does not rebuild the graph or reset its camera. The older `ResearchLayer` and its panels remain in source as compatibility fallback classes but are no longer mounted by `DrawDeskScreen`. A standalone read-only `BROWSE` screen is not implemented yet.

At archive widths of at least `620`, the index/summary widths are `142/176`; below that they are `92/112`, and below `340` they are `72/84`. The graph consumes the remaining center width. The project dialog follows the original `ResearchDetailPanel` and `TechIcons.DIALOG` size of `302 x 170`, shrinking only when the archive cannot provide an `8` pixel safe margin. Its `z=600` render transform keeps graph items below the dialog, and its dimming mask uses the archive's actual render origin and full dimensions.

Graph zoom is clamped to `0.15-1.75`. Node bounds scale directly with zoom instead of retaining a `54 x 24` pixel floor. `fitToVisible` centers the complete projected graph and sets zoom to the same `15%` lower bound. Node content is never disabled by a zoom threshold: research icons retain a `4` pixel minimum and names retain a `0.25` text-scale minimum.

While the archive is active, `DrawDeskScreen` temporarily hides native `AbstractWidget` children injected into `CUIMenuScreenWrapper`. FTB's `SidebarGroupGuiButton` overrides the native render path and ignores `visible/active`, so the archive also uses reflection to move groups out of `SidebarButtonManager` and attempts to restore a saved set when returning to the drawing desk or closing the screen. Repeated archive ticks catch delayed widget injection. This is best-effort compatibility: if FTB repopulates groups while the archive is open, the current implementation can overwrite its saved set, so resource-reload restoration still needs in-game verification.

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

`ResearchGraphSnapshot#fromResearches` copies IDs, normalized categories, parent IDs, derived child IDs, hidden state, and display-only layout hints into stable ID order. `ResearchGraphProjection#forResearchType` excludes `hidden` nodes and edges, keeps current-type nodes as primary, and retains visible ancestors as low-priority prerequisite context. Projection never changes coordinates.

`ResearchGraphLayoutEngine` is a deterministic definition-time layout pass:

1. skip missing-parent edges and emit `MISSING_PARENT` diagnostics;
2. collapse cycles with Tarjan SCC and emit `CYCLE` diagnostics;
3. assign left-to-right rank by longest path in the condensed DAG;
4. establish stable category/ID lane order and run four barycentric sweeps;
5. preserve manual anchors and move only automatic nodes out of occupied bounds;
6. report overlapping manual anchors without moving either anchor;
7. calculate padded world bounds for future fit and viewport clipping.

`ResearchArchiveLayer` filters definitions before archive components receive them. Outside editor mode, a research must be non-hidden and `isShowable()`, `isUnlocked()`, or `isCompleted()`; otherwise it is absent from the graph, index, selection reconciliation, search, tooltip, summary, and dialog. The player archive never creates anonymous unknown-project placeholders and never exposes internal research IDs in tooltips.

Editor mode passes all definitions into the archive's index/summary/dialog path, including hidden definitions. It does **not** expose hidden nodes in the graph: `ResearchGraphSnapshot` preserves `hidden=true`, and both projection/layout exclude those nodes. “Editor access to all definitions” therefore applies to list/detail navigation, not the visual graph.

`ResearchGraphViewport extends PanZoomViewport` renders the filtered snapshot and projection directly. The Chorda CUI base owns the reusable two-dimensional camera, left/middle-button panning, pointer-anchored wheel zoom, fit/center operations, world/screen conversion, rectangular scissoring, visibility checks, and allocation-free axis-aligned segment clipping. The archive keeps only research-specific projection, layout, node virtualization and hit testing, status presentation, and per-research-type camera persistence. Search highlights matching visible nodes by research ID or localized name and dims other nodes without changing coordinates. Search changes rebuild only a cached ID match set; research-type changes rebuild the category projection.

Current research definitions do not yet expose `display.layout`; `ResearchGraphSnapshot#fromResearches` therefore supplies `AUTO`. The model already accepts manual hints so a future optional codec field does not require a layout-engine rewrite.

## Rendering And Derived-State Performance

The archive deliberately reuses CUI where it removes shared control or rendering work without turning every domain object into a child widget:

- `PanZoomViewport` is the shared CUI `UILayer` for map-style virtual canvases. It avoids the general `UILayer` stencil pass, scissors only virtual world content, leaves ordinary CUI children available for overlay controls, and exposes a camera-constraint hook for bounded maps.
- `ResearchGraphViewport` subclasses that shared viewport; its fit and selected-node focus tools are CUI `Button` controls with framework-owned hit testing, tooltips, cursor state, and click feedback.
- The graph's background, grid, clipped orthogonal edges, and visible node backgrounds are submitted through one `TesselateHelper.ShapeTesslator` buffer. Icons and names render in a second pass because textured drawing cannot occur while the shared shape buffer is open.
- Horizontal and vertical edge segments are clamped to the viewport before vertices are appended. Nodes outside the viewport are excluded from both shape and content passes.
- `ResearchTypeListPanel` remains a virtual row renderer because CUI traverses every child each frame. Its filtered/sorted definition list is cached by definition revision, type, search query, active research, and bookmarks, and its hovered row is calculated once per render.
- `ResearchProjectWorkspace` caches clue presentation rows, detail/theory filtering, wrapped description lines, and content-height metrics for the selected research and dialog width. Definition, selected research, progress, active-research, clue-progress, and resize changes invalidate that presentation.
- `DrawDeskScreen` still polls for delayed optional FTB widgets while the archive is open, but archive layout runs only when the scaled window dimensions or project-dialog visibility change, and already hidden native widget state is not rewritten.

Graph layout, projection, node hit testing, low-zoom icon/text presentation, and virtual list rows remain archive-specific code. Converting all research nodes, map markers, or rows into CUI children would add full child traversal and transform work each frame without replacing the owning domain logic.

## Refresh Contract

`ResearchGui` distinguishes definition, research-progress, active-research, and clue-progress notifications. Definition-sync completion and the incremental research, active, clue, and effect handlers emit narrow notifications after applying synchronized data. `DrawDeskScreen` forwards these notifications to the mounted archive.

`ResearchArchiveLayer#onResearchDefinitionsChanged` always rebuilds the graph snapshot and layout. Research- and clue-progress notifications first compare the player-visible definition ID set; they rebuild only when discovery or completion changes that set. Other progress, active-research, and clue notifications are read by the affected index, node, summary, or dialog presentation without resetting selection, camera, zoom, filter, bookmarks, or list scroll.

The full-state `FHResearchDataSyncPacket` is an exception: it replaces client `TeamResearchData` and rebuilds derived state/JEI, but does not call any `ResearchUtils.notify...` archive hook. After a team change or `/reload` while the archive is open, live-read values may update but discovery-dependent definition sets, layout, and selection reconciliation can remain stale until another notification or screen reopen. This notification gap is source-confirmed; its exact visual impact still requires an in-game reproduction.

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

Tests under `src/test/java/com/teammoeg/frostedresearch` and `src/test/java/com/teammoeg/chorda` cover:

- unchanged `ResearchData#getTotalCommitted` and required-clue behavior;
- per-type state retention and navigation back order;
- clue sorting, system point row, tab classification, and read-only routing;
- normal/editor definition visibility and empty experiment-tab presentation;
- deterministic layout under reversed registry order;
- multi-node/self cycles, missing parents, manual-anchor preservation/conflicts;
- hidden-node privacy, category alias normalization, and prerequisite context projection;
- project-list cache reuse and invalidation;
- search updates not rebuilding graph projection;
- shared camera sanitization, pointer-anchored zoom, left/middle-button panning, bounds fitting, coordinate conversion, visibility checks, and horizontal/vertical edge clipping.

The CUI rendering and responsive geometry have compile and state-model coverage. Runtime FPS, allocation, and draw-call improvements still require in-game JFR or Spark profiling plus visual QA across GUI scales.
