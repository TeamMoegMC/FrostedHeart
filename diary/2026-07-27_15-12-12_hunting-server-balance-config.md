# Hunting server balance configuration

- Time: `2026-07-27 15:12:12 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `FHConfig server town settings, HuntingBaseBuilding, HuntingBaseBlockEntity, and hunt terrain-resource naming`

## Completed

- Added a dedicated `ITown.Hunting` server-config section for standard-worker loot rolls, per-base minimum rolls, loot luck, worker-slot density, space and temperature requirements, proficiency, attribute weights, building-rating weights, resident-assignment priorities, and heat-network behavior.
- Normalized hunting productivity around the same fixed standard worker used by mining: all four attributes at 50 and zero profession proficiency.
- Replaced the hard-coded `2` rolls per legacy score with `7/6` loot-table rolls per standard worker-day, and replaced the legacy `0.1` luck coefficient with `7/120` loot luck per standard worker; both defaults are algebraically equivalent to the old formulas.
- Added accurate Java names for hunt reserve and recovery density while retaining the legacy fields and TOML keys as compatibility aliases.

## Decisions

- Preserve the existing one-roll-per-workable-base minimum by default, including bases with no productive worker, but expose `minimumLootRollsPerBaseDay = 0` as the explicit way to require labor.
- Keep the loot table data-driven; config controls rolls and luck, while item probabilities and stack sizes remain in `data/frostedheart/loot_tables/town/hunting.json`.
- Expose heat endpoint construction parameters with comments that priority/intake changes require existing block entities to reload.

## Validation

- `./gradlew compileJava --offline` completed successfully with only the repository's existing deprecation warnings.
- `./gradlew build --offline` completed successfully with only the repository's existing non-fatal license violations.
- Formula-equivalence checks across different attributes, proficiencies, and one-to-four-worker groups reproduced both the previous integer loot-roll counts and LootContext luck.
- `git diff --check` passed.

## Remaining

- Tanning racks are scanned and displayed but still do not affect hunting output.
- Hunting proficiency is read but is not increased by the hunting work method.
- Hunt resource is consumed for successful rolls even when warehouse capacity rejects some or all generated loot, matching existing behavior.
