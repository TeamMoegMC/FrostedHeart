# Town Implementation Reference

> Status: Partial
> Last verified: 2026-08-20
> Scope: State ownership, daily lifecycle, building extension, and town-data synchronization
> Code anchors: `TeamTownData`, `TeamTown`, `ITownBuilding.CODEC`, `DataSyncCache`, `TownTransportState`, `TownResourceUpdatePacket`

This document records invariants that are easy to violate when extending the town system. Source remains authoritative; gameplay formulas and simulator behavior belong in [town-model.md](town-model.md), while citizen presence and movement belong in [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md).

## State And Entry Points

`TeamTownData` owns persistent town state through team `SpecialData`: buildings, residents, resources, history, plans, policies, transport state, and settlement-day state. `TeamTown` is the `ITown` facade used to mutate that state. Do not create a second persistent owner. `TownTransportState` belongs to the town and holds the aggregate daily transport report; an individual `TransportStationBuilding` owns only its station report.

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

Inside `TeamTownData#buildingsWork`, service reconstruction has a stricter nested order:

1. `TeamTownResourceHolder#resetAllServices` clears all service resources, including `MAX_CAPACITY` and `TRANSPORT_CAPACITY`.
2. `TeamTownData#reloadMaxCapacity` rebuilds warehouse capacity.
3. Every non-house building whose `shouldRunDailySettlement()` is true executes once in work-priority order. A workable `TransportStationBuilding` adds its deterministic output through `TownResourceActions.VirtualResourceAttributeAction`; it never writes the holder directly.
4. `TeamTownData#finishDailyTransportSettlement` snapshots town total transport capacity and reserved capacity into `TownTransportState.DailyReport`. Reserved capacity is `0` until transport consumers are implemented.

`TRANSPORT_CAPACITY` is a daily service, not a consumable stock. It does not use warehouse capacity, does not carry into the next settlement, and future consumers reserve capacity instead of applying a resource `COST` action.

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

The station report is persisted and incrementally synchronized as part of its building object. The town aggregate report is persisted in `TeamTownData.transportState` and piggybacks on `TownResourceUpdatePacket`; the packet is sent when either a resource key or the aggregate report changes. Client application updates both the resource holder and `TownTransportState` before firing the shared resource-data callback.

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
