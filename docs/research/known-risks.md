# Research System Known Risks And Validation Gaps

- Status: `Current`
- Last verified: `2026-08-27`
- Scope: Resolved legacy issues, implemented V2 Phase 2 boundaries, and remaining integrated/manual validation
- Code anchors: [`ResearchCatalog`](../../src/main/java/com/teammoeg/frostedresearch/ResearchCatalog.java), [`ResearchResultCatalog`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/ResearchResultCatalog.java), [`TeamKnowledgeData`](../../src/main/java/com/teammoeg/frostedresearch/data/TeamKnowledgeData.java), [`TechnologyAccessResolver`](../../src/main/java/com/teammoeg/frostedresearch/knowledge/TechnologyAccessResolver.java), [`ResearchHooks`](../../src/main/java/com/teammoeg/frostedresearch/ResearchHooks.java)

## Current Assessment

The legacy hardening items recorded before Phase 2 remain resolved. The rapid Phase 2 generalization has completed its serial compile, unit, GameTest, build, and catalogue-preflight baseline, but not the restarted-client portion of the matrix. This document therefore distinguishes server/automated evidence from presentation and live synchronization behavior that still needs client re-verification.

Create and Immersive Engineering are required by the Frosted Heart product. Missing-Create/missing-IE startup combinations are intentionally outside scope. BROWSE and EXPERIMENT remain future UI/system features and receive no new entry point or data model in this work.

V2 Phase 2 now includes timed block/living-entity records with retained context, provider enrichment, evidence pins, hidden inspiration candidates, executable Idea/protocol/resolution/Finding-view handlers, a three-page full team-knowledge presentation, generic person-package dialogue, and the bundled `the_winter_rescue:geology_understanding` content plugin. Prototype host/socket/contribution behavior and resident computation remain later phases. The companion pack's 81 legacy definitions remain unchanged.

Construction and Procedure have distinct contracts: Construction is only a multiblock formation entitlement; Procedure is only a right-click block entitlement. Existing `EffectBuilding` and `EffectUse` project into those respective channels without becoming new result assets.

## Resolved Definition, Identity, And Math Risks

| Resolved | Former risk | Current contract | Automated evidence |
|---|---|---|---|
| 2026-08-23 | partial catalogue mutation, missing parents/cycles, invalid values and duplicate IDs | `ResearchCatalog` parses filename-sorted candidates and aggregates codec, identity, parent/DAG, point/insight/count, clue contribution, and minigame diagnostics before installation | `ResearchCatalogPreflightTest`; external catalogue validation task |
| 2026-08-23 | invalid local catalogue threw before the chunk-progress listener existed, leaving `Minecraft#doWorldLoad` in its wait loop | local worlds run the full catalogue preflight before the integrated-server thread; rejection closes pending resources and shows a localized error screen; partial-start save and climate shutdown paths are null-safe | Mixin target/refmap validation during `compileJava`; invalid-catalogue in-game regression scenario |
| 2026-08-23 | active/project/clue/effect meaning depended on registry/list order | current persistence and packets use research string IDs and clue/effect nonces; order may change safely | `TeamResearchDataMigrationTest`, `ResearchNetworkPacketTest` |
| 2026-08-23 | no rename/delete migration contract | optional `legacyIds` migrate only into an absent formal key; canonical conflicts and deleted records remain preserved orphans with diagnostics | `TeamResearchDataMigrationTest` |
| 2026-08-23 | old integer active selection could silently retarget | schema `2` saves string active IDs; legacy integers resolve only through a real `fhregistries.dat` snapshot and otherwise clear only selection | migration tests plus defensive constructor paths |
| 2026-08-23 | integer progress, negative commits, overflow and non-finite progress | committed points are `long`; negative persisted values normalize to zero; negative API input throws; zero is inert; arithmetic is overflow-safe and progress finite in `[0,1]` | `ResearchDataBehaviorTest` |
| 2026-08-23 | `FHRegistry#runIfPresent(int)` used one-based subtraction | lookup is zero-based with complete bounds checks | `FHRegistryTest` |
| 2026-08-23 | string `putVariantLong` wrote a double | all long overloads write `LongTag` without precision loss above `2^53` | `ResearchDataAPITest` |

## Resolved Persistence And Network Risks

| Resolved | Former risk | Current contract | Automated evidence |
|---|---|---|---|
| 2026-08-23 | full/delta payloads used registry order, list indices and effect `BitSet` | full and incremental state are string/nonce-keyed maps or fields | codec, migration, and packet tests |
| 2026-08-23 | custom clue NBT disappeared on the wire | `ClueData` network encoding includes the full custom `CompoundTag` | `ResearchDataBehaviorTest`, `ResearchNetworkPacketTest` |
| 2026-08-23 | malformed/stale packets dereferenced invalid targets | bounded IDs/NBT plus enum/value/target validation discard bad input with rate-limited diagnostics | `ResearchNetworkPacketTest` |
| 2026-08-23 | client could display a partially received catalogue | definition packets stage until end-of-transfer whole-catalogue validation succeeds | staged `FHResearch` client lifecycle |
| 2026-08-23 | full team replacement left an open archive stale | dedicated replacement notification rebuilds visibility, graph/layout, and state caches while retaining valid navigation/camera state | archive state/visibility tests and notification path |
| 2026-08-23 | completing a bound non-current research cleared the current project | completion clears selection only when the completed formal ID equals the active ID | `TeamResearchData#checkResearchComplete` guard |

The new binary packet format is intentionally incompatible with older clients. `FRNetwork` requires an exact mod version, so mixed versions are rejected during the Forge channel handshake rather than accepted with ambiguous state.

## Resolved Gameplay And Integration Risks

| Resolved | Former risk | Current contract | Automated evidence |
|---|---|---|---|
| 2026-08-23 | calculator points could be redirected across teams and extraction ignored `max` | placement/first-real-interaction establishes persistent team owner; FakePlayer/cross-team access fails; bounded extraction retains the remainder | `ResearchGameTests` Forge GameTest |
| 2026-08-23 | machine APIs could bypass ownership | `ComputeMachine` query/extraction requires `ServerPlayer`; click and rubbing use the same authorization | full and no-FTB/no-JEI GameTest runs |
| 2026-08-23 | static Create owner leaked across executions | actual Create recipe execution is wrapped in nested, exception-safe `ThreadLocal` owner context | nested/exception/double-thread `ResearchHooksTest`; successful mixin startup |
| 2026-08-23 | absent/fake/ownerless automation could pass locked content | player paths use current real-player team; machine paths use persisted owner; locked content fails closed without a valid authority | entry-point source audit and GameTest |
| 2026-08-23 | every nonempty item appeared examinable | side-effect-free eligibility exactly models unfinished item clue, valid rubbing, or inspiration actions | UI predicate and server submission now share the same conditions |
| 2026-08-23 | duplicate/dead Frosted Research FTB event bridge | only Chorda forwards FTB team create/change/delete/transfer events; Frosted Research consumes the Chorda event | no-FTB startup plus Chorda bridge source contract |
| 2026-08-23 | ordinary research code loaded JEI/FTB client classes when absent | neutral JEI bridge, class-gated FTB mixins, and optional pseudo sidebar mixin isolate optional dependencies | `runGameTestServer -PwithoutFtb -PwithoutJei` |
| 2026-08-23 | advancement/kill/`always` listeners could leak, duplicate, or cross team boundaries across reload/removal | team listeners use exact team IDs, `always` remains global per triggering team, and reload/removal rebuilds from a cleared listener registry | two-team real-event lifecycle assertions in `ResearchGameTests` |
| 2026-08-23 | reset/recompletion and non-current completion could revoke or clear unrelated state | reversible effects are revoked through their effect contract, orphan progress survives removal, and only the exactly current completion clears selection | `TeamResearchDataResetTest` plus real team-data assertions in `ResearchGameTests` |

## Resolved Client/UI Risks

| Resolved | Former risk | Current contract | Automated evidence |
|---|---|---|---|
| 2026-08-23 | hidden research disappeared from the editor graph | editor visibility is carried from snapshot through projection/layout and node/edge rendering; normal privacy remains unchanged | `ResearchGraphVisibilityTest` |
| 2026-08-23 | fit always selected minimum zoom | actual visible bounds are fit to both axes with `24px` padding, clamped to `0.15..1.75` | shared viewport tests and implementation |
| 2026-08-23 | reflection removed/restored FTB sidebar groups | native widgets are disabled while open and an optional pseudo mixin cancels FTB's visibility-ignoring render; no FTB collection is mutated | full/no-FTB startup; in-game reload QA remains below |

## Phase 2 Rapid-Iteration Boundaries

| State | Boundary | Consequence / next evidence |
|---|---|---|
| source fix, client recheck pending | the earlier `FHKnowledgeDataSyncPacket` used a Forge safe-referent lambda that rejected the client installer; it now dispatches to `ClientKnowledgeSnapshotHandler` with `DistExecutor.unsafeRunWhenOn` | restart the development client and prove the already-saved records appear after login and after `/reload` |
| implemented, integrated recheck pending | an invalid Phase 1 smoke topic in the current debug world referenced the unregistered `frostedresearch_test:smoke_view`; that reference has been removed from the test fixture and current debug-world mirror | confirm `catalogRevision > 0` and the bundled geology topic installs in the same world without deleting its saved knowledge |
| local full projection, server paging later | `KnowledgeLabProjection` sends every client-safe observation, Idea, artifact and acquired result in the full team snapshot; the three archive pages scroll locally and preserve explicit orphan rows, but there is not yet a server cursor/search protocol for very large histories | current teams can inspect their complete synchronized history; add bounded server paging before observation counts make full snapshots materially expensive |
| intentionally minimal | the evidence board does not yet have a typed, extensible `EvidenceRelation` engine for contradictions, repeated sources, or context comparisons | registered Idea handlers remain authoritative; add neutral relation cards when a second context-heavy content fixture needs them |
| capture field selection is profile-based | the notebook currently cycles `STANDARD`, `COMPACT`, and `ENVIRONMENT` retained-field profiles and automatically saves successful captures; it has no context-chip editor or save/discard draft screen yet | use the three profiles to validate the context contract, then add per-capture chips/draft review without changing Idea semantics |
| compatibility coupling remains | `KnowledgeRecord` and `ObservationProvider.Contribution` still type sealed facts as `OreProspectingModel.Snapshot`, and `TeamResearchService` retains geology aliases/wrappers for existing callers | the executable workflow and UI are generic, but Frosted Research's model package is not yet completely independent of Frosted Heart geology types |
| intentionally small content catalogue | `PersonKnowledgePackageCatalog` is a generic registration surface and Frosted Heart's integration registers prospecting/cold-weather content, but dialogue currently shares the first available package and only prospecting emits a research offer | the common dialogue/persistence/recruitment path is reusable; data-authored multi-offer choice remains future iteration |
| automated baseline passed, client pending | the serial compile, full unit suite, normal and no-FTB/no-JEI GameTest runs, build, and 81-definition catalogue preflight pass after the three-page/context refactor; the normal server run reports `18` required tests passed and the optional-dependency run exits successfully | keep Phase 2 in progress until the restarted-client acceptance below proves the presentation/sync path |

## Remaining Supported-Boundary Work

| Priority | Kind | Boundary | Acceptance |
|---|---|---|---|
| P2 | planned audit | Unknown third-party machines and capability/recipe execution paths are not automatically intercepted by the owner contract | complete [`2026-08-23_00-27-05_research-third-party-automation-ownership-audit.md`](../../plans/2026-08-23_00-27-05_research-third-party-automation-ownership-audit.md), inventory every installed automation path, add owner/no-owner tests, and document each explicit integration or accepted bypass |
| P2 | integrated QA | Drawing-desk menu/CUI authorization, full-state team switching, overlapping/non-reversible/infinite reward reset/recompletion, and malicious coordinate scenarios still need the two-client matrix | unauthorized operations cause no state change or disconnect; authorized state refreshes immediately; retained rewards neither duplicate nor disappear |
| P2 | visual QA | FTB sidebar suppression during resource reload and GUI-scale `1/2/4` graph fit require a real client | sidebar never renders/clicks while open and naturally reflects current FTB state after close; all nodes retain the required margin |
| P3 | future feature | `ResearchOpenContext.BROWSE` has state-model support but no production screen/entry | design and implement separately if a standalone read-only browser is approved |
| P3 | future feature | `EXPERIMENT` is an empty tab reserved for future town/research integration | define its authoritative records and lifecycle in a separate design/plan before implementation |
| P3 | planned content | geology is the only bundled topic; resident calculation and institute/experiment workflows are not implemented | implement the Phase 3 vertical slices without changing the Phase 1–2 result/access contracts |
| P2 | client acceptance | timed block/entity capture, profile switching, the three archive pages, free laboratory/card navigation, direct Idea creation, lightweight geology theory, neutral empty-person dialogue, and Finding/Design reveal have not yet been rechecked together after the latest refactor | complete the manual loop in a restarted development client and record the result in a later diary entry |
| P2 | integrated QA | full knowledge snapshot login/team-switch/reload plus new-result and legacy-overlap access needs real-client and dedicated-server coverage | the client projection, recipe execution, IE formation, RightClickBlock and JEI agree after every full replacement |
| P5 | future feature | `upgrade_prototype` is identity-only and cannot install or contribute | implement profile semantics, fabrication workflow, host storage, sockets, deduplication and host-aware resolver before exposing installation |

## Validation Commands

The maintained automated baseline is:

```text
./gradlew --no-daemon clean compileJava compileTestJava
./gradlew --no-daemon test
./gradlew --no-daemon runGameTestServer
./gradlew --no-daemon runGameTestServer -PwithoutFtb -PwithoutJei
./gradlew --no-daemon build validateResearchCatalog
git diff --check
```

Use a disposable world for the remaining migration and multiplayer matrix. Do not run destructive migration scenarios against a production world.
