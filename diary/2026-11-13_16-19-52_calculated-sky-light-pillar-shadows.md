# Calculated Sky Light Pillar Shadows And Event-Driven Light Tracking

- Time: `2026-11-13 16:19:52 +0200`
- Author: `Claude; Anthropic Opus 5.5, with DeepSeek subagents through Subordinate; implementing agent for empireu (outside contributor)`
- Status: `partial`
- Scope: `content/climate/render/sky` (`LightPillarWorldChanges`, `LightPillarHorizons`, `LightPillarOccluders`, `LightPillarRenderer`), the `*_LightPillars` mixins, `light_pillars.glsl`, `passes_light.csv`, GameTests. Branch `feature/calculated-sky`, after `2026-11-13_16-19-01_calculated-sky-diamond-dust-light-pillars.md`.

## Completed

- `8a548e97e`: `LightPillarWorldChanges` receives every client-side world change the pillars depend on, through the `*_LightPillars` mixins on `setBlocksDirty`, `onLightUpdate`, and `readSectionList`, and Forge's `ChunkEvent`.
- `8b8cdb663`: the light scan is an index of every loaded chunk that changes only on those events, replacing the passes and surface heights. `LightPillarWorkQueue` queues a chunk when its sky light arrives and a section when its sky light or an emitter changes.
- Uncommitted: shadows. `LightPillarHorizons` keeps `64` azimuth bins of `16` samples per slot, each the highest angle above the light's top at which blocks stop its light, `LightPillarOccluders` deciding what stops light. The shader drops the crystals whose light leaves the light below it; the screen-space rule and the cloud height uniform are gone.

## Decisions

- empireu decided to index every loaded chunk, to hold a slot's fade-in until its horizons are built, and that glass does not stop light: blocks without collision and those in `passes_light.csv` let light through, every other block stops it at the top of its outline shape.
- Embeddium overwrites `LevelRenderer.setSectionDirty`, so the hooks sit on the client level, the chunk cache, and the packet listener instead.
- Standing still the counters showed about `1,670` block changes a second, so the scan skips changes that keep the block and its emission.

## Validation

- `33` of `33` GameTests after the second step, `0.009 ms` per tick standing still against `0.120 ms` for the passes. empireu checked placing, breaking, covering a torch, and relogging.
- A Subordinate review led to four fixes, among them bin rebuilds starting where the last ran out. All `42` tests pass after the shadows, with rows as wide as the half rows next to them.
- In empireu's scene the torches in log rings, behind a log, and under a tree show no pillar; the tree one is a false shadow, as the highest block of a column counts as a wall.

## Remaining

- In game: building and breaking a wall, a piston, `/fill`, TNT, standing still, relogging, and Esc.
- empireu asked next for a fade of the crystals near the eye and overhangs.
- Update the docs for the scan and the shadows, and add a plan file in `plans/`.
- `lightPillarLightPassesBelowTheViewRayBeforeTheLight` no longer guards any code. The permafrost changes rebuild chunk meshes on every client, and `CalculatedSkyClientCommand.ENABLED` goes to `false` before a pull request.
