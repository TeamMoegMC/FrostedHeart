# Superflat temperature and a town that does not wait for its tower

- Time: `2026-11-10 16:23:06 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `completed`
- Scope: `content/climate/WorldTemperature`, `town/block/blockscanner/BlockScanner`, `TeamTownData.executionLevel` and its callers, GameTests for both
- Plan: [2026-11-10_16-23-06_flat-world-temperature-and-towerless-town](../plans/2026-11-10_16-23-06_flat-world-temperature-and-towerless-town.md)

## Completed

- **Superflat has no underground.** `WorldTemperature.hasNoUnderground` is true for a dimension whose chunk generator is
  `FlatLevelSource`. There, every height up to sea level reads as sea level:
  - the altitude term is 0, so there is no geothermal heat;
  - air takes the whole climate, and blocks take `blockMaximumClimateAffection`, since no rock keeps the cold out.
  - Above sea level and in every other dimension nothing changes. `BlockScanner.sampleNaturalTemperature` now uses the
    shared `WorldTemperature.blockClimateAffection` instead of its own copy of the block formula.
  - Before, the superflat ground (y = −61) sat in the band below y = −55 that adds 20 °C per block. A house's air at
    y = −60 to −58 read 100, 80 and 60 °C, 80 °C on average, which is what `town-third` showed.
- **The town no longer waits for its tower.** `TeamTownData.executionLevel` returns the buildings' dimension, or the
  tower's while there are no buildings, and no longer requires generator data. Sync, the per-second update, runtime
  repair, the citizen sim, P2P, automatic settlement and `/town tick` all run without a tower.
  - `TeamTownData.townGenerator` returns the tower only when it stands in the town's own dimension. The tower's town
    tick, directions, the settlement report's position, the tower status and refugees all use it, so a tower in another
    dimension is no tower for this town.
  - Settlement continues without a tower: houses keep only their natural temperature and no refugees arrive.
  - Debug commands: `/town tick` and `/town spawn_refugees` now say "The town has no buildings or energy tower yet"
    when the town has neither, and `spawn_refugees` explains that refugees need a tower in the town's own dimension.
- **Test world config.** Restored `institute-fresh`'s house maximum from 200 °C to the default 40 °C in both copies.
  It was raised on 11-09 to get past the superflat heat instead of fixing it.

## Decisions

- Superflat dimensions have no geothermal heat, because there is nothing below the bedrock (owner, 2026-11-10).
- Town updates are independent of the tower, because a player may dismantle it at any time; settlement continues
  without one (owner, 2026-11-10).
- Lapse above sea level stays measured from y = 63 in superflat too, so its ground reads as sea level.

## Validation

- **The GameTest world is normal terrain.** The plan's first draft said it was superflat; the first run showed
  otherwise (`level-type=minecraft:normal`, overworld `minecraft:noise`). Tests stand at y ≈ −60 inside normal
  terrain's deep band, so this change leaves their temperatures as they were. The flat rule is tested through pure
  functions (`altitude(int, boolean)`, `airClimateAffection`, `blockClimateAffection(int, boolean)`) and detection on a
  real `FlatLevelSource`.
- **Town suites:** 62 pass on their own (the nine suites of earlier rounds plus the new tower-less sync test).
  - `townWithoutTowerStillSendsItsChanges` lets the server's own town update deliver a building placed with no tower,
    then a tower formed later and its dismantling, to two connections. The old gate never ticked such a town.
  - `towerInAnotherDimensionLeavesTheTownRunning` and `generatorRemovalLeavesTheTownRunning` replace the two tests that
    asserted the pause.
- **Default development set:** 91 tests, 6 failing, none touching towns. A first combined run that also held the
  persistence suites failed differently, because `save-off` is server-wide; the groups now run in separate launches.
  - Four fail the same way with the old tower gate restored through a temporary switch, so they predate this change:
    `waterInteriorWaitsForAnEdge`, `sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift`,
    `warmedMaterialRemainsVisibleAfterTheRuntimeCloses`, `campfireSurfaceUsesMaterialAndLocalAvailability`.
  - Two timing tests, `placedReservoirsReceiveTheDroppedItemRadiationBoundary` and
    `sixteenFiveBlockChainsFallWithoutDuplicateEntities`, failed in one run with this change and passed in another,
    and passed in the comparison run.
  - A final run on the final code failed exactly the four that fail with the old gate; both timing tests passed.
  - In normal terrain the new temperature functions reduce to the old arithmetic, and the GameTest server has no
    superflat dimension, so the temperature change cannot move these results.
- **In game, `town-third` only** (the owner's choice), single-player client:
  - A ninth house placed with no tower appeared on the seal within two seconds: 「全镇 9 座建筑」. Before, the client
    kept the first building only.
  - Every house read 「室温 −10°C 太冷」 with heavy frost. The first house had read 80 °C 太热; the new house started cold.
  - Forming the tower needed `/research complete all` first: frostedresearch cancels multiblock formation until the
    team has the technology, which is normal survival behaviour. Once formed, the house header gained 「镇东」 and the
    tower card read 「关着 · 完好」 without reopening anything.
  - Breaking the tower's trigger block dismantled it; the direction disappeared and the card read 「镇上还没有能量塔」.
  - `/town tick` without a tower advanced the town to 第 1 日. The debug resident command added nobody, because every
    house was below 0 °C and unfit.

## Remaining

- Wording for the tower-less town, left for the UI pass: after a dismantle 「镇上还没有能量塔」 reads as "not yet", and the
  empty-town note still says 「留意镇外的难民」 although refugees only come to a town with a working tower.
- The research-complete toasts cover the board's right edge (the reviewer's point about tips over the board).
- The four thermal GameTests that already failed before this change.
