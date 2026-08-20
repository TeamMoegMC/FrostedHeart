# Citizen Flywheel M3 dynamic instances

- Time: `2026-08-19 23:20:17 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `CitizenInstanceData, FlywheelCitizenBackend, dynamic Flywheel shader, Body/Billboard LOD, tests, and town living documentation`

## Completed

- Replaced the frozen 46-byte `OrientedData` PoC with one 58-byte `CitizenInstanceData` per batch-owned citizen. Snapshot and light changes dirty only the affected instance; render frames no longer rebuild citizen body vertices.
- Added `citizen.vert` GPU interpolation, 30-tick bounded extrapolation, short-path two-rate yaw convergence, opposite arm/leg swing, standing bob, sleeping transforms, and standing/sleeping Billboard transforms.
- Added shared 144-vertex Body and four-vertex Billboard models for each of seven vanilla wide-skin materials. Rigid `partId` is encoded in the otherwise unused static vertex color R channel because Flywheel 0.6.11 converts instanced models to fixed `Formats.BLOCK` vertices.
- Added stateful Body/Billboard ownership: Body enters at 68 blocks and remains through 72; Billboard remains outside 68 and is removed beyond the 96-block AOI. Detailed entities remain mutually exclusive.
- Added tick-only, staggered light sampling. An unchanged light result does not dirty the instance.
- Kept `/citizen_debug backend flywheel_poc` as a compatibility alias and added `/citizen_debug backend flywheel_m3`; the backend now requires Flywheel `INSTANCING` and falls back to `cpu_batch` for BATCHING/OFF or runtime failure.
- Updated `citizen-rendering-at-scale.md`, `hybrid-simulation-architecture.md`, and the town documentation index.

## Decisions

- Use one compact instance and one shader branch per citizen. Six per-part instancers would multiply resident instance memory and snapshot upload traffic by roughly six.
- Do not add a custom vertex format: Flywheel's instancing model pool converts every model to `Formats.BLOCK`, so a custom attribute would be discarded. Encoding `partId` in static vertex color survives that conversion without a Flywheel patch or direct OpenGL path.
- Reject Flywheel BATCHING for M3. It CPU-bakes instance transforms and cannot provide the `uTime`-driven animation and snapshot-only dirty contract.
- Keep `CpuBatchCitizenBackend` as the default until M3 graphics, GPU budget, resource reload, and shader-mod compatibility are validated in the client.

## Validation

- `./gradlew test`: 71 suites, 233 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL`.
- Focused tests cover the 58-byte stride and exact writer offsets, six Body part tags, Billboard tag, animation-clock wrapping, shortest yaw deltas, 68/72 LOD hysteresis, and packaged Flywheel program/shader resources.
- `git diff --check`: no whitespace errors; only existing LF-to-CRLF working-copy warnings.

## Remaining

- Run the 1024 moving and sleeping client benchmarks with `flywheel_m3`; confirm shader compilation, movement, halt, turning, limb animation, sleep orientation, UVs, lighting, and expected 58-byte dirty peaks.
- Validate Body/Billboard transitions by crossing 68/72 blocks and confirm no ghost or empty frame.
- Repeat F3+T, dimension changes, Embeddium, and Oculus without/with a shader pack; capture JFR/RenderDoc data before considering M3 for the default backend.
