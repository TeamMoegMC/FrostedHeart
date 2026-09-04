# Receiver-lazy static Block radiation architecture correction

- Time: `2026-09-01 03:11:53 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `thermal Brick residency and optional player-only lava/fire radiation plans`

## Completed

- Separated source/frontier-owned thermal Brick residency from optional static
  Block-radiation coverage. Player, infrared, crop, town, and radiation queries
  create no thermal Page, cell, pair, dormant interest, or worker input.
- Replaced the Page-coupled radiation target with a dimension-owned,
  receiver-lazy known-Brick index. Palette-negative sections retain nothing;
  positive sections capture only requested unknown Bricks under a fixed budget,
  and compiled-empty Bricks are not rescanned.
- Updated the implementation sequence, cost contract, tests, performance gates,
  acceptance criteria, rejected alternatives, documentation impact, and outcome
  so they no longer treat Page admission or retirement as radiation lifecycle.
- Corrected the thermal runtime plan's remaining player-created-worker and
  retained-player-lease wording.

## Decisions

- Keep one `knownBrickMask`, one `emitterMask`, and compact packed emitters per
  covered palette-positive section until chunk unload. This is less lifecycle
  state than observer leases, refcounts, expiry wheels, or proximity eviction.
- Use public `LevelChunkSection.maybeHas` only as a negative palette gate. Do not
  depend on palette internals, scan every loaded section, or rescan the complete
  receiver sphere each update.
- Static 8-block torso rays bypass `ReceiverCache`; short retracing removes
  static source revisions and wall-witness invalidation. Existing physical
  Campfire/machine receiver caching remains unchanged.
- Bound radiation coverage independently from thermal `maximumPages` and count
  its measured worst-case storage in the existing dimension memory reservation.
- The current Java implementation remains Page-coupled and is explicitly marked
  partial. Its state/profile tables, packed emitters, and targeted lava
  invalidation are reusable, but its Page handle/install/remove path is not the
  accepted final architecture.

## Validation

- `git diff --check`: passed; only the repository's existing LF-to-CRLF warning
  was reported.
- Every relative Markdown link in both updated plans and this diary resolves.
- Targeted conflict search found no checked Page-coupled radiation stage,
  admission-owned radiation lifetime, static witness reuse, or remaining
  player-created worker/lease instruction.
- Java tests were not run because this change modifies plans and diary only.

## Remaining

- Implement Stage 1 items 2-4 in the player thermal plan, then run the focused
  lifecycle, coverage, cost, and live fire/lava validation defined there.
