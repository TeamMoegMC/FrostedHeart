# Player Thermal Parameter Formatting

- Time: `2026-08-30 17:47:41 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation agent`
- Status: `completed`
- Scope: `player thermal method declarations and calls`

## Completed

- Replaced one-parameter-per-line formatting with compact single-line calls or
  semantic parameter groups across the player thermal domain classes.
- Kept lines within 120 columns without adding parameter carriers, state, or
  runtime work.

## Decisions

- This pass changes formatting only. Formula order, evaluation order, player
  state, equipment behavior, and persistence remain unchanged.
- Living documentation did not require an update because behavior did not
  change.

## Validation

- `git diff --check -- src/main/java/com/teammoeg/frostedheart/content/climate/player`: passed.
- `./gradlew.bat compileJava --offline --console=plain`: passed with existing
  Mixin and deprecation warnings.

## Remaining

- None.
