# 居民 Flywheel 静态实例 PoC

- Time: `2026-08-19 22:33:20 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `M2 citizen Flywheel backend, shared body mesh, debug switching, metrics, tests, and living documentation`

## Completed

- Added `FlywheelCitizenBackend`, an explicit non-default M2 backend that obtains the current Flywheel world `MaterialManager` without adding a carrier entity or per-citizen `EntityInstance`.
- Added one shared six-box, 144-vertex Body model per built-in wide-skin material. Each crowd resident uses Flywheel `OrientedData` with a measured 46-byte stride; detailed fake entities remain exclusively owned and are not duplicated.
- Added candidate-cache prewarming, static instance creation/removal, 96-block ownership filtering, creation-time light sampling, renderer-manager replacement detection, render-origin rewrites, world cleanup, health checks, and coordinator CPU fallback.
- Added `/citizen_debug backend status|flywheel_poc|cpu_batch`. The PoC freezes creation-time position and yaw by design; dynamic snapshots, animation, sleeping transforms, and billboard LOD remain M3 work.
- Updated citizen metrics wording and both living rendering documents. Metrics retain `peakInstanceDirtyBytes` until reset so the one-frame initial upload remains observable; the backend hook timer is documented as excluding Flywheel engine render-layer CPU and GPU time.

## Decisions

- Use Flywheel 0.6.11's public `InstanceManager.materialManager` directly, with `FlywheelCitizenBackend` as the single crowd owner. A dummy carrier entity adds lifecycle and registration complexity without providing ownership that the backend does not already have.
- Keep `cpu_batch` as the default and require explicit debug activation until UV, depth, light, resource reload, Embeddium, and Oculus tests pass in a real client.
- Use built-in `OrientedData` for M2 because its 46-byte layout fits the 64-byte instance budget and proves persistent upload behavior. M3 still needs custom data and shader logic for interpolation and animation.

## Validation

- `gradlew compileJava`: passed.
- Focused `FlywheelCitizenBackendTest`: 3 tests passed. It verifies the 46-byte instance budget, 144-vertex body, and all 256 quaternion orientations against `CitizenBatchRenderLayout`.
- Full Gradle suite: 229 tests across 71 suites, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESSFUL`).
- `git diff --check` passed with only the repository's existing LF-to-CRLF working-tree warnings; the changed M2 source, tests, diary, and living documents contain no trailing whitespace.

## Remaining

- Run the documented 1024-moving static scene in the development client and verify rendering with Flywheel instancing and batching, Embeddium, F3+T, and Oculus with and without a shader pack.
- Capture JFR/RenderDoc data. `CitizenRenderMetrics` cannot measure Flywheel's own render-layer CPU or GPU pass.
- After the M2 decision gate passes, implement M3 dynamic snapshot interpolation, animation, sleeping transforms, billboard LOD, and dirty light updates.
