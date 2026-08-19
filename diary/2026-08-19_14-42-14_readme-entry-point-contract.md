# README entry-point contract

- Time: `2026-08-19 14:42:14 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `AGENTS.md; non-root READMEs; town and scenario references; project memory`

## Completed

- Added the four-question README contract to `AGENTS.md` and centralized concise documentation-writing rules there.
- Reduced every tracked non-root README to local scope, authority, next reading, and minimum action. The root `README.md` was intentionally unchanged.
- Moved scenario field and output details to `Scripts/town_scenarios/scenario-reference.md`.
- Replaced the large source-package town guide with a short pointer and a focused `docs/town/implementation-reference.md` covering durable lifecycle, extension, and synchronization invariants.
- Corrected the documentation status in `.Codex/memory/architecture.md`.

## Decisions

- READMEs are entry points, not full references; detailed mechanics, schemas, and implementation invariants live in purpose-named documents.
- Rules have one owner and are linked rather than repeated across directory indexes.
- The town implementation reference is `Partial` because it deliberately covers high-risk extension contracts rather than the whole subsystem. The relocated scenario reference remains `Transitional` pending a source-by-source content audit.

## Validation

- Verified every local Markdown link in the changed entry points and new references resolves.
- `git diff --check` passed.
- Verified the root `README.md` content hash matches `HEAD`.
- Reduced tracked non-root README prose from 5,451 words to 894 words; retained detailed material in non-README references.
- No build was run because runtime code and data were unchanged.

## Remaining

- Continue the open town-documentation split as individual gameplay areas are verified; do not expand the town README during that work.
