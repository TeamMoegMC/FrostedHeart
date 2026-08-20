# Citizen Flywheel PoC name removal

- Time: `2026-08-20 15:38:49 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `completed`
- Scope: `Citizen debug backend command, coordinator API, tests, and town living documentation`

## Completed

- Removed the obsolete `/citizen_debug backend flywheel_poc` compatibility alias. The only explicit GPU diagnostic command is now `/citizen_debug backend flywheel_m3`.
- Renamed `CitizenDebugClientCommand.useFlywheelPocBackend` and `CitizenRenderCoordinator.useFlywheelPocBackend` to `useFlywheelM3Backend`, including all test call sites.
- Replaced remaining PoC wording in current town documentation with M2 static-validation or M3 terminology and stated that the static backend and command entry no longer exist.
- Searched current source, tests, docs, plans, discussions, and filenames for additional `flywheel_poc`, `useFlywheelPocBackend`, and PoC naming; no Citizen rendering remnants remain outside append-only historical diary entries.

## Decisions

- Preserve old diary entries because they accurately record the former M2 implementation and project rules prohibit rewriting development history.
- Keep `flywheel_m3_instancing` as the active backend identifier. It describes the current implementation and is not a compatibility alias.

## Validation

- `./gradlew.bat test --console=plain`: passed (`BUILD SUCCESSFUL`).
- Current-tree searches outside `diary/` and `docs/deprecated/` return no Citizen Flywheel PoC identifiers or command aliases.

## Remaining

- None.
