# Research JEI revoke visibility

- Time: `2026-08-25 22:20:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Frosted Research V2 Design revoke and JEI recipe visibility`

## Completed

- Fixed JEI recipe visibility refresh after a V2 Design is revoked.
- Replaced the recipe-object identity lookup with an index of the actual JEI runtime registrations keyed by stable Minecraft recipe ID.
- Grant, revoke, catalogue removal, and overlapping legacy/V2 sources now update the JEI entries registered for the affected recipe ID.
- Updated the living integration documentation with the stable-ID matching contract.

## Decisions

- Access remains recipe-ID-specific. Other recipes producing the same output remain visible unless their own IDs are managed and locked.

## Validation

- `./gradlew compileJava` passed.

## Remaining

- Recheck `frostedresearch_test:smoke_design` in a client because JEI runtime visibility cannot be asserted by the headless unit-test suite.
