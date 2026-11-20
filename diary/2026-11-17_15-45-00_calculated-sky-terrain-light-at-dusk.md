# Calculated Sky Terrain Light At Dusk

- Time: `2026-11-17 15:45:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek and GLM subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (investigation; no code change kept)
- Scope: `SkyViewSet.daylightColor`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `8592bc71c`. A follow-up to the adapted exposure, which was expected to keep the terrain lit through civil twilight and did not.

## Completed

- Measured every input of `SkyViewSet.daylightColor` with temporary logging in harness set `terrain` (sun `10` to `-20 degrees`, defaults against `exposureAdapts` off), run `terrain-2026-11-17_14-58-39`.
- Cause: from sun `0` to `-4 degrees` the adapted exposure rises about `20` times while the ground radiance falls about `27` times. So the displayed snow, `(0.110, 0.082, 0.113)` at `-4`, is already below vanilla's night light `(0.1152, 0.1152, 0.24)`, and the `max` in `daylightColor` shows the night light with and without adaptation. The light scale and the division by snow under a unit sun are not the cause.
- Docs: the known limit on the daylight now gives this cause.

## Decisions

- Tried and reverted: adding the displayed snow to the night light, `V_night + (1 - V_night) C`, instead of taking the larger. In run `terrain-2026-11-17_15-26-32` it gave a smooth dusk to about `-10 degrees`, but it changed the light at every sun height below noon: sunlit snow at sun `4 degrees` went from `(132, 127, 130)` to `(149, 146, 166)`, lavender, and at `-4` the terrain was brighter than vanilla's. A soft maximum would keep the day but lose most of the dusk. empireu chose to revert, since the night light stands in for gameplay, not for a real light that adds, and a sunset seen in real time on `2026-11-16` already looked good.
- So the terrain reaches the night light between sun `-2` and `-4 degrees` with adaptation, where vanilla reaches it near `-6`: about `6` to `12` seconds earlier at normal speed.

## Validation

- After the revert: `115` of `115` calculated sky GameTests, and the code matches `8592bc71c`.
- Two read-only reviews of the reverted change (DeepSeek, GLM) found no defect. DeepSeek pointed out the change at low sun.

## Remaining

- None for the terrain light.
