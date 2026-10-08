# Restore legacy clothing exchange in the player energy model

- Time: `2026-09-23 18:50:58 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation collaborator`
- Status: `completed`
- Scope: `BodyPartData`, `PartClothData`, `PlayerThermalModel`, player climate documentation, Forge GameTest

## Completed

- Kept all existing armor insulation recipes, attributes, slots, and layer weights. The weighted legacy insulation score again multiplies ambient exchange by `100 / (100 + score)` in the new player energy model.
- Applied the factor to passive air, water, powder-snow, lava, and Wet conductances. Split direct source radiation from ambient exchange so a warm campfire still adds heat through clothing; radiant heat proof remains effective.
- Added a production-path Forge GameTest using registered jackets, a real server player and world field, real body updates, and an ignited campfire. Updated the living player-temperature documentation.

## Decisions

- Reproduce the old clothing's relative reduction of environmental exchange. The old weather, physiology, and absolute update cadence are different and were not restored.
- Retain the current water/wind/radiant proof inputs and fixed tissue resistance. No recipe, datapack, or companion repository data changes were made.
- At the user's direction, remove the attempted JUnit test and validate through the actual Forge GameTest server. The JUnit task was interrupted before tests ran.

## Validation

- Java 17 `runGameTestServer --offline --no-daemon --max-workers=1 --console=plain` started and completed 103 Forge GameTests. The new clothing/campfire production scenario passed.
- The full task exited 4 because four other required tests failed: `sensibleairandmaterialusethesameclockwithoutsavedrift` (`only v4 is written`), `minecraftresidencyhandoffheatshalfairneighbor` (`pre-existing campfires were not discovered`), `campfiresurfaceusesmaterialandlocalavailability` (`both source Pages must be readable`), and `livematerialswinandstoredbrickscanberemovedwithinthesamepage` (`solver=NaN`). These test other thermal runtime and material paths; this work did not change those paths.
- `git diff --check` passed.

## Remaining

- The four other failing Forge GameTests need separate diagnosis before claiming a fully green suite.
- A local client play session with the companion modpack was not performed; compare cold-weather play time and health-screen trend in normal gameplay before treating the restored clothing curve as final balance.
