# Town resident food nutrition ordering

- Time: `2026-08-10 17:56:10 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `same-level resident food consumption ordering, shared FH nutrition calculation, tests and town documentation`

## Completed

- Added `TownFoodNutritionModel` as the shared implementation of the FH scalar nutrition value previously calculated inside `HouseBuilding`.
- Changed only `RESIDENT_FOOD_LEVEL` attribute consumption to sort candidates inside one level by nutrition per resident food resource unit, highest first.
- Added a stable item registry ID plus NBT tie-breaker so identical-quality foods no longer depend on `HashMap` iteration order.
- Reused the same nutrition extraction method in house daily settlement, keeping selection and resident recovery on one mathematical definition.
- Documented the formula and priority hierarchy in `docs/town-model.md` and the town package README.

## Decisions

- Food level remains the primary key. Nutrition quality only orders items within the level selected by the existing level 4-to-0 action.
- The quality score is `NutritionRecipe.getNutritionValue()/4` divided by the item's resident food resource amount. This directly maximizes the existing house metric `N_sum/F_consumed` and introduces no new configurable multiplier.
- Invalid or missing nutrition and non-positive resource amounts have quality zero. Equal scores use registry ID and NBT ordering for reproducibility.
- This deliberately uses the existing scalar nutrition sum rather than adding dietary diversity or per-channel balancing in this small fix.

## Validation

- `./gradlew test` — successful; 9 tests passed, including 3 new nutrition-ordering tests.
- `git diff --check` — clean.

## Remaining

- TWR KubeJS food tags and external datapack cross-level duplicates still need a separate runtime audit.
- If nutrition diversity becomes a gameplay requirement later, design it as a separate daily resident metric instead of silently changing this inventory priority score.
