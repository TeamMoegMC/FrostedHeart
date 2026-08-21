# Research Definitions And Codecs

- Status: `Current`
- Last verified: `2026-08-21`
- Scope: Definition sources, JSON schema, graph rules, clues, effects, localization, stable identifiers, reload, and editor behavior
- Code anchors: [`FHResearch#loadAll/#init`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`FHRegistry`](../../src/main/java/com/teammoeg/frostedresearch/FHRegistry.java), [`Research.CODEC`](../../src/main/java/com/teammoeg/frostedresearch/research/Research.java), [`ResearchCategory.CODEC`](../../src/main/java/com/teammoeg/frostedresearch/research/ResearchCategory.java), [`Clue.CODEC`](../../src/main/java/com/teammoeg/frostedresearch/research/clues/Clue.java), [`Effect.CODEC`](../../src/main/java/com/teammoeg/frostedresearch/research/effects/Effect.java)

## Where Definitions Come From

The server reads every direct child matching `*.json` in:

```text
<server-config-directory>/fhresearches/
```

`FHResearch#loadAll` uses the file basename without `.json` as the research's string ID. The ID is not a JSON field. For example, `generator_efficiency_1.json` defines research ID `generator_efficiency_1`.

This loader is configuration-based, not datapack-based:

- the mod's `src/main/resources/data/frostedresearch/` contains ordinary recipes and loot tables, not research definitions;
- the development `run/config/fhresearches/` directory contains a working catalogue but is not tracked by this Git repository;
- the companion modpack currently supplies 81 definitions under its `config/fhresearches/`;
- most definition translations in that pack live under `kubejs/assets/twr_researches/lang/`.

A standalone server therefore needs the config catalogue in addition to this mod JAR. Missing config files do not fall back to bundled definitions.

## Research JSON Schema

The effective schema from `Research.CODEC` is:

| JSON field | Type | Required | Default | Runtime meaning |
|---|---|---:|---|---|
| `icon` | `CIcon` | no | no-op icon | archive icon |
| `category` | research category | yes | — | one of the five fixed fields |
| `parents` | string array | no | `[]` | all resolvable parents must be finished before activation |
| `clues` | clue array | no | `[]` | contribution and required-gate definitions |
| `ingredients` | ingredient/count array | no | `[]` | activation material cost |
| `effects` | effect array | no | absent/empty | completion rewards and unlock declarations |
| `name` | string | no | `""` | display text or translation selector |
| `desc` | string array | no | `[]` | primary description lines |
| `descAlt` | string array | no | `[]` | alternate description lines |
| `showAltDesc` | boolean | no | `false` | use alternate-description behavior |
| `hideEffects` | boolean | no | `false` | suppress effect details in presentation |
| `locked` | boolean | no | `false` | stored as `inCompletable`; normal activation is rejected |
| `hidden` | boolean | no | `false` | hide from the normal archive graph and discovery UI |
| `keepShow` | boolean | no | `false` | keep the project showable after normal transitions |
| `infinite` | boolean | no | `false` | allow repeat levels after every effect is granted |
| `points` | integer | **yes** | — | required experiment points |
| `insight` | integer | no | `1` | insight-level activation cost |

Although the Java member holding required points is initialized to `1000`, `Research.CODEC` makes `points` mandatory. Omitting it from JSON is a decode error, not a `1000` default. The codec does not enforce positive points, nonnegative insight, nonnegative clue values, or bounded clue contributions.

The boolean flags are top-level fields. There is no nested `flags` object.

## Category Encoding

`ResearchCategory` is not an open registry. It contains exactly:

| Token | Canonical resource ID |
|---|---|
| `rescue` | `frostedresearch:rescue` |
| `living` | `frostedresearch:living` |
| `production` | `frostedresearch:production` |
| `ars` | `frostedresearch:ars` |
| `exploration` | `frostedresearch:exploration` |

For compatibility, the codec also accepts the legacy namespace `frostedheart:<token>`. The client normalizes both forms to the same category. Packet compression represents categories by ordinal byte, so reordering the enum-like `ALL` list would be a network compatibility change.

## Graph Semantics

`Research.parents` is a `HashSet<String>`. During `FHResearch#reindex`/`Research#doIndex`, each resolvable parent receives the research in its derived `children` set. `children` is not serialized.

Runtime semantics are:

- activation is unlocked only when every **resolvable** parent is completed;
- a root research has no parents and is unlocked;
- `isShowable` exposes roots and projects for which at least one parent has become visible/unlocked, subject to hidden/editor UI rules;
- a missing parent ID is silently omitted by `getParents`, so a project whose parents are all missing behaves like a root for unlocking;
- no server-side validation rejects missing parents or cycles;
- the archive graph separately diagnoses missing-parent edges and strongly connected cycles so it can still lay out malformed definitions.

The system is therefore a directed graph by convention, not a schema-enforced DAG.

## Clue Base Schema

`Clue.CODEC` is a typed codec. Every clue has:

| Field | Type | Required | Default | Meaning |
|---|---|---:|---|---|
| `type` | string discriminator | yes | — | selects the concrete clue codec |
| `id` | string | yes | — | clue nonce; persistence, localization, and delta-sync identity |
| `name` | string | no | `""` | label override |
| `desc` | string | no | `""` | description override |
| `hint` | string | no | `""` | hint override |
| `required` | boolean | no in base codec | `false` | must be completed before the research can finish |
| `value` | float | yes | — | dimensionless contribution fraction |

`required` and `value` are independent. A required clue with `value: 0` is a pure completion gate; a non-required clue may still contribute points. Contributions are summed, including negative or greater-than-one values, because the codec performs no range validation.

### Built-In Clue Types

| `type` | Extra fields | Trigger and scope |
|---|---|---|
| `custom` | none | no built-in trigger; external code must complete it |
| `item` | `item` ingredient/count; optional `consume=false` | drawing-desk submission against the current active research; may consume the matched stack |
| `game` | required integer `level` | drawing-desk card game completion at a sufficient level |
| `advancement` | `advancement`; optional `criterion=""`; listener `always` | advancement/criterion listener |
| `kill` | `entity`; listener `always` | completes when the team listener receives a kill whose entity type matches `entity` |

`MinigameClue#setLevel` clamps to `0..3`, but codec construction assigns the decoded value directly. JSON levels outside that interval therefore bypass the setter clamp.

`ListenerClue` has its own base codec in which `required`, `value`, and `always` are required JSON fields. This differs from the ordinary clue base where `required` defaults to false. In current code, `always: true` initialization passes a null team to listeners whose implementations dereference the team; see [known-risks.md](known-risks.md).

`ResearchHooks#kill` ignores already-completed clues, evaluates the killed entity through `KillClue#isCompleted`, and marks/removes the matching team listener after completion. The current development catalogue's three `animal_cage.json` kill clues therefore use the normal server kill-event path.

## Effect Base Schema

`Effect.CODEC` is another typed codec. Every effect has:

| Field | Type | Required | Default | Meaning |
|---|---|---:|---|---|
| `type` | string discriminator | yes | — | selects the effect codec |
| `id` | string | yes | — | effect nonce; saved grant identity and delta-sync identity |
| `name` | string | no | `""` | display label |
| `tooltip` | string array | no | `[]` | descriptive lines |
| `icon` | `CIcon` | no | none | presentation icon |
| `hidden` | boolean | no | `false` | suppress effect presentation |

### Built-In Effect Types

| `type` | Extra fields | Persistent result | Grant mode |
|---|---|---|---|
| `multiblock` | `multiblock` resource ID | adds to team `MultiblockUnlockList` | automatic/replayed |
| `recipe` | either `item` ingredient or `recipes` resource-ID list | expands and adds recipes to team `RecipeUnlockList` | automatic/replayed |
| `use` | `blocks` block list | adds to team `BlockUnlockList` | automatic/replayed |
| `category` | `category` resource ID | adds to team `CategoryUnlockList` | automatic/replayed |
| `stats` | `vars` string, `val` double, `percent` boolean | adds a numeric value to persisted `TeamResearchData.variants` | automatic; load mode does not add again |
| `custom` | none | records the effect as granted | automatic |
| `item` | `rewards` item-stack list | gives items to the claiming player | player claim |
| `command` | `rewards` command-string list | runs commands as a server command source | player claim |
| `experience` | `experience` integer | gives player experience | player claim |

Effects that need a concrete player return ungranted when completion or load calls them with `player == null`. They remain available for `FHEffectTriggerPacket` and the project dialog's claim action. Unlock-list effects grant during completion and replay during team initialization. Stats are already persisted in `variants`, so `EffectStats` deliberately does nothing in load mode.

Command effects substitute `@p`, `@x`, `@y`, `@z`, and `@t` before dispatch. Because definitions are trusted server configuration, anyone who can edit research JSON effectively controls a server-console command surface.

`EffectCrafting` resolves recipe IDs and ingredient-matched outputs through the current `RecipeManager`. Invalid explicit recipe IDs are skipped. Its resolution is refreshed after resource reload.

## Text And Localization

Definitions may carry display strings directly or rely on generated translation keys. Clue nonces participate in fallback keys of the form:

```text
clue.frostedresearch.<research-id>.clue.<clue-id>.<field>
```

Research/effect helpers similarly derive keys from the research ID and nonce when configured text does not provide the final display text. Keep the file ID and all nonces stable or update language assets at the same time.

The mod JAR currently provides `en_us` and `zh_cn` base language resources and five category icons. The companion pack owns much of the research-specific text, so definition changes may require edits in the companion repository after reading its `AGENTS.md`.

## Stable Identity And Ordering Contract

Four identifiers or orderings affect saved/networked meaning:

| Element | Durable identity | Additional wire identity | Unsafe change |
|---|---|---|---|
| research | filename/string ID | persistent integer slot | rename/reuse without migration |
| parent | research string ID | — | rename without updating every child |
| clue | `id` nonce | list index in delta/full network data | rename, duplicate, or reorder |
| effect | `id` nonce | list index in delta/full network data | rename, duplicate, or reorder |

Persistent `ResearchData` maps clue/effect state by nonce, but packets compress it into definition-order lists. Both stable unique nonces **and** stable ordering matter. There is no schema version or migration layer that translates old IDs/orderings.

`FHRegistry` preserves research string-to-integer slots across restarts. A removed definition leaves a null/tombstone slot so later numeric IDs do not shift. `prepareReload` clears live objects while retaining those names; `clear` discards the mapping. `Research#setNewId` and editor deletion reset affected team data rather than migrate it.

## Load, Reload, And Editor Lifecycle

At server startup `FHResearch#load`:

1. reads the saved research name-slot list from `fhregistries.dat` when present;
2. loads and decodes config JSON;
3. installs definitions back into their historical slots or appends new slots;
4. derives children, initializes clues/effects, populates global lock declarations, and emits load events.

`ResearchLoadEvent.Pre`, `.Post`, and `.Finish` bracket definition processing. `PopulateUnlockListEvent` lets integrations contribute lock declarations.

The permission-level-2 edit command toggles editor state stored in `fheditor.dat`. Editor save operations write config JSON. The editor is operational tooling, not a compatibility migration facility: rename/delete operations can discard progress, and the archive graph still excludes hidden nodes even though editor lists and details can access them.

## Authoring Checklist

Before deploying a definition change:

1. Keep existing research filenames, clue IDs, effect IDs, clue order, and effect order unless a migration has been designed.
2. Give every research a positive `points` value and every insight cost a nonnegative value.
3. Use unique clue/effect IDs inside each research.
4. Resolve every parent ID and verify the whole catalogue is acyclic.
5. Keep contribution values intentional; normally use `0..1` and ensure their sum reflects the desired point shortcut.
6. Avoid `always: true` listener clues until their null-team initialization path is fixed and tested.
7. Test kill clues with both matching and non-matching entity types when changing listener routing.
8. Treat command effects as privileged server configuration.
9. Update companion-pack definitions/translations together, then test an existing world as well as a new world.
10. Test definition login sync and an already-open archive after `/reload`.
