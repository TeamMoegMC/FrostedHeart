# Sparse Thermal Runtime V1 plan recovery and integration

- Time: `2026-08-24 02:31:23 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Recovered the exact pre-replacement temperature plan from successful Codex patch history: `229,541` bytes and `2,984` lines.
- Integrated the newer `Sparse Thermal Runtime V1 Engineering Specification` as the authoritative `0..91` implementation contract.
- Restored verified current-state evidence, compatibility and migration contracts, Phase 0 baselines, capacity accounting, competing prototypes, open parameters, and documentation impact as explicit appendices.
- Corrected the top-level backend model by retaining `NaturalBackend`, `AnalyticFieldBackend`, and `SparseThermalMeshBackend` while treating `CACHED_LOCAL_SURFACE` as an execution tier.

## Decisions

- The V1 core is `ThermalPage + MixedGeometryBrick + ThermalCell + BoundaryOperator` with uniform thermal time and hard work/memory budgets.
- The former RC semantic compiler, compiled-plan cache, island, partition, portal, guarded partition scheduler, and modal capsule are not parallel V1 paths; their replacement mapping is recorded in Appendix D.
- Existing behavior evidence remains migration input, not proof that the new runtime is implemented.

## Validation

- Verified recovery against the pre-replacement metadata, title, line count, byte count, and final Outcome.
- Verified the integrated plan retains all V1 numbered sections `0..91`, all implementation phases, correctness tests, metrics, failure conditions, gameplay examples, and Definition of Done.
- Documentation-only architecture work; no Java runtime behavior changed and Java tests were not required.

## Remaining

- Execute Phase 0 baselines and pure Java geometry/reference prototypes before changing production temperature paths.
