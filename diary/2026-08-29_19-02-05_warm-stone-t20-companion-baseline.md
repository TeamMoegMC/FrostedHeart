# Warm stone T20 companion baseline

- Time: `2026-08-29 19:02:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `TheWinterRescue constraints, existing IDs, recipe inputs, and two-repository state before C3 pack content`

## Completed

- Confirmed that `TheWinterRescue` has no `AGENTS.md` at its root or elsewhere in its worktree; read its applicable `kubejs/CONTRIBUTING.md` before any pack-content modification.
- Rechecked FrostedHeart registrations and accepted T00-T19 contracts for `frostedheart:warm_stone`, `frostedheart:hot_water_bag`, `frostedheart:thermal_reservoir`, and the one-slot `warm_stone` Curios type.
- Located companion recipe, research, quest, and Create-tooltip entry points. No pre-existing warm-stone, hot-water-bag, thermal-reservoir, or same-purpose Curios identifier exists there.
- Confirmed that `caupona:nail_soup` is the existing Hot Water input produced from water by existing campfire/smoking recipes and by Create mixing. It is the only approved explicit thermal input for the optional hot-water-bag filling path.

## Decisions

- Preserve the existing user-staged `kubejs/server_scripts/src/recipes/shaped/new.js` change and untracked `.workbuddy/` in TheWinterRescue.
- Do not add a campfire recipe for either reservoir; dropped-item heating remains the accepted generic physical-source receiver path.
- Do not add or describe a warm-stone charger recipe. The existing charger schema and unrelated activated-carbon recipe do not authorize a reservoir cost.

## Validation

- Both repository state scans and relevant-ID scans completed.
- `git diff --check` passed in FrostedHeart; both working-tree and staged `git diff --check` passed in TheWinterRescue.
- JDK `17.0.2` at `C:\Program Files\Java\jdk-17.0.2` and Node `v24.19.0` are available for subsequent verification.

## Remaining

- T21: add normal acquisition recipes and the real-hot-water-only filling recipe.
- T22: add research, quest, and Create-style tooltip content; do not begin T23.
