# Calculated Sky Light Pillar Overhangs And Near Fade

- Time: `2026-11-13 19:01:53 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarHorizons`, `LightPillarOptics`, `LightPillarRenderer`, `SkySetting`), `light_pillars.glsl`, GameTests. After `35cee329a`, the last step of `2026-11-13_16-19-52_calculated-sky-light-pillar-shadows.md`.

## Completed

- Near fade: `pillarNearFade` (`8` blocks by default, see [Configuration](../docs/climate/calculated-sky.md#3-configuration)). `LightPillarOptics.nearWeight` weights each crystal by its distance from the eye, from `0` at `0.25` of the setting to `1` at it, and cuts the highest tilt where the crystals reach the start of the fade.
- Overhangs: each sample of `LightPillarHorizons` holds the horizon `G`, the ceiling `F`, and the top `H` above the light's top, and light leaving at angle `a` passes when `G < a < F` or `a > H`. The light's own column sets the top to `CLOSED`.
- The renderer passes ceiling and top in `vec2 overhangs[48]` and the setting in `nearFade`; the shader sums the tilts below `(e - H) / 2` and between `(e - F) / 2` and `(e - G) / 2`.

## Decisions

- empireu asked for both steps on `2026-11-13`: the near fade removes the band over the log roof from the last entry, and the overhangs give the torch under the tree its pillar back.
- `CLOSED` is `pi`, not `pi / 2`, so a block above the light also stops light rising past the vertical away from the eye; with `pi / 2` the ribbon edge test found `2.28` times the visible value.
- Overhangs of different columns merge into one band from the lowest ceiling to the highest top, so light between two overhangs at different heights counts as stopped.

## Validation

- `48` of `48` tests, with ribbon edges at most `0.986` times the visible value with overhangs and a near fade. Both shader stages pass `glslangValidator`.
- In game the torch under the tree shows its pillar again, and empireu found the result fine.
- Known limit: ribbon edges near the zenith leak when the horizon or the top is close to `pi / 2`, up to `7.7` times the visible value. Evaluating the width at the edge's elevation fixes tops up to `1.5` radians but doubles the vertex stage's work.

## Remaining

- In game: the torch in glass under a log roof, and the blocks read in `/calculatedSky pillars` inside a forest.
- The checks of changes from the last entry, and the docs update for all of this work, with a plan file in `plans/`.
- Decide on the zenith leak. The accumulation target for overlapping ribbons is left open, and `CalculatedSkyClientCommand.ENABLED` goes to `false` before a pull request.
