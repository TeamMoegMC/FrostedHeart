# Nutrition Documentation

Nutrition covers distinct player-survival and town-resident models.

| Document | Scope | Status |
|---|---|---|
| [nutrition-player-resident.md](nutrition-player-resident.md) | Current player/resident mechanics, unified food profiles, priority-ordered house menus, and attribute support | Current |

Primary anchors: `FoodNutritionProfile`, `FoodNutritionResolver`, `PlayerNutritionState`, `NutritionCapability`, `NutritionRecipe`, `ResidentNutrition`, `ResidentNutritionSupportModel`, `ResidentAttributeModel`, `TownHousingMealService`, `FHCapabilities.PLAYER_NUTRITION`, `frostedheart:diet_override`, `FHConfig`.

Use the current-state document as the implementation baseline. The accepted redesign decisions and their resolution are recorded in [the player/resident nutrition redesign discussion](../../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md), [the original implementation plan](../../plans/2026-08-19_18-23-50_nutrition-resident-attribute-redesign.md), [the type-semantics cleanup](../../plans/2026-08-19_23-05-55_nutrition-type-semantics-cleanup.md), and [the simplified resident attribute model](../../plans/2026-08-20_17-31-55_resident-nutrition-attribute-simplification.md).
