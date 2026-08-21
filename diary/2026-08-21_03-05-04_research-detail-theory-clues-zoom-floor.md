# Research detail theory clues and zoom floor

- Time: `2026-08-21 03:05:04 +08:00`
- Author: `Codex (OpenAI; development agent)`
- Status: `completed`
- Scope: `ResearchProjectWorkspace`, `ResearchGraphViewport`, `ResearchWorkspaceState`, focused tests, and research GUI documentation

## Completed

- Included real theoretical `MinigameClue` rows in the `DETAIL` checklist while retaining the dedicated `THEORY` filtered view.
- Kept `EXPERIMENT` empty for future town integration and continued excluding the synthetic experiment-points row from clickable clue lists.
- Changed the graph zoom lower bound to `15%` and made “fit all” center the current projection at that same fixed zoom.
- Preserved low-zoom icon and name rendering minimums.

## Decisions

- `DETAIL` is the complete player-facing clue checklist; `THEORY` is an additional focused view, not an exclusive storage location.
- The zoom control and “fit all” share `ResearchWorkspaceState.MIN_ZOOM` so their lower-bound behavior cannot diverge.

## Validation

- `ResearchProjectWorkspaceContentTest` covers theoretical clues appearing in both `DETAIL` and `THEORY`, with `EXPERIMENT` empty.
- `ResearchWorkspaceStateTest` locks the `15%` lower bound and scaled node/content minimums.
- `./gradlew.bat test --tests "com.teammoeg.frostedresearch.*"` passed.
- `./gradlew.bat test` passed for the full repository.
- `git diff --check` reported no whitespace errors.

## Remaining

- In-game visual QA is still needed to confirm the `15%` overview framing at supported GUI scales.
