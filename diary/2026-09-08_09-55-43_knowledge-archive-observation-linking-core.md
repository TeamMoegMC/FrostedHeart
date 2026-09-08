# Knowledge archive, observation and linking core

- Time: `2026-09-08 09:55:43 +0800`
- Author: `Codex; OpenAI GPT-6; primary implementation agent with three parallel implementation agents`
- Status: `completed`
- Scope: `frostedresearch/knowledge`, minimal result/desk/network bridges, knowledge note assets, docs/knowledge

## Completed

- Implemented the core described by the new read-only [knowledge design](../design/knowledge_research.md), without
  reading or using old plans.
- Replaced acquired-result-set authority with one team archive and inbox, immutable observation identities/snapshots,
  independent definitions, learning/forgetting events, understanding checks, dormant-state queries, explicit
  initial/command grants and explicit team merge.
- Added typed full unordered 2–5 input linking, actual first bindings and alternative paths,
  observation-use/independent-count-aware difference checks, staged hints, daily sleep/discussion opportunities and
  research provenance APIs.
- Added R-key world observation, locked server-timed sampling and preview; physical item retrieval; two-paper blank
  research notes; immutable copies and original-source preservation; atomic note export. Wheel menu default moved to G
  to keep both defaults usable.
- Added drawing desk knowledge browsing, type/subtype/status/search filters, expandable record groups, batch
  learning/deletion, explicit link-output selection, known/historical relation views, sleep theme and mutually confirmed
  player discussion.
- Preserved the existing research workflow and downstream access integrations while projecting effective archived
  results. Existing physical prototype fabrication remains separate from idempotent Prototype knowledge.
- Implemented compressed 128 KiB snapshot fragments installed atomically, and sequential 128-key requests with one
  captured batch selection and combined per-entry feedback.
- Added [knowledge documentation](../docs/knowledge/README.md),
  a [loadable example datapack](../docs/knowledge/example-datapack/README.md), updated the system index and corrected
  affected knowledge paragraphs in four existing research documents.

## Decisions

- New production code lives under `knowledge/` wherever possible. The existing `data/TeamKnowledgeData` component
  address is a thin bridge to schema-2 `KnowledgeState`; no old knowledge save migration was implemented, as requested.
- Learning requirements do not become continuing validity requirements. Forgetting is non-cascading, and deleting a
  producing rule does not erase knowledge definitions or history.
- Observations are only collapsed when current registered uses establish substitutability without losing needed
  independent records. Unknown equivalence preserves records.
- Static idea/result content and project declarations are datapack-authored. The example is documentation/test content,
  not automatically shipped progression or balancing content.
- The new research execution system, resident work scheduling/facilities and numeric prototype upgrades remain outside
  this core, as explicitly deferred by the design. Integration APIs and actual research history are present.

## Validation

- `./gradlew compileJava` passed; the final focused test run also recompiled the final main and test sources
  successfully.
-
`./gradlew test --tests 'com.teammoeg.frostedresearch.knowledge.link.LinkMatcherTest' --tests 'com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitionsTest' --tests 'com.teammoeg.frostedresearch.knowledge.item.ResearchNotesTest' --tests 'com.teammoeg.frostedresearch.knowledge.KnowledgeServiceTest' --tests 'com.teammoeg.frostedresearch.knowledge.network.KnowledgeSnapshotTransferTest'`
passed **27 tests**, zero failures/errors/skips.
- Tests cover unordered matching, identity/multiplicity, distance/source/unknown fields, atomic bad-pack rejection,
  example loading, observation immutability, event cancellation and idempotence, understanding/dormancy, overflow merge,
  actual link/research histories, progressive daily hints, note identity/origin and item exclusion, and >5 MiB
  compressed snapshot reassembly.
- Plain JUnit requires starting the Forge event bus and materializing event listener lists because it does not execute
  FML's transformed startup; the fixture now follows that lifecycle and checks real cancellation callbacks.
- `git diff --check` passed; all local links in `docs/knowledge` resolve and example JSON parses.
- Companion repository: no edits by this work. No companion `AGENTS.md` was found at the repository or checked instance
  parents. Its pre-existing `config/wheelmenu-visibility.json` modification remains untouched.
- No old research regression suite or game-client visual/multiplayer acceptance run was performed; the user's requested
  focus was the new core and architectural replacement.

## Remaining

- In-game visual and multiplayer acceptance of the new UI/observation/notes flow.
- Production content authoring; downstream research-task execution, resident facilities and prototype numerical behavior
  await their later designs.
