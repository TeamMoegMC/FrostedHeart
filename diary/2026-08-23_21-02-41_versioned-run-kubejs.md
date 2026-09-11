# Versioned run KubeJS sources

- Time: `2026-08-23 21:02:41 +0800`
- Author: `Codex; OpenAI; implementation collaborator`
- Status: `completed`
- Scope: `.gitignore` rules for the local run directory

## Completed

- Kept generated and local runtime state under `run/` ignored while re-including `run/kubejs/` recursively for version control.
- No living game-system documentation changed because this only changes repository file inclusion.

## Decisions

- Used `run/*` plus explicit negation rules instead of `run/`, because Git cannot re-include descendants of a directory that is itself excluded.

## Validation

- `git status --short --untracked-files=all -- run` exposes only `run/kubejs/startup_scripts/src/registries/item.js`.
- `git check-ignore -v run/servers.dat` confirms non-KubeJS runtime state remains ignored.
- `git diff --check -- .gitignore` passed.

## Remaining

- None.
