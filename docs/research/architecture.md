# Research System Architecture

- Status: `Current`
- Last verified: `2026-08-25`
- Scope: Runtime ownership, legacy progression, V2 Phase 1 result/access foundation, lifecycle, and the shortest path through the implementation
- Code anchors: [`FRSpecialDataTypes`](../../src/main/java/com/teammoeg/frostedresearch/FRSpecialDataTypes.java), [`FHResearch`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamResearchData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamResearchData.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java)

## One-Sentence Model

A server retains the legacy `Research` graph and its `TeamResearchData`, while V2 Phase 1 independently compiles five typed datapack results into a revisioned snapshot and stores four acquireable result-ID sets in `TeamKnowledgeData`; `TechnologyAccessResolver` combines both authorities into the one default-open projection consumed by gameplay and JEI.

## Ownership Layers

```mermaid
flowchart LR
    Config["config/fhresearches/*.json\nexternal research catalogue"] --> Validation["ResearchCatalog\nwhole-catalogue validation"]
    Validation --> Registry["FHResearch + FHRegistry\nstring IDs; legacy slots only"]
    Registry --> Runtime["Research / Clue / Effect\nimmutable-ish definitions"]
    Runtime --> Team["TeamResearchData\nteam authority"]
    Team --> One["ResearchData\nper-project state"]
    Team --> Derived["unlock lists\nderived cross-system caches"]
    Team --> Consumers["variants\npersisted cross-system attributes"]
    Hooks["events, blocks, recipes, commands"] --> Team
    Team --> Packets["full and incremental S2C packets"]
    Registry --> Packets
    Packets --> Client["Chorda client team mirror\narchive and drawing-desk UI"]
```

There are three different kinds of data. Keeping them separate is the most important architectural rule:

| Layer | Owner | Examples | Persistence |
|---|---|---|---|
| Definition | `FHResearch.researches` | parents, clues, effects, costs, flags, migration aliases | JSON in server `config/fhresearches`; old name-to-slot snapshot remains only for legacy active-ID migration |
| Authoritative progress | team `TeamResearchData` | string-keyed active project, long committed points, nonce-keyed clues/effects, insight, variants | versioned Chorda team NBT |
| Derived/cache/client state | unlock lists, graph layout, `ResearchWorkspaceState` | locked recipe sets, UI selection, camera, search, bookmarks | rebuilt or transient; not durable team truth |
| V2 result definitions | `ResearchResultCatalog.Snapshot` | topic ownership, five typed results, prototype profile revision, managed targets | effective datapack JSON; immutable in memory with monotonic revision |
| V2 team authority | team `TeamKnowledgeData` | acquired Finding, Design, Construction, and Procedure IDs, including orphans | schema-1 Chorda team NBT |
| V2 projection | `KnowledgeProjection`, `TechnologyAccessProjection` | active finding views and access decisions with provenance | rebuilt and sent in a full S2C snapshot; not durable truth |
| Prototype fact | `upgrade_prototype` ItemStack | profile, frozen revision, serial, owner team | namespaced item NBT; never an acquired team ID |

The client must not invent a second progression model. It reads a synchronized Chorda team mirror through `ClientResearchDataAPI` and sends intent packets for server validation.

## Module Bootstrapping

`FRMain` is the Forge mod entry point for mod ID `frostedresearch`. It registers:

- `FRContents`: drawing desk content, research items, recipe serializers, menu, sounds, and Create-backed calculator content;
- `FRSpecialDataTypes.RESEARCH_DATA`: Chorda's `SpecialDataType<TeamResearchData>` with local ID `research`;
- `FRSpecialDataTypes.KNOWLEDGE_DATA`: independent `SpecialDataType<TeamKnowledgeData>` with ID `frostedresearch:knowledge`;
- `frostedresearch:upgrade_prototype`: non-stackable physical shell, intentionally absent from the normal creative tab;
- server configuration and common/client event handlers;
- the `FRNetwork` message channel during common setup;
- compatibility modules selected by loaded-mod checks.

Important registry IDs include:

| Kind | ID |
|---|---|
| block and block entity | `frostedresearch:drawing_desk` |
| menu | `frostedresearch:draw_desk` |
| Create block and block entity | `frostedresearch:mechanical_calculator` |
| recipe types and serializers | `frostedresearch:paper`, `frostedresearch:inspire` |
| creative tab | `frostedresearch:main` |

## Package Map

| Package | Responsibility | Start with |
|---|---|---|
| `research` | transactional catalogue loading, string identity, definition graph, lock lists | `ResearchCatalog`, `FHResearch`, `Research` |
| `research.clues` | polymorphic prerequisites and contribution triggers | `Clue`, `ListenerClue`, concrete clue classes |
| `research.effects` | polymorphic rewards and derived unlock state | `Effect`, concrete effect classes |
| `data` | team/project state, formulas, persistence codecs, public enum tokens | `TeamResearchData`, `ResearchData`, `ClueData`, `ResearchVariant` |
| `knowledge` | V2 result codecs, minimal datapack catalogue, projections, provenance and resolver | `ResearchResult`, `ResearchResultCatalogLoader`, `TechnologyAccessResolver` |
| `network` | definition, full-state, delta-state, and player-intent messages | `FRNetwork`, packet classes |
| `handler` and `events` | Forge lifecycle, trigger routing, locks, reload and team events | `FHServerEvents`, `ResearchCommonEvents`, `ResearchHooks` |
| `blocks`, `item`, `recipe` | drawing desk, calculator, tools, paper/inspiration recipes | `DrawingDeskTileEntity`, `MechanicalCalculatorTileEntity`, `RubbingTool` |
| `gui` | drawing-desk screen, archive, graph, project workspace | `DrawDeskScreen`, `ResearchArchiveLayer` |
| `api` | team-data lookup, mutation service and client/server helper boundary | `ResearchDataAPI`, `KnowledgeDataAPI`, `TeamResearchService` |
| `compat` | JEI, FTB Quests/Teams, Tetra, Create and IE integration | each compat package |
| `number` | unfinished/unused number-resource abstraction | do not assume it drives current progression |

## Server Lifecycle

```mermaid
sequenceDiagram
    participant Forge as Forge lifecycle
    participant Catalog as FHResearch
    participant Chorda as Chorda team storage
    participant Team as TeamResearchData
    participant Client as Player client

    Forge->>Catalog: serverAboutToStart / load()
    Catalog->>Catalog: read fhregistries.dat for legacy active migration
    Catalog->>Catalog: sort, parse, and validate all config/fhresearches/*.json
    Catalog->>Catalog: atomically install and reindex valid definitions
    Chorda->>Team: decode team NBT
    Forge->>Team: TeamLoadedEvent / initResearch()
    Team->>Team: replay granted effects and restart active clues
    Forge->>Client: login sync definitions in registry order
    Forge->>Client: then full TeamResearchData mirror
    Forge->>Catalog: data-pack reload invokes ResearchHooks.ServerReload
    Catalog->>Catalog: reject invalid candidate without mutating live state
    Catalog->>Team: migrate aliases, rebuild listeners and derived unlocks
    Catalog->>Client: staged definitions, then string-keyed full team data
    Forge->>Catalog: level save / save registry and editor state
    Chorda->>Team: save team NBT
```

Legacy research JSON is deliberately outside the vanilla datapack resource tree. In parallel, V2 Phase 1 now reads `data/<namespace>/frostedresearch/topics/<path>.json` and `.../prototypes/<path>.json`; an empty V2 directory is valid. A V2 candidate is installed only after aggregate validation, and an invalid candidate retains the previous result snapshot. This does not change the companion pack's role as the sole production source of the current playable legacy catalogue, and Phase 1 ships no formal topic content.

## Runtime Control Flow

The common player flow is:

1. A player opens a team-owned drawing desk.
2. The client selects a definition from the synchronized catalogue.
3. `FHResearchControlPacket(COMMIT_ITEM)` asks the server to pay required items and insight levels and activate the project.
4. `TeamResearchData` may hold several activated or paused `ResearchData` records, but `activeResearchId` selects at most one current project for listener/item/game routing.
5. Direct experiment points and completed clues update the project.
6. `TeamResearchData#checkResearchComplete` is the only normal completion decision point.
7. Completion grants team-safe effects immediately; effects requiring a concrete player remain claimable.
8. Rebuilt unlock lists and persisted variants are consumed by crafting, block, multiblock, generator, forecast, villager, and UI systems.

`ResearchData.active` means a project has been paid for and is eligible to resume; it is not the same as being the one selected by `TeamResearchData.activeResearchId`.

## Authority And Trust Boundaries

- The server owns definitions after load, team progress, costs, clue completion, experiment points, effects, and commands.
- The client owns archive presentation state only. `ResearchWorkspaceState` is scoped to one screen instance and is not persisted across reopening.
- Definitions and console-command effects are trusted administrator/modpack configuration, not untrusted player input.
- Player C2S packets are untrusted input. Research-control paths re-check game rules; drawing-desk operations additionally bind the packet to the sender's open menu, loaded tile, level, distance, team owner, operation shape, and `9 x 9` board bounds.
- Research data belongs to a Chorda team, not a player capability. With FTB Teams present, membership selects the team holder; otherwise Chorda supplies a distinct single-player fallback team per player.
- Every restricted execution resolves an owner before authorization. Player actions use the real player's current team. Machine actions use the block entity's persisted owner. Fake players and ownerless machines fail closed for restricted content, while unrestricted content remains executable.
- Create mechanical crafting scopes its owner in a nested, exception-safe `ThreadLocal` around the actual recipe lookup. The owner cannot leak between neighbouring machines, nested calls, exceptions, or server threads.

## Extension Boundaries

Supported extension points are narrower than the package structure may suggest:

- add clue/effect codecs through their typed registries before definitions load;
- read/write team state through `ResearchDataAPI` and listen to research events;
- declare new research JSON in server configuration and translations in resource assets;
- add enforcement for a new game system by using global lock lists plus team unlock lists, or by consuming a variant.

There is still no datapack replacement for the playable legacy research graph or general-purpose `Requirement` hierarchy. The implemented V2 loader compiles only `format`, `presentation`, `results`, ordinary rewards, and prototype identity/revision; future workflow fields are tolerated but have no Phase 1 meaning. See [results-and-access.md](results-and-access.md) for the exact boundary.
