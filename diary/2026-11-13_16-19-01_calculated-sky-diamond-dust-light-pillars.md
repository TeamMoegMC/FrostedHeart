# Calculated Sky Diamond Dust Law And Light Pillars

- Time: `2026-11-13 16:19:01 +0200`
- Author: `Claude; Anthropic Opus 5.5, with a DeepSeek reviewer through pi; implementing agent for empireu (outside contributor)`
- Status: `completed`
- Scope: `content/climate/render/sky` (`DiamondDustLaw`, `IceMirror`, `LightPillarOptics`, `LightPillarSources`, `LightPillarRenderer`, `SkyDrawTimer`, `SkySetting`), `shaders/light_pillars.glsl`, `datasets/light_pillars`, GameTests, `docs/climate/calculated-sky.md`

## Completed

- Steps A and B of the visuals plan in four commits on `feature/calculated-sky`: `9ce1bf828` the dust law, `4cb9578b9` `IceMirror` and the view-ray integral, `671fe0ca7` the scan, and `cb15106e6` the drawing. How it works is in [Light pillars](../docs/climate/calculated-sky.md#light-pillars), [Diamond dust from the weather](../docs/climate/calculated-sky.md#diamond-dust-from-the-weather), [Lights for light pillars](../docs/climate/calculated-sky.md#lights-for-light-pillars), and [Drawing light pillars](../docs/climate/calculated-sky.md#drawing-light-pillars).

## Decisions

- empireu's answers to the plan on `2026-11-11`: the dust follows every snow under the sky's own switch, and no aurora until this agenda is done.
- Pillars are drawn per pixel on ribbons `1` block from the eye without a depth test, since the crystals that light one pixel spread along its view ray; on sky texels they slid and morphed as the eye moved (`runClient`, `2026-11-12`).
- For the scan empireu chose double-buffered passes, a configurable radius, and the override table, and left the timing to the instrumentation.
- The light side of the shadow was a screen-space rule, scene points horizontally nearer than the light getting no pillar light; it misses walls around a light, and the next entry replaces it.

## Validation

- GameTests: `18` of `18` after step A, `24` of `24` after the scan, `30` of `30` after the drawing; both shader stages passed `glslangValidator`.
- empireu confirmed the dust law in `runClient`; the scan went in after a code review without an in-game run, its tick cost measured at `0.120 ms` per tick in the next entry.
- empireu's three looks at the drawing on `2026-11-12` found sliding texels, pillars bending with view bobbing, too low a resolution, `BRIGHTNESS` at `5009`, pillars on a wall in front of distant torches, and the pillar above a ring of walls.
- A read-only review by a second model through pi fixed the saved blend equation, half-loaded shaders, cleanup after a failure, and docs. The full `frostedheart` namespace was not run.

## Remaining

- Overlapping pillars show up to `1.29` times too bright, since each ribbon is sRGB-encoded before the screen blend; an accumulation target would fix it.
- A fade of crystals within `8` to `24` blocks of the eye was proposed and not decided.
- Pausing can step the climate clock back and snap the ice and dust to their targets; empireu wants to see first whether the snap is visible.
- Glints, the sun pillar, and moonlight from the plan are not started, and the plan has no file in `plans/` yet.
- `CalculatedSkyClientCommand.ENABLED` goes to `false` before a pull request.
