# Calculated Sky Falling Glints

- Time: `2026-11-15 22:44:27 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu in game: "We got a good result"; the harness set and a measured recording remain)
- Scope: `content/climate/render/sky` (`DustGlitterField`, `DustGlitterRenderer`, `SkySetting`), `diamond_dust.glsl`, `DustGlitterGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `f5fe11fed`. Replaces the look of `2026-11-15_21-00-48_calculated-sky-dust-glints-first-pass.md`.

## Completed

- Stateless lives, the split clock, the halo shares, near glints, and instanced quads, as in [Glints of diamond dust](../docs/climate/calculated-sky.md#glints-of-diamond-dust).
- After empireu's first look asked for longer lit times and more size difference between near and far: `dustGlitterLitSeconds` (`0.2` to `4`) and `dustGlitterSizeSpread` (`0` to `2`), as described there. The near blur now only dims, since the size law widens near glints.
- `dustGlitterPlainShare` replaces `dustGlitterFloor` under a new key, since a first pass config holds `0.03`.

## Decisions

- Four Subordinates reviewed the plan: the old floor counted twice (plain directions would have shown `64%`), the lit count left out the fade (about `50` lit, not `70`), lit times had too long a tail, glints popped at a halo's edge and the near threshold, ring glints saturated, and quads missed face culling.
- Lives of `1` to `2 s` instead of `3` to `7 s` at the reference's lit time: the same glints for a quarter of the points, so the first defaults (count `16384`, plain share `0.5`) gave about `75` glints lit at once.
- Calibration over a background of `100` of `255`, since the reference's rise of about `50` is over trees and at the noise floor over sky above `200`.
- The common drift is removed rather than kept at zero speed, since the shown share rises to `1 + 0.075` on the halo, so no halo life is dimmed by the margin.

## Validation

- `glslangValidator` compiles and links both stages of `diamond_dust.glsl` as `#version 150`. `gametest-frostedheart_calculated_sky`: `99` of `99` (log `gametest-frostedheart_calculated_sky-2026-11-15_22-37-59`), then `101` of `101` (log `gametest-frostedheart_calculated_sky-2026-11-15_23-*`) after the lit time and size settings.
- `DustGlitterGameTests`, rewritten by a Subordinate: seven new tests (lives per wrap, the split clock, lit times and envelope, the fall's fan, the middle of the lit window, the halo shares, near glints), the box test rewritten, the twinkle test removed; later two more (`glitterLitTimeStretchesWithItsSetting`, `glitterSizeFollowsDistanceWithItsLightSpread`), with the wrap test at life rates `1`, `0.4`, and `0.05`.
- Subordinate code review (deepseek): the shader matches the twin quantity by quantity and every number in the docs recomputes; the GL review returned empty answers, so uniforms, allocations, corner math, and culling were checked by hand.

## Remaining

- The `dust` harness set at the new defaults, the `/calculatedSky dust` readings at count `32768`, and optionally a recording measured against the reference.
- Glints still show indoors and in caves until step 4b.
