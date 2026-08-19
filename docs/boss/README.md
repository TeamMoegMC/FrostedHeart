# Curiosity Encounter Documentation

This directory currently documents the world encounter named "Curiosity of the Deep Frostland." It connects encounter state, temperature mechanics, temporary arena changes, persistence, spawning, rendering, audio, and rewards.

## Reading Map

| Document | Use it for | Documentation status |
|---|---|---|
| [curiosity-boss-design.md](curiosity-boss-design.md) | Encounter intent, state machine, temperature interaction, arena behavior, configuration, integration, and implementation decisions | Transitional. The document began as a design plan and was updated while the encounter was implemented; verify exact current behavior against source. |

## Primary Code Anchors

- `CuriosityEntity` and related entity, model, and renderer classes
- `FHEntityTypes.CURIOSITY`
- `FHConfig.SERVER.CURIOSITY`
- `ChunkHeatData`, `SphereHeatArea`, and the surrounding-temperature pipeline

Human lore and world design under [`design/`](../../design/) are relevant background and are strictly read-only for agents.
