# Forge GameTest sources

This directory contains reusable Forge GameTest cases, test-only observers, catalogue fixtures and resources. They use the `gameTest` source set and are excluded from the ordinary production JAR.

Production source and data under `src/main/` and `src/generated/` define implemented behaviour. These tests describe the checks they perform; successful cases do not prove untested loads.

Start with the relevant system in [docs/README.md](../../docs/README.md). The `gameTest` source set and `runGameTestServer` task are configured in [build.gradle](../../build.gradle); temperature behaviour and configuration are described in [the thermal runtime contract](../../docs/climate/thermal-runtime-control.md).

Thermal tests retain input/source ownership, query and save lifecycle, quality recovery, gameplay integration, and current two-base/scattered/overload fixtures. One-off recording clients, retired capacity variants and duplicate entry wrappers were removed; their history is recorded in [the thermal consolidation](../../diary/2026-10-29_23-38-00_thermal-development-history.md#retired-validation-tools). Other systems' test code and fixture data are separate from that cleanup.

Use Java 17 and compile the test source set with `./gradlew compileGameTestJava` (`.\gradlew.bat compileGameTestJava` on Windows). For example, select the compact-storage checks with `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_compact_storage`. Runtime worlds, logs and profiling recordings are generated outputs and stay outside Git; test source and deliberate fixtures stay versioned.

Player heating equipment checks use namespace `frostedheart_equipment_heating`:
real item capabilities, sustained body curves, stacked devices, fuel cadence and
reload, vest demand, reservoir energy conservation and the actual player tick.
Their gameplay contract and validation scope belong to
[player temperature](../../docs/climate/player-temperature.md).

Knowledge observation checks use namespace `frostedresearch_observation`:
`./gradlew runGameTestServer -PgameTestNamespaces=frostedresearch_observation`.
They cover shared item sampling, 30/60-tick sessions, non-consuming inventory
inspection and interruption when targets, tools or menus change.
`./gradlew runObservationUiClient` creates a fresh local world and uses Minecraft's
keyboard/mouse callbacks with a test-only held-input device to exercise world and inventory observation, including
Shift-right-click and creative inventory. It focuses its own client window,
writes screenshots and `result.json` to `build/observation-ui-validation/`, then
exits and restores the options it changed. These client checks need a desktop
session; they do not verify physical OS input delivery. Player controls are described in
[observations and notes](../../docs/knowledge/observations-and-notes.md).

For the shared thermal comparison baseline, run [`scripts/run-thermal-baseline.ps1`](../../scripts/run-thermal-baseline.ps1). The [baseline reference](../../docs/climate/thermal-validation-baseline.md) owns its scene list, recorded configuration, clocks and result interpretation.
