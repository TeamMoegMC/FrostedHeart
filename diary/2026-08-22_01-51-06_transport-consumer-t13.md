# Transport capacity consumer T13 knowledge closeout

- Time: `2026-08-22 01:51:06 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `town transport living documentation, consumer plans, H01 evidence boundary, and P2P prerequisite contract`

## Completed

- Closed the warehouse-interface transport consumer source plan and execution checklist after T00-T12, two H01 feedback rounds, and R01-R05.
- Updated `docs/transport_station_design.md` to describe the implemented warehouse consumer, authoritative full/incremental snapshot synchronization, and collapsed Mayor Seal endpoint details.
- Rechecked `docs/town/implementation-reference.md` and `docs/town/town-model.md`; they already contain the final ownership, formula, Codec, lifecycle, throttling, permissions, feedback, menu-limit, and scroll-step contracts.
- Updated the deferred P2P draft with the implemented generic endpoint APIs and removed its obsolete requested/active-rate premise. It remains a draft and may now begin P00 investigation.
- Kept `docs/README.md` unchanged because warehouse interface is still the only transport consumer; a separate logistics-system entry would be premature.

## Decisions

- The final persistent setting is one accepted `rateItemsPerSecond`. Admission rejection preserves an existing reservation or creates a disabled zero-rate record for a new endpoint; town shortage only changes the derived effective rate.
- P2P must reuse `TransportEndpointId`, `TransportEndpointRequest`, `TransportReservationResult`, the `TeamTown` mutation facade, and `TownTransportSnapshot`; it must add its own endpoint kind and server-side topology/distance validation rather than mutate the town Map directly.
- The user's final R05 acceptance closes the UI feedback loop, but does not retroactively prove every manual lifecycle, low-TPS, extreme-throughput, or item-conservation scenario. Those scenarios retain automated evidence and are recorded as residual manual-validation risk.

## Validation

- `cleanTest test compileJava --offline --no-daemon --console=plain`: passed with `386` tests, zero failures, errors, or skipped tests; compilation passed.
- Local Markdown links in the four T13-edited docs/plans: passed.
- `git diff --check`: passed.
- Single-player screenshots and feedback exposed two rounds of UI/state issues; R01-R05 addressed them, and the user accepted the final R05 result before authorizing T13.

## Remaining

- Network packet type/count/frequency observation: untested because no packet-observation tool is available.
- Actual idle-interface disk-write frequency: untested because no disk-write observation tool is available.
- Multiplayer synchronization, Tip delivery, and permission interaction: untested because multiplayer testing is currently unavailable.
- P2P devices, pairing topology, direct-link distance behavior, and transfer safety remain future work beginning at P00 in `plans/2026-08-21_01-39-26_transport-p2p-devices.md`.
