# Minecraft-first thermal plan corrections

- Time: `2026-08-24 03:18:41 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md`

## Completed

- Integrated the latest Minecraft-first review into the existing temperature plan without replacing its `0..91` V1 structure, three backends, recovered architecture evidence, migration contracts, phases, or appendices.
- Replaced the fixed four-region/256-atom contract with benchmark-derived finite `Rmax` and `64 * Rmax` primitive atom spans; added `BlockState + FluidState` resolved geometry, bounded contextual resolver dependency masks, read-closure accounting, separate airflow/contact/radiation channels, and explicit dynamic-geometry exclusions.
- Restored Door/Trapdoor/FenceGate to normal geometry mutation and Brick rebuild while limiting `GateOperator` to non-topological adjustable conductance; restricted stateless thin-wall bridging to one confirmed barrier block.
- Added tick-end mutation/source ordering, sealed input watermarks, one logical in-flight epoch per dimension on a bounded shared executor, `maxSolveDeltaTicks`, explicit `TIME_DEGRADED`, compiler-footprint publication reuse with Page fallback, passive `PLAYER_BODY`, and profile-owned `TransitionMutationPolicy`.
- Added queue-specific sticky recovery. Source resync now carries revision, watermark, cumulative emitted energy, current power/ports/binding, and retained binding segments; unrecoverable spatial attribution is recorded as `SOURCE_RESYNC_LOSS`.
- Kept source-bearing Pages active in the baseline prototype and specified `STEADY_SOURCE_SLEEP` only as a benchmark-gated optimization with a fixed-point certificate, revision wake, periodic revalidation, and steady-loss ledger.

## Decisions

- Minecraft BlockState/FluidState topology remains the V1 geometry authority; registered contextual resolution is bounded and main-thread snapshot-backed, while workers still receive only resolved primitive IDs.
- Page-wide geometry revision fallback remains the correctness baseline. Fine-grained publication reuse is optional and requires complete compiler-emitted support footprints, including adjacent interfaces.
- Delayed asynchronous work may skip cadence but may not pass an unvalidated arbitrary `dt` to thermal kernels; intervals beyond the measured bound use an observable degraded branch without epoch backlog or hidden energy debt.
- Queue capacity is transport capacity, not state authority. Page/source/reservoir-owned sticky state must make overflow recoverable and observable.

## Validation

- Verified numbered sections `0..91` occur exactly once, phases are exactly `0` and `A..L`, appendices are exactly `A..E`, and the file contains one H1.
- Verified all `800` Markdown code fences are paired, no trailing whitespace exists, and stale fixed-region, 256-atom, Door-as-Gate, per-dimension-thread, arbitrary-latest-`dt`, and source-no-sleep wording is absent.
- Ran `git diff --check` for the plan successfully; only Git's existing LF-to-CRLF working-copy notice was emitted.
- Documentation-only architecture work; Java tests and living `docs/climate/` updates were not required.

## Remaining

- Execute Phase 0 and PR 0-4 to measure `Rmax`, resolver closures, `maxSolveDeltaTicks`, source resync history capacity, publication-footprint value, shared-executor sizing, and whether steady-source sleep is justified before Minecraft gameplay integration.
