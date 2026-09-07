# Thermal nested reachability and allocation closure

- Time: `2026-08-30 18:27:56 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `confirmed nested dead code, signature ownership, loaded-section memory, source limits, arena allocation, sparse mutation capture, tests, and living documentation`

## Completed

- Removed the production-unreachable stateless/natural-rock material branches,
  deep poles, fixed material boundaries, snow/ice/custom transition actions,
  custom handler, unused mutation policies, missing-port redistribution, and
  `SourceChannel.OTHER`. Current capacitive surfaces, recipe-driven phase
  transitions, continuous sources, and retained `IMPULSE` behavior remain.
- Replaced `LocalAirRegionPattern` and the worker reconversion pass with one
  canonical `ConservativeAirGeometry.Resolution`. Removed five signature fields
  that every production classifier supplied as zero.
- Removed the remaining handwritten test-only or zero-call members, including
  `ContactPattern.fullBlock`, `PageState.resolved`, buoyancy factor/status
  accessors, and exchange-result status accessors.
- Simplified `ThermalMemoryBudget` to its real publication/radiation byte
  admission role. Added explicit dimension bounds of `65,536` physical sources
  and `131,072` retained source nodes so primitive source storage cannot grow
  without limit.
- Made loaded-section mutation bitsets and Page-halo directories lazy. Added an
  adaptive Page dirty-center bitmap only after the small-array threshold.
- Replaced boxed free-span trees with pooled primitive AVL nodes plus primitive
  boundary maps. The hierarchical live-word summary now tracks only LIVE cells,
  and old-span release performs one arena ownership validation.
- Reused one `TopologyView`, narrowed same-package runtime contracts, and
  restored scalar-only `FarFieldSettings` to a compact package record.

## Validation

- Java 17 `compileJava`, `compileTestJava`, and `compileGameTestJava`: successful.
- Thermal JUnit: `95/95`, zero failures, errors, or skips.
- Forge GameTest: all `14/14` required tests passed.
- Arena churn coverage exercises repeated best-fit split, merge, tail trimming,
  and slot reuse through production allocation/release APIs.
- Final residual-name search found none of the removed models, fields, helpers,
  policies, or boxed arena indexes.
- `git diff --check`: passed.

## Documentation

- Updated `thermal-runtime-architecture-and-optimization.md` for stable worker
  Brick directories, lazy section memory, primitive free-span indexing, source
  limits, current phase behavior, and final validation counts.
- Updated `heat-production-and-network.md` to describe current surface-pole and
  source-limit behavior rather than unregistered deep-pole functionality.
- Updated the active async-runtime plan with checked closure items and outcome.

## Remaining

- Controlled JFR/heap profiling remains the existing architecture acceptance
  task; no further source cleanup is reserved by this entry.
