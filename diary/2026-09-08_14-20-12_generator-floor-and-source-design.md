# Generator floor and source design clarification

- Time: `2026-09-08 14:20:12 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `revised generator heating proposal following explicit user clarification`

## Completed

- Revised the [generator plan](../plans/2026-09-08_14-14-41_generator-analytic-heat-field.md) to retain the existing physical source and add an analytic floor.
- Superseded the replacement recommendation in the [earlier design investigation](2026-09-08_14-14-41_generator-analytic-field-design.md); no replacement was implemented.

## Decisions

- The user requires both mechanisms. Compose `max(physical temperature, local natural temperature + maximum matching generator delta)` before existing explicit control fields.
- Preserve source power, exhaust connectivity, normal machine publication, and solver state. The floor is query-only and cannot seed a mesh or feed composed temperatures back into it.
- Record that additional visible enclosure heating starts only above the floor, and that useful source power/connectivity must be established with real production scenarios.

## Validation

- Rechecked current player/passive/town composition paths: player/passive already receive natural temperature; covered town mesh hits need a local natural query to compute the floor.
- No Java changes or test execution. The plan includes real T1/T2 GameTests and source/enclosure performance checks; no JUnit is planned.
- Documentation impact: revised the intended-work plan and appended this diary. Living docs are unchanged because behavior has not changed.

## Remaining

- Implement the combined model and perform the specified production validation. Infrared currently shows physical source heating, not the analytic floor.

## Further Clarification: 2026-09-08 14:26:24 +08:00

- The user clarified that the floor protects regional gameplay heating, not only player characters. The normal generator source works outdoors and indoors; the floor prevents outdoor dissipation from defeating the intended game effect.
- Updated the plan to explicitly include character, crop, and town temperature consumers and their actual decisions in production validation. Superseded the intermediate character-only interpretation expressed in commentary; no code implemented that interpretation.
- Corrected planned living-document links to the existing filenames. This remains design work without Java changes or test execution.
