# Generative Research V2 Phase 2 generalization pass

- Time: `2026-08-26 17:24:59 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and documentation team`
- Status: `partial`
- Scope: `topic-free observations, executable research workflows, Knowledge Laboratory disclosure, person knowledge packages, geology plugin separation, client knowledge synchronization, plan and docs/research`

## Completed

- Changed `research_notebook` from a geology-only gate into an observation channel for any block. The generic fallback records subject, visible state, dimension, exact position, time, observer, and channel; `ObservationProviderRegistry` lets a higher-priority content provider choose kind, facets, semantic deduplication, and sealed facts.
- Moved geology observation registration into Frosted Heart's `GeologyResearchIntegration`. Copper and stone still retain their compatibility types and `16³` regional keys, while stone seals the shared horizontal-4/vertical-3 `OreProspectingModel` snapshot. Generic block records use exact position and canonical visible state instead.
- Replaced the workflow registry's validation-only ID sets with executable `IdeaSourceHandler`, `ProtocolHandler`, `ResolutionHandler`, and `FindingViewHandler` registrations. `TeamResearchService` now delegates candidate matching, action compilation, protocol execution, and result resolution through those handlers.
- Made drawing-desk inspiration sessions hold up to three candidates. Matching happens before paper/ink is consumed, pending candidates remain server-only during the card game, completion revalidates them, and the player chooses a candidate before an `IdeaRecord` is created or merged.
- Changed `KnowledgeProjection` and the Knowledge Laboratory to show generic observation summaries, a newest-first scrollable inbox with actual block names/positions, simple visible relations, post-Idea method buttons, reports, candidates, and acquired Finding counts. Executable method buttons address an exact topic/protocol pair and candidate choices temporarily own the worksheet area. Loaded topic goals and geology task cards are no longer projected before an Idea exists.
- Kept geology as the first registered content workflow. An outcrop plus nearby rock can produce the Idea before a distant control is collected; comparison and resolution remain post-Idea and still award the geology Finding plus copper-pick Design together.
- Generalized person dialogue around persistent knowledge-package IDs. `PersonKnowledgePackageCatalog` is now a generic registration surface and Frosted Heart's `PersonKnowledgeIntegration` supplies the prospecting and non-geology cold-weather definitions. The shared experience action also supports an empty outcome; only a package-specific reply names its domain. Refugees and residents share the same catalogue/dialogue path and recruitment keeps the initialized overlay without rerolling.
- Made missing topic/Idea declarations recoverable: a direct offer is recorded as `IdeaRecord.State.ORPHAN`, produces no action/result authority, and can merge back into an active Idea when the same stable definition and source are recorded again.
- Moved full knowledge-snapshot installation to the client-only `ClientKnowledgeSnapshotHandler` and changed the physical-side dispatch to `DistExecutor.unsafeRunWhenOn`. Removed the invalid `frostedresearch_test:smoke_view` reference that kept the current debug catalogue at revision zero.
- Added a non-geology test-resource topic using the generic `record_pair`, `compare_records`, and `comparison_resolution` handlers. This fixture exercises a furnace block-state Idea without adding a topic-specific drawing-desk or person branch.
- Updated the research living documentation and the main V2 plan to describe the implemented generic path and its remaining limits. The work intentionally evolves schema 2 in place during rapid iteration instead of creating a schema-3 migration project.

## Decisions

- Observation records do not belong to topics. A record that matches no current topic remains valid history and may become useful after a later datapack reload.
- Topic definitions provide stable handler IDs; bounded registered handlers provide executable meaning. The common drawing desk, projection, and service do not own the geology evidence recipe.
- Idea discovery and formal research are separate disclosure phases. Before an Idea, the player sees records and neutral relations; action cards appear only from persisted Ideas, and Finding views appear only after acquisition.
- The current generic foundation is intentionally small. The visible relationship summary is not yet a complete typed `EvidenceRelation` engine, Frosted Heart's current package definitions are bootstrap-registered and dialogue shares the first available entry, and compatibility wrappers still leave some Frosted Research code coupled to `OreProspectingModel`.

## Validation

- Earlier concurrent Gradle processes shared output directories and were discarded as evidence. The parent implementation task then ran `./gradlew --no-daemon clean compileJava compileTestJava` and `./gradlew --no-daemon test` serially; both completed successfully.
- `./gradlew --no-daemon runGameTestServer` and `./gradlew --no-daemon runGameTestServer -PwithoutFtb -PwithoutJei` each reported `All 18 required tests passed`. The task already depends on `cleanResearchGameTestWorld`, so no separate clean task was added to those command lines.
- `./gradlew --no-daemon build validateResearchCatalog` completed successfully and the catalogue preflight validated all `81` companion definitions. The build printed the repository's existing broad license-header warnings but did not fail.
- Final whole-worktree `git diff --check` passed after the implementation and documentation edits.
- Documentation was checked against the current Java/data resources. Restarted-client acceptance remains separate from these automated results.
- Restarted-client behavior has not yet been reverified after the synchronization and generalization changes. In particular, the existing four saved records have not yet been observed in the repaired inbox path.

## Remaining

- Restart the development client and verify catalogue revision, existing-record inbox population, arbitrary-block notebook feedback, no topic leakage before Idea, candidate choice, post-Idea distant-control comparison, neutral empty-person dialogue, and Finding/Design presentation/access.
- Decide in a later rapid iteration whether to add a typed reusable `EvidenceRelation` layer, context-specific person follow-up/offer choice, and a geology-independent sealed-fact envelope. These are not prerequisites for claiming that the present source changes already exist, but they remain prerequisites for the broader architecture described by the long-term plan.
- This entry supersedes the earlier claim that Phase 2 was fully completed; Phase 2 remains `in-progress` until the pending restarted-client validation is recorded.
