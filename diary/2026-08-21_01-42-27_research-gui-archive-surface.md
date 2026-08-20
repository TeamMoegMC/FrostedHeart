# Research GUI Archive Surface

- Time: `2026-08-21 01:42:27 +08:00`
- Author: `Codex (OpenAI; implementation and validation)`
- Status: `completed`
- Scope: `ResearchArchiveLayer`, drawing-desk/archive routing, full graph viewport, project summary/dialog, clue routing, localization, and research documentation

## Completed

- Mounted a responsive `ResearchArchiveLayer` in `DrawDeskScreen` while retaining the existing `DrawDeskLayer` instance and fixed slot coordinates.
- Added a searchable type/project index, per-type camera restoration, full dependency graph rendering, blank/middle-button panning, pointer-anchored zoom, fit, focus, selection, bookmarks, and hidden-safe unknown nodes.
- Added the requested concise right-hand project summary. Graph-node clicks or the summary action open a centered small project-file dialog with detail, theory, and experiment tabs.
- Reused `ResearchClueViewFactory` for clue rows and existing packets for commit/start/pause/claim actions. Clue destinations only route to the drawing desk or world.
- Added one-shot item-examination and theory-game focus frames, plus project -> archive -> drawing desk -> close handling for `Esc` and mouse-back.
- Added Chinese and English archive strings and updated the living research documentation and active implementation plan.

## Decisions

- Kept the right side concise; full research information and actions live in a centered modal dialog instead of a large persistent right workspace.
- Kept `ResearchLayer` in source as an unmounted fallback. No research, clue, team-data, or packet codec was changed.
- Excluded `hidden` research from graph projection, index, selection reconciliation, search, tooltip, summary, and dialog. Non-showable research remains anonymous.
- Rebuild graph layout only on definition notifications; progress, active-research, and clue notifications preserve camera, filter, selection, bookmarks, and list scroll.

## Validation

- `./gradlew compileJava` passed; only existing repository deprecation warnings were reported.
- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed: 15 tests.
- `./gradlew test` passed for the full repository suite; existing duplicate-resource and deprecation warnings remain.
- Both modified language JSON files parsed successfully, and `git diff --check` reported no whitespace errors beyond repository line-ending notices.

## Remaining

- Run in-game visual and interaction QA across GUI scales, especially `320x240`, `427x240`, and wide layouts.
- The standalone read-only `BROWSE` entry, explicit status/reward filters, keyboard graph navigation, optional `display.layout` codec/editor support, and measured graph-render performance remain later plan work.
