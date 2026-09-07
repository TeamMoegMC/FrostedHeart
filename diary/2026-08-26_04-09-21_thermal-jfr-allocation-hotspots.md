# Thermal JFR allocation hotspot optimization

- Time: `2026-08-26 04:09:21 +08:00`
- Author: `Codex; primary implementation and verification agent`
- Status: `completed`
- Scope: `conservative air lookup, face traversal, and Page geometry migration`

## Completed

- Analyzed the `224 s` stable gameplay window in `thermal-production-accessor-fixed-2026-08-26_03-44-46.jfr`; thermal stacks represented about `26.7%` of sampled server CPU and `44.1 MiB/s` of sampled allocation.
- Replaced `Resolution.componentAt` enhanced iteration with indexed immutable-list access, removing the per-call `ImmutableCollections$ListItr` allocation identified by JFR.
- Replaced production `ConservativeAirGeometry.Face.values()` traversal and ordinal decoding with allocation-free bounded ordinal iteration.
- Replaced the three maximum-size microcell overlap tuple arrays in `MinecraftThermalTopologyApplier.calculateMigration` with old/new cell aggregate arrays.
- Made migration use regular coverage directly and calculate each old cell temperature once, avoiding redundant component resolution and division for every represented microcell.
- Added `GeometryMigrationLedger.calculateAggregatedGeometryMigration` as the shared conservation and signed ingress/egress finalization path; sparse migration now reuses the same finalization instead of duplicating it.

## Decisions

- Preserve Page rebuild, arena layout, cell authority, topology compilation, and migration formulas. This change only removes allocation and repeated bookkeeping from measured hot paths.
- Do not add JMH, retained profiling state, or Page-owned scratch tables. A real-save JFR rerun remains the performance authority.
- Living climate documentation was not changed because physical behavior, lifecycle, configuration, and cross-system contracts are unchanged.

## Validation

- `./gradlew test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --console=plain`: thermal JUnit `204/204` and Forge GameTest `12/12` required passed.
- `git diff --check`: no whitespace errors.
- Production thermal source contains no remaining `ConservativeAirGeometry.Face.values()` call.

## Remaining

- Repeat the same real-save JFR workload and compare thermal allocation, G1 humongous allocation, Full GC pauses, and sampled server CPU against the saved baseline.
- If Page rebuild remains a CPU hotspot after allocation removal, use the new recording to distinguish expected Page admission/mutation from avoidable rebuild scheduling before changing topology granularity.
