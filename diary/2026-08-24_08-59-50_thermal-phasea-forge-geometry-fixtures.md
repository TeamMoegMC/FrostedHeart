# Thermal Phase A Forge Geometry Fixtures

- Time: `2026-08-24 08:59:50 +08:00`
- Author: `Codex phase0_writer_census; OpenAI gpt-5.6-sol, ultra; Phase A GameTest subagent`
- Status: `completed`
- Scope: `content.climate.thermal.profile.minecraft` Forge GameTests

## Completed

- Added four Forge GameTests using the production `StateStaticThermalResolver`, `LoadedOnlyResolverSnapshot`, `DependencyOffsetMask`, and `ResolverBlockView` APIs with the existing `phase0a_empty` structure.
- Covered captured Vanilla air, solid, slab, stairs, Door, Trapdoor, fence, pane, one-layer snow, and waterlogged partial-block states. Door and Trapdoor state changes alter geometry, north-south fence and pane collision shapes retain two separate side regions, and one-layer snow follows its empty collision shape.
- Proved that a waterlogged slab creates no air opening under the current conservative fluid fallback while medium, material-contact, and radiation metadata remain independent channels.
- Covered complete loaded `NEIGHBOR_26` capture, out-of-mask and outside-build-height sentinels, a fully unloaded remote `NEIGHBOR_26` footprint without a chunk load, static piston base/head resolution, moving-piston exclusion, and unsupported BlockEntity access.

## Decisions

- Vanilla fixtures are representative tests of the generic static path, not namespace-specific policy or a per-mod compatibility matrix.
- The GameTests contain only placement and assertion helpers; resolver, snapshot, mask, status, and policy behavior remain owned by production APIs.
- No Create fixture was added because Phase 0a already proves that assembled contraptions are air while moving and re-enter through ordinary world mutation on disassembly.

## Validation

- `gradlew.bat compileJava --no-daemon --console=plain`: passed.
- Focused geometry/profile JUnit suite: `40/40` passed with no failures, errors, or skips.
- `gradlew.bat runGameTestServer --no-daemon --console=plain`: all `15/15` required tests passed; the new Phase A geometry batch passed `4/4`.
- `git diff --check`: passed with only the existing LF-to-CRLF plan warning.

## Remaining

- Phase A still requires the separately owned census/performance evidence and must not be treated as production runtime wiring.
- No living documentation changed because these fixtures do not alter player-visible behavior, persistence, configuration, or cross-system runtime contracts.
