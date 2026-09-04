# Thermal Brick residency plan correctness closure

- Time: `2026-08-31 23:32:51 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `active thermal async plan Brick residency correctness, minimal state, mutation channels, pending requests, and overload behavior`

## Completed

- Added the single dimension-owned pending residency map required to cancel
  unadmitted Page requests without repeated unchanged completion payloads or
  continuation-parent graphs.
- Replaced the impossible missing-neighbor proof/error estimate with an
  owner-side aperture candidate and a face-component residual computed only
  from known topology and temperatures.
- Collapsed captured/active into one monotonic worker resident mask and made
  frontier state reusable scratch instead of retained Page state.
- Split sparse mutation behavior into resident geometry invalidation and
  independently observed source, sky-column, and radiation channels.
- Closed work-limit behavior with the existing retry backoff, no new expansion
  delta, no fake natural sink, and explicit bounded-memory local overheating.
- Added coalesced same-cut Brick directory replacement, player-required versus
  source-seed separation, mixed-component face validation, pending cancellation,
  and work-limit acceptance scenarios.

## Decisions

- The minimum retained worker Page masks are `resident`, `resolved`, `required`,
  `sourceSeed`, and `hot`; no persistent frontier mask is justified.
- `REFINE_HIGH` and `RELEASE_LOW` remain one mandatory pre-production reference
  gate and are not exposed as configuration or selected from infrared precision
  alone.
- No LOD, room graph, one-pole impedance, matrix-free traversal, or
  steady-source sleep was added to the implementation scope.

## Validation

- `git diff --check` passed for the active plan with only the existing
  line-ending warning.
- Every relative Markdown link in the active plan resolves.
- Conflict search found no remaining missing-neighbor proof, unknown-neighbor
  error formula, captured/active pair, continuation reverse index, or
  previous-Page lease contract.
- Java tests were not run because this work changes plans and diary only.

## Remaining

- Implement the corrected plan beginning with the threshold reference fixture,
  then run its functional and controlled 100-source validation gates.
