# Mayor's Seal Virtual Resources View

- Time: `2026-08-20 22:21:43 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Mayor's Seal virtual-resource navigation, warehouse/transport summaries, localization, and UI metrics`

## Completed

- Added `TownVirtualResourcesTab` after the building tab and updated the event-tab index used by event-tip navigation.
- Added a left-side selector over every `VirtualResourceType`, specialized warehouse-capacity and transport-capacity
  detail pages, and a generic fallback for future virtual-resource types.
- Displayed warehouse total, occupied, available, shortfall, utilization, and status from `TeamTownResourceHolder`.
- Displayed transport total, reserved, available, shortfall, effective transfer scale, and settlement availability from
  the synchronized transport resource and `TownTransportState.DailyReport`.
- Added bilingual labels, resource icons, selector hover names, and reusable unframed/resettable `TownInfoPanel` support.
- Fixed a `TownTransportState.DailyReport` static-initialization cycle exposed when the nested report class loaded before
  the outer state class.

## Decisions

- The view reads the existing client town snapshot and `TownResourceUpdatePacket`; no parallel request or UI-only state
  synchronization was introduced.
- Transport reservation remains the morning report value, currently `0`, until consumer endpoint reservations exist.
  The future endpoint Map must become the source for the live reservation summary without changing this navigation UI.
- Capacity pages clamp displayed availability to zero and show a separate shortfall when usage exceeds capacity.

## Validation

- `compileJava --offline --no-daemon --console=plain` passed with JDK 17.
- Targeted metrics, transport-state, and resource-packet tests passed after the initialization fix.
- The complete `test --offline --no-daemon --console=plain` suite passed: 332 tests, 0 failures, 0 errors, 0 skipped.
- Both language JSON files parsed successfully and `git diff --check` passed.

## Remaining

- Perform in-game visual acceptance for both locales when convenient.
- When transport consumers are implemented, replace the daily-report reservation source with the live endpoint Map
  aggregate and retain the morning report only as historical settlement data.
