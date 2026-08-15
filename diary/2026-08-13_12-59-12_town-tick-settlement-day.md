# Town tick settlement-day semantics

- Time: `2026-08-13 12:59:12 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `/town tick`, town observation history sequencing, event labels, audit units, tests, and model documentation`

## Completed

- Changed town history from world-day replacement to monotonic town settlement days. Every completed `tickMorning`, including every `/town tick`, now appends a distinct history snapshot and therefore represents one day of town-state advancement.
- Added the pure `TownHistoryModel.nextSettlementDay` rule: the first entry uses the stable `WorldClockSource` day; each later entry uses `max(currentWorldDay, latestSettlementDay + 1)`. Manual ticks do not mutate global world time.
- Kept client history merging idempotent by town-day key so duplicate network packets still replace rather than duplicate an entry. The retained limit now means the latest configured number of town settlements.
- Updated the command success text, event-time localization, Java documentation, audit unit, and `docs/town-model.md` to state the new semantics.

## Decisions

- Refugee spawning and waiting were intentionally untouched. They continue to use stable world-clock days and their existing same-world-day guard; manual town advancement does not create additional refugee refresh opportunities.
- No separate persisted counter was added. The latest retained history entry is sufficient to continue the monotonic sequence, including after save reload and 30-entry trimming.

## Validation

- Added tests for repeated manual settlement advancement, natural world-day catch-up, duplicate packet replacement, and retention.
- Relevant history and stage-0 audit tests — successful.
- `./gradlew test` — successful.
- English and Chinese locale JSON parsing and `git diff --check` — successful.

## Remaining

- In-game smoke test: execute `/town tick` repeatedly without changing world time and confirm that each invocation adds one chart point and one distinct town-day label while refugee spawning remains world-day limited.
