# Town System Documentation

Town management covers residents, housing, work, production, storage, nutrition, heating, daily settlement, citizen presence, and the Mayor's Seal.

| Document | Scope | Status |
|---|---|---|
| [town-model.md](town-model.md) | Mechanics, formulas, simulator, and balance evidence | Transitional |
| [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md) | Citizen behavior, movement, rendering, synchronization, and persistence | Transitional |
| [citizen-rendering-at-scale.md](citizen-rendering-at-scale.md) | Current 1,000-visible render pipeline, Flywheel instancing backend, validation contract, and remaining rollout path | Transitional |
| [implementation-reference.md](implementation-reference.md) | State ownership, lifecycle, extension points, and town-data synchronization | Partial |
| [p2p-logistics.md](p2p-logistics.md) | P2P terminals, pairing, capacity, buffers, filters, transfer, and lifecycle | Current |

Primary anchors: `TeamTownData`, `TeamTown`, `TownModelParameters`, `CitizenSim`, `FHConfig.SERVER.TOWN`.

Verify transitional claims against source. Inspect the companion repository when recipes, loot, datapacks, or pack configuration supply town inputs.
