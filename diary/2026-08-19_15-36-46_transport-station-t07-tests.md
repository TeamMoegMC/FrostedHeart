# Transport Station T07 Tests

- Time: `2026-08-19 15:36:46 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `transport-station building tests and shared work-building removal cleanup`

## Completed

- Added concrete Transport Station staffing coverage for plan normalization, target bounds, and zero assignment capacity while unworkable.
- Added an independently testable scan-result application path and verified equivalent periodic scans do not emit duplicate building changes.
- Extended Codec round-trip assertions to include a non-empty occupied volume.
- Extended building-removal coverage for rostered and inconsistent resident work references, and made shared work-building removal clear the detached roster.
- Added a dedicated regression assertion for the shared `setMaxResidents` net-change guard.

## Decisions

- Kept scanner traversal private and exposed only the package-local pure result-application step needed by tests.
- Fixed roster cleanup in `AbstractTownResidentWorkBuilding#onRemoved` so every resident workplace follows the same removal contract.

## Validation

- `compileTestJava --offline --no-daemon --console=plain` passed.
- `TransportStationStaffingTest`, `TransportStationBuildingChangeTest`, `TransportStationBuildingCodecTest`, and `TownBuildingRemovalTest` each passed in a separate Gradle invocation.

## Remaining

- T08 must run the broader Transport Station and town integration suite, full tests, data-generation scope review, and final diff checks.
