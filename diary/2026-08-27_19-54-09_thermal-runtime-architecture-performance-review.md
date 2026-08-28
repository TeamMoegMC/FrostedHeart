# Thermal runtime architecture and performance review

- Time: `2026-08-27 19:54:09 +08:00`
- Author: `Codex; OpenAI GPT-5 primary documentation agent, with three gpt-5.6-luna max read-only reviewers`
- Status: `completed`
- Scope: `docs/climate thermal runtime architecture, lifecycle, complexity, optimization candidates, documentation index, and contract corrections`

## Completed

- Added `docs/climate/thermal-runtime-architecture-and-optimization.md` as the current cross-layer contract for per-dimension ownership, Page/Brick/arena/fragment state, identity and watermarks, admission/mutation/tick/unload lifecycle, topology transactions, source ledger, solver recovery, publication, material/phase, radiation, complexity, and validation gaps.
- Recorded the localization work already implemented: section-indexed Page/source binding, dirty-Brick replacement, indexed free spans, deferred dirty-owner drain, local material/FarField refresh, bounded natural/sky work, transactional topology patching, substep rollback, exact in-flight recovery, and unchanged hard-cap suppression.
- Recorded P0/P1/P2 optimization candidates without claiming measured gains. The highest structural candidates are live-slot publication, active/touched source accumulators, structural-revision preflight caching, live-storage full rebuilds, and state-set sum traversal.
- Updated the climate README reading order and linked the new runtime document from the integration and heat-production documents.
- Corrected stale documentation: topology apply uses `APPLIED`/`TOPOLOGY_UNCHANGED` as success states while `DUPLICATE` is a scheduler seal result; `DimensionThermalRuntime.Diagnostics` is deleted; gameplay has seven capacitive material profiles; the latest baseline is 187 thermal JUnit tests and 12 required Forge GameTests.
- Incorporated final reviewer corrections for `T_reference + H/C`, topology `PageState` signature ownership, queue-full unload retention, recovery-tick epoch count, complete apply statuses, Page-slot high-water complexity, per-span arena work, solver state-set scans, and global Page discovery during chunk unload.

## Decisions

- Keep the current synchronous server-main-thread architecture as the documented production contract. `TemperatureThreadingPool` remains disabled legacy source and no active thermal `Executor` is implied.
- Keep current behavior and candidate work visibly separate. Static complexity findings require representative-save JFR before implementation order or performance gains are claimed.
- Preserve generation-safe reuse, source event ordering, compensated floating-point traversal, topology transactionality, phase ACK authority, natural fallback, and idempotent lifecycle handling in any future optimization.
- Keep detailed thermal traversal ownership in the new runtime document; retain `data-lifecycle-and-integration.md` for cross-system recipe, capability, network, configuration, and command integration.

## Validation

- Three independent `gpt-5.6-luna` reviewers at max reasoning audited architecture/lifecycle, documentation contracts, and performance traversals, then reviewed the final document. All concrete corrections were incorporated.
- Verified every relative Markdown link in the four changed climate documents resolves.
- `git diff --check`: passed; only the repository's existing LF/CRLF conversion warnings were reported.
- Searched the climate documents for stale `175/175`, thermal `10` GameTests, `APPLIED`/`DUPLICATE` apply wording, dormant capacitive profiles, and live `DimensionThermalRuntime.Diagnostics`; no stale positive contract remains.
- Gradle was not rerun because this change only edits Markdown. The documented `187/187` JUnit and `12/12` Forge GameTest baseline comes from `diary/2026-08-27_05-10-05_thermal-substep-rollback-hardcap-suppression.md`.

## Remaining

- Run representative large-save and multiplayer JFR before selecting or implementing the candidate traversal optimizations.
- Add automated coverage for first-player admission, cross-section movement, recipe reload cache invalidation, long-lived physical sources, real `ServerLevel.tickChunk` Mixin equivalence, and multiplayer P95.
- Evaluate calling `WorldTemperature.clear()` from recipe reload so world/biome natural-temperature caches cannot retain old recipe values.
