# Bundled Phase 2 geology topic and recipes

- Time: `2026-08-26 16:07:27 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Main-mod datapack distribution for the Phase 2 topic, recipes, text, and reload tag validation`

## Completed

- Added the formal `the_winter_rescue:geology_understanding` topic to the main mod datapack at `data/the_winter_rescue/frostedresearch/topics/geology_understanding.json`.
- Added the open notebook recipe `the_winter_rescue:research/research_notebook` and Design-managed copper-pick recipe `the_winter_rescue:research/copper_pro_pick` to the same bundled datapack. Their ingredients and stable IDs match the companion KubeJS mirrors.
- Added the topic's English and Chinese text to Frosted Research's own language resources, so `runClient` does not depend on companion assets for this vertical slice.
- Changed required block-tag validation to derive its tag universe from the active reload `ResourceManager`. Forge's live registry tag collection is not yet rebound when the mod reload listener runs and had incorrectly rejected the two valid geology tags during first startup.
- Added packaged-resource assertions for the topic and recipes, plus a validator test showing that current-reload tag resources are accepted and missing resources are still diagnosed.
- Updated the living research documentation and Phase 2 outcome to identify the mod datapack as the bundled source and the companion resources as compatible mirrors.

## Validation

- All five new/changed JSON resources parsed successfully.
- Focused `ResearchResultCodecTest` and `ResearchResultCatalogTest` passed.
- `./gradlew cleanResearchGameTestWorld runGameTestServer` installed catalogue revision 1 with `1` topic and `2` results, then passed all `18` required GameTests.
- The successful reload contains no `unknown block tag` or `unknown recipe` diagnostics for the geology topic.

## Remaining

- A currently running development client must be restarted because the reload validator Java code changed. After restart, verify the log reports one installed topic/two results and check both recipes in JEI/crafting.
- The companion KubeJS topic and recipes intentionally remain as mirrors for the existing full-pack distribution path.
