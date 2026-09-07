# Thermal deletion-first convergence

- Time: `2026-08-26 20:47:13 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal solver representation, topology dirty bookkeeping, material ordering, physical source rebinding, deferred mutation drain, tests, and documentation`

## Completed

- Deleted the flat `ThermalSweep` builder, operation columns, validation branch, and execution loops; production and tests now use `ThermalSweepFragments` as the sole solver layout.
- Deleted Page-local material order, fragment traversal/rank mirrors, mutation-history reordering, and full-domain rank rebuilds. Material contribution order is fixed by fragment and operation index.
- Replaced three topology dirty Page maps plus `topologyDirty` with one identity Page set while keeping the existing work-specific Brick masks.
- Deleted the physical source target-section reverse index and reused `installedActiveBySection` for direct Page lookup. Kept topology-generation fallback for first cold Page admission.
- Added an existing-revision fast return to deferred mutation draining without adding another owner collection.
- Reduced the five core production files from `10,648` to `9,674` lines, a net deletion of `974` lines; production class count did not increase.
- Updated the climate lifecycle documentation and active architecture plan to remove the obsolete material traversal description.

## Decisions

- Preserve `ThermalCellArena` as the only `H/C` authority and retain physical formulas, source `integral(P dt)`, FarField, radiation, lifecycle generations, cadence, and gameplay values unchanged.
- Use deterministic spatial material order because final topology, not mutation history, should determine floating-point aggregation order.
- Prefer bounded full-source invalidation over a second reverse index; the existing source cap bounds work and direct installed-section lookup avoids Page scans.
- Leave `ThermalCellArena.findFreeSpan` unchanged until a same-workload real-save JFR identifies it as a meaningful hotspot.

## Validation

- `./gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `212/212` passed with zero failures, errors, or skips across `35` test classes.
- Forge GameTest: `14/14` required tests passed.
- Deleted-name search returned no callers outside the thin `ThermalSweep` factory itself; `git diff --check` reported no whitespace errors.

## Remaining

- Capture the same repeated door/mining workload in a real save with JFR before considering an arena free-span allocator or any further performance data structure.
