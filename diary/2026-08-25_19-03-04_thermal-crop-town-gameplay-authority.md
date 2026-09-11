# Crop and town thermal gameplay authority

- Time: `2026-08-25 19:03:04 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `crop PlantStatus temperature, house/hunting building temperature, Minecraft thermal query boundary, living climate/town documentation, and thermal implementation plan`

## Completed

- Changed the existing crop query boundary to return published Air Mesh temperature on a valid hit and the already-computed legacy block temperature on every miss.
- Routed all three `WorldTemperature.checkPlantStatus` temperature paths through that result while preserving weather-first early returns.
- Changed the existing town projection boundary to return the weighted published-air average only when every group is covered; partial or missing regions use the complete legacy voxel average.
- Made `HouseBlockScanner` and `HuntingBaseBlockScanner` store that selected temperature, so housing scores, hunting conditions, and downstream daily settlement now consume it.
- Updated the living climate/town documentation and Phase K implementation status.

## Decisions

- Crop and town queries remain passive: they do not start a runtime, admit Pages, load chunks, retain per-consumer state, execute radiation, or mutate thermal energy.
- Keep legacy fallback as a resolved edge case for absent, stale, unresolved, or partial publications; never combine a partial town result with legacy voxels.
- Do not fabricate a machine consumer. The repository has no ordinary machine that currently reads environment temperature, so the registered-machine API remains observation-only.
- Do not enable Phase H/I material or snow/ice mutation profiles because production material parameters are still absent and the gameplay registry remains empty.

## Validation

- Targeted Java 17 JUnit passed `239/239` tests with zero failures, errors, or skips.
- Forge GameTest passed all `19/19` required tests. The production scenario proved a crop uses published `0°C` instead of legacy `5°C`, and a complete town projection returns published `0°C`.
- Existing bulk-miss assertions still proved that 10,000 crop misses and 4,096 town group misses do not grow Page count, arena high-water mark, live cells, or total enthalpy.
- `git diff --check` reported no whitespace errors; existing LF-to-CRLF notices remain.

## Remaining

- Verify crop growth/survival and house/hunting temperatures around a real heated base.
- Define production material profiles before enabling Phase H/I material and snow/ice gameplay authority.
- Connect the registered-machine query only when an actual environment-sensitive machine owns a receiver point and cadence.
