# Space station ring layout and section slots

- Time: `2026-10-18 03:50:04 +08:00`
- Author: `DeepSeek Harness coding agent; AI implementation agent`
- Status: `completed`
- Scope: `content/space/station` ring geometry, section roles and station assembly; `src/test/java/.../space/station`; `docs/space`

## Completed

- Replaced the empty `SpaceStation` skeleton with a station ring model:
  - `StationShape`: `innerRadius`, `layerCount`, `layerDepth`, `sectionArcLength`, `phaseRadians`; derives
    `layerRadius(L) = innerRadius + L * layerDepth` and `count(L) = max(1, floor(2πr(L) / sectionArcLength))`.
  - `StationLayout`: immutable slot table built from a shape; slot `i` of `n` sits at
    `phase + 2π(i + 0.5) / n`, centre `(r cos, r sin)`, anchor `(round(x), 0, round(z))`.
  - `StationSlot`: layer, index, per-layer count, angle, exact centre, block anchor and `anchorAt(int y)`.
  - `SpaceStation`: placement, `sectionAt`, `sections`, `sectionCount`, `remainingSlots`, `count(type)`,
    `missingTypes`, `work(time)`, plus the `SpaceResource` stockpile.
- Added the seven section roles in `SectionType`: `RESIDENTIAL`, `AGRICULTURE`, `MANUFACTURING`, `REFINING`,
  `POWER`, `PORT`, `LIFE_SUPPORT`.
- `Section` now exposes `getType()` and `getSlot()`; `StationSection` is the placeholder implementation.
- `StationLayout` rejects a shape whose rounded anchors collide instead of giving two sections the same position.
- Added `docs/space/README.md` and the `Space station` row in `docs/README.md`.

## Decisions

- Layers are concentric rings in one ring plane, because the requirement ties section count to the perimeter; `y`
  is `0` locally and `StationSlot#anchorAt` covers other deck heights. Vertical stacking is not modelled yet.
- Section count is always derived from the circumference and never stored separately.
- Slot boundaries sit on `phaseRadians`; centres are offset by half a slot. Anchors are rounded block positions, so a
  layout whose slots would collide is rejected at build time.
- Section behaviour stays out of the geometry model: one placeholder `StationSection` is enough until role-specific
  production is designed; dedicated classes belong in `content/space/station/sections`.
- No world blocks, block entities, menus, packets or persistence were added in this step.

## Validation

- `./gradlew.bat test --tests "com.teammoeg.frostedheart.content.space.station.*"` passed: `12` tests, no failures
  (`StationLayoutTest` 7, `SpaceStationTest` 5).
- `./gradlew.bat test` (full unit suite): `691` tests, `3` failures, all outside `content/space` and untouched by
  this change:
  - `DormantChunkThermalStateTest` is order-dependent; it passes when rerun alone.
  - `TownHousingPlanTest#normalizationAddsBetterHousingFirstAndEditsAreBounded` throws
    `Cannot get config value before config is loaded` in the development environment.
  - `WarehouseBuildingCodecTest#codecPersistsOnlyWarehouseStructureAndCapacityState` fails its codec assertion.
- `git diff --check` passed.

## Remaining

- Station origin/world placement, station blocks and block entities, `ConstructionSite` behaviour.
- Persistence and codecs for `StationShape`/`SpaceStation`, synchronization and menus.
- Role-specific production, consumption and power rules for the seven section types.
- Docs impact: new living document `docs/space/README.md`; `docs/README.md` index updated. No existing system
  document changed.
