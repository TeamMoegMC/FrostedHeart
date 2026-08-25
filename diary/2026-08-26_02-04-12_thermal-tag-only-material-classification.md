# Tag-only thermal material classification

- Time: `2026-08-26 02:04:12 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `Minecraft gameplay material classification`

## Completed

- Removed every `SoundType` branch from `MinecraftThermalInput.classifyGameplayMaterial` and its category helpers.
- Removed the explicit air branch. Air now naturally returns no material profile through the same final `blocksMotion()` rule as other non-solid states.
- Reduced the pure JUnit material fixture to the contract it can verify without a loaded datapack tag set.

## Decisions

- Fabric, glass, metal, wood, earth, and masonry require explicit Minecraft, Forge, or Frosted Heart block tags.
- An untagged motion-blocking state uses `GENERIC_SOLID`; an untagged non-motion-blocking state receives no ordinary material profile.

## Validation

- Java 17 offline targeted JUnit passed `29/29`, including `MinecraftThermalInputTest`.
- `MinecraftThermalInput` contains no `SoundType`, `getSoundType`, or `state.isAir()` classification branch.

## Remaining

- Modded blocks needing a specific category should provide a shared block tag; no sound-based compatibility fallback is retained.
