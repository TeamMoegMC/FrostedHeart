# Sparse Section-Component thermal plan replacement

- Time: `2026-08-24 02:13:18 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Replaced the current climate and temperature implementation plan with the frozen Sparse Section-Component finite-volume runtime proposal.
- Preserved the three-backend model, interest permissions, physical `P/H/C/G` contracts, passive-consumer rules, hard budgets, and staged prototype gates.
- Replaced the former `REDUCED_RC_ISLAND`, `IslandRuntime`, partition, and portal runtime with lazy `ThermalPage` storage, mixed-only `4³ ComponentBrick` geometry, sparse `4/8/16` air control volumes, implicit Air-Air exchange, explicit material and far-field boundaries, and independent radiation.

## Decisions

- The current plan file remains the single authoritative pre-implementation plan; the superseded RC-island architecture is not retained as a competing appendix.
- Complex topology rebuilds are bounded to a local `4³` brick, while the inactive world retains no thermal mesh state.
- Runtime implementation remains gated by Phase 0 baselines, reference tests, memory admission, shadow comparison, and the proposal's engineering stop criteria.

## Validation

- Confirmed the replacement plan contains all numbered sections `0` through `80`, implementation phases `0` through `12`, core test groups, metrics, prototype targets, and the final architecture definition.
- Ran Markdown and diff integrity checks; no runtime code or implemented behavior changed, so Java tests and living `docs/climate/` updates were not required.

## Remaining

- Execute Phase 0 measurements and build the pure Java geometry and finite-volume reference prototypes before changing production temperature paths.
