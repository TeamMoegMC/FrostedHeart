# Health-screen core temperature trend

- Time: `2026-09-22 21:24:09 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation collaborator`
- Status: `completed`
- Scope: `HealthStatMenu`, `HealthStatScreen`, English/Chinese localization, player-temperature documentation

## Completed

- Added rising/stable/falling core-temperature text below the health-screen body and nutrition widgets.
- Reused the server's current/previous core-temperature observations and existing menu integer slot synchronization. The completed observation includes subsequent equipped warm-stone/hot-water-bag exchange.
- Documented the trend in the living player-temperature reference and linked it from the nutrition interface reference.

## Decisions

- The trend describes core temperature direction, independently of environmental HUD temperature and body-status colors. Changes within `0.001 C` per elapsed game second count as stable.
- Clothing parameters and body physics remain pending discussion; this work implements only the requested health-screen observation.

## Validation

- `gradlew.bat compileJava --offline --no-daemon --max-workers=1 --console=plain` with Java 17: passed (21 existing compiler warnings).
- Both edited localization JSON files parse successfully and contain all three trend keys.
- `git diff --check`: passed.
- Checked the existing body-step/reservoir ordering and matching client/server menu-slot registration.

## Remaining

- Visual inspection in a running game was not performed.
- Clothing balance and any core/skin model changes require the separate design decisions discussed with the user.
