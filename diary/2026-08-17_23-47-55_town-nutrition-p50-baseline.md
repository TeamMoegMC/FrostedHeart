# Town nutrition P50 quick baseline

- Time: `2026-08-17 23:47:55 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Stage 4 fixed-population nutrition simulation scenario and reports`

## Completed

- Added `Scripts/town_scenarios/experiments/stage4-t1-p50-quick.json`, an isolated fixed-seed `50 residents x 120 days x 100 trials` version of the existing compact T1 hunting-only Stage 4 setup.
- Ran the scenario without changing model parameters or pack nutrition recipes and generated the standard resident dynamics plots under `build/reports/town-model/figures/stage4-t1-p50-quick-100`.

## Decisions

- Kept the formal Stage 4 inputs unchanged apart from population points and trial count so this run is a directly comparable quick baseline.
- Retained hunting-only food supply; this run measures the current failure mode rather than proposing a balanced diet.

## Validation

- `./gradlew runTownSimulation --offline ...stage4-t1-p50-quick.json...` completed successfully in 6 seconds.
- All 100 trials survived 120 days without food shortage or resident exits.
- Fat, carbohydrate, and vegetable crossed the severe threshold for all 50 residents on day 5; protein crossed for a mean 32.52 residents on day 5 and 17.48 on day 6.
- The last-30-day median reserves were fat `0.301`, carbohydrate `0`, protein `1.838`, and vegetable `0`; median health was `47.087` and mental was `65.000`.
- Generated CSV files and four PNG plots successfully; `git diff --check` passed for the scenario.

## Remaining

- Correct the Stage 3/4 production-processing-meal ordering before using the simulator for balance changes.
- Run a small P50 parameter comparison that slows reserve turnover and, separately, supplies carbohydrate and vegetable foods from existing read-only recipes. Pure hunting cannot sustain channels with zero intake.
