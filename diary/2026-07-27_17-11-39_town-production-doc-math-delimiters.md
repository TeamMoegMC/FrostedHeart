# Town production document math delimiters

- Time: `2026-07-27 17:11:39 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `docs/town-hunting-and-mining-productivity-model.md`

## Completed

- Replaced all block-math `\[` and `\]` delimiters with the repository renderer-compatible `$$` form.

## Decisions

- Kept inline formula delimiters unchanged and changed the display-math blocks explicitly identified by the user.

## Validation

- Confirmed there are no remaining `\[` or `\]` block delimiters.
- Confirmed the document contains 100 `$$` delimiter lines, forming 50 balanced display-math blocks.
- `git diff --check` reported no whitespace errors.

## Remaining

- None.
