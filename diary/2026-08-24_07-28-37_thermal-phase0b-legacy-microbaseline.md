# Thermal Phase 0b Legacy Microbaseline

- Time: `2026-08-24 07:28:37 +08:00`
- Author: `Codex; primary implementation and validation agent`
- Status: `partial`
- Scope: `build.gradle`, `src/jmh/java/com/teammoeg/frostedheart/content/climate/thermal/benchmark`, Phase 0b plan and climate lifecycle documentation

## Completed

- Added a Java 17 JMH source set and repeatable `thermalLegacyJmh`, `thermalLegacyJfr`, `thermalLegacyRetainedHeap`, `thermalBenchmarkEnvironmentManifest`, and aggregate `thermalLegacyBaseline` tasks.
- Measured the real legacy `ChunkHeatData.queryAdjust` empty path and 1/10/100-adjuster hit/miss paths with one fork, three one-second warmups, five one-second measurements, and the JMH GC profiler.
- Captured an isolated JOL retained-object-graph estimate for 0/1/10/100 adjusters and a 15-second diagnostic JFR for populated queries.
- Added reference pair/boundary and synthetic `4^3` Brick compile calibration benchmarks. These are harness calibration only, not V1 candidate evidence.
- Updated the active thermal plan and `docs/climate/data-lifecycle-and-integration.md`. No player-visible behavior, persistence, configuration, networking, or gameplay authority changed.

## Decisions

- Keep Phase 0b `partial`. This run is a local legacy microbaseline; it does not represent a production modpack server or any of the four proposed candidates.
- Run JMH on the Forge runtime classpath. A flattened fat JAR changed Log4j/Forge class initialization and silently omitted failed legacy results, so the replacement task uses `fail-on-error` plus an explicit required-result check.
- Bootstrap `SharedConstants` and Vanilla registries before fixture setup. Bootstrap and DataFixer work is outside measured benchmark operations.
- Report JMH allocation as effectively `0 B/op`: the tiny sub-millibyte normalized values are profiler sampling noise, and immediate field consumption allows HotSpot to scalar-replace `HeatQueryResult`.

## Validation

- Java 17 `compileJmhJava test --tests '*thermal*'`: passed.
- Java 17 `thermalLegacyJmh`: `11` result rows completed. `queryAdjust` hit was `3.177/21.134/198.132 ns/op` for 1/10/100 adjusters; miss was `2.684/13.948/135.606 ns/op`; all were effectively `0 B/op` with zero measurement GC.
- Java 17 `thermalLegacyRetainedHeap`: 0/1/10/100-adjuster object graphs were `72/240/1032/9912 B`.
- Java 17 `thermalLegacyJfr`: passed; generated a `1,317,659 B`, 15-second recording with `737` execution samples.
- Raw local artifacts: `build/reports/thermal-phase0b/environment.json`, `legacy-jmh.json`, `legacy-jmh.txt`, `legacy-query-diagnostic.jfr`, `legacy-query-jfr-jmh.json`, and `legacy-retained-heap.json`.

## Remaining

- Benchmark `SurroundingTemperatureSimulator` with main-thread snapshot construction separated from worker simulation and report p50/p95/p99.
- Run production-mod-list 1/10/50/100-player, crop, town, forced-random-tick, network, and multi-dimension scenarios with whole-server retained heap and GC evidence.
- Implement executable `CachedAnalyticSurface`, `SparseThermalRuntimeV1`, `PerBlockSparseGraph`, and `ReferenceFiniteVolume` candidates before any comparative claim.
- Freeze workload-specific thresholds only after those production-like runs; do not enable V1 gameplay authority before the remaining gates pass.
