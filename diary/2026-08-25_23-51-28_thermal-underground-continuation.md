# Thermal underground continuation closure

- Time: `2026-08-25 23:51:28 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `Minecraft thermal Page admission, underground FarField frontier handling, tests, climate documentation, and active thermal plan`

## Completed

- Removed local open-direction count as outdoor evidence. Only loaded sky exposure can approve the full open-space FarField.
- Added one loaded-only continuation Page along each non-sky open face of a directly admitted player or physical-source Page. Continuations use `getChunkNow`, do not recurse, and have a hard `64 Page/dimension` ownership cap.
- Added a weak continuation boundary for approved in-domain profiles using `Gopen * actualArea / referenceArea * windScale / (1 + 16)` while keeping the topology degraded.
- Consolidated duplicate captured-Page admission code and preserved direct-interest promotion without adding a resolver, callback framework, or persistent adjacency graph.

## Decisions

- Multiple local exits are not proof of outdoors; tunnels and multi-branch caves can have several non-sky directions.
- A long tunnel is not collapsed directly into the full ambient impedance because the reference harness observed a threshold-crossing mismatch for that approximation.
- Continuation Pages are bounded loaded-state cache entries. Chunk unload removes them; the system never obtains tickets or forces chunk loads for thermal closure.

## Validation

- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --no-daemon --console=plain` passed `240/240` tests.
- `./gradlew.bat runGameTestServer --no-daemon --console=plain` passed all `20/20` required Forge GameTests, including the loaded-only non-recursive continuation path.
- `git diff --check` reported no whitespace errors; existing line-ending conversion warnings remain.

## Remaining

- Obtain production-like `1/10/50/100` player CPU, TPS, allocation, and retained-memory evidence before freezing multiplayer capacity claims.
- Surface composition, non-phase material calibration, cold-side phase authority, old `ChunkHeatData` migration, and real asynchronous scheduling remain separate work.
