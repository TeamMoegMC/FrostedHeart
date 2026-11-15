# Calculated Sky Falling Glints

- Time: `2026-11-15 22:44:27 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu's look in game: "We got a good result"; the harness set and a measured recording remain)
- Scope: `content/climate/render/sky` (`DustGlitterField`, `DustGlitterRenderer`, `SkySetting`), `diamond_dust.glsl`, `DustGlitterGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `f5fe11fed`. Replaces the look of `2026-11-15_21-00-48_calculated-sky-dust-glints-first-pass.md`.

## Completed

- **Stateless lives.** Each point lives one life after another, `1` to `2 s` each at the reference's lit time, a whole number of lives per `4096 s` wrap. Each life is a new crystal from a hash of the point and the life's number: a start in the `32` block cube, a fall at `dustGlitterFallSpeed` times `0.7` to `1.3` within `30 degrees` of straight down, and one lit window of `0.17 + 0.16 h + 0.42 h^12` seconds with `0.05 s` ramps, so a glint is above half its peak for `0.12` to `0.7 s` with quartiles `0.16`, `0.2`, `0.25 s`, as measured on empireu's video of real diamond dust. A glint falls on a straight line while lit and never lights again in the same place.
- **Clock in two parts.** The CPU passes the clock's share of the wrap as a whole number of `1 / 4096` steps and the rest, so the life's number and age stay exact in float to about a microsecond and the life spanning the wrap keeps its number.
- **Halos show more glints, not brighter ones.** The phase sets the share of lives shown, `dustGlitterPlainShare` (default `0.2`) away from the halos and all of them on the halos, with a margin of `0.15` on each life's hash; halo glints take half the halo's color. These choices and the near share are taken at the middle of the lit window, so they hold while the eye stands still.
- **Near glints.** Within `4` blocks a glint's whole light falls as the square of its blur (up to `3`), it fades out by `1` block, and only `25%` of lives show within `3` blocks. The first version also widened them by the blur, which the size spread below replaced.
- **Instanced quads.** One `glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, count)`; corners are spanned in the vertex stage, so sprites clip at the screen's edge instead of vanishing. Face culling is saved, turned off, and restored.
- **After empireu's first look** (empireu had set brightness `1.25`, plain share `0.2`, fall speed `1`): longer lit times and more size difference between near and far were asked for. `dustGlitterLitSeconds` (`0.2` to `4`) is the median lit time; it stretches lit windows, ramps, and lives by one factor, so the share of lit lives stays. `dustGlitterSizeSpread` (`0` to `2`) sets a width of `(6 / r)^p` times `dustGlitterSize`, from a quarter to 4 times, with each glint's whole light spread over its width and cores no thinner than `0.6` pixels. The near blur now only dims (whole light `1 / b^2`), since the size law widens near glints.
- Settings: `dustGlitterPlainShare` replaces `dustGlitterFloor` (a new key, since a first pass config holds `0.03`), new `dustGlitterFallSpeed`; `CALIBRATED_BRIGHTNESS` `0.5`. Docs: the section "Glints of diamond dust", the settings, the GameTest list, and the known limits.

## Decisions

- Plan `D:\repos\TWR\FALLING_GLINTS.md` (workspace notes, outside the repository), reviewed by four Subordinates before implementing. The review found the old floor counted twice (plain directions would have shown 64%, not the intended share), the lit count leaving out the fade (about 50 lit, not 70), lit times with too long a tail, pops at a halo's edge and at the near threshold, saturated colored ring glints, and missing face culling for quads. All fixed in the plan before code.
- Lives of `1` to `2 s` instead of `3` to `7 s` at the reference's lit time: the same glints for a quarter of the points, so the first defaults (count `16384`, plain share `0.5`) gave about `75` glints lit at once.
- Calibration over a background of `100` of `255`, since the reference's rise of about `50` is over trees and at the noise floor over sky above `200` (measured with a background breakdown of the video). Glints fade over bright sky and snow, as there.
- The common drift is removed rather than kept at zero speed. The shown share rises to `1 + 0.075` on the halo, so no halo life is dimmed by the margin.
- The defaults are the values empireu settled on in game: brightness `2`, plain share `0.2`, fall speed `2` (about `260` pixels a second at `6` blocks, a third of the close-up reference's screen speed), size spread `1.5`, lit seconds `1`, count `32768`, and size `3.5`. About `60` glints are then lit at once in plain directions, five times as dense on the halos.

## Validation

- `bash agent-tools/build.sh` compiles both source sets. `glslangValidator` compiles and links both stages of `diamond_dust.glsl` as `#version 150`.
- `bash agent-tools/gametest.sh frostedheart_calculated_sky`: `99` of `99` (log `gametest-frostedheart_calculated_sky-2026-11-15_22-37-59`). `DustGlitterGameTests` was rewritten by a Subordinate from an exact specification: seven new tests (lives per wrap, the split clock against double and across the wrap, lit times and envelope, the fall's fan, the middle of the lit window, the halo shares, near glints), the box test rewritten, the twinkle test removed.
- After the lit time and size settings: `101` of `101` (log `gametest-frostedheart_calculated_sky-2026-11-15_23-*`), with the existing tests at a time scale of `1`, the wrap test at life rates `1`, `0.4`, and `0.05`, and two new tests (`glitterLitTimeStretchesWithItsSetting`, `glitterSizeFollowsDistanceWithItsLightSpread`), again written by a Subordinate from an exact specification. The shader links again.
- Code review (Subordinate, deepseek): the shader matches the twin quantity by quantity up to float rounding, and every number in the docs recomputes. The GL review session returned empty answers, so uniforms, per frame allocations, corner math, and culling were checked by hand.
- In game, empireu tuned the settings and approved the look. GPU and CPU time from `/calculatedSky dust` were not recorded here.

## Remaining

- The `dust` harness set at the new defaults, GPU and CPU time and allocated bytes from `/calculatedSky dust` at count `32768`, and optionally a recording measured against the reference (`video_glints.py`, `video_paths.py`).
- Glints still show indoors and in caves until step 4b.
