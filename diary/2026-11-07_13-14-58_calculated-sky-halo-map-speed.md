# Calculated Sky Halo Map Build Speed

- Time: `2026-11-07 13:14:58 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`IceHaloFormulas`, `IceHaloMap`, `IcePrism`, `SkyTables`, `SkyView`, `CalculatedSkyRenderer`)

## Completed

- `IcePrism.trace` and `traceMirrors` hand their outputs over as the cosine and sine of the deviation instead of an angle from `atan2`, `traceMirrors` computes the hexagon corners once per call, and `IceHaloFormulas.addEndToSide` reads its `360` face angles from a table.
- Side face cones add consecutive points of one bin once, `IceHaloMap.inverseBinSolidAngle` replaces a division per point, and `blurAzimuth` pads each row only as far as the blur reaches.
- After empireu asked why anything was still allocated, maps and views are built without allocating, through instance buffers, `IceHaloMap.Workspace`, and a `SkyView` class that `SkyTables.computeView` fills in place. The times are in [Validation And Performance](../docs/climate/calculated-sky.md#validation-and-performance).

## Decisions

- empireu asked whether 32 bit floats would help: float copies of the map storage, and of the side face loop and bin lookup, timed the same (`225..244 ms`) and moved the totals by `0.016%`, so the code stays in double, as Java's trigonometry is double only. Smaller bin lookup tables (`4096` and `1024` cells instead of `65536`), a bin lookup stepping from the previous point's bin, and keeping the three colors next to each other were left out, as the first two measured no gain and side face writes were no longer costly after merging.
- Scratch buffers live in instances, not static fields, since the builds run on Minecraft's shared background executor and static buffers would corrupt maps if two builds ran at once. The renderer's single workspace runs one build at a time and keeps at most one spare map and view, so a settings change or a sun jump of more than one map step still allocates.

## Validation

- Outside the game, a scratch harness built maps at sun `2` to `70 degrees`, against the committed code in alternating runs. All steps before the ray tracer change gave bit for bit identical maps; after it, every bin farther than `2 degrees` from the sun is identical, and within `2 degrees` bins differ by up to `0.3%` to `2.1%` of the map's peak, with the same totals.
- The allocation-free builds refilled one map and one view at the six elevations: `0` bytes per map and per view from the first build on, and the last round bit for bit identical to the version before.
- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky` with JDK 17: all 6 required tests passed, against the unchanged reference values. Docs updated; `runClient`: empireu reported "Ingame checks out".

## Remaining

- A halo map time from `/calculatedSky` in game with the faster build, at a known sun elevation.
- The sun on a row boundary makes the row of light along the sun direction depend on rounding; moving the rows by half a bin would stabilize it but changes the map grid and the GameTest references, and it is invisible in game, under the sun's glow and blurred by `0.26 degrees`.
