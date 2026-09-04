# Player Thermal Domain Classification

- Time: `2026-08-30 17:26:08 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `player temperature orchestration, model, environment, equipment, regulation, injury, and living documentation`

## Completed

- Removed `TemperatureComputation.ThermalStep` and restored the existing lazy
  `HeatingDeviceContext` as the only player-owned calculation scratch.
- Kept the human-extracted `PlayerThermalModel` as an independent stateless
  formula class and removed every dependency from it to the coordinator.
- Added stateless domain owners: `PlayerThermalEnvironment`,
  `PlayerEquipmentHeating`, `PlayerThermoregulation`, and
  `PlayerThermalInjury`.
- Reduced `TemperatureComputation` to explicit method-local orchestration with
  five documented phases. No universal data bag hides the values crossing
  environment, equipment, physiology, model, and publication boundaries.

## Decisions

- Prefer domain classes with explicit physical inputs over a mutable
  `ThermalStep`, even though some model calls require multi-line parameter
  lists. The parameters make dependencies visible and add no player memory.
- Domain classes are stateless and package-local; they add no runtime instances,
  NBT, packets, caches, collections, or per-update allocation.

## Validation

- `.\gradlew.bat compileJava --offline --console=plain`: passed with the
  repository's existing Mixin and deprecation warnings.
- Scoped `git diff --check`, conflict-marker census, and `ThermalStep` reference
  census passed.

## Remaining

- None for this domain-classification pass.
