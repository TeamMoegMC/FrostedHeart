# Calculated Sky Delta Scaled Ice

- Time: `2026-11-08 14:18:16 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyAtmosphere`, `SkyTables`, `SkyTransmittance`), `calculated_sky.fsh`, `CalculatedSkyGameTests`

## Completed

- Delta scaled the ice ([Delta scaled ice](../docs/climate/calculated-sky.md#delta-scaled-ice)): the totals count only the half that does not diffract, the sun's square keeps the full ice extinction, and `SkyTables.diffractionWeights` scales the peak per row by `(1 - exp(-f tau)) / (f tau)`.
- GameTest `sunSquareAndAureoleCarryTheDeltaScaledBeam`.

## Decisions

- empireu asked on `2026-11-08` to delta scale the ice before the weather law, after the `v1` set at depth `0.5` showed a dim orange sun and a dark gray sky and snow at sunrise and sunset.
- All paths are delta scaled, not only the sun's beam, because the path tracer showed the sky seen through ice and the light between scatterings losing the diffracted half too; without the weight the glow at depth `0.5` and a sun at `3 degrees` would carry about three times the light that reaches it.
- The weight is per row of the sky view, from the ice optical depth along the row, rather than a second ice view, which would have doubled its memory and uploads; it is exact toward the sun.
- The transmittance rows were concentrated in the ice band in the same change, because the glow around a low sun was overestimated by `6..7%` at depth `0.5` from linear interpolation of `exp(-tau)` between rows about `1 km` apart in the ice. `64` rows with `32` in the band made the sky without ice worse, so the table grew to `128` rows.

## Validation

- Preview mode `delta` against the path tracer at depths `0.1` to `2` and sun elevations `3` to `30 degrees`: the mean dE went from `2.4..11.4` to `1.6..4.7`, the light within `10 degrees` of the sun from `0.28..0.85` to `0.59..1.00` of the path tracer's, and the ground from `0.54..1.09` to `0.79..1.07`.
- `compileJava compileGameTestJava runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky`: all `10` required tests passed.
- empireu took the set `v1` at depth `0.1` (`v1-2026-11-08_14-24-33`) and `0.5` (`v1-2026-11-08_14-25-20`), all frames settled: at `0.5` sunrise and sunset show a warm glow around the sun and a brown sky over warm snow instead of the dim orange square and the dark gray; at `0.1` frames differ from the run before by a mean of `3..5` display steps. empireu accepted the remaining error of up to about `8%` near a low sun at thick ice, as anything up to `10%` is.

## Remaining

- At depth `2` and a low sun the fast model stays about `20%` darker than the path tracer, where the multiple scattering treats repeated scattering by the forward scattering crystals as even in all directions.
- The weather law, with the wash-out depths measured again under delta scaling.
