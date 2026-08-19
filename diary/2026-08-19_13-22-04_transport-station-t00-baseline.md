# Transport station T00 baseline

- Time: `2026-08-19 13:22:04 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent /root`
- Status: `completed`
- Scope: `T00 baseline; Java/Gradle environment, town tests, and compile verification`

## Completed

- Captured the existing worktree baseline: branch `master`, HEAD `5a7a175aa`, with pre-existing documentation, diary, simulation, and backup changes preserved.
- Confirmed Java `17.0.2` and local Gradle `8.1.1` are usable.
- Verified the approved transport-station documentation and task checklist were present before implementation begins.

## Decisions

- The Gradle wrapper could not create its default `C:\.gradle` lock directory, and a workspace-only offline run lacked the `com.diffplug.eclipse.apt:4.2.0` plugin. The baseline therefore used the existing local Gradle 8.1.1 distribution with Java 17 and the configured user Gradle cache.
- No source, resource, or generated project file was changed by T00. The temporary workspace Gradle home created during the failed offline attempt was removed.

## Validation

- `gradle.bat compileJava --no-daemon --console=plain` - passed in 1m41s; 4 actionable tasks.
- `gradle.bat test --tests 'com.teammoeg.frostedheart.content.town.*' --offline --no-daemon --console=plain` - passed; 172 tests, 0 failures, 0 skipped across 51 XML result files.
- Compile emitted 20 pre-existing warnings: one Mixin visibility warning and JEI deprecation warnings.
- Test resource processing emitted six pre-existing duplicate item-model path warnings for scaffolding assets.
- Git status after validation shows no new source/resource changes; `git diff --check` remains clean apart from the repository's LF-to-CRLF warning.

## Remaining

- Proceed to `T01` (TransportStation Building data model and Codec). Preserve the baseline warnings unless a later task directly changes their owning code.
