# Research graph readable field overview redesign

- Time: `2026-10-24 02:22:00 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `partial` (superseded by the user-selected continuous full-tree zoom redesign)
- Scope: Research archive semantic zoom, field overview, client validation and localization

## Completed

- Replaced the earlier low-zoom marker/leader-line presentation with `ResearchGraphOverview`: existing research fields, localized names, completed/total counts, completion bars, available/active counts and named project previews with native-size item icons. Wide views show all populated fields; narrow views paginate without shrinking text or icons; a single field uses more space for previews.
- Retained 16 by 16 GUI-pixel item icons and normal font size in both compact and full project cards. Project names remain inside the cards. Removed `ResearchGraphLabelLayout`, detached labels, global label searching, pinned previews and all label leaders.
- Preview clicks select and focus the original project at at least 100% zoom. Field-header clicks apply the existing field filter and focus a project at readable zoom. Wheel transitions out of overview enter the hovered project; ordinary graph wheel input retains its pointer anchor. Panning operates on the individual graph.
- Overview statistics use projected primary projects only; prerequisite context is not counted twice. Search statistics and previews include matching projects only. Existing archive discovery/hidden-definition filtering remains the input boundary.
- Kept the shared shape pass, `CIconBatch`, graph geometry/style caches and reusable node/edge buffers. The overview caches counts, clipped names, priorities, page geometry and hit records by revisions and presentation inputs. Hover does not invalidate it. No per-project CUI widgets or offscreen targets were introduced.
- Retained the earlier verified localized-component parser fixes and screen-adapter origin correction.
- Updated the opt-in real-client driver to check usable information, native icon sizes, names and state counts; preview location, project dialogs, overview/graph wheel behavior, narrow pagination, field entry, a single-field view, search/no results, an empty graph, filtering and static/hover cache reuse.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) to describe this implementation. The [earlier diary entry](2026-10-24_01-59-10_research-graph-semantic-zoom-readable-labels.md) records the now-rejected marker/leader approach and is preserved as history.

## Decisions

- Below the space required by a readable individual card, summarize actual fields instead of removing project identities. Aggregation makes the number of represented projects explicit; named previews are navigation entries, and the existing project index still contains every matching project.
- Reuse existing categories and visibility/state caches. Stable priority passes select previews only when the snapshot changes. No new graph clustering model or research-state rules are needed.
- Use the current checkout, ordinary dependencies and Gradle outputs. Defer all executable validation to one final real-game batch; no JUnit or standalone model tests.

## Validation

- First unified batch passed the production catalogue (86 definitions), all four required research/login Forge GameTests and ten client display scenarios. Wide overview showed five fields, ten named previews and all 86 projects represented; static geometry/overview rebuild counts were zero. Input validation then failed because the unfocused window did not deliver the requested cursor move. The driver now invokes Minecraft's normal mouse-move callback.
- A second build stopped at a missing `ChunkGeneratorStructureState` import in the workspace's hydrothermal mixin; only that import was added. A third build compiled but the GameTest server encountered `NoClassDefFoundError: QueryPublication$MutableSample` while discovering unrelated thermal tests; that class is present in current source and outputs. These runs do not validate the final continuous implementation.
- The user rejected abrupt presentation switches and selected full-tree preservation with moderately shrinking real icons. The overview implementation was removed rather than retained as a second automatic view. Current behavior is documented in `research-ui.md` and the newer continuous-zoom diary entry.

## Remaining

- Superseded. Continue validation with the continuous full-tree implementation; the field-overview design is not current behavior.
