# Heavy Load no longer crashes on the first weight crossing

- Time: `2026-11-04 17:50:16 +02:00`
- Author: `empireu; Anthropic Claude Opus 5.5; outside contributor`
- Status: `completed`
- Scope: `PlayerTemperatureComputation.updatePlayer`, `effect.frostedheart.heavy_load` lang, `ThermalLoadedWorldGameTests`

## Completed

- Fixed the server crash when total clothing weight first passes 10. `updatePlayer` read the `HEAVY_LOAD` amplifier without checking that the player had the effect, and that null pointer came before the only code that adds it. Since `af6baeca1` the effect could never apply, and from the code a saved overweight player should crash again on joining (not tested before the fix).
- Added the missing effect names: `Heavy Load` and `超重` ("overweight").
- Added the GameTest `heavyLoadFollowsLiningWeightFromTheFirstCrossing`. It covers staying under the limit, the first crossing, moving up and down a level, clearing the effect, and restoring movement speed.
- Documentation impact: none. No living document describes Heavy Load.

## Decisions

- Kept the existing limit and penalty, and only removed the crash. Players will now see the effect for the first time: 15% less movement speed at the first level, and another 15% for every 5 weight above 10.
- The Chinese name is a contributor's choice, so maintainers should confirm it.

## Validation

- Reproduced in `runClient` before the fix: three fox jackets in the torso lining (weight 10.8) crashed with the null pointer at `PlayerTemperatureComputation.java:174`.
- `runGameTestServer -PgameTestNamespaces=frostedheart` ran 60 tests. Before the fix, the new test failed with the same null pointer. After the fix it passes, and 58 of 60 pass.
- The same 2 tests fail in both runs, and neither involves clothing: `sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift` asserts save version 5 while `DormantChunkThermalState.FORMAT_VERSION` is 6, and `campfireSurfaceUsesMaterialAndLocalAvailability` fails with "closed publication must commit a field-only full", which was not investigated. These runs included uncommitted polar bear tags.
- After the fix, in `runClient`: two worlds saved with overweight linings opened without crashing. Three fox jackets in the torso lining applied Heavy Load, and taking them off cleared it. Filling the legs lining as well (weight 19.2) raised it to level II. The session log has no null pointer and no new crash report.

## Remaining

- The two thermal GameTest failures above remain.
- Not tested in the full pack or on a dedicated server.
