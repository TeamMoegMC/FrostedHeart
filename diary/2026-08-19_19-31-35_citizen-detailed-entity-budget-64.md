# 居民高质量实体预算调整为 64

- Time: `2026-08-19 19:31:35 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `completed`
- Scope: `docs/town/citizen-rendering-at-scale.md`

## Completed

- Unified the proposed `maxDetailedCitizenEntities` default and acceptance ceiling at 64 high-quality client proxy entities.
- Updated the architecture diagram, milestone acceptance, performance contract, and Top-K test capacities.
- Retained separate 32- and 64-entity benchmark cases so the final default remains evidence-based across supported client configurations.

## Decisions

- Treat 64 as the proposed high-quality default while keeping the configurable `0..128` range and strict Top-K cap.
- Keep sleeping citizens out of the proxy-entity layer and continue rendering non-promoted near citizens through the crowd backend.
- Require the same render-thread p95 budget for the 64-entity case; reduce only the general default, not the supported maximum, if profiling fails.

## Validation

- Checked all remaining `32` and `64` references in the rendering plan and left unrelated distance ranges and byte-size limits unchanged.
- Documentation-only change; no Java build or test suite was required.

## Remaining

- Implement and profile the 32/64 comparison during milestone M0 before enabling the new backend by default.
