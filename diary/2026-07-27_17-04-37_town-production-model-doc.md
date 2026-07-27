# Town hunting and mining production model documentation

- Time: `2026-07-27 17:04:37 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `docs/town-hunting-and-mining-productivity-model.md`

## Completed

- Added one Chinese reference document covering the shared resident productivity formula and the complete hunting and mining settlement chains.
- Documented Minecraft units, default config values, building and assignment effects, terrain-resource caps, loot or biome weights, warehouse behavior, examples, and current implementation caveats.

## Decisions

- Kept both systems in one document at the user's request so their common \(S_i\) definition and different downstream settlement rules can be compared directly.
- Described current code behavior rather than an intended future design, including missing automatic proficiency growth, nonrecovering chunk ORE, mining weight coupling, and different warehouse failure modes.

## Validation

- Verified all five relative source links resolve.
- Rechecked the standard-worker defaults: hunting `1.75` expected items/day and mining `3.5` item units/day.
- `git diff --check` reported no whitespace errors.

## Remaining

- None.
