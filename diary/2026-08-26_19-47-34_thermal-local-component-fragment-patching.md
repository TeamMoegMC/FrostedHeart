# Thermal local component and fragment patching

- Time: `2026-08-26 19:47:34 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Minecraft thermal mutation batching, Air connectivity, sweep storage, tests, living documentation, and active thermal plan`

## Completed

- Added batch-final Brick cancellation so a break/place sequence that restores the installed topology advances the geometry acknowledgement without allocating cells, migrating enthalpy, patching the sweep, rebuilding FarField, or incrementing topology generation.
- Replaced ordinary-mutation active-dimension work with identity dirty Page sets and the installed sorted Page/section index. Ordinary mutation no longer recreates or sorts active Pages and does not scan `arena.highWaterMark()`.
- Added Brick-addressable `ThermalSweepFragments` for primitive Air pairs, aggregated material contributions, material boundaries, phase contacts, and slot-addressed FarField boundaries. Runtime topology installation commits one pending fragment patch while it owns the logical writer.
- Replaced repeated global FarField union-find with persistent primitive adjacency, `componentBySlot`, and component members. Edge insertion merges endpoint components; edge deletion flood-fills only touched old components to identify splits and re-emits only affected boundaries.
- Kept natural FarField temperature once per Page, shared singleton empty fragments, and only one unresolved-open-patch integer per Brick instead of retaining each compiled open-patch object's arrays.
- Fixed simultaneous material traversal replacements so disjoint dirty Page ranges accumulate by traversal position rather than overwriting each other.
- Updated `docs/climate/data-lifecycle-and-integration.md` and the active sparse thermal runtime plan to describe the installed local patch path and its explicit full-rebuild boundaries.

## Decisions

- Preserve Page/Brick canonical traversal, material contribution aggregation order, forward/reverse execution, boundary/phase order, compensated state summation, stale-slot rejection, source accounting, and arena `H/C` ownership.
- Allow full traversal only for Page admission/retirement or fragment-count changes, global FarField input changes, unknown-position resync/ring recovery, and amortized primitive-capacity growth.
- Keep one derived primitive Air adjacency index because component-local split detection cannot be exact from pair fragments alone; it stores no thermal state and is rebuilt from the authoritative fragment layout when lifecycle changes.

## Validation

- `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `214/214` passed with zero failures, errors, or skips.
- Forge GameTest: `14/14` required tests passed.
- Coverage includes batch-final topology cancellation, unrelated-component isolation, local opening merge/closing split, fragmented/flat sweep equivalence in both directions, two disjoint traversal replacements in one patch, and repeated fragment replacement without state-reference underflow.

## Remaining

- Use the same real-save mutation workload in a future JFR comparison to quantify CPU and allocation improvement; no further global traversal remains on the ordinary known-position mutation path.
