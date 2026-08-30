# Thermal batch sequence restart-loop correction

- Time: `2026-08-30 15:05:43 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `DimensionInputAccumulator batch identity and thermal worker restart lifecycle`

## Completed

- Reproduced the repeated worker failure at the real
  `ThermalDimensionEngine.validateBatch` rejection branch with JDWP.
- Confirmed that the engine had completed sequence `1` at tick `32100`, while
  the next same-generation cut repeated sequence `1` at tick `32120`.
- Fixed `DimensionInputAccumulator.seal` so its sequence field advances after
  each sealed cut.
- Added a focused producer test covering two consecutive cuts and the first cut
  after replacement with a new dimension generation.

## Decisions

- Kept generation restart ownership unchanged: a new accumulator and engine both
  restart their sequence at `1`.
- Did not change mailbox, worker-pool, topology, or validation behavior because
  runtime evidence showed a producer-local duplicate rather than a concurrency
  or topology failure.
- Added no production diagnostics, counters, compatibility paths, traversals, or
  test-only production APIs.
- No living climate document changed because its consecutive-sequence lifecycle
  was already correct; the implementation now conforms to it.

## Validation

- `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain`: successful.
- Thermal JUnit: `109/109`, zero failures, errors, or skips.
- Forge GameTest: `14/14` required tests passed.
- Real quick-play client loaded the existing world and ran from integrated server
  startup at `15:03:59` through `15:05:11`; the fresh log contained zero matches
  for the stale-batch failure, thermal worker failure, or non-live cell failure.
- `git diff --check`: passed after the source, test, plan, and diary changes.

## Remaining

- None.
