# 居民详细实体 Top-K 与剔除缓存优化

- Time: `2026-08-19 19:54:35 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `citizen client config, detailed proxy selection, CPU batch culling, tests, and living documentation`

## Completed

- Added client config `maxDetailedCitizenEntities` with default 64 and range `0..128`; changes apply on the next client tick and zero disables all detailed proxies.
- Replaced distance-only proxy creation with allocation-stable `DetailedCitizenSelector` Top-K selection: 16-block entry, 20-block exit, four-block retention advantage, interaction priority, crosshair priority, distance order, and stable citizen-id tie-breaking.
- Reduced `FakeCitizenManager` from two full cache passes to one candidate/crosshair pass plus bounded selected and active-proxy passes. Non-selected near citizens remain owned by `ClientCitizenRenderer`.
- Replaced per-citizen per-frame culling `AABB` construction with snapshot-swept cached bounds covering both interpolation endpoints and the maximum 1.5-second dead-reckoning extrapolation.
- Added selector capacity/priority/hysteresis tests and cached-cull-bound geometry tests; updated both town rendering documents and proxy entity Javadocs.

## Decisions

- Keep the detailed limit client-only and independent from server AOI/presentation budgets.
- Use a primitive max heap and reusable candidate arrays so steady-state selection creates no per-candidate collections or boxed values.
- Promote the active trade target before the crosshair target, then rank retained and new normal candidates by effective distance and stable id.
- Accept conservative swept-frustum false positives in exchange for no render-frame allocation and no false culling during delayed snapshot extrapolation.

## Validation

- The complete focused citizen suite passed: 38 tests, 0 failures, 0 errors, 0 skipped. This includes `DetailedCitizenSelectorTest`, `ClientCitizenCullBoxTest`, and the existing citizen client regression coverage.
- The full Gradle test suite passed: 212 tests across 66 suites, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESSFUL` in 34 seconds). Compilation emitted only pre-existing JEI removal/deprecation warnings.
- `git diff --check` passed; Git reported only the repository's existing LF-to-CRLF working-tree warnings.

## Remaining

- Add the deterministic 32/64/256/1024 benchmark scene and in-game counters required to measure the 64-proxy p95 budget.
- Implement the M1 renderer ownership/backend boundary, then validate a static Flywheel thousand-instance PoC before replacing CPU-generated body geometry.
