# TeamTownData staffing documentation

- Time: `2026-08-14 18:12:05 +0800`
- Author: `Codex; OpenAI GPT-5; primary development agent`
- Status: `completed`
- Scope: `src/main/java/com/teammoeg/frostedheart/content/town/TeamTownData.java`

## Completed

- Added JavaDoc for staffing-plan Codec migration, construction, normalization, server edits, client replacement, roster consistency repair, atomic daily assignment, target-crossing events, and client listener notification.
- Documented why work-building rosters are not capacity-trimmed through unordered sets before the deterministic planner runs.

## Decisions

- Documentation explicitly distinguishes planning from committing: the old roster remains intact while the pure assignment plan is calculated, then all work links are replaced together.
- Documented the side effect of `getStaffingPlan()`: it normalizes stale/missing building entries before returning the immutable plan.

## Validation

- `./gradlew compileJava` passed with only the repository's existing warnings.
- `git diff --check` passed.

## Remaining

- None.
