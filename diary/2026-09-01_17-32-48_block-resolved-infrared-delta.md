# Block-resolved infrared delta protocol

- Time: `2026-09-01 17:32:48 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `infrared query publication epochs, changed-Brick network codec, block-resolution client texture, shader sampling, tests, and climate documentation`

## Completed

- Replaced Page-representative infrared temperatures with one server-resolved
  actual Air-cell sample per world-block center while preserving the existing
  solver topology and 20-tick transport step.
- Reused topology and quantized-temperature dirty events to atomically stamp
  fixed Page/Brick epoch arrays. Presence changes now add/remove individual
  Pages, and no-visible-change polls send no S2C response.
- Added the flat `INVALID`/`UNIFORM`/`INDEXED`/`RAW` Brick codec and kept one
  bounded reusable server builder without per-player state or history.
- Replaced the client heap mirror/full-delta upload with one direct `144^3`
  `R16I` mirror, one 8 KiB Page scratch, and `16^3` Page subimage uploads.
- Updated the infrared shader to one block per texel and retained its single
  temperature fetch and existing unpack-state reset contract.
- Updated the climate living documentation and implementation plan.

## Decisions

- Brick remains the topology, residency, dirty, and wire-record address; it no
  longer defines display resolution.
- Block-center sampling is exact for full-block wall separation but does not
  claim arbitrary sub-block component accuracy.
- Changed data outside a client's view does not need an empty epoch ACK. Keeping
  the older client epoch preserves correctness and avoids repeated empty S2C.
- Kept 40-tick entity-ID-staggered polling, no observer/cache, no infrared Page
  admission, and no change to the unaccepted `airMixingWPerBlockK` candidate.

## Validation

- `compileJava`, `compileTestJava`, and `compileGameTestJava`: passed.
- Selected thermal and infrared JUnit suite: `117/117` passed.
- Forge GameTest server: `14/14` required tests passed.
- Wall fixture resolves opposite block centers in one Brick to different Air
  slots and resolves the wall center as invalid.
- Wire tests cover all four modes, deterministic round trips, and the exact
  `935,296`-byte structural payload; the complete response is below 1 MiB.
- Stale-symbol search and `git diff --check`: passed.

## Remaining

- Run live Vanilla and Oculus/Embeddium infrared movement/crouch/full/delta
  rendering checks.
- Run the planned 100-client target-load JFR/heap/network measurement before
  marking the implementation plan completed.
