# BlockTemp Bulk-Material Reduction Architecture

- Time: `2026-08-22 17:32:31 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Revised the thermal architecture to treat dense `BlockTempData` blocks as materials rather than per-block emitters.
- Added `ThermalMaterialProfile`, explicit legacy/thermostatic/capacitive/phase-change behaviors, deterministic surface evidence, bounded surface discovery, effective thermal penetration depth, and material-to-receiver coupling reduction.
- Replaced exact bulk connected-component ownership with `MaterialReservoirAggregate` keys so block removal does not require flood-fill or dynamic connectivity splits.
- Made surface patches compiler evidence only; the runtime solver receives aggregate `MaterialCouplingEdge` entries and queries continue to read immutable node snapshots.
- Added concrete ice, packed-ice, blue-ice, lava, magma-block, fluid-burst, profile-reload, and adversarial checkerboard behavior and tests.
- Added a falsifiable performance decision: compare cached analytic surfaces, the reduced material graph, a bounded per-block graph, and an offline finite-volume reference under one contract.
- Expanded the implementation path to Phase 0-8 with a primitive main-thread face-delta path, worker reduction, shadow competition, per-material selection, and production gates.

## Decisions

- Did not claim absolute maximum performance before benchmarks. A material enters the stateful graph only when its amortized compile/solve/query cost beats the analytic model while satisfying gameplay error limits.
- Kept material behavior data-driven rather than adding another full runtime backend or allowing regions to oscillate between complete solvers.
- Required confirmed exposed area for heat exchange and allowed missing area to underestimate temporarily; unvisited surfaces cannot contribute optimistic through-wall heat.
- Preserved current `BlockTempData.temperature`, `level_divide`, and `must_lit` through `LEGACY_SURFACE`; existing values are not reinterpreted as SI temperatures or powers.
- Bounded dense-material work by face budgets, coupling saturation, error tolerances, primitive delta batching, and sleeping thermal islands.

## Validation

- Verified all `30` numbered top-level headings are sequential.
- Verified all five relative Markdown links resolve, all `110` code-fence markers are balanced, and no trailing whitespace exists.
- `git diff --check` reported no content errors; only the existing LF-to-CRLF working-copy warning was emitted.
- Cross-checked current BlockTemp values: ice `-10`, packed ice `-20`, blue ice `-30`, lava `1000`, and magma block `500`; confirmed the current lava recipe does not enable `level_divide`.

## Remaining

- Implement Phase 0 competitive prototypes and collect reproducible multiplayer baselines before selecting production material behaviors.
- Calibrate surface transfer, flux saturation, effective penetration depth, and compatibility mappings in shadow mode.
- No living `docs/climate/` update is required because runtime behavior did not change.
