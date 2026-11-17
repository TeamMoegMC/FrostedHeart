# Calculated Sky Light Pillar Cells

- Time: `2026-11-14 15:50:53 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarCells`, `LightPillarRenderer`, `LightPillarOptics`, `SkySetting`), `bootstrap/client/FHShaders`, `shaders/light_pillars.glsl`, GameTests, `docs/climate/calculated-sky.md`

## Completed

- Light pillars can be drawn in square cells on each pillar's own grid, computed once per cell in an atlas, after `2026-11-14_00-28-53_calculated-sky-light-pillar-glitter-draws.md` (`b4d6f682b`). The setting `pillarTexelDegrees`, the grid, the atlas, the scene share, and the debug modes with cells are in [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars); Java twins `LightPillarCells`, `LightPillarOptics.sceneShare` and `normalShare`.
- Five GameTests, written by a Subordinate session (DeepSeek) from my specification: `lightPillarCellAtlasKeepsEveryRibbonApart`, `lightPillarCellsStandOnTheLight`, `lightPillarSceneShareFollowsTheCutIntegral`, `lightPillarSceneShareStaysWithinItsBounds`, `lightPillarNormalShareFollowsTheNormalDistribution`.

## Decisions

- On `2026-11-14` empireu asked for lower resolution that is visibly pixelated, finer than the sky's texels, and stable in space. Screen-aligned low resolution makes pillars crawl as the view turns and sky-anchored texels made them crawl as the eye moved, so each pillar has its own grid; snapping pixels to cells alone would save no time, so cells are computed once in an atlas. empireu accepted the pillar-anchored grid, `0.25 degree` cells, and glitter per cell, and asked to implement it in the game rather than test it first.
- The atlas holds one value per cell, and the per-pixel scene depth is applied afterwards as the Gaussian share of the tilts in front of the scene, since moments of the tilt distribution per cell would take three channels; ribbons too wide for a band, and rows beyond `1024`, compute their cells at the pixel.
- A read-only Subordinate review (DeepSeek) found no error in the cell indexing, the layout, or the clip mapping, and four problems, all fixed: the atlas pass inherited the scissor test; the atlas quad's sides ran inside the screen quad's sagged corners where a ribbon widened between rows, so those cells read `0`; the atlas was deleted only in frames that drew pillars; and the debug mode javadoc was incomplete.
- The first scene share weighed the tilts by their Gaussian alone, which kept `0.33` of a pillar's light in front of terrain within the near fade where the integral keeps `0.019`; it now also weighs the near weight (`nearTiltMass`), falling linearly in the tilt between the crystals at the near fade and at its start.

## Validation

- All four shader variants pass `glslangValidator`, and `compileJava` and `compileGameTestJava` pass. GameTests: all `57` pass. The scene share is within `0.063` of the cut integral for plates and `0.235` for wobbling plates (tolerances `0.1` and `0.25`), worst within the near fade of a light `10` blocks away; before the near weight it was `0.31` for plates and failed. `normalShare` is within `1.4e-4`.
- In game, empireu read `0.18 ms` of GPU time with cells of `0.25 degrees` against `0.25 ms` per pixel, and found the cells look better, stylized. The fill view showed no blue, so every cell read lay within the atlas.

## Remaining

- The streaks over the near tree, which stay with `pillarTexelDegrees` at `0` and so come from the pillar model. For a light far below the eye, the low part of its pillar comes from crystals within a few blocks of the eye, in front of the tree, which shadows them in reality. The horizons around each light sample `64` directions at `16` distances out to about `158` blocks, so a single tree near the eye can be missed. To investigate next, with the screen-space rule that step 3 of the shadow work removed as a candidate fix.
- Timing per debug mode with cells, to see whether the atlas pass, the screen pass, or the doubled vertex stage takes the time.
- The scene share for wobbling plates within the near fade is the roughest part of the approximation.
