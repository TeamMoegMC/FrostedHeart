# Knowledge interaction fixes and research graph loop

- Time: `2026-09-08 17:35:52 +0800`
- Author: `Codex; OpenAI; primary implementation agent`
- Status: `completed`
- Scope: `knowledge observation, inbox/hints, CUI graph/copy desk, temporary research command and example datapack`

## Completed

- Addressed the user's eleven screenshots from 15:28 through 16:10: copy desk feedback/current note, disabled-action
  explanations, localized groups with indented branch guides, raised observation prompt, and a complete graph reading
  surface.
- Item inbox acquisition now deduplicates recorded item type/data independently of RecordId and source. The UI waits for
  replies and marks a matching recorded input; research notes contribute their contents and cannot become item
  observations.
- Replaced repeat-click observation with a held-key gesture. Holding starts once; releasing cancels, including delayed
  active-completion responses. Start accepts the outlined target within the player's actual reach instead of casting
  toward a block center; extended reach remains usable. Indirect preview is separate.
- Saved hints deduplicate globally by key/text. Loading/reloading reconciles old literal stages with current translated
  hints; revisiting a known thought in a dream consumes the day opportunity without another stored copy.
- Added independent Chorda pan/zoom graph and floating node sheets. Declared projects disclosed by learned ideas appear
  before execution with planned edges; actual research and link records remain distinct. Opening and closing a sheet
  preserves the graph camera.
- Graph topology/ranking is computed on data changes with O(V+E) traversal and stable cycle breaks. Rendering clips
  offscreen nodes/edges, caches visible labels and pointer hits, and does not run a force simulation.
- Added `/knowledge research complete <project> [idea]` for development. It requires an active originating idea, records
  a real run, calls normal result learning and batches the completion snapshot.
- Expanded the example to 9 ideas, 9 projects, 17 results and 12 link rules, including branches, convergences, two
  return paths, recipe access, block operation and IE formation outcomes. Synced the example namespace into the
  installed knowledge-test datapack.
-
Updated [workbench](../docs/knowledge/workbench.md), [observation](../docs/knowledge/observations-and-notes.md), [state/API](../docs/knowledge/state-and-api.md)
and [playthrough](../docs/knowledge/playthrough.md) documentation.

## Decisions

- Compare actual captured item data, not quantity or new observation UUIDs. Existing distinct observation records are
  not silently deleted.
- Display project declarations without inventing completed research history; the temporary command skips tasks only and
  retains ordinary team/result semantics.
- Keep prior CUI local relation rendering for a connection's floating sheet; the overview uses Chorda PanZoomViewport
  and does not depend on the legacy research archive implementation.
- Following the user's tool feedback, remaining source/data edits used apply_patch. No new tests were added; only the
  existing example-count expectations changed.

## Validation

- Final `./gradlew compileJava` passed.
- Existing `KnowledgeServiceTest`, `KnowledgeDefinitionsTest`, `knowledge.client.*`, and `ResearchNotesTest` checks
  passed: 26 tests, no failures/errors/skips.
- `git diff --check` passed.
- Supplied screenshots were inspected; this turn did not perform a new live client UI session.
- No binary player/team save was manually rewritten. Hint reconciliation occurs through the normal knowledge
  initialization/reload lifecycle.

## Remaining

- In-game acceptance of the held gesture, copy-desk feedback, floating graph sheets and expanded research route.
- The actual research task execution remains deferred; the new completion command is explicitly temporary.

