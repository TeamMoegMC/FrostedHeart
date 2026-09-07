# Thermal Phase J radiation

- Time: `2026-08-25 03:38:00 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `content.climate.thermal.radiation, Minecraft thermal input/source integration, climate documentation and thermal implementation plan`

## Completed

- Added the dormant main-thread `RadiationService` with a source-origin section index, range/flux filtering, deterministic top-K selection, three-point player sampling, hard work caps, and a bounded revision witness cache.
- Added loaded-only quarter-block Minecraft DDA using the independent `radiationOcclusionPatternId` channel and independent loaded-section revisions.
- Connected Campfire and Generator radiation origins, power shares, enable/disable, replacement, and unload lifecycle to their existing physical source ownership without introducing a second source ledger.
- Added focused JUnit coverage and a Forge GameTest for live-source replay, real stone occlusion, restored visibility after wall removal, witness cache hits, and read-only source accounting.
- Corrected source and section revision allocation to advance the monotonic counters; the real wall GameTest now proves mutation invalidation with the same receiver generation instead of forcing cache replacement.
- Made source-cap admission refusal produce `RADIATION_BUDGET_LIMITED` and reduced confidence, so a bounded partial source index cannot report a complete result.
- Derived source-bucket bounds from the full feet-to-head sampling extent, preventing section-edge receivers from missing a source that is still inside one ray's configured range.
- Updated the living climate documentation and implementation plan to describe the dormant Phase J behavior and Phase K boundary.

## Decisions

- Receiver sampling is observational: it never mutates the source timeline, deducts emitted energy, or injects radiation into the air mesh.
- Radiation occlusion remains independent from collision, airflow aperture, and material contact geometry.
- Unloaded or unresolved space blocks conservatively, while source discovery, candidates, rays, DDA steps, witnesses, and optional memory all have explicit bounds.
- Material radiation and `PlayerTemperatureData` authority remain deferred to later work; Phase J stays dormant until consumer migration gates pass.

## Validation

- `.\gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` passed.
- Repository JUnit: `808/808`; thermal JUnit: `235/235`; `RadiationServiceTest`: `5/5`; Forge GameTest: `19/19` required.
- `git diff --check` passed.

## Remaining

- Implement Phase K / PR13 consumer migration while keeping the legacy gameplay path authoritative until Phase 0b production-like evidence, FarField approval, and gameplay/reference calibration gates pass.
- Material radiation remains a separate deferred feature.
