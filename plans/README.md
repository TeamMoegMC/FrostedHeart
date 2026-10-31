# Implementation Plans

This flat directory holds verified investigations and intended implementation work. Plans are not evidence of current behavior; source and living documentation are authoritative.

For temperature work, start at [the current thermal backlog](2026-10-29_23-38-00_thermal-next-steps.md). Completed thermal iteration plans were consolidated into [the development history](../diary/2026-10-29_23-38-00_thermal-development-history.md); a few old entries remain for other modules' references.

Name files `YYYY-MM-DD_HH-mm-ss_<system-topic>.md`. Before implementation, recheck assumptions against source, data, docs, relevant discussions, and read-only design. Finish each plan as `completed`, `superseded`, or `abandoned`, recording its outcome and links to resulting documentation and diary entries.

Use this opening:

```markdown
# <Plan title>

- Time: `<YYYY-MM-DD HH:mm:ss UTC offset>`
- Authors: `<names; provider/model and role when applicable>`
- Status: `<draft | ready | in-progress | blocked | completed | superseded | abandoned>`
- Scope: `<affected game system or paths>`
- Related: `<design, docs, discussions, issues, or identifiers>`
```

Add only sections the work needs, such as `Goal`, `Verified Current State`, `Decisions`, `Steps`, `Validation`, `Documentation Impact`, `Open Questions`, and `Outcome`.
