# Thermal arena and transaction readability closure

- Time: `2026-08-30 20:05:25 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal dead fields, arena phase/layout ownership, material-contact construction, topology transaction construction, tests, and living documentation`

## Completed

- Removed the write-only `BrickMaterialKernel.arena`,
  `TopologyPlan.parameters`, and `WorkerBrickTopology.fragment` fields and their
  redundant arguments. Each active Brick no longer duplicates the solver's
  fragment reference.
- Extracted the reusable `ThermalBrickCellLayout` and the arena-slot-indexed
  primitive `ThermalPhaseReservoirStore`. `ThermalCellArena` retains cell
  authority and its public phase API while decreasing from 1,163 to 927 lines.
- Replaced the 16-position `MaterialContacts` call with one reusable
  surface/phase builder owned by `BrickMaterialKernel`.
- Replaced the 14-position `PreparedTopologyChange` call with one reusable
  grouped builder owned by `TopologyPlan`. `PageWrite` call sites now use named
  active/retirement factories.

## Decisions

- The extracted phase store remains one primitive SoA per arena and uses the
  same arena slot. It creates no per-cell or per-request object.
- Builders are one per compiler/plan and transfer the existing primitive arrays
  into the existing persistent payload. They create no per-Brick or
  per-transaction grouping object.
- `MinecraftPageManager` was explicitly outside this change.

## Validation

- Java 17 production, test, and GameTest compilation: passed.
- Thermal JUnit: `96/96` passed.
- Forge GameTest: all `14/14` required tests passed.
- Compiled field read/write closure reports zero non-constant write-only fields.
- Removed-symbol search and `git diff --check`: passed.

## Documentation

- Updated the living thermal architecture for the extracted layout/phase store,
  non-duplicated Brick fragment ownership, and reusable grouped builders.

## Remaining

- `MinecraftPageManager` readability work remains intentionally unchanged.
- Controlled JFR/heap profiling remains the existing performance-evidence task.
