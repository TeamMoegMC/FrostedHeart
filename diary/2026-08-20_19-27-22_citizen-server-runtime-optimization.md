# Citizen server runtime optimization

- Time: `2026-08-20 19:27:22 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `partial`
- Scope: `Citizen server registry, active simulation iteration, AOI candidate selection, delta synchronization, tests, and living documentation`

## Completed

- Reused double-buffered container/id registries and duplicate-ID scratch in `CitizenSimScheduler.refreshRegistry`.
- Reduced `TownSimData` and `UnmanagedCitizenData` initial `CitizenSim` capacity from 64 to 16 while retaining the existing minimum and doubling growth policy.
- Added pooled, per-container active stable-ID lists inside `CitizenSimScheduler`; `BehaviorSystem` and `MovementSystem` now consume active IDs rather than scanning inactive rows every tick.
- Added pooled 16-block visibility cells to the existing `SpatialGrid`; `SyncEngine` now performs coarse AOI candidate collection followed by the existing exact eligibility and 96-block distance checks.
- Changed delta collection to unique tracked IDs and reused player coordinate arrays while preserving all-player nearest-distance tiers, per-player tracked packet grouping, immediate updates, halt priority, and end-of-flush canonical writeback.
- Updated `docs/town/hybrid-simulation-architecture.md` and the implementation plan. Added capacity/growth and visibility-grid regression coverage.

## Decisions

- Added no production Java classes and changed no NBT schema, packet wire format, configuration default, visibility radius, cap, or behavior cadence.
- Stable IDs remain the active-list key because `CitizenSim.remove` uses swap-remove; persistent row indexes would be incorrect.
- Visibility cells include every valid behavior state, including sleepers. `CitizenPresence.presentationEligible` remains authoritative for valid-bed filtering.
- Packet-owned lists and `FriendlyByteBuf` were not pooled because their asynchronous encoding lifetime has not been profiled or proven safe.

## Validation

- `./gradlew.bat test --console=plain`: `BUILD SUCCESSFUL in 39s`; 9 actionable tasks, 4 executed and 5 up-to-date.
- `git diff --check`: passed; only LF/CRLF conversion warnings were emitted.
- Static review confirmed `pendingImmediate`, halt-edge priority, exact AOI ranking, server-wide cap selection, and canonical writeback order are unchanged.

## Remaining

- Run the fixed 10,000-total / 3,000-active, 1/2/4-player game scenarios from the plan and record spark/JFR before/after distributions.
- Verify registry allocation stacks, cold-population scaling, AOI/delta scaling, GC, retained heap, and network byte counts in game. No runtime performance result is claimed by this entry.
