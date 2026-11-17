# Calculated Sky Dust Glints In Open Air

- Time: `2026-11-16 17:55:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu checked it on a house; the harness set and the column counters were not run)
- Scope: `content/climate/render/sky` (`DustColumnWindow`, `DustColumns`, `DustGlitterRenderer`, `LightPillarWorldChanges`, `SkySetting`), `diamond_dust.glsl`, `DustColumnGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `8bab2f421`. Step 4b of the remaining sky work.

## Completed

- Glints show only where their crystal could have fallen, as in [Glints of diamond dust](../docs/climate/calculated-sky.md#glints-of-diamond-dust): at or above the first open height of its column at the middle of its lit window, and in open air where it is now.
- Switch `dustGlitterOpenAir` (default on) turns the test off, for a before and after in one game; harness set `dust-shade`, variants `open-air` and `open-air-off`.

## Decisions

- empireu proposed sky light `15`, open to the sky, since the glints fall along columns, but sky light passes glass undimmed (`AbstractGlassBlock.propagatesSkylightDown` returns true, and glass has no occlusion shape), and empireu wants no glints under glass, since the crystals cannot fall through it. The `MOTION_BLOCKING` heightmap is that falling rule.
- No shade toward the light. The planned march over a heightmap toward the sun and empireu's shadow maps for Embeddium and Flywheel from another project were left out: vanilla terrain shows no sun shadows, so glints missing in a shadow nobody sees on the ground would read as a bug; a low sun, when glints look best, has the longest shadows; and a shadow map is a terrain pass for a few hundred glints.
- A window of `64`, not the plan's `256` with a coarse map, since the march is gone: the middle of a lit window stays within about `31` blocks of the eye sideways even at the longest lit time and fastest fall.

## Validation

- `gametest-frostedheart_calculated_sky`: `105` of `105` (log `gametest-frostedheart_calculated_sky-2026-11-16_17-56-34`), after the review fixes. `DustColumnGameTests` (four tests) was written by a Subordinate.
- Two Subordinate reviews (deepseek). Fixed: a glint whose middle lay just above a roof fell through the ceiling into the room for the rest of its lit window, so the shader also tests where the glint is; for one tick after a jump the shader read the old window's edge, so glints are hidden while the eye's column is outside the window; the bitwise and takes an `ivec2` mask. Left: at the largest lit time and fall speed, a glint at the edge of the reach can read the next column in for a tick after the eye crosses a block. FrostedHeart's permafrost changes mark nothing, since both states stop crystals.

## Remaining

- Optional: the harness set `dust-shade` with recorded poses, and `/calculatedSky dust` readings of the column work.
- Lamp glints (step 4c).
