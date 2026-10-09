# Thermal v5 GameTest and commit validation

- Time: `2026-10-09 18:42:04 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering agent`
- Status: `completed`
- Scope: `outdated dormant thermal persistence test assertion and final reservoir registration validation`

## Completed

- Updated `ThermalDormantCoolingGameTests.sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift` to expect storage version 5, matching `DormantChunkThermalState.FORMAT_VERSION`, and updated the assertion message.
- Retained the twenty repeated save/reload cycles and all subsequent air/material cooling and temperature checks.
- Resolves the sole remaining failure recorded in [the reservoir registration entry](2026-10-09_18-13-27_reservoir-registrate-datagen-fix.md).

## Decisions

- Correct the outdated test expectation without changing production persistence or the project build configuration.
- Documentation impact: no additional living-document update was needed because implemented storage behavior is unchanged. The accompanying reservoir registration change already updates the player-temperature document.

## Validation

- `runGameTestServer --offline --no-daemon --console=plain`, Java 17.0.2 and existing project build settings: `BUILD SUCCESSFUL`; all 104 required GameTests passed, including the complete dormant cooling test and all placed-reservoir tests. Log: `build/reservoir-registration-commit-validation.log`.
- `git diff --check`: passed.
- Build configuration files remain unchanged. Data generation passed in the preceding reservoir registration validation and was not repeated for this assertion-only change.

## Remaining

- None.
