# Frosted Research

- Status: `Current`
- Last verified: `2026-08-26`
- Scope: Legacy research progression plus V2 results/access and the Phase 2 topic-free observation, hidden-Idea, executable-workflow, person-knowledge, and Knowledge Laboratory foundation
- Code anchors: [`FHResearch`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`ResearchCatalog`](../../src/main/java/com/teammoeg/frostedresearch/ResearchCatalog.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java)

## What Is This System?

Frosted Research is a server-authoritative, team-shared technology system. The existing configuration catalogue still owns the playable directed research graph. V2 adds a parallel datapack catalogue with five result definitions—Finding, Design, Construction, Procedure, and Prototype—plus independent team knowledge and one shared access projection. Its Phase 2 discovery path starts with topic-free records: the research notebook can observe any block and registered providers may enrich those records. On the evidence-board path, a topic does not surface until pinned evidence produces a candidate and the drawing-desk inspiration game finishes; a registered direct knowledge offer may instead create an Idea without pretending that the board matched. Registered idea, protocol, resolution, and Finding-view handlers execute content workflows without putting a geology branch in the drawing desk or Knowledge Laboratory. The bundled `the_winter_rescue:geology_understanding` topic is the first content plugin and coexists with the old geology research and `MinigameClue` path.

## What Is Authoritative?

Java source under `src/main/java/com/teammoeg/frostedresearch` is authoritative for behavior. Server configuration under `config/fhresearches/*.json` remains authoritative for the legacy playable catalogue. Effective datapack resources under `data/*/frostedresearch/topics` and `data/*/frostedresearch/prototypes` are authoritative for V2 result/profile declarations when present. Chorda team files own both legacy progress and V2 acquired result IDs. This documentation explains those sources but does not replace them.

## Where Next?

Read in this order:

1. [architecture.md](architecture.md) — mental model, ownership boundaries, package map, and lifecycle.
2. [definitions-and-codecs.md](definitions-and-codecs.md) — research JSON, IDs, graph semantics, clue/effect types, and authoring compatibility.
3. [results-and-access.md](results-and-access.md) — five result types, executable topic catalogue, projections, legacy provenance, prototype shell, and current V2 boundary.
4. [state-persistence-and-sync.md](state-persistence-and-sync.md) — team data structures, formulas, state transitions, files, and packets.
5. [gameplay-and-integrations.md](gameplay-and-integrations.md) — drawing desk, experiment sources, unlock enforcement, APIs, events, commands, and optional mods.
6. [research-ui.md](research-ui.md) — archive UI, graph layout, navigation, and client refresh behavior.
7. [known-risks.md](known-risks.md) — confirmed defects, explicit boundaries, and validation gaps.

Intended changes belong in `plans/` and open design choices in `discussion/`; neither is evidence of current behavior.

## Minimum Contribution Step

Before changing this system, identify the owning document above, preserve stable research/clue/effect identifiers or declare explicit `legacyIds`, run `./gradlew test --tests "com.teammoeg.frostedresearch.*"`, validate the production catalogue with `./gradlew validateResearchCatalog -PresearchCatalogDir=<path>`, and update the document plus `diary/` if any documented contract changes.
