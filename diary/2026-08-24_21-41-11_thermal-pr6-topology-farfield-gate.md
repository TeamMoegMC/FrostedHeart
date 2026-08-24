# Thermal PR6 Topology Guard And FarField Gate

- Time: `2026-08-24 21:41:11 +08:00`
- Author: `Codex; OpenAI; primary engineering agent`
- Status: `completed implementation; production approval pending`
- Scope: `content.climate.thermal.mesh`, `phase0.reference`, focused tests, active thermal plan, and climate lifecycle documentation

## Completed

- Added `TopologyGuard`, which classifies one loaded-only frontier snapshot as `MATERIAL`, `OPEN_AMBIENT`, `OPEN_CONTINUATION`, or `UNRESOLVED` without World access or broad topology scans.
- Added `FarFieldProfileRegistry` with the complete classification key, static conductance, calibrated power/temperature domain, signed boundary-energy envelope, and explicit candidate/approved state.
- Connected approved FarField decisions directly to the existing `ThermalSweep.BoundaryOperation`; no second boundary solver or thermal state store was introduced.
- Added `FarFieldReferenceHarness`, an independent multi-cell RK4 finite-volume reference plus analytic local static-impedance candidate. Fit cases alone select `Ginf`; holdout cases independently evaluate temperature trajectory, gameplay threshold crossing, signed Natural-boundary energy, and phase received power.
- Added a synthetic coverage matrix for open space, half-open space, cavern, tunnel exit, calm/windy buckets, and `1/10/100 kW` source powers.

## Decisions

- Missing topology evidence, missing/unapproved profiles, and operating points outside the calibrated domain remain `UNRESOLVED/OPEN_CONTINUATION` and generate no transport.
- `CANDIDATE` profiles cannot be returned by the topology compiler. An approved profile with any threshold-crossing mismatch is invalid.
- Phase 0b has not frozen production gameplay tolerances or supplied real reference evidence. The production registry therefore remains empty; synthetic test tolerances prove gate mechanics only and do not approve Minecraft integration.
- PR7 runtime correctness may proceed against this boundary contract. PR8 remains blocked until at least the required outdoor buckets pass the real holdout gate and the remaining Phase 0b production workload gate is closed.

## Validation

- Focused PR6 JUnit: `9/9` passed.
- Thermal JUnit: `186/186` passed.
- Repository JUnit: `714/714` passed with zero failures.
- Forge GameTest: `14/14` required tests passed.
- `gradlew test runGameTestServer --no-daemon --console=plain`: `BUILD SUCCESSFUL`.
- `git diff --check`: no errors; only existing CRLF conversion warnings.

## Documentation Impact

- Updated `docs/climate/data-lifecycle-and-integration.md` with the Phase E ownership, runtime containment, calibration boundary, test coverage, and current non-production status.
- Updated the active thermal plan with the PR6 implementation snapshot, validation totals, pending approval gate, and PR7/PR8 sequencing.
- No player-visible temperature, persistence, configuration, networking, or gameplay authority changed.

## Remaining

- Freeze Phase 0b workload-specific FarField tolerances and provide fit/holdout reference evidence for production buckets.
- Implement PR7 dimension runtime ownership, frame cuts, mailbox/publication, scheduling, sleep/wake, and memory admission.
- Do not begin PR8 Minecraft production integration until the FarField and remaining Phase 0b gates pass.
