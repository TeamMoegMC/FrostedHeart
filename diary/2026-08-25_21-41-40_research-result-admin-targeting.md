# Research result administrative targeting

- Time: `2026-08-25 21:41:40 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `V2 result administrator commands, TeamResearchService, TeamKnowledgeData, test datapack instructions, and research documentation`

## Completed

- Added `/research result revoke <id>` and `/research result info <id>` beside the existing grant command.
- Added `/research <online-player> result grant|revoke|info <id>`; unqualified result commands still target the command source's current team. Both forms remain available under `/frostedheart research`.
- Added idempotent four-category removal and full snapshot synchronization. Revoke can delete orphan IDs absent from the current catalogue; known Prototype results reject revoke because they are physical items.
- Added result inspection for topic/type/payload, team acquisition categories, orphan state, and prototype profile revision. Suggestions merge current catalogue IDs with the affected team's retained orphan IDs.
- Updated the Phase 1 plan, living research documentation, and manual test datapack instructions.

## Decisions

- Explicit player targeting resolves the player's current Chorda team at command execution time rather than accepting a raw team UUID.
- Prototype grant delivers a new physical item to the affected player. Prototype revoke does not scan or delete inventory/world items.

## Validation

- `./gradlew compileJava test --tests "com.teammoeg.frostedresearch.data.TeamKnowledgeDataTest"` passed.

## Remaining

- None for the requested command surface.
