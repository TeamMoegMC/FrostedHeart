# P2P Bidirectional Flow Lines

- Time: `2026-08-24 22:29:00 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P bidirectional connection layout, GUI tests, plans, and living docs`

## Completed

- Replaced the combined bidirectional connection sentence with two independent
  `connection_flow` components, preserving sender-to-receiver order and the full rate unit on each
  line.
- Gave the bidirectional related-terminal row 32 pixels of height and placed its coordinates on the
  third line. Direction text now uses the normal font renderer instead of `drawFittedString`.
- Kept endpoint rows at 22 pixels with two visible records so receiving terminals do not lose list
  density for their multiple sources.
- Added a regression test for the two bidirectional lines and their local/peer argument order.

## Decisions

- The two directions are separate facts and therefore separate lines. Scaling one combined sentence
  made the most important connection information harder to read and was not an appropriate overflow
  strategy.

## Validation

- Focused `P2PTerminalScreenTest`: 8 tests passed.
- Java 17 full regression: 537 tests in 139 suites passed with zero failures, errors, or skips.
- The reused 6 GiB Gradle daemon first exhausted native memory. Validation was rerun successfully in
  a single-use 3 GiB process; this was an environment failure, not a compilation or test failure.

## Remaining

- Reopen a bidirectional connection in the Chinese client and confirm both normal-size direction
  lines and the coordinate line have clear spacing at the active GUI scale.
