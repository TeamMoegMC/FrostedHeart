# Calculated Sky Light Pillar Fog Fade

- Time: `2026-11-14 21:27:36 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `complete`
- Scope: `content/climate/render/sky` (`SkySetting`, `LightPillarSources`, `LightPillarRenderer`, `LightPillarOptics`), GameTests, `docs/climate/calculated-sky.md`. Branch `feature/pillar-fake`, uncommitted, after `209db9225`.

## Completed

Light pillars now reach the render distance and fade out with their lights in the fog. Before, the setting `pillarChunkRadius` (default `8`, at most `16`) limited the lights that are chosen, so at a render distance of `12` pillars ended at about `130` to `140` blocks, each fading out over half a second as its chunk left the circle (empireu, in game).

- The setting `pillarChunkRadius` (`SkySetting.PILLAR_CHUNK_RADIUS`) is removed. `LightPillarSources.chunkRadius` is `Options.getEffectiveRenderDistance`, which also takes a server's smaller view distance. The index already covered every loaded chunk, so the tick does no more work. The circle test `LightPillarSelection.isWithinChunkRadius` is unchanged.
- `LightPillarRenderer.fillPillars` multiplies each slot's opacity by how much of its light shows through the terrain fog, read from `RenderSystem` (fog start, end, shape, and the fog color's opacity). `LevelRenderer.renderLevel` sets the terrain fog before the terrain and keeps it until `FogRenderer.setupNoFog`, after `AFTER_WEATHER`. A slot hidden in the fog is written as unused, so a frame with only such slots draws nothing.
- `LightPillarOptics.fogDistance` and `fogVisibility` follow `fog_distance` and `linear_fog` of vanilla's `fog.glsl`: a cylinder takes the larger of the horizontal and the vertical distance, all of the light shows up to the fog start, and a smoothstep takes it to the fog end, times the fog color's opacity. At a render distance of `12` the fog runs from `172.8` to `192` blocks, so pillars fade over that band, while the chunk circle reaches about `208` blocks.
- The fade goes by the cluster's center, the point the pillar is drawn for. It also hides pillars under blindness, under water, and in FrostedHeart's snow fog (`FogModification`), which shortens the fog end.
- empireu's client config no longer has the `pillarChunkRadius` line.

## Decisions

- empireu chose to drop the setting instead of raising its range, and to fade pillars with the fog.
- The fade follows the fog's own values instead of a band before the render distance, so it ends where the terrain ends whatever sets the fog.
- No before run: empireu's in-game look that prompted the change is the before data.

## Validation

- `bash agent-tools/build.sh` passes.
- `bash agent-tools/gametest.sh frostedheart_calculated_sky`: all `69` pass, with the new `lightPillarFadesWithItsLightInTheFog` (fog shares at the fog start, middle, three quarters, and end at a render distance of `12`, half opacity, no fog, and cylinder and sphere distances).
- In game, by empireu: the fade checks out.

## Remaining

- More lights compete for the `48` slots at a larger render distance. `/calculatedSky pillars` shows the chosen count against the slots used.
- `docs/climate/calculated-sky.md` drops the setting from the table and names the render distance and the fog fade under "Lights for light pillars". The rest of that section still describes the passes from before the event-driven index, as before this change.
