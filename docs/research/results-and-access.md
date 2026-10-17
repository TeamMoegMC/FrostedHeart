# Research Results And Technology Access

- Status: `Current`
- Last verified: `2026-09-08` (knowledge integration verified against source)
- Scope: Result payloads, compatibility topic catalogue, active knowledge projections, legacy entitlement provenance,
  physical prototype shell, and existing result commands
- Code anchors: [`ResearchResult`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResult.java), [`ResearchResultCatalogLoader`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalogLoader.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`TeamResearchService`](../../src/main/java/com/teammoeg/frostedresearch/api/TeamResearchService.java), [`UpgradePrototypeItem`](../../src/main/java/com/teammoeg/frostedresearch/item/UpgradePrototypeItem.java)

## Implemented Boundary

The five existing `ResearchResult` payloads are reused by the independent [knowledge system](../knowledge/README.md).
Observations, ideas, results, inbox learning, linking, notes, and hints are implemented there. This document owns the
result/access bridge and the existing topic/prototype formats. Existing `config/fhresearches/*.json`, including
`generator_efficiency_1` and its `generator_effi` stats effect, retain their legacy behavior. Evidence/experiment
execution, research-institute work scheduling, and installed numerical upgrades remain outside the implemented knowledge
core.

Five result definitions exist:

| Type           | Stable payload                 |         Team acquisition | Direct consumer                                                                                                         |
|----------------|--------------------------------|-------------------------:|-------------------------------------------------------------------------------------------------------------------------|
| `finding`      | `id`, optional `views`         |                      yes | `KnowledgeProjection` only                                                                                              |
| `design`       | `id`, nonempty `recipes`       |                      yes | recipe access                                                                                                           |
| `construction` | `id`, nonempty `multiblocks`   |                      yes | multiblock formation access                                                                                             |
| `procedure`    | `id`, nonempty `usable_blocks` |                      yes | right-click block access                                                                                                |
| `prototype`    | `id`, `profile`                | yes, as result knowledge | no numerical upgrade consumer yet; the existing `/research result grant` command separately fabricates a physical shell |

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
- ordinary item `rewards`, which are validated but not granted by `/research result grant`.

Unknown future fields may remain in the JSON but have no runtime meaning yet. A prototype declaration currently requires
`format: 1` and a positive integer `revision`; additional host, material, and numerical contribution fields currently
have no runtime implementation.

Catalogue validation aggregates diagnostics for wrong formats/revisions, duplicate global result IDs, empty or duplicate target lists, cross-type Construction/Procedure fields, missing recipes, multiblocks, blocks, reward items, and prototype profiles. Finding view-handler semantics are deferred until a Finding consumer exists.

`ResearchResultCatalog.Snapshot` is immutable. Every valid install receives a monotonically increasing `catalogRevision` and derives three managed target universes. Empty directories install a valid empty snapshot. An invalid reload logs all diagnostics and leaves the previous snapshot installed.

The compatibility topic catalogue remains available. New knowledge results use
`data/<namespace>/frostedresearch/knowledge/results/<path>.json`: `ResultDefinition` wraps the same payload with title,
body, independent understanding requirements, and an enabled flag. See [knowledge definitions](../knowledge/README.md)
for current examples and loading rules.

## Team Authority

`FRSpecialDataTypes.KNOWLEDGE_DATA` retains the Chorda component ID `frostedresearch:knowledge`. `TeamKnowledgeData` now
extends `KnowledgeState`, whose schema `2` saves the archive, inbox, original records, actual link/research/acquisition
history, hints, and daily opportunities. The earlier four acquired-ID sets are no longer the persisted authority; this
schema change does not migrate that experimental knowledge format. Legacy `TeamResearchData` persistence is separate and
unchanged.

All result kinds, including Prototype, use one idempotent `KnowledgeKey.result(id)` archive identity. Missing or
disabled definitions make archived knowledge dormant while preserving its identity and history. `KnowledgeService`
supplies ordinary learning, special initial/command grants, forgetting, events, and synchronization;
`TeamResearchService` bridges existing result commands to it.
See [knowledge state and API](../knowledge/state-and-api.md).

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
managed = declared by a knowledge result, compatibility topic result, or legacy lock effect
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

Every fabrication creates a new serial. `ItemHandlerHelper#giveItemToPlayer` supplies the existing
inventory-or-nearby-drop delivery path. Uninitialized or damaged shells remain items and expose no valid identity. The
shell has no host, socket, installation, contribution, tint, overlay, or upgrade GUI behavior. Learning a Prototype
knowledge element does not fabricate another shell or stack numerical effects.

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

These existing commands resolve definitions from `ResearchResultCatalog`. Finding, Design, Construction, and Procedure
grants invoke `KnowledgeService` as special command grants. The Prototype branch of `/research result grant` retains its
physical behavior: each invocation fabricates a new shell for the affected player. Its revoke branch still rejects known
Prototype definitions. Other revocations call the unified forget operation, including for retained unknown IDs. Info
reports the compatibility catalogue and its existing four-kind acquisition view. These commands do not grant ordinary
topic rewards.

The independent `/knowledge grant result <id>` grants a knowledge archive identity, including Prototype, idempotently;
it does not fabricate physical prototypes. `/knowledge forget result <id>` removes that knowledge identity through the
shared event flow. Use the [knowledge API and command reference](../knowledge/state-and-api.md) for ordinary acquisition
and learning.

`knowledge_snapshot` transports one logical full S2C replacement containing:

- schema-2 `TeamKnowledgeData`;
- `catalogRevision`;
- compiled `KnowledgeProjection`;
- compiled `TechnologyAccessProjection`, including provenance;
- `archive_view`, the current team-visible entries, actual discovered relations, acquisition history, and revealed
  hints.

It is sent after login, team change, knowledge mutations, relevant legacy effect grant/reset, and catalogue reload.
Client installation replaces the Chorda knowledge component and both projections, installs `KnowledgeClientState`, and
requests JEI synchronization. Knowledge synchronization encodes the full snapshot once as compressed NBT, divides it
into 128 KiB fragments, and installs the replacement only after all fragments arrive. This avoids the single-NBT packet
limit without adding a knowledge-record limit. Batch learning/deletion sends captured selections in requests of at most
128 keys, coalesces each request's team update, and accumulates all per-entry outcomes before showing the final
selection summary. Unrevealed link input definitions are not sent.

## Current Limits

- Finding view IDs are structural references only until view handlers are implemented.
- Topic presentation currently compiles only its icon; workflow fields are ignored.
- Prototype profiles are identity/revision declarations only.
- Ordinary knowledge gameplay can import result notes and learn them after satisfying independent understanding
  requirements. Full new research execution remains a separate integration; the knowledge API records starts/completions
  and preserves produced results.
- Existing third-party execution boundaries remain explicit integrations rather than automatic global interception.
