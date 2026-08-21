# Research Archive Construction Crash Fix

- Time: `2026-08-21 02:00:10 +08:00`
- Author: `Codex (OpenAI; investigation, implementation, and validation)`
- Status: `completed`
- Scope: `DrawDeskScreen`, `ResearchArchiveLayer`, and archive construction regression coverage

## Completed

- Restored lazy archive construction so opening the drawing desk only initializes the existing drawing-desk surface; the archive is created after the technology-tree action.
- Fixed the archive constructor crash by initializing `ResearchTypeListPanel` and `ResearchGraphViewport` before setting the search box text that invokes their filter callbacks.
- Added `ResearchArchiveLayerConstructionTest` to cover the constructor callback ordering that caused the reported crash.

## Decisions

- Kept the requested concise right summary and centered project-detail dialog unchanged.
- Corrected lifecycle and initialization ordering instead of suppressing the null pointer or weakening the search callback.
- No living documentation update was needed because player-facing behavior and system contracts did not change.

## Validation

- In-game report confirmed lazy construction restored right-click opening of the drawing desk.
- The supplied crash stack identified `ResearchArchiveLayer$1.onTextChanged` during `TextBox.setText` as the archive crash root cause.
- `./gradlew test --tests "com.teammoeg.frostedresearch.gui.archive.ResearchArchiveLayerConstructionTest"` passed.
- `./gradlew test --tests "com.teammoeg.frostedresearch.*"` passed.
- `./gradlew test` passed for the full repository suite.

## Remaining

- Restart the client with the rebuilt classes and manually verify drawing desk -> technology tree -> archive -> project dialog.
