# Thermal latest-frame retry crash fixed

- Time: `2026-08-27 02:16:50 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and investigation agent`
- Status: `completed`
- Scope: `MinecraftThermalTopologyApplier` stale geometry handling and its unit coverage

## Completed

- Traced the integrated-server crash to `cancelUnchangedBrickMutations`, where a rejected unchanged-Brick acknowledgement threw `LatestFrameException` before the existing catch scope.
- Converted that expected stale-input result into `ApplyStatus.LATEST_FRAME_REQUIRED`, allowing `MinecraftThermalInput.dispatchSealedFrame` to request an urgent retry instead of terminating the server tick.
- Added a regression test that seals an unchanged mutation cut, advances the live Page revision before apply, verifies the stale cut requests the latest frame, and verifies the following frame applies successfully.

## Decisions

- A Page rejecting an acknowledgement because its live revision, dirty ownership, or full-resync state moved beyond the captured cut is a recoverable latest-frame condition, not a fatal invariant violation.
- No living documentation changed because the intended latest-only topology contract did not change; this fix restores that existing contract.
- The crash was in the active thermal topology path and was unrelated to the separately disabled `TemperatureThreadingPool` lifecycle.

## Validation

- The new targeted regression test failed against the old implementation with `MinecraftThermalTopologyApplier$LatestFrameException` and passed after the fix.
- `./gradlew.bat compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --tests "com.teammoeg.frostedheart.content.climate.player.SurroundingTemperatureSimulator*" --offline --console=plain` completed successfully.
- `182/182` selected JUnit tests passed across `34` suites with zero failures, errors, or skips.
- A fresh `runClient` session loaded `new world CT`, ran the integrated server through save-and-pause and game-mode changes, then exited normally with all dimensions saved. `latest.log` contained no `LatestFrameException` or crash markers, and no new crash report was created.
- `git diff --check` reported no whitespace errors; only existing line-ending conversion warnings were emitted.

## Remaining

- None.
