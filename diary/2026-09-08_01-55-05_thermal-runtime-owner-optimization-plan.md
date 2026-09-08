# Thermal runtime owner optimization plan

- Time: `2026-09-08 01:55:05 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering/design agent`
- Status: `completed`
- Scope: `thermal runtime discovery plan, existing owner overhead and validation`

## Completed

- Updated the [implementation plan](../plans/2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md) with explicit CPU/memory assumptions and separate correctness and optimization stages.
- Identified ownersByIdentity and per-owner AtomicBoolean allocations as existing structures, not the discovery failure's cause or new design overhead.
- Specified attachment-based identity lookup, conditional instance removal, inline atomic fields, and focused lifecycle/concurrency validation.

## Decisions

- Implement runtime discovery correctness first, then independently simplify the identity index and atomic allocations.
- Keep full normal machine output publication. Treat observe early return as a lower-priority measured optimization.
- Retain reusable mutation bitmaps and the single chunk discovery budget until measurements justify further changes.
- Documentation impact: only intended work changed; living docs remain unchanged by this turn and must be updated with implementation.

## Validation

- Rechecked SectionOwner attachment/index usage, atomic operation sites, mutation draining, and PhysicalSourceSpatialIndex.observe against current source.
- Reviewed plan consistency and relative Markdown links. Cost figures are structural estimates, not benchmark results.
- No Java implementation, compilation, or tests were run for this documentation-only update.

## Remaining

- Execute the ready plan, run relevant JUnit/GameTests, and measure startup, discovery, allocation, retained memory, and steady-state costs under equivalent valid source coverage.
