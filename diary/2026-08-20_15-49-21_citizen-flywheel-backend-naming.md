# Citizen Flywheel backend naming

- Time: `2026-08-20 15:49:21 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `Citizen Flywheel backend runtime identifiers, debug command, tests, plans, and town living documentation`

## Completed

- Replaced the development-milestone name M3 with capability-based Flywheel naming throughout current Citizen code and project knowledge.
- Changed the explicit debug command from `/citizen_debug backend flywheel_m3` to `/citizen_debug backend flywheel` without retaining a legacy alias.
- Changed the requested/active backend identifier from `flywheel_m3_instancing` to `flywheel_instancing`.
- Renamed `BackendPreference.FLYWHEEL_M3` to `FLYWHEEL` and `useFlywheelM3Backend` to `useFlywheelBackend`; updated logs, comments, test names, variables, and assertions.
- Updated the town documentation index, rendering reference, hybrid architecture document, and the completed Body clock plan to use Flywheel/Flywheel instancing terminology.

## Decisions

- Keep `FlywheelCitizenBackend` as the implementation class name and `flywheel_instancing` as the status identifier. The latter states the required Flywheel capability and distinguishes it from Flywheel BATCHING/OFF.
- Do not provide old command or status aliases because these are client-session diagnostics with no persistence, network, or save-data compatibility requirement.
- Preserve earlier diary entries that mention M3 because diary history is append-only and accurately records the former milestone name.

## Validation

- `./gradlew.bat test --console=plain`: passed (`BUILD SUCCESSFUL`).
- Searches across current Citizen source, tests, town docs, plans, and discussions return no `M3`, `flywheel_m3`, `FLYWHEEL_M3`, or `useFlywheelM3` references.

## Remaining

- None.
