# Thermal dirty Brick rebuild

- Time: `2026-08-26 05:45:49 +08:00`
- Author: `Codex; OpenAI; primary implementation and verification agent`
- Status: `completed`
- Scope: `Minecraft thermal geometry mutation batching, Page fragments, Air adjacency, material dependencies, and replacement sweep lifecycle`

## Completed

- Added a fixed first-dirty `+5 tick` geometry rebuild deadline. Later mutations join the same batch without extending it, while source-only solves retain the last released geometry watermark.
- Kept the initial coarse Page split as a one-time full build, then retained independent primitive `4x4x4` arena fragments and rebuilt only dirty Bricks.
- Cached Air pair operations by owning Brick and refreshed only the changed Brick plus negative-axis interface owners.
- Collected material candidates during the same 64-block geometry traversal and refreshed material contacts only for the changed Brick and its six face neighbors.
- Preserved unchanged arena slots and enthalpy, and delayed old fragment release until the replacement sweep committed.

## Decisions

- Geometry batching is deadline-based rather than debounce-based so continuous door or mining mutations cannot postpone rebuilding indefinitely.
- No JMH harness, profiler table, global `BlockState` lookup table, or additional runtime subsystem was introduced.
- Correctness validation establishes semantic equivalence and bounded work; CPU and allocation improvement percentages require a fresh controlled production JFR.

## Validation

- Java 17 offline Gradle: `compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `205/205` passed.
- Forge GameTest: `14/14` required passed, comprising `13` thermal scenarios and `1` Frosted Research scenario.
- Targeted tests cover fixed-deadline sealing, local fragment replacement, unchanged slot and enthalpy stability, material/phase migration, and Page lifecycle release.

## Remaining

- Record the same door/mining workload in a fresh production JFR before claiming a measured CPU or allocation reduction.
