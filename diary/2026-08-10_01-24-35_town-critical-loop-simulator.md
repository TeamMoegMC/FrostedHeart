# Town critical-loop simulator

- Time: `2026-08-10 01:24:35 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Scripts/town_sim.py`, `Scripts/town_model/`, `docs/TWR城镇数值模型设计.md`

## Completed

- Added separate `current_compat` and `target_rc` town models with hourly climate, spherical heat fields, building RC response, daily SWE/FCE production and resident settlement.
- Added TOML scenarios, source-drift audit, Monte Carlo policies, parameterized layout search, sensitivity analysis, CSV/JSON/self-contained HTML reports, and a reproducible Conda environment file.
- Added 8/24/48 population reference layouts and a first-pass key fuel/food process graph. The target model routes the heat network only to spherical heaters; legacy direct building endpoints remain compatibility fixtures only.
- Installed Matplotlib and Pytest into the existing `standard` Conda environment as approved.

## Decisions

- Keep heat-field startup (`C·V`) separate from steady boundary/volume loss (`U·A + L·V`); fuel-per-degree-volume is derived output, not an independent knob.
- Keep food energy and nutrition separate, and keep population, SWE, FCE, and heated beds as different quantities.
- Treat substantial soft pressure as stock below three days, load shedding, or at least the configured daily comfort-degree-hour threshold; raw degree-hours remain available regardless of this reporting threshold.
- The reference stochastic climate is stage-limited and makes high-tier events rare. It must not sample the entire -90°C range uniformly merely because the climate system can eventually reach it.

## Validation

- `conda run -n standard pytest -q Scripts/town_model/tests` — 20 passed.
- `conda run -n standard python Scripts/town_sim.py audit --json` — all 9 checked Java assumptions passed.
- Smoke-tested all four CLI commands, generated HTML/CSV/JSON reports, verified seeded parallel Monte Carlo outside the restricted sandbox, and verified serial fallback inside it.
- Ran 120-day design-cold scenarios for all three target policies at 8/24/48 population. Normal power headroom was about 24–25%; forecast control avoided the health loss seen without intervention.
- Ran a fixed-seed 8-resident Monte Carlo benchmark with 100 runs per policy. Forecast survival was 99%, collapse 1%, and stockout/unsafe 3%, versus 76%, 24%, and 97% for no intervention. Conservative used 14.4% more fuel than forecast. Forecast soft pressure was still 80%, above the intended 70% ceiling.
- TWR repository was not changed; its existing `kubejs/config/client.properties` modification remains untouched.

## Remaining

- Run and review the full 1000-seed matrices before adopting reference values as gameplay config. The checked-in TOML is a calibration baseline, not final balance.
- Improve throughput before routine full-matrix sweeps: 300 serial 120-day runs took about 71.5 seconds, so cache or vectorize the geometry/thermal loop and profile the multiprocessing path.
- Stage climate distributions by progression/year, import real `OccupiedVolume` geometry, and expand only the fuel/food automation chains selected for final gameplay.
- The conservative policy's >=10% cost tradeoff is visible in sampled multi-wave sequences but is not guaranteed in every scale or single design event; keep this as an explicit acceptance check.
