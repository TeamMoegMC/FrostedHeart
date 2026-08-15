# Refresh wandering-refugee dimensions after age synchronization

- Time: `2026-08-08 17:42:13 UTC+8`
- Author: `Codex; OpenAI GPT-5; primary coding agent`
- Status: `completed`
- Scope: `content/town/resident/WanderingRefugee`

## Completed

- Added an `onSyncedDataUpdated` handler for the custom `AGE` entity-data field.
- Age changes now call `refreshDimensions()`, updating the cached hitbox and eye height
  when the server assigns an age, NBT restores it, or the client receives synchronized data.

## Decisions

- Followed vanilla `AgeableMob`'s synchronized baby-data pattern instead of refreshing only
  inside `setAgeGroup`; the callback covers both server and client synchronization paths
  without duplicate refreshes.

## Validation

- `./gradlew compileJava --rerun-tasks` passed; only existing deprecation and unchecked
  warnings were emitted.
- `git diff --check` passed.

## Remaining

- In-game verification should compare infant/child selection boxes, collision, doorway
  movement, and eye height against the visually scaled model.
