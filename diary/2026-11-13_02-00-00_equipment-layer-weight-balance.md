# Equipment layer weight balance

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; six real-client outfit checks passed
- Scope: equipment weight, generated armor data, polar bear lining eligibility

## Changes

- Weight factors by occupied layer count, outer to inner: [1], [1.1, 1.7], [1.5, 2.3, 3.5], [1.8, 3, 4.8, 7]. Empty lining slots do not consume an order position.
- Rebalanced clothing base weights and explicitly generated six vanilla armor set weights. Data-defined outer armor now exposes its weight attribute, matching lining data.
- All five polar bear clothing pieces accept lining placement.
- Updated docs/climate/player-temperature.md with the implemented formulas, units and defaults.

## Validation

- Production Java compilation passed. Initial offline runData failed due to three missing dependencies; online runData and subsequent processResources succeeded.
- Real Forge client, independent flat save weight-balance-20261113, survival mode. Clothing inserted through actual ClothesScreen slots; effects queried from server ActiveEffects, never injected heavy_load.
- Iron + hay: 9.7515, no heavy load; iron + wool: 9.7515, none; hide alone: 7.2, none; hide + rabbit: 13.518, level I; polar bear alone in lining: 11.2, level I; iron + polar bear lining: 26.568, level IV.
- Hands have one lining slot; mixed two-set outfits use the inner set glove. Weights calculated from generated JSON and recorded actual occupied GUI slots.
- Valid first-round records and original inventory/outfit screenshots are local under build/weight-validation/results.json and run/agent/screenshots/. Screenshots, worlds and logs are excluded from the commit.
- Screenshot cleanup attempt with hidden chat disabled command sending; those attempted replacement captures are not validation evidence. The original six results remain authoritative.
- git diff --check passed. No new full GameTest run or three/four-layer client matrix was performed.
