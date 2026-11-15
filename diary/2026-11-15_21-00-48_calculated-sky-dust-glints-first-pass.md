# Calculated Sky Dust Glints First Pass

- Time: `2026-11-15 21:00:48 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `in progress` (first pass committed as a checkpoint; its look is to be replaced)
- Scope: `content/climate/render/sky` (`DustGlitterField`, `DustGlitterRenderer`, `SkyView`, `SkyViewSet`, `SkyTables`, `IceHaloFormulas`, `IceHaloMap`, `SkySetting`, `CalculatedSkyRenderer`, `CalculatedSkyClientCommand`, `LightPillarSources`), `FHShaders`, `diamond_dust.glsl`, `DustGlitterGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `ee35448a1`. Step 4a of the remaining sky work, after `2026-11-15_18-02-44_calculated-sky-sun-pillar.md`.

## Completed

- **Glints of diamond dust near the eye.** One draw of points with no vertex data at `AFTER_WEATHER`: `gl_VertexID` is hashed to a position in a 32 block cube that repeats over the world, shown at the copy nearest the eye and faded out from 12 to 16 blocks. The drawn share follows the dust's extinction as a nested set, and the height above the dust's bottom weighs the light.
- **Light.** `SkyView.lightTransmittance`, sampled at the camera's radius toward the light and blended like the views times the light's scale, so the moon's glints work with no extra code. The phase is the sky's halo profile and maps; below -2 degrees the plates' mirror lobe continues the map's pillar, with each plate kind's lobe scale kept by `IceHaloMap.mirrorLobeScale`.
- **Sprites.** Each point is a round sprite with a tapered Gaussian core (`dustGlitterSize`), tone mapped per pixel.
- Settings `dustGlitterBrightness`, `dustGlitterFloor`, `dustGlitterCount`, `dustGlitterSize`; `/calculatedSky dust` prints the drawn points, GPU and CPU time, and allocated bytes.
- Docs: section "Glints of diamond dust", the settings, the command, the GameTests, and the known limits.

## Decisions

- The first calibration assumed 80% of sunlight at the eye; at a 10 degree sun through ice of depth 0.1 it is about 0.2, 0.1, and 0.04 per channel, so single pixel glints were 10 to 20 steps over a gray sky and invisible. Sprites and a gain of 20 made them visible.
- empireu found the look jarring and a little seizure inducing: glints seem to appear and disappear at random and do not move. Measured against empireu's video of real diamond dust: count (about 70 to 100 lit), size (about 4 px), and rise (50 to 65 of 255) match, but reference glints fall 25 to 43 px per frame on straight downward paths while steadily lit for about 0.2 s, where ours move 0.5 px per frame, flash with a peak, and light again on their sway circle 1 to 3 s later.
- empireu agreed to commit this pass as a checkpoint and redesign the look as falling glitter: stateless lives per point, each a new crystal that falls about 0.5 blocks/s within 30 degrees of vertical and is lit steadily for 0.15 to 0.7 s, more glints rather than brighter ones on the halos, rare faint near glints, and instanced quads. A CPU simulation with world access, collisions, and entities was considered and rejected for cost, state, and allocations; step 4b's heightmap will end glints at the ground and keep them from under roofs.

## Validation

- `bash agent-tools/build.sh` compiles. `bash agent-tools/gametest.sh frostedheart_calculated_sky`: 93 of 93. The lobe test holds bins under a tenth of the largest to a tenth of the largest, since tail bins at about 5% of the peak miss the map by up to 3.9% of their own value.
- Subordinate reviews: math and GL state of the points, then GL and brightness of the sprites. Fixed: the height as a brightness weight (points popped at a height threshold), a neutral floor color, and doc sentences. No code bugs in the sprite change.
- Minecraft's context is OpenGL 3.2 core, forward compatible (`Window`), so `gl_PointCoord` needs no enable.
- In `runClient` with `/calculatedSky set dustExtinction 1`: draws, GPU about 0.005 ms, CPU about 0.02 ms, 0 bytes allocated once warm. Harness runs `dust-2026-11-15_19-41-23` and `_19-51-16` against the before run `_18-37-58`.

## Remaining

- The falling glitter redesign, reviewed before implementing, then empireu's look, a recording measured against the reference, the harness set, and the U4 values.
- Glints show indoors and in caves until step 4b.
