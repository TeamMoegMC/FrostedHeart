# Calculated Sky Dust Glints From Lamps

- Time: `2026-11-16 18:30:00 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu checked it in game and kept the default of `dustLampGlitter`; the readings of the light work were not taken)
- Scope: `content/climate/render/sky` (`DustLightVolume`, `DustLights`, `DustColumns`, `DustGlitterRenderer`, `LightPillarWorldChanges`, `SkySetting`, `CalculatedSkyClientCommand`, `CalculatedSkyScreenshots`), `diamond_dust.glsl`, `DustLightGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `6d91513bb`. Step 4c of the remaining sky work.

## Completed

- **Lamps light glints.** Torches, lamps, and other light blocks light the glints of diamond dust by the block light where the glint is now, filtered between blocks, so a glint brightens and dims smoothly as it falls past a lamp. Block light has no direction, so lamps light the plain share of lives (`shownWeight(plainShare, ...)` with the direction weight's hash), with no halo weighting. The color is vanilla's block light color from `LightTexture`'s formula without flicker, clamped to `1` (`DustLightVolume.blockLightColor`): warm at middle levels, white next to the lamp. The lamp term is added after the exposure, in display units like vanilla's block light. Lamp glints pass the same open-air test and take the same envelope, fade, height, size, and near weights.
- **Draw at night.** The draw no longer stops when the sky gives no beam, if lamps can light glints, so lamp glints show on a moonless night.
- **Light volume.** `DustLightVolume` holds the block light of `36` by `36` by `36` blocks around the block corner nearest the camera, each block at its world position modulo `36`, with a bit set of blocks to read. `DustLights` follows the camera each client tick, reads up to `5,832` marked blocks a tick through the block light storage's `getDataLayerData` (keeping the last section's array), and uploads the volume as an `R8` 3D texture (`GL_LINEAR`, `GL_REPEAT`) when a light changed. Blocks entering the volume have no light until read. The existing light hooks in `LightPillarWorldChanges`, which dropped block light, now pass it: light the client computes (`ClientChunkCache.onLightUpdate`) and light from the server (`ClientPacketListener.readSectionList`) mark the section's blocks inside the volume. Nothing polls the world.
- **Shader.** `blockLightAt` samples the volume with the texture's own filter at the glint's offset, from the texel of the volume's first block and the camera's place counted from it, clamped half a block inside the volume; `blockLightColor` colors it. The renderer binds the texture on unit `4` with raw GL and restores the previous 3D binding, and passes a lamp gain of `0` while the camera's block is outside the volume.
- **Setting** `dustLampGlitter` (default `1`, `0` to `10`): the brightness of lamp glints at full block light, `0` turns them and the volume off, which gives the before and after in one game. `/calculatedSky dust` prints a line about the volume. The screenshot harness waits while lights are read.
- Docs: the section "Glints of diamond dust" (the lamp rule and color, the volume and its margin, the hooks and reads, the brightness formula with the lamp term, the cull order, unit `4`, the command), the settings table, the command, the harness's wait, the GameTest list, and the known limits.

## Decisions

- empireu decided to read the block light at the glint's current position, sampled trilinearly, instead of at the middle of its lit window, which can lie up to about `19` blocks from the eye at the defaults. A wrapped volume then needs a margin of ghost cells, which empireu asked about: the filter must never read the two blocks on either side of the seam. Glints show within `16` blocks, the filter reads half a block more, and the camera of a frame can be about a tick's move from the camera the tick followed. Centering the volume on the nearest block corner puts the camera `17.5` to `18.5` blocks from its first block, so `36` blocks hold a drift of `1` block on every side. The plan's `32` had no margin.
- Vanilla's block light color is computed in the shader from the level, not read from the lightmap texture, since night vision and the brightness slider bring dark and bright levels together there.
- The lamp term is not multiplied by the sky's exposure, so lamp glints keep their brightness if the exposure changes with the sun (step 5).
- The whole volume is uploaded when a light changed (`46,656` bytes), as the column window is, instead of the plan's slabs, which would need split uploads where the wrap cuts them.
- A chunk unload inside the volume is not handled: at the shortest view distance the volume reaches at most two chunks from the camera's chunk, which stay loaded, and a jump refills the volume.

## Validation

- `bash agent-tools/build.sh` compiles both source sets. `glslangValidator` compiles and links both stages of `diamond_dust.glsl` as `#version 150`.
- `bash agent-tools/gametest.sh frostedheart_calculated_sky`: `109` of `109` (log `gametest-frostedheart_calculated_sky-2026-11-16_18-38-48`), after the review fix. `DustLightGameTests` (four tests) was written by a Subordinate from an exact specification (`D:\repos\TWR\scratch\subordinate\lamps\task-tests.md`) and checked against it: the volume's follow, refill, and budget, section marks and rereads, the shader's filtered sample against the two world blocks around a glint for cameras up to `1` block from the tick's, also far from the origin, and the color against vanilla's formula at every level.
- Reviews by two Subordinates (deepseek, `scratch/subordinate/lamps/result-lamps-review-*.md`). The slabs a move clears, the index mapping, section overlaps, the sample against its twin (worked by hand for an eye at `-100.25`), the margin, the color, the lamp term, the draw's gate, the 3D texture and its pixel store, unit `4`, the hooks, and allocation are right. Fixed from the reviews: for up to one tick after a level change, the shader could read the earlier level's light, and the same held for the open-air columns of step 4b, so `DustLights.covers` and `DustColumns.covers` also check the level. Left: after a jump or a level change the volume is read in texel order, so lamp glints near the eye can stay dark for up to `8` ticks; the color is the block light before the gamma curve, as planned, so it falls off faster than the terrain's light looks; redundant clears at the corners of a move, about `415` section lookups per tick at the full budget, and the texture is never deleted, as the column texture is not.

- In game on `2026-11-16`, empireu found the default settings fine, `dustLampGlitter` `1` included.

## Remaining

- Optional: `/calculatedSky dust` readings of the light work (reads, uploads, time, allocated bytes) standing still and walking.
- Optional: vanilla's gamma curve on the lamp color, if lamp glints fade too close to their lamps.
