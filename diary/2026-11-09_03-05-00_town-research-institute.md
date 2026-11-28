# Town Research Institute

- Time: `2026-11-09 03:05:00 -0600`
- Author: `Claude; Anthropic/claude-opus-5-5; town and knowledge implementation`
- Status: `completed`
- Scope: `content/town/buildings/institute`, `TownResearchSettlement`, `VirtualResourceType.RESEARCH_LABOR`, `frostedresearch/knowledge` (capacity extension, `labor` requirements, labor allocation, institute association, `labor_report`), journal and experiment table UI, example datapack, companion-pack recipe

## Completed

- New town building `frostedheart:research_institute` (type key `researchInstitute`), following [the plan](../plans/2026-11-09_02-15-31_town-research-institute.md):
  - Room scan like the warehouse; bookshelves, drawing desks (main half only) and decorations counted with `countAllBlock` through three new block tags.
  - Works only when area ≥ 9, volume ≥ 18, effective temperature ≥ 10 °C (north-face heat endpoint like the hunting base) and average block light ≥ 8. Light comes from the new `BlockScanner.sampleBlockLight` and ignores daylight.
  - Researcher slots = min(desks, ⌊space rating × area / 4⌋).
- Function 1, knowledge capacity: new extension point `KnowledgeCapacity`. A working institute grants `bookshelves × 16` archive and `× 4` inbox observations. The configured base dropped from 4096 / 512 to 128 / 64. The snapshot carries `capacity`, and the journal's inbox and archive pages show used / limit.
- Function 2, research labor:
  - Per-researcher labor = `linearResidentProductivity` (intelligence weight 60) × (1 + 0.3 × decoration rating).
  - It joins the new leveled daily service `RESEARCH_LABOR` at the researcher's education level. `resetAllServices` now clears every level.
  - Study records learning activity 1.0 and research proficiency.
- Optional `labor` block on calculation and experiment requirements (`required`, `workers`, `days`, `education`, `items`), with validation:
  - Optional labor shares stand in for the player's games or experiment runs.
  - Required labor must also be finished, and may carry a requirement alone (`count: 0`).
  - Progress is kept per run in worker-days (`ResearchHistory.labor`).
  - Drawing desk and experiment table only take requirements whose player part is still open.
- Daily coupling `TownResearchSettlement` runs after `buildingsWork`:
  - `KnowledgeService.workLabor` serves requirements by education, highest first, using the lowest qualifying level, at most `workers` worker-days a day. Daily items come from the warehouse in proportion through `TownResearchSupplies`.
  - Idle labor of education ≥ 2 associates with chance `1 − e^(−0.15 L)`. A success links a new idea (`institute:link:<rule>`), or else gives a hint.
  - The day's `LaborReport` is saved as `KnowledgeState.labor_report`.
- Candidate link search now has a step budget, which closes review P2-7.
- UI:
  - Institute screen with overview, researchers and research tabs; the research tab reads the knowledge snapshot.
  - Journal research page shows labor requirements, progress and the last morning's allocation, with a "waiting on the institute" state.
  - Mayor's Seal shows building details and a research labor resource page.
- Assets and data: blockstate, models, loot table, placeholder texture (open book in the town-core frame), tags, zh/en text. FH protocol is now `building-data-2`.
- Example datapack: `basic_switch` and `pressure_control` accept optional labor; `kiln` gained a labor-only requirement "整理烧窑记录". Synced to `run/saves/knowledge-test-world`.
- Companion pack: shaped recipe `BAB/ACA/BAB` with planks, stone bricks and a bookshelf in `kubejs/server_scripts/src/recipes/shaped/new.js`.
- Docs: [town-model.md §24](../docs/town/town-model.md), [building-data.md](../docs/town/building-data.md), [implementation-reference.md](../docs/town/implementation-reference.md), [definitions.md](../docs/knowledge/definitions.md), [state-and-api.md](../docs/knowledge/state-and-api.md), [workbench.md](../docs/knowledge/workbench.md), [playthrough.md](../docs/knowledge/playthrough.md).

## Decisions

- Capacity still limits observations only. The design document limits observation records, and research outputs must never be lost to a full archive.
- Capacity follows workability, so a cold or dark institute grants nothing. Saved records stay, and only new ones are refused, exactly as when the configured limit drops.
- Labor accumulates in worker-days and items are drawn fractionally, so a 1.9-worker day is not wasted. Fractional amounts are what the warehouse already stores.
- Allocation serves the requirements that ask for the most education first. With nested qualifications, this together with lowest-level-first is optimal.
- Institute proficiency starts at 0 and is not part of recruit generation. That keeps the paired-seed resident generation order unchanged.
- The research step catches its own exceptions, so a knowledge-side failure cannot fail the town day.
- Function 3 ("探索派遣型证据收集") is not implemented: the request names it without behaviour, and evidence must be real observations. Remote observation would need unloaded chunks, which the town code deliberately never loads.

## Validation

- `./gradlew compileJava compileGameTestJava`: success.
- `./gradlew runGameTestServer -PgameTestNamespaces=frostedheart_institute,frostedheart_buildings,frostedheart_town_view`: `All 31 required tests passed`, again after the in-game fixes. The seven new tests in `ResearchInstituteGameTests` cover:
  - a real furnished room (4 wall bookshelves, 2 two-block desks counted as 2, lectern decoration, dark → unworkable, glowstone ceiling → lit);
  - capacity added and withdrawn through `KnowledgeCapacity.limits`;
  - researchers' labor by education level, the learning activity and the unworkable report;
  - the building codec round trip;
  - labor validation (3 valid and 8 malformed shapes);
  - allocation order, proportional paper, item shortage and shares standing in for games;
  - association linking a new idea once, and the settlement report;
  - labor and report persistence, including an unreadable report that is written back.
  - The seventh building type was added to the existing wire-codec test.
- `./gradlew runGameTestServer` (default namespaces `frostedresearch`, `frostedheart`, `frostedresearch_knowledge_login`, `frostedheart_thin_ice`, `frostedheart_icicle`): 90 tests, 86 passed, 4 failed. Every failure is a thermal test unrelated to this work:
  - `waterInteriorWaitsForAnEdge`
  - `sensibleAirAndMaterialUseTheSameClockWithoutSaveDrift`
  - `warmedMaterialRemainsVisibleAfterTheRuntimeCloses`
  - `campfireSurfaceUsesMaterialAndLocalAvailability`

  One of them asserts the thermal checkpoint `version == 5`, while `DormantChunkThermalState.FORMAT_VERSION` is already 6 since the earlier thermal commits. All research and knowledge tests passed, including the experiment-table space test and the knowledge login tests.
- In a game client through the agent bridge (`Scripts/agent_client.py`, flat test world `institute-fresh`; see "In game" below).

## In game

Driven through the agent client bridge in a flat world with a T1 generator tower, a warehouse holding paper, charcoal and planks, a house and a 2-desk institute with 13 bookshelves, a lectern and sea lanterns. Test-world-only config: house maximum temperature 200 and association rate 100.

- Overview: workable status, failure reasons while dark or cold, researcher slots 2/2, desks, bookshelves, decoration 37 % (+11 % labor), lamp light 12.8, temperature, heating +24, area and volume, and capacity +192 archive / +48 inbox.
- Researchers tab: per-researcher labor 1.20 at doctoral level, research proficiency and daily growth, with the extra education-tier line.
- Research tab: forecast by tier, this building's last settlement, the town report with per-requirement allocations, idle labor, association, and the team's capacity 0/320 and 0/112.
- Journal: the research page lists the three example projects with labor requirements. `kiln`'s labor-only "整理烧窑记录" shows daily items, worker-day progress and the last morning's allocation; the player's finished part shows the disabled "等待研究所协作" button.
- Settlement numbers match the allocation rules exactly. After one day, paper went 64 → 60.81, charcoal 32 → 31.405 and planks 64 → 63.919: kiln got 1.19 worker-days from the doctoral researcher, `basic_switch` 1.0 and `pressure_control` 0.04 from the level-1 researcher.

- After the fixes below, a second session in the same world covered the rest:
  - A morning with no residents replaced the old allocations with "上次清晨没有研究人力：没有研究所在工作。" in the research tab.
  - With bread stocked and three new residents (education 0 and 1), `pressure_control` took its last 0.91 worker-days and the planks stopped at exactly 60.
  - Two gates were lowered in that world only so these residents could serve kiln and associate: association education 0 in its server config, and education 0 for kiln's labor in its datapack copy. Both were restored afterwards; the education gates themselves are covered by the GameTests.
  - Kiln then got its last 0.46 worker-days (charcoal exactly 30.0, paper 56.0). The idle labor associated, and the research tab showed "联想：想到了「回应的回声」".
  - The idea `knowledge_example:feedback` was in the archive with source "研究所的同伴想到的。".
  - Kiln's labor-only step read "这一项已经完成", "完成研究" was enabled, and completing it gave "研究完成了，成果已经整理。".
  - The inbox's empty page now opens with "来件中的观察：0/112（其中 48 由研究所的书架提供）". The archive tab's tooltip reads "档案中的观察：0/320（其中 192 由研究所的书架提供）".

Found and fixed in game:

- Loading an older save crashed with `ExceptionInInitializerError`: the transport station and mine base daily-report constructors called the outer building's static `sanitize`, so the outer class initialised while the nested record's `CODEC` was still null. This dates from before this work (763d8475b). Each report now has its own `finiteNonNegative`; the unused `MineBaseBuilding.sanitize` is gone.
- When the institute stopped (in the test, because the unfed test residents left the town), the team's report kept the last working day's allocations, so the journal and the research tab went on showing stale labor. Now `TownResearchSettlement.retireReport` replaces a report that still shows labor with an empty one on the first morning without labor. Both screens say there was no research labor that morning. A GameTest covers the single replacement.
- Worker-days showed 1.1 in the journal (floored) and 1.2 in the institute (rounded) for the same 1.19. Both now floor to two decimals, so unfinished work never reads as done.
- The journal's capacity line was clipped below the empty-page text and missing once records existed. It now leads the empty page and is the tooltip of the inbox and archive tabs.
- The desks label wrapped; it is shorter.

Balance observations, not changed:

- Lamps are heat sources in Frosted Heart (glowstone +5, lantern +10), so a well-lit institute is also a warmer one. Meanwhile a room near a level-1 T1 tower in an older save sat at 9.9 °C, just under the 10 °C threshold.
- Open optional-labor requirements absorb labor before association, so association rarely runs while any are open. This follows the design ("空闲人力"), but players may want a way to keep some labor free.
- The test residents, given no food, left the town within a few days. A real institute needs the town's housing and food to keep its researchers.
- At a 427×240 GUI (1280×720 window, GUI scale 3), the journal's research reading pane shows only two or three lines above the requirement buttons, so labor details need scrolling. The layout predates this work and has more room at larger GUI sizes.

## Remaining

- Function 3, dispatch evidence collection, needs a design decision from the project owner.
- No quest or ponder scene teaches the institute yet.
- The institute is not modelled in the Stage 3/4 town simulator.
- Labor goes to every open run automatically; there is no per-run opt-out.
