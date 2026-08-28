# Thermal runtime architecture documentation refresh

- Time: `2026-08-27 23:47:14 +08:00`
- Author: `Codex; OpenAI GPT-5; primary documentation integrator with three Luna Max read-only reviewers`
- Status: `completed`
- Scope: `docs/climate thermal runtime architecture, lifecycle, source integration, player timing, navigation, and performance evidence boundaries`

## Completed

- Reconciled the living architecture, lifecycle, heat-source, player-temperature, and climate index documents with the exact-position 20-tick mutation implementation.
- Documented the two-cut source lifecycle: source flush precedes topology apply, commit only marks section-local bindings dirty, the next accepted cut emits rebind, and positive energy on the new sink requires a later nonzero interval.
- Distinguished the 20-tick routine geometry deadline, 5-tick steady solver cadence, immediate full-resync/identity invalidation path, and the GameTest 30-tick observation window.
- Defined `385 live cells / 936 Air pairs` as the seven-Page GameTest work set rather than a Page size or energy count.
- Clarified that `long[64] = 512 B` is a temporary dense mutation payload, while section owners and PageState desired overlays have eager sparse storage and each PageState retains `int[4096]` applied signatures.
- Separated the two pre-change JFR baselines from the still-missing controlled post-change JFR; no CPU, allocation, retained-memory, or cache acceptance claim was added.
- Incorporated independent read-only reviews from Luna Max agents covering architecture/transactions, lifecycle/source timing, and complexity/memory/performance evidence.

## Decisions

- Keep the synchronous server-main-thread architecture as current truth; `TemperatureThreadingPool` remains dormant legacy source with production lifecycle calls disabled.
- Treat topology publication, source rebind, and positive source integration as separate observable milestones.
- Keep structural complexity estimates explicitly separate from profiler rankings and production acceptance.

## Validation

- `git diff --check` reported no whitespace errors; only existing LF/CRLF conversion warnings.
- Relative Markdown link scan across the five touched climate documents found no missing targets.
- `git diff --name-only -- design` returned empty; `design/` was not modified.
- Java compilation/tests were not rerun for this Markdown-only refresh. The documented existing baselines remain `189/189` thermal JUnit and `12/12` required GameTests; the earlier complete build ran 784 tests with one unrelated hard-coded town-save fixture failure.

## Remaining

- Run the controlled post-change 120-second repeated-door JFR after a 10-second warmup and update the performance evidence and cache decision.
- Resolve or provision the unrelated `TeamTownActualSaveCodecProbeTest` local save fixture before claiming a completely green repository build.
