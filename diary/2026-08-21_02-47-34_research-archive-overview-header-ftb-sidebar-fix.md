# Research Archive Overview, Header, And FTB Sidebar Fix

- Time: `2026-08-21 02:47:34 +08:00`
- Author: `Codex (OpenAI; debugging, implementation, and validation)`
- Status: `completed`
- Scope: `ResearchWorkspaceState`, `ResearchGraphViewport`, archive header layout, drawing-desk FTB integration, tests, and research UI documentation

## Completed

- Lowered the graph zoom floor from `55%` to `3%` and removed the `54 x 24` rendered-node floor that prevented fit-all from reducing the full graph.
- Kept icon and name rendering active at every zoom. Tiny nodes retain a `4px` icon and `0.25` text-scale minimum instead of becoming empty frames.
- Moved `ResearchFieldTabBar` into the same `31` pixel top header as the archive title and drawing-desk action, returning the recovered second row to graph content.
- Replaced the ineffective FTB widget visibility-only workaround with manager-level sidebar suspension. The latest sidebar group set is restored on drawing-desk return or screen close.
- Resolved `SidebarButtonManager` lazily so Frosted Research still opens without FTB Library, which is not a mandatory dependency in `mods.toml`.

## Decisions

- Full-tree overview keeps topology compact without removing project identity; tiny labels can extend beyond their proportional frame, while zoom/focus restores comfortable reading size.
- FTB groups are suspended only while this drawing-desk screen is in archive mode. Repeated ticks preserve groups repopulated by a resource reload before clearing them again.
- Native external widget state is still preserved separately because other integrations do respect `AbstractWidget.visible/active`.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.gui.archive.ResearchWorkspaceStateTest" --tests "com.teammoeg.frostedresearch.gui.archive.ResearchArchiveLayerConstructionTest"` passed.
- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed.
- `./gradlew test` passed for the full repository suite.
- The `81` JSON definitions in `run/config/fhresearches` form `15` ranks with calculated bounds of about `2608 x 1276`; the `320 x 240` QA viewport requires about `3.14%`, above the new minimum.
- `git diff --check` reported no whitespace errors; only existing line-ending conversion notices were present.

## Remaining

- Verify in game that fit-all contains the current modpack research dataset at each supported GUI scale, the six field icons remain in the top header without overlap, FTB `My Team` and related sidebar buttons disappear in archive mode, and all sidebar buttons return in drawing-desk mode.
