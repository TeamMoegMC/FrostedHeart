# Receiver-lazy radiation lock closure

- Time: `2026-09-01 05:08:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `BlockRadiationIndex producer locking, physical DDA section reuse, and performance contracts`

## Completed

- Reduced stable coverage to one main-thread `coveredBySection` lookup per
  section and removed producer-lock acquisition from pending/dirty reads.
- Moved emitter comparison, target growth, allocation, and copy outside the
  cross-thread dirty producer lock; only known-mask reservation/rollback and
  structural coverage publication/removal remain synchronized.
- Reused the `SectionOwner.section()` already resolved by physical witness entry,
  eliminating the second `getChunkNow` per crossed physical-ray section.
- Corrected plan language for current-section owner reuse, the independent
  main-thread radiation limit, and the exact producer-lock boundary.

## Decisions

- Kept the existing primitive double dirty buffers and one coverage map. No new
  index, sidecar, lock, cache, or compatibility path was added.

## Validation

- Per user direction, no Gradle task or automated test was run. Static symbol,
  call-site, field-use, and diff checks were used.

## Remaining

- User-owned build and in-game validation, followed by controlled profiling.
