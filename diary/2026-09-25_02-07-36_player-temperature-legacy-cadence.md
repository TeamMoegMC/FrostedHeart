# Restore legacy-paced player temperature with retained Wet cooling

- Time: `2026-09-25 02:07:36 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation collaborator`
- Status: `completed`
- Scope: `PlayerThermalModel`, `PlayerThermoregulation`, player temperature observation, HUD, health trend, Forge GameTest, climate documentation

## Completed

- Split the former shared player exchange multiplier into Air `0.0824`, water `0.51`, powder snow `0.116`, Wet `0.03`, lava `2`, direct radiation `2`, and internal transfer `2`. Kept the existing clothing score and its `100/(100+I)` factor.
- Changed normal-difficulty basal and walking power to `30.625 W` each; sprint has no additional power, matching the legacy executed branch. Restored stepped cold/hot regulation at core offsets `0.1/0.5/1 C` and its food/water exhaustion scale. Difficulty now scales basal and movement power as well as regulation.
- Changed the HUD temperature observation from still-air equivalent to sampled Thermal Air. The equivalent remains available to `/temperature get`; the body sync payload stays five bytes and the player save schema is unchanged. Lowered health-screen trend threshold to `0.0002 C` per elapsed game second.
- Extended the registered-clothing/campfire Forge GameTest to check sampled-Air HUD semantics and retained Wet cooling. Added a real-world player regulation GameTest using food availability.
- Updated `docs/climate/player-temperature.md` for current formulas, observations, defaults, and synchronization. No companion-pack recipe, data, script, or configuration file was changed.

## Decisions

- Retained Wet as an extra cooling path at a reduced multiplier after the user's clarification; water contact and Wet remain separate.
- Kept world heat-source power, wearable reservoirs, equipment power, body heat capacity, physiological time scale, and per-part energy storage unchanged. The calibration targets legacy early cold-weather cadence without reviving the old weather/wind formula defects.
- Kept direct radiation independent of the reduced Air conductance so a campfire can still warm clothed players. Preserved the environmental equivalent for diagnostics instead of presenting it as literal air temperature.

## Validation

- JDK 17 `compileJava compileGameTestJava --offline --no-daemon --max-workers=1 --console=plain`: passed.
- JDK 17 `runGameTestServer -PgameTestNamespaces=frostedheart --offline --no-daemon --max-workers=1 --console=plain`: natural random test world completed 103 tests with one required failure, `realChunkTicksFreezeOnlyTheScheduledSurfaceSample` (`surface sampling must still freeze an exposed cold water edge`). Both player-temperature production batches passed. A prior diary records this unrelated water-sampling test as world-dependent in a normal random test world.
- Repeated the full Forge GameTest server run with `run-gametest/server.properties` temporarily set to seed `0`, flat world: all 103 required tests passed. Restored the original blank seed and `minecraft:normal` settings afterward.
- `git diff --check` passed. No JUnit or isolated formula acceptance test was run.

## Remaining

- Inspect the HUD and health trend visually in the companion modpack client. Compare outdoor wind, nearby campfire, Wet clothes, sauna, and equipment over real play time before treating these source defaults as final balance.
- The water-surface test remains environment-dependent in a random natural GameTest world; this body-temperature change does not touch its world random-tick path.
