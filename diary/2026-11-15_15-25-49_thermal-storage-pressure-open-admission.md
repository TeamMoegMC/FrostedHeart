# Thermal: an admission waiting for memory no longer stops neighbouring bases

- Time: `2026-11-15 15:25:49 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `implemented; broad GameTests show only the six base-commit failures; final 100-player run (r9) matches master freshness`
- Scope: `ThermalDimensionEngine`, `ThermalResidencyTimeGameTests`, `ThermalNetworkLoadServer` (fixture),
  `docs/climate/thermal-runtime-control.md`
- Follows: [r4 with bounded scout trips](2026-11-15_13-01-58_thermal-load-r4-scout-sites.md), which left the master
  control run and the recovery tail open

## Master control (r5)

Master `37adbc659` with the fixture files copied in, the same r4 setup (a copy of r3's world, `scoutSites=8`, 100
players, 400 s measured plus 30 s recovery with scouts returned home, 8 GiB heap, 5 processors):

| | r4 branch | r5 master |
|---|---|---|
| TPS | 18.93 | 18.84 |
| server tick p50 / p95 / p99 ms | 26.1 / 38.5 / 58.3 | 26.1 / 39.2 / 59.3 |
| thermal main mean / p99 / max ms | 0.65 / 2.86 / 25.2 | 0.80 / 3.07 / 26.5 |
| admission refusals, accounted peak | 149, 139.1 MB | 143, 140.1 MB |
| unavailable resident samples | 294 / 34306 | 498 / 34102 |
| recovery missing / old; tail missing / old, tail max age | 160 / 100; 90 / 70, 443 | 3 / 0; 0 / 0, 3 |

The late refusals are not from the branch. The recovery tail was branch-only in this pair.

## Finding the stall

- r4's saved world: each base Section's checkpoint carries the tick its Tile last advanced to. Bases 3 and 4 stopped
  at 21380 and 21480 and one chunk of base 5 at 21460; their scouts had come home at 21223. Every other base reached
  about 21900, and in r5's world all 25 did.
- The fixture got a recovery stall dump (`recovery-stall.jsonl`, below). r6, the r4 setup again with the lifecycle
  trace and the dump, reproduced it and worse: stale samples from about measured tick 7000 (981 age violations, 1294
  refusals, accounted peak 142.5 MB); recovery 406 missing, 545 old, never all fresh.
- The dumps show one `FixedGeometry` at stage 1 with `capacityWait` equal to the current capacity revision, target tick
  21260, boundary 748 Sections, from recovery tick 60 to the end. Its inputs were admissions of new Sections around
  the bases (heat through the walls) and brick growth. Every Section of the stalled bases sat at 21260 with a physical
  cut at 21260; the batch retried at each capacity change and was refused each time.
- Cause: admitting a Section the field did not represent was a physical input. It cut its whole boundary at its target
  tick, and with memory full the batch waited and held every base in that boundary. `thermal-runtime-control.md`
  already said pure expansion waits without stopping existing space; brick growth did, admissions did not. The wait
  logic is master's own code; the branch's extra `natural` float per cell makes the budget limit somewhat more likely.

## Completed

- `fixedOpenAdmission`: an admission of a Section with neither a Tile nor an installed Page is storage, like brick
  growth (`fixedStorageOnly`). It makes no physical cut, its application tick floats to its boundary's first aligned
  tick, and its Page queue keeps later inputs behind it.
- `fixedStorageCuts`: a waiting admission still bounds its own Section at its admission tick, through
  `geometryBoundary` only (source binding and routing). A source in a Section that does not exist yet keeps its J in
  the producer instead of piling up in the input store; numerical time is not bounded.
- Storage and physical changes never share a batch. Selection order is maintenance, physical, storage. A storage batch
  refused for memory waits in the geometry slot (a retry is one reservation), cuts nothing, and yields to maintenance and
  to any ready physical change (`fixedGeometryYields`).
- Storage pressure (`fixedStorageWait`): after a refusal, new storage batches hold one input and form only after capacity
  changed; the first storage batch that pays ends it. Skipped storage registers a capacity wait for an idle coordinator.
- `FixedGeometry.storageAligned`: storage re-forms instead of installing new Tiles at a tick its boundary has left.
- Diagnostics: `regionalProgressDescription` adds `storagePressure`, `storageCuts` and a pending input's `storage` flag.
- GameTests `ThermalResidencyTimeGameTests.anUnpaidNeighbourAdmissionLeavesTheAdjacentBaseOnItsClock` and
  `anEditInsideAWaitingAdmissionIsKeptBehindIt`: a warm base Brick at a Section face, its neighbour admitted with memory
  held, a wall edited in the same cut, and in the second an edit inside the waiting Section.
- Fixture: `recovery-stall.jsonl` rows with the main-thread view (stale or missing players, their `describeAirRead`,
  frozen state, chunk load and dormant ticks) and the worker view copied at a round end (engine clocks, geometry task,
  snapshot and restore queues, watched Sections with cuts and activation blockers, domains, pending inputs with the
  boundary Sections that block them). Up to 12 dumps, at most 6 from the measured phase; `recovery.jsonl` frames gain
  `missingByBase` and `oldByBase`.

## Validation

- The two new GameTests fail on the old engine exactly like the load test ("expected=60,actual=40", the base held at
  the admission's tick) and pass with the fix.
- First version (return every refused storage batch, a lower-bound affordability check): the broad set had 11 failures,
  the six known and five new. Two were energy: without the old cut, a source inside the waiting Section bound early and
  its J sat in the input store (`TiledThermalSaturationGameTests`). Three pause a geometry task at stage 1 through a
  capacity wait (`oldEditsAndTheWaitingPagesOwnRetirementFinishAtTheMemoryTarget`, two `TiledThermalEngineGameTests`
  unload tests). The own-Section cut and waiting in the slot fixed all five.
- `frostedheart_tiled_saturation`, `frostedheart_tiled_engine`, `frostedheart_residency_time`, `frostedheart_geometry_cut`:
  30/30 on the final code.
- Broad set (`thermal-runtime-namespaces.txt` plus default, gameplay balance, integration, ice cave, icicle, hot spring,
  thin ice, climate cover; 285 tests), on the second version and again on the final code: 6 failed each time, the six
  that fail on the base commit
  (`waterInteriorWaitsForAnEdge`, `warmedMaterialRemainsVisibleAfterTheRuntimeCloses`,
  `sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift`, `campfireSurfaceUsesMaterialAndLocalAvailability`,
  `completeReloadRebuildsMaterialProfilesAfterTagsBind`, `loadedCampfiresRecoverAndLaterMutationSurvivesReload`).
- 100-player runs, all the r4 setup with the lifecycle trace except r4/r5:

| | r4 | r5 master | r6 unfixed | r7 | r8 | r9 final |
|---|---|---|---|---|---|---|
| TPS | 18.93 | 18.84 | 18.77 | 18.83 | 18.82 | 18.59 |
| thermal main mean / p99 / max ms | 0.65 / 2.86 / 25.2 | 0.80 / 3.07 / 26.5 | 0.67 / 2.96 / 14.7 | 0.62 / 2.65 / 12.1 | 0.63 / 2.79 / 12.0 | 0.63 / 2.87 / 17.9 |
| measured age violations, max age | 0, 23 | 0, 23 | 981, 423 | 168, 63 | 66, 43 | 0, 23 |
| recovery tail missing / old, max age | 90 / 70, 443 | 0 / 0, 3 | 180 / 180, 563 | 0 / 0, 3 | 0 / 0, 3 | 1 / 0, 3 |
| refusals | 149 | 143 | 1294 | 479 | 633 | 589 |

  r7 ran the second version (refused batches with physical changes returned and re-formed). Its 168 stale samples were
  one 200-tick window at the memory-pressure onset, with coordinator rounds nearly halved at flat worker CPU: the
  re-formed physical batch was planned twice. Keeping storage and physical changes apart (r8) removed that path. r8's
  66 stale samples are two windows at scout departures under pressure (measured ticks 7200–7400 and 7800–8000), at most
  3 ticks over the 40-tick limit; its dumps show no held structure, only the worker two cuts behind and catching up.
  The final code also orders the storage scan so the cheap storage and capacity checks run before the boundary reads.
  r9 ran exactly that code: no age violations, max age 23 as on master; one missing sample in the recovery tail (master
  0), first all fresh after 120 ticks as on master. Its lower TPS is outside the thermal code: thermal main time and
  worker CPU match r8, while the mean tick is 0.8 ms longer, main CPU 1.2 % higher and "Can't keep up" 16 against 13.
  Refusals count every cheap retry of a waiting batch.
- r9's bots exited 1 after the run had completed and qualified (100 joined, no disconnects): a socket end event wrote
  to their closed event log (`ERR_STREAM_WRITE_AFTER_END` at `Scripts/thermal-network-clients.cjs:41`). A shutdown race
  in the bot script; the results are unaffected.

## Documentation

- `docs/climate/thermal-runtime-control.md`: new subsection "扩容与内存压力" under input, geometry and physical time;
  last-verified line updated.

## Remaining

- One run each: r8 lagged two cuts under memory pressure during scout departures (66 of 34001 samples, at most 43
  ticks), r9 did not. Storage still re-forms and is planned again after each yield. A lower-bound check
  before re-forming would cut futile planning, but `TiledThermalEngineGameTests.unloadKeepsTheAlreadyPreparedOrderedGameplayAndColdMassResult`
  pauses its growth task at stage 1 by letting it re-form at once after a backing reconciliation returns it, so that
  test would need another pause.
- A physical input that needs new storage (a lifecycle replacement with new Bricks) still cuts and waits as before.
- A source in a neighbouring Section whose port reaches into a waiting Section is held by that Section's storage cut,
  as it was by the old full cut.
- After a yield under pressure, a new base of several Sections can enter one Section at a time.
- Dormant air and material cuts inside an admission are values at its target tick; a late installation does not cool
  them further.
- `Scripts/thermal-network-clients.cjs` can crash at shutdown (above); the event writer should ignore events after the
  log closes.
