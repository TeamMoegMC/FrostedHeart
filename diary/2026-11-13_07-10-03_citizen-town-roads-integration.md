# Citizen town road navigation and client presentation

- Author: Codex
- Date: 2026-11-13 UTC
- Status: completed; 28 GameTests passed, real client screenshots captured and game closed
- Scope: player-town citizen road navigation and client-local one-block variety, steps and ladders

## Changes

- Use all scanned room doors as building road ports, falling back to controller-neighbour ports without layout data. Building entrances share one graph node; indoor routes are not computed.
- Candidate pruning includes each controller-to-port Manhattan radius; retain the 60-move road segment bound.
- TownRoadNetwork shares adjacency, spatial indices and up to 256 cached node-pair routes. TownRoadNavigation keeps runtime-only stable-id cursors; handles idle placement, wandering, actual 3D work locations, homecoming, vertical ladder progress and explicit building-port transfers.
- Player-town road movement bypasses old 2D flow fields, terrain conformance and separation pushes. New obstacles stop progress; removal allows resuming. Sleep/removal/detach/version changes invalidate runtime cursors. AI and unmanaged citizens keep existing movement.
- Low-frequency spawn entries include nullable roadTown UUID; ordinary movement batches retain their existing layout. Protocol suffix citizen-road-1 distinguishes the updated format.
- Share one per-client-tick presentation sample across detailed entities, CPU batches, GPU interpolation and picking. Stable id-based side offsets are bounded by one block, support/headroom/sweep checks, with center fallback. Ladders cancel side offsets and use support-facing climbing poses; stairs/slabs use local collision surfaces.
- Resident presentation requests the versioned road cache even while J highlights are off. Cache lifecycle follows level/connection/town changes; existing bed sleep presentation remains separate.
- Add pure graph/placement checks and real server vertical-navigation/obstacle regressions. Development bridge roads setup/populate/status builds an isolated real town fixture, adds actual residents and records renderer/server state; test tooling is excluded from production JARs.
- Update building-data, hybrid-simulation-architecture and citizen-rendering-at-scale living docs, and track the implementation plan outcome separately.

## Validation

- Initial production and GameTest compilation passed twice.
- First combined run: all 25 required tests passed.
- Added direct vertical arrival and dynamic obstacle tests: all 10 citizen-road tests passed.
- Final combined 28-test run (including actual entrance-radius pruning): all 28 required tests passed; BUILD SUCCESSFUL in 3m 59s. Log: /tmp/citizen-roads-final-gametest.log.
- Actual graphical client in independent save citizen-road-integration-20261113, with five real houses, staircase, ladder and one isolated platform. Scanned graph selected four working buildings and three witness routes.
- Added 16 actual town residents using town.addResident with controlled 20°C housing during fixture recruitment. No separate decorative/fake population was injected. Server and client both reported 16 residents; all 16 had road presentation before J was pressed.
- Actual Flywheel and cpu_batch backends both rendered the population. Near-camera metrics reported cache=16, detailed=16; screenshots exercise the real detailed-player model. Samples showed ladder traversal (1–4 simultaneous climbing residents), server Y from 65 to 73.773, and varied client horizontal offsets (sample maximum 0.726 blocks; not a full distribution bound benchmark).
- Close ladder screenshot: run/agent/screenshots/01794554623473824400-citizen-roads-ladder-close-final.png. Close stairs screenshot: run/agent/screenshots/01794554628993232300-citizen-roads-stairs-close-final.png. The latter was captured after observing resident id 16 actively traversing stairs at x=3.5, y=66.4834, z=10.9834.
- Flywheel overview: run/agent/screenshots/01794554305481644200-citizen-roads-flywheel-overview.png. CPU overview: run/agent/screenshots/01794554309898402500-citizen-roads-cpu-overview.png. Screenshots and build/citizen-road-client-evidence.jsonl are local generated artifacts, not committed.
- Quit via the real bridge. Client log confirms Stopping at 07:24:51 UTC, integrated server shutdown and All dimensions are saved at 07:24:53, ending BUILD SUCCESSFUL in 12m 36s. Outer unified-exec session reported 143 after shutdown, as in the earlier client smoke run. Client log: /tmp/citizen-roads-client.log.
- git diff --check passed. No thousand-resident performance benchmark, full overnight economy/trade suite or complete multiplayer lifecycle matrix claimed.
