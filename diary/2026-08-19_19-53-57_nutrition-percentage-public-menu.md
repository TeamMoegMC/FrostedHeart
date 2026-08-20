# Nutrition percentage, public menu, and resident attribute support

- Time: `2026-08-19 19:53:57 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `player nutrition, food resolution, resident meals, nutrition-driven attributes, town simulation and UI`

## Completed

- Introduced one `0..100` `FoodNutritionProfile` and `FoodNutritionResolver` for static recipes, Tooltip, players, residents and Caupona dynamic stacks without changing `new_food_value.xlsx` or generated recipe values.
- Migrated player NBT and configuration scales, made gains depend on effective hunger, made loss deterministic, clamped all writes and removed overeating reverse consumption.
- Changed resident daily food demand to `20`, implemented the hard-tier eight-chunk public menu, kept saturation in food resource units only and removed runtime `nutritionQuality`.
- Added configurable normalized support matrices, long-term EMA satisfaction, stable personal potentials, actual-work activity vectors, nutritional caps, growth/decay, effective intelligence and persisted settlement explanations.
- Reused the pure menu and attribute models in gameplay and Stage 3/4 simulations; updated [nutrition](../docs/nutrition/nutrition-player-resident.md), [town](../docs/town/town-model.md), the [decision discussion](../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md), and the [completed plan](../plans/2026-08-19_18-23-50_nutrition-resident-attribute-redesign.md).

## Decisions

- Public nutrition facts are percentages, while player and resident consumers retain different settlement timing.
- A resident's meal quantity remains energy-based; its four nutrition channels use vanilla hunger only.
- Nutrition enables recovery and activity-driven development. It does not directly create strength or intelligence, and it never permanently removes learned intelligence.
- Old `HouseBuilding.DailyReport` nutrition fields remain decode-only for save compatibility.

## Validation

- `./gradlew test`: `197` tests, `0` failures, `0` errors.
- Focused coverage includes raw-to-percent examples, effective hunger, NBT/config migration, hunger-only resident points, hard food tiers, common menu composition, matrix fallback, EMA, deterministic potentials, activity gates, slow strength decay and non-decaying intelligence.
- `git diff --check` and modified JSON parsing passed; invariant searches found no runtime `nutritionQuality` decision path or direct static recipe consumer outside `FoodNutritionResolver`.
- Two `50 residents x 120 days x 100 trials` simulations completed without NaN/Infinity. Pure hunting had P50 food-potential self-supply `0.6146` and survival `0`; daily `6.25` baked potatoes had P50 `0.6514` and survival `0.06`.

## Remaining

- Rebalance town food production and external supply around daily demand `20`; the old `6.5`-demand survival baseline is no longer applicable.
- Smoke-test dynamic Caupona food, player NBT migration and resident explanation layout in a real modpack world before release.
