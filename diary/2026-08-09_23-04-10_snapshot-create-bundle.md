# Replace retired dependency mirrors for snapshot data generation

- Time: `2026-08-09 23:04:10 UTC+8`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `build.gradle`

## Completed

- Removed the ModMaven fallback after CI downloaded its HTML redirect responses as JARs,
  first for Forge userdev and then for Mojang DataFixerUpper.
- Replaced the unavailable tterrag-hosted Create artifact with the existing Curse Maven
  coordinate for Create 0.5.1.j (`create-328085:5838779`).
- Switched Registrate resolution to the mirror used by Create's current official build and
  made the standalone dependency compile-only because the Create release bundles it.
- Added a small Gradle extraction task that exposes the matching bundled Flywheel JAR to
  the Java compiler while leaving runtime loading to Create's Jar-in-Jar metadata.

## Decisions

- Use the complete Create distribution as the runtime dependency because Flywheel 0.6.9+
  is intentionally distributed inside dependent mods rather than as a standalone download.
- Keep Flywheel and Registrate compile-only outside the Create bundle to prevent duplicate
  Java modules during `runData`.
- Avoid broad third-party Maven mirrors so unrelated dependencies cannot be replaced by
  HTML redirect or challenge pages again.

## Validation

- Verified the Create Curse Maven JAR is a valid 15,583,566-byte archive containing
  `flywheel-forge-1.20.1-0.6.11-13.jar` and `Registrate-MC1.20-1.3.3.jar`.
- A clean-cache `./gradlew compileJava --stacktrace --no-daemon` completed successfully.
- The workflow-equivalent `./gradlew runData -Pmod_version="0.7.6-snapshot882" --stacktrace
  --no-daemon` completed successfully with the final configuration.
- `git diff --check` passed; incidental generated-resource newline changes were removed.

## Remaining

- Push this change and confirm the next `Publish Snapshots` Actions run completes the later
  packaging, artifact upload, and publication steps.
