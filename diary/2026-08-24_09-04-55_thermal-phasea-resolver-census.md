# Thermal Phase A Resolver Census Integration

- Time: `2026-08-24 09:04:55 +08:00`
- Author: `Codex; primary integration agent`, assisted by `phasea_core_contract`, `phasea_shape_resolver`, and `phase0_writer_census` (`OpenAI gpt-5.6-sol, ultra`)
- Status: `partial`
- Scope: `content.climate.thermal.geometry`, `content.climate.thermal.profile`, Phase A Forge GameTests, resolver census reporting, and the active thermal plan

## Completed

- Added bounded dependency masks, immutable audited resolver snapshots, explicit resolution reasons, correctness-width thermal signatures, primitive `int` signature IDs, and a resolver extension interface.
- Added the clipped `VoxelShape` adapter, generic state-static resolver, loaded-only Forge snapshot capture, common Vanilla geometry fixtures, waterlogged channel separation, and moving-piston dynamic exclusion.
- Added a Forge census over the active registry. Its report is `build/reports/thermal-phase-a/resolver-census.json`.
- Updated the active thermal plan with the implemented scope, measured census, and remaining Phase A gates. No living documentation changed because the V1 runtime is not wired to gameplay.
- Re-ran the existing Phase 0b Forge capture. It still records that the legacy player constructor loads a missing four-chunk footprint; no further legacy sampler optimization was performed.

## Decisions

- Every `hasDynamicShape() == false` state uses the same generic resolver, including modded states. No per-mod static allowlist or compatibility matrix is maintained.
- Explicit override and bounded contextual resolver interfaces remain available for meaningful exceptions. Unregistered dynamic geometry stays observable unsupported; moving piston stays `UNRESOLVED_DYNAMIC`.
- Non-empty `FluidState` uses a conservative full-block airflow blocker in the SELF_ONLY path and does not call neighbor-dependent `FluidState#getShape`.
- The observed maximum of four local air regions does not freeze production `Rmax`; contextual outputs and performance evidence are still missing. IDs remain `int`.

## Validation

- Java 17 thermal JUnit: `84/84` passed, with no failures, errors, or skips.
- Java 17 Forge GameTest: `15/15` required passed; Phase A contributed four geometry tests and one census test.
- Census: `2,392` blocks, `84,147` states, `82,198` generic-static resolved states, `259` unique geometry signatures, maximum observed local-region count `4`, `1,937` dynamic unsupported states, and `12` moving-piston unresolved states. Geometry-only scan time was `326,079,500 ns`.
- Loaded-only remote `NEIGHBOR_26` capture retained `27` unloaded sentinels and did not load the target chunk.
- `git diff --check` passed apart from the repository's existing LF-to-CRLF warning for the shared plan.

## Remaining

- Implement physical explicit profiles and real registered contextual resolver outputs, then repeat the census with non-neutral metadata.
- Measure datapack reload peak, resolver/Brick timing, allocation, and retained memory before freezing `Rmax` or packed widths.
- Keep Phase A `in-progress`; this slice does not enable production thermal authority.
