# Town nutrition Stage 4 Monte Carlo

- Time: `2026-08-17 23:20:43 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `town nutrition parameters, Stage 3/4 resident simulation, population sweep reports and plots`

## Completed

- Finished and verified the configurable four-channel nutrition integration in Stage 3/4, including resident timelines, severe-threshold events, and trailing-window population equilibrium fields.
- Fixed the resident Codec test setup and a one-resident Stage 3 housing fixture that had zero effective capacity.
- Corrected the default absolute child-to-adult threshold from `30` to `60` after the infant threshold became `30`; this restores a 30-day infant stage followed by a 30-day child stage.
- Generated the formal fixed-seed `120 days x 1000 trials x 20 populations` report at `build/reports/town-model/simulations/stage4-t1-population-sweep-nutrition-1000`.
- Generated and visually inspected all four resident plots under `build/reports/town-model/figures/stage4-resident-dynamics-nutrition-1000`.

## Decisions

- Kept the committed 20 explicit population points and `P=24` daily timeline so the expensive resident-level meal selection remains practical while preserving the requested 1..200 population curve.
- Treated a dead population as zero in the equilibrium report; the `P=1` zero line is therefore collapse, not a physiological equilibrium.
- Kept simulation inputs read-only from the companion pack and made no pack-repository changes.

## Validation

- `./gradlew compileJava compileTestJava --offline` passed.
- Focused nutrition, housing, policy, Stage 3, and Stage 4 tests passed (32 tests); `./gradlew test --offline` passed all 161 tests.
- Python simulation tests passed 19/20. The remaining legacy audit still reports the pre-existing `WorldTemperature` and `HouseBlockEntity` source-pattern drift.
- A 3-trial end-to-end sweep and plot smoke test passed before the formal run.
- The formal sweep completed in 8m57s and produced 20 population rows, 120,000 daily timeline rows, 24,000 initial residents, and 63,863 event rows. CSV widths were consistent and contained no NaN/Infinity.
- Visual inspection confirmed readable, non-overlapping titles, legends, axes, uncertainty bands, and subplot labels in all four final PNGs.

## Remaining

- Current meat-only baseline nutrition is not balanced: at `P=24`, all four channels cross the severe line on day 5; carbohydrate and vegetable settle at zero, fat near `0.3`, and protein near `1.8`.
- Use this run to tune food recipes or `nutritionReferencePerFoodUnit`; do not interpret the current low nutrition reserves as a desired equilibrium.
- The Mayor's Seal and nutrition UI still need the previously recorded in-game multi-client visual smoke tests.
