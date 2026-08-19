# How should we split the transitional town documentation?

- Time: `2026-08-19 14:14:17 +0800`
- Author: `Codex; OpenAI GPT-5; documentation collaborator`
- Status: `open`
- Related: [town documentation index](../docs/town/README.md), [town model](../docs/town/town-model.md), [citizen simulation architecture](../docs/town/hybrid-simulation-architecture.md)

## Context

The town documentation contains a large amount of valuable investigation, mathematics, implementation reasoning, and experimental results. It is also difficult to use as living system documentation in its current form:

- `town-model.md` is more than 1,800 lines and combines current game mechanics, simulator interfaces, implementation stages, generated observations, balance experiments, and future work.
- `hybrid-simulation-architecture.md` began as a proposal and later accumulated corrections describing implemented citizen behavior.
- The earlier numerical-model document overlaps both of them and may use assumptions that were later changed.

These are observations about the documents, not a claim that their technical contents are wrong. Each fact still needs to be checked against current source and data during migration.

## Initial Proposal

Split the material by gameplay question rather than by Java package or implementation phase:

- `town-overview.md`: system boundaries, daily lifecycle, central concepts, and reading map.
- `residents-housing-and-care.md`: recruitment, identity, attributes, housing, food, nutrition, health, morale, aging, sleep, and death.
- `workforce-production-and-storage.md`: eligibility, assignment, productivity, mining, hunting, processing, warehouse settlement, and failure behavior.
- `town-heating-and-climate.md`: building temperature, climate inputs, heat fields, generator behavior, settlement timing, and current thermal limitations.
- `citizen-simulation-and-presence.md`: authoritative citizen state, movement, behavior, visibility, synchronization, persistence, and interaction.
- `management-and-observability.md`: Mayor's Seal controls, policies, statistics, events, history, and player-facing feedback.
- `town-modeling-and-balance.md`: shared mathematical definitions, simulator entry points, scenarios, output metrics, calibration baselines, and reproducibility.

This is deliberately a small set of substantial documents. Creating one file per class, building, or formula would recreate the codebase rather than explain the game system.

During migration:

- Verified implemented behavior would move into the relevant living document.
- Unimplemented stages and actionable redesigns would become timestamped files in `plans/`.
- Alternative models, balance ideas, and historical interpretations worth discussing would become timestamped files in `discussion/`.
- Experimental results would remain in living docs only when they define a currently relevant baseline and include enough provenance to reproduce them.
- The old documents would be removed or marked deprecated only after their useful content and incoming links were accounted for.

For searchability, filenames could use stable English ASCII keywords while the document body remains Chinese, English, or bilingual according to the contributors maintaining that system.

## Open Questions

1. Are these gameplay boundaries natural to people currently working on the town system, or should food/nutrition and citizen simulation be independent top-level systems?
2. Which numerical experiments represent maintained balance baselines, and which are only historical investigation?
3. Should the migration happen as one focused documentation task, or incrementally whenever the corresponding gameplay area is next changed?
4. Is a single language preferred for living documentation, or is keyword-searchable naming sufficient while prose follows the author and audience?
5. Which document should be rewritten first to provide the most useful entry point for a developer unfamiliar with the town system?

## Desired Outcome

The discussion should produce an agreed set of document boundaries and a first migration target. That result can then become an implementation plan; this post itself is not that plan.
