# Phase I Brick-local phase reservoir

- Time: `2026-08-25 02:34:57 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed`
- Scope: `dormant Brick-local latent-energy reservoirs, bounded Minecraft phase mutations, and validation`

## Completed

- Added `PHASE_RESERVOIR` profiles and explicit mutation policies/actions to the immutable worker-safe `MaterialBoundaryRegistry`.
- Added Page-owned primitive phase state to `ThermalCellArena`: latent energy, candidate mask, reserved energy, request sequence/state, and at most one outstanding request per reservoir.
- Added conservative Air-to-phase contacts and fixed-capacity request/ACK transport in `PhaseTransitionRuntime`, including reservoir-owned request retry, retained ACK outcomes, generation checks, and a committed latent-energy ledger.
- Compiled exposed candidates by world-aligned `4x4x4` Brick/profile in `MinecraftThermalTopologyApplier`, migrated latent/request state across rebuilds, and excluded reservoirs from ordinary Air adjacency.
- Added bounded pre-seal request handling in `MinecraftThermalInput` with loaded Page/chunk, current profile, and exact material-air microface validation; built-in actions remove one snow layer or melt ice to water, while `MinecraftPhaseTransitionHandler` owns custom transitions.

## Decisions

- A reservoir is a latent-energy account, not a second temperature authority, global SnowPatch, per-block timer, or per-block thermal node.
- Mutation policy remains separate from thermal energy accounting; machine-driven profiles may ignore `randomTickSpeed`, while ambient-compatible profiles may respect it.
- Mutation energy is reserved before the request and committed only after an applied ACK. Rejected mutations retain absorbed heat; retry keeps the original request.
- Phase I remains dormant shadow code. Legacy world transitions and gameplay temperature queries remain authoritative until calibration and activation gates pass.

## Validation

- `gradlew.bat test runGameTestServer --offline --no-daemon --console=plain` with Java 17: passed.
- JUnit XML after the final interface regression test: thermal `230/230`, repository `803/803`, with zero failures, errors, or skips.
- Forge GameTest: `18/18` required tests passed.
- `git diff --check`: passed apart from working-copy LF-to-CRLF notices.

## Documentation impact

- Updated `docs/climate/data-lifecycle-and-integration.md` with Phase I ownership, lifecycle, backpressure, mutation validation, and test coverage.
- Updated `docs/climate/heat-production-and-network.md` with the dormant phase energy and mutation contract.
- Updated the thermal implementation plan to mark Phase I / PR11 complete and Phase J / PR12 Radiation next.

## Remaining

- Implement Phase J / PR12 Radiation as a read-only receiver-side service.
- Production-like Phase 0b workload evidence, approved FarField profiles, and gameplay/reference calibration remain activation gates.
