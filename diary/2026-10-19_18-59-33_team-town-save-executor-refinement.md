# Team and town save executor refinement

- Time: `2026-10-19 18:59:33 +08:00`
- Author: `Codex; OpenAI; primary architecture analysis`
- Status: `completed`（design refinement; implementation pending）
- Scope: `Proposed persistence queue concurrency, Minecraft executor reuse, save completion lifecycle`

## Completed

- Refined the [architecture plan](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则) to distinguish one concurrent save task from one dedicated physical thread.
- Verified local Minecraft 1.20.1 source: `Util.ioPool()` is a cached executor; vanilla `IOWorker` uses `ProcessorMailbox` on it for ordered work.
- Specified admission before executor submission, world-scoped task draining, and shared executor ownership.

## Decisions

- Start with one concurrent background save task on the existing I/O executor. Keep the bounded, coalescing record queue in the save coordinator.
- Do not close Minecraft's shared executor when closing a world. A reused pool may still create a physical thread when needed.
- Compare concurrency 1 and 2 only when populated-world measurements show compression CPU and queue delay justify it. Preserve ordering per record and measure competition with native chunk I/O and other mod work.
- Documentation impact: only the proposed architecture changed; current system documentation remains accurate to the unmodified implementation.

## Validation

- Read `Util` and `IOWorker` from the existing Forge 1.20.1 source artifact and checked existing Chorda client use of `Util.ioPool()`.
- Documentation-only refinement; no production code, configuration, or save contents changed. No new throughput benchmark was run.

## Remaining

- Implement and measure the proposed persistence pipeline before claiming a sufficient concurrency level for large towns.
