# Calculated Sky Own Sun

- Time: `2026-11-07 14:34:26 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`SkyTables`, `SkyView`, `CalculatedSkyRenderer`), `LevelRendererMixin`, `assets/frostedheart/shaders/calculated_sky.fsh`

## Completed

- The shader draws the sun as a square like the core of vanilla's sun texture, and each texel takes the color of its own elevation ([Sun](../docs/climate/calculated-sky.md#sun)). The outline uses each pixel's own direction, so the sun glides with sharp edges like vanilla's quad.
- `LevelRendererMixin.frostedheart$hideVanillaSun` gives vanilla's sun quad no size when the calculated sky drew its sun in the same `renderSky` call; the moon and stars stay vanilla.
- Sky views now exist for sun elevations on a `0.25 degree` grid, and the shader and the fog color blend them by the exact sun elevation ([Runtime](../docs/climate/calculated-sky.md#runtime)).

## Decisions

- Vanilla adds its sun on top of the sky; empireu saw the core as a square in the sky's glow at ice optical depth `0.1` and agreed to replace it.
- empireu chose the color of each texel's own elevation over one color for the whole square, so the bottom reddens first at sunset and the horizon cuts the square off without a separate test. It has no brightness setting, since the sky's glow is already white out to `2..3 degrees`.
- empireu asked for the sun to glide smoothly. The first version faded the edge rows and columns, and the sun seemed to change from one state to another: edge texels made it look as if it changes shape every `1.7 s`, and every new sky view, every `0.25 degrees` or `0.8 s`, shifted the sky near a low sun by up to `7` display steps of `255` at sun `4.6 degrees`. The outline moved to screen pixels and the views are blended like the halo maps.
- Vanilla's sun is hidden by `@ModifyConstant` on its half size `30.0F`, which Oculus and Embeddium do not touch. The flag is cleared at the start of every render and set after a successful draw, so vanilla's sun returns underwater, while tables build, outside the overworld, with a shader pack, and after an error.

## Validation

- Outside the game, the new `SkyTables` gave sun transmittances within `3%` of `SkyTransmittance`, and two views `0.25 degrees` apart blended halfway stayed within `1` display step of a view for the sun's exact elevation.
- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky` with JDK 17: all 7 required tests passed, including the new `sunReddensTowardTheHorizonAndSetsBelowIt`, before and after the blended views. Docs updated.
- `runClient` with the first version: empireu found the low sun striking, and the square sat on vanilla's core within `2` to `4` pixels. The sun seeming to change state led to the pixel outline and blended views, which empireu confirmed in game, along with the sky and sun looking the same underwater.

## Remaining

- empireu found ice optical depth `0.1` too little and asked for the ice and nanobot optical depths to follow a weather law, a separate change.
