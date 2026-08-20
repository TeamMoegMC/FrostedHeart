# Nutrition type semantics cleanup

- Time: `2026-08-19 23:05:55 +0800`
- Authors: `wyc; system designer`, `Codex; OpenAI implementation collaborator`
- Status: `completed`
- Scope: `food profile, player nutrition state, resolver event, Caupona compatibility`
- Related: [nutrition redesign](2026-08-19_18-23-50_nutrition-resident-attribute-redesign.md), [nutrition living document](../docs/nutrition/nutrition-player-resident.md)

## Goal

Remove the unitless `Nutrition`, `MutableNutrition`, and `ImmutableNutrition` abstraction. Give recipe raw values, public food percentages, player state, and resident state separate type boundaries without changing formulas, persistence, generated food data, or resident behavior.

## Decisions

- `FoodNutritionProfile` is an immutable `0..100` food fact and has no generic vector interface.
- `PlayerNutritionState` is an immutable `0..100` state with explicit eating, hunger-loss, and channel-write transitions.
- `GatherFoodNutritionEvent` carries an original and result profile; Caupona is the only internal listener and replaces the complete result.
- `NutritionRecipe` exposes named raw channels to the Resolver. The `/400` conversion remains explicit.
- No external Java API compatibility is retained. The unused player nutrition packet and all three legacy types are deleted.

## Validation

- Cover recipe raw conversion, profile bounds and Tooltip shares.
- Cover player gain/loss, effective hunger, state bounds, default state and old/new NBT.
- Cover Caupona weighted percentage accumulation without a second raw conversion.
- Verify legacy types and ambiguous mutation APIs have no source references.
- Run the complete Java tests, JSON parsing and `git diff --check`.

## Documentation Impact

- Update the nutrition living document and README type anchors.
- Add a completion diary entry; record release-only in-world smoke tests as remaining.

## Outcome

Completed on `2026-08-19 23:10:53 +0800`.

- Deleted `Nutrition`, `MutableNutrition`, `ImmutableNutrition`, and the unregistered `PlayerNutritionSyncPacket`.
- Reduced `FoodNutritionProfile` to an immutable percentage value and added the distinct immutable `PlayerNutritionState` with explicit state transitions.
- Made `GatherFoodNutritionEvent` carry only an original/result profile; moved Caupona weighting into a compat-local accumulator and removed ambiguous raw/percent conversion paths.
- Kept NBT version 2, legacy `/100` migration, configuration, generated recipe values, player formulas, and all resident behavior unchanged.

`./gradlew test` passed `201` tests with `0` failures and `0` errors. Modified JSON parsing, source invariant searches, and `git diff --check` also passed. See [the completion diary](../diary/2026-08-19_23-10-53_nutrition-type-semantics-cleanup.md) and [the updated living document](../docs/nutrition/nutrition-player-resident.md).
