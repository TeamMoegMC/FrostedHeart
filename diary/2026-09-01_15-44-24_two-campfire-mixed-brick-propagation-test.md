# Two-campfire mixed-Brick propagation test

- Time: `2026-09-01 15:44:24 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `thermal frontier directions, mixed-Brick campfire binding, and Air mixing calibration evidence`

## Completed

- Added a deterministic fixture for the reported source Brick at section
  `(0,4,3)`, Brick index `2`, and verified all six open regular-Air frontier
  directions produce the exact same-section and neighboring-section masks.
- Added a mixed-Brick fixture for campfires at `(10,66,50)` and `(10,66,49)`.
  Both convection ports bind to one Air slot and deliver exactly `12,800 J`
  during the first bound one-second interval.
- Admitted every geometrically open neighbor in that fixture and verified heat
  reaches negative/positive X, negative/positive Z, and positive Y. The solid
  floor correctly prevents negative-Y residency.
- Verified the four horizontal neighbor temperatures differ by less than 5%; the
  upward neighbor is warmer through the existing buoyancy rule.
- Ran a test-only 60-second fixed-topology sweep. Source-to-horizontal-neighbor
  gradients were `10.8407 C` at `96`, `7.3670 C` at `192`, `3.5954 C` at
  `384`, and `1.6012 C` at `768 W/(block*K)`. Temporary sweep input/output was
  removed after measurement.

## Decisions

- The reported asymmetry is not caused by section addressing, frontier bit
  direction, missing cross-section pairs, or fixed solver traversal direction.
- Keep production unchanged until the existing `airMixingWPerBlockK` calibration
  is selected. Do not add source stencils, finer cells, or another propagation
  path.
- `384 W/(block*K)` is the first measured candidate below a 4 C local gradient;
  it is not accepted as the production default until the 100-source residency
  gate passes.

## Validation

- Focused `ThermalDimensionEngineTest`: passed.
- Complete thermal JUnit selection: `112/112` passed.
- `compileGameTestJava`: passed.
- Documentation behavior did not change; no living document update was needed.

## Remaining

- Run the controlled 100-source residency/JFR/heap gate for the measured
  `384 W/(block*K)` candidate before changing the production default.
