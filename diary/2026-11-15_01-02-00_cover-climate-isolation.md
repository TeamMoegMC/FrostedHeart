# Climate isolation by cover, and a per-block natural background for the thermal field

- Time: `2026-11-15 01:02:00 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `implemented and GameTested in worktree branch claude/cover-climate-isolation, now on master 37adbc659; not committed; not checked in game`
- Scope: `content/climate/WorldTemperature`, `BlockTemperatureModel`, `ClimateCover*`, `gamedata/climate/WorldClimate`,
  `CoveredClimateHistory`, `FHConfig.SERVER.CLIMATE`, `mixin/minecraft/thermal/LevelChunkMixin_ClimateCover`,
  `thermal/solver/TiledThermalField`, `NaturalProfile(s)`, `ThermalExchangeKernel`, `MinecraftEnvironmentCapture`,
  `MinecraftThermalInput`, `ThermalDimensionEngine`, `WorkerPageStore`, dormant persistence classes, `BlockScanner`

## Why

The owner reported, in a cave room at y = −58: the temperature orb read 30 °C; infrared showed blue everywhere; after
lighting a campfire the infrared turned yellow nearby and the orb dropped to about 10 °C inside the yellow area and went
back to 30 °C outside it; the soil thermometer read about 10 °C in the yellow area and "no material data" in the blue.

- The 30 °C came from the old altitude function: a band below y = −55 added 20 °C per block. It also jumped at
  y = −55 and y = 240/241.
- The jump between yellow and blue: the thermal field held every cell of a Section at one natural value taken at the
  Section centre (about 10 °C there), while outside the field each position used its own natural (30 °C).
- Blue infrared is the shader's colour for "no data" (−20 °C). `material()` is NaN without live or stored material
  state by design. Neither was changed here.

The owner then decided the model: drop the geothermal band; use realistic long-term gradients (−0.6 °C per 100 blocks
above y = 64, +3 °C per 100 blocks below); the −10 °C dimension baseline is the world's long-term mean; the real
advantage underground is isolation from the climate, made by real opaque cover with a time lag
τ(c) = τ10 · (c/10)², τ10 = 3 days by default.

## Completed

Natural model (phase A):

- `WorldTemperature.altitude` is one continuous line with a bend at `ALTITUDE_REFERENCE_LEVEL = 64`:
  `-0.006 °C/block` above, `+0.03 °C/block` below. Superflat (`hasNoUnderground`) reads 0 at and below 64.
  The stratosphere, sea-level and lava bands, the build-height clamps and `airClimateAffection`/`blockClimateAffection`
  are gone.
- Cover: `ClimateCover.coverAbove` counts opaque full blocks (`isSolidRender`) above a position, up to
  `coverMaximumBlocks` (16). `ClimateCoverColumns` caches each column's top-down heights per chunk;
  `LevelChunkMixin_ClimateCover` drops a column when an opacity change can alter its cover.
- Lag: `CoveredClimateHistory` records the hourly global climate in `WorldClimate.updateCache` and returns a normalized
  exponential low pass with τ(c) over `ceil(4τ)+1` hours. Local values follow `getTemp`: `min(global, curtain)`, or
  `min(global, 0)` without a curtain. The history is saved in `WorldClimate` NBT (`coveredClimate`), not in packets.
- Config: `coverResponseDaysAtTenBlocks = 3.0` and `coverMaximumBlocks = 16` replace `climateStoneInterfaceLevel` and
  `climateSeaLevel`. `blockMaximumClimateAffection` (0.5) now applies flat to blocks at every height and cover.
- `naturalAir = D + B + A + C_c` and `naturalBlock = D + B + A + 0.5 · C_c`, where `C_c` is the covered climate.
  `BlockScanner.sampleNaturalTemperature` uses the same pieces. `BlockTemperatureModel.climateBlockAffection` and
  `blockTemperature` were removed.

Thermal field (phase B):

- The main thread publishes a `NaturalProfile` for every tracked Section (Brick base `D + B`, per-block cover, covered
  climate by cover at the Section centre), through `NaturalProfiles` to the worker. Climate tables are republished when
  any cover level moves by 0.05 °C (checked on the existing 200-tick refresh); cover columns are refreshed from the
  mixin, at most 64 per tick.
- `TiledThermalField` keeps a float `natural[cell]` per Tile cell. Every exchange (air pairs, routes, material pairs via
  a new `backgroundOffsetC` argument of `ThermalExchangeKernel.exchangeMaterialsInto`, FarField and environment baths),
  every initialization, the query reconstruction and the residency test now work on departures from each cell's own
  background. At rest every cell sits on its own background and neighbours with different backgrounds exchange nothing.
- A new profile takes effect at the next `begin` (`refreshNaturalProfiles` → `shiftToNatural`): each cell keeps its
  departure and moves with the background; the energy is booked as external energy.
- Persistence: capture into chunk data keeps only Bricks and bodies that depart from their own block's natural
  (`AirFieldCheckpoint.retain(long, IntToDoubleFunction)`, `MaterialSectionState.retain(long, long,
  IntToDoubleFunction)`); dormant restore relaxes air and bodies toward each cell's natural
  (`DormantAirCut.cellTemperatureC(brick, block, naturalC)`, `TiledThermalField.naturalC(position)`); every dormant
  reading relaxes toward the block's `naturalAir`: air queries, the thermometer and phase controller
  (`projectMaterial`), infrared (`InfraredCapture.readStoredPage`) and the cooling before a dormant block change.
  `SectionSnapshot.naturalC` defaults to NaN so hand-built snapshots fall back to `ambientC`.

## Decisions

- Glass, leaves, ice, liquids and air do not count as cover. A pit with a glass roof gets the open climate, which is
  the exploit the owner wanted closed.
- The low pass is normalized over the hours actually recorded, so a new or upgraded world needs no special start state:
  it reads the current climate until history accumulates.
- Covered climate in the field profile is sampled once per Section (its centre's chunk and curtain); cover and altitude
  are per block and the biome per Brick. This keeps a profile at 64 + 4096 + 17 values and the refresh cheap.
- Material cells keep using the air natural as their background, as before. Inside a resident Page the passive
  `block()` query therefore still differs from `naturalBlock` outside by `(1 − 0.5) · C_c` at rest. Changing `block()`
  was offered earlier and not confirmed, so it is unchanged.
- Without a white curtain the local climate is `min(global, 0)`, copied from `getTemp`, so cover 0 equals `getTemp`.
  This means positive global climate (warm events peak at +8 °C) is capped at 0 °C everywhere. That cap predates this
  work and was left alone.
- Dormant paths that still use the Section natural were kept on purpose to limit the change: maintenance ageing,
  admission filtering, edits after a checkpoint, legacy Brick means. They are listed in the runtime document.

## Validation

- `./gradlew compileJava compileGameTestJava --offline`: passes (only existing deprecation warnings).
- `frostedheart_climate_cover` (new, 8 tests, `ClimateCoverGameTests` and `NaturalBackgroundFieldGameTests`): 8/8
  passed. They cover the composition of the natural formula, continuity of the altitude line, superflat, cover counting
  and edits, damping of short versus lasting climate, save/load of the history, no flow at rest between different
  backgrounds (energy unchanged to 1e-3 J over 20 steps), and a −15 °C background change reaching every cell with the
  energy booked as external.
- Broad regression with phase A only (`thermal-runtime-namespaces.txt` plus `frostedheart`, gameplay balance,
  integration, ice cave, icicle, hot spring, thin ice and climate cover; one launch, 277 tests): 6 failed. Four fail the
  same way on earlier code per the 2026-11-09 and 2026-11-10 diaries: `waterInteriorWaitsForAnEdge`,
  `sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift`, `warmedMaterialRemainsVisibleAfterTheRuntimeCloses`,
  `campfireSurfaceUsesMaterialAndLocalAvailability`. The other two are full-reload tests in
  `ThermalLoadedWorldGameTests`: `completeReloadRebuildsMaterialProfilesAfterTagsBind` and
  `loadedCampfiresRecoverAndLaterMutationSurvivesReload`.
- Broad regression with phase B (same set, 279 tests): 7 failed, the six above plus
  `elapsedTimeRefreshesInfraredWithoutEditingStoredEnergy` ("IR and thermometer share projected material temperature").
  Phase B caused it: the thermometer projected a dormant body toward its block's natural while infrared still used the
  Section value. Infrared and the cooling before a dormant block change now use the block's natural too.
- Default namespaces plus climate cover, after that fix (98 tests): 6 failed, the four known ones and the two reload
  tests; the infrared test passes. So the reload tests do not fail only because of the one-launch broad set. This run
  (about 01:13–01:17) overlapped the first minutes of another session's thermal load test (100 players, 25 bases)
  started from the main checkout, and may have disturbed that test's timings.
- Same default namespaces on the base commit `7aa1d82b9` (exported with `git archive` into `build/`, no changes;
  92 tests, including the two natural-formula tests this work removed): the same 6 failed. The two reload tests
  therefore fail before this work. Reading the code: after a full reload `MinecraftThermalInput.startLoadedSources`
  starts the runtime at the first loaded chunk with a lit campfire or cold-pending heat, and only that start section
  gets an owner at once (`attachLoadedWorld` → `loadedSectionOrAttach(center)`); other sections wait for per-tick
  source discovery. A campfire or pending heat left by an earlier test in the same server makes the tests' own fire
  section ownerless right after the reload. Not fixed here.
- Net result: compared with the base, this work adds 8 passing tests, removes 2 obsolete ones, and introduces no new
  failure.
- `2026-11-15 10:18 -0600`, on the owner's request: the branch was fast-forwarded from `7aa1d82b9` to master
  `37adbc659` (passive queries in page-less chunks skip owner attachment and read the dormant record; equipment weight
  docs). The uncommitted work was set aside as a patch and reapplied with a three-way merge; the only shared file,
  `MinecraftThermalInput`, merged without conflict. The new fast path ends in the dormant air sample this work changed,
  and the two fit. After the update: compile passes; `frostedheart_climate_cover` and
  `frostedheart_passive_query_fast_path` pass 9/9. The broad and default suites above ran on `7aa1d82b9` and were not
  rerun after the update.
- Run time, for whoever runs this again: the one-launch broad set takes about 14 min, almost all of it fixed real-time
  waits in a few tests run one batch after another. `gameplay_balance` takes 223 s; the six `fixed_worker_*` failure
  tests about 38 s each (`thenIdle(500)` then `thenIdle(200)`); `frostedheart_thermal_async_minecraft_residency_handoff`
  61 s; `concurrent_save_actual_world` 29 s. 150 of 195 batches finish within 2 s, and server start takes about 30 s.
  For iteration, run only the namespaces a change touches.

## Documentation

- `docs/climate/world-climate-and-temperature.md`: §4 (altitude, new §4.1 cover and lag), §5 (block natural), §6 (air
  natural and the field background), the dormant note, §9 persistence and the header.
- `docs/climate/thermal-runtime-control.md`: new section "自然背景" with the field and persistence contract.
- `docs/climate/README.md`: scope of the world-climate entry.
- Companion pack repository: searched for the removed and new config keys; none appear (its
  `defaultconfigs/frostedheart-server.toml` has no climate-affection keys), so nothing changed there.

## Remaining

- Not checked in game, including the owner's y = −58 cave.
- Balance of τ10 and the cover limit is untested in play.
- The `block()` difference inside resident Pages, and the 0 °C cap on positive climate, are open decisions for the
  owner.
- The Section-natural dormant paths listed above can misjudge small perturbations in Sections whose backgrounds differ
  a lot (near the surface, under uneven cover).
- Six GameTests fail on the base commit as well: the four recorded since 2026-11-09 and the two order-dependent reload
  tests described under Validation.
