# Transport consumer full-sync and Mayor's Seal correction R03

- Time: `2026-08-21 23:35:47 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `full town synchronization, authoritative transport snapshots, Mayor's Seal transport details, tests, and documentation`

## Completed

- Reproduced the H01 defect: persistent `TransportReservation.CODEC` correctly omitted derived capacity, so decoding only the town data produced a live reserved-capacity cache of zero despite a nonzero morning report.
- Extended `TeamTownDataS2CPacket` with the same bounded `TownTransportSnapshot` contract used by `TownResourceUpdatePacket`. Full-sync handling now decodes town data, applies the authoritative snapshot, replaces the client instance, and then fires the existing single full-sync callback batch.
- Reordered the Mayor's Seal transport page to live summary, latest morning settlement, and a bottom device-detail control. Device details default to collapsed and retain their client-only expanded state across supplier-driven snapshot refreshes without resetting the detail scroll.
- Expanded rows retain stable endpoint ordering and distinguish interface coordinates from warehouse-core coordinates. They show one accepted setting, actual effective rate, server-derived distance factor, reserved transport capacity, and throttled/admission state; raw scale is no longer player-facing.
- Added clickable `TownInfoPanel.Row` support without changing existing read-only rows, plus Chinese and English labels for expand/collapse, distance factor, and throttled status.
- Updated living transport documentation and both implementation plans for R03.

## Decisions

- The full packet carries a snapshot rather than asking the client to recalculate derived capacity from local configuration.
- For a nonzero endpoint, the displayed distance factor is inferred from authoritative snapshot values as `reservedTransportCapacity / rateItemsPerSecond`. A disabled zero-rate endpoint displays no factor because division cannot recover it.
- R03 does not claim visual acceptance; default and large GUI-scale appearance remains part of H01.

## Validation

- R03 full/incremental synchronization, detail-state, stable-order, distance-factor, and localization matrix: 7 tests passed.
- Full `test compileJava --offline --no-daemon --console=plain`: 380 tests passed with zero failures, errors, or skips; compilation passed.
- `git diff --check`: passed.

## Remaining

- Execute R04's combined correction matrix, then repeat H01 single-player UI acceptance.
- Packet observation, disk-write observation, and multiplayer synchronization remain untested under the stated environment limitations.
