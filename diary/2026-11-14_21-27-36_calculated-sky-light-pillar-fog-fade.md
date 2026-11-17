# Calculated Sky Light Pillar Fog Fade

- Time: `2026-11-14 21:27:36 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `complete`
- Scope: `content/climate/render/sky` (`SkySetting`, `LightPillarSources`, `LightPillarRenderer`, `LightPillarOptics`), GameTests, `docs/climate/calculated-sky.md`. Branch `feature/pillar-fake`, uncommitted, after `209db9225`.

## Completed

- Light pillars reach the render distance and fade out with their lights in the fog, as described under [Lights for light pillars](../docs/climate/calculated-sky.md#lights-for-light-pillars). Before, `pillarChunkRadius` (default `8`, at most `16`) limited the chosen lights, so at a render distance of `12` pillars ended at about `130` to `140` blocks, each fading out over half a second as its chunk left the circle (empireu, in game).
- The setting `pillarChunkRadius` (`SkySetting.PILLAR_CHUNK_RADIUS`) is removed; `LightPillarSources.chunkRadius` is the render distance, which the index already covered, so the tick does no more work, and `LightPillarSelection.isWithinChunkRadius` is unchanged.
- `LightPillarRenderer.fillPillars` multiplies each slot's opacity by how much of its light shows through the terrain fog, read from `RenderSystem` (fog start, end, shape, and the fog color's opacity); `LevelRenderer.renderLevel` sets that fog before the terrain and keeps it until `FogRenderer.setupNoFog`, after `AFTER_WEATHER`. A slot hidden in the fog is written as unused.
- The fade follows vanilla's `fog.glsl`: a cylinder takes the larger of the horizontal and the vertical distance, all of the light shows up to the fog start, and a smoothstep takes it to the fog end, times the fog color's opacity. At a render distance of `12` the fog runs from `172.8` to `192` blocks, while the chunk circle reaches about `208`.
- The fade goes by the cluster's center, the point the pillar is drawn for. It also hides pillars under blindness, under water, and in FrostedHeart's snow fog (`FogModification`), which shortens the fog end.
- empireu's client config no longer has the line.

## Decisions

- empireu chose to drop the setting instead of raising its range, and to fade pillars with the fog.
- The fade follows the fog's own values instead of a band before the render distance, so it ends where the terrain ends whatever sets the fog.
- No before run: empireu's in-game look that prompted the change is the before data.

## Validation

- Calculated sky GameTests: all `69` pass, with the new `lightPillarFadesWithItsLightInTheFog` (fog shares at the fog start, middle, three quarters, and end at a render distance of `12`, half opacity, no fog, and cylinder and sphere distances).
- In game, empireu: the fade checks out.

## Remaining

- More lights compete for the `48` slots at a larger render distance; `/calculatedSky pillars` shows the chosen count against the slots used.
- The rest of that docs section still describes the passes from before the event-driven index.
