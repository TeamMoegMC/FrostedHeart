# Rubbing tool team binding

- Author: Codex
- Date: 2026-11-22
- Added team UUID binding alongside research and checks before collection/submission.
- Empty legacy tools may rebind; positive legacy points retain data but cannot be assigned to an arbitrary team.
- Updated gameplay documentation and added real GameTest coverage for binding, NBT round trip, cross-team denial, same-team credit and legacy handling.
- Validation completed below.

## Validation follow-up

- Author: Codex; verified 2026-11-22.
- Ran the following as two independent commands, each with its own redirected log:
  - `./gradlew --offline validateResearchCatalog > build/rubbing-team-catalog-validation.log 2>&1`: BUILD SUCCESSFUL; 86 research definitions validated.
  - `./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedresearch -PthermalTestProcessors=4 > build/rubbing-team-validation-retry.log 2>&1`: BUILD SUCCESSFUL; all 2 required tests passed, including the rubbing binding checks inside `mechanicalCalculatorClaimsAndEnforcesTeamOwner` and `ExperimentSpaceGameTests.enclosedRoomCountsAirAndWallsAndAnOpeningFailsIt`.
- Previous run (`build/rubbing-team-validation.log`) failed only the room test's assertion at `ExperimentSpaceGameTests.java:62`: expected `space.open` with `ok=false` after removing one roof plank, but received `space.enclosed`, volume 3091, and 4/5 planks. The closed-room assertion had already passed. The rubbing/calculator test was not the failed test.
- Room path: `ExperimentSpaceGameTests` -> `ExperimentConditions.space` -> `RoomScanner` -> `ConfinedSpaceScanner` / `AbstractBlockScanner`. It scans level air/blocks; it does not call `RubbingTool`, `ResearchHooks.submitItem`, or team binding/ownership checks. None of these room/scanner source files or the room test were changed by this task.
- The fixture uses the 3x3x3 `empty.snbt` template and removes only the local roof at (1,2,1), without clearing the column above to build height. `ConfinedSpaceScanner.getAirsAboveLong` stops at non-air above; removing that local roof does not guarantee open sky outside the template. External geometry is a plausible cause of the earlier enclosed volume, not directly proven by the log.
- No source or test edits occurred between the failed run and successful retry. The room failure therefore did not reproduce with identical rubbing changes; the dependency inspection provides no connection to rubbing team logic. A deterministic fixture/world investigation remains separate work; no room behavior or assertions were changed to obtain a pass.
- `git diff --check`: passed. Runtime logs are generated artifacts, not tracked files.
