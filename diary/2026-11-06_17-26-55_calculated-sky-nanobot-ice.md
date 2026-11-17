# Calculated Sky From Nanobot And Ice Scattering

- Time: `2026-11-06 17:26:55 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky`, `LevelRendererMixin`, `FogRendererMixin`, `FHConfig` group `CalculatedSky`, `calculated_sky.fsh`, GameTest namespace `frostedheart_calculated_sky`

## Completed

- Added an optional overworld sky calculated with the lookup tables of Hillaire 2020, off by default ([Model](../docs/climate/calculated-sky.md#1-model)), toggled at runtime through the client config group `CalculatedSky` and `/calculatedSky`; later stages in [the plan](../plans/2026-11-05_21-16-30_calculated-sky.md).
- Split the ice phase into a diffraction peak, a broad lobe, and the `22 degree` ring, with a fade in snow ([Ice crystals and halos](../docs/climate/calculated-sky.md#ice-crystals-and-halos)), and took the fog color outside snow from the sky's horizon toward the camera ([Fog color](../docs/climate/calculated-sky.md#fog-color)).

## Decisions

- Tables are built on the CPU on `Util.backgroundExecutor()`, because the Java model was checked against an offline Monte Carlo reference and a GLSL port would need checking again.
- The dome is an inject after the first `VertexBuffer.unbind()` in `LevelRenderer.renderSky`, so vanilla keeps its sun, moon, stars, and exits for fog and blindness.
- The three-part ice phase replaces a single lobe of asymmetry `0.8` that made the halo too big; the ring stayed at `22 degrees`, from the prism angle of ice.
- Snow first faded toward the gray of a uniform sky giving the snow the same light, but that gray was darker and more neutral than the pack's lavender snow fog, leaving a horizon band; it now fades toward the fog color by the rain level.
- The fog color is a mixin in `FogRenderer.setupColor` before its call to `getPriorityFogFunction`, not `ViewportEvent.ComputeFogColor`, which fires after vanilla's darkening for blindness, darkness, and bosses and would undo it; it leaves out the diffraction peak and the ring and replaces the orange tint of the removed sunrise fan.
- Single scattering in the style of Unity's The Blacksmith was rejected offline as too dark and too olive over snow.

## Validation

- `runGameTestServer -PgameTestNamespaces=frostedheart_calculated_sky` on `2026-11-06`: 4 of 4 passed, covering a published Mie value, the Rayleigh limit, phase normalization, the ice asymmetry, and the default sky against the preview.
- `runClient` by empireu: the toggle worked both ways, the halo was centered on the vanilla sun, and the frame rate stayed at the `144` cap. Finishing the snow fade at `0.8` was tried and undone, because empireu likes the faint ring and it matches vanilla's sun, which also stays at a fifth.

## Remaining

- Try blindness in clear weather, where the fog should turn black as in vanilla, and check whether the ring shows through a blizzard; if it does, finish the fade by the thunder level.
- Compare pixels with the preview and go through the plan's edge cases, then the later stages: daylight color, aerial perspective, and ice that follows weather.
