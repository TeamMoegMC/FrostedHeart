# Nutrition type semantics cleanup

- Time: `2026-08-19 23:10:53 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `food profile, player nutrition state, resolver event, Caupona compatibility`

## Completed

- Removed the unitless `Nutrition`, `MutableNutrition`, and `ImmutableNutrition` hierarchy.
- Made `FoodNutritionProfile` an immutable `0..100` food fact and introduced immutable `PlayerNutritionState` for player-only writes, eating and hunger loss.
- Changed `NutritionRecipe` to expose named raw channels and made Resolver perform the explicit `/400` conversion.
- Replaced Event mutation buffers with an original/result `FoodNutritionProfile`; migrated Caupona to a compat-local percentage accumulator.
- Removed the unregistered player nutrition packet and its dead registration/sending comments; updated Tooltip, menu, commands, tests and [living documentation](../docs/nutrition/nutrition-player-resident.md).

## Decisions

- Recipe raw values, food percentages, player state and resident reserves do not share a common vector interface.
- `GatherFoodNutritionEvent` is an internal typed bridge for optional compat, not a third-party compatibility API.
- Player NBT keys and version remain unchanged; this refactor does not alter gameplay formulas or generated food data.

## Validation

- `./gradlew test`: `201` tests, `0` failures, `0` errors.
- Focused tests cover recipe raw conversion, profile bounds, Tooltip shares, effective hunger, fixed loss, state bounds, old/default NBT and Caupona percentage weighting.
- Modified JSON parsing and `git diff --check` passed.
- Static searches found no legacy nutrition type, mutation-buffer API, ambiguous raw/percent adapter, or dead sync-packet reference.

## Remaining

- Smoke-test an actual Caupona soup and a legacy player save in the assembled modpack before release.
