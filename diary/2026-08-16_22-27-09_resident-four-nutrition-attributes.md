# Add four nutrition attributes to residents

- Time: `2026-08-16 22:27:09 +08:00`
- Author: Codex/software engineer assistant
- Status: completed
- Scope: `src/main/java/com/teammoeg/frostedheart/content/town/resident/Resident.java`, `src/main/java/com/teammoeg/frostedheart/content/town/buildings/house/HouseResidentPanel.java`

## Completed

- Added four resident nutrition fields: `fat`, `carbohydrate`, `protein`, `vegetable`.
- Default value is `7000.0` per nutrient, same as player nutrition initial value.
- Added getters/setters, `addXxx` helpers, `setNutrition`, `getNutrition`, and a clamp to `[0, 100000]`.
- Extended `Resident.CODEC` with an optional `nutrition` object (`ResidentNutrition`) so old saves decode with defaults and new values are persisted/synced.
- Extended NBT `serialize()`/`deserialize()` with the four flat keys for the AI-town save path.
- Added the four nutrients to the house resident detail UI, using `NUTRITION_FULL_LEVEL = 10000` as the displayed full reference.

## Decisions

- Used a nested `ResidentNutrition` record in the Codec instead of four extra top-level Codec fields, because `RecordCodecBuilder.group` supports at most 16 fields and the existing Resident codec already has 13.
- Kept the underlying maximum at `100000` to match the player nutrition clamp, while UI displays `/10000` to match the player nutrition screen convention.
- Did not yet integrate these attributes into food consumption or health/mental settlement; this is only the data-model first step.

## Validation

- `gradlew compileJava` passes (only pre-existing deprecation warnings).
- Selected resident/food tests pass:
  - `com.teammoeg.frostedheart.content.town.resident.*`
  - `com.teammoeg.frostedheart.content.town.resource.TownFood*`
- Full `gradlew test` still reports 22 pre-existing environment/initialization failures unrelated to this change (Minecraft/CodecUtil static init in unit-test runtime).

## Remaining

- Next step: decide how per-resident nutrition is gained/consumed during house daily settlement and how it affects health/mental recovery or food selection.
- Consider adding the same fields to `TownStageThreeState.ResidentState` if the numerical simulator needs per-resident nutrition.
