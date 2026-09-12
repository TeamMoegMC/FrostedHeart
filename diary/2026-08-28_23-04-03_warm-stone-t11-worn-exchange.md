# Warm stone T11 worn three-node exchange

- Time: `2026-08-28 23:04:03 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `server player cadence, Curios warm_stone slot, item/player bidirectional exchange`

## Completed

- Added `WearableThermalExchangeHandler` to connect version-1 reservoir state, the existing three-node pure model, and the T10 player core adjustment API.
- Inserted one worn-reservoir exchange after both normal and `INSULATION` body processing and before the existing body synchronization packet.
- Added focused tests for exchange direction, zero-or-one Stack writeback, initialization, degraded inputs, management-mode policy, and non-reservoir stacks.
- Updated the player-temperature and lifecycle living docs, the implementation plan, and the warm-stone handoff.

## Decisions

- Missing or invalid state initializes from the finite server environment and stops for that cadence. Initialization and exchange never write the same Stack twice in one cadence.
- Creative, spectator, entity-invulnerable, and ability-invulnerable players skip before Curios lookup or state initialization. `INSULATION` remains a body-processing branch and does not suppress the worn thermal reservoir.
- The handler owns reusable mutable model results and scratch storage. It introduces no alternate solver, collection allocation, inventory scan, entity scan, or independent network packet.
- Player temperature is passed to the model as `coreBodyTemp + 37degC`; only the T10 core adjustment API mutates player data, preserving hands, feet, and previous-core history.

## Validation

- JDK 17 focused handler run: `7/7` tests passed.
- JDK 17 T00-T11 direct selection: `12` suites, `52/52` tests passed.
- JDK 17 broader player/thermal/Curios regression: `47` suites, `241/241` tests passed.
- Compilation emitted only the repository's existing Mixin/JEI warnings; the T11 handler static scan found no copied exponential solver or collection use.

## Remaining

- T12 must add server-only ordinary-inventory environment exchange while excluding the equipped Curios Stack and duplicate same-tick writes.
- Gate B remains open until actual Curios/container synchronization packet counts and tooltip freshness are observed. No dedicated thermal-reservoir packet should be added without that evidence.
- Dropped-item and world-environment behavior remains outside this phase.
