# Knowledge snapshot coalescing and server access projection cache

- Time: `2026-10-19 00:00:13 -0500`
- Author: `Claude Opus 5.5 (Anthropic); implementation agent`
- Status: `completed`
- Scope: review items P1-1 steps 1 and 3 and P1-2 of
  [the knowledge system review](../discussion/2026-10-18_23-01-04_knowledge-system-development-review.md) (untracked in
  the main checkout when this entry was written). Code: `api/TeamResearchService`, `api/KnowledgeDataAPI`,
  `api/ClientKnowledgeDataAPI`, `knowledge/TechnologyAccessResolver`, `knowledge/KnowledgeSyncSnapshot`,
  `knowledge/KnowledgeRuntime`, `knowledge/KnowledgeService`, `knowledge/network/KnowledgeSnapshotPacket`,
  `knowledge/network/KnowledgeWorkbenchActionPacket`, `knowledge/client/KnowledgeClientState`, `ResearchHooks`,
  `FHResearch.reindex`, `handler/ResearchCommonEvents`, `FRNetwork`, deleted `network/FHKnowledgeDataSyncPacket`.
  Docs: `docs/knowledge/state-and-api.md` and three `docs/research` files.

## Completed

- Re-verified the review's facts at `a8d89e5f2` before changing code. All held:
  - Every `notifyChanged` called `TeamResearchService.sync`, which built a full snapshot even for teams with nobody
    online.
  - The snapshot carried `team_data` (the whole `KnowledgeState`) besides `archive_view`. On the client,
    `ClientKnowledgeDataAPI.getData()` was reachable only through the client branch of `KnowledgeDataAPI.getData(Player)`,
    which no code calls with a client player. `knowledgeProjection()` had no caller.
  - Snapshots per operation: `link` with a new discovery 2, `initialize` N+1, `grantResult` 2, login 2. `/reload` sent
    one for every team, offline ones included, before the final tags, and then one more per online team.
  - `ResearchHooks.canUseBlock`, `canUseRecipe`, `canFormMultiblock` and `hasMultiblock` rebuilt the projection on
    every call, walking every archive key.
- P1-1 step 1:
  - `KnowledgeSyncSnapshot` is now `catalog_revision`, `knowledge`, `technology`, `archive_view`. The client no longer
    installs a knowledge component into `CClientTeamDataManager`.
  - `ClientKnowledgeDataAPI.getData()` is removed, and `KnowledgeDataAPI.getData` takes a `ServerPlayer`.
  - `FHKnowledgeDataSyncPacket` and its `knowledge_data` registration are deleted; the client install moved to
    `KnowledgeSnapshotPacket.install`. The never-sent `snapshot` branch of `KnowledgeClientState.receiveReply` is gone.
  - `TeamKnowledgeData` is unchanged.
- P1-1 step 3:
  - `TeamResearchService.sync(team)` now only marks the team pending and drops its cached projection.
  - `KnowledgeRuntime.tick` calls `TeamResearchService.flush()` at `ServerTickEvent` `END`. The flush builds one
    snapshot per pending team that has an online member with a connection, and logs a failing team without stopping
    the others.
  - `send(player, team)` serves login (`LOWEST`) and desk `REFRESH`. It sends at once and folds in a pending change.
  - `KnowledgeWorkbenchActionPacket` calls `flush(team)` before every reply, so the snapshot still arrives first.
  - Removed: the `NORMAL`-priority login sync in `ResearchCommonEvents.syncDataToClient`, the extra sync in
    `grantResult`, `KnowledgeService.batchDepth`/`syncPending`, and `ResearchHooks.syncKnowledgeSnapshots`.
  - `ServerReload` now only drops caches. `KnowledgeRuntime.datapackSync` marks the online teams once the tags are
    final.
- Snapshots per operation, before → after:

  | Operation                                         | Before                           | After                                    |
  |---------------------------------------------------|----------------------------------|------------------------------------------|
  | `link` with a new discovery                       | 2                                | 1                                        |
  | `initialize`                                      | N+1                              | 1                                        |
  | `grantResult`                                     | 2                                | 1                                        |
  | Login                                             | 2                                | 1                                        |
  | `/reload`                                         | every team, then each online team | 1 per online team                        |
  | Batch learn/delete request, research completion   | 1                                | 1                                        |

  More generally, every change to one team within a tick now shares one snapshot.
- P1-2:
  - `TechnologyAccessResolver` caches one projection per team. Fake players and team-less machines share one more
    ownerless entry.
  - An entry records the `TeamKnowledgeData` and `TeamResearchData` instances, `mutationRevision()`, and the
    `ResearchResultCatalog` and `KnowledgeDefinitions` snapshots. Any difference rebuilds it.
  - Explicit invalidation happens on the sync mark (which also covers legacy grants/resets, team changes and the
    post-reload marks), in `ResearchHooks.reload`, in `FHResearch.reindex`, and at the end of `ServerReload`.
  - While a team is pending, reads rebuild without caching.
  - `KnowledgeSyncSnapshot.create` reuses the cached projection.
- Tests:
  - `KnowledgeSyncSnapshotTest` now asserts the exact encoded keys and the round trip.
  - New `TechnologyAccessResolverCacheTest` has four tests:
    - entries are reused until knowledge changes, including a direct `acquireDesign`;
    - catalogue and definition installs rebuild entries;
    - a pending team is not cached until its flush;
    - `invalidateAll` drops the ownerless entry too.
  - New GameTest `KnowledgeLoginGameTests.teamMarksCoalesceIntoOneSnapshot` counts Frosted Research channel packets on
    an embedded connection. Two marks send nothing until one flush sends exactly one transfer, and
    `send(player, team)` carries and settles a pending mark.
- Left a hand-over note for the parallel P0-1 persistence session in the shared session notes. The cache relies on
  every archive mutation incrementing the `KnowledgeState` revision. `KnowledgeState.java` and `Observation.java` were not
  edited, and the persisted format is unchanged.

## Decisions

- Cache entries are validated by input identity plus state revision, not only by explicit invalidation calls.
  Catalogue and definition installs replace immutable snapshots, and every `KnowledgeState` write bumps
  `mutationRevision()`. So `ResearchResultCatalog` and `KnowledgeDefinitions` need no hooks into the resolver, and
  direct `TeamKnowledgeData` writes cannot leave a stale entry. `TeamResearchData` has no revision, so it relies on the
  sync marks that its access-effect grants and resets already send.
- A pending team is not cached. An operation can change state after its own mark within the same call: an infinite
  research resets right after granting. Pending reads therefore stay exact until the flush. For the rest of that tick
  the cost equals the old per-call rebuild.
- The workbench flushes before every reply, not only before `RETRIEVE`. The client sends batch requests one after
  another, so this adds no snapshot compared with before and keeps a single ordering rule.
- `ServerReload` no longer marks teams. Reload listeners can finish ticks before tags are bound, so a mark there could
  flush a snapshot built against old tags. `OnDatapackSyncEvent` with no player follows every reload on a running
  server and marks the online teams.
- The client knowledge accessor is removed and `KnowledgeDataAPI.getData` is narrowed to `ServerPlayer`. Without
  `team_data` the accessor would always return empty data. Every caller already passed a `ServerPlayer`, and
  server-side behaviour is unchanged.
- `knowledge` and `catalog_revision` stay in the snapshot. They are small and outside this task, even though no client
  code reads them today.
- Removing `knowledge_data` shifts later message IDs. Client and server must run the same build, which the existing
  exact-version channel handshake already assumes.
- Documentation:
  - `docs/knowledge/state-and-api.md`: only the 「生命周期、同步与命令」 section changed.
  - `docs/research/architecture.md`: the projection row.
  - `docs/research/results-and-access.md`: the cache and legacy-sync sentences and the snapshot contents.
  - `docs/research/state-persistence-and-sync.md`: the code anchor, the packet table and the snapshot paragraph.
  - `Last verified` stamps are unchanged: in-game acceptance and other baseline items are still open. `design/` was
    not touched.

## Validation

- `./gradlew test --tests 'com.teammoeg.frostedresearch.*'`: BUILD SUCCESSFUL, 112 tests in 34 classes, 0 failures.
  The previous baseline was 108 tests in 33 classes.
- `./gradlew runGameTestServer`: BUILD SUCCESSFUL in 1m 30s, `All 106 required tests passed :)`. That is the 105
  existing tests plus the new one; both knowledge GameTests ran with FTB Teams.
- `./gradlew runGameTestServer -PwithoutFtb -PwithoutJei`, first run: `1 required tests failed :(`. The failure was
  `phaseRequestSurvivesSameBrickTopologyChurn` (thermal: "phase Block never completed during topology churn",
  `randomTickSpeed=3`), already recorded as intermittent in
  [the 2026-09-16 thermal entry](2026-09-16_16-00-58_thermal-unused-abstractions.md). The new knowledge test passed,
  and `firstLoginWaitsForTeamCreation` logged its designed skip.
- The same command, second run: BUILD SUCCESSFUL in 1m 13s, `All 106 required tests passed :)`.
- `./gradlew test` (all modules): 695 tests, 3 failed. The baseline was 691 tests, and the 4 new tests account for the
  difference. The failures are the three deterministic ones in `frostedheart.content` already recorded in
  [the baseline re-run](2026-10-18_22-31-13_knowledge-research-validation-baseline-rerun.md): `TownHousingPlanTest`,
  `WarehouseBuildingCodecTest` and `DormantChunkThermalStateTest`.
- `git diff --check`: clean.

## Remaining

- Not exercised with real clients: login, `/reload`, team changes and desk flows on a dedicated server with several
  players. This matches the open integrated-QA item in [known-risks.md](../docs/research/known-risks.md).
- Not done: P1-1 step 2 (acquisitions without element copies; persistence, owned by the parallel session) and step 4
  (observation equivalence signatures). Each snapshot still costs `KnowledgeView` O(inbox × archive × slot
  conditions), but now at most once per team per tick.
- Pre-existing and unchanged: an infinite research with `EffectCrafting`, `EffectBuilding` or `EffectUse` drops out of
  the projection as soon as `resetForRepeat` clears its completion. The old immediate snapshot was built before that
  reset, so the client showed access the server denied. With the end-of-tick snapshot, both now deny. This needs a
  design decision if the pack contains such research.
- `TechnologyAccessResolver.hasFinding` still builds a `KnowledgeProjection` per call. It is not a hot path today.
