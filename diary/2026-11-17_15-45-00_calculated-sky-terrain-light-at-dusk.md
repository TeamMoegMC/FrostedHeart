# Calculated Sky Terrain Light At Dusk

- Time: `2026-11-17 15:45:00 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done` (investigation; no code change kept)
- Scope: `SkyViewSet.daylightColor`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `8592bc71c`. A follow-up to the adapted exposure, which was expected to keep the terrain lit through civil twilight and did not.

## Completed

- **Measured why the adapted exposure leaves the terrain light unchanged.** Temporary logging after each harness shot printed every input of `SkyViewSet.daylightColor` and the lightmap change of `CalculatedSkyEffects`. Harness set `terrain` (pose on the ground, sun `10` to `-20 degrees`, variant `defaults` against the same with `exposureAdapts` off), run `terrain-2026-11-17_14-58-39`.
- **Cause.** From sun `0` to `-4 degrees` the adapted exposure rises about `20` times, from `6` to `117`, while the ground radiance falls about `27` times, from `0.00267` to `0.0000993`. So the snow as the sky's exposure displays it, `(0.110, 0.082, 0.113)` at `-4`, is already below vanilla's night light `(0.1152, 0.1152, 0.24)`, and the `max` in `daylightColor` shows the night light with and without adaptation. Adaptation lifts the red channel above it only at `-2` (`0.137` against `0.115`). The light scale stays `1` to `-12 degrees`, and the division by snow under a unit sun moves only from `0.90` to `1.00`, so neither is the cause.
- Snow near the eye on screen, measured by `scratch/measure/snow.py`: `(84, 79, 115)` at sun `0`, `(80, 74, 115)` adapted and `(72, 74, 115)` fixed at `-2`, and `(72, 74, 115)` in both from `-4` on, which is the night light.
- Docs: the known limit on the daylight now gives this cause, and the last-verified line names the set.

## Decisions

- **Tried and reverted: adding the displayed snow to the night light**, `V_night + (1 - V_night) C`, in place of the larger of the two. In run `terrain-2026-11-17_15-26-32` it gave a smooth dusk on the terrain to about `-10 degrees` with adaptation, but it changes the light at every sun height below noon: snow at sun `4 degrees` went from `(132, 127, 130)` to `(149, 146, 166)`, lavender from the night light's blue, and at `-4` the terrain was brighter than vanilla's. Two read-only reviewers (DeepSeek, GLM) found no defect in it, and DeepSeek pointed out the change at low sun. A soft maximum would keep the day but lose most of the dusk. empireu chose to revert, since the night light stands in for gameplay, not for a real light that adds, and a sunset seen in real time on `2026-11-16` already looked good.
- So the terrain reaches the night light between sun `-2` and `-4 degrees` with adaptation, where vanilla's light reaches it near `-6`. That is `2` to `4 degrees` earlier, about `6` to `12 seconds` at normal speed.

## Validation

- `bash agent-tools/gametest.sh frostedheart_calculated_sky` after the revert: all `115` required tests pass.
- The tree after the revert matches `8592bc71c` in code. Only this entry and the docs change.

## Remaining

- Nothing for the terrain light. Next in the remaining sky work: the day and night grids for the maintainers and the pull request checklist.
