# Frosted Research

- Status: `Current`
- Last verified: `2026-09-08` (knowledge integration verified against source)
- Scope: Legacy research progression, result catalogue, technology access projections, physical prototype shell, and the
  integration with the independent knowledge system
- Code anchors: [`FHResearch`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`ResearchCatalog`](../../src/main/java/com/teammoeg/frostedresearch/ResearchCatalog.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java)

## What Is This System?

Frosted Research contains the existing team research progression and an
independent [knowledge system](../knowledge/README.md). The configuration catalogue still owns the playable research
graph. Knowledge now provides observations, ideas, results, the shared inbox/archive, linking, notes, and hints; its
active results feed the existing technology access projection. These research documents own the legacy project workflow
and its integration points.

## What Is Authoritative?

Java source under `src/main/java/com/teammoeg/frostedresearch` is authoritative for behavior. Server configuration under
`config/fhresearches/*.json` remains authoritative for the legacy playable catalogue. Effective datapack resources under
`data/*/frostedresearch/topics` and `data/*/frostedresearch/prototypes` are authoritative for V2 result/profile
declarations when present. Chorda team files own both legacy progress and the separate knowledge archive, inbox, and
history. The new knowledge definition paths and state contracts are documented
under [knowledge](../knowledge/README.md). This documentation explains those sources but does not replace them.

## Where Next?

Read in this order:

1. [architecture.md](architecture.md) — mental model, ownership boundaries, package map, and lifecycle.
2. [definitions-and-codecs.md](definitions-and-codecs.md) — research JSON, IDs, graph semantics, clue/effect types, and authoring compatibility.
3. [results-and-access.md](results-and-access.md) — five result types, the compatibility topic catalogue, projections,
   legacy provenance, and physical prototype shell.
4. [state-persistence-and-sync.md](state-persistence-and-sync.md) — team data structures, formulas, state transitions, files, and packets.
5. [gameplay-and-integrations.md](gameplay-and-integrations.md) — drawing desk, experiment sources, unlock enforcement, APIs, events, commands, and optional mods.
6. [research-ui.md](research-ui.md) — archive UI, graph layout, navigation, and client refresh behavior.
7. [known-risks.md](known-risks.md) — confirmed defects, explicit boundaries, and validation gaps.

For observations, ideas, knowledge results, inbox learning, notes, links, and hints, start
at [Knowledge](../knowledge/README.md). Intended changes belong in `plans/` and open design choices in `discussion/`;
neither is evidence of current behavior.

## Minimum Contribution Step

Before changing this system, identify the owning document above, preserve stable research/clue/effect identifiers or declare explicit `legacyIds`, run `./gradlew test --tests "com.teammoeg.frostedresearch.*"`, validate the production catalogue with `./gradlew validateResearchCatalog -PresearchCatalogDir=<path>`, and update the document plus `diary/` if any documented contract changes.
