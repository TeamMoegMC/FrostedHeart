# Deprecated documentation area

- Time: `2026-08-19 14:21:36 +0800`
- Author: `Codex; OpenAI GPT-5; primary documentation agent`
- Status: `completed`
- Scope: `AGENTS.md; docs/README.md; docs/deprecated/; docs/town/README.md`

## Completed

- Added `docs/deprecated/` for obsolete documents that retain useful historical reasoning but must not be treated as current system truth.
- Moved `TWR城镇数值模型设计.md` from the active town documentation into the deprecated area and added a visible warning with links to current town references.
- Removed the document from the town system's primary reading map while preserving a clearly labeled historical link.

## Decisions

- Deprecated documents remain discoverable but are excluded from normal system reading paths and are not maintained alongside current implementation.
- `plans/` and `discussion/` remain the destinations for future work and open ideas; `docs/deprecated/` is only for superseded reference material.
- Documents with no continuing reference value should rely on Git and diary history rather than accumulating in the deprecated directory.

## Validation

- Verified the old town path no longer exists and the document is present under `docs/deprecated/`.
- Verified all local Markdown links in the changed navigation and deprecated documents resolve using UTF-8 path handling.
- `git diff --check` reported no whitespace errors.

## Remaining

- None.
