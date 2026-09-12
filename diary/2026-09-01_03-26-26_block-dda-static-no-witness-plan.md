# Block DDA and static no-witness radiation plan

- Time: `2026-09-01 03:26:26 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `optional static Block radiation tracing and thermal residency plan contracts`

## Completed

- Added one shared block-grid DDA as the final radiation occlusion traversal.
  Whole-BlockState occlusion no longer justifies quarter-cell stepping.
- Added one `collectWitnesses` tracer argument. Static rays use false and perform
  no Page, mutation-owner, section-revision, witness-array, or receiver-cache
  work; physical rays use true and preserve the existing witness behavior.
- Added one per-trace current-section reference so BlockState reads require at
  most one loaded-only section lookup per crossed section instead of one chunk
  lookup per entered block.
- Updated Stage 1, exact costs, functional tests, performance validation,
  acceptance criteria, rejected alternatives, documentation impact, and outcome.
- Corrected the thermal residency plan so static coverage and physical witness
  ownership may reuse mutation owners without creating or retaining a thermal
  Page.

## Decisions

- Keep exact source/receiver doubles, corner/edge tie handling, the safety step
  cap, and quarter-position receiver-cache identity. Only traversal resolution
  changes from quarter-cell to block-grid.
- Use one boolean on the existing tracer call instead of a second tracer,
  interface, cache, or duplicated DDA implementation.
- The conservative 8-block static bound is 24 block-boundary advances per ray;
  100 one-Hz receivers with eight selected emitters are bounded by 19,200
  advances per second total.
- Prove result equivalence with a test-owned quarter-grid reference, then delete
  the old production traversal rather than retaining a compatibility path.

## Validation

- `git diff --check`: passed; only the repository's existing LF-to-CRLF warning
  was reported.
- Every relative Markdown link in both updated plans and this diary resolves.
- Targeted conflict search found no remaining 96/76,800 quarter-grid cost,
  static revision/witness reuse, Page-neighbor rebuild, or per-block chunk-lookup
  instruction.
- Java tests were not run because this change modifies plans and diary only.

## Remaining

- Implement the receiver-lazy index and Stage 1 block-DDA/no-witness changes,
  then run the focused equivalence, lifecycle, JFR, and live fire/lava fixtures.
