# Hunting GUI player terminology

- Time: `2026-07-27 23:43:15 +0800`
- Author: `Codex /root; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Hunting Base GUI, en_us/zh_cn localization`

## Completed

- Renamed the Hunting Base effective-temperature label to `室温` / `Room temperature`.
- Replaced player-visible loot-roll/extraction terminology with `捕猎次数` and natural English hunt-count wording.
- Removed the fractional carry percentage row from the next-settlement forecast.
- Reworded the below-one fractional settlement message as an expected hunt count below one, without exposing the accumulator implementation.
- Updated planned, resource-supported, and actual hunting-count labels in both forecast and last-settlement sections.
- Reworded generated loot as loot obtained.

## Decisions

- Internal field, codec, config, and method names retain `roll` terminology because this change only concerns player-facing language.
- Fractional carry remains part of the production model and persistence; only its direct percentage observation was removed.

## Validation

- No player-facing Chinese `抽取` or English `loot roll` wording remains in the localization files.
- Both localization JSON files pass `jq empty`.
- `git diff --check` passes.
- `./gradlew build` passes. The repository-wide license-report task continues to emit its existing non-fatal list.

## Remaining

- Continue collecting in-game wording feedback for the other production and workability labels.
