# Frosted Research

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: Implemented research definitions, team state, progression, persistence, synchronization, gameplay surfaces, unlock effects, integrations, and known implementation limits
- Code anchors: [`FRMain`](../../src/main/java/com/teammoeg/frostedresearch/FRMain.java), [`FHResearch`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`Research`](../../src/main/java/com/teammoeg/frostedresearch/research/Research.java), [`ResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/ResearchData.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java), [`DrawDeskScreen`](../../src/main/java/com/teammoeg/frostedresearch/gui/drawdesk/DrawDeskScreen.java), [`ResearchArchiveLayer`](../../src/main/java/com/teammoeg/frostedresearch/gui/archive/ResearchArchiveLayer.java)

## What Is This System?

Frosted Research is a server-authoritative, team-shared technology system. Configuration files define a directed research graph, clues, costs, experiment-point targets, and completion effects. Runtime team data records activated and completed projects, insight, clue progress, effect claims, and cross-system attributes. The drawing desk is the main player surface; hooks and mixins enforce unlocked recipes, blocks, multiblocks, and JEI categories elsewhere.

## What Is Authoritative?

Java source under `src/main/java/com/teammoeg/frostedresearch` is authoritative for behavior. Server configuration under `config/fhresearches/*.json` is authoritative for the loaded research catalogue; in the companion pack, localized research text is primarily under `kubejs/assets/twr_researches/lang/`. Chorda team files are authoritative for saved progress. This documentation explains those sources but does not replace them.

## Where Next?

Read in this order:

1. [architecture.md](architecture.md) — mental model, ownership boundaries, package map, and lifecycle.
2. [definitions-and-codecs.md](definitions-and-codecs.md) — research JSON, IDs, graph semantics, clue/effect types, and authoring compatibility.
3. [state-persistence-and-sync.md](state-persistence-and-sync.md) — team data structures, formulas, state transitions, files, and packets.
4. [gameplay-and-integrations.md](gameplay-and-integrations.md) — drawing desk, experiment sources, unlock enforcement, APIs, events, commands, and optional mods.
5. [research-ui.md](research-ui.md) — archive UI, graph layout, navigation, and client refresh behavior.
6. [known-risks.md](known-risks.md) — confirmed defects, fragile contracts, and validation gaps.

Intended changes belong in `plans/` and open design choices in `discussion/`; neither is evidence of current behavior.

## Minimum Contribution Step

Before changing this system, identify the owning document above, preserve stable research/clue/effect identifiers and definition order, run `./gradlew test --tests "com.teammoeg.frostedresearch.*"`, and update the document plus `diary/` if any documented contract changes.
