# Infrared in a new world: an unpublished field no longer hides the natural base

- Time: `2026-11-15 12:22:38 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `fixed and GameTested; not checked in game`
- Scope: `InfraredCapture.capture`, infrared and world-climate documentation
- Follows: [colour scale](2026-11-15_11-41-55_infrared-violet-to-red-scale.md),
  [infrared natural base](2026-11-15_10-48-51_infrared-natural-base.md)

## Report

In the owner's new singleplayer world, infrared was violet (missing values) everywhere. Lighting the first campfire
turned the whole view cyan at once.

## Cause

The client log showed the thermal runtime starting at world creation (`Thermal runtime: fixed cell enthalpy` at
12:05:26), long before the campfire. Such a runtime has a `QueryPublication` that has never published:
`QueryPublication.valid` becomes true only in `publishFixed`, so `beginRead` fails. `InfraredCapture.capture` treats a
failed read as a concurrent publication exchange: it retries once and then returns null, so the request handler sends
only an ACK. The ACK carries no `InfraredNaturalClimate`, and the client never got a window or a natural climate.
Before the natural base this looked the same as "no data". When the campfire made the field publish, the next reply
was a full one with the climate, so the whole natural base appeared at once; the cyan was the correct natural
temperature (about −10 °C at the start of a new world: climate 0 after the `min(global, 0)` cap, −10 dimension).

## Fix

The retry-then-null rule protects a readable client baseline from being mistaken for deleted. A client without one
(`knownReadable == false`) has nothing to lose, so after the retry `capture` now continues without the field, as if
there were no runtime: the window, stored bodies, analytic fields and the natural climate are sent. A client with a
readable baseline keeps the old behaviour.

## Validation

- New `InfraredUnpublishedFieldGameTests.anUnpublishedFieldStillSendsTheWindowAndNaturalClimate`: with a fresh, never
  published fixed publication, a full request from a client without a readable baseline gets a full, unreadable reply
  that carries the natural climate, while a request from a client with a readable baseline still gets null.
- Regression, one launch, 99 tests (`frostedheart_climate_cover`, the infrared protocol namespaces,
  `frostedheart_tiled_publication`, default `frostedheart`): the new test and all 30 tests of `defaultBatch` (the
  infrared protocol tests) pass. The 6 failures are the ones that fail on base `7aa1d82b9`, with the same messages,
  including `campfireSurfaceUsesMaterialAndLocalAvailability` ("closed publication must commit a field-only full").

## Documentation

- `docs/climate/world-climate-and-temperature.md`: the read-race rule now states the exception for clients without a
  readable baseline.
- `docs/climate/infrared-view.md`: why the natural climate needs a data reply in a new world.

## Remaining

- Not checked in game.
- Pixels that are not captured terrain, such as the sun and some translucent faces, read an air cell and show the
  violet placeholder, as they showed blue before. Leaving missing values untinted would separate them from real
  −50 °C; not changed here.
