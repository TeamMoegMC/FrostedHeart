# Resident attributes and profession proficiency progression

- Time: `2026-07-27 18:51:43 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `resident initialization/persistence, house stat updates, hunting/mining productivity and workday proficiency growth`

## Completed

- Added the fixed-adult attribute model: independently sample strength and intelligence from the average of four uniform samples, while retaining health and mental at 50.
- Initialized independent hunting and mining proficiency with the low-biased `50U²` distribution, persisted both existing storage keys, and added one-time migration for missing legacy values.
- Made missing legacy strength and intelligence default to 50 for both codec and NBT loading; existing legal saved values remain unchanged.
- Removed the house's daily strength gain and starvation strength loss so current gameplay no longer changes adult strength.
- Added clamped diminishing proficiency growth with a per-resident, per-profession, once-per-town-day guard.
- Granted hunting experience to eligible workers when at least one HUNT unit is available, including fractional-roll days; granted mining experience only after positive ORE extraction. Storage rejection does not suppress either profession's experience.
- Updated hunting/mining weights, mining mastery bonus and cap, shared progression config, and the planner-facing production-model documentation.

## Decisions

- Daily production is calculated before proficiency is increased.
- `canResidentWork` filters daily production and experience; `canResidentBeAssigned` remains unchanged by explicit design.
- Existing valid config values remain user overrides; the new weights, bonuses and caps are defaults for new or regenerated config entries.

## Validation

- `./gradlew build --offline` completed successfully with the repository's existing 20 deprecation warnings and non-fatal repository-wide license report.
- A fixed-seed, one-million-sample probe measured adult attribute mean `50.0189`, standard deviation `14.4240`, initial proficiency mean `16.6442`, and `P(p <= 25) = 0.7081`; all samples stayed in range.
- The same probe verified mining/hunting lower, standard-worker and upper productivity endpoints, plus growth thresholds of 22/59/88/128 effective days for 50/80/90/100.
- `git diff --check` passed, and `ITownResidentWorkBuilding.canResidentBeAssigned` has no diff.

## Remaining

- The repository still has no unit-test framework under `src/test`; work-opportunity integration cases were validated through code paths and compilation rather than committed automated game tests.
