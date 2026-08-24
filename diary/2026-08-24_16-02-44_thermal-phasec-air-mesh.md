# Thermal Phase C Regular Air Mesh

- Time: `2026-08-24 16:02:44 +08:00`
- Author: `Codex; primary engineering agent`
- Status: `completed`
- Scope: `content.climate.thermal.mesh`, Phase C regular AirCell state, coverage lookup, implicit adjacency, living climate documentation, and the active thermal plan

## Completed

- Added `ThermalCellArena`, a growable primitive SoA for regular 4/8/16 AirCells with enthalpy `H`, capacity `C`, Page ownership, world support, level, medium, flags, and derived `T = Tref + H/C`.
- Added dense Page-layout LOD replacement. Split and merge allocate a complete replacement span, reuse the validated reference formulas, preserve signed/negative enthalpy, and keep old state live until the Page installs complete replacement coverage.
- Added commit/rollback ownership checks. Commit rejects old coverage refs, mismatched widths, incomplete new-cell ownership, and a Page that has not installed the replacement span.
- Added allocation-free `ThermalPage.tryQueryPublishedCoverage` with caller-owned `MutableCoverageQuery`; stale live/published identities clear the result and require Page-wide fallback.
- Added `ImplicitAirAdjacency`. It enumerates only +X/+Y/+Z from the negative-axis owner, derives positive partitions from published coverage, delegates canonical overlap geometry to `FacePatchIterator`, and keeps no persistent generic edge graph.
- Left non-regular and mixed supports behind `CoverageCellResolver`; they are not silently opened and remain a later mixed face-port integration task.

## Decisions

- Phase C regular support refs equal wide `int` arena slots. This is a correctness layout, not a frozen packed production representation.
- A Page owns one dense cell span. Local split/merge therefore replaces the complete Page span instead of leaving fragmented per-cell allocations.
- Old/new span double ownership is intentional until coverage handoff commits; later memory admission must include this publication/rebuild peak.
- Phase C does not implement pair/boundary exchange, `SolveEpoch`, mixed Brick ports, Minecraft runtime wiring, or gameplay authority.

## Validation

- `gradlew test runGameTestServer --no-daemon`: build successful on Java 17.
- Thermal JUnit: `124/124`; full repository JUnit: `652/652`; zero failures or errors.
- Forge GameTest: all `15/15` required tests passed.
- Phase C added 12 focused JUnit cases covering primitive H/C state, negative enthalpy, 16-to-4 split, 4-to-16 merge, rollback and coverage handoff, O(1) publication query invalidation, internal fine-grid faces, 8-to-4 and cross-Page 16-to-4 ownership, stale frontier behavior, and the non-regular support adapter boundary.

## Remaining

- Implement Phase D / PR 4 pair and boundary kernels, buoyancy conductance, sealed `SolveEpoch`, and bounded no-backlog time semantics.
- Replace correctness traversal scratch only if PR 4 measurements show it on the steady solver hot path; no Phase A or legacy benchmark rerun is needed now.
- Mixed Brick face ports remain explicit later integration work. Minecraft gameplay still uses the legacy temperature authority.
