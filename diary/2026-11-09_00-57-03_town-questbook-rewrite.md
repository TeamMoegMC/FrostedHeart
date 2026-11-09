# Town Quest Chapter Rewrite

- Time: `2026-11-09 00:57:03 local`
- Author: `Claude; Anthropic/claude-opus-5-5; quest content`
- Status: `completed`
- Scope: companion pack `config/ftbquests/quests/chapters/building_town_expansion.snbt`,
  `kubejs/assets/twr_quests/lang/{zh_cn,en_us}.json`

## Completed

- Rewrote the town chapter (`6E4D174C980DB004`) as a newcomer guide. The town unlocks after the Tier One Generator
  quest (`69D939596ED4C0AF`), runs on its own, and produces basic resources. The chapter has 22 quests.
- Path through the chapter: building rules → warehouse/house → recruit → survival → workforce dispatch →
  mining/hunting → transport station → warehouse interface → automatic coal supply and automatic cooking →
  self-sufficiency capstone. Side quests cover comfort, food levels and nutrition, attributes, residential care and
  laws, level emitter, P2P terminals, and the repeatable Mayor's Seal.
- All 12 previous quest IDs and their task and reward IDs are reused, so existing progress is preserved. The
  end-of-mainline marker `2E165BB1FB0CE6A0` was kept unchanged and only repositioned.
- All text now lives in lang keys, matching the other chapters. Only literal blank lines and `{@pagebreak}` stay in the
  SNBT. The chapter subtitle was updated.

## Decisions

- Every number in the text is a current source default: refugee batches, house 0–40 °C and comfort 24 °C, 20 food units
  per resident-day, mining connection radius 1024, 1000 ore per chunk, T1 at about 21 coal/day, and 64 transport
  capacity per worker. See [town-model.md](../docs/town/town-model.md) and `TownModelParameters.Defaults`.
- The coal quest says plainly that `GeneratorData.consumesFuel` only reads the tower input slot. The warehouse is not
  drained automatically, so players must route coal through a warehouse interface.
- The hunting and capstone quests state that raw-meat hunting cannot feed a resident, and that early towns need farm
  food. This follows the current 20-unit demand.
- The warehouse storage rack is not taught. `WarehouseStorageRackBlock` has full collision and is not in
  `frostedheart:town/furniture`, so `LoadedTownWorld#getBlockType` treats it as `WALL`. It therefore never enters the
  room's occupied cells, and `WarehouseBlockEntity#scanStructure` always counts 0 racks.

- Hand-written FTB Quests IDs must be positive hex longs, with a first digit of 0–7. The first generated batch contained
  19 IDs at or above `8000000000000000`. FTB Quests reassigned them on the first client load and dropped every
  dependency that pointed at them. The chapter was regenerated with the IDs FTB Quests had assigned and the dependencies
  restored, then committed to the companion pack as `aff763a7`.

## Validation

- A custom SNBT parser parsed all 15 chapter files. In the town chapter, every `{quest.twr...}` key exists in both lang
  files, no town key is orphaned, IDs are unique across chapters, dependencies resolve, item IDs are registered, and
  quest positions do not overlap.
- Not checked in game; the quest book still needs to be opened in a client.

## Remaining

- Storage racks add no warehouse capacity (see Decisions). Fix in code or data, then add a quest.
- A stray untitled warehouse quest `2D89D6FAE456A35A` remains in `a_day_of_generator_engineer.snbt`.
- The text of `2E165BB1FB0CE6A0` still says town building is coming in a future update.
- `tip.snbt` quest `52A2EE5B2340CF54` references an undefined `refugees` tip.
