# 居民可见预算、服务器总上限与床上睡眠展示实现

- Time: `2026-08-19 17:12:55 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `town citizen presence, cross-dimension visibility budgets, sleep synchronization, client rendering, server config, tests, and living documentation`

## Completed

- Added transient `CitizenSim.presentationFlags` with `PRESENT_ON_VALID_BED`; growth and swap-remove preserve it, while save/load intentionally does not persist it.
- Split `CitizenPresence.presentationEligible` from `interactionAllowed`. Valid-bed sleepers may render but never move, enter the spatial grid, or accept actions.
- Changed sleep, wake, and bed-layout refreshes to send immediate discrete snapshots or immediate despawns as appropriate; `ClientCitizen` snaps across sleep transitions instead of interpolating through walls.
- Added stable per-player Top-K AOI selection with active `TradeContainer` priority, a four-block retention advantage, and deterministic citizen-id tie-breaking.
- Added `maxVisibleCitizensPerPlayer` (`128`, range `0..4096`) and cross-dimension `maxVisibleCitizensPerServer` (`1024`, range `0..65536`). The server cap counts render relations, so one citizen visible to two players consumes two slots.
- Made server-wide selection run at server tick end, applying despawns before spawns. C2S citizen actions now require the citizen to be in the sender's authoritative tracked set.
- Prevented sleepers from creating `FakeCitizenEntity` instances and added horizontal near/mid-distance sleeper boxes plus a far-distance bed-plane outline with a low culling AABB.
- Updated `docs/town/hybrid-simulation-architecture.md` to describe the implemented budgets, packet flow, sleep lifecycle, rendering paths, persistence, and interaction checks.

## Decisions

- The global limit covers all players and dimensions and counts actual client presentation relationships, not unique citizen ids, because network, cache, and rendering costs are per observer.
- Per-player candidates are bounded first, then the server-wide primitive heap retains the globally best relationships. Scratch arrays and sets are reused; no persistent per-player/per-citizen visibility relation is saved.
- Existing tracked citizens rank as four blocks nearer. This gives a transitive total ordering while preventing small boundary movement from causing repeated spawn/despawn churn.
- Sleeping residents share the normal visibility budget and always use batch rendering, including at close range.

## Validation

- `./gradlew.bat compileJava compileTestJava --console=plain`: passed; only the project's existing 20 JEI removal/deprecation warnings and existing Mixin/unchecked notices were reported.
- Medium-effort test subtask ran `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.*" --console=plain`: 28 tests across 7 suites passed, 0 failed, 0 errors, 0 skipped.
- `git diff --check`: passed; only line-ending conversion notices were reported.

## Remaining

- Perform an in-game visual smoke test for all four bed facings, bed destruction/revalidation, night entry and staggered wake-up.
- Perform a two-player, cross-dimension smoke test while hot-changing both visibility limits, including `0`, and confirm each client cache plus the server-wide relation total remains bounded.
