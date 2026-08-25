# Research Results And Technology Access

- Status: `Current`
- Last verified: `2026-08-25`
- Scope: Generative Research V2 Phase 1 result definitions, minimal datapack catalogue, team authority, projections, legacy entitlement provenance, prototype shell, commands, reload, and explicit non-goals
- Code anchors: [`ResearchResult`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResult.java), [`ResearchResultCatalogLoader`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalogLoader.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`TeamResearchService`](../../src/main/java/com/teammoeg/frostedresearch/api/TeamResearchService.java), [`UpgradePrototypeItem`](../../src/main/java/com/teammoeg/frostedresearch/item/UpgradePrototypeItem.java)

## Implemented Boundary

Phase 1 implements a mergeable foundation alongside the current `Research`/`Effect` system. It does not implement the evidence board, ideas, research institute, experiment apparatus, installed upgrades, or any formal V2 topic. Existing `config/fhresearches/*.json`, including `generator_efficiency_1` and its `generator_effi` stats effect, are unchanged.

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

A topic currently compiles only:

- required `format: 3`;
- optional `presentation.icon`;
- a list of the five typed `results`;
- ordinary item `rewards`, which are validated but not granted by the Phase 1 result command.

Unknown future fields may remain in the JSON but have no runtime meaning yet. A prototype declaration currently requires `format: 1` and a positive integer `revision`; additional host/BOM/contribution fields are deferred to Phase 5.

Catalogue validation aggregates diagnostics for wrong formats/revisions, duplicate global result IDs, empty or duplicate target lists, cross-type Construction/Procedure fields, missing recipes, multiblocks, blocks, reward items, and prototype profiles. Finding view-handler semantics are deferred until a Finding consumer exists.

`ResearchResultCatalog.Snapshot` is immutable. Every valid install receives a monotonically increasing `catalogRevision` and derives three managed target universes. Empty directories install a valid empty snapshot. An invalid reload logs all diagnostics and leaves the previous snapshot installed.

Phase 1 intentionally adds no built-in topic resources. Stable companion KubeJS recipe IDs and the five initial content slices begin in later phases.

## Team Authority

`FRSpecialDataTypes.KNOWLEDGE_DATA` registers the independent Chorda ID `frostedresearch:knowledge`. `TeamKnowledgeData` schema `1` persists:

```text
acquiredFindingIds
acquiredDesignIds
acquiredConstructionIds
acquiredProcedureIds
```

Acquisition is set-like and idempotent. A result removed from the current catalogue remains in its saved set as orphan history but produces no projection entry. There is no `acquiredPrototypeIds` field.

`KnowledgeDataAPI` resolves server or client team data. Mutations are coordinated by `TeamResearchService`; `TeamResearchManager` is now only a compatibility facade.

## Projection And Default-Open Rule

`KnowledgeProjection` contains only acquired, currently resolvable Finding IDs, their views, and result provenance. A Finding never changes a technology target.

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

Every fabrication creates a new serial. `ItemHandlerHelper#giveItemToPlayer` supplies the existing inventory-or-nearby-drop delivery path. Uninitialized or damaged shells remain items and expose no valid identity. Phase 1 has no host, socket, installation, contribution, tint, overlay, or upgrade GUI behavior.

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

It is sent after login, team change, V2 acquisition, relevant legacy effect grant/reset, and catalogue reload. Client installation replaces the Chorda knowledge component and both projections in one queued task, then requests JEI synchronization. There is no Phase 1 delta protocol.

## Current Limits

- No official topic/profile content is bundled.
- Finding view IDs are structural references only until view handlers are implemented.
- Topic presentation currently compiles only its icon; workflow fields are ignored.
- Prototype profiles are identity/revision declarations only.
- Normal gameplay cannot yet accept a V2 result; the administrator command is the only acquisition/fabrication entry point.
- Existing third-party execution boundaries remain explicit integrations rather than automatic global interception.
