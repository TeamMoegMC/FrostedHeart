# Town cold-house and food-resource settlement fixes

- Time: `2026-08-10 16:54:55 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town daily building scheduling, house settlement, item-resource conversion, Java tests, town model documentation`

## Completed

- Added a daily-settlement eligibility hook. Most buildings still require `isBuildingWorkable()`, while structurally valid houses settle existing residents even outside the `[0,50]°C` workable range.
- Added resident food conversion from vanilla hunger plus nominal saturation: `H + 2*H*saturationModifier`. Explicit `ItemResourceAmountRecipe` values remain authoritative.
- Extracted both rules into small numerical functions and added JUnit 5 regression tests.
- Updated `docs/town-model.md` and the town README. With the corrected food values, current hunting yields about `5.2070` raw or `22.5596` fully cooked food units per SWE-day, not `1.1974` food units.

## Decisions

- Temperature still controls whether a house is workable and can receive new residents; it no longer cancels obligations for residents already assigned there.
- The food formula uses actual nominal saturation points (`2*H*modifier`), not the saturation modifier itself. Items without a positive vanilla food value retain the historical fallback of `1` to avoid zero-value consumption failures.
- No TWR files or simulator implementation were changed in this task.

## Validation

- `./gradlew compileJava` — successful.
- `./gradlew test` — successful, 5 tests passed.
- Verified the documented vanilla meat properties against the mapped Minecraft `Foods` source and recomputed raw/fully cooked hunting baselines from `loot_tables/town/hunting.json`.

## Remaining

- Extreme temperature currently suppresses recovery through the existing house formula but does not directly inflict a temperature damage term.
- Invalid, overlapping, or undersized houses still do not settle; audit resident associations for a separate non-temperature stasis path.
- The future Java audit should report non-edible items present in resident-food tags and explicit food-resource recipe overrides.
