# Mayor's Seal operational observability

- Time: `2026-08-13 12:27:05 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `town operational status, daily history/events, networking, Mayor's Seal UI, shared observation parameters, tests, and model documentation`

## Completed

- Added a shared, pure operational-status model and a gameplay adapter that reads current residents, warehouse resources, occupied buildings, `GeneratorData`, generator recipes, research, and `FHConfig`. Food and T1 fuel reserves now use exact shared equations; tower input and warehouse fuel are combined, while T2 is explicitly unavailable until heat-network load is modeled.
- Added per-second, server-authoritative status polling while the Mayor's Seal is open. Requests carry no town identifier, are rate-limited per player, and responses carry server time so the client can reject stale packets.
- Extended daily town history with an optional operational snapshot, same-day replacement, configurable 30-day trimming, incremental online sync, threshold crossings, recovery events, queued `GeneratorData.isActive` service crossings, and resident exit causes. Legacy saves decode without inventing zero-valued observations.
- Expanded the Mayor's Seal with decision-oriented overview fields, resident/survival chart views, a localized event-history page, missing-data line gaps, 7/3-day reference lines, current building-temperature thresholds, and residential/hunting detail diagnostics.
- Added observation defaults through `TownModelParameters.Defaults -> FHConfig`, reused the same defaults in stage-4 observers without changing their current results, exported them in stage-0 audit output, and documented the implementation in `docs/town-model.md`.
- Corrected the stage-4 daily aggregate formatter to match its emitted columns; this was exposed while running the requested small simulation regression.

## Decisions

- Current status refreshes every 20 client ticks, while charts and event history remain daily. The server always resolves the requesting player's own team town.
- T1 fuel days include loaded process ticks plus every whole fuel-recipe application in the tower input and warehouse, after current duration multiplier and research efficiency. T1 has no steam-level power surcharge; that field is written only by T2 logic.
- The tower control switch and actual heating state are deliberately distinct: UI and service events use `isWorking` and `isActive` respectively.
- Reserve crossings emit only the most severe event when one update crosses both 7- and 3-day lines. Recovery requires reaching at least 7 days.
- No gameplay settlement values, TWR recipes/datapacks, HUD, sound, chat warning, or tower automation were changed.

## Validation

- `./gradlew test` — successful after final compatibility adjustment; includes reserve equations, integer recipe consumption, normal/overdrive and research cases, T2 unavailability, threshold transitions, old/new codec behavior, same-day history replacement, trimming, packet codecs, request rate limiting, and stale-response rejection.
- Stage-4 small regression: baseline 24-resident scenario, three fixed seeds, 120 measured days — completed successfully with unchanged shared observation thresholds and finite output.
- `git diff --check` — successful.
- English and Chinese locale JSON files were parsed successfully.

## Remaining

- Perform the planned in-game smoke test for one-second inventory response, normal/overdrive T1 reserve changes, `/town tick` same-day replacement, persistence across reopen/restart, building-temperature warnings, resident risk/exit events, and T2 unavailable wording.
- T2 heaters and heat-network load remain stage 5 work; the UI intentionally does not estimate T2 fuel endurance.
