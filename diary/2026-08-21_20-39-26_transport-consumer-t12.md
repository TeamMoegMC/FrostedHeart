# Transport consumer automated audit T12

- Time: `2026-08-21 20:39:26 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `transport regression matrix, Stage Four parameter trace, and idle-interface performance`

## Completed

- Added cross-module regressions for multiple interfaces and warehouses, shortage recovery, warehouse shrink, same-position warehouse replacement, and atomic client resource/snapshot application. Existing tests continue to cover legacy save upgrade, unloaded core removal, permission checks, and partial inventory Action results.
- Verified that `TownModelParameters.transportConsumers` remains the Forge-independent simulation input, `TownStageZeroAudit` records values/units/config symbols, and `TownStageFourSimulator.Summary` emits those parameters in JSON output.
- Replaced repeated full reservation recalculation and summation with a parameter-snapshot guard and O(1) aggregate cache in `TownTransportState`. Persistence still omits derived reservation capacity and rebuilds it after load.
- Added a snapshot-limit idle audit covering 4096 reservations for 20 ticks and an independent 4096-interface empty-inventory/visual-state audit.
- Updated living transport documentation and both implementation plans for the T12 result. No generated resource changed, so the already clean T10 `runData` result remains applicable.

## Decisions

- Performance evidence is a deterministic operation-scale regression plus exact side-effect assertions, with a generous five-second timeout; it does not claim a production server TPS benchmark.
- Repeated reads may create lightweight immutable views but must not rescan the reservation map, mark transport sync dirty, submit inventory Actions, call the inventory-change persistence gate, or request an unchanged BlockState write.
- H01 remains the authority for real-game TPS, disk, packet, GUI, multiplayer, and placed-block observations.

## Validation

- Transport directed matrix: 45 tests passed with zero failures or errors.
- Warehouse-interface directed suite: 13 tests passed with zero failures or errors.
- Town package suite: 326 tests passed with zero failures or errors.
- Full `test compileJava --offline --no-daemon --console=plain`: 377 tests passed with zero failures, errors, or skips; compilation passed.
- The 4096-reservation, 20-tick idle query test completed in about 0.243 seconds and left `DataSyncCache` clean. The 4096 empty-interface audit completed in about 0.025 seconds with zero Actions, inventory changes, continuations, or visual writes.
- `git diff --check`: passed.

## Remaining

- H01 must perform the real-game lifecycle, throughput, GUI, multiplayer, disk, and network acceptance scenarios.
- T13 must close final living docs and plan outcomes after the chosen handling of H01. P2P devices remain in their independent plan.
