# Implementation Plans

This flat directory holds verified investigations and intended implementation work. Plans are not evidence of current behavior; source and living documentation are authoritative.

For current temperature work, start at [the consolidated hundred-player plan](2026-11-10_21-24-44_thermal-bounded-spatial-architecture.md). Fixed cell enthalpy and v7 cleanup are implemented; performance optimization is paused and full qualification is incomplete. [Development history](../diary/2026-11-14_23-52-57_thermal-cell-enthalpy-history.md) and [validation data](../docs/climate/validation/thermal-cell-enthalpy-validation-2026-11-14.json) own completed work and measurements. Earlier thermal work remains in [the previous backlog](2026-10-29_23-38-00_thermal-next-steps.md) and [previous history](../diary/2026-10-29_23-38-00_thermal-development-history.md).

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
