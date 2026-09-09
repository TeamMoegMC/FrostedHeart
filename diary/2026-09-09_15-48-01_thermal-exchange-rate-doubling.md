# Thermal exchange rate doubling

- Time: `2026-09-09 15:48:01 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `PlayerThermalModel`, wearable thermal profiles, thermal regression tests, and climate documentation

## Completed

- Added a `2.0` gameplay multiplier to warm-stone and hot-water-bag core/surface and surface/player exchange rates; inventory and exposed environment rates derive from the updated surface/player values.
- Doubled finalized passive player conductances for air, water, powder snow, lava, and Wet, and doubled conservative internal body-part transfers.
- Kept environment observation, active heat power, and the separate world thermal runtime unchanged.
- Updated the wearable parameter tests, reproducible thermal curves, and the player-temperature living document.

## Decisions

- Applied the player multiplier after clothing/tissue series resistance is calculated, so clothing resistance keeps its existing relationship while the resulting exchange rate doubles.
- Kept the two multipliers explicit in their owning local models; no global world-solver conductance change was introduced.

## Validation

- `compileJava --offline --no-daemon --max-workers=1 --console=plain`: passed with Java 17.0.2.
- `compileGameTestJava --offline --no-daemon --max-workers=1 --console=plain --init-script build/warm-stone-verification.init.gradle`: passed.
- Focused thermal JUnit suite through the warm-stone verification init script: `82/82` passed.
- `git diff --check`: passed.

## Remaining

- No interactive client run was performed for this numerical balance change.
