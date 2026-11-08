# Team and town architecture engineering loop review

- Time: `2026-10-19 21:17:44 +08:00`
- Author: `Codex; OpenAI; primary architecture reviewer`
- Status: `completed`（engineering-contract review and specification repairs; implementation pending）
- Scope: `Initialization, settlement commit, runtime boundaries, notification recovery, synchronization baselines, removal, replacement, allocator lifecycle and shutdown`

## Completed

- Added the [loop review](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#15-工程接线与恢复细则完整回路复查补充), repaired the [overall specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) in place, and aligned the [persistence engineering specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则).
- Identified eleven engineering gaps or ambiguous handoffs. Added persisted pending initialization provenance, shared per-tick clock sampling, stable runtime candidates, settlement reentry handling, and bounded lifecycle application at operation boundaries.
- Specified prepare/commit/publish for history, townDay and automatic completion, with signal consumption at commit and separate post-commit notification recovery.
- Defined owned recovery flags, retry timing, current-instance validation and shutdown cancellation. Replacement rollback now ends at the business commit boundary.
- Added explicit full-capture results, per-connection baselines, PENDING/PRESENT/ABSENT envelopes and a merged client resynchronization request. Accounted for locally generated client-holder identity and all relevant lifecycle/packet entry points.
- Closed component removal through runtime/simulation detachment, explicit empty simulation persistence, client absence and non-creating queries. Generator removal retains economic state.
- Recorded the third unmanaged citizen container and its per-dimension allocator. First allocation and late restored-container admission must calibrate IDs before registration/spawn, including after periodic registry scans are removed.
- Added concrete prepare/invalidation and source-entry tables, plus implementation acceptance conditions for the previously implicit paths.

## Decisions

- Retain the existing overall architecture; add narrowly owned contracts rather than general transaction, messaging or scheduling frameworks.
- Keep persistence capture read-only and possible without an attached Level. Pending initialization must survive saving before the clock is ready.
- Treat encoded-null failure, true absence and a pending baseline as different states. Deliver the necessary protocol/client/server changes together during the designated stage.
- Preserve the original vanilla writer for unmanaged citizen data; it is distinct from the old global player/AI town writer being retired by the persistence migration.
- Documentation impact: revised plans and appended this diary; current behavior docs, production code, configuration, read-only design and companion-pack files were untouched.

## Validation

- Inspected actual recordDailySnapshot mutation ordering, complete-packet error handling, provider/UI creating getters, team events, generator assignment paths, network registration and UnmanagedCitizenData/allocator logic.
- Ran `python build/team-town-architecture-loop-model-20261019.py`: [model](../build/team-town-architecture-loop-model-20261019.py), [report](../build/team-town-architecture-loop-model-20261019.json). Constructed ten counterexample traces for insufficient prior interpretations and passed 1435 corrected-contract checks.
- Model scope is finite contracts only; it does not execute Java, Forge, real transport/codecs, disk persistence, arbitrary whole-day rollback or performance tests. Earlier models retain their separate limited scopes.
- Checked the changed documents for UTF-8, local links, fenced blocks, final newlines and trailing whitespace before delivery. Target production source paths have no changes from this task.

## Remaining

- Implement M0–M8 and the mapped persistence phases with the added initialization, commit-boundary, runtime-boundary, recovery, baseline, absence and allocator regressions.
- Verify real event ordering, packet recovery, stop/restart and multiplayer behavior; measure the specified workloads before optional performance changes.
- Do not interpret a ready architecture specification or passing contract model as completed production engineering.
