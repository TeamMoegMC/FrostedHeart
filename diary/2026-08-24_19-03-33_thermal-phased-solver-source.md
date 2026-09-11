# Thermal Phase D Combined Solver And Source

- Time: `2026-08-24 19:03:33 +08:00`
- Author: `Codex; primary engineering agent`
- Status: `completed`
- Scope: `content.climate.thermal.solver`, `content.climate.thermal.source`, combined PR 4+5 correctness foundation, living climate documentation, and the active thermal plan

## Completed

- Added pair and fixed-boundary exchange kernels using seconds and `-expm1`, bounded buoyancy conductance, compiled forward/reverse `ThermalSweep`, sealed `SolveEpoch` input cuts, latest-only single-in-flight scheduling, and bounded `TIME_DEGRADED` step planning.
- Added the packed physical-source registry, versioned ports/bindings, independent node power accumulators, exact event-time `integral(P dt)`, impulse and rebind handling, unload settlement, bounded retained segment history, checksum-only cumulative energy, and explicit history-exhaustion loss.
- Added `ThermalSourceTimeline` as the concrete bounded source command owner. Register, power, enable, rebind, impulse, cold-route, and unload commands are consumed in game-tick and source-watermark order.
- Integrated source application and transport in `ThermalStepExecutor`. A zero-duration source cut runs before transport, every bounded substep receives source energy before its sweep, and a degraded suffix still receives complete source energy without inventing transport or phase work.
- Removed the old generic `SourceEnergyApplier`, `IntervalOperator`, and empty phase callback. Removed the unused per-event `SourceMutation` result allocation and the report field that always claimed zero executed phase steps.
- Corrected applied-watermark ownership. Non-source streams are checked before execution; the timeline proves and advances its own source cut; the report returns the actual source watermark for scheduler completion.
- Fixed a queue defect found by the combined integration tests: successful offers calculated `nextOfferedWatermark + 1` without storing it, so every command had watermark `1` and only the first command of a sealed cut was consumed.
- Changed bulk node-energy delivery so an exception leaves the undelivered energy pending for retry instead of clearing it first.

## Decisions

- PR 4 and PR 5 are one acceptance unit. Source timing is not an opaque solver callback, and source semantics cannot be reviewed independently of tick-boundary execution.
- `NodeEnergyConsumer` remains only as the narrow final node-enthalpy delivery boundary until the dimension runtime supplies the real `ThermalCellArena` node mapping. `SourceResyncReplayer.ReplayTarget` remains only for recovery delivery to mesh, internal reservoir, or explicit loss. Neither boundary owns event order, cadence, or source state.
- Private immutable source command records remain in the correctness implementation for readability. Primitive event batches can replace them only if runtime profiling identifies this admitted command path as material.
- Sleep/wake, hard active-state caps, actual mesh/source binding, cross-stream geometry/source frame-cut application, mailbox/publication, and production adapters belong to the later dimension runtime and Minecraft integration. They were not represented by placeholder callbacks here.
- The legacy system remains the only gameplay authority.

## Validation

- `gradlew test runGameTestServer --no-daemon --console=plain`: build successful on Java 17.
- Full repository JUnit: `697/697`; thermal JUnit: `169/169`; zero failures or errors.
- Forge GameTest: all `15/15` required tests passed.
- Solver/source packages contain `45` focused JUnit cases. New combined cases cover non-empty source plus non-empty sweep sharing the same enthalpy array, monotonic queue retry, same-tick impulse/rebind order, actual applied source watermarks, and destination-failure energy retention.

## Documentation Impact

- Updated `docs/climate/data-lifecycle-and-integration.md` with Phase D ownership, code anchors, test counts, removed abstractions, and the remaining production boundary.
- Updated the active thermal plan so PR 4+5 is one completed correctness unit and the next implementation gate is Phase E / PR 6 FarField, followed by PR 7 runtime ownership.
- No player-facing behavior, persistence, configuration, networking, or gameplay documentation changed because the V1 path is still disconnected from production authority.

## Remaining

- Implement Phase E / PR 6 FarField holdout reference fixtures and reject uncalibrated `STATIC_IMPEDANCE` if the gate fails.
- Implement PR 7 dimension runtime ownership, actual `ThermalCellArena` source binding, geometry/source same-tick frame cuts, bounded mailbox/publication, sleep/wake, and global/dimension memory admission.
- Do not run production CPU or retained-memory acceptance against this correctness-only executor; those measurements require the runtime path and representative workloads.
