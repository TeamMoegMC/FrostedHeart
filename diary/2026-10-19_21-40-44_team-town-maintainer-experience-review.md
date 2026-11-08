# Team and town architecture: maintainer experience review

- Time: `2026-10-19 21:40:44 +08:00`
- Author: `Codex; OpenAI; primary architecture reviewer`
- Status: `completed`（subjective design assessment and documentation improvements; implementation pending）
- Scope: `Human readability, change locality, state encapsulation, onboarding and the first implementation slice`

## Completed

- Reviewed the proposed architecture from the perspective of a Java/Forge developer newly maintaining this system. This is an assistant's design judgment, not a measured study with human participants or a score for production runtime quality.
- Assessed the input specification at approximately **7.8/10**: eight equally weighted dimensions sum to 62, giving an unrounded mean of 7.75. The main strengths are ownership and separable rules; the main costs are reading/navigation, local-change overhead and rollout scope.
- Revised the [overall specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) in place instead of creating another competing architecture plan. Added task-specific reading routes and made each rule's owning section explicit.
- Added a clearly labeled proposed-work link to the [town documentation entry](../docs/town/README.md), preserving its distinction between implemented behavior and planned work.
- Specified a package-private TownRuntime state holder separate from TownRuntimeManager, and a small immutable AutoSettlementState value object to centralize the already agreed state transitions.
- Clarified that local Resident changes can use their owning object's named methods, while cross-object relationships use TeamTown/helpers. Retained direct array access inside the existing simulation subsystem.
- Defined the first readable implementation slice around the existing staffing-target edit path, with behavior and cold-read acceptance conditions.

## Assessment

| Dimension | Score / 10 | Reason for the score |
|---|---:|---|
| Responsibility and data ownership | 9 | Owners, thread boundaries and independent output consumers are explicit |
| Reading and navigation | 7 | The input master had 642 lines and persistence details 529; ordinary tasks lacked a short reading route |
| Local feature changes | 7 | The facade could become too broad, and callers could have to remember too many state details |
| Lifecycle and failure semantics | 8 | Contracts cover key paths, but their implementation still needs disciplined ownership and integration checks |
| Testability of the design | 9 | Pure rules and narrow persistence/transport boundaries support focused tests; actual integration tests remain pending |
| Debugging and observability | 8 | Phase, owner, version and recovery states are specified; real debugging examples have not been exercised |
| Performance fit | 8 | Existing SoA/main-thread design and bounded frozen I/O are appropriate; actual scale and hotspots remain unmeasured |
| Rollout cost for a mod | 6 | Multiple lifecycle, protocol and persistence changes still create a substantial implementation and review burden |

Scores describe the design reviewed at the start of this task. The resulting specification edits do not earn an automatic higher score. An 8.5–9 target requires a readable implemented slice, independent cold-reading and the relevant behavior/performance evidence.

## Decisions

- Reduce the concepts a normal caller must remember, rather than weakening initialization, persistence or recovery contracts to remove lines from a diagram.
- Keep TownRuntime internal to the town core; external callers use TeamTown or explicit manager operations. Keep manager orchestration separate from assignment, transport, simulation and encoding algorithms.
- Encapsulate pending/ready, replacement and completion transitions in AutoSettlementState while preserving existing NBT fields and all previously defined timing/merge rules. Keep window math in TownScheduleModel.
- Use the existing staffing edit as the first M2/M3 example after M1 foundations. Preserve its authoritative response, deferred staffing effect and save/reload behavior; later protocol changes still ship as their complete planned stage.
- Documentation impact: changed the target plan and added a navigation link in docs/town/README.md. No implemented-behavior reference, production source, configuration or read-only design content changed.

## Validation

- Read the current TownStaffingEditRequestPacket path and the TeamTownData staffing getter/setters to anchor the example in real code.
- Checked score arithmetic and the input document line counts. Checked changed Markdown encoding, links, fences, final newlines and trailing whitespace before delivery.
- Did not rerun protocol models, Java builds or game tests: this revision changes organization and encapsulation guidance, not the agreed protocol, timing behavior or file layout. The proposed Java types have not been implemented.
- No independent human cold-read test or implementation timing measurement was performed; none is reported as passed.

## Remaining

- Implement the first staffing-target slice within M1–M3 and verify that a new maintainer can identify its owner, operation, output and test without studying queue internals or relying on chat history.
- Reassess readability and change scope from actual code before copying the pattern to the rest of the system.
- Continue the existing M0–M8 roadmap and measure optional optimizations only after the relevant baseline exists.
