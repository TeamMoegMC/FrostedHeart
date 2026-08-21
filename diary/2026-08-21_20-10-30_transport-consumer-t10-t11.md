# Transport consumer feedback T10-T11

- Time: `2026-08-21 20:10:30 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent with delegated T11 implementation agent`
- Status: `completed`
- Scope: `Mayor's Seal transport details, warehouse-interface BlockState feedback, and daily shortage Tips`

## Completed

- Changed `TownVirtualResourcesPanel` to show the live transport aggregate and stable reservation rows, with interface and warehouse-core positions labeled separately; the morning `DailyReport` is now a distinct historical section.
- Added direct navigation through `TownManagerClientHelper#openTransportCapacity` for transport notifications.
- Added the finite `WarehouseInterfaceBlock.TRANSPORT_STATE` visual model with active, disabled, shortage, and unavailable variants for every horizontal facing.
- Added `WarehouseInterfaceTransportStatus.THROTTLED` so a previously active reservation reflects a town-wide supply shortfall in both its menu and block appearance.
- Added a bounded, numeric-only S2C shortage notice, per-town-day server deduplication, online-team delivery, localized Tip presentation, and a click action that only opens the transport detail page.
- Updated `docs/town/implementation-reference.md`, `docs/transport_station_design.md`, and both transport consumer plans. `docs/town/town-model.md` needed no T10-T11 update because formulas and simulation parameters did not change.

## Decisions

- Admission failure and town-wide throttling remain separate menu states but share the shortage block model; detailed causes do not enter BlockState.
- Unbound and temporarily unavailable bindings share the unavailable block model; the menu keeps their distinct text.
- The block entity may recompute its visual state each server tick, but it reuses the current `BlockState` and calls `Level#setBlock` only after a net visual change with `Block.UPDATE_CLIENTS`.
- Shortage notifications contain only validated total, reserved, shortfall, and effective-scale numbers. Display text is client localization, and the first version emits no recovery Tip.

## Validation

- `test --tests "*WarehouseInterfaceTransportViewTest" --tests "*WarehouseInterfaceFeedbackResourcesTest" --offline --no-daemon --console=plain`: passed; covers throttling, finite mapping, net-change policy, 16 blockstate variants, model references, and bilingual detail keys.
- Delegated T11 tests: `11` focused tests passed; expanded `TownTransport`, `TownSignal`, and Tip runtime selection passed `44` tests.
- `test --tests "*TownTransport*" --tests "*WarehouseInterface*" --tests "*TownSignal*" --offline --no-daemon --console=plain`: passed.
- `runData --offline --no-daemon --console=plain`: passed; `src/generated/resources` stayed clean.
- `test compileJava --offline --no-daemon --console=plain`: passed; `371` tests, zero failures, errors, or skips.
- Resource JSON parsing and `git diff --check`: passed.

## Remaining

- T12 still owns the final simulation/performance audit and broad validation matrix; this work did not claim that task.
- H01 still needs real-game checks for GUI scale and long endpoint lists, all four placed-block appearances, two-client delivery, same-tick Tip presentation, and click navigation.
- T13 will perform final documentation and plan closure after T12/H01. P2P devices remain in their separate plan.
