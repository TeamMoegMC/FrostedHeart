# Town resident food level tags

- Time: `2026-08-10 17:48:12 +0800`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `resident food level tags, tag datagen cleanup, resource regression tests, town model documentation`

## Completed

- Replaced the single mixed resident-food list with five explicit, mutually exclusive FH/vanilla tag files for levels 0–4.
- Classified dangerous/unmodelled ingredients as level 0, basic raw foods as level 1, ordinary cooked foods and staples as level 2, compound/high-density meals as level 3, and complete/rare foods as level 4.
- Ensured every built-in raw/cooked meat pair has a strictly higher cooked level. Added previously omitted vanilla apple, poisonous potato, and rotten flesh.
- Removed the stale generated level-0 bread tag and its datagen statement so the hand-maintained files are the single built-in source of truth.
- Added a resource regression test that rejects cross-level duplicates and locks the raw/cooked ordering.

## Decisions

- Kept non-edible items from the historical resident-food list at level 0 instead of silently removing them; they remain emergency fallback inputs pending a dedicated tag audit.
- Kept cake at level 0 because the item has no `FoodProperties` and town settlement does not model its placed slices.
- Used `replace: false` so companion packs can extend each level. External cross-level additions must be detected by the future runtime audit.
- This task implements cross-level priority only. Deterministic ordering within one level remains separate work.

## Validation

- Parsed all five JSON files: 75 total built-in entries and no duplicates; counts are 16/25/20/10/4 for levels 0–4.
- `./gradlew test` — successful; the new tag regression and the existing town tests passed.
- `processResources` produced exactly one resident-food tag file for every level 0–4.

## Remaining

- Implement deterministic same-level food ordering using an explicit quality/energy rule and stable item-ID tie-breaker.
- Review TWR KubeJS additions separately; its current level-0 additions are outside this FH/vanilla classification pass.
