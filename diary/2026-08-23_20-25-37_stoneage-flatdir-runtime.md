# Stone Age local flatDir runtime dependency

- Time: `2026-08-23 20:25:37 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `build.gradle local development mod repository and runClient runtime classpath`

## Completed

- Added the scoped `Local Dev Mods` flat-directory repository for JARs under `dev-mods/`.
- Replaced the failing Stone Age Curse Maven runtime coordinate with the local `stone-age-1.20.1-1.6.8.jar` coordinate while retaining ForgeGradle deobfuscation.
- No living game-system documentation changed because this only changes the local development dependency source.

## Decisions

- Used the synthetic group `local.devmods` and restricted the flat-directory repository to that group so unrelated Maven dependencies do not resolve from unversioned local files.
- Kept Stone Age as `clientRuntimeOnly` because the compatibility Mixin targets its entry point by string and does not require Stone Age classes on the compile classpath.

## Validation

- `./gradlew dependencies --configuration clientRuntimeClasspath` resolved `local.devmods:stone-age-1.20.1:1.6.8_mapped_parchment_2023.09.03-1.20.1` successfully.
- `./gradlew runClient` compiled the project, discovered the mapped Stone Age JAR as mod `stone_age` version `1.20.1-1.6.8`, completed resource loading, and started the sound engine before being stopped manually.
- `git diff --check -- build.gradle` passed.

## Remaining

- The local coordinate must be updated if the Stone Age JAR filename or version changes.
