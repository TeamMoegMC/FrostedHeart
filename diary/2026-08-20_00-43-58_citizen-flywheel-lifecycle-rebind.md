# Citizen Flywheel lifecycle rebind

- Time: `2026-08-20 00:43:58 +08:00`
- Author: `Codex (OpenAI GPT-5; coding agent)`
- Status: `partial`
- Scope: `CitizenRenderCoordinator`, `CitizenRenderBackend`, `CitizenClientEvents`, `FlywheelCitizenBackend`, coordinator tests, and town Citizen documentation

## Completed

- Traced the dimension fallback to `CitizenRenderCoordinator.render` checking M3 health while the backend still referenced the previous `ClientLevel`.
- Traced `/flywheel backend instancing` corruption to Flywheel replacing and deleting the current `InstanceWorld` during `LevelRenderer.allChanged`, while Citizen entries still referenced the old `MaterialManager` until a later tick.
- Added explicit client-level and renderer-reload lifecycle callbacks. Dimension changes now rebind before health checks; `ReloadRenderersEvent` is consumed at `EventPriority.LOWEST`, after Flywheel's default instance-world replacement.
- Changed invalidated manager/world cleanup to discard detached logical handles without calling the old instancer API, then recreate cached citizens against the new manager while retaining cached light and walk phase where applicable.
- Updated `docs/town/citizen-rendering-at-scale.md` and `docs/town/hybrid-simulation-architecture.md` with the implemented lifecycle contract and remaining client validation.

## Decisions

- Keep the selected M3 backend across a supported dimension change. Fall back to `cpu_batch` only when the new world cannot use instancing or the rebind fails.
- Treat Flywheel renderer replacement as an event boundary, not a next-tick polling condition. Manager identity polling remains only a fallback.
- Never call `delete()` on handles whose owning Flywheel manager or world may already be disposed.

## Validation

- `./gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.CitizenRenderCoordinatorTest --tests com.teammoeg.frostedheart.content.town.citizen.client.FlywheelCitizenBackendTest`: passed, 18 tests.
- `./gradlew.bat test`: passed, 71 suites / 239 tests / 0 failures / 0 errors / 0 skipped.
- Regression coverage includes backend preservation on world clear, level-rebind failure fallback, immediate renderer-reload callback, renderer-reload failure fallback, and listener priority.

## Remaining

- In an actual client, repeat `/flywheel backend instancing` while M3 owns a 1024-moving benchmark and verify Body layout remains complete and stable while walking.
- Travel overworld to End/Nether and back, then verify `/citizen_debug backend status` remains `flywheel_m3_instancing` and no lifecycle fallback error is logged.
- Oculus compatibility and GPU performance validation remain intentionally deferred.
