# Town section snapshot TTL and grace cache

- Author: Codex
- Date: 2026-11-13 UTC
- Status: implemented; production/test compilation and all 41 GameTests passed
- Scope: shared road section snapshots, dirty deadlines, stale-while-revalidate refresh and lifecycle

## Changes

- Add SectionSnapshotCache: 100-tick base TTL, 20-tick dirty TTL and 40-tick grace. Reads never extend deadlines. Dirty writes shorten the remaining deadline without sliding it on repeated writes.
- Fresh reads reuse bytes without refresh. Grace reads return the old completed snapshot immediately and enqueue one FIFO tail refresh; repeated accesses do not duplicate or move existing work. At hard expiry remove the entry and obsolete stale-only jobs; cold readers coalesce onto one future and await new data.
- Refresh replaces immutable byte storage rather than editing old arrays. Per-level LRU capacity is 1024 sections, about 4 MiB of classified bytes plus metadata. Unloaded masks are not stored as shared entries.
- Add TownSectionSnapshots registry and main-thread servicing before town runtime ticks, globally bounded to two section loads per dimension per tick, including repeated service-call protection. Age/refresh service runs even when a town has no active search batch.
- TownConnectivity workers retain local leases into the shared cache across sources and rounds. Local readers observe dirty deadline shortening, entry retirement and refresh without consulting the global lock for every block. Source searches still never read the world off-thread.
- Add LevelChunkMixin_TownSectionCache to actual successful state writes, covering player changes, commands, fluids and neighbour updates; section-edge dependencies are dirtied too. Chunk unload retires matching entries, neighbour chunk lifecycle dirties surrounding context, datapack reload shortens existing lifetimes, and level/server shutdown closes pending requests.
- Adapt development path probes to the new shared service and Snapshot.load helper. Probe schema 2 separates future lookups from actual copies and adds cache statistics; replay no longer assumes one future per copy. Existing uncommitted performance tools/reports remain in the workspace.
- Update docs/town/building-data.md and src/gametest/README.md. Append a baseline-version note to the earlier natural-world performance report; no new performance speedup is claimed.

## Validation

- ./gradlew --offline compileJava compileGameTestJava --no-daemon: BUILD SUCCESSFUL, 1m 22s. Existing mixin/deprecation/resource-duplication warnings remain.
- ./gradlew --offline runGameTestServer -PgameTestNamespaces=frostedheart_town_connectivity,frostedheart_citizen_roads,frostedheart_section_cache -PgameTestDirectory=build/town-section-cache-gametest --no-daemon: All 41 required tests passed; BUILD SUCCESSFUL, 3m 17s.
- 13 new cases cover fresh TTL boundaries, immediate grace returns, hard expiry, unaccessed expiry, repeated dirty writes, FIFO/budget/deduplication, coalesced readers, LRU eviction, unloaded masks, invalidation during load, loader failure/clock rollback, real world state-write dirty TTL and section-boundary invalidation.
- Logs: /tmp/town-section-cache-final-compile.log and /tmp/town-section-cache-gametest.log (generated local artifacts). GameTest server exited normally; no graphical client was launched for this change.
- git diff --check passed. No repeat of the natural-world performance matrix; prior baseline timings do not measure this cache. No commit or push requested/performed for this task.
