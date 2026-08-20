# 居民营养与属性成长模型简化重构

- Time: `2026-08-20 17:31:55 +0800`
- Authors: `wyc; system designer`, `Codex; OpenAI implementation collaborator`
- Status: `completed`
- Scope: `resident nutrition support, strength/intelligence progression, persistence, UI, town simulation`
- Related: [`2026-08-19_18-23-50_nutrition-resident-attribute-redesign.md`](2026-08-19_18-23-50_nutrition-resident-attribute-redesign.md), [`nutrition-player-resident.md`](../docs/nutrition/nutrition-player-resident.md), [`2026-08-19_15-02-51_nutrition-redesign-boundaries.md`](../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md)

## Goal

Replace long-term nutrition, UUID-derived potentials, nutrition caps, effective intelligence, and the elder strength floor with one current-support attribute transition shared by gameplay and simulation. Preserve player nutrition, resident reserves, the public menu, and health/mental recovery.

## Decisions

- Daily strength/intelligence change is positive growth minus current malnutrition decay minus elder age decay.
- Effective activity is the age baseline plus the recorded activity applied to the remaining share: infant `1.0`, child `0.7`, adult `0.3`, elder `0.1`.
- Growth keeps a linear remaining-gap factor, so adult attributes approach `100` exponentially over time.
- Deficiency activates below strength/intelligence support `0.40/0.30`, uses exponent `1.5`, and loses at most `0.70/0.17` points per day at zero support and attribute `100`.
- Elder strength uses growth `0.06` and age decay `0.0048`, giving a full-nutrition idle equilibrium of `20`; elder intelligence uses `0.05/0.002`, giving equilibrium `60`.
- Strength support weights become protein/fat/vegetable/carbohydrate `0.75/0.08/0.03/0.14`.
- Juvenile full-support rates become infant `1.8/1.6` and child `3.9/4.2`, targeting approximately `70/70` after the current 30-day infant and 30-day child stages with mining/hunting activity.
- All numeric tuning is exposed through `TownModelParameters` and `FHConfig`.
- Obsolete `nutritionDevelopment` save data is ignored. Existing attributes and reserves remain authoritative; the new explanation snapshot begins empty.

## Steps

1. Add flat activity/change types and move the shared transition into `ResidentAttributeModel`.
2. Remove long-term/potential/effective-intelligence state and replace the persisted snapshot.
3. Update gameplay lifecycle, production, Stage 3/4 simulation, configuration, audits, and UI.
4. Replace obsolete tests with formula, persistence, parity, and calibration coverage.
5. Update living docs and discussion, run full validation and the requested simulation, then record the outcome here and in the diary.

## Validation

- Verify the three accepted 15-day adult malnutrition examples, juvenile `70/70` calibration, elder equilibria, and exponential adult approach.
- Verify no remaining EMA, potential, nutrition-cap, effective-intelligence, or elder-floor decision path.
- Run the full Java suite, JSON parsing, `git diff --check`, and `50 residents x 120 days x 100 trials`.

## Outcome

Completed on `2026-08-20`.

- Added top-level `ResidentActivity` and flat `ResidentAttributeChange`; `ResidentAttributeModel.settleDailyAttribute` now owns the shared strength/intelligence transition.
- Removed EMA, UUID potentials, nutrition caps, effective intelligence and the elder strength floor from resident state, gameplay, production and Stage 3/4 simulation.
- Replaced `nutritionDevelopment` persistence with a non-causal `ResidentNutritionSnapshot`; old fields are ignored while existing attributes and reserves remain unchanged.
- Added all accepted rates, thresholds, age activity baselines, elder decay values, work activity vectors and strength weights to `TownModelParameters`/`FHConfig`, including one-time version-2 migration for changed existing defaults.
- Updated resident detail explanations, Stage 0 audit, living nutrition/town docs and the original discussion.
- `./gradlew test` passed `205` tests. Modified JSON parsing, static invariant searches and `git diff --check` passed.
- The requested `50 residents x 120 days x 100 trials` baseline completed at `build/reports/town-model/simulations/2026-08-20-resident-attribute-simplification-p50-baseline`: food-potential self-supply P50 `0.6296`, food-shortage probability `1.0`, survival probability `0.0`. Daily metric CSV files contain no NaN/Infinity.
