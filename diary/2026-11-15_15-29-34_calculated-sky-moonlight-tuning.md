# Calculated Sky Moonlight Tuning

- Time: `2026-11-15 15:29:34 +0200`
- Author: `Claude; Anthropic claude-opus-5-5; implementing agent for empireu (outside contributor)`
- Status: `done`
- Scope: `content/climate/render/sky` (`SkySetting`, `LightPillarRenderer`, `CalculatedSkyScreenshots`), `calculated_sky.fsh`, GameTests, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `429625f56`. Follows `2026-11-15_13-54-24_calculated-sky-moonlight.md`, which left the moon's brightness and three questions from empireu's night session open.

## Completed

- **Brightness.** `moonlightBrightness` defaults to `0.0125`, half the first value.
- **Pillars under the moon.** `LightPillarRenderer.daylightOpacity` fades the pillars by the fog only while the frame's light is the sun. Under the moon they are at full strength, as they were at night before the moon.
- **The sun through snow.** `calculated_sky.fsh` keeps the sun's square apart from the sky and adds it after the weather graying, the fog, and the horizon fade, times `1 - rainLevel`, as vanilla adds its sun. It adds `mix(D(sky + sun) - D(sky), D(sun), rainLevel)` with `D` the display of a radiance, which in clear weather is exactly what the square added inside the radiance.
- **Harness weathers.** `CalculatedSkyScreenshots` has the weathers `snowfall` and `blizzard`, with the levels the server sends a player, and records them for a client rain level from `0.5` to below `0.9`.
- **Docs.** The brightness, the shader's steps, the pillars' opacity, the harness weathers, and the known limits on the daylight and snow.

## Decisions

- empireu chose the brightness `0.0125`, having tried similar values in the night session. Screenshots at `0.025` put the sky about `15..30 degrees` from a full moon overhead near `(27, 33, 48)` of `255` and the horizon near `(44, 49, 64)` at blue shift `0.5` and the thinnest ice. Scaled to `0.0125` through the tone map, that is near `(17, 21, 33)` and `(30, 33, 45)`. The plan's target of `0.10` sRGB near the zenith would be `0.015`.
- No weather law on the brightness. empireu had suggested one. The tables already follow the weather: thicker ice before snow dims the moon and brightens the glow around it, about as much as doubling the gain, and snow fades the sky to the fog. A law would be a factor with no input from the sky model.
- empireu asked for pillars a little brighter at night, impressive but not obstructive. The fade by the fog was meant to hide them by day. With a moonlit fog it dimmed them to about `0.43` at `0.025`, or `0.57` at `0.0125`. The screen blend already lowers their contrast against a brighter sky, so the fade now applies only under the sun. The sun's light fades to black above the switch, so the opacity reaches `1` before the light changes.
- The moon showed through snow while the sun did not. The cause is the rain level: the server sends a player `0.8` in snow and blizzards (`PlayerWeatherCompatibilityModel.fromClimate`), and vanilla adds its sun and moon with alpha `1 - rainLevel`. So vanilla's moon showed at a fifth, as vanilla's sun did before the calculated sky. Inside the sky's radiance our sun was lost, because at the snow's ice depth the glow around it is as bright as the square. empireu decided that both should show faintly. My first version added the square's own display, `D(sun)`, at every rain level. A Subordinate review found that this brightens any square that does not saturate the tone map, by up to about `89` steps, for example a low sun through thick ice before snow, or the blue of a setting sun. So in clear weather the square adds its increment over the sky, which equals the old sum, and the rain level blends toward its own display.
- The harness's `snow` and `storm` keep rain level `1`, so the sets taken before stay comparable, and the play levels are new keys.
- Step 5 of the remaining work planned to dim the moon sprite by the ice, which would hide it in snow. The plan now says to change that before the step lands.

## Validation

- `bash agent-tools/build.sh`: the mod and the GameTests compile. `glslangValidator` accepts the edited `calculated_sky.fsh`.
- A new GameTest, written by a Subordinate session to a spec, which I reviewed: `fullMoonDisplaysAsANightSky`. At the default settings, the thinnest ice, and a full moon overhead, the sky `30 degrees` from the moon displays `(16.3, 20.4, 31.8)` steps and the fog `(29.0, 32.6, 44.1)`, within the ranges `6..35` and `15..55` of green set from the screenshots, below `64` in every channel, and bluer than red. The half moon is dimmer in every channel (sky `(1.6, 2.2, 4.3)`), and the new moon is under `0.03` steps. `bash agent-tools/gametest.sh frostedheart_calculated_sky`: all `84` tests pass, and the `109` hashes of `SkyBaselineHashGameTests` still equal those from before step 1.
- Two read-only Subordinate reviews of the diff. The harness review found the weathers wired through holding, set files, shots, the manifest, and recording, and the new enum constants safe to load on the client. It noted that the client only ever holds rain level `0` or `0.8`, since the server's ramp is not sent, and that an existing config file keeps `moonlightBrightness = 0.025`, as `run/config/frostedheart-client.toml` does. The shader review confirmed the pillars' opacity is continuous across both switches, and found the brightening of unsaturated squares described above, which is fixed.

- In `runClient` on `2026-11-15` empireu took the harness set `step2b` in `run/sky-shots`: the full and gibbous moon overhead in clear weather and in `snowfall`, the horizon, the torch field at day time `13000` and `18000`, the rig at `18000`, and the sun at day time `2000` in `clear`, `snowfall`, and `blizzard`, each with vanilla, the default sky, and ice depth `3`. All `33` shots settled at `moonlightBrightness` `0.0125`. The top band of the full moon overhead in the default sky measured `(16.9, 21.1, 32.8)`, as the GameTest computes. empireu found the set fine, and the pillars under the moon too, and said the team will tune the pillars to its liking.

## Remaining

- The open checks of the moonlight entry: snow at dusk (`FogModification`), relog at night, `/time set` across a phase change, F3+T, Esc while the moon fades, the frame rate, the build times at the switch, and the day pair of `step2` before and after.
- `SkyBaselineHashGameTests` stays untracked until the review of step 2 is done.
