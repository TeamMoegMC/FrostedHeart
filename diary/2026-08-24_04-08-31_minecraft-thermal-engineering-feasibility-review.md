# Minecraft thermal engineering feasibility review

- Time: `2026-08-24 04:08:31 +08:00`
- Author: `Codex; OpenAI; main engineering agent, with minecraft_geometry_review and thermal_runtime_review (gpt-5.6-sol, ultra)`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Ran a three-agent Minecraft engineering review and added a `48`-row feasibility index: implementable/reasonable choices are marked `✓`, conditional prototypes are marked `✓ 有条件`, and rejected assumptions are marked `✗` with an authoritative replacement.
- Preserved the recovered three-backend architecture, interest permissions, sparse Page/Brick/Cell model, physical `P/H/C/G` contracts, source ledger, migration evidence, and appendices A-E.
- Corrected geometry raster semantics, coarse/fine face ownership, geometry volume-energy exchange, low-level section mutation/lifecycle gating, Create dynamic exclusion, source replay, delayed-time semantics, publication ABA handling, bounded dispatch, global memory admission, radiation discovery, and passive-query cost accounting.
- Reordered delivery around `PR 0a` mutation GameTests, `PR 0b` numeric/workload acceptance, synthetic geometry, Forge resolver census, solver/source prototypes, a pre-integration FarField holdout gate, coordinator/publication proof, and only then production Minecraft wiring.
- Added layered JUnit 5, Forge GameTest, JMH/JFR, and production-like shadow validation; expanded failure containment, metrics, Definition of Done, implementation comment diagrams, and worktree dependency lanes.

## Decisions

- The plan is ready only for Phase 0 and gated prototypes; Minecraft production integration is not approved until mutation/lifecycle, FarField, publication/mailbox, global memory, and workload gates pass.
- V1 uses Page-wide publication revision fallback and keeps every non-zero source Page active. Fine publication reuse and source sleeping remain V2 data questions rather than hidden V1 options.
- Scheduling delay never becomes a source-energy outlet. Retained source segments replay in order, cumulative emitted energy is checksum-only, and `TIME_DEGRADED` records skipped transport/phase time while applying all attributable source energy.
- Non-equilibrium state removal is a signed environment exchange. Source, phase, and stateful material ownership cannot be reclaimed as ordinary cache state.

## Validation

- Verified numbered sections `0..91` occur exactly once, phases are exactly `0` and `A..L`, appendices are exactly `A..E`, and the plan contains one H1.
- Verified all Markdown code fences are paired, there is no trailing whitespace, all required corrected-contract anchors are present, and removed V1 metrics/API paths are absent.
- Ran `git diff --check` for the plan; it passed with only Git's existing LF-to-CRLF working-copy notice.
- Java/Forge tests were not run because this change modifies the implementation plan and diary only; the plan now assigns those tests to the appropriate implementation gates.

## Remaining

- Execute Phase 0a and 0b. Do not begin production thermal runtime integration until the plan's explicit gates approve it.
