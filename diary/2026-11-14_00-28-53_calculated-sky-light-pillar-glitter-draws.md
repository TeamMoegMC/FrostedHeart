# Calculated Sky Light Pillar Glitter Draws

- Time: `2026-11-14 00:28:53 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`LightPillarOptics`, `LightPillarRenderer`), `shaders/light_pillars.glsl`, GameTests, `docs/climate/calculated-sky.md`

## Completed

- Light pillars draw their glitter counts and sum their integral with less work, after `2026-11-13_22-31-49_calculated-sky-light-pillar-gpu-time.md` (`d51fbfba0`). The draws, the two savings in the integral, the `4` point Gauss Legendre rule over tilts within `3 sigma` instead of `8` points within `4 sigma`, and the debug mode are in [Light pillars](../docs/climate/calculated-sky.md#light-pillars) and [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).

## Decisions

- On `2026-11-13` empireu asked for an approximation of the integral and allowed up to `10` read-only Subordinate sessions for ideas; six looked at the integral, quadrature rules, the cost per point, and the glitter. empireu chose the glitter draws and the exact savings in the integral; the approximations of the integral and the area of the wobbling plates' ribbons stay open.
- Rejected the gamma form of the Wilson Hilferty approximation for the draws, which two sessions proposed: it never draws `0` and has twice the Poisson distribution's skew, a Kolmogorov Smirnov distance of `0.108` at `N = 6`. The rounded Cornish Fisher expansion keeps whole counts and matches the distribution.
- Rejected one Box Muller pair shared by this period's count and the next: the next period's first count would differ from this period's second, so cells would jump at the period's end.
- Rejected two identities, the light's leg times its mean density as the dust column over `sin(e - 2 tau)` and the across scale as `cos(beta - tau) / (cos tau sin(e - beta))`: both hold, but in single precision the first lost accuracy where the light's leg is long and nearly level, and the second saved nothing.
- After the debug mode showed the integral takes most of the time, empireu set the standard on `2026-11-14`: approximations only need to look about right, not match within a display step. A review's Gauss Hermite rule rarely applies to wobbling plates, and its rule from the window's moments needs an erf approximation, moments up to the seventh, and two fallbacks; its step counts were about `34` times too high, since `1.7%` relative error makes one display step at the most sensitive brightness. Gauss Legendre with fewer points over a range cut to `3 sigma` does as well with no fallback, and empireu took it. Per-pillar texels at a stable lower resolution were left for later.

## Validation

- A Java port of the shader's math matches `dustRadiance`, `dustCrystalCount`, and `widestLateralSpread` within `5e-10` relative over `200000` random cases.
- `lightPillarCrystalDrawsFollowThePoissonDistribution`, run outside the game, gives Kolmogorov Smirnov distances of at most `0.0036` for means from `0.3` to `10000`; `lightPillarSparkleKeepsTheMeanValue` is within `0.04%` of the mean value from `N = 4` up.
- Both shader stages pass `glslangValidator`, `compileJava` passes, and all `52` GameTests pass.
- In game, the laptop GPU lowers its clock while it is not fully loaded, so the earlier readings measured the clock as much as the work. F3+T did not load a shader copied into `build/resources/main`, so those swaps compared a shader with itself; only restarts with the shader in `src` count. After restarts the integral takes `61%` of the draw, and the `4` point rule reads `0.25 ms` against `0.372 ms` for the `8` point rule.

## Remaining

- Rendering the pillars at a lower resolution and upscaling them, which empireu chose next on `2026-11-14`.
- Smaller cuts left open: `3` points, up to about `10` display steps off in the cores of plate pillars, the per-pixel setup moved out of the integral, and a narrower glitter widening.
- The screenshot harness does not wait for pillars to fade in after a move, so it cannot take pillar comparisons yet.
- The remaining items of the last entry.
