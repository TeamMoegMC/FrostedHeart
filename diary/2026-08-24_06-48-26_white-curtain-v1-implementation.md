# White curtain V1 spatial weather implementation

- Time: `2026-08-24 06:48:26 +08:00`
- Author: `Codex; primary implementation agent, with read-only gpt-5.6-sol ultra performance review`
- Status: `partial`
- Scope: `content.climate.gamedata.climate`, climate packets/events, client weather state/render/fog/sound, weather Mixins/config, climate living docs and V1 plan

## Completed

- Extracted the persisted `WhiteCurtainDescriptor` and shared `WhiteCurtainFieldModel` while preserving `area`, `move`, `climate` and `whiteCurtainInfos` save keys.
- Added generation-aware exact-phase server cache expiry and immediate invalidation for add, clear, load and natural prune; inclusive corridor endpoints now agree across intersection, forecast and gameplay queries.
- Added sparse dimension snapshots and client reconstruction with fixed Fast/Fancy grids, active-front candidate sorting, fair global wall-slice allocation, bounded snow/terrain work, shared fog, one wind loop and explicit Compatibility/Fast/Fancy player choices.
- Changed clock reconstruction to the authoritative `dayTime` source. Snapshot and climate packets carry `sec + clockDayTime`; frozen daylight stays frozen, small corrections replace outstanding error, and sleep/command discontinuities re-anchor through the existing climate packet.
- Split render-frame ownership from tick ownership. The frame now freezes the current `Camera.setup` result at `LevelRenderer.renderLevel`; sound, `tickRain` and ground effects consume the tick camera sample instead of a stale render frame.
- Moved Vanilla weather compatibility scheduling and capability lookup from every server tick to the existing one-second climate branch. Stable state sends no weather packet.
- Added `5 logical seconds` snow/whiteout phase smoothing and one-point smoothed indoor exposure. Fixed zero-volume wind-loop startup and End-conquered respawn synchronization.
- Decoupled precipitation ownership from the exposure-scaled camera point. The unattenuated previous/current grid footprint now keeps V1 ownership indoors and renders a nearby snow band before the camera crosses its front, while exposure still suppresses local snow, fog, wind, sound and ground effects.
- Split the constant-cost dayTime advance from spatial field evaluation. Compatibility mode keeps the analytic white-curtain clock current without rebuilding candidates or grids, invalidates its old spatial grid, and reconstructs the current front on the first V1 tick after re-enabling the backend.
- Closed the Java-side render-state contract: the weather pass now restores the entering shader, texture unit 0 and shader color without a per-frame capturing supplier, and restores the `AFTER_WEATHER` canonical depth-write/depth-test/cull/blend state in every `finally` path. Runtime GL capture with and without Oculus remains required.
- Removed empty-batch wrapper creation with `endOrDiscardIfEmpty`, changed wall vertical coordinates to the level build range relative to the camera, and moved radius rejection from the full corridor center to each fixed segment so a nearby end of a very wide curtain is not incorrectly culled. Non-empty `Tesselator.end`, true frustum/screen clipping and persistent VBO work remain open measurement gates.
- Removed the `ChunkPos` allocation from `WorldClimate.getTemp(BlockPos)` white-curtain cache hits and added a precipitation-only grid sampler that skips visibility interpolation.
- Updated `docs/climate/weather-rendering.md`, `data-lifecycle-and-integration.md`, `world-climate-and-temperature.md`, and the V1 plan to distinguish implemented behavior from remaining runtime gates.

## Decisions

- Keep all server authority sparse and analytic; no render-distance interest map, snowflake sync, moving weather entity or server visual grid.
- Do not use FPS-driven V1 fallback. Fast, Fancy and Compatibility remain explicit player choices; only a renderer functional fault quarantines custom rendering for the level session.
- Treat CPU, GPU and allocation as separate release gates. Fixed work counters are evidence of bounded scaling, not proof that millisecond or `0 B/frame` targets are met.
- Keep the low-cost indoor model to one reused eye-position `canSeeSky` query per client tick. Terrain/depth-aware shelter belongs to later measured work.

## Validation

- Java 17 `compileJava compileTestJava`: passed.
- V1 targeted JUnit suite: `34 tests, 0 failures, 0 errors`, including codec/model/cache/clock/snapshot/state/frame/compatibility coverage.
- Java 17 full suite: `151 suites, 581 tests, 0 failures, 0 errors`.
- Earlier client smoke reached the main menu with Embeddium/Oculus, Mixins and resources loaded without a V1-related exception; the client was then stopped intentionally.

## Remaining

- Enter a world and run the manual outside/front/core/retreat/indoors matrix, mode switching, login/dimension/End-return lifecycle, fault injection and two-client opposite-side validation.
- Capture server tick/capability/packet counters plus client tick, render-thread, GPU P50/P95/P99, JFR allocation/retained memory and `30/60/144+ FPS` evidence on low-end and mainstream hardware.
- The non-empty `Tesselator.end()` wall/snow path still creates batch wrapper objects. If JFR fails the allocation gate, replace it with persistent VBOs and a reusable staging buffer.
- Add measured frustum/screen-segment culling, terrain-conforming wall geometry and the formal V2 backend lifecycle before claiming the V1 handoff gate complete.
