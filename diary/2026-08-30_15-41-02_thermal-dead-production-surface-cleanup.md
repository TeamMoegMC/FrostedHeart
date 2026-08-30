# Thermal dead production surface cleanup

- Time: `2026-08-30 15:41:02 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `new thermal runtime production references, signature capture, solver helpers, and focused tests`

## Completed

- Audited all `74` remaining thermal production files for zero-reference types,
  methods, fields, and test-only public entrances.
- Deleted `GeometrySummaryCache`, `DependencyOffsetMask`, `ResolverBlockView`,
  `ThermalSignatureResolver`, and `ThermalSignatureResolverDispatcher`, plus
  their four prototype-only test classes.
- Replaced the empty dispatcher and reusable dependency cubes with direct
  loaded-state resolution through `StateStaticThermalResolver`.
- Removed test-only geometry, solver, and signature convenience methods, one
  unused solver helper, three never-emitted geometry flags, and unnecessary
  public constant visibility.
- Kept tests on actual result structures and production kernels rather than
  retaining production APIs for test convenience.

## Decisions

- Static block states remain the only configured resolver path. Dynamic shapes
  retain the existing conservative unsupported behavior.
- Single-position capture still uses `getChunkNow`; the simplification does not
  introduce chunk loading or a world scan.
- Kept the exact-tick signed-joule impulse contract temporarily because an older
  explicit plan required it. It currently has no gameplay producer and remains
  the only non-Forge zero-production-caller exception pending a user decision.
- No living climate document changed because none documented the removed empty
  extension branches and gameplay behavior is unchanged.

## Validation

- Thermal-scoped diff: `+163 / -1630`, net `-1467` lines.
- `gradlew.bat compileJava compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain`: successful.
- Thermal JUnit: `96/96`, zero failures, errors, or skips.
- Forge GameTest: all `14/14` required tests passed, including async signature
  capture, sequence, mutation, source, admission, and query scenarios.
- Deleted type names have zero remaining compiled class files.
- `git diff --check`: passed.

## Remaining

- Decide whether the intentionally retained impulse contract should gain a real
  gameplay producer or be removed end to end.
