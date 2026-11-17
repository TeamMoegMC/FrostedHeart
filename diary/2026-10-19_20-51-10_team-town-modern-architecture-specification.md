# Team and town modern architecture specification

- Time: `2026-10-19 20:51:10 +08:00`
- Author: `Codex; OpenAI; primary architecture designer`
- Status: `completed`（architecture specification and finite design checks; production implementation pending）
- Scope: `Team/town data authority, business APIs, daily settlement, scheduling, runtime lifecycle, synchronization, persistence integration and maintainability`

## Completed

- Created the [implementation specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) as the single overall entry point, with concrete class responsibilities, dependency boundaries, code layout, lifecycle order, defaults and M0–M8 acceptance steps.
- Marked the [previous overall review](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md) superseded while preserving its source investigation. Updated the persistence overview and [engineering specification](../plans/2026-10-19_20-42-32_team-town-modern-architecture-specification.md#16-持久化具体实现与统一测试细则) to point to the new overall entry and retain responsibility for their existing file/queue protocols.
- Resolved automatic timing policy: primary-world existing climate clock, stable holder-ID phase, latest observed opportunity only, no offline replay, separate manual advancement, explicit new-town/legacy/current-load initialization and a small per-town persisted deduplication state.
- Defined transient skip tracking so offline skipped opportunities alone do not dirty an entire town; persisted attempted and completed facts remain distinct.
- Specified the daily business completion point before external notifications. Notification failures request repair/resynchronization without rerunning committed economics; pre-completion failures retain their actual state and suppress same-window automatic retries.
- Added read-purity, semantic-mutation, ownership and extension rules, plus an explicit event sequence and a minimal approach to transport and assignment responsibility extraction.
- Retained existing component storage, SoA simulation, pure models, resource actions and packet formats, with measured algorithm/index/view optimizations as later conditional work.

## Decisions

- Keep one authority for economic data and the existing authority for simulation data. Main-thread business writes and frozen background inputs remain the baseline.
- Preserve building-roster authority and legacy staffing migration order. Do not confuse warehouse-interface disabled admission with rejection of a new P2P binding.
- Reuse WorldClimate clock values and WorldClockSource elapsed-time semantics instead of introducing another durable global clock or schedule metadata file.
- Keep packet/client and persistence state consumers independent. Fold server-end visibility ordering into one explicit town event chain during the designated migration stage, not via implicit subscriber order.
- Maintainability is enforced through named responsibilities, side-effect-free reads, explicit lifecycle ownership, concrete result semantics, small contextual records and behavior-focused acceptance tests.
- Documentation impact: this work changes plans and adds this diary only; no target behavior was presented as already implemented in living docs. Production code, configuration, read-only design and companion-pack files were untouched.

## Validation

- Rechecked current WorldClockSource, WorldClimate getters, climate/citizen event ordering, town mutation and sync paths, manual advancement and the existing persistence engineering contract.
- Ran `python build/team-town-schedule-model-20261019.py`: [model](../build/team-town-schedule-model-20261019.py), [result](../build/team-town-schedule-model-20261019.json); 3399 checks passed across 20 phases, including clock-tail interpolation, negative windows, old-save migration, offline/config/level skips, restart, replacement, manual advancement and failure/notification semantics.
- The model does not execute production Java, Forge events, codecs, disk persistence, arbitrary whole-day rollback, gameplay balance or performance. It does not replace implementation tests or broaden the earlier persistence-model claims.
- Checked five changed Markdown documents for UTF-8, local links, fenced blocks, final newlines and trailing whitespace before delivery. No build or Minecraft test run was required for this design-only change.

## Remaining

- Implement M0–M8 with the mapped persistence stages, updating phase outcomes and owning system docs as behavior becomes real.
- Verify the specified completion point, notification recovery and Server END ordering against actual Java/Forge behavior; perform representative multiplayer/restart/performance acceptance.
- Use measured results to decide optional candidate-index, housing-selection, registry-scan, view-packet and worker-limit changes; do not assume the architecture document itself delivers a speedup.
