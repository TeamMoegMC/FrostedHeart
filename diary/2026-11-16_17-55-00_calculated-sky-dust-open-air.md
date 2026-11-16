# Calculated Sky Dust Glints In Open Air

- Time: `2026-11-16 17:55:00 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu checked it in game on a house; the harness set and the column counters were not run)
- Scope: `content/climate/render/sky` (`DustColumnWindow`, `DustColumns`, `DustGlitterRenderer`, `LightPillarWorldChanges`, `SkySetting`, `CalculatedSkyClientCommand`, `CalculatedSkyScreenshots`), `diamond_dust.glsl`, `DustColumnGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `8bab2f421`. Step 4b of the remaining sky work.

## Completed

- **Glints only where crystals can fall.** A life of a glint shows only if, at the middle of its lit window, it is at or above the first open height of its column, from the `MOTION_BLOCKING` heightmap (`blocksMotion()` or a fluid, `DustColumnWindow.stopsCrystals`). Glass, panes, iron bars, fences, walls, leaves, ice, water, slabs, and stairs close a column; carpets, snow layers, torches, rails, cobwebs, and plants do not. No glints under roofs, glass, or trees, or in caves; the air outside a window or a cave mouth still glitters.
- **Column window.** `DustColumnWindow` holds `64` by `64` first open heights around the camera's column at world column modulo `64`, with a bit set of columns to read. `DustColumns` follows the camera each client tick, reads up to `1024` marked columns through `ClientLevel.getHeight`, and uploads the window as an `R32F` texture when a height changed. Columns entering the window count as closed until read. Block changes that change `stopsCrystals` and chunks that arrive mark columns through the existing hooks in `LightPillarWorldChanges`. Nothing polls the world.
- **Shader.** After the near cull, two `texelFetch`es: the glint's column at the middle of its lit window, which decides the life without popping, and its column where it is now, so a glint goes out where it falls onto a roof instead of falling through the ceiling into the room below. Glints under cover are moved outside the clip volume before the phase lookup. The renderer passes the window's first texel, the eye's column in the window, the eye's place in its block, and its height, so the shader sees small numbers, and hides every glint while the eye's column is outside the window (after a jump, before the next tick).
- **Switch** `dustGlitterOpenAir` (default on) turns the test off, for a before and after in one game. `/calculatedSky dust` prints a line about the columns. The screenshot harness waits while columns are read. Harness set `run/sky-shots/dust-shade.json` with variants `open-air` and `open-air-off`.
- Docs: the section "Glints of diamond dust" (open air, the window, the hooks, the cull order, units `0` to `3`), the settings table, the command, the harness's wait, and the known limits.

## Decisions

- empireu proposed using sky light `15`, open to the sky, since the glints fall along columns. Sky light passes glass undimmed (`AbstractGlassBlock.propagatesSkylightDown` returns true, and glass has no occlusion shape), and empireu wants no glints under glass, since the crystals cannot fall through it. The `MOTION_BLOCKING` heightmap is that falling rule, kept by Minecraft per column on the client, filled from the chunk packet and updated in `LevelChunk.setBlockState`.
- No shade toward the light. The planned march over a heightmap toward the sun and empireu's shadow maps for Embeddium and Flywheel from another project were weighed and left out: vanilla terrain shows no sun shadows, so glints missing in a shadow nobody sees on the ground would read as a bug; a low sun, when glints look best, has the longest shadows; and a shadow map is a terrain pass for a few hundred glints. A march over the same column heights remains possible later.
- A window of `64`, not the plan's `256` with a coarse map, since the march is gone: the middle of a lit window stays within about `31` blocks of the eye sideways even at the longest lit time and fastest fall.
- The source facts (client heightmaps, every block change path, chunk events, no allocation in `getHeight`) were already verified in the plan's source lookup, so no new lookup ran.

## Validation

- `bash agent-tools/build.sh` compiles both source sets. `glslangValidator` compiles and links both stages of `diamond_dust.glsl` as `#version 150`.
- `bash agent-tools/gametest.sh frostedheart_calculated_sky`: `105` of `105` (log `gametest-frostedheart_calculated_sky-2026-11-16_17-56-34`), after the review fixes. `DustColumnGameTests` (four tests) was written by a Subordinate from an exact specification (`D:\repos\TWR\scratch\subordinate\shade\task-tests.md`) and checked against it.
- Reviews by two Subordinates (deepseek, `scratch/subordinate/shade/result-shade-review-*.md`). The addressing agrees between the shader and `DustColumnWindow` for eyes at negative coordinates and near the world's edge, `follow` and `readMarked` are right, the window holds the widest reach (`30.6` blocks) with a column to spare, the off switch and unread columns behave, the texture and its state are sound, nothing allocates per tick or frame, and the harness's wait ends within `4` ticks; FrostedHeart's permafrost changes mark nothing, since both states stop crystals. Fixed from the reviews: a glint whose middle lies just above a roof kept falling through the ceiling into the room for the rest of its lit window, so the shader now also tests where the glint is; for one tick after a jump the shader read the old window's edge, so glints are hidden while the eye's column is outside the window; the bitwise and takes an `ivec2` mask instead of a scalar. Left: at the largest lit time and fall speed, a glint at the edge of the reach can read the next column in for a tick after the eye crosses a block.
- In game on `2026-11-16`, empireu viewed a single house and found it working fine.

## Remaining

- Optional: the harness set `dust-shade` with recorded poses (variants `open-air` and `open-air-off`), and `/calculatedSky dust` readings of the column work (reads, uploads, time, allocated bytes) standing still and walking.
- Lamp glints (step 4c).
