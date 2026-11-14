# Town largest-cluster working policy

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; all 17 GameTests passed
- Scope: road cluster selection, team tower ownership, client status synchronization

## Changes

- Compute connectivity components, then select working buildings. If any component reaches the owning team's tower, only the tower-connected buildings qualify. Otherwise, select the component with the most buildings, including towns without a tower or with an unreachable tower.
- Resolve only the current holder's generator via TeamTownData.townGenerator; foreign teams' towers are not graph endpoints.
- Break equal-size ties by the first controller in BlockPos.compareTo order.
- Keep actual connectedToTower separate from connectivityWorkable. The latter gates normal building work and non-residential settlement. Existing building-specific requirements still apply.
- Synchronize both states in incremental/full management views and carry connected/working sets separately in the road packet. Disk loads recompute topology.
- Green road/core highlights identify the fallback working cluster, cyan identifies actual tower connectivity, orange roads/red cores identify nonworking clusters.
- Recompute on tower/building/dimension changes even during the periodic cooldown; no tower no longer disables searches.
- Update docs/town/building-data.md. Add five tests covering absent/unreachable towers, smaller tower-connected clusters, deterministic ties and real separate team generator records. Packet regression now round-trips distinct working and connected sets.

## Validation

- Offline compileJava and compileGameTestJava passed (1m 21s).
- GameTest namespace frostedheart_town_connectivity in isolated build/town-cluster-policy-gametest: all 17 required tests passed. Local generated log: /tmp/town-cluster-policy-gametest.log.
- git diff --check passed.
- No graphical-client rendering or large-town performance validation claimed.
