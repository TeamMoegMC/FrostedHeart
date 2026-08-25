# Campfire power gameplay calibration

- Time: `2026-08-25 18:51:28 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `Minecraft physical Campfire source profile, energy-ledger tests, climate documentation, and thermal implementation plan`

## Completed

- Raised the Campfire physical-source profile from `1,000 W` to `8,000 W`.
- Kept the existing `80/20` partition, yielding `6,400 W` convection and `1,600 W` direct radiation.
- Updated the profile contract and Forge energy-ledger assertions for the new power.
- Updated the living heat-production documentation and Phase G implementation snapshot.

## Decisions

- Treat `8,000 W` as a provisional gameplay calibration for enclosed-room heating.
- Do not change radiation response coefficients, ray budgets, source ownership, energy accounting, phase-transition temperatures, latent heat, or mutation policy.
- The calibration adds no runtime object, lookup table, scan, ray, or memory reservation.

## Validation

- Targeted Java 17 JUnit passed `239/239` tests with zero failures, errors, or skips.
- Forge GameTest passed all `19/19` required tests, including the updated Campfire air and declared-loss energy ledger.
- `git diff --check` reported no whitespace errors; existing LF-to-CRLF notices remain.

## Remaining

- Verify the resulting HUD and enclosed-room warm-up curve in a real save and recalibrate the single rated-power constant if the gameplay response is still too weak or too strong.
