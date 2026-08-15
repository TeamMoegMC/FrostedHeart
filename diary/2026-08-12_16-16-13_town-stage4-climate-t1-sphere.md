# Town stage 4: current climate and one T1 sphere

## Summary

- **Author:** Codex (GPT-5)
- **Scope:** Implement phase 4 of `docs/town-model.md`: extract the current ordinary climate, block-temperature and integer T1 sphere formulas into game-owned shared functions, then couple explicit building interior voxels to the phase-3 resident/resource loop.
- **Balancing:** No default parameter values were changed.

## Decisions

- Added `ClimateEventModel`, `BlockTemperatureModel` and `SphericalHeatFieldModel`; current runtime classes delegate to these formulas, so the simulator does not carry a second implementation.
- Added all ordinary long-term climate and block-climate constants to `TownModelParameters.Defaults`, `TownModelParameters.ClimateParameters` and matching `FHConfig.SERVER.CLIMATE` entries. Simulation reads the parameter snapshot; runtime gameplay reads `FHConfig`.
- Extracted the block heat application multiplier from the hard-coded `2.0`; its default remains unchanged and both runtime and simulator now read the shared parameter chain.
- Kept one T1 tower only. T2, heaters, heat inertia and the random T1 heat-level ramp remain out of scope.
- Used 365-day climate burn-in and excluded the opening story blizzard. Simulation tracks have independent deterministic RNG streams while preserving the current event distributions, durations, Gaussian perturbations, interpolation and three-track temperature combination.
- Used explicit integer interior voxel boxes. Stage 4 requires one aggregate house and one aggregate hunting building, all voxels above sea level, snowy-plains-style `0°C` biome input and ignored altitude contribution.
- Recorded every hour for risk, but passed only the configured morning snapshot to the existing daily loop. Cold hunting skips work; existing cold-house residents still consume food and receive temperature stress; mines remain temperature-independent.
- Because phase 3 returns only a daily T1 service fraction, served time is placed at the start of the following day. This preserves the daily service amount but is not an exact tick-level outage timestamp.

## T1 capacity result

- Default normal T1: `r=16 block`, `H=10°C`.
- Exact integer sphere volume: `17,077 voxel`.
- Centered, fully covered three-high footprint upper bound: `793 floor block`, or `2,379 interior voxel`.
- Current housing formula gives a purely geometric upper bound of about `198 residents` with enough beds and no allowance for walls, tower, access or furniture.
- Above sea level in Overworld snowy plains, `N=-10+0.5 climate` and the active normal T1 full-coverage `0°C` limit is `climate=-20°C`.
- For coverage fraction `f`, the cold-region threshold is `climate_min=20-40f`. The 48-person reference house (`f=0.88455`) reaches about `-15.38°C`; its hunting base (`f=0.83160`) reaches about `-13.26°C`.

## Monte Carlo validation

Ran `120 days × 1,000 fixed seeds` for compact 8/24/48-person references:

| Population | House/hunt coverage | Field utilization | Median house workable hours | No-death probability |
|---:|---:|---:|---:|---:|
| 8 | `1.000 / 1.000` | `0.0450` | `0.5587` | `0.850` |
| 24 | `1.000 / 1.000` | `0.1349` | `0.8580` | `0.855` |
| 48 | `0.8845 / 0.8316` | `0.2315` | `0.7476` | `0.710` |

The 8-person result is dominated by the already-known insufficient T1 fuel labor and has `1,003` median starved hours. The 24-person reference remains fully covered and usually sustains fuel. The 48-person reference has more labor but exceeds the sphere, exposing the desired geometry/resource coupling. These values are diagnostics, not a balance recommendation.

## Validation performed

- `./gradlew test` — passed, 58 tests including all existing stage 0–3 regressions and new alpha, block heat, sphere boundary/count, climate interpolation/reproduction and phase-4 reference-layout tests.
- `audit` generated `build/reports/town-model/audit/stage4-current` and now includes climate/shared-geometry sources plus TWR snowy plains.
- Generated 8/24/48 reports under `build/reports/town-model/simulations/stage4-t1-*-1000`.
- Generated and visually inspected `stage4-t1-capacity-and-climate.png` and `stage4-t1-coverage-thermal-limit.png` with `Scripts/plot_town_stage4.py` in Conda `standard`.

## Remaining work

- Do not tune phase-4 parameters until the reference layouts and current heat-strength interpretation are reviewed.
- A later refinement may extract tick-level T1 heat service/ramp state if exact intra-day outage timing becomes necessary.
- Phase 5 remains the isolated T2 network/heater model; none of it was touched here.
