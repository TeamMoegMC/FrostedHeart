# Warm stone adaptation to generator gameplay fields

- Time: `2026-09-09 11:50:02 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `warm stone and hot-water bag compatibility with master@7ec9ffd13, including generator-field update f3f6b0e8c`

## Completed

- Fetched origin; local master already contained the latest remote changes. The remote refactor had removed `MinecraftThermalInput.gameplayItemEnvironment` and its bounded position cache while the dropped-item handler and tests still called them.
- Restored that narrow query using the current raw Air/dormant fallback and independent item radiation budget. Composed world-owned `MinecraftGameplayFields` even when no physical runtime exists. Generator heat uses `max(rawOrFallback, localNaturalAir + matchingDelta)` before the other control modes.
- Cached only physical inputs, then composed fields at each exact item receiver position. Same-tick provider updates/removal and field boundaries within a cached quarter-block remain visible. Runtime close clears the item cache and retains world-owned fields.
- Verified inventory already uses the updated passive compositor. Worn reservoirs initialize from composed player air and continue exchanging through the existing five-part body delta; no extra generator-to-worn-item transfer or balance change was added.
- Added two Forge cases covering both reservoir items, real inventory/item hooks, field-only operation, initialized dropped warming, active-cache update/removal/boundaries, runtime close, and worn initialization. Extended the real generator enclosure case to verify that dropped sampling does not add the floor to an already hotter physical result.
- Documentation impact: updated [world temperature](../docs/climate/world-climate-and-temperature.md), [lifecycle](../docs/climate/data-lifecycle-and-integration.md), and [player temperature](../docs/climate/player-temperature.md). No companion-pack changes.

## Decisions

- Reuse the existing world field index and item radiation service; retain the 64-position cache and existing 64/32/4/4 item radiation budget.
- Preserve the dropped-item minimum age and staggered cadence. The first new fixture mistakenly checked initialization before age 20; correcting it to exercise ticks 20–39 and 40–59 made the existing production cadence pass.
- Limit JUnit compilation/execution to warm-stone-related sources through a generated local init script. Upstream thermal tests still import deleted `ConservativeAirGeometry`/`ComponentBrickCompiler` classes; their migration is outside this adaptation.

## Validation

- Java 17.0.2, offline Gradle, one worker. Used a 1536 MiB heap for initial compilation, then 1024 MiB for the test driver, 512 MiB for JUnit, and 1408 MiB for the Forge server because this workstation previously exhausted native memory with the default 6 GiB daemon.
- `gradlew.bat compileJava compileGameTestJava`: passed. Full local output: `build-warm-stone-adaptation.log`.
- Targeted JUnit: 18 suites, 82 tests, zero failures/errors/skips. Selected reservoir handlers/models, five-part delta, radiation, item query cache, Curios slot, and warm-stone command tests. Reports: `build/test-results/warm-stone-adaptation`; local selection/memory options: `build/warm-stone-verification.init.gradle`.
- Final `gradlew.bat runGameTestServer --offline --no-daemon --max-workers=1 --init-script build/warm-stone-verification.init.gradle` run: **41/42 required tests passed**, including both new cases, the expanded real generator enclosure, and the existing actual ItemEntity/Campfire radiation case. Output: `build-warm-stone-gametest-final.log`.
- Real enclosure: physical Air `29.860526 C`, generator block-query floor `25.100000 C`; the dropped-item assertion compares the same physical value against its own local natural-Air floor and passed.
- `git diff --check`: passed.

## Remaining

- The unchanged upstream `ThermalInfraredGameTests.largeGeneratorFieldFitsTheProductionDisplayPayload` repeatedly fails for a radius-128 generator: `writerIndex(983035) + minWritableBytes(8)` exceeds `InfraredBrickCodec.MAX_PAYLOAD_BYTES = 960 * 1024`. This causes the full Forge task to exit with failure despite all warm-stone cases passing. The failing capture runs with the physical runtime closed and does not call the item query; infrared encoding was not changed here.
- Full repository JUnit was not run because the upstream geometry tests need migration to the new block model.
- Changes remain in the local working tree; no commit or push was performed for this inspection/adaptation.
