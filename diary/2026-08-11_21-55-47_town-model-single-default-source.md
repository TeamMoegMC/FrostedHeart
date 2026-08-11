# Town model single default source

- Time: `2026-08-11 21:55:47 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `stage-0 town parameters, T1 generator runtime config, audit provenance and scheduling`

## Completed

- Made `TownModelParameters.Defaults` the single source-owned default table for every FH-controlled parameter currently present in the stage-0 model.
- Removed balancing constants from `GeneratorFuelModel` and `GeneratorHeatFieldModel`; these classes now contain only parameterized pure formulas.
- Added `FHConfig.SERVER.TOWN.GENERATOR_T1` entries for fuel-duration multiplier, normal/overdrive process-tick costs, heat-field radii and temperature per level. Added `townUpdateIntervalGameTicks` for the generator's online-town batch cadence.
- Changed `GeneratorData` and `ClimateCommonEvents` to read runtime values only from `FHConfig`; simulator defaults continue to come only from `TownModelParameters.currentDefaults()`.
- Updated the stage-0 source snapshot to label each source-owned value as `shared-default` and record its `TownModelParameters.Defaults -> FHConfig` mapping. Minecraft's 24,000-tick day is separately labelled `minecraft-unit`.
- Updated `docs/town-model.md`, the town package README and added a source-contract regression test for the mapping.

## Decisions

- Keep Minecraft unit conversions in `TownModelParameters.GameUnits`; they are not balancing parameters and are not exposed in `FHConfig`.
- Keep FH/TWR recipes, loot tables, tags, research JSON and KubeJS weights as separately audited external data rather than copying them into Java defaults.
- Existing server TOML values remain intentional runtime overrides. Changing a Java default does not overwrite an already generated explicit config value.
- Only stage-0 model parameters are centralized in this pass. Later housing, climate, resident and T2 parameters should enter `TownModelParameters` and `Defaults` when their corresponding model phase is implemented.

## Validation

- `./gradlew test` — successful; 20 tests, zero failures/errors/skips.
- `runTownSimulation audit` against the current TWR development instance — successful; snapshot `a39c1a1f80d0a7e91ee5b6fa260461ab2af5b3fd1cc1c7bf1c6e1046298e04ed`, unchanged stage-0 numerical results, and no audit issues.
- `git diff --check` — clean.

## Remaining

- No in-game manual smoke test was performed. Add later-phase parameters incrementally instead of centralizing values the simulator does not yet define or consume.
