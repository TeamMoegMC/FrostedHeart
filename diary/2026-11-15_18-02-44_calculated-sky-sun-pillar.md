# Calculated Sky Sun Pillar

- Time: `2026-11-15 18:02:44 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done`
- Scope: `content/climate/render/sky` (`IceMirror`, `IceHaloFormulas`, `IceHaloMap`, `IceCrystalData`), the datasets `ice_halos/paths_*.csv` and `src.md`, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `ec1c851ab`. Step 3 of the remaining sky work, after `2026-11-15_15-29-34_calculated-sky-moonlight-tuning.md`.

## Completed

- **The sun pillar.** The end faces of plates mirror the sun into a pillar through its mirror image at elevation `-s`. `IceMirror` gives its shape: a Gaussian of `2 sigma` along the vertical and `2 sigma |sin g|` across, with `g = (v - s) / 2` the grazing angle, times `|sin g| R(|g|) / D(s)`, where `D` is the plate's mean area seen from the sun, its tilt included. `IceHaloFormulas.addMirrorPillar` spreads the plates' traced mirrored share over that shape, moving only the part inside the halo map out of the sun's bin, bin by bin from Gaussian masses. With the sun below the map, where its other light is dropped, the part inside the map is added.
- **A path class `end_mirror`** in the four `paths_*.csv`, taken out of `other`, read by `IceCrystalData` (`END_MIRROR_PATH = 4`, `OTHER_PATH = 5`). Every kind but plates puts it at the sun with `other`, as before.
- `IceMirror.pillarPhase` evaluates the pillar per steradian with an extra variance, for the diamond dust's glints below `-2 degrees` in a later step.
- **Docs.** The sun pillar under the halo maps, the path classes, the GameTests, and the known limits.

## Decisions

- The plan scaled the lobe to the level plate share `sin(s) R(s) / (sin(s) + 1.47 q cos(s))`, which is `0` with the sun at the horizon. Writing the lobe per steradian from the tilt density gives the same for level plates and keeps the wobbling plates' pillar at sunset, the moment real sun pillars show. empireu agreed.
- The preview's tracer (check V1 of the plan) gave the mirrored share for plates and wobbling plates from `-3` to `30 degrees`. The slab formula is `4%` (sun at the horizon) to `19%` (`10 to 15 degrees`) above it, past the plan's `15%`, since a slab's inside reflections partly leave a real plate through a side face. As the plan said for that case, the datasets were regenerated with `end_mirror` and the formula gives only the shape. The traced share is `0.039` at `0 degrees`, `0.143` at `3`, `0.18` at `5`, and `0.062` at `30` for plates, and `0.142` at `0` for wobbling plates. Without the class, the first version capped the moved light at each kind's `other` share.
- empireu asked that the pillar add to the sunrise and sunset and take nothing away. Only the sun's own bin, under the `8.6 degree` square, loses light, and only for a sun below about `5 degrees`. Everything else is added.
- The part of the pillar below the map stays at the sun as the plan said, so the map's total does not change while the sun is in it.
- Not changed: the sky's lowest `4.6 degrees` fade into the fog color, which has no ice light, and the pillar is brightest just there. To be judged from the harness set.

## Validation

- `bash agent-tools/build.sh` compiles. The temporary `SkyPillarProbeGameTests` was removed before the commit.
- empireu's agent added the mirror count to the preview (`mirrorshare` mode), then the `end_mirror` class to its `datasets` mode, both through `run.ps1`. Its checks: the `other` share reproduces the shipped files at all rows, the first four classes of the regenerated files are identical as text, `end_mirror + other` equals the old `other` within the last digit, and the preview's `checks` mode passes 41 of 41. Two read-only Subordinate reviews of its code found one inverted flag in its first pass, which it fixed before the numbers above.
- GameTests, written by a Subordinate session to a spec and reviewed by me, plus `mirrorPillarMovesThePartOfItsShapeInsideTheMap`, which I added after a review: see the doc's GameTest list. `bash agent-tools/gametest.sh frostedheart_calculated_sky`: 88 of 88, counting the temporary probe below.
- A temporary printing GameTest (`SkyPillarProbeGameTests`) read the sky with the pillar. At ice depth `0.1` toward the sun, `3 to 4 degrees` up, red is `93` of `255` against `24` five degrees to the side with the sun at `-4 degrees`, `95` against `35` at `-3`, and `98` against `49` at `-2`, so the pillar is about `1 degree` wide and gone by about `8 degrees` up. At depth `0.5` it is only `3` to `6` steps above its surroundings.
- Two read-only Subordinate reviews of the diff. The math review found the code right for the shipped data, a NaN for a plate with no tilt (fixed: the pillar's height has a floor), and that no test pinned the moved share (the new test). The cost review found no allocation, a cost of about `1 to 3%` of a map build, and two wrong doc sentences, both fixed.
- empireu took the harness set `sunpillar` (`run/sky-shots/sunpillar.json`, west at sun `6` to `-6 degrees`, east at `-3`, `0`, `3`, ice depths `0.1`, `0.5`, `1.5`) before the change, run `sunpillar-2026-11-15_17-12-19`, and after it, run `sunpillar-2026-11-15_18-09-53`. The second run was taken in a window `991` pixels tall, so the two were compared by eye, not by pixel. With the sun at `-3 degrees` and ice depth `0.1` a red column about `1 degree` wide and `4 to 5 degrees` tall stands above the horizon where the run before shows a faint smudge, and the arcs look unchanged. At `-1 degree` the difference is small.
- In `runClient` on `2026-11-15` empireu watched the pillar in a sunset: it lasts a few seconds, "a lot of work for such a short effect, but players may appreciate it".

## Remaining

- Not checked in game: a pillar over a hill, a sunrise, and Esc while it shows.
- The preview's own halo maps still put `end_mirror` at the sun, so its reference values have no pillar.
- `IceMirror.pillarPhase` waits for the diamond dust's glints.
