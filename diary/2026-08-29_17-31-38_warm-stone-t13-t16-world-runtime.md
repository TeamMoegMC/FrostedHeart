# Warm stone T13-T16 world runtime

- Time: `2026-08-29 17:31:38 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `one-point radiation receiver, per-level item environment cache, exact dropped-item hook, transient lifecycle`

## Completed

- Generalized `RadiationService` so player three-point and item one-point queries share source discovery, top-K, DDA, witness, flux, and scratch logic while retaining separate receiver caches and work limits.
- Added `MinecraftThermalInput.sampleItemEnvironment`/`gameplayItemEnvironment` with publication/analytic/natural composition, one-point radiation, and a fixed `64`-entry per-level same-tick local cache.
- Added `DroppedReservoirExchangeHandler` and connected it only through `WarmStoneItem.onEntityItemUpdate` for UUID-staggered `20` loaded-tick cadence and dropped surface/environment exchange.
- Completed stale-sample, pickup/redrop/dimension/unload timing, NBT, and removal boundaries; updated climate living docs, the plan, and shared handoff.

## Decisions

- Player radiation remains `128 receivers / 64 visits / top 8 / 24 rays`; item radiation uses independent `64 / 32 / 4 / 4` limits and cannot evict player witnesses.
- The per-level item sample cache admits `64` unique quarter-block positions per game tick. Overflow and stale observations advance air-only rather than freezing or replaying against future conditions.
- Dropped cadence uses transient `ItemEntity.tickCount` and stable UUID buckets. No entity map, Stack timing NBT, wall-clock catch-up, Page admission, chunk loading, block scan, or entity enumeration was added.

## Validation

- JDK 17 takeover baseline: `49` suites, `255/255` tests.
- T13: `13/13`; T14: `14/14`; T15 final: `28/28`; T16: `30/30`.
- Expanded player/thermal/Curios regression: `50` suites, `266/266`, zero failures, errors, or skips.
- Focused forbidden-path scans found no dropped-path `WorldTemperature.air`, Page admission, chunk load, block scan, entity enumeration, static entity map, or `WeakHashMap`.

## Remaining

- T17 comprehensive one-point/publication/cache regression, T18 Campfire and occlusion Forge GameTests, and T19 bulk dropped-item workload/cleanup verification. Gate C remains open.
