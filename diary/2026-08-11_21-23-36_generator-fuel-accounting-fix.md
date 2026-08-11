# Generator fuel accounting fixes

- Time: `2026-08-11 21:23:36 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `shared generator fuel model, GeneratorData refill behavior, stage-0 audit and town-model documentation`

## Completed

- Changed generator refill accounting from replacement to a carried process-tick balance. An exact balance is consumed before another fuel item loads, a partial tail is added to the next item's duration, and a failed refill no longer clears the remaining balance.
- Changed effective fuel duration to explicit decimal multiplication followed by `FLOOR`, fixing cumulative efficiency level 2 from `1439/2879` to `1440/2880` coal/coke process ticks.
- Updated the stage-0 audit and `docs/town-model.md`: current 20-tick T1 rates now equal the analytical rates, and the two resolved warnings are no longer emitted.
- Added regression coverage for exact refill boundaries, partial balance carry, decimal rounding, and equivalence between 1-tick and 20-tick updates across several fuel durations.

## Decisions

- Preserve the existing round-down design rule instead of switching all fractional durations to nearest-integer rounding.
- Keep the fix in the shared `GeneratorFuelModel` and `GeneratorData`; T1 and T2 use the same fuel accounting code, so T2 also receives the generic correctness fix even though its heat-network model remains outside stage 0.
- Keep analytical and `currentTownBatch` audit metrics as separate regression signals; after the fix they must be equal.

## Validation

- `./gradlew test` — successful; 19 tests, zero failures/errors.
- `runTownSimulation audit` against the current TWR development instance — successful; no-research current T1 rates are `21.4285714 coal/day` and `10.7142857 coke/day`, level-2 durations are `1440/2880`, and `issues` is empty.
- `git diff --check` — clean.

## Remaining

- No in-game manual smoke test was performed. Stage 1 should retain the integer process-tick balance in `TownState` rather than rounding the long-run daily rate.
