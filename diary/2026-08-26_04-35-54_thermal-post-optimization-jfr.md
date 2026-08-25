# Thermal post-optimization real-save JFR

- Time: `2026-08-26 04:35:54 +08:00`
- Author: `Codex; primary performance analysis agent`
- Status: `completed`
- Scope: `real-save thermal CPU, allocation, GC, and tick comparison`

## Completed

- Recorded `thermal-post-optimization-2026-08-26_04-28-36.jfr` from the production client path for `210 s`; the stable comparison window excludes the first and last `10 s` and covers `190.458 s`.
- Recomputed the previous `224.098 s` baseline and the new window with the same streaming analysis.
- Confirmed that `Resolution.componentAt`, `Face.values`, and maximum-size migration tuple arrays disappeared as dominant allocation sources.
- Thermal sampled allocation fell from `44.073 MiB/s` to `19.253 MiB/s`, while sampled server-thread allocation fell from `102.664 MiB/s` to `68.186 MiB/s`.
- G1 Old collections fell from `10` to `0`; humongous-allocation GC disappeared. Total GC pause fell from `4680.497 ms` to `387.003 ms`, and the longest pause fell from `500.428 ms` to `14.062 ms`.

## Decisions

- Treat allocation and GC optimization as successful, but do not declare overall thermal performance complete.
- The new workload sampled `rebuildPage` about `3.2x` more often per second than the baseline. Thermal sampled CPU share rose from `26.710%` to `55.494%`; tick rolling-average mean rose from `6.282 ms` to `10.258 ms`, P95 from `11.788 ms` to `20.129 ms`, and maximum from `21.304 ms` to `72.087 ms`.
- Do not infer a thermal retained-memory leak from whole-JVM post-GC heap growth (`1833-2069 MiB`) without a controlled workload or heap ownership evidence.
- Living climate documentation was not changed because this validation changed no runtime behavior or contract.

## Validation

- JFR contains `50,968` allocation samples, `4,675` execution samples, `70` garbage collections, `206` server-tick events, a shutdown event, and no data-loss event.
- Stable-window thermal allocation persisted between `9.363` and `30.796 MiB/s` across 30-second buckets; rebuild work was sustained rather than one startup spike.
- Remaining leading production allocation paths are material-boundary and topology compilation, including `combinedFaceMask`, `addFixedConductance`, `ImplicitAirAdjacency.addPair`, `AirRegionKey`, `AirMicrocell`, and topology collection maps/lists.

## Remaining

- Determine whether sustained Page dirtiness came from player movement/Page admission, real block mutations, or neighbor material propagation under the recorded workload.
- Eliminate the remaining immutable-list iterators in geometry aggregation.
- If ordinary stationary gameplay still rebuilds full Pages, replace whole-Page mutation rebuild with bounded dirty-Brick compilation while preserving Page transaction and migration authority.
- Repeat the same controlled stationary and movement workloads after the rebuild-frequency fix.
