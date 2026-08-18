# Town residential care, nutrition, and ration guarantees

- Time: `2026-08-17 12:17:19 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `town residents, housing assignment, food settlement, Mayor's Seal UI, persistence and networking`

## Completed

- Added persistent four-channel resident nutrition with legacy Codec/NBT defaults and resident-specific recovery/growth effects.
- Added `TownHousingPlan`, centralized two-pass guaranteed ration allocation, recipient-specific high-level food selection, and atomic daily residential reassignment.
- Added the extensible `TownPolicyState` and three residential-care laws with next-settlement activation and a shared seven-day cooldown.
- Added Mayor's Seal housing/policy tabs, server-authoritative edit packets, client updates, bilingual text, and nutrition displays in resident/workforce/house views.
- Updated the town model and package reference, including the revised morning settlement order and exact formulas.

## Decisions

- The housing queue is the single priority source after legacy migration; comfort only seeds old saves. Guarantee counts reserve first-pass full rations but are not occupancy caps.
- Fat scales existing intelligence growth, protein scales infant/child strength growth, carbohydrate directly supports mental recovery, and vegetables directly support health recovery. Fat/protein amplify recovery only when the corresponding direct nutrient exists.
- Daytime recruits take the first free bed without moving existing residents. Full triage occurs only at the next morning settlement.
- Death/exit checks run after centralized meals and recovery so a resident can be rescued by the day's care assignment.

## Validation

- `./gradlew test --offline` — full suite passed, including new nutrition, triage, allocation, policy, migration, packet and aging tests.
- Both locale JSON files parsed successfully during development; Java main and test compilation completed as part of the suite.

## Remaining

- Perform an in-game smoke test with two clients for drag ordering, guarantee edits, policy pending/cooldown display, daily moves, exact food depletion, and save/reload.
- Stage 3/4 simulations do not yet model four-channel nutrition, residential queues, or care policies; extend and rerun them before balance conclusions.
