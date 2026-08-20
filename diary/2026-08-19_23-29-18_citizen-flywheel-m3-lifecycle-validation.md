# Citizen Flywheel M3 lifecycle validation

- Time: `2026-08-19 23:29:18 +08:00`
- Author: `Codex (OpenAI GPT-5); primary coding agent`
- Status: `partial`
- Scope: `FlywheelCitizenBackend M3 client validation and town living documentation`

## Completed

- Interpreted and recorded the 1024-moving F3+T and moving-to-sleeping benchmark results for the 58-byte M3 instance layout.
- Updated `citizen-rendering-at-scale.md` and `hybrid-simulation-architecture.md` to distinguish validated instance lifecycle counters from pending visual and performance acceptance.

## Decisions

- Accept the F3+T peak of `111,360 B` as exactly one deletion and recreation of 960 batch-owned instances: `960 * 58 * 2`.
- Accept the moving-to-sleeping peak of `115,072 B` as a one-frame model migration: 960 walking Body deletions plus 1024 sleeping Body creations, `(960 + 1024) * 58`. It is not a 112 KiB resident buffer or recurring full upload.
- Treat Flywheel's missing `uWindowSize` DEBUG as harmless because the citizen program does not use that uniform and the shader compiler removes it.
- Keep `cpu_batch` as the default until visual behavior, Body/Billboard transitions, compatibility, and GPU budgets are validated.

## Validation

- After F3+T with `benchmark=moving:1024`: `detailed=64`, `body=960`, `billboard=0`, `batchDraws=7`, steady `instanceDirtyBytes=0`, peak `111360`.
- After direct switch to `benchmark=sleeping:1024`: `detailed=0`, `body=1024`, `billboard=0`, `batchDraws=7`, steady `instanceDirtyBytes=0`, peak `115072`.
- Both peaks match the exact 58-byte lifecycle accounting formulas; no persistent per-frame instance upload is present in the reported steady frames.

## Remaining

- Manually confirm moving citizens translate, turn, halt, and animate limbs without UV, orientation, lighting, ghosting, or depth errors.
- Confirm sleeping Body orientation and UVs, then exercise Body/Billboard transitions across 68/72 blocks.
- Test Embeddium and Oculus without/with a shader pack, then collect JFR/RenderDoc measurements before enabling M3 by default.
