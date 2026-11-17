# Research graph performance review and title-cache reuse

- Time: `2026-10-24 03:52:24 +08:00`
- Author: `Codex; OpenAI GPT-6; performance review, implementation and real-client validation`
- Status: `completed`
- Scope: Research graph rendering and typography, existing opt-in client validation and research UI documentation

## Completed

- Reviewed `ResearchGraphViewport`, its shared `PanZoomViewport`, archive presentation/state refresh, shape/icon batching, item-model preparation and bitmap-font generation/lifecycle. Existing reusable buffers, one shape pass, grouped item rendering, direct font pass, hit cache and pointer-anchored camera motion remain appropriate for this catalogue. Dynamic item models remain resolved once per submitted item per frame for correct animated/resource behavior.
- Found concrete redundant work: every camera translation rebuilt clipped strings, wrapping and styled title sequences even though the title and text constraints were unchanged. Added one latest `NodeTitleLayout` per projected source, keyed only by text width, two-line capacity and presentation revision. Geometry continues to move; warmed titles are reused across panning, window movement and zoom frames with unchanged integer text constraints.
- Kept the cache bounded by source nodes, with no ID lookup map or collection of zoom variants. Projection replacement replaces its source caches. Empty second lines use the shared empty sequence. Reworked clipping to inspect the fitting prefix before overflow handling, removing the prior full-title width pass and repeated prefix scan for word wrapping.
- Changed the existing title-plan observation counter to count actual per-node layout misses. Extended actual client input with rendered pan/return/repeat stages and an assertion that warm repeated panning performs zero title layouts while geometry changes. Name inspection also checks that cached text matches its current source's localized name.
- Documentation impact: updated [research-ui.md](../docs/research/research-ui.md) with independent geometry/text invalidation, bounded cache ownership, prefix clipping, counter semantics and real panning verification. Joined frame geometry, font assets and the existing rendering passes are retained.

## Decisions

- Remove proven repeated work with one local cache instead of adding a general cache framework, spatial index, persistent item-model cache or new rendering subsystem.
- Use actual work counts to verify the improvement. Zero title rebuilds is not an FPS, allocation-byte or total-memory benchmark, and no absolute hardware-independent optimum is claimed.
- Prepare source and documentation before a unified final production-catalogue/client run in the current checkout. No copied checkout, artifact pinning, JUnit or standalone model tests.

## Validation

- Final command: `.\gradlew.bat validateResearchCatalog runResearchUiClient --offline --no-daemon --console=plain`, Java 17, current checkout. [Log](../build/research-ui-title-cache-review-20261024-final.log): `BUILD SUCCESSFUL in 2m 23s`; all 86 production research definitions passed.
- [Real-client result](../build/research-ui-validation/2026-10-24_03-56-13/result.json): 16 display scenarios, search and an additional pan-cache observation (18 observations total), plus 21 interaction stages passed. Chinese/English, GUI scales 1/2/3, minimum/compact/full zoom, shared frame geometry, source/language title ownership, icon/name input, modal retention, focus, pointer-anchored smooth zoom, real panning, search, empty graph and normal visibility all passed.
- After rendering both views and repeating actual mouse dragging, the 24 visible nodes reused all title layouts: `repeatedPanTitleRebuilds = 0`, while the render-plan counter confirmed geometry had rebuilt. Every static display interval also observed zero geometry/title misses. This confirms removal of repeated text layout, not a measured FPS delta or zero total frame allocation.
- Compared all common scenario results with the preceding joined-frame run: node count, icon count, named-node count, complete-name count, two-line count and displayed character count were identical in all scenarios. Inspected actual Chinese compact and English panned screenshots under [this run](../build/research-ui-validation/2026-10-24_03-56-13/). Font atlas/source observations remained one 256 KiB atlas payload and a 3,655,680-byte source image.
- `git diff --check` passed for touched tracked source/documentation. No copied checkout, extra font asset, new render pass, JUnit or standalone model tests were introduced.

## Remaining

- None for the reviewed repeated-work issue. FPS, load-time, allocation-byte and total-memory deltas were not profiled; the review does not claim an absolute optimum across hardware or future catalogue sizes.
