# Real player eating and nutrition maintenance investigation

- Time: `2026-11-09 21:58:10 +0800`
- Author: `Codex; OpenAI GPT-6; investigation collaborator`
- Status: `completed`
- Scope: `actual item consumption, static/dynamic food profiles, nutrition gain and maintenance, low-reserve maximum health`

## Completed

- Followed up on reports that eating provided no nutrition, reserves fell rapidly and maximum health stayed very low. Retained the user's explicit instruction to preserve current gameplay values.
- Added reusable `NutritionEatingGameTests` using connected real `ServerPlayer` instances, actual registered items and food recipes, `Item.use`, player ticks and the production Forge Finish subscribers; no nutrition event was fabricated or invoked directly.
- Verified `11` consumption cases: final and stacked cooked beef; beef/carrot with only one missing hunger; bread, apple and baked potato; golden apple at full hunger; beef at zero reserves; a real Caupona meat-soup fixture in a bowl and in the last `250 mB` thermos serving.
- Confirmed pack-rate gains: full beef protein `15 -> 15.576`; bread carbohydrate `15 -> 15.3`; apple vegetable `15 -> 15.288`; baked potato carbohydrate/vegetable `15 -> 15.24/15.12`; the meat-soup fixture protein `15 -> 15.6912` from both bowl and thermos. All restored the expected real hunger. Full-hunger golden apple correctly provided zero nutrition under the existing formula.
- Verified an actual full hunger/meal cycle: four reserves `70`; lose `8` hunger through vanilla exhaustion; eat beef to restore it. Final protein was about `70.336`, other channels about `69.76`; the aggregate reserve loss was about `0.384`.
- Analyzed all `110` generated static profiles: `107` have a total below `100`; only cake, cooked fat and Supplementaries pancake reach `100`, and none of those three provides vegetables. At gain/loss `0.12/0.03`, aggregate maintenance requires profile total at least `100` before bounds. The present static-food model therefore cannot maintain all four positive reserves indefinitely. Dynamic Caupona food must be evaluated separately.
- Verified low-stock recovery: from four zero reserves, one beef raises protein to `0.576`, but an explicit maximum-health recalculation still yields `1`. A functioning eating event does not immediately restore a previously depleted player's health capacity.
- Updated the [living nutrition reference](../docs/nutrition/nutrition-player-resident.md) with the actual eating results, maintenance constraint, scope and low-health behavior. Saved [compact evidence](../docs/nutrition/validation/player-real-eating-balance-2026-11-09.json).

## Decisions

- The prior configuration/death fix remains valid, but does not prove the entire nutrition economy is healthy. Report actual consumption behavior separately from food/consumption balance and persistent depleted reserves.
- Do not retune acquisition, consumption, food profiles, maximum health or old player saves without a new user decision; the user expressly requested unchanged current logic values.
- Keep all prior diary entries and validation records intact. Production nutrition code/data and the companion pack's already-unified `0.12/0.03` template were not changed by this investigation.

## Validation

- Java 17 targeted compile and real Forge server: `runGameTestServer -I build/nutrition-eating.init.gradle -PgameTestDirectory=build/nutrition-eating-run -PgameTestNamespaces=frostedheart_nutrition_eating -x compileGameTestJava -x cleanResearchGameTestWorld --offline`: `1` required GameTest passed, comprising `11` real use cases and `1` real hunger/meal cycle.
- The run loads the exact Frosted Heart and Caupona server templates from the companion repository. It tests a legitimate cooked-beef soup NBT fixture with healing `8`, not every possible cooking recipe or the entire pack's KubeJS environment.
- An initial launch was blocked by an unrelated incomplete `IceCaveCrystalBoundaryGameTests$1` class in the shared GameTest output. The targeted run now exposes only its own test classes and omits unrelated thermal test-only observers, following the existing targeted-run pattern. No unrelated source was edited.
- Inspected Forge `LivingEntity.completeUsingItem` bytecode: its Finish event receives a copy made before the consumed stack is reduced, so the final ordinary food item is not lost from this event. Actual final-item tests confirm the result.
- Static JSON analysis, whitespace/document link checks and the preservation of production nutrition formulas/data were verified separately from runtime observations.

## Remaining

- The existing fixed four-channel consumption and ordinary static-food profiles still have a maintenance deficit; no balance adjustment was authorized in this follow-up.
- A deployed server's actual world config and old player data were unavailable. Existing world overrides and historical nutrient losses are not recovered by a new default template.
- Physical-client rendering and every food in the full modpack were not tested.
