# Warm stone T21 recipes

- Time: `2026-08-29 19:07:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `TheWinterRescue acquisition recipes and real-hot-water-only hot-water-bag filling`

## Completed

- Added `kubejs/server_scripts/src/recipes/warm_stone.js` in TheWinterRescue with separate shaped recipes for `frostedheart:warm_stone` and `frostedheart:hot_water_bag`.
- Added a shapeless filling recipe which consumes the existing 250 mB `caupona:nail_soup` wooden cup, returns an empty wooden cup, and writes a version-one `frostedheart:thermal_reservoir` with both nodes at `60 degC`.
- Updated `docs/climate/player-temperature.md` with the two normal entries, the exact hot-water input, initial state, and the continuing generic dropped-item heat-source path.

## Decisions

- Normal acquisition never initializes either thermal node; only verified Hot Water authorizes the explicit `60 degC` state.
- No Campfire recipe was added. Campfire charging remains the accepted common ItemEntity physical-source receiver behavior.
- No `frostedheart:charger` recipe or cost was added or implied.

## Validation

- `node --check kubejs/server_scripts/src/recipes/warm_stone.js` passed.
- A static contract assertion confirmed both item IDs, the version-one thermal compound, exact Hot Water input, two `60.0` node values, empty-cup return, and absence of `charger`/`campfire` recipe IDs.
- `git diff --check` passed in FrostedHeart; `git diff --check` and `git diff --cached --check` passed in TheWinterRescue.

## Remaining

- T22: integrate research, quest, and Create-style tooltip content without suggesting an available charger.
- Do not begin T23.
