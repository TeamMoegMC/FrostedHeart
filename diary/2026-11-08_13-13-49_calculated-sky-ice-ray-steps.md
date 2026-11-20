# Calculated Sky Ice Ray Steps

- Time: `2026-11-08 13:13:49 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `complete`
- Scope: `content/climate/render/sky` (`SkyRaySteps`, `SkyTables`, `SkyTransmittance`, `SkyAtmosphere`), `CalculatedSkyGameTests`

## Completed

- `SkyRaySteps.fill` splits a ray where it crosses the ice band (`6.2..15.8 km`) and gives each crossing `12` equal steps, with the even grid's cells elsewhere; the view rays, `G`, and both tables use it ([Ray steps](../docs/climate/calculated-sky.md#ray-steps)).
- The multiple scattering table has `48` rows instead of `32`, spaced evenly below, inside, and above the band. GameTest `rayStepsCoverTheIceLayerOnEveryPath`.

## Decisions

- empireu agreed on `2026-11-08` that the ice optical depth should follow the weather, rising ahead of forecast snow as cirrostratus comes before a front, with depth changes crossfaded between tables; diamond dust comes after the sky.
- The fix comes first as its own step, because the probe for the law found that even steps put one or two samples in the `1.2 km` ice layer, leaving bands across the sky above depth `0.3` and, at depth `3`, errors of up to `64` display steps and twice the ground light.
- Twelve band steps were chosen over `8` and `16`: eight left `7` display steps at depth `0.5` and sixteen were no better. With `24` even view steps instead of `30` the sky without ice got worse, so the even steps stayed.
- Concentrating the multiple scattering rows in the band at the same `32` rows matched `128` even rows, and the sun cosines made no difference; `48` rows brought depth `3` from `4.3` to `1.5` display steps for `1.5` times the table's build.

## Validation

- A probe outside the game: at the default depth `0.1` the largest error away from the sun fell from `3.7` to `1.6` display steps.
- The ice optical depth along the ray steps is within `0.45%` of a fine integration for the view rows and `1.5%` at worst for a ray grazing the band; the GameTest asks for `1%` on the view and `2%` on the others, after `0.5%` failed at `0.56%`.
- `compileJava compileGameTestJava runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky`: all `9` required tests passed.
- empireu took the set `v1` at depth `0.1` (`v1-2026-11-08_13-33-08`) and `0.5` (`v1-2026-11-08_13-33-59`): at `0.1` it matches the run before (`v1-2026-11-07_17-36-15`) within one display step; at `0.5` the halos are stronger and the sky overhead about `34` steps whiter, but the sun at sunrise and sunset is a small dim orange square.

## Remaining

- The dark low sun at depth `0.5` comes from the model, not the steps: the sun's beam loses the whole ice depth along a slant path about `13` times the vertical at `3 degrees`, while the diffracted half keeps going within a few degrees of it. Delta scaling was proposed as the next step, before the weather law, whose ranges would then be measured again.
