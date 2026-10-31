# Forge GameTest sources

This directory contains reusable Forge GameTest cases, test-only observers, catalogue fixtures and resources. They use the `gameTest` source set and are excluded from the ordinary production JAR.

Production source and data under `src/main/` and `src/generated/` define implemented behaviour. These tests describe the checks they perform; successful cases do not prove untested loads.

Start with the relevant system in [docs/README.md](../../docs/README.md). The `gameTest` source set and `runGameTestServer` task are configured in [build.gradle](../../build.gradle); temperature behaviour and configuration are described in [the thermal runtime contract](../../docs/climate/thermal-runtime-control.md).

Thermal tests retain input/source ownership, query and save lifecycle, quality recovery, gameplay integration, and current two-base/scattered/overload fixtures. One-off recording clients, retired capacity variants and duplicate entry wrappers were removed; their history is recorded in [the thermal consolidation](../../diary/2026-10-29_23-38-00_thermal-development-history.md#retired-validation-tools). Other systems' test code and fixture data are separate from that cleanup.

Use Java 17 and compile the test source set with `./gradlew compileGameTestJava` (`.\gradlew.bat compileGameTestJava` on Windows). For example, select the compact-storage checks with `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_compact_storage`. Runtime worlds, logs and profiling recordings are generated outputs and stay outside Git; test source and deliberate fixtures stay versioned.
