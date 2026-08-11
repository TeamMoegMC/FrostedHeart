# Town resident, housing and work parameter integration

- Time: `2026-08-11 22:29:01 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town model parameters, FHConfig, resident/house/work formulas, stage-0 audit`

## Completed

- Expanded `TownModelParameters` with resident rules/aging, housing settlement/structure/decoration, common building temperature/space scoring, mining/hunting work rules, and ore/HUNT terrain-resource records.
- Moved all 105 in-scope FH defaults to `TownModelParameters.Defaults`; every matching `FHConfig` declaration now references that table. `GameUnits.GAME_TICKS_PER_DAY` remains the sole non-configurable Minecraft unit constant.
- Added `BUILDING_SCORING` and `RESIDENT_RULES` config sections and extracted the house structure/capacity/rating and resident morning/work-eligibility hardcodes without changing their defaults.
- Parameterized `TownMathFunctions` building ratings. Added `ResidentDailyModel` so gameplay and later simulation share morning homeless/removal and work-eligibility formulas.
- Made resident proficiency storage maximum configurable and applied it consistently to normalization, growth calculation and UI displays.
- Expanded `TownStageZeroAudit` to emit exact `Defaults -> FHConfig` provenance for the new parameters and updated `docs/town-model.md` and the town package README.

## Decisions

- Climate, refugee/weather population inflow, heaters and T2/legacy building heat endpoints remain excluded until their dedicated phases.
- Only ore and HUNT terrain resources enter this closed-loop model; tree, POI and salvage resources remain outside scope.
- Resident random recruitment/attribute-generation distributions remain outside the first fixed-population simulation. Aging parameters are snapshotted now because they already affect resident runtime state, but stage 1 need not execute aging.
- Runtime gameplay continues to read only `FHConfig`; the simulator continues to build defaults only through `TownModelParameters.currentDefaults()`.

## Validation

- `./gradlew test` — successful, zero failures.
- `runTownSimulation audit` against the current TWR development instance — successful; snapshot `050aec23631474c66f3a9f52af02c4c6844c7e04b9df2cbf211e9c3b65acfa2b`.
- Final source snapshot contains 155 inputs: 105 `shared-default`, one `minecraft-unit`, and 49 external/derived data inputs. Existing stage-0 numerical baselines are unchanged.
- `git diff --check` — clean.

## Remaining

- No in-game manual smoke test was performed. The next implementation step is stage 1 deterministic T1/mining production using these records; climate and T2 stay deferred.
