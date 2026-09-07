# Thermal static-state mutation filter

- Time: `2026-08-26 05:02:08 +08:00`
- Author: `Codex; OpenAI; primary implementation and verification agent`
- Status: `partial`
- Scope: `Minecraft thermal mutation invalidation and measured topology allocation hot paths`

## Completed

- Confirmed from the production JFR and mutation path that every distinct `BlockState` previously invalidated Page geometry, including source-only changes such as `CampfireBlock.LIT`.
- Kept physical source observation first, then skipped Page revision, radiation invalidation, geometry-ring input, and urgent topology solve when two states of the same automatically trusted static block have unchanged compiled thermal semantics.
- Used the existing canonical `BlockState` and cached static collision-shape objects directly. No global `BlockState` ID table, per-state signature table, per-Page filter state, or new runtime class is retained.
- Removed measured iterator and aggregation allocation from conservative air and implicit adjacency compilation without changing geometry or conductance formulas.
- Extended the existing Minecraft integration GameTest to distinguish exact-state equality, thermally equivalent Campfire lit state, changed Door open state, changed stair half, and air-to-solid replacement.

## Decisions

- `oldState == newState` remains the no-op gate. Door open/closed and Campfire lit/unlit are different canonical `BlockState` objects, so state identity alone cannot decide thermal geometry equality.
- For `hasDynamicShape=false`, compare the state-supplied cached collision shape rather than hard-coding `DoorBlock`, stairs, fences, panes, or mod block subclasses. Fluid occupancy, radiation occlusion, material class, and recipe-compiled phase profile remain independent invalidation inputs.
- Dynamic shapes, custom resolver cuts, off-thread changes, block replacement, and any changed thermal semantic retain the existing conservative invalidation path.

## Validation

- Java 17 offline Gradle: `compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain` completed successfully.
- Thermal JUnit: `204/204` passed.
- Forge GameTest: `13/13` required passed, comprising `12` thermal scenarios and `1` Frosted Research scenario.
- `git diff --check` reported no whitespace errors before the final documentation update.

## Remaining

- Record a fresh controlled production JFR. Tests prove behavior and lifecycle correctness but do not yet quantify the reduction in `rebuildPage` CPU or allocation rate.
