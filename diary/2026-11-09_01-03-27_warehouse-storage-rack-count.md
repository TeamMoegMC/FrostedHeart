# Warehouse Storage Rack Counting

- Time: `2026-11-09 01:03:27 local`
- Author: `Claude; Anthropic/claude-opus-5-5; town gameplay fix`
- Status: `completed`
- Scope: `WarehouseBlockEntity`, `TownBuildingGameTests`, `docs/town/town-model.md`; companion-pack town quest chapter;
  GameTest compile fixes in `content/world/features/deposit`

## Completed

- Warehouse storage racks now add capacity. `WarehouseBlockEntity#scanStructure` counted racks with
  `RoomData#countInsideBlock`. A rack has full collision and is not in `frostedheart:town/furniture`, so
  `LoadedTownWorld#getBlockType` classifies it as `WALL`. Wall blocks never enter the occupied cells, so every warehouse
  reported 0 racks. The scan now uses `countAllBlock` (inside plus six-way-adjacent cells), which matches how house
  decorations are already counted.
- Added GameTest `warehouseCountsStorageRacksPlacedInTheRoom` (namespace `frostedheart_buildings`). It builds a real
  5×3×5 room and places racks free-standing, in a corner, and as a two-high stack. It asserts that all 4 are counted and
  that capacity grows by more than 3×512. The existing room-scan test now shares the new `buildEnclosedRoom` helper.
- Documented the warehouse capacity formula and the rack rule in [town-model.md §21.3](../docs/town/town-model.md).
- Added the optional "仓库储物架" quest `40C8C1D93D6D8F84` to the town chapter. This resolves the remaining item
  from [the quest rewrite](2026-11-09_00-57-03_town-questbook-rewrite.md).
- `compileGameTestJava` was already failing at `fdacc2bc7`, which blocked every GameTest. Two minimal fixes:
    - `DepositGameTests`: declared `ParameterList<Holder<Biome>>` explicitly.
    - `DepositClientValidation`: restored the missing `stateCounts(int[])` helper, mapping block id to name the same way
      as `block_counts`.

## Decisions

- Kept rack walkability unchanged. The alternative was adding racks to `town/furniture`, which would make them `DECO`,
  change pathing semantics and let residents stand inside racks. Changing the counting rule alone is the smaller change.
- Existing saves need no migration: the next structure rescan picks up placed racks.

## Validation

- `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_buildings`: before the fix, 17 tests ran and only the
  new test failed (`got 0`). After the fix, all 17 passed and the build succeeded.
- The quest chapter was regenerated from HEAD with the generator script, and the static validator reports no problems (
  23 quests).

## Remaining

- `DepositClientValidation` allocates `waterOtherCounts`/`airOtherCounts` but never increments them, so those two report
  fields stay empty. The owner of the deposit work should confirm the intended counting.
