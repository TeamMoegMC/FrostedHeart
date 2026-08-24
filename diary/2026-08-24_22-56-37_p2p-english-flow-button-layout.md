# P2P English Flow And Button Layout

- Time: `2026-08-24 22:56:37 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P endpoint flow wrapping, bidirectional unlink layout, peer-filter tooltip, plans, and living docs`

## Completed

- Replaced the shipping and receiving terminal's fitted connection text with the same normal-size,
  width-aware semantic wrapping used by the bidirectional terminal.
- Expanded endpoint connection rows to 32 pixels so a wrapped direction and its coordinate retain
  normal line spacing while two endpoint records remain visible.
- Moved the bidirectional unlink button from the flow-text area to the coordinate row. The English
  `/s` suffix can no longer render underneath the button.
- Removed the peer-filter item preview from the constrained text area. Hovering the connection row
  now shows the same read-only peer-filter tooltip.
- Updated the P2P living document, source plan, and task checklist for the seventh H01 feedback round.

## Decisions

- Every terminal role follows the same rendered-width rule: draw the complete direction at normal
  size when it fits, otherwise draw source and target-rate semantic lines at normal size.
- The unlink command remains directly available per connection, but it must not reserve space inside
  a localized flow line.

## Validation

- Focused `P2PTerminalScreenTest` and `P2PTerminalResourcesTest`: passed.
- Java 17 full regression: 537 tests in 139 suites passed with zero failures, errors, or skips.
- `git diff --check`: passed.

## Remaining

- Reopen shipping, receiving, and bidirectional terminals in English at the active GUI scale and
  confirm normal-size flow text, semantic wrapping, unlink placement, and peer-filter hover behavior.
