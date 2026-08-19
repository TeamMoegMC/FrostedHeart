# Citizen Oculus dual path and shared Body semantics

- Time: `2026-08-20 01:52:35 +08:00`
- Author: `Codex (OpenAI GPT-5; primary coding agent)`
- Status: `partial`
- Scope: `Citizen CPU/M3 rendering compatibility, shared layout/animation semantics, backend lifecycle, tests, and town living documentation`

## Completed

- Confirmed that Flywheel 0.6.11 intentionally selects `OFF` while an Oculus shader pack is active. The missing Billboard head came from Citizen falling back to the older CPU renderer, not from the M3 head shader itself.
- Split requested backend intent from the active backend. A temporary M3 failure now activates `cpu_batch` without losing the M3 request; `ReloadRenderersEvent` retries M3 after Flywheel rebuilds its renderer. Explicit `backend cpu_batch` cancels restoration.
- Consolidated CPU/M3 Body and Billboard semantics in `CitizenBatchRenderLayout`: six Body parts, two Billboard quads, skin UVs, sleep anchors/scales, model axes, and limb swing signs/scales now have one Java definition.
- Moved accumulated walk phase ownership from Flywheel entries to `ClientCitizen`. Backend switches, Body/Billboard LOD changes, renderer reloads, and origin rebuilds no longer reset the phase; the duplicate Flywheel phase map was removed.
- Updated the CPU Body fallback to remove its old whole-body bob and use the same snapshot-distance phase, speed-scaled opposing limb motion, part pivots, and sleeping geometry as M3.
- Kept the implementation compact: no new Body/animation class was introduced, and the temporary standalone `CitizenBillboardLayout` was folded into the existing batch layout.
- Added backend status semantics and a fixed Oculus enable/disable validation sequence to `docs/town/citizen-rendering-at-scale.md`; updated `docs/town/hybrid-simulation-architecture.md` to describe the shared contract.

## Decisions

- Follow Create's dual-path compatibility boundary: Flywheel instancing when available, standard Minecraft `RenderType`/`VertexConsumer` CPU batching while shaders disable instancing. Do not bypass Flywheel's shader-pack safety check.
- Do not add an Oculus shadow-pass Mixin in this fix. Batched citizens are not entities replayed by Oculus's entity shadow pass; detailed `FakeCitizenEntity` residents retain normal shadows, while batch Body shadow support remains a separate optional integration.
- Keep `cpu_batch` as the startup default until the remaining Oculus visual matrix and GPU budget are measured.

## Validation

- Focused layout, Flywheel, and coordinator tests: passed, `26` tests.
- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.town.citizen.*" --console=plain`: passed.
- `./gradlew.bat test --console=plain`: passed, `71` suites / `245` tests / `0` failures / `0` errors / `0` skipped.
- Automated coverage includes shared Body/Billboard dimensions and UVs, Body part animation signs, `ClientCitizen` walk-phase ownership, shader constants, temporary CPU fallback restoration, explicit CPU cancellation, and M3 health recovery.

## Remaining

- In an Oculus client, validate CPU Body and Billboard body/head rendering with a shader pack enabled, then disable shaders and confirm automatic M3 restoration without missing, misplaced, or disappearing residents.
- Complete JFR/RenderDoc CPU/GPU performance capture before making M3 the default.
