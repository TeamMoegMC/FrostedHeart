# RC Partition Scheduler Implementation Plan

- Time: `2026-08-22 22:20:19 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: [`plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`](../plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md)

## Completed

- Rechecked the current section random-tick path, `SnowLayerBlockMixin_Melt`, `BlockStateBaseMixin_RandomTick`, `SchedulerQueue`, `ClimateCommonEvents#onServerTick`, and `CampfireTileEntityMixin_TimeLimit#cookTick` before revising the target architecture plan.
- Replaced the claim that every lit campfire creates or joins an RC island with an implementable two-step lifecycle: `O(1)` packed source registration first, then RC partition admission only after enclosure, capacity, phase-change, or transport evidence is confirmed.
- Specified a per-dimension `ThermalRuntime` using primitive struct-of-arrays, size-class arenas, partition offset/count headers, fixed active solve buckets, requested/processed wake epochs, and bounded SPSC wake/request/ack rings.
- Removed general crossing deadlines, per-snow timers, and first-production modal capsules. Active partitions advance from `lastIntegratedTick`; sleeping partitions retain the same primitive ranges but perform no per-tick solve; only empty, expired partitions are reclaimed.
- Defined local snow and material mutation as one packed request per aggregate patch. The worker integrates aggregate enthalpy, the main thread validates and changes at most one candidate under budget, and bounded six-neighbor/frontier refill replaces section scans or per-block scheduling.
- Updated performance and memory formulas, raw byte targets, implementation phases, failure modes, scenarios, and production gates to match the concrete runtime.

## Decisions

- Snow blocks do not schedule themselves. `LevelTickEvent` advances admitted thermal partitions; native snow random ticks remain only for ambient behavior outside local ownership.
- Existing `SchedulerQueue` is not reused for thermal partitions because its block-entity object list and linear position deduplication do not meet the hot-path memory or CPU requirements.
- The exact one-pole exponential is an integration optimization after a partition is processed, not a future-event scheduler. General multi-node crossing time is outside first-production scope.
- Modal `DORMANT_CAPSULE` reduction is deferred until retained-heap measurements prove that it beats direct storage of typical `2..16` node primitive graphs by a material margin and passes energy/projection error tests.

## Validation

- Verified top-level plan headings are sequential `1..30`.
- Verified all `184` Markdown fence lines are balanced.
- Verified all five relative climate documentation links resolve.
- Verified no trailing whitespace in the target plan.
- Ran `git diff --check` for the target plan; it reported no content errors, only the existing LF-to-CRLF working-copy warning.
- Documentation-only work; Java tests were not run because runtime behavior and `docs/climate/` did not change.

## Remaining

- Execute Phase 0 on fixed server hardware and record current forced-random-section, player simulation, source, crop, town, retained-heap, allocation, and multiplayer-base baselines.
- Implement and benchmark the pure Java primitive partition prototype before changing Minecraft production behavior.
