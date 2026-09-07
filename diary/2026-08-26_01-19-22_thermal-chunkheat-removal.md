# Thermal chunk heat removal and backend convergence

- Time: `2026-08-26 01:19:22 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `partial`
- Scope: `climate temperature compositor, physical heat producers, analytic fields, infrared payload, legacy chunk heat removal, tests, plans, and living documentation`

## Completed

- Deleted `ChunkHeatData`, all `IHeatArea` implementations, the chunk capability attachment, periodic revalidation, the old invalidation packet, and their dedicated JMH/JFR/JOL tasks and fixtures.
- Routed Campfire, Generator, Radiator, and Fountain only through `MinecraftPhysicalSourceManager`; no device creates an analytic field or a second spatial heat representation.
- Kept `AnalyticField` only for Boss, script, and administrator control effects. Curiosity uses `ADD_DELTA`, and `/heat_adjust` uses the explicit analytic backend.
- Changed infrared payload generation to read current analytic fields and physical sources directly. It remains request-on-view-chunk-change and has no periodic polling or source-to-chunk invalidation table.
- Expanded the physical-source GameTest to account for all four producers in one ledger and added Fountain/Radiator profile coverage.
- Updated the active thermal plan and living climate/town documentation to distinguish deleted historical measurements from current behavior.

## Decisions

- There is no migration adapter, source-position exclusion table, shadow backend, or fallback to the deleted chunk heat system.
- Page/publication misses use the natural backend. Device heat is physical power; non-conservative analytic fields are not a device compatibility mechanism.
- Old chunk heat benchmark numbers remain only as explicitly historical migration evidence.

## Validation

- Source scan found no `ChunkHeatData`, `CHUNK_HEAT`, `IHeatArea`, old heat-area implementation, invalidation packet, or revalidation reference under `src/main`, `src/test`, `src/jmh`, or `build.gradle`.
- `git diff --check` reported no whitespace errors.
- Java 17 compilation reached project sources without a thermal error, then failed on 19 unrelated references to the concurrently renamed `FHTags.Blocks.SLUDGE`; thermal JUnit, JMH compilation, and Forge GameTest could not be rerun reliably past that blocker.

## Remaining

- After the unrelated tag rename is reconciled, run thermal JUnit, `jmhClasses`, and `runGameTestServer`; confirm the expanded four-device physical-source scenario.
- Production-like multiplayer CPU, allocation, retained-memory, and gameplay calibration remain separate thermal-runtime work.
