# Town connectivity doors and boundary tests

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; all nine GameTests passed
- Scope: door traversal and virtual town connectivity boundary verification

## Changes

- `LoadedTownWorld.getPathBlockType` classifies every DoorBlock as AIR for road snapshots regardless of material, half or open state. Ordinary room scanning retains original collision classification.
- Extracted the production connectivity calculation behind package-local virtual-world inputs; tests execute the same BFS and union-find as asynchronous snapshots.
- Corrected Manhattan candidate pruning: each stair step changes both horizontal and vertical coordinates, requiring a conservative `2 * MAX_DISTANCE + 4` bound including controller entrance offsets. Final route limit remains 60 moves.
- Added nine GameTests covering 60/61-step inclusivity, relay sharing, wall/head clearance, 60 ascending steps, ladders versus unsupported air, wood/iron door classification, disconnected clusters at negative coordinates, empty towns and cancellation.
- Updated docs/town/building-data.md with current door behavior, pruning and validation command.

## Validation

- Offline compileJava and compileGameTestJava passed.
- Dedicated GameTest run uses namespace frostedheart_town_connectivity and isolated build/town-connectivity-gametest directory.
- Offline runGameTestServer completed successfully in 3m 30s: “All 9 required tests passed”. Logs: /tmp/town-path-gametest.log (local generated artifact).
- git diff --check passed. No graphical client, asynchronous section-budget stress test or large-world performance validation claimed.
