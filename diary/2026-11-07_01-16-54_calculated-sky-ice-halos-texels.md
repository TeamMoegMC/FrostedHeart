# Calculated Sky Ice Crystal Halos On Sky Texels

- Time: `2026-11-07 01:16:54 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky`, `FHConfig` client group `CalculatedSky`, `calculated_sky.fsh`, `datasets/ice_halos`, GameTest namespace `frostedheart_calculated_sky`

## Completed

- Halo display from hexagonal ice crystals, replacing the three-part ice phase: the 22 and 46 degree halos, sun dogs, parhelic and circumzenithal arcs, tangent and circumscribed halos, and Parry arcs ([Ice crystals and halos](../docs/climate/calculated-sky.md#ice-crystals-and-halos), stage 3 of [the plan](../plans/2026-11-05_21-16-30_calculated-sky.md)).
- The sky is drawn on square texels fixed to it, `0.5 degrees` by default, from `texelDegrees`; `0` draws a smooth sky ([Configuration](../docs/climate/calculated-sky.md#configuration)).
- `IceCrystalData` reads the tables once; `IceHaloProfile`, `IceHaloMap`, `IceHaloFormulas`, and `IcePrism` build the display on the background executor. Ground irradiance takes all ice single scattering, the fog color none.

## Decisions

- With empireu: sky-fixed texels over screen-space ones, raw data after ElectricalAge2's datasets, no preview animation, ice optical depth `0.1` with `0.3` to compare, and the ring model dropped without a toggle.
- The tables are ray traced offline, since tracing in game would cost seconds per sun elevation; the game evaluates formulas over them, about `0.45 s` per map. The generator is the preview's `datasets` mode, outside the repository; empireu may move it into the mod later.
- Blending the two halo maps around the sun replaced the planned fade over time since an upload, which would have lagged behind the sun, at the same cost.
- empireu chose `0.5 degree` texels after `1 degree` looked coarse, at `2.45` times the earlier sampling. Rejected: blurring as for `1 degree`, free but as soft; denser steps alone, which left a dotted parhelic circle; everything denser (`1.8 s` per map), with the same faint traces.
- A compute shader was set aside: Minecraft 1.20.1 asks for an OpenGL 3.2 core context and macOS stops at 4.1, the CPU needs the map for the ground light, and the GameTests run without GL.

## Validation

- `compileJava compileGameTestJava` with JDK 17: no errors. `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky`: 6 of `6`, after the dataset test's path shares were accepted from `0.99` to `1` (`0.99972`).
- Looks in `runClient`: halos around the vanilla sun; the red fog at sunset is stage 2's fog color; no popping, but at `0.5` and `0.25 degree` texels the arcs broke into colored speckles, as sampling and blur were set for `1 degree`; with the denser sampling at ice optical depth `0.1`, "It looks fine".
- The preview comparison, the `datasets` integration, and the build times are in [Validation And Performance](../docs/climate/calculated-sky.md#validation-and-performance).

## Remaining

- The square vanilla sun shows within the calculated sun glow at ice optical depth `0.1`, since vanilla draws its sun on top of the dome.
- The halo map build time in game against the preview, at a known sun elevation.
- Missing halo features and halos tied to weather ([Known Limits](../docs/climate/calculated-sky.md#known-limits)); blindness in clear weather and the ring through a blizzard.
