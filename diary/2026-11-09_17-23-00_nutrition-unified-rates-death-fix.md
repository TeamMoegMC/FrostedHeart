# Unified nutrition rates and death lifecycle fix

- Time: `2026-11-09 17:23:00 +0800`
- Author: `Codex; OpenAI GPT-6; implementation collaborator`
- Status: `completed`
- Scope: `Frosted-Heart player nutrition/config/menu; TheWinterRescue defaultconfigs/frostedheart-server.toml`

## Completed

- Followed the user's explicit choice to support unified nutrition-rate configurations only. Removed `nutritionScaleVersion`, `FHConfig.migrateNutritionScale`, and `NutritionScaleMigration`; rates are now read literally with source defaults `nutritionGainRate=1.0` and `nutritionConsumptionRate=0.25`.
- Preserved the latest implementation's correctly converted gameplay values. TheWinterRescue's legacy template `0.0003/0.0003` is now explicitly `0.12/0.03`, with no scale marker. These are the former one-time conversion results, not new balance choices.
- Left `PlayerNutritionState`, food profiles, eating/hunger formulas, upper bounds, maximum-health calculations, and illness thresholds unchanged. Player NBT's separate storage version remains to correctly read existing player data; resident-attribute migration also remains independent.
- Stopped nutrition consumption, punishment and nutrition-attribute updates for dead or removed players. Real exhaustion may still change vanilla hunger during death, but cannot drain nutrition through the health tick.
- Routed health-screen requests through `HealthScreenHandler`: resolve the current player by UUID; defer a dead player's request to its actual respawn; merge duplicate requests; clear abandoned requests on logout/server stop. Menus are invalid once their specific owner dies or is replaced.
- Added reusable `NutritionConfigGameTests` and `NutritionLifecycleGameTests` under `src/gametest/` and updated the [living nutrition contract](../docs/nutrition/nutrition-player-resident.md).
- Updated only the companion pack's nutrition-rate values and explanatory comments. The companion checkout had no applicable `AGENTS.md`; its README and `Development.md` were read, and related identifiers were checked across both repositories before editing.

## Decisions

- Use the same percentage-point units in source defaults, pack templates and runtime; no configuration compatibility layer, conversion flags, rate-size guesses, or newly introduced migration framework.
- Treat correct former runtime outputs as the balance baseline: source `1.0/0.25`, pack `0.12/0.03`. A numeric food-profile and hunger-loss comparison verifies those results directly.
- Keep prior investigation history and its old-behavior JSON unchanged. The new regression artifact describes the final simplified implementation.
- Existing worlds retain their own `serverconfig/frostedheart-server.toml`; changing a pack template does not override a world's customized rates or reconstruct already-lost player nutrition. The user identified the pack repository as the configuration target, not a live server save.

## Validation

- Incremental Java 17 production compilation passed; the final targeted Forge run compiles the changed production code and the two nutrition regression classes against the existing runtime.
- Full `compileGameTestJava` encountered unrelated existing errors in `DepositClientValidation` (`stateCounts(int[])` missing) and `DepositGameTests` (biome-holder generic mismatch). Nutrition regressions are independently compiled by the local `build/nutrition-fix.init.gradle`; no unrelated source was changed.
- `runGameTestServer -I build/nutrition-fix.init.gradle -PgameTestDirectory=build/nutrition-unified-pack-run -PgameTestNamespaces=frostedheart_nutrition_config,frostedheart_nutrition_lifecycle -x compileGameTestJava -x cleanResearchGameTestWorld --offline`: uses the exact updated companion template in a fresh world. Results are retained in [the regression artifact](../docs/nutrition/validation/nutrition-unified-rates-death-fix-2026-11-09.json).
- Config cases cover concurrent loading/reloading, four repeated disk reloads, source/pack/custom/zero/small literal rates, and remote in-memory configuration. Numeric controls cover food-profile gains, unchanged hunger-overflow handling, and per-hunger losses.
- Lifecycle cases cover one live hunger deduction without repeat, `200` dead ticks at consumption rates `0.25` and `25`, preservation of four `15` reserves after respawn, automatic deferred menu opening with encoded `1500`, resolving an old entity reference, and clearing an abandoned request on logout.
- Companion validation parses TOML, compares new rates with the previous template's successful conversion, and verifies every other parsed setting is unchanged. Git validation is reported separately for the two repositories.

## Remaining

- Physical-client rendering was not exercised; menu ownership, real respawn events, network menu creation and synchronized data-slot values were validated on the Forge server.
- Full GameTest compilation still has the unrelated deposit-test failures listed above.
- No live server deployment or player-save restoration was requested or performed.
