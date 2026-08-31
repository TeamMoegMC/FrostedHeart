# Thermal radiation minimal hot-path closure

- Time: `2026-09-01 02:40:03 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `thermal state capture, static Block radiation, DDA mutation ownership, chunk lifecycle, and signature storage`

## Completed

- Attached an existing `SectionOwner` when DDA first tracks a section loaded
  before runtime startup, without admitting or retaining a Page.
- Replaced dimension-wide radiation revision scans and fixed vertical neighbor
  Page scans with changed-chunk section lookup and the existing `pagesByChunk`
  index.
- Reduced Page/radiation capture to one state-code lookup per block and added a
  primitive radiator mask so empty Bricks skip compilation and nonempty Bricks
  visit only emitters.
- Reused `PageSignatures.Builder` Brick scratch, moved the LiquidBlock cached
  type check ahead of the feature read, and removed the test-only signature
  count plus the degenerate topology-equivalence API.

## Decisions

- Added no retained index, compatibility path, production probe, counter, or
  new runtime abstraction. Existing Page ownership, state-code, and chunk index
  remain the authorities.

## Validation

- Per user direction, no Gradle task or automated test was run. Static symbol,
  call-site, and diff checks were used to close the change.

## Remaining

- User-owned in-game validation.
