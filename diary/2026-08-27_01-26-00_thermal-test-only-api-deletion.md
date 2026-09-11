# Thermal test-only API deletion

- Time: `2026-08-27 01:26:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal production API, geometry mutation transport, matching tests, player temperature documentation`

## Completed

- Deleted test-only object wrappers, alternate compiler/adjacency entrances,
  copied snapshots, dead accessors and unused state from the thermal packages.
- Reduced geometry mutation transport to the fields consumed by production.
  `GeometryDeltaCoalescer` still merges by Page-local Brick and
  `GeometryDeltaRing` still transports section identity, lifecycle, revision,
  tick and `baseBrickIndex`; the unused per-voxel mask and its arrays are gone.
- Removed the unimplemented `surfaceFluxW` query field and unavailable flag,
  then corrected `docs/climate/player-temperature.md` to describe the actual
  caller-owned sample.
- Removed test-only signature/material construction helpers and unused public
  constants. Tests now compose the underlying production contracts directly.

## Decisions

- A mutation is rebuilt at exact world-aligned `4x4x4` Brick granularity, so a
  second 64-bit voxel mask cannot affect the compiled topology and is not part
  of the transport contract.
- Keep Forge reflection entrances, third-party resolver registration/context
  boundaries, `ThermalSourceMode.IMPULSE` and the executor dispatch boundary.
- Do not replace deleted APIs with managers, adapters, diagnostics or future
  placeholders.

## Validation

- `./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --rerun-tasks --offline --console=plain` succeeded: `175/175` tests passed across `32` suites with zero failures, errors or skips.
- `./gradlew.bat runGameTestServer --offline --console=plain` succeeded: all `11/11` required Forge GameTests passed.
- Deleted-symbol searches returned no matches; `git diff --check` reported no whitespace errors.

## Remaining

- No further zero-production-use thermal API was proven by this audit. Future
  removal should again require a production-reference and contract check.
