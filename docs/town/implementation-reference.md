# Town Implementation Reference

> Status: Partial
> Last verified: 2026-08-22
> Scope: State ownership, daily lifecycle, building extension, transport reservations, and town-data synchronization
> Code anchors: `TeamTownData`, `TeamTown`, `ITownBuilding.CODEC`, `DataSyncCache`, `TownTransportState`, `TownTransportSnapshot`, `TownResourceUpdatePacket`, `WarehouseInterfaceBlockEntity`, `WarehouseInterfaceTransportView`

This document records invariants that are easy to violate when extending the town system. Source remains authoritative; gameplay formulas and simulator behavior belong in [town-model.md](town-model.md), while citizen presence and movement belong in [hybrid-simulation-architecture.md](hybrid-simulation-architecture.md).

## State And Entry Points

`TeamTownData` owns persistent town state through team `SpecialData`: buildings, residents, resources, history, plans, policies, transport state, and settlement-day state. `TeamTown` is the `ITown` facade used to mutate that state. Do not create a second persistent owner. `TownTransportState` belongs to the town and holds both endpoint reservations and the aggregate daily transport report; an individual `TransportStationBuilding` owns only its station report.

Town buildings are serialized polymorphically through `ITownBuilding.CODEC`. Their block entities create and register logical building objects. Removal must use `TeamTown.removeTownBlock`; direct map removal bypasses resident assignments, mine links, warehouse capacity, and occupied-area cleanup.

Resource mutations use `TeamTownResourceActionExecutorHandler` and `TownResourceActions`. `TownResourceManager` and direct `addUnsafe` or `costUnsafe` calls are not extension APIs.

## Daily Lifecycle

`TeamTownData.tickMorning` is the settlement boundary. Its order is behavior:

1. Validate buildings and occupied-area overlap.
2. Activate pending policy, apply daily nutrition loss, allocate housing, and assign work.
3. Update refugees, mine links, and terrain resources.
4. Run production buildings, centralized ration allocation, and priority-ordered house menus through `TownHousingMealService`.
5. Apply aging, recovery, resident exit, resource recovery, history, and signal events.

Changing this order changes gameplay contracts and requires updates to tests, [town-model.md](town-model.md), and the development diary.

Inside `TeamTownData#buildingsWork`, service reconstruction has a stricter nested order:

1. `TeamTownResourceHolder#resetAllServices` clears all service resources, including `MAX_CAPACITY` and `TRANSPORT_CAPACITY`.
2. `TeamTownData#reloadMaxCapacity` rebuilds warehouse capacity.
3. Every non-house building whose `shouldRunDailySettlement()` is true executes once in work-priority order. A workable `TransportStationBuilding` adds its deterministic output through `TownResourceActions.VirtualResourceAttributeAction`; it never writes the holder directly.
4. `TeamTownData#finishDailyTransportSettlement` snapshots town total transport capacity and the reservation map's current nominal reserved capacity into `TownTransportState.DailyReport`.

`TRANSPORT_CAPACITY` is a daily service, not a consumable stock. It does not use warehouse capacity, does not carry into the next settlement, and consumers reserve capacity instead of applying a resource `COST` action.

## Transport Reservations

`TransportEndpointId.endpointPos` is the physical consumer position. For a warehouse interface this is the interface block's `GlobalPos` and the only positional identity in its reservation. Interfaces are town-owned devices and do not select or persist one supplying warehouse core.

All player setting changes go through `TeamTown#registerOrUpdateTransportEndpoint` or `#unregisterTransportEndpoint`. `TransportEndpointRequest` contains only endpoint identity, kind, and the proposed integer rate; the server derives the distance metric from the applied town topology. `TransportReservation.rateItemsPerSecond` is the one accepted setting, while effective rate is derived at runtime from the town scale and is not persisted as a second setting. A rejected change to an existing endpoint returns `TransportReservationDecision.INSUFFICIENT_CAPACITY` while leaving the reservation and transport dirty state unchanged. A rejected new endpoint records `rateItemsPerSecond = 0`, `reservedTransportCapacity = 0`, and `TransportAdmissionStatus.DISABLED`.

`WarehouseTopologySnapshot` is transient server state owned by `TeamTownData`. It contains the authoritative town dimension and a stable core-position-sorted list of `WarehouseTopologyEntry(corePos, capacityWeight)` for workable warehouses with finite positive capacity. Building add/remove and relevant setters mark the topology dirty. `TeamTownData#refreshWarehouseTopologyIfDirty` rebuilds it once, compares the complete snapshot, atomically replaces every warehouse-interface reservation, marks transport dirty at most once, and then notifies loaded `WarehouseTopologyListener`s. A clean prepare does not rebuild the list or iterate endpoints.

For endpoint `e`, `TransportReservationModel#warehouseWeightedDistance` computes `sum(W_i * Manhattan(e, i)) / sum(W_i)`. The implementation normalizes weights by their maximum before summation. `warehouseDistanceFactor` returns `1 + warehouseDistanceCostPerBlock * distance`, with a default cost of `0.05` per block; reserved capacity is rate times this factor. Missing/empty topology, dimension mismatch, or invalid numeric output yields `TransportAdmissionStatus.UNAVAILABLE`, preserves the setting rate, and sets both derived metric and capacity to zero. A later factual topology refresh restores `ACTIVE` or zero-rate `DISABLED` without admission rejection.

`TownTransportState` keeps the aggregate reserved capacity as a derived O(1) cache and remembers the `TransportConsumerParameters` snapshot used to calculate every reservation. Repeated interface/menu/status reads with unchanged parameters do not rescan or reconstruct the reservation map. A parameter change performs one full recalculation; endpoint replace/remove paths update the aggregate incrementally. Neither cache is an additional persisted authority: persisted reservations still omit `reservedTransportCapacity`, and the first read after load rebuilds it from the current parameters.

`TownWarehouseDeviceAccess` is the shared authority boundary for warehouse interfaces and level emitters. First valid interaction claims an unowned device for the player's team. Every menu command rechecks the current menu instance, live block entity identity, eight-block range, owner team, town dimension, and device dimension. `TeamTownProvider` is persisted on the device; no warehouse position is stored. Both devices may be placed anywhere in the authoritative town dimension and are no longer discovered by `WarehouseBlockScanner`.

`WarehouseInterfaceBlockEntity` creates or verifies its town reservation on claim, load, and before balancing. New claims use the configured default `20 items/s`. Breaking or replacing the interface unregisters its endpoint; ordinary chunk unload preserves the reservation and resets only loaded runtime state. Warehouse removal changes topology but never unregisters a device.

`WarehouseLevelEmitterBlockEntity` is not a transport endpoint and consumes no transport capacity. While loaded and its town has usable warehouse topology, it owns one exact-item `TownResourceWatcher`; filter changes reset and recreate that watcher. It compares the watched long stock against a normalized threshold (`>= 1`) in the selected `WarehouseRedstoneMode`, emits either `0` or `15`, and notifies neighbors only when the output bit changes. Stock-only changes still persist/synchronize state but do not cause neighbor-update storms. Unavailable topology immediately resets the watcher and output; recovery recreates it and refreshes current stock. Menu display saturates long stock at `Integer.MAX_VALUE` instead of overflowing.

The interface's `TransportTransferBudget` keeps only a runtime decimal remainder in `[0, 1)` item. When demand exists, each server tick adds `effectiveRate / 20` and floors one shared budget. `WarehouseInterfaceTransfer` applies that budget to exports before restocking, caps every `ItemStackAction`, and deducts only the action result's actual item count. Unused whole-item budget is discarded; the remainder is never serialized.

## Adding A Building

1. Implement the logical `<Name>Building`, normally from `AbstractTownBuilding` or `AbstractTownResidentWorkBuilding`.
2. Define its `Codec` and register a stable key in `ITownBuilding.CODEC`.
3. Implement the block and `AbstractTownBuildingBlockEntity`; add a scanner only when the structure requires one.
4. Register the block, block entity, tags, and optional menu and screen through `FHBlocks`, `FHBlockEntityTypes`, `FHRegistrateTags`, `FHMenuTypes`, and `FHScreens` as applicable.
5. Route resource changes through actions and lifecycle removal through `TeamTown.removeTownBlock`.
6. Test codec round trips, registration/removal cleanup, work eligibility, resource capacity, and client synchronization affected by the building.

`isBuildingWorkable()` requires initialization, a valid structure, and no occupied-area overlap. Residential buildings use centralized housing and meal settlement; they are not ordinary work buildings.

## Synchronization Invariants

`ObservableTownMap`, object change listeners, and resource listeners feed the single dirty-state owner, `TeamTownData.DataSyncCache`. Server `tick()` prepares dirty warehouse topology before draining transport state, then flushes dirty keys through `TownBuildingUpdatePacket`, `TownResidentUpdatePacket`, and `TownResourceUpdatePacket`. Full `TeamTownDataS2CPacket` synchronization is a recovery and entry-point mechanism, not the normal per-tick path.

The station report is persisted and incrementally synchronized as part of its building object. The town state is persisted in `TeamTownData.transportState`. Both `TownResourceUpdatePacket` and `TeamTownDataS2CPacket` carry an immutable `TownTransportSnapshot` containing the daily report, stable reservation list, current total capacity, server-derived reservation capacities, effective warehouse count, and server `warehouseDistanceCostPerBlock`. Incremental application updates the resource holder and snapshot before firing one resource callback. Full sync decodes the persistent town data, applies the accompanying snapshot, replaces the client instance, and then fires one existing full-sync callback batch. This is required because `TransportReservation.CODEC` intentionally omits derived capacity while `TransportReservation.SNAPSHOT_CODEC` includes it. Snapshot decoding limits the reservation count and rejects invalid numeric fields.

The warehouse interface menu uses `WarehouseInterfaceTransportView.CODEC` through a custom `CDataSlot`; the view includes the server's current `maximumRateItemsPerSecond` and derived distance factor so client validation and display do not assume local server config. The client sends only one proposed integer setting. Typed values above the synchronized maximum are rejected locally, and scrolling the rate field submits a clamped adjustment with `1/8/16/64` steps for none/Shift/Ctrl/Shift+Ctrl. `WarehouseInterfaceBlockEntity#setTransportRate` reuses `TownWarehouseDeviceAccess` and does not trust client distance, effective rate, or capacity values. Rejected increases reset the unfocused input to the accepted server value and expose only transient command feedback. A newly claimed interface that cannot reserve its default rate remains owned at rate zero and notifies only the interacting player; load and background checks do not broadcast this failure.

New or changed building and resident setters must return when the value is unchanged, then call `fireChange()` after a real change. Some legacy setters do not yet apply that guard. Resource updates receive additional value-level deduplication; buildings and residents do not.

Client building screens resolve the synchronized snapshot through `AbstractTownBuildingBlockEntity.getTown()` and `CClientTeamDataManager`. They must not use server-only `CTeamDataManager`. Data panels should read current values through suppliers during rendering; data-change callbacks must not rebuild the content layer and discard scroll or selection state.

The Mayor's Seal `TownVirtualResourcesTab` lists every `VirtualResourceType` and keeps specialized presentation in
`TownVirtualResourcesPanel`. Warehouse capacity reads `MAX_CAPACITY`, `TeamTownResourceHolder#getOccupiedCapacity`, and
their difference. The transport page derives its live aggregate and stable endpoint rows from the synchronized
`TownTransportSnapshot`: the endpoint key is labeled as the interface position and no individual warehouse core is shown. Live summary appears first and `DailyReport` follows in
a latest-morning settlement section. Device details are last and default to collapsed; the clickable
`TownInfoPanel.Row` control toggles only `TownVirtualResourcesPanel.TransportDetailsState`, so supplier-driven snapshot
refreshes do not rebuild the panel or reset its scroll. Expanded rows show the accepted setting, effective rate, capacity-weighted
average distance, reserved capacity, derived throttled/admission state, and the server-authoritative distance factor. Unavailable
rows show no distance or factor; raw `scaleMetric` is not exposed as a debug label.
`TownManagerClientHelper#openTransportCapacity` opens this page directly for notification actions.

`WarehouseInterfaceBlock.TRANSPORT_STATE` is a four-value visual property: `active`, `disabled`, `shortage`, and
`unavailable`. An active reservation throttled by a town-wide shortfall uses the shortage model. Admission rejection is a
transient command result: an existing endpoint keeps its prior visual state, while a rejected new endpoint is disabled.
Unowned interfaces and interfaces whose town topology is unavailable share the unavailable model because their detailed reason remains in the menu.
`WarehouseInterfaceBlockEntity#updateTransportBlockState` recomputes the derived state on the server but calls
`Level#setBlock` with `Block.UPDATE_CLIENTS` only after a net property change, so it does not create neighbor-update or
per-tick BlockState write storms.

Morning transport shortage feedback is server-owned. `TeamTownData#finishDailyTransportSettlement` evaluates
`TransportReservationModel#meaningfullyGreater(reserved, total)` once for the next town settlement day and queues no
recovery notice. `TownTransportShortageNotificationPacket` is a bounded S2C-only numeric notice containing validated total,
reserved, shortfall, and effective scale values; it never carries arbitrary display text. The shared `sendToOnline` path
delivers it to current team members, and the localized Tip click action only opens the Mayor's Seal transport page.

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
