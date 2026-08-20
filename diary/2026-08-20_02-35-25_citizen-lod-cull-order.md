# Citizen LOD state update before CPU culling

- Time: `2026-08-20 02:35:25 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `ClientCitizenRenderer` and the shared coordinator-owned batch LOD state

## Completed

- Moved the coordinator LOD-owner update ahead of back-face and frustum rejection in the CPU renderer.
- Residents that leave the `96`-block range while off-screen now evict stale Body/Billboard state; re-entering the view cannot resurrect an old Body owner.
- Kept the existing single cache traversal and did not add a second per-frame resident loop.

## Decisions

- Distance-based presentation state must be updated for every non-detailed cached resident that reaches the renderer distance check, even when its geometry is culled.

## Validation

- `./gradlew.bat test --console=plain`: passed (`BUILD SUCCESSFUL`).
- `git diff --check`: no whitespace errors; only existing line-ending warnings.

## Remaining

- In-game Oculus/CPU/M3 visual regression remains the final graphical validation.
