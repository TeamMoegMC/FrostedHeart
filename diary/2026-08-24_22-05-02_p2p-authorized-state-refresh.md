# P2P Authorized State Refresh

- Time: `2026-08-24 22:05:02 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P terminal authorization, visual-state refresh, GUI regression tests, plans, and living docs`

## Completed

- Traced the remaining unbound flash on menu open and rate changes to
  `P2PTerminalBlockEntity#claimOrAuthorize`, which refreshed the visual state with null town and
  binding arguments after every successful authorization.
- Replaced that forced-unbound refresh with `refreshVisualStateFromTown`, which resolves the
  authoritative town and passes its current `P2PBindingState` into the existing state selector.
  A genuinely unresolved town still follows the unbound path.
- Added the missing Minecraft registry bootstrap to `P2PTerminalScreenTest` so the focused class no
  longer depends on another test running first, and added the normal bound-to-idle assertion.
- Updated the P2P living document and both implementation plans with the corrected cause and H01
  fourth-round status.

## Decisions

- Fixed the server fact producer instead of suppressing `UNBOUND` frames in the client. The observed
  frame was an actual block-state write, not merely a temporary menu snapshot placeholder.
- Kept the existing null pre-sync menu sentinel and client BlockState fallback; those still cover the
  interval before the first authoritative menu snapshot and are separate from this server bug.

## Validation

- Focused `P2PTerminalScreenTest`: 7 tests passed.
- Java 17 full regression: 536 tests in 139 suites passed with zero failures, errors, or skips.
- `compileJava` and `compileTestJava` passed; only existing compiler warnings were reported.

## Remaining

- Recheck a bound terminal in game by opening it, applying a typed rate, scrolling the rate field, and
  changing a filter. None of those interactions should display or render `UNBOUND` for one frame.
