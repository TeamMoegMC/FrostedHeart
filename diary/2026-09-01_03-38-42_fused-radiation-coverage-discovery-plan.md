# Fused static-radiation coverage and discovery plan

- Time: `2026-09-01 03:38:42 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `receiver-lazy Block radiation query contract and player thermal plan`

## Completed

- Replaced separate coverage and emitter-discovery plan paths with one
  `BlockRadiationIndex.ensureAndVisitNearby(...)` call over at most eight nearby
  sections.
- Removed `NearbySourceIndex.isEmpty()` and remaining-candidate-budget as provider
  invocation gates. Empty state and zero remaining visits still establish
  unknown coverage; only emitter submission observes visit capacity.
- Added the full-known-section fast path, which skips requested-mask construction
  when `knownBrickMask == -1L`.
- Updated exact costs, ownership closure, Stage 1, functional tests, performance
  validation, acceptance criteria, rejected alternatives, documentation impact,
  and outcome.

## Decisions

- Keep physical-source discovery first. Pass only its remaining visit count to
  the fused provider; do not let physical saturation starve future static
  coverage.
- Continue coverage checks after emitter submission stops, so every receiver
  update retains the same at-most-eight-section correctness bound.
- Add no preparation cache, section list, player state, second traversal, or new
  lifecycle owner. The fused method deletes an interface method and a call path.
- For 100 one-Hz receivers, the stable bound remains 800 section resolutions per
  second total, never 1,600 from separate ensure/discovery passes.

## Validation

- `git diff --check`: passed; only the repository's existing LF-to-CRLF warning
  was reported.
- Every relative Markdown link in the updated plan and this diary resolves.
- Targeted conflict search found no remaining `ensureNearby` production call,
  nonempty/remaining-visit provider gate, duplicated 8+8 section traversal, or
  1,600-section-resolution acceptance path.
- Java tests were not run because this change modifies plan and diary only.

## Remaining

- Implement Stage 1 items 2-4 and run the empty-index, saturated-physical-budget,
  fused-section-count, block-DDA, lifecycle, JFR, and live fire/lava fixtures.
