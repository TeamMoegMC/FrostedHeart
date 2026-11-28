# Calculated Sky Light And Draw Timer

- Time: `2026-11-15 13:07:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyFrame`, `CalculatedSkyRenderer`, `CalculatedSkyClientCommand`, `CalculatedSkyScreenshots`, new `SkyLight` and `SunElevationTimes`), GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, uncommitted, after `df48ae583`. Step 1 of the remaining sky work.

## Completed

- **Sky draw timer.** `CalculatedSkyRenderer` has its own `SkyDrawTimer` around `draw`, not `updateTables`, so the uploads of finished builds stay out of the means ([Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars)).
- **`SkyLight`.** The light the sky is drawn for, with its elevation, direction, side, and a scale on its radiance, and no client imports. `SkyFrame.fill` sets it from the sun, and the renderer reads `SkyFrame.light()`. The moon is the sun negated. Nothing uses the moon or the scale yet, and no shader or uniform changed ([Runtime](../docs/climate/calculated-sky.md#2-runtime)).
- **Harness poses by sun elevation.** `SunElevationTimes.dayTime` turns a sun elevation and a branch (`setting` or `rising`) into the day time that puts the sun nearest it; poses take `sunElevations` and `sunBranch` ([Screenshot harness](../docs/climate/calculated-sky.md#screenshot-harness)).

## Decisions

- `SkyFrame` no longer has sun accessors, since nothing else read them; later steps add what they need.
- The timer's means include the first draw after a shader reload, which links the program inside the timed region, as the pillar timer does; one draw in `120`.

## Validation

- A temporary GameTest (`SkyBaselineHashGameTests`, written by a Subordinate session to a spec) printed `109` FNV-1a hashes of the halo profile, halo maps, tables, sky views, fog and daylight colors, and grid indices. All `72` tests passed before the change, and all `109` hashes are the same after it.
- Two read-only Subordinate reviews found no bugs. Fixed from their nits: the doc's wording of the branches, the warning for a clamped elevation, and the note that the sky's frame time also holds the pillars' time with diamond dust. They also found that sunrise at elevation `0` is day time `23215`, not `23222` as the plan said.
- New GameTests, written by a Subordinate session to a spec: `skyLightPutsTheMoonOppositeTheSun`, `sunElevationTimesRoundTripOnBothBranches` (`-18` to `90 degrees` come back within `0.0081 degrees`), and `sunElevationTimesAreSymmetricAroundMidnight` (noon `6000`, midnight `18000`, sunset `12785`, sunrise `23215`). All `75` tests pass.
- In `runClient` on `2026-11-15` empireu read the sky timer at 1080p in fullscreen: a GPU time of `0.12..0.17 ms`, a CPU time of `0.013..0.017 ms`, and a frame time about `0.08..0.10 ms` above the sky off. That is below the timer's GPU time, plausibly because at these frame rates the CPU and the GPU work overlap.

## Remaining

- Step 2, moonlight, starts with the measurement of the switch elevation.
- `SkyBaselineHashGameTests` stays until the check after step 2, then goes.
