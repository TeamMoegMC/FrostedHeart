# Team and town domain, data and runtime architecture review

- Time: `2026-10-19 20:23:37 +08:00`
- Author: `Codex; OpenAI; primary architecture reviewer`
- Status: `completed`（source investigation and architecture proposal; production implementation pending）
- Scope: `Team/town domain boundaries, data authority, daily logic, scheduling, assignment costs, runtime lifecycle and network integration`

## Completed

- Added the [overall architecture review](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) and linked it from the [persistence architecture](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则), explicitly limiting the latter to a subsystem.
- Reviewed the current strengths: team components, Resident/CitizenSim separation, SoA simulation, pure assignment/care models, main-thread business operations, existing incremental synchronization and navigation snapshot boundaries.
- Verified that building rosters are authoritative for current player-town assignments; Resident positions are mirrors, and legacy staffing targets must be migrated before roster cleanup.
- Distinguished durable player decisions, settled daily services/history, saved simulation state and transient runtime indexes. Loading must not run another economic day.
- Identified automatic settlement's dependency on HashMap enumeration position and its missing automatic completion key. Preserved deliberate repeated manual `/town tick` advancement and world-day-limited refugee refresh.
- Checked daily production's lack of an equal-priority tie-break and the staffing planner's repeated workplace/resident scans. Recorded the O(W·R²) worst-case comparison bound and repeated housing sorting as analysis, not measured lag.
- Proposed three principal responsibility extractions: TownSettlement, TownSync and a per-server TownRuntimeManager, while keeping existing state/model/simulation types and reusing persistence E1/E2/E3–E7.
- Documented distinct activation conditions for economic work, visible citizens, network observers, offline heat publication and AI towns.

## Decisions

- Preserve current domain structure and gameplay order during mechanical extraction. Keep player intent, current assignments and derived indexes distinct.
- Keep gameplay state on the server main thread. Pure navigation and frozen persistence work may use background executors; a new economic thread is not required.
- Profile assignment, ordinary tick, synchronization and persistence separately. Only replace periodic structural scans after all lifecycle paths have explicit registration; continue activity/AOI refresh as players move.
- The new plan remains draft specifically for automatic-clock choice, skip/catch-up behavior and legacy automatic-key initialization. This does not block equivalent responsibility extraction or the established persistence work.
- Documentation impact: only plans and this diary entry changed. Current behavior docs were not rewritten to present the proposal as implemented. Production code, configuration and read-only design files were untouched by this task.

## Validation

- Read current team identity/transfer paths, town codecs and daily pipeline, assignment and housing implementations, simulation adoption/registry refresh, full-state packet capture and navigation installation.
- Checked all three changed documents as UTF-8, including 49 local links, fenced blocks, final newlines and trailing whitespace: no issues.
- No build, Minecraft execution, algorithm benchmark or new gameplay test was run for this documentation-only work. Existing 831 persistence-model checks do not validate the broader gameplay or runtime architecture.

## Remaining

- Implement and validate A0–A7 and the referenced persistence stages; mark each actual outcome in the plan and update owning system docs when behavior changes.
- Finalize automatic settlement policy before modifying its trigger; test legacy migration, manual advancement, time jumps, dimension changes and member online state under that policy.
- Measure representative towns before choosing candidate-index, housing-selection, structural-registry or view-packet optimizations.
