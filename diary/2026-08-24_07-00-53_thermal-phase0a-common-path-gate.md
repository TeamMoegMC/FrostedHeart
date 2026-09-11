# Thermal Phase 0a Common-Path Gate

- Time: `2026-08-24 07:00:53 +08:00`
- Author: `Codex (OpenAI; main engineering agent)`; review by `phase0_enabled_mod_census (gpt-5.6-sol, ultra)`
- Status: `completed`
- Scope: `content.climate.thermal.phase0.mutation`, the Phase 0 architecture plan, and climate lifecycle documentation

## Completed

- Finished the GameTest-only five-argument `LevelChunkSection#setBlockState` probe, loaded-section owner mapping, lifecycle generation, tick coalescing, publication rejection, and sticky full-resync path for the frozen common mutation routes.
- Covered Vanilla/Forge setters, fluid and waterlogged changes, doors, recursive Sponge writes, moving pistons, real ticket load/unload/reload, and real Create assemble/disassemble. Create contraptions remain air while moving and emit no movement geometry delta.
- Added distinct raw block/biome callbacks for `FastNoiseEngine` when a section already has a loaded owner. Ordinary unmapped worldgen remains zero thermal work.
- Added `DebugCommand restore_backup` whole-section owner rebinding, replacement-identity unload cleanup, and a mandatory full resnapshot before publication.
- Replaced generation-only resync acknowledgement with `ResyncToken`, bound to section identity, lifecycle generation, required revision, and reason. The regression test proves an old R1 rebuild cannot clear a newer R2 requirement.
- Reduced the executable writer census to representative Vanilla/Forge/Create/Frosted Heart paths. The prior 21-runtime exhaustive investigation and `/resetchunks` gate assertions remain in block comments instead of being deleted.
- Corrected the census so FastNoise raw block writes route to `RAW_BLOCK_DIAGNOSTIC_FALLBACK`, matching the explicit implementation callback rather than the debug fingerprint scanner.

## Decisions

- Phase 0a completeness is scoped to common paths. Unknown third-party bypasses receive a dedicated adapter after a reproducible player report; exhaustive enabled-mod enumeration is not a gate.
- Fingerprints are GameTest/manual-debug diagnostics only. Production does not periodically scan active sections.
- `/resetchunks` is deferred and does not block Phase 0a. If support is added later, it must discard the refreshed chunk's old thermal Page and lazily rebuild from the new chunk snapshot; old thermal state is not preserved.
- `PhaseZeroThermalRouting` still keeps the legacy implementation as the only gameplay authority. Completing Phase 0a does not enable the V1 runtime.

## Validation

- Java 17 `test --tests "com.teammoeg.frostedheart.content.climate.thermal.*"`: `60/60` passed, no failures or skips.
- Java 17 `runGameTestServer`: `9/9` required passed; `8/8` are Phase 0a tests.
- `compileJava` passed as part of both Gradle runs.
- Independent ultra review found no remaining reproducible P0/P1/P2 blocker inside the frozen common-path scope after the R1/R2 ACK and FastNoise route fixes.
- `git diff --check` passed apart from repository line-ending conversion notices.

## Documentation

- Updated `plans/2026-08-22_16-47-30_probe-compiled-sparse-conservative-thermal-graph.md` to mark Phase 0a complete under the frozen common-path scope and remove exhaustive census, periodic fingerprint, and `/resetchunks` from its gate.
- Updated `docs/climate/data-lifecycle-and-integration.md` with the implemented lifecycle/recovery contracts and current test counts.

## Remaining

- Phase 0b still needs fixed-environment legacy baselines, multiplayer JFR/JMH, retained-memory evidence, real workload thresholds, and the four-candidate comparison. No CPU-time or memory budget is claimed by this Phase 0a work.
- Phase A still needs the real Forge shape resolver census, Minecraft shape adapters, fixture matrix, and measured closure/allocation costs.
- `/resetchunks` operation-level invalidation is implemented only if that administrative compatibility is explicitly brought back into scope.
