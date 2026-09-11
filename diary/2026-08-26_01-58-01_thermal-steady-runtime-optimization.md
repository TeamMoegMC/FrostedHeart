# Thermal steady runtime optimization

- Time: `2026-08-26 01:58:01 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `thermal gameplay scheduling, topology rebuild state, query publication, Phase 0 packaging, and material classification`

## Completed

- Coalesced stable gameplay solves to a five-tick cadence while Page, chunk, geometry, FarField, and physical-source changes request an urgent solve.
- Stopped sealing and dispatching equilibrium dimensions until a new input event; unresolved topology remains a degraded query state but no longer prevents sleep.
- Made `desiredSignatureIds[4096]` dirty-only and released it after commit, leaving stable Pages with only the applied signature cut.
- Started `QueryPublication` at 256 slots and grew it against the arena high-water mark before publication instead of reserving 65,536 slots per dimension.
- Split the production section mutation Mixin from the Phase 0 probe. Production/deobf JARs exclude Phase 0 reference, census, events, and GameTest classes; the probe Mixin is disabled unless the GameTest property is set.
- Restored the established `FHTags.Blocks.SLUDGE` name and resource ID, and rejected air before sound-based gameplay material classification.

## Decisions

- Source event ticks remain authoritative for exact `P*dt`; solve coalescing changes transport cadence, not injected energy.
- Stable frames reuse the installed `ThermalSweep`; `ThermalCellArena`, source timeline, radiation, and FarField scratch were not rewritten.
- The async boundary remains the existing serial `Executor` interface; gameplay continues to use main-thread `Runnable::run` until production workload evidence justifies a scheduler change.

## Validation

- Java 17 offline Gradle `compileJava` passed.
- Offline targeted JUnit passed `29/29`: `SolveEpochContractTest`, `DimensionThermalRuntimeTest`, `MinecraftThermalInputTest`, and `MinecraftThermalTopologyApplierTest`.
- Offline `jar deobfJar` passed. Both artifacts contain `MinecraftThermalEvents` and `LevelChunkSectionMixin_ThermalInput`; configured verification-entry matches were `0` in each artifact.
- `git diff --check` passed with existing line-ending warnings only.

## Remaining

- Production-like `1/10/50/100` player server workloads, solve CPU, retained heap, and allocation/GC measurements remain the next performance gate.
