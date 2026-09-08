# Dynamic-shape Brick recovery design

- Time: `2026-09-08 20:15:13 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `source-grounded plan for generator topology and unsupported block geometry`

## Completed

- Wrote the [implementation plan](../plans/2026-09-08_20-15-13_thermal-dynamic-shape-static-fallback.md), including resolution policy, exact edit boundaries, lifecycle, cost model, and real Forge acceptance cases.
- Verified T1/T2 shape functions are constant full cubes; these generators need neither position/facing geometry caches nor arbitrary runtime shape evaluation.
- Traced frozen signature publication, loaded-only capture, signature-based mutation filtering, and neutral-material handling. Confirmed profile-zero signatures can share geometry without inheriting material contacts.

## Decisions

- Use startup-compiled exact generator geometry plus a shared material-neutral solid fallback for unsupported registered states. Preserve unavailable world input as unresolved.
- Keep current integer lookup, immutable worker inputs, and event-driven topology updates. General dynamic geometry is deferred until a concrete accurate-compatibility requirement warrants its complexity.
- Treat fallback insulation/closed-opening errors and recovered physical solver costs explicitly; do not claim universal accuracy, zero total runtime cost, or measured optimal performance.

## Validation

- Source inspection only; no Java changed and no tests or benchmarks executed.
- Documentation impact: new plan and this diary entry. Living system documents remain unchanged because behavior is unchanged.

## Remaining

- Implement and run the plan's real T1/T2, unsupported-block, lifecycle, material, alignment, and performance checks. No JUnit is planned.
