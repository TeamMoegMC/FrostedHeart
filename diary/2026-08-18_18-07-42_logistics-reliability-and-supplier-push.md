# Logistics reliability and supplier push

- Time: `2026-08-18 18:07:42 +08:00`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `robotics logistics core, network scheduling, grid caches, workers, persistence, and tests`

## Completed

- Bound the formal logistics multiblock registration to `LogisticCoreLogic`/`LogisticState`, and added deferred network loading, dirty marking, capability initialization, shutdown, and chunk-registration release.
- Connected supplier chests to active push tasks. Suppliers now enqueue one deduplicated task per occupied slot and only extract after a fillable storage target is available.
- Made carried items recoverable on task completion, failure, cancellation, network replacement, and core shutdown. Remainders first return to fillable network storage and otherwise drop at the core.
- Persisted active tasks and retry counters, restored task deduplication keys after load, and added bounded retries for temporarily unavailable requesters or storage targets.
- Replaced shuffled task admission with stable FIFO scheduling, fixed queue draining, and retained the 20-active-task limit without random priority inversion.
- Added explicit worker detachment when changing or losing networks, nearest-core selection, periodic full hub/child revalidation, stale-cache pruning, and block-break inventory drops.
- Reworked requester admission to one in-flight task per filter, avoiding empty-slot index underflow and repeated over-allocation.
- Added focused unit coverage in `src/test/java/com/teammoeg/frostedheart/content/robotics/logistics/LogisticNetworkTest.java`.

## Decisions

- Source-only supplier grids remain indexed for extraction but are excluded from placement, so active pushes cannot loop back into supplier inventory.
- A carried stack retries an unavailable destination for 15 work cycles at 20 ticks each, then uses the common recovery path; this tolerates short unloads without reserving a worker forever.
- Queued tasks are not persisted because they have not extracted items and worker blocks recreate demand/supply tasks after load. Active tasks are persisted because they may own extracted stacks.
- The hub performs a full child and aggregate cache rebuild every 200 network ticks as a correctness fallback alongside incremental updates.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetworkTest --offline --no-daemon --console=plain` with Java 17 - passed, 6 tests; main and test compilation passed in the same build.
- Unfiltered `./gradlew test` - 170 tests executed, 18 failed. The first failure is the pre-existing `DailyKitchenTest` registry initialization error documented in `diary/2026-08-17_22-33-50_drawer-capability-throttle.md`; 17 later failures are follow-on shared-JVM initialization errors.
- `git diff --check` - no whitespace errors; Git only reported LF-to-CRLF checkout warnings.

## Remaining

- Perform an in-game smoke test for formal multiblock assembly/disassembly, supplier-to-storage transfer, requester delivery, chunk unload/reload, server restart with an in-flight stack, and deterministic selection between multiple nearby cores.
- Network ownership/team isolation remains unchanged because this subsystem has no established worker ownership contract yet.
