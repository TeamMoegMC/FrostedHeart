# 营养百分比、公共菜单与居民属性支持模型重构

- Time: `2026-08-19 18:23:50 +0800`
- Authors: `wyc; system designer`, `Codex; OpenAI implementation collaborator`
- Status: `completed`
- Scope: `player nutrition, shared food resolution, town public meals, resident nutrition-driven attributes`
- Related: [`docs/nutrition/nutrition-player-resident.md`](../docs/nutrition/nutrition-player-resident.md), [`discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md`](../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md)

## Goal

Expose every nutrition value as a `0..100` percentage, retain the existing spreadsheet and generated recipes, and make one `FoodNutritionResolver` authoritative for static and dynamic food. Keep player consequences while making player storage deterministic. Replace resident-specific greedy meals with a public menu, then make current and long-term nutrition explainably support health, mental state, strength, and intelligence.

## Decisions

- `FoodNutritionProfile` divides existing recipe values by `400` and clamps each channel to `0..100`; channel totals are not renormalized.
- Player storage is directly `0..100`. Effective hunger only grants nutrition for the hunger actually restored. Gain is `effectiveHunger * profile / 100 * 1.0`; loss is fixed at `hungerLost * 0.25`.
- Resident food quantity continues to use hunger plus nominal saturation, but nutrition points use only `itemFraction * hunger * profile`. Defaults become food `20`, reference points `200`, daily loss `1`, reference gain `2`, and maximum coverage `2`.
- Food levels remain absolute `4..0` priorities. A town-wide eight-chunk menu minimizes the aggregate four-channel shortfall to reserve `70`, then every recipient receives the same composition in proportion to their ration.
- `nutritionQuality` and the aggregate nutrition recovery multiplier are removed. No yesterday-meal coverage metric is added.
- Current availability is `clamp(reserve / 70, 0, 1)`. Four configurable normalized weight rows produce health, mental, strength, and intelligence support.
- Long-term availability uses a daily EMA with alpha `0.1`.
- Resident strength and intelligence potentials are stable UUID-derived four-sample values mapped to `40..100`, never below current attributes.
- Infants and children develop naturally. Adults grow only after successful building activity; mining and hunting default to physical `1.0`, learning `0.25`. Elders retain age decay and do not grow.
- Nutrition can slowly decay strength below a long-term support threshold, but never permanently decays intelligence. Effective intelligence is used by assignment, production, and forecasts.

## Formulas

```text
profile_i = clamp(recipe_i / 400, 0, 100)

playerGain_i = effectiveHunger * profile_i / 100 * nutritionGainRate
playerLoss_i = hungerLost * nutritionConsumptionRate

residentPoints_i = sum(itemFraction_j * hunger_j * profile_ij)
coverage_i = clamp(residentPoints_i / referencePoints, 0, maximumCoverage)
reserve_i' = clamp(reserve_i - dailyLoss + gainAtReference * coverage_i, 0, 100)

n_i = clamp(reserve_i / healthyReserve, 0, 1)
Q_r = sum(normalizedWeight_ri * n_i)
avgN_i' = 0.9 * avgN_i + 0.1 * n_i

healthNutrition = 0.25 + 0.75 * Q_health
mentalNutrition = 0.35 + 0.65 * Q_mental

C_strength = strengthPotential * (0.65 + 0.35 * avgQ_strength)
growth_strength = T_strength * g_strength
    * (0.2 + 0.8 * sqrt(Q_strength * avgQ_strength))
    * max(0, 1 - strength / C_strength)

C_intelligence = intelligencePotential * (0.85 + 0.15 * avgQ_intelligence)
growth_intelligence = T_intelligence * g_intelligence
    * (0.4 + 0.6 * sqrt(Q_intelligence * avgQ_intelligence))
    * max(0, 1 - intelligence / C_intelligence)

effectiveIntelligence = intelligence * (0.85 + 0.15 * Q_intelligence)
```

Default matrix columns are protein, fat, vegetable, carbohydrate:

| Result | Protein | Fat | Vegetable | Carbohydrate |
|---|---:|---:|---:|---:|
| Health | 0.50 | 0.10 | 0.30 | 0.10 |
| Mental | 0.10 | 0.30 | 0.20 | 0.40 |
| Strength | 0.55 | 0.15 | 0.05 | 0.25 |
| Intelligence | 0.05 | 0.30 | 0.40 | 0.25 |

## Steps

1. Add the shared profile and resolver, then migrate player storage, gain/loss, Tooltip, commands, effects, dynamic food, and configuration.
2. Rework resident nutrition points and the global public menu while preserving food levels, ration guarantees, and care ordering. Remove aggregate nutrition quality.
3. Add persistent long-term nutrition, potentials, and last-settlement snapshots; add transient daily activity and a pure attribute transition model shared by gameplay and simulation.
4. Route effective intelligence through assignment, production, and forecasts. Expose potentials, nutrition caps, actual changes, growth efficiency, and limiting channels in resident details.
5. Update current documentation, append the discussion decision, run all tests and the requested P50 comparison, then record the outcome here and in the diary.

## Validation

- Cooked beef resolves raw protein `24000` to `60%`; hunger `8` gives `480` resident points and player gain `4.8` when fully consumed.
- Baked potato resolves carbohydrate/vegetable to `40%/20%`; hunger `5` gives `200/100` resident points.
- Saturation changes resident food quantity but never per-item nutrition points.
- Static and dynamic stacks resolve identically for player, resident, Tooltip, and simulation paths.
- Public-menu composition is common to all recipients and respects hard food levels.
- Codec/config migration, matrix normalization, EMA, deterministic potentials, activity gates, caps, decay, effective intelligence, and explanation snapshots receive focused tests.
- Full Java and simulator tests pass; a `50 residents x 120 days x 100 trials` comparison completes without non-finite output.

## Documentation Impact

- Substantially revise the nutrition living document and the resident nutrition sections of the town model.
- Append the accepted decisions to the related discussion.
- Add a completion diary entry and update this plan with status and outcome.

## Outcome

Completed on `2026-08-19 19:53:57 +0800`.

- Added `FoodNutritionProfile` and the authoritative `FoodNutritionResolver`; static duplicates now produce a stable diagnostic choice and Caupona derives its result from actual ingredient stacks.
- Migrated player persistence, commands, UI and consequences to `0..100`; effective hunger controls gains, hunger loss controls fixed consumption, and the versioned config migration preserves prior configured rates.
- Replaced per-resident greedy meals with the eight-chunk, hard-tier public menu. Resident points use hunger only while resource allocation retains hunger plus saturation; actual warehouse consumption is distributed using one shared composition.
- Removed runtime `nutritionQuality`, added normalized configurable support matrices, EMA nutrition, UUID-derived potentials, activity-gated growth, strength decay, effective intelligence and persisted explanation snapshots.
- Routed gameplay and Stage 3/4 simulation through shared pure nutrition/menu/attribute formulas. Updated the nutrition and town living docs and appended the accepted result to the related discussion.

Validation completed with `197` Java tests, `0` failures and `0` errors; JSON and `git diff --check` passed. The requested two `50 residents x 120 days x 100 trials` runs completed without non-finite output. The old pure-hunting scenario produced food-potential self-supply P50 `0.6146` and survival `0`; adding `6.25` baked potatoes per day produced P50 `0.6514` and survival `0.06`. These results establish a new food-economy balance baseline rather than an acceptance threshold.

See [the implementation diary](../diary/2026-08-19_19-53-57_nutrition-percentage-public-menu.md), [the nutrition living document](../docs/nutrition/nutrition-player-resident.md), and [the town model](../docs/town/town-model.md).
