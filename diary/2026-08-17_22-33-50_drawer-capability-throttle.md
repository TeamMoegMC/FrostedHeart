# Storage Drawers capability transfer throttling

- Time: `2026-08-17 22:33:50 +08:00`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Storage Drawers controller/slave item capability and transfer tests`

## Completed

- Replaced the blanket controller/slave capability denial with a wrapped item handler that moves at most one item every 10 ticks on input and every 40 ticks on output.
- Shared cooldowns across all sides, slots, and capability wrappers of one controller or slave block entity; simulation and failed transfers do not consume cooldown.
- Kept non-item capabilities unavailable so Storage Drawers' custom repository capability cannot bypass the item transfer limits.
- Added unit coverage for exact cooldown boundaries, one-item batches, simulation, shared wrapper state, and independent input/output cooldowns.

## Decisions

- Limit successful item count as well as call frequency so bulk-transfer pipes cannot turn one allowed call into a full-stack transfer.
- Keep separate state per exposed controller/slave block entity. Multiple controller-slave access blocks can therefore provide parallel throughput.
- Use level game time and do not persist these short cooldowns across block entity unloads.

## Validation

- `./gradlew.bat compileJava --offline --no-daemon --console=plain` with Java 17 - passed.
- Targeted `DrawerItemHandlerThrottleTest` - passed.
- All test classes except `DailyKitchenTest` - passed. The unfiltered suite's first failure is the pre-existing `DailyKitchenTest` initializing `ItemStack` before Minecraft registries are bootstrapped, which then poisons the shared test JVM and causes 17 follow-on failures.
- `git diff --check` - no whitespace errors; Git only reported the existing LF-to-CRLF checkout warning for the modified Mixin file.

## Remaining

- Perform an in-game smoke test with a vanilla hopper and a bulk-transfer pipe or mechanical arm against both a controller and a slave block.
- Independently add Minecraft registry bootstrap setup to `DailyKitchenTest` if a clean unfiltered unit suite is required.
