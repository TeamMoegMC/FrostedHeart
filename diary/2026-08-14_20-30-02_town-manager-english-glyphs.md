# Town manager English glyph fallback

- Time: `2026-08-14 20:30:02 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Mayor's Seal English localization and town-building temperature formatting`

## Completed

- Replaced unsupported English staffing separators/arrows with compact ASCII text and shortened the summary to fit the panel.
- Replaced degree signs and related mathematical punctuation in affected English town temperature strings; temperature numbers are now unit-free code arguments and each locale supplies its own unit.
- Kept Chinese `°C` presentation while English uses `C` for compatibility with the pack's English default font.

## Decisions

- Fix the presentation at localization boundaries rather than changing numerical data or installing a font override.
- Preserve intentional non-ASCII icon strings elsewhere unless they are known to be broken.

## Validation

- `./gradlew compileJava processResources` passed with existing repository warnings only.
- Both locale JSON files parsed successfully; `git diff --check` passed.

## Remaining

- Reopen the Mayor's Seal in English and verify the staffing summary/counts, survival temperature title, and building-detail temperature line visually.
