# Calculated Sky Adapted Exposure

- Time: `2026-11-16 21:30:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (closed by empireu on `2026-11-16` after a sunset in real time)
- Scope: `content/climate/render/sky` (`SkyAdaptation`, `SkyView`, `SkyViewSet`, `SkyFrame`, `CalculatedSkyRenderer`, `SkySetting`), `SkyAdaptationGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `ba9382fdc`. Step 5 of the remaining sky work, the dusk.

## Completed

- The exposure adapts to the sky's luminance (`SkyAdaptation.exposure`, switch `exposureAdapts`), by the key of Krawczyk, Myszkowski, and Seidel 2005 between a day and a night limit. The formula, the unit, and the defaults are in the docs under [Adapted exposure](../docs/climate/calculated-sky.md#adapted-exposure).
- It feeds the sky shader, the fog color, the terrain daylight, the star dimming, and the sunlit and moonlit glints. The moon sprite keeps the day exposure (`SkyFrame.dayExposure`). Lamp glints and light pillars do not use the exposure.
- The moon is at its real brightness, `moonlightBrightness` `2.5e-6`, which keeps empireu's tuned moonlit look at the night exposure. `moonlightFadeDegrees` is `1` (was `4`), so the moon is full at sun `-16`.

## Decisions

- With empireu: the key alone would show every day and every night alike, so it runs between limits that keep the tuned day and night. The day limit is the clear sky with the sun at the horizon (`800 cd/m2`), not the `5.5 degrees` first proposed, to stay near the current look. The night limit sits just below a full moon overhead, so the full moon is "factored in a little bit".
- No time course: the exposure follows the outdoor sky only, so caves and houses are not brightened.
- After the first harness run, empireu found the aerial views "incredibly bright". Adapting to the sky's mean let the fog toward the sun turn vivid orange at sun `-6`, and the twilight cut from red at `-8` to black at `-12`. Fixed by adapting to the larger of the mean and the brightest horizon (`SkyView.brightestHorizonRadiance`) and moving `moonlightSwitchDegrees` to `-15`. Rejected: the mean horizon, which still let the glow's red rise from `131` to `183` between `0` and `-6`.
- empireu stopped the work when it drifted into a patch after each run. The bright red band at `-6..-8` is the exposure's overshoot, recorded as a known limit with a steady alternative and not changed, since a sunset played in real time looked good.

## Validation

- `SkyAdaptationGameTests` (three tests, written by a Subordinate from an exact specification) and `CalculatedSkyGameTests.fullMoonDisplaysAsANightSky`: `115` of `115`. Their first run failed two on design values: the adaptation radiance left out the ground's glow, a third of the sky's light at sun `30`, and the night limit `0.025` lay above an overhead full moon's sky.
- Harness sets `adapt` and `moonfade`, runs `adapt-2026-11-16_21-37-36`, `adapt-2026-11-16_22-35-11`, and `moonfade-2026-11-16_22-53-39`. With the defaults the twilight lasts to about sun `-12`, where the fixed exposure reaches the floor at `-4` on the ground. Day at sun `0` and night from `-16` match the fixed exposure within about `2` steps.
- Two Subordinate reviews. Fixed from them: limits given the wrong way round, a luminance that is not a number, and the manifest printing `0.000` for the moon's brightness.

## Remaining

- A saved client config keeps the old `moonlightSwitchDegrees`, `moonlightFadeDegrees`, and `adaptationNightLuminance`, and Forge clamps a saved `moonlightBrightness` of `0.0125` to the new maximum `1e-4`. Remove those lines to get the defaults.
- Known limits in the docs: the fog color at height, the one-light gap before the moon, and the exposure's overshoot. The terrain light at dusk was explained on `2026-11-17`.
