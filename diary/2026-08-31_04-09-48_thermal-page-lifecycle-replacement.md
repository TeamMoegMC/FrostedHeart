# Thermal Page lifecycle replacement

- Time: `2026-08-31 04:09:48 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal topology Page retirement/admission transaction`

## Completed

- Coalesced an old-handle retirement and newer same-section admission into one
  topology replacement draft.
- Reused the committed worker Page slot, migrated current Air/material heat,
  rebuilt the exact fragment neighborhood, rebound sources, and released old
  spans only after the new lifecycle was installed.
- Kept old phase-request identity out of the new Page lifecycle and cleared the
  retired handle publication after commit.
- Added a direct engine test with an active adjacent Page to cover ownership,
  old-span reference closure, slot reuse, and publication handoff.

## Decisions

- Retained the existing `TopologyPlan -> PreparedTopologyChange ->
  TopologyCommitter` transaction chain. No coordinator, inverse reference
  index, global scan, delayed admission, or persistent replacement state was
  added.
- A replacement temporarily stages one new Page while the old spans remain
  live, then returns to the original steady-state memory footprint.

## Validation

- Production, JUnit, and GameTest source compilation passed on Java 17.
- Focused thermal JUnit passed: `102/102`.
- Forge GameTest passed: `14/14` required tests.
- `git diff --check` passed.

## Remaining

- None.
