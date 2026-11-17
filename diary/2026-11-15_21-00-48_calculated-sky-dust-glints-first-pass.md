# Calculated Sky Dust Glints First Pass

- Time: `2026-11-15 21:00:48 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `in progress` (first pass committed as a checkpoint; its look is to be replaced)
- Scope: `content/climate/render/sky` (`DustGlitterField`, `DustGlitterRenderer`, `SkyView`, `IceHaloMap`, `SkySetting`), `diamond_dust.glsl`, `DustGlitterGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `ee35448a1`. Step 4a of the remaining sky work, after `2026-11-15_18-02-44_calculated-sky-sun-pillar.md`.

## Completed

- A first pass at the glints of diamond dust near the eye: one draw of points with no vertex data at `AFTER_WEATHER`, with the position, the copy nearest the eye, the fade, the nested extinction share, the height weight, the light, and the phase as in [Glints of diamond dust](../docs/climate/calculated-sky.md#glints-of-diamond-dust), each point a round sprite with a tapered Gaussian core (`dustGlitterSize`).

## Decisions

- The first calibration assumed `80%` of sunlight at the eye; at a `10 degree` sun through ice of depth `0.1` it is about `0.2`, `0.1`, and `0.04` per channel, so single pixel glints were `10` to `20` steps over a gray sky and invisible. Sprites and a gain of `20` made them visible.
- empireu found the look jarring and a little seizure inducing: glints seem to appear and disappear at random and do not move. Against empireu's video of real diamond dust, count (about `70` to `100` lit), size (about `4 px`), and rise (`50` to `65` of `255`) match, but the reference's glints fall `25` to `43 px` per frame on straight paths while lit for about `0.2 s`, where ours move `0.5 px` per frame, flash, and light again on their sway circle `1` to `3 s` later.
- empireu agreed to commit this pass as a checkpoint and redesign the look as falling glitter: stateless lives per point, each a new crystal falling about `0.5` blocks/s within `30 degrees` of vertical, lit steadily for `0.15` to `0.7 s`, more glints rather than brighter ones on the halos, rare faint near glints, and instanced quads. A CPU simulation with world access, collisions, and entities was rejected for cost, state, and allocations.

## Validation

- Calculated sky GameTests: `93` of `93`.
- Subordinate reviews: math and GL state of the points, then GL and brightness of the sprites. Fixed: the height as a brightness weight (points popped at a height threshold), a neutral floor color, and doc sentences.
- In `runClient` with `/calculatedSky set dustExtinction 1`: draws, GPU about `0.005 ms`, CPU about `0.02 ms`, `0` bytes allocated once warm. Harness runs `dust-2026-11-15_19-41-23` and `_19-51-16` against `_18-37-58`.

## Remaining

- The falling glitter redesign, then empireu's look, a recording measured against the reference, the harness set, and the U4 values.
- Glints show indoors and in caves until step 4b.
