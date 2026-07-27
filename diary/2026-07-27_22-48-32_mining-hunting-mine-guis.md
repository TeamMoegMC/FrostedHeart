# Mining, hunting, and mine GUIs

- Time: `2026-07-27 22:48:32 +0800`
- Author: `Codex /root; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `MineBaseBuilding, HuntingBaseBuilding, MineBuilding, daily production reports, town GUI`

## Completed

- Added persisted, backward-compatible mining and hunting daily reports containing planned work, actual work, per-item production, per-item storage, and an explicit stop reason.
- Derived storage loss from produced minus stored amounts and captured the real result of town item-resource actions.
- Added read-only global and per-chunk terrain-resource queries that do not mutate town data or depend on the server-only active-chunk cache.
- Added the four-tab Mining Base screen: overview, workforce, production, and connected mines.
- Added the three-tab Hunting Base screen: overview, workforce, and production.
- Added the two-tab Mine screen: overview/connection state and shared chunk deposit/output composition.
- Added shared scrollable information and workforce panels with mouse wheel and draggable scrollbars.
- Kept the player inventory and hotbar visible in all three menus.
- Replaced chat-debug right-click output with refresh-and-open menu behavior while retaining access to invalid buildings.
- Added item icons and localized hover titles to every new tab, plus English and Chinese text for all new content.
- Added initialization and overlap fields to all three building codecs so the full town snapshot can explain base workability correctly on the client.

## Decisions

- Reports are updated whenever a workable production building is invoked, including zero-production cases. Buildings skipped because they are unworkable retain their previous report as the “last settlement.”
- Mining reports use the actual storage action result for each fractional item amount. Hunting reports use the actual partial storage result for each generated item batch.
- Forecasts intentionally ignore warehouse capacity, but apply current worker eligibility, linked-mine validity, ore reserve, fractional hunting carry, and hunt-resource limits.
- Mine connection status remains separate from `MineBuilding.isBuildingWorkable`, matching the existing linking model.
- Missing legacy reports decode as empty reports. Missing legacy initialization/overlap fields decode as false, and invalid or empty legacy biome identifiers fall back safely to `minecraft:plains`.
- No incremental town synchronization or dedicated production-report packet was introduced.

## Validation

- Both localization JSON files pass `jq empty`.
- All new localization keys referenced by the screens exist in both English and Chinese.
- `git diff --check` passes.
- `./gradlew compileJava` passes.
- `./gradlew build` passes, including resource processing and jar assembly. The repository-wide license-report task continues to emit its existing non-fatal list.

## Remaining

- Visually verify text clipping, scrollbar feel, tab icons, and item-row density in game at the supported GUI scales.
- Exercise live settlements with full, partially full, and rejecting warehouses to confirm the displayed report matches player expectations.
