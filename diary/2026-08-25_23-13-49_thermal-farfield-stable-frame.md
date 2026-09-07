# Thermal FarField closure and stable-frame reuse

- Time: `2026-08-25 23:13:49 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `Minecraft thermal Page capture, FarField compilation, stable frame dispatch, tests, climate documentation, and active thermal plan`

## Completed

- Added a fixed `16x16 byte` per-Page sky-exposure cut captured from the already loaded chunk heightmap. A single open frontier can now use the existing FarField when its air component has direct outdoor proof.
- Reused the calibrated open-air impedance for each dimension's own `WorldTemperature.naturalAir` background and applied the existing global `0..100` wind as a continuous `1.0..1.8` conductance multiplier.
- Added an unchanged-topology path: frames with no geometry, lifecycle, material, natural-temperature, sky, or wind change acknowledge non-source watermarks while retaining the installed `ThermalSweep`.
- Updated living climate documentation and the active implementation plan with the new behavior and remaining underground-continuation boundary.

## Decisions

- Do not infer that every missing neighboring Page is outdoors. A non-sky-exposed, single-direction frontier remains `OPEN_CONTINUATION` because it may lead to an unadmitted room or tunnel.
- Do not add a topology-by-wind profile matrix. Opening area, natural temperature, and one bounded wind scalar cover the current gameplay boundary without new runtime abstractions.
- A `null` topology replacement in `DimensionThermalRuntime.finishTopologyUpdate` explicitly means retaining the current arena-bound sweep; it is used only while the logical writer is held.

## Validation

- `./gradlew.bat test --tests com.teammoeg.frostedheart.content.climate.thermal.mesh.TopologyGuardTest --tests com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.MinecraftThermalTopologyApplierTest --no-daemon --console=plain` passed.
- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --no-daemon --console=plain` passed `239/239` tests.
- `./gradlew.bat runGameTestServer --no-daemon --console=plain` passed all `19/19` required Forge GameTests.
- `git diff --check` reported no whitespace errors; existing line-ending conversion warnings remain.

## Remaining

- Add bounded mesh continuation for underground single-direction frontiers; do not replace it with ambient loss.
- Measure the stable-frame fast path in the complete production-like pipeline before publishing CPU or allocation claims.
- Non-phase material calibration, cold-side phase authority, surface composition, old `ChunkHeatData` migration, and real asynchronous scheduling remain separate work.
