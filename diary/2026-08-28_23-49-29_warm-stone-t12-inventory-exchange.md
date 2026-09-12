# Warm stone T12 ordinary-inventory exchange

- Time: `2026-08-28 23:49:29 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent, with t12-inventory-exchange sub-agent`
- Status: `completed`
- Scope: `server player inventory cadence, passive air exchange, duplicate Stack write prevention`

## Completed

- Added `InventoryThermalExchangeHandler` and connected it through `WarmStoneItem.inventoryTick` for exact ordinary-player-inventory Stack ticks.
- Added fixed `20 game tick` cadence, bounded `1.0 s` elapsed, Curios equipped-Stack exclusion, and same-server-tick identity deduplication.
- Connected valid states to `ReservoirEnvironmentExchange.advanceInventoryInto`; missing or invalid states initialize once without an exchange in the same cadence.
- Added focused tests for cadence, server-player context, exact ordinary-inventory ownership, initialization, direction, equipped exclusion, duplicate work, invalid inputs, non-reservoir stacks, equal temperatures, and persisted-float no-write behavior.
- Updated the player-temperature and lifecycle living docs, implementation plan, and shared handoff.

## Decisions

- Inventory uses `WorldTemperature.naturalAir` plus `MinecraftThermalInput.gameplayPassiveEnvironment`. The primary review removed the initial `WorldTemperature.air` call because its per-query Gaussian noise would become part of persistent temperature integration.
- Passive runtime composition may read an existing air publication or analytic fields but creates no Page interest and performs no radiation query. Inventory therefore remains shielded from direct radiation.
- Only `PlayerInventory.items` participates. Armor, offhand, Curios, external/unticked containers, clients, non-player entities, and `ItemEntity` are outside T12.
- Cadence and deduplication are transient handler state, not a second ItemStack authority. The identity set retains at most the relevant Stack objects claimed in the current server tick and clears on the next claimed tick.
- Writes are skipped when solver output would serialize to the same version-1 float values, avoiding unchanged NBT churn.

## Validation

- JDK 17 focused T03/T05/T06/T11/T12 selection: `5` suites, `33/33` tests passed; T12 handler `11/11`.
- JDK 17 broader player/thermal/Curios regression: `48` suites, `252/252` tests passed.
- The first focused attempt found an orphaned generated `ObservableTownMapTest$Recorder.class`; all selected suites themselves passed. A clean rebuild removed it. Because ForgeGradle offline mode then skipped its missing generated MCP config, one non-offline build restored that cache input before successful offline regression.
- Compilation emitted only existing Mixin/JEI and duplicate-resource warnings.

## Remaining

- Gate B still requires real Curios/container packet counts and tooltip-freshness observation. No dedicated synchronization packet was added.
- Unticked containers remain frozen and offline time is not replayed.
- Dropped-item air/radiation sampling and lifecycle remain T13-T19 work.
