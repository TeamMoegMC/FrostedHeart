# Calculated Sky Dust Glints From Lamps

- Time: `2026-11-16 18:30:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (empireu kept the default of `dustLampGlitter` in game; the readings of the light work were not taken)
- Scope: `content/climate/render/sky` (`DustLightVolume`, `DustLights`, `DustGlitterRenderer`, `SkySetting`), `diamond_dust.glsl`, `DustLightGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `6d91513bb`. Step 4c of the remaining sky work.

## Completed

- Lamps light glints by the block light where the glint is now, filtered between blocks, in vanilla's block light color and added after the exposure, as in [Glints of diamond dust](../docs/climate/calculated-sky.md#glints-of-diamond-dust).
- `dustLampGlitter` is the brightness of lamp glints at full block light; `0` turns them and the volume off, which gives the before and after in one game.

## Decisions

- With empireu: the light is read where the glint is now, not at the middle of its lit window, which can lie up to about `19` blocks from the eye. That needs a margin, which empireu asked about: the filter must never read the two blocks on either side of the seam, so the volume is `36` blocks, not the plan's `32`.
- The lamp term is not multiplied by the sky's exposure, so lamp glints keep their brightness if the exposure changes with the sun (step 5).
- The whole volume is uploaded on a light change (`46,656` bytes), as the column window is, instead of the plan's slabs, which would need split uploads where the wrap cuts them.
- A chunk unload inside the volume is not handled: the volume reaches at most two chunks from the camera's, which stay loaded, and a jump refills it.

## Validation

- `gametest-frostedheart_calculated_sky`: `109` of `109` (log `gametest-frostedheart_calculated_sky-2026-11-16_18-38-48`).
- `DustLightGameTests` (four tests, written by a Subordinate): the volume's follow, refill, and budget, section marks and rereads, the filtered sample against the two world blocks around a glint, and the color against vanilla's formula at every level.
- Two Subordinate reviews (deepseek). Fixed: the shader could read the earlier level's light for up to one tick after a level change, and the open-air columns of step 4b had the same fault, so `DustLights.covers` and `DustColumns.covers` also check the level. Left: lamp glints near the eye can stay dark for up to `8` ticks after a jump or a level change, a move's corners clear redundantly at about `415` section lookups a tick at the full budget, and the texture is never deleted, as the column texture is not.

## Remaining

- Optional: `/calculatedSky dust` readings of the light work standing still and walking, and vanilla's gamma curve on the lamp color if lamp glints fade too close to their lamps, since the color falls off faster than the terrain's light looks.
