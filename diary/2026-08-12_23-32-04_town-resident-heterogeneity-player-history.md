# Town resident heterogeneity and player-first histories

- Time: `2026-08-12 23:32:04 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `resident generation/config, stage-3/4 initialization, population sweep output, player-facing plots, tests, and town-model documentation`

## Completed

- Added the pure `ResidentGenerationModel` and extracted current recruitment age weights, age-day ranges, age-specific attribute distributions, initial proficiency distributions, ordinary health/mental values, fallback weights, and cold-survivor modifiers into `TownModelParameters.Defaults`.
- Added `FHConfig.SERVER.TOWN.RESIDENT_GENERATION`; gameplay continues to read config while the simulator reads `TownModelParameters`. Existing refugee weights now also take their defaults from the shared table, and stage-0 audit lists every added parameter.
- Added scenario-selectable `fixed` versus `gameGenerated` population initialization. Stage 4 now uses the exact seeded gameplay distribution and retains the existing shared daily aging model.
- Replaced the 100-point population curve with 20 explicit points while retaining critical populations around 13. Captured all 1,000 trial/day states and events for P=24 plus every initial resident.
- Added player-first town history, event raster, and initial resident distribution figures before the existing aggregate population diagnostics.

## Decisions

- No independent simulator noise was introduced. Ordinary recruits all retain current-code health/mental 50; heterogeneity comes only from current age, strength, intelligence, proficiency, and aging rules.
- A concrete timeline selects the smallest trial whose first-exit day is closest to the median among exit runs; if no run exits, it uses the adverse-event-count median. The full raster always displays all trials.
- Detailed trial output is limited to one configurable population to keep the 20-point × 1,000-seed run fast and files manageable.

## Validation

- `./gradlew test` — all tests passed, including resident generation reproducibility, age weights/bounds, stage-4 generated-population replay, observations, and existing model regressions.
- Stage-0 CLI audit succeeded and emitted all `residents.generation.*` values in `build/town-model/audit-resident-generation/source-snapshot.json`.
- Formal `20 population point × 1,000 paired seed × 120 day` Java run completed in about 73 seconds; plotting in Conda `standard` generated all 13 PNGs at 320 dpi.
- P=24 initialized 24,000 residents with observed age fractions `0.09871/0.20054/0.59692/0.10383`, close to configured `0.1/0.2/0.6/0.1`.
- P=24 exit probability is `0.149`; all exit runs warned earlier with P50 lead `15 days`. Current shared health stress still synchronizes all 24 exits within an affected run, a gameplay-model finding rather than a plotting artifact.
- `git diff --check` passed.

## Remaining

- Mayor's Seal UI remains unchanged; this task only establishes shared signals, simulation output, and presentation evidence.
- Current resident attribute heterogeneity changes labor but does not desynchronize health/mental because ordinary recruits start at identical values and share one house settlement. Any future individual resilience must be a deliberate gameplay model, not simulator-only noise.
- T2, heaters, heat inertia, and later model stages remain untouched.
