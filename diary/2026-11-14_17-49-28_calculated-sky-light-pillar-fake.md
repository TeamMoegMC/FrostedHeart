# Calculated Sky Light Pillar Fake

- Time: `2026-11-14 17:49:28 +0200`, revised `2026-11-14 18:13 +0200` and `2026-11-14 19:10 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarShelter`, `LightPillarClusters`, `LightPillarRenderer`), `shaders/light_pillars.glsl`, GameTests. Branch `feature/pillar-fake`, uncommitted.

## Completed

- Pillars are drawn as a fake closed form instead of the integral along the view ray, after `2026-11-14_15-50-53_calculated-sky-light-pillar-cells.md`.
- `LightPillarClusters` merges only lights that touch, replacing the `8` by `8` by `8` cells, splitting only at section boundaries.
- `LightPillarShelter` replaces `LightPillarHorizons`: a cap from the light's own column, or the highest of its `4` side columns' caps for a covered full block, and `32` rim directions to the steepest wall within `2` blocks.
- `light_pillars.glsl` computes the closed form with the dust density and the optical depth along both legs; the fade in, the cap, and the near fade weigh the value only, so glints dim without thinning, and a cluster's pillar is widened across by `a = halfWidthAcross / d`.
- New settings `pillarFadeInSigmas` (`2`), `pillarBaseShare` (`0.5`), `pillarDepthBand` (`0.5` blocks); `shelters[48]` and `spans[48]`, and tests such as `lightClustersMergeLightsThatTouch`.

## Decisions

- The rim, the cap, and the fade in are in the value function, so cells apply them at their center; the scene test runs before the value.
- `LightPillarShelter` builds every marked slot in the tick that marks it, without a budget; an open cap is stored as an infinite height.
- First round, changed after review: the fade weight thinned the glitter count, and the rim was a height on the sheet, lifting nothing for a light below the eye (empireu: every pillar started at the same height whatever the walls).
- Third round, after empireu's second look: the rim ignored walls in the `4` side columns, a pillar showed above an inverted U, and torches `2` or `3` apart merged; empireu asked to stop the cells and merge only touching lights.
  - empireu chose the rim toward the eye over the plan's rim, which lifts a beam only inside an enclosure: walls lift a pillar only on their side.
  - The plan's `8` by `8` by `8` limit is dropped, since it would split a 3 by 3 floor at `x` `7` to `9`, and its half-block span would give `1.43 degrees` at `20` blocks against `0.27 degrees` of spread, so lights are points.
  - The plan's glitter count per cluster would make merged glints `N` times sparser and brighter, so it is times the light count.
  - A covered full block uses its side columns' caps, since a one-block beam over a glowstone otherwise showed through; the shelter is read at a light's block center, not the cluster's middle.
- Limits kept: a wide cluster's walls and cap are read at one light, and wider pillars are drawn per pixel ([Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars)).

## Validation

- `glslangValidator` passes all four stages, and both source sets compile.
- Calculated sky GameTests: all `64` in the second round, the `57` before and `7` new, and all `66` in the third.
- Subordinate reviews, read only, led to the shelter light, the cap of a covered full block, and dropping the cell limit.
- The closed form against `dustRadiance`: plates worst `1.088` times, wobbling `1.57`.
- In game, by empireu: `0.13 ms` in the first round, `0.11 ms` in the third; a roofed shelter light hides a glowstone floor's pillar, while the rest stays open.

## Remaining

- empireu judges this round: a plus of walls, a full ring, and a plus with a gap from both sides, an inverted U, glowstone in a wall, and torches `2` apart and touching.
- The GPU time of this round is not measured; wide pillars cost two `exp` per pixel.
- `docs/climate/calculated-sky.md` is not updated, as the fake is not adopted; removing `LightPillarHorizons` and the integral twins follows.
