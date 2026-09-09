# Thermal reservoir rate retune

- Time: `2026-09-09 18:47:23 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `WearableThermalProfile`, wearable thermal regression fixtures, and the player-temperature living document

## Completed

- Raised the final core/surface transfer multiplier for both warm reservoirs to `4.0` relative to the original constants.
- Raised the final surface/player transfer multiplier for both warm reservoirs to `5.0` relative to the original constants.
- Kept inventory and dropped/placed environment conductances derived from surface/player transfer: `0.5x` and `8x` respectively.
- Updated the frozen half-life assertions, direct player-rate assertions, reproducible curves, and climate documentation.
- Kept the player environmental exchange model at its previously selected `2.0x` multiplier; this change only retunes reservoir profiles.

## Final source values

- Warm stone: core/surface `2.46452e-4 /s`, surface/player `6.0e-4 /s`, inventory environment `3.0e-4 /s`, dropped/placed environment `4.8e-3 /s`.
- Hot-water bag: core/surface `3.6968e-3 /s`, surface/player `4.0e-4 /s`, inventory environment `2.0e-4 /s`, dropped/placed environment `3.2e-3 /s`.
- Isolated core/surface half-lives are `45 s` and `7.5 s` for the warm stone and hot-water bag.

## Validation

- Warm-stone verification init-script selection: `82/82` JUnit tests passed.
- `compileJava` and `compileGameTestJava` passed on Java 17.0.2 in offline mode.
- Curve fixture was regenerated from the actual test output after the final profile change.
- `git diff --check` passed.

## Remaining

- No interactive client balance run was performed for this numerical retune.
