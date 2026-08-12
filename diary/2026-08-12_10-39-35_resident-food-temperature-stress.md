# Resident food and temperature stress

- Time: `2026-08-12 10:39:35 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `housing resident effects, shared defaults/config, stage-1/2 diagnostics, plots, and model documentation`

## Completed

- Replaced the linear missing-food loss with a bounded power response. The default exponent is `2`, while the original zero-food health/mental maxima remain `8/5 points per resident-day`.
- Added independent bounded cold/heat stress outside the inclusive `[0,40]°C` housing range. Defaults are `20°C` to full stress, exponent `2`, and maximum direct health/mental losses `10/5 points per resident-day`.
- Added all five new controls to `TownModelParameters.Defaults`, `HousingParameters`, `FHConfig.SERVER.TOWN.HOUSING`, and the source audit. Gameplay uses runtime config values; the Java simulator uses the same pure `HouseDailyModel` functions with `TownModelParameters`.
- Expanded `ResidentEffects`, summary JSON, and both house sweep CSVs with food/temperature stress, split penalties, total penalties, recovery, and net changes. Regenerated the house-response plot and updated `docs/town-model.md` and the town README.

## Decisions

- Existing temperature and comfort ratings continue to scale recovery. Direct temperature damage is a separate additive term and does not multiply food stress, so each balancing control remains independently interpretable.
- The same `minimumTemperatureCelsius` / `maximumTemperatureCelsius` pair controls both resident assignment and the no-direct-stress range; no second pair of thresholds was introduced.
- Temperature stress saturates at the configured full-stress distance. This prevents unbounded daily losses while still making severe cold or heat dangerous.

## Validation

- `./gradlew test` — full suite passed.
- Fixed-seed stage-1/2 Java simulation passed: at full nutrition and `24°C`, 80% food satisfaction gives `+0.47921 health/day` and `+0.28657 mental/day`; at full food and `-10°C`, temperature stress is `0.25` and net changes are `-2.48293/-0.93637`.
- Final audit found `35` source files and `184` parameter entries; snapshot hash `26758ce2e8f30b82d1657dbf1b665a46ef63a5f37f19e11b765290e1313c3f16`. All five new defaults map to `FHConfig` fields.
- `conda run -n standard python Scripts/plot_town_stage12.py ...` regenerated both 320-dpi figures; the house response figure was visually inspected at `4110×1619` pixels.
- `git diff --check` passed.

## Remaining

- No cross-day resident/inventory feedback, climate, or T2 behavior was added; those remain stage 3+.
- The ignored development save `run/saves/20030716/serverconfig/frostedheart-server.toml` still contains the persisted old `maximumTemperatureCelsius = 50.0`. Source defaults are now `40.0`, but an existing world config remains an intentional runtime override until regenerated or edited.
- No new in-game smoke test was run for this formula change.
