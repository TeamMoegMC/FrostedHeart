# Transport consumers T03-T09

- Time: `2026-08-21 18:22:56 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town transport reservations, synchronization, warehouse-interface lifecycle, throughput limiting, and rate menu`

## Completed

- Added the `TeamTown` authoritative admission, metric refresh, summary, snapshot, and unregister paths; morning reports now snapshot live nominal reservations.
- Added stable `TownTransportSnapshot` resource synchronization and atomic client application.
- Connected warehouse interface bind/load/rescan/unbind/removal behavior to interface-position reservations while preserving reservations across ordinary chunk unload.
- Limited real warehouse item movement with a runtime fractional budget and one shared export/restock budget per server tick.
- Added a server-validated rate command, direct input, `0/20/64/320/1280` shortcuts, immutable Codec menu view, and English/Chinese presentation.
- Updated `docs/town/implementation-reference.md`, `docs/town/town-model.md`, `docs/transport_station_design.md`, and both transport consumer plans.

## Decisions

- Endpoint identity is the warehouse interface `GlobalPos`; the warehouse core `GlobalPos` remains a separate binding field.
- Long-lived rates and reservations are not reduced during a supply shortfall. Every active endpoint receives the same `min(1, total/reserved)` effective-rate scale.
- `TransportTransferBudget` stores only a decimal remainder below one item. `BigDecimal` avoids the observed `7 items/s -> 699.999999...` long-run floor error without a token quantum or ULP adjustment.
- Inventory actions are capped before execution and the budget is reduced only by the executor's returned modified stack count.

## Validation

- `test --tests "*TownTransport*" --tests "*WarehouseInterface*" --offline --no-daemon --console=plain`: passed.
- `test --tests "com.teammoeg.frostedheart.content.town.*" --offline --no-daemon --console=plain`: passed.
- `test compileJava --offline --no-daemon --console=plain`: passed; full suite reported `355` tests, zero failures/errors/skips.
- `git diff --check`: passed.

## Remaining

- T10-T13 remain: Mayor's Seal real-time endpoint rows, block-state feedback, daily shortage Tip, final performance/regression work, living-document completion, and plan outcome closure.
- H01 game-integration acceptance still needs real server checks for unload/reload, rebinding, low TPS, permissions, and GUI scale/localization.
- P2P devices remain deferred to `plans/2026-08-21_01-39-26_transport-p2p-devices.md`.
