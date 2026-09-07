# Player body-energy and HUD first pass

- Time: `2026-08-30 03:24:47 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `player temperature capability, five-part body calculation, HUD, body sync, clothing, Wet/contact inputs, heating equipment, food, effects, diagnostics, and living climate documentation`

## Completed

- Replaced the mixed relative/absolute Celsius player update in place with one
  five-part body-energy calculation using explicit `J`, `W`, `W/K`, `W/m2`,
  `m2*K/W`, `m/s`, and absolute Celsius boundaries.
- Kept the existing clothing inventories, equipment stacks and NBT, difficulty,
  effects, food interactions, device fuel/storage logic, and Campfire
  source/radiation path. Old transient body/environment values are deliberately
  ignored while clothing and difficulty survive load and clone.
- Changed the HUD number to environmental equivalent Celsius and the existing
  orb color to synchronized net body-power direction with a `15 W` deadband and
  20-tick client hold. The body packet is an 8-byte quantized payload sent only
  when changed on the temperature cadence plus lifecycle synchronization.
- Added fixed body immersion bands, stable closed-form contact integration,
  coarse Wet exchange with a shared evaporation ceiling, `canSeeSky` wind
  gating, explicit equipment watts, and food joules.
- Moved reusable calculation scratch into each player's lazy
  `HeatingDeviceContext`, cached enum iteration arrays, and removed duplicate
  lifecycle packet transmission without adding a parallel thermal model.
- Set the unified gameplay time scale to `8`, targeting the first naked dry
  torso cold threshold after roughly `45..60 s` in calm `-15 C` Air.

## Decisions

- The many primitive `static final` values in `TemperatureComputation` are
  immutable model constants, not per-player mutable state. Keeping them inline
  avoids a parameter object and runtime allocation; mutable arrays remain
  player-owned.
- `INSULATION`, creative, spectator, and invulnerable states continue sampling
  the environment while freezing body energy and suppressing climate injury.
- The health menu now reads relative part temperature through
  `PlayerTemperatureData.getBodyTempByPart`; the removed duplicate
  `BodyPartData.temperature` field was not restored for compatibility.
- Nearby ambient lava and ordinary-fire Air/radiation are outside this pass.
  Local entity contact remains a primitive body input, and Campfire remains on
  its existing source/radiation implementation.

## Validation

- `git diff --check` completed with line-ending warnings only and no whitespace
  errors.
- Static caller census found no remaining player-temperature uses of
  `getMaxTempAddValue`, `getMinTempAddValue`, `addEffectiveTemperature`, or the
  removed `BodyPartData.getTemperature` API.
- `./gradlew.bat build --offline --console=plain` reached `compileJava` and
  reported seven migration errors: two non-final food lambda captures and five
  health-menu calls to `BodyPartData.getTemperature`. All seven reported sites
  were corrected afterward. Per the requested single production-build run, the
  Gradle build was not invoked a second time.

## Remaining

- Rerun the production build when another build invocation is allowed; the
  post-build compile fixes have only static verification in this pass.
- Calibrate the implemented cold-air, wind, water, Wet, Campfire recovery, and
  equipment-power curves in real gameplay without adding scenario timers.
- Implement topology-aggregated nearby lava and ordinary-fire Air/radiation in
  the next pass; do not alter or duplicate Campfire's current source path.
