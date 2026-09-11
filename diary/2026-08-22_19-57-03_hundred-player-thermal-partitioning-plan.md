# Hundred-Player Thermal Partitioning Plan

- Time: `2026-08-22 19:57:03 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Verified and recorded current-player costs beyond the particle count: up to eight asynchronous `PalettedContainer` copies, about `164 KiB/worker` of raw `WorkBuffer` arrays, and the exact-position/`64`-tick seed-window submission skip in `TemperatureThreadingPool`.
- Verified and recorded endgame-base amplification paths outside the player solver: `ChunkHeatData` copies a radius-24 field into roughly `16..25` chunks, crop random ticks and `CropGrowEvent.Pre` can both query temperature, and town scanners repeatedly query every interior air voxel within a `4,096`-position scan cap.
- Kept the fixed three backends and three local execution tiers, while dividing connected RC islands into receiver/gate/low-conductance partitions joined by single-owner conservative portal edges.
- Added `ACTIVE_FULL -> SLEEPING_FULL -> DORMANT_CAPSULE -> RECLAIMED`; capsules preserve total enthalpy, port/query projections, and bounded slow modes while releasing full node, edge, and witness arrays.
- Added packed source lifecycle records with per-node power accumulators, fixed-array small-graph kernels, per-partition snapshots, field-local invalidation, and shared deterministic query frames.
- Added crop touched-region reuse and `TownThermalProjection`, so passive consumers cannot create interest and stable town refreshes no longer need temperature queries for every interior voxel.
- Replaced the multiplayer complexity and memory model with full-partition, capsule, source, query-frame, and town-group terms. Added explicit `100 players in 10 shared bases` and `100 players in 100 separate bases` scenarios.
- Reordered implementation so the full conservative partition graph is validated before capsule reduction, and shared query frames are validated before crop/town migration.

## Decisions

- `IslandRuntime` remains the connected-component ownership and single-writer boundary; partition is the minimum solve, sleep, compression, wake, and publication boundary. This avoids a generic multi-backend runtime or room flood fill.
- Every portal has one owner and a monotonic integration clock. Each interval is computed once and applied equal-and-opposite to both partitions.
- Capsule reduction is conditional model-order reduction, not a tier downgrade. Active phase changes, nonlinear gates/flow, unresolved fast modes, or consumers needing distinct internal temperatures prevent compression.
- Query frames cache only deterministic natural/local/analytic air composition. Player-specific campfire occlusion, body/equipment effects, and compatibility Gaussian noise remain outside the shared result.
- Ordinary machines have no thermal state by default. Declared thermal sources add packed lifecycle entries and node accumulator deltas; only machines with independent thermal history receive capacity nodes.
- The architecture remains a hundred-player production candidate, not a proven capacity result. The `<8 MiB` target and `16..32 MiB/dimension` hard cap must be validated on fixed hardware with realistic bases.

## Validation

- Verified top-level headings are sequential `1..30` and all numbered subheadings are sequential under their parent.
- Verified all `160` Markdown code-fence lines are balanced.
- Verified all five relative documentation links resolve.
- Verified the plan contains no machine-local PDF path and no trailing whitespace.
- Ran `git diff --check` for the target plan; it reported no content errors, only the existing LF-to-CRLF working-copy warning.
- Did not run Java tests because runtime code and `docs/climate/` were intentionally unchanged.

## Remaining

- Execute Phase 0 on fixed server hardware and capture reproducible player, Generator, crop, town, retained-heap, allocation, GC, and multiplayer endgame-base baselines.
- Prototype the full partition graph first, then measure small-kernel thresholds, capsule mode/error bounds, query-frame buckets, town projection group caps, and wake latency before enabling any optimization in production.
