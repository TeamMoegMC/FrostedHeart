# Continuous Air Runtime

- Status: `Partial; production path implemented, lifecycle and capacity work still in progress`
- Last verified: `2026-09-21`
- Scope: optional continuous Air backend, material coupling, source delivery, spatial queries and version-5 checkpoints
- Code anchors: `AirFieldLayout`, `LocalAirShape`, `AirFieldCompiler`, `AirShapeStore`, `AirOperatorFragment`, `CoupledThermalOperator`, `PcgSolver`, `ContinuousAirSolver`, `AirLoadTable`, `CutLoadBuffer`, `AirFieldCheckpoint`, `ThermalDimensionEngine.process`

## Activation and ownership

`FHConfig.COMMON.THERMAL_RUNTIME.continuousAir` is a restart setting, default `false`.
`MinecraftThermalProfiles.Tuning` captures it with the other thermal settings.
The enabled backend uses the existing dimension worker, source ledger, Page capture,
phase controller and query publication. It creates no additional executor or world sampler.
The default scalar backend is described in [runtime architecture](thermal-runtime-architecture-and-optimization.md).

`ContinuousAirSolver` owns the live Air coefficients. Material enthalpy H (J) and phase
branch remain authoritative in `ThermalCellArena`. Air arena values are derived component
means for existing topology/residency infrastructure; live continuous queries evaluate
the spatial coefficients. The main thread reads the double-buffered `QueryPublication`.

## Spatial representation

An actual connected Air component in a 4×4×4 Brick uses eight trilinear coarse functions.
`AirFieldCompiler` shares coefficient identities across open neighboring Brick faces;
solid walls do not create connections. Page dependencies follow the shared functions.
`AirFieldLayout.Component` owns coefficient indices, Air block masks and geometry dependencies.
Positions use world coordinates, with floor-based indexing for negative coordinates.

Real positive-power `AIR_FACE` outlets also request two local shapes with scales 1 and 3,
within `LocalAirShape.RADIUS = 6` blocks. `AirShapeStore` prepares dimensionless static
screened-diffusion shapes on captured geometry, with separate vertex branches where
Air is disconnected. Missing support Bricks are requested through normal worker residency.
A shape is installed only when its support is captured. Routed sources use the actual
nearest Air outlet face found by `AirRouteCompiler`, rather than the ventilated material's position.

Temperature is `T(x) = Tref + Σ qi φi(x)`, where T, Tref and qi are in °C and φi is dimensionless.
The same functions supply volume integration, face contacts, source loads, queries and checkpoints.
Air capacity defaults to `airHeatCapacityJPerBlockK = 1200 J/(block·K)` and mixing to
`airMixingWPerBlockK = 96 W/(block·K)`; both are runtime configuration values.
Shapes and preintegrated volume arrays are reused when their local Air geometry and mixing mask match.

## Source and material advancement

`ThermalSourceLedger.acceptAndRecord` settles source event times into a reusable
`CutLoadBuffer`. Nonempty spans carry W; equal-endpoint impulses carry signed J.
One `AIR_STENCIL` binding refers to one `AirLoadTable` entry, irrespective of basis count.
The numerical load scatters its spatial weights; delivery is counted once for the physical port.
`SourceBinding` reserves bit 31 of the target index to distinguish Air-load indices from material slots.

`ContinuousAirSolver.solveCut` splits at event timestamps and at most 20 ticks (1 game second).
It advances Air and materials together by backward Euler. `CoupledThermalOperator` stores
volume arrays and flat contact weights; each contact applies a gather/scatter term without
expanding a dense matrix. Coarse/coarse transport mass is lumped; cross terms involving a local
mode remain. Topology projection uses the full mass matrix instead.

Contact activity/environment are refreshed once per material trial; diagonal/preconditioner
preparation then runs once per buoyancy iteration, after the material branches are selected.
`materialContactRates` gathers the same contact temperatures but scatters only the material
entries consumed by H/branch trials. Two-entry contacts have an explicit rank-one application
inside `apply`; general spatial traces retain the packed loop. These paths use the same weights,
conductance and accumulation order, with no altered thermal parameters or convergence threshold.

`PcgSolver` reuses primitive work arrays, warm starts, scalar Jacobi and paired local-mode
Cholesky blocks. Its preconditioned correction threshold is `1e-9 °C`, with a recomputed
residual before acceptance. This stopping criterion is not a certified physical temperature-error bound.
Buoyancy uses an outer iteration. Material trials keep candidate H/branch separate from
accepted state. A material segment crossing rejects the coupled substep and reduces its duration;
only solver-scale endpoint rounding is canonicalized. Latent and waiting materials use
fixed-temperature rows, with actual contact energy updating latent H.

The engine settles the previous geometry's source interval, commits the accepted state,
processes ACK/environment/topology, rebuilds the spatial layout when topology is committed,
rebinds sources, releases old spans, collects normal phase requests and publishes one cut.
`MinecraftPhaseController` performs real world conversions and supplies the existing ACK.
Confirmed source delivery is recorded after numerical acceptance.

## Queries and persistence

Live player, block and item queries evaluate the actual requested xyz. The item quarter-position
cache retains reusable radiation work but refreshes spatial Air at exact xyz. Existing analytic
fields and direct radiation are composed afterward. `ThermalEnvironmentSample.airBasisTerms`
and `airSampleTick` expose whether a real spatial publication supplied the result; Natural fallback
has no spatial basis. Cross-Page dependencies must match the captured geometry.
Infrared continues to read material temperature.

`DormantChunkThermalState` reads and writes only root `FrostedHeartThermal.version = 5`.
Version 4 and older thermal records are treated as missing; there is no migration reader or dual write.
The continuous path stores `AirFieldCheckpoint`: double coarse values, static local-shape Section
pieces and amplitudes, plus the capture tick/natural baseline. Runtime slots are not serialized.
Continuous checkpoints do not also write the scalar Brick preview. The scalar payload remains only
for the currently selectable scalar backend, within the same version-5 root.

Material H/branch/law/BlockState and per-record ages stay in `MaterialSectionState`.
Dormant point queries evaluate the saved field and existing natural half-life cooling.
Admission reconstructs geometry from world blocks, restores shape pieces and projects the old
field into current coefficients. Partial Section captures retain unrepresented spatial history.
This is finite-support spatial history, not a replay of unloaded-world simulation.

## Validation and remaining work

`ContinuousAirProductionGameTests` uses actual blocks, fuel capabilities, flint-and-steel,
shovels, normal world ticks and public temperature/material queries. It never writes H,
constructs an engine, calls a solver, or creates synthetic ACKs. The selected namespace is
`frostedheart_production`, containing the four shorter heating, spatial, reload and slab-outlet scenarios.
The long powder-snow phase scenario is in `ContinuousAirPhaseProductionGameTests`, selected only
with `-PgameTestNamespaces=frostedheart_production_slow`; it is temporarily excluded from the
normal development run at the user's request. The fixture paces GameTest world ticks at 20 TPS because its default
unthrottled clock can outrun the production worker.

Run with the GameTest common config's `continuousAir = true`:

```powershell
./gradlew.bat runGameTestServer -PgameTestNamespaces=frostedheart_production --offline --no-daemon --console=plain '-Dnet.minecraftforge.gradle.check.certs=false'
```

This task clears only its pre-existing `run-gametest/world` test directory. Restore any temporary
test config after the run. Logs and profiles belong in `build/`; validation outcomes belong in the diary.

Outstanding implementation limits:

- Spatial rebuild/projection still occurs after topology installation; preparation/reservation is not yet integrated into the topology transaction.
- All retained Air/operator/shape/scratch allocations are not yet charged to the shared 128 MiB budget. Query arrays have their own accounting.
- Continuous-backend sleep is disabled. Environment-only topology updates may still trigger unnecessary full rebuilds.
- Mode filtering currently compares the two candidates per footprint; general overlapping-mode rank pruning is pending. Inactive-mode removal uses a 0.01 °C coefficient heuristic, not a projection-error bound.
- Full server restart, dense-source capacity, geometry mutation coverage and target-server latency have not been certified. The implementation remains opt-in.

Intended work and acceptance criteria are tracked in the [engineering plan](../../plans/2026-09-18_20-53-28_continuous-air-r4-cpu-engineering.md).
