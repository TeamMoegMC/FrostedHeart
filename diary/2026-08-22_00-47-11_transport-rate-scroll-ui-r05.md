# Warehouse interface rate scroll UI R05

- Time: `2026-08-22 00:47:11 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `warehouse interface rate input, menu layout, server-derived view limits, tests, plans, and living documentation`

## Completed

- Removed the five fixed rate shortcut buttons and their row. Shifted both interface slot bands, transport status rows, and player inventory upward by 16 pixels; the menu height is now 218 pixels.
- Added immediate mouse-wheel rate adjustment over the input field. None, Shift, Ctrl, and Shift+Ctrl use the same `1`, `8`, `16`, and `64` increments as warehouse target amounts.
- Added `maximumRateItemsPerSecond` to `WarehouseInterfaceTransportView.CODEC`. The server supplies the current configured maximum; the input verifier, character limit, scroll clamp, and range hint all use that value.
- Numeric input above the synchronized maximum is rejected during editing. Other invalid intermediate text remains visible in the existing red error state and cannot be submitted.
- Centralized logical and rendered slot coordinates in `WarehouseInterfaceMenu` without introducing a common-to-client class dependency.
- Updated living town documentation and both transport plans with the second H01 feedback and R05 outcome.

## Decisions

- The client range is not hard-coded to 1280: 1280 remains the source default, while a server-configured maximum travels through the authoritative menu view.
- Scrolling sends the same single integer rate command as typing and pressing Set. Existing server-side proximity, team, binding, configured range, and capacity admission checks remain authoritative.
- Fixed shortcut buttons were removed rather than merely hidden; the reclaimed row reduces the complete menu height.

## Validation

- Warehouse and transport directed matrix: `55` tests passed.
- Entire `com.teammoeg.frostedheart.content.town.*` test domain: `334` tests passed.
- Full `test compileJava`: `386` tests passed with zero failures or errors; compilation passed.
- `git diff --check`: passed.

## Remaining

- Recheck menu spacing, all four scroll modifiers, upper-bound typing, and successful/rejected rate updates in the running client as the next H01 pass.
- Packet observation, disk-write observation, and multiplayer behavior remain untested under the existing environment limitations.
