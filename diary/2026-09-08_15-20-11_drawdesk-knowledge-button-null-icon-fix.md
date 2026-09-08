# Drawing desk knowledge button null icon fix

- Time: `2026-09-08 15:20:11 +0800`
- Author: `Codex; OpenAI; primary implementation agent`
- Status: `completed`
- Scope: `DrawDeskLayer` knowledge entry button

## Completed

- Fixed the `latest.log` crash in `Button.drawIcon` while rendering the drawing desk. The new text button passed `null`,
  while CUI uses `CIcons.nop()` to represent no icon.
- Replaced that constructor argument with `CIcons.nop()`.

## Decisions

- Correct the caller to follow the existing CUI contract. No framework changes or new tests.
- Documentation impact: no living behavior contract changed; recorded the fix here only.

## Validation

- `./gradlew compileJava` passed.

## Remaining

- None.
