# 100-player load test r4 on the climate-cover branch, with bounded scout trips

- Time: `2026-11-15 13:01:58 -0600`
- Author: `Claude (Anthropic Opus 5.5); implementer, decisions by the project owner`
- Status: `run completed; attribution of late admission refusals open`
- Scope: `ThermalNetworkLoadServer` (fixture), `Scripts/thermal-network-load.init.gradle`, a load run of branch
  `claude/cover-climate-isolation` at `47d2c7c3e`
- Follows: [passive query fast path](2026-11-15_00-00-40_thermal-passive-query-fast-path.md) (runs r1–r3)

## Why

The owner wants a 100-player load test before merging the branch into master. On this machine r1–r3 never finished:
scouts teleport every 600 ticks to `origin(target) + (8192 + cycle × 256, …)`, so every trip generates about 845 new
chunks (5 groups, view distance 6), and world generation and saving fall behind. r3 dropped to 3.6–7.8 TPS when trip
8 reached ungenerated terrain and the watchdog stopped it at 6000 ticks. The owner asked to reduce the exploration.

## Completed

- New fixture setting `frostedheart.thermalLoad.scoutSites` (`-PthermalLoadScoutSites`, default 0 = old behaviour):
  with K > 0 the trip site is `cycle % K`, for both the target base and the offset, so trips after the first K revisit
  terrain earlier trips generated. `measuring.json` records the value.
- Run r4 in the worktree's `build/thermal-network-load-r4`, a copy of r3's world and configuration (without its
  `stop.json`, logs and results). r2 had generated trips 0–7 in that world, so `scoutSites=8` generated no new
  terrain. Same parameters as r3: 100 players, 25 bases, 1600 sources, 400 s measured plus 30 s recovery with scouts
  returned home, 8 GiB heap, 5 processors, view and simulation distance 6, `sync-chunk-writes=true`. The owner's game
  was closed; the linked E2E session held its real clients until the run ended.

## Results

- All 8000 measured ticks and the recovery completed; all 100 bots stayed joined (`qualified: true`). 18.93 TPS;
  server tick p50/p95/p99 26.1/38.5/58.3 ms, one 1.7 s spike in the fixture's own work (`fixtureMainMs` max 1685 ms).
- Thermal main thread per tick, compared with r3's first 4800 ticks (same machine, master `37adbc659`):

| per tick | r3 0–4800 | r4 |
|---|---|---|
| passive queries, count / time | 1847 / 321 µs | 2067 / 234 µs |
| `thermal_input_tick` | 130 µs | 145 µs |
| `player_thermal_update` | 194 µs | 194 µs |
| thermal main p50 / p95 / p99 / mean | 0.52 / 1.81 / 2.98 / 0.73 ms | 0.43 / 1.92 / 2.86 / 0.65 ms |

  The natural computation of a passive query (cover lookup, covered climate) runs before `thermal_passive_query` and is
  not inside any instrumented stage; it is inside the tick time.
- Resident samples: age p99 23 ticks, no age violations, 294 of 34306 unavailable (0.86%).
- Memory: accounted thermal memory stayed at 90–110 MB for most of the run, rose to 118–131 MB after about 4800 ticks,
  and from 6800 ticks reached the 128 MiB target: 149 admission refusals by 8000 (r3: 0 refusals up to 6000 ticks,
  accounted peak 137.3 MB; Windows r10: 0, peak 123.2 MB). Peak sampled heap 8.3 of 8.6 GB, 3.1 GB at the end.
- Recovery (scouts home): first all fresh after 120 ticks, but the last 10 s still had 90 of 910 samples missing and
  70 old, age p99 323 ticks (r10: tail 0 missing, 0 old, age p99 3).

## Reading

- No main-thread regression from the branch is visible; `thermal_input_tick` is about 15 µs higher, in line with the
  per-Section natural profiles and cover refreshes the branch added to the environment capture.
- The branch adds a float `natural` per field cell: about 4 of 67 accounted bytes per cell, 6% more field memory. That
  can bring the budget limit forward, but accounted memory also grows over the run on its own, and r3 never reached
  6800 ticks, so this run cannot attribute the late refusals or the recovery tail. A control run of master with the
  same r4 setup would.

## Documentation

- None: the load harness and its parameters are not documented under `docs/`; this entry records the new setting.

## Remaining

- Control run of master `37adbc659` with `scoutSites=8` on a copy of the same world, to attribute the refusals and
  the recovery tail.
- If the branch is the cause: store `natural` as a short (0.01 °C steps, half the cost) or derive it from the shared
  `NaturalProfile`.
