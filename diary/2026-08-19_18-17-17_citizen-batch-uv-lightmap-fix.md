# 居民批量模型 UV 与 lightmap 修复

- Time: `2026-08-19 18:17:17 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `citizen batch skin UVs, client light sampling, render state, and living documentation`

## Completed

- Corrected `ClientCitizenRenderer.addTexturedBox` so the top-face and east-face U boundaries branch independently exactly like vanilla `ModelPart.Cube`; head and limb side/back faces no longer sample shifted skin regions.
- Replaced the unlit `POSITION_TEX_COLOR` path with `POSITION_COLOR_TEX_LIGHTMAP` and submitted a packed sky/block light value with every batch vertex.
- Explicitly enabled the Minecraft lightmap texture while the seven citizen skin buffers are drawn and restored it afterward, because the preceding entity RenderTypes clear that state before `AFTER_ENTITIES`.
- Added per-citizen light sampling caches. Moving across a block refreshes immediately; stationary citizens refresh on staggered 5–8 tick intervals using one shared `MutableBlockPos` and no per-sample position allocation.
- Updated `docs/town/hybrid-simulation-architecture.md` with the implemented lightmap and sampling behavior.

## Decisions

- Used the lightweight position/color/texture/lightmap shader rather than `NEW_ENTITY`: it fixes environmental brightness without adding overlay and normal attributes to every low-LOD vertex.
- Cached packed light in `ClientCitizen` rather than a separate id map, avoiding another render-thread lookup and preventing cache-lifetime cleanup work.
- Kept skin grouping, visibility budgets, LOD distances, geometry counts, and the no-sleeping-fake-entity rule unchanged.

## Validation

- `.\gradlew.bat compileJava --console=plain`: passed; only the project's existing 20 JEI removal/deprecation warnings and existing Mixin/unchecked notices were reported.
- Medium-effort subtask ran `.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.*" --console=plain`: 28 tests passed; build successful.
- Compared the corrected UV boundary equations with the mapped Minecraft 1.20.1 `ModelPart.Cube` constructor.
- Confirmed Forge's entity RenderTypes clear the lightmap state before `RenderLevelStageEvent.Stage.AFTER_ENTITIES`, requiring the explicit scoped binding.
- `git diff --check`: passed; only line-ending conversion notices were reported.

## Remaining

- Perform an in-game night/interior visual smoke test at medium and far LOD, including all four sleeping orientations; automated tests do not execute the GPU shader pipeline.
