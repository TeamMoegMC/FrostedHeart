# Thermal sparse top-layer sky capture plan

- Time: `2026-09-01 00:14:38 +08:00`
- Author: `Codex; OpenAI GPT-5; primary architecture/documentation agent`
- Status: `completed`
- Scope: `active thermal Brick residency plan direct-sky capture cost`

## Completed

- Replaced fixed 256-column heightmap capture for every new Page with 16-column
  capture for each newly resident Page-top-layer Brick.
- Kept the existing `byte[256]` sky array and sparse `PageEnvironmentUpdate`;
  unknown columns initialize to `16` and cannot prove direct sky.
- Defined resident top-layer bits as the existing proof of captured column
  groups, avoiding a second `skyKnownMask` or another message schema.
- Updated cold-admission cost, implementation order, resync behavior, and
  acceptance scenarios for zero-query lower-layer admission and incremental
  top-layer additions.

## Decisions

- Retain one Page natural-temperature query because it initializes Air/material
  state and owns the Page residual/FarField reference.
- Retain 64 BlockState reads as the exact worst-case lower bound for a new
  non-air Brick; `hasOnlyAir()` remains the zero-read section fast path.
- Do not inspect `PalettedContainer` internals or add uniform-solid heuristics,
  sparse sky storage, new masks, caches, or compatibility accessors.

## Validation

- Source search confirms `firstExposedLocalY` is consumed only by direct-sky
  boundary compilation.
- `git diff --check` passed for the active plan with only the existing
  line-ending warning.
- Every relative Markdown link in the active plan resolves.
- Plan search confirms no fixed-256 admission-cost statement remains.
- Java tests were not run because this work changes plans and diary only.

## Remaining

- Implement the corrected plan beginning with its threshold reference fixture,
  then run the functional and controlled 100-source validation gates.
