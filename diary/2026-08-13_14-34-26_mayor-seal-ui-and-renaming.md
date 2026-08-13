# Mayor's Seal UI refinement and renaming

- Time: `2026-08-13 14:34:26 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Mayor's Seal overview/statistics/resident UI, operational history, town commands, naming packets, shared observation config, tests, and model documentation`

## Completed

- Split tower control and actual heating into separate overview rows. Tower mode no longer carries the accumulated overdrive value; tower condition now reports intact, recovering, deteriorating, or broken and uses a colored ten-cell bar whenever wear is nonzero.
- Replaced raw overview health/morale values with four bars in decision order: average health, average morale, weaker-resident health, and weaker-resident morale. Player-facing chart labels no longer expose the `P10` statistical term; the shared calculation remains the tenth percentile internally.
- Replaced the survival view's separate house/hunting temperature curves with one lowest-building-temperature curve. Temperature-bearing buildings opt into the traversal through `ITownTemperatureBuilding`; houses and hunting bases implement it now, and future building types can do the same without changing the observer.
- Raised the default configurable observation history from 30 to 90 town settlements. Existing operational history codecs accept the retired hunting-temperature field and migrate it into the generic building-temperature sample when no new sample exists.
- Added `/town tick [repeats]`, default 1 and constrained to 1–90. Each repeat performs one full `tickMorning`; refugee timing remains independent and unchanged.
- Added inline editing for the town title and for the selected resident's surname/given name. The server always resolves the sender's own team town, verifies resident membership, strips formatting/control codes, enforces bounded Unicode-safe names, permits an empty surname, and rejects an empty given name. Lightweight authoritative town-name sync prevents stale client display.
- Updated `docs/town-model.md` and both locales for the new player-facing semantics.

## Decisions

- Colored bars use ten cells and intentionally omit percentages/numeric `/ 100` values on the overview. Exact history values remain available in the statistics charts.
- The generic temperature observation includes every registered temperature-bearing town building, occupied or not. Occupied-house safety and staffed-hunting shutdown counts remain separate internal event inputs.
- The configured `historyDays` value means retained settlement snapshots, including manual `/town tick` advances; it does not affect `WorldClockSource` or refugee refresh.
- Name edits are server-authoritative and carry no client-supplied team identifier. Client editors retain pending text until the authoritative update arrives so valid changes do not briefly flash back to the old name.

## Validation

- `./gradlew test` — successful, 85 tests before the final packet-test addition.
- Targeted name normalization and town/resident packet round-trip tests — successful after the packet-test addition.
- Java compilation completed as part of both test runs; only existing deprecation warnings were emitted.
- English and Chinese locale JSON parsing and `git diff --check` — successful.

## Remaining

- In-game smoke test the visual spacing/color bars, 90-point chart readability, `/town tick 90` runtime, editing by Enter/click-away/Escape, empty-surname handling, empty-given-name rejection, and name synchronization for another online teammate.
