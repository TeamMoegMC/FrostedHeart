# Dormant thermal chunk storage

- Time: `2026-08-31 00:46:53 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `chunk-local Page temperature persistence, unloaded decay, source retention, admission restore, and crop/phase fallback lifecycle`

## Completed

- Added lazy `LevelChunk` dormant thermal NBT with bounded regular/exact-mixed/
  collapsed residual encoding, `1/16 C` quantization, capacity-weighted Brick
  means, half-life decay, and mixed-only O(1) fallback indexes.
- Captured coherent Page/query cuts before retirement, save, stop, reload, and
  worker replacement; restored Air components and material poles through the
  existing staged Brick migration without a second topology pass or residual
  clone.
- Added disk-only one-shot support for campfire/generator/radiator/fountain,
  consumed only after disk load. Normal query/admission ignores the bit, and
  unload captures Pages before removing physical-source target buckets.
- Added live -> last coherent publication -> dormant chunk -> natural query
  ordering, including passive crop/town queries before a runtime starts.
- Removed duplicate composition from the precomputed crop-temperature overload,
  made `CropGrowEvent.Pre` deny rather than immediately kill, and retained phase
  ownership while an existing Page handle is pending/stale.

## Decisions

- Persist no topology, arena identity, source history, material energy, partial
  phase energy, global map, unloaded solver, or network payload.
- Exact mixed storage is capped at 256 components; larger Pages retain one Brick
  mean. The half-life is restart-scoped COMMON config, default `1800` seconds.
- Routine live query/mutation/solver/source paths do not consult dormant storage.
  Save/load/retirement work is Page- or chunk-local and allocation-bounded.

## Validation

- Production, JUnit, and GameTest source compilation: passed on Java 17.
- Focused thermal JUnit: `99/99` passed.
- Forge GameTest: all `14/14` required tests passed.
- Full `build` recompiled production and ran 687 tests; the only failure was the
  unrelated `TeamTownActualSaveCodecProbeTest` missing its external save file.
- `git diff --check`: passed.

## Remaining

- Run controlled encoded-NBT size, quick-teleport/restart, crop/source JFR, and
  long heap workloads before assigning measured CPU/allocation percentages.
