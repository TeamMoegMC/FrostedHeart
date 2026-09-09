# Thermal runtime loaded-world reconciliation design

- Time: `2026-09-08 00:38:47 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `runtime startup/source discovery investigation and proposed replacement of campfire discovery polling`

## Completed

- Verified that runtime startup does not attach already-loaded chunks, Page admission currently triggers source scans, and section mutation delivery depends on an attached owner.
- Distinguished recipe-reload runtime replacement from worker-only restart, which already reseeds retained main-thread indexes.
- Confirmed available loaded-chunk enumeration and pending/live BlockEntity position enumeration against the local mapped Forge sources and existing access transformer.
- Delivered the [implementation proposal](../plans/2026-09-08_00-38-47_thermal-runtime-loaded-world-reconciliation.md).

## Decisions

- Propose runtime-owned initial world attachment plus existing incremental events, with a temporary bootstrap cursor and the existing source recovery collection.
- Keep physical source discovery independent of Page admission and remove campfire polling during implementation.
- Preserve actual machine production callbacks; source discovery must not execute fuel-consuming machine ticks.
- This task requested a design. No Java, tests, or implemented behavior were changed. Living documentation therefore needs no current-behavior update in this task.

## Validation

- Read production runtime, Page/source ownership, machine producers, current GameTest reproduction, and local Forge lifecycle implementation.
- Reviewed the proposal against startup, reload, owner replacement, pending BE, unload/reload, capacity recovery, and worker restart paths.
- No builds, tests, or performance measurements were run for this design-only task. Earlier red/green GameTest results are historical evidence recorded in the linked September 1 diary, not rerun results.

## Remaining

- Implement and validate the draft plan. Bootstrap chunk budget and startup/steady-state performance remain unmeasured.
