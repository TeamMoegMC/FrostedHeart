# Calculated Sky Light And Draw Timer

- Time: `2026-11-15 13:07:00 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyFrame`, `CalculatedSkyRenderer`, `CalculatedSkyClientCommand`, `CalculatedSkyScreenshots`, new `SkyLight` and `SunElevationTimes`), GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, uncommitted, after `df48ae583`. Step 1 of the remaining sky work (moonlight, sun pillar, diamond dust glints, stars and the evening), planned in the contributor's workspace.

## Completed

- **Sky draw timer.** `CalculatedSkyRenderer` has its own `SkyDrawTimer` around `draw`, not around `updateTables`, so the texture uploads of finished builds stay out of the means. `/calculatedSky` prints it as its `Sky draw` line.
- **`SkyLight`.** The light the sky is drawn for, the sun or the moon, with its elevation, direction, side, and a scale on its radiance. It has no client imports. `SkyFrame.fill` sets it from the sun with the same expressions as before, and the renderer reads `SkyFrame.light()` for the grid indices, the blends, the fog color's azimuth, and the uniforms. The moon is the sun negated: elevation `-e`, direction `(-x, -y)`, and side `-side`. Nothing uses the moon or the scale yet, and no shader or uniform changed.
- **Harness poses by sun elevation.** `SunElevationTimes.dayTime` turns a sun elevation and a branch (`setting`, from noon to midnight, or `rising`) into the day time from `0` to `23999` that puts the sun nearest it. With `T` from `DimensionType.timeOfDay`, the sun's elevation is `asin(cos(2 pi T))`, so `T = (90 - e) / 360` setting and `(270 + e) / 360` rising, and the day fraction `d` after day time `6000` follows by bisection on `2 d - cos(pi d) / 2 = 3 T - 0.5`. Poses take `sunElevations` and `sunBranch`, whose times go after the pose's `times`.
- **Docs.** `docs/climate/calculated-sky.md`: the code anchors, `SkyFrame`'s light in the runtime paragraph, the `SkyDrawTimer` paragraph, the harness's `poses` row, and the measured GPU time of the draw.

## Decisions

- `SkyFrame` no longer has sun accessors, since nothing else read them. Later steps add what they need, for example the sun's elevation for choosing the light.
- The timer's means include the first draw after a shader reload, which links the program inside the timed region, as the pillar timer does. One draw in `120`.

## Validation

- Before the change, a temporary GameTest (`SkyBaselineHashGameTests`, written by a Subordinate session to a spec) printed `109` FNV-1a hashes of the halo profile, `8` halo maps, the tables of `4` ice depths, `40` sky views, and the fog and daylight colors of `27` blended view sets, plus the grid indices. All `72` tests of `frostedheart_calculated_sky` passed.
- Two read-only Subordinate reviews of the diff (correctness and byte identity; GL state, timing, and allocations) found no bugs. Fixed from their nits: the doc's wording of the branches, the warning for a clamped elevation, and the note that the frame time with the sky on and off also holds the pillars' time when diamond dust is in the air. They also found that sunrise at elevation `0` is day time `23215`, not `23222` as the plan said, since the two branches are symmetric around `18000`.
- After the change, all `109` hashes are the same as before.
- New GameTests, written by a Subordinate session to a spec: `skyLightPutsTheMoonOppositeTheSun` (the sun's values bit for bit as `SkyFrame` computed them, the moon's negated, and a reused light that forgets the moon), `sunElevationTimesRoundTripOnBothBranches` (sun elevations from `-18` to `90 degrees` through vanilla's `timeOfDay` come back within `0.0081 degrees`, on the right side of noon), and `sunElevationTimesAreSymmetricAroundMidnight` (noon `6000`, midnight `18000`, sunset `12785`, sunrise `23215`, and the two branches mirrored around `18000` within a tick). `bash agent-tools/gametest.sh frostedheart_calculated_sky`: all `75` tests pass.
- In `runClient` on `2026-11-15`, empireu read the sky timer at 1080p in fullscreen, looking at the sun or the moon:

| Time | GPU | CPU | Frames per second, sky on | Sky off | Frame time difference |
|---|---|---|---|---|---|
| Day | `0.16..0.17 ms` | `0.014 ms` | `640..670` | `680..720` | about `0.10 ms` |
| Dusk | `0.12 ms` | `0.017 ms` | `700..720` | `750..760` | about `0.08 ms` |
| Night | `0.12 ms` | `0.013 ms` | `830..840` | `900..920` | about `0.10 ms` |

  The frame time difference is taken from the middle of each range. It is below the timer's GPU time, plausibly because at these frame rates the CPU and the GPU work overlap. The F3 screen lowers the frame rate a lot, so the rates are rough.

## Remaining

- Step 2, moonlight, starts with the measurement of the switch elevation.
- `SkyBaselineHashGameTests` stays until the check after step 2, then goes.
