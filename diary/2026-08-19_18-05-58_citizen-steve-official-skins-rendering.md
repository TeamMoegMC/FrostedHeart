# 居民 Steve 模型与原版默认皮肤渲染

- Time: `2026-08-19 18:05:58 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `citizen client models, LOD skin rendering, performance, and living documentation`

## Completed

- Added `CitizenSkins`, which deterministically maps a stable citizen id to Minecraft's built-in wide-arm Makena, Efe, Noor, Kai, Ari, Zuri, Sunny skin resources.
- Replaced the near-range villager model and wandering-trader texture with vanilla's wide-arm `PlayerModel`; close citizens retain vanilla limb animation, lighting, and skin overlay layers.
- Reworked the non-entity path to render textured Steve-proportion head, torso, arms, and legs in the medium LOD, plus skin-derived billboards in the far LOD.
- Kept sleeping citizens horizontal and entirely in the batch path, with no fake entity creation.
- Updated `docs/town/hybrid-simulation-architecture.md` with the skin mapping, geometry, and batching contract.

## Decisions

- Reused the official client resources at `textures/entity/player/wide/*.png`; no Mojang textures are copied into mod assets.
- Derived the skin locally from citizen id, so LOD transitions and cache respawns keep the same appearance without adding packet fields or persistence data.
- Per-frame visibility and frustum checks remain a single cache pass. Geometry is written into seven reusable buffers and only used skins are submitted, for at most seven draw calls and no per-citizen grouping collections.
- The textured box UV writer follows vanilla `ModelPart.Cube`'s 64x64 unfolding and uses scalar locals to avoid per-box temporary arrays and render-thread GC pressure.

## Validation

- Confirmed all seven requested `assets/minecraft/textures/entity/player/wide/*.png` entries exist in the Minecraft 1.20.1 client jar.
- `.\gradlew.bat compileJava --console=plain`: passed; only the project's existing 20 JEI removal/deprecation warnings and existing Mixin/unchecked notices were reported.
- Medium-effort subtask ran `.\gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.*" --console=plain`: 28 tests passed, 0 failures, 0 errors, 0 skipped.
- `git diff --check`: passed; only line-ending conversion notices were reported.

## Remaining

- Perform an in-game visual smoke test across near, medium, far, and all four sleeping orientations; automated tests cannot validate GPU texture orientation or visual skin-layer continuity.
