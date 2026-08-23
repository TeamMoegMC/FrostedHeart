# Blizzard And White Curtain Rendering Architecture

- Time: `2026-08-24 04:02:41 +08:00`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `docs/climate/weather-rendering.md`, climate documentation index and lifecycle cross-references

## Completed

- Traced global blizzard and local white-curtain state from `WorldClimate` / `WhiteCurtainInfo` through the overwritten server weather cycle, per-player Vanilla weather packets, client precipitation Mixin, particles, sounds, and fog events.
- Added a current living document that separates active rendering paths from the unreferenced `BlizzardRenderer`, unused white-curtain rectangle helpers, and unregistered `DimensionSpecialEffectsMixin`.
- Recorded concrete optimization boundaries: render radius is mislabeled as density, the default blizzard loop covers 961 columns per frame, full-strength ground effects can attempt up to 200 samples per client tick, and weather transitions can send two strength packets per player tick.

## Decisions

- Documented white curtain as a server-side position-shifted climate event, not a distinct client renderer, because the client receives no curtain geometry and only renders its connection-local rain/thunder state.
- Kept `FHClimatePacket` HUD/forecast synchronization separate from Vanilla game-event packets that actually drive rendering.
- Recorded observed implementation mismatches as current behavior without changing source: global snow only enables server rain for `BLIZZARD`, white-curtain cache invalidation is hourly, the master render switch does not gate radius/particles/sounds/fog, and the sky Mixin is not registered.

## Validation

- Searched all main and test sources for `BlizzardRenderer`, `WhiteCurtainInfo`, `LevelRendererMixin`, `FogModification`, weather configuration consumers, packet constructors, and Mixin registrations.
- Verified the active Mixin list in `src/main/resources/frostedheart.mixins.json` and confirmed no direct automated weather-rendering tests currently exist.
- Validated relative Markdown links under `docs/climate/`, checked the edited documentation with `git diff --check`, and ran the focused Gradle Java compilation.

## Remaining

- Establish in-game CPU/GPU and packet baselines before changing the rendering pipeline; specifically compare normal snow, global blizzard, and two players on opposite sides of a white-curtain boundary.
- Add regression coverage for white-curtain phase/cache behavior and pure tests for the per-player rain/thunder transition before changing synchronization cadence.
