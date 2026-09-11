# Thermal frontier mask and capacity escalation plan

- Time: `2026-09-01 04:03:57 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `cross-section thermal Brick residency plan, frontier hot path, and capacity gates`

## Completed

- Removed the stale fifth `required` Page mask from every final architecture
  description. Absolute desired residency remains completion scratch plus the
  main-thread last-accepted value.
- Reused the byte removed with `WorkerBrickTopology.continuationFaceMask` as
  coarse `outerOpenFaceMask`. Mixed Bricks retain exact face-port/component
  checks after the coarse rejection.
- Replaced any implied 64-Brick frontier loop with six fixed Page-local
  directional masks, masked resident-bit shifts, and set-bit iteration.
- Defined threshold selection as a sweep for the largest correctness-valid
  `REFINE_HIGH` and highest lower non-toggling `RELEASE_LOW`.
- Added an ordered capacity response that distinguishes live-cell admission,
  pair storage/execution, converged-source CPU, and true cell-count failures
  before reopening another solver or LOD.

## Decisions

- Retain exactly four worker Page masks: `resident`, `resolved`, `sourceSeed`,
  and `hot`. Add no retained frontier mask, candidate array, or hot-Page index.
- Keep the Page-header scan bounded by `maximumPages`; skip exact frontier work
  when `sourceSeedMask | hotMask` is zero.
- If only the `65,536` live-cell gate fails while existing arena/work/heap/P99
  gates pass, raising only that limit up to the existing `131,072` arena-slot
  ceiling is the first admissible response. This is not pre-authorized without
  workload evidence.
- Reopen matrix-free regular Air only for pair cost, steady-source sleep only
  for converged powered-domain CPU, and all-air LOD only for a persistent live-
  cell count failure.

## Validation

- `git diff --check`: passed; only the repository's existing LF-to-CRLF warning
  was reported.
- Every relative Markdown link in the updated plan and this diary resolves.
- Targeted conflict search found no five-mask final state, retained `required`
  authority, 64-Brick frontier-loop instruction, unordered alternative-solver
  escalation, or missing `outerOpenFaceMask` validation contract.
- Java tests were not run because this change modifies plan and diary only.

## Remaining

- Implement the Brick residency correction beginning with the threshold sweep,
  then run its functional, 100-source, JFR, heap, and capacity-specific gates.
