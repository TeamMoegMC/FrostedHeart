# Generative Research V2 Phase 2 geology vertical slice

- Time: `2026-08-26 14:55:45 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Frosted Research V2 evidence, ideas, manual comparison, knowledge-laboratory UI, geology Finding/Design resolution, person knowledge, companion topic/recipes, tests, plan, and docs/research`

## Completed

- Added the non-stackable `frostedresearch:research_notebook`. It archives copper outcrops and stone samples with dimension, block, position, time, observer provenance, and a semantic 16-cubed section identity. Repeated observations merge instead of creating duplicate records.
- Extracted the read-only `OreProspectingModel` from the prospector pick and geologist's hammer. The tools retain their prior half-open ranges, ore-tag behavior, durability, and message noise; notebook samples seal the same horizontal-4, vertical-3 mineral snapshot without projecting its counts to clients.
- Upgraded `TeamKnowledgeData` to schema 2 with observations, ideas, comparison artifacts, and four persistent entitlement sets. The persistence codec retains sealed facts, while the network codec and `KnowledgeProjection` expose only safe summaries, at most three prioritized action cards, reports, and acquired Findings.
- Extended drawing desks with five per-block evidence pins, organization intent, legacy/inspiration game purpose, and a candidate idea. The old clue path still commits legacy game levels. The V2 inspiration path consumes the existing paper/ink cost, revalidates pins when the game finishes, and requires an explicit “record this idea” action before the team idea is changed.
- Added the full-window Knowledge Laboratory surface with an observation inbox, evidence board, action cards, idea/report state, and resolution controls. The rock-and-ore-signs workflow requires an outcrop, a nearby sample whose sealed scan volume covers it, and a control sample from a different horizontal section.
- Added deterministic manual comparison outcomes. Only `MATCH` readies the idea for acceptance; `NO_MATCH` and `INSUFFICIENT` remain preserved reports and direct the player toward more evidence. Acceptance atomically grants `frostedheart:prospecting_signs_indicate_nearby_ore` and `frostedheart:copper_prospecting_pick` once.
- Added registered Finding view handlers. The geology Finding reveals only a coarse trace/no-clear-trace interpretation on sample summaries; the prospector pick remains the detailed immediate tool.
- Added persistent `PersonKnowledgeOverlay` data for refugees and residents. Eligible refugees initialize once, including empty outcomes; experienced people can teach the same idea without bypassing comparison. Recruitment copies the overlay before attempting town insertion and removes the refugee only after success.
- Added the formal companion-pack geology topic, localized text, the stable copper-pick recipe ID `the_winter_rescue:research/copper_pro_pick`, and the stable early notebook recipe `the_winter_rescue:research/research_notebook`. Existing index-derived recipe IDs were not shifted.
- Updated the living research documentation and marked Phase 2 complete in the V2 plan.

## Decisions

- Phase 2 remains a player-operated workflow. Resident computation, shifts, and research-institute queues remain Phase 3 work.
- Legacy `geology_understanding` coexists with the formal topic and is not migrated into a Finding. Legacy access continues to be an independent copper-pick recipe provenance.
- Missing formal topics preserve observations, ideas, reports, and entitlements as recoverable orphan data. Reload rejection retains the previous valid catalogue.
- Server-sealed mineral counts never cross the knowledge synchronization boundary. Finding handlers receive projections instead of reading world NBT or implementation fields.

## Validation

- `./gradlew test` passed the full unit-test suite, including schema migration, semantic deduplication, sealed-fact network redaction, idea-source merging, action ordering/limit, comparison outcomes, person-overlay boundaries, and workflow codec coverage.
- `./gradlew cleanResearchGameTestWorld runGameTestServer` passed all `18` required GameTests, including the complete geology vertical slice and legacy coexistence assertions.
- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei` passed the same `18` required GameTests.
- `./gradlew validateResearchCatalog -PresearchCatalogDir="<companion>/config/fhresearches"` validated all `81` legacy companion definitions.
- `./gradlew build` passed. It reported only the repository's existing license-header and duplicate-resource warnings.
- Companion topic/language JSON parsing, `node --check` for both changed recipe scripts, and separate `git diff --check` checks passed.

## Remaining

- Full client interaction still needs an in-pack manual pass for the Knowledge Laboratory layout, paper/ink consumption, KubeJS runtime recipe IDs, Finding text, and JEI/real crafting visibility after Design acquisition.
- The previously recorded JEI revoke-visibility issue remains outside this phase and was not reopened.
- Phase 3 owns resident calculation work orders and research-institute scheduling.
