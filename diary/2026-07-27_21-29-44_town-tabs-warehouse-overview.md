# Town tab icons and warehouse overview

- Time: `2026-07-27 21:29:44 +0800`
- Author: `Codex /root; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town tab framework, House GUI, Warehouse GUI, en_us/zh_cn localization`

## Completed

- Corrected the shared tab-button argument order so the bright texture is used for the active tab and the dark texture for inactive tabs.
- Extended town tabs with a foreground item icon and localized hover title.
- Assigned House/Red Bed icons to the House overview/resident tabs and Warehouse/Chest icons to the Warehouse overview/inventory tabs.
- Localized the Warehouse container and overview in English and Chinese.
- Changed the Warehouse overview to show only the overall workable status when valid and list only unmet initialization, overlap, or structure conditions when invalid.
- Displayed Warehouse item capacity, area, and volume as integers; capacity is labeled `物品容积` in Chinese.

## Decisions

- The existing texture UVs were already correct. The visual inversion came from passing inactive and active backgrounds to `TabImageButtonElement` in the wrong order, so the fix belongs in the shared town screen.
- Warehouse workability currently inherits exactly the three base-building predicates. No area, volume, or capacity threshold was invented for the UI.
- Tab foreground icons use the existing CUI item-icon renderer instead of introducing new texture assets.

## Validation

- `jq empty` passed for both edited localization files.
- `git diff --check` passed.
- `./gradlew compileJava --offline` passed with the repository's existing warnings.
- `./gradlew build --offline` passed; existing repository-wide license warnings remain non-fatal.

## Remaining

- Visually verify icon centering, active-state contrast, and tooltip placement in game at commonly used GUI scales.
