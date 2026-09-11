# Phase H material boundaries

- Time: `2026-08-25 01:44:50 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `dormant stateless, capacitive, and natural-rock material boundaries in the sparse thermal runtime`

## Completed

- Added an immutable worker-safe `MaterialBoundaryRegistry` with explicit material profiles and `4x4x4` contact masks; profile/contact ID zero remains no boundary.
- Extended `ThermalCellArena` so sparse material surface/deep poles share the Page-owned authoritative `H/C` span with air cells and retire with that span.
- Compiled exact one-block stateless wall bridges, air-to-capacitive-surface pairs, surface-to-deep pairs, and exposed natural-rock boundaries without collapsing multi-block walls into one conductance.
- Preserved material enthalpy across topology rebuilds using stable block/face/interface-plane/depth keys, dirtied only immediately adjacent sections, and avoided stable-tick material rescans.
- Added integration coverage for stateless wall thickness, capacitive storage/release and migration, Page retirement, and exposed-only natural rock.

## Decisions

- Phase H remains dormant shadow code. The default topology overload uses an empty material registry, so legacy gameplay temperature remains authoritative.
- Geothermal input enters only through an exposed natural-rock material surface; buried rock creates neither material poles nor a direct air boundary.
- `PHASE_RESERVOIR` is deferred to Phase I / PR11 rather than being folded into material boundary compilation.
- Moving Create structures remain air, static `hasDynamicShape=false` states remain trusted, and unsupported dynamic geometry remains unresolved.

## Validation

- `gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` with Java 17: passed in `2m 3s`.
- JUnit XML: thermal `223/223`, repository `796/796`, with zero failures, errors, or skips.
- Forge GameTest: `18/18` required tests passed.
- `git diff --check`: passed; only existing LF-to-CRLF working-copy notices were reported.

## Documentation impact

- Updated `docs/climate/data-lifecycle-and-integration.md` with ownership, lifecycle, migration, invalidation, and validation behavior.
- Updated `docs/climate/heat-production-and-network.md` with the three dormant material boundary models and SI parameter meanings.
- Updated the thermal implementation plan to mark Phase H / PR10 complete and Phase I / PR11 as next.

## Remaining

- Implement Phase I / PR11 Brick-local phase reservoirs.
- Production-like Phase 0b workload evidence and approved FarField profiles remain activation gates; gameplay query authority must not switch before those gates and later shadow workloads pass.
