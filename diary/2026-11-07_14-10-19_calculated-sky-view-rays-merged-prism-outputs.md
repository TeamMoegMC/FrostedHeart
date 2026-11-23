# Calculated Sky View Rays And Merged Prism Outputs

- Time: `2026-11-07 14:10:19 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyTables`, `SkyView`, `IceHaloProfile`, `IceHaloMap`, `IcePrism`)

## Completed

- `SkyTables.buildViewRays` works out the steps of every row's view ray once per setting, since they depend only on the row's elevation, so `integrateView` no longer repeats them, with `900,000` calls of `Math.exp`, for each of the `100` columns on every view.
- `IceHaloProfile.irradiance` and `IceHaloMap.irradiance` fill all channels in one pass, and `SkyView.sampleChannels` reads every channel of a texel with one position lookup.
- `IcePrism` keys each output by the direction class of the light and its exit face and merges those of one trace call that share a key, which cut the points written per map from about `1,100,000` to about `150,000` ([Ice crystals and halos](../docs/climate/calculated-sky.md#ice-crystals-and-halos)).

## Decisions

- empireu asked whether 32 bit floats with polynomial sine and cosine would help: polynomial sine, cosine, and exponential saved about `2 ms` of `82` per map and `3.5 ms` of `28` per view, and a float copy of the ray tracers gained nothing, so empireu kept the library calls.
- A shared table of end face to end face light by deviation was set aside: one table read by every axis would freeze its sampling noise into the arcs, unless built from about `25 million` rays or shipped as a dataset.
- A version of `IcePrism` that worked out the reachable faces and Fresnel factors once per direction class was dropped: its preparation cost as much as it saved, and it was no faster than merging alone (`72..76 ms` against `72..74 ms`) with about twice the code.
- Merged outputs take the direction of the first light in their slot; later light in the same slot leaves in the same direction up to rounding.

## Validation

- Outside the game, a scratch harness built maps and views at sun `2` to `70 degrees`, against the committed code in alternating runs.
- Sky views are bit for bit identical; halo maps are identical farther than `2 degrees` from the sun, and within `2 degrees` `54` to `108` of `198,720` values differ by up to `0.8%` of the map's peak. `0` bytes are allocated per map and per view.
- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky` with JDK 17: all 6 required tests passed, against the unchanged reference values. Docs updated; empireu found the optimizations sufficient in `runClient`.

## Remaining

- Of a map's `73 ms`, ray tracing takes about `39 ms` (Parry side paths `15 ms`, end face to end face `24.5 ms`) and side face cones `24 ms`; faster tracing needs other samples, which changes the halos and needs a look in game.
- Of a view's `15.5 ms`, the view rays take about `13 ms`; the transmittance and multiple scattering lookups still compute their radius dependent parts, including a square root, at every step.
