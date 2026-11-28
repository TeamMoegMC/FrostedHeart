# Town model JSON example syntax fix

- Time: `2026-08-24 23:34:31 +08:00`
- Author: `Codex; OpenAI GPT-5; documentation and validation role`
- Status: `completed`
- Scope: `docs/town/town-model.md`, Stage 3/4 staffing JSON example

## Completed

- Added the missing outer JSON object braces around the `staffing` example so the Markdown JSON code block is syntactically valid.

## Decisions

- Kept the example focused on the newly added `staffing` field while representing it as a valid root object for IDE and parser validation.

## Validation

- Parsed the extracted JSON example successfully with PowerShell's `ConvertFrom-Json`.
- `git diff --check` passed.

## Remaining

- None.
