# Thermal resync, admission, and face paths

- Time: `2026-08-26 16:48:01 +08:00`
- Author: `Codex; OpenAI; primary implementation and verification agent`
- Status: `completed`
- Scope: `Minecraft thermal full-resync, captured Page admission, FarField face traversal, and coarse/fine pair ownership`

## Completed

- Kept the authoritative 4096-signature full-resync snapshot, but compared topology semantics into a 64-bit Brick mask and rebuilt only changed fragments. An unchanged snapshot clears the exact sticky token without replacing arena slots.
- Removed the temporary `16^3` all-air arena cell from captured Page admission. New captured Pages remain empty and unpublished until their final 64 world-aligned `4^3` fragments commit.
- Replaced volume-filtered FarField and continuation loops with direct 256-block face traversal.
- Corrected negative-side pair dependency refresh for a neighboring coarse support by resolving the fragment owner from the arena support origin in O(1).

## Decisions

- Full-resync still reads all 4096 signatures when mutation positions are unknown; eliminating that observation would make the snapshot non-authoritative. The expensive compiler, allocation, and migration work is now Brick-local.
- Explicit proven-all-air Pages may remain coarse until a real change; only the unnecessary transient coarse cell in captured admission was removed.
- No persistent checksum table, per-Page diff cache, profiler counter, or new runtime subsystem was added.

## Validation

- Java 17 offline Gradle: `compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `206/206` passed.
- Forge GameTest: `14/14` required passed, comprising `13` thermal scenarios and `1` Frosted Research scenario.
- The new aggregate test locks empty captured admission, one-Brick full-resync replacement, unchanged-snapshot slot stability, enthalpy stability, sticky-token ACK, and cross-Page coarse owner refresh.

## Remaining

- Record the same production mutation workload in a fresh JFR before assigning a measured CPU or allocation reduction percentage.
