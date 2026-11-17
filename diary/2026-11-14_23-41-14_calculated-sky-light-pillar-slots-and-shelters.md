# Calculated Sky Light Pillar Slots And Shelters

- Time: `2026-11-14 23:41:14 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`LightPillarRenderer`, `LightPillarShelter`, `LightPillarClusters`), `light_pillars.glsl`, new `light_pillars_composite.fsh`, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/pillar-fake`, uncommitted, after `60a159a58`. Steps 1 to 4 of the pillar slots plan.

## Completed

- `256` slots in the `PillarSlots` block and a linear sum tone mapped by `light_pillars_composite.fsh`, so overlaps no longer reach up to `1.29` times too bright: [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).
- Walls per light: `LightPillarShelter` reads one light, so a cluster's shelter is copied into its slot each tick and new slots fade in at once.
- A rim box joins the cap box, and a column that gets its light queues the sections of the `8` columns around it.
- Ceilings for overhangs: the bottom of the first block that stops light, folded into the cap as `d tan(c)` (`LightPillarOptics.shelteredCap`).

## Decisions

- Clusters merge only when their caps are equal and every rim and ceiling rounds to the same `2` degree step, and a cluster keeps its shelter light's exact angles, not the rounded ones.
- One working tree for all four steps, since empireu asked for as much as possible while away; step 1's shelter build budget was left out, as step 3 removes the per-slot builds it was for.
- The atlas grows `2` bands at a time; `R11F_G11F_B10F` would halve the `16.6 MB` with `6` and `5` bit mantissas.
- A change above a light's cap but inside its rim box queues its section too, which rewalks one section more than needed and keeps the boxes simple.
- A chunk unload does not queue the sections around it, since their lights keep the walls read while it was loaded; the next load queues them, where the old shelter re-read them.

## Validation

- `build.sh` and `glslangValidator` pass.
- GameTests `70` of `71` first, the porch test's ceiling off by `1e-4` from `SQUARE_MARGIN`, now compared with the ceiling the slot holds; all `71` pass after the fixes.
- Two read-only Subordinate reviews: fixed the ribbons losing the daylight and rain opacity once it moved into the composite (now `summedOpacity`) and two stale javadocs.
- In game by empireu on `2026-11-15`: the repro row matches the GameTest, and empireu chose `pillarBrightness` `0.3` instead of `1`, since `0.5` may look strange to players.

## Remaining

- Not checked in game: the porch platform, overlapping torch fields, single pillars, the pillar line in the most lit place, and GPU time against `60a159a58`.
- Step 5, a longer reach, was left out with empireu's agreement.
- Accepted: glowstone in a snowy floor gets rims from the snow layers beside it, so such a floor can split into many clusters; torches do not.
