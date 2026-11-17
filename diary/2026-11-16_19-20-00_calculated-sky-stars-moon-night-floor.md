# Calculated Sky Stars, Moon Sprite, And Night Floor

- Time: `2026-11-16 19:20:00 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `done` (the night floor becomes airglow under an adapted exposure)
- Scope: `content/climate/render/sky` (`NightSkyDimming`, `CalculatedSkyRenderer`, `SkyViewSet`, `SkyFrame`, `SkyDisplay`, `SkySetting`), `LevelRendererMixin`, `calculated_sky.fsh`, `NightSkyGameTests`, `docs/climate/calculated-sky.md`. Branch `feature/calculated-sky`, after `4ab060baf`.

## Completed

- **Stars, moon sprite, and night floor.** While the calculated sky is drawn, vanilla's stars are dimmed by the ice and by the sky's own brightness, the moon sprite takes the sun's square's transmittance at its place, and the sky has a floor tinted like moonlight that the fog also takes. The formulas, the settings `starTwilightDisplay` (`0.25`) and `nightSkyFloor` (`0.04`), and the hooks are under [Stars, the moon sprite, and the night floor](../docs/climate/calculated-sky.md#stars-the-moon-sprite-and-the-night-floor).
- Both factors are computed once per frame after the draw (`CalculatedSkyRenderer.updateNightSky`) and used only while the sky was drawn in the frame. The hooks are `@ModifyArg` handlers in `LevelRendererMixin`, which allocate nothing, instead of the plan's `@Inject` at the return of `ClientLevel.getStarBrightness`, whose callback is allocated on every call.

## Decisions

- The moon sprite takes option (a), decided with empireu on `2026-11-16`: the tone mapped transmittance of the moon's disc as the sun's square has it, not `exp(-d / max(sin e, 0.05))`, which would hide the moon in snow, and not a factor only outside snow. It is per channel, so the moon also reddens low, and it takes neither `moonlightBrightness` nor the phase, since the sprite's texture shows the phase.
- The sky overhead for the stars counts the ice's tumbling crystals at the angle between the zenith and the light, but at least `45 degrees`, so a moon overhead does not dim every star by its own glare.
- The floor is tinted by the moonlight's blue shift, so it follows empireu's choice of the night's color.

## Validation

- The mod and both source sets compile, and `glslangValidator` compiles `calculated_sky.fsh` as `#version 150`.
- `frostedheart_calculated_sky`: `112` of `112`. `NightSkyGameTests` (three tests), written by a Subordinate from an exact specification, checks the star scale against its closed form and its value with a black sky and no ice, the moon color against the sun's square at the moon's elevation, and the floor's luminance and tint.
- Reviews by two Subordinates confirmed the ordinals `2` and `3` of `setShaderColor`, the frame gating, the floor in the shader and the fog, and allocation; fixed from them, wording only. Left: ordinal injection would retarget if another mod added a `setShaderColor` call to `renderSky`.
- empireu took `step5` before (`step5-2026-11-16_19-08-13`) and after (`step5-2026-11-16_19-52-09`), `125` shots each, all settled: the star light at sun `-18 degrees` was `85%` of the run before at ice `0.02`, `44%` at `0.4`, `12%` at `1`, and none at `3`; the moon turns amber at `6 degrees` through ice `0.4`, and the floor lifts a moonless sky to about `10` of `255`.

## Remaining

- The harness set `dusk` (before `dusk-2026-11-16_19-10-38`, after `dusk-2026-11-16_19-54-30`) showed that no fixed exposure gives a dusk: exposure `24` washes out the sky at sun `5 degrees`, and at `-6 degrees` even `24` is black while vanilla is still orange. empireu chose to base the dusk physically: an exposure adapted to the sky's luminance, in which the night floor becomes airglow as a radiance and `moonlightBrightness` may become the real moon's.
