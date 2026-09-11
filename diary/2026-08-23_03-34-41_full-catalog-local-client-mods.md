# Full-catalogue local client dependencies

- Time: `2026-08-23 03:34:41 +08:00`
- Author: `Codex; OpenAI; coding agent`
- Status: `completed`
- Scope: `runClient companion-pack dependencies and Stone Age development compatibility`

## Completed

- Copied the companion pack's KubeJS stack, Charcoal Pit, Stone Age, and Immersive Industry JARs byte-for-byte into the ignored `run/dev-mods/` directory; the existing companion `kubejs/` directory remains under `run/`.
- Added conditional `clientRuntimeOnly fg.deobf(...)` dependencies for those local production JARs so ForgeGradle remaps them instead of loading them raw from `run/mods/`.
- Added `ExampleModMixin` to bypass Stone Age 1.6.8's development-only safe-referent rejection while retaining sided proxy selection.

## Decisions

- Production JARs used by a named Forge development run belong in `run/dev-mods/`; `run/mods/` remains empty so the same mod is not loaded raw or duplicated.
- Immersive Engineering and Create were not copied because the Gradle run already provides them.
- Local companion JARs remain optional and ignored; builds without those files continue to resolve the normal declared dependencies.

## Validation

- Verified all nine copied JARs against their source SHA-256 checksums and opened each archive with `unzip -t`.
- `./gradlew dependencies --configuration clientRuntimeClasspath` resolved all nine through ForgeGradle's mapped development artifacts.
- `./gradlew compileJava` passed.
- `./gradlew runClient` loaded the Stone Age compatibility Mixin, completed KubeJS startup/client scripts and resource loading, started the sound engine, and remained live at the main menu until the verification process was stopped manually.

## Remaining

- Enter a disposable copy of `20030716` and validate catalogue migration, or confirm that any remaining invalid catalogue is rejected by the localized preflight screen without starting an integrated server.
