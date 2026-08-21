# Research Archive Privacy, Layout, And Modal Fix

- Time: `2026-08-21 02:31:15 +08:00`
- Author: `Codex (OpenAI; implementation and validation)`
- Status: `completed`
- Scope: `ResearchArchiveLayer`, archive panels, project dialog, drawing-desk widget integration, localization, tests, and research documentation

## Completed

- Restored the original technology tree's top icon-tab pattern through `ResearchFieldTabBar`; moved search directly above the left project index and removed category rows from that index.
- Filtered definitions before archive state, graph, list, summary, and dialog reconciliation. Normal mode now receives only non-hidden `isShowable`, unlocked, or completed research; editor mode retains all definitions.
- Removed normal-mode unknown-project placeholders and internal research IDs from graph/list tooltips.
- Moved research materials and all non-theory clue rows into `DETAIL`, kept only `MinigameClue` rows in `THEORY`, and made `EXPERIMENT` an empty future town-system boundary.
- Restored the centered dialog to the original `TechIcons.DIALOG` `302 x 170` surface, rendered it at `z=600`, covered the exact full archive with its dimming mask, and clamped dialog scrolling.
- Temporarily hides native `AbstractWidget` instances injected into the drawing-desk wrapper while the archive is active, including delayed FTB buttons, then restores their original state.

## Decisions

- Player discovery privacy is enforced at the archive data boundary rather than with anonymous rows, so unrevealed names, IDs, icons, and graph topology never reach normal archive components.
- The experiment tab remains present as an API/UI affordance but currently owns no research material, clue, or point presentation.
- The concise right summary remains unchanged in responsibility; complete project information and actions stay in the centered dialog.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.gui.archive.*"` passed.
- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed, including Java compilation and new visibility/tab-content coverage.
- `./gradlew test` passed for the full repository suite.
- Both research language JSON files parsed successfully; `git diff --check` reported only existing line-ending notices.
- Updated `docs/research/README.md`, `docs/research/research-ui.md`, and the active GUI plan to match the implemented privacy, layout, and tab contracts.

## Remaining

- In-game visual verification is still required for FTB widget injection timing, GUI scale variants, top field-tab spacing, the full modal mask, and item rendering depth.
