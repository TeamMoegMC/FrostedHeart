# Transport station design

- Time: `2026-08-19 12:23:40 +08:00`
- Author: `Codex; OpenAI GPT-5; primary design agent /root`
- Status: `completed`
- Scope: `docs/transport_station_design.md; town building and transport-capacity architecture`

## Completed

- Reviewed the town architecture memory, town README, current building, staffing, persistence, UI, resource-action, and removal paths, plus the recent logistics diary entries.
- Reworked the transport-station design around the required `TownLogistics*`, `transport_station`, `TransportStation`, and `货运站` names.
- Split delivery into a town-building foundation, daily town-capacity production, and an optional robotics-logistics bridge, with registration, resources, compatibility, test, and in-game acceptance checklists.

## Decisions

- The first milestone is a complete building lifecycle without capacity output; production balance and consumer semantics are deferred rather than embedded as arbitrary constants.
- The current `TownStaffingPlan` controls inter-building priority; the deprecated `getResidentPriority()` must not be used for the new building.
- Town `TRANSPORT_CAPACITY` and the separate fixed-point robotics provider proposal remain distinct until an explicit ownership-safe adapter is designed.
- The requested English display name is recorded exactly as `TransportStation`; natural-language `Transport Station` remains an explicit pre-release confirmation.

## Validation

- Confirmed all referenced source and design paths exist.
- `git diff --check` passed; Git only reported the repository's LF-to-CRLF checkout warning.
- No compile or runtime test was run because this task changed documentation only.

## Remaining

- Before production implementation, decide resident attribute weights, output per standard worker-day, stockpiling rules, and whether robotics logistics is the first consumer.
- Implement and validate milestones A through C described in `docs/transport_station_design.md`.
