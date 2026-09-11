# Temperature System Current Documentation

- Time: `2026-08-22 04:48:40 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `docs/README.md`, `docs/climate/`

## Completed

- Read the implemented climate, world-temperature, local heat-area, player-temperature, generator, heat-network, configuration, recipe, capability, event, packet, command, Mixin, and test paths.
- Added a climate documentation index and four classified living documents covering formulas, units, lifecycle, persistence, performance paths, consumers, and known implementation boundaries.
- Recorded current non-physical semantics explicitly: no unified energy conservation, material heat capacity, SI power, or stateful convection model exists yet.

## Decisions

- Documented current behavior without changing source code or treating intended refactors as implemented behavior.
- Kept world climate/temperature, player temperature, heat production/network, and data/lifecycle separate because they use adjacent but non-unified numerical models.
- Treated source and runtime consumers as authoritative where names, comments, old diary entries, or configuration intent disagree with execution.

## Validation

- Verified every relative Markdown link under `docs/climate/` resolves and found no trailing whitespace.
- `git diff --check -- docs/README.md` passed; only the existing Windows LF-to-CRLF warning was emitted.
- With JDK 17 and the existing user Gradle cache, ran `gradlew.bat test` for `BlockTemperatureModelTest`, `ClimateEventModelTest`, `SphericalHeatFieldModelTest`, `GeneratorHeatFieldModelTest`, `GeneratorFuelModelTest`, and `SurroundingTemperatureSimulatorCacheTest`: `BUILD SUCCESSFUL`.
- The first isolated-cache attempt was blocked before tests by CurseMaven HTTP 429 responses; the offline cached rerun passed.

## Remaining

- Before implementing convection, heat capacity, or physical power, define compatibility tests for player-body progression, async sampling timing, data reload cache invalidation, negative heat areas, heat-network distribution, and `ServerLevel.tickChunk` Mixin equivalence.
- Measure player sampling, temperature-aware random ticks, heat-area maintenance, player packet traffic, and heat-network allocation as separate performance paths.
