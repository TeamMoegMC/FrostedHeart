# Transport Capacity H03 and Consumer Plan

- Time: `2026-08-20 16:57:40 +08:00`
- Author: `Codex; OpenAI GPT-5; planning and documentation role`
- Status: `completed`
- Scope: `transport-station H03 decisions and deferred transport-capacity consumers`

## Completed

- Froze the Transport Station production parameters: `64` capacity per standard worker-day, attribute weights
  `35 / 15 / 30 / 20`, attribute productivity `0.5～1.5`, maximum proficiency bonus `0.8`, and final productivity
  `0.5～2.3`.
- Replaced the old persistent-stock assumption with a daily town-service lifecycle: reset before station work, rebuild from
  all workable stations, then publish the town aggregate report.
- Split per-station production reporting from town-owned total and occupied capacity reporting.
- Added a separate draft plan for future endpoint reservations, rejected-interface state, P2P Manhattan distance,
  warehouse cube-root volume distance, proportional throttling, UI state, and Tips.
- Updated the existing Transport Station design and task checklist so milestone B does not absorb future logistics
  consumers.

## Decisions

- `resources[TRANSPORT_CAPACITY]` remains the single current-total authority; `TownTransportState` owns aggregate reports
  and will later own the endpoint reservation Map.
- A rejected new endpoint remains registered with active rate and reserved capacity set to zero and requires manual player
  reconfiguration.
- The exact function converting rate and distance metric into reserved transport capacity remains intentionally open until
  endpoint implementation.

## Validation

- Reviewed the revised design, task checklist, and future plan for stale cross-day-stock wording and conflicting ownership.
- `git diff --check` passed.
- No source code changed, so compilation and automated game tests were not required.

## Remaining

- Execute Transport Station tasks T09 through T13.
- Resolve the open occupancy formula and endpoint-specific implementation questions before starting the deferred consumer
  plan.
- No living `docs/town/` update was needed because implemented behavior did not change.
