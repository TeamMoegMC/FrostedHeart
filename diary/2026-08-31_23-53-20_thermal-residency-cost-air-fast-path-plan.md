# Thermal residency cost and all-Air fast-path plan correction

- Time: `2026-08-31 23:53:20 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `active thermal Brick residency plan cost bounds and cold Page capture`

## Completed

- Corrected residency evaluation from a frontier-only claim to
  `O(P_active + F_frontier)`, deliberately retaining a bounded Page-header scan
  instead of adding an incremental frontier index.
- Added the exact `LevelChunkSection.hasOnlyAir()` cold-admission fast path with
  one shared immutable uniform-Air Brick payload and zero individual BlockState
  reads.
- Documented the complete cold Page cost: one natural sample, 256 existing
  heightmap-column queries, and either zero all-Air state reads or 64 state reads
  per requested Brick for a non-air section.
- Added validation for shared Air payload identity, exact sparse capture counts,
  and separate Page-header/frontier-face visits.

## Decisions

- Keep full 256-column sky capture for a new Page. Sparse sky-known state would
  add masks, unknown-sky semantics, and incremental lifecycle for insufficient
  benefit.
- Do not add a retained frontier-Page index; scanning at most `maximumPages`
  headers once per residency evaluation is the lower-complexity whole-system
  choice.
- No other solver, LOD, impedance, room, cache, or sleep mechanism entered the
  implementation scope.

## Validation

- `git diff --check` passed for the active plan with only the existing
  line-ending warning.
- Every relative Markdown link in the active plan resolves.
- Plan search confirms the all-Air proof, complete admission-cost formula,
  corrected complexity, and corresponding acceptance scenarios are present.
- Java tests were not run because this work changes plans and diary only.

## Remaining

- Implement the corrected plan beginning with its threshold reference fixture,
  then run the functional and controlled 100-source validation gates.
