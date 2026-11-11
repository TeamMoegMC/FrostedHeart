# Town UI: notice-board theme and the research institute sample

- Time: `2026-11-10 00:31:15 -0600`
- Author: `Claude; Anthropic/claude-opus-5-5; town UI implementation`
- Status: `in-progress`
- Scope: `chorda/client/cui` (transform hit-testing, `ProgressBar`, tinted icons, UI sounds, stale-focus guard), `content/town/ui`, `content/town/client/board`, every building screen under `content/town/buildings` (view, presenter, screen, menu), `item/townmanager` (the Mayor's Seal), town lang files, `Scripts/gen_town_ui_textures.py`

## Completed

Phase 0 (most of it) and phase 1 of [the town UI plan](../plans/2026-11-09_21-45-44_town-ui-redesign.md):

- **CUI.**
  - Fixed the mouse mapping of transformed layers. `UILayer.updateRenderInfo` pivoted on the parent's origin instead of the layer's own position, so only layers at (0, 0) were hit-tested correctly. `ArchiveScreen` was unaffected because its transform sits on the primary layer.
  - Added `ProgressBar` and `Theme.drawProgressBar` (flat default).
  - Added `CTextureIcon.drawTinted`, for nine-slices too; plain `draw` resets the shader color, so a prior tint was lost.
  - Added `CInputHelper.playUISound`.
  - Documented in [chorda-cui.md §7.1–7.2](../docs/chorda-cui.md).
- **Number rules.** `TownFormat`: hands rounded down with "不足 1", efficiency percent, progress capped at 99 until done, stars, temperatures that round toward failing and never across a bound, frost levels, light levels, chance words and meter, compact counts, stock days.
- **Detailed mode.**
  - `/town ui detailed [on|off] [players]` needs permission level 2.
  - The switch lives in the player's persisted data. It is cleared at login without the permission.
  - It reaches the client in `TownUiModeS2CPacket`; the FH protocol gained `town-ui-1`.
- **Theme and art.**
  - `NoticeBoardTheme` and `NoticeBoardIcons`.
  - 17 deterministic placeholder textures from `Scripts/gen_town_ui_textures.py`; the size and nine-slice table is at the top of the script.
- **Components.**
  - `PaperCard`: paper, note or grid, pin or tape, a slight CUI-transform tilt, frost.
  - `HitArea`, `PhotoCard`, `PlaqueTab`, `ScrollColumn`.
  - `NoteStack`: improvements first, flip by click or wheel, the 新 stamp, a corner hint.
  - `BoardDraw`: text, stars, chips, stamps, the town seal; Latin town names use word initials.
  - `NoticeBoardBuildingScreen`: window-sized board, plaque tabs, a red debug plaque in detailed mode, and rebuilds that keep UI state.
- **Research institute.**
  - `InstituteView` plus the pure `InstituteViewPresenter`.
  - `ResearchInstituteScreen` now has overview, researchers, joint research and association tabs. The debug tab keeps the original rows and can copy them all.
  - The menu no longer adds the player inventory.
  - About 210 zh/en strings were written to the copy rules; `ResidentNutritionExplanation.limitingNutrients(resident, max)` was extracted for the notes.
- **Docs.**
  - [town-model.md §24.5](../docs/town/town-model.md).
  - [chorda-cui.md](../docs/chorda-cui.md).
  - The plan's progress section.

## Decisions

- The presenter is common code and returns whole numbers and text only. GameTests can then assert the "no decimals" rule on everything the player view would show, with translation arguments flattened.
- Building numbers follow position order for now. Persistent numbering, direction labels and renaming need the town-data table and a packet, so they are a separate step.
- The debug tab reuses the old `TownInfoPanel` and `TownWorkforcePanel` instead of re-theming them. The data stays exactly as before, and the dark panels make it obvious that the view is technical.
- In-game checks used a client with its own game directory (`-I <scratch>/board_client.init.gradle` setting `workingDirectory`). The owner's own dev client was running from the shared `run/` and was left alone.

## Validation

- `./gradlew compileJava compileGameTestJava`: success.
- `runGameTestServer` for `frostedheart_town_ui,frostedheart_institute,frostedheart_buildings,frostedheart_town_view`: `All 34 required tests passed`. The three new UI tests cover the number rules, the institute view (no decimals anywhere, note priority, frost and status), and the detailed mode's permission and clearing.
  - The first attempt used `makeMockServerPlayerInLevel`. Its full login runs other mods' handlers that send packets over a channel-less connection, so the test now uses `FakePlayerFactory`, like the other suites. `TownUiMode.sync` also skips players without an open connection.
- In game at three GUI sizes: 853×480 (scale 3, board at its 560×340 maximum), 640×360 (scale 4) and the 427×240 minimum (scale 6). Every tab fit without scrolling at the minimum. Fixes made after looking:
  - Cards now follow their content: the conditions and research notes are split, and the personnel card and association notes size to fit.
  - The note signature sits clear of the folded corner.
  - A Latin town name gives word initials on the seal.
  - Level-0 education reads "不要求学历".
  - The home line reads "住在…，暖和".
  - The association idle line only shows when someone thought.
  - The plaque column is part of the centered layer, so the plaques stay on screen.
- In game at 853×480 before those fixes:
  - All five tabs rendered.
  - The detailed-mode command answered "Dev 的城镇界面详细模式：开", and the red plaque and debug rows appeared.
  - Note-stack flip, tab switching and the association links are wired; flipping was not screenshotted.

## Second round: every town screen

The second round moved the house, warehouse, mine base, mine, hunting base, transport station and the Mayor's Seal onto
the notice board.

- **Shared work board.**
  - `WorkView`, `WorkPresenters` and `WorkBoardScreen` give the three staffed production buildings the same overview,
    crew and output tabs.
  - The mine base adds a mines tab.
  - Each building supplies only its presenter, icons and the original text panels for the debug tab.
- **Seal.**
  - The seal keeps its tab order, so `TownManagerClientHelper` links still land on the right page.
  - Overview, residents, buildings, laws and events are new board pages.
  - The building cards call each building's own presenter, so the seal and the building's board never disagree.
  - Services, staffing, housing and statistics are the original panels hung on a `Chalkboard`, which keeps their
    drag-and-drop and sliders.
  - The chalkboard shrinks through a CUI scale transform when the window is small.
- **Warehouse pack slots.**
  - Vanilla `Slot` positions are final, so `WarehouseMenu` places the pack at fixed screen-layer coordinates in the
    board's top-left.
  - The ledger takes whatever width and height remain.
- **Focus bug.**
  - Typing into the warehouse search box lost characters.
  - The board rebuilds every few seconds, and `PrimaryLayer` kept routing keys to the detached old text box.
  - `PrimaryLayer` now drops focus from elements no longer in the tree, and the warehouse hands focus to the new box.

### Second-round validation

- GameTests:
  - `frostedheart_town_ui` (7) and `frostedheart_institute` (7) pass.
  - The new tests cover the house, warehouse, three work buildings and the seal's building list, each checked for
    decimals.
- In game at 640×360 GUI (2560×1440 framebuffer, scale 4), in the run-board copy of `institute-fresh`:
  - **House:** overview, residents, meal and debug tabs. The 79 °C test house reads 太热了, because the copy allows
    200 °C.
  - **Warehouse:**
    - Stored a stack by clicking the ledger, then took it back out.
    - The search filters, and an unknown name shows 没找到「…」.
  - **Work buildings:** built test rooms for a mine base, hunting base, transport station and mine, then ran
    `/town tick`. After the tick, all four boards showed workers, yield, deposits and capacity.
  - **Seal:** all ten tabs opened; the laws, buildings and residents pages were checked by eye.

## Third round: names, the seal's panels, and every action in game

- **Building names.**
  - Numbers are kept per type: a new building takes the smallest free number, and nobody is renumbered.
  - The number and an optional custom name ride in each building's own codec through `AbstractTownBuilding.named`,
    which wraps every `MAP_CODEC`. The plan had proposed a separate table in the town data. The codec route needs no
    extra bookkeeping on removal, and both persistence and sync get the fields for free.
  - Directions follow the plan: they are measured from the energy tower, and within 16 blocks of it a building is 镇中心.
  - The client never had the tower's position. The server now copies it in `prepareView`, sends it with the full
    snapshot, and sends `TownTowerUpdatePacket` when it changes.
  - Coordinates are gone from player views.
- **Seal panels.**
  - Services, staffing, housing and statistics are drawn as paper strips with brass-pin sliders (`BoardQueue`).
- **Portraits.** They are pixel busts generated from the resident id. Cells stay whole pixels. The grid's empty top row
  may be clipped, which lets a 48×58 photo hold a 36×42 bust instead of 24×28.
- **CUI fixes found while driving the game.**
  - Drags read the pointer from the previous frame, so a fast drag dropped a row one place short. The screen wrappers
    now refresh the pointer before dispatching a drag, as they already did for clicks.
  - `TextBox.selectAll()` and an opt-in `showGhostWhenFocused` let a rename box open with the old name selected and
    still show its fallback when cleared.

### Third-round validation

- GameTests: two new tests in `frostedheart_town_ui`.
  - The naming test covers numbering, reuse of free numbers, custom names, the codec round trip, tower-relative
    directions, no tower, and name cleaning.
  - The institute test checks that an idea names the knowledge it came from: one, two, more, or no record.
- In game at 427×240 GUI (1280×720 framebuffer, scale 3), driven through the bridge, which gained `drag x1 y1 x2 y2`:
  - **Staffing:**
    - Dragged the transport station's handle up the queue, and it moved.
    - Dragged its pin to 2 of 4, and the row turned amber for the missing hand.
    - The first try dropped one row short, which led to the drag fix.
  - **Housing:** reordered the two houses and set a two-person meal guarantee.
  - **Renames:**
    - Town: Dev's Town became 雪松镇.
    - Resident: Esc cancels, Enter renames.
    - Building: a blank name restores 住宅 1 号.
    - Each box opens with the old name selected.
  - **Hot town:**
    - With the house limit at 40 °C, the morning settlement moved all five residents out of the 80 °C houses.
    - Every page agreed afterwards: 0/5 housed, 人手 0, and the seal's chips.
  - **Cold town:**
    - With the limit raised to 85 °C, the house board frosted over and read 太冷.
    - With a 120 °C minimum working temperature, the hunting base stopped with 太冷了，手都伸不开.
  - **Laws:** the dependent-first law enacted last round shows 现行 after the settlement. Decree cards shrink to their
    wording, so the tips fit on a small screen.

## Fourth round: the leftovers, and a seal that opened by itself

- **Frost transition.** `FrostFade` remembers the frost level a board last showed. On opening it eases to the current
  level over 0.6 seconds, and so does a change while the board is open. Neighbouring overlays cross-fade, and icicles
  grow or draw back, so a thaw reads as melting. Every sheet shifts with the header.
- **Plaques slide.** `TabSlide` keeps the timing on the screen, because plaques are rebuilt with the page.
- **The seal popped up behind buildings.**
  - Six building blocks returned PASS on the client, so a held Mayor's Seal was used as well as the block.
  - The board then opened on top of the seal, and closing it brought the seal back. The house already returned
    SUCCESS, which is why the house never showed it.
  - The blocks now claim the main-hand click on the client, the vanilla pattern for menu blocks.
- **Debug pages on graph paper.**
  - `TownInfoPanel.ledger()` and `TownWorkforcePanel.paper()` draw the original rows in ink.
  - Inline colours are mapped segment by segment. A first version flattened them, and the text bars turned solid black.
  - The seal's debug tab is five raw sheets. Its old residents, buildings and laws panels, and the `Chalkboard` they
    hung on, were unused afterwards and are deleted.
- **Other screens and words.**
  - Warehouse interface readouts are whole numbers, with the actual rate per minute; the P2P terminal's transport is
    rounded too. Both keep exact figures in detailed mode. The rate players type in was already a whole number per
    second.
  - The 8 nutrition events had no words for their tips or event lines, so a tip would have shown its raw key. They have
    words now, with a GameTest over every event type, endpoint kind and admission state in both languages.
  - The transport shortage tip only gives the gap, rounded up.
  - Nutrient names are 主食, 肉食, 蔬菜 and 油脂 everywhere.
- **Association sources.** Past ideas show where they came from too, so the line can be seen without waiting for a
  lucky morning. The client has no record of the day an idea was found, and the journal only opens inside a drawing
  desk, so the list can show neither a day nor a link.

### Fourth-round validation

- GameTests: the six town suites, 54 tests, pass, including the new language-coverage test.
- In game at 640×360 GUI (2560×1440 framebuffer, scale 4):
  - **Seal fix:** with the seal in hand, right-clicking the hunting base opened its board, and Esc went back to the
    world.
  - **Frost:** frames at about 0.1, 0.3 and 1 second after opening the cold hunting base show the frost and icicles
    growing.
  - **Mine:** the board shows its 能用 and 矿藏每天回一点 chips.
  - **Transport slip:** reads 昨天跑出 155 运力，全进了城镇的账。
  - **Association:** each past idea shows a line such as 由「拉杆机关」和「传讯札记」想到的。 Two `/town tick`s
    brought the researchers back to work after the hot-town eviction.
  - **Debug sheets:** the seal's five sheets and the institute's overview and crew panel are on graph paper. After the
    inking fix, numbers keep their colours and the text bars show filled segments in amber or red against grey.
  - **Logistics:**
    - Warehouse interface: 实际速率：每分钟 1200 个 · 占用运力：28 · 城镇运力：剩余 127 / 总量 155.
    - P2P terminal: 可用运力：127 / 155.
    - Detailed mode shows the exact figures.

## Fifth round: the idea list, debug sheets everywhere, and a full sweep

- **Past ideas.**
  - When the institute finds an idea, the knowledge service records the town day in the state's day map, and the view
    sends it with the acquisitions. The list is sorted newest first and says 第 N 日.
  - Clicking an entry pins a journal excerpt in the left column: name, day, sources and the journal's description. The
    real journal lives inside the drawing desk's container, so the board shows the excerpt instead of opening it.
  - To see a real idea in game, I granted two research results with `/knowledge grant`. The next settlement produced
    交错的支撑 from 材料札记 and 承重札记.
- **Debug sheets.**
  - The crew panel sizes itself from its bounds and fills the page.
  - The house's overview, meal and residents sheets are on graph paper; `HouseDebugRows` replaces two legacy panels,
    which are deleted.
  - The warehouse's debug lines were English format strings and now have Chinese and English wording.
- **Typesetting.**
  - `BoardDraw.lines` applies the Chinese line-breaking rule. Closing punctuation hangs at the end of the line it
    belongs to, and opening brackets move down. Before, a note could start a line with 。 or 」.
  - Tips cards only show whole tips.
  - The empty crew card no longer says the same thing twice.
  - The warehouse hint fits on a small screen.

### Fifth-round validation

- GameTests: 59 pass across the six town suites, `frostedheart_translation`, `frostedresearch` and
  `frostedresearch_knowledge_login`. The institute test also checks the recorded town day and its key.
- In game at 427×240 GUI, after a two-day recovery from the hot-town eviction:
  - Every tab of every building, captured as contact sheets: house (3), warehouse (2), mine base (4), hunting base (3),
    transport station (3), mine (1), institute (4).
  - Each building's debug tab in detailed mode, with its second section.
  - The institute's idea notice: big title, 由「材料札记」和「承重札记」想到的。, the 新想法 stamp. The list puts it
    first with 第 31 日, and clicking it pins the excerpt.
  - The line-breaking rule on a hunting base note and on the excerpt.

## Sixth round: the real journal, the empty town, and a final sweep

- **The journal away from the desk.**
  - `KnowledgeLayer` already guarded a missing desk menu in two places. With every desk call guarded, it now runs
    read-only: no tools, no organising, linking or research actions, and the close button reads 合上.
  - `KnowledgeJournalViewer` hosts it as a plain CUI screen sized like the desk's journal, opened at one entry by
    `showEntry`. That picks the archive page and the kind tab, focuses the entry and scrolls the list to it.
  - The excerpt's 在札记里打开 opens it, and closing it returns to the institute board.
- **FTB sidebar.**
  - The research mod already hid FTB Library's sidebar buttons over the desk.
  - Screens that fill the window now carry `HidesSidebarButtons`, and the same mixin checks it. The buttons no longer
    sit on the boards' top plaque.
- **A starving, then empty town.**
  - I drained the warehouse's food with `/town resources costByType` and ran twelve settlements, until every resident
    had left.
  - Popups along the way: 食物储备已跌破危险线, then 2 and 3 名居民因生命状况离开或死亡, 1 个保障岗位尚未补足, and
    2 名居民已恢复工作能力.
  - The empty town showed what the boards had never been asked:
    - The seal said 人人有家 · 人人有活 with nobody in town, and its food card said 还在清点.
    - The house meal page showed 吃饱 100% for zero eaters.
    - The institute blamed education (没有未受教育以上的人) when it simply had nobody.
  - All three now read as an empty town.
  - Five new residents and food brought the town back.
- **Debug sheets kept rebuilding.**
  - Building boards rebuild on every resource change, so a debug sheet jumped back to the top while being read. The
    seal had marked its debug tab live and never showed this.
  - Debug tabs now rebuild only on resize or a mode change.
- **CUI.** Clicks, releases, drags and the wheel refresh the pointer and hover state before dispatch.

### Sixth-round validation

- GameTests: 59 pass across nine suites. The house test now checks that a house its people just left still shows their
  last meal, while a house nobody lived in promises none. The seal test checks the empty town's chip, note, food and
  well-being.
- In game, with the final build, all at 427×240 GUI:
  - The seal's nine tabs and all seven buildings' tabs, in the recovered town.
  - Every debug section of every building and of the seal.
  - The residents debug sheet with five residents, scrolled and still in place after a rebuild window.
  - The journal opened at 交错的支撑, read-only, and closing it returned to the board.
  - The starving popups.
  - The empty town's seal, house overview, house meal page and institute association page.

## Seventh round: polish and Chinese line breaking

- **What the remaining limits really are.**
  - Undated ideas: the institute's association exists only in this uncommitted work, since `instituteAssociation`
    is in no commit. Every institute idea in a released build gets its day; only this development's test world has
    a few from before the fifth round.
  - Read-only journal away from the desk: the design places linking in the drawing desk
    (`design/knowledge_research.md`, the 想法 section), so the board only reads it.
- **Services page.** Each service gets a summary note above its ledger: a big figure, a paper bar and a verdict, the
  same language as the stat notes elsewhere.
- **Tower card.** Each line has a small lamp in its tone.
- **Empty crews.** A building without workers hangs a row of dashed 空位 frames, one per post, with the note beside
  them.
- **Chinese line breaking.**
  - Vanilla `Font.split` breaks at the last space that fits. Chinese strings here space their numbers, so a long
    sentence broke before its first number and left a stub line such as 还空着.
  - `TownTextLayout.breakLines` fills Chinese lines character by character. It keeps numbers and Latin words whole,
    honours no-break spaces and kinsoku, and avoids a one-character last line.
  - English keeps vanilla word wrapping.
  - 117 strings now bind a number to its measure word with a no-break space.
- **A display crash.** One launch died in GLFW's video-mode query while the display slept. That is an environment
  problem, not this code; waking the display fixed it.

### Seventh-round validation

- GameTests: 59 pass across the nine suites.
- In game:
  - At 640×360: the services notes for all three services, the tower lamps, and the empty hunting crew's frames.
  - At 426×240, with the GUI scale pinned in the test client: notes on the hunting base, institute, warehouse and the
    seal's laws, all wrapped with the new breaker.

## Eighth round: evidence for the open points

- **Recorded days, proven.** The association GameTest now follows a real institute association. It checks that the
  town day lands in the knowledge state, survives `KnowledgeView.create` and is read back by `KnowledgeJournalData`.
- **Undated ideas.** Ideas found before the day was recorded now read 第 N 日以前, where N is the earliest recorded
  day in the list. That is always true, because recording began on day N. No other source can date them: settlement
  history keeps no game time, and the ideas' own acquisition time is game ticks, which do not map back to town days
  after `/town tick`.
- **Read-only journal, exercised.** In game I opened it from the institute and went through all five pages: inbox,
  archive, links graph with zoom, hints, and research with a project opened. Closing it returned to the board.
  - Read-only follows the design doc, which places linking in the drawing desk. The owner never asked for it
    separately, so it stays open to change.
- **A third GUI size.** At 512×288, the seal's nine tabs and all twenty building tabs render cleanly. Pages have now
  been checked at 427×240, 512×288 and 640×360.

### Eighth-round validation

- GameTests: 59 pass across the nine suites, with the extended association test.
- In game: the walk through the read-only journal, the past ideas list with 第 31 日 and 第 31 日以前, and the full
  tab sweep at 512×288.

## Ninth round: a second world and the English interface

- **A second world.**
  - I tested a copy of `run/saves/20030716-3`, an older dev save with a different town: 12 residents, six buildings
    within 16 blocks of the tower, two houses stacked at one spot, no mine, no institute. It predates building numbers,
    so numbering ran as a migration.
  - **A broken warehouse.** With a failed room it refused its board and printed three raw English debug lines in chat.
    It now opens like every other building, on its overview, and the stock tab is a 暂停存取 notice. Storing and taking
    stay refused server-side (`canUseStock`), as before.
  - **Two calendars.** The events showed world days (第 243 日) while the header showed the town's age (第 20 日).
    History has one entry per settlement, and the town's day advances with each, so an entry's position gives its town
    day. The debug sheet now labels raw world days 世界第 N 日.
  - **Low reserves.** 不到 1 天 sat above 够全镇吃. Low and critical reserves now say 该多存些吃的了 and 快断粮了;
    fuel likewise.
  - **Not changed.** Every room in that save fails today's structure check, though it passed when saved. From outside
    the rooms look intact. That is the room scanner, not the UI.
  - **Actions on the migrated town.** I moved the hunting base's pin from 3 to 8 of 12 on a stopped building, swapped
    the stacked houses, and renamed the upper house 楼上的屋子, which kept 住宅第 2 号.
- **A fresh idea, in game.** I granted results until a rule completed. The next settlement found 纸页与木棍, listed
  first as 第 65 日.
- **English, seen in game for the first time.**
  - Plurals: 62 `.one` forms, picked by `TownFormat.counted` when the leading count is one; the Chinese copies repeat
    the plain text.
  - The bar label column now fits its widest label.
  - Service names are shorter, stamps share one case, and Spirit replaces Morale.

### Ninth-round validation

- GameTests: 61 pass across the nine suites. New: town-day dating of events from an older history, and singular forms.
  The second also checks that every `.one` key exists in Chinese.
- In game:
  - Older town: all tabs of the seal and five buildings, the broken warehouse's board and closed stock, events in
    town days, and the actions above.
  - institute-fresh: the fresh idea.
  - English: two full sweeps of all 29 tabs, before and after the fixes.

## Tenth round: the reviewer's points

An independent review of earlier screenshots listed twelve problems. Each was checked against the code first; the ones
that held were fixed, together with two wording problems found while testing a town without a tower.

- **One warmth word for a home.** The tile said 合适 while comfort said 太热了 and the residents' cards said 暖和, and the
  cards ignored the cool margin. Now one verdict, `TownFormat.homeTemperature`, adds 不够暖和 and 有点热 for a livable but
  uncomfortable home, and every place uses it. The bare-home note names its real cause.
- **One headcount.** The overview counted from the last settlement, the residents page from today's duties, and staffing
  from who could work somewhere. All three now share `BoardPresenters.headcount`: 在岗 / 空闲 / 歇工 / 休养, children apart.
  A job at a stopped workplace is 歇工, and a stopped hunting base no longer forecasts a hunt.
- **Notes that matched their screen.** A stalled institute task asked for 中学 because the requirement was hard-coded; it
  now names the task's own. The chance reads 没有可想的了 when nothing is left to connect. A tie in the shortest nutrient
  goes to the lowest bar.
- **Numbers.** The services note rounded used, total and left independently (409 / 3887 · 3479 left); left is now total
  minus the shown used, in the ledger too. 「224 / 0」 and 「矿井 0 / 0」 show only what means something. Health and spirit
  read 「51 · 51」 instead of a fraction.
- **Board craft.** Captions on bare wood sit on paper strips in dark ink. Tips have a hanging indent, and short last lines
  are filled from the line above. Tips wait while a board is open. The staffing and housing pins sit on their track
  instead of over the counts, which now read 「在岗 1/2 · 保障 0」, and a how-to note fills the space below the strips and
  explains the colours. Building cards on the seal show a pictogram per type, and the seal's tabs no longer reuse
  icons from the boards. The warehouse lists 概览 first but still opens on its stock.
- **Voice and terms.** Stopping faults say 得修 in red; the leak note is in a resident's words; unsigned notes are from
  镇公所. 研究人力 became 研究人手, the warehouse's state is only 停用, and several stiff lines were rewritten.
- **Smaller items.** Duplicate first names on a photo wall add the surname's initial; a strip that cannot fit everyone
  ends with +N; empty stars have a darker outline; bars are coloured by value, with the job's bar marked by a bold label;
  journal titles that do not fit end in an ellipsis instead of going blank.
- **Not changed.** Stock counts already draw last, bottom-right, shadowed and abbreviated; the reviewer's sheet was scaled
  down. The town seal's initials for Latin names are a matter of taste.

### Tenth-round validation

- GameTests: the nine town suites, 62 tests, all pass, after every fix below as well.
- Two launches first failed before the title screen with GLFW's "Can't find a primary monitor" while the display was
  locked; the next one, with the owner at the machine, started.
- **In game, institute-fresh** (a superflat world; its houses now read −13 °C, see the flat-world diary), all nine seal
  tabs and every tab of the house, warehouse, hunting base and institute:
  - The residents headline adds up (5 = 3 on duty + 1 off + 1 child), and Joseph's photo is stamped 歇工.
  - The services note and ledger agree: 405 / 3887, 3482 left.
  - Building cards show a bed, chest, bow, ore, pickaxe, book and minecart. The pins sit on the tracks, the counts read
    「在岗 2/4 · 保障 2」, and the how-to notes fill the space below.
  - The house's comfort reads 「简陋 · 太冷」 beside −13 °C. The cold note is tagged 「得修」, and a stopped hunting base says
    「停工中，明天不出猎」.
- **What the check found, and the fixes:**
  - Corner toasts (research done, advancements) are vanilla toasts, which the tip change did not cover. They now wait
    too: an advancement granted with the seal open stayed hidden, then slid in once it closed.
  - White stock counts vanished on pale icons (「51」 on paper). They now have a dark outline all round and read on every
    icon.
  - A stopped institute still stamped its researcher 在岗, listed him under 今天在岗, and promised growth. Work boards now
    count someone on duty only while the building works. A fit worker at a stopped building is 歇工 and gets no note
    about their own fitness, since the building's note says why. By the time of the recheck everyone had moved out of the
    frozen houses, so no worker was left at a stopped building to show this; the GameTests cover the code path.
  - A resident card said 缺蔬菜 while its lowest bar was 油脂: it named the last meal's weighted shortfall, the bars show
    reserves. The card now says 「上一顿缺蔬菜」 and the meal tab names the lowest reserve 「储备最少的是油脂」.
  - The job's main bar was always purple; bars are now coloured by value everywhere, and the bold label marks the job's.
  - The colour key in the staffing note wrapped mid-phrase; it is shorter.
  - Day 66 brought three more: an empty house's meal said the warehouse had no food (it held 19 days), an empty unfit
    house said 不宜居 twice, and the seal showed 人人有活 beside 5 人歇工. They now read 「这天屋里没人住，没开饭」 and
    「还空着 4 个床位，修好了就有人搬来」, and 人人有活 appears only when nobody is free or off. Rechecked in game.

## Remaining

- Ideas found before the day was recorded show a bound, not an exact day.
- Whether the journal should be editable away from the drawing desk is the owner's call.
- Why the older save's rooms fail today's structure check.
