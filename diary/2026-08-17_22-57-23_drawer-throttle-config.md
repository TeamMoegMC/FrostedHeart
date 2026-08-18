# Configurable Storage Drawers transfer cooldowns

- Time: `2026-08-17 22:57:23 +08:00`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `server config and Storage Drawers capability throttling`

## Completed

- Added server config values `Storage Drawers.inputCooldownTicks` and `Storage Drawers.outputCooldownTicks`, defaulting to 10 and 40 with a minimum of one tick.
- Replaced fixed throttle constants with runtime config suppliers while retaining shared per-block input/output cooldown state.
- Added regression coverage using non-default 3/7 tick cooldowns.

## Decisions

- Read the configured interval when a successful transfer starts its next cooldown. Reloaded values therefore apply to subsequent successful transfers without recreating the capability wrapper.
- Keep the values in the synced server config because they affect server-authoritative inventory behavior.

## Validation

- Targeted `DrawerItemHandlerThrottleTest` with Java 17 - passed.
- All test classes except the pre-existing bootstrap-broken `DailyKitchenTest` - passed.
- Staged and unstaged `git diff --check` - no whitespace errors; only repository LF-to-CRLF checkout warnings were reported.

## Remaining

- Perform an in-game config reload/restart smoke test with a hopper and a bulk-transfer pipe.
