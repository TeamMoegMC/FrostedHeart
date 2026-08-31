# Receiver-lazy static Block radiation

- Time: `2026-09-01 04:27:48 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `static fire/lava coverage, Page decoupling, cold capture, mutation lifecycle, and radiation DDA`

## Completed

- Replaced Page admission-owned static radiation with palette-gated,
  receiver-lazy known-Brick coverage shared per dimension until chunk unload.
- Added one bounded pending Brick map with a 64-Brick-per-tick cold budget while
  retaining the existing two 20-tick primitive dirty buffers for known changes.
- Removed radiation callbacks from signature capture and Page admission,
  recapture, retry, retirement, restart, and dormant lifecycle.
- Made the nearby provider run even with empty coverage or no remaining source
  visits, while suppressing only emitter submission after visit exhaustion.
- Replaced quarter-cell ray traversal with one loaded-only block-grid DDA.
  Static rays bypass receiver caches, witnesses, Page lookup, and section
  revisions; physical sources retain their existing cached witnesses.

## Decisions

- Coverage uses section bit masks for the conservative 8-block cube to avoid a
  routine 125-Brick loop; emitter distance remains the exact 8-block sphere.
- Palette-negative sections retain no radiation state. Covered positive sections
  reuse the existing mutation owner rather than adding a section sidecar.
- The 64-Brick cold budget bounds at most 17,408 BlockState accesses in the
  all-lava worst case: 64 primary and at most 208 neighbor reads per Brick.

## Validation

- Per user direction, no Gradle task or automated test was run. Static symbol,
  call-site, lifecycle, and diff checks were used.

## Remaining

- User-owned build and in-game fire/lava validation, followed by controlled JFR
  and retained-heap measurement.
