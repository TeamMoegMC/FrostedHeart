# Town Simulation Scenarios

This directory contains committed inputs for the Java town simulator. `baseline/` holds reproducible reference cases; `experiments/` holds targeted sweeps and calibration runs.

Scenario JSON owns experiment inputs only. Gameplay defaults remain authoritative in `TownModelParameters.currentDefaults()` and `FHConfig`; resolved values are written to `summary.json`.

Read [scenario-reference.md](scenario-reference.md) for fields, stage boundaries, and outputs, then [town-model.md](../../docs/town/town-model.md) for formulas and interpretation.

Run a scenario with:

```bash
./gradlew runTownSimulation -PtownArgs='simulate --pack-root "<TWR .minecraft>" --scenario Scripts/town_scenarios/<file>.json --output build/reports/town-model/<run>'
```
