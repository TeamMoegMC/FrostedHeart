# Refugee settlement adopts the logical climate day

- Time: `2026-08-08 17:29:08 UTC+8`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `content/climate/event/ClimateCommonEvents`, `content/town`, wandering-refugee persistence

## Completed

- Moved `WorldClockSource` advancement ahead of the town loop and update it every
  server-level tick, so a sleep jump or `/time set` rollback is normalized before
  `tickMorning` reads the logical date. Climate cache/network work remains once per second.
- Switched refugee spawn deduplication, refugee waiting-day settlement, and town history
  snapshots to `WorldClimate.getWorldDay`.
- Changed persisted refugee day markers to `long`; `lastRefugeeSpawnDay` now defaults to
  `-1` so logical day zero can settle, and entity NBT still accepts the old integer tag.
- Updated the town subsystem README to describe the logical-day codec.

## Decisions

- Reused the existing persisted `WorldClockSource` instead of deriving a day independently
  from `gameTime` or raw `dayTime`.
- Kept `updateCache` and climate packet work on the existing 20-tick cadence; only the cheap
  clock delta ingestion now runs each tick to guarantee ordering before town settlement.
- Preserved the contributor's unrelated `FHConfig` edits without modification.

## Validation

- `./gradlew compileJava --rerun-tasks` passed; only existing deprecation and unchecked
  warnings were emitted.
- `git diff --check` passed before the diary entry was added.

## Remaining

- In-game validation: repeat `/time set 999t` and confirm the logical day advances before
  the morning refugee guard; also verify a normal sleep advances refugee waiting days once.
