# Empty-shape heat cache preservation

- Time: `2026-08-14 21:57:42 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `surrounding temperature block-info cache and recipe reload invalidation`

## Completed

- Changed block-info compaction so an empty collision shape reuses the air singleton only when its computed temperature is also zero; empty-shape heat sources such as fire now retain their configured heat.
- Added `BlockInfoCachePolicy` as a configuration-independent rule seam and covered cold empty, heated empty, and cold full shapes with regression tests.
- Added a monotonic `BlockTempData` recipe generation; global `BlockState` cache entries are lazily replaced when their generation is stale, preventing stale temperatures without initializing the simulator during world loading.

## Decisions

- Kept the merged global cache optimization and corrected only its equivalence rule: collision geometry and temperature are independent properties.
- Used lightweight generation-based invalidation because the recipe reload runs before the server config is available; directly calling a simulator cache-clear method there would trigger premature static configuration access.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulatorCacheTest` passed.
- `./gradlew build` passed, including the full test suite; existing duplicate-resource and license warnings remain.
- `git diff --check` passed.

## Remaining

- In-game smoke-test fire and soul fire before and after a data-pack reload to confirm their observed surrounding temperature remains nonzero.
