# 千人同屏居民渲染技术方案

- Time: `2026-08-19 19:20:27 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `docs/town citizen rendering current-state audit and proposed implementation path`

## Completed

- Audited the active citizen client pipeline, visibility budgets, cache interpolation, fake-entity lifecycle, CPU batch geometry, lighting, and mandatory Flywheel dependency.
- Added `docs/town/citizen-rendering-at-scale.md` with a 1,000-visible target architecture, strict near-entity cap, Flywheel single-carrier instance design, CPU fallback, phased implementation, performance contract, test matrix, and risks.
- Updated the town documentation index and corrected stale visibility defaults plus misleading instancing/selection claims in `hybrid-simulation-architecture.md`.

## Decisions

- Keep the server SoA simulation, 96-block AOI, compact packets, and client snapshot cache; the next scaling work is primarily client rendering.
- Cap vanilla-quality fake entities independently from the server visibility budget, and render every non-promoted citizen through a data-only crowd backend.
- Prefer the repository's mandatory Flywheel 0.6 dependency behind a project-owned backend interface, using one client-only carrier for many instance slots rather than one entity per citizen.
- Retain the current CPU batch renderer as a compatibility and failure fallback until the instanced path passes Embeddium/Oculus and resource-lifecycle validation.

## Validation

- Cross-checked current claims against `ClientCitizenRenderer`, `FakeCitizenManager`, `ClientCitizen`, `ClientCitizenCache`, `SyncEngine`, `FHConfig`, `build.gradle`, `gradle.properties`, and `mods.toml`.
- Documentation links and whitespace were checked after editing; no Java behavior changed, so no build or test suite was required.

## Remaining

- Implement milestones M0 through M5 from `docs/town/citizen-rendering-at-scale.md`, beginning with the deterministic benchmark scene and strict fake-entity Top-K cap.
- Capture baseline JFR/RenderDoc evidence before selecting thresholds or enabling the Flywheel backend by default.
