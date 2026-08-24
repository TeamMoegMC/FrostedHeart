# Thermal Phase 0 contract and mutation spike

- Time: `2026-08-24 04:50:44 +08:00`
- Author: `Codex; OpenAI; main engineering agent, with phase0a_mutation_spike (gpt-5.6-sol, ultra)`
- Status: `partial`
- Scope: `content.climate.thermal.phase0`, Phase 0 Forge GameTest support, and the sparse thermal runtime plan

## Completed

- Added pure-Java reference contracts for game time and SI thermal units, `H=C(T-Tref)`, exact constant/piecewise `integral(P dt)`, analytic pair/fixed-boundary exchange, pure-LOD migration, geometry ingress/egress/residual accounting, and typical/stress workload evaluation.
- Added `14` workload descriptors and a routing contract that keeps legacy as the only gameplay authority while V1 remains shadow-only.
- Added a GameTest-only five-argument `LevelChunkSection#setBlockState` probe with loaded-section identity ownership, lifecycle generation, revision/publication rejection, tick coalescing/watermarks, unmapped/off-thread handling, raw-palette fingerprint recovery, and section-indexed moving-geometry exclusion.
- Covered direct and ordinary block writes, water/waterlogged changes, Door/Trapdoor/FenceGate, recursive Sponge changes, moving piston transitions, lifecycle recovery, and dynamic exclusion re-admission. Dynamic invalidation also includes the actually indexed old sections when a caller omits `oldBounds`.
- Enabled the `frostedheart` GameTest namespace and made Architectury available to the dedicated dev runtime required by FTB/Item Filters.
- Updated the plan to `in-progress` and documented that Phase 0 does not change production temperature behavior.

## Decisions

- Phase 0 remains partial: executable numerical contracts and the current mutation spike are evidence, but synthetic exclusion is not evidence for real Create integration and lifecycle-core tests are not evidence for chunk-manager unload/reload.
- The mutation Mixin is enabled only by the GameTest JVM property. Production legacy temperature paths, persistence, configuration, packets, and gameplay remain unchanged.
- Workload descriptors and acceptance logic do not substitute for measured JFR/JMH, retained heap, or workload-specific thresholds.

## Validation

- Java 17 `test --tests "com.teammoeg.frostedheart.content.climate.thermal.phase0.reference.*"`: `22/22` JUnit tests passed; `compileJava` succeeded.
- Java 17 default `runGameTestServer`: `6/6` required tests passed, including `5` Phase 0a GameTests, with Architectury, FTB, Item Filters, and Create loaded.
- The same GameTest suite also passed in the reduced `-PwithoutFtb -PwithoutJei` dev runtime.
- `git diff --check` passed; only existing LF-to-CRLF working-copy notices were reported.

## Remaining

- Add real Create assemble/move/disassemble and chunk-manager unload/reload GameTests.
- Census target-mod mutation writers and add adapters or bounded detection for any path not covered by the low-level hook.
- Measure periodic fingerprint coverage/cost and complete fixed-environment legacy/competitor workloads with JFR/JMH, retained heap, multiplayer data, and explicit thresholds.
- Do not begin production Minecraft integration until the remaining Phase 0 and later FarField/publication/memory gates pass.
