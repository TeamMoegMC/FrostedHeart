# Thermal deferred mutation scan reduction

- Time: `2026-08-26 17:20:01 +08:00`
- Author: `Codex; OpenAI; primary implementation and verification agent`
- Status: `completed`
- Scope: `Minecraft thermal mutation capture, full-resync observation, topology drain, physical source reconciliation, and integration tests`

## Completed

- Preserved exact block coordinates from off-thread `LevelChunkSection#setBlockState` callbacks in a lazy section-owner bitset instead of upgrading the affected Page to a 4096-signature full-resync.
- Coalesced repeated positions and used a 64-bit nonempty-word mask so main-thread drain visits only populated words and blocks. The temporary `long[65]` is released after drain; stable sections retain no 520-byte bitmap.
- Replayed each deferred position through the existing loaded-only resolver dependency path and reconciled the final Campfire state before physical source flush.
- Added a monotonic dimension-level handoff revision so a mutation arriving across drain/deadline reset receives a later fixed deadline.
- For mutation recovery, kept authoritative 4096-signature capture only when the source has genuinely unknown positions. Initial Page admission still performs its one-time complete capture. Interior reads now use the already located `LevelChunkSection`; only dependency halo reads query neighboring loaded chunks.
- A semantically unchanged full-resync now acknowledges its exact sticky token, republishes the unchanged Page identity, and skips global pair/FarField topology compilation.

## Decisions

- Ordinary main-thread mining was already Brick-local and did not scan 4096 blocks. The remaining full scan is retained for raw container or section replacement where no dirty coordinates exist; removing it would make the resnapshot non-authoritative.
- No persistent per-Page checksum, global BlockState table, profiler counter, JMH source set, or new runtime subsystem was added.

## Validation

- `compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully on Java 17.
- Thermal JUnit: `206/206` passed.
- Forge GameTest: `14/14` required passed, including `13` thermal scenarios and `1` Frosted Research scenario.
- The production-shaped mutation GameTest now proves an off-thread single-block callback produces `fullResyncPages == 0`, rebuilds its local Brick, and retains an unaffected Brick's arena support.
- `git diff --check` reported no whitespace errors; only existing CRLF conversion warnings remain.

## Remaining

- Use a fresh production JFR with the repeated door/mining workload before claiming a measured CPU reduction. The code-level scan bounds are verified, but no percentage is inferred from GameTest timing.
