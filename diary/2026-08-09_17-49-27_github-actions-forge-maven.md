# Repair snapshot workflow Java and Forge dependency resolution

- Time: `2026-08-09 17:49:27 UTC+8`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `.github/workflows/publish_snapshot.yml`, `build.gradle`

## Completed

- Standardized both snapshot workflow jobs on Temurin JDK 17 and removed the later JDK
  20/21/25 setup steps and empty manual `JAVA_HOME_*` exports.
- Added temporary stacktrace/info logging to the `runData` step for actionable CI dependency diagnostics.
- Put the official MinecraftForge Maven repository first for `net.minecraftforge` artifacts.
- Confirmed that ModMaven returned a 622-byte HTML redirect page for the Forge 47.3.0
  `userdev` JAR while the official Forge Maven returned the valid 3,005,262-byte ZIP.

## Decisions

- Kept Gradle 8.1.1 and ran it on Java 17, matching the project's declared toolchain and
  avoiding the Java 25 class-file incompatibility.
- Retained ModMaven as the existing JEI fallback, but ensured ForgeGradle's internal
  downloader encounters the official Forge artifact before the bad mirror response.

## Validation

- `git diff --check` passed.
- Ruby YAML parsing of `.github/workflows/publish_snapshot.yml` passed.
- `GRADLE_USER_HOME=/tmp/frostedheart-codex-gradle ./gradlew --version` confirmed
  Gradle 8.1.1 starts on JDK 17.0.20.
- A clean-cache `./gradlew help --stacktrace --no-daemon` reproduced the original
  `ZipException` before the repository fix and passed after it (`BUILD SUCCESSFUL`).

## Remaining

- Push the changes and confirm the next `Publish Snapshots` run completes `runData`, jar
  creation, artifact upload, and COS publication. Remove `--info` later if CI verbosity is
  no longer useful.
