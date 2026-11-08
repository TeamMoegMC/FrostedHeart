# Team and town persistence loop closure review

- Time: `2026-10-19 19:37:21 +08:00`
- Author: `Codex; OpenAI; primary design reviewer`
- Status: `completed`（design repair and finite model checks; production implementation pending）
- Scope: `Record participation, captured completion facts, replacement/deletion ordering, post-commit phases, consumer rebinding and stop failure exits`

## Completed

- Added the [loop review](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#161-复查后收紧的决定) and repaired the [engineering specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则).
- Defined a shared record-source registry covering team, AI town, metadata and retired records, with read-only simulation capture and explicit component removal.
- Verified that client and server reuse TeamDataHolder; specified explicit per-instance server persistence binding so integrated-server client copies never enter the disk pipeline.
- Added immutable CommitFacts to snapshots/results and separated version acknowledgement from post-commit effects at batch boundaries.
- Closed the two-record team replacement gap using durable superseded-holder relationships and retirement only after confirmed replacement, with ordered metadata follow-up.
- Verified actual retained consumers in GeneratorContainer and WarehouseLevelEmitterBlockEntity; specified menu closure, watcher rebinding, simulation takeover and restoration on failed replacement.
- Added abortDrain and a stopping-preparation snapshot so driver failure/native save exceptions have an explicit completion/report path.

## Decisions

- Keep one logical save task, existing NBT, main-thread gameplay, and native save cadence. The new replacement field describes final identity relationships, not an operation log.
- Use three finite strong-save phases rather than repeatedly recapturing ALWAYS_CAPTURE business data.
- Distinguish a terminal failed request from successfully persisted state; keep old formal files and report unsaved versions on permanent I/O failure.
- Documentation impact: only plans and this new diary entry describe the proposed changes; current behavior docs and production code remain unchanged.

## Validation

- Read current transfer, creation, lazy simulation lookup, menu binding, warehouse subscription, and server lifecycle code.
- Ran the [finite model](../build/team-town-persistence-loop-model-20261019.py): reproduced three prior-protocol counterexamples and passed 831 corrected-model checks, including a second transfer before the first transfer has been persisted; [JSON results](../build/team-town-persistence-loop-model-20261019.json).
- The model performs no Minecraft execution or save-file I/O and provides no Java concurrency or performance certification.
- Markdown links, fences, UTF-8, whitespace and agreement between the plan documents are checked before delivery.

## Remaining

- Implement E0-E7 and run the added F-group lifecycle, queue, transfer and consumer-rebinding regressions. Keep runtime correctness claims separate from this design review.
