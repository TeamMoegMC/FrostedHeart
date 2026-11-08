# Research graph compact names and two-line titles

- Time: `2026-10-24 03:04:19 +08:00`
- Author: `Codex; OpenAI GPT-6; implementation and real-game validation`
- Status: `completed` (validated single-frame version; appearance and fractional font scaling superseded by the next revision)
- Scope: `ResearchGraphViewport`, removal of `ResearchGraphLabelPlacement`, opt-in client validation and research UI documentation

## Completed

- Replaced floating optional labels with a name column inside every project card. Distant cards use spare rank-column space while leaving a dependency corridor; zoom retains the same node centers, topology and 8-16 pixel real icons.
- Scale names continuously from 75% to normal size using the existing card expansion curve. GUI scale 1 keeps normal text size. When the card height permits, long names wrap into two lines; English prefers word boundaries and CJK uses glyph boundaries. Tiny slots keep original name prefixes instead of spending their character budget on an ellipsis. Full names remain in tooltips and the selected-project summary.
- Removed the occupancy grid, separate label buffer/records, width-variant arrays, label priority passes, optional-label hiding and label hit rectangles. The whole card, including the scaled name, targets its research project.
- Use one reused font matrix for the entire title pass and cache font coordinates/one-or-two-line strings with geometry. GUI scale is part of `RenderPlanKey`; hover and synchronized-state styling do not rebuild typography. Retained the 150 ms pointer-anchored camera animation and reusable shape/icon rendering.
- Strengthened the real-client driver to require names for every visible card; inspect font bounds inside cards and non-overlapping card geometry; record complete/prefixed/two-line names and character counts; test GUI scale 1, Chinese/English compact names, minimum-zoom icon/name clicks, hover cache reuse and existing modal/zoom/pan/search/empty/visibility paths.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with compact-width/font formulas, wrapping, short-prefix behavior, removed label machinery, cache inputs and current validation scope. Earlier completed diary entries remain historical records of the replaced optional-label behavior.

## Decisions

- Reserve a name slot for every project instead of asking independent labels to compete for free space. This preserves project identity and dependency relationships with less layout machinery.
- Moderate font shrinking plus two-line text preserves more characters than simply hiding ordinary project names. At the smallest scale a short prefix is intentional; hover retains the original full title.
- Reuse one font transform rather than allocating a pose/matrix per node. Run actual game validation only after implementation and documentation, using ordinary Gradle outputs in the current checkout.

## Validation

- Unified command: `.\gradlew.bat validateResearchCatalog runGameTestServer runResearchUiClient -PgameTestNamespaces=frostedresearch,frostedresearch_knowledge_login --offline --no-daemon --console=plain`, using Java 17 and the current checkout.
- [Final log](../build/research-ui-named-cards-20261024-final-r2.log): `BUILD SUCCESSFUL in 2m 47s`; 86 production definitions and all four required research/login Forge GameTests passed. The first attempt was interrupted by a transient missing generated recipe during another Gradle invocation; the ordinary retry succeeded without changing the recipe.
- [Actual framebuffer captures](../build/research-ui-validation/2026-10-24_03-09-19/) covered 16 display scenarios, a search observation and 17 interaction stages. At Chinese 15%, 79 in-view icons retained 79 names (160 name characters); at 45%, all 24 in-view names were complete. Every settled display scenario observed zero static geometry/title-plan rebuilds. Minimum-zoom icon/name input, hover cache reuse, dialogs, focus, pointer-anchored intermediate zoom, pan, search, empty graph and normal visibility passed.
- After screenshot inspection the user requested the previous separate icon/name frame composition and clearer text. This validation applies to the single-frame, fractionally scaled-font implementation recorded here; the following diary entry owns validation of its replacement.

## Remaining

- The subsequent user request replaces the single-frame appearance and fractional font scaling while retaining mandatory short names, wrapping and graph continuity.
