# Thermal Phase B Page And Face Ownership

- Time: `2026-08-24 15:22:56 +08:00`
- Author: `Codex; primary integration agent`, assisted by `phaseb_face_ownership` (`OpenAI gpt-5.6-sol`, `ultra`)
- Status: `completed`
- Scope: `content.climate.thermal.geometry`, `content.climate.thermal.mesh`, Phase B / PR 3 correctness foundation, living climate documentation, and the active thermal plan

## Completed

- Added section-aligned `ThermalPage` with fixed `int coverageRef[64]`, 4/8/16 coverage widths, 73 primitive geometry summaries, mixed/dirty Brick masks, dense cell/boundary spans, and separate live/published `long` revisions.
- Added O(1) active mutation invalidation and per-tick/per-Brick voxel coalescing into a fixed primitive SPSC `GeometryDeltaRing`.
- Added Page-owned sticky full-geometry resync. Ring overflow cannot lose the recovery requirement, later mutations advance its revision, and stale resnapshot tokens cannot clear newer work.
- Added complete coverage-state validation and blocked publication while coarse support is invalid or any Brick remains dirty.
- Added world-negative-axis `FacePatchIterator` ownership for aligned 4/8/16 cells with canonical signed world keys, exact overlap area, and exact center distance.
- Added `GeometryMigrationLedger` as a runtime wrapper over `ThermalMigrationReference`; extended the reference contract to cover complete air removal and creation without duplicating migration formulas.

## Decisions

- Queue contents are transport state, never geometry authority. `ThermalPage` live revision and sticky resync requirement remain authoritative even when the ring is full.
- A mutation inside coarse coverage invalidates publication immediately; partial Brick acknowledgement cannot make the page publishable. A complete aligned coverage partition must be installed first.
- Cell and boundary activity use dense arena spans and do not impose a fixed 128-cell mask or narrow ID width.
- Phase B remains pure Java and is not wired to Minecraft gameplay. Actual `ThermalCellArena`, solver/runtime coordinator, production hooks, and query composition remain later phases.

## Validation

- `gradlew test runGameTestServer --no-daemon`: build successful on Java 17.
- Thermal JUnit: `112/112`; full repository JUnit: `640/640`; zero failures, errors, or skips.
- Forge GameTest: all `15/15` required tests passed.
- Phase B added 22 focused JUnit cases for summaries, Page/coverage/revision/resync behavior, face ownership, and signed migration; Phase 0 migration added one complete removal/creation edge case.
- `git diff --check` passed after documentation updates.

## Remaining

- Implement Phase C `ThermalCellArena`, actual 4/8/16 `H/C` state, coverage query, and implicit Air-Air adjacency.
- Then implement PR 4 pair/boundary kernels and bounded solve-time semantics before any production Minecraft integration.
- Phase 0b production-like multiplayer and whole-server retained-memory evidence remains partial.
