# Generated Resource Git Line-Ending Stability

- Time: `2026-08-19 21:47:57 +08:00`
- Author: `Codex; GPT-5; primary coding agent`
- Status: `completed`
- Scope: `Git configuration and tracked generated resources`

## Completed

- Added repository-level `.gitattributes` coverage for `src/generated/resources/**` with LF line endings.
- Set repository-local `core.autocrlf=false` and `core.eol=lf`.
- Re-normalized the generated-resource index; the previous 2,053 false modifications no longer appear.

## Decisions

- Kept generated resources tracked because they are part of the existing project workflow; this fix changes line-ending handling instead of ignoring them.
- Left unrelated untracked work untouched.

## Validation

- `git add --renormalize -- src/generated/resources` staged no generated-resource content changes.
- `git diff --name-only -- src/generated/resources` returned no paths.
- `git check-attr text eol -- src/generated/resources/assets/frostedheart/blockstates/house.json` reports `text: set` and `eol: lf`.

## Remaining

- Commit `.gitattributes` and this diary entry when the repository is ready for the next checkpoint.
