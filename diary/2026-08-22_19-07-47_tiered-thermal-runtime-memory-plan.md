# Tiered Thermal Runtime and Memory Plan

- Time: `2026-08-22 19:07:47 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Consolidated the proposed temperature runtime into three fixed backends and three internal execution-cost tiers: analytic background, cached stateless local surface, and reduced stateful RC island.
- Changed receiver interest from unconditional island creation to minimum-sufficient tier classification during cold compile or geometry revision; upgrades are monotonic for a lease lifetime.
- Defined outdoor ice/snow background handling, cached local ice/lava legacy responses, and RC promotion for enclosure, convection, power, heat capacity, residual heat, and phase-change requirements without per-block emitters or material nodes.
- Added bounded receiver compilation, packed delta coalescing, sparse `4^3` revision metadata, per-island query projection publication, stable player tick buckets, shared work deduplication, and overload fallbacks.
- Added primitive-memory targets, admission before allocation, bounded bindings/lookups/witnesses/radiation caches/queues, CLOCK reclamation, publication peak reservation, and explicit physical-source memory exhaustion behavior.
- Added cost equations, byte estimates, 50/100-player scenarios, cold-miss workload ranges, implementation phases, failure cases, and production gates for CPU, retained heap, allocation, GC, correctness, and snapshot age.
- Expanded local-result lifecycle rules so background and surface evidence cannot accumulate across revisions, while RC residual enthalpy is retained until the stateful island is safely reclaimed.

## Decisions

- The production candidate does not scan or copy `16^3` sections, build a global snow/ice/lava index, create fixed thermal grids, or allocate per-block thermal state.
- Background and cached-surface results do not allocate `IslandSlot`; only features requiring thermal history or transport pay for an RC island.
- Tier selection is not a fourth backend or per-query solver race. Profiles define allowed approximations, compilation chooses the cheapest sufficient tier, and shadow benchmarks determine which profiles may remain stateless.
- Geometry witnesses are deterministic validity evidence. They must be compressed, split, or rejected at capacity, never silently truncated or retained as an unbounded history.
- The architecture is a measurable Pareto candidate, not an unqualified optimum. Production replacement remains gated on comparisons with the current implementation, cached analytic surfaces, reduced graphs, bounded per-block graphs, and an offline reference solver.

## Validation

- Verified sequential top-level headings `1` through `30` and coherent numbered subheading parents.
- Verified all `136` Markdown code-fence lines are balanced.
- Verified all five relative documentation links resolve and the plan contains no machine-local PDF path or trailing whitespace.
- Ran `git diff --check` for the target plan; it reported no content errors, only the existing LF-to-CRLF working-copy warning.
- Did not run Java tests because runtime behavior and `docs/climate/` were intentionally unchanged.

## Remaining

- Execute Phase 0 on fixed hardware to lock legacy behavior and collect reproducible CPU, retained-heap, allocation, GC, multiplayer, and network baselines.
- Implement the three pure-Java/shadow production candidates and offline reference model before selecting per-material thresholds or replacing any Minecraft query path.
