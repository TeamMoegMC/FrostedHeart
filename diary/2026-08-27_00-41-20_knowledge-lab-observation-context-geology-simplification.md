# Knowledge Lab archive, timed observations, and elementary geology loop

- Time: `2026-08-27 00:41:20 +08:00`
- Author: `Codex; OpenAI GPT-5; implementation and documentation team`
- Status: `partial`
- Scope: `Knowledge Laboratory UI/projection, drawing-desk inspiration sessions, research notebook capture, ObservationContext, geology Phase 2 fixture, tests and research living docs`

## Completed

- Rebuilt the Knowledge Laboratory as three navigable archive pages for every client-safe team observation, Idea, work artifact, and acquired Finding/Design/Construction/Procedure. Lists scroll over the complete synchronized projection, details retain orphan IDs instead of hiding them, and shared layout/button geometry keeps rendering, hover, and clicks aligned.
- Removed the active-game forced redirect. Players may enter the laboratory while the V2 card game is active, inspect any archive page, and return to the same card state. The card page identifies the frozen evidence set, the laboratory exposes an explicit cancel action, and reopening the game no longer consumes paper or ink again.
- Extended observations with retained context facts and block/living-entity targets. The notebook now uses a server-validated `40`-tick capture with HUD progress, cancels incomplete or invalid captures, and cycles `STANDARD`, `COMPACT`, and `ENVIRONMENT` retention profiles. The environment profile records `WorldTemperature.block` only when a soil thermometer is carried.
- Added a client-safe `KnowledgeLabProjection` alongside the smaller ambient projection. Full snapshots install both atomically; sealed mineral counts remain absent from the network while public subject, position, state, context, facets, annotations, Idea state, artifact summary, and result target information remain browsable.
- Simplified the first geology fixture to any recorded `forge:stone` plus any recorded `forge:ores`, in any order and at any distance. Finishing the card game automatically records a sole matching Idea; one lightweight theory action marks it ready, and acceptance grants the existing Finding and copper-prospecting-pick Design without a control sample or new comparison artifact.
- Updated the research plan and living documentation to distinguish the implemented local full projection/profile-based capture from still-planned server paging, context chips, draft review, typed context values, and the non-geology context fixture.

## Decisions

- Knowledge Lab is the presentation of `TeamKnowledgeData`, not a geology worksheet. Observations, Ideas, artifacts, results, and recoverable orphans each need a stable route independent of the currently selected topic.
- An active drawing-desk game is a persistent session, not a modal page lock. View navigation must not mutate the game, pins, or paid resources.
- The introductory geology topic demonstrates direct Idea discovery rather than controlled experimentation. Old comparison artifacts remain readable for development-world continuity, but they are not a gate and no replacement artifact is fabricated.
- During rapid iteration, context was added to the current codecs without a schema bump. The first UI uses explicit retention profiles and automatic save so the playable loop can be tested before committing to the final context-chip/draft interaction.

## Validation

- `./gradlew compileJava --rerun-tasks --no-daemon` completed successfully after the UI, observation, and workflow changes.
- `./gradlew compileTestJava --rerun-tasks --no-daemon` completed successfully.
- Focused observation, knowledge-snapshot, layout, and Phase 2 data tests completed successfully.
- `./gradlew test --no-daemon` completed successfully.
- `./gradlew runGameTestServer --no-daemon` completed successfully and reported `All 18 required tests passed`.
- `./gradlew runGameTestServer -PwithoutJei -PwithoutFtb --no-daemon` completed successfully.
- `./gradlew build validateResearchCatalog --no-daemon` completed successfully; catalogue preflight validated all `81` research definitions.
- Language/topic JSON parsing and whole-worktree `git diff --check` completed successfully before this diary was appended; the final checks were repeated after documentation cleanup.

## Remaining

- Restart the development client and visually verify GUI scales, hover/legibility, all three archive pages, laboratory/card round trips, one-time resource cost, timed block/entity capture, profile switching, and the complete elementary geology result path.
- Add per-field context chips, capture draft save/discard, authoritative Frosted Heart climate-event fields, target-change/instrument-removal capture cancellation, and bounded server-side knowledge paging/search.
- Replace string context facts with the planned typed field/value contract, remove the remaining core `OreProspectingModel` compatibility coupling, and add the sheep/evening/tundra non-geology context fixture before freezing the public authoring schema.
