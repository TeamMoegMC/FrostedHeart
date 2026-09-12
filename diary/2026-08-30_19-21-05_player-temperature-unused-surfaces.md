# Player Temperature Unused Surfaces

- Time: `2026-08-30 19:21:05 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation agent`
- Status: `completed`
- Scope: `legacy radiation probes and unused thermal status synchronization`

## Completed

- Removed the detached radiant-temperature compatibility methods and their
  only legacy test.
- Removed unused thermal status calculation, player fields, sync comparison,
  packet byte, and client assignment.
- Updated the living climate documents for the remaining 5-byte body packet.

## Decisions

- Kept environment and absolute core temperature synchronization because both
  have current client consumers.
- Did not change player heat formulas, HUD behavior, equipment, Wet, or body
  integration.

## Validation

- Scoped reference and trailing-whitespace checks passed.
- `./gradlew.bat compileJava --offline --console=plain`: passed with existing
  Mixin and deprecation warnings.

## Remaining

- None for the two approved removals.
