# Restore Environmental Temperature Orb Colors

- Time: `2026-08-30 03:49:03 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `FrostedHud`, player thermal client sync, body data packet, climate documentation

## Completed

- Restored the temperature orb's existing cold-to-hot texture selection from
  the environmental equivalent Celsius value.
- Removed the unused client net-power presentation, deadband, and client packet
  field. Server-side net body power remains available to body calculation and
  the temperature diagnostic command.
- Reduced `FHBodyDataSyncPacket` to the environment, core temperature, and
  status fields required by the HUD and existing client state.

## Decisions

- The HUD number and orb color intentionally share the environmental
  temperature source. This prevents fast net-power changes from making the
  visual temperature jump while preserving the existing HUD layout.

## Validation

- Confirmed no source or living-document references remain to the removed
  client net-power presentation APIs.
- No build or test command was run for this focused display correction.

## Remaining

- None for the requested HUD behavior. Broader ambient lava and ordinary-fire
  topology remains outside this correction.
