# Research Definitions And Codecs

- Status: `Current`
- Last verified: `2026-08-26`
- Scope: Legacy definitions plus V2 Phase 2 topic workflow/result/profile datapack schema, stable identities, validation, and reload
- Code anchors: [`FHResearch#init/#reloadCatalog`](../../src/main/java/com/teammoeg/frostedresearch/FHResearch.java), [`ResearchCatalog`](../../src/main/java/com/teammoeg/frostedresearch/ResearchCatalog.java), [`ResearchTopicDefinition`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchTopicDefinition.java), [`ResearchWorkflowRegistry`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchWorkflowRegistry.java), [`ResearchResultCatalogLoader`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalogLoader.java), [`ResearchResult`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResult.java)

## Where Definitions Come From

The server reads every direct child matching `*.json` in:

```text
<server-config-directory>/fhresearches/
```

`FHResearch#loadAll` uses the file basename without `.json` as the research's string ID. The ID is not a JSON field. For example, `generator_efficiency_1.json` defines research ID `generator_efficiency_1`.

The playable legacy loader is configuration-based, not datapack-based:

- the mod's `src/main/resources/data/frostedresearch/` contains ordinary recipes and loot tables, not research definitions;
- the development `run/config/fhresearches/` directory may contain a local working catalogue but is not a production fallback;
- the companion modpack currently supplies 81 definitions under its `config/fhresearches/`;
- most definition translations in that pack live under `kubejs/assets/twr_researches/lang/`.

A standalone server therefore needs the complete config catalogue in addition to this mod JAR. Missing or empty config does not fall back to bundled definitions and now aborts world startup. The production companion pack is the only authoritative catalogue distribution.

### Local full-pack client runs

`run/config/fhresearches/` may reference registry entries supplied by companion-pack mods and KubeJS. The `runClient` classpath therefore obtains Immersive Industry, Stone Age, Charcoal Pit, and KubeJS from CurseMaven using pinned project/file IDs. Production JARs should not be copied into `run/mods/`: raw KubeJS binaries are not remapped for the named development environment and fail during Mixin application.

KubeJS also requires Rhino and Architectury. Rhino is resolved from Latvian Mods Maven and Architectury from its existing repository; these are hard libraries rather than KubeJS addons. No KubeJS Create, KubeJS Immersive Engineering, LootJS, or PonderJS runtime is included. The local `run/kubejs/` input keeps only `startup_scripts/src/registries/item.js`, which registers the item IDs used while decoding the production research catalogue without running companion recipes, client behavior, or server behavior.

Stone Age 1.6.8 uses a `DistExecutor.safeRunForDist` lambda shape that Forge rejects only in a non-production environment. `ExampleModMixin` redirects that proxy selection to `unsafeRunForDist`; the selected client/server proxy and production sided behavior remain the same, while the development-only referent validator is bypassed.

## V2 Datapack Topic And Result Schema

V2 result declarations are a separate, implemented datapack slice:

```text
data/<namespace>/frostedresearch/topics/<path>.json
data/<namespace>/frostedresearch/prototypes/<path>.json
```

Topics require `format: 3`. In addition to optional `presentation`, typed `results`, and ordinary item `rewards`, Phase 2 decodes `legacy`, `idea_sources`, `inspiration`, `protocols`, and `resolution`. Prototype declarations require `format: 1` and a positive integer `revision`. The mod datapack bundles `the_winter_rescue:geology_understanding` with `legacy.mode = coexist` and its two stable recipes; the companion pack mirrors those resources.

Workflow references are closed registries rather than reflective Java/NBT paths. `ResearchWorkflowRegistry` owns executable `IdeaSourceHandler`, inspiration-provider ID, `ProtocolHandler`, `ResolutionHandler`, and `FindingViewHandler` registrations. The core currently registers the generic `frostedresearch:record_pair`, `frostedresearch:compare_records`, `frostedresearch:comparison_resolution`, and `frostedresearch:archive` fixture handlers. Frosted Heart's `GeologyResearchIntegration` separately registers `frostedheart:field_evidence`, `frostedheart:person_experience`, `frostedheart:manual_field_comparison`, `frostedheart:field_comparison_resolution`, `frostedheart:geology_archive`, and `frostedheart:prospecting_report_detail`.

`ResearchResultCatalogLoader` verifies every referenced provider/resolver/view, nonempty and recognized comparison outcome, required block tag, resolution result ID, and exact result target. Required block tags are checked against the current reload's `ResourceManager` resources rather than the not-yet-bound runtime tag collection. A Design still names exact recipe IDs, including `the_winter_rescue:research/copper_pro_pick`. Any workflow or result diagnostic rejects the whole candidate and retains the previous `ResearchResultCatalog.Snapshot`.

The topic declaration is server execution data, not a pre-discovery task list. `ResearchWorkflowRegistry#findCandidates` evaluates loaded topic definitions against already archived records but returns nothing to the worksheet until a registered pattern matches; action cards are compiled only from persisted `IdeaRecord` values. Consequently topic names, target results, and protocol actions are not projected merely because the catalogue contains a topic.

`ResearchResult.CODEC` dispatches by a stable string `type` and preserves raw `ResourceLocation` values:

| `type` | Fields | Validation |
|---|---|---|
| `finding` | `id`, optional `views` | every view must name a registered `FindingViewHandler` |
| `design` | `id`, `recipes` | nonempty; recipes must exist |
| `construction` | `id`, `multiblocks` | nonempty; multiblocks must exist; rejects `usable_blocks` |
| `procedure` | `id`, `usable_blocks` | nonempty; blocks must exist; rejects `multiblocks` |
| `prototype` | `id`, `profile` | referenced prototype declaration must exist |

Result IDs are globally unique across the effective topic catalogue. Loader diagnostics also cover duplicate target IDs, missing reward items, non-positive reward counts, wrong formats/revisions, and overlong stable IDs. A valid candidate atomically replaces `ResearchResultCatalog.Snapshot` and increments its revision. Any diagnostic retains the last-known-good snapshot.

See [results-and-access.md](results-and-access.md) for runtime projection and legacy coexistence.

## Research JSON Schema

The effective schema from `Research.CODEC` is:

| JSON field | Type | Required | Default | Runtime meaning |
|---|---|---:|---|---|
| `icon` | `CIcon` | no | no-op icon | archive icon |
| `category` | research category | yes | — | one of the five fixed fields |
| `parents` | string array | no | `[]` | every validated parent must be finished before activation |
| `legacyIds` | string array | no | `[]` | previous research IDs accepted only while migrating saved data |
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

Although the Java member holding required points is initialized to `1000`, `Research.CODEC` makes `points` mandatory. Omitting it from JSON is a decode error, not a `1000` default. Whole-catalogue validation additionally requires `points > 0`, `insight >= 0`, and positive ingredient counts.

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

`Research.parents` is a `HashSet<String>`. During `FHResearch#reindex`/`Research#doIndex`, every validated parent receives the research in its derived `children` set. `children` is not serialized.

Runtime semantics are:

- activation is unlocked only when every parent is completed;
- a root research has no parents and is unlocked;
- `isShowable` exposes roots and projects for which at least one parent has become visible/unlocked, subject to hidden/editor UI rules;
- blank, missing, self-referential, and cyclic parents are rejected before the candidate catalogue can be installed;
- the archive graph retains defensive missing-edge/cycle diagnostics for tests and locally constructed models, but production definitions cannot reach that state.

The installed production catalogue is therefore a validated directed acyclic graph.

The companion and development definitions `coke_oven`, `mechanical_bellows`, `storage_drawers`, and `tetra` explicitly declare `"parents": []`. They are independent roots; the deleted `workbench` ID is not treated as an optional or implicit prerequisite.

## Clue Base Schema

`Clue.CODEC` is a typed codec. Every clue has:

| Field | Type | Required | Default | Meaning |
|---|---|---:|---|---|
| `type` | string discriminator | yes | — | selects the concrete clue codec |
| `id` | string | yes | — | clue nonce; persistence, localization, and delta-sync identity |
| `legacyIds` | string array | no | `[]` | previous clue nonces accepted only for saved-data migration within this research |
| `name` | string | no | `""` | label override |
| `desc` | string | no | `""` | description override |
| `hint` | string | no | `""` | hint override |
| `required` | boolean | no in base codec | `false` | must be completed before the research can finish |
| `value` | float | yes | — | dimensionless contribution fraction |

`required` and `value` are independent. A required clue with `value: 0` is a pure completion gate; a non-required clue may still contribute points. The catalogue validator requires every contribution to be finite and within `[0,1]`.

### Built-In Clue Types

| `type` | Extra fields | Trigger and scope |
|---|---|---|
| `custom` | none | no built-in trigger; external code must complete it |
| `item` | `item` ingredient/count; optional `consume=false` | drawing-desk submission against the current active research; may consume the matched stack |
| `game` | required integer `level` | drawing-desk card game completion at a sufficient level |
| `advancement` | `advancement`; optional `criterion=""`; listener `always` | advancement/criterion listener |
| `kill` | `entity`; listener `always` | completes when the team listener receives a kill whose entity type matches `entity` |

`MinigameClue` decodes through `Codec.intRange(0, 3)`, and catalogue validation repeats that invariant before installation.

`ListenerClue` has its own base codec in which `required`, `value`, and `always` are required JSON fields. This differs from the ordinary clue base where `required` defaults to false. `always: false` listeners are registered only while their research is current for a team. `always: true` listeners are registered during definition indexing with a null team ID, which `ResearchHooks.ListenerList` interprets as global scope: the listener is considered for every team event and its own clue-completion state prevents duplicate completion.

`ResearchHooks#kill` ignores already-completed clues, evaluates the killed entity through `KillClue#isCompleted`, and marks/removes the matching team listener after completion. The current development catalogue's three `animal_cage.json` kill clues therefore use the normal server kill-event path.

## Effect Base Schema

`Effect.CODEC` is another typed codec. Every effect has:

| Field | Type | Required | Default | Meaning |
|---|---|---:|---|---|
| `type` | string discriminator | yes | — | selects the effect codec |
| `id` | string | yes | — | effect nonce; saved grant identity and delta-sync identity |
| `legacyIds` | string array | no | `[]` | previous effect nonces accepted only for saved-data migration within this research |
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

## Stable Identity And Migration Contract

Runtime persistence and network state use the following stable identities:

| Element | Formal identity | Migration declaration | Scope |
|---|---|---|---|
| research | filename/string ID | top-level `legacyIds` | whole catalogue |
| parent | research string ID | update the parent reference to the new formal ID | whole catalogue |
| clue | `id` nonce | clue `legacyIds` | one research |
| effect | `id` nonce | effect `legacyIds` | one research |

Aliases never become new persistent keys. During team-data reconciliation, a legacy record moves only when the formal key is absent. If both exist, the formal record wins and the legacy record remains unchanged as an orphan with a warning; records for deleted definitions are also preserved as orphans. Reordering definitions, clues, or effects no longer changes saved or network meaning.

`fhregistries.dat` and `FHRegistry` retain historical name/slot ordering only to translate a schema-0/1 integer `active` value. They are no longer authoritative for current persistence or packet identity. A missing snapshot, out-of-range slot, or slot whose definition was deleted clears only the current selection and logs a migration diagnostic.

## Whole-Catalogue Validation

`ResearchCatalog#load` sorts direct `*.json` children by filename, parses all files into a candidate, aggregates diagnostics, and installs nothing until the whole candidate is valid. Validation rejects:

- a missing or empty directory;
- blank, over-128-character, or conflicting formal IDs and `legacyIds` in their scopes;
- blank, missing, self-referential, or cyclic parents;
- non-positive research points, negative insight, and non-positive research/item-clue counts;
- non-finite or out-of-range `[0,1]` clue contributions;
- minigame levels outside `0..3`.

`./gradlew validateResearchCatalog -PresearchCatalogDir=<path>` performs the registry-independent catalogue preflight used for external pack CI. Actual Forge startup then runs the full production codecs and the same runtime invariants after all required registries are available.

## Load, Reload, And Editor Lifecycle

For a local world, `MinecraftResearchCatalogPreflightMixin` first validates the catalogue at `Minecraft#doWorldLoad` before the integrated-server thread exists. An invalid candidate is logged in full, the pending world resources and level lock are closed, and an error screen returns the player to the title screen. This prevents vanilla's pre-level loading loop from waiting forever for a chunk-progress listener that an aborted server never created. It does not replace server authority.

At server startup `FHResearch#load`:

1. reads the saved research name-slot list from `fhregistries.dat` when present;
2. builds a filename-sorted candidate from all config JSON;
3. aggregates codec and graph/value/identity diagnostics;
4. aborts startup on any diagnostic, or atomically installs, reindexes, populates global lock declarations, and emits load events.

The early-shutdown path is also defensive: `MinecraftServerMixin#saveAllChunks` does not emit custom save events or dereference an absent overworld, and `TemperatureUpdate#shutdown` is idempotent when climate initialization never ran.

During `/reload`, candidate parsing and validation happen before `ResearchHooks.reload` or registry mutation. Failure logs the aggregated diagnostics and keeps the previous catalogue, listeners, and team data. Success stops old listeners, installs the candidate, reconciles every loaded team's aliases/orphans, rebuilds derived unlocks/listeners, sends definitions, and then sends full team state.

The client likewise stages the registry start plus every `FHResearchSyncPacket`. Only a valid `FHResearchSyncEndPacket` swaps the complete client catalogue; malformed/incomplete definition streams leave the previous UI definitions intact.

`ResearchLoadEvent.Pre`, `.Post`, and `.Finish` bracket definition processing. `PopulateUnlockListEvent` lets integrations contribute lock declarations.

The permission-level-2 edit command toggles editor state stored in `fheditor.dat`. Editor save operations write config JSON. The editor is operational tooling, not an automatic migration author: planned renames must declare `legacyIds`. Editor graph snapshots, projection, layout, nodes, and edges include hidden research; normal mode excludes it.

## Authoring Checklist

Before deploying a definition change:

1. Keep existing research filenames and clue/effect IDs at no more than 128 characters, or declare the previous identity exactly once in `legacyIds`; ordering may change safely.
2. Give every research a positive `points` value and every insight cost a nonnegative value.
3. Use unique clue/effect IDs inside each research.
4. Resolve every parent ID and keep the whole catalogue acyclic; the production loader enforces both.
5. Keep contribution values intentional; normally use `0..1` and ensure their sum reflects the desired point shortcut.
6. Treat `always: true` as a global listener and test its real advancement/kill event path before deployment; use `always: false` when the clue should run only while its project is selected.
7. Test kill clues with both matching and non-matching entity types when changing listener routing.
8. Treat command effects as privileged server configuration.
9. Update companion-pack definitions/translations together, run `validateResearchCatalog`, then test an existing world as well as a new world.
10. Test definition login sync and an already-open archive after `/reload`.
