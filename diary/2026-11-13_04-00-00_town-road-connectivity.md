# Town road connectivity

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; offline compileJava passed
- Scope: town building road access to the town energy tower

## Changes

- Reuse room standing, step and ladder rules for bounded 60-step searches between controller-adjacent standing cells.
- Union connected building nodes, sharing the tower connection across each cluster.
- Two shared background workers request classified section snapshots; each town services at most two sections per tick without loading chunks. Release section caches per source search.
- Wait 1200 ticks after completion before the next batch. Reject results after tower, dimension or building membership changes; never overlap a town's batches.
- Require tower connectivity for building workability and ordinary daily work. Preserve residential maintenance settlement.
- Carry connectivity through incremental and full management synchronization, not disk recovery.
- Document behavior and limitations in docs/town/building-data.md.

## Validation

- Initial offline compileJava passed with existing deprecation/mixin warnings.
- Final ./gradlew --offline compileJava --no-daemon: BUILD SUCCESSFUL (2m 18s), 21 existing warnings. git diff --check passed.
- No GameTest or in-game validation performed. Controller-adjacent entry selection and multiblock tower access require gameplay acceptance checks.
- Snapshot acquisition spans ticks; road changes are reflected by periodic recalculation rather than an atomic terrain snapshot.
