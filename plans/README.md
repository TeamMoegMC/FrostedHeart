# Implementation Plans

This is a flat working area for investigations and implementation plans shared by humans and agents. A plan captures verified starting facts, intended changes, decisions, steps, and validation needed for the next development stage. It may describe behavior that does not exist yet and is therefore not living documentation.

## Conventions

1. Keep all plan files directly in this directory; do not create subdirectories.
2. Use a local timestamp and searchable system/topic keywords: `YYYY-MM-DD_HH-mm-ss_<system-topic>.md`.
3. Identify the authors, creation time, scope, and status.
4. Before implementation, recheck the plan's assumptions against current source, data, documentation, relevant discussions, and read-only human design.
5. Update the plan when implementation materially changes its decisions or remaining steps.
6. On completion, record the outcome and link the resulting documentation and diary entry. Mark unused plans `superseded` or `abandoned` rather than leaving their authority ambiguous.
7. Completed plans remain useful reasoning records but are never evidence of current behavior by themselves.
8. Never include credentials, tokens, private user data, or other secrets.

Suggested opening:

```markdown
# <Plan title>

- Time: `<YYYY-MM-DD HH:mm:ss UTC offset>`
- Authors: `<names; provider/model and role when applicable>`
- Status: `<draft | ready | in-progress | blocked | completed | superseded | abandoned>`
- Scope: `<affected game system or paths>`
- Related: `<design, docs, discussions, issues, or identifiers>`
```

Useful sections include `Goal`, `Non-goals`, `Verified Current State`, `Decisions`, `Implementation Steps`, `Compatibility`, `Validation`, `Documentation Impact`, `Open Questions`, and `Outcome`. Use only the sections the work needs; this directory should remain easy to contribute to.
