# Resident nutrition attribute model simplification

- Time: `2026-08-20 18:00:07 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `resident nutrition support, strength/intelligence progression, persistence, UI, town simulation`

## Completed

- Replaced the long-term nutrition/potential/cap model with the shared daily equation in `ResidentAttributeModel.settleDailyAttribute`: growth minus current-support nutrition decay minus elder age decay.
- Added clamped top-level `ResidentActivity` and flat `ResidentAttributeChange`; work activity combines by channel maximum and mining/hunting remain `(1.0, 0.25)` by default.
- Removed EMA, deterministic potentials, nutrition attribute caps, effective intelligence and the elder strength floor from gameplay and Stage 3/4.
- Simplified resident persistence to four nutrition reserves plus a flat explanatory `ResidentNutritionSnapshot`. Old `nutritionDevelopment` data is ignored by Codec and raw NBT paths without changing stored attributes or reserves.
- Added all accepted balance values to `TownModelParameters.Defaults` and `FHConfig`, updated the Stage 0 parameter audit, added config version migration, and changed strength weights to `0.75/0.08/0.03/0.14`.
- Updated both resident detail views and English/Chinese text to show actual activity, effective activity, growth, nutrition decay, age decay, net change and dynamic limiting nutrients.
- Updated [nutrition](../docs/nutrition/nutrition-player-resident.md), [town](../docs/town/town-model.md), the [decision discussion](../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md) and the [completed plan](../plans/2026-08-20_17-31-55_resident-nutrition-attribute-simplification.md).

## Decisions

- Only current post-meal support affects strength/intelligence. The persisted snapshot is explanation output and never future model input.
- Positive growth caps do not clip acquired attributes. Support below the maintenance threshold opens a separate nonlinear permanent decay term.
- Elder growth and fixed age decay compete in the same equation, allowing good nutrition and activity to maintain attributes without a hard floor.
- Stored intelligence is the only intelligence used by production, assignment and simulation.

## Validation

- `./gradlew test`: `205` tests, all passed.
- Regression coverage includes activity interpolation/max, strength weights, threshold boundaries, `D^1.5`, all three accepted 15-day adult samples, juvenile `70/70` calibration, elder equilibria, no strength floor, adult exponential approach, transition-day growth, old Codec/raw NBT behavior and snapshot round-trip.
- Modified JSON parsing, static removal searches and `git diff --check` passed.
- `50 residents x 120 days x 100 trials` completed at `build/reports/town-model/simulations/2026-08-20-resident-attribute-simplification-p50-baseline`. Food-potential self-supply P50 was `0.6296`; every trial experienced food shortage and survival remained `0`, so the known food-economy deficit remains visible. Daily CSV metrics contain no NaN/Infinity.

## Remaining

- Smoke-test the expanded resident explanation rows and old-world load in the real modpack UI before release.
