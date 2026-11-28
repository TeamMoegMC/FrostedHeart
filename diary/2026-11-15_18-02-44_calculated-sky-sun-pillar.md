# Calculated Sky Sun Pillar

- Time: `2026-11-15 18:02:44 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done`
- Scope: `content/climate/render/sky` (`IceMirror`, `IceHaloFormulas`, `IceHaloMap`, `IceCrystalData`), the `ice_halos` datasets, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `ec1c851ab`. Step 3 of the remaining sky work.

## Completed

- The sun pillar, with its shape and traced `end_mirror` share under [Ice crystals and halos](../docs/climate/calculated-sky.md#ice-crystals-and-halos); `addMirrorPillar` moves only the part inside the halo map out of the sun's bin.
- The path class `end_mirror` in the four `paths_*.csv`, out of `other`, read by `IceCrystalData` (`END_MIRROR_PATH` `4`, `OTHER_PATH` `5`); every kind but plates puts it at the sun with `other`.
- `IceMirror.pillarPhase` gives the pillar per steradian with an extra variance, for the diamond dust's glints in a later step.

## Decisions

- The plan's level plate share is `0` with the sun at the horizon, so the lobe is written per steradian from the tilt density, which keeps the wobbling plates' pillar at sunset, when real ones show. empireu agreed.
- The tracer's mirrored share runs from `-3` to `30 degrees`; the slab formula is `4%` to `19%` above it, past the plan's `15%`, since a slab's inside reflections partly leave a real plate through a side face. So the datasets were regenerated with `end_mirror` and the formula gives only the shape; the first version had capped the moved light at each kind's `other` share.
- empireu asked that the pillar add to the sunrise and sunset and take nothing away; only the sun's own bin, under the `8.6 degree` square, loses light, and only below about `5 degrees`.

## Validation

- `build.sh` compiles; the temporary `SkyPillarProbeGameTests` was removed before the commit; the GameTests, written by a Subordinate to a spec and reviewed by me, pass `88` of `88`.
- Two read-only Subordinate reviews: fixed a NaN for a plate with no tilt (the height has a floor) and two wrong doc sentences, and added `mirrorPillarMovesThePartOfItsShapeInsideTheMap` to pin the moved share; cost `1 to 3%` of a map build.
- empireu took the harness set `sunpillar` before and after the change, runs `sunpillar-2026-11-15_17-12-19` and `sunpillar-2026-11-15_18-09-53`: at sun `-3 degrees` and ice depth `0.1` a red column about `1 degree` wide and `4 to 5 degrees` tall stands where the run before shows a faint smudge.
- In `runClient` on `2026-11-15` empireu watched the pillar in a sunset: it lasts a few seconds, "a lot of work for such a short effect, but players may appreciate it".

## Remaining

- Not checked in game: a pillar over a hill, a sunrise, and Esc while it shows.
- The preview's halo maps still put `end_mirror` at the sun, so its reference values have no pillar.
- `pillarPhase` waits for the diamond dust's glints.
