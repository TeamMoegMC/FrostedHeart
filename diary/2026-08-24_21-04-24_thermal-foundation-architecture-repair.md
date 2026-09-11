# Thermal foundation architecture repair

- Time: `2026-08-24 21:04:24 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `content.climate.thermal.mesh`, `solver`, `source`, Phase 0 scaffolding, tests, plans, and climate lifecycle documentation

## Completed

- Unified regular/mixed component enthalpy and capacity in `ThermalCellArena`; source and transport now mutate the same arena.
- Connected `ComponentBrickCompiler.CompiledBrick` volumes, centroids, and face ports to arena allocation and concrete regular/mixed pair compilation.
- Bound frozen sweeps to one arena with endpoint lifecycle generations; stale reused slots are rejected before mutation.
- Added batch source preflight so stale or numerically invalid targets leave pending energy available for retry.
- Removed obsolete `ThermalPage` runtime placeholders, moved test-only Phase 0 helpers out of production sources, commented the dynamic exclusion GameTest, and reversed migration formula ownership into `GeometryMigrationLedger`.

## Decisions

- PR7 will schedule and publish the existing arena-bound sweep; it must not introduce a second `H/C` store or a mixed resolver callback.
- Moving structures remain air while moving. Unsupported or stale geometry remains a Page-wide rebuild/fallback condition until runtime integration exists.
- `PhaseZeroThermalRouting` and `ThermalBenchmarkEvidence` are test fixtures, not runtime or measured-evidence authorities.

## Validation

- `gradlew test runGameTestServer --no-daemon --console=plain`: passed.
- Thermal JUnit: `177/177`; repository JUnit: `705/705`; required Forge GameTest: `14/14`.
- Directed coverage includes regular/mixed fractional aperture, mixed/mixed intersection and closed aperture, foreign Page coverage rejection, source/sweep arena identity, stale source retention, and stale sweep slot reuse.

## Remaining

- PR6 FarField holdout gate.
- PR7 dimension runtime: frame cuts, mailbox/publication, scheduling, sleep/wake, and memory admission.
- Minecraft production hooks and gameplay query migration remain gated; this repair does not change player-visible temperature behavior.
