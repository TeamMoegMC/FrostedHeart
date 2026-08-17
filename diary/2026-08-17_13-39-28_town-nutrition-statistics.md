# Town nutrition history and statistics view

- Time: `2026-08-17 13:39:28 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `town daily history, Mayor's Seal statistics UI, localization and compatibility tests`

## Completed

- Added `TownNutritionHistory` to each post-settlement `TownHistoryEntry`, recording average and P10 values for fat, carbohydrate, protein, and vegetables.
- Added a third “Nutrition” view to `TownStatisticsPanel` with four compact historical charts and adaptive plot height.
- Added healthy `70` and severe-deficiency `20` reference lines, bilingual labels, Codec compatibility, and capture/round-trip tests.

## Decisions

- Store nutrition in daily history instead of showing only live values so players can diagnose long-term diet effects.
- Show both population averages and the low tail; the UI calls P10 “weaker residents” to remain player-facing.
- Decode pre-feature history as unavailable and render line gaps. A missing old sample must not be interpreted as zero nutrition.

## Validation

- `./gradlew test --offline` — full suite passed after the history and UI changes.
- Locale JSON parsing and final whitespace checks are recorded in the task handoff.

## Remaining

- Perform an in-game visual smoke test for the three statistics buttons, four compact chart labels, colors, and reference-line readability at GUI scale.
