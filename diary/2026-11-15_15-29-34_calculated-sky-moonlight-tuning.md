# Calculated Sky Moonlight Tuning

- Time: `2026-11-15 15:29:34 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done`
- Scope: `content/climate/render/sky` (`SkySetting`, `LightPillarRenderer`, `CalculatedSkyScreenshots`), `calculated_sky.fsh`, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `429625f56`. Follows `2026-11-15_13-54-24_calculated-sky-moonlight.md`.

## Completed

- **Brightness.** `moonlightBrightness` defaults to `0.0125`, half the first value ([Moon](../docs/climate/calculated-sky.md#moon)).
- **Pillars under the moon.** `LightPillarRenderer.daylightOpacity` fades them by the fog only while the frame's light is the sun, so under the moon they are at full strength, as before the moon ([Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars)).
- **The sun through snow.** The shader adds the sun's square after the weather graying, the fog, and the horizon fade, times `1 - rainLevel`, as `mix(D(sky + sun) - D(sky), D(sun), rainLevel)` ([Runtime](../docs/climate/calculated-sky.md#2-runtime)).
- **Harness weathers** `snowfall` and `blizzard`, with the levels the server sends a player ([Screenshot harness](../docs/climate/calculated-sky.md#screenshot-harness)).

## Decisions

- empireu chose `0.0125`, half the first value, having tried similar values in the night session; the plan's target of `0.10` sRGB near the zenith would be `0.015`.
- No weather law, which empireu suggested: the tables already follow the weather, thicker ice before snow dimming the moon about as much as doubling the gain, so a law would be a factor with no input from the sky model.
- empireu asked for pillars a little brighter at night, impressive but not obstructive; the screen blend already lowers their contrast against a brighter sky, so the fade by the fog now applies only under the sun.
- The moon showed through snow while the sun did not, since vanilla adds its sun and moon with alpha `1 - rainLevel`, which is `0.8` in snow. empireu decided both should show faintly; adding the square's own display at every rain level instead brightened any square that does not saturate the tone map by up to about `89` steps, so in clear weather the square adds its increment over the sky and the rain blends toward its own display.
- Step 5's plan to dim the moon sprite by the ice, which would hide it in snow, now says to change that first.

## Validation

- The mod and the GameTests compile, `glslangValidator` accepts the shader, and `fullMoonDisplaysAsANightSky` checks the full moon: at the default settings and the thinnest ice the sky `30 degrees` from the moon displays `(16.3, 20.4, 31.8)` steps and the fog `(29.0, 32.6, 44.1)`, and the new moon is under `0.03` steps. All `84` tests of `frostedheart_calculated_sky` pass, and the `109` hashes still equal those from before step 1.
- Two read-only Subordinate reviews confirmed the weathers' wiring and the pillars' continuity, and noted that the client only ever holds rain level `0` or `0.8` and that an existing config file keeps `moonlightBrightness = 0.025`; the brightening of unsaturated squares is fixed.
- empireu took `step2b` (the full and gibbous moon in clear weather and `snowfall`, the horizon, the torch field, the rig, and the sun in `clear`, `snowfall`, and `blizzard`): all `33` shots settled at `0.0125`, the top band of the full moon measured `(16.9, 21.1, 32.8)`, and he found the set and the pillars under the moon fine.

## Remaining

- The open checks of the moonlight entry: snow at dusk (`FogModification`), relog at night, `/time set` across a phase change, F3+T, Esc while the moon fades, the frame rate, the build times at the switch, and the day pair of `step2`.
- `SkyBaselineHashGameTests` stays untracked until the review of step 2 is done.
