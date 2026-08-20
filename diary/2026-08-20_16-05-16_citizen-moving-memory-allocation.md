# Citizen moving memory allocation reduction

- Time: `2026-08-20 16:05:16 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `ClientCitizen culling bounds, 1024-moving client allocation profile, regression coverage, and town rendering documentation`

## Completed

- Profiled the running 1024-moving client with `jcmd` and a 20-second JFR recording instead of inferring retained memory from process RSS.
- Confirmed that Citizen/Flywheel wrapper objects are small and stable: 1024 client states, 960 instance payloads/entries, and 64 detailed proxies have less than 0.3 MiB combined shallow size.
- Confirmed direct buffers stayed near 5.0 MiB during the recording; the Citizen workload was not growing a Java direct-buffer pool.
- Identified an approximately 9.9 MiB sampled `AABB` allocation path from eager `ClientCitizen.update -> createCullingBox` calls, even though the active Flywheel backend never reads CPU frustum boxes.
- Changed `ClientCitizen` to invalidate its swept culling box on snapshots and materialize it only when the CPU renderer calls `cullingBox()`.
- Added a regression test covering zero eager materialization, stable reuse between reads, invalidation after an update, and refresh on the next CPU visibility read.
- Updated `docs/town/citizen-rendering-at-scale.md` with the measured memory composition, remaining bounded detailed-entity allocation, and the backend-specific culling lifecycle.

## Decisions

- Preserve the exact swept bounds and CPU fallback behavior; this is a lifecycle change, not a culling or rendering-policy change.
- Leave the second sampled `AABB` path in vanilla `Entity.setPos` unchanged. It is bounded by `maxDetailedCitizenEntities` (64 by default), while bypassing vanilla bounding-box updates would risk stale frustum, picking, and entity-section behavior.
- Treat the observed hundreds of MiB as allocation-driven G1 heap commitment until a new-build comparison proves otherwise. The profile found no Citizen-sized retained object graph or expanding direct buffer.

## Validation

- `./gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.ClientCitizenCullBoxTest --console=plain`: passed.
- Baseline `jcmd GC.class_histogram`: `1024 ClientCitizen` (155,648 B), `960 CitizenInstanceData` (76,800 B), `960 FlywheelCitizenBackend.Entry` (23,040 B), and `64 FakeCitizenEntity` (46,592 B).
- Baseline JFR: `ClientCitizen.createCullingBox` and `FakeCitizenManager.drive -> Entity.setPos` were the only client Citizen allocation samples; direct-buffer capacity remained approximately 5.0 MiB.

## Remaining

- Restart the client with this build and repeat the same 1024-moving 20-second JFR capture. The `ClientCitizen.createCullingBox` allocation stack must be absent while `active=flywheel_instancing`; the bounded detailed-entity `Entity.setPos` stack may remain.
