# Thermal Phase A Core Resolution Contract

- Time: `2026-08-24 08:54:07 +08:00`
- Author: `Codex phasea_core_contract; OpenAI gpt-5.6-sol, ultra; core contract subagent`
- Status: `completed`
- Scope: `content.climate.thermal.profile` core values and pure-Java contract tests

## Completed

- Added the immutable 27-bit `DependencyOffsetMask` with mandatory `SELF`, predefined `SELF_ONLY` / `NEIGHBOR_6` / `NEIGHBOR_26`, explicit subsets, and reverse invalidation footprints.
- Added explicit resolved, unresolved, and conservative-unsupported outcomes with stable reasons, plus an immutable snapshot-backed `ResolverBlockView` whose audited access returns sentinels without retaining a World, chunk, callback, or supplier.
- Added unique declared-read accounting for contextual census and closure measurements; repeated reads count once while unloaded and missing declared cells remain observable reads.
- Added correctness-width local-region/signature values, primitive `int` signature resolution, an immutable deduplicated registry snapshot, and the `ThermalSignatureResolver` extension point for explicit or bounded contextual registrations.
- Added pure-Java tests for `27` affected centers, the `5^3 = 125` mutation read closure, the `6^3 = 216` cold-Brick snapshot halo, sentinel normalization, immutable snapshots, independent signature channels, and IDs above `65,535`.

## Decisions

- All legal dependency masks include the target state and stay inside its `3x3x3` cube.
- Snapshot capture and resolver access are separate: the snapshot stays immutable, while one short-lived access audit prevents an ignored missing/unloaded/out-of-mask read from publishing a resolved opening.
- Workers receive primitive signature IDs only. ID and region correctness fields remain `int`; no packed width is selected before census evidence.
- The generic state-static policy, Forge shape adapter, census, and GameTests remain integration concerns. The core exposes deterministic resolver IDs and registration contracts without per-mod compatibility logic.

## Validation

- `gradlew.bat test --tests "com.teammoeg.frostedheart.content.climate.thermal.*" --no-daemon --console=plain`: `84/84` passed with no failures, errors, or skips.
- Core-only profile tests: `11/11` passed; the integrated profile set including the state-static resolver passed `19/19`.
- `git diff --check`: passed with only the existing LF-to-CRLF plan warning.
- Per-file `git diff --no-index --check` over all `11` new core/profile source and test files: no whitespace errors.

## Remaining

- Phase A remains in progress until the Forge census/report, loaded-only contextual fixtures, GameTests, and performance evidence are complete.
- No living documentation changed because this slice has no production wiring or player-visible behavior; the active implementation plan remains the intended-work authority.
