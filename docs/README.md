# Frosted Heart System Documentation

This directory is the maintained explanation of how Frosted Heart's implemented game systems work. It is written for humans and agents who need a reliable mental model before changing a system.

Source code, configuration, datapack content, and companion-pack data are the final authority. A document that has not been verified against them must say so visibly.

## System Catalog

| Game system | Start here | Coverage |
|---|---|---|
| Town management | [town/README.md](town/README.md) | Production, residents, housing, nutrition, heating, simulation, and management observability. Existing documents predate this documentation contract and are being incrementally separated. |
| Curiosity encounter | [boss/README.md](boss/README.md) | Encounter state machine, temperature interaction, arena behavior, persistence, and integration. The current document is a transitional design/implementation reference. |
| Nutrition | [nutrition/README.md](nutrition/README.md) | Player and town nutrition mechanics plus historical balance analysis. The imported conversation still needs separation into current mechanics and discussion. |

Only add a system directory when there is useful documentation to place in it. Use player-facing systems such as `town`, `climate`, `energy`, `research`, or `encounters`; do not mirror Java package structure.

Documents that have been replaced but still contain useful historical context belong in [`deprecated/`](deprecated/). They are excluded from the primary system reading path and must not be treated as current behavior.

## What Belongs Here

Living documentation should explain current implemented behavior:

- the player-facing purpose and system boundaries;
- important concepts, terms, and invariants;
- state ownership, persistence, synchronization, and lifecycle;
- formulas, units, defaults, configuration, and data inputs;
- interactions with other game systems;
- exact code and data anchors needed to investigate further;
- known limitations or intentionally missing behavior.

Unimplemented proposals belong in [`plans/`](../plans/) or [`discussion/`](../discussion/). Completed work history belongs in [`diary/`](../diary/). Human creative intent under [`design/`](../design/) is read-only for agents.

## Document Header

New or substantially revised documents should begin with a compact status block:

```markdown
> Status: Current
> Last verified: YYYY-MM-DD
> Scope: <gameplay boundary>
> Code anchors: `ClassName`, `methodName`, `registry:id`
> Related: [design](../../design/...), [plan](../../plans/...), [discussion](../../discussion/...)
```

Use `Transitional`, `Partial`, or `Deprecated` instead of `Current` when appropriate. Status describes documentation reliability, not whether the feature itself is finished.

## Writing Rules

1. Prefer plain explanations and diagrams of state or data flow over inventories of classes.
2. Use exact class, method, registry, config, command, and data identifiers so readers can search the repository.
3. Define every formula symbol and give values a unit, range, provenance, and distinction between source default and runtime configuration.
4. Link to paths and symbols rather than line numbers, which become stale quickly.
5. Separate verified current behavior from limitations and future work. Link to a plan or discussion instead of embedding a speculative roadmap.
6. Avoid duplicate references. Consolidate useful facts, then move an obsolete document to `docs/deprecated/` only when its historical reasoning remains useful. Git and the diary preserve everything else.
7. Keep figures beside their owning system when adding new assets. `docs/figures/` is retained for existing town-model figures during the transition.

## Maintenance Workflow

Before changing a system, read its system README, relevant living documents, human design sources, active plans, and recent diary entries. After changing behavior, update the affected documentation in the same work and record the documentation impact in the diary.

Documentation normally needs an update when work changes player-visible mechanics, formulas or defaults, state transitions, persistence, networking contracts, configuration, data formats, commands, or cross-system behavior. Pure refactors that preserve these contracts may record `Documentation impact: none` in the diary.
