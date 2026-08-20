# House-priority meals and daily menu UI

- Time: `2026-08-20 21:10:39 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `town housing meal priority, house daily persistence, house UI`

## Completed

- Replaced the one-composition town menu call with priority-ordered house groups while preserving centralized two-pass ration amounts.
- Made earlier `TownHousingPlan` entries reserve high-tier inventory before later houses and score each menu against only that house's resident nutrition deficits.
- Persisted the actual consumed item/NBT and fractional amount in `HouseBuilding.DailyMeal`, using authoritative resource-executor results rather than planned amounts.
- Added a “Today's Meal” house tab with item-grid rendering, quantity overlays, scrolling, vanilla tooltips, settlement day, and distinct never-settled/unserved states.
- Updated the nutrition and town living docs and appended the revised decision to the nutrition discussion.

## Decisions

- Housing priority controls meal quality and composition; ration guarantees and town-wide second-pass sharing control quantity.
- Residents in one house share composition and scale only by their existing allocation. Different houses may receive different menus.
- `nutritionQuality` remains absent. Actual item stacks are the player-facing explanation of food quality.
- Existing building Codec and synchronization own the latest menu; no separate packet or meal-history system was added.

## Validation

- `./gradlew test`: `207` tests, `0` failures, `0` errors.
- Priority-menu tests verify that the earlier house consumes scarce level-4 food and the later house falls back to level 3.
- Daily-meal Codec tests preserve NBT and `0.3125` item amounts and distinguish no snapshot from an empty served day.
- Modified language/scenario JSON parsing, static old-contract searches, and `git diff --check` passed.

## Remaining

- Smoke-test the new house meal tab in the real client with many menu entries and a Caupona item whose identity depends on NBT.
- Stage 3/4 currently represent one abstract house and therefore do not report multi-house food-quality stratification.
