# Transport transfer cadence implementation

- Time: `2026-08-26 19:45:34 +08:00`
- Author: `DeepSeek Harness coding agent; work-like-codex; implementation and validation role`
- Status: `completed`
- Scope: `TransportTransferBudget`, warehouse-interface block-level scheduling, P2P sender scheduling, related tests and living town docs

## Completed

- Extended `TransportTransferBudget` into a runtime-only state machine:
  - configured rate → base interval mapping (`20/10/5/2/1` ticks for `1..32/33..64/65..128/129..640/641..1280` items/s);
  - absolute-game-time due checks, one-second token bucket capped at 64, fractional carry-over, adaptive failure/success intervals (max 80, half toward base);
  - first success after backoff clears remaining tokens; pause/wake/reset lifecycle without offline catch-up.
- Rewrote `TransportTransferBudgetTest` with interval boundary, failure/success, fractional `17.25`, capacity, backoff-clear, independent-instance, pause/reset, wake, and low-rate gating coverage.
- Integrated `WarehouseInterfaceBlockEntity` with block-level scheduling: cheap checks before resource actions, `needsBalance` as work flag, `nextAttemptTick` as run gate, one shared budget across all nine slots, whole-run success/failure feedback, watcher wake without clearing backoff.
- Integrated `P2PTerminalBlockEntity` sender scheduling:
  - removed `FAILURE_COOLDOWN_TICKS`, `failureCooldown`, `hasTransferDemand`, `P2PFairTransferScheduler`, `incoming.size()`/`sourceCount` rate compensation;
  - positive budget is required before resolving source/target item handlers;
  - multiple due senders can run in the same server tick; each sender uses its own effective rate.
- Simplified `P2PItemTransfer.Result` by removing cooldown-only fields while preserving simulation-first commit and recovery-stack logic.
- Updated `docs/town/p2p-logistics.md` and `docs/town/implementation-reference.md` to describe the implemented cadence model.
- Updated both transport plans’ statuses and task checklists; added this diary entry.

## Decisions

- Redstone pause uses `pause()` (clear tokens, preserve current interval) instead of `reset()`, because only a real successful transfer may shorten the backoff interval.
- Unbound, zero-rate, invalid-reservation, topology-unavailable, and removal paths use full `reset()` because those states invalidate the scheduling context.
- The receiver-side 20-tick container probe is kept unchanged and is not part of sender scheduling state.
- The unrelated `TeamTownActualSaveCodecProbeTest` was not modified; it reads a hardcoded macOS path that does not exist on this Windows environment.

## Validation

- Focused Java 17 tests passed:
  `TransportTransferBudgetTest`, `WarehouseInterfaceBalanceTest`, `P2PItemTransferTest`.
- Town system tests: `430` tests completed, `1` failed (`TeamTownActualSaveCodecProbeTest.actualSaveSurvivesTheFullSyncCodec`, `FileNotFoundException` at hardcoded `/Users/wyc/...` path).
- Full test suite: `806` tests completed, `1` failed (same unrelated environment-specific test).
- `git diff --check` passed.
- Grep confirmed no remaining production references to `failureCooldown`, `P2PFairTransferScheduler`, `hasTransferDemand`, or `incoming.size()` rate compensation.
- Unit-level performance evidence: counting `IItemHandler` test confirms zero source/target slot reads while the scheduler has no positive budget; `sourceScanCount` is incremented only after a positive-budget attempt resolves handlers.

## Remaining

- The full Gradle suite is red only because of the pre-existing environment-specific `TeamTownActualSaveCodecProbeTest`; no gameplay or transport test failed.
- No in-game before/after profiling was run; the performance claim is supported by the structural gating and counting-handler test, not by a full server benchmark.
