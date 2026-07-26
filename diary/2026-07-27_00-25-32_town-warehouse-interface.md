# Town warehouse interface implementation

- Time: `2026-07-27 00:25:32 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `AGENTS.md`, `diary/`, the town warehouse package, and warehouse-interface resources

## Completed

- Established the shared development-diary format and made diary reading/writing part of the repository instructions for future contributors and agents.
- Added a nine-slot warehouse interface with nine item/NBT-exact target settings, automatic excess storage, and automatic restocking.
- Added wall-scan discovery, warehouse binding/unbinding lifecycle checks, item capability exposure, menu, CUI screen, registration, persistence, and resource synchronization.
- Added models, language fragments, generated loot/tag/lang data, and changed the outward face to the `smoke_block_t1` top texture.

## Decisions

- The interface is a block entity bound to an existing `WarehouseBuilding`, not a new town building type; inventory exchange uses the existing town resource actions.
- Handwritten GUI translations live under `src/main/resources`; block language, tags, and loot are produced by the existing Registrate datagen chain.
- Only the outward model face uses `frostedheart:block/smoke_dispenser_i_top`; the other five faces keep the warehouse wall texture.

## Validation

- User in-game testing found no logic issues.
- `./gradlew runData` completed successfully and produced only the expected warehouse-interface data.
- `./gradlew build` completed successfully after the resource and texture changes. The repository still reports its pre-existing non-fatal license warnings.

## Remaining

- Final crafting recipe and final artwork are intentionally undecided.
- The implementation and this diary entry are currently uncommitted.
