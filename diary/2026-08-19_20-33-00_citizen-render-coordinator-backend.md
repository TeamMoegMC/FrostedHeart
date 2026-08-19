# 居民渲染 Coordinator 与 Backend 边界

- Time: `2026-08-19 20:33:00 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `citizen client ownership, packet/render lifecycle routing, CPU backend fallback, tests, and living documentation`

## Completed

- Added `CitizenRenderBackend`, `CpuBatchCitizenBackend`, and `CitizenRenderCoordinator`; the existing CPU renderer remains the only production backend, so rendered geometry and packet formats are unchanged.
- Routed network spawn/update/despawn, benchmark add/update/remove, client tick, render, resource reload, dimension change, and logout through the coordinator. Despawn now releases an active fake entity immediately instead of waiting for the next client tick.
- Added exclusive `CitizenRenderOwnership`: awake selected proxies use `DETAILED_ENTITY`, remaining residents use `BODY_BATCH` through 64 blocks or `BILLBOARD_BATCH` through 96 blocks, sleeping residents never use detailed entities, and invalid/out-of-range values use `NONE`.
- Implemented candidate-backend prewarming: initialize, replay the current cache, verify health, atomically replace the current backend, then close the previous backend.
- Added failure isolation for non-CPU backend add/update/remove, client tick, render, clear, resource reload, and health checks. Failure switches to a prewarmed CPU backend; render failure does not draw CPU again in the partially rendered frame, avoiding duplicate geometry.
- Added a backend tick hook and routed deterministic benchmark movement updates through backend notifications so the future Flywheel backend observes the same test workload as the CPU backend.
- Added backend name reporting to `/citizen_debug metrics` and the debug HUD; updated both living town rendering documents.

## Decisions

- Keep the ownership policy stateless and derived from stable id proxy ownership, state, and distance rather than maintaining a second id-to-owner map that could drift from `FakeCitizenManager`.
- Treat CPU backend failures as programming errors and propagate them. Fallback only helps when a replaceable non-CPU backend fails.
- Allow at most one empty batch frame after a render-time backend exception; drawing CPU after a backend may have partially submitted would risk visible duplicate geometry.
- Retain the CPU renderer's reusable `BufferBuilder` capacity across worlds. It contains no citizen id state and is intentionally reused; cache, fake entities, metrics, and backend-owned id state are cleared.

## Validation

- Focused citizen suite passed after the final M1 changes. Coverage includes ownership distance/state boundaries, backend initialization rejection, atomic replacement, unhealthy resource-reload fallback, injected render failure fallback, and world-clear callbacks.
- Full Gradle suite passed: 225 tests across 70 suites, 0 failures, 0 errors, 0 skipped (`BUILD SUCCESSFUL` in 35 seconds). Compilation emitted only the existing JEI removal/deprecation warnings.
- An initial lifecycle test exposed two initialization assumptions: `FakeCitizenManager.clearAll` dereferenced a null test client, and coordinator logging initialized the complete `FHMain` registry graph. The final implementation tolerates a missing client during clear and uses a dedicated logger, after which the suite passed.
- `git diff --check` passed; trailing-whitespace search found no matches in the new M1 source, tests, or living documentation. Git reported only existing LF-to-CRLF working-tree warnings.

## Remaining

- Execute in-game M0/M1 smoke tests for benchmark commands, overlay, dimension/logout cleanup, resource reload, and unchanged CPU rendering.
- Implement and visually validate the M2 static Flywheel thousand-instance PoC before connecting dynamic snapshots or enabling any new backend by default.
