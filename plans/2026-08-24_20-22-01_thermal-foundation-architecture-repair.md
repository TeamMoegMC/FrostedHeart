# Thermal foundation architecture repair

- Time: `2026-08-24 20:22:01 +08:00`
- Authors: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `content.climate.thermal.mesh`, `solver`, `source`, `phase0` scaffolding, tests, and climate documentation
- Related: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Goal

Repair the five verified foundation gaps before PR 6/7 work starts. The result must have one enthalpy/capacity authority, a real mixed-Brick mesh path, no obsolete Page-owned runtime state, no test-only Phase 0 claims in the production source set, and no `mesh -> phase0.reference` dependency.

## Verified Current State

1. `ThermalCellArena` owns private `H/C`, while `ThermalSweep` and `ThermalStepExecutor` mutate unrelated caller arrays. `ThermalSourceTimeline` reaches its destination through a generic consumer.
2. `ComponentBrickCompiler.CompiledBrick` contains component volumes, centroids, and face ports, but only tests/JMH consume it. `ImplicitAirAdjacency` skips non-regular coverage through `CoverageCellResolver`.
3. `ThermalPage` owns scalar solve watermarks, active/sleep state, degraded-time state, and a boundary span even though the later runtime contract defines five input streams and no runtime consumes these Page fields.
4. `Phase0aMutationAdapterContract` and `Phase0aDynamicExclusionIndex` are synthetic test contracts in `src/main`. The latter contradicts the frozen rule that moving structures are air while moving.
5. `PhaseZeroThermalRouting` and `ThermalBenchmarkEvidence` are self-contained evidence scaffolding, not runtime routing/reporting. `GeometryMigrationLedger` delegates production migration math backwards into `phase0.reference`.

## Decisions

- `ThermalCellArena` remains the only mutable `H/C` owner. Solver, executor, and source integration accept the arena directly; raw state arrays and the source destination callback are removed from that execution path.
- A thermal-node source binding targets an arena slot plus lifecycle generation. A stale or released target is rejected before energy is removed from the accumulator.
- `CompiledBrick` is reused directly. Mixed components receive ordinary arena slots; one Brick support ref identifies the component span and its existing face-port table. No second mixed geometry model is introduced.
- `ImplicitAirAdjacency` becomes the concrete regular/mixed pair compiler. It intersects mixed face aperture masks, aggregates repeated component-pair patches, and emits `ThermalSweep.PairOperation` values. The future `CoverageCellResolver` callback is removed.
- `ThermalPage` owns geometry, coverage, revision, and coherent publication identity only. PR7 will own five-stream `InputWatermarks`, active/sleep state, time degradation, scheduling, and boundary-operator storage.
- Phase 0 evidence helpers that have no production caller move to the test source set. The obsolete dynamic-exclusion GameTest remains as a commented historical fixture; it is not executable production semantics.
- `GeometryMigrationLedger` becomes the neutral formula owner. `ThermalMigrationReference` verifies/delegates to it, reversing the current dependency without adding a new formula class.

## Data Flow

```text
Minecraft/fixture geometry snapshot
              |
              v
     ComponentBrickCompiler
       components + ports
              |
              v
  ThermalCellArena (only H/C authority)
       |                 ^
       | coverage refs   | exact integral(P dt)
       v                 |
    ThermalPage     ThermalSourceTimeline
       |
       v
 ImplicitAirAdjacency
 regular faces + mixed aperture intersections
       |
       v
 ThermalSweep.PairOperation[]
       |
       v
 ThermalStepExecutor -> ThermalSweep
       |
       +---- mutates the same ThermalCellArena in place
```

## What Already Exists

- Reuse `ComponentBrickCompiler.CompiledBrick`; do not rebuild component decomposition or face-port storage.
- Reuse `ThermalCellArena` allocation/replacement and `H=C(T-Tref)` accessors; extend its existing SoA ownership.
- Reuse `ThermalExchangeKernel`, `BuoyancyConductance`, `ThermalStepPlan`, `SolveEpoch`, and `InputWatermarks` unchanged.
- Reuse `ThermalSourceRegistry` and `NodePowerAccumulatorArena`; only replace their final destination boundary with the arena.
- Reuse `GeometryMigrationLedger`; move formulas into it instead of creating another utility.

## Implementation Steps

1. Make `ThermalSweep`, `ThermalStepExecutor`, and `ThermalSourceTimeline` arena-native. Add lifecycle-checked arena node writes and update solver/source tests to prove source and transport mutate the same slots.
2. Extend `ThermalCellArena` with mixed-component allocation using `CompiledBrick` volumes/centroids/ports. Keep regular and mixed cells in one dense Page span and expose only concrete scalar queries needed by pair compilation.
3. Replace `CoverageCellResolver` with direct regular/mixed traversal. Compile canonical pair operations using `G = kMix * A / (dA + dB)`, with a positive lower bound for mixed centroid-to-face distance.
4. Remove obsolete Page solve/runtime fields and boundary span placeholders. Retain geometry revision/topology/publication identity.
5. Move test-only Phase 0 classes out of `src/main`, comment the obsolete dynamic exclusion fixture, and make migration dependency direction neutral.
6. Update the main thermal plan, living climate integration documentation, and append a development diary entry.

Sequential implementation, no parallelization opportunity: the steps share arena slot identity, Page coverage, pair compilation, and solver/source tests.

## Failure Modes And Coverage

| Code path | Production failure | Handling | Required test |
|---|---|---|---|
| source -> arena | stale slot reused after Page rebuild | lifecycle generation mismatch leaves pending energy undrained | stale binding rejection and successful rebind |
| sweep -> arena | compiled operation references a released slot | reject before the first mutation | stale compiled sweep is atomic |
| mixed allocation | component capacity or support overlap is invalid | validate the full layout before allocation | invalid/misaligned/overlapping mixed support |
| mixed face pair | aperture masks do not overlap | emit no pair | mixed/mixed closed aperture |
| regular/mixed face pair | repeated ports create duplicate exchanges | aggregate by canonical cell pair | exact pair count and summed fractional area |
| Page publication | mixed coverage points at another Page or non-support component | reject compilation | foreign/invalid coverage ref |
| migration | complete air creation/removal uses an empty side | signed ingress/egress and zero residual | empty-old and empty-new cases |

No new silent failure is accepted: stale topology and stale source bindings are explicit rejected states, while unsupported geometry remains the existing conservative Page fallback.

## Test Coverage Map

```text
arena allocation
  +-- regular cells                         [existing unit tests]
  +-- mixed component H/C + lifecycle      [new unit tests]
  +-- invalid mixed layout                  [new unit tests]

pair compilation
  +-- regular <-> regular                   [updated existing tests]
  +-- regular <-> mixed fractional ports   [new unit tests]
  +-- mixed <-> mixed aperture overlap      [new unit tests]
  +-- stale owner/neighbor publication      [updated existing tests]

solve execution
  +-- closed-system conservation            [updated existing tests]
  +-- boundary energy ledger                [updated existing tests]
  +-- source integral enters arena H        [updated integration test]
  +-- same arena H is swept afterward       [updated integration test]
  +-- stale source/sweep target rejection   [new regression tests]
```

## Performance Boundaries

- No per-tick World reads, dynamic-shape compatibility scans, or moving-structure tracking are added.
- Mixed compilation may allocate temporary aggregation objects; stable sweeps retain only primitive operation arrays and mutate arena arrays in place.
- No persistent generic air-edge graph is introduced.
- This repair establishes a measurable runtime path but does not claim whole-server CPU or memory acceptance. Those figures still require PR7/shadow workloads.

## NOT In Scope

- FarField/static impedance: PR6 remains a separate holdout gate.
- Dimension mailbox, scheduling, sleeping, memory admission, and publication buffers: PR7 owns them.
- Minecraft production mutation/query wiring: PR8 remains gated.
- Third-party dynamic-shape compatibility beyond the existing conservative unsupported interface.
- Moving contraption thermal state or exclusion tracking: moving structures remain air.
- Material boundaries, phase reservoirs, radiation, and gameplay consumer migration.

## Validation

Run once after implementation:

```text
gradlew test runGameTestServer --no-daemon --console=plain
```

Acceptance requires all JUnit and required Forge GameTests to pass, no production reference to the relocated Phase 0 evidence helpers, and no remaining raw-array solver entry point.

## Outcome

Completed on `2026-08-24`.

- `ThermalCellArena` is now the single mutable `H/C` authority for regular cells and compiled mixed-Brick components. Source integration writes exact energy directly to it; `ThermalSweep` binds the same arena and captures endpoint lifecycle generations.
- `ImplicitAirAdjacency.compileOwnedPairs` now emits concrete regular/regular, regular/mixed, and mixed/mixed operations from published coverage and `CompiledBrick` aperture ports. The resolver callback was removed.
- `ThermalPage` now owns geometry, coverage, topology/publication identity, and one cell span only. Scalar solve watermarks, active/degraded state, boundary span, and unused solve bookkeeping were removed.
- Phase 0 routing/evidence and synthetic mutation/exclusion helpers moved to the test source set. The obsolete moving-structure exclusion GameTest remains commented; moving structures are air while moving.
- `GeometryMigrationLedger` now owns production migration formulas, and `ThermalMigrationReference` delegates to it.
- Stale source delivery is preflighted before any batch write and retains pending energy. A frozen sweep rejects released or reused endpoint generations before its first mutation. The executor rejects source and sweep objects bound to different arenas.

Validation: Java 17 `gradlew test runGameTestServer --no-daemon --console=plain` passed with thermal JUnit `177/177`, repository JUnit `705/705`, and required Forge GameTest `14/14`.

PR6 FarField and PR7 runtime scheduling/publication remain separate work. No Minecraft gameplay query or production route was enabled by this repair.
