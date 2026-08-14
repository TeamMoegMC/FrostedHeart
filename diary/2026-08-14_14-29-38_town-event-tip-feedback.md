# Town event Tip feedback layer

- Time: `2026-08-14 14:29:38 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town signal aggregation, tower service debounce, transient networking, Tip runtime presentation/queueing, Mayor's Seal navigation, config, localization, tests, and model documentation`

## Completed

- Added a server-side transient notification path over the persisted town event model. Settlement bursts use latest-state-per-domain compaction, same-cause resident exits accumulate, and one server tick emits at most one tower batch plus one settlement brief to all online team members.
- Kept every observed tower service crossing in daily history while making the first loss immediate and coalescing later tower flapping through a 200-game-tick window. An unformed multiblock is now observed as not heating even if `GeneratorData.isActive` is stale.
- Added a safe S2C packet containing only notification ID, type, severity, and affected count. Runtime `Component` Tip contents remain outside the existing Tip Codec/archive/state format.
- Added severity-based transient Tip presentation, five-event truncation, event-page click-through, a dedicated client toggle, and a pure preemption queue that resumes an interrupted tutorial exactly once.
- Corrected daily hunting/work-loss severity to `WARNING` and fixed same-day same-cause resident exits being deduplicated to one.
- Documented the feedback layer in [`docs/town-model.md`](../docs/town-model.md) and added English/Chinese player text.

## Decisions

- Reserve Tip text names the configurable warning/danger lines instead of hard-coding the current 7/3-day defaults.
- Tower breakage is represented as `actualPos != null && isActive`; `isWorking` remains only the control switch.
- Disabling global Tips clears all queued content; disabling town event Tips drops current/queued town notifications only. Neither path replays discarded or offline events.

## Validation

- Focused event, packet, presentation, debounce, runtime-Codec, and Tip queue tests — successful.
- `./gradlew test` — successful, 104 tests and zero failures/errors before the final debounce boundary refinement; the final full rerun also succeeded.
- Stage-4 24-resident regression, 3 fixed seeds × 120 measured days — completed with finite output under `build/reports/town-model/simulations/town-tip-regression-3`.
- English and Chinese source locale JSON parsing and `git diff --check` — successful.

## Remaining

- In-game smoke-test loss/recovery timing, `/town tick 90` brief compaction, multi-player receipt, click-through, both client toggles, and tutorial interruption/resume ordering.
