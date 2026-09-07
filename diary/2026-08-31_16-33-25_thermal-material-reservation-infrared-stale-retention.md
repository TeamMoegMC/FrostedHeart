# Thermal material reservation and infrared stale retention

- Time: `2026-08-31 16:33:25 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `ThermalSolver material-edge preparation and MinecraftThermalInput infrared snapshot availability`

## Completed

- Changed `MaterialEdgeTable.reserve` to cover the larger of final edge count
  and the insertion-before-deletion peak, preventing prepared commits from
  exhausting an unchanged-final-size table.
- Changed temporarily invalid or over-age infrared publications to omit the
  response instead of sending an authoritative empty full snapshot. The client
  retains its existing mirror until normal change-ID/presence recovery data is
  available; valid retirement behavior is unchanged.
- Updated the thermal runtime, climate, lifecycle, and infrared plan documents.

## Decisions

- Kept reservation in topology preparation so `TopologyCommitter` remains
  allocation-free and single-pass.
- Reused the existing client temperature mirror and null-response packet path;
  no cache, timeout, packet type, observer, or per-player server state was added.
- Did not modify Page lifecycle replacement. Its rare closure exception was not
  reproduced independently from the material-edge restart storm.

## Validation

- `compileJava compileTestJava` passed.
- Thermal and infrared codec JUnit passed: `105/105`.
- Forge GameTest passed: `14/14` required tests.
- Added `materialReservationCoversInsertionBeforeDeletionPeak`, which exercises
  a full table, one insertion before one deletion, and unchanged final size.
- `git diff --check` passed with only existing line-ending warnings.

## Remaining

- Repeat the reported live topology churn and confirm that material-capacity
  worker failures and infrared orange/blue clearing no longer occur. Investigate
  replacement closure separately only if it remains after this fix.
