# Curiosity Encounter Documentation

This directory covers the "Curiosity of the Deep Frostland" encounter: its state machine, arena, temperature effects, persistence, presentation, and rewards.

| Document | Scope | Status |
|---|---|---|
| [curiosity-boss-design.md](curiosity-boss-design.md) | Design intent plus accumulated implementation decisions | Transitional |

Primary anchors: `CuriosityEntity`, `FHEntityTypes.CURIOSITY`, `FHConfig.SERVER.CURIOSITY`, `MinecraftThermalInput.AnalyticField`, `AnalyticCombineMode.ADD_DELTA`.

Verify behavior against source; use read-only [`design/`](../../design/) for lore and creative intent.
