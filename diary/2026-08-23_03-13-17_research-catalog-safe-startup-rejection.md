# Safe local rejection for invalid research catalogues

- Time: `2026-08-23 03:13:17 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `integrated-server research catalogue startup rejection and partial-start cleanup`

## Completed

- Added `MinecraftResearchCatalogPreflightMixin` at `Minecraft#doWorldLoad` so a local invalid catalogue is rejected before the integrated-server thread and chunk-progress wait loop are created.
- The rejection logs all diagnostics, closes the pending `WorldStem` and level lock, clears the bundled world resource pack, and displays a localized error screen instead of appearing to hang.
- Made pre-level `MinecraftServer#saveAllChunks` skip custom save events and absent-overworld access, and made `TemperatureUpdate#shutdown` idempotent for partial initialization.

## Decisions

- Strict whole-catalogue rejection remains unchanged. The correction changes failure delivery and cleanup, not validation severity or fail-closed semantics.
- The integrated client performs an early preflight for usability, while the server still repeats full validation as the authority.

## Validation

- `./gradlew compileJava test` passed; the generated refmap contains the mapped `Minecraft#doWorldLoad` target for `MinecraftResearchCatalogPreflightMixin`.
- Research Mixin and language JSON files passed `jq` parsing.

## Remaining

- Manually open a local world with the currently invalid development catalogue and confirm the localized error screen appears, the title-screen button responds, the world is immediately unlocked, and no server crash report or shutdown NPE is produced.
