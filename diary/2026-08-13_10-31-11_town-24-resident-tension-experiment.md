# Town 24-resident steady-state tension experiment

- Time: `2026-08-13 10:31:11 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `stage-4 Java simulator, shared forecast parameter, 24-resident scenario, reports, figures, tests, and town-model documentation`

## Completed

- Added a stage-4 tension experiment that holds population at 24, scans explicit player-built mining/hunting capacities, burns climate and mutable town state in together, caps operational food/fuel reserves through a scenario-level external transmitter, and compares paired fixed-T1 versus forecast-driven overdrive policies.
- Kept every state transition coupled to the existing Java town, resident, T1 fuel, spherical heat-field, and climate kernels. The simulator adds orchestration and observations only; it does not reimplement those formulas in Python.
- Extracted the forecast category sensitivity constant into `TownModelParameters.Defaults`, wired gameplay through `FHConfig`, and included it in stage-0 audit output.
- Added self-defining JSON/CSV output and four player-first/analytical figures: paired history, event raster, capacity map, and policy cost/reliability comparison. Documented all definitions and formal results in `docs/town-model.md`.

## Decisions

- The formal experiment uses `24 residents`, `365 climate burn-in days`, `120 joint town/climate burn-in days`, `120 measured days`, and `1,000 paired seeds`. No opening story blizzard is injected.
- The detailed boundary layout is `mine capacity 8 / hunting capacity 4`, with 14 food-days and 21 normal-T1 fuel-days as operating caps. These caps are scenario automation controls, not gameplay config.
- The minimal forecast policy uses only the current categorical forecast thresholds: a strong-or-colder category in the next 24 hours enables current T1 overdrive. It never controls from exact future temperature.
- Hunting capacity 4 is the useful tension boundary under current values. Capacity 3 is structurally unreliable and 6/8 removes the survival distinction; mining capacity 5–8 has little effect on exits in this experiment.

## Validation

- `./gradlew test` — successful, including the new tension-scenario validation, forecast threshold boundary, compact workplace capacity, finite reserve caps, and all existing regressions.
- `runTownSimulation audit` against the current TWR development instance — successful; snapshot `aaf6e7ac9d2906b74d151eb2dc939f855e7bb19ea3cda62bda8aad0ca9d87a34` contains `climate.forecastSensitivityCelsius=2.0` with its shared default/config source chain.
- `conda run -n standard python -m py_compile Scripts/plot_town_stage4_tension.py` — successful.
- Formal `16 layout × 2 policy × 1,000 paired seed × 240 town day` run completed and reproduced after fixing fixed-policy forecast event visibility.
- At mine 8 / hunt 4, fixed T1 produced `63.2%` full no-exit runs; forecast-driven overdrive produced `100%`, with `46.3%` soft-instability runs and `71.1%` more loaded T1 fuel.
- Final plots were regenerated at 320 dpi and visually inspected; `git diff --check` passed after whitespace cleanup.

## Remaining

- The simple forecast policy is deliberately not optimized and may be too fuel-expensive; later tuning should adjust thresholds/costs only after deciding the desired player warning and survival target.
- Resident exits can still synchronize because generated residents share initial health/mental values and house-level stress. This is a gameplay-model limitation, not simulator noise.
- T2 networks, heaters, thermal inertia, and stage 5+ remain untouched.
