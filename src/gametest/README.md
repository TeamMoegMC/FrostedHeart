# Forge GameTest sources

This directory contains reusable Forge GameTest cases, test-only observers, catalogue fixtures and resources. They use the `gameTest` source set and are excluded from the ordinary production JAR.

Production source and data under `src/main/` and `src/generated/` define implemented behaviour. These tests describe the checks they perform; successful cases do not prove untested loads.

Start with the relevant system in [docs/README.md](../../docs/README.md). The `gameTest` source set and `runGameTestServer` task are configured in [build.gradle](../../build.gradle); temperature behaviour and configuration are described in [the thermal runtime contract](../../docs/climate/thermal-runtime-control.md).

Thermal tests retain input/source ownership, query and save lifecycle, quality recovery, gameplay integration, and current two-base/scattered/overload fixtures. One-off recording clients, retired capacity variants and duplicate entry wrappers were removed; their history is recorded in [the thermal consolidation](../../diary/2026-10-29_23-38-00_thermal-development-history.md#retired-validation-tools). Other systems' test code and fixture data are separate from that cleanup.

Use Java 17 and compile the test source set with `./gradlew compileGameTestJava` (`.\gradlew.bat compileGameTestJava` on Windows). For example, select the compact-storage checks with `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_compact_storage`. Runtime worlds, logs and profiling recordings are generated outputs and stay outside Git; test source and deliberate fixtures stay versioned.

For town staffing and housing queues, use `-I Scripts/town-scheduling-validation.init.gradle` and namespace `frostedheart_town_scheduling`. The same init file provides `runTownSchedulingClient`, which creates a disposable world and checks the rendered Mayor's Seal with actual management packets and mouse events; its JSON result must pass for the task to succeed. The [town synchronization reference](../../docs/town/persistence-and-runtime.md#验证入口) owns the scenarios, evidence and controlled temperature.

That client entry also calls `TownEditorClientChecks` for controlled component reply sequences. The returned `editorReplyChecks` list records completed checks; earlier result files without that field do not cover them. These added checks are prepared but unrun as of 2026-11-09, following the user's instruction not to build this change.

Player heating equipment checks use namespace `frostedheart_equipment_heating`:
real item capabilities, sustained body curves, stacked devices, fuel cadence and
reload, vest demand, reservoir energy conservation and the actual player tick.
Their gameplay contract and validation scope belong to
[player temperature](../../docs/climate/player-temperature.md).

For the shared thermal comparison baseline, run [`scripts/run-thermal-baseline.ps1`](../../scripts/run-thermal-baseline.ps1). The [baseline reference](../../docs/climate/thermal-validation-baseline.md) owns its scene list, recorded configuration, clocks and result interpretation.

For infrared transport and actual GL display, run [`Scripts/run-infrared-network-e2e.py`](../../Scripts/run-infrared-network-e2e.py) with Java 17. It starts an isolated dedicated server, two rendering clients and an ordered TCP delay/rate proxy; the [infrared reference](../../docs/climate/infrared-view.md) owns its scenarios, invocation and verification limits.

The memory-lifecycle regressions use namespaces `frostedheart_preparation_retention`,
`frostedheart_geometry_input_order`, `frostedheart_checkpoint_boundary`, and
`frostedheart_persistence_overload`; `frostedheart_concurrent_save` covers accepted
and unaccepted input during a running solve. Their separate GameTest batches permit a
combined run without closing another test's live thermal runtime. They check
collection of completed builders before closing a heated world, actual adjacent
Page publication order when fair selection first visits the later input, incoming
geometry holds during unload-job cleanup, connecting Pages that are announced but
not yet installed, and a real flushed chunk save with every
thermal worker paused. The last test also reads disk NBT to check pending heat and
the changed block's material identity.

These focused checks complement the ordinary dedicated-server source/chunk
workload in [`Scripts/run-thermal-oom-repro.py`](../../Scripts/run-thermal-oom-repro.py).
The [memory evidence reference](../../docs/climate/thermal-server-memory-evidence.md)
records the confirmed reference defect, heap analysis, configuration differences,
and limits on attributing the original remote OOM.
