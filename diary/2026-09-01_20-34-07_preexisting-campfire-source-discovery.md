# Pre-existing campfire source discovery

- Time: `2026-09-01 20:34:07 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `lit campfire discovery, sparse thermal residency, and neighboring Brick propagation`

## Completed

- Traced the reported infrared hole through the saved chunk at `(0,3)`: the
  campfire Brick retained a large residual while its positive-X neighbor had no
  stored residual, despite an open eight-block Air face.
- Reproduced the lifecycle failure with a Forge GameTest in which lit campfires
  existed before the lazy `MinecraftThermalInput` runtime started. Before the
  fix the source remained exactly at natural temperature because neither chunk
  load nor BlockState mutation discovered it.
- Added position-staggered 20-tick observation from the existing lit campfire
  `cookTick` to `MinecraftThermalInput.onCampfireTick`. The first observation
  registers the source; unchanged observations do not enter the dirty queue.
- Extended engine regressions to verify that a residency-expanded mixed
  neighbor keeps receiving heat after migration and that its infrared Brick
  epoch advances.
- Updated the thermal runtime plan and living source/runtime documentation.

## Decisions

- Reuse the existing campfire server tick instead of scanning loaded chunks,
  adding player-driven residency, retaining another index, or introducing a
  scheduler. Steady cost is one active-runtime lookup, BlockState read, and
  source-map probe per lit campfire per second.

## Validation

- Red/green Forge GameTest: pre-existing campfires failed with
  `source=10.0, natural=10.0` before the hook and passed after it; all 16 required
  GameTests passed.
- Complete thermal JUnit selection plus `InfraredPacketCodecTest` and
  `InfraredViewRendererStateTest`: passed.
- `compileJava` and `compileGameTestJava`: passed.

## Remaining

- Reopen the original save with the rebuilt classes and visually confirm that
  the formerly missing neighbor Brick is admitted and leaves the invalid-blue
  state after normal propagation time.
