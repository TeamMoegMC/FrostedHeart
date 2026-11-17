# Calculated Sky Screenshot Harness

- Time: `2026-11-07 16:39:59 +0200`
- Author: `Claude; Anthropic Opus 5.5; implementing agent for empireu (outside contributor)`
- Status: `complete`
- Scope: `content/climate/render/sky` (`CalculatedSkyScreenshots`, `CalculatedSkyRenderer`, `CalculatedSkyClientCommand`)

## Completed

- `CalculatedSkyScreenshots` adds `/calculatedSky screenshots [<set>|stop]` and `beginSet`, `pose`, `endSet`, `time`, and `weather`; every combination of a set's poses, times, weather, and variants becomes one screenshot with a line in `manifest.tsv` ([Screenshot harness](../docs/climate/calculated-sky.md#screenshot-harness)).
- The subcommands exist only while `CalculatedSkyScreenshots.ENABLED` is `true`, and a run, a hold, and a recording add their listeners when they start and remove them when they end. `CalculatedSkyRenderer.isSettled` reports whether the sky is drawn from tables, halo maps, and views built for the current config and sun elevation with no build pending, and `isAllowed` is public so the harness can tell a sky that cannot be drawn from one still building.
- The contributor's `ScreenshotGrid`, outside the repository, reads a manifest with `--manifest` and puts each variant in its own column.

## Decisions

- empireu asked for one command in a fresh world that takes the whole comparison set, behind a flag hardcoded in the Java code.
- Time, rain, thunder, and the view direction are held on the client, since `/time set` backwards adds a climate day each time and would bring the scripted storm closer.
- Poses teleport through the integrated server with `ServerPlayer.teleportTo`, so a run needs a singleplayer world but no cheats; a pose is a world position, an offset from the run's start, or neither.
- Variants set the config without saving it, and the run restores the config, view, weather, HUD, and position at its end, also when stopped or the world closes.
- A shot waits at least `1 s`, longer after a weather change or a move, and after `20 s` is taken anyway with the reason in the manifest, rather than ending the run.
- Embeddium `0.3.31` replaces `LevelRenderer.hasRenderedAllChunks` with `SodiumWorldRenderer.isTerrainRenderComplete`, by disassembly of its `WorldRendererMixin`, so the vanilla method still waits for the terrain.
- Snow and storms are only the client's rain and thunder levels; a real FrostedHeart storm needs a world that is in one.

## Validation

- `compileJava` with JDK 17 passed with no warnings in the sky package, and all `7` required GameTests passed; the harness runs only in a client with a world, so they do not cover it.
- `ScreenshotGrid --manifest` laid out a made-up run of three shots in two rows, leaving the missing cell empty and printing the shot that had not settled.
- empireu's first set took `36` shots (two poses, times `6000`, `12500`, and `12800`, clear weather and snow, the sky off, on, and on with ice depth `0.5`), all settled after `1` to `7 s`, but its level poses left the sun out of frame, and from `20` blocks up the snow fog hid all terrain. Sets gained groups, and chosen coordinates still missed good views, so empireu suggested recording poses by hand with their times, which gave `beginSet`, `pose`, and `endSet`, with `time` and `weather` holding what the client shows for framing. His set `v1` of six poses gave `12` frames, all settled at the frame cap of about `144` per second.

## Remaining

- empireu reviewed the harness and chose to commit `CalculatedSkyScreenshots.ENABLED` as `true` while the sky is developed on `feature/calculated-sky`. It is set to `false` before a pull request.
- Plan step 4 used the harness for its comparison, in `diary/2026-11-07_17-32-58_calculated-sky-daylight.md`.
