# Project knowledge and documentation system

- Time: `2026-08-19 14:11:13 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `AGENTS.md; docs/; plans/; discussion/; live documentation path references`

## Completed

- Defined the authority and lifecycle of human design, living system documentation, implementation plans, open discussions, project memory, and the development diary.
- Added a central system catalog and maintenance contract in `docs/README.md`, plus entry-point READMEs for the existing town, Curiosity encounter, and nutrition documentation.
- Established flat, timestamped collaboration conventions for `plans/` and `discussion/` without requiring subdirectories or manually maintained topic indexes.
- Marked existing mixed-purpose documents as transitional in their catalogs so proposals and imported conversations are not mistaken for fully verified current behavior.
- Corrected current Curiosity and town documentation paths in living prose and source comments. Historical diary entries were left unchanged.

## Decisions

- Kept structured navigation only under `docs/`; `plans/` and `discussion/` remain low-friction flat directories discovered by timestamped, keyword-rich filenames and full-text search.
- Made all of `design/` strictly read-only for agents, including file creation, deletion, moves, renames, and formatting.
- Kept source code and data as the final authority while requiring documentation updates in the same work when gameplay contracts change.
- Deferred splitting the large legacy town, encounter, and nutrition documents. Their new system READMEs describe their transitional status and safe reading boundaries.

## Validation

- Verified every local Markdown link in the new and updated navigation files resolves.
- Verified all newly listed code anchors against current source and removed stale live references to pre-reorganization documentation paths.
- `git diff --check` reported no whitespace errors.
- No build was run because the only Java changes were documentation comments and no runtime behavior changed.

## Remaining

- Incrementally rewrite transitional documents into focused current-system references as those systems are next changed; move remaining speculative material into new flat plan or discussion entries when it becomes active.
