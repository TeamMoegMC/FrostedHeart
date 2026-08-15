# Mayor's Seal textbox recursion crash

- Time: `2026-08-13 14:44:52 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `TownNameEditor, TownResidentsPanel, Mayor's Seal client rendering`

## Completed

- Fixed the immediate `StackOverflowError` when opening the Mayor's Seal. `TownNameEditor.commit` called `TextBox.setText(..., false)`, but Chorda's implementation still invokes `onTextChanged` through `moveCursorToEnd`; the callback recursively called `commit` and `setText` without a base case.
- Town and resident editors now treat `onTextChanged` as a notification that may also represent cursor movement or programmatic synchronization. Names are submitted once on Enter, Tab where applicable, or focus loss instead of on every character/cursor update.
- Added guarded resident-editor synchronization so selecting a resident or receiving authoritative data cannot emit transient mixed first/last-name requests.

## Decisions

- Do not rely on Chorda's `setText(..., false)` to suppress callbacks: its current implementation suppresses only the final explicit callback, not the callback caused by moving the cursor.
- Keep server validation and pending-authoritative synchronization unchanged; the fix is limited to client editor lifecycle and request timing.

## Validation

- Recompiled main and test sources successfully.
- `./gradlew test` — successful.
- `git diff --check` — successful.
- The supplied crash stack was matched directly to the removed recursive call chain.

## Remaining

- In-game smoke test: reopen the Mayor's Seal, then edit town and resident names using Enter and click-away to confirm focus behavior in the live UI.
