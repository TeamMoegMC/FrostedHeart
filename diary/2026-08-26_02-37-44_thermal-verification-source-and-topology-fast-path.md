# Thermal verification source split and topology fast path

- Time: `2026-08-26 02:37:44 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `thermal verification packaging, topology stable-frame allocation, and material-neighbor invalidation`

## Completed

- Moved `6,016` lines of Phase 0 reference/probe/census, Phase A census, Forge GameTest, and the probe Mixin from `src/main/java` to the dedicated `thermalVerification` source set.
- Wired JUnit and JMH to the verification output and made `runGameTestServer` load the verification classes and `frostedheart.phase0a.mixins.json` explicitly.
- Removed production JAR class-name exclusion rules and the production Mixin plugin branch for the Phase 0 probe; production thermal Java is now `24,569` lines across `65` files.
- Made unchanged topology frames ACK and return before allocating the active Page list or section index.
- Replaced the boxed topology section index with `Long2ObjectOpenHashMap` and changed material dependency propagation from a candidate-by-change cross scan to six direct neighbor lookups.
- Corrected `MinecraftMaterialBoundaryTest` to seal a second epoch before requesting a second runtime solve, which also exercises the unchanged-topology path.

## Decisions

- Kept all public `MinecraftThermalInput.enableTopologyApplication` overloads because they remain integration contracts; removing unused in-repository overloads would change the public API rather than merely reduce duplication.
- Kept full Page rebuild and whole-sweep replacement unchanged. Brick-local incremental rebuild remains profile-gated because it would add substantial state and correctness surface.
- Marked the verification-only probe injection `remap=false`; it runs only in the deobfuscated GameTest source set and therefore must not create a production refmap dependency.

## Validation

- Java 17 offline production, `thermalVerification`, JUnit, and JMH compilation passed.
- Java 17 thermal JUnit passed `251/251`.
- Forge GameTestServer passed `21/21` required tests, including Phase 0a probe, Phase A census/geometry, and Minecraft thermal integration batches.
- Production and deobf JARs contained zero matches for Phase 0, thermal GameTest, probe Mixin, verification Mixin config, or verification refmap entries.
- Living climate documentation and the active thermal plan were updated for the source-set and topology-path changes.

## Remaining

- Measure real production mutation workloads before deciding whether full `16^3` Page rebuild should become `4^3` Brick-local rebuild.
- Production-like `1/10/50/100` player CPU, retained heap, allocation, GC, and TPS evidence remains open.
