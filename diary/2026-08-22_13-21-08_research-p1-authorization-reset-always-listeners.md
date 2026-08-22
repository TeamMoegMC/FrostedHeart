# Research P1 authorization, reset, and always-listener fixes

- Time: `2026-08-22 13:21:08 +0800`
- Author: `Codex; OpenAI; implementation and validation role`
- Status: `completed`
- Scope: drawing-desk C2S authorization, administrative research reset, global listener initialization, regression tests, and `docs/research/`

## Completed

- Bound every drawing-desk operation packet to the sender's current drawing-desk menu, exact loaded tile and dimension, eight-block interaction range, and current team ownership; rejected invalid operation shapes and out-of-board card positions.
- Corrected `CardPos#valueOf` cache bounds so hostile negative coordinates are represented for validation instead of indexing the cache with a negative column.
- Made administrative reset revoke current and repeat-level-recorded reversible effects, clear matching current selection, reset repeat level, restore shared unlock entries from remaining grants, and synchronize effect, variant, and project state.
- Split infinite-research iteration rollover into `TeamResearchData#resetForRepeat`, preserving already-awarded iteration effects before incrementing the repeat level.
- Made null-team tick/kill listener registration and removal represent global scope for `always: true` definitions, and skipped repeated tick completion writes once a team's clue state is complete.
- Added focused regression tests and updated the research architecture, definition, state, gameplay, and known-risk documentation.

## Decisions

- Required an actually open `DrawDeskContainer`, not only proximity to a block position, so normal menu lifetime remains the server-side session capability.
- Replayed remaining authoritative grants after revocation, preventing one reset from removing an unlock still granted by another completed research without clearing unrelated retained rewards.
- Reversed `EffectStats` exactly, but did not infer refunds for aggregate insight use or activation ingredients and did not attempt to claw back item, experience, or command side effects that have no reliable provenance.
- Added the counted `Effect#revoke(TeamResearchData, int)` contract so additive effects can reverse every infinite iteration while set-like unlock effects remain idempotent.
- Preserved infinite-research reward accumulation by keeping normal iteration rollover distinct from permission-level administrative rollback.
- Used the existing null UUID meaning in `ResearchHooks.ListenerList` as the global-listener contract rather than introducing another listener registry.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedresearch.network.FHDrawingDeskOperationPacketTest" --tests "com.teammoeg.frostedresearch.data.TeamResearchDataResetTest" --tests "com.teammoeg.frostedresearch.research.clues.AlwaysListenerClueTest"` — `BUILD SUCCESSFUL` (`9` actionable tasks, `4` executed, `5` up-to-date); existing deprecation warnings only.
- After adding counted infinite-effect reversal, `./gradlew test --tests "com.teammoeg.frostedresearch.data.TeamResearchDataResetTest"` — `BUILD SUCCESSFUL in 24s`.
- Final `./gradlew test` — `BUILD SUCCESSFUL in 6s` (`9` actionable tasks, `2` executed, `7` up-to-date); existing deprecation warnings only.

## Remaining

- Exercise these contracts on an integrated multiplayer server with two teams/desks, overlapping unlock effects, a real advancement/kill event, and player-bound rewards; unit tests cover the corrected authorization decisions and state transitions but not Forge networking/menu lifecycle end to end.
