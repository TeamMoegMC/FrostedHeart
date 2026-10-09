# Reservoir block registration and data generation

- Time: `2026-10-09 18:13:27 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `warm stone / hot-water bag Registrate registration, language and loot generation, and existing GameTests`

## Completed

- Removed the reservoir block's item-language-key override and the two `noLang()` callbacks. Registrate now generates separate standard `block.frostedheart.*` translations while `WarmStoneItem` preserves the existing `item.frostedheart.*` translations. Added both Chinese block names.
- Replaced both `FHLootGen.existed()` exemptions with normal `RegistrateBlockLootTables.dropSelf` generation. Generated and retained both block loot tables and the English / upside-down English block translations.
- Retained `ThermalReservoirBlock.getDrops` so ordinary breaking and support loss return the complete stored stack, including temperatures and custom names. Placement, persistence, and exposed air/radiation exchange are unchanged.
- Updated four GameTest classes to obtain the registered items from `FHBlocks.WARM_STONE.asItem()` / `FHBlocks.HOT_WATER_BAG.asItem()`, replacing references to the removed `FHItems` fields.
- Documentation impact: updated [player temperature](../docs/climate/player-temperature.md) with the language and loot generation contracts.

## Decisions

- Keep block-side `.item(...)` registration and existing item IDs, item models, Curios tags, and thermal profiles.
- Keep the useful placement and thermal GameTests. Do not remove coverage to avoid compilation errors.
- Project build settings were not changed. Removed the temporary validation init script at the user's request; final data generation and GameTest execution used the existing build configuration.
- The companion repository has no `AGENTS.md`. Checked its warm-stone recipes and research IDs; all item IDs remain compatible. No companion files were changed.

## Validation

- `compileGameTestJava`: passed, including production compilation and all GameTest sources, resolving the previous ten missing-field errors. Log: `build/reservoir-registration-validation.log`.
- `runData runGameTestServer --continue --offline --no-daemon --console=plain`, Java 17.0.2, existing project build configuration: data generation passed without missing-loot-table or duplicate-language-key errors. Only the two block translations per generated locale and two new loot tables were produced for this change. Log: `build/reservoir-registration-final-validation.log`.
- GameTest: 103/104 passed. All four placed-reservoir cases passed: real right-click / creative placement / reload / pick block / breaking / support loss for both items; hot and cold environment exchange; fresh initialization; and real campfire radiation. Existing generator-field and cache tests also passed.
- `git diff --check`: passed. Verified no changes to `build.gradle`, `gradle.properties`, or `settings.gradle`, and no remaining references to the deleted reservoir fields in `src/`.

## Remaining

- Unrelated `ThermalDormantCoolingGameTests.sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift` still asserts storage version 4, while `DormantChunkThermalState.FORMAT_VERSION` is 5. This is the sole full-suite failure and was not changed in this work.
