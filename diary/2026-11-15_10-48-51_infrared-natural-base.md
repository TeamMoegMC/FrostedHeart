# Infrared shows the natural temperature, and the soil thermometer always reads

- Time: `2026-11-15 10:48:51 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `implemented and GameTested in worktree branch claude/cover-climate-isolation; not committed; not checked in game`
- Scope: `InfraredCapture`, `InfraredNaturalClimate` (new), infrared request/response packets,
  `MinecraftThermalInput.InfraredSnapshot`, `InfraredViewRenderer`, `InfraredNaturalFill` (new), `ClimateCover`,
  `LevelChunkMixin_ClimateCover`, `WorldClimate`, `FHClientEvents`, `SoilThermometer`
- Follows: [cover climate isolation](2026-11-15_01-02-00_cover-climate-isolation.md)

## Why

The owner's first report, at y = −58: infrared was blue everywhere and did not show the cave's natural temperature,
and the soil thermometer said "no material temperature" on blocks outside the thermal field. The server sends infrared
values only for blocks with a material temperature or an analytic field; everything else was INVALID, which the shader
draws as −20 °C. The thermometer read `WorldTemperature.material`, NaN without a live or stored body.

## Decisions (owner)

- The client computes the natural temperature, not the server. The server would have to send several hundred KB per
  full window and resend each climate hour, and spend tens of milliseconds per player per full capture.
- Material blocks use the air formula `WorldTemperature.naturalAir` (full climate), the same background the thermal
  field holds them against, so readings do not jump at the field's edge. The block formula (0.5 × climate) would have
  required giving material cells a separate background in the field.

## Completed

- **Server.** `InfraredCapture.naturalClimate` builds an `InfraredNaturalClimate` for the 9×9 window: covered climate for
  cover 0..limit (`WorldClimate.openCoveredClimates` shared by chunks without a white curtain,
  `WorldClimate.coveredClimates` for each curtain chunk), the superflat flag and the lower clamp, with a content-hash
  revision. It travels in the first part of a reply. The request carries `knownNaturalRevision`; a client with another
  revision makes an otherwise empty reply non-empty. Callers that do not track it pass `UNTRACKED`, which keeps the old
  empty-reply rule (the 10-argument snapshot entry, the old `capture` signature, the request's convenience
  constructors), so existing tests keep their meaning.
- **Client.** `InfraredNaturalFill` keeps a 144³ natural value per block, computed from the client's own blocks,
  biomes (at Brick centres, as `NaturalProfile` does), `WorldTempData`/`BiomeTempData` and `ClimateCoverColumns`, plus
  the server's climate tables. Air stays INVALID. It computes at most 32 Pages per client tick, nearest first, only
  while infrared is open. `InfraredViewRenderer.uploadPage` shows it wherever the mirror is INVALID; full uploads merge
  Page by Page once natural values exist.
- **Recompute triggers.** New revision → all Pages; window move → entering Pages (the overlap shifts like the
  mirror); client chunk load (`FHClientEvents.loadClientChunk`) → that column; client block change in a chunk whose
  cover the client has read → its Page, and the Pages below when opacity changed (`LevelChunkMixin_ClimateCover` →
  `ClimateCover.clientBlockChanges`). Pages of unloaded chunks wait without using the budget.
- **Soil thermometer.** `SoilThermometer.reportMaterialTemperature` falls back to `WorldTemperature.naturalAir`.
- The client reads biome and dimension data directly instead of through `WorldTemperature`'s caches, which are plain
  maps used by the integrated server's thread.

## Validation

- Compile passes.
- New `InfraredNaturalFillGameTests` (namespace `frostedheart_climate_cover`):
  - `repliesCarryTheNaturalClimateUntilTheClientHoldsIt`: the table equals `WorldClimate.getCoveredTemp` at every
    cover; flag and clamp are the server's; a client holding the revision gets no reply (checked when the window has
    no analytic field); another revision gets it again; the first part keeps it over the wire.
  - `filledBlocksReadTheirNaturalAirAndFollowCoverChanges`: running the client fill on the server level, every non-air
    block of the test Section reads its `naturalAir` within one 0.25 °C step and air has no value; after an opaque
    block is placed above, the recomputed column matches again.
- Regression run, one launch, 98 tests: `frostedheart_climate_cover` (10), the infrared protocol namespaces
  `frostedheart_infrared_bandwidth`, `_diagnostics`, `_network`, `_stream`, `_window` and
  `frostedheart_tiled_publication` (26, in `defaultBatch`), and the default `frostedheart` namespace. Both new tests
  and all 26 infrared protocol tests pass. The 6 failures are exactly the ones that fail on base `7aa1d82b9` (see the
  cover-climate entry): four recorded since 2026-11-09 and the two order-dependent reload tests.

## Documentation

- `docs/climate/infrared-view.md`: new section "自然温度底色", data path, and what blue still means.
- `docs/climate/world-climate-and-temperature.md`: the infrared paragraph on missing values and the removed
  729-value background.
- `docs/climate/data-lifecycle-and-integration.md`: soil thermometer fallback; request and reply fields.

## Remaining

- Not checked in game. The fill's client-side cost (estimated about a tenth of a millisecond per solid Page, so
  about 3 ms per tick while filling) is not measured.
- Entities and other non-terrain pixels read the air cell and stay blue where the server sends no air value, as
  before.
- The goggle HUD (`SoilThermometerRequestPacket`) still shows `WorldTemperature.block`, the crop environment, which
  uses the block formula.
