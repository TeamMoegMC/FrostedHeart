# Town workforce dispatch

- Time: `2026-08-14 17:56:41 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `town staffing persistence, daily assignment, Mayor's Seal UI, event feedback, Stage 3/4 simulator`

## Completed

- Replaced sticky vacancy filling with the shared two-pass `TownAssignmentModel`: strict ordered guaranteed targets followed by lowest capacity-fill surplus distribution.
- Added persisted `TownStaffingPlan`, legacy roster migration, atomic daily roster rebuilding, server-authoritative edit/sync packets, and a drag/slider staffing tab in the Mayor's Seal.
- Made mining and hunting production consume the morning roster snapshot, added staffing target crossing events/Tips, and retired the old per-building priority formulas from active gameplay.
- Added Stage 3/4 scenario staffing inputs and daily observables for target coverage, unassigned/disabled labour, and workplace changes. Updated `docs/town-model.md` and marked old sticky-assignment numerical plots as historical.

## Decisions

- A target is guaranteed first-pass staffing, not a hard cap. Workers beyond targets are distributed by minimum `assigned / capacity`; queue order breaks exact ties.
- Every resident is reassessed once per town settlement. Ineligible residents immediately release jobs; previous employment only breaks equal productivity scores to reduce needless churn.
- Queue/target edits take effect at the next settlement. Rejected or stale optimistic client edits receive the authoritative plan so the UI cannot remain desynchronized.
- Legacy assignment config keys remain readable for old server TOML files but are documented as ignored by the new planner.

## Validation

- `./gradlew test` — full suite passed.
- `./gradlew runTownSimulation ... stage3-t1-24-residents.json --runs 2 --seed 123` — passed against the attached TWR data; generated a 25-column `daily.csv` with 12 miners and 12 hunters under the zero-target baseline.
- Parsed both locale files and all Stage 3/4 scenario JSON with `jq`; `git diff --check` passed.

## Remaining

- Perform an in-game smoke test for drag reorder, target slider, next-day application, unavailable-building display, and two-client synchronization.
- Regenerate the formal 1,000-seed Stage 3/4 plots before using old probability or critical-population results for balance decisions.
