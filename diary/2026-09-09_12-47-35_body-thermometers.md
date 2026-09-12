# Body thermometer precision and creative thermometer

- Time: `2026-09-09 12:47:35 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `ThermometerItem`, `CreativeThermometerItem`, temperature display packet, item registration, language and climate documentation

## Completed

- Changed floating-point `temperature_display` messages to retain one-decimal formatting while preserving integer formatting for integer messages.
- Added `frostedheart:creative_thermometer`, which reports the absolute core body temperature immediately on server-side right-click in any game mode.
- The creative thermometer sends `Float.toString` output directly through a localized server component, avoiding the normal packet's one-decimal quantization.
- Added the item model, localized name/message/tooltip entries, and updated the player-temperature and network lifecycle documentation.

## Decisions

- Kept the existing 100-tick held measurement for the mercury body thermometer.
- Kept the creative thermometer's raw output in Celsius so the diagnostic value is not converted or rounded by the client unit setting.
- Reused the existing mercury thermometer texture for the new item's model; no new visual asset was required.
- Documentation impact: updated [`docs/climate/player-temperature.md`](../docs/climate/player-temperature.md) and [`docs/climate/data-lifecycle-and-integration.md`](../docs/climate/data-lifecycle-and-integration.md).

## Validation

- `JAVA_HOME=C:\Program Files\Java\jdk-17.0.2 gradlew.bat compileJava --offline --no-daemon --max-workers=1 --console=plain`: passed.
- `gradlew.bat processResources --offline --no-daemon --max-workers=1 --console=plain`: passed; existing duplicate model-path warnings remain.
- `JAVA_HOME=C:\Program Files\Java\jdk-17.0.2 gradlew.bat compileGameTestJava --offline --no-daemon --max-workers=1 --console=plain`: passed.
- Language and model JSON parsing passed; `git diff --check` passed for the changed tracked files.
- `compileTestJava` remains blocked by existing missing `ConservativeAirGeometry` and `ComponentBrickCompiler` test dependencies; no test source was changed here.

## Remaining

- None for the requested implementation. In-game manual interaction across survival, creative, adventure, and spectator modes was not run in this validation.
