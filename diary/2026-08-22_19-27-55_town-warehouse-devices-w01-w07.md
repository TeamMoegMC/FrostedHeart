# Town-Owned Warehouse Devices W01-W07

- Time: `2026-08-22 19:27:55 +08:00`
- Author: `Codex; implementation and validation role`
- Status: `completed`
- Scope: `town transport reservations, warehouse topology, warehouse interface, warehouse level emitter, synchronization, UI resources, tests, and living documentation`

## Completed

- Replaced per-warehouse interface ownership with a town-owned endpoint keyed by the interface `GlobalPos`; interfaces and level emitters can be placed anywhere in the authoritative town dimension and persist only `TeamTownProvider` ownership.
- Added capacity-weighted average Manhattan distance over all workable finite-positive-capacity warehouses. The interface reserves `rateItemsPerSecond * (1 + warehouseDistanceCostPerBlock * distance)` and becomes `UNAVAILABLE` without usable same-dimension topology while preserving its single setting rate.
- Added transient `WarehouseTopologySnapshot` rebuild-on-dirty handling, atomic reservation replacement, loaded-device listeners, and clean-path operation counters. Warehouse scanning and removal no longer discover, own, unregister, or wake interface/emitter blocks.
- Updated interface synchronization and compact UI, Mayor's Seal transport details, bilingual resources, and the level emitter's town inventory watcher, threshold modes, persistence, unavailable behavior, saturated stock display, and net-output neighbor updates.
- Added pure emitter model/persistence tests, shared menu authority tests, 4096-listener topology tests, codec tests, snapshot/network tests, and updated existing transport/warehouse regressions.
- Fixed circular building Codec initialization with `CodecUtil.DispatchNameCodecBuilder#typeLazy`, preserving both inline map encoding and legacy integer building-index decoding.

## Decisions

- Warehouse capacity is the distance weight; device distance is the three-dimensional Manhattan distance to warehouse cores. The default distance cost is `0.05` per block.
- Topology changes are factual refreshes, not player admission requests: they may create a shortfall or `UNAVAILABLE` state but do not create a second requested/active rate.
- Level emitters consume no transport capacity and are not transport endpoints. They use one exact-item town resource watcher while loaded and usable.
- No migration was retained for the unreleased per-warehouse device fields; losing that development-only state is acceptable.

## Validation

- W07 directed matrix: `97` tests, `0` failures/errors.
- Town package: `367` tests, `0` failures/errors.
- Full `cleanTest test compileJava`: `463` tests, `0` failures/errors; compilation successful.
- `runData` was not run because no block model or tag resources changed.

## Remaining

- H02 game validation remains: arbitrary placement, real neighbor updates, transfer behavior, chunk unload/reload, reconnect/restart, bilingual GUI scale, and invalid-menu scenarios.
- Packet-type observation, disk-write observation, and multiplayer testing remain `未测` because the required tools/environment are unavailable.
- W08 must record H02 outcomes and update the P2P plan against the final generic reservation API.
