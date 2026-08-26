# Thermal Test-Only API Deletion

- Time: `2026-08-27 00:44:10 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/climate/thermal` and matching tests
- Related: [`2026-08-26_22-17-53_thermal-production-caller-convergence.md`](2026-08-26_22-17-53_thermal-production-caller-convergence.md)

## Goal

Continue deletion-first convergence after the production lifecycle tests landed. Remove production objects and public methods whose only purpose is to support a test-specific path, while preserving the primitive runtime data flow and current gameplay semantics.

## Rules

1. A test caller does not justify a production object wrapper or alternate compiler entrance.
2. Migrate tests to the same primitive ring, Brick compiler, source producer, receiver query, and lifecycle entrances used by gameplay.
3. Do not add replacement managers, adapters, caches, diagnostics, or future-only abstractions.
4. Keep `ThermalSourceMode.IMPULSE`, the third-party resolver extension interfaces, and the single `enableDispatch(Executor)` asynchronous boundary.
5. Run the full thermal JUnit and Forge GameTest suites once after the complete deletion batch.

## Work

- Delete `GeometryDelta` and the object-returning/object-accepting conveniences around `GeometryDeltaRing`.
- Delete the whole-Page `ImplicitAirAdjacency.compileOwnedPairs` entrance and the production `PositiveNeighbors.none()` test convenience. Make tests aggregate the real `compileOwnedBrickPairs` production call.
- Audit remaining source/radiation and arena APIs. Delete only surfaces with proven production-zero callers when the test can use the real gameplay entrance without introducing another layer.
- Search deleted symbols, measure the production diff, run full validation, and record the outcome.

## Outcome

Completed on `2026-08-27`.

- Deleted the object `GeometryDelta` path, whole-Page adjacency/test helpers,
  copied resolver snapshot and other production APIs whose only callers were
  tests. Tests now consume the same primitive rings, Brick compiler and
  lifecycle entrances as gameplay.
- Removed unused state and accessors from the arena, Page, source, publication,
  geometry and runtime surfaces. Kept `ThermalSourceMode.IMPULSE`, resolver
  registration/context contracts and the single executor dispatch boundary.
- Removed `changedVoxelMask` from the Page coalescer and `GeometryDeltaRing`.
  Production only consumes `baseBrickIndex` and recompiles that exact `4x4x4`
  Brick, so the per-voxel mask duplicated memory without affecting topology.
- Removed the always-zero `MutableEnvironmentSample.surfaceFluxW` placeholder
  and its always-set, unread unavailable flag. Updated the living player
  temperature document to list only implemented query outputs.
- Removed remaining test-only construction conveniences and unused constants,
  including `ThermalSignatureRegistry.Builder.internResolution`,
  `MaterialBoundaryRegistry.Profile.stateless`, `OFFSETS_PER_AXIS` and
  `INITIAL_UNIFORM_STEP_TICKS`.

Validation:

- `compileJava`, `compileGameTestJava` and all `175/175` thermal JUnit tests
  passed with zero failures, errors or skips across `32` suites.
- `runGameTestServer` passed all `11/11` required Forge GameTests.
- Deleted-symbol searches returned no matches and `git diff --check` passed.
