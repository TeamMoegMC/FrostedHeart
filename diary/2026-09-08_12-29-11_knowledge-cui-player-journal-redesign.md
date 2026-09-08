# Knowledge CUI player journal redesign

- Time: `2026-09-08 12:29:11 +0800`
- Author: `Codex; OpenAI GPT-6; primary design and implementation agent`
- Status: `completed`
- Scope: `knowledge client UI/presentation, drawing desk CUI integration, note display, sample prose, knowledge docs`

## Completed

- Read the eight user screenshots from `2026-09-08_11.07.52.png` through `11.13.49.png` and designed the interaction
  in [the new UI plan](../plans/2026-09-08_11-33-10_knowledge-cui-journal-experience.md) before implementing.
- Replaced the standalone vanilla knowledge widget screen with independent `KnowledgeLayer` inside the existing
  `DrawDeskScreen` Chorda container. Original research archive and knowledge remain separate implementations.
- Added paper-bookmarks, a virtualized note index, readable right-hand pages, explicit inbox selection mode and removal
  confirmation, a comparison tray, candidate thoughts, local relationship pages, reflections and an integrated copying
  desk using existing inventory slot positions.
- Converted observation progress/record previews to Chorda `PrimaryLayer`/`CUIScreenWrapper`; retained locked sampling,
  cancellation and live-world behavior. Cancellation follows the actual configured observation key (currently default
  N), and pending accept state survives detail expansion.
- Added `KnowledgePresentation`/`ObservationPresentation` for localized registry names, combined date/place, qualitative
  conditions and optional precise details. Removed raw UUID/registry ID/NBT/tick text from ordinary player views and
  note names/tooltips.
- Reused existing `draw_desk.png`, `escritoire.png`, paper, borders and slot graphics. No new bitmap asset is required.
- Changed example titles/body/hints to translation keys with separate Chinese/English content. Updated only
  corresponding prose fields in the installed `run/saves/knowledge-test/datapacks/example-datapack`; knowledge
  definitions/conditions and saved team history were not migrated or rewritten.
- Removed the server's unused raw field-summary string from the visible snapshot. Client indexes, translated search,
  label wrapping and local graph geometry are rebuilt on data/language/layout changes; draw work is limited to visible
  rows/text/current relation.
- Updated [workbench documentation](../docs/knowledge/workbench.md), knowledge observation/definition/README docs, and
  the drawing desk integration description in `docs/research/research-ui.md`.

## Decisions

- Reading and selecting are separate actions. Bulk controls appear during organization; comparison controls appear after
  adding notes. This avoids an always-visible technical command panel.
- Keep the existing Chorda container and slot IDs rather than adding another inventory implementation or altering slot
  positions. The copying leaf exposes the useful slots, and the legacy ink slot returns with the original desk.
- Use one virtual list and one cached paragraph renderer instead of an element per archived record. Buttons use
  nine-slice frames instead of repeated 4-pixel tiles.
- Ordinary text omits identity/debug fields while the original record remains available to game rules. Unknown
  measurements are omitted, not displayed as zeros.
- Optional future art is three 16×16 observation/idea/result classification stamps, normal/selected states. Existing
  objects, feather and paper work as placeholders.

## Validation

- Final `./gradlew compileJava` passed.
-
`./gradlew test --tests 'com.teammoeg.frostedresearch.knowledge.client.*' --tests 'com.teammoeg.frostedresearch.knowledge.item.ResearchNotesTest' --tests 'com.teammoeg.frostedresearch.knowledge.definition.KnowledgeDefinitionsTest'`
passed **16 tests**, zero failures/errors/skips. Seven new UI/presentation tests cover localized names and prose, hidden
technical identifiers, unknown measurements, note names, snapshot indexing, actual empty CUI construction, narrow/wide
layout bounds and five-input relation geometry/scrollable overflow.
- `git diff --check` passed. Local knowledge-document links resolve; Chinese and English journal translation key sets
  match.
- The updated development client successfully started with the local GLFW library. The window-control tool timed out and
  resolved Minecraft to installed launcher applications instead of exposing the Java game window; no updated gameplay UI
  screenshot or visual acceptance was obtained. The client process started by this task was stopped afterwards.

## Remaining

- In-game visual and interaction acceptance of the new journal and observation screens on the user's client.
  Compilation/layout tests are not reported as visual acceptance.
- Existing saved hints retain their old literal history text; newly authored example hints use translated player prose.
