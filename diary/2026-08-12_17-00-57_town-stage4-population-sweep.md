# Town stage 4 population sweep and complete figure set

- Time: `2026-08-12 17:00:57 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `stage-4 Java simulator, scenario interface, Monte Carlo reports, plots, tests, and docs`

## Completed

- Added optional `populationSweep` stage-4 scenarios and a Java one-process sweep runner. The committed experiment scans 100 evenly spaced integer populations from 1 through 200 with paired seeds.
- Each population is converted to stacked, three-high balanced rectangles whose current shared house and hunting capacity formulas meet the requested population. Initial edible food preserves reserve days; fixed T1 coke does not scale with population.
- Added daily cross-seed reserve distributions to the single-scenario stage-4 output and added precise fuel/food self-supply, shortage, survival, no-shortage, and Wilson interval fields to stage-4 aggregates.
- Added `population.csv`, `reserve-trajectories.csv`, and a self-defining sweep `summary.json`. Ten trajectory populations are retained: `1/8/11/12/13/14/16/24/48/200`.
- Replaced the old three-population plotting interface with five no-title figures covering geometry/temperature, both self-supply ratios with theory, all four outcome probabilities, both reserve trajectories, and the analytic coverage/climate limit. Removed the ambiguous `coupled` labels.
- Updated `Scripts/town_scenarios/README.md` and `docs/town-model.md` with exact metric definitions, layout construction, commands, and results.

## Decisions

- Used the current code capacity formulas rather than scaling the earlier generous `16 floor block/resident` reference buildings. This makes the scan test the claimed approximately 200-resident T1 housing geometry without introducing a second capacity model.
- Used 1,000 runs per curve population to retain the statistical precision of the existing stage-3/4 references. Every binary estimate carries a Wilson 95% interval and population comparisons reuse the same seeds.
- Reserve plots use only ten selected populations and emphasize 13 residents; the other aggregate plots use all 100 requested curve points.
- Kept all existing phase-4 exclusions: no T2/heaters, heat inertia, random T1 ramp, story blizzard, or exact intra-day fuel outage timing.

## Validation

- `./gradlew compileJava compileTestJava` — passed.
- `./gradlew test --tests 'com.teammoeg.frostedheart.content.town.model.TownStageFourModelTest'` — passed, including 100-point uniqueness and 200-person current-capacity layout checks.
- Two-seed end-to-end CLI smoke sweep — produced 101-line `population.csv` and 1,201-line `reserve-trajectories.csv` and all five figures.
- A 200-seed full sweep first passed as an end-to-end scaling check in about 78 seconds.
- Final `120 days × 1,000 seeds × 100 population points` sweep plus six trajectory-only points — passed in about 6 minutes 36 seconds; report is under `build/reports/town-model/simulations/stage4-t1-population-sweep-1000`.
- Generated and visually inspected all five final PNGs with Matplotlib in Conda `standard` under `build/reports/town-model/figures/stage4-population-sweep-1000`.
- `./gradlew test` — full repository test suite passed after the final documentation and plot changes.

## Remaining

- The approximately 198-resident bound remains a house-only arbitrary-footprint geometric upper bound. The current 200-person balanced rectangular house/hunting layout is not fully covered (`90.07% / 85.14%`).
- Parameter tuning remains intentionally deferred; phase 5 T2/heater work is untouched.
