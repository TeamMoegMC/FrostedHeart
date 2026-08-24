# P2P Terminal GUI H01 Follow-up

- Time: `2026-08-24 16:49:37 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and validation role`
- Status: `completed implementation; in-game recheck pending`
- Scope: `P2P terminal screens, filters, rate controls, connection rows, buffers, localization, plans, and docs`

## Input

- The first in-game H01 screenshots showed a generic terminal title, three top-level tabs, clipped endpoint coordinates, unlabeled
  bidirectional buffers, and a capacity line whose total was clipped after `/`.
- The requested follow-up also required warehouse-interface-style mouse-wheel rate adjustment.

## Change

- Added concrete `P2PEndpointTerminalScreen` and `P2PBidirectionalTerminalScreen` layouts over the same validated
  `P2PTerminalMenu` protocol.
- Added role-specific container titles for shipping, receiving, and bidirectional terminals.
- Replaced the top-level send/receive tabs with one Filter tab. Bidirectional terminals select Input filter or Output filter inside it;
  Input maps to the pending-shipment send filter and Output maps to the received-item receive filter.
- Split the bidirectional 4+4 slots into left and right groups labeled Pending shipment and Received.
- Gave endpoint coordinates a dedicated fitted line, preserving the complete X/Y/Z text instead of keeping only the first wrapped line.
- Displayed remaining and total town capacity together without truncating the denominator.
- Added mouse-wheel submission over the rate field with the warehouse interface's exact `1/8/16/64` increments, synchronized maximum,
  high-resolution scroll accumulation, and existing server admission checks.
- Extracted `TransportRateScroll` and routed the warehouse interface through it so the two controls cannot drift.

## Documentation Impact

- Updated `docs/town/p2p-logistics.md` with concrete screen ownership, filter mapping, buffer labels, coordinate/capacity layout, and rate input.
- Updated the source plan and task T07 outcome; H01 remains open for a recheck of the revised screens.

## Validation

- P2P plus warehouse-interface focused matrix: 53 tests in 10 suites passed with zero failures, errors, or skips.
- `compileJava compileTestJava`: passed; only existing Mixin, deprecated API, and duplicate-resource warnings were reported.
- Full Java 17 regression: 531 tests in 139 suites passed with zero failures, errors, or skips.
- The source language fragments and merged `zh_cn.json` / `en_us.json` resources parsed successfully.
- Changed-file whitespace/final-newline checks and `git diff --check` passed.

## Remaining

- Reopen all three terminals in-game and inspect both concrete screens at common GUI scales in Chinese and English.
- Confirm mouse-wheel modifier steps and server rejection rollback in the running client.
