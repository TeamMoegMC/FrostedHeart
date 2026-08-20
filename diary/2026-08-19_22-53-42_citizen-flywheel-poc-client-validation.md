# 居民 Flywheel PoC 实机验收

- Time: `2026-08-19 22:53:42 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent, with user-operated client validation`
- Status: `completed`
- Scope: `M2 Flywheel instancing, 64 detailed-owner accounting, steady uploads, F3+T rebuild, and visual confirmation`

## Completed

- Validated the 1024-moving benchmark with `flywheel_poc_instancing` and `maxDetailedCitizenEntities=64`: 64 detailed entities plus 960 Flywheel Body instances, seven active skin materials, and no missing or duplicate render ownership.
- Confirmed steady-state `instanceDirtyBytes=0`. The first sampled upload peak was 50,048 bytes, exactly `(1024 creations + 64 promotion removals) * 46`.
- Reloaded resources with F3+T. The backend remained active, ownership stayed 64 + 960, the scene remained visually correct, and the dirty peak became 88,320 bytes, exactly `(960 removals + 960 recreations) * 46`.
- User confirmed no inverted models, UV errors, ghosting, penetration, or abnormal lighting in the tested scene.

## Decisions

- Close the M2 base instancing/resource-reload decision gate and proceed to M3 dynamic instances.
- Treat `peakInstanceDirtyBytes` as sampled logical write traffic, not resident buffer size. The resident M2 buffer in the validated scene is `960 * 46 = 44,160` bytes.

## Validation

- Client metrics before reload: `cache=1024`, `detailed=64`, `body=960`, `batchDraws=7`, steady dirty `0`, peak dirty `50048`, backend `flywheel_poc_instancing`.
- Client metrics after F3+T: the same ownership/backend counts, steady dirty `0`, peak dirty `88320`.
- Visual result: confirmed correct by the user.

## Remaining

- Validate Flywheel batching and Oculus without/with a shader pack as separate compatibility cases.
- Implement M3 dynamic snapshot interpolation, animation, sleeping transforms, billboard LOD, and dirty light updates.
