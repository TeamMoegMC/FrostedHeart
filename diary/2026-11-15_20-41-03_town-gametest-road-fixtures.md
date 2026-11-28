# Town: GameTest fixtures link their buildings for the road rule

- Time: `2026-11-15 20:41:03 -0600`
- Author: `Claude (Anthropic Opus 5.5)`
- Status: `tests only; no game code changed`
- Scope: `src/gametest/java/com/teammoeg/frostedheart/content/town/` — `TeamTownPersistenceGameTests`,
  `TownBuildingGameTests`, `ResearchInstituteGameTests`, `TownSchedulingGameTests`, `TownViewSyncGameTests`
- Follows: [town road connectivity](2026-11-13_04-00-00_town-road-connectivity.md)

## Why

Since road connectivity, `AbstractTownBuilding.isBuildingWorkable()` also needs `connectivityWorkable`, which only the
asynchronous `TownConnectivity` scan sets, driven from `TeamTownData.tick`. Fifteen GameTests in
`frostedheart_buildings`, `frostedheart_institute`, `frostedheart_persistence`, `frostedheart_town_scheduling` and
`frostedheart_town_view` built a building and expected it to work right after its room scan; all fifteen failed on
master `fef357626`.

## Completed

- Fixtures that are not about roads and are checked synchronously mark their buildings linked, as the scan would:
  the `populatedTown` house (four persistence tests and three building tests), the recovery test's replacement house,
  the real-room house, both institutes, and a house reloaded from a save, which never carries road state (as
  `LaborDispatchGameTests` already did).
- Tests that run live town ticks wait for the real scan: the lamp-light institute test waits in `succeedWhen` until
  `connectivityView().view().working()` holds the institute, and the two-player menu test waits until it holds the
  house and the warehouse before moving stock.
- `TownViewSyncGameTests.Session` lays a stone walk along the door side (x 3 to 23, z -1 from its origin), so its
  house, warehouse and extra room are really linked by road. `linkAll()` covers its synchronous steps and
  `scanWorks(...)` reads the scan's verdict. `TownSchedulingGameTests.populate` links its three buildings.
- `loadedCoreWorkloadsReportCaptureAndSaveCosts` removes each measured town before its first tick.
- `realRoomScanTracksDoorBedAndStableRepeat` judges habitability at a controlled 20 °C and puts the sampled
  temperature back before its unchanged-rescan check.

## Findings

- Scan starvation. The workload towns (up to 500 houses at x 1620 to 2600, z 1600) sit in chunks that unload once the
  test ends. There `TownSectionSnapshots.copySection` returns null, so every lookup gets a one-tick MISSING snapshot and
  asks again, with two sections served per tick. Those scans held both `town-connectivity` workers (both parked in
  `CompletableFuture.timedGet`) for the rest of the default batch. The institute's one-building scan, which reads no
  section at all, never got a thread in 200 ticks, and the view session's warehouse stayed unlinked, so its first
  deposit threw an `AssertionError` that crashed the GameTest server.
- Climate. Since `2f18c0265` an enclosed stone room at y ≈ -58 in the GameTest world samples about -6.4 °C, below
  `HOUSING_MINIMUM_TEMPERATURE_CELSIUS` (0 °C), so the real-room house failed on temperature as well as on roads.

## Validation

- `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_buildings,frostedheart_institute,frostedheart_persistence,frostedheart_town_scheduling,frostedheart_town_view -PgameTestDirectory=build/town-fix-gametest`:
  on `fef357626`, 15 of 45 required tests failed; with this change all 45 passed in two consecutive runs.
- With temporary logging (removed): the session's road scan linked the house and the warehouse within 4 ticks of
  `warm()`, kept both through the extra building's placement and removal, and the institute's scan linked it within
  20 ticks.

## Remaining

- Game code, not changed here: a scan over unloaded chunks can hold one of the two shared workers for a long time, so
  two such towns delay every other town's scan. Any building change unlinks the whole town until its next scan
  finishes, so such a delay keeps that town offline. A cancelled scan, normal after any building change, is logged as
  `ERROR Town connectivity scan failed`.
- From reading `TownAssignments.reconcile`, not exercised by a test: a roster restored from retained data keeps
  residents only while `isBuildingWorkable()`, which is always false at load because road links are not saved.
- The opt-in client drivers `TownViewClientValidation` and `TownSchedulingClientValidation` share `Session` and now
  show the walk; they were not run.

## Documentation

- No living document changed: game behavior is unchanged.
