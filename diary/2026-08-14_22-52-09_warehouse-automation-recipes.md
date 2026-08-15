# Warehouse automation recipes

- Time: `2026-08-14 22:52:09 +0800`
- Author: `Codex; OpenAI GPT-5; primary agent /root`
- Status: `completed`
- Scope: `TWR-1.20-Dev/.minecraft/kubejs/server_scripts/src/recipes/shaped/new.js`

## Completed

- Added shaped KubeJS recipes for `frostedheart:warehouse_interface` and `frostedheart:warehouse_level_emitter` in the companion modpack repository.
- Appended both recipes to the shaped-recipe array so existing index-derived recipe IDs remain stable.

## Decisions

- The warehouse interface uses iron plates, treated wood, brass funnels, a hopper, and a servo core to reflect its item-transfer and automatic-restocking role.
- The warehouse level emitter uses iron plates, copper wire, redstone torches, a comparator, and an electron tube to reflect its stock-sensing and redstone-output role.
- Both recipes produce one block and use the existing indexed recipe-registration convention in `new.js`; appending them avoids renumbering older recipes.

## Validation

- `node --check kubejs/server_scripts/src/recipes/shaped/new.js` completed successfully.
- The changed recipe script passes `git diff --check`; all referenced Frosted Heart output IDs match their registrations.
- Full companion-repository `git diff --check` is affected only by pre-existing trailing whitespace in the user's modified `config/frostedheart-client.toml`.

## Remaining

- In-game JEI/reload verification is recommended when running the updated Frosted Heart build.
