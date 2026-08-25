# Thermal Phase K crop shadow query

- Time: `2026-08-25 04:30:19 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `passive crop query, real PlantStatus observation, retained-state invariant, climate documentation and thermal implementation plan`

## Completed

- Added primitive-coordinate `sampleCropEnvironment`, real legacy/new observation through `observeCropEnvironment`, bounded crop hit/miss/comparison counters, and `CropShadowSnapshot` in the existing Minecraft input owner.
- Connected all three temperature-producing `WorldTemperature.checkPlantStatus` paths: ordinary growth/sapling calls, precomputed random-tick survival/death calls, and temperature-preserved calls. Weather shortcuts that never calculate block temperature do not create observations.
- Kept legacy `WorldTemperature.block` results authoritative. The shadow comparison intentionally records legacy block temperature against published Air Mesh temperature without claiming that the missing surface channel is already modeled.
- Extended the existing Minecraft input GameTest with an admitted crop read, one real `PlantStatus` comparison, and 10,000 passive misses that preserve Page count, arena high-water, live-cell count, and total enthalpy.
- Updated living climate documentation and the Phase K implementation snapshot.

## Decisions

- Crop queries remain `PASSIVE_BLOCK_TICK`: they cannot create Page, Brick, Cell, Interest, radiation work, or retained per-crop state.
- No `SharedQueryFrame` object was added. Different crop positions do not share publication values; a revision-safe frame cache remains conditional on Phase 0b workload evidence showing meaningful same-position repetition.
- Crop gameplay cannot switch to the new sample until the surface compositor and gameplay/reference calibration define how Air Mesh temperature replaces or combines with legacy block temperature.

## Validation

- `.\gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` passed.
- Repository JUnit: `809/809`; thermal JUnit: `236/236`; Forge GameTest: `19/19` required.
- `git diff --check` passed.

## Remaining

- Continue Phase K in the frozen order: town, then HUD consumers.
- Keep legacy player, crop, and machine gameplay authoritative until production-like Phase 0b, FarField approval, and gameplay/reference calibration gates pass.
