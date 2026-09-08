# Warm stone upstream merge

- Time: `2026-09-08 23:45:28 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `complete the existing merge of origin/master@bd89ab7ab into master@53d0e833d, retaining warm-stone integration`

## Completed

- Fetched origin and confirmed the pending merge target is the latest remote master.
- Resolved three conflicted files. `MinecraftThermalInput.close()` clears the warm-stone item environment cache and closes the upstream lazy infrared payload only when allocated.
- Retained upstream heat-source discovery and infrared fixes together with the existing dropped-reservoir sampling API.
- Documentation impact: merged the climate lifecycle and world-temperature documents, preserving the warm-stone sections and the upstream verification date of September 8. Other upstream documentation changes are included unchanged.

## Decisions

- Continued the merge already in progress on master. Existing unrelated untracked files were not staged.
- Used JDK 17.0.2 for this validation process because the inherited JAVA_HOME points to a Java 8 installation that cannot start the configured Gradle daemon.

## Validation

- `gradlew.bat test compileGameTestJava --console=plain`: passed on JDK 17.0.2.
- JUnit: 191 suites, 784 tests, zero failures, errors, or skips.
- `git diff --cached --check`: passed; no unresolved merge entries remain.

## Remaining

- Forge GameTests were compiled but not executed in this merge validation.
- No remote push was requested or performed.
