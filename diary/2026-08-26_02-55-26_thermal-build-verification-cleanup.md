# Thermal build and verification cleanup

- Time: `2026-08-26 02:55:26 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `thermal Gradle wiring, disposable validation sources, generated artifacts, and retained Forge GameTests`

## Completed

- Reduced `build.gradle` from `1,352` to `658` lines by removing the JMH plugin, JOL dependency, Phase 0/census run properties, probe Mixin config, and the one-off JMH/JFR/JOL/evidence/report task block; the retained source set explicitly targets lowercase `src/gametest` for cross-platform builds.
- Replaced the broad `thermalVerification` source set with a minimal `gameTest` source set containing only `FrostedHeartPhaseAGeometryGameTests` and `FrostedHeartMinecraftThermalInputGameTests`.
- Deleted Phase 0 reference/probe/writer-census/adapter assets, Phase A resolver census, self-validating evidence JUnit, all ten JMH fixture sources, and the verification-only Mixin configuration.
- Ran the standard Gradle `clean`, removing the old `thermal-phase0b`, `thermal-phase-a`, and `thermal-phase-l` reports, JMH generated classes/resources/results, old source-set outputs, and the large JMH artifact.
- Updated the living climate documentation and active thermal plan so deleted benchmark and probe paths are no longer presented as current build entry points.

## Decisions

- Keep long-lived unit tests and the `11` Forge GameTests that exercise real geometry/runtime integration; remove synthetic census, historical migration evidence, and test harnesses with no production consumer.
- Do not keep a permanent thermal benchmark framework in the main build. Future performance work should begin from a concrete regression or a real save workload and use an external profiler or a narrowly scoped temporary harness.
- This cleanup does not change `H/C/P*dt`, topology, source, radiation, publication, material, or gameplay query semantics.

## Validation

- Java 17 `compileJava compileGameTestJava compileTestJava` passed.
- Remaining thermal JUnit passed `202/202`.
- Forge GameTestServer passed `12/12` required tests: Phase A geometry `4`, Minecraft thermal integration `7`, and Frosted Research `1`.
- Java 17 `jar deobfJar` passed; both artifacts retain `MinecraftThermalInput` and contain zero thermal Phase 0, census, probe, JMH, verification-source-set, or thermal GameTest entries.
- The removed report/JMH/source-set build directories and `*-jmh.jar` are absent after regeneration.

## Remaining

- Production-like multiplayer CPU, allocation, retained heap, GC, and TPS measurements remain gameplay validation work, not permanent Gradle build machinery.
