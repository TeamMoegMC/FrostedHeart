# Generator analytic field design investigation

- Time: `2026-09-08 14:14:41 +08:00`
- Author: `Codex; OpenAI GPT-6; primary design agent`
- Status: `completed`
- Scope: `generator heating proposal and current analytic/Page behavior explanation`

## Completed

- Created the [generator analytic field proposal](../plans/2026-09-08_14-14-41_generator-analytic-heat-field.md).
- Verified historical sphere geometry, constant positive temperature delta, maximum overlap, configurable level formulas, and afterheat from Git history and current generator code.
- Verified current analytic composition, runtime startup coupling, face-driven Brick expansion, fixed Page overhead, and whole-resident-mask retention while a Page has a source/hot Brick.

## Decisions

- Recommend a small generator map and maximum-delta reduction before existing control fields, with unchanged-report coalescing and normal machine tick recovery.
- Recommend replacing generator environmental physical output to avoid duplicate heating and generator-driven mesh expansion. This remains a proposal, not an implemented contract.
- State the boundaries for unloaded generators and infrared explicitly. Mark the stale town fallback description in the plan for correction with the consumer documentation update.

## Validation

- Read source and historical files through `git show`/`git grep`; inspected current admission, residency, query, and generator lifecycle paths.
- No Java edits, JUnit, GameTest run, or performance measurement was performed for this design-only request.
- Documentation impact: added the plan and this diary; living behavior documents remain unchanged because the proposed behavior is not implemented.

## Remaining

- Implement and validate the proposed model after design review; the plan defines real production scenarios and the performance evidence still needed.
