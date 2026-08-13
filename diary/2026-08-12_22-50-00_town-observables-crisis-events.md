# Town player observables and crisis event analysis

- Time: `2026-08-12 22:50:00 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `shared town observations, daily history, stage-4 event/episode simulation, reports, plots, tests, and design docs`

## Completed

- Added Forge-independent `TownObservationModel` kernels for resident mean/P10/minimum state, current work eligibility, next-morning exit risk, reserve slope/time-to-empty, Fano factor, and exit-interval CV.
- Added persisted `TownSignalEvent` records with threshold type, severity, affected count, episode ID, and detail. Extended backward-compatible `TownHistoryEntry` snapshots with low-tail state, work/exit risk counts, tower state, climate level, and daily events.
- Connected actual resident exits, tower/climate crossings, and resident threshold crossings to gameplay daily history without changing settlement outcomes or the Mayor's Seal UI.
- Added the stage-4 observer and explicit three-day warning/seven-day recovery episode definition. Single-scenario reports now include `observations.csv` and `events.csv`; population sweeps include distributions for reserve trends, low-tail state, event rates/Fano factors, episode size, warning lead time, and recovery.
- Kept threshold event frequency separate from impact: event rate/Fano count event records; episode size counts the union of resident IDs affected, so one resident cannot be double-counted.
- Extended `Scripts/plot_town_stage4.py` with five new no-title figures while retaining all five existing stage-4 figures. Updated `docs/town-model.md` and scenario documentation with formulas, definitions, results, and UI interpretation boundaries.

## Decisions

- Player-facing candidates are reserve days/trend, unable-to-work and next-morning-exit-risk counts, and a 30-day event timeline. Fano factor, interval CV, and episode size remain design diagnostics.
- A crisis episode starts on a three-day reserve warning, actual food/tower shortfall, critical building temperature failure, exit risk, or exit; it ends only when both reserves reach seven days and critical services/risk recover.
- Did not add arbitrary resident noise. The current standard-adult benchmark therefore reveals synchronized all-or-none failure and cannot yet measure the benefit of P10 under real resident heterogeneity.

## Validation

- `./gradlew test` — all 63 tests passed, including new shared observation kernel tests.
- End-to-end three-seed population-sweep smoke run — all CSV rows had consistent widths and all ten stage-4 figures rendered.
- Final `1–200 population × 1,000 paired seed × 120 day` Java sweep passed in about 8 minutes. `population.csv` has 100 data rows/100 columns; `reserve-trajectories.csv` has 1,200 data rows/37 columns; maximum P95 episode affected fraction is exactly `1.0`, never above the population.
- Generated and visually inspected five new PNGs under `build/reports/town-model/figures/stage4-t1-population-sweep-observables-1000` using Matplotlib in Conda `standard`.

## Remaining

- Mayor's Seal UI intentionally remains unchanged pending review of these signals.
- The standard benchmark initializes homogeneous residents. A future heterogeneity experiment should sample the current gameplay-owned resident attribute generation, not invent an independent noise model.
- T2/heaters and later model stages remain untouched.
