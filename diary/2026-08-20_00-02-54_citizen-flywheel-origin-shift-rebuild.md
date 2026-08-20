# Citizen Flywheel origin-shift rebuild

- Time: `2026-08-20 00:02:54 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `FlywheelCitizenBackend, FlywheelCitizenBackendTest, and town citizen rendering documentation`

## Completed

- Traced the persistent Body disappearance to Flywheel 0.6.11 origin shifting: `InstancingEngine.beginFrame` clears every material's `GPUInstancer.data` before notifying origin listeners, while the backend retained its logical `entries` and only marked the detached `CitizenInstanceData` handles dirty.
- Made `FlywheelCitizenBackend` an `InstancingEngine.OriginShiftListener` and registered it with the active instancing engine.
- Replaced coordinate-only origin rewriting with immediate slot recreation from `ClientCitizenCache`; the rebuild preserves cached packed light and accumulated walk phase.
- Kept the tick-time origin comparison as a fallback if an origin callback is missed.
- Added a regression test requiring the custom backend to implement Flywheel's origin invalidation listener contract.

## Decisions

- Treat any Flywheel origin change as complete instance-slot invalidation. `InstanceData.markDirty()` updates existing slots but cannot reinsert a handle removed by `GPUInstancer.clear()`.
- Rebuild inside the origin callback so the newly created instances are available to the same render frame, rather than waiting for the next client tick.
- Reuse cached light during this rare rebuild to avoid up to 1024 synchronous world light queries in the frame that shifts origin.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.client.FlywheelCitizenBackendTest`: `BUILD SUCCESSFUL`.
- `./gradlew test`: `BUILD SUCCESSFUL`.
- `git diff --check`: no whitespace errors; only pre-existing LF-to-CRLF working-copy warnings.

## Remaining

- In the client, enable `flywheel_m3`, load 1024 moving residents, cross a Flywheel origin boundary by moving more than 100 blocks along one axis, and verify Body/Billboard instances remain visible with stable ownership counts.
- Expected origin-rebuild dirty peak: `111,360 B` for 960 batch instances with 64 detailed entities, or `118,784 B` for 1024 sleeping batch instances; steady frames must return to `0 B`.
