# Player nutrition death and configuration migration investigation

- Time: `2026-11-09 16:20:00 +0800`
- Author: `Codex; OpenAI GPT-6; investigation collaborator`
- Status: `completed`
- Scope: `player nutrition, death/respawn lifecycle, health menu, persistent server-config migration; investigation only`

## Completed

- Investigated the latest working source (`mod_version=0.7.13`, Minecraft `1.20.1`, Forge `47.3.0`) after a report of nutrition falling from `15%` to zero around death. The user confirmed use of the latest version but did not distinguish the death screen from post-respawn observation.
- Verified `HealthCommonEvents.onPlayerRespawn` copies the immutable state directly. Real `PlayerList.respawn` retained four `15` channels both immediately and after `200` player ticks of waiting. Current version-2 NBT also round-tripped `15` unchanged.
- Verified normal food loss is settled once: `20 -> 19` hunger changed every channel `15 -> 14.75`, and `200` subsequent ticks did not repeat that deduction.
- Verified pending exhaustion can still consume nutrition while dead: with zero saturation and `40` exhaustion, `9` hunger was lost and each channel became `12.75` at the normal `0.25` rate. Nutrition tick handling currently has no alive-state check.
- Verified a newly constructed menu for the removed dead entity uses default zero slots once its capability becomes unavailable at death tick `20`; the retained state remains `15`. A new post-respawn menu correctly encodes `1500`. Existing bound menus and a physical client's rendering were not tested by this observation.
- Found a real migration race: `onConfigLoading` and `onConfigReloading` can concurrently execute the unsynchronized version check/read/convert/write sequence in `migrateNutritionScale`. A natural fresh-world run saved `nutritionGainRate=400` instead of `1`. A separate observed fresh-world run logged `Loading` on `Server thread` and `Reloading` on `Thread-2` both entering at version `1`.
- Used a test-only timing barrier around reads of the real file-backed config, retaining production migration methods, to force one caller to pause after its version check while another completed. The resumed caller read the converted values and converted them again: consumption `0.0025 -> 0.25 -> 25`, gain `0.0025 -> 1 -> 400`, final version `2`. `NutritionCapability.consume(1)` then changed all four channels `15 -> 0`.
- Reproduced the full player path using that migrated rate: four channels started at `15`, hunger `20`, saturation `0`, exhaustion `4.1`; after death and one real player tick, hunger became `19` and all four channels became `0`. Real `PlayerList.respawn` preserved the zero values and produced maximum health `1`. This establishes a concrete death-associated failure path without claiming the reporting server has the raced configuration.
- Read the companion pack at `D:/TheWinterRescue`: its clean default template still uses legacy gain/consumption `0.0003/0.0003`, with no KubeJS nutrition/death writer found. Expected one-time migration is `0.12/0.03`; a second conversion gives `48/3`. No companion files were changed. No `AGENTS.md` was present in that checkout, and no pack modification was attempted.
- Updated the [living nutrition reference](../docs/nutrition/nutrition-player-resident.md) to document the confirmed lifecycle edges and unfixed migration race. Production source, gameplay balance and player saves were not modified.

## Decisions

- Treat the report as an investigation request. Preserve implementation and report a concrete reproduction, rather than changing death penalties or nutrition balance without evidence.
- Distinguish a dead entity's empty menu from actual loss. Distinguish a demonstrated bad migration schedule from proof that the reporting server has that configuration.
- Keep temporary GameTest observers, timing instrumentation, worlds and raw logs under ignored `build/nutrition-investigation/`; retain compact observations in [the validation record](../docs/nutrition/validation/player-death-and-config-migration-2026-11-09.json).
- Do not infer current percentage NBT from unversioned low numbers. A deliberately stripped version converts `15` to `0.15` as documented, but no normal death path removes the version or reloads nutrition NBT.

## Validation

- Java 17, incremental compilation using the existing Forge runtime; no checkout copy, second production build, or path-dependent artifact hash.
- `compileNutritionInvestigation -I build/nutrition-investigation/run.init.gradle --offline`: passed.
- `runGameTestServer -I build/nutrition-investigation/run.init.gradle -PgameTestDirectory=build/nutrition-investigation/config-run -PgameTestNamespaces=frostedheart_nutrition_investigation -x compileGameTestJava -x cleanResearchGameTestWorld --offline`: one required GameTest passed, covering six real player lifecycle scenarios and current/legacy NBT diagnostics.
- Forty unscheduled simultaneous pairs of migration calls did not reproduce repeated conversion. This is a timing-sensitive defect, not a failure-rate estimate.
- `runGameTestServer -I build/nutrition-investigation/run.init.gradle -PgameTestDirectory=build/nutrition-investigation/config-run -PgameTestNamespaces=frostedheart_nutrition_race -PnutritionControlledRace=true -x compileGameTestJava -x cleanResearchGameTestWorld --offline`: the required controlled-interleaving GameTest passed, proving the repeated conversion, actual nutrition deduction and complete real death/respawn path. The isolated test temporarily disabled its file watch to control the two production entrypoint calls and restored the original Forge watcher for teardown; it did not modify production logic.
- Reviewed vanilla/Forge `Player.tick`, `FoodData.tick`, `ServerPlayer.doTick`, `ForgeConfigSpec.ConfigValue`, and `ConfigFileTypeHandler.ConfigWatcher` bytecode alongside current project source.

## Remaining

- The reporting server's live world config and its player observations are unavailable; the specific incident is not assigned to a cause solely from the latest-version answer.
- The migration race, dead-player settlement and unavailable-capability menu behavior remain unfixed. A repair should make version/rate conversion coherent under loading and reload, preserve deliberate pack tuning, and handle menu/player lifecycle consistently.
- Physical-client reproduction of opening the menu for a removed dead entity remains unverified.
