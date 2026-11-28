# Labor dispatch validation

- Author: Codex
- Date: 2026-11-13
- Status: complete

Added `LaborDispatchGameTests.supplyPersistenceAndWithdrawal` under namespace `frostedheart_labor_dispatch`. Initial run exposed missing `ITownBuilding.BASE_CODEC` registration during town sync; registered `laborDispatch` alongside the persisted building registry. Corrected test fixtures to use `Resident.AGE_ADULT` and real block removal instead of modifying the read-only building map.

Final offline `runGameTestServer` completed successfully: all 1 required tests passed, exit code 0, BUILD SUCCESSFUL in 2m 42s. Log: `build/labor-dispatch-validation-final.log`. `git diff --check` passed. Earlier failed runs are retained in `build/labor-dispatch-validation.log` and `build/labor-dispatch-validation-rerun.log`.

Coverage: core registration; eligible worker count; direct building and town wire dispatch codec round-trips; health and structure invalidation; stale extra provider reset by town tick; real core removal and building unregistration. Not covered: room scanning, daily staffing, positive supply to machine levels, client rendering, separate-process restart. Updated living documentation `docs/town/labor-dispatch.md` with evidence and limits.
