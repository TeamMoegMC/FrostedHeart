# Thermal cross-Page staged-cut crash

- Time: `2026-08-26 03:30:36 +08:00`
- Author: `Codex; primary implementation and verification agent`
- Status: `completed`
- Scope: `MinecraftThermalTopologyApplier` material-boundary compilation and staged signature lifetime

## Completed

- Fixed the production crash where material-boundary compilation read a neighboring Page's released `desiredSignatureIds` after that Page had already committed earlier in the same rebuild batch.
- Added a two-Page material-boundary regression test covering adjacent dirty Pages and verifying staged signature arrays are released after commit.
- Audited every `desiredSignatureIds` read and every `PageState.dirty` transition; no other cross-Page transient-array read remains.

## Decisions

- A desired neighbor read falls back to `appliedSignatureIds` only after the desired array has been committed and released. This preserves the batch cut without restoring a permanent second `int[4096]` per Page.
- Living climate documentation was not changed because the fix restores the documented behavior and does not alter formulas, lifecycle contracts, configuration, or player-facing semantics.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --console=plain`: `203` tests, `0` failures, `0` errors, `0` skipped.
- `./gradlew runGameTestServer --console=plain`: all `12` required GameTests passed.
- `git diff --check`: no whitespace errors in the fix.

## Remaining

- Repeat the real-save JFR workload now that world admission no longer crashes.
