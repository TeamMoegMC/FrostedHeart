# Nutrition Documentation

Nutrition covers distinct player-survival and town-resident models.

| Document | Scope | Status |
|---|---|---|
| [nutrition-player-resident.md](nutrition-player-resident.md) | Verified player/resident mechanics, shared food data, current balance, and next-design boundaries | Current |

Primary anchors: `NutritionCapability`, `NutritionRecipe`, `ResidentNutrition`, `TownHousingMealService`, `FHCapabilities.PLAYER_NUTRITION`, `frostedheart:diet_override`, `FHConfig`.

Use the current-state document as the implementation baseline. The open redesign questions are in [the player/resident nutrition redesign discussion](../../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md); actionable implementation work belongs in [`plans/`](../../plans/).
