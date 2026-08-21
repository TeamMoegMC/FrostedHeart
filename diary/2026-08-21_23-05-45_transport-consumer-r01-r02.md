# Transport consumer single-rate correction R01-R02

- Time: `2026-08-21 23:05:45 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `transport reservation contract, warehouse-interface admission feedback, menu view, tests, and living documentation`

## Completed

- Replaced the unpublished requested/active transport pair with one persisted accepted `rateItemsPerSecond` across parameters, requests, reservations, codecs, snapshots, town APIs, configuration, audit output, menus, and Mayor's Seal call sites.
- Existing endpoint admission rejection now returns the unchanged reservation without marking transport state dirty. A rejected new endpoint is retained as a bound `DISABLED` reservation with rate and reserved capacity both zero.
- Simplified the warehouse-interface menu to current effective item rate, reserved transport capacity, remaining available transport, and total transport. The effective-rate row turns red under town-wide throttling; raw scale and ambiguous labels were removed.
- Rejected manual increases are transient menu feedback and the input returns to the accepted server value. A failed initial admission notifies only the player who placed the interface; load and background scans do not broadcast.
- Updated the transport implementation reference, town model, source plan, and execution checklist for the implemented R01-R02 behavior.

## Decisions

- No compatibility codec, data fix, or migration was added for the unpublished dual-rate development data. Old transport reservation data may be discarded as explicitly accepted for R01.
- `INSUFFICIENT_CAPACITY` remains a transient `TransportReservationDecision`, not a persisted admission status or block visual state.
- R03 remains responsible for the known Mayor's Seal full-sync reserved-capacity defect and the default-collapsed endpoint detail layout.

## Validation

- R01-R02 directed matrix: 47 tests passed with zero failures or errors.
- Full `test compileJava --offline --no-daemon --console=plain`: 378 tests passed with zero failures, errors, or skips; compilation passed.
- `git diff --check`: passed.

## Remaining

- Execute R03 and R04, then repeat H01 single-player UI acceptance. Default and large GUI-scale visual fit has not yet been rechecked.
- Packet observation, disk-write observation, and multiplayer synchronization remain untested due to the stated environment limitations.
