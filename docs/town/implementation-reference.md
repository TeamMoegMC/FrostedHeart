# Town Implementation Reference

> Status: Partial
> Last verified: 2026-08-19
> Scope: State ownership, daily lifecycle, building extension, and town-data synchronization
> Code anchors: `TeamTownData`, `TeamTown`, `ITownBuilding.CODEC`, `DataSyncCache`

This document records invariants that are easy to violate when extending the town system. Source remains authoritative; gameplay formulas and simulator behavior belong in [town-model.md](town-model.md), while citizen presence and movement belong in [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md).

## State And Entry Points

`TeamTownData` owns persistent town state through team `SpecialData`: buildings, residents, resources, history, plans, policies, and settlement-day state. `TeamTown` is the `ITown` facade used to mutate that state. Do not create a second persistent owner.

Town buildings are serialized polymorphically through `ITownBuilding.CODEC`. Their block entities create and register logical building objects. Removal must use `TeamTown.removeTownBlock`; direct map removal bypasses resident assignments, mine links, warehouse capacity, and occupied-area cleanup.

Resource mutations use `TeamTownResourceActionExecutorHandler` and `TownResourceActions`. `TownResourceManager` and direct `addUnsafe` or `costUnsafe` calls are not extension APIs.

## Daily Lifecycle

`TeamTownData.tickMorning` is the settlement boundary. Its order is behavior:

1. Validate buildings and occupied-area overlap.
2. Activate pending policy, apply daily nutrition loss, allocate housing, and assign work.
3. Update refugees, mine links, and terrain resources.
4. Run production buildings and centralized `TownHousingMealService` food settlement.
5. Apply aging, recovery, resident exit, resource recovery, history, and signal events.

Changing this order changes gameplay contracts and requires updates to tests, [town-model.md](town-model.md), and the development diary.

## Adding A Building

1. Implement the logical `<Name>Building`, normally from `AbstractTownBuilding` or `AbstractTownResidentWorkBuilding`.
2. Define its `Codec` and register a stable key in `ITownBuilding.CODEC`.
3. Implement the block and `AbstractTownBuildingBlockEntity`; add a scanner only when the structure requires one.
4. Register the block, block entity, tags, and optional menu and screen through `FHBlocks`, `FHBlockEntityTypes`, `FHRegistrateTags`, `FHMenuTypes`, and `FHScreens` as applicable.
5. Route resource changes through actions and lifecycle removal through `TeamTown.removeTownBlock`.
6. Test codec round trips, registration/removal cleanup, work eligibility, resource capacity, and client synchronization affected by the building.

`isBuildingWorkable()` requires initialization, a valid structure, and no occupied-area overlap. Residential buildings use centralized housing and meal settlement; they are not ordinary work buildings.

## Synchronization Invariants

`ObservableTownMap`, object change listeners, and resource listeners feed the single dirty-state owner, `TeamTownData.DataSyncCache`. Server `tick()` flushes dirty keys through `TownBuildingUpdatePacket`, `TownResidentUpdatePacket`, and `TownResourceUpdatePacket`. Full `TeamTownDataS2CPacket` synchronization is a recovery and entry-point mechanism, not the normal per-tick path.

New or changed building and resident setters must return when the value is unchanged, then call `fireChange()` after a real change. Some legacy setters do not yet apply that guard. Resource updates receive additional value-level deduplication; buildings and residents do not.

Client building screens resolve the synchronized snapshot through `AbstractTownBuildingBlockEntity.getTown()` and `CClientTeamDataManager`. They must not use server-only `CTeamDataManager`. Data panels should read current values through suppliers during rendering; data-change callbacks must not rebuild the content layer and discard scroll or selection state.

## Investigation Anchors

| Concern | Start at |
|---|---|
| Persistent state and settlement | `TeamTownData`, `TeamTown` |
| Buildings and codecs | `building/ITownBuilding`, `block/AbstractTownBuildingBlockEntity` |
| Residents and nutrition | `resident/Resident`, `resident/ResidentNutrition` |
| Resources and storage | `resource/TeamTownResourceHolder`, `resource/action`, `buildings/warehouse` |
| Citizen presence | `citizen/sim`, `citizen/sync`, `citizen/client` |
| Operational history | `observation`, `TownHistoryEntry` |
| Network packets | `network`, `FHNetwork` |
