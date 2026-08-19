# Frosted Heart - Codex Instructions

## Shared Project Memory
Read these files for project context (maintained by Codex, shared across the team):
- [.Codex/memory/project-structure.md](.Codex/memory/project-structure.md) - Module layout and package maps
- [.Codex/memory/architecture.md](.Codex/memory/architecture.md) - Patterns, build commands, conventions

## Project Knowledge

Project knowledge is separated by purpose and authority:

| Location | Purpose | Maintenance rule |
|---|---|---|
| [design/](design/) | Human-authored lore, world design, and creative principles | **Read-only for agents. Never edit, add, delete, rename, move, or reformat anything under `design/`.** |
| [docs/](docs/) | Living explanations of how implemented game systems currently work | Maintain when behavior, models, lifecycle, persistence, configuration, or cross-system contracts change. |
| [plans/](plans/) | Flat working area for implementation investigations and next-step plans | May describe intended behavior. Keep status and outcome explicit; plans are not current system truth. |
| [discussion/](discussion/) | Flat, open forum for ideas, questions, alternatives, and balancing discussion | Non-authoritative. Append responses or create a related file instead of rewriting another contributor's words. |
| [diary/](diary/) | Chronological record of completed development and validation | Append a new entry after development; do not rewrite history. |

Source code and data remain the final authority for implemented behavior. When they disagree with `docs/`, verify the behavior and correct or clearly mark the documentation.

Never place credentials, tokens, private user data, or other secrets in project knowledge.

## README Contract

Every README should answer only four questions:

1. What is this directory or project?
2. What is authoritative here?
3. Where should the reader go next?
4. What is the minimum action needed to use or contribute to it?

Keep answers local. Link to the file that owns a rule or detail instead of repeating it. Put current behavior in `docs/`, intended work in `plans/`, open ideas in `discussion/`, and completed history in `diary/`; a README is an entry point, not the full reference.

## Development Documentation

- Start at [docs/README.md](docs/README.md), then read the README and relevant documents for the game system being changed.
- Organize living documentation by player-facing game system, not Java package, code type, or contributor.
- Treat `docs/deprecated/` as historical reference only. Do not use it as evidence of current behavior, update deprecated documents as if they were living references, or include them in a system's primary reading path.
- Keep exact searchable anchors in documentation: classes, methods, registry IDs, configuration keys, commands, data files, formulas, units, and defaults.
- Begin new or substantially revised system documents with status, last-verified date, scope, and code anchors. Status describes document reliability, not feature completeness.
- Define formula symbols, units, ranges, and whether values are source defaults or runtime configuration. Link paths and symbols rather than unstable line numbers.
- Put unsettled ideas in `discussion/` and actionable pre-implementation work in `plans/`. Do not present either as implemented behavior.
- When a change affects documented behavior, update the relevant living document in the same development work. Record the documentation impact in the diary entry, including when no update was needed.
- When introducing a major game system, add or update its entry in `docs/README.md` and provide a system README when more than one document exists.

## Plans And Discussions

- Read [plans/README.md](plans/README.md) and [discussion/README.md](discussion/README.md) before creating files there.
- Keep both directories flat. Name entries with a local timestamp and specific searchable keywords: `YYYY-MM-DD_HH-mm-ss_<system-topic>.md`.
- Identify the author and creation time. Plans must also state their status and be updated with an outcome when completed, superseded, or abandoned.
- Before implementing an existing plan, verify that its assumptions still match current source, data, documentation, and human design.

## Development Diary
- Read [diary/README.md](diary/README.md) and the latest relevant diary entries before starting development.
- After completing development work, create a new timestamp-prefixed, mnemonic-suffixed Markdown entry in `diary/` using the shared format.
- Identify yourself in every entry and record decisions, validation, and remaining work so future humans and agents do not repeat completed investigation.

## Related repository

The companion modpack repository is attached as a secondary project folder.

Before changing KubeJS scripts, recipes, datapacks, quests, or pack configuration:
- locate and read the companion repository's AGENTS.md;
- inspect both repositories for related identifiers and registrations;
- validate and report changes for each Git repository separately.
