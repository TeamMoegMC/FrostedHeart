# Thermal machine source feasibility

- Time: `2026-09-08 01:35:34 +08:00`
- Author: `Codex; OpenAI GPT-6; primary engineering/design agent`
- Status: `completed`
- Scope: `generator, radiator, fountain lifecycle coverage and feasibility of the thermal discovery plan`

## Completed

- Traced T1/T2 generator and radiator production through HeatingLogic, including the T2 superclass call and GeneratorData townProcessedTicks early return. Normal machine calculations still publish complete thermal output.
- Traced fountain output/removal, multiblock disassembly source IDs, and origin-chunk unloading.
- Verified required Forge and fastutil method signatures with javap against existing mapped dependencies. Inspected IE master tick dispatch and its chunk availability condition.
- Updated the [plan](../plans/2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md) to ready for implementation with explicit producer and validation contracts.

## Decisions

- Apply enabled/positive-power retention at the shared Minecraft physical source observation entry, covering all four current profiles. Zero-share AIR_FACE ports should not seed Pages.
- Reuse normal complete machine output instead of introducing machine bootstrap ticks, a provider registry, or per-machine publish epochs.
- Scope restoration to a producer's next valid normal tick; do not describe town simulation or a persistently non-ticking machine as an already-supported physical output producer.
- Only the plan and this diary changed. Living documentation will be updated when the proposed runtime/source behavior is implemented.

## Validation

- Read production producer, state, removal, runtime, source profile, and existing test code.
- javap confirmed loaded-chunk enumeration, BE candidate access, and ordered primitive-map methods in the current dependencies; IE dispatch reaches IServerTickableComponent after its existing chunk condition.
- No new implementation was compiled, and no GameTest or performance result is claimed. The plan specifies actual producer coverage in addition to existing facade-level tests.

## Remaining

- Implement the ready plan; validate machine restart/removal, fuel consumption, source capacity, dormant support, and startup/steady-state performance.
