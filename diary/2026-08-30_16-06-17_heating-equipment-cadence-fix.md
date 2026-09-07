# Heating Equipment Cadence Fix

- Time: `2026-08-30 16:06:17 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `HeatingDeviceContext, six BodyHeatingCapability implementations, player temperature documentation`

## Completed

- Made heating-pad, oxygen-candle, steam-bottle, hand-stove, mushroom-bed, and
  heater-vest resource use proportional to real elapsed seconds instead of one
  fixed unit per player-temperature update.
- Preserved every rated power and the default 20-tick consumption behavior.
  Partial intervals store only `frostedheart:partial_heating_second`; the key is
  absent at whole-second cadence and removed when its value returns to zero.
- Prevented equipment resource use when physiological simulation time is zero.
- Allowed the coal hand stove to consume its final fuel unit and bounded ash at
  its existing capacity.

## Decisions

- Reuse `HeatingDeviceContext` for elapsed-time scaling and one synchronous
  timer result. Do not add another capability, timer class, per-tick scan, cache,
  or bidirectional device/body thermodynamics.
- Keep primary item NBT keys and all existing item powers unchanged.

## Validation

- Static caller census found exactly six production `tickHeating`
  implementations and confirmed all six use the shared elapsed time.
- Scoped `git diff --check` passed with line-ending warnings only.
- Full `compileJava` reached unrelated concurrent Thermal geometry errors in
  `ThermalSignatureCatalog`; it reported no errors in the equipment or player
  temperature files changed here.

## Remaining

- Rerun `compileJava` after the concurrent Thermal geometry work compiles.
