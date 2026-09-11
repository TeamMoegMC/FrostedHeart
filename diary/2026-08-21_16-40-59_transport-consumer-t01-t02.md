# Transport Consumer T01-T02 Foundation

- Time: `2026-08-21 16:40:59 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `transport-consumer parameters, pure reservation math, endpoint persistence, and aggregate state`

## Completed

- Added immutable consumer parameters and the Forge-independent reservation model for the frozen warehouse formula:
  `rate * (1 + 0.05 * sqrt(warehouseVolume))`.
- Added `TransportEndpointId` for the physical warehouse-interface position and a separate
  `TransportReservation.boundWarehouseCorePos` for its bound warehouse core.
- Extended `TownTransportState` with a stable, immutable reservation view and a forward-compatible optional
  reservation Codec field. Derived reserved capacity is not persisted and is recomputed from current parameters.
- Added the four source defaults to `TownModelParameters.Defaults`, the server configuration, and the stage-zero
  parameter audit.

## Decisions

- Use raw double capacity values, without rate tokens or quantization. Capacity comparison uses an 8-ULP tolerance
  only for admission and shortage boundaries.
- Persist endpoint inputs as a sorted entry list. Malformed entries recover independently with a warning; every
  duplicate endpoint group is discarded rather than selecting an order-dependent winner.
- This work deliberately does not add a `TeamTown` reservation write API, daily settlement integration, client sync,
  warehouse lifecycle registration, or actual item throttling.

## Validation

- `test --tests "*TransportReservation*" --offline --no-daemon --console=plain`: passed, 4 tests.
- `test --tests "*TownTransportStateTest" --offline --no-daemon --console=plain`: passed, 7 tests.
- Parameter default/audit tests, all `com.teammoeg.frostedheart.content.town.*` tests, the full test suite, and
  `compileJava` passed using JDK 17.
- `git diff --check` passed.

## Remaining

- Continue with `T03`: introduce the server-authoritative `TeamTown` reservation state machine and invoke capacity
  cache recalculation from its lifecycle.
