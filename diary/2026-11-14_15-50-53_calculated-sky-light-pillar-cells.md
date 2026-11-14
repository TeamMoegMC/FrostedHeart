# Calculated Sky Light Pillar Cells

- Time: `2026-11-14 15:50:53 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarCells`, `LightPillarRenderer`, `LightPillarOptics`, `SkySetting`), `bootstrap/client/FHShaders`, `shaders/light_pillars.glsl`, GameTests, `docs/climate/calculated-sky.md`

## Completed

Light pillars can be drawn in square cells on a grid of each pillar's own, computed once per cell in an atlas, after `2026-11-14_00-28-53_calculated-sky-light-pillar-glitter-draws.md` (`b4d6f682b`).

- New setting `pillarTexelDegrees` (`SkySetting.PILLAR_TEXEL_DEGREES`, default `0.25`, `0` draws per pixel as before). Each ribbon's grid has columns by the lateral angle from the pillar's axis, the middle one on it, and rows by elevation up from `max(phi, -phi - 6 sigma)`, so cells stay on the sky as the view turns and move with the light as the eye moves.
- `LightPillarRenderer` first draws the ribbons into the cell atlas, an `R16F` texture laid out by `LightPillarCells`, with `light_pillars.glsl` compiled with `CELL_ATLAS` (`FHShaders.getLightPillarCellsVertex` and `getLightPillarCellsFragment`). That pass computes each cell once at its center without the scene, with the cell's glitter. The ribbons on the screen then read their cell, or compute it where it lies beyond the atlas, and keep the share of its light from crystals in front of the scene (`sceneShare`, Gaussian weights of the tilts only). The two programs are held by `PillarProgram`. The ribbon buffer marks the top vertices of each quad by a doubled side.
- With cells, margins and the lateral blur follow the cell where it is larger than a pixel, glitter draws one count per cell, the fill view shows cells computed at the pixel in blue, debug modes `1` and `2` act on the cell values, and mode `4` leaves out the atlas pass.
- Java twins: `LightPillarCells` for the layout and the grid, `LightPillarOptics.sceneShare` and `normalShare`.
- Five GameTests, written by a Subordinate session (DeepSeek) from my specification: `lightPillarCellAtlasKeepsEveryRibbonApart`, `lightPillarCellsStandOnTheLight`, `lightPillarSceneShareFollowsTheCutIntegral`, `lightPillarSceneShareStaysWithinItsBounds`, `lightPillarNormalShareFollowsTheNormalDistribution`.
- `docs/climate/calculated-sky.md` describes the cells, the atlas, the setting, the debug modes and fill view with cells, and the new GameTests.

## Decisions

- On `2026-11-14` empireu asked for lower resolution that is visibly pixelated, finer than the sky's texels, and stable in space. Screen-aligned low resolution makes pillars crawl under the blocks as the view turns, and sky-anchored texels made them crawl as the eye moved in the first pillar draw on `2026-11-12`, so each pillar has its own grid. Snapping pixels to cells alone would have saved no time, since every pixel would still compute its cell, so cells are computed once in an atlas. empireu accepted the pillar-anchored grid, `0.25 degree` cells, and glitter per cell, and asked to implement it directly in the game instead of a look test or a probe of ribbon widths first.
- The atlas holds one value per cell. Per-pixel scene depth is applied afterwards as the Gaussian share of the tilts in front of the scene, since storing moments of the tilt distribution per cell would take three channels. Ribbons wider than `16` plate columns or `64` wobbling plate columns, and rows beyond `1024`, compute their cells at the pixel instead, which keeps the atlas to about `6.5 MB` at `0.25 degrees`.

- A read-only Subordinate review (DeepSeek) found no error in the cell indexing, the layout, or the clip mapping, and four problems, all fixed: the atlas pass inherited the scissor test, which would leave parts of the atlas uncleared and unwritten where it is on, so it is turned off for the atlas pass and restored; where a ribbon widened between two rows, the atlas quad's sides ran inside the screen quad's sagged corners, so those cells read `0`, and now each atlas quad stands on its screen quad's corners and the next quad covers the middle above them (the last segment cannot widen, since each row takes the largest width of its neighbors); the atlas was deleted only in frames that drew pillars, and now at the start of every frame; and the debug mode javadoc did not say how mode `1` looks with cells.
- The first scene share weighed the tilts by their Gaussian alone. Within the near fade it kept `0.33` of a pillar's light in front of terrain where the integral keeps `0.019`, a torch `10` blocks away with terrain about `2` blocks off. It now also weighs the near weight, taken to fall linearly in the tilt between the crystals at the near fade and at its start, in closed form from the normal distribution and density (`nearTiltMass`).

## Validation

- All four shader variants pass `glslangValidator`, and `compileJava` and `compileGameTestJava` pass.
- GameTests on `2026-11-14`, `bash agent-tools/gametest.sh frostedheart_calculated_sky`: all `57` pass. The scene share is within `0.063` of the cut integral for plates and `0.235` for wobbling plates (tolerances `0.1` and `0.25`), both worst within the near fade of a light `10` blocks away; before the near weight it was `0.31` for plates and failed. `normalShare` is within `1.4e-4`. At `0.25 degrees` each ribbon has `420` rows, and the rows reach `0.0044` radians past the top of the highest ribbon.

- In game on `2026-11-14`, empireu read `0.18 ms` of GPU time with cells of `0.25 degrees`, against `0.25 ms` per pixel, and found the cells look better than per pixel, stylized. The fill view showed no blue, so every cell read lay within the atlas.
- In the same session, from a hill with a tree in front and torches in a river valley far below behind it, faint pillar streaks showed over the tree. They stayed with `pillarTexelDegrees` at `0`, so they come from the pillar model, not from the cells.

## Remaining

- The streaks over the near tree. For a light far below the eye, the low part of its pillar comes from crystals within a few blocks of the eye, here in front of the tree, which shadows them from the light in reality. The horizons around each light should cut them, but they sample `64` directions at `16` distances out to about `158` blocks and interpolate between samples about `20` blocks apart there, so a single tree near the eye can be missed. To investigate next, with the screen-space rule that step 3 of the shadow work removed as a candidate fix.
- Timing per debug mode with cells, to see whether the atlas pass, the screen pass, or the doubled vertex stage takes the time.
- The scene share for wobbling plates within the near fade is the roughest part of the approximation.
