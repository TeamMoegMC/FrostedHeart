# Thermal PR7 Runtime Ownership And Publication

- Time: `2026-08-24 22:15:06 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `content/climate/thermal runtime/query ownership, scheduling, publication, admission, tests, plan and climate lifecycle documentation`

## Completed

- Added `ThermalMemoryBudget` with simultaneous server-global and dimension admission, separate `CRITICAL/OPTIONAL` accounting, and reservation ownership that charges replacement backing before old storage is released.
- Added `QueryPublication` with admitted preallocated double buffers, monotonic odd/even `publicationVersion`, lifecycle/geometry/topology/epoch envelope, one bounded retry, caller-owned output, O(1) sleeping republish, resize peak charging, and permanent lifecycle retirement.
- Added `DimensionThermalRuntime` as the sole logical writer around the existing latest-only scheduler, arena-native source/executor/sweep path, explicit non-source ACK, hard cell/pair/boundary caps, and conservative whole-solve-set sleep/wake.
- Added `ThermalRuntimeCoordinator` with a fixed primitive ready queue, one queued/running entry per dimension, sticky queue-full re-offer, FIFO/age promotion, reserved recovery capacity/quota, and generation-safe unload/replacement.
- Added `20` focused tests across memory admission, concurrent publication, runtime execution/sleep, coordinator overflow/fairness, and unload generation. Updated the implementation plan and living climate lifecycle documentation.

## Decisions

- Kept exactly one mutable `H/C` authority in `ThermalCellArena`; query publication contains only primitive temperature/medium/flags projections.
- Did not create a generic callback/event framework, another boundary solver, an unbounded executor queue, or partial Page sleep. Current `ThermalSweep` is aggregate, so sleep skips only a proven-stable complete solve set.
- Captured publication geometry/topology identity at worker start. A newer ACK arriving during a solve remains monotonic but forces old-envelope query fallback instead of allowing an old solve to claim a new revision.
- PR7 is a pure-Java correctness layer. It does not connect Minecraft world mutation producers or replace legacy gameplay authority.

## Validation

- PR7 targeted JUnit: `20/20` passed.
- Java 17 thermal JUnit: `206/206` passed.
- Java 17 repository JUnit: `734/734` passed with zero failures, errors, or skips.
- Forge GameTest: `14/14` required tests passed.
- `gradlew test runGameTestServer --no-daemon --console=plain`: `BUILD SUCCESSFUL`.
- `git diff --check`: no whitespace errors; only existing line-ending conversion warnings.

## Remaining

- PR8 Minecraft integration remains blocked by the Phase E approved FarField profile gate and missing Phase 0b production-like multiplayer/server evidence.
- When those gates are satisfied, PR8 must provide concrete geometry/source/chunk/profile/transition frame producers and query composition without reintroducing resolver callbacks or a second thermal state store.
