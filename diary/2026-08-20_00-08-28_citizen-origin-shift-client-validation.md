# Citizen origin-shift client validation

- Time: `2026-08-20 00:08:28 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent, recording user validation`
- Status: `completed`
- Scope: `FlywheelCitizenBackend origin-shift recovery client validation and town living documentation`

## Completed

- Recorded the client confirmation that the repaired M3 backend remains visually correct after the Flywheel render-origin transition.
- Closed the persistent Body disappearance regression: batch citizens no longer require a FakeEntity or Billboard owner transition to become visible again.
- Updated the town rendering documents from pending validation to verified behavior.

## Decisions

- Treat the origin-shift lifecycle fix as accepted for visual continuity and instance recovery.
- Do not infer dirty-byte peaks or timing data from this run because no metrics output was supplied.

## Validation

- User client result: no visible issue after exercising the repaired rendering path; the previously reported Body flash-and-disappear behavior did not recur.

## Remaining

- None for the origin-shift disappearance regression. Billboard silhouette, Oculus compatibility, and GPU performance remain separate M3 acceptance items.
