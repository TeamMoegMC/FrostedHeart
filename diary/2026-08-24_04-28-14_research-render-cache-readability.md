# Research render-cache readability refactor

- Time: `2026-08-24 04:28:14 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `Research archive render snapshots, cache identities, layout caches, node hit testing, and Chorda icon texture batching`

## Completed

- Replaced the flat 27-component `WorkspacePresentation` with a display-ready `WorkspaceRenderSnapshot` composed from header, progress, tab, detail, theory, experiment, and footer render data.
- Reduced `ResearchProjectWorkspace#renderSnapshotFor` to cache validation and named builder orchestration; text wrapping, clue rows, materials, effects, and footer actions now have explicit ownership.
- Grouped graph geometry, node style, list filtering, row-title, summary, empty-text, archive-layout, and drawing-desk-size cache identities into named key types.
- Added editor mode to the left-list cache identity and a regression test so legacy ordering cannot be reused across an editor-mode transition.
- Encapsulated the graph mouse-hit fields in one reusable `NodeHitCache` and separated `ResearchArchiveViewCache.View` localized presentation from synchronized progress state.
- Split `CIconBatch` texture submission into named layer discovery, layer rendering, texture-group rendering, and request lookup methods without replacing its reusable request buffers.
- Updated `docs/research/research-ui.md` with the implemented cache and render-snapshot boundaries.

## Decisions

- Cache-key records are created only when their cache is rebuilt; cache-hit checks use `matches` methods so static frames do not allocate keys.
- Kept `ResearchGraphViewport.NodeRenderData`, rectangle buffers, and `CIconBatch` requests as flat reusable mutable hot-path storage. Wrapping each entry would reduce locality and add indirection without clarifying ownership.
- Preserved the `15%-175%` zoom contract, low-zoom icon/text floors, legacy left-list ordering, graph layout, clue/completion rules, packet actions, and dynamic item-model lookup behavior.

## Validation

- `./gradlew.bat compileJava --no-daemon --console=plain`: passed after each refactor stage.
- Targeted archive/CUI/item-batch tests: `12` suites, `35` tests, zero failures, errors, or skips; the final list-cache regression class also passed both of its tests.
- `./gradlew.bat test --tests "com.teammoeg.frostedresearch.*" --no-daemon --console=plain`: `20` suites, `56` tests, zero failures, errors, or skips.
- `./gradlew.bat test --no-daemon --console=plain`: `137` suites, `503` tests, zero failures, errors, or skips.

## Remaining

- In-game visual QA and FPS/JFR profiling were not repeated because this pass changes code structure rather than rendered behavior or batching policy.
