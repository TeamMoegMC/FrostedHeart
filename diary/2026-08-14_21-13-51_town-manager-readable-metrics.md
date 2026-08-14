# Town manager readable metrics and town day

- Time: `2026-08-14 21:13:51 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `Mayor's Seal overview/resident UI, town history synchronization, localization`

## Completed

- Replaced the pale town-name editor background with a dark header and added hover hints to town and resident name editors; resident editors now reserve a 36-pixel label column for English `Last`/`First`.
- Added a persistent `townDay` counter to `TeamTownData`. It advances once per completed settlement, survives Codec save/load, and accompanies incremental history packets so the open Mayor's Seal updates immediately.
- Added Town Day below the title and moved the overview panel down to avoid overlap.
- Added optional row tooltips to `TownInfoPanel`; overview health/morale bars now expose exact values on hover.
- Converted resident health, mental, strength, intelligence, and each work proficiency to colored ten-segment bars with exact hover values.
- Localized education levels 0–5 as Uneducated, Primary, Secondary, Higher, Master, and Doctoral, with Chinese equivalents and an out-of-range fallback.
- Updated the active `run/saves/20030716` server config from 30 to 90 retained history entries. The code default was already 90; the old per-world config caused the observed 30-day cap.

## Decisions

- Keep the statistics header truthful by showing the number of samples currently retained rather than claiming 90 days before 90 snapshots exist.
- Migrate old saves without `townDay` from their retained history length because the actual founding day was never stored and cannot be reconstructed after trimming.
- Attach exact values to hover tooltips while leaving compact bars as the normal player-facing presentation.

## Validation

- `./gradlew compileJava processResources` passed with existing repository warnings only.
- `./gradlew test` passed, including new town-day Codec and history-packet round-trip coverage.
- Both locale JSON files parsed with `jq`; `git diff --check` passed.

## Remaining

- Restart the test world so its server config reloads, then visually verify title/name hover areas, Town Day advancement, all resident bars, and history growth beyond 30 entries.
