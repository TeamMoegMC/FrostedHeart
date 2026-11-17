# Town: road scans remember routes through unloaded terrain, and the road view is saved

- Time: `2026-11-15 23:57:57 -0600`
- Author: `Claude (Anthropic Opus 5.5); design chosen by the project owner`
- Status: `implemented; GameTests pass; tried in a real client`
- Scope: `TownConnectivity`, `AbstractWorld.isKnown`, `TeamTownData` (`roads` field), `TownRoadClientTools`,
  three new GameTests, `docs/town/building-data.md`
- Follows: [road scans keep links while rescanning](2026-11-15_22-37-22_town-road-scan-keeps-links.md)

## Why

The previous entry let a scan that saw only part of a town add links but never remove them. The owner found the result
odd: with one building out of range, a road broken in front of the player never disconnected anything, until every
building happened to be loaded at once.

Unloaded chunks cannot change, so the routes the last scan found are still true where their terrain is not loaded.

## Completed

- Every scan now checks the previous routes first (`calculateView(..., known)`). A route still holds if every step in
  loaded terrain is still walkable from the one before, with its first and last steps still entrances of its
  buildings; steps in unloaded terrain stand as they were. The searches then look for what is not linked yet,
  unloaded terrain reading as walls. All buildings and the tower take part, and every result applies the full rule,
  so links can be added and removed. The grant-only path (`grant`, partial batches) is gone.
- `AbstractWorld.isKnown` (default true); the scan's snapshot world answers false for unloaded sections.
- A town with no building in loaded terrain is still not scanned (nothing there can have changed).
- `TeamTownData` saves the road view as `roads` (`TownConnectivity.RESULT_CODEC`: connected and working positions,
  the routes' steps). Loading restores the view and each building's two link flags. A new view marks the town for
  saving. The client codec carries no routes. Older saves have no field and start from the first scan, as before.
- `TownRoadClientTools`: `roads outpost <x> <z>` builds a registered house far away; `roads status` lists working and
  loaded positions.

## Validation

- New GameTests:
  - `brokenRoadUnlinksAlthoughPartOfTheTownIsOutOfRange`: fails with the grant-only code ("the house cut off from the
    path stops working, although part of the town is out of range"), passes now.
  - `savedTownKeepsItsRoadLinks`: fails before ("a saved town keeps its road view, routes included"), passes now.
  - `rememberedRoutesHoldOnlyWhereTerrainIsUnknown` (synthetic world): a route holds through unknown cells and breaks at
    a known wall.
- The out-of-range test first failed for a different reason: a rescan started right after a block change read the
  section copy from before it (by design it may lag by `DIRTY_TTL_TICKS` + `GRACE_TICKS`). The test now waits that out.
- Eight town namespaces (`frostedheart_town_connectivity`, `_buildings`, `_institute`, `_persistence`,
  `_town_scheduling`, `_town_view`, `_citizen_roads`, `_labor_dispatch`): all 79 required tests pass.
- Real client (`runClient -I Scripts/dev_client_bridge.init.gradle`, creative flat world `road-links-test`, world spawn
  moved to 5000/5000 so spawn chunks do not hold the town):
  - `roads setup`: 5 houses, 4 linked with 3 routes; the one across the gap does not work.
  - `roads outpost 600 0`: a sixth house; its chunk unloads.
  - With the outpost out of range, removing the stairs unlinked the house at (3, 70, 23) at the next periodic rescan
    (3 working, 2 routes). Rebuilding them relinked it (4 working, 3 routes).
  - Teleported to (3000, 100, 3000): every building unloaded; after a full rescan period still 4 working, 3 routes.
  - Saved and quit, reopened while still far away: 4 working and 3 routes straight from the save. The team file holds
    `roads` with 3 routes.
  - Back at the town, the J overlay shows the same roads. No errors from road scans or town saving in the client log.
  - Screenshots: `run/agent/screenshots/*-01-town-linked.png`, `*-02-stairs-broken.png`, `*-05-after-reload-overlay.png`
    in this worktree; the world is also copied to the main checkout's `run/saves/road-links-test`.

## Remaining

- Only the route the last scan used is remembered. If it breaks in loaded terrain while a second road runs through
  unloaded terrain, the building counts as unlinked until that terrain loads and is scanned.
- A road removed less than one scan interval before its chunk unloads takes effect on the next visit.
- Not tried on a multi-team dedicated server.
- `TownConnectivity.tick` also changes in the uncommitted mines-off-roads work in the main checkout; expect to merge by
  hand.

## Documentation

- `docs/town/building-data.md`, "道路连接与能量塔": remembered routes replace the grant-only rule; the saved road view;
  status line updated.
