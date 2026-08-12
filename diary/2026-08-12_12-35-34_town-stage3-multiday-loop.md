# Town stage-3 multi-day loop

- Time: `2026-08-12 12:35:34 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `shared town kernels, constant-temperature T1 multi-day simulator, scenarios, reports, tests, plots, and town-model documentation`

## Completed

- Added `TownStageThreeScenario/State/Model/Theory/Simulator` and dispatched `TownSimulationMain simulate` by `modelStage`. Stage 3 advances 120-day resident, sticky-job, inventory, processing, HUNT/carry, proficiency/aging, and finite T1 fuel state while reusing gameplay-owned pure kernels.
- Extracted shared `TownInventoryModel`, `ResidentAgingModel`, and `TownAssignmentModel`; gameplay now calls them. Mining and hunting assignment priorities also moved into their existing shared daily models. Exact assignment ties now use stable building/resident encounter order.
- Extended `GeneratorFuelModel` with finite-supply, full-batch settlement. Stage 3 records T1 process-tick service separately from integer fuel-item loading and preserves the cross-day fuel balance.
- Added 1/8/24/48-resident JSON baselines, atomic per-resource ledgers, run/daily/frontier/order CSVs, Wilson 95% intervals, and theory-versus-cumulative-SWE checks. TWR coke is explicitly resolved to `immersiveengineering:coal_coke`; no nonexistent synthetic item is used.
- Added the Matplotlib stage-3 plot script, committed two 320-dpi figures, and updated `docs/town-model.md` with exact definitions, results, and stage boundaries.

## Decisions

- Stage 3 remains fixed at `24°C`; climate, spherical heat coverage, T2, refugees, machines, and transport paths are excluded. Tower starvation is recorded but does not yet cool houses, so zero deaths must not be interpreted as successful heating.
- Initial seven-day food/coke reserves affect operational survival only. Structural fuel/food coverage numerators contain only new mine/hunt/processing output.
- Current sticky assignments are preserved: existing workers are not reconsidered, including ineligible workers who block a slot. Automatic assignment fills only vacancies and is not a resource-demand optimizer.
- Mining keeps `ADD ATTEMPT`, hunting keeps `ADD MAXIMIZE`, and all mining/hunting item iteration is stable for reproducible warehouse-edge behavior.

## Validation

- `./gradlew test` — full suite passed, including finite T1 supply, inventory modes, aging, deterministic assignment, fixed-seed replay, sticky ineligible workers, and item-ledger closure.
- Final FH/TWR audit snapshot `1b17d6a700111360bf74c20630d54ebab9266c20782126027bd8cfcfda3a1334`; core theory remains `1.1666667 coal/mining-SWE-day`, `22.5596491 cooked-food/hunting-SWE-day`, and `9.1836735 mining SWE` for the coke T1 route.
- Four 120-day × 1000-seed baselines completed. Potential fuel coverage for 1/8/24/48 residents is `0 / 0.4694 / 1.7211 / 3.5987`; potential food coverage is `6.1469 / 3.8636 / 3.3393 / 3.2139`. No-shortage probabilities are `0 / 0 / 1 / 1`.
- Cumulative simulator coefficients match theory: coal/SWE is exact to floating-point tolerance; cooked food/SWE relative error ranges from about `-0.21%` to `+0.18%` across the four Monte Carlo sets.
- Six fixed-seed building-order permutations for 24 residents changed cumulative coverage by less than `0.2%` in the generous baseline.
- `conda run -n standard python Scripts/plot_town_stage3.py ...` produced and visually checked the feasibility-frontier and reserve-trajectory figures. `git diff --check` passed before diary creation.

## Remaining

- Stage 4 must connect T1 fuel service to the real spherical heat field and building voxel temperature, then let starvation affect work and residents. Until that exists, stage-3 survival probability is not a heating reliability metric.
- The current automatic assignment materially overproduces one resource at larger populations and never rebalances against inventory demand. This is measured, not fixed; a later design decision is needed before changing gameplay assignment behavior.
- Stage 3 assumes unlimited mine relocation, ideal external logistics/ash removal, and scenario-level coal/meat processing capacities. Warehouse geometry and actual machine/network paths are intentionally absent.
