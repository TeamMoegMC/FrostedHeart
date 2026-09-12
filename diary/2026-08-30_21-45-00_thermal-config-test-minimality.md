# Thermal config test minimality

- Time: `2026-08-30 21:45:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal config unit-test surface only`

## Completed

- Removed `configuredCampfireFreezesPowerAndOneRadiationShare`. It asserted
  factory arithmetic rather than the actual config-to-runtime gameplay path.

## Decisions

- Constructor validation and the existing startup/GameTest path are sufficient;
  simple configured-share arithmetic does not justify a dedicated test.
- Production config, profile, source-index, and worker code are unchanged.

## Validation

- Thermal JUnit: pending rerun.
- `git diff --check`: pending rerun.

## Remaining

- None.
