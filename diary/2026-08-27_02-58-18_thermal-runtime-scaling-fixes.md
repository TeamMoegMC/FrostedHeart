# Thermal runtime scaling fixes

- Time: `2026-08-27 02:58:18 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and review agent`
- Status: `completed`
- Scope: `Thermal arena allocation, physical source rebinding, mutation drain, environment refresh, FarField patching, tests, and climate living documentation`

## Completed

- Replaced `ThermalCellArena` high-water free-slot scans with start/length indexed best-fit spans, adjacent-span coalescing, and tail high-water contraction.
- Added `MinecraftPhysicalSourceManager.sourcesByTargetSection`; Page invalidation, successful admission/geometry commit, withdrawal, and chunk load now dirty only source ports targeting the affected section. Port resolution uses `installedActiveBySection` directly.
- Added concurrent `deferredDirtyOwners`; unchanged deferred revision returns immediately, and changed revisions no longer scan every attached section owner.
- Replaced dimension-wide 200-tick Page environment scans with a section-hash-staggered natural-temperature queue capped at 16 Pages per tick, one O(1) wind sample, and mutation-local sky-column refreshes. Natural temperature, sky exposure, and wind now patch only affected FarField components without replacing geometry coverage.
- Corrected the sky refresh timing found during review: `LevelChunkSection.setBlockState` returns before `LevelChunk` updates its heightmap, so main-thread XZ columns are coalesced and queried at tick-end. Raw container and section identity replacement enqueue the owning chunk's 256 columns once.
- Added `ThermalSweepFragments` preflight validation for replacement generations, FarField live slots, final operation counts, and aggregated material conductance before any pair/reference/traversal mutation.
- Updated `docs/climate/data-lifecycle-and-integration.md` and `docs/climate/world-climate-and-temperature.md` to describe the implemented scaling and refresh boundaries.

## Decisions

- Keep the existing Page/Brick traversal order, material contribution order, arena `H/C` ownership, source energy accounting, and latest-only frame behavior.
- Keep full topology compilation for Page/fragment lifecycle changes. Natural temperature, sky exposure, and wind are derived boundary changes and remain incremental.
- Retain one `OpenPatchFragment` per Brick so sky evidence can be removed exactly; use reference counts because one air cell can receive evidence from multiple fragments.
- Bound natural-temperature work per tick. More than 3,200 continuously admitted Pages may refresh slower than 200 ticks instead of creating a periodic dimension spike.

## Validation

- `gradlew.bat compileJava --offline --console=plain` completed successfully.
- `gradlew.bat compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain` completed successfully: `181/181` JUnit tests passed with zero failures, errors, or skips.
- `gradlew.bat runGameTestServer --offline --console=plain` completed successfully: all `11/11` required Forge GameTests passed.
- Added regression coverage for arena best-fit/coalescing/tail contraction, Page admission and section-local source notifications, environment-only coverage stability, and rejected fragment patches leaving the installed sweep unchanged.
- `git diff --check` reported no whitespace errors; only repository line-ending conversion warnings were emitted.

## Remaining

- Run JFR against the same large real-save mutation/source workload before and after these changes to quantify P95 tick time, allocation rate, free-span index size, and environment refresh backlog.
