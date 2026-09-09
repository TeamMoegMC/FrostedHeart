# Placeable warm stones and hot-water bags

- Time: `2026-09-09 12:36:41 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `warm stone / hot-water bag block placement, stored thermal state, exposed environment exchange, models, and climate documentation`

## Completed

- Converted `WarmStoneItem` into a `BlockItem` while retaining the existing item IDs, item translation keys, item models, thermal profiles, inventory/dropped hooks, and Curios contract.
- Registered `frostedheart:warm_stone` and `frostedheart:hot_water_bag` blocks plus shared block entity type `frostedheart:thermal_reservoir`. Added small horizontal models using the existing item textures and four placement orientations.
- `ThermalReservoirBlockEntity` stores one complete item under `ReservoirItem`. Placement, save/reload, pick block, ordinary breaking, and support loss preserve the temperatures and original item data. Creative placement copies the stack without consuming it.
- Added a position-based entry to the existing exposed-item environment query. Placed blocks reuse `DroppedReservoirExchangeHandler.exchangeInto`, including air, world-owned generator fields, and bounded direct radiation. They exchange once per 20 loaded ticks after the initial loaded-second delay; unloaded time is not simulated.
- Documentation impact: updated [player temperature](../docs/climate/player-temperature.md), [climate lifecycle](../docs/climate/data-lifecycle-and-integration.md), and [world temperature](../docs/climate/world-climate-and-temperature.md). No companion-pack changes.

## Decisions

- Sample above the model at block-relative `(0.5, 0.3125, 0.5)` using the same physical sample cache and radiation budget as dropped items. No new physical world heat source is registered, matching dropped-reservoir behavior.
- Use ordinary block entity update tags/packets for the saved item so client pick block sees the current state. Drop the saved stack directly through `getDrops`, following existing stateful-block conventions.
- Retain the existing item presentation and capacity/exchange constants. Block models use existing texture regions; no texture files were modified.

## Validation

- `compileJava`: passed with Java 17.0.2, offline Gradle, one worker, and a 1536 MiB daemon heap. The first invocation inherited the workstation's Java 8 `JAVA_HOME`; setting Java 17 for the command resolved it. Output: `build-placed-reservoir-compile.log`.
- `compileGameTestJava test --init-script build/warm-stone-verification.init.gradle`: passed. Existing scoped warm-stone suite: 18 suites, 82 tests, zero failures/errors/skips. The existing local init script excludes upstream JUnit sources that still reference removed thermal geometry classes. Output: `build-placed-reservoir-tests.log`.
- `runGameTestServer` with the same init script: 45/46 required tests passed. All four new cases passed: real right-click placement / creative placement / reload / pick block / breaking / support loss for both items; hot/cold block ticks for both items; fresh initialization; and real campfire radiation with placed/dropped query equivalence for both items. Existing dropped, inventory, and generator-field cases also passed. Output: `build-placed-reservoir-gametest.log`.
- Inspected a local isometric rendering of both JSON block models with their actual item textures (`build/placed-reservoir-models-preview.png`). This is an asset preview, not a Minecraft client screenshot.
- `git diff --check`: passed.

## Remaining

- The existing `ThermalInfraredGameTests.largeGeneratorFieldFitsTheProductionDisplayPayload` still exceeds the 960 KiB infrared payload limit, making the full GameTest task exit with failure. This issue was already documented in the [preceding adaptation entry](2026-09-09_11-50-02_warm-stone-generator-field-adaptation.md) and is unrelated to placement.
- Interactive client visual inspection was not run. No commit or push was made.
