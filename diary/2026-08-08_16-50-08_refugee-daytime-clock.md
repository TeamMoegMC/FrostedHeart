# Refugee daily settlement uses the town morning clock

- Time: `2026-08-08 16:50:08 UTC+8`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `content/town/TeamTownData`, `content/town/resident/WanderingRefugee`

## Completed

- Changed automatic refugee refresh deduplication from `gameTime / 24000` to
  `dayTime / 24000`, matching the clock that triggers `tickMorning`.
- Applied the same date source to debug-spawn bookkeeping and refugee waiting-day
  settlement, so sleeping through the night advances both refresh and departure days.
- Updated comments to document the shared `dayTime` clock.

## Decisions

- Used `dayTime` directly as requested, prioritizing consistency with the existing
  town morning scheduler and normal sleep-based day advancement.
- Kept the existing persisted integer fields and NBT/Codec formats unchanged.

## Validation

- `./gradlew compileJava --rerun-tasks` passed; only the existing deprecation and
  unchecked-operation warnings were emitted.
- `git diff --check` passed for the change.

## Remaining

- In-game validation should confirm one refresh/wait settlement after sleeping across
  a day boundary.
- The existing `lastRefugeeSpawnDay = 0` sentinel still suppresses the first world
  day's roll; changing that behavior was outside this focused clock-source fix.
