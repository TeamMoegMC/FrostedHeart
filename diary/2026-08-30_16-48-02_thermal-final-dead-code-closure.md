# Thermal final dead-code closure

- Time: `2026-08-30 16:48:02 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `completed`
- Scope: `all new thermal production sources, main classfiles, retained IMPULSE contract, focused tests, and living architecture documentation`

## Completed

- Re-audited every thermal top-level type, declared method, declared field, and
  every method reference emitted by all main-source classfiles.
- Removed the remaining write-only and test-only payloads: Brick geometry
  summaries, compiler diagnostic wrappers, duplicate geometry metadata,
  geometry event ticks, query medium/flags/topology state, radiation confidence
  and flags, arena cell flags, unused Air/material profile writes, source
  binding variants, and non-production numerical wrappers.
- Converted array-backed material/profile records and analytic/FarField records
  to explicit immutable classes so they expose only methods with real callers.
- Reduced `QueryPublication` double-buffer payload from `40` to `24` bytes per
  arena slot and removed one byte of cell flags per arena slot.
- Updated the living thermal architecture and active implementation plan.

## Decisions

- `ThermalSourceMode.IMPULSE` is retained. The user explicitly confirmed the
  exact-tick signed-joule contract, so `emitSourceImpulse` is a named whitelist
  entry rather than dead code even though no current gameplay producer calls it.
- Forge subscriber methods and interface implementations are roots even when
  bytecode calls target the annotation or interface rather than the concrete
  method.
- Compiler-generated enum methods and necessary record methods are not treated
  as handwritten production surface. Records with unused mutable-array accessors
  were converted to explicit classes.

## Validation

- Source census: `73` thermal production files; no zero-reference type or field,
  and no zero-call method outside the Forge/IMPULSE whitelist.
- Main classfile census: `14` apparent unreferenced top-level methods, classified
  exactly as `7` Forge subscribers, `6` interface implementations, and
  `DimensionInputAccumulator.emitSourceImpulse`.
- Thermal-scoped diff: `+503 / -2361`, net `-1858` lines.
- `gradlew.bat compileJava --offline --console=plain`: successful.
- `gradlew.bat compileGameTestJava test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" runGameTestServer --offline --console=plain`: successful.
- Thermal JUnit: `96/96`, zero failures, errors, or skips.
- Forge GameTest: all `14/14` required tests passed.
- `git diff --check`: passed after source edits; final documentation check was
  repeated before handoff.

## Remaining

- None.
