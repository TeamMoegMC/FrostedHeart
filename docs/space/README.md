# Space Station

- Status: `Current` (ring layout and section placement only; no world content yet)
- Last verified: `2026-10-18`
- Scope: station ring parameters, per-layer section counts, section slot coordinates, section roles, and station assembly
- Code anchors: [`StationShape`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/StationShape.java), [`StationLayout`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/StationLayout.java), [`StationSlot`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/StationSlot.java), [`SpaceStation`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/SpaceStation.java), [`SectionType`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/SectionType.java), [`Section`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/Section.java)

## What This System Is

A space station is a flat ring. The ring is made of concentric layers from the inside (`layer 0`) to the outside
(`layerCount - 1`). Every layer is divided into sections, and each section occupies exactly one slot with a stable
coordinate. The seven section roles are `RESIDENTIAL`, `AGRICULTURE`, `MANUFACTURING`, `REFINING`, `POWER`, `PORT`,
and `LIFE_SUPPORT` ([`SectionType`](../../src/main/java/com/teammoeg/frostedheart/content/space/station/SectionType.java)).

This document covers geometry and assembly only. Production, consumption, power, construction and world block content
are not implemented yet.

## Geometry

`StationShape(innerRadius, layerCount, layerDepth, sectionArcLength, phaseRadians)`:

| Symbol | Meaning | Unit |
|---|---|---|
| `innerRadius` | centreline radius of the innermost layer | blocks |
| `layerDepth` | radial distance between two adjacent layer centrelines | blocks |
| `layerCount` | number of concentric layers | count |
| `sectionArcLength` | target arc length occupied by one section | blocks |
| `phaseRadians` | rotation of the slot grid around the ring | radians |

Layer `L` has centreline radius

```text
r(L) = innerRadius + L * layerDepth
```

and circumference

```text
C(L) = 2 * π * r(L)
```

The number of sections on that layer is derived from the circumference, never stored separately:

```text
count(L) = max(1, floor(C(L) / sectionArcLength))
```

The outer layers therefore always have at least as many sections as the inner layers, and a short ring still has one
valid slot per layer. `StationShape.totalSectionCount()` is the sum over all layers.

## Slot Coordinates

Slots of a layer are indexed in increasing angle order. Slot `i` of a layer with `n` slots has its centre at

```text
angle(i) = phaseRadians + 2π * (i + 0.5) / n
centerX  = r(L) * cos(angle(i))
centerZ  = r(L) * sin(angle(i))
```

Slot boundaries sit on `phaseRadians`, so slot centres are offset by half a slot from the seam at angle zero.

The integer block position of a slot is

```text
anchor = (round(centerX), 0, round(centerZ))
```

`anchor.getY()` is `0` because every layer is drawn in the same ring plane; `StationSlot#anchorAt(int y)` returns the
same position at another deck height. `StationSlot#centerRadius()` returns the exact distance from the ring centre.

Deriving anchors by rounding is only valid while two slots cannot round to the same block.
`StationLayout.of(shape)` rejects such a shape with `IllegalArgumentException` instead of silently giving two sections
the same position; increase `layerDepth` or `sectionArcLength` in that case.

## Assembly

`StationLayout` is immutable and built once per shape:

- `slots()` — every slot, ordered by layer and then index;
- `slots(int layer)` — one layer's slots, ordered around the ring;
- `slot(int layer, int index)` — a single slot;
- `slotAt(Vec3i anchor)` — the slot at an anchor, if any;
- `sectionCount()` / `sectionCount(int layer)` / `layerCount()`.

`SpaceStation` binds sections to that layout:

- `place(Section)` accepts a section only when its slot was derived from this layout and the anchor is still empty;
- `place(SectionType, layer, index)` places a plain `StationSection`;
- `sectionAt(Vec3i)` / `sections()` / `sectionCount()` / `remainingSlots()` / `count(SectionType)` / `missingTypes()`;
- `work(int time)` runs every placed section in placement order.

`SpaceStation` also owns the station stockpile keyed by `SpaceResource` (`FOODS`, `WATER`, `OXYGEN`, `CARBON_DIOXIDE`,
`COMPONENT`, `METAL`, `ORE`, `ICE`, `ELECTRIC`, `FUEL_CELL`, `NUCLEAR_CELL`, `FUSION_CELL`).

`Section` is the behaviour seam: implementations provide `getType()`, `getSlot()` and `work(SpaceStation, int)`.
`StationSection` is the placeholder implementation used until a role gets dedicated behaviour in
`content/space/station/sections`.

## Boundaries

- No world blocks, block entities, menus, packets, or persistence are implemented for stations.
- `ConstructionSite` is still an empty placeholder.
- Layers only vary in radius; there is no vertical stacking or ring thickness model.
- Station-local coordinates only; placing a station in a world is a later concern.

## Minimum Action

```text
./gradlew test --tests "com.teammoeg.frostedheart.content.space.station.*"
```
