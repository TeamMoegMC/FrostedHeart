# P2P English Flow Wrapping

- Time: `2026-08-24 22:45:03 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P localized connection wrapping, peer-filter preview layout, resources, tests, plans, and living docs`

## Completed

- Added width-aware rendering for bidirectional flow labels. A complete localized direction stays on
  one normal-size line when it fits within 152 pixels; otherwise it becomes a source-arrow line and a
  target-rate line.
- Expanded the bidirectional connection row to 46 pixels, leaving room for four English direction
  lines plus the coordinate line before the rate controls.
- Moved the peer-filter item preview to the lower-right corner of the row and updated its tooltip hit
  area so it does not cover the English flow text.
- Added Chinese and English localization keys for both semantic line fragments and covered them in
  resource tests.

## Decisions

- Wrapping is based on rendered width, not the active locale. This keeps short translations compact
  while giving any long translation the same overflow behavior.
- Direction text remains at the normal font size. Overflow is handled by semantic line breaks rather
  than scaling or clipping.

## Validation

- Focused `P2PTerminalScreenTest` and `P2PTerminalResourcesTest`: passed.
- Java 17 full regression: 537 tests in 139 suites passed with zero failures, errors, or skips.

## Remaining

- Reopen the bidirectional terminal in English and confirm the four direction fragments, coordinate,
  unlink button, and filter preview do not overlap at the active GUI scale.
