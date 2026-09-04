# Warm stone temperature-refactor merge

- Time: `2026-09-04 20:54:00 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `merge origin/master@9424491bf into warm-stone and adapt wearable reservoirs to the refactored player and world thermal runtime`

## Completed

- Merged the main temperature refactor without retaining the removed Celsius-core or old environment-sample APIs.
- Mapped the normalized wearable player node to one atomic uniform delta across all five body-part energy nodes, preserving the whole-body `245000 J/K` ratio and frozen exchange rates.
- Reconnected dropped reservoirs to current live/last/dormant/natural Air sampling, analytic fields, physical and static-block radiation, a 64-entry same-tick position cache, and a separate `64/32/4/4` item receiver budget.
- Updated climate living docs, the completed implementation plan, discussion handoff, unit tests, and Forge GameTest coverage.

## Decisions

- Player radiation continues through the new watt-based body model. The frozen `q*0.8/6` equivalent-temperature conversion is now documented as reservoir-only.
- Player receiver limits remain `128 receivers / 64 visits / top 8 / 24 rays`; item witnesses cannot evict player witnesses.
- The Forge dropped-item case asserts positive production Campfire radiation and real ItemEntity state advancement. Occlusion and source-state transitions remain deterministic unit-test and accepted T25 manual evidence because Forge GameTests share a world with other live heat sources.
- No compatibility layer, balance change, dedicated sync, Campfire/charger recipe, client config, or `design/` edit was introduced.

## Validation

- JDK `17.0.2` targeted JUnit: `16` suites, `77/77`, zero failures/errors/skips.
- Full JUnit: `192` suites, `785/785`, zero failures/errors/skips. The lower total reflects main-branch test replacement/removal.
- Full Gradle build passed. Forge GameTest passed `17/17 required`.
- Initial Forge failures were diagnosed as invalid fixed-environment assertions in the shared test world; the test was separated into direct radiation and real ItemEntity lifecycle claims. One pre-existing async handoff assertion occasionally ran before its neighbor publication and now waits within its existing 1300-tick timeout.
- TheWinterRescue passed KubeJS syntax, seven JSON parses, research/quest/language/Hot Water NBT/no-charger assertions, and both diff checks. Full catalogue validation reports only the known `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra` missing `workbench` parents; no `warm_stone` error.

## Remaining

- Unticked containers pause and offline wall time is not replayed.
- The aggregate-temperature Tooltip/client-config diagnostic remains deferred until explicitly restored.
- No Campfire or charger recipe and no reservoir-specific synchronization exist.
