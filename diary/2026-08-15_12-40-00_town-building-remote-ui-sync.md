# Town building UI synchronization on dedicated servers

- Time: `2026-08-15 12:40:00 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent /root`
- Status: `completed`
- Scope: `town client data source, house/mining/hunting/warehouse menus, synchronization tests and documentation`

## Completed

- Made `AbstractTownBuildingBlockEntity#getTown()` choose the synchronized `CClientTeamDataManager` snapshot on the logical client while retaining serialized-provider lookup on the logical server.
- Routed house, mining-base, mine, and hunting-base menu town/resident queries through the block entity's canonical town accessor.
- Removed Warehouse building metadata from vanilla `ContainerData`; capacity, dimensions, and status now read the same live town-building snapshot as the other building GUIs. The dedicated warehouse item-list packet remains unchanged.
- Added a dedicated-client-topology regression test covering all five building types, full-snapshot replacement, and warehouse capacities of 22,000 and 44,000.

## Decisions

- Client-side missing data before the first full snapshot is treated as a transient empty result and does not log warnings or run the server-only missing-building recovery path.
- No save Codec, incremental packet, channel registration, gameplay calculation, or companion-modpack data was changed.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.ClientTownDataSourceTest` — successful.
- `./gradlew test` — full suite successful; Java compilation completed as part of the run with only existing warnings.
- `git diff --check` — successful after the final documentation and diary additions.

## Remaining

- Smoke-test from a real remote client against a dedicated server: open house, mining base, hunting base, mine, and warehouse; compare warehouse capacity with `/town resources`; then observe an incremental building/resident update while a GUI remains open.
