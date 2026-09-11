# Thermal Phase L shadow runtime observability

- Time: `2026-08-25 16:38:11 +08:00`
- Author: `Codex; primary implementation agent`
- Status: `completed`
- Scope: `generation-safe runtime diagnostics, Phase K consumer aggregation, query fallback/timing counters, Minecraft shadow snapshot, living climate documentation and thermal implementation plan`

## Completed

- Added an on-demand `MinecraftThermalInput.ShadowRuntimeSnapshot` that combines the existing player, registered-machine, crop, and town shadow comparisons with Page, mixed-Brick, source, radiation, publication-age, fallback, timing, coordinator, topology, and solve state.
- Added `DimensionThermalRuntime.Diagnostics` as the single runtime snapshot owner. It reports stable lifecycle/publication state and marks `writerBusy=true` instead of racing mutable arena or sweep counters while the logical writer is active.
- Retained the actual `DimensionThermalRuntime.RunReport` in the existing shadow dispatch report, and added saturating query/fallback/timing counters without introducing a parallel metrics runtime or another query path.
- Synchronized `QueryPublication` capacity and reserved-byte access with resize so diagnostics cannot observe the backing-buffer transition inconsistently.
- Corrected the Phase K boundary: HUD remains downstream of `TemperatureUpdate -> PlayerTemperatureData -> FHBodyDataSyncPacket` and is not a fifth thermal consumer.

## Decisions

- Shadow observability must be passive and bounded. It cannot wait for the worker, read mutable SoA state without ownership, load chunks, admit Pages, send a client packet, alter HUD cadence, or change gameplay authority.
- Writer-busy snapshots return `-1` for mutable arena/sweep counters. This keeps the ownership contract explicit instead of presenting a racy number as exact.
- Raw nanosecond accumulators are diagnostic inputs only. They do not replace production-like JFR/JMH, retained-size measurements, or the `1/10/50/100` player Phase L workload gate.

## Validation

- Thermal JUnit: `238/238` passed, including the writer-owned diagnostics boundary.
- Forge GameTest: `19/19` required passed, including the combined runtime snapshot assertions in the existing Minecraft thermal input scenario.
- Full repository JUnit executed `812` tests: `811` passed; the only failure was the unrelated `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`, whose external save fixture was absent.
- `git diff --check` reported no whitespace errors; the repository's existing LF-to-CRLF warnings remain.

## Remaining

- Capture production-like Phase L JFR/JMH and retained-size evidence for `1/10/50/100` players, then evaluate the frozen CPU and memory gates without switching gameplay authority.
- Complete Phase 0b production-like evidence and approve applicable FarField profiles before considering the gameplay query compositor or authority migration.
