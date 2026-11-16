# Town: road scans keep links while rescanning and judge only loaded terrain

- Time: `2026-11-15 22:37:22 -0600`
- Author: `Claude (Anthropic Opus 5.5); task approved by the project owner`
- Status: `implemented; GameTests pass`
- Scope: `TownConnectivity`, `TownSectionSnapshots.isLoaded`, two new GameTests, `docs/town/building-data.md`
- Follows: [town GameTest fixtures for the road rule](2026-11-15_20-41-03_town-gametest-road-fixtures.md)

## Why

Two problems found while fixing the town GameTest fixtures:

- Any change to a town's buildings, doors or tower unlinked every building in the town until the next scan's result
  arrived. During that window warehouse capacity was 0, institutes and labor dispatch gave nothing, and a morning
  settlement that landed in it treated the whole town as not working.
- Unloaded chunks read as walls (`SectionSnapshotCache.MISSING` is filled with `WALL`, as documented). A town nobody was
  near was still rescanned. It found no links, and once that scan finished only one of its buildings kept working,
  including at its morning settlement while a member was online elsewhere. Unloaded sections are not cached, so each
  search start of such a scan waited in the shared two-per-tick queue again. Two such towns held both
  `town-connectivity` threads, and every other town's scan waited behind them.

Correction to what I told the owner earlier today (not recorded elsewhere): unloaded terrain is not "climbable
everywhere"; I read the zero-filled array and missed the static `WALL` fill.

## Completed

- A building, door or tower change cancels the running scan and starts a rescan, but keeps every building's links until
  the result arrives. Removed buildings drop out of the published view, together with routes that end at them or at a
  former tower. New buildings stay unlinked until a scan includes them. Only a dimension change clears all links.
- A scan only covers buildings, and a tower, whose chunks are loaded: those under the controller's surrounding 5x5
  square and those of its declared entrances. With none loaded the town is not scanned and keeps its links; it looks
  again after `UNLOADED_RETRY_TICKS` (100).
- A scan that left out any building or the tower can only add links (`TownConnectivity.grant`). A scanned building
  works once its part of the network holds a working building or the tower. A town with no working building yet takes
  the rule's verdict on what was seen. Routes among scanned nodes are replaced; the rest keep theirs.
- A result is dropped if the scanned chunks unloaded while it ran.
- Within one scan, a section found unloaded is not requested again (`Batch.missing`).
- A cancelled scan no longer logs `ERROR Town connectivity scan failed`.

## Decisions

- Grant-only for partial scans: partial terrain cannot show that a link is gone, and a town with one building out of
  range must still link new buildings at home. Cost: such a town notices a removed road only when all of its buildings
  are loaded at once. Recorded in the docs as a known limitation.
- Links are still not saved (the original design). After a restart a town nobody is near stays without links until
  someone comes near; before this change its first scan unlinked it anyway. Saving the last result would close this;
  left for the owner to decide.

## Validation

- `TownViewSyncGameTests.rescanKeepsTheLinkedWarehouseWorking`: before the fix it fails with "a rescan must not unlink the
  warehouse while it runs" when run on its own.
- `TownBuildingGameTests.unloadedTownsKeepTheirLinksAndLeaveTheScanThreadsFree`: two 150-house towns at x 8000/8512,
  z 8000 lose their chunks and then change; a one-building town must get its first scan within 40 ticks, and the far
  towns must keep their links. Before the fix: "a one-building town waited over 40 ticks behind unloaded towns' scans".
- The GameTest world has random terrain at y 65, so that test clears the space above its floor. Without that, its far
  towns sometimes sat inside hills and could not link, which first looked like a scan bug.
- `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_town_connectivity,frostedheart_buildings,frostedheart_institute,frostedheart_persistence,frostedheart_town_scheduling,frostedheart_town_view,frostedheart_citizen_roads,frostedheart_labor_dispatch -PgameTestDirectory=build/scan-fix-gametest`:
  all 76 required tests pass; with the old `TownConnectivity` the two new tests fail.
- Not validated in a real game or on a multi-team server.

## Remaining

- Coordinate with the uncommitted main-checkout work that takes mines off the road network (`TownConnectivity.onRoads`,
  `requiresRoad()`); both touch `TownConnectivity.tick`.
- A removed town's scan already running is not cancelled; with this change such scans are short.
- From reading `TownAssignments.reconcile` (previous entry): a roster restored from retained data is emptied at load
  because links are unknown then; its residents return at the next reconcile via their pending intent.

## Documentation

- `docs/town/building-data.md`, section "道路连接与能量塔": rescans keep links, loaded-terrain-only scans, grant-only
  partial scans and their limitation, no error for cancelled scans; the section snapshot notes describe `Batch.missing`.
