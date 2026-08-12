# Town model stages 1–2

- Time: `2026-08-12 00:03:49 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town mining/hunting/food/house numerical kernels, Java simulator, audit, plots, and docs`

## Completed

- Extracted `MiningDailyModel`, `HuntingDailyModel`, full `HouseDailyModel.evaluateSettlement`, house capacity, food priority/consumption, and abstract meat-processing kernels. Gameplay mining, hunting, housing, capacity, and same-level food-quality code now calls the shared functions with runtime `FHConfig` values.
- Added the independent one-day stage 1–2 Java simulator, source-data loader, exact theory helpers, scenario schema, CLI `simulate`, baseline scenario, JSON/CSV outputs, and fixed-seed hunting sampling.
- Extended `audit` to hash the new pure-model sources and read resident food-level Tags plus raw/cooked meat nutrition recipes. Final audit snapshot: `35` source files, `179` parameters, hash `a1bf73bf37fd2012ddd8a7da3067e6ff53a8030aff05403a3d55ce534d5f71cf`.
- Added Matplotlib plotting from Java-generated CSV and committed the production-balance and house-response PNGs under `docs/figures/town-model/`.
- Updated `docs/town-model.md`, the town README, and scenario documentation with exact stage boundaries, formulas, commands, observations, and results.

## Decisions

- Stages 1–2 are independent one-day kernel experiments. Hunting output is not fed into house inventory, and resident state, work eligibility, proficiency, assignments, and inventories do not advance to another day; those remain stage 3.
- Partial meat-processing theory uses a `6/7 hunting SWE` unit-roll experiment, which produces exactly one roll/day at the current `7/6 roll/SWE-day` rate. This makes every processing-capacity point exactly enumerable without a multi-day simulation.
- Finite meat-processing capacity follows an explicit rational-player policy: highest cooked-minus-raw food-unit gain first. It is scenario logic, not a claim about a particular Create/IE machine.
- Plots follow `Figure_Guidelines.md`: Arial, colorblind-accessible colors, theory lines, simulation markers/error bars, outward ticks, no top/right spines, and 320 dpi PNG.

## Validation

- `./gradlew test` — complete test suite passed after the final code changes.
- `runTownSimulation simulate ...stage12-one-day.json` — `10000` fixed-seed runs; meat `1.0233` vs theory `1.0263158` per executed roll and cooked food `19.22036` vs theory `19.3368421`; both within `3 × SE`.
- `runTownSimulation audit ...stage12-final` — all current algebraic baselines preserved; food Tags/nutrition and six stage-1/2 model sources entered the snapshot.
- `conda run -n standard python Scripts/plot_town_stage12.py ...` — both PNGs generated and visually inspected; `python -m py_compile` passed.
- `git diff --check` — passed.

## Remaining

- Do not add cross-day inventory/order/worker feedback before stage 3 review.
- Current formulas reveal two future balancing questions, not fixed here: cold temperature only suppresses recovery and never directly damages a fed resident; meat nutrition quality is only a few percent of the configured `7000 nutrition/food-unit` reference, so the `0.5` recovery floor dominates.
- Climate and spherical heat fields remain stage 4; T2 remains stage 5.
