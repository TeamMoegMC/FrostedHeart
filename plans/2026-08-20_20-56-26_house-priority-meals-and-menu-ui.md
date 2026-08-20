# 住宅优先供餐与今日餐食展示

- Time: `2026-08-20 20:56:26 +0800`
- Authors: `wyc; system designer`, `Codex; OpenAI implementation collaborator`
- Status: `completed`
- Scope: `town housing meal allocation, house daily persistence, house UI, town simulation documentation`
- Related: [`docs/nutrition/nutrition-player-resident.md`](../docs/nutrition/nutrition-player-resident.md), [`docs/town/town-model.md`](../docs/town/town-model.md), [`discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md`](../discussion/2026-08-19_15-02-51_nutrition-redesign-boundaries.md)

## Goal

Restore the intended policy meaning that earlier houses in `TownHousingPlan` receive first access to better food, without returning to resident-by-resident menu planning. Each house receives one shared menu, residents scale that composition by their existing ration allocation, and players can inspect the actual item stacks consumed by that house in the latest daily settlement.

## Verified Current State

- `TownFoodAllocationModel` already assigns guaranteed rations in housing-plan order and then shares remaining quantity town-wide.
- `TownHousingMealService` currently generates one town-wide menu, so housing priority affects quantity but not food composition.
- `ResidentPublicMenuModel` can plan a menu for any recipient group; its current town-wide scope is imposed by the caller.
- `HouseBuilding.DailyReport` is persisted and synchronized through the existing town building snapshot, while `HouseMenu` deliberately adds no separate network layer.
- Stage 3/4 currently model one abstract house, so using the same menu model once remains equivalent there.

## Decisions

- Determine resident ration allocations town-wide exactly as before.
- Iterate occupied houses in `TownHousingPlan` order. For each house, plan up to the configured number of selection chunks against the remaining item inventory, execute the costs immediately, and let later houses see only the remainder.
- Within one house, score nutrition against that house's residents. All residents receive the same actual menu composition scaled by their allocated food-resource share.
- Persist the latest actual house menu as item identity plus a finite non-negative `double` amount. Preserve item NBT and derive the display `ItemStack` from `ItemStackResourceKey`.
- Record resource-executor `modifiedAmount`, not the planner request, so the UI and nutrition settlement describe actual warehouse consumption.
- Add a third house-screen tab that renders item icons, exact decimal amounts, and vanilla tooltips. Distinguish no prior settlement from a settled day with no food.
- Store only the latest settlement menu; long-term food history is out of scope.

## Steps

1. Make the pure menu model describe one meal recipient group rather than one town.
2. Refactor `TownHousingMealService` to plan and consume one menu per house in priority order, distribute its actual result within that house, and persist menu entries in `HouseBuilding.DailyReport`.
3. Add the house meal tab and localized labels.
4. Cover priority depletion, common within-house composition, actual consumed amounts, menu Codec behavior, and empty states.
5. Update living documentation and discussion, run Java/JSON/diff validation, then record the result in this plan and a diary entry.

## Validation

- With insufficient level-4 food, the earlier house consumes it and a later house falls back to level 3.
- Residents in one house receive the same composition but retain distinct full/partial ration amounts.
- Different houses can receive different item compositions and nutrition profiles.
- Persisted menu entries retain item NBT and actual fractional amounts through Codec round-trip.
- Empty, unfed, and never-settled houses present distinct UI states.
- Stage 3/4 continue using the same pure selection and nutrition formulas.

## Documentation Impact

- Replace the obsolete town-wide common-composition contract in nutrition and town living docs.
- Append the revised housing-priority decision to the existing nutrition discussion.
- Add a completion diary entry and this plan's final outcome.

## Outcome

Completed on `2026-08-20 21:10:39 +0800`.

- Added `ResidentPublicMenuModel.planInPriorityOrder`; each house is now a recipient group and consumes planning inventory before the next house is evaluated.
- Kept the existing two-pass resident ration allocation. `TownHousingMealService` distributes each house's actual menu by resident allowance share and uses resource-executor `modifiedAmount` for nutrition, totals, and reporting.
- Added `HouseBuilding.DailyMeal` and `MealEntry` persistence with stable item/NBT identity, finite non-negative fractional amounts, explicit empty-served days, and settlement-day identity.
- Added the read-only “Today's Meal” house tab with item icons, rounded integer count overlays, exact fractional amounts in tooltips, scrolling, and vanilla item tooltips. Existing town building synchronization carries the data.
- Updated nutrition and town living docs and appended the superseding decision to the original discussion.

Validation completed with `207` Java tests, `0` failures and `0` errors. Focused tests cover priority inventory depletion, house-internal common composition, menu NBT/fractional Codec round-trip, and empty-state distinctions. Modified JSON parsing, semantic invariant searches, and `git diff --check` passed. Stage 3/4 were not rerun because their current single abstract house still executes one unchanged menu-planning pass; they do not yet model multi-house quality stratification.
