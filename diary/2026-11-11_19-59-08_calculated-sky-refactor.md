# Calculated Sky Refactor

- Time: `2026-11-11 19:59:08 +0200`
- Author: `Claude; Anthropic Opus 5.5, with a DeepSeek reviewer through pi; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyFrame`, `SkyViewSet`, `SkyBuilder`, `SkyState`, `SkyTextures`, `SkyGlPass`, `SkySetting`, `BandRows`, `SkyDisplay`, `CalculatedSkyClientCommand`), `FHConfig`, GameTests, `docs/climate/calculated-sky.md`, the plan

## Completed

Five commits on `feature/calculated-sky`, nothing drawn or computed differently:

- `f9dc8a2b1`: `SkyFrame` works out the sun's elevation and direction, the ice depth, the grid indices, the exposure, and the rain and thunder levels once per frame ([Runtime](../docs/climate/calculated-sky.md#2-runtime)).
- `f2d3758ed`: `SkyViewSet` holds the four views by slot and owns the fog color and daylight blends.
- `6d6eaa88c`: the renderer is split into `SkyBuilder`, `SkyState`, `SkyTextures`, and `SkyGlPass`, and a renderer of about `380` lines.
- `5582826b7`: `SkySetting` holds every setting's key, comment, default, range, and rebuilds; new GameTest `settingsRebuildOnlyWhatDependsOnThem` ([Configuration](../docs/climate/calculated-sky.md#3-configuration)).
- The commit of this entry: `BandRows`, one `rowPositionAt`, `SkyRaySteps.ICE_BAND_STEPS` declared once, `SkyDisplay`, and the command gated by `CalculatedSkyClientCommand.ENABLED`.

## Decisions

- LDLib's `UniformCache` allocates per uniform call, so the draw uses raw `glUniform` calls keyed by the `ShaderProgram`.
- empireu dropped array textures, the only part that would have changed GL behavior.
- empireu kept the harness and gated the whole command, so without it an error clears only with a restart.
- The shared GLSL include was skipped, since `Shaders.load` passes the source to GL without `#moj_import`.
- `SkyViewSet.daylightColor` keeps its own snow normalization through `SkyDisplay.encodeSrgb`, since `toneMap` would group `-exposure * albedo / pi` differently.
- No build speedups: the builds take about `10%` of one background thread while the sun moves, which empireu found sufficient on `2026-11-07`.
- After each step a second model (DeepSeek V4.1 Flash through pi, read only) reviewed the change and found no bugs; notes on an unbind outside `finally`, visibility, docs wording, and a shadowed name were applied.

## Validation

- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky` with JDK 17 after every step: all `16` tests, and `17` once `settingsRebuildOnlyWhatDependsOnThem` was added.
- empireu checked the frame context, the view set, and the renderer split in `runClient`, and approved the settings table and the duplicate removal.
- A temporary probe in `runClient` on `2026-11-11` measured `0` bytes allocated per frame by the draw, the fog color, and the daylight once warm; early `24` bytes per draw were likely a mixin callback the JIT removes, not traced.
- The last commit was checked outside the game on the compiled classes before and after: every output was byte identical, and warm timings matched: two table builds `298 ms` before and `302 ms` after, four views `77.8 ms` and `77.7 ms`.
- The full `frostedheart` GameTest namespace was not run.

## Remaining

- `CalculatedSkyClientCommand.ENABLED` goes to `false` before a pull request.
- The GPU time of the draw has never been measured.
- Diamond dust, light pillars, and moonlight come next, on `SkyFrame`, `SkyGlPass`, and `SkySetting`.
