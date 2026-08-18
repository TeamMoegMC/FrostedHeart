# Citizen radius-aware wall collision

- Time: `2026-08-18 16:22:19 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `content/town/citizen/sim`, fake citizen entity dimensions, collision regression tests

## Completed

- Replaced point-sized wall contact with a shared `0.6`-block citizen width and an outward-rounded fixed-point collision radius (`308/1024` blocks).
- Movement now checks the body's leading edge and perpendicular footprint before entering a new cell, then clamps the body edge rather than its center to the blocking face.
- Added recovery for persisted citizens left exactly on old integer wall boundaries; the nearest passable adjacent cell is selected and the citizen is moved fully into its interior.
- Added `MovementSystemCollisionTest` coverage for both travel directions, negative coordinates, and exact-contact versus one-unit penetration.

## Decisions

- Fixed the authoritative server simulation rather than offsetting client rendering, so synchronization, picking, and every render LOD use the same corrected position.
- Kept the existing low-query grid model: normal within-cell motion performs no new world query; footprint checks occur when the leading edge crosses a cell boundary, while old-boundary recovery only runs for exact integer coordinates.
- The proxy entity width now references `CitizenState.BODY_WIDTH` so rendered width and simulation radius cannot silently drift apart.

## Validation

- Before the fix, the new wall-clamp regression suite failed all 3 initial tests, proving the old center-on-face behavior.
- `gradlew test --tests com.teammoeg.frostedheart.content.town.citizen.sim.MovementSystemCollisionTest --console=plain`: passed, 4 tests.
- `gradlew test --console=plain`: passed, 144 tests, 0 failures/errors/skips; compilation retained only 20 pre-existing JEI removal warnings.
- `git diff --check`: no whitespace errors (only the repository's existing LF-to-CRLF notices).

## Remaining

- Verify in `runClient` with straight walls, inside/outside corners, crowded separation pressure, and a loaded save containing citizens already resting on wall boundaries.
- Non-full-block obstacles still use the existing cell-level `passable` approximation; exact fence/wall/pane voxel-shape navigation is a separate behavior and performance decision.
