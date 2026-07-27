# Config-driven house daily model and resident GUI

- Time: `2026-07-27 19:49:14 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town house work settlement, housing config, daily observations, house menu/screen, localization`

## Completed

- Replaced the old house shortage/buff branches and duplicate food consumption with one daily settlement driven by `FHConfig.SERVER.TOWN.HOUSING`.
- Added explicit per-resident-day config names for food consumption, zero-food health/mental loss, maximum recovery, nutrition scaling, and normalized comfort weights; kept the temperature curve and thresholds hardcoded.
- Added a persisted `HouseBuilding.DailyReport` containing the last settlement's resident count, food, nutrition, effective temperature, component ratings, and overall comfort.
- Added a two-tab house GUI: the overview displays the last settlement, while the resident panel supports selection and shows four attributes, mining/hunting proficiency, and a clearly labeled next-day forecast.
- Registered `HouseMenu` / `HouseScreen`, changed house interaction to open the GUI, and added English/Chinese strings.
- Reused the existing every-tick full `TeamTownData` synchronization; no house packet or incremental synchronization path was added.

## Decisions

- Food is consumed once per house workday with `MAXIMIZE` and descending food level. Food satisfaction uses actual resource units consumed; nutrition quality uses nutrition per consumed resource unit.
- Health recovery depends on temperature, while mental recovery depends on the normalized temperature/space/decoration comfort score. Strength and intelligence are not changed by housing.
- Resident lookup in the client GUI filters the synced residents by `housePos`, which also works with legacy house data that did not encode its resident UUID collection.
- The resident delta is a forecast using current post-settlement attributes under the last observed house conditions, not a claim about the previous day's exact delta.

## Validation

- `./gradlew compileJava --offline` completed successfully with the repository's existing 20 deprecation warnings.
- `./gradlew build --offline` completed successfully; resource processing, packaging, checks, and the repository's non-fatal license report ran.
- Pure-model probes verified half food satisfaction `0.5`, half nutrition quality `0.5`, weighted comfort `0.55`, perfect-condition deltas `health +1.0 / mental +0.75`, and half-food deltas `health -3.5 / mental -2.125`.
- Both localization JSON files parse with `jq`; `git diff --check` passes.

## Remaining

- Perform an in-game visual pass at multiple GUI scales and with more than eleven residents to tune clipping or spacing if needed.
