# Warm stone T22 pack content

- Time: `2026-08-29 19:20:14 +08:00`
- Author: `Codex; OpenAI GPT-5; primary engineering agent`
- Status: `completed`
- Scope: `TheWinterRescue research, quest, Create-style tooltip, and FrostedHeart integration documentation`

## Completed

- Added `config/fhresearches/warm_stone.json` as a `hand_warmer` child that unlocks the warm-stone and hot-water-bag recipe effects.
- Added optional `Carry The Warmth` quest `6A97729E570341EF` after the existing `t0` Campfire quest, with separate acquisition tasks for both items.
- Added English and Chinese Create-style tooltip, research, and quest text covering the dedicated `warm_stone` slot, two-way heat transfer, dropped charging beside a registered physical heat source, and paused temperature evolution in unticked containers.
- Added `!/fhresearches/*.json` to TheWinterRescue `config/.gitignore` so the new research definition is not masked by the root configuration ignore rule.
- Updated `docs/climate/player-temperature.md` with the companion research, quest, and tooltip integration anchors and limits.

## Decisions

- Progress text describes a burning Campfire only as an example of the existing registered physical-source receiver path; it does not create a Campfire recipe.
- Text does not claim or imply a reservoir charger path. The Hot Water Bag tooltip only documents the already-implemented Hot Water fill result.
- T23's test Stack and manual-observation tooling were not started. The deferred aggregate-temperature Tooltip/config follow-up remains out of scope until explicitly restored by the user.

## Validation

- Parsed all changed companion JSON resources, checked KubeJS syntax, validated research parent/effects and bilingual key presence, and checked the new FTB Quest SNBT fragment's balance, references, and identifiers.
- JDK `17.0.2`: forced player/thermal/Curios regression passed `53` suites and `277/277` tests, with zero failures, errors, or skips.
- `git diff --check` passed in FrostedHeart and in both worktree/index modes for TheWinterRescue.
- Full `validateResearchCatalog` was blocked by pre-existing missing parent `workbench` errors in `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra`; it did not report `warm_stone`. The environment rejected the isolated temporary-catalog setup, so no temporary files were created.

## Remaining

- Stop before T23. T23 owns development test stacks and manual observation tools; T24-T28 own broader validation, gameplay matrix, documentation consolidation, two-repository verification, and final outcome.
