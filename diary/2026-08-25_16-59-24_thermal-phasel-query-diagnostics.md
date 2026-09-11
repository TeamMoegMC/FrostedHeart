# Thermal Phase L synthetic query diagnostics

- Time: `2026-08-25 16:59:24 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `synthetic published-air JMH/JFR/JOL diagnostics, Page hot-index allocation, Gradle evidence bundle, climate lifecycle documentation and thermal implementation plan`

## Completed

- Added `ThermalShadowQueryFixtures`, which combines the production `ThermalPage`, `ThermalCellArena`, `DimensionThermalRuntime`, and `QueryPublication` owners for shared-Page and distributed-Page query batches.
- Added `ThermalShadowQueryBenchmark` with `1/10/50/100` sequential receiver parameters and `ThermalShadowQueryRetainedSize` with matching isolated object-graph measurements.
- Added the `thermalPhaseLQueryDiagnostics` Gradle bundle. It writes forked JMH JSON/text, a diagnostic 100-receiver JFR, JOL retained-size JSON, and an environment/limitations manifest under `build/reports/thermal-phase-l/`.
- Used the first diagnostic run to identify boxed `long` section-key allocation in `MinecraftThermalInput.pages`, then replaced only that hot index with the existing fastutil `Long2ObjectOpenHashMap`.

## Decisions

- Receiver counts in this harness label sequential query batches; they are not represented as real `ServerPlayer` instances and cannot be reported as multiplayer TPS evidence.
- The benchmark mirrors the production Page lookup, coverage envelope, runtime publication read, post-read Page validation, and publication-age check without extracting a new production callback or query abstraction.
- Only the measured hot Page index was changed. Source reference counts, witnesses, and other low-frequency maps remain untouched.
- The manifest records the measurement environment and explicit limitations without requiring a path-sensitive source hash or copied build version.

## Validation

- `thermalPhaseLQueryDiagnostics` completed successfully on Java 17 and generated all seven expected artifacts.
- After the primitive Page index change, both layouts measured about `0.01 B/query`; the preliminary distributed layout had measured about `24 B/query` from boxed section keys.
- Final 100-query batches averaged about `4.9 us`; p95 was `5.0 us`, with p99 `12.69 us` for one shared Page and `13.30 us` for 100 distributed Pages.
- JOL isolated graphs measured `11,264 B` for 100 receivers sharing one Page and `313,472 B` for 100 distributed Pages/Cells.
- Thermal JUnit passed `238/238`; Forge GameTest passed `19/19` required; `git diff --check` reported no whitespace errors.

## Remaining

- Capture real `1/10/50/100` player production-like server workloads with TPS, main-thread and worker percentiles, retained heap, allocation/GC, fallback/publication age, queue age, world reads, chunk loads, and energy ledgers.
- Exercise the shared/isolated base, exploration, dynamic-base, stable/changing-source, crop, dense-radiation, and multi-dimension workloads before Phase L acceptance or gameplay-authority migration.
