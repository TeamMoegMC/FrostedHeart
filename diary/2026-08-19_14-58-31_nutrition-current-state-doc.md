# Nutrition current-state documentation

- Time: `2026-08-19 14:58:31 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `docs/nutrition, player nutrition, resident nutrition, shared food data`

## Completed

- Reverified player nutrition persistence, eating and hunger-loss formulas, overflow behavior, effects, UI, commands, compatibility event, and disabled standalone synchronization against current source.
- Reverified persistent resident nutrition, centralized morning meals, ration and food selection, reserve formulas, recovery and growth effects, care priority, history statistics, and the retained aggregate house metric.
- Replaced the imported conversation and obsolete food-efficiency table with a current implementation baseline, updated nutrition documentation status, and identified next-design boundaries.
- Inspected the attached companion pack's resident food tags without changing it; only level 0 is currently defined in its source, and no companion `AGENTS.md` was present.

## Decisions

- Marked nutrition documentation `Current` because the document now separates verified behavior, compatibility leftovers, balance evidence, and unpromoted experiments.
- Removed `total recipe nutrition / hunger` as a claimed player-efficiency metric because `NutritionCapability.eat` already multiplies recipe nutrition by hunger.
- Kept the historical filename for existing links, but changed its title and purpose from a conversation transcript to the design baseline requested by the maintainer.

## Validation

- Compared the document against `NutritionCapability`, `HealthCommonEvents`, `NutritionRecipe`, `TownHousingMealService`, `ResidentNutrition`, `HouseDailyModel`, `TeamTownData`, `TownModelParameters`, `FHConfig`, relevant tests, and recent nutrition diary entries.
- Counted `110` generated `frostedheart:diet_override` recipes and checked the attached companion pack for all five resident food-level identifiers.
- `git diff --check` passed before the final diary addition; final documentation checks were run afterward.

## Remaining

- Player nutrition gain, consumption, overflow, punishment, and synchronization paths still lack focused automated tests.
- Gameplay defaults for resident nutrition remain unbalanced in the verified hunting-only baseline; the successful experimental calibration was not promoted.
- Use the design questions in `docs/nutrition/nutrition-player-resident.md` as the starting point for the next discussion.
