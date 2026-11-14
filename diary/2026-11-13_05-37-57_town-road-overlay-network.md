# Town road overlay and network synchronization

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; all 12 GameTests passed
- Scope: town connectivity spanning-forest witnesses, on-demand network view, client road highlighting

## Changes

- Keep one shortest-path witness per union. Remove remaining entrances of already-connected target nodes from the BFS target set.
- Publish immutable connected-node and witness-route snapshots with a UUID version that changes only with their contents.
- C2S requests poll every 40 ticks only while the existing zone view is open. Resolve the player's own town and dimension; unchanged versions produce no S2C response.
- S2C packets identify the town, dimension and version. Encode each route's first point absolutely and all later adjacent steps in one byte each.
- Reuse the room overlay key (default J). Cyan floors/cores identify powered paths/buildings, amber floors identify detached cluster paths, and red cores identify detached buildings. Draw within 96 blocks of the camera.
- Deduplicate floor positions on packet receipt, not each render frame. Clear caches on overlay close, dimension/town/connection changes and full-view installation.
- Update English/Chinese key labels and docs/town/building-data.md.
- Add three regression tests for forest witnesses, detached witness flags and packet round trips including negative coordinates, stairs, ladders and section crossings.

## Validation

- Offline compileJava and compileGameTestJava passed (1m 23s).
- English/Chinese modified JSON parsed successfully; offline processResources passed (17s), with existing duplicate-resource warnings.
- Dedicated test run uses frostedheart_town_connectivity and isolated build/town-road-overlay-gametest.
- Dedicated run completed: “All 12 required tests passed” (local log /tmp/town-road-overlay-gametest.log).
- git diff --check passed. No graphical-client rendering or real client/server round-trip acceptance claimed.
