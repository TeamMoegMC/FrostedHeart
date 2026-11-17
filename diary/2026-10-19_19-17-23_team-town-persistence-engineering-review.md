# Team and town persistence engineering review

- Time: `2026-10-19 19:17:23 +08:00`
- Author: `Codex; OpenAI; primary architecture reviewer and engineering author`
- Status: `completed`（source review and implementation specification; production implementation pending）
- Scope: `Team/town persistence plan, concrete integration points, save lifecycle, queue semantics, migration and validation handoff`

## Completed

- Added the [engineering specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则) and reconciled the [architecture plan](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则) with its final decisions.
- Verified additional integration issues: duplicate scenario type instances, external/internal team-ID lookup in owner transfer, TeamCommand set discarding decoded state, ObservableTownMap mutation timing and view bypasses, and duplicate manager cleanup ownership.
- Defined explicit main-thread and worker state, one autonomous drain on the shared executor, a per-drain completion Future, bounded queue accounting including the oversized-record case, and version-based confirmation after actual file replacement.
- Specified ordinary save capture at the existing HEAD path and forced/stop capture plus outcome merge at RETURN. The original HEAD event remains once per invocation so FHResearch.save still runs.
- Defined V1 records, migration source precedence, success-dependent metadata cleanup, and retained deletion markers preventing old global data from reappearing.
- Added E0-E8 implementation order, field/mutator coverage, planned test cases, real workload metrics, and independent-JVM commands that skip the existing GameTest world-deletion task on the read run.

## Decisions

- Keep baseline eager decoding and existing gameplay models. Cold-component lazy decoding is a measured E8 extension rather than a dependency of the first writer.
- Preserve module boundaries with one explicit team-record extension; TownSimData remains a pure town simulation object.
- Freeze changes before background work; never apply save-completion mutations on a worker or require the next server tick to complete a drain.
- Documentation impact: engineering details remain in plans; living system documentation is reserved for actual implementation changes. Existing diary history and read-only design are unchanged.

## Validation

- Read the actual team data APIs, component registrations and mutators, map observers, citizen restoration, native save/stop code, research save listener, and Gradle GameTest configuration.
- No production code or save contents were changed, and no performance/build pass is claimed for this design-only work.
- Local Markdown links, code fences, UTF-8 text, whitespace, proposed type labels, and the two plans' key decisions are checked before delivery.

## Remaining

- Implement E0-E7 and run the specified deterministic, Forge lifecycle, independent-JVM and real populated-world validation before claiming production completion. Enter E8 only with measured need.
