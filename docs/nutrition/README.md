# Nutrition Documentation

Nutrition currently spans player survival and town-resident settlement. Do not assume that player nutrition pools and resident nutrition use the same model.

## Reading Map

| Document | Use it for | Documentation status |
|---|---|---|
| [prev-conversation.md](prev-conversation.md) | An imported explanation of player nutrition, resident nutrition, food data, efficiency calculations, and balance concerns | Transitional source note. Reverify mechanics and data before relying on it; balance opinions belong in `discussion/` when revisited. |

## Primary Code And Data Anchors

- `NutritionCapability`, `NutritionRecipe`, and `FHCapabilities.PLAYER_NUTRITION`
- `Resident`, town food settlement, and resident nutrition statistics
- `frostedheart:diet_override` recipes
- `FHConfig` nutrition gain and consumption settings

Future work should separate verified player mechanics, verified resident mechanics, and balancing discussion into distinct artifacts.
