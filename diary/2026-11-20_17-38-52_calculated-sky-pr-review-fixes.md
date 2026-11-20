# Calculated sky PR review fixes

- Time: `2026-11-20 17:38:52 +02:00`
- Author: `Claude (Opus 5.5), supervised by empireu`
- Status: Review comments on PR #248 answered, upstream `06ae74bf9` merged, compiled and checked in `runClient`.
- Scope: `CalculatedSkyRenderer.fogColor`, `CalculatedSkyRenderer.daylightColor`, `CalculatedSkyEffects.getSunriseColor`, `LightPillarSources.onClientTick`, `LightPillarWorldChanges.followLevel`, `LevelRendererMixin.frostedheart$hideVanillaSun`, `SkyGlPass.begin`, `LightPillarRenderer.draw`, `DustGlitterRenderer`, `FHClientEventsMod`.

## Changes

- `fogColor` and `daylightColor` catch `RuntimeException` like `render` does, call `fail`, and return null or false, so an error falls back to vanilla fog and sky light instead of crashing every frame.
- `LightPillarSources.onClientTick` passes every level change, including no level, to `LightPillarWorldChanges.followLevel`, so the work queue releases the old `ClientLevel` after logout.
- `getSunriseColor` hides the vanilla sunrise band only when the camera is not in a fluid, matching the early return in `render`. Vanilla water fog hides the sky, so the difference is hard to see.
- The `30.0F` sun size constant in `renderSky` is pinned with `ordinal = 0`. Vanilla and Forge 1.20.1 have it once, and `setShaderColor` ordinals 2 and 3 are the sun and moon color and the stars, read with mc-src-ts.
- The saved active texture unit comes from `GlStateManager._getActiveTexture()` instead of a `glGetInteger` query in the three draw paths. The rest of the state cache is private, so the other queries stay.
- Removed an import of `BlockTempStats`, which upstream deleted in `8090320ae` and an earlier merge kept.

## Validation and remaining work

- `compileJava compileGametestJava` pass with JDK 17. Two read-only reviewers found no bugs in the fixes.
- In `runClient` the sky, sun, moon, stars, pillars and glints look as before, and pillars return after quitting to the title screen and rejoining.
- Not tested in the full pack. Mixin conflicts with Oculus or Embeddium in `renderSky` stay unchecked.
- Documentation impact: none, the documented behavior does not change.
