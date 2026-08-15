# Immediate town-building unregistration

- Time: `2026-08-15 13:23:01 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent /root`
- Status: `completed`
- Scope: `town building lifecycle, resident/mine/warehouse cleanup, warehouse access guards, tests and documentation`

## Completed

- Added server-side active-removal unregistration to `AbstractTownBuildingBlockEntity`; chunk unloads keep their mapping, while stale block entities cannot recover or delete a live replacement.
- Made `TeamTown.removeTownBlock` the idempotent cleanup path for active teardown, same-position replacement, and morning reconciliation. It now clears all resident house/work references to the removed position without reallocating, prunes mine links and derived ore coverage, recomputes warehouse capacity, and recalculates both true and false overlap flags.
- Kept warehouse inventory and `occupiedCapacity` as town-level state. Removing warehouse capacity can leave the town over capacity: new additions are rejected while existing stock remains consumable.
- Unbound loaded warehouse interfaces and level emitters before removal so their watchers are released. Unloaded devices remain inert through their invalid saved mapping.
- Guarded stale warehouse menus and C2S inventory interactions with the live core block entity and building mapping.
- Added `TownBuildingRemovalTest` coverage for residents, mine links, idempotence, warehouse over-capacity behavior, overlap cleanup, and same-position replacement.

## Decisions

- No pending-warehouse store, save Codec migration, packet-format change, or companion-modpack change is needed; warehouse stock is already independent from individual `WarehouseBuilding` instances.
- Resident records survive removal, but their affected position is cleared immediately. Housing and work assignment remain morning-only.
- `tickMorning.checkBlocks` remains a fallback for abnormal edits and save inconsistencies, but delegates cleanup to the same path as active removal.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.TownBuildingRemovalTest` — successful (5 tests).
- `./gradlew test` — full suite successful; Java compilation completed with 20 existing warnings.
- `git diff --check` — successful after the final code, documentation, and diary updates.

## Remaining

- Recommended in-game smoke test: break/replace/explode every town core while the town-manager or warehouse menu is open, then repeat across chunk unload/reload and command replacement to confirm immediate visual synchronization and device behavior in a real server world.
