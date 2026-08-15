# Town numerical model stage 0 audit

- Time: `2026-08-10 23:36:02 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `T1 generator shared numerical functions, town parameter records, FH/TWR source audit, algebraic baselines, CLI and tests`

## Completed

- Added Forge-independent stage-0 parameter records and algebra in `content/town/model`.
- Added `GeneratorFuelModel` and `GeneratorHeatFieldModel`; `GeneratorData` now calls these same functions for fuel duration, refill predicate, town batch size, radius and heat-field temperature.
- Moved phase-0 FHConfig default values to constants shared with `TownModelParameters`, without changing their values or config keys.
- Added the `TownSimulationMain audit` command and `runTownSimulation` Gradle entry. The audit parses FH coal/coke recipes and hunting loot, TWR fossil-deposit weights and generator-efficiency research files.
- Added `source-snapshot.json` with parameter units, source symbols, absolute source paths and SHA-256 hashes, plus `audit-report.json` with formulas, inputs, metrics, exclusions and compatibility issues.
- Updated `docs/town-model.md` and the town package README. Stage 0 remains algebra-only; no multi-day simulation or T2 network was added.

## Decisions

- Preserved the documented ideal recipe-duration fuel baselines, but did not treat them as current inventory behavior.
- Added current-code steady-state T1 rates because `GeneratorData` refills at `process <= 20`: no-research coal/coke rates are `21.8182/10.8108` items per active day, requiring `18.7013/9.2664` mining SWE.
- Preserved and reported the level-2 generator-efficiency floating-point truncation (`1439/2879` process ticks rather than decimal-arithmetic `1440/2880`); stage 0 does not fix gameplay behavior.
- Limited source parsing to quantities used by stage 0. Food nutrition/tag auditing waits for stage 2, climate for stage 4, and all T2 network inputs for stage 5.

## Validation

- `./gradlew test` — successful; 18 tests, zero failures/errors.
- Ran `runTownSimulation` against the current TWR development instance; every documented stage-0 ideal baseline was reproduced and both JSON reports were generated under `build/reports/town-model/audit/stage0-current/`.
- `git diff --check` — clean.

## Remaining

- Stage 1: implement a one-day deterministic resource ledger for SWE, mining, infinite mine relocation, abstract coal-to-coke processing and T1 fuel consumption. Use current batched fuel rates for compatibility and retain ideal rates as comparison metrics.
- Decide separately whether the generator batch-tail loss and level-2 floating truncation should be fixed in gameplay; do not change them silently through the simulator.
