# Receiver-Driven Background-Substrate Thermal Plan

- Time: `2026-08-22 18:25:09 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Upgraded the pre-implementation thermal architecture to keep the three fixed backends while making `LocalTransportBackend` receiver-driven.
- Separated material behavior from discovery roles, moved outdoor ice and snow to an analytic natural background, and limited stateful material coupling to locally observed receiver interfaces.
- Defined the constant-size `1x2` enclosed-ice reduction, lazy enthalpy-preserving air-layer splits, one- or two-pole RC material reservoirs, and deterministic first-hit geometry compilation.
- Replaced immediate per-block face reads with packed old/final-new delta coalescing, sparse `4^3` revision microtiles, burst dirty-tile collapse, and exact gate generations.
- Replaced world-level snapshot copying with immutable per-island publication, single-writer solving, slot generations, and acquire-time snapshot validation.
- Added query-purpose activation permissions, multiplayer tick bucketing, same-island work deduplication, hierarchical fair budgets, sleeping/CLOCK eviction, and compact change-driven body-temperature synchronization.
- Reordered Phase 0-8 and expanded the reference, ice/snow, lava-to-obsidian, crop/machine, publication, network, and 50/100-player validation gates.

## Decisions

- No global ice/snow block or surface index, mandatory `16^3` root, section scan, or per-block material node belongs in the production architecture.
- Low-discrepancy and hierarchical probes may estimate error or help cold lookup, but deterministic faces/DDA and revision witnesses own topology truth.
- A `4^3` microtile is revision metadata only; it is never a query/simulation grid and never implies a 64-block read.
- Players, physical sources, and registered machines may create thermal islands. Crops, town scans, and ordinary block queries may only reuse published islands or fall back to the natural backend.
- Runtime behavior did not change, so living `docs/climate/` files were intentionally left untouched.

## Validation

- Verified the plan retains sequential top-level headings `1` through `30`.
- Verified all Markdown code fences are balanced and all five relative documentation links resolve.
- Verified the repository document contains no machine-local path to the reviewed PDF.
- Ran `git diff --check` for the target plan; it reported no content errors, only the existing LF-to-CRLF working-copy warning.
- Did not run Java tests because this change only updates a plan and diary.

## Remaining

- Execute Phase 0 to lock current behavior, rebuild reproducible performance baselines, and implement the offline reference contract before changing production runtime behavior.
- Use the defined shadow benchmarks and production gates to decide per-material analytic versus reduced-graph behavior; do not claim absolute optimality before those measurements.
