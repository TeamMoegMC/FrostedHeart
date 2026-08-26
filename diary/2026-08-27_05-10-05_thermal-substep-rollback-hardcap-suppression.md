# Thermal substep rollback and hard-cap suppression

- Time: `2026-08-27 05:10:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation and investigation agent`
- Status: `completed`
- Scope: `Thermal transport mutation recovery, phase request rollback, sustained work-limit lifecycle, tests, logs, and climate documentation`

## Completed

- Added a reusable arena mutation checkpoint and phase-request transaction so a transport substep restores cell enthalpy, reserved phase energy, request state, sequence, and request-ring write position after a mutation-stage exception.
- Changed failed transport continuation to retry from the failed substep start while preserving the already-accounted source cursor, instead of treating a partially executed substep as complete.
- Split recovery into `TOPOLOGY` and `WORK_LIMIT`. A sustained hard-cap now installs an empty recovery sweep, suppresses mesh publication and physical source binding, and ACKs unchanged frames without rebuilding the same oversized work set.
- Limited hard-cap recompilation attempts to real Page, geometry, material, FarField, or retirement input changes. Successful replacement sweep installation clears suppression.
- Corrected the work-limit warning to describe topology-input recovery rather than Page retirement alone, and encoded the no-change topology ACK as `DUPLICATE` in the regression test.

## Decisions

- `preflight` validates the immutable sweep targets and inputs but does not claim that later mutation code cannot throw; the entire mutation substep therefore owns rollback.
- Work-limit refusal is not topology corruption. Keep gameplay on natural fallback until a changed work set can be evaluated again.
- Keep stale geometry-frame retry, transport rollback, and work-limit suppression as separate lifecycle paths.

## Validation

- Independent `gpt-5.6-luna` verification ran `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain`: successful; all `187/187` selected JUnit tests passed.
- `gradlew.bat runGameTestServer --offline --console=plain`: successful; all `12/12` required Forge GameTests passed.
- `git diff --check`: successful with no whitespace errors; only repository LF/CRLF conversion warnings were reported.

## Remaining

- Measure large-save hard-cap entry and recovery with JFR before claiming a tick-time result; lifecycle correctness is covered, production cost is not yet profiled.
