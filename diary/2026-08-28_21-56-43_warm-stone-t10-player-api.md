# Warm stone T10 player core adjustment API

- Time: `2026-08-28 21:56:43 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent, with A10-player-api sub-agent`
- Status: `completed`
- Scope: `PlayerTemperatureData core-part adjustment and aggregate ownership`

## Completed

- Added `applyCoreBodyTemperatureDelta(float)` to atomically adjust head, torso, and legs while immediately refreshing the core aggregate.
- Refactored the normal `update()` path to share one private core-temperature recalculation method with the new API.
- Added focused tests for positive, negative, zero, nonfinite, overflow, endpoint isolation, and previous-core history behavior.

## Decisions

- Invalid or overflowing adjustments return `false` without partial mutation; valid zero adjustments return `true` and can repair a stale aggregate.
- Hands, feet, and `prevCoreBodyTemp` are outside the new API's ownership. Existing `BodyPart.affectsCore` values remain the sole weighting authority.
- Living climate docs were not changed because T10 is an unconnected internal API; T11 owns the first player-visible use.

## Validation

- A10's JDK 17 targeted run passed the new tests `5/5` plus the player radiation regression `1/1`.
- Primary-agent JDK 17 review run passed `15/15` across the new test, the radiation regression, and `ThreeNodeWearableHeatExchangeTest`.
- Scoped whitespace validation for the T10 source and test is clean. Unrelated concurrent Curios work retains its existing line-ending diff and was not modified.

## Remaining

- T11 must invoke this API after normal body updates while preserving invulnerable and insulation semantics, then measure Stack writeback and synchronization behavior for Gate B.
