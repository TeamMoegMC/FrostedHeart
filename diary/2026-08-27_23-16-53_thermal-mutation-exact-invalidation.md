# Thermal mutation exact invalidation implementation

- Time: `2026-08-27 23:16:53 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation and lifecycle review agent`
- Status: `partial`
- Scope: `thermal geometry mutation intake, Page revision/publication lifecycle, sparse topology/material rebuild, source rebind integration, tests, and living documentation`

## Completed

- Replaced immediate mutation dependency expansion and the separate thread-external dense mask with one sparse-first exact-position accumulator and a fixed 20-tick dimension deadline.
- Deleted `GeometryDeltaCoalescer`, `GeometryDeltaRing`, `ThermalPage.latestBrickMutationRevisions`, their constructor/drain paths, and transport-only tests.
- Changed the resolved input lifecycle to carry stable `ThermalPage` identity, exact centers, `PAGE_REVISION`, and single-owner full snapshots.
- Added sparse desired-signature overlay, K-Brick staging/commit, reusable `BrickCompileScratch`, shared registry-cut geometry tables, exact face propagation, and separate material surface/binding dirtiness.
- Preserved main-thread authority, stale publication fallback, loaded-only capture, transactional Page/arena replacement, source event-time settlement, in-flight recovery, and idempotent retirement/close.
- Corrected the physical-source GameTest fixture from an undersized 256-cell limit to 1,024 cells. Its seven-Page continuation topology has 385 live cells and 936 pairs. Moved the ledger assertion to 30 ticks so it observes one real solve interval after the 20-tick topology cut and same-tick rebind.
- Updated the thermal runtime and lifecycle living documents. No cache or active thermal worker was added; `TemperatureThreadingPool` remains disabled legacy source.

## Decisions

- Keep exact positions rather than a permanent Page bitmap; sparse tiers are `short[8/16/32/64/128]`, with `long[64]` only after the 129th distinct position.
- Treat a rebind consumed at the current source cursor as a zero-length interval. Energy appears on the following solve interval; the test must not require positive routed energy at the rebind tick itself.
- Do not change the unrelated town save probe merely to make the complete build green.
- Keep the implementation plan `in-progress` until the controlled post-change JFR decides whether any cache follow-up is justified.

## Validation

- Independent Luna Max: `.\gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --offline --console=plain` succeeded: `189/189`, zero failures/errors/skips.
- Independent Luna Max: `.\gradlew.bat runGameTestServer --offline --console=plain` succeeded: all `12/12` required tests passed; no thermal work-limit warning and no source failure.
- Independent Luna Max: `.\gradlew.bat build --offline --console=plain` reached the full test suite but failed only in unrelated `TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`: missing hard-coded local save path; `784` tests completed, `1` failed.
- `git diff --check` reported no whitespace errors; only existing LF/CRLF conversion warnings.

## Remaining

- Repeat the controlled 120-second room-toggle JFR after a 10-second warmup, record exact counters/tick distributions/allocation/after-GC heap, and decide the separate cache question.
- Resolve or provision the unrelated `TeamTownActualSaveCodecProbeTest` save fixture before claiming a completely green repository build.
