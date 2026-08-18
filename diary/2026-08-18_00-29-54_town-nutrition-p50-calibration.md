# Town nutrition P50 calibration

- Time: `2026-08-18 00:29:54 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Stage 3/4 settlement order, nutrition calibration overrides, supplemental-food simulation, P50 comparison`

## Completed

- Corrected the Stage 3/4 daily order to decay nutrition, assign the pre-treatment work roster, produce and process resources, add explicit scenario supplies, settle the evening meal and housing recovery, age residents, then apply homeless/removal settlement.
- Added command-line-only population-sweep overrides for nutrition reference density, reserve loss, and reference gain. They are captured in `summary.json` and do not modify gameplay defaults.
- Added scenario-declared simulation foods and daily warehouse supplies. Food level and nutrition are read from existing tags/generated recipes; daily supply is population-scaled, capacity-limited, and recorded as `external_supply`.
- Fixed meal utility so health/mental condition demand only amplifies carbohydrate/vegetable selection while the corresponding projected nutrition gap remains. This prevents all meal chunks from stacking one food after its nutrition channel is already filled.
- Added the P50 hunting-only and baked-potato scenarios plus a four-way comparison plotting script.

## Decisions

- Did not edit any diet recipe or companion-pack file.
- Did not promote the successful experimental values to `TownModelParameters.Defaults` / `FHConfig`: the tested candidate remains `reference=200`, `loss=1`, `gain=2` pending a broader population scan and an in-game food-source decision.
- Modeled the carbohydrate/vegetable source as 1 baked potato per day at the eight-resident scenario basis, scaling to 6.25 per day for 50 residents. This is an explicit external logistics input, not hunting output or endogenous farming.

## Validation

- Focused settlement, food-loading, scaling, and nutrition override tests passed.
- `./gradlew test --offline` passed all 165 tests.
- Python simulation tests passed 19/20. The remaining failure is the pre-existing source-audit drift for `WorldTemperature` and `HouseBlockEntity`.
- Four principal `50 residents x 120 days x 100 trials` comparisons and intermediate calibration runs completed successfully.
- The final candidate had 100% survival and no food shortage. Its last-30-day medians were health `51.50`, mental `76.50`, fat `70.06`, carbohydrate `77.62`, protein `99.88`, and vegetable `74.89`; all four severe counts were zero on day 119 across all trials.
- Plot scripts compiled; scenarios parsed; `git diff --check` passed; final comparison PNGs were visually inspected.

## Remaining

- Decide which real gameplay system supplies carbohydrate/vegetable foods; pure hunting still reaches full carbohydrate and vegetable deficiency even with calibrated pacing.
- Before changing defaults, run the candidate across multiple population/supply points and check that protein near 100 is acceptable rather than evidence that a single shared reference should become per-channel.
- The Mayor's Seal and nutrition UI still need in-game multi-client visual smoke tests.
