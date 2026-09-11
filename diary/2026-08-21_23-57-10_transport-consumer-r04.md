# Transport consumer correction regression R04

- Time: `2026-08-21 23:57:10 +08:00`
- Author: `Codex; OpenAI GPT-5; primary agent`
- Status: `completed`
- Scope: `single-rate admission, full/incremental transport synchronization, warehouse interface presentation state, tests, and plans`

## Completed

- Audited the R04 acceptance matrix against the R01-R03 tests. Existing coverage already locked rejected increases, unchanged reservations and dirty state, zero-rate rejected endpoints, single-rate persistence, shortage behavior, Mayor's Seal collapse/order, and the 4096-endpoint idle-read limit.
- Added a real full-packet then incremental-packet round trip. The client first restores a nonzero derived reservation of `28` from `TeamTownDataS2CPacket`, then converges to `56` through `TownResourceUpdatePacket` without falling back to the persistence-only zero cache.
- Routed the production incremental handler and packet test through package-private `TownResourceUpdatePacket#applyTo` so the regression exercises the same atomic apply path.
- Added pure presentation checks for the warehouse rate input, shortage recovery, and the rule that a failed new endpoint has a notification recipient only when the triggering operator UUID is known.
- Updated both transport implementation plans with the R04 result and next step.

## Decisions

- R04 retains no requested/active legacy migration case because this code has not been released and R01 intentionally replaced the development-only format.
- The notification recipient test covers policy without constructing a live `ServerPlayer`; multiplayer delivery remains a separate H01 environment limitation.
- No living behavior document changed: R04 added test seams and evidence but did not alter the R01-R03 player-facing contract.

## Validation

- Transport-directed tests: `33` passed with zero failures.
- Warehouse-interface tests: `16` passed with zero failures.
- Entire `com.teammoeg.frostedheart.content.town.*` test domain: `331` passed with zero failures.
- Full `test compileJava`: `383` tests passed with zero failures or errors; compilation passed.
- Legacy dual-rate search found only negative Codec assertions proving the fields are absent.
- `git diff --check`: passed.

## Remaining

- Repeat the executable single-player H01 scenarios, then complete T13 documentation and outcome closure.
- Packet-type/count observation, disk-write observation, and multiplayer synchronization remain untested under the stated environment limitations.
