# Thermal phase profile accessor crash

- Time: `2026-08-26 03:43:53 +08:00`
- Author: `Codex; primary implementation and verification agent`
- Status: `completed`
- Scope: `MinecraftThermalTopologyApplier` gameplay phase-candidate ownership query

## Completed

- Fixed the real-save crash where `hasAppliedPhaseCandidate` validated a live `PHASE_RESERVOIR` and then read it through the material-pole-only `materialProfileId` accessor.
- Added regression assertions for two installed candidates and one absent candidate in a shared Brick-local phase reservoir.
- Audited all production `materialProfileId` and `phaseProfileId` call sites; no second phase-reservoir/material-pole accessor mismatch remains.

## Decisions

- Use the existing typed `phaseProfileId` accessor. Arena layout, reservoir lifetime, topology compilation, and transition behavior are unchanged.
- Living climate documentation was not changed because this restores the documented ownership path without changing lifecycle, formulas, configuration, or player-facing semantics.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --console=plain`: `203` tests, `0` failures, `0` errors, `0` skipped.
- `./gradlew runGameTestServer --console=plain`: all `12` required GameTests passed.
- `git diff --check`: no whitespace errors.

## Remaining

- Re-enter the same real save and repeat the JFR workload to verify the production `ServerLevel.tickChunk` path.
