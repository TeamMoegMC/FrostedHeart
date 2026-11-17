# Labor system documentation

- Author: Codex
- Date: 2026-11-13 UTC
- Status: complete; documentation only
- Scope: labor dispatch, team worker pool and machine allocation contracts

Expanded `docs/town/labor-dispatch.md` against `LaborDispatchBuilding`, `LaborDispatchBlockEntity`, `ITownResidentWorkBuilding`, `ResidentDailyModel`, `TeamTownData.tick`, and `MachineLevelManager`. Documented resident eligibility defaults, connectivity gates, two allocation stages, capacity rather than daily output, desired/actual machine levels, rounding and shortages, provider channel ownership, and persistence/recomputation. Corrected the town index table separator and clarified its labor entry.

Validation: reviewed formulas and method names against current source; checked relative Markdown links and `git diff --check`. No production code or resources changed, so no build or gameplay rerun was needed. Existing GameTest/datagen evidence and its untested scenarios remain explicitly attributed to earlier validation in the living document.
