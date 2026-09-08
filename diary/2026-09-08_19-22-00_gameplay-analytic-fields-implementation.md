# Shared gameplay analytic fields and generator floor

- Time: `2026-09-08 19:22:00 +08:00`
- Author: `Codex; OpenAI GPT-6; primary implementation agent`
- Status: `completed`
- Scope: `world-owned generator/boss/command fields, gameplay composition, lifecycle, and Forge production-path validation`

## Completed

- Implemented the [reviewed architecture](../plans/2026-09-08_18-28-06_gameplay-analytic-fields-architecture.md): `MinecraftGameplayFields` owns one index per actual world; physical reloads keep the same index and field-only publication does not start a dimension runtime.
- Added full provider/owner/channel identity, cached generator keys, unchanged-report comparisons, and ordered entries with a hash map for updates. Ordinary queries compose directly; town and phase queries reuse scalar reductions.
- Generator retains its physical source and supplies `max(raw, localNatural + maximumMatchingDelta)` before boss/command controls. Existing team enumeration publishes the floor, reconciles removed holders, and preserves unloaded-team afterheat without loading tower chunks.
- Corrected authoritative T1/T2 disassembly cleanup, dimension-aware binding, command/boss identity collisions, and same-object boss readdition. Player, crop, town, and explicit analytic phase thresholds consume the shared composition.
- Updated living [world temperature](../docs/climate/world-climate-and-temperature.md), [lifecycle](../docs/climate/data-lifecycle-and-integration.md), [heat production](../docs/climate/heat-production-and-network.md), [runtime architecture](../docs/climate/thermal-runtime-architecture-and-optimization.md), [boss](../docs/boss/curiosity-boss-design.md), and [town](../docs/town/town-model.md) documentation. Historical offline `TownStageFourModel` formulas remain intact and are marked as legacy references.

## Decisions

- Keep O(F) compact-list containment queries and expected O(1) unchanged/value-only updates. Generator reconciliation adds O(F) work around an existing holder enumeration. No new scheduler, spatial tree, per-chunk field copies, mesh leases, or field serialization.
- Separate world lifetime from physical runtime lifetime to fix restoration and publication behavior. This ownership change is not itself a claim of faster steady-state queries; map/key/entry storage increases index memory over the old single list.
- Real disassembly can run after the master block entity loses its state. Enumerate authoritative team data at this rare event rather than retain another reverse index or poll block entities.
- An explicit analytic bound may authorize its own warming threshold within physical ownership. Delta-only fields cannot bypass latent heat; cold controls lower that bound.

## Validation

- Java 17: `./gradlew.bat runGameTestServer --offline --no-daemon --console=plain` completed successfully at 19:21:58, with **all 33 required tests passing** after final diagnostic cleanup. No JUnit additions or execution. Full local output: `run-gametest/analytic-fields-verification.log`; server evidence: `run-gametest/logs/latest.log` (generated, not committed).
- Eight new [production-path tests](../src/gametest/java/com/teammoeg/frostedheart/content/climate/thermal/runtime/minecraft/ThermalGameplayFieldGameTests.java), plus expanded real T1/T2 tests, exercise command/player/town/actual wheat growth decisions, fixed-time dimensions, real boss AI/removal/revival, runtime/profile reload, rebind/team transfer, actual tower-chunk unload without query reload, and loaded recipe ice transitions inside/outside physical ownership.
- The configured ice warming target is thin ice, not an assumed vanilla water result. Tests verify that target and cold/delta-only exclusions using the actual temperature-transition mixin.
- Real T1 exhaust enclosure: natural `-4.7999997 C`, floor `25.2000003 C`, raw Air `29.8836922 C`, gameplay `29.883692 C`. Outdoor gameplay `25.2000008 C`, with raw Air unavailable (`NaN`). This proves the floor works without a physical result and that a connected, resolvable enclosure can exceed it; it does not measure an outdoor dissipation curve.
- Final production-facade query sample, 20,000 queries per case after warmup: F=1 `88.065 ns/query, 0 bytes`; F=16 `171.53 ns/query, 0 bytes`; F=128 `770.125 ns/query, 4,088 total allocated bytes`. Timings and incidental allocations varied between runs. These are same-process descriptive samples, not old/new baselines, retained-heap measurements, or multiplayer performance claims.

## Remaining

- Existing dynamic-shape topology limitation: `MinecraftThermalProfiles.prepare` assigns unresolved geometry to dynamic blocks; `BrickTopologyCompiler` leaves the containing Brick unresolved. A generator exhaust sharing that Brick can have a registered source but no physical Air result. The passing enclosure fixture aligns the exhaust above the tower's Brick; analytic floor behavior does not depend on that alignment. General dynamic-shape support remains separate work.
- No full server-stop/restart restoration test, long-duration normal-server/multiplayer stress run, retained-heap measurement, matched old/new benchmark, or complete physical Page center/face/edge/corner comparison was performed. The broader acceptance matrix is retained in the plan with these limits stated explicitly.
