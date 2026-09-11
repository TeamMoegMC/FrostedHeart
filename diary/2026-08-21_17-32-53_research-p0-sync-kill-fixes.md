# Research P0 synchronization and kill-clue fixes

- Time: `2026-08-21 17:32:53 +0800`
- Author: `Codex; OpenAI; implementation and validation role`
- Status: `completed`
- Scope: `SinglePlayerTeam`, research kill listeners, regression tests, and `docs/research/`

## Completed

- Fixed `SinglePlayerTeam#getOnlineMembers` so an online fallback-team player is returned to `TeamDataHolder#sendToOnline`; offline players still produce an empty immutable collection.
- Fixed `ResearchHooks#kill` to ignore/prune completed listeners, evaluate `KillClue#isCompleted` against the killed entity, and complete/remove only matching team listeners.
- Added regression coverage for fallback-team member collection and kill-clue completion decisions.
- Updated the research definition, persistence/synchronization, and known-risk documents to describe the corrected behavior and retain the remaining end-to-end validation gaps.

## Decisions

- Preserved the existing team broadcast and listener-list architecture; both defects were local control-flow errors and did not require a new synchronization path or clue model.
- Kept stale completed kill listeners removable by returning success for an already-completed listener, while avoiding a second completion mutation.
- Extracted small package-private pure helpers where Minecraft server/entity instances would otherwise make the regression tests depend on a full game server.

## Validation

- `./gradlew test --tests 'com.teammoeg.chorda.dataholders.team.SinglePlayerTeamTest' --tests 'com.teammoeg.frostedresearch.ResearchHooksTest'` — `BUILD SUCCESSFUL`.
- `./gradlew test` — `BUILD SUCCESSFUL` (`9` actionable tasks, `4` executed, `5` up-to-date); existing deprecation warnings only.
- `git diff --check` — passed.

## Remaining

- Add a GameTest or multiplayer integration test that observes an actual no-FTB incremental packet and a real server kill event end to end; current unit tests cover the corrected local decisions.
