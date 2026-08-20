# Citizen AUTO render backend release default

- Time: `2026-08-20 15:20:20 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `CitizenRenderCoordinator, Citizen debug commands, coordinator tests, and town living documentation`

## Completed

- Made `AUTO` the client startup preference. The first client-world transition attempts `flywheel_m3_instancing` and keeps `cpu_batch` active when Flywheel `INSTANCING` is unavailable.
- Added `/citizen_debug backend auto`; backend status distinguishes the requested preference from the active backend and reports compatibility fallback.
- Preserved AUTO or explicit M3 intent across runtime faults, dimension changes, and renderer reloads. A later `ReloadRenderersEvent` retries M3 after Flywheel has rebuilt its renderer.
- Kept explicit `cpu_batch` as a session diagnostic override that cancels pending automatic restoration. Explicit `flywheel_m3` remains available for compatibility fault injection.
- Protected M3 candidate construction as well as initialization and health checks. A constructor failure now leaves the initialized CPU backend active instead of escaping the fallback boundary.
- Updated `docs/town/citizen-rendering-at-scale.md` and `docs/town/hybrid-simulation-architecture.md` to describe AUTO as the implemented default and CPU as the compatibility path.

## Decisions

- Keep the preference client-only and session-local. This change adds no server packet, synchronized field, persistence format, Forge configuration, or simulation behavior.
- Retry only on world or renderer lifecycle events, not every frame, so unsupported hardware and Oculus shader-pack fallback do not create a probe loop.
- Do not bypass Flywheel's capability or shader-pack checks. AUTO selects M3 only after candidate initialization, cache replay, and health validation succeed.

## Validation

- `./gradlew.bat test --tests com.teammoeg.frostedheart.content.town.citizen.client.CitizenRenderCoordinatorTest --console=plain`: passed.
- `./gradlew.bat test --console=plain`: passed (`BUILD SUCCESSFUL`).
- Coverage includes AUTO success, unavailable instancing fallback and reload recovery, M3 candidate construction failure, runtime health fallback, and explicit CPU cancellation for both AUTO and explicit M3 requests.

## Remaining

- Before packaging the release, perform one client smoke pass: start without issuing a backend command and verify `requested=auto`; repeat with Oculus shaders enabled, then disable shaders and verify automatic M3 restoration after renderer reload.
