# Supplier passive source behavior

- Time: `2026-08-18 21:38:05 +08:00`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `logistics supplier scheduling and focused logistics tests`

## Completed

- Removed the supplier chest's periodic `LogisticPushTask` submission, including its per-slot task keys and push worker.
- Kept the supplier connected to the logistics hub as a source-only grid, so requester tasks can still extract matching items from it.
- Renamed the focused PushTask test to state that it covers an explicitly queued task rather than supplier-driven behavior.
- Added the missing `ItemStack` and `TooltipFlag` imports in `LogisticChestBlock`, which were required to compile an already-present hover-text method during validation.

## Decisions

- Supplier chests are passive sources: no inventory moves from a supplier to storage unless another logistics task requests or reroutes it.
- `LogisticPushTask` remains available for explicit/internal routing because request delivery can use it for a carried remainder after its original destination cannot accept it.

## Validation

- `./gradlew test --tests com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetworkTest --offline --no-daemon --console=plain` with Java 17 - passed, 6 tests; main and test compilation passed.
- `git diff --check` - no whitespace errors; Git only reported LF-to-CRLF checkout warnings.

## Remaining

- Perform an in-game check that placing items in a supplier chest does not move them until a requester creates demand.
