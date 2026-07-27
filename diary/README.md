# Development Diary

This directory is the shared development log for human contributors and agents.

## Rules

1. Before starting development, read the latest entries relevant to the target system.
2. After completing development work, add one concise Markdown entry. Do not silently skip the diary when the code change is small.
3. Name entries with a local timestamp and a short mnemonic suffix: `YYYY-MM-DD_HH-mm-ss_<mnemonic-topic>.md`.
   - Make the suffix specific and searchable so `ls diary` quickly reveals relevant history.
   - Prefer lowercase kebab-case, for example `2026-07-27_00-25-32_town-warehouse-interface.md`.
   - Avoid generic suffixes such as `changes`, `work`, or `update`.
4. State who you are, including agent or contributor name, provider/model when applicable, and team role.
5. Record facts that prevent repeated work: what changed, key decisions and reasons, validation performed, known limitations, and the next useful step.
6. Keep entries concise. Link to repository paths instead of copying large code snippets or logs.
7. Never include credentials, tokens, private user data, or other secrets.
8. Do not rewrite another contributor's entry except to correct a clearly identified factual error. Add a newer entry when plans or conclusions change.

## Shared Entry Format

```markdown
# <Task or feature>

- Time: `<YYYY-MM-DD HH:mm:ss UTC offset>`
- Author: `<name; provider/model if applicable; team role>`
- Status: `<completed | partial | blocked>`
- Scope: `<affected subsystem or paths>`

## Completed

- <implemented or investigated result>

## Decisions

- <decision and brief reason>

## Validation

- <command, test, or manual result>

## Remaining

- <known limitation, risk, or next step; write "None" when finished>
```
