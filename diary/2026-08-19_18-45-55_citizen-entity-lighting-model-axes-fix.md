# 居民实体光照与模型坐标轴根因修复

- Time: `2026-08-19 18:45:55 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `citizen batch RenderType, Steve model axes, billboard UV orientation, regression tests, and town living documentation`

## Completed

- Corrected the conclusion recorded in `2026-08-19_18-17-17_citizen-batch-uv-lightmap-fix.md`: Minecraft 1.20.1's `position_color_tex_lightmap.fsh` receives `UV2` but never samples `Sampler2`, so submitting packed light through that shader could not change citizen brightness.
- Replaced the manual batch shader path with seven cached `RenderType.entityCutoutNoCull` instances. Their `DefaultVertexFormat.NEW_ENTITY` vertices now include skin UV, `OverlayTexture.NO_OVERLAY`, packed light, and normals; the vanilla entity shader samples the lightmap and applies directional lighting.
- Rebuilt standing and sleeping model axes around vanilla `LivingEntityRenderer`'s `scale(-1, -1, 1)` convention. Standing skin front (`ModelPart` local `-Z`) follows citizen forward and head top (local `-Y`) points upward; sleeping skin front points upward and the head points along the bed direction.
- Added `CitizenBatchRenderLayout` with precomputed axes for all 256 visual yaw steps. The renderer reuses these immutable records without per-frame axis allocation.
- Corrected the standing billboard V orientation and supplied world normals for box faces and both billboard paths. Each face normal is transformed once through the current `PoseStack` normal matrix and reused for four vertices.
- Updated `docs/town/hybrid-simulation-architecture.md` to describe the implemented entity RenderType, lightmap behavior, model axes, and allocation profile.

## Decisions

- Used the vanilla entity cutout pipeline rather than another manually selected core shader because the RenderType owns the matching shader, lightmap/overlay state, texture, and `NEW_ENTITY` vertex contract together.
- Kept the existing per-citizen packed-light cache, seven-skin grouping, distance thresholds, server visibility budget, and the rule that sleeping citizens do not create fake entities.
- Removed a JUnit assertion that directly initialized `RenderType`: a plain test JVM has no bootstrapped Minecraft registry and fails with `IllegalArgumentException: Not bootstrapped`. Shader selection was instead verified against the mapped 1.20.1 `RenderType` source and bundled GLSL; executable regression tests cover the pure coordinate mapping.

## Validation

- `\.\gradlew.bat compileJava compileTestJava --console=plain`: passed; only the project's existing 20 JEI removal/deprecation warnings and existing Mixin/unchecked notices were reported.
- Medium-effort resident suite: `\.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.*" --console=plain`: 30 tests passed, 0 failed, build successful.
- `ClientCitizenBatchRenderLayoutTest`: standing and sleeping front/top orientation checks passed, including orthonormality across all 256 visual yaw steps.
- Compared `ModelPart.Cube`, `LivingEntityRenderer`, `RenderType.entityCutoutNoCull`, `position_color_tex_lightmap.fsh`, and `rendertype_entity_cutout_no_cull.{vsh,fsh}` from the mapped/bundled Minecraft 1.20.1 sources.

## Remaining

- Run an in-game night/interior visual smoke test at 24-64 and 64-96 block LODs, including all four standing and sleeping orientations. Compilation and CPU tests cannot execute or inspect the live GPU shader output.
