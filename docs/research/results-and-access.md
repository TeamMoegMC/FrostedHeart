# Research Results And Technology Access

- Status: `Current`
- Last verified: `2026-08-26`
- Scope: Generative Research V2 Phase 2 topic-free observations, executable workflows, results, knowledge projection, team authority, legacy provenance, prototype shell, commands, reload, and explicit non-goals
- Code anchors: [`ResearchResult`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResult.java), [`ResearchResultCatalogLoader`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalogLoader.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`TeamResearchService`](../../src/main/java/com/teammoeg/frostedresearch/api/TeamResearchService.java), [`UpgradePrototypeItem`](../../src/main/java/com/teammoeg/frostedresearch/item/UpgradePrototypeItem.java)

## Implemented Boundary

Phase 1 implemented the mergeable result/access foundation alongside the current `Research`/`Effect` system. Phase 2 adds topic-free observations, a generic evidence board, hidden Idea matching, executable protocol/resolution handlers, work reports, Finding views, and the formal `the_winter_rescue:geology_understanding` content plugin. It still does not implement the research-institute queue, experiment apparatus, resident calculation work orders, or installed upgrades. Existing `config/fhresearches/*.json`, including legacy `geology_understanding`, remain unchanged and coexist.

Five result definitions exist:

| Type | Stable payload | Team acquisition | Direct consumer |
|---|---|---:|---|
| `finding` | `id`, optional `views` | yes | `KnowledgeProjection` only |
| `design` | `id`, nonempty `recipes` | yes | recipe access |
| `construction` | `id`, nonempty `multiblocks` | yes | multiblock formation access |
| `procedure` | `id`, nonempty `usable_blocks` | yes | right-click block access |
| `prototype` | `id`, `profile` | no | fabricates a physical `upgrade_prototype` ItemStack |

Construction and Procedure are intentionally separate. Construction cannot declare `usable_blocks`; Procedure cannot declare `multiblocks`. Procedure currently means exactly the `RightClickBlock` permission already enforced by `ResearchHooks#canUseBlock`; it does not claim placement, breaking, capability, automation, maintenance, or numerical behavior.

All recipe, multiblock, block, item, result, topic, and profile identities remain raw `ResourceLocation` values in definitions and projections. Decoding does not replace them with runtime objects.

## Minimal Datapack Catalogue

The server reads effective resources from:

```text
data/<namespace>/frostedresearch/topics/<path>.json
data/<namespace>/frostedresearch/prototypes/<path>.json
```

A topic currently compiles:

- required `format: 3`;
- optional `presentation.icon`;
- a list of the five typed `results`;
- ordinary item `rewards`;
- Phase 2 `legacy`, `idea_sources`, `inspiration`, `protocols`, and `resolution` workflow declarations.

Unknown future fields may remain in the JSON but have no runtime meaning yet. A prototype declaration currently requires `format: 1` and a positive integer `revision`; additional host/BOM/contribution fields are deferred to Phase 5.

Catalogue validation aggregates diagnostics for wrong formats/revisions, duplicate global result IDs, empty or duplicate target lists, cross-type Construction/Procedure fields, missing recipes, multiblocks, blocks, reward items, prototype profiles, workflow providers/resolvers/outcomes/tags/results, and unregistered Finding views.

`ResearchResultCatalog.Snapshot` is immutable. Every valid install receives a monotonically increasing `catalogRevision` and derives three managed target universes. Empty directories install a valid empty snapshot. An invalid reload logs all diagnostics and leaves the previous snapshot installed.

The mod datapack bundles the first formal topic at `data/the_winter_rescue/frostedresearch/topics/geology_understanding.json` plus stable recipe IDs `the_winter_rescue:research/copper_pro_pick` and `the_winter_rescue:research/research_notebook`. The companion repository mirrors the same topic and recipes so full-pack distribution remains compatible.

## Team Authority

`FRSpecialDataTypes.KNOWLEDGE_DATA` registers the independent Chorda ID `frostedresearch:knowledge`. `TeamKnowledgeData` schema `2` persists observation records, ideas, comparison artifacts, and:

```text
acquiredFindingIds
acquiredDesignIds
acquiredConstructionIds
acquiredProcedureIds
```

Acquisition is set-like and idempotent. A result removed from the current catalogue remains in its saved set as orphan history but produces no projection entry. There is no `acquiredPrototypeIds` field.

Ideas have an equivalent recoverable boundary. `TeamResearchService#recordIdea` marks an offer whose topic is absent, or whose declared Idea ID no longer resolves, as `IdeaRecord.State.ORPHAN` instead of discarding it. Orphans do not compile actions or resolve results. If the definition returns, recording the same stable topic/Idea again merges its sources/evidence and restores its non-orphan state; reload alone does not fabricate a new source or silently advance it.

`KnowledgeDataAPI` resolves server or client team data. Mutations are coordinated by `TeamResearchService`; `TeamResearchManager` is now only a compatibility facade.

## Projection And Default-Open Rule

`KnowledgeProjection` contains acquired, currently resolvable Finding IDs and view provenance plus safe Phase 2 observation, Idea, comparison, and action summaries. Observation summaries carry kind, subject, visible block state, dimension/position, public facets, channels, last-observed time, and Finding-produced annotation IDs. They do not carry observer history, semantic keys, or sealed facts. A Finding never changes a technology target; `frostedheart:prospecting_report_detail` projects only the coarse annotations `frostedheart:ore_trace_present` or `frostedheart:ore_trace_absent` for rock samples.

Loading a topic does not itself project its name, target results, or action list. Before an Idea exists, the client sees its ordinary record inbox and desk pins; candidate matching runs server-side. After the inspiration game completes, the desk exposes up to three candidates, and after the player records one, the projection may include its Idea summary and up to three handler-compiled action cards. Formal Finding views enter the projection only after acquisition.

`TechnologyAccessProjection` contains three independent managed sets and three target-to-source maps:

```text
Design       -> managed/unlocked recipe IDs
Construction -> managed/formable multiblock IDs
Procedure    -> managed/usable block IDs
```

For each target:

```text
managed = declared by the V2 snapshot or a legacy lock effect
allowed = !managed || sources is not empty
```

`AccessDecision` returns all three values: `managed`, `allowed`, and `sources`. An ordinary target absent from both systems therefore retains the historical default-open behavior.

The public Boolean adapters are `TechnologyAccessResolver#hasFinding`, `#isRecipeUnlocked`, `#canFormMultiblock`, and `#canUseBlock`. `ResearchHooks`, vanilla/campfire/Create/IE recipe call sites, IE formation, right-click block enforcement, generator formation checks, and JEI consume this same compiled answer.

## Legacy Coexistence And Provenance

Legacy effects remain unchanged and are projected only into their matching channel:

| Legacy authority | Projection channel |
|---|---|
| `EffectCrafting` | Design-like recipe entitlement |
| `EffectBuilding` | Construction-like multiblock entitlement |
| `EffectUse` | Procedure-like usable-block entitlement |

Legacy entitlement sources are rebuilt by walking loaded research definitions and requiring both a completed `ResearchData` and the matching granted `effectData` nonce. The resolver does not attempt to recover source identity from `UnlockList`, because those sets intentionally contain no provenance.

A V2 result and one or more legacy effects may target the same ID. Every source remains in the decision. Removing/resetting one source leaves access open while another source remains. Grant and reset of legacy access effects send a fresh full knowledge snapshot so client gameplay and JEI see the same answer.

## Physical Prototype Shell

`frostedresearch:upgrade_prototype` is non-stackable, is not added to the ordinary creative tab, and currently reuses the Frosted Research intelligence icon. A fabricated stack has a namespaced `frostedresearch:prototype` compound containing:

```text
schema: 1
profile: ResourceLocation string
profile_revision: positive integer frozen at fabrication
serial: random UUID
owner_team: Chorda team UUID
```

Every fabrication creates a new serial. `ItemHandlerHelper#giveItemToPlayer` supplies the existing inventory-or-nearby-drop delivery path. Uninitialized or damaged shells remain items and expose no valid identity. The current implementation has no host, socket, installation, contribution, tint, overlay, or upgrade GUI behavior.

## Command And Synchronization

Permission-level-2 commands are:

```text
/research result grant <result-id>
/research result revoke <result-id>
/research result info <result-id>
/research <online-player> result grant|revoke|info <result-id>
/frostedheart research result grant|revoke|info <result-id>
/frostedheart research <online-player> result grant|revoke|info <result-id>
```

The unqualified form targets the command source's current team. Placing an online player immediately after `research` targets that player's current team instead; `/frostedheart research <online-player> result ...` is the equivalent alias.

Finding, Design, Construction, and Procedure perform idempotent team acquisition. Prototype fabricates one new physical item for the affected player on every invocation. Revoke removes the four team-owned kinds, including orphan IDs whose definitions are no longer loaded; known Prototype results reject revoke because their authority is the physical item. Info reports the current definition and team acquisition/orphan state without mutation. These commands do not grant the topic's ordinary rewards.

`knowledge_data` is a full S2C replacement packet containing:

- the four acquired ID sets;
- `catalogRevision`;
- compiled `KnowledgeProjection`;
- compiled `TechnologyAccessProjection`, including provenance.

It is sent after login, team change, observation/Idea/report mutation, V2 acquisition, relevant legacy effect grant/reset, and catalogue reload. `FHKnowledgeDataSyncPacket` dispatches through the client-only `ClientKnowledgeSnapshotHandler`; in one queued client task that handler clears stale state, replaces the result-ID team copy, swaps the revision/knowledge/access projection record, and then requests JEI synchronization. This remains a full replacement rather than a delta protocol.

## Current Limits

- The geology vertical slice is the only bundled official topic; no official Prototype profile is bundled.
- Finding views must resolve through a registered `FindingViewHandler`; topic JSON cannot read world NBT or Java fields directly.
- Topic presentation currently compiles only its icon; the Phase 2 workflow fields are active through registered providers and resolvers.
- Prototype profiles are identity/revision declarations only.
- The geology workflow is the only production content currently wired to normal gameplay. A non-geology `block_state_comparison` topic exists only as a test-resource fixture for the generic record-pair/compare/resolution path; other V2 results still require later content or the administrator command.
- Existing third-party execution boundaries remain explicit integrations rather than automatic global interception.
