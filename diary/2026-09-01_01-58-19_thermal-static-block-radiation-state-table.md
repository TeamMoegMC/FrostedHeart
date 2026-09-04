# Thermal static Block radiation and state-table convergence

- Time: `2026-09-01 01:58:19 +08:00`
- Author: `Codex; OpenAI GPT-5; primary implementation agent`
- Status: `partial`
- Scope: `thermal profile tables, Page signatures, static fire/lava radiation, receiver lifecycle, configuration, tests, plans, and living climate documentation`

## Completed

- Replaced `ThermalSignatureRegistry` and per-dimension
  `ThermalSignatureCatalog` with one server-wide primitive
  `ThermalSignatureTable` that interns geometry separately and shares component
  lookup by geometry.
- Replaced the BlockState identity/phase maps with one tagged
  `MinecraftStateThermalTable`. Campfire, static radiation, and exceptional DDA
  occlusion semantics use sparse primitive extended entries.
- Changed uniform `PageSignatures` payloads from per-Brick length-one arrays to
  canonical immutable `Integer` values; nonuniform payloads remain
  `char[64]`/`int[64]`.
- Added `BlockRadiationIndex`: one packed emitter per emitting Brick, sparse
  section storage, two 20-tick dirty buffers, exposed-area lava compilation,
  fixed fire power, owner-index-first neighbor capture, and bounded nearby
  discovery through the existing `RadiationService` candidate/ray cache.
- Added the targeted `LiquidBlock.updateShape` lava marker, exact
  `STATIC_BLOCK_REVISION`, O(1) receiver removal on logout/dimension exit, static
  fire tag, and restart-only COMMON configuration.
- Updated the active plan and living runtime, heat-production, lifecycle, and
  player-temperature documentation.

## Decisions

- Static fire/lava radiation is a read-only player observation. It does not
  write Air, material, phase, crops, dormant state, infrared, or
  `ThermalSourceLedger`.
- Ordinary mutation stays on one tagged-state lookup. Signature-equal DDA
  occlusion exceptions use one byte per signature plus sparse extended bytes,
  not a dense BlockState bitset or per-mutation predicate calls.
- Horizontal boundary capture reuses `ownersBySection`; `getChunkNow` is only a
  missing-attachment fallback. Six section references are reusable scratch and
  clear after every section batch.
- Two newly added GameTests were removed at the user's request. No production
  test hooks or counters were retained.

## Validation

- `compileJava`, `compileTestJava`, and `compileGameTestJava`: passed.
- Thermal JUnit selection: `104/104` passed.
- `RadiationServiceTest` covers static one-torso-ray sampling, witness reuse,
  and exact receiver removal. `ThermalSignatureTableTest` covers dense IDs,
  canonical uniform payloads, and geometry-shared component lookup.
- Final Forge GameTest execution was not rerun after the requested GameTest
  cleanup; the user took over live validation.

## Remaining

- Validate ordinary fire, soul fire, exposed/enclosed lava, wall occlusion,
  extinguishing, and chunk-boundary behavior in the user's live game.
- Run controlled 100-player/source JFR and retained-heap workloads before
  claiming measured CPU or memory percentages.
